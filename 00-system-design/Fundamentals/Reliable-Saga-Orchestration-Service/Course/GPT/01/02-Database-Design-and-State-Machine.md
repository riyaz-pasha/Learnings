# Chapter 2 — Database Design and State Machine

Now that we understand why workers exist, let's answer a more fundamental question:

What exactly should the orchestrator store in its database so that any worker, at any point in time, can understand what has happened and what needs to happen next?

This is probably the most important design decision in the entire orchestrator.

We'll build the database from the business requirements rather than randomly creating tables.

## 1. First, understand what we need to remember

Let's take our Order Checkout saga.

```
1. CREATE_ORDER
2. RESERVE_INVENTORY
3. CHARGE_PAYMENT
4. CREATE_SHIPMENT
```

Suppose execution reaches this point:

```
CREATE_ORDER        SUCCEEDED
RESERVE_INVENTORY   SUCCEEDED
CHARGE_PAYMENT      RUNNING
CREATE_SHIPMENT     BLOCKED
```

Now imagine the worker crashes.

A new worker starts. It needs to answer:

1. Which saga is this?
2. Which workflow is being executed?
3. Which steps have completed?
4. Which step is currently executing?
5. What happens if the previous worker never returns?
6. Which service should I call?
7. What payload should I send?
8. Which idempotency key should I use?
9. How many times has this operation been attempted?
10. When should I retry?
11. What should I compensate if this operation permanently fails?

Our database must contain enough durable information to answer these questions.

Let's derive the tables.

# 2. Table 1: `sagas` — One row per distributed transaction

First, we need a table representing the overall workflow.

```
CREATE TABLE sagas (
    id UUID PRIMARY KEY,

    saga_type VARCHAR(100) NOT NULL,
    definition_version INTEGER NOT NULL,

    business_reference VARCHAR(255),

    status VARCHAR(50) NOT NULL,

    input_payload JSONB NOT NULL,
    context JSONB,

    failure_code VARCHAR(100),
    failure_message TEXT,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);
```

Example record:

| Column             | Value                         |
| ------------------ | ----------------------------- |
| id                 | SAGA-1001                     |
| saga_type          | ORDER_CHECKOUT                |
| definition_version | 1                             |
| business_reference | ORDER-123                     |
| status             | RUNNING                       |
| input_payload      | Order/customer/amount details |
| context            | Intermediate results          |

Let's understand the fields that matter.

### `id`

Unique identifier for this particular saga execution.

Every order checkout gets its own saga ID.

```
ORDER-123 -> SAGA-1001
ORDER-124 -> SAGA-1002
ORDER-125 -> SAGA-1003
```

Do not confuse `saga_id` with `order_id`.

An order is a business entity. A saga is an execution of a distributed workflow.

### `saga_type`

Identifies the business workflow.

For example:

```
ORDER_CHECKOUT
MONEY_TRANSFER
CUSTOMER_ONBOARDING
HOTEL_BOOKING
PRODUCT_RETURN
```

The orchestrator uses this to find the appropriate workflow definition.

### `definition_version`

This is something many basic implementations overlook.

Imagine your application initially has:

```
ORDER_CHECKOUT v1

1. Create Order
2. Reserve Inventory
3. Charge Payment
```

Later, you deploy:

```
ORDER_CHECKOUT v2

1. Create Order
2. Validate Address
3. Reserve Inventory
4. Charge Payment
```

What happens to sagas that were started using v1 but are still running?

Should they suddenly start executing v2?

Usually not.

An in-flight saga should continue using the workflow definition it started with, unless you explicitly implement a migration.

Therefore, store the workflow definition version.

This is important for production deployments.

### `business_reference`

Identifies the business entity associated with the workflow.

Examples:

```
ORDER-123
TRANSFER-456
BOOKING-789
```

This helps with debugging, querying and reconciliation.

### `input_payload`

Stores the original input required to execute the workflow.

For example:

```
{
  "orderId": "ORDER-123",
  "customerId": "CUSTOMER-456",
  "amount": 2500,
  "currency": "INR"
}
```

Why store it?

Because a worker might execute this saga several hours after it was submitted.

It must not depend on the original HTTP request or the original API instance still being alive.

### `context`

This deserves special attention.

A workflow often needs information returned by previous steps.

For example:

Order Service returns:

```
{
  "orderId": "ORDER-123",
  "status": "CREATED"
}
```

Inventory Service returns:

```
{
  "reservationId": "RES-987"
}
```

