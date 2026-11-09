# Multi-region system design: a complete mental model

The main difficulty with multi-region system design is that several independent decisions get mixed together:

- Where does a piece of data live?
- Which region owns that data?
- How many copies of that data exist?
- Which copy handles reads?
- Which copy accepts writes?
- What happens when a region becomes unavailable?
- How do services communicate when the data they need is in another region?

Once you separate these decisions, multi-region design becomes much easier. You can combine a relatively small number of patterns to design systems such as Netflix, Amazon, Uber, global SaaS platforms, banking systems, and social networks.

The most important idea is:

Sharding decides which database partition owns a piece of data. Replication decides where copies of that partition exist. Multi-region architecture decides where those partitions, copies, and application services are deployed.

Let's derive the architecture step by step.

## 1. First, understand three independent concepts

Imagine we have a system serving users in three regions:

- US
- Europe
- Asia

We have a database containing 900 million user records.

Before discussing regions, let's understand what happens when this database grows.

### 1.1 Scaling with multiple database servers

Suppose we initially have one database:

```
             Application
                  |
                  v
             Database
          900M user records
```

We can scale it vertically by increasing CPU, memory, and storage.

But eventually, one database server may not be sufficient.

We introduce sharding.

### 1.2 Sharding: dividing ownership of data

We divide the dataset into three partitions:

```
                    Application
                         |
                 Database Router
                         |
             +-----------+-----------+
             |           |           |
             v           v           v
          Shard 1     Shard 2     Shard 3
          User        User        User
          1-300M      300-600M    600-900M
```

Each shard owns a different portion of the data.

For example:

| Shard   | Data owned     |
| ------- | -------------- |
| Shard 1 | Users 1–300M   |
| Shard 2 | Users 300–600M |
| Shard 3 | Users 600–900M |

A user belongs to one shard according to a shard key, such as `user_id`.

A request for user 125 goes to Shard 1. A request for user 450 goes to Shard 2.

The critical point: Shards are not copies of one another.

They contain different pieces of the logical dataset.

### 1.3 Replication: creating copies of the same data

Now imagine Shard 1 goes down.

We need another copy of its data.

So we introduce replication:

```
                 Shard 1
                 Primary
                    |
             Replication
                    |
             +------+------+
             |             |
             v             v
         Replica 1      Replica 2
```

All three contain the same logical data for Shard 1.

Typically:

- The primary accepts writes.
- Replicas receive changes from the primary.
- Reads can be served by the primary or suitable replicas.
- A replica may be promoted if the primary fails.

The same thing happens independently for Shard 2 and Shard 3.

This gives us two separate dimensions:

| Concept     | What it solves                 | What it means                                 |
| ----------- | ------------------------------ | --------------------------------------------- |
| Sharding    | Capacity and data distribution | Different servers own different data          |
| Replication | Availability and read scaling  | Multiple servers hold copies of the same data |

You can shard without replication, replicate without sharding, or use both.

### 1.4 Multi-region: placing these database servers geographically

Now suppose our application serves users across the world.

We might deploy infrastructure in three regions:

```
       US Region          Europe Region        Asia Region
    +-------------+      +-------------+     +-------------+
    | Application |      | Application |     | Application |
    | Database    |      | Database    |     | Database    |
    | Cache       |      | Cache       |     | Cache       |
    | Queue       |      | Queue       |     | Queue       |
    +-------------+      +-------------+     +-------------+
```

But a crucial question remains:

What exactly should each region's database contain?

There are several valid answers:

1. Every region contains all the data.
2. Each region contains only the data belonging to that region.
3. Each region owns some data but also keeps replicas of data owned elsewhere.
4. Some data is global, while other data is region-specific.

These are fundamentally different architectures.

Don't assume that deploying an application in three regions means putting the same database in all three regions. That is one possible design, not a universal rule.

## 2. The four important data-placement patterns

Most multi-region database designs can be understood using four patterns.

| Pattern                                         | What each region stores                              | Typical purpose                                          |
| ----------------------------------------------- | ---------------------------------------------------- | -------------------------------------------------------- |
| A. Full replication                             | All or most of the same dataset                      | Local reads, global availability                         |
| B. Geographic sharding                          | Only its assigned portion of the dataset             | Data locality, residency, regional ownership             |
| C. Sharded ownership with cross-region replicas | Some data it owns and copies of data owned elsewhere | Local writes for some data, disaster recovery for others |
| D. Hybrid                                       | Different strategies for different data types        | Real-world systems                                       |

We will examine each one, including the actual read and write flows.

## 3. Pattern A: the same data is available in multiple regions

Let's start with a common requirement.

Imagine a social media application. A user in India should be able to read a public profile belonging to someone in the US. A user in Europe should also be able to access that profile.

The application needs a copy of the same logical data in multiple regions.

For simplicity, assume we have:

- US Region
- Europe Region
- Asia Region

Every region needs access to the full user dataset.

There are two major ways to implement this.

### 3.1 Approach A1: one primary region, replicas in other regions

This is the simplest multi-region replication pattern.

```
                         Global Routing
                               |
                 +-------------+-------------+
                 |             |             |
                 v             v             v
              US Region   Europe Region   Asia Region
                 |             |             |
                 v             v             v
              App Servers   App Servers   App Servers
                 |             |             |
                 v             v             v
             +--------+    +--------+    +--------+
             |Primary |--->|Replica |--->|Replica |
             |   DB   |    |   DB   |    |   DB   |
             +--------+    +--------+    +--------+
                 US          Europe         Asia
```

The US database is the primary. Europe and Asia maintain replicas.

All three databases contain the same logical dataset.

Who handles writes?

The US primary handles writes.

Suppose a user in India updates their profile.

The flow is:

```
User in India
     |
     v
Asia Application
     |
     v
Asia Database Replica
     |
     | Cannot accept this write
     v
Forward write to US Primary
     |
     v
US Primary Database
     |
     | Commit
     v
Replication to Europe and Asia
```

The write must reach the primary because the replicas are not authoritative write targets.

The application can forward the write to the primary, or a database-aware driver/service can route it there.

Who handles reads?

Reads can be served locally.

```
User in India
     |
     v
Asia Application
     |
     v
Asia Database Replica
     |
     v
Return data
```

This reduces read latency because the request doesn't have to travel to the US.

But there is an important issue.

#### The replication-lag problem

Suppose the user updates their profile in the US.

The US database commits the update at 10:00:00.

The change reaches Asia at 10:00:03.

During these three seconds, a read from Asia might return the old profile.

This is called replication lag.

The application needs to decide whether stale reads are acceptable.

For public profile information, a short delay might be acceptable.

For a bank balance, it probably isn't.

#### What if the user writes and immediately reads?

Consider this sequence:

1. User updates their address.
2. The write succeeds in the US.
3. The user immediately requests their profile from Asia.
4. The Asian replica has not received the update yet.
5. The user sees the old address.

This violates the user's expectation of read-your-writes consistency.

Common solutions include:

Solution 1: Route the user's subsequent reads to the primary.

After a successful write, keep the user's reads on the primary for a period or until the replica catches up.

Solution 2: Return a replication position or version.

The write response contains a database log position or application version. The next read can wait until its regional replica has applied at least that position. If it hasn't, route the read to the primary.

Solution 3: Use the primary for consistency-sensitive reads.

For example, profile updates may be followed by a primary read, while ordinary profile browsing uses regional replicas.

This allows us to use local replicas for most reads without pretending that they are always up to date.

#### What happens if the US region goes down?

Initially:

```
US Region                 Europe Region          Asia Region

Primary DB  ----------->  Replica DB  <--------> Replica DB
```

The primary is unavailable.

We need to promote one replica:

```
US Region                 Europe Region          Asia Region

Unavailable               New Primary            Replica
                               |
                               |
                               v
                         Accepts writes
```

Now Europe becomes the new primary.

Requests must be routed to Europe for writes.

This is an example of active-passive multi-region architecture.

The application may run in all three regions, but only one database region accepts writes.

#### Synchronous versus asynchronous replication

Replication can work in two important ways.

Asynchronous replication

The primary commits locally and acknowledges the write before remote replicas have necessarily received it.

```
Client
  |
  v
Primary DB
  |
  | Commit locally
  v
Acknowledge client
  |
  | Replicate later
  v
Remote replicas
```

Advantages:

- Lower write latency.
- The remote region does not need to acknowledge every write.
- A remote replica being temporarily unavailable need not block local writes.

Disadvantage:

\- If the primary region is destroyed before a committed write reaches the replicas, that write can be lost during failover.

Synchronous replication

The primary waits for the required remote acknowledgement or quorum before acknowledging the transaction.

```
                   +-----> Replica Europe
                   |
Client ---> Primary DB
                   |
                   +-----> Replica Asia

             Wait for required acknowledgements
                          |
                          v
                    Acknowledge client
```

Advantages:

\- Acknowledged writes can be protected against loss in the failure scenarios covered by the replication quorum.

Disadvantages:

- Higher write latency.
- Network connectivity to the required replicas becomes part of the write path.
- Failure or isolation of the required quorum can make writes unavailable.

One important detail: synchronous replication does not always mean waiting for every replica. A database may wait for a quorum or a configured subset. The exact guarantee depends on the database's replication and commit protocol.

For cross-region systems, the network round trip can be significant. That is why many applications use asynchronous cross-region disaster-recovery replication, even when they use stronger synchronous replication within a region.

### 3.2 When should we use this approach?

Use one primary with cross-region replicas when:

- Your application needs copies of the same dataset in multiple regions.
- Most writes can be handled centrally.
- Regional reads should be fast.
- A small amount of replication lag is acceptable.
- You can tolerate a controlled database promotion during a regional outage.

Typical examples:

- Public product catalogs.
- Read-heavy content platforms.
- Public profile data.
- Global configuration data.
- Systems that need warm disaster recovery.

One limitation is worth remembering:

Local application servers do not automatically mean local writes.

In this architecture, an Asian user may connect to an Asian application server, but their write could still travel to a US database.

That distinction is extremely important when reasoning about latency.

### 3.3 Approach A2: multiple regions accept writes

Now imagine the same application has users all over the world, and we want users to write locally.

A single write-primary region can create high latency for users far away.

For example:

- User in India writes to the US primary.
- User in Germany writes to the US primary.
- User in Brazil writes to the US primary.

Every write has to travel to the US.

We might instead consider an active-active database architecture.

```
             +-----------------------------+
             |        Global Routing       |
             +-----------------------------+
                    /       |       \
                   /        |        \
                  v         v         v
               US Region  EU Region  Asia Region
                  |         |          |
                  v         v          v
               Database  Database   Database
               Read/Write Read/Write Read/Write
```

All three regions accept writes.

But now we have introduced a difficult problem.

### The write conflict problem

Suppose the same user has a record:

```
User 100
display_name = "Alex"
```

Two requests arrive almost simultaneously:

- A user in the US changes it to `"Alex Smith"`.
- A user in Europe changes it to `"Alex Jones"`.

Both regions accept the write locally.

What should the final value be?

```
US Database                    Europe Database

Alex Smith                     Alex Jones
```

We now have conflicting updates.

The system needs a conflict-resolution strategy.

There are several choices.

#### Strategy 1: Last-write-wins

Use a timestamp or version to decide which update wins.

For example:

```
US update:     10:00:01
Europe update: 10:00:03

Final value: Alex Jones
```

This is relatively simple.

But it can silently discard a valid update. Clock synchronization and tie-breaking also matter.

It may be acceptable for certain profile attributes or low-value preferences.

It is generally unsuitable for financial balances or inventory reservations.

#### Strategy 2: Application-level merging

Instead of replacing an entire record, treat updates as separate operations.

For example:

```
User preferences

theme = dark
language = en
notifications = enabled
```

A US update changes `theme`.

A Europe update changes `language`.

These updates can be merged because they affect different attributes.

But if both updates modify the same attribute, the application still needs a conflict policy.

#### Strategy 3: CRDTs

Conflict-free Replicated Data Types are data structures designed so that independently applied updates can be merged deterministically, under their defined operation and delivery assumptions.

For example, a distributed set of user reactions can use a mergeable representation.

CRDTs are useful when the data operation itself can be designed to merge safely.

They are not a universal solution for arbitrary SQL transactions.

#### Strategy 4: use a single writer for each record

This is one of the most useful approaches in real-world system design.

Instead of allowing every region to update every record, assign each record to one owning region.

For example:

| User     | Home region | Write owner |
| -------- | ----------- | ----------- |
| User 100 | US          | US          |
| User 200 | Europe      | Europe      |
| User 300 | Asia        | Asia        |

Now:

- US owns User 100.
- Europe owns User 200.
- Asia owns User 300.

Other regions can still hold replicas.

```
              Global User Dataset
                       |
             Partition by user_id
                       |
       +---------------+---------------+
       |               |               |
       v               v               v
    US-owned        EU-owned        Asia-owned
      data             data             data
       |               |               |
       v               v               v
    US Primary      EU Primary      Asia Primary
       |               |               |
       v               v               v
    Replicas        Replicas        Replicas
```

This gives us multiple write-primary regions at the system level, but each individual record still has one authoritative writer.

That distinction is critical:

A system can be active-active without having multiple writers for the same record.

We will use this pattern repeatedly when discussing geographic sharding.

#### Strategy 5: use a globally coordinated database

Some distributed databases provide a single logical database across multiple regions, using consensus and transaction protocols to coordinate writes.

Applications can interact with one logical database, while the database manages replication, leader placement, and consistency.

This can provide strong consistency across regions, but it doesn't make the network latency disappear.

For example, Google Cloud Spanner documents multi-region configurations with replicas across regions and quorum-based writes. Writes can incur cross-region coordination latency, while appropriately configured replicas can improve read locality.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.cloud.google.com\&sz=32)

Google Cloud Documentation+1



This approach is useful when global consistency is important enough to justify the latency, infrastructure cost, and database constraints.

### 3.4 When should we use active-active writes?

Use multi-region writes when:

- Users must write locally in multiple regions.
- A centralized write region creates unacceptable latency or throughput limitations.
- The data model supports conflict resolution or controlled ownership.
- You understand the consistency guarantees of the storage system.

The safest starting point for a design interview is often single-writer ownership per record or partition, with replicas elsewhere.

Only introduce unrestricted multi-writer behavior when the requirements justify it.

## 4. Pattern B: different regions own different data (geographic sharding)

Now let's consider a completely different requirement.

Imagine we operate a SaaS application with customers in:

- United States
- European Union
- India

Regulations and contractual requirements say that each customer's personal and business data must remain within its approved geographic boundary.

This is not simply a latency problem.

We are not allowed to place every customer's data in every region.

We need geographic sharding.

### 4.1 How geographic sharding works

We assign each customer or tenant a home region.

```
                  Global Application
                          |
                    Routing Layer
                          |
             +------------+------------+
             |            |            |
             v            v            v
          US Region    EU Region    India Region
             |            |            |
             v            v            v
          US Data      EU Data      India Data
```

For example:

| Customer   | Home region | Where their data lives |
| ---------- | ----------- | ---------------------- |
| Customer A | US          | US database            |
| Customer B | Europe      | EU database            |
| Customer C | India       | India database         |

The datasets are different.

The US database does not have Customer B's data.

The EU database does not have Customer C's data.

This is sharding at the regional level.

### 4.2 But each region still needs database replicas

Geographic sharding does not mean every region has only one database server.

Within each region, we still need high availability.

For example:

```
                     US Region
                         |
                  US Application
                         |
                         v
                    US Primary DB
                         |
              +----------+----------+
              |                     |
              v                     v
          Replica AZ-2          Replica AZ-3
```

The same applies to Europe and India.

Each region owns its own data and replicates that data within its failure domain.

Now imagine the US region needs disaster-recovery protection as well.

We could keep a disaster-recovery replica in another approved region:

```
              US Region                   DR Region
          +----------------+          +----------------+
          |                |          |                |
          | US Primary DB  |--------->| US DR Replica  |
          |                |          |                |
          +----------------+          +----------------+
```

This replica contains US-owned data.

It does not become European customer data merely because the physical database is hosted in Europe. However, whether such placement is legally allowed depends on the actual jurisdiction, applicable rules, contracts, and encryption/access controls.

If regulations prohibit the data from leaving the US, that DR placement is not allowed. You would need an approved location within the permitted boundary.

