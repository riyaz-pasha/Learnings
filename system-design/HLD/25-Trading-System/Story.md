# Designing a Trading Platform — Chapter 1: The Core Problem

Let's start at the very beginning — not with architecture, but with *why this problem is hard.*

---

## The Story Begins: A Marketplace with a Chaos Problem

Imagine it's 1990. You want to buy 100 shares of a company. You call your broker, who shouts your order onto a trading floor. Somewhere across the room, another broker has a client who wants to *sell* 100 shares. If they find each other, a trade happens. If not, both go home empty-handed.

The problem? It's chaos. There are thousands of buyers and sellers. Nobody has a clear picture of "what's available at what price." Prices are inconsistent — the same stock might sell for $50.00 on one side of the room and $50.05 on another side, simultaneously.

So the industry asked: **what if we could automate this?** What if a computer could keep track of every buyer, every seller, every price — and match them instantly?

That's the core job of a trading platform. And the central data structure that makes it possible is called the **Order Book**.

---

## The Order Book: The Heart of Everything

Think of the order book as two sorted lists living side by side.

On one side, you have all the people who want to **buy** (called "bids"). They're sorted from highest price to lowest — because the most eager buyer (the one willing to pay the most) sits at the top.

On the other side, you have all the people who want to **sell** (called "asks" or "offers"). They're sorted from lowest price to highest — because the most eager seller (the one willing to accept the least) sits at the top.

A trade happens when the top of one side meets the top of the other — that is, when the highest bid is *at least* as high as the lowest ask. This meeting point is called the **spread**.

Here's a concrete example. Say you're looking at the order book for AAPL:Right now, no trade happens. Alice wants to pay $149.95, but Eve won't sell for less than $150.05. The gap between them — the **spread** — is $0.10.

Now imagine a new buyer, Zara, comes in and says: *"I'll buy 300 shares at $150.05."* Her bid exactly matches Eve's ask. The matching engine fires instantly — 300 shares change hands at $150.05, Eve's order disappears from the book, and Zara is done. That's a trade.

---

## The Matching Engine: The Brain of the Platform

The component responsible for watching the order book and firing trades is called the **Matching Engine**. It's the single most critical component in the entire system. Every other part of the platform — the APIs, the databases, the dashboards — exists to serve this one piece.

The matching engine follows a priority rule called **Price-Time Priority** (also called FIFO matching):

First, the order with the *best price* wins. If two buyers both want to buy at $149.95, the one who submitted their order *earlier* gets matched first. This makes the system fair — you're rewarded for acting quickly and bidding aggressively.

Let's now look at the two types of orders every trading platform must handle:

A **Limit Order** says *"I want to buy/sell X shares, but only at this specific price or better."* If the market never reaches that price, the order just sits in the book waiting. Alice's bid of $149.95 is a limit order.

A **Market Order** says *"I want to buy/sell X shares right now, at whatever the best available price is."* It doesn't wait — it hits the book immediately and matches against whatever is there. Market orders guarantee execution but not price.

---

## Now the First Real Problem Appears: Speed

So the engineers built this system. They put the order book in a database (say, PostgreSQL). Every time an order comes in, they do something like:

```sql
-- Check if a matching order exists
SELECT * FROM asks WHERE price <= 150.05 ORDER BY price ASC, time ASC LIMIT 1;

-- If match found, execute trade and update quantities
UPDATE asks SET qty = qty - 300 WHERE id = 'eve_order';
INSERT INTO trades VALUES (...);
```

It works. In a lab. But then they go live, and the system starts receiving **50,000 orders per second.** Each one hits the database. The database starts sweating. Latency creeps from milliseconds to hundreds of milliseconds. Traders are furious — in markets, a 200ms delay means you're trading on stale prices.

The core insight the engineers had: **the order book doesn't need to be in a database.** The database is optimized for persistence and complex queries — but the matching engine needs something far simpler: a sorted data structure that can insert, delete, and find the best price in microseconds.

So they moved the entire order book **into memory** — specifically, into a data structure like a sorted map (a Red-Black Tree or a Skip List), keyed by price level. Now matching happens at nanosecond speed, entirely in RAM.

The architecture split into two clean layers:

The key insight here is the **separation of concerns**: the matching engine does one thing at blazing speed (match orders in memory), and the database handles persistence *asynchronously* — it's not on the critical path. A trade happens the moment it's matched in RAM; the database write follows shortly after as a background operation.

---

## Quick Recap Before We Move Forward

So far you've learned the two foundational concepts of any trading platform. The order book is the core data structure — two sorted lists of buyers and sellers — and a trade fires the moment the best bid meets or crosses the best ask. The matching engine is the brain that watches the book and executes this logic, and it must live in memory to achieve the required speed. The database is deliberately kept off the critical path.

---

**Take a moment and think about this:** If the matching engine lives entirely in memory, what happens when the server crashes? The entire order book — thousands of live orders from real traders — is gone. How would you recover? 

This is the exact question the engineers faced next, and it leads us into the topics of **Write-Ahead Logging, Event Sourcing, and Replication** — which we'll cover in Chapter 2.

**Are you comfortable with everything so far?** Let me know and we'll move to the next chapter, or if anything feels unclear, ask away!

---

# Chapter 2: Surviving a Crash — WAL, Event Sourcing & Replication

## The Problem: Memory is Volatile

Let's pick up exactly where we left off. The matching engine is running beautifully in RAM — matching 50,000 orders per second with nanosecond latency. Then at 2:47 PM on a Tuesday, the server loses power.

When it comes back up, the in-memory order book is **completely empty**. Every live order that traders had placed — "buy 500 AAPL at $149.95", "sell 200 TSLA at $320.10" — is gone. Traders are furious. Regulators are calling. The exchange is liable for millions.

So the engineers asked: **how do we recover the order book after a crash, without slowing down the matching engine?**

---

## First Attempt: Just Write Everything to the Database

The naive fix is to write every order to the database *before* processing it. Something like:

```
Trader sends order
  → Save to DB (disk write)
  → Then process in matching engine
  → Then respond to trader
```

This works for recovery — on restart, just reload all open orders from the database. But it **completely destroys the latency.** A disk write takes 1–10 milliseconds. At 50,000 orders/second, you've just made the system 1000x slower. You've moved the bottleneck from RAM back to disk. This approach was quickly abandoned.

---

## The Real Solution: Write-Ahead Log (WAL)

Here's the key insight the engineers borrowed from database internals: **you don't need to write the full state — you only need to write the intent.**

Instead of saving the entire order book to disk after every change, you write a tiny, sequential log entry *before* processing each order. This log looks like:

```
[timestamp=1000] ORDER_PLACED  { id: A1, side: BUY,  price: 149.95, qty: 500 }
[timestamp=1001] ORDER_PLACED  { id: A2, side: SELL, price: 150.05, qty: 300 }
[timestamp=1002] ORDER_CANCELLED { id: A1 }
[timestamp=1003] TRADE_EXECUTED { buyer: A3, seller: A2, price: 150.05, qty: 300 }
```

This is the **Write-Ahead Log**, or WAL. Writing sequentially to disk like this is *extremely* fast — roughly 10–100x faster than a random write — because the disk head never has to seek around. It just appends to the end of the file, one line after another.

Now the flow looks like this:

```
Trader sends order
  → Append one line to WAL on disk  ← fast sequential write
  → Process in matching engine (RAM) ← nanosecond fast
  → Respond to trader

  (separately, async)
  → Periodically save full snapshot to DB
```

**Recovery after a crash becomes simple:** load the last snapshot from the database, then *replay* the WAL from that point forward. Every order gets re-processed in sequence, and the in-memory order book is fully reconstructed. This typically takes seconds, not minutes.

---

## This Idea Has a Deeper Name: Event Sourcing

Once the engineers saw the power of the WAL, they realized they were sitting on something more profound. Instead of storing *state* ("the current order book"), they were storing *events* ("every action that ever happened"). This pattern is called **Event Sourcing**.

The difference is subtle but important. Think of it like this analogy: a bank doesn't store just your current balance. It stores every deposit and withdrawal since the account opened. Your balance is *derived* by replaying that history. This means you can audit anything, rewind to any point in time, and reconstruct any past state of the world.

A trading platform with event sourcing can answer questions like:
- "What did the order book look like at exactly 2:46:59 PM before the crash?"
- "Was this particular trade executed fairly — did the time priority rule hold?"
- "Show me every order this user placed in the last 30 days."

None of these are possible if you only store current state. The log *is* the truth; everything else is derived from it.

```
The log (source of truth):
─────────────────────────────────────────────────────
t=1  ORDER_PLACED   { BUY  500 @ 149.95 }
t=2  ORDER_PLACED   { SELL 300 @ 150.05 }
t=3  ORDER_PLACED   { BUY  300 @ 150.05 }  ← triggers match!
t=4  TRADE_EXECUTED { price: 150.05, qty: 300 }
─────────────────────────────────────────────────────
         ↓ replay anytime to rebuild ↓
         
    Current Order Book (derived view)
    BIDS: [500 @ 149.95]
    ASKS: []   ← Eve's ask was consumed
```

The order book in memory is just a *projection* — a derived view built by replaying events. You can throw it away and rebuild it anytime.

---

## But Now a New Problem Appears: Single Point of Failure

The WAL solves the crash recovery problem beautifully. But the engineers soon realized there's a scarier scenario: **what if the entire machine dies?** Not a software crash — a hardware failure. The disk could fail. The data center could lose power. A network partition could make the server unreachable.

With a single server, you have a **Single Point of Failure (SPOF)**. If it goes down, the entire trading platform goes down with it. For a stock exchange, even 30 seconds of downtime can cause millions in losses and regulatory penalties.

The solution is **Replication** — running multiple copies of the matching engine and keeping them in sync.

---

## Replication: The Two Approaches

The engineers explored two approaches, each with very different tradeoffs.

**Approach 1: Active-Passive (Leader-Follower) Replication**

You have one **leader** (the primary matching engine) and one or more **followers** (standbys). Every event the leader processes gets streamed to the followers in real time. The followers apply the same events and maintain an identical copy of the order book. If the leader dies, a follower is promoted to become the new leader.

```
                    ┌─────────────┐
    Orders ──────→  │   LEADER    │ ──→ Trades
                    │ (primary)   │
                    └──────┬──────┘
                           │ stream events
                    ┌──────▼──────┐
                    │  FOLLOWER 1 │  (hot standby)
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │  FOLLOWER 2 │  (warm standby)
                    └─────────────┘
```

This is relatively simple and works well. The downside: only the leader does real work. Followers are expensive machines sitting idle, just waiting for a disaster.

**Approach 2: Active-Active Replication**

What if both machines could process orders? This sounds appealing, but it immediately runs into a brutal problem: **how do you guarantee that both engines process orders in the same sequence?**

Imagine Alice's order arrives at Machine A at the same microsecond that Bob's order arrives at Machine B. Machine A processes Alice first and produces a trade. Machine B processes Bob first and produces a different trade. Now the two machines have diverged — they have different order books, different trade histories. This is a **split-brain** scenario and it's catastrophic for a trading system where fairness and consistency are legally mandated.

The only way to make active-active work is to add a **sequencer** — a component that assigns a global sequence number to every order *before* it reaches any matching engine. Now both engines process events in the same order, guaranteed.

```
    Orders
       │
       ▼
  ┌─────────────┐
  │  SEQUENCER  │  assigns order: 1, 2, 3, 4...
  └──────┬──────┘
         │ same sequence
    ┌────┴────┐
    ▼         ▼
┌───────┐  ┌───────┐
│  ME-A │  │  ME-B │  both process events in identical order
└───────┘  └───────┘
  primary    replica
  (matches)  (verifies)
```

Now ME-A and ME-B will always produce identical order books. ME-A actively processes trades. ME-B silently mirrors every step. If ME-A dies, ME-B takes over instantly — with zero state to catch up on because it's already perfectly in sync.

This sequencer pattern is actually how modern ultra-low-latency exchanges like NASDAQ and NYSE work in practice.

---

## The Tricky Part: How Much Replication Lag Can You Tolerate?

Here's where it gets nuanced. Replication takes time — even microseconds matter. The engineers had to choose between two consistency models:

**Synchronous replication** means the leader waits for at least one follower to acknowledge it received the event *before* confirming the trade to the trader. This gives you zero data loss on failover, but it adds the round-trip latency of the replication network to every single trade. For a platform promising sub-millisecond execution, this is painful.

**Asynchronous replication** means the leader confirms the trade immediately and replicates in the background. Latency stays low, but if the leader crashes in the tiny window before the follower catches up, you lose the most recent events. For a trading system, losing a trade is legally unacceptable.

The industry solution: a hybrid called **semi-synchronous replication**. The leader waits for *one* follower to acknowledge (guaranteeing at least one copy exists), but doesn't wait for all followers. You get near-zero data loss with only a small latency penalty.

---

## Recap of Chapter 2

The story so far: matching in memory (Chapter 1) introduced a crash vulnerability, which the WAL solved by writing sequential intent logs before processing. Event sourcing revealed that the log itself is the source of truth, and the in-memory book is just a derived view. Replication solved the single point of failure, with the active-passive (leader-follower) model being the standard approach, and the active-active + sequencer pattern being used where instant failover is critical.

A useful mental model: the WAL is like a video game's save file — you don't save the entire game world, just the list of moves. Replication is like playing that same save file on two consoles simultaneously, keeping them in sync.

---

**Before we move to Chapter 3**, think about this question: the matching engine currently handles *all* stocks on a single machine. Apple, Tesla, Google, Amazon — one engine, one order book, one log. What happens when you're doing 500,000 orders per second across 10,000 different stocks? Even RAM and the WAL have limits.

This is what leads us to **Sharding** — and it's where the design gets really interesting. Say the word and we'll dive in!

---

# Chapter 3: Sharding — Splitting the World to Scale

## The Problem: One Engine Can't Handle Everything

Let's set the scene. Your trading platform is now reliable — the WAL protects you from crashes, replication protects you from hardware failure. Business is booming. You've expanded from one stock exchange to a full platform covering 10,000 stocks across multiple markets.

At peak hours, orders are flying in at **500,000 per second**. But your matching engine is a single process on a single machine. Even the fastest server in the world has limits — eventually the CPU can't keep up, the RAM fills with order books for thousands of symbols, and the WAL starts lagging behind because there's simply too much to write.

The engineers asked: **what if we could split the work across multiple machines?** Instead of one engine handling all 10,000 stocks, what if 10 engines each handled 1,000 stocks? Or 100 engines each handled 100 stocks?

This idea — splitting data and workload across multiple machines — is called **Sharding** (also called *horizontal partitioning*). Each piece is called a **shard**.

---

## The Natural Sharding Key: Stock Symbol

The first question in any sharding design is: *what do you split on?* The answer should be something that naturally groups related work together, so that most operations touch only one shard rather than many.

For a trading platform, the answer is obvious: **shard by stock symbol**. All orders for AAPL go to Shard 1. All orders for TSLA go to Shard 2. And so on. This works perfectly because a trade for AAPL *never* needs to interact with a trade for TSLA — they're completely independent markets. There's no "cross-shard" coordination needed for the core matching logic.

```
           Incoming Orders
                 │
         ┌───────▼────────┐
         │    ROUTER      │  "Which shard owns this symbol?"
         └───────┬────────┘
        ┌────────┼─────────┐
        ▼        ▼         ▼
   ┌─────────┐ ┌─────────┐ ┌─────────┐
   │ Shard 1 │ │ Shard 2 │ │ Shard 3 │
   │  AAPL   │ │  TSLA   │ │  GOOGL  │
   │  MSFT   │ │  AMZN   │ │  META   │
   │  NVDA   │ │  NFLX   │ │  AMD    │
   └─────────┘ └─────────┘ └─────────┘
```

Each shard is an independent matching engine with its own in-memory order book, its own WAL, and its own replication setup. Shards don't know about each other. They are islands.

---

## How the Router Knows Where to Send Orders

There are three strategies for mapping a symbol to a shard, and each has real tradeoffs.

**Strategy 1: Static Mapping (the simple approach)**

You maintain a lookup table: AAPL → Shard 1, TSLA → Shard 2, etc. The router checks this table on every incoming order. This is simple, predictable, and easy to reason about.

```
Symbol Table:
  AAPL  →  Shard 1
  MSFT  →  Shard 1
  TSLA  →  Shard 2
  AMZN  →  Shard 2
  GOOGL →  Shard 3
  ...
```

The problem is **hotspots**. On the day Apple announces a new iPhone, AAPL might receive 10x more orders than any other symbol. Shard 1 is overwhelmed while Shards 2 and 3 sit relatively idle. You've scaled horizontally but created an uneven load distribution.

**Strategy 2: Hash-Based Sharding**

Instead of a manual table, compute `shard_id = hash(symbol) % num_shards`. AAPL hashes to some number, you take the modulo, and it lands on a shard. Because good hash functions distribute values evenly, the load spreads naturally.

```python
# Simple example
def get_shard(symbol, num_shards):
    return hash(symbol) % num_shards

get_shard("AAPL", 3)   # → shard 0
get_shard("TSLA", 3)   # → shard 1
get_shard("GOOGL", 3)  # → shard 2
```

This is better for distribution, but it introduces a nasty problem: **what happens when you add a new shard?** If you go from 3 shards to 4, `hash(symbol) % 4` produces completely different assignments. Almost every symbol moves to a different shard. During the migration, orders for AAPL might be flying to Shard 0 on some servers and Shard 3 on others — total chaos. You'd have to pause the entire exchange to reshard, which is unacceptable.

**Strategy 3: Consistent Hashing (the production solution)**

This is the clever fix. Imagine arranging all possible hash values in a giant ring — like a clock face with numbers from 0 to 1,000,000. Each shard "owns" a slice of that ring. When a new symbol comes in, you hash it to a point on the ring, then walk clockwise until you hit the nearest shard boundary.

```
        0
    ────────
   /  Shard1 \
  │    AAPL   │  250,000
  │    MSFT   ├────────────
   \          /   Shard2
    ──────────    TSLA
   500,000  /    AMZN
      │    /
   Shard3 /
   GOOGL /
  750,000
```

Now when you add Shard 4, it takes over only *one slice* of the ring. Only the symbols in that slice move — typically about 1/N of all symbols, where N is the new total number of shards. Everything else stays exactly where it is. You've solved the resharding problem with minimal disruption.

---

## The Problem Nobody Anticipated: Hotspot Stocks

Consistent hashing solves *adding shards*, but it doesn't solve the hotspot problem — AAPL during an earnings announcement simply generates more traffic than AAPL hashes can distribute.

The solution the engineers reached for was **virtual nodes** (vnodes). Instead of each physical shard owning one point on the ring, it owns many points — perhaps 100. So Shard 1 might own 100 scattered positions on the ring. This means even if one position on the ring gets hammered (high traffic stocks), the load is distributed across all physical shards more evenly, because a hot stock is statistically unlikely to land on *all* of Shard 1's 100 positions.

But even this has limits. For genuinely extreme cases (think GameStop in 2021 — a single stock with 100x normal volume), exchanges sometimes dedicate an *entire shard* to just that one symbol, temporarily. The routing table gets a special-case entry: `GME → dedicated shard 99`. After the frenzy subsides, it goes back to normal routing. This manual override is a safety valve.

---

## Now a Deeper Problem Emerges: Cross-Shard Queries

Matching orders works great with sharding because each match is contained within one symbol. But users need more than just order matching — they need things like:

- "Show me all open orders for this user" (a user might have orders on 50 different symbols, on 50 different shards)
- "What's this user's total portfolio value?" (requires data from every shard)
- "Generate end-of-day reports" (aggregate data across all shards)

