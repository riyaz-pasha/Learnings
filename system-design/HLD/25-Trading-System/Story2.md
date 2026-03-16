Great question, Riyaz. This is **exactly the right way to learn HLD for interviews** — understanding *why systems evolved*, not just memorizing architectures. That’s how you answer confidently in interviews.

We’ll build the **Trading Platform** the same way real systems evolved (like Zerodha, Robinhood, Binance, NYSE systems).

We will go **step-by-step like a story**, and **only move forward when you’re comfortable**.

---

# Story: How Trading Platforms Evolved

Imagine it is **2005**.

A small fintech startup wants to build an **online trading platform**.

Users should be able to:

• See stock prices
• Place buy/sell orders
• Track their portfolio

Very simple.

So the team builds the **first version**.

---

# Step 1 — The Simplest Possible Trading System

### Requirements

Users should be able to:

1. Login
2. View stock prices
3. Place buy order
4. Place sell order
5. See order status

Example:

User places order:

```
BUY 10 shares of AAPL at $150
```

---

# First Architecture (Naive Design)

```
Users
  |
  v
Load Balancer
  |
  v
Trading Server
  |
  v
Database
```

Trading server handles everything:

• authentication
• order placement
• matching
• portfolio updates
• price updates

---

# Example Flow

User places order:

```
BUY 10 AAPL @ $150
```

Flow:

```
User → API → Trading Server → DB
```

Trading server:

1️⃣ Validate user balance
2️⃣ Store order
3️⃣ Match with sell orders
4️⃣ Execute trade
5️⃣ Update portfolio

---

# Database Tables (First Version)

### Users

```
users
------
id
name
balance
```

### Orders

```
orders
------
id
user_id
stock
type (BUY/SELL)
price
quantity
status
```

### Trades

```
trades
------
id
buy_order_id
sell_order_id
price
quantity
timestamp
```

### Portfolio

```
portfolio
---------
user_id
stock
quantity
```

---

# Order Matching (Very Basic)

When user places buy order:

```
BUY AAPL $150 qty 10
```

Server checks:

```
Is there a SELL order ≤ $150 ?
```

Example:

```
SELL AAPL $149 qty 10
```

Trade executes.

---

# Execution Example

Order book:

```
SELL ORDERS
-----------
AAPL $149 qty 10
AAPL $151 qty 20
```

User places:

```
BUY AAPL $150 qty 10
```

Matching engine finds:

```
SELL $149
```

Trade executes.

---

# Why This Works Initially

This architecture works when:

• 100 users
• 10 orders/sec

Because:

```
Everything runs on ONE server
```

Very simple.

---

# But Then The Platform Grows

The company launches publicly.

Suddenly:

```
Users = 2 million
Orders = 50,000 per second
Price updates = 1 million/sec
```

Now the system breaks.

---

# Problems Start Appearing

### Problem 1 — Single Server Bottleneck

Everything runs on:

```
Trading Server
```

So CPU becomes overloaded.

Requests queue up.

Users see:

```
Order failed
Timeout
```

---

### Problem 2 — Database Bottleneck

Every order does:

```
write order
update portfolio
write trade
```

Database becomes overloaded.

---

### Problem 3 — Matching Engine is Slow

Matching logic is expensive.

If thousands of orders arrive:

```
BUY
BUY
BUY
SELL
SELL
BUY
SELL
```

Matching becomes slow.

---

### Problem 4 — System Crash = Trading Stops

If the trading server crashes:

```
Entire platform is down
```

Which is unacceptable.

---

# Interview Insight

At this stage interviewers expect you to say:

> The monolithic architecture becomes a bottleneck. We must split responsibilities.

---

# Before Moving Further — Key Question

A **real trading platform** has **ONE critical component**.

Everything revolves around it.

That component is the **Matching Engine**.

Example systems:

• NASDAQ
• NYSE
• Binance
• Coinbase

All are built around **matching engines**.

---

# Before we go further, let's pause here.

Because **the next step is the most important part of trading systems**.

We will discuss:

### Next Step (Very Important)

```
How a REAL Matching Engine Works
```

Topics we will cover next:

• Order book design
• Price-time priority
• Buy/sell queues
• Data structures used
• Latency requirements
• How exchanges process 1M orders/sec

---

# Quick Check for You

Before I continue, tell me:

1️⃣ Do you want the **Matching Engine explained first** (core of trading systems)?

or

2️⃣ Do you want to first discuss **how to break the monolith into services**?

For **interviews**, the best path is:

```
Matching Engine → Order Book → Scaling → Sharding → Replication → Fault tolerance
```

Once we go into **Matching Engine**, the design becomes **much more interesting**.

---
Perfect. Now we move to the **heart of every trading platform**.

If you deeply understand this part, you will already be **ahead of 90% of candidates in HLD interviews**.

---

# Step 2 — The Matching Engine (Heart of a Trading System)

