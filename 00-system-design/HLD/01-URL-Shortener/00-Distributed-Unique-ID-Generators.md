# **Distributed Unique ID Generators — Complete Guide**

Distributed unique ID generation is an important System Design topic because almost every large-scale system needs to generate identifiers without depending on a single database or server.

Think about systems such as:

- Amazon generating millions of order IDs.
- Uber generating trip IDs.
- Instagram generating post IDs.
- Kafka generating event identifiers.
- Payment systems generating transaction IDs.
- Microservices creating records independently across multiple regions.

The challenge is simple to describe but surprisingly interesting:

**How can multiple servers generate unique IDs simultaneously, at a massive scale, without coordinating with each other for every ID?**

Let's understand the problem from first principles.

# **1. First understand the problem**

Imagine you have three application servers:

```
                Application
                    |
          +---------+---------+
          |         |         |
       Server A  Server B  Server C
          |         |         |
          ?         ?         ?
```

All three servers need to generate IDs for newly created orders.

Suppose they independently generate IDs:

```
Server A → 1001
Server B → 1001
Server C → 1001
```

We have a collision.

A unique ID generator must ensure that:

```
Server A → 1001
Server B → 1002
Server C → 1003
```

Or perhaps:

```
Server A → 891273645123
Server B → 891273645124
Server C → 891273645125
```

The exact format doesn't matter. What matters is that IDs don't collide.

## **1.1 What makes distributed ID generation difficult?**

There are several challenges.

| Challenge              | Explanation                                                        |
| ---------------------- | ------------------------------------------------------------------ |
| Uniqueness             | Two servers must never generate the same ID.                       |
| Scalability            | Millions of IDs may need to be generated per second.               |
| Availability           | ID generation should continue even when some infrastructure fails. |
| Low latency            | Generating an ID should not require an expensive network request.  |
| Distributed generation | Multiple servers and regions should generate IDs independently.    |
| Ordering               | Sometimes IDs should roughly follow creation time.                 |
| Storage efficiency     | Smaller IDs can reduce database index and storage costs.           |

Not every application needs all these properties.

For example, a payment system may prioritize uniqueness and reliability. A social media platform may also care about database index locality and time ordering.

This is why we have different ID-generation algorithms.

# **2. The big picture: All major approaches**

Before diving into the details, remember that almost all distributed ID-generation approaches belong to a few fundamental families.

| Approach                     | Basic idea                                         | Examples                      |
| ---------------------------- | -------------------------------------------------- | ----------------------------- |
| Database sequence            | One central authority allocates increasing numbers | SQL sequences, auto-increment |
| Distributed range allocation | Give each server a unique range of numbers         | Hi-Lo, Segment ID             |
| Centralized counter          | One service atomically increments a counter        | Redis INCR, Ticket Server     |
| Random generation            | Generate sufficiently large random values          | UUIDv4                        |
| Time-based generation        | Combine time with machine information              | UUIDv1, UUIDv6                |
| Time + machine + sequence    | Combine timestamp, machine ID and local counter    | Snowflake                     |
| Time + randomness            | Combine timestamp with random bits                 | UUIDv7, ULID, KSUID           |
| Structured document ID       | Combine timestamp, process randomness and counter  | MongoDB ObjectId              |
| Deterministic generation     | Hash a namespace and a name                        | UUIDv3, UUIDv5                |

The most important algorithms to understand deeply are:

1. Database sequence and distributed range allocation.
2. Redis INCR / Ticket Server.
3. UUIDv4.
4. UUIDv1 and UUIDv6.
5. Twitter Snowflake.
6. UUIDv7.
7. ULID.
8. MongoDB ObjectId.
9. KSUID.

We'll also discuss how to choose between them.

# **3. Approach 1: Database Auto-Increment / Sequence**

This is the simplest way to generate unique identifiers.

Imagine an application backed by PostgreSQL or MySQL.

```
Application
    |
    v
Database
    |
    v
ID = 1001
ID = 1002
ID = 1003
ID = 1004
```

Whenever a new record is inserted, the database generates the next available number.

Example:

```
CREATE TABLE orders (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id BIGINT,
    amount DECIMAL
);
```

Inserting three orders:

```
INSERT INTO orders (customer_id, amount)
VALUES (101, 500);

INSERT INTO orders (customer_id, amount)
VALUES (102, 700);

INSERT INTO orders (customer_id, amount)
VALUES (103, 900);
```

The resulting IDs could be:

```
1001
1002
1003
```

## **3.1 How does the database guarantee uniqueness?**

The database maintains a sequence.

Conceptually:

```
Current sequence value = 1000

Client A requests next value
    → 1001

Client B requests next value
    → 1002

Client C requests next value
    → 1003
```

The database serializes access to the sequence internally.

Even when multiple transactions request a number simultaneously, they receive different values.

## **3.2 The distributed problem**

Now imagine:

```
                    Application
                         |
             +-----------+-----------+
             |           |           |
          Server A    Server B    Server C
             |           |           |
             +-----------+-----------+
                         |
                    PostgreSQL
```

All application servers depend on the same database to generate IDs.

This works, but creates a centralized dependency.

Potential problems:

- Database becomes a bottleneck at extremely high write volumes.
- Database availability directly affects ID generation.
- Cross-region applications may experience additional network latency.
- The database must handle the coordination and allocation workload.

**Important:** Database auto-increment is still perfectly reasonable for many applications. Don't automatically replace it with a distributed ID generator just because you're building a distributed system.

## **3.3 What happens with multiple independent databases?**

Suppose you have two regions:

```
Region A                     Region B

Database A                   Database B

1                            1
2                            2
3                            3
```

Both databases independently generate the same IDs.

You now have collisions when combining the records.

One solution is to use different ranges:

```
Database A → 1, 2, 3, 4, 5 ...
Database B → 1000001, 1000002, 1000003 ...
```

Another is to use different increments and offsets:

```
Database A → 1, 3, 5, 7, 9 ...
Database B → 2, 4, 6, 8, 10 ...
```

This works with known, fixed database identities, but becomes less convenient as the number of databases changes.

### **Remember**

Database sequences are centralized allocation.

They provide straightforward uniqueness, but the database participates in the ID-generation process.

# **4. Approach 2: Distributed Range Allocation (Hi-Lo / Segment ID)**

This is one of the most important approaches because it solves the database bottleneck without completely abandoning database-based coordination.

The key idea is:

**Instead of asking the database for every single ID, ask the database for a batch of IDs and generate them locally.**

Let's understand why this works.

## **4.1 The naive implementation**

Suppose 3 application servers need to generate IDs.

```
Server A ───┐
Server B ───┼──── Database Sequence
Server C ───┘
```

Every time a server needs an ID, it contacts the database.

Suppose we generate 1 million IDs.

That's potentially 1 million database sequence allocation requests.

Can we reduce this?

Yes.

## **4.2 The range allocation solution**

Instead of allocating one ID at a time, allocate a range.

Suppose the database allocates ranges of 1,000 IDs.

```
                  ID Allocator
                  (Database)
                       |
             Allocate unique ranges
                       |
          +------------+------------+
          |            |            |
       Server A     Server B     Server C
          |            |            |
      1 - 1000     1001 - 2000  2001 - 3000
```

Now each server can generate IDs independently.

Example:

**Server A:**

```
1001
1002
1003
1004
...
2000
```

**Server B:**

```
2001
2002
2003
2004
...
3000
```

**Server C:**

```
3001
3002
3003
...
4000
```

Notice that they don't need to contact the database for every ID.

They contact it only when they need a new range.

## **4.3 How does the database allocate ranges safely?**

Maintain an allocation table:

```
CREATE TABLE id_allocator (
    biz_tag VARCHAR(100) PRIMARY KEY,
    max_id BIGINT NOT NULL,
    step BIGINT NOT NULL
);
```

Example:

| biz_tag | max_id | step |
| ------- | ------ | ---- |
| orders  | 1000   | 1000 |

When Server A requests a range, the database atomically updates `max_id`.

Conceptually:

```
UPDATE id_allocator
SET max_id = max_id + step
WHERE biz_tag = 'orders'
RETURNING max_id;
```

The previous value and new value determine the allocated range.

Server A gets:

```
Range: 1001 - 2000
```

The database now stores:

```
max_id = 2000
```

Server B requests another range:

```
Range: 2001 - 3000
```

The update must be atomic, so concurrent requests cannot receive the same range.

## **4.4 What happens inside the application server?**

Each server maintains a local counter.

```
class SegmentGenerator {

    private long currentId;
    private long maxId;

    public synchronized long nextId() {

        if (currentId > maxId) {
            allocateNewSegment();
        }

        return currentId++;
    }

    private void allocateNewSegment() {
        // Request a new range from the allocator database.
    }
}
```

The real implementation needs to handle synchronization, segment replacement, failures and safe memory visibility. This is just the basic idea.

The important point:

```
ID generation = Local operation

Range allocation = Occasional database operation
```

## **4.5 Hi-Lo algorithm**

Hi-Lo is a closely related technique.

The name itself tells you how it works:

- HI = Group or range number.
- LO = Position inside that group.

Suppose the database gives us:

```
HI = 17
```

And our configured block size is:

```
LO range = 0 to 999
```

We can generate:

```
ID = HI * blockSize + LO
```

Therefore:

```
HI = 17
Block size = 1000

LO = 0   → 17000
LO = 1   → 17001
LO = 2   → 17002
...
LO = 999 → 17999
```

Once the LO counter is exhausted, request another HI value.

For example:

```
HI = 18

18000
18001
18002
...
18999
```

The HI value is allocated centrally. LO is incremented locally.

This is a very useful mental model.

## **4.6 What happens when a server crashes?**

Suppose Server A received:

```
1001 - 2000
```

But it generated only:

```
1001 - 1200
```

Then Server A crashes.

What happens to:

```
1201 - 2000?
```

Those IDs are wasted.

We must not casually assign them to another server because Server A might still be running, or might recover and continue using its previously allocated range.