Suddenly you have a **fan-out problem**. To answer "show me all orders for user Alice," the query must be sent to all shards simultaneously, each shard searches its own data, and then the results are merged together. This is called a **scatter-gather** pattern.

```
  Query: "All orders for Alice"
              │
    ┌─────────┼─────────┐
    ▼         ▼         ▼
 Shard 1   Shard 2   Shard 3   ← parallel queries (scatter)
 [AAPL]    [TSLA]    [GOOGL]
    │         │         │
    └─────────┼─────────┘
              ▼
           MERGER            ← combine results (gather)
              │
           Alice's full order list
```

This works, but it's slow and expensive — you're touching every shard for one user's query. The engineers realized they needed a **separate read-optimized data store** just for user-level queries, completely decoupled from the matching engine shards.

This is the idea behind the **CQRS pattern** — Command Query Responsibility Segregation — where the write path (order matching) and the read path (user queries, dashboards, reports) are completely separate systems, each optimized for their own job. The matching engine writes events, those events are asynchronously consumed by a separate read service that builds user-facing views. You never ask the matching engine to do reporting work.

We'll go deep into CQRS in a later chapter. For now, file it away as: *write path and read path should be separated.*

---

## Replication Meets Sharding: Now You Have Both

Here's a mental model check. Once you introduce both sharding and replication, your deployment looks like this:

```
       ┌──────────────────────────────────┐
       │         Shard 1 (AAPL, MSFT)     │
       │  ┌──────────┐   ┌──────────┐    │
       │  │  Leader  │──▶│ Follower │    │
       │  └──────────┘   └──────────┘    │
       └──────────────────────────────────┘

       ┌──────────────────────────────────┐
       │         Shard 2 (TSLA, AMZN)     │
       │  ┌──────────┐   ┌──────────┐    │
       │  │  Leader  │──▶│ Follower │    │
       │  └──────────┘   └──────────┘    │
       └──────────────────────────────────┘
```

Each shard is independently replicated. So if you have 10 shards and each has 2 replicas, you have 20 matching engine processes running. This is totally normal in production. Each shard is its own little kingdom — independent WAL, independent replication lag, independent failover.

An important consequence: the failure of Shard 2 doesn't affect Shard 1 at all. AAPL keeps trading even if TSLA's shard goes down for maintenance. This is called **fault isolation**, and it's one of the biggest benefits of sharding beyond just performance.

---

## Recap of Chapter 3

The story so far: you sharded by stock symbol, which gives you clean fault isolation and horizontal scaling since trades never cross symbol boundaries. Consistent hashing with virtual nodes solved the "what happens when we add shards" problem gracefully. The scatter-gather problem for cross-user queries revealed that the matching engine's shards are not the right place for user-facing reads, pointing us toward the read/write separation we'll explore later.

A helpful way to think about sharding: it's like splitting a single massive library into 10 smaller branch libraries, each serving its own neighborhood. The branches operate independently, and you assign neighborhoods (symbols) to branches using a rule (consistent hashing) that makes re-assignment minimally disruptive when you open a new branch.

---

**Before Chapter 4, here's something to think about:** You now have 10 shards, each receiving orders from thousands of traders simultaneously. How do those traders *connect* to the platform? If they're all hammering a single entry point, you've just moved the bottleneck from the matching engine to the front door. And what happens when a trader sends an order and then immediately asks "did my order execute?" — how does that response get routed back to the right trader, from the right shard?

This leads us to **API Gateway design, connection management, and the order lifecycle** — Chapter 4. Ready?

---
# Chapter 4: The Front Door — API Gateway, Connections & the Order Lifecycle

## The Problem: 50,000 Traders Knocking at Once

Let's paint the picture. You've got a beautifully sharded matching engine — 10 independent shards, each replicated. Now imagine 50,000 traders sitting at their terminals, all trying to send orders simultaneously. Each one needs to connect to the platform, authenticate, send orders, and receive responses — often within single-digit milliseconds.

If every trader connects directly to a matching engine shard, you have immediate chaos. The shard has to handle networking, authentication, rate limiting, and TLS encryption *on top of* matching orders at nanosecond speed. You've just forced your Formula 1 race car to also serve as a taxi. The engineers quickly realized the system needed a dedicated "front door" — a layer whose *only* job is to handle the outside world. This is the **API Gateway**.

---

## What the API Gateway Actually Does

Think of the API Gateway like the lobby of a large office building. It doesn't do the actual work of any department inside. Its job is to greet everyone who walks in, check their ID, figure out which floor they need, send them to the right elevator, and make sure the building doesn't get overcrowded. The matching engines are the departments upstairs — they never see the chaos of the lobby.

Concretely, the API Gateway is responsible for five things, and understanding each one matters for interviews.

**Authentication and Authorization** happen first, before any order touches the matching engine. Every trader has an API key or session token. The gateway validates it against an auth service on every single request. If the token is invalid, expired, or belongs to a suspended account, the request dies right there — the matching engine never even knows it existed. This is the *security perimeter*.

**Rate Limiting** is next. A legitimate trader might send 100 orders per second. A buggy trading bot might accidentally send 100,000. Without rate limiting, one misbehaving client can starve everyone else. The gateway tracks request counts per client (usually in a fast in-memory store like Redis) and rejects anything beyond the allowed quota with a `429 Too Many Requests` response. This protects the entire system from accidental or malicious overload.

**Routing** is the gateway figuring out which shard to send the order to. Remember the consistent hashing router from Chapter 3? That logic lives in the gateway. An order for AAPL goes to Shard 1; an order for TSLA goes to Shard 2. The trader never needs to know which shard exists — they just send to the gateway and it handles the rest.

**Protocol Translation** is something many people miss in interviews. External clients might connect over REST (HTTP), WebSocket, or FIX protocol (a specialized financial messaging format used by institutional traders). But internally, the matching engine speaks a lean binary protocol optimized for speed. The gateway translates between these worlds — it's the diplomat who speaks both languages.

**Load Balancing** across multiple gateway instances themselves, since the gateway can also become a bottleneck if you only run one of them. You typically run 5–10 gateway servers behind a hardware load balancer, all stateless, so any gateway can handle any request.

```
 Traders (REST / WebSocket / FIX)
            │
    ┌───────▼────────┐
    │  Load Balancer │  (hardware or DNS-based)
    └───────┬────────┘
            │
   ┌────────┼────────┐
   ▼        ▼        ▼
 GW-1     GW-2     GW-3   ← stateless API gateways
            │
     ┌──────▼──────┐
     │  Auth / Rate │  ← shared services
     │  Limit check │
     └──────┬──────┘
            │  route by symbol
   ┌────────┼────────┐
   ▼        ▼        ▼
Shard-1  Shard-2  Shard-3  ← matching engines
```

---

## The Critical Design Choice: REST vs WebSocket

Now here's where it gets interesting, and this is a topic interviewers love. How should traders *connect* to the platform? There are two fundamentally different communication models, and each one is right for a different job.

**REST (HTTP request-response)** works like a phone call you initiate. You dial, you speak, you get an answer, the call ends. Every order submission is a fresh HTTP request: the trader sends a POST to `/orders`, the gateway routes it, the matching engine processes it, and a response comes back. Clean, stateless, simple.

The problem is that HTTP is inherently *pull-based*. If a trader wants to know whether their order filled, they have to keep asking — "did it fill yet? did it fill yet? did it fill yet?" This is called **polling**, and at scale it's wasteful. 50,000 traders polling every 100ms generates 500,000 requests per second just for status checks, producing no useful data most of the time.

**WebSocket** solves this with a persistent, bidirectional connection. Instead of a phone call, think of it as an open walkie-talkie channel. The trader connects once, and then both sides can push messages to each other at any time without the overhead of re-establishing a connection. When the matching engine executes a trade, it *pushes* the fill notification directly to the trader's WebSocket connection in real time — no polling needed.

