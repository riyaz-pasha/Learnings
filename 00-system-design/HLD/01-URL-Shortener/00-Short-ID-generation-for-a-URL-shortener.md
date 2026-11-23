# **Designing short, unique IDs for a distributed URL shortener**

Your reasoning is mostly correct. The confusion comes from mixing up **three separate things**:

1. **Storage size:** How many bytes does the database use to store an ID?
2. **ID space:** How many unique IDs can the generator produce?
3. **Representation length:** How many characters are needed to display an ID in Base62 or Base64?

These are related, but they are not the same thing.

The most important point is:

> **You can store an ID in a 64-bit&#x20;**`BIGINT`**&#x20;while deliberately generating IDs that fit into just 7 Base62 characters. You do not have to use the entire range of the&#x20;**`BIGINT`**.**

Let's build the reasoning from the ground up, then compare the practical ID-generation approaches for a distributed URL shortener.

## **1. First, understand the size of the Base62 namespace**

Suppose our alphabet contains:

`0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ`

That's 62 different characters.

Each character can represent one of 62 possible values. Therefore:

| Short-code length | Number of possible codes | Approximate bits required |
| ----------------- | ------------------------ | ------------------------- |
| 1                 | 62                       | 6                         |
| 2                 | 3,844                    | 12                        |
| 3                 | 238,328                  | 18                        |
| 4                 | 14.7 million             | 24                        |
| 5                 | 916 million              | 30                        |
| 6                 | 56.8 billion             | 36                        |
| 7                 | 3.52 trillion            | 42                        |
| 8                 | 218 trillion             | 48                        |

The formula is:

\\[ \text{Number of possible codes} = 62^L \\]

where \\(L\\) is the number of characters.

So your calculation is correct in principle:

\\[ 62^7 \approx 3.52 \text{ trillion} \\]

Seven characters give us enough namespace for trillions of unique short codes.

One small refinement: a Base62 character represents \\(\log_2(62) \approx 5.954\\) bits, not exactly 6 bits. We often round it to 6 bits for estimation.

For 7 characters:

\\[ 7 \times \log_2(62) \approx 41.68 \text{ bits} \\]

That means 42 bits are sufficient to represent the *number* of Base62 codes available. But a 42-bit integer can represent slightly more values than fit into 7 Base62 characters, so we must enforce the Base62 range limit.

## **2. Why PostgreSQL&#x20;**`BIGINT`**&#x20;is not a problem**

You said:

- PostgreSQL `INTEGER` uses 4 bytes = 32 bits.
- PostgreSQL `BIGINT` uses 8 bytes = 64 bits.
- Seven Base62 characters need approximately 42 bits.
- Therefore, `BIGINT` looks like unnecessary storage.

The important distinction is that **the database column's storage capacity does not determine the length of the generated short code**.

For example, imagine the database generates these numeric IDs:

```
1
2
3
4
...
1000000
...
3521614606207
```

We then convert each number to Base62:

```
1       -> 1
2       -> 2
...
61      -> Z
62      -> 10
63      -> 11
...
```

The database stores the numeric value. Our application converts it into a short string when creating the short URL.

A PostgreSQL `BIGINT` can store all these values easily. We simply stop generating new IDs once we reach the maximum value that can be represented by seven Base62 characters.

That maximum is:

\\[ 62^7 - 1 = 3,521,614,606,207 \\]

So our usable numeric range can be:

\\[ 0 \leq ID < 62^7 \\]

**We are using an 8-byte database type to store a number whose permitted range requires only 42 bits.** The extra capacity is unused, but it does not make our short code longer.

This is perfectly reasonable. PostgreSQL does not provide a standard 42-bit integer type, and saving a few bytes per row is usually less important than keeping the implementation simple and reliable.

## **3. How Base62 encoding actually works**

Think of Base62 as decimal representation, except instead of 10 digits, we have 62 digits.

In decimal:

\\[ 125 = 1 \times 10^2 + 2 \times 10^1 + 5 \times 10^0 \\]

In Base62, the digits range from `0` to `Z`.

Let's use this alphabet:

```
0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ
```

Some examples:

| Decimal number | Base62 code | Explanation                         |
| -------------- | ----------- | ----------------------------------- |
| 0              | `0`         | First character                     |
| 1              | `1`         | Second character                    |
| 10             | `a`         | Alphabet starts after 9             |
| 61             | `Z`         | Last Base62 character               |
| 62             | `10`        | Just like decimal 10, but in Base62 |
| 63             | `11`        |                                     |
| 125            | `21`        | \\(2 \times 62 + 1\\)               |
| 3,844          | `100`       | \\(1 \times 62^2\\)                 |

The conversion algorithm is simple:

**Decimal to Base62:**

1. Divide the number by 62.
2. Take the remainder. It identifies the next Base62 character.
3. Divide the number by 62 again.
4. Repeat until the number becomes zero.
5. Reverse the collected characters.

For example, converting 125:

\\[ 125 \div 62 = 2 \text{ remainder } 1 \\]

The remainder `1` maps to character `1`.

Then:

\\[ 2 \div 62 = 0 \text{ remainder } 2 \\]

The remainder `2` maps to character `2`.

Reading the characters in reverse order gives:

```
125 (decimal) = 21 (Base62)
```

**Base62 to decimal** is the reverse operation:

\\[ 21\_{62} = 2 \times 62 + 1 = 125 \\]

This is important for our URL shortener because we can decode the short code back into the original numeric ID.

## **4. The simplest URL shortener: Database sequence + Base62**

Let's build the first version.

### **ID generation**

In PostgreSQL, we create a sequence with a maximum value corresponding to our seven-character Base62 limit.

```
CREATE SEQUENCE short_code_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 3521614606207
    NO CYCLE;
```

Our URL mapping table:

```
CREATE TABLE url_mapping (
    id BIGINT PRIMARY KEY DEFAULT nextval('short_code_seq'),
    original_url TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

When a user submits a URL:

```
https://example.com/products/electronics/mobile/iphone
```

The application:

1. Inserts the original URL into PostgreSQL.
2. PostgreSQL generates a unique numeric ID.
3. The application converts the ID into Base62.
4. It returns the resulting short URL.

For example:

```
Database ID: 125
Base62:      21
Short URL:   https://sho.rt/21
```

Another request:

```
Database ID: 126
Base62:      22
Short URL:   https://sho.rt/22
```

And so on.

### **How does redirection work?**

A user visits:

```
https://sho.rt/21
```

The application:

1. Extracts `21`.
2. Decodes Base62 `21` into decimal `125`.
3. Queries PostgreSQL using the primary key.
4. Gets the original URL.
5. Redirects the user.

Conceptually:

```
                 CREATE SHORT URL

Client
  |
  v
API Server
  |
  v
PostgreSQL Sequence
  |
  v
Numeric ID: 125
  |
  v
Base62 Encoder
  |
  v
Short Code: 21
  |
  v
Store URL Mapping / Return Short URL


                 REDIRECT

Browser
  |
  v
GET /21
  |
  v
API Server
  |
  v
Base62 Decoder
  |
  v
Numeric ID: 125
  |
  v
Database Lookup
  |
  v
Original URL
  |
  v
HTTP Redirect
```

Notice something useful here: **we don't necessarily need to store the Base62 short code in the database.**

Since it is deterministically derived from the numeric ID, we can decode the incoming short code and directly query the `BIGINT` primary key.

This saves us from maintaining a separate short-code column and index, although storing the code can still be useful for other requirements.

### **Does this guarantee uniqueness?**

Yes, provided all IDs come from the same properly coordinated sequence.

Consider two application servers receiving requests simultaneously:

```
Server A              PostgreSQL              Server B

   |                      |                       |
   |------ nextval ------>|                       |
   |                      |                       |
   |<-------- 125 --------|                       |
   |                      |<------ nextval -------|
   |                      |                       |
   |                      |-------- 126 --------->|
```

PostgreSQL sequence generation is atomic. Concurrent calls get distinct values.

And Base62 conversion is one-to-one over the permitted numeric range. Therefore, distinct numeric IDs produce distinct canonical Base62 codes.

There is no hash collision problem.

One detail: database sequences can have gaps. For example, if an insert fails after getting ID `125`, the next request might get `126`. This is perfectly fine. **Unique IDs are important; consecutive IDs are not.**

## **5. But is a database sequence scalable enough?**

This is where your question becomes a distributed systems design problem.

The simple approach has a limitation:

Every new short URL needs a numeric ID from a centrally coordinated source.

Initially, this is fine:

```
                 API Servers
               /      |      \
              /       |       \
          Server A Server B Server C
              \       |       /
               \      |      /
                PostgreSQL
                 Sequence