Payment Service returns:

```
{
  "paymentId": "PAY-456"
}
```

Later operations and compensations may need these identifiers.

We can persist those results in the saga context:

```
{
  "orderId": "ORDER-123",
  "reservationId": "RES-987",
  "paymentId": "PAY-456"
}
```

This is essentially the accumulated execution context of the saga.

Important: Store the minimum durable information needed for future steps and compensation. Don't blindly store sensitive payment data or large response bodies.

# 3. Table 2: `saga_steps` — One row per logical operation

The saga table answers:

> What is the overall state of this business workflow?

But it cannot tell us everything that happens inside the workflow.

For that, we need the steps table.

```
CREATE TABLE saga_steps (
    id UUID PRIMARY KEY,

    saga_id UUID NOT NULL REFERENCES sagas(id),

    step_key VARCHAR(100) NOT NULL,
    step_order INTEGER NOT NULL,

    forward_status VARCHAR(50) NOT NULL,

    compensation_status VARCHAR(50) NOT NULL,

    forward_idempotency_key VARCHAR(255),
    compensation_idempotency_key VARCHAR(255),

    forward_request JSONB,
    compensation_request JSONB,

    forward_response JSONB,
    compensation_response JSONB,

    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    UNIQUE (saga_id, step_key),
    UNIQUE (saga_id, step_order)
);
```

For our example:

| Step              | Order | Forward status | Compensation status |
| ----------------- | ----- | -------------- | ------------------- |
| CREATE_ORDER      | 1     | SUCCEEDED      | NOT_STARTED         |
| RESERVE_INVENTORY | 2     | SUCCEEDED      | NOT_STARTED         |
| CHARGE_PAYMENT    | 3     | READY          | NOT_STARTED         |
| CREATE_SHIPMENT   | 4     | BLOCKED        | NOT_REQUIRED        |

Notice something:

I am using two different status fields:

- `forward_status`
- `compensation_status`

Why?

Because forward execution and compensation are two different processes.

For example:

```
Forward:
RESERVE_INVENTORY = SUCCEEDED

Compensation:
RESERVE_INVENTORY = READY
```

Both statements can be true.

The inventory was successfully reserved. Now we need to release it.

Keeping the two lifecycles separate makes the database much easier to understand.

### Why do we store request and response payloads?

Suppose we need to execute:

```
CHARGE_PAYMENT
```

The worker needs to know what to send.

It should be able to reconstruct the operation without depending on some temporary Java object.

For example:

```
{
  "orderId": "ORDER-123",
  "customerId": "CUSTOMER-456",
  "amount": 2500,
  "currency": "INR"
}
```

The response might be:

```
{
  "paymentId": "PAY-456",
  "status": "SUCCESS"
}
```

We persist the response because the compensation operation may need the `paymentId`.

For example:

```
POST /payments/PAY-456/refund
```

One implementation detail: you don't necessarily need to duplicate every payload in both `saga_steps` and `sagas.context`. Define one authoritative place for each piece of data. The separate fields above are illustrative.

# 4. Understanding the most important thing: State machines

So far, we have created tables.

But how do we decide which status to assign?

We need a state machine.

A state machine defines:

- What states are allowed.
- Which transitions are allowed.
- What event causes a transition.
- What actions are permitted in each state.

Without this, developers might update statuses however they like, creating inconsistent workflows.

Let's derive it.

## 4.1 Forward execution states

I recommend starting with these states:

| State      | Meaning                                        |
| ---------- | ---------------------------------------------- |
| BLOCKED    | Waiting for prerequisite steps                 |
| READY      | Eligible for execution                         |
| RUNNING    | A worker has claimed it                        |
| RETRY_WAIT | Failed or uncertain attempt; waiting for retry |
| SUCCEEDED  | Business operation completed successfully      |
| FAILED     | Forward operation has permanently failed       |
| SKIPPED    | Intentionally not required                     |

Let's understand each one.

BLOCKED

This step cannot execute yet.

Initially:

```
CREATE_ORDER        READY
RESERVE_INVENTORY   BLOCKED
CHARGE_PAYMENT      BLOCKED
CREATE_SHIPMENT     BLOCKED
```

Why?

Because inventory reservation requires an order to exist.

Payment requires inventory reservation to succeed.

Shipment requires payment to succeed.

READY

The prerequisites are satisfied. The operation can be executed.

It does not mean the operation is running.

RUNNING

A worker has claimed the operation and is executing it.