### 4.3 How reads and writes happen

Suppose Customer B belongs to Europe.

The customer travels to India and opens the application.

Should we access the Indian database?

No.

The customer’s home region is Europe.

We route the request to the European data owner.

```
Customer B in India
        |
        v
India Application / Global Router
        |
        | Resolve Customer B's home region
        v
     Europe Region
        |
        v
    Europe Database
        |
        v
     Response
```

The customer may experience a longer round trip, but the system preserves data ownership and residency requirements.

This is an important principle:

The user's current physical location does not necessarily determine where their data is stored.

A customer's home region is a logical ownership decision, not necessarily their current location.

### 4.4 How does the application know the home region?

We need a routing mechanism.

A simple example is a tenant-region mapping:

```
tenant_id       home_region
--------------------------------
tenant-100      us-east
tenant-200      eu-west
tenant-300      ap-south
tenant-400      eu-west
```

The application uses this mapping to route requests.

```
Incoming Request
      |
      v
Extract tenant_id
      |
      v
Look up tenant home region
      |
      v
Select regional service endpoint
      |
      v
Connect to the owning database
```

This mapping is sometimes called a tenant directory, shard map, or routing directory.

In a large system, the directory itself must be highly available and cached carefully.

A common implementation uses:

- A globally available routing/control-plane service.
- Regional caches of tenant-to-region mappings.
- A durable source of truth for ownership.
- Versioned mappings so stale routers do not use outdated ownership information.

The system must avoid changing a tenant's region casually. Moving a tenant means migrating data and updating ownership safely.

### 4.5 How do we scale inside each geographic region?

Suppose Europe becomes very large.

We don't necessarily create another top-level region.

We can shard the European dataset further:

```
                    EU Region
                        |
                  EU Application
                        |
                   DB Router
                        |
            +-----------+-----------+
            |           |           |
            v           v           v
         EU Shard 1  EU Shard 2  EU Shard 3
            |           |           |
            v           v           v
         Replicas    Replicas    Replicas
```

Now we have two levels of partitioning:

1. Geographic partitioning: which region owns a tenant?
2. Database sharding: which shard inside that region owns the tenant?

For example:

```
Tenant 100 -> US -> US Shard 2
Tenant 200 -> EU -> EU Shard 1
Tenant 300 -> India -> India Shard 3
```

This combination is especially useful for large SaaS platforms.

Azure's sharding guidance describes this geographic approach: requests are routed to shards based on a geographic attribute, and further partitioning can be used within a region to distribute load.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://learn.microsoft.com\&sz=32)

Microsoft Learn



### 4.6 What happens when a region fails?

Suppose the European region fails.

The primary database is unavailable.

We promote the European tenant data's DR replica, assuming one exists and is sufficiently up to date.

```
Before failure:

EU Primary DB  ----------->  EU DR Replica

After failure:

EU Primary DB: Unavailable

EU DR Replica: Promoted
               |
               v
          Accepts writes
```

The router must now direct European tenant traffic to the promoted database.

This is still active-passive for each tenant's authoritative data, even though the full application is active in several regions.

Remember: active-active application deployment and active-active database writes are different concepts.

## 5. Pattern C: each region owns some data and replicates other regions' data

This is an important variation because it combines geographic sharding with replication.

Suppose our application has three regions.

Each region owns its own users, but we want every region to be able to read public user information from other regions.

For example:

- US owns US users.
- Europe owns European users.
- Asia owns Asian users.
- All three regions can read public user profiles globally.

We could do this:

```
                        Global Dataset
                              |
             +----------------+----------------+
             |                |                |
             v                v                v
          US Region       EU Region        Asia Region
             |                |                |
             |                |                |
       +-----+-----+    +-----+-----+    +-----+-----+
       |           |    |           |    |           |
       v           v    v           v    v           v
    US Primary  EU     EU Primary  US   Asia Primary EU
                 Replica            Replica           Replica
       |
   Replicates US-owned data
   to other regions
```

To make the behavior easier to see, let's represent the data ownership explicitly.

| Region | Data it owns | Data it replicates from elsewhere |
| ------ | ------------ | --------------------------------- |
| US     | US users     | EU and Asia users                 |
| Europe | EU users     | US and Asia users                 |
| Asia   | Asia users   | US and EU users                   |

Each region is authoritative for its own records.

For example, the US region is the only write owner for US users.

Europe and Asia can read replicated copies of US users, but they cannot directly overwrite US-owned records.

### 5.1 How does a write happen?

A US user updates their profile.

```
US User
   |
   v
US Application
   |
   v
US Primary DB
   |
   | Commit
   |
   +-------------------+
   |                   |
   v                   v
EU Replica         Asia Replica
```

The write is committed in the US.

The change is replicated to Europe and Asia.

The system has one authoritative write owner, but several read locations.

### 5.2 How does a read happen?

A European user views a US user's public profile.

```
European User
      |
      v
Europe Application
      |
      v
Europe Database
      |
      v
Local replica of US user
      |
      v
Return profile
```

No cross-region database call is necessary, provided the required public profile fields have been replicated to Europe.

This gives us local read performance without allowing arbitrary local writes.

### 5.3 Why is this useful?

It is especially useful when:

- Each entity has a clear owning region.
- Users need to interact with entities from other regions.
- Most reads can tolerate a small amount of replication lag.
- There is a good reason to keep write ownership regional.

Potential examples:

- A marketplace with sellers assigned to home regions.
- A social platform with public profiles.
- A global SaaS system with regional tenant ownership and globally discoverable organization metadata.
- A platform where regional operational data must remain local but selected information can be shared globally.

### 5.4 A more realistic variation: replicate only selected fields

You don't always need to replicate the complete record.

Suppose a European user views a US user's profile.

The full US record might contain:

```
User
-----
user_id
name
email
phone
address
government_id
preferences
billing_details
```

But the public profile needs only:

```
PublicUserProfile
-----------------
user_id
display_name
profile_image
public_bio
```

The US region can publish a restricted representation to other regions.

```
US Primary Database
        |
        v
  Change/Event Publisher
        |
        v
 Public Profile Event
        |
        +-------------> Europe
        |
        +-------------> Asia
```

This provides a useful separation:

- The authoritative private record stays in the home region.
- A safe, purpose-specific representation is distributed globally.
- Remote reads do not require access to the private database.

This is often better than replicating the entire database everywhere.

## 6. Pattern D: hybrid data placement — how real systems are usually designed

So far, we've discussed the patterns separately.

But real systems rarely use only one of them.

Different data has different requirements.

Let's design a global e-commerce system.

It has:

1. Product catalog
2. User profiles
3. Shopping carts
4. Orders
5. Payments
6. Inventory
7. Search
8. Analytics

Should all these datasets use the same replication strategy?

Definitely not.

Let's examine them.

### 6.1 Product catalog: globally replicated

Product information is read heavily across the world.

Suppose a product is created in the US.

Users in Europe and Asia should also be able to see it.

We can maintain a global catalog with read replicas in each region.

```
               Product Catalog
                 US Primary
                      |
              +-------+-------+
              |               |
              v               v
           EU Replica      Asia Replica
```

Product updates can be asynchronous if a short delay is acceptable.

The system gets fast regional reads.

### 6.2 User profiles: geographic ownership

Suppose a user is assigned to a home region.

Their profile is stored in that region.

```
US Users   -> US Database
EU Users   -> EU Database
Asia Users -> Asia Database
```

A regional replica or DR copy can protect the data, subject to residency rules.

### 6.3 Shopping carts: user/tenant-owned

A shopping cart belongs to a user.

We can route it to the user's home region.

For anonymous sessions, we may initially use an edge/session store or assign a temporary home region, then migrate or associate the cart when the user signs in.

A cart may need to be accessible from multiple devices and regions.

The key decision is not that every device gets an independent cart database. It is that all updates for the same logical cart must have a well-defined owner and concurrency strategy.

### 6.4 Orders: regional ownership with strong transactional requirements

An order is generally authoritative in a specific region.

A user places an order.

The order service writes it to the appropriate authoritative database.

The transaction might need to atomically update:

- Order record
- Order items
- Payment intent or payment workflow record
- Inventory reservation record, depending on the architecture

We should avoid casually replicating writes across multiple regions and allowing all replicas to update orders.

A reasonable approach is:

```
User
 |
 v
Regional Order Service
 |
 v
Order DB (authoritative region)
 |
 v
Outbox Event
 |
 v
Queue / Event Broker
 |
 +---------> Fulfillment
 |
 +---------> Notifications
 |
 +---------> Analytics
```

The order is authoritative in one region; downstream views can be replicated.

### 6.5 Payments: strict ownership and explicit consistency

Payments are different from product catalog reads.

We must avoid double charging, duplicate captures, or losing a committed financial operation.

Typical design choices include:

- A clearly defined payment/order owner.
- Idempotency keys.
- Durable transaction records.
- Strict transactional semantics for the authoritative ledger.
- Explicit handling of uncertain external payment-provider outcomes.
- Replication and disaster recovery designed around financial recovery requirements.

We should not use an arbitrary last-write-wins conflict-resolution strategy for payment balances or ledger entries.

The business operation must be designed so that retries cannot create duplicate financial effects.

### 6.6 Inventory: ownership by stock location or warehouse

Inventory is often best partitioned by the physical location that owns the stock.

For example:

```
US Warehouse  -> US Inventory DB
EU Warehouse  -> EU Inventory DB
India Warehouse -> India Inventory DB
```

A customer may search globally for available products, but the authoritative stock count is owned by the warehouse or inventory partition responsible for that item.

Search results can be based on replicated or materialized inventory views.

But actual reservation and allocation must be validated against the authoritative inventory data.

### 6.7 Search: globally distributed read model

Search queries often need to see products, sellers, and availability from many regions.

Instead of querying every transactional database for every search request, maintain regional search indexes or global indexes populated from events.

```
US Product DB ----+
                  |
EU Product DB ----+----> Event Pipeline ----> Search Index
                  |
Asia Product DB --+
```

We can maintain:

- Regional search indexes.
- A global index.
- Multiple indexes grouped by product category or marketplace.

The exact choice depends on latency, data residency, freshness, and search scope.

### 6.8 Putting the hybrid design together

Our e-commerce architecture now looks like this:

```
                         Users Worldwide
                                |
                         Global Routing
                                |
             +------------------+------------------+
             |                  |                  |
             v                  v                  v
          US Region          EU Region          Asia Region
             |                  |                  |
       +-----+------+     +-----+------+     +-----+------+
       |            |     |            |     |            |
       v            v     v            v     v            v
    Regional     Regional Regional    Regional Regional   Regional
    Services      Cache   Services     Cache  Services     Cache
       |                  |                  |
       v                  v                  v
   US Databases       EU Databases       Asia Databases
       |                  |                  |
       +------------------+------------------+
                          |
                     Event Pipeline
                          |
                 +--------+--------+
                 |                 |
                 v                 v
             Global Search    Analytics / BI
```

Each database has its own ownership and replication rules.

The catalog might be replicated globally.

Customer and order data might be geographically partitioned.

Warehouse inventory might be owned by individual warehouses.

Search and analytics might use separate read models.

The most important practical lesson: choose the replication and sharding strategy per data domain, not per application.

## 7. How cross-region queries actually work

This is one of the most important topics for system design interviews.

Imagine an API:

```
GET /users/100/orders
```

User 100 belongs to the US region.

But the request reaches the European application.

Where should the query execute?

There isn't one universal answer. It depends on the type of query.

### 7.1 Case 1: querying a single entity

Suppose we need to retrieve one user's orders.

We know that User 100 belongs to the US region.

The correct approach is to route the query to the US data owner.

```
European User
      |
      v
Europe API Server
      |
      v
Resolve user_id = 100
      |
      v
User's home region = US
      |
      v
Call US Order Service
      |
      v
US Order Database
      |
      v
Return orders
```

Notice that the European service does not need direct access to the US database.

It calls the US service through a defined API.

This matters for service ownership, authorization, data governance, schema changes, and operational isolation.

The remote call introduces latency, so the application should apply deadlines, timeouts, retries where safe, and appropriate bulkheads.

When to use this: single-entity lookups or operations where the authoritative record is in another region.

### 7.2 Case 2: query data from all regions

Suppose we have an administrative API:

```
GET /admin/users
```

It must return users from the US, Europe, and Asia.

But each region owns different users.

We have three databases:

```
       US DB          EU DB          Asia DB
          \             |             /
           \            |            /
            \           |           /
             v          v          v
                 Query Service
                       |
                       v
                 Merge Results
                       |
                       v
                   Response
```

One approach is called scatter-gather.

The query service sends requests to all relevant regions, receives their results, and merges them.

For example:

```
Request: GET /admin/users?status=ACTIVE

US returns:   1,000 users
EU returns:     800 users
Asia returns:   600 users

Query Service merges and sorts the results.

Final result: 2,400 users
```

This works, but it has disadvantages:

- The request waits for multiple regions.
- Network calls increase latency.
- One slow region can delay the complete request.
- A failed region may result in partial results.
- Sorting and pagination become more difficult.
- Querying every region can be expensive.

For example, a naive implementation might ask every region for page 1 with 100 records. But that doesn't necessarily give the globally correct first 100 records when sorted by creation time.

A more careful implementation uses a globally coordinated cursor, asks each region for enough candidates, and merges their ordered results.

That is substantially more complicated.

Scatter-gather is useful for relatively small administrative queries or workloads where global fan-out is acceptable.

It is often a poor choice for a high-volume user-facing API.

### 7.3 Case 3: use a global read model or materialized view

Suppose we frequently need to query all orders across all regions.

Instead of contacting every operational database for every query, we can create a global read model.

Each region publishes relevant order events.

```
       US Order DB
             |
             v
        US Event Bus
             |
             |
       EU Order DB
             |
             v
        EU Event Bus
             |
             |
      Asia Order DB
             |
             v
       Asia Event Bus
             |
             |
             v
       Global Event Pipeline
             |
             v
       Global Read Model
             |
             v
       Admin Query Service
```

For example, the global read model might contain:

```
order_id
customer_id
home_region
order_status
total_amount
created_at
```

The administrative API queries this read model.

The read model is updated asynchronously, so it may lag behind the transactional databases.

This is a form of materialized view / CQRS-style read model.

We separate:

- Transactional writes: handled by authoritative regional databases.
- Global reads: handled by a dedicated read-optimized representation.

This is frequently a better choice for dashboards, analytics, global reporting, and search.

### 7.4 Case 4: use a global search or analytics platform

Some queries don't belong in an operational database at all.

Examples:

- Search across millions of products.
- Global order statistics.
- Revenue by country.
- Monthly active users across all regions.
- Business intelligence reports.

For these, use purpose-built systems:

- Search indexes.
- Data warehouses.
- Data lakes.
- OLAP systems.
- Aggregation pipelines.

A common flow:

```
Regional Transactional Databases
                 |
                 v
           CDC / Events
                 |
                 v
          Data Ingestion
                 |
                 v
      Global Data Warehouse
                 |
                 v
         Analytics Queries
```

Be careful with data residency. A global analytics pipeline must not become a way of silently moving restricted raw data outside its allowed jurisdiction. You may need regional warehouses, filtered events, anonymization, aggregation, or approved residency boundaries.

### 7.5 Case 5: replicate only the data needed for joins

Imagine a query needs:

```
Orders
JOIN Products
JOIN Users
```

Orders are geographically sharded.

Product catalog data is globally available.

User information belongs to its home region.

We should avoid assuming that a normal SQL join can execute efficiently across three remote databases.

Instead, consider:

- Replicate the necessary product fields into each order region.
- Store required customer references and selected display fields with the order.
- Retrieve private user details from the user's owning region when needed.
- Maintain a purpose-built read model for global reporting.

For example, an order record may store a snapshot of the product title and price at the time of purchase.

That is useful not only for cross-region queries but also for preserving historical order correctness when the product catalog changes.

### 7.6 The cross-region query decision table

