# Chapter 8 — Choosing the Right Database and Infrastructure

We've now designed almost the entire Saga Orchestrator.

We know how to:

- Persist workflows and their steps.
- Execute work using multiple workers.
- Claim actions safely.
- Recover from crashes.
- Prevent duplicate business effects.
- Retry failed operations.
- Reconcile unknown outcomes.
- Execute compensation.
- Scale across multiple instances, availability zones and database shards.

Now comes an important architectural decision:

Which database should we use to implement this coordination service?

We shouldn't choose a database simply because it is popular, fast, or marketed as highly scalable.

We should derive the choice from the guarantees our orchestrator needs.

## 1. First, derive the database requirements

Forget database names for a moment.

Imagine we have this workflow record:

```
SAGA-1001

CREATE_ORDER        SUCCEEDED
RESERVE_INVENTORY   SUCCEEDED
CHARGE_PAYMENT      READY
CREATE_SHIPMENT     BLOCKED
```

Our orchestrator must support several operations.

### Requirement 1: Atomic state transitions

When a worker completes a step, we want:

```
1. Mark current action SUCCEEDED.
2. Store participant response.
3. Activate next action.
4. Insert outbox event.
```

All these changes must commit together.

Otherwise, we might get:

```
CHARGE_PAYMENT = SUCCEEDED
CREATE_SHIPMENT = BLOCKED
```

with no reliable mechanism to activate shipment.

We need transactions covering multiple related records.

### Requirement 2: Concurrent work claiming

Imagine 100 workers attempt to claim available actions simultaneously.

We need a database mechanism that prevents two workers from acquiring the same current ownership.

We also need:

- Atomic conditional updates.
- Concurrency control.
- Leases.
- Fencing tokens.
- Unique constraints.

### Requirement 3: Durable retry scheduling

We need to efficiently find:

```
READY actions
Due RETRY_WAIT actions
Expired RUNNING leases
```

The database should support efficient queries against these changing states.

### Requirement 4: Idempotency

We need database-enforced uniqueness for logical operation identities.

For example:

```
tenant_id + operation_name + idempotency_key
```

Two simultaneous requests must not create two independent business effects.

### Requirement 5: Durable execution history

We need to store:

- Saga execution.
- Action status.
- Attempt history.
- Error information.
- Compensation status.
- Outbox events.
- Idempotency records.

### Requirement 6: High availability

We need:

- Replication.
- Failover.
- A single authoritative writer for a saga.
- Well-understood durability guarantees.

Now the selection becomes easier.

| Requirement                 | Important database capability            |
| --------------------------- | ---------------------------------------- |
| Atomic workflow transitions | Transactions                             |
| Concurrent claiming         | Row locks or conditional writes          |
| Duplicate protection        | Unique constraints                       |
| Retry scheduling            | Efficient indexed queries                |
| Large history               | Partitioning and archival                |
| Crash recovery              | Durable commits                          |
| HA                          | Replication and failover                 |
| Distributed scaling         | Sharding or distributed SQL, when needed |

The important observation:

Our orchestrator is not simply a high-throughput key-value store. It is a transactional state machine with a work-scheduling responsibility.

That strongly influences the database choice.

# 2. PostgreSQL — My default recommendation

PostgreSQL

For the architecture we've designed, PostgreSQL would be my first choice.

Why?

Because almost every important requirement maps naturally to an existing PostgreSQL capability.

| Our requirement              | PostgreSQL capability                 |
| ---------------------------- | ------------------------------------- |
| Atomic state changes         | ACID transactions                     |
| Prevent concurrent claiming  | `SELECT FOR UPDATE`                   |
| Skip work claimed by others  | `SKIP LOCKED`                         |
| Duplicate protection         | Unique constraints                    |
| Safe conditional updates     | `UPDATE ... WHERE ... RETURNING`      |
| Retry scheduling             | B-tree and partial indexes            |
| Flexible workflow metadata   | JSONB                                 |
| Efficient history management | Table partitioning                    |
| Reliable event publishing    | Transactional Outbox                  |
| HA                           | Streaming and synchronous replication |

Let's connect these to our actual implementation.

### A. Atomic workflow transitions

Remember this transaction?

```
BEGIN;

UPDATE saga_actions
SET status = 'SUCCEEDED'
WHERE id = :action_id
  AND status = 'RUNNING'
  AND claim_generation = :generation;

-- Verify exactly one row updated.

UPDATE saga_actions
SET status = 'READY'
WHERE id = :next_action_id
  AND status = 'BLOCKED';

INSERT INTO outbox_events (...);

COMMIT;
```

PostgreSQL handles this naturally.

If anything fails before commit, the whole transaction rolls back.

We don't need to introduce a separate distributed transaction coordinator to keep these records consistent.

### B. Concurrent worker claiming

We already implemented:

```
SELECT id
FROM saga_actions
WHERE status = 'READY'
ORDER BY created_at
FOR UPDATE SKIP LOCKED
LIMIT 10;
```

This is a particularly convenient mechanism for multiple database-backed queue consumers.

