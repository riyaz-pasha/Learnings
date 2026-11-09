# Multi-Region System Design: From Fundamentals to Production Architecture

The difficulty you're facing is a very common transition in system design.

You already understand the building blocks:

- Add application servers to scale horizontally.
- Shard databases to distribute data.
- Replicate databases for availability.
- Add caches to reduce database load.
- Use queues to decouple services.
- Use CDNs to serve content closer to users.

But when you introduce multiple geographical regions, these components stop being independent.

Now you must answer questions such as:

- Which region owns a particular piece of data?
- Can two regions write to the same data simultaneously?
- When a user writes in India, how does a user in Europe see that change?
- Does every region have a complete copy of the database?
- What happens if a region fails while replication is in progress?
- Where should Redis, Kafka, and background workers run?
- How do you query information that is distributed across several regions?

The key to understanding all of this is to stop thinking of multi-region as simply deploying the same architecture in multiple locations.

Multi-region architecture is primarily a problem of data ownership, data placement, replication, and coordination.

Once these four concepts become clear, the rest of the architecture follows naturally.

We'll build the mental model progressively, like the evolution of a real system.

## Roadmap

We'll use these chapters to build one consistent understanding.

| Chapter                      | What we'll establish                                                              |
| ---------------------------- | --------------------------------------------------------------------------------- |
| 1. The foundation            | Regions, shards, replicas, ownership, and the different meanings of multi-region  |
| 2. Same data everywhere      | Global datasets, local reads, write routing, replication, and consistency         |
| 3. Different data per region | Regional ownership, regional shards, replicas, and disaster recovery              |
| 4. Cross-region queries      | Routing, fan-out, aggregation, global indexes, and search                         |
| 5. Caching                   | CDN, regional Redis, cache invalidation, and consistency                          |
| 6. Queues and events         | Regional queues, event replication, consumers, ordering, and duplicate processing |
| 7. Failures and recovery     | Failover, split-brain, RPO/RTO, promotion, and failback                           |
| 8. Complete architectures    | Applying everything to realistic interview problems                               |

Let's start with the most important chapter. Don't skip it: almost every confusing multi-region design decision comes from mixing up the concepts introduced here.

# Chapter 1 — The Foundation: Understand Data Placement First

## 1.1 First, understand what a region actually represents

Imagine you're building a global application.

Your users are distributed across:

- India
- Europe
- North America
- Southeast Asia

You deploy your application in three geographical regions.

