# Chapter 5 — Retries, Timeouts, Failures and Unknown Outcomes

This chapter brings together almost everything we've learned so far.

We have:

- A durable Saga Orchestrator.
- A database-backed worker engine.
- Atomic work claiming.
- Leases and fencing tokens.
- Idempotent participant services.
- Retryable forward and compensation actions.

But one major question remains:

When something goes wrong, how does the orchestrator decide what to do next?

Should it retry?

Should it compensate?

Should it wait?

Should it ask the participant what happened?

Should it stop and request human intervention?

We need a systematic way to make these decisions.

Let's build it from the beginning.

## 1. First, understand that not all failures are equal

Imagine our orchestrator executes:

```
1. CREATE_ORDER        SUCCEEDED
2. RESERVE_INVENTORY   SUCCEEDED
3. CHARGE_PAYMENT      RUNNING
4. CREATE_SHIPMENT     BLOCKED
```

Now Payment Service returns an error.

What should the orchestrator do?

It depends on what actually happened.

Consider these scenarios.

Scenario A: Insufficient funds

Payment Service returns:

```
422 Unprocessable Entity
```

```
{
  "code": "INSUFFICIENT_FUNDS"
}
```

This is a definitive business rejection.

Retrying the same payment with the same customer balance probably won't help.

We should eventually compensate the order and inventory reservation, according to the business policy.

Scenario B: Payment Service is temporarily unavailable

Payment Service returns:

```
503 Service Unavailable
Retry-After: 30
```

This may be a temporary technical failure.

We should wait and retry, provided the operation's idempotency contract makes the retry safe.

Scenario C: Network timeout

```
Orchestrator sends payment request.

Payment Service receives it.

Payment Service charges customer.

Payment Service commits database.

Orchestrator times out before receiving the response.
```

This is fundamentally different.

The payment might have succeeded.

We cannot conclude that it failed.

Scenario D: Invalid request

Payment Service returns:

```
400 Bad Request
```

There is probably a bug in our payload, schema, or workflow configuration.

Retrying the same request will not fix it.

We need to stop automatic retries and investigate or compensate, depending on whether any business effect occurred.

This tells us something important:

Failure handling should be based on the meaning and certainty of the outcome, not merely on whether an HTTP request returned an error.

## 2. Build a failure classification system

I would start with four broad categories.

| Category                    | What we know                                                | Typical action                                          |
| --------------------------- | ----------------------------------------------------------- | ------------------------------------------------------- |
| Confirmed success           | Business operation completed                                | Persist success and continue                            |
| Confirmed permanent failure | Operation definitively did not succeed or was rejected      | Fail the step and consider compensation                 |
| Transient failure           | Operation can reasonably succeed later                      | Schedule retry                                          |
| Unknown outcome             | We cannot determine whether the business operation happened | Reconcile or safely retry with the same idempotency key |

Let's understand each one.

### Category 1: Confirmed success

Example:

```
200 OK
```

```
{
  "paymentId": "PAY-100",
  "status": "SUCCESS"
}
```

The participant has explicitly confirmed success.

The orchestrator should:

1. Validate the response.
2. Persist the response.
3. Mark the action SUCCEEDED.
4. Make dependent steps eligible.
5. Complete the database transaction.

No retry is necessary.

### Category 2: Confirmed permanent failure

Examples:

```
INSUFFICIENT_FUNDS
INVALID_CUSTOMER
INVALID_PAYMENT_DETAILS
BUSINESS_RULE_VIOLATION
```

These errors indicate that executing the same operation again with unchanged input will not help.

The orchestrator should:

1. Record the failure.
2. Mark the action FAILED.
3. Evaluate the workflow's failure policy.
4. Initiate compensation if required.

Not every permanent failure requires compensation.

For example, if the first operation was rejected and no business effect occurred, the saga might simply become FAILED.

### Category 3: Transient failure

Examples:

```
HTTP 429
HTTP 503
Temporary connection refusal
Temporary database unavailability
Temporary service overload
```

These failures may disappear over time.

The orchestrator should:

1. Record the failed attempt.
2. Calculate the next retry time.
3. Mark the action RETRY_WAIT.
4. Release its lease.
5. Let another worker execute it when eligible.

It should not block a worker by sleeping for 30 seconds.

### Category 4: Unknown outcome

This is the most important category.

Examples:

```
HTTP timeout after sending request
Connection reset while waiting for response
Worker crash after participant commit
Gateway timeout
Ambiguous HTTP 500 from a participant
```

The orchestrator doesn't know whether the participant performed the operation.

The correct response is:

Don't confuse uncertainty with failure.

We need to establish the actual outcome before making a business decision.

# 3. Create an explicit failure decision tree

Instead of writing random `if/else` blocks in every worker, create one centralized failure classifier.

Conceptually:

\#chatgpt-mermaid-\_r_4p0\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4p0\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4p0\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4p0\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4p0\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4p0\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4p0\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4p0\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4p0\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4p0\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4p0\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4p0\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4p0\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4p0\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4p0\_ p{margin:0;}#chatgpt-mermaid-\_r_4p0\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4p0\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4p0\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4p0\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4p0\_ .label text,#chatgpt-mermaid-\_r_4p0\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4p0\_ .node rect,#chatgpt-mermaid-\_r_4p0\_ .node circle,#chatgpt-mermaid-\_r_4p0\_ .node ellipse,#chatgpt-mermaid-\_r_4p0\_ .node polygon,#chatgpt-mermaid-\_r_4p0\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4p0\_ .rough-node .label text,#chatgpt-mermaid-\_r_4p0\_ .node .label text,#chatgpt-mermaid-\_r_4p0\_ .image-shape .label,#chatgpt-mermaid-\_r_4p0\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4p0\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4p0\_ .rough-node .label,#chatgpt-mermaid-\_r_4p0\_ .node .label,#chatgpt-mermaid-\_r_4p0\_ .image-shape .label,#chatgpt-mermaid-\_r_4p0\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4p0\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4p0\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4p0\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4p0\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4p0\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4p0\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4p0\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4p0\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4p0\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4p0\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4p0\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4p0\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4p0\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4p0\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4p0\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4p0\_ .icon-shape,#chatgpt-mermaid-\_r_4p0\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4p0\_ .icon-shape p,#chatgpt-mermaid-\_r_4p0\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4p0\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4p0\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4p0\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4p0\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4p0\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4p0\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4p0\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4p0\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4p0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4p0\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4p0\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4p0\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4p0\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4p0\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4p0\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4p0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4p0\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4p0\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4p0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4p0\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4p0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4p0\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4p0\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4p0\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4p0\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4p0\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4p0\_ .node rect,#chatgpt-mermaid-\_r_4p0\_ .node circle,#chatgpt-mermaid-\_r_4p0\_ .node ellipse,#chatgpt-mermaid-\_r_4p0\_ .node polygon,#chatgpt-mermaid-\_r_4p0\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4p0\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4p0\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4p0\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4p0\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4p0\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Participant operation finishesor times outWas success confirmed?Mark SUCCEEDEDWas permanent rejectionconfirmed?Mark FAILEDIs the error transient andretry-safe?Schedule RETRY_WAITOutcome UNKNOWNReconcile participantoperationActual outcome?MANUAL_INTERVENTION_REQUIREDYesNoYesNoYesNo / uncertainSucceededDefinitely not executedPermanently rejectedCannot establish

Notice the `reconcile` step.

We will discuss exactly how it works shortly.

