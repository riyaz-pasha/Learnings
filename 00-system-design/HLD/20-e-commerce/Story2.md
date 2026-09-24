Perfect. This is **exactly the right way to learn HLD for interviews** — step-by-step reasoning instead of memorizing architecture diagrams.

Since you are preparing for **product companies / MAANG interviews**, we'll treat this like a **real interview conversation**.

We will build **Amazon step-by-step like engineers actually did**.

And we will cover **all HLD topics naturally while evolving the system**:

* Functional Requirements
* Non-Functional Requirements
* Capacity Estimation
* API Design
* Data Modeling
* Caching
* Load Balancing
* Database Scaling
* Sharding
* Replication
* Async Processing
* Search Systems
* Recommendation Systems
* Fault Tolerance
* Observability

But **we won't jump to the final architecture immediately.**

We will build it like a **story of evolution**.

---

# Step 1 — The Problem That Started Everything

Let's go back in time.

### Imagine it's early 2000s.

Some founders want to build an **online bookstore**.

People should be able to:

1. Browse books
2. Search books
3. Add to cart
4. Place order
5. Pay online

Basically the **first Amazon MVP**.

---

# Step 2 — Functional Requirements (Interview First Question)

In interviews you should start with this.

### Core Functional Requirements

Customers should be able to:

1. Register / login
2. Browse products
3. Search products
4. View product details
5. Add to cart
6. Checkout
7. Make payment
8. Track orders

Sellers should be able to:

1. List products
2. Update inventory
3. View orders

---

# Step 3 — Non Functional Requirements

This is **where senior engineers stand out in interviews**.

For Amazon-like systems:

### High Availability

Amazon **cannot go down**.

Target:

```
99.99% availability
```

Downtime allowed per year:

```
~52 minutes
```

---

### High Scalability

Amazon scale is massive.

Example numbers (simplified):

Users: 300M+

Peak traffic:

```
1M+ requests/sec
```

Products:

```
500M+
```

Orders per day:

```
10M+
```

---

### Low Latency

User should see product pages fast.

Target:

```
< 200ms
```

---

### Consistency Requirements

Some operations must be **strongly consistent**.

Example:

```
Inventory updates
Payment
Order placement
```

Some can be **eventually consistent**:

```
Recommendations
Reviews
Analytics
```

---

# Step 4 — Capacity Estimation (Interviewers LOVE this)

Let's assume simplified numbers.

### Users

```
100M users
```

Active users per day:

```
10M
```

---

### Traffic

Assume average:

```
100M requests/day
```

Requests per second:

```
~1200 RPS
```

Peak (10x):

```
12000 RPS
```

---

### Orders

Orders/day:

```
1M
```

Orders/sec:

```
~12/sec
```

Peak:

```
120/sec
```

---

### Product Catalog

Products:

```
100M
```

Average product size:

```
2 KB
```

Storage:

```
200 GB
```

Very manageable.

---

# Step 5 — First Naive Architecture (How Startups Start)

Engineers build something simple.

### Single Server Architecture

```
Users
  |
Internet
  |
Web Server
  |
Application
  |
Database
```

Example stack:

```
Nginx
Java / Node / Python
MySQL
```

---

### Database Tables

Users

```
users
------
id
name
email
password
address
```

Products

```
products
--------
id
title
price
description
inventory
seller_id
```

Orders

```
orders
------
id
user_id
total_price
status
created_at
```

Order Items

```
order_items
-----------
order_id
product_id
quantity
price
```

Cart

```
cart
-----
user_id
product_id
quantity
```

---

### Flow Example — Place Order

1 User opens product page

```
GET /products/{id}
```

2 Add to cart

```
POST /cart
```

3 Checkout

```
POST /orders
```

Server does:

```
check inventory
create order
update inventory
process payment
```

---

# Step 6 — Problems Appear (System Growth)

After some months…

Traffic increases.

Now system starts breaking.

### Problem 1 — Server Overload

Single server cannot handle:

```
12k requests/sec
```

CPU spikes.

Server crashes.

---

### Problem 2 — Database Bottleneck

All requests go to **single database**.

Examples:

```
Product reads
Cart updates
Orders
Inventory updates
Search
```

DB becomes bottleneck.

---

### Problem 3 — Slow Product Pages

Each page load queries DB:

```
SELECT * FROM products
```

Millions of users → DB overload.

---

### Problem 4 — No Fault Tolerance

If server crashes:

```
Entire website down
```

---

# Step 7 — First Fix Engineers Try

They add **multiple servers**.

```
             +--------+
Users -----> |  LB    |
             +--------+
             /   |   \
            /    |    \
         App1  App2  App3
             \    |    /
               Database
```

Load balancer distributes traffic.

---

### Why This Works

Instead of:

```
1 server
```

Now:

```
10 servers
```

Each handles part of traffic.

---

### Important Interview Concept

This works because **servers are stateless**.

Meaning:

```
Any request can go to any server
```

No session stored in server memory.

Sessions stored in:

```
Redis / DB
```

---

# Interview Tip

Always say this:

> "Application servers should be stateless to allow horizontal scaling."

Interviewers love this.

---

# But A New Problem Appears

Database still single.

Now 10 app servers are hitting **1 DB**.

DB becomes the bottleneck.

---

# This Leads To The Next Big Idea

Engineers introduce:

```
Caching
```

This is **one of the biggest improvements in e-commerce systems**.

Example:

```
Product page
Home page
Popular items
```

These don't change often.

So we cache them.

---

# Next Step (Important)

Next we will cover **one of the most important HLD concepts**:

# Redis Caching Layer

And we will discuss:

* Cache Aside Pattern
* Read Through Cache
* Write Through
* Cache Invalidation
* Hot Keys
* Cache Stampede

And **why Amazon relies heavily on caching**.

---

Before moving forward I want to check something (like a real interview discussion):

### Question for you

When a user opens a product page:

```
GET /products/123
```

Should the system:

A)

```
Always query database
```

B)

```
Check cache first
If miss → query DB
```

Which one is correct **and why?**

Answer this — then we'll move to the **next architecture evolution**.

---

Good. Now we move to the **first major scalability improvement used by almost every large system**.

This concept appears in **almost every HLD interview**.

# Step 8 — Introducing Caching (The First Big Optimization)

Let’s continue our story.

We already built this system:

```
Users
   |
Load Balancer
   |
App Servers
   |
Database
```

Traffic increased.

Now imagine **10K users opening the same product page**.

Example:

```
GET /products/123
```

Every request runs:

```
SELECT * FROM products WHERE id = 123
```

So DB receives **10K identical queries**.

Database CPU spikes.

Latency increases.

Eventually:

```
DB becomes the bottleneck
```

---

# Key Observation Engineers Made

Product data **doesn't change frequently**.

Example product:

```
iPhone 15
Price: ₹80,000
Description: ...
```

This data might change **once a day**.

But it might be **read millions of times**.

So engineers asked:

> Why query DB for the same data repeatedly?

This leads to **Caching**.

---

# New Architecture With Cache

```
Users
   |
Load Balancer
   |
App Servers
   |
Cache (Redis / Memcached)
   |
Database
```

Now requests flow like this:

```
Client → App → Cache → DB
```

---

# Product Page Flow With Cache

User opens:

```
GET /products/123
```

### Step 1

App checks cache.

```
Redis GET product:123
```

### Case 1 — Cache Hit

Cache contains:

```
product:123
{
 name: "iPhone 15"
 price: 80000
 description: "..."
}
```

Response returned immediately.

Latency:

```
~1 ms
```

---

### Case 2 — Cache Miss