RETRY_WAIT

The operation needs another attempt, but not immediately.

For example, an HTTP 503 response might lead to:

```
status = RETRY_WAIT
next_attempt_at = 10 minutes later
```

SUCCEEDED

The participant has confirmed successful completion.

FAILED

The operation has reached a permanent failure or exhausted its configured retry policy.

This does not necessarily mean the whole saga is terminal. The orchestrator may need to compensate earlier successful steps.

### State transitions

\#chatgpt-mermaid-\_r_3d4\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3d4\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3d4\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3d4\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3d4\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3d4\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3d4\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3d4\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3d4\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3d4\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3d4\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3d4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3d4\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3d4\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3d4\_ p{margin:0;}#chatgpt-mermaid-\_r_3d4\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3d4\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3d4\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3d4\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3d4\_ .label text,#chatgpt-mermaid-\_r_3d4\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3d4\_ .node rect,#chatgpt-mermaid-\_r_3d4\_ .node circle,#chatgpt-mermaid-\_r_3d4\_ .node ellipse,#chatgpt-mermaid-\_r_3d4\_ .node polygon,#chatgpt-mermaid-\_r_3d4\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3d4\_ .rough-node .label text,#chatgpt-mermaid-\_r_3d4\_ .node .label text,#chatgpt-mermaid-\_r_3d4\_ .image-shape .label,#chatgpt-mermaid-\_r_3d4\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3d4\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3d4\_ .rough-node .label,#chatgpt-mermaid-\_r_3d4\_ .node .label,#chatgpt-mermaid-\_r_3d4\_ .image-shape .label,#chatgpt-mermaid-\_r_3d4\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3d4\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3d4\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3d4\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3d4\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3d4\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3d4\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3d4\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3d4\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3d4\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3d4\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3d4\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3d4\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3d4\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3d4\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3d4\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3d4\_ .icon-shape,#chatgpt-mermaid-\_r_3d4\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3d4\_ .icon-shape p,#chatgpt-mermaid-\_r_3d4\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3d4\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3d4\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3d4\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3d4\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3d4\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3d4\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3d4\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3d4\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3d4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3d4\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3d4\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3d4\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3d4\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3d4\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3d4\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3d4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3d4\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3d4\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3d4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3d4\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3d4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3d4\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3d4\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3d4\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3d4\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3d4\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3d4\_ .node rect,#chatgpt-mermaid-\_r_3d4\_ .node circle,#chatgpt-mermaid-\_r_3d4\_ .node ellipse,#chatgpt-mermaid-\_r_3d4\_ .node polygon,#chatgpt-mermaid-\_r_3d4\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3d4\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3d4\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3d4\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3d4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3d4\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}BLOCKEDREADYRUNNINGSUCCEEDEDRETRY_WAITFAILEDSKIPPEDDependencies satisfiedWorker claims stepConfirmed successRetryable / uncertain outcomeRetry becomes duePermanent failure / retriesexhaustedBusiness condition saysunnecessaryWorkflow branch excluded

An important detail: `RUNNING` is not proof that the worker is still alive.

It simply means a worker claimed the work. We'll solve worker crashes and abandoned claims in the next chapter using leases.

Also, a timeout that produces an unknown outcome must not be interpreted as confirmed failure. The orchestrator must resolve or safely retry it using the same logical idempotency key.

## 4.2 Saga-level states

Now consider the overall saga.

It needs a separate state machine.

I recommend these core states:

| State                        | Meaning                                                      |
| ---------------------------- | ------------------------------------------------------------ |
| RUNNING                      | Forward execution is in progress                             |
| COMPENSATING                 | Previously successful operations are being compensated       |
| SUCCEEDED                    | All required forward operations succeeded                    |
| COMPENSATED                  | Required compensations completed                             |
| FAILED                       | Workflow permanently failed without outstanding compensation |
| MANUAL_INTERVENTION_REQUIRED | Automated recovery cannot safely continue                    |

The lifecycle:

\#chatgpt-mermaid-\_r_3dd\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3dd\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3dd\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3dd\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3dd\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3dd\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3dd\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3dd\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3dd\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3dd\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3dd\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3dd\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3dd\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3dd\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3dd\_ p{margin:0;}#chatgpt-mermaid-\_r_3dd\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3dd\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3dd\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3dd\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3dd\_ .label text,#chatgpt-mermaid-\_r_3dd\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3dd\_ .node rect,#chatgpt-mermaid-\_r_3dd\_ .node circle,#chatgpt-mermaid-\_r_3dd\_ .node ellipse,#chatgpt-mermaid-\_r_3dd\_ .node polygon,#chatgpt-mermaid-\_r_3dd\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3dd\_ .rough-node .label text,#chatgpt-mermaid-\_r_3dd\_ .node .label text,#chatgpt-mermaid-\_r_3dd\_ .image-shape .label,#chatgpt-mermaid-\_r_3dd\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3dd\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3dd\_ .rough-node .label,#chatgpt-mermaid-\_r_3dd\_ .node .label,#chatgpt-mermaid-\_r_3dd\_ .image-shape .label,#chatgpt-mermaid-\_r_3dd\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3dd\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3dd\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3dd\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3dd\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3dd\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3dd\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3dd\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3dd\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3dd\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3dd\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3dd\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3dd\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3dd\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3dd\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3dd\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3dd\_ .icon-shape,#chatgpt-mermaid-\_r_3dd\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3dd\_ .icon-shape p,#chatgpt-mermaid-\_r_3dd\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3dd\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3dd\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3dd\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3dd\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3dd\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3dd\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3dd\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3dd\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3dd\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3dd\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3dd\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3dd\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3dd\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3dd\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3dd\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3dd\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3dd\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3dd\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3dd\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3dd\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3dd\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3dd\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3dd\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3dd\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3dd\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3dd\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3dd\_ .node rect,#chatgpt-mermaid-\_r_3dd\_ .node circle,#chatgpt-mermaid-\_r_3dd\_ .node ellipse,#chatgpt-mermaid-\_r_3dd\_ .node polygon,#chatgpt-mermaid-\_r_3dd\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3dd\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3dd\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3dd\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3dd\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3dd\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}RUNNINGSUCCEEDEDCOMPENSATINGCOMPENSATEDMANUAL_INTERVENTION_REQUIREDFAILEDEvery required step succeedsPermanent forward failureAll required compensationssucceedRecovery needs human actionFailure before any businesseffects, no compensationneededOperator resolves issue andresumes

Why distinguish `FAILED` and `COMPENSATED`?

Consider these two situations:

Situation A

Order creation failed before anything was created.

```
CREATE_ORDER = FAILED

Saga = FAILED
```

There is nothing to compensate.

Situation B

Order creation and inventory reservation succeeded, but payment failed.

```
CREATE_ORDER = SUCCEEDED
RESERVE_INVENTORY = SUCCEEDED
CHARGE_PAYMENT = FAILED
```

We must compensate.

```
Saga = COMPENSATING
```

Once order cancellation and inventory release succeed:

```
Saga = COMPENSATED
```

The business transaction failed, but its compensations completed.

These are different outcomes and should not be represented by one generic FAILED status.

# 5. Compensation states — Why do we need separate ones?

Let's examine the same example.

Forward execution:

```
CREATE_ORDER        SUCCEEDED
RESERVE_INVENTORY   SUCCEEDED
CHARGE_PAYMENT      FAILED
CREATE_SHIPMENT     BLOCKED
```

The saga enters:

```
COMPENSATING
```

Now we should execute compensations in reverse order.

```
RELEASE_INVENTORY
CANCEL_ORDER
```

The compensation statuses become:

| Step              | Forward status | Compensation status |
| ----------------- | -------------- | ------------------- |
| CREATE_ORDER      | SUCCEEDED      | READY               |
| RESERVE_INVENTORY | SUCCEEDED      | READY               |
| CHARGE_PAYMENT    | FAILED         | NOT_REQUIRED        |
| CREATE_SHIPMENT   | BLOCKED        | NOT_REQUIRED        |

But we should not execute both compensation operations concurrently when the business workflow requires reverse order.

We start:

```
RELEASE_INVENTORY = READY
CANCEL_ORDER = BLOCKED
```

After inventory is released:

```
RELEASE_INVENTORY = SUCCEEDED
CANCEL_ORDER = READY
```

Now order cancellation can execute.

Recommended compensation states:

| State               | Meaning                                       |
| ------------------- | --------------------------------------------- |
| NOT_REQUIRED        | No compensation is applicable                 |
| NOT_STARTED         | Compensation has not yet been scheduled       |
| READY               | Compensation is ready                         |
| RUNNING             | Compensation is being executed                |
| RETRY_WAIT          | Compensation needs another attempt            |
| SUCCEEDED           | Compensation completed                        |
| MANUAL_INTERVENTION | Automated compensation cannot safely continue |

