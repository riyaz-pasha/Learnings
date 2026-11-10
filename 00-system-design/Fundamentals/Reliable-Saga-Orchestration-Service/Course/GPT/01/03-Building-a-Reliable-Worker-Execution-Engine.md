# Chapter 3 — Building a Reliable Worker Execution Engine

Now we are getting to the heart of the Saga Orchestrator.

In Chapter 2, we designed the database and state machine.

We know that a step can be:

```
BLOCKED
READY
RUNNING
RETRY_WAIT
SUCCEEDED
FAILED
```

We also know that multiple workers can execute different sagas simultaneously.

But we haven't solved the most important concurrency problem:

How do we guarantee that two workers don't execute the same step at the same time, and how do we recover when a worker crashes?

We'll build this from first principles.

Our implementation will use PostgreSQL because it provides row-level locking and `SKIP LOCKED`, which are particularly useful for implementing database-backed work queues.

## 1. Start with a simple problem: Two workers, one step

Imagine our database contains:

```
SAGA-1001

CREATE_ORDER        SUCCEEDED
RESERVE_INVENTORY   READY
CHARGE_PAYMENT      BLOCKED
CREATE_SHIPMENT     BLOCKED
```

We have two workers:

```
Worker A
Worker B
```

Both are continuously looking for work.

Let's start with the simplest implementation.

```
SELECT *
FROM saga_steps
WHERE forward_status = 'READY'
LIMIT 1;
```

What can go wrong?

Consider this timeline:

```
Time     Worker A                 Worker B
------------------------------------------------
T1       Query database
T2                                Query database
T3       Gets RESERVE_INVENTORY
T4                                Gets RESERVE_INVENTORY
T5       Starts execution
T6                                Starts execution
```

Both workers now execute:

```
RESERVE_INVENTORY
```

This is a disaster.

We might reserve inventory twice.

You might suggest:

"Let's update the status to RUNNING after selecting it."

Let's try.

Worker A:

```
UPDATE saga_steps
SET forward_status = 'RUNNING'
WHERE id = 'STEP-1001';
```

Worker B:

```
UPDATE saga_steps
SET forward_status = 'RUNNING'
WHERE id = 'STEP-1001';
```

Both updates may execute successfully, one after another.

Why?

Because we haven't made selecting and claiming the work one atomic operation.

We need a mechanism that allows exactly one worker to claim a particular piece of work at a time.

This brings us to database locking.

# 2. Understanding row-level locking

Let's first understand the underlying idea.

Imagine the database has this record:

```
STEP-1001

status = READY
```

Worker A executes:

```
BEGIN;

SELECT *
FROM saga_steps
WHERE id = 'STEP-1001'
FOR UPDATE;
```

What does `FOR UPDATE` mean?

It tells PostgreSQL:

> I am selecting this row because I intend to modify it. Lock this row so another transaction cannot simultaneously acquire a conflicting lock or modify it.

Now imagine Worker B executes the same query.

```
BEGIN;

SELECT *
FROM saga_steps
WHERE id = 'STEP-1001'
FOR UPDATE;
```

Worker B cannot acquire that conflicting lock while Worker A holds it.

It has to wait.

Conceptually:

```
                  STEP-1001
                  status=READY
                       |
              +--------+--------+
              |                 |
              v                 v
           Worker A          Worker B
              |
              | FOR UPDATE
              v
         Acquires lock
              |
              | Worker B cannot
              | acquire lock yet
              |
              v
         Updates status
         to RUNNING
              |
              v
            COMMIT
              |
              v
          Lock released
              |
              v
       Worker B continues
```

When Worker A commits, Worker B can proceed.

Under PostgreSQL's normal READ COMMITTED isolation, Worker B sees the committed row version and its updated status.

If Worker B is using a locking query with the original `status = 'READY'` condition, it won't select that row anymore.

So the basic solution is:

Lock the row, check that it is eligible, update its status, and commit.

PostgreSQL holds row-level locks until the transaction ends.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.postgresql.org\&sz=32)

PostgreSQL+1



But this creates another problem.

Imagine we have 100 workers and 100 eligible steps.

We don't want workers sitting around waiting for other workers to finish claiming rows.

We want them to pick different available records.

That's why PostgreSQL provides something very useful.

# 3. Understanding `SKIP LOCKED`

Consider this table:

| Step   | Status |
| ------ | ------ |
| STEP-1 | READY  |
| STEP-2 | READY  |
| STEP-3 | READY  |
| STEP-4 | READY  |

Now three workers are looking for work.

Worker A:

```
BEGIN;

SELECT *
FROM saga_steps
WHERE forward_status = 'READY'
ORDER BY created_at
FOR UPDATE
LIMIT 1;
```

Suppose it gets STEP-1 and locks it.

Worker B executes:

```
SELECT *
FROM saga_steps
WHERE forward_status = 'READY'
ORDER BY created_at
FOR UPDATE SKIP LOCKED
LIMIT 1;
```

What happens?

It encounters STEP-1.

But STEP-1 is already locked by Worker A.

Instead of waiting, Worker B skips it and selects STEP-2.

Worker C selects STEP-3.

We now have:

```
Worker A -> STEP-1
Worker B -> STEP-2
Worker C -> STEP-3
```

All workers can claim different work without unnecessarily waiting for one another.

That's exactly what we need.

`SKIP LOCKED` is specifically documented by PostgreSQL as useful for queue-like tables with multiple consumers. It intentionally produces an inconsistent view of locked rows, so it should be used for work claiming, not ordinary business queries.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.postgresql.org\&sz=32)