Imagine the company grew.

Now there are:

```
5 million users
200k orders per second
```

The earlier architecture breaks because **order matching is the hardest part**.

So engineers asked:

> "What is the single most important component in a trading system?"

Answer:

```
Matching Engine
```

Everything else exists to **feed orders into the matching engine**.

---

# Real Exchanges Work Like This

In systems like:

• NASDAQ
• NYSE
• Binance
• Coinbase

Architecture roughly becomes:

```
Users
  |
  v
API Servers
  |
  v
Order Gateway
  |
  v
Matching Engine
  |
  v
Trade Events
  |
  v
Portfolio / Risk / Notifications
```

The **Matching Engine does ONLY ONE JOB**:

```
Match BUY orders with SELL orders
```

Nothing else.

---

# Step 3 — What Is an Order Book?

Every stock has its own **Order Book**.

Example:

```
Stock: AAPL
```

Order book contains:

```
BUY ORDERS (Bids)
SELL ORDERS (Asks)
```

Example:

```
BUY SIDE (Bids)

Price     Quantity
150       10
149       20
148       50


SELL SIDE (Asks)

Price     Quantity
151       10
152       15
153       30
```

Key rule:

```
BUY orders sorted DESCENDING
SELL orders sorted ASCENDING
```

Why?

Because traders want:

```
Highest BUY
Lowest SELL
```

---

# Example Order Matching

User places:

```
BUY AAPL @ $151 qty 10
```

Matching engine checks:

```
Is there a SELL ≤ $151 ?
```

Order book:

```
SELL

151 qty 5
152 qty 10
```

Result:

```
BUY 10 @151
```

Execution:

```
match 5 @151
remaining buy = 5
```

Next SELL:

```
152 > 151
```

So matching stops.

Remaining order stays in book.

---

# Order Book After Matching

```
BUY

151 qty 5   <-- remaining


SELL

152 qty 10
```

---

# Interview Insight

This rule is called:

```
Price-Time Priority
```

Meaning:

1️⃣ Best price first
2️⃣ If same price → earliest order first

Example:

```
BUY orders

150  (placed 10:01)
150  (placed 10:02)
```

Execution order:

```
10:01 first
10:02 next
```

This is **mandatory for fairness** in exchanges.

---

# Step 4 — Data Structures for Order Book

Interviewers LOVE this question.

A naive candidate says:

```
store in DB
```

But that's wrong.

Matching engine must be **EXTREMELY FAST**.

Typical requirement:

```
Latency < 1 millisecond
```

Database is too slow.

So we store the order book **in memory**.

---

# Data Structure Used

Most exchanges use something like:

```
TreeMap<price, Queue<orders>>
```

or

```
Red Black Tree
```

Structure:

```
BUY SIDE

price -> queue
```

Example:

```
150 -> [order1, order2]
149 -> [order3]
148 -> [order4]
```

Each price has a **FIFO queue**.

---

# Example

BUY orders:

```
BUY 150 qty10 (user1)
BUY 150 qty5  (user2)
```

Stored as:

```
150 -> queue

[user1, user2]
```

Matching pops from front.

---

# Complexity

Operations must be fast.

```
Insert order  : O(log n)
Match order   : O(log n)
Remove order  : O(log n)
```

Where `n` = price levels.

This allows **hundreds of thousands orders/sec**.

---

# Step 5 — Matching Engine Flow

When order arrives:

```
BUY 100 AAPL @150
```

Flow:

```
Receive Order
      |
Check Order Book
      |
Match With Sell Orders
      |
Generate Trades
      |
Update Order Book
      |
Emit Trade Events
```

Important:

```
Matching engine is SINGLE THREADED
```

This surprises many engineers.

---

# Why Single Thread?

Because of **consistency**.

If multiple threads match orders:

```
Thread1 → matches order
Thread2 → matches same order
```

You get:

```
Double execution
```

Which is catastrophic.

So exchanges do:

```
Single threaded matching
```

But optimized extremely heavily.

---

# Throughput of Real Systems

Example:

NASDAQ:

```
>1M orders/sec
```

Latency:

```
~100 microseconds
```

They achieve this by:

```
C++ matching engine
In-memory order book
Lock-free structures
```

---

# Step 6 — Trade Events

After match occurs:

```
BUY 10 @150
SELL 10 @150
```

Trade event generated:

```
TradeEvent
---------
symbol
price
qty
buyOrderId
sellOrderId
timestamp
```

Event is published to:

```
Kafka / Event Bus
```

Consumers:

```
Portfolio Service
Notification Service
Market Data Service
Analytics
```

This is called:

```
Event Driven Architecture
```

---

# System Now Looks Like This

```
Users
  |
  v
API Servers
  |
  v
Order Gateway
  |
  v
Matching Engine
  |
  v
Event Bus
  |
  v
Consumers
```

---

# Interview Gold Point

Never allow external systems to call matching engine directly.