The compensation state machine is similar to forward execution, but its purpose is different.

Very important: A forward step with an unknown outcome must not automatically be treated as failed and excluded from compensation.

For example, the Payment Service might have charged the customer but the response was lost.

The orchestrator must first establish whether the payment actually happened, usually by querying the participant or retrying with the same idempotency key.

Otherwise, it could incorrectly declare the saga compensated while the customer remains charged.

# 6. Table 3: `saga_step_attempts` — Why do we need execution history?

Now we reach another important modeling decision.

Suppose we have:

```
CHARGE_PAYMENT

Attempt 1: HTTP timeout
Attempt 2: HTTP 503
Attempt 3: Success
```

Should we create three different saga step records?

No.

All three attempts belong to the same logical step.

The step represents the business operation:

```
CHARGE_PAYMENT
```

The attempts represent individual execution tries.

These are different concepts.

So let's create another table.

```
CREATE TABLE saga_step_attempts (
    id UUID PRIMARY KEY,

    step_id UUID NOT NULL REFERENCES saga_steps(id),

    action_type VARCHAR(30) NOT NULL,

    attempt_number INTEGER NOT NULL,

    worker_id VARCHAR(255),

    idempotency_key VARCHAR(255),

    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,

    outcome VARCHAR(50),

    http_status INTEGER,
    error_code VARCHAR(100),
    error_message TEXT,

    request_payload JSONB,
    response_payload JSONB,

    UNIQUE (step_id, action_type, attempt_number)
);
```

Example:

| Step           | Action  | Attempt | Outcome  |
| -------------- | ------- | ------- | -------- |
| CHARGE_PAYMENT | FORWARD | 1       | TIMEOUT  |
| CHARGE_PAYMENT | FORWARD | 2       | HTTP_503 |
| CHARGE_PAYMENT | FORWARD | 3       | SUCCESS  |

Why is this useful?

### A. Debugging

You can answer:

- How many times did this step execute?
- Which worker executed it?
- What errors did it receive?
- When did the participant respond?
- How long did each attempt take?

### B. Retry management

You can distinguish the logical step from its individual execution attempts.

### C. Audit history

For financial workflows, it is particularly important to know what the orchestrator attempted and what responses it received.

One important detail:

The attempts table is an execution history. The current step status remains the authoritative state used to decide what happens next.

# 7. Table 4: Idempotency — Two different levels

We discussed idempotency in the previous chapter, but now we should include it in our database design.

There are actually two separate problems.

### Problem A: The client submits the same saga request twice

Example:

```
Request 1:
Idempotency-Key: checkout-123

Request 2:
Idempotency-Key: checkout-123
```

We must not create two independent sagas.

We can use:

```
CREATE TABLE saga_idempotency (
    tenant_id VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,

    request_hash VARCHAR(255) NOT NULL,

    saga_id UUID NOT NULL REFERENCES sagas(id),

    created_at TIMESTAMP NOT NULL,

    PRIMARY KEY (tenant_id, idempotency_key)
);
```

When a request arrives:

1. Calculate a hash of its normalized business request.
2. Attempt to insert the idempotency record.
3. Create the saga and steps in the same transaction.
4. Commit everything together.

If the same key arrives again:

- Same request hash: Return the existing saga ID and current status.
- Different request hash: Return HTTP 409 Conflict.

Why compare hashes?

Because this should not happen:

```
First request:
checkout-123
amount = 2500

Second request:
checkout-123
amount = 5000
```

The client has reused the key for a different operation.

We should reject it rather than silently returning the result of a different request.

### Problem B: The orchestrator calls a participant more than once

For example:

```
Orchestrator -> Payment Service

Idempotency-Key:
SAGA-1001-CHARGE_PAYMENT
```

The participant must maintain its own idempotency records in its own database.

This is separate from `saga_idempotency`.

We'll cover the complete mechanism in Chapter 4.

For now, the key distinction is:

| Idempotency                 | Prevents                   |
| --------------------------- | -------------------------- |
| Client-to-orchestrator      | Duplicate saga creation    |
| Orchestrator-to-participant | Duplicate business effects |

Never assume that one idempotency table can solve both problems.

# 8. Table 5: `outbox_events` — Reliable notifications

This table is optional if we initially use database polling.

But it becomes important when we introduce Kafka or RabbitMQ.

