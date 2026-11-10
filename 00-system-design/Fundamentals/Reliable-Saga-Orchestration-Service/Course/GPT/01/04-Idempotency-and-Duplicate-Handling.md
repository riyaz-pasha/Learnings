# Chapter 4 — Idempotency and Duplicate Handling

This chapter is arguably the most important part of building a reliable Saga system.

So far, we've made the orchestrator reliable:

- Workers can crash and recover.
- Multiple workers can execute concurrently.
- Leases allow abandoned work to be reclaimed.
- Fencing prevents stale workers from modifying current workflow state.
- The database remembers what needs to happen next.

But there is still one problem we haven't solved.

What prevents a participant service from performing the same business operation twice?

The answer is idempotency.

We'll understand it from the participant service's perspective, because that's where duplicate business effects must ultimately be prevented.

## 1. The problem: Why can the same operation arrive multiple times?

Let's take a payment operation.

The orchestrator wants to charge a customer ₹2,500.

It sends:

```
POST /payments
Idempotency-Key: SAGA-1001-CHARGE_PAYMENT
Content-Type: application/json
```

```
{
  "customerId": "CUSTOMER-456",
  "orderId": "ORDER-123",
  "amount": 2500,
  "currency": "INR"
}
```

Payment Service executes:

```
1. Receive request.
2. Validate request.
3. Deduct ₹2,500 from customer balance.
4. Commit database transaction.
5. Return SUCCESS.
```

But something goes wrong.

```
Orchestrator                 Payment Service
     |                              |
     | -------- Request ----------> |
     |                              |
     |                       Deduct ₹2,500
     |                       COMMIT
     |                              |
     | <------- SUCCESS ------------|
     |
     X Network failure
```

The orchestrator never receives the response.

What should it do?

It cannot assume the payment failed.

It also cannot safely assume the payment succeeded.

It must retry or reconcile the operation.

So it sends the same logical request again.

```
Orchestrator                 Payment Service
     |                              |
     | -------- Request ----------> |
     |                              |
     |                       Deduct ₹2,500 again?
     |                              |
```

Without idempotency, the customer might be charged twice.

This problem is not unique to Sagas.

It happens whenever a client retries a request after an ambiguous network outcome.

## 2. What does idempotency actually mean?

An operation is idempotent when repeating the same logical operation produces the same intended effect as performing it once.

Mathematically:

\\[ f(f(x)) = f(x) \\]

For a payment command, we want:

```
First request:
    Charge ₹2,500
    Payment ID = PAY-100

Second identical request:
    Do not charge again
    Return Payment ID = PAY-100
```

The second request must return the original operation's result.

Notice the important distinction:

Idempotency does not mean that the request is executed only once. It means that repeated executions do not create additional business effects.

The HTTP request might arrive 2, 5, or 20 times.

The business effect should still happen only once for that logical operation.

This is what makes retries safe.

# 3. Where should idempotency be implemented?

An important architectural rule:

The service that owns the business data must own the idempotency mechanism for operations against that data.

For example:

| Operation         | Service responsible for idempotency |
| ----------------- | ----------------------------------- |
| Create order      | Order Service                       |
| Reserve inventory | Inventory Service                   |
| Charge payment    | Payment Service                     |
| Refund payment    | Payment Service                     |
| Create shipment   | Shipping Service                    |
| Cancel shipment   | Shipping Service                    |

Why?

Because only the participant knows whether its business operation has already been applied.

The orchestrator cannot reliably determine this from its own database.

For example:

```
Orchestrator DB:

CHARGE_PAYMENT = RUNNING
```

This doesn't tell us whether the customer has been charged.

The Payment Service database knows.

Therefore, the participant must recognize repeated logical operations and return their existing outcomes.

## 3.1 Two different idempotency boundaries

We now have two independent idempotency mechanisms.

```
Client
   |
   | Idempotency-Key: checkout-123
   v
Saga Orchestrator
   |
   | Idempotency-Key: SAGA-1001-CHARGE_PAYMENT
   v
Payment Service
```

| Boundary                   | What it protects                    |
| -------------------------- | ----------------------------------- |
| Client → Orchestrator      | Prevents duplicate saga creation    |
| Orchestrator → Participant | Prevents duplicate business effects |

These must not be confused.

Even if the orchestrator successfully prevents duplicate saga creation, a single saga may execute a participant operation multiple times due to retries and recovery.

# 4. Design the participant's idempotency database

Let's use Payment Service as our example.

We need a table that remembers every logical operation it has processed.

```
CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,

    tenant_id VARCHAR(100) NOT NULL,
    operation_name VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,

    request_hash VARCHAR(64) NOT NULL,

    status VARCHAR(30) NOT NULL,

    response_status INTEGER,
    response_body JSONB,

    resource_id VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,

    UNIQUE (
        tenant_id,
        operation_name,
        idempotency_key
    )
);
```

Let's understand every important field.

| Field             | Why we need it                                                     |
| ----------------- | ------------------------------------------------------------------ |
| `tenant_id`       | Separates idempotency scopes between tenants                       |
| `operation_name`  | Distinguishes operations such as CHARGE_PAYMENT and REFUND_PAYMENT |
| `idempotency_key` | Identifies one logical operation                                   |
| `request_hash`    | Detects accidental reuse of a key with different parameters        |
| `status`          | Identifies the processing outcome                                  |
| `response_status` | Remembers the original HTTP status                                 |
| `response_body`   | Allows the original response to be returned again                  |
| `resource_id`     | References the created business resource                           |

Why use all three columns in the unique constraint?

```
UNIQUE (
    tenant_id,
    operation_name,
    idempotency_key
)
```

Because idempotency keys should have a clearly defined scope.

For example:

```
Tenant A:
    CHARGE_PAYMENT + KEY-123

Tenant B:
    CHARGE_PAYMENT + KEY-123
```

These can be independent operations.

Similarly:

```
CHARGE_PAYMENT + KEY-123
REFUND_PAYMENT + KEY-123
```

These are different operation types.

You can also use a globally unique idempotency key if your API contract guarantees that. What matters is that the uniqueness scope is intentional and enforced by the database.

# 5. The request hash — Why is it necessary?

Consider this situation.

First request:

```
POST /payments
Idempotency-Key: KEY-123
```

```
{
  "orderId": "ORDER-123",
  "amount": 2500,
  "currency": "INR"
}
```

Payment Service successfully charges ₹2,500.

Now another request arrives:

```
POST /payments
Idempotency-Key: KEY-123
```

```
{
  "orderId": "ORDER-123",
  "amount": 5000,
  "currency": "INR"
}
```

The key is identical, but the amount has changed.

What should happen?

We must reject the request.

Returning the original success response without checking the payload could hide a client-side bug.

Executing it as a new payment would be even worse.

Therefore, we calculate a request hash.

Conceptually:

```
String requestHash = sha256(
    canonicalize(request)
);
```

For example:

```
Request 1:

SHA256({
  "orderId":"ORDER-123",
  "amount":2500,
  "currency":"INR"
})

= HASH-A
```

The database stores:

```
KEY-123
HASH-A
```

When the next request arrives:

```
KEY-123
HASH-B
```

We compare:

```
HASH-A != HASH-B
```

Return:

```
HTTP/1.1 409 Conflict
```

```
{
  "error": "IDEMPOTENCY_KEY_REUSED",
  "message": "This idempotency key was previously used with different request parameters."
}
```

There is another important insight here.

Do not use the request hash itself as the idempotency key.

Why?

Imagine a customer legitimately wants to purchase the same product twice.

Both requests may have exactly the same payload.

```
Purchase 1: Product A, Quantity 1
Purchase 2: Product A, Quantity 1
```

These are two different business intentions.

A caller-generated unique key distinguishes them.

The request hash is only there to verify that a previously used key is being reused with the same intent.

AWS discusses this distinction in its idempotent API design guidance.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://aws.amazon.com\&sz=32)

Amazon Web Services, Inc.+1



# 6. The most important implementation: Atomic idempotency + business operation

Now let's build the actual Payment Service.

Suppose the payment service owns a database:

```
PAYMENT DATABASE

idempotency_records
payments
accounts
ledger_entries
```

We need to guarantee that:

- The idempotency key gets recorded.
- The payment gets created.
- The customer balance gets updated.
- The response gets stored.

Either all these changes happen or none of them happen.

This is where a local ACID transaction becomes extremely useful.

## 6.1 The naive implementation — Don't do this

A developer might write:

```
public PaymentResponse charge(PaymentRequest request,
                              String key) {

    var existing = repository.findByKey(key);

    if (existing.isPresent()) {
        return existing.get().getResponse();
    }

    // Charge customer.
    Payment payment = paymentRepository.save(...);

    // Store idempotency key.
    idempotencyRepository.save(...);

    return response;
}
```

What's wrong?

There is a race condition.

Imagine two requests arrive simultaneously.

```
Request A                   Request B
    |                           |
    v                           v
Check key                  Check key
    |                           |
    v                           v
Not found                  Not found
    |                           |
    v                           v
Create payment             Create payment
    |                           |
    v                           v
Save key                   Save key
```

Both requests passed the check.

We might create two payments.

Checking first and inserting later is not enough.

We need the database to enforce uniqueness.

## 6.2 Correct implementation — Use a unique constraint and one transaction

Our transaction should follow this order:

```
BEGIN TRANSACTION

1. Attempt to register idempotency key.
2. If key already exists, retrieve existing outcome.
3. Verify request hash.
4. If new key:
       Perform business operation.
       Store original response.
       Mark idempotency record completed.

COMMIT TRANSACTION
```

The unique constraint is the critical concurrency guard.

Let's see how.

### Step 1: Start by inserting the idempotency record

```
INSERT INTO idempotency_records (
    id,
    tenant_id,
    operation_name,
    idempotency_key,
    request_hash,
    status,
    created_at
)
VALUES (
    :id,
    :tenant_id,
    'CHARGE_PAYMENT',
    :idempotency_key,
    :request_hash,
    'PROCESSING',
    clock_timestamp()
)
ON CONFLICT (
    tenant_id,
    operation_name,
    idempotency_key
)
DO NOTHING
RETURNING id;
```

There are two possible outcomes.

Case A: A row is returned.

We successfully registered a new logical operation.

We are the first transaction allowed to process this key.

Case B: No row is returned.

The key already exists.

Some other request has already processed or is processing this logical operation.

We need to retrieve its result.

### Step 2: If the key is new, perform the business operation

For example:

```
UPDATE accounts
SET balance = balance - 2500
WHERE customer_id = 'CUSTOMER-456'
  AND balance >= 2500;
```

Check the affected row count.

If one row was updated, continue.

If zero rows were updated, the customer may not exist or may have insufficient funds. Determine the exact reason according to your business rules.

Then create the payment record:

```
INSERT INTO payments (
    payment_id,
    order_id,
    customer_id,
    amount,
    status
)
VALUES (
    'PAY-100',
    'ORDER-123',
    'CUSTOMER-456',
    2500,
    'SUCCESS'
);
```

Also record the corresponding ledger entry.

### Step 3: Store the original response

```
UPDATE idempotency_records
SET
    status = 'SUCCEEDED',
    response_status = 200,
    response_body = '{
        "paymentId": "PAY-100",
        "status": "SUCCESS"
    }',
    resource_id = 'PAY-100',
    completed_at = clock_timestamp()
WHERE tenant_id = :tenant_id
  AND operation_name = 'CHARGE_PAYMENT'
  AND idempotency_key = :idempotency_key;
```

### Step 4: Commit everything

```
COMMIT;
```

Now the following changes are durable together:

```
Idempotency record created
Customer balance updated
Payment created
Ledger entry created
Original response stored
```

