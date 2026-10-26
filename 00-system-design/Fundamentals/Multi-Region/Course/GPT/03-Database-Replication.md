# Chapter 3: Database Replication — Primary Databases, Read Replicas, and Backups

This chapter covers one of the most important parts of multi-region architecture: how to keep database data available when the region containing the primary database becomes unavailable, without silently losing important writes or corrupting business state.

We will use PostgreSQL as the main example, with cloud-managed database concepts that also apply to other database systems.

By the end of this chapter, you should understand where database copies live, how changes travel between them, when a replica can be promoted, why replication lag matters, and why backups remain necessary even when multiple regions have copies of the data.

## 3.1 First, understand the three different things people call a database replica

Consider an application with a PostgreSQL database in Mumbai and a second copy in Singapore.

People might describe the Singapore database as a replica, standby, or backup. These terms are not interchangeable.

| Concept          | Purpose                                                   | Typical behavior                                                               |
| ---------------- | --------------------------------------------------------- | ------------------------------------------------------------------------------ |
| Primary database | Owns the authoritative data and accepts the normal writes | Receives transactions and commits them                                         |
| Read replica     | Serves read queries using replicated data                 | Usually follows the primary; may lag behind it                                 |
| Standby database | Is prepared to take over if the primary fails             | Often follows the primary and may be promoted                                  |
| Backup           | Preserves recoverable historical data                     | Can restore data from an earlier point in time, depending on the backup system |

A standby can also be configured to serve read queries, so these categories can overlap. The important difference is the role each copy plays in the recovery design.

### Why these distinctions matter

Suppose your primary database contains an order created at 10:00 AM.

At 10:01 AM, an attacker or faulty application executes a query that accidentally deletes thousands of orders.

If the deletion is replicated immediately, the replica may also delete those orders.

A replica primarily helps maintain another copy of current data. A backup gives you a way to recover an earlier valid state.

Similarly, if your primary region fails, a read replica might be promotable to become a new primary. But promotion is a recovery operation; it does not automatically guarantee that every recent committed transaction was replicated.

Replication provides another copy. Backups provide historical recovery. Failover changes which database is authoritative.

Those are three separate responsibilities.

## 3.2 How replication actually works inside PostgreSQL

To understand replication properly, start with the database's internal mechanism: the Write-Ahead Log (WAL).

PostgreSQL uses WAL to record changes that can be used to recover database state. Before changes to the relevant data files are considered safely persisted, the corresponding WAL records must be written according to PostgreSQL's durability rules.

A replica can receive those WAL records and replay them to catch up with the primary.

### Step 1: The application updates the primary

Your Spring Boot service executes:

```
UPDATE customer
SET address = 'Berlin'
WHERE customer_id = 100;
```

PostgreSQL executes the transaction and generates WAL records representing the necessary database changes.

### Step 2: The primary commits the transaction

The primary records the transaction's commit in the WAL.

With the usual asynchronous replication configuration, PostgreSQL can acknowledge the commit to the application without waiting for a remote standby to confirm that it has received or replayed the relevant WAL.

The customer sees a successful response even though the replica might not yet contain the latest change.

### Step 3: WAL travels to the standby

A PostgreSQL standby can receive WAL through streaming replication or recover WAL from an archive.

Streaming replication sends WAL records without waiting for an entire WAL segment to fill. PostgreSQL streaming replication is asynchronous by default.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.postgresql.org\&sz=32)

PostgreSQL

+1



### Step 4: The standby replays the WAL

The standby applies the received changes to its own database state.

When the relevant WAL records have been replayed, a read query against that standby can see the updated customer address.

Here is the overall flow:

PostgreSQL StandbyPostgreSQL PrimarySpring Boot AppPostgreSQL StandbyPostgreSQL PrimarySpring Boot App#chatgpt-mermaid-\_r_2m2\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2m2\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2m2\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2m2\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2m2\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2m2\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2m2\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2m2\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2m2\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2m2\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2m2\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2m2\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2m2\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2m2\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2m2\_ p{margin:0;}#chatgpt-mermaid-\_r_2m2\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_2m2\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2m2\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2m2\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2m2\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2m2\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_2m2\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2m2\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2m2\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2m2\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_2m2\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2m2\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2m2\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2m2\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_2m2\_ .labelText,#chatgpt-mermaid-\_r_2m2\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2m2\_ .loopText,#chatgpt-mermaid-\_r_2m2\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2m2\_ .sectionTitle,#chatgpt-mermaid-\_r_2m2\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2m2\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_2m2\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2m2\_ .noteText,#chatgpt-mermaid-\_r_2m2\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_2m2\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_2m2\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_2m2\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_2m2\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_2m2\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_2m2\_ .actor-man circle,#chatgpt-mermaid-\_r_2m2\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_2m2\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2m2\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2m2\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2m2\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2m2\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2m2\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2m2\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2m2\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2m2\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2m2\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2m2\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2m2\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2m2\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2m2\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2m2\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2m2\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2m2\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2m2\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2m2\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Replication and replay may lagUPDATE customerGenerate WALCommit transactionCommit successfulStream WAL recordsReceive and replay WAL