PostgreSQL+1



## 3.1 The important distinction

These two SQL statements have different behaviors:

```
FOR UPDATE
```

Means:

> Wait if another transaction has a conflicting lock.

```
FOR UPDATE SKIP LOCKED
```

Means:

> Skip rows that are currently locked and find another eligible row.

For our work-dispatching mechanism, the second behavior is generally preferable.

# 4. Our first production-style worker claiming implementation

Let's put this together.

We need some additional columns in `saga_steps`.

```
ALTER TABLE saga_steps
ADD COLUMN lease_owner VARCHAR(255),
ADD COLUMN lease_until TIMESTAMPTZ,
ADD COLUMN claim_generation BIGINT NOT NULL DEFAULT 0;
```

We'll understand all three shortly.

For now, let's implement atomic claiming.

### SQL implementation

```
BEGIN;

WITH candidate AS (
    SELECT id
    FROM saga_steps
    WHERE forward_status = 'READY'
    ORDER BY created_at
    FOR UPDATE SKIP LOCKED
    LIMIT 1
)
UPDATE saga_steps s
SET
    forward_status = 'RUNNING',
    lease_owner = :worker_id,
    lease_until = clock_timestamp() + INTERVAL '45 seconds',
    claim_generation = claim_generation + 1,
    attempt_count = attempt_count + 1,
    updated_at = clock_timestamp()
FROM candidate c
WHERE s.id = c.id
RETURNING s.*;

COMMIT;
```

This is a simplified first version. We'll add retry and crash-recovery eligibility shortly.

Let's understand exactly what happens.

Step 1: Find one eligible step.

```
SELECT id
FROM saga_steps
WHERE forward_status = 'READY'
ORDER BY created_at
FOR UPDATE SKIP LOCKED
LIMIT 1;
```

This finds an available step and locks it.

Step 2: Update its status.

```
UPDATE saga_steps
SET forward_status = 'RUNNING'
```

It also records who claimed it.

```
lease_owner = :worker_id
```

And when ownership expires:

```
lease_until = clock_timestamp() + INTERVAL '45 seconds'
```

Step 3: Increment the claim generation.

```
claim_generation = claim_generation + 1
```

We'll understand why shortly.

Step 4: Commit.

After commit, the row lock is released.

But the persisted `RUNNING` status and lease information remain.

The worker receives the claimed row through `RETURNING`.

If no row is returned, there is currently no work available.

This entire claim operation is one short database transaction.

The worker must also create its `saga_step_attempts` record in this transaction, so the claim and attempt history are consistent.

For example:

```
STEP-1001

forward_status    = RUNNING
lease_owner       = worker-A
lease_until       = 17:30:45
claim_generation  = 1
attempt_count     = 1
```

We have successfully claimed the work.

But we haven't yet executed the remote service.

That comes next.

# 5. Very important: Why we release the DB lock before making an HTTP call

This is one of the most important implementation details.

A beginner might write:

```
@Transactional
public void executeStep() {

    SagaStep step = claimStep();

    inventoryService.reserveInventory(step.getRequest());

    markStepSucceeded(step);
}
```

This looks convenient.

But consider:

```
Database transaction begins
        |
        v
Lock saga step
        |
        v
HTTP request to Inventory Service
        |
        | 30 seconds
        |
        v
Update step status
        |
        v
Database transaction commits
```

For 30 seconds, we are holding a database transaction open.

This can cause:

- Long-lived row locks.
- Database connection exhaustion.
- Increased transaction contention.
- Delayed visibility of state transitions.
- Problems when remote services become slow or unavailable.

Instead, split the process into three separate stages.

```
TRANSACTION 1
    Claim step
    Mark RUNNING
    Save ownership
    Save attempt
    COMMIT

OUTSIDE TRANSACTION
    Call Inventory Service

TRANSACTION 2
    Record response
    Update step status
    Make next step READY
    Update saga status if needed
    COMMIT
```

In Java, conceptually:

```
public void executeOneStep() {

    // Transaction 1: Claim and persist ownership.
    ClaimedStep step = claimService.claimOne();

    if (step == null) {
        return;
    }

    // No database transaction should be held here.
    StepResult result = participantClient.execute(
        step.getRequest(),
        step.getIdempotencyKey()
    );

    // Transaction 2: Persist outcome.
    completionService.complete(step, result);
}
```

In Spring, make sure these transaction boundaries are actually implemented. For example, transactionally annotated methods called through separate Spring-managed beans avoid common self-invocation problems with proxy-based transaction management.

The important point is:

The database transaction protects the state transition, not the entire remote business operation.

# 6. Now the big problem: What if a worker crashes?

So far, we've solved duplicate claiming during normal execution.

But let's consider a crash.

Our database contains:

```
STEP-1001

forward_status    = RUNNING
lease_owner       = worker-A
claim_generation  = 1
lease_until       = 17:30:45
```

Worker A has claimed the step.

Now:

```
Worker A crashes.
```

What happens to the database record?

Nothing automatically.

It still says:

```
RUNNING
```

And another worker querying this condition:

```
WHERE forward_status = 'READY'
```

will never find it.

The saga gets stuck forever.

How do we solve this?

## 6.1 Introduce the concept of a lease

A lease is a temporary ownership period.

Think of it like renting something.

Worker A claims a task for a limited amount of time.

For example:

```
Worker A owns STEP-1001 until 17:30:45.
```

After that time, if the task is still unfinished, another worker may take over.

The database record already contains:

```
lease_owner
lease_until
```

Now our worker recovery mechanism has a way to distinguish:

- A task currently owned by a worker with a valid lease.
- A task whose worker has disappeared or whose lease has expired.

For example:

```
STEP-1001

status       = RUNNING
lease_owner  = worker-A
lease_until  = 17:30:45
```

Current database time:

```
17:31:00
```

The lease has expired.

Worker B can now claim this step.

```
Before:

STEP-1001
RUNNING
owner = worker-A
lease expired


After:

STEP-1001
RUNNING
owner = worker-B
lease renewed
generation = 2
```

Notice something:

We don't necessarily need to reset the step to READY first.

The claiming logic can directly reclaim expired RUNNING records.

## 6.2 Updated claim query

Now our claim query needs to support three situations:

1. A new READY step.
2. A RETRY_WAIT step whose retry time has arrived.
3. A RUNNING step whose lease expired.

```
BEGIN;

WITH candidate AS (
    SELECT id
    FROM saga_steps
    WHERE
        forward_status = 'READY'

        OR (
            forward_status = 'RETRY_WAIT'
            AND next_attempt_at <= clock_timestamp()
        )

        OR (
            forward_status = 'RUNNING'
            AND lease_until <= clock_timestamp()
        )

    ORDER BY created_at

    FOR UPDATE SKIP LOCKED
    LIMIT 1
)
UPDATE saga_steps s
SET
    forward_status = 'RUNNING',

    lease_owner = :worker_id,

    lease_until =
        clock_timestamp() + INTERVAL '45 seconds',

    claim_generation = claim_generation + 1,

    attempt_count = attempt_count + 1,

    updated_at = clock_timestamp()

FROM candidate c

WHERE s.id = c.id

RETURNING s.*;

COMMIT;
```

This is the basic lease-based claiming mechanism.

A production query should use indexes and appropriate priority/scheduling order for READY work, retries and expired leases. We will refine those when discussing scale.

### What happens now?

Let's simulate it.

```
17:30:00

Worker A claims STEP-1001.

lease_until = 17:30:45
generation  = 1
```

Worker A crashes.

```
17:30:45

Lease expires.
```

Worker B queries for work.

It finds:

```
forward_status = RUNNING
lease_until <= current database time
```

It reclaims the task.

```
17:30:46

Worker B claims STEP-1001.

lease_owner      = worker-B
lease_until      = 17:31:31
claim_generation = 2
```

The work can now continue.

# 7. A lease alone is NOT enough — The stale worker problem

We've solved worker crashes, but we have introduced a subtle problem.

Consider this timeline:

```
17:30:00

Worker A claims STEP-1001.
Lease expires at 17:30:45.

Worker A calls Inventory Service.
```

Suppose Inventory Service is extremely slow.

Its HTTP request is still executing.

Now:

```
17:30:45

Worker A's lease expires.
```

Worker B assumes the work is abandoned and reclaims it.

```
17:30:46

Worker B claims STEP-1001.
```

Now both workers might still be running!

```
Worker A:
    Still waiting for Inventory Service.

Worker B:
    Sends another request to Inventory Service.
```

This creates a potential duplicate execution.

Why did this happen?

Because:

Lease expiration does not prove that the original worker has actually stopped.

It only means its ownership period has expired.

There is no reliable way for the database to know whether a remote worker has crashed, been paused by the operating system, lost network connectivity, or is simply slow.

We need two additional mechanisms:

1. Lease renewal.
2. Fencing tokens.

Let's understand both.

# 8. Lease renewal — Keeping ownership alive

Imagine a worker is processing a long-running step.

Its current lease is 45 seconds.

But the remote operation might take two minutes.

We don't want another worker to reclaim the task simply because the original operation is taking longer than expected.

The worker can renew its lease periodically.

For example:

```
Initial lease: 45 seconds

After 10 seconds:
    Renew for another 45 seconds.

After another 10 seconds:
    Renew for another 45 seconds.

Continue while the operation is active.
```

SQL:

```
UPDATE saga_steps
SET lease_until =
    clock_timestamp() + INTERVAL '45 seconds'

WHERE id = :step_id

  AND forward_status = 'RUNNING'

  AND lease_owner = :worker_id

  AND claim_generation = :claim_generation

  AND lease_until > clock_timestamp();
```

Why are we checking all these conditions?

Because the worker must prove that it still owns the current execution.

Suppose Worker A's generation is 1.

Worker B has reclaimed the task and its generation is 2.

Worker A tries renewing:

```
Worker A:

worker_id = worker-A
generation = 1

Database:

worker_id = worker-B
generation = 2
```

The UPDATE affects zero rows.

Worker A has lost ownership.

It must not continue pretending it owns the task.

### How frequently should we renew?

There is no universal number.

A reasonable example:

```
Lease duration: 45 seconds
Heartbeat interval: 10 seconds
```

But choose values based on:

- Expected operation duration.
- HTTP timeout.
- Database latency.
- Scheduling delays.
- Infrastructure pauses.

Use the database clock for lease calculations rather than relying on each worker machine's clock. This avoids clock-skew problems.

For long-running HTTP operations, the heartbeat should be independent of the blocked request thread.

If renewing the lease fails, the worker should consider its ownership lost, stop or cancel ongoing work where possible, and never commit a result using its old ownership token.

However, cancellation is best-effort. A remote service may already be processing the request.

That's why we need fencing and idempotency.

# 9. Fencing tokens — Preventing stale workers from modifying current state

This is a critical distributed systems concept.

Let's continue our previous scenario.

Initially:

```
STEP-1001

lease_owner = worker-A
claim_generation = 1
```

Worker A starts executing.