If cache doesn't contain product:

App queries DB.

```
SELECT * FROM products WHERE id = 123
```

Then stores result in cache.

```
SET product:123 {...}
```

Then returns response.

---

# This Pattern Is Called

### Cache Aside Pattern (Lazy Loading)

Flow:

```
1 Check cache
2 If miss → read DB
3 Store in cache
4 Return response
```

This is the **most commonly used caching pattern**.

Amazon, Netflix, Uber all use it.

---

# Why Cache Is Powerful

Example:

Without cache:

```
10K requests/sec → DB
```

With cache:

```
10K requests/sec → Redis
10 requests/sec → DB
```

Huge DB load reduction.

---

# Why Redis Is Used

Redis is an **in-memory datastore**.

Memory is extremely fast.

Latency comparison:

| Storage | Latency   |
| ------- | --------- |
| Disk DB | 5–10 ms   |
| SSD     | 1–2 ms    |
| Redis   | ~0.2–1 ms |

So Redis is **10–100x faster** than DB.

---

# What Should We Cache?

In e-commerce systems typically:

### Product details

```
product:123
```

### Product price

```
price:123
```

### Product inventory (sometimes)

```
inventory:123
```

### Product reviews

```
reviews:123
```

### Homepage products

```
homepage:popular
```

---

# Important Interview Question

### Should we cache inventory?

Answer:

```
Usually NO
```

Because inventory requires **strong consistency**.

Example:

Inventory = 1

Two users buy simultaneously.

If cached incorrectly:

```
Overselling happens
```

So inventory often goes **directly to DB**.

---

# Next Problem Engineers Faced

Caching helped a lot.

But now new problems appear.

### Problem 1 — Cache Invalidation

What if price changes?

Example:

```
iPhone price changed
80000 → 75000
```

But cache still has:

```
80000
```

Users see wrong price.

---

# Cache Invalidation Strategies

When DB updates product:

```
UPDATE products SET price=75000 WHERE id=123
```

We also:

```
DELETE cache key product:123
```

Next request will fetch fresh data.

This is called:

```
Cache Invalidation
```

---

# Problem 2 — Cache Stampede

Imagine a **popular product**.

Cache entry expires.

Suddenly **10K requests arrive simultaneously**.

All of them miss cache.

All hit DB.

DB gets overwhelmed.

This is called:

```
Cache Stampede
```

---

# Solution Engineers Use

### Request Coalescing

Only **one request queries DB**.

Other requests wait.

Example:

```
1 request → DB
9999 wait
```

---

# Problem 3 — Hot Keys

Some products are extremely popular.

Example:

```
PS5
iPhone
Diwali deals
```

Millions of requests hit:

```
product:ps5
```

Single Redis node may get overloaded.

Solution:

```
Redis clustering
```

We distribute cache across nodes.

---

# Updated Architecture

Now system looks like this:

```
                +-----------+
Users --------->|  Load     |
                | Balancer  |
                +-----------+
                    |
        -------------------------
        |         |            |
      App1      App2        App3
        |         |            |
        --------Redis Cluster----
                    |
                Database
```

---

# Huge Improvement Achieved

DB load reduced drastically.

Latency improved.

System can handle much higher traffic.

---

# But Now Another Problem Appears

Search becomes the biggest issue.

Users search like:

```
"iphone"
"red shoes under 2000"
"wireless headphones"
```

Our database queries look like:

```
SELECT * FROM products
WHERE name LIKE "%iphone%"
```

This becomes **extremely slow** for millions of products.

Database is not optimized for **full text search**.

---

# This Leads To The Next Big Evolution

Engineers introduced **Search Systems** like:

```
Elasticsearch
Solr
```

This is **one of the most important topics in e-commerce HLD interviews**.

We will cover:

* Why SQL search fails
* How search engines index data
* Inverted indexes
* Search ranking
* Filters
* Facets
* Autocomplete
* Typo tolerance

---

Before moving ahead, quick **interview check question**.

When should we **NOT use cache**?

Example options:

A

```
Product description
```

B

```
Product reviews
```

C

```
Inventory count
```

D

```
Homepage products
```

Which one should **not be cached** and why?

---
Correct. ✅

**Inventory count** usually should **not be cached (or must be handled very carefully)** because it requires **strong consistency**.

Example:

```
Inventory = 1
User A buys
User B buys
```

If both read a **cached value = 1**, both orders may succeed → **overselling**.

So inventory is typically handled through:

* **DB transactions**
* **Atomic updates**
* **Distributed locks**
* **Reservation systems**

We'll revisit this later when we design the **order placement system**, because preventing **overselling** is a classic HLD interview discussion.

---

# Step 9 — Search Problem Appears

Now let's continue the story.

Our system currently looks like this:

```
Users
   |
Load Balancer
   |
App Servers
   |
Redis Cache
   |
Database
```

Everything is working fine for:

* Product page
* Cart
* Orders

But a **major problem appears**.

---

# Real User Behavior

Users **rarely browse all products**.

They search.

Example:

```
"iphone"
"red shoes under 2000"
"gaming laptop"
"wireless earbuds"
```

Search becomes **one of the most used features** in an e-commerce system.

At Amazon scale:

```
Millions of searches per minute
```

---

# First Attempt (Using Database)

Engineers initially tried using SQL.

Example query:

```sql
SELECT * FROM products
WHERE name LIKE '%iphone%'
```

Or

```sql
SELECT * FROM products
WHERE description LIKE '%iphone%'
```

---

# Why This Becomes Slow

Imagine:

```
Products = 100 million
```

Database must scan:

```
100M rows
```

For each search request.

Even with indexing:

```
LIKE %iphone%
```

**cannot use index efficiently**.

This leads to:

```
Very slow queries
High DB CPU usage
Poor search experience
```

---

# Engineers Realized Something Important

Databases are optimized for:

```
Transactional queries
```

Example:

```
get product by id
get order by id
update inventory
```

But search is a different problem.

Search requires:

```
Full text matching
Ranking
Filtering
Sorting
Fuzzy matching
```

So engineers introduced **Search Engines**.

---

# Step 10 — Introducing Search Engine

New architecture:

```
Users
   |
Load Balancer
   |
App Servers
   |
Redis Cache
   |
Search Engine (Elasticsearch)
   |
Database
```

Database remains **source of truth**.

Search engine is used for:

```
Product search
Filtering
Ranking
Autocomplete
```

---

# Example Search Flow

User searches:

```
"iphone"
```

Request:

```
GET /search?q=iphone
```

App server queries:

```
Elasticsearch
```

Search engine returns:

```
product ids
```

Example:

```
[123, 890, 654, 921]
```

Then app fetches product details from:

```
Cache / DB
```

---

# Why Search Engines Are Fast

Search engines use something called:

# Inverted Index

This is **one of the most important search concepts**.

---

# Normal Database Index

Traditional DB index looks like:

```
product_id -> product_name
```

Example:

```
1 -> iPhone 15
2 -> Samsung Galaxy
3 -> Sony Headphones
```

But for search we want:

```
word -> products
```

---

# Inverted Index

Search engine stores:

```
iphone -> [1, 8, 23, 90]
samsung -> [2, 9, 33]
headphones -> [3, 7, 88]
```

Now searching **iphone** is extremely fast.

Just lookup:

```
iphone
```

Return:

```
[1, 8, 23, 90]
```

No full table scan.

---

# Example Product Index

Product:

```
id: 101
name: "Apple iPhone 15 Pro"
description: "256GB smartphone"
```

Search engine tokenizes words:

```
apple
iphone
15
pro
256gb
smartphone
```