Workers can claim different rows without unnecessarily blocking one another.

### C. Unique idempotency constraints

```
CREATE TABLE idempotency_records (
    tenant_id VARCHAR(100) NOT NULL,
    operation_name VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,

    request_hash VARCHAR(64) NOT NULL,

    status VARCHAR(30) NOT NULL,

    response_body JSONB,

    PRIMARY KEY (
        tenant_id,
        operation_name,
        idempotency_key
    )
);
```

The database itself protects the uniqueness rule.

We aren't depending on application-level synchronization.

### D. Flexible workflow data

JSONB is useful for storing:

```
{
  "orderId": "ORDER-123",
  "customerId": "CUSTOMER-456",
  "amount": 2500,
  "currency": "INR"
}
```

But I would not store everything as JSON.

Keep fields used for:

- State transitions.
- Ownership.
- Retry eligibility.
- Idempotency.
- Relationships.

as properly typed relational columns.

Use JSONB for flexible payloads and execution context.

This gives us the advantages of both structured data and flexible workflow metadata.

### E. Large execution history

PostgreSQL partitioning helps manage large attempt-history and outbox tables.

For example:

```
saga_step_attempts

attempts_2026_09
attempts_2026_10
attempts_2026_11
```

You can archive old partitions without continuously deleting millions of individual rows.

PostgreSQL officially documents all these capabilities, including transactions, row locking, partial indexes, JSONB and declarative partitioning.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.postgresql.org\&sz=32)

PostgreSQL+3



### Where PostgreSQL becomes challenging

It isn't unlimited.

Potential bottlenecks include:

- A single primary's write capacity.
- High contention on heavily updated rows.
- Too many concurrent database connections.
- Large indexes.
- WAL generation and replication lag.
- Hot partitions or hot tenants.
- Cross-region database latency.

But these are reasons to optimize, partition, or eventually shard.

They aren't reasons to reject PostgreSQL at the beginning.

My recommendation: Start with PostgreSQL, preferably using one authoritative primary per Saga database shard.

# 3. MySQL — A perfectly reasonable alternative

MySQL with InnoDB is also a viable choice.

It supports:

- ACID transactions.
- Row-level locking.
- Unique constraints.
- Conditional updates.
- Locking reads using `FOR UPDATE`.
- `SKIP LOCKED`.
- Replication and high availability.

In fact, MySQL 8.4 explicitly supports `SKIP LOCKED` for queue-like workloads.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://dev.mysql.com\&sz=32)

MySQL+2



So we can implement our worker claiming mechanism in MySQL too.

Conceptually:

```
START TRANSACTION;

SELECT id
FROM saga_actions
WHERE status = 'READY'
ORDER BY created_at
LIMIT 10
FOR UPDATE SKIP LOCKED;

-- Update the selected actions to RUNNING.
-- Assign worker ownership and fencing generation.

COMMIT;
```

The same architecture applies.

### PostgreSQL vs MySQL for our workflow

| Requirement                  | PostgreSQL | MySQL InnoDB                          |
| ---------------------------- | ---------- | ------------------------------------- |
| ACID transactions            | Yes        | Yes                                   |
| Row-level locking            | Yes        | Yes                                   |
| `SKIP LOCKED`                | Yes        | Yes                                   |
| Unique constraints           | Yes        | Yes                                   |
| JSON support                 | JSONB      | JSON                                  |
| Partial indexes              | Yes        | No equivalent general partial indexes |
| Table partitioning           | Yes        | Yes                                   |
| Relational workflow modeling | Excellent  | Excellent                             |
| Database-backed workers      | Excellent  | Excellent                             |

One difference worth understanding is indexing.

PostgreSQL supports partial indexes such as:

```
CREATE INDEX idx_ready_actions
ON saga_actions (created_at)
WHERE status = 'READY';
```

This lets us create an index containing only eligible records.

In MySQL, you generally design composite indexes around the query predicates and ordering instead.

For example:

```
CREATE INDEX idx_actions_status_created
ON saga_actions (status, created_at, id);
```

### Which would I choose?

For a new standalone orchestration service, I would lean toward PostgreSQL.

But I would not reject MySQL if:

- Your organization already operates MySQL reliably.
- Your engineering team understands InnoDB well.
- Your infrastructure and backup practices are already mature.
- The workload fits its transaction and indexing model.

There is no reason to introduce a new database technology merely because PostgreSQL has some convenient features.

Both PostgreSQL and MySQL can implement this architecture correctly.

# 4. DynamoDB — Can a Saga Orchestrator use NoSQL?

Amazon DynamoDB is an interesting alternative.

At first, it might seem like a better choice for an orchestrator.

Why?

Our workload looks like:

```
Get Saga
Get Action

Update Action
Update Saga

Find Ready Actions

Find Due Retries
```

These are mostly structured reads and writes.

DynamoDB provides:

- Conditional writes.
- Atomic updates to individual items.
- Transactions across multiple items.
- Horizontal partitioning.
- Managed availability and scaling.