| Query requirement                             | Preferred approach                                      |
| --------------------------------------------- | ------------------------------------------------------- |
| Read one known entity                         | Route to its owning region                              |
| Read the same globally replicated data        | Read local replica, subject to consistency requirements |
| Read a few entities from known regions        | Parallel regional API calls                             |
| Administrative query over all regions         | Scatter-gather, when volume is manageable               |
| Frequent global dashboards                    | Global or regional materialized views                   |
| Large-scale analytical queries                | Data warehouse / OLAP                                   |
| Global product or content search              | Search index                                            |
| Join regional data with global reference data | Local reference-data copies or dedicated read model     |

One rule is worth remembering:

Do not make every cross-region query a live distributed database query.

Most scalable global systems deliberately move data into the representation best suited to the query, rather than executing arbitrary joins over distant operational databases.

## 8. Read and write consistency across regions

We have discussed replication and routing. Now let's make the consistency choices explicit.

Consider a database with:

```
US Primary
    |
    +-------> Europe Replica
    |
    +-------> Asia Replica
```

Suppose a user changes their account status from `ACTIVE` to `BLOCKED`.

How should reads behave in different regions?

There are multiple possible guarantees.

### 8.1 Eventual consistency

A write is committed in the primary region.

Other regions receive the change eventually.

```
Time 10:00:00

US:     BLOCKED
EU:     ACTIVE
ASIA:   ACTIVE

Time 10:00:02

US:     BLOCKED
EU:     BLOCKED
ASIA:   ACTIVE

Time 10:00:04

US:     BLOCKED
EU:     BLOCKED
ASIA:   BLOCKED
```

Eventually, all replicas converge, assuming replication succeeds and the system recovers from failures.

This is suitable for:

- Public profile updates.
- Product descriptions.
- Recommendation data.
- Non-critical preferences.
- Some social feed information.

It is not suitable for every business operation.

### 8.2 Strongly consistent reads and writes

Suppose a user is blocked for security reasons.

We may not want another region to continue accepting operations using stale account status.

One option is to read from the authoritative database or use a database that provides a suitable strong-consistency guarantee.

```
Request from Asia
       |
       v
Authoritative DB / Strong read path
       |
       v
Latest committed state
```

This may require a cross-region round trip.

Another option is a globally distributed database that coordinates writes through consensus.

The exact mechanism depends on the database.

The important principle is:

Strong consistency is a guarantee about the order and visibility of operations, not simply about having several replicas.

Having three copies of a database does not automatically give you strong consistency.

### 8.3 Bounded-staleness reads

Sometimes we can accept a maximum replication delay.

For example:

> The application can show a value that is at most five seconds old.

This is called bounded staleness.

It is useful for workloads such as:

- Product availability displays.
- Dashboards.
- Public content.
- Some recommendation systems.

The application needs to know the staleness guarantee actually provided by its storage and replication mechanism. A configured cache TTL alone does not guarantee that the underlying replica is equally fresh.

### 8.4 Read-your-writes consistency

Suppose a user updates a record and immediately reads it.

They expect to see their update even if other users in distant regions haven't seen it yet.

We can achieve this by:

- Sending subsequent reads to the primary.
- Keeping the user's requests on a region that has the committed write.
- Returning a version or replication position and waiting for the read replica to catch up.
- Using a datastore that provides an appropriate session-consistency guarantee.

This is weaker than requiring every user in the world to observe the write immediately, but it often gives a much better user experience.

### 8.5 Monotonic reads

Imagine a user reads a profile and sees version 12.

Then their next request goes to another regional replica that has only applied version 10.

They see an older profile.

That is a non-monotonic read.

To prevent this, the application can maintain a session version and make sure subsequent reads do not return an earlier version.

This is another reason session-aware routing can be useful.

### 8.6 Consistency is a business decision

Don't choose consistency for the entire application.

Choose it for each operation.

| Operation                       | Possible consistency requirement                           |
| ------------------------------- | ---------------------------------------------------------- |
| Public profile                  | Eventual                                                   |
| Product description             | Eventual or bounded staleness                              |
| User's own profile after update | Read-your-writes                                           |
| Bank ledger transaction         | Strong transactional semantics                             |
| Inventory reservation           | Strong protection against overselling                      |
| Search results                  | Eventual                                                   |
| Analytics dashboard             | Eventual                                                   |
| Security permission revocation  | Strong or carefully bounded propagation, depending on risk |

Also remember that a strongly consistent database does not automatically make the entire business workflow strongly consistent.

For example, writing a payment record and updating an external payment provider are not one atomic database transaction. The workflow still needs idempotency, durable state, retries, and reconciliation.

## 9. How to route user requests to regions

We now know that data can belong to different regions.

But how do requests reach the right region?

There are usually two different routing decisions:

1. Which region should handle the user's application request?
2. Which region owns the requested data?

These decisions may produce the same region, but they do not have to.

### 9.1 Global traffic routing

Imagine a user in India visits our application.

We deploy application servers in:

- US
- Europe
- Asia

A global routing layer directs the request to a suitable region.

```
                     User
                       |
                       v
                DNS / Global Router
                       |
                Health + Routing Policy
                       |
          +------------+------------+
          |            |            |
          v            v            v
       US Region   EU Region    Asia Region
```

The routing policy might consider:

- Geographic proximity.
- Network latency.
- Region health.
- Capacity.
- Traffic distribution.
- Data residency or regulatory constraints.

Common implementations include global load balancers, DNS-based routing, Anycast-based routing, and cloud-managed traffic management.

The nearest region is not always the best region. A user's home region may also influence where their application request needs to be processed.

### 9.2 Application region versus data-owner region

Consider this example:

- User is physically in India.
- User belongs to the Europe region.
- The nearest application deployment is Asia.
- User's personal data is stored in Europe.

The flow becomes:

```
                 User in India
                       |
                       v
                Global Router
                       |
                       v
                 Asia App Server
                       |
                       v
                 Resolve Data Owner
                       |
                       v
                 Europe Service
                       |
                       v
                 Europe Database
```

The application code runs in Asia, but the authoritative data operation runs in Europe.

This is a perfectly valid design.

It also tells us that network latency is not determined only by the distance between the user and the application.

It depends on the full request path, including database calls and dependencies.

### 9.3 What about sessions?

A common misconception is that a user must always remain attached to one application server or region.

That isn't necessarily true.

Prefer stateless application servers with sessions stored in an appropriate shared or replicated store.

However, if a workflow requires read-your-writes consistency, you may route the session to the relevant data owner or preserve the necessary consistency token.

Sticky sessions can be useful in certain situations, but they should not be used as a substitute for correct data ownership and consistency.

## 10. How to design caches in multi-region systems

This is another area where people often overcomplicate the design.

Imagine we have three regions:

```
US Region             EU Region             Asia Region

App                   App                   App
 |                     |                     |
 v                     v                     v
Cache                 Cache                 Cache
 |                     |                     |
 v                     v                     v
Database              Database              Database
```

Should the cache be globally shared or local to each region?

In most designs, regional caches are independent.

Let's understand why.

### 10.1 Regional cache architecture

Each region maintains a cache close to its application servers.

For example:

```
              US Region
             +----------+
             | App      |
             | Redis US |
             | DB US    |
             +----------+

              EU Region
             +----------+
             | App      |
             | Redis EU |
             | DB EU    |
             +----------+

              Asia Region
             +----------+
             | App      |
             | Redis AS |
             | DB AS    |
             +----------+
```

Why?

- Low network latency.
- Regional failure isolation.
- Independent scaling.
- Less cross-region network traffic.
- Better control over data placement.

For ordinary cache-aside reads:

```
Request
   |
   v
Local Redis
   |
   +---- Cache Hit ----> Return data
   |
   +---- Cache Miss
             |
             v
       Appropriate database
             |
             v
       Populate local cache
             |
             v
         Return data
```

Notice the phrase "appropriate database."

In a geographic-sharding design, a request in Asia may still need to query the European data owner when its cache misses.

### 10.2 What happens when data changes?

Suppose a product is stored in the US and replicated to Europe and Asia.

Each region has a Redis cache containing the product.

The product is updated in the US.

How do the other caches find out?

One common approach is event-based invalidation.

```
US Product Database
        |
        v
   Product Update
        |
        v
   Outbox Event
        |
        v
   Event Pipeline
        |
        +--------------+
        |              |
        v              v
     EU Region      Asia Region
        |              |
        v              v
  Invalidate Key  Invalidate Key
```

The application invalidates the affected key in each region.

The next request reloads the data from the local replica or another appropriate source.

### 10.3 Important: don't invalidate caches before the database commits

A safe general sequence is:

1. Update the database.
2. Commit the database transaction.
3. Publish an invalidation event through a reliable mechanism.
4. Invalidate regional cache entries.
5. Allow future requests to reload the new version.

The outbox pattern is useful because database updates and event publication are not automatically one atomic operation.

If the process crashes after updating the database but before publishing the event, an outbox record allows the event to be retried.

### 10.4 What if an invalidation event is delayed?

A regional cache may continue serving stale data.

Possible protections include:

- A reasonable TTL.
- Versioned cache entries.
- Invalidation events.
- Read-through from authoritative storage for sensitive operations.
- A maximum acceptable staleness policy.

For example, use a versioned key:

```
product:100:version:12
```

A new product update creates version 13.

If a region still refers to version 12, it can detect that it is stale when compared against an appropriate version source.

For critical operations, don't rely solely on Redis invalidation to guarantee correctness.

### 10.5 Should Redis itself be replicated across regions?

It is possible, but don't make that the default.

A multi-region Redis deployment introduces additional complexity:

- Replication lag.
- Cross-region latency.
- Failover coordination.
- Potential write conflicts.
- Cache consistency behavior.
- Cost.

Caches are typically disposable copies. It is often simpler to rebuild each regional cache from the appropriate source of truth.

Replicate the authoritative database or event stream as required; rebuild cache entries locally.

### 10.6 What about CDN?

CDN is another level of caching, typically closer to users.

A typical architecture:

```
User
  |
  v
CDN / Edge Cache
  |
  | Cache Miss
  v
Global Traffic Router
  |
  v
Regional Application
  |
  v
Regional Redis
  |
  v
Database
```

Use CDN for:

- Static images.
- JavaScript and CSS assets.
- Public videos.
- Public product images.
- Public content with suitable cache policies.

Be careful with personalized or sensitive responses. Cache keys must include all relevant identity and variation dimensions, and private data should not accidentally become publicly cacheable.

For example, a public product image can be cached globally.

A user's private billing details should not be cached in a shared CDN cache.

### 10.7 Cache strategy by data type

| Data                   | Typical caching approach                             |
| ---------------------- | ---------------------------------------------------- |
| Public product catalog | Regional Redis and/or CDN                            |
| Static assets          | CDN                                                  |
| Public profiles        | Regional cache with invalidation/TTL                 |
| User session           | Regional or appropriately distributed session store  |
| Shopping cart          | Cache plus durable authoritative storage             |
| Bank balance           | Avoid trusting a stale cache for financial decisions |
| Search results         | Regional search cache                                |
| Security permissions   | Carefully controlled TTL and invalidation            |

Key principle: A cache does not determine data ownership. It only accelerates access to a copy of data.

## 11. How to design queues and event processing across regions

Now let's bring asynchronous processing into the architecture.

Suppose a user in Europe places an order.

The order service saves it in the European database.

We then need to:

- Send an email.
- Reserve inventory.
- Start payment processing.
- Notify the shipping service.
- Update search or analytics views.

Should all regions share one queue?

Not necessarily.

The correct choice depends on where the business operation is owned and where the consumer's data lives.

### 11.1 Pattern 1: local queue and local consumers

This is the simplest and often best starting point.

Each region has its own queue.

```
                  US Region
              +---------------+
              | US App        |
              | US Database   |
              | US Queue      |
              | US Consumers  |
              +---------------+

                  EU Region
              +---------------+
              | EU App        |
              | EU Database   |
              | EU Queue      |
              | EU Consumers  |
              +---------------+

                  Asia Region
              +---------------+
              | Asia App      |
              | Asia Database |
              | Asia Queue    |
              | Asia Consumers|
              +---------------+
```

Suppose a European user creates an order.

The flow:

```
EU User
   |
   v
EU Order Service
   |
   v
EU Database
   |
   | Transaction:
   | - Insert order
   | - Insert outbox event
   |
   v
Outbox Publisher
   |
   v
EU Queue
   |
   +---------> Payment Workflow
   |
   +---------> Notification Worker
   |
   +---------> Fulfillment Workflow
   |
   +---------> Analytics Publisher
```

The consumers operate close to the relevant data.

Advantages:

- Low latency.
- Clear regional ownership.
- Failure isolation.
- No need for all background processing to cross regions.
- Easier data residency controls.

This is particularly useful when a workflow is tied to a specific region's data.

### 11.2 Pattern 2: local processing, then publish globally

Now suppose the European order must be visible in a global analytics platform.

The order event needs to reach a global pipeline.

```
                 EU Region

            EU Order Database
                    |
                    v
               Outbox Event
                    |
                    v
                EU Queue
                    |
                    v
              Event Publisher
                    |
                    v
             Cross-Region Event
                    |
                    v
           Global Analytics Pipeline
                    |
                    v
             Global Read Model
```

Other regions don't need to process the original order as if it belonged to them.

They may only need a safe event containing the fields required for a global read model.

This is an important distinction:

Replicating an event does not mean giving every region ownership of the underlying business entity.

### 11.3 Pattern 3: a global workflow needs to reach the owning region

Imagine a service accepts a global command:

```
Generate monthly invoice for tenant-200
```

Tenant 200 belongs to Europe.

The request may arrive in the US.

We should route the command to the European region.

```
US API
  |
  v
Resolve tenant home region
  |
  v
Publish / Forward command to EU
  |
  v
EU Queue
  |
  v
EU Invoice Worker
  |
  v
EU Database
```

The US region should not simply consume the command and try to update European data directly.

The owner region processes the command.

### 11.4 Pattern 4: regional Kafka clusters

For a Kafka-based system, one common approach is to deploy a separate Kafka cluster in each region.

```
US Region                    EU Region                    Asia Region

US Kafka                     EU Kafka                     Asia Kafka
   |                            |                            |
   v                            v                            v
US Consumers                EU Consumers                Asia Consumers
```

Regional services produce and consume locally.

For globally required events, clusters can exchange selected topics using an appropriate replication or event-bridging mechanism.

For example:

```
US Kafka --------+
                 |
EU Kafka --------+----> Global Event Pipeline
                 |
Asia Kafka ------+
```

This can be preferable to forcing every producer in every region to synchronously write to one distant Kafka cluster.

However, cross-cluster replication needs careful design:

- Which topics should cross regions?
- Should all events or only selected events be replicated?
- How are duplicates handled?
- How is event ordering preserved?
- What happens when a region is disconnected?
- How are offsets and consumer progress managed?
- What is the recovery procedure?

Do not assume that replicating Kafka topics across regions automatically preserves one global ordering of all events.

Kafka ordering is scoped to a partition. Cross-cluster replication may introduce additional delays and ordering considerations.

### 11.5 What about SQS?

You can use a separate queue in each region as well.

For example:

```
US App -> US SQS -> US Workers
EU App -> EU SQS -> EU Workers
Asia App -> Asia SQS -> Asia Workers
```

If an event must reach another region, use an explicit event-forwarding or replication mechanism.

For SQS-style queue processing, remember:

- Messages may be delivered more than once.
- Consumers should be idempotent.
- The database update and message acknowledgement are not one atomic operation.
- Use durable retries and dead-letter handling.
- Preserve business ownership when routing messages.

A cross-region queue forwarding mechanism can also deliver duplicates, so the consumer must tolerate repeated events.

### 11.6 How does ordering work across regions?

Suppose a user performs:

```
1. CREATE_ORDER
2. CANCEL_ORDER
3. REFUND_PAYMENT
```

These events must be processed in the correct business order.

A sensible strategy is to route all events for the same order to the same logical processing partition.

For example:

```
partition_key = order_id
```

The consumer processes events for a particular order sequentially.

But do not assume that all events from all regions can have one global total order without coordination.

Global ordering across unrelated partitions is expensive and often unnecessary.

For cross-region replication, include metadata such as:

```
event_id
aggregate_id
aggregate_version
event_type
source_region
occurred_at
```

Then consumers can:

- Deduplicate by `event_id`.
- Check aggregate versions.
- Detect missing or out-of-order events.
- Ignore already-applied events.
- Trigger reconciliation when gaps are detected.

