# **Designing a Scalable Distributed URL Shortener**

The most important thing to understand is that **slug generation, database sharding, server routing, caching, and multi-region replication are separate problems**. They work together, but solving one does not automatically solve the others.

We'll build the design incrementally, starting with one server and one database. At every stage, we'll identify the problem created by growth, why the previous design stops being sufficient, and what we introduce to solve it.

I'll use this as our working example:

- A user submits a long URL.
- The system returns a short URL such as `https://sho.rt/aB91xZ`.
- Someone opens that short URL.
- The system finds the original URL and redirects the visitor.
- Slugs must contain at most **8 characters**.
- The system must support high read and write throughput, database sharding, caching, and multiple regions.

# **1. First, understand the actual problem**

A URL shortener looks simple because its main operation is just a lookup:

\\[ \text{slug} \rightarrow \text{original URL} \\]

For example:

| Slug     | Original URL                                       |
| -------- | -------------------------------------------------- |
| `aB91xZ` | `https://example.com/products/123?campaign=summer` |
| `xY72pQ` | `https://example.com/articles/distributed-systems` |
| `kL53mN` | `https://example.com/users/profile/456`            |

We need to support two primary operations.

**Create a short URL**

```
POST /api/shorten

{
  "longUrl": "https://example.com/some/very/long/path"
}
```

Response:

```
{
  "shortUrl": "https://sho.rt/aB91xZ"
}
```

**Redirect using a short URL**

```
GET /aB91xZ
```

Response:

```
HTTP/1.1 302 Found
Location: https://example.com/some/very/long/path
```

Notice the important difference between these operations:

- Creating a URL is a **write**.
- Opening a short URL is a **read**.

In a typical URL shortener, reads greatly outnumber writes. A link may be created once but opened thousands or millions of times.

That means we should optimize the architecture primarily for fast, highly available reads, without compromising the correctness of writes.

## **1.1 What are we storing?**

We don't need to store the long URL in every cache or routing component. Our essential database record can be:

```
URL_MAPPING
-----------
slug             PRIMARY KEY
original_url
created_at
expires_at
status
```

For example:

```
slug       = aB91xZ
original_url = https://example.com/products/123
created_at = ...
expires_at = ...
status     = ACTIVE
```

The `slug` is the lookup key.

This is important because **the slug is the natural partitioning and lookup key for redirect requests**. Every redirect begins with a slug, not with an internal database ID.

We will use that fact when designing sharding.

# **2. Understand the 8-character slug constraint**

Before designing the database, let's understand how many unique short URLs we can create.

Assume our slug can use:

- `a-z`: 26 characters
- `A-Z`: 26 characters
- `0-9`: 10 characters

That gives us a Base62 alphabet:

\\[ 26 + 26 + 10 = 62 \\]

If a slug can contain at most 8 characters, the number of possible slugs of exactly 8 characters is:

\\[ 62^8 = 218,340,105,584,896 \\]

That's approximately **218 trillion combinations**.

If we also allow shorter slugs, the total number of non-empty slugs of length 1 through 8 is:

\\[ 62 + 62^2 + \dots + 62^8 \\]

So the namespace is large, but not unlimited.

### **Why this matters**

The slug is a globally unique identifier from the application's point of view.

If two users receive the same slug for different URLs, the system cannot reliably redirect either user. Therefore, slug creation must guarantee uniqueness, even when multiple application servers are creating URLs concurrently.

We must also ensure the slug generator never produces more than 8 characters.

There are several ways to generate slugs. We'll evaluate them later, because the generation strategy affects the way we can route requests to database shards.

For now, remember:

> We need a globally unique slug, but we do not necessarily need a globally sequential slug.

That distinction gives us flexibility.

# **3. How should we generate the slug?**

Now we have a namespace of approximately 218 trillion possible 8-character Base62 slugs.

The next question is:

**When multiple application servers receive requests at the same time, how can they generate unique slugs without coordinating with each other for every request?**

Let's consider three approaches.

## **Approach A: Use a global auto-increment ID**

Imagine our database generates IDs:

```
1
2
3
4
5
...
```

We convert each number into Base62.

For example:

```
ID 100  -> 1c
ID 1000 -> G8
ID 1001 -> G9
```

The actual encoding depends on the Base62 implementation.

The advantage is that IDs are inherently unique when allocated by one authoritative sequence generator.

However, there are problems:

1. A single global sequence generator can become a bottleneck.
2. Multiple regions need a coordinated ID-allocation strategy.
3. Sequential slugs are predictable. Someone could enumerate them.
4. A normal 64-bit ID cannot fit into 8 Base62 characters.

The last point is important.

\\[ 62^8 \approx 2.18 \times 10^{14} \\]

A 47-bit integer fits within this capacity:

\\[ 2^{47} \approx 1.41 \times 10^{14} \\]

But a full 64-bit ID needs up to 11 Base62 characters.

So we could build a custom compact ID generator with a maximum value that fits in the 8-character namespace. It could allocate blocks of IDs to different application servers or use region-specific ID ranges.

For example:

```
Region A: IDs 0       - 10 billion
Region B: IDs 10B     - 20 billion
Region C: IDs 20B     - 30 billion
```

Each region gets a disjoint range.

This prevents two regions from generating the same ID, but range allocation and namespace management introduce additional complexity.

**When is this useful?**

When we specifically want ID-based ordering, sequential allocation, or a predictable method of deriving storage partitions from IDs.

It is not mandatory for a URL shortener.

## **Approach B: Generate a random Base62 slug**

This is much simpler.

Whenever an application server receives a creation request, it generates a random 8-character slug.

For example:

```
aB91xZ73
pQ28kLm5
xY72pQ10
```

The application server doesn't need to ask a central service for a unique ID.

Different servers generate random candidates independently.

But there is one problem:

**What happens if two servers generate the same slug?**

Suppose:

```
Server A generates: aB91xZ73
Server B generates: aB91xZ73
```

Both servers attempt to store different long URLs against the same slug.

We cannot allow both inserts to succeed.

We solve this through a database uniqueness constraint.

```
CREATE TABLE url_mapping (
    slug VARCHAR(8) PRIMARY KEY,
    original_url TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL
);
```

The creation flow becomes:

1. Generate a random slug.
2. Determine its database shard.
3. Attempt to insert the mapping.
4. If insertion succeeds, return the slug.
5. If the primary-key constraint reports a duplicate, generate another slug and retry.

Pseudocode:

```
while (true) {
    String slug = generateRandomBase62Slug(8);

    int bucket = getBucket(slug);
    DatabaseShard shard = topology.getShard(bucket);

    try {
        shard.insert(slug, originalUrl);
        return slug;
    } catch (DuplicateKeyException e) {
        // Extremely unlikely collision.
        // Generate another candidate and retry.
    }
}
```

Notice that we are relying on the database's atomic uniqueness constraint, not a separate check-then-insert operation.

A separate check would be unsafe:

```
Server A: Does slug exist? No.
Server B: Does slug exist? No.

Server A: Insert slug.
Server B: Insert slug.
```

Both could observe that the slug doesn't exist. Only an atomic uniqueness constraint protects us from that race.

### **Is random generation practical?**

Yes, because the namespace is large.

The probability that a newly generated random slug collides with an existing slug is approximately:

\\[ P(\text{collision on next attempt}) = \frac{N}{62^8} \\]

where \\(N\\) is the number of existing slugs.

For example, with 1 billion existing slugs:

\\[ P \approx \frac{10^9}{218 \times 10^{12}} \\]

\\[ P \approx 0.00000458 \\]

That's approximately 0.000458%.

The probability grows as the namespace fills, so we should monitor collision retries and namespace utilization.

Use a cryptographically secure random generator rather than a predictable random-number generator. Also, don't treat possession of a random slug as an authorization mechanism for sensitive content.

## **Approach C: Generate a unique ID and obfuscate it**

Another approach is:

```
Unique ID -> Obfuscation -> Base62 encoding -> Slug
```

For example:

```
Generated ID: 123456
       |
       v
Obfuscation/permutation
       |
       v
Encoded Base62 value
       |
       v
Short slug
```

A bijective permutation, such as a suitably constructed Feistel permutation over a fixed-size domain, can make sequential IDs appear less predictable without introducing collisions.

This is useful when we want collision-free ID generation but don't want users to see sequential IDs.

We still need a distributed ID allocation mechanism, though.

## **Which approach should we choose?**

For our design, let's choose:

**Random 8-character Base62 slugs + database uniqueness constraint + deterministic database-shard routing.**

Why?

- No central ID generator is required.
- Application servers can generate slugs independently.
- We don't need globally coordinated counters.
- Random slugs are less predictable than sequential IDs.
- The slug itself is enough to determine where its record belongs.

This last point is the bridge to database sharding.

# **4. Why do we need database sharding?**

Let's start with one database.

```
              Application Servers
                       |
                       |
                       v
                 One Database
```

Every mapping is stored in the same database.

Initially, this is perfectly reasonable.

But as traffic grows, several problems appear:

- The database needs to store more records.
- The database needs to process more reads.
- The database needs to process more writes.
- Its indexes become larger.
- Its storage and I/O requirements increase.
- Eventually, adding CPU, memory, or storage to one machine is not enough or becomes too expensive.

We can introduce read replicas to distribute read traffic, but replicas do not solve the entire problem.

For example:

```
                  Application
                       |
               -----------------
               |               |
               v               v
           Primary DB       Read Replica
            (writes)          (reads)
```

This helps with reads, but all writes still go through the primary.

Also, the primary database still stores the entire dataset.

So we introduce **sharding**.

## **4.1 What is sharding?**

Instead of storing every URL mapping in one database, we divide the data into multiple database partitions.

For example:

```
             Application Servers
                      |
              Shard Routing Layer
                      |
          ------------+------------
          |           |           |
          v           v           v
       Shard 1     Shard 2     Shard 3
```

Each shard stores only a subset of the URL mappings.

For example:

```
Shard 1
-------
aB91xZ73
kL53mN22
...

Shard 2
-------
pQ28kLm5
zT18nR42
...

Shard 3
-------
xY72pQ10
mN82vB31
...
```