```
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,

    aggregate_id UUID NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    payload JSONB NOT NULL,

    created_at TIMESTAMP NOT NULL,

    published_at TIMESTAMP,

    publish_attempts INTEGER NOT NULL DEFAULT 0
);
```

Suppose an inventory step succeeds.

We want to notify the scheduler that the next step is ready.

We perform:

```
BEGIN;

UPDATE saga_steps
SET forward_status = 'SUCCEEDED'
WHERE id = '...';

UPDATE saga_steps
SET forward_status = 'READY'
WHERE step_key = 'CHARGE_PAYMENT'
  AND saga_id = '...';

INSERT INTO outbox_events (...);

COMMIT;
```

Now the state transition and event creation are atomic.

A publisher can read the outbox and publish events to the broker.

Remember:

The outbox is not the source of truth for saga progress.

The saga and steps tables are authoritative. The outbox makes notifications reliable.

# 9. Now let's understand the actual database transaction boundaries

This is where all the tables come together.

There are four important stages.

## Transaction 1: Start the saga

Client submits a request.

The orchestrator executes:

```
BEGIN;

-- Register client idempotency key.

-- Create saga.

-- Create all saga steps.

-- Mark the first eligible step READY.

-- Optionally create an outbox event.

COMMIT;
```

Then return:

```
202 Accepted
```

Everything is persisted together.

There must not be a situation where the saga exists but its required steps are missing.

## Transaction 2: Worker claims a step

Suppose a worker finds:

```
RESERVE_INVENTORY = READY
```

It must claim that step before executing it.

Conceptually:

```
BEGIN;

UPDATE saga_steps
SET forward_status = 'RUNNING'
WHERE id = :step_id
  AND forward_status = 'READY';

-- Record ownership and attempt information.

COMMIT;
```

This is a simplified illustration. In practice, we also need atomic claiming, lease expiry and protection against stale workers.

We'll implement those in Chapter 3.

## Outside the database transaction: Call participant

Now the worker calls:

```
POST /inventory/reservations
Idempotency-Key: SAGA-1001-RESERVE_INVENTORY
```

Notice that we are not holding a database transaction open during this HTTP call.

Why?

Because the HTTP request could take 30 seconds, or the remote service could never respond.

Holding database transactions and row locks during external network calls is generally a bad design.

It wastes connections, prolongs locks, increases contention and creates operational problems.

The worker must commit its claim, make the external call, and then persist the outcome in a separate transaction.

## Transaction 3: Persist the outcome

Suppose the inventory operation succeeds.

```
BEGIN;

-- Mark current step SUCCEEDED.

-- Save participant response.

-- Insert execution attempt result.

-- Mark the next eligible step READY.

-- Update saga metadata.

-- Optionally insert outbox notification.

COMMIT;
```

All these orchestrator-side changes are committed together.

If this transaction fails, the worker must not assume its state update succeeded.

It must recover by consulting the database and safely retrying or reconciling the operation.

This separation is fundamental:

```
Transaction 1: Accept saga
Transaction 2: Claim step
             |
             v
         HTTP call
             |
             v
Transaction 3: Record result and schedule next action
```

There is no single ACID transaction covering all three stages or spanning the participant's database.

# 10. Important: Should we store `current_step` in the saga table?

You might have noticed that I did not include a `current_step` column in the main saga schema.

Why?

For a simple sequential workflow, we could store:

```
current_step = 3
```

But imagine a more complex workflow:

```
                 CREATE_ORDER
                       |
                RESERVE_INVENTORY
                       |
                +------+------+
                |             |
           CHARGE_PAYMENT  VALIDATE_ADDRESS
                |             |
                +------+------+
                       |
                 CREATE_SHIPMENT
```

Payment and address validation can execute in parallel.

Which one is the current step?

Both are!

A single integer cannot accurately represent that.

Therefore:

The state of individual steps and their dependencies should be the source of truth, not a single current-step pointer.

For a basic sequential system, you can maintain `current_step` as a convenient denormalized field, but it should not be authoritative.

For workflows with branching and parallel execution, model dependencies explicitly, for example using a `saga_step_dependencies` table or an immutable workflow definition.

# 11. What happens when the workflow definition changes?

Let's consider this production scenario.

Day 1:

```
Deploy ORDER_CHECKOUT v1

1. CREATE_ORDER
2. RESERVE_INVENTORY
3. CHARGE_PAYMENT
```

A saga starts:

```
SAGA-1001
definition_version = 1
```

Day 2:

You deploy a new version:

```
ORDER_CHECKOUT v2

1. CREATE_ORDER
2. VALIDATE_ADDRESS
3. RESERVE_INVENTORY
4. CHARGE_PAYMENT
```

Meanwhile, SAGA-1001 is still running.

What should the worker execute?

It must continue using version 1.

A good implementation follows this principle:

- Workflow definitions are versioned and immutable.
- Each saga stores its definition version.
- Workers load the correct version when executing that saga.
- Old workflow definitions remain available until no active sagas depend on them.

For especially long-running workflows, you may also persist the resolved step definitions and immutable request payloads at saga creation time.

This protects against accidentally changing the behavior of already accepted business transactions.

# 12. Indexes — Don't forget how workers find work

Once you have thousands or millions of steps, reading the database efficiently matters.

For example, PostgreSQL:

```
CREATE INDEX idx_ready_steps
ON saga_steps (created_at)
WHERE forward_status = 'READY';
```

For retry scheduling:

```
CREATE INDEX idx_retry_steps
ON saga_steps (next_attempt_at)
WHERE forward_status = 'RETRY_WAIT';
```

For looking up steps in a saga:

```
CREATE INDEX idx_saga_steps
ON saga_steps (saga_id, step_order);
```

For attempts:

```
CREATE INDEX idx_step_attempts
ON saga_step_attempts (step_id, attempt_number);
```

The exact indexes will depend on your query patterns.

We'll refine these when we discuss concurrent worker claiming.

# 13. Putting the database together

Here's our logical data model:

\#chatgpt-mermaid-\_r_3i7\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3i7\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3i7\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3i7\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3i7\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3i7\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3i7\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3i7\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3i7\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3i7\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3i7\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3i7\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3i7\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3i7\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3i7\_ p{margin:0;}#chatgpt-mermaid-\_r_3i7\_ .entityBox{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3i7\_ .relationshipLabelBox{fill:rgb(48, 48, 48);opacity:0.7;background-color:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3i7\_ .relationshipLabelBox rect{opacity:0.5;}#chatgpt-mermaid-\_r_3i7\_ .labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-\_r_3i7\_ .edgeLabel{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3i7\_ .edgeLabel .label rect{fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3i7\_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3i7\_ .edgeLabel .label{fill:rgb(31, 78, 148);font-size:14px;}#chatgpt-mermaid-\_r_3i7\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3i7\_ .edge-pattern-dashed{stroke-dasharray:8,8;}#chatgpt-mermaid-\_r_3i7\_ .node rect,#chatgpt-mermaid-\_r_3i7\_ .node circle,#chatgpt-mermaid-\_r_3i7\_ .node ellipse,#chatgpt-mermaid-\_r_3i7\_ .node polygon{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3i7\_ .relationshipLine{stroke:rgb(175, 175, 175);stroke-width:1px;fill:none;}#chatgpt-mermaid-\_r_3i7\_ .marker{fill:none!important;stroke:rgb(175, 175, 175)!important;stroke-width:1;}#chatgpt-mermaid-\_r_3i7\_ [data-look=neo].labelBkg{background-color:rgba(48, 48, 48, 0.5);}#chatgpt-mermaid-\_r_3i7\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3i7\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3i7\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3i7\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3i7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3i7\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3i7\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3i7\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3i7\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3i7\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3i7\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3i7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3i7\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3i7\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3i7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3i7\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3i7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3i7\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}SAGASuuididPKstringsaga_typeintdefinition_versionstringbusiness_referencestringstatusjsoninput_payloadjsoncontexttimestampcreated_atSAGA_STEPSuuididPKuuidsaga_idFKstringstep_keyintstep_orderstringforward_statusstringcompensation_statusstringforward_idempotency_keyjsonforward_requestjsonforward_responsetimestampnext_attempt_atSAGA_STEP_ATTEMPTSuuididPKuuidstep_idFKstringaction_typeintattempt_numberstringworker_idstringoutcometimestampstarted_attimestampcompleted_atOUTBOX_EVENTSuuididPKuuidaggregate_idstringevent_typejsonpayloadtimestampcreated_attimestamppublished_atSAGA_IDEMPOTENCYstringtenant_idPKstringidempotency_keyPKstringrequest_hashuuidsaga_idFKcontainsrecordsgeneratesidentified_by

The whole model can be remembered through five simple questions:

| Table                | Question it answers                                      |
| -------------------- | -------------------------------------------------------- |
| `sagas`              | Which distributed business workflow is running?          |
| `saga_steps`         | What operations need to happen, and what is their state? |
| `saga_step_attempts` | What actually happened during each execution attempt?    |
| `saga_idempotency`   | Have we already accepted this client's request?          |
| `outbox_events`      | Which state changes need to be communicated reliably?    |

# 14. A complete example: Watch the database evolve

Let's execute a real saga and observe its state.

Initial state:

```
SAGA-1001 = RUNNING

CREATE_ORDER        READY
RESERVE_INVENTORY   BLOCKED
CHARGE_PAYMENT      BLOCKED
CREATE_SHIPMENT     BLOCKED
```

Event 1: Worker claims CREATE_ORDER

```
CREATE_ORDER = RUNNING
```

Attempt 1 is recorded.

Event 2: Order Service succeeds

```
CREATE_ORDER        SUCCEEDED
RESERVE_INVENTORY   READY
CHARGE_PAYMENT      BLOCKED
CREATE_SHIPMENT     BLOCKED
```

The next step becomes eligible in the same database transaction.

Event 3: Worker claims RESERVE_INVENTORY

```
RESERVE_INVENTORY = RUNNING
```

Attempt 1 is recorded.

Event 4: Inventory Service returns HTTP 503

```
RESERVE_INVENTORY = RETRY_WAIT
next_attempt_at = ...
```

The overall saga remains RUNNING.

Event 5: Retry becomes due

```
RESERVE_INVENTORY = READY
```

A worker claims it again.

Event 6: Inventory reservation succeeds

```
CREATE_ORDER        SUCCEEDED
RESERVE_INVENTORY   SUCCEEDED
CHARGE_PAYMENT      READY
CREATE_SHIPMENT     BLOCKED
```

Event 7: Payment Service permanently declines the payment

```
CHARGE_PAYMENT = FAILED
```

The saga enters:

```
SAGA-1001 = COMPENSATING
```

Event 8: Schedule compensation

```
RELEASE_INVENTORY = READY
CANCEL_ORDER      = BLOCKED
```

Event 9: Inventory release succeeds

```
RELEASE_INVENTORY = SUCCEEDED
CANCEL_ORDER      = READY
```

Event 10: Order cancellation succeeds

```
CANCEL_ORDER = SUCCEEDED
```

Final state:

```
SAGA-1001 = COMPENSATED
```

The database now has a complete record of what happened, including the failed payment attempt, successful forward steps and successful compensations.

A new worker can inspect that information at any point without needing the original worker's memory.

# 15. What we deliberately haven't solved yet

Our database model is ready conceptually, but a very important problem remains.

Imagine two workers:

```
Worker A
Worker B
```

Both query:

```
SELECT *
FROM saga_steps
WHERE forward_status = 'READY';
```

Both see:

```
RESERVE_INVENTORY = READY
```

What prevents them from both executing the same step?

Also:

- What if Worker A claims the step and crashes?
- What if its lease expires but the old worker is still alive?
- What if Worker B takes over?
- What if Worker A comes back and tries to update the database?
- What if the scheduler picks up the same step repeatedly?

These are concurrency and ownership problems.

A simple SELECT followed by UPDATE is not enough.

We need atomic claiming, leases, ownership tokens, and fencing against stale workers.

## Chapter 2 — What you should now understand

The central mental model is:

A saga is a durable workflow instance. A step is a logical business operation. An attempt is one physical execution of that operation.

Never mix these three concepts.

The orchestrator database remembers the workflow, its operations, and their execution history. The state machine controls which transitions are legal.

And remember this particularly important rule:

Never keep a database transaction open while calling another microservice. Commit your state changes, make the remote call, and persist the outcome in another local transaction.

## Next chapter: Chapter 3 — Reliable Worker Execution Engine

This is where we make the design genuinely reliable.

We'll build the actual worker execution mechanism:

1. How workers discover READY steps.
2. How multiple workers safely claim work using PostgreSQL.
3. `SELECT FOR UPDATE SKIP LOCKED` — why it exists and how it works.
4. What leases are and why a PROCESSING status alone is insufficient.
5. How lease expiration and crash recovery work.
6. How to prevent stale workers from overwriting newer state using fencing tokens.
7. How to execute HTTP calls without holding DB locks.
8. How to recover when a worker crashes before or after a participant succeeds.

We'll walk through two workers competing for the same step, at the SQL level, so the concurrency behavior becomes intuitive rather than something you have to memorize.