The diagram illustrates asynchronous replication. Synchronous replication changes when the application receives its commit acknowledgment.

### A detail worth remembering

Replication does not necessarily mean that PostgreSQL executes the original SQL statement independently on the secondary.

With physical streaming replication, the standby replays WAL corresponding to changes at the database storage level. With logical replication, changes are represented as logical data modifications and applied to subscribed tables.

These are different mechanisms with different capabilities.

## 3.3 Physical replication versus logical replication

These concepts are especially important when deciding how to replicate PostgreSQL across environments.

### Physical replication

Physical replication reproduces the underlying database changes represented by WAL.

Conceptually:

```
Primary PostgreSQL
       |
       | WAL records
       v
Standby PostgreSQL
       |
       v
Replayed database state
```

Common characteristics:

- Used for standby databases and many PostgreSQL read-replica configurations.
- Replicates the physical database state rather than selecting arbitrary individual business rows.
- Can support standby promotion and database recovery.
- Requires compatible database versions and a suitable replication configuration.
- Is generally appropriate when you want another database copy to follow the primary.

### Logical replication

Logical replication publishes and applies logical changes, such as inserts, updates, and deletes, for selected tables.

Conceptually:

```
Primary PostgreSQL
       |
       | Logical change stream
       v
Subscriber PostgreSQL
       |
       v
Selected tables updated
```

Common characteristics:

- Can replicate selected tables rather than necessarily replicating the entire database.
- Can be useful for data migrations, selective synchronization, and integrating separate database environments.
- Can support certain database-version migration scenarios, subject to compatibility and configuration.
- Requires careful schema and replication configuration management.

Logical replication is not automatically a multi-master database solution. If two regions independently modify the same records, you still need a conflict-handling and ownership strategy.

For most straightforward primary-to-standby disaster recovery designs, investigate physical replication first. Consider logical replication when selective data movement or a different migration or integration requirement justifies it.

PostgreSQL's official documentation discusses physical WAL shipping, streaming replication, standby servers, and the separate capabilities of logical replication.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.postgresql.org\&sz=32)

PostgreSQL

+1



## 3.4 What does replication lag mean?

Replication lag is the delay between changes reaching the primary and the replica becoming sufficiently up to date.

Consider this timeline:

| Time         | Event                              |
| ------------ | ---------------------------------- |
| 10:00:00.000 | Primary commits order `O100`.      |
| 10:00:00.100 | Application receives success.      |
| 10:00:00.300 | Replica receives the relevant WAL. |
| 10:00:00.500 | Replica replays the commit.        |

In this example, the replica becomes aware of the transaction before it has fully applied the change. The relevant values are illustrative.

Now imagine that the replica falls 30 seconds behind during a heavy workload.

This creates two problems.

Problem 1: Stale reads. A query against the replica may return an old value even though the primary has already committed a newer value.

Problem 2: Potential data loss during failover. If the primary fails before the latest commits reach the replica, promoting the replica can omit those commits.

Replication lag therefore influences both user experience and disaster recovery.

### Why can lag increase?

Common causes include:

- Heavy write activity on the primary.
- Insufficient CPU, disk throughput, or I/O capacity on the replica.
- Network congestion or connection interruptions between regions.
- Replication being unable to keep up with WAL generation.
- Large transactions or bursts of write activity.
- Delayed replay on the standby.
- Replica configuration or resource constraints.

Cross-region replication adds network latency and dependency on inter-region connectivity.

### How should you monitor lag?

Do not rely on a single metric without understanding what it measures.

For PostgreSQL, relevant information can include primary WAL generation and send positions, standby receive and replay positions, replication connection state, and WAL retention. The `pg_stat_replication` view exposes information about connected standbys. AWS-managed PostgreSQL services expose additional replication metrics appropriate to their configuration.

For example, on the primary:

```
SELECT
    application_name,
    state,
    sync_state,
    sent_lsn,
    write_lsn,
    flush_lsn,
    replay_lsn
FROM pg_stat_replication;
```

This query shows selected replication details for connected standbys. It does not, by itself, give a complete or universally accurate measure of the age of every missing transaction.

For example:

- A standby can receive WAL but lag in replaying it.
- An LSN difference measures WAL position, not directly a number of seconds.
- A time-based lag metric can have limitations when a system is idle.
- A disconnected standby might not appear in the primary's connected-standby view.

Therefore, monitor replication connection state, send/write/flush/replay progress, available lag metrics, and the last known replication health. Set thresholds according to your RPO.

Important: an RPO of five seconds does not mean it is acceptable to ignore 30 seconds of replication lag because the database is usually faster. If the outage happens at that moment, the actual recovery might violate the objective.