Now the overall capacity can grow by adding more shards.

But a new problem appears.

**When a redirect request arrives with a slug, how does the application know which database contains that slug?**

We need a routing strategy.

# **5. How do we identify the correct database shard?**

There are multiple ways to do this.

Let's understand them before choosing one.

## **Approach A: Use a range-based shard key**

Suppose we use numerical IDs.

We could allocate ID ranges to different shards:

```
Shard 1: ID 1       - 1,000,000
Shard 2: ID 1,000,001 - 2,000,000
Shard 3: ID 2,000,001 - 3,000,000
```

To find the shard:

```
if (id <= 1_000_000) {
    return shard1;
} else if (id <= 2_000_000) {
    return shard2;
} else {
    return shard3;
}
```

This is range-based sharding.

### **Advantages**

It is easy to understand and route.

It can also be useful for queries involving ID ranges.

### **Problems**

Imagine all new IDs are increasing:

```
ID 1000001
ID 1000002
ID 1000003
ID 1000004
```

All new writes go to Shard 2.

When we allocate a new range, writes move to the next shard. This can create uneven distribution and a hot shard.

Also, if we want to move records between ranges, routing metadata must be updated carefully.

For our URL shortener, we need a distribution strategy that spreads traffic across database shards.

## **Approach B: Use modulo hashing**

Suppose we have four physical shards.

For every slug:

\\[ shard = hash(slug) \bmod 4 \\]

For example:

```
hash(aB91xZ73) % 4 = 0 -> Shard 0
hash(pQ28kLm5) % 4 = 2 -> Shard 2
hash(xY72pQ10) % 4 = 1 -> Shard 1
hash(kL53mN22) % 4 = 3 -> Shard 3
```

The important property:

**The same slug always produces the same shard, as long as the hashing algorithm and shard count remain unchanged.**

Therefore, every application server can independently calculate the correct shard.

No central lookup is needed.

### **This is already a good solution. But it has a scaling problem.**

Suppose we initially have four shards:

\\[ hash(slug) \bmod 4 \\]

Later, we add four more shards.

Now the formula becomes:

\\[ hash(slug) \bmod 8 \\]

Consider a slug:

```
hash(slug) = 17
```

Initially:

\\[ 17 \bmod 4 = 1 \\]

It belongs to Shard 1.

After increasing the shard count:

\\[ 17 \bmod 8 = 1 \\]

This one happens to remain on Shard 1.

But consider:

```
hash(slug) = 18
```

Initially:

\\[ 18 \bmod 4 = 2 \\]

After increasing the shard count:

\\[ 18 \bmod 8 = 2 \\]

Again, unchanged.

And:

```
hash(slug) = 19
```

Initially it belongs to Shard 3. After adding shards, it belongs to Shard 3.

These examples happen to remain unchanged because we doubled the shard count and selected small hash values. But in general, many hash values will map to a different shard.

For example:

```
hash(slug) = 5

5 % 4 = 1
5 % 8 = 5
```

The slug moves from Shard 1 to Shard 5.

In fact, when increasing from \\(N\\) shards to \\(2N\\), approximately half the keys change their assigned shard under ordinary modulo hashing.

Imagine we have billions of URL mappings.

Changing the shard count would require moving a massive amount of data.

We need a better approach.

## **Approach C: Introduce logical buckets (recommended)**

This is the design I recommend for our URL shortener.

The idea is simple:

**Don't directly map each slug to a physical database shard. First map it to a stable logical bucket. Then map that bucket to a physical database shard.**

Let's understand why.

### **Step 1: Create a fixed number of logical buckets**

Let's say we create 4,096 logical buckets.

```
Bucket 0
Bucket 1
Bucket 2
Bucket 3
...
Bucket 4095
```

These buckets are logical partitions. They are not individual database servers.

They are simply stable groups into which slugs are distributed.

### **Step 2: Hash the slug to identify its bucket**

We calculate:

\\[ bucket = hash(slug) \bmod 4096 \\]

For example:

```
hash(aB91xZ73) % 4096 = 105
hash(pQ28kLm5) % 4096 = 3021
hash(xY72pQ10) % 4096 = 821
hash(kL53mN22) % 4096 = 105
```

So:

```
aB91xZ73 -> Bucket 105
pQ28kLm5 -> Bucket 3021
xY72pQ10 -> Bucket 821
kL53mN22 -> Bucket 105
```

Notice that two different slugs can belong to the same bucket. That is perfectly fine.

A bucket is not a slug. It is just a logical grouping.

### **Step 3: Assign buckets to physical database shards**

Now we maintain a mapping:

```
Bucket -> Physical Shard
```

For example:

```
Buckets 0   - 1023 -> Shard A
Buckets 1024 - 2047 -> Shard B
Buckets 2048 - 3071 -> Shard C
Buckets 3072 - 4095 -> Shard D
```

Our complete mapping looks like this:

```

                 Slug
                   |
                   v
          hash(slug) % 4096
                   |
                   v
             Logical Bucket
                   |
          +--------+--------+
          |        |        |
          v        v        v
       Bucket 105  Bucket 821  Bucket 3021
          |        |        |
          v        v        v
        Shard A  Shard A   Shard C

```

For our earlier examples:

```
aB91xZ73 -> Bucket 105  -> Shard A
pQ28kLm5 -> Bucket 3021 -> Shard C
xY72pQ10 -> Bucket 821  -> Shard A
kL53mN22 -> Bucket 105  -> Shard A
```

The application can calculate the bucket independently.

It then needs the bucket-to-shard mapping.

### **Step 4: What happens when we add more database shards?**

Suppose Shard A is becoming overloaded.

We add Shard E.

Previously:

```
Buckets 0 - 1023 -> Shard A
```

We can move some logical buckets:

```
Buckets 0 - 511   -> Shard A
Buckets 512 - 1023 -> Shard E
```

The bucket IDs themselves do not change.

For example:

```
aB91xZ73 -> Bucket 105 -> Shard A
```

It still produces Bucket 105.

But the physical ownership of Bucket 105 can be changed when we decide to move it.

**This is the key advantage.**

We don't need to change the hashing formula when adding physical database shards. We only change the bucket-to-shard assignment.

The same bucket can be moved to a different physical database while the logical identity remains stable.

### **Why 4,096 buckets?**

It's just an example, not a fixed requirement.

We choose enough logical buckets to allow granular balancing and migration.

For example, having 4,096 buckets gives us many smaller units to distribute across physical shards.

The number should be chosen based on expected data volume, shard capacity, and the operational cost of managing partitions.

# **6. Does the application maintain a shard map, or does it query a central service?**

This is one of the most important architectural questions.

Let's look at the two possibilities.

## **Option A: Query a central routing service for every request**

Imagine:

```
Request
   |
   v
Application Server
   |
   v
Shard Directory Service
   |
   v
Returns Shard C
   |
   v
Database Shard C
```

The directory service stores:

```
Bucket 105  -> Shard A
Bucket 821  -> Shard A
Bucket 3021 -> Shard C
```

Every application server asks this service where to route a request.

Why is this a problem?

Our URL shortener is read-heavy. Imagine thousands or millions of redirect requests per second.

Now every request depends on another service just to discover which database it should query.

This creates:

- Additional network latency.
- A possible throughput bottleneck.
- A dependency that can fail.
- An unnecessary extra network hop on the hot path.

We have distributed our database but introduced another central service that every request depends on.

That's not what we want.

## **Option B: Application servers maintain a local shard-routing map**

Instead, we distribute the routing configuration to every application server.

For example, an application server maintains:

```
Bucket -> Shard Group

0    -> Shard A
1    -> Shard A
2    -> Shard B
...
105  -> Shard A
...
821  -> Shard A
...
3021 -> Shard C
...
4095 -> Shard D
```

When a request arrives:

1. The application extracts the slug.
2. It calculates the bucket.
3. It looks up the bucket in its local routing map.
4. It obtains the database shard.
5. It communicates directly with that shard's database endpoint.

No central service is contacted for each request.

### **But where does the local map come from?**

We still need a central place to manage configuration.

For that, we introduce a **shard topology/configuration service**.

Its responsibility is to maintain authoritative routing metadata.

Conceptually:

```
               Shard Configuration Store
                         |
                         |
                 Topology Manager
                         |
              Distributes new versions
                         |
           +-------------+-------------+
           |             |             |
           v             v             v
       App Server 1  App Server 2  App Server 3
       Local Map     Local Map     Local Map
```

The topology manager distributes routing updates through a configuration mechanism, such as a control-plane API, a configuration store with watches, or a push-based configuration service.

Application servers refresh their local maps when the topology changes.

We can also periodically refresh as a safety mechanism.

The important point is:

**The configuration service is used to distribute routing metadata, not to answer every redirect request.**

### **What if an application server has an outdated map?**

This is important because routing metadata is distributed.

Suppose:

```
Old map:
Bucket 105 -> Shard A

New map:
Bucket 105 -> Shard E
```

Some application servers may still have the old map.

During a migration, we must make stale routing safe.

We can use:

- Versioned topology configurations.
- A migration state for buckets.
- Forwarding from the old shard to the new owner during transition.
- Write fencing during cutover.
- A temporary dual-read strategy, where necessary.

We will discuss migration later.

# **7. Shard routing and database server discovery are two different things**

We need to be very clear about this distinction.

When we say:

```
Bucket 105 -> Shard A
```

What exactly is Shard A?

Is it one physical database server?

Not necessarily.

A shard is a logical data partition. It may be implemented by a database group containing multiple servers.

For example:

```
Shard A
  |
  +---- Primary DB
  |
  +---- Replica 1
  |
  +---- Replica 2
```

Now we have two separate routing decisions.

**Decision 1: Which shard contains this slug?**

```
Slug -> Bucket -> Shard Group
```

**Decision 2: Which database server inside that shard should handle the request?**

```
Shard Group -> Primary or Replica
```

We can maintain a shard topology like this:

```
Bucket 105
    |
    v
Shard A
    |
    +---- Primary: db-a-primary
    |
    +---- Replica 1: db-a-replica-1
    |
    +---- Replica 2: db-a-replica-2
```

The application can use:

- The primary for writes.
- A replica for ordinary reads.
- The primary for reads that require strong read-after-write consistency.
- A healthy replica selected through load balancing for other reads.

The database group can expose stable service endpoints rather than individual machine IPs.

For example:

```
shard-a-primary.database.internal
shard-a-read.database.internal
```

A database proxy or service-discovery system can resolve these to the current healthy database servers.

This gives us three distinct pieces:

| Component                          | Responsibility                                                 |
| ---------------------------------- | -------------------------------------------------------------- |
| Hash function                      | Converts slug into a logical bucket                            |
| Shard topology                     | Maps a logical bucket to a database shard group                |
| Database service discovery / proxy | Finds the current primary or replica servers inside that group |

The application does not need to know every physical database IP permanently.

It needs a stable way to identify the shard group and connect to its current endpoints.

# **8. Complete sharding implementation**

Let's now combine these concepts.

We use:

- Random 8-character Base62 slugs.
- 4,096 logical buckets.
- A versioned bucket-to-shard topology.
- A primary and replicas within each database shard.

The routing function can look like this:

```
public class ShardRouter {

    private final ShardTopology topology;

    public ShardRouter(ShardTopology topology) {
        this.topology = topology;
    }

    public DatabaseShard getShard(String slug) {

        // Step 1: Identify the logical bucket.
        int bucket = Math.floorMod(
            stableHash(slug),
            4096
        );

        // Step 2: Find the physical shard group.
        return topology.getShardForBucket(bucket);
    }

    private int stableHash(String slug) {
        // Use a deterministic, consistently configured hash.
        return slug.hashCode();
    }
}
```

This is illustrative code. For production, use a deliberately selected, stable hash algorithm with a consistent implementation across languages and deployments. Don't casually rely on a runtime-dependent hash implementation.

The topology might expose:

```
DatabaseShard shard =
    shardRouter.getShard("aB91xZ73");
```

Then:

```
shard.getPrimary().insert(...);
```

Or:

```
shard.getReadReplica().findBySlug(...);
```

Our request-serving logic is now independent of how physical database servers are deployed.

### **What if a database shard has many records?**

That's fine.

The application routes to the shard, and the database uses its local index to find the row.

The shard should have a primary-key index on `slug`.

The application does not need to search every shard.

It calculates one destination and queries that shard.

This is what makes sharding useful: we distribute both storage and request load, while preserving efficient key-based lookups.

# **9. Now introduce caching**

We've solved database sharding.

But let's think about the actual redirect traffic.

Suppose someone creates this mapping:

```
aB91xZ73 -> https://example.com/products/123
```

And then 10 million people open the same short URL.

Without caching, what happens?

```
10 million requests
        |
        v
Application Servers
        |
        v
Shard Router
        |
        v
Shard A
        |
        v
Database lookup
```

Even though we have multiple database shards, every request still reaches a database.

This is wasteful.

There is no reason to repeatedly retrieve the same mapping from the database when the result is relatively stable.

So we introduce caching.

## **9.1 What should we cache?**

The most frequently needed information is:

```
slug -> original URL
```

For example:

```
Cache:

aB91xZ73 -> https://example.com/products/123
pQ28kLm5 -> https://example.com/articles/456
xY72pQ10 -> https://example.com/users/789
```

When the same slug is requested again, we return the cached original URL instead of querying the database.

But where should the cache be placed?

## **9.2 Introduce a regional distributed cache**

Initially, imagine putting a cache inside every application server.

```
Application Server 1 -> Local Cache 1
Application Server 2 -> Local Cache 2
Application Server 3 -> Local Cache 3
```

There is a problem.

Suppose Server 1 receives `aB91xZ73` and caches it.

Later, Server 2 receives the same slug.

Server 2 doesn't know what Server 1 cached.

It must query the database and populate its own cache.

This creates duplicate cache entries and makes it difficult to reuse cached values across application instances.

We introduce a shared distributed cache, commonly Redis.

```
             Application Servers
               |       |       |
               +-------+-------+
                       |
                       v
                Redis Cluster
                       |
                       |
                       v
                  DB Shards
```

Now all application servers within a region can access a shared cache.

### **Cache-aside pattern**

We use a pattern called cache-aside.

The application explicitly checks the cache before accessing the database.

The read flow is:

```
                Request: /aB91xZ73
                         |
                         v
                  Application
                         |
                         v
                    Redis GET
                         |
                +--------+--------+
                |                 |
             Cache Hit         Cache Miss
                |                 |
                v                 v
          Get original URL    Calculate bucket
                |                 |
                |                 v
                |            Find DB shard
                |                 |
                |                 v
                |            Query database
                |                 |
                |                 v
                |            Get original URL
                |                 |
                |                 v
                |            Store in Redis
                |                 |
                +--------+--------+
                         |
                         v
                   HTTP Redirect
```

In code:

```
public String resolve(String slug) {

    // 1. Check cache.
    String cachedUrl = redis.get(slug);

    if (cachedUrl != null) {
        return cachedUrl;
    }

    // 2. Calculate the database shard.
    DatabaseShard shard = shardRouter.getShard(slug);

    // 3. Fetch from database.
    String originalUrl = shard.findBySlug(slug);

    if (originalUrl == null) {
        throw new ShortUrlNotFoundException();
    }

    // 4. Populate cache.
    redis.set(
        slug,
        originalUrl,
        Duration.ofHours(1)
    );

    return originalUrl;
}
```

This is the basic cache-aside implementation.

Notice something important:

**We calculate the database shard only after a cache miss.**

If the URL is in Redis, we don't need to identify its database shard at all.

That saves both database traffic and routing work.

## **9.3 Does Redis need the same sharding map as the database?**

No.

This is an important distinction.

We have two separate partitioning systems:

**Database sharding**

```
Slug -> Bucket -> Database Shard
```

**Redis sharding**

```
Cache Key -> Redis Cluster Hash Slot -> Redis Node
```

A Redis Cluster can independently distribute keys across its own nodes.

For example:

```
              Redis Cluster
                   |
          +--------+--------+
          |        |        |
          v        v        v
        Node 1  Node 2   Node 3
```

All application servers use the Redis cluster's client routing mechanism.

The Redis cluster doesn't have to use the same physical partitioning as the database.

For instance:

```
Database:
aB91xZ73 -> Shard A

Redis:
aB91xZ73 -> Node 2
```

That's perfectly fine.

Redis stores a copy of the result; it doesn't need to know which database shard originally owns it.

We could deliberately colocate cache data with database shards in a specialized architecture, but that is not necessary for our design.

## **9.4 Should we also have an application-local cache?**

Yes, potentially.

There is another optimization we can introduce.

Even Redis involves a network call.

For very frequently accessed URLs, we might want to avoid repeatedly contacting Redis.

We can introduce an L1 in-memory cache inside each application instance.

Then we have:

```
                 Application Server
                         |
                         v
                  L1 Local Cache
                         |
                  Cache Miss?
                         |
                         v
                   L2 Redis
                         |
                  Cache Miss?
                         |
                         v
                    DB Shards
```

Let's understand the hierarchy.

| Layer          | Location                   | Purpose                                   |
| -------------- | -------------------------- | ----------------------------------------- |
| CDN/edge cache | Near the user              | Avoid requests reaching our application   |
| L1 cache       | Inside application process | Very fast local lookup                    |
| L2 cache       | Regional Redis cluster     | Shared cache across application instances |
| Database       | Sharded database clusters  | Durable source of truth                   |

We don't necessarily need every layer initially.

A sensible evolution is:

1. Start with regional Redis.
2. Add L1 caching for exceptionally hot keys if Redis traffic becomes significant.
3. Add CDN caching where the redirect semantics and invalidation requirements permit it.

### **A subtle problem with L1 caching**

Suppose the URL is changed.

Redis is invalidated, but an application server still has an old value in its local memory.

That server continues returning the old URL.

This is why L1 caching needs a deliberately short TTL, an invalidation mechanism, versioning, or some combination of these.

The more caching layers we introduce, the more complicated invalidation becomes.

Don't add a cache layer just because it is possible. Add it when the performance benefit justifies the operational complexity.

# **10. Cache expiration and invalidation**

Now consider this scenario.

A user creates:

```
aB91xZ73 -> https://example.com/old-page
```

The mapping is cached.

Later, an administrator changes the destination:

```
aB91xZ73 -> https://example.com/new-page
```

What happens if we update the database but don't update the cache?

Redis continues returning:

```
aB91xZ73 -> https://example.com/old-page
```

The user is redirected to the wrong URL.

We need a cache invalidation strategy.

## **10.1 Cache expiration using TTL**

Every cache entry should have a time-to-live.

For example:

```
Key: aB91xZ73
Value: https://example.com/old-page
TTL: 1 hour
```

After an hour, the cache entry expires.

The next request fetches the latest mapping from the database and repopulates the cache.

TTL is a safety net, but it doesn't provide immediate consistency.

If we change the destination five minutes after caching, the old value may remain for another 55 minutes.

## **10.2 Invalidate the cache after database updates**

A common approach is:

1. Update the database.
2. Commit the transaction.
3. Delete the corresponding Redis cache entry.

```
Update Request
      |
      v
Update Database
      |
      v
Commit Transaction
      |
      v
Delete Cache Entry
```

The next read will miss Redis, fetch the updated database value, and repopulate the cache.

This is suitable for many systems.

But now think about multiple regions.

We will need to invalidate caches in other regions too.

And there is another subtle problem: what happens if the database transaction commits successfully, but the application crashes before publishing the invalidation event?

The database contains the new value, but the cache may keep the old value.

## **10.3 Use the Transactional Outbox pattern**

We can make database changes and invalidation-event creation reliable by storing an outbox event in the same database transaction.

For example:

```
Database Transaction
    |
    +-- Update URL_MAPPING
    |
    +-- Insert OUTBOX_EVENT
    |
    +-- Commit
```

