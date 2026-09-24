# Design Twitter — A Complete Narrative Walkthrough

*A story-driven HLD deep dive: naive beginnings, real failures, and the iterative fixes that turn a single-server toy into a system that serves hundreds of millions of people.*

---

## Chapter 0: Scoping the Problem Before Writing Any Architecture

Every failed "Design Twitter" interview has the same root cause: the candidate tries to design *everything* — likes, retweets, search, DMs, trending topics, notifications, ranking — and runs out of time before ever justifying the one piece that actually matters: **the home timeline**. So before drawing a single box, let's do the thing interviewers actually want to see — deliberately narrowing scope and saying *why*.

**In scope (P0/P1):**
1. Post a tweet (text, ~280 chars, optionally one image/video)
2. Follow / unfollow a user
3. View home timeline — tweets from people you follow, in order
4. View a user's profile timeline — all tweets by one person

That's it. Those four alone are enough to generate almost every hard distributed-systems question an interviewer has in their back pocket: fan-out-on-write vs. fan-out-on-read, the celebrity/hot-key problem, caching, sharding by user, eventual consistency, CAP trade-offs.

**Explicitly out of scope, and why each one is a trap:**
- **Likes / retweets / replies** — real data-modeling work, but they don't change the *core* architecture once timelines are solved. Bolt-on at the end if time remains.
- **Search** — this is secretly "design a search engine" (inverted indices, relevance scoring). Different interview entirely.
- **Notifications** — a pub/sub fan-out problem, related in spirit but distinct enough to deserve its own session.
- **Trending topics / hashtags** — a streaming heavy-hitters problem (Count-Min Sketch, sliding windows). Different toolbox.
- **DMs** — basically "design a chat system."
- **ML ranking of the feed** — assume reverse-chronological. Ranking is an ML-systems conversation, not a storage/distribution one.
- **Media transcoding / CDN internals** — wave at "blob storage + CDN," don't go deep on video pipelines.

Why does naming this cut list matter so much? Because it signals to the interviewer that you're making a *deliberate* trade-off, not an accidental omission. Good system design is as much about what you refuse to build as what you build.

**Non-functional requirements — and the one that decides everything downstream:**
- Availability over strict consistency. A timeline that's 500ms stale is invisible to a user. A timeline that returns an error page is not.
- Durability — a tweet, once acknowledged, should essentially never disappear.
- Low latency reads — people refresh their feed dozens of times a day; this has to feel instant.
- Scalable to hundreds of millions of users and a read:write ratio that skews enormously toward reads.

**Rough capacity numbers, derived from scratch and reused throughout.** It's not enough to state these — showing the arithmetic is exactly what an interviewer is listening for, because it proves the numbers aren't memorized, they're *derivable* from a couple of starting assumptions.

*Starting assumptions:*
- 250M daily active users (DAU)
- Each active user posts ~2 tweets/day on average
- Each active user opens the app and pulls their timeline ~10 times/day
- Average tweet, including metadata, is ~300 bytes; assume every tweet has a small chance of media, ignore media storage for this pass (handled separately in Chapter 8)

*Write throughput:*
```
Tweets/day = 250M users × 2 tweets/day = 500M tweets/day
Writes/sec (avg) = 500,000,000 / 86,400 sec ≈ 5,800 writes/sec
```
Round to **~6,000 writes/sec average**. Traffic isn't uniform across the day — assume a 3–5x peak-to-average ratio for a global product with regional peak hours overlapping, so **peak writes ≈ 20,000–30,000/sec**.

*Read throughput:*
```
Timeline reads/day = 250M users × 10 opens/day = 2.5B reads/day
Reads/sec (avg) = 2,500,000,000 / 86,400 ≈ 29,000/sec
```
That's already a **~5:1 read-to-write ratio** just from this one action; add profile-page views, individual tweet loads, and search-adjacent traffic and real systems see ratios of 100:1 to 1000:1 for the full read surface. Peak reads, same 3–5x multiplier: **~100,000–150,000/sec**. This is the number that actually decides the architecture — reads dominate everything downstream, which is exactly why Chapter 1 breaks on the read path, not the write path.

*Storage, over a 3-year retention window:*
```
Total tweets stored = 500M/day × 365 days × 3 years ≈ 550 billion rows
Raw tweet storage = 550B rows × 300 bytes ≈ 165 TB
```
Add indexes (roughly 1.5–2x the raw data for a table indexed on author_id + created_at) and you're looking at **~300 TB** for the `tweets` table alone before replication. Multiply by 3 copies (1 primary + 2 replicas, Chapter 5) and you're at **~900 TB** across the fleet — this single number is the concrete justification for sharding in Chapter 4: no single machine holds a comfortable multi-terabyte working set *and* serves 100K+ QPS simultaneously.

```
follows edges ≈ 250M users × 200 avg follows ≈ 50 billion edges
follows storage ≈ 50B × ~40 bytes/edge ≈ 2 TB (much smaller than tweets, but read extremely hot)
```

*Redis timeline cache sizing:*
```
250M users × 800 cached tweet_ids × 8 bytes/id ≈ 1.6 TB
```
Comfortably shardable across a modest Redis cluster (Chapter 4), and small enough that even a full cold rebuild from the database, while painful, is bounded and recoverable.

With scope and numbers derived from first principles, we can start where every good design starts: **the dumbest thing that could possibly work.**

---

## Chapter 1: Day 0 — The Single-Server Baseline

**Architecture:**

```
[Client] --> [Single App Server] --> [Single PostgreSQL Database]
```

One monolith. One relational database. No cache, no queue, no replicas. This isn't a strawman to knock down for effect — it's genuinely how an MVP of Twitter would be built, and for a small number of users it is *correct*.

**Schema — just enough to function:**

```sql
users(id, username, ...)

follows(follower_id, followee_id, created_at)
  -- PK (follower_id, followee_id)
  -- also index followee_id — we'll see exactly why soon

tweets(id, author_id, text, created_at)
  -- index on (author_id, created_at) for profile timeline
```

**API surface — full request/response contracts, not just endpoint names.** A design doc that only names endpoints leaves the hardest part (pagination, idempotency, error shape) implicit. Let's pin these down once, here, so every later chapter can refer back to a concrete contract instead of a vague verb.

```
POST /tweet
Request:
{
  "text": "just had the best coffee of my life",
  "media_id": null,                    -- optional, from a prior /media/upload call
  "idempotency_key": "client-generated-uuid-abc123"   -- see Chapter 9, prevents duplicate posts on retry
}
Response: 201 Created
{
  "tweet_id": "1698765120000-shard7-0042",   -- Snowflake ID, see Chapter 4
  "author_id": "bob",
  "created_at": "2026-08-18T10:32:00Z"
}
```

```
POST /follow
Request:  { "followee_id": "eve" }
Response: 204 No Content
Errors:   409 Conflict if already following, 404 if followee_id doesn't exist
```

```
GET /timeline/home?cursor=<opaque_cursor>&limit=20
Response: 200 OK
{
  "tweets": [
    { "tweet_id": "...", "author_id": "bob", "text": "...", "created_at": "...", "author_avatar_url": "..." },
    ...
  ],
  "next_cursor": "eyJ0cyI6MTY5ODc2NDUwMH0="   -- opaque, encodes the timestamp/id to resume from; see the
                                                  pagination note in Chapter 2 for why this replaces LIMIT/OFFSET
}
```