```

As traffic grows:

- Multiple application instances request IDs.
- The database becomes a shared dependency for ID allocation.
- Multiple database shards cannot independently use the same sequence without coordination.
- Multi-region deployments require globally coordinated uniqueness.

We need to scale ID generation without increasing the short-code length.

### **Solution: Allocate ID ranges (block allocation)**

This is one of the most practical approaches for a URL shortener.

Instead of asking the database for an ID for every request, ask it for a **block of IDs**.

Suppose we allocate 100,000 IDs at a time.

The central allocator gives:

| Application server | Allocated ID range |
| ------------------ | ------------------ |
| Server A           | 1–100,000          |
| Server B           | 100,001–200,000    |
| Server C           | 200,001–300,000    |
| Server D           | 300,001–400,000    |

Now each application server generates IDs locally.

For example:

```
Server A:
  1
  2
  3
  4
  ...
  100000

Server B:
  100001
  100002
  100003
  ...
  200000
```

No server needs to contact the central allocator for each new URL.

The application can use an atomic counter to safely generate IDs within its allocated range.

A simplified PostgreSQL range reservation can look like this:

```
UPDATE id_allocator
SET next_id = next_id + 100000
WHERE name = 'short_url'
  AND next_id <= 3521614606207 - 100000 + 1
RETURNING
    next_id - 100000 AS range_start,
    next_id - 1      AS range_end;
```

The allocator row is updated atomically, so concurrent servers receive non-overlapping ranges.

For example:

```
Server A requests a block
        |
        v
Allocator returns [1, 100000]

Server B requests a block
        |
        v
Allocator returns [100001, 200000]

Server C requests a block
        |
        v
Allocator returns [200001, 300000]
```

Now the application servers generate Base62 IDs locally.

**What happens if Server A crashes after using only 20,000 IDs?**

The remaining 80,000 IDs are wasted.

That's okay.

We don't need continuous IDs. We need unique IDs. Wasting some IDs is a reasonable trade-off for scalability.

The important rule is that an allocated range must never be handed to another server while the original server could still be using it.

This design is commonly described as **range allocation**, **block allocation**, or a form of the **Hi-Lo ID generation strategy**.

It gives us:

- Globally unique IDs through non-overlapping ranges.
- Local ID generation without a database call for every URL.
- Short Base62 codes.
- A relatively simple implementation.

There is still a central allocator, but it is contacted once per block rather than once per URL. It can also be replicated or partitioned as requirements grow.

## **6. What about Snowflake IDs?**

Now let's revisit your Snowflake question.

A typical Snowflake ID is composed of:

```
+-------------------+------------+----------------+
| Timestamp         | Worker ID  | Sequence       |
+-------------------+------------+----------------+
```

A common design uses:

| Field     | Bits   | Purpose                                  |
| --------- | ------ | ---------------------------------------- |
| Timestamp | 41     | Millisecond timestamp                    |
| Worker ID | 10     | Identifies the generating machine        |
| Sequence  | 12     | Multiple IDs within the same millisecond |
| **Total** | **63** |                                          |

The exact allocation varies by implementation, but this is a popular arrangement.

It allows distributed machines to generate IDs without asking a central database for every ID.

For example:

```
Machine A:
  Timestamp = T
  Worker ID = 1
  Sequence  = 1, 2, 3...

Machine B:
  Timestamp = T
  Worker ID = 2
  Sequence  = 1, 2, 3...
```

The worker ID differentiates machines, while the sequence differentiates IDs generated by the same machine during the same time unit.

### **But your observation is correct: Snowflake still gives us a long ID.**

A positive 63-bit Snowflake ID, when converted to Base62, requires up to **11 characters**.

Why?

\\[ 62^{10} \approx 8.39 \times 10^{17} \\]

\\[ 62^{11} \approx 5.20 \times 10^{19} \\]

The positive range of a signed 64-bit integer is larger than \\(62^{10}\\), but smaller than \\(62^{11}\\).

Therefore, 11 Base62 characters can be required.

So:

```
64-bit Snowflake ID
        |
        v
