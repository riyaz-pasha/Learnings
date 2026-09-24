## Origin Hook

It's 1994. The web has roughly a few thousand pages, and Jerry Yang and David Filo are manually bookmarking sites into a directory — "Yahoo!" — because that's genuinely how you found things. Within two years the web doubles past what any human can curate by hand, and a research project at Stanford (the future Google) realizes the only way to keep a map of the web current is to build a program that behaves like a diligent, tireless intern: start from a few known pages, read every link on them, go visit those, repeat forever. The problem was never "can we download a webpage" — that's a GET request. The problem is that the web is simultaneously *enormous*, *constantly changing*, and full of *impolite traps* (infinite calendar pages, duplicate mirrors, servers that will ban you for hitting them too hard) — and you have to discover it without a map, while not getting stuck or making enemies.

## Scoped Requirements

**P0/P1 — these drive the design:**

1. **Scale of coverage** — crawl on the order of billions of web pages (say, 1B+ URLs) within a reasonable refresh window. This is the one that forces distribution, sharding, and dedup at scale.
2. **Politeness** — never hammer a single host with concurrent/rapid requests; respect `robots.txt` and crawl-delay. This is deceptively the *hardest* constraint — it's the one that will generate the most "why not just X" discussion, because naive scaling solutions (just add more workers!) actively make this worse, not better.
3. **Freshness / re-crawl policy** — pages change at different rates (news homepage vs. a static PDF), and the crawler needs to revisit accordingly rather than treating every URL as crawl-once.
4. **Extensibility of the frontier** — new URLs are discovered continuously from parsed pages and must be dedup'd against a massive already-seen set without that check becoming the bottleneck.

**Crux requirement:** #2, Politeness, combined with #1's scale. The naive "just parallelize" instinct is exactly wrong here, and untangling *why* — and what data structure actually solves it — is where we'll spend most of our narrative time (this is basically a distributed rate-limiter/queueing problem wearing a crawler costume).