The transaction commits both changes together.

A background publisher reads the outbox and publishes events to a message broker such as Kafka.

```
                Database
                    |
               Outbox Table
                    |
                    v
               Outbox Publisher
                    |
                    v
                Message Broker
                    |
           +--------+--------+
           |                 |
           v                 v
      Region A           Region B
      Cache Consumer     Cache Consumer
           |                 |
           v                 v
       Redis A             Redis B
```

An event might look like:

```
{
  "eventType": "URL_UPDATED",
  "slug": "aB91xZ73",
  "version": 2
}
```

Each regional consumer invalidates or updates its corresponding cache entry.

The message publisher and consumers should be idempotent, because events can be delivered more than once.

For stronger protection against a delayed old read repopulating an invalidated cache, attach a version to each mapping and use version-aware cache updates.

For example:

```
Database mapping:
slug = aB91xZ73
version = 2
```

A cache update based on version 1 should not overwrite a value already stored at version 2.

### **What if the URL is never supposed to change?**

That makes our lives easier.

For immutable URL mappings:

- Store the mapping durably.
- Cache it with a TTL.
- Populate regional caches on demand.
- When the link expires or is disabled, publish an invalidation or tombstone event.

We don't need a complex update workflow for every link.

## **10.4 What about caching a slug that doesn't exist?**

Suppose someone requests:

```
GET /notARealSlug
```

Our application checks Redis, gets a miss, then queries the database and finds nothing.

Repeated requests for this nonexistent slug would repeatedly hit the database.

We can introduce **negative caching**.

```
notARealSlug -> NOT_FOUND
TTL: 30 seconds
```

Now repeated requests can be answered without querying the database every time.

However, use short negative-cache TTLs and consider the creation race:

```
Time 1: Slug does not exist.
Time 2: A request caches NOT_FOUND.
Time 3: Another request creates that slug.
Time 4: A user requests it, but the negative cache still says NOT_FOUND.
```

A negative cache could hide a newly created URL temporarily.

Cache invalidation after creation, especially for a slug that could have been negatively cached, or a short negative TTL helps manage this.

In our random-slug design, that particular slug is unlikely to be requested before creation, but this can still happen through collisions, guessing, or malicious requests.

# **11. What happens with extremely popular links?**

Consider:

```
aB91xZ73
```

A celebrity shares the link. Suddenly millions of people open it.

This is a classic hot-key problem.

Even if we have 100 database shards, all requests for this slug go to the same logical bucket and the same owning shard on a cache miss.

But with caching, most requests should not reach the database.

Our strategy is:

```
Request
   |
   v
Regional Cache
   |
   +-- Cache Hit -> Return URL
   |
   +-- Cache Miss -> DB shard
```

For extremely popular links, several additional techniques help.

**Local cache:** Each application instance can keep an in-memory copy of hot links.

**CDN/edge cache:** For eligible redirects, the CDN can cache the redirect response near users.

**Request coalescing:** When a cache entry expires, 1,000 concurrent requests may all try to fetch the same database record. We can make one request perform the fetch while the others wait briefly for its result. This is often called single-flight or request collapsing.

**Probabilistic early refresh:** Refresh a popular entry slightly before it expires so that expiration doesn't produce a sudden database spike.

**Replica scaling:** If many distinct cache misses reach the database, add read capacity through replicas or more shards.

One important observation:

> Database sharding distributes different keys across different shards. Caching reduces repeated requests for the same key. These solve different problems.

Caching a hot URL is often much more effective than trying to distribute that single URL across many database shards.

# **12. Now comes the interesting part: Multi-region architecture**

So far, all our application servers, Redis instances, and database shards can be located in one region.

For example:

```
                 INDIA REGION
             (Everything is here)

             Application Servers
                     |
                     v
                Redis Cluster
                     |
                     v
              Database Shards
```

This works well as long as users are relatively close to the region.

But suppose our users are distributed globally:

```
India
USA
Europe
Singapore
Australia
```

A user in the USA sending every redirect request to an Indian data center may experience unnecessary network latency.

We want to serve users from nearby regions.

So we introduce multiple regions.

## **12.1 First, replicate the application infrastructure**

We deploy our application and cache in multiple regions.

```
                 GLOBAL USERS
                       |
                 DNS / Anycast
                       |
          +------------+------------+
          |            |            |
          v            v            v
      India         USA          Europe
      Region        Region       Region
        |             |            |
        v             v            v
      App           App          App
        |             |            |
        v             v            v
      Redis         Redis        Redis
        |             |            |
        v             v            v
      Database      Database     Database
```

The idea is that a user should normally be served by a nearby region.

For example:

```
User in India -> India Region
User in USA   -> USA Region
User in Europe -> Europe Region
```

Now we have a new question:

**How does a USA application server find a URL mapping that was created in India?**

This is where we need to distinguish between two things:

1. Cache locality.
2. Durable database-data locality.

They are not the same.

A cache in the USA does not automatically contain data created in India. And a database replica in the USA does not automatically contain every record stored in India unless we replicate that data.

Let's solve these problems separately.

# **13. How should we distribute database shards across regions?**

There are two broad designs.

## **Design A: Every region has a full replica of the database**

Suppose we have four logical database shards.

```
                  GLOBAL DATABASE
                         |
             +-----------+-----------+
             |           |           |
             v           v           v
          India         USA        Europe
          Region        Region     Region
             |           |           |
        All Shards   All Shards  All Shards
        (Replicas)   (Replicas)  (Replicas)
```

Every region contains the data for all shards.

A mapping created in India gets replicated to the USA and Europe.

The benefit is straightforward:

A user in the USA can read the mapping from the USA database replica instead of crossing the world.

This is attractive for read-heavy workloads.

### **But what about writes?**

We should not allow every region to independently update the same mapping without a conflict-resolution strategy.

A simple and safe choice is:

**Every database shard has one authoritative write primary. Other regions maintain read replicas.**

For example:

```
Shard A
-------
India Primary
USA Replica
Europe Replica

Shard B
-------
USA Primary
India Replica
Europe Replica

Shard C
-------
Europe Primary
India Replica
USA Replica
```

Each shard has a home region.

Writes for that shard go to its home-region primary.

Reads can use local replicas.

This is a single-writer-per-shard design, not unrestricted active-active writing.

### **How do we know which region owns a shard?**

Remember our earlier mapping:

```
Slug -> Bucket -> Physical Shard
```

We extend it:

```
Slug -> Bucket -> Home Region -> Shard
```

For example:

```
Bucket 105  -> India -> Shard A
Bucket 821  -> USA   -> Shard B
Bucket 3021 -> Europe -> Shard C
```

Our routing topology now contains region ownership too.

A versioned topology could conceptually look like:

```
Bucket | Home Region | Shard
-------|-------------|------
105    | India       | A
106    | USA         | B
107    | Europe      | C
108    | India       | A
109    | USA         | B
...
```

The exact assignment should be based on capacity and traffic, not necessarily an equal division of bucket IDs.

All regions receive the same authoritative bucket-placement configuration.

This means that an application server in any region can determine the home region and shard for any slug.

**The application still doesn't need to query a central routing service for every request.**

It uses the local topology map.

## **13.1 What if the global dataset is too large to replicate everywhere?**

Then we have another option.

We can keep the authoritative database data in its home region and replicate only where necessary.

For example:

```
Bucket 105 -> India
Bucket 821 -> USA
Bucket 3021 -> Europe
```

Instead of storing all records everywhere:

- India stores its owned buckets.
- USA stores its owned buckets.
- Europe stores its owned buckets.

When a request arrives in a region that doesn't own the data, we can route the request to the home region.

For example:

```
User in USA
     |
     v
USA Application
     |
     v
Bucket calculation
     |
     v
Home region = India
     |
     v
India Database Shard
```

This saves replication and storage costs but introduces cross-region read latency.

A middle ground is to maintain replicas in selected regions based on traffic and availability requirements.

### **Which approach should we use?**

For a globally popular URL shortener, I would generally prefer:

- A single authoritative write owner per bucket.
- Regional read replicas where global read latency justifies their cost.
- Regional caches in every region.
- Home-region fallback when a local replica cannot yet satisfy a read.

This gives us predictable writes and geographically distributed reads.

# **14. The most important multi-region question: What happens to caches?**

Let's use an example.

A user in India creates:

```
aB91xZ73 -> https://example.com/products/123
```

The mapping is stored in the database.

Later, a user in the USA opens:

```
https://sho.rt/aB91xZ73
```

The request reaches the USA region.

But the India Redis cache contains this mapping. The USA Redis cache doesn't.

What should happen?

This is exactly the cache locality problem.

## **14.1 Should we update all regional caches whenever a URL is created?**

We could do that:

```
Create URL in India
       |
       v
India Database
       |
       v
Publish Event
       |
       +----------+----------+
       |          |          |
       v          v          v
    Redis       Redis      Redis
    India       USA        Europe
```

Every new URL mapping would be pushed into every regional Redis cache.

At first glance, this sounds reasonable.

But think about the scale.

Suppose:

- We create 10 million URLs per day.
- We have 10 regions.
- Every URL is inserted into every regional Redis cluster.

That's 100 million regional cache insertions per day, before accounting for retries and other cache traffic.

Most of these URLs may never be accessed from most of those regions.

We would be storing a huge amount of unnecessary data in caches.

Remember:

**A cache is not our durable source of truth. It is a temporary, performance-oriented copy of frequently accessed data.**

We should not treat every cache as a replica that must contain the complete dataset.

## **14.2 Recommended approach: Regional cache-aside**

Give every region its own Redis cluster.

```
                 GLOBAL USERS
                       |
          +------------+------------+
          |            |            |
          v            v            v
        INDIA         USA         EUROPE
          |            |            |
          v            v            v
       Redis A      Redis B      Redis C
```

Each Redis cluster gets populated when a user in that region actually requests a URL.

For example:

**Step 1: A URL is created in India.**

```
India Database:

aB91xZ73 -> https://example.com/products/123
```

We do not need to populate every regional cache.