## 3.5 Asynchronous versus synchronous replication

This is one of the most important decisions in database recovery design.

It determines whether the primary can acknowledge a write before a standby has confirmed the required replication progress.

### A. Asynchronous replication

The primary commits a transaction without waiting for the remote standby to confirm that transaction's WAL has been received or replayed.

ReplicaPrimaryAppReplicaPrimaryApp#chatgpt-mermaid-\_r_2nr\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2nr\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2nr\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2nr\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2nr\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2nr\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2nr\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2nr\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2nr\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2nr\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2nr\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2nr\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2nr\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2nr\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2nr\_ p{margin:0;}#chatgpt-mermaid-\_r_2nr\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_2nr\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2nr\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2nr\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2nr\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2nr\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_2nr\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2nr\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2nr\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2nr\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_2nr\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2nr\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2nr\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2nr\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_2nr\_ .labelText,#chatgpt-mermaid-\_r_2nr\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2nr\_ .loopText,#chatgpt-mermaid-\_r_2nr\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2nr\_ .sectionTitle,#chatgpt-mermaid-\_r_2nr\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2nr\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_2nr\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2nr\_ .noteText,#chatgpt-mermaid-\_r_2nr\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_2nr\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_2nr\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_2nr\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_2nr\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_2nr\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_2nr\_ .actor-man circle,#chatgpt-mermaid-\_r_2nr\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_2nr\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2nr\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2nr\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2nr\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2nr\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2nr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2nr\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2nr\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2nr\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2nr\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2nr\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2nr\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2nr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2nr\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2nr\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2nr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2nr\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2nr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2nr\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Commit orderPersist commit WALSuccessSend WALApply changes

The exact acknowledgment and persistence behavior depends on the primary's durability configuration, but the key point is that the application response does not wait for the remote replica.

Advantages

- Lower write latency.
- Less dependence on the remote region being reachable for every commit.
- Typically more tolerant of cross-region network latency.
- Suitable when the business can tolerate a nonzero recovery data-loss window.

Disadvantages

- Recent committed transactions can be missing if a primary fails before replication catches up.
- A promoted replica might have incomplete data relative to the previous primary.
- The system needs a defined strategy for identifying and reconciling potentially missing business operations.

Asynchronous replication is common in cross-region read-replica and disaster recovery designs.

### B. Synchronous replication

With synchronous replication, the primary waits for the required acknowledgment from a synchronous standby before acknowledging the transaction under the configured commit policy.

In PostgreSQL, the exact guarantee depends on the `synchronous_commit` setting and which standby acknowledgment mode is configured.

For example:

```
Application sends transaction
        |
        v
Primary writes the transaction's WAL
        |
        v
Primary waits for required standby acknowledgment
        |
        v
Primary acknowledges commit to the application
```

PostgreSQL supports modes that wait for the remote standby to write, flush, or replay the transaction's WAL, with different guarantees and performance implications. The term "synchronous" by itself does not mean that every query is already visible on every replica before the application receives a response.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.postgresql.org\&sz=32)

PostgreSQL

+1



Advantages

- Can provide stronger transaction durability against the failure of an individual primary, assuming the appropriate standby acknowledgment and failure assumptions.
- Reduces the window for losing acknowledged transactions during failover.
- Helps meet stringent data-loss objectives.

Disadvantages

- Adds latency to writes because the primary must wait for the required acknowledgment.
- Depends on the synchronous standby and network meeting the necessary availability requirements.
- Can delay or block commits if the required acknowledgment cannot be obtained, depending on configuration.
- Requires careful topology and failure-policy design.

### C. Why synchronous cross-region replication can be expensive

Suppose the primary is in Mumbai and the synchronous standby is in Frankfurt.

The database cannot acknowledge a transaction until the required replication acknowledgment travels across the inter-region path.

The cost is not just a few extra milliseconds in theory. The actual latency depends on network round-trip time, persistence behavior, load, congestion, and the selected acknowledgment policy.

For a high-throughput transactional system, this can meaningfully affect write latency and throughput.

This is why a common architecture uses strong synchronous durability within a region and asynchronous replication to a distant disaster recovery region.

That choice can meet local availability requirements while accepting a nonzero cross-region RPO. It is not appropriate for every business requirement, but it is often a practical trade-off.

### D. A subtle but critical distinction

Synchronous replication can provide stronger protection against data loss, but it does not automatically guarantee a zero-data-loss recovery in every conceivable disaster.

The result depends on which replica acknowledged the commit, what was persisted, whether that replica survives, how promotion is performed, and the failure model.

For example, if a transaction was acknowledged only after the standby flushed its WAL, that standby has a stronger durability guarantee than one that merely received the data into memory. But if the acknowledged standby is also destroyed and no surviving copy contains the transaction, the guarantee has limits.

Design principle: select the synchronization policy based on the actual failure scenarios you need to survive, not just the word "synchronous."

