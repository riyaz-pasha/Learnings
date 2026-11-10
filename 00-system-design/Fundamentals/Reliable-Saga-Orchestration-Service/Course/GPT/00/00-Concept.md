# Building a reliable Saga orchestration service

The most important thing to understand is that a reliable Saga orchestrator is not simply a service that calls other services sequentially and updates a status column.

It is a durable state machine that coordinates multiple independent transactions. It must be able to recover from a crash at any point, tolerate duplicate messages, distinguish a real failure from an unknown outcome, retry safely, and compensate for already completed operations.

The fundamental design principle is:

> Persist what must happen before attempting it. Make every operation safely repeatable. Record what happened before deciding what happens next.

We will build the design around a familiar example:

A customer places an order. The system needs to:

1. Reserve inventory.
2. Authorize payment.
3. Create a shipment.
4. Mark the order as confirmed.

Suppose shipment creation fails. We must release the payment authorization and inventory reservation, then mark the order as failed or cancelled.

The services own their own databases:

```
                    Saga Orchestrator
                           |
                    Orchestration DB
                           |
             +-------------+-------------+
             |             |             |
        Inventory       Payment       Shipping
         Service        Service        Service
             |             |             |
        Inventory DB   Payment DB    Shipping DB
```

The orchestrator does not directly modify the inventory, payment, or shipping databases. It tells the respective services what to do and records their outcomes.

There is no single ACID transaction spanning all these databases. Each service performs its own local transaction, and the orchestrator coordinates the overall business process.

## 1. First, understand what the orchestrator is responsible for

Think of the orchestrator as a durable project manager.

It maintains the plan, knows which tasks have completed, knows which task should happen next, and knows what to do when something goes wrong.

For one order, its durable state might look like this:

| Step | Operation                  | Status               |
| ---- | -------------------------- | -------------------- |
| 1    | Reserve inventory          | SUCCEEDED            |
| 2    | Authorize payment          | SUCCEEDED            |
| 3    | Create shipment            | FAILED               |
| 4    | Void payment authorization | COMPENSATION_PENDING |
| 5    | Release inventory          | PENDING              |

The orchestrator must be able to reconstruct the situation from its database after a complete process restart.

It must not depend on:

- An in-memory list of completed steps.
- A running HTTP request.
- A particular worker remaining alive.
- A response being delivered exactly once.
- A message broker delivering messages only once.

The database is the source of truth for the workflow. Workers and message brokers help make progress, but they do not own the workflow's state.

This distinction will guide every design decision that follows.

## 2. Why the simple implementation is not reliable

Imagine implementing the coordinator like this:

```
def process_order(order):    create_workflow_record(order)    reserve_inventory(order)    update_step_status("INVENTORY_RESERVED")    authorize_payment(order)    update_step_status("PAYMENT_AUTHORIZED")    create_shipment(order)    update_step_status("SHIPMENT_CREATED")    mark_order_completed(order)
```

And suppose a service call fails:

```
try:    reserve_inventory(order)except Exception:    mark_workflow_failed(order)
```

This looks reasonable, but there are several dangerous failure windows.

### Problem A: The remote operation succeeds, but the coordinator crashes

Consider:

```
1. Coordinator calls Inventory Service.
2. Inventory Service successfully reserves 2 items.
3. Inventory Service commits its database transaction.
4. Inventory Service sends a successful response.
5. Coordinator crashes before updating its own database.
```

When the coordinator restarts, its database still says:

```
Inventory Reservation = PENDING
```

But the inventory has already been reserved.

The coordinator does not know whether the operation happened.

Should it call the inventory service again?

If it does, it might reserve the inventory twice.

### Problem B: The coordinator records success, but the remote operation never happens

Suppose the coordinator does this:

```
1. Update step status to STARTED.
2. Coordinator crashes before calling the Inventory Service.
```

After restarting, the coordinator sees that the step started, but there is no actual reservation.

It must know whether to retry the operation.

### Problem C: The operation succeeds, but the response gets lost

```
Coordinator                 Inventory Service
     |                              |
     |---- Reserve inventory ------>|
     |                              |
     |                         DB COMMIT
     |                              |
     |<---- Success response -------X
     |
     |         Timeout
```

The inventory service performed the operation, but the coordinator never received the response.

A timeout does not mean that the remote operation failed.

It means that the coordinator does not know the outcome.

This is one of the most important concepts in distributed systems:

An unknown outcome must not be treated as a definite failure.

### Problem D: Duplicate requests

Suppose the coordinator retries after a timeout:

```
Reserve inventory (request 1) -> succeeds
Reserve inventory (request 2) -> arrives later
```

Without idempotency, both requests might reserve inventory.

### Problem E: Compensation also fails

Suppose shipment creation fails, and the coordinator attempts to void the payment authorization.

The payment service is temporarily unavailable.

Now what?

The coordinator cannot simply give up. The workflow is not successfully completed, but it is also not fully compensated.

It needs a durable compensation state and a way to retry later.

These are not exceptional edge cases. They are normal situations that a reliable distributed workflow must expect.

We can solve them by separating workflow state, message delivery, and business operations.

## 3. The architecture we want to build

A robust design looks like this:

\#chatgpt-mermaid-\_r_6te\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_6te\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_6te\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_6te\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_6te\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6te\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_6te\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_6te\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_6te\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_6te\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_6te\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_6te\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6te\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6te\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_6te\_ p{margin:0;}#chatgpt-mermaid-\_r_6te\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6te\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6te\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6te\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_6te\_ .label text,#chatgpt-mermaid-\_r_6te\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6te\_ .node rect,#chatgpt-mermaid-\_r_6te\_ .node circle,#chatgpt-mermaid-\_r_6te\_ .node ellipse,#chatgpt-mermaid-\_r_6te\_ .node polygon,#chatgpt-mermaid-\_r_6te\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_6te\_ .rough-node .label text,#chatgpt-mermaid-\_r_6te\_ .node .label text,#chatgpt-mermaid-\_r_6te\_ .image-shape .label,#chatgpt-mermaid-\_r_6te\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_6te\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_6te\_ .rough-node .label,#chatgpt-mermaid-\_r_6te\_ .node .label,#chatgpt-mermaid-\_r_6te\_ .image-shape .label,#chatgpt-mermaid-\_r_6te\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_6te\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_6te\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6te\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6te\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_6te\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_6te\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_6te\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6te\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6te\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_6te\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_6te\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6te\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6te\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_6te\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6te\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_6te\_ .icon-shape,#chatgpt-mermaid-\_r_6te\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_6te\_ .icon-shape p,#chatgpt-mermaid-\_r_6te\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_6te\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_6te\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6te\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_6te\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_6te\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_6te\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_6te\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_6te\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_6te\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6te\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_6te\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_6te\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_6te\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6te\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_6te\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_6te\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6te\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_6te\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_6te\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6te\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_6te\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6te\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_6te\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_6te\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_6te\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_6te\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_6te\_ .node rect,#chatgpt-mermaid-\_r_6te\_ .node circle,#chatgpt-mermaid-\_r_6te\_ .node ellipse,#chatgpt-mermaid-\_r_6te\_ .node polygon,#chatgpt-mermaid-\_r_6te\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_6te\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_6te\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_6te\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_6te\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6te\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Client / APISaga CoordinatorCoordinator PostgreSQL DBWorkflow WorkersOutbox DispatcherMessage BrokerInventory ServicePayment ServiceShipping ServiceInventory DBPayment DBShipping DB

There are five important pieces:

| Component            | Responsibility                                                      |
| -------------------- | ------------------------------------------------------------------- |
| Coordinator API      | Accepts a business request and creates a Saga.                      |
| Coordinator database | Stores workflow state, steps, attempts, and history.                |
| Workflow workers     | Decide what action should happen next and process timeouts/retries. |
| Outbox dispatcher    | Reliably publishes commands and events.                             |
| Participant services | Execute their own business operations and return durable outcomes.  |

A message broker such as Kafka or RabbitMQ can transport messages. SQS can also be used for a queue-based implementation.

The choice of broker is separate from the choice of the coordinator's database.

### The central execution pattern

Every important action follows this pattern:

```
1. Persist the intention in a database transaction.
2. Commit the transaction.
3. Deliver the message or execute the action.
4. Receive the outcome.
5. Persist the outcome in another database transaction.
6. Decide and persist the next action.
```

Notice what is absent:

We never hold a database transaction open while waiting for another service's HTTP response or message.

This is important for connection usage, lock duration, recovery, and correctness.

## 4. Design the Saga as a state machine

Before designing tables or retry logic, define the lifecycle.

For the overall workflow, we could use:

```
                    +---------+
                    | PENDING |
                    +----+----+
                         |
                         v
                    +---------+
              +---->| RUNNING |
              |     +----+----+
              |          |
              |          +--------------------+
              |          |                    |
              |          v                    v
              |      COMPLETED          COMPENSATING
              |                               |
              |                               v
              |                         COMPENSATED
              |                              
              |                         or COMPENSATION_FAILED
              |
              +----- Retry / Resume
```

A more complete set of Saga statuses:

| Status                | Meaning                                                                                  |
| --------------------- | ---------------------------------------------------------------------------------------- |
| `PENDING`             | Created but execution has not started.                                                   |
| `RUNNING`             | Forward operations are being executed.                                                   |
| `COMPENSATING`        | An operation failed or cancellation was requested; compensations are running.            |
| `COMPLETED`           | All required forward operations succeeded.                                               |
| `COMPENSATED`         | All required compensations succeeded.                                                    |
| `FAILED`              | Workflow failed without needing compensation, or failed before any side effect occurred. |
| `COMPENSATION_FAILED` | Compensation could not be completed automatically and requires intervention.             |
| `CANCELLED`           | Workflow was cancelled according to the business rules.                                  |

I would keep `COMPENSATION_FAILED` separate from `COMPENSATED`. A workflow must never claim that it has been rolled back when some compensating action is still outstanding.