[Microsoft global network - Azure | Microsoft Learn](https://images.openai.com/static-rsc-4/VdhBaCZtIZ8Oq4VJSxkyIQVC5vkUlESLp-uHMBmxuKLeOaZyQQ9HvijgQE_rcCtDdmyCfcwgSux_DQGpDNW1KAb0IrJtl1BZgaY1isz4EHsa9cEktSIS5BAgmDaAl1hswI7OjA8qrZt6L2z3GMBeTfEXuMQTPGD5M7YtjkA5d_Q?purpose=inline)

[learn.microsoft.com](https://learn.microsoft.com/en-us/azure/networking/microsoft-global-network?utm_source=chatgpt.com)

For example:

```
                   GLOBAL USERS
                        |
                 Global Load Balancer
                        |
          +-------------+-------------+
          |             |             |
          v             v             v
      INDIA REGION  EUROPE REGION  US REGION
          |             |             |
       App Servers   App Servers   App Servers
          |             |             |
       Database      Database      Database
          |             |             |
       Cache         Cache         Cache
       Queue         Queue         Queue
```

A region is a geographical deployment location containing infrastructure such as application servers, databases, caches, and queues.

A region usually contains multiple Availability Zones (AZs), which provide fault isolation within that region.

For example:

```
INDIA REGION
    |
    +--- Availability Zone A
    |       |
    |       +--- Application servers
    |       +--- Database replicas
    |
    +--- Availability Zone B
    |       |
    |       +--- Application servers
    |       +--- Database replicas
    |
    +--- Availability Zone C
            |
            +--- Application servers
            +--- Database replicas
```

Important distinction:

- Multi-AZ protects you against failures within a geographical region.
- Multi-region protects you against failures affecting an entire geographical region and can reduce geographical latency.

You don't automatically need multi-region just because your application has many users.

A well-designed multi-AZ deployment can be sufficient. Multi-region adds operational complexity, replication concerns, inter-region networking costs, and consistency trade-offs.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

AWS Prescriptive Guidance



## 1.2 The most important distinction: Sharding vs replication

This is the foundation of everything that follows.

Imagine you have 100 million users.

You want to store their information in a database.

There are two independent problems:

Problem A: The database is too large or busy.

You divide the data into smaller portions.

This is sharding.

Problem B: You need multiple copies of the data for availability or geographical reads.

You copy the data to other database nodes.

This is replication.

These solve different problems.

Sharding — divide the data

100 million users

Shard 1

Users 1–33M

Shard 2

Users 33–66M

Shard 3

Users 66–100M

Each shard owns a different portion of the dataset. Together, they contain all users.

Replication — copy the data

Shard 1

Database A

Copy of Shard 1

Database B

Copy of Shard 1

Database C

Copy of Shard 1

Each replica contains the same logical shard, although copies might temporarily differ while replication is catching up.

Remember these two statements:

> Sharding answers: Which database partition contains my data?

> Replication answers: Where else does a copy of that partition exist?

They are not alternatives. A database can be sharded and replicated simultaneously.

## 1.3 Now combine sharding and replication

Let's combine both.

Suppose we have 100 million users divided into three shards.

Each shard has three replicas.

```
                  GLOBAL DATASET
                  100M USERS
                      |
          +-----------+-----------+
          |           |           |
          v           v           v
        SHARD 1     SHARD 2     SHARD 3
          |           |           |
       +--+--+     +--+--+     +--+--+
       |  |  |     |  |  |     |  |  |
       R1 R2 R3    R1 R2 R3    R1 R2 R3
```

Notice something important:

There are three distinct shards, but nine physical database copies.

The logical dataset is still the same 100 million users.

Replication doesn't create additional logical users. It creates additional physical copies of existing users.

This same principle applies when you introduce regions.

## 1.4 The big question: What exactly are you distributing across regions?

There are two fundamentally different situations.

Situation A: Every region needs access to the same dataset.

Example: A social media application.

A user in India creates a public post. A user in Europe should be able to read it.

Situation B: Each region owns its own dataset.

Example: A banking or regional commerce application.

Customers in India have their data stored in India. Customers in Europe have their data stored in Europe.

These two situations require different architectures.

And there is a third situation that appears frequently in real systems:

Situation C: A hybrid of both.

Some data is global, while other data is owned by individual regions.

For example, an e-commerce application might have:

- A globally available product catalog.
- Regionally owned customer records.
- Regionally partitioned orders.
- Globally accessible product reviews.
- Region-specific payment and compliance information.

We'll understand all three.

## 1.5 Situation A: Same data must exist in multiple regions

Imagine this requirement:

A user in India publishes a post. Users in India, Europe, and the US should be able to read it.

What happens if we keep the database only in India?

```
INDIA                       EUROPE
  |                            |
 User A                       User B
  |                            |
  v                            |
 India Application             |
  |                            |
  v                            |
 India Database <--------------+
                       Read from India
```

This works functionally, but Europe must communicate with India to access the data.

The consequences are:

1. Higher network latency.
2. Dependence on India's availability.
3. Potentially expensive cross-region traffic.
4. A bottleneck if global traffic grows.

So we introduce database replicas in other regions.

```
                  INDIA DATABASE
                       |
                Replication Stream
                  /          \
                 /            \
                v              v
       EUROPE DATABASE      US DATABASE
```

Now each region has a local copy of the data.

A European user can read from the European database rather than travelling all the way to India.

But wait.

Which database should accept writes?

This is where the real multi-region design begins.

There are three major write models.

### Model 1: Single-writer, multiple-readers

A strong default for many systems

Choose one region as the owner of writes for a dataset.

For example:

```
                  INDIA
               WRITE LEADER
                    |
                DATABASE
                    |
         +----------+----------+
         |                     |
         v                     v
      EUROPE                  USA
      REPLICA                REPLICA
```

Suppose a user in India creates a post.

The write flow is:

```
1. User sends a request in India
             |
             v
2. India Application
             |
             v
3. India Primary Database
             |
             v
4. Write is committed
             |
             v
5. Change is replicated asynchronously
             |
        +----+----+
        |         |
        v         v
    Europe       USA
    Replica      Replica
```

When replication completes:

```
India DB:    Post 123 exists
Europe DB:   Post 123 exists
USA DB:      Post 123 exists
```

All three regions eventually contain the same data.

But they may not contain it at exactly the same instant.

That is the key property of asynchronous replication.

Now consider reads:

```
INDIA USER  --> India DB
EUROPE USER --> Europe DB
USA USER    --> USA DB
```

Each region reads locally.

This is called local reads with asynchronous cross-region replication.

The main advantage is low read latency.

The trade-off is that a user might temporarily read stale data from a replica.

We'll explore this in Chapter 2.

### Model 2: Multi-writer, multiple-readers

Requires explicit conflict handling

What if users in all regions should be able to write locally?

For example:

A user in India updates their profile in India, while a user in Europe updates another profile in Europe.

You could allow every regional database to accept writes.

```
        INDIA              EUROPE               USA
          |                  |                   |
       READ/WRITE          READ/WRITE           READ/WRITE
          |                  |                   |
       DATABASE            DATABASE             DATABASE
          |                  |                   |
          +------------------+-------------------+
                     Replication
```

This is called multi-leader or multi-writer replication.

It solves a latency problem: users don't have to send every write to a central region.

But now imagine two users editing the same record simultaneously.

```
                 USER A
                   |
                   v
              INDIA DB
              name = "Alex"
                   |
                   | Replication
                   |
                   v
              EUROPE DB
                   ^
                   |
                   | Local write
                   |
              name = "John"
                   ^
                   |
                 USER B
```

Which value is correct?

The system must have a conflict-resolution policy.

Possible approaches include:

- Last-write-wins.
- Version-based conflict detection.
- Optimistic concurrency control.
- Application-specific merging.
- Ownership rules that prevent conflicting writes in the first place.

Never assume multi-writer automatically means conflict-free.

For example, DynamoDB Global Tables offer multi-region eventual consistency with asynchronous replication and last-writer-wins conflict resolution in their MREC mode. Its newer MRSC mode offers a different consistency model with additional placement and latency constraints.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon DynamoDB+1



### Model 3: Globally coordinated strong consistency

What if a user writes something in India and the system must guarantee that a subsequent read in Europe immediately sees that committed value?

You need a stronger coordination mechanism.

One approach is quorum-based replication.

```
                WRITE REQUEST
                     |
                     v
                 DB LEADER
                     |
           +---------+---------+
           |         |         |
           v         v         v
        Replica   Replica   Replica
          A         B         C
           |         |         |
           +---------+---------+
                Quorum
                     |
                     v
               COMMIT WRITE
```

A quorum is a sufficient number of replicas agreeing to commit a write.

The exact quorum topology depends on the database.

The key trade-off is that replicas in distant regions introduce network round-trip latency.

Strong global consistency is possible, but it is not free.

Google Spanner, for example, uses read-write, read-only, and witness replicas with leader-based transaction processing and voting for writes. Its multi-region placement is designed around the latency and availability requirements of that coordination.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.cloud.google.com\&sz=32)

Google Cloud Documentation+1



The lesson: Don't use globally coordinated writes for everything just because you have multiple regions. Use them where the business rules actually require that guarantee.

## 1.6 Situation B: Different data belongs to different regions

Now consider a different requirement.

A banking application has customers in India and Europe.

Assume its policy requires Indian customer records to remain in India, and European customer records to remain in Europe.

You don't want every region to contain every customer's data.

Instead, assign data ownership by region.

```
                GLOBAL APPLICATION
                       |
              Regional Data Router
                  /         \
                 /           \
                v             v
             INDIA          EUROPE
             REGION          REGION
                |              |
             DB India       DB Europe
                |              |
          India customers  Europe customers
```

Now, a user from India gets routed to India, and a user from Europe gets routed to Europe.

This is not replication of the same complete dataset.

This is regional data partitioning.

Think of it as sharding at the geographical level.

For a customer:

```
customerId = 1001
homeRegion = INDIA
```

The router knows that customer 1001 belongs to India.

All requests for that customer's authoritative data should be sent to India.

Likewise:

```
customerId = 2001
homeRegion = EUROPE
```

This customer's authoritative data resides in Europe.

The database in each region can still be sharded internally.

For example:

```
                    GLOBAL
                       |
             +---------+---------+
             |                   |
          INDIA               EUROPE
             |                   |
       +-----+-----+       +-----+-----+
       |     |     |       |     |     |
      I-1   I-2   I-3      E-1   E-2   E-3
```

Here:

- I-1, I-2, I-3 are shards belonging to the India region.
- E-1, E-2, E-3 are shards belonging to the Europe region.

Each regional shard can have its own replicas for availability.

For example:

```
INDIA
  |
  +--- Shard I-1
  |       +--- Primary (AZ-A)
  |       +--- Replica (AZ-B)
  |       +--- DR Replica (Europe, if permitted)
  |
  +--- Shard I-2
          +--- Primary (AZ-A)
          +--- Replica (AZ-C)
          +--- DR Replica (another approved region)
```

The Europe database isn't automatically a copy of the entire India database.

It might contain only the disaster-recovery copy of India's data, if the relevant data residency rules permit that.

This distinction is essential:

A database replica does not have to be located in the same region as its primary, but its placement must satisfy your recovery, latency, and data residency requirements.

## 1.7 The three patterns side by side

Let's compare the three situations.

Pattern A — Globally replicated data

Same logical data everywhere

```
     India DB  <----> Europe DB  <----> US DB
         Same dataset in every region
```

Every region has a copy of the global dataset, or of the relevant replicated shards. Reads can be local. Writes depend on the chosen consistency model.

Pattern B — Regionally owned data

Different data in each region

```
     India DB              Europe DB
     India data            Europe data
         |                     |
     India replicas       Europe replicas
```

Each region owns a different portion of the dataset. Requests route to the data's home region.

Pattern C — Hybrid

Most realistic complex applications

```
           GLOBAL CATALOG
           replicated globally
                  |
        +---------+---------+
        |         |         |
      INDIA     EUROPE      US
        |         |         |
      Local     Local      Local
      Orders    Orders     Orders
        |         |         |
     Regional  Regional   Regional
     replicas  replicas   replicas
```

Global data is replicated, while region-owned business data remains partitioned by region.

## 1.8 The mental model you should always use

When a system design question mentions multiple regions, don't immediately draw three copies of the entire architecture.

Start with these four questions:

Question 1: What data exists?

Identify the logical datasets.

For example:

```
Users
Products
Orders
Payments
Posts
Messages
```

Question 2: Who owns each dataset?

Is it global, regionally owned, tenant-owned, or owned by a particular entity?

Question 3: Where must each dataset be available?

Does every region need the whole dataset? Only a subset? Or just a searchable summary?

Question 4: What consistency is required?

Can a region temporarily see stale information? Must a write be globally visible immediately? Can two regions update the same record?

Answering these four questions gives you most of the database architecture.

# Chapter 2 — Same Data Everywhere: How Reads, Writes and Replication Actually Work

Let's go deeper into the first architecture.

Imagine we are designing a global social media platform.

Requirements:

1. Users across the world can create posts.
2. Users across the world can read posts.
3. A post should be accessible from any region.
4. Reads should be fast.
5. The system should survive a regional failure.

We'll use India, Europe, and the US.

## 2.1 First decide the database topology

There are two important ways to distribute a globally shared dataset.

### Option A: Full dataset replicated in every region

```
                  GLOBAL DATASET
                  All 100M users
                       |
          +------------+------------+
          |            |            |
          v            v            v
        INDIA        EUROPE          US
          |            |             |
       All data     All data       All data
       copied       copied         copied
```

Every region contains all 100 million users.

Advantages:

- Any region can read any user's data locally.
- Simple read routing.
- Each region can continue serving reads independently if another region fails.

Disadvantages:

- Storage is multiplied by the number of regions.
- Writes must be propagated everywhere.
- Replication bandwidth grows with the amount of changed data and number of destinations.
- Conflict management becomes important if multiple regions accept writes.

This is appropriate when the dataset is manageable and global read availability is a priority.

But what if the database has 50 billion records?

Replicating every record everywhere may be unnecessarily expensive.

### Option B: Shard globally, then replicate each shard selectively

This is where sharding and replication become especially interesting.

Suppose:

```
Global dataset
     |
     +--- Shard 1
     +--- Shard 2
     +--- Shard 3
     +--- Shard 4
```

Instead of putting the entire database in every region, assign shards to regions and make copies where needed.

For example:

```
                   GLOBAL SHARD PLACEMENT

INDIA                  EUROPE                 USA

Shard 1 (Primary)      Shard 2 (Primary)      Shard 3 (Primary)
Shard 2 (Replica)      Shard 1 (Replica)      Shard 4 (Primary)
Shard 3 (Replica)      Shard 4 (Replica)      Shard 1 (Replica)
Shard 4 (Replica)      Shard 3 (Replica)      Shard 2 (Replica)
```

This is only an illustrative placement, not a universal layout.

The important point:

A global dataset can be sharded, and each shard can have its own replication topology.

For example, Shard 1 might have its primary in India and replicas in Europe and the US.

Shard 2 might have its primary in Europe.

Therefore, don't think in terms of one global database primary necessarily.

You can have many shard leaders, each with its own ownership and replication arrangement.

This gives you an important design dimension:

| Decision             | Example                  |
| -------------------- | ------------------------ |
| Shard ownership      | Shard 1 belongs to India |
| Primary location     | India                    |
| Replica locations    | Europe and US            |
| Read preference      | Nearest healthy replica  |
| Write destination    | Shard 1's primary        |
| Failover destination | An eligible replica      |

These decisions can be made independently for each shard.

## 2.2 How does a write happen when a user accesses a non-primary region?

This is the workflow you should be able to draw confidently in an interview.

Assume:

- Shard 1's primary is in India.
- Europe contains a replica of Shard 1.
- A European user wants to update a record belonging to Shard 1.

What happens?

Write workflow — single-writer replication

European user

Updates user profile

Europe application

Routing layer

Determines that this record belongs to Shard 1

Write owner

India — Shard 1 Primary

Authoritative write happens here

Commit successful

Replication stream

Europe replica

Eventually updated

US replica

Eventually updated

Notice an important detail.

The application handling the request and the database accepting the write do not have to be in the same region.

That is a fundamental multi-region concept.

The application can run close to the user, but it must send the write to the authoritative database.

You have two possible ways to organize routing:

Approach 1: Route the entire request to the home region.

```
Europe User
    |
Europe Global Load Balancer
    |
Route to India
    |
India Application
    |
India Primary DB
```

Approach 2: Execute the application locally and route only the database write.

```
Europe User
    |
Europe Application
    |
Route DB request to India
    |
India Primary DB
```

The second approach can be useful when most of the business processing happens locally.

In practice, you should avoid making one cross-region call for every small database operation. Batch operations, keep related data together, or move the complete command handling to its owner region when that reduces network round trips.

### What happens after the write commits?

The primary database usually maintains a transaction log, such as a write-ahead log.

Conceptually:

```
UPDATE users
SET name = 'John'
WHERE user_id = 101;
```

The database records the committed change in its durable log.

A replication mechanism transfers committed log changes or equivalent change records to other regions.

```
India Primary
    |
    | Committed change
    v
Replication mechanism
    |
    +-------------------+
    |                   |
    v                   v
Europe Replica       US Replica
```

The replica applies the changes locally.

This is how regional database replicas catch up.

The exact mechanism varies by database technology: physical log shipping, logical replication, change-data capture, or a managed global replication service.

## 2.3 The next question: When should the write response return?

There is a subtle but very important decision here.

Should India return success immediately after committing locally, or wait until Europe and the US also have the data?

Let's compare.

### Asynchronous replication

```
Write
  |
  v
India Primary
  |
  v
Commit locally
  |
  v
Return SUCCESS
  |
  +-----> Replicate to Europe
  |
  +-----> Replicate to US
```

The user gets a faster response.

But Europe might not have received the update yet.

For a short period:

```
India:   name = John
Europe:  name = Alex
USA:     name = Alex
```

Eventually:

```
India:   name = John
Europe:  name = John
USA:     name = John
```

This is eventual consistency.

The important property is not that replicas are always immediately identical, but that they converge when updates stop and replication succeeds.

### Synchronous or quorum-based replication

Another possibility:

```
Write
  |
  v
Primary
  |
  +------> Replica A
  |
  +------> Replica B
  |
  v
Wait for the required acknowledgements
  |
  v
Commit / Return SUCCESS
```

Now the write waits for a defined level of replication or quorum agreement.

The precise guarantee depends on the database protocol.

You should not interpret synchronous replication as necessarily waiting for every replica in every region. It might wait for a quorum or for a particular configured set of replicas.

The trade-off:

- Stronger durability and consistency guarantees.
- More coordination and latency.
- Writes can become unavailable when the required quorum is unreachable.

### Interview rule

Use asynchronous cross-region replication when:

- The application can tolerate some replication delay.
- Low write latency matters.
- A small recovery-point loss during disaster is acceptable, or other mechanisms protect committed data.

Consider synchronous global coordination when:

- Strong consistency is a business requirement.
- Global write latency is acceptable.
- The database supports a suitable distributed consensus topology.

Don't blindly make all cross-region replication synchronous.

## 2.4 If reads are local, can they return outdated data?

Absolutely.

Consider this situation:

```
User A: India

1. Updates profile name to "John".
2. India DB commits.
3. India returns success.
4. Europe has not received the update yet.
```

Immediately afterwards:

```
User B: Europe

1. Reads the same user's profile.
2. Europe DB returns "Alex".
```

This is a stale read.

So now we must answer an important product-level question:

What consistency does the application actually require?

There are several useful approaches.

### Approach A: Accept eventual consistency

For a social media feed, recommendations, counters, or analytics, a brief delay may be acceptable.

```
Write in India
     |
     v
Read in Europe
     |
     v
May return old value temporarily
     |
     v
Replication completes
     |
     v
Future read returns new value
```

### Approach B: Read your own writes

A user who has just updated their profile should immediately see the updated profile.

You don't necessarily need every user in the world to see that update immediately.

Solutions include:

- Temporarily route that user's subsequent reads to the writer region.
- Keep a session or entity affinity with the owning region.
- Read the authoritative primary after the write.
- Use version or replication-position tokens and wait until the chosen replica has caught up.

Example:

```
India User
    |
    v
Write profile version 12
    |
    v
Successful response
    |
    v
Read profile
    |
    +----> Primary, or replica known to have version 12
```

You can even return a version with the write response:

```
{
  "userId": 101,
  "version": 12
}
```

The read path can ensure it does not return a version lower than 12.

The actual implementation depends on the database's consistency and replication APIs.

### Approach C: Route critical reads to the primary

For example, consider an inventory purchase.

```
Product Inventory = 1
```

Two customers are trying to purchase the last unit.

You should not let both customers purchase based on an outdated regional replica.

The reservation or decrement must happen through an authoritative transaction or concurrency-control mechanism.

You can use local replicas for displaying approximate product availability, but the final purchase must be validated against authoritative state.

A useful distinction: Not every read needs strong consistency, but correctness-critical decisions must use an authoritative path.

## 2.5 What if every region wants to write locally?

Now let's consider the multi-writer architecture more carefully.

Imagine an application where users are spread globally and updates happen frequently.

We might want:

```
India User  -> India Database
Europe User -> Europe Database
USA User    -> US Database
```

This is attractive because writes are geographically local.

However, multi-writer requires us to understand the unit of conflict.

Suppose two independent users update their own profiles.

```
India DB:
User 101 -> Name = John

Europe DB:
User 202 -> Name = Mary
```

These changes don't conflict because they modify different records.

But suppose two regions update the same record:

```
India DB:
Account 101 -> Balance = 500

Europe DB:
Account 101 -> Balance = 300
```

A naive last-write-wins merge could lose an update.

This is why financial balances are a poor example of data that should be blindly replicated with multi-writer conflict resolution.

There are several design choices:

| Strategy            | How it works                                  | Suitable example                        |
| ------------------- | --------------------------------------------- | --------------------------------------- |
| Single-writer       | One authority accepts writes                  | Bank account balance                    |
| Entity ownership    | Each entity has a home region                 | User profile, order                     |
| Conflict resolution | Concurrent updates are reconciled             | Some collaborative or profile data      |
| CRDT-style merging  | Operations/state are designed to merge safely | Certain counters and collaborative data |
| Consensus           | Coordinate writes before commitment           | Strongly consistent distributed records |

One particularly useful pattern is single-writer per entity, rather than single-writer for the entire database.

For example:

```
User 101 -> India is write owner
User 202 -> Europe is write owner
User 303 -> USA is write owner
```

All three regions can accept local writes for their own entities.

Replication distributes the changes to other regions.

This gives you much of the write-latency advantage of active-active applications without allowing arbitrary simultaneous writes to the same entity.

Be careful: operations involving multiple entities owned by different regions may still require distributed coordination or a saga.

## 2.6 A real product example: Social media

Let's put this into a realistic workflow.

Suppose a user in India creates a post.

```
                  INDIA
                    |
                Create post
                    |
                    v
             India Post Service
                    |
                    v
             India Post Database
                    |
             Commit post 123
                    |
            +-------+-------+
            |               |
            v               v
        Replication      Event stream
            |               |
       +----+----+          |
       |         |          |
       v         v          v
    Europe      USA      Feed generation
    Replica     Replica
```

Users can read from local databases.

But there is one extra issue.

What about the followers' home feeds?

The post data being globally available does not mean every user's feed must also be stored in every region.

A feed is often a derived data structure.

It can be generated locally, materialized in the user's home region, or assembled at read time.

This illustrates an important principle:

Different datasets in the same application can use completely different multi-region patterns.

- Post data: globally replicated.
- User profile: potentially single-writer per user.
- Home feed: materialized separately by user or region.
- Like count: possibly asynchronously aggregated.
- Privacy settings: require an authoritative, carefully ordered evaluation.

Don't design the entire application around one database topology.

# Chapter 3 — Different Data in Different Regions: Regional Ownership and Backup

Now let's completely change the requirement.

Imagine an e-commerce platform operating in India, Europe, and the US.

Assume its business and regulatory requirements say:

- Indian customer data must remain in India.
- European customer data must remain in Europe.
- US customer data must remain in the US.
- Each region must continue operating independently.
- We need disaster recovery if an entire region goes down.

Now global replication of customer records is inappropriate.

We need regional ownership.

## 3.1 Establish regional data ownership

Our architecture:

```
                     GLOBAL ENTRY POINT
                             |
                    Regional Data Router
                    /         |         \
                   /          |          \
                  v           v           v
                INDIA       EUROPE        USA
                  |           |            |
               App + DB    App + DB     App + DB
                  |           |            |
               India       Europe        US
               Customers   Customers     Customers
               Orders      Orders        Orders
               Payments    Payments      Payments
```

Each region owns its own customers, orders, and payments.

This means:

```
customer_id = 101
home_region = INDIA
```

The Indian region is authoritative for customer 101.

The same applies to other regions.

## 3.2 Where do shards and replicas fit now?

This is where many people make a mistake.

They think:

"If data is different in each region, I don't need replication."

That is wrong.

Replication is still useful. You just replicate the regional data to protect it.

For example:

```
                     INDIA REGION
                          |
                    India Database
                          |
                +---------+---------+
                |                   |
                v                   v
          Shard I-1             Shard I-2
                |                   |
            +---+---+           +---+---+
            |       |           |       |
          AZ-A     AZ-B        AZ-A     AZ-B
          Primary  Replica     Primary  Replica
```

This protects against an Availability Zone failure.

But what if the entire India region becomes unavailable?

For that, you might add a disaster-recovery copy in another permitted region.

```
            INDIA                         DR REGION
              |                              |
       India Primary DB                      |
              |                              |
              +---- Asynchronous Replication-+
                                             |
                                       Standby DB
                                       India data
```

The DR region holds a backup or replica of India's data, subject to applicable residency rules.

Notice that we have not suddenly made the DR region the owner of Indian customer data.

India is still the logical owner.

The other region is only a recovery location.

### The difference between a replica and a backup

This is worth understanding separately.

| Replica                                                              | Backup                                            |
| -------------------------------------------------------------------- | ------------------------------------------------- |
| Usually continuously updated                                         | Created periodically or continuously as snapshots |
| Used for reads, failover, or availability depending on configuration | Used for point-in-time recovery                   |
| Can replicate accidental deletions or corrupted changes              | Can preserve earlier recoverable states           |
| Often designed for low RPO                                           | Depends on backup frequency and retention         |
| Doesn't necessarily protect against logical corruption               | Can help recover from logical corruption          |

A replica is not a substitute for backups.

A production system normally needs both.

## 3.3 How does the request work?

Consider an Indian user placing an order.

```
Indian Customer
      |
      v
India Load Balancer
      |
      v
India Application
      |
      v
India Order Service
      |
      v
India Order Database
      |
      +---- India Primary DB
      |
      +---- India replicas (AZ-level)
      |
      +---- DR replica (if permitted)
```

Everything is handled locally.

The user doesn't need to communicate with the European or US databases.

Now consider a European user.

Their data is processed in Europe.

The architecture is independent.

This is called a regional isolation model.

It reduces cross-region dependency and can support data residency requirements.

## 3.4 How do we know where the data belongs?

We need a routing mechanism.

There are two common approaches.

### Approach A: Store the region in entity metadata

For example:

```
{
  "customerId": 101,
  "homeRegion": "INDIA"
}
```

When a request comes in:

```
Request(customerId = 101)
       |
       v
Find homeRegion
       |
       v
INDIA
       |
       v
Route to India application/database
```

For frequently accessed routing information, we can store the region in:

- A globally available routing directory.
- A regional cache.
- A service-discovery or configuration system.
- A deterministic lookup based on a stable partitioning scheme.

The directory must not itself become a single point of failure.

Also, authorization and residency enforcement must happen independently of routing. A malicious client must not be able to simply change `homeRegion` in a request and access another region's data.

### Approach B: Deterministic region assignment

For example:

```
tenantId = 5001
```

A mapping function or maintained partition map determines its home region.

One possible assignment:

```
Tenant range             Home region

1 - 1,000,000            India
1,000,001 - 2,000,000    Europe
2,000,001 - 3,000,000    USA
```

In production, you'd normally choose a more flexible assignment strategy than fixed numeric ranges, because ranges can become imbalanced and reassignment requires care.

You could also use a consistent-hashing or directory-based approach.

The important requirement is that every service agrees on the same ownership mapping.

## 3.5 The most important distinction: Regional active-active vs database active-active

Suppose the application is deployed in all three regions.

Can all regions be active?

Absolutely.

```
INDIA APP           EUROPE APP           US APP
     |                   |                  |
     v                   v                  v
 INDIA DB             EUROPE DB          US DB
```

All three application deployments can handle traffic at the same time.

But each region only writes to its own authoritative data.

This is an important concept:

Active-active application deployment does not require active-active writes to the same database records.

It means multiple regions actively serve traffic.

Data ownership can still be single-writer per region or entity.

That distinction is extremely useful in interviews.

## 3.6 What happens when India goes down?

Let's walk through a failure.

Normal state:

```
INDIA REGION
  India Primary DB
       |
       | Async replication
       v
DR REGION
  Standby copy of India DB
```

Now India's entire region fails.

What happens?

```
1. Health checks detect India is unavailable.
                |
                v
2. Confirm that this is a regional failure,
   not a temporary network issue.
                |
                v
3. Determine whether the DR copy is healthy
   and sufficiently caught up.
                |
                v
4. Promote the DR database, if eligible.
                |
                v
5. Fence the old primary so it cannot
   continue accepting writes.
                |
                v
6. Update routing and write ownership.
                |
                v
7. Route eligible India traffic to DR.
```

The fencing step is critical.

Imagine the India region is actually alive but temporarily isolated from the global routing network.

If the DR region promotes its copy, and India continues accepting writes, both regions can accept updates.

You now have two primaries.

This is split-brain.

When connectivity returns, the databases may have diverged.

A safe failover mechanism must ensure only one valid writer is authorized for a given shard or entity.

Common techniques include:

- Leader leases or fencing tokens.
- Quorum-based leader election.
- Epoch or generation numbers.
- Explicit writer revocation.
- A controlled ownership transition.

Don't simply promote another database because a health check failed.

## 3.7 RPO and RTO: The two numbers that shape this design

These are interview essentials.

RPO — Recovery Point Objective

How much data are you willing to lose, measured in time?

Example:

```
RPO = 5 minutes
```

You are willing to lose at most five minutes of acknowledged data in a disaster.

With asynchronous replication, the amount of potentially lost data depends on replication lag and the state of the last durable records.

For example:

```
India Primary:
10:00 -> Write A
10:01 -> Write B
10:02 -> Write C
10:03 -> India fails

DR replica:
10:00 -> Write A
10:01 -> Write B
```

Write C may be lost when the DR is promoted.

RTO — Recovery Time Objective

How long can the service remain unavailable while recovering?

Example:

```
RTO = 10 minutes
```

You need the service restored within ten minutes.

An architecture with a continuously running standby and automated promotion may achieve a lower recovery time than one requiring backup restoration and a complete database rebuild.

| Recovery strategy  | Typical trade-off                                                    |
| ------------------ | -------------------------------------------------------------------- |
| Backup and restore | Lowest infrastructure cost, potentially higher RTO                   |
| Pilot light        | Core recovery infrastructure exists, capacity must be brought up     |
| Warm standby       | Reduced recovery time, standby capacity already running              |
| Active-active      | Fast regional traffic redirection, but substantially more complexity |

These aren't guaranteed recovery times. Actual RPO/RTO must be measured and tested.

### A practical decision

For an important system:

```
India Regional Primary
      |
      +---- Multi-AZ synchronous protection
      |
      +---- Cross-region asynchronous DR
      |
      +---- Point-in-time backups
```

This gives different layers of protection against different failure types.

### One more important scenario: What if data must never leave India?

Then a DR copy in Europe might be forbidden.

Your DR must be in an approved location, or your recovery objective must be designed around the permitted geography.

A disaster-recovery architecture cannot override data residency requirements.

# Chapter 4 — Cross-Region Queries: How Do We Query Data That Lives Everywhere?

Now we reach one of the most confusing areas.

Suppose the application has regional ownership:

```
India DB:   Indian customers and orders
Europe DB:  European customers and orders
USA DB:     US customers and orders
```

A customer asks:

"Show me all orders placed by customer 101."

Where is customer 101?

If the customer belongs to India, the request routes to India.

Simple.

But what if an internal business dashboard asks:

"Show me total sales across India, Europe, and the US."

Now the data is distributed.

This requires a different query pattern.

There are four major approaches.

## 4.1 Pattern 1: Route directly to the owning region

Use this when the queried entity has a known home region.

```
Request(customerId = 101)
             |
             v
      Region Directory
             |
             v
           INDIA
             |
             v
         India DB
             |
             v
        Customer result
```

This is the ideal approach for entity-specific lookups.

Examples:

- Get a customer's profile.
- Get the details of an order.
- Get a bank account's current balance.
- Update a user's address.

Don't broadcast these requests to every region.

## 4.2 Pattern 2: Scatter-gather or fan-out

Use this when you really need data from multiple regions.

Suppose you want global sales totals.

```
                 GLOBAL API
                     |
              Query Coordinator
                     |
          +----------+----------+
          |          |          |
          v          v          v
        INDIA      EUROPE       USA
        DB Query   DB Query    DB Query
          |          |          |
          v          v          v
        100M       200M       150M
          |          |          |
          +----------+----------+
                     |
                     v
                  Aggregate
                     |
                     v
              Global Total = 450M
```

This is called scatter-gather.

The coordinator:

1. Sends a query to each participating region.
2. Collects responses.
3. Aggregates the results.
4. Returns the final response.

For example:

```
{
  "indiaSales": 100000000,
  "europeSales": 200000000,
  "usSales": 150000000,
  "globalSales": 450000000
}
```

This is conceptually simple, but there are trade-offs.

Latency

The global response cannot be returned until the required regions respond or the request times out.

If:

```
India response  = 50 ms
Europe response = 120 ms
USA response    = 200 ms
```

Then the coordinator waits at least approximately 200 ms, plus its own processing and network overhead, if it requires all three responses.

Partial failure

What if Europe is down?

You must decide whether to:

- Fail the entire request.
- Return a partial result.
- Return an explicitly degraded result with an unavailable-region indicator.
- Use a previously generated aggregate.

Never silently present partial results as complete results.

Data transfer

Don't send millions of raw rows across the network just to calculate totals.

Push the aggregation to the regional databases.

Instead of:

```
Fetch all orders to coordinator
         |
         v
Calculate SUM in coordinator
```

Do:

```
SELECT SUM(amount)
FROM orders
WHERE created_at >= :start
  AND created_at < :end;
```

Each region returns a small aggregate.

Use scatter-gather for reasonably bounded queries, not as the default way every user request accesses global data.

## 4.3 Pattern 3: Global read models and indexes

This is one of the most valuable patterns in large-scale distributed systems.

Imagine the application needs a global search feature.

A user searches for:

```
"wireless headphones"
```

The actual product/catalog data might be distributed among multiple regions.

You could query all regional databases every time.

But that becomes expensive and slow.

Instead, build a global search index.

```
INDIA DB         EUROPE DB         USA DB
    |                |               |
    v                v               v
  CDC/Event       CDC/Event       CDC/Event
    |                |               |
    +----------------+---------------+
                     |
                     v
              Search Index Builder
                     |
                     v
              Global Search Index
                     |
                     v
              Search API
```

What is happening?

Whenever data changes in a source database:

1. The change is published through a change-data-capture or event pipeline.
2. A processor consumes the change.
3. It updates a searchable representation.
4. The global search index becomes available.

The index may contain only what is necessary:

```
{
  "productId": 987,
  "name": "Wireless Headphones",
  "category": "Electronics",
  "availabilityRegion": ["INDIA", "EUROPE"]
}
```

When a user searches, the search service queries the index rather than all source databases.

For sensitive records, store only permitted fields. An index is also a data store and must follow the same privacy and residency rules.

This pattern is called a read model or materialized view.

It is closely related to CQRS:

- Write model: authoritative business database.
- Read model: optimized representation for queries.

An essential point:

A search index is not the authoritative source of truth.

If a user selects an order, the application retrieves the actual order from its owning region and checks the current authoritative state.

You use the index to find candidates, not to perform every correctness-critical operation.

## 4.4 Pattern 4: Global analytics warehouse

Now imagine you need:

- Sales by region.
- Average order value globally.
- Weekly growth.
- Revenue per country.
- Historical business trends.

Do not make every analytics request fan out to production databases.

Instead:

```
India DB ----\
Europe DB ----+---- CDC / Data Pipeline
US DB --------/              |
                             v
                       Data Warehouse
                             |
                             v
                       Analytics Queries
                             |
                             v
                       Reports / Dashboards
```

This separates operational workloads from analytical workloads.

A practical architecture often uses:

| Requirement                      | Suitable solution                                                    |
| -------------------------------- | -------------------------------------------------------------------- |
| Fetch one entity                 | Route to home region                                                 |
| Search global catalog            | Global search index                                                  |
| Get a global real-time-ish total | Scatter-gather or maintained aggregate                               |
| Historical reports               | Data warehouse                                                       |
| Cross-region relational joins    | Avoid on hot paths; use a read model or controlled distributed query |

## 4.5 What about cross-region database joins?

Suppose:

```
India DB:
Customers

Europe DB:
Orders

USA DB:
Payments
```

And a query needs to join all three.

A naive design would perform a distributed join during every request.

This introduces:

- Cross-region network calls.
- Large data movement.
- Variable latency.
- Complex transaction semantics.
- Partial failure handling.
- Privacy and data residency concerns.

The better strategy is normally to design the data model around ownership and access patterns.

Options include:

1. Co-locate data that is commonly queried together.
2. Denormalize frequently needed information.
3. Maintain a materialized view.
4. Use asynchronous analytics pipelines.
5. Use a distributed SQL database only when distributed query and transaction capabilities are genuinely required.

An important interview insight:

Cross-region queries are a data-access problem. The solution is not always to query multiple databases. Often, the better solution is to change how the read data is maintained.

## 4.6 Pagination across regions

A subtle production issue: suppose you ask for the latest 20 orders globally.

Each region returns its latest 20:

```
India:  10:00, 09:58, 09:55, ...
Europe: 10:02, 09:59, 09:54, ...
USA:    10:01, 09:57, 09:53, ...
```

The coordinator needs to merge and sort the results.

You can't simply append the lists.

You also need stable pagination.

One approach is a cursor that represents the position in each regional result stream.

```
{
  "indiaCursor": "...",
  "europeCursor": "...",
  "usCursor": "...",
  "lastSeenTimestamp": "..."
}
```

This illustrates why global queries can become expensive even when each individual regional query is easy.

# Chapter 5 — How to Design Caches in Multi-Region Architectures

Now let's introduce caching.

A common mistake is to think:

"I have three regions, so I'll deploy the same Redis cluster globally."

That's not necessarily a good design.

We need to think about cache locality and ownership.

## 5.1 First, establish the cache hierarchy

Consider a globally accessible application.

A typical design:

```
                   USER
                     |
                     v
                  CDN / EDGE
                     |
                     v
               Global Load Balancer
                     |
          +----------+----------+
          |          |          |
          v          v          v
        INDIA      EUROPE       USA
          |          |           |
        App        App          App
          |          |           |
        Redis      Redis        Redis
          |          |           |
        DB         DB           DB
```

We have multiple cache layers.

Layer 1: CDN / Edge cache

Useful for:

- Images.
- Videos.
- Static JavaScript/CSS.
- Public product information.
- Publicly cacheable responses.

It is physically distributed near users.

Layer 2: Regional application cache

For example, Redis or an in-memory cache in each region.

Useful for:

- Frequently requested database records.
- Session or configuration information.
- Hot product metadata.
- Read models.
- Computed data.

Layer 3: Database's internal cache

The database itself also has buffer pools and caching.

These are independent layers with different lifecycles.

## 5.2 Should Redis be globally distributed?

Let's say you have:

```
Redis India
Redis Europe
Redis USA
```

The default design should be to keep each regional cache close to the application using it.

Why?

Suppose every request from Europe accesses Redis in India.

```
Europe Application
        |
        | Cross-region network call
        v
    India Redis
```

You have defeated the purpose of local caching.

A regional application should normally read its regional cache.

```
India App  -> India Redis
Europe App -> Europe Redis
US App     -> US Redis
```

But what if all three regions need the same cache entry?

There are two common solutions.

### Option A: Populate each region's cache independently

Example:

```
Global Product Record
        |
  Replicated to regions
        |
   +----+----+
   |    |    |
   v    v    v
 India Europe USA
 Cache Cache Cache
```

Each region fills its cache from its local database replica.

No global Redis request is required.

### Option B: Propagate invalidation events

Suppose product 101 changes.

```
India DB:
Product 101 updated
       |
       v
Outbox / Event Stream
       |
       v
ProductUpdated event
       |
   +---+---+
   |       |
   v       v
Europe   USA
Redis    Redis
Invalidated
```

Now the next read in Europe or the US reloads the value.

An event announcing a change does not necessarily need to carry the entire cache value. Sometimes an invalidation event is enough.

### A practical production rule

For most regional application caches:

- Keep caches local.
- Populate them from local authoritative or replicated data.
- Propagate invalidation or update events where necessary.
- Use TTL as a safety mechanism.
- Avoid requiring every request to consult a global cache cluster.

## 5.3 How do you handle cache invalidation?

Let's walk through it.

A user updates their profile in India.

```
1. UPDATE India DB
          |
          v
2. Transaction commits
          |
          v
3. Publish ProfileUpdated event
          |
          v
4. Replication / event propagation
          |
    +-----+-----+
    |           |
    v           v
Europe Redis  US Redis
Invalidate     Invalidate
```

But there's a subtle race condition.

Suppose:

1. Europe receives an event and invalidates its cache.
2. A request reads Europe DB before replication catches up.
3. The stale value gets inserted into Redis again.

Your invalidation happened, but the stale value was re-cached.

Possible solutions include:

- Wait for replica catch-up before allowing the new version into the cache.
- Use version-aware caching.
- Route reads to the authoritative source after invalidation.
- Propagate update payloads with ordered versions.
- Use short TTLs as a bounded-staleness safety net.

For example:

```
Cached profile:
userId = 101
version = 11

Update arrives:
userId = 101
version = 12
```

The cache must not overwrite version 12 with a delayed version 11 result.

Version checks are especially useful when asynchronous events can arrive late or out of order.

## 5.4 How should a cache key be designed?

Suppose you're caching customer 101.

A global cache key might look like:

```
customer:101
```

In a regional system, think carefully.

Does the key identify the global entity, or a regional representation?

For example:

```
india:customer:101
europe:customer:101
```

Or:

```
tenant:5001:customer:101:v12
```

The choice depends on ownership, authorization, and the nature of the cached data.

Don't put sensitive user data into globally shared caches simply because the same application runs in multiple regions.

Cache location and access policy must follow data placement and authorization rules.

# Chapter 6 — Queues and Event Streaming Across Regions

Now let's discuss queues.

This is an important part of multi-region architecture because people frequently confuse three different things:

1. Replicating database data.
2. Replicating events/messages.
3. Processing messages in different regions.

These are related, but they are not the same.

## 6.1 First, understand that queues are also regional

Suppose your application uses Kafka.

A simple regional setup:

```
INDIA REGION
    |
    +--- India Application
    |
    +--- India Kafka Cluster
    |
    +--- India Consumers

EUROPE REGION
    |
    +--- Europe Application
    |
    +--- Europe Kafka Cluster
    |
    +--- Europe Consumers

USA REGION
    |
    +--- US Application
    |
    +--- US Kafka Cluster
    |
    +--- US Consumers
```

Each region operates its own queue or event-streaming infrastructure.

Why?

Because applications should generally publish and consume locally.

You don't want:

```
Europe App
    |
    | Cross-region network dependency
    v
India Kafka
```

This introduces network latency and dependency on another region.

Instead:

```
Europe App -> Europe Kafka -> Europe Consumers
```

But now comes the important question:

What if an event generated in India must be consumed in Europe?

There are several patterns.

## 6.2 Pattern A: Local queue, no cross-region event replication

Suppose an Indian customer places an order.

Only Indian services need to process that order.

```
INDIA
  |
Order Service
  |
India Database
  |
India Queue
  |
  +--- Inventory Consumer
  |
  +--- Notification Consumer
  |
  +--- Shipping Consumer
```

There is no reason to propagate the order event to Europe or the US.

This is especially useful for regionally owned data.

The benefits:

- Low latency.
- Regional isolation.
- Simpler operations.
- Reduced cross-region transfer.
- Fewer unnecessary copies of sensitive information.

Don't replicate every event globally just because you have multiple regions.

## 6.3 Pattern B: Publish locally, then replicate events to other regions

Now suppose an Indian user creates a public post that must be available globally.

You might want a global event distribution architecture.

```
                         INDIA
                           |
                      Post Service
                           |
                      India Database
                           |
                      India Kafka
                           |
                    PostCreated event
                           |
                Cross-region replication
                     /             \
                    v               v
                Europe Kafka      US Kafka
                    |               |
                    v               v
               Feed Consumer   Feed Consumer
```

The India Kafka cluster is local to the producer.

The replication system transfers selected events to Europe and the US.

This is geo-replication of event streams.

With Kafka, Apache MirrorMaker 2 supports directional replication flows, including active/active and active/passive arrangements, topic replication, and consumer-offset checkpointing. It is important to distinguish this cross-cluster replication from replication among brokers inside an individual Kafka cluster.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://kafka.apache.org\&sz=32)

Apache Kafka+1



One way to think about the two levels:

```
Within a region:

Kafka Broker 1 <----> Kafka Broker 2
       |
   Same cluster
   Broker replication


Across regions:

India Kafka -----> Europe Kafka
       |
   Cross-cluster
   Geo-replication
```

These are separate operations with different failure and consistency characteristics.

## 6.4 Pattern C: Centralized queue with regional consumers

Another possible design:

```
                GLOBAL EVENT PLATFORM
                         |
              +----------+----------+
              |          |          |
              v          v          v
            India      Europe       USA
           Consumer   Consumer   Consumer
```

Here, producers in multiple regions publish to a central queue.

This can work for certain workloads, but you must understand the trade-offs:

- Every producer may incur cross-region network latency.
- Central queue failure can affect all regions.
- Data residency may prevent centralization.
- The central event system can become a bottleneck.

Sometimes it is appropriate, especially for non-latency-sensitive central workflows or global coordination.

It should not be your automatic default.

## 6.5 How do we prevent database and queue inconsistency?

This is one of the most important production topics.

Imagine this operation:

```
1. Save order in database.
2. Publish OrderCreated to Kafka.
```

What happens if the database commit succeeds but Kafka publication fails?

```
India Database:
Order 101 exists

India Kafka:
OrderCreated event missing
```

Now your downstream consumers don't know the order exists.

Reverse the order:

```
1. Publish OrderCreated to Kafka.
2. Save order in database.
```

Now what if Kafka succeeds, but the database transaction fails?

You have an event for an order that does not exist.

This is the dual-write problem.

### Solution: Transactional Outbox Pattern

Store the business record and the event to be published in the same database transaction.

```
                 Order Service
                       |
                       v
                Database Transaction
                       |
                +------+------+
                |             |
                v             v
             Orders         Outbox
             Table          Table
                |             |
                +------+------+
                       |
                    COMMIT
                       |
                       v
                 Outbox Publisher
                       |
                       v
                   Kafka
```

Example:

```
BEGIN;

INSERT INTO orders(id, customer_id, amount)
VALUES (101, 5001, 2000);

INSERT INTO outbox(event_id, event_type, payload)
VALUES (
  'evt-101',
  'OrderCreated',
  '{"orderId":101}'
);

COMMIT;
```

Now either both records commit, or neither commits.

A separate process reads the outbox and publishes events to Kafka.

After publication, the outbox record can be marked as processed or retained according to your cleanup strategy.

Important: This does not make the entire database-to-Kafka pipeline exactly-once automatically.

For example:

1. Publisher sends the event.
2. Kafka accepts it.
3. Publisher crashes before marking the outbox row as published.
4. Publisher restarts and sends the event again.

Therefore, consumers must be idempotent.

### Idempotent consumers

An event should contain a unique ID:

```
{
  "eventId": "evt-101",
  "eventType": "OrderCreated",
  "orderId": 101
}
```

The consumer records which event IDs it has already processed.

```
Receive event evt-101
       |
       v
Already processed?
   /       \
 Yes        No
  |          |
Ignore     Process
             |
             v
         Save event ID
```

Ideally, the consumer's business operation and deduplication record are committed atomically.

This makes duplicate delivery safe.

## 6.6 How should events flow in the same-data vs different-data architectures?

Let's compare.

| Scenario                         | Queue architecture                                 |
| -------------------------------- | -------------------------------------------------- |
| Region-owned order events        | Local queue                                        |
| Global public post events        | Local producer + selected cross-region replication |
| Cross-region analytics           | Events/CDC into a central analytical pipeline      |
| DR for queue infrastructure      | Replicate required topics to standby cluster       |
| Regional notification processing | Local queue and local consumers                    |
| Global workflow coordination     | Explicit cross-region event or command routing     |

For a global event system, avoid blindly replicating everything.

Create replication policies by event category and destination.

For example:

```
OrderCreated:
  India -> India consumers
  Europe -> Europe consumers
  US -> US consumers

PublicPostCreated:
  India -> All regions
  Europe -> All regions
  US -> All regions

SensitivePaymentDetailsChanged:
  Publish only to permitted destinations
```

## 6.7 What about ordering across regions?

Suppose:

```
Event 1: AccountCreated
Event 2: AccountUpdated
Event 3: AccountDisabled
```

These events must be processed in the correct order for that account.

Within a Kafka partition, ordering is preserved for records written to that partition.

But do not assume global ordering across regions.

Two independently active producers can generate events concurrently.

A useful strategy is to:

- Route events for the same entity through its owning region.
- Partition by entity ID.
- Include event version or sequence information.
- Reject or defer out-of-order updates when necessary.
- Make processing idempotent.

Example:

```
{
  "eventId": "evt-300",
  "entityId": 101,
  "version": 14,
  "eventType": "AccountUpdated"
}
```

The consumer can track the last applied version and determine whether the incoming event is applicable.

The key idea: Global event replication does not automatically provide global ordering or global exactly-once execution.

# Chapter 7 — Regional Failures, Failover and Recovery

Now let's connect everything.

A multi-region architecture isn't complete until we understand how it behaves during failures.

Imagine India, Europe, and the US are all serving traffic.

```
         INDIA            EUROPE             USA
           |                 |                |
         APP + DB          APP + DB         APP + DB
           |                 |                |
       Queue + Cache      Queue + Cache    Queue + Cache
```

Let's examine different failure scenarios.

## 7.1 Failure scenario A: Application servers in India fail

Database is healthy.

```
INDIA
  |
  X Application servers unavailable
  |
  v
  Database healthy
```

This is relatively straightforward.

Global traffic routing detects that the India application endpoint is unhealthy.

It routes traffic to another healthy application region.

But the data ownership rule still applies.

If an Indian user's record is owned by India's database, a European application can potentially handle the request and communicate with India's database.

It does not automatically mean the user's database must be promoted or relocated.

This is why application availability and data availability should be treated separately.

## 7.2 Failure scenario B: India's database primary fails

Suppose:

```
India Primary
     |
     +---- India Standby Replica (AZ-B)
     |
     +---- DR Replica
```

The primary fails but the region is still alive.

The database can promote a suitable in-region replica.

```
Old Primary (failed)
        |
        X

AZ-B Replica
        |
        v
New Primary
```

This is a database failover within the region.

The application may only experience a short database interruption.

Depending on the database system, promotion can be automatic or require an operator.

## 7.3 Failure scenario C: Entire India region goes down

Now everything in India is unavailable.

```
INDIA                    EUROPE
  |                         |
  X Region failure          |
                            |
                            v
                    India DR database copy
```

You perform the disaster-recovery workflow we covered in Chapter 3:

1. Detect and assess the failure.
2. Verify the DR copy's health and replication progress.
3. Establish exclusive write authority.
4. Promote the replica.
5. Update service routing and ownership metadata.
6. Re-establish queues, caches, and dependent services.
7. Resume traffic.

The important part is that failover has to include more than the database.

You need the application, network routing, event consumers, scheduled jobs, credentials, secrets, and supporting services to be available in the recovery location.

## 7.4 Failure scenario D: Cross-region network partition

This is particularly dangerous.

Imagine:

```
INDIA                    EUROPE
  |                         |
  |                         |
  +----X Network X----------+
```

India and Europe cannot communicate.

But both regions might still be running.

If both accept writes to the same database records, you can get conflicting versions.

There are three broad choices:

Choice 1: Single-writer with strict ownership

The home region remains the only valid writer. Other regions may reject writes or route them through another valid path.

Choice 2: Quorum-based consistency

Only a side that can obtain the required quorum can commit writes.

The isolated side may become unavailable for writes.

Choice 3: Multi-writer with conflict resolution

Both sides accept writes, and the system reconciles divergent changes later.

This requires an explicit merge strategy and is appropriate only for data whose operations can be reconciled safely.

For financial transfers or inventory reservations, this is generally not something to solve with a naive merge.

## 7.5 Never forget split-brain prevention

Let's look at the dangerous situation one more time.

```
               NORMAL
                 |
        India = Primary
        Europe = Replica
                 |
                 v
           Network failure
                 |
          +------+------+
          |             |
          v             v
     India thinks    Europe promotes
     it is primary   itself to primary
          |             |
          v             v
       Writes         Writes
          |             |
          +------+------+
                 |
                 v
            CONFLICT
```

You now have two independent histories of the same data.

The recovery protocol must prevent or reconcile this.

A practical mechanism is a monotonically increasing fencing epoch.

For example:

```
Current owner: India
Ownership epoch: 10
```

After a controlled failover:

```
Current owner: DR region
Ownership epoch: 11
```

Services and database write paths reject operations using the obsolete epoch.

This is a conceptual illustration; actual fencing must be enforced by a trusted coordination system or the storage/write path, not merely by a value checked in an unreliable application cache.

## 7.6 Active-active vs active-passive: The complete picture

Let's compare them carefully.

Active-passive

```
INDIA                  EUROPE

App ACTIVE             App STANDBY
DB PRIMARY             DB REPLICA

       Replication -------->
```

Normal operation: India serves the application and owns the writes.

During failure: Europe is promoted to active after recovery procedures.

Simpler data ownership

Active-active

```
INDIA                  EUROPE

App ACTIVE             App ACTIVE
DB PRIMARY             DB PRIMARY
       <---- Replication ---->
```

Both regions serve traffic.

The database can still use single-writer per entity, or use multi-writer conflict resolution.

More coordination and conflict concerns

A useful nuance:

There are several independent axes:

| Component       | Possible deployment                      |
| --------------- | ---------------------------------------- |
| Application     | Active-active                            |
| Database        | Single-writer / multi-writer             |
| Regional shards | Each independently owned                 |
| Replicas        | Synchronous / asynchronous               |
| Cache           | Independent regional caches              |
| Queue           | Local clusters / geo-replicated clusters |
| Traffic routing | Active-active / primary-secondary        |

You don't have to pick one global "active-active" label and apply it to everything.

For example, a system can have:

```
Applications: Active-active
Databases: Single-writer per shard
Queues: Active-active regional producers
Caches: Independent local caches
Replication: Asynchronous cross-region
```

This is a perfectly reasonable architecture for many applications.

# Chapter 8 — Complete Real-World Architecture Patterns

Now that you understand the individual components, let's put them together.

The purpose of this chapter is to help you recognize the right pattern when an interviewer gives you a new system design question.

## 8.1 Example A: Global social media platform

Requirements:

- Users create posts from anywhere.
- Everyone can read public posts.
- User profiles are globally accessible.
- Home feeds should load quickly.
- The platform needs regional disaster recovery.

One possible architecture:

[Vanguard’s global multi-region approach | by Vanguard Tech | Vanguard Tech | Medium](https://images.openai.com/static-rsc-4/PtmKkycMbci2jPOLTrD4bCOv_phYiRyww6OBljqirVh33UxOP4U5zJo1d-3bKiAUnxm0cHo0_Ps6mdR4k6cC0Na0bx9r1H9uGl1Xo0hlKeARQDaCXMpq5dw4jeZ507S0ljXBKlJT47bWnRdV5xu7DaqW_r3iXYrh9QBXBIuEj0s?purpose=inline)

[medium.com](https://medium.com/vanguard-technology/vanguards-global-multi-region-approach-c7bb22e7317a?utm_source=chatgpt.com)

```
                      GLOBAL USERS
                           |
                    Global Load Balancer
                           |
             +-------------+-------------+
             |             |             |
             v             v             v
           INDIA         EUROPE          USA
             |             |             |
          App Layer     App Layer     App Layer
             |             |             |
          Local Redis   Local Redis   Local Redis
             |             |             |
          Database      Database      Database
             |             |             |
             +-------------+-------------+
                           |
                  Event Replication
                           |
                  Feed Processing
                           |
                  Regional Read Models
```

Possible data ownership:

| Dataset       | Architecture                                                     |
| ------------- | ---------------------------------------------------------------- |
| Public posts  | Globally replicated or distributed by owner with regional copies |
| User profiles | Single-writer per user, with replicated read copies              |
| Followers     | Sharded by user or relationship ownership                        |
| Home feed     | Regionally materialized                                          |
| Likes         | Entity-owned writes and distributed aggregation                  |
| Notifications | Local queues and region-specific consumers                       |
| Analytics     | Central/global analytical read model, subject to policy          |

Why?

Not every entity needs the same topology.

The author owns the original post. The feed is a derived representation that can be copied or computed separately.

The system is optimized for fast regional reads without pretending that all data changes require strong global transactions.

## 8.2 Example B: Banking platform with data residency

Requirements:

- Customer data belongs to a specific region.
- Orders and account records should remain within approved geographical boundaries.
- Transactions must be correct.
- Regional outage recovery is required.

Architecture:

```
               GLOBAL BANKING PLATFORM
                         |
                Regional Routing
                   /         \
                  /           \
                 v             v
              INDIA          EUROPE
                 |              |
           India App       Europe App
                 |              |
         Account Service  Account Service
                 |              |
           India DB        Europe DB
                 |              |
         Multi-AZ HA      Multi-AZ HA
                 |              |
         Approved DR      Approved DR
                 |              |
          India replica   Europe replica
```

For an account balance:

- Only the authoritative account service performs balance mutations.
- Concurrent transactions must be serialized or otherwise handled with correct database concurrency controls.
- Other regions must not blindly write to the same account replica.
- A replica used for displaying a balance must not be treated as authoritative for a transfer.

Example:

```
Transfer $100

Account A: $500
Account B: $200

Transaction:
  Debit A by $100
  Credit B by $100

Result:
  Account A = $400
  Account B = $300
```

The debit and credit must be coordinated according to the system's transactional guarantees.

A cross-region transfer between two region-owned accounts can require a carefully designed saga or a strongly consistent transaction mechanism, depending on the product's correctness requirements.

Why not simply replicate account balances everywhere and let all regions update them?

Because concurrent updates could violate money-conservation invariants.

This is a perfect example of how business semantics determine database architecture.

## 8.3 Example C: Global e-commerce platform

Requirements:

- Global product discovery.
- Region-specific inventory.
- Regional orders.
- Local payment integrations.
- Fast search.
- Global reporting.

Architecture:

```
                      GLOBAL USERS
                           |
                     API Gateway / LB
                           |
            +--------------+--------------+
            |              |              |
            v              v              v
          INDIA          EUROPE           USA
            |              |              |
        App Services   App Services   App Services
            |              |              |
        Redis Local    Redis Local    Redis Local
            |              |              |
        Regional DB    Regional DB    Regional DB
            |              |              |
        Orders         Orders         Orders
        Inventory      Inventory      Inventory
            |              |              |
            +--------------+--------------+
                           |
                     CDC / Events
                           |
                   Global Search Index
                           |
                   Global Analytics
```

Possible placement:

| Data             | Placement                                |
| ---------------- | ---------------------------------------- |
| Product catalog  | Global read availability                 |
| Product images   | CDN                                      |
| Inventory        | Regionally owned or explicitly allocated |
| Customer records | Customer home region                     |
| Orders           | Order's owning region                    |
| Payments         | Appropriate regional payment boundary    |
| Search index     | Global or regional projections           |
| Analytics        | Data warehouse                           |

Important scenario:

A user searches for a product globally, but purchases it in India.

The search response can come from a global read index.

The final purchase must validate inventory and pricing using the authoritative inventory and order systems.

The search index must never be the authority for whether the last unit can be sold.

## 8.4 Example D: Global chat application

Requirements:

- Users across countries can chat.
- Messages must appear in order within a conversation.
- Conversations might involve participants from different regions.
- Messages need to survive failures.

One useful design is to assign each conversation a home region.

```
conversationId = 9001
homeRegion = INDIA
```

Then:

```
Indian participant
       |
       v
India Chat Service
       |
       v
India Conversation Database
       |
       v
Message event
       |
       +---------> Europe consumers
       |
       +---------> US consumers
```

Messages in that conversation are serialized by the owning region.

This simplifies ordering.

But users in Europe may incur cross-region latency for sending messages if the conversation's owner is in India.

There are alternatives:

- Assign conversations dynamically to participant regions.
- Co-locate chat participants or conversations.
- Use a distributed database supporting the required ordering/consistency.
- Accept a more complex multi-writer conflict-resolution approach.

The correct solution depends on the expected latency, ordering, and availability requirements.

The key is that a conversation, order, or account is often a better ownership unit than an entire geographical database.

# Chapter 9 — The Architecture Decisions You Should Make in Every Multi-Region Interview

Now let's turn everything into a repeatable way of thinking.

The biggest mistake in interviews is drawing:

```
India App + DB
Europe App + DB
US App + DB
```

And then saying:

"We will replicate data between regions."

That isn't enough.

The interviewer will start asking:

- What exactly are you replicating?
- Which region accepts writes?
- What if two regions update the same record?
- What happens when replication is delayed?
- How do you find the region that owns an entity?
- What happens if a region goes down?

You need to establish the architecture before drawing the components.

## 9.1 The 10 questions to ask yourself

Use this sequence for practically every multi-region system.

## Multi-region design checklist

1. What is the geographical requirement?

   Is multi-region needed for low latency, regional availability, disaster recovery, regulatory requirements, or all of them?
2. What are the logical datasets?

   Identify users, orders, products, messages, payments, and other entities.
3. Who owns each dataset?

   Global ownership, regional ownership, tenant ownership, or per-entity ownership?
4. Where does each shard live?

   Is the entire dataset in every region? Are shards distributed across regions? Does each region contain only its own shards?
5. Where are replicas placed?

   Same AZ, different AZ, another region, or multiple recovery regions?
6. Who can write?

   One global leader, one leader per shard, one leader per entity, or multiple writers with conflict resolution?
7. What consistency is necessary?

   Eventual consistency, read-your-writes, strong consistency, or transactional consistency?
8. How do cross-region queries work?

   Owner routing, scatter-gather, global read models, search indexes, or analytics pipelines?
9. How are supporting components distributed?

   Local caches, regional queues, event replication, global CDN, and background workers?
10. What is the failure strategy?

    RPO, RTO, promotion, fencing, traffic routing, and recovery?

## 9.2 One very important distinction: Traffic routing vs data routing

This deserves special attention.

There are actually two different routing problems.

Traffic routing: Which application region should handle this user's request?

Data routing: Which region contains the authoritative data required by this request?

They are not necessarily the same.

Example:

```
European User
      |
      v
Global Load Balancer
      |
      v
Europe Application
      |
      v
Data Ownership Router
      |
      v
India Database
```

The European user is handled by a nearby application, but the particular record belongs to India.

The global load balancer doesn't automatically know database ownership.

You need an application-level routing or data-access mechanism.

The same applies to reads.

A read router can choose:

```
Read request
    |
    v
Does local region have a valid replica?
    |
   Yes
    |
    v
Is its consistency sufficient?
    |
  +--+--+
  |     |
 Yes    No
  |     |
Local  Authoritative
read   region
```

This is an incredibly useful distinction to remember.

# Chapter 10 — Quick Reference: Which Architecture Should I Choose?

This is the summary that should become intuitive.

## 10.1 Data placement decision table

| Requirement                                  | Data architecture                                    |
| -------------------------------------------- | ---------------------------------------------------- |
| Every region needs all data                  | Full regional replication                            |
| All regions need only some shared records    | Selective replication                                |
| Large global dataset, partitioned and copied | Global sharding + per-shard replication              |
| Each region owns different data              | Regional sharding                                    |
| Regional ownership with disaster recovery    | Regional shards + permitted cross-region DR replicas |
| Some globally shared, some regional data     | Hybrid architecture                                  |

## 10.2 Read/write decision table

| Requirement                                             | Recommended approach                      |
| ------------------------------------------------------- | ----------------------------------------- |
| Low-latency global reads                                | Regional read replicas                    |
| Simple write ownership                                  | Single-writer                             |
| Independent entities updated in multiple regions        | Single-writer per entity                  |
| Concurrent updates to the same entity                   | Conflict resolution or coordinated writes |
| Globally strong consistency                             | Consensus/quorum-based database           |
| Regional users should see their own changes immediately | Read-your-writes mechanism                |
| Critical transaction correctness                        | Authoritative transactional write path    |

## 10.3 Cross-region query decision table

| Requirement                         | Recommended approach                               |
| ----------------------------------- | -------------------------------------------------- |
| Fetch one user or order             | Route to its home region                           |
| Search globally                     | Global or regional search indexes                  |
| Get totals from every region        | Scatter-gather                                     |
| Historical reporting                | Data warehouse                                     |
| Frequently accessed global summary  | Materialized aggregate                             |
| Cross-region joins on every request | Usually redesign using denormalization/read models |

## 10.4 Cache and queue decision table

| Component                        | Typical strategy                                      |
| -------------------------------- | ----------------------------------------------------- |
| CDN                              | Global edge distribution                              |
| Redis                            | Local regional caches                                 |
| Cache synchronization            | Invalidation/update events, versions and TTL          |
| Local background tasks           | Regional queues                                       |
| Globally relevant events         | Local publishing + selective cross-region replication |
| Queue disaster recovery          | Replicated streams or standby clusters                |
| Cross-region consumer processing | Idempotent consumers with explicit ordering rules     |

# Chapter 11 — Five Mistakes to Avoid in Interviews

These are worth remembering because they are common architectural mistakes.

Mistake 1: Assuming multiple regions mean every database is replicated everywhere.

No.

You must decide what data is replicated and where.

You can have full replication, selective replication, regional ownership, or hybrid placement.

Mistake 2: Assuming active-active means every database accepts writes from every region.

No.

Applications can run active-active while database writes remain single-writer per shard or entity.

This is often a much simpler and safer architecture.

Mistake 3: Assuming replication means the data is immediately consistent.

No.

Asynchronous replicas may be behind.

Your application needs an explicit policy for stale reads, read-your-writes, and correctness-critical operations.

Mistake 4: Assuming queues automatically distribute messages globally.

No.

Each region may have an independent queue.

Cross-region event distribution must be explicitly designed, including replication scope, delivery guarantees, ordering, and duplicate handling.

Mistake 5: Assuming a database replica is sufficient for disaster recovery.

No.

You also need backups, recovery procedures, writer fencing, routing changes, queue recovery, and failover testing.

# Chapter 12 — The Final Mental Model

Let's bring everything together into one architecture.

Imagine a global e-commerce application.

```
                         GLOBAL USERS
                              |
                      GLOBAL LOAD BALANCER
                              |
               +--------------+--------------+
               |              |              |
               v              v              v
             INDIA          EUROPE           USA
               |              |              |
        +------+-----+  +-----+------+  +----+------+
        |            |  |            |  |           |
      App         Redis App        Redis App      Redis
        |            |  |            |  |           |
        +------+-----+  +-----+------+  +----+------+
               |              |              |
               v              v              v
           REGIONAL        REGIONAL        REGIONAL
           DATABASE       DATABASE        DATABASE
               |              |              |
           Local HA       Local HA        Local HA
               |              |              |
               +--------------+--------------+
                              |
                     SELECTIVE REPLICATION
                         /          \
                        v            v
                    Read Models   DR Replicas
                        |
                +-------+-------+
                |               |
                v               v
           Global Search    Analytics
                |
                v
               CDN

     REGIONAL EVENT INFRASTRUCTURE
                |
        +-------+-------+
        |       |       |
       IN      EU      US
        |       |       |
     Kafka   Kafka   Kafka
        |       |       |
        +-------+-------+
                |
        Selected geo-replication
```

This is an illustrative hybrid architecture, not a recommendation to replicate every database or every event to every destination.

The actual placement depends on the data ownership and consistency requirements.

But now you know what every connection represents:

- Application deployments provide regional compute.
- Regional databases hold authoritative data or local replicas.
- Shards divide datasets.
- Database replicas provide availability and potentially local reads.
- Cross-region replication distributes changes.
- Redis provides regional caching.
- CDN provides edge caching.
- Regional Kafka clusters provide local event processing.
- Geo-replication distributes selected events.
- Read models provide efficient cross-region queries.
- DR replicas support recovery.
- Global traffic routing directs users to available application regions.
- Data routing directs operations to the correct data owner.

## The most important takeaway

There is one idea I want you to retain above everything else:

Don't start a multi-region design by deciding how many databases, caches, or queues to deploy. Start by deciding who owns the data and where that data must be available.

Everything else follows.

For every dataset, mentally construct this table:

| Dimension      | Decision                                |
| -------------- | --------------------------------------- |
| Data ownership | Who is authoritative?                   |
| Partitioning   | How is the data divided?                |
| Placement      | Which regions contain it?               |
| Replication    | Which copies exist and why?             |
| Write path     | Who can modify it?                      |
| Read path      | Which copy can serve a read?            |
| Consistency    | How fresh must that copy be?            |
| Recovery       | What happens when its owner fails?      |
| Caching        | Where can derived copies live?          |
| Events         | Which regions need its changes?         |
| Queries        | How will distributed data be retrieved? |

If you can answer these questions, you can design multi-region architecture for almost any system.

## A few authoritative references to reinforce the concepts

These are useful when you want to see how real database and messaging products implement the patterns we discussed.

1. [DynamoDB Global Tables — replication and consistency](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_HowItWorks.html?utm_source=chatgpt.com) — particularly useful for understanding multi-region eventual consistency, strong consistency, and conflict resolution.
2. [DynamoDB global table design best practices](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/bp-global-table-design.html?utm_source=chatgpt.com) — useful for regional endpoints and local database access.
3. [Google Spanner replication](https://docs.cloud.google.com/spanner/docs/replication?utm_source=chatgpt.com) — excellent for understanding leaders, read-only replicas, witness replicas, and quorum-based writes.
4. [Apache Kafka Geo-Replication](https://kafka.apache.org/35/operations/geo-replication-cross-cluster-data-mirroring/?utm_source=chatgpt.com) — useful for cross-region queues, active-active and active-passive replication, and event distribution.
5. [AWS Multi-Region Architecture](https://docs.aws.amazon.com/prescriptive-guidance/latest/security-reference-architecture/multi-region-architecture.html?utm_source=chatgpt.com) — helpful for deciding when multi-region is actually justified.

## How to practice this for interviews

Rather than memorizing the diagrams, take a system design problem and classify every dataset into one of three categories:

- Globally replicated.
- Regionally owned.
- Hybrid / derived read model.

Then trace one write, one read, one cross-region query, and one regional failure.

That exercise will force you to apply all the concepts rather than just recognize the terminology.

The next particularly valuable exercise is to take a single system, such as WhatsApp, Instagram, or Uber, and walk through its complete multi-region architecture from request routing to database ownership, replication, Redis, Kafka, and regional failover.
