# Reliable Background Job Processing: Tasks, Queues, Workers, Leases, Heartbeats, Retries and Idempotency

What you are describing is one of the most important patterns in distributed backend engineering.

It appears in almost every large-scale system:

- Video processing platforms: encode a video, generate thumbnails, create different resolutions.
- Payment systems: process payments, reconcile transactions, issue refunds.
- E-commerce: generate invoices, reserve inventory, send notifications.
- Data platforms: execute reports, export large datasets, process files.
- AI platforms: execute LLM workflows, generate embeddings, process documents.
- Cloud platforms: run deployments, provision infrastructure, execute long-running workflows.

The pattern is often called Reliable Asynchronous Job Processing, with concepts from distributed task queues, leases, heartbeats, work claiming, retry management and idempotent processing.

We will build a production-grade design from first principles.

The important thing is not just to understand how a worker picks up a message. You should understand what happens when:

1. The API server crashes after saving a task but before publishing to SQS.
2. Two workers receive the same message.
3. A worker crashes after starting the task.
4. A worker becomes unresponsive but is still running.
5. Processing takes 30 minutes or several hours.
6. A heartbeat succeeds but extending the SQS visibility timeout fails.
7. A worker completes the task but crashes before acknowledging the message.
8. A task is retried five times and fails every time.
9. A previous worker resumes after its lease has expired.
10. The external operation succeeds but updating the database fails.

These are not unusual edge cases. They are normal failure scenarios in distributed systems.

# 1. Start with the basic problem

Imagine we are building a video processing platform.

A user uploads a video and requests processing.

The processing might involve:

- Validating the uploaded file.
- Extracting metadata.
- Generating thumbnails.
- Creating 480p, 720p and 1080p versions.
- Uploading processed files to S3.
- Updating the processing status.

This might take 20 seconds, 10 minutes or even an hour.

We don't want the user's HTTP request to remain open until the entire processing is finished.

Instead, we separate task creation from task execution.

## Initial architecture

\#chatgpt-mermaid-\_r_6qv\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_6qv\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_6qv\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_6qv\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_6qv\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6qv\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_6qv\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_6qv\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_6qv\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_6qv\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_6qv\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_6qv\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6qv\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6qv\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_6qv\_ p{margin:0;}#chatgpt-mermaid-\_r_6qv\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6qv\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6qv\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6qv\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_6qv\_ .label text,#chatgpt-mermaid-\_r_6qv\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6qv\_ .node rect,#chatgpt-mermaid-\_r_6qv\_ .node circle,#chatgpt-mermaid-\_r_6qv\_ .node ellipse,#chatgpt-mermaid-\_r_6qv\_ .node polygon,#chatgpt-mermaid-\_r_6qv\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_6qv\_ .rough-node .label text,#chatgpt-mermaid-\_r_6qv\_ .node .label text,#chatgpt-mermaid-\_r_6qv\_ .image-shape .label,#chatgpt-mermaid-\_r_6qv\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_6qv\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_6qv\_ .rough-node .label,#chatgpt-mermaid-\_r_6qv\_ .node .label,#chatgpt-mermaid-\_r_6qv\_ .image-shape .label,#chatgpt-mermaid-\_r_6qv\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_6qv\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_6qv\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6qv\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6qv\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_6qv\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_6qv\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_6qv\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6qv\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6qv\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_6qv\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_6qv\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6qv\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6qv\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_6qv\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6qv\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_6qv\_ .icon-shape,#chatgpt-mermaid-\_r_6qv\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_6qv\_ .icon-shape p,#chatgpt-mermaid-\_r_6qv\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_6qv\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_6qv\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6qv\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_6qv\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_6qv\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_6qv\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_6qv\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_6qv\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_6qv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6qv\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_6qv\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_6qv\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_6qv\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6qv\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_6qv\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_6qv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6qv\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_6qv\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_6qv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6qv\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_6qv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6qv\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_6qv\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_6qv\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_6qv\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_6qv\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_6qv\_ .node rect,#chatgpt-mermaid-\_r_6qv\_ .node circle,#chatgpt-mermaid-\_r_6qv\_ .node ellipse,#chatgpt-mermaid-\_r_6qv\_ .node polygon,#chatgpt-mermaid-\_r_6qv\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_6qv\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_6qv\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_6qv\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_6qv\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6qv\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientAPIDatabaseSQS QueueWorker 1Worker 2Worker 3

The API creates a task and stores it in the database.

Then it publishes a message to SQS.

Workers independently consume messages and execute the actual work.

For example:

Step 1: User requests processing

```
POST /videos/123/process
```

Step 2: API creates a task

Database:

| id       | resource_id | status |
| -------- | ----------- | ------ |
| task-101 | video-123   | QUEUED |

Step 3: API publishes a message

```
{
  "taskId": "task-101",
  "eventId": "event-101",
  "eventType": "VIDEO_PROCESSING_REQUESTED"
}
```

Step 4: A worker receives the message.

Worker 2 picks up `task-101` and starts processing.

Step 5: Worker updates the database.

| id       | resource_id | status  |
| -------- | ----------- | ------- |
| task-101 | video-123   | RUNNING |

Step 6: Worker finishes processing.

| id       | resource_id | status    |
| -------- | ----------- | --------- |
| task-101 | video-123   | SUCCEEDED |

That is the happy path.

But this design is not reliable yet.

We need to understand why.

# 2. First fundamental concept: The queue is not the source of truth

Suppose the API performs these two operations:

```
Task task = taskRepository.save(newTask);

sqsClient.sendMessage(message);
```

What happens when the database operation succeeds but SQS publishing fails?

For example:

```
1. API receives request.
2. Task is saved in DB.
3. API tries publishing message to SQS.
4. SQS is unavailable.
5. API fails.
```

Now:

```
Database:
    task-101 -> QUEUED

SQS:
    No message
```

The task exists, but nobody knows it needs to be processed.

This is called the dual-write problem.

We are writing to two independent systems:

- Database.
- Message broker.

There is no ordinary database transaction that atomically commits both operations.

## Solution: Transactional Outbox Pattern

Instead of publishing directly to SQS, we introduce an outbox table.

The API performs two database writes in one database transaction:

1. Insert the task.
2. Insert an outbox event.

```
BEGIN;

INSERT INTO tasks (
    id,
    status,
    created_at
)
VALUES (
    'task-101',
    'QUEUED',
    NOW()
);

INSERT INTO outbox_events (
    id,
    aggregate_id,
    event_type,
    payload,
    status,
    created_at
)
VALUES (
    'event-101',
    'task-101',
    'TASK_CREATED',
    '{"taskId":"task-101"}',
    'PENDING',
    NOW()
);

COMMIT;
```

Now both records either exist or neither exists.

A separate background publisher continuously reads pending outbox events and publishes them to SQS.

\#chatgpt-mermaid-\_r_6s1\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_6s1\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_6s1\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_6s1\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_6s1\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6s1\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_6s1\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_6s1\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_6s1\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_6s1\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_6s1\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_6s1\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6s1\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6s1\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_6s1\_ p{margin:0;}#chatgpt-mermaid-\_r_6s1\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6s1\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6s1\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6s1\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_6s1\_ .label text,#chatgpt-mermaid-\_r_6s1\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6s1\_ .node rect,#chatgpt-mermaid-\_r_6s1\_ .node circle,#chatgpt-mermaid-\_r_6s1\_ .node ellipse,#chatgpt-mermaid-\_r_6s1\_ .node polygon,#chatgpt-mermaid-\_r_6s1\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_6s1\_ .rough-node .label text,#chatgpt-mermaid-\_r_6s1\_ .node .label text,#chatgpt-mermaid-\_r_6s1\_ .image-shape .label,#chatgpt-mermaid-\_r_6s1\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_6s1\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_6s1\_ .rough-node .label,#chatgpt-mermaid-\_r_6s1\_ .node .label,#chatgpt-mermaid-\_r_6s1\_ .image-shape .label,#chatgpt-mermaid-\_r_6s1\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_6s1\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_6s1\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6s1\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6s1\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_6s1\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_6s1\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_6s1\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6s1\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6s1\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_6s1\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_6s1\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6s1\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6s1\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_6s1\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6s1\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_6s1\_ .icon-shape,#chatgpt-mermaid-\_r_6s1\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_6s1\_ .icon-shape p,#chatgpt-mermaid-\_r_6s1\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_6s1\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_6s1\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6s1\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_6s1\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_6s1\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_6s1\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_6s1\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_6s1\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_6s1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6s1\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_6s1\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_6s1\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_6s1\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6s1\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_6s1\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_6s1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6s1\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_6s1\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_6s1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6s1\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_6s1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6s1\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_6s1\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_6s1\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_6s1\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_6s1\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_6s1\_ .node rect,#chatgpt-mermaid-\_r_6s1\_ .node circle,#chatgpt-mermaid-\_r_6s1\_ .node ellipse,#chatgpt-mermaid-\_r_6s1\_ .node polygon,#chatgpt-mermaid-\_r_6s1\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_6s1\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_6s1\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_6s1\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_6s1\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6s1\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientAPIDatabase TransactionTasks TableOutbox TableOutbox PublisherSQSWorker Instances

The publisher can:

1. Read pending events.
2. Publish them to SQS.
3. Mark them as published.

There is still one important failure:

```
1. Publisher sends event to SQS.
2. SQS successfully stores it.
3. Publisher crashes before marking outbox event as published.
4. Publisher restarts.
5. It publishes the same event again.
```

Therefore, the message can be duplicated.

This is expected. Our system must tolerate duplicate messages.

That brings us to our next concept.

# 3. Second fundamental concept: At-least-once delivery

A common misconception is:

> Once a worker receives a message, that message belongs exclusively to that worker.

That is not a safe assumption.

Let's understand SQS.

When Worker 1 receives a message, SQS does not immediately delete it.

Instead, it temporarily hides the message from other consumers.

This period is called the Visibility Timeout.

For example, configure:

```
Visibility Timeout = 60 seconds
```

The flow becomes:

```
Time 0:
    Message is available in SQS.

Time 1:
    Worker 1 receives the message.

Time 1 - 61:
    Message is invisible to other consumers.

Time 61:
    If the message has not been deleted,
    it becomes visible again.
```

Another worker can receive it.

Why does SQS work this way?

Because SQS cannot know whether Worker 1:

- Is still processing.
- Has crashed.
- Has become unresponsive.
- Has completed processing but failed to acknowledge it.

So SQS uses a timeout as a recovery mechanism.

An important distinction:

Receiving a message is not the same as completing a message.

The worker must explicitly delete the message after successfully handling it.

Amazon SQS Standard queues provide at-least-once delivery. In rare cases, even a message that was received and deleted can be delivered again because of the distributed nature of the service. Therefore, consumers must be idempotent.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Queue Service+2



This gives us our first major guarantee:

The queue helps us retry delivery, but it does not guarantee that business processing happens only once.

We must solve that at the application level.

# 4. Introduce a task state machine

Now we need to move from a simple status column to a well-defined lifecycle.

A task is not simply `QUEUED`, then `RUNNING`, then `COMPLETED`.

There are retries, cancellations, worker failures and scheduling delays.

We will use the following states.

| Status             | Meaning                                              |
| ------------------ | ---------------------------------------------------- |
| `QUEUED`           | Task is waiting to be picked up.                     |
| `RUNNING`          | A worker has successfully claimed the task.          |
| `RETRY_WAIT`       | Processing failed, but another attempt is scheduled. |
| `SUCCEEDED`        | Processing completed successfully.                   |
| `FAILED`           | Processing failed permanently or exhausted retries.  |
| `CANCEL_REQUESTED` | Someone requested cancellation of a running task.    |
| `CANCELLED`        | Task was successfully cancelled.                     |

The state transitions look like this:

\#chatgpt-mermaid-\_r_6t4\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_6t4\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_6t4\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_6t4\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_6t4\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6t4\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_6t4\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_6t4\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_6t4\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_6t4\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_6t4\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_6t4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6t4\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6t4\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_6t4\_ p{margin:0;}#chatgpt-mermaid-\_r_6t4\_ defs [id$="-barbEnd"]{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6t4\_ g.stateGroup text{fill:rgb(31, 78, 148);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_6t4\_ g.stateGroup text{fill:rgb(237, 237, 237);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_6t4\_ g.stateGroup .state-title{font-weight:bolder;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6t4\_ g.stateGroup rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_6t4\_ g.stateGroup line{stroke:rgb(175, 175, 175);stroke-width:1;}#chatgpt-mermaid-\_r_6t4\_ .transition{stroke:rgb(175, 175, 175);stroke-width:1;fill:none;}#chatgpt-mermaid-\_r_6t4\_ .stateGroup .composit{fill:transparent;border-bottom:1px;}#chatgpt-mermaid-\_r_6t4\_ .stateGroup .alt-composit{fill:#e0e0e0;border-bottom:1px;}#chatgpt-mermaid-\_r_6t4\_ .state-note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_6t4\_ .state-note text{fill:rgb(237, 237, 237);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_6t4\_ .stateLabel .box{stroke:none;stroke-width:0;fill:rgb(9, 23, 44);opacity:0.5;}#chatgpt-mermaid-\_r_6t4\_ .edgeLabel .label rect{fill:rgb(9, 23, 44);opacity:0.5;}#chatgpt-mermaid-\_r_6t4\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_6t4\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6t4\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6t4\_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6t4\_ .label div .edgeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6t4\_ .stateLabel text{fill:rgb(237, 237, 237);font-size:10px;font-weight:bold;}#chatgpt-mermaid-\_r_6t4\_ .node circle.state-start{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6t4\_ .node .fork-join{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6t4\_ .node circle.state-end{fill:rgb(31, 78, 148);stroke:transparent;stroke-width:1.5;}#chatgpt-mermaid-\_r_6t4\_ .end-state-inner{fill:transparent;stroke-width:1.5;}#chatgpt-mermaid-\_r_6t4\_ .node rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_6t4\_ .node polygon{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_6t4\_ [id$="-barbEnd"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6t4\_ .statediagram-cluster rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_6t4\_ .cluster-label,#chatgpt-mermaid-\_r_6t4\_ .nodeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6t4\_ .statediagram-cluster rect.outer{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_6t4\_ .statediagram-state .divider{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_6t4\_ .statediagram-state .title-state{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_6t4\_ .statediagram-cluster.statediagram-cluster .inner{fill:transparent;}#chatgpt-mermaid-\_r_6t4\_ .statediagram-cluster.statediagram-cluster-alt .inner{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_6t4\_ .statediagram-cluster .inner{rx:0;ry:0;}#chatgpt-mermaid-\_r_6t4\_ .statediagram-state rect.basic{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_6t4\_ .statediagram-state rect.divider{stroke-dasharray:10,10;fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_6t4\_ .note-edge{stroke-dasharray:5;}#chatgpt-mermaid-\_r_6t4\_ .statediagram-note rect{fill:rgb(48, 48, 48);stroke:rgb(58, 132, 63);stroke-width:1px;rx:0;ry:0;}#chatgpt-mermaid-\_r_6t4\_ .statediagram-note rect{fill:rgb(48, 48, 48);stroke:rgb(58, 132, 63);stroke-width:1px;rx:0;ry:0;}#chatgpt-mermaid-\_r_6t4\_ .statediagram-note text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6t4\_ .statediagram-note .nodeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6t4\_ .statediagram .edgeLabel{color:red;}#chatgpt-mermaid-\_r_6t4\_ [id$="-dependencyStart"],#chatgpt-mermaid-\_r_6t4\_ [id$="-dependencyEnd"]{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);stroke-width:1;}#chatgpt-mermaid-\_r_6t4\_ .statediagramTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6t4\_ [data-look="neo"].statediagram-cluster rect{fill:rgb(9, 23, 44);stroke:url(#chatgpt-mermaid-\_r_6t4\_-gradient);stroke-width:1;}#chatgpt-mermaid-\_r_6t4\_ [data-look="neo"].statediagram-cluster rect.outer{rx:5px;ry:5px;filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6t4\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_6t4\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_6t4\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_6t4\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_6t4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6t4\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_6t4\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_6t4\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_6t4\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6t4\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_6t4\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_6t4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6t4\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_6t4\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_6t4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6t4\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_6t4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6t4\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}QUEUEDRUNNINGSUCCEEDEDRETRY_WAITFAILEDCANCELLEDCANCEL_REQUESTEDWorker claims taskProcessing succeedsRetryable failureRetry becomes duePermanent failure / retriesexhaustedCancelCancelCancellation requestedWorker stopsCompletion winscancellation race

Notice something important:

`RETRY_WAIT` is different from `QUEUED`.

A task in `RETRY_WAIT` should not be processed immediately.

For example:

```
Task: task-101

Attempt 1:
    Failed because an external API returned HTTP 503.

Retry policy:
    Retry after 30 seconds.

Current state:
    RETRY_WAIT

next_attempt_at:
    2026-11-05 17:30:30
```

When the scheduled retry becomes due, a scheduler or dispatcher publishes another message and makes the task eligible for processing.

This separation gives us control over retry timing.

# 5. Design the database properly

Let's design the core tables before discussing workers further.

The database is our durable task ledger and source of truth.

SQS is responsible for message delivery. The database is responsible for task state, ownership, attempts, scheduling and results.

We need at least three tables.

## 5.1 Tasks table

PostgreSQL example:

```
CREATE TABLE tasks (
    id UUID PRIMARY KEY,

    task_type VARCHAR(100) NOT NULL,

    payload JSONB NOT NULL,

    status VARCHAR(30) NOT NULL,

    -- Retry management
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 5,
    next_attempt_at TIMESTAMPTZ,

    -- Worker ownership
    lease_owner VARCHAR(150),
    lease_token BIGINT NOT NULL DEFAULT 0,
    lease_expires_at TIMESTAMPTZ,

    -- Monitoring
    heartbeat_at TIMESTAMPTZ,
    progress_at TIMESTAMPTZ,
    progress JSONB,

    -- Execution information
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    -- Failure information
    last_error_code VARCHAR(100),
    last_error_message TEXT,

    -- API-level business deduplication
    idempotency_key VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Some important fields deserve special attention.

| Field              | Why do we need it?                                          |
| ------------------ | ----------------------------------------------------------- |
| `attempt_count`    | How many application-level execution attempts have started? |
| `max_attempts`     | Maximum permitted attempts.                                 |
| `next_attempt_at`  | When should a retry become eligible?                        |
| `lease_owner`      | Which worker instance currently owns the task?              |
| `lease_token`      | Which generation of ownership is this?                      |
| `lease_expires_at` | Until when is this worker's ownership valid?                |
| `heartbeat_at`     | When did the worker last prove it was alive?                |
| `progress_at`      | When did it last report actual progress?                    |
| `progress`         | Current processing stage or progress information.           |

We will understand `lease_token` in depth shortly. It is particularly important.

Useful indexes:

```
CREATE INDEX idx_tasks_status_next_attempt
ON tasks (status, next_attempt_at);

CREATE INDEX idx_tasks_expired_leases
ON tasks (lease_expires_at)
WHERE status = 'RUNNING';

CREATE UNIQUE INDEX uq_tasks_idempotency
ON tasks (idempotency_key)
WHERE idempotency_key IS NOT NULL;
```

For a multi-tenant system, include `tenant_id` in the table and make the idempotency constraint tenant-aware.

## 5.2 Task attempts table

Why not keep all the retry information inside the tasks table?

Because we want an execution history.

Imagine:

```
task-101

Attempt 1:
    Worker 1
    Started at 10:00
    Failed at 10:02
    Error: External API timeout

Attempt 2:
    Worker 3
    Started at 10:03
    Failed at 10:07
    Error: S3 upload failure

Attempt 3:
    Worker 2
    Started at 10:10
    Succeeded at 10:15
```

The task table should represent the latest state.

The attempts table should represent the history.

```
CREATE TABLE task_attempts (
    id UUID PRIMARY KEY,

    task_id UUID NOT NULL REFERENCES tasks(id),

    attempt_number INTEGER NOT NULL,

    worker_id VARCHAR(150),

    lease_token BIGINT NOT NULL,

    status VARCHAR(30) NOT NULL,

    started_at TIMESTAMPTZ NOT NULL,

    finished_at TIMESTAMPTZ,

    error_code VARCHAR(100),

    error_message TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (task_id, attempt_number)
);
```

Possible attempt statuses:

```
RUNNING
SUCCEEDED
FAILED
ABANDONED
CANCELLED
```

An `ABANDONED` attempt is useful when a worker disappears without reporting success or failure.

For example:

```
Attempt 1:
    Worker 1 claimed task.
    Worker 1 crashed.
    Lease expired.
    Recovery process marked attempt ABANDONED.

Attempt 2:
    Worker 2 claimed task.
    Successfully completed.
