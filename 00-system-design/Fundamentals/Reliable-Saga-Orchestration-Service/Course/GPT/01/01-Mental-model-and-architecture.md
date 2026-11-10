# Building a Reliable Saga Orchestrator — From First Principles

Your understanding is already quite close to how a production Saga Orchestrator works.

The important thing is that a production-grade orchestrator is not simply a service that:

1. Reads pending steps from a database.
2. Calls another microservice.
3. Updates the step status.
4. Repeats.

That is the basic workflow. The difficult part is making sure this workflow remains correct when anything can fail at any point, including the orchestrator itself, its database, the network, or a participant service.

For example:

- What happens if Service A successfully deducts money, but the orchestrator crashes before receiving the response?
- What happens if two orchestrator instances pick up the same pending saga?
- What happens if the orchestrator marks a step as PROCESSING and then crashes?
- What happens if a compensation fails?
- What happens if the client sends the same request five times?
- What happens if the database commits but the HTTP response is lost?
- What happens if a participant takes 60 seconds to respond?
- What happens if 10,000 sagas are running simultaneously?

These are the problems our design must solve.

Let's construct the system from scratch.

## The learning journey

We'll develop the design in these chapters.

| Chapter                                | What we will understand                                                                    |
| -------------------------------------- | ------------------------------------------------------------------------------------------ |
| 1. Mental model and architecture       | What the orchestrator actually owns, how a saga starts, and how the entire lifecycle works |
| 2. Database design and state machine   | Tables, states, transitions, and how to represent saga execution                           |
| 3. Reliable execution engine           | How workers pick tasks, claim ownership, execute steps, and recover from crashes           |
| 4. Idempotency and duplicate handling  | How to prevent duplicate business operations across services                               |
| 5. Retries, timeouts and failures      | Retry policies, unknown outcomes, circuit breakers, and permanent failures                 |
| 6. Compensation                        | How rollback actually works, reverse execution, failed compensation, and recovery          |
| 7. Concurrency and scaling             | Multiple orchestrators, parallel execution, scheduling, partitioning, and backpressure     |
| 8. Database and infrastructure choices | PostgreSQL vs NoSQL, message brokers, transactional outbox, and production architecture    |
| 9. Complete implementation             | Putting everything together with Java/Spring Boot-style pseudocode and SQL                 |

Let's begin.

# Chapter 1 — Understanding the actual mental model

## 1. First, why do we need a Saga Orchestrator?

Imagine an e-commerce application.

A customer places an order.

To complete this operation, we need:

1. Order Service: Create an order.
2. Inventory Service: Reserve inventory.
3. Payment Service: Charge the customer.
4. Shipping Service: Create a shipment.

Each service owns its own database.

Our business requirement is:

> Complete the order only when all required operations succeed. Otherwise, perform appropriate actions to compensate for previously completed operations.

Ideally, we would like to write something like:

```
@Transactional
public void placeOrder() {
    orderService.createOrder();
    inventoryService.reserveInventory();
    paymentService.chargeCustomer();
    shippingService.createShipment();
}
```

But this does not work as expected.

Why?

`@Transactional` controls a transaction within its configured transactional resource. It cannot automatically make changes in four independent service databases one atomic transaction.

Consider this execution:

```
Order Service
    Order created successfully

Inventory Service
    Inventory reserved successfully

Payment Service
    Payment charged successfully

Shipping Service
    FAILED
```

Now we have:

- An order created.
- Inventory reserved.
- Customer charged.
- No shipment.

Our application is in an inconsistent business state.

A traditional database transaction could roll back all participating database changes if they belonged to one transaction. But these services own different databases and commit independently.

This is where the Saga pattern comes in.

A Saga is a sequence of independently committed local transactions, coordinated so that the overall business process eventually reaches a successful or appropriately compensated state.

The two important operations are:

Forward operation: Perform the next business action.

Compensating operation: Perform a new business action that counteracts a previously completed action.

For example:

```
Forward execution:

1. Create order
2. Reserve inventory
3. Charge customer
4. Create shipment

Suppose step 4 fails.

Compensation:

3. Refund customer
2. Release inventory
1. Cancel order
```

Notice something important:

We are not restoring the databases to their previous physical states through one ACID rollback.

We are executing new business operations that counteract previous operations.

This distinction is fundamental to everything that follows.

AWS also explicitly identifies eventual consistency, idempotency, and the lack of transaction isolation as important Saga design considerations.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

AWS Prescriptive Guidance+2



## 2. What exactly is the Orchestrator responsible for?

Let's introduce a new service:

Saga Orchestrator

Its responsibility is to manage the lifecycle of a distributed business transaction.

It does not own the order, inventory, payment, or shipping business data.

Those services remain responsible for their own business operations.

Instead, the orchestrator owns:

- Saga execution state.
- The sequence of steps.
- Which step should execute next.
- Whether a step succeeded or failed.
- Retry scheduling.
- Compensation scheduling.
- Saga completion status.
- Recovery after crashes.

Think of it as a workflow engine with durable memory.

The orchestrator remembers:

> What business workflow am I executing? What has already happened? What should happen next? What should I do if the next operation fails?

