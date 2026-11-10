# Chapter 6 — Compensation: How Rollback Actually Works

This chapter is extremely important because compensation is where the Saga pattern differs most from traditional database transactions.

We've already built a reliable mechanism for:

- Executing forward operations.
- Recovering crashed workers.
- Preventing duplicate operations.
- Retrying transient failures.
- Resolving unknown outcomes.

Now imagine our Order Checkout workflow reaches this state:

```
CREATE_ORDER        SUCCEEDED
RESERVE_INVENTORY   SUCCEEDED
CHARGE_PAYMENT      SUCCEEDED
CREATE_SHIPMENT     FAILED
```

We have a problem.

The order exists, inventory has been reserved, and the customer has been charged.

But the shipment could not be created.

Our business process cannot simply stop here.

We need a way to counteract the effects of previous operations.

That's compensation.

## 1. First, understand why traditional rollback doesn't work

Imagine we have a traditional database transaction:

```
@Transactional
public void processOrder() {

    orderRepository.save(order);

    inventoryRepository.reserve(product);

    paymentRepository.charge(customer);

    shippingRepository.createShipment();
}
```

Suppose the last operation fails.

The database transaction can roll back all its participating database changes.

But our distributed workflow looks different:

```
Order Service
    |
    | Local DB transaction commits
    v
Inventory Service
    |
    | Local DB transaction commits
    v
Payment Service
    |
    | Local DB transaction commits
    v
Shipping Service
    |
    X Failure
```

By the time Shipping Service fails, the previous services have already committed their own transactions.

We cannot reach into their databases and undo those commits.

Even if we technically had access, that would violate service ownership and could interfere with unrelated business operations.

So we need a different mechanism.

### Introduce business-level compensation

Instead of physically rolling back the old transactions, we execute new transactions that counteract their business effects.

For example:

| Forward operation | Compensating operation        |
| ----------------- | ----------------------------- |
| Create order      | Cancel order                  |
| Reserve inventory | Release reservation           |
| Charge payment    | Refund payment                |
| Authorize payment | Void authorization            |
| Create shipment   | Request shipment cancellation |
| Book hotel        | Cancel booking                |

Notice something important:

We are not deleting historical records.

We are creating new business operations that counteract earlier operations.

For example:

```
Payment history:

10:00  CHARGE  ₹2,500  SUCCESS
10:05  REFUND  ₹2,500  SUCCESS
```

The original charge is not erased.

The refund is a separate financial transaction.

This is why compensation is not the same as database rollback.

Microsoft's Compensating Transaction guidance makes this distinction explicit: compensations are application-specific operations, can fail themselves, and might not restore the exact original state.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://learn.microsoft.com\&sz=32)

Microsoft Learn+1



# 2. The most important mental model: Every forward operation needs a recovery strategy

Let's revisit our checkout workflow.

```
ORDER_CHECKOUT

Step 1: CREATE_ORDER
Step 2: RESERVE_INVENTORY
Step 3: CHARGE_PAYMENT
Step 4: CREATE_SHIPMENT
```

Before implementing this workflow, we should define two things for every step:

1. What operation should execute if everything goes normally?
2. What should happen if a later operation makes the overall business process fail?

For example:

| Step | Forward operation | Compensation                |
| ---- | ----------------- | --------------------------- |
| 1    | Create order      | Cancel order                |
| 2    | Reserve inventory | Release inventory           |
| 3    | Charge payment    | Refund payment              |
| 4    | Create shipment   | Cancel shipment if possible |

Think of a Saga step as a business action with a recovery strategy.

Conceptually:

```
interface SagaStep {

    StepResult execute(SagaContext context);

    CompensationResult compensate(
        SagaContext context,
        StepResult originalResult
    );
}
```

This is illustrative; actual implementations may define forward and compensation handlers separately.

The important thing is that compensation must be explicitly designed.

It cannot be generated automatically by the orchestrator.

Why?

Because only the business domain knows how to counteract an operation correctly.

For example:

```
Inventory Service:

Forward:
    Reserve 2 units.

Compensation:
    Release the specific reservation.
```

It shouldn't simply execute:

```
UPDATE inventory
SET available_quantity = available_quantity + 2;
```

Why not?

Because the reservation might have expired, been consumed, partially fulfilled, or already been released.

The correct compensation should operate on the original reservation identity and validate its current state.

This is the difference between reversing a database number and reversing a business operation.

# 3. Let's understand the complete failure scenario

Assume:

```
SAGA-1001

CREATE_ORDER        SUCCEEDED
RESERVE_INVENTORY   SUCCEEDED
CHARGE_PAYMENT      SUCCEEDED
CREATE_SHIPMENT     FAILED
```

The orchestrator knows that shipment creation permanently failed.

We decide to compensate.

What does the orchestrator do?

It should not simply execute:

```
orderService.cancelOrder();
inventoryService.releaseInventory();
paymentService.refundPayment();
```

There are several problems with doing this directly:

- What if the orchestrator crashes halfway through?
- What if the refund times out?
- What if inventory release succeeds but its response is lost?
- What if compensation is accidentally executed twice?
- What if two workers begin compensation simultaneously?
- What if the shipment operation's outcome was actually unknown?

Instead, compensation must become another durable workflow.

The orchestrator doesn't perform compensation as an emergency cleanup block. It schedules compensating actions in its database, just like normal forward actions.

That is the key architectural insight.

## 3.1 The overall state transition

Before compensation:

```
SAGA-1001 = RUNNING
```

After a definitive forward failure:

```
SAGA-1001 = COMPENSATING
```

We then schedule compensation actions.

For example:

```
REFUND_PAYMENT       READY
RELEASE_INVENTORY    BLOCKED
CANCEL_ORDER         BLOCKED
```

Once all required compensations finish:

```
SAGA-1001 = COMPENSATED
```

The same worker engine handles both forward and compensation actions.

We do not need a completely separate compensation execution infrastructure.

# 4. Why do compensations normally execute in reverse order?

This is a natural question.

Suppose our forward workflow is:

```
S1: CREATE_ORDER
        |
        v
S2: RESERVE_INVENTORY
        |
        v
S3: CHARGE_PAYMENT
        |
        v
S4: CREATE_SHIPMENT
```

Each operation depends on the previous one.

Now suppose S4 fails.

We want to execute:

```
C3: REFUND_PAYMENT
        |
        v
C2: RELEASE_INVENTORY
        |
        v
C1: CANCEL_ORDER
```

Why this order?

Think about the dependencies.

- Payment is associated with an order and its inventory reservation.
- Inventory reservation is associated with the order.
- Therefore, reversing the later business effects before reversing their prerequisites is generally safer.

Consider what happens if we cancel the order first.

The payment might still be successful, and the inventory might still be reserved.

We would have a cancelled order associated with active payment and inventory operations.

That may violate business invariants or make subsequent operations harder.

Reverse execution naturally respects the dependency chain.

### The intuition

Forward:

```
Build something layer by layer.
```

Compensation:

```
Remove the dependent layers before removing the foundations.
```

This is why reverse order is the natural default for a simple sequential Saga.

But there is an important qualification:

Compensation does not always have to happen in the exact reverse of execution order.

Business dependencies determine the correct order, not an unconditional rule that the step number must decrease.

Microsoft explicitly documents that some compensations can execute in parallel and some must follow business-specific ordering rather than strict reverse order.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://learn.microsoft.com\&sz=32)

Microsoft Learn



# 5. How should the orchestrator represent compensation in the database?

Remember our previous `saga_actions` table.

We introduced it because forward execution and compensation both represent executable work.

Let's extend that design.

Conceptually:

```
SAGA-1001
    |
    +-- STEP: CREATE_ORDER
    |       |
    |       +-- FORWARD action
    |       |       status = SUCCEEDED
    |       |
    |       +-- COMPENSATE action
    |               status = READY
    |
    +-- STEP: RESERVE_INVENTORY
    |       |
    |       +-- FORWARD action
    |       |       status = SUCCEEDED
    |       |
    |       +-- COMPENSATE action
    |               status = READY
    |
    +-- STEP: CHARGE_PAYMENT
            |
            +-- FORWARD action
            |       status = SUCCEEDED
            |
            +-- COMPENSATE action
                    status = BLOCKED
```

For a sequential Saga, the payment refund must happen before inventory release, and inventory release must happen before order cancellation.

The action model might look like this:

```
CREATE TABLE saga_actions (
    id UUID PRIMARY KEY,

    saga_id UUID NOT NULL,
    step_id UUID NOT NULL,

    action_key VARCHAR(150) NOT NULL,
    action_type VARCHAR(30) NOT NULL,

    status VARCHAR(50) NOT NULL,

    idempotency_key VARCHAR(255) NOT NULL,

    request_payload JSONB,
    response_payload JSONB,

    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,

    lease_owner VARCHAR(255),
    lease_until TIMESTAMPTZ,
    claim_generation BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    UNIQUE (saga_id, action_key),
    UNIQUE (idempotency_key)
);
```

Here, `action_key` distinguishes individual executable commands.

For example:

```
CREATE_ORDER_FORWARD

CREATE_ORDER_COMPENSATE

RESERVE_INVENTORY_FORWARD

RESERVE_INVENTORY_COMPENSATE

CHARGE_PAYMENT_FORWARD

CHARGE_PAYMENT_COMPENSATE
```

Why not just use `step_order`?

Because a more complex workflow may have multiple forward or compensation actions associated with a logical step.

For example, canceling a booking might require:

```
1. Cancel hotel booking.
2. Cancel airport transfer.
3. Refund deposit.
```

You may model those as separate compensation actions.

The table should reflect actual executable business operations, rather than assuming every step always has exactly one reverse operation.

### What information must compensation have?

Consider the inventory operation.

Forward request:

```
{
  "orderId": "ORDER-123",
  "productId": "PRODUCT-100",
  "quantity": 2
}
```

Response:

```
{
  "reservationId": "RES-987",
  "status": "RESERVED"
}
```

Compensation request:

```
{
  "reservationId": "RES-987",
  "reason": "ORDER_CHECKOUT_FAILED"
}
```

Notice that compensation uses the reservation ID returned by the forward operation.

This is why we persisted forward responses in Chapter 2.

Without them, the orchestrator might not know which exact business resource needs to be compensated.

Persist the information needed to compensate at the moment the forward step succeeds.

Don't assume that you can reconstruct it later by querying a mutable business entity.

# 6. The most critical implementation detail: When exactly does compensation start?

Let's consider:

```
CREATE_ORDER        SUCCEEDED
RESERVE_INVENTORY   SUCCEEDED
CHARGE_PAYMENT      SUCCEEDED
CREATE_SHIPMENT     FAILED
```

The failure is confirmed.

What should happen in the database?

We must make the transition to compensation durable and atomic.