```

This is useful for debugging and operational visibility.

## 5.3 Outbox table

We already discussed the transactional outbox.

It might look like:

```
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,

    aggregate_id UUID NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    payload JSONB NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    published_at TIMESTAMPTZ
);
```

The outbox publisher reads pending events, publishes them and marks them as published.

The publisher itself must handle duplicate publication.

An outbox event ID or task ID should be included in the SQS message to help identify duplicate events.

# 6. Third fundamental concept: A worker must claim a task

Now let's look at the most important part.

Suppose we have three worker instances:

```
Worker 1
Worker 2
Worker 3
```

SQS delivers `task-101` to Worker 1.

But because message delivery is at-least-once, Worker 2 might receive a duplicate.

Or the original message might become visible because the visibility timeout expires.

We must prevent both workers from processing the same task concurrently.

We need an ownership mechanism.

This is where database leases come in.

## 6.1 What is a lease?

A lease is a time-limited ownership agreement.

Think of renting a parking space.

You are allowed to use it until 10:30 AM.

If you renew the agreement, your ownership continues.

If you don't renew it, the space becomes available to someone else.

A worker lease works similarly.

Example:

```
Task: task-101

Worker: worker-1

Lease started:
    10:00:00

Lease expires:
    10:01:30
```

Worker 1 owns the task until 10:01:30, provided it continues renewing its lease.

If the worker crashes and does not renew the lease, the task becomes eligible for another worker to claim after expiry.

This is fundamentally different from an indefinite database lock.

We don't want to hold a database transaction or row lock open for 30 minutes while processing a video.

Instead:

1. Acquire ownership using a short database transaction.
2. Commit the transaction.
3. Perform the actual processing outside the transaction.
4. Renew ownership periodically using short transactions.
5. Release ownership by completing the task or transitioning it to another state.

## 6.2 How does a worker claim a task?

Let's assume:

```
Task ID: task-101

Worker ID: worker-1

Lease duration: 90 seconds
```

Worker 1 executes a conditional database update.

```
UPDATE tasks
SET
    status = 'RUNNING',

    attempt_count = attempt_count + 1,

    lease_owner = 'worker-1',

    lease_token = lease_token + 1,

    lease_expires_at = NOW() + INTERVAL '90 seconds',

    heartbeat_at = NOW(),

    started_at = COALESCE(started_at, NOW()),

    updated_at = NOW()

WHERE id = 'task-101'

AND (
    status IN ('QUEUED', 'RETRY_WAIT')
    OR (
        status = 'RUNNING'
        AND lease_expires_at < NOW()
    )
)

AND (
    next_attempt_at IS NULL
    OR next_attempt_at <= NOW()
)

RETURNING *;
```

This query performs several operations atomically:

- Checks whether the task is eligible.
- Changes its state to `RUNNING`.
- Increments the attempt count.
- Assigns ownership to the worker.
- Increments the lease token.
- Sets the lease expiration time.

The worker has successfully claimed the task only if the database returns the updated row.

If it returns no row, the worker did not acquire ownership.

Why is this important?

Suppose two workers execute the query simultaneously.

```
Worker 1:
    Claims task-101

Worker 2:
    Claims task-101
```

Both cannot successfully change the same eligible row from its previous state.

PostgreSQL coordinates concurrent updates using row locking and transaction isolation.

The conditional update is therefore our atomic ownership operation.

We should also insert the corresponding `task_attempts` record in the same short transaction.

If we use the `RUNNING`-with-expired-lease case, the old attempt should first be marked `ABANDONED` in that transaction.

## 6.3 What happens when a worker fails to claim?

There are several possibilities.

| Database state                    | What should the new worker do?                         |
| --------------------------------- | ------------------------------------------------------ |
| `SUCCEEDED`                       | Acknowledge/delete the duplicate SQS message.          |
| `FAILED`                          | Acknowledge/delete the message.                        |
| `CANCELLED`                       | Acknowledge/delete the message.                        |
| `RUNNING` with an unexpired lease | Another worker owns it. Do not process it.             |
| `QUEUED` or eligible `RETRY_WAIT` | Investigate why the claim failed; retry or reschedule. |
| `RUNNING` with an expired lease   | Try the atomic claim again.                            |

There is one subtle but important design decision here.

What should we do with an SQS message when the task is already being processed by another worker?

Two valid approaches exist:

Approach A: Delay the duplicate message.

Change its visibility timeout to approximately the remaining database lease duration.

This allows the message to become available again if the original worker fails.

Approach B: Delete the duplicate message.

This is safe only when we also have a reliable database-based recovery mechanism that discovers expired `RUNNING` tasks and republishes them.

For our production design, I prefer the second approach combined with a lease-recovery mechanism.

Why?

Because the database is the durable task ledger. We should not rely exclusively on an individual SQS message remaining available forever.

# 7. Fourth fundamental concept: There are TWO different leases

This is probably the most important distinction in the entire design.

You have two independent timeouts:

1. SQS Visibility Timeout.
2. Application Database Lease.

They solve related but different problems.

## 7.1 SQS Visibility Timeout

This is controlled by SQS.

Its purpose is:

> Prevent other consumers from immediately receiving the same message while the current consumer is processing it.

Example:

```
SQS message received by Worker 1.

Visibility timeout = 120 seconds.

During these 120 seconds:
    Other workers normally cannot receive that message.

After 120 seconds:
    If the message hasn't been deleted,
    it becomes available again.
```

It does not establish that Worker 1 is the legitimate owner of the business task.

It only controls message availability.

## 7.2 Database Lease

This is controlled by our application.

Its purpose is:

> Establish which worker currently has the authority to execute and update the task.

Example:

```
Task: task-101

Worker: worker-1

DB lease:
    Expires in 90 seconds.
```

The worker must renew this lease.

If the lease expires, the worker loses authority to continue as the task owner.

## 7.3 Compare them

| Property                                | SQS Visibility Timeout        | DB Lease                          |
| --------------------------------------- | ----------------------------- | --------------------------------- |
| Maintained by                           | AWS SQS                       | Our application                   |
| Purpose                                 | Hide a message temporarily    | Establish task ownership          |
| Prevents delivery to another worker     | Normally yes, until expiry    | No, not at queue level            |
| Prevents concurrent business processing | Not reliably                  | Through atomic claiming           |
| Renewal                                 | `ChangeMessageVisibility`     | Conditional DB update             |
| Expiration                              | Message becomes visible again | Task becomes eligible for reclaim |
| Authority                               | Broker-level delivery control | Application-level ownership       |

Think about it this way:

```
SQS Visibility Timeout:
    "Don't give this message to somebody else yet."

DB Lease:
    "Worker 1 is currently authorized to process task-101."
```

We need both.

## 7.4 How should we configure them?

Let's use an example:

```
DB Lease Duration:       90 seconds

SQS Visibility Timeout:  120 seconds

Heartbeat Interval:      30 seconds
```

The worker renews both every 30 seconds.

SQSDatabaseWorkerSQSDatabaseWorker#chatgpt-mermaid-\_r_6vk\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_6vk\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_6vk\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_6vk\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_6vk\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6vk\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_6vk\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_6vk\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_6vk\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_6vk\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_6vk\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_6vk\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6vk\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6vk\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_6vk\_ p{margin:0;}#chatgpt-mermaid-\_r_6vk\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_6vk\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6vk\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6vk\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_6vk\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6vk\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_6vk\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6vk\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6vk\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6vk\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_6vk\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6vk\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6vk\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_6vk\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_6vk\_ .labelText,#chatgpt-mermaid-\_r_6vk\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_6vk\_ .loopText,#chatgpt-mermaid-\_r_6vk\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_6vk\_ .sectionTitle,#chatgpt-mermaid-\_r_6vk\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_6vk\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_6vk\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_6vk\_ .noteText,#chatgpt-mermaid-\_r_6vk\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_6vk\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_6vk\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_6vk\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_6vk\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_6vk\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_6vk\_ .actor-man circle,#chatgpt-mermaid-\_r_6vk\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_6vk\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_6vk\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_6vk\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_6vk\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_6vk\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_6vk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6vk\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_6vk\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_6vk\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_6vk\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6vk\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_6vk\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_6vk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6vk\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_6vk\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_6vk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6vk\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_6vk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6vk\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}loop[Every 30 seconds]Claim taskLease acquired (90 sec)Change visibility (120 sec)Visibility extendedRenew DB lease (90 sec)Lease renewedChange visibility (120 sec)Visibility extendedMark task SUCCEEDEDCompletion committedDeleteMessageMessage deleted

These numbers are illustrative, not universal.

The important relationship is that:

- Heartbeats should happen comfortably before expiration.
- The visibility timeout should be long enough to avoid premature redelivery.
- The DB lease must be renewed reliably.
- The application must tolerate duplicate delivery even with these settings.

One more important SQS constraint: the visibility timeout cannot exceed 12 hours from the original `ReceiveMessage` request. Extending it does not restart that 12-hour maximum. For work longer than that, use smaller independently recoverable jobs or a workflow orchestrator such as Step Functions.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Queue Service+1



# 8. Fifth fundamental concept: Heartbeat mechanism

Now we know that the worker owns a task for a limited period.

But what if processing takes longer than the lease?

Example:

```
Task:
    Generate 3 hours of video content.

Worker:
    worker-1

Initial DB lease:
    90 seconds
```

After 90 seconds, the lease expires.

Another worker might claim the task while Worker 1 is still processing.

We clearly don't want that.

We need the worker to continuously prove that it is alive and extend its ownership.

This is called a Heartbeat Mechanism.

## 8.1 How heartbeat works

Imagine:

```
Initial lease:
    10:00:00 to 10:01:30

Heartbeat at 10:00:30:
    Extend to 10:02:00

Heartbeat at 10:01:00:
    Extend to 10:02:30

Heartbeat at 10:01:30:
    Extend to 10:03:00
```

The worker continuously renews its lease.

Notice that the expiration time is calculated relative to the current time, not the original start time.

The worker does not need to know how long processing will take.

It just needs to keep renewing while it is healthy and authorized to continue.

## 8.2 How do we implement database heartbeat?

The worker executes:

```
UPDATE tasks
SET
    heartbeat_at = NOW(),

    lease_expires_at = NOW() + INTERVAL '90 seconds',

    updated_at = NOW()

WHERE id = :task_id

AND status = 'RUNNING'

AND lease_owner = :worker_id

AND lease_token = :lease_token

