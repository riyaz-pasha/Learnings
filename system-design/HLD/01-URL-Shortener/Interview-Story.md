## Scoped Requirements

**P0 — Core, drives the design:**

1. **Shorten**: given a long URL, return a short unique code (`POST /shorten`).
2. **Redirect**: given a short code, redirect to the original long URL, fast, and almost always available (`GET /{code}`).
3. **Uniqueness at scale**: two different long URLs must never collide on the same short code — and this has to hold even when many servers are generating codes concurrently, with no single choke point doing the checking.

**P1 — Shapes the architecture but isn't the crux:**

4. **Read-heavy skew**: real-world traffic on these systems is typically 100:1 (or worse) reads-to-writes — someone shortens a link once, then it gets clicked thousands of times. This is what justifies aggressive caching later.

**Cutting these P2s (explicitly, with reasoning):**

- **Custom aliases** ("make my link `bit.ly/my-sale`") — no architectural impact, just an extra existence-check before the same write path.
- **Click analytics / dashboards** — a separate async pipeline (e.g., fire an event to Kafka, let a downstream service aggregate it). Doesn't touch the core read/write path we care about.
- **Link expiration (TTL)** — a background sweeper job or a TTL column; doesn't change core design.
- **User accounts / auth / "my links" dashboard** — standard CRUD auth, not distributed-systems-interesting.
- **Rate limiting abusive shorteners** — orthogonal, bolt-on concern (token bucket at the edge), not core to *this* system's identity.

**The one requirement we'll spend the most time on: #3, uniqueness at scale.**

Here's why that's the meaty one, in one sentence to preview the shape of the story: the *naive* way to guarantee uniqueness (auto-increment ID in one database, or "generate random code, then check if it exists") either creates a single bottleneck every write has to go through, or creates a race condition once you have multiple app servers writing at the same time — and fixing that cleanly, without coordination overhead, is basically the entire interview.

Everything else (caching, replication, load balancing) is important but relatively standard once you've seen it in other systems. The ID-generation problem is the part that's *specific* to "shorten a URL" and where FAANG interviewers dig hardest with "why not just—" questions.

---

Does this scoped list look right to you? Once you confirm, I'll start with Day 0 — the dumbest thing that could work.

---

## Day 0: The Dumbest Thing That Could Work

Picture a single engineer, Priya, building this over a weekend. She's got one app server and one Postgres database. That's it.

**The data model:**

```sql
CREATE TABLE urls (
    id BIGSERIAL PRIMARY KEY,
    long_url TEXT NOT NULL,
    short_code VARCHAR(7) UNIQUE,
    created_at TIMESTAMP DEFAULT now()
);
```

**The trick for generating the short code**: Priya doesn't invent a random string and hope it's free. She lets Postgres hand her a guaranteed-unique number — the auto-incrementing `id` — and then just *translates* that number into letters and digits, the same way you'd convert decimal to hexadecimal, but using a 62-character alphabet (`a-z`, `A-Z`, `0-9`) instead of 16.

Think of it like a car odometer that, instead of wrapping at 9, wraps at 61 and uses a letter instead of a digit. `id = 1` becomes `"1"`. `id = 62` becomes `"10"`. `id = 100000000` becomes something like `"6LAmH"` — six characters, covering 62^6 ≈ 56 billion codes.

**Write flow (`POST /shorten`):**

1. Client sends `POST /shorten` with body `{"long_url": "https://example.com/some/very/long/path"}`.
2. App Server runs: `INSERT INTO urls (long_url) VALUES ('https://example.com/...') RETURNING id;`
3. Postgres returns, say, `id = 100000000`.
4. App Server converts that integer to base62 in-process → `"6LAmH"`.
5. App Server runs: `UPDATE urls SET short_code = '6LAmH' WHERE id = 100000000;`
6. Returns `{"short_url": "https://tiny.ly/6LAmH"}` to the client.

**Read flow (`GET /{code}`):**

1. Client hits `GET /6LAmH`.
2. App Server runs: `SELECT long_url FROM urls WHERE short_code = '6LAmH';`
3. Server responds `301 Moved Permanently` (or `302`, more on that trade-off later) with `Location: https://example.com/...`.

```
Client → App Server → Postgres
             ↑ base62(id) ↑
```

**Why this is a *reasonable* Day 0, not a strawman:**

The guarantee it gives you is real and valuable: **uniqueness is trivially, perfectly correct**, because Postgres's `BIGSERIAL` is a single source of truth handing out sequential numbers one at a time under the hood (it's backed by a sequence object with its own atomic increment — no two transactions can ever get the same value, even if they commit out of order). There's no coordination protocol to design, no race condition to reason about, no distributed consensus. One machine, one sequence, done. It's also *dense* — codes are short because you're not wasting space on collision-avoidance padding.

What it deliberately has **not** paid for yet: availability beyond one box, and throughput beyond one box's write capacity. Priya knows this. She's trading "this can never break in a subtle way" for "this will break in an obvious way the moment load shows up" — which is exactly the right trade to make on day 0, because obvious breakage is cheap to diagnose and this lets her ship something correct today.

---

Next up: we do the arithmetic — DAU, QPS, and a specific number where this single Postgres box stops being a "when we get big enough" hypothetical and becomes Priya's Monday-morning pager going off.

---

## Break It: The Monday Morning Page

Priya's service ships, gets picked up by a scrappy marketing team, and six months later it's handling real traffic. Let's do the arithmetic she should have done before bed, not after the pager went off.

**Assume:**
- 100 million DAU (daily active users) using the service indirectly (mostly clicking links, not shortening them)
- Each user clicks ~2 short links a day → reads
- 1 in 500 users shortens a link a day → writes

**Reads (redirects):**

```
100,000,000 users × 2 clicks/day = 200,000,000 reads/day
200,000,000 / 86,400 seconds/day ≈ 2,315 QPS average
```

**Writes (shortens):**

```
100,000,000 / 500 = 200,000 shortens/day
200,000 / 86,400 ≈ 2.3 QPS average
```

At first glance: 2.3 writes/sec looks trivially fine for one Postgres box — a single well-tuned instance can do thousands of writes/sec. So where's the fire?

**Two separate fires, and they're different fires:**

**Fire #1 — reads have no floor, and traffic isn't average, it's bursty.** Average QPS is a lie that hides the worst moment of your day. A marketing team drops a shortened link in a Super Bowl ad, or a celebrity — call him **@DwayneJ** — tweets a `tiny.ly` link to 200 million followers. In the 10 minutes after that tweet, you don't get 2,315 QPS, you get a spike that might be 50-100x that on a single hot row: tens of thousands of reads/sec all hitting `SELECT ... WHERE short_code = 'aB3xQ1'` — the *same row*, over and over. Postgres can serve that row fast individually, but every one of those reads still costs a network round trip, a connection from the pool, a B-tree index lookup, and a lock acquisition path — and it's competing with every *other* read and write on the same single box. One hot key can degrade the whole instance for everyone else, the same way one clogged checkout lane backs up the whole grocery store even though the other lanes are empty.

