# Scoping "Design Twitter" for L4/L5 HLD

Twitter is huge in scope, so the whole interview hinges on picking the right 2-4 features to go deep on. Here's what I'd propose:

## Core Requirements (P0/P1)

1. **Post a tweet** (text, ~280 chars, optionally media) — the basic write path.
2. **Follow / unfollow a user** — defines the social graph.
3. **View home timeline** — a feed of tweets from people you follow, reverse-chronological (or ranked, but let's start chronological). **This is the single most interesting problem in the whole system** — it's where fan-out, caching, and celebrity-user edge cases live.
4. **View a user's profile timeline** — all tweets by one user.

These four alone are enough to generate almost every hard distributed-systems question an interviewer wants to ask: fan-out-on-write vs fan-out-on-read, hot users, caching, sharding by user, eventual consistency.

## What I'm explicitly cutting (P2 / out of scope)

- **Likes, retweets, replies/threads** — these add data modeling complexity but don't change the *core* architecture once you've solved timelines. I can bolt these on briefly at the end if you want.
- **Search** — this is basically "design a search engine" (inverted indices, Elasticsearch-like systems) and deserves its own separate design session.
- **Notifications** — a pub/sub fan-out problem, related but distinct enough to be its own story.
- **Trending topics / hashtags** — a streaming/counting problem (think heavy hitters, Count-Min Sketch), also a separate deep dive.
- **DMs** — basically a separate chat-system design.
- **Ranking algorithm for the feed** (ML-based relevance ranking, ad injection) — I'll assume simple reverse-chronological. Ranking is a whole ML-systems conversation, not a storage/distribution one.
- **Media storage/CDN details for images/video** — I'll wave at "store in blob storage + CDN" but not go deep on video transcoding pipelines, since that's more media-infra than social-graph HLD.
- **Admin/moderation/analytics dashboards** — no architectural impact.

The reason this cut list matters: almost every "Design Twitter" interview failure I've seen comes from candidates trying to cover likes+retweets+search+notifications+trending all at once and running out of time before ever justifying the timeline architecture — which is the part interviewers actually care about.

---

Does this scope work for you? Once you confirm, we'll start at Day 0: the dumbest possible single-server version that handles posting, following, and reading a timeline.

---

## Day 0: The Dumbest Thing That Could Work

**Architecture:**

```
[Client] --> [Single App Server] --> [Single PostgreSQL/MySQL DB]
```

That's it. One server running a monolith app (handles tweet posting, follow/unfollow, timeline reads), one relational database. No cache, no queue, no replicas.

**Data model (just enough to function):**

```sql
users(id, username, ...)

follows(follower_id, followee_id, created_at)
  -- PK (follower_id, followee_id)
  -- index on followee_id too, we'll see why soon

tweets(id, author_id, text, created_at)
  -- index on (author_id, created_at) for profile timeline
```

**API surface:**

- `POST /tweet {text}` → insert into `tweets`
- `POST /follow {followee_id}` → insert into `follows`
- `GET /timeline/home` → the interesting one:

```sql
SELECT t.* FROM tweets t
JOIN follows f ON t.author_id = f.followee_id
WHERE f.follower_id = :me
ORDER BY t.created_at DESC
LIMIT 20
```

- `GET /timeline/user/:id` → `SELECT * FROM tweets WHERE author_id = :id ORDER BY created_at DESC LIMIT 20`

**Why this is a reasonable starting point:**

This isn't a strawman — this is genuinely how an MVP / hackathon version of Twitter would work, and it's *correct*. It's strongly consistent (you follow someone, refresh, you see their tweets — no lag), it's simple to reason about, and for a small number of users (say, under 10K) this JOIN query is fast because indexes make it cheap and the working set fits in memory/cache at the DB layer anyway.

The reason we start here isn't "because it's naive" — it's because **it establishes the baseline correctness model**. Every later iteration is a deliberate trade-off *away* from this simplicity, and you need to be able to name exactly what you're trading away. Interviewers love when you say "we're giving up X consistency guarantee that Day 0 had, in exchange for Y" rather than just piling on infrastructure because it sounds impressive.

**Quick capacity gut-check even at this stage:**
Twitter-scale numbers (I'll use these throughout): ~250M daily active users, ~500M tweets/day → ~6,000 tweets/sec average, but home timeline *reads* dominate — read:write ratio is typically 100:1 to 1000:1 for social feeds. So reads, not writes, will be our first bottleneck to break.

---

**Likely follow-up questions at this stage:**

1. **"Why relational DB and not NoSQL from the start?"**
   → Answer: At Day 0 scale, relational gives us JOINs (which we need for the fan-in timeline query), transactions, and strong consistency for free. NoSQL's benefits (horizontal write scaling, schema flexibility) aren't needed yet — introducing them now would be solving a problem we don't have. Good HLD is about deferring complexity until data forces your hand.

2. **"What breaks first as we scale — the JOIN query or something else?"**
   → Answer: The home timeline JOIN. It requires scanning `follows` for potentially thousands of followee_ids, then hitting `tweets` for each — this get progressively worse as follow-counts and tweet-density grow, and it can't be helped much by simple indexing once we're distributed across shards (JOINs across shards are expensive/impossible). This is the thread we'll pull on next.

Got it? Say "next" and we'll break Day 0 with concrete numbers on the timeline read.

---

## Breaking Day 0: The Home Timeline Read Problem

**The concrete scenario:**

Let's put numbers on it. Say we're at "medium Twitter" scale: 50M users, average user follows 200 people, average user posts 2 tweets/day.

Every time *any* user opens the app (and they do this constantly — Twitter users refresh their timeline dozens of times a day), we run:

```sql
SELECT t.* FROM tweets t
JOIN follows f ON t.author_id = f.followee_id
WHERE f.follower_id = :me
ORDER BY t.created_at DESC
LIMIT 20
```

For a user following 200 people, this query has to:
1. Look up 200 followee_ids from `follows`
2. Find recent tweets from *each* of those 200 authors in `tweets`
3. Merge-sort all of them by time, take top 20

**Where this falls over — two separate failure modes:**

**1. Read QPS overload.** If we have 50M DAU each opening the app ~10x/day and each open triggers a timeline fetch, that's 500M timeline reads/day ≈ 5,800 QPS average, but social traffic is bursty — peak can be 5-10x average, so realistically **30,000-50,000 QPS** of this expensive JOIN hitting a single DB. A single Postgres instance tops out at roughly a few thousand QPS for queries this complex (200-way fan-in join), even with good indexes, because each query touches many rows across many index lookups, not one cache-friendly page.

**2. The "celebrity problem" makes it worse per-query, not just in aggregate.** If even a small fraction of the 200 people you follow include a celebrity with 50M followers who tweets often, your timeline query still has to fan in from all 200 authors on *every single read* — there's no way to know in advance which of your followees are "quiet" vs "prolific" without doing the work.

**3. Vertical scaling hits a wall.** Our first instinct might be "bigger DB server." But even the largest single Postgres/MySQL instance (say, 128 cores, 1TB+ RAM) caps out — you cannot buy your way past ~tens of thousands of complex QPS on one machine. And single-server means single point of failure: if that box dies, the *entire product* is down. At 50M DAU that's an unacceptable blast radius.

**The core tension to name explicitly:** this isn't really a "the DB is slow" problem — it's an architectural mismatch. We're doing an **expensive fan-in computation on every read**, when actually most of that computation (who-follows-whom, who-tweeted-what) is highly cacheable/precomputable, because tweets don't change after they're posted and follow graphs change slowly relative to how often people read their timeline.

That reframe — "we're recomputing something on every read that we could precompute once on write" — is exactly the insight that leads to the next iteration.

---

**Likely follow-up questions at this stage:**

1. **"Why not just add a read replica or two instead of redesigning?"**
   → Answer: Read replicas *do* help and we should still add them (cheap win, buys us time) — but they don't fix the fundamental issue that each query is O(followees) expensive. Replicas scale QPS linearly-ish but the celebrity/fan-in cost per query stays the same, and replication lag introduces staleness we'd need to handle anyway. It's a stopgap, not a solution — good to mention you'd do it in parallel, but it doesn't address root cause.

2. **"Why not just cache the query result?"**
   → Answer: Good instinct, but naive result-caching (cache the whole timeline response) invalidates constantly — anyone I follow tweeting invalidates my cached timeline, and with 200 followees that's frequent. We need something smarter than "cache the read" — we need to think about *precomputing on write*. That's the fan-out-on-write idea, coming next.

Got it? Say "next" for the fan-out-on-write iteration.

---

## Iteration 1: Fan-out on Write (Precomputed Timelines)

**The core idea:** Instead of computing the timeline at read time by joining across everyone you follow, precompute it at write time. When a user posts a tweet, we immediately push (fan out) that tweet into the precomputed timeline of every one of their followers.

**New data model — a per-user timeline cache:**

Think of it conceptually as:

```
timeline:{user_id} -> [tweet_id_1, tweet_id_2, tweet_id_3, ...]  (sorted by time, capped at ~800 recent)
```

This is a perfect fit for a **Redis sorted set** (or similar) — score = timestamp, member = tweet_id. Not the source of truth (that's still `tweets` in the DB — Redis is a derived cache), but it's what we read from on the hot path.

**New write path:**

```
POST /tweet {text}
  1. Insert into tweets table (source of truth, unchanged)
  2. Look up follower_ids of author (from follows table)
  3. For each follower_id: push tweet_id onto timeline:{follower_id} in Redis
```

**New read path — dramatically simpler:**

```
GET /timeline/home
  1. ZRANGE timeline:{me} 0 20   (Redis, O(log N))
  2. Batch-fetch those 20 tweet bodies (from a tweet cache, or DB by ID — cheap, it's a primary key lookup)
```

This read is now a single Redis lookup instead of a 200-way fan-in JOIN. That's the difference between a query that costs "touch 200 index ranges" and one that costs "read one sorted list." At our scale (30-50K peak QPS of reads), Redis handles this trivially — a single well-provisioned Redis cluster does 100K+ ops/sec per node.

**What we gained:**
- Reads are now O(1)-ish and *cheap*, regardless of how many people you follow. This matters enormously since reads outnumber writes 100-1000:1 — we've moved the expensive work to the rare side (writes) from the frequent side (reads).
- Read latency becomes predictable and low (sub-10ms), which is what actually matters for user-perceived app performance.

**What we gave up / new problems introduced:**

1. **Write amplification.** Posting one tweet is no longer one DB insert — it's now "1 insert + N pushes," where N = follower count. For a normal user (few hundred followers) this is fine. But for a celebrity with 50M followers, one tweet now triggers **50M writes**. This is the infamous "celebrity problem," and it's severe enough that fan-out synchronously in the request path would make posting take forever (or fail) for high-follower accounts.

2. **Consistency became eventual, not strong.** Day 0 was strongly consistent — post a tweet, it's immediately visible to your followers on their next JOIN query. Now there's a window where the tweet exists in `tweets` but hasn't yet been fanned out to all followers' timeline caches. We've explicitly traded consistency for read latency — worth saying out loud in an interview, because CAP-type trade-offs are exactly what they're listening for.

3. **Storage cost increased.** We're duplicating tweet_ids across potentially millions of follower timeline lists instead of storing tweet data once. This is a deliberate space-for-time trade.

4. **Fan-out needs to be async**, not part of the synchronous request/response cycle — otherwise a celebrity's tweet-post API call would hang for the time it takes to do millions of writes. This means we now need a **message queue** (Kafka is the standard answer) between "tweet posted" and "fan-out worker pushes into follower timelines," and a pool of fan-out worker processes consuming from it.

Updated write path:

```
POST /tweet → insert into tweets DB → publish event to Kafka topic → return 200 to user immediately
                                              |
                                    [Fan-out worker pool] consumes event,
                                    pushes tweet_id into each follower's
                                    Redis timeline list
```

This is why "post successful" and "visible in all followers' feeds" are no longer the same instant — and that's an acceptable, deliberate trade for this system (unlike, say, a banking transaction, where eventual consistency would be unacceptable).

---

**Likely follow-up questions at this stage:**

1. **"What alternative did you reject here, and why?"**
   → We could've stuck with fan-out-on-read (Day 0's approach) and just thrown a bigger cache/more replicas at it. Rejected because it doesn't fix the fundamental O(followees) cost per read, and reads vastly outnumber writes — optimizing the rare operation (write) at the expense of the frequent one (read) is the wrong lever to pull at this traffic shape.

2. **"What happens to a user who posts before their followers have any cached timeline (new user, empty cache)?"**
   → We handle cache misses by falling back to Day 0's on-the-fly JOIN query, then populate the cache. This is a classic cache-fill-on-miss pattern — good to mention as it shows you're not treating the DB as vestigial, it's still the source of truth and fallback.

3. **"How do you bound the size of each user's timeline list so Redis doesn't grow unbounded?"**
   → Cap each ZSET at ~800 entries (`ZREMRANGEBYRANK` trims the tail) — nobody scrolls back further than that in practice, and older tweets can always be served from a "load more" path that falls back to the DB.

This celebrity write-amplification problem is the big one left — that's genuinely what makes Twitter's real design famous (their actual "hybrid" approach). Got it? Say "next" and we'll fix the celebrity fan-out problem specifically.

---

## Iteration 2: The Celebrity Problem → Hybrid Fan-out

**The concrete failure case:**

A celebrity account with 50M followers tweets. Fan-out means 50M writes into Redis timeline lists. Even with async workers pulling from Kafka, this is brutal:

- At even a generous 100K writes/sec across a large worker pool, 50M writes takes **500 seconds (~8 minutes)** just for that one tweet to fully propagate.
- Worse: celebrities often tweet in bursts, or multiple celebrities tweet around the same event (e.g., during a live sports game) — worker pools get backed up, fan-out lag balloons, and now *regular* users' tweets queued behind celebrity fan-out jobs get delayed too, since they likely share the same queue/worker pool.
- There's also pure waste: many of a celebrity's 50M followers are inactive/never log in that day. We just did 40M+ writes that will be read zero times before falling out of the 800-item cap.

**The fix: Hybrid fan-out, branching on follower count.**

Split users into two classes:

- **Normal users** (below some threshold, say <10K followers — call this the "celebrity threshold"): fan-out-on-write, exactly as before. Fast, cheap, and it's the vast majority of users.
- **Celebrities** (above the threshold): **don't fan out at all on write.** Their tweets just sit in the `tweets` table as normal.

**New read path — merge at read time, but only for celebrities you follow:**

```
GET /timeline/home
  1. ZRANGE timeline:{me} 0 20         (precomputed part: normal follows)
  2. For each celebrity I follow: fetch their recent tweets directly
     (small, bounded set — I follow maybe a handful of celebrities, not thousands)
  3. Merge-sort the two lists by timestamp, take top 20
```

Note the asymmetry: step 2 is cheap because while a celebrity has millions of *followers*, any individual *user* follows only a small number of celebrities (bounded — nobody follows 10,000 celebrities). So we've reduced "50M writes on tweet" to "a few extra cheap reads, merged in, per timeline fetch" — moving the cost back to read time, but only for the tiny slice of accounts where write-fan-out was pathological.

**What we gained:**
- Celebrity tweet-posting is now fast (single DB insert, no fan-out storm).
- No more queue backlog poisoning regular users' fan-out.
- We still get the fast-read benefit of precomputed timelines for the 99%+ of follows that are normal accounts.

**What we gave up / new problems:**

1. **Read path is slightly more complex again** — it's now a merge of "cached precomputed list" + "live fetch for celebrities I follow," not a single Redis lookup. Small latency cost, but bounded and acceptable (we went from O(all followees) to O(celebrities followed), which is a tiny constant in practice — maybe 0-20 people).

2. **We need a threshold and a way to track "is this account a celebrity"** — e.g., a `is_celebrity` flag on the user row, recomputed periodically (daily batch job) based on follower count crossing some threshold. This introduces a small operational concern: the threshold is a tuning knob, and an account crossing it (going viral) needs a migration — e.g., a previously-normal user gaining 500K followers overnight needs to flip to celebrity mode, and probably needs a backfill/cleanup of previously fanned-out data (or just let it decay/expire naturally since we cap timeline lists anyway).

3. **This is explicitly a heuristic, not a clean abstraction** — worth saying in the interview that real systems (this is literally what Twitter does, and what's discussed in their public engineering blog posts) accept this kind of "special-case the outliers" approach because a small number of accounts (celebrities) contribute a wildly disproportionate share of total system cost. This is a classic power-law / Pareto distribution problem, and hybrid approaches like this are the standard answer.

---

**Likely follow-up questions at this stage:**

1. **"Why not just rate-limit or delay celebrity fan-out instead of skipping it?"**
   → Delaying still means doing 50M writes eventually — doesn't reduce total work, just spreads it out, and you still need read-time fallback for the delay window anyway (so you might as well just always read celebrities at read-time and skip the write cost entirely).

2. **"What threshold would you pick for 'celebrity,' and how would you decide?"**
   → No single right number — reason about it out loud: pick a threshold where write-fan-out cost (threshold × avg tweets/day) stays within your worker pool's comfortable throughput, e.g., if a worker pool comfortably handles 10K writes/tweet, set celebrity threshold around there. Mention this would be empirically tuned, not hardcoded from first principles.

3. **"What if I follow 500 celebrities — doesn't your 'cheap because bounded' claim break down?"**
   → Fair edge case — cap it: even for the celebrity-merge step, only merge in from, say, the top 20-50 most recently active celebrities you follow (or paginate/degrade gracefully). This is a good moment to show you think about the tail of your own tail cases.

This hybrid model is basically the crux of the "impressive" answer for this system — most of what's left is rounding it out with sharding, replication, multi-region, and failure handling. Got it? Say "next" for sharding/partitioning the underlying data stores.

---

## Iteration 3: Sharding the Underlying Data Stores

We've fixed the read/write pattern conceptually, but we still have a single Postgres instance and a single Redis instance sitting behind all of this. At 50M+ users and 500M tweets/day, neither survives on one box — not for QPS reasons alone now, but for **data size**. Let's break this concretely and fix it.

**Where single-node storage breaks:**

- `tweets` table: 500M tweets/day × 365 days × a few years of retention = tens of billions of rows. Even at ~300 bytes/row that's multiple TB — won't comfortably fit on one machine's disk/memory for fast access, and a single primary can't absorb 6K+ writes/sec plus all the celebrity-read traffic forever.
- `follows` table: 50M users × ~200 average follows = 10B edges. Big, but more importantly it's *read very hot* (every fan-out touches it).
- Redis timeline cache: 50M users × 800 tweet_ids × ~8 bytes = ~320GB just for the sorted sets — feasible on one big Redis node today, but doesn't leave room to grow and is a single point of failure for the entire read path.

**Fix: partition each store by a sensible key.**

**1. `tweets` table → shard by `author_id` (or a hash of it).**
Why author_id and not tweet_id or time? Because our two main tweet-lookup patterns are "get this user's tweets" (profile timeline) and "get this tweet by ID" (timeline hydration) — sharding by author_id keeps a user's full tweet history co-located on one shard, making profile-timeline reads a single-shard query. Tweet-by-ID lookups need a way to route to the right shard — we solve this with a **globally unique, sortable tweet ID that embeds shard info**, e.g., Twitter's real-world approach: Snowflake IDs (64-bit: timestamp + shard/worker ID + sequence number). This means from the ID alone you can extract which shard to query, no separate lookup needed. This is worth naming explicitly — it's a nice trick interviewers like seeing candidates know.

**2. `follows` table → shard by `follower_id`.**
Why follower_id, not followee_id? Because our hottest query on this table is "give me all the people X follows" (used during fan-out to find who to push to... wait, actually fan-out needs the reverse: "who follows this author"). Let's be precise here — we actually need **both directions**:
- Fan-out (on tweet post): "who follows author X" → needs index/shard by `followee_id`.
- "who do I follow" (used less often, e.g. displaying your following list): needs by `follower_id`.

Real systems solve this by maintaining **two denormalized copies** of the edge — a `followers` table sharded by followee_id, and a `following` table sharded by follower_id — trading storage duplication for the ability to shard each one optimally for its dominant access pattern. This is a good concrete example of "denormalize for read performance" that's very interview-relevant.

**3. Redis timeline cache → shard by `user_id` (consistent hashing across Redis nodes).**
Each user's `timeline:{user_id}` ZSET lives on one node determined by consistent hashing. This also naturally distributes load and lets us scale by adding nodes without a full rehash of everything (consistent hashing minimizes reshuffling — only ~1/N of keys move when you add the Nth node, versus a naive `hash % N` which reshuffles almost everything).

**What we gained:**
- Each store now horizontally scales — add shards as data/QPS grows, no more single-machine ceiling.
- Fan-out writes are distributed — no single Redis node absorbs all the traffic.

**What we gave up / new problems:**
1. **Cross-shard queries become hard or impossible.** Day 0's elegant JOIN is now fully dead — you cannot efficiently JOIN across shards in a distributed relational setup. This is fine because we already killed that query path with fan-out-on-write, but it's worth stating explicitly: sharding is *why* the JOIN-based Day 0 approach fundamentally cannot scale, not just "it's slow."
2. **Resharding/rebalancing is now an operational concern** — as shards grow unevenly (a shard with more celebrity accounts gets hotter), we need consistent hashing (for Redis) and/or a shard-splitting strategy (for the DB) to rebalance.
3. **We need a routing layer** — a shard map / directory service (or the app servers embed shard-routing logic, e.g., "author_id % 4096 → shard N") so any node can find the right shard for a given key.

---

**Likely follow-up questions at this stage:**

1. **"Why not shard by tweet_id (random/hash) instead of author_id?"**
   → We'd lose locality for profile-timeline reads (fetching one user's tweets would fan out across all shards). Author_id sharding trades "slightly uneven shard sizes due to celebrity users" for "profile reads stay single-shard," which is the right trade since profile-timeline reads are frequent and celebrity-skew can be handled separately (that hot shard could even get extra replicas).

2. **"How do you handle a celebrity's shard becoming a hotspot?"**
   → Give hot shards extra read replicas, or in extreme cases isolate very large accounts onto their own dedicated shard — acknowledging that perfectly even sharding is impossible under a power-law distribution, so you build in per-shard elasticity rather than assuming uniform load.

Got it? Say "next" for replication, consistency model, and failure handling (what happens when a shard/replica dies).

---

## Iteration 4: Replication, Consistency, and Failure Handling

We've sharded for scale, but each shard as described so far is still a single node — a single point of failure. If a `tweets` shard dies, every user whose data lives there loses read/write access. At 50M+ users this is not acceptable. Let's fix availability.

**The fix: replicate each shard.**

**For the `tweets` and `follows` DB shards:**
Each shard becomes a **primary + 2 replicas** (standard leader-follower replication), likely spread across different availability zones. Writes go to the primary; reads can be served from replicas.

- Replication is **asynchronous** here (primary acks the write, then streams to replicas in the background) — because synchronous replication would mean every tweet-post waits on multiple network round-trips before returning success, hurting write latency for a system where losing a few seconds of a single tweet on primary failure is an acceptable risk (unlike, say, a payments ledger).
- This means there's a small **replication lag window** (typically milliseconds, but nonzero) — if the primary dies right after acking a write but before replicating it, that write can be lost. We accept this for tweets (worst case: rare tweet loss, user just reposts) but it's worth explicitly naming as a trade-off.
- **Failover:** if a primary dies, we promote a replica (via a consensus-based coordinator, e.g., a system like etcd/ZooKeeper-backed leader election, or a managed cloud DB's built-in failover). There's a brief write-unavailability window during promotion (seconds), but reads can often continue from surviving replicas.

**For the Redis timeline cache shards:**
Redis itself isn't the source of truth (the DB is), so we treat it differently — if a Redis node dies, we don't need durable failover the same way. We can either:
- Run Redis with its own primary-replica setup (Redis Sentinel/Cluster) for fast automatic failover and minimal cache-cold-start, **or**
- Accept that a dead cache node just means a temporary flood of cache misses that fall back to the DB-driven fan-in query (Day 0's query, still alive as a fallback!) — degraded performance but not an outage.

Most real systems do the former (replicate the cache too) because a fully cold cache after a node death, at our QPS, could overwhelm the DB layer with fallback fan-in queries — so cache replication is really about *protecting the DB*, not just protecting cache-hit latency.

**Consistency model — let's be explicit about CAP here, because interviewers want this named:**

We're choosing **availability + partition tolerance over strong consistency (AP over CP)** for almost every part of this system:
- A tweet might take a moment to propagate to all followers' timelines (fan-out lag).
- A follow/unfollow might take a moment to be reflected everywhere.
- A replica might serve a slightly stale read right after a write.

This is the correct choice *because of the domain*: nobody's timeline being 500ms stale is a real problem, but the whole site being down because we insisted on strict consistency would be a disaster. Contrast this explicitly if asked: a banking system would flip this trade-off (CP over AP) because stale/lost data is unacceptable there even at the cost of availability.

**Failure handling patterns worth naming (even briefly) for L4/L5 credit:**

- **Idempotency:** tweet-post requests should carry a client-generated idempotency key, so retries (from client timeouts) don't create duplicate tweets.
- **Timeouts + retries with backoff:** app servers calling DB/Redis/Kafka should have sane timeouts and exponential backoff, not hang indefinitely.
- **Circuit breakers:** if a shard or the fan-out worker pool is unhealthy, the app layer should fail fast (e.g., serve slightly stale cached timeline) rather than pile up requests against a dying dependency.
- **Kafka durability:** the fan-out event queue itself should be replicated (Kafka does this natively via partition replication) — losing a fan-out event means some followers just miss a tweet in their precomputed timeline until they hit a cache-miss fallback.

**What we gained:** no single node failure takes down the product; the system degrades gracefully (slower/staler) rather than failing hard.

**What we gave up:** more operational complexity (failover coordination, monitoring replica lag), and we've fully committed to eventual consistency as a first-class property of the system, not an incidental side effect.

---

**Likely follow-up questions at this stage:**

1. **"How would you detect and handle replica lag becoming too large?"**
   → Monitor lag (seconds behind primary) as a metric; if a replica falls too far behind, route reads away from it temporarily (health-check-based routing) rather than serving very stale data, and alert if lag persists — this is a routing/observability, not a hard architectural, fix.

2. **"What if Kafka itself is down — does tweeting stop working?"**
   → This is a good pressure-test question. Answer: the tweet write to the `tweets` DB (source of truth) can still succeed even if the Kafka publish fails temporarily — publish should be best-effort with retry/dead-letter-queue, decoupled from the write's success response, so a user's tweet isn't lost even if fan-out is delayed. Good place to mention outbox pattern (write tweet + write "pending fan-out event" in the same DB transaction, separate process reliably drains it into Kafka) if you want to go deep.

Got it? Say "next" and we'll close out with multi-region/globalization and a final capacity summary + full architecture recap.

---

## Iteration 5: Multi-Region + Final Recap

**Why go multi-region at all:**

Two distinct motivations, worth separating explicitly because interviewers probe this:

1. **Latency** — a user in Tokyo hitting a single US-based data center adds 150-200ms round-trip before any processing even starts. For a product where people refresh their feed dozens of times a day, that's a real, felt degradation.
2. **Availability/blast radius** — an entire region going down (AWS us-east-1 outage, natural disaster, etc.) shouldn't take the whole product offline globally.

**Approach: regional deployments with a home-region model.**

- Each user is assigned a **home region** (typically based on signup location) — their primary shard for `tweets`/`follows` lives there.
- App servers and Redis caches are deployed in multiple regions (US, EU, APAC, etc.) — a user's *reads* are served from their nearest region's cache/replica where possible.
- **Writes** (posting a tweet) route to the user's home region's primary — we don't do multi-region active-active writes for the same row, because that reopens hard conflict-resolution problems (concurrent writes to the same data in two regions) for very little benefit here, since a user's own tweet-writes aren't latency-critical in the same way reads are (you post once, you read constantly).
- Regions replicate data cross-region asynchronously (same trade-off as before: async replication, eventual consistency, now with cross-region lag which is larger — tens to low-hundreds of ms — but same acceptable trade-off logic as before).

**The interesting wrinkle: fan-out across regions.** If I'm in Tokyo and I follow someone whose home region is US, my precomputed timeline cache (which might live in an APAC Redis cluster for my own locality) still needs that US user's tweet pushed into it. So the fan-out worker pool needs to be **globally aware** — when User A tweets, fan-out needs to push into follower timeline caches *wherever those followers' regional caches live*, not just within one region. This adds cross-region network calls to the fan-out path, which is exactly why fan-out is async via Kafka in the first place — it absorbs this extra latency without blocking the tweet-post response.

**Data sovereignty** (briefly, since it's a real P1 in modern systems even if not the flashiest topic): some jurisdictions (EU/GDPR) require certain user data to stay within region. This reinforces the home-region model — a user's data lives in their home region by default/compliance requirement, not just for latency.

---

## Final Architecture Recap

```
                         [Global Load Balancer / GeoDNS]
                                    |
              ---------------------------------------------
              |                    |                       |
        [US Region]           [EU Region]            [APAC Region]
              |
      [App Servers - stateless, autoscaled]
              |
      -----------------------------------------------
      |                |                |            |
 [tweets DB       [follows DB     [Redis timeline  [Kafka: 
  shards,          shards,         cache shards,    tweet-posted
  sharded by       sharded by      sharded/hashed   events]
  author_id,       follower_id &   by user_id,          |
  primary+replicas followee_id,    primary+replica]  [Fan-out
  per shard]       primary+replica per shard]          worker pool]
                    per shard]                          (pushes into
                                                      follower timeline
                                                      caches, globally)
```

**Read path:** Client → nearest region → Redis timeline cache (ZRANGE) + live celebrity-tweet merge → hydrate tweet bodies → return.

**Write path:** Client → home region → insert into `tweets` (sharded by author_id) → publish event to Kafka → return 200 → async fan-out workers push tweet_id into followers' Redis caches (skipping celebrities, per Iteration 2) globally.

**Consistency stance:** AP over CP, deliberately, everywhere except the core `tweets`/`follows` writes themselves (those are still transactional/durable at the single-row level — we're eventually consistent about *propagation*, not about losing a write once acknowledged).

---

### The "why not X" arsenal — the questions most likely to actually get asked, all in one place:

- **"Why not just use a graph DB for follows?"** → Could work for graph-traversal-heavy queries (mutual friends, recommendations), but our access patterns are simple key lookups (by follower_id or followee_id), which a sharded relational/KV store handles fine and more simply. Graph DBs shine when you need multi-hop traversal, which basic Twitter timeline serving doesn't.
- **"Why Kafka and not just a simple task queue?"** → Kafka gives us replayability (a fan-out worker crash doesn't lose the event, consumer just resumes from offset), high throughput, and multi-consumer fan-out (e.g., same event feeds fan-out workers AND notification workers AND analytics, without coupling). A simple queue (SQS-like) works too but Kafka's log-based model is the more defensible answer at this scale.
- **"Why cap timeline cache at 800 items instead of unlimited?"** → Bounds memory cost predictably; virtually no user scrolls back further, and older content falls back to the (rarer, acceptable-latency) DB path.

That's the full arc — Day 0 single-server/single-DB, broken by fan-in read cost, fixed by fan-out-on-write, broken again by the celebrity problem, fixed by hybrid fan-out, then hardened with sharding, replication/failure handling, and multi-region. Want to now drill into any single piece deeper (e.g., actually design the Snowflake ID scheme, or go through the "add likes/retweets" extension), or run a mock Q&A where I play interviewer and pressure-test you on this?

---