For now, understand that it prevents us from compensating incorrectly.

## 3.1 HTTP status codes are clues, not the complete failure policy

A useful initial classification:

| HTTP response / error                      | Typical interpretation             | Possible action                                 |
| ------------------------------------------ | ---------------------------------- | ----------------------------------------------- |
| 200 / 201                                  | Success                            | Complete action                                 |
| 202                                        | Accepted, still processing         | Track operation status                          |
| 400                                        | Invalid request                    | Permanent failure                               |
| 401 / 403                                  | Authentication/authorization issue | Stop and investigate                            |
| 404                                        | Resource or endpoint not found     | Depends on API semantics                        |
| 409                                        | Conflict or duplicate              | Interpret response contract                     |
| 422                                        | Business rejection                 | Usually permanent                               |
| 429                                        | Rate limited                       | Retry after instructed delay                    |
| 500                                        | Internal error                     | Potentially ambiguous                           |
| 502 / 504                                  | Gateway error / timeout            | Potentially ambiguous                           |
| 503                                        | Service unavailable                | Usually retryable, but verify operation outcome |
| Connection refused before request delivery | Usually transient                  | Retry                                           |
| Timeout after request transmission         | Unknown outcome                    | Reconcile or idempotently retry                 |

Why am I deliberately saying typical and potentially?

Because HTTP status codes don't tell us everything about business execution.

For example:

Imagine the participant performs:

```
1. Commit payment.
2. Serialize response.
3. Return HTTP 500 due to a response-processing error.
```

The client sees HTTP 500.

But the payment succeeded.

Similarly, an API gateway can return 504 even though its downstream service completed the operation.

Therefore, the participant API should expose meaningful application-level error codes and a well-defined operation contract.

Microsoft's transient-fault guidance similarly recommends basing retries on the nature of the failure, rather than blindly retrying every HTTP error.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://learn.microsoft.com\&sz=32)

Microsoft Learn+1



# 4. Understand timeout before understanding retry

This is one of the biggest sources of mistakes in distributed systems.

Let's say we configure:

```
HTTP timeout = 5 seconds
```

What does this actually mean?

It means:

> The orchestrator will stop waiting for this request after its configured deadline.

It does NOT mean:

> The participant will stop executing after five seconds.

These are completely different things.

## 4.1 The timeout scenario

Consider:

```
Time 0s:

Worker sends request.

Time 1s:

Payment Service receives request.

Time 2s:

Payment Service starts processing.

Time 4s:

Payment Service commits payment.

Time 5s:

Orchestrator HTTP timeout occurs.

Time 6s:

Payment Service sends response.
```

From the orchestrator's perspective:

```
REQUEST TIMED OUT
```

From the participant's perspective:

```
PAYMENT SUCCESSFUL
```

Both are true.

This is not a contradiction.

They are describing different sides of a distributed operation.

A timeout controls how long the caller waits. It does not provide a guarantee about whether the remote business operation happened.

## 4.2 Different types of timeouts

In a production HTTP client, there are several timeout categories.

| Timeout                             | Meaning                                           |
| ----------------------------------- | ------------------------------------------------- |
| Connection-pool acquisition timeout | How long to wait for an available HTTP connection |
| DNS timeout                         | How long name resolution may take                 |
| Connect timeout                     | How long establishing a connection may take       |
| TLS handshake timeout               | How long secure connection negotiation may take   |
| Response/read timeout               | How long to wait for response data                |
| Overall request deadline            | Maximum total time allowed for the request        |

You don't necessarily need to configure every one independently in every HTTP client, but you should understand what the client library is actually enforcing.

For example:

```
Connection-pool acquisition: 500 ms
Connect timeout:              1 second
Response timeout:             5 seconds
Overall deadline:             7 seconds
```

These are illustrative values, not universal production defaults.

### Why do we need an overall deadline?

Imagine:

```
Connection acquisition = 5 seconds
Connect timeout        = 5 seconds
Response timeout       = 10 seconds
```

The total request can take significantly longer than expected if each phase has its own independent allowance.

The overall deadline provides a bound for the complete operation.

Google's SRE guidance explains why deadlines matter and why they should be propagated through nested service calls. Without bounded deadlines, requests can keep consuming resources long after their callers have given up.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://sre.google\&sz=32)

Cascading Failures: Reducing System Outage



## 4.3 What should the orchestrator do when a timeout occurs?

Suppose:

```
CHARGE_PAYMENT

Attempt 1
Started: 10:00:00
Timed out: 10:00:05
```

Don't do this:

```
catch (TimeoutException ex) {
    markFailed();
    compensatePreviousSteps();
}
```

This is dangerous.

Instead:

```
catch (TimeoutException ex) {
    markOutcomeUnknown();
    scheduleReconciliation();
}
```

We need to determine whether the operation happened.

The options are:

1. Query the participant's operation-status API.
2. Retry using the same idempotency key.
3. Wait for an asynchronously delivered completion event.
4. Escalate for manual reconciliation if none of these can establish the outcome.

We'll explore these in Section 10.

# 5. How should retries actually work?

Let's assume the participant has confirmed that an operation can be safely retried.

For example:

```
Inventory Service temporarily unavailable.
No reservation has been completed.
```

We want another attempt.

A naive implementation:

```
while (true) {
    try {
        callParticipant();
        break;
    } catch (Exception ex) {
        Thread.sleep(1000);
    }
}
```

Why is this bad?

- The worker thread remains occupied.
- The database lease may expire.
- The application may keep hammering an unhealthy service.
- Thousands of workers may retry simultaneously.
- The workflow may never terminate.

Instead, retries should be durable scheduled work.

Remember our state machine?

```
READY
  |
  v
RUNNING
  |
  v
RETRY_WAIT
  |
  v
READY
```

The retry is simply another future execution of the same logical action.

## 5.1 Persist the retry information

Suppose an inventory request fails with HTTP 503.

We record:

```
saga_id              = SAGA-1001
action               = RESERVE_INVENTORY
status               = RETRY_WAIT
attempt_count        = 1
next_attempt_at      = 10:00:15
last_error_code      = SERVICE_UNAVAILABLE
```

The worker releases its lease.

It can immediately claim another task.

When the retry time arrives, the scheduler makes this action eligible for another claim.

The workflow database contains everything needed to resume it, even if the original worker disappears.

### Example database update

```
BEGIN;

UPDATE saga_actions
SET
    status = 'RETRY_WAIT',
    next_attempt_at = :retry_time,

    lease_owner = NULL,
    lease_until = NULL,

    updated_at = clock_timestamp()

WHERE id = :action_id
  AND status = 'RUNNING'
  AND lease_owner = :worker_id
  AND claim_generation = :claim_generation
  AND lease_until > clock_timestamp();

-- Verify exactly one row was updated.

UPDATE saga_step_attempts
SET
    outcome = 'RETRYABLE_FAILURE',
    error_code = :error_code,
    error_message = :error_message,
    completed_at = clock_timestamp()
WHERE id = :attempt_id;

COMMIT;
```

All changes happen in one database transaction.

A stale worker cannot schedule a retry after losing ownership.

Also, the retry time must not be based on a worker's local clock. Persist it using the database clock or another carefully synchronized time source.

# 6. Exponential backoff — Why retries should become slower

Suppose Payment Service is temporarily overloaded.

We have 1,000 workers.

All receive:

```
503 Service Unavailable
```

Imagine every worker retries after exactly one second.