There is one particularly important design principle:

The database is the source of truth for workflow progress. Workers are replaceable executors.

A worker can crash. An application instance can restart. A new worker can continue the workflow using the previously persisted state.

That is the foundation of reliability.

## 3. The architecture we are going to build

Let's separate the orchestrator into its logical responsibilities.

\#chatgpt-mermaid-\_r_2v3\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2v3\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2v3\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2v3\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2v3\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2v3\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2v3\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2v3\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2v3\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2v3\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2v3\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2v3\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2v3\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2v3\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2v3\_ p{margin:0;}#chatgpt-mermaid-\_r_2v3\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2v3\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2v3\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2v3\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2v3\_ .label text,#chatgpt-mermaid-\_r_2v3\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2v3\_ .node rect,#chatgpt-mermaid-\_r_2v3\_ .node circle,#chatgpt-mermaid-\_r_2v3\_ .node ellipse,#chatgpt-mermaid-\_r_2v3\_ .node polygon,#chatgpt-mermaid-\_r_2v3\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2v3\_ .rough-node .label text,#chatgpt-mermaid-\_r_2v3\_ .node .label text,#chatgpt-mermaid-\_r_2v3\_ .image-shape .label,#chatgpt-mermaid-\_r_2v3\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2v3\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2v3\_ .rough-node .label,#chatgpt-mermaid-\_r_2v3\_ .node .label,#chatgpt-mermaid-\_r_2v3\_ .image-shape .label,#chatgpt-mermaid-\_r_2v3\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2v3\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2v3\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2v3\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2v3\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2v3\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2v3\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2v3\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2v3\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2v3\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2v3\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2v3\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2v3\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2v3\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2v3\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2v3\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2v3\_ .icon-shape,#chatgpt-mermaid-\_r_2v3\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2v3\_ .icon-shape p,#chatgpt-mermaid-\_r_2v3\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2v3\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2v3\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2v3\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2v3\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2v3\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2v3\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2v3\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2v3\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2v3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2v3\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2v3\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2v3\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2v3\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2v3\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2v3\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2v3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2v3\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2v3\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2v3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2v3\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2v3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2v3\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2v3\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2v3\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2v3\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2v3\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2v3\_ .node rect,#chatgpt-mermaid-\_r_2v3\_ .node circle,#chatgpt-mermaid-\_r_2v3\_ .node ellipse,#chatgpt-mermaid-\_r_2v3\_ .node polygon,#chatgpt-mermaid-\_r_2v3\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2v3\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2v3\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2v3\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2v3\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2v3\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Saga OrchestratorScheduler / DispatcherWorker 1Worker 2Workflow State MachineCalling Service / ClientSaga APIOrchestrator DatabaseOrder ServiceInventory ServicePayment ServiceShipping ServiceStart SagaPersist saga + steps202 AcceptedResultResultResultResult

This is a logical architecture, not necessarily six separate deployable services.

For example, the API, scheduler, and workers might all be part of the same Spring Boot application.

We separate them conceptually because they have different responsibilities.

| Component           | Responsibility                                     |
| ------------------- | -------------------------------------------------- |
| Saga API            | Accept new saga requests                           |
| Database            | Durably store saga state and execution information |
| Scheduler           | Find work that needs execution                     |
| Worker              | Execute one saga operation                         |
| State machine       | Determine the next valid state and action          |
| Participant service | Execute its own local business transaction         |

One orchestrator may have multiple API instances and many worker instances.

They all share the same durable workflow database.

## 4. Starting a Saga — What exactly happens?

Let's walk through an actual request.

Suppose an Order Service calls the Saga Orchestrator.

Request:

```
POST /sagas
Idempotency-Key: order-123-checkout
Content-Type: application/json
```

Payload:

```
{
  "sagaType": "ORDER_CHECKOUT",
  "businessReference": "ORDER-123",
  "input": {
    "orderId": "ORDER-123",
    "customerId": "CUSTOMER-456",
    "amount": 2500
  }
}
```

The orchestrator receives this request.

### Step 1: Validate the request

It checks:

- Is the saga type supported?
- Is the request valid?
- Are all required fields present?
- Is the idempotency key present?
- Is the requested workflow version supported?

It also identifies the saga definition.

For example:

```
ORDER_CHECKOUT

Step 1: Create Order
Step 2: Reserve Inventory
Step 3: Charge Payment
Step 4: Create Shipment
```

This is the workflow definition.

An important distinction:

The workflow definition describes what should happen.

The saga instance describes one particular execution of that workflow.

For example, 10,000 orders may execute the same workflow definition, but each order has its own saga instance and independent execution progress.

### Step 2: Persist the saga before acknowledging it

The orchestrator creates its records.

Conceptually:

```
SAGA TABLE

saga_id: SAGA-1001
business_reference: ORDER-123
saga_type: ORDER_CHECKOUT
status: RUNNING
current_step: 1
created_at: ...
```

And:

```
SAGA_STEPS TABLE

saga_id       step         status
-------------------------------------
SAGA-1001     CREATE_ORDER PENDING
SAGA-1001     RESERVE_STOCK PENDING
SAGA-1001     CHARGE_PAYMENT PENDING
SAGA-1001     CREATE_SHIPMENT PENDING
```

But there is one important improvement to your proposed design.

Creating the saga and its steps must happen in one database transaction.

For example:

```
BEGIN;

INSERT INTO sagas (...);

INSERT INTO saga_steps (...);
INSERT INTO saga_steps (...);
INSERT INTO saga_steps (...);
INSERT INTO saga_steps (...);

COMMIT;
```

Why?

Imagine the main saga record is committed, but the application crashes before inserting all the steps.

Now you have an incomplete workflow.

Or imagine the steps are inserted but the main saga record isn't.

The database would contain inconsistent workflow information.

A single local database transaction solves this problem.

We also need to handle duplicate requests, which we'll cover deeply in Chapter 4.

For now, understand that the request's idempotency key should have a database uniqueness constraint and be checked in the same transaction that creates the saga.

### Step 3: Return a response

You asked whether this should be `200 OK` or something else.

Use:

```
HTTP/1.1 202 Accepted
Content-Type: application/json
```

```
{
  "sagaId": "SAGA-1001",
  "status": "RUNNING",
  "message": "Saga accepted for execution",
  "statusUrl": "/sagas/SAGA-1001"
}
```

Why 202?

Because the orchestrator has accepted and durably recorded the request, but the distributed business process has not finished.

The distinction is:

```
Request accepted != Business operation completed
```

The API should not wait for the entire distributed workflow unless you deliberately design a synchronous execution mode.

The calling service can query:

```
GET /sagas/SAGA-1001
```

Or subscribe to completion events.

Critical rule: Never return an accepted response before the saga has been durably persisted.

Otherwise, the caller might believe execution has started even though the orchestrator has lost the request.

## 5. How does the orchestrator actually start executing?

This is where your original understanding needs an important addition.

You said:

> Orchestrator reads pending sagas from the main table and looks for pending steps.

That is a reasonable starting point.

However, we should not think of the orchestrator as one process scanning a table and executing everything itself.

Instead, think of it as a durable task scheduler with multiple workers.

A saga becomes eligible for execution when it has an action ready.

For example:

```
SAGA-1001

CREATE_ORDER       PENDING
RESERVE_STOCK      PENDING
CHARGE_PAYMENT     PENDING
CREATE_SHIPMENT    PENDING
```

Only the first step is eligible.

Why not all four?

Because this workflow has dependencies:

```
CREATE_ORDER
      |
      v
RESERVE_STOCK
      |
      v
CHARGE_PAYMENT
      |
      v
CREATE_SHIPMENT
```

The second step cannot start until the first succeeds.

After the first step succeeds:

```
CREATE_ORDER       SUCCEEDED
RESERVE_STOCK      READY
CHARGE_PAYMENT     PENDING
CREATE_SHIPMENT    PENDING
```

Now the inventory operation is eligible.

Notice I used `READY` rather than setting every pending step to ready.

The difference:

- `PENDING`: Not yet eligible for execution.
- `READY`: Eligible to execute now.
- `RUNNING`: A worker has claimed it and is executing it.
- `SUCCEEDED`: Execution completed successfully.

We will design the complete state machine in Chapter 2.

### An important detail: Every saga progresses independently

Suppose we have three sagas:

```
SAGA-1:
    CREATE_ORDER       SUCCEEDED
    RESERVE_STOCK      READY

SAGA-2:
    CREATE_ORDER       SUCCEEDED
    RESERVE_STOCK      SUCCEEDED
    CHARGE_PAYMENT     READY

SAGA-3:
    CREATE_ORDER       READY
```

The scheduler can execute work from different sagas concurrently.

For example:

```
Worker 1 -> SAGA-1 -> Reserve inventory
Worker 2 -> SAGA-2 -> Charge payment
Worker 3 -> SAGA-3 -> Create order
```

There is no reason to make one saga wait for another unrelated saga.

However, steps belonging to the same sequential saga must respect their dependencies.

This is how one orchestrator can manage thousands of independent workflows.

## 6. The most important problem: The orchestrator cannot trust an HTTP response

Let's execute the first step.

Worker 1 wants to execute:

```
SAGA-1001
STEP: CREATE_ORDER
```

It calls Order Service:

```
POST /orders
Idempotency-Key: SAGA-1001-CREATE_ORDER
```

Order Service performs its operation:

```
@Transactional
public OrderResponse createOrder(
        CreateOrderRequest request,
        String idempotencyKey) {

    // Check whether this operation was already performed.

    // Create the order.

    // Store the idempotency key and result.

    // Both writes belong to the same database transaction.

    return response;
}
```

Order Service commits its database transaction and returns success.

Normally:

```
Worker
   |
   | HTTP request
   v
Order Service
   |
   | Create order
   | Commit DB transaction
   |
   v
HTTP 200 SUCCESS
   |
   v
Worker receives success
   |
   v
Orchestrator updates step status
```

Looks simple.

But now let's introduce a failure.

### Failure scenario A: The participant succeeds, but the response never arrives