**Step 2: Someone in the USA opens the short URL.**

```
USA Application
      |
      v
USA Redis
      |
      v
Cache Miss
```

**Step 3: The USA application fetches the mapping from a database source.**

It can use a sufficiently up-to-date local database replica if available. Otherwise, it routes to the bucket's home-region database.

**Step 4: It populates the USA Redis cache.**

```
USA Redis:

aB91xZ73 -> https://example.com/products/123
```

**Step 5: Subsequent USA requests are cache hits.**

```
USA User
   |
   v
USA Application
   |
   v
USA Redis
   |
   v
Original URL
```

We don't need to update the USA cache for every new URL. We populate it when the URL is actually requested there.

This is the same cache-aside strategy we used within one region, now applied independently across regions.

# **15. But what about database replication lag?**

This is one of the most important parts of multi-region URL-shortener design.

Suppose a URL is created in India.

```
India Primary DB:
aB91xZ73 -> https://example.com/products/123
```

The database asynchronously replicates the mapping to the USA.

But replication takes 2 seconds.

Now consider:

```
Time 0.0 seconds:
URL is created in India.

Time 0.2 seconds:
USA user opens the URL.

Time 2.0 seconds:
USA database replica receives the mapping.
```

The USA application checks its local database replica at 0.2 seconds.

The record doesn't exist yet.

If we simply return 404, we have a correctness problem.

The URL was successfully created, but a user cannot immediately access it from another region.

We need a strategy for this.

## **15.1 Strategy 1: Use the home-region primary for recently created URLs**

The simplest correct approach is:

1. Check the regional cache.
2. On a miss, try the local replica if it is sufficiently up-to-date.
3. If the record is missing and replication may be behind, read from the authoritative home-region primary.
4. If the authoritative owner also says the mapping doesn't exist, return 404.
5. If it exists, populate the regional cache.

For example:

```
               USA User
                   |
                   v
               USA App
                   |
                   v
                USA Redis
                   |
               Cache Miss
                   |
                   v
          Calculate Bucket / Owner
                   |
                   v
          USA Read Replica
                   |
                Not Found
                   |
                   v
          India Primary DB
                   |
                Found!
                   |
                   v
          Populate USA Redis
                   |
                   v
             HTTP Redirect
```

This approach is simple and protects correctness.

It introduces a cross-region round trip on some cache misses, but those misses should be relatively infrequent once data has replicated and caches have warmed.

**Important:** A missing row in a lagging replica does not prove that the slug does not exist globally.

This is why the authoritative owner matters.

## **15.2 Strategy 2: Use replication positions or consistency tokens**

We can make this more sophisticated.

Suppose the primary commits a transaction and returns a replication position or version token.

Conceptually:

```
Write committed at version: V1005
```

If a subsequent read requires read-after-write consistency, the application can check whether its local replica has applied at least that version.

```
Required version: V1005
USA replica applied: V1002
```

The replica isn't caught up.

The application can wait briefly, select a sufficiently advanced replica, or route to the primary.

Actual implementations depend on the database and replication technology. A database LSN, commit timestamp, or comparable replication watermark may be used.

This avoids blindly treating an old replica as authoritative.

## **15.3 Strategy 3: Synchronously replicate before acknowledging the creation**

Another possibility is:

```
India Primary
     |
     v
Commit mapping
     |
     v
Wait for USA and Europe replication
     |
     v
Return success
```

Now we can guarantee that the mapping has been replicated to the selected regions before the create request succeeds.

The trade-off is write latency.

The user creating a URL in India may have to wait for cross-region network communication.

We also become dependent on the availability of the regions included in our synchronous replication policy.

For a high-throughput URL shortener, this is often not worth imposing on every create operation.

## **My recommendation**

For our design:

**Use asynchronous cross-region replication, regional cache-aside, and authoritative home-region fallback on a cache miss when a local replica may be stale.**

This provides:

- Fast local reads after cache population.
- Durable, authoritative writes.
- Reasonable write latency.
- Correctness for newly created mappings.
- Eventual availability of data from local replicas.

# **16. Should cache invalidation happen in all regions?**

Now let's answer your question directly.

It depends on what operation happened.

| Operation               | Home-region cache           | Other regional caches                             |
| ----------------------- | --------------------------- | ------------------------------------------------- |
| Create a new URL        | Optionally populate it      | Usually populate lazily on demand                 |
| Read a URL              | Populate on miss            | Each region populates independently               |
| Change the destination  | Invalidate/update           | Publish invalidation/update events                |
| Disable or delete a URL | Invalidate                  | Invalidate across regions                         |
| URL expires             | Respect expiry at read time | Respect expiry; propagate tombstones where needed |

### **Case 1: Creating a new URL**

We generally should not eagerly insert every newly created URL into every Redis cluster.

Use lazy cache population.

The durable database replication process makes the data available to regions, while the cache fills based on regional demand.

### **Case 2: Updating a URL**

Now we should notify other regions.

Imagine:

```
Old mapping:
aB91xZ73 -> old-url

New mapping:
aB91xZ73 -> new-url
```

If the USA Redis contains the old mapping, it must be invalidated or updated.

Use the transactional-outbox mechanism we discussed earlier:

```
Home Region DB
      |
      v
Transactional Outbox
      |
      v
Message Broker
      |
      +------------+------------+
      |            |            |
      v            v            v
  India Cache  USA Cache   Europe Cache
   Consumer     Consumer      Consumer
```

The consumers process update events.

For revocation or security-sensitive disable operations, ordinary eventual cache invalidation may not be sufficient. You must decide whether short TTLs, a strongly consistent status check, or a stricter global invalidation acknowledgement is required.

The key trade-off is:

- Faster redirects through caching.
- Faster propagation of destination changes and revocations.

You cannot have unlimited caching and instantaneous global changes for free.

### **Case 3: A popular URL is created**

Suppose a link will be used globally in a large marketing campaign.

We may decide to proactively warm the caches or CDN locations where we expect traffic.

But this is an optimization for known hot links, not the default behavior for every URL.

**The general rule is: replicate durable data according to our availability and latency requirements; populate caches according to demand.**

# **17. Putting everything together: The recommended architecture**

We now have all the building blocks.

Here is the architecture we have evolved towards.

\#chatgpt-mermaid-\_r_a2h\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_a2h\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_a2h\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_a2h\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_a2h\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a2h\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_a2h\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_a2h\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_a2h\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_a2h\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_a2h\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_a2h\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a2h\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a2h\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_a2h\_ p{margin:0;}#chatgpt-mermaid-\_r_a2h\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a2h\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a2h\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a2h\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_a2h\_ .label text,#chatgpt-mermaid-\_r_a2h\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a2h\_ .node rect,#chatgpt-mermaid-\_r_a2h\_ .node circle,#chatgpt-mermaid-\_r_a2h\_ .node ellipse,#chatgpt-mermaid-\_r_a2h\_ .node polygon,#chatgpt-mermaid-\_r_a2h\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_a2h\_ .rough-node .label text,#chatgpt-mermaid-\_r_a2h\_ .node .label text,#chatgpt-mermaid-\_r_a2h\_ .image-shape .label,#chatgpt-mermaid-\_r_a2h\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_a2h\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_a2h\_ .rough-node .label,#chatgpt-mermaid-\_r_a2h\_ .node .label,#chatgpt-mermaid-\_r_a2h\_ .image-shape .label,#chatgpt-mermaid-\_r_a2h\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_a2h\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_a2h\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a2h\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a2h\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_a2h\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_a2h\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_a2h\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_a2h\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_a2h\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_a2h\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_a2h\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a2h\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a2h\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_a2h\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a2h\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_a2h\_ .icon-shape,#chatgpt-mermaid-\_r_a2h\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_a2h\_ .icon-shape p,#chatgpt-mermaid-\_r_a2h\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_a2h\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_a2h\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_a2h\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_a2h\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_a2h\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_a2h\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_a2h\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_a2h\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_a2h\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a2h\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_a2h\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_a2h\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_a2h\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a2h\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_a2h\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_a2h\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a2h\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_a2h\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_a2h\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a2h\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_a2h\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a2h\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_a2h\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_a2h\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_a2h\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_a2h\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_a2h\_ .node rect,#chatgpt-mermaid-\_r_a2h\_ .node circle,#chatgpt-mermaid-\_r_a2h\_ .node ellipse,#chatgpt-mermaid-\_r_a2h\_ .node polygon,#chatgpt-mermaid-\_r_a2h\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_a2h\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_a2h\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_a2h\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_a2h\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a2h\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Europe RegionUSA RegionIndia RegionEurope RegionRedirect / Create APIL1 CacheEurope RedisLocal Shard RouterLocal DB ReplicasHome DB PrimariesUSA RegionRedirect / Create APIL1 CacheUSA RedisLocal Shard RouterLocal DB ReplicasHome DB PrimariesIndia RegionRedirect / Create APIL1 CacheIndia RedisLocal Shard RouterLocal DB ReplicasHome DB PrimariesGlobal UsersGlobal DNS / Anycast / CDNGlobal Versioned ShardTopologyconfig distributionconfig distributionconfig distributionAsync ReplicationAsync Replication

A clarification about the diagram: each region can have database shards for which it is the home region, as well as replicas of shards owned by other regions. The application uses the topology to distinguish the authoritative primary from a local replica.

The diagram simplifies some database connections to keep the high-level flow visible.

Let's walk through the two major flows completely.

# **18. Complete write flow: Creating a short URL**

Imagine a user in India submits:

```
https://example.com/products/123
```

### **Step 1: Request reaches the nearest application region**

```
User in India
      |
      v
India API Gateway
      |
      v
Create URL Service
```

The service validates the request.

For example:

- Is the URL syntactically valid?
- Is the scheme allowed, usually HTTP or HTTPS?
- Is the requested custom slug valid, if custom slugs are supported?
- Is the user within creation limits?
- Is this request a retry of an earlier operation?

If create requests need safe retry semantics, we can accept an idempotency key and store the result against it.

### **Step 2: Generate the slug**

Our chosen method is random 8-character Base62 generation.