The simplest safe approach is:

**Abandon the unused part of the allocated range. Allocate a completely new range to the replacement server.**

This creates gaps, but gaps are not a problem for most ID systems.

## **4.7 How can we improve availability?**

A common optimization is double buffering.

Instead of waiting until the current range is exhausted:

```
Current segment: 1001 - 2000
```

Start fetching the next segment when the current segment reaches, for example, 80%.

```
Current segment: 1001 - 2000

                 |
                 | 80% consumed
                 v

         Request next segment

Current segment: 1001 - 2000
Next segment:    2001 - 3000
```

When the current segment is exhausted, switch to the already-loaded segment.

This avoids making a database request at the exact moment an application needs an ID.

### **Advantages**

- Small numeric IDs.
- Very high generation throughput.
- No database request for every ID.
- Works with multiple independent application servers.
- Straightforward implementation.

### **Disadvantages**

- Requires a central range allocator.
- Unused ranges can create gaps.
- Needs careful concurrency and range management.
- Allocator availability matters when a new range is needed.

### **When should you use it?**

When you want numeric IDs, high throughput and simple distributed generation, but don't want to introduce Snowflake's clock and worker-ID complexity.

**Real-world connection:** Meituan's Leaf ID generator implements a Segment mode using database-backed range allocation. It also offers a Snowflake mode.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub+1



# **5. Approach 3: Redis INCR / Ticket Server**

This is another important approach.

It is conceptually similar to database auto-increment, except Redis acts as the central counter.

## **5.1 Basic idea**

Redis provides an atomic increment operation:

```
INCR order_id
```

Suppose the current value is:

```
1000
```

Now three servers request an ID concurrently.

```
Server A → INCR order_id → 1001
Server B → INCR order_id → 1002
Server C → INCR order_id → 1003
```

Redis guarantees that each increment operation is atomic on the relevant primary.

That's why servers receive different IDs.

## **5.2 Architecture**

```
               Application
                    |
          +---------+---------+
          |         |         |
       Server A  Server B  Server C
          |         |         |
          +---------+---------+
                    |
                   Redis
                    |
                INCR order_id
```

A simple Java example:

```
public long generateId() {
    return redisTemplate
            .opsForValue()
            .increment("order_id");
}
```

Every call gets a different value, assuming the counter is not reset or rolled back.

## **5.3 Why is this called a Ticket Server?**

Think about a movie theatre.

A ticket counter issues:

```
Ticket 1
Ticket 2
Ticket 3
Ticket 4
```

Two people cannot receive the same ticket number.

A centralized ID-generating service works similarly.

Every application requests the next ticket.

This pattern is commonly called a Ticket Server.

## **5.4 What are the problems?**

The biggest problem is centralization.

Imagine generating 100,000 IDs per second.

Every request goes to Redis.

```
Server A ───┐
Server B ───┼──── Redis
Server C ───┤
Server D ───┘
```

Redis becomes a central dependency.

Other concerns:

**1. Network latency**

Every ID requires a Redis network operation.

**2. Throughput bottleneck**

The counter key is updated serially on its Redis primary. A single hot key cannot be distributed across multiple Redis Cluster primaries merely by adding more shards.

**3. Redis availability**

When Redis is unavailable, ID generation is unavailable unless you've designed another fallback.

**4. Failover and durability**

Redis replication is not automatically a consensus-backed, strictly durable sequence allocator. A primary can acknowledge an increment before a replica has received it.

If the primary fails and a replica is promoted with a counter value behind the acknowledged value, previously issued IDs could potentially be issued again.

This is a critical issue for systems requiring strict uniqueness.

## **5.5 Optimization: Allocate blocks using INCRBY**

Here's an interesting improvement.

Instead of calling `INCR` for every ID, use:

```
INCRBY order_id 1000
```

Suppose Redis returns:

```
2000
```

The requesting server owns:

```
1001 - 2000
```

It generates those 1,000 IDs locally.

The next server requests another block:

```
INCRBY order_id 1000
```

Redis returns:

```
3000
```

It owns:

```
2001 - 3000
```

You have now combined two concepts:

```
Redis atomic counter
        +
Range allocation
        =
Distributed local ID generation
```

This dramatically reduces calls to Redis.

### **Comparison: Redis INCR vs Segment allocation**

| Feature                | Redis INCR  | Segment allocation                |
| ---------------------- | ----------- | --------------------------------- |
| Central allocator      | Redis       | Database or another allocator     |
| Per-ID network request | Yes         | No                                |
| Local ID generation    | No          | Yes                               |
| Sequential IDs         | Yes         | Locally sequential within a range |
| Main bottleneck        | Hot counter | Range allocation                  |
| Crash gaps             | Possible    | Possible                          |

The important distinction is that Redis INCR is a centralized counter approach, whereas segment allocation amortizes centralized coordination across a batch of IDs.

# **6. Approach 4: UUID Version 4 (Random UUID)**

Now let's explore a completely different approach.

Instead of coordinating with a database or Redis, what if every server independently generates a sufficiently random ID?

This is the idea behind UUIDv4.

UUID stands for:

**Universally Unique Identifier.**

A UUID is 128 bits long.

Example:

```
550e8400-e29b-41d4-a716-446655440000
```

The important part is that UUIDv4 does not need any centralized ID generator.

## **6.1 How does UUIDv4 work?**

Imagine three application servers:

```
Server A → Generate random UUID
Server B → Generate random UUID
Server C → Generate random UUID
```

They independently generate IDs.

```
Server A:
a8f1c220-...

Server B:
74d9b630-...

Server C:
09c2e814-...
```

There is no communication between them.

The generated values are sufficiently random that the probability of collision is astronomically small when generated correctly.

## **6.2 Why 128 bits?**

A 128-bit value provides:

\\[ 2^{128} \\]

possible combinations.

However, UUIDv4 reserves some bits for its version and variant.

Therefore, it has **122 random bits**.

Number of possible random values:

\\[ 2^{122} \approx 5.3 \times 10^{36} \\]

That's an enormous number.

## **6.3 But what if two servers generate the same random ID?**

This is where probability comes into the picture.

UUIDv4 cannot mathematically guarantee that a collision is impossible.

Instead, it makes the probability negligibly small.

The approximate birthday-bound formula is:

\\[ P(\text{collision}) \approx \frac{n(n-1)}{2 \times 2^k} \\]

Where:

- \\(n\\) = number of generated IDs.
- \\(k\\) = number of random bits.

For UUIDv4:

\\[ k=122 \\]

For example, suppose you generate one trillion UUIDv4 values:

\\[ n=10^{12} \\]

The approximate probability of at least one collision is:

\\[ P \approx 9.4 \times 10^{-14} \\]

That's extremely small.

## **6.4 Why is UUIDv4 attractive in distributed systems?**

Consider this architecture:

```
             Distributed Application

       +-----------+-----------+-----------+
       |           |           |           |
    Server A    Server B    Server C    Server D
       |           |           |           |
    UUIDv4      UUIDv4      UUIDv4      UUIDv4
       |           |           |           |
       +-----------+-----------+-----------+
```

Each application server generates IDs independently.

No database calls.

No Redis calls.

No worker ID assignment.

No clock synchronization requirement.

That's a significant advantage.

## **6.5 The major disadvantage: Random index insertion**

Suppose you store UUIDs as primary keys in a relational database.

```
ID
------------------------------------
a8f1...
74d9...
09c2...
d5a1...
```

Because IDs are random, new values can be inserted at different locations in a B-tree index.

For a large table, this can lead to:

- Poorer index locality.
- More page splits.
- Increased page writes.
- Less effective caching in some workloads.

This is one reason time-ordered IDs are attractive.

We'll discuss UUIDv7 shortly.

### **Advantages**

- No coordination between servers.
- Extremely low generation latency.
- Very simple implementation.
- Excellent scalability.
- Good for independently deployed services.

### **Disadvantages**

- 128-bit identifier.
- Random ordering.
- Potential index fragmentation.
- Collision probability is tiny but not mathematically zero.

### **When should you use it?**

When you prioritize decentralized generation and don't need IDs to reflect creation time.

UUIDv4 is especially useful for IDs generated independently by many services or clients.

# **7. Approach 5: UUID Version 1 and Version 6 (Time-Based UUIDs)**

UUIDv4 uses random data.

UUIDv1 and UUIDv6 use time-related data, along with additional information to distinguish generators.

The fundamental idea is:

**Combine timestamp + machine identity + clock sequence.**

## **7.1 UUIDv1**

UUIDv1 contains:

| Field               | Bits    | Purpose                                                |
| ------------------- | ------- | ------------------------------------------------------ |
| Timestamp           | 60      | Time in 100-nanosecond intervals                       |
| Clock sequence      | 14      | Helps handle clock rollback and node changes           |
| Node ID             | 48      | Usually a MAC address or randomly generated node value |
| Version and variant | 6       | UUID format information                                |
| **Total**           | **128** |                                                        |

The 60-bit timestamp uses 100-nanosecond intervals since October 15, 1582.

Let's simplify the concept.

Suppose we have:

```
Server A:
Timestamp = 1000
Node ID = A

Server B:
Timestamp = 1000
Node ID = B
```

Although both servers generate an ID at the same timestamp, their node identifiers distinguish them.

Even when one machine generates multiple UUIDs at very similar timestamps, the clock sequence and timestamp handling help preserve uniqueness.

The clock sequence is particularly important when the system clock moves backwards.

## **7.2 UUIDv6**

UUIDv6 is essentially a more database-friendly arrangement of the time-based UUID structure.

The issue with UUIDv1 is that its timestamp fields are split and arranged in a way that does not provide ideal chronological lexical sorting.

UUIDv6 rearranges the timestamp bits so that the most significant time bits come first.

Conceptually:

```
UUIDv1:
Timestamp fields arranged in legacy order

UUIDv6:
Timestamp fields arranged in chronological order
```

Both are time-based, but UUIDv6 is better suited to sorting by timestamp.