**Fire #2 — the single Postgres box is a single point of failure, full stop.** There's exactly one database. If that instance's disk fills up, or it needs a restart for a routine version upgrade, or the VM it's on gets rebooted by the cloud provider, **the entire product is down** — not degraded, down, both reads and writes, for every user, everywhere in the world, including someone in Mumbai whose request now has to round-trip to a single us-east-1 box even on a good day, adding 200ms+ of pure geography to every redirect.

And there's a subtler third problem lurking, worth naming now because it's the one that will actually shape the *next* several iterations: **the auto-increment ID is what made uniqueness trivial, but it only stays trivial as long as there's exactly one database issuing IDs.** The moment Priya's on-call rotation says "let's just add a second Postgres box for redundancy," she's one bad afternoon away from two servers independently handing out `id = 100000001` to two different users' links — silently overwriting one person's URL with another's. The fix for Fire #2 (add more DB servers) directly reopens the "meaty" problem we scoped at the start: uniqueness without a single choke point.

**A concrete worked failure:** Say `tiny.ly` is on a single `db.r6g.xlarge` Postgres instance. AWS does an unplanned host retirement on that instance at 2:47am. Every redirect — 2,315 QPS worth, ignoring bursts — starts returning connection errors until someone manually promotes a backup or spins up a new box. If there's no standby, that's not a "degraded experience," that's a full outage measured in the time it takes a human to wake up, SSH in, and restore from the last WAL checkpoint.

---

Next up: we fix Fire #2 first (the single-point-of-failure one) since it's the more urgent one — Priya adds database replication. But that immediately raises a question: replicas of a *single* writer are easy; the real fork in the road is what happens when we also want to spread out the *writes*, and that's where the auto-increment trick we leaned on for uniqueness starts to actively fight us.

---

## Iteration 1: Read Replicas — Fixing the Single Point of Failure (Fire #2)

Priya's fix: keep one **primary** Postgres for writes, but attach 2-3 **read replicas** that continuously stream the primary's write-ahead log (WAL) and apply it locally. Reads get spread across replicas; only writes go to the primary.

The analogy that actually maps here: think of a newsroom with one reporter (primary) who writes the story, and several photocopiers (replicas) that continuously copy the latest draft. Anyone who wants to *read* today's paper grabs a copy from whichever photocopier is free. Nobody reads directly over the reporter's shoulder anymore — that would mean only one person can read at a time, and it'd slow the reporter down. But if a photocopier is a few seconds behind because it hasn't picked up the reporter's latest edit, that's **replication lag** — the copy you grabbed might be a beat stale.

**What changed — write flow:** unchanged. Writes still go through `INSERT ... RETURNING id` → base62 → `UPDATE`, all against the primary. (Referencing Iteration 0, no new detail needed here.)

**What changed — read flow (`GET /{code}`):**

1. Client hits `GET /6LAmH`.
2. Load-balancer-aware App Server routes the query to one of N **read replicas**, not the primary — e.g., round-robin or least-connections across `replica-1`, `replica-2`, `replica-3`.
3. Chosen replica runs: `SELECT long_url FROM urls WHERE short_code = '6LAmH';` against its own local, slightly-lagged copy of the table.
4. Server responds `301`/`302` with `Location: <long_url>`.

```
                        ┌──> Replica 1 ──┐
Client → App Server ────┼──> Replica 2 ──┼──> (reads)
    │                   └──> Replica 3 ──┘
    └──(writes only)──> Primary ──(WAL stream)──> replicas
```

**What we gained:**
- If the primary dies, reads keep working off replicas while a new primary is promoted — the product degrades to "can't shorten new links" instead of "fully down."
- Read capacity now scales roughly linearly: add a 4th replica, absorb more read QPS. This directly helps with the *average*-load part of Fire #1, though not the single-hot-row part yet — that's still one row on one machine no matter how many replicas share the load, since each replica is a full copy, not a partition.
- Geographic reads can improve if replicas are placed in multiple regions (more on this in the globalization chapter later).