**Explicitly cutting (P2, no major architectural impact):**
- Full-text search indexing / ranking (that's a downstream consumer of crawled content, not the crawler's job)
- JavaScript rendering for SPA-heavy sites (real systems handle this, but it's a "swap in a headless browser at the fetch step" detail, not a new architecture)
- Deep content-quality/spam classification
- Sitemap.xml prioritization nuances — mention in passing, no dedicated iteration

Does this scope look right to you, or do you want JS rendering pulled in as a core requirement? Once you confirm, I'll start Day 0.

---

Taking that as confirmation on the scope — let's build.

## Day 0: The Dumbest Thing That Could Work

**Meet the setup.** One machine. One process. A single MySQL table called `urls`, and a Python script running in a `while True` loop.

```sql
CREATE TABLE urls (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  url VARCHAR(2048) UNIQUE,
  status ENUM('pending','in_progress','done') DEFAULT 'pending',
  discovered_at TIMESTAMP DEFAULT NOW()
);
```

The loop:

1. `SELECT * FROM urls WHERE status='pending' LIMIT 1`
2. `requests.get(url)` — fetch the page, synchronously, and wait for the response.
3. Parse the HTML with something like BeautifulSoup, pull out every `<a href>`.
4. For each new link: `INSERT IGNORE INTO urls (url) VALUES (?)` — the `UNIQUE` constraint on `url` is doing our deduplication for us.
5. Save the page body to local disk as a flat file.
6. `UPDATE urls SET status='done' WHERE id=?`
7. Go back to step 1.

```
┌──────────┐   pick pending URL   ┌───────────┐
│  MySQL   │◄────────────────────►│  Crawler   │──► GET page
│  urls    │   insert new links    │  (1 proc)  │◄── HTML
└──────────┘                       └────┬──────┘
                                         │
                                         ▼
                                  local disk (HTML files)
```

**Why this is a legitimate starting point, not a strawman:** it gives you a genuinely simple correctness guarantee — *every URL is fetched exactly once, and the "have I seen this?" check is a single atomic unique-key insert*, so there's no possible way to double-crawl a page or lose a discovered link. There's no distributed coordination problem yet because there's nothing distributed. If you only needed to crawl, say, one company's own 50,000-page documentation site, this is roughly a reasonable answer — you'd tune it, but you wouldn't need to rearchitect it.

It also happens to already respect politeness *by accident*: since it's single-threaded and synchronous, it can literally only ever hit one host at a time, at whatever pace `requests.get()` takes to return. Fewer resources, but zero risk of accidentally DDoS-ing anyone.

## Break It

Let's put a number on "billions of pages." Say the target is 1 billion pages crawled once every 30 days (a reasonable freshness bar for "the median web page").

```
1,000,000,000 pages / 30 days
= 33,333,333 pages/day
÷ 86,400 seconds/day
≈ 386 pages/second sustained average
```

Now check Day 0's actual throughput. A synchronous fetch-parse-store cycle — network round trip, DNS lookup on cache miss, parsing, a couple of DB round trips — is optimistically ~200-500ms per page on a good day, more when a server is slow to respond. That's **2-5 pages/second**, single-threaded.

To hit 386 pages/sec at that per-page cost, you'd need on the order of **100-1,000 concurrent fetches in flight at any moment** just to keep up with *average* load, before you even think about traffic being bursty around specific events (a breaking-news site gets re-crawled aggressively; everything else waits).

Here's the specific worked failure: imagine `pending` fills up with 50,000 URLs that all happen to be from `nytimes.com` — a very normal thing to happen, since one homepage crawl discovers hundreds of links to the same domain. Someone's obvious "fix" for the speed problem is: *just spin up 200 threads pulling from the same query.* Do that, and 200 threads immediately start hammering nytimes.com's servers concurrently, because nothing in this design tracks "which host is currently being hit." That's not a hypothetical — that's an accidental denial-of-service attack against a news organization, from code you wrote to be helpful. nytimes.com's ops team notices, and now your crawler's IP range is in their firewall's blocklist, permanently, and word travels between site operators about which crawlers to block on sight.

So the naive scaling move — "just add more workers reading from the same queue" — doesn't just fail to help, it actively makes the *worse* problem (politeness) worse while barely touching the *real* problem, because a single MySQL table with `SELECT ... LIMIT 1` and no host-awareness has no concept of "spread this load across hosts" baked in at all.

Next time: we fix raw throughput first with the boring move (concurrency + separating fetch from parse), and I'll show you exactly why that boring move alone still isn't enough to survive contact with nytimes.com — setting up why politeness needs its own dedicated data structure, not just "more workers."

Got it / next?

---

## Iteration 1: Concurrency + Decoupling Fetch from Parse

**The scenario:** Alice (imagine she's the engineer on call) does the obvious first fix. She swaps the single loop for a pool of 200 async worker coroutines, all pulling from the same `pending` queue, and — reasonably — separates *fetching* from *parsing* into two stages, because parsing HTML is CPU-bound and fetching is I/O-bound; mixing them means your CPU sits idle waiting on network I/O. So:

- A pool of **Fetcher workers** (async, e.g. `asyncio` + `aiohttp`) just do `GET url → raw HTML`, then drop it onto a queue.
- A pool of **Parser workers** consume raw HTML, extract links, and push new URLs back into `pending`.

```
                 ┌─────────────┐
   pending  ────►│  Fetcher    │──┐
   queue         │  pool (200) │  │  raw HTML
                 └─────────────┘  ▼
                            ┌─────────────┐
                            │  Parsed     │
                            │  queue      │
                            └──────┬──────┘
                                   ▼
                            ┌─────────────┐
   new URLs ◄────────────── │  Parser     │
   → pending                │  pool       │
                            └─────────────┘
```

**Why this looked reasonable:** it's the textbook fix for exactly the throughput math we just did — 200 concurrent fetchers, each averaging ~300ms per request, gives you roughly `200 / 0.3s ≈ 666 pages/sec`, comfortably past our 386/sec target. On paper, problem solved.

**The specific way it breaks:** the queue has no concept of *which host* a URL belongs to — it's just a FIFO (or a `SELECT ... LIMIT 200` batch) of URLs in whatever order they were discovered. Because one crawl of `nytimes.com`'s homepage discovers hundreds of `nytimes.com` links in a row, those links tend to land in the queue clustered together. When 200 fetchers grab a batch of "next 200 pending URLs," it's entirely plausible that 40, 60, even 100+ of them are all `nytimes.com`. Now instead of one thread politely waiting its turn, you have 60-100 concurrent connections hitting the same host in the same second — which is *worse* than Day 0, not better. You've taken the exact failure mode we identified and multiplied it by however many workers you added. Throughput went up; the thing most likely to get you IP-banned also went up, in lockstep.

The bug here isn't "not enough concurrency." It's that **concurrency and politeness are in direct tension unless something explicitly partitions work by host**, and nothing in "pool of workers reading a shared queue" does that by construction.

**What we gained:** raw throughput — theoretically past our 386 pages/sec target, and CPU-bound parsing no longer blocks I/O-bound fetching.

**What we gave up / new problem introduced:** no host-level fairness or rate control at all. Politeness got strictly worse under load, precisely when we need the crawler to behave *better* — real-world sites are non-uniformly linked, so any global queue will cluster by host in bursts.

**Alternatives considered and rejected:**
- *"Just add a global `time.sleep()` between all requests"* — rejected, because it throttles the entire crawler to be polite to the busiest host, tanking throughput for the 99% of hosts that could handle more traffic just fine.
- *"Randomly shuffle the queue before each batch"* — rejected, it's a band-aid that reduces clustering probabilistically but doesn't bound it; with a heavy-tailed link distribution (a few domains linked from everywhere), you'll still get unlucky batches, just less often. It doesn't give you a guarantee, and interviewers will ask for one.

**Follow-up questions:**
- *"Why not just rate-limit at the network layer, like a shared token bucket for the whole crawler?"* — Because that limits *total* crawler throughput, not per-host throughput. You'd still be free to send 60 concurrent requests to one host as long as your global budget allows it — the constraint needs to be host-scoped, not fleet-scoped.
- *"Could you fix this by just running one crawler process per domain?"* — That's actually close to the right instinct (isolate by host), but naively it doesn't scale — you don't know the set of domains in advance, and you'd have millions of near-idle processes for small sites. We need something that gives per-host isolation *without* per-host infrastructure.

Next up: the actual fix — a per-host queueing structure (the classic "mailbox per host" design) that gives us host isolation without one process per domain, plus where `robots.txt` and crawl-delay actually get enforced in the flow.

Got it / next?

---

## Iteration 2: The Per-Host Mailbox (Politeness, Properly)

**The analogy first.** Think of a hotel mail room, not a single inbox. If every guest's mail just piled into one bin sorted by arrival time, the mail clerk delivering it might end up walking to Room 204 four times in five minutes because that guest happens to get a lot of mail, while Room 301 waits all day. The fix a real mail room uses: **one pigeonhole slot per room.** The clerk works down the row of pigeonholes, delivers *one* item from a slot, moves to the next slot, and only comes back to Room 204's slot after visiting every other occupied room in between. No room ever gets hammered, no room gets starved — the *structure itself* enforces fairness, without the clerk needing to remember timestamps or think about it.

That's exactly the shape of the fix: instead of one shared FIFO queue of URLs, we give **each host its own queue**, and a scheduler that round-robins across host-queues rather than draining one queue with many workers.

**The concrete structure (this is essentially Mercator's design, which is what Google's original crawler used):**

- A **Host Manager** service owns a mapping `host → queue of pending URLs for that host` (in practice, a partitioned structure — think Kafka topic-per-shard or a Redis data structure per host, not literally millions of DB tables).
- Each host-queue also carries a `next_allowed_fetch_time` — a per-host cooldown timer, set from either `robots.txt`'s `Crawl-delay` directive or a sane default (say, 1 request per host per second if the site doesn't specify).
- A layer of **Fetcher workers** don't pull "the next URL" — they ask the Host Manager for "the next *host* that is both non-empty and past its cooldown," pop one URL from that host's queue, fetch it, then update `next_allowed_fetch_time = now + delay` for that host before releasing it back into rotation.

```
   host queues (per-domain)
   ┌──────────────┐
   │ nytimes.com  │───┐
   │ [u1,u2,u3..] │   │
   └──────────────┘   │
   ┌──────────────┐   │      ┌────────────┐
   │ wikipedia.org│───┼─────►│  Host      │──► "here's an eligible
   │ [u4,u5..]    │   │      │  Manager / │     host + 1 URL"
   └──────────────┘   │      │  Scheduler │
   ┌──────────────┐   │      └─────┬──────┘
   │ smallblog.io │───┘            │
   │ [u6]         │                ▼
   └──────────────┘         ┌─────────────┐
                             │  Fetcher    │
                             │  pool       │
                             └─────────────┘
```

Now 200 fetcher workers can run flat-out — but because they're all asking the *same* Host Manager for eligible hosts, and nytimes.com goes into cooldown the instant one worker grabs a URL from it, no second worker can grab another nytimes.com URL until that cooldown expires. Concurrency scales throughput across *many different hosts simultaneously* — which is exactly what you want, since crawling 200 different small sites in parallel is perfectly polite — while naturally serializing requests *within* any single host.

**Where robots.txt fits:** before a host's queue is drained for the first time, the Host Manager (or a dedicated fetch) pulls `https://host/robots.txt`, parses `Disallow` and `Crawl-delay`, caches it (say, 24h TTL), and applies it as a filter on URL admission (disallowed paths never get queued) and as the cooldown value (crawl-delay overrides the default).

**What we gained:** a structural, not probabilistic, politeness guarantee — no single host can ever have more than one request in flight at a time (or faster than its declared crawl-delay), regardless of total fleet concurrency. Throughput scales with the *number of distinct hosts in flight*, which for a general web crawl is huge.

**What we gave up / new problem introduced:** the Host Manager is now a piece of shared, stateful coordination that every fetcher talks to before every fetch — it's a potential bottleneck and a single point of failure. Also: what happens when one host (say, a monster like `blogspot.com`, hosting millions of distinct blogs at different subpaths) dominates the discovered-URL distribution? Its queue could grow to hold millions of URLs while thousands of tiny host-queues sit at length 1 — we've fixed *fairness of request rate* but not yet *fairness of queue backlog / worker starvation* if one worker pool has to hold that much state in memory.

**Alternatives considered and rejected:**
- *"Just use a global rate limiter with a much lower ceiling"* — rejected per the earlier follow-up: it can't express "no more than 1 req/sec to *this specific* host" without being host-aware, and host-awareness *is* the per-queue structure — you can't bolt it on afterward as a single number.
- *"DNS-level or IP-level throttling instead of hostname"* — worth a callout: some crawlers actually key cooldowns by **IP address**, not hostname, because many small sites share one IP on shared hosting, and hammering that IP with 50 "different" hostnames is just as impolite. Real systems (Mercator) key by IP for exactly this reason. We'll treat "host" loosely as "IP or registrable domain" going forward — good detail to mention in an interview.

| Approach | Per-host fairness guarantee | Scales with total concurrency | Handles crawl-delay |
|---|---|---|---|
| Global shared queue (Iter. 1) | No | Yes (badly — worse politeness) | No |
| Global token bucket | No (fleet-wide only) | Partially | No |
| Per-host queue + cooldown (Iter. 2) | Yes | Yes | Yes |

**Follow-up questions:**
- *"How do you pick which eligible host to serve next, if 10,000 hosts are all past cooldown at once?"* — Round-robin or a priority queue keyed by `next_allowed_fetch_time` (a min-heap gives you O(log n) "give me the next host whose cooldown has expired"), optionally weighted by priority signals like domain authority or freshness need — we'll fold in priority in the freshness iteration.
- *"What if the Host Manager itself goes down?"* — It needs to be horizontally partitioned (e.g., shard hosts across N Host Manager instances by `hash(host) % N`), each backed by durable storage (Redis with persistence, or Kafka partitions) so state survives a restart — this is the same sharding problem we'll hit for the URL frontier generally, next.

Next up: that queue-backlog imbalance I flagged (`blogspot.com` with millions of URLs vs. a host-queue of length 1) is really a sharding problem — we'll partition the whole frontier across many machines and hit the *cross-shard* headache: what happens when a single fetcher machine needs to own "all of blogspot.com" and that's still too much for one box.

Got it, or questions on the per-host design first?

---

## Iteration 3: Sharding the Frontier — and the Mega-Host Problem

**The scenario:** we now distribute the Host Manager across, say, 100 machines, sharded by `hash(host) % 100` — this is the natural next move once you accept "one Host Manager" can't hold state for tens of millions of hosts in memory on one box. Bob (another engineer) does exactly this: `shard_id = hash(host) % 100`, each shard owns a disjoint slice of hosts, and a Fetcher worker asks *its* shard for eligible hosts.

This works beautifully for the long tail — `smallblog.io`, `janes-recipes.net`, millions of low-traffic sites — each lands on some shard, contributes a queue of length 1-50, and the 100 shards end up roughly balanced, because there are millions of small hosts and `hash()` spreads them uniformly.

**The break:** `hash()` spreads *hosts* uniformly — it says nothing about spreading *URLs* uniformly, because URL count per host is wildly non-uniform. `blogspot.com` alone might account for tens of millions of distinct blog URLs (`blogspot.com/blog1`, `blogspot.com/blog2`, ...), all sharing one hostname. Under `hash(host) % 100`, every single one of those tens of millions of URLs lands on *the same shard* — because it's the same host. That one shard's queue balloons to millions of entries while its 99 siblings sit lightly loaded. Worse, the politeness rule ("one request in flight per host") means that shard can only ever have *one fetcher actively pulling from blogspot.com at a time* regardless of how many millions of URLs are queued there — so that shard's backlog doesn't just grow, it never drains, while its machine's CPU and network sit mostly idle waiting on a single-threaded trickle of 1 req/sec.

This is the classic **hot shard / hot partition** problem, and it's worth naming precisely because it's a general distributed-systems pattern, not a crawler-specific quirk (same failure shape as a hot Redis key, or a Cassandra partition for a celebrity user).

**Why the obvious fix — "just give blogspot.com its own dedicated shard" — isn't quite enough:** even a dedicated shard for blogspot.com is still bottlenecked at ~1 req/sec by the *per-host* politeness rule, because `blogspot.com` is still one hostname as far as robots.txt and crawl-delay are concerned. Giving it a whole shard just wastes 99 idle CPUs' worth of capacity on one host that can only be crawled slowly by design.

**The actual fix — subdomain/path-aware sharding key:** treat `blog1.blogspot.com` and `blog2.blogspot.com` as *separate effective hosts* for politeness and sharding purposes when the platform genuinely hosts independent sites at that granularity (this is a real, documented distinction — sites like blogspot, wordpress.com, tumblr, github.io are effectively multi-tenant, and politeness should be scoped to the *tenant*, not the platform domain). So the shard key becomes something closer to `hash(registrable_host)` where `registrable_host` resolves `blog1.blogspot.com` and `blog2.blogspot.com` to different politeness buckets, while still correctly collapsing `www.nytimes.com` and `nytimes.com` (or `nytimes.com/section/1`, `/section/2`) into the *same* bucket, since those genuinely share one server's goodwill.

```
                     hash(registrable_host) % N
   ┌────────────────────────────────────────────────┐
   │  Shard 7           Shard 42          Shard 88   │
   │ nytimes.com     blog1.blogspot.com  wikipedia   │
   │  [big but        [independent        .org       │
   │   single-tenant,  tenant, own        [big but    │
   │   ~1req/sec]      politeness]         1req/sec]  │
   │                  blog2.blogspot.com              │
   │                   [separate shard,               │
   │                    different key]                │
   └────────────────────────────────────────────────┘
```

**What we gained:** shard load now correlates with actual crawlable-in-parallel capacity rather than raw hostname string, so we stop stranding capacity on artificially-merged mega-hosts.

**What we gave up / new problem:** we now need a (maintained, imperfect) list of known multi-tenant domains to know when to split by subdomain vs. collapse to the registrable domain — this is a real operational artifact real crawlers maintain (similar in spirit to the Public Suffix List used by browsers for cookie scoping). It's a heuristic, not a clean algorithm, and it needs upkeep as new platforms emerge.

**Alternatives considered and rejected:**
- *"Just increase replication/parallelism within the hot shard's machine"* — rejected: doesn't help, because the bottleneck isn't the shard machine's compute, it's the *politeness cooldown on the host itself*. More fetcher threads on that box just contend for the same 1-req/sec slot.
- *"Cap queue length per shard and drop overflow"* — rejected as a primary fix: silently dropping discovered URLs means large legitimate sites (blogspot hosts millions of real, distinct blogs) never get fully crawled — that's a correctness/coverage regression, not an acceptable trade-off, though a bounded queue *with backpressure* (pause discovery from that host until it drains) is a reasonable defensive measure layered on top.

| Approach | Fixes hot shard? | Wastes idle capacity? | Needs maintained heuristic? |
|---|---|---|---|
| `hash(hostname)` sharding | No | No | No |
| Dedicated shard for known mega-hosts | Partially (still 1 req/s capped) | Yes (idle CPUs) | Yes (which hosts?) |
| `hash(registrable_host)`, tenant-aware | Yes | No | Yes |

**Follow-up questions:**
- *"How would you even detect a host is 'multi-tenant' without a manually maintained list?"* — Heuristics: a small set of known platforms (curated list, like the Public Suffix List), plus a runtime signal — if a hostname's discovered-URL count crosses a threshold (say 100K+ distinct paths under one host) and those paths don't share content structure, flag it for re-sharding at the subdomain level. Not perfect, but self-correcting over time.
- *"What if the number of shards N needs to change as the web grows — doesn't rehashing move everything?"* — Yes, plain `hash(host) % N` rehashes almost everything on resize. Real systems use **consistent hashing** here instead (same hash-ring idea used for cache sharding) so adding shard 101 only reassigns ~1/101 of hosts, not all of them.

Next up: we've been assuming the URL-dedup check ("have we already discovered this URL?") is cheap — at 1 billion+ URLs, that assumption breaks too, and the fix is a genuinely fun data-structure story (Bloom filters).

Got it / next?

---

## Iteration 4: The "Have We Seen This URL?" Problem at Scale

**The scenario:** every time a Parser worker extracts links from a page, it needs to answer one question per link: *have we already discovered this URL before?* Back in Day 0, that was free — a `UNIQUE` constraint on a MySQL column did it in one INSERT. Let's check whether that assumption survives.

**The arithmetic:** at 1 billion+ pages crawled, and each page discovering roughly 20-30 outbound links on average, that's on the order of **20-30 billion URL-seen? checks** over the crawl's lifetime — most of which will come back "yes, seen it" (the web is densely interlinked; the same popular URLs get discovered from thousands of different pages). Each check, if it's a lookup against a relational table with a B-tree index on a 2KB `VARCHAR` column, costs a disk seek in the worst case once the index no longer fits in memory — and an index over a billion+ long URL strings absolutely does not fit in memory. Even at a generous 1ms per lookup, 386 pages/sec × ~25 links/page ≈ **9,650 dedup checks/sec**, each potentially hitting disk — that's a database begging to fall over, and it's on the hot path of *every single link discovered*, not an occasional query.

**Attempt 1 — the "obvious" fix: cache it in memory.** Someone suggests: keep the full set of seen URLs in a Redis `SET`, since Redis is in-memory and fast. This works... until you do the storage math. A billion URLs, averaging maybe 60-100 bytes each once you account for Redis's per-entry overhead — that's **60-100GB of RAM just for the dedup set**, before you've stored a single byte of actual crawled content. It scales *linearly* with URL count, and the web isn't slowing down — 2 billion URLs means 200GB, and this has to sit in memory in front of every dedup check to be fast. It's not wrong, exactly — it *works* — but it's an expensive, ever-growing tax on a check that's fundamentally binary (seen / not seen).

**Attempt 2 — shard the seen-set across machines like everything else.** Reasonable next move: partition the URL-seen set the same way we partitioned the frontier, `hash(url) % N` machines, each holding its slice in memory. This does bring per-machine memory down linearly with N. But now every dedup check is a **network round-trip** to whichever shard owns that URL's hash — and remember, we need ~9,650+ of these per second, each one now paying network latency instead of a local memory lookup. It also means every single one of those billions of *unique* URLs' full string is being stored somewhere, forever, just to answer a yes/no question. We've traded a memory problem for a network-hop-count problem, and we're still storing more information than the question actually requires.

**The reframe — we don't need the URL, we need the answer to one yes/no question.** This is where a **Bloom filter** earns its keep. The analogy: imagine a bouncer at a club who doesn't keep a guest list with names — instead, for every guest that's already entered, they flip a few specific light switches on a big panel of, say, a billion switches (which switches, determined by running the guest's name through a few different hash functions). To check "has this person been in before?", the bouncer runs the *new* name through those same hash functions and checks: are *all* those specific switches already on? If even one is off, the answer is a certain **no** — this guest has never triggered that combination before. If all of them are on... probably yes, but maybe not — some *other* combination of past guests happened to flip that exact same set of switches by coincidence. That's the trade: a Bloom filter can give you a **false positive** (says "seen" when it wasn't) but **never a false negative** (it will never wrongly say "new" for something actually seen) — and it does this in a fixed amount of memory that doesn't grow with how many *names* you check, only with how many switches (bits) you allocate up front.