## **7.3 Limitations**

UUIDv1 has some disadvantages:

- Can expose a machine's MAC address.
- Reveals time-related information.
- More complicated than random UUID generation.
- Requires correct clock-sequence and node management.

UUIDv6 improves timestamp ordering but retains the time-based UUID approach.

For new designs, UUIDv7 is generally a more attractive option. RFC 9562 recommends UUIDv7 over UUIDv1 and UUIDv6 when possible.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.rfc-editor.org\&sz=32)

RFC Editor+1



# **8. Approach 6: Twitter Snowflake (Extremely Important)**

Now we reach one of the most important distributed ID-generation algorithms for System Design interviews.

If you remember only one structured distributed ID-generation algorithm, remember **Snowflake**.

The central idea is:

> Combine timestamp + machine ID + sequence number to generate unique IDs independently on different machines.

Unlike UUIDv4, Snowflake can produce compact, 64-bit, roughly time-ordered IDs.

Twitter introduced Snowflake to generate unique 64-bit identifiers in a distributed environment without depending on a central counter for every ID.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://blog.x.com\&sz=32)

X Blog+1



## **8.1 Why do we need Snowflake?**

Let's revisit the problems.

**Database auto-increment:**

```
Central database required.
```

**Redis INCR:**

```
Central Redis required.
```

**UUIDv4:**

```
Decentralized generation.
But IDs are 128-bit random values.
No time ordering.
```

Can we get the best of both worlds?

- No central request for every ID.
- Small numeric identifiers.
- Multiple servers can generate IDs independently.
- IDs are roughly time ordered.
- High throughput.

Snowflake addresses these requirements.

## **8.2 The Snowflake ID structure**

A common Twitter-style Snowflake uses 64 bits:

```
+----------+-------------------+----------------+----------------+
| 1 bit    | 41 bits           | 10 bits        | 12 bits        |
+----------+-------------------+----------------+----------------+
| Sign     | Timestamp         | Worker ID      | Sequence       |
+----------+-------------------+----------------+----------------+
```

Let's understand each component.

| Component | Bits   | Purpose                                                                |
| --------- | ------ | ---------------------------------------------------------------------- |
| Sign bit  | 1      | Kept at zero for a positive signed 64-bit integer                      |
| Timestamp | 41     | Milliseconds since a custom epoch                                      |
| Worker ID | 10     | Identifies the generating worker                                       |
| Sequence  | 12     | Distinguishes IDs generated by the same worker in the same millisecond |
| **Total** | **64** |                                                                        |

The 41 + 10 + 12 layout is the important part to remember.

### **A. Timestamp: 41 bits**

Why do we store the timestamp?

Because time itself helps make the ID unique.

Suppose:

```
Server A generates an ID at 10:00:00.001

Server B generates an ID at 10:00:00.002
```

The timestamps are different.

If we put the timestamp in the most significant bits, IDs generated later will generally have larger values.

Why use a custom epoch instead of the Unix epoch of 1970?

Because we don't need to represent dates before our system started.

For example, we can choose:

```
Custom epoch = 2025-01-01
```

Then we store:

```
Current timestamp - Custom epoch
```

This saves bits because we store only the elapsed time.

With 41 timestamp bits and millisecond precision:

\\[ 2^{41} \text{ milliseconds} \\]

This gives approximately:

\\[ 69.7 \text{ years} \\]

So the configured timestamp field lasts approximately 69.7 years from the chosen epoch.

### **B. Worker ID: 10 bits**

Why do we need a worker ID?

Because multiple machines may generate IDs during the same millisecond.

Imagine:

```
Server A:
Timestamp = 1000

Server B:
Timestamp = 1000
```

The timestamp alone cannot distinguish them.

We assign a different worker ID to each generator.

```
Server A → Worker ID 10

Server B → Worker ID 20

Server C → Worker ID 30
```

Now the combination of timestamp and worker ID distinguishes their generation streams.

With 10 bits:

\\[ 2^{10} = 1024 \\]

We can assign up to 1,024 distinct worker identifiers.

A common implementation divides those 10 bits into:

```
5 bits → Datacenter ID
5 bits → Worker ID
```

This gives:

```
32 datacenters × 32 workers = 1024 combinations
```

This division is an implementation choice, not a requirement of Snowflake.

**Important: How do we assign worker IDs?**

This is an operationally important part of the algorithm.

Options include:

- Static configuration.
- ZooKeeper.
- etcd.
- Database-backed worker registration.
- Kubernetes StatefulSets with controlled identity assignment.

The critical requirement is:

**Two active ID generators must not accidentally use the same worker ID in the same ID space.**

A worker ID allocator must also prevent unsafe reuse of an ID while an old generator could still be operating.

### **C. Sequence: 12 bits**

Now imagine Server A generates multiple IDs within the same millisecond.

```
Timestamp = 1000
Worker ID = 10
```

It needs to generate:

```
Order 1
Order 2
Order 3
Order 4
```

All four IDs have the same timestamp and worker ID.

How do we distinguish them?

That's the responsibility of the sequence number.

The sequence number starts at zero and increments for each ID generated within that millisecond.

```
Timestamp   Worker ID   Sequence

1000        10          0
1000        10          1
1000        10          2
1000        10          3
1000        10          4
```

With 12 bits:

\\[ 2^{12} = 4096 \\]

So a worker can generate up to 4,096 IDs per millisecond.

That's theoretically:

\\[ 4096 \times 1000 = 4,096,000 \\]

Or approximately **4.096 million IDs per second per worker**, subject to implementation and hardware limitations.

## **8.3 Now understand the complete algorithm**

Let's walk through actual ID generation.

Suppose we have:

```
Custom epoch = E

Current timestamp relative to E = 1000 ms
Worker ID = 17
Sequence = 0
```

We want to generate five IDs.

### **First ID**

```
Timestamp = 1000
Worker ID = 17
Sequence = 0
```

We construct:

```
0000000000000000000000000000000000000000000000000000000000000000
```

Conceptually, with the actual field values:

```
[Timestamp: 1000][Worker: 17][Sequence: 0]
```

### **Second ID**

Same millisecond.

Increment the sequence:

```
[Timestamp: 1000][Worker: 17][Sequence: 1]
```

### **Third ID**

```
[Timestamp: 1000][Worker: 17][Sequence: 2]
```

### **Fourth ID**

```
[Timestamp: 1000][Worker: 17][Sequence: 3]
```

### **Fifth ID**

```
[Timestamp: 1000][Worker: 17][Sequence: 4]
```

Now suppose the system clock advances to the next millisecond.

```
Timestamp = 1001
Worker ID = 17
Sequence = 0
```

We reset the sequence to zero.

Why?

Because the timestamp has changed. The new timestamp already distinguishes these IDs from those generated in the previous millisecond.

The pattern is:

```
New millisecond:
    Reset sequence to 0.

Same millisecond:
    Increment sequence.
```

That's essentially the entire Snowflake algorithm!

## **8.4 How do we combine the fields into one number?**

We use bit shifting.

The structure is:

```
[ Sign | Timestamp | Worker ID | Sequence ]
```

The sequence uses the last 12 bits.

The worker ID sits immediately before it.

The timestamp sits before both.

Therefore:

```
long id =
      (timestamp << 22)
    | (workerId << 12)
    | sequence;
```

Why 22?

Because:

```
12 bits → Sequence

10 bits → Worker ID

Total lower bits = 12 + 10 = 22
```

Why 12?

Because only 12 bits are reserved for sequence.

The OR operation combines the non-overlapping fields.

### **Let's calculate a real example.**

Assume:

```
Timestamp = 1000
Worker ID = 17
Sequence = 42
```

Our formula:

\\[ ID = (1000 \ll 22) \\;|\\; (17 \ll 12) \\;|\\;42 \\]

The result is:

```
ID = 4,194,373,674
```

The actual timestamp is normally the elapsed milliseconds since the configured epoch, not the absolute Unix timestamp.

## **8.5 Snowflake generation algorithm in Java**

Here's a simplified implementation of the core algorithm.

```
public class SnowflakeIdGenerator {

    // Example: 41 timestamp bits, 10 worker bits, 12 sequence bits.

    private static final long WORKER_ID_BITS = 10;
    private static final long SEQUENCE_BITS = 12;

    private static final long MAX_WORKER_ID =
            (1L << WORKER_ID_BITS) - 1;

    private static final long MAX_SEQUENCE =
            (1L << SEQUENCE_BITS) - 1;

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    private static final long TIMESTAMP_SHIFT =
            SEQUENCE_BITS + WORKER_ID_BITS;

    // Example epoch. Production systems should choose
    // and permanently document their own epoch.
    private static final long EPOCH = 1735689600000L;

    private final long workerId;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator(long workerId) {

        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    "Invalid worker ID"
            );
        }

        this.workerId = workerId;
    }

    public synchronized long nextId() {

        long timestamp = System.currentTimeMillis();

        // Case 1: Clock moved backwards.
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException(
                    "Clock moved backwards"
            );
        }

        // Case 2: Same millisecond.
        if (timestamp == lastTimestamp) {

            sequence = (sequence + 1) & MAX_SEQUENCE;

            // Sequence overflow: wait for next millisecond.
            if (sequence == 0) {
                timestamp = waitUntilNextMillis(lastTimestamp);
            }

        } else {

            // Case 3: New millisecond.
            sequence = 0;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long waitUntilNextMillis(long previousTimestamp) {

        long timestamp = System.currentTimeMillis();

        while (timestamp <= previousTimestamp) {
            timestamp = System.currentTimeMillis();
        }

        return timestamp;
    }
}
```

This demonstrates the core state machine. A production implementation needs additional decisions around interruption, clock rollback, persistence across restart, worker registration, and monitoring.

## **8.6 Important edge case: What happens when the sequence overflows?**

We have 12 sequence bits.

Maximum sequence:

```
4095
```

Suppose a particular worker generates 4,096 IDs in the same millisecond.