Instead:

```
Order Gateway
```

Why?

Because gateway handles:

```
authentication
validation
rate limiting
risk checks
```

---

# But We Still Have BIG Problems

Even with matching engine, new issues appear.

---

# Problem 1 — Single Matching Engine

If engine crashes:

```
Trading stops
```

Bad.

---

# Problem 2 — Millions of Stocks

Each stock has its own order book.

Example:

```
AAPL
GOOG
MSFT
TSLA
BTC
ETH
```

Single engine can't handle everything.

---

# Problem 3 — Order Gateway Overload

Millions of users sending orders.

Gateway becomes bottleneck.

---

# The Next Evolution

Engineers solved this with:

```
Partitioning the Matching Engines
```

This introduces one of the **most important system design concepts**.

```
Sharding
```

But not normal DB sharding.

This is **Symbol-based sharding**.

---

# Next Step (Very Important)

Next we will cover:

```
Sharding the Matching Engines
```

This includes:

• symbol partitioning
• routing orders correctly
• horizontal scaling
• consistent hashing
• hot stock problem

This is **exactly the level FAANG interviews expect**.

---

Before I continue, one quick check:

Do you want me to also explain:

1️⃣ **How Binance / NASDAQ architecture actually looks internally**

or

2️⃣ Continue the **story evolution → sharding the matching engine**

Both are extremely useful for interviews.

---
Perfect. Now we continue the **story evolution**.

You already understand:

• Matching Engine
• Order Book
• Price-Time Priority
• In-memory data structures
• Event-driven architecture

Now the system grows further.

---

# Step 3 — The System Breaks Again

Our platform is successful.

Traffic grows:

```
Users: 10 million
Orders/sec: 500k
Symbols: 8000+
```

But we only have **one matching engine**.

Architecture now:

```
Users
   |
API Servers
   |
Order Gateway
   |
Matching Engine
   |
Event Bus
```

Soon we see problems.

---

# Problem 1 — CPU Bottleneck

Every order goes to the **same engine**.

Example incoming orders:

```
BUY AAPL
BUY TSLA
SELL MSFT
BUY GOOG
SELL AAPL
BUY NVDA
```

The engine processes them **one by one**.

Even if AAPL and TSLA are unrelated.

This wastes CPU capacity.

---

# Problem 2 — Single Point of Failure

If the matching engine crashes:

```
Entire trading platform stops
```

Exchanges cannot allow this.

---

# Problem 3 — Memory Limits

Remember:

```
Order books are stored in memory
```

If we have:

```
8000 stocks
millions of orders
```

Memory becomes huge.

---

# The Big Realization

Engineers realize something important.

Orders for different stocks are **independent**.

Example:

```
AAPL orders never interact with TSLA orders
```

So why process them in the same engine?

---

# Solution — Symbol-Based Sharding

Instead of:

```
1 Matching Engine
```

We create:

```
Many Matching Engines
```

Each engine handles **a subset of symbols**.

Architecture becomes:

```
                  +----------------+
Users → API → Gateway → Router
                  |
     -------------------------------------
     |            |           |           |
 Engine 1      Engine 2    Engine 3    Engine 4
 (A–F)         (G–M)       (N–T)       (U–Z)
```

Each engine maintains its **own order books**.

---

# Example

Suppose we shard like this:

```
Engine 1 → AAPL, AMD, AMZN
Engine 2 → GOOG, META
Engine 3 → TSLA, NVDA
```

Orders are routed accordingly.

Example:

```
BUY AAPL → Engine 1
SELL TSLA → Engine 3
BUY GOOG → Engine 2
```

Now engines can run **in parallel**.

---

# Throughput Improvement

Before:

```
1 engine → 50k orders/sec
```

After sharding:

```
10 engines → 500k orders/sec
```

Linear scaling.

This is **horizontal scaling**.

---

# But Now a New Problem Appears

How does the **Gateway know where to send orders?**

Example:

```
BUY AAPL
```

Which engine?

We need a **routing strategy**.

---

# Simple Approach — Symbol Lookup Table

We maintain a table:

```
Symbol → Engine
```

Example:

```
AAPL → Engine 1
GOOG → Engine 2
TSLA → Engine 3
```

Order flow:

```
User Order
   |
Gateway
   |
Lookup symbol
   |
Send to correct engine
```

Very simple.

---

# But Then System Grows Again

New stocks get listed every day.

Example:

```
New IPO → ZOOM
New IPO → OPENAI
New IPO → ARM
```

If we hardcode mapping:

```
Symbol → Engine
```

We must constantly update routing tables.

Operational nightmare.

---

# Better Solution — Consistent Hashing

Instead of manual mapping:

```
engine = hash(symbol) % N
```

Example:

```
hash(AAPL) → Engine 3
hash(TSLA) → Engine 1
hash(GOOG) → Engine 2
```

This distributes load **automatically**.