```
10:00:00
1000 requests fail.

10:00:01
1000 retries arrive.

10:00:02
1000 retries arrive again.
```

What happens?

The participant might still be recovering from the original overload.

The retries create more load, worsening the problem.

This can create a cascading failure.

We need to progressively increase the time between attempts.

This is exponential backoff.

## 6.1 Understand the formula

Let:

- \\(B\\) = initial backoff.
- \\(n\\) = retry number, starting from 0.
- \\(C\\) = maximum backoff.

Then:

\\[ D_n = \min(C, B \times 2^n) \\]

Suppose:

```
Initial delay = 2 seconds
Maximum delay = 60 seconds
```

Then:

| Retry | Calculation  | Delay  |
| ----- | ------------ | ------ |
| 1     | 2 × 2⁰       | 2 sec  |
| 2     | 2 × 2¹       | 4 sec  |
| 3     | 2 × 2²       | 8 sec  |
| 4     | 2 × 2³       | 16 sec |
| 5     | 2 × 2⁴       | 32 sec |
| 6     | min(60, 64)  | 60 sec |
| 7     | min(60, 128) | 60 sec |

Conceptually:

```
Attempt 1 -> Wait 2 seconds
Attempt 2 -> Wait 4 seconds
Attempt 3 -> Wait 8 seconds
Attempt 4 -> Wait 16 seconds
Attempt 5 -> Wait 32 seconds
Attempt 6 -> Wait 60 seconds
```

Why is this better?

As a dependency becomes unhealthy, we gradually reduce how frequently we contact it.

But we have another problem.

All workers might still calculate exactly the same delays.

That's where jitter comes in.

# 7. Jitter — Preventing thousands of workers from retrying together

Imagine 10,000 workers receive a failure at almost the same moment.

With plain exponential backoff:

```
All retry after 2 seconds.

All retry after 4 seconds.

All retry after 8 seconds.
```

This creates synchronized retry bursts.

We want to spread the retries across a time interval.

One commonly used strategy is full jitter.

Formula:

\\[ D_n = Random(0,\min(C,B \times 2^n)) \\]

Using our previous configuration:

```
Initial delay = 2 seconds
Maximum delay = 60 seconds
```

Possible retry delays:

| Retry | Maximum calculated delay | Actual random delay |
| ----- | ------------------------ | ------------------- |
| 1     | 2 sec                    | 1.3 sec             |
| 2     | 4 sec                    | 3.7 sec             |
| 3     | 8 sec                    | 2.1 sec             |
| 4     | 16 sec                   | 12.5 sec            |
| 5     | 32 sec                   | 22 sec              |
| 6     | 60 sec                   | 44 sec              |

The actual values vary.

This spreads retry traffic instead of producing synchronized bursts.

Google SRE and Microsoft's transient-fault guidance both emphasize exponential backoff with randomization as a way to avoid retry amplification and cascading failures.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://sre.google\&sz=32)

Cascading Failures: Reducing System Outage+2



## 7.1 Java implementation

```
public Duration calculateBackoff(
        int attempt,
        Duration baseDelay,
        Duration maxDelay) {

    int exponent = Math.min(attempt - 1, 30);

    long baseMillis = baseDelay.toMillis();
    long maxMillis = maxDelay.toMillis();

    long exponential;

    try {
        exponential = Math.multiplyExact(
            baseMillis,
            1L << exponent
        );
    } catch (ArithmeticException ex) {
        exponential = maxMillis;
    }

    long upperBound = Math.min(
        exponential,
        maxMillis
    );

    long delay = ThreadLocalRandom.current()
            .nextLong(upperBound + 1);

    return Duration.ofMillis(delay);
}
```

For a production implementation, also validate the configuration values and guard the `upperBound + 1` calculation against overflow.

The important idea is the formula, not memorizing the code.

# 8. What if the participant tells us when to retry?

Consider this response:

```
HTTP/1.1 429 Too Many Requests
Retry-After: 30
```

The participant is explicitly asking us to wait at least 30 seconds.

We should respect that instruction.

Our retry policy should combine:

- Our own backoff calculation.
- The participant's `Retry-After` instruction.
- Our maximum permitted retry delay.
- The overall workflow deadline.

For a delta-seconds `Retry-After` value, the calculation might be:

\\[ D = \max(D\_{\text{local}},D\_{\text{server}}) \\]

For an HTTP-date value, calculate the remaining time until that date.

If the requested delay exceeds the maximum time we're willing to keep the action scheduled, we should defer it according to our policy or escalate it. We should not silently retry earlier than the participant requested.

Also, don't synchronize thousands of clients at precisely the same retry instant. Where appropriate, add controlled positive jitter after the server's minimum requested delay.

## 8.1 Example

Suppose:

```
Our exponential backoff = 8 seconds

Retry-After = 30 seconds
```

We wait at least 30 seconds.

If our policy adds up to 5 seconds of positive jitter:

```
Actual delay = 30–35 seconds
```

The participant gets time to recover.

# 9. Maximum retries — Should we retry forever?

Absolutely not.

Imagine a payment operation has been retrying for 24 hours.

```
Attempt 1: Timeout
Attempt 2: HTTP 503
Attempt 3: Timeout
Attempt 4: HTTP 503
Attempt 5: Timeout
...
Attempt 100: Timeout
```

At some point, automatic retrying is no longer useful.

We need explicit retry limits.

But here's something important:

A retry policy should have more than just a maximum attempt count.

Consider these limits:

| Limit                   | Why it exists                                                        |
| ----------------------- | -------------------------------------------------------------------- |
| Maximum attempts        | Prevents excessive retries for one action                            |
| Maximum retry age       | Prevents an operation from remaining in automated retry indefinitely |
| Overall saga deadline   | Limits the business workflow duration                                |
| Per-attempt timeout     | Bounds individual network operations                                 |
| Dependency retry budget | Controls aggregate retry traffic                                     |
| Business deadline       | Prevents executing operations after they are no longer useful        |

For example:

```
Payment operation policy:

Maximum attempts: 8
Maximum automatic retry age: 30 minutes
Maximum per-attempt deadline: 8 seconds

Overall checkout deadline: 45 minutes
```

These are example values. Payment, inventory and shipping might have very different policies.

A saga can be long-running even when individual steps have relatively short execution deadlines.

For example:

```
Hotel reservation:

1. Reserve room.
2. Wait for payment confirmation.
3. Wait for external identity verification.
4. Finalize booking.
```

A workflow might legitimately remain active for hours or days.

Therefore, don't blindly apply a five-minute retry policy to every workflow.

## 9.1 What happens when the limit is reached?

Suppose:

```
CHARGE_PAYMENT

Attempt 1: Timeout
Attempt 2: Timeout
Attempt 3: HTTP 503
Attempt 4: Timeout

Maximum automatic attempts = 4
```

We transition the action to something like:

```
RECONCILIATION_REQUIRED
```

Or, if the outcome is already known to be a permanent failure:

```
FAILED
```

These must not be confused.

A useful set of terminal and operational statuses is:

```
SUCCEEDED
FAILED
RECONCILIATION_REQUIRED
MANUAL_INTERVENTION_REQUIRED
```

When automated attempts are exhausted, the action must remain visible and recoverable.

It must not disappear.

# 10. Retry versus reconciliation — This is the critical distinction

Let's return to the timeout scenario.

```
Worker sends CHARGE_PAYMENT.

Payment Service processes payment.

Payment Service commits.

Network drops response.

Worker times out.
```

We have an unknown outcome.