Conceptually:

```
BEGIN;

-- 1. Mark the failed forward action.
UPDATE saga_actions
SET status = 'FAILED'
WHERE id = :failed_action_id
  AND status = 'RUNNING'
  AND lease_owner = :worker_id
  AND claim_generation = :generation
  AND lease_until > clock_timestamp();

-- Verify that exactly one row was updated.

-- 2. Stop forward progress.
UPDATE sagas
SET status = 'COMPENSATING',
    updated_at = clock_timestamp()
WHERE id = :saga_id
  AND status = 'RUNNING';

-- Verify that the expected state transition occurred.

-- 3. Prevent further forward actions from being scheduled.

-- 4. Schedule compensation work or create a durable
--    compensation-planning task.

-- 5. Record an audit/outbox event.

COMMIT;
```

There is an important refinement here.

What if other forward steps were executing concurrently?

For example:

```
                 CREATE_ORDER
                      |
               +------+------+
               |             |
        RESERVE_STOCK    VALIDATE_ADDRESS
               |             |
               +------+------+
                      |
                CHARGE_PAYMENT
```

Suppose `RESERVE_STOCK` fails while `VALIDATE_ADDRESS` is still running.

We cannot simply declare that all forward work has stopped and immediately compensate the order.

Why?

Because the address validation worker may still be executing, and more importantly, a participant operation may have committed even if its worker hasn't received the response.

We need to manage in-flight actions.

### A safe compensation initiation protocol

Step 1: Switch the saga into COMPENSATING.

This prevents the scheduler from claiming any new forward actions.

Step 2: Stop activating downstream forward dependencies.

No more normal progression should occur.

Step 3: Resolve all already-running forward actions.

For each in-flight action:

- Allow a valid worker to report a definitive outcome.
- Reconcile actions whose outcomes become unknown.
- If an in-flight operation succeeded, include its effect in the compensation plan.
- If it definitively did not execute, no compensation is needed for that operation.

Step 4: Build the compensation plan.

Include every confirmed forward business effect that requires compensation.

Step 5: Execute the compensation plan according to its dependencies.

This is a crucial detail.

Fencing tokens protect the orchestrator database from stale workers, but they do not automatically stop a remote participant from completing an already-sent request.

Therefore, you must not compensate a participant operation while its forward outcome is unresolved.

For particularly sensitive operations, participant APIs should also support operation-status lookup, cancellation where possible, or business-level fencing to prevent late execution.

# 7. How to create compensation actions atomically

Let's assume all relevant forward outcomes are now established.

Our database contains:

```
CREATE_ORDER        SUCCEEDED
RESERVE_INVENTORY   SUCCEEDED
CHARGE_PAYMENT      SUCCEEDED
CREATE_SHIPMENT     FAILED
```

For this example, the compensation actions are:

```
REFUND_PAYMENT
RELEASE_INVENTORY
CANCEL_ORDER
```

We need to persist these actions without creating duplicates.

Conceptually:

```
BEGIN;

INSERT INTO saga_actions (
    id,
    saga_id,
    step_id,
    action_key,
    action_type,
    status,
    idempotency_key,
    request_payload,
    created_at,
    updated_at
)
VALUES
(
    :action_1,
    :saga_id,
    :payment_step_id,
    'CHARGE_PAYMENT_COMPENSATE',
    'COMPENSATE',
    'READY',
    :refund_idempotency_key,
    :refund_payload,
    clock_timestamp(),
    clock_timestamp()
),
(
    :action_2,
    :saga_id,
    :inventory_step_id,
    'RESERVE_INVENTORY_COMPENSATE',
    'COMPENSATE',
    'BLOCKED',
    :release_idempotency_key,
    :release_payload,
    clock_timestamp(),
    clock_timestamp()
),
(
    :action_3,
    :saga_id,
    :order_step_id,
    'CREATE_ORDER_COMPENSATE',
    'COMPENSATE',
    'BLOCKED',
    :cancel_idempotency_key,
    :cancel_payload,
    clock_timestamp(),
    clock_timestamp()
);

COMMIT;
```

The `UNIQUE (saga_id, action_key)` constraint protects against duplicate compensation planning.

The first compensation action is READY.

The others are BLOCKED until their predecessors complete.

You can combine compensation-plan creation with the transition that establishes compensation mode, provided all required outcomes are already known.

For parallel workflows with unresolved forward operations, the plan may need to be built incrementally as each outcome becomes established.

The key is that no compensation action should be lost between discovering a failure and persisting the action that must handle it.

# 8. How does compensation execution actually happen?

Here's the beautiful part.

We reuse the exact same worker execution engine from Chapter 3.

Remember:

```
READY
  |
  v
RUNNING
  |
  +---- SUCCEEDED
  |
  +---- RETRY_WAIT
  |
  +---- UNKNOWN / RECONCILIATION
```

These transitions apply to compensation actions too.

Suppose the worker claims:

```
REFUND_PAYMENT
```

It calls:

```
POST /payments/PAY-100/refunds
Idempotency-Key: SAGA-1001:CHARGE_PAYMENT:COMPENSATE
```

Payment Service executes the refund using its own local transaction and idempotency protection.

If successful:

```
REFUND_PAYMENT = SUCCEEDED
```

The orchestrator then activates:

```
RELEASE_INVENTORY = READY
```

Inventory Service receives:

```
POST /inventory/reservations/RES-987/release
Idempotency-Key: SAGA-1001:RESERVE_INVENTORY:COMPENSATE
```