```
Generated slug: aB91xZ73
```

### **Step 3: Calculate the logical bucket**

```
bucket = hash("aB91xZ73") % 4096
```

Suppose:

```
Bucket = 105
```

### **Step 4: Identify the authoritative shard and region**

The local topology map contains:

```
Bucket 105 -> India -> Shard A
```

The request has arrived in India, and the bucket belongs to India.

We can write directly to Shard A's primary.

But suppose the bucket belongs to the USA instead:

```
Bucket 105 -> USA -> Shard B
```

Then the India service must forward the write to the USA home region, or use an internal write-routing service that does so.

The important invariant is that the authoritative database owner receives the write.

### **Step 5: Insert into the primary database**

```
INSERT INTO url_mapping (
    slug,
    original_url,
    created_at,
    expires_at,
    status
)
VALUES (
    'aB91xZ73',
    'https://example.com/products/123',
    CURRENT_TIMESTAMP,
    NULL,
    'ACTIVE'
);
```

If the slug already exists, the database rejects the insert due to the primary-key constraint.

We generate a new random slug and retry.

### **Step 6: Commit the mapping**

Once the database transaction successfully commits, the record is durable according to the database's configured durability guarantees.

If our service also needs to publish a creation or analytics event, it can write that into the transactional outbox as part of the transaction.

### **Step 7: Return the short URL**

```
{
  "shortUrl": "https://sho.rt/aB91xZ73"
}
```

We don't need to wait for every region's Redis cache to be populated.

We also don't necessarily need to wait for every remote read replica, unless our product specifically promises synchronous global visibility before acknowledging creation.

### **Step 8: Replicate database data**

The authoritative shard asynchronously replicates the mapping to its configured regional replicas.

```
India Shard A Primary
       |
       +---------> USA Shard A Replica
       |
       +---------> Europe Shard A Replica
```

The other regions can now serve the mapping locally once replication catches up.

### **The complete write path**

\#chatgpt-mermaid-\_r_a42\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_a42\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_a42\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_a42\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_a42\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a42\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_a42\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_a42\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_a42\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_a42\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_a42\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_a42\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a42\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a42\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_a42\_ p{margin:0;}#chatgpt-mermaid-\_r_a42\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a42\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a42\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a42\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_a42\_ .label text,#chatgpt-mermaid-\_r_a42\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a42\_ .node rect,#chatgpt-mermaid-\_r_a42\_ .node circle,#chatgpt-mermaid-\_r_a42\_ .node ellipse,#chatgpt-mermaid-\_r_a42\_ .node polygon,#chatgpt-mermaid-\_r_a42\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_a42\_ .rough-node .label text,#chatgpt-mermaid-\_r_a42\_ .node .label text,#chatgpt-mermaid-\_r_a42\_ .image-shape .label,#chatgpt-mermaid-\_r_a42\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_a42\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_a42\_ .rough-node .label,#chatgpt-mermaid-\_r_a42\_ .node .label,#chatgpt-mermaid-\_r_a42\_ .image-shape .label,#chatgpt-mermaid-\_r_a42\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_a42\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_a42\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a42\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a42\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_a42\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_a42\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_a42\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_a42\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_a42\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_a42\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_a42\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a42\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a42\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_a42\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a42\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_a42\_ .icon-shape,#chatgpt-mermaid-\_r_a42\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_a42\_ .icon-shape p,#chatgpt-mermaid-\_r_a42\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_a42\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_a42\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_a42\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_a42\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_a42\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_a42\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_a42\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_a42\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_a42\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a42\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_a42\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_a42\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_a42\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a42\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_a42\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_a42\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a42\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_a42\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_a42\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a42\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_a42\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a42\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_a42\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_a42\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_a42\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_a42\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_a42\_ .node rect,#chatgpt-mermaid-\_r_a42\_ .node circle,#chatgpt-mermaid-\_r_a42\_ .node ellipse,#chatgpt-mermaid-\_r_a42\_ .node polygon,#chatgpt-mermaid-\_r_a42\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_a42\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_a42\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_a42\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_a42\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a42\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Create RequestValidate RequestGenerate Random SlugHash Slug to BucketLookup Bucket in LocalTopologyLocal Region Owns Bucket?Write to Home Shard PrimaryForward to Home RegionUnique Constraint Passed?Commit MappingReturn Short URLAsync Database ReplicationOutbox Events if NeededYesNoNoYes

# **19. Complete read flow: Opening a short URL**

Now someone in the USA opens:

```
https://sho.rt/aB91xZ73
```

Let's see exactly what happens.

### **Step 1: DNS/edge routing**

The request reaches the nearest suitable edge or application region.

```
USA User
    |
    v
Global DNS / CDN
    |
    v
USA Region
```

A CDN may already have a cached redirect response. Whether it can use that response depends on the redirect cache policy, expiry, and how quickly we need updates and revocations to take effect.

Assume it does not have the response.

### **Step 2: The redirect service receives the slug**

```
GET /aB91xZ73
```

It extracts:

```
slug = aB91xZ73
```

It validates the format.

### **Step 3: Check application-local cache**

```
L1 Cache:
aB91xZ73 -> ?
```

Suppose it is missing.

### **Step 4: Check the USA Redis cluster**

```
USA Redis:
aB91xZ73 -> ?
```

Suppose this is also a cache miss.

### **Step 5: Calculate the bucket**

```
hash(aB91xZ73) % 4096 = 105
```

### **Step 6: Look up the topology**

The USA application's routing map says:

```
Bucket 105 -> India -> Shard A
```

So India is the authoritative home region.

### **Step 7: Try local replica**

Suppose the USA has a replica for Shard A.

The service checks whether it can safely use this replica.

If its replication state is sufficiently current, it queries the replica.

If the record is not there and the replica may be lagging, the missing result is not treated as proof that the URL doesn't exist.

### **Step 8: Fall back to the home-region primary if necessary**

The service queries India Shard A's primary.

```
USA Application
       |
       v
India Shard A Primary
       |
       v
Find slug
       |
       v
Return original URL
```

### **Step 9: Populate the USA regional cache**

The application stores:

```
USA Redis:

aB91xZ73 -> https://example.com/products/123
```

It may also populate its L1 cache.

### **Step 10: Return the redirect**

```
HTTP/1.1 302 Found
Location: https://example.com/products/123
```

On subsequent requests from the USA, the mapping will normally be returned from the USA cache.

It does not need to contact India again until the cache entry expires or is invalidated.

### **The complete read path**

\#chatgpt-mermaid-\_r_a5l\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_a5l\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_a5l\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_a5l\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_a5l\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a5l\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_a5l\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_a5l\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_a5l\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_a5l\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_a5l\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_a5l\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a5l\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a5l\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_a5l\_ p{margin:0;}#chatgpt-mermaid-\_r_a5l\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a5l\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a5l\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a5l\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_a5l\_ .label text,#chatgpt-mermaid-\_r_a5l\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a5l\_ .node rect,#chatgpt-mermaid-\_r_a5l\_ .node circle,#chatgpt-mermaid-\_r_a5l\_ .node ellipse,#chatgpt-mermaid-\_r_a5l\_ .node polygon,#chatgpt-mermaid-\_r_a5l\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_a5l\_ .rough-node .label text,#chatgpt-mermaid-\_r_a5l\_ .node .label text,#chatgpt-mermaid-\_r_a5l\_ .image-shape .label,#chatgpt-mermaid-\_r_a5l\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_a5l\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_a5l\_ .rough-node .label,#chatgpt-mermaid-\_r_a5l\_ .node .label,#chatgpt-mermaid-\_r_a5l\_ .image-shape .label,#chatgpt-mermaid-\_r_a5l\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_a5l\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_a5l\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a5l\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a5l\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_a5l\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_a5l\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_a5l\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_a5l\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_a5l\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_a5l\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_a5l\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a5l\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a5l\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_a5l\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a5l\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_a5l\_ .icon-shape,#chatgpt-mermaid-\_r_a5l\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_a5l\_ .icon-shape p,#chatgpt-mermaid-\_r_a5l\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_a5l\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_a5l\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_a5l\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_a5l\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_a5l\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_a5l\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_a5l\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_a5l\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_a5l\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a5l\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_a5l\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_a5l\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_a5l\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a5l\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_a5l\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_a5l\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a5l\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_a5l\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_a5l\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a5l\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_a5l\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a5l\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_a5l\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_a5l\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_a5l\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_a5l\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_a5l\_ .node rect,#chatgpt-mermaid-\_r_a5l\_ .node circle,#chatgpt-mermaid-\_r_a5l\_ .node ellipse,#chatgpt-mermaid-\_r_a5l\_ .node polygon,#chatgpt-mermaid-\_r_a5l\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_a5l\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_a5l\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_a5l\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_a5l\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a5l\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}GET short URLNearest RegionExtract and Validate SlugL1 Local CacheReturn RedirectRegional RedisCalculate Logical BucketRead Local DB ReplicaRecord Found and ReplicaCurrent?Populate Regional CacheFind Home Region and ShardRead Home PrimaryMapping Found?404 Not FoundReturn HTTP RedirectHitMissHitMissYesNoYesNo

This is the key read flow to understand and be able to explain in an interview.

# **20. How do we add database shards without breaking everything?**

We've discussed logical buckets, but let's explore how we actually scale the database over time.

Suppose we initially have four database shards:

```
Shard A -> Buckets 0 - 1023
Shard B -> Buckets 1024 - 2047
Shard C -> Buckets 2048 - 3071
Shard D -> Buckets 3072 - 4095
```

After several months, Shard A becomes overloaded.

We add Shard E.

We want to move some buckets from A to E.

For example:

```
Before:

Buckets 0 - 1023 -> Shard A


After:

Buckets 0 - 511   -> Shard A
Buckets 512 - 1023 -> Shard E
```

The hash function doesn't change.

```
aB91xZ73 -> Bucket 105
```

It will always produce Bucket 105.

Only the physical owner of Bucket 105 changes if we move that bucket.

## **20.1 How do we migrate the data?**