Its lease expires.

Worker B takes over.

```
STEP-1001

lease_owner = worker-B
claim_generation = 2
```

Now imagine Worker A finally receives a successful response from Inventory Service.

It tries to update the database:

```
UPDATE saga_steps
SET forward_status = 'SUCCEEDED'
WHERE id = 'STEP-1001';
```

What's wrong with this query?

It doesn't check who owns the task!

Worker A can overwrite Worker B's state.

We need to prevent that.

## 9.1 Use a monotonically increasing generation

Every time a worker claims a step, increment:

```
claim_generation
```

Example:

| Claim        | Worker   | Generation |
| ------------ | -------- | ---------- |
| First claim  | Worker A | 1          |
| Second claim | Worker B | 2          |
| Third claim  | Worker C | 3          |
| Fourth claim | Worker A | 4          |

The generation is a fencing token.

It identifies the latest ownership epoch.

When Worker A originally claimed the task, it received:

```
generation = 1
```

When Worker B took over:

```
generation = 2
```

Now Worker A cannot modify the task using generation 1.

## 9.2 Protect completion with the fencing token

Instead of:

```
UPDATE saga_steps
SET forward_status = 'SUCCEEDED'
WHERE id = :step_id;
```

Use:

```
UPDATE saga_steps
SET
    forward_status = 'SUCCEEDED',
    forward_response = :response,

    lease_owner = NULL,
    lease_until = NULL,

    updated_at = clock_timestamp()

WHERE id = :step_id

  AND forward_status = 'RUNNING'

  AND lease_owner = :worker_id

  AND claim_generation = :claim_generation

  AND lease_until > clock_timestamp();
```

Now suppose:

Worker A:

```
worker_id = worker-A
generation = 1
```

Database:

```
worker_id = worker-B
generation = 2
```

Worker A executes the UPDATE.

Result:

```
Updated rows = 0
```

The old worker has no authority to complete the step.

Worker B is the current owner.

This is the purpose of fencing.

The generation check prevents a stale worker from overwriting the current workflow state, even if that worker resumes after a long pause.

### What if Worker B's lease expires too?

The process continues:

```
Worker A -> generation 1
Worker B -> generation 2
Worker C -> generation 3
```

Only the worker holding the current generation can update the step.

The fundamental rule: Every state-changing worker operation must be conditional on its current claim ownership.

That includes:

- Successful completion.
- Retry scheduling.
- Permanent failure.
- Lease renewal.
- Compensation transitions.

Do not protect only the success path.

# 10. Important distinction: Fencing protects the orchestrator DB, not automatically the participant

This is where we need to be very careful.

Suppose:

```
Worker A -> calls Inventory Service
Worker A lease expires

Worker B -> takes over
Worker B -> calls Inventory Service
```

Worker A's fencing token prevents it from updating the orchestrator database after Worker B takes over.

But does it stop Worker A's HTTP request from reaching Inventory Service?

No.

The database fencing token protects the orchestrator's state. It does not automatically fence external side effects.

For that, the participant must handle duplicate operations safely.

For example:

```
POST /inventory/reservations
Idempotency-Key: SAGA-1001-RESERVE_INVENTORY
```

Both Worker A and Worker B must use the same key.

Not:

```
Worker A: SAGA-1001-RESERVE_INVENTORY-ATTEMPT-1
Worker B: SAGA-1001-RESERVE_INVENTORY-ATTEMPT-2
```

That would defeat deduplication.

The logical idempotency key must stay stable across retries and worker takeovers.

For operations where the downstream system itself supports fencing, you can also propagate the increasing generation and have it reject older generations.

But ordinary participant services should at least implement durable idempotency.

We'll deeply explore how participants implement it in Chapter 4.

# 11. What happens after a worker receives a successful response?

We've successfully claimed a step and called the participant.

Now we must persist its result.

Suppose:

```
SAGA-1001

CREATE_ORDER        SUCCEEDED
RESERVE_INVENTORY   RUNNING
CHARGE_PAYMENT      BLOCKED
CREATE_SHIPMENT     BLOCKED
```

Worker B receives:

```
{
  "reservationId": "RES-987",
  "status": "SUCCESS"
}
```

The orchestrator must perform several changes atomically:

1. Mark inventory reservation successful.
2. Store the participant response.
3. Close the execution attempt.
4. Make the next step eligible.
5. Update the saga metadata.

For our simple sequential workflow:

```
BEGIN;

UPDATE saga_steps
SET
    forward_status = 'SUCCEEDED',
    forward_response = :response,
    lease_owner = NULL,
    lease_until = NULL,
    updated_at = clock_timestamp()

WHERE id = :step_id
  AND forward_status = 'RUNNING'
  AND lease_owner = :worker_id
  AND claim_generation = :claim_generation
  AND lease_until > clock_timestamp();

-- Verify that exactly one row was updated.

-- If zero rows were updated, this worker is stale.
-- Roll back; do not schedule the next step.

UPDATE saga_step_attempts
SET
    outcome = 'SUCCESS',
    completed_at = clock_timestamp(),
    response_payload = :response

WHERE id = :attempt_id;

UPDATE saga_steps
SET forward_status = 'READY'
WHERE saga_id = :saga_id
  AND step_order = :current_order + 1
  AND forward_status = 'BLOCKED';

-- Update saga metadata.

COMMIT;
```

The row-count check is essential.

Imagine Worker A has lost ownership.

Its first UPDATE affects zero rows.

It must not continue updating the next step or mark the saga successful.

