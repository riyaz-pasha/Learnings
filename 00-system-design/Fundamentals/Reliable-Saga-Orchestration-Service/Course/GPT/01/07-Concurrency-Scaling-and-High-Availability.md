# Chapter 7 — Concurrency, Scaling and High Availability

So far, we've built a Saga Orchestrator that can reliably execute workflows even when:

- Workers crash.
- Participant services become unavailable.
- HTTP responses are lost.
- Duplicate requests arrive.
- Compensation operations fail.
- Multiple workers compete for the same task.

Now let's imagine the orchestrator has become successful, and business traffic grows.

Initially:

```
100 sagas / minute
10 workers
1 PostgreSQL database
```

Later:

```
10,000 sagas / minute
500 workers
Millions of active and historical saga actions
Multiple application instances
Multiple availability zones
Multiple regions
```

Can our architecture handle this?

Yes, but we need to understand where the bottlenecks actually are.

The first thing I want you to understand is:

Scaling the workers, scaling the database, scaling the message distribution, and scaling the participant services are four different problems.

Adding more workers does not automatically make the database faster.

Adding more database replicas does not automatically increase write throughput.

Adding Kafka does not automatically make workflow execution reliable.

We'll build the scaling architecture one problem at a time.

## 1. First, understand what exactly needs to scale

Our architecture has several independently scalable components.

```
                 Calling Services
                        |
                        v
                 Load Balancer
                        |
             +----------+----------+
             |          |          |
             v          v          v
          API-1       API-2       API-3
             |          |          |
             +----------+----------+
                        |
                        v
                 PostgreSQL Primary
                        |
             +----------+----------+
             |          |          |
             v          v          v
          Worker-1   Worker-2   Worker-N
             |          |          |
             +----------+----------+
                        |
                        v
                 Participant Services
```

Let's identify the different workloads.

| Component            | Workload                                 | Scaling requirement                    |
| -------------------- | ---------------------------------------- | -------------------------------------- |
| API servers          | Accept new saga requests                 | Scale with incoming request traffic    |
| Worker instances     | Execute forward and compensation actions | Scale with ready action backlog        |
| PostgreSQL           | Persist saga state and coordinate claims | Scale with transaction and I/O load    |
| Message broker       | Distribute notifications                 | Scale with event volume                |
| Participant services | Execute business operations              | Scale according to downstream capacity |

These components should not be tightly coupled to one another's deployment capacity.

For example, imagine a sudden increase in new saga requests.

We might increase API capacity without immediately increasing the number of workers.

The backlog will grow temporarily, but the system remains operational as long as the queue is durable and the business deadlines allow it.

Similarly, suppose there are millions of delayed retries, but only a small number of actions are currently READY.

We don't need thousands of workers constantly waiting for future retry times.

We need an efficient scheduler.

This is the first important scaling principle:

Scale each part according to its own workload, not according to the total application traffic.

# 2. Don't shard prematurely — Start with one PostgreSQL primary

Let's first answer a common architectural question:

> We are building a distributed orchestrator. Should we immediately use sharded databases?

No.

For most implementations, I would initially choose one PostgreSQL cluster with:

- One primary database for writes.
- One or more standby replicas for high availability.
- Read replicas if read-heavy reporting requires them.
- Multiple stateless API instances.
- Multiple worker instances.

Why?

Because our workflow naturally benefits from database transactions.

Consider a successful step transition:

```
1. Mark current action SUCCEEDED.
2. Save participant response.
3. Activate next action.
4. Update saga state.
5. Insert outbox event.
```

We want all these changes to commit atomically.

If all workflow records are in one PostgreSQL database, this is relatively straightforward.

```
BEGIN;

UPDATE saga_actions
SET status = 'SUCCEEDED'
WHERE id = :action_id;

UPDATE saga_actions
SET status = 'READY'
WHERE id = :next_action_id;

UPDATE sagas
SET updated_at = clock_timestamp()
WHERE id = :saga_id;

INSERT INTO outbox_events (...);

COMMIT;
```

One database transaction protects this state transition.

Now imagine distributing these tables across multiple unrelated databases.

The saga might be in Database A, the action in Database B, and the outbox in Database C.

Our atomic transition becomes a distributed transaction itself.

We have introduced another coordination problem just to coordinate the original coordination problem!

Therefore, keep the records that participate in a saga state transition together.

The default architecture should be one relational database cluster, not a collection of sharded databases.

PostgreSQL can scale quite far through indexing, partitioning, connection management, replication and hardware upgrades before application-level database sharding becomes necessary.

# 3. Scale the worker pool independently

Suppose we start with:

```
Orchestrator API instances: 3

Worker instances: 2

Workers per instance: 10
```

Total worker execution slots:

\\[ 2 \times 10 = 20 \\]

Now our workload grows.

We notice:

```
READY actions = 50,000

Average action waiting time = 3 minutes
```

Workers are not processing actions fast enough.

We can increase the worker capacity:

```
Orchestrator API instances: 3

Worker instances: 10

Workers per instance: 10
```

Total worker slots:

\\[ 10 \times 10 = 100 \\]

Notice that we didn't change the API capacity.

We didn't need to.

The API and execution engine are independently scalable.

### A useful throughput calculation

Let's assume:

```
Average participant execution time = 2 seconds

Concurrent worker slots = 100
```

Ignoring database overhead and other delays, approximate maximum throughput is:

\\[ Throughput = \frac{Concurrency}{Average\ Execution\ Time} \\]

\\[ = \frac{100}{2} = 50 \\]

So approximately 50 actions per second.

Or:

```
50 actions/second
3,000 actions/minute
```

This is only a theoretical estimate.

Actual throughput will also depend on:

- Database claim latency.
- Completion transaction latency.
- Participant service limits.
- HTTP connection pools.
- Retry frequency.
- Lease renewal overhead.
- CPU and memory.
- Network latency.

### A more useful intuition: Little's Law

Little's Law tells us:

\\[ L = \lambda W \\]

Where:

- \\(L\\) = Average number of operations in the system.
- \\(\lambda\\) = Throughput (operations per second).
- \\(W\\) = Average time an operation spends in the system.

Suppose:

```
Target throughput = 50 actions/second

Average execution time = 2 seconds
```

Then:

\\[ L = 50 \times 2 = 100 \\]

Approximately 100 concurrent execution slots are needed to sustain that throughput, assuming the workload and latency remain stable.

This helps reason about worker concurrency instead of arbitrarily choosing a number.

However, do not scale indefinitely.

Every worker can consume:

- One or more database connections at a time.
- An HTTP connection.
- Memory.
- CPU.
- A participant's capacity.

We'll see how to control this.

# 4. The database can become the bottleneck before your workers do

This is a very important real-world scenario.

Suppose we have:

```
Worker instances = 50

Workers per instance = 100

Total workers = 5,000
```