Concretely: a bit array of size `m`, `k` independent hash functions. To add a URL: compute `k` hash values mod `m`, set those `k` bits to 1. To check: compute the same `k` hashes, and if *any* bit is 0, it's definitely new; if all are 1, treat it as "probably seen" (with a small, tunable false-positive rate — commonly tuned to ~1%).

For our scale: a Bloom filter sized for 1 billion URLs at a 1% false-positive rate needs roughly **9.6 bits per element** (~1.2GB total) — compare that to 60-100GB for storing full URL strings in a set. That's a two-order-of-magnitude memory reduction, and it fits comfortably on a single machine (or gets sharded for even more headroom), with lookups that are pure in-memory bit checks — no disk, no network hop, if it's colocated with the Parser worker.

**The catch, and why it's an acceptable one here:** a 1% false-positive rate means roughly 1 in 100 genuinely-new URLs gets wrongly marked "already seen" and silently dropped — we never crawl it. For a *search index* covering the general web, missing an occasional obscure page is a tolerable trade-off (the web is redundant; a missed page is often linked from elsewhere too, giving you another shot at discovering it). It would **not** be acceptable for a system where every record matters — e.g., you wouldn't use a Bloom filter to check "has this bank transaction already been processed?" There, false positives cost real correctness, so you'd eat the cost of an exact structure instead.