What happens when it needs the 4,097th ID?

We cannot reuse sequence zero with the same timestamp and worker ID.

That would produce a duplicate.

The solution:

**Wait until the next millisecond.**

```
Timestamp = 1000

Sequence = 0
Sequence = 1
...
Sequence = 4095

Sequence exhausted!

Wait for timestamp = 1001

Reset sequence = 0
Continue generation.
```

This preserves uniqueness.

An implementation could also return a capacity-exceeded error rather than waiting.

## **8.7 The biggest Snowflake problem: Clock rollback**

This is arguably the most important failure scenario to understand.

Imagine:

```
Last generated timestamp = 1000

Now system clock = 999
```

Why can this happen?

- NTP clock correction.
- Manual clock changes.
- Virtual machine clock adjustments.
- Host clock synchronization problems.
- Machine restoration or migration.

Consider what happens if we generate another ID using timestamp 999.

We might revisit a previously used timestamp + worker ID + sequence combination.

That can cause duplicate IDs.

Therefore, most Snowflake implementations reject generation when:

```
currentTimestamp < lastTimestamp
```

Some implementations wait for the clock to catch up when the rollback is small.

Others use a logical timestamp or other recovery mechanisms.

But the important principle is:

**Never silently continue generating IDs using a timestamp that has moved backwards.**

Also, persisting the previous timestamp or safely assigning a new worker identity after restart may be necessary. Otherwise, a restarted process could forget previously used state.

## **8.8 Does Snowflake guarantee globally chronological ordering?**

Not necessarily.

It provides approximate time ordering.

Suppose:

```
Server A clock = 10:00:00.001
Server B clock = 10:00:00.003
```

Server B's clock is ahead by 2 milliseconds.

Server B can generate a numerically larger ID even if its operation happened later in real time only according to its local clock.

Snowflake generally gives:

- Increasing IDs within one correctly functioning generator.
- IDs that broadly follow timestamps across generators.
- No guaranteed global causal ordering across distributed machines.

This distinction matters enormously in distributed systems.

### **Advantages**

- Compact 64-bit numeric IDs.
- Very high throughput.
- No central request for every ID.
- Roughly time ordered.
- Suitable for distributed generation.

### **Disadvantages**

- Requires unique worker ID assignment.
- Sensitive to clock rollback.
- Requires operational monitoring.
- Timestamp capacity is finite.
- IDs can reveal approximate creation time.

### **When should you use it?**

When you need high-throughput, compact, numeric, roughly time-ordered IDs, particularly for large-scale distributed applications.

# **9. Approach 7: UUID Version 7 (Modern Time-Ordered UUID)**

This is one of the most interesting approaches for modern applications.

We previously saw that UUIDv4 offers decentralized random generation, but random IDs can cause poor database index locality.

UUIDv7 addresses that problem.

The core idea is:

**Timestamp + Randomness.**

Unlike Snowflake, UUIDv7 doesn't need a worker ID for uniqueness.

It uses a large random component.

UUIDv7 is standardized in RFC 9562, published in May 2024.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.rfc-editor.org\&sz=32)

RFC Editor+1



## **9.1 UUIDv7 structure**

UUIDv7 is also 128 bits.

Its structure is:

```
+----------------+---------+---------------+---------+----------------+
| 48 bits        | 4 bits  | 12 bits       | 2 bits  | 62 bits        |
+----------------+---------+---------------+---------+----------------+
| Timestamp      | Version | Random/Counter | Variant | Random/Counter |
+----------------+---------+---------------+---------+----------------+
```

Let's understand it:

| Component      | Bits | Purpose                        |
| -------------- | ---- | ------------------------------ |
| Unix timestamp | 48   | Milliseconds since Unix epoch  |
| Version        | 4    | Identifies UUID version 7      |
| Random A       | 12   | Randomness and/or monotonicity |
| Variant        | 2    | UUID variant                   |
| Random B       | 62   | Randomness and/or monotonicity |

Therefore:

```
48 timestamp bits
+
74 available random/counter bits
+
6 format bits
=
128 bits
```

## **9.2 How does UUIDv7 work?**

Imagine three different servers.

```
Server A
Timestamp = 1000 ms
Random = A1B2...

Server B
Timestamp = 1000 ms
Random = C3D4...

Server C
Timestamp = 1001 ms
Random = E5F6...
```

Their IDs conceptually look like:

```
[1000][A1B2...]
[1000][C3D4...]
[1001][E5F6...]
```

Notice something important.

The timestamp comes first.

Therefore, IDs generated in later milliseconds generally have larger values when compared using their binary or canonical byte representation.

This is the key benefit over UUIDv4.

## **9.3 Why is this beneficial for databases?**

Imagine inserting millions of records into a B-tree index.

With UUIDv4:

```
Random insertion locations
```

With UUIDv7:

```
Mostly increasing insertion locations
```

This can improve index locality and reduce random insertion behavior.

It doesn't mean every distributed insertion is perfectly ordered, but the timestamp prefix provides a much more database-friendly pattern.

## **9.4 Does UUIDv7 guarantee uniqueness?**

No deterministic mathematical guarantee exists when using random bits.

It provides a huge random space.

The RFC also defines approaches for improving monotonicity using:

- Sub-millisecond timestamp fractions.
- Counters.
- Random values.

This helps implementations produce increasing UUIDs within the same timestamp interval.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://datatracker.ietf.org\&sz=32)

Universally Unique IDentifiers (UUIDs)+1



## **9.5 UUIDv4 vs UUIDv7**

| Feature                     | UUIDv4       | UUIDv7         |
| --------------------------- | ------------ | -------------- |
| Size                        | 128 bits     | 128 bits       |
| Timestamp embedded          | No           | Yes            |
| Randomness                  | 122 bits     | Up to 74 bits  |
| Sortable by generation time | No           | Generally yes  |
| Database index locality     | Often poorer | Usually better |
| Machine ID required         | No           | No             |
| Central coordination        | No           | No             |
| Clock dependency            | No           | Yes            |

## **9.6 UUIDv7 vs Snowflake**

This comparison is extremely important.

| Feature                     | UUIDv7                                     | Snowflake                                |
| --------------------------- | ------------------------------------------ | ---------------------------------------- |
| Size                        | 128 bits                                   | 64 bits                                  |
| Timestamp                   | Yes                                        | Yes                                      |
| Random bits                 | Yes                                        | No                                       |
| Worker ID                   | Not required                               | Required                                 |
| Central worker registration | Not required                               | Usually needed                           |
| Clock dependency            | Yes                                        | Yes                                      |
| Per-millisecond generation  | Random/counter capacity                    | 4,096 per worker in the common layout    |
| Database locality           | Good chronological tendency                | Good chronological tendency              |
| Collision handling          | Extremely low random collision probability | Deterministic under generator invariants |

My preference for a new general-purpose distributed application would often be UUIDv7 when 128-bit identifiers are acceptable.

It avoids managing worker IDs and provides timestamp-oriented sorting.

Use Snowflake when 64-bit numeric IDs, compact indexes, or very high controlled per-node generation rates are important.

# **10. Approach 8: ULID (Universally Unique Lexicographically Sortable Identifier)**

ULID is another very useful time-based identifier.

In fact, its fundamental approach is very similar to UUIDv7.

The difference is mainly in its bit allocation and textual representation.

## **10.1 Structure**

ULID is 128 bits:

```
+----------------+----------------------+
| Timestamp      | Randomness           |
+----------------+----------------------+
| 48 bits        | 80 bits              |
+----------------+----------------------+
```

- 48 bits: Unix timestamp in milliseconds.
- 80 bits: Randomness.

Example:

```
01ARZ3NDEKTSV4RRFFQ69G5FAV
```

Unlike UUID, ULID uses Crockford's Base32 encoding.

That's why it uses 26 characters instead of the familiar 36-character UUID representation.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub



## **10.2 How does it work?**

Suppose a server generates an ID at:

```
Timestamp = 1000 ms
Randomness = A1B2...
```

The resulting ID represents:

```
[Timestamp: 1000][Random: A1B2...]
```

Another server generates an ID at the same millisecond:

```
[Timestamp: 1000][Random: C3D4...]
```

A third server generates an ID one millisecond later:

```
[Timestamp: 1001][Random: E5F6...]
```

Because the timestamp occupies the most significant bits, the encoded identifiers can be sorted lexicographically by creation timestamp.

## **10.3 What is Monotonic ULID?**

Here's an interesting feature.

Suppose one application server generates two IDs during the same millisecond.

The default approach generates two independent random components.

Therefore, their relative ordering isn't guaranteed.

A monotonic ULID generator solves this within its generator instance.

Suppose the first random component is:

```
A100
```

The next value can increment it:

```
A101
```

The next:

```
A102
```

And so on.

Conceptually:

```
Timestamp = 1000

ULID 1 → [1000][A100]
ULID 2 → [1000][A101]
ULID 3 → [1000][A102]
ULID 4 → [1000][A103]
```

This gives monotonic ordering within the same generator, provided its state is maintained.

It does not automatically establish global ordering across independent machines.

## **10.4 ULID vs UUIDv7**

| Feature              | ULID                | UUIDv7                                |
| -------------------- | ------------------- | ------------------------------------- |
| Size                 | 128 bits            | 128 bits                              |
| Timestamp            | 48 bits             | 48 bits                               |
| Random bits          | 80 bits             | 74 available bits                     |
| Representation       | 26-character Base32 | 36-character UUID string              |
| Sortability          | Yes                 | Yes                                   |
| Monotonic generation | Supported           | Supported through optional mechanisms |
| Standard             | ULID specification  | RFC 9562                              |

### **When should you use ULID?**

When you want:

- Time-sortable identifiers.
- Text-friendly IDs.
- URL-friendly representations.
- Decentralized generation.
- No worker ID allocation.

ULID is a strong alternative to UUIDv7.

# **11. Approach 9: MongoDB ObjectId**

MongoDB uses a special identifier called ObjectId.