If anything fails before commit, the transaction rolls back.

The idempotency record also rolls back.

This is very important.

Suppose we stored the idempotency key in one transaction and then created the payment in another.

A crash between them could leave:

```
Idempotency key exists.

Payment does not exist.
```

Now a retry might incorrectly conclude that the payment was already processed.

By putting the idempotency record and business changes in one local transaction, we avoid that inconsistency.

AWS explicitly emphasizes this atomic relationship between recording the client token and performing associated mutations.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://aws.amazon.com\&sz=32)

Amazon Web Services, Inc.+1



# 7. What happens when two identical requests arrive simultaneously?

Let's understand the database concurrency behavior. This is where the design becomes particularly powerful.

Suppose two requests arrive:

```
Request A:
KEY-123, Amount = 2500

Request B:
KEY-123, Amount = 2500
```

Both try inserting:

```
UNIQUE (
    tenant_id,
    operation_name,
    idempotency_key
)
```

Remember: the first transaction has not committed yet.

What happens?

### Timeline

Payment DatabaseRequest BRequest APayment DatabaseRequest BRequest A#chatgpt-mermaid-\_r_4cp\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4cp\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4cp\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4cp\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4cp\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4cp\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4cp\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4cp\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4cp\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4cp\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4cp\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4cp\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4cp\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4cp\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4cp\_ p{margin:0;}#chatgpt-mermaid-\_r_4cp\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_4cp\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4cp\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4cp\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4cp\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4cp\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_4cp\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4cp\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4cp\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4cp\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_4cp\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4cp\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4cp\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4cp\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_4cp\_ .labelText,#chatgpt-mermaid-\_r_4cp\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4cp\_ .loopText,#chatgpt-mermaid-\_r_4cp\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4cp\_ .sectionTitle,#chatgpt-mermaid-\_r_4cp\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4cp\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_4cp\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4cp\_ .noteText,#chatgpt-mermaid-\_r_4cp\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_4cp\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4cp\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4cp\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4cp\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_4cp\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_4cp\_ .actor-man circle,#chatgpt-mermaid-\_r_4cp\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_4cp\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4cp\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4cp\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4cp\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4cp\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4cp\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4cp\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4cp\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4cp\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4cp\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4cp\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4cp\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4cp\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4cp\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4cp\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4cp\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4cp\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4cp\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4cp\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Waits on conflicting unique keyOnly one business effectBEGININSERT KEY-123Insert succeedsBEGININSERT KEY-123Debit customer + create paymentStore responseCOMMITON CONFLICT DO NOTHINGSELECT existing recordOriginal successful responseCOMMIT

Why does Request B wait?

Because PostgreSQL enforces the unique constraint.

When two transactions try to insert conflicting unique keys, the second transaction must wait for the first transaction's outcome.

- If A commits, B encounters the conflict.
- If A rolls back, B can insert the key and proceed.

PostgreSQL documents this behavior through its unique constraint and `INSERT ... ON CONFLICT` semantics.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.postgresql.org\&sz=32)

PostgreSQL+2



This is much stronger than checking an in-memory map or doing a Java-level existence check.

The database enforces the rule even when requests arrive simultaneously on different application instances.

### One subtle implementation detail

With PostgreSQL READ COMMITTED isolation:

After the conflicting `INSERT ... ON CONFLICT DO NOTHING` returns no row, execute a separate SELECT:

```
SELECT *
FROM idempotency_records
WHERE tenant_id = :tenant_id
  AND operation_name = 'CHARGE_PAYMENT'
  AND idempotency_key = :key;
```

This new statement obtains a fresh READ COMMITTED snapshot and can see the transaction that just committed.

If you're using a different isolation level, you may need to handle serialization failures or retry the transaction appropriately.

Also, configure sensible lock and statement timeouts. A duplicate request should not wait indefinitely if the original transaction is stuck.

# 8. What if the first request is still PROCESSING?

This is an important question.

Suppose Request A is taking 30 seconds.

Request B arrives with the same key after 2 seconds.

There are two different implementation strategies, and we need to distinguish them.

## Approach A: Short, synchronous participant operation

This is the design we just implemented.

```
Request A:

BEGIN TRANSACTION
    Insert idempotency record
    Perform business operation
    Store response
COMMIT
```

While this transaction is running, the idempotency record is not committed and therefore is not visible to other transactions.

Request B tries inserting the same key.

It waits on the unique constraint.

Then:

- If A commits, B detects the conflict and retrieves A's result.
- If A rolls back, B can become the transaction that performs the operation.

There is no separately committed PROCESSING record.

This is generally a good solution for short operations whose effects are entirely within one database.

However, it means the second HTTP request might wait.

We can use transaction and lock timeouts to bound that waiting.

## Approach B: Long-running participant operation

Now imagine Payment Service performs:

```
1. Validate payment.
2. Communicate with an external payment gateway.
3. Wait for gateway authorization.
4. Process the result.
5. Update local payment records.
```

This might take several seconds or longer.

We shouldn't keep a database transaction open throughout this process.

Instead, use a durable asynchronous operation.

The lifecycle becomes:

```
Request A
    |
    v
Transaction 1
    |
    | Insert idempotency record
    | status = PROCESSING
    | Create payment record = PENDING
    | Insert outbox event
    |
    v
COMMIT
    |
    v
Return 202 ACCEPTED
    |
    v
Background worker processes payment
    |
    v
Update payment + idempotency result
    |
    v
COMMIT
```

Now the idempotency record is visible as PROCESSING.

Request B arrives.

It finds:

```
KEY-123
status = PROCESSING
```

It does not start another payment.

It can return:

```
HTTP/1.1 202 Accepted
```

```
{
  "operationId": "PAY-100",
  "status": "PROCESSING",
  "statusUrl": "/payments/PAY-100"
}
```

The caller can query the operation status.

### Important distinction