But we need to understand how to redesign the database model around DynamoDB.

We cannot simply take our PostgreSQL tables, convert them into DynamoDB tables and expect the same architecture to work.

## 4.1 Replace relational transactions with conditional transactional writes

Remember our PostgreSQL code?

```
UPDATE saga_actions
SET status = 'RUNNING'
WHERE id = :action_id
  AND status = 'READY';
```

In DynamoDB, we can use a conditional update.

Conceptually:

```
UpdateItem:

Key:
    actionId = ACTION-1001

Condition:
    status = READY

Update:
    status = RUNNING
    leaseOwner = worker-A
    leaseUntil = ...
    generation = generation + 1
```

The update succeeds only if the condition is satisfied.

If two workers attempt this simultaneously, only the update that satisfies the condition against the current item state can succeed.

This is how we implement conditional ownership.

DynamoDB supports condition expressions for `PutItem`, `UpdateItem` and `DeleteItem`.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon DynamoDB+1



## 4.2 Replace multiple SQL statements with TransactWriteItems

Suppose an action succeeds.

We need:

```
ACTION-1 = SUCCEEDED
ACTION-2 = READY
SAGA = RUNNING
OUTBOX EVENT = CREATED
```

DynamoDB provides `TransactWriteItems`.

Conceptually:

```
TransactWriteItems:

1. Update ACTION-1
   Condition: Current generation = 4
              Current owner = worker-A

2. Update ACTION-2
   Condition: Status = BLOCKED

3. Update SAGA
   Condition: Status = RUNNING

4. Put OUTBOX EVENT
   Condition: Event doesn't already exist
```

These operations commit atomically or fail together.

So DynamoDB can support the atomic workflow transition we need.

However, there are important transaction limits.

AWS currently documents a maximum of 100 distinct items and 4 MB per `TransactWriteItems` transaction. Transactions operate within one AWS Region.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon DynamoDB+2



For ordinary Saga transitions involving a few records, this is generally sufficient.

## 4.3 The biggest challenge: Finding READY actions

In PostgreSQL, we can simply query:

```
SELECT *
FROM saga_actions
WHERE status = 'READY';
```

With an appropriate index.

DynamoDB works differently.

You design your keys around your access patterns.

For example, we might maintain an action scheduling index:

```
Partition Key: READY#BUCKET-07
Sort Key:      2026-11-10T10:35:00#ACTION-1001
```

And another entry for:

```
Partition Key: RETRY#BUCKET-07
Sort Key:      2026-11-10T10:40:00#ACTION-1002
```

Workers query the relevant scheduling partitions.

We might create time buckets or hash buckets to distribute work.

Why?

Because putting every READY action under one partition key creates a potential hot partition.

We need to distribute scheduling entries while maintaining efficient discovery.

Now there is another subtle issue.

Suppose we query a Global Secondary Index to find READY actions.

DynamoDB Global Secondary Index reads are eventually consistent.

So the scheduler might see an old entry.

That's okay only if the actual claim is a conditional update against the authoritative action item.

The GSI helps discover candidate work.

It does not grant ownership.

DynamoDB documents that GSI queries are eventually consistent, whereas strongly consistent reads are available for base tables and local secondary indexes.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon DynamoDB+1



## 4.4 Can DynamoDB handle idempotency?

Yes.

For example:

```
PK = TENANT#A
SK = IDEMPOTENCY#KEY-123
```

Create the record using:

```
ConditionExpression:

attribute_not_exists(PK)
```

Only one request can successfully create that key.

You can combine idempotency registration and business-state updates through a DynamoDB transaction.

However, be mindful of the transaction client token.

DynamoDB's native `TransactWriteItems` client token has a limited idempotency window of 10 minutes according to its API documentation.

That is not sufficient as your only business idempotency mechanism for Saga recovery that might happen days later.

You still need your own durable idempotency record.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon DynamoDB



### DynamoDB's strengths and weaknesses

| Strengths                     | Challenges                                     |
| ----------------------------- | ---------------------------------------------- |
| Managed scaling               | Access patterns must be designed in advance    |
| Conditional writes            | No general SQL-style arbitrary filtering       |
| Multi-item transactions       | Transaction limits                             |
| High-volume key-based access  | Scheduling indexes need careful design         |
| Managed replication options   | Cross-region transactional semantics need care |
| No database server management | Hot partition management                       |

One particularly important multi-region detail:

With DynamoDB Global Tables, transactional operations provide their atomic guarantees within the originating Region, not as an atomic multi-item transaction replicated across all Regions.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon DynamoDB+1



### My verdict on DynamoDB

DynamoDB is a good choice when:

- You are already building an AWS-native architecture.
- Your access patterns are predictable.
- You need managed, horizontally scalable key-based storage.
- Your workflow can be modeled with item-level conditional writes and bounded transactions.
- Your team understands partition-key and index design.

But for the generic Saga Orchestrator we have designed, PostgreSQL is simpler.

DynamoDB is viable, but it requires a different scheduling and data-modeling approach.