It is another interesting example because it combines:

**Timestamp + Process Randomness + Counter.**

Unlike UUID and Snowflake, ObjectId is only 12 bytes.

That's 96 bits.

## **11.1 Structure**

MongoDB ObjectId:

```
+------------------+------------------+----------------+
| Timestamp        | Random Value     | Counter        |
+------------------+------------------+----------------+
| 4 bytes          | 5 bytes          | 3 bytes        |
+------------------+------------------+----------------+
| 32 bits          | 40 bits          | 24 bits        |
+------------------+------------------+----------------+
```

| Component    | Size         | Purpose                                       |
| ------------ | ------------ | --------------------------------------------- |
| Timestamp    | 4 bytes      | Creation time in Unix seconds                 |
| Random value | 5 bytes      | Random value generated for the client process |
| Counter      | 3 bytes      | Incrementing counter                          |
| **Total**    | **12 bytes** |                                               |

This structure is documented by MongoDB.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.mongodb.com\&sz=32)

MongoDB Docs



## **11.2 How does it work?**

Imagine Server A starts.

It generates a random process-specific value:

```
Process random = AABBCCDDEE
```

It initializes a counter.

Suppose:

```
Counter = 100
```

Now it generates three ObjectIds in the same second.

```
ObjectId 1:
[Timestamp][AABBCCDDEE][100]

ObjectId 2:
[Timestamp][AABBCCDDEE][101]

ObjectId 3:
[Timestamp][AABBCCDDEE][102]
```

Now another server starts.

Its process-specific random value is different:

```
Process random = FF11223344
```

Its IDs look like:

```
[Timestamp][FF11223344][100]
[Timestamp][FF11223344][101]
```

Even though both servers generate IDs in the same second, the random component distinguishes their generation streams with extremely high probability.

## **11.3 Advantages**

- Compact: only 12 bytes.
- Includes creation-time information.
- No central ID generator.
- Simple generation.
- Useful for distributed document databases.

## **11.4 Disadvantages**

- Timestamp precision is only seconds.
- Not strictly globally chronological.
- IDs reveal approximate creation time.
- Process randomness is probabilistic, not a mathematical guarantee of global uniqueness.

### **When should you use it?**

When you're working with MongoDB, ObjectId is usually sufficient as the native document identifier.

You generally don't need to introduce a separate Snowflake-style generator just to generate MongoDB document IDs.

# **12. Approach 10: KSUID (K-Sortable Unique Identifier)**

KSUID was developed by Segment.

It follows the time + randomness approach, but uses a larger identifier.

## **12.1 Structure**

KSUID is 160 bits.

```
+----------------+----------------------+
| Timestamp      | Random Payload       |
+----------------+----------------------+
| 32 bits        | 128 bits             |
+----------------+----------------------+
```

- 32-bit timestamp in seconds.
- 128-bit random payload.

It is encoded using Base62 and represented as a 27-character string.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://uniqueid.tech\&sz=32)

uniqueid.tech



## **12.2 Example**

Conceptually:

```
[Timestamp: 32 bits][Randomness: 128 bits]
```

Two servers generate:

```
Server A → Timestamp 1000 + Random A
Server B → Timestamp 1000 + Random B
```

Both receive different random payloads with overwhelmingly high probability.

Since timestamps occupy the most significant bits, KSUIDs can be sorted by time.

## **12.3 ULID vs KSUID**

| Feature             | ULID         | KSUID    |
| ------------------- | ------------ | -------- |
| Size                | 128 bits     | 160 bits |
| Timestamp precision | Milliseconds | Seconds  |
| Randomness          | 80 bits      | 128 bits |
| Representation      | Base32       | Base62   |
| String length       | 26           | 27       |
| Sortable            | Yes          | Yes      |

### **When should you use KSUID?**

When you want sortable, decentralized identifiers and prefer its encoding and ecosystem.

However, for most new applications, I'd first evaluate UUIDv7 and ULID before introducing KSUID.

# **13. Approach 11: Deterministic UUID (UUIDv3 / UUIDv5)**

This approach is a little different.

All the algorithms we've discussed generate a new unique identifier for a new entity.

What if you want to generate the *same identifier* whenever the same input is provided?

That's where deterministic UUIDs come in.

UUIDv3 and UUIDv5 generate identifiers using:

```
Namespace + Name → Hash → UUID
```

UUIDv3 uses MD5.

UUIDv5 uses SHA-1.

## **13.1 Example**

Suppose you have a user whose canonical identifier is:

```
user@example.com
```

You can combine it with a namespace:

```
Namespace = USER_NAMESPACE

Name = user@example.com
```

Then calculate:

```
UUIDv5(USER_NAMESPACE, "user@example.com")
```

Every service using the same namespace and exactly the same canonical input gets the same UUID.

```
Service A → UUID X

Service B → UUID X

Service C → UUID X
```

No coordination is needed.

## **13.2 Why is this useful?**

Imagine migrating data between two systems.

You want to ensure that the same logical entity receives the same identifier in both systems.

Instead of maintaining a central ID mapping service, you can derive the identifier from a stable input.

Other use cases:

- Deterministic resource identifiers.
- Idempotent processing.
- Reproducible identifiers during data migration.
- Deduplication of logical entities.

## **13.3 Important limitation**

UUIDv5 is not an ordinary unique event ID generator.

Suppose:

```
Payment 1 → customer 101
Payment 2 → customer 101
```

If you use just `customerId=101` as the name, both payments receive the same ID.

That is wrong because they are different payment events.

You need a name representing the entity whose identity you want to derive.

Also, UUIDv5 collision probability is not mathematically zero.

### **When should you use it?**

When you need the same logical input to produce the same identifier, not when you need a fresh identifier for every new event.

# **14. Advanced variation: Sonyflake**

You may encounter Sonyflake while studying Snowflake.

It is a Snowflake-inspired generator that changes the bit allocation.

The commonly documented default layout is:

```
+----------------+----------------+----------------+
| Time           | Sequence       | Machine ID     |
+----------------+----------------+----------------+
| 39 bits        | 8 bits         | 16 bits        |
+----------------+----------------+----------------+
```

The timestamp uses 10-millisecond units instead of 1-millisecond units.

The trade-off:

| Property                        | Snowflake  | Sonyflake |
| ------------------------------- | ---------- | --------- |
| Time bits                       | 41         | 39        |
| Time unit                       | 1 ms       | 10 ms     |
| Machine bits                    | 10         | 16        |
| Sequence bits                   | 12         | 8         |
| Machines                        | 1,024      | 65,536    |
| IDs per time unit per generator | 4,096      | 256       |
| Approximate lifetime            | 69.7 years | 174 years |

Sonyflake supports more machine identities and a longer time horizon, but reduces the per-instance sequence capacity.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://sonyflake.readthedocs.io\&sz=32)

sonyflake.readthedocs.io+1



**The important lesson:** Snowflake is not one fixed bit layout. You can change the allocation depending on your workload.

For example:

- Need more machines? Increase machine bits.
- Need more IDs per millisecond? Increase sequence bits.
- Need a longer timestamp lifetime? Increase timestamp bits or change the time unit.

But you only have a fixed number of bits available.

# **15. An advanced concept: Hybrid Logical Clock (HLC)**

You may encounter Hybrid Logical Clocks in distributed systems.

They are not a standalone replacement for every ID generator, but they are useful when you want an identifier or ordering mechanism that incorporates logical ordering in addition to physical time.

## **15.1 Why do we need it?**

Suppose two services generate events:

```
Service A:
Physical time = 1000

Service B:
Physical time = 999
```

But Service B has already received an event from Service A.

We know that the event generated by A happened before B processed it.

However, B's physical clock says 999, which is earlier than A's 1000.

This is a clock synchronization problem.

## **15.2 How HLC helps**

HLC combines:

```
Physical timestamp + Logical counter
```

Conceptually:

```
HLC = (Physical Time, Logical Counter)
```

Example:

```
First event:
(1000, 0)

Second event in the same physical time:
(1000, 1)

Another event:
(1000, 2)
```

When a service receives an event with a later HLC value, it updates its own logical clock to preserve the ordering relationship.

You can then combine:

```
HLC timestamp + Node ID
```

to construct a distributed identifier.

The node ID distinguishes independent generators with otherwise identical HLC values.

**Important distinction:** HLC is particularly useful for capturing causality-related ordering information. It doesn't automatically make all events globally ordered, nor does an HLC timestamp alone guarantee unique IDs.

# **16. Complete comparison of all algorithms**

Now let's consolidate everything.

This is the table I'd recommend keeping as your primary revision reference.

| Algorithm               | ID size        | Main components                             | Central coordination? | Time sortable?       |
| ----------------------- | -------------- | ------------------------------------------- | --------------------- | -------------------- |
| Database Auto-Increment | Usually 64-bit | Sequence                                    | Yes                   | Yes, within sequence |
| Hi-Lo                   | Usually 64-bit | Allocated group + local counter             | Range allocation      | Not globally         |
| Segment ID              | Usually 64-bit | Allocated range + local counter             | Range allocation      | Not globally         |
| Redis INCR              | Usually 64-bit | Atomic counter                              | Yes                   | Yes, within counter  |
| UUIDv4                  | 128-bit        | Randomness                                  | No                    | No                   |
| UUIDv1                  | 128-bit        | Timestamp + node + clock sequence           | No central allocator  | Not ideally          |
| UUIDv6                  | 128-bit        | Reordered timestamp + node + clock sequence | No central allocator  | Yes                  |
| Snowflake               | 64-bit         | Timestamp + worker + sequence               | Worker allocation     | Approximately        |
| UUIDv7                  | 128-bit        | Timestamp + randomness/counter              | No                    | Yes                  |
| ULID                    | 128-bit        | Timestamp + randomness                      | No                    | Yes                  |
| MongoDB ObjectId        | 96-bit         | Timestamp + process randomness + counter    | No                    | Approximately        |
| KSUID                   | 160-bit        | Timestamp + randomness                      | No                    | Yes                  |
| UUIDv5                  | 128-bit        | Namespace + name hash                       | No                    | No                   |