```
GET /timeline/user/:id?cursor=<opaque_cursor>&limit=20
-- same shape as /timeline/home, but sourced entirely from that one author's shard (Chapter 4), no merge step needed
```

Two details worth calling out explicitly because they get asked about directly: the `idempotency_key` on `POST /tweet` means a client that times out and retries the exact same request never creates a duplicate tweet — the app server checks whether that key was already processed (a short-lived key→tweet_id mapping in Redis is enough) before inserting. And every list response uses a **cursor**, never a raw page number — the reasoning for why is in Chapter 2, but the short version is that offset-based pagination silently breaks the moment the underlying data is sharded.

And here's the query that this entire multi-chapter story is secretly about:

```sql
SELECT t.* FROM tweets t
JOIN follows f ON t.author_id = f.followee_id
WHERE f.follower_id = :me
ORDER BY t.created_at DESC
LIMIT 20
```

**Why this baseline deserves respect, not dismissal:** it's strongly consistent — follow someone, refresh, you see their tweets immediately, no lag whatsoever. It's trivial to reason about. And for under ~10K users this JOIN is genuinely fast, because the indexes make it cheap and the whole working set fits comfortably in the database's memory cache anyway.

The reason we start here isn't "because it's naive" — it's because **it establishes the baseline correctness model**. Every iteration from here on is a *deliberate* trade-off away from this simplicity. In an interview, being able to say "we're giving up the consistency guarantee Day 0 had, in exchange for read latency" is worth far more than piling on infrastructure because it sounds impressive.

**Where this breaks first — and why it's not really "the database is slow":**

Picture a user following 200 people, each posting a couple times a day. To build her timeline, that JOIN has to: look up 200 followee_ids, find recent tweets from *each* of those 200 authors, merge-sort everything by time, and take the top 20. Every single refresh, for every single user, does this from scratch — even though the answer barely changes between refreshes.

Now scale it: 250M users, average 200 follows each, opening the app ~10 times a day. That's tens of thousands of QPS of this 200-way fan-in join hitting one machine. A single Postgres instance tops out at a few thousand QPS for queries this complex, because each query touches many scattered rows across many index ranges — it's nothing like a cache-friendly single-page read.

And then there's the twist that makes this worse than ordinary overload: **the celebrity problem**. If even one of the 200 people you follow is an account with 50 million followers who tweets constantly, your query still has to fan in from all 200 authors on *every single read* — there's no way to know in advance which of your followees are prolific versus quiet without doing the work each time. A celebrity's post doesn't just create one hot row; it creates a hot row that tens of millions of independent queries are simultaneously trying to reach through their own 200-way join.

The honest reframe, the one that actually leads somewhere: **we are recomputing something on every read that we could precompute once, on write.** Tweets don't change after they're posted. Follow graphs change slowly relative to how often people check their timeline. This asymmetry — cheap-to-precompute, expensive-to-recompute-per-read — is exactly the insight that unlocks the next iteration.

*A tempting shortcut worth naming and rejecting: "just add read replicas."* Replicas do help and you'd add them regardless — they're a cheap, complementary win. But they don't fix the fact that each individual query is O(followees) expensive; they just let you run more of those expensive queries in parallel before falling over. It's a stopgap, not a redesign.

*Another tempting shortcut: "just cache the whole timeline response."* Better instinct, but naive result-caching invalidates constantly — anyone among your 200 follows tweeting invalidates your cached timeline, and with 200 people that's frequent. What you actually want isn't "cache the read" — it's "precompute on write." That's fan-out.

---

## Chapter 2: Fan-out on Write — Precomputing the Timeline

### The Insight, Stated Plainly

Stop computing Alice's timeline when Alice asks for it. Instead, the moment anyone Alice follows posts a tweet, immediately push that tweet into a pre-built, ready-to-read list that belongs to Alice. By the time she opens the app, her timeline already exists — reading it is just picking up something that was already assembled for her.

This is the **push model**, or **fan-out on write**. It's the mirror image of what Day 0 did. Day 0 was *lazy* — defer everything to read time. This is *eager* — do the work the instant it becomes possible, long before anyone asks.

### The New Data Structure

Conceptually, every user gets their own precomputed list:

```
timeline:{user_id} -> [tweet_id_1, tweet_id_2, tweet_id_3, ...]   (sorted by time, capped at ~800)
```

This maps beautifully onto a **Redis sorted set** — score = timestamp, member = tweet_id. Crucially, Redis here is not the source of truth. The `tweets` table still is. Redis holds a *derived*, disposable view that can always be rebuilt from the database if it's ever lost.

### Walking Through It With a Real Example

Say Bob has three followers — Alice, Carol, and Dave — and posts *"just had the best coffee of my life."*

**Step 1 — Save Bob's tweet** (unchanged from Day 0):
```
tweets: { id: 9001, author_id: bob, text: "just had the best coffee...", created_at: 10:32am }
```

**Step 2 — Look up Bob's followers:**
```sql
SELECT follower_id FROM follows WHERE followee_id = bob_id;
-- [alice_id, carol_id, dave_id]
```

**Step 3 — Push the tweet_id into each follower's Redis list:**
```
ZADD timeline:alice  1698765120  9001
ZADD timeline:carol  1698765120  9001
ZADD timeline:dave   1698765120  9001
```

Now when Alice opens her app, reading her timeline is almost embarrassingly simple:

```
ZRANGE timeline:alice 0 20        -- O(log N), single Redis lookup
```

That's the entire read path. One data structure, one user's rows, no joins, no cross-user scanning, sub-millisecond. Compare this to the 200-way fan-in join from Day 0 — we haven't made the *total* amount of work in the system smaller, we've moved *when* it happens, from the frequent side of the ratio (reads) to the rare side (writes). Since reads outnumber writes by 100:1 to 1000:1, this is exactly the right lever to pull.

### The Analogy That Makes It Click

Think of two newspaper delivery strategies. The Pull model is a newsstand: every publisher's papers pile up centrally, and when you want the news, you personally dig through the pile, find what you care about, and carry it home — the newsstand does no work until you show up, and then makes *you* do all of it. The Push model is home delivery: a carrier proactively visits every publisher each morning, assembles your personal bundle, and leaves it on your doorstep before you wake up. Reading is now instant, because the effort was front-loaded to delivery time. Your Redis timeline list *is* that doorstep bundle.

### What We Gave Up — Four Real Costs

**1. Write amplification.** Posting a tweet used to be one insert. Now it's "one insert + N pushes," where N is follower count. For a normal user with a few hundred followers, trivial. For a celebrity with 50 million, that single tweet just became **50 million writes**. Even at a generous 100,000 writes/sec across a large worker pool, that's roughly 500 seconds — about 8 minutes — before the tweet has fully propagated to everyone. On a product promising real-time feeds, that's not a rounding error, it's a broken promise. This is the **celebrity fan-out problem**, and it's severe enough to need its own chapter (next one).

