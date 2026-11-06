# Choosing a Database for Reliable Background Processing: PostgreSQL vs DynamoDB

The most important thing to understand is:

The choice between SQS and Kafka does not determine whether you should use PostgreSQL or DynamoDB.

Both databases can work with both messaging systems.

The database is responsible for maintaining the business and execution state of tasks. The messaging system is responsible for delivering messages or events.

However, the best database depends on:

- How many tasks you create and process.
- How frequently workers update task state.
- Whether you need complex queries.
- Whether you need transactions across multiple records.
- Whether you need to coordinate ownership and retries.
- Whether you need ordering at the business-entity level.
- Whether your system is deployed in one region or multiple regions.
- How much operational complexity you are willing to manage.

For the systems we've designed, I would generally start with PostgreSQL unless the workload has characteristics that make DynamoDB a better fit.

But let's understand why, including how I would design each database for SQS and Kafka.

# 1. First, understand what we are storing in the database

Let's revisit our background-task architecture.

Suppose a user requests a large report export.

The task needs to:

1. Fetch data from multiple services.
2. Generate a report.
3. Upload the report to S3.
4. Update task status.
5. Notify the user.

We need to store much more than just the task ID.

Our database is responsible for maintaining the execution ledger.

I would divide the data into five logical categories.

| Data       | Example                               | Why we store it                            |
| ---------- | ------------------------------------- | ------------------------------------------ |
| Task       | `task-101`, `QUEUED`                  | Current task state                         |
| Attempt    | Attempt 1 failed, attempt 2 succeeded | Execution history                          |
| Ownership  | Worker 3, lease token 42              | Prevent concurrent ownership               |
| Scheduling | Retry after 30 seconds                | Retry management                           |
| Events     | Task created, task completed          | Reliable event publication and consumption |

A task record might look like this:

```
{
  "taskId": "task-101",
  "taskType": "REPORT_EXPORT",
  "status": "RUNNING",

  "attemptCount": 2,
  "maxAttempts": 5,

  "leaseOwner": "worker-3",
  "leaseToken": 42,
  "leaseExpiresAt": "2026-11-06T10:30:00Z",

  "heartbeatAt": "2026-11-06T10:28:30Z",

  "progress": {
    "stage": "GENERATING_PDF",
    "percentage": 70
  }
}
```

We also need:

```
tasks
task_attempts
outbox_events
consumer_inbox
aggregate_processing_state (when ordering is required)
```

The database needs to support these operations efficiently:

```
CREATE TASK

CLAIM TASK

RENEW LEASE

UPDATE PROGRESS

COMPLETE TASK

FAIL TASK

SCHEDULE RETRY

RECOVER EXPIRED TASK

RECORD EVENT

CHECK DUPLICATE EVENT
```

This list is important because we should select our database based on these access patterns, not just based on how many records we store.

# 2. Can PostgreSQL and DynamoDB both work with SQS and Kafka?

Absolutely.

Let's start with the architecture matrix.

| Architecture                                  | PostgreSQL | DynamoDB  |
| --------------------------------------------- | ---------- | --------- |
| SQS + long-running workers                    | Excellent  | Excellent |
| SQS + short background tasks                  | Excellent  | Excellent |
| SQS FIFO + ordered tasks                      | Excellent  | Excellent |
| Kafka + direct consumers                      | Excellent  | Excellent |
| Kafka + DB-backed task execution              | Excellent  | Excellent |
| Kafka + event ingestion and task scheduling   | Excellent  | Excellent |
| Kafka + multiple independent task subscribers | Excellent  | Excellent |

There is no inherent restriction.

The difference is how we model and access the data.

PostgreSQL is relational and SQL-oriented. DynamoDB is a distributed key-value/document database designed around predictable access patterns and horizontal scaling.

Let's examine both.

# 3. Option 1: PostgreSQL

PostgreSQL

PostgreSQL is my default recommendation for the task-processing architecture we've been designing.

Why?

Because this system has several related entities and state transitions that need to remain consistent.

We have:

```
Task
  |
  +---- Task Attempts
  |
  +---- Outbox Events
  |
  +---- Consumer Inbox
  |
  +---- Aggregate Processing State
```

These are naturally relational.

We need transactional guarantees when updating related records.

We need conditional updates for task ownership.

We need flexible queries for scheduling and monitoring.

PostgreSQL provides these capabilities very naturally.

## 3.1 PostgreSQL schema

A simplified schema:

```
CREATE TABLE tasks (
    id UUID PRIMARY KEY,

    task_type VARCHAR(100) NOT NULL,

    aggregate_id VARCHAR(255),

    sequence_number BIGINT,

    payload JSONB NOT NULL,

    status VARCHAR(30) NOT NULL,

    attempt_count INTEGER NOT NULL DEFAULT 0,

    max_attempts INTEGER NOT NULL DEFAULT 5,

    next_attempt_at TIMESTAMPTZ,

    lease_owner VARCHAR(150),

    lease_token BIGINT NOT NULL DEFAULT 0,

    lease_expires_at TIMESTAMPTZ,

    heartbeat_at TIMESTAMPTZ,

    progress JSONB,

    idempotency_key VARCHAR(255),

    created_at TIMESTAMPTZ DEFAULT NOW(),

    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

Attempts:

```
CREATE TABLE task_attempts (
    id UUID PRIMARY KEY,

    task_id UUID REFERENCES tasks(id),

    attempt_number INTEGER NOT NULL,

    worker_id VARCHAR(150),

    lease_token BIGINT NOT NULL,

    status VARCHAR(30) NOT NULL,

    started_at TIMESTAMPTZ NOT NULL,

    finished_at TIMESTAMPTZ,

    error_code VARCHAR(100),

    error_message TEXT,

    UNIQUE(task_id, attempt_number)
);
```

Outbox:

```
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,

    aggregate_id UUID NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    payload JSONB NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    created_at TIMESTAMPTZ DEFAULT NOW(),

    published_at TIMESTAMPTZ
);
```

We can add the inbox table when building the Kafka consumer architecture.

## 3.2 Why PostgreSQL works so well for task claiming

Remember, multiple workers might attempt to claim the same task.

We need an atomic ownership operation.

PostgreSQL makes this straightforward.

```
UPDATE tasks
SET
    status = 'RUNNING',

    attempt_count = attempt_count + 1,

    lease_owner = :worker_id,

    lease_token = lease_token + 1,

    lease_expires_at =
        clock_timestamp() + INTERVAL '90 seconds',

    heartbeat_at = clock_timestamp()

WHERE id = :task_id

AND (
    status IN ('QUEUED', 'RETRY_WAIT')
    OR (
        status = 'RUNNING'
        AND lease_expires_at < clock_timestamp()
    )
)

AND (
    next_attempt_at IS NULL
    OR next_attempt_at <= clock_timestamp()
)

RETURNING *;
```

Suppose 10 workers try to claim the same task.

Only one successfully changes the row under the conditions.

The remaining workers cannot claim it while the new lease is valid.

This is exactly the operation we need.

We can also insert the attempt record in the same transaction.

```
BEGIN;

-- Claim task and retrieve the new lease token.

UPDATE tasks
SET ...
WHERE ...
RETURNING *;

-- Insert task_attempts record using returned values.

INSERT INTO task_attempts (...);

COMMIT;
```

The claim and attempt creation either both commit or both roll back.

This is one of PostgreSQL's biggest advantages for this design.

# 4. PostgreSQL: How the important operations work

Let's look at the operations in our architecture.

| Operation                  | PostgreSQL mechanism                     |
| -------------------------- | ---------------------------------------- |
| Create task                | INSERT                                   |
| Create task + outbox event | Database transaction                     |
| Claim task                 | Conditional UPDATE                       |
| Prevent concurrent claims  | Row-level locking and atomic update      |
| Renew lease                | Conditional UPDATE                       |
| Increment fencing token    | Atomic counter update                    |
| Create attempt history     | INSERT in same transaction               |
| Schedule retry             | UPDATE + outbox INSERT                   |
| Recover expired leases     | Indexed query + `FOR UPDATE SKIP LOCKED` |
| Deduplicate event          | UNIQUE constraint                        |
| Enforce entity ordering    | Row locking + sequence validation        |

Notice how natural these operations are in a relational database.

## 4.1 Transactional outbox

For an SQS architecture:

```
BEGIN;

INSERT INTO tasks (...);

INSERT INTO outbox_events (...);