Imagine all 5,000 workers continuously doing:

```
1. SELECT READY action.
2. Claim action.
3. Renew lease.
4. Save attempt.
5. Save result.
6. Activate next action.
```

Our PostgreSQL database now handles a very large number of small transactions.

The problem is no longer necessarily the number of business operations.

It might be the number of coordination transactions required to execute them.

For every logical action, we might generate several database writes:

```
Claim transaction
Attempt record creation
Lease renewal
Completion transaction
Outbox insertion
Next action activation
```

A single Saga with four steps can generate many database transactions.

At a large scale, this adds up.

So we need to optimize the execution engine itself.

## 4.1 Don't create one database connection per worker thread

A common mistake:

```
5,000 worker threads
5,000 database connections
```

This can make the database slower rather than faster.

PostgreSQL connections consume resources, and increasing active connections indefinitely can lead to contention and throughput degradation.

Connection pooling and controlled concurrency are important.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://wiki.postgresql.org\&sz=32)

PostgreSQL wiki



For example:

```
Worker concurrency = 500

Database connection pool = 40

HTTP connection pool = 500
```

This is only an example.

Why might database connections be fewer than worker slots?

Because workers should spend most of their time waiting on remote participant services, not holding database connections.

The actual sequence is:

```
Worker starts
    |
    v
Borrow DB connection
    |
    v
Claim action
    |
    v
Commit and return DB connection
    |
    v
Call participant HTTP API
    |
    v
Borrow DB connection
    |
    v
Persist result
    |
    v
Commit and return connection
```

The worker does not need to hold a DB connection during the HTTP call.

We can therefore have more logical worker slots than database connections.

The important requirement is that the connection pool has enough capacity for the database work we want to perform concurrently, without overwhelming PostgreSQL.

## 4.2 Batch claiming — Reduce database round trips

Our current worker claims one action at a time.

```
SELECT ...
FOR UPDATE SKIP LOCKED
LIMIT 1;
```

Imagine 1,000 workers each claiming one action.

That's 1,000 separate claim transactions.

We can reduce the overhead by claiming a small batch.

For example:

```
BEGIN;

WITH candidates AS (
    SELECT id
    FROM saga_actions
    WHERE status = 'READY'
    ORDER BY created_at
    FOR UPDATE SKIP LOCKED
    LIMIT 10
)
UPDATE saga_actions a
SET
    status = 'RUNNING',
    lease_owner = :worker_id,
    lease_until =
        clock_timestamp() + INTERVAL '45 seconds',
    claim_generation = claim_generation + 1,
    attempt_count = attempt_count + 1
FROM candidates c
WHERE a.id = c.id
RETURNING a.*;

COMMIT;
```

Now the worker gets up to 10 actions in one database transaction.

It can execute them independently.

But be careful.

Suppose a worker has 10 execution slots and claims 100 actions.

Only 10 begin executing immediately.

The other 90 sit in memory while their leases are already running.

They might expire before execution even starts.

So a useful rule is:

Batch size should not exceed the worker's immediately available execution capacity by a large margin.

For example:

```
Available execution slots = 10

Claim batch size = 10
```

Or claim smaller batches dynamically.

Batch claiming reduces database round trips, but it does not remove the need for:

- Individual action ownership.
- Per-action fencing tokens.
- Stable idempotency keys.
- Independent attempt records.

Also, don't hold row locks while executing the batch. Commit the claim transaction before making participant calls.

# 5. Optimize how workers discover eligible actions

Remember our claim query:

```
SELECT id
FROM saga_actions
WHERE
    status = 'READY'

    OR (
        status = 'RETRY_WAIT'
        AND next_attempt_at <= clock_timestamp()
    )

    OR (
        status = 'RUNNING'
        AND lease_until <= clock_timestamp()
    )

ORDER BY created_at
FOR UPDATE SKIP LOCKED
LIMIT 10;
```

This is logically correct as a first design.

But as the table grows, the combination of several conditions and a shared ordering can make query planning and index usage less efficient.

We can improve the scheduling design.

### Approach A: Separate work categories

Create separate indexed paths for:

```
READY actions
Due RETRY_WAIT actions
Expired RUNNING leases
```

For example:

```
CREATE INDEX idx_actions_ready
ON saga_actions (created_at, id)
WHERE status = 'READY';
```

```
CREATE INDEX idx_actions_due_retry
ON saga_actions (next_attempt_at, id)
WHERE status = 'RETRY_WAIT';
```

```
CREATE INDEX idx_actions_expired_lease
ON saga_actions (lease_until, id)
WHERE status = 'RUNNING';
```

Now the database can locate each category through a more targeted index.

The scheduler can process these categories with separate claim queries.

For example:

```
Scheduler A:
    Finds READY actions.

Scheduler B:
    Finds due retries.

Scheduler C:
    Recovers expired leases.
```

They can also have different polling intervals and priorities.

### Why separate them?

Suppose there are:

```
READY actions = 1,000
RETRY_WAIT actions = 5 million
```

We don't want millions of future retries interfering with the discovery of immediately executable actions.

The database should not need to repeatedly scan all future retry records just to find READY work.

### Approach B: Separate queues by participant

As the workload increases, you might organize execution by target dependency:

```
ORDER_ACTIONS
INVENTORY_ACTIONS
PAYMENT_ACTIONS
SHIPPING_ACTIONS
```

Each queue can have its own:

- Worker pool.
- Concurrency limit.
- Rate limit.
- Retry policy.
- Priority.
- Monitoring.

This is particularly useful when participant services have very different capacities.

For example, Payment Service might safely handle 100 concurrent calls, while Inventory Service can handle 1,000.

A single unrestricted worker pool cannot express that distinction very well.

# 6. Prevent database contention inside the Saga itself

There is another subtle scaling problem.

Remember our `sagas` table?

Every time an action completes, we might update its parent saga:

```
UPDATE sagas
SET updated_at = clock_timestamp()
WHERE id = :saga_id;
```

This seems harmless.

But imagine one Saga has 100 independent actions executing in parallel.

```
SAGA-1001

ACTION-1  RUNNING
ACTION-2  RUNNING
ACTION-3  RUNNING
...
ACTION-100 RUNNING
```

All 100 workers finish around the same time.

If every worker tries to update the same saga row, they contend for that row's lock.

The parent saga row becomes a hotspot.

### Better approach

Treat the individual action records as authoritative for execution progress.

Update the saga row only when a meaningful overall workflow transition occurs.

For example:

```
RUNNING -> COMPENSATING
RUNNING -> SUCCEEDED
COMPENSATING -> COMPENSATED
```

Don't update the parent merely because a worker renews an action lease or changes an ordinary attempt counter.

Also:

- Store high-frequency lease information on the action row.
- Keep attempt history in its own table.
- Avoid unnecessary saga context rewrites.
- Use conditional state transitions.
- Do not make every worker lock the same parent row just to read workflow metadata.