---

# But Even This Has a Problem

Imagine we add a new engine.

Before:

```
3 engines
```

Now:

```
4 engines
```

Hash result changes.

Example:

```
hash(AAPL) % 3 → Engine 1
hash(AAPL) % 4 → Engine 3
```

Now AAPL order book moves.

But the **order book is in memory**.

Migration becomes difficult.

---

# Real Solution — Consistent Hash Ring

Instead of:

```
hash(symbol) % N
```

We use:

```
Consistent Hashing Ring
```

Concept:

```
Hash space: 0 — 360
```

Example:

```
Engine1 → 50
Engine2 → 150
Engine3 → 250
```

Symbols also hash to ring.

Example:

```
AAPL → 120
TSLA → 200
GOOG → 20
```

Routing rule:

```
Symbol goes to next engine clockwise
```

Example:

```
AAPL(120) → Engine2
TSLA(200) → Engine3
GOOG(20) → Engine1
```

Now if a new engine is added:

```
Only small subset moves
```

Which makes scaling easier.

---

# Interview Insight

Most candidates say:

```
Shard by user_id
```

But trading systems shard by:

```
symbol
```

Because matching must happen **within the same engine**.

If buy order is in engine1 and sell order in engine2:

```
Matching becomes impossible
```

---

# Another Critical Problem Appears

Not all stocks are equal.

Example:

```
AAPL → 100k orders/sec
Small stock → 50 orders/sec
```

If both map to same engine:

```
Engine overloaded
```

This is called:

```
Hot Partition Problem
```

---

# Real Exchanges Solve This With

```
Dynamic Symbol Allocation
```

Popular stocks get **dedicated engines**.

Example:

```
Engine 1 → AAPL
Engine 2 → TSLA
Engine 3 → NVDA
```

Less popular stocks grouped together.

---

# Architecture Now Looks Like

```
Users
   |
API Servers
   |
Order Gateway
   |
Routing Layer
   |
---------------------------------------
|        |        |        |           |
ME-1     ME-2     ME-3     ME-4     ME-5
(AAPL)  (TSLA)   (NVDA)   (SMALL)  (SMALL)
```

Each engine:

```
Single threaded
In-memory order books
Ultra low latency
```

---

# But We Still Have a Huge Problem

If **one engine crashes**:

```
All orders for that symbol are lost
```

Which is catastrophic.

So exchanges must ensure:

```
Zero order loss
```

This introduces a **very important concept**.

```
Replication + Event Log
```

This is similar to:

• Kafka
• Database WAL
• Distributed logs

---

# Next Step (One of the Most Important Parts)

Next we will cover:

### Fault Tolerance in Trading Systems

Including:

• write-ahead logs
• engine replication
• active-passive engines
• deterministic replay
• recovery after crash

This part is **very impressive in interviews**.

---

Before I continue, quick check:

Do you want me to explain first:

1️⃣ **How trading engines avoid losing orders (WAL + replication)**
or

2️⃣ **How exchanges handle 1M+ market data updates/sec (price streaming)**

Both are major interview topics.

---
Perfect. We'll continue **step-by-step**.
Now we address one of the **most critical interview topics** for trading systems.

This is where many candidates struggle.

---

# Step 4 — Preventing Order Loss (Fault Tolerance)

Our architecture now looks like this:

```
Users
   |
API Servers
   |
Order Gateway
   |
Routing Layer
   |
---------------------------------------
|        |        |        |          |
ME-1     ME-2     ME-3     ME-4      ME-5
(AAPL)  (TSLA)   (NVDA)   (SMALL)   (SMALL)
```

Each **Matching Engine (ME)**:

* runs **single-threaded**
* keeps **order books in memory**
* processes orders **very fast**

But now comes the **biggest risk in trading systems**.

---

# The Catastrophic Scenario

Imagine this situation.

Order arrives:

```
BUY 100 AAPL @ $150
```

The matching engine processes it.

The order is placed in the order book.

But suddenly:

```
Server crashes
Power failure
Kernel panic
Machine reboot
```

Since the **order book was in memory**, we lose:

```
All orders
All matches
All trades
```

This is **unacceptable** for a trading platform.

Financial loss + legal consequences.

So engineers asked:

> How do we make sure no order is ever lost?

---

# First Attempt — Save Orders in Database

One idea:

```
Receive order
↓
Write to DB
↓
Process in matching engine
```

Architecture:

```
Gateway
   |
Database
   |
Matching Engine
```

---

# Why This Doesn't Work

Databases are **too slow**.

Typical latency:

```
DB write = 3–10 ms
```

Matching engines need:

```
< 1 millisecond
```

If every order waits for DB:

```
Latency becomes huge
```

Traders will leave the platform.

---

# Real Solution — Write Ahead Log (WAL)

Instead of writing to DB, exchanges use an **append-only log**.

Concept:

```
Order arrives
↓
Append to log
↓
Process in matching engine
```

Architecture:

```
Gateway
   |
Order Log (WAL)
   |
Matching Engine
```

---

# What is WAL?

WAL = **Write Ahead Log**

It is a **sequential file**.

Example log:

```
1: BUY AAPL 100 @150
2: SELL AAPL 50 @151
3: BUY TSLA 10 @700
4: SELL AAPL 100 @149
```

Each order becomes an **event in the log**.

---

# Why WAL Is Fast

Because it is **sequential disk write**.

Disk operations:

```
Random write → slow
Sequential append → very fast
```

Latency:

```
~10–50 microseconds
```

Perfect for trading systems.

---

# Order Processing Flow (With WAL)

New flow:

```
User places order
      |
Gateway validates
      |
Append order to WAL
      |
Send to Matching Engine
      |
Matching Engine processes
      |
Trade event emitted
```

Now if the engine crashes:

```
We still have the log
```

---

# Recovery Process

Suppose engine crashes.

When it restarts:

```
Replay WAL
```

Example:

Log contains:

```
BUY AAPL 100 @150
SELL AAPL 50 @149
BUY AAPL 20 @151
```

Engine replays them:

```
Rebuild order book
Reconstruct trades
```

System recovers.

---

# Important Interview Concept

This is called:

```
Deterministic Replay
```

Meaning:

If the same orders are replayed in the same order:

```
System produces identical state
```

This makes recovery reliable.

---

# Real-World Systems Using This Idea

Very similar systems exist in:

* Kafka
* Database WAL
* Raft log replication
* Event sourcing systems

Trading platforms heavily rely on this model.

---

# But Another Problem Appears

Even with WAL, there is still a risk.

Scenario:

```
Order arrives
↓
Matching engine processes it
↓
Machine crashes BEFORE WAL flush
```

Order could still be lost.

So engineers improved the flow.

---

# Correct Order Flow

```
Order arrives
↓
Append to WAL
↓
Confirm WAL write
↓
Send to Matching Engine
```

Now the system guarantees:

```
If order accepted → it is persisted
```

---

# But What If the Entire Server Dies?

Example:

```
Disk failure
Machine failure
Datacenter failure
```

WAL stored on same server may be lost.

So we introduce **replication**.

---

# Step 5 — Replicated Matching Engines

Architecture becomes:

```
               +------------------+
Gateway -----> |  WAL Replication |
               +------------------+
                        |
                ------------------
                |                |
         Primary Engine      Replica Engine
```

Primary engine processes orders.

Replica engine:

```
Reads same WAL
Replays orders
Maintains identical order book
```

---

# How Replica Works

Example WAL:

```
1 BUY AAPL 100 @150
2 SELL AAPL 50 @149
3 BUY AAPL 20 @151
```

Primary engine:

```
processes events
```

Replica engine:

```
replays same events
```

Both engines maintain **identical state**.

---

# If Primary Engine Crashes

Failover happens.

```
Replica becomes new primary
```

Trading continues.

Downtime becomes minimal.

---

# Architecture Now

```
Users
   |
API Servers
   |
Order Gateway
   |
WAL Cluster
   |
Routing Layer
   |
-------------------------------------------------
|                 |               |              |
ME1(P)        ME2(P)          ME3(P)         ME4(P)
ME1(R)        ME2(R)          ME3(R)         ME4(R)
```

P = Primary
R = Replica

Each engine has **backup replica**.

---

# Another Important Interview Question

Interviewers may ask:

> Why not use multiple active matching engines for the same symbol?

Answer:

Because of **order consistency**.

Example:

```
BUY order processed by engine1
SELL order processed by engine2
```

They may not see each other.

Result:

```
Trade never happens
```

So exchanges ensure:

```
One active engine per symbol
```

Replica only **replays events**, not match.

---

# Current Guarantees

With WAL + replication:

We achieve:

```
No order loss
Fast recovery
High availability
Deterministic state
```

Which are critical for financial systems.

---

# Current Architecture Summary

```
Users
   |
API Servers
   |
Order Gateway
   |
WAL Cluster
   |
Routing Layer
   |
Matching Engines (Sharded)
   |
Event Bus
   |
Consumers
```

---

# But A New Challenge Appears

Even after this architecture, there is another massive load.

Not from orders.

From **market data**.

Example:

Every trade generates price updates.

Millions of users subscribe to price updates like:

```
AAPL price
TSLA price
BTC price
ETH price
```

If:

```
1M users
1000 symbols
```

Market data traffic becomes **insane**.

This leads to the next major system.

---

# Next Step (Extremely Important)

Next we will explore:

### Market Data Distribution System

This includes:

* real-time price streaming
* pub-sub architecture
* websocket scaling
* fan-out problem
* Kafka vs multicast
* how exchanges push millions of updates/sec

This is **another favorite interview topic**.

---

Before we move forward, quick check for you:

Did you fully understand:

* WAL
* deterministic replay
* replication of matching engines
* failover

Or do you want me to **deep dive into WAL + replication with a real example (step-by-step event flow)** before moving ahead?

---
Great. Now we move to another **very important part of trading platforms**.

In many interviews, candidates design **order placement well**, but they completely miss **market data distribution**, which is actually **the biggest traffic in trading systems**.

Let’s continue the story.

---

# Step 6 — The Market Data Explosion

Our system currently works like this:

```text
Users → API → Gateway → WAL → Matching Engines
                                   |
                                   v
                               Trade Events
                                   |
                                   v
                               Event Bus
```

Every time a trade happens:

```text
BUY 10 AAPL @150
SELL 10 AAPL @150
```

The matching engine generates:

```text
TradeEvent
symbol: AAPL
price: 150
qty: 10
timestamp
```

This event must be sent to **users watching AAPL**.

---

# The Real Problem

Imagine the platform grows.

Example numbers:

```
Active traders: 2 million
Symbols: 5000
Price updates/sec: 500k
```

Users constantly watch price updates like:

* AAPL
* TSLA
* BTC
* ETH

Every trade changes:

* last traded price
* best bid
* best ask
* order book depth

So the system must **broadcast updates continuously**.

---

# Naive Approach (Doesn't Work)

One simple approach is:

```text
Matching Engine → API Server → Send update to users
```

Architecture:

```text
Matching Engine
      |
      v
API Servers
      |
      v
Users
```

---

# Why This Fails

Example scenario:

```
AAPL trade occurs
```

Users subscribed:

```
200,000 users
```

The system would need to send:

```
200,000 messages instantly
```

Now imagine:

```
1000 trades/sec
```

Traffic becomes:

```
200M messages/sec
```

This will crash API servers.

This is called the **fan-out problem**.

---

# The Real Solution — Pub/Sub System

Instead of pushing updates directly, we introduce a **market data streaming layer**.

Architecture becomes:

```text
Matching Engine
      |
      v
Market Data Stream (Kafka / PubSub)
      |
      v
Market Data Servers
      |
      v
Users
```

Now the system becomes **event-driven**.

---

# Step 1 — Publish Market Events

Whenever a trade occurs:

Matching engine publishes event:

```
TradeEvent
symbol: AAPL
price: 150
qty: 10
```

Published to a **streaming system**.

Examples used in real systems:

* Kafka
* Aeron
* Chronicle Queue
* NATS
* custom exchange buses

---

# Step 2 — Topic Based Streaming

Streams are organized by **symbol topics**.

Example:

```
Topic: market.AAPL
Topic: market.TSLA
Topic: market.BTC
```

Now updates go to correct topic.

Example:

```
Trade AAPL → topic market.AAPL
```

---

# Step 3 — Market Data Servers

Users do not connect to Kafka directly.

Instead they connect to **Market Data Servers**.

Architecture:

```text
Matching Engine
      |
      v
Kafka / Stream Bus
      |
      v
Market Data Servers
      |
      v
WebSocket Connections
      |
      v
Users
```

These servers:

* consume market events
* push updates to subscribed users

---

# Why WebSockets?

Stock prices change **continuously**.

If we use HTTP polling:

```
Client → request price
Server → response
```

This causes:

```
Millions of HTTP requests/sec
```

Instead we use **WebSockets**.

WebSocket allows:

```
persistent connection
bi-directional communication
low latency
```

Users receive updates instantly.

---

# Example WebSocket Flow

User opens trading app.

Client sends:

```
SUBSCRIBE AAPL
SUBSCRIBE TSLA
```

Server stores subscription.

Now whenever update arrives:

```
Trade AAPL
```

Server pushes update:

```
{
 symbol: "AAPL",
 price: 150,
 qty: 10
}
```

No polling required.

---

# But Another Scaling Problem Appears

Imagine:

```
2 million users connected
```

Each WebSocket server can handle maybe:

```
50k connections
```

So we need:

```
40+ WebSocket servers
```

Architecture becomes:

```text
Users
   |
Load Balancer
   |
------------------------------
|        |        |          |
WS1      WS2      WS3      WS4
   |        |        |        |
   --------Kafka Consumer------
```

Each server consumes market updates.

---

# But Another Problem Appears

Suppose:

```
AAPL trade occurs
```

All WebSocket servers receive it.

But maybe only **20k users actually watch AAPL**.

So sending updates to every server wastes bandwidth.

---

# Real Optimization — Symbol Partitioning

Market data streams are partitioned.

Example:

```
Partition 1 → AAPL, AMD
Partition 2 → TSLA, NVDA
Partition 3 → GOOG, META
```

WebSocket servers subscribe only to relevant partitions.

Architecture:

```text
Matching Engine
      |
      v
Kafka (Partitioned Topics)
      |
      v
Market Data Nodes
      |
      v
WebSocket Servers
      |
      v
Users
```