## 3.6 Can we make every write synchronous across multiple regions?

Technically, some database architectures support cross-region synchronous replication or distributed consistency mechanisms. Whether this is practical depends on the database, service, topology, workload, and required failure semantics.

Imagine three replicas distributed across separate regions.

A transaction needs acknowledgment from a defined subset of nodes before it can be considered committed.

You are effectively trading some combination of latency and availability for stronger durability or consistency guarantees.

During a network partition, the system must follow an explicit policy:

- Continue writes only where the required consistency conditions can be satisfied.
- Reject or delay writes where those conditions cannot be satisfied.
- Or allow looser synchronization and accept the associated risk of stale or conflicting state.

There is no general-purpose setting that delivers immediate global writes, zero latency, zero data loss, perfect consistency, and uninterrupted availability through every network partition.

This is why multi-region database selection should begin with the required transaction guarantees and failure model.

## 3.7 Read replicas: when to use them and how to avoid stale reads

A read replica is useful when you want to serve read traffic away from the primary.

Consider the following architecture:

\#chatgpt-mermaid-\_r_2q1\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2q1\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2q1\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2q1\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2q1\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2q1\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2q1\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2q1\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2q1\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2q1\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2q1\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2q1\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2q1\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2q1\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2q1\_ p{margin:0;}#chatgpt-mermaid-\_r_2q1\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2q1\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2q1\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2q1\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2q1\_ .label text,#chatgpt-mermaid-\_r_2q1\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2q1\_ .node rect,#chatgpt-mermaid-\_r_2q1\_ .node circle,#chatgpt-mermaid-\_r_2q1\_ .node ellipse,#chatgpt-mermaid-\_r_2q1\_ .node polygon,#chatgpt-mermaid-\_r_2q1\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2q1\_ .rough-node .label text,#chatgpt-mermaid-\_r_2q1\_ .node .label text,#chatgpt-mermaid-\_r_2q1\_ .image-shape .label,#chatgpt-mermaid-\_r_2q1\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2q1\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2q1\_ .rough-node .label,#chatgpt-mermaid-\_r_2q1\_ .node .label,#chatgpt-mermaid-\_r_2q1\_ .image-shape .label,#chatgpt-mermaid-\_r_2q1\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2q1\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2q1\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2q1\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2q1\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2q1\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2q1\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2q1\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2q1\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2q1\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2q1\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2q1\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2q1\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2q1\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2q1\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2q1\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2q1\_ .icon-shape,#chatgpt-mermaid-\_r_2q1\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2q1\_ .icon-shape p,#chatgpt-mermaid-\_r_2q1\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2q1\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2q1\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2q1\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2q1\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2q1\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2q1\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2q1\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2q1\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2q1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2q1\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2q1\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2q1\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2q1\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2q1\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2q1\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2q1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2q1\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2q1\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2q1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2q1\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2q1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2q1\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2q1\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2q1\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2q1\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2q1\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2q1\_ .node rect,#chatgpt-mermaid-\_r_2q1\_ .node circle,#chatgpt-mermaid-\_r_2q1\_ .node ellipse,#chatgpt-mermaid-\_r_2q1\_ .node polygon,#chatgpt-mermaid-\_r_2q1\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2q1\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2q1\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2q1\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2q1\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2q1\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Spring Boot ApplicationPostgreSQL PrimaryRead Replica: Region ARead Replica: Region BRead/Write Routing LogicReplicationReplication

The application can send writes to the primary and selected reads to replicas.

This can reduce read load on the primary and improve query latency when the replica is geographically closer to the user.

However, the application must classify read operations according to their consistency requirements.

### Example: product catalog

A user views a product description.

If the regional replica is a little behind, the result may still be acceptable. Product descriptions and other low-risk catalog data are often good candidates for eventually consistent reads, depending on the business.

### Example: account balance

A user deposits money and immediately checks their balance.

If the write goes to the primary and the subsequent read goes to a lagging replica, the user might see the old balance.

That is a much more serious problem.

### Example: order creation

A user places an order and receives an order ID.

They then immediately load the order details. If the order detail query goes to a lagging replica, the application might temporarily report that the order does not exist.

The application should generally read from the authoritative source after the write or use another mechanism that guarantees the read has caught up sufficiently.

### Practical read-routing policy

| Operation                        | Preferred approach                                                         |
| -------------------------------- | -------------------------------------------------------------------------- |
| Browse low-risk catalog data     | Regional replica or cache can be suitable                                  |
| Read immediately after a write   | Primary or a consistency-aware read path                                   |
| Check critical transaction state | Authoritative read or a properly designed consistency mechanism            |
| Generate analytics               | A replica or analytical data store may be suitable                         |
| Handle financial invariants      | Use the authoritative transactional model, not a potentially stale replica |

The exact policy should be based on business semantics, not a blanket rule that "all GET requests go to replicas."