This reduces contention.

For parallel workflows, maintain a dependency model that allows workers to determine whether successors are ready without repeatedly serializing every operation through the parent record.

There is a trade-off: calculating successors dynamically can be expensive. You can maintain durable dependency counters or ready-action records, provided updates to them remain atomic.

The guiding principle is:

Parallel actions should not unnecessarily contend on one shared saga-level record.

# 7. Database partitioning versus database sharding

This distinction is extremely important for system design interviews.

They solve different problems.

Suppose our `saga_step_attempts` table contains:

```
5 billion historical attempt records
```

The table has become very large.

We want to improve query performance and simplify archival.

One solution is partitioning.

## 7.1 Table partitioning

Partitioning means dividing one logical table into smaller physical tables managed by the same PostgreSQL database cluster.

For example, partition attempts by month:

```
saga_step_attempts

        |
        +-- attempts_2026_09
        |
        +-- attempts_2026_10
        |
        +-- attempts_2026_11
        |
        +-- attempts_2026_12
```

A query for November only needs to scan the relevant partition when PostgreSQL can prune the others.

Example:

```
SELECT *
FROM saga_step_attempts
WHERE started_at >= '2026-11-01'
  AND started_at < '2026-12-01';
```

This is called partition pruning.

PostgreSQL supports range, list and hash partitioning. It also supports detaching or dropping partitions, which can make historical data management much more efficient than deleting billions of individual rows.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.postgresql.org\&sz=32)

PostgreSQL+1



### Which tables should we partition?

My initial candidates:

| Table                | Possible partition strategy                        |
| -------------------- | -------------------------------------------------- |
| `saga_step_attempts` | Range by execution date                            |
| `outbox_events`      | Range by creation date                             |
| `saga_actions`       | Possibly by creation date or hash                  |
| `sagas`              | Only when size and query patterns justify it       |
| `saga_idempotency`   | Carefully, based on key scope and retention policy |

Don't partition every table just because it is possible.

For example, active saga records might be relatively small compared with execution history.

Partitioning the attempts table might deliver most of the benefit without adding unnecessary complexity to active execution.

### An important PostgreSQL caveat

Partitioning can affect uniqueness constraints.

For a partitioned table, a primary key or unique constraint generally needs to include all partition-key columns.

Suppose we partition attempt history by `started_at`.

Then a global constraint like:

```
UNIQUE (step_id, action_type, attempt_number)
```

cannot necessarily be enforced across all time partitions directly unless the partition key is included.

You must design this before choosing a partition key.

For example:

```
UNIQUE (
    step_id,
    action_type,
    attempt_number,
    started_at
)
```

But this changes the uniqueness semantics.

Alternatively, keep globally unique identities in an unpartitioned registry or use a different key design.

This is one reason table partitioning should be planned around both query patterns and data integrity requirements.

## 7.2 Database sharding

Sharding means distributing data across multiple independent database instances.

For example:

```
                    Saga Router
                         |
             +-----------+-----------+
             |           |           |
             v           v           v
          Shard 1     Shard 2     Shard 3
          DB-A        DB-B        DB-C
```

Each database stores only a portion of the total Saga workload.

For example:

```
Shard 1:
    SAGA-001
    SAGA-004
    SAGA-007

Shard 2:
    SAGA-002
    SAGA-005
    SAGA-008

Shard 3:
    SAGA-003
    SAGA-006
    SAGA-009
```

Now we have distributed the write workload across multiple database primaries.

Unlike table partitioning, this can increase aggregate database capacity by adding independent write nodes.

But it introduces routing, rebalancing, failure and cross-shard query complexity.

### The difference

| Database partitioning                                       | Database sharding                                        |
| ----------------------------------------------------------- | -------------------------------------------------------- |
| One database cluster                                        | Multiple database clusters                               |
| One primary write authority per cluster                     | Multiple independent write authorities                   |
| Logical table split into physical pieces                    | Data split across database instances                     |
| Primarily helps large table management and query efficiency | Helps distribute write load and storage                  |
| Simpler transactions across the database                    | Cross-shard transactions require additional coordination |

Partitioning is not the same as sharding.

## 7.2 Which Saga records should live on the same shard?

This is where database sharding becomes interesting.

Suppose we have three databases:

```
Database A
Database B
Database C
```

We need to decide how to distribute Saga data.

My recommendation is:

Put all records belonging to one saga on the same database shard.

For example:

```
SAGA-1001

Database A:
    saga record
    step records
    action records
    attempt records
    outbox events
    idempotency registration
```

And:

```
SAGA-1002

Database B:
    saga record
    step records
    action records
    attempt records
    outbox events
    idempotency registration
```

Why?

Consider our completion transaction:

```
Mark action succeeded
Activate next action
Update saga state
Insert outbox event
```

If all those records are on the same shard, we still get normal local ACID transactions.

If they are on different shards, we would need cross-database coordination.

That defeats much of the simplicity we worked to achieve.

### How do we choose the shard?

A natural strategy is hashing the saga ID:

```
shard = hash(sagaId) % numberOfShards
```

Example:

```
hash(SAGA-1001) % 3 = 0
    -> Database A

hash(SAGA-1002) % 3 = 1
    -> Database B

hash(SAGA-1003) % 3 = 2
    -> Database C
```

This distributes sagas relatively evenly, assuming a good hash function and sufficient workload diversity.

Another option is tenant-based routing:

```
tenant A -> Database A
tenant B -> Database B
tenant C -> Database C
```

This gives you tenant isolation and simpler tenant-level reporting.

But it has a downside: a very large tenant might overwhelm one database.

You can also use a hybrid strategy:

```
Normal tenants:
    Hash(tenant_id, saga_id) -> shard

Large dedicated tenant:
    Dedicated shard
```

This is useful for multi-tenant systems.

### A subtle but important point: Idempotency routing

Suppose a client submits:

```
Idempotency-Key: checkout-123
```

The orchestrator must determine which shard should own this request before creating the saga.

One approach:

```
1. Calculate a deterministic saga ID.
2. Calculate the shard from the saga ID.
3. Perform the idempotency registration and saga creation there.
```

Another approach is to use a small global routing registry for client request idempotency.

What you should avoid is creating the saga on one shard and storing its unique idempotency registration on a different shard without a reliable transaction strategy.

That would recreate the distributed atomicity problem.

### What about querying sagas?

For a query such as:

```
GET /sagas/SAGA-1001
```

The shard can be determined directly from the saga ID.

Simple.

But imagine:

```
GET /sagas?businessReference=ORDER-123
```

If the business reference doesn't contain the shard key, we might have to query all shards.

That's called scatter/gather.

It is more expensive.

You can introduce a routing index or make the business reference part of the deterministic routing scheme.

The trade-off is between routing efficiency, even distribution, and tenant isolation.