### Individual step statuses

Each step needs its own lifecycle:

```
PENDING
   |
   v
COMMAND_QUEUED
   |
   v
IN_PROGRESS
   |
   +----------------------+
   |                      |
   v                      v
SUCCEEDED              RETRY_WAIT
                          |
                          v
                     COMMAND_QUEUED

If a permanent failure occurs:

IN_PROGRESS
   |
   v
FAILED
```

Compensation has its own lifecycle:

```
COMPENSATION_PENDING
          |
          v
COMPENSATION_QUEUED
          |
          v
COMPENSATING
          |
          +-------------------+
          |                   |
          v                   v
       COMPENSATED        RETRY_WAIT
                              |
                              v
                     COMPENSATION_QUEUED
```

Why maintain separate forward and compensation statuses?

Because these are two different operations with different outcomes.

For example:

```
Inventory reservation: SUCCEEDED
Inventory release:     COMPENSATION_FAILED
```

The original reservation really did succeed. It must not be overwritten as though it never happened.

The coordinator needs this history for recovery, support, audit, and operational troubleshooting.

## 5. How do we store this in the coordinator database?

For this workflow, I would start with PostgreSQL.

We will discuss the database choice in detail later. First, let's design the data model.

A reasonable starting point is four logical tables:

1. `sagas`
2. `saga_steps`
3. `outbox_messages`
4. `saga_events`

And an inbox/deduplication table for incoming messages.

### 5.1 `sagas` table

One row per business workflow.

```
CREATE TABLE sagas (
    id UUID PRIMARY KEY,

    saga_type VARCHAR(100) NOT NULL,

    business_id VARCHAR(200) NOT NULL,

    status VARCHAR(40) NOT NULL,

    current_step INTEGER,

    version BIGINT NOT NULL DEFAULT 0,

    idempotency_key VARCHAR(200) NOT NULL,

    request_payload JSONB,

    error_details JSONB,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    completed_at TIMESTAMPTZ,

    UNIQUE (saga_type, idempotency_key)
);
```

Example:

| id         | business_id | status  | current_step |
| ---------- | ----------- | ------- | ------------ |
| `saga-123` | `order-456` | RUNNING | 2            |

Some important fields:

`business_id`

The business entity associated with the workflow. In our example, the order ID.

`idempotency_key`

Prevents the client from accidentally creating the same workflow multiple times.

For example, the client submits an order but times out before receiving the response. It retries with the same idempotency key.

The unique constraint ensures that the system does not create two workflows for the same logical request.

In production, also compare a request hash when a key is reused. The same key with a different request body should not silently return an unrelated result.

`version`

Supports optimistic concurrency control.

For example, two workers might try to advance the same Saga simultaneously. Version-based updates help prevent conflicting state transitions.

`request_payload`

Stores the information necessary to execute or recover the workflow.

Avoid blindly putting huge documents or sensitive payment details into JSONB. Store only the necessary information, with references to secure systems where appropriate.

### 5.2 `saga_steps` table

One row per forward step.

```
CREATE TABLE saga_steps (
    id UUID PRIMARY KEY,

    saga_id UUID NOT NULL REFERENCES sagas(id),

    step_name VARCHAR(150) NOT NULL,

    sequence_number INTEGER NOT NULL,

    status VARCHAR(40) NOT NULL,

    command_id UUID NOT NULL UNIQUE,

    command_payload JSONB,

    result_payload JSONB,

    attempt_count INTEGER NOT NULL DEFAULT 0,

    next_attempt_at TIMESTAMPTZ,

    started_at TIMESTAMPTZ,

    completed_at TIMESTAMPTZ,

    error_details JSONB,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (saga_id, sequence_number),
    UNIQUE (saga_id, step_name)
);
```

Example:

| saga_id  | step_name         | sequence | status      |
| -------- | ----------------- | -------- | ----------- |
| saga-123 | RESERVE_INVENTORY | 1        | SUCCEEDED   |
| saga-123 | AUTHORIZE_PAYMENT | 2        | SUCCEEDED   |
| saga-123 | CREATE_SHIPMENT   | 3        | IN_PROGRESS |

This table stores the execution state of each operation.

Why a separate row for each step?

Because workflows have multiple independent operations, and each operation needs its own:

- Status.
- Command identifier.
- Retry count.
- Timeout/deadline.
- Request and result.
- Error details.
- Completion time.

It is much easier to query and recover from individual step failures this way.

For a reusable production framework, you may also have a `saga_step_executions` or `saga_actions` table to represent both forward and compensation operations independently. The simpler design above is sufficient to introduce the concepts; the important thing is that compensation has its own durable status and command identity.

### 5.3 `outbox_messages` table

This is one of the most important parts of the entire design.