Remember: an HTTP GET does not automatically mean that stale data is acceptable.

## 3.8 Database promotion: turning a replica into the new primary

Now imagine that Region A contains your primary PostgreSQL database and Region B contains a read replica.

The primary region becomes unavailable.

The replica cannot continue operating as a normal standby forever if the application needs to accept writes. At some point, the recovery process must promote it to be the new authoritative database.

### Before failover

\#chatgpt-mermaid-\_r_2qa\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2qa\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2qa\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2qa\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2qa\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qa\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2qa\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2qa\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2qa\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2qa\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2qa\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2qa\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qa\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qa\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2qa\_ p{margin:0;}#chatgpt-mermaid-\_r_2qa\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qa\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qa\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qa\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2qa\_ .label text,#chatgpt-mermaid-\_r_2qa\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qa\_ .node rect,#chatgpt-mermaid-\_r_2qa\_ .node circle,#chatgpt-mermaid-\_r_2qa\_ .node ellipse,#chatgpt-mermaid-\_r_2qa\_ .node polygon,#chatgpt-mermaid-\_r_2qa\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2qa\_ .rough-node .label text,#chatgpt-mermaid-\_r_2qa\_ .node .label text,#chatgpt-mermaid-\_r_2qa\_ .image-shape .label,#chatgpt-mermaid-\_r_2qa\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2qa\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2qa\_ .rough-node .label,#chatgpt-mermaid-\_r_2qa\_ .node .label,#chatgpt-mermaid-\_r_2qa\_ .image-shape .label,#chatgpt-mermaid-\_r_2qa\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2qa\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2qa\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qa\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qa\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2qa\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2qa\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2qa\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2qa\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2qa\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2qa\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2qa\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qa\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qa\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2qa\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qa\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2qa\_ .icon-shape,#chatgpt-mermaid-\_r_2qa\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2qa\_ .icon-shape p,#chatgpt-mermaid-\_r_2qa\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2qa\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2qa\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2qa\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2qa\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2qa\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2qa\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2qa\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2qa\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2qa\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qa\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2qa\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2qa\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2qa\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qa\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2qa\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2qa\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qa\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2qa\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2qa\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qa\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2qa\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qa\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2qa\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2qa\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2qa\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2qa\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2qa\_ .node rect,#chatgpt-mermaid-\_r_2qa\_ .node circle,#chatgpt-mermaid-\_r_2qa\_ .node ellipse,#chatgpt-mermaid-\_r_2qa\_ .node polygon,#chatgpt-mermaid-\_r_2qa\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2qa\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2qa\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2qa\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2qa\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qa\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Application APrimary AStandby BApplication BAsynchronous replicationCannot perform normal writes

### After successful promotion

\#chatgpt-mermaid-\_r_2qj\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2qj\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2qj\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2qj\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2qj\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qj\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2qj\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2qj\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2qj\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2qj\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2qj\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2qj\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qj\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qj\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2qj\_ p{margin:0;}#chatgpt-mermaid-\_r_2qj\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qj\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qj\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qj\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2qj\_ .label text,#chatgpt-mermaid-\_r_2qj\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qj\_ .node rect,#chatgpt-mermaid-\_r_2qj\_ .node circle,#chatgpt-mermaid-\_r_2qj\_ .node ellipse,#chatgpt-mermaid-\_r_2qj\_ .node polygon,#chatgpt-mermaid-\_r_2qj\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2qj\_ .rough-node .label text,#chatgpt-mermaid-\_r_2qj\_ .node .label text,#chatgpt-mermaid-\_r_2qj\_ .image-shape .label,#chatgpt-mermaid-\_r_2qj\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2qj\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2qj\_ .rough-node .label,#chatgpt-mermaid-\_r_2qj\_ .node .label,#chatgpt-mermaid-\_r_2qj\_ .image-shape .label,#chatgpt-mermaid-\_r_2qj\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2qj\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2qj\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qj\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qj\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2qj\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2qj\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2qj\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2qj\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2qj\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2qj\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2qj\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qj\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qj\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2qj\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qj\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2qj\_ .icon-shape,#chatgpt-mermaid-\_r_2qj\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2qj\_ .icon-shape p,#chatgpt-mermaid-\_r_2qj\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2qj\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2qj\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2qj\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2qj\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2qj\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2qj\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2qj\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2qj\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2qj\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qj\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2qj\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2qj\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2qj\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qj\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2qj\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2qj\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qj\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2qj\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2qj\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qj\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2qj\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qj\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2qj\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2qj\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2qj\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2qj\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2qj\_ .node rect,#chatgpt-mermaid-\_r_2qj\_ .node circle,#chatgpt-mermaid-\_r_2qj\_ .node ellipse,#chatgpt-mermaid-\_r_2qj\_ .node polygon,#chatgpt-mermaid-\_r_2qj\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2qj\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2qj\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2qj\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2qj\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qj\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Application BNew Primary BOld Primary AMust not accept competingwrites