# 8. Avoid starving smaller tenants — Fair scheduling

Suppose we operate a multi-tenant Saga Orchestrator.

Our workload:

```
Tenant A:
    1,000,000 actions READY

Tenant B:
    100 actions READY

Tenant C:
    50 actions READY
```

If workers always select the oldest eligible action globally, Tenant A might consume most of the execution capacity.

Tenant B and Tenant C could wait much longer than necessary.

This is called the noisy-neighbor problem.

How do we solve it?

We need fair scheduling.

### Approach A: Per-tenant concurrency limits

For example:

```
Tenant A:
    Maximum 100 concurrent actions

Tenant B:
    Maximum 20 concurrent actions

Tenant C:
    Maximum 20 concurrent actions
```

Tenant A can still process more work, but it cannot monopolize the entire system.

### Approach B: Weighted fair scheduling

Suppose:

```
Tenant A weight = 5
Tenant B weight = 2
Tenant C weight = 1
```

The scheduler distributes execution opportunities approximately according to these weights, subject to available work and concurrency limits.

This is not necessarily strict FIFO, but it gives predictable resource allocation.

### Approach C: Queue partitioning

We can maintain separate logical queues:

```
QUEUE-A
QUEUE-B
QUEUE-C
```

Workers select queues based on their scheduling policy.

For larger systems, this can be implemented with a scheduler that assigns work to bounded execution pools.

### What about priorities?

Some sagas are more important than others.

For example:

```
Priority 1:
    Money transfers

Priority 2:
    Customer checkout

Priority 3:
    Marketing workflows
```

But be careful with priority queues.

If high-priority work never stops arriving, low-priority workflows might starve forever.

Introduce aging or reserved capacity so that low-priority work still makes progress.

# 9. What happens to millions of delayed retries?

This is another scalability challenge.

Imagine:

```
5 million saga actions

4 million are RETRY_WAIT

Only 10,000 are READY
```

Should workers repeatedly query all 4 million retries?

No.

We need to make retry scheduling efficient.

Remember our index:

```
CREATE INDEX idx_actions_due_retry
ON saga_actions (next_attempt_at, id)
WHERE status = 'RETRY_WAIT';
```

A scheduler can find actions whose retry times have arrived:

```
SELECT id
FROM saga_actions
WHERE status = 'RETRY_WAIT'
  AND next_attempt_at <= clock_timestamp()
ORDER BY next_attempt_at
FOR UPDATE SKIP LOCKED
LIMIT 100;
```

Only due retries are eligible.

Future retries remain untouched.

### Why not keep every delayed retry in an in-memory timer?

Because:

- The application might restart.
- Different instances would need shared scheduling.
- Millions of timers consume memory.
- Deployment would lose timer state.
- Long delays would need additional persistence.

The database holds the durable schedule.

The scheduler can use efficient indexed queries or a broker-based delay mechanism, but the authoritative retry time should remain durable.

### Separate retry scheduling from execution

I prefer this conceptual architecture:

```
                 Saga Database
                       |
          +------------+------------+
          |            |            |
          v            v            v
       READY       RETRY_WAIT   EXPIRED LEASE
          |            |            |
          |            v            |
          |       Due Scheduler     |
          |            |            |
          +------------+------------+
                       |
                       v
                 Eligible Actions
                       |
                       v
                 Worker Pool
```

A due retry is simply another eligible action.

It does not need a dedicated sleeping worker.

# 10. When should we introduce Kafka or RabbitMQ?

So far, we've used PostgreSQL polling.

Let's examine when that becomes insufficient.

Imagine:

```
100 worker instances.

Each polls PostgreSQL every 100 milliseconds.
```

That's potentially 1,000 polling queries per second, even if there is very little new work.

We can reduce this using backoff and adaptive polling, but eventually an event-driven notification mechanism might be useful.

This is where a message broker can help.

### Database polling architecture

```
PostgreSQL
    |
    v
Workers periodically query
    |
    v
Claim eligible action
    |
    v
Execute
```

### Database + broker architecture

```
PostgreSQL
    |
    | Transactional Outbox
    v
Outbox Publisher
    |
    v
Kafka / RabbitMQ
    |
    v
Worker receives notification
    |
    v
Claims action from PostgreSQL
    |
    v
Executes participant
```

Notice the critical detail:

The broker notifies workers about available work. The database still decides who owns the work.

Why not simply consume the message and execute the payload directly?

You can build that architecture too, but then the broker must become part of your durable execution authority, and you need to design recovery and transactional state transitions carefully.

For our database-backed orchestrator, retaining database ownership simplifies the model.

### Example event

```
{
  "eventId": "EVT-1001",
  "eventType": "SAGA_ACTION_READY",
  "actionId": "ACTION-1001",
  "sagaId": "SAGA-1001"
}
```

Worker receives it.

It queries the database and atomically claims the action.

If the event is duplicated, the database claim prevents two current owners.

If the event is lost, a periodic database reconciliation poll can still find the READY action.

This provides both efficient dispatch and durable recovery.

### Why use Kafka?

Kafka is particularly useful when:

- You have high event volume.
- Multiple independent consumers need the same events.
- You need partitioned event streams.
- You need event retention and replay.
- You want an event-driven worker architecture.

However, Kafka is not automatically a replacement for the Saga database.

Kafka has its own delivery semantics and transaction model. Apache Kafka distinguishes at-most-once, at-least-once and exactly-once processing, and its exactly-once guarantees have specific boundaries; they do not magically cover arbitrary external HTTP calls or participant databases.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://kafka.apache.org\&sz=32)

Apache Kafka



### What about message ordering?

Suppose events belong to:

```
SAGA-1001
```

We publish:

```
ACTION-1_READY
ACTION-1_SUCCEEDED
ACTION-2_READY
```

If ordering is important, use `sagaId` as the Kafka record key so related events go to the same partition.

That provides ordering within the partition.

But remember:

- Ordering in Kafka does not guarantee that an HTTP participant completed an operation.
- Duplicate notifications are still possible.
- Different workers might process duplicate or stale notifications.
- The database state machine must still validate every transition.

Kafka ordering complements the state machine; it does not replace it.

# 11. High availability — What if the orchestrator instance crashes?

We've discussed scaling, but scaling is not the same as availability.

Suppose:

```
API-1
API-2
API-3

Worker-1
Worker-2
Worker-3
```

Worker-2 crashes.

What happens?

Nothing catastrophic.

Other workers continue claiming available work.

Any abandoned actions are recovered after their leases expire.

Now suppose API-1 crashes.

The load balancer routes new requests to API-2 and API-3.

Existing sagas continue executing independently.

Now suppose the entire application instance crashes.

The other instances still have access to the same database.

The workflow state is not stored in any one application's memory.

This gives us application-level high availability.

But we haven't solved the most important infrastructure failure yet.