Let's assume we decide to move Bucket 512.

We can use the following migration process.

**Step 1: Provision the destination shard.**

Create Shard E and prepare its indexes, storage, replication, and monitoring.

**Step 2: Copy the bucket's existing records.**

Copy all records belonging to Bucket 512 from Shard A to Shard E.

We might store the bucket ID alongside each record to make migration and filtering straightforward.

```
Shard A:
Bucket 512 -> 100,000 records

Shard E:
Bucket 512 -> 100,000 copied records
```

**Step 3: Capture changes while copying.**

Users may still create, update, or disable URLs while the initial copy runs.

We must ensure those changes aren't lost.

A common solution is change data capture (CDC), using the database's transaction log or another reliable change stream.

```
Shard A
   |
   +---- Initial Snapshot --------> Shard E
   |
   +---- Changes After Snapshot --> Shard E
```

Shard E catches up with the changes made during the copy.

**Step 4: Cut over ownership.**

Once the destination is synchronized, we stop accepting new writes for the bucket on Shard A.

We establish a final migration watermark, ensure Shard E has applied the changes through that watermark, and then change the topology:

```
Old:
Bucket 512 -> Shard A

New:
Bucket 512 -> Shard E
```

During cutover, stale application servers need to be handled safely.

For example, a server with an older topology might still try to write to Shard A.

We can use topology versions or ownership epochs:

```
Bucket 512:
Old owner epoch = 10
New owner epoch = 11
```

A write carrying an outdated ownership epoch is rejected or forwarded, and the application refreshes its routing configuration and retries.

The old shard must not accept an uncoordinated write after the bucket has been fenced and moved. Otherwise, we could create two independent authoritative copies.

**Step 5: Retire old data after verification.**

Keep the old copy temporarily for operational safety, then remove it after confirming that the new owner is serving traffic correctly.

### **Why use logical buckets instead of moving entire shards?**

Imagine that we didn't have logical buckets.

We would have to move large portions of physical shards when adding capacity.

With logical buckets, we can move smaller groups of records.

This gives us much finer control over:

- Data distribution.
- Storage balance.
- Request distribution.
- Shard expansion.
- Migration.
- Regional ownership changes.

One more important point: don't balance only by record count.

For a URL shortener, we should monitor both:

- Storage and index size.
- Read/write traffic per bucket.

A bucket containing fewer records might still receive substantially more traffic due to popular links.

# **21. What happens when servers, databases, or regions fail?**

A distributed design must explain not just the normal request path but also how it behaves when components fail.

Let's examine the most important failures.

## **21.1 Application server fails**

This is relatively straightforward.

Our application servers are stateless except for temporary local caches and locally cached topology configuration.

```
Before:

Load Balancer
   |
   +---- App 1
   +---- App 2
   +---- App 3


App 2 crashes


After:

Load Balancer
   |
   +---- App 1
   +---- App 3
```

The load balancer stops sending requests to the failed server.

Other application servers independently calculate the same bucket and use the same topology.

We don't lose any URL mappings because they are stored in the database.

## **21.2 Redis fails**

Redis is a cache, not the source of truth.

If Redis becomes unavailable:

```
Application
     |
     v
Redis Unavailable
     |
     v
Database Shard
```

The application can fall back to the database.

But there's a danger.

Suppose Redis was handling 95% of reads.

If Redis fails, the database suddenly receives all the traffic.

This is called a cache-failure load spike.

We should prepare for it using:

- Redis high availability and failover.
- Circuit breakers.
- Database admission control.
- Request coalescing for hot keys.
- Temporary rate limiting or load shedding.
- Controlled cache recovery and warming.

The key idea is that cache failure should degrade performance, not immediately make the application unusable.

## **21.3 One database primary fails**

Suppose:

```
Shard A:

Primary:  Failed
Replica 1: Healthy
Replica 2: Healthy
```

We need database failover.

The database cluster or failover manager selects an eligible replica and promotes it to primary.

The service endpoint for Shard A's primary must then resolve to the new primary.

The application can continue using a stable shard-group endpoint:

```
shard-a-primary.database.internal
```

It shouldn't need a new application release just because a database machine changed.

The failover process must also ensure that the previous primary cannot continue accepting writes after a new primary has been elected. We need proper fencing or equivalent split-brain protection.

## **21.4 A region becomes unavailable**

Suppose India becomes unavailable.

```
India Region: DOWN

USA Region: UP
Europe Region: UP
```

Global DNS or traffic management routes users to the remaining healthy regions.

But what about buckets for which India was the home region?

Their authoritative primary is unavailable.

We need a failover strategy:

1. Determine which database replicas have sufficiently up-to-date data.
2. Promote an eligible replica in another region.
3. Fence the old primary.
4. Update the global shard-placement configuration.
5. Distribute the new topology version to the remaining application servers.
6. Resume writes when ownership is established.

For example:

```
Before:

Bucket 105 -> India -> Shard A


After failover:

Bucket 105 -> USA -> Shard A
```

There may be some data loss if the database used asynchronous replication and the failed primary had committed writes that never reached a replica.

That is why we need to define our recovery objectives:

- **RPO:** How much committed data loss, if any, is acceptable?
- **RTO:** How quickly must the service recover?

Synchronous replication, stronger durability settings, and consensus-based failover can reduce data-loss risk, but introduce availability and latency trade-offs.

Don't simply say, "We have replicas, so failover is solved." Replicas, primary election, fencing, and recovery objectives are all part of the design.

## **21.5 Topology/configuration service fails**

Remember that applications have a local topology map.

This is useful here.

Even if the topology manager becomes unavailable, existing application servers can keep routing requests using their last known valid configuration.

This means topology distribution should not become a mandatory online dependency for every redirect.

However, a topology change cannot be distributed while the configuration system is unavailable.

We should fail safely for administrative operations such as shard migration and ownership changes rather than allowing conflicting configurations.

# **22. A few more design decisions that make the system production-ready**

We have the core architecture. Now let's address some important details that make it a proper production system.

## **22.1 Do not update click counts synchronously in the URL record**

Imagine a popular link receives one million redirects per second.

A tempting implementation is:

```
UPDATE url_mapping
SET click_count = click_count + 1
WHERE slug = 'aB91xZ73';
```

This is a bad approach for the redirect hot path.

All those requests would contend on one database row.

We have a hot-row problem.

Instead:

```
Redirect Request
       |
       +---- Resolve URL and return redirect
       |
       +---- Publish click event asynchronously
```

The click event might contain:

```
{
  "slug": "aB91xZ73",
  "timestamp": "2026-11-20T10:00:00Z",
  "region": "USA"
}
```

A message-processing or analytics pipeline consumes these events and aggregates them separately.

For example:

```
Redirect Service
      |
      v
Message Broker
      |
      v
Analytics Consumers
      |
      v
Analytics Database / Data Warehouse
```

We keep redirect lookup and analytics workloads separate.

That prevents analytics updates from overwhelming the primary URL-mapping database.

## **22.2 Think about URL expiry**

Suppose users can create links that expire after 30 days.

The mapping has:

```
expires_at = 2026-12-20T10:00:00Z
```

We should check the expiration during resolution.

Expired URLs should return an appropriate response instead of redirecting.

We should also make the cache TTL no longer than the remaining validity period.

For example:

```
URL expires in 5 minutes.
Cache TTL = 5 minutes.
```

Not:

```
URL expires in 5 minutes.
Cache TTL = 1 hour.
```

A background cleanup process can delete expired records eventually. Expiration should not depend on the cleanup job running exactly on time.

## **22.3 Choosing HTTP 301 versus 302**

A URL shortener commonly uses an HTTP redirect response.

The choice matters.

| Response | Meaning                               | Typical use                                 |
| -------- | ------------------------------------- | ------------------------------------------- |
| 301      | Permanent redirect                    | Destination is intended to remain permanent |
| 302      | Temporary redirect                    | Destination might be changed                |
| 307      | Temporary redirect, preserving method | When method preservation matters            |

For a general URL shortener, 302 is a sensible default if destinations might change.

We should be deliberate about cache headers as browsers and CDNs may cache redirects.

For example, a long-lived 301 may continue to be used by a browser even after we update the destination on our server.

## **22.4 Rate limiting and abuse protection**

Anyone can potentially create short URLs or send redirect requests.

We need controls for:

- Creation rate limits per user or API key.
- Global rate limits to protect the service.
- Slug enumeration and automated abuse.
- Phishing and malware reporting.
- Blocked destinations and abuse detection.
- Limits on URL length and allowed schemes.
- Abuse monitoring and takedown mechanisms.

The redirect endpoint shouldn't fetch the destination URL on the user's behalf. It should return a redirect response. That separation also avoids turning a basic redirect lookup into an unnecessary server-side URL-fetching operation.

## **22.5 Database indexes**

Our primary lookup is:

```
SELECT original_url, expires_at, status
FROM url_mapping
WHERE slug = ?;
```

The most important index is:

```
PRIMARY KEY (slug)
```

The slug is also our sharding key, so a lookup reaches one shard and uses its local primary-key index.

If we introduce extra queries such as:

```
Find all URLs created by a particular user.
```

We need to think separately about those access patterns.

A user ID may not be the sharding key, so such queries may require additional indexes, a separate user-to-slug index, or a secondary data store.

The important design rule is:

**Choose the shard key based primarily on the dominant access pattern.**

For this URL shortener, the dominant pattern is resolving a URL by slug. That's why slug is a good shard key.

# **23. Answering your original questions directly**

Let's consolidate the most important design decisions.

## **23.1 How do we identify which shard a slug belongs to?**

We use a deterministic hash:

\\[ bucket = hash(slug) \bmod 4096 \\]

Then use the routing configuration:

\\[ shard = topology[bucket] \\]

For multi-region:

\\[ (homeRegion, shard) = topology[bucket] \\]

The same slug always produces the same logical bucket.

The bucket's physical owner can change through a controlled migration.

## **23.2 Does the slug itself tell us which shard it belongs to?**

Not directly in our recommended design.

The slug is an opaque random string.

We calculate a logical bucket by hashing it.