AND lease_expires_at > NOW();
```

The important part is the `WHERE` condition.

We verify:

1. The task is still running.
2. This worker still owns it.
3. This is the same ownership generation.
4. The existing lease hasn't expired.

If one row is updated:

```
Heartbeat successful.
Worker continues.
```

If zero rows are updated:

```
Heartbeat rejected.
Worker has lost ownership.
```

The worker must stop processing as safely and quickly as possible.

Never assume that a worker still owns a task just because it successfully claimed it five minutes ago.

Ownership must be continuously validated.

For PostgreSQL, I would use `clock_timestamp()` rather than `NOW()` for lease calculations if operations could run inside longer transactions, because `NOW()` represents the transaction start time.

## 8.3 How do we extend SQS visibility?

The worker must also extend the SQS visibility timeout.

It does this using:

```
ChangeMessageVisibilityRequest request =
    ChangeMessageVisibilityRequest.builder()
        .queueUrl(queueUrl)
        .receiptHandle(receiptHandle)
        .visibilityTimeout(120)
        .build();

sqsClient.changeMessageVisibility(request);
```

This says:

```
Keep this particular SQS message invisible
for another 120 seconds.
```

Important: we use the receipt handle, not the message ID.

Every time SQS delivers a message, it provides a receipt handle associated with that particular receive operation. A subsequent receive generates a new handle. Use the most recently received handle for changing visibility or deleting the message.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Queue Service+1



## 8.4 Should DB heartbeat and SQS heartbeat happen together?

Yes, logically they belong to the same heartbeat cycle, but they are two independent network operations.

For example:

```
Every 30 seconds:

1. Renew DB lease.
2. Extend SQS visibility timeout.
```

But there is no atomic transaction covering both.

Consider these failure scenarios.

Scenario A: Database heartbeat succeeds, SQS extension fails.

```
DB lease:
    Successfully renewed.

SQS visibility:
    Extension failed.
```

Another worker might receive the message once it becomes visible.

But when it attempts to claim the task, the database will reject it because Worker 1 still has an active lease.

So the duplicate worker must not start processing.

Worker 1 should retry the SQS visibility extension and monitor the failure.

Scenario B: SQS extension succeeds, database heartbeat fails.

```
SQS visibility:
    Successfully extended.

DB lease:
    Renewal rejected.
```

Worker 1 no longer has confirmed processing ownership.

It must stop processing.

Even though the message is invisible, the database is the authority. Another worker or the recovery mechanism can eventually reclaim the task.

Scenario C: Both fail.

The worker cannot confirm that it still owns the task.

It must stop accepting ownership-dependent work and allow recovery.

The important rule:

> Extending SQS visibility does not renew the database lease. Renewing the database lease does not extend SQS visibility.

## 8.5 Heartbeat versus progress

There is another subtle distinction.

A heartbeat means:

> I am alive and I still own this task.

Progress means:

> I have actually advanced the task.

These are not the same.

Consider:

```
Task:
    Generate a large report.

Worker:
    Alive.

Heartbeat:
    Updated every 30 seconds.

Processing:
    Stuck forever on one database query.
```

The worker is alive, but it is making no progress.

Therefore, I recommend tracking both:

```
heartbeat_at:
    Last worker heartbeat.

progress_at:
    Last meaningful progress update.
```

For example:

```
{
  "stage": "GENERATING_PDF",
  "completed_pages": 75,
  "total_pages": 100
}
```

This allows us to differentiate:

| Situation                                     | Interpretation                         |
| --------------------------------------------- | -------------------------------------- |
| Recent heartbeat, recent progress             | Healthy worker                         |
| Recent heartbeat, no progress for a long time | Possibly stuck                         |
| Old heartbeat, no progress                    | Worker probably crashed or disappeared |
| Recent heartbeat, lease renewal rejected      | Ownership has been lost                |

Also, keep heartbeat execution independent of potentially blocking processing work.

For example, if a CPU-intensive operation blocks the worker's only event loop, a heartbeat scheduled on that same event loop might never execute.

A separate heartbeat thread, scheduler or independently responsive execution mechanism is preferable.

# 9. Sixth fundamental concept: Fencing tokens

Now we arrive at one of the most important distributed-systems concepts.

A database lease alone is not sufficient.

Let me explain with a real scenario.

## 9.1 The split-brain problem

Suppose Worker 1 claims a task.

```
Task: task-101

Worker: worker-1

Lease expires at:
    10:01:30
```

Worker 1 starts processing.

Suddenly, its network connection to the database becomes unavailable.

Worker 1 is not necessarily dead.

It might still be executing the business logic.

But it cannot renew its lease.

At 10:01:30, its lease expires.

Worker 2 discovers the expired lease and claims the task.

```
Worker 2:
    Claims task-101.
    Starts processing.
```

Now imagine Worker 1's network connection recovers.

Worker 1 resumes processing.

We now have:

```
Worker 1:
    Still executing old attempt.

Worker 2:
    Executing new attempt.
```

Both workers are alive.

Both might try to update the same records.

This is called a split-brain or stale-owner problem.

A lease expiration does not magically stop the previous worker's process.

This is a fundamental property of distributed systems.

## 9.2 Solution: Fencing token

Remember this database field?

```
lease_token BIGINT NOT NULL DEFAULT 0
```

Every time a new worker successfully claims a task, we increment it.

Imagine:

```
Initial:
    lease_token = 40
```

Worker 1 claims the task:

```
Worker 1:
    lease_token = 41
```

Worker 1's lease expires.

Worker 2 claims it:

```
Worker 2:
    lease_token = 42
```

Worker 1 resumes.

It still carries:

```
lease_token = 41
```

Worker 2 carries:

```
lease_token = 42
```

We use the token to reject operations from old owners.

For example, Worker 1 tries:

```
UPDATE tasks
SET progress = '{"stage":"UPLOADING"}'
WHERE id = 'task-101'
AND status = 'RUNNING'
AND lease_owner = 'worker-1'
AND lease_token = 41;
```

But the current token is 42.

No row is updated.

Worker 1 is rejected.

Worker 2 is the current owner.

## 9.3 Why is a fencing token better than just a worker ID?

Because the same worker instance might acquire the task more than once.

Consider:

```
Worker 1 claims task.
Token = 41.

Worker 1 loses lease.

Worker 2 claims task.
Token = 42.

Worker 2 fails.

Worker 1 eventually claims task again.
Token = 43.
```

The worker ID is again `worker-1`, but its old execution still has token 41.

The token differentiates separate ownership generations.

It is monotonically increasing for each task.

An older execution can never have a higher fencing token than a newer execution.

## 9.4 Fencing must protect external side effects too

Here is an important trap.

Suppose Worker 1 calls an external service:

```
Worker 1:
    Upload video to external processing service.

Worker 1 loses lease.

Worker 2:
    Takes over.

Worker 2:
    Uploads video to external processing service.
```

Even though our database rejects Worker 1's stale updates, the external service might still accept its request.

The database fencing token cannot automatically protect another system.

We need to propagate the ownership generation where possible, or use operation-level idempotency.

For systems that support fencing, the downstream system remembers the highest accepted token and rejects lower tokens.

For systems that don't support fencing, use stable idempotency keys, conditional operations, or another resource-specific concurrency mechanism.

This distinction matters enormously.

A lease controls ownership. A fencing token protects against stale ownership. Idempotency protects against repeated business operations.

They solve related but different problems.

# 10. Seventh fundamental concept: Retry management

Now let's discuss retries properly.

Imagine:

```
Task:
    Generate invoice PDF.

Attempt 1:
    External storage unavailable.

Attempt 2:
    Database connection timeout.

Attempt 3:
    PDF generation successful.
```

We need a reliable retry strategy.

But first, there are two different types of retries.

## 10.1 Queue delivery retry versus task execution retry

This distinction is very important.

Queue delivery retry

SQS delivers the same message again.

For example:

```
Worker 1 receives message.
Worker 1 crashes.
Visibility timeout expires.
Worker 2 receives message again.
```

SQS tracks approximate message receive counts.

Task execution retry

Our application decides that the business operation failed and should be executed again.

For example:

```
Attempt 1 fails.
Application decides to retry after 30 seconds.
Attempt 2 starts.
```

These counters are not equivalent.

A message might be delivered multiple times without actually starting business processing.

For example:

```
SQS Receive 1:
    Worker crashes before claiming task.

SQS Receive 2:
    Duplicate message arrives while another worker owns task.

SQS Receive 3:
    Another duplicate delivery.

SQS Receive 4:
    Finally, a worker starts actual processing.
```

SQS receive count could be four, while the application's execution attempt count is one.

Therefore:

- Use SQS receive count for queue delivery diagnostics and redrive protection.
- Use the database `attempt_count` for actual application execution attempts.

Do not treat them as interchangeable.

## 10.2 Which retry strategy should we use?

There are two reasonable approaches.

### Approach A: Let SQS handle retries

When processing fails:

```
Worker does not delete the message.

Visibility timeout expires.

SQS makes it available again.

Another worker receives the message.
```

After a configured number of receives, SQS moves the message to a Dead Letter Queue (DLQ).

This is straightforward, but the retry schedule is largely controlled by queue visibility settings.

It is suitable for relatively simple consumers.

### Approach B: Application-managed retries

This is my preferred approach for long-running tasks with a proper task database.

When processing fails:

1. Update the attempt record.
2. Calculate the retry time.
3. Change the task state to `RETRY_WAIT`.
4. Clear the current worker ownership.
5. Commit the database transaction.
6. Delete the current SQS message.
7. Let a scheduler or dispatcher publish another message when the retry becomes due.

Example:

```
Task ID:
    task-101

Attempt:
    1

Result:
    FAILED

Error:
    External API timeout

Retry policy:
    Exponential backoff

Next retry:
    30 seconds later

Task status:
    RETRY_WAIT