**What we gained:** dedup memory footprint drops ~50-100x, checks become pure in-memory bit operations, and the structure size is fixed and predictable regardless of URL string length or count growth patterns.

**What we gave up / new problem introduced:** a small, tunable rate of silently-dropped new URLs (false positives) — and Bloom filters don't support deletion cleanly (you can't safely unset a bit, since other URLs may share it), so if a URL ever needs to be "forgotten" (e.g., deliberately re-crawled from scratch), you can't do that by mutating the filter.

**Alternatives considered and rejected:**
- *Full exact set (Redis/hash set), sharded* — rejected as primary structure due to the 50-100x memory cost at this scale, though real systems sometimes layer a small exact LRU cache in front of the Bloom filter for very recently-seen URLs, to avoid even the rare false-positive-driven re-fetch of a hot URL.
- *A relational index (Day 0's approach)* — rejected, established earlier: doesn't fit in memory at this scale, disk-seek-bound under load.

| Structure | Memory @ 1B URLs | Lookup cost | False positives? | Supports delete? |
|---|---|---|---|---|
| Relational unique index | Doesn't fit in RAM | Disk seek | No | Yes |
| Sharded exact set (Redis) | ~60-100GB | Network hop | No | Yes |
| Bloom filter | ~1.2GB | In-memory bit check | ~1% (tunable) | No |

**Follow-up questions:**
- *"What if 1% false-positive-driven missed pages is genuinely unacceptable for this use case?"* — Tune `k` and `m` up (more bits, more hash functions) to push the false-positive rate down — it's a direct memory-vs-accuracy dial, e.g. dropping to 0.1% costs roughly double the bits per element, still far cheaper than an exact set at this scale.
- *"Where does the Bloom filter physically live given we've sharded the frontier across machines?"* — Shard it the same way, `hash(url) % N` matching the frontier shards, so the dedup check and the URL's eventual queue placement are naturally co-located and both benefit from the same partitioning.

Next up: we've been treating "crawl once" as the whole job, but real pages change at wildly different rates — we need a re-crawl / freshness policy, and I'll show you why "just re-crawl everything every 30 days on a fixed schedule" breaks down just as badly as our earlier naive attempts did.

Got it / next?

---

## Iteration 5: Freshness — Not All Pages Age the Same

**The scenario:** two pages, discovered on the same day. Page A is `cnn.com/live/breaking-news` — content changes every few minutes. Page B is `irs.gov/pub/form-1040-instructions-2019.pdf` — a scanned tax form from years ago that will never change again. Our crawler so far treats both identically: crawl once, done, maybe re-crawl "everything" on some fixed cadence like 30 days.

**Attempt 1 — fixed global re-crawl interval.** The simplest possible policy: every URL gets re-crawled every 30 days, full stop. Run the math on what this actually costs: if we're maintaining 1 billion pages and re-crawling all of them every 30 days *in addition* to crawling new pages, that's the same ~386 pages/sec we calculated for initial crawl, sustained **forever**, just to stand still on freshness — doubling our steady-state capacity requirement, permanently. And it still gives CNN's breaking-news page a *worse* freshness guarantee than it needs (staleness up to 30 days is unacceptable for live news) while wasting enormous effort re-fetching the IRS PDF every month when it will never differ from what we already have.

**Attempt 2 — shrink the fixed interval to satisfy the most demanding pages.** Fine, someone says, re-crawl everything every **1 hour** instead, so news sites stay fresh. Now the math gets absurd: 1 billion pages / 1 hour = **277,777 pages/second** sustained, forever — a ~700x increase in required throughput over our original target, almost entirely spent re-fetching millions of static PDFs, terms-of-service pages, and abandoned blogs that haven't changed since 2019. This "fix" solves freshness for the 0.1% of pages that need it by wildly overpaying on the 99.9% that don't.

**The reframe — freshness needs to be a per-URL, adaptive priority, not a global constant.** The real fix: track, per URL, how often it *actually* changes, and let that observed rate set its own re-crawl interval. Concretely:

- Every time a URL is re-crawled, compare a content hash (`SHA-256` of the fetched body, or a normalized version of it) against the hash from last crawl.
- If the content changed: shorten that URL's re-crawl interval (it's "hot").
- If unchanged: lengthen the interval (up to some cap, e.g. 6-12 months for pages that never change).
- Store this as a **priority score** — really, a `next_crawl_time` per URL — and instead of a plain FIFO within each host-queue, the queue becomes a **priority structure** (min-heap or a Redis sorted set keyed by `next_crawl_time`) so the scheduler naturally pulls "whichever URLs are due" rather than "whichever URL arrived first."

This is the same shape of idea as TCP's exponential backoff, just inverted — instead of backing off after failure, we're adjusting an interval based on an observed rate, converging toward each page's actual "metabolism." A simple version: on change, `interval = max(min_interval, interval / 2)`; on no-change, `interval = min(max_interval, interval * 1.5)`. New URLs start with a moderate default (say, 7 days) until enough history accumulates to adapt.

**Where this plugs into what we've already built:** recall each host's queue was a structure the Fetcher pulls "next eligible URL" from, gated by the politeness cooldown. Now within a host's queue, eligibility is a *combination* of two independent gates — politeness cooldown (host-level, "can I hit this host again yet") **and** freshness due-time (URL-level, "is this specific URL due for re-crawl yet") — and a URL is only actually fetched when *both* are satisfied.

```
Host queue (nytimes.com), sorted by next_crawl_time:
┌─────────────────────────────────────────────┐
│ /live/breaking     next_crawl: 09:03:00      │ ← due now
│ /section/politics  next_crawl: 09:15:00      │
│ /archive/2019/xyz  next_crawl: 14 days out   │
└─────────────────────────────────────────────┘
        │
        ▼ (host cooldown also checked: last fetched 09:02:58,
           cooldown 1s → eligible at 09:02:59) ✓ both gates open
        ▼
   Fetcher pulls /live/breaking-news
```

**What we gained:** total re-crawl throughput now scales with the web's *actual aggregate rate of change*, not with a worst-case global constant — news pages get near-real-time freshness, static content gets crawled rarely, and total system load stays close to our original 386 pages/sec baseline instead of 700x inflated.

**What we gave up / new problem introduced:** every URL now needs persisted state beyond "seen or not" — a `next_crawl_time`, a rolling change-rate estimate, a last-content-hash — which is real storage and update cost per URL (though far cheaper than the throughput blowup we were facing). It also introduces a cold-start problem: brand new URLs have no history, so their initial interval is a guess, and a genuinely fast-changing new page (a fresh live-blog) won't get its due until it's proven itself a few cycles in.

**Alternatives considered and rejected:**
- *Let site operators declare change frequency via sitemap.xml `<changefreq>`* — real crawlers do use this as a *hint*, but it's rejected as the sole mechanism because it's self-reported and frequently wrong or absent — observed behavior (did it actually change?) is ground truth; declared metadata is a prior at best.
- *Priority purely by domain authority/pagerank, ignoring observed change rate* — rejected: it would correctly prioritize crawling nytimes.com *at all* over some obscure blog, but wouldn't distinguish nytimes.com's live blog (changes hourly) from its 2015 archive page (dead) — you need the change-rate signal specifically for *re-crawl* cadence, domain authority is a separate signal for initial discovery priority.

| Policy | Steady-state throughput | News freshness | Wastes effort on static pages |
|---|---|---|---|
| Fixed 30-day recrawl | ~386/sec (2x baseline) | Poor (up to 30d stale) | Yes |
| Fixed 1-hour recrawl | ~277,777/sec | 
| Adaptive per-URL interval | Close to baseline | Good (hot pages converge fast) | No |

**Follow-up questions:**
- *"How do you avoid the cold-start problem hurting a genuinely important new page, like breaking news from a brand-new event?"* — Seed initial interval partly from domain-level priors (a page on a known high-authority news domain starts with a shorter default interval than a personal blog) rather than one universal default — domain reputation informs the *prior*, observed change rate refines it afterward.
- *"What if content changes in a way that's cosmetically different but not meaningfully different — like an ad rotating or a timestamp updating?"* — Hash a *normalized* version of the content (strip known-volatile elements like ad iframes, timestamps, view counters) rather than the raw byte stream, or use a similarity threshold (e.g., simhash/minhash) instead of exact-match hashing, so trivial diffs don't falsely trigger "hot page" treatment.

Next up: we've built a correct single-region system — sharded frontier, politeness, dedup, freshness. Now let's talk about what happens when a fetcher machine dies mid-crawl, a parse worker crashes after dequeuing but before finishing, or a host is simply unreachable — the failure-handling story (retries, idempotency, dead-letter queues) before we go multi-region.

Got it / next?

---

## Iteration 6: When Machines Die Mid-Crawl

**The scenario:** a Fetcher worker pops `nytimes.com/live/breaking-news` off its host queue, marks it "in progress," starts the HTTP fetch... and the machine it's running on gets OOM-killed, or the AZ it's in has a network blip, right in the middle of that request. What happens to that URL?