# 5. MongoDB — Is a document database suitable?

MongoDB can implement a Saga Orchestrator.

But its suitability depends on how we model the workflow.

MongoDB has an interesting feature:

A single document update is atomic.

It also supports multi-document ACID transactions across documents and collections, including sharded clusters. However, multi-document transactions have additional performance costs compared with single-document operations.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.mongodb.com\&sz=32)

MongoDB Docs+2



Let's explore two designs.

## 5.1 Design A: Store the whole Saga inside one document

For example:

```
{
  "_id": "SAGA-1001",
  "sagaType": "ORDER_CHECKOUT",
  "status": "RUNNING",
  "steps": [
    {
      "key": "CREATE_ORDER",
      "status": "SUCCEEDED"
    },
    {
      "key": "RESERVE_INVENTORY",
      "status": "READY"
    },
    {
      "key": "CHARGE_PAYMENT",
      "status": "BLOCKED"
    }
  ]
}
```

This has an advantage.

A single document update can atomically change the Saga's state.

You can use atomic conditional updates to claim or transition the workflow.

But consider a Saga with 500 actions.

Multiple workers might need to update different embedded action records inside the same document.

They now compete to update the same document.

We have recreated the parent-row contention problem from Chapter 7.

Also, the document grows as execution history accumulates.

MongoDB has a 16 MiB BSON document size limit.

A single large Saga document is therefore not a great design for unlimited workflow history.

## 5.2 Design B: Separate documents for Saga, actions and attempts

We could instead use:

```
sagas collection

{
    _id: SAGA-1001,
    status: RUNNING
}
```

```
saga_actions collection

{
    _id: ACTION-1001,
    sagaId: SAGA-1001,
    status: READY,
    generation: 0
}
```

```
saga_attempts collection

{
    _id: ATTEMPT-1001,
    actionId: ACTION-1001,
    outcome: TIMEOUT
}
```

This avoids updating one massive Saga document.

We can create unique indexes and use conditional updates.

But now, when an action completes, we might need to atomically update multiple documents:

```
Action 1 = SUCCEEDED

Action 2 = READY

Saga metadata updated

Outbox event created
```

That requires a multi-document transaction.

MongoDB supports it, but it introduces additional transaction overhead and operational considerations.

### When would I choose MongoDB?

If:

- The workflow is naturally document-centric.
- Most state transitions can be expressed as single-document updates.
- The application already runs on MongoDB.
- The team understands its transaction and consistency model.
- The workload has suitable access patterns.

I wouldn't choose MongoDB over PostgreSQL merely because a Saga contains JSON payloads.

PostgreSQL JSONB already handles flexible workflow metadata while preserving relational transactions and indexes.

MongoDB is viable, but PostgreSQL is my preference for this particular coordination workload.

# 6. Cassandra — Why a highly scalable distributed database isn't automatically suitable

Now consider a very different database.

Apache Cassandra is designed for horizontally distributed workloads.

It offers:

- High write throughput.
- Partition-based data distribution.
- Tunable consistency.
- Replication.
- Lightweight transactions for conditional updates.

So why not use it?

Because we need to look beyond throughput.

Our orchestrator frequently performs operations like:

```
1. Check action state.
2. Claim action if still READY.
3. Update action state.
4. Activate dependent action.
5. Update Saga state.
6. Store an event.
```

These operations naturally span related records.

Cassandra's data model and transaction semantics are different.

## 6.1 How conditional claiming would work

Cassandra supports conditional writes, such as:

```
INSERT INTO saga_actions (...)
VALUES (...)
IF NOT EXISTS;
```

And:

```
UPDATE saga_actions
SET status = 'RUNNING'
WHERE saga_id = ?
  AND action_id = ?
IF status = 'READY';
```

These are called Lightweight Transactions (LWT).

They use Paxos to provide linearizable conditional updates.

This can be used to implement compare-and-set style ownership.

But LWT carries a non-negligible performance cost and Cassandra's documentation recommends using it selectively.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://cassandra.apache.org\&sz=32)

Apache Cassandra Documentation+2



## 6.2 The real challenge: Cross-record workflow transitions

Suppose:

```
ACTION-1 = SUCCEEDED
ACTION-2 = READY
SAGA = RUNNING
OUTBOX EVENT = CREATED
```

We want these changes to be atomic.

Cassandra provides logged batches and lightweight transaction capabilities, but they are not a general substitute for arbitrary relational multi-row transactions.

Its newer transaction capabilities and version-specific features should also be evaluated separately rather than assuming PostgreSQL semantics.

You must design the data model around Cassandra's partition boundaries and supported atomicity guarantees.

This is a substantial change to our orchestration architecture.

### When would Cassandra make sense?

Possibly when:

- You have extremely high write volumes.
- Your workflow state is naturally partitioned by Saga.
- You can keep the critical state transitions within supported partition-level transaction boundaries.
- You can tolerate a more specialized scheduling architecture.
- Your team is experienced with Cassandra's consistency and partitioning model.