**2. Consistency became eventual, not strong.** Day 0 was strongly consistent: post, refresh, see it. Now there's a real window — however brief — where a tweet exists in the database but hasn't yet reached every follower's Redis list. We've explicitly traded consistency for read latency. Say this out loud in an interview; CAP-flavored trade-offs are exactly what's being listened for.

**3. Storage cost went up.** We're now duplicating tweet_ids across potentially millions of follower lists instead of storing each tweet once. Deliberate space-for-time trade.

**4. Fan-out has to be asynchronous.** If fan-out happened synchronously inside the `POST /tweet` request, a celebrity's post would hang the API call for minutes. So we need a **message queue** — Kafka is the standard answer — sitting between "tweet was posted" and "fan-out workers push it into follower lists," plus a pool of worker processes consuming from that queue.

**Updated write path:**
```
POST /tweet → insert into tweets DB → publish "tweet_posted" event to Kafka → return 200 immediately
                                              │
                                    [Fan-out worker pool] consumes the event,
                                    pushes tweet_id into each follower's
                                    Redis timeline list, asynchronously
```

"Post successful" and "visible in every follower's feed" are no longer the same instant — and for a social feed (unlike, say, a bank transfer) that's a perfectly acceptable, deliberate trade.

### A Detail Interviewers Probe: The Cold-Start Case

What happens to a brand-new user with an empty Redis timeline, or a user whose cache entry expired? You fall back to Day 0's on-the-fly JOIN query, serve that, and populate the cache from the result. This is the classic **cache-fill-on-miss** pattern — and it's a good moment to point out that the database never became vestigial; it's still the source of truth and the fallback path when the fast path is unavailable.

### Bounding the List

Cap each Redis sorted set at roughly 800 entries, trimming the tail with `ZREMRANGEBYRANK`. Nobody scrolls back further than that in practice, and anything older is served by falling back to the database — a "load more" path that trades a little latency for not having to keep unbounded state in memory forever.

### Why Pagination Can't Just Be `LIMIT 20 OFFSET 40`

It's worth pausing on this because it's an easy detail to hand-wave and interviewers specifically probe it. Day 0's offset-based pagination ("give me rows 40 through 60") works fine on one unsharded table with a stable sort order. It breaks in two separate ways once fan-out and sharding are involved.

First, **offsets don't survive concurrent writes.** If Alice is scrolling and requests "page 3" (offset 40), and in between her page 2 and page 3 requests five new tweets arrived at the top of her timeline, everything shifts — offset 40 now points to different rows than it did a second ago. She'll see duplicates or skip tweets entirely. This isn't a sharding problem yet, it's just a fundamental mismatch between offsets and a live, constantly-changing list.

Second, once celebrity merging (Chapter 3) is involved, "page 3 of my timeline" isn't even a single ordered list anymore — it's two lists (the Redis ZSET and the live celebrity fetch) being merged at read time. An offset into that merged, virtual list doesn't correspond to any stable offset into either underlying source.

**The fix is a cursor**: instead of "give me rows 40–60," the client says "give me the 20 items *after* this specific point in time." A cursor is just an opaque, typically base64-encoded token that encodes the timestamp (and tie-breaking tweet_id, since two tweets can share a timestamp) of the last item the client saw:

```
cursor = base64({ "ts": 1698764500, "tweet_id": "1698764500000-shard3-0091" })
```

The next request becomes `ZRANGEBYSCORE timeline:alice -inf (1698764500 LIMIT 20` — "everything with a timestamp strictly before this cursor, newest first, capped at 20." This is stable under concurrent writes (new tweets arriving at the top never shift what "before this cursor" means), and it composes cleanly with the celebrity-merge step, since both sources can independently apply "before this cursor" and the merge step just sorts the combined, already-filtered results.

---

## Chapter 3: The Celebrity Problem → Hybrid Fan-out

### Making the Failure Concrete

A celebrity with 50 million followers tweets. Fan-out means 50 million writes into Redis lists. A few things go wrong simultaneously, not just one:

- **Raw throughput.** Even a large, well-provisioned worker pool doing 100K writes/sec needs ~500 seconds — roughly 8 minutes — to finish propagating one tweet.
- **Queue poisoning.** Celebrities often post in bursts, or several celebrities post around the same live event. Worker pools back up, and if regular users share the same queue/worker pool, *their* ordinary tweets get stuck in line behind the celebrity fan-out storm too.
- **Pure waste.** A meaningful fraction of a celebrity's 50 million followers are inactive that day. We just performed tens of millions of writes that will be read zero times before falling out of the 800-item cap.

### The Fix: Branch on Follower Count

Split the user base into two classes, based on a threshold (say, 10,000 followers):

- **Normal users** (below threshold): fan-out-on-write exactly as before. Fast, cheap, and this is the overwhelming majority of accounts.
- **Celebrities** (above threshold): **skip fan-out entirely at write time.** Their tweets just sit in the `tweets` table like they did on Day 0.

### The New Read Path — A Merge, Not a Pure Lookup

```
GET /timeline/home
  1. ZRANGE timeline:{me} 0 20          -- precomputed part, from normal follows
  2. For each celebrity I follow:       -- small, bounded set — nobody follows
        fetch their recent tweets          thousands of celebrities
        directly from the tweets table
  3. Merge-sort both lists by timestamp, take top 20
```

The asymmetry that makes this cheap is worth stating explicitly: a celebrity has millions of *followers*, but any individual *user* follows only a handful of celebrities. So instead of 50 million writes at post time, we've turned the cost into "a few extra, cheap reads, merged in at read time" — and only for the tiny slice of accounts where write-fan-out was pathological in the first place.

### What We Gained, What We Gave Up

**Gained:** celebrity tweet-posting becomes a single fast DB insert again. No more queue backlogs poisoning regular users. We still keep the fast-read benefit of precomputed timelines for the 99%+ of follows that are ordinary accounts.

**Gave up:**
1. The read path is a bit more complex again — a merge of "cached list" + "live celebrity fetch," not one Redis lookup. Small, bounded latency cost (you're merging in at most a handful of extra sources).
2. We need to *track* which accounts are celebrities — typically an `is_celebrity` flag recomputed by a periodic batch job as follower counts cross the threshold. An account going viral overnight needs to migrate from "fan-out" to "read-time merge" mode, and any previously fanned-out data can simply be left to decay via the existing cap rather than requiring an urgent cleanup.
3. This is explicitly a heuristic, not a clean abstraction — and it's worth saying so. Real systems (this is genuinely close to what Twitter's own engineering blog has described) accept "special-case the outliers" because a small number of accounts contribute a wildly disproportionate share of total system cost. It's a classic Pareto / power-law situation, and hybrid designs are the standard, industry-accepted answer to it.

**The follow-up an interviewer will ask: "why not just delay celebrity fan-out instead of skipping it?"** Because delaying still means eventually doing 50 million writes — it doesn't reduce total work, only spreads it out — and you'd still need a read-time fallback for the delay window anyway. If you need that fallback regardless, you might as well always read celebrities at read-time and skip the write cost entirely.