What if PostgreSQL itself goes down?

# 12. Database high availability — Primary and standby

A typical architecture:

```
                 Application Instances
                          |
                          v
                   Database Endpoint
                          |
                          v
                   PostgreSQL Primary
                          |
                  WAL Replication
                          |
              +-----------+-----------+
              |                       |
              v                       v
          Standby A               Standby B
```

All writes go to the primary.

Standbys receive changes through PostgreSQL replication.

If the primary fails, an appropriate standby can be promoted to become the new primary.

The application reconnects through a stable database endpoint.

PostgreSQL supports streaming replication, synchronous replication, asynchronous replication and standby promotion as part of its high-availability facilities.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.postgresql.org\&sz=32)

PostgreSQL+1



But we must understand one important trade-off.

## 12.1 Asynchronous replication

Suppose:

```
Primary DB:

SAGA-1001 = RUNNING

ACTION-1 = SUCCEEDED
ACTION-2 = READY
```

The primary commits the update.

But the standby has not received it yet.

Now the primary crashes.

We promote the standby.

Its database might contain:

```
SAGA-1001 = RUNNING

ACTION-1 = RUNNING
ACTION-2 = BLOCKED
```

Some previously acknowledged transactions might be missing.

Why?

Because asynchronous replication does not require the standby to acknowledge every transaction before the primary reports success.

PostgreSQL explicitly documents this possible data-loss window during asynchronous failover.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.postgresql.org\&sz=32)

PostgreSQL+1



This matters enormously for Saga orchestration.

Imagine the participant successfully charged the customer, but the orchestrator database failed over to a standby that hasn't received the corresponding success update.

After failover, the orchestrator might retry the payment.

Our participant-side idempotency protects the business operation from being charged twice.

But the orchestrator still needs to reconstruct the correct state.

This is why durable participant operation identities and status reconciliation are so important.

## 12.2 Synchronous replication

Now suppose we configure synchronous replication.

The primary does not acknowledge a transaction commit until the configured synchronous standby has acknowledged the required WAL durability level.

Conceptually:

```
Worker
   |
   v
Primary DB
   |
   | Commit transaction
   |
   v
Standby receives WAL
   |
   v
Standby acknowledges
   |
   v
Primary acknowledges commit to worker
```

This reduces the risk of losing an acknowledged transaction during failover.

But there is a cost.

The write transaction waits for the replication round trip.

PostgreSQL documents this latency trade-off explicitly.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.postgresql.org\&sz=32)

PostgreSQL



For our orchestrator:

- Saga acceptance must be durable.
- Claim and action transitions must survive the chosen failure model.
- Outbox records must survive consistently with the state transitions they represent.

I would consider synchronous replication for important durability requirements within a nearby availability-zone deployment.

For cross-region synchronous replication, carefully evaluate the latency and availability consequences.

Do not assume that synchronous replication means no failures or no ambiguous client outcomes.

For example, the database might commit, but the application loses the connection before receiving the commit acknowledgment.

The application still doesn't know whether the transaction committed.

It must reconnect and inspect durable state using stable operation identifiers.

## 12.3 What about split brain?

Suppose:

```
Primary A becomes unreachable.

Standby B gets promoted.

Primary A is actually still alive but isolated by a network partition.
```

Now two database nodes might believe they should accept writes.

This is dangerous.

A proper HA setup must have a reliable failover manager and fencing mechanism.

The old primary must be prevented from continuing to act as an independent writer after the new primary is promoted.

Never solve this by simply pointing workers at whichever database responds first.

The system needs one valid write authority per shard.

# 13. What happens to running workers during database failover?

Let's trace an actual failure.

```
Worker A

1. Claims CHARGE_PAYMENT.
2. Calls Payment Service.
3. Payment Service commits successfully.
4. PostgreSQL primary fails.
```

The orchestrator database might recover with:

```
CHARGE_PAYMENT = RUNNING
```

Or perhaps the completion transaction had committed on the old primary but was lost due to asynchronous replication.

Another worker may eventually reclaim the action.

It sends:

```
POST /payments
Idempotency-Key: SAGA-1001:CHARGE_PAYMENT:FORWARD
```

Payment Service returns the original successful result.

The orchestrator persists:

```
CHARGE_PAYMENT = SUCCEEDED
```

This is exactly why the reliability mechanisms from Chapter 4 remain necessary even when the orchestrator database has replication.

Database replication protects data durability according to its configured guarantees.

Participant idempotency protects business effects across uncertain execution and recovery.

They solve different problems.

### During database failover, workers should:

1. Stop attempting writes to the failed primary.
2. Reconnect through the managed database endpoint.
3. Re-read their current action ownership and generation.
4. Never assume an earlier unacknowledged database transaction committed.
5. Reconcile the participant outcome where necessary.
6. Resume only after the new primary is authoritative.

A worker should never assume that a connection failure means the database transaction rolled back.

That is another form of ambiguous outcome.

# 14. Multiple availability zones versus multiple regions

Let's distinguish these architectures.

### Multi-AZ deployment

Imagine:

```
Region A

Availability Zone 1
    API instances
    Workers
    PostgreSQL Primary

Availability Zone 2
    API instances
    Workers
    PostgreSQL Standby

Availability Zone 3
    PostgreSQL Standby
```

The goal is to survive the loss of an individual availability zone.

The application can continue in the surviving zones, provided the database failover mechanism promotes an appropriate standby.

This is usually the first high-availability step.

### Multi-region deployment

Now imagine:

```
                 Region A
              Primary Cluster
                    |
                    | Replication
                    |
                    v
                 Region B
              Standby Cluster
```

If Region A becomes unavailable, Region B can potentially take over.

But this introduces:

- Longer replication distances.
- Higher replication latency.
- More complex failover.
- Potential data-loss considerations with asynchronous replication.
- Region ownership and routing problems.
- Cross-region participant invocation.

For a coordination service, I would initially prefer:

One active write region per database shard, with a warm standby in another region.

Why?

Because it keeps the ownership model simple.

We have one authoritative database for each saga.

If we let multiple regions independently accept workflow state transitions for the same saga, we create a distributed consistency problem.

We would need to coordinate:

- Current action ownership.
- Claim generations.
- Saga state transitions.
- Compensation initiation.
- Idempotency registrations.
- Outbox publishing.

That is a lot of unnecessary complexity.

You can have multiple regions serving API traffic, but route each saga's writes to its authoritative region.

This is active-active at the application-routing level, but single-writer at the saga database level.

### What if we genuinely need active-active regions?

You can distribute different saga partitions across different regions.

For example:

```
Region A:
    SAGA-1
    SAGA-2
    SAGA-3

Region B:
    SAGA-4
    SAGA-5
    SAGA-6
```

Each saga has one authoritative region.

This is much easier than allowing both regions to write the same saga state concurrently.

