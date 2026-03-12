# Redis Sharding — Let's Build the Story from Scratch

Let's start right at the beginning, before Redis even enters the picture. To understand *why* Redis does what it does, you first need to feel the pain that engineers felt before the solution existed.

---

## Chapter 1: The Single Server Problem

Imagine you're building a wildly popular e-commerce app. You decide to use Redis as your cache — storing user sessions, product data, shopping cart info. Life is great. Your single Redis server hums along happily.

Then your app blows up. You go from 10,000 users to 10 million. Suddenly your single Redis server is:

**Running out of memory.** Redis lives entirely in RAM. A single machine might give you 64GB or 128GB, but your dataset is now 500GB. There's simply no room.

**Becoming a bottleneck.** Even if memory wasn't an issue, one machine can only handle so many requests per second. You're now getting 1 million requests/second and a single server is choking.

**A single point of failure.** If that one machine dies, your entire caching layer dies with it.

So the obvious question engineers asked was: *"What if we just split the data across multiple Redis servers?"*

This is the core idea of **sharding** — distributing your data across multiple machines (called shards or nodes) so that each machine only holds a *portion* of the total data.

But here's where the story gets interesting, because the *how* of splitting data turned out to be a surprisingly hard problem.

---

## Chapter 2: The Naive Approach — Modulo Hashing

The first idea engineers reached for was beautifully simple. Say you have 3 Redis servers (nodes). When you want to store a key like `"user:1234"`, you:

1. Hash the key to get a number: `hash("user:1234") = 87239`
2. Take that number modulo the number of nodes: `87239 % 3 = 2`
3. Store the key on **Node 2**

To retrieve it later, you do the exact same math and land on the same node. Clean, fast, deterministic.

```
Key: "user:1234"
hash = 87239
87239 % 3 = 2  →  goes to Node 2

Key: "product:99"
hash = 54100
54100 % 3 = 1  →  goes to Node 1
```

This works perfectly... until the day you need to **add or remove a node**.

### The Catastrophic Reshuffling Problem

Your system grows and you need to add a 4th node. Now your modulo changes from `% 3` to `% 4`. Let's see what happens to our keys:

```
Key: "user:1234"
hash = 87239
87239 % 4 = 3  →  now goes to Node 3  ❌ (was Node 2!)

Key: "product:99"
hash = 54100
54100 % 4 = 0  →  now goes to Node 0  ❌ (was Node 1!)
```

Almost **every single key** maps to a different node now. In a production system with millions of cached keys, this means you suddenly can't find anything. Every cache lookup misses. Every miss hits your database. Your database gets absolutely hammered. This is called a **cache stampede** and it can bring down your entire system.

In practice, when you change from N to N+1 nodes with simple modulo hashing, roughly `(N-1)/N` of all your keys end up remapped — so going from 3 to 4 nodes remaps about 75% of everything. That's a catastrophe.

This is the pain point that drove engineers to look for something smarter.

---

Take a moment to make sure this all clicks. The key insight to hold onto is: **modulo hashing is fragile because it ties every key's location to the total number of nodes.** Change the denominator, and the whole system breaks.

Does this make sense so far? Once you're comfortable with this problem, we'll move into **Consistent Hashing** — the elegant solution the industry came up with next (and the one Redis *considered* before going a different direction entirely). 🚀

---

# Chapter 3: Consistent Hashing — The Elegant Fix

So the industry had a clear problem: modulo hashing causes massive reshuffling whenever the cluster size changes. Engineers needed a scheme where adding or removing a node only affects a *small, predictable* portion of keys — not everything at once.

The solution that emerged in 1997 (from a famous paper by Karger et al.) was **Consistent Hashing**. The core idea is genuinely beautiful, so let's build it up piece by piece.

---

## The Hash Ring

Instead of thinking of your hash space as a straight line (`0` to some big number), consistent hashing bends that line into a **circle** — called the hash ring.

Imagine a clock face, but instead of 12 numbers, it has numbers from `0` to `2³²-1` (about 4 billion positions), wrapping around. The ring looks like this:

```
           0
      ...  |  ...
   3.8B         0.4B
      
   2.8B         1.2B
      ...  |  ...
          2B
```

Now you place your **nodes** on this ring by hashing the node's name or IP address. Say you have 3 nodes:

```
hash("node-A") = 0.3B   →  placed at position 300 million on the ring
hash("node-B") = 1.5B   →  placed at position 1.5 billion
hash("node-C") = 3.2B   →  placed at position 3.2 billion
```