This reduces unnecessary traffic.

---

# Another Critical Concept — Snapshot + Delta

When user opens a trading screen:

They need the **current order book**.

Example:

```
BUY
150 qty 100
149 qty 80

SELL
151 qty 60
152 qty 90
```

If we only send trade updates, the client cannot reconstruct the full book.

So exchanges use:

```
Snapshot + Delta Updates
```

---

# Snapshot

Initial state.

Example:

```
ORDER_BOOK_SNAPSHOT
symbol: AAPL
bids: [...]
asks: [...]
```

---

# Delta Updates

After snapshot, only changes are sent.

Example:

```
ORDER_BOOK_UPDATE
price:150
qty:80
```

Client updates UI.

---

# This Is How Real Exchanges Work

Example market feeds:

* NASDAQ ITCH
* Binance Market Stream
* Coinbase Feed

They all follow:

```
snapshot
+
incremental updates
```

---

# Another Interview Topic — Latency

Traders care about **latency**.

Typical expectations:

```
Retail platforms → <100ms
Professional trading → <1ms
HFT → <100 microseconds
```

To achieve this:

Exchanges use:

* binary protocols
* UDP multicast
* kernel bypass networking

But for our system design interview, **Kafka + WebSockets is enough**.

---

# Our Architecture Now

```text
Users
   |
Load Balancer
   |
API Servers
   |
Order Gateway
   |
WAL
   |
Routing Layer
   |
Matching Engines
   |
Market Event Bus
   |
Market Data Servers
   |
WebSocket Servers
   |
Users
```

Now the system can handle:

* millions of users
* hundreds of thousands of orders/sec
* millions of market updates/sec

---

# But We Still Haven't Covered

A **very critical trading system component** that interviewers often ask about.

This is called:

```
Risk Management System
```

Example questions:

* What if user has no balance?
* What if order size is too big?
* What if margin is insufficient?
* What if system allows unlimited leverage?

Exchanges must **validate orders before they reach matching engine**.

Otherwise traders could break the market.

---

# Next Step

Next we will explore:

### Risk Management + Order Validation System

Including:

* pre-trade risk checks
* margin calculation
* preventing invalid orders
* rate limiting traders
* protecting the matching engine

This is another **core trading platform component**.

---

Before we move ahead, quick check:

Do you want the next section to be:

1️⃣ **Risk Management System (pre-trade checks)**
or

2️⃣ **Database design for trading systems (orders, portfolios, trades)**

Both are common **HLD interview follow-ups**.

---
Great. Now we go deeper into a part interviewers **almost always ask after the architecture**:

> **“What would the database schema look like?”**

But instead of jumping to tables, we’ll follow the **real evolution story** again — because trading systems **cannot use databases in the normal way**.

---

# Step 7 — The Database Problem in Trading Systems

Let’s recall our current architecture:

```
Users
   |
API Servers
   |
Order Gateway
   |
WAL
   |
Matching Engines
   |
Event Bus
   |
Consumers
```

Matching engines store **order books in memory**, not in a database.

But we still need databases for things like:

* user accounts
* balances
* trade history
* portfolio
* reporting
* compliance

So engineers initially tried a **traditional database approach**.

---

# First Attempt — Store Everything in DB

Initial design:

```
Gateway → Matching Engine → Database
```

Tables:

```
users
orders
trades
portfolio
```

---

## Example Order Flow

User places order:

```
BUY 10 AAPL @150
```

System does:

```
1 insert order
2 match trade
3 insert trade
4 update portfolio
```

This works at small scale.

But soon it breaks.

---

# Problem — Database Cannot Handle Trading Load

Example numbers:

```
orders/sec = 200,000
trades/sec = 100,000
updates/sec = millions
```

Every trade requires:

```
insert trade
update portfolio
update balances
update order status
```

Database becomes the **bottleneck**.

Even distributed databases struggle with this.

---

# The Realization

Engineers realized something important:

> **The database should not be in the critical trading path.**

Instead, the **matching engine remains the source of truth during trading**, and the database is updated **asynchronously**.

---

# Event Sourcing Approach

Instead of writing directly to DB:

```
Matching Engine → Event Bus → Consumers → Database
```

Example event:

```
TradeExecuted
symbol: AAPL
buyer: user1
seller: user2
price: 150
qty: 10
```

Consumers update databases.

---

# Now Database Writes Are Async

This removes database latency from the trading pipeline.

New flow:

```
Order → Matching Engine → Trade Event → Kafka
                                  |
                                  v
                          Database Consumers
```

Matching engine stays **ultra fast**.

Database updates happen **eventually**.

---

# Database Responsibilities

The database is used for:

```
User accounts
Balances
Portfolio
Trade history
Order history
Analytics
```

Not for matching.

---

# Core Database Tables

Now let's design the **core schema**.

---

# 1 Users Table