You still need to account for cross-region service dependencies and the fact that a Saga may call participants in another region.

We'll revisit the multi-region implications in a later architecture chapter.

# 15. Can we use read replicas to scale database reads?

Yes.

But we must distinguish between reads that can tolerate stale data and reads that cannot.

Imagine:

```
GET /sagas/SAGA-1001
```

The saga just completed.

The primary database says:

```
status = SUCCEEDED
```

But a read replica hasn't received the latest update yet.

It might return:

```
status = RUNNING
```

This is replication lag.

For normal dashboards or historical reports, this may be acceptable.

For workflow coordination, it is not.

### My recommendation

| Query                     | Database                                        |
| ------------------------- | ----------------------------------------------- |
| Claim READY actions       | Primary                                         |
| Renew action lease        | Primary                                         |
| Persist success/failure   | Primary                                         |
| Schedule retries          | Primary                                         |
| Initiate compensation     | Primary                                         |
| Get immediate saga status | Primary                                         |
| Historical reports        | Read replica                                    |
| Aggregate analytics       | Read replica or analytics store                 |
| Operational dashboards    | Usually read replica, with freshness indicators |

Never claim work using a potentially stale read replica.

Never make a critical workflow transition based on a stale replica result.

A read replica is useful for distributing read-heavy workloads, but it does not increase the primary's write capacity.

If users need immediate read-after-write consistency, route their status query to the primary or implement an appropriate consistency mechanism.

# 16. What about Kafka's scalability?

Let's go a little deeper.

Suppose we introduce Kafka as a notification mechanism.

```
Saga DB
    |
    v
Outbox Publisher
    |
    v
Kafka Topic: saga-actions
```

Imagine the topic has:

```
12 partitions
```

We run:

```
4 worker consumers
```

The consumer group distributes the partitions among those consumers.

The maximum number of consumers actively assigned partitions at a time is limited by the partition count.

For example:

```
Consumer 1 -> Partitions 0, 1, 2
Consumer 2 -> Partitions 3, 4, 5
Consumer 3 -> Partitions 6, 7, 8
Consumer 4 -> Partitions 9, 10, 11
```

Now deploy 20 consumers.

Some will remain idle because there aren't enough partitions to assign to all of them.

This is one reason partition count matters when designing Kafka-based worker distribution.

Kafka orders records within each partition, not across the entire topic.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://kafka.apache.org\&sz=32)

Apache Kafka+1



### How do we choose the partition key?

For Saga notifications, a useful choice is:

```
partition key = sagaId
```

This groups events belonging to the same saga into the same partition.

Why?

Because we may want related Saga events to be processed in order.

But there is a subtle trade-off.

Suppose one particular saga generates an extremely high volume of events.

If every event uses the same key, they all go to one partition.

That partition can become a hotspot.

For a normal Saga, this is generally not a problem because the workflow itself has limited concurrency.

For extremely high-volume workflows, you might use a different key or split the work into independently executable streams.

However, if you lose ordering, you must rely more heavily on the database state machine and dependency validation.

### An important point

Kafka partitions are not Saga database shards.

They serve different purposes:

| Kafka partitions                     | Saga database shards                           |
| ------------------------------------ | ---------------------------------------------- |
| Distribute event-stream processing   | Distribute authoritative workflow data         |
| Control consumer parallelism         | Distribute database reads and writes           |
| Preserve ordering within a partition | Preserve local workflow transaction boundaries |
| Can be replayed                      | Hold current workflow state                    |
| Notification and event transport     | Durable execution authority                    |

Don't confuse them when designing your architecture.

# 17. How do we handle bulk Saga requests?

Let's consider another real-world scenario.

The calling service submits:

```
Start 10,000 order checkout sagas.
```

Should we create one massive Saga containing 10,000 orders?

Usually, no.

Each order is an independent business workflow.

I would model:

```
BATCH-100

    SAGA-001
    SAGA-002
    SAGA-003
    ...
    SAGA-10000
```

Every saga has its own:

- State machine.
- Execution steps.
- Idempotency identities.
- Retry policies.
- Compensation process.
- Final outcome.

For example:

```
SAGA-001 = SUCCEEDED
SAGA-002 = COMPENSATED
SAGA-003 = RUNNING
SAGA-004 = MANUAL_INTERVENTION_REQUIRED
```

The batch itself can have an aggregate status:

```
BATCH-100 = PARTIALLY_COMPLETED
```

But the individual sagas remain independent.

### Why is this better?

Suppose one of the 10,000 orders fails.

We should not necessarily compensate all the other 9,999 orders.

Each saga should recover independently.

You can also enforce batch-level admission controls:

```
Maximum active sagas per batch = 500
```

As some complete, additional sagas become eligible.

This protects the orchestrator and participant services from sudden traffic spikes.

If the API must accept a very large batch, create a durable batch job and produce the individual saga records in controlled chunks rather than holding one enormous transaction open.

# 18. How should we autoscale workers?

Now that the system has independent worker instances, we can autoscale them.

But what metrics should determine the number of workers?

A naive approach is:

```
CPU utilization > 70%
    |
    v
Add more workers
```

This is insufficient.

Workers might mostly be waiting on remote HTTP responses, so CPU utilization may be low even though the backlog is growing rapidly.

Instead, monitor the execution backlog.

I would use these metrics:

| Metric                               | What it tells us                              |
| ------------------------------------ | --------------------------------------------- |
| READY action count                   | How much immediately executable work exists   |
| Oldest READY action age              | How long work has been waiting                |
| Actions completed per second         | Actual execution throughput                   |
| Average action duration              | How long actions occupy workers               |
| RETRY_WAIT count                     | Retry workload                                |
| Expired lease count                  | Worker instability or undersized lease policy |
| Database connection pool utilization | Database access pressure                      |
| Database transaction latency         | Coordination overhead                         |
| Participant latency                  | Downstream capacity and health                |
| Reconciliation backlog               | Uncertain operations needing resolution       |

### Example autoscaling rule

Suppose:

```
READY actions > 10,000

AND

Oldest READY age > 30 seconds

AND

Database utilization is below its safe threshold
```

Then:

```
Increase worker instances.
```

But suppose:

```
READY actions = 10,000

Database is already saturated.
```

Adding more workers may make the situation worse.

We should instead:

- Reduce unnecessary polling.
- Reduce claim contention.
- Optimize indexes and queries.
- Reduce worker database connection demand.
- Increase database capacity.
- Consider database sharding if the single primary has reached its limits.

Similarly, if Payment Service is saturated, adding payment workers is not the answer.

We should apply backpressure.

Autoscaling should consider the bottleneck, not simply the size of the backlog.

# 19. Prevent one slow participant from consuming the entire worker pool

Imagine:

```
Payment Service response time = 30 seconds

Inventory Service response time = 100 milliseconds
```

We use a single pool of 100 workers.

Now 100 payment requests become active.