**Attempt 1 — mark "in progress" in the queue, move on.** This is basically what we've implicitly been doing: pop the URL, flip its status, fetch, parse, flip to "done." The failure mode is obvious once you say it out loud: if the worker dies between "flip to in_progress" and "flip to done," that URL is now stuck in `in_progress` **forever** — no other worker will ever pick it up (it's not `pending` anymore), and nothing ever un-sticks it. At our scale, workers die constantly — hardware fails, deploys roll, spot instances get reclaimed — so over time an accumulating fraction of the frontier silently leaks into this stuck state. Months in, some meaningful chunk of "should-be-crawled" URLs have just vanished from active rotation, and nobody notices until someone asks "why don't we have anything from this domain."

**Attempt 2 — add a timeout: if `in_progress` for too long, reset to `pending`.** Reasonable improvement — a background sweeper checks for URLs stuck `in_progress` past some threshold (say, 5 minutes) and resets them to `pending` so another worker retries. This fixes the "leaked forever" problem, but introduces a subtler one: **what if the original worker didn't actually die** — it was just slow (a large page, a laggy host), and it's about to successfully finish and write results, at the exact moment the sweeper resets the URL to `pending` and a *second* worker picks it up and starts fetching the same URL again. Now you might get two fetches of the same page in flight, two sets of parsed links both trying to insert into the frontier, possibly two writes of the crawled content. This is a **duplicate processing** problem, and it's the same shape of issue as "at-least-once delivery" in any message queue — the sweeper's timeout-and-retry is *necessary* for durability but *insufficient* on its own, because it trades "lost work" for "possibly-duplicated work."

**The reframe — accept at-least-once delivery, make the operations idempotent instead of trying to guarantee exactly-once.** Trying to guarantee a URL is fetched *exactly* once, globally, across an unreliable fleet of machines, is fighting the fundamental nature of distributed systems (you cannot get exactly-once delivery without either a perfect coordinator or idempotent operations — this is worth saying explicitly in an interview, it signals you understand *why* the pattern exists rather than just naming it). So instead: let retries happen, and make every downstream write safe to apply twice.

Concretely, per operation:

- **Content storage write:** instead of "append crawled content," write is `PUT content/{url_hash}` (an overwrite, keyed by a deterministic hash of the URL) — applying it twice produces the identical end state, not two records.
- **Frontier insertion of newly discovered links:** already idempotent from Day 0 — `INSERT IGNORE` / a Bloom-filter-gated add either way, re-adding an already-known URL is a no-op.
- **Marking the URL done / updating `next_crawl_time`:** an idempotency key — e.g., a monotonically increasing `crawl_attempt_id` per URL — so if worker A's "delayed" write arrives *after* worker B's retry already completed and updated state, the write with the older attempt ID is detected and discarded rather than blindly overwriting newer state (classic last-writer-wins-by-timestamp, or a version/fencing token check).

For the "what if a specific host is just permanently unreachable" case (DNS failure, server down, 500s every time) — this needs **retry with exponential backoff and jitter**, not an immediate re-queue: first retry after ~1s, then ~2s, ~4s, ~8s, capped at some max (say 1 hour), with random jitter added to each so that if 10,000 URLs from a host all failed at the same moment (the host went down), their retries don't all pile up at the exact same instant and hit it again in a synchronized wave the moment it recovers. After N failed attempts (say, 5), the URL moves to a **dead-letter queue** — not discarded, just parked out of active rotation, so a human or a periodic slow-sweep can revisit it without it clogging normal scheduling.

Two more failure isolation pieces worth naming explicitly:

- **Circuit breaker per host:** if a host is failing on, say, >50% of requests over a rolling window, trip a breaker that stops sending it new requests for a cooldown period entirely — this protects *our* fleet's resources (connection pools, retry queues) from being wasted hammering a host that's clearly down, distinct from the per-URL retry logic.
- **Bulkhead isolation:** a slow or hanging host (TCP connects but never responds) shouldn't be able to exhaust the *shared* fetcher thread/connection pool such that healthy hosts get starved waiting for a free slot — cap concurrent connections *per host* (we already have this from politeness) and ensure a slow host's stuck connections can't block workers assigned to other hosts, e.g. via per-host or per-shard worker pools rather than one global pool where one bad host can hog every slot.

```
   fetch fails
       │
       ▼
   retry count < 5? ──No──► Dead-letter queue (parked, human/slow sweep)
       │ Yes
       ▼
   backoff = min(max_backoff, base * 2^attempt) + jitter
       │
       ▼
   re-queue after backoff ──► (meanwhile: circuit breaker tracks
                                failure rate for this host; if >50%
                                over window, trip — pause all traffic
                                to host for cooldown)
```

**What we gained:** durability (no silently-lost URLs) without the fragility of trying to guarantee exactly-once semantics across an unreliable fleet — the system tolerates worker deaths, slow hosts, and transient failures gracefully rather than needing them to not happen.

**What we gave up / new problem introduced:** every write path now has to be *designed* for idempotency rather than written naturally — that's real engineering discipline overhead, and the dead-letter queue itself becomes a thing that needs monitoring (a growing DLQ is a leading indicator something's systematically wrong, e.g. a change to a common CMS breaking your parser).

**Alternatives considered and rejected:**
- *Distributed transactions / two-phase commit across fetch-write-frontier-update* — rejected: far too much coordination overhead and latency for a system optimizing for raw throughput; the whole point of the crawler's design so far has been avoiding synchronous cross-machine coordination on the hot path.
- *Immediate retry with no backoff* — rejected, established above: synchronized retry storms make transient host outages worse, not better, exactly like the "add more workers" mistake from Iteration 1 but in the time dimension instead of the concurrency dimension.

**Follow-up questions:**
- *"How do you decide the timeout threshold for the sweeper without either leaking (too long) or duplicating too often (too short)?"* — Set it relative to observed p99 fetch+parse latency with margin (e.g., 5-10x p99), and prefer erring toward "occasionally duplicate" over "sometimes leak," since idempotency already absorbs duplicates safely but nothing recovers a silently leaked URL without the sweeper.
- *"Why jitter specifically, isn't backoff alone enough?"* — Backoff alone still leaves everyone who failed at the same instant retrying at the same future instant (they all compute the same `base * 2^attempt`); jitter (adding a random offset) desynchronizes the retry wave so recovery doesn't get immediately re-hammered by a thundering herd the moment it comes back up.

Next up: last major piece — multi-region. Crawling from one region means every fetch pays cross-continent latency to reach, say, an Australian or Japanese host, and it raises a data-sovereignty wrinkle too — then we'll assemble the full architecture recap and the "why not X" cheat sheet.

Got it / next?

---


## Iteration 7: Going Multi-Region

**The scenario:** our entire fleet so far sits in, say, `us-east-1`. Every fetch to a Japanese news site, an Australian government page, or a German blog pays a full round-trip across an ocean before the first byte even arrives — call it 150-250ms of pure network latency *before* the target server does any work, on top of whatever its own response time is. At 386+ pages/sec sustained, and a meaningful fraction of the web living outside North America, that latency tax isn't a rounding error — it inflates the time each connection is held open, which inflates the concurrency needed to hit the same throughput, which inflates cost, all for the same amount of "real" work done.