When you want to store a key like `"user:1234"`, you hash that key too and find its position on the ring. Then you walk **clockwise** until you hit the first node you encounter — that's where the key lives.

```
hash("user:1234") = 0.9B
→ walk clockwise from 0.9B
→ first node encountered is Node-B at 1.5B
→ store on Node-B ✅
```

```
hash("product:99") = 2.1B
→ walk clockwise from 2.1B
→ first node encountered is Node-C at 3.2B
→ store on Node-C ✅
```

Each node effectively "owns" the arc of the ring that falls *before* it (counter-clockwise). Node-B owns everything between Node-A and Node-B. Node-C owns everything between Node-B and Node-C. And so on.

---

## Why Adding a Node Is Now Cheap

Here's the magic. Say you add a new **Node-D** at position `1.1B` on the ring (between Node-A at 0.3B and Node-B at 1.5B).

Before Node-D existed, Node-B owned everything from `0.3B` to `1.5B`. Now Node-D steps in at `1.1B` and takes ownership of the arc from `0.3B` to `1.1B`. Node-B is left with only `1.1B` to `1.5B`.

This means **only the keys that were in that one arc need to move** — specifically, only the keys that hash to between `0.3B` and `1.1B` migrate from Node-B to Node-D. Every other key on every other node is completely untouched.

In a cluster with N nodes, adding one new node only disturbs `1/N` of the total keys on average. Going from 3 to 4 nodes? You move roughly 25% of data instead of 75%. Going from 100 to 101 nodes? You move about 1% of data. This is a *massive* improvement over modulo hashing.

---

## But Consistent Hashing Has Its Own Problem — Uneven Distribution

You might already be seeing the issue. You're placing nodes on the ring by hashing their names/IPs, and **there's no guarantee those positions will be evenly spread out.**

With just 3 nodes, you might get something like:

```
Node-A at position:  0.1B  (very early)
Node-B at position:  0.15B (right next to A!)
Node-C at position:  0.2B  (still close to A and B!)
```

Now Node-C "owns" the arc from `0.2B` all the way around to `0.1B` — that's nearly the *entire ring*. Node-C gets 90% of the keys while Node-A and Node-B share the remaining 10%. This is called a **hotspot** and it defeats the whole purpose of sharding.

The fix engineers came up with is called **virtual nodes** (vnodes). Instead of placing each physical node once on the ring, you place it *many times* using different hash inputs:

```
Node-A appears at: hash("node-A-1"), hash("node-A-2"), hash("node-A-3") ... (say 150 positions)
Node-B appears at: hash("node-B-1"), hash("node-B-2"), hash("node-B-3") ... (150 positions)
Node-C appears at: hash("node-C-1"), hash("node-C-2"), hash("node-C-3") ... (150 positions)
```

With 150 virtual positions per node spread across the ring, the laws of statistics kick in and the distribution becomes much more even. Cassandra, for example, uses this exact approach.

---

## So Does Redis Use Consistent Hashing?

Here's the plot twist. Redis looked at consistent hashing, understood it well, and then said: *"This is clever, but it's still operationally complicated, and there's a cleaner way."*

The problems Redis's designers saw with consistent hashing were:

**The ring is conceptually indirect.** "Walk clockwise to find your node" is intuitive as a diagram, but in implementation it means every client needs to maintain a sorted map of the ring and do a binary search lookup for every request.

**Virtual nodes add coordination complexity.** When a node joins or leaves, you have to redistribute potentially dozens of virtual node positions, update every client's ring view, and carefully coordinate the data migration. At scale, this coordination is painful.

**You can't easily reason about "which keys live together."** If you want to guarantee that `"user:1234:profile"` and `"user:1234:cart"` are always on the same node (which is useful for atomic operations), consistent hashing makes this hard to guarantee without careful key design.

Redis wanted something more **explicit, predictable, and operationally transparent**. Something where a human — or a monitoring tool — could look at the cluster and immediately understand exactly which node is responsible for exactly which keys.

That solution is **Hash Slots** — and it's what Redis Cluster actually uses. It trades some of the theoretical elegance of consistent hashing for a system that's far easier to reason about, manage, and inspect in production.

---

This is where it gets really interesting, because Redis's hash slot design is genuinely clever in its simplicity. Are you ready to dig into how hash slots work? 🚀

---

# Chapter 4: Hash Slots — Redis's Own Invention