COMMIT;
```

The publisher reads pending outbox events and publishes them to SQS.

For Kafka, the same approach applies, except the publisher sends the event to Kafka.

We don't need fundamentally different task databases for the two messaging systems.

## 4.2 Retry scheduler

Suppose the task failed and needs retrying.

```
SELECT id
FROM tasks
WHERE status = 'RETRY_WAIT'
AND next_attempt_at <= clock_timestamp()
ORDER BY next_attempt_at
LIMIT 100
FOR UPDATE SKIP LOCKED;
```

Multiple scheduler instances can process different rows without all competing for the same tasks.

The scheduler transitions eligible tasks and records new outbox events within short transactions.

## 4.3 Recovery scheduler

We can identify expired leases:

```
SELECT id
FROM tasks
WHERE status = 'RUNNING'
AND lease_expires_at < clock_timestamp()
ORDER BY lease_expires_at
LIMIT 100
FOR UPDATE SKIP LOCKED;
```

This is useful for finding tasks whose workers disappeared.

We can mark old attempts as `ABANDONED` and schedule another attempt.

## 4.4 Monitoring and operational queries

This is another major PostgreSQL advantage.

For example:

Find all tasks stuck in RUNNING for more than 30 minutes:

```
SELECT *
FROM tasks
WHERE status = 'RUNNING'
AND heartbeat_at < NOW() - INTERVAL '30 minutes';
```

Find the most frequently failing task types:

```
SELECT
    task_type,
    COUNT(*) AS failed_tasks
FROM tasks
WHERE status = 'FAILED'
GROUP BY task_type
ORDER BY failed_tasks DESC;
```

Find average processing duration by task type:

```
SELECT
    t.task_type,
    AVG(
        EXTRACT(EPOCH FROM (a.finished_at - a.started_at))
    ) AS average_seconds
FROM task_attempts a
JOIN tasks t ON t.id = a.task_id
WHERE a.status = 'SUCCEEDED'
GROUP BY t.task_type;
```

These queries are straightforward.

With DynamoDB, we would need to design specific indexes and often maintain additional aggregates for these access patterns.

# 5. PostgreSQL trade-offs: Why not always use it?

PostgreSQL is excellent, but it is not without limitations.

Its biggest architectural limitation for this workload is that the primary database instance handles writes.

Imagine:

```
10,000 tasks are running.

Every 30 seconds:
    Each worker renews its lease.

That means:
    Approximately 333 heartbeat updates/second.
```

Now add:

```
Task creation:
    200 writes/second.

Task completion:
    200 writes/second.

Retry scheduling:
    50 writes/second.

Lease recovery:
    20 writes/second.
```

Your database now handles approximately 803 writes per second, excluding other application workloads.

This isn't necessarily a problem for PostgreSQL. A properly sized PostgreSQL cluster can handle substantial write throughput.

But we need to think about where the load comes from.

## 5.1 Write amplification

A heartbeat looks simple:

```
UPDATE tasks
SET
    heartbeat_at = clock_timestamp(),
    lease_expires_at = clock_timestamp() + INTERVAL '90 seconds'
WHERE id = :task_id;
```

But internally, PostgreSQL has to:

- Find the row.
- Acquire the required row-level lock.
- Create a new row version under MVCC.
- Generate WAL.
- Maintain relevant indexes.
- Eventually reclaim obsolete row versions through vacuum.

If you update thousands of task records continuously, this creates substantial write activity.

An important optimization:

Don't index every frequently changing column.

For example, an index on `heartbeat_at` might create unnecessary index maintenance if you update it every 30 seconds.

Index fields based on actual query requirements.

## 5.2 The single-writer bottleneck

PostgreSQL can scale reads using read replicas.

However, read replicas do not automatically increase the write capacity of the primary.

For example:

```
                 PostgreSQL

                 Primary
                    |
          +---------+---------+
          |                   |
          v                   v
      Read Replica 1     Read Replica 2
```

You can route:

```
Task creation       -> Primary
Task claim          -> Primary
Lease renewal       -> Primary
Task completion     -> Primary

Task history        -> Read replicas
Reporting           -> Read replicas
Analytics queries   -> Read replicas
```

Do not use an asynchronously replicated read replica to decide whether a task lease is valid or whether a claim succeeded. It might return stale information.

Also, a traditional RDS Multi-AZ standby is for high availability, not ordinary read scaling. RDS read replicas or a suitable Multi-AZ DB cluster are the relevant read-scaling options.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Relational Database Service+2



## 5.3 Connection management

Suppose we have:

```
500 worker instances.
```

Each worker opens:

```
10 database connections.
```

We now have:

```
5,000 database connections.
```

That can overwhelm the database even if the actual SQL workload is reasonable.

Solutions:

- Use a connection pool such as PgBouncer or RDS Proxy where appropriate.
- Keep transactions short.
- Don't hold connections while processing videos, generating PDFs or calling external APIs.
- Limit concurrent worker processing based on database capacity.
- Batch operations when doing so doesn't compromise correctness.

The worker should borrow a connection, perform its lease update, commit and return the connection. It should not hold a database connection for the duration of a 30-minute task.

## 5.4 Table growth

Consider:

```
tasks:
    10 million records.

task_attempts:
    100 million records.

outbox_events:
    100 million records.
```

This creates a lifecycle and retention problem.

I would:

- Partition large historical tables, often by time.
- Archive completed task and attempt history.
- Delete published outbox records according to retention requirements.
- Keep operational task records only as long as required.
- Tune autovacuum and monitor table/index bloat.
- Avoid excessively large JSON payloads in frequently updated task rows.

Keep the current task state relatively small. Store large input and output files in S3 and keep their object keys in the database.

## 5.5 How far can PostgreSQL scale?

Quite far.

You don't immediately need a distributed database because you have millions of tasks.

A sensible progression is:

```
Stage 1:
    One managed PostgreSQL primary.
    Proper indexes.
    Connection pooling.

Stage 2:
    Larger primary instance.
    Read replicas.
    Partition historical tables.
    Optimize heartbeat and scheduler queries.

Stage 3:
    Separate operational task database
    from other application workloads.
    Use CDC for outbox publishing.
    Archive old records.

Stage 4:
    Partition/shard by tenant or workload
    when a single writer becomes the limiting factor.

Stage 5:
    Consider distributed PostgreSQL-compatible
    systems or a NoSQL store if justified
    by measured workload requirements.
```

PostgreSQL also has distributed extensions and compatible distributed SQL alternatives, such as Citus and CockroachDB. These introduce additional architectural and operational trade-offs.

My point: PostgreSQL is not automatically unsuitable for high scale. Its write capacity and access patterns must be measured before deciding to move away from it.

# 6. Option 2: DynamoDB

Amazon DynamoDB

Now let's consider DynamoDB.

DynamoDB is very attractive for this architecture, especially when:

- You have a very large number of tasks.
- Most operations are key-based.
- Traffic fluctuates considerably.
- You want a managed, horizontally scalable database.
- You are operating primarily on AWS.
- You don't need complex joins and ad hoc SQL queries.

Its biggest advantage is that you don't have to manage a traditional relational database writer for the workload.

DynamoDB distributes data across partitions using partition keys and scales throughput according to its capacity model. Its on-demand capacity mode supports automatic scaling without requiring you to provision capacity for every expected traffic pattern.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon DynamoDB+1



However, DynamoDB requires a different way of thinking.

With PostgreSQL, you design your normalized entities and then query them. With DynamoDB, you start with the queries and design the keys and indexes required to support them.

Let's design it for our task-processing system.

# 7. DynamoDB data model

We should first list the operations.

```
1. Get task by ID.

2. Claim task by ID.

3. Renew lease by ID.

4. Update progress by ID.

5. Mark task complete by ID.

6. Get all attempts for task ID.

7. Find tasks whose retries are due.

8. Find tasks whose leases have expired.

9. Find tasks by status and tenant.

10. Find task history for a particular order.
```

Notice that most are direct lookups or updates by task ID.

DynamoDB is particularly good at this kind of workload.

## 7.1 Option A: Separate tables

We could create:

```
Tasks
TaskAttempts
OutboxEvents
ConsumerInbox
AggregateProcessingState
```

For example:

Tasks

```
{
  "taskId": "task-101",
  "status": "RUNNING",
  "attemptCount": 2,
  "leaseOwner": "worker-3",
  "leaseToken": 42,
  "leaseExpiresAt": 1793961000,
  "taskType": "REPORT_EXPORT"
}
```

TaskAttempts

```
{
  "taskId": "task-101",
  "attemptNumber": 2,
  "workerId": "worker-3",
  "status": "RUNNING",
  "startedAt": 1793960400
}
```

The partition key for the attempts table could be `taskId`, with `attemptNumber` as the sort key.

This makes it straightforward to retrieve the execution history for a task.

## 7.2 Option B: Single-table design

DynamoDB also supports single-table design.

We can store multiple logical entity types in one table.

Example:

| PK          | SK             | Data                       |
| ----------- | -------------- | -------------------------- |
| `TASK#101`  | `METADATA`     | Current task state         |
| `TASK#101`  | `ATTEMPT#0001` | First attempt              |
| `TASK#101`  | `ATTEMPT#0002` | Second attempt             |
| `TASK#101`  | `EVENT#0001`   | Task event                 |
| `TASK#101`  | `EVENT#0002`   | Task event                 |
| `ORDER#101` | `TASK#101`     | Task associated with order |

An example task item:

```
{
  "PK": "TASK#101",
  "SK": "METADATA",

  "status": "RUNNING",

  "attemptCount": 2,

  "leaseOwner": "worker-3",

  "leaseToken": 42,

  "leaseExpiresAt": 1793961000
}
```

An attempt item:

```
{
  "PK": "TASK#101",
  "SK": "ATTEMPT#0002",

  "workerId": "worker-3",

  "status": "RUNNING",

  "startedAt": 1793960400
}
```

Now we can query:

```
PK = TASK#101
```

And retrieve the task and its attempts.

Single-table design can reduce the number of tables and support efficient access patterns.

But don't adopt it just because it's a popular DynamoDB pattern.

For an engineering team more familiar with relational modeling, separate tables can be easier to understand and operate.

Both approaches are valid.

# 8. How do we claim a task in DynamoDB?

This is particularly interesting because DynamoDB provides conditional writes.

Remember our PostgreSQL operation:

```
UPDATE tasks
SET lease_owner = 'worker-1'
WHERE id = 'task-101'
AND status = 'QUEUED';
```

We need the same guarantee in DynamoDB.

We use `UpdateItem` with a `ConditionExpression`.

Conceptually:

```
UpdateItem:

Key:
    PK = TASK#101
    SK = METADATA

ConditionExpression:

    (
        status = QUEUED
        OR
        (
            status = RUNNING
            AND leaseExpiresAt < currentTime
        )
    )

UpdateExpression:

    SET
        status = RUNNING,
        leaseOwner = worker-1,
        leaseExpiresAt = newExpiry

    ADD
        attemptCount 1,
        leaseToken 1
```

The database atomically checks the condition and applies the update.

If the condition fails, the operation is rejected.

If it succeeds, we retrieve the updated item and its new fencing token.

DynamoDB supports conditional writes specifically to prevent conflicting updates from concurrent clients.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon DynamoDB+1



## 8.1 What happens if two workers claim simultaneously?

```
Worker 1:
    UpdateItem(task-101)

Worker 2:
    UpdateItem(task-101)
```

Suppose Worker 1 succeeds first.

The task becomes:

```
status = RUNNING
leaseOwner = worker-1
leaseToken = 41
```

Worker 2's condition is evaluated against the updated item.

The condition fails.

Worker 2 cannot claim the task.

This is equivalent to our atomic PostgreSQL claim.

### Important DynamoDB-specific detail

Unlike PostgreSQL, the condition cannot simply call `NOW()` on the database server.

The application supplies a timestamp such as `:now` and the calculated new expiration time.

Therefore, use synchronized clocks across workers, such as NTP, account for clock skew and use a sufficient safety margin.

The monotonic fencing token is particularly important because the timestamp alone should never be your only protection against stale workers.

Also, keep the conditional update as the authority. A preceding `GetItem` check is not a substitute for the conditional write.

# 9. How do we find expired leases in DynamoDB?

This is where the difference between PostgreSQL and DynamoDB becomes more significant.

In PostgreSQL, we can simply execute:

```
SELECT *
FROM tasks
WHERE status = 'RUNNING'
AND lease_expires_at < NOW();
```

DynamoDB doesn't support arbitrary queries across the entire table like this.

We must create an index for the access pattern.

We can use a Global Secondary Index (GSI).

## 9.1 Lease recovery GSI

Suppose we design:

```
GSI partition key:
    GSI1PK

GSI sort key:
    GSI1SK
```

For running tasks:

```
GSI1PK = LEASE#SHARD#03

GSI1SK = 1793961000#TASK#101
```

We distribute lease records across several shards:

```
LEASE#SHARD#00
LEASE#SHARD#01
LEASE#SHARD#02
LEASE#SHARD#03
...
LEASE#SHARD#15
```

The recovery scheduler queries each shard for entries whose expiration timestamps are before the current time.

Conceptually:

```
Query:

GSI1PK = LEASE#SHARD#03

GSI1SK <= currentTimestamp
```

It then retrieves the candidate task IDs.

For each candidate, it performs a conditional update against the base table to confirm that:

```
status = RUNNING

leaseExpiresAt < now
```

Only then does it recover the task.

Why do we need to validate again?

DynamoDB GSIs are eventually consistent.

An index entry can be stale.

For example:

```
1. Worker renews lease.
2. Base task item is updated.
3. Recovery scheduler queries the GSI.
4. The GSI still temporarily shows the old expiration.
5. Scheduler sees a supposedly expired task.
```

The conditional update on the base item prevents an incorrect recovery.

GSI queries support eventual consistency only, so they should be used to discover candidates rather than to make the final ownership decision.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon DynamoDB+1



## 9.2 Retry scheduler GSI

Similarly:

```
GSI2PK = RETRY#SHARD#03

GSI2SK = 1793962000#TASK#101
```

The scheduler queries:

```
GSI2PK = RETRY#SHARD#03

GSI2SK <= currentTimestamp
```

It gets the tasks whose retry times are due.

It then conditionally moves them to an eligible state or publishes their retry events through the outbox mechanism.

## 9.3 Why do we use shards?

Imagine we create a GSI like this:

```
GSI partition key = RUNNING
```

Now every running task is written to the same logical partition key.

This can create a hot key.

Instead:

```
RUNNING#SHARD#00
RUNNING#SHARD#01
RUNNING#SHARD#02
RUNNING#SHARD#03
```

We distribute index writes.

For example:

```
shard = hash(taskId) % 16
```

The scheduler queries the relevant shards in parallel.

This is a very important DynamoDB scaling technique.

However, shard count must be designed based on throughput and access patterns. More shards also mean more queries and more coordination.

# 10. DynamoDB transactions: Can we maintain consistency across tasks and attempts?

Yes.

This is another important feature.

Suppose a worker claims a task.

We want to:

1. Update the task.
2. Create an attempt record.
3. Record a corresponding event.

We can use `TransactWriteItems`.

Conceptually:

```
TransactWriteItems:

1. Update TASK#101 / METADATA

   Condition:
       Task is QUEUED or expired.

   Update:
       status = RUNNING
       leaseOwner = worker-1
       leaseToken = leaseToken + 1


2. Put TASK#101 / ATTEMPT#0001

   Condition:
       This attempt does not already exist.


3. Put OUTBOX#event-101

   Condition:
       This event does not already exist.
```

All these changes succeed or fail together.

DynamoDB supports ACID transactions across multiple items and tables in the same AWS account and Region. `TransactWriteItems` supports up to 100 actions and a 4 MB aggregate item size. Transactions also have additional capacity consumption compared with ordinary writes.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon DynamoDB+1



This is important because people sometimes assume:

> DynamoDB doesn't support transactions, so I cannot implement a reliable task system.

That is incorrect.

DynamoDB supports multi-item transactions.

However, its transactional model and query capabilities differ from PostgreSQL.

# 11. Outbox pattern in DynamoDB

We also need to solve the dual-write problem.

Suppose:

```
1. Create task in DynamoDB.
2. Publish message to Kafka or SQS.
```

We cannot independently perform these operations and assume they succeed atomically.

With DynamoDB, we have two useful approaches.

## Approach A: DynamoDB transaction + outbox item + DynamoDB Streams

This is my preferred approach when using DynamoDB for the task ledger.

Inside one transaction:

```
TransactWriteItems:

1. Put Task item.

2. Put Outbox item.
```

For example:

```
{
  "PK": "OUTBOX#SHARD#03",
  "SK": "EVENT#event-101",

  "eventId": "event-101",

  "eventType": "TASK_CREATED",

  "taskId": "task-101",

  "status": "PENDING"
}
```

Enable DynamoDB Streams.

Whenever the outbox record is created, a stream consumer can detect it and publish it to SQS or Kafka.

\#chatgpt-mermaid-\_r_4da\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4da\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4da\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4da\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4da\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4da\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4da\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4da\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4da\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4da\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4da\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4da\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4da\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4da\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4da\_ p{margin:0;}#chatgpt-mermaid-\_r_4da\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4da\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4da\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4da\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4da\_ .label text,#chatgpt-mermaid-\_r_4da\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4da\_ .node rect,#chatgpt-mermaid-\_r_4da\_ .node circle,#chatgpt-mermaid-\_r_4da\_ .node ellipse,#chatgpt-mermaid-\_r_4da\_ .node polygon,#chatgpt-mermaid-\_r_4da\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4da\_ .rough-node .label text,#chatgpt-mermaid-\_r_4da\_ .node .label text,#chatgpt-mermaid-\_r_4da\_ .image-shape .label,#chatgpt-mermaid-\_r_4da\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4da\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4da\_ .rough-node .label,#chatgpt-mermaid-\_r_4da\_ .node .label,#chatgpt-mermaid-\_r_4da\_ .image-shape .label,#chatgpt-mermaid-\_r_4da\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4da\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4da\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4da\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4da\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4da\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4da\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4da\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4da\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4da\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4da\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4da\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4da\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4da\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4da\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4da\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4da\_ .icon-shape,#chatgpt-mermaid-\_r_4da\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4da\_ .icon-shape p,#chatgpt-mermaid-\_r_4da\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4da\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4da\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4da\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4da\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4da\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4da\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4da\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4da\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4da\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4da\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4da\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4da\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4da\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4da\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4da\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4da\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4da\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4da\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4da\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4da\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4da\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4da\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4da\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4da\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4da\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4da\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4da\_ .node rect,#chatgpt-mermaid-\_r_4da\_ .node circle,#chatgpt-mermaid-\_r_4da\_ .node ellipse,#chatgpt-mermaid-\_r_4da\_ .node polygon,#chatgpt-mermaid-\_r_4da\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4da\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4da\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4da\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4da\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4da\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}APIDynamoDB TransactionTask ItemOutbox ItemDynamoDB StreamsLambda / PublisherSQS or Kafka