What should the orchestrator do?

There are three useful approaches.

## Approach A: Query participant operation status

This is often the clearest approach.

Design your participant API to expose an operation-status endpoint.

For example:

```
GET /payments/operations/SAGA-1001-CHARGE_PAYMENT
```

Possible responses:

Payment succeeded:

```
200 OK
```

```
{
  "operationId": "SAGA-1001-CHARGE_PAYMENT",
  "status": "SUCCEEDED",
  "paymentId": "PAY-100"
}
```

Payment is still processing:

```
200 OK
```

```
{
  "operationId": "SAGA-1001-CHARGE_PAYMENT",
  "status": "PROCESSING"
}
```

Payment was definitively rejected:

```
200 OK
```

```
{
  "operationId": "SAGA-1001-CHARGE_PAYMENT",
  "status": "REJECTED",
  "reason": "INSUFFICIENT_FUNDS"
}
```

The orchestrator can now make an informed decision.

Notice that the HTTP response to the status query is `200` because the status lookup itself succeeded. The business operation's outcome is represented separately in the response body.

I strongly recommend designing operation-status APIs for important participant operations, especially financial operations.

## Approach B: Retry the original request with the same idempotency key

Suppose your Payment Service has a correctly implemented idempotency mechanism.

You can send:

```
POST /payments
Idempotency-Key: SAGA-1001-CHARGE_PAYMENT
```

Again.

If the original payment succeeded, Payment Service returns the stored result.

If it never happened, Payment Service executes it.

If it is currently processing asynchronously, the participant returns its current operation state.

This makes the retry safe.

For many APIs, retrying the original request is simpler than introducing a separate reconciliation endpoint.

However, status lookup is useful when you want to resolve an ambiguous result without initiating another attempt.

## Approach C: Receive asynchronous completion events

Instead of constantly polling Payment Service:

```
Payment Service
    |
    | PaymentSucceeded
    v
Message Broker
    |
    v
Saga Orchestrator
```

Payment Service emits an event:

```
{
  "eventId": "EVT-1001",
  "operationId": "SAGA-1001-CHARGE_PAYMENT",
  "paymentId": "PAY-100",
  "status": "SUCCEEDED"
}
```

The orchestrator consumes it and updates its database.

But event delivery introduces its own reliability problems:

- The event might arrive more than once.
- The event might arrive before the original HTTP response.
- The event might be delayed.
- The event might arrive after the orchestrator has already reconciled the result.
- Event publishing might fail.

Therefore:

1. Use an outbox in the participant service.
2. Give every event a stable unique ID.
3. Deduplicate incoming events.
4. Validate the associated action and logical operation ID.
5. Apply the resulting state transition transactionally.
6. Never allow a late event to overwrite a newer, contradictory state without reconciliation.

You can also combine events with periodic status reconciliation.

This is often a good design for long-running participants.

## 10.1 Which approach should we choose?

| Approach               | Best suited for                                                                                 |
| ---------------------- | ----------------------------------------------------------------------------------------------- |
| Status API             | Financial operations and important business actions                                             |
| Retry original request | Participants with strong idempotency support                                                    |
| Completion events      | Asynchronous and long-running participant operations                                            |
| Combination            | High-value workflows where eventual notification and independent reconciliation are both needed |

For an Order Checkout system, I would generally support a status endpoint and idempotent request replay for payment and inventory operations. Events can be added to improve notification latency.

# 11. Circuit breakers — What happens when a participant is continuously failing?

We've learned exponential backoff.

But imagine Payment Service is completely down.

We have 10,000 pending payment operations.

Each operation has its own retry schedule.

Even with backoff, we may continue sending requests to an unavailable participant.

We need to protect that dependency.

This is where the Circuit Breaker pattern comes in.

Think of an electrical circuit breaker.

If the electrical system detects a fault, it disconnects the circuit rather than allowing the fault to continue causing damage.

A software circuit breaker behaves similarly.

It generally has three states.

### State 1: CLOSED

Everything is operating normally.

Requests are allowed.

```
Orchestrator
     |
     v
Circuit Breaker: CLOSED
     |
     v
Payment Service
```

The circuit breaker monitors failures.

### State 2: OPEN

Suppose the number or percentage of recent failures crosses a configured threshold.

The circuit opens.

```
Orchestrator
     |
     v
Circuit Breaker: OPEN
     |
     X
Payment Service
```

New calls are rejected locally without contacting the participant.

What happens to the saga actions?

They should remain durably scheduled.

For example:

```
CHARGE_PAYMENT = RETRY_WAIT
next_attempt_at = ...
```

The worker should not endlessly try the same unavailable dependency.

It can release the action and work on another participant or another saga.

Importantly, an open circuit is not a business failure.

Do not compensate an order simply because the circuit breaker prevented a payment call.

### State 3: HALF_OPEN

After some recovery period, the circuit breaker permits a limited number of test requests.

```
Orchestrator
     |
     v
Circuit Breaker: HALF_OPEN
     |
     v
Limited test requests
     |
     v
Payment Service
```

If the requests succeed:

```
HALF_OPEN -> CLOSED
```

If they fail:

```
HALF_OPEN -> OPEN
```

This helps determine whether the participant has recovered.

\#chatgpt-mermaid-\_r_501\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_501\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_501\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_501\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_501\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_501\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_501\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_501\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_501\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_501\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_501\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_501\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_501\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_501\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_501\_ p{margin:0;}#chatgpt-mermaid-\_r_501\_ defs [id$="-barbEnd"]{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_501\_ g.stateGroup text{fill:rgb(31, 78, 148);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_501\_ g.stateGroup text{fill:rgb(237, 237, 237);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_501\_ g.stateGroup .state-title{font-weight:bolder;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_501\_ g.stateGroup rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_501\_ g.stateGroup line{stroke:rgb(175, 175, 175);stroke-width:1;}#chatgpt-mermaid-\_r_501\_ .transition{stroke:rgb(175, 175, 175);stroke-width:1;fill:none;}#chatgpt-mermaid-\_r_501\_ .stateGroup .composit{fill:transparent;border-bottom:1px;}#chatgpt-mermaid-\_r_501\_ .stateGroup .alt-composit{fill:#e0e0e0;border-bottom:1px;}#chatgpt-mermaid-\_r_501\_ .state-note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_501\_ .state-note text{fill:rgb(237, 237, 237);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_501\_ .stateLabel .box{stroke:none;stroke-width:0;fill:rgb(9, 23, 44);opacity:0.5;}#chatgpt-mermaid-\_r_501\_ .edgeLabel .label rect{fill:rgb(9, 23, 44);opacity:0.5;}#chatgpt-mermaid-\_r_501\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_501\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_501\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_501\_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_501\_ .label div .edgeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_501\_ .stateLabel text{fill:rgb(237, 237, 237);font-size:10px;font-weight:bold;}#chatgpt-mermaid-\_r_501\_ .node circle.state-start{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_501\_ .node .fork-join{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_501\_ .node circle.state-end{fill:rgb(31, 78, 148);stroke:transparent;stroke-width:1.5;}#chatgpt-mermaid-\_r_501\_ .end-state-inner{fill:transparent;stroke-width:1.5;}#chatgpt-mermaid-\_r_501\_ .node rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_501\_ .node polygon{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_501\_ [id$="-barbEnd"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_501\_ .statediagram-cluster rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_501\_ .cluster-label,#chatgpt-mermaid-\_r_501\_ .nodeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_501\_ .statediagram-cluster rect.outer{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_501\_ .statediagram-state .divider{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_501\_ .statediagram-state .title-state{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_501\_ .statediagram-cluster.statediagram-cluster .inner{fill:transparent;}#chatgpt-mermaid-\_r_501\_ .statediagram-cluster.statediagram-cluster-alt .inner{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_501\_ .statediagram-cluster .inner{rx:0;ry:0;}#chatgpt-mermaid-\_r_501\_ .statediagram-state rect.basic{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_501\_ .statediagram-state rect.divider{stroke-dasharray:10,10;fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_501\_ .note-edge{stroke-dasharray:5;}#chatgpt-mermaid-\_r_501\_ .statediagram-note rect{fill:rgb(48, 48, 48);stroke:rgb(58, 132, 63);stroke-width:1px;rx:0;ry:0;}#chatgpt-mermaid-\_r_501\_ .statediagram-note rect{fill:rgb(48, 48, 48);stroke:rgb(58, 132, 63);stroke-width:1px;rx:0;ry:0;}#chatgpt-mermaid-\_r_501\_ .statediagram-note text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_501\_ .statediagram-note .nodeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_501\_ .statediagram .edgeLabel{color:red;}#chatgpt-mermaid-\_r_501\_ [id$="-dependencyStart"],#chatgpt-mermaid-\_r_501\_ [id$="-dependencyEnd"]{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);stroke-width:1;}#chatgpt-mermaid-\_r_501\_ .statediagramTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_501\_ [data-look="neo"].statediagram-cluster rect{fill:rgb(9, 23, 44);stroke:url(#chatgpt-mermaid-\_r_501\_-gradient);stroke-width:1;}#chatgpt-mermaid-\_r_501\_ [data-look="neo"].statediagram-cluster rect.outer{rx:5px;ry:5px;filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_501\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_501\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_501\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_501\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_501\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_501\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_501\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_501\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_501\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_501\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_501\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_501\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_501\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_501\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_501\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_501\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_501\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_501\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}CLOSEDOPENHALF_OPENFailure threshold reachedRecovery wait elapsedTest requests succeedTest requests fail