Index becomes:

```
apple -> [101]
iphone -> [101]
pro -> [101]
smartphone -> [101]
```

Now queries are extremely fast.

---

# Another Feature — Filters

Users search:

```
"shoes"
```

Then apply filters:

```
Brand = Nike
Price < 5000
Color = Black
```

Search engines handle these efficiently.

---

# How Data Gets Into Elasticsearch

Important concept.

Database is source of truth.

When product changes:

```
Product created
Product updated
Price changed
```

We update search index.

Two approaches:

---

### Approach 1 — Sync Update

App updates:

```
Database
Search Index
```

Problem:

```
Slow writes
Coupling
```

---

### Approach 2 — Async Indexing (Better)

Architecture:

```
DB
 |
Event (Kafka)
 |
Search Indexer
 |
Elasticsearch
```

Flow:

```
Product updated
→ Event produced
→ Indexer updates search index
```

This is **event-driven architecture**.

Very common in large systems.

---

# Updated Architecture

```
                    +----------------+
Users  -----------> | Load Balancer  |
                    +----------------+
                           |
                -------------------------
                |         |            |
             App1       App2         App3
                |         |            |
                -------- Redis ---------
                         |
                    Database
                         |
                       Kafka
                         |
                    Indexer
                         |
                   Elasticsearch
```

---

# Why This Is Powerful

Search engine now handles:

```
Search queries
Ranking
Filters
Sorting
Autocomplete
```

Database handles:

```
Transactions
Orders
Inventory
Payments
```

Each system handles what it is **best at**.

---

# But The System Still Has Major Problems

Next problems appear when traffic grows further.

Biggest one:

# Database Scaling Problem

Single database cannot handle:

```
Millions of users
Millions of orders
Billions of records
```

Engineers must introduce:

```
Read Replicas
Database Sharding
```

These are **very important HLD interview topics**.

---

Next step we will cover:

# Database Replication

Topics we'll go deep into:

* Read replicas
* Leader-follower replication
* Read scaling
* Replication lag
* Failover
* Multi-region replication

These questions appear in **almost every HLD interview**.

---

Before moving forward, quick interview question:

Why **should the search engine NOT be the source of truth** for products?

Why do we still keep **database as primary storage**?

Think like a **system designer**.

---

Excellent answer. ✅ You are thinking in the **correct system design direction**.

Let’s refine it the way you would say it in an **interview**.

---

# Why Elasticsearch Should NOT Be the Source of Truth

You already mentioned two key points. Let's structure it clearly.

### 1️⃣ Elasticsearch is Eventually Consistent

Search index updates are usually **asynchronous**.

Example flow:

```
Product price updated → DB
Event sent → Kafka
Indexer updates → Elasticsearch
```

Between these steps there is **delay**.

So for a short time:

```
DB price = ₹75000
Elasticsearch price = ₹80000
```

If Elasticsearch were the **source of truth**, we could show **incorrect data**.

---

### 2️⃣ Search Index Can Be Rebuilt

Search engines are designed to be **reconstructable**.

If Elasticsearch cluster crashes:

```
We rebuild index from DB
```

Because DB stores the **authoritative data**.

This is a very common interview statement:

> “Search index is a derived datastore, not the source of truth.”

---

### 3️⃣ Elasticsearch Is Not Built for Transactions

Databases provide:

```
ACID guarantees
Transactions
Constraints
```

Example:

```
Place Order
Decrease Inventory
Create Order Record
Process Payment
```

These operations require **atomicity**.

Search engines cannot guarantee that.

---

### 4️⃣ Data Integrity

Databases enforce:

```
Primary keys
Foreign keys
Unique constraints
```

Search engines do not.

Example:

```
Order must reference valid product
```

DB ensures this.

---

# Interview-Level Answer (Best Way to Say It)

You can answer like this:

> "Elasticsearch is used only for fast search queries. It is eventually consistent and cannot guarantee transactional integrity. The database remains the source of truth because it provides ACID guarantees, data integrity, and can be used to rebuild the search index if needed."

That answer would make **interviewers very happy**.

---

# Now We Continue the Story

Our system currently looks like this:

```
Users
   |
Load Balancer
   |
App Servers
   |
Redis Cache
   |
Database
   |
Kafka
   |
Indexer
   |
Elasticsearch
```

Traffic continues to grow.

Now a **new major problem appears**.

---

# Step 11 — Database Scaling Problem

Our database is handling:

```
Product data
Users
Orders
Cart
Inventory
Payments
Reviews
```

And traffic is growing.

Example load:

```
50K requests/sec
```

Database CPU reaches:

```
95%
```

Queries become slow.

This happens because **all reads and writes hit the same DB**.

---

# Engineers Observe Something Important

Most traffic is **READ traffic**.

Example breakdown:

```
Reads  = 95%
Writes = 5%
```

Example reads:

```
View product
View order history
View reviews
Browse categories
```

Writes are fewer:

```
Place order
Update inventory
Add review
```

So engineers think:

> "Can we distribute READ load across multiple databases?"

This leads to the **next evolution**.

---

# Step 12 — Database Replication (Read Replicas)

Architecture becomes:

```
              +------------+
              |  Primary   |
              |  Database  |
              +------------+
                 /     \
                /       \
        +-----------+  +-----------+
        | Read DB 1 |  | Read DB 2 |
        +-----------+  +-----------+
```

---

# How Replication Works

Primary database handles:

```
All WRITES
```

Replicas handle:

```
READ queries
```

Flow:

```
App → Write → Primary DB
App → Read → Replica DB
```

---

# Example

User places order:

```
INSERT INTO orders
```

Goes to:

```
Primary DB
```

User opens product page:

```
SELECT * FROM products
```

Goes to:

```
Replica DB
```

---

# Replication Mechanism (Important)

Primary database generates something called:

```
Write Ahead Log (WAL)
```

Every change is recorded in WAL.

Example:

```
INSERT order 123
UPDATE inventory
UPDATE price
```

Replicas **continuously copy WAL** and apply changes.

So replicas stay **almost in sync**.

---

# Updated Architecture

```
                +-------------+
Users --------->| LoadBalancer|
                +-------------+
                       |
              ---------------------
              |        |         |
            App1     App2      App3
              |        |         |
             Redis Cache
              |
        -----------------------
        |        |           |
     Primary   Replica1   Replica2
        DB
```

---

# Huge Benefit

If we have:

```
1 Primary
5 Replicas
```

Then:

```
Read capacity = 5x
```

Massive scaling improvement.

---

# But Replication Introduces A New Problem

This is a **classic interview trap**.

Imagine this flow:

User places order.

```
Write → Primary DB
```

Immediately user checks order history.

```
Read → Replica DB
```

But replica **has not replicated yet**.

Result:

```
Order not found
```

This problem is called:

# Replication Lag

---

# Common Solutions

Engineers use strategies like:

### Read Your Writes

After write:

```
Route reads to primary temporarily
```

---

### Session Stickiness

User session reads from same DB.

---

### Critical Reads → Primary

Example:

```
Order status
Payment
Inventory
```

---

# Interview Tip

You should mention:

> “Replication improves read scalability but introduces eventual consistency due to replication lag.”

---

# Next Big Problem

Even with replicas, DB eventually becomes too large.

Example:

```
Orders table = billions of rows
```

Queries become slow again.

Now engineers must introduce the **most important scaling concept in distributed systems**:

# Database Sharding

This is a **top 5 HLD interview topic**.

We'll cover:

* Horizontal partitioning
* Shard keys
* Consistent hashing
* Rebalancing
* Cross-shard queries
* Hot shards