```

Why do I prefer this?

Because our application can control:

- Maximum attempts.
- Retry timing.
- Exponential backoff.
- Error classification.
- Tenant-specific retry policies.
- Whether a task should be retried at all.
- When to mark a task permanently failed.

For this design, the database remains authoritative for retry scheduling.

The outbox pattern is also used when publishing the next retry event.

## 10.3 Exponential backoff

We shouldn't retry every second.

Suppose an external service is down.

Thousands of workers continuously retrying every second might make the outage worse.

Instead, progressively increase the delay.

A common formula is:

\\[ delay = \min(maxDelay,\ baseDelay \times 2^{attempt-1}) \\]

For example:

```
Base delay = 10 seconds
Maximum delay = 5 minutes
```

| Attempt | Retry delay |
| ------- | ----------- |
| 1       | 10 seconds  |
| 2       | 20 seconds  |
| 3       | 40 seconds  |
| 4       | 80 seconds  |
| 5       | 160 seconds |

In production, add jitter.

Jitter introduces randomness so that thousands of failed tasks don't all retry at exactly the same time.

A common full-jitter strategy is:

\\[ delay = random(0,\min(maxDelay,\ baseDelay \times 2^{attempt-1})) \\]

This spreads retries over time.

## 10.4 When should we retry?

Not every error is retryable.

Consider:

| Error                            | Retry?                                |
| -------------------------------- | ------------------------------------- |
| Network timeout                  | Yes                                   |
| External API HTTP 503            | Yes                                   |
| Database temporarily unavailable | Yes                                   |
| Rate limit / HTTP 429            | Usually, respecting `Retry-After`     |
| Invalid input                    | No                                    |
| Unsupported file format          | No                                    |
| Permission denied                | Usually no; investigate configuration |
| Resource temporarily unavailable | Usually yes                           |
| Malformed message                | No; quarantine or DLQ                 |

A good worker distinguishes:

```
Retryable error:
    Try again later.

Permanent error:
    Mark task FAILED.

Unknown error:
    Apply a conservative retry policy,
    log enough context and eventually stop.
```

## 10.5 How should we update the database after a failed attempt?

Suppose Worker 1 fails on attempt 2.

We should perform the following operations in one database transaction:

```
BEGIN;

UPDATE task_attempts
SET
    status = 'FAILED',
    finished_at = NOW(),
    error_code = 'EXTERNAL_API_TIMEOUT',
    error_message = 'External service timed out'
WHERE task_id = :task_id
  AND attempt_number = :attempt_number
  AND lease_token = :lease_token;

UPDATE tasks
SET
    status = 'RETRY_WAIT',

    next_attempt_at = :next_attempt_at,

    lease_owner = NULL,
    lease_expires_at = NULL,

    last_error_code = 'EXTERNAL_API_TIMEOUT',
    last_error_message = 'External service timed out',

    updated_at = NOW()

WHERE id = :task_id
  AND status = 'RUNNING'
  AND lease_owner = :worker_id
  AND lease_token = :lease_token;

COMMIT;
```

In real code, verify that both ownership conditions succeeded. If they didn't, the worker has lost authority and must not overwrite the new owner's state.

The attempt count should not be incremented here. It was already incremented when the attempt was claimed.

If attempts are exhausted:

```
status = FAILED
```

Otherwise:

```
status = RETRY_WAIT
```

A scheduler picks up the task when `next_attempt_at` arrives.

One more subtlety: if a worker crashes before recording a failure, the recovery mechanism must handle its abandoned attempt. We cannot rely on every failure being reported by the worker itself.

# 11. Eighth fundamental concept: Recovering tasks when workers crash

This is where the entire design comes together.

Suppose:

```
10:00:00
    Worker 1 claims task-101.

10:00:10
    Worker 1 starts video processing.

10:00:30
    Worker 1 sends heartbeat.

10:00:45
    Worker 1's machine crashes.

After 10:00:45
    No more heartbeats.
```

The database still says:

```
task-101

status = RUNNING

lease_owner = worker-1

lease_expires_at = 10:02:00
```

The database does not automatically know that Worker 1 has crashed.

It only knows that the lease will expire at 10:02:00.

We need some component to discover expired leases.

This component is called a Lease Reaper, Task Recovery Scheduler, or simply a recovery job.

## 11.1 What does the recovery scheduler do?

It periodically searches for tasks like:

```
SELECT *
FROM tasks
WHERE status = 'RUNNING'
AND lease_expires_at < clock_timestamp();
```

It identifies tasks whose workers haven't renewed their leases.

Then it recovers them.

The recovery logic is:

```
Find expired task.

        |
        v

Is retry budget available?

    /           \
  Yes            No
   |              |
   v              v
RETRY_WAIT       FAILED
   |
   v
Calculate next_attempt_at
   |
   v
Publish a new queue event when due
```

The task attempt that lost ownership should be marked `ABANDONED`.

## 11.2 Why do we need a recovery scheduler when SQS already retries?

Good question.

SQS visibility timeout normally makes an unacknowledged message available again.

So why do we need another mechanism?

Consider what happens if:

1. Worker 1 receives a message.
2. Worker 1 claims the database task.
3. Worker 1 extends SQS visibility.
4. Worker 1 crashes.
5. Another worker receives a duplicate copy of the message.
6. The duplicate worker sees an active DB lease.
7. The duplicate worker deletes its copy because the original worker appears to own the task.
8. The original SQS message is somehow deleted, lost through a bug or otherwise no longer available for recovery.
9. The DB lease eventually expires.

Now the task is stuck in `RUNNING`, and no queue message remains.

A recovery scheduler solves this problem.

It doesn't matter whether the original SQS message is available.

The database still contains the unfinished task.

The recovery scheduler can publish another event.

This is why the database should be the durable task ledger and the queue should be treated as a delivery mechanism.

## 11.3 How should we implement the recovery scheduler?

We might run several recovery scheduler instances.

We don't want every scheduler processing the same expired task.

We can use PostgreSQL row locking with `SKIP LOCKED`.

Illustrative pattern:

```
BEGIN;

SELECT id, lease_owner, lease_token
FROM tasks
WHERE status = 'RUNNING'
AND lease_expires_at < clock_timestamp()
ORDER BY lease_expires_at
LIMIT 100
FOR UPDATE SKIP LOCKED;
```

The scheduler then updates the selected records and writes corresponding outbox events in the same transaction.

The actual implementation should also:

- Mark the corresponding old attempt `ABANDONED`.
- Check the maximum attempt count.
- Move the task to `RETRY_WAIT` or `FAILED`.
- Clear expired ownership.
- Calculate the retry schedule.
- Create a uniquely identifiable outbox event.

After committing, the outbox publisher handles SQS publication.

Multiple recovery schedulers can work concurrently because they lock different rows.

### A critical implementation detail

Do not perform SQS network calls while holding database row locks.

The database transaction should only:

1. Select eligible records.
2. Change their state.
3. Record recovery events in the outbox.
4. Commit.

The publisher handles network operations separately.

This keeps transactions short and reduces database contention.

## 11.4 How frequently should the recovery scheduler run?

It depends on your recovery objectives.

For example:

```
Lease duration:
    90 seconds

Recovery scheduler interval:
    10 seconds
```

A crashed worker might be detected approximately 90–100 seconds after its last successful lease renewal, assuming the recovery scheduler is operating normally.

The detection delay is not guaranteed to be exactly 90 seconds because scheduling and database polling also take time.

The lease duration is therefore a trade-off:

| Short lease                          | Long lease                                  |
| ------------------------------------ | ------------------------------------------- |
| Faster crash recovery                | Slower crash recovery                       |
| More heartbeat requests              | Fewer heartbeat requests                    |
| More sensitive to network delays     | More tolerant of temporary delays           |
| More risk of accidental lease expiry | Slower recovery from genuinely dead workers |

Choose it based on processing characteristics and failure-detection requirements.

# 12. Ninth fundamental concept: Exactly-once processing is not what we should promise

Let's discuss a very important distributed-systems reality.

Suppose Worker 1 processes a payment.

The payment provider successfully charges the customer.

But immediately afterward, Worker 1 crashes before updating our database.

Our database says:

```
Task status = RUNNING
```

But the payment was actually successful.

The recovery scheduler eventually allows Worker 2 to retry the task.

Worker 2 calls the payment provider again.

What happens?

The customer could be charged twice.

This is a serious problem.

Our lease and fencing mechanisms cannot solve this on their own.

We need idempotency.

## 12.1 What is idempotency?

An operation is idempotent if executing it multiple times has the same intended business effect as executing it once.

Consider:

```
Set order status to PAID.
```

Executing this repeatedly doesn't create multiple payments.

But:

```
Charge customer ₹1,000.
```

Executing it twice might charge the customer twice.

The second operation needs additional protection.

## 12.2 Use a stable idempotency key

Every logical task should have a stable identifier.

For example:

```
task_id = task-101
```

For the payment operation, we pass:

```
POST /payment-provider/charges
Idempotency-Key: task-101-payment
```

Suppose:

```
Worker 1:
    Sends payment request.

Payment provider:
    Successfully charges customer.

Worker 1:
    Crashes before saving result.

Worker 2:
    Retries same request.

Payment provider:
    Recognizes the idempotency key.
    Returns the original result instead of charging again.
```

Now the retry is safe.

The external service must support this feature for the protection to work.

The idempotency key must remain the same for retries of the same logical operation.

Generating a new key for every attempt defeats the purpose.

## 12.3 Idempotency at different layers

There are actually several different idempotency problems.

| Layer              | Problem                               | Protection                                 |
| ------------------ | ------------------------------------- | ------------------------------------------ |
| API layer          | Client submits the same request twice | API idempotency key                        |
| Outbox layer       | Publisher sends the same event twice  | Stable event ID and consumer deduplication |
| Queue layer        | SQS delivers a message twice          | Task ownership and terminal-state checks   |
| Task layer         | Same task is attempted multiple times | Task-level idempotency                     |
| Business operation | Payment or external request repeats   | External API idempotency key               |
| Database layer     | Worker inserts the same output twice  | Unique constraints and conditional writes  |

Let's understand one example.

A user sends:

```
POST /reports/export
Idempotency-Key: export-request-123
```

The API might create:

```
task-101
```

The task message gets published three times.

Three workers might eventually receive copies of that message.

However:

- The database claim prevents simultaneous legitimate ownership.
- The task state prevents processing already completed tasks.
- The output can use a deterministic S3 object key.
- A unique constraint prevents duplicate output records.
- The API idempotency key prevents the original user request from creating multiple logical export tasks.

Each layer handles a different kind of duplication.

## 12.4 Can we guarantee exactly once?

Not across arbitrary independent systems.

Consider:

```
1. Update external payment provider.
2. Update our PostgreSQL database.
3. Delete SQS message.
```

These three operations cannot ordinarily be committed as one atomic transaction.

The payment provider might succeed while our DB update fails.

Our DB update might succeed while SQS deletion fails.

Therefore, a realistic and robust target is:

At-least-once delivery + durable ownership + idempotent business effects + recoverable state transitions.

This can provide effectively-once business outcomes when every relevant side effect is properly protected.

Do not promise exactly-once execution just because the queue is FIFO or the worker uses a distributed lock.

# 13. Tenth fundamental concept: Correct completion order

Let's say a worker has successfully generated a report.

It has:

```
S3 object:
    reports/task-101.pdf