| Synchronous operation                                   | Asynchronous operation                                 |
| ------------------------------------------------------- | ------------------------------------------------------ |
| Idempotency row and business effects committed together | Initial idempotency row committed before processing    |
| Duplicate requests wait for the original transaction    | Duplicate requests can observe PROCESSING              |
| Suitable for short local DB operations                  | Suitable for long-running operations                   |
| A crash rolls back the unfinished transaction           | A crash requires durable recovery and worker ownership |

Don't introduce separately committed PROCESSING states just because they look useful.

For short database operations, one atomic transaction is simpler and safer.

For long-running operations, use a durable state machine with the same reliability techniques we developed for the Saga Orchestrator.

# 9. How should the participant store and return the original response?

Let's say the first request successfully created a payment.

The original response was:

```
HTTP/1.1 200 OK
```

```
{
  "paymentId": "PAY-100",
  "orderId": "ORDER-123",
  "amount": 2500,
  "status": "SUCCESS"
}
```

The participant stores this result against:

```
KEY-123
```

Now Request B comes with the same key and same payload.

Instead of executing the business operation again:

```
public PaymentResponse charge(PaymentRequest request,
                              String idempotencyKey) {

    IdempotencyRecord record =
        idempotencyRepository.find(...);

    if (record != null) {

        validateRequestHash(record, request);

        if (record.isCompleted()) {
            return record.getResponse();
        }
    }

    // Execute only for a genuinely new operation.
}
```

Conceptually, this is what we want.

A real implementation must use the atomic insert/unique-constraint mechanism from the previous section rather than relying on this initial lookup alone.

The response should be semantically equivalent to the original result.

This matters because the caller should not need special handling for duplicate requests.

For example:

```
First request:

201 Created
paymentId = PAY-100

Second request:

201 Created
paymentId = PAY-100
```

The second request returns the existing payment identity.

It does not create another one.

You don't necessarily have to store every HTTP header. Store what is needed to reconstruct the relevant original response, such as:

- HTTP status.
- Response body.
- Resource ID.
- Relevant response headers, such as `Location`.

Avoid replaying request-specific or volatile headers such as trace IDs.

# 10. What about failures? Should failed results also be stored?

Yes, but we need to distinguish different types of failure.

Consider three scenarios.

### Scenario A: Permanent business failure

Payment Service receives:

```
{
  "customerId": "CUSTOMER-456",
  "amount": 2500
}
```

The customer has insufficient funds.

This is a definitive business outcome.

We might record:

```
KEY-123

status = REJECTED_FINAL
response_status = 422
response_body = {
    "code": "INSUFFICIENT_FUNDS"
}
```

Commit that result.

If the same request arrives again with the same key, return the same rejection.

Why?

Because this key represents the same logical payment command.

If the caller wants to initiate a genuinely new payment attempt after the customer adds funds, that should be a new logical command with a new key.

Whether a business rejection should be cached permanently or until a policy-defined expiry is a business/API contract decision. The important point is to make it deliberate.

### Scenario B: Transient technical failure before the business transaction commits

Suppose the database is temporarily unavailable.

Or an internal error occurs before the operation can be committed.

The entire transaction rolls back:

```
Idempotency record: ROLLBACK
Payment record:     ROLLBACK
Balance update:     ROLLBACK
```

When the caller retries, the key can be processed again.

We haven't committed a successful business effect.

### Scenario C: The service crashes during execution

Suppose:

```
BEGIN

Insert idempotency record
Update account balance
Create payment

CRASH before COMMIT
```

The database rolls back the unfinished transaction.

The retry can safely process the same key.

But suppose instead:

```
COMMIT successful

CRASH before HTTP response
```

The idempotency record and payment both exist.

The retry returns the original payment.

This is exactly the failure scenario we designed for.

### A useful classification

| Outcome                         | What happens to idempotency record?             |
| ------------------------------- | ----------------------------------------------- |
| Successful business operation   | Store successful result                         |
| Permanent business rejection    | Usually store rejected result                   |
| Transient failure before commit | Roll back, allowing safe retry                  |
| Process crash before commit     | Database rolls back                             |
| Process crash after commit      | Return original committed result                |
| Unknown external side effect    | Reconcile or retry using downstream idempotency |

Notice the final row. It needs special treatment.

# 11. The tricky real-world scenario: What if Payment Service calls an external payment gateway?

Our previous implementation assumed that Payment Service could complete its business operation entirely within its own database transaction.

But real payment systems often look like this:

```
Orchestrator
    |
    v
Payment Service
    |
    v
Payment Database
    |
    v
External Payment Gateway
    |
    v
Bank / Payment Network
```

Now we have two independent transactional systems:

1. Our Payment Service database.
2. The external payment gateway.

We cannot put both inside one PostgreSQL transaction.

Consider:

```
Payment Service:

1. Record idempotency key.
2. Call payment gateway.
3. Gateway charges customer.
4. Payment Service crashes before updating its database.
```

Our database might still say:

```
PAY-100 = PENDING
```

But the gateway might have successfully charged the customer.

This is the same ambiguity problem we encountered between the orchestrator and participants.

The solution is to extend idempotency to the next boundary.

## 11.1 Use a stable gateway idempotency key

Payment Service creates its own durable payment operation:

```
paymentId = PAY-100
```

Then calls the payment gateway using a stable key:

```
PAY-100-CAPTURE
```

If the payment service crashes and retries, it sends the same key.

```
First attempt:
    PAY-100-CAPTURE

Second attempt:
    PAY-100-CAPTURE

Third attempt:
    PAY-100-CAPTURE
```

If the gateway supports idempotency, it recognizes the same logical payment operation.

Payment Service can retrieve the original result without charging the customer again.

Every external side-effect boundary needs its own duplicate-protection strategy.

The participant's idempotency record alone cannot protect a separate payment gateway.

## 11.2 A reliable Payment Service architecture