In real Java code, perform the ownership check and all subsequent workflow mutations within the same transaction. If the guarded update fails, roll back the entire completion transaction.

Also, the `step_order + 1` example works only for a strictly sequential workflow.

For branching or parallel workflows, activate a successor only when all its prerequisite steps have succeeded.

For example:

```
                CREATE_ORDER
                      |
               RESERVE_INVENTORY
                      |
                +-----+-----+
                |           |
           CHARGE_PAYMENT  VALIDATE_ADDRESS
                |           |
                +-----+-----+
                      |
                CREATE_SHIPMENT
```

Payment completion alone should not activate shipment if address validation is still pending.

# 12. The complete worker execution lifecycle

Let's now assemble everything.

This is what one worker actually does.

\#chatgpt-mermaid-\_r_3vf\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3vf\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3vf\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3vf\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3vf\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vf\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3vf\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3vf\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3vf\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3vf\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3vf\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3vf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3vf\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3vf\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3vf\_ p{margin:0;}#chatgpt-mermaid-\_r_3vf\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vf\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vf\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vf\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3vf\_ .label text,#chatgpt-mermaid-\_r_3vf\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vf\_ .node rect,#chatgpt-mermaid-\_r_3vf\_ .node circle,#chatgpt-mermaid-\_r_3vf\_ .node ellipse,#chatgpt-mermaid-\_r_3vf\_ .node polygon,#chatgpt-mermaid-\_r_3vf\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3vf\_ .rough-node .label text,#chatgpt-mermaid-\_r_3vf\_ .node .label text,#chatgpt-mermaid-\_r_3vf\_ .image-shape .label,#chatgpt-mermaid-\_r_3vf\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3vf\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3vf\_ .rough-node .label,#chatgpt-mermaid-\_r_3vf\_ .node .label,#chatgpt-mermaid-\_r_3vf\_ .image-shape .label,#chatgpt-mermaid-\_r_3vf\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3vf\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3vf\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3vf\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3vf\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3vf\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3vf\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3vf\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3vf\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3vf\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3vf\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3vf\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vf\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vf\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3vf\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vf\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3vf\_ .icon-shape,#chatgpt-mermaid-\_r_3vf\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3vf\_ .icon-shape p,#chatgpt-mermaid-\_r_3vf\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3vf\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3vf\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3vf\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3vf\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3vf\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3vf\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3vf\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3vf\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3vf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3vf\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3vf\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3vf\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3vf\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3vf\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3vf\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3vf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3vf\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3vf\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3vf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3vf\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3vf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3vf\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3vf\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3vf\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3vf\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3vf\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3vf\_ .node rect,#chatgpt-mermaid-\_r_3vf\_ .node circle,#chatgpt-mermaid-\_r_3vf\_ .node ellipse,#chatgpt-mermaid-\_r_3vf\_ .node polygon,#chatgpt-mermaid-\_r_3vf\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3vf\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3vf\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3vf\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3vf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3vf\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Worker starts pollingClaim eligible step using DBtransactionWas a step claimed?Sleep / wait for notificationReceive claim token andleaseCall participant outside DBtransactionResult received?Complete step using claimtokenOwnership still valid?Persist success and nexteligible stepDiscard stale result /reconcileSchedule retry and releaseleaseMark failed and begincompensationResolve or retry with sameidempotency keyNoYesSuccessYesNoRetryable failurePermanent failureUnknown outcome

Let's translate that into Java-like pseudocode:

```
public void workerLoop() {

    while (!Thread.currentThread().isInterrupted()) {

        ClaimedStep step = claimService.claimOne();

        if (step == null) {
            sleepWithSmallJitter();
            continue;
        }

        try {

            StepResult result = participantClient.execute(
                step.getEndpoint(),
                step.getRequest(),
                step.getIdempotencyKey()
            );

            completionService.complete(step, result);

        } catch (RetryableException ex) {

            retryService.scheduleRetry(step, ex);

        } catch (PermanentException ex) {

            failureService.markFailed(step, ex);

        } catch (UnknownOutcomeException ex) {

            recoveryService.scheduleReconciliation(step, ex);
        }
    }
}
```

This is conceptual code. Actual production implementations should also handle cancellation, worker shutdown, unexpected exceptions, transaction failures and response validation.

One important detail:

`completionService.complete()` must verify the claim generation and ownership before persisting the result.

Similarly, retry and failure updates must verify them.

A worker must never be able to modify workflow state merely because it knows the saga ID.

# 13. The most important crash scenarios — Let's test our design

A reliable system should be evaluated by intentionally crashing it at different points.

Let's walk through the possibilities.

## Scenario A: Worker crashes before claiming

```
Database:
READY

Worker A:
Crashes before claim.
```

Nothing happens.

Another worker can claim the step.

## Scenario B: Worker crashes while the claim transaction is executing

```
Worker A:

BEGIN
SELECT FOR UPDATE
UPDATE status = RUNNING

CRASH before COMMIT
```

The database rolls back the unfinished transaction.

The step remains READY.

Another worker can claim it.

## Scenario C: Worker crashes immediately after claiming

```
Worker A:
Claims step.
Commits transaction.

CRASH before HTTP call.
```

Database:

```
RUNNING
lease_until = future time
```

Another worker cannot claim it immediately.

After lease expiry, another worker can reclaim it.

No business operation may have happened yet, but retrying is safe.

## Scenario D: Worker crashes during the HTTP call

```
Worker A:
Sends request.

Order Service:
Processes request.

Worker A:
CRASHES.
```

The remote outcome is unknown.

After lease expiry, another worker reclaims the step and sends the same request with the same idempotency key.