DynamoDB Streams captures changes in near real time and retains stream records for 24 hours. If the publisher is unavailable beyond the stream retention period, you need an additional recovery/reconciliation mechanism for unpublished outbox records.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon DynamoDB+1



Also, the publisher must tolerate repeated processing and use a stable event ID.

## Approach B: Transactional outbox with an explicit polling publisher

You could also store outbox records in DynamoDB and have a scheduler periodically query pending events through a GSI.

Then:

```
1. Query PENDING outbox records.
2. Publish to Kafka/SQS.
3. Conditionally mark them published.
```

This is possible, but you must carefully design the indexing, sharding, ownership and retry mechanisms.

I generally prefer the Streams approach for a DynamoDB-native event publisher, together with a durable outbox reconciliation process.

# 12. DynamoDB advantages and disadvantages

Let's summarize.

| Advantage                     | Why it helps                                                                             |
| ----------------------------- | ---------------------------------------------------------------------------------------- |
| Horizontal scaling            | Good for large, distributed workloads                                                    |
| Managed infrastructure        | Less database administration                                                             |
| High key-based throughput     | Fast task lookup and updates                                                             |
| Conditional writes            | Atomic ownership and state transitions                                                   |
| Transactions                  | Coordinate task, attempt and outbox records                                              |
| On-demand capacity            | Useful for unpredictable workloads                                                       |
| AWS integration               | Streams, Lambda, SQS and other AWS services                                              |
| No traditional primary writer | Doesn't have the same single-writer architecture as a conventional PostgreSQL deployment |

And the trade-offs:

| Disadvantage                      | Why it matters                                                  |
| --------------------------------- | --------------------------------------------------------------- |
| No joins                          | Related data must be retrieved through designed access patterns |
| No arbitrary SQL                  | Operational queries require indexes or precomputed views        |
| GSI eventual consistency          | Candidate discovery can return stale results                    |
| Index design is essential         | Queries not anticipated in advance are difficult                |
| Hot partition keys                | A poorly designed key can limit throughput                      |
| Transactions cost more            | Multi-item operations use additional capacity                   |
| More complicated scheduler design | Retries and expired leases need suitable indexes                |
| Clock management                  | Lease comparisons use application-supplied time                 |
| Duplicate data                    | Query-specific access patterns may require denormalization      |

## A particularly important warning: Do not use TTL for leases

You might be tempted to do this:

```
leaseExpiresAt = now + 90 seconds

TTL = leaseExpiresAt
```

And assume that DynamoDB deletes the lease record when the worker's lease expires.

Don't do this.

DynamoDB TTL is intended for asynchronous data cleanup.

Expired records can remain in the table for days before deletion.

It is not a real-time lock or lease mechanism.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon DynamoDB



The correct approach is:

```
Lease expires:
    Compare leaseExpiresAt with current time.

Recovery scheduler:
    Finds expired tasks.

Conditional update:
    Claims the task for a new worker.
```

Use TTL only for eventually deleting old records, such as old attempt logs, deduplication records after the required retention window, or expired operational metadata.

# 13. Comparing PostgreSQL and DynamoDB for our actual operations

Now let's compare them specifically for our workload, rather than general database features.

| Operation                 | PostgreSQL                   | DynamoDB                                |
| ------------------------- | ---------------------------- | --------------------------------------- |
| Create task               | INSERT                       | PutItem                                 |
| Create task + outbox      | SQL transaction              | TransactWriteItems                      |
| Claim task                | Conditional UPDATE           | UpdateItem + ConditionExpression        |
| Renew lease               | Conditional UPDATE           | Conditional UpdateItem                  |
| Fencing token             | Atomic increment             | Atomic ADD                              |
| Record attempt            | INSERT                       | PutItem / TransactWriteItems            |
| Find expired tasks        | Indexed SQL query            | GSI query                               |
| Find due retries          | Indexed SQL query            | GSI query                               |
| Update aggregate sequence | UPDATE with row locking      | Conditional UpdateItem / transaction    |
| Duplicate event detection | Unique constraint            | Conditional PutItem                     |
| Task history              | SQL JOIN / SELECT            | Query by partition key and sort key     |
| Task status reporting     | Flexible SQL                 | Designed indexes or separate read model |
| Cleanup historical data   | DELETE / partition removal   | TTL                                     |
| Change data capture       | Logical decoding / CDC tools | DynamoDB Streams                        |

Both can implement the same business guarantees.

The key difference is that PostgreSQL gives us more flexible querying and relational transactions, while DynamoDB makes massive key-based workloads easier to distribute and operate.

# 14. Now let's apply this to SQS

Suppose we choose SQS Standard for background tasks.

Our architecture:

\#chatgpt-mermaid-\_r_4e5\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4e5\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4e5\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4e5\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4e5\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4e5\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4e5\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4e5\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4e5\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4e5\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4e5\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4e5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4e5\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4e5\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4e5\_ p{margin:0;}#chatgpt-mermaid-\_r_4e5\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4e5\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4e5\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4e5\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4e5\_ .label text,#chatgpt-mermaid-\_r_4e5\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4e5\_ .node rect,#chatgpt-mermaid-\_r_4e5\_ .node circle,#chatgpt-mermaid-\_r_4e5\_ .node ellipse,#chatgpt-mermaid-\_r_4e5\_ .node polygon,#chatgpt-mermaid-\_r_4e5\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4e5\_ .rough-node .label text,#chatgpt-mermaid-\_r_4e5\_ .node .label text,#chatgpt-mermaid-\_r_4e5\_ .image-shape .label,#chatgpt-mermaid-\_r_4e5\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4e5\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4e5\_ .rough-node .label,#chatgpt-mermaid-\_r_4e5\_ .node .label,#chatgpt-mermaid-\_r_4e5\_ .image-shape .label,#chatgpt-mermaid-\_r_4e5\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4e5\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4e5\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4e5\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4e5\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4e5\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4e5\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4e5\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4e5\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4e5\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4e5\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4e5\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4e5\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4e5\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4e5\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4e5\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4e5\_ .icon-shape,#chatgpt-mermaid-\_r_4e5\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4e5\_ .icon-shape p,#chatgpt-mermaid-\_r_4e5\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4e5\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4e5\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4e5\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4e5\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4e5\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4e5\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4e5\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4e5\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4e5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4e5\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4e5\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4e5\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4e5\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4e5\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4e5\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4e5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4e5\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4e5\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4e5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4e5\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4e5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4e5\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4e5\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4e5\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4e5\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4e5\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4e5\_ .node rect,#chatgpt-mermaid-\_r_4e5\_ .node circle,#chatgpt-mermaid-\_r_4e5\_ .node ellipse,#chatgpt-mermaid-\_r_4e5\_ .node polygon,#chatgpt-mermaid-\_r_4e5\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4e5\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4e5\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4e5\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4e5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4e5\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientAPITask DatabaseOutboxSQS PublisherSQSMultiple WorkersExternal Services / S3Retry SchedulerLease Recovery Scheduler

Which database should we choose?

Let's consider both.

## 14.1 SQS + PostgreSQL

This would be my default recommendation.

The flow:

```
1. API receives task request.

2. PostgreSQL transaction:
       Insert task.
       Insert outbox event.

3. Outbox publisher:
       Publish to SQS.

4. Worker receives message.

5. Worker:
       Atomically claims task in PostgreSQL.

6. Worker:
       Processes task.
       Renews DB lease.
       Extends SQS visibility.

7. Worker:
       Updates task to SUCCEEDED.

8. Worker:
       Deletes SQS message.
```

### Why PostgreSQL?

Because our task system is fundamentally a state machine.

We have:

```
QUEUED
RUNNING
RETRY_WAIT
FAILED
SUCCEEDED
CANCEL_REQUESTED
CANCELLED
```

We need:

- Transactional state changes.
- Task attempts.
- Lease management.
- Flexible operational queries.
- Retry scheduling.
- Idempotency constraints.
- Outbox records.

PostgreSQL handles these extremely naturally.

### How would it scale?

Suppose:

```
Task creation:
    500 tasks/second

Active tasks:
    20,000

Heartbeat interval:
    30 seconds
```

Heartbeat write rate:

\\[ \frac{20,000}{30} \approx 667 \\]

That's approximately 667 lease updates per second.

Now add task creation, completion, retries and monitoring.

We would size PostgreSQL based on the total write rate, transaction patterns, indexes and latency requirements.

We can:

- Keep operational tables small.
- Partition attempts and outbox records.
- Batch scheduler operations.
- Use connection pooling.
- Scale the primary vertically.
- Move reporting reads to replicas.
- Separate task processing from unrelated transactional workloads.

This is a perfectly reasonable architecture for a substantial system.

## 14.2 SQS + DynamoDB

This is equally valid.

The flow:

```
1. API receives task request.

2. DynamoDB transaction:
       Put task item.
       Put outbox item.

3. DynamoDB Streams:
       Detect outbox creation.

4. Publisher:
       Publish to SQS.

5. Worker receives message.

6. Worker:
       Conditional UpdateItem to claim task.

7. Worker:
       Performs processing.
       Renews DB lease using conditional update.
       Extends SQS visibility.

8. Worker:
       Conditionally updates task to SUCCEEDED.

9. Worker:
       Deletes SQS message.
```

### Why DynamoDB?

The access pattern is predictable.

Most worker operations look like:

```
Get task by taskId.

Update task by taskId.

Renew lease by taskId.

Complete task by taskId.
```

These are excellent DynamoDB use cases.

The queue handles task distribution.

DynamoDB handles task state and ownership.

You can scale the task ledger without managing a conventional PostgreSQL writer.

### How would it scale?

Suppose:

```
Active tasks:
    100,000

Heartbeat interval:
    30 seconds
```

The heartbeat workload is approximately:

\\[ \frac{100,000}{30} \approx 3,333 \\]

That is approximately 3,333 task-item updates per second.

DynamoDB can be a good fit here, provided:

- Partition keys distribute writes evenly.
- Global secondary indexes can handle their write load.
- Capacity and throttling are managed.
- The retry and recovery schedulers use sharded indexes.
- You monitor consumed capacity and throttling.

Remember, updating an indexed attribute also creates index maintenance work.

In our case, changing:

```
leaseExpiresAt
```

may also update the lease-recovery GSI.

That is part of the write workload.

My opinion: For a task-processing system with very high numbers of independent task records and relatively simple key-based access patterns, DynamoDB is a strong option.

But if you need flexible reporting, complicated workflow queries or multiple related state transitions, PostgreSQL will often be easier.

# 15. Now apply this to Kafka

There are two architectures we discussed.

We need to treat them differently.

## 15.1 Kafka with direct task execution

Architecture:

```
Kafka Topic
     |
     v
Consumer Group
     |
     v
Worker
     |
     v
Task Database
     |
     v
External Business Operations
     |
     v
Commit Kafka Offset
```

Here, Kafka handles event delivery and partition assignment.

The database handles the business state.

### Kafka + PostgreSQL

This is a very good combination.

For example:

```
1. Consumer receives offset 105
   from partition 2.

2. Worker claims task in PostgreSQL.

3. Worker executes business logic.

4. Worker records SUCCESS in PostgreSQL.

5. Worker commits offset 106.
```

If the worker crashes between steps 4 and 5, the record is replayed.

PostgreSQL detects the already-completed task using its current state or inbox event ID.

The task isn't executed again.

### Kafka + DynamoDB

The same architecture is possible.

```
1. Consumer receives Kafka record.

2. Worker conditionally claims task
   in DynamoDB.

3. Worker executes business logic.

4. Worker conditionally updates task
   to SUCCEEDED.

5. Worker commits Kafka offset.
```

A duplicate record encounters the existing state or inbox event and does not repeat the business operation.

There is one additional challenge:

Kafka partition ownership can change during a rebalance.

DynamoDB's conditional ownership update and fencing token must protect the task independently of Kafka's consumer assignment.

The same is true with PostgreSQL.

Neither database changes the fundamental Kafka offset-management problem.

The choice between PostgreSQL and DynamoDB is still about the business-state access pattern.

## 15.2 Kafka ingestion + DB-backed task executor

This is the architecture I would particularly recommend for long-running and retry-heavy jobs.

Remember the separation:

```
Kafka:
    Delivers ordered domain events.

Database:
    Persists accepted tasks and workflow state.

Worker pool:
    Executes long-running business tasks.
```

The Kafka consumer has a short responsibility.

It performs:

```
1. Receive event.
2. Deduplicate event.
3. Persist task.
4. Commit Kafka offset.
```

Actual task execution happens separately.

Here, we have two strong options.

### Kafka + PostgreSQL

```
Kafka
  |
  v
Ingestion Consumer
  |
  v
PostgreSQL Inbox + Task
  |
  v
Task Coordinator
  |
  v
Worker Pool
  |
  v
PostgreSQL Task State
```

Advantages:

- One transaction for inbox and task creation.
- Unique constraints for deduplication.
- SQL-based retry scheduling.
- Easy lease recovery.
- Aggregate-level sequence locking.
- Flexible operational queries.

This is my preferred design when business workflow correctness and query flexibility are important.

### Kafka + DynamoDB

```
Kafka
  |
  v
Ingestion Consumer
  |
  v
DynamoDB Transaction
  |
  +---- Task Item
  |
  +---- Inbox Item
  |
  v
Task Coordinator
  |
  v
Worker Pool
  |
  v
DynamoDB Conditional Updates
```

The ingestion consumer performs a transactional write containing:

```
Put inbox event.

Put task item.

Optionally update aggregate state.
```

Only after success does it commit the Kafka offset.

The task coordinator discovers eligible items using appropriate indexes.

Workers claim those items with conditional writes.

This can scale very well for enormous volumes of independent tasks, but its indexing and scheduling design requires more care.

### Comparison

| Concern                             | Kafka + PostgreSQL                  | Kafka + DynamoDB                    |
| ----------------------------------- | ----------------------------------- | ----------------------------------- |
| Inbox deduplication                 | Unique constraint                   | Conditional PutItem                 |
| Task creation                       | SQL transaction                     | TransactWriteItems                  |
| Consumer replay handling            | Straightforward                     | Straightforward                     |
| Ordered business workflows          | SQL row locking and sequence checks | Conditional sequence updates        |
| Retry scheduling                    | SQL indexes and scheduler           | Sharded GSI and scheduler           |
| Expired lease recovery              | SQL index and SKIP LOCKED           | GSI queries and conditional updates |
| Task status queries                 | Very flexible                       | Must design access patterns         |
| Scaling predictable key-based reads | Good                                | Excellent                           |
| Operational administration          | More database maintenance           | More index/capacity management      |

# 16. Does Kafka itself require a database?

Not necessarily.

This is another important distinction.

Consider this architecture:

```
Kafka Topic A
     |
     v
Consumer
     |
     v
Transform Event
     |
     v
Kafka Topic B
```

Suppose all we do is:

1. Read a Kafka event.
2. Transform it.
3. Write another Kafka event.

We can use Kafka transactions to atomically publish the output and commit input offsets.

We might not need PostgreSQL or DynamoDB for the processing state at all.

Kafka can provide the event-processing guarantees for this Kafka-to-Kafka flow.

But now consider:

```
Create report.
Track processing progress.
Retry after 30 seconds.
Allow cancellation.
Show task status to user.
Store output location.
Maintain execution history.
Recover expired leases.
```

Now we have a business task-management system.

We need a durable application-level state store.

That could be PostgreSQL or DynamoDB.

Kafka's committed offsets do not replace the task ledger.

For streaming stateful applications, Kafka Streams also offers local state stores and changelog topics, but a queryable business task API usually needs an appropriately designed state or database layer.

# 17. How does this architecture scale at 10,000, 100,000 and 1 million tasks?

This is where database selection becomes interesting.

Let's take three different scales.

Assume:

```
Task execution duration:
    10 minutes average

Heartbeat interval:
    30 seconds

Each running task:
    One heartbeat update every 30 seconds
```

The number of database heartbeat writes is:

\\[ Heartbeat\ writes/sec = \frac{Active\ tasks}{Heartbeat\ interval} \\]

| Concurrent running tasks | Heartbeats per second |
| ------------------------ | --------------------- |
| 1,000                    | 33                    |
| 10,000                   | 333                   |
| 50,000                   | 1,667                 |
| 100,000                  | 3,333                 |
| 500,000                  | 16,667                |
| 1,000,000                | 33,333                |

These numbers represent only heartbeat updates.

We still need to account for:

- Task creation.
- Task claims.
- Task completion.
- Attempt history.
- Retry scheduling.
- Lease recovery.
- Outbox publishing.
- Progress updates.

And a transaction may involve multiple writes.

For example, a claim might update the task and insert an attempt record.

A completion might update the task and attempt records.

A DynamoDB transaction may also have additional capacity overhead.

This is why simply calculating the number of tasks isn't enough.