The second diagram assumes Region A has failed or has been fenced off, and Region B has been safely promoted.

### What does promotion actually mean?

Promotion changes a standby's database role so that it can accept normal writes.

But promotion alone does not complete regional failover.

Your recovery procedure must also account for application connection routing, database credentials and endpoints, connection pools, background workers, and any other clients that depend on the old primary.

A managed database service may automate some of these actions. Others still require application or infrastructure configuration.

### The hardest question: how do you know which replica is safe to promote?

Suppose the primary has acknowledged 1,000 transactions, but the replica contains only the first 990.

If you promote the replica, ten acknowledged transactions may be missing.

The system must distinguish between:

- The primary being unreachable.
- The primary being genuinely unable to continue operating.
- A network partition that prevents communication with the primary.
- A replica that is sufficiently current for the recovery objective.
- A replica that is running but is too far behind to meet that objective.

If the old primary is merely unreachable from the failover controller but is still accepting writes, promoting another database without preventing conflicting writes can create split-brain.

A safe design needs a controlled write-ownership transition.

### A safe conceptual failover sequence

1. Detect the failure and evaluate its scope.
2. Determine whether the old primary can still accept writes.
3. Fence the old primary or otherwise establish that it cannot accept conflicting writes.
4. Select the best eligible standby according to the recovery policy.
5. Evaluate replication position and the associated RPO.
6. Promote the standby.
7. Redirect application connections to the new primary.
8. Recover or reconcile affected jobs and business operations.
9. Verify critical transactions and data integrity.
10. Monitor the new primary and preserve evidence needed for later reconciliation.

Not every environment requires a human to execute every step. Some steps can be automated safely, provided the automation accounts for failure ambiguity and the database's supported failover behavior.

### Why connection pools matter

Imagine that Spring Boot uses a connection pool such as HikariCP.

After a regional database promotion, existing connections may still point to the old database endpoint or hold broken network sessions.

The application may need to discard stale connections, refresh connection information, and retry certain failures.

But blindly retrying a transaction is unsafe when the client does not know whether the original transaction committed.

For example, the database could commit an order, but the response may be lost when the connection breaks. A retry could create a duplicate order unless the operation is protected by an idempotency key or another business-level uniqueness mechanism.

This is one reason database failover and application recovery must be designed together.

## 3.9 Why backups are different from replicas

Suppose you have the following architecture:

\#chatgpt-mermaid-\_r_2qs\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2qs\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2qs\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2qs\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2qs\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qs\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2qs\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2qs\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2qs\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2qs\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2qs\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2qs\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qs\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qs\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2qs\_ p{margin:0;}#chatgpt-mermaid-\_r_2qs\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qs\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qs\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qs\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2qs\_ .label text,#chatgpt-mermaid-\_r_2qs\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qs\_ .node rect,#chatgpt-mermaid-\_r_2qs\_ .node circle,#chatgpt-mermaid-\_r_2qs\_ .node ellipse,#chatgpt-mermaid-\_r_2qs\_ .node polygon,#chatgpt-mermaid-\_r_2qs\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2qs\_ .rough-node .label text,#chatgpt-mermaid-\_r_2qs\_ .node .label text,#chatgpt-mermaid-\_r_2qs\_ .image-shape .label,#chatgpt-mermaid-\_r_2qs\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2qs\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2qs\_ .rough-node .label,#chatgpt-mermaid-\_r_2qs\_ .node .label,#chatgpt-mermaid-\_r_2qs\_ .image-shape .label,#chatgpt-mermaid-\_r_2qs\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2qs\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2qs\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qs\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qs\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2qs\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2qs\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2qs\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2qs\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2qs\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2qs\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2qs\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qs\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qs\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2qs\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qs\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2qs\_ .icon-shape,#chatgpt-mermaid-\_r_2qs\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2qs\_ .icon-shape p,#chatgpt-mermaid-\_r_2qs\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2qs\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2qs\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2qs\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2qs\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2qs\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2qs\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2qs\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2qs\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2qs\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qs\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2qs\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2qs\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2qs\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qs\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2qs\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2qs\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qs\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2qs\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2qs\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qs\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2qs\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qs\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2qs\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2qs\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2qs\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2qs\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2qs\_ .node rect,#chatgpt-mermaid-\_r_2qs\_ .node circle,#chatgpt-mermaid-\_r_2qs\_ .node ellipse,#chatgpt-mermaid-\_r_2qs\_ .node polygon,#chatgpt-mermaid-\_r_2qs\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2qs\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2qs\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2qs\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2qs\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qs\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Primary DatabaseRegional ReplicaBackups and Transaction LogArchivesPromotable StandbyRestore to a SelectedRecovery PointContinuous replication

The replica is primarily designed to follow the primary. The backup system is designed to preserve recoverable versions of data.