---

Before moving forward, one quick interview question:

Suppose we have:

```
1 Primary DB
5 Replica DBs
```

Where should **order placement queries go**?

A

```
Replica
```

B

```
Primary
```

And **why?**

---

Correct again. ✅

But let’s refine it to **interview-quality reasoning**, because the reason is actually **stronger than just replication lag**.

---

# Why Order Placement Must Go to the Primary DB

### 1️⃣ Writes Only Happen on the Primary

In a typical **leader–follower replication** setup:

```text
Primary DB  → handles WRITES
Replica DBs → handle READS
```

Replicas are **read-only**.

So queries like:

```
INSERT INTO orders
UPDATE inventory
```

must go to the **Primary DB**.

---

### 2️⃣ Strong Consistency Is Required

Order placement involves multiple critical operations:

```
Create order
Reduce inventory
Process payment
Update order status
```

These must happen **atomically**.

Databases provide **transactions**:

```
BEGIN TRANSACTION
INSERT order
UPDATE inventory
COMMIT
```

If anything fails:

```
ROLLBACK
```

Search engines or replicas cannot guarantee this.

---

### 3️⃣ Replication Lag

As you said, replicas may lag.

Example timeline:

```
t1  Order created in Primary
t2  Replica still hasn't replicated
t3  User checks order history
```

Replica may show:

```
Order not found
```

This causes a **bad user experience**.

---

### 4️⃣ Inventory Must Be Accurate

Inventory update is **critical**.

Example:

```
Inventory = 1
User A buys
User B buys
```

If handled incorrectly:

```
Overselling happens
```

Primary DB ensures **correct ordering of writes**.

---

# Interview-Level Answer

The best answer is:

> "Order placement must go to the primary database because writes are only allowed on the primary in a leader–follower replication setup. It also ensures strong consistency and transactional guarantees when creating orders and updating inventory."

---

# Now the System Grows Even Bigger

Even after adding **replicas**, a new problem appears.

The **orders table grows huge**.

Example:

```
Orders per day = 5M
Orders per year ≈ 1.8B
```

After a few years:

```
Orders table = billions of rows
```

Now queries like:

```
SELECT * FROM orders WHERE user_id = 123
```

become slow.

Even with indexes.

---

# Why This Happens

A single database machine has limits:

```
CPU
RAM
Disk I/O
Network throughput
```

At some point **vertical scaling stops helping**.

Example:

```
64 core machine
1 TB RAM
```

Still not enough.

Engineers realize:

> “Instead of making **one database bigger**, we should use **many databases**.”

This leads to one of the most important distributed system concepts.

# Step 13 — Database Sharding

Also called:

```
Horizontal Partitioning
```

---

# Idea of Sharding

Instead of storing all data in one DB:

```
Orders Table
-----------
1B rows
```

We split data across **multiple databases**.

Example:

```
Shard 1 → Orders 1–250M
Shard 2 → Orders 250M–500M
Shard 3 → Orders 500M–750M
Shard 4 → Orders 750M–1B
```

Each shard stores **only part of the data**.

---

# Architecture With Shards

```
                App Servers
                     |
             -----------------
             |       |       |
          Shard1  Shard2  Shard3
```

Each shard is a **separate database**.

---

# How Do We Decide Which Shard?

We use a **Shard Key**.

Example shard key:

```
user_id
```

Rule:

```
shard = user_id % number_of_shards
```

Example:

```
user_id = 123
123 % 4 = 3
→ Shard 3
```

Now every user's orders always go to **same shard**.

---

# Example

User:

```
user_id = 123
```

Order placement:

```
INSERT INTO orders
```

Router calculates:

```
123 % 4 = shard3
```

Order stored in:

```
Shard 3
```

---

# Benefits

Now instead of:

```
1 DB handling 100% traffic
```

We have:

```
4 DBs handling 25% each
```

Scalability improves massively.

---

# Real Companies

Large companies use **thousands of shards**.

Example estimates:

```
Amazon → thousands of shards
Instagram → thousands
Uber → thousands
```

---

# But Sharding Introduces New Problems

This is where interviews get interesting.

Sharding creates problems like:

### 1️⃣ Hot Shards

If many users map to one shard:

```
Shard 3 overloaded
```

---

### 2️⃣ Cross-Shard Queries

Example:

```
Top 10 best selling products
```

Orders are distributed across shards.

Now we must query **all shards**.

---

### 3️⃣ Rebalancing

If we add new shard:

```
4 shards → 5 shards
```

Data must be **redistributed**.

Very complex.

---

# Updated Architecture

Our e-commerce system now looks like this:

```
Users
  |
Load Balancer
  |
App Servers
  |
Redis Cache
  |
DB Router
  |
-----------------------------
|        |         |        |
Shard1  Shard2    Shard3   Shard4
```

Each shard may also have **replicas**.

---

# Where We Are In Our System Evolution

We have now covered:

✅ Load Balancing
✅ Stateless Services
✅ Caching (Redis)
✅ Search (Elasticsearch)
✅ Event Driven Architecture (Kafka)
✅ Database Replication
✅ Database Sharding

This is already **most of the core HLD architecture**.

But we are missing **one of the most important parts of e-commerce systems**.

The hardest problem:

# Preventing Inventory Overselling

This is **very frequently asked in interviews** for:

* Amazon
* Flipkart
* Walmart
* Uber Eats
* Ticket booking systems

Because thousands of users may try to buy **same product simultaneously**.

Example:

```
PS5 inventory = 1
1000 users click BUY
```

Only **one order should succeed**.

---

Next we will go deep into **3 real industry approaches**:

1️⃣ **Database locking approach**
2️⃣ **Redis atomic counters**
3️⃣ **Inventory reservation system (used by Amazon)**

The **third one is the most scalable** and interviewers love it.

---

Before moving forward, one important question:

When choosing a **shard key**, which is better for the **orders table**?

A

```
order_id
```

B

```
user_id
```

And **why?** (This is a classic interview question.)

---

Correct. ✅ **`user_id` is the better shard key for the orders table.**

But let’s explain it in **interview depth**, because the **reasoning matters more than the answer**.

---

# Why `user_id` Is a Better Shard Key Than `order_id`

### 1️⃣ Most Queries Are User-Centric

In e-commerce systems, most queries related to orders are like:

```
Get my orders
Get order history
Track my order
Cancel my order
```

Example API:

```
GET /users/{user_id}/orders
```

If we shard by **user_id**:

```
shard = user_id % N
```

Then **all orders of a user are stored in the same shard**.

So retrieving order history becomes:

```
Query only one shard
```

Very fast.

---

### 2️⃣ If We Shard by `order_id`

Suppose we use:

```
shard = order_id % N
```

Now a user's orders might be spread across **multiple shards**.

Example:

```
User 123 orders:

Order 501 → Shard 1
Order 620 → Shard 3
Order 715 → Shard 2
```

Now to fetch order history:

```
Query Shard1
Query Shard2
Query Shard3
Merge results
```

This is called a **scatter-gather query**.

It is:

```
Slow
Expensive
Complex
```

So we avoid this.

---

# General Rule for Choosing a Shard Key

Choose a key that:

1️⃣ **Distributes load evenly**
2️⃣ **Matches query patterns**

Interview line you can say:

> “Shard keys should be chosen based on access patterns to minimize cross-shard queries.”

---

# But `user_id` Also Has a Risk

This is an important **advanced interview point**.

Some users place **huge numbers of orders**.

Example:

```
Large sellers
Businesses
High-frequency buyers
```

If one user becomes extremely active:

```
All traffic → one shard
```