We need to calculate total database operations.

## 17.1 Scaling PostgreSQL

For PostgreSQL, I would use this architecture:

\#chatgpt-mermaid-\_r_4gk\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4gk\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4gk\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4gk\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4gk\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gk\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4gk\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4gk\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4gk\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4gk\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4gk\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4gk\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gk\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gk\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4gk\_ p{margin:0;}#chatgpt-mermaid-\_r_4gk\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gk\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gk\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gk\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4gk\_ .label text,#chatgpt-mermaid-\_r_4gk\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gk\_ .node rect,#chatgpt-mermaid-\_r_4gk\_ .node circle,#chatgpt-mermaid-\_r_4gk\_ .node ellipse,#chatgpt-mermaid-\_r_4gk\_ .node polygon,#chatgpt-mermaid-\_r_4gk\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4gk\_ .rough-node .label text,#chatgpt-mermaid-\_r_4gk\_ .node .label text,#chatgpt-mermaid-\_r_4gk\_ .image-shape .label,#chatgpt-mermaid-\_r_4gk\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4gk\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4gk\_ .rough-node .label,#chatgpt-mermaid-\_r_4gk\_ .node .label,#chatgpt-mermaid-\_r_4gk\_ .image-shape .label,#chatgpt-mermaid-\_r_4gk\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4gk\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4gk\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gk\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gk\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4gk\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4gk\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4gk\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4gk\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4gk\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4gk\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4gk\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gk\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gk\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4gk\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gk\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4gk\_ .icon-shape,#chatgpt-mermaid-\_r_4gk\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4gk\_ .icon-shape p,#chatgpt-mermaid-\_r_4gk\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4gk\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4gk\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4gk\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4gk\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4gk\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4gk\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4gk\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4gk\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4gk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gk\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4gk\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4gk\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4gk\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gk\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4gk\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4gk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gk\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4gk\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4gk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gk\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4gk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gk\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4gk\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4gk\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4gk\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4gk\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4gk\_ .node rect,#chatgpt-mermaid-\_r_4gk\_ .node circle,#chatgpt-mermaid-\_r_4gk\_ .node ellipse,#chatgpt-mermaid-\_r_4gk\_ .node polygon,#chatgpt-mermaid-\_r_4gk\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4gk\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4gk\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4gk\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4gk\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gk\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Task APIPostgreSQL PrimaryWorker PoolRetry SchedulerRecovery SchedulerOutbox PublisherRead Replica 1Read Replica 2Task Status / ReportingAnalytics / Monitoring

### Stage 1: One primary

Initially:

```
PostgreSQL Primary

Tasks:
    1 million

Task attempts:
    5 million

Workers:
    100
```

One appropriately sized PostgreSQL instance might be sufficient.

The number of rows isn't the only factor. The write rate, working set, indexes, WAL volume, storage latency and query complexity matter.

Use:

- Primary-key lookup for task ownership.
- Partial indexes for queued, retry and expired tasks.
- Short transactions.
- Connection pooling.
- Small scheduler batches.
- Proper autovacuum configuration.

For example:

```
CREATE INDEX idx_tasks_retry_due
ON tasks (next_attempt_at)
WHERE status = 'RETRY_WAIT';

CREATE INDEX idx_tasks_expired_lease
ON tasks (lease_expires_at)
WHERE status = 'RUNNING';
```

### Stage 2: Separate read and write workloads

Suppose users continuously request:

```
GET /tasks/task-101
GET /tasks/task-102
GET /tasks/task-103
```

These are read-heavy requests.

We can serve suitable read queries through replicas.

The actual claiming and lease management must stay on the authoritative writer.

### Stage 3: Separate workload databases

Suppose the same PostgreSQL instance handles:

```
Application transactions
Background task operations
Analytics
Reporting
Audit history
```

Eventually, these workloads start competing.

We can separate them:

```
Application PostgreSQL
    |
    +-- Orders
    +-- Payments
    +-- Customers

Task PostgreSQL
    |
    +-- Tasks
    +-- Task Attempts
    +-- Outbox
    +-- Inbox
    +-- Workflow State
```

This reduces workload interference.

### Stage 4: Partition large tables

For example:

```
task_attempts_2026_10
task_attempts_2026_11
task_attempts_2026_12
```

Partitioning can make retention management and large-table maintenance easier.

The current task table can remain relatively small, while historical attempts are archived or partitioned.

### Stage 5: Sharding

Eventually, suppose one primary writer is no longer sufficient.

We can shard by tenant:

```
Tenant 1-1000:
    PostgreSQL Shard 1

Tenant 1001-2000:
    PostgreSQL Shard 2

Tenant 2001-3000:
    PostgreSQL Shard 3
```

All task records for a tenant go to the same shard.

This gives us independent write capacity across shards.

But it introduces:

- Shard routing.
- Cross-shard queries.
- Cross-shard transactions.
- Rebalancing.
- Operational complexity.

Don't introduce this until necessary.

Important: PostgreSQL table partitioning is not the same as sharding. Table partitioning organizes data inside a database; it does not automatically distribute writes across independent database servers.

## 17.2 Scaling DynamoDB

DynamoDB scales differently.

We don't normally scale it by adding PostgreSQL-style read replicas or increasing one primary writer's machine size.

Instead, we design appropriate partition keys and throughput.

\#chatgpt-mermaid-\_r_4ho\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4ho\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ho\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ho\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4ho\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ho\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4ho\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4ho\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4ho\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4ho\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4ho\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4ho\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ho\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ho\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4ho\_ p{margin:0;}#chatgpt-mermaid-\_r_4ho\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ho\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ho\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ho\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4ho\_ .label text,#chatgpt-mermaid-\_r_4ho\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ho\_ .node rect,#chatgpt-mermaid-\_r_4ho\_ .node circle,#chatgpt-mermaid-\_r_4ho\_ .node ellipse,#chatgpt-mermaid-\_r_4ho\_ .node polygon,#chatgpt-mermaid-\_r_4ho\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4ho\_ .rough-node .label text,#chatgpt-mermaid-\_r_4ho\_ .node .label text,#chatgpt-mermaid-\_r_4ho\_ .image-shape .label,#chatgpt-mermaid-\_r_4ho\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4ho\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4ho\_ .rough-node .label,#chatgpt-mermaid-\_r_4ho\_ .node .label,#chatgpt-mermaid-\_r_4ho\_ .image-shape .label,#chatgpt-mermaid-\_r_4ho\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4ho\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4ho\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ho\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ho\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4ho\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4ho\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ho\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ho\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ho\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4ho\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4ho\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ho\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ho\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4ho\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ho\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4ho\_ .icon-shape,#chatgpt-mermaid-\_r_4ho\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ho\_ .icon-shape p,#chatgpt-mermaid-\_r_4ho\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4ho\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4ho\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ho\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4ho\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4ho\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4ho\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4ho\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4ho\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4ho\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4ho\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ho\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ho\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ho\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4ho\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4ho\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4ho\_ .node rect,#chatgpt-mermaid-\_r_4ho\_ .node circle,#chatgpt-mermaid-\_r_4ho\_ .node ellipse,#chatgpt-mermaid-\_r_4ho\_ .node polygon,#chatgpt-mermaid-\_r_4ho\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4ho\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4ho\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4ho\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4ho\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ho\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Task APIDynamoDBWorker FleetTask SchedulerRecovery SchedulerStream PublisherDynamoDB StreamsKafka / SQS

### Stage 1: On-demand capacity

For an initially unpredictable workload, I would consider on-demand capacity.

For example:

```
Task volume:

Morning:
    500 tasks/second

Afternoon:
    5,000 tasks/second

Night:
    100 tasks/second
```

On-demand capacity reduces the need to continuously manage provisioned read and write capacity.

AWS documents automatic scaling and pay-per-request pricing for on-demand mode, although sudden traffic growth beyond previously established peaks can still require consideration and preparation.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon DynamoDB+1



### Stage 2: Partition-key distribution

Suppose we use:

```
PK = TASK#taskId
```

For example:

```
TASK#101
TASK#102
TASK#103
TASK#104
```

Task records are distributed based on their partition-key values.

This is generally a good approach because task IDs can be distributed widely.

But consider this GSI:

```
GSI partition key = RUNNING
```

Every running task writes to the same logical index partition key.

This is a bad design at high volume.

Instead:

```
RUNNING#SHARD#00
RUNNING#SHARD#01
RUNNING#SHARD#02
...
RUNNING#SHARD#31
```

This distributes the index traffic.

The same applies to retry scheduling.

### Stage 3: Separate base table and index workloads

Your base task item might be updated every 30 seconds.

But its lease index also needs to reflect the new expiration timestamp.

You therefore need to calculate capacity for:

```
Base task table writes.

Lease GSI writes.

Retry GSI writes.

Other GSI maintenance.
```

Don't forget that indexes aren't free.

### Stage 4: Capacity and throttling management

Monitor:

- Consumed read capacity.
- Consumed write capacity.
- Throttled requests.
- GSI throttling.
- Partition-level activity.
- Hot partition keys.
- Transaction conflicts.
- Conditional check failures.

Use on-demand or provisioned capacity with autoscaling depending on workload predictability and economics.

At large scale, you can also use different tables for operational tasks, audit history and high-volume event records rather than making one table handle every access pattern.

# 18. A direct comparison of scaling PostgreSQL and DynamoDB

| Scaling concern         | PostgreSQL                                  | DynamoDB                                                 |
| ----------------------- | ------------------------------------------- | -------------------------------------------------------- |
| Compute scaling         | Increase primary capacity                   | Managed distributed capacity                             |
| Read scaling            | Read replicas                               | Distributed reads                                        |
| Write scaling           | Primarily scale primary; shard when needed  | Distribute keys across partitions                        |
| Connection management   | Important                                   | No persistent SQL connection pool requirement            |
| Data partitioning       | Declarative table partitioning              | Built-in partition distribution                          |
| Sharding                | Additional architecture required            | Built into the data distribution model                   |
| Query flexibility       | Excellent                                   | Access patterns must be designed                         |
| Transactions            | Strong relational transactions              | ACID multi-item transactions                             |
| Hot-key problem         | Row contention and hot index pages          | Hot partition keys                                       |
| Indexes                 | Flexible B-tree, GIN, partial indexes, etc. | Carefully designed GSIs and LSIs                         |
| Operational maintenance | Vacuum, indexes, backups, capacity          | Capacity, key design, indexes, throttling                |
| Bursty traffic          | Requires sufficient headroom                | On-demand is useful                                      |
| Complex reporting       | Excellent                                   | Usually requires another read model or analytical system |

There is no universal winner.

But there is a practical distinction:

PostgreSQL makes complex state management easier. DynamoDB makes very large, predictable key-based workloads easier to distribute.

# 19. What about other databases?

PostgreSQL and DynamoDB are the two I would primarily evaluate.

But there are other options.

| Database                   | Would I use it here?           | Reason                                                                                                         |
| -------------------------- | ------------------------------ | -------------------------------------------------------------------------------------------------------------- |
| Aurora PostgreSQL          | Yes                            | Managed PostgreSQL-compatible database with AWS operational features                                           |
| MySQL / Aurora MySQL       | Yes                            | Good relational alternative, especially if already used                                                        |
| MongoDB                    | Sometimes                      | Useful if task data is naturally document-oriented and the team already operates it                            |
| Cassandra / ScyllaDB       | Specialized use cases          | High write throughput and distributed key-based access, but more involved state coordination                   |
| CockroachDB                | Specialized use cases          | Distributed SQL and multi-region transactional requirements                                                    |
| YugabyteDB                 | Specialized use cases          | Distributed PostgreSQL-compatible SQL architecture                                                             |
| Redis                      | No, not as primary task ledger | Excellent for caching, ephemeral coordination and rate limiting, but not a substitute for durable task history |
| Elasticsearch / OpenSearch | No, not as primary task ledger | Excellent for search and operational discovery, not authoritative transactional ownership                      |
| S3                         | Complementary                  | Excellent for large task inputs, outputs and archived artifacts                                                |

Some important clarifications:

### Aurora PostgreSQL

Amazon Aurora PostgreSQL

This is worth considering if you want PostgreSQL semantics with AWS-managed database infrastructure.

It can be a good default for an AWS-hosted task system.

It does not eliminate the need to design write throughput, lease updates, connection management and transaction patterns.

### CockroachDB / YugabyteDB

Consider distributed SQL if you have a genuine requirement for:

- Multiple geographical write regions.
- Distributed transactional data.
- High availability across regions.
- SQL semantics with horizontal distribution.

Don't use them simply because your task count becomes large.

### Cassandra / ScyllaDB

These can be suitable for huge write-heavy workloads with straightforward partition-key-based access.

However, our task system needs conditional claims, multi-item state changes, retry scheduling and ownership management.

They would require more deliberate modeling than PostgreSQL or DynamoDB.

### Redis

I might use Redis for:

- Caching frequently requested task statuses.
- Rate limiting workers.
- Best-effort coordination.
- Short-lived operational metadata.

But I would not use Redis alone as the source of truth for task ownership and durable execution history.

A Redis lock is not a replacement for the complete database lease, fencing and recovery design.

# 20. Can we use both PostgreSQL and DynamoDB in the same system?

Yes, technically and architecturally.

But I would not introduce both without a clear need.

Consider:

```
                  Task System
                      |
          +-----------+-----------+
          |                       |
          v                       v
     PostgreSQL                DynamoDB
          |                       |
    Core task state          High-volume lookup
    Retry management         Task status projection
    Attempts                 Read-heavy metadata
    Ownership                Fast key-based reads
    Outbox
```

There are cases where this makes sense.

For example:

PostgreSQL is the authoritative task database.

DynamoDB holds a denormalized, read-optimized representation of task status.

A user querying task status can read from DynamoDB:

```
GET /tasks/task-101
```

The core workflow and state transitions happen in PostgreSQL.

A CDC or event publisher updates the DynamoDB projection.

But now you have:

- Two copies of data.
- Synchronization delays.
- Eventual consistency.
- Additional failure scenarios.
- More operational cost.
- More complicated debugging.

You should only introduce this when PostgreSQL read workload genuinely needs to be isolated.

For most systems, starting with one authoritative database is better.

# 21. My recommended architecture for both flows

Now let's make an actual decision.

Assume we are building the systems we've been discussing:

- Background tasks.
- Long-running operations.
- Multiple worker instances.
- Lease management.
- Heartbeats.
- Retry policies.
- Task progress.
- External API integrations.
- Task history.
- Recovery from worker crashes.
- Reliable message publication.

Here is what I would choose.

## Option A: SQS-based task execution

\#chatgpt-mermaid-\_r_4j4\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4j4\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4j4\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4j4\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4j4\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4j4\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4j4\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4j4\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4j4\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4j4\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4j4\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4j4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4j4\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4j4\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4j4\_ p{margin:0;}#chatgpt-mermaid-\_r_4j4\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4j4\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4j4\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4j4\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4j4\_ .label text,#chatgpt-mermaid-\_r_4j4\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4j4\_ .node rect,#chatgpt-mermaid-\_r_4j4\_ .node circle,#chatgpt-mermaid-\_r_4j4\_ .node ellipse,#chatgpt-mermaid-\_r_4j4\_ .node polygon,#chatgpt-mermaid-\_r_4j4\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4j4\_ .rough-node .label text,#chatgpt-mermaid-\_r_4j4\_ .node .label text,#chatgpt-mermaid-\_r_4j4\_ .image-shape .label,#chatgpt-mermaid-\_r_4j4\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4j4\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4j4\_ .rough-node .label,#chatgpt-mermaid-\_r_4j4\_ .node .label,#chatgpt-mermaid-\_r_4j4\_ .image-shape .label,#chatgpt-mermaid-\_r_4j4\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4j4\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4j4\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4j4\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4j4\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4j4\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4j4\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4j4\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4j4\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4j4\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4j4\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4j4\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4j4\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4j4\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4j4\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4j4\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4j4\_ .icon-shape,#chatgpt-mermaid-\_r_4j4\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4j4\_ .icon-shape p,#chatgpt-mermaid-\_r_4j4\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4j4\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4j4\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4j4\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4j4\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4j4\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4j4\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4j4\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4j4\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4j4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4j4\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4j4\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4j4\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4j4\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4j4\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4j4\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4j4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4j4\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4j4\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4j4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4j4\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4j4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4j4\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4j4\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4j4\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4j4\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4j4\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4j4\_ .node rect,#chatgpt-mermaid-\_r_4j4\_ .node circle,#chatgpt-mermaid-\_r_4j4\_ .node ellipse,#chatgpt-mermaid-\_r_4j4\_ .node polygon,#chatgpt-mermaid-\_r_4j4\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4j4\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4j4\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4j4\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4j4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4j4\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Task APIPostgreSQLTransactional OutboxPublisherSQS StandardWorker FleetExternal APIs / S3Retry SchedulerLease Recovery Scheduler

Database: PostgreSQL

Why?

- Relational task lifecycle.
- Easy transaction management.
- Simple task ownership.
- Flexible retries and recovery.
- Attempt history.
- Easy monitoring and reporting.
- Transactional outbox.

When would I replace PostgreSQL with DynamoDB?

When the workload becomes extremely large and predominantly key-based, and PostgreSQL's write capacity or operational cost becomes the actual limiting factor.

DynamoDB is a viable choice from the beginning too, particularly for a serverless AWS architecture with predictable access patterns.

## Option B: Kafka-based event processing with long-running tasks