Microsoft's Circuit Breaker guidance describes this same pattern: stop repeatedly accessing a failing dependency, allow it time to recover, and probe it cautiously before fully restoring traffic.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://learn.microsoft.com\&sz=32)

Microsoft Learn+1



### Important: Where should circuit breakers live?

Usually in the orchestrator's participant-client layer, potentially with per-dependency or per-endpoint isolation.

For example:

```
Order Service:
    Circuit Breaker A

Inventory Service:
    Circuit Breaker B

Payment Service:
    Circuit Breaker C

Shipping Service:
    Circuit Breaker D
```

You don't want a failing Shipping Service to unnecessarily prevent the orchestrator from executing Payment Service operations.

In a multi-instance deployment, each instance may maintain its own circuit-breaker state. If you need global coordination, you can introduce distributed controls, but that adds complexity. A practical starting point is local breakers combined with distributed rate limits or downstream capacity controls.

# 12. Avoid retry storms and duplicated retry layers

This is an important production issue.

Imagine this architecture:

```
Saga Orchestrator
      |
      v
Order Service
      |
      v
Inventory Service
      |
      v
Database
```

Suppose each layer implements three retries.

Then the total number of attempts can multiply.

For example:

```
Orchestrator:
    3 attempts

Order Service:
    3 attempts per orchestrator attempt

Inventory Service:
    3 attempts per Order Service attempt
```

Worst-case attempts against the database:

\\[ 3 \times 3 \times 3 = 27 \\]

A single logical operation can generate 27 attempts against the deepest dependency.

This is called retry amplification.

Microsoft explicitly warns about duplicated retry layers because their combined retry counts can multiply and overload downstream systems.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://learn.microsoft.com\&sz=32)

Microsoft Learn+1



## 12.1 Who should own retries?

I recommend this responsibility split:

| Layer                              | Responsibility                                                            |
| ---------------------------------- | ------------------------------------------------------------------------- |
| Saga Orchestrator                  | Business-operation retry policy and workflow scheduling                   |
| HTTP client                        | Limited transport-level behavior, with no conflicting hidden retry policy |
| Participant                        | Idempotency and participant-side recovery                                 |
| Participant's external integration | Its own idempotency and recovery for downstream side effects              |

The orchestrator should own the business-level retry schedule because it owns the Saga workflow.

The HTTP client should not independently perform a large number of hidden retries.

For example, if the HTTP client silently retries three times, and the orchestrator also retries the action three times, you may unexpectedly make nine requests.

Make all retry behavior visible and intentional.

# 13. Retry budgets — Limiting the total retry traffic

Per-action retry limits aren't enough.

Consider:

```
10,000 active sagas.

Each saga:
    Maximum 5 retries.

Total possible retries:
    50,000
```

Even though every individual saga respects its limit, the participant might be overwhelmed by the aggregate retry volume.

This is where retry budgets help.

A retry budget limits the total retry traffic sent to a dependency within some time period.

For example:

```
Payment Service:

Maximum retry requests = 100 per minute.
```

Suppose the orchestrator already used 100 retries during the current minute.

A new retry becomes due.

Instead of sending it immediately, the orchestrator defers it or marks it for controlled recovery.

The operation remains durable.

We are not losing the business transaction.

We are controlling how much additional traffic we send to an unhealthy dependency.

### Why is this different from a circuit breaker?

| Mechanism           | What it controls                                     |
| ------------------- | ---------------------------------------------------- |
| Maximum attempts    | Retries of one action                                |
| Exponential backoff | When an action retries                               |
| Retry budget        | Aggregate retry volume                               |
| Circuit breaker     | Whether calls to an unhealthy dependency are allowed |
| Rate limiter        | Total request rate                                   |
| Bulkhead            | Concurrent resource consumption                      |

These mechanisms complement one another.

Microsoft's transient-fault guidance recommends retry budgets in addition to per-request retry limits because thousands of individually small retry policies can collectively create a major overload.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://learn.microsoft.com\&sz=32)

Microsoft Learn+1



# 14. What happens when retries are exhausted?

Let's walk through three different scenarios.

## Scenario A: We know the operation never happened

Example:

```
Inventory Service returns:

503 Service Unavailable
```

Suppose the service contract guarantees that this particular response means no reservation was performed.

We retry according to policy.

Eventually:

```
Maximum attempts reached.
```

The orchestrator marks:

```
RESERVE_INVENTORY = FAILED
```

Now it can initiate compensation for the previously successful order creation.

```
CREATE_ORDER = SUCCEEDED

RESERVE_INVENTORY = FAILED

COMPENSATE CREATE_ORDER
```

This is safe because we know inventory was never reserved.

## Scenario B: We don't know whether the operation happened

Example:

```
Charge payment request sent.

Timeout.

Retry 1: Timeout.

Retry 2: Timeout.

Retry 3: Timeout.
```

We have exhausted automatic retries.

But do we know whether the customer was charged?

No.

Therefore, this is the wrong transition:

```
CHARGE_PAYMENT = FAILED
SAGA = COMPENSATING
```

Why?

We might refund an operation that never happened, or worse, report compensation as complete while an actual charge remains.

Instead:

```
CHARGE_PAYMENT = RECONCILIATION_REQUIRED

SAGA = MANUAL_INTERVENTION_REQUIRED
```

Or use an equivalent nonterminal operational state according to your schema.

The orchestrator stops automatic execution until the outcome is established.