Base62 encoding
        |
        v
Up to 11 characters
```

Snowflake solves the distributed ID generation problem. It does not automatically solve the short-ID representation problem.

### **Can we design a smaller Snowflake?**

Yes!

But we must reduce the number of bits.

Remember:

\\[ 62^7 \approx 3.52 \text{ trillion} \\]

And:

\\[ 2^{41} \approx 2.20 \text{ trillion} \\]

Therefore:

\\[ 2^{41} < 62^7 \\]

This means **any non-negative 41-bit integer can fit inside a 7-character Base62 code**.

We can design a compact time-based distributed ID generator using 41 bits.

For example:

| Field               | Bits   | Capacity                       |
| ------------------- | ------ | ------------------------------ |
| Timestamp (seconds) | 29     | Approximately 17 years         |
| Worker ID           | 5      | 32 machines                    |
| Sequence            | 7      | 128 IDs per second per machine |
| **Total**           | **41** |                                |

The maximum generation rate is:

\\[ 32 \times 128 = 4096 \text{ IDs/second} \\]

The resulting ID is constructed by combining these three fields:

```
+----------------------+----------------+----------------+
| Timestamp (29 bits)  | Worker (5 bits)| Sequence (7 bits)|
+----------------------+----------------+----------------+
```

The resulting number is always less than \\(2^{41}\\), so it fits in seven Base62 characters.

This is a valid compact Snowflake-style design, but notice what we sacrificed:

- The timestamp uses seconds instead of milliseconds.
- We have a limited number of worker IDs.
- We can generate only 128 IDs per second per worker.
- The timestamp field has a limited lifetime.

We can redistribute bits to support more machines or more IDs per second, but every additional bit assigned to one field must be taken away from another.

**This is the central trade-off of compact distributed IDs: timestamp range, number of machines, and generation throughput compete for a limited bit budget.**

Also, an ordinary 42-bit allocation cannot automatically use its entire range in a seven-character Base62 code. Since \\(2^{42} > 62^7\\), we would need to restrict the generated values to the Base62 range. Using 41 bits avoids that complication.

### **When would I choose compact Snowflake?**

Choose it when:

- ID generation must happen independently on multiple machines.
- You want to avoid a central database dependency.
- The required generation rate fits the chosen bit allocation.
- You can manage worker IDs and clock synchronization.
- Seven-character codes are a firm requirement.

For a typical URL shortener, I would not automatically choose Snowflake. A block-allocated sequence is often simpler and provides a much larger usable ID namespace for the same seven-character requirement.

## **7. What about randomly generating seven-character codes?**

Another possibility is to generate a random string:

```
a8Kp29Q
Zx72mP1
bT9kL0x
```

Seven Base62 characters give us:

\\[ M = 62^7 = 3.52 \times 10^{12} \\]

At first glance, this looks excellent.

But there is a problem: **random code generation does not guarantee uniqueness.**

Imagine two requests independently generate:

```
Request A -> a8Kp29Q
Request B -> a8Kp29Q
```

Now both want the same short code.

This is the birthday paradox.

### **How likely are collisions?**

For a randomly generated code, the approximate probability of at least one collision among \\(n\\) codes is:

\\[ P(\text{collision}) \approx 1 - e^{-\frac{n(n-1)}{2M}} \\]

For our seven-character namespace:

| Number of generated codes | Probability of at least one collision |
| ------------------------- | ------------------------------------- |
| 10,000                    | \~0.0014%                             |
| 100,000                   | \~0.14%                               |
| 1 million                 | \~13.2%                               |
| 10 million                | \~99.9999%                            |

The important distinction is that these are probabilities of at least one collision across the entire generated collection, not the probability that every new request collides.

At one million existing codes, the probability that the *next* random code hits an existing one is still only about:

\\[ \frac{1,000,000}{3.52 \times 10^{12}} \approx 0.0000284\\% \\]

But across millions of generation attempts, collision events will eventually occur.

### **Can we handle collisions?**

Absolutely.

Use a unique constraint on the short code:

```
CREATE TABLE url_mapping (
    short_code VARCHAR(7) PRIMARY KEY,
    original_url TEXT NOT NULL
);
```

The generation process becomes:

```
Generate random code
        |
        v