So Redis decided to throw out the ring entirely and design something from scratch. The question they started with was refreshingly simple: *"What if instead of a dynamic ring, we just pre-divide the entire key space into a fixed number of buckets — and then assign those buckets to nodes?"*

That's the entire idea of hash slots. Let's build it up carefully.

---

## The Fixed Key Space

Redis decided that the total key space would always be divided into exactly **16,384 slots** (numbered `0` to `16383`). This number is fixed forever — it never changes regardless of how many nodes you have. Think of these 16,384 slots as physical pigeonholes on a wall. Every key that ever enters your Redis cluster must go into exactly one of these pigeonholes.

To figure out which slot a key belongs to, Redis does something very straightforward:

```
slot = CRC16(key) % 16384
```

CRC16 is just a hashing function that takes your key string and produces a number. You then take that number modulo 16384, and you land on a slot between 0 and 16383. That's it. No ring, no clockwise walking, no virtual nodes — just a single arithmetic operation.

Let's trace through a real example:

```
key = "user:1234"
CRC16("user:1234") = 31778
31778 % 16384 = 15394   →  this key lives in slot 15394

key = "product:99"
CRC16("product:99") = 8700
8700 % 16384 = 8700     →  this key lives in slot 8700
```

Now, every key has a permanent, deterministic home slot. The slot number for `"user:1234"` will always be 15394, on any machine, forever. There's no ambiguity.

---

## Assigning Slots to Nodes

Here's where the elegance comes in. Once you have 16,384 slots, you simply divide them among your nodes. Say you start with 3 nodes — Redis would split them up roughly like this:

```
Node-A  →  owns slots 0     to 5460    (5,461 slots)
Node-B  →  owns slots 5461  to 10922   (5,462 slots)
Node-C  →  owns slots 10923 to 16383   (5,461 slots)
```

So if a key hashes to slot 15394, Redis knows it's in the range `10923–16383`, which belongs to Node-C. Done. No lookup table, no ring traversal — just a range check.

Every node in the cluster maintains a complete mental map of this slot assignment. So does every Redis client. When you try to access `"user:1234"`, your Redis client computes slot 15394, checks its map, and goes directly to Node-C. One hop, no guessing.

---

## What Happens When You Add a Node

This is where you really start to appreciate the design. Say your cluster grows and you add a new **Node-D**. You want to rebalance the cluster so all four nodes share the load equally.

Instead of any magical ring reshuffling, the cluster admin simply *reassigns some slots* from existing nodes to Node-D. It might look like this:

```
Take slots 0–1364     away from Node-A  →  give to Node-D
Take slots 5461–6825  away from Node-B  →  give to Node-D
Take slots 10923–12287 away from Node-C →  give to Node-D
```

After this, Node-D owns about 25% of the slots, and the other three nodes each own about 25% too. Now here's the critical thing to understand: **only the keys that live in those specific slots need to physically move.** All other keys stay exactly where they are, completely undisturbed.

And this migration happens **slot by slot**, one at a time, while the cluster is still serving live traffic. Redis doesn't need to take the cluster offline. A slot is in one of three states at any moment: *stable* (fully owned by a node), *migrating* (in the process of moving out), or *importing* (in the process of receiving). The cluster handles requests correctly throughout this whole process.

---

## Why 16,384? That's a Weirdly Specific Number.

This is a question that will impress an interviewer if you bring it up unprompted, because it shows you thought deeply about the design rather than just memorizing facts.