An operator or automated reconciliation process can query the payment provider using the merchant transaction reference.

Once the truth is established:

```
Payment succeeded:

CHARGE_PAYMENT = SUCCEEDED
```

Or:

```
Payment never happened:

CHARGE_PAYMENT = FAILED
SAGA = COMPENSATING
```

The business decision is based on the actual outcome.

## Scenario C: The participant definitively rejected the operation

Example:

```
PAYMENT_DECLINED
```

The participant has confirmed that the payment was rejected.

No further retries are useful without changing the business conditions.

The orchestrator can start compensation.

```
SAGA = COMPENSATING
```

The key distinction:

Retry exhaustion tells us that automated execution has stopped. It does not necessarily tell us that the business operation failed.

This distinction is essential in financial workflows.

# 15. What about compensation failures?

Everything we've discussed applies to compensation too.

Compensation is not magically reliable.

Suppose:

```
CREATE_ORDER = SUCCEEDED

RESERVE_INVENTORY = SUCCEEDED

CHARGE_PAYMENT = FAILED
```

Now the orchestrator starts:

```
RELEASE_INVENTORY
CANCEL_ORDER
```

Inventory Service receives the release request.

It successfully releases stock.

But the HTTP response is lost.

The orchestrator sees a timeout.

What should it do?

Exactly what we learned in Chapter 4:

```
RELEASE_INVENTORY
    Outcome = UNKNOWN
```

Use the same compensation idempotency key.

```
SAGA-1001:RESERVE_INVENTORY:COMPENSATE
```

Retry or reconcile safely.

When the release is confirmed:

```
RELEASE_INVENTORY = SUCCEEDED
```

Then schedule:

```
CANCEL_ORDER = READY
```

Now imagine releasing inventory permanently fails.

We cannot simply declare the saga compensated.

Instead:

```
SAGA = MANUAL_INTERVENTION_REQUIRED
```

The inventory reservation may still exist.

This must remain visible and actionable.

### An important operational principle

Don't automatically stop compensating every other independent successful step just because one compensation needs investigation.

For some workflows, independent compensations may safely continue.

For others, compensation order is strict.

The workflow definition should specify these dependencies.

For a simple sequential saga, the normal compensation sequence is reverse order.

For a workflow with parallel branches, compensation may be partially parallel, but dependencies must still be respected.

# 16. Dead-letter queues — What purpose do they serve?

You will often hear about a DLQ (Dead Letter Queue).

Let's understand where it fits.

Suppose a message broker is distributing work notifications.

A consumer repeatedly fails to process a particular message.

After a configured number of attempts, the broker may move that message into a DLQ.

The idea is:

> Preserve the failed message so it can be investigated or replayed instead of endlessly blocking the normal queue.

However, in our database-backed Saga design, there is an important distinction.

The DLQ should not be the only place where an unfinished business operation exists.

The saga database remains the authoritative record.

For example:

```
saga_actions:

action_id = ACTION-100
status = MANUAL_INTERVENTION_REQUIRED

last_error = UNKNOWN_PAYMENT_OUTCOME
attempt_count = 8
```

A corresponding event or notification can be placed into an operational DLQ.

But the action and its history must remain in the database.

For a database-polling architecture, you might not need a broker DLQ at all.

You can use a durable recovery queue implemented through database statuses and queries.

For example:

```
SELECT *
FROM saga_actions
WHERE status IN (
    'RECONCILIATION_REQUIRED',
    'MANUAL_INTERVENTION_REQUIRED'
)
ORDER BY updated_at;
```

This becomes an operational work queue for investigation.

# 17. Let's design the complete retry policy for a real saga

Now we'll combine everything into an actual example.

Suppose we have:

```
ORDER_CHECKOUT
```

Steps:

```
1. CREATE_ORDER
2. RESERVE_INVENTORY
3. CHARGE_PAYMENT
4. CREATE_SHIPMENT
```

We define policies.

These are illustrative starting values, not defaults you should copy blindly.

| Configuration     | Inventory | Payment   | Shipping  |
| ----------------- | --------- | --------- | --------- |
| HTTP deadline     | 3 sec     | 8 sec     | 5 sec     |
| Initial backoff   | 2 sec     | 3 sec     | 5 sec     |
| Maximum backoff   | 30 sec    | 60 sec    | 120 sec   |
| Maximum attempts  | 5         | 6         | 8         |
| Maximum retry age | 10 min    | 30 min    | 2 hours   |
| Unknown outcome   | Reconcile | Reconcile | Reconcile |

Now let's execute.

### Event 1: Order creation succeeds

```
CREATE_ORDER = SUCCEEDED

RESERVE_INVENTORY = READY
```

### Event 2: Inventory returns HTTP 503

```
RESERVE_INVENTORY = RETRY_WAIT

attempt_count = 1

next_attempt_at = NOW + jittered backoff
```

The worker releases ownership.

### Event 3: Retry becomes eligible

Another worker claims the action.

It uses:

```
SAGA-1001:RESERVE_INVENTORY:FORWARD
```

Inventory reservation succeeds.

```
RESERVE_INVENTORY = SUCCEEDED

CHARGE_PAYMENT = READY
```

### Event 4: Payment times out

```
CHARGE_PAYMENT = UNKNOWN
```

The orchestrator queries:

```
GET /payments/operations/SAGA-1001-CHARGE_PAYMENT
```

Payment Service returns:

```
{
  "status": "PROCESSING"
}
```

The orchestrator does not retry immediately.

It schedules reconciliation for a later time.

### Event 5: Payment status becomes successful

Payment Service returns:

```
{
  "status": "SUCCEEDED",
  "paymentId": "PAY-100"
}
```

The orchestrator records:

```
CHARGE_PAYMENT = SUCCEEDED

CREATE_SHIPMENT = READY
```

### Event 6: Shipping returns HTTP 429

```
Retry-After: 45
```

The orchestrator waits at least the instructed duration, with appropriate positive jitter.

### Event 7: Shipment succeeds

```
CREATE_SHIPMENT = SUCCEEDED

SAGA = SUCCEEDED
```

Throughout this entire workflow:

- Workers were never blocked sleeping between retries.
- No database transaction was held open during HTTP calls.
- Idempotency keys remained stable.
- Ambiguous payment outcomes were reconciled.
- Temporary failures did not trigger unnecessary compensation.
- Each operation's retry policy was independently configurable.

This is the behavior we want.

# 18. What should we persist in the database for retry and reconciliation?

Let's refine our `saga_actions` table.

We need enough information to resume retry processing, even after a complete orchestrator restart.

I would include:

```
ALTER TABLE saga_actions
ADD COLUMN last_error_code VARCHAR(100),
ADD COLUMN last_error_message TEXT,
ADD COLUMN last_outcome VARCHAR(50),

ADD COLUMN next_attempt_at TIMESTAMPTZ,

ADD COLUMN max_attempts INTEGER,
ADD COLUMN max_retry_age INTERVAL,

ADD COLUMN first_attempt_at TIMESTAMPTZ,
ADD COLUMN last_attempt_at TIMESTAMPTZ,

ADD COLUMN reconcile_attempt_count INTEGER NOT NULL DEFAULT 0,

ADD COLUMN action_deadline_at TIMESTAMPTZ;
```

Some of these values can instead live in a versioned workflow definition. The important thing is to persist or be able to reliably reconstruct the policy governing an already-running action.

Now we can represent:

```
ACTION: CHARGE_PAYMENT

status: RECONCILIATION_WAIT

execution_attempt_count: 3
reconcile_attempt_count: 2

last_outcome: UNKNOWN

last_error_code: READ_TIMEOUT

next_attempt_at: 10:30:00

idempotency_key:
SAGA-1001:CHARGE_PAYMENT:FORWARD
```

Notice that we maintain two different counters:

- `execution_attempt_count`: How many times we attempted the business operation.
- `reconcile_attempt_count`: How many times we attempted to establish its actual outcome.

Why separate them?

Because querying payment status doesn't necessarily execute another payment.

We shouldn't mix business-operation attempts with status checks.

## 18.1 A more complete action state machine

Now that we understand unknown outcomes, our state machine needs a few refinements.

\#chatgpt-mermaid-\_r_592\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_592\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_592\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_592\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_592\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_592\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_592\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_592\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_592\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_592\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_592\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_592\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_592\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_592\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_592\_ p{margin:0;}#chatgpt-mermaid-\_r_592\_ defs [id$="-barbEnd"]{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_592\_ g.stateGroup text{fill:rgb(31, 78, 148);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_592\_ g.stateGroup text{fill:rgb(237, 237, 237);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_592\_ g.stateGroup .state-title{font-weight:bolder;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_592\_ g.stateGroup rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_592\_ g.stateGroup line{stroke:rgb(175, 175, 175);stroke-width:1;}#chatgpt-mermaid-\_r_592\_ .transition{stroke:rgb(175, 175, 175);stroke-width:1;fill:none;}#chatgpt-mermaid-\_r_592\_ .stateGroup .composit{fill:transparent;border-bottom:1px;}#chatgpt-mermaid-\_r_592\_ .stateGroup .alt-composit{fill:#e0e0e0;border-bottom:1px;}#chatgpt-mermaid-\_r_592\_ .state-note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_592\_ .state-note text{fill:rgb(237, 237, 237);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_592\_ .stateLabel .box{stroke:none;stroke-width:0;fill:rgb(9, 23, 44);opacity:0.5;}#chatgpt-mermaid-\_r_592\_ .edgeLabel .label rect{fill:rgb(9, 23, 44);opacity:0.5;}#chatgpt-mermaid-\_r_592\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_592\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_592\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_592\_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_592\_ .label div .edgeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_592\_ .stateLabel text{fill:rgb(237, 237, 237);font-size:10px;font-weight:bold;}#chatgpt-mermaid-\_r_592\_ .node circle.state-start{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_592\_ .node .fork-join{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_592\_ .node circle.state-end{fill:rgb(31, 78, 148);stroke:transparent;stroke-width:1.5;}#chatgpt-mermaid-\_r_592\_ .end-state-inner{fill:transparent;stroke-width:1.5;}#chatgpt-mermaid-\_r_592\_ .node rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_592\_ .node polygon{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_592\_ [id$="-barbEnd"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_592\_ .statediagram-cluster rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_592\_ .cluster-label,#chatgpt-mermaid-\_r_592\_ .nodeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_592\_ .statediagram-cluster rect.outer{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_592\_ .statediagram-state .divider{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_592\_ .statediagram-state .title-state{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_592\_ .statediagram-cluster.statediagram-cluster .inner{fill:transparent;}#chatgpt-mermaid-\_r_592\_ .statediagram-cluster.statediagram-cluster-alt .inner{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_592\_ .statediagram-cluster .inner{rx:0;ry:0;}#chatgpt-mermaid-\_r_592\_ .statediagram-state rect.basic{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_592\_ .statediagram-state rect.divider{stroke-dasharray:10,10;fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_592\_ .note-edge{stroke-dasharray:5;}#chatgpt-mermaid-\_r_592\_ .statediagram-note rect{fill:rgb(48, 48, 48);stroke:rgb(58, 132, 63);stroke-width:1px;rx:0;ry:0;}#chatgpt-mermaid-\_r_592\_ .statediagram-note rect{fill:rgb(48, 48, 48);stroke:rgb(58, 132, 63);stroke-width:1px;rx:0;ry:0;}#chatgpt-mermaid-\_r_592\_ .statediagram-note text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_592\_ .statediagram-note .nodeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_592\_ .statediagram .edgeLabel{color:red;}#chatgpt-mermaid-\_r_592\_ [id$="-dependencyStart"],#chatgpt-mermaid-\_r_592\_ [id$="-dependencyEnd"]{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);stroke-width:1;}#chatgpt-mermaid-\_r_592\_ .statediagramTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_592\_ [data-look="neo"].statediagram-cluster rect{fill:rgb(9, 23, 44);stroke:url(#chatgpt-mermaid-\_r_592\_-gradient);stroke-width:1;}#chatgpt-mermaid-\_r_592\_ [data-look="neo"].statediagram-cluster rect.outer{rx:5px;ry:5px;filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_592\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_592\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_592\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_592\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_592\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_592\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_592\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_592\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_592\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_592\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_592\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_592\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_592\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_592\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_592\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_592\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_592\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_592\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}READYRUNNINGSUCCEEDEDRETRY_WAITFAILEDRECONCILIATION_WAITMANUAL_INTERVENTION_REQUIREDWorker claimsConfirmed successRetryable failureConfirmed permanentfailureUnknown outcomeRetry becomes dueOperation foundsuccessfulOperation found rejectedSafe replay with same keyCannot establish outcomeOperator confirms successOperator confirms failureApproved safe retry

This makes an important distinction visible:

`RETRY_WAIT` means we know enough to schedule another attempt.

`RECONCILIATION_WAIT` means we first need to establish what happened or use a safe idempotent replay strategy.

Both can eventually lead to another execution, but they represent different knowledge about the world.

# 19. How would I implement the centralized retry decision?

Rather than scattering retry logic throughout your application, create something like:

```
public class FailureDecisionEngine {

    public ActionDecision decide(
            Action action,
            ParticipantResult result,
            RetryPolicy policy) {

        if (result.isConfirmedSuccess()) {
            return ActionDecision.succeed();
        }

        if (result.isConfirmedPermanentFailure()) {
            return ActionDecision.fail();
        }

        if (result.isUnknownOutcome()) {
            return ActionDecision.reconcile();
        }

        if (!result.isRetryable()) {
            return ActionDecision.fail();
        }

        if (action.isPastDeadline()) {
            return ActionDecision.manualIntervention();
        }

        if (action.getAttemptCount() >= policy.maxAttempts()) {
            return ActionDecision.exhausted();
        }

        Duration delay = calculateBackoff(
            action.getAttemptCount(),
            policy
        );

        delay = respectRetryAfter(
            delay,
            result.getRetryAfter()
        );

        return ActionDecision.retryAt(
            Instant.now().plus(delay)
        );
    }
}
```

This is conceptual code.

In production, there are a few additional details:

- Use the database time when persisting the scheduled retry.
- Check the overall retry age and workflow deadline.
- Apply the dependency's retry budget.
- Handle circuit-breaker state.
- Distinguish retry exhaustion with a known outcome from retry exhaustion with an unknown outcome.
- Make sure the decision and status transition are committed using the current claim generation.

Also, the engine must understand the participant's contract.

For example, an HTTP 500 isn't automatically a permanent error or a safe retry.

The participant's structured error code and operation-status contract should provide the necessary information.

# 20. Monitoring — How do we know the system is healthy?

A reliable system needs more than correct code.

We need visibility into failure behavior.

I would monitor these metrics.