Attempt database insert
        |
        v
Did insert succeed?
    /         \
  Yes          No
   |            |
 Return       Generate
 code         another code
```

A database unique constraint is essential. A preliminary `SELECT` followed by an `INSERT` is not sufficient because two concurrent requests could both observe that the code is unused.

### **When is this approach useful?**

Random codes are useful when:

- You do not want sequential-looking URLs.
- You want code generation distributed across application servers.
- You are comfortable handling collisions and retries.
- You have sufficient namespace capacity.

However, as the namespace fills up, retries become increasingly frequent.

Also, random short codes should not be treated as authentication for private resources. Access control and rate limiting are still necessary.

## **8. Why not hash the original URL?**

You already identified the main problem.

Suppose:

```
URL A -> SHA-256 -> 256-bit hash
URL B -> SHA-256 -> 256-bit hash
```

A 256-bit hash is far too long for a short URL, so we truncate it.

For example:

```
SHA-256
   |
   v
Take a small portion
   |
   v
Convert to Base62
   |
   v
7-character code
```

The problem is that truncation reduces the available hash space. Different URLs can produce the same truncated value.

If we use a seven-character namespace, we are effectively assigning each URL one of only \\(62^7\\) possible identifiers.

We can detect a collision using a database unique constraint, but then we need to generate another identifier, possibly with a salt or counter.

There is another consideration: two identical URLs may be expected to produce the same short code, or they may be expected to produce separate short URLs. That is a product decision, not something hashing automatically solves.

For most URL shorteners, it is simpler to:

1. Generate a unique numeric ID.
2. Convert it to Base62.
3. Store the mapping between ID and original URL.

Use a hash when you specifically want content-derived or deterministic identifiers, not simply because you need a unique short code.

## **9. Comparing the approaches**

| Approach                           | Collision handling                           | Distributed generation               | Seven-character support | Main trade-off                             |
| ---------------------------------- | -------------------------------------------- | ------------------------------------ | ----------------------- | ------------------------------------------ |
| DB auto-increment + Base62         | No collisions with one coordinated sequence  | Limited by central allocator         | Yes                     | Database dependency                        |
| DB range/block allocation + Base62 | No collisions if ranges never overlap        | Yes, locally within allocated ranges | Yes                     | Wasted IDs and allocator coordination      |
| Standard 64-bit Snowflake + Base62 | No collisions with correct worker allocation | Yes                                  | No, up to 11 characters | Longer codes                               |
| Custom 41-bit Snowflake + Base62   | No collisions with correct bit allocation    | Yes                                  | Yes                     | Reduced timestamp/worker/throughput budget |
| Random Base62                      | Possible; retry with unique constraint       | Yes                                  | Yes                     | Collision retries and namespace saturation |
| Truncated URL hash                 | Possible                                     | Yes                                  | Yes                     | Collision management and hash semantics    |

## **10. What I would actually build for this URL shortener**

For your requirement, I would start with:

**PostgreSQL BIGINT sequence + Base62 encoding + block allocation when the system grows.**

Here is why.

### **Proposed architecture**

```
                       URL CREATION

                  Client
                    |
                    v
               Load Balancer
                    |
                    v
             URL Shortener API
             /       |       \
            /        |        \
       Server A   Server B   Server C
            |        |        |
            v        v        v
        Local ID  Local ID  Local ID
        Counter   Counter   Counter
            |        |        |
            +--------+--------+
                     |
                     v
              ID Range Allocator
                     |
                     v
                 PostgreSQL
                     |
                     v
             URL Mapping Table


                       REDIRECTION

                 Browser
                    |
                    v
             GET /{shortCode}
                    |
                    v
              URL Redirect API
                    |
                    v
              Base62 Decoder
                    |
                    v
                Numeric ID
                    |
                    v
               Redis Cache
                    |
              Cache Miss?
                    |
                    v
                PostgreSQL
                    |
                    v
               Original URL
                    |
                    v
               HTTP Redirect
```

### **URL creation flow**

Suppose Server A has received the range:

```
[1, 100000]
```

A request arrives:

```
Original URL:
https://example.com/products/iphone
```

Server A generates ID `125` using its local counter.

It converts:

```
125 -> Base62 -> 21
```

It stores:

```
id           = 125
original_url = https://example.com/products/iphone
```

And returns:

```
https://sho.rt/21
```

### **URL redirection flow**

The user visits:

```
https://sho.rt/21
```

The API:

```
21 (Base62)
   |
   v