If the participant completed the operation, it should return the stored result instead of repeating the business effect.

## Scenario E: Participant succeeds, but worker crashes before saving success

This is perhaps the most important one.

```
Worker A
    |
    v
Inventory Service
    |
    v
Reservation committed
    |
    v
Response received
    |
    X Worker crashes before updating orchestrator DB
```

Orchestrator DB still says:

```
RESERVE_INVENTORY = RUNNING
```

After lease expiry:

```
Worker B claims the operation.
```

It uses the original idempotency key.

Inventory Service recognizes that the reservation already succeeded.

It returns the original outcome.

Worker B persists:

```
RESERVE_INVENTORY = SUCCEEDED
```

This is how the two services cooperate to survive the crash.

## Scenario F: Worker crashes after updating success but before committing

The entire completion transaction rolls back.

The step remains RUNNING until lease expiry.

It gets retried with the same participant idempotency key.

No duplicate business effect should occur.

## Scenario G: Worker crashes after committing success

```
Database:
RESERVE_INVENTORY = SUCCEEDED
CHARGE_PAYMENT = READY
```

The next worker can discover CHARGE_PAYMENT and continue.

The previous worker's memory is no longer needed.

## Scenario H: Old worker returns after another worker has taken over

```
Worker A: generation 1
Worker B: generation 2
```

Worker A tries completing the step.

Its conditional UPDATE affects zero rows.

It cannot overwrite Worker B's state.

The stable participant idempotency key protects the business operation.

These eight scenarios form a useful foundation for your failure-injection tests.

# 14. How should a worker handle retryable and permanent failures?

We haven't yet implemented the other two paths in our worker.

Suppose Inventory Service returns HTTP 503.

The worker should not immediately start another HTTP call inside the same execution loop.

Instead, it should persist the retry schedule.

### Retryable failure

```
BEGIN;

UPDATE saga_steps
SET
    forward_status = 'RETRY_WAIT',
    next_attempt_at = :retry_time,
    lease_owner = NULL,
    lease_until = NULL,
    updated_at = clock_timestamp()

WHERE id = :step_id
  AND forward_status = 'RUNNING'
  AND lease_owner = :worker_id
  AND claim_generation = :claim_generation
  AND lease_until > clock_timestamp();

-- Verify exactly one row was updated.

UPDATE saga_step_attempts
SET
    outcome = 'RETRYABLE_FAILURE',
    error_message = :error,
    completed_at = clock_timestamp()
WHERE id = :attempt_id;

COMMIT;
```

The worker is now free to execute other tasks.

When `next_attempt_at` arrives, another worker can claim the step.

### Permanent failure

Suppose Payment Service returns:

```
{
  "code": "INSUFFICIENT_FUNDS",
  "message": "Payment declined"
}
```

The worker reports this outcome to the orchestrator's state machine.

The orchestrator transitions the step to FAILED and the saga to COMPENSATING when compensation is necessary.

The worker does not independently decide which earlier operations need compensation.

That decision belongs to the workflow state machine.

### Unknown outcome

Suppose the HTTP request times out.

The worker doesn't know whether the payment happened.

It should not confidently mark the step FAILED.

It should persist an outcome such as:

```
UNKNOWN
```

Then the orchestrator can:

1. Query the participant to determine whether the operation succeeded.
2. Retry the same logical operation with the same idempotency key.
3. Wait for a participant's operation-status response.
4. Escalate to manual reconciliation if the result cannot be established safely.

This is why we distinguish a confirmed failure from an unknown result.

We'll explore the retry and reconciliation policies in Chapter 5.

# 15. How many workers should we run?

Let's say we have:

```
Worker pool size = 10

Each worker can process one active operation.
```

Conceptually:

```
Worker 1  -> SAGA-1  -> CREATE_ORDER
Worker 2  -> SAGA-2  -> CHARGE_PAYMENT
Worker 3  -> SAGA-3  -> RESERVE_INVENTORY
Worker 4  -> SAGA-4  -> CREATE_SHIPMENT
Worker 5  -> SAGA-5  -> CREATE_ORDER
Worker 6  -> Waiting for work
Worker 7  -> Waiting for work
Worker 8  -> Waiting for work
Worker 9  -> Waiting for work
Worker 10 -> Waiting for work
```

Now imagine we deploy three orchestrator instances:

```
Instance A: 10 worker slots
Instance B: 10 worker slots
Instance C: 10 worker slots
```

Total execution capacity:

```
30 concurrent operations
```

All instances use the same PostgreSQL database.

`FOR UPDATE SKIP LOCKED` allows them to claim different available rows.

The database coordinates ownership.

We don't need to distribute locks through an external distributed-lock service just to claim these database records.

### Important: Worker count is not unlimited

Suppose we run 500 workers, but Payment Service can comfortably handle only 50 concurrent requests.

Our orchestrator could overwhelm Payment Service.

We need bounded concurrency.

For example:

```
Maximum active operations:

Order Service:       100
Inventory Service:   50
Payment Service:     20
Shipping Service:    40
```

These limits are illustrative.

They should be based on downstream capacity, rate limits, latency and resource consumption.

A worker pool might have:

- A global concurrency limit.
- A per-participant concurrency limit.
- A per-tenant concurrency limit.
- A per-saga concurrency limit, where appropriate.

This is called backpressure.

It prevents the orchestrator from generating more work than the downstream system can safely handle.

### Should one worker claim multiple steps at once?

Generally, claim only as much work as the worker can begin executing immediately.

Imagine one worker claims 100 steps, but it can execute only 5 concurrently.