| Metric                      | Why it matters                     |
| --------------------------- | ---------------------------------- |
| Sagas started per minute    | Incoming workflow volume           |
| Sagas succeeded             | Business completion rate           |
| Sagas compensated           | Business failure and recovery rate |
| Active RUNNING actions      | Current execution load             |
| READY action backlog        | Pending execution workload         |
| RETRY_WAIT count            | Transient dependency problems      |
| RECONCILIATION_WAIT count   | Ambiguous outcomes                 |
| Oldest pending action age   | Stuck workflows                    |
| Attempts per dependency     | Retry amplification                |
| Participant timeout rate    | Latency or availability problems   |
| Circuit breaker state       | Dependency health                  |
| Compensation failures       | Unrecovered business effects       |
| Manual intervention backlog | Operational recovery demand        |

There are some particularly useful alerts.

Alert 1: Increasing reconciliation backlog

```
Payment reconciliation backlog > threshold
```

This might indicate payment gateway instability or a broken participant integration.

Alert 2: Oldest action waiting too long

```
Oldest READY/RETRY_WAIT action > allowed age
```

This can indicate workers are not keeping up.

Alert 3: Compensation failure

```
Compensation action permanently failed
```

This might require immediate operational attention.

Alert 4: Increasing retries without increasing success

```
Retries increasing
Success rate decreasing
```

This often indicates retry amplification or an unhealthy participant.

Also record structured logs containing:

```
sagaId
actionId
stepName
attemptNumber
workerId
claimGeneration
participant
idempotencyKey
correlationId
outcome
errorCode
duration
```

Do not log sensitive payment payloads, authentication credentials, or other secrets.

# 21. The complete failure-handling architecture

We can now put everything together.

\#chatgpt-mermaid-\_r_5bi\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_5bi\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5bi\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5bi\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_5bi\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5bi\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_5bi\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_5bi\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_5bi\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_5bi\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_5bi\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_5bi\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5bi\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5bi\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_5bi\_ p{margin:0;}#chatgpt-mermaid-\_r_5bi\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5bi\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5bi\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5bi\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_5bi\_ .label text,#chatgpt-mermaid-\_r_5bi\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5bi\_ .node rect,#chatgpt-mermaid-\_r_5bi\_ .node circle,#chatgpt-mermaid-\_r_5bi\_ .node ellipse,#chatgpt-mermaid-\_r_5bi\_ .node polygon,#chatgpt-mermaid-\_r_5bi\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_5bi\_ .rough-node .label text,#chatgpt-mermaid-\_r_5bi\_ .node .label text,#chatgpt-mermaid-\_r_5bi\_ .image-shape .label,#chatgpt-mermaid-\_r_5bi\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_5bi\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_5bi\_ .rough-node .label,#chatgpt-mermaid-\_r_5bi\_ .node .label,#chatgpt-mermaid-\_r_5bi\_ .image-shape .label,#chatgpt-mermaid-\_r_5bi\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_5bi\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_5bi\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5bi\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5bi\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_5bi\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_5bi\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5bi\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5bi\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5bi\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_5bi\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_5bi\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5bi\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5bi\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_5bi\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5bi\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_5bi\_ .icon-shape,#chatgpt-mermaid-\_r_5bi\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5bi\_ .icon-shape p,#chatgpt-mermaid-\_r_5bi\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_5bi\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_5bi\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5bi\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_5bi\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_5bi\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_5bi\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_5bi\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_5bi\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_5bi\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5bi\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_5bi\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_5bi\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_5bi\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5bi\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_5bi\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_5bi\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5bi\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_5bi\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_5bi\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5bi\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_5bi\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5bi\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5bi\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5bi\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_5bi\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_5bi\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_5bi\_ .node rect,#chatgpt-mermaid-\_r_5bi\_ .node circle,#chatgpt-mermaid-\_r_5bi\_ .node ellipse,#chatgpt-mermaid-\_r_5bi\_ .node polygon,#chatgpt-mermaid-\_r_5bi\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_5bi\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_5bi\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_5bi\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_5bi\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5bi\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Worker executes actionCall participant with stableidempotency keyClassify resultPersist SUCCEEDEDPersist FAILEDCheck retry policyRetry permitted?Calculate backoff + jitterPersist RETRY_WAITFail or escalate according tooutcomePersistRECONCILIATION_WAITQuery status / safeidempotent replay / awaiteventOutcome established?Retry reconciliationRecovery deadlineexhausted?MANUAL_INTERVENTION_REQUIREDSchedule next workflowactionEvaluate compensationpolicyWorker executes when retryis dueConfirmed successPermanent rejectionTransient failureYesNoUnknown outcomeSuccessDefinitive failureStill unknownNoYes

This is the foundation of a robust failure-handling system.

The most important thing is that the system doesn't simply classify every exception as SUCCESS or FAILED.

It explicitly supports the possibility that the result is unknown.

## Chapter 5 — What you should now understand

Let's reduce this entire chapter to six fundamental ideas.

1\. Timeout is not failure.

A timeout means the caller stopped waiting. The participant may have committed successfully.

2\. Retry only when the operation is safe to retry.

For distributed business operations, that generally means using stable participant-side idempotency.

3\. Persist retries instead of sleeping.

A worker should release its resources, and the database should remember when the operation becomes eligible again.

4\. Backoff with jitter protects recovering services.

Without it, thousands of workers may retry simultaneously and make an outage worse.

5\. Retry exhaustion does not establish the business outcome.

A payment that times out eight times is not necessarily a failed payment.

6\. Compensation needs the same reliability guarantees.

Refunds, releases and cancellations are business operations that can fail, time out and be duplicated.

The mental model is:

> Retry when another attempt can safely make progress. Reconcile when you don't know what happened. Compensate when a definitive business failure requires reversing previous effects.

For further reading, the [Google SRE chapter on cascading failures](https://sre.google/sre-book/addressing-cascading-failures/?utm_source=chatgpt.com) and [Microsoft's transient fault handling guidance](https://learn.microsoft.com/en-us/azure/architecture/best-practices/transient-faults?utm_source=chatgpt.com) are especially valuable. They explain why poorly designed retries can turn a small service outage into a system-wide outage.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://sre.google\&sz=32)

Cascading Failures: Reducing System Outage+2



# Next Chapter — Chapter 6: Compensation — How Rollback Actually Works

This is where we'll understand one of the most misunderstood parts of the Saga pattern.

Consider:

```
CREATE_ORDER        SUCCEEDED
RESERVE_INVENTORY   SUCCEEDED
CHARGE_PAYMENT      SUCCEEDED
CREATE_SHIPMENT     FAILED
```

What exactly does rollback mean here?

Does the orchestrator delete the order?

Does it directly update inventory quantities?

Does it call the Payment Service to refund the customer?

What happens if the refund fails?

What if the shipment was already handed to the courier?

We'll build the compensation mechanism from scratch.

Specifically:

1. Why Saga compensation is not traditional database rollback.
2. How to define a forward operation and its corresponding compensation.
3. Why compensation normally executes in reverse dependency order.
4. How to handle parallel steps and dependency graphs.
5. How to persist and execute compensation actions.
6. How compensation uses its own idempotency keys, leases and retries.
7. What happens when compensation permanently fails.
8. What to do with irreversible operations such as emails, notifications and shipments.
9. Forward recovery versus backward recovery.
10. How to identify the point after which compensation is no longer appropriate.

We'll use the same Order Checkout workflow and evolve it into a complete compensation engine.