125 (Decimal)
   |
   v
Redis lookup
   |
   v
PostgreSQL lookup if cache miss
   |
   v
https://example.com/products/iphone
```

This is efficient because our short code directly represents the database primary key.

### **What about scaling the database?**

We can introduce:

- Redis for frequently accessed URL mappings.
- Database read replicas for redirect traffic.
- Database partitioning or sharding as the URL mapping table grows.
- ID range allocation across application servers to reduce centralized coordination.

One important detail: if we eventually distribute the database across multiple shards, we must still ensure that every shard receives non-overlapping ID ranges. Independent auto-increment sequences starting at 1 on every shard are not globally unique.

## **11. How long will seven-character IDs actually last?**

This is an important capacity-planning question.

Our namespace contains:

\\[ 3,521,614,606,208 \text{ possible IDs} \\]

Let's calculate how long it lasts at different ID-generation rates.

| Generation rate | IDs per year  | Approximate namespace lifetime |
| --------------- | ------------- | ------------------------------ |
| 100/sec         | 3.15 billion  | 1,116 years                    |
| 1,000/sec       | 31.5 billion  | 112 years                      |
| 10,000/sec      | 315 billion   | 11.2 years                     |
| 100,000/sec     | 3.15 trillion | 1.12 years                     |
| 1,000,000/sec   | 31.5 trillion | 41 days                        |

This assumes the system generates IDs continuously at that rate.

For a typical URL shortener, seven characters provide a substantial namespace.

But there is an important architectural decision:

**What is our expected lifetime and maximum ID-generation rate?**

If we need 20 years at 10,000 IDs per second, seven characters aren't sufficient. We would need eight characters, a different encoding strategy, or a namespace-expansion plan.

Do not confuse the number of URLs stored with the number of requests handled. A URL may receive millions of redirects but consumes only one ID when created.

## **12. One last clarification: Base62 vs Base64**

You mentioned:

> 64 bits / 6 bits = 10 characters.

The correct calculation is:

\\[ \left\lceil \frac{64}{6} \right\rceil = 11 \\]

So a 64-bit integer needs up to 11 Base64 digits when represented as an integer.

There is a small technical distinction:

- **Base62:** 62 alphanumeric characters. Convenient for short URL paths.
- **Base64:** 64 symbols, including `+` and `/` in its standard alphabet.
- **Base64URL:** Uses URL-safe alternatives (`-` and `_`). Eight bytes encoded as unpadded Base64URL require 11 characters; standard padded Base64 uses 12 characters for those eight bytes.

For a URL shortener, I would generally choose Base62 because its alphabet is entirely alphanumeric.

# **Final recommendation**

Let's connect all the ideas.

| Your requirement                      | My recommendation                                                                |
| ------------------------------------- | -------------------------------------------------------------------------------- |
| Short codes of at most 7 characters   | Base62                                                                           |
| Enough capacity for trillions of URLs | Seven-character Base62 namespace                                                 |
| Numeric ID storage                    | PostgreSQL `BIGINT`                                                              |
| Initial ID generation                 | PostgreSQL sequence with an explicit maximum                                     |
| Distributed ID generation             | Block/range allocation                                                           |
| Application-side generation           | Atomic counter within an allocated range                                         |
| URL lookup                            | Decode Base62 and query by numeric primary key                                   |
| Frequently accessed URLs              | Redis cache                                                                      |
| Avoiding sequential-looking codes     | Random codes with unique-index validation and retries, or reversible obfuscation |

The core idea to remember is:

**The database does not need a 42-bit datatype just because your short codes need approximately 42 bits. A 64-bit&#x20;**`BIGINT`**&#x20;can store those values, and you can restrict the ID generator to the range that fits into seven Base62 characters.**

And remember these distinctions:

- **Base62** makes IDs shorter.
- **Sequences and range allocation** provide uniqueness.
- **Snowflake** provides distributed generation without a central per-ID dependency.
- **Random generation** provides less predictable codes, but requires collision handling.
- **Database indexes and caching** make URL retrieval efficient.

For this design, I would begin with a capped PostgreSQL sequence and Base62. When ID-generation throughput becomes a bottleneck, introduce range allocation without changing the public short-code format or the URL lookup mechanism.





































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------



























# Short ID generation for a URL shortener

Your reasoning is mostly right. There is one core confusion behind your question, so I'll fix that first, then cover the real problem (generating IDs in a distributed system).

## 1. The key insight: storage size is not string length

You are mixing up two things:

- **Column size** is how many bytes the database reserves. A BIGINT always takes 8 bytes.
- **Encoded length** depends only on how big the number's value is.

```
BIGINT column (64 bits reserved)