All workers are waiting on Payment Service.

Inventory operations are READY but cannot start.

This is called resource starvation.

We can solve it through bulkheads.

Think of a ship with separate compartments.

If one compartment floods, it shouldn't sink the entire ship.

Similarly, divide execution capacity.

```
                Saga Scheduler
                      |
          +-----------+-----------+
          |           |           |
          v           v           v
       Payment    Inventory    Shipping
        Pool         Pool        Pool
          |           |           |
       20 slots    50 slots    30 slots
```

Each pool has independent:

- Concurrency limits.
- Timeouts.
- Circuit breakers.
- Retry budgets.
- Rate limits.

If Payment Service becomes slow, inventory operations can still progress.

We can also combine these pools with a global worker capacity limit.

For example:

```
Global active worker limit = 200

Payment limit = 40
Inventory limit = 100
Shipping limit = 60
```

The scheduler controls the allocation.

This is much more predictable than allowing all workers to call every participant without limits.

# 20. What happens when a region or shard becomes overloaded?

Let's assume we have three Saga database shards.

```
Shard A:
    100,000 active sagas

Shard B:
    100,000 active sagas

Shard C:
    5,000,000 active sagas
```

Shard C is under disproportionate load.

We have a hot shard.

Possible causes:

- One exceptionally large tenant.
- Bad hash distribution.
- A traffic-heavy business workflow.
- Too many retries routed to one shard.
- One participant generating a large backlog.

The solution depends on the cause.

If the issue is an oversized tenant, give that tenant dedicated capacity.

If the issue is a bad shard distribution, change the routing strategy or rebalance data.

If the issue is retries, introduce dependency-level retry budgets.

If one saga itself generates too much parallel work, limit concurrency for that saga.

Don't immediately add more shards without understanding the hot spot.

### Rebalancing a Saga shard

Suppose we need to move a tenant's workloads from Database A to Database B.

A safe migration requires more than copying rows.

We must ensure:

1. No new sagas are routed to the old shard after the migration begins.
2. Existing active sagas have one authoritative owner.
3. Running workers cannot keep writing to the old database after ownership changes.
4. Idempotency registration and action identities are preserved.
5. Outbox events are migrated consistently.
6. Routing is switched only when the target has a consistent copy.
7. Recovery procedures exist for an interrupted migration.

Use a migration state or shard ownership mechanism and fencing to prevent both shards from executing the same saga as independent authorities.

This is operationally complex, which is another reason to avoid sharding until the workload justifies it.

# 21. The complete architecture at scale

Let's assemble everything we've discussed.

\#chatgpt-mermaid-\_r_3ub\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3ub\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3ub\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3ub\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3ub\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ub\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3ub\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3ub\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3ub\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3ub\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3ub\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3ub\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ub\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ub\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3ub\_ p{margin:0;}#chatgpt-mermaid-\_r_3ub\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ub\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ub\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ub\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3ub\_ .label text,#chatgpt-mermaid-\_r_3ub\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ub\_ .node rect,#chatgpt-mermaid-\_r_3ub\_ .node circle,#chatgpt-mermaid-\_r_3ub\_ .node ellipse,#chatgpt-mermaid-\_r_3ub\_ .node polygon,#chatgpt-mermaid-\_r_3ub\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3ub\_ .rough-node .label text,#chatgpt-mermaid-\_r_3ub\_ .node .label text,#chatgpt-mermaid-\_r_3ub\_ .image-shape .label,#chatgpt-mermaid-\_r_3ub\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3ub\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3ub\_ .rough-node .label,#chatgpt-mermaid-\_r_3ub\_ .node .label,#chatgpt-mermaid-\_r_3ub\_ .image-shape .label,#chatgpt-mermaid-\_r_3ub\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3ub\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3ub\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ub\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ub\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3ub\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3ub\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3ub\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3ub\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3ub\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3ub\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3ub\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ub\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ub\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3ub\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ub\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3ub\_ .icon-shape,#chatgpt-mermaid-\_r_3ub\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3ub\_ .icon-shape p,#chatgpt-mermaid-\_r_3ub\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3ub\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3ub\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3ub\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3ub\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3ub\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3ub\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3ub\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3ub\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3ub\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ub\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3ub\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3ub\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3ub\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ub\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3ub\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3ub\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ub\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3ub\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3ub\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ub\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3ub\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ub\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3ub\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3ub\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3ub\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3ub\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3ub\_ .node rect,#chatgpt-mermaid-\_r_3ub\_ .node circle,#chatgpt-mermaid-\_r_3ub\_ .node ellipse,#chatgpt-mermaid-\_r_3ub\_ .node polygon,#chatgpt-mermaid-\_r_3ub\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3ub\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3ub\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3ub\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3ub\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ub\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Participant ServicesBounded Worker PoolsReliable DispatchSaga Data LayerStateless API TierPayment ServiceInventory ServiceShipping ServiceOrder ServicePayment PoolInventory PoolShipping PoolOrder PoolTransactional OutboxKafka / RabbitMQOptionalPolling / Recovery SchedulerPostgreSQL Primary / ShardStandby ReplicaRead ReplicaAPI Instance 1API Instance 2API Instance NCalling ServicesAPI Load Balancer

This is a logical reference architecture. A production system may combine or separate components differently.

Some particularly important characteristics:

- API instances are stateless.
- Worker instances are replaceable.
- Database writes go to the authoritative primary.
- Standby replicas provide database HA.
- Read replicas serve suitable read-heavy workloads.
- The outbox makes notifications durable.
- The broker distributes notifications, if required.
- Worker pools are bounded and independently controlled.
- Participant services have their own idempotency mechanisms.
- Database sharding can be introduced when one primary is no longer sufficient.

# 22. When should we actually introduce each scaling mechanism?

You don't want to implement everything on day one.

I would evolve the system in stages.

| Stage | Architecture                                        | When it makes sense                                       |
| ----- | --------------------------------------------------- | --------------------------------------------------------- |
| 1     | One PostgreSQL primary + API + polling workers      | Initial production implementation                         |
| 2     | Multiple API and worker instances                   | Increased request and execution volume                    |
| 3     | Connection pooling, partial indexes, batch claiming | Database coordination overhead increases                  |
| 4     | Dedicated worker pools and backpressure             | Different participant capacities                          |
| 5     | Outbox + Kafka/RabbitMQ                             | Polling overhead or event-driven integration requirements |
| 6     | Database partitioning and archival                  | Large execution history and retention requirements        |
| 7     | PostgreSQL HA with standby promotion                | Availability and durability requirements                  |
| 8     | Database sharding                                   | One database primary can no longer support workload       |
| 9     | Multi-region shard ownership                        | Regional availability and latency requirements            |

This is the architectural evolution I would recommend.

Don't start at Stage 9.

The additional infrastructure brings operational complexity, and it doesn't automatically improve reliability.