There is another possible design in which we encode shard or region information into the slug itself:

```
Slug = Region/Shard bits + Unique ID bits
```

For example, some of the slug's bits could identify the region.

But that has trade-offs:

- It consumes part of our limited 8-character namespace.
- It couples slug generation to physical placement.
- Changing shard allocation becomes more complicated.
- It exposes some internal structure.

We don't need that complexity.

**For our architecture, hash the slug and use a local routing map.**

## **23.3 Does every application server maintain a map?**

Yes.

Each application server maintains a local, versioned topology configuration.

Conceptually:

```
Application Server
       |
       v
  Local Topology Map
       |
       +-- Bucket 105  -> India / Shard A
       +-- Bucket 821  -> USA / Shard B
       +-- Bucket 3021 -> Europe / Shard C
```

A control-plane service distributes changes to these maps.

We do not query it for each request.

## **23.4 How does the application identify the physical database server?**

There are two routing steps:

```
Slug
 |
 v
Hash -> Logical Bucket
 |
 v
Topology Map -> Shard Group
 |
 v
Database Service Discovery
 |
 +--> Primary (for writes)
 |
 +--> Replica (for reads)
```

The shard group is a logical identity. Service discovery or a database proxy resolves its current primary and replica endpoints.

We don't hardcode physical database IP addresses in the application.

## **23.5 How does caching work with multiple shards?**

Caching happens before database routing.

```
Request
   |
   v
L1 Cache
   |
   v
Regional Redis
   |
   +-- Hit -> Return original URL
   |
   +-- Miss
          |
          v
     Hash slug
          |
          v
     Find DB shard
          |
          v
     Query database
```

Redis has its own partitioning mechanism, independent of database sharding.

A cache hit doesn't need a database shard lookup.

## **23.6 How does multi-region caching work?**

Each region has its own cache cluster.

```
India -> Redis India
USA   -> Redis USA
EU    -> Redis Europe
```

We generally do not populate every regional cache whenever a URL is created.

Instead:

- New URL: persist to the authoritative database; cache locally or populate on demand.
- First regional read: fetch from a sufficiently current local replica or fall back to the home primary.
- Subsequent regional reads: serve from that region's cache.
- Update/delete/revoke: publish invalidation events to affected regional caches.
- Expiration: enforce expiry during resolution and use cache TTLs appropriately.

This keeps cache storage demand-driven.

## **23.7 How do reads and writes differ across regions?**

| Operation                             | Routing                                                                    |
| ------------------------------------- | -------------------------------------------------------------------------- |
| Create URL                            | Generate slug, calculate bucket, write to the bucket's home-region primary |
| Redirect, cache hit                   | Serve from the nearest region's cache                                      |
| Redirect, cache miss                  | Try a sufficiently current local replica                                   |
| Redirect, local replica miss or stale | Query the authoritative home-region primary                                |
| Database replication                  | Asynchronously replicate to configured regional replicas                   |
| Database failover                     | Promote a replica, fence old primary, update topology                      |

This is our recommended default consistency model.

# **24. The final architecture I would recommend**

Here is the design in one place.

| Concern                | Decision                                              |
| ---------------------- | ----------------------------------------------------- |
| Slug format            | Random 8-character Base62                             |
| Slug uniqueness        | Database unique constraint, retry on collision        |
| Primary identifier     | Slug itself                                           |
| Database shard key     | Slug                                                  |
| Logical partitioning   | 4,096 fixed virtual buckets                           |
| Bucket assignment      | Stable hash of slug modulo bucket count               |
| Physical shard routing | Versioned bucket-to-shard map                         |
| Multi-region routing   | Bucket-to-home-region-and-shard map                   |
| Routing configuration  | Locally cached by application servers                 |
| Database writes        | One authoritative primary per bucket                  |
| Database reads         | Regional replicas where available                     |
| Read fallback          | Home-region primary for stale or missing replica data |
| Cache strategy         | Cache-aside                                           |
| Cache placement        | Separate Redis cluster per region                     |
| Hot links              | L1 cache, CDN where appropriate, request coalescing   |
| Cache invalidation     | Transactional outbox and regional consumers           |
| Database replication   | Asynchronous cross-region replication                 |
| Analytics              | Asynchronous event pipeline                           |
| Shard scaling          | Move logical buckets between physical shards          |
| Failover               | Replica promotion, fencing, topology update           |

The architecture looks like this at a conceptual level:

\#chatgpt-mermaid-\_r_a9l\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_a9l\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_a9l\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_a9l\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_a9l\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a9l\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_a9l\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_a9l\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_a9l\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_a9l\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_a9l\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_a9l\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a9l\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a9l\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_a9l\_ p{margin:0;}#chatgpt-mermaid-\_r_a9l\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a9l\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a9l\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a9l\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_a9l\_ .label text,#chatgpt-mermaid-\_r_a9l\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a9l\_ .node rect,#chatgpt-mermaid-\_r_a9l\_ .node circle,#chatgpt-mermaid-\_r_a9l\_ .node ellipse,#chatgpt-mermaid-\_r_a9l\_ .node polygon,#chatgpt-mermaid-\_r_a9l\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_a9l\_ .rough-node .label text,#chatgpt-mermaid-\_r_a9l\_ .node .label text,#chatgpt-mermaid-\_r_a9l\_ .image-shape .label,#chatgpt-mermaid-\_r_a9l\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_a9l\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_a9l\_ .rough-node .label,#chatgpt-mermaid-\_r_a9l\_ .node .label,#chatgpt-mermaid-\_r_a9l\_ .image-shape .label,#chatgpt-mermaid-\_r_a9l\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_a9l\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_a9l\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a9l\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a9l\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_a9l\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_a9l\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_a9l\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_a9l\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_a9l\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_a9l\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_a9l\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a9l\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a9l\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_a9l\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a9l\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_a9l\_ .icon-shape,#chatgpt-mermaid-\_r_a9l\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_a9l\_ .icon-shape p,#chatgpt-mermaid-\_r_a9l\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_a9l\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_a9l\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_a9l\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_a9l\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_a9l\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_a9l\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_a9l\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_a9l\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_a9l\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a9l\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_a9l\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_a9l\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_a9l\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a9l\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_a9l\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_a9l\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a9l\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_a9l\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_a9l\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a9l\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_a9l\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a9l\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_a9l\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_a9l\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_a9l\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_a9l\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_a9l\_ .node rect,#chatgpt-mermaid-\_r_a9l\_ .node circle,#chatgpt-mermaid-\_r_a9l\_ .node ellipse,#chatgpt-mermaid-\_r_a9l\_ .node polygon,#chatgpt-mermaid-\_r_a9l\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_a9l\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_a9l\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_a9l\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_a9l\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a9l\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Global UsersGlobal Traffic Routing / CDNIndia RegionUSA RegionEurope RegionRedirect and Create ServicesRedirect and Create ServicesRedirect and Create ServicesL1 Cache + India RedisL1 Cache + USA RedisL1 Cache + Europe RedisLocal Topology ConfigurationLocal Topology ConfigurationLocal Topology ConfigurationBucket to Home Region andShard MappingDatabase RoutingShard A PrimaryShard B PrimaryShard C PrimaryRegional ReplicasOutbox / EventsMessage BrokerRegional Cache InvalidationConsumersAnalytics Pipeline

# **25. The mental model: Understand it without memorizing it**

I want to finish by bringing everything back to the original problem.

Imagine you are implementing the system yourself.

You receive:

```
https://sho.rt/aB91xZ73
```

You ask yourself:

**Question 1: Where is the original URL?**

It is stored in a database record whose primary lookup key is `aB91xZ73`.

**Question 2: How do I avoid reading the database every time?**

Check the local and regional caches first.

**Question 3: The cache doesn't have it. Which database should I query?**

Hash the slug to get its logical bucket.

**Question 4: How do I know which physical database owns that bucket?**

Consult the local bucket-to-shard routing map.

**Question 5: The bucket has a home region. What if I'm in another region?**

Use a sufficiently up-to-date local database replica. If it cannot serve the read safely, query the authoritative home-region primary.

**Question 6: Why not ask another service where the database is on every request?**

Because that introduces unnecessary latency and a bottleneck. The application already has the routing configuration locally.

**Question 7: What happens when we add more shards?**

Move logical buckets to new physical shards. The hashing formula and bucket identities remain unchanged.

**Question 8: Why doesn't every region have the mapping in Redis?**

Because caches should contain frequently accessed data, not necessarily every record. Each region populates its cache as users request links.

**Question 9: What happens when the destination changes?**

Commit the database update, publish a reliable event, and invalidate or update regional caches.

**Question 10: What happens when a region fails?**

Route users to healthy regions. Promote eligible database replicas if necessary, fence the previous primary, and update the topology.

That's the entire architecture.

## **The three most important ideas to retain**

**1. Separate logical identity from physical location.**

```
Slug -> Logical Bucket -> Physical Shard
```

The logical bucket is stable. The physical shard can change.

That's what makes horizontal database scaling manageable.

**2. Separate data ownership from data access.**

```
Home-region primary -> Owns the writes

Regional replica     -> Serves local reads

Regional Redis       -> Serves frequent reads
```

A region can serve a URL without being its authoritative owner.

**3. Separate durable replication from caching.**

```
Database replication -> Makes durable data available in other regions.

Cache-aside          -> Makes frequently accessed data fast in each region.
```

Database replication is a data-availability decision. Cache population is a performance decision.

They should not be confused.

## **A final design principle**

When designing distributed systems, try to avoid making every component responsible for everything.

For this URL shortener:

- The slug generator generates unique identifiers.
- The hash function determines logical placement.
- The topology map determines physical placement.
- Service discovery finds the current database endpoints.
- The database owns durable mappings.
- Replicas improve read locality and availability.
- Redis accelerates repeated lookups.
- The event pipeline propagates changes and handles analytics.
- The control plane manages placement and topology changes.

Each component has one clear responsibility.

Once you understand why each responsibility exists, the architecture becomes much easier to derive from scratch rather than memorize.






















-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------

