value = 125
  bits: 0000...0000 1111101      <- only 7 bits actually used
  base62: "21"                   <- 2 chars

value = 1,000,000,000
  bits used: 30
  base62: "15FTGg"               <- 6 chars
```

A BIGINT holding the number 1 billion encodes to **6 characters**. The empty upper bits cost you nothing in the string. You never encode the whole 64-bit space, only the current counter value.

> **Remember:** The column is a box. The string length depends on what is inside the box, not on the box size.

So "we need <8 chars but BIGINT is 64 bits" is not a problem for a plain auto-increment. The counter only reaches big numbers if you actually create that many URLs.

## 2. Fixing the numbers

You wrote 62^6 is "millions". It is much bigger:

| Chars | Combinations | Roughly |
|---|---|---|
| 6 | 62^6 | 56.8 billion |
| 7 | 62^7 | 3.5 trillion |
| 8 | 62^8 | 218 trillion |

Your bit math is right: 62^7 ≈ 2^41.7, so you need about 42 bits.

**Do we need 7 characters?** Suppose 100 million new URLs per month:
- 1.2 billion per year
- 12 billion in 10 years

That is under 56.8 billion, so even **6 characters** is enough for 10 years. Seven is very safe.

## 3. Base62 by hand (so it sticks)

Alphabet: `0-9` (values 0–9), `a-z` (10–35), `A-Z` (36–61).

Encode **125**:
```
125 / 62 = 2  remainder 1
  2 / 62 = 0  remainder 2

Read remainders bottom to top: 2, 1  ->  "21"
```

Encode **1,000,000,000**, by finding how many of each power of 62 fit:
```
62^5 = 916,132,832 -> 1 time   (left: 83,867,168)
62^4 =  14,776,336 -> 5 times  (left:  9,985,488)
62^3 =     238,328 -> 41 times (left:    214,040)
62^2 =       3,844 -> 55 times (left:      2,620)
62^1 =          62 -> 42 times (left:         16)
62^0 =           1 -> 16 times

digits: 1, 5, 41, 55, 42, 16
chars:  1  5   F   T   G   g   ->  "15FTGg"
```

It is the same as converting to hex, just with 62 symbols instead of 16.

## 4. Why base64 on a 64-bit number gives about 11 characters

You calculated 64 / 6 ≈ 10. It rounds up to **11** (10.67). That is the length when you use **all 64 bits**, meaning numbers near 9.2 × 10^18.

```
Plain counter:  1, 2, 3 ... 3 billion   -> small numbers -> 6-7 chars
Full 64-bit:    ~9,000,000,000,000,000,000 -> 11 chars
```

You only get 11 characters if your IDs are huge. That leads to Snowflake.

## 5. Why Snowflake gives long IDs

Snowflake layout:

```
| 1 bit | 41 bits timestamp (ms) | 10 bits machine | 12 bits sequence |
```

The timestamp sits in the **highest bits**. Milliseconds since the epoch are already about 1.7 trillion, so a Snowflake ID is around 1.8 × 10^18 from day one. That encodes to **11 base62 characters** immediately.

So your intuition was correct: **plain Snowflake is too long for a short URL**. It is great for IDs like tweet IDs, but not for 7-character links.

The fix is to design your own smaller Snowflake, or avoid Snowflake. Section 6 covers the options.

## 6. The real problem: generating IDs across many servers

A single Postgres `AUTO_INCREMENT` gives short IDs, but it has two problems:
- It is a **single point of failure**.
- It is a **write bottleneck**, because every app server asks one DB for every ID.

You want many app servers generating unique IDs independently. These are the common solutions.

### Option A: Range allocation (most popular, my recommendation)

A small central service (a DB row, ZooKeeper, or etcd) hands out **blocks** of IDs.

```
              +-----------------+
              |  Counter store  |  next_start = 3,000,000
              +--------+--------+
                       |   "give me a block of 1,000,000"
      +----------------+----------------+
      v                v                v
 App Server 1     App Server 2     App Server 3
 [1M .. 2M)       [2M .. 3M)       [3M .. 4M)
 uses counter     uses counter     uses counter
 in memory        in memory        in memory