\#chatgpt-mermaid-\_r_4jb\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4jb\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4jb\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4jb\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4jb\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4jb\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4jb\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4jb\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4jb\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4jb\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4jb\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4jb\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4jb\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4jb\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4jb\_ p{margin:0;}#chatgpt-mermaid-\_r_4jb\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4jb\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4jb\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4jb\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4jb\_ .label text,#chatgpt-mermaid-\_r_4jb\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4jb\_ .node rect,#chatgpt-mermaid-\_r_4jb\_ .node circle,#chatgpt-mermaid-\_r_4jb\_ .node ellipse,#chatgpt-mermaid-\_r_4jb\_ .node polygon,#chatgpt-mermaid-\_r_4jb\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4jb\_ .rough-node .label text,#chatgpt-mermaid-\_r_4jb\_ .node .label text,#chatgpt-mermaid-\_r_4jb\_ .image-shape .label,#chatgpt-mermaid-\_r_4jb\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4jb\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4jb\_ .rough-node .label,#chatgpt-mermaid-\_r_4jb\_ .node .label,#chatgpt-mermaid-\_r_4jb\_ .image-shape .label,#chatgpt-mermaid-\_r_4jb\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4jb\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4jb\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4jb\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4jb\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4jb\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4jb\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4jb\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4jb\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4jb\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4jb\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4jb\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4jb\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4jb\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4jb\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4jb\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4jb\_ .icon-shape,#chatgpt-mermaid-\_r_4jb\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4jb\_ .icon-shape p,#chatgpt-mermaid-\_r_4jb\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4jb\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4jb\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4jb\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4jb\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4jb\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4jb\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4jb\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4jb\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4jb\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4jb\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4jb\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4jb\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4jb\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4jb\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4jb\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4jb\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4jb\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4jb\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4jb\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4jb\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4jb\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4jb\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4jb\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4jb\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4jb\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4jb\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4jb\_ .node rect,#chatgpt-mermaid-\_r_4jb\_ .node circle,#chatgpt-mermaid-\_r_4jb\_ .node ellipse,#chatgpt-mermaid-\_r_4jb\_ .node polygon,#chatgpt-mermaid-\_r_4jb\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4jb\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4jb\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4jb\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4jb\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4jb\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Producer ServicePostgreSQLOutboxKafka PublisherKafka TopicKafka Ingestion ConsumerTask CoordinatorWorker FleetExternal SystemsRetry SchedulerLease Recovery Scheduler

Database: PostgreSQL

Why?

- Kafka handles ordered event delivery.
- PostgreSQL handles durable task registration.
- Inbox deduplication handles Kafka replay.
- PostgreSQL handles retries and leases.
- Workers operate independently of Kafka consumer poll intervals.
- SQL sequence validation handles per-entity workflow ordering.

DynamoDB could replace PostgreSQL while keeping the same architectural separation.

# 22. The database choice I would make at different scales

These are recommendations, not fixed thresholds. Actual capacity depends on workload shape, transaction size, indexes, hardware, consistency requirements and operational expertise.

| System characteristics                                         | My choice                                                         |
| -------------------------------------------------------------- | ----------------------------------------------------------------- |
| Small task system, 100–1,000 tasks/sec                         | PostgreSQL                                                        |
| Medium system, several thousand tasks/sec                      | PostgreSQL, benchmarked and optimized                             |
| High-volume task system with complex state transitions         | PostgreSQL or DynamoDB, based on write profile and query needs    |
| Extremely high-volume independent tasks with simple key access | DynamoDB                                                          |
| Long-running workflows with rich status and history queries    | PostgreSQL                                                        |
| AWS-native serverless task processing                          | DynamoDB                                                          |
| Multiple independent Kafka event consumers plus workflow state | PostgreSQL or DynamoDB                                            |
| Multi-region transactional SQL requirements                    | Evaluate CockroachDB/YugabyteDB or another distributed SQL design |
| Event streaming without complex task state                     | Kafka itself may be sufficient                                    |

I would not use an arbitrary task-count threshold to migrate from PostgreSQL to DynamoDB.

For example, having 100 million historical task records does not automatically mean DynamoDB is required.

A well-indexed, partitioned PostgreSQL deployment can store substantial volumes.

Conversely, even a modest DynamoDB deployment can become expensive or inefficient if its access patterns are poorly designed.

# 23. How I would approach the design in an actual project

I would use the following progression.

Phase 1: Start with PostgreSQL + SQS Standard.

Implement:

- `tasks`
- `task_attempts`
- `outbox_events`
- Conditional task claiming.
- DB lease and fencing token.
- SQS visibility extension.
- Heartbeats.
- Retry scheduler.
- Lease recovery scheduler.
- Idempotent external operations.

Use managed PostgreSQL, such as RDS PostgreSQL or Aurora PostgreSQL, if AWS is your platform.

Phase 2: Measure database load.

Track:

- Database write throughput.
- Heartbeat write throughput.
- Claim latency.
- Completion latency.
- Transaction conflicts.
- Lock contention.
- Connection usage.
- WAL generation.
- Index and table bloat.
- Queue backlog and processing duration.

Phase 3: Optimize PostgreSQL.

Before migrating:

- Optimize indexes.
- Reduce unnecessary heartbeats.
- Batch scheduler operations.
- Pool connections.
- Archive historical data.
- Separate read-heavy workloads.
- Isolate task processing from other application workloads.

Phase 4: Reevaluate the database.

If key-based access is dominant, traffic is huge and unpredictable, and the system benefits from automatic horizontal distribution, evaluate DynamoDB.

If complex transactions, relational queries and reporting dominate, continue with PostgreSQL or a suitable distributed SQL design.

Phase 5: Introduce multiple databases only when justified.

For example:

```
PostgreSQL:
    Authoritative task state.

DynamoDB:
    Read-optimized task status projection.

S3:
    Large input/output files.

Kafka:
    Event distribution and replay.

SQS:
    Independent background job delivery.

Redis:
    Cache and rate limiting.
```

Each technology has a clear responsibility.

# 24. Final decision: What would I personally choose?

For the architecture we've designed, my default would be:

| Component               | Technology                 | Responsibility                         |
| ----------------------- | -------------------------- | -------------------------------------- |
| Task database           | PostgreSQL                 | Authoritative task state               |
| Task history            | PostgreSQL                 | Attempts and failure history           |
| Outbox / Inbox          | PostgreSQL                 | Reliable publication and deduplication |
| Background delivery     | SQS Standard               | Distribute independent tasks           |
| Ordered event streaming | Kafka, when required       | Event distribution and replay          |
| Large file storage      | S3                         | Task inputs and outputs                |
| Cache                   | Redis, optional            | Frequently accessed task metadata      |
| Monitoring              | CloudWatch / OpenTelemetry | Health and observability               |

I would make two important architectural decisions:

1. Don't introduce Kafka when all you need is reliable background task distribution. SQS is simpler.
2. Don't introduce DynamoDB just because you expect the application to scale. Start with the database that makes correctness and maintenance easiest, then move when measured workload requirements justify it.

My strongest recommendation: PostgreSQL is the better default for this particular task-state and workflow-management problem. DynamoDB is equally valid, and can become the better choice when you have enormous key-based write throughput, predictable queries and a strong reason to use its managed horizontal scaling.

## 25. Test your understanding with three design questions

These are useful because they force you to think about database selection independently of the messaging system.

Question 1:

You have 50,000 concurrent tasks, each renewing its database lease every 30 seconds. Workers mostly access tasks by task ID. You don't need complex reporting.

Would you choose PostgreSQL or DynamoDB? Why?

Question 2:

You have a payment workflow where every account has an ordered sequence of transactions. You need to update account balances, create ledger entries and record corresponding events atomically.

Would you choose PostgreSQL or DynamoDB? Why?

Question 3:

You have Kafka with 100 partitions, multiple consumer groups, and consumers that need to replay historical events. The actual task execution may take 2 hours.

Would you store processing state in Kafka, PostgreSQL or DynamoDB? Where would you manage task leases and retries?

The key skill is to reason from access patterns, consistency requirements, transaction boundaries and scaling characteristics, not from the popularity of the database.

### Official technical references

- [DynamoDB transactions](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/transactions.html?utm_source=chatgpt.com).
- [DynamoDB conditional writes and concurrency control](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/BestPractices_ImplementingVersionControl.html?utm_source=chatgpt.com).
- [DynamoDB secondary indexes](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/SecondaryIndexes.html?utm_source=chatgpt.com).
- [DynamoDB on-demand capacity and scaling](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/on-demand-capacity-mode.html?utm_source=chatgpt.com).
- [DynamoDB TTL](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/TTL.html?header=0\&pop-up=1\&utm_source=chatgpt.com).
- [AWS Transactional Outbox Pattern](https://docs.aws.amazon.com/en_en/prescriptive-guidance/latest/cloud-design-patterns/transactional-outbox.html?trk=article-ssr-frontend-pulse_little-text-block\&utm_source=chatgpt.com).
- [RDS PostgreSQL read replicas](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_PostgreSQL.Replication.ReadReplicas.html?utm_source=chatgpt.com).