For a trading platform, the industry standard is to use **both**, for different things. Order submission goes over REST (it's a one-time action, stateless, easy to retry). Market data and order updates come back over WebSocket (continuous streams, low latency, push-based). This is called a **hybrid protocol design**, and it's worth mentioning explicitly in interviews.

---

## Following an Order End-to-End: The Full Lifecycle

This is arguably the most important thing to understand for HLD interviews — being able to trace a single order through the entire system from the moment a trader clicks "buy" to the moment they see "filled" on their screen. Let's walk through it step by step.

**Step 1 — The trader sends an order.** Alice opens her trading app and submits: "Buy 500 AAPL at $150.00 limit." Her client sends a POST request over HTTPS to the API Gateway, with her API key in the header.

**Step 2 — Gateway authenticates and rate-checks.** The gateway hits the auth service (a fast Redis lookup, typically sub-millisecond), confirms Alice's API key is valid and she hasn't exceeded her quota. If either check fails, she gets a `401` or `429` immediately and nothing proceeds.

**Step 3 — Gateway assigns an order ID and routes.** The gateway generates a globally unique order ID (we'll talk about how in a moment), looks up which shard owns AAPL, and forwards the order over an internal binary protocol to Shard 1's matching engine. The gateway also saves a mapping: `order_id → connection_id`, so it knows which WebSocket connection to push the response back to later.

**Step 4 — The WAL write happens first.** Before Shard 1's matching engine touches the order, it appends one line to its Write-Ahead Log: `ORDER_PLACED { id: X, side: BUY, price: 150.00, qty: 500, trader: Alice }`. Only after this write is confirmed does the engine process the order.

**Step 5 — The matching engine processes the order.** The engine checks the ask side of AAPL's in-memory order book. If there's a sell order at $150.00 or below, a trade fires immediately. If not, the order sits in the book at the BUY side, waiting.

Let's say a match *does* happen — Eve has 500 shares for sale at $149.95 (Alice gets the better price since she was willing to pay more). Two events are generated: `TRADE_EXECUTED` and `ORDER_FILLED`.

**Step 6 — The response travels back.** The matching engine publishes the `TRADE_EXECUTED` event to an internal message queue (like Kafka). Multiple consumers pick this up: the persistence service writes it to the database, the risk service updates Alice's position, and the notification service pushes a fill confirmation back through the API Gateway to Alice's WebSocket connection.

**Step 7 — Alice sees "Filled."** Within 1–5 milliseconds of clicking Buy, Alice's app shows the filled notification. The trade is done.

```
Alice clicks BUY
      │
      ▼
  API Gateway
  ├── auth check       (~0.3ms)
  ├── rate limit check (~0.1ms)
  ├── assign order ID  (~0.05ms)
  └── route to Shard 1 (~0.2ms)
      │
      ▼
  Shard 1 (AAPL)
  ├── WAL write        (~0.5ms)
  ├── match in memory  (~0.001ms)
  └── publish event    (~0.1ms)
      │
      ▼
  Message Queue (Kafka)
  ├── → Persistence DB (async)
  ├── → Risk Service   (async)
  └── → Notification   (~0.5ms)
      │
      ▼
  Alice's WebSocket: "FILLED @ $149.95"

Total end-to-end: ~2ms
```

---

## The Order ID Problem: How Do You Generate Unique IDs at Scale?

Here's something interviewers frequently probe: the gateway needs to generate a unique order ID for every single order. With 10 gateway servers each handling 50,000 orders per second, that's 500,000 IDs per second that must be globally unique, sortable by time, and generated without any central coordination (because central coordination is a bottleneck).

Using a simple auto-incrementing database counter is immediately out — it requires a round-trip to the database on *every order*, at 500,000/second. That's a bottleneck that kills the whole system.

The industry answer is **Snowflake IDs**, a scheme invented at Twitter. A Snowflake ID is a 64-bit integer composed of three pieces packed together: a 41-bit timestamp (milliseconds since a fixed epoch), a 10-bit machine ID (uniquely assigned to each gateway server), and a 12-bit sequence counter (a local counter that resets every millisecond). 

```
64-bit Snowflake ID:
┌─────────────────────┬────────────┬──────────────┐
│  41 bits timestamp  │ 10 bits    │  12 bits     │
│  (ms since epoch)   │ machine ID │  sequence    │
└─────────────────────┴────────────┴──────────────┘
```

This gives you 4,096 unique IDs per millisecond per machine, with no coordination between machines. IDs are sortable by time (newer orders always have larger IDs), globally unique (because machine IDs are unique), and generated entirely in local memory with no database round-trip. It's an elegant solution to a problem that looks deceptively simple.

---

## Recap of Chapter 4

The API Gateway is the "lobby" of the system — handling auth, rate limiting, routing, and protocol translation so that the matching engines never have to. REST and WebSocket serve different jobs: REST for order submission, WebSocket for real-time pushes. Following the order lifecycle end-to-end is critical interview knowledge — you need to be able to trace a single order from the client's click to the filled confirmation, naming every component it touches. And Snowflake IDs solve the uniqueness problem at scale without any central coordination.

---

**Here's something to ponder before Chapter 5:** you now have matching engines publishing tens of thousands of trade events every second. The database that stores these trades, user portfolios, and order histories is going to receive an absolutely enormous write load. How do you design a database layer that can handle this — and critically, how do you make sure that *reads* (user portfolio pages, trade history) stay fast even while writes are hammering it at the same time?

This is where we talk about **database design, indexing, read replicas, and the CQRS pattern** we teased earlier. Ready for Chapter 5?

---

# Chapter 5: The Database Layer — Writes, Reads, and the CQRS Pattern

## The Problem: One Database, Two Very Different Jobs

You now have matching engines firing thousands of trade events every second, each one needing to be persisted. At the same time, thousands of traders are opening their portfolio pages, checking their order history, and watching their positions update in real time. All of this is hitting the same database.

This is where a subtle but devastating tension appears. **Writes and reads want completely opposite things from a database.**

Writes want to be fast and sequential — append a new trade, update a position, insert an order record. Reads want to be flexible and rich — "give me all trades for Alice in the last 7 days, grouped by symbol, sorted by time." These two workloads fight each other constantly. Heavy writes slow down reads because they hold locks and dirty the cache. Heavy reads slow down writes because they compete for I/O bandwidth. Running both on the same database is like using the same road for both a Formula 1 race and a Sunday farmers market. Neither works well.

The engineers realized they needed to stop treating the database as one monolithic thing, and instead design the **write path** and the **read path** as completely separate systems, each optimized for its own job. This is the **CQRS pattern** — Command Query Responsibility Segregation. The name sounds academic, but the idea is beautifully simple: *commands* (writes — place order, execute trade) go one way, and *queries* (reads — show portfolio, fetch history) go a completely different way.

---

## Building the Write Path: Optimized for Speed and Durability

The write path's only job is to record what happened as fast as possible and never lose it. Every trade execution, every order placement, every cancellation is an *event* that must be durably persisted.

The right tool here is a database optimized for high-throughput sequential writes. **Apache Cassandra** is the industry favourite for this job, and understanding *why* it's fast is important for interviews.

Most traditional databases (like PostgreSQL) use a data structure called a B-Tree on disk. Every time you write a record, the database has to find the right position in the tree, potentially read several disk pages, update them, and write them back. This involves random disk I/O, which is expensive — even on SSDs, random writes are significantly slower than sequential ones.

Cassandra instead uses a structure called an **LSM Tree** (Log-Structured Merge Tree). When a write comes in, it goes first into an in-memory buffer called a Memtable. Periodically, the Memtable is flushed to disk as an immutable sorted file called an SSTable. Because each flush is a pure sequential write (no seeking, no updating, just appending a new file), it is *extremely* fast. The tradeoff is that reads become more complex — you might have to look through several SSTables to find a record. But for the write path, that's an acceptable tradeoff.

```
  Trade events arriving
          │
          ▼
    ┌─────────────┐
    │  Memtable   │  ← in-memory, very fast writes
    │  (in RAM)   │
    └──────┬──────┘
           │ flush when full
           ▼
    ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
    │  SSTable 1  │   │  SSTable 2  │   │  SSTable 3  │
    │  (on disk)  │   │  (on disk)  │   │  (on disk)  │
    └─────────────┘   └─────────────┘   └─────────────┘
         ↑ immutable sorted files, pure sequential writes
```

Think of it like a writer who jots quick notes in a notepad (Memtable) and then periodically types them up into clean, sorted documents (SSTables). The typing is fast because you're always writing new pages, never erasing and rewriting old ones.

---

## The Read Path Problem: Queries the Write Store Can't Handle Well

Now let's think about what the read path needs to do. A trader opens their dashboard and wants to see: their current open orders, their filled trades for today, their total P&L by symbol, and real-time price updates. Every single one of these queries is fundamentally different in shape from what the write store is good at.

Cassandra is optimized for reading back data when you know exactly *what* you're looking for (give me all trades with `trade_id = XYZ`). But it struggles with the kind of flexible, aggregated queries a trading dashboard needs — filtering, grouping, joining across multiple concepts. You'd be fighting the database's data model on every query.

The CQRS insight is that you don't even try to do these reads from the write store. Instead, you build **separate read models** — databases that are purpose-built for exactly the queries you need. These read models are not the source of truth; they're derived views that are rebuilt by consuming the event stream from the write path.

Here's how the flow looks when a trade executes:

```
  TRADE_EXECUTED event
          │
          ▼
   Message Queue (Kafka)
          │
    ┌─────┼──────┐
    ▼     ▼      ▼
  Write  Read   Read
  Store  Model  Model
 (Cassandra) (PostgreSQL) (Redis)
  "what    "user portfolio  "real-time
 happened"   & history"      prices"
```

Each consumer of the Kafka event updates its own specialized store. The PostgreSQL read model might maintain a `user_positions` table that is kept up to date every time a trade fires — it's not normalized event data, it's a pre-computed view of "what does Alice currently own?" Redis might cache the latest price of every symbol in memory, updated on every trade, so that price lookups are a single in-memory key-value fetch rather than a database query.

---

## A Concrete Example: What Alice's Portfolio Query Looks Like

Let's make this tangible. Without CQRS, fetching Alice's portfolio from the raw event store would look something like this — computationally expensive every single time:

```sql
-- This runs on EVERY page load. Scanning millions of trade records.
SELECT symbol, SUM(CASE WHEN side='BUY' THEN qty ELSE -qty END) as position
FROM trades
WHERE trader_id = 'alice'
GROUP BY symbol;
```

With CQRS, you instead maintain a `user_positions` table in the read model that is updated *whenever a trade happens* — not when Alice opens her page. The table always reflects the current state:

```sql
-- This table is pre-computed and always ready.
-- Alice's page load is just a single key lookup.
SELECT symbol, position, avg_cost, unrealized_pnl
FROM user_positions
WHERE trader_id = 'alice';
```

The work shifts from "compute everything at read time" to "update incrementally at write time." Since writes happen once per trade but reads happen millions of times per day, this is a massive win. You're doing the expensive computation once and caching the result permanently, rather than re-doing it on every read.

---

## Indexing: Why Queries Go Fast or Slow

Whether you're using the write store or the read model, you'll need indexes. Indexing is one of the most frequently probed topics in system design interviews, and it's worth understanding deeply rather than just knowing the word.

An index is a separate data structure that the database maintains alongside your data, designed to answer one specific question very fast. The classic analogy is a book's index — rather than reading every page to find where "order book" is mentioned, you jump straight to the index, look up "order book," and get the page numbers immediately. The tradeoff is that the index takes up extra space and must be updated on every write.

For a trading platform, the most important indexes are on the fields you filter and sort by most frequently. On the `trades` table, you'd index `(trader_id, timestamp)` because the most common query is "all trades for this user, sorted by time." On the `orders` table, you'd index `(symbol, status, timestamp)` because the matching engine reads open orders sorted by price-time priority.

The dangerous mistake — and one interviewers specifically watch for — is **over-indexing**. If you put an index on every column "just in case," every single write to that table now has to update all those indexes simultaneously. A table with 10 indexes makes writes roughly 10x more expensive. On a write-heavy table receiving 50,000 inserts per second, this is devastating. The discipline is to index *only* the access patterns you have proven you need.

A subtler point worth mentioning: a **composite index** on `(trader_id, timestamp)` is useful for queries that filter by trader_id first. But it is *not* useful for a query that only filters by timestamp — the database can't use the index efficiently if you skip the first column. This is called the "leftmost prefix rule" and it trips up many engineers in practice.

---

## Read Replicas: Scaling Reads Horizontally

Even with perfectly designed indexes and purpose-built read models, a single database server will eventually hit a ceiling on how many reads it can serve — especially for a popular trading platform where thousands of users are constantly refreshing their dashboards.

The solution is **read replicas**. You have one primary database node that accepts all writes, and you spin up multiple read-only replicas that asynchronously receive and apply those writes. All read traffic is distributed across the replicas, while the primary handles only writes.

```
  Trade events (writes)
          │
          ▼
    ┌─────────────┐
    │   PRIMARY   │
    │  (writes)   │
    └──────┬──────┘
           │ async replication
    ┌──────┼──────┐
    ▼      ▼      ▼
┌───────┐┌───────┐┌───────┐
│Replica││Replica││Replica│
│  1    ││  2    ││  3    │
└───────┘└───────┘└───────┘
  ↑         ↑        ↑
  Read traffic distributed across replicas
```

The important nuance here is **replication lag**. Because replicas apply writes asynchronously, a replica might be a few hundred milliseconds behind the primary. For most reads this is fine — if Alice checks her portfolio 200ms after a trade, she sees the updated position. But there's a nasty edge case: if Alice places an order and then *immediately* checks her order history, she might hit a replica that hasn't received the write yet and see her order as "missing." This is called a **stale read**.

The solution for these cases is **read-your-own-writes consistency** — after a trader submits an order, their subsequent reads are routed to the primary (not a replica) for a brief window, say 5 seconds. After that window, normal replica routing resumes. This gives the replication lag time to catch up, so the trader always sees a consistent view of their own actions. Redis plays a useful role here too — you can cache the newly placed order in Redis immediately after submission, so the UI can show it to the user from cache even before the replica catches up.

---

## Putting It All Together: The Full Data Layer

At this point, the data layer of the trading platform looks like a carefully coordinated ensemble rather than a single database:

```
  Matching engine emits events
               │
               ▼
          Kafka (event stream)
               │
    ┌──────────┼──────────┐
    ▼          ▼          ▼
Cassandra  PostgreSQL    Redis
(raw event  (user read   (real-time
  store)     models)     cache)
 "history"  "portfolio"  "prices &
                          sessions"
```

Cassandra handles the firehose of raw events with high-throughput sequential writes. PostgreSQL maintains the user-facing read models — pre-computed portfolio views, trade history, P&L — with proper indexes for flexible queries. Redis handles anything that needs sub-millisecond access: current prices, session tokens, rate limit counters, and the read-your-own-writes cache.

Each database is doing *exactly the job it's best at*. Nobody is asking Cassandra to do flexible joins. Nobody is asking Redis to store 5 years of trade history. This specialization is what lets the system scale to the load a real exchange sees.

---

## Recap of Chapter 5

The central lesson is that treating the database as a single monolithic system is the root cause of most performance problems at scale. CQRS separates the write path (optimized for durability and throughput, using Cassandra's LSM-tree architecture) from the read path (optimized for flexible queries, using pre-computed PostgreSQL views and Redis caches). Kafka acts as the connective tissue, carrying events from the write side to all the read-side consumers asynchronously. Indexing is powerful but must be disciplined — over-indexing hurts writes as much as missing indexes hurt reads. Read replicas scale the read tier horizontally, but replication lag requires the read-your-own-writes pattern to keep individual users' views consistent.

---

**Here's the question to sit with before Chapter 6:** everything we've built so far assumes that when an order arrives and a trade fires, either *everything* succeeds — the WAL is written, the match happens, the Kafka event is published, the database is updated — or the system crashes and we replay from the WAL. But what about partial failures? What if the matching engine processes the trade successfully but the Kafka publish fails? What if the database write succeeds but the user's balance deduction fails? You end up in a state where the system *thinks* a trade happened but the accounting doesn't agree. In financial systems, this kind of inconsistency can mean real money disappearing.

This is the problem of **distributed transactions and exactly-once processing** — arguably the hardest problem in the entire design. Ready for Chapter 6?

---

# Chapter 6: Distributed Transactions — The Hardest Problem in the Design

## The Problem: Partial Failures Are Worse Than Total Failures

Let me start with why partial failures are so much scarier than total failures, because this is the intuition you need before anything else makes sense.

If the entire system crashes, you know exactly what to do — replay the WAL, rebuild state, come back up. Everything is consistent because nothing happened. But a *partial* failure is insidious. Some things happened, some didn't, and the system has no automatic way to know which is which. In a financial system, this asymmetry can manifest as real money either disappearing or being double-counted.

Here's the specific scenario that kept the engineers up at night. Suppose a trade executes between Alice (buyer) and Bob (seller). The matching engine successfully matches them in memory, writes the event to the WAL, and then needs to do three more things: publish the event to Kafka, deduct $15,000 from Alice's cash balance, and credit Bob's cash balance with $15,000. Now suppose the Kafka publish succeeds, Alice's deduction succeeds, and then the server crashes before Bob's credit happens. Alice has lost $15,000. Bob never received it. The money has vanished from the system entirely.

Or consider the reverse — suppose the matching engine processes the trade, and due to a network glitch it *thinks* the Kafka publish failed, so it retries and publishes the same trade event twice. Now the downstream consumer processes both events, Alice gets charged twice, and Bob gets credited twice. $15,000 was conjured out of thin air.

Both scenarios are catastrophic. The first is called a **lost update**. The second is called a **duplicate processing** problem. Together, they define the core challenge: in a distributed system, how do you guarantee that a trade either happens *completely* or *not at all*, with no in-between states, and exactly once?

---

## The Classic Solution Everyone Reaches For First: Two-Phase Commit (2PC)

The first tool engineers historically reached for was the **Two-Phase Commit protocol**, or 2PC. The idea is elegant on the surface. You introduce a central **coordinator** that orchestrates the transaction across all participating services. The protocol runs in two phases — hence the name.

In **Phase 1 (Prepare)**, the coordinator sends a "prepare" message to all participants — the database, the Kafka broker, the balance service. Each participant does all the work needed to complete the transaction but doesn't commit yet. They write to a temporary holding area and reply either "yes, I'm ready to commit" or "no, I can't do it." Think of it like everyone in a group chat replying "ready" before anyone actually sends the group message.

In **Phase 2 (Commit or Abort)**, if *all* participants said yes, the coordinator sends a "commit" message and everyone finalizes. If *any* participant said no, the coordinator sends "abort" and everyone rolls back their temporary work, leaving the system as if nothing had happened.

```
Coordinator
     │
     │── "Prepare?" ──→ Database       "Yes, ready"
     │── "Prepare?" ──→ Kafka          "Yes, ready"
     │── "Prepare?" ──→ Balance Svc    "Yes, ready"
     │
     │   (all said yes)
     │
     │── "Commit" ──→ Database         commits
     │── "Commit" ──→ Kafka            commits
     │── "Commit" ──→ Balance Svc      commits
```

This sounds perfect. In practice, it has two serious problems that make it unsuitable for a high-throughput trading platform.

The first problem is **blocking**. Between Phase 1 and Phase 2, every participant is holding locks — they've reserved resources and are waiting for the coordinator's commit signal. If the coordinator crashes after Phase 1 but before Phase 2, every participant is stuck holding those locks indefinitely. They can't commit because they never got the signal. They can't abort because maybe the coordinator sent the commit to some of them before crashing. They just wait. This is called the **in-doubt window**, and it blocks other transactions from accessing those resources. On a system doing 500,000 trades per second, even a 10ms in-doubt window is catastrophic.

The second problem is **latency**. 2PC requires at least two full network round-trips to all participants before a transaction can complete. For a trading engine that must respond in under a millisecond, adding two synchronous network round-trips — each potentially taking 1–5ms — makes it fundamentally incompatible with the performance requirements.

2PC works fine for traditional enterprise systems doing hundreds of transactions per second. At trading platform scale, it simply doesn't fit.

---

## The Better Mental Model: The SAGA Pattern

The engineers needed a way to handle multi-step operations across services without holding locks across the network. The answer is the **SAGA pattern**, and the shift in thinking it requires is subtle but profound.

Instead of trying to make all steps happen atomically in one big transaction, a SAGA breaks the operation into a *sequence of smaller, independent local transactions*, each of which can be individually committed. If a later step fails, you don't try to "undo" earlier steps by rolling them back — you instead execute **compensating transactions** that semantically reverse what was done.

Think of it like booking a holiday. When you book flights, hotels, and a rental car separately, there's no global transaction holding all three together. If the hotel booking fails after flights are confirmed, you don't time-travel to un-buy the flights. Instead you run a compensation: you cancel the flights (a forward action that undoes the effect). The SAGA is the sequence of bookings plus the defined cancellation procedures for each step.

For a trade execution, the SAGA looks like this:

```
Step 1: Matching engine writes TRADE_EXECUTED to WAL
            └── if fails: nothing to compensate, just retry

Step 2: Publish event to Kafka
            └── if fails: compensate by marking trade as failed in WAL

Step 3: Deduct from Alice's balance
            └── if fails: compensate Step 2 by publishing TRADE_REVERSED event

Step 4: Credit Bob's balance
            └── if fails: compensate Step 3 by crediting Alice back
                          compensate Step 2 by publishing TRADE_REVERSED event
```

Each step is a small, local, immediately committed transaction. The magic is that every step has a defined compensation — a known way to undo its effect if something downstream fails. The system is never stuck in limbo because there are no long-held locks. If something goes wrong, you run the compensations and arrive at a consistent state, even if that state is "the trade didn't happen."

The tricky thing about SAGAs that you should mention in interviews is that during the saga's execution, the system is in a **temporarily inconsistent state**. Between Step 3 and Step 4, Alice's balance has been deducted but Bob's hasn't been credited yet. For a brief window, $15,000 has "left" Alice but not "arrived" at Bob. This is called an **intermediate state**, and it means other parts of the system must be designed to tolerate seeing these temporary inconsistencies — for instance, Bob shouldn't be allowed to trade based on money that's in-flight. This is managed through careful use of balance reservations and "pending" states, which we'll revisit when we talk about the risk engine.

---

## The Exactly-Once Problem: Idempotency is the Key

The SAGA solves the "what do we do when something fails" problem. But there's a related problem that's just as important: **what if a step succeeds, but we're not sure if it succeeded?**

This sounds strange, but it's one of the most common situations in distributed systems. A service sends a request to Kafka, Kafka processes it, but the network drops the acknowledgement on the way back. The sending service never received the "ok" — so from its perspective, the request might have failed. Should it retry? If it does retry and the original *did* succeed, you now have a duplicate event in Kafka, which will cause downstream consumers to process the same trade twice.

The universal solution is **idempotency**. An operation is idempotent if performing it multiple times has the same effect as performing it once. Making every step in the pipeline idempotent means retrying is always safe — duplicates become harmless.

In practice, you achieve idempotency through **idempotency keys**. Every event carries a globally unique ID (remember Snowflake IDs from Chapter 4?). Every consumer, before processing an event, checks a fast key-value store like Redis: "have I already processed event with ID `abc-123`?" If yes, it acknowledges the event and skips processing — this is a duplicate. If no, it processes the event and writes `abc-123` to the store before acknowledging.

```
Event arrives with id: "trade-abc-123"
         │
         ▼
Check Redis: "seen trade-abc-123 before?"
         │
    ┌────┴────┐
  Yes          No
    │          │
  Skip       Process
  (duplicate) + write "trade-abc-123" to Redis
              + acknowledge event
```

This pattern is called **at-least-once delivery with idempotent consumers**, and it's the production standard. Kafka itself guarantees at-least-once delivery (every event will be delivered, possibly more than once). Your consumer guarantees that processing is idempotent. Together they achieve the effect of exactly-once semantics without the performance cost of 2PC.

The Redis deduplication store must be fast — you're checking it on every single event, potentially millions per second. You also need to set an expiry on these keys (say, 24 hours), otherwise the Redis store grows without bound. Since duplicate events typically arrive within seconds of the original, a 24-hour window is more than sufficient coverage.

---

## The Outbox Pattern: Tying the WAL and Kafka Together

There's still one gap we haven't plugged. The matching engine writes to its WAL and then publishes to Kafka as two separate actions. What if it writes the WAL successfully but crashes before publishing to Kafka? The trade is in the WAL but never reaches Kafka, so downstream services — the balance updater, the portfolio service — never know it happened. The WAL and Kafka have diverged.

The fix is a pattern called the **Transactional Outbox**. Instead of writing to Kafka directly, the matching engine writes the event to a local "outbox" table in the *same* atomic write as the WAL entry. A separate background process — called the **outbox relay** — reads from the outbox table and publishes to Kafka, then marks the entry as published.

```
Matching Engine (single atomic write):
  ├── WAL entry: TRADE_EXECUTED { id: abc-123 ... }
  └── Outbox table: { event_id: abc-123, published: false }

(separately, background process)
Outbox Relay:
  ├── reads unpublished rows from outbox table
  ├── publishes each to Kafka
  └── marks row as published: true
```

Now the worst case is that the relay publishes to Kafka but crashes before marking the row as published, causing a duplicate publish. But you've already solved that with idempotent consumers — the downstream service will detect the duplicate via its Redis deduplication check and safely skip it. The WAL and Kafka are now guaranteed to be consistent, and any duplicates are harmlessly filtered.

This combination — SAGA for multi-step operations, idempotent consumers for duplicate protection, and the outbox pattern for bridging local writes to the message queue — is what a production trading platform actually uses. It's considerably more complex than 2PC, but it achieves correctness without blocking and without sacrificing the microsecond performance the matching engine needs.

---

## Recap of Chapter 6

The core lesson is that distributed correctness in a financial system isn't achieved by one big atomic transaction — it's achieved by designing every step to be independently safe. SAGAs replace atomic transactions with sequences of local commits plus defined compensation logic, eliminating long-held locks across the network. Idempotency keys with Redis deduplication make retries safe, achieving effectively-once semantics on top of at-least-once delivery. The outbox pattern closes the gap between the local WAL write and the Kafka publish, ensuring those two always stay in sync. Together these three patterns are the foundation of a distributed financial system that is both fast and correct.

A useful mental model: think of the whole system as a relay race. Each runner (service) owns their leg of the race completely and independently. If a runner drops the baton, there's a defined rule for what happens next — not a global pause where everyone freezes and waits. The race continues safely and predictably no matter what goes wrong.

---

**Before Chapter 7, here's the scenario to think about:** everything we've built so far protects against *technical* failures — crashes, network drops, duplicate messages. But what about *logical* failures? What if a trader tries to sell 1,000 shares they don't own? What if a trading algorithm goes berserk and submits $10 billion worth of orders in 3 seconds? What if two legitimate trades, when combined, would give one trader an illegally large position in a stock? These aren't system failures — the infrastructure is working perfectly. They're business logic violations that can cause real-world financial harm and regulatory penalties.

This is the job of the **Risk Engine** — the guardian that sits between the trader and the matching engine, deciding in microseconds whether an order is safe to proceed. Ready for Chapter 7?

---

# Chapter 7: The Risk Engine — The Guardian at the Gate

## The Problem: A Technically Perfect System Can Still Cause Financial Catastrophe

Let me tell you about a real event that shaped how the industry thinks about this problem. In August 2012, a trading firm called Knight Capital deployed a new piece of software. Due to a configuration error, an old dormant algorithm accidentally reactivated alongside the new one. Within 45 minutes, the system had submitted millions of erroneous orders, accumulating a $7 billion unintended position in various stocks. By the time humans noticed and intervened, the firm had lost $440 million — nearly four times its annual profit. Knight Capital nearly went bankrupt from a single 45-minute software bug.

The matching engine did exactly what it was designed to do. Every order was valid in isolation. The system had no technical failure whatsoever. What was missing was a layer that asked the question: *"Yes, this order is technically valid — but should we actually allow it?"*

That layer is the **Risk Engine**, and after Knight Capital, every serious trading platform treats it as non-negotiable infrastructure, not an afterthought.

---

## What the Risk Engine Actually Checks

The risk engine sits between the API Gateway and the matching engine. Every single order passes through it before being allowed to proceed. Think of it as a very fast, very smart bouncer at a nightclub — it doesn't care whether your ID is real (that's authentication's job), it cares whether *letting you in right now* is a good idea.

There are three categories of checks it performs, and understanding each one matters for interviews.

**Pre-trade checks** are the most critical because they happen *before* the order reaches the matching engine. If a check fails here, the order is rejected cleanly — nothing has happened, no state has changed, no compensation is needed. This is the cheapest possible place to catch a problem.

The first pre-trade check is **position limits**. Every trader has a maximum allowed position in any single stock. If Alice already owns 10,000 shares of AAPL and her limit is 10,000, she cannot buy another share. The risk engine checks her current position — fetched from an in-memory cache, never a slow database query — and rejects the order instantly if it would breach the limit.

The second check is **funds sufficiency**. Before Alice can buy $15,000 worth of stock, the risk engine verifies she actually has $15,000 available. Critically, it checks *available* funds, not just total funds — money that is already reserved for pending orders doesn't count. This is why the SAGA pattern from Chapter 6 uses balance *reservations*: when an order is submitted, the required funds are immediately reserved (removed from "available"), so subsequent orders can't double-spend the same money.

The third check is **order rate limits**, which are distinct from the API gateway's rate limits. The gateway limits how many HTTP requests a client can make. The risk engine limits how many *orders* a trader can submit per second — a much more semantically meaningful constraint. A buggy algorithm might make a single API call that submits 10,000 orders in a batch. The gateway rate limiter wouldn't catch this, but the risk engine's order rate limit would.

The fourth check is **price sanity validation**, sometimes called a "fat finger" check. If AAPL is currently trading at around $150 and a trader submits an order to buy at $15,000, that's almost certainly a typo — someone added an extra zero. The risk engine compares the submitted price against the current market price and rejects any order that deviates by more than a configured threshold, say 20%. This alone would have caught some of the Knight Capital damage.

```
Order arrives
      │
      ▼
┌─────────────────────────────┐
│         RISK ENGINE         │
│                             │
│  1. Position limit check    │
│  2. Funds sufficiency check │
│  3. Order rate limit check  │
│  4. Price sanity check      │
│  5. Regulatory checks       │
│                             │
└──────────┬──────────────────┘
           │
     ┌─────┴──────┐
   REJECT       APPROVE
     │               │
  back to        Matching
  trader         Engine
```

**Regulatory checks** form the fifth and often most complex category. Financial markets have strict rules about market manipulation. Two common violations the risk engine must detect are **wash trading** (a trader simultaneously buying and selling the same stock to create the illusion of volume, inflating the price artificially) and **position concentration** (a single trader or group accumulating such a large percentage of a stock's total shares that they can manipulate its price). These checks require the risk engine to look across a trader's entire order history, not just the current order, which makes them more expensive — they run on a slightly longer time window, asynchronously, and can cancel orders retroactively if a violation is detected.

---

## The Speed Problem: How Do You Run All These Checks in Microseconds?

Here's the tension that makes the risk engine architecturally interesting. You need to run five categories of checks on every order, potentially 500,000 times per second, before the order reaches the matching engine. If the risk engine adds even 1 millisecond of latency, you've broken the sub-millisecond promise the platform makes to traders.

The answer is that the risk engine must keep *all the state it needs* in memory, just like the matching engine does. It maintains an in-memory mirror of every trader's current position, available balance, and order submission rate. These values are updated asynchronously from the event stream — every time a trade executes or an order is placed, a Kafka consumer updates the risk engine's in-memory state. The checks themselves never touch a disk or make a network call — it's purely a computation on in-memory data.

```
  Trade/Order events (Kafka)
            │
            ▼ (async, background)
  ┌─────────────────────────┐
  │    Risk Engine State    │
  │    (all in memory)      │
  │                         │
  │  trader_positions: {    │
  │    alice: {AAPL: 9500}  │
  │    bob:   {TSLA: 200}   │
  │  }                      │
  │  available_funds: {     │
  │    alice: $50,000       │
  │    bob:   $12,000       │
  │  }                      │
  │  order_rates: {         │
  │    alice: 45/sec        │
  │  }                      │
  └─────────────────────────┘
            ↑
  (Order checks read from here — no disk, no network)
```

The important subtlety here is that this in-memory state is *eventually consistent* with the true source of truth in the database. There will always be a tiny lag — perhaps a few milliseconds — between a trade executing and the risk engine's in-memory state reflecting it. In practice, this is acceptable because the risk engine errs on the side of caution: it uses slightly conservative estimates of available funds (reserving a bit more than needed) to absorb any brief inconsistency.

---

## Circuit Breakers: The Last Line of Defense

Even with all these checks, there's a category of problem none of them can catch: *emergent* failures. A single order might pass every check individually, but a *pattern* of orders across hundreds of traders could indicate that something is systemically wrong — a market crash, a data feed corruption feeding wrong prices to hundreds of algorithms simultaneously, or a coordinated attack.

The risk engine implements **circuit breakers** — automatic pauses triggered by system-wide anomalies — borrowed directly from electrical engineering. In a circuit, a breaker trips when current exceeds a safe threshold, cutting the flow and preventing damage. In the trading platform, a circuit breaker trips when certain thresholds are crossed at the market level, not just the individual trader level.

The most common circuit breaker is the **market-wide halt**: if the price of any stock moves more than a configured percentage in a short window — say 10% in 5 minutes — all trading in that stock is paused automatically. This is called a **limit up/limit down** rule and it's mandated by regulators precisely to prevent flash crashes. In 2010, the Dow Jones Industrial Average dropped nearly 1,000 points in minutes during what became known as the Flash Crash, triggered partly by algorithmic trading cascades that circuit breakers would have interrupted.

A second type is the **trading account halt**: if a single account submits more than N orders in M seconds that all get rejected (which is a strong signal of a runaway algorithm), that account is automatically suspended and flagged for human review.

```
  Normal state: circuit CLOSED → orders flow through

  Trigger detected:
  ├── Stock price moved 10% in 5 minutes
  ├── Single account: 1000 rejected orders in 10 seconds
  └── System-wide: error rate > 1% of all orders

  Circuit OPENS → all new orders for affected scope are rejected
                → alert sent to human operators
                → auto-closes after configured cool-down period
                   (or manual override by operator)
```

The circuit breaker pattern is worth mentioning explicitly in interviews because it demonstrates that you've thought about *recovery from abnormal states* — not just normal operations. A system that can detect its own anomalies and safely pause is far more resilient than one that blindly continues processing until a human notices something is wrong.

---

## The Risk Engine and the Matching Engine: Keeping State in Sync

There's a subtle but important design question here: the risk engine and the matching engine each maintain their own in-memory state. The matching engine knows the current order book; the risk engine knows current positions and balances. What happens if they drift out of sync?

Imagine a scenario where the risk engine approves an order (Alice has $50,000 available), but between the approval and the order reaching the matching engine, a different order from Alice executes on a different shard and consumes that $50,000. Now the matching engine processes Alice's new order, but the risk engine's approval was based on stale data.

This is handled through the **reservation model** mentioned earlier. When the risk engine approves an order, it doesn't just check Alice's balance — it *reserves* the required funds immediately, deducting them from "available" in its own in-memory state. Now if a second order arrives a microsecond later for Alice, the risk engine sees the already-reduced available balance and correctly rejects or limits it. The reservation acts as an optimistic lock that prevents double-spending without requiring coordination between the risk engine and the matching engine.

```
Alice has $50,000 available

Order 1 arrives: Buy AAPL for $30,000
  Risk engine: check → $50,000 available ✓
  Risk engine: reserve $30,000 → available now = $20,000
  Order 1 forwarded to matching engine

Order 2 arrives (50ms later): Buy TSLA for $25,000
  Risk engine: check → $20,000 available
  $25,000 > $20,000 → REJECTED
  (correctly prevented double-spending)

Later, when Order 1 fills:
  Kafka event updates risk engine state
  $30,000 reservation consumed → position updated
```

The reservation is eventually reconciled against the actual trade outcome via the Kafka event stream. If an order is cancelled rather than filled, the reservation is released and the funds become available again.

---

## Recap of Chapter 7

The risk engine is the layer that separates a technically correct system from a financially safe one. It sits between the gateway and the matching engine, running pre-trade checks — position limits, funds sufficiency, order rate limits, price sanity, and regulatory validations — entirely in memory to maintain microsecond speed. Its in-memory state is kept current through the same Kafka event stream that drives the rest of the system. Circuit breakers provide a system-level safety valve, automatically halting trading when anomalous patterns are detected before human operators can intervene. And the reservation model ensures that the risk engine's approvals are binding — once funds are reserved, they can't be double-spent by a second concurrent order.

The most important mental model here is that the risk engine is the system's immune system. Most of the time it's invisible, adding a nearly imperceptible overhead to every order. But when something goes wrong — a buggy algorithm, a market anomaly, a data feed corruption — it's the thing that stands between a bad day and a Knight Capital-level catastrophe.

---

**Before Chapter 8, here's what to think about:** you now have a complete trading engine — orders flow in, risks are checked, trades execute, events are persisted, and positions are updated. But there are thousands of traders who aren't actively placing orders at this moment — they're just *watching*. They want to see live price updates, order book depth, their portfolio value ticking up and down in real time. Some of them are individual investors on a web app; some are institutional traders with dedicated servers consuming a raw market data feed at gigabytes per second.

How do you broadcast market data to potentially millions of passive observers without that read load interfering with the write-critical matching engine? This is the **market data distribution problem**, and it requires a completely different architectural pattern — one built around **pub/sub fan-out, feed handlers, and CDN-level distribution**. Ready for Chapter 8?

---

# Chapter 8: Market Data Distribution — Broadcasting to the World

## The Problem: A Firehose Pointed at Millions of People

Let's start by understanding the scale of what we're talking about. The matching engine is currently producing a continuous stream of events — every trade that executes, every time the order book changes, every new order that arrives or gets cancelled. For a liquid stock like AAPL during market hours, the order book might update thousands of times per second. Across all 10,000 symbols on the platform, you might have **1–5 million market data events per second** flowing out of the matching engines.

Now consider who wants to see this data. There are retail traders on a web app who want to see a price chart update every second or two. There are professional traders on desktop terminals who want every single order book change the moment it happens. There are institutional investors with co-located servers who want the raw binary feed with sub-millisecond latency. There are mobile app users who just want to know if their stock went up today.

The naive approach — having every interested party connect directly to the matching engine and pull updates — would immediately destroy everything you've built. The matching engine would spend all its time handling read connections instead of matching orders. One million passive observers would overwhelm a system designed to process 500,000 active orders per second.

The fundamental principle the engineers arrived at is this: **the matching engine should never know that observers exist.** It just fires events into a pipeline and forgets about them. Everything downstream is someone else's problem. This principle — where producers don't know about consumers — is the essence of the **publish-subscribe (pub/sub) pattern**.

---

## The Architecture: A Staged Fan-Out Pipeline

The solution is to build a pipeline that progressively fans out the data — starting narrow at the matching engine and widening as it flows toward the millions of end consumers. Think of it like a river delta. The Amazon River carries enormous volume through a single channel, but by the time it reaches the ocean it has spread across hundreds of smaller streams, each serving a different region. No single stream carries the full volume of the original river.

The pipeline has four distinct stages, and each one solves a specific problem.

**Stage 1 — The Matching Engine publishes raw events to Kafka.** This is the same Kafka pipeline from the previous chapters. The matching engine doesn't know or care what happens next. It just appends events to a Kafka topic, one topic per symbol. AAPL's events go to the `market-data.AAPL` topic, TSLA's go to `market-data.TSLA`, and so on. Kafka retains these events for a configurable window — say 7 days — so late-joining consumers can catch up.

**Stage 2 — Feed Handlers consume from Kafka and normalize the data.** Raw matching engine events are in an internal binary format optimized for speed, not readability. Feed handlers are specialized consumers that read from Kafka, translate events into standardized formats (more on this shortly), and compute derived data like the current best bid/ask, last traded price, and volume-weighted average price (VWAP). Feed handlers are stateful — they maintain a running picture of the order book for each symbol — but they're read-only. They never write back to the matching engine.

**Stage 3 — Distribution servers receive from feed handlers and push to subscribers.** This is where the fan-out actually happens. Each distribution server maintains thousands of open WebSocket connections to end clients. When a feed handler publishes a price update, the distribution server identifies which connected clients have subscribed to that symbol and pushes the update to each of them. Because distribution servers are stateless with respect to market data (they just relay what feed handlers send them), you can run as many as you need and route clients to any of them.

**Stage 4 — CDN and caching layer for slower consumers.** Not everyone needs real-time millisecond updates. A mobile app user checking a price once every few seconds is perfectly served by a cached value. A CDN node geographically close to the user can serve the last known price from its own cache, updated every second by a background process. This takes an enormous amount of load off the distribution servers for users who don't actually need the real-time feed.

```
  Matching Engine
        │
        │ raw events
        ▼
      Kafka
  (topic per symbol)
        │
        │ consume
        ▼
  Feed Handlers
  (normalize + compute derived data)
        │
        │ normalized updates
   ┌────┼────┐
   ▼    ▼    ▼
  DS-1 DS-2 DS-3   ← Distribution Servers
  (WebSocket connections to clients)
        │
   ┌────┼────┐
   ▼    ▼    ▼
Pro    Retail   Mobile
traders  web     app
(raw)  (1s bar) (cached)
```

---

## The Fan-Out Problem: One Event, One Million Recipients

Let's zoom into Stage 3 and think carefully about what happens when a single AAPL trade executes. That one event needs to reach every single client who has subscribed to AAPL price updates. If one million clients are watching AAPL, that's one event in and one million pushes out. This is called the **fan-out problem**, and it's one of the hardest scaling challenges in the entire system.

The naive implementation in a single distribution server would be a loop: iterate over all one million AAPL subscribers and push the update to each WebSocket connection. Even if each push takes just one microsecond, one million pushes takes one second. By the time the last subscriber receives the update, it's already a second stale — completely unacceptable.

The solution has two parts. First, you distribute the subscriber population across many distribution servers. If you have 100 distribution servers and one million AAPL subscribers, each server handles 10,000 subscribers. Each server pushes to 10,000 connections in parallel (using asynchronous I/O, so it doesn't wait for each push to complete before starting the next). This brings the fan-out time down to the microsecond range.

Second, you use a **topic-based subscription model** where each distribution server only receives updates for the symbols its connected clients have subscribed to. A distribution server with no AAPL subscribers never receives AAPL updates — there's no wasted bandwidth. This requires a subscription registry — a central store (typically Redis) that maps each symbol to the set of distribution servers that have at least one subscriber for that symbol. When a client subscribes to AAPL, their distribution server registers itself in Redis as an AAPL listener. Feed handlers consult this registry to determine where to send each update.

```
Client subscribes to AAPL:
  → DS-7 registers in Redis: "AAPL → [DS-1, DS-7, DS-23]"

AAPL trade executes:
  → Feed handler checks Redis
  → Sends update only to DS-1, DS-7, DS-23
  → DS-7 pushes to its 10,000 AAPL subscribers in parallel
  → DS-2, DS-5, DS-99... receive nothing (no AAPL subscribers)
```

This combination — horizontal scaling of distribution servers plus topic-based selective delivery — is how you fan out one event to one million recipients in milliseconds.

---

## Different Consumers Need Different Data Formats

Here's something that surprises many people when they first encounter this problem: different categories of consumers don't just want different *speeds* of data — they want fundamentally different *representations* of the same data.

A retail trader on a web app wants a simple JSON object: `{ symbol: "AAPL", price: 150.05, change: +1.2% }`. They don't care about the full order book depth. They want something a browser can parse easily and display on a chart.

A professional algorithmic trader wants **Level 2 data** — the full order book showing every bid and ask at every price level, not just the best one. They want to see that there are 5,000 shares waiting to be bought at $149.90, 8,000 at $149.85, and so on. This lets them anticipate price movements based on where large orders are clustered. Level 2 data is roughly 50–100x larger than simple price data.

An institutional investor with a co-located server wants the raw binary feed in **FIX protocol** or a proprietary binary format, with no JSON serialization overhead whatsoever, delivered over a direct network connection with guaranteed microsecond latency. They're paying a premium specifically for the lowest possible latency.

The feed handler layer solves this by producing multiple output streams from the same input. One feed handler reads the raw Kafka events and produces three outputs simultaneously: a "ticker" stream of simple last-trade prices, a "depth" stream of full order book snapshots, and a "raw" binary stream for institutional consumers. Each distribution tier subscribes to the stream appropriate for its clients.

---

## Snapshots vs. Deltas: A Subtlety Worth Knowing

There's a nuance in how market data is transmitted that's worth understanding for interviews because it reveals a real engineering tradeoff.

When a client first connects and subscribes to AAPL, they need the *current* state of the order book — all the bids and asks right now. This is called a **snapshot**. It might be several kilobytes of data.

After that, they don't need the full order book every time something changes. They just need to know what *changed* — "the bid at $149.90 increased from 5,000 to 6,200 shares." This is called a **delta** or **incremental update**. It might be just 20 bytes.

Transmitting full snapshots on every update would be catastrophically wasteful — imagine sending 5KB of data 1,000 times per second per subscriber. Instead, the feed handler sends one snapshot when a client connects, and then only deltas from that point forward. The client maintains its own local copy of the order book and applies each delta as it arrives, keeping its local copy perfectly in sync.

The tricky problem is what happens when a client misses a delta — due to a brief network hiccup, say. Now their local order book copy has a gap. Applying subsequent deltas on top of a gapped state produces a corrupted picture. The solution is **sequence numbers**: every delta carries a sequence number, and if a client receives sequence 1042 after 1040 (missing 1041), it knows it has a gap. It immediately discards its local state and requests a fresh snapshot, then resumes processing deltas from that point. This sequence number check is a simple but critical piece of correctness logic that every serious market data client implements.

---

## The Slow Consumer Problem

There's one more failure mode worth discussing because it comes up in interviews and reveals mature systems thinking. What happens when a distribution server's connection to a particular client is slow — maybe the client is on a poor mobile network and can't consume data as fast as the server is producing it?

If the server buffers all the undelivered updates, the buffer grows without bound and eventually exhausts memory. If it drops updates, the client's view of the market becomes inconsistent — they might see a price jump from $150 to $155 without seeing the intermediate prices, which can cause their trading algorithm to make bad decisions.

The production solution is a combination of two things. For clients who need every update (professional traders), the server uses a fixed-size ring buffer per connection. If the buffer fills up, the server disconnects the client and forces them to reconnect — getting a fresh snapshot and restarting the delta stream. This is harsh but honest. A stale, gapped feed is worse than no feed.

For clients who only need periodic updates (retail web users), the server uses a **conflation** strategy: if multiple updates for the same symbol arrive before the previous one was delivered, only the *latest* one is sent. The intermediate updates are discarded because the client only cares about the current price, not the path it took to get there. Conflation dramatically reduces bandwidth for slow clients while keeping their view roughly accurate.

---

## Recap of Chapter 8

The central lesson is that the market data problem is fundamentally a *read scaling* problem, completely separate from the write scaling of the matching engine. The staged pipeline — Kafka to feed handlers to distribution servers to clients — progressively fans out data from one source to millions of consumers, with each stage solving a specific problem. Topic-based selective delivery prevents distribution servers from receiving data their clients don't need. Multiple output formats serve the different needs of retail, professional, and institutional consumers. The snapshot-plus-delta model keeps bandwidth efficient while maintaining consistency, with sequence numbers as the safety net for detecting gaps. And the slow consumer problem is handled by either forced reconnection (for complete-data consumers) or conflation (for latest-value consumers).

The mental model that ties it all together is the river delta. The matching engine is the single source of truth upstream. By the time the data reaches consumers, it has branched into many specialized streams — each the right size, speed, and format for its particular audience — without any of that complexity touching the matching engine itself.

---

**Before Chapter 9, here's the question to sit with:** you now have a complete, working, horizontally scaled trading platform. But how do you know it's actually *working correctly* at any given moment? How do you know a particular shard isn't falling behind? How do you detect that the risk engine's in-memory state has drifted from the database? How do you know a feed handler is producing stale prices because its Kafka consumer is lagging? And when something *does* go wrong at 3 AM, how do you alert the right engineer within seconds rather than waiting for a trader to call and complain?

This is the domain of **observability — metrics, distributed tracing, alerting, and the on-call engineering culture** that keeps a financial system running 24/7. Ready for Chapter 9?

---

# Chapter 9: Observability — Knowing What's Happening Inside a Black Box

## The Problem: A System That Can Fail Silently

Here's a scenario that should genuinely frighten you. It's 2:30 PM on a busy trading day. The matching engine is running. Orders are being accepted. The website shows a green status page. But unknown to everyone, the Kafka consumer feeding the risk engine's in-memory state has been lagging 45 seconds behind for the past 10 minutes. The risk engine is making decisions based on positions and balances that are nearly a minute stale. Traders are being approved for orders they shouldn't be, and rejected for orders they should be allowed to place. Real money is being mispriced.

Nobody knows. No alarm has fired. No engineer has been paged. The system *looks* healthy from the outside because orders are still flowing through. The damage is accumulating silently.

This is the nightmare that observability is designed to prevent. The goal is simple to state but hard to achieve: **at any moment, any engineer should be able to look at a dashboard and know with confidence exactly what the system is doing, where it's healthy, and where it's struggling** — before a trader calls to complain.

---

## The Three Pillars of Observability

The industry has converged on three complementary tools for understanding what a distributed system is doing. Each one answers a different question, and you need all three because no single one is sufficient on its own.

**Metrics** answer the question *"how is the system behaving overall right now?"* They are numerical measurements collected at regular intervals — things like "the matching engine is processing 47,293 orders per second" or "Kafka consumer lag on the risk engine topic is 12 events" or "the 99th percentile order-to-fill latency is 2.3 milliseconds." Metrics are cheap to store because they're just numbers, and they're perfect for dashboards and alerting thresholds. If the Kafka lag number suddenly jumps from 12 to 45,000, your alerting system fires immediately. The weakness of metrics is that they tell you *that* something is wrong but rarely *why*.

**Logs** answer the question *"what exactly happened?"* Every significant event in the system writes a structured log entry — a JSON object with a timestamp, a severity level, a description, and relevant context like order IDs, trader IDs, and shard numbers. When the risk engine rejects an order, it logs exactly why. When the matching engine executes a trade, it logs the full details. Logs are the forensic evidence you use after something has gone wrong to reconstruct exactly what happened and in what sequence. The weakness of logs is that at 500,000 events per second, you're generating enormous volumes of text, and finding the relevant needle in that haystack is genuinely hard.

**Traces** answer the question *"what path did this specific request take through the system, and how long did each step take?"* A trace follows a single order from the moment it enters the API Gateway to the moment the fill confirmation reaches the trader, recording a timestamped "span" for each component it passes through. If a particular order took 47 milliseconds instead of the expected 2 milliseconds, the trace tells you exactly which component was responsible — the auth check took 0.3ms, the risk engine took 0.2ms, the Kafka publish took 44ms. The trace pinpoints the bottleneck precisely. The weakness of traces is that they're expensive to collect and store for every request, so most systems sample them — collecting full traces for perhaps 1% of requests, with higher sampling rates triggered automatically when latency spikes are detected.

Together these three form a complete picture. Metrics tell you something is wrong. Logs tell you what happened. Traces tell you where the time went.

---

## What You Actually Measure in a Trading Platform

Understanding the *concept* of metrics is one thing. Knowing *which specific metrics matter* for a trading platform is what separates a good interview answer from a great one. Let me walk through the most important ones by system component.

For the **matching engine**, the critical metrics are orders processed per second (throughput), the time from order received to match decision (matching latency — this should be in the single-digit microsecond range), the size of the in-memory order book (number of open orders per symbol), and the WAL write latency (how long each sequential disk write is taking). If WAL write latency climbs above a few hundred microseconds, the disk is becoming a bottleneck.

For the **Kafka pipeline**, the single most important metric is **consumer lag** — the difference between the latest event in a Kafka topic and the last event a consumer has processed. A consumer lag of 0 means the consumer is perfectly keeping up. A lag of 10,000 means the consumer is 10,000 events behind and falling further behind with every passing second. Consumer lag on the risk engine's topic is particularly dangerous, as the 45-second scenario described above demonstrates. You want an alert that fires the instant lag on any safety-critical consumer exceeds a few hundred events.

```
Kafka topic:    [event 1][event 2]...[event 50,000]
                                                  ↑
                                           latest offset

Risk engine consumer:       [event 1]...[event 47,500]
                                                 ↑
                                          consumer offset

Consumer lag = 50,000 - 47,500 = 2,500 events  ← alert if too high
```

For the **API Gateway**, you measure request rate per second, error rates broken down by error type (4xx client errors vs 5xx server errors), and the latency distribution of requests — specifically the 50th, 95th, and 99th percentile latencies rather than just the average. This last point is worth dwelling on because it's a common interview topic.

---

## Why Averages Lie and Percentiles Tell the Truth

Suppose your API Gateway is processing 1,000 requests per second. 990 of them complete in 1ms. But 10 of them — perhaps orders that hit an overloaded shard — take 500ms. The average latency is roughly 6ms. That sounds acceptable. But the 99th percentile latency is 500ms, meaning 1 in 100 traders is experiencing half-second delays. For a trading platform, that's a serious problem that the average completely hides.

This is why production systems universally report latency as a distribution of percentiles — p50 (the median experience), p95 (the experience of the top 5% slowest requests), and p99 (the experience of the top 1% slowest). Some high-frequency trading platforms even track p99.9 and p99.99 because even one-in-ten-thousand slow requests represents real traders being disadvantaged.

A useful mental model: averages describe the typical experience, but percentiles describe the worst experience for a specific fraction of your users. If you promise sub-millisecond execution and your p99 latency is 50ms, you're breaking that promise for 1% of all orders — tens of thousands of orders per day. That's not a rounding error; that's a service level violation.

---

## Distributed Tracing: Following a Single Order Through 12 Services

Let's make distributed tracing concrete with an example. An order enters the system and something about it is slow — the trader reports it took 80ms instead of the usual 2ms. Without tracing, you have no idea where those 80ms went. With tracing, the picture is immediate:

```
Trace ID: trade-abc-789   Total: 80.4ms
│
├── API Gateway: auth check              0.3ms  ✓
├── API Gateway: rate limit check        0.1ms  ✓
├── API Gateway: route to shard          0.1ms  ✓
├── Risk Engine: position check          0.2ms  ✓
├── Risk Engine: funds check             0.2ms  ✓
├── Matching Engine: WAL write           0.4ms  ✓
├── Matching Engine: order book insert   0.001ms ✓
├── Kafka: publish event                76.2ms  ← HERE
├── Notification: push to client         2.8ms  ✓
└── Total                               80.4ms
```

Kafka publish took 76 of the 80 milliseconds. That's immediately actionable — you investigate the Kafka broker, perhaps find that its log disk is nearly full and write performance has degraded, and fix it. Without the trace, you'd be guessing. Each component in the system adds its own span to the trace by passing a **trace context** header forward through every network call — a small ID that says "this request is part of trace abc-789, and I'm span number 7." This propagation is the key engineering decision: every service must be written to accept the incoming trace context and pass it along to every outbound call, otherwise the trace breaks at that boundary.

---

## Alerting: The Art of Waking Up the Right Person

Collecting metrics and traces is only useful if someone acts on them. The alerting system is what converts a metric crossing a threshold into a page to an on-call engineer's phone at 3 AM. Getting alerting right is genuinely hard, and the failure modes are instructive.

**Too many alerts** is as dangerous as too few. If engineers receive 200 alert pages on a normal day, they become desensitized — they start ignoring pages, assuming they're noise, and miss the one that actually matters. This is called **alert fatigue**, and it's cited as a contributing factor in several major production incidents at large tech companies. The discipline is to alert only on *symptoms* that directly affect users — high latency, elevated error rates, consumer lag crossing a dangerous threshold — rather than on every internal metric that looks slightly off.

**Alerts must be actionable**. Every alert that fires should have a clear answer to "what do I do right now?" An alert that says "CPU usage is 78%" with no context about what's normal or what to check is nearly useless at 3 AM. A good alert says "Kafka consumer lag on risk-engine-positions topic has exceeded 5,000 events for 2 minutes — check the risk engine pod health and Kafka broker disk usage." The runbook — a documented set of steps for investigating and resolving a specific alert — should be linked directly in the alert message.

For a trading platform, the most critical alerts, in rough priority order, are consumer lag on safety-critical topics (risk engine, balance service), WAL write latency (indicates disk trouble on a matching engine shard), matching engine throughput drop (could mean a shard has failed or become overloaded), and API Gateway 5xx error rate (indicates systemic failures reaching end users).

---

## The Heartbeat Pattern: Detecting Silence

There's a category of failure that metrics and logs struggle to detect: **the failure of silence**. If a feed handler crashes and stops producing market data updates, no error is thrown — the absence of events is itself the problem. Metrics that measure event *rates* will drop to zero, which should trigger an alert. But what if the metric collection system itself is affected?

The industry solution is the **heartbeat pattern**. Every critical component in the system publishes a small "I am alive" message to a designated topic every few seconds — a heartbeat. A separate watchdog service monitors these heartbeats and alerts if any component misses more than two or three consecutive beats. This is completely independent of the normal metric pipeline, so even if the metric collection system has a problem, the heartbeat monitoring will still catch a dead component.

Think of it like a hiking buddy system. You agree that if you haven't heard from your partner in 30 minutes, something is wrong and you raise the alarm. The absence of communication is itself the signal, not a specific error message.

---

## Bringing It All Together: The Observability Stack

In practice, a trading platform's observability infrastructure looks like a separate parallel system sitting alongside the main platform, consuming a small slice of every component's output.

Metrics flow from every service into a time-series database like **Prometheus**, which stores the numerical values over time. A dashboard tool like **Grafana** reads from Prometheus and renders the real-time charts that operators watch during trading hours. Alert rules in Prometheus fire when thresholds are crossed, routing pages to on-call engineers via **PagerDuty**.

Logs flow from every service into a log aggregation pipeline like the **ELK stack** (Elasticsearch, Logstash, Kibana), where engineers can search across millions of log lines using structured queries — "show me all REJECTED events for trader ID alice in the last 5 minutes."

Traces are collected by an **OpenTelemetry** instrumentation layer embedded in every service, sampled at a configurable rate, and stored in a distributed trace store like **Jaeger** or **Zipkin**. When an engineer investigates a latency spike, they pull the trace for the affected time window and immediately see which span was responsible.

```
Every service emits:
├── Metrics   → Prometheus → Grafana dashboards
│                         → Alertmanager → PagerDuty → on-call engineer
├── Logs      → Logstash  → Elasticsearch → Kibana search
└── Traces    → OpenTelemetry → Jaeger → latency investigation
```

The crucial design principle is that the observability pipeline must be **completely independent** of the trading pipeline. If Prometheus is overloaded, the matching engine keeps running. If Kafka (the trading one) has a problem, the heartbeat monitoring (which uses its own lightweight pub/sub) still works. You never want a situation where the tool designed to detect problems is itself taken down by the problem it was meant to detect.

---

## Recap of Chapter 9

Observability is what transforms a distributed system from a black box into a transparent machine. Metrics give you the high-level vital signs — throughput, latency percentiles, consumer lag — and drive your alerting thresholds. Logs give you the forensic detail to reconstruct exactly what happened after an incident. Traces give you the causal chain from a single request through every service it touched, making latency investigations precise rather than speculative. Alerting discipline is as important as the technical stack — too many alerts creates the same blindness as too few. And the heartbeat pattern closes the gap that all other monitoring misses: detecting failures of silence, when a component simply stops producing output without throwing an error.

The mental model I find most useful here is that of a hospital's monitoring equipment. A patient can have a hundred sensors attached — heart rate, blood pressure, oxygen saturation, temperature — and most of the time they all show normal values and the nurses barely glance at them. But the moment one reading crosses a threshold, an alarm fires immediately and the right person responds. The sensors don't keep the patient alive; the doctors and nurses do. But without the sensors, the doctors are working blind. Observability is exactly this for your trading platform.

---

**Before Chapter 10, here's the question to sit with:** you now have a system that is fast, reliable, scalable, consistent, safe, and observable. But we've been assuming the entire time that the platform serves traders in one region — say the United States. What happens when you need to serve traders in Europe and Asia simultaneously? A trader in Tokyo connecting to a server in New York experiences roughly 150 milliseconds of network latency just from the speed of light across the Pacific. For a trading platform promising sub-millisecond execution, that is catastrophically slow. And if you deploy separate matching engines in each region, how do you handle a trader in Tokyo buying a stock from a seller in London — a cross-region trade that requires coordination across two geographically distant matching engines?

This is the **global deployment and geo-distribution problem**, and it forces us to confront a fundamental theorem of distributed systems — the CAP theorem — in a very concrete and painful way. Ready for Chapter 10?

---


# Chapter 10: Going Global — Geo-Distribution and the CAP Theorem

## The Problem: Physics is the Enemy

Every chapter so far has been about defeating software limitations — slow databases, single points of failure, hot shards, partial failures. This chapter is different. This chapter is about fighting **physics itself**, and physics doesn't negotiate.

The speed of light through a fibre optic cable is approximately 200,000 kilometres per second. The distance from Tokyo to New York is roughly 11,000 kilometres. That means a signal travelling one way takes at minimum 55 milliseconds — and in practice, with routing hops and network overhead, it's closer to 150ms round trip. You cannot optimize this away. You cannot throw more servers at it. The electrons simply cannot travel faster.

For a retail banking app, 150ms is perfectly fine. For a trading platform that promises sub-millisecond order execution, it is an **absolute dealbreaker**. A trader in Tokyo submitting an order to a server in New York is at a 150ms structural disadvantage compared to a trader sitting next to that server in Manhattan. In high-frequency trading, where strategies are built on microsecond edges, this is the difference between profit and loss on every single trade.

So the engineers faced a seemingly simple question with a devastatingly complex answer: **can we just deploy matching engines in every region?** Put one in New York, one in London, one in Tokyo. Each region's traders connect to their local engine. Orders match locally with sub-millisecond latency. Problem solved.

Except it immediately creates a new problem that is arguably worse than the original one.

---

## The Split-Brain Problem Returns, But Globally

Imagine you deploy matching engines in New York and Tokyo, both handling AAPL. A seller in Tokyo lists 500 shares at $150.00. A buyer in New York wants to buy 500 shares at $150.00. These two orders should match — they're a perfect pair. But the New York engine doesn't know about the Tokyo seller, and the Tokyo engine doesn't know about the New York buyer. Both engines have incomplete views of the global order book. The match never happens, even though it should.

Worse, suppose both engines *do* know about each other and try to coordinate. A sell order sits in Tokyo's book. Two buyers — one in New York, one in London — both submit market orders simultaneously. Without coordination, both engines might match against the same Tokyo sell order, resulting in the same 500 shares being sold twice. This is a **duplicate execution**, and in financial systems it means one party receives shares they didn't pay for, and another party never receives shares they did pay for.

This tension — between the need for local performance and the need for global consistency — is not just a trading platform problem. It is a fundamental theorem of distributed systems. And understanding it deeply is what separates senior engineers from junior ones in system design interviews.

---

## The CAP Theorem: You Cannot Have Everything

In 2000, computer scientist Eric Brewer proposed what became known as the **CAP theorem**. It states that a distributed system can guarantee at most two of the following three properties simultaneously. You can never have all three.

**Consistency** means every read sees the most recent write. If a sell order is placed in Tokyo, any query from any node in the world immediately sees that order. There is one global truth and everyone agrees on it.

**Availability** means every request receives a response. The system never says "I can't answer right now." Even if some nodes are having problems, the system keeps serving requests.

**Partition Tolerance** means the system continues operating even when the network between nodes fails — a **network partition**. When New York and Tokyo can't communicate with each other due to a cable failure, each side keeps running rather than grinding to a halt.

The critical insight is that network partitions are not optional. In a globally distributed system, the network *will* partition eventually — a cable gets cut, a router fails, a data centre loses connectivity. So partition tolerance is not a choice you make; it is a reality you accept. This means the real tradeoff is between **Consistency and Availability** during a partition. You can have one or the other, but not both.

A **CP system** (Consistent + Partition Tolerant) chooses consistency. During a partition, if New York loses contact with Tokyo, New York refuses to process any AAPL orders until connectivity is restored — because it cannot guarantee its view of the order book is complete. This is safe but the system becomes unavailable to traders during the partition.

An **AP system** (Available + Partition Tolerant) chooses availability. During a partition, New York keeps accepting and matching orders even though it can't see Tokyo's orders. Traders get responses immediately, but the two sides may diverge — the same share might be sold twice, creating an inconsistency that must be reconciled later.

For a trading platform, this is a genuinely agonizing choice. Inconsistency means potential duplicate trades — a financial and regulatory nightmare. But unavailability means traders literally cannot place orders — also a financial and regulatory nightmare. There is no clean answer, and the industry's response to this dilemma is more nuanced than simply picking one side.

---

## The Industry's Actual Answer: Segment by Listing

The insight that resolves this dilemma is a subtle but important one: **you don't need every matching engine to handle every stock globally**. Instead, each stock has exactly one **home exchange** — one authoritative matching engine — and that engine's location is a deliberate business and technical choice.

Apple is primarily listed on NASDAQ, so AAPL's matching engine lives in New York. Toyota is primarily listed on the Tokyo Stock Exchange, so 7203.T's matching engine lives in Tokyo. Volkswagen trades on the Frankfurt Stock Exchange, so its engine lives in Frankfurt. Traders anywhere in the world can submit orders for any stock, but those orders are always routed to the authoritative matching engine for that stock, regardless of where the trader is located.

This is called the **single-master model per instrument**, and it is exactly how real global exchanges operate today. NYSE, NASDAQ, the London Stock Exchange, and the Tokyo Stock Exchange each handle their own listed instruments. Cross-listings (where a stock trades on multiple exchanges simultaneously) are handled through separate mechanisms called ADRs (American Depositary Receipts) that create distinct tradeable instruments rather than sharing a single order book.

```
  Trader in Tokyo wants to buy AAPL:
  
  Tokyo Trader
       │
       │ order routed to AAPL's home exchange
       ▼
  New York Matching Engine  ← authoritative for AAPL
       │
       │ fill confirmation (150ms round trip)
       ▼
  Tokyo Trader sees: "Filled @ $150.05"
  
  (150ms latency accepted — this is the price of global consistency)
```

This is a pragmatic acceptance of the CAP theorem. You choose consistency — one authoritative engine, one global truth — and you accept that traders geographically distant from that engine will experience higher latency. The alternative, splitting the order book for a single stock across multiple regions, introduces consistency problems that are worse than the latency.

---

## What You *Can* Distribute Regionally: Everything Except Matching

Just because the matching engine for AAPL must live in New York doesn't mean a Tokyo trader's entire experience has to cross the Pacific. You can deploy regional infrastructure that handles everything *except* the actual matching, dramatically reducing perceived latency for most operations.

The API Gateway and authentication layer can be deployed regionally. A Tokyo trader's order submission hits a Tokyo gateway within a few milliseconds, is authenticated locally, passes the risk engine checks locally (the risk engine's in-memory state is replicated regionally), and *then* is forwarded to New York for matching. The trader waits the 150ms only for the matching step — the rest of the lifecycle is local.

Market data distribution is entirely regional. Remember the fan-out pipeline from Chapter 8? You run regional distribution servers in Tokyo, London, and Frankfurt that receive the data from New York via a dedicated low-latency network backbone and serve it to local clients. A Tokyo trader watching AAPL prices sees updates with only a few milliseconds of latency — the data is coming from a Tokyo distribution server, not New York. Only the actual order execution crosses the Pacific.

User account management, portfolio views, trade history, and all the read-side queries from Chapter 5 can be served from regional read replicas. A Tokyo trader opening their portfolio page doesn't wait for a transatlantic round trip — they're reading from a database replica co-located with them.

```
TOKYO REGION                     NEW YORK REGION
─────────────────                ─────────────────────
API Gateway (local auth)    ──→  Matching Engine (AAPL)
Risk Engine (local copy)    ←──  WAL + Event stream
Read replicas (portfolio)
Market data servers
  ↓ receive from NY
  ↓ serve locally
  Tokyo traders
```

This architecture minimizes the latency impact to the one operation that genuinely requires global consistency — the match itself — while keeping everything else fast and local.

---

## The Low-Latency Network Backbone: Not All Internet is Equal

There is one more piece of this puzzle that is worth knowing for interviews because it reveals how serious the industry is about squeezing out every possible millisecond. Major exchanges and trading platforms do not route their inter-region traffic over the public internet. They lease or build **dedicated fibre optic networks** between their data centres, specifically optimized for low latency.

The public internet routes traffic based on cost and policy, not speed. A packet from Tokyo to New York might bounce through 12 different routers in Seoul, Osaka, Los Angeles, and Chicago before arriving. Each hop adds latency. A dedicated private line takes the most direct physical path and skips all those intermediate routers.

Some firms go even further. In 2010, a company called Spread Networks spent $300 million drilling a perfectly straight tunnel through the Appalachian Mountains to lay a fibre cable between Chicago and New York — shaving 3 milliseconds off the round-trip time compared to existing cables that followed road networks. Three milliseconds. Three hundredths of the time it takes to blink. And it was worth $300 million to high-frequency trading firms because that 3ms advantage on every single trade added up to enormous profits over millions of trades per day.

More recently, microwave and millimetre-wave radio transmission has replaced fibre for some routes because radio waves travel faster through air than light through glass. The speed-of-light limitation is the same, but microwave signals don't have to follow the curved path of a cable — they go in a perfectly straight line, which is physically shorter. There are now networks of microwave towers between Chicago and New York, and between London and Frankfurt, operated specifically for low-latency trading.

You don't need to know these specific details for a system design interview, but the underlying principle is important: **the physical network layer is a first-class engineering concern in a trading platform**, not something you assume away. When you discuss geo-distribution in an interview, mentioning that your inter-region links should be dedicated low-latency connections rather than public internet demonstrates a level of depth that stands out.

---

## Data Residency and Regulatory Complexity

There is a dimension of global deployment that is entirely non-technical but frequently comes up in interviews with candidates who have real-world experience: **regulatory requirements around data residency**.

Many jurisdictions have laws requiring that financial data about their citizens or companies must be stored on servers physically located within their borders. The European Union's GDPR, for instance, imposes strict rules about transferring personal data outside the EU. India's data localisation rules require certain financial data to remain in India. China's regulations are even more restrictive.

This means your elegant global architecture — where read replicas in Tokyo are fed by a primary database in New York — might be illegal for European users. You may be legally required to maintain a complete, independent database of EU users' data within the EU, synchronized with the global system in a way that satisfies both technical consistency requirements and legal data residency requirements simultaneously.

This doesn't change the fundamental architecture we've discussed, but it adds constraints on *which data* can flow between regions and in what direction. In interviews, acknowledging this kind of real-world constraint — even briefly — signals that you understand that system design exists in a legal and business context, not just a technical vacuum. It's the kind of observation that distinguishes someone who has thought deeply about production systems from someone who has only considered the happy path.

---

## Recap of Chapter 10

The story of global deployment is ultimately a story about accepting constraints rather than eliminating them. Physics imposes a floor on cross-region latency that no amount of engineering can remove. The CAP theorem imposes a choice between consistency and availability during network partitions that no architecture can escape. The industry's answer to both is the same: embrace the constraint, then minimize its impact. Assign each instrument to a single authoritative matching engine — accepting that distant traders will experience the round-trip latency — while deploying everything else regionally so that only the one operation requiring global consistency actually crosses the ocean. Dedicated low-latency network backbones shrink that round trip as much as physics allows. And regulatory data residency requirements add another layer of constraints that must be designed around, not ignored.

The most important mental model here is that of a supreme court system. There is one authoritative ruling for each case — you cannot have two courts issue contradictory verdicts on the same case. But you can have regional courts handle local matters, only escalating to the supreme court when a definitive ruling is needed. The supreme court is the bottleneck, but you minimize what goes to it. That is exactly what the single-master-per-instrument model does for a trading platform.

---

We've now covered ten chapters of deep, interconnected concepts. Before we move to the final chapter, it's worth pausing to look at the **complete picture** of what we've built together, because in an interview, being able to narrate the entire system coherently — from a trader clicking "buy" all the way to a globally distributed, observable, fault-tolerant platform — is the ultimate test. 

**Chapter 11 will be our synthesis chapter** — putting every piece together into a clean interview narrative, identifying the key talking points for each component, the tradeoffs you should proactively mention, and the follow-up questions interviewers typically ask so you're never caught off guard. Think of it as your interview preparation guide for everything we've covered. Ready?

---

# Chapter 11: The Complete Picture — Your Interview Narrative

## How Interviewers Actually Evaluate System Design Answers

Before we synthesize everything, let's talk about what the interviewer is actually measuring, because this shapes how you should structure your answer. They are not checking whether you can recite a list of components. Any candidate can say "I'd use Kafka and Redis and Cassandra." What they're evaluating is whether you can **reason about tradeoffs** — whether you understand *why* each component exists, *what problem* it solves, and *what you give up* by choosing it. The candidate who says "I'd use Cassandra for the write store because its LSM-tree architecture makes sequential writes fast at the cost of slower reads, which is acceptable here because reads are handled by a separate read model" is demonstrating entirely different depth than the candidate who says "I'd use Cassandra because it's fast."

The second thing they're evaluating is whether your design **tells a coherent story**. A trading platform has dozens of components, and it's easy to dump them all into a diagram and draw arrows between them. What's hard — and what impresses interviewers — is narrating the system as a sequence of problems and solutions, where each solution naturally motivates the next problem. That is exactly the structure we've built across these ten chapters, and it's the structure you should use in an interview.

A typical HLD interview gives you 45 minutes. A good rule of thumb is to spend the first 5 minutes clarifying requirements, the next 25 minutes designing the core system, and the last 15 minutes doing a deep dive on whichever component the interviewer finds most interesting. Let's structure our synthesis around this.

---

## Phase 1: Clarifying Requirements (5 minutes)

Never skip this phase, and never treat it as a formality. The questions you ask at the start signal your engineering maturity more than almost anything else. For a trading platform, the questions that reveal the most important design constraints are these.

Ask about **scale**. "How many active traders are we expecting? How many orders per second at peak? How many symbols are we supporting?" The answers determine whether you need sharding, how aggressive the caching needs to be, and what kind of database throughput you're designing for.

Ask about **latency requirements**. "Are we building for retail traders, institutional traders, or both? Is sub-millisecond execution a hard requirement or a nice-to-have?" This determines whether you need the in-memory matching engine or whether a simpler database-backed approach would suffice, and it determines whether you need co-location services.

Ask about **consistency requirements**. "What's the acceptable behaviour during a partial failure — is it better to reject an order we're uncertain about, or to accept it and reconcile later?" This question tells the interviewer you understand the CAP theorem before you've even drawn a single box.

Ask about **geography**. "Are we serving traders in one region or globally?" If the answer is globally, you immediately introduce the geo-distribution problem and the single-master-per-instrument model.

With these answers established, you can say: "Given these requirements, I'll design for a system that handles 500,000 orders per second across 10,000 symbols, with sub-millisecond matching latency, global trader access, and strict financial consistency. Let me walk you through the design from the core outward."

---

## Phase 2: The Core Design Narrative (25 minutes)

The narrative arc you should follow mirrors exactly the chapters we've covered. Start at the heart of the system and work outward, introducing each component only when the previous one has revealed a problem that motivates it.

**Start with the order book and matching engine.** Explain the two sorted lists — bids and asks — and the price-time priority matching rule. Explain why this must live in memory: a database-backed matching engine doing 500,000 writes per second with disk I/O on the critical path is fundamentally incompatible with sub-millisecond latency. The matching engine is a sorted in-memory data structure — a red-black tree or skip list keyed by price — and a trade fires the instant the best bid meets or crosses the best ask.

Immediately follow this with the crash recovery question. "But if the order book is in memory, what happens on a crash?" This is where you introduce the WAL — sequential append-only writes that record every event before processing it. And this naturally leads you to event sourcing: the WAL is not just a crash recovery mechanism, it's the source of truth, and the in-memory order book is simply a projection derived by replaying it.

**Then introduce replication** to solve the single point of failure. Describe the active-passive model — one leader, one or more followers receiving a streamed copy of every event — and explain the sequencer variant for active-active setups. Mention semi-synchronous replication as the production standard that balances data safety against latency.

**Then introduce sharding** to solve the single-machine throughput ceiling. Shard by stock symbol — the natural boundary because trades are never cross-symbol. Walk through why consistent hashing with virtual nodes is better than static mapping or simple modulo hashing. Mention fault isolation as a benefit beyond just throughput: Shard 2 going down doesn't affect trading on Shard 1.

At this point, pause and draw the audience a mental picture. You now have a system that looks like a grid: rows are shards, columns are replicas. Each cell is an independent matching engine process. There are no cross-cell dependencies for order matching.

**Then work outward to the API Gateway.** Explain what it protects the matching engine from: authentication overhead, rate limiting, protocol translation, and routing logic. Describe the hybrid protocol design — REST for order submission, WebSocket for real-time push notifications. Introduce Snowflake IDs as the solution to generating globally unique, time-sortable order IDs without a central coordinator.

**Then narrate the full order lifecycle** end to end. A trade entering at the gateway, being authenticated and rate-checked, receiving a Snowflake ID, being routed to the correct shard, hitting the WAL, matching in memory, publishing to Kafka, being consumed by the persistence service and notification service, and arriving as a fill confirmation back on the trader's WebSocket. If you can walk this through fluently, naming every component and the timing at each step, you will visibly impress the interviewer.

**Then introduce the database layer and CQRS.** The write path — Cassandra with its LSM-tree writes — handles the firehose of trade events. The read path — PostgreSQL with pre-computed user portfolio views, and Redis for sub-millisecond caching — handles user-facing queries. Kafka connects them asynchronously, decoupling write throughput from read complexity. Explain why this separation exists: writes and reads want opposite things from a database, and trying to serve both from the same store means neither works well.

**Then tackle distributed transactions.** Explain why 2PC is unsuitable — blocking during the in-doubt window and the latency cost of two round trips on every trade. Introduce the SAGA pattern as the alternative: independent local transactions with defined compensating actions rather than one big atomic lock. Add idempotent consumers with Redis deduplication to handle the at-least-once delivery that Kafka guarantees. And tie the WAL to Kafka together using the outbox pattern to prevent the gap between a successful WAL write and a failed Kafka publish.

**Then introduce the risk engine** as the layer that protects against logical failures rather than technical ones. Pre-trade checks — position limits, funds sufficiency, order rate limits, fat-finger price validation — all run in microseconds because all state is in memory, kept current by the same Kafka event stream. Circuit breakers provide the system-level safety valve for emergent anomalies. The balance reservation model prevents double-spending between concurrent orders without requiring distributed locks.

**Then describe market data distribution** as a completely separate read-scaling concern. The staged fan-out pipeline: Kafka to feed handlers to distribution servers to clients. Topic-based selective delivery so distribution servers only receive updates for their subscribers' symbols. Snapshot-plus-delta delivery to keep bandwidth efficient. Conflation for slow consumers.

**Then cover observability** — the three pillars of metrics, logs, and traces, and why you need all three. Mention that consumer lag on safety-critical Kafka topics is the single most important metric for a trading platform. Explain why percentile latency (p99, p99.9) matters more than average latency. Describe the heartbeat pattern for detecting failures of silence.

**Finally, address geo-distribution.** The CAP theorem, the impossibility of having consistency and availability during a partition, and the industry's pragmatic resolution: single-master per instrument, with everything except the actual matching deployed regionally. Accept the latency for the one operation that requires global consistency; minimize everything else.

---

## The Tradeoffs You Should Proactively Mention

Interviewers specifically look for whether you volunteer tradeoffs without being asked, because it shows you understand that every design decision has a cost. Here are the most important ones to mention naturally as you go through the design.

When you talk about in-memory matching, mention that you're trading durability for speed, and that the WAL is what makes this trade acceptable. When you talk about sharding, mention that cross-shard queries require scatter-gather and that this is why you separate the read model. When you talk about CQRS, mention that the read models are eventually consistent — there is a brief window after a trade where the portfolio view might not reflect it, and you handle this with the read-your-own-writes pattern. When you talk about asynchronous replication, mention the replication lag and the possibility of data loss in the gap before a follower catches up, and why you use semi-synchronous replication to close that window. When you talk about the SAGA pattern, mention intermediate inconsistent states and that other parts of the system must be designed to tolerate seeing in-flight transactions.

Each of these mentions takes only one or two sentences, but they demonstrate that you're thinking about the system as it would actually behave in production, not as it looks on a whiteboard.

---

## The Follow-Up Questions Interviewers Typically Ask

After the core design, interviewers probe specific areas. Knowing what's coming lets you think about these in advance rather than being caught cold.

"How would you handle a scenario where the matching engine crashes mid-trade — after the WAL write but before the Kafka publish?" This is asking about the outbox pattern. Your answer is that the outbox relay reads unpublished WAL entries on restart and republishes them, and idempotent consumers handle any duplicates safely.

"How would you scale the system if trading volume suddenly 10x'd due to a market event?" This is asking about your scaling strategy. Your answer covers adding shards (consistent hashing makes this minimally disruptive), scaling distribution servers horizontally (they're stateless), and scaling Kafka by adding partitions. The matching engine itself can be vertically scaled first — more CPU, more RAM — before resorting to further sharding.

"How do you ensure fairness — that orders with the same price are processed in arrival order?" This is asking about price-time priority and the sequencer pattern. The WAL timestamp establishes arrival order. If you have a multi-gateway setup, the sequencer assigns global sequence numbers before orders reach the matching engine.

"What happens if the risk engine's in-memory state is wrong?" This is asking about the lag and reservation model. Your answer: the risk engine errs conservatively using balance reservations, and the lag is bounded to milliseconds by the Kafka consumer catching up. For safety-critical lag breaches, a circuit breaker halts trading until the state is verified.

"How would you design the system for a new type of instrument — say, options or futures?" This is a scope-expansion question to see if your design is flexible. Options and futures have more complex matching rules (American vs European options, expiry dates, strike prices), but the fundamental architecture — in-memory order book, WAL, event sourcing, Kafka pipeline — is the same. The matching logic is more complex, but the surrounding infrastructure is unchanged. This is actually a strength of the event sourcing model: the event log records what happened regardless of instrument complexity.

---

## A One-Paragraph Summary You Can Say Out Loud

If an interviewer ever asks you to summarize the entire system in a minute, this is roughly what you want to be able to say naturally:

"The core of the system is a sharded in-memory matching engine where each shard owns a set of stock symbols and runs independently. Orders flow in through a stateless API Gateway that handles authentication, rate limiting, and routing. Before reaching the matching engine, every order passes through an in-memory risk engine that validates position limits, funds availability, and price sanity. The matching engine writes each event to a Write-Ahead Log before processing it, making the in-memory order book fully recoverable after a crash. Matched trades are published to Kafka, which fans out to a Cassandra write store for raw event persistence, PostgreSQL read models for user-facing queries, and a regional market data distribution pipeline for real-time price feeds. All services maintain idempotency using deduplication keys to make retries safe, and multi-step operations use the SAGA pattern with compensating transactions instead of distributed locks. Each shard is replicated using semi-synchronous active-passive replication. For global traders, every stock has a single authoritative matching engine in its home region, while authentication, risk checking, and market data are deployed regionally to minimize cross-ocean round trips for everything except the actual match. The whole system is observed through a parallel metrics, logging, and tracing pipeline with consumer lag on Kafka safety-critical topics as the primary health signal."

If you can say something like that fluently, pointing at a diagram you've drawn on the whiteboard as you go, you will pass any trading platform HLD interview at any tier of any company.

---

## The Bigger Pattern Worth Internalising

Now that you've seen the entire design, step back and notice the architectural pattern that appears over and over again across every chapter. A fast in-memory component handles the critical path. A durable append-only log sits underneath it for recovery. An asynchronous event stream fans out to all secondary systems. Each secondary system is purpose-built for its own job and never asked to do someone else's. And a safety layer — whether it's the risk engine, the circuit breaker, or the idempotency check — sits at every boundary where money could be lost.

This pattern — in-memory speed, log-backed durability, async fan-out, purpose-built consumers, boundary safety checks — is not unique to trading platforms. It appears in payment processing systems, booking platforms, inventory management systems, and anywhere else where correctness and speed are both non-negotiable. Once you can see this pattern in a trading platform, you'll recognise it instantly when a different design problem in an interview has the same underlying shape. That is the real payoff of understanding a system at this depth rather than memorising its components.

---

You've now covered the complete design of a trading platform — from the first principles of order matching all the way to global deployment, financial safety, and operational observability. Every concept was introduced when a previous design decision created a problem that demanded it. That's not just a good way to learn; it's the right way to narrate any system design in an interview.

Congratulations on making it through all ten chapters. Go build something great — or at least ace the interview. You've earned it.

---

# High-Level Design: Trading Platform
> Complete Interview Reference — covers requirements, estimations, component design, feature flows, failure handling, and tradeoffs.

---

## Table of Contents

1. Problem Statement
2. Functional Requirements
3. Non-Functional Requirements
4. Assumptions
5. Capacity Estimation
6. High-Level Architecture
7. Core Component Design
8. Detailed Feature Flows
9. Data Models
10. Database Design
11. API Design
12. Caching Strategy
13. Message Queue Design
14. Failure Scenarios & Mitigations
15. Key Design Tradeoffs
16. Interview Cheat Sheet

---

## 1. Problem Statement

Design a globally distributed stock trading platform that allows retail and institutional traders to place, manage, and execute buy/sell orders for financial instruments in real time. The platform must support sub-millisecond order matching, guarantee financial consistency with zero tolerance for lost trades or double executions, distribute real-time market data to millions of passive observers, and remain available 24/7 across multiple geographic regions.

---

## 2. Functional Requirements

### 2.1 Order Management
- Traders can place **Limit Orders**: execute only at a specified price or better. These sit in the order book until matched or cancelled.
- Traders can place **Market Orders**: execute immediately at the best available price. Guaranteed execution, no price guarantee.
- Traders can **cancel** any open (unmatched) order.
- Traders can **amend** an open order's quantity. Price amendments create a new order to preserve time priority fairness.
- Every order gets a globally unique, time-sortable **Order ID** assigned at submission.

### 2.2 Order Matching
- Orders are matched using **Price-Time Priority (FIFO)**: best price wins; tie-broken by earliest submission time.
- A trade executes the instant the best bid price meets or crosses the best ask price.
- **Partial fills** are supported — a 1000-share buy can match across multiple smaller sell orders.
- The result of a match is a **Trade** — an immutable record of price, quantity, buyer, and seller.

### 2.3 Market Data
- The platform publishes a real-time **order book** (Level 2 data) showing all bids and asks at every price level.
- The platform publishes a **ticker feed** of last-traded prices, volume, and OHLC bars.
- Clients can subscribe to data at different granularities: full depth, top-of-book only, or periodic snapshots.

### 2.4 Portfolio & Account Management
- Traders can view their **current positions** (shares held per symbol).
- Traders can view their **available cash balance** and **reserved balance** (funds locked for pending orders).
- Traders can view their **trade history** and **order history** with filtering and pagination.
- The system computes and displays **unrealized P&L** and **realized P&L** per position.

### 2.5 Risk Controls
- Pre-trade validation: reject orders that exceed position limits, insufficient funds, or violate price sanity bounds.
- Circuit breakers: auto-halt trading in a symbol if price moves beyond a configured threshold in a short window.
- Account-level halt: automatically suspend accounts that show runaway algorithmic behaviour.

### 2.6 Settlement & Clearing
- After a trade executes, the system updates buyer and seller balances and positions.
- The platform integrates with a clearing house to settle trades (transfer of actual shares and cash) within T+2 (two business days) per regulatory standard.

### 2.7 User & Auth Management
- Traders can register, authenticate, and manage API keys.
- Role-based access: retail traders, institutional traders, market makers, and platform admins have different permissions and rate limits.

---

## 3. Non-Functional Requirements

### 3.1 Performance
- Order-to-acknowledgement latency: **< 10ms** end-to-end (gateway to matching engine to response).
- Matching engine internal latency: **< 1ms** from order receipt to match decision.
- Market data propagation latency: **< 50ms** from trade execution to client receiving the update.
- System throughput: **500,000 orders/second** at peak across all symbols.

### 3.2 Availability
- Platform uptime: **99.99%** (less than 52 minutes downtime per year).
- No single point of failure anywhere in the critical trading path.
- Regional failures must not affect trading globally — fault isolation by shard and by region.

### 3.3 Consistency
- Financial data (balances, positions, trade records) must be **strongly consistent** — no money lost, no double executions, ever.
- Order book state must be **eventually consistent** across replicas within milliseconds.
- Read-your-own-writes consistency for traders viewing their own order and trade history.

### 3.4 Durability
- No trade event may be lost even in the event of a hardware failure.
- The system must be recoverable to a consistent state within **60 seconds** of a matching engine crash.
- Write-Ahead Logging guarantees zero data loss for events that have been acknowledged.

### 3.5 Scalability
- The system must scale horizontally by adding shards without downtime.
- Read traffic (market data, portfolio views) must scale independently of write traffic (order matching).
- The system must support **10,000 tradeable symbols** simultaneously.

### 3.6 Security
- All external communication encrypted via TLS 1.3.
- API key authentication with per-key rate limits.
- All financial operations produce an immutable, auditable event log.
- Protection against replay attacks, order spoofing, and wash trading.

### 3.7 Regulatory Compliance
- Every trade must be reportable to regulators with full audit trail.
- Market manipulation patterns (wash trading, layering, spoofing) must be detected and flagged.
- Data residency rules must be respected — EU user data must remain in EU region.

---

## 4. Assumptions

- The platform lists equities (stocks) primarily. Options and futures can be added later without changing core architecture, only matching logic.
- Trading hours are defined per exchange (e.g. NYSE: 9:30 AM – 4:00 PM ET). The system handles pre-market and after-hours trading as separate sessions with different liquidity.
- Each stock has exactly one **home exchange** and one authoritative matching engine shard. Cross-listed stocks (e.g. a stock trading on both NYSE and LSE) are treated as distinct instruments.
- The platform does not operate as a broker itself — it provides the exchange infrastructure. Regulatory brokerage requirements are handled by integrated broker-dealer partners.
- Settlement and clearing is outsourced to a clearing house (like DTCC). The platform's responsibility ends at publishing confirmed trade events.
- The system assumes a reliable network backbone between data centres (dedicated leased lines, not public internet).
- All monetary values are stored in the smallest currency unit (cents for USD) as integers to avoid floating-point precision errors entirely.

---

## 5. Capacity Estimation

### 5.1 Scale Parameters
- **Active traders**: 5 million registered, 500,000 concurrently active during peak hours.
- **Symbols**: 10,000 tradeable instruments.
- **Peak order rate**: 500,000 orders/second across all symbols.
- **Average order rate per symbol**: 50 orders/second (some liquid stocks like AAPL see 5,000/second; many small-caps see < 1/second).
- **Market data subscribers**: 2 million concurrent connections (retail + institutional).

### 5.2 Storage Estimation
- **Trade record size**: ~200 bytes per trade (order IDs, price, quantity, timestamp, buyer, seller).
- **Trades per day**: ~50 million (assuming 30% of orders result in immediate fills).
- **Daily trade storage**: 50M × 200B = **10 GB/day**.
- **Order records (including cancelled)**: 500,000 orders/sec × 6.5 hours × 200B = **~2.3 TB/day** (raw, pre-compression). With 5:1 compression: **~460 GB/day**.
- **Market data events**: 5 million events/second × 50 bytes = **250 MB/second** = **6.5 TB/day**.
- **5-year retention**: roughly **10–15 PB** total across all data types.

### 5.3 Memory Estimation (Per Matching Engine Shard)
- **Order book per symbol**: 1,000 price levels × 2 sides × 100 bytes per level = ~200 KB per symbol.
- **Shard handles 100 symbols**: 100 × 200 KB = **20 MB** for order books.
- **WAL buffer**: 128 MB in-memory ring buffer before flush.
- **Total per shard**: ~512 MB RAM sufficient; deploy with 8–16 GB for headroom.

### 5.4 Network Estimation
- **Inbound order traffic**: 500,000 orders/sec × 256 bytes = **128 MB/s inbound**.
- **Outbound market data**: 5M events/sec × 50 bytes = **250 MB/s** to fan-out layer.
- **Inter-region replication**: ~50 MB/s per shard pair.
- **Total egress to clients**: ~10 Gbps at peak across all distribution servers.

### 5.5 Sharding Estimate
- 500,000 orders/sec across 10,000 symbols.
- A single matching engine shard can handle ~50,000 orders/sec comfortably.
- Required shards: 500,000 / 50,000 = **10 shards minimum**.
- With 2x headroom for hotspots and growth: **20 shards deployed**, each with 1 leader + 1 follower replica = **40 matching engine processes**.

---

## 6. High-Level Architecture

```
                         ┌─────────────────────────────────────────┐
                         │              EXTERNAL CLIENTS            │
                         │   Retail Web  |  Mobile  |  Algo Traders │
                         └──────────────────┬──────────────────────┘
                                            │ HTTPS / WebSocket / FIX
                         ┌──────────────────▼──────────────────────┐
                         │           LOAD BALANCER (L4/L7)          │
                         └──────────────────┬──────────────────────┘
                                            │
               ┌────────────────────────────┼────────────────────────────┐
               ▼                            ▼                            ▼
         ┌──────────┐                 ┌──────────┐                 ┌──────────┐
         │  API GW1  │                 │  API GW2  │                 │  API GW3  │
         │ (stateless│                 │ (stateless│                 │ (stateless│
         │  auth,    │                 │  routing) │                 │  routing) │
         │  rate lim)│                 └──────────┘                 └──────────┘
         └─────┬─────┘
               │
    ┌──────────▼──────────┐
    │     RISK ENGINE      │  (in-memory, pre-trade checks)
    └──────────┬──────────┘
               │ approved orders only
    ┌──────────▼──────────────────────────────────────────┐
    │               MATCHING ENGINE LAYER                  │
    │  ┌──────────┐  ┌──────────┐  ┌──────────┐           │
    │  │ Shard 1  │  │ Shard 2  │  │ Shard N  │  (sharded │
    │  │AAPL,MSFT │  │TSLA,AMZN │  │ ...      │  by symbol│
    │  │Leader+   │  │Leader+   │  │          │           │
    │  │Follower  │  │Follower  │  │          │           │
    │  └──────────┘  └──────────┘  └──────────┘           │
    └──────────────────────┬──────────────────────────────┘
                           │ trade events
    ┌──────────────────────▼──────────────────────────────┐
    │                  KAFKA (Event Stream)                │
    └──────┬───────────────┬──────────────────────────────┘
           │               │
   ┌───────▼──────┐  ┌─────▼──────────┐  ┌───────────────────┐
   │  Write Store │  │  Read Models   │  │  Market Data Fan  │
   │  (Cassandra) │  │  (PostgreSQL + │  │  Out Pipeline     │
   │  raw events  │  │   Redis cache) │  │  (Feed Handlers + │
   └──────────────┘  └────────────────┘  │  Distribution Svr)│
                                         └───────────────────┘
```

---

## 7. Core Component Design

### 7.1 API Gateway
The API Gateway is the single entry point for all external traffic. It is **stateless** — any gateway instance can handle any request — which means you can scale it horizontally by simply adding more instances.

**Responsibilities:**
- TLS termination and certificate management.
- Authentication via API key or session token (sub-millisecond Redis lookup).
- Rate limiting per trader (token bucket algorithm, state stored in Redis).
- Protocol translation: REST/WebSocket/FIX → internal binary protocol.
- Order routing: looks up which shard owns the requested symbol using the shard map (consistent hashing).
- Assigns a globally unique **Snowflake ID** to every incoming order.
- Maintains a `connection_id → websocket` mapping to route fill notifications back to the correct client.

**Snowflake ID structure (64-bit integer):**
```
[ 41-bit timestamp (ms since epoch) | 10-bit machine ID | 12-bit sequence counter ]
```
This gives 4,096 unique IDs per millisecond per gateway machine, with zero coordination between gateways.

### 7.2 Matching Engine
The matching engine is the core of the platform. It is a single-threaded process (no locks needed) running entirely in RAM.

**Internal data structure — the Order Book:**
- Two sorted data structures: a `bids` map (sorted highest price first) and an `asks` map (sorted lowest price first).
- Each price level contains a **queue** of orders at that price, sorted by arrival time (FIFO).
- Implemented as a **Red-Black Tree** or **Skip List** keyed by price, with O(log N) insert and O(1) best-price lookup.

**Matching algorithm (Price-Time Priority):**
1. New order arrives.
2. If it is a market order, take the best available price on the opposing side.
3. If it is a limit order, check if the best opposing price satisfies it (best ask ≤ limit bid price for buys, best bid ≥ limit ask price for sells).
4. If match found, create a `TRADE_EXECUTED` event. Reduce quantities. Remove fully filled orders from the book.
5. If the incoming order is partially filled or unfilled, insert it into the book at the correct price level, at the back of the queue for that price.
6. Before any of this, append one line to the **WAL**.

**Write-Ahead Log (WAL):**
Every event is written sequentially to a local WAL file before processing. Sequential disk appends are ~100x faster than random writes. On crash restart, the engine loads the last snapshot from object storage and replays the WAL from that point to reconstruct the full order book state.

**Replication:**
Each shard runs in active-passive mode. The leader streams WAL entries to one or more followers in real time. Followers apply the same events in the same order, maintaining identical order book state. On leader failure, a follower is promoted by the shard coordinator (implemented via distributed consensus using Raft or ZooKeeper). Semi-synchronous replication: the leader waits for one follower to acknowledge before confirming a write, ensuring zero data loss on failover.

### 7.3 Risk Engine
The risk engine sits between the API Gateway and the matching engine. It runs **pre-trade checks** entirely in memory and must complete within microseconds.

**Pre-trade checks:**
- **Position limit check**: Does this trade keep the trader within their maximum allowed position for this symbol?
- **Funds sufficiency check**: Does the trader have enough *available* balance (total minus reserved) for this order? On approval, the required funds are immediately **reserved** to prevent double-spending.
- **Order rate check**: Has this trader exceeded their maximum orders-per-second limit?
- **Price sanity check (fat finger)**: Is the submitted price within ±20% of the current market price? Rejects obvious typos.
- **Wash trade check**: Is the trader simultaneously submitting matching buy and sell orders for the same symbol (self-trading)?

**State management:**
All state is kept in a local in-memory HashMap, updated asynchronously by a Kafka consumer reading the same trade event stream. A separate **reconciliation job** runs every 5 minutes to compare the in-memory state against the database and correct any drift caused by Kafka consumer lag.

**Circuit breakers:**
- Symbol-level halt: if price of any symbol moves > 10% in 5 minutes, all orders for that symbol are rejected until an operator or timer resets the breaker.
- Account-level halt: if an account generates > 1,000 rejected orders in 10 seconds, the account is auto-suspended.

### 7.4 Message Queue (Kafka)
Kafka is the backbone connecting the matching engine (write side) to all downstream consumers (read side). This decoupling is what allows the matching engine to stay fast without waiting for downstream services to process events.

**Topic design:**
- One Kafka topic per symbol: `trades.AAPL`, `trades.TSLA`, etc.
- A separate `orders.*` topic stream for all order lifecycle events (placed, cancelled, amended).
- A `user-events` topic for account-level events (deposits, withdrawals, balance changes).

**Key properties used:**
- **At-least-once delivery**: every event is guaranteed to be delivered, possibly more than once. All consumers must be idempotent.
- **Log retention**: 7 days of events retained, allowing consumers to replay from any point in the last week.
- **Ordered within a partition**: events for the same symbol always arrive in order because they share a partition.

### 7.5 Write Store (Cassandra)
Cassandra stores the raw, immutable event log — every trade, every order state change, forever. This is the **source of truth** for regulatory reporting and auditing.

**Why Cassandra:** Its LSM-Tree (Log-Structured Merge Tree) architecture makes sequential writes extremely fast. New writes go to an in-memory Memtable, which is periodically flushed to an immutable SSTable file on disk. No random I/O on writes.

**Primary table:** `trades` partitioned by `(symbol, date)` and clustered by `(timestamp, trade_id)`. This design allows efficient lookups like "all trades for AAPL today" without touching other partitions.

### 7.6 Read Models (PostgreSQL + Redis)
Rather than querying Cassandra for user-facing reads, the platform maintains **pre-computed, purpose-built read models** in PostgreSQL. These are updated asynchronously by Kafka consumers every time a trade executes.

**Key read model tables:**
- `user_positions`: current shares held per trader per symbol (updated on every fill).
- `user_orders`: all open orders for a trader (updated on place/cancel/fill).
- `user_trades`: all filled trades for a trader, last 90 days.
- `user_balances`: current cash and reserved amount per trader.

**Redis caching layer:**
- Current best bid/ask (last-known price) per symbol: refreshed on every trade, TTL 1 second.
- Session tokens and API key lookups: TTL matches session expiry.
- Rate limit counters per trader: sliding window, TTL 1 minute.
- Read-your-own-writes buffer: newly placed orders cached for 5 seconds so the trader sees them immediately even before the replica catches up.

### 7.7 Market Data Distribution
The market data pipeline fans out trade events from one source to millions of subscribers.

**Pipeline stages:**
1. **Feed Handlers** consume from Kafka, normalize events into standardized formats (JSON for retail, binary FIX for institutional), and compute derived values like VWAP and OHLC bars.
2. **Distribution Servers** maintain persistent WebSocket connections with clients and push updates. Each server handles ~50,000 connections. With 2 million subscribers, you need ~40 distribution servers.
3. **Subscription Registry (Redis)**: maps each symbol to the set of distribution servers that have at least one subscriber for that symbol. Feed handlers consult this to avoid broadcasting to irrelevant servers.
4. **CDN layer**: for retail clients on web/mobile who only need a price update every 1–2 seconds, a CDN node serves cached last-known prices, reducing load on distribution servers.

**Snapshot + Delta delivery:**
On connect, a client receives a full order book snapshot for their subscribed symbols. After that, only **deltas** (incremental changes) are sent. Each delta carries a sequence number; if the client detects a gap, it requests a fresh snapshot. This keeps bandwidth orders of magnitude lower than sending full snapshots on every change.

---

## 8. Detailed Feature Flows

### 8.1 Order Placement — Full End-to-End Flow

This is the most important flow to know completely for any interview.

**Step 1 — Client sends order.** Alice submits "Buy 500 AAPL at $150.00 limit" via her trading app. Her client sends an HTTPS POST to the API Gateway with her API key in the header.

**Step 2 — API Gateway processes the request.**
- Validates the API key against Redis (~0.2ms).
- Checks rate limit counter in Redis (~0.1ms).
- Generates a Snowflake order ID (~0.05ms).
- Looks up shard mapping: AAPL → Shard 3.
- Saves mapping: `order_id → connection_id` for routing the response back later.
- Forwards the order over internal binary protocol to Shard 3 (~0.2ms).

**Step 3 — Risk Engine pre-trade checks.**
- Checks Alice's in-memory reserved balance: she has $80,000 available. Buy requires $75,000. Pass.
- Checks Alice's AAPL position: she holds 2,000 shares; limit is 5,000. Adding 500 keeps her at 2,500. Pass.
- Checks order rate: Alice has submitted 12 orders in the past second; limit is 100/sec. Pass.
- Checks price sanity: current AAPL price is $149.80; her limit of $150.00 is within ±20%. Pass.
- **Reserves $75,000** from Alice's available balance immediately.
- Total time: ~0.3ms.

**Step 4 — Matching Engine receives the order.**
- **WAL write first**: appends `ORDER_PLACED {id: X, side: BUY, price: 150.00, qty: 500, trader: alice}` to the WAL (~0.5ms for sequential disk write).
- Checks the ask side of the AAPL order book. Best ask is $149.90 (Eve wants to sell 500 shares). Since $149.90 < $150.00 (Alice's limit), a match fires.
- Creates `TRADE_EXECUTED {buyer: alice, seller: eve, price: $149.90, qty: 500}`. Alice gets the better price since she was willing to pay up to $150.00.
- Removes Eve's order from the ask side. Both orders are fully filled.
- WAL append: `TRADE_EXECUTED {...}`.
- Total time: ~0.01ms (pure in-memory operation after the WAL write).

**Step 5 — Events published to Kafka.**
- Matching engine publishes `TRADE_EXECUTED` event to `trades.AAPL` topic.
- Uses the **Outbox pattern**: the event is first written to a local outbox table atomically with the WAL entry, then the outbox relay picks it up and publishes to Kafka. This prevents the gap where WAL succeeds but Kafka publish fails.

**Step 6 — Downstream consumers process the event.**
- **Persistence consumer**: writes trade record to Cassandra.
- **Read model consumer**: updates `user_positions` and `user_balances` in PostgreSQL for both Alice and Eve.
- **Notification consumer**: pushes fill confirmation to Alice's WebSocket via the API Gateway using the `order_id → connection_id` mapping saved in Step 2.
- **Risk engine consumer**: updates Alice's and Eve's in-memory position and balance state.
- **Market data consumer**: feed handler receives the trade, updates last price for AAPL, pushes delta to distribution servers, which push to all AAPL subscribers.

**Step 7 — Alice sees "Filled @ $149.90"** on her screen, approximately 2–5ms after clicking Buy.

**Timeline summary:**
```
t=0ms       Alice clicks Buy
t=0.2ms     Gateway authenticates
t=0.5ms     Risk checks pass, funds reserved
t=1.0ms     WAL write completes
t=1.01ms    Order matched in memory
t=1.5ms     Kafka event published
t=2.5ms     Notification sent to Alice's WebSocket
t=5ms       Portfolio view updated in read model
t=5ms       Market data feed updated for all AAPL subscribers
```

---

### 8.2 Order Cancellation Flow

**Step 1** — Alice sends `DELETE /orders/{order_id}`.

**Step 2** — Gateway authenticates and routes to the correct shard (shard is determined by the symbol embedded in the order, looked up from an order registry in Redis).

**Step 3** — Matching engine:
- Looks up the order in the in-memory order book.
- Verifies the order belongs to Alice (authorization check).
- Checks the order is still open (not already filled or partially filled to zero remaining).
- Appends `ORDER_CANCELLED {id: X}` to the WAL.
- Removes the order from the in-memory order book.
- Publishes `ORDER_CANCELLED` event to Kafka.

**Step 4** — Downstream:
- Risk engine consumer **releases the reserved funds** back to Alice's available balance.
- Read model consumer removes the order from `user_orders`.
- Alice's WebSocket receives `ORDER_CANCELLED` confirmation.

**Race condition:** What if a fill and a cancel arrive at the matching engine simultaneously? The single-threaded nature of the matching engine resolves this deterministically — whichever arrives first wins. If the fill processes first, the cancel finds no open order and returns an error. If the cancel processes first, the fill finds no opposing order and does nothing. No locks needed because the engine is single-threaded.

---

### 8.3 Market Data Subscription Flow

**Step 1** — Client opens a WebSocket connection to a distribution server and sends: `SUBSCRIBE {symbols: ["AAPL", "TSLA"], level: "L2"}`.

**Step 2** — Distribution server registers the subscription in Redis: `AAPL_subscribers → add(DS-7)`, `TSLA_subscribers → add(DS-7)`.

**Step 3** — Distribution server sends a **full snapshot** of the current AAPL and TSLA order books to the client. This snapshot is fetched from the feed handler's in-memory state.

**Step 4** — From this point on, whenever a trade or order book change occurs for AAPL:
- Feed handler publishes a **delta event** with a sequence number.
- Redis registry tells the feed handler which distribution servers have AAPL subscribers.
- Feed handler sends the delta only to those distribution servers.
- Distribution server pushes the delta to all its AAPL subscribers over WebSocket.

**Step 5** — Client applies each delta to its local order book copy, keeping its view perfectly in sync.

**Sequence gap handling:** If the client receives sequence 1042 after 1040 (missing 1041 due to a network glitch), it immediately requests a fresh snapshot and resumes from there. Stale data is discarded.

**Slow consumer handling:** If a client cannot consume data fast enough, the distribution server's per-connection ring buffer fills up. Once full, the server disconnects the client and forces a reconnect-with-snapshot. A stale, gapped feed is worse than a brief disconnect.

---

### 8.4 Trade Settlement Flow

After a trade executes, the platform handles the immediate ("cash and position") settlement internally, while the actual securities settlement (physical transfer of shares) is handled externally via a clearing house.

**Immediate (real-time) settlement:**
- On `TRADE_EXECUTED` event, the balance service debits Alice's cash by the trade value and credits Eve's cash.
- The position service increments Alice's AAPL position by 500 shares and decrements Eve's by 500 shares.
- These two operations form a **SAGA**: if either fails, a compensating transaction reverses the completed step.

**Clearing house settlement (T+2):**
- The platform publishes confirmed trade events to a clearing house integration service.
- The clearing house batches all trades for the day, nets offsetting positions, and transfers actual securities between custodian accounts.
- This happens asynchronously and does not affect the real-time trading experience.

---

### 8.5 System Startup / Crash Recovery Flow

**On matching engine restart:**
1. Load the **latest snapshot** of the order book from object storage (S3 or equivalent). Snapshots are taken every 5 minutes.
2. Open the **WAL file** and seek to the position corresponding to the snapshot timestamp.
3. **Replay all WAL entries** from that point forward, re-applying each event to rebuild the in-memory order book state.
4. Verify the final state against the follower replica (which may have continued running). If there are discrepancies, the follower's state is treated as authoritative.
5. Resume accepting orders.

**Typical recovery time:** 5 minutes of WAL replay at normal speeds takes 10–30 seconds to replay due to the speed of sequential reads.

---

### 8.6 Adding a New Shard (Re-sharding Flow)

**Why this is non-trivial:** If you use `shard = hash(symbol) % N` and N increases from 10 to 11, most symbols move to new shards, requiring a massive migration under live traffic.

**Consistent hashing approach:**
1. New shard S11 is added to the ring at a new position.
2. The shard coordinator identifies which symbols currently owned by the adjacent shard (say, S3) now fall within S11's ring range — typically ~10% of S3's symbols.
3. S3 streams its order book state for those symbols to S11 (state transfer).
4. Once S11 confirms it has fully received and applied the state, the routing table is atomically updated to point those symbols at S11.
5. S3 discards its now-transferred order books and frees memory.
6. No other shards are affected. No downtime.

---

## 9. Data Models

### 9.1 Order
```
Order {
  order_id        : int64       (Snowflake ID)
  trader_id       : string
  symbol          : string      (e.g. "AAPL")
  side            : enum        (BUY | SELL)
  order_type      : enum        (LIMIT | MARKET)
  price           : int64       (in cents, 0 for market orders)
  original_qty    : int64       (shares)
  remaining_qty   : int64       (decremented on partial fill)
  status          : enum        (OPEN | PARTIALLY_FILLED | FILLED | CANCELLED)
  created_at      : timestamp
  updated_at      : timestamp
}
```

### 9.2 Trade
```
Trade {
  trade_id        : int64       (Snowflake ID)
  buy_order_id    : int64
  sell_order_id   : int64
  buyer_id        : string
  seller_id       : string
  symbol          : string
  price           : int64       (in cents — execution price)
  quantity        : int64       (shares traded)
  executed_at     : timestamp
  sequence_num    : int64       (monotonically increasing per symbol)
}
```

### 9.3 User Position
```
UserPosition {
  trader_id       : string
  symbol          : string
  quantity        : int64       (current shares held; negative = short)
  avg_cost        : int64       (in cents, volume-weighted average entry price)
  unrealized_pnl  : int64       (derived: (current_price - avg_cost) × quantity)
  updated_at      : timestamp
}
```

### 9.4 User Balance
```
UserBalance {
  trader_id       : string
  total_balance   : int64       (in cents)
  reserved_balance: int64       (in cents — locked for open orders)
  available_balance: int64      (derived: total - reserved)
  updated_at      : timestamp
}
```

### 9.5 WAL Entry (event log format)
```
WalEntry {
  sequence_num    : int64
  timestamp       : int64       (epoch microseconds)
  event_type      : enum        (ORDER_PLACED | ORDER_CANCELLED | TRADE_EXECUTED | ...)
  payload         : bytes       (serialized proto/avro of the specific event)
  checksum        : int32       (CRC32 for corruption detection)
}
```

---

## 10. Database Design

### 10.1 Storage Technology Choices

**Cassandra (Write Store — Trade & Order Events)**
- Chosen for: extremely high write throughput, linear horizontal scalability, no single point of failure, built-in multi-datacenter replication.
- Partition key design: `(symbol, date)` so all trades for AAPL on a given day are co-located. Cluster key: `(executed_at DESC, trade_id)` for efficient time-range queries.
- Tradeoff: poor at flexible queries and joins. That is acceptable because Cassandra is only used for the raw event log — all user-facing queries go through the read models.

**PostgreSQL (Read Models — Positions, Balances, Orders)**
- Chosen for: ACID transactions, rich query support, excellent index performance, MVCC for concurrent reads without blocking writes.
- Read replicas: 3 replicas per region, with all user-facing reads distributed across replicas. Writes go to primary only.
- Key indexes: `user_positions(trader_id, symbol)`, `user_orders(trader_id, status, created_at)`, `trades(trader_id, executed_at DESC)`.
- Replication lag mitigation: newly placed orders cached in Redis for 5 seconds (read-your-own-writes pattern) to hide replication lag from the submitting trader.

**Redis (Cache & Coordination)**
- Current prices (last trade) per symbol: TTL 1 second.
- Session/API key lookup: TTL = session expiry.
- Rate limit counters: sliding window with TTL 60 seconds.
- Balance reservations: TTL 24 hours (orders expire after a day if unfilled).
- Shard routing map: persistent, updated only during resharding.
- Kafka consumer deduplication keys: TTL 24 hours, used by all idempotent consumers.

### 10.2 Sharding Strategy for Databases

**Cassandra** shards automatically using consistent hashing on the partition key. No manual intervention needed for scaling — add nodes and the ring redistributes.

**PostgreSQL** read models are sharded by `trader_id` using consistent hashing — all data for trader Alice is on Shard A, all data for Bob is on Shard B. This ensures user portfolio queries never cross shard boundaries.

---

## 11. API Design

### 11.1 REST Endpoints

```
POST   /v1/orders                      Place a new order
DELETE /v1/orders/{order_id}           Cancel an open order
GET    /v1/orders/{order_id}           Get order status
GET    /v1/orders?status=OPEN          List trader's open orders (paginated)
GET    /v1/trades?symbol=AAPL&limit=50 Get trader's recent fills
GET    /v1/portfolio                   Get current positions and balances
GET    /v1/symbols/{symbol}/orderbook  Get current order book snapshot
GET    /v1/symbols/{symbol}/trades     Get recent public trades for a symbol
POST   /v1/auth/token                  Get session token
POST   /v1/auth/apikeys                Create an API key
```

### 11.2 WebSocket Streams

Connection: `wss://api.exchange.com/stream`

```
Client → Server (subscribe):
{ "action": "subscribe", "symbols": ["AAPL", "TSLA"], "channels": ["ticker", "orderbook", "fills"] }

Server → Client (ticker update):
{ "channel": "ticker", "symbol": "AAPL", "price": 15005, "volume": 12300, "seq": 10042 }

Server → Client (orderbook delta):
{ "channel": "orderbook", "symbol": "AAPL", "seq": 10043,
  "bids": [{"price": 14995, "qty": 800}],   ← updated levels
  "asks": [{"price": 15010, "qty": 0}] }    ← qty=0 means remove this level

Server → Client (fill notification):
{ "channel": "fills", "order_id": "...", "status": "FILLED",
  "filled_qty": 500, "fill_price": 14990, "timestamp": "..." }
```

### 11.3 Request / Response Schema (Order Placement)

```
POST /v1/orders
Request:
{
  "symbol": "AAPL",
  "side": "BUY",
  "order_type": "LIMIT",
  "price": 15000,          ← in cents ($150.00)
  "quantity": 500,
  "time_in_force": "GTC"   ← Good Till Cancelled | DAY | IOC | FOK
}

Response 200:
{
  "order_id": "7362845019283456",
  "status": "OPEN",
  "created_at": "2025-03-16T09:32:10.123Z",
  "reserved_funds": 7500000  ← in cents ($75,000.00)
}

Response 400 (risk rejection):
{
  "error": "INSUFFICIENT_FUNDS",
  "message": "Available balance $12,000 is less than required $75,000",
  "available": 1200000,
  "required": 7500000
}
```

---

## 12. Caching Strategy

### 12.1 What to Cache and Why

**Symbol prices (Redis, TTL 1s):** The most-read value in the entire system. Every dashboard, every portfolio P&L calculation, every risk check reads the current price. Serving it from Cassandra on every read would be catastrophic. Redis makes it a sub-millisecond memory lookup.

**Session tokens and API keys (Redis, TTL = session expiry):** Authentication happens on every single API request. This cannot hit PostgreSQL every time at 500,000 requests/second. Redis makes auth ~0.2ms.

**Rate limit counters (Redis, sliding window, TTL 60s):** Token bucket or sliding window counters must be shared across all gateway instances so a trader cannot bypass limits by spreading requests across gateways.

**Order read-your-own-writes cache (Redis, TTL 5s):** After placing an order, the trader should see it immediately when they query their open orders — even though the PostgreSQL read replica may be a few hundred milliseconds behind. The gateway caches newly placed orders in Redis for 5 seconds and merges them with the database results on read.

**Order book snapshots (feed handler in-memory):** Feed handlers maintain the current full order book state in memory for each symbol they handle, so they can serve a snapshot to any newly subscribing client without touching Cassandra.

### 12.2 Cache Invalidation Strategy

For symbol prices, invalidation is simple: the feed handler overwrites the Redis key on every trade. TTL acts as a safety net.

For session tokens, explicit invalidation happens on logout. TTL handles expiry.

For the read-your-own-writes cache, the entry is either overwritten (order status updated to FILLED/CANCELLED) or expires after 5 seconds — by which time the read replica has definitely caught up.

---

## 13. Message Queue Design

### 13.1 Why Kafka

Kafka is chosen over alternatives (RabbitMQ, AWS SQS) because: it retains the full event log for 7 days (allowing replay), it maintains order within a partition (critical for sequential order book events), it handles 5M+ messages/second without breaking a sweat, and it supports many independent consumer groups (matching engine events can be consumed simultaneously by persistence, risk, market data, and notification services without any of them affecting each other's offset).

### 13.2 Topic Design

```
trades.{symbol}        ← one topic per symbol, partitioned by symbol
                          (AAPL trades never mix with TSLA trades)

orders.lifecycle       ← all order placed/cancelled/amended events
                          partitioned by trader_id (all of Alice's
                          orders in one partition, in order)

user.balances          ← deposit, withdrawal, and settlement events
                          partitioned by trader_id

market.data.{symbol}   ← order book change events, consumed by
                          feed handlers for market data distribution

system.heartbeats      ← periodic "I am alive" messages from every
                          critical component, consumed by the watchdog
```

### 13.3 Consumer Groups

Each downstream service has its own consumer group, so they maintain independent read positions (offsets) in Kafka. The persistence service being slow has zero impact on the notification service.

### 13.4 Idempotency and Deduplication

Every message carries a unique `event_id`. Every consumer, before processing a message, checks Redis: "have I already processed this event_id?" If yes, acknowledge and skip. If no, process, write to Redis, then acknowledge. This makes every consumer safe to retry and makes Kafka's at-least-once delivery effectively exactly-once from the consumer's perspective. Redis keys expire after 24 hours — sufficient coverage since duplicate events arrive within seconds.

---

## 14. Failure Scenarios & Mitigations

### 14.1 Matching Engine Crash

**What happens:** The in-memory order book is lost.

**Mitigation:** Replay the WAL from the last snapshot. With snapshots every 5 minutes and fast sequential reads, recovery takes ~10–30 seconds. The follower replica (which may still be running) can be promoted immediately while the leader recovers, giving near-zero downtime. Failover is automatic via the shard coordinator.

### 14.2 Kafka Broker Failure

**What happens:** Event publishing may fail; consumers may stall.

**Mitigation:** Kafka runs as a cluster with replication factor 3 — any two brokers can fail and the cluster continues. The outbox pattern ensures the matching engine's WAL and Kafka stay in sync — if Kafka is temporarily unavailable, the outbox relay retries until it succeeds. Consumers resume from their last committed offset when Kafka recovers.

### 14.3 Risk Engine Crash / Stale State

**What happens:** Risk engine's in-memory state may be stale (Kafka consumer lag), causing it to approve orders it should reject or reject orders it should approve.

**Mitigation:** The risk engine errs conservatively — it uses slightly inflated reserved amounts to absorb brief lag. A circuit breaker halts trading if Kafka consumer lag on the risk engine topic exceeds a threshold (e.g. 5,000 events, representing ~0.1 seconds of lag). The reconciliation job running every 5 minutes catches any accumulated drift.

### 14.4 Database Primary Failure (PostgreSQL)

**What happens:** Writes to read models fail; traders may see stale portfolio data.

**Mitigation:** PostgreSQL runs with synchronous streaming replication to one standby. Automatic failover (via Patroni or AWS RDS Multi-AZ) promotes the standby in ~30 seconds. Because PostgreSQL read models are derived from Kafka (not the source of truth), they can be fully rebuilt from the Kafka event log at any time. The source of truth (Cassandra + WAL) is unaffected.

### 14.5 Network Partition Between Regions

**What happens:** Tokyo region loses connectivity to New York region. Tokyo traders cannot reach the AAPL matching engine in New York.

**Mitigation:** The system chooses **consistency over availability** for matching (CP in CAP terms). During the partition, Tokyo traders cannot trade AAPL — they receive an error explaining the service is temporarily unavailable for that symbol. Trading for Tokyo-listed stocks (whose engines are in Tokyo) continues normally. Once the partition heals, the Tokyo gateway resumes routing orders to New York. No inconsistency is possible because there is only one authoritative matching engine per symbol.

### 14.6 Runaway Trading Algorithm (Business Logic Failure)

**What happens:** A buggy algorithm submits orders at 100x normal rate, overwhelming a shard.

**Mitigation:** API Gateway rate limiting rejects excess requests at the front door. Risk engine order-rate checks provide a second line of defence. If a shard still shows abnormal load (detected via metrics), the circuit breaker pauses that account. Human operators are paged within seconds via automated alerting on the "rejected order rate" metric.

### 14.7 Duplicate Trade Execution

**What happens:** Kafka delivers the same `TRADE_EXECUTED` event twice; consumers process it twice; Alice is charged double.

**Mitigation:** Every consumer checks the Redis deduplication store before processing. If `trade_id` is already present, the event is acknowledged and skipped. The balance service uses database-level idempotency: `INSERT INTO trades ... ON CONFLICT (trade_id) DO NOTHING`. Multiple layers of deduplication ensure exactly-once semantics even if upstream retries.

---

## 15. Key Design Tradeoffs

**In-memory matching vs. database matching.** In-memory gives sub-millisecond latency but requires WAL + replication for durability. Database-backed matching is simpler to operate but cannot meet the latency requirements. The WAL makes the in-memory approach safe, so the tradeoff is decisively in favour of in-memory.

**CQRS (separate read and write paths) vs. a single database.** CQRS adds operational complexity — you now have Cassandra, PostgreSQL, and Redis to operate instead of one database. But it is the only way to serve both 500,000 writes/second and millions of reads/second with low latency. The tradeoff is complexity in exchange for independent scalability of reads and writes.

**Eventual consistency of read models vs. strong consistency.** Read models (portfolio views, order history) are a few hundred milliseconds behind the source of truth during peak load. This is acceptable for user dashboards. It is not acceptable for the risk engine, which must see accurate balances — the risk engine's state is kept strongly consistent via the reservation model, at the cost of slightly over-reserving funds.

**Single-master per instrument vs. multi-region active-active matching.** Single-master guarantees consistency — no duplicate trades, no split-brain order books. Multi-region active-active would give lower latency for distant traders but introduces the coordination problem that no distributed system has cleanly solved without sacrificing correctness or performance. For a financial exchange where correctness is legally mandated, single-master is the only viable choice.

**At-least-once delivery with idempotent consumers vs. exactly-once delivery.** Kafka supports exactly-once delivery using transactions, but it adds significant latency and complexity. At-least-once delivery with idempotent consumers achieves the same correctness guarantee (no duplicates processed) at much lower overhead, because the deduplication check is a simple Redis key lookup.

**Synchronous vs. asynchronous replication.** Synchronous replication (wait for follower acknowledgement before confirming to trader) gives zero data loss on failover but adds the round-trip latency to every trade. Asynchronous replication is faster but risks losing the most recent events if the leader crashes. Semi-synchronous replication (wait for one follower only) is the standard compromise — near-zero data loss with only ~1ms added latency.

---

## 16. Interview Cheat Sheet

### Components and Their Primary Purpose

**API Gateway** — auth, rate limit, route, translate protocol, assign Snowflake IDs.

**Risk Engine** — pre-trade checks in memory (position limits, funds, fat-finger, wash trade), circuit breakers.

**Matching Engine** — in-memory order book (red-black tree), price-time priority, single-threaded, WAL-backed.

**WAL (Write-Ahead Log)** — sequential disk log written before processing; enables crash recovery via snapshot + replay.

**Kafka** — async event bus connecting write side to all read-side consumers; enables CQRS; at-least-once delivery.

**Cassandra** — raw event write store; LSM-tree for high write throughput; partitioned by (symbol, date).

**PostgreSQL** — read models for user portfolio, positions, orders; ACID; read replicas for scale.

**Redis** — prices, session cache, rate limits, deduplication store, reservations, routing map.

**Feed Handlers** — normalize market data from Kafka, compute derived values (VWAP, OHLC), serve snapshots.

**Distribution Servers** — WebSocket fan-out to millions of market data subscribers; snapshot + delta protocol.

### The Tradeoffs Interviewers Want You to Volunteer

**CAP theorem**: the system chooses Consistency + Partition Tolerance (CP) for matching, accepting that distant traders experience higher latency (or temporary unavailability during a partition) rather than risking inconsistency.

**CQRS**: reads and writes are decoupled. This means read models are eventually consistent with a lag of milliseconds to seconds. Read-your-own-writes is solved with a Redis buffer, not by making reads strongly consistent — that would sacrifice read scalability.

**Idempotency**: at-least-once delivery is good enough when every consumer is idempotent. This is simpler and faster than Kafka exactly-once transactions, with the same correctness outcome.

**Sharding key choice**: symbol is the natural key because trades never cross symbol boundaries. This gives perfect fault isolation (one shard's failure doesn't affect others) but means user-level queries (all orders for Alice) must scatter-gather across shards — solved by maintaining user-centric read models in PostgreSQL.

### Numbers to Remember

```
Matching engine latency:     < 1ms
End-to-end order latency:    < 10ms
Market data latency:         < 50ms
Peak throughput:             500,000 orders/second
Matching shards:             20 (with 40 processes including replicas)
Distribution servers:        40 (for 2M concurrent WebSocket clients)
Daily trade storage:         ~10 GB
Kafka retention:             7 days
Snapshot frequency:          every 5 minutes
Recovery time after crash:   10–30 seconds
```

### Follow-Up Questions and Short Answers

**"How do you handle partial fills?"** The incoming order's remaining quantity decreases on each partial fill. A `TRADE_EXECUTED` event is published for each partial fill. The order stays in the book until remaining quantity reaches zero, at which point it is removed and a final `ORDER_FILLED` event is published.

**"How do you prevent a trader from seeing stale balances after placing an order?"** The read-your-own-writes pattern: the newly placed order and balance reservation are written to Redis immediately and merged into query results for the next 5 seconds, regardless of what the read replica shows.

**"What happens if two traders simultaneously submit matching orders?"** The single-threaded matching engine serializes them — one arrives first (even by a microsecond) and matches first. The second order either matches against remaining quantity, sits in the book, or is rejected (for IOC orders). No race condition is possible in a single-threaded engine.

**"How do you scale if one symbol (like AAPL) gets 100x normal volume?"** AAPL can be assigned its own dedicated shard. The routing table gets a special-case entry `AAPL → shard_99`. After the volume spike subsides, AAPL returns to its normal shard. This is a manual override — the risk engine's circuit breaker may have already auto-halted trading if the volume was accompanied by extreme price movement.

**"How do you ensure fairness across geographically distant traders?"** The platform cannot eliminate the latency disadvantage of physical distance — this is a physics limit. What it can do is ensure that given the same network latency, time priority is respected. The sequencer assigns arrival-order sequence numbers before orders enter the matching engine, preventing gateway-level reordering.

**"How would you add support for options contracts?"** The matching engine architecture is unchanged. Options have more complex validity rules (expiry dates, strike prices, exercise style) that the risk engine and order validation layer must enforce. The order book data model gains additional fields. The pricing feed adds options-specific derived values (IV, Greeks). This is additive, not architectural.

---

*End of Document*