They protect against different failure scenarios.

| Failure                                        | Replica alone sufficient?                                            | Backup or point-in-time recovery needed?                          |
| ---------------------------------------------- | -------------------------------------------------------------------- | ----------------------------------------------------------------- |
| Primary server failure                         | Often, if the replica is healthy and can be promoted                 | Not necessarily                                                   |
| Entire primary region failure                  | Only if the replica or its data remains accessible in another region | May be needed depending on the recovery design                    |
| Accidental deletion replicated to the replica  | Generally no                                                         | Yes, if you need to recover the earlier state                     |
| Malicious or faulty bulk update                | Not necessarily; the bad update may replicate                        | Usually needed to recover the prior valid state                   |
| Database corruption replicated to another copy | Not necessarily                                                      | An unaffected backup or other clean recovery source may be needed |
| Need to recover data from last Tuesday         | A current replica is not enough                                      | Historical backup or point-in-time recovery is needed             |

### A practical backup strategy

For a production PostgreSQL application, consider these elements:

Automated backups and point-in-time recovery (PITR). These allow recovery to a supported point in time within the configured retention window. With PostgreSQL, this commonly combines a base backup and the necessary WAL archives.

Scheduled snapshots. Useful for preserving a database state before migrations, major releases, or other significant changes.

Cross-region backup protection. Ensure that an entire regional outage cannot destroy every recoverable copy.

Retention and deletion policies. Define how long backups are retained and how they are removed.

Restore tests. Validate that backups actually restore into a usable database with the expected schema, data, configuration, and access controls.

For Amazon RDS, automated backups support point-in-time recovery, while manual snapshots can be retained and copied across regions. The exact behavior and retention options depend on the database service and configuration.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Relational Database Service

+1



### A critical security consideration

A backup is not useful merely because a console shows that a backup operation succeeded.

You need to establish that it can be restored, that you can access it when the original region is unavailable, and that the restored database is not automatically exposed to the same compromised credentials or deletion paths as the original system.

For important systems, backup access controls and recovery credentials should be designed as part of the disaster recovery plan.

## 3.10 How this maps to AWS: RDS, read replicas, and Aurora Global Database

Because the cloud service determines which replication and failover capabilities are available, distinguish the underlying database engine from the managed service.

### Option A: Amazon RDS for PostgreSQL

A common design is:

- An RDS PostgreSQL instance in Region A.
- A cross-region read replica in Region B.
- Automated backups and appropriate snapshot retention.
- A documented process to promote the replica if the primary region fails.
- Application routing and connection handling to reach the promoted database.

Amazon RDS supports cross-region read replicas for PostgreSQL, subject to its supported versions and regional availability.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Relational Database Service

+1



This is useful for regional disaster recovery and read scaling, but you must still define the recovery procedure, the tolerated replication lag, and how application connections move to the promoted instance.

### Option B: Amazon Aurora Global Database

Aurora Global Database is a different managed design for distributing Aurora database clusters across regions.

A typical setup has a primary Aurora cluster in one region and secondary clusters in other regions. The architecture is intended to support low-latency cross-region replication and regional recovery.

The important trade-off is that the secondary clusters in this model follow the primary rather than independently accepting normal writes to the same global database.

AWS documents managed switchover and failover procedures for Aurora Global Database. Its failover guidance emphasizes checking replication lag because asynchronous replication can mean that a secondary is behind the primary. It also notes that a promoted secondary may need configuration adjustments and that rebuilding the other secondary regions can take from minutes to hours, depending on the circumstances.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Aurora



### How should you choose?

| Requirement                                                                              | Starting point to investigate                                                                   |
| ---------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| Existing PostgreSQL application requiring a secondary recovery copy                      | RDS PostgreSQL cross-region read replica                                                        |
| Managed Aurora deployment requiring regional recovery and cross-region data distribution | Aurora Global Database                                                                          |
| Selective replication or a database migration with different data requirements           | PostgreSQL logical replication or a suitable migration mechanism                                |
| Globally distributed writes with strong consistency requirements                         | A database whose documented transaction and regional failure guarantees meet those requirements |

These options are not interchangeable. Evaluate the supported engine versions, region availability, replication guarantees, promotion process, backup behavior, costs, and connection-routing model before selecting one.

## 3.11 A concrete regional failure scenario

Let's bring the concepts together.

Your application uses:

- Spring Boot in Region A and Region B.
- PostgreSQL primary in Region A.
- PostgreSQL read replica in Region B.
- Regional Redis instances.
- Background workers and message queues.
- Global traffic routing.

Suppose Region A fails at 14:00.

### Scenario A: the replica is almost fully caught up

The recovery procedure fences the old primary, promotes the replica, redirects traffic, and validates critical application operations.

The surviving region can start accepting writes after the database and application are ready.

The exact recovery time depends on the detection mechanism, promotion process, connection handling, application capacity, and dependencies.