**Another likely one: "what if I follow 500 celebrities — doesn't the 'cheap because bounded' claim break?"** Fair edge case. Cap it — merge in only the top 20–50 most recently active celebrities you follow, or paginate and degrade gracefully beyond that. Showing you've thought about the tail of your own tail case is exactly the kind of depth that separates a good answer from a great one.

This hybrid model is genuinely the crux of the interview. Everything from here — sharding, replication, caching layers, load balancing, geographic distribution, fault tolerance — is about hardening this core idea so it survives at real-world scale and doesn't fall over the moment a single machine dies.

---

## Chapter 4: Sharding the Underlying Stores

We've fixed the *access pattern*. We still have a single Postgres instance and a single Redis instance sitting behind everything — and at 250M+ users and 500M tweets/day, neither one survives on a single box, not primarily because of QPS anymore, but because of raw **data size**.

**Where a single node runs out of room** (reusing the Chapter 0 numbers directly — this is the payoff of deriving them once, from scratch, up front):
- `tweets`: ~300TB before replication (Chapter 0's derivation), tens of billions of rows over a 3-year window. Won't fit comfortably in one machine's memory for fast access, and one primary can't absorb ~6K writes/sec plus all the celebrity read-time-merge traffic indefinitely.
- `follows`: ~50 billion edges, ~2TB. Smaller than `tweets`, but read extremely hot — every fan-out operation touches it.
- Redis timeline cache: ~1.6TB (Chapter 0). Feasible on one very large node today, but leaves no room to grow, and is a single point of failure for the entire hot read path.

**The fix: partition each store by a key chosen for its dominant access pattern — not the same key for every store.**

**`tweets` → shard by `author_id`.** The two dominant lookup patterns are "get this user's tweets" (profile timeline) and "get this specific tweet by ID" (hydrating a timeline). Sharding by author_id keeps a user's entire tweet history co-located on one shard, so profile reads stay single-shard. For the by-ID lookup, you need a way to route to the right shard without a separate directory call — the elegant, widely-cited answer is a **Snowflake-style ID**: a 64-bit identifier that embeds a timestamp, a shard/worker ID, and a sequence number, so the shard is derivable from the ID itself. Worth naming explicitly; interviewers like seeing this trick recognized.

*Concretely, the 64 bits break down like this:*

```
 1 bit          41 bits                    10 bits        12 bits
[unused] [timestamp, ms since epoch] [shard/worker ID] [sequence #]
```

- **41 bits of timestamp** (milliseconds since a custom epoch, not 1970) gives roughly 69 years of range before overflow — plenty, and crucially, IDs generated later always sort numerically higher than IDs generated earlier, so `ORDER BY tweet_id` and `ORDER BY created_at` agree without needing a separate index.
- **10 bits of shard/worker ID** allows up to 1,024 distinct ID-generating nodes — enough to cover every shard, each of which can mint its own IDs independently without coordinating with any other shard.
- **12 bits of sequence number** allows 4,096 unique IDs to be minted *within the same millisecond, on the same shard*, before it has to wait for the next millisecond tick — comfortably above our ~6,000 writes/sec average spread across many shards.

Given a tweet_id, extracting the shard is a pure bitmask operation — `(tweet_id >> 12) & 0x3FF` — no lookup table, no network call, no coordination service required. This is the entire reason a by-ID tweet fetch can go straight to the correct shard instead of needing a separate directory service.

**`follows` → needs both directions, so shard both directions separately.** Fan-out (on tweet post) needs "who follows author X" — that wants an index/shard by `followee_id`. Displaying "who do I follow" wants `follower_id`. Real systems resolve this by maintaining **two denormalized copies of the edge** — a `followers` table sharded by followee_id, and a `following` table sharded by follower_id — trading storage duplication for the ability to shard each one optimally for its own dominant query. This is a clean, concrete example of "denormalize for read performance," and interviewers love hearing it stated that plainly.

**Redis timeline cache → shard by `user_id`, using consistent hashing.** Each `timeline:{user_id}` list lives on whichever node consistent hashing assigns it to. This distributes both storage and load, and — crucially — lets you add nodes later while moving only roughly `1/N` of keys, instead of the near-total reshuffle a naive `hash(key) % N` scheme would force on you the moment N changes.

*Why naive `hash % N` breaks so badly, with actual numbers:* say you have 4 Redis nodes and route with `hash(user_id) % 4`. Every user's data lands on `hash(user_id) mod 4`. Now you add a 5th node because you're running out of capacity. Every single formula output changes — `hash(user_id) % 4` and `hash(user_id) % 5` agree for almost no inputs — so roughly 80% of all keys now map to a different node than before. That's ~1.3TB of the 1.6TB timeline cache needing to move across the network simultaneously, right at the moment you were trying to relieve capacity pressure. It makes the fix worse than the problem.

*How consistent hashing actually avoids this — walked through concretely:* imagine a ring of hash values from 0 to some large maximum, drawn as a clock face. Each of your Redis nodes is hashed (by its node ID/name) onto a position on that ring:

```
                    0
               ┌────┼────┐
          Node D    │    Node A
        (pos 270)    │    (pos 60)
               │      │      │
          270 ─┤      │      ├─ 60
               │      │      │
          Node C    │    Node B
        (pos 180)    │    (pos 150)
               └────┼────┘
                   150/180
```

To find which node owns `timeline:alice`, hash `alice` to get a position on the same ring — say it lands at position 40 — then walk *clockwise* until you hit the first node. Position 40 → clockwise → Node A at position 60. Alice's data lives on Node A.

Now add a 5th node, **Node E**, at position 30. Only keys that hash to positions between the *previous* node counter-clockwise from Node E (Node D, at 270) and Node E's new position (30) get reassigned — everything from 270 through 30 on the ring, which used to belong to Node A, now belongs to Node E instead. Every other key on the ring — everything owned by Node B, Node C, and the rest of Node D and Node A's territory — is completely untouched, because walking clockwise from their hash position still lands on the same node it always did.

With 5 roughly evenly-spaced nodes, that reassigned slice is about `1/5` of the ring, so **~20% of keys move**, not 80%. In practice, to avoid any one node's slice of the ring being disproportionately large or small by chance, each physical node is actually placed at *many* positions on the ring (typically 100–200 "virtual nodes" per physical node) — this smooths out the uneven-territory problem and keeps the fraction of keys that move on any add/remove close to the theoretical `1/N`, rather than being at the mercy of an unlucky hash.

**What sharding costs us:**
1. **Cross-shard joins are dead.** Day 0's elegant JOIN cannot survive a sharded relational layer — you can't efficiently join across separate machines. We already killed that query path with fan-out-on-write, but it's worth stating explicitly: sharding is *why* Day 0's approach fundamentally cannot scale, not merely "it's slow."
2. **Rebalancing becomes an operational concern.** Shards grow unevenly — a shard that happens to hold more celebrity accounts gets hotter. Consistent hashing handles this for Redis; the DB layer needs a shard-splitting strategy.
3. **We need a routing layer** — a shard map, or routing logic embedded in the app servers (e.g., `author_id % 4096 → shard N`) — so any node can find the correct shard for a given key.

A natural follow-up: *"why not shard by tweet_id instead of author_id?"* — you'd lose locality for profile reads, since fetching one user's tweets would now fan out across every shard. Author_id sharding trades "some shards run hotter because of celebrity skew" for "profile reads always stay single-shard," which is the right call since profile reads are frequent and celebrity skew can be handled separately (extra replicas on hot shards, or isolating very large accounts onto their own dedicated shard).

---

## Chapter 5: Replication, Consistency, and What Happens When a Machine Dies

Sharding fixed scale. It did nothing for availability — each shard as described is still a single node, and a single point of failure. If one `tweets` shard dies, every user whose data lives there loses read and write access. At this scale, that's not tolerable.

### Primary–Replica Replication

Each shard becomes a small cluster: one **primary** plus two or three **replicas**, ideally spread across different availability zones. Writes go exclusively to the primary. Reads — the overwhelming majority of traffic — can be served by the primary *or* any replica, which multiplies read throughput without touching the write path at all.

```
                    ┌─────────────┐
  All writes ──────►│   Primary   │
                    └──────┬──────┘
                           │ replicates
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │Replica 1 │ │Replica 2 │ │Replica 3 │
        └──────────┘ └──────────┘ └──────────┘
                     reads distributed here
```

We use **asynchronous replication**: the primary acknowledges a write and returns success without waiting for any replica to confirm receipt. This keeps writes fast. The cost is a real, if usually tiny, **replication lag window** — if the primary dies in the instant after acknowledging a write but before streaming it to a replica, that write is lost. For a tweet, this is an acceptable risk (worst case: the user just posts again); for a payments ledger it would be unacceptable, and that contrast is worth naming out loud, because it shows the trade-off is domain-driven, not accidental.

**On failure**, a replica is promoted to primary — via a consensus-based coordinator (etcd/ZooKeeper-style leader election, or a managed cloud database's built-in failover). There's a brief write-unavailable window during promotion, typically seconds, though surviving replicas can often keep serving reads throughout.

### The Sneaky Everyday Problem: Read-Your-Own-Writes

Even with no server dying, asynchronous replication creates a subtler, everyday bug-that-isn't-a-bug. Alice posts a tweet. It's written to the primary. But her profile page read might land on a replica that hasn't caught up yet — so Alice briefly sees her own tweet disappear, then refreshes and sees it reappear once the replica catches up. This is called **read-your-own-writes inconsistency**, and the standard fix is to route any read of a user's *own* recent data to the primary specifically, while letting everyone-else's-feed reads hit replicas where slight staleness is invisible and fine.

### CAP, Stated Plainly and Applied to This System

**Consistency** — every read gets the latest write, no matter which node answers. **Availability** — every request gets *a* response, always, even if it's not the freshest data. **Partition tolerance** — the system keeps working even when some nodes temporarily can't talk to others. Because real networks partition — a cable gets cut, a switch fails — partition tolerance isn't optional for a distributed system spanning many machines. That means every partition forces an explicit choice between Consistency and Availability; you cannot have both at once.

For a newsfeed, the industry consensus is unambiguous: **AP over CP**. It's far better for Alice to see a feed that's a few seconds stale than to see an error page. This is the same choice we've been making implicitly this entire chapter — asynchronous replication, eventual fan-out, tolerable read-your-own-writes lag — and CAP is just the formal name for the family of trade-offs we've already been making. Contrast explicitly if asked: a bank account balance would flip this and choose CP, accepting downtime over ever showing a stale or lost figure.

### The Redis Layer Needs Its Own Story

Redis isn't the source of truth, so a dead cache node doesn't threaten durability the way a dead DB primary would. Two reasonable options: replicate Redis too (Sentinel/Cluster) for fast automatic failover and minimal cold-start pain, or accept that a dead node just means a temporary flood of cache misses that fall back to the DB-driven fan-in query — degraded, but not an outage. Most production systems pick the former, because at real QPS a fully cold cache could overwhelm the database with fallback queries; replicating the cache is really about *protecting the database*, not just protecting cache-hit latency.

### Failure-Handling Patterns Worth Naming Even Briefly

- **Idempotency** — tweet-post requests carry a client-generated idempotency key, so a client-side retry after a timeout doesn't create a duplicate tweet.
- **Timeouts + exponential backoff with jitter** — never wait indefinitely on a DB/Redis/Kafka call; back off with increasing, randomized delays so retries don't synchronize into new load spikes.
- **Circuit breakers** — if a shard or the fan-out worker pool is unhealthy, fail fast (serve a slightly stale cached timeline) instead of piling requests against a dying dependency.
- **Kafka durability** — the fan-out event queue itself is replicated (Kafka does this natively via partition replication); losing a fan-out event just means some followers see a delayed tweet until a cache-miss fallback catches them up.
- **Outbox pattern** — write the tweet and a "pending fan-out event" in the same DB transaction; a separate, reliable process drains that outbox into Kafka. This means a tweet's durability never depends on Kafka being up at that exact instant.

---

## Chapter 6: Caching — Why We Layer a Second Speed Boost on Top of Redis Timelines

You might reasonably ask: didn't we already introduce Redis as a cache in Chapter 2? Yes — but that was specifically the *precomputed timeline list*. There's a second, distinct caching need: the actual **content** of tweets and user profiles that gets hydrated once you have the list of tweet_ids.

### Why This Needs Its Own Layer

`ZRANGE timeline:alice 0 20` gives you 20 tweet_ids. To render them, you need each tweet's text, author name, and avatar — and a single popular tweet might be part of *thousands* of different users' timelines simultaneously. Without a dedicated content cache, every one of those timeline renders triggers a fresh database read for the same tweet. With one, the first person whose timeline includes that tweet triggers a cache fill, and everyone after that gets it from memory.

```
Key:   "tweet:9001"
Value: { author_id: bob, text: "...", created_at: "10:32am", like_count: 847 }
TTL:   3600 seconds
```

### Cache Hit vs. Cache Miss, and Why the Ratio Matters

A **hit** means Redis has the data and it hasn't expired — return it in under a millisecond, database never consulted. This should be the outcome 90–99% of the time in a well-tuned system; that ratio, your **cache hit rate**, is one of the most important numbers you'd watch in production. A **miss** falls back to the database, serves the real data, and populates the cache for next time — a **cache fill**.

### Cache Invalidation — Genuinely One of the Hard Problems

Phil Karlton's famous line — "there are only two hard things in computer science: cache invalidation and naming things" — is funny because it's true. If Bob edits or deletes a tweet, stale cached copies can keep serving the old version. Three real strategies, each with a real trade-off:

- **TTL-based expiration** — simplest. Stale content survives for up to one TTL window, then refreshes naturally. Completely acceptable for most social content; a 5-minute window of slight staleness is invisible to almost everyone.
- **Active invalidation** — the write path explicitly deletes the affected cache key the moment data changes. Fresher, but now every write path needs to know exactly which cache keys to invalidate — this becomes a tangled dependency graph in a large system.
- **Write-through** — every database write is mirrored into the cache at the same time, keeping them in lockstep. Elegant, but doubles the work on every write, and a partial failure (cache write succeeds, DB write fails, or vice versa) creates its own inconsistency to handle.

Most real systems mix these: TTL for the feed itself (staleness is fine), active invalidation for anything that must never show stale-but-wrong content (a deleted tweet can't briefly reappear), and write-through for rarely-changing profile data that needs to be correct everywhere the instant it does change.

### The Cache Stampede

Here's a failure mode that only shows up when caching is working *really* well and then something interrupts the pattern. A hot tweet is cached and being served to thousands of requests per second. Its TTL expires at exactly the same instant for everyone. In the next millisecond, a thousand simultaneous requests all miss the cache and all fire the *same* database query at once — a database that had been getting almost no direct traffic suddenly takes a thousand identical hits simultaneously. This is a **cache stampede** (or thundering herd), and it can be severe enough to crash the very database the cache was protecting.

The fix: a **mutex lock**, where only the first request to detect a miss is allowed to query the database and refill the cache, while everyone else waits briefly or is served a slightly stale value — or **probabilistic early expiration**, where the cache starts refreshing itself *before* the TTL fully expires, smoothing the stampede into a gentle trickle instead of a single synchronized spike.

### Eviction — Memory Is Always Finite

Redis can't hold everything the database can. When it fills up, something has to be evicted, and the strategy matters. **LRU (Least Recently Used)** is the standard choice and maps beautifully onto our workload: a celebrity's tweets, read by tens of millions of people, naturally stay hot in cache essentially forever, while a tweet from three months ago that only its author's mother reads naturally falls out — which is exactly correct, since that rare database hit is a totally acceptable cost.

---

## Chapter 7: Load Balancing — Making a Fleet of Servers Look Like One

### Why This Question Appears the Moment You Have Two Servers

A single app server can't serve hundreds of millions of users, so you run many identical instances in parallel. The instant you have two, a new question appears: when Alice's request arrives, which server actually handles it? What if one server is already overloaded while another sits idle? What if a server crashes mid-request?

### What a Load Balancer Does

It sits in front of the entire server fleet as the single point of contact. From the outside, your whole backend looks like one address; the load balancer decides, per request, which specific server actually handles it. The analogy that lands well: a maitre d' at a busy restaurant. Customers don't seat themselves — that leaves some tables slammed and others empty. The maitre d' routes them, adjusts when a server calls in sick, and starts directing traffic toward a new server the moment they clock in — invisible to the customer, who just sits down and gets served.

### Choosing an Algorithm

**Round robin** cycles through servers in order — simple, but blind to the fact that a "fetch 3 followees" request and a "merge in 40 celebrities" request cost wildly different amounts, so it can keep sending work to an already-backlogged server. **Least connections** tracks how many active requests each server is handling and routes new ones to whoever has the fewest — this naturally adapts to the uneven cost of our requests, which is exactly why it tends to be the better fit for a system like this one. **Weighted round robin** accounts for heterogeneous hardware, giving beefier machines proportionally more traffic.

### Health Checks and Statelessness

The load balancer pings every server periodically (an HTTP GET to `/health` is typical); a server that stops responding is pulled from rotation immediately, and rejoins once it starts passing checks again. This only works cleanly if application servers are **stateless** — holding nothing about a user locally between requests. If Server A cached Alice's session in local memory, her requests would need "sticky" routing back to that exact server, and losing Server A would log her out. The fix is to push all shared state into external stores (sessions into Redis, everything else into the database), leaving the app servers as interchangeable, disposable workers — a **share-nothing architecture**.

### Layer 4 vs. Layer 7

Layer 4 balances purely on IP/TCP information, extremely fast but blind to request content. Layer 7 reads the actual HTTP request — path, headers — and can route `/timeline/*` to one specialized cluster and `/upload` (media) to another, plus handle SSL termination centrally. For a system with genuinely different workloads behind different endpoints, Layer 7 is almost always the right call.

### The Load Balancer's Own Single Point of Failure

If the load balancer is the single entry point, doesn't it become the new single point of failure we were trying to eliminate? Yes — and the answer is the same pattern applied one level up: load balancers run in an **active-passive pair**, continuously heartbeating each other, with automatic failover to the standby (typically within a second or two) if the active one dies.

---

## Chapter 8: Media Storage and CDN Basics (Kept Deliberately Shallow)

Riyaz's original scope explicitly leaves deep media-infra out — but it's worth grounding the shallow version, because "wave at it correctly" still requires knowing what to wave at.

Images and video attached to a tweet don't belong in the relational database — a blob store (S3-style object storage) holds the actual bytes, and the `tweets` row just stores a reference URL. The genuinely interesting distributed-systems idea to know, even without going deep, is that **static media is trivially cacheable at the edge** — a CDN with points of presence around the world caches a popular image near the user requesting it, so a photo attached to a viral tweet is served from a server a few milliseconds away rather than crossing an ocean to your origin infrastructure every single time. This matters disproportionately for a hot tweet, because the same image might be viewed by millions of people within minutes — exactly the shape of traffic a CDN is built to absorb without ever bothering your origin servers.

Dynamic, personalized content (a user's actual timeline) can't be cached this way at the edge, since it's different for every viewer — but the static assets *inside* that timeline (avatars, photos, video thumbnails) absolutely can, and offloading them is often the single highest-leverage, lowest-effort latency win available.

---

## Chapter 9: Fault Tolerance — Designing for the Fact That Something Is Always Broken

A humbling truth about systems at this scale: failures aren't exceptional events, they're the normal operating condition. Across thousands of servers and dozens of services, *something* is always degrading somewhere. The real design question was never "how do we prevent every failure" — that's impossible — it's **"how does the system keep working correctly, or at least gracefully, while parts of it are broken?"**

### Fan-out Worker Crashes Mid-Delivery

Bob has 800 followers. A worker has fanned out to 600 of them when it crashes. Left unhandled, 200 followers silently never see the tweet — and silent partial failure is worse than a loud one, because nobody knows anything went wrong. The fix comes from a queue property called **at-least-once delivery**: a worker doesn't tell Kafka "I'm done" (an *ack*) until it has fully finished. If it crashes before acking, the message becomes available again for another worker to retry from scratch. Bob's fan-out eventually reaches all 800 — the 600 who already got it just receive a harmless duplicate write, since writing the same tweet_id into a timeline list twice produces the same result as writing it once. That property — repeating an operation has the same effect as doing it once — is **idempotency**, and it's precisely what makes retry-on-failure safe.

### A Downstream Service Goes Slow, Not Dead

Slow is more dangerous than dead, because dead fails fast and slow doesn't. If the profile service (names/avatars) starts taking 3 seconds instead of 50ms, every timeline request blocks on it, request queues fill up across your whole app fleet, and one small service's slowness cascades into a total outage — a **cascade failure**. **Timeouts** cap how long any single call is allowed to take. **Circuit breakers** — named after the same component in a home fuse box — track the failure rate of calls to a dependency and, once it crosses a threshold, "trip" into an open state where they stop even attempting calls for a cooldown period, returning a fallback immediately instead. After the cooldown, a single probe call decides whether to close the circuit again or keep it open. **Bulkheading** — borrowed from ship design, where watertight compartments keep one flooded section from sinking the whole vessel — gives each downstream dependency its own isolated thread pool, so a struggling profile service can only exhaust its own allotment, never starve your database or cache calls.

### Graceful Degradation as a Design Choice, Not an Accident

When assembling a timeline in parallel across cache, celebrity-merge, and profile lookups, what should happen if one of those calls fails or times out? Almost always: **return a partial result rather than nothing.** Missing avatars beats no timeline. A cached, slightly-stale portion of the feed beats an error screen. This is implemented with **parallel requests plus an overall deadline** — fire every dependency call simultaneously, set a hard budget (say 200ms total), and whatever hasn't returned by the deadline gets a sane fallback value instead of blocking the whole response.

### Retry Storms and Why Backoff Needs Jitter

Naive immediate retries after a failure can make an already-struggling dependency worse — every rejected request retries instantly, doubling load, causing more rejections. **Exponential backoff** (100ms, 200ms, 400ms...) spaces retries out to give the dependency room to recover. But if thousands of workers all follow the identical backoff schedule, they retry in synchronized waves anyway — so you add **jitter**, randomizing each wait slightly, spreading retries into a smooth stream instead of periodic spikes.

### Seeing It Happen: Metrics, Logs, Traces

None of the above matters if you can't detect it happening. **Metrics** — QPS, error rate, cache hit rate, queue depth — feed alerts that wake someone up before users complain. **Logs** reconstruct exactly what happened for a specific failed request. **Distributed tracing** follows one user's request across every service it touches, timing each hop — the tool that turns "the timeline feels slow" into "the profile service call specifically is taking 2 seconds."

---

## Chapter 10: Multi-Region — Bringing the System Physically Closer to Users

### The Constraint No Engineering Can Remove

Data in fiber-optic cable moves at roughly two-thirds the speed of light — a hard physical floor. A round trip between Tokyo and a US data center costs 150–200ms in raw physics alone, before a single byte of application logic runs. For a product refreshed dozens of times a day, that's a felt, constant tax.

### Two Distinct Reasons to Go Multi-Region

**Latency** — put infrastructure physically near users so requests don't cross oceans. **Blast radius** — an entire region failing (a cloud provider outage, a natural disaster) shouldn't take the whole product down globally.

### The Home-Region Model

Each user gets a home region (typically based on signup location); their primary shard for `tweets`/`follows` lives there. App servers and Redis caches deploy in multiple regions, and *reads* are served from whichever region is nearest a given user. *Writes* route to the user's home-region primary — deliberately **not** active-active multi-region writes to the same row, because that reopens genuinely hard conflict-resolution problems (two regions concurrently modifying the same data) for very little benefit here, since a user's own writes aren't the latency-sensitive side of the equation — reads are.

### The Interesting Wrinkle: Global Fan-out

If a Tokyo-based user follows a US-home-region author, and that author tweets, the fan-out worker has to push the new tweet_id into the Tokyo user's *local* APAC Redis cluster, not just a US one — fan-out has to be **globally aware**, crossing regions as needed. This is exactly why fan-out was made asynchronous via Kafka in the first place: it absorbs this extra cross-region latency without ever blocking the original tweet-post response.

Cross-region replication of the underlying data typically runs 100–500ms behind, larger than in-region lag but accepted for the same reason as before — a feed a second or two stale across continents is invisible to users, in sharp contrast to a domain like banking where this trade-off would flip entirely. Companies operating at this scale often invest in private backbone network links between their own data centers specifically to shrink this window, bypassing the public internet.

**Data sovereignty** is worth a brief, honest mention even though it's not the flashiest topic: jurisdictions like the EU (GDPR) require certain user data to physically remain within region — which reinforces the home-region design as a compliance requirement, not merely a latency optimization.

---

## Chapter 11: One Tweet, Traced End to End Through Every Component

Every chapter above solved one problem in isolation. It's worth stitching them together once, concretely, so the whole system reads as one coherent machine instead of ten separate patches. Here's exactly what happens, in order, when Bob — a normal user with 400 followers, not a celebrity — posts a tweet, and Alice, one of his followers, reads it thirty seconds later.

**1. Bob hits "Post."** His client sends `POST /tweet` with an idempotency key (Chapter 0's API contract) to `api.twitter.com`.

**2. DNS/GeoDNS routes him to his home region** (Chapter 10) — say, US-East, since Bob signed up there.

**3. The regional load balancer** (Chapter 7) picks a healthy app server using least-connections, terminating SSL on the way in (Layer 7).

**4. The app server checks the idempotency key** in Redis — first time seeing it, so it proceeds.

**5. The app server writes the tweet to the `tweets` table**, sharded by `author_id` (Chapter 4). Bob's `author_id` hashes to Shard 7. A Snowflake ID is minted right there on Shard 7's ID generator, embedding Shard 7's worker ID in its bits, so the ID alone will let anyone find this tweet later without a lookup.

**6. That write goes to Shard 7's primary**, which acknowledges immediately and asynchronously streams the row to its two replicas (Chapter 5) — the write doesn't wait for them.

**7. The app server publishes a `tweet_posted` event to Kafka** (Chapter 2) — `{tweet_id, author_id: bob, created_at}` — and immediately returns `201 Created` to Bob's client. Total time so far: comfortably under 100ms. Bob sees "Tweet sent" and moves on with his day, with no idea about the six steps that happen next.

**8. A fan-out worker, somewhere in the worker pool, consumes that Kafka event.** It looks up Bob's `is_celebrity` flag — false — and queries the `followers` table (Chapter 4, sharded by `followee_id`) for everyone who follows Bob: 400 user_ids, including Alice.

**9. For each of those 400 followers, the worker pushes `tweet_id` onto `timeline:{follower_id}`** in Redis. Alice's `user_id` hashes to a position on the consistent-hashing ring (Chapter 4) that lands on, say, Redis Node 12. `ZADD timeline:alice <timestamp> <tweet_id>` lands there. This is one of 400 essentially-identical writes, finishing in well under a second total across the worker pool, since 400 is nowhere near celebrity scale.

**10. Thirty seconds later, Alice opens her app in Tokyo.** GeoDNS routes her to the APAC region, not US-East — her *reads* are served locally even though her data's timeline-cache write in step 9 happened to land on a specific Redis node that's part of a cluster replicated toward her region.

**11. Alice's `GET /timeline/home` hits the APAC load balancer**, lands on a stateless app server there, which does two things in parallel: `ZRANGE timeline:alice` (getting Bob's tweet_id plus everything else recent) and a live fetch of any celebrities Alice follows (Chapter 3) — say she follows one, so this is a single cheap query, not a fan-out.

**12. The app server merges both lists by timestamp**, and for each tweet_id in the final top-20 — including Bob's — checks the **content cache** (Chapter 6) for the actual tweet text and Bob's avatar URL. Cache miss on this specific tweet the first time anyone in APAC reads it, so it falls through to Shard 7 (routed to directly via the Snowflake ID's embedded shard bits, no directory lookup needed), fetches the row, and populates the content cache for the next reader.

**13. The response comes back to Alice** with Bob's tweet included, a `next_cursor` for pagination (Chapter 2) encoding the oldest timestamp in this batch, and — separately — the CDN (Chapter 8) is what actually served Bob's avatar image and any attached photo, from an edge node physically near Alice in Tokyo, not from US-East at all.

Every mechanism from Chapters 1 through 10 shows up exactly once in this trace, doing exactly the job it was introduced to do. If a step here doesn't map cleanly back to a chapter, that's usually a sign of a gap in the design — which is a useful self-check technique to reuse in an actual interview: after presenting a full architecture, pick one concrete request and narrate it end to end. It's often where hidden gaps surface.

---

## Final Recap — The Whole System in One Diagram

```
                          [Global Load Balancer / GeoDNS]
                                      │
                ┌─────────────────────┼─────────────────────┐
                │                     │                      │
          [US Region]            [EU Region]            [APAC Region]
                │
     [App Servers — stateless, autoscaled, behind Layer-7 LB]
                │
      ┌─────────┼──────────────┬──────────────────┬─────────────┐
      │         │               │                  │             │
 [tweets DB  [follows DB   [Redis timeline    [Content cache   [Kafka:
  shards,     shards,       cache shards,      for tweet/       tweet_posted
  sharded by  sharded both  sharded by         profile          events]
  author_id,  by follower_  user_id via        content]              │
  Snowflake   id and        consistent                          [Fan-out
  IDs;        followee_id;  hashing;                              worker pool
  primary +   primary +     primary + replica                     — global-
  replicas    replicas      per shard]                             aware,
  per shard]  per shard]                                           skips
                                                                     celebrities]
```

**Read path:** Client → nearest region's LB → app server → Redis timeline (ZRANGE) merged with live celebrity fetch → content cache hydrates each tweet → return, typically sub-100ms end to end.

**Write path:** Client → home region → insert into `tweets` (sharded by author_id) → publish event to Kafka → return 200 immediately → async, globally-aware fan-out workers push into every follower's Redis timeline (skipping celebrity authors entirely, per Chapter 3).

**Consistency stance, stated once and applied everywhere:** AP over CP for propagation — a tweet's *visibility* to the world is eventually consistent, deliberately. But the underlying write itself, once acknowledged, is durable at the single-row level; we're eventually consistent about *how fast it spreads*, never casual about *whether it's actually saved*.

---

## The "Why Not X" Arsenal — Rapid-Fire Answers for the Questions Most Likely to Actually Land

- **"Why not a graph database for follows?"** — Graph DBs shine at multi-hop traversal (mutual friends, recommendations). Our access patterns are simple key lookups by follower_id or followee_id, which a sharded relational/KV store handles just as well, more simply.
- **"Why Kafka specifically, not a simple task queue?"** — Replayability (a crashed worker resumes from its last offset instead of losing the event), very high throughput, and multi-consumer fan-out (the same "tweet posted" event can simultaneously feed fan-out workers, notification workers, and analytics without coupling them together). A simpler queue works too; Kafka's log-based model is just the more defensible answer at this scale.
- **"Why cap the timeline cache at 800 items instead of storing everything?"** — Bounds memory cost predictably. Virtually nobody scrolls back further, and anything older falls back to the rarer, acceptable-latency database path.
- **"Why relational for tweets/follows and not NoSQL from day one?"** — At Day 0 scale, relational gives JOINs, transactions, and strong consistency for free — exactly what the naive timeline query needs. NoSQL's real benefits (horizontal write scaling, flexible schema) aren't needed yet; introducing that complexity before the data forces your hand is solving a problem you don't have.
- **"How would you pick the celebrity threshold?"** — No universally correct number; reason about it live. Pick a threshold where write-fan-out cost (threshold × average tweets/day) stays comfortably within your worker pool's throughput, and say plainly that in practice this would be empirically tuned, not derived from first principles.

---

*This walkthrough intentionally follows the same arc every time, because that repetition is the actual lesson: notice a real cost, name exactly which trade-off you're making to fix it, and state explicitly what you gave up in exchange. Day 0's single JOIN → broken by fan-in read cost → fixed by fan-out-on-write → broken again by the celebrity write storm → fixed by hybrid fan-out → hardened with sharding, replication, caching, load balancing, and geographic distribution → made resilient with explicit fault-tolerance patterns. Every "next" in this document is one more iteration of that same loop.*

---

## Glossary — Every Term, Defined Once, Findable Later

- **Fan-out on write (push model)** — precompute a user's timeline the moment someone they follow posts, so reads are cheap. Chapter 2.
- **Fan-out on read (pull model)** — compute a timeline at request time by joining across everyone followed. Day 0's approach; expensive at scale. Chapter 1.
- **Celebrity/hot-key problem** — a single account with a disproportionately large follower or follow count breaks the assumptions either fan-out model relies on. Chapter 3.
- **Idempotency** — performing an operation twice has the same effect as performing it once, which is what makes retrying a failed request safe. Chapters 0, 9.
- **Sharding** — splitting one logical table across many physical machines by a partition key, because no single machine can hold or serve the full dataset. Chapter 4.
- **Snowflake ID** — a 64-bit ID encoding timestamp + shard ID + sequence number, so the shard owning any given ID is derivable without a lookup. Chapter 4.
- **Consistent hashing** — a hashing scheme that minimizes how many keys move when nodes are added/removed (≈1/N instead of nearly all of them). Chapter 4.
- **Primary–replica replication** — one authoritative node (primary) takes writes; read-only copies (replicas) absorb read traffic and stand by for failover. Chapter 5.
- **Replication lag / eventual consistency** — the brief, real window where a replica hasn't yet caught up to the primary's latest write. Chapter 5.
- **CAP theorem** — under a network partition, a distributed system must choose Availability or Consistency; Partition tolerance isn't optional. Chapter 5.
- **Read-your-own-writes inconsistency** — a user briefly can't see their own just-made write because it landed on a replica that hasn't caught up. Chapter 5.
- **Cache hit rate** — the fraction of reads served from cache without touching the database; the single most-watched cache metric in production. Chapter 6.
- **Cache stampede / thundering herd** — many simultaneous requests miss the cache at once (e.g. on TTL expiry) and hit the database simultaneously. Chapter 6.
- **LRU eviction** — evict the Least Recently Used cache entry when the cache is full; maps naturally onto popularity-skewed content. Chapter 6.
- **Least connections (load balancing)** — route each new request to whichever server currently has the fewest active requests. Chapter 7.
- **Share-nothing / stateless architecture** — app servers hold no per-user state locally, so any server can handle any request and load balancing works cleanly. Chapter 7.
- **CDN / edge server** — geographically distributed servers that cache static content close to users, avoiding a round trip to the origin. Chapter 8.
- **Circuit breaker** — stops calling a failing/slow dependency for a cooldown period instead of piling up requests against it. Chapter 9.
- **Bulkheading** — isolating each dependency's resource pool (e.g. thread pool) so one slow dependency can't starve the others. Chapter 9.
- **Exponential backoff with jitter** — retries wait progressively longer, with randomization, to avoid synchronized retry storms. Chapter 9.
- **Graceful degradation** — returning a partial, still-useful result when part of the system fails, instead of a full error. Chapter 9.
- **Multi-region / home-region model** — each user's data lives primarily in one region for low-latency local writes, while reads are served from whichever region is nearest them. Chapter 10.
- **Cursor-based pagination** — paginating by "everything after this specific point" (an opaque token) instead of a numeric offset, so it survives concurrent writes and merged/sharded sources. Chapter 2.