```
Worker
   |
   | HTTP request
   v
Order Service
   |
   | Create order
   | Commit database
   |
   X Network timeout
```

What does the worker know?

Nothing definitive.

It does not know whether:

1. Order Service never received the request.
2. Order Service received it but failed.
3. Order Service created the order successfully, but the response got lost.

This is called an ambiguous outcome.

A network timeout does not mean the operation failed.

That is one of the most important concepts in distributed systems.

### Failure scenario B: The participant succeeds, but the orchestrator crashes

```
Worker
   |
   v
Order Service
   |
   | Order created successfully
   |
   v
HTTP SUCCESS
   |
   X Worker crashes
```

The order exists.

But the orchestrator database still says:

```
CREATE_ORDER: RUNNING
```

When another worker recovers this work, what should it do?

It will probably have to send the request again.

And that is correct — provided the participant handles duplicate requests safely.

We cannot completely prevent duplicate delivery in a distributed system by relying on HTTP or worker execution alone.

Instead, we design the system so that duplicate execution does not create duplicate business effects.

We'll build this mechanism in Chapter 4.

For now, remember:

The orchestrator provides reliable execution attempts. The participant must provide idempotent business effects.

AWS and the Idempotent Consumer pattern both emphasize this requirement.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

AWS Prescriptive Guidance+1



## 7. What should happen after a successful step?

Assume Order Service returns success.

The worker now needs to update the orchestrator database.

Conceptually:

```
BEGIN;

UPDATE saga_steps
SET status = 'SUCCEEDED',
    completed_at = NOW()
WHERE saga_id = 'SAGA-1001'
  AND step_name = 'CREATE_ORDER';

UPDATE saga_steps
SET status = 'READY'
WHERE saga_id = 'SAGA-1001'
  AND step_name = 'RESERVE_STOCK';

UPDATE sagas
SET current_step = 2,
    updated_at = NOW()
WHERE saga_id = 'SAGA-1001';

COMMIT;
```

The exact schema may differ, but the important part is that these changes are committed together.

The orchestrator should never persist:

```
CREATE_ORDER = SUCCEEDED
RESERVE_STOCK = READY
```

as two separate, uncoordinated database transactions.

Otherwise, a crash between updates could leave the workflow in an inconsistent state.

After the transaction commits:

```
SAGA-1001

CREATE_ORDER       SUCCEEDED
RESERVE_STOCK      READY
CHARGE_PAYMENT     PENDING
CREATE_SHIPMENT    PENDING
```

The scheduler will eventually pick up the next ready step.

Then the same process repeats.

## 8. What happens if a step fails?

Let's say inventory reservation fails.

```
CREATE_ORDER       SUCCEEDED
RESERVE_STOCK      FAILED
CHARGE_PAYMENT     PENDING
CREATE_SHIPMENT    PENDING
```

The orchestrator now needs to decide what to do.

Not every failure means immediate compensation.

For example:

| Failure                 | Typical response                          |
| ----------------------- | ----------------------------------------- |
| Temporary network error | Retry                                     |
| HTTP 503                | Retry with backoff                        |
| Rate limit              | Retry according to Retry-After/policy     |
| Invalid customer data   | Permanent failure                         |
| Insufficient inventory  | Business failure; compensate earlier work |
| Payment declined        | Business failure; compensate earlier work |
| Unknown timeout         | Resolve/retry safely using idempotency    |

This is an important distinction.

There are two broad recovery strategies.

Forward recovery: Try executing the failed operation again.

Backward recovery: Execute compensating operations for previously completed steps.

For example:

```
CREATE_ORDER       SUCCEEDED
RESERVE_STOCK      SUCCEEDED
CHARGE_PAYMENT     FAILED
```

The orchestrator may now enter compensation:

```
RELEASE_STOCK
CANCEL_ORDER
```

Why reverse order?

Because the most recently completed operation is generally the first one to counteract.

But remember: compensation is not guaranteed to restore the exact previous state. It is a new business action.

For example, a shipment already handed to a courier cannot necessarily be physically undone. The system may need to request shipment cancellation or initiate a return instead.

Compensation can also fail. In that case, it must remain a durable piece of work, with its own retry and recovery policy.

We'll give this an entire chapter.

## 9. One improvement to the architecture: How do workers know a saga is ready?

There are two common approaches.

### Approach A: Database polling

Workers periodically query the orchestrator database:

```
SELECT *
FROM saga_steps
WHERE status = 'READY'
  AND next_attempt_at <= NOW()
ORDER BY created_at
LIMIT 100;
```

They then claim eligible records and execute them.

This is straightforward and is often a good starting point.

### Approach B: Database + message broker

Suppose a step succeeds.

The orchestrator updates its database and needs to notify workers that the next step is ready.

A tempting implementation is:

```
1. Update database.
2. Publish message to Kafka/RabbitMQ.
```

But this creates a failure window:

```
Database update succeeds
        |
        v
Orchestrator crashes
        |
        X
Message never published
```

Now the next step is ready in the database, but workers never receive the notification.

The solution is the Transactional Outbox Pattern.

In the same database transaction, persist both:

- The saga state transition.
- An event saying the next step is ready.

For example:

```
BEGIN;

UPDATE saga_steps
SET status = 'SUCCEEDED'
WHERE ...;

UPDATE saga_steps
SET status = 'READY'
WHERE ...;

INSERT INTO outbox_events (
    event_type,
    aggregate_id,
    payload
)
VALUES (
    'SAGA_STEP_READY',
    'SAGA-1001',
    '...'
);

COMMIT;
```

A separate publisher reads the outbox and publishes events to the message broker.

If the publisher crashes after publishing but before marking the event as published, it may publish the same event again.

That's okay: consumers must be idempotent.

This is why the database remains authoritative and the message broker is primarily a notification and distribution mechanism.

The Transactional Outbox pattern addresses exactly this database/message-publishing atomicity problem.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://microservices.io\&sz=32)

microservices.io+1



My recommendation:

- Start with database polling if throughput and latency requirements permit.
- Introduce an outbox and broker when you need efficient event-driven dispatch.
- Do not make the broker the only place where workflow progress exists.

## 10. The complete lifecycle — Putting all the pieces together

Let's combine everything.

Suppose we receive:

```
POST /sagas

Idempotency-Key: checkout-123

ORDER_CHECKOUT
```

The complete lifecycle looks like this:

\#chatgpt-mermaid-\_r_34c\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_34c\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_34c\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_34c\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_34c\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34c\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_34c\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_34c\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_34c\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_34c\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_34c\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_34c\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_34c\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_34c\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_34c\_ p{margin:0;}#chatgpt-mermaid-\_r_34c\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34c\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34c\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34c\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_34c\_ .label text,#chatgpt-mermaid-\_r_34c\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34c\_ .node rect,#chatgpt-mermaid-\_r_34c\_ .node circle,#chatgpt-mermaid-\_r_34c\_ .node ellipse,#chatgpt-mermaid-\_r_34c\_ .node polygon,#chatgpt-mermaid-\_r_34c\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_34c\_ .rough-node .label text,#chatgpt-mermaid-\_r_34c\_ .node .label text,#chatgpt-mermaid-\_r_34c\_ .image-shape .label,#chatgpt-mermaid-\_r_34c\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_34c\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_34c\_ .rough-node .label,#chatgpt-mermaid-\_r_34c\_ .node .label,#chatgpt-mermaid-\_r_34c\_ .image-shape .label,#chatgpt-mermaid-\_r_34c\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_34c\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_34c\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_34c\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_34c\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_34c\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_34c\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_34c\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_34c\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_34c\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_34c\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_34c\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34c\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34c\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_34c\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34c\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_34c\_ .icon-shape,#chatgpt-mermaid-\_r_34c\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_34c\_ .icon-shape p,#chatgpt-mermaid-\_r_34c\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_34c\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_34c\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_34c\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_34c\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_34c\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_34c\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_34c\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_34c\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_34c\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_34c\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_34c\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_34c\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_34c\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_34c\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_34c\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_34c\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_34c\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_34c\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_34c\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_34c\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_34c\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_34c\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_34c\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_34c\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_34c\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_34c\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_34c\_ .node rect,#chatgpt-mermaid-\_r_34c\_ .node circle,#chatgpt-mermaid-\_r_34c\_ .node ellipse,#chatgpt-mermaid-\_r_34c\_ .node polygon,#chatgpt-mermaid-\_r_34c\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_34c\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_34c\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_34c\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_34c\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_34c\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Client submits saga requestValidate request andidempotency keyDB transaction: create sagaand stepsCommit transactionReturn 202 AcceptedScheduler finds READY workWorker claims stepPersist execution ownershipCall participant with stableidempotency keyParticipant result?Persist SUCCEEDED and nextstep READYPersist retry time andattemptStart compensationExecute completed steps'compensations in reverseCompensation complete?Saga COMPENSATEDRetry / manual recoveryResolve or retry idempotentlyAny steps remaining?Saga SUCCEEDEDSuccessTransient failurePermanent business failureYesNoUnknown outcomeNo

One nuance: the scheduler need not continuously scan every saga and every step. It should find eligible actions. How it claims those actions safely when several workers operate simultaneously is a major part of Chapter 3.

### The actual ownership boundaries

This distinction will help avoid a lot of architectural confusion:

| Responsibility                              | Owner                               |
| ------------------------------------------- | ----------------------------------- |
| Deciding which business step comes next     | Orchestrator                        |
| Persisting workflow execution state         | Orchestrator DB                     |
| Creating an order                           | Order Service                       |
| Reserving inventory                         | Inventory Service                   |
| Charging/refunding money                    | Payment Service                     |
| Creating/cancelling shipment                | Shipping Service                    |
| Preventing duplicate participant operations | Each participant service            |
| Deciding whether to retry or compensate     | Orchestrator                        |
| Actually reversing business effects         | Participant compensation operations |

The orchestrator coordinates. It does not directly manipulate another service's database.

## 11. The three rules I want you to remember

Everything we build in the upcoming chapters will follow these three rules.

Rule 1: Never depend on in-memory state for workflow recovery.