\#chatgpt-mermaid-\_r_4ge\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4ge\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ge\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ge\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4ge\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ge\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4ge\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4ge\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4ge\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4ge\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4ge\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4ge\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ge\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ge\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4ge\_ p{margin:0;}#chatgpt-mermaid-\_r_4ge\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ge\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ge\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ge\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4ge\_ .label text,#chatgpt-mermaid-\_r_4ge\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ge\_ .node rect,#chatgpt-mermaid-\_r_4ge\_ .node circle,#chatgpt-mermaid-\_r_4ge\_ .node ellipse,#chatgpt-mermaid-\_r_4ge\_ .node polygon,#chatgpt-mermaid-\_r_4ge\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4ge\_ .rough-node .label text,#chatgpt-mermaid-\_r_4ge\_ .node .label text,#chatgpt-mermaid-\_r_4ge\_ .image-shape .label,#chatgpt-mermaid-\_r_4ge\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4ge\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4ge\_ .rough-node .label,#chatgpt-mermaid-\_r_4ge\_ .node .label,#chatgpt-mermaid-\_r_4ge\_ .image-shape .label,#chatgpt-mermaid-\_r_4ge\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4ge\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4ge\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ge\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ge\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4ge\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4ge\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ge\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ge\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ge\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4ge\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4ge\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ge\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ge\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4ge\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ge\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4ge\_ .icon-shape,#chatgpt-mermaid-\_r_4ge\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ge\_ .icon-shape p,#chatgpt-mermaid-\_r_4ge\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4ge\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4ge\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ge\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4ge\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4ge\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4ge\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4ge\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4ge\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4ge\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ge\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4ge\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4ge\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4ge\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ge\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4ge\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4ge\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ge\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4ge\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4ge\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ge\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4ge\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ge\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ge\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ge\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4ge\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4ge\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4ge\_ .node rect,#chatgpt-mermaid-\_r_4ge\_ .node circle,#chatgpt-mermaid-\_r_4ge\_ .node ellipse,#chatgpt-mermaid-\_r_4ge\_ .node polygon,#chatgpt-mermaid-\_r_4ge\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4ge\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4ge\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4ge\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4ge\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ge\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Orchestrator requestPayment APITransaction 1Idempotency recordPayment PENDINGOutbox eventCOMMITReturn 202 AcceptedPayment WorkerPayment GatewayStable gateway idempotencykeyGateway result?Persist payment SUCCESSPersist payment REJECTEDReconcile or retry safelyNotify OrchestratorSuccessDeclinedUnknown / Timeout

This architecture uses the Transactional Outbox Pattern to ensure that creating the payment operation and scheduling its execution cannot become inconsistent.

The payment worker then performs the external gateway operation with a stable gateway-level idempotency key.

If a gateway doesn't support idempotency, you need another reliable reconciliation mechanism, such as querying by a unique merchant reference. You cannot safely assume that retrying an ambiguous charge is harmless.

# 12. How should the orchestrator generate idempotency keys?

Now let's return to the orchestrator.

We want it to generate stable keys for participant operations.

For example:

```
SAGA-1001

Step 1: CREATE_ORDER
Step 2: RESERVE_INVENTORY
Step 3: CHARGE_PAYMENT
```

We can generate:

```
SAGA-1001:CREATE_ORDER:FORWARD

SAGA-1001:RESERVE_INVENTORY:FORWARD

SAGA-1001:CHARGE_PAYMENT:FORWARD
```

For compensation:

```
SAGA-1001:CHARGE_PAYMENT:COMPENSATE

SAGA-1001:RESERVE_INVENTORY:COMPENSATE

SAGA-1001:CREATE_ORDER:COMPENSATE
```

These keys identify logical actions, not individual execution attempts.

### The most important rule: Never change the key when retrying the same operation.

Consider:

```
Worker A:

SAGA-1001:CHARGE_PAYMENT:FORWARD

Attempt 1
```

Worker A crashes.

Worker B takes over:

```
SAGA-1001:CHARGE_PAYMENT:FORWARD

Attempt 2
```

Worker B must use the exact same key.

Even Worker C, months later during recovery, must use the same key if it is retrying that same logical action.

Do not include:

- Worker ID.
- Attempt number.
- Timestamp.
- Retry count.
- Lease generation.

Those values change between attempts.

If you incorporate them into the idempotency key, you will accidentally create a new logical operation for every retry.

### Why compensation needs a different key

Consider:

```
Forward operation:

CHARGE_PAYMENT
    Idempotency-Key = SAGA-1001:CHARGE_PAYMENT:FORWARD
```

Later, the saga fails.

We need:

```
REFUND_PAYMENT
    Idempotency-Key = SAGA-1001:CHARGE_PAYMENT:COMPENSATE
```

These are two distinct business operations.

The first charges the customer.

The second refunds the customer.

Each must be independently idempotent.

Also, multiple attempts to execute the refund must reuse the compensation key.

For a production system, a UUID generated and persisted at action creation is another good approach. The important thing is that the action identity is durable and unchanged throughout its retries.

### What about different payloads?

The orchestrator should not reuse a key for a changed command.

For example:

```
First command:
CHARGE_PAYMENT
Amount = 2500
Key = SAGA-1001:CHARGE_PAYMENT:FORWARD
```

Changing the amount to 3000 while reusing that key should produce a conflict.

A legitimate new payment command requires a new action identity and key.

# 13. What should happen when the participant receives a duplicate?

Let's combine the complete request-handling algorithm.

Suppose Payment Service receives:

```
POST /payments
Idempotency-Key: SAGA-1001:CHARGE_PAYMENT:FORWARD
```

The service follows this decision process:

\#chatgpt-mermaid-\_r_4hq\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4hq\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4hq\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4hq\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4hq\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hq\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4hq\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4hq\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4hq\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4hq\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4hq\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4hq\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4hq\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4hq\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4hq\_ p{margin:0;}#chatgpt-mermaid-\_r_4hq\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hq\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hq\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hq\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4hq\_ .label text,#chatgpt-mermaid-\_r_4hq\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hq\_ .node rect,#chatgpt-mermaid-\_r_4hq\_ .node circle,#chatgpt-mermaid-\_r_4hq\_ .node ellipse,#chatgpt-mermaid-\_r_4hq\_ .node polygon,#chatgpt-mermaid-\_r_4hq\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4hq\_ .rough-node .label text,#chatgpt-mermaid-\_r_4hq\_ .node .label text,#chatgpt-mermaid-\_r_4hq\_ .image-shape .label,#chatgpt-mermaid-\_r_4hq\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4hq\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4hq\_ .rough-node .label,#chatgpt-mermaid-\_r_4hq\_ .node .label,#chatgpt-mermaid-\_r_4hq\_ .image-shape .label,#chatgpt-mermaid-\_r_4hq\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4hq\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4hq\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4hq\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4hq\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4hq\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4hq\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4hq\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4hq\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4hq\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4hq\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4hq\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hq\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hq\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4hq\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hq\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4hq\_ .icon-shape,#chatgpt-mermaid-\_r_4hq\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4hq\_ .icon-shape p,#chatgpt-mermaid-\_r_4hq\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4hq\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4hq\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4hq\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4hq\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4hq\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4hq\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4hq\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4hq\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4hq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4hq\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4hq\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4hq\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4hq\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4hq\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4hq\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4hq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4hq\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4hq\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4hq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4hq\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4hq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4hq\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4hq\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4hq\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4hq\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4hq\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4hq\_ .node rect,#chatgpt-mermaid-\_r_4hq\_ .node circle,#chatgpt-mermaid-\_r_4hq\_ .node ellipse,#chatgpt-mermaid-\_r_4hq\_ .node polygon,#chatgpt-mermaid-\_r_4hq\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4hq\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4hq\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4hq\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4hq\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4hq\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Receive requestValidate basic requestCalculate canonical requesthashAttempt atomic idempotencyregistrationNew key inserted?Execute business transactionBusiness outcome?Persist business effect +responsePersist rejection resultRollback transactionCOMMITFetch existing idempotencyrecordRequest hash matches?409 ConflictExisting status?Return original successReturn original rejectionReturn 202 / operation statusYesSuccessPermanent rejectionTransient technical failureNoNoYesSUCCEEDEDREJECTED_FINALPROCESSING

There is one implementation nuance here:

The `PROCESSING` branch applies to an asynchronously committed operation.

For a short synchronous operation, the idempotency row is initially uncommitted and a duplicate normally waits on the unique constraint rather than observing PROCESSING.

That distinction prevents us from mixing two different transaction models.

# 14. What does the Java implementation look like?

Let's put the concepts into a simplified Spring Boot-style design.

I would separate the API logic, idempotency handling and business operation.

### Controller

```
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> charge(
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody PaymentRequest request) {

        PaymentResponse response =
                paymentService.charge(request, key);

        return ResponseEntity.ok(response);
    }
}
```

### Service

Conceptually:

```
@Service
public class PaymentService {

    @Transactional
    public PaymentResponse charge(
            PaymentRequest request,
            String idempotencyKey) {

        String requestHash = calculateHash(request);

        // 1. Try to register the logical operation.
        boolean isNew = registerIdempotencyKey(
            idempotencyKey,
            requestHash
        );

        // 2. If existing, retrieve original outcome.
        if (!isNew) {

            IdempotencyRecord record =
                    findExistingRecord(idempotencyKey);

            validateHash(record, requestHash);

            return record.getStoredResponse();
        }

        // 3. Execute actual business operation.
        Payment payment = executePayment(request);

        // 4. Build original response.
        PaymentResponse response =
                buildResponse(payment);

        // 5. Persist response against idempotency key.
        completeIdempotencyRecord(
            idempotencyKey,
            response
        );

        // 6. Return.
        // Spring commits the transaction after successful return.
        return response;
    }
}
```

This is conceptual code, not a complete implementation.

In particular:

- `registerIdempotencyKey()` must use the database unique constraint and `INSERT ... ON CONFLICT DO NOTHING RETURNING`.
- `findExistingRecord()` must retrieve the committed original record.
- The business operation and idempotency completion must participate in the same transaction.
- Business rejections that should be replayed must be returned as committed outcomes rather than thrown as exceptions that roll back the transaction.
- Unexpected technical failures should roll back the entire operation.

Also, if you implement registration and lookup using repository methods, ensure they use the same transaction and the intended PostgreSQL isolation level.

The most important part is not the Java code.

It is the transactional guarantee:

```
IDEMPOTENCY RECORD
       +
BUSINESS EFFECT
       +
STORED RESPONSE
       |
       v
ONE DATABASE TRANSACTION
```

# 15. How long should we retain idempotency records?

This is a surprisingly important production consideration.

Imagine:

```
Day 1:

Payment succeeds.

Idempotency key = KEY-123
```

We store the record.

```
Day 2:

Orchestrator experiences recovery issues.
```

It retries the same payment.

The Payment Service recognizes the key.

No second payment happens.

Now imagine we deleted the idempotency record after 24 hours.

```
Day 3:

Orchestrator retries KEY-123.

Payment Service:
    Key not found.

Creates another payment.
```

This could be disastrous.

Therefore, retention must be based on the maximum possible retry and recovery horizon.

Consider:

- Maximum Saga execution duration.
- Maximum retry period.
- Manual recovery duration.
- Delayed or duplicated messages.
- Disaster recovery and data restoration.
- External participant reconciliation.

For financial operations, I would generally preserve a durable operation identifier or unique business operation reference much longer than a short-lived HTTP response cache.

You can eventually archive response details, but don't remove the only mechanism that prevents an old logical operation from being executed again.

A useful distinction:

```
Response cache:
    Can potentially expire.

Business operation identity:
    Must remain protected against duplicate effects.
```