The remaining 95 steps are sitting in its local queue while their leases are already running.

Some leases might expire before execution even starts.

That's undesirable.

For our initial implementation:

One available execution slot should claim one task.

Later, you can implement controlled batch claiming when it actually improves database throughput.

# 16. How should workers poll the database?

We now have a functioning claim query.

But workers need a loop that continuously looks for work.

A basic approach:

```
while (running) {

    ClaimedStep step = claimService.claimOne();

    if (step == null) {
        Thread.sleep(1000);
        continue;
    }

    process(step);
}
```

This works, but constantly polling at a very high frequency can generate unnecessary database load.

There are several improvements.

### A. Adaptive polling

When work is available, continue polling quickly.

When no work is available, gradually increase the delay.

For example:

```
No work found:

100 ms
200 ms
400 ms
800 ms
1.5 seconds
```

Introduce random jitter so that all worker instances don't wake up at exactly the same moment.

Reset the delay when work becomes available.

### B. Message-based notifications

You can also use:

```
Database
   |
   | Outbox event
   v
Message Broker
   |
   v
Worker receives notification
   |
   v
Worker claims actual DB record
```

Notice something important:

The notification does not grant ownership of the work. The database claim still does.

If Kafka delivers the same notification twice, two workers might wake up, but only one can claim the particular READY record.

If notifications are lost, polling can still discover the work.

This is a useful combination:

- Database for durable truth.
- Outbox for reliable publication.
- Broker for efficient notification.
- Worker for execution.

You don't need Kafka or RabbitMQ from day one.

PostgreSQL polling is a perfectly reasonable starting point.

# 17. Database indexes for the execution engine

As the number of steps grows, we need to make claims efficient.

We should not scan millions of historical SUCCEEDED records every time a worker looks for work.

For PostgreSQL, we can use partial indexes.

```
CREATE INDEX idx_steps_ready
ON saga_steps (created_at, id)
WHERE forward_status = 'READY';
```

For retries:

```
CREATE INDEX idx_steps_retry
ON saga_steps (next_attempt_at, id)
WHERE forward_status = 'RETRY_WAIT';
```

For expired leases:

```
CREATE INDEX idx_steps_expired_lease
ON saga_steps (lease_until, id)
WHERE forward_status = 'RUNNING';
```

These indexes help the database locate the relevant categories of work without indexing every possible historical state in the same way.

For a high-throughput system, you should inspect the actual query plans with `EXPLAIN (ANALYZE, BUFFERS)`.

Also, be aware that `SKIP LOCKED` does not guarantee perfectly fair FIFO scheduling. A locked older row can be skipped while newer rows are processed.

If fairness matters, introduce explicit scheduling priorities, aging, tenant fairness and monitoring for the oldest eligible task.

# 18. One important refinement to our database model

In Chapter 2, we modeled a step like this:

```
SAGA_STEP

forward_status
compensation_status
```

That is a useful conceptual model.

But now that we have built the execution engine, there's a design question.

Suppose the forward operation and compensation operation have different:

- Execution attempts.
- Idempotency keys.
- Leases.
- Retry schedules.
- Request payloads.
- Responses.

Should we keep squeezing all this into one `saga_steps` row?

For a simple sequential implementation, we can use shared ownership fields to ensure that only one action is active at a time.

But for a more extensible system, I prefer introducing a separate table:

`saga_actions`

Think about it.

A forward operation is executable work.

A compensation operation is also executable work.

Instead of treating them as two special status columns, represent each as an individual action.

Conceptually:

```
SAGA-1001

STEP: RESERVE_INVENTORY

    ACTION 1: FORWARD
        status = SUCCEEDED

    ACTION 2: COMPENSATE
        status = READY
```

Both actions can have the same execution infrastructure.

An illustrative schema:

```
CREATE TABLE saga_actions (
    id UUID PRIMARY KEY,

    saga_id UUID NOT NULL,
    step_id UUID NOT NULL,

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

    UNIQUE (step_id, action_type)
);
```

Now both forward execution and compensation execution use the same worker infrastructure:

```
                  saga_actions
                       |
              +--------+--------+
              |                 |
              v                 v
         FORWARD ACTION   COMPENSATE ACTION
              |                 |
              +--------+--------+
                       |
                       v
                Worker Engine
                       |
                       v
               Participant APIs
```

This is an implementation refinement, not a change in the Saga pattern itself.

For a mature orchestration engine, this separation is attractive because every executable operation follows the same claim/lease/attempt/result lifecycle.

It also avoids accidentally sharing the same lease or attempt counters between forward and compensation operations.

# 19. What happens during deployments and graceful shutdown?

Imagine you're deploying a new version of the orchestrator.

There are 20 workers currently executing steps.

You should not suddenly terminate all their work without considering ownership.

A graceful shutdown should:

1. Stop claiming new tasks.
2. Allow active operations to finish within a configured shutdown window.
3. Complete and persist successful results.
4. Stop renewing leases for work that is no longer being executed.
5. Leave unfinished or uncertain operations recoverable through lease expiry.

One important rule:

Don't blindly reset every RUNNING step to READY during shutdown.

An HTTP request might already have reached the participant and succeeded.

Let the lease and idempotency mechanisms handle recovery.

# 20. Our complete worker architecture

Now let's put the entire chapter together.