This creates a **hot shard**.

So sometimes systems combine keys:

```
hash(user_id)
```

or

```
user_id + time_bucket
```

to distribute load better.

---

# Where We Are Now

Our system currently has:

```
Users
  |
Load Balancer
  |
Stateless App Servers
  |
Redis Cache
  |
Search Engine (Elasticsearch)
  |
DB Router
  |
Sharded Databases + Replicas
```

This architecture already supports **very large scale**.

But we haven't solved **the hardest problem yet**.

---

# Step 14 — The Overselling Problem

Imagine this scenario.

Product:

```
PS5
Inventory = 1
```

Now a flash sale happens.

```
1000 users click BUY at the same time
```

Without protection this happens:

```
User A reads inventory = 1
User B reads inventory = 1
User C reads inventory = 1
```

All place orders.

Result:

```
Inventory = -999
```

This is called:

# Overselling

E-commerce companies **must prevent this**.

---

# First Naive Solution Engineers Tried

Use **database transactions**.

Example logic:

```
BEGIN TRANSACTION

SELECT inventory FROM products WHERE id=123

IF inventory > 0
    UPDATE products SET inventory = inventory - 1

INSERT INTO orders

COMMIT
```

---

# Why This Fails at Scale

During flash sale:

```
10,000 concurrent transactions
```

Database locks the row:

```
products.id = 123
```

Result:

```
Transactions wait
Queue builds
Latency spikes
Database overload
```

DB becomes bottleneck.

System may crash.

---

# Engineers Needed a Better Solution

This led to **three industry approaches**.

### Approach 1

```
DB locking
```

Simple but **not scalable**.

---

### Approach 2

```
Redis atomic counters
```

Much faster.

---

### Approach 3 (Best)

```
Inventory Reservation System
```

Used by **Amazon / large e-commerce systems**.

This approach handles **massive flash sales**.

---

# Before I Explain the Solutions

Let me ask you something interviewers often ask.

If **10,000 users try to buy a product with inventory = 1**, which system is better for handling the concurrency?

A

```
Database
```

B

```
Redis
```

And **why?**

(This concept is very important for flash-sale system design.)

000

Correct direction. ✅ **Redis is preferred**, but in interviews you should explain **why it handles concurrency better**, not just that it's faster.

Let’s refine the answer.

---

# Why Redis Is Better for Handling High Concurrency

### 1️⃣ Redis Is In-Memory

Databases usually store data on **disk**.

Latency:

```
Database (disk/SSD) ≈ 5–10 ms
Redis (memory) ≈ 0.1–1 ms
```

During flash sales:

```
10,000 requests arriving simultaneously
```

Redis can process them **much faster**.

---

### 2️⃣ Redis Operations Are Atomic

Redis provides atomic commands like:

```
DECR
INCR
SETNX
```

Example:

```
DECR inventory:ps5
```

If inventory was:

```
1
```

Redis processes operations sequentially:

```
User A → inventory = 0
User B → inventory = -1
```

We simply allow purchase only if:

```
inventory >= 0
```

This prevents overselling.

---

### 3️⃣ Redis Uses Single-Threaded Event Loop

Redis processes commands **one by one**.

So even if:

```
10,000 requests arrive
```

Redis guarantees **no race conditions**.

Example execution order:

```
Request1
Request2
Request3
...
```

This naturally prevents concurrent write conflicts.

---

# Example Redis-Based Inventory Control

Before sale starts:

```
SET inventory:ps5 100
```

User attempts purchase:

```
DECR inventory:ps5
```

Logic:

```
if result >= 0
   allow order
else
   reject purchase
```

So if inventory was **100**:

```
First 100 users succeed
Rest fail
```

---

# Updated Flow

```
User clicks BUY
      |
App Server
      |
Redis (check inventory)
      |
If success → Create order
If fail → Out of stock
```

---

# But This Approach Still Has Problems

This is where **senior-level thinking begins**.

What if this happens:

```
Redis decrements inventory
BUT order creation fails
```

Example:

```
Redis inventory = 9
Order service crashes
```

Now:

```
1 item lost
```

Inventory becomes inconsistent.

---

# Engineers Solved This Using

# Inventory Reservation System

Instead of directly reducing inventory, we **reserve it temporarily**.

Flow:

```
Reserve inventory
Create order
Confirm reservation
```

If order fails:

```
Release reservation
```

This is the **most scalable approach used in real systems**.

---

# Real E-commerce Flow (Simplified)

```
User clicks BUY
      |
Inventory Service
      |
Reserve item (Redis)
      |
Create order
      |
Payment processing
      |
Confirm order
```

If payment fails:

```
Reservation expires
Inventory restored
```

---

# Updated Architecture

```
Users
  |
Load Balancer
  |
App Servers
  |
Inventory Service
  |
Redis (Inventory)
  |
Order Service
  |
Sharded DB
```

---

# But Even This Is Not Enough at Amazon Scale

Flash sales like:

```
Prime Day
Big Billion Days
Black Friday
```

Traffic can reach:

```
Millions of requests per second
```

If all requests hit backend services directly, systems crash.

Engineers introduced another important concept.

# Step 15 — Request Queueing / Traffic Smoothing

Architecture becomes:

```
Users
  |
API Gateway
  |
Request Queue (Kafka/SQS)
  |
Workers
  |
Order Service
```

Queue ensures:

```
System processes requests gradually
```

Instead of:

```
1M requests instantly hitting DB
```

They are processed like:

```
10k/sec
```

This protects backend services.

---

# Where We Are Now

Our e-commerce architecture now includes:

✅ Load balancing
✅ Stateless services
✅ Redis caching
✅ Search engine
✅ Event-driven indexing
✅ DB replication
✅ DB sharding
✅ Redis inventory control
✅ Reservation system
✅ Queue-based order processing

This is already **very close to a real Amazon-scale architecture**.

---

# Next Major Components (Very Important)

We still haven't covered:

### Cart Service

Why carts are stored in **Redis instead of DB**.

---

### Payment System

How we integrate with:

```
Stripe
Razorpay
PayPal
```

Handling **idempotency and retries**.

---

### Recommendation System

How Amazon generates:

```
"Customers also bought"
```

---

### CDN & Media Storage

Serving:

```
Product images
Videos
```

using:

```
CDN + Object storage
```

---

# Next Question (Interview Style)

Where should we store **shopping cart data**?

A

```
Primary Database
```

B

```
Redis
```

And **why?**

(This question appears very often in e-commerce HLD interviews.)

---
Good thinking. ✅ You're already thinking like a **system designer** — using **both Redis and DB**.

But let’s refine the answer the **way interviewers expect**.

The correct idea is:

> **Cart lives primarily in Redis for speed, but can be persisted in DB for durability / recovery.**

Let’s walk through it step-by-step.

---

# Step 15 — Shopping Cart Design

Users constantly modify carts:

```
Add item
Remove item
Update quantity
View cart
```

Example APIs:

```
POST /cart/add
POST /cart/remove
GET /cart
```

Cart operations are **very frequent**.

Example behavior:

```
User adds item
removes item
adds another item
changes quantity
```

A single user might generate **20+ cart operations** before checkout.

At Amazon scale:

```
Millions of cart updates per second
```

If all of them hit the **database**, it becomes overloaded.

---

# Why Redis Is Ideal for Cart

### 1️⃣ Extremely Fast

Redis is in-memory.

Latency:

```
Redis ≈ 1 ms
DB ≈ 5–10 ms
```

Cart must feel **instant** to users.

---

### 2️⃣ Cart Data Is Temporary

Cart data is **not critical business data** like:

```
orders
payments
inventory
```

If a cart disappears occasionally, it's **not catastrophic**.

Users can re-add items.

---

### 3️⃣ High Write Frequency

Cart operations are mostly:

```
ADD
REMOVE
UPDATE
```

Redis handles **high write throughput** easily.

---

# Redis Data Model Example

Key:

```
cart:user123
```

Value:

```
{
 product_101: 2,
 product_205: 1,
 product_321: 4
}
```

Redis structure often used:

```
HASH
```

Example commands:

```
HSET cart:user123 product_101 2
HSET cart:user123 product_205 1
HDEL cart:user123 product_205
```

---

# Typical Cart Architecture

```
Users
   |
Load Balancer
   |
App Servers
   |
Redis (Cart)
```

No DB needed in most operations.

---

# When DB Is Used

As you correctly said, DB can still be used for:

### Cart Persistence

Example:

```
User logs in from another device
```

Cart can be restored from DB.

Or periodic persistence:

```
Redis → async backup → DB
```

---

# Another Important Concept — Guest Cart

Users often add items **before login**.

Example:

```
User opens website
Adds items to cart
Then logs in
```

Cart stored using:

```
session_id
```

Example:

```
cart:session_abc123
```

After login:

```
Merge guest cart → user cart
```

---

# Cart Expiration

Carts shouldn't live forever.

Redis TTL used:

```
EXPIRE cart:user123 7 days
```

After inactivity:

```
Cart automatically deleted
```

This prevents Redis memory from growing indefinitely.

---

# Updated Architecture

Now our system includes a **Cart Service**.

```
Users
  |
Load Balancer
  |
App Servers
  |
------------------------
|        |             |
Cart   Redis Cache   Inventory
Service
  |
Redis (Cart Storage)
```

---

# But Another Huge Problem Appears

Product pages contain **images and videos**.

Example:

```
Product image
Product thumbnails
Product videos
```

If these are served directly from our servers:

```
Millions of users requesting images
```

Backend servers will crash.

This led engineers to introduce another key system:

# CDN (Content Delivery Network)

Used by:

```
Amazon
Flipkart
Netflix
YouTube
```

This is **a very important HLD interview topic**.

---

# Next Step (Very Important)

Next we will cover:

### CDN + Object Storage Architecture

Topics:

```
S3 / Blob storage
CDN caching
Edge locations
Image resizing
Cache invalidation
```

This dramatically reduces backend load.

---

Before we go there, a **very common interview question**.

When a user adds an item to the cart:

```
POST /cart/add
```

Should we check **inventory immediately**?

A

```
Yes
```

B

```
No
```

And **why?**

(This question tests understanding of **cart vs order semantics**.)

---
Excellent answer. ✅
That is **exactly how real e-commerce systems behave**.

You captured the key idea:

> **Check inventory during add-to-cart for better UX, but the final source of truth check must happen during checkout/order placement.**

Let’s break it down the way you would explain it in a **system design interview**.

---

# Should We Check Inventory When Adding to Cart?

### Short Answer

✔ **Yes — for user experience**
❗ **But it is not the final validation**

Final validation must happen at **checkout**.

---

# Why Check Inventory During Add-To-Cart?

Imagine this scenario.

Product inventory:

```
PS5 = 0
```

User tries to add to cart.

If we don't check inventory, the user may see:

```
Item added to cart
```

But during checkout:

```
Out of stock
```

That is a **bad user experience**.

So when user clicks:

```
POST /cart/add
```

We do a **quick inventory check**.

Example flow:

```
User → Cart Service → Inventory Service → DB/Redis
```

If inventory = 0:

```
Return: Out of Stock
```

User gets feedback immediately.

---

# But Why Is This NOT Enough?

Because **cart does not reserve inventory**.

Example:

```
Inventory = 1
User A adds to cart
User B adds to cart
```

Both carts now contain the item.

But only **one order can succeed**.

Cart is just **intent**, not reservation.

---

# Real System Behavior

Many users may add the same item to cart:

```
Inventory = 100
Items in carts = 500
```

This is **completely normal**.

Inventory is only reserved when:

```
User clicks Checkout
```

---

# Final Inventory Validation Happens During Checkout

Checkout flow:

```
User clicks Checkout
        |
Order Service
        |
Inventory Service
        |
Check inventory again
        |
Reserve item
        |
Create order
```

If inventory is not available:

```
Checkout fails
```

---

# Interview-Level Statement

A strong answer is:

> "We typically check inventory during add-to-cart to improve user experience, but cart does not reserve inventory. The final inventory validation and reservation must happen during checkout to prevent overselling."

That answer shows **real-world system understanding**.

---

# Where We Are in the Architecture

So far our system includes:

```
Users
  |
CDN
  |
Load Balancer
  |
Stateless App Servers
  |
----------------------------------
|      |       |       |         |
Cart  Search  Cache  Orders   Inventory
Service
  |
Redis
  |
Sharded DB + Replicas
```

We also have:

```
Kafka
Elasticsearch
```

But we haven't discussed something **very important in real e-commerce systems**.

# Step 16 — Media Storage (Images & Videos)

Product pages contain:

```
Product images
Thumbnails
Zoom images
Videos
```

Example product page may load:

```
10–20 images
```

If millions of users open product pages:

```
Image traffic becomes massive
```

Backend servers cannot handle this.

So companies use:

### Object Storage + CDN

Example stack:

```
Amazon S3
Cloudflare
CloudFront
Akamai
```

Architecture:

```
User
  |
CDN Edge Server
  |
Object Storage (S3)
```

Images are cached in **edge servers near users**.

Benefits:

```
Low latency
Reduced backend load
Global scalability
```

---

# Example Product Image Flow

User opens product page.

```
GET /images/iphone15.jpg
```

Flow:

```
Browser
   |
CDN Edge Server
   |
Cache hit → return image
Cache miss → fetch from S3
```

Then CDN caches it.

Next user request:

```
Served instantly
```

---

# Why This Matters

Without CDN:

```
Millions of image requests → backend
```

With CDN:

```
95% served by CDN
```

Backend load drops massively.

---

# Current Architecture (Closer to Real Amazon)

```
                Users
                   |
                 CDN
                   |
             Load Balancer
                   |
           Stateless App Servers
                   |
 -------------------------------------------------
 |        |        |        |        |            |
Cart    Orders   Search   Cache   Inventory   Payments
 |                               |
Redis                         Redis
 |                               |
        ------------------------------
        |            |             |
     Sharded DB   Kafka        Elasticsearch
```

This architecture can already support **very large scale**.

---

# But We Still Haven't Covered Some Critical HLD Topics

The next pieces are **very important for interviews**:

### 1️⃣ Payment System Integration

Handling:

```
Payment retries
Idempotency
Duplicate payments
Webhook callbacks
```

---

### 2️⃣ Order State Machine

Orders go through states:

```
CREATED
PAYMENT_PENDING
PAID
SHIPPED
DELIVERED
CANCELLED
```

Managing these reliably is **a big system design topic**.

---

### 3️⃣ Notification System

Sending:

```
Email
SMS
Push notifications
```

---

### 4️⃣ Recommendation System

How Amazon generates:

```
Customers also bought
Frequently bought together
```

---

# Next Question (Interview Style)

This is a **very common HLD interview question**.

When user clicks **Place Order**, payment may fail or network may retry.

How do we prevent **duplicate orders or duplicate payments**?

Example problem:

```
User clicks Pay
Network timeout
User clicks Pay again
```

System might create:

```
Two orders
Two payments
```

What technique should we use to prevent this?