### Scenario B: the replica is significantly behind

The replica contains data only up to 13:59:20, while the primary acknowledged transactions through 14:00.

Promoting it immediately may lose roughly 40 seconds of recent changes.

The recovery team has to evaluate the business RPO, the available WAL, and whether another usable recovery source exists. It must also decide how to handle business operations that may have committed in the failed region but are absent from the promoted database.

For example, a customer may have received a successful payment response even though the corresponding order record did not survive in the promoted database.

You should not blindly retry the payment or assume that the transaction never happened. The payment provider's transaction status and idempotency mechanism may be necessary to reconcile the state safely.

### Scenario C: Region A recovers unexpectedly

The old primary returns after Region B has been promoted and begun accepting writes.

The old primary must not simply resume serving writes as though nothing happened.

It may contain a divergent transaction history. Depending on the database service and recovery process, it may need to be rebuilt or rejoined through a supported procedure.

This illustrates why failover, split-brain prevention, and failback need to be considered together.

## 3.12 Database replication: common pitfalls and best practices

| Pitfall                                             | Why it is dangerous                                                  | Better practice                                                    |
| --------------------------------------------------- | -------------------------------------------------------------------- | ------------------------------------------------------------------ |
| Treating a replica as a backup                      | Deletes and corruption can propagate                                 | Maintain independent recoverable backups                           |
| Ignoring replication lag                            | The replica may violate RPO or serve stale reads                     | Monitor replication progress and define thresholds                 |
| Promoting a replica without fencing the old primary | Both databases might accept conflicting writes                       | Establish authoritative write ownership before promotion           |
| Using replicas for every read                       | Immediate post-write reads might be stale                            | Route reads according to consistency requirements                  |
| Ignoring database capacity                          | A replica may not handle production traffic after failover           | Test full-load capacity and scale standby resources appropriately  |
| Assuming cloud-managed failover handles everything  | Applications and queues can still fail or retain stale connections   | Test the whole recovery process                                    |
| Blindly retrying failed writes                      | A transaction may have committed before its response was lost        | Use idempotency and reconcile uncertain outcomes                   |
| Never testing a restore                             | Backups may be unusable or recovery may be too slow                  | Test restoration and verify the resulting system                   |
| Forgetting WAL retention and replication slots      | A lagging or disconnected consumer can increase storage pressure     | Monitor WAL retention and configure capacity and cleanup carefully |
| Ignoring schema changes across replicas             | Replication or application compatibility can break during deployment | Plan migrations and compatibility across the replication topology  |

Two details deserve special emphasis.

First, a replica must be sized for the role it needs to perform. A low-capacity replica might serve read traffic successfully in ordinary conditions but fail to keep up with replication or production writes when promoted.

Second, replication lag must be treated as an operational signal, not merely a performance statistic. When a replica is intended for disaster recovery, excessive lag may directly indicate that the configured recovery objective is at risk.

## 3.13 A practical design checklist

Before introducing a cross-region database replica, write down the answers to these questions.

Data and recovery: What is the RPO? Which replica is eligible for promotion? How will the system recover writes that might be missing? Are historical backups protected outside the primary failure domain?

Consistency: Which reads may use replicas? Which operations require immediate read-after-write behavior? What happens when a transaction commits but the response is lost?

Failover: Who or what decides to promote the replica? How is the old primary fenced? How are connections and background workers redirected? How will failback work?

Operations: How are replication lag, database availability, WAL retention, storage, and connection saturation monitored? Has the full recovery process been tested?

Compliance and security: Are replica and backup locations permitted? Are encryption, credentials, access controls, and retention policies configured for the secondary region?

These decisions should be documented before relying on the replica for production disaster recovery.

## 3.14 Chapter 3: What you should remember

1. PostgreSQL physical streaming replication sends WAL records from the primary to a standby, which replays them.
2. Asynchronous replication usually lowers write latency but can lose recent acknowledged transactions during failover.
3. Synchronous replication can strengthen durability guarantees but may increase latency or reduce write availability when the required acknowledgments cannot be obtained.
4. A read replica can improve read scaling and geographic performance, but it may return stale data.
5. Promotion changes which database is authoritative; it does not, by itself, complete application failover.
6. A replica is not a substitute for a backup. Backups and point-in-time recovery protect against failures that simple replication cannot.
7. Split-brain prevention, idempotent operations, recovery testing, and failback are part of database design—not optional operational extras.

The central design principle: choose a replication model based on the consistency, durability, latency, and recovery guarantees your application requires.

Next: Chapter 4 — Data Consistency Across Regions

We will investigate what happens when a user writes data in one region and immediately reads it from another, how replication lag creates inconsistent results, and how to handle read-after-write consistency, concurrent updates, idempotency, ordering, split-brain, and conflict resolution.

We will also connect these concepts to PostgreSQL transactions, Redis caches, and asynchronous messaging.