# 23. A few important pitfalls when scaling this architecture

Before we conclude, let's address some mistakes that can appear even in otherwise well-designed implementations.

### Mistake 1: Assuming `SKIP LOCKED` guarantees perfect fairness

It doesn't.

Suppose:

```
ACTION-1 is locked.

ACTION-2 is READY.

ACTION-3 is READY.
```

A worker might skip ACTION-1 and process ACTION-2.

This is exactly what we want for concurrency.

But it means the system doesn't guarantee strict FIFO ordering.

PostgreSQL explicitly describes `SKIP LOCKED` as producing an inconsistent view, appropriate for queue-like consumers rather than ordinary application queries.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.postgresql.org\&sz=32)

PostgreSQL+1



For fairness, monitor the age of the oldest eligible action and introduce scheduling policies where necessary.

### Mistake 2: Using read replicas for worker coordination

A read replica might show:

```
ACTION-1 = READY
```

even though the primary has already changed it to RUNNING.

Never use a replica to make ownership decisions.

Use the authoritative primary for claims and state transitions.

### Mistake 3: Increasing workers without controlling participant concurrency

More workers might simply overwhelm Payment Service.

Use bounded execution pools, rate limits and circuit breakers.

### Mistake 4: Keeping all historical data forever in hot tables

Completed sagas and attempt records can grow enormously.

Use retention policies, archival and carefully chosen table partitioning.

But don't prematurely delete the identity and audit information required to prevent duplicate business operations.

### Mistake 5: Introducing Kafka but ignoring the database

A broker can distribute work efficiently.

It cannot replace the database's authoritative workflow state unless you deliberately redesign the entire state-management model around durable event processing.

For our architecture, use the broker for notifications and the database for workflow ownership.

### Mistake 6: Running active writers for the same saga in multiple regions

This introduces conflicting state transitions.

Keep one authoritative writer per saga or database shard, and use an explicit failover protocol to transfer ownership.

### Mistake 7: Updating the entire saga record for every small execution event

This creates contention, particularly for parallel workflows.

Keep high-frequency action state on the action records and minimize unnecessary parent-row updates.

# 24. How do we know when one PostgreSQL cluster is no longer enough?

Don't decide based on a fixed number of sagas or workers.

A system with 100 million mostly idle completed saga records may be easier to operate than one with 100,000 highly concurrent, retry-heavy actions.

Look at actual saturation indicators:

| Indicator                          | Possible response                                  |
| ---------------------------------- | -------------------------------------------------- |
| CPU saturation                     | Optimize queries or add database capacity          |
| High disk I/O                      | Improve indexes, storage or workload distribution  |
| High transaction latency           | Investigate locks, WAL, contention and query plans |
| Excessive connection waits         | Tune pools and transaction concurrency             |
| Slow action claiming               | Improve partial indexes and scheduler queries      |
| Large hot action tables            | Partition and archive                              |
| Primary write throughput exhausted | Consider database sharding                         |
| Uneven tenant workload             | Dedicated tenant shards or fair scheduling         |
| Excessive cross-region latency     | Region affinity and local ownership                |

One useful approach is to inspect:

```
SELECT *
FROM pg_stat_activity;
```

And:

```
SELECT *
FROM pg_stat_replication;
```

These help investigate current database activity and replication.

PostgreSQL also exposes lock wait events and replication statistics through its monitoring facilities.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.postgresql.org\&sz=32)

PostgreSQL+1



Use these alongside application-level metrics rather than trying to diagnose everything from database CPU utilization.

# 25. The final architecture I would recommend for a large production system

Assume we are building a generalized Saga Orchestrator for several business domains.

My preferred architecture would be:

Execution and coordination

- Stateless REST API instances.
- PostgreSQL as authoritative workflow state.
- Multiple independent worker instances.
- Atomic action claiming with `SKIP LOCKED`.
- Leases and fencing tokens.
- Separate forward and compensation actions.
- Durable retry scheduling.
- Participant-side idempotency.

Performance

- Connection pools.
- Partial indexes for eligible work.
- Small batch claiming.
- Separate worker pools by participant.
- Backpressure and rate limits.
- Partitioned attempt and event history.
- Archival for completed workflows.

Messaging

- Transactional Outbox.
- Kafka or RabbitMQ when notification throughput justifies it.
- Consumers that use the database to claim authoritative work.
- Duplicate event handling.
- Polling as a recovery fallback.

Availability

- Multiple API and worker instances.
- PostgreSQL primary and standby.
- Database failover with single-writer enforcement.
- Defined RPO and RTO.
- Participant reconciliation after ambiguous failures.
- Region affinity for each saga.

This gives us an architecture that can grow without changing the core business execution model.

## Chapter 7 — The mental model

Let's simplify everything we've learned.

Think of scaling as four separate questions:

| Question                                              | Solution                           |
| ----------------------------------------------------- | ---------------------------------- |
| More requests are arriving.                           | Scale API instances.               |
| More actions need execution.                          | Scale worker capacity.             |
| The database cannot handle the coordination workload. | Optimize, partition, or shard.     |
| The system must survive infrastructure failures.      | Replication, failover and fencing. |

And remember these five principles:

1. Scale workers independently of API servers.
2. Keep the database transactions short and minimize shared-row contention.
3. Use partitioning for large-table management; use sharding when independent database write capacity is required.
4. Use a broker for efficient event distribution, not as a substitute for workflow correctness.
5. Give each saga one authoritative state owner, even when the application runs across multiple instances or regions.

The most important lesson:

A reliable orchestration design doesn't require every component to be maximally distributed.

It requires every component to have a clear responsibility, a clear source of truth, and a well-defined recovery mechanism.

## Next Chapter — Chapter 8: Choosing the Right Database and Infrastructure

We've used PostgreSQL throughout our implementation.

But is PostgreSQL actually the best choice?

What about:

- MySQL?
- DynamoDB?
- MongoDB?
- Cassandra?
- CockroachDB?
- FoundationDB?
- Redis?
- Kafka as the primary execution queue?

We'll compare them based on the actual requirements of our workflow, rather than general database popularity.

We'll examine:

1. Why Saga Orchestrators generally benefit from relational databases.
2. PostgreSQL vs MySQL for this exact workload.
3. When DynamoDB makes sense and how conditional updates replace row locking.
4. Why Redis alone is usually insufficient for authoritative Saga state.
5. Why Cassandra-style databases require a different coordination design.
6. Whether distributed SQL databases are useful.
7. Whether Kafka can replace database polling and how far that architecture can go.
8. How to choose between database polling, broker-based scheduling and event sourcing.
9. A recommended production infrastructure on cloud platforms.
10. Final architecture and the trade-offs I would explain in a system design interview.

Our goal in the next chapter is to understand not just which database to choose, but why the database's capabilities must match the guarantees our Saga execution engine needs.