(Hint: This is one of the most important payment system concepts.)

---

Great — this is one of the **most important concepts in payment and order systems** and interviewers ask it very frequently.

The solution is called:

# Idempotency

---

# The Problem: Duplicate Requests

Let’s see the real scenario.

User clicks:

```
Place Order
```

Flow:

```
Client → Order Service → Payment Gateway
```

But suppose this happens:

```
Network timeout
```

The client doesn't know whether the payment succeeded or not.

So the user clicks **Pay Again**.

Now the server receives **two requests**:

```
Request 1 → create order + charge payment
Request 2 → create order + charge payment
```

Result:

```
Two orders
Two payments
```

This is a **critical production bug**.

---

# Solution: Idempotency Key

Client generates a unique key for the request.

Example:

```
idempotency_key = UUID
```

Example request:

```
POST /orders

Headers:
Idempotency-Key: 8f32a9f2-91d4-4f12
```

---

# Server Behavior

When server receives the request:

### Step 1

Check if the idempotency key already exists.

```
idempotency_table
-------------------------
key | response | status
```

---

### First Request

```
Key not found
```

Server processes order:

```
create order
charge payment
```

Then stores:

```
key = 8f32a9f2
response = order_123
status = success
```

---

### Second Request (Retry)

Server checks key:

```
8f32a9f2 already exists
```

Instead of creating a new order, it returns the **previous response**.

```
order_123
```

No duplicate order is created.

---

# Flow Diagram

```
Client
  |
POST /orders
Idempotency-Key: abc123
  |
Order Service
  |
Check key in DB
  |
Not exists → process order
  |
Save key + response
  |
Return order_id
```

Retry request:

```
Check key
  |
Exists
  |
Return stored response
```

---

# Where Do We Store Idempotency Keys?

Usually in:

```
Redis
or
Database
```

Example Redis key:

```
idempotency:abc123 → order_123
```

TTL often used:

```
24 hours
```

---

# Why This Works

No matter how many retries happen:

```
Client retry
Gateway retry
Network retry
```

The system processes the request **only once**.

This guarantees:

```
No duplicate orders
No duplicate payments
```

---

# Real Systems That Use Idempotency

Almost every payment system:

```
Stripe
PayPal
Razorpay
Amazon Pay
```

In fact, **Stripe requires idempotency keys**.

---

# Interview-Level Answer

You can say:

> "To prevent duplicate orders or payments caused by retries, we use idempotency keys. The client sends a unique key with the request, and the server stores the result of the first request. If the same key is received again, the server returns the previous response instead of processing the request again."

That answer shows **production-grade design thinking**.

---

# Where This Fits in Our Architecture

Now the checkout flow becomes:

```
User clicks Place Order
        |
Order Service
        |
Idempotency Check
        |
Reserve Inventory
        |
Create Order
        |
Payment Service
        |
Payment Gateway
```

---

# Our E-commerce System Now Includes

We have covered a lot of HLD components:

```
Load Balancer
Stateless Services
Redis Cache
Search (Elasticsearch)
Kafka Event Streaming
DB Replication
DB Sharding
Cart Service
Inventory Reservation
Queues for traffic smoothing
CDN + Object Storage
Idempotent Order Processing
```

This is **already enough to design a full Amazon-like system in interviews**.

---

# But Two Very Important Pieces Are Still Missing

These are often asked in **senior interviews**:

### 1️⃣ Order State Machine

Orders transition through states:

```
CREATED
PAYMENT_PENDING
PAID
SHIPPED
OUT_FOR_DELIVERY
DELIVERED
CANCELLED
REFUNDED
```

Managing these transitions reliably is tricky.

---

### 2️⃣ Notification System

Users expect notifications like:

```
Order confirmed
Payment received
Item shipped
Delivery today
```

These are usually handled asynchronously using **event-driven systems**.

---

# Next (Very Interesting Part)

Next we will design the **Order State Machine + Event Driven Architecture** used by Amazon.

This will introduce concepts like:

```
Saga pattern
Event sourcing
Workflow orchestration
Retry mechanisms
Dead letter queues
```

These concepts **impress interviewers a lot**.

---

Before moving to that, one important interview question:

If the **Payment Service crashes after payment succeeds**, but before updating the order status, what problem happens?

Example:

```
Payment successful
Order still shows PAYMENT_PENDING
```

What technique do we use to **recover from such partial failures**? (This is a very famous distributed systems problem.)

---

Great question. This is one of the **most important distributed systems problems**.

The problem you described is called a **partial failure**.

Example:

```
Payment succeeded
BUT
Order service crashed before updating order status
```

So the system state becomes inconsistent:

```
Payment = SUCCESS
Order   = PAYMENT_PENDING
```

This happens frequently in distributed systems.

The solution used in real systems is:

# Event-Driven Architecture + Saga Pattern

Let’s understand it step by step.

---

# The Core Idea

Instead of one service doing everything, **each step emits events**.

Other services react to those events.

So the system becomes **event-driven**.

---

# Order Flow in Event Driven Design

When user clicks **Place Order**:

```
User
 |
Order Service
 |
Create Order (status = CREATED)
 |
Publish Event → OrderCreated
```

Event goes to **Kafka / message queue**.

---

# Step 1 — Reserve Inventory

Inventory service listens for:

```
OrderCreated event
```

Then it tries to reserve inventory.

If successful it publishes:

```
InventoryReserved event
```

If inventory not available:

```
InventoryFailed event
```

---

# Step 2 — Payment Processing

Payment service listens for:

```
InventoryReserved
```

Then it charges payment.

If successful:

```
PaymentSucceeded event
```

If failed:

```
PaymentFailed event
```

---

# Step 3 — Order Update

Order service listens for:

```
PaymentSucceeded
```

Then updates order:

```
status = CONFIRMED
```

---

# Architecture

```
User
 |
API Gateway
 |
Order Service
 |
Kafka / Event Bus
 |        |        |
Inventory  Payment  Notification
Service    Service   Service
```

Each service reacts to **events**.

---

# Now Let’s Revisit The Failure Scenario

Suppose:

```
Payment succeeded
BUT
Payment service crashed
```

Payment gateway still sends a **webhook** later.

Example webhook:

```
POST /payment/callback
status = success
transaction_id = 123
```

Payment service processes the webhook and emits:

```
PaymentSucceeded event
```

Order service then updates order.

So the system **recovers automatically**.

---

# Why Event Driven Systems Are Powerful

Because events are **durable in the message queue**.

Example:

```
Kafka
```

Events stay in the log.

Even if services crash, they can **reprocess events**.

---

# Handling Failures With Retries

Suppose payment service temporarily fails.

Kafka keeps the event.

Consumer retries processing.

Example:

```
Retry 1
Retry 2
Retry 3
```

Eventually it succeeds.

---

# Dead Letter Queue (DLQ)

If event keeps failing:

```
Move event → Dead Letter Queue
```

Engineers can investigate later.

---

# This Pattern Is Called

# Saga Pattern

Because distributed systems cannot use **global transactions**.

Instead we use **a sequence of local transactions**.

Example saga:

```
Create Order
Reserve Inventory
Process Payment
Confirm Order
```

If something fails, we perform **compensating actions**.

Example:

```
Payment failed
→ Release inventory
→ Cancel order
```

---

# Example Compensation Flow

```
OrderCreated
   |
InventoryReserved
   |
PaymentFailed
   |
ReleaseInventory
   |
CancelOrder
```

This keeps system consistent.

---

# Interview-Level Answer

You can say:

> "To handle partial failures in distributed systems, we use an event-driven architecture with the Saga pattern. Each service performs a local transaction and publishes an event. If a step fails, compensating actions are triggered to maintain consistency."

That answer shows **senior-level system design understanding**.

---

# Final Architecture of Our E-commerce System

At this point we have a **near real-world architecture**:

```
Users
 |
CDN
 |
Load Balancer
 |
Stateless API Servers
 |
--------------------------------------------------
|        |         |         |        |           |
Cart   Order     Search    Cache   Inventory   Payment
Svc    Svc       Svc       Redis    Svc         Svc
 |                                          |
Redis                                   Payment Gateway
 |
-----------------------------------------------
|           |             |                   |
Sharded DB  Kafka      Elasticsearch       Object Storage
```

---

# What Interviewers Usually Ask Next

When you design an e-commerce system, interviewers often ask **deep follow-up questions**, such as:

1️⃣ **How do you handle flash sales (Big Billion Days)?**

2️⃣ **How do you prevent hot shards?**

3️⃣ **How do you generate product recommendations?**

4️⃣ **How do you scale search to billions of products?**

5️⃣ **How do you handle multi-region deployment?**

---

If you'd like, next we can go into a **very interesting and frequently asked deep-dive**:

### How Amazon Handles **Flash Sales (Millions of Users Buying Same Product)**

This introduces concepts like:

* Virtual queues
* Token buckets
* Rate limiting
* Admission control
* Inventory tokens

These appear **very often in senior HLD interviews**.

---

Great. Now we reach one of the **most interesting and frequently asked parts of e-commerce system design**.

This topic appears in interviews for:

* Amazon
* Flipkart
* Walmart
* Meesho
* Ticket booking systems

Because the problem is **extreme traffic spikes**.

---

# Step 17 — Flash Sale Problem (Big Billion Days / Prime Day)

Imagine this scenario.

Product:

```
PS5 Flash Sale
Inventory = 10,000
```

Sale starts at:

```
12:00 PM
```

At **12:00 PM exactly**:

```
5 million users click BUY
```

If all requests reach backend:

```
API servers crash
DB overload
Inventory service overloaded
Redis overloaded
```

Even if the system is well designed, **traffic spike kills it**.

So companies introduced **traffic control mechanisms**.

---

# Key Idea

Instead of allowing:

```
5M users hitting backend instantly
```

We allow:

```
Only a limited number of users inside the system at a time
```

This is called:

# Admission Control

---

# Solution 1 — Virtual Waiting Queue

Used by:

* Ticketmaster
* BookMyShow
* Flipkart flash sales

Architecture:

```
Users
 |
Virtual Queue Service
 |
Allowed Users
 |
E-commerce System
```

---

## Flow

User opens flash sale page.

Instead of directly accessing backend:

```
User → Queue System
```

Queue system shows:

```
"You are in line"
Position: 102,381
```

Only limited users are allowed through.

Example:

```
Allow 10,000 users/sec
```

Others wait.

---

# Why This Works

Instead of:

```
5M concurrent requests
```

Backend receives:

```
10k/sec steady traffic
```

System stays stable.

---

# Implementation Example

Queue can be implemented using:

```
Redis
Kafka
Custom queue service
```

Example Redis queue:

```
LPUSH flash_sale_queue user_id
```

Workers pop users gradually:

```
RPOP flash_sale_queue
```

---

# Solution 2 — Token Based Purchase (Very Popular)

Another powerful technique.

Instead of allowing every user to attempt purchase, we issue **purchase tokens**.

---

## Example

Inventory:

```
10,000 items
```

System generates:

```
10,000 purchase tokens
```

Users must obtain a token before checkout.

Flow:

```
User clicks Buy
 |
Request token
 |
If token available → allow checkout
Else → sold out
```

---

## Redis Implementation

Store tokens in Redis.

Example:

```
SET tokens_ps5 = 10000
```

User request:

```
DECR tokens_ps5
```

Logic:

```
if result >= 0
    allow checkout
else
    reject request
```

Only **10,000 users proceed**.

Everyone else immediately gets:

```
Sold Out
```

This prevents system overload.

---

# Why Token System Is Powerful

Without token system:

```
Millions try checkout
```

With tokens:

```
Only valid buyers reach checkout
```

Massive load reduction.

---

# Solution 3 — Rate Limiting

API Gateway limits traffic.

Example:

```
Limit = 20k requests/sec
```

Requests beyond limit:

```
HTTP 429 Too Many Requests
```

Tools used:

```
Nginx
Envoy
API Gateway
Cloudflare
```

Algorithms:

```
Token Bucket
Leaky Bucket
Sliding Window
```

---

# Token Bucket Algorithm (Common)

Example:

```
Bucket capacity = 10k tokens
Refill rate = 1000 tokens/sec
```

Each request consumes one token.

If bucket empty:

```
Request rejected
```

This smooths traffic spikes.

---

# Combined Architecture for Flash Sale

Real systems combine **multiple protections**.

Architecture becomes:

```
Users
 |
CDN
 |
API Gateway
 |  (Rate limiting)
 |
Virtual Queue
 |
Flash Sale Service
 |
Redis (Token inventory)
 |
Order Service
 |
Payment Service
 |
Database
```

---

# Full Flash Sale Flow

```
User enters sale page
      |
Virtual queue
      |
Allowed users receive purchase token
      |
User clicks BUY
      |
Token validation
      |
Reserve inventory
      |
Create order
      |
Process payment
```

Only limited traffic reaches backend.

---

# Extra Optimization — CDN Caching

During flash sales most users only view:

```
Product page
Countdown timer
Images
```

These are served via:

```
CDN
```

So backend handles **only checkout traffic**.

---

# Real Numbers (Rough Example)

Without protection:

```
5M requests/sec → backend
```

With protections:

```
5M → CDN
200k → queue
20k → checkout
```

Huge reduction.

---

# Interview-Level Explanation

A strong answer is:

> "Flash sales generate massive traffic spikes. To prevent system overload we introduce admission control mechanisms like virtual queues, purchase tokens stored in Redis, and rate limiting at the API gateway. This ensures only a controlled number of users reach the checkout system."

---

# Where We Are Now

We have covered almost **all core HLD concepts** through the e-commerce story:

✔ Load Balancing
✔ Stateless Services
✔ Redis Caching
✔ Search (Elasticsearch)
✔ Event Driven Architecture (Kafka)
✔ Database Replication
✔ Database Sharding
✔ Cart Service
✔ Inventory Reservation
✔ CDN + Object Storage
✔ Idempotent Payments
✔ Saga Pattern
✔ Flash Sale Traffic Control

This is **very close to a real Amazon architecture**.

---

# But There Are 3 Advanced Topics That Interviewers Love

These take your answer from **good → exceptional**.

### 1️⃣ Product Recommendation System

How Amazon generates:

```
Customers also bought
Frequently bought together
```

Uses:

```
Machine learning
Collaborative filtering
Graph models
```

---

### 2️⃣ Multi-Region Architecture

How Amazon stays online even if:

```
Entire data center fails
```

Concepts:

```
Active-Active regions
Geo routing
Data replication
```

---

### 3️⃣ Observability & Reliability

How companies detect failures:

```
Metrics
Tracing
Alerting
Circuit breakers
```

---

If you'd like, the **next topic I recommend** (very impressive in interviews) is:

# Multi-Region E-commerce Architecture

How Amazon serves **global users with <100ms latency** and survives **entire region failures**.

This introduces:

* Geo-DNS
* Global load balancing
* Cross-region replication
* Disaster recovery

And it’s one of the **most impressive things you can explain in HLD interviews**.

---