\#chatgpt-mermaid-\_r_44j\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_44j\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_44j\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_44j\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_44j\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44j\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_44j\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_44j\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_44j\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_44j\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_44j\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_44j\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_44j\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_44j\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_44j\_ p{margin:0;}#chatgpt-mermaid-\_r_44j\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44j\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44j\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44j\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_44j\_ .label text,#chatgpt-mermaid-\_r_44j\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44j\_ .node rect,#chatgpt-mermaid-\_r_44j\_ .node circle,#chatgpt-mermaid-\_r_44j\_ .node ellipse,#chatgpt-mermaid-\_r_44j\_ .node polygon,#chatgpt-mermaid-\_r_44j\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_44j\_ .rough-node .label text,#chatgpt-mermaid-\_r_44j\_ .node .label text,#chatgpt-mermaid-\_r_44j\_ .image-shape .label,#chatgpt-mermaid-\_r_44j\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_44j\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_44j\_ .rough-node .label,#chatgpt-mermaid-\_r_44j\_ .node .label,#chatgpt-mermaid-\_r_44j\_ .image-shape .label,#chatgpt-mermaid-\_r_44j\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_44j\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_44j\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_44j\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_44j\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_44j\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_44j\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_44j\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_44j\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_44j\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_44j\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_44j\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44j\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44j\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_44j\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44j\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_44j\_ .icon-shape,#chatgpt-mermaid-\_r_44j\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_44j\_ .icon-shape p,#chatgpt-mermaid-\_r_44j\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_44j\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_44j\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_44j\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_44j\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_44j\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_44j\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_44j\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_44j\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_44j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_44j\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_44j\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_44j\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_44j\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_44j\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_44j\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_44j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_44j\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_44j\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_44j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_44j\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_44j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_44j\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_44j\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_44j\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_44j\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_44j\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_44j\_ .node rect,#chatgpt-mermaid-\_r_44j\_ .node circle,#chatgpt-mermaid-\_r_44j\_ .node ellipse,#chatgpt-mermaid-\_r_44j\_ .node polygon,#chatgpt-mermaid-\_r_44j\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_44j\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_44j\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_44j\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_44j\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_44j\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Orchestrator InstancesInstance BInstance AWorker B1Worker B2Worker A1Worker A2PostgreSQLSaga ActionsOrder ServiceInventory ServicePayment ServiceShipping ServiceAtomic claimAtomic claimAtomic claimAtomic claimHTTP + stable idempotency keyResultResultResultResultFenced state updateFenced state updateFenced state updateFenced state update

Notice that:

- Every worker uses the same database.
- Every worker claims work atomically.
- Only the current owner can update the workflow state.
- Every worker can recover work abandoned by another worker.
- No worker is essential to the existence of a saga.
- Any worker instance can execute the next eligible action.

This is the foundation of a horizontally scalable execution engine.

# 21. The five concepts you should never confuse

Let's summarize the important distinctions.

| Concept                  | What it solves                                                                   |
| ------------------------ | -------------------------------------------------------------------------------- |
| `FOR UPDATE SKIP LOCKED` | Prevents concurrent transactions from claiming the same available row            |
| RUNNING status           | Records that an action has been claimed                                          |
| Lease                    | Allows another worker to recover abandoned work after ownership expires          |
| Fencing token            | Prevents an old worker from modifying state after a newer worker takes ownership |
| Idempotency              | Prevents repeated execution attempts from creating duplicate business effects    |

These mechanisms complement each other. None replaces the others.

The most important mental model is:

A worker does not own a saga permanently. It temporarily owns an execution attempt.

The saga and its actions belong to the database.

Workers are replaceable.

If Worker A crashes, Worker B continues.

If Worker B crashes, Worker C continues.

If the orchestrator instance crashes, another instance continues.

The workflow does not depend on the lifetime of any individual process.

## Chapter 3 — What we have achieved

We now have a worker engine that supports:

- Concurrent execution across multiple instances.
- Atomic work claiming.
- Lease-based crash recovery.
- Lease renewal for long-running operations.
- Fencing against stale workers.
- Short database transactions.
- Independent HTTP execution.
- Durable retry scheduling.
- Bounded concurrency and backpressure.
- A common execution mechanism for forward and compensation actions.

One limitation remains deliberately unsolved:

Even with all these mechanisms, the same participant API can receive the same logical operation multiple times.

Our orchestrator can prevent duplicate workers from simultaneously owning the same current database state, but it cannot guarantee that only one HTTP request reaches the participant.

That's the next major challenge.

# Next Chapter — Chapter 4: Idempotency and Duplicate Handling

This is where we'll solve one of the hardest problems in the entire Saga architecture.

We'll build the participant service itself.

Consider this exact scenario:

```
Worker A
    |
    | POST /payments
    | Idempotency-Key: SAGA-1001-CHARGE_PAYMENT
    |
    v
Payment Service
    |
    | Deducts ₹2,500
    | Commits database transaction
    |
    v
HTTP response
    |
    X Network failure
```

The orchestrator doesn't know whether the payment happened.

It retries.

```
Worker B
    |
    | POST /payments
    | Same Idempotency-Key
    |
    v
Payment Service
```

How does Payment Service know not to deduct ₹2,500 again?

We'll build this from the database level.

Specifically:

1. Why checking idempotency in Java before starting a transaction is insufficient.
2. How to create an idempotency table in every participant service.
3. How to atomically store the idempotency key and perform the business operation.
4. What happens when two identical requests arrive simultaneously.
5. How to store and return the original response.
6. What happens when a request is still PROCESSING.
7. How to handle failures and transaction rollbacks.
8. Why the orchestrator's key must remain stable across all attempts.
9. How idempotency works for payment, inventory, refunds and compensation.

This chapter will connect the orchestrator's reliability mechanisms with the reliability of the individual microservices.