The orchestrator must be able to restart and continue every unfinished saga using its database.

Rule 2: Never assume an HTTP timeout means the operation failed.

The remote service might have committed successfully. Every participant operation must be designed for safe retries.

Rule 3: Never confuse compensation with database rollback.

Compensation is another business transaction, with its own failure modes.

If these three ideas are clear, the architecture starts becoming much easier to reason about.

## Chapter 1 — What we established

Your original architecture was fundamentally right. We made five important refinements:

1. Saga creation is atomic: Create the main saga record, all step records, and initial scheduling information in one database transaction.
2. 202 Accepted is appropriate: The orchestrator acknowledges durable acceptance, not completion of the distributed business operation.
3. Workers execute independently: Multiple workers can execute different sagas concurrently, while respecting dependencies within each saga.
4. Execution is recoverable: The database holds workflow progress; workers can crash and other workers can resume execution.
5. Participants must be idempotent: The orchestrator cannot guarantee that an HTTP request is delivered exactly once or that a response is received exactly once.

One additional point to keep in mind: A saga provides business-level consistency through eventual completion or compensation. It does not automatically provide the isolation guarantees of a single ACID transaction.

### Next: Chapter 2 — Designing the Database and State Machine

We'll design the actual tables, including:

- `sagas`
- `saga_steps`
- `saga_attempts` (execution history)
- `outbox_events`
- Idempotency records

More importantly, we'll understand why each table exists, which statuses are necessary, which transitions are legal, and how to avoid contradictory states.

We'll use an actual Order Checkout example and trace every database transaction, including what gets committed and what happens when the application crashes between operations.










---








This is a very good question. In fact, you don't strictly need workers. You can directly call the next service from the orchestrator.

But there's an important reason production systems introduce workers: separating the responsibility of accepting a workflow from the responsibility of executing it.

Let's derive this from scratch.

# 1. Start with the simplest possible implementation

Imagine we build a Saga Orchestrator.

A client calls:

```
POST /sagas
```

The orchestrator does this:

```
public void startSaga(SagaRequest request) {

    // 1. Persist saga and all steps.

    // 2. Execute first step.
    orderService.createOrder();

    // 3. Execute second step.
    inventoryService.reserveInventory();

    // 4. Execute third step.
    paymentService.chargePayment();

    // 5. Execute fourth step.
    shippingService.createShipment();

    // 6. Mark saga successful.
}
```

This is perfectly valid as a basic orchestration implementation.

There is no worker, no scheduler, no message broker.

The orchestrator directly calls each participant.

Let's see what happens when we try to make this production-ready.

# 2. Problem 1: What happens to the API request?

Suppose the entire saga takes 15 seconds.

```
Client
  |
  | POST /sagas
  v
Orchestrator
  |
  | Create Order       (2 seconds)
  |
  | Reserve Inventory  (3 seconds)
  |
  | Charge Payment     (5 seconds)
  |
  | Create Shipment    (5 seconds)
  |
  v
Return HTTP Response
```

The client has to wait 15 seconds.

Now imagine:

- 1,000 clients initiate sagas simultaneously.
- Each saga takes 15 seconds.
- Your orchestrator has limited HTTP request-processing threads.

What happens?

Those request threads become occupied waiting for remote services.

You might eventually exhaust your request thread pool.

For example:

```
Orchestrator HTTP thread pool = 200 threads

200 requests arrive.

All 200 threads are occupied executing distributed transactions.

Another 500 requests arrive.

They must wait in the server queue or get rejected.
```

The problem is that accepting a request and executing a potentially long-running workflow have different resource and latency requirements.

We can solve this by separating them.

# 3. Introduce background execution

Instead of executing the whole saga inside the API request:

```
public SagaResponse startSaga(SagaRequest request) {

    Saga saga = saveSagaAndSteps(request);

    executeSaga(saga);

    return new SagaResponse(saga.getId());
}
```

We change it to:

```
public SagaResponse startSaga(SagaRequest request) {

    Saga saga = saveSagaAndSteps(request);

    return new SagaResponse(
        saga.getId(),
        "RUNNING"
    );
}
```

Now the API returns immediately after the database transaction commits.

```
Client
   |
   | POST /sagas
   v
Orchestrator API
   |
   | Persist saga + steps
   |
   | COMMIT
   |
   v
202 ACCEPTED
```

Meanwhile, something else executes the saga.

That something is a worker.

```
                   Orchestrator
                 /              \
                /                \
         API Handler           Worker
              |                   |
              v                   v
          Save Saga          Execute Saga
              |                   |
              v                   v
          Database  <---------- Database
                                  |
                                  v
                             Order Service
                                  |
                                  v
                           Inventory Service
                                  |
                                  v
                            Payment Service
```

Notice something:

The worker does not necessarily mean a separate microservice.

It is simply a component responsible for executing eligible workflow tasks.

It could be:

- A background thread in the same Spring Boot application.
- A dedicated worker process.
- Multiple worker instances running in different machines.
- Kafka or RabbitMQ consumers.
- Database-polling workers.

The distinction is logical, not necessarily physical.

# 4. Problem 2: What happens if the orchestrator crashes?