If successful:

```
RELEASE_INVENTORY = SUCCEEDED
CANCEL_ORDER = READY
```

Finally:

```
POST /orders/ORDER-123/cancel
Idempotency-Key: SAGA-1001:CREATE_ORDER:COMPENSATE
```

When this succeeds:

```
CANCEL_ORDER = SUCCEEDED
```

The saga becomes:

```
SAGA-1001 = COMPENSATED
```

The same mechanisms we already designed apply:

- Workers claim compensation actions atomically.
- Leases protect execution ownership.
- Fencing protects state updates.
- Stable idempotency keys protect repeated compensation requests.
- Retry policies handle transient errors.
- Reconciliation handles unknown outcomes.

No special compensation worker infrastructure is required.

# 9. Does compensation always have to execute one step at a time in reverse order?

No.

Let's improve our mental model.

Reverse execution is a good default for a strictly sequential workflow.

But real workflows are often graphs, not simple lists.

Consider:

```
                 CREATE_ORDER
                      |
               +------+------+
               |             |
        RESERVE_STOCK    VALIDATE_ADDRESS
               |             |
               +------+------+
                      |
                CHARGE_PAYMENT
                      |
                CREATE_SHIPMENT
```

Suppose shipment creation fails.

We might have:

```
CREATE_ORDER        SUCCEEDED
RESERVE_STOCK       SUCCEEDED
VALIDATE_ADDRESS    SUCCEEDED
CHARGE_PAYMENT      SUCCEEDED
CREATE_SHIPMENT     FAILED
```

Possible compensations:

```
REFUND_PAYMENT
       |
       +----------------------+
       |                      |
RELEASE_STOCK          REVERSE_ADDRESS_HOLD
       |                      |
       +----------+-----------+
                  |
             CANCEL_ORDER
```

Some compensation actions may run in parallel.

Why?

After payment is refunded, releasing stock and reversing a separately held address verification resource might be independent.

But order cancellation should wait until its dependent compensations complete if that is required by the business rules.

### The general rule

For a dependency graph:

```
A -> C
B -> C
C -> D
```

Forward order:

```
A and B can run in parallel.
C waits for both.
D waits for C.
```

If we must compensate:

```
D compensation first.
Then C compensation.
Then A and B compensations can run in parallel.
```

In general, compensation follows the inverse dependency graph, not necessarily a literal reverse list.

The orchestrator therefore needs dependency metadata.

For example:

```
CREATE TABLE saga_action_dependencies (
    action_id UUID NOT NULL,
    depends_on_action_id UUID NOT NULL,

    PRIMARY KEY (
        action_id,
        depends_on_action_id
    )
);
```

For each action, the scheduler determines eligibility from the status of its prerequisites.

A compensation action is READY only when every action it depends on has succeeded.

There should also be cycle detection when validating workflow definitions.

This allows the orchestrator to support sequential, branching and parallel workflows using the same execution engine.

# 10. What happens if compensation itself fails?

This is where many simplistic Saga implementations fall apart.

Imagine:

```
REFUND_PAYMENT = SUCCEEDED

RELEASE_INVENTORY = RUNNING

CANCEL_ORDER = BLOCKED
```

Inventory Service returns:

```
503 Service Unavailable
```

What should happen?

We should not mark the entire saga as COMPENSATED.

Because inventory is still reserved.

We should also not necessarily mark the entire saga as permanently FAILED.

The compensation might succeed on the next attempt.

Instead:

```
REFUND_PAYMENT = SUCCEEDED

RELEASE_INVENTORY = RETRY_WAIT

CANCEL_ORDER = BLOCKED

SAGA = COMPENSATING
```

The compensation engine continues retrying.

When inventory release succeeds:

```
RELEASE_INVENTORY = SUCCEEDED

CANCEL_ORDER = READY
```

After order cancellation succeeds:

```
SAGA = COMPENSATED
```

### What if compensation permanently fails?

Suppose the inventory reservation cannot be released because the inventory system has entered an unrecoverable state.

Or the participant can no longer determine whether the stock was released.

Now:

```
RELEASE_INVENTORY = MANUAL_INTERVENTION_REQUIRED

SAGA = MANUAL_INTERVENTION_REQUIRED
```

The system must preserve the information required to investigate and resolve the problem.

For example:

```
Saga ID: SAGA-1001

Business reference: ORDER-123

Forward:
    CREATE_ORDER = SUCCEEDED
    RESERVE_INVENTORY = SUCCEEDED
    CHARGE_PAYMENT = SUCCEEDED

Compensation:
    REFUND_PAYMENT = SUCCEEDED
    RELEASE_INVENTORY = UNKNOWN
    CANCEL_ORDER = BLOCKED

Reason:
    Inventory release outcome could not be established.
```

The operational team can now investigate the exact reservation and its history.

Once resolved, the operator may:

- Confirm that inventory was already released.
- Approve a safe retry.
- Initiate an alternative business recovery.
- Record a manual correction with an audit trail.

Never mark compensation successful simply because retries were exhausted.

A compensation action becomes successful only when its business effect is confirmed.

Microsoft's guidance specifically recommends durable compensation progress, idempotent compensation commands, and manual intervention when automated recovery cannot complete.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://learn.microsoft.com\&sz=32)

Microsoft Learn+1



## 10.1 Compensation status versus Saga status

Keep these separate.

For example:

| Compensation state                                  | Saga state                                          |
| --------------------------------------------------- | --------------------------------------------------- |
| Compensation actions executing                      | COMPENSATING                                        |
| Compensation retry pending                          | COMPENSATING                                        |
| Compensation outcome unknown                        | COMPENSATING or recovery-required operational state |
| Some compensation permanently requires intervention | MANUAL_INTERVENTION_REQUIRED                        |
| All required compensations confirmed                | COMPENSATED                                         |

A saga can enter MANUAL_INTERVENTION_REQUIRED even when some compensation actions have already succeeded.

That is not a contradiction.

It means automated recovery cannot currently establish the required final business state.

# 11. What exactly does compensation do to business data?

This deserves some practical examples.

Compensation must be designed inside each participant service.

The orchestrator sends a command. It does not modify the participant's database.

Let's inspect each operation.

### A. Order Service

Forward:

```
Create ORDER-123
```

Compensation:

```
Cancel ORDER-123
```

A possible order state machine:

```
PENDING
   |
   v
CONFIRMED
   |
   v
CANCELLATION_REQUESTED
   |
   v
CANCELLED
```

Why not delete the order?

Because the order is part of the audit history.

You need to know that it was created and subsequently cancelled.

Also, cancellation might be rejected if the order has already been handed to fulfillment.

### B. Inventory Service

Forward:

```
Reserve RES-987
```

Compensation:

```
Release RES-987
```

A possible reservation state machine:

```
RESERVED
    |
    v
RELEASED
```

Or:

```
RESERVED
    |
    v
CONSUMED
```

If the reservation has already been consumed or fulfilled, releasing it might no longer be valid.

The inventory service must check the reservation's actual state.

This is why the compensation should reference the reservation ID, not simply add a quantity back.

### C. Payment Service

Forward:

```
Charge PAY-100
```

Compensation:

```
Refund PAY-100
```

A possible payment lifecycle:

```
AUTHORIZED
    |
    v
CAPTURED
    |
    v
PARTIALLY_REFUNDED
    |
    v
REFUNDED
```

A real payment lifecycle has more states, but this illustrates an important distinction.

If the payment is only authorized, the correct compensation may be voiding or releasing the authorization.

If it has been captured, compensation may require a refund.

These are not interchangeable operations.

Also:

- A refund may not settle immediately.
- A partial refund may be required by business rules.
- Gateway settlement might already have occurred.
- A refund can itself fail.

The Payment Service must define what constitutes a confirmed refund and expose its status to the orchestrator.

### D. Shipping Service

Forward:

```
Create shipment.
```

Compensation depends on the real-world stage.

```
SHIPMENT_CREATED
        |
        v
CANCELLATION_REQUESTED
        |
        v
CANCELLED
```

But suppose:

```
SHIPMENT_CREATED
        |
        v
HANDED_TO_COURIER
        |
        v
IN_TRANSIT
```

Now cancellation might be impossible.

The right business operation may be:

```
REQUEST_RETURN
```

Or:

```
INITIATE_RECALL
```

These are new business processes, not literal reversal of the original shipment.

This brings us to a very important principle.

# 12. Compensation is not always possible — Understand irreversible operations

Some operations cannot be meaningfully undone.

Examples:

- Sending an email.
- Sending an SMS.
- Publishing a public notification.
- Physically shipping an item.
- Delivering a digital product.
- Performing an external action with a legally binding consequence.
- Transferring money into an external account where reversal is not guaranteed.

Suppose our workflow is:

```
1. CREATE_ORDER
2. RESERVE_INVENTORY
3. CHARGE_PAYMENT
4. SEND_ORDER_CONFIRMATION_EMAIL
```

Now imagine the email is successfully sent.

Then the final operation fails.

Can we unsend the email?

No.

We might send another email saying the order was cancelled.

But that's a corrective action, not a reversal.

We should therefore classify workflow operations.

| Operation type          | Example                 | Recovery strategy                     |
| ----------------------- | ----------------------- | ------------------------------------- |
| Reversible              | Reserve inventory       | Release reservation                   |
| Compensatable           | Capture payment         | Issue refund                          |
| Partially compensatable | Ship product            | Request cancellation or return        |
| Irreversible            | Send email              | Send correction or notification       |
| Externally irreversible | Completed bank transfer | Attempt formal reversal or escalation |

There are two important design approaches.

## Approach A: Delay irreversible actions until the end

Consider:

```
1. CREATE_ORDER
2. RESERVE_INVENTORY
3. CHARGE_PAYMENT
4. CREATE_SHIPMENT
5. SEND_CONFIRMATION_EMAIL
```

We place customer-facing notifications after the critical business operations succeed.

This reduces the number of cases where we need to correct an already-sent notification.

But be careful: "at the end" must mean the business process has reached the appropriate committed state, not just that the final HTTP call returned success.

For example, you might define:

```
SAGA = SUCCEEDED
        |
        v
Publish OrderConfirmed event
        |
        v
Send confirmation email
```

The notification itself is then a separate delivery workflow.

Use an outbox so that the success event cannot be lost between the Saga database commit and event publication.

The notification consumer should also use deduplication.

## Approach B: Define an explicit point of no return

Suppose a booking system has these stages:

```
1. Reserve flight
2. Reserve hotel
3. Charge payment
4. Issue ticket
5. Confirm booking
```

Ticket issuance might be an irreversible or costly-to-reverse business boundary.

We should define the point after which ordinary Saga compensation is no longer appropriate.

Before that point:

```
Failure -> Compensate
```

After that point:

```
Customer cancellation -> Separate cancellation/refund workflow
```

This distinction is very useful.

A customer canceling an already-completed order is not necessarily the same thing as compensating a partially completed checkout.

It is a new business command with its own policy, permissions, fees and lifecycle.

Microsoft recommends explicitly defining irreversible steps and points of no return, placing irreversible operations after important validations where possible.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://learn.microsoft.com\&sz=32)

Microsoft Learn+1



# 13. Forward recovery versus backward recovery

Let's connect compensation with the retry decisions from Chapter 5.

Suppose:

```
CREATE_ORDER = SUCCEEDED

RESERVE_INVENTORY = SUCCEEDED

CHARGE_PAYMENT = FAILED
```

Should we always compensate?

Not necessarily.

There are two major strategies.

### Forward recovery

We keep trying to complete the original business goal.

Examples:

- Payment gateway temporarily unavailable.
- Inventory service temporarily unavailable.
- Shipping provider temporarily unavailable.
- A booking provider can retry after a temporary reservation conflict.

```
Step failed temporarily
        |
        v
Wait / retry / reconcile
        |
        v
Continue forward
```

### Backward recovery

We abandon the original business goal and counteract the completed operations.

Examples:

- Payment definitively declined.
- Customer is no longer eligible.
- Required inventory cannot be obtained.
- Business validation permanently rejects the operation.

```
Permanent business failure
        |
        v
Stop forward progress
        |
        v
Compensate completed operations
        |
        v
Reach compensated outcome
```

AWS describes these two directions as forward recovery through continuation and backward recovery through compensating transactions.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

AWS Prescriptive Guidance+1



### There is also a third possibility: Alternative forward path

Consider hotel booking.

```
1. Reserve flight.
2. Reserve Hotel A.
3. Hotel A fails.
```

What should happen?

We could:

Option 1: Compensate flight reservation.

Option 2: Try Hotel B.

Option 3: Ask the customer whether they want another hotel.

Option 4: Keep the flight reservation pending while waiting for a decision, if the reservation rules allow it.

This is important because not every failed step should automatically initiate compensation.

The workflow definition should express business policies:

```
If HOTEL_A unavailable:
    Try HOTEL_B

If HOTEL_B unavailable:
    Request customer decision

If customer rejects alternatives:
    Compensate flight reservation
```

This is what makes a Saga a business workflow rather than simply a chain of HTTP calls with automatic rollback.

# 14. What if the compensation API returns 202 Accepted?

This is another real-world scenario worth understanding.

Suppose Payment Service receives:

```
POST /payments/PAY-100/refunds
```

It returns:

```
202 Accepted
```

```
{
  "refundId": "REF-100",
  "status": "PROCESSING"
}
```

Can the orchestrator mark:

```
REFUND_PAYMENT = SUCCEEDED
```

No.

What has happened?

The Payment Service has accepted the refund operation, but the refund itself hasn't necessarily completed.

We need to distinguish:

```
Compensation command accepted
        !=
Compensation business effect completed
```

A better representation is:

```
REFUND_PAYMENT = WAITING_FOR_RESULT
```

The orchestrator can:

1. Wait for a `RefundSucceeded` event.
2. Query the refund status.
3. Reconcile the refund if the event is delayed or lost.

Only after the refund is confirmed should the orchestration engine mark:

```
REFUND_PAYMENT = SUCCEEDED
```

Then it can activate the next dependent compensation.

The same principle applies to cancellations.

A shipping service accepting a cancellation request doesn't necessarily mean the courier has cancelled the shipment.

The business outcome must be confirmed according to the participant's API contract.

# 15. What should happen to forward actions that haven't started?

Consider:

```
CREATE_ORDER        SUCCEEDED
RESERVE_INVENTORY   FAILED
CHARGE_PAYMENT      BLOCKED
CREATE_SHIPMENT     BLOCKED
```

The saga enters COMPENSATING.

Should we create compensation actions for every step?

No.

We only compensate business effects that actually happened and require compensation.

| Forward state                   | Compensation required?                      |
| ------------------------------- | ------------------------------------------- |
| SUCCEEDED                       | Usually yes, if the step has a compensation |
| FAILED, effect confirmed absent | No                                          |
| BLOCKED                         | No                                          |
| READY, never executed           | No                                          |
| SKIPPED                         | No                                          |
| RUNNING                         | Resolve the outcome first                   |
| UNKNOWN                         | Reconcile before deciding                   |

In our example:

```
CREATE_ORDER = SUCCEEDED
RESERVE_INVENTORY = FAILED
```

We schedule:

```
CANCEL_ORDER = READY
```

There is no need to release inventory because reservation definitively failed.

There is no need to refund payment because payment never happened.

There is no need to cancel shipment because shipment was never created.

This seems obvious, but it prevents a very dangerous implementation mistake:

Never compensate an operation merely because it appears in the workflow definition. Compensate the business effects that were actually committed.

# 16. Putting the compensation lifecycle into the state machine

Let's define the overall saga lifecycle again.

```
RUNNING
   |
   | Definitive failure requiring rollback
   v
COMPENSATING
   |
   | All required compensation actions succeed
   v
COMPENSATED
```

If compensation cannot be completed automatically:

```
COMPENSATING
   |
   | Permanent unresolved issue
   v
MANUAL_INTERVENTION_REQUIRED
```

An operator resolves it and the saga resumes its compensation process.

When the original workflow succeeds:

```
RUNNING
   |
   | All required forward operations confirmed
   v
SUCCEEDED
```

A separate customer cancellation after success is generally a new workflow.

### Full lifecycle

\#chatgpt-mermaid-\_r_5sa\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_5sa\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5sa\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5sa\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_5sa\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5sa\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_5sa\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_5sa\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_5sa\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_5sa\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_5sa\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_5sa\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5sa\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5sa\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_5sa\_ p{margin:0;}#chatgpt-mermaid-\_r_5sa\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5sa\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5sa\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5sa\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_5sa\_ .label text,#chatgpt-mermaid-\_r_5sa\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5sa\_ .node rect,#chatgpt-mermaid-\_r_5sa\_ .node circle,#chatgpt-mermaid-\_r_5sa\_ .node ellipse,#chatgpt-mermaid-\_r_5sa\_ .node polygon,#chatgpt-mermaid-\_r_5sa\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_5sa\_ .rough-node .label text,#chatgpt-mermaid-\_r_5sa\_ .node .label text,#chatgpt-mermaid-\_r_5sa\_ .image-shape .label,#chatgpt-mermaid-\_r_5sa\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_5sa\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_5sa\_ .rough-node .label,#chatgpt-mermaid-\_r_5sa\_ .node .label,#chatgpt-mermaid-\_r_5sa\_ .image-shape .label,#chatgpt-mermaid-\_r_5sa\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_5sa\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_5sa\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5sa\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5sa\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_5sa\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_5sa\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5sa\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5sa\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5sa\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_5sa\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_5sa\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5sa\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5sa\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_5sa\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5sa\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_5sa\_ .icon-shape,#chatgpt-mermaid-\_r_5sa\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5sa\_ .icon-shape p,#chatgpt-mermaid-\_r_5sa\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_5sa\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_5sa\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5sa\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_5sa\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_5sa\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_5sa\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_5sa\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_5sa\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_5sa\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5sa\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_5sa\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_5sa\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_5sa\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5sa\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_5sa\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_5sa\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5sa\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_5sa\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_5sa\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5sa\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_5sa\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5sa\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5sa\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5sa\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_5sa\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_5sa\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_5sa\_ .node rect,#chatgpt-mermaid-\_r_5sa\_ .node circle,#chatgpt-mermaid-\_r_5sa\_ .node ellipse,#chatgpt-mermaid-\_r_5sa\_ .node polygon,#chatgpt-mermaid-\_r_5sa\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_5sa\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_5sa\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_5sa\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_5sa\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5sa\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}SAGA RUNNINGForward step outcomePersist result and schedulenext stepRetry / waitReconcileInitiate compensationFreeze new forwardexecutionResolve in-flight operationsBuild compensationdependency graphExecute compensationsAll required compensationscomplete?SAGA COMPENSATEDSchedule compensation retryMANUAL INTERVENTIONREQUIREDAll required forward stepscomplete?SAGA SUCCEEDEDConfirmed successTransient failureUnknown outcomeSucceededFailedPermanent business failureYesTransient failureUnresolved / permanent issueYes

# 17. Complete example — Let's follow every database transition

Let's do one end-to-end example.

The saga is:

```
SAGA-1001

1. CREATE_ORDER
2. RESERVE_INVENTORY
3. CHARGE_PAYMENT
4. CREATE_SHIPMENT
```

### Initial execution

```
CREATE_ORDER = SUCCEEDED
RESERVE_INVENTORY = SUCCEEDED
CHARGE_PAYMENT = SUCCEEDED
CREATE_SHIPMENT = READY
```

### Shipment creation fails permanently

Suppose the shipping provider returns:

```
{
  "code": "SERVICE_AREA_NOT_SUPPORTED"
}
```

The shipping operation is confirmed as unsuccessful.

The orchestrator transitions:

```
SAGA = COMPENSATING
```

It freezes additional forward execution.

It now creates compensation actions:

```
REFUND_PAYMENT       READY
RELEASE_INVENTORY    BLOCKED
CANCEL_ORDER         BLOCKED
```

### Compensation 1: Refund payment

Worker A claims:

```
REFUND_PAYMENT = RUNNING
```

It calls Payment Service using the stable compensation idempotency key:

```
SAGA-1001:CHARGE_PAYMENT:COMPENSATE
```

Payment Service returns:

```
{
  "refundId": "REF-100",
  "status": "SUCCESS"
}
```

The orchestrator persists:

```
REFUND_PAYMENT = SUCCEEDED
RELEASE_INVENTORY = READY
CANCEL_ORDER = BLOCKED
```

### Compensation 2: Release inventory

Worker B claims:

```
RELEASE_INVENTORY = RUNNING
```

It calls Inventory Service.

The inventory service successfully releases the reservation.

But the HTTP response is lost.

The orchestrator sees an unknown outcome.

It does not assume failure.

It retries using the same idempotency key:

```
SAGA-1001:RESERVE_INVENTORY:COMPENSATE
```

Inventory Service recognizes the previous release and returns the original success.

The orchestrator records:

```
RELEASE_INVENTORY = SUCCEEDED
CANCEL_ORDER = READY
```

### Compensation 3: Cancel order

Worker C claims:

```
CANCEL_ORDER = RUNNING
```

It calls Order Service.

Order Service changes:

```
ORDER-123: CONFIRMED -> CANCELLED
```

The orchestrator persists:

```
CANCEL_ORDER = SUCCEEDED
```

### Final state