A few important qualifications:

- "Time sortable" generally means identifiers sort by their encoded timestamps, not that they establish perfect global chronological order.
- Snowflake uniqueness depends on correct worker allocation, clock handling and sequence management.
- Random identifiers provide probabilistic rather than absolute uniqueness.
- Hi-Lo and segment allocation guarantee uniqueness of allocated ranges, not global creation-time ordering.

# **17. Which algorithm should you choose in a real system?**

This is where System Design interviews become interesting.

Let's take different requirements.

## **Scenario 1: Normal backend application**

Suppose you're building:

```
E-commerce application
Users
Products
Orders
Payments
```

Traffic:

```
1,000 - 10,000 requests/second
```

You use PostgreSQL.

Do you need Snowflake?

Probably not.

A PostgreSQL sequence is perfectly reasonable.

Why introduce additional operational complexity when your database can handle the requirement?

**Recommendation:** Database sequence.

## **Scenario 2: Microservices deployed independently**

Imagine:

```
Order Service
Payment Service
Inventory Service
Shipment Service
```

Each service might generate identifiers independently.

You want:

- No central ID generation service.
- No dependency on Redis for every ID.
- Simplicity.
- Portable IDs.
- Reasonable database index locality.

**Recommendation:** UUIDv7.

It offers decentralized generation and time-oriented identifiers.

## **Scenario 3: Extremely high-throughput distributed application**

Imagine a large-scale social network.

```
Millions of posts
Millions of comments
Millions of interactions
```

You want:

- Numeric IDs.
- Compact storage.
- High generation throughput.
- Timestamp information.
- Distributed generation.

**Recommendation:** Snowflake.

You can customize the bit allocation according to the number of machines and generation rate you need.

## **Scenario 4: High throughput but you prefer database-backed allocation**

Imagine:

```
Many application servers
Relational database
High write throughput
Numeric IDs required
```

You don't want a time-based ID algorithm.

**Recommendation:** Segment ID / Hi-Lo.

Allocate blocks and generate identifiers locally.

## **Scenario 5: MongoDB application**

You store documents in MongoDB.

**Recommendation:** MongoDB ObjectId is usually sufficient.

Use a different generator only when your application has specific requirements beyond MongoDB's native identifier.

## **Scenario 6: Idempotent resource creation**

Suppose different services must calculate the same ID for the same logical entity.

**Recommendation:** UUIDv5.

It is deterministic and doesn't require a central allocator.

## **Scenario 7: Externally visible, URL-friendly identifiers**

You want identifiers that are:

- Sortable.
- Human-readable.
- Easy to copy.
- URL friendly.

**Recommendation:** ULID or UUIDv7.

# **18. Important System Design considerations**

Knowing the algorithms is only half the story.

In an actual system design interview, you should also explain the problems surrounding them.

## **18.1 Uniqueness vs Ordering**

These are two different properties.

Suppose we generate:

```
ID A = 1001
ID B = 1002
ID C = 1003
```

The IDs are unique.

But does that necessarily mean A was created before B, and B before C?

Only if the allocator's ordering semantics actually correspond to your creation events.

For example, a Snowflake ID contains a timestamp.

But two machines can have clock skew.

Therefore:

```
Unique ID ≠ Globally ordered event
```

Remember this.

When strict global ordering is required, you generally need some form of coordination or a well-defined ordering mechanism.

Examples:

- A centralized sequence allocator.
- A partition-specific Kafka offset.
- A database commit sequence.
- A consensus-based ordering mechanism.

Even these provide different kinds of ordering guarantees, so choose according to the actual requirement.

## **18.2 Clock synchronization**

Time-based ID generators depend on clocks.

Imagine:

```
Server A clock = 10:00:00.100

Server B clock = 10:00:00.095
```

Even when B creates an ID after A, B's ID may be numerically smaller.

Therefore, a time-based ID does not necessarily establish global chronological order.

For Snowflake, clock rollback can also cause duplicate IDs unless explicitly handled.

For UUIDv7 and ULID, clocks that move backwards can disrupt chronological sorting or monotonic generation.

## **18.3 Worker ID assignment**

For Snowflake, this is especially important.

Suppose:

```
Server A → Worker ID 10

Server B → Worker ID 10
```

Both servers generate an ID in the same millisecond.

Both start with sequence zero.

```
A → [1000][10][0]

B → [1000][10][0]
```

Collision!

Therefore, worker identity management is not optional.

You need a reliable assignment mechanism.

## **18.4 Database storage and indexing**

Consider the difference:

```
UUIDv4:
Randomly distributed IDs.

UUIDv7:
Mostly chronological IDs.

Snowflake:
Mostly chronological numeric IDs.
```

For large relational tables, this can influence:

- B-tree index locality.
- Page splits.
- Cache efficiency.
- Storage overhead.
- Write amplification.

Also remember that a UUID primary key can increase the size of secondary indexes because the primary key is often included in their entries.

## **18.5 ID predictability and security**

Sequential IDs are easy to guess.

Time-based IDs can expose approximate creation times and may reveal information about system activity.

For example:

```
/orders/1001
/orders/1002
/orders/1003
```

An attacker might try other IDs.

Never treat an unguessable-looking ID as authorization.

A UUID is not a substitute for authentication and access-control checks.

For externally accessible resources, consider a separate opaque public identifier or token when the threat model requires it.

## **18.6 ID generator failure**

Every ID generator has a failure model.

| Algorithm         | Important failure to handle                                 |
| ----------------- | ----------------------------------------------------------- |
| Database sequence | Database unavailable                                        |
| Redis INCR        | Redis failover, loss of acknowledged increments             |
| Segment ID        | Allocator unavailable and range exhaustion                  |
| UUIDv4            | Weak or improperly seeded random generator                  |
| Snowflake         | Duplicate worker ID, clock rollback                         |
| UUIDv7            | Clock issues and randomness quality                         |
| ULID              | Clock issues, incorrect monotonic state handling            |
| ObjectId          | Process random collision probability and counter management |

A production generator also needs concurrency safety, monitoring, rollover handling and safe restart behavior.

# **19. The mental model: How to remember every algorithm easily**

Here's the most important memory trick.

Instead of memorizing ten different algorithms, remember the underlying building blocks.

There are essentially five ways of establishing uniqueness.

### **1. Central coordination**

```
Database / Redis

One central counter
     |
     v
Unique sequence values
```

Examples:

- Auto-increment.
- Redis INCR.
- Ticket Server.

### **2. Pre-allocated uniqueness**

```
Central allocator
      |
      +---- Server A → Range A
      |
      +---- Server B → Range B
      |
      +---- Server C → Range C
```

Examples:

- Hi-Lo.
- Segment ID.
- Meituan Leaf Segment Mode.

### **3. Randomness**

```
Random bits → Extremely low collision probability
```

Examples:

- UUIDv4.
- NanoID.

### **4. Time-based uniqueness**

There are two important variations.

**Variation A: Time + Machine + Counter**

```
Timestamp + Worker ID + Sequence
```

Examples:

- Snowflake.
- Sonyflake.

**Variation B: Time + Randomness**

```
Timestamp + Random bits
```

Examples:

- UUIDv7.
- ULID.
- KSUID.
- MongoDB ObjectId (also includes process randomness and counter).

### **5. Deterministic hashing**

```
Namespace + Name → Hash → Identifier
```

Example:

\- UUIDv5.

That's the entire family of approaches in one mental model.

# **20. My recommended learning priority for System Design interviews**

You don't need to memorize every algorithm equally.

For Google, Meta and senior backend System Design interviews, I'd prioritize the following:

| Priority | Algorithm                         | What you should master                                                   |
| -------- | --------------------------------- | ------------------------------------------------------------------------ |
| 1        | Snowflake                         | Bit allocation, generation logic, worker IDs, clock rollback, throughput |
| 2        | UUIDv4                            | Randomness, birthday paradox, collision probability                      |
| 3        | UUIDv7                            | Timestamp + randomness, database locality, comparison with UUIDv4        |
| 4        | Database Sequence / Ticket Server | Centralized generation, bottlenecks, availability                        |
| 5        | Segment / Hi-Lo                   | Range allocation, atomic reservation, crash handling                     |
| 6        | ULID                              | Timestamp + randomness, monotonic generation, comparison with UUIDv7     |
| 7        | MongoDB ObjectId                  | Timestamp + process randomness + counter                                 |
| 8        | UUIDv1 / UUIDv6                   | Time-based UUID internals and historical differences                     |
| 9        | KSUID / Sonyflake                 | Variations and bit-allocation trade-offs                                 |
| 10       | UUIDv5                            | Deterministic identity generation                                        |

## **Final recommendation: What I would actually choose**

| Requirement                                  | My first choice   |
| -------------------------------------------- | ----------------- |
| Simple relational application                | Database sequence |
| Distributed application, general-purpose IDs | UUIDv7            |
| Very high throughput, compact 64-bit IDs     | Snowflake         |
| Numeric IDs with database-backed allocation  | Segment ID        |
| Random, decentralized identifiers            | UUIDv4            |
| Human-friendly sortable identifiers          | ULID              |
| MongoDB document IDs                         | ObjectId          |
| Deterministic identifiers                    | UUIDv5            |

**One final insight:** There is no universally best distributed ID-generation algorithm.

The real engineering decision is balancing:

\\[ \text{Uniqueness} + \text{Throughput} + \text{Availability} \\]

against:

\\[ \text{Coordination} + \text{Complexity} + \text{Storage} \\]

Once you understand that trade-off, choosing an ID-generation algorithm in almost any System Design problem becomes much easier.

## **Further reading: Primary references**

These are useful references for understanding the actual implementations and specifications.