**Attempt 1 — just add more fetcher machines in us-east-1 to compensate for the latency.** This is the same category of mistake as Iteration 1's "add more workers" — it treats a latency problem as a throughput problem. You can absolutely throw more concurrent connections at the slowness and hit your QPS target on paper, but you're now holding open thousands of extra long-lived connections just to mask geography, and — critically — this does nothing for **data sovereignty** requirements some sites or jurisdictions may have around where crawling/logging infrastructure is allowed to originate from, or for genuinely time-sensitive local content (a regional news outlet's breaking story) where round-trip latency itself, not just throughput, determines how fresh your crawl can be.

**The reframe — deploy fetcher pools regionally, close to the hosts they crawl, coordinated by a home-region frontier.** The Host Manager / frontier state (which hosts, which URLs, cooldowns, freshness schedules) stays as a globally-coordinated logical service — but the actual **Fetcher pools become regional**, and a URL gets routed to the fetcher pool geographically closest to the host it's crawling (determined via IP geolocation of the target, or a simple TLD/registrar heuristic as a fast first pass).

```
                    ┌─────────────────────────┐
                    │  Global Frontier /       │
                    │  Host Manager (sharded,  │
                    │  region-aware routing)   │
                    └───────────┬─────────────┘
              ┌──────────────────┼──────────────────┐
              ▼                  ▼                   ▼
     ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
     │ us-east-1        │ │ eu-west-1        │ │ ap-northeast-1   │
     │ Fetcher pool     │ │ Fetcher pool     │ │ Fetcher pool     │
     │ → US/CA hosts    │ │ → EU hosts       │ │ → JP/APAC hosts  │
     └────────┬─────────┘ └────────┬─────────┘ └────────┬─────────┘
              │                    │                    │
              └──────────► Parsed content → regional
                           object storage, replicated
                           back to global index async
```

Content itself lands in **regional storage first** (cheap, low-latency write near where it was fetched), then replicates asynchronously to wherever the global index/search-serving layer lives — this is the same "write locally, replicate async" pattern we'd use for any multi-region system, and it means a transient cross-region link issue delays global visibility of that page, not the crawl itself.

**What we gained:** fetch latency drops to local-network levels for the majority of hosts (each region mostly crawls its own geographic neighborhood), which both increases achievable throughput per connection and directly improves freshness for time-sensitive regional content; it also gives a real answer to data-sovereignty asks ("crawling and initial storage of EU sites happens within EU infrastructure").

**What we gave up / new problem introduced:** the Host Manager's coordination now spans regions — deciding "which region should own this host's queue and cooldown state" needs to be a stable, low-churn decision (you don't want a host's ownership flapping between regions), and cross-region replication of the global frontier/dedup state reintroduces a CAP trade-off: we explicitly choose **AP over CP** here — availability and partition tolerance over strict consistency — because a region briefly seeing slightly stale freshness/dedup state (crawling a URL that was *just* claimed by another region, milliseconds ago) is a tolerable, self-correcting cost (idempotent writes absorb it), whereas making every host-ownership decision wait for cross-region consensus would reintroduce exactly the synchronous-coordination latency tax we're trying to eliminate.

**Alternatives considered and rejected:**
- *Strongly consistent global lock on host ownership (e.g., cross-region consensus via Paxos/Raft on every claim)* — rejected: correct, but pays a cross-region round-trip on the coordination path for every host claim, defeating the latency win we're trying to achieve; and host ownership doesn't need strict consistency, it needs eventual stability — a soft-lease with periodic renewal (a region "owns" a host for the next 10 minutes, renewable) achieves the same practical goal without the consensus cost.
- *Single global region, just closer to the "average" host (e.g., somewhere central like Europe)* — rejected: there is no single point on Earth that's close to US, EU, and APAC hosts simultaneously; it minimizes worst-case latency for nobody.

**Follow-up questions:**
- *"What happens if two regions briefly both think they own the same host and both fetch it?"* — Exactly the idempotency story from Iteration 6 absorbs it: duplicate fetch, duplicate `PUT content/{url_hash}` overwrite, no corruption — mildly wasteful, self-correcting once the lease conflict resolves, and rare enough (leases renewed well before expiry, short claim windows) not to matter at scale.
- *"Why not just replicate the entire frontier/Bloom filter to every region synchronously so there's never any ambiguity?"* — Sync replication across 3+ regions on every URL discovery would mean every parse-worker's link-insert waits on a multi-region round-trip, which is the exact synchronous cross-region coordination cost this whole iteration exists to avoid — async replication with idempotent conflict resolution gets 99% of the benefit at a fraction of the latency cost.

---

## Full Architecture Recap

```
                              ┌───────────────────────────┐
                              │   Seed URLs (manual/       │
                              │   sitemap ingestion)        │
                              └──────────────┬──────────────┘
                                             ▼
 ┌───────────────────────────────────────────────────────────────────┐
 │  GLOBAL FRONTIER (sharded by hash(registrable_host), consistent    │
 │  hashing for resize) — per host: queue of URLs sorted by           │
 │  next_crawl_time, + cooldown/robots.txt state + circuit breaker    │
 │  state. Region-aware: each host leased to nearest region.          │
 └───────┬──────────────────────────┬──────────────────────┬─────────┘
         ▼                          ▼                       ▼
 ┌───────────────┐         ┌───────────────┐        ┌───────────────┐
 │ us-east-1      │         │ eu-west-1      │        │ ap-northeast-1 │
 │ Fetcher pool   │         │ Fetcher pool   │        │ Fetcher pool   │
 │ (per-host      │         │ (per-host      │        │ (per-host      │
 │ cooldown gate) │         │ cooldown gate) │        │ cooldown gate) │
 └───────┬────────┘         └───────┬────────┘        └───────┬────────┘
         │  raw HTML                │                         │
         ▼                          ▼                         ▼
 ┌───────────────────────────────────────────────────────────────────┐
 │  PARSER WORKERS (per region) — extract links, hash content,        │
 │  update next_crawl_time (adaptive), write PUT content/{url_hash}   │
 │  to regional storage                                               │
 └───────┬───────────────────────────────────────────────────────────┘
         │ new links
         ▼
 ┌───────────────────────────────────────────────────────────────────┐
 │  BLOOM FILTER (sharded w/ frontier) — seen? check before enqueue   │
 └───────┬───────────────────────────────────────────────────────────┘
         │ new only
         ▼
   back into Global Frontier (loop)

 Failure handling overlaid throughout: retry+backoff+jitter on fetch
 failure → dead-letter queue after N attempts; circuit breaker per
 host; bulkhead (per-host connection caps) protects shared pools;
 idempotent writes absorb at-least-once duplication from sweeper
 timeouts and cross-region lease races.

 Regional content storage → async replication → global index/search
 layer (downstream consumer, out of scope).
```

## The "Why Not X" Arsenal

1. **"Why not just add more worker threads to scale throughput?"** — Without host-aware partitioning, more workers means more concurrent hits on the same hot host, worsening politeness, not fixing throughput.
2. **"Why not a global rate limiter instead of per-host queues?"** — A fleet-wide budget can't express "≤1 req/sec to *this* host specifically"; politeness is inherently host-scoped.
3. **"Why not exact-match dedup (a hash set) instead of a Bloom filter?"** — Costs 50-100x the memory at billion-URL scale for a fact (exactness) the use case doesn't need; occasional missed pages are tolerable for general web coverage.
4. **"Why not a fixed global re-crawl interval?"** — Either wastes throughput re-fetching static pages (long interval) or requires ~700x capacity to keep news fresh (short interval); per-URL adaptive intervals track actual change rate instead.
5. **"Why not guarantee exactly-once fetch semantics?"** — Requires either a perfect global coordinator or heavy distributed transactions, both of which reintroduce the synchronous cross-machine coordination the design otherwise avoids; idempotent writes + at-least-once delivery achieve durability more cheaply.
6. **"Why not retry immediately without backoff/jitter?"** — Synchronized retry storms re-hammer a recovering host the instant it comes back, recreating the outage.
7. **"Why not a single global region for simplicity?"** — No single region is close to all geographies; cross-continent round-trips inflate latency, connection hold time, and hurt freshness for regional/time-sensitive content, plus ignores data-sovereignty constraints.
8. **"Why not strongly-consistent cross-region coordination for host ownership?"** — Pays a cross-region round-trip on every host claim; soft leases with async, idempotent-conflict-resolved replication get nearly the same correctness at a fraction of the latency cost (explicit AP-over-CP choice).

---

Want to go deeper on any single piece (e.g., the Public-Suffix-List-style multi-tenant detection, or the min-heap scheduler implementation), or should I switch into interviewer mode and pressure-test you on the whole design end-to-end?

---

Good question to stop and dig into — this is exactly the kind of thing an interviewer will probe ("why did you introduce a queue here?"), and "because that's what production crawlers do" is not an answer that survives follow-up.

Let's rewind carefully.

## Day 0 actually already had a queue — just a disguised one

Look again at Day 0's table:

```sql
status ENUM('pending','in_progress','done')
```

`SELECT * FROM urls WHERE status='pending' LIMIT 1` — that *is* a queue. It's "give me the next unit of work." A queue, at its core, isn't a specific technology (Kafka, RabbitMQ, whatever) — it's just: **a place to put work items, where a consumer can ask "what's next?" without needing to know who produced the item or when.**

So the real question isn't "why did we add a queue that wasn't there before." It's: **why did one shared queue, sitting between one producer and one consumer, stop being enough?**

## The actual trigger: doing two different jobs, at two different speeds, in one loop

Look at Day 0's loop again:

1. Pop URL
2. Fetch (network — I/O bound, waiting on someone else's server)
3. Parse (CPU — waiting on nothing but your own processor)
4. Insert new links
5. Mark done
6. Repeat

These five steps run **strictly one after another, on one thread.** While step 2 is waiting on a slow server, your CPU is doing *nothing* — it's just sitting there. While step 3 is chewing through parsing a big page, your network connection is *idle* — you're not fetching anything else.

That's the concrete pain. You have one resource (network) that's slow and mostly-waiting, and a different resource (CPU) that's fast and bursty, and you're forcing them to take turns instead of letting both run flat out simultaneously.

## Why "just make it concurrent" isn't enough on its own — you need a queue *specifically*

Here's the direction-finding move: once you decide "fetching and parsing should happen concurrently, not in lockstep," ask yourself — **how does a fetch result get from the fetcher to the parser, if they're now two independent things running at their own pace?**

You have exactly two options, and it's worth naming both to see why one wins:

**Option A — direct handoff (fetcher calls parser directly, like a function call).** Fetcher finishes a fetch, immediately calls `parse(html)` itself. But now the fetcher can't start its *next* fetch until parsing is done — you've just glued the two stages back together with extra steps. No actual overlap gained.

**Option B — a queue between them.** Fetcher finishes a fetch, drops the HTML into a shared buffer, and *immediately* moves on to the next fetch — it doesn't wait for anyone to consume what it just produced. Separately, whenever a parser is free, it pulls the next item off that buffer and works on it, at its own pace.

Option B is the only one that actually decouples the two stages' *speed* from each other. That decoupling — "producer can keep producing without waiting for the consumer to catch up, consumer can keep consuming without waiting for the producer's next item" — is the entire reason a queue is the right structure here. It's a shock absorber between two things moving at different, independent rates.

## The second reason: many workers, one shared pool of work, no collisions

There's a second, separate pressure pushing toward an explicit queue: once you have **200 fetcher workers** instead of 1, they all need to pull from the *same* pool of pending URLs without two workers grabbing the same URL at once, or corrupting each other's view of "what's left to do."

A plain in-memory list doesn't give you that safety for free — two threads reading/removing from a Python list concurrently will race and corrupt state. A queue (or a DB row locked via `SELECT ... FOR UPDATE`, or a real message queue like Kafka/SQS) gives you **atomic "pop one item, and guarantee no one else also got it"** as a built-in property. That's not optional once you have concurrent consumers — it's the thing that makes concurrency safe at all.

## So, stated as a chain of reasoning (the thing to say out loud in an interview)

> "Day 0 does fetch and parse serially on one thread, so the CPU sits idle during network waits, and vice versa. Fixing that means running fetch and parse concurrently, as independent stages. Two independent stages, moving at different speeds, need a buffer between them so the fast one doesn't block on the slow one — that's a queue. Separately, once I have many fetcher workers instead of one, they all need to safely share one pool of pending work without colliding — a queue gives me that atomic hand-out for free."

That's the direction: **queue = decoupler for stages at different speeds, and safe shared work-distribution for many concurrent consumers.** Anywhere you see "these two things should run independently" or "multiple workers need to share one pool of work," that's your signal to reach for a queue — not because it's a standard component, but because those are the two specific problems a queue actually solves.

Want me to point out where this same "why a queue" reasoning shows up again later in the design (hint: it does, at least twice more)?

---

Let's rebuild this one the same way — starting from what was actually broken, not from the solution.

## Start with the exact problem we left off with

At the end of Iteration 1, we had 200 fetcher workers all pulling from **one shared queue** of URLs, in whatever order they'd been discovered.

The problem: URLs from the same host tend to arrive in the queue clustered together, because crawling one page discovers dozens of links to that same site. So a batch of "next 200 URLs" might contain 60+ URLs that are all `nytimes.com`.

Result: 60 workers hit `nytimes.com` in the same second. That's not "fast crawling" — that's an accidental denial-of-service attack.

**So the actual question we need to answer is narrow and specific:** how do we let 200 workers run at full speed, *without* letting more than one of them hit the same host at the same time?

## Why small tweaks to the existing queue don't fix it

Before jumping to "give each host its own queue," it's worth checking why the obvious cheap fixes fail — this is the part that makes the real fix feel earned instead of arbitrary.

**Cheap fix 1 — shuffle the queue randomly.** Reduces clustering, doesn't eliminate it. With enough workers pulling fast enough, you'll still get unlucky batches sometimes. No guarantee, just lower odds.

**Cheap fix 2 — global rate limit ("crawler can only do 500 req/sec total").** This caps *total* speed, but says nothing about *distribution*. You could easily have 500 req/sec entirely aimed at one host and still be "within budget."

Both fail for the same underlying reason: **the queue has no concept of "host" at all.** It just sees URLs as interchangeable strings. Politeness is a *per-host* constraint, so any fix that doesn't track state per-host can't actually enforce it. That's the insight that forces the next design.

## The fix, built from that insight: one queue per host

If the rule is "never more than one request in flight to a given host," then the cleanest way to enforce that mechanically is to make each host's URLs physically live in their own separate line.

```
nytimes.com  → [url1, url2, url3, ...]
wikipedia.org → [url4, url5, ...]
smallblog.io  → [url6]
```

Instead of workers asking "what's the next URL in the global queue," they ask a coordinator — the **Host Manager** — "which *host* is currently allowed to be fetched from, and what's its next URL?"

## The hotel mailroom picture, plainly

A hotel doesn't dump every guest's mail into one bin sorted by delivery time — that would mean the clerk revisits Room 204 four times in an hour because that guest gets a lot of mail, while Room 301 waits all day.

Instead: **one pigeonhole slot per room.** The clerk delivers one item from a slot, moves to the next slot, and only returns to Room 204 after visiting everyone else.

Map that back: "room" = host, "mail item" = URL, "clerk" = fetcher worker. Giving each host its own slot is what guarantees fairness — not because the clerk is being careful, but because the *structure itself* makes it impossible to over-visit one room without first passing through all the others.

## How the cooldown actually stops the pile-up

Structure alone (separate queues) isn't quite enough — you also need a timer, or nothing stops one *fast* worker from draining `nytimes.com`'s whole queue back-to-back by itself.

So each host also carries a `next_allowed_fetch_time`. The moment any worker pulls a URL from `nytimes.com`, that host is marked "not eligible again until +1 second" (or whatever its `robots.txt` crawl-delay says). Any other worker asking "give me eligible hosts" simply won't be offered `nytimes.com` until that timer clears — regardless of how many workers are free and ready.

That's the actual mechanism that turns "one host, one request at a time" from a hope into a guarantee: it's enforced by *state the coordinator checks before handing out work*, not by workers behaving themselves.

## Why this doesn't kill throughput

The key realization: politeness only restricts requests *to the same host*. It says nothing about running 200 different hosts concurrently.

So while `nytimes.com` is in cooldown, the other 199 workers are freely fetching from 199 *other* hosts — `wikipedia.org`, `smallblog.io`, thousands of others — completely unaffected by nytimes's cooldown. You get full concurrency across the *breadth* of the web, while each individual host sees traffic no faster than it asked for.

That's the shift in one sentence: **we moved the unit of "wait your turn" from the whole crawler down to each individual host**, so waiting on one host never blocks progress on any other host.

---

Does that land clearer? Happy to also walk through *why* the queue-backlog problem (blogspot with millions of URLs) shows up next, if that connection isn't obvious yet either.

---

Good question to nail down — "we use a per-host queue" is exactly the kind of thing that sounds fine until an interviewer asks "show me the actual data structure and the actual command," and hand-waving there loses points.

## First: why Kafka is the wrong tool for "one queue per host"

The instinct to reach for Kafka makes sense — it's the default answer for "queue" in a lot of interviews. But check it against what we actually need.

Kafka's queues are **topics**, split into **partitions**. Partitions aren't free — a single Kafka cluster realistically handles on the order of a few thousand to low tens of thousands of partitions total, because each partition costs real overhead (open file handles, replication threads, memory).

We need **one independent queue per host**, and we're talking about tens of millions of distinct hosts. That's off by three or four orders of magnitude from what Kafka partitions are built for. If you tried "one Kafka partition per host," the cluster would fall over from partition-count overhead long before you got anywhere near real web scale.

Kafka is a great fit *elsewhere* in this system (e.g., streaming discovered-links events between Parser and Frontier) — just not for "millions of tiny independent per-host lines," because that's not the shape of problem it's built to hold.

## What we actually need, restated as a data-structure problem

Two separate pieces of state, per shard:

1. **For each host, a list of its pending URLs** — just needs push/pop.
2. **Across all hosts, "which ones are past their cooldown right now"** — needs to be queried efficiently, not scanned linearly.

That second one is the part people usually skip over. If you have 500,000 hosts on one shard and need to find "which of these are eligible right now," you cannot linearly scan 500,000 cooldown timestamps on every single fetch. You need a structure that keeps them **sorted by eligibility time**, so "give me the next eligible one" is fast.

## The real implementation: Redis, two structures

This is the practical choice real systems use (Redis or something Redis-like — an in-memory store with sorted-set support).

**Structure 1 — per-host URL queue.** A Redis `LIST` per host:

```
RPUSH host:queue:nytimes.com "https://nytimes.com/live/breaking"
RPUSH host:queue:nytimes.com "https://nytimes.com/section/politics"
LPOP  host:queue:nytimes.com    # pop the next URL for that host
```

You don't pre-create millions of empty lists — a list simply exists the moment you `RPUSH` into it, and disappears when it's empty. Redis handles millions of small keys like this fine; it's one of its core use cases.

**Structure 2 — the "who's eligible" index.** A Redis `ZSET` (sorted set), where the *score* is the timestamp each host becomes eligible again:

```
ZADD host_ready 1755000000 nytimes.com
ZADD host_ready 1755000001 wikipedia.org
ZADD host_ready 1755000010 smallblog.io
```

To find hosts eligible **right now**, a worker asks for everything scored at or below the current time:

```
ZRANGEBYSCORE host_ready -inf <now> LIMIT 0 1
```

That single command is doing the job of "give me any host whose cooldown has expired" in `O(log n)` — it's a skip-list under the hood, so it stays fast even with millions of hosts tracked. This is the concrete answer to "how do we avoid scanning."

## Making the pop-and-recool atomic (this is the part that's easy to get wrong)

Here's the subtlety: a worker needs to do three things as one unit — pick an eligible host, pop a URL from its list, and reset its cooldown — **without another worker sneaking in between steps 1 and 3** and grabbing the same host.

If you did this as three separate Redis calls, two workers could both read "nytimes.com is eligible" before either one has re-cooled it, and both would fetch from it simultaneously — exactly the bug we're trying to prevent.

The fix: bundle all three steps into one **Lua script**, which Redis executes atomically (Redis runs one Lua script to completion before touching anything else):

```lua
-- EVAL this script, called with KEYS[1]=host_ready, ARGV[1]=now, ARGV[2]=cooldown_seconds
local host = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1], 'LIMIT', 0, 1)[1]
if not host then return nil end

local url = redis.call('LPOP', 'host:queue:' .. host)
if not url then
  redis.call('ZREM', KEYS[1], host)   -- empty queue, drop it from rotation
  return nil
end

redis.call('ZADD', KEYS[1], ARGV[1] + ARGV[2], host)  -- push cooldown forward
return {host, url}
```

One round-trip, one atomic unit, no race window. This is the actual mechanism that makes "no two workers hit the same host" a hard guarantee instead of a hope.

## Sharding this across machines

One Redis instance holding *all* hosts globally would itself become the single point of failure and bottleneck we flagged back in Iteration 2. So you partition:

```
shard_id = hash(registrable_host) % N
```

Each shard is its own Redis instance (or a slot range in Redis Cluster), owning a disjoint slice of hosts — its own `host_ready` ZSET and its own set of `host:queue:{host}` lists. Fetcher workers are assigned to shards (or ask a routing layer which shard owns a given host), so the Lua-script pop above always runs against one shard's local Redis, never across shards.

## Durability — the piece Redis alone doesn't give you

Redis is fast because it's in-memory, but that means a crashed Redis node without persistence loses the whole frontier for its shard. Two practical answers, often combined:

- **Redis AOF (append-only file) persistence** — every write is logged to disk, replayed on restart. Costs some write latency, buys durability.
- **Kafka as the durable source of truth for *newly discovered* URLs**, with Redis as the fast working queue. Parser workers publish discovered links to a Kafka topic; a consumer drains Kafka and does the `RPUSH`/`ZADD` into Redis. If Redis's shard dies, you can rebuild its queues by replaying Kafka from the last committed offset.

That's actually a clean division of labor worth stating explicitly: **Kafka for durable, ordered ingestion of new work; Redis for the fast, mutable "what's eligible right now" scheduling state.** Each tool doing the part it's actually good at, instead of forcing one tool to do both jobs.

---

**Follow-up questions an interviewer might chase this with:**

- *"What if one shard's Redis instance is under memory pressure from a host with millions of queued URLs?"* — This is the blogspot hot-shard problem from Iteration 3, resurfacing at the implementation layer: cap per-host list length with backpressure (pause discovery from that host once its queue passes a threshold), or apply the subdomain-aware resharding key we discussed.
- *"Why Lua script instead of a Redis transaction (`MULTI`/`EXEC`)?"* — `MULTI`/`EXEC` queues commands but can't make a *decision* mid-transaction (like "only pop if eligible") — it just replays a fixed command list. Lua scripts can branch on intermediate results, which is exactly what "check eligibility, conditionally pop, conditionally re-cool" needs.

Want to go one level deeper on how the durable Kafka-to-Redis rebuild actually works on shard restart, or move on?

---

Good next thread to pull — this is where the "per-host queue" from before needs an upgrade, because a plain `LIST` (FIFO) can't express "serve this URL before that one."

## The gap in what we built so far

The per-host queue was a Redis `LIST`:

```
RPUSH host:queue:nytimes.com url1
RPUSH host:queue:nytimes.com url2
LPOP  host:queue:nytimes.com   # always returns whichever was pushed first
```

A `LIST` only knows *insertion order*. But priority — from Iteration 5 — is about **due time** (`next_crawl_time`), which has nothing to do with when a URL happened to be discovered. A URL discovered yesterday might be due to re-crawl in 6 months (a static PDF); a URL discovered five minutes ago might be due *right now* (a breaking-news page that just changed). FIFO order can't represent that at all.

## The fix: swap the per-host LIST for a per-host ZSET

Same move we already made once — a Redis `ZSET`, scored by `next_crawl_time`, but this time it's **per host** instead of one global one:

```
ZADD host:urls:nytimes.com <next_crawl_time_1> "https://nytimes.com/live/breaking"
ZADD host:urls:nytimes.com <next_crawl_time_2> "https://nytimes.com/archive/2019/xyz"
```

To get the most urgent URL for that host:

```
ZRANGE host:urls:nytimes.com 0 0 WITHSCORES   # lowest score = earliest due = most urgent
```

That's the whole trick — reuse the exact same sorted-set structure we already used for host-level cooldowns, just keyed one level lower, per URL instead of per host.

## Now there are two "is this ready" questions, and they need to merge into one

This is the part worth being precise about, because it's easy to hand-wave.

We actually have **two separate gates**:

1. **Host-level politeness gate** — has the cooldown since the last fetch to this host expired?
2. **URL-level freshness gate** — is this *specific* URL's `next_crawl_time` actually due yet?

A host can be off cooldown but have *nothing* due — e.g. `nytimes.com`'s cooldown cleared a second ago, but its most urgent queued URL isn't due for another 3 hours. Fetching from it right now would be wasted effort (and would mean re-crawling something before it's actually due, since we'd just grab whatever's at the front).

So the host's "ready time," stored in the global `host_ready` ZSET, should really be:

```
host_ready_score = max(cooldown_expiry_time, earliest_url_due_time_for_this_host)
```

Whichever of the two constraints is *more restrictive* wins. This single number now correctly encodes both gates at once — no separate check needed at read time.

## Updating the atomic Lua script

Same shape as before, just one more step:

```lua
-- KEYS[1] = host_ready, ARGV[1] = now, ARGV[2] = default_cooldown
local host = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1], 'LIMIT', 0, 1)[1]
if not host then return nil end

local key = 'host:urls:' .. host
local result = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
if #result == 0 then
  redis.call('ZREM', KEYS[1], host)  -- nothing queued, drop from rotation
  return nil
end

local url = result[1]
redis.call('ZREM', key, url)  -- claim it

-- recompute this host's next ready time from its remaining URLs
local nextItem = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
local nextDue = (#nextItem > 0) and tonumber(nextItem[2]) or nil
local cooldownExpiry = ARGV[1] + ARGV[2]

if nextDue then
  redis.call('ZADD', KEYS[1], math.max(cooldownExpiry, nextDue), host)
else
  redis.call('ZREM', KEYS[1], host)  -- re-added later when a new URL lands
end

return {host, url}
```

Same atomicity property as before — one script, one round-trip, no race between two workers grabbing the same host's most-urgent URL.

## Where "priority" (not just freshness) actually enters

Two distinct things get called "priority" in a crawler, worth separating:

**Re-crawl priority** — already covered: it's just the `next_crawl_time` score, driven by the adaptive change-rate logic from Iteration 5. A page that changes often gets a lower (sooner) score.

**Initial discovery priority** — a *brand-new* URL, never crawled before, needs a starting score before we have any change-rate history for it. This is where domain authority comes in: instead of every new URL defaulting to the same `now + 7 days`, a URL discovered on a known high-authority domain (say, a `.gov` site, or a domain with a high inbound-link count) gets seeded with a smaller offset — `now + 1 hour` — so it surfaces sooner the first time, even before we've observed how often it actually changes.

Concretely, at insert time:

```
initial_score = now + base_delay / priority_weight(domain)
ZADD host:urls:{host} initial_score url
```

where `priority_weight` is just a lookup — a known-authoritative domain gets a bigger divisor (smaller resulting delay), an unknown domain gets the plain default.

## Why we didn't need a *third*, separate priority queue

You might expect "priority" to need its own dedicated structure sitting above everything else. It doesn't, and it's worth saying why: **priority here is just an input into the score used by structures we already have.** There's no separate ranking pass — the ZSET ordering *is* the priority ordering, for both which host gets served next (`host_ready`) and which URL gets served next within that host (`host:urls:{host}`). Adding a third structure would just be duplicating information the two ZSETs already encode.

---

**Likely follow-up:** *"What if a very high-priority URL is discovered on a host that's mid-cooldown — does it have to wait?"* — Yes, and that's intentional: politeness is a hard constraint, priority is a soft one. A breaking-news URL on a host that was just fetched 0.5 seconds ago still waits out the remaining cooldown; priority only affects ordering *among* URLs that are otherwise eligible, it never overrides the politeness gate.

Want to go into how this handles the "many hosts tied at the same ready time" case at real scale, or move to something else?

---