```
CREATE TABLE outbox_messages (
    id UUID PRIMARY KEY,

    aggregate_id UUID NOT NULL,

    message_type VARCHAR(150) NOT NULL,

    payload JSONB NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    published_at TIMESTAMPTZ,

    attempt_count INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

This table guarantees that an intended command or event is not lost between the database transaction and message publication.

We will examine exactly why this is needed shortly.

### 5.4 `saga_events` table

This is the workflow's historical record.

```
CREATE TABLE saga_events (
    id UUID PRIMARY KEY,

    saga_id UUID NOT NULL REFERENCES sagas(id),

    event_type VARCHAR(150) NOT NULL,

    step_name VARCHAR(150),

    payload JSONB,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Example history:

```
10:00:00 ORDER_WORKFLOW_CREATED
10:00:01 INVENTORY_RESERVATION_REQUESTED
10:00:02 INVENTORY_RESERVATION_SUCCEEDED
10:00:03 PAYMENT_AUTHORIZATION_REQUESTED
10:00:04 PAYMENT_AUTHORIZATION_SUCCEEDED
10:00:05 SHIPMENT_CREATION_REQUESTED
10:00:15 SHIPMENT_CREATION_FAILED
10:00:16 COMPENSATION_STARTED
10:00:17 PAYMENT_VOID_REQUESTED
```

This is useful for audit logs, debugging, and understanding what happened.

The current state should be stored in the Saga and step records, so normal processing does not have to replay the entire event history every time.

### 5.5 Incoming message deduplication

The coordinator also needs to know whether it has already processed an incoming message.

For example:

```
CREATE TABLE processed_messages (
    consumer_name VARCHAR(100) NOT NULL,

    message_id UUID NOT NULL,

    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (consumer_name, message_id)
);
```

This is an inbox-style deduplication table.

When the same event is delivered twice, the unique constraint lets the coordinator recognize that it has already processed that event.

The same principle will also be required inside every participant service.

## 6. The most important reliability pattern: Transactional Outbox

Let's solve the first major failure problem.

The coordinator needs to do two things:

1. Update its database.
2. Send a command to another service.

For example:

```
Update step status = COMMAND_QUEUED
Send ReserveInventory command
```

These are two separate operations involving two separate systems:

- PostgreSQL.
- Message broker.

There is no ordinary local database transaction that can atomically commit both.

### The dangerous implementation

```
# Update databasestep.status = "COMMAND_QUEUED"db.commit()# Publish messagebroker.publish("ReserveInventory", payload)
```

Imagine:

```
1. Database commit succeeds.
2. Coordinator crashes.
3. Message is never published.
```

Now the database says that the command is queued, but no command was actually sent.

What if we reverse the order?

```
broker.publish("ReserveInventory", payload)step.status = "COMMAND_QUEUED"db.commit()
```

Now imagine:

```
1. Message is published successfully.
2. Coordinator crashes.
3. Database transaction never commits.
```

The inventory service might receive a command for which the coordinator has no committed record.

Neither ordering solves the problem.

### The solution: Transactional Outbox

Instead of publishing directly, store the message in the same database transaction as the state change.

```
BEGIN;

UPDATE saga_steps
SET status = 'COMMAND_QUEUED',
    updated_at = NOW()
WHERE id = :step_id;

INSERT INTO outbox_messages (
    id,
    aggregate_id,
    message_type,
    payload
)
VALUES (
    :message_id,
    :saga_id,
    'ReserveInventory',
    :payload
);

COMMIT;
```

Now both records are committed atomically.

There are only two possible outcomes:

Case 1: Transaction rolls back

```
Step status update: NOT COMMITTED
Outbox insert:      NOT COMMITTED
```

Nothing happens.

Case 2: Transaction commits

```
Step status update: COMMITTED
Outbox insert:      COMMITTED
```

The coordinator has durably recorded that the command needs to be sent.

Even if the coordinator crashes immediately afterwards, the outbox message remains in PostgreSQL.

### How does the message actually get published?

A separate background worker, called the Outbox Dispatcher, continuously reads pending messages.

```
def publish_pending_messages():    messages = claim_pending_messages()    for message in messages:        broker.publish(            topic=message.message_type,            payload=message.payload        )        mark_as_published(message.id)
```

The dispatcher should claim messages using a lease or row lock, rather than allowing multiple workers to process the same row concurrently.

For PostgreSQL, a common approach is:

```
BEGIN;

SELECT id
FROM outbox_messages
WHERE status = 'PENDING'
  AND available_at <= NOW()
ORDER BY created_at
FOR UPDATE SKIP LOCKED
LIMIT 100;

-- Claim selected messages by updating their status/lease.

COMMIT;
```

The publication happens outside this database transaction. Once the broker acknowledges publication, the dispatcher marks the message as published.

There is one subtle but very important problem remaining.

### What if publishing succeeds but marking the message as published fails?

```
1. Dispatcher publishes message successfully.
2. Message broker accepts it.
3. Dispatcher crashes before updating PostgreSQL.
4. Dispatcher restarts.
5. It sees the message as PENDING.
6. It publishes it again.
```

Therefore, Transactional Outbox provides reliable eventual publication, not exactly-once delivery.

Duplicates are expected.

This is not a problem when consumers are designed to be idempotent.

We deliberately choose a system where messages might be delivered more than once rather than one where an important command could silently disappear.

This is a fundamental reliability trade-off.

## 7. How a participant service processes commands reliably

Now let's look at the Inventory Service.

The coordinator publishes:

```
{
  "messageId": "msg-100",
  "commandId": "cmd-100",
  "sagaId": "saga-123",
  "stepName": "RESERVE_INVENTORY",
  "sequence": 1,
  "type": "ReserveInventory",
  "payload": {
    "orderId": "order-456",
    "productId": "product-10",
    "quantity": 2
  }
}
```

The Inventory Service receives it.

A naive implementation might simply do:

```
def reserve_inventory(command):    inventory.available_quantity -= command.quantity    db.commit()    publish_success_event()
```

But this is not safe.

Consider the earlier duplicate message scenario. If the same command is delivered again, inventory could be reduced twice.

### The participant needs an Inbox pattern

Each participant should maintain a record of commands it has already processed.

For example:

```
CREATE TABLE inbox_messages (
    consumer_name VARCHAR(100) NOT NULL,

    message_id UUID NOT NULL,

    command_id UUID NOT NULL,

    status VARCHAR(30) NOT NULL,

    result_payload JSONB,

    processed_at TIMESTAMPTZ,

    PRIMARY KEY (consumer_name, message_id),

    UNIQUE (consumer_name, command_id)
);
```

The `command_id` identifies the logical operation, not an individual delivery attempt.

Now the Inventory Service processes the command inside one local database transaction.

Conceptually:

```
def handle_reserve_inventory(command):    with db.transaction():        existing = find_by_command_id(command.command_id)        if existing:            return existing.result_payload        insert_inbox_record(            command_id=command.command_id,            message_id=command.message_id        )        inventory = get_inventory_for_update(            command.product_id        )        if inventory.available_quantity < command.quantity:            result = {                "success": False,                "reason": "INSUFFICIENT_INVENTORY"            }        else:            inventory.available_quantity -= command.quantity            create_reservation(                order_id=command.order_id,                product_id=command.product_id,                quantity=command.quantity,                command_id=command.command_id            )            result = {                "success": True,                "reservation_id": "reservation-123"            }        update_inbox_result(command.command_id, result)        insert_outbox_event(            type="InventoryReservationResult",            payload=result        )
```

The important part is not the exact code. It is the transaction boundary.

These operations must commit together:

```
1. Record that the command was processed.
2. Perform the inventory operation.
3. Store the result.
4. Store the response event in the participant's outbox.
```

If any operation fails, all of them roll back.

### Why is this safe?

Consider the possible failures.

Scenario A: Inventory operation fails before commit

```
Inbox record:       ROLLBACK
Inventory update:   ROLLBACK
Response outbox:    ROLLBACK
```

The broker can redeliver the command. It will be processed again.

Scenario B: Inventory operation commits, but the service crashes before acknowledging the message

```
Inbox record:       COMMITTED
Inventory update:   COMMITTED
Response outbox:    COMMITTED
Message ACK:        NOT SENT
```

The broker redelivers the command.

The service finds the existing `command_id`, recognizes the duplicate, and does not reserve inventory again.

Scenario C: Inventory operation commits, but the response is not published

The response is already stored in the participant's outbox.

Its outbox dispatcher can publish it later.

This is the same reliability pattern we used in the coordinator.

### A critical detail: duplicate requests must return the original result

Suppose the first reservation succeeded.

The coordinator did not receive the response and sends the same logical command again.

The Inventory Service must not just say:

```
Duplicate command.
Ignoring it.
```

The coordinator still needs the result!

It should return or republish the previously stored outcome:

```
{
  "commandId": "cmd-100",
  "success": true,
  "reservationId": "reservation-123"
}
```

It must not execute the business operation again.

### The same principle applies to every participant

| Service   | Example operation | Idempotency requirement                |
| --------- | ----------------- | -------------------------------------- |
| Inventory | Reserve 2 items   | One reservation per logical command.   |
| Payment   | Authorize $100    | One authorization per logical command. |
| Shipping  | Create shipment   | One shipment per logical command.      |

For external systems, such as payment gateways, the participant must also use that provider's idempotency mechanism when available. Its local database transaction cannot atomically commit with the external provider.

That external boundary needs its own recovery strategy, which we will discuss below.

## 8. How does the coordinator receive the response?

Suppose the Inventory Service successfully completes the reservation.

It produces an event:

```
{
  "messageId": "event-100",
  "commandId": "cmd-100",
  "sagaId": "saga-123",
  "stepName": "RESERVE_INVENTORY",
  "sequence": 1,
  "type": "InventoryReservationSucceeded",
  "payload": {
    "reservationId": "reservation-123"
  }
}
```

The coordinator consumes this event.

It must not simply update a status column without considering duplicate delivery and concurrency.

Instead, it performs a local transaction:

```
BEGIN;

-- 1. Deduplicate the incoming event.
INSERT INTO processed_messages (
    consumer_name,
    message_id
)
VALUES (
    'saga-coordinator',
    :message_id
)
ON CONFLICT DO NOTHING;

-- Continue only if the insert was new.

-- 2. Update the corresponding step.
UPDATE saga_steps
SET status = 'SUCCEEDED',
    result_payload = :result,
    completed_at = NOW(),
    updated_at = NOW()
WHERE command_id = :command_id
  AND status IN ('COMMAND_QUEUED', 'IN_PROGRESS');

-- 3. Update the Saga state/version.

-- 4. Insert the next command into the outbox
--    if all validation and transition rules pass.

COMMIT;
```

The real implementation should check the affected row count and validate that:

- The Saga exists.
- The event belongs to that Saga.
- The command matches the expected step.
- The event has not already been processed.
- The step is in a state where this transition is valid.
- The event is not stale or inconsistent with a newer state.

The event deduplication, step update, Saga transition, and possible next outbox insertion should be atomic.

Only acknowledge the broker message after the database transaction commits.

If the coordinator crashes before acknowledgment, the event can be redelivered. Its inbox table prevents the same event from advancing the workflow twice.

### What does the coordinator do next?

For a sequential workflow:

```
Inventory Reservation: SUCCEEDED
                  |
                  v
        Enqueue Authorize Payment
                  |
                  v
          Payment Service
```

It does not immediately need to call the Payment Service.

It records the next command in its outbox, commits the transaction, and lets the dispatcher publish it.

This ensures that the coordinator never advances the workflow without durably recording the next action.

## 9. How do retries work?

This is another area where reliable systems are often implemented incorrectly.

There are two different things to retry:

1. Delivery of a command.
2. Execution of a business operation.

They are related, but they are not the same.

### 9.1 Message delivery retries

The message broker may redeliver a message because:

- A consumer crashed before acknowledging it.
- The acknowledgement was lost.
- The consumer's processing took longer than the visibility timeout.
- A network failure occurred.

This is a delivery retry.

The message should retain the same logical `command_id`.

### 9.2 Business operation retries

Suppose the Inventory Service is temporarily unavailable, or a network problem prevents it from completing the request.

The coordinator may decide to retry the business operation.

For example:

```
Attempt 1: Inventory service unavailable
Attempt 2: Database connection timeout
Attempt 3: Reservation succeeds
```

We need to track these attempts.

A command might contain:

```
{
  "commandId": "cmd-100",
  "sagaId": "saga-123",
  "stepName": "RESERVE_INVENTORY",
  "attempt": 3
}
```

The command ID remains the same across retries of the same logical operation.

The attempt number changes.

Why?

Because the participant must recognize that all three attempts are asking it to perform the same reservation, not three separate reservations.

### 9.3 Exponential backoff with jitter

We should not retry every 100 milliseconds.

Imagine 10,000 workflows failing because a participant's database is temporarily unavailable.

If all workflows retry immediately, they can overload the already struggling service.

Use exponential backoff:

\\[ delay = \min(initialDelay \times 2^{attempt-1}, maxDelay) \\]

Then add random jitter.

For example:

| Attempt | Approximate delay |
| ------- | ----------------- |
| 1       | 1 second          |
| 2       | 2 seconds         |
| 3       | 4 seconds         |
| 4       | 8 seconds         |
| 5       | 16 seconds        |
| 6       | 32 seconds        |

In practice, full jitter can be calculated as:

\\[ delay = random(0,\min(cap, base \times 2^{attempt})) \\]

The exact values depend on the service's latency, failure patterns, and business deadline.

### 9.4 Do not keep a worker sleeping

A bad approach:

```
while attempts < 5:    try:        execute_step()        break    except Exception:        sleep(30)
```

This occupies a worker while waiting.

A better approach is:

```
Step status: RETRY_WAIT
Attempt count: 3
Next attempt at: 14:30:30
```

Persist the retry time.

A scheduler or worker periodically finds eligible retries:

```
SELECT id
FROM saga_steps
WHERE status = 'RETRY_WAIT'
  AND next_attempt_at <= NOW()
ORDER BY next_attempt_at
LIMIT 100;
```

It claims the eligible records, moves them into a runnable state, and inserts the retry command into the outbox in the same transaction.

The worker can then process other work.

### 9.5 Should every failure be retried?

No.

We should classify failures.

| Failure                            | Typical treatment                                     |
| ---------------------------------- | ----------------------------------------------------- |
| Network timeout                    | Retry, but outcome may be unknown.                    |
| Temporary database outage          | Retry.                                                |
| HTTP 503                           | Retry.                                                |
| Rate limiting                      | Retry according to `Retry-After` or service policy.   |
| Invalid request                    | Permanent failure.                                    |
| Insufficient inventory             | Usually business failure; do not retry indefinitely.  |
| Invalid payment method             | Usually permanent failure.                            |
| Authentication/configuration error | Alert and investigate; blind retries will not fix it. |

There is a critical difference between a technical failure and a business rejection.

For example, insufficient inventory is a valid business response. It should usually trigger the Saga's failure/compensation path, not repeated attempts to reserve the same unavailable stock.

### 9.6 What about maximum retries?

There are two broad approaches.

Bounded retry policy

Retry a limited number of times, or until a business deadline.

When exhausted, move the workflow into failure handling.

Long-lived recovery policy

Some operations, especially compensations, may be retried for a long time or until an operator resolves the issue.

Do not assume that all operations have the same retry policy.

Also distinguish an exhausted delivery retry from an exhausted business retry. A broker DLQ is not proof that the business operation failed. It means that automatic message delivery/processing has stopped and needs recovery or intervention.

## 10. How do we handle timeouts and unknown outcomes?

This deserves its own section because it is probably the most subtle part of a reliable Saga implementation.

Imagine:

```
Coordinator                     Payment Service
     |                                 |
     |---- Authorize payment ---------->|
     |                                 |
     |                            Payment succeeds
     |                                 |
     |                            Database committed
     |                                 |
     |<----------- Response ------------X
     |
     |       Timeout
```

What should happen?

The coordinator cannot simply mark the payment as failed and compensate inventory.

The payment might already be authorized.

It must first establish the operation's actual status.

### Use a three-way interpretation

When a request times out, think in terms of:

```
SUCCESS
FAILURE
UNKNOWN
```

`UNKNOWN` is a real operational state, not just an error message.

For example, add:

```
OUTCOME_UNKNOWN
```

to the step lifecycle, or use an `IN_PROGRESS` state with a separate uncertainty/error reason.

The coordinator can then do one of the following.

Option A: Retry using the same idempotency key

This is usually the simplest approach.

The Payment Service receives the same logical command again.

If the previous attempt succeeded, it returns the original result.

If it never succeeded, it attempts the authorization.

Option B: Query the participant's operation status

The Payment Service can expose an API such as:

```
GET /payment-operations/{commandId}
```

Possible results:

```
{
  "status": "SUCCEEDED",
  "authorizationId": "auth-123"
}
```

```
{
  "status": "FAILED",
  "reason": "PAYMENT_DECLINED"
}
```

```
{
  "status": "IN_PROGRESS"
}
```

This is particularly helpful for long-running operations.

Option C: Reconcile with an external provider

Suppose the Payment Service calls an external payment gateway.

The gateway may successfully authorize the card while the Payment Service crashes before saving the result.

The participant's database is now also uncertain.

It needs to query the payment provider using the provider's transaction reference or idempotency key, rather than blindly issuing another charge or authorization.

This is a separate distributed boundary:

```
Payment Service
      |
      v
Payment Gateway
      |
      v
External payment network
```

The participant service needs its own durable operation record, idempotency key, retry policy, and reconciliation job.

The general rule is: never compensate an operation merely because its response timed out. First resolve whether the forward operation took effect.

## 11. How does compensation actually work?

Now let's get to the defining feature of Saga orchestration.

Suppose the workflow has completed:

```
1. Reserve Inventory       SUCCEEDED
2. Authorize Payment       SUCCEEDED
3. Create Shipment         FAILED
```

We must undo the effects of the earlier successful steps.

This is called compensation.

The compensation operations are:

| Forward operation | Compensation                     |
| ----------------- | -------------------------------- |
| Reserve inventory | Release inventory                |
| Authorize payment | Void payment authorization       |
| Create shipment   | Cancel shipment, where supported |

Notice something important:

Compensation is not a database rollback.

The coordinator cannot execute:

```
ROLLBACK;
```

on another service's committed transaction.

That transaction has already committed. It is over.

Instead, compensation is another business operation that semantically reverses the effect of the earlier operation.

For example:

```
Reserve inventory
        |
        v
Release inventory
```

Or:

```
Authorize payment
        |
        v
Void authorization
```

A payment refund is also a compensating action in some workflows, but it is not identical to voiding an authorization. A void cancels an authorization before settlement, whereas a refund reverses a completed payment through a new financial transaction.

### 11.1 Compensation order: reverse execution order

For a sequential Saga:

```
Forward execution:

1. Reserve Inventory
2. Authorize Payment
3. Create Shipment
```

If step 3 fails:

```
Compensation:

1. Void Payment Authorization
2. Release Inventory
```

This is reverse order.

Why?

Because later steps may depend on earlier steps.

Generally, undo the most recently completed business effects first.

However, this is a dependency rule, not an absolute law. A complex workflow can have parallel branches and business-specific compensation dependencies. In those cases, compensation should follow a dependency graph rather than blindly reversing every numerical sequence.

### 11.2 Persist compensation intent before executing it

When shipment creation fails, the coordinator should not directly call the payment service.

Instead, it starts a compensation transaction:

```
BEGIN;

UPDATE sagas
SET status = 'COMPENSATING',
    version = version + 1,
    updated_at = NOW()
WHERE id = :saga_id;

UPDATE saga_steps
SET status = 'COMPENSATION_PENDING'
WHERE saga_id = :saga_id
  AND step_name = 'AUTHORIZE_PAYMENT';

INSERT INTO outbox_messages (
    id,
    aggregate_id,
    message_type,
    payload
)
VALUES (
    :compensation_command_id,
    :saga_id,
    'VoidPaymentAuthorization',
    :payload
);

COMMIT;
```

The outbox dispatcher publishes the compensation command.

The Payment Service handles it using the same inbox/idempotency/outbox approach we discussed earlier.

When the payment service confirms that the authorization has been voided, the coordinator records the result and schedules the inventory release.

### 11.3 What if compensation fails?

Suppose:

```
Create Shipment:              FAILED
Void Payment Authorization:   SUCCEEDED
Release Inventory:            FAILED
```

The Saga is not compensated yet.

It should remain in a state such as:

```
COMPENSATING
```

The inventory release is retried according to its own compensation retry policy.

If automatic recovery is exhausted or the operation needs human intervention:

```
COMPENSATION_FAILED
```

The system should expose enough details for an operator to investigate:

- Original forward command ID.
- Compensation command ID.
- Reservation ID.
- Last attempted time.
- Error details.
- Number of attempts.
- Next retry time.
- Participant operation status.

An operator might be able to retry the release, invoke an approved manual remediation process, or confirm that the effect was already reversed.

Do not silently mark the whole Saga as rolled back.

### 11.4 Compensations themselves must be idempotent

Imagine this:

```
Release inventory
        |
        v
Inventory is released
        |
        v
Coordinator crashes before receiving response
        |
        v
Same compensation command is retried
```

The Inventory Service must not release the same reservation twice or accidentally modify unrelated stock.

Use a stable compensation command ID and a unique operation record.

For example:

```
reservation_id = reservation-123

compensation_operation_id = comp-123
```

The participant should also verify that the reservation is in a state that permits release.

A second release request should return the original successful result, not perform another business mutation.

### 11.5 What if an operation cannot be compensated?

This is a real-world constraint.

Consider sending a physical package.

Once it is handed over to a courier, the system might not be able to reverse the shipment immediately.

Instead, compensation may require:

```
Cancel shipment if not dispatched.
Otherwise:
    Create return request.
    Notify customer.
    Process refund according to policy.
```

Similarly, sending an email cannot truly be undone. The compensation might be sending a correction email.

So we need to design the Saga around business semantics, not pretend that all operations are mathematically reversible.

This is why compensation is better understood as a business-level corrective action rather than a rollback.

## 12. Let's put everything together: complete order workflow

Now let's walk through the complete execution, including the database operations and message flow.

### Step 0: Customer submits an order

The client sends:

```
POST /orders
Idempotency-Key: request-abc-123
```

The Order API asks the coordinator to start a Saga.

The coordinator generates:

```
saga_id = saga-123
order_id = order-456
```

It performs one local database transaction:

```
BEGIN;

INSERT INTO sagas (
    id,
    saga_type,
    business_id,
    status,
    idempotency_key
)
VALUES (
    'saga-123',
    'ORDER_FULFILLMENT',
    'order-456',
    'RUNNING',
    'request-abc-123'
);

INSERT INTO saga_steps (
    id, saga_id, step_name, sequence_number,
    status, command_id
)
VALUES (
    'step-1', 'saga-123', 'RESERVE_INVENTORY',
    1, 'COMMAND_QUEUED', 'cmd-100'
);

INSERT INTO outbox_messages (
    id, aggregate_id, message_type, payload
)
VALUES (
    'msg-100',
    'saga-123',
    'ReserveInventory',
    '{"orderId":"order-456","productId":"product-10","quantity":2}'
);

COMMIT;
```

The API can return:

```
202 Accepted
```

```
{
  "orderId": "order-456",
  "sagaId": "saga-123",
  "status": "PROCESSING"
}
```

Why `202 Accepted`?

Because accepting the workflow does not mean that the order has been fully processed.

The client can check the status later:

```
GET /orders/order-456
```

Or:

```
GET /sagas/saga-123
```

### Step 1: Reserve inventory

The outbox dispatcher publishes:

```
ReserveInventory
```

The Inventory Service:

1. Receives the message.
2. Checks its inbox using `command_id`.
3. Checks that sufficient inventory is available.
4. Creates a reservation.
5. Updates available/reserved inventory.
6. Stores the command result.
7. Adds a success event to its outbox.
8. Commits its database transaction.

The Inventory Service publishes:

```
InventoryReservationSucceeded
```

The coordinator consumes it.

In one transaction, it:

```
1. Deduplicates the incoming event.
2. Marks RESERVE_INVENTORY as SUCCEEDED.
3. Marks the next step as COMMAND_QUEUED.
4. Inserts AuthorizePayment into the outbox.
5. Commits.
```

### Step 2: Authorize payment

The Payment Service receives:

```
AuthorizePayment
```

It processes the request using its own inbox and operation records.

Suppose authorization succeeds.

It emits:

```
PaymentAuthorizationSucceeded
```

The coordinator updates its database:

```
RESERVE_INVENTORY = SUCCEEDED
AUTHORIZE_PAYMENT = SUCCEEDED
CREATE_SHIPMENT = COMMAND_QUEUED
```

It inserts `CreateShipment` into the outbox in the same transaction.

### Step 3: Create shipment

The Shipping Service receives:

```
CreateShipment
```

Suppose the courier integration is unavailable.

The Shipping Service returns a transient error or leaves the operation in progress, depending on what it knows.

Suppose it establishes that shipment creation genuinely failed.

It publishes:

```
ShipmentCreationFailed
```

The coordinator receives the event.

Its transaction does:

```
1. Deduplicate the event.
2. Mark CREATE_SHIPMENT as FAILED.
3. Mark the Saga as COMPENSATING.
4. Mark AUTHORIZE_PAYMENT compensation as pending.
5. Insert VoidPaymentAuthorization into the outbox.
6. Commit.
```

### Step 4: Void payment authorization

The Payment Service receives:

```
VoidPaymentAuthorization
```

It voids the authorization.

It publishes:

```
PaymentAuthorizationVoided
```

The coordinator records:

```
AUTHORIZE_PAYMENT = COMPENSATED
```

Then it schedules:

```
ReleaseInventory
```

### Step 5: Release inventory

The Inventory Service receives:

```
ReleaseInventory
```

It releases the reservation and publishes:

```
InventoryReservationReleased
```

The coordinator records:

```
RESERVE_INVENTORY = COMPENSATED
```

All required compensations have now completed.

The Saga becomes:

```
COMPENSATED
```

The final picture:

```
FORWARD EXECUTION
=================

Reserve Inventory        SUCCEEDED
        |
        v
Authorize Payment        SUCCEEDED
        |
        v
Create Shipment          FAILED


COMPENSATION
============

Void Payment             COMPENSATED
        |
        v
Release Inventory        COMPENSATED


FINAL SAGA STATUS
=================

COMPENSATED
```

This is the complete pattern.

One thing to notice: the coordinator never needed to keep one thread, HTTP connection, or database transaction alive throughout this entire workflow. It persisted progress and resumed when events arrived.

## 13. How do we handle concurrency and prevent conflicting workflow execution?

Now imagine 10,000 orders are being processed concurrently.

More importantly, imagine that two coordinator workers accidentally pick up the same Saga.

Both might see:

```
Current status = RUNNING
Current step = 2
```

Both could try to schedule the shipping command.

We need concurrency control.

### 13.1 Use optimistic locking for Saga state

We have a `version` column in the `sagas` table.

Suppose the current version is 10.

Worker A and Worker B both read version 10.

Worker A updates:

```
UPDATE sagas
SET status = 'RUNNING',
    version = 11
WHERE id = :saga_id
  AND version = 10;
```

One row is updated.

Worker B executes the same update, but no row matches because the version is now 11.

It must reload the Saga and reconsider what to do.

This prevents two workers from successfully applying the same state transition based on stale data.

### 13.2 Use row locks when necessary

For operations where we need to serialize updates, PostgreSQL row-level locking is useful.

```
BEGIN;

SELECT *
FROM sagas
WHERE id = :saga_id
FOR UPDATE;

-- Validate the current Saga state.
-- Apply the transition.
-- Update the next step.
-- Insert outbox command.

COMMIT;
```

Only one transaction can hold the conflicting row lock at a time.

For this design, I would generally use optimistic version checks for normal state transitions and short row-locked transactions when coordinating a step transition.

Do not lock a Saga and then call another service while holding the lock.

### 13.3 Enforce invariants with database constraints

Application-level checks alone are insufficient.

Useful constraints include:

```
UNIQUE(saga_type, idempotency_key)
UNIQUE(saga_id, sequence_number)
UNIQUE(command_id)
UNIQUE(consumer_name, message_id)
```

They prevent duplicate logical workflow creation, duplicate steps, duplicate commands, and duplicate event processing.

For a strictly sequential Saga, you should also ensure that only the expected next forward step can be started.

The database version and state-transition checks should enforce this, not just an in-memory `current_step` variable.

### 13.4 What about the same event arriving after compensation has started?

This is another reason to validate legal state transitions.

Suppose:

```
Shipment failed.
Saga entered COMPENSATING.
```

Then, due to message delay, an older `ShipmentSucceeded` event arrives.

The coordinator must not blindly change the Saga back to `RUNNING` or `COMPLETED`.

It should check:

- The command ID and step identity.
- Whether the event is already processed.
- Whether the step was previously marked failed.
- Whether that transition is legal.
- Whether the event represents a genuinely conflicting outcome that needs reconciliation.

A message's arrival time is not the same as the time its business operation occurred.

This is why event IDs, command IDs, sequence numbers, versions, and explicit state transitions all matter.

## 14. What happens if the coordinator itself crashes?

Let's examine every major failure point.

| Failure point                                                          | What happens after recovery?                                                               |
| ---------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| Before the workflow creation transaction commits                       | No workflow exists; the client can safely retry with the same idempotency key.             |
| After Saga creation commits but before the API responds                | Client retries; coordinator finds the existing Saga.                                       |
| Before the command is published                                        | Pending outbox message is eventually published.                                            |
| After publish but before marking outbox published                      | Message may be published again; participant inbox prevents duplicate business execution.   |
| Participant crashes before its transaction commits                     | Local transaction rolls back; command can be redelivered.                                  |
| Participant commits but crashes before acknowledging                   | Command may be redelivered; participant returns the stored result.                         |
| Participant commits but cannot publish its response                    | Participant outbox publishes it later.                                                     |
| Coordinator receives response but crashes before commit                | Broker redelivers; coordinator processes it.                                               |
| Coordinator commits response handling but crashes before acknowledging | Broker redelivers; coordinator inbox deduplicates it.                                      |
| Compensation succeeds but its response is lost                         | Compensation command/result can be retried or reconciled using the stable idempotency key. |
| Coordinator worker crashes during a retry delay                        | Retry schedule remains in the database; another worker can resume it.                      |

This is the real value of the design.

A process restart should affect availability and progress, not the correctness of the workflow.

The database contains enough durable information for a different worker to continue.

### Worker leases and recovery

For workers that claim jobs from PostgreSQL, use a lease:

```
claimed_by = worker-7
lease_until = 14:35:00
```

A worker periodically renews the lease while processing.

If it dies, the lease expires and another worker can claim the job.

Do not rely on a permanent `PROCESSING` status without a recovery mechanism. Otherwise, a crashed worker can strand work indefinitely.

## 15. What about cancellation while the Saga is running?

Real systems need cancellation too.

Suppose a customer cancels an order while payment authorization is in progress.

The coordinator must not simply set:

```
Saga status = CANCELLED
```

That would be unsafe because inventory or payment operations may already have happened.

Instead:

```
Customer cancellation requested
             |
             v
Coordinator validates current state
             |
             v
Stop scheduling new forward steps
             |
             v
Resolve in-flight operation outcomes
             |
             v
Compensate successful effects
             |
             v
Mark Saga CANCELLED
```

The tricky part is in-flight work.

Imagine a payment authorization is still pending when cancellation arrives.

The coordinator should not immediately assume that no authorization occurred. It must cancel the in-flight operation where possible or reconcile its outcome, and then compensate any successful authorization.

The cancellation request itself should be idempotent.

Also define business rules:

- Can an order be cancelled after shipment creation?
- Can a shipment be cancelled after dispatch?
- Is a partial cancellation allowed?
- Can payment be refunded after settlement?
- Is there a cancellation fee?

There is no generic technical answer to these questions. The state machine needs to encode the actual business policy.

## 16. How does this work at scale?

Let's say we start with:

```
1 coordinator
1 PostgreSQL database
1 outbox dispatcher
1 message broker
3 participant services
```

Initially, this is completely reasonable.

As traffic grows, we scale the components independently.

### 16.1 Scale the coordinator horizontally

Instead of one coordinator process:

```
                  API Load Balancer
                         |
           +-------------+-------------+
           |             |             |
      Coordinator  Coordinator   Coordinator
         Pod 1        Pod 2         Pod 3
           |             |             |
           +-------------+-------------+
                         |
                  PostgreSQL
```

All coordinator instances share the same durable state.

They do not share in-memory Saga objects.

Any worker can process any eligible Saga, subject to locking and ownership rules.

### 16.2 Scale workers independently

You can have different worker pools for different activities:

```
Coordinator DB
      |
      +---------------------+
      |                     |
      v                     v
 Forward Step Workers   Compensation Workers
      |                     |
      v                     v
 Forward Command        Compensation Command
      |                     |
      +----------+----------+
                 |
                 v
           Message Broker
```

Why separate these pools?

Imagine 20,000 workflows have entered compensation because an external dependency is down.

We don't want those compensation tasks to consume all worker capacity and prevent normal workflows from starting or progressing.

Separate pools, concurrency limits, and priorities provide isolation.

You can also have separate outbox dispatchers for different message categories.

### 16.3 Use message partitioning

If using Kafka, partition messages by `saga_id`.

```
Kafka Topic: saga-commands

Partition 0: saga-A, saga-D, saga-G
Partition 1: saga-B, saga-E, saga-H
Partition 2: saga-C, saga-F, saga-I
```

This helps maintain order for messages with the same partition key.

However, partition ordering is not a replacement for state validation.

A delayed message, retry topic, duplicate, or independently produced event may still arrive at an unexpected point in the workflow.

Always validate the expected step, command ID, and workflow version.

With queue-based systems, ordering may require FIFO queues or explicit sequence checks. You should not assume a standard queue provides strict per-Saga ordering.

### 16.4 Database connection and worker management

Suppose you have 100 worker instances, each with a pool of 20 PostgreSQL connections.

That's potentially 2,000 connections.

Your database might not be configured to handle this.

Use connection pooling, bounded concurrency, batch operations, and separate worker limits.

Do not scale worker count without considering database capacity and the downstream services' capacity.

### 16.5 Prevent one failing dependency from affecting everything

Suppose the Payment Service is down.

Without control mechanisms, thousands of workers may repeatedly send payment commands.

Use:

- Bounded concurrency per participant.
- Circuit breakers.
- Exponential backoff and jitter.
- Rate limits.
- Separate retry queues or scheduled retry tables.
- Backpressure when outbox or broker lag grows.

When payment becomes healthy, gradually resume traffic instead of releasing every waiting command at once.

### 16.6 What about high-volume database growth?

The coordinator database will store:

- Active Saga state.
- Step execution state.
- Outbox messages.
- Incoming message deduplication.
- Workflow event history.

These have different retention and access patterns.

I would:

1. Keep active workflow state readily accessible.
2. Index rows needed by workers and schedulers.
3. Partition large append-only event/history tables by time.
4. Archive or delete historical records after the required retention period.
5. Keep deduplication records for at least the maximum period in which old commands/events can be redelivered.
6. Avoid storing large binary files in JSONB; store references to object storage instead.

Useful indexes might include:

```
CREATE INDEX idx_saga_steps_retry
ON saga_steps (next_attempt_at)
WHERE status = 'RETRY_WAIT';

CREATE INDEX idx_outbox_pending
ON outbox_messages (available_at, created_at)
WHERE status = 'PENDING';

CREATE INDEX idx_sagas_status
ON sagas (status, updated_at);
```

For outbox tables, use batch claiming with `FOR UPDATE SKIP LOCKED`.

Also be careful about how you mark outbox messages as published. Avoid repeatedly scanning a huge table containing years of already-published messages.

### 16.7 What if PostgreSQL itself becomes the bottleneck?

You have several options:

First: optimize one PostgreSQL deployment.

A well-designed PostgreSQL instance can handle a substantial amount of workflow traffic.

Use:

- Appropriate indexes.
- Short transactions.
- Efficient outbox polling.
- Connection pooling.
- Partitioning where justified.
- Read replicas for read-heavy operational queries.

Do not send coordination decisions to a lagging read replica. State transitions must use the authoritative primary or another consistency mechanism that provides the required guarantees.

Second: partition workflows by tenant or workflow ID.

For example:

```
Tenant group A -> PostgreSQL shard 1
Tenant group B -> PostgreSQL shard 2
Tenant group C -> PostgreSQL shard 3
```

Or route by a stable hash of `saga_id`.

The important design principle is to keep the Saga's state, its steps, and its coordination outbox on the same database shard.

That allows their state transitions to remain locally atomic.

Third: separate operational history from active state.

Older event history can be archived without affecting active workflow processing.

Do not introduce database sharding just because the design is distributed. Introduce it when measured load, storage, or availability requirements justify the operational complexity.

## 17. Which database should we use?

Now let's compare the choices specifically for the coordinator's workflow database.

### PostgreSQL

My default recommendation for this design.

Why?

The coordinator needs strong transactional guarantees across several related records.

For example, when a participant response arrives, the coordinator may need to atomically:

```
1. Deduplicate the event.
2. Mark the current step successful.
3. Update the Saga version/status.
4. Create the next step.
5. Insert the next command into the outbox.
```

PostgreSQL handles this naturally with one transaction.

It also provides:

| Capability                             | Why it matters                                            |
| -------------------------------------- | --------------------------------------------------------- |
| ACID transactions                      | Multiple workflow records can be updated atomically.      |
| Unique constraints                     | Enforce idempotency and prevent duplicate steps/messages. |
| Row-level locking                      | Coordinate workers safely.                                |
| Optimistic updates                     | Prevent conflicting state transitions.                    |
| JSONB                                  | Store flexible command/result payloads.                   |
| Indexing                               | Efficiently find pending work and retries.                |
| Partitioning                           | Manage large historical/workflow tables.                  |
| Mature replication and backup features | Support availability and disaster recovery.               |

It is also convenient to inspect a problematic workflow manually:

```
SELECT *
FROM sagas
WHERE id = :saga_id;

SELECT *
FROM saga_steps
WHERE saga_id = :saga_id
ORDER BY sequence_number;

SELECT *
FROM saga_events
WHERE saga_id = :saga_id
ORDER BY created_at;
```

For a typical enterprise application coordinating several service transactions, PostgreSQL is a very strong starting point.

### DynamoDB

DynamoDB can also work.

For example, you could model:

```
PK = SAGA#saga-123
SK = METADATA

PK = SAGA#saga-123
SK = STEP#001

PK = SAGA#saga-123
SK = STEP#002

PK = SAGA#saga-123
SK = EVENT#001
```

And use conditional updates and DynamoDB transactions to protect state transitions.

It can be attractive when:

- You have very high throughput.
- Your access patterns are predictable.
- You want a managed, horizontally scalable key-value/document database.
- You have a strong AWS-centric architecture.
- You can design your workflow data around partition keys and conditional writes.

But you need to think carefully about:

- Conditional transitions.
- Transaction limits.
- Query patterns.
- Secondary indexes.
- TTL and retention.
- Idempotency.
- Outbox dispatch using Streams or another mechanism.
- Hot partition avoidance.

For example, you cannot simply assume that a DynamoDB Stream and a workflow state update constitute one atomic transaction with an external message broker. The same dual-write problem must still be solved.

### My comparison

| Requirement                       | PostgreSQL                             | DynamoDB                                             |
| --------------------------------- | -------------------------------------- | ---------------------------------------------------- |
| Multi-record transactions         | Excellent                              | Supported, with constraints                          |
| Relational workflow data          | Natural                                | Requires deliberate modeling                         |
| Unique constraints                | Native                                 | Usually implemented with conditional writes and keys |
| Complex operational queries       | Excellent                              | More limited and access-pattern driven               |
| Very high horizontal throughput   | Requires planning/scaling architecture | Strong managed horizontal scaling                    |
| Flexible workflow history queries | Straightforward                        | Requires indexing/modeling                           |
| Atomic state + outbox update      | Straightforward                        | Possible with appropriate transaction design         |
| Operational debugging             | Convenient SQL                         | Requires more specialized queries                    |

For the workflow you described, I would choose PostgreSQL first.

DynamoDB is a valid choice, but it is not automatically a better choice just because the system is distributed.

The coordinator does not need to be a massively horizontally distributed database simply because it coordinates distributed services.

### What about Redis?

I would not use Redis as the authoritative database for this workflow.

Redis is excellent for:

- Caching.
- Distributed rate limiting.
- Short-lived coordination.
- Worker leases in some designs.
- Fast counters.

But the coordinator must durably preserve:

- Workflow state.
- Step execution history.
- Pending commands.
- Retry schedules.
- Compensation state.
- Deduplication records.

Those are critical business records.

Use a durable database as the source of truth. Redis can complement it.

### What about Kafka?

Kafka is a message transport and durable event log, not a direct replacement for the coordinator's workflow database.

Kafka is useful for:

- Publishing commands/events.
- Buffering traffic.
- Retaining event streams.
- Partitioning messages.
- Replaying messages under controlled conditions.

But we still need an authoritative place to answer:

```
What is the current state of Saga saga-123?
Which steps succeeded?
Which compensation is pending?
When should the next retry happen?
```

Kafka can help distribute work, but it does not remove the need for durable workflow-state management.

## 18. An important issue: Saga consistency and isolation

So far, we have discussed reliability, duplicate messages, retries, and compensation.

There is another important limitation of Saga: it does not provide global ACID isolation.

Consider:

```
Customer A starts ordering the last available item.

Inventory reservation succeeds.

Payment authorization is in progress.
```

At this point, another customer may interact with the same product or the reservation may expire.

There is no single global transaction holding a database lock across all services.

The workflow has intermediate states.

This means your business system must explicitly manage those states.

### Example: Inventory reservation

Instead of simply subtracting inventory forever, model a reservation:

```
available_quantity = 10

Customer A reserves 2:

available_quantity = 8
reserved_quantity = 2
```

A reservation can have an expiry:

```
reservation_id = reservation-123
status = RESERVED
expires_at = 14:45:00
```

If payment succeeds, confirm the reservation.

If payment fails, release it.

If the Saga crashes permanently, an expiry/reconciliation mechanism can help recover abandoned reservations.

Be careful: an expiry process must not release stock while the payment operation is still genuinely in flight. It should coordinate with the workflow or enforce a lease/renewal protocol.

### Common approaches for Saga isolation

| Technique                 | Purpose                                                |
| ------------------------- | ------------------------------------------------------ |
| Reservations              | Temporarily hold resources such as inventory or seats. |
| Semantic locks            | Mark a business entity as being processed.             |
| Optimistic version checks | Prevent stale updates.                                 |
| State validation          | Reject operations that are no longer legal.            |
| Expiring holds            | Prevent indefinitely abandoned reservations.           |
| Escrow techniques         | Control concurrent use of limited resources.           |
| Reconciliation            | Discover and repair inconsistent or stranded states.   |

For example, consider two customers booking the same last movie seat.

The reservation service should enforce seat ownership using a local atomic database operation, such as:

```
UPDATE show_seats
SET status = 'HELD',
    hold_id = :hold_id,
    held_until = :deadline
WHERE show_id = :show_id
  AND seat_id = :seat_id
  AND status = 'AVAILABLE';
```

Proceed only if exactly one row was updated.

The Saga then coordinates payment and booking confirmation.

This protects the core invariant even though the entire workflow is not one ACID transaction.

## 19. Reconciliation: the reliability mechanism people often forget

Retries and messaging are not enough.

A production coordinator also needs periodic reconciliation.

Why?

Because sometimes a system can reach a state where automatic mechanisms no longer make progress.

Examples:

```
Saga status = RUNNING
No progress for 30 minutes.

Step status = IN_PROGRESS
No response received.

Compensation status = COMPENSATING
No successful response after several attempts.

Outbox message status = PENDING
Message has not been published for 10 minutes.
```

A reconciliation worker scans for these conditions.

For each case, it determines whether to:

- Retry delivery.
- Re-query a participant.
- Retry a business operation with the same idempotency key.
- Resume compensation.
- Escalate to an operator.
- Mark the workflow as requiring manual resolution.

A particularly useful reconciliation technique is comparing the coordinator's state against participant operation records.

For example:

```
Coordinator believes:
    Payment authorization = OUTCOME_UNKNOWN

Payment Service reports:
    Authorization = SUCCEEDED

Coordinator action:
    Record success and continue.
```

Or:

```
Coordinator believes:
    Inventory release = COMPENSATION_PENDING

Inventory Service reports:
    Reservation = RELEASED

Coordinator action:
    Record compensation success.
```

This lets the system recover even when an event was lost, delayed, misrouted, or moved to a dead-letter queue.

### Useful operational metrics

At minimum, monitor:

- Number of active Sagas.
- Number of Sagas stuck in each state.
- Oldest pending outbox message.
- Outbox publication failure rate.
- Retry rate per participant.
- Compensation success/failure rate.
- Average Saga completion time.
- Unknown-outcome count.
- DLQ depth.
- Database connection saturation.
- Message broker consumer lag.
- Workflows awaiting manual intervention.

A system is not reliable merely because it has retries. It is reliable when failures are visible, recoverable, and operationally manageable.

## 20. Does this design work with HTTP, Kafka, and SQS?

Yes. The reliability principles do not depend on a particular communication mechanism.

The difference is how commands are delivered and how responses come back.

### Option A: HTTP-based orchestration

```
Coordinator
    |
    | HTTP request
    v
Inventory Service
    |
    | HTTP response
    v
Coordinator
```

This can work, provided the coordinator:

1. Persists the command before sending the request.
2. Uses a stable idempotency key.
3. Does not hold a database transaction open during the HTTP call.
4. Persists the result after receiving a response.
5. Handles timeouts as unknown outcomes.
6. Has a recovery worker to retry or query unfinished operations.

For example:

```
POST /inventory/reservations
Idempotency-Key: cmd-100
```

The Inventory Service should persist the result associated with that key.

HTTP does not remove the need for the outbox. You can use the outbox dispatcher to initiate HTTP requests instead of publishing broker messages.

For short-running workflows with low-to-moderate throughput, this can be perfectly reasonable.

### Option B: SQS-based orchestration

```
Coordinator DB
      |
      v
Coordinator Outbox
      |
      v
   SQS Queue
      |
      v
Inventory Consumer
      |
      v
Inventory DB
      |
      v
SQS Response Queue
      |
      v
Coordinator Consumer
```

SQS provides durable queuing, buffering, and visibility-timeout-based delivery.

The consumer must acknowledge the message only after its local transaction commits.

Messages can be delivered more than once. The inbox/idempotency design is therefore still essential.

A dead-letter queue is useful for messages that repeatedly fail processing, but a DLQ is not an automatic business compensation mechanism. The coordinator must still reconcile the corresponding Saga.

### Option C: Kafka-based orchestration

```
Coordinator
     |
     v
Outbox
     |
     v
Kafka: saga-commands
     |
     v
Inventory Consumer
     |
     v
Inventory DB + Outbox
     |
     v
Kafka: saga-results
     |
     v
Coordinator Consumer
```

Kafka is attractive when:

- Event throughput is high.
- Many services consume the same events.
- Event retention and replay are important.
- Per-Saga message ordering is useful.
- There are multiple independent downstream consumers.

Use `saga_id` as a message key when you need events for one Saga to go to the same partition.

Kafka's idempotent producer and transactions can improve guarantees within Kafka, but they do not automatically make PostgreSQL updates and external service operations exactly-once.

The participant still needs a reliable database/message boundary.

### How I would choose

| Scenario                                                          | Reasonable starting choice             |
| ----------------------------------------------------------------- | -------------------------------------- |
| Small application, few services, simple workflow                  | HTTP + PostgreSQL outbox               |
| General asynchronous business workflows                           | SQS + PostgreSQL                       |
| High-volume event-driven architecture                             | Kafka + PostgreSQL                     |
| AWS-native application with predictable key-value access patterns | SQS/Kafka + DynamoDB can be considered |

The orchestrator's state model remains essentially the same across these choices.

## 21. Handling different kinds of real-world workflows

We should not design a Saga engine that only supports a hardcoded sequence of three operations.

Real workflows can have different structures.

### 21.1 Sequential workflows

Example: order fulfillment.

```
Reserve Inventory
       |
       v
Authorize Payment
       |
       v
Create Shipment
       |
       v
Confirm Order
```

Only start the next step after the previous step succeeds.

This is the simplest Saga shape.

### 21.2 Conditional workflows

Example: payment processing.

```
Validate Order
      |
      v
Check Payment Method
      |
      v
   Decision
   /      \
Card      Wallet
 |          |
Authorize  Debit
 |          |
 +----+-----+
      |
      v
Continue Order
```

The coordinator evaluates a condition and decides which next step to enqueue.

The chosen path must be persisted so that recovery does not make a different decision after a restart because the business data has changed.

### 21.3 Parallel workflows

Suppose an order needs both inventory reservation and fraud verification.

These are independent:

```
              Validate Order
                    |
          +---------+---------+
          |                   |
          v                   v
   Reserve Inventory    Fraud Verification
          |                   |
          +---------+---------+
                    |
             Both succeed?
                    |
                    v
             Authorize Payment
```

The coordinator should record the results independently and move forward only when the required dependencies are satisfied.

If fraud verification fails after inventory has been reserved, compensate the inventory reservation.

If multiple branches succeed, all applicable compensation operations must be considered.

This is why a general-purpose coordinator should model step dependencies rather than assume every workflow is a simple sequence.

### 21.4 Long-running workflows

Examples:

- Account onboarding.
- Insurance claim processing.
- Loan approval.
- Manual document verification.
- Merchant registration.
- Provisioning cloud infrastructure.

Some steps might take hours or days.

For example:

```
Create Account
      |
      v
Verify Documents
      |
      v
WAIT FOR HUMAN APPROVAL
      |
      v
Configure Account
      |
      v
Activate Account
```

The workflow remains durably stored while waiting.

The worker does not stay alive for the entire waiting period.

When the human approval event arrives, the coordinator resumes the workflow.

### 21.5 Steps that do not have a meaningful compensation

Some actions cannot be reversed.

For example:

- Sending a notification.
- Sending an email.
- Publishing an externally consumed event.
- Dispatching a physical shipment.

For these steps, decide whether the action should happen early or late, whether it needs a corrective action, and what business outcome to expose if the Saga later fails.

For example, send a customer confirmation only after the order has reached the appropriate committed business state.

A reliable workflow definition must describe the business semantics of each step, not just its forward API and reverse API.

## 22. How would I make this a reusable coordination service?

So far, our example has used hardcoded step names.

For a real reusable Saga orchestrator, I'd separate workflow definitions from workflow executions.

A workflow definition describes what can happen.

An execution records what actually happened for one particular business operation.

For example, an order workflow definition could look conceptually like this:

```
workflow: ORDER_FULFILLMENT
version: 1

steps:
  - name: RESERVE_INVENTORY
    service: inventory
    action: reserve
    compensation: release
    next: AUTHORIZE_PAYMENT

  - name: AUTHORIZE_PAYMENT
    service: payment
    action: authorize
    compensation: void_authorization
    next: CREATE_SHIPMENT

  - name: CREATE_SHIPMENT
    service: shipping
    action: create
    compensation: cancel
    next: CONFIRM_ORDER

  - name: CONFIRM_ORDER
    service: orders
    action: confirm
    terminal: true
```

This is illustrative configuration, not a complete workflow-definition language.

A production definition should also specify:

- Step input/output contracts.
- Dependencies and branching conditions.
- Timeout policy.
- Retry policy.
- Compensation policy.
- Whether a step is optional.
- Whether it can execute in parallel.
- What constitutes a permanent business failure.
- Version and compatibility rules.

### Why workflow versioning matters

Imagine an order Saga was started using workflow version 1.

A new deployment introduces workflow version 2, which adds fraud verification.

What happens to existing workflows?

You should not silently apply the new workflow definition to old executions.

Persist the selected workflow version when creating the Saga:

```
saga_id = saga-123
workflow_type = ORDER_FULFILLMENT
workflow_version = 1
```

Existing workflows continue with the definition they started with, while new workflows use version 2.

Otherwise, deployment changes can alter the meaning of an already-running business transaction.

### What should the reusable engine actually do?

I would give it these core responsibilities:

| Engine capability            | Responsibility                                     |
| ---------------------------- | -------------------------------------------------- |
| Workflow definition registry | Knows the permitted steps and transitions.         |
| Execution manager            | Creates and loads Saga instances.                  |
| State machine                | Validates and applies transitions.                 |
| Command dispatcher           | Sends forward and compensation commands.           |
| Event handler                | Processes participant outcomes.                    |
| Retry scheduler              | Finds operations ready for another attempt.        |
| Timeout manager              | Detects operations exceeding their deadline.       |
| Compensation manager         | Executes reverse business operations.              |
| Recovery/reconciliation      | Finds workflows that have stopped making progress. |
| Observability                | Exposes current state, history, and errors.        |

Keep participant-specific business rules inside the participant services.

The coordinator decides what should happen next. The participant decides how its business operation is performed safely.

This separation is very important when the same coordination engine is used for many domains.

## 23. Exactly-once execution: what can we actually guarantee?

You will often hear statements such as:

- Kafka guarantees exactly-once processing.
- SQS guarantees at-least-once delivery.
- The consumer is idempotent, so the operation happens exactly once.

Let's be precise about this.

Across independent databases, services, and external systems, we cannot generally guarantee that a remote operation is physically executed exactly once.

For example:

```
Payment Service sends authorization request.
Payment Gateway authorizes the payment.
Payment Service crashes before receiving the response.
```

The Payment Service does not know whether authorization happened.

It might need to retry or query the gateway.

We therefore design around:

At-least-once delivery + idempotent business operations + durable state + reconciliation.

The goal is to ensure that repeated delivery of the same logical command does not produce repeated business effects.

### Important: distinguish these identifiers

This is worth getting right in your implementation.

| Identifier                | Purpose                                        | Changes on retry?                            |
| ------------------------- | ---------------------------------------------- | -------------------------------------------- |
| `saga_id`                 | Identifies the overall workflow                | No                                           |
| `step_id`                 | Identifies a step execution                    | No                                           |
| `command_id`              | Identifies one logical business operation      | No for delivery retries of that operation    |
| `message_id`              | Identifies a particular command/event envelope | No when republishing the same outbox message |
| `compensation_command_id` | Identifies one logical compensation operation  | No for delivery retries                      |
| `attempt_number`          | Identifies an execution attempt                | Yes                                          |

There is one important nuance about retrying business operations.

Suppose a participant returns a definitive business rejection:

```
Reserve inventory
Result: INSUFFICIENT_INVENTORY
```

That result should be stored against the command ID. Replaying the same command should return the same result.

If the business later decides to try again after inventory changes, that is a new logical attempt with a new command identity, under an explicit workflow policy.

On the other hand, if a transient infrastructure failure occurs before the participant commits any operation, its transaction should roll back, including the inbox record. Redelivery of the same command can then safely execute it.

Do not treat every error as a final result and permanently cache it.

This distinction prevents a lot of subtle idempotency bugs.

## 24. A few more production rules worth following

These are important enough to make explicit.

1\. Never call remote services inside an open database transaction.

Persist intent, commit, and then communicate through an outbox or an appropriate durable dispatch mechanism.

2\. Never let the participant modify the coordinator's database.

Each service owns its database. Communicate through commands and events.

3\. Never assume timeout means failure.

An operation may have succeeded even though its response was lost. Resolve unknown outcomes using idempotent retries, operation-status queries, or reconciliation.

4\. Never assume compensation is guaranteed to succeed.

Compensation is another distributed operation that needs persistence, retries, idempotency, and operational visibility.

5\. Never depend on a worker remaining alive.

Persist the workflow state, retry schedules, and unfinished operations.

6\. Never use a new idempotency key for a delivery retry.

Otherwise, the participant could interpret the retry as a new business operation.

7\. Never hold locks across network calls.

Use short transactions and durable state transitions.

8\. Never rely only on an in-memory timeout.

Store operation deadlines and have a recovery/scheduling mechanism find overdue work.

9\. Never assume the Saga provides global isolation.

Protect important business invariants inside the individual services using reservations, conditional updates, version checks, and other appropriate techniques.

10\. Never consider a Saga completed merely because all forward commands were dispatched.

A Saga is complete only when all required business operations have reached their successful terminal states.

## 25. My recommended implementation for this particular problem

Putting everything together, this is the architecture I would build.

\#chatgpt-mermaid-\_r_7e4\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_7e4\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_7e4\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_7e4\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_7e4\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7e4\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_7e4\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_7e4\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_7e4\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_7e4\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_7e4\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_7e4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7e4\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7e4\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_7e4\_ p{margin:0;}#chatgpt-mermaid-\_r_7e4\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7e4\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7e4\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7e4\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_7e4\_ .label text,#chatgpt-mermaid-\_r_7e4\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7e4\_ .node rect,#chatgpt-mermaid-\_r_7e4\_ .node circle,#chatgpt-mermaid-\_r_7e4\_ .node ellipse,#chatgpt-mermaid-\_r_7e4\_ .node polygon,#chatgpt-mermaid-\_r_7e4\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_7e4\_ .rough-node .label text,#chatgpt-mermaid-\_r_7e4\_ .node .label text,#chatgpt-mermaid-\_r_7e4\_ .image-shape .label,#chatgpt-mermaid-\_r_7e4\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_7e4\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_7e4\_ .rough-node .label,#chatgpt-mermaid-\_r_7e4\_ .node .label,#chatgpt-mermaid-\_r_7e4\_ .image-shape .label,#chatgpt-mermaid-\_r_7e4\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_7e4\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_7e4\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7e4\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7e4\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_7e4\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_7e4\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_7e4\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_7e4\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_7e4\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_7e4\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_7e4\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7e4\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7e4\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_7e4\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7e4\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_7e4\_ .icon-shape,#chatgpt-mermaid-\_r_7e4\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_7e4\_ .icon-shape p,#chatgpt-mermaid-\_r_7e4\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_7e4\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_7e4\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_7e4\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_7e4\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_7e4\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_7e4\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_7e4\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_7e4\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_7e4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7e4\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_7e4\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_7e4\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_7e4\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7e4\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_7e4\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_7e4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7e4\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_7e4\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_7e4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7e4\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_7e4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7e4\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_7e4\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_7e4\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_7e4\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_7e4\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_7e4\_ .node rect,#chatgpt-mermaid-\_r_7e4\_ .node circle,#chatgpt-mermaid-\_r_7e4\_ .node ellipse,#chatgpt-mermaid-\_r_7e4\_ .node polygon,#chatgpt-mermaid-\_r_7e4\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_7e4\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_7e4\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_7e4\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_7e4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7e4\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Shipping ServicePayment ServiceInventory ServiceSaga Coordination ServiceShipping ServiceShipping DBShipping OutboxPayment ServicePayment DBPayment OutboxInventory ServiceInventory DBInventory OutboxPostgreSQLState Machine / WorkflowWorkersRetry and Timeout SchedulerOutbox DispatcherEvent HandlerClientCoordinator APIMessage Broker

### Database choice

| Component                  | My recommendation                                                              |
| -------------------------- | ------------------------------------------------------------------------------ |
| Coordinator state          | PostgreSQL                                                                     |
| Coordinator step execution | PostgreSQL                                                                     |
| Coordinator outbox/inbox   | PostgreSQL                                                                     |
| Coordinator event history  | PostgreSQL, with archival/partitioning as needed                               |
| Inventory service          | Its own suitable transactional database                                        |
| Payment service            | Its own suitable transactional database                                        |
| Shipping service           | Its own suitable transactional database                                        |
| Message transport          | SQS for queue-oriented workflows; Kafka for high-volume event-driven workflows |

PostgreSQL is the coordinator's source of truth. The broker is responsible for message transport, not workflow correctness.

### Core transaction boundaries

There are four particularly important transaction boundaries in this design.

| Transaction                        | What must commit together?                                                         |
| ---------------------------------- | ---------------------------------------------------------------------------------- |
| Coordinator creates/advances Saga  | State transition + step update + next outbox command                               |
| Participant processes command      | Inbox/dedup record + local business operation + result + response outbox           |
| Coordinator processes response     | Incoming-event deduplication + step result + Saga transition + next outbox command |
| Participant processes compensation | Compensation deduplication + reverse business operation + result + response outbox |

These four boundaries are the foundation of the whole implementation.

### Recommended implementation order

I would build it incrementally:

1. Implement the Saga state machine and PostgreSQL schema.
2. Implement Saga creation with client idempotency.
3. Implement the transactional outbox and dispatcher.
4. Implement participant inbox processing and idempotent operations.
5. Implement response consumption and atomic state transitions.
6. Implement failure classification, timeout handling, and retries.
7. Implement compensation as an independent workflow operation.
8. Implement worker leases and crash recovery.
9. Implement reconciliation, monitoring, and operator recovery.
10. Add parallel steps, conditional branches, and workflow versioning as requirements demand.

Don't start by implementing a highly generic workflow engine. First get one sequential workflow correct under crashes, duplicates, timeouts, and compensation failures. Then generalize it.

## 26. Final mental model

The easiest way to remember the entire design is to think about three separate responsibilities.

The coordinator is responsible for deciding what should happen.

It stores the Saga's current state, knows which step should execute, detects failures, and decides when compensation is required.

The participant is responsible for safely performing its own business operation.

It uses its own database transaction, idempotency records, and outbox. It never relies on the coordinator to protect its own business data.

The message infrastructure is responsible for transporting commands and events.

It provides reliable delivery and buffering, but duplicates and delays must be expected.

The complete process is:

```
        COORDINATOR
             |
             v
    Persist next intention
             |
             v
       Commit DB
             |
             v
       Publish command
             |
             v
        PARTICIPANT
             |
             v
     Deduplicate command
             |
             v
   Execute local transaction
             |
             v
      Store result + event
             |
             v
       Commit DB
             |
             v
      Publish response
             |
             v
        COORDINATOR
             |
             v
    Deduplicate response
             |
             v
   Persist workflow outcome
             |
             v
      Decide next action
             |
             +------------------+
             |                  |
             v                  v
        Next step          Compensation
```

The underlying reliability philosophy is:

- Crash safety: Every important decision is durable.
- Delivery safety: A message can be delivered more than once.
- Execution safety: The same logical business operation cannot accidentally execute twice.
- Recovery safety: An independent worker can resume unfinished workflows.
- Compensation safety: Previously committed business effects can be corrected through explicit, idempotent operations.
- Operational safety: Unresolved workflows remain visible and can be reconciled or manually remediated.

One final recommendation: when implementing this in production, also evaluate Temporal. It provides durable workflow execution, timers, retries, and recovery, so you may not need to build all the orchestration infrastructure yourself. However, it does not eliminate the need for idempotent participant operations, correct database transaction boundaries, or business-level compensation.

The central lesson: A reliable Saga orchestrator does not try to make a distributed transaction behave like one giant database transaction. It makes every individual operation durable, repeatable, observable, and recoverable, then coordinates them through a persistent state machine.