Antirez (Redis's creator) actually explained this decision in a GitHub issue. There are two forces pulling in opposite directions here.

On one side, you want *enough* slots so that when you have a large cluster — say 1,000 nodes — each node still owns a meaningful range of slots (16,384 ÷ 1,000 ≈ 16 slots per node). If you only had 1,000 total slots and 1,000 nodes, you'd have one slot per node, and rebalancing would become rigid and inflexible.

On the other side, every node in the cluster sends **heartbeat messages** to every other node, and each heartbeat message contains a bitmap representing which slots that node owns. A bitmap for 16,384 slots is exactly `16,384 ÷ 8 = 2,048 bytes` — just 2KB. This is small enough to send constantly without clogging up the network.

If Redis had chosen, say, 65,536 slots instead, that bitmap would be 8KB per heartbeat message. With hundreds of nodes all gossiping with each other every second, that adds up fast. So 16,384 is the sweet spot — expressive enough for large clusters, compact enough for efficient gossip.

---

## Hash Tags — Controlling Where Keys Land

Now here's a feature that consistent hashing really struggles with, but hash slots handles beautifully. Sometimes you *want* two different keys to land on the same node — for example, if you're using Redis's `MULTI/EXEC` transactions or Lua scripts that need to atomically touch multiple keys at once. Redis only allows multi-key operations if all the keys involved are on the same node.

Redis solves this with **hash tags**. If a key contains a substring wrapped in curly braces `{}`, Redis only hashes the part *inside* the braces to determine the slot. Everything outside the braces is ignored for slot assignment purposes.

So if you design your keys like this:

```
"user:{1234}:profile"   →  CRC16("1234") % 16384 = slot 6025
"user:{1234}:cart"      →  CRC16("1234") % 16384 = slot 6025
"user:{1234}:wishlist"  →  CRC16("1234") % 16384 = slot 6025
```

All three keys hash using just `"1234"`, so they always land on the same slot, which means the same node. You can now safely run a transaction touching all three keys atomically. This is a powerful and explicit feature — you're in full control of co-location by designing your key names thoughtfully.

---

## The Mental Model to Keep

Before moving on, here's the clean mental model to lock in your head for interviews:

Think of hash slots as a **post office with 16,384 PO boxes**. Every key in your system gets permanently assigned to one PO box (via CRC16 mod 16384). Your Redis nodes are like postal workers — each worker is *responsible* for a range of PO boxes. When you add a new worker, you just reassign some PO box ranges to them, and move only the mail that was sitting in those boxes. All other mail stays put. The number of PO boxes (16,384) never changes — only which worker manages which range changes.

---

The natural next question is: *how do clients know which node to talk to?* What happens if your client's slot map is stale and it goes to the wrong node? This brings us to one of the most interview-relevant topics: **redirection, the MOVED error, and the ASK error**. Ready to go there? 🚀

---

# Chapter 5: How Clients Find the Right Node — MOVED and ASK

So far we've established that the cluster has 16,384 slots distributed across nodes, and every key maps to exactly one slot. But here's the thing — your application doesn't talk to "the cluster" as a single entity. It talks to **individual nodes**. So the question becomes: how does your client know *which* node to talk to for any given key? And what happens when it gets it wrong?

This is where the story gets really operationally interesting.

---

## Every Node Knows Everything

The first important thing to understand is that in Redis Cluster, every single node holds a **complete copy of the slot map** — the full table of which node owns which slots. This is not centralized somewhere. There's no master directory server. Every node gossips with every other node and keeps its own up-to-date picture of the entire cluster topology.

This means any node can tell you, *"Hey, I don't own that slot, but Node-C does."* That's a crucial design decision because it means there's no single point of failure for routing information.

---

## The Client's Slot Cache

A smart Redis client (like `redis-py`, `Jedis`, or `StackExchange.Redis`) does something clever on startup. When it first connects to the cluster, it fetches the full slot map by sending a `CLUSTER SLOTS` or `CLUSTER SHARDS` command. The response tells it something like:

```
Slots 0–5460      →  Node-A at 192.168.1.1:6379
Slots 5461–10922  →  Node-B at 192.168.1.2:6379
Slots 10923–16383 →  Node-C at 192.168.1.3:6379
```

The client caches this map locally. Now, for every command it sends, it computes the slot (`CRC16(key) % 16384`), checks its local cache, and directly connects to the right node. In the happy path, this means **zero extra hops** — the client goes straight to the correct node every single time.

But what happens when the map becomes stale?

---

## The MOVED Error — Permanent Redirect

Imagine the cluster just finished migrating slot 15394 from Node-C to Node-D. Your client doesn't know this yet — its cached map still says slot 15394 belongs to Node-C. So it sends `GET user:1234` to Node-C.

Node-C receives the request, computes the slot, checks its own slot table, and realizes: *"I don't own slot 15394 anymore. Node-D does."* Rather than silently failing or returning a wrong result, Node-C sends back a very specific error response:

```
-MOVED 15394 192.168.1.4:6379
```

This is called a **MOVED error**. It's Redis's way of saying: *"This key has permanently moved. The correct address, right now and going forward, is this node."*

A smart client receives this MOVED error and does two things. First, it immediately retries the command against Node-D at `192.168.1.4:6379`. Second — and this is the important part — it **updates its local slot cache** so that slot 15394 now points to Node-D. All future requests for keys in that slot go directly to Node-D without any redirection. The client self-heals.

The MOVED error essentially means *"you're wrong, update your map and go there permanently."* It's a correction, not a temporary detour.

---

## The ASK Error — Temporary Redirect

Now here's where it gets subtle, and this is the kind of nuance that separates a great interview answer from a mediocre one.

What happens *during* a slot migration, while keys are in the process of being moved from Node-C to Node-D? This is a live system — you can't just pause the world while migration happens. At any given moment, some keys from slot 15394 might still be sitting on Node-C, while others have already been moved to Node-D. The slot is in a half-migrated state.

If a client asks Node-C for `user:1234` during this window, Node-C checks and finds the key has already been migrated to Node-D. Node-C responds with:

```
-ASK 15394 192.168.1.4:6379
```

This is the **ASK error**. It looks similar to MOVED but its meaning is critically different. ASK says: *"For this one specific request, try Node-D. But don't update your slot map — I still officially own this slot, we're just in the middle of moving things."*

The client follows the ASK by first sending a special `ASKING` command to Node-D (which tells Node-D to accept a request for a slot it's still importing), and then retrying the original command. Crucially, the client does **not** update its cache — because the migration isn't done yet and the slot ownership hasn't officially transferred.

---

## MOVED vs ASK — The Mental Model

The easiest way to keep these straight in your head is to think of it like moving houses.

A **MOVED** error is like updating your address with the post office. The move is done. Your old address is no longer valid. Anyone who tries to reach you at the old address should be told your new permanent address and update their records.

An **ASK** error is like calling the moving company mid-move. Some of your boxes are at the new house, some are still at the old one. If someone needs a specific box that already went to the new house, you tell them: *"Go check the new house for this one thing — but don't officially think of me as living there yet, because the move isn't complete."*

```
MOVED  →  Migration is DONE.    Update your cache. Go there permanently.
ASK    →  Migration in PROGRESS. Don't update cache. Go there just this once.
```

---

## What About Dumb Clients?

Not all Redis clients are "cluster-aware." Some simpler clients don't understand MOVED or ASK at all. In that case, Redis Cluster can still work — but you'd need a **proxy layer** in front of the cluster that handles all the routing logic on behalf of dumb clients. This is a valid architectural pattern too, and worth mentioning in an interview as a tradeoff between client complexity and infrastructure complexity.

---

## The Full Request Flow — Putting It Together

Let's walk through the complete lifecycle of a single request so everything clicks as one coherent picture.

Your application wants to `GET user:1234`. Here's exactly what happens:

The client computes `CRC16("user:1234") % 16384 = slot 15394`. It checks its local cache and sees slot 15394 belongs to Node-C. It sends the request directly to Node-C. If Node-C still owns that slot and the key is there, it responds immediately — this is the fast, happy path and it's what happens the vast majority of the time.

If Node-C has already fully migrated slot 15394 to Node-D, it responds with `MOVED 15394 Node-D`. The client updates its cache, retries on Node-D, and succeeds. From this point forward, all requests for slot 15394 go straight to Node-D.

If slot 15394 is currently mid-migration and this particular key has already moved, Node-C responds with `ASK 15394 Node-D`. The client sends `ASKING` to Node-D, retries the command, and gets its answer — but doesn't update its cache yet.

The beauty of this system is that **the cluster is always self-correcting**. Even if a client has a completely stale slot map, it will always eventually reach the right node through at most one redirect. It might take two hops the first time, but after that it's corrected and back to one hop. In a well-functioning cluster, redirections are rare events, not the normal path.

---

Before we move forward, here's a good question to sit with: *if every node already knows the full slot map, why doesn't Redis just have the wrong node transparently proxy the request to the right node without telling the client?* It would feel seamless to the application. Why does Redis choose to expose the MOVED error instead?

Think about it for a moment — the answer reveals something important about Redis's design philosophy. When you have a thought on it, let me know and we'll discuss before moving to the next topic: **replication and what happens when a node actually fails**. 🚀

---

# Chapter 6: Replication and Handling Node Failures

But first — the question I left you with. *Why does Redis return a MOVED error to the client instead of just silently proxying the request to the correct node?*

The answer comes down to **latency and honesty**. If Node-C silently proxied your request to Node-D, you'd get your answer, but two things would go wrong. First, you'd be paying for an extra network hop on *every single request* until the client figures out the correct node on its own — and it never would, because it's never told. Second, Node-C becomes a hidden bottleneck, absorbing traffic that isn't even meant for it. Redis's philosophy is to be *transparent and explicit* — tell the client the truth immediately so it can correct itself and be faster forever after. One redirect, then permanent efficiency. That's a much better tradeoff than comfortable ignorance with a permanent hidden tax.

Alright, now let's talk about what happens when things actually break.

---

## The Problem: What If a Node Dies?

Everything we've discussed so far assumes all your nodes are healthy and alive. But in a real production system running on real hardware across a real network, nodes *will* die. Disks fail. Networks partition. Processes crash. A Redis Cluster that can't survive node failures would be useless in production, no matter how elegant its sharding scheme is.

So Redis Cluster is built with replication baked in from the start. Every master node that owns slots can — and should — have one or more **replica nodes** that mirror its data exactly.

---

## Master and Replica — The Basic Relationship

When you set up Redis Cluster in production, you typically configure it with at least 3 master nodes and 3 replica nodes (one replica per master). The layout looks like this:

```
Master-A  (owns slots 0–5460)       ←→  Replica-A
Master-B  (owns slots 5461–10922)   ←→  Replica-B
Master-C  (owns slots 10923–16383)  ←→  Replica-C
```

The replica's only job during normal operation is to stay in sync with its master. Every write that goes to Master-A is asynchronously replicated to Replica-A. The replica sits quietly in the background, up-to-date, ready, but not serving any client write requests. This is called **asynchronous replication**, and that word *asynchronous* is extremely important — we'll come back to it because it has real consequences.

By default, clients only read from and write to masters. Replicas just shadow their master silently. (You can optionally enable reading from replicas for read-heavy workloads, but with a caveat that the data might be slightly stale — something worth mentioning in an interview.)

---

## When a Master Dies — The Failover Process

Say Master-B suddenly dies. It owns slots 5461–10922, which means a significant chunk of your keyspace just became unreachable. Redis Cluster needs to promote Replica-B to take over. Here's how that process unfolds, step by step.

**Step 1 — Detecting the failure.** Redis Cluster uses a gossip protocol where every node constantly sends `PING` messages to a random subset of other nodes and expects `PONG` responses back. When a node stops responding to pings, other nodes mark it as `PFAIL` — *Possible Failure*. It's possible because network hiccups can cause missed pings even when a node is healthy.

**Step 2 — Reaching consensus.** A single node's suspicion isn't enough to trigger a failover — that would be too fragile. Instead, the cluster waits until a *majority* of master nodes have independently flagged Master-B as unreachable. Only once that quorum is reached does the status escalate from `PFAIL` to `FAIL`. This majority requirement is critical because it prevents what's called a **split-brain** scenario, where two parts of a partitioned network each think the other side's nodes are dead and both try to take over simultaneously.

**Step 3 — The replica campaigns.** Once Master-B is declared `FAIL`, Replica-B knows it's time to step up. It starts a **leader election** by broadcasting a message to all master nodes saying *"I want to become the new master for slots 5461–10922, please vote for me."* The other master nodes each get one vote and will grant it as long as they haven't already voted for someone else in this election epoch.

**Step 4 — Promotion.** Once Replica-B collects votes from a majority of masters, it promotes itself. It announces to the entire cluster: *"I am now the master for slots 5461–10922."* Every node updates its slot map. Every client that was getting MOVED errors for those slots now gets redirected to the new master. The cluster heals itself, completely automatically, typically within a few seconds.

---

## The Uncomfortable Truth — Asynchronous Replication Means Possible Data Loss

Now here's the part that most tutorials gloss over, but that you absolutely must understand for interviews — and more importantly, for building real systems.

Because replication is **asynchronous**, there is always a small window where Master-B has accepted writes that haven't yet been copied to Replica-B. Think of it this way: a client writes a key to Master-B, Master-B confirms success immediately, and then starts the process of sending that write to Replica-B in the background. If Master-B crashes in that window — even a window of just a few milliseconds — those writes are gone. When Replica-B takes over, it simply doesn't have them.

This is not a bug. It's a deliberate design decision. If Redis Cluster waited for Replica-B to confirm every write before acknowledging to the client, every single write operation would be dramatically slower (you'd be waiting for a network round trip to the replica on every write). For a cache, that tradeoff is almost always wrong. You accept a tiny chance of losing very recent writes in exchange for dramatically better write performance.

This means Redis Cluster is not suitable as your **primary source of truth** for data you can't afford to lose. It's designed for workloads like caching, session storage, and leaderboards — where losing a handful of the most recent writes during a failover is acceptable. If you need strong durability guarantees, you'd add Redis persistence (RDB/AOF) on top, or use a different tool entirely for that data.

In an interview, if someone asks *"can you lose data in Redis Cluster?"*, the correct answer is: *"Yes, in a failover scenario, you can lose writes that were acknowledged by a master but not yet replicated. The window is small but real, and it's a conscious tradeoff for write performance."* That answer shows you understand the system deeply.

---

## What If There's No Replica Available?

There's one more scenario worth understanding. Say you're running a lean setup and Master-B dies but there's no Replica-B — maybe it died earlier and you hadn't replaced it yet. Now slots 5461–10922 are permanently unavailable. There's no one to promote.

By default, Redis Cluster will **stop accepting commands entirely** for those slots, because serving partial data could cause application-level inconsistencies. The cluster is partially down. This is called a **cluster down** state for the affected slots.

You can change this behavior with a configuration flag called `cluster-allow-reads-when-down`, which lets the cluster keep serving stale reads even if some slots are unavailable — but this is a deliberate, opt-in choice because it means serving potentially incorrect data.

---

## The Mental Model for Replication

The way to think about Redis Cluster replication is as an **insurance policy with a small deductible**. The insurance (replica) protects you from total loss when something goes wrong. But the deductible (asynchronous replication lag) means you might lose a tiny bit right before the disaster. You're not fully protected against every possible loss — you're protected against catastrophic, total unavailability, which is the thing that matters most for availability in a caching layer.

---

Here's something to think about before we move on: we've now established that Redis Cluster requires a **majority of master nodes** to be alive and agreeing for things to work correctly — for FAIL detection, for replica elections. This raises an interesting question: *why is the minimum recommended production setup 3 master nodes and not 2?* What specifically breaks if you only have 2 masters?

Sit with that for a moment. When you're ready, we'll move into the final chapter that ties everything together: **what makes a good Redis Cluster topology**, and how you'd actually discuss all of this in an HLD interview with confidence. 🚀

---

# Chapter 7: Cluster Topology, the Quorum Question, and Nailing the HLD Interview

Let's start with the question I left you with, because the answer unlocks something fundamental about how distributed systems think about consensus.

---

## Why 3 Masters and Not 2?

The answer is pure mathematics, and once you see it, you'll never forget it.

For any decision in Redis Cluster — declaring a node as `FAIL`, electing a new master — the cluster requires agreement from a **strict majority** of master nodes. Strict majority means *more than half*. The reason for requiring majority rather than, say, any single node's opinion, is to prevent split-brain: a situation where the network splits into two isolated halves and both halves try to independently make decisions, resulting in chaos.

Now watch what happens with 2 masters. Majority of 2 is... 2. You need both nodes to agree. The moment one master dies, you have only 1 out of 2 — that's not a majority. The surviving master cannot declare the dead one as FAIL, cannot hold an election, cannot promote any replica. The cluster is completely paralyzed, even though one perfectly healthy master is still sitting there. You've essentially built a system where any single node failure causes total unavailability. That's worse than no clustering at all.

With 3 masters, majority is 2. One master dies, the remaining two can still agree, reach quorum, declare the failure, and elect a new master. The cluster heals. This is why 3 is the hard minimum — it's the smallest number where you can lose one node and still have enough survivors to form a majority.

This pattern, by the way, shows up everywhere in distributed systems. Zookeeper recommends odd-numbered ensembles (3, 5, 7) for the exact same reason. Raft consensus needs a majority quorum. etcd, which powers Kubernetes, follows the same logic. The underlying principle is always the same: you need `(N/2) + 1` nodes to agree, so with an even number you waste fault tolerance, and with an odd number you maximize it. Going from 2 to 3 masters gains you one fault tolerance. Going from 4 to 5 also gains you one more, but going from 3 to 4 gains you nothing — which is why 4-master setups are unusual. You always want odd numbers.

---

## Production Topology — What It Actually Looks Like

Now let's paint a complete picture of what a real, well-designed Redis Cluster looks like in production, because interviewers love to ask you to sketch this out.

The standard minimum production setup is **3 masters + 3 replicas**, with a very important constraint: each replica should live in a **different physical availability zone** than its master. If you're on AWS, for example, you'd spread across us-east-1a, us-east-1b, and us-east-1c like this:

```
Zone A               Zone B               Zone C
--------             --------             --------
Master-A             Master-B             Master-C
(slots 0–5460)       (slots 5461–10922)   (slots 10923–16383)

Replica-C            Replica-A            Replica-B
(mirrors Master-C)   (mirrors Master-A)   (mirrors Master-B)
```

Notice the cross-zone placement of replicas. Replica-A is in Zone B, not Zone A. This is deliberate. If an entire availability zone goes down — which does happen in cloud environments — you don't lose both a master and its only replica simultaneously. If Zone A dies, Master-A is gone but Replica-A in Zone B is perfectly healthy and can take over. The cluster survives a full zone outage.

For larger systems, you'd scale to 6 masters + 6 replicas, or more, while maintaining this cross-zone principle. The slot ranges just get divided more finely.

---

## Putting the Whole Story Together

You now have all the pieces. Let me connect them into one coherent narrative, because in an HLD interview this is exactly the story you'd tell — not as a list of facts, but as a logical progression of ideas.

You start with the fundamental constraint: Redis lives in RAM, and RAM on one machine is finite. So you must shard. The naive approach is modulo hashing — hash the key, mod by the number of nodes — but this causes catastrophic reshuffling every time the cluster size changes, because the denominator of your modulo changes and almost every key remaps.

Consistent hashing solved the reshuffling problem beautifully by using a ring and only disrupting `1/N` of keys when a node joins or leaves. But Redis chose not to use it, because the ring is operationally opaque — it's hard to look at a running cluster and immediately understand exactly which keys live where, and virtual nodes add coordination complexity during membership changes.

Redis instead invented hash slots — a fixed space of 16,384 slots, each key assigned to a slot via `CRC16(key) % 16384`, and slots explicitly assigned to nodes in contiguous ranges. This makes the key-to-node mapping completely transparent and deterministic. Rebalancing means explicitly reassigning slot ranges and migrating only the keys in those slots, with the cluster staying live throughout.

Clients maintain a local slot map and go directly to the right node for every request. When the map is stale, nodes return MOVED errors (for completed migrations) or ASK errors (for in-progress migrations), allowing clients to self-correct. MOVED updates the client's cache permanently; ASK is a one-time detour that deliberately doesn't update the cache.

Every master has a replica for fault tolerance. The cluster uses a gossip protocol and majority voting to detect failures and promote replicas automatically. Replication is asynchronous, which means a small window of data loss is possible during failover — a conscious tradeoff for write performance. And the minimum of 3 masters exists precisely because majority consensus requires at least `(N/2) + 1` surviving nodes, making 3 the smallest number where you can tolerate one failure.

---

## The Interview Cheat Sheet — Questions You Should Now Be Able to Answer Cold

Rather than a list, think of these as a conversation you could have confidently with an interviewer.

If they ask *"how does Redis shard data?"*, you explain hash slots, the fixed 16,384 slot space, CRC16 hashing, and how slots map to nodes — and you contrast it with consistent hashing to show you understand why Redis made that choice.

If they ask *"what happens when you scale the cluster?"*, you explain slot migration: slots are reassigned from existing nodes to the new node, only keys in those slots move, the cluster stays live, and clients get MOVED errors if they had a stale map, which they use to self-correct.

If they ask *"how does Redis handle node failures?"*, you walk through the gossip protocol, PFAIL vs FAIL, the replica election process, and you proactively mention the asynchronous replication caveat and what it means for data loss.

If they ask *"can you lose data in Redis Cluster?"*, you give the nuanced answer: yes, during a failover, writes acknowledged by a master but not yet replicated can be lost. It's a deliberate tradeoff for write performance, and it means Redis Cluster is appropriate for caching and session storage but not as the sole store for data that cannot be lost.

If they push further and ask *"how would you minimize that data loss?"*, you talk about Redis persistence options — RDB snapshots and AOF (Append-Only File) logging — and the `WAIT` command, which lets a specific write block until it's confirmed on N replicas, giving you synchronous replication on demand for critical writes.

---

## One Last Thing Worth Knowing — What Redis Cluster Deliberately Doesn't Support

Knowing the limitations of a system is just as impressive as knowing its strengths, because it shows you think like an engineer and not just a memorizer.

Redis Cluster does not support multi-key commands across different slots. If `"user:1234:profile"` is on Node-A and `"product:99"` is on Node-C, you cannot run a single `MGET` on both of them. This is why hash tags exist — by designing your keys with `{}` to force co-location, you can make related keys share a slot and enable multi-key operations. In an interview, if you're designing a system that needs atomic operations across multiple keys, mentioning hash tags as the solution shows real practical knowledge.

---

You've now built up the complete mental model from the very first principles — from the pain of a single overloaded server, all the way through modulo hashing, consistent hashing, hash slots, client routing, replication, failover, and cluster topology. Every concept connects to the one before it. That's exactly the kind of thinking that makes an HLD interview answer memorable. 🚀