```
SAGA-1001 = COMPENSATED
```

And the business records might look like:

```
Order:
    ORDER-123 = CANCELLED

Inventory:
    RES-987 = RELEASED

Payment:
    PAY-100 = REFUNDED

Shipment:
    Not created
```

Nothing was physically rolled back.

Each service executed its own local business operation.

Every operation has its own audit history.

The orchestrator simply coordinated the recovery.

# 18. How do I design a Saga so that compensation doesn't become a nightmare?

This is the practical checklist I would use when defining any new workflow.

For every forward step, ask these questions:

| Question                                               | Why it matters                               |
| ------------------------------------------------------ | -------------------------------------------- |
| What exact business effect does this operation create? | Identifies what must be counteracted         |
| What identifier does the participant return?           | Needed to target the original resource       |
| What is the compensation command?                      | Defines how to recover                       |
| Can the compensation partially succeed?                | Determines the state machine                 |
| Is the compensation idempotent?                        | Makes retries safe                           |
| Can the compensation be delayed?                       | Determines the acceptable consistency window |
| Does the compensation depend on other compensations?   | Defines execution ordering                   |
| Can this operation be irreversible?                    | Determines the point of no return            |
| What if the compensation is rejected?                  | Defines manual recovery                      |
| What if the result is unknown?                         | Defines reconciliation requirements          |

For example:

```
Step: CHARGE_PAYMENT

Forward:
    POST /payments

Forward idempotency key:
    SAGA-1001:CHARGE_PAYMENT:FORWARD

Required response data:
    paymentId

Compensation:
    POST /payments/{paymentId}/refunds

Compensation idempotency key:
    SAGA-1001:CHARGE_PAYMENT:COMPENSATE

Retry policy:
    6 attempts, exponential backoff

Unknown outcome:
    Query payment/refund status

Irreversible boundary:
    External settlement may require formal refund/reversal.
```

This kind of step definition makes the workflow understandable, testable and recoverable.

# 19. The mental model — What you should remember naturally

Let's reduce compensation to a few ideas.

Imagine you're organizing an event.

You:

1. Book a venue.
2. Order food.
3. Pay the catering company.
4. Arrange transportation.

Transportation cannot be arranged.

You don't magically travel back in time and undo the earlier actions.

You contact the respective providers:

- Cancel the catering order or request a refund.
- Release or cancel the venue booking.
- Cancel any other reservations that are no longer needed.

Some cancellations might succeed immediately.

Some might take time.

Some might incur cancellation charges.

Some might be impossible.

You must track each cancellation until its actual outcome is known.

That's exactly how Saga compensation works.

The orchestrator is the coordinator that remembers:

- What was successfully done?
- What needs to be counteracted?
- In which order?
- Which compensation has completed?
- Which compensation needs retrying?
- Which operation needs human intervention?

Compensation is simply another durable workflow, triggered by the failure of the original workflow.

And it uses the same worker, database, leases, fencing, idempotency and retry mechanisms.

## Chapter 6 — What we have achieved

We can now handle:

- Reverse execution for sequential workflows.
- Dependency-aware compensation for parallel workflows.
- Partial compensation.
- Compensation retries.
- Unknown compensation outcomes.
- Participant-side duplicate protection.
- In-flight forward operations during compensation.
- Irreversible business actions.
- Explicit points of no return.
- Manual recovery.
- Asynchronous cancellation and refund operations.

The five most important principles:

1. Compensation is a new business transaction, not a physical database rollback.
2. Compensate only confirmed business effects.
3. Follow business dependencies, not blindly decreasing step numbers.
4. Compensation must be as reliable and idempotent as forward execution.
5. Never declare a saga compensated until every required compensation is confirmed.

Useful references:

- [Microsoft — Compensating Transaction Pattern](https://learn.microsoft.com/azure/architecture/patterns/compensating-transaction?trk=article-ssr-frontend-pulse_little-text-block\&utm_source=chatgpt.com).
- [AWS — Saga Patterns](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/saga-patterns.html?utm_source=chatgpt.com).
- [AWS — Saga Orchestration Pattern](https://docs.aws.amazon.com/en_en/prescriptive-guidance/latest/cloud-design-patterns/saga-orchestration.html?utm_source=chatgpt.com).

# Next Chapter — Chapter 7: Concurrency, Scaling and High Availability

Now we have built a fairly complete execution engine.

But let's imagine the system is managing:

```
10 million saga instances.

50 million saga actions.

500 worker instances.

Multiple tenants.

Multiple participant services.

Thousands of actions becoming eligible every second.
```

Our design must scale without losing correctness.

We'll explore:

1. How to scale API instances and worker instances independently.
2. How to partition and distribute saga execution.
3. How PostgreSQL `SKIP LOCKED` behaves at high concurrency.
4. How to avoid database hotspots and excessive polling.
5. How to manage per-service and per-tenant concurrency.
6. How to handle millions of delayed retries efficiently.
7. Whether to partition tables and archive completed sagas.
8. When to introduce Kafka or RabbitMQ.
9. How to achieve high availability across multiple availability zones and regions.
10. How to handle database failover while workers are executing.
11. How to prevent a single noisy tenant from starving everyone else.
12. How to monitor throughput, backlog, latency and execution saturation.

We'll also address a particularly interesting question:

Should the entire Saga Orchestrator use one central database, or should we partition sagas across multiple databases?

This will take our design from a reliable single-cluster implementation to an architecture suitable for large-scale distributed systems.