### 11.7 What happens if a region fails while processing messages?

Imagine Europe goes down.

The order is committed to the EU database, but the notification hasn't been processed.

What should happen?

A reliable design should ensure the committed business operation is not lost merely because the worker stopped.

Typical protections:

1. Persist the business transaction and outbox event durably.
2. Publish the event using a retryable publisher.
3. Keep the queue's message durability and retention requirements appropriate.
4. Make consumers idempotent.
5. Recover processing after the region or its failover environment becomes available.
6. Track poison messages in a dead-letter queue.

For disaster recovery, decide explicitly whether the queue itself must be replicated, whether it can be reconstructed from the outbox or event log, and how much recovery time is acceptable.

### 11.8 Queue placement decision table

| Requirement                              | Suitable approach                                   |
| ---------------------------------------- | --------------------------------------------------- |
| Process an operation using regional data | Regional queue and consumer                         |
| Update a regional database               | Consumer runs near the database owner               |
| Notify a user across regions             | Route event to the appropriate notification service |
| Global analytics                         | Replicate selected events to global pipeline        |
| Global command affecting a tenant        | Route command to tenant's owner region              |
| Preserve per-order ordering              | Partition by order ID and process sequentially      |
| Recover from region failure              | Durable events, idempotency, replay, DR procedures  |

One more important distinction:

A queue is not the source of truth for a completed database transaction.

The durable database record and/or outbox is what guarantees that the business operation and its event can be recovered.

## 12. Regional failure: how does the entire system recover?

So far, we have focused mostly on healthy operation.

But a major reason for multi-region architecture is to survive regional failures.

Let's consider a US-primary, Europe-replica database.

### 12.1 Understand RPO and RTO

Two terms are essential.

RPO — Recovery Point Objective

How much data loss can we tolerate?

For example:

- RPO = 0 seconds: the recovery design must not lose acknowledged committed transactions within its stated failure model.
- RPO = 5 seconds: up to five seconds of committed data might be lost.
- RPO = 1 minute: up to one minute of committed data might be lost.

RTO — Recovery Time Objective

How long can recovery take?

For example:

- RTO = 10 seconds.
- RTO = 5 minutes.
- RTO = 1 hour.

These are targets, not automatic guarantees.

We need to choose a replication and deployment architecture that can meet them.

### 12.2 Different disaster-recovery strategies

| Strategy           | What runs in DR region?                     | Recovery characteristics                                  |
| ------------------ | ------------------------------------------- | --------------------------------------------------------- |
| Backup and restore | Backups or snapshots                        | Slowest, lowest continuously running cost                 |
| Pilot light        | Minimal infrastructure and data protection  | Requires scaling and startup                              |
| Warm standby       | Running but reduced-capacity infrastructure | Faster recovery                                           |
| Hot standby        | Full or near-full duplicate environment     | Faster recovery, higher cost                              |
| Active-active      | Multiple regions actively serving traffic   | Can reduce failover time, but data coordination is harder |

A database replica by itself does not provide full regional disaster recovery.

You also need to consider:

- Application deployments.
- Secrets and configuration.
- Network and identity dependencies.
- Routing.
- Queues.
- Storage.
- Scheduled jobs.
- Monitoring and alerting.
- External integrations.
- Recovery procedures.

### 12.3 Step-by-step failure flow

Let's say US is the write-primary, and Europe maintains a DR replica.

Normal architecture:

```
                 Global Router
                      |
             +--------+--------+
             |                 |
             v                 v
          US Region        EU Region
             |                 |
           App               App
             |                 |
             v                 v
          Primary DB ------> DR Replica
```

Now the US region fails.

Step 1: Detect the failure.

Health monitoring detects that US is unavailable.

Don't fail over merely because one application instance is unhealthy. Confirm that the failure condition and recovery policy justify a regional failover.

Step 2: Prevent the old primary from accepting writes.

This is critical.

Suppose US temporarily becomes reachable again while Europe has already been promoted.

We must prevent both regions from accepting conflicting writes to the same data.

This is called split-brain prevention.

Typical mechanisms include:

- Fencing the old primary.
- Leader leases with carefully controlled expiration.
- Epoch or generation numbers.
- A reliable failover control plane.
- Network or infrastructure isolation of the old writer.

Step 3: Promote the replica.

Europe becomes the new authoritative write region.

Step 4: Update the region-routing configuration.

Clients and services must send writes to Europe.

Step 5: Restore dependent processing.

Ensure queues, consumers, scheduled workflows, and external integrations are working.

Step 6: Rebuild or warm caches.

Cache entries might be missing or stale. Repopulate them from the recovered authoritative data.

Step 7: Reconcile uncertain operations.

A request may have committed in US immediately before failure, even though the client never received its response.

The client might retry.

The application must use idempotency and transaction-state checks to prevent duplicate business effects.

### 12.4 What about the old primary coming back?

Do not simply bring the old US primary online as a writable primary again.

It may contain data that conflicts with what Europe accepted during the outage.

A controlled recovery normally looks like:

```
Europe: Current Primary
    |
    v
Reconcile / Rebuild US Data
    |
    v
US: New Replica
    |
    v
Verify Replication Health
    |
    v
Optional Controlled Failback
```

Failback is a separate planned operation, not an automatic return to the old topology.

### 12.5 What changes in an active-active system?

Active-active systems have a different problem.

If both regions accept writes for the same data, a network partition may leave each region accepting a different set of updates.

You need a consistency strategy that defines whether writes:

- Continue locally with eventual reconciliation.
- Require a globally reachable quorum.
- Are restricted according to ownership.
- Are rejected when the authoritative owner is unreachable.

Do not assume that active-active means every region can continue every operation during every network partition.

Availability, consistency, and conflict handling still need to be explicitly designed.

## 13. Put everything together: three realistic system design workflows

Let's apply the patterns to systems you might encounter in interviews.

The purpose is to see how the same building blocks are combined differently depending on the business requirements.

### 13.1 Workflow A: global video streaming platform (Netflix-like)

Requirements:

- Users worldwide.
- Fast playback.
- Global content catalog.
- User-specific viewing history.
- Recommendations.
- High availability.
- Large media files.

We should not design all these components with the same database strategy.

#### Data placement

| Data            | Placement strategy                 | Reason                            |
| --------------- | ---------------------------------- | --------------------------------- |
| Video files     | Distributed object storage and CDN | Serve content close to viewers    |
| Content catalog | Replicated globally                | Read by users everywhere          |
| User profiles   | Home-region ownership              | User data management and locality |
| Watch history   | User/region partitioning           | High-volume user-specific writes  |
| Recommendations | Regional/global read models        | Fast personalized reads           |
| Analytics       | Data lake/warehouse                | Large-scale aggregation           |

#### Architecture

```
                       Global Users
                            |
                            v
                         CDN Edge
                            |
                  +---------+---------+
                  |                   |
                  v                   v
              US Region          EU Region
                  |                   |
                  v                   v
             App Servers          App Servers
                  |                   |
          +-------+-------+   +-------+-------+
          |               |   |               |
          v               v   v               v
       User DB       Catalog DB  User DB   Catalog DB
       (US-owned)    (Replica)   (EU-owned)  (Replica)
          |               |       |             |
          v               v       v             v
       US Queue       Events   EU Queue       Events
          |                       |
          +-----------+-----------+
                      |
                      v
                Recommendation
                  Pipeline
                      |
                      v
              Regional Read Models
```

Video content is served primarily through CDN and storage distribution. You don't need every regional application server to fetch every video from one central database.

The catalog can be distributed for fast browsing, while viewing history remains associated with its owning user or data domain.

#### Read flow: browse catalog

```
User in India
      |
      v
Asia Application
      |
      v
Asia Catalog Replica
      |
      v
Return catalog
```

#### Write flow: update watch history

```
User in India
      |
      v
Asia Application
      |
      v
Resolve user's data owner
      |
      v
Authoritative Watch History Service
      |
      v
Database Write
      |
      v
Regional Event / Queue
      |
      v
Recommendation and Analytics Updates
```

The actual home-region policy depends on the platform's requirements. The key idea is that catalog distribution and user-history ownership are independent decisions.

### 13.2 Workflow B: global SaaS application

Imagine a SaaS platform serving thousands of businesses.

Requirements:

- Customers from different countries.
- Tenant data must remain in approved jurisdictions.
- Users from anywhere should access their tenant.
- Each tenant has projects, users, documents, and billing information.
- Global administration and reporting.

This is a strong example of geographic sharding.

#### Data ownership

```
Tenant A -> US Region
Tenant B -> EU Region
Tenant C -> India Region
Tenant D -> US Region
```

Each tenant's data is stored within its assigned home region.

Within each region, we may have several database shards:

```
                  US Region
                      |
             +--------+--------+
             |        |        |
             v        v        v
          Shard 1  Shard 2  Shard 3
```

Each shard has regional replicas and possibly an approved DR replica.

#### Request workflow

Suppose a user belonging to Tenant B accesses the application from India.

```
User in India
      |
      v
Global Load Balancer
      |
      v
Asia Application
      |
      v
Tenant Directory
      |
      v
Tenant B -> Europe
      |
      v
Europe Application / Data Service
      |
      v
Europe DB
```

The request can enter through Asia but the authoritative operation happens in Europe.

#### Reporting workflow

Suppose the SaaS administrator wants a global count of active tenants.

We should not query thousands of tenant databases synchronously.

Instead:

```
US Tenant DBs ----+
                  |
EU Tenant DBs ----+----> Regional Aggregation
                  |             |
India Tenant DBs -+             v
                          Approved Global
                          Aggregate Events
                                |
                                v
                         Reporting Store
```

The aggregate should contain only what is permitted to leave each jurisdiction.

This separates global reporting from tenant-level transactional ownership.

#### Important design choice

The tenant directory is itself a critical system.

It must be available to route requests, but its records should not contain or expose unnecessary tenant data.

We can cache tenant-to-region mappings in each application region while retaining an authoritative, versioned directory.

### 13.3 Workflow C: global booking / ticket reservation system

This is especially useful for understanding why unrestricted multi-region writes can be dangerous.

Imagine a platform booking seats for events across different countries.

Requirements:

- Users worldwide.
- Event listings visible globally.
- Seat inventory is owned by the venue/event.
- A seat must never be confirmed for two customers.
- Booking operations may involve payment and ticket generation.

#### Data placement

| Data                 | Placement                                                    |
| -------------------- | ------------------------------------------------------------ |
| Event catalog        | Distributed globally                                         |
| Venue metadata       | Distributed or replicated                                    |
| Seat inventory       | Owned by event/venue partition                               |
| Reservations         | Same authoritative owner as seat inventory                   |
| Payments             | Explicit payment transaction owner                           |
| Search               | Global or regional read model                                |
| Ticket delivery data | Associated with booking ownership, with appropriate replicas |

#### Why should we choose event/venue ownership?

Imagine a concert in London.

Users from India, the US, and Europe all want to book seats.

If every region independently accepts a write to a local seat replica, two users might reserve the same seat.

Instead, assign the event's inventory to one authoritative owner.

```
                  Event: London Concert
                           |
                           v
                    EU Inventory DB
                           |
                 +---------+---------+
                 |                   |
                 v                   v
             Reservation A      Reservation B
```

The US and Asia application servers route booking requests to the owner.

The reservation operation uses an atomic database operation or suitable locking/conditional update.

For example, a seat reservation might conceptually be:

```
UPDATE seats
SET status = 'HELD',
    reservation_id = :reservation_id,
    hold_expires_at = :expiry
WHERE event_id = :event_id
  AND seat_id = :seat_id
  AND status = 'AVAILABLE';
```

The application checks the affected row count.

Only one concurrent operation should successfully change the available seat to held, assuming the database transaction and constraints are correctly implemented.

#### Read workflow

A user in India searches for seats.

```
India User
    |
    v
Asia Search Service
    |
    v
Regional Search Index
    |
    v
Show available-seat information
```

Search availability may be slightly stale.

#### Reservation workflow

When the user actually clicks Book:

```
India User
    |
    v
Asia Application
    |
    v
Resolve event inventory owner
    |
    v
EU Reservation Service
    |
    v
EU Inventory DB
    |
    v
Atomic Seat Hold
    |
    v
Payment Workflow
    |
    v
Confirm / Release Reservation
```

The search index is not the authority for confirming a seat.

The authoritative reservation service is.

This is a fundamental pattern:

Use distributed read models for discovery, but validate important business operations against the authoritative owner.

## 14. The most useful architecture combinations to remember

Rather than memorizing dozens of architectures, learn these reusable combinations.

| Requirement                              | Data strategy                                              | Read strategy                                    | Write strategy                                          |
| ---------------------------------------- | ---------------------------------------------------------- | ------------------------------------------------ | ------------------------------------------------------- |
| Global reference data                    | Full replication                                           | Local replica                                    | Single owner or coordinated global database             |
| Global content with frequent local reads | Primary plus remote replicas                               | Local reads with staleness policy                | Primary-region writes                                   |
| Global users with regional ownership     | Geographic sharding                                        | Route to owner or use approved local projections | Home-region writes                                      |
| Globally discoverable regional entities  | Regional ownership plus selected-field replication         | Local replica/read model                         | Owner-region writes                                     |
| Global analytics                         | Regional operational databases plus aggregation pipeline   | Global OLAP/read model                           | Regional transactions; asynchronous events              |
| High-value transactions                  | Authoritative partition ownership and suitable replication | Strong read when needed                          | Single owner or coordinated strong-consistency database |
| Worldwide static content                 | Distributed storage and CDN                                | Edge cache                                       | Origin/storage update and invalidation                  |

### One particularly useful pattern: multiple regional primaries

Consider:

```
US Data Owner  -> US Primary -> EU Replica -> Asia Replica

EU Data Owner  -> EU Primary -> US Replica -> Asia Replica

Asia Data Owner -> Asia Primary -> US Replica -> EU Replica
```

This architecture has:

- Three regions.
- Three data ownership groups.
- One primary per ownership group.
- Cross-region replicas.

It is a perfectly valid way to build an active-active global application while maintaining single-writer ownership for each group of records.

The application can accept writes in all three regions, but each write must go to the primary responsible for that record.

This is a pattern you should be comfortable drawing in interviews.

## 15. Common mistakes in multi-region system design

These are the mistakes I would specifically avoid in interviews.

Mistake 1: Assuming every region should have a complete copy of the database.

That can be expensive and may violate residency or privacy requirements. Replicate only the data that needs to be available elsewhere.

Mistake 2: Treating replicas as writable databases without discussing conflicts.

If two databases accept writes to the same logical record, explain how conflicts are resolved or how ownership prevents them.

Mistake 3: Assuming the closest application region is always the data region.

Application placement and data ownership are separate decisions.

Mistake 4: Assuming all cross-region reads must query the source database.

Frequently used remote data can be distributed through replicas, events, search indexes, or materialized views.

Mistake 5: Using global synchronous replication for everything.

This can increase latency and reduce availability during network failures. Apply it where the consistency requirements justify the cost.

Mistake 6: Assuming a regional database replica is a complete DR solution.

You also need application infrastructure, networking, queues, secrets, routing, and recovery procedures.

Mistake 7: Using one global Redis cluster for all regional traffic by default.

Independent regional caches are often simpler, faster, and more resilient.

Mistake 8: Using one global queue for every workflow by default.

Local queues and consumers are often better for regional operations. Replicate or forward only the events that need global processing.

Mistake 9: Performing distributed joins for every API call.

This creates high latency and operational coupling. Use local projections, materialized views, or service-based lookups where appropriate.

Mistake 10: Forgetting about split brain during failover.

You must prevent an old primary and its promoted replacement from accepting conflicting writes.

Mistake 11: Treating active-active as a guarantee that every write can succeed everywhere.

Network partitions and quorum requirements still matter.

Mistake 12: Adding multi-region before understanding the requirement.

A well-designed multi-Availability-Zone architecture inside one region may be enough for many systems. Multi-region brings additional operational, consistency, networking, and cost complexity. AWS's multi-region guidance explicitly recommends adopting it for documented needs such as regional disaster recovery, residency, fault isolation, or global latency requirements rather than as a default.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

AWS Prescriptive Guidance



## 16. A reusable multi-region design framework for interviews

Here is the workflow I recommend using whenever an interviewer asks you to design a globally distributed system.