```

- Each server increments a counter **in memory**, which is extremely fast.
- It talks to the central store only once per million IDs.
- IDs are unique with no coordination between servers.
- If a server crashes, you lose the rest of its block. That wastes some numbers, but with 3.5 trillion available it doesn't matter.

You then base62-encode each ID. IDs stay short, because they are just a growing counter.

### Option B: Key Generation Service (KGS)

Pre-generate random 7-character keys offline and store them in a table:

```
keys table:   key       | used
              aB3xK9p   | false
              Zq81LmT   | false
```

App servers fetch a batch (say 1000 keys) and mark it used. Each server hands out its batch from memory.

- Pros: IDs are **random**, so they can't be guessed or enumerated, and there are no collisions at request time.
- Cons: it is another component to build and run, and you must take care that two servers never get the same batch.

### Option C: Custom mini-Snowflake (fits in 41 bits)

You need to stay under about 2^41.7 so it fits in 7 chars. Make your own layout:

```
| 30 bits time (seconds) | 5 bits machine | 6 bits sequence |   = 41 bits
```

- 30 bits of seconds is about 34 years.
- 5 bits gives 32 machines.
- 6 bits gives 64 IDs per second per machine, so about 2,000 IDs/sec in total.

Pros: no coordination at all. Cons: low throughput per machine, and a **clock going backwards** can create duplicates. Because of this, most real systems prefer Option A.

### Option D: Hash the URL and truncate

Take MD5 or SHA of the URL, base62-encode it, and keep the first 7 chars. Collisions are a real problem here (birthday paradox):

```
Space = 3.5 trillion
50% chance of a collision after only about 2 million URLs
```

Handling it: add a **UNIQUE constraint** on the short code. If an insert fails, add a salt to the URL (for example a counter) and retry. This works but costs extra DB lookups and retries. It is mainly useful if you want the **same long URL to always produce the same short code** (deduplication).

## 7. Problem: sequential IDs are guessable

With counters, `15FTGg` is followed by `15FTGh`. Anyone can scan every short link.

If that matters, **scramble the number before encoding** with a reversible function (a bijection):

```
id = 1000
scrambled = (id * LARGE_PRIME) mod 62^7     <- still unique, looks random
code = base62(scrambled)
```

Because it is reversible, uniqueness is preserved. Libraries like Sqids or Hashids do this.

## 8. The final design (what I'd say in an interview)

```
Client -> Load Balancer -> App Servers
                              |  (ID block of 1M, cached in memory)
                              v
        Range allocator (ZooKeeper/DB) <- only hit once per 1M IDs
                              |
   id -> (optional scramble) -> base62 -> short code
                              |
                              v
        Sharded DB: code -> long_url   (+ cache for reads)
```

## One-page cheat sheet

| Approach | Short? | Collisions? | Needs coordination? | Verdict |
|---|---|---|---|---|
| UUID | No (22+ chars) | No | No | Too long |
| Hash + truncate | Yes | Yes, retries needed | No | Only for dedup |
| Single auto-increment | Yes | No | Yes, SPOF | Doesn't scale |
| Plain Snowflake | No (11 chars) | No | No | Too long |
| **Range allocation + base62** | **Yes** | **No** | **Rarely** | **Best default** |
| KGS | Yes | No | Light | Good if you need unguessable codes |
| Custom mini-Snowflake | Yes | No | No | Fine, but clock risk |

## Three lines to memorize

1. **String length depends on the value, not the column size.** A BIGINT holding 1 billion is 6 chars.
2. **Plain Snowflake is long** because the timestamp is in the high bits (11 chars).
3. **Hand out ID ranges** so servers count locally in memory, then base62 the result.

If you'd like, we can continue with the next piece: the DB schema and sharding, read caching, or custom aliases and expiry. Which one next?