Database:
    task-101 = RUNNING

SQS:
    message still exists
```

What should happen next?

The correct order is:

```
1. Complete the business processing.

2. Persist the output.

3. Update task status to SUCCEEDED.

4. Commit the database transaction.

5. Delete the SQS message.
```

For example:

```
BEGIN;

UPDATE tasks
SET
    status = 'SUCCEEDED',

    completed_at = clock_timestamp(),

    lease_owner = NULL,
    lease_expires_at = NULL,

    updated_at = clock_timestamp()

WHERE id = :task_id

AND status = 'RUNNING'

AND lease_owner = :worker_id

AND lease_token = :lease_token

AND lease_expires_at > clock_timestamp();

-- Verify exactly one row was updated.

UPDATE task_attempts
SET
    status = 'SUCCEEDED',
    finished_at = clock_timestamp()
WHERE task_id = :task_id
AND attempt_number = :attempt_number
AND lease_token = :lease_token
AND status = 'RUNNING';

COMMIT;
```

The completion transaction must verify ownership.

If the task update affects zero rows, the worker must not assume completion was accepted.

It might have lost its lease.

If the database commit succeeds, then:

```
sqsClient.deleteMessage(
    DeleteMessageRequest.builder()
        .queueUrl(queueUrl)
        .receiptHandle(receiptHandle)
        .build()
);
```

Delete the message.

## 13.1 Why must DB update happen before SQS deletion?

Consider the wrong order:

```
1. Delete SQS message.
2. Update DB to SUCCEEDED.
3. Worker crashes before DB update.
```

Now:

```
SQS:
    Message deleted.

DB:
    Task still RUNNING.
```

The queue cannot redeliver that deleted message.

Recovery now depends entirely on the database recovery scheduler.

The correct order avoids unnecessarily losing the normal delivery retry mechanism.

## 13.2 What happens if the DB update succeeds but SQS deletion fails?

This is a very common failure.

```
1. Processing succeeds.
2. Database marks task SUCCEEDED.
3. Worker tries deleting SQS message.
4. SQS request fails.
```

Eventually, the message might be delivered again.

Another worker receives it and checks the database.

It finds:

```
status = SUCCEEDED
```

The worker does not process the task again.

It simply deletes the duplicate message using its current receipt handle.

This is exactly why the database status must be checked before performing business work.

# 14. Complete failure scenarios

Let's walk through the important real-world situations.

These examples are what you should be able to explain confidently in a system-design interview.

## Scenario A: API crashes after saving the task

```
API creates task.
API commits task + outbox event.
API crashes before publishing to SQS.
```

Result:

```
Task remains in DB.
Outbox event remains PENDING.
```

Recovery:

Outbox publisher picks it up and publishes it.

Protection: Transactional Outbox.

## Scenario B: Outbox publisher publishes twice

```
Publisher sends message.
SQS accepts it.
Publisher crashes before marking event published.
Publisher retries.
```

Result:

Two messages may exist.

Recovery:

Workers consult the database and claim the task atomically.

Protection: Database ownership and idempotency.

## Scenario C: Two workers receive the same task

```
Worker 1 receives task-101.
Worker 2 receives duplicate task-101.
```

Worker 1 claims:

```
lease_owner = worker-1
lease_token = 41
```

Worker 2 tries to claim.

The database rejects it because the lease is active.

Protection: Conditional database update.

## Scenario D: Worker crashes during processing

```
Worker 1 claims task.
Worker 1 starts processing.
Worker 1 crashes.
No more heartbeat.
```

The DB lease eventually expires.

SQS visibility also eventually expires, unless the message has already been acknowledged or otherwise removed.

Recovery scheduler discovers the expired task and schedules another attempt.

Protection: Lease expiration + recovery scheduler + retry policy.

## Scenario E: Worker is slow, but still healthy

```
Worker 1 processes for 40 minutes.

Lease duration = 90 seconds.

Heartbeat every 30 seconds.
```

The worker continuously renews:

```
DB lease.
SQS visibility timeout.
```

Other workers don't normally receive that message, and the DB doesn't permit them to claim the task.

Protection: Heartbeats.

## Scenario F: Worker 1 becomes stale and Worker 2 takes over

```
Worker 1:
    Token = 41.

Worker 1 loses DB connection.

Lease expires.

Worker 2:
    Token = 42.

Worker 1 resumes.
```

Worker 1's operations with token 41 are rejected.

Protection: Fencing token.

## Scenario G: External operation succeeds but worker crashes before DB completion

```
Worker 1:
    Calls payment provider.

Payment provider:
    Charges customer.

Worker 1:
    Crashes before saving SUCCESS.
```

Worker 2 eventually retries.

It uses the same payment idempotency key.

The payment provider returns the existing result.

Protection: External idempotency.

## Scenario H: Worker finishes, but SQS deletion fails

```
Worker:
    Completes processing.

Database:
    SUCCEEDED.

SQS:
    DeleteMessage fails.
```

Message is delivered again.

The next worker checks the database and sees `SUCCEEDED`.

It doesn't repeat processing.

Protection: Durable terminal state + idempotent consumer.

## Scenario I: Worker keeps heartbeating but does no useful work

```
Worker:
    Alive.

Heartbeat:
    Continues.

Processing:
    Stuck.
```

Our lease mechanism thinks the worker is alive, but the actual task is stalled.

Monitor `progress_at` separately and implement task-specific execution deadlines.

Protection: Progress monitoring + execution timeout + stuck-task detection.

## Scenario J: Worker starts a task but crashes before the initial DB claim completes

There are two possibilities:

Database transaction never committed:

The task remains eligible for another worker to claim.

Database transaction committed but the worker didn't receive the commit response:

The worker might not know whether it acquired ownership.

It must read the task state and establish whether its owner ID and fencing token were committed before performing any business work.

This is another reason why generating an ownership token and returning it from the claim transaction is useful.

# 15. What happens when a task keeps failing?

We should never retry forever.

Imagine:

```
Task:
    Process an invalid video.

Every attempt:
    Fails with unsupported codec.
```

After five attempts:

```
attempt_count = 5
max_attempts = 5
status = FAILED
```

The task becomes terminal.

We should retain:

- Last error.
- All previous attempt records.
- Worker information.
- Failure timestamps.
- Relevant diagnostic information.

The message handling mechanism must also stop generating new retries for that task.

## Dead Letter Queue

SQS provides Dead Letter Queues for messages that can't be successfully consumed after a configured number of receives.

For example:

```
Main Queue:
    video-processing-queue

DLQ:
    video-processing-dlq

maxReceiveCount:
    5
```

A message that exceeds the configured receive threshold is moved to the DLQ.

The DLQ is useful for:

- Malformed messages.
- Poison messages.
- Unsupported message versions.
- Repeated infrastructure-level processing failures.
- Diagnosing unexpected consumer errors.

There is an important difference:

A task marked `FAILED` in our database is not automatically the same thing as a message in the DLQ.

In our preferred application-managed retry design, we persist failures and schedule retries in the database. We acknowledge the current message once the retry decision is durably recorded.

Therefore, the SQS receive count won't represent the total number of business execution attempts.

Use:

- Database retry limits for logical tasks.
- SQS DLQ for messages that cannot be handled by the queue consumer.

Also configure DLQ retention carefully. For standard queues, the original enqueue timestamp is retained when a message moves to the DLQ, so AWS recommends setting the DLQ retention period longer than the source queue's retention period.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Queue Service+1



# 16. What does a real worker look like?

Let's combine everything into one worker lifecycle.

Imagine a Java worker application.

It contains these components:

```
WorkerApplication
    |
    +-- SqsMessagePoller
    |
    +-- TaskClaimService
    |
    +-- TaskProcessor
    |
    +-- LeaseManager
    |
    +-- HeartbeatScheduler
    |
    +-- TaskCompletionService
    |
    +-- RetryManager