1. [Twitter — Announcing Snowflake](https://blog.x.com/engineering/en_us/a/2010/announcing-snowflake?source=post_page---------------------------\&utm_source=chatgpt.com) — Original motivation and architecture.
2. [RFC 9562 — UUID specification](https://www.rfc-editor.org/rfc/rfc9562.html?utm_source=chatgpt.com) — Official specifications for UUIDv1, v4, v6 and v7.
3. [Meituan Leaf — Distributed ID Generator](https://github.com/Meituan-Dianping/Leaf?utm_source=chatgpt.com) — Real-world Segment and Snowflake implementations.
4. [ULID specification](https://github.com/ulid/spec?utm_source=chatgpt.com) — Detailed timestamp, randomness and monotonicity implementation.
5. [MongoDB ObjectId documentation](https://www.mongodb.com/docs/v7.0/reference/method/objectid/?utm_source=chatgpt.com) — Official ObjectId structure.
6. [Sonyflake implementation](https://github.com/sony/sonyflake/blob/master/README.md?utm_source=chatgpt.com) — Snowflake-inspired alternative.

**My suggested next step:** Build a complete Snowflake ID generator in Java, including thread safety, worker ID assignment, sequence overflow, clock rollback handling, restart safety, and concurrent stress tests. That exercise will make the core distributed ID-generation concepts stick.































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------





















For a **URL Shortener (like Bitly or TinyURL)**, the problem becomes slightly different.

We don't just need a unique ID. We need a **unique ID that is also very short**, because that ID becomes part of the shortened URL.

For example:

```
Original URL:
https://www.amazon.com/electronics/laptops/apple-macbook-pro-16-inch

Short URL:
https://sho.rt/aB91xK
```

Here:

```
Domain    = sho.rt
Short ID  = aB91xK
```

Our main challenge is generating `aB91xK` efficiently, ensuring it is unique across all application servers.

Let's build the solution from scratch.

# **1. First understand why we need Base62 encoding**

Suppose we generate a normal numeric ID:

```
1000000001
1000000002
1000000003
```

These IDs are unique, but they are relatively long.

Can we represent the same numbers using fewer characters?

Yes. Use **Base62 encoding**.

Base62 uses 62 characters:

```
0123456789
abcdefghijklmnopqrstuvwxyz
ABCDEFGHIJKLMNOPQRSTUVWXYZ
```

So:

```
Base10 → 10 possible digits (0–9)

Base62 → 62 possible characters
```

This means we can represent much larger numbers using fewer characters.

## **1.1 How many IDs can we represent?**

This is the most important calculation.

With Base62:

| Length        | Possible combinations |
| ------------- | --------------------- |
| 1 character   | 62                    |
| 2 characters  | 3,844                 |
| 3 characters  | 238,328               |
| 4 characters  | 14.7 million          |
| 5 characters  | 916 million           |
| 6 characters  | 56.8 billion          |
| 7 characters  | 3.52 trillion         |
| 8 characters  | 218.3 trillion        |
| 9 characters  | 13.54 quadrillion     |
| 10 characters | 839 quadrillion       |

Formula:

\\[ \text{Possible IDs} = 62^L \\]

Where \\(L\\) is the number of characters.

For example:

\\[ 62^6 = 56,800,235,584 \\]

So a six-character Base62 string can represent approximately **56.8 billion unique IDs**.

That's quite a lot!

**Our target could be a 6–8 character short code.**

# **2. Solution 1: Centralized Counter + Base62**

Let's start with the simplest implementation.

Imagine we have a database sequence:

```
1
2
3
4
5
...
```

Whenever someone submits a long URL, we get a new numeric ID and convert it to Base62.

Architecture:

```
                  User
                    |
                    v
              URL Shortener
                    |
                    v
              ID Generator
                    |
                    v
                Database
                    |
              Generate ID
                    |
                    v
                 Base62
                    |
                    v
              Short URL
```

Let's see an example.

Suppose our database sequence generates:

```
ID = 125
```

Convert 125 from Base10 into Base62.

Using the alphabet:

```
0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ
```

We calculate:

\\[ 125 = 2 \times 62 + 1 \\]

Therefore:

```
Base62(125) = 21
```

Our shortened URL becomes:

```
https://sho.rt/21
```

Another request:

```
ID = 126

Base62(126) = 22
```

Result:

```
https://sho.rt/22
```

The mapping table looks like:

| Numeric ID | Base62 | Short URL |
| ---------- | ------ | --------- |
| 1          | 1      | sho.rt/1  |
| 2          | 2      | sho.rt/2  |
| 10         | a      | sho.rt/a  |
| 61         | Z      | sho.rt/Z  |
| 62         | 10     | sho.rt/10 |
| 125        | 21     | sho.rt/21 |

This is deterministic.

Given a numeric ID, its Base62 representation is always the same.

### **Java implementation**

```
public class Base62Encoder {

    private static final String ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final int BASE = 62;

    public static String encode(long number) {

        if (number < 0) {
            throw new IllegalArgumentException(
                    "Number must be non-negative"
            );
        }

        if (number == 0) {
            return "0";
        }

        StringBuilder result = new StringBuilder();

        while (number > 0) {

            int remainder = (int) (number % BASE);

            result.append(ALPHABET.charAt(remainder));

            number = number / BASE;
        }

        return result.reverse().toString();
    }

    public static long decode(String encoded) {

        long result = 0;

        for (char c : encoded.toCharArray()) {

            int value = ALPHABET.indexOf(c);

            if (value < 0) {
                throw new IllegalArgumentException(
                        "Invalid Base62 character"
                );
            }

            result = Math.addExact(
                    Math.multiplyExact(result, BASE),
                    value
            );
        }

        return result;
    }

    public static void main(String[] args) {

        String encoded = encode(125);

        System.out.println(encoded); // 21

        long decoded = decode(encoded);

        System.out.println(decoded); // 125
    }
}
```

Notice that:

```
Base62 encoding = Reversible

Base62 decoding = Original numeric ID
```

We don't need to store both representations.

We can decode the short code to get the original numeric ID and retrieve the corresponding URL.

### **But there's a problem!**

Where are we generating the numeric ID?

Using a centralized database sequence means every application server depends on the same database.

```
Server A ───┐
Server B ───┼──── Database Sequence
Server C ───┘
```

This could become a bottleneck.

Let's solve that.

# **3. Solution 2: Distributed Range Allocation + Base62**

This is one of my preferred approaches when we specifically need **short numeric-derived IDs**.

We combine two algorithms we already discussed:

1. Segment ID / Hi-Lo.
2. Base62 encoding.

The architecture:

```
                     ID Allocator
                     (Database)
                          |
                Allocates ID ranges
                          |
             +------------+------------+
             |            |            |
          Server A     Server B     Server C
             |            |            |
          1-1000       1001-2000   2001-3000
             |            |            |
             +------------+------------+
                          |
                   Local ID Generator
                          |
                       Base62
                          |
                   Unique Short Code
```

## **3.1 Step-by-step example**

Suppose our ID allocator uses a block size of 1,000.

Server A requests a range.

```
Server A → 1 to 1000
```

Server B requests another range.

```
Server B → 1001 to 2000
```

Server C requests another range.

```
Server C → 2001 to 3000
```

Now each server generates IDs independently.

Suppose Server A generates:

```
ID = 125
```

Base62 converts it:

```
125 → 21
```

Server B generates:

```
ID = 1125
```

Its Base62 value will be different.

No collisions are possible as long as allocated ranges never overlap and each range is used correctly.

## **3.2 What happens if Server A crashes?**

Suppose Server A has generated:

```
1 to 200
```

But it crashes before using:

```
201 to 1000
```

We abandon the unused range.

We do not give that range to another server while Server A might still be able to use it.

Gaps are completely acceptable.

For example:

```
Server A: 1 - 1000
Server B: 1001 - 2000

Server A crashes after ID 200.

Server D gets: 2001 - 3000
```

The unused IDs are never reused.

## **3.3 Why is this attractive?**

Because:

```
ID allocation → Occasional database operation

ID generation → Local operation

Base62 encoding → Local CPU operation
```

We avoid a database call for every shortened URL.

### **But what about having multiple regions?**

You can allocate disjoint ranges to different regions, or have an allocator that safely reserves ranges for individual generator instances.

For example:

```
Region A → 1 - 1,000,000

Region B → 1,000,001 - 2,000,000

Region C → 2,000,001 - 3,000,000
```

Each region can allocate smaller local blocks within its reserved space.

The key is that the entire reservation hierarchy must guarantee non-overlapping allocations.

# **4. Solution 3: Snowflake ID + Base62**

Another option is to combine Snowflake with Base62.

Remember Snowflake:

```
64-bit ID =
Timestamp + Worker ID + Sequence
```

Each application server independently generates a numeric ID.

Then:

```
Snowflake ID → Base62 → Short Code
```

Architecture:

```
                 URL Shortener
                       |
          +------------+------------+
          |            |            |
       Server A     Server B     Server C
          |            |            |
       Snowflake    Snowflake    Snowflake
          |            |            |
       Numeric ID   Numeric ID   Numeric ID
          |            |            |
       Base62        Base62       Base62
          |            |            |
       Short Code   Short Code   Short Code
```

## **4.1 Example**

Suppose the generated Snowflake IDs are:

```
Server A → 4194373674
Server B → 4194373675
Server C → 4194373676
```

Convert each to Base62:

```
4194373674 → Base62 representation
4194373675 → Base62 representation
4194373676 → Base62 representation
```

Each produces a different identifier.

### **But there's an important trade-off!**

A typical 64-bit Snowflake ID needs up to **11 Base62 characters**.

Why?

Because:

\\[ 62^{10} < 2^{64} < 62^{11} \\]

Therefore, if your requirement is a maximum of 6–8 characters, a regular 64-bit Snowflake ID may be too long.

You can customize the Snowflake bit allocation and the lifetime of your ID namespace, but that comes with trade-offs.

Also, you need worker ID allocation and clock rollback handling.

**My view:** Snowflake is excellent when you need 64-bit numeric IDs and high-throughput generation. However, it isn't automatically the best choice when the primary objective is minimizing the URL code length.

# **5. Solution 4: Generate Random Base62 Strings**

Now let's look at a very different approach.

What if we don't generate a numeric ID at all?

What if we directly generate a random string?

For example:

```
aB91xK
Xz72pQ
mN83vA
```

Architecture:

```
             URL Shortener
                   |
             Random Generator
                   |
             Generate 8 chars
                   |
             Check uniqueness
                   |
               Database
                   |
                 Save
```

No central counter.

No range allocation.

No Snowflake.

## **5.1 How does this work?**

Suppose we want six-character IDs.

Each position can contain one of 62 characters.

Therefore:

\\[ 62^6 = 56.8 \text{ billion combinations} \\]

We generate:

```
aB91xK
```

Then check whether it already exists in our database.

If it doesn't:

```
INSERT short_code, original_url
```

If it does:

```
Generate another random code.
Retry.
```

### **Java example**

```
public class RandomShortCodeGenerator {

    private static final String ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int LENGTH = 8;

    public static String generate() {

        StringBuilder code = new StringBuilder(LENGTH);

        for (int i = 0; i < LENGTH; i++) {

            int index = RANDOM.nextInt(ALPHABET.length());

            code.append(ALPHABET.charAt(index));
        }

        return code.toString();
    }
}
```

We use `SecureRandom` here because publicly visible short codes should not be unnecessarily predictable.

## **5.2 But how do we guarantee uniqueness?**

This is critical.

Suppose two servers independently generate the same ID:

```
Server A → aB91xK

Server B → aB91xK
```

Both check the database at almost the same time.

Both see:

```
Code doesn't exist.
```

Both attempt to insert.

Now what?

We need a **unique constraint on short_code in the database**.

```
CREATE TABLE urls (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    short_code VARCHAR(16) NOT NULL UNIQUE,
    original_url TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

The database guarantees that only one insert succeeds.

The other server receives a unique-constraint violation.

It generates another code and retries.

This is important:

**Never rely only on checking whether a code exists before inserting it. The unique database constraint is what protects against concurrent collisions.**

## **5.3 What about collision probability?**

This is where the birthday paradox matters.

For six characters:

\\[ M = 62^6 = 56.8 \text{ billion} \\]

The probability of at least one collision after generating \\(n\\) independent random IDs is approximately:

\\[ P \approx 1-e^{-\frac{n(n-1)}{2M}} \\]

For small probabilities:

\\[ P \approx \frac{n(n-1)}{2M} \\]

For example:

| Generated IDs | Approximate probability of at least one collision with 6 characters |
| ------------- | ------------------------------------------------------------------- |
| 10,000        | 0.088%                                                              |
| 100,000       | 8.4%                                                                |
| 1 million     | 99.98%                                                              |

This doesn't mean your application will fail. The database unique constraint and retry mechanism resolve collisions.

But as the namespace fills, retries become more frequent.

For this reason, choose code length based on expected volume.

For example, eight characters provide:

\\[ 62^8 = 218,340,105,584,896 \\]

That gives a much larger namespace.

### **Advantages**

- No central ID allocator.
- No worker ID management.
- Easy to scale across multiple servers and regions.
- IDs are difficult to predict when using secure randomness.
- Simple implementation.

### **Disadvantages**

- Collision checks and retries are required.
- Needs a unique constraint.
- Collision/retry frequency increases as the namespace fills.
- Doesn't provide chronological ordering.

# **6. Comparing the four approaches**

Now let's consolidate our options.

| Property               | Counter + Base62            | Segment + Base62        | Snowflake + Base62              | Random Base62                  |
| ---------------------- | --------------------------- | ----------------------- | ------------------------------- | ------------------------------ |
| Unique IDs             | Yes                         | Yes                     | Yes, with correct configuration | With DB constraint and retries |
| Code length            | Variable                    | Variable                | Up to 11 chars for 64-bit IDs   | Configurable                   |
| Central coordination   | Every ID                    | Per range               | Worker assignment               | No                             |
| Generation latency     | DB/network                  | Local                   | Local                           | Local                          |
| Time ordering          | Increasing allocation order | Not globally guaranteed | Approximate                     | No                             |
| Collision retries      | No, with safe allocator     | No, with safe allocator | No, with safe configuration     | Yes                            |
| Operational complexity | Low                         | Medium                  | High                            | Low–medium                     |

# **7. What would I actually choose for a URL Shortener?**

This is the most important part.

It depends on the scale and requirements.

I would evaluate two primary designs.

## **Option A: Random Base62 (8–10 characters)**

For a typical URL shortener:

```
                         URL Shortener
                               |
                        Load Balancer
                               |
                    +----------+----------+
                    |          |          |
                 Server A   Server B   Server C
                    |          |          |
                    +----------+----------+
                               |
                      Random Base62 Generator
                               |
                        Generate Code
                               |
                         Database INSERT
                               |
                        UNIQUE short_code
                               |
                       Store URL Mapping
```

Why would I choose this?

- No dependency on a centralized ID generator.
- Easy to scale horizontally.
- No worker assignment.
- Relatively difficult-to-guess IDs.
- Eight-character codes provide a large namespace.

This is a very reasonable design for a modern URL shortener.

I'd start with eight characters and monitor collision retries. Increase the length as usage grows or based on the namespace capacity you want to reserve.

## **Option B: Segment ID + Base62 (6–8 characters)**

For a high-throughput system that wants compact IDs:

```
                        URL Shortener
                              |
                     ID Generator Library
                              |
                     Local ID Allocation
                              |
                    Segment / Range Manager
                              |
                       Base62 Encoding
                              |
                       Unique Short Code
```

Why would I choose this?

- Guaranteed non-overlapping numeric IDs.
- No random collision retries.
- Very efficient generation.
- Numeric IDs can be encoded into compact strings.

This is particularly attractive when we want strict uniqueness and control over a limited short-code namespace.

One important detail: if you promise exactly seven characters, your numeric allocator must never exceed the seven-character namespace capacity of \\(62^7\\). You need a plan for expansion.

# **8. Complete URL Shortener flow**

Let's connect ID generation with the actual application.

Suppose a user submits:

```
POST /api/shorten
Content-Type: application/json
```

Request:

```
{
  "url": "https://example.com/products/laptop"
}
```

### **Step 1: Generate a short code**

Using random Base62:

```
shortCode = aB91xK72
```

### **Step 2: Store the mapping**

```
INSERT INTO urls (short_code, original_url)
VALUES (
    'aB91xK72',
    'https://example.com/products/laptop'
);
```

### **Step 3: Return the short URL**

```
{
  "shortUrl": "https://sho.rt/aB91xK72"
}
```

### **Step 4: User opens the short URL**

```
GET /aB91xK72
```

The redirect service executes:

```
SELECT original_url
FROM urls
WHERE short_code = 'aB91xK72';
```

It retrieves:

```
https://example.com/products/laptop
```

Then returns:

```
HTTP/1.1 302 Found
Location: https://example.com/products/laptop
```

For frequently accessed URLs, you'd normally put Redis in front of the database:

```
                  GET /aB91xK72
                         |
                         v
                       Redis
                         |
                +--------+--------+
                |                 |
              Cache Hit         Cache Miss
                |                 |
                |                 v
                |              Database
                |                 |
                +--------+--------+
                         |
                         v
                     Redirect
```

Notice that Redis is being used as a **URL mapping cache**, not necessarily as the ID generator.

These are two separate responsibilities.

# **9. Important design decisions people often miss**

### **A. Should we use a hash of the original URL?**

A common idea is:

```
Original URL → SHA-256 → Base62 → Short Code
```

But there are problems.

Different URLs can produce the same shortened code when the hash is truncated.

Also, two requests for the same URL would always produce the same ID, which might not be the intended product behavior.

Hashing can be useful when you explicitly want deterministic deduplication, but I wouldn't make it the default ID-generation strategy.

### **B. Should short codes be sequential?**

Consider:

```
sho.rt/100
sho.rt/101
sho.rt/102
sho.rt/103
```

Users can easily enumerate them.

An attacker might discover other people's shortened URLs.

Possible solutions:

- Random Base62 identifiers.
- Obfuscation/permutation of sequential IDs.
- Separate internal numeric IDs from public short codes.

Remember that unpredictable identifiers are not a substitute for authorization checks.

### **C. Should the code be exactly six characters?**

Not necessarily.

You need to determine the namespace capacity.

For example:

```
6 characters → 56.8 billion combinations
7 characters → 3.52 trillion combinations
8 characters → 218.3 trillion combinations
```

You should also reserve capacity for future growth.

### **D. Can two different original URLs have the same short code?**

They must not.

Enforce this at the database level:

```
CREATE UNIQUE INDEX idx_urls_short_code
ON urls(short_code);
```

This is non-negotiable, regardless of your generation algorithm.

# **10. Final mental model**

Remember these three options for URL shortener interviews:

```
OPTION 1: Counter + Base62

Database Sequence
       |
       v
    Numeric ID
       |
       v
   Base62 Encode
       |
       v
   Short URL
```

```
OPTION 2: Segment + Base62

Database Allocates Range
       |
       v
Application Generates Locally
       |
       v
    Numeric ID
       |
       v
   Base62 Encode
       |
       v
   Short URL
```

```
OPTION 3: Random Base62

Secure Random Generator
       |
       v
   Short String
       |
       v
 Database UNIQUE Constraint
       |
       +---- Collision → Retry
       |
       v
   Short URL
```

**My recommendation:**

For a typical URL shortener, I'd start with **8-character cryptographically random Base62 codes + a database unique constraint + retry on collision**.

For a very high-throughput URL shortener where you want deterministic allocation without collision retries, I'd choose **Segment ID + Base62**.

The most important insight is that Base62 is only an encoding mechanism. It doesn't generate uniqueness. The underlying counter, allocated range, Snowflake ID or random generator is responsible for producing the unique identifier.

That distinction is fundamental to designing a URL shortener.