Also, the retention policy should be part of the API contract. The caller must know how long it can safely retry with the same key.

# 16. Real-world scenarios — Applying idempotency correctly

Let's see how this design applies to different business operations.

| Operation         | Business effect                      | Idempotency protection                                   |
| ----------------- | ------------------------------------ | -------------------------------------------------------- |
| Create Order      | Create order record                  | Unique operation key                                     |
| Reserve Inventory | Reserve stock quantity               | Unique reservation reference                             |
| Charge Payment    | Deduct/authorize money               | Payment operation ID + gateway key                       |
| Refund Payment    | Credit money back                    | Separate refund operation ID                             |
| Create Shipment   | Create shipment                      | Unique shipment command reference                        |
| Send Notification | Send email/SMS                       | Provider idempotency or deduplication                    |
| Transfer Money    | Debit one account and credit another | One unique transfer operation with atomic ledger updates |

Let's examine some less obvious cases.

### A. Inventory reservation

Suppose:

```
Product ID = PRODUCT-100
Quantity = 2

Idempotency-Key = SAGA-1001:RESERVE_INVENTORY:FORWARD
```

Inventory Service executes:

```
BEGIN;

-- Register idempotency key.

UPDATE inventory
SET available_quantity = available_quantity - 2
WHERE product_id = 'PRODUCT-100'
  AND available_quantity >= 2;

-- Verify affected rows.

INSERT INTO reservations (
    reservation_id,
    saga_operation_id,
    product_id,
    quantity,
    status
)
VALUES (
    'RES-100',
    'SAGA-1001:RESERVE_INVENTORY:FORWARD',
    'PRODUCT-100',
    2,
    'RESERVED'
);

-- Save original response.

COMMIT;
```

A retry returns `RES-100`.

It must not reserve two additional units.

The unique operation reference on the reservation table provides an additional domain-level safeguard.

### B. Inventory compensation

Later, the saga fails.

The orchestrator calls:

```
POST /inventory/reservations/RES-100/release
Idempotency-Key: SAGA-1001:RESERVE_INVENTORY:COMPENSATE
```

Inventory Service releases the reservation.

If the request is repeated, it must not increment available inventory twice.

The reservation itself should also have a valid state transition:

```
RESERVED -> RELEASED
```

An already released reservation should return the same logical result without modifying stock again.

This illustrates a useful principle:

Idempotency keys protect logical requests, while business state transitions protect domain invariants.

Both are valuable.

### C. Money transfer

Suppose:

```
Account A -> Account B
Amount = ₹5,000
```

You must not blindly perform:

```
Debit A
Credit B
```

twice.

A safe implementation uses:

- One unique transfer operation ID.
- One database transaction for both ledger entries, if they share a database.
- Unique constraints on the transfer and ledger operation references.
- A stored response.
- Reconciliation if an external banking system participates.

Never assume that making the HTTP endpoint idempotent alone guarantees that all individual financial mutations are atomic.

# 17. What if the participant's PROCESSING operation gets stuck?

Remember our asynchronous Payment Service design?

It commits:

```
PAY-100

status = PROCESSING
```

Then a background worker starts talking to the payment gateway.

Now imagine:

```
Payment Worker A starts processing.

Gateway receives the request.

Payment Worker A crashes.
```

The payment record remains PROCESSING.

We cannot leave it there forever.

Do you recognize this problem?

It's almost identical to the orchestrator's worker recovery problem from Chapter 3.

We can reuse the same mechanisms:

```
PAY-100

status = PROCESSING
worker_id = payment-worker-A
lease_until = ...
claim_generation = 1
```

After lease expiry:

```
payment-worker-B claims PAY-100

claim_generation = 2
```

It retries the gateway operation using the same gateway idempotency key.

If the gateway already processed the payment, it returns the original outcome.

The payment service then persists the result.

This gives us an important general principle:

Idempotency is not just a feature of the Saga Orchestrator. Every service that performs recoverable side effects may need its own durable idempotent execution mechanism.

# 18. What idempotency does NOT guarantee

I want to clarify a subtle but important point.

You might hear people say:

"We have exactly-once execution."

This is misleading.

We cannot generally guarantee that a distributed request physically executes exactly once.

For example:

```
Worker A sends request.
Worker A times out.

Worker B sends the same request.

Both requests reach the participant.
```

The participant may receive the request twice.

What we can guarantee, within a properly implemented transactional boundary, is:

```
Multiple execution attempts
          |
          v
One logical operation
          |
          v
One committed business effect
```

This is often described as effectively-once business effects through idempotency.

However, there are boundaries.

Suppose Payment Service:

1. Updates its database.
2. Sends an email.
3. Crashes before recording that the email was sent.

The email may be sent twice.

The Payment Service's database idempotency record does not magically protect the email provider.

Each side effect must be handled separately, using an idempotent provider API, a deduplicating consumer, or another appropriate recovery mechanism.

Likewise, if an external provider offers neither idempotency nor reliable status lookup, there may be no way to safely resolve an ambiguous outcome automatically. That is a genuine system limitation, not something the orchestrator can solve with another retry.

# 19. Testing idempotency — The scenarios you must test

Before calling a participant service production-ready, test these situations.

| Test                                     | Expected result                |
| ---------------------------------------- | ------------------------------ |
| Same request, same key, sequentially     | Original result returned       |
| Same request, same key, concurrently     | One business effect            |
| Same key, different payload              | HTTP 409                       |
| First request fails before commit        | Retry can execute              |
| First request commits, response is lost  | Retry returns original result  |
| Application crashes during transaction   | Transaction rolls back         |
| Application crashes after commit         | Retry returns stored outcome   |
| Worker retries after lease expiry        | Same logical effect            |
| Compensation request is duplicated       | Compensation happens only once |
| Old duplicate arrives after a long delay | No duplicate business effect   |

For a payment service, I would especially test this:

```
Send 100 concurrent requests.

All requests have:
    Same tenant
    Same operation
    Same idempotency key
    Same amount

Expected:

    1 payment record
    1 debit
    1 ledger operation
    All successful callers receive the same payment identity
```

Then repeat with a different amount but the same key.

Expected:

```
409 Conflict
```

No additional debit.

These tests expose concurrency bugs that ordinary unit tests often miss.

# 20. Let's connect everything to our Saga Orchestrator

We now have two independently reliable systems.

System A: Saga Orchestrator

Owns:

- Saga state.
- Step state.
- Worker execution.
- Retries.
- Leases.
- Compensation decisions.
- Participant invocation.

System B: Participant Service

Owns:

- Its business database.
- Idempotency records.
- Business operations.
- Original operation results.
- Its own downstream integrations.
- Participant-side recovery.

The complete execution now looks like this:

Payment GatewayPayment DBPayment ServiceOrchestrator DBOrchestrator WorkerPayment GatewayPayment DBPayment ServiceOrchestrator DBOrchestrator Worker#chatgpt-mermaid-\_r_4l4\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4l4\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4l4\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4l4\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4l4\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4l4\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4l4\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4l4\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4l4\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4l4\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4l4\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4l4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4l4\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4l4\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4l4\_ p{margin:0;}#chatgpt-mermaid-\_r_4l4\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_4l4\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4l4\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4l4\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4l4\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4l4\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_4l4\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4l4\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4l4\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4l4\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_4l4\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4l4\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4l4\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4l4\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_4l4\_ .labelText,#chatgpt-mermaid-\_r_4l4\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4l4\_ .loopText,#chatgpt-mermaid-\_r_4l4\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4l4\_ .sectionTitle,#chatgpt-mermaid-\_r_4l4\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4l4\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_4l4\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4l4\_ .noteText,#chatgpt-mermaid-\_r_4l4\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_4l4\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4l4\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4l4\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4l4\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_4l4\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_4l4\_ .actor-man circle,#chatgpt-mermaid-\_r_4l4\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_4l4\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4l4\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4l4\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4l4\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4l4\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4l4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4l4\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4l4\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4l4\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4l4\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4l4\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4l4\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4l4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4l4\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4l4\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4l4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4l4\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4l4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4l4\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Payment Service processes asynchronouslyEvent might be delivered more than onceClaim CHARGE_PAYMENTClaim token + stable keyCharge payment + idempotency keyAtomic idempotency registrationCreate payment + outboxCommit202 Accepted, PAY-100Charge with stable gateway keyPayment successfulPersist payment SUCCESSCommitPaymentSucceeded eventVerify event ID and current stateRecord result and schedule next step

Notice something interesting.

There are multiple idempotency boundaries:

```
Client
  |
  | Client idempotency key
  v
Orchestrator
  |
  | Participant operation idempotency key
  v
Payment Service
  |
  | Gateway idempotency key
  v
External Payment Gateway
```

Each boundary has its own operation identity and protection.

An idempotency key from one boundary should not automatically be assumed to protect another boundary.

The services cooperate to provide reliable business execution.

# 21. The mental model — Understand this without memorizing implementation details

Imagine a customer asks you to perform an action.

You give the action a unique identity:

```
ACTION-123
```

Before performing the action, you establish whether that identity has already been processed.

If it's new:

```
Perform the operation.
Remember the outcome.
```

If it's already completed:

```
Don't perform it again.
Return the previous outcome.
```

If it's still executing:

```
Don't start another execution.
Return or resolve its current state.
```

If the previous execution failed before committing:

```
Allow another attempt.
```

That is the entire intuition behind idempotency.

The database uniqueness constraint prevents concurrent duplicate registrations.

The local ACID transaction connects the idempotency record to the business effect.

The stored result makes retries predictable.

The stable key makes recovery possible across different workers and different machines.

## Chapter 4 — What we have achieved

We can now handle:

- Duplicate client requests.
- Duplicate worker requests.
- Concurrent requests with the same key.
- Lost HTTP responses.
- Worker crashes after participant success.
- Participant transaction failures.
- Duplicate compensation requests.
- Nested external service calls.
- Asynchronous participant operations.
- Long-running operation recovery.

Three principles are particularly important:

1. Idempotency key identifies a logical operation, not an attempt.
2. The idempotency record and local business mutation must be atomic.
3. Every independently executed side effect needs appropriate duplicate protection.

For further reference, the [AWS Builders' Library article on making retries safe with idempotent APIs](https://aws.amazon.com/builders-library/making-retries-safe-with-idempotent-APIs/?did=ba_card\&utm_source=chatgpt.com) is particularly useful. It explains why clients should explicitly communicate their intent through unique request identifiers rather than relying on payload similarity.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://aws.amazon.com\&sz=32)

Amazon Web Services, Inc.+1



# Next Chapter — Chapter 5: Retries, Timeouts, Failures and Unknown Outcomes

Now let's tackle another major part of the design.

Imagine Payment Service takes 10 seconds to respond.

The orchestrator has a 5-second HTTP timeout.

What should happen?

Should it retry immediately?

What if the payment is still processing?

What if the participant returns HTTP 500 after actually committing the payment?

What if retries keep failing for 24 hours?

We'll build a complete failure classification and retry strategy.

Specifically:

1. Difference between business failures and technical failures.
2. Retryable vs non-retryable errors.
3. The difference between an HTTP timeout and a confirmed operation failure.
4. Exponential backoff and jitter.
5. Retry budgets and maximum attempts.
6. Circuit breakers and rate limiting.
7. How to handle ambiguous participant outcomes.
8. Reconciliation APIs and operation-status polling.
9. What happens when a retry budget is exhausted.
10. Dead-letter queues and manual intervention.

The key question we'll answer is:

How does the orchestrator decide whether to retry, compensate, wait, or ask for human intervention without accidentally charging a customer twice or losing a business operation?