```

### Worker processing pseudocode

```
void handleMessage(SqsMessage message) {

    // 1. Parse and validate the message.
    TaskMessage taskMessage = parse(message);

    String taskId = taskMessage.taskId();

    // 2. Claim the task using an atomic DB update.
    Optional<TaskLease> lease =
        taskClaimService.tryClaim(taskId, workerId);

    if (lease.isEmpty()) {

        // Already completed?
        // Another active worker?
        // Not yet eligible?
        //
        // Handle according to the current DB state.
        handleUnclaimedMessage(message, taskId);

        return;
    }

    TaskLease currentLease = lease.get();

    // 3. Start heartbeat mechanism.
    HeartbeatHandle heartbeat =
        heartbeatScheduler.start(
            currentLease,
            message.receiptHandle()
        );

    try {

        // 4. Execute business logic.
        TaskResult result =
            taskProcessor.process(
                taskId,
                currentLease
            );

        // 5. Verify that ownership is still valid.
        heartbeat.assertLeaseValid();

        // 6. Mark task completed in DB.
        taskCompletionService.complete(
            taskId,
            currentLease,
            result
        );

        // 7. Delete message only after DB completion.
        sqsClient.deleteMessage(
            message.receiptHandle()
        );

    } catch (RetryableException ex) {

        // Persist retry decision before acknowledging.
        retryManager.scheduleRetry(
            taskId,
            currentLease,
            ex
        );

        sqsClient.deleteMessage(
            message.receiptHandle()
        );

    } catch (PermanentException ex) {

        taskCompletionService.fail(
            taskId,
            currentLease,
            ex
        );

        sqsClient.deleteMessage(
            message.receiptHandle()
        );

    } finally {

        heartbeat.stop();
    }
}
```

This is conceptual pseudocode, not a complete production implementation.

There are some important implementation details:

- `tryClaim` must be an atomic conditional update.
- A retry should only be acknowledged after its DB transition and retry scheduling are committed.
- `complete` and `fail` must verify the ownership token.
- If heartbeat detects lost ownership, the worker must stop ownership-dependent operations.
- If the database is unavailable and the outcome is uncertain, do not blindly delete the message.
- `handleUnclaimedMessage` must distinguish an active owner from a terminal task and coordinate with recovery.
- The worker should retain the correct receipt handle for the particular delivery.
- Unexpected failures must not cause the worker to acknowledge a task whose durable state is unknown.

Also, the heartbeat scheduler needs its own lifecycle management. It should not continue extending the lease after the task has finished.

# 17. The complete production architecture

Let's now assemble everything we have learned.

\#chatgpt-mermaid-\_r_7bm\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_7bm\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_7bm\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_7bm\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_7bm\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7bm\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_7bm\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_7bm\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_7bm\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_7bm\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_7bm\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_7bm\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7bm\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7bm\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_7bm\_ p{margin:0;}#chatgpt-mermaid-\_r_7bm\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7bm\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7bm\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7bm\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_7bm\_ .label text,#chatgpt-mermaid-\_r_7bm\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7bm\_ .node rect,#chatgpt-mermaid-\_r_7bm\_ .node circle,#chatgpt-mermaid-\_r_7bm\_ .node ellipse,#chatgpt-mermaid-\_r_7bm\_ .node polygon,#chatgpt-mermaid-\_r_7bm\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_7bm\_ .rough-node .label text,#chatgpt-mermaid-\_r_7bm\_ .node .label text,#chatgpt-mermaid-\_r_7bm\_ .image-shape .label,#chatgpt-mermaid-\_r_7bm\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_7bm\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_7bm\_ .rough-node .label,#chatgpt-mermaid-\_r_7bm\_ .node .label,#chatgpt-mermaid-\_r_7bm\_ .image-shape .label,#chatgpt-mermaid-\_r_7bm\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_7bm\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_7bm\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7bm\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7bm\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_7bm\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_7bm\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_7bm\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_7bm\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_7bm\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_7bm\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_7bm\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7bm\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7bm\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_7bm\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7bm\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_7bm\_ .icon-shape,#chatgpt-mermaid-\_r_7bm\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_7bm\_ .icon-shape p,#chatgpt-mermaid-\_r_7bm\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_7bm\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_7bm\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_7bm\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_7bm\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_7bm\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_7bm\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_7bm\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_7bm\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_7bm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7bm\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_7bm\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_7bm\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_7bm\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7bm\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_7bm\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_7bm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7bm\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_7bm\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_7bm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7bm\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_7bm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7bm\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_7bm\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_7bm\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_7bm\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_7bm\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_7bm\_ .node rect,#chatgpt-mermaid-\_r_7bm\_ .node circle,#chatgpt-mermaid-\_r_7bm\_ .node ellipse,#chatgpt-mermaid-\_r_7bm\_ .node polygon,#chatgpt-mermaid-\_r_7bm\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_7bm\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_7bm\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_7bm\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_7bm\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7bm\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}DatabasePostgreSQLTasks TableTask Attempts TableOutbox Events TableClientTask Creation APIOutbox PublisherSQS Main QueueWorker 1Worker 2Worker NAtomic Task ClaimBusiness ProcessingExternal Services / S3 / APIsHeartbeat ManagerLease Recovery SchedulerRetry SchedulerDead Letter QueuePoison messages

Let's understand the responsibilities of each component.

| Component                | Responsibility                                               |
| ------------------------ | ------------------------------------------------------------ |
| Task Creation API        | Creates task records and outbox events.                      |
| PostgreSQL               | Maintains authoritative task state and ownership.            |
| Outbox Publisher         | Reliably publishes task messages to SQS.                     |
| SQS                      | Delivers messages to workers.                                |
| Worker                   | Executes business logic.                                     |
| Task Claim Service       | Atomically establishes task ownership.                       |
| Heartbeat Manager        | Renews DB lease and SQS visibility.                          |
| Retry Scheduler          | Makes failed tasks eligible for another attempt.             |
| Lease Recovery Scheduler | Recovers tasks whose workers disappeared.                    |
| DLQ                      | Isolates messages that cannot be processed normally.         |
| Monitoring               | Detects stalled tasks, failures and infrastructure problems. |

Notice that the worker does not directly own the entire reliability mechanism.

Reliability is distributed across the database, queue, worker and recovery components.

That is intentional.

# 18. Complete lifecycle: From request to success

Now let's follow one task through the entire system.

Assume:

```
Task ID:
    task-101

Task Type:
    VIDEO_PROCESSING

Maximum attempts:
    5

DB Lease Duration:
    90 seconds

SQS Visibility Timeout:
    120 seconds

Heartbeat Interval:
    30 seconds
```

### Phase 1: Task creation

The API receives:

```
POST /videos/123/process
```

It creates the task and outbox event in one transaction.

Database:

```
task-101

status = QUEUED
attempt_count = 0
lease_token = 0
```

Outbox:

```
event-101

status = PENDING
```

API responds:

```
202 Accepted
```

```
{
  "taskId": "task-101",
  "status": "QUEUED"
}
```

The client can check the task status separately.

### Phase 2: Message publication

The outbox publisher picks up `event-101`.

It publishes:

```
{
  "eventId": "event-101",
  "taskId": "task-101",
  "eventType": "TASK_CREATED",
  "schemaVersion": 1
}
```

SQS acknowledges the message.

The publisher marks the outbox event as published.

### Phase 3: Worker receives message

Worker 1 receives the message.

It requests an initial SQS visibility timeout of 120 seconds.

It then queries the task database.

```
Task status = QUEUED
```

Worker 1 attempts the atomic claim.

The database updates:

```
status = RUNNING

attempt_count = 1

lease_owner = worker-1

lease_token = 1

lease_expires_at = now + 90 seconds
```

Worker 1 has successfully acquired ownership.

### Phase 4: Worker begins execution

Worker 1 starts video processing.

It records:

```
{
  "stage": "EXTRACTING_METADATA",
  "percentage": 10
}
```

### Phase 5: Heartbeat

After 30 seconds:

```
Worker 1:
    Renews DB lease.

Database:
    Accepts heartbeat.

Worker 1:
    Extends SQS visibility.

SQS:
    Accepts extension.
```

The worker continues processing.

This repeats periodically.

### Phase 6: Processing failure

Suppose the video processor returns a temporary network error.

Worker 1 classifies it as retryable.

It updates:

```
attempt 1 = FAILED

task status = RETRY_WAIT

next_attempt_at = now + 30 seconds
```

Ownership is cleared.

The recovery is durable in the database before Worker 1 acknowledges the current SQS message.

A dispatcher publishes a new message when the retry becomes due.

### Phase 7: Second attempt

Worker 2 receives the new message.

It claims the task:

```
attempt_count = 2

lease_owner = worker-2

lease_token = 2

status = RUNNING
```

This is a new ownership generation.

### Phase 8: Successful processing

Worker 2 completes the video.

It uploads the processed files.

It updates the task and attempt records in a database transaction:

```
task-101

status = SUCCEEDED

attempt_count = 2

completed_at = now
```

It commits.

Then it deletes the SQS message.

The task is finished.

Final state:

```
{
  "taskId": "task-101",
  "status": "SUCCEEDED",
  "attemptCount": 2,
  "result": {
    "videoId": "video-123",
    "outputLocation": "s3://processed-videos/video-123/"
  }
}
```

That is the complete lifecycle.

# 19. Additional real-world scenarios you should account for

The architecture above is the foundation. Real production systems need a few additional mechanisms.

## 19.1 Graceful worker shutdown

Imagine your deployment platform is terminating Worker 1 during a new deployment.

Worker 1 is processing a task.

We should not immediately kill it without considering the task.

A graceful shutdown sequence should be:

```
1. Receive SIGTERM.

2. Stop accepting new SQS messages.

3. Allow in-flight tasks to finish within
   the configured shutdown grace period.

4. Continue heartbeats while legitimately processing.

5. If completion is not possible, stop safely.

6. Do not acknowledge unfinished messages.

7. Stop heartbeats when ownership is abandoned.

8. Allow the task to be recovered.
```

For long-running tasks, it may be better to save checkpoints and resume from the last completed stage.

The shutdown timeout is separate from the task execution timeout and lease duration.

## 19.2 Task execution timeout

A lease expiring does not mean a task has exceeded its allowed total execution time.

For example:

```
Task:
    Video processing.

Allowed execution time:
    2 hours.

DB lease:
    90 seconds.

Heartbeat:
    Every 30 seconds.
```

The worker can legitimately renew its lease for two hours.

However, if it keeps running beyond the two-hour task deadline, we might want to terminate it.

Therefore, track a separate field:

```
deadline_at
```

Distinguish:

| Timeout                      | Purpose                                             |
| ---------------------------- | --------------------------------------------------- |
| SQS visibility timeout       | Controls message visibility.                        |
| DB lease expiration          | Controls task ownership validity.                   |
| Task execution deadline      | Maximum permitted processing duration.              |
| External API timeout         | Maximum time spent waiting on a downstream request. |
| Worker shutdown grace period | Time available during termination.                  |

Don't use one timeout for all these responsibilities.

## 19.3 Cancellation

Suppose a user requests cancellation of a report that is currently processing.

We should not simply delete the SQS message.

It might already be running.

Instead:

```
RUNNING
    |
    v
CANCEL_REQUESTED
```

The worker periodically checks cancellation status or receives a cancellation signal.

If the work can be stopped safely:

```
CANCELLED
```

If the operation is already at an irreversible stage, cancellation may not be possible.

For example, a payment already submitted to a payment processor may need compensation or a refund instead of cancellation.

Cancellation is therefore cooperative, not magical.

## 19.4 Long-running tasks should be checkpointed

Suppose a data-export task needs 5 hours.

Instead of treating it as one completely indivisible operation:

```
Generate report:
    5 hours
```

Break it into recoverable stages:

```
1. Fetch data.
2. Generate first 10,000 records.
3. Generate next 10,000 records.
4. Generate remaining records.
5. Combine files.
6. Upload final result.
```

Persist progress after each meaningful stage.

If the worker crashes at stage 4, the next attempt can resume from stage 4 instead of repeating everything.

For very long workflows, consider AWS Step Functions rather than keeping a single SQS message in flight. The 12-hour SQS visibility limit is an important architectural boundary.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Queue Service+1



# 20. Standard SQS versus FIFO SQS

One more point worth understanding.

Which SQS queue should we use?

For most independent background jobs, I would start with Standard queues.

| Property               | Standard                       | FIFO                                            |
| ---------------------- | ------------------------------ | ----------------------------------------------- |
| Throughput             | Very high                      | More constrained by ordering and message groups |
| Ordering               | Best effort                    | Preserved within a message group                |
| Duplicate delivery     | Possible                       | Still possible in consumer failure scenarios    |
| Producer deduplication | No built-in FIFO deduplication | 5-minute deduplication window                   |
| Use case               | Independent background jobs    | Ordered processing requirements                 |

Example:

Standard queue

Suitable for:

```
Process video A
Process video B
Generate report C
Send notification D
```

These tasks are independent.

FIFO queue

Useful when you need:

```
Order 123:

1. ORDER_CREATED
2. PAYMENT_CONFIRMED
3. ORDER_SHIPPED
```

You can use `MessageGroupId` to preserve ordering within a group while processing different groups concurrently.

However, FIFO is not a replacement for:

- Database task claims.
- Heartbeats.
- Fencing tokens.
- Idempotency.
- Retry management.

SQS FIFO's producer deduplication window is five minutes. It does not make an entire business workflow exactly-once.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Queue Service+2



# 21. What should we monitor in production?

Having the mechanisms is not enough. We need to know when they are failing.

I would monitor the following.

| Metric                 | Why it matters                                                    |
| ---------------------- | ----------------------------------------------------------------- |
| Queue depth            | How much work is waiting?                                         |
| Age of oldest message  | Are tasks getting delayed?                                        |
| Tasks in `RUNNING`     | How much work is executing?                                       |
| Expired DB leases      | How many workers are losing ownership?                            |
| Heartbeat failures     | Are workers or network connections unhealthy?                     |
| No-progress duration   | Are tasks stuck?                                                  |
| Retry count            | Are downstream systems unreliable?                                |
| Task failure rate      | Is business processing failing?                                   |
| DLQ message count      | Are poison messages accumulating?                                 |
| Processing duration    | How long does each task type take?                                |
| Claim conflicts        | How frequently are workers seeing duplicate or concurrent claims? |
| Outbox publication lag | Are tasks being created but not dispatched?                       |

You should also log these fields in structured logs:

```
{
  "taskId": "task-101",
  "attemptNumber": 2,
  "workerId": "worker-2",
  "leaseToken": 2,
  "eventId": "event-101",
  "stage": "VIDEO_ENCODING",
  "correlationId": "request-123"
}
```

This makes it possible to reconstruct exactly what happened to a particular task.

I would also create alerts for:

- Tasks stuck in `RUNNING` beyond the expected duration.
- Large increases in expired leases.
- Outbox events remaining unpublished.
- Continuously growing queue age.
- DLQ messages appearing unexpectedly.
- High retry rates against a downstream dependency.

# 22. A few architectural decisions to make consciously

Before calling this a production-ready solution, consider these decisions.

| Question                                       | Recommended approach                    |
| ---------------------------------------------- | --------------------------------------- |
| Where is the source of truth?                  | Database                                |
| How are tasks published reliably?              | Transactional Outbox                    |
| Can the same message be delivered twice?       | Yes, design for it                      |
| How is concurrent ownership prevented?         | Atomic conditional DB claim             |
| How is a worker's ownership maintained?        | Renewable database lease                |
| How is message redelivery delayed?             | SQS Visibility Timeout                  |
| How do we know the worker is alive?            | Heartbeat                               |
| How do we know it is making progress?          | Separate progress updates               |
| How do we prevent stale workers from writing?  | Fencing tokens and conditional writes   |
| How do we retry failed tasks?                  | Application-managed retries             |
| How do we recover crashed workers?             | Lease Recovery Scheduler                |
| How do we prevent duplicate business effects?  | Idempotency                             |
| When do we delete an SQS message?              | After a durable state transition        |
| How do we handle permanently failing messages? | Failed task state and DLQ               |
| How do we process very long work?              | Checkpointing or workflow orchestration |

There are also two separate scaling concerns.

Worker scaling: Scale workers based on queue depth, message age, processing time and downstream capacity.

Database scaling: Avoid excessive heartbeat writes. Every running task produces periodic database writes. At 10,000 concurrent tasks with a 30-second heartbeat, that is roughly 333 DB heartbeats per second, before counting claims, completions and other updates.

You can optimize heartbeat frequency, batch operations where appropriate, and isolate task state from other transactional workloads as volume grows.

Also enforce concurrency limits against downstream systems. A queue with 100,000 tasks should not cause 100,000 simultaneous requests to a payment provider or database.

# 23. The mental model I want you to retain

Let's reduce the whole architecture to six responsibilities.

1\. Outbox = Reliable publication

The task and the event must be persisted together. The publisher can publish multiple times, so consumers must tolerate duplicates.

2\. SQS Visibility Timeout = Message delivery protection

It temporarily prevents other consumers from receiving a message. It is not business ownership.

3\. DB Lease = Temporary processing ownership

A worker must acquire ownership atomically and keep renewing it.

4\. Heartbeat = Proof of continued ownership

A worker periodically confirms that it is alive and extends both its database lease and queue visibility.

5\. Fencing Token = Protection from stale workers

A previous worker might resume after losing ownership. A monotonically increasing token prevents it from overwriting the newer owner's state, provided downstream writes also enforce fencing where necessary.

6\. Idempotency = Protection from repeated business effects

A task may execute multiple times. Repeating its business operation must not produce unintended duplicate outcomes.

And there are two supporting mechanisms:

- Retry scheduler: decides when and whether failed tasks should run again.
- Lease recovery scheduler: discovers tasks abandoned by dead workers.

Here is the overall mental model:

\#chatgpt-mermaid-\_r_7gq\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_7gq\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_7gq\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_7gq\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_7gq\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7gq\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_7gq\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_7gq\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_7gq\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_7gq\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_7gq\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_7gq\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7gq\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7gq\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_7gq\_ p{margin:0;}#chatgpt-mermaid-\_r_7gq\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7gq\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7gq\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7gq\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_7gq\_ .label text,#chatgpt-mermaid-\_r_7gq\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7gq\_ .node rect,#chatgpt-mermaid-\_r_7gq\_ .node circle,#chatgpt-mermaid-\_r_7gq\_ .node ellipse,#chatgpt-mermaid-\_r_7gq\_ .node polygon,#chatgpt-mermaid-\_r_7gq\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_7gq\_ .rough-node .label text,#chatgpt-mermaid-\_r_7gq\_ .node .label text,#chatgpt-mermaid-\_r_7gq\_ .image-shape .label,#chatgpt-mermaid-\_r_7gq\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_7gq\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_7gq\_ .rough-node .label,#chatgpt-mermaid-\_r_7gq\_ .node .label,#chatgpt-mermaid-\_r_7gq\_ .image-shape .label,#chatgpt-mermaid-\_r_7gq\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_7gq\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_7gq\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7gq\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7gq\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_7gq\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_7gq\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_7gq\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_7gq\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_7gq\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_7gq\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_7gq\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7gq\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7gq\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_7gq\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7gq\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_7gq\_ .icon-shape,#chatgpt-mermaid-\_r_7gq\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_7gq\_ .icon-shape p,#chatgpt-mermaid-\_r_7gq\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_7gq\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_7gq\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_7gq\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_7gq\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_7gq\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_7gq\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_7gq\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_7gq\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_7gq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7gq\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_7gq\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_7gq\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_7gq\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7gq\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_7gq\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_7gq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7gq\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_7gq\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_7gq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7gq\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_7gq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7gq\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_7gq\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_7gq\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_7gq\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_7gq\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_7gq\_ .node rect,#chatgpt-mermaid-\_r_7gq\_ .node circle,#chatgpt-mermaid-\_r_7gq\_ .node ellipse,#chatgpt-mermaid-\_r_7gq\_ .node polygon,#chatgpt-mermaid-\_r_7gq\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_7gq\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_7gq\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_7gq\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_7gq\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7gq\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Create TaskPersist Task + OutboxPublish to SQSWorker Receives MessageAtomically Claim DB TaskInspect Current Task StateNo Processing / HandleDuplicateStart ProcessingStart HeartbeatRenew DB LeaseExtend SQS VisibilityContinue ProcessingResult?Persist Result + SUCCEEDEDDelete SQS MessagePersist RETRY_WAITRetry SchedulerPersist FAILEDLease ExpiresRecovery SchedulerClaim FailedClaim SuccessfulSuccessRetryable ErrorPermanent ErrorWorker Dies

## 24. What makes this design reliable?

One last way of testing your understanding is to remove one component at a time.

| Remove this mechanism    | What can go wrong?                                                   |
| ------------------------ | -------------------------------------------------------------------- |
| Transactional outbox     | Tasks can remain in DB without queue messages.                       |
| Atomic DB claim          | Duplicate deliveries can cause concurrent execution.                 |
| DB lease                 | Crashed workers can leave tasks permanently stuck.                   |
| Heartbeat                | Healthy long-running workers can lose ownership.                     |
| SQS visibility extension | Messages can be repeatedly redelivered during processing.            |
| Fencing token            | Stale workers can overwrite newer results.                           |
| Idempotency              | A retried operation can charge a customer or create an output twice. |
| Retry scheduler          | Transient failures may never be retried appropriately.               |
| Recovery scheduler       | A task can remain `RUNNING` after its message disappears.            |
| DLQ / failure limits     | Poison messages can repeatedly consume resources.                    |
| Monitoring               | Operational failures become difficult to detect and diagnose.        |

The key insight is that we are not trying to make a worker run exactly once. We are designing a system where a task can be delivered multiple times, attempted multiple times, interrupted at any moment, recovered by another worker, and still produce the correct business outcome.

That is the foundation of reliable background processing.

## 25. Interview-ready explanation

Suppose an interviewer asks:

"How would you design a reliable background task processing system using SQS and multiple workers?"

I would explain it in this order:

1. The API creates a task in the database and an outbox event in the same transaction. An outbox publisher sends the event to SQS.
2. Workers consume messages from SQS. Since delivery is at least once, we don't assume that messages are unique.
3. Before processing, a worker atomically claims the task using a conditional database update. This prevents concurrent workers from acquiring a currently valid lease.
4. The worker receives a time-limited database lease and a fencing token. It periodically renews the lease and extends the SQS visibility timeout using heartbeats.
5. If the worker crashes, its lease eventually expires. A recovery scheduler identifies abandoned tasks and schedules another attempt.
6. If processing fails, the application classifies the error and either schedules a retry using exponential backoff with jitter or marks the task permanently failed.
7. Fencing tokens prevent stale workers from updating newer task generations. Idempotency keys protect external business operations from duplicate execution.
8. On success, we persist the result and mark the task completed in the database before deleting the SQS message.
9. Duplicate deliveries become harmless because the database already records the task's current state.
10. We monitor queue age, heartbeat failures, expired leases, retries, stuck tasks and DLQ messages.

That gives us a system with at-least-once delivery, recoverable execution, controlled retries and effectively-once business outcomes where all side effects are appropriately protected.

My recommendation for your system-design preparation: Master the relationship between three concepts in particular: DB Lease, SQS Visibility Timeout and Fencing Token. People often understand each individually but fail to explain how they interact during network partitions, worker crashes and duplicate deliveries. Once you understand those interactions, the rest of the architecture becomes much easier to reason about.