But it would not be my first choice for a general-purpose Saga coordination database.

High write scalability does not automatically mean high suitability for workflow coordination.

# 7. CockroachDB — What about distributed SQL?

This is an interesting alternative.

CockroachDB provides SQL, ACID transactions and distributed data replication.

It is particularly interesting when you want SQL-style data modeling while distributing data across database nodes and potentially regions.

For our workflow:

```
UPDATE action
UPDATE next action
UPDATE saga
INSERT outbox
```

We can use a transaction.

CockroachDB provides SERIALIZABLE transaction isolation.

However, this comes with an important implementation requirement:

Your application must handle transaction retries.

When concurrent transactions conflict, CockroachDB may return a retryable serialization error.

The application may need to rerun the entire transaction.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.cockroachlabs.com\&sz=32)

cockroachlabs.com+2



### Why does this matter to our worker?

Imagine:

```
Transaction:

1. Mark ACTION-1 successful.
2. Make ACTION-2 ready.
3. Insert outbox event.
```

The transaction encounters a serialization conflict.

We must retry the entire database transaction.

But here's the important distinction:

We should only retry the database completion transaction.

We must not repeat the participant HTTP call simply because the database transaction encountered a serialization error.

The participant might already have completed the operation successfully.

Correct design:

```
Participant HTTP call
        |
        v
Receive success
        |
        v
Database transaction fails with retryable serialization error
        |
        v
Retry database transaction
        |
        v
Commit result
```

Do not:

```
Database transaction fails
        |
        v
Call participant again
```

This is a subtle but very important implementation detail.

Also, if a database operation has an ambiguous commit result, use stable action IDs and idempotent conditional database operations to determine whether the state transition actually committed.

### When would I choose CockroachDB?

When:

- Distributed SQL is an important requirement.
- You want SQL transactions across distributed nodes.
- Cross-region resilience or data locality is part of the architecture.
- You can accommodate transaction retry semantics.
- Your team understands distributed database operations and their latency implications.

However, distributed SQL does not make every transaction cheap.

A transaction that requires coordination across distant regions can have considerable latency.

For our initial design, I would still choose PostgreSQL.

For an orchestrator whose database genuinely needs distributed SQL capabilities, CockroachDB is a serious option.

# 8. Redis — Can we store Saga state in Redis?

Redis is extremely fast.

It supports:

- Atomic commands.
- Lua scripting.
- Transactions.
- Streams.
- Consumer groups.
- Persistence through RDB and AOF.

Redis Streams even provides pending-entry tracking for consumer groups, which can help with worker delivery and recovery.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs+1



So why not use it as our only orchestration database?

Let's understand the issue.

## 8.1 Redis is very useful for dispatching work

Imagine:

```
Saga Database
      |
      v
Redis Stream
      |
      v
Worker Consumer Groups
```

We can use Redis Streams to notify workers about available actions.

For example:

```
XADD saga-actions * \
    actionId ACTION-1001 \
    sagaId SAGA-1001
```

Workers consume using consumer groups.

They can acknowledge completed notifications.

Redis maintains information about messages that have been delivered but not acknowledged.

This is useful for queue-like workloads.

## 8.2 But should Redis be the source of truth?

Remember our requirements.

We need to persist:

```
Saga state
Action state
Execution attempts
Compensation history
Idempotency records
Retry schedules
Workflow definition versions
Audit history
```

Redis can persist data.

But its durability depends on configuration.

For example:

| Persistence configuration | Behavior                                         |
| ------------------------- | ------------------------------------------------ |
| No persistence            | Data is not recovered from disk                  |
| RDB                       | Periodic snapshots                               |
| AOF with `everysec`       | Writes are fsynced approximately once per second |
| AOF with `always`         | Stronger durability at increased latency cost    |
| RDB + AOF                 | Combined snapshot and write-log approach         |

Redis documents that AOF with `everysec` can lose approximately one second of recent writes during certain failures, while `always` provides stronger persistence at a performance cost.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs



For a generic Saga Orchestrator, I prefer not to make the execution state dependent on a Redis persistence configuration when a relational database can provide the transaction model we need more naturally.

### Where Redis fits beautifully

```
PostgreSQL:
    Authoritative Saga state.

Redis:
    Caching.
    Fast notification.
    Optional rate limiting.
    Short-lived scheduling hints.
```

You can absolutely build a reliable Redis-backed system with suitable persistence and replication configurations.

But Redis would not be my default choice for the sole authoritative store of a business workflow that may need recovery days or weeks later.

Use Redis to accelerate execution, not as an automatic replacement for durable workflow storage.

# 9. Kafka — Can Kafka itself become our Saga database?

This is an important architectural distinction.

Apache Kafka is not a traditional relational database.

It is a distributed event-streaming platform.

It excels at:

- High-throughput event delivery.
- Partitioned processing.
- Event retention.
- Replay.
- Consumer groups.
- Event-driven architectures.

One might consider designing the entire orchestrator around an event log.

For example:

```
SagaStarted
ActionClaimed
PaymentSucceeded
InventoryReserved
ShipmentFailed
CompensationStarted
PaymentRefunded
InventoryReleased
SagaCompensated
```

The current Saga state is then derived by replaying events.

This is Event Sourcing.

It's a legitimate architecture.

But it is a different design from the database-backed state machine we've been building.

## 9.1 Database-backed state machine

Our current architecture:

```
PostgreSQL

SAGAS
SAGA_ACTIONS
SAGA_ATTEMPTS
OUTBOX_EVENTS
```

We update current state:

```
CHARGE_PAYMENT = SUCCEEDED
```

We also record execution history.

The worker queries the current state and claims eligible work.

## 9.2 Event-sourced architecture

An event-sourced architecture might store:

```
SAGA-1001 EVENT STREAM

1. SagaStarted
2. CreateOrderSucceeded
3. InventoryReservationSucceeded
4. PaymentChargeRequested
5. PaymentChargeSucceeded
6. ShipmentCreationRequested
```

The current state is derived by applying events.

Conceptually:

```
SagaState state = initialState();

for (SagaEvent event : eventStream) {
    state = apply(state, event);
}
```

The event stream is the authoritative history.

A materialized view or projection can provide the current state for efficient queries.

### Advantages

- Complete business history.
- Natural auditability.
- Replay and debugging.
- Event-driven integration.
- Ability to build multiple projections.

### Challenges

- Event schema evolution.
- Projection rebuilding.
- Duplicate event handling.
- Event ordering.
- Event versioning.
- Snapshot management.
- More complicated debugging and operational tooling.

Most importantly, the event log alone does not magically solve work claiming or participant idempotency.

You still need a reliable way to coordinate execution attempts.

Kafka's own documentation distinguishes at-most-once, at-least-once and exactly-once semantics, and warns that exactly-once claims need to be understood within their actual boundaries. External systems still need appropriate cooperation for reliable side effects.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://kafka.apache.org\&sz=32)

Apache Kafka



### My recommendation

I would not choose full event sourcing simply because we need a Saga Orchestrator.

I would start with:

```
PostgreSQL:
    Current workflow state.

Execution history:
    Durable attempts and transition records.

Transactional Outbox:
    Reliable event publication.

Kafka:
    High-throughput notifications and integrations.
```

If there is a strong business requirement for complete event replay, an audit log, or multiple independent projections, consider event sourcing as an intentional architectural choice.

Don't accidentally create a partial event-sourced system without understanding its operational consequences.

# 10. Complete database comparison — Which one actually fits?

Let's put all the databases together.

| Database     | Transaction model                                                 | Concurrent work claiming                       | Workflow scheduling                               | My assessment                       |
| ------------ | ----------------------------------------------------------------- | ---------------------------------------------- | ------------------------------------------------- | ----------------------------------- |
| PostgreSQL   | Relational ACID                                                   | Excellent with row locks and `SKIP LOCKED`     | Excellent with indexes                            | Best default                        |
| MySQL InnoDB | Relational ACID                                                   | Excellent with locking reads and `SKIP LOCKED` | Good with suitable indexes                        | Excellent alternative               |
| DynamoDB     | Conditional and multi-item transactions                           | Conditional updates                            | Requires access-pattern-driven scheduling indexes | Good AWS-native alternative         |
| MongoDB      | Document atomicity + multi-document transactions                  | Conditional document updates                   | Requires additional scheduling design             | Viable                              |
| Cassandra    | Partition-level atomicity, LWT and newer transaction capabilities | Conditional operations                         | More specialized                                  | Not my default                      |
| CockroachDB  | Distributed SQL ACID                                              | Transactional concurrency control              | Good, with transaction-retry handling             | Good distributed SQL option         |
| Redis        | Atomic commands and scripts; configurable persistence             | Possible through atomic operations             | Excellent for fast queueing                       | Better as supporting infrastructure |
| Kafka        | Durable event log and transactions within defined boundaries      | Consumer-group coordination                    | Event-driven dispatch                             | Not a traditional workflow database |

A few important distinctions:

- MySQL is not significantly inferior to PostgreSQL for basic transactional orchestration.
- DynamoDB is not inherently unsuitable; it requires designing around partition keys, conditional operations and transactional limits.
- MongoDB can support this workflow, but multi-document atomicity must be designed deliberately.
- Cassandra's scalability doesn't compensate for the extra complexity of coordinating multi-record workflow transitions.
- Redis is useful for dispatch and caching, but persistence configuration matters if it is authoritative.
- Kafka is a messaging and event-streaming platform, not a direct replacement for our relational workflow model.

Relevant official documentation:

- [PostgreSQL locking reads](https://www.postgresql.org/docs/14/sql-select.html?utm_source=chatgpt.com).
- [MySQL InnoDB locking reads](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html?trk=article-ssr-frontend-pulse_little-text-block\&utm_source=chatgpt.com).
- [DynamoDB transactions](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/transactions.html?utm_source=chatgpt.com).
- [MongoDB transactions](https://www.mongodb.com/docs/manual/core/transactions/?locale_fallback=de-de\&utm_source=chatgpt.com).
- [Cassandra conditional operations](https://cassandra.apache.org/doc/latest/cassandra/developing/cql/dml.html?utm_source=chatgpt.com).
- [CockroachDB transactions](https://www.cockroachlabs.com/docs/stable/developer-basics.html?utm_source=chatgpt.com).
- [Redis persistence](https://redis.io/docs/latest/management/persistence/?utm_source=chatgpt.com).
- [Kafka delivery semantics](https://kafka.apache.org/32/design/design/?utm_source=chatgpt.com).

# 11. The infrastructure I would actually build

Now let's stop comparing databases and design the system we would deploy.

Assume we are building a general-purpose Saga Orchestration service using Java and Spring Boot.

Here is what I would choose.

## 11.1 Initial production architecture

```
                Calling Services
                       |
                       v
                Load Balancer
                       |
             +---------+---------+
             |         |         |
             v         v         v
           API-1     API-2     API-3
             |         |         |
             +---------+---------+
                       |
                       v
              PostgreSQL Primary
                       |
          +------------+------------+
          |            |            |
          v            v            v
       Worker-1     Worker-2     Worker-N
          |            |            |
          +------------+------------+
                       |
                       v
              Participant Services
```

Technology choices:

| Component                | Technology                                           |
| ------------------------ | ---------------------------------------------------- |
| API                      | Java + Spring Boot                                   |
| Persistence              | PostgreSQL                                           |
| ORM / SQL access         | Spring Data JDBC or jOOQ                             |
| Database connection pool | HikariCP                                             |
| Worker execution         | Java background executors                            |
| HTTP client              | Java HttpClient, WebClient or another bounded client |
| Scheduling               | Database polling initially                           |
| Reliability              | Leases, fencing tokens, idempotency                  |
| Monitoring               | Metrics, tracing and structured logs                 |

Why Spring Data JDBC or jOOQ rather than automatically choosing JPA?

Because our workflow engine performs many explicit state transitions and conditional SQL updates.

We need precise control over:

```
UPDATE ...
WHERE status = ...
  AND claim_generation = ...
```

An ORM is not mandatory here.

You can use JPA for ordinary CRUD and explicit SQL for concurrency-sensitive operations.

I would avoid trying to hide every important workflow transition behind generic repository methods.

These are critical correctness operations, so the SQL should be clear and reviewable.

## 11.2 Production high-availability architecture

Once availability requirements increase:

```
                   REGION A
          +-------------------------+
          |                         |
          |     Load Balancer       |
          |           |             |
          |       API Instances     |
          |           |             |
          |       Worker Pools      |
          |           |             |
          |    PostgreSQL Primary   |
          |           |             |
          |      PostgreSQL Standby|
          +-----------|-------------+
                      |
             Replication / Backups
                      |
                      v
                   REGION B
          +-------------------------+
          |                         |
          |   Standby DB Cluster    |
          |                         |
          |   Standby Applications  |
          |                         |
          +-------------------------+
```

For production, I would additionally introduce:

- Automated database failover.
- Point-in-time recovery backups.
- A defined RPO and RTO.
- Durable outbox publishing.
- Kafka or RabbitMQ when needed.
- Reconciliation jobs.
- Operational dashboards.
- Manual recovery tooling.

The important design decision is that a Saga must have one authoritative writer at any point in time.

# 12. Should we use Kafka from day one?

My answer: Usually no.

Let's compare the two architectures.

### Architecture A: PostgreSQL only

```
API
 |
 v
PostgreSQL
 |
 v
Workers poll for READY actions
 |
 v
Participant Services
```

Advantages:

- Simple.
- Fewer components.
- Easy to debug.
- No additional broker infrastructure.
- Recovery comes directly from persisted state.

This is what I would initially implement.

### Architecture B: PostgreSQL + Outbox + Kafka

```
API
 |
 v
PostgreSQL
 |
 v
Transactional Outbox
 |
 v
Kafka
 |
 v
Workers
 |
 v
Participant Services
```

Advantages:

- More efficient event-driven dispatch.
- Multiple independent consumers.
- Event replay.
- Better integration with other event-driven systems.
- Less dependency on frequent database polling.

But more infrastructure also means more failure modes.

You now need to manage:

- Broker availability.
- Partitions.
- Consumer rebalancing.
- Duplicate messages.
- Outbox publishing.
- Consumer offsets.
- Event retention.
- Schema evolution.

My recommendation:

Start with PostgreSQL polling. Introduce Kafka when polling, event distribution or integration requirements justify it.

Importantly, moving from polling to Kafka does not require redesigning the Saga state machine.

The state machine remains the same.

Only the mechanism that tells workers about eligible work changes.

# 13. My final recommendation — What would I choose?

For the exact coordination workflow we've designed, I would choose:

## PostgreSQL + Java/Spring Boot + Background Workers

Why?

Because this particular workload is fundamentally about:

1. Managing relationships between sagas, steps and actions.
2. Making multiple workflow-state updates atomic.
3. Safely coordinating competing workers.
4. Scheduling retries.
5. Enforcing idempotency through unique constraints.
6. Persisting compensation progress.
7. Supporting operational queries and historical execution analysis.

PostgreSQL provides a very natural foundation for all of these.

My preferred design:

| Concern           | Choice                                         |
| ----------------- | ---------------------------------------------- |
| Primary database  | PostgreSQL                                     |
| Database topology | Primary + standby                              |
| API instances     | Stateless Spring Boot instances                |
| Worker execution  | Multiple background worker instances           |
| Claiming          | `FOR UPDATE SKIP LOCKED`                       |
| Ownership         | Leases + fencing tokens                        |
| State transitions | Conditional SQL updates and transactions       |
| Idempotency       | Unique constraints + stored operation outcomes |
| Retry scheduling  | Indexed `next_attempt_at`                      |
| Compensation      | Separate durable actions                       |
| Event publishing  | Transactional Outbox                           |
| Messaging         | Kafka when required                            |
| Cache             | Redis optionally                               |
| Historical data   | PostgreSQL partitioning and archival           |
| Multi-region      | One authoritative writer per Saga/shard        |

### When would I choose something else?

| Situation                                                                      | My choice   |
| ------------------------------------------------------------------------------ | ----------- |
| New generic Saga Orchestrator                                                  | PostgreSQL  |
| Existing enterprise MySQL infrastructure                                       | MySQL       |
| AWS-native, high-scale key/value workflow                                      | DynamoDB    |
| Distributed SQL and regional database requirements                             | CockroachDB |
| Document-oriented application already using MongoDB                            | MongoDB     |
| Extremely high-volume wide-column workloads with specialized workflow modeling | Cassandra   |
| Fast notification and caching                                                  | Redis       |
| High-throughput distributed event delivery                                     | Kafka       |

The most important thing is not the brand of database.

It is whether the database supports the consistency and coordination guarantees your workflow needs.

# 14. One final architectural principle: Don't over-distribute the coordination service

There is an interesting irony here.

We're building a distributed transaction coordinator.

It might seem natural to distribute every part of the coordinator itself:

```
Distributed API
Distributed scheduler
Distributed database
Distributed worker ownership
Distributed event processing
Distributed compensation engine
```

But that creates enormous operational complexity.

I would rather build:

```
Stateless horizontally scaled API
             |
             v
One transactional database cluster
             |
             v
Multiple stateless workers
             |
             v
Distributed participant services
```

And introduce database sharding or regional ownership only when there is a real capacity or availability requirement.

Why?

Because the orchestrator's job is to reduce complexity in distributed business workflows.

We shouldn't introduce unnecessary distributed coordination inside the orchestrator itself.

Keep the control plane as simple and reliable as possible, while scaling the execution plane independently.

That's the architecture I would defend in a system design interview.

# Chapter 8 — What we have established

You should now understand the database decision through the requirements, rather than memorizing database names.

The key takeaways:

- PostgreSQL is the strongest default for our relational, transaction-heavy workflow.
- MySQL is a perfectly viable alternative.
- DynamoDB requires access-pattern-driven modeling and conditional transactions.
- MongoDB works with an appropriately designed document model.
- Cassandra trades relational convenience for a specialized distributed data model.
- CockroachDB is attractive when distributed SQL is an actual requirement.
- Redis is excellent supporting infrastructure, but needs careful durability configuration.
- Kafka complements the workflow database; it doesn't automatically replace it.

# Next Chapter — Chapter 9: Complete Implementation in Java and Spring Boot

Now we've spent eight chapters understanding the architecture.

The final step is to turn it into an implementation.

I would build a complete Order Checkout Saga Orchestrator with:

1\. Database schema

- `sagas`
- `saga_steps`
- `saga_actions`
- `saga_step_attempts`
- `saga_idempotency`
- `outbox_events`
- Action dependency records

2\. REST APIs

```
POST   /sagas
GET    /sagas/{sagaId}
GET    /sagas/{sagaId}/steps
POST   /sagas/{sagaId}/retry
POST   /sagas/{sagaId}/cancel
```

3\. Execution engine

- Worker lifecycle.
- Atomic claiming.
- Lease renewal.
- Fencing.
- Participant HTTP clients.
- State transitions.

4\. Participant implementation

- Idempotency table.
- Unique constraints.
- Atomic business operations.
- Stored responses.
- Operation-status APIs.

5\. Failure handling

- Retry policy.
- Exponential backoff and jitter.
- Unknown-outcome reconciliation.
- Circuit breakers.
- Compensation.

6\. Production concerns

- Multiple application instances.
- Transactional outbox.
- Observability.
- Database failover.
- Recovery testing.

We'll build it incrementally, including actual SQL and Java code, and explain why each component exists.

The ultimate goal is that you should be able to look at a new distributed business workflow, identify its requirements, design the state machine, derive the database schema, implement the workers, and make it reliable without needing to memorize a predefined Saga architecture.