**What we gave up / new problem introduced:**
- **Replication lag → a real consistency bug**, not a hypothetical one: Priya shortens a link on her laptop for a demo, immediately texts the `tiny.ly/xY9z` link to a colleague, colleague clicks it 200ms later, and hits a replica that hasn't caught the WAL update yet → **404, "link not found," even though it definitely exists.** This is CAP's C-vs-A trade-off showing up concretely: we chose **availability** (replicas keep serving reads even if slightly stale) over **strict consistency** (every read reflects the very latest write). This system is fine with *eventual* consistency on reads because a few hundred milliseconds of staleness on a freshly-created link is a minor, self-healing annoyance — not a money-losing bug like it would be in, say, a bank balance.
- Still doesn't fix Fire #2's write half: **one primary is still one box for all writes**, and still one thing that, if it goes down, halts all shortening (reads keep working, writes don't) until a replica is manually or automatically promoted.
- Still doesn't touch the auto-increment-ID coupling: there's still only *one* place issuing IDs, so uniqueness is still trivially safe — for now.

**What we considered and rejected:**
- *"Just make the single Postgres box bigger (vertical scaling)"* — rejected because it has a hard ceiling (biggest instance type available) and does nothing for the single-point-of-failure problem; a bigger single box is still a single box.
- *"Add a second primary, both accepting writes (multi-primary)"* — rejected at this stage because it reopens the ID-collision problem immediately (two sequences, two `id=100000001`s) with no payoff yet, since write volume (2.3 QPS) isn't remotely the bottleneck — only availability is. Don't solve a problem you don't have yet.

---

**Likely interviewer follow-ups:**

- *"What happens to a user who creates a link and immediately shares it, and the read hits a lagged replica?"* → Either accept brief eventual consistency (acceptable for this product), or route reads-immediately-after-write for that specific user/session to the primary for a few seconds ("read-your-writes" consistency), then fall back to replicas.
- *"How do you handle primary failure — is promotion automatic?"* → Manual promotion is slow (human-in-the-loop, minutes of write downtime); production systems use a consensus-based failover tool (e.g., Patroni with etcd, or a managed service like RDS Multi-AZ) so a new primary is elected and promoted in seconds, and replicas are automatically repointed.

---

Next up: read replicas handled *availability*, but they did nothing for the hot-row problem — @DwayneJ's link is still hammering one row on one machine (primary or replica) tens of thousands of times a second. That's what pulls us toward caching.

---

## Iteration 2: Caching — Fixing the Hot-Row Problem (Fire #1, part 2)

@DwayneJ tweets his `tiny.ly/aB3xQ1` link. In the next ten minutes, 40,000 requests/sec hit `GET /aB3xQ1`. Every single one of those still does the exact same thing: acquire a connection, walk a B-tree index, return one row. Spreading those across three replicas just means each replica eats ~13,000 QPS of *identical, wasteful, repeated work* — you're paying full database cost to answer a question whose answer hasn't changed in the last hour.

The analogy: this is the difference between a librarian re-walking to the stacks to fetch the same bestselling novel for the 40,000th customer today, versus just keeping a stack of copies at the front desk. The book didn't change. Walking to the stacks every time is pure waste once you notice the same request repeating.

So Priya puts a **Redis cache** in front of the database, keyed by short code, holding `short_code → long_url`.

**What changed — read flow (`GET /{code}`), now branching on cache hit vs. miss:**

**Branch A — cache hit (the common case, especially for viral links):**
1. Client hits `GET /aB3xQ1`.
2. App Server runs `GET short:aB3xQ1` against Redis.
3. Redis returns the long URL string directly from memory — sub-millisecond, no database touched at all.
4. Server responds `301` with `Location: <long_url>`.

**Branch B — cache miss (first click ever, or entry evicted):**
1. Client hits `GET /aB3xQ1`.
2. App Server runs `GET short:aB3xQ1` against Redis → nil.
3. App Server falls back to Postgres replica: `SELECT long_url FROM urls WHERE short_code = 'aB3xQ1';`.
4. App Server writes the result back into Redis for next time: `SET short:aB3xQ1 "https://example.com/..." EX 86400` (24-hour TTL — an eviction policy, not a correctness requirement, since URLs almost never change once created).
5. Server responds `301` with `Location: <long_url>`.

```
                              ┌─ hit ──> return long_url (fast)
Client → App Server → Redis ─┤
                              └─ miss ─> Replica DB → backfill Redis → return long_url
```

**What we gained:**
- @DwayneJ's link now gets served from memory after the *first* request. 40,000 QPS on one hot key becomes ~1 database read and 39,999 in-memory lookups — the database barely notices the spike at all. This is what actually fixes the hot-row problem that replicas alone couldn't touch.
- Redis is genuinely built for this: single-digit-millisecond reads, easily handles hundreds of thousands of ops/sec on modest hardware, because there's no disk seek, no index walk, no query planner — just a hash-map lookup.

**What we gave up / new problems introduced:**

1. **Cache invalidation on writes.** If Priya ever supports *editing* a shortened link's target (not in original scope, but interviewers will ask), the cached copy goes stale until TTL expires or she explicitly `DEL`s the key on write. For pure create-once-immutable URLs (our actual scope), this is minor — but it's the first crack in "cache = free win, no downsides."
2. **Cache stampede risk.** Picture a *new* celebrity link going viral in the same second it's created, or Redis evicting a still-hot key under memory pressure. If 40,000 requests hit a cache miss for the *same* key simultaneously, all 40,000 fall through to the database at once — a self-inflicted thundering herd that can knock over the very replica the cache was supposed to protect. Fix: **request coalescing / locking** — the first request to miss acquires a short-lived lock (e.g., `SET lock:aB3xQ1 1 NX PX 2000`), fetches from DB, populates cache, releases lock; the other 39,999 either wait briefly on the lock or get served a slightly-stale/placeholder response rather than all hammering Postgres simultaneously.
3. **Redis itself is now a new dependency that can fail.** If Redis goes down entirely, every request becomes a forced cache miss simultaneously — effectively a stampede across the *entire* keyspace at once, not just one hot key. This needs a **circuit breaker**: if Redis calls start timing out or erroring past a threshold, the App Server should trip the breaker and go straight to the DB replicas for a cooldown window, rather than wasting time retrying a dead cache on every single request and adding latency on top of an outage.
4. **Memory is finite, so eviction matters.** Redis can't hold every URL ever shortened forever; Priya sets `maxmemory-policy allkeys-lru` so cold, rarely-clicked links get evicted first, keeping hot ones resident — this is a deliberate trade of "always fast" for "fast for what's actually popular," which is fine because the traffic distribution here is famously long-tailed (a small fraction of links get the vast majority of clicks).

**What we considered and rejected:**
- *"Just add more read replicas instead of a cache"* — rejected because it doesn't fix the underlying waste, it only spreads it across more machines. You'd need dozens of replicas to absorb a true hot-key spike via raw DB capacity, versus one modestly-sized Redis cluster that costs a fraction as much and responds ~100x faster per request.
- *"Cache at the CDN/edge layer instead of app-level Redis"* — not rejected, actually complementary, and we'll circle back to this in the globalization chapter; a CDN can cache redirects even closer to users, but Priya starts with app-level Redis because it's simpler to reason about and invalidate for now.

---

**Likely interviewer follow-ups:**

- *"What if Redis and the cache-population write race — two requests miss at once, both write to Redis?"* → Harmless here specifically because the value is immutable (same short code always maps to the same long URL), so a double-write is just redundant, not corrupting. This tolerance is a direct payoff of choosing an immutable data model early.
- *"301 or 302 redirect — does it matter?"* → Yes, and it interacts with caching in an interesting way: a `301` (permanent) gets cached by the *user's browser itself*, so repeat visits from the same user never even hit your server again — great for load, bad if you ever want click analytics or need to change the target. A `302` (temporary) always round-trips through your server. Given we cut analytics from scope, `301` is defensible, but it's worth naming the trade-off explicitly if asked.

---

Next up: reads and single-primary-writes are now in good shape. But we still haven't touched the thing we flagged as the real interview centerpiece — what happens to our clean, trivially-unique auto-increment ID scheme the moment write volume or availability needs force us to shard the database into multiple independent primaries.

---

## Iteration 3: The Uniqueness Problem Resurfaces — Sharding the Primary

Here's the trigger, concretely: Priya's on-call lead points out that the single primary is still a single point of failure *for writes*, and also that 2.3 QPS today assumes a small, sleepy product — if `tiny.ly` gets adopted by a major platform's internal link-wrapping (every outbound link in every email from a big marketing tool gets shortened), write QPS could jump 100-1000x overnight. The fix on paper is obvious: **shard the database** — run multiple independent Postgres primaries, each owning a slice of the keyspace, so writes parallelize instead of queuing on one box.

But the moment there are, say, `shard-0`, `shard-1`, `shard-2`, `shard-3`, each running its own independent `BIGSERIAL`, the trick we leaned on since Day 0 quietly breaks. Each shard's sequence starts at 1 and counts up *independently*. `shard-0` issues `id=1` to Alice's link. `shard-1`, completely unaware of `shard-0`, *also* issues `id=1` to Bob's link, at the same moment. Both get base62-encoded to the exact same short code. Two totally different URLs now collide on `tiny.ly/1`. This isn't a rare edge case — it's guaranteed to happen constantly, because every shard's counter restarts from the same place.

The real-world analogy: it's like four separate DMV branches each independently numbering driver's licenses starting from #1, with no phone line between them. Works fine as long as there's one branch. The instant you open branch #2, you get two different people both holding license #1 — and nobody notices until the collision causes real harm (someone's URL silently gets overwritten by someone else's).

So the actual problem isn't "how do we shard a database" — DBs get sharded all the time. It's specifically: **how do multiple independent writers generate IDs that are guaranteed unique *without* talking to each other on every single request** (a global lock/coordinator would just recreate the single-point-of-failure we're trying to escape).

This is where we spend the real interview time. There are three real families of answer, and Priya is going to evaluate them like a systems engineer, not just pick one:

**Option A — Centralized ID-generation service** (a dedicated "ticket server" that hands out ranges of IDs, e.g., Flickr's old approach: one service says "here, App Server, you own IDs 5,000,001 through 5,001,000, hand those out locally until you run out, then come back for another block"). Fast per-request (no network call needed until a block is exhausted), but it's a new single point of failure/bottleneck that we've now recreated — just moved, not eliminated.

**Option B — Decentralized, self-sufficient generation (Twitter Snowflake-style)**: each App Server/shard generates its own ID *locally*, with zero coordination, by baking uniqueness into the ID's structure itself — e.g., `[timestamp bits][machine/shard-ID bits][per-ms sequence bits]`. As long as every machine has a distinct machine ID (assigned once, at boot, not per-request), no two machines can ever produce the same number, because the machine-ID bits alone guarantee separation.

**Option C — Hash the URL itself** (e.g., MD5/SHA-256 the long URL, take the first 6-8 base62 characters). Deceptively appealing — no coordination *and* no counter needed at all — but it reopens the exact same collision risk in a different disguise: two different long URLs can produce the same truncated hash prefix (a birthday-paradox collision), so you still need a `SELECT ... WHERE short_code = X` existence check before every insert, which is itself a serialization point across shards, and gets *worse*, not better, as the table grows.

Priya's team is going to land on **Option B (Snowflake-style local generation)** as the L4/L5-defensible answer, because it's the only one of the three that achieves true zero-coordination writes at arbitrary shard count — but I want to stop here and let you sit with these three options before we build out Option B's actual bit layout and walk the write flow through it in detail, since that's the part interviewers drill on hardest ("how many bits for what, and why").

---

**Likely interviewer follow-up right at this fork:**

- *"Why not just use a UUID (e.g., UUIDv4) instead of any of this — isn't that already unique with zero coordination?"* → It is unique, and zero-coordination, which is genuinely attractive — but a UUID is 128 bits / 36 characters as a string, which defeats the entire product goal of a *short* URL, and it's also random, which means it doesn't sort by creation time (useful for debugging/pagination later) and causes poor B-tree index locality on insert (random inserts across the whole key range cause more page splits than roughly-increasing inserts). We could truncate a UUID to fit in 6-7 chars, but then we're back to the birthday-paradox collision-check problem from Option C, just with a fancier input.

---

Got it, next, or want to dig into why Option A's "hand out blocks of IDs" isn't just "good enough" before we move to B?

---

## Iteration 3b: Building Option B — Snowflake-Style ID Generation

Let's build the actual bit layout, because "each machine generates locally" is the easy part to say and the part interviewers actually probe is *how*.

The core idea, in plain language first: instead of asking a database "what's the next number?", each App Server just **assembles** a unique number out of ingredients it already has on hand — the current time, its own pre-assigned identity, and a small local counter — the same way a shipping label is unique not because a central office tracked every package ever shipped, but because it combines *this warehouse's ID* + *today's date* + *the Nth package packed at this warehouse today*. No warehouse needs to call another warehouse to know its label won't collide — the label's structure itself guarantees that.

**The 64-bit layout** (fits in a signed 64-bit integer, same type Postgres's `BIGINT`/original `BIGSERIAL` already used — nothing about the storage layer needs to change):

```
 1 bit          41 bits                    10 bits         12 bits
[unused] [   timestamp (ms since epoch) ][ machine ID ] [ sequence ]
```

- **41 bits of timestamp** — milliseconds since a custom epoch (e.g., Jan 1, 2024, not Jan 1, 1970, to stretch the usable range further). 2^41 ms ≈ 69.7 years of headroom.
- **10 bits of machine ID** — up to 2^10 = 1,024 distinct App Servers/shards can each get a permanently assigned, unique ID (assigned once at boot, e.g., from a small config value or a coordination service like Zookeeper *just for machine registration*, not for every write).
- **12 bits of sequence** — a local counter that resets to 0 every millisecond, allowing up to 2^12 = 4,096 unique IDs *per machine, per millisecond* before it would need to wait for the next tick.

That last number is worth sanity-checking against our actual load: our worst-case write estimate was maybe a few hundred QPS even under generous growth assumptions — 4,096 IDs/ms *per machine* is 4,096,000 IDs/sec per machine, which is wildly more headroom than we need. This over-provisioning is deliberate and cheap — bits are free at design time, coordination is not.

**What changed — write flow (`POST /shorten`), now with local ID generation instead of `BIGSERIAL`:**

1. Client sends `POST /shorten` with `{"long_url": "https://example.com/..."}`.
2. Request hits App Server instance `machine_id = 7` (assigned at boot, stored in local config/env).
3. App Server's local Snowflake generator does, entirely in-process, no network call:
   - Read current time → `1723999999123` ms since custom epoch.
   - Check local sequence counter for this millisecond — if this is the first ID this ms, `sequence = 0`.
   - Bit-shift and OR together: `id = (timestamp << 22) | (machine_id << 12) | sequence`.
4. App Server converts `id` → base62 → e.g. `"8mZq2X"`.
5. App Server determines which shard owns this write — typically routed by hashing the short code or by the machine's own fixed shard assignment (App Server 7 always writes to `shard-1`, say).
6. App Server runs against `shard-1`: `INSERT INTO urls (id, long_url, short_code) VALUES (8817234958, 'https://example.com/...', '8mZq2X');` — note `id` is now supplied directly by the app, not auto-generated by the DB, since uniqueness is already guaranteed before the query is even issued.
7. Returns `{"short_url": "https://tiny.ly/8mZq2X"}`.

```
                 machine_id=7 (fixed at boot)
                        │
Client → App Server 7 ──┤  local: (timestamp<<22)|(machine_id<<12)|seq
                        │
                        └──> INSERT into shard-1  (no cross-shard call, no lock)

App Server 12 (different box) generates IDs independently, same instant,
guaranteed distinct because machine_id differs → writes to shard-3.
```

**Read flow**: unchanged in mechanism (cache → replica fallback from Iteration 2), except now the App Server needs to know *which shard* to query for a given short code before it can even check cache/DB — we'll need a routing layer for that, which is genuinely the next problem, not this one.

**What we gained:**
- **True zero-coordination writes.** No machine ever talks to another machine or a central counter to generate an ID. Throughput scales linearly by just adding more App Server + shard pairs.
- IDs are **roughly time-sortable** (since timestamp is the highest-order bits), which is a nice free side-benefit for debugging, pagination, and eyeballing "was this link made before or after that one."
- No existence-check needed before insert (unlike the hash-the-URL approach) — the ID's structure *is* the uniqueness proof.

**What we gave up / new problems introduced:**
1. **Clock dependency.** If a machine's system clock jumps backward (NTP correction, VM migration hiccup), it could theoretically regenerate a timestamp it already used, risking a collision with its own prior IDs. Real Snowflake implementations detect this explicitly: if `current_time < last_seen_time`, the generator refuses to issue IDs and either waits or errors, rather than silently risking a collision.
2. **Machine ID assignment becomes a small but real operational problem.** Somebody/something has to guarantee no two App Servers ever get the same `machine_id` — typically solved with a lightweight coordination service (Zookeeper, etcd) used *only* at boot time to claim an ID, not on the hot path. This is a much smaller ask than coordinating every write, but it's not literally zero infrastructure.
3. **Sequence exhaustion within a millisecond** — vanishingly unlikely at our scale (we're nowhere near 4,096 writes/ms/machine), but the generator does need to handle it gracefully: spin/wait for the next millisecond tick rather than overflow into the machine-ID bits.

**What we considered and rejected:**
- *Option A (centralized ticket server handing out ID blocks)* — rejected as the primary mechanism because it reintroduces a single service every App Server must periodically call, and under our target scale that service becomes a bottleneck/SPOF exactly like the thing we're trying to eliminate — even though block-fetching is infrequent, it's still a coordination dependency we don't need when Option B gets us to zero.
- *Option C (hash-based, e.g., MD5 truncated)* — rejected because collisions are probabilistically inevitable as the table grows (birthday paradox), forcing a `SELECT`-before-`INSERT` existence check on every write, which is a cross-shard serialization point that gets worse with scale, not better — the opposite direction we want.

---

**Likely interviewer follow-ups:**

- *"What if two machines briefly have the same machine_id due to a mis-deploy?"* → That's the one scenario Snowflake genuinely can't self-heal from — it's why machine ID assignment needs a real guarantee (a coordination service enforcing exclusivity), not just "hope config is right." Worth naming as the one hard dependency the whole scheme rests on.
- *"Why 41/10/12 bits specifically, why not e.g. more machine-ID bits?"* → It's a direct trade against expected scale: 10 bits (1,024 machines) comfortably exceeds any realistic fleet size for this product, while 12 bits of sequence gives huge per-machine headroom; if you genuinely expected >1,024 machines you'd shift bits from sequence to machine-ID, since the total must stay at 63 usable bits.

---

Next up: App Servers now generate IDs locally and know which shard *they* write to — but reads are a different problem. Given an arbitrary short code typed into a browser, how does any App Server know *which of N shards* even holds that row? That routing question is what we tackle next.

---

## Iteration 4: Routing Reads — Which Shard Holds This Code?

Here's the gap Iteration 3b left open: writes were easy to route because each App Server had a *fixed* shard it always wrote to (machine 7 always writes to shard-1). But a **read** for `GET /8mZq2X` can land on *any* App Server, from *any* client, with no memory of which machine originally created that code. So: given a short code, which of `shard-0` through `shard-3` even has the row?

The analogy: this is a coat-check problem. If you hand your coat to attendant #3, and later a *different* attendant needs to find it, they can't just guess — either every attendant needs a shared ledger ("ticket #8817 → rack 3"), or the ticket number itself needs to *encode* which rack it went to, so any attendant can read the ticket and know instantly, no ledger lookup required.

We actually already have the second option sitting right there, unused: **the machine_id embedded in the Snowflake ID itself.**

**The mechanism:** decide shard assignment as a deterministic function of the ID's embedded machine_id (or more precisely, extend the scheme slightly so the ID directly encodes a `shard_id` — in practice teams often just make `machine_id` and `shard_id` the same concept, or compute `shard_id = machine_id % num_shards`). Since the timestamp/machine/sequence bits are all still sitting inside the base62-decoded ID, **any App Server can decode a short code back into its numeric ID, extract the shard bits, and know exactly where to query — with no lookup, no coordination, no ledger.**

**What changed — read flow (`GET /{code}`):**

1. Client hits `GET /8mZq2X`.
2. App Server (any of them — doesn't matter which one) reverse-decodes: base62 `"8mZq2X"` → integer `id = 8817234958123`.
3. App Server extracts the shard bits: `shard_id = (id >> 12) & 0x3FF` (i.e., pull out the 10 machine-ID bits, mask to shard count) → `shard_id = 1`.
4. **Branch — cache check first (unchanged mechanism from Iteration 2, just now cache is either a single shared Redis cluster or itself keyed/routed similarly)**:
   - **Hit**: `GET short:8mZq2X` on Redis → returns long URL directly, done, shard routing above was computed but the DB was never touched.
   - **Miss**: proceed to step 5.
5. App Server queries specifically `shard-1`'s replica (not a blind broadcast to all shards): `SELECT long_url FROM urls WHERE short_code = '8mZq2X';`
6. Backfill Redis: `SET short:8mZq2X "https://example.com/..." EX 86400`.
7. Respond `301` with `Location: <long_url>`.

```
Client → App Server → decode base62 → extract shard_id from ID bits
                            │
                            ▼
                    "this belongs to shard-1"
                            │
                            ▼
                Redis (hit?) ──miss──> shard-1 replica only
                    │hit                      │
                    └──────────> return long_url <──── backfill
```

**What we gained:**
- **No routing table, no lookup service, no "ask a coordinator which shard" step.** Routing is computed in-process from the code itself — same zero-coordination property that made ID generation cheap now makes *routing* cheap too. This is the payoff of choosing an ID scheme that's structured rather than opaque.
- Reads only ever hit *one* shard's replica set, never fan out to all shards to "find" the row — critical, because a fan-out-to-all-shards read pattern would mean every single redirect costs N database queries instead of 1, which defeats the entire point of sharding.

**What we gave up / new problems introduced:**
1. **Shard count is now baked into the ID format at birth.** If Priya wants to go from 4 shards to 8 shards later (resharding), every *existing* short code's embedded shard bits still point to its original shard — that's actually fine and desired (old codes still resolve correctly) — but *new* machines need shard-ID assignment that doesn't collide with the old scheme, and any capacity-rebalancing logic needs to account for old codes living permanently on their original shards rather than being freely relocatable. This is a real operational constraint worth naming, not a blocker.
2. **Uneven shard load if traffic isn't evenly distributed across machine IDs at write time** — e.g., if App Servers 0-3 (mapping to shard-0) happen to be behind a particular load balancer pool that gets more traffic, shard-0 grows hotter than shard-3. Mitigated at the load-balancer layer (even distribution across App Servers) rather than at the ID/routing layer.
3. **This only works because we chose a *structured*, decodable ID.** If Priya had gone with Option C (hash the URL) instead back in Iteration 3b, there'd be no embedded shard information at all, and she'd be forced into either a lookup table (extra hop, extra dependency) or consistent hashing over the short code itself (viable, but a different, heavier mechanism) to answer "which shard?" This is a quiet but real payoff of the Snowflake decision compounding into this iteration.

**What we considered and rejected:**
- *"Maintain a separate shard-lookup table/service (`short_code → shard_id`), consulted on every read"* — rejected because it reintroduces exactly the kind of extra network hop and shared-state dependency we spent Iteration 3b eliminating; it would work, but it's strictly worse than "the answer is already encoded in the thing you already have."
- *"Just broadcast the read to all shards and take whichever one answers"* — rejected outright: turns every single redirect into N queries instead of 1, multiplying database load by shard count for zero benefit, the opposite of why we sharded in the first place.

---

**Likely interviewer follow-ups:**

- *"What happens when you need to add shard-4 and shard-5 later — how does that migration actually work?"* → New App Servers get new machine_ids that map to the new shards; all *newly created* codes route correctly from day one. Existing codes keep working unchanged since their shard bits already point at valid, still-running old shards — so this is an additive, not a migratory, operation; you're not required to physically move any existing rows.
- *"Doesn't decoding the ID on every read add latency?"* → Negligible — it's a bitmask and a shift, sub-microsecond in-process CPU work, nothing close to a network call. Worth saying explicitly if asked, because interviewers sometimes probe whether you understand the cost difference between "in-process computation" and "network hop."

---

We now have: writes generating unique IDs with zero coordination, and reads routing to the correct shard with zero coordination, all backed by a cache for the hot path. Next up: we've been assuming one region so far — what breaks when `tiny.ly` goes global, and a user in Mumbai is still round-tripping to a primary shard sitting in `us-east-1`?

---

## Iteration 5: Going Global — Multi-Region

Here's the concrete trigger: our Mumbai user, call her **Ananya**, clicks a `tiny.ly` link. Every hop we've built so far — App Server, Redis, shard replica — is sitting in `us-east-1`. Physics doesn't negotiate: a round trip from Mumbai to Virginia is ~230-250ms of pure speed-of-light-in-fiber cost, before any actual work happens. For a redirect — a product whose entire value proposition is "instant" — that's brutal. Compare that to a user in Virginia hitting the same link: ~5-10ms. Ananya gets a noticeably worse product for the crime of living somewhere else, and there's nothing our caching or sharding work fixed, because none of it addressed *geography*, only *load*.

The analogy: this is a chain of coffee shops with one central roastery. If every customer in every city has to wait for beans shipped fresh from the one roastery each morning, distance alone determines freshness and wait time — no amount of making the roastery *bigger* or *faster internally* helps the Tokyo customer, only opening a roastery closer to Tokyo does.

**Split the problem by read vs. write, because they have very different globalization needs:**

**Reads are the easy 90% win.** Redirects are, by definition, read-only and (per our scope) tolerant of a few hundred ms of staleness already — we established that back in Iteration 1. So: deploy **regional read replicas** (extending the Iteration 1 mechanism, not replacing it) — `ap-south-1` (Mumbai region) gets its own replica streaming WAL from each shard's primary, plus its own **regional Redis cache**. Layer a **CDN or geo-DNS/anycast routing** in front so Ananya's request never leaves her region for a cache-hit or replica-served read.

**What changed — read flow (`GET /{code}`), now with regional routing added at the front:**

1. Ananya's client hits `GET /8mZq2X` — DNS/anycast resolves her to the **nearest** edge/App-Server region, `ap-south-1`, not `us-east-1`.
2. `ap-south-1` App Server decodes base62 → shard_id (unchanged mechanism from Iteration 4).
3. Check **regional** Redis cache in `ap-south-1`: `GET short:8mZq2X`.
   - **Hit**: return long URL, entire request stayed inside `ap-south-1`, ~10-20ms round trip for Ananya. Done.
   - **Miss**: query the **regional read replica** of the relevant shard (a replica physically in `ap-south-1`, streaming from the primary wherever it lives), backfill regional Redis, return. Still no cross-ocean hop for the *read itself* — only the very first, rare cross-region replication stream that keeps the replica warm operates in the background, not on the request path.

```
Ananya (Mumbai) ──> geo-DNS/anycast ──> ap-south-1 App Server
                                              │
                                    decode → shard_id
                                              │
                              ap-south-1 Redis (hit?) ──miss──> ap-south-1 replica
                                              │hit                     │
                                              └────> long_url <────────┘

(background, not on request path:)
us-east-1 shard primary ──WAL stream──> ap-south-1 replica
```

**Writes are the hard 10%.** `POST /shorten` needs a globally unique ID — and our Snowflake scheme already handles multi-machine uniqueness *within* a region, but if Ananya's write goes to an `ap-south-1` App Server with its own machine_id pool, and a Virginia user's write goes to a `us-east-1` App Server with a *different* machine_id pool, they still can't collide **as long as machine_id assignment is globally exclusive**, not just regionally exclusive — i.e., `ap-south-1` and `us-east-1` machine pools must never overlap in their assigned IDs. That's a small but real extension of the machine-ID-assignment coordination we already flagged as a soft dependency back in Iteration 3b — it just needs to be global-scope, not regional-scope, which in practice means the same lightweight coordination service (etcd/Zookeeper) just needs a global view, still only touched at boot, still not on the hot path.

Where does Ananya's *write* actually land, physically? Two honest options, and this is a real trade-off, not a solved problem:
- **Write to the nearest region's primary** (a true multi-primary, multi-region setup) — fastest writes for everyone, but now requires careful conflict handling if the same logical shard has primaries in multiple regions accepting concurrent writes (mitigated here because each *shard* still has exactly one primary — we're not making shards multi-primary, just distributing *which region* each shard's single primary lives in, e.g., shard-1's primary sits in `ap-south-1`, shard-3's sits in `us-east-1`).
- **Write to a single "home" region always, accept the latency hit on writes only** — simpler to reason about, and defensible here because writes are ~2-3 QPS and rare per-user (someone shortens a link once, then it's read thousands of times) — the 200ms penalty lands on a much smaller, much less latency-sensitive slice of traffic than reads do.

Given our actual write volume, the second option — accept write latency, optimize read latency — is the pragmatic, interview-defensible choice, and it's a nice concrete instance of a general principle: **you optimize the path that's hot, not the path that's rare**, even if the rare path is individually slower.

**What we gained:**
- Read latency for the vast majority of global users drops from ~230ms to ~10-20ms — a huge, directly-felt product improvement, for the 90%+ of traffic that's reads.
- Regional replicas also add *another* layer of blast-radius protection: a full `us-east-1` region outage no longer takes down redirects for users being served out of `ap-south-1`'s cache/replica (though writes for shards whose primary lives in the downed region would still be affected).

**What we gave up / new problems introduced:**
1. **Replication lag is now cross-continent, not cross-rack** — the WAL stream from a `us-east-1` primary to an `ap-south-1` replica has real network latency and jitter on top of the local-lag we already accepted in Iteration 1, so "eventual" consistency's window widens from milliseconds to potentially low seconds under network stress. Still fine for our use case (immutable URLs), but worth naming as a magnitude change, not just a repeat of the same trade-off.
2. **Data sovereignty / compliance** enters the picture for the first time — some jurisdictions (EU under GDPR, for instance) care where data is physically stored and replicated, which can constrain "just replicate everything everywhere" as a blanket strategy. Not deeply relevant to plain URL redirection data, but real production systems can't ignore it, and it's a legitimate follow-up an interviewer might raise.
3. **Operational surface area roughly multiplies per region** — more Redis clusters, more replicas, more monitoring dashboards, more places for a bad deploy to hide. This is a real cost, not free.

**What we considered and rejected:**
- *"Just replicate the entire database to every region, full multi-primary, everywhere accepts writes for everything"* — rejected as overkill for our write volume; the coordination complexity of true multi-primary conflict resolution (e.g., vector clocks, last-write-wins semantics, or consensus protocols like Raft across regions) is a large engineering cost to pay for a write path that's doing a couple QPS globally. We're solving a read-latency problem; multi-primary-everywhere solves a write-latency problem we don't have.
- *"CDN-cache the redirect response itself at the edge, skip App Servers entirely for cache hits"* — not rejected, actually a nice additional layer worth mentioning: since our links are 301s (permanent redirects) and immutable, a CDN edge node can cache `GET /8mZq2X → 301 Location: ...` directly, serving repeat global traffic without even reaching our regional App Server. This stacks on top of, rather than replaces, the regional Redis layer — most useful for extremely hot links across a wide geography.

---

**Likely interviewer follow-ups:**

- *"What if a write needs to go to a shard whose primary is in a region that's currently down?"* → That shard's writes fail/queue until the primary is promoted from a replica (same failover mechanism from Iteration 1, just now cross-region) — other shards, whose primaries live elsewhere, are entirely unaffected, which is a nice isolation property of per-shard (not global) primaries.
- *"How do you decide which region owns which shard's primary?"* → Typically by where that shard's *creation* traffic is heaviest (data locality follows write locality) — though for a product like this with fairly uniform global write volume, it may just be an arbitrary even split across regions for simplicity, since write latency differences are tolerable per our earlier trade-off.

---

We now have global reads, regional caching, sharded/routable writes, and cross-region replication. Next up (the last piece before we assemble the full picture): a quick pass on load balancing algorithm choice and health checks — how does Ananya's request actually *find* the `ap-south-1` App Server pool in the first place, and what happens when one specific App Server instance inside that pool goes unhealthy?

---

## Iteration 6: Load Balancing & Health Checks

Two separate routing questions got glossed over so far, and interviewers will pull on both threads: **(1)** how does Ananya's request even find `ap-south-1` before it finds a specific App Server, and **(2)** once inside `ap-south-1`, how does traffic avoid the one App Server instance that just started throwing errors?

The analogy: think of an airport. Which *city* you fly into (geo-DNS picking `ap-south-1` vs `us-east-1`) is a completely different decision from which *gate agent* checks you in once you're in the terminal (load balancer picking a specific healthy App Server). Different layers, different mechanisms, different failure modes.

**Layer 1 — which region (already covered in Iteration 5):** geo-DNS or anycast routing gets Ananya's request to the nearest region's edge. Not revisiting that mechanism here.

**Layer 2 — which App Server within that region:** this is new. Inside `ap-south-1`, there might be 50 App Server instances behind a load balancer. Two design choices matter here, and they're genuinely different trade-offs, not just implementation details:

**L4 vs. L7 load balancing** — plain language first: an L4 (transport-layer) load balancer looks only at IP/port and blindly forwards packets/connections without ever reading the HTTP content inside — like a mail sorter that routes envelopes by zip code without opening them. An L7 (application-layer) load balancer actually reads the HTTP request — method, path, headers — and can route intelligently based on content, like a mail sorter who opens the envelope and routes based on what's inside.

For `tiny.ly`, **L7 is the right call**, and here's why it's not just "the fancier option": since `GET /{code}` and `POST /shorten` have very different performance profiles (reads are cache-hit-fast and vastly more frequent; writes always hit a database), an L7 load balancer can route them differently — e.g., health-check and pool App Servers for reads separately from write-handling servers, or apply different timeout/retry policies per path. An L4 balancer, blind to the URL path, can't make that distinction; it just spreads TCP connections evenly regardless of what's inside them.

**Algorithm choice** — "spread requests evenly" sounds like it should just mean round-robin, but that's naive here: cache-hit requests (Branch A from Iteration 2) finish in ~1ms; cache-miss requests that fall through to the DB (Branch B) take meaningfully longer. Pure round-robin can pile slow, DB-bound requests onto one server while a neighboring server sits comparatively idle, purely by bad luck of the draw. **Least-connections** (route to whichever healthy server currently has the fewest in-flight requests) adapts to that reality automatically — a server that's currently stuck waiting on slow cache-miss DB queries naturally receives fewer *new* requests until it catches up, without anyone hand-tuning weights.

**Health checks — the other half of this iteration.** The load balancer periodically hits each App Server on a lightweight endpoint, e.g. `GET /healthz`, expecting a fast `200 OK`. If an instance misses N consecutive checks (say, 3 checks at 5-second intervals — 15 seconds to detect), the LB pulls it from the rotation *before* real user traffic keeps getting routed to a server that's already failing. This is what actually makes "one App Server crashes" a non-event instead of a burst of user-facing 500s.

**What changed — request flow, health-check branch made explicit:**

1. LB sends `GET /healthz` to App Server instance every 5s.
2. **Branch — healthy**: instance responds `200 OK` within timeout (e.g., 2s) → stays in rotation, eligible for least-connections routing.
3. **Branch — unhealthy**: instance times out or returns non-200, 3 consecutive times → LB removes it from the pool immediately; in-flight requests already assigned to it either complete or hit their own client-side timeout and get retried (with backoff+jitter, not instantly hammered) against a *different* healthy instance.
4. Once the instance recovers and passes checks again (or a fresh instance replaces it via auto-scaling), it's added back into rotation gradually — often with a brief "slow start" ramp so a just-recovered server isn't instantly slammed with a full share of traffic before its caches/connections warm up.

```
Ananya ──> geo-DNS ──> ap-south-1 LB (L7, least-connections)
                              │
                    ┌─────────┼─────────┬──────────┐
               App Srv A  App Srv B  App Srv C   App Srv D (unhealthy,
              (healthy)  (healthy)  (healthy)     pulled from rotation)
                    ▲
              healthz polled every 5s by LB
```

**What we gained:**
- A single bad App Server instance degrades gracefully (its share of traffic quietly redistributes) instead of surfacing as user-facing errors.
- Read/write traffic can be shaped differently at the routing layer, matching the actual cost asymmetry between the two paths we've built this whole design around.

**What we gave up / new problems introduced:**
- **L7 costs more per request than L4** — actually parsing HTTP headers/paths is more CPU work than blind packet forwarding, so L7 LBs have a lower raw throughput ceiling per instance than L4. For our QPS (low thousands, not millions), this is a non-issue; it would matter at Google/Cloudflare-scale edge traffic.
- **Health check false negatives** — a transient blip (one slow GC pause, one momentarily saturated network link) can trip a health check and pull a perfectly good server out of rotation for a control-loop cycle, wasting capacity right when you might need it most (e.g., during a traffic spike). This is why the "3 consecutive failures" threshold exists — a compromise between fast detection and tolerance for noise, not an arbitrary number.

**What we considered and rejected:**
- *"Just use round-robin, it's simpler"* — not wrong for uniform-cost requests, but rejected here specifically because our two request types (cache-hit reads vs. cache-miss/write) have meaningfully different costs, and round-robin's blindness to that leads to uneven real load despite even request *counts*.
- *"Skip health checks, rely on the client retrying on failure"* — rejected because it pushes the cost of detecting failure onto every single user request instead of a background process, meaning real users eat failed requests and retry latency instead of the system self-healing before they ever notice.

---

**Likely interviewer follow-ups:**

- *"What if the load balancer itself becomes a bottleneck or fails?"* → LBs themselves typically run redundantly (active-active pairs, or a managed cloud LB service that's already replicated under the hood) sitting behind the DNS/anycast layer — the same "no single box" principle from Iteration 1 applies recursively to the LB layer itself.
- *"Client-side retries — how do you avoid a retry storm during an outage?"* → Exponential backoff with jitter (each failed client waits a randomized, growing interval before retrying, not a fixed one) so thousands of clients don't all retry in perfect lockstep and re-create the exact overload that caused the failure in the first place.

---

That covers the last structural piece. Next up: a short pass on observability (metrics/logs/tracing — what you'd actually *watch* to know this system is healthy or catching fire), then the full recap: complete architecture diagram, the "why not X" cheat sheet, and your choice of drilling deeper or a mock interview pressure-test.

---

## Iteration 7: Observability — What Tells You It's on Fire Before Users Do

Everything we've built has failure modes we've named explicitly along the way — replication lag, cache stampedes, hot shards, clock skew, unhealthy instances. Observability is the layer that turns "this *could* happen" into "we'd know within seconds if it *is* happening." Three distinct tools, each answering a different question — this distinction matters because interviewers will ask "metrics vs. logs vs. tracing, when do you reach for which."

**Metrics — "is something wrong, right now, in aggregate?"** Numbers over time, cheap to store, cheap to alert on. For `tiny.ly` specifically, the metrics that map directly to the failure modes we designed around:

- **Cache hit rate** (Redis hits ÷ total reads) — this is the single most important number in the whole system. If it drops from 99% to 80% suddenly, that's either a fresh viral link stampeding (Iteration 2's exact failure mode) or Redis itself degrading — and it's a leading indicator that database load is about to spike *before* the database itself looks unhealthy.
- **Replication lag** (seconds behind primary, per replica, per region) — directly measures how stale a "stale read" from Iteration 1/5 actually is right now. Alert if it exceeds a few seconds.
- **P50/P99/P999 latency, split by read vs. write, split by region** — averages hide exactly the problem we care about most: a P50 of 15ms can coexist with a P999 of 4 seconds if one shard is struggling, and averaging across regions would hide Ananya's Mumbai latency inside a healthy-looking global average.
- **Per-shard QPS and error rate** — the direct signal for "shard-1 is hot" (uneven load from Iteration 4's follow-up #2) versus every shard being evenly loaded.
- **Health-check pass/fail counts per App Server pool** — feeds straight from Iteration 6.

**Logs — "what exactly happened for this one request?"** Structured (JSON, not free text) so they're queryable — e.g., every request logs `{request_id, short_code, shard_id, cache_hit: bool, latency_ms, region}`. When a specific user reports "my link doesn't work," logs are how you find *that one request* instead of staring at aggregate dashboards.

**Tracing — "as this request crossed multiple services, where did the time actually go?"** A single `GET /{code}` in our final architecture touches: LB → App Server → decode → Redis → (maybe) replica → response. Distributed tracing (e.g., OpenTelemetry, propagating a `trace_id` through every hop) lets you see a waterfall: 0.1ms to decode, 0.3ms Redis check, 45ms replica query on cache miss — immediately telling you *where* a slow request's time went, rather than guessing.

**The connective tissue — alerting thresholds tied to the failure modes we actually designed for**, not generic "CPU > 80%" noise:
- Cache hit rate < 95% sustained for 2 minutes → page (possible stampede in progress).
- Replication lag > 5s on any replica → page (staleness window widening beyond acceptable).
- Any single shard's error rate > 1% → page (possible hot-shard or primary failure, isolated).
- Circuit breaker (Iteration 2, Redis dependency) tripped → page (cache layer degraded, DB about to feel it).

This closes the loop: every "what we gave up" we named across six iterations now has a corresponding metric that tells you, in production, whether that trade-off is currently being paid for cheaply or expensively.

---

## Full Architecture Recap

**Write path:**
```
Client → POST /shorten {long_url}
   → App Server (fixed machine_id, e.g. 7)
       → generate id = (timestamp<<22)|(machine_id<<12)|sequence   [Snowflake, zero-coordination]
       → base62(id) → short_code
       → INSERT INTO shard-N (id, long_url, short_code)            [N = machine_id's assigned shard]
   → return {short_url}
```

**Read path:**
```
Client → GET /{short_code}
   → geo-DNS/anycast → nearest region (e.g. ap-south-1)
   → L7 LB (least-connections, health-checked pool)
   → App Server:
       decode base62 → id → extract shard_id (bitmask, in-process)
       → regional Redis: GET short:{code}
           ── hit ──> return 301 Location: long_url
           ── miss ─> regional replica of shard_id: SELECT long_url ...
                       → backfill Redis (TTL, LRU eviction)
                       → return 301 Location: long_url
```

**Full picture:**
```
                     ┌── ap-south-1 ──────────────────┐   ┌── us-east-1 ─────────────────┐
Client(Mumbai) ──DNS→│ LB → App Servers → Redis(local) │   │ LB → App Servers → Redis(local)│←── Client(Virginia)
                     │            │                     │   │            │                    │
                     │      shard replicas (1,3)         │   │      shard replicas (0,2)        │
                     └────────────┼─────────────────────┘   └────────────┼──────────────────────┘
                                  │                                      │
                          WAL stream (async, cross-region)               │
                                  ▼                                      ▼
                     shard-1 primary (ap-south-1)          shard-0 primary (us-east-1)
                     shard-3 primary (ap-south-1)          shard-2 primary (us-east-1)

Cross-cutting: circuit breakers (Redis dependency), health checks (App Servers),
metrics/logs/tracing (cache hit rate, replication lag, per-shard error rate, P999 latency)
```

## The "Why Not X" Arsenal

1. **"Why not just use UUIDs?"** → Unique and coordination-free, but 128 bits / 36 chars defeats "short" URL, and random ordering hurts index locality and time-sortability.
2. **"Why not hash the URL (MD5/SHA) for the code?"** → Truncated hashes collide (birthday paradox), forcing a pre-insert existence check that becomes a cross-shard serialization bottleneck as the table grows.
3. **"Why not a centralized ID-generation service?"** → Works, but recreates a single bottleneck/SPOF every writer must call — Snowflake's local generation achieves true zero-coordination instead.
4. **"Why not just add more read replicas instead of caching?"** → Spreads waste across more machines instead of eliminating it; one hot key still costs a full DB query per hit — Redis answers 100x faster at a fraction of the cost.
5. **"Why not broadcast reads to all shards?"** → Multiplies every redirect's cost by shard count for zero benefit; embedding shard_id in the ID itself makes routing free.
6. **"Why not full multi-primary, multi-region writes everywhere?"** → Solves a write-latency problem we don't have (2-3 QPS) at the cost of real conflict-resolution complexity (Raft/vector clocks) — we optimize the hot path (reads), not the rare one.
7. **"Why not round-robin load balancing?"** → Blind to the real cost asymmetry between cache-hit and cache-miss requests; least-connections adapts automatically.
8. **"Why not skip health checks and let clients retry on failure?"** → Pushes failure-detection cost onto every user request instead of a background loop catching it first.

---

Want to (a) drill deeper into any single piece — e.g., the Snowflake clock-skew edge case, or resharding mechanics — or (b) do a mock Q&A where I play interviewer and pressure-test the whole design?