```
users
------
user_id (PK)
name
email
created_at
status
```

Example:

```
1  Riyaz
2  Alice
3  Bob
```

---

# 2 Accounts Table

Separating accounts allows multi-wallet systems.

```
accounts
--------
account_id
user_id
balance
currency
updated_at
```

Example:

```
account_id | user_id | balance
101        | 1       | 100000
102        | 2       | 50000
```

---

# 3 Orders Table

Stores order history.

```
orders
------
order_id
user_id
symbol
side
price
quantity
filled_quantity
status
created_at
```

Example:

```
order_id: 9001
user_id: 1
symbol: AAPL
side: BUY
price: 150
qty: 10
status: PARTIAL
```

---

# Order Status

Common statuses:

```
NEW
PARTIAL
FILLED
CANCELLED
REJECTED
```

---

# 4 Trades Table

Every matched trade is stored here.

```
trades
------
trade_id
symbol
price
quantity
buy_order_id
sell_order_id
timestamp
```

Example:

```
trade_id: 5001
symbol: AAPL
price: 150
qty: 10
```

---

# 5 Portfolio Table

Tracks user holdings.

```
portfolio
---------
user_id
symbol
quantity
avg_price
updated_at
```

Example:

```
user1
AAPL
10 shares
avg_price 150
```

---

# 6 Order Events Table (Optional)

Many trading systems keep order state transitions.

```
order_events
------------
event_id
order_id
event_type
timestamp
metadata
```

Example:

```
ORDER_CREATED
ORDER_MATCHED
ORDER_FILLED
ORDER_CANCELLED
```

Useful for auditing.

---

# Database Scaling Problem

Now imagine:

```
100M users
billions of trades
```

Single database won't work.

We must **shard the database**.

---

# Database Sharding Strategy

There are two common choices.

---

## Option 1 — Shard by User ID

```
shard = user_id % N
```

Example:

```
user 1 → shard1
user 2 → shard2
user 3 → shard3
```

This works well for:

* portfolios
* accounts
* orders

Because queries are user-based.

---

## Option 2 — Shard by Symbol

```
symbol_hash % N
```

This works well for:

* trade history
* market analytics

But user queries become harder.

---

# Real Systems Use Hybrid Sharding

Typical pattern:

```
User Data → shard by user_id
Trade Data → shard by symbol
```

Example architecture:

```
User DB Cluster
(sharded by user_id)

Trade DB Cluster
(sharded by symbol)
```

This distributes load.

---

# Replication Strategy

Each shard has replicas.

Example:

```
Primary DB
   |
   ├── Replica 1
   └── Replica 2
```

Reads go to replicas.

Writes go to primary.

---

# Example Queries

User opening portfolio:

```
SELECT * FROM portfolio WHERE user_id=1
```

User viewing order history:

```
SELECT * FROM orders WHERE user_id=1 ORDER BY created_at
```

Market analytics:

```
SELECT * FROM trades WHERE symbol='AAPL'
```

---

# Cold Storage for Trade History

Trade data grows extremely fast.

Example:

```
10M trades/day
```

Over years:

```
billions of rows
```

Older trades move to **cold storage**:

Examples:

* S3
* data warehouse
* Hadoop
* BigQuery

Used for analytics.

---

# Final Database Architecture

```
                Event Bus
                    |
                    v
            -----------------
            |               |
       Order DB        Trade DB
     (user sharded)  (symbol sharded)
            |
            v
        Portfolio DB
```

All updated via **event consumers**.

---

# Interview Gold Points

Strong candidates mention:

```
Database not in matching path
Event-driven updates
User-based sharding
Symbol-based sharding
Async persistence
Cold storage for trade history
```

These show **production-level understanding**.

---

# Current Full System

```
Users
 |
Load Balancer
 |
API Servers
 |
Order Gateway
 |
WAL
 |
Routing Layer
 |
Matching Engines (Sharded)
 |
Market Event Bus
 |
Consumers
 |
Databases
```

This is already **very close to real exchange architecture**.

---

# Next Important Topics (Interview Critical)

We still haven’t covered some **very powerful HLD interview discussions**:

1️⃣ **Risk Management System (margin checks, balance validation)**
2️⃣ **Handling order cancellations**
3️⃣ **Rate limiting traders**
4️⃣ **Hot symbol problem (AAPL meltdown)**
5️⃣ **Multi-region trading architecture**

These are the topics that make interviewers think:

> “This candidate really understands distributed systems.”

---

Since we’re going step-by-step, the **best next concept** is:

### Risk Management System

Because **orders must be validated before reaching the matching engine**.

Otherwise traders could:

```
buy $1B with $10 balance
```

Which would break the system.

---

If you'd like, next I can explain **Risk Management System** again **as a story evolution**, including:

* pre-trade validation
* margin system
* circuit breakers
* protection against rogue traders

And this part is **very commonly asked in HLD interviews.**