Don't immediately draw three regions with databases and caches.

Start with the requirements.

### Step 1: Why do we need multiple regions?

Identify the actual motivation.

| Requirement                | Architectural consequence                                         |
| -------------------------- | ----------------------------------------------------------------- |
| Regional disaster recovery | Replicate critical data and prepare a recovery environment        |
| Low read latency           | Regional read replicas or distributed read models                 |
| Low write latency          | Local write ownership or globally coordinated database            |
| Data residency             | Geographic data partitioning and approved replication boundaries  |
| Global availability        | Deploy application and dependencies across regions                |
| Global scale               | Regional partitions, workload distribution, and regional capacity |

These are different requirements and may lead to different architectures.

### Step 2: Classify the data

For every major entity, ask:

- Is the data global or regional?
- Is it public or private?
- Does it need to be available in all regions?
- Does it have a clear owner?
- Is replication allowed?
- How fresh must remote copies be?
- Must transactions be strongly consistent?

Create a table like this during your interview:

| Entity    | Placement               | Write owner     | Remote reads                     |
| --------- | ----------------------- | --------------- | -------------------------------- |
| Product   | Global                  | Product owner   | Local replicas                   |
| User      | Regional                | Home region     | Selected fields or remote lookup |
| Order     | Regional                | Order owner     | Read model                       |
| Inventory | Warehouse/event owner   | Inventory owner | Search projection                |
| Analytics | Global/approved regions | Pipeline        | Aggregated queries               |

You can modify this table for any problem.

### Step 3: Choose the shard key and ownership model

Examples:

| System           | Possible ownership key                       |
| ---------------- | -------------------------------------------- |
| SaaS             | tenant ID                                    |
| Social network   | user ID                                      |
| E-commerce       | order ID / customer region / inventory owner |
| Booking          | event ID / venue ID                          |
| Messaging        | conversation ID                              |
| Video platform   | user ID / content ID                         |
| Financial system | account ID / legal entity                    |

Pick a key that keeps the data and operations that need strong transactional behavior together.

Also consider hotspots. A popular tenant, event, or celebrity can create a heavily loaded partition.

### Step 4: Decide where copies exist

For every shard or ownership group, answer:

```
Who owns it?

Where is the primary?

Which regions have replicas?

Are replicas synchronous or asynchronous?

Can replicas serve reads?

Can they be promoted?

What is the permitted data residency boundary?
```

This is where you decide the replication topology.

### Step 5: Define read and write paths separately

Draw two flows for each important data domain.

For reads:

```
User
 |
 v
Nearest Healthy App Region
 |
 v
Local Cache / Local Replica
 |
 | Cache miss / Owner required
 v
Authoritative Region or Read Model
```

For writes:

```
User
 |
 v
Nearest Healthy App Region
 |
 v
Resolve Data Owner
 |
 v
Authoritative Write Service
 |
 v
Primary Database
 |
 +----> Replication
 |
 +----> Outbox / Event
```

This forces you to make the data ownership decision explicit.

### Step 6: Design cross-region queries

For every API requiring remote data, ask:

- Can it query a known owner?
- Can it use a regional replica?
- Is scatter-gather acceptable?
- Should we create a materialized view?
- Does it actually belong in a search or analytics platform?

Try to keep high-volume user-facing operations local.

### Step 7: Design cache placement

Usually start with:

```
Each Region:
Application -> Local Redis -> Appropriate Database

Globally:
CDN -> Public Static / Cacheable Content
```

Then add cross-region invalidation or versioning only where required.

### Step 8: Design queue placement

Usually start with:

```
Each Region:
Database -> Outbox -> Local Queue -> Local Consumers

For global workflows:
Regional Events -> Event Replication / Routing -> Global Consumers
```

Make ownership, idempotency, ordering, and replay explicit.

### Step 9: Design failover and recovery

Define:

- RPO.
- RTO.
- Primary promotion.
- Split-brain prevention.
- Traffic routing.
- Queue recovery.
- Cache recovery.
- Reconciliation.
- Failback.

Don't just say "we have replicas, so failover is handled."

### Step 10: Explain the trade-offs

A strong interview answer should explicitly acknowledge:

- Higher infrastructure and replication cost.
- Cross-region network latency.
- Stale reads.
- Distributed transaction complexity.
- Data residency restrictions.
- Failure and failover complexity.
- More complicated monitoring and debugging.

This demonstrates that you're making an architectural choice rather than adding regions because they sound scalable.

## 17. One more important distinction: Availability Zones, regions, replicas, and backups

You will often see architects draw databases in multiple regions, but it is important to understand the different failure boundaries.

A typical cloud region contains multiple Availability Zones (AZs).

For example:

```
                  US Region
        +---------------------------+
        |                           |
        |   Availability Zone A     |
        |   +-------------------+   |
        |   | Primary Database  |   |
        |   +-------------------+   |
        |                           |
        |   Availability Zone B     |
        |   +-------------------+   |
        |   | Replica           |   |
        |   +-------------------+   |
        |                           |
        |   Availability Zone C     |
        |   +-------------------+   |
        |   | Replica           |   |
        |   +-------------------+   |
        +---------------------------+
```

These AZs are intended to provide fault isolation within a region.

We can then add another region for disaster recovery:

```
                US Region                         EU Region
       +------------------------+        +------------------------+
       |                        |        |                        |
       | AZ-A    AZ-B    AZ-C   |        | AZ-A    AZ-B    AZ-C   |
       |  |       |       |     |        |  |       |       |     |
       | Primary -> Replicas    |------->| DR Replica             |
       |                        |        |                        |
       +------------------------+        +------------------------+
```

A typical arrangement is:

- Synchronous or quorum-based replication within a region, when the database supports it.
- Asynchronous replication across regions for disaster recovery, when the workload can tolerate the resulting RPO.
- Automated or controlled promotion during failure.

The exact database topology varies.

Also, a replica is not a backup.

Consider this:

A user accidentally deletes one million records.

The deletion is replicated to all replicas.

Now every replica contains the deleted state.

Replication protected us from a server failure, but not from accidental deletion or logical corruption.

We still need independent backups, point-in-time recovery, retention policies, and ideally appropriately protected backup copies.

| Mechanism                           | Protects against                                                |
| ----------------------------------- | --------------------------------------------------------------- |
| Multiple AZs                        | Local hardware/AZ failure                                       |
| Cross-region replicas               | Regional failure, depending on replication and promotion design |
| Backups                             | Accidental deletion, corruption, historical recovery            |
| Immutable backups                   | Certain destructive or malicious changes                        |
| Multi-region application deployment | Regional application availability                               |

## 18. The final mental model: think in terms of ownership and copies

Let's bring everything together.

Imagine this is our global application:

```
                         GLOBAL USERS
                              |
                              v
                    GLOBAL TRAFFIC ROUTER
                              |
               +--------------+--------------+
               |              |              |
               v              v              v
           US REGION      EU REGION      ASIA REGION
               |              |              |
          Application    Application    Application
               |              |              |
          Local Cache    Local Cache    Local Cache
               |              |              |
               v              v              v
          DB / Router     DB / Router     DB / Router
               |              |              |
               +--------------+--------------+
                              |
                      Cross-region paths
                              |
             +----------------+----------------+
             |                |                |
             v                v                v
        Data Owner       Replication       Read Models
             |                |                |
             v                v                v
         Primary DB      Other Regions     Search / BI
             |                |                |
             v                v                v
          Outbox          Local Cache      Global Queries
             |
             v
         Local Queue
             |
             v
         Consumers
```

There are five important questions to answer for every data domain:

1\. Ownership

Which region owns this data?

2\. Replication

Where should copies of this data exist?

3\. Read routing

Can I read locally, or must I contact the owner?

4\. Write routing

Which region is allowed to accept the authoritative write?

5\. Consistency

How fresh must a remote copy be, and what happens when regions cannot communicate?

Once these are defined, cache placement, queues, cross-region queries, and failover become much easier to derive.

### A compact decision tree

You can use this as a starting point during interviews.

\#chatgpt-mermaid-\_r_522\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_522\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_522\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_522\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_522\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_522\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_522\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_522\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_522\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_522\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_522\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_522\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_522\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_522\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_522\_ p{margin:0;}#chatgpt-mermaid-\_r_522\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_522\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_522\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_522\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_522\_ .label text,#chatgpt-mermaid-\_r_522\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_522\_ .node rect,#chatgpt-mermaid-\_r_522\_ .node circle,#chatgpt-mermaid-\_r_522\_ .node ellipse,#chatgpt-mermaid-\_r_522\_ .node polygon,#chatgpt-mermaid-\_r_522\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_522\_ .rough-node .label text,#chatgpt-mermaid-\_r_522\_ .node .label text,#chatgpt-mermaid-\_r_522\_ .image-shape .label,#chatgpt-mermaid-\_r_522\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_522\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_522\_ .rough-node .label,#chatgpt-mermaid-\_r_522\_ .node .label,#chatgpt-mermaid-\_r_522\_ .image-shape .label,#chatgpt-mermaid-\_r_522\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_522\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_522\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_522\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_522\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_522\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_522\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_522\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_522\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_522\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_522\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_522\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_522\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_522\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_522\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_522\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_522\_ .icon-shape,#chatgpt-mermaid-\_r_522\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_522\_ .icon-shape p,#chatgpt-mermaid-\_r_522\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_522\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_522\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_522\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_522\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_522\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_522\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_522\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_522\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_522\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_522\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_522\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_522\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_522\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_522\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_522\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_522\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_522\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_522\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_522\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_522\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_522\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_522\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_522\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_522\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_522\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_522\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_522\_ .node rect,#chatgpt-mermaid-\_r_522\_ .node circle,#chatgpt-mermaid-\_r_522\_ .node ellipse,#chatgpt-mermaid-\_r_522\_ .node polygon,#chatgpt-mermaid-\_r_522\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_522\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_522\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_522\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_522\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_522\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Identify data domainMust the same data beavailable globally?Must writes happenlocally in multiple regions?Geographic shardingSingle writer / primaryCan concurrent writesbe safely merged?Multi-writer withconflict resolutionSingle writer per entityor globally coordinated DBReplicate data to requiredregionsAssign home region / ownerReplicate within approvedfailure and residencyboundariesChoose read consistencyDesign routing, cache,queues and recoveryYesNoNoYesYesNo

## 19. A practical set of rules to remember

When you are designing a system, these are the defaults I would start with:

| Component             | Recommended starting point                                                             |
| --------------------- | -------------------------------------------------------------------------------------- |
| Application servers   | Deploy in multiple regions when availability or latency requires it                    |
| Database ownership    | One authoritative owner per entity or transaction partition                            |
| Database replicas     | Within-region replicas for availability; cross-region replicas for DR or read locality |
| Global reference data | Replicate where required                                                               |
| Region-specific data  | Keep in its home region                                                                |
| Global reads          | Use local replicas, projections, or dedicated read models                              |
| Cross-region writes   | Route to the authoritative owner                                                       |
| Cache                 | Independent regional cache by default                                                  |
| CDN                   | Cache suitable public/static content at the edge                                       |
| Queue                 | Regional queues for regional workflows                                                 |
| Global events         | Explicit event forwarding or replication                                               |
| Analytics             | Separate read-optimized data pipeline                                                  |
| Failover              | Define RPO, RTO, fencing, promotion, and reconciliation                                |
| Backups               | Maintain separately from replication                                                   |

A particularly useful design default is:

Single-writer ownership per entity + regional replicas + local caches + regional queues + asynchronous event distribution.

This pattern covers a surprisingly large number of real-world systems.

You can make it more sophisticated when you have an actual requirement for unrestricted multi-writer operations, globally coordinated transactions, or more complex data distribution.

## 20. How to deepen this further: the most important concepts to master

There are a few subjects underneath this architecture that are worth understanding independently.

| Topic                         | Why it matters                                                         |
| ----------------------------- | ---------------------------------------------------------------------- |
| Leader election and quorum    | How a database decides who can accept writes                           |
| Distributed consensus         | How multiple nodes agree on committed state                            |
| Split-brain prevention        | How to avoid two regions becoming independent primaries                |
| Replication lag and WAL/CDC   | How database changes move between regions                              |
| Outbox and event replication  | How to distribute reliable business events                             |
| Consistency models            | How to define what users see after a write                             |
| Sharding and shard maps       | How to route data to its owner                                         |
| RPO/RTO and disaster recovery | How to design and measure recovery                                     |
| CQRS and materialized views   | How to support global queries without querying every database          |
| Data residency                | How to design placement and replication around geographic restrictions |

Some useful references for the underlying architecture patterns:

- [Google Cloud Spanner: regional and multi-region configurations](https://docs.cloud.google.com/spanner/docs/instance-configurations?utm_source=chatgpt.com) — a concrete example of distributed replication, quorum writes, leader regions, and read replicas.
- [AWS: Multi-Region Architecture](https://docs.aws.amazon.com/prescriptive-guidance/latest/security-reference-architecture/multi-region-architecture.html?utm_source=chatgpt.com) — disaster recovery, resilience, and when multi-region is justified.
- [Microsoft Azure: Sharding Pattern](https://learn.microsoft.com/azure/architecture/patterns/sharding?utm_source=chatgpt.com) — geographic sharding, shard routing, and combining regional and intra-region partitioning.

My biggest recommendation for your interview preparation: practice designing three data domains independently within the same system: one globally replicated, one geographically sharded, and one with strict transactional ownership. Then connect their read paths, write paths, queues, caches, and recovery flows.

Once you can do that, you won't need to memorize separate multi-region architectures for every system design problem. You'll be composing the same small set of proven patterns based on the requirements.














---







































# Multi-region request routing: from the incoming request to the correct database

This is the next important piece of multi-region architecture.

You have identified the key question: When a request reaches a server in one region, how does that server know which region and which database should handle the actual operation?

The answer is that we need to distinguish two separate routing decisions:

1. Application routing: Which region should receive and process the user's request?
2. Data routing: Which database shard and which database replica should handle the requested operation?

These are independent decisions.

For example, a user in India might send a request to an application server in Asia, but the data they need might belong to Europe. The Asian server can still process the request and communicate with the European data-owning service.

This is not an exception. It is a normal and important architecture pattern.

The mental model to remember is:

Route the request to a suitable application region. Then route each data operation to the appropriate data owner or read replica.

Let's build the complete workflow.

## 1. First, distinguish the user's location, application region, and data region

Suppose we deploy our application in three regions:

- US
- Europe
- Asia

A user is physically in India.

We have three possible locations involved in a request:

| Location                | Meaning                                                |
| ----------------------- | ------------------------------------------------------ |
| User's current location | Where the user is physically accessing the application |
| Application region      | Where the request is initially processed               |
| Data-owner region       | Where the authoritative database record is owned       |

These locations don't have to be the same.

Consider this example:

```
User:                   India
Application region:     Asia
Tenant's home region:   Europe
Database primary:       Europe
Read replica:           Asia
```

A request can follow this path:

```
                 User in India
                       |
                       v
                 Global Routing
                       |
                       v
                  Asia Region
                       |
                       v
               Asia Application
                       |
                       v
                Resolve Tenant
                       |
                       v
                Tenant belongs
                   to Europe
                       |
                       v
             Check available database
                       |
              +--------+--------+
              |                 |
              v                 v
         Asia Replica      Europe Primary
              |                 |
              v                 v
        Read (when safe)   Write / strong read
```

Notice something important:

The Asian application server can access an Asian read replica without making the user travel all the way to Europe, provided the replica is authorized, sufficiently fresh, and allowed by the data-placement policy.

For a write, the request must reach the authoritative owner in Europe.

This is the separation we need to design carefully.

## 2. Stage one: How is the initial application region selected?

When a user sends a request, the request generally doesn't choose a particular backend server directly.

It goes through a global traffic-routing layer.

A simplified architecture:

```
                       Users
                         |
                         v
                 Global Traffic Router
                         |
              +----------+----------+
              |          |          |
              v          v          v
           US Region  EU Region  Asia Region
              |          |          |
              v          v          v
           Regional   Regional   Regional
           Load       Load       Load
           Balancer   Balancer   Balancer
              |          |          |
              v          v          v
           App Servers App Servers App Servers
```

The global traffic router chooses an application region. The regional load balancer then selects an available application server.

### 2.1 What determines the application region?

Usually, several factors are considered.

A. Network latency

Send the request to a region that provides a good network path.

For example, an Indian user might be directed to Asia rather than the US.

B. Region health

If the nearest region is unavailable, route the user to another healthy region.

C. Capacity

If Asia is overloaded, traffic can be shifted to another region, subject to data and compliance constraints.

D. Data residency

Some applications must process certain data within approved geographic boundaries.

This can override the nearest-region decision.

E. Session or workflow requirements

A user might have an active workflow that benefits from remaining in a particular region.

But this should be a deliberate routing policy, not an assumption that every request must always return to the same server.

### 2.2 Does this route the user to their home region or nearest region?

There is no universal answer. There are three common approaches.

| Strategy                                            | When to use it                                                                              |
| --------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| Nearest healthy region                              | Users access mostly public or globally available data                                       |
| User/tenant home region                             | Most requests operate on geographically owned user or tenant data                           |
| Nearest application region, then data-owner routing | Application is globally deployed but data ownership is independent of application placement |

The third approach is particularly flexible.

For example:

```
User in India
      |
      v
Asia Application
      |
      +---- Read global catalog from Asia replica
      |
      +---- Read EU tenant data from EU replica/owner
      |
      +---- Write EU tenant data to EU primary
```

We can also combine the first two approaches.

For a SaaS platform where 95% of requests operate on one tenant's data, it may be more efficient to route that tenant's requests directly to its home region.

For a global social network, it might be better to route users to the nearest healthy application region and route individual data operations separately.

The important thing is not to choose one global routing rule for every system. Choose it based on the dominant request and data-access patterns.

## 3. Stage two: How does the application server identify the correct database?

Now let's get inside the application server.

Suppose the request has reached an application server in Asia.

```
GET /tenants/tenant-200/orders/order-9001
```

The application needs to answer four questions:

1. Who does this data belong to?
2. Which region owns it?
3. Which database shard contains it?
4. Which database endpoint should handle this particular operation?

We should not hardcode these decisions throughout the business logic.

Instead, introduce a data routing layer (also called a shard router, database router, or data access routing layer).

### 3.1 The architecture inside a regional application

```
                  Incoming Request
                         |
                         v
                  API Controller
                         |
                         v
                 Authentication
                 and Authorization
                         |
                         v
                  Business Service
                         |
                         v
                  Data Router
                         |
                 +-------+-------+
                 |       |       |
                 v       v       v
              Resolve  Resolve  Resolve
              Owner    Shard    Read/Write
                 |       |       |
                 +-------+-------+
                         |
                         v
                   Select Endpoint
                         |
              +----------+----------+
              |                     |
              v                     v
          Primary DB           Read Replica
              |                     |
              v                     v
            Write                Read
```

The application doesn't need to know the physical database topology in every business method.

It asks the router for the appropriate database connection.

### 3.2 What information does the router use?

The router needs several things.

A. Data ownership key

An identifier that tells us which logical entity or partition we are accessing.

Examples:

| System         | Routing key                 |
| -------------- | --------------------------- |
| SaaS           | `tenant_id`                 |
| Social network | `user_id`                   |
| E-commerce     | `order_id` or `customer_id` |
| Ticket booking | `event_id`                  |
| Messaging      | `conversation_id`           |
| Banking        | `account_id`                |

B. Operation type

Is this operation a read or write?

But even this isn't sufficient. A read can have different consistency requirements.

C. Consistency requirement

For example:

```
READ_EVENTUAL
READ_BOUNDED_STALENESS
READ_YOUR_WRITES
READ_STRONG
WRITE
```

D. Placement and topology metadata

The router needs to know:

- Which region owns the data?
- Which shard owns the partition?
- Where is that shard's primary?
- Which replicas exist?
- Which replicas are allowed for this data?
- Which replicas are healthy?
- Which replicas are sufficiently up to date?

This information should come from a routing configuration or metadata service, not from assumptions scattered through application code.

## 4. Introduce a shard directory / placement directory

Let's design the component that provides this information.

Suppose we have a multi-tenant SaaS platform.

Our tenants are geographically distributed.

We maintain a tenant placement directory:

### 4.1 Tenant placement metadata

Conceptually, it could look like this:

| tenant_id  | home_region | placement_version |
| ---------- | ----------- | ----------------- |
| tenant-100 | us-east     | 1                 |
| tenant-200 | eu-west     | 1                 |
| tenant-300 | ap-south    | 1                 |
| tenant-400 | eu-west     | 1                 |

This answers:

Which region owns this tenant?

Next, within each region, we have database shards.

### 4.2 Regional shard metadata

For example:

| region   | shard_id     | primary   |
| -------- | ------------ | --------- |
| us-east  | us-shard-1   | US AZ-A   |
| us-east  | us-shard-2   | US AZ-A   |
| eu-west  | eu-shard-1   | EU AZ-A   |
| eu-west  | eu-shard-2   | EU AZ-A   |
| ap-south | asia-shard-1 | Asia AZ-A |

And each shard has replicas:

| Shard        | Primary   | Replica 1 | Replica 2      |
| ------------ | --------- | --------- | -------------- |
| us-shard-1   | US AZ-A   | US AZ-B   | EU approved DR |
| us-shard-2   | US AZ-A   | US AZ-B   | EU approved DR |
| eu-shard-1   | EU AZ-A   | EU AZ-B   | EU AZ-C        |
| eu-shard-2   | EU AZ-A   | EU AZ-B   | EU AZ-C        |
| asia-shard-1 | Asia AZ-A | Asia AZ-B | Approved DR    |

These are illustrative placements, not a recommendation to move European data into the US or vice versa. The actual replica locations must respect the applicable data-placement restrictions.

We can keep this metadata in a control-plane database and distribute it to application regions.

### 4.3 Why separate placement metadata from application configuration?

Imagine we have 5,000 tenants.

We don't want to deploy new application code every time we move a tenant to another shard.

Instead:

```
                Control Plane
                      |
              Placement Directory
                      |
          +-----------+-----------+
          |           |           |
          v           v           v
       US Router   EU Router   Asia Router
          |           |           |
          v           v           v
       Local      Local       Local
       Routing    Routing     Routing
       Cache      Cache       Cache
```

The application regions cache the routing metadata locally.

When tenant placement changes, the control plane publishes an updated mapping.

The routers refresh their metadata.

This is called control plane and data plane separation.

- Control plane: manages ownership, shard assignments, topology, and routing configuration.
- Data plane: receives requests and executes the actual operations using that configuration.

This separation becomes extremely valuable at scale.

### 4.4 What happens when metadata is cached?

Suppose Asia has cached this mapping:

```
tenant-200 -> EU Shard 1
```

Then the tenant is moved to EU Shard 2.

Some Asian application servers may still have the old mapping.

A production-quality design should use:

- Versioned routing metadata.
- Cache invalidation or refresh.
- A safe fallback when a mapping is outdated.
- Controlled migration.
- A routing epoch or generation number.

Don't assume that changing one mapping instantly updates every application instance.

Also, don't let a stale application route a write to an old primary after a failover. The write path needs a way to reject stale ownership information.

## 5. The actual request workflow: step by step

Let's use a real example.

A user in India accesses Tenant 200, which belongs to Europe.

The request arrives at an Asian application server.

The user wants to read an order.

### 5.1 Read workflow

```
GET /tenants/tenant-200/orders/order-9001
```

The flow:

```
             User in India
                   |
                   v
              Asia Region
                   |
                   v
            Asia Application
                   |
                   v
             Authenticate
                   |
                   v
          Extract tenant_id
             tenant-200
                   |
                   v
            Data Router
                   |
                   v
        Tenant Placement Directory
                   |
                   v
         tenant-200 -> EU Region
                   |
                   v
           Determine DB shard
                   |
                   v
              EU Shard 1
                   |
                   v
        Determine read consistency
                   |
             +-----+------+
             |            |
             v            v
      Replica eligible?   No suitable
             |            replica
             v                |
        Select replica       v
             |           EU Primary
             v                |
         Read data            |
             |                |
             +-------+--------+
                     |
                     v
               Return result
```

In this example, a nearby Asian replica could be used if it contains the required data and satisfies the consistency and residency requirements.

If there is no eligible replica, the read can be sent to the European primary.

Notice that the application server doesn't blindly use the nearest database.

It first establishes the correct data owner and shard. Only then does it choose a suitable endpoint.

### 5.2 Write workflow

Now consider:

```
PATCH /tenants/tenant-200/orders/order-9001
```

The routing flow is almost the same, except the operation requires the authoritative write target.

```
             User in India
                   |
                   v
              Asia Region
                   |
                   v
            Asia Application
                   |
                   v
             Authenticate
                   |
                   v
          Extract tenant_id
                   |
                   v
             Data Router
                   |
                   v
         Tenant Placement Directory
                   |
                   v
             EU Region
                   |
                   v
              EU Shard 1
                   |
                   v
              EU Primary
                   |
                   v
              Commit Write
                   |
             +-----+------+
             |            |
             v            v
       EU Replicas    Outbox Event
                          |
                          v
                     EU Queue
```

The Asian application server can forward the operation to the European order service. In a service-oriented architecture, this is often preferable to allowing the Asian application to connect directly to the European database.

The service owning the data is responsible for its database and transaction rules.

### 5.3 Who actually performs replica selection?

There are two common architectures.

Option A: Application-level read/write routing

The application has a database router.

```
Application
    |
    v
Data Access Layer
    |
    +---- Write -> Primary endpoint
    |
    +---- Read  -> Eligible replica endpoint
```

The application configures separate database connection pools.

For example:

```
Database Connections

Primary:
  eu-shard-1-primary.internal

Read replicas:
  eu-shard-1-replica-a.internal
  eu-shard-1-replica-b.internal
  asia-eu-shard-1-replica.internal
```

The router chooses the endpoint based on the operation and policy.

Option B: Database proxy or managed endpoint

The application connects through a database-aware proxy or endpoint:

```
Application
     |
     v
Database Proxy
     |
     +---- Writes -> Primary
     |
     +---- Reads  -> Read Replicas
```

The proxy performs some of the routing work.

However, don't assume every database proxy understands:

- Your tenant ownership.
- Geographic residency.
- Your business-level consistency requirements.
- Which replica has observed a particular user's last write.

The application or service still needs to make those higher-level decisions.

For a multi-region application, a combination of application-level data placement and database-level endpoint management is common.

## 6. How should the router decide between a primary and a replica?

Let's make the read/write decision precise.

Suppose a shard has:

```
Shard: EU Shard 1

Primary:
  EU AZ-A

Replicas:
  EU AZ-B
  EU AZ-C
  Asia Region
```

A request arrives in Asia.

The router has to consider three things:

1. Which shard owns this data?
2. Which replicas are eligible?
3. Which consistency guarantee does the operation require?

### 6.1 Decision table

| Operation                                    | Preferred target                          | Reason                        |
| -------------------------------------------- | ----------------------------------------- | ----------------------------- |
| Insert order                                 | Primary                                   | Authoritative write           |
| Update order                                 | Primary                                   | Authoritative write           |
| Delete order                                 | Primary                                   | Authoritative write           |
| Read public order summary                    | Nearby eligible replica                   | Stale reads may be acceptable |
| Read after updating order                    | Primary or sufficiently caught-up replica | Read-your-writes              |
| Read payment status for a financial decision | Strong authoritative read                 | Avoid stale decision          |
| Generate analytics report                    | Read model / warehouse                    | Optimized for aggregation     |
| Global search                                | Search index                              | Optimized for discovery       |

### 6.2 A more sophisticated replica-selection algorithm

Suppose we receive:

```
Operation: READ
Consistency: EVENTUAL
Request Region: Asia
Data Owner: Europe
Shard: EU Shard 1
```

The router identifies all eligible replicas.

```
                    All Replicas
                         |
                         v
                 Filter by Policy
                         |
          +--------------+--------------+
          |              |              |
          v              v              v
       EU AZ-B        EU AZ-C        Asia
          |              |              |
          +--------------+--------------+
                         |
                         v
                 Remove Unhealthy
                         |
                         v
                 Check Replication Lag
                         |
                         v
                Check Residency Rules
                         |
                         v
                Rank by Network Latency
                         |
                         v
                 Select Replica
```

A useful abstract selection rule is:

```
Eligible replica =
    healthy
    AND authorized for this data
    AND within allowed staleness
    AND capable of satisfying the consistency requirement
```

Then select the best replica among the eligible candidates.

If no suitable replica is available:

- Fall back to the primary, if the latency and availability policy allows it.
- Wait for a replica to catch up, if appropriate.
- Return a retryable error or temporarily reject the request if neither is safe.

Don't silently use an unsuitable replica just because it is closest.

### 6.3 Example: read-your-writes

Suppose:

1. User updates an order in Europe.
2. The European primary commits the update.
3. The application receives success.
4. The user immediately requests the order from Asia.
5. The Asian replica has not yet applied the change.

We have a stale-read problem.

One solution is to return a consistency token in the write response.

Conceptually:

```
{
  "orderId": "order-9001",
  "version": 15,
  "consistencyToken": "commit-position-abc"
}
```

The next read sends that token.

```
Asia Application
       |
       v
Data Router
       |
       v
Asia Replica
       |
       v
Has applied required commit position?
       |
   +---+---+
   |       |
  Yes      No
   |       |
   v       v
 Read    Wait or route
         to primary
```

The exact token depends on the database. It might be a log sequence number, a transaction timestamp, or an application-level version.

This is one way to implement session consistency.

Key rule: replica selection is not just a latency decision. It is a consistency decision.

## 7. Now the important part: where does consistent hashing fit into multi-region routing?

This is where we need to be particularly careful.

Consistent hashing solves a different problem from geographic routing.

Consistent hashing answers: which logical shard owns this key?

It does not inherently answer:

- Which region should receive the user's request?
- Which region owns the user's data?
- Whether a database is a primary or replica.
- Whether a replica is sufficiently up to date.

Those decisions need to be layered around the hashing algorithm.

Let's start with a single-region example and evolve it.

### 7.1 Consistent hashing in a single region

Imagine we have a social media application.

We have 100 million users.

Instead of storing every user on one database, we have four database shards:

```
                    Application
                         |
                         v
                    Shard Router
                         |
             +-----------+-----------+
             |           |           |
             v           v           v
          Shard 1     Shard 2     Shard 3
                          |
                         Shard 4
```

We need to decide which shard owns a user.

A simple approach is:

```
shard_id = hash(user_id) % number_of_shards
```

For four shards:

```
user_id = 100
hash(100) % 4 = 2

user_id = 200
hash(200) % 4 = 0
```

This works initially.

But suppose we add a fifth shard.

```
shard_id = hash(user_id) % 5
```

A large proportion of keys may now map to different shards.

This means we potentially have to move a huge amount of data.

Consistent hashing reduces this remapping problem.

### 7.2 How consistent hashing works

Imagine a circular hash space.

We place the database shard nodes on this ring.

We also hash each user ID to a position on the ring.

A key belongs to the first shard node encountered while moving clockwise.

```
                 Hash Ring

                     0
                     |
              Shard A
                  /
                 /
       User 100 *
               /
              /
         Shard B
              |
              |
        User 200 *
              |
          Shard C
              |
              |
          Shard D
```

In production implementations, we commonly use virtual nodes to distribute keys more evenly across physical shard nodes.

The advantage:

When we add Shard E, we only need to move the keys whose ownership changes, rather than redistributing the entire dataset.

Consistent hashing is therefore a way to map keys to logical shard nodes while limiting reassignment when the shard membership changes.

But here's the key question:

What happens when we introduce multiple regions?

There are two important ways to use this concept.

## 8. Multi-region hashing: the correct way to apply it

### Approach 1: two-level routing — region first, shard second

This is one of the most useful patterns when data has geographic ownership.

We split routing into two stages.

Stage A: determine the data's home region.

Stage B: use hashing to find the shard inside that region.

```
                  Incoming Request
                         |
                         v
                  Extract tenant_id
                         |
                         v
                Determine Home Region
                         |
             +-----------+-----------+
             |           |           |
             v           v           v
          US Region  EU Region   Asia Region
             |           |           |
             v           v           v
         Hash to      Hash to     Hash to
         US shard     EU shard    Asia shard
             |           |           |
             v           v           v
         Shard 1     Shard 2     Shard 3
```

Let's understand this using a SaaS application.

Suppose we have:

```
tenant-100 -> US
tenant-200 -> Europe
tenant-300 -> Asia
```

We first look up the tenant's home region.

Then, inside the relevant regional database cluster, we calculate the shard.

For example:

```
Request: tenant-200
       |
       v
Home Region = Europe
       |
       v
Hash(tenant-200)
       |
       v
Hash Slot = 4500
       |
       v
Slot 4500 belongs to EU Shard 2
       |
       v
EU Shard 2 Primary
```

The database cluster can maintain its own consistent-hashing ring or hash-slot ownership map.

This produces:

```
US Region
    |
    +---- Hash Slots
             |
             +---- US Shard 1
             +---- US Shard 2
             +---- US Shard 3


Europe Region
    |
    +---- Hash Slots
             |
             +---- EU Shard 1
             +---- EU Shard 2
             +---- EU Shard 3


Asia Region
    |
    +---- Hash Slots
             |
             +---- Asia Shard 1
             +---- Asia Shard 2
             +---- Asia Shard 3
```

Each region has its own partitioning scheme.

Why is this useful?

Because geographic ownership and horizontal scaling remain independent.

We can add a new European shard without moving US or Asian tenants.

We can migrate a tenant to another region without necessarily changing the hash algorithm.

We can scale each region according to its own workload.

### Approach 1: what happens to replicas?

Let's extend EU Shard 2.

```
                  EU Shard 2
                      |
                +-----+-----+
                |           |
                v           v
             Primary     Replica
             EU AZ-A     EU AZ-B
                            |
                            v
                      Read Replica
                       Asia Region
                       (if permitted)
```

Our two-level routing now has three stages:

```
1. Resolve region
       |
       v
2. Resolve logical shard
       |
       v
3. Resolve physical database endpoint
       |
       +---- Write -> Primary
       |
       +---- Read  -> Eligible replica
```

This is a very useful architecture to use in system design interviews.

### Approach 2: globally sharded data with replicas in multiple regions

Now consider data that is genuinely global.

For example, a globally distributed product catalog.

There is no requirement that European customers' products belong exclusively to Europe or that US products belong exclusively to the US.

We can have one logical sharding topology:

```
                 Global Product Dataset
                           |
                    Global Shard Router
                           |
             +-------------+-------------+
             |             |             |
             v             v             v
          Shard 1       Shard 2       Shard 3
```

Each shard has a primary and replicas in different regions.

For example:

```
                  Logical Shard 1

          US Region          EU Region         Asia Region
              |                  |                  |
              v                  v                  v
          Primary DB --------> Replica DB ------> Replica DB
```

And independently:

```
                  Logical Shard 2

          US Region          EU Region         Asia Region
              |                  |                  |
              v                  v                  v
          Replica DB <-------- Primary DB ------> Replica DB
```

The write owner might differ by logical shard.

For example:

| Logical shard | Primary region | Replica regions |
| ------------- | -------------- | --------------- |
| Shard 1       | US             | Europe, Asia    |
| Shard 2       | Europe         | US, Asia        |
| Shard 3       | Asia           | US, Europe      |

Now the global shard router first identifies the logical shard. The request routing layer then selects the appropriate regional endpoint for that shard.

The important thing is that the same shard has one authoritative primary, even though it has copies in several regions.

This architecture can support global reads and distribute primary writes across regions at the shard level.

But the write for a specific key still goes to its shard's owner.

### 8.1 Which approach should you choose?

| Requirement                                           | Preferred routing                               |
| ----------------------------------------------------- | ----------------------------------------------- |
| Tenant data must belong to a specific region          | Region first, then shard                        |
| User data has a home region                           | Region first, then shard                        |
| All data is global and residency is not restrictive   | Global shard map, followed by replica selection |
| Inventory belongs to a warehouse                      | Warehouse/event owner first, then shard         |
| Global product catalog                                | Global shard map with regional replicas         |
| Different data domains have different placement rules | Separate routing maps per domain                |

Don't blindly use one global consistent-hashing ring for every table in your application.

Different data domains may need different partition keys, regional ownership, and replication policies.

## 9. Consistent hashing versus hash slots: how to configure it

You mentioned both consistent hashing and hash slots.

They are related approaches to distributing keys, but their operational details are different.

### 9.1 Consistent hashing

In consistent hashing:

1. Create a hash space.
2. Assign shard nodes to positions in that space.
3. Hash the key.
4. Locate the next shard node on the ring.
5. Route the key to that shard.

Virtual nodes are generally used to improve distribution.

Conceptually:

```
hash(tenant_id) -> ring_position -> shard_id
```

### 9.2 Hash slots

Another approach is to divide the entire key space into a fixed number of slots.

For example:

```
Total slots = 16384

Slot 0-2047       -> Shard 1
Slot 2048-4095   -> Shard 2
Slot 4096-6143   -> Shard 3
...
```

The application calculates:

```
slot = HASH(partition_key) % 16384
```

Then it looks up which shard owns that slot.

Redis Cluster uses this type of model: keys map to 16,384 hash slots, and slots are assigned to cluster nodes. The cluster supports moving slots between nodes as the topology changes.

The important benefit is that the slot space remains stable even when physical nodes change.

For a multi-region system, we can use the same conceptual hierarchy.

```
                   tenant_id
                       |
                       v
                 Determine Region
                       |
                       v
                  Calculate Slot
                       |
                       v
                Find Slot Owner
                       |
                       v
               Resolve Shard Primary
                       |
                       v
              Resolve Read Replicas
```

For example:

```
tenant-200
    |
    v
EU Region
    |
    v
Hash Slot 4500
    |
    v
EU Shard 2
    |
    +---- Primary: EU AZ-A
    |
    +---- Replica: EU AZ-B
    |
    +---- Optional replica: approved remote region
```

### 9.3 Should the application calculate the slot itself?

It can.

But the application must have an up-to-date slot-to-shard mapping.

For example:

```
Slot 0-2047      -> eu-shard-1
Slot 2048-4095  -> eu-shard-2
Slot 4096-6143  -> eu-shard-3
```

This mapping can be distributed through routing metadata.

Alternatively, the database cluster or proxy may own slot discovery and routing.

The choice depends on the database and architecture.

### 9.4 What happens when a shard is added?

Suppose the European region initially has three shards.

```
EU Shard 1
EU Shard 2
EU Shard 3
```

We add EU Shard 4.

We don't want to arbitrarily change the number of shards in a modulo calculation and remap every key.

Instead, we use consistent hashing or fixed hash slots.

For example:

```
Before:

Slots 0-4999       -> Shard 1
Slots 5000-9999    -> Shard 2
Slots 10000-16383  -> Shard 3

After:

Slots 0-2499       -> Shard 1
Slots 2500-4999    -> Shard 4
Slots 5000-9999    -> Shard 2
Slots 10000-16383  -> Shard 3
```

We can move a subset of slots from Shard 1 to Shard 4.

But remember:

Changing the routing map does not move the actual database records.

A safe resharding operation needs to:

1. Select the slots to move.
2. Copy or stream their data to the destination shard.
3. Handle writes occurring during migration.
4. Verify the destination data.
5. Update routing metadata.
6. Stop serving the old ownership once safe.
7. Clean up the old data when the migration is complete.

This is a separate distributed data migration workflow.

### 9.5 One major mistake: using a different ring in each region

Imagine Asia, Europe, and the US have independently generated shard rings.

The same key might map to different physical shards in each region because the rings or node membership differ.

For example:

```
hash(user-100)

US Ring     -> Shard 1
Europe Ring -> Shard 3
Asia Ring   -> Shard 2
```

That is not necessarily wrong if these are intentionally independent regional datasets.

But it is wrong if those regions are supposed to contain replicas of the same logical shard.

For replicated data, the system must have a shared logical partition identity and consistent ownership metadata.

For example:

```
user-100 -> Logical Shard 1

Logical Shard 1:
    Primary: US DB
    Replica: EU DB
    Replica: Asia DB
```

The different regional endpoints must still represent the same logical partition.

### 9.6 Do you need hashing to select the home region?

Usually, don't use an unqualified global consistent-hashing ring to decide the user's home region.

For geographically owned data, use an explicit placement policy.

For example:

```
tenant-100 -> US
tenant-200 -> EU
tenant-300 -> Asia
```

Why?

Because home-region assignment may depend on:

- Data residency.
- Customer contract.
- Legal jurisdiction.
- Tenant onboarding.
- Capacity.
- Enterprise requirements.
- Explicit migration decisions.

A hash algorithm doesn't understand these business rules.

You may use deterministic hashing to assign new tenants across eligible regions for load distribution, but the resulting assignment should be persisted as authoritative placement metadata.

For example:

```
New tenant
    |
    v
Determine allowed regions
    |
    v
Select region based on capacity / distribution policy
    |
    v
Persist tenant -> home region
    |
    v
Hash tenant within that region to find a shard
```

After assignment, the application reads the persisted mapping rather than recalculating a different home region every time.

This is important because adding or removing regions must not unexpectedly change the home region of existing tenants.

## 10. A useful distinction: partition key versus region key

These are two separate architectural decisions.

Consider a SaaS platform:

```
tenant_id = tenant-200
user_id = user-900
project_id = project-500
```

We might choose:

```
Region Key:      tenant_id
Partition Key:   tenant_id
```

So all records for one tenant belong to the same logical shard.

Alternatively:

```
Region Key:      tenant_id
Partition Key:   project_id
```

Here, the tenant still belongs to Europe, but its projects may be spread across several European shards.

Which should we choose?

It depends on the access patterns and transactions.

### Example A: tenant-based partitioning

```
Tenant 200
   |
   v
EU Region
   |
   v
EU Shard 1

All tenant data:
    Users
    Projects
    Documents
    Permissions
```

Advantages:

- Tenant-specific queries are local to one shard.
- Easier tenant isolation.
- Tenant-level transactions can stay within one database.
- Easier to move the whole tenant.

Disadvantage:

\- A very large tenant can become a hot shard.

### Example B: project-based partitioning

```
Tenant 200
   |
   v
EU Region
   |
   +---- Project 1 -> EU Shard 1
   |
   +---- Project 2 -> EU Shard 2
   |
   +---- Project 3 -> EU Shard 3
```

Advantages:

- Large tenants can scale across shards.
- Project-specific operations are well distributed.

Disadvantages:

- Tenant-wide queries may touch multiple shards.
- Cross-project transactions become more complicated.
- Moving or managing tenant data becomes more involved.

A good rule is:

Choose the partition key that keeps the most important transactional and query operations together.

Do not choose a shard key merely because it produces a uniform hash distribution.

Data locality and transaction locality matter just as much.

## 11. How routing works in different real-world systems

Let's take the same routing architecture and see how the routing key and region selection change for different systems.

This is important because there is no universal shard key or routing algorithm.

### 11.1 SaaS application: route using tenant ID

Suppose a request comes in:

```
GET /tenants/tenant-200/projects/project-500
```

Routing:

```
1. Extract tenant_id = tenant-200
2. Look up tenant's home region
3. tenant-200 -> EU
4. Hash tenant_id to determine shard
5. Find shard's primary and replicas
6. Select endpoint based on operation
```

The tenant ID is a good partition key if most operations involve one tenant and tenant-level transactions should remain together.

```
India User
    |
    v
Asia Application
    |
    v
Tenant Router
    |
    v
EU Region
    |
    v
Hash(tenant-200)
    |
    v
EU Shard 2
    |
    +---- Read -> Eligible replica
    |
    +---- Write -> Primary
```

### 11.2 Social network: route using user ID

Suppose:

```
GET /users/user-100/profile
```

We have:

```
user-100 -> US home region
```

An Indian user wants to view this profile.

There are two possible flows.

Public profile read:

```
India User
    |
    v
Asia Application
    |
    v
User ID Router
    |
    v
User 100 belongs to US
    |
    v
Asia local profile replica
    |
    v
Return profile
```

Profile update:

```
India User
    |
    v
Asia Application
    |
    v
User ID Router
    |
    v
US Data Owner
    |
    v
US Primary
    |
    v
Update profile
```

For social networks, we might also use a separate partitioning strategy for posts and feeds.

For example:

```
user_id -> home region -> user shard
post_id -> post shard
feed_id -> feed shard
```

Don't assume that every entity in a social network must be stored on the same shard just because it belongs to a user.

The right choice depends on whether you optimize for user-profile operations, post retrieval, feed generation, or another access pattern.

### 11.3 Ticket booking: route using event ID or inventory owner

Suppose a user wants to reserve a seat for an event in London.

```
POST /events/event-500/reservations
```

The correct ownership key is usually related to the event or its inventory owner, not the user's current location.

```
User in India
      |
      v
Asia Application
      |
      v
Extract event_id
      |
      v
Event Ownership Directory
      |
      v
event-500 -> EU
      |
      v
Hash(event_id)
      |
      v
EU Inventory Shard 2
      |
      v
EU Primary
      |
      v
Atomic Seat Reservation
```

For this particular system, the write must go to the authoritative inventory owner.

A nearby Asian read replica can help with browsing event information, but it cannot independently authorize a seat reservation.

### 11.4 E-commerce: different routing keys for different domains

An e-commerce system might have:

```
Product:
    product_id -> global product shard

Customer:
    customer_id -> customer home region

Order:
    order_id -> order owner

Inventory:
    warehouse_id + product_id -> inventory owner

Payment:
    payment/account ownership -> authoritative payment system
```

Even though these entities belong to the same e-commerce application, they have different routing requirements.

This is why a single global database router with one universal hashing algorithm is usually not enough.

We need routing policies per data domain.

### 11.5 Chat application: route using conversation ID

Suppose two people in different countries communicate.

A sensible ownership decision is to assign the conversation to a logical owner region.

```
conversation-100 -> Europe
```

All messages for that conversation go to the owning conversation service.

```
User A in US
       |
       v
US Application
       |
       v
Conversation Router
       |
       v
EU Conversation Service
       |
       v
EU Message Shard
       |
       v
Persist message
       |
       v
Replicate / Publish notification
       |
       v
User B in Asia
```

Why use `conversation_id`?

Because we often want:

- Messages in a conversation to be stored together.
- Ordering within a conversation.
- One authoritative sequence of message versions.
- Efficient conversation-history queries.

Other conversations can belong to different regions.

This gives us multiple active regions without requiring every message in a conversation to be independently coordinated worldwide.

## 12. How to configure routing policies properly

So far, we have discussed the architecture conceptually.

Now let's see how we might represent its configuration.

A useful design is to maintain routing rules per data domain rather than scattered `if/else` conditions throughout the application.

For example, an illustrative configuration could look like this:

```
data_domains:

  users:
    placement_strategy: home_region
    partition_key: user_id
    shard_strategy: consistent_hashing
    write_policy: single_primary
    read_policy: regional_replica
    max_replica_lag_ms: 2000

  orders:
    placement_strategy: tenant_home_region
    partition_key: tenant_id
    shard_strategy: hash_slots
    write_policy: single_primary
    read_policy: primary_for_consistency_sensitive_reads

  products:
    placement_strategy: global
    partition_key: product_id
    shard_strategy: hash_slots
    write_policy: designated_shard_owner
    read_policy: nearest_eligible_replica

  inventory:
    placement_strategy: warehouse_region
    partition_key: warehouse_id
    shard_strategy: hash_slots
    write_policy: authoritative_owner
    read_policy: regional_projection
```

This isn't a standard configuration format. It illustrates the kinds of decisions that should be explicit in your architecture.

Let's define the responsibilities of the router.

### 12.1 Routing policy

The routing policy understands:

- Which key identifies ownership.
- Whether ownership is regional or global.
- Which consistency requirement applies.
- Which replicas can serve reads.
- What to do when the primary or replicas are unavailable.

### 12.2 Placement directory

The placement directory maps entities or tenants to logical regions.

```
tenant-200 -> EU
user-100   -> US
event-500  -> EU
```

### 12.3 Shard map

The shard map maps a partition key or hash slot to a logical shard.

```
EU:
    slot 0-2047      -> EU Shard 1
    slot 2048-4095  -> EU Shard 2
    slot 4096-6143  -> EU Shard 3
```

### 12.4 Database topology

The topology maps each logical shard to its physical endpoints.

```
EU Shard 2:

    Primary:
        eu-db-2-primary

    Replicas:
        eu-db-2-replica-a
        eu-db-2-replica-b
        approved-remote-replica
```

Notice that these are four separate concerns:

```
Placement:
    Which region?

Partitioning:
    Which logical shard?

Topology:
    Which physical database endpoints?

Access policy:
    Which endpoint for this operation?
```

Keeping these decisions separate makes the architecture significantly easier to operate and evolve.

## 13. A simplified implementation of the database router

Let's translate the design into pseudocode.

Imagine our application exposes a routing abstraction:

```
DatabaseConnection resolve(
    DataDomain domain,
    String partitionKey,
    Operation operation,
    ConsistencyLevel consistency
);
```

For example:

```
resolve(
    DataDomain.ORDERS,
    "tenant-200",
    Operation.READ,
    ConsistencyLevel.EVENTUAL
);
```

The internal flow could be:

```
                    resolve()
                       |
                       v
                Load Domain Policy
                       |
                       v
              Resolve Data Ownership
                       |
                       v
                 Determine Region
                       |
                       v
               Calculate Hash Slot
                       |
                       v
                Resolve Logical Shard
                       |
                       v
               Load Shard Topology
                       |
             +---------+---------+
             |                   |
             v                   v
           WRITE                READ
             |                   |
             v                   v
       Select Primary      Filter Replicas
             |                   |
             |                   v
             |             Check Health
             |                   |
             |                   v
             |             Check Lag
             |                   |
             |                   v
             |             Check Policy
             |                   |
             |                   v
             |             Rank by Latency
             |                   |
             |                   v
             |             Select Replica
             |                   |
             +---------+---------+
                       |
                       v
               Return Connection
```

An illustrative implementation:

```
DatabaseEndpoint resolve(
    DataDomain domain,
    String partitionKey,
    Operation operation,
    ConsistencyLevel consistency,
    String requestRegion,
    ConsistencyToken token
) {
    // 1. Load the data placement policy.
    DomainPolicy policy = policyRegistry.get(domain);

    // 2. Resolve the owning region.
    Region ownerRegion = placementDirectory.resolve(
        domain,
        partitionKey
    );

    // 3. Resolve the logical shard.
    Shard shard = shardMap.resolve(
        ownerRegion,
        partitionKey,
        policy.shardStrategy()
    );

    // 4. Writes always use the authoritative write owner.
    if (operation == Operation.WRITE) {
        return topology.getCurrentPrimary(shard);
    }

    // 5. Strong reads use the authoritative read path.
    if (consistency == ConsistencyLevel.STRONG) {
        return topology.getStrongReadEndpoint(shard);
    }

    // 6. Find eligible replicas.
    List<DatabaseEndpoint> candidates =
        topology.getReplicas(shard).stream()
            .filter(DatabaseEndpoint::isHealthy)
            .filter(replica -> policy.isAllowedLocation(
                replica.region()
            ))
            .filter(replica -> policy.isWithinStalenessLimit(
                replica
            ))
            .filter(replica -> token == null
                || replica.hasApplied(token))
            .toList();

    // 7. Select the best eligible replica.
    if (!candidates.isEmpty()) {
        return latencySelector.selectBest(
            requestRegion,
            candidates
        );
    }

    // 8. Fall back to the primary if policy permits.
    if (policy.allowPrimaryFallback()) {
        return topology.getCurrentPrimary(shard);
    }

    throw new NoSuitableReplicaException(shard.id());
}
```

This is conceptual Java code, not a complete production implementation. In practice, primary selection also needs leader/failover epochs, endpoint discovery, connection-pool management, deadlines, and safe transaction handling.

One other important point: a strong read is not necessarily just a read from a node called `primary`. Distributed databases may support other strongly consistent read mechanisms. The endpoint-selection logic must follow the database's actual consistency guarantees.

### 13.1 Should this be implemented in every microservice?

Not necessarily.

You have three choices:

A. Shared routing library

All services use a common routing library.

Advantages:

- Consistent behavior.
- Centralized routing logic.
- Less duplicated implementation.

The library needs a safe configuration refresh mechanism and version compatibility.

B. Database proxy or routing service

Services connect through a managed router or proxy.

Advantages:

- Centralized topology awareness.
- Easier connection management.
- Less routing logic inside application services.

It must still receive enough information to route correctly.

C. Data-owning service

For cross-region requests, call the service that owns the entity.

This is often the cleanest microservice boundary:

```
Asia API
    |
    v
Tenant Router
    |
    v
Europe Order Service
    |
    v
Europe Database Router
    |
    v
Europe Order Shard
```

The Europe Order Service owns the database and decides which physical database endpoint to use.

My preferred general design is to keep ownership resolution in a reusable routing/control-plane capability and keep database access inside the service that owns that data.

## 14. Important: every storage layer has its own routing map

This is a subtle but important point.

Suppose our application uses:

- PostgreSQL for persistent data.
- Redis for caching.
- Kafka for asynchronous events.

Do all three use the same hash algorithm and shard mapping?

Not automatically.

Each storage system has its own partitioning and topology.

For example:

```
                tenant-200
                     |
             Placement Directory
                     |
                     v
                 EU Region
                     |
          +----------+----------+
          |          |          |
          v          v          v
       Postgres    Redis      Kafka
       Shard 2     Cluster    Cluster
          |          |          |
          v          v          v
       DB Router  Redis Slot  Topic Partition
```

These are separate routing decisions.

### PostgreSQL

We might use:

```
tenant_id -> EU region -> DB shard 2
```

### Redis

Redis Cluster may map a cache key to a hash slot and then to a Redis node.

For example:

```
cache:tenant-200:order-9001
        |
        v
    Hash Slot
        |
        v
    Redis Node
```

That Redis node is not automatically PostgreSQL Shard 2.

### Kafka

We might publish events using:

```
key = order_id
```

Kafka uses that key for partition selection.

```
order_id
    |
    v
Kafka partition
    |
    v
Consumer for that partition
```

This helps preserve the required per-key ordering within the Kafka topic's partitioning model.

Don't assume the hash slot of Redis, the hash partition of PostgreSQL, and the Kafka partition are the same thing.

You may deliberately align partitioning across systems to improve locality, but they remain separate infrastructures with their own mappings and failure behavior.

For example, all three might use `tenant_id` as a logical grouping key, but PostgreSQL, Redis, and Kafka will independently map that key to their respective physical partitions.

## 15. What happens when the routing configuration changes?

A distributed router must handle changes without requiring application redeployment.

There are three important changes.

### 15.1 A primary database fails

Normal configuration:

```
EU Shard 2

Primary:  EU AZ-A
Replica:  EU AZ-B
Replica:  EU AZ-C
```

EU AZ-A fails.

The failover process:

```
Detect failure
     |
     v
Elect / select new primary
     |
     v
Fence old primary
     |
     v
Promote EU AZ-B
     |
     v
Update shard topology
     |
     v
Publish new routing epoch
     |
     v
Application routers refresh
     |
     v
New writes go to EU AZ-B
```

The logical shard has not changed.

Only its primary endpoint has changed.

This is important because we shouldn't need to change the hashing algorithm or move the data to a different shard when promoting a replica.

### 15.2 A new shard is added

Initially:

```
EU:
    Shard 1
    Shard 2
    Shard 3
```

We add Shard 4.

The shard map and hash-slot ownership are updated through a controlled resharding operation.

```
Old shard map
      |
      v
Select slots to move
      |
      v
Copy / synchronize data
      |
      v
Verify destination
      |
      v
Switch routing map
      |
      v
New shard map
```

Existing requests should continue working during migration.

In a production system, we also need to prevent requests from using an out-of-date mapping for writes. This might involve routing epochs, migration states, forwarding from the old shard, or temporary ownership coordination.

### 15.3 A tenant moves to another region

Suppose:

```
tenant-200 -> Europe
```

The customer requires migration to the US region.

We should not simply change:

```
tenant-200 -> US
```

because the data is still physically present in Europe.

A safe migration requires:

1. Create the destination database/shard.
2. Copy existing tenant data.
3. Capture or synchronize ongoing changes.
4. Briefly coordinate writes or use a controlled dual-write/catch-up mechanism.
5. Verify the destination data.
6. Switch authoritative ownership.
7. Update routing metadata.
8. Ensure old application instances cannot continue writing to the previous owner.
9. Retain or delete the old data according to the migration and retention policy.

The directory change is the final part of a data migration, not the entire migration.

## 16. The routing architecture I would recommend as a starting point

Let's put all the concepts together into one reusable design.

\#chatgpt-mermaid-\_r_6pg\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_6pg\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_6pg\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_6pg\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_6pg\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6pg\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_6pg\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_6pg\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_6pg\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_6pg\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_6pg\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_6pg\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6pg\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6pg\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_6pg\_ p{margin:0;}#chatgpt-mermaid-\_r_6pg\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6pg\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6pg\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6pg\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_6pg\_ .label text,#chatgpt-mermaid-\_r_6pg\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6pg\_ .node rect,#chatgpt-mermaid-\_r_6pg\_ .node circle,#chatgpt-mermaid-\_r_6pg\_ .node ellipse,#chatgpt-mermaid-\_r_6pg\_ .node polygon,#chatgpt-mermaid-\_r_6pg\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_6pg\_ .rough-node .label text,#chatgpt-mermaid-\_r_6pg\_ .node .label text,#chatgpt-mermaid-\_r_6pg\_ .image-shape .label,#chatgpt-mermaid-\_r_6pg\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_6pg\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_6pg\_ .rough-node .label,#chatgpt-mermaid-\_r_6pg\_ .node .label,#chatgpt-mermaid-\_r_6pg\_ .image-shape .label,#chatgpt-mermaid-\_r_6pg\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_6pg\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_6pg\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6pg\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6pg\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_6pg\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_6pg\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_6pg\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6pg\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6pg\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_6pg\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_6pg\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6pg\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6pg\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_6pg\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6pg\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_6pg\_ .icon-shape,#chatgpt-mermaid-\_r_6pg\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_6pg\_ .icon-shape p,#chatgpt-mermaid-\_r_6pg\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_6pg\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_6pg\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6pg\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_6pg\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_6pg\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_6pg\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_6pg\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_6pg\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_6pg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6pg\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_6pg\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_6pg\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_6pg\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6pg\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_6pg\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_6pg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6pg\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_6pg\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_6pg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6pg\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_6pg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6pg\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_6pg\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_6pg\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_6pg\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_6pg\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_6pg\_ .node rect,#chatgpt-mermaid-\_r_6pg\_ .node circle,#chatgpt-mermaid-\_r_6pg\_ .node ellipse,#chatgpt-mermaid-\_r_6pg\_ .node polygon,#chatgpt-mermaid-\_r_6pg\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_6pg\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_6pg\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_6pg\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_6pg\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6pg\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}User RequestGlobal Traffic RouterNearest Suitable ApplicationRegionRegional Load BalancerApplication ServerAuthenticate and AuthorizeIdentify Data Domain andPartition KeyLoad Routing PolicyResolve Home Region / DataOwnerResolve Logical ShardOperation Type?Find Current PrimaryEvaluate Read ConsistencySuitable Replica Available?Select Eligible Replica byLatencyPrimary / Wait / Reject perPolicyExecute OperationReturn ResultWriteReadYesNo

There are a few reasons I prefer this architecture:

- Application servers remain stateless.
- Data ownership is explicit.
- Sharding can change independently of global traffic routing.
- Replicas can be selected according to consistency and locality.
- Different data domains can follow different routing policies.
- Failover changes endpoint metadata rather than requiring widespread application code changes.

### The actual routing metadata

I would maintain four logical types of information:

| Metadata            | Example                                             | Responsibility               |
| ------------------- | --------------------------------------------------- | ---------------------------- |
| Placement directory | `tenant-200 -> EU`                                  | Geographic ownership         |
| Shard map           | `EU + slot 4500 -> EU Shard 2`                      | Logical partition ownership  |
| Database topology   | `EU Shard 2 -> Primary + replicas`                  | Physical endpoints           |
| Routing policy      | `Orders: single writer, strong reads when required` | Operation-specific decisions |

These can be stored in a control-plane service and cached within each region.

Do not necessarily implement four separate services. They are four separate responsibilities that can be implemented together.

## 17. Which routing approach should you use for different problem statements?

Here is the decision table I would keep in mind during interviews.

| System         | Initial application routing   | Data routing key           | Write destination             | Read destination                  |
| -------------- | ----------------------------- | -------------------------- | ----------------------------- | --------------------------------- |
| Global SaaS    | Nearest region or tenant home | Tenant ID                  | Tenant's home-region primary  | Eligible regional replica         |
| Social network | Nearest region                | User ID                    | User's authoritative region   | Local profile replica or owner    |
| Ticket booking | Nearest region                | Event ID / inventory owner | Event's authoritative primary | Local read model for discovery    |
| E-commerce     | Nearest region                | Depends on data domain     | Product/order/inventory owner | Local replicas and read models    |
| Global chat    | Nearest suitable region       | Conversation ID            | Conversation owner            | Local message projection or owner |
| Global catalog | Nearest region                | Product ID                 | Product shard owner           | Nearest eligible replica          |
| Banking        | Approved processing region    | Account ID / legal entity  | Authoritative account owner   | Strongly consistent read path     |

One observation: application routing is typically a user/request-level decision, while data routing can happen independently for every data domain and operation within that request.

A single API request might access more than one region.

For example, a product-details page could:

- Read product information from a local catalog replica.
- Read customer information from the customer's home region.
- Read inventory projections from nearby regional indexes.
- Send an actual reservation to the inventory owner's primary.

There is no need to route the entire request to one database region merely because one of its dependencies belongs there.

## 18. Five rules that will prevent most routing mistakes

Rule 1: Never confuse application location with data ownership.

A request being processed in Asia does not mean its data must be stored in Asia.

Rule 2: Determine the logical owner before selecting a physical replica.

First establish which shard contains the data. Then select the primary or a valid replica of that shard.

Rule 3: Hashing determines partition ownership, not consistency or replica selection.

Consistent hashing and hash slots tell you where a key belongs. Your topology and consistency policy determine which physical database can serve the operation.

Rule 4: A write must reach the authoritative owner.

Don't send writes to arbitrary regional replicas. A promoted replica can become the new primary, but promotion requires controlled failover and protection against split brain.

Rule 5: Routing configuration is dynamic infrastructure.

The mapping must handle shard additions, replica health, failover, region migration, and stale configuration.

## 19. The mental model to master

Whenever you see a multi-region system design question, mentally execute this sequence:

```
                  INCOMING REQUEST
                         |
                         v
             Which application region?
                         |
                         v
                 APPLICATION SERVER
                         |
                         v
                What data is requested?
                         |
                         v
                What is its owner key?
                         |
                         v
                  Which home region?
                         |
                         v
                   Which shard?
                         |
                         v
                 What operation?
                         |
              +----------+----------+
              |                     |
              v                     v
            WRITE                  READ
              |                     |
              v                     v
       Authoritative Primary   Consistency Requirement
                                    |
                                    v
                              Eligible Replicas
                                    |
                                    v
                             Best Available
                                    |
                                    v
                              Execute Query
```

The most important distinction is between these three levels:

| Level                        | Question                                              | Mechanism                                             |
| ---------------------------- | ----------------------------------------------------- | ----------------------------------------------------- |
| 1. Geographic routing        | Where should the request be processed?                | Global load balancer / traffic router                 |
| 2. Data partition routing    | Which region and shard own the data?                  | Placement directory + shard map / hashing             |
| 3. Physical endpoint routing | Which database instance should handle this operation? | Primary/replica topology + consistency-aware selector |

Once you understand these three levels, multi-region request routing becomes much less mysterious.

My recommended default for many interview problems is:

```
Nearest suitable application region
          |
          v
Resolve data owner
          |
          v
Region placement directory
          |
          v
Consistent hashing / hash slots
          |
          v
Logical shard
          |
          v
Primary for writes
Eligible nearby replica for reads
```

Then change the ownership key, regional placement, and read-consistency rules according to the actual problem.

That is a much stronger design approach than simply saying, "We'll put a load balancer in front of three regions and replicate the databases."