This is actually the more important reason for introducing workers.

Let's go back to your idea:

> After one step succeeds, directly call the next service.

Suppose we implement it like this:

```
public void executeSaga(Saga saga) {

    orderService.createOrder();

    updateStepStatus("CREATE_ORDER", "SUCCEEDED");

    inventoryService.reserveInventory();

    updateStepStatus("RESERVE_INVENTORY", "SUCCEEDED");

    paymentService.chargePayment();

    updateStepStatus("CHARGE_PAYMENT", "SUCCEEDED");
}
```

Consider this execution:

```
CREATE_ORDER
      |
      v
SUCCESS
      |
      v
Update DB: SUCCEEDED
      |
      v
RESERVE_INVENTORY
      |
      X
Orchestrator crashes
```

The database contains:

```
CREATE_ORDER        SUCCEEDED
RESERVE_INVENTORY   RUNNING
CHARGE_PAYMENT      PENDING
```

The process that was executing the saga is gone.

Who will execute the remaining steps?

There is no one.

You might say:

"Okay, when the application restarts, I'll scan the database and resume the incomplete sagas."

Exactly!

But think about what you just introduced.

You need a background process that:

1. Finds unfinished sagas.
2. Finds the next eligible step.
3. Determines whether it can be executed.
4. Executes it.
5. Recovers abandoned execution attempts.

Congratulations! You've just introduced the basic responsibility of a worker.

The important insight is that workers aren't introduced merely to call APIs asynchronously.

They provide a controlled, repeatable execution mechanism that can resume durable work.

# 5. Problem 3: What if the orchestrator commits but crashes before calling the next service?

Let's examine a particularly important failure window.

Suppose:

```
Step 1: CREATE_ORDER
```

succeeds.

The orchestrator performs:

```
BEGIN;

UPDATE saga_steps
SET status = 'SUCCEEDED'
WHERE step_name = 'CREATE_ORDER';

UPDATE saga_steps
SET status = 'READY'
WHERE step_name = 'RESERVE_INVENTORY';

COMMIT;
```

Now the application crashes immediately after the commit.

It never gets to:

```
inventoryService.reserveInventory();
```

What happens?

The database says:

```
CREATE_ORDER        SUCCEEDED
RESERVE_INVENTORY   READY
```

But nobody has actually called Inventory Service.

How does the system recover?

A worker or scheduler periodically looks for:

```
SELECT *
FROM saga_steps
WHERE status = 'READY';
```

It discovers that inventory reservation is waiting.

It executes the operation.

This is an important property:

The database records what needs to happen. Workers ensure that eligible work eventually gets attempted.

Even with direct step-to-step calls, you still need some recovery mechanism for this failure window.

# 6. Problem 4: What if 10,000 sagas need execution?

Now imagine your orchestrator is managing 10,000 independent sagas.

Their states might look like:

```
SAGA-1    RESERVE_INVENTORY READY
SAGA-2    CHARGE_PAYMENT READY
SAGA-3    CREATE_ORDER READY
SAGA-4    CREATE_SHIPMENT READY
SAGA-5    RESERVE_INVENTORY READY
...
SAGA-10000 CREATE_ORDER READY
```

If you execute everything inside the API request threads, you couple execution capacity to API traffic and request-thread capacity.

Workers allow us to scale execution independently.

For example:

```
                    Orchestrator DB
                          |
                          v
                    Work Scheduler
                          |
             +------------+------------+
             |            |            |
             v            v            v
          Worker 1     Worker 2     Worker 3
             |            |            |
             v            v            v
           Saga 1       Saga 2       Saga 3
           Saga 4       Saga 5       Saga 6
           Saga 7       Saga 8       Saga 9
```

We can run 3, 10, or 100 workers depending on workload and available capacity.

We can also have different worker pools for:

- Payment operations.
- Inventory operations.
- Compensation operations.
- High-priority workflows.

This gives us independent execution capacity and backpressure.

For example, if Payment Service becomes slow, we can limit how many payment-related operations execute concurrently rather than allowing all workers to overwhelm it.

Workers aren't a way to make downstream services faster. They give us control over how much work we send to them.

# 7. But couldn't we directly call the next step AND have a recovery scheduler?

Absolutely!

This is a perfectly reasonable architecture.

Consider:

```
public void executeNextStep(Saga saga) {

    Step step = getNextReadyStep(saga);

    executeStep(step);

    markStepSuccessful(step);

    Step nextStep = getNextStep(saga);

    executeNextStep(saga);
}
```

Additionally, run a periodic recovery job:

```
@Scheduled(fixedDelay = 5000)
public void recoverIncompleteSagas() {

    List<Saga> sagas = findReadySagas();

    for (Saga saga : sagas) {
        executeNextStep(saga);
    }
}
```

This can work.

However, we should address two things.

First: The direct execution chain still occupies the thread that started it. If the saga is initiated by the API handler, the original request remains blocked.

You could instead run the chain in a background executor. At that point, you're already using a worker-like execution model.

Second: You must avoid two execution paths processing the same step simultaneously.

For example:

```
Direct execution:
    Worker A starts RESERVE_INVENTORY

Recovery scheduler:
    Worker B sees RESERVE_INVENTORY as READY
    Worker B also starts RESERVE_INVENTORY
```

Now two workers execute the same step.

You need atomic claiming, concurrency control, and idempotency.

We'll discuss the exact implementation in Chapter 3.

# 8. So what is the actual difference between these approaches?

Let's compare them.

| Concern                     | Direct synchronous execution              | Background workers                      |
| --------------------------- | ----------------------------------------- | --------------------------------------- |
| API response                | Waits for execution (unless detached)     | Returns after durable acceptance        |
| Long-running workflows      | Holds request thread                      | Independent of request thread           |
| Crash recovery              | Needs a separate recovery mechanism       | Workers can rediscover persisted work   |
| Horizontal scaling          | Coupled to API execution capacity         | Execution scales independently          |
| Backpressure                | Harder to control through request threads | Can control worker concurrency          |
| Retry scheduling            | Can be implemented directly               | Naturally fits a durable task scheduler |
| Independent execution pools | More difficult                            | Easier                                  |
| Operational control         | Less flexible                             | More flexible                           |

Neither approach magically solves duplicate execution or distributed failures.

Those still require durable state transitions, atomic claims, stable idempotency keys, and participant-side deduplication.

# 9. What I would actually build

For a reliable production-grade Saga Orchestrator, I would use this architecture:

\#chatgpt-mermaid-\_r_398\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_398\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_398\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_398\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_398\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_398\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_398\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_398\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_398\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_398\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_398\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_398\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_398\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_398\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_398\_ p{margin:0;}#chatgpt-mermaid-\_r_398\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_398\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_398\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_398\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_398\_ .label text,#chatgpt-mermaid-\_r_398\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_398\_ .node rect,#chatgpt-mermaid-\_r_398\_ .node circle,#chatgpt-mermaid-\_r_398\_ .node ellipse,#chatgpt-mermaid-\_r_398\_ .node polygon,#chatgpt-mermaid-\_r_398\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_398\_ .rough-node .label text,#chatgpt-mermaid-\_r_398\_ .node .label text,#chatgpt-mermaid-\_r_398\_ .image-shape .label,#chatgpt-mermaid-\_r_398\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_398\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_398\_ .rough-node .label,#chatgpt-mermaid-\_r_398\_ .node .label,#chatgpt-mermaid-\_r_398\_ .image-shape .label,#chatgpt-mermaid-\_r_398\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_398\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_398\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_398\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_398\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_398\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_398\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_398\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_398\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_398\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_398\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_398\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_398\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_398\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_398\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_398\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_398\_ .icon-shape,#chatgpt-mermaid-\_r_398\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_398\_ .icon-shape p,#chatgpt-mermaid-\_r_398\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_398\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_398\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_398\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_398\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_398\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_398\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_398\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_398\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_398\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_398\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_398\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_398\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_398\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_398\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_398\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_398\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_398\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_398\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_398\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_398\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_398\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_398\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_398\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_398\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_398\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_398\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_398\_ .node rect,#chatgpt-mermaid-\_r_398\_ .node circle,#chatgpt-mermaid-\_r_398\_ .node ellipse,#chatgpt-mermaid-\_r_398\_ .node polygon,#chatgpt-mermaid-\_r_398\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_398\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_398\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_398\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_398\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_398\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Execution WorkersWorker 1Worker 2Worker NCalling ServiceSaga APIPostgreSQLScheduler / DispatcherOrder / Inventory / Payment /ShippingPOST /sagasAtomic saga + steps insert202 AcceptedEligible workEligible workEligible workClaim and executeClaim and executeClaim and executeResultsResultsResultsPersist transitionsPersist transitionsPersist transitions

The lifecycle:

1. API receives the request.
2. API atomically persists the saga and its steps.
3. API returns `202 Accepted`.
4. Scheduler identifies eligible steps.
5. A worker atomically claims one step.
6. Worker calls the participant service.
7. Worker records the result and makes the next step eligible.
8. Workers continue executing eligible steps.
9. If a worker crashes, another worker can reclaim abandoned work.
10. If a saga fails permanently, the same execution infrastructure can execute compensation steps.

The scheduler can use database polling initially. Later, we can introduce an outbox and message broker without changing the fundamental workflow model.

# 10. The most important distinction to remember

There are actually three separate concepts here:

| Concept                   | Question it answers                             |
| ------------------------- | ----------------------------------------------- |
| Saga Orchestration        | What should happen next?                        |
| Durable workflow database | What has happened, and what remains to be done? |
| Worker execution          | Who actually performs the next operation?       |

You can combine all three inside one application, but their responsibilities remain different.

My recommendation: Don't think of a worker as an additional architectural complication or necessarily another microservice. Think of it as a replaceable executor of durable work.

The orchestrator API accepts work. The database remembers work. Workers execute work.

That separation makes the system easier to recover, scale, control, and operate.

And one final nuance: a worker alone does not make the system reliable. The combination of durable workflow state, safe work claiming, idempotent participant operations, retries, and crash recovery is what makes it reliable.

