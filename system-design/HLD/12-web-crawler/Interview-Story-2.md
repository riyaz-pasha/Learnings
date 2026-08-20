# Design a Web Crawler & Search Index — A Complete Narrative Walkthrough

*A story-driven HLD deep dive: naive beginnings, real failures, and the iterative fixes that turn a single-server crawler into a system that indexes billions of pages without getting itself banned by the entire internet.*

*Every chapter below is written so that no step is implied — if a component does something, this doc says which component triggers it, what data it reads, what data it writes, and where that data lives.*

---

## Chapter 0: Scoping the Problem Before Writing Any Architecture

"Design a web crawler" is a deceptively dangerous prompt because it sounds like one system when it's actually two very different systems glued together: a **crawler** (go fetch pages) and a **search engine** (let people query what you fetched).

Trying to design both to full depth in one session is how candidates run out of time. So — same move as always — scope hard, and say why, before drawing a single box.

### Answering the scoping questions up front, because they change the architecture

**What's the crawler's purpose — full web index, or a narrower vertical (news, e-commerce)?**

We'll design for a **general-purpose crawler** feeding a general text search index. This is the hardest, most general version of the problem, and it naturally covers narrower verticals as a special case. A news-only crawler is this system with a much more aggressive recrawl policy (Chapter 10) and a narrower seed set (Chapter 1) — nothing about the core architecture changes.

**Should we keep refreshing pages for the latest content?**

Yes — this is a first-class requirement, not an afterthought. A crawler that indexes the web once and never revisits it produces an index that's permanently stale within days. We'll design **recrawl scheduling** explicitly (Chapter 10), not bolt it on at the end.

**Should we support JavaScript rendering?**

Yes, but *selectively*. This is one of the more expensive and interesting trade-offs in the whole system (Chapter 6). Treating "render everything with a headless browser" as the default would blow the compute budget by an order of magnitude, for a feature most pages don't need.

**Should we handle duplicate content?**

Yes, at two distinct levels that are easy to conflate:

- Duplicate *URLs* — same page, different query strings. Solved in the URL frontier (Chapter 4).
- Duplicate *content* — different URLs, near-identical text. Solved with content fingerprinting (Chapter 5).

These need different tools because they're detected at different points in the pipeline: a duplicate URL is caught **before** we ever fetch anything, from the string alone. Duplicate content can only be caught **after** we've fetched and parsed the page, because you need the actual text to compare it against anything.

**Are we storing images/video, or just text and links?**

For this design: **text and links, plus URLs pointing at media, not the media bytes themselves.** Storing and indexing images/video is a genuinely separate system (reverse image search, video transcoding) that would double our scope for comparatively little architectural insight into crawling itself. We store a reference (the `src` attribute's value, captured verbatim at parse time) and move on.

### Functional Requirements

- **P0 — Crawling.** Fetch pages starting from seed URLs, discover new URLs by following links, and — critically — **respect politeness**: don't hammer any single host, obey `robots.txt`.
- **P0 — Indexing.** Parse fetched pages, extract meaningful content, build a searchable index from it.
- **P0 — Search.** Given a query, return relevant, ranked pages quickly.

### Non-Functional Requirements

- **Scalable** — the crawl needs to eventually reach and index 1B+ URLs, growing over time; today's 100M is a slice of that, not the ceiling.
- **Available** — search serving is user-facing and needs to stay up even while crawling and indexing are happening in the background. These are different availability domains and shouldn't take each other down — a crawler outage should never turn into a search outage, and vice versa.
- **Polite** — unusual as a non-functional requirement for most systems, but for a crawler it's existential. An impolite crawler gets IP-banned by the sites it's trying to index, which is a self-inflicted, permanent outage with no retry that fixes it.

### Capacity Numbers, Derived Cleanly

*Starting assumption, corrected:* 100M URLs to crawl in an initial 10-day pass. (10 days is itself a choice, not a given — it's a round number picked to keep the arithmetic simple; a real crawl would pick this based on how fast the index needs to reach usable coverage.)

```
10 days = 10 × 86,400 sec = 864,000 sec ≈ 8.64 × 10^5 sec

Crawl rate = 100,000,000 URLs / 864,000 sec ≈ 116 URLs/sec average
```

Round to **~120 URLs/sec average**. As with every crawl-style workload, this isn't remotely uniform — some hosts respond in tens of milliseconds, others take seconds, and politeness delays (Chapter 3) mean the *actual* number of fetcher workers needed is much higher than "120 requests/sec" implies, because most of the time each worker spends per host is spent *waiting*, not fetching. Chapter 3 works this out concretely.

*Storage, per your own math — and it checks out:*
```
100M pages × 10MB/page (raw HTML, generous upper bound) = 10^8 × 10^7 bytes = 10^15 bytes = 1 PB
```
That's raw HTML for one crawl pass at the 100M scale. At the NFR's actual target of 1B+ URLs, that's **~10PB** of raw page storage — a number that immediately rules out storing raw HTML in a normal database and points straight at blob storage (Chapter 7).

*Index size, back-of-envelope:* a rough rule of thumb for an inverted index (Chapter 8) is that it runs 10–30% of the raw text size, once you strip HTML markup and boilerplate down to actual content. If raw HTML is ~10MB/page but the actual extractable text is closer to ~50KB/page (HTML markup, scripts, and styling dominate raw page weight), then:
```
100M pages × 50KB extracted text ≈ 5TB of raw text
Inverted index ≈ 15-25% of that ≈ 750GB-1.25TB at 100M-page scale
```
At 1B+ pages, that's **~10-15TB** for the index — large, but a completely different, much more tractable problem than the raw-HTML storage number. This gap between "raw crawl storage" and "index storage" is worth internalizing early, because it's *why* the crawl pipeline and the search-serving pipeline end up as separately-scaled systems (Chapter 8 onward) rather than one monolith.

With scope, requirements, and numbers pinned down, let's start exactly where your draft did: **the dumbest thing that could possibly work.**

---

## Chapter 1: v0 — The Single-Server Baseline

**Architecture:**

```
[Seed URL list] --> [Single Worker Loop] --> [Single Database]
```

One process. One loop. One database. This is, again, not a strawman — it's a genuinely correct way to crawl a few thousand pages, and walking through it carefully is what lets us name precisely which part breaks first.

### The loop, spelled out completely

Nothing here is implicit — here's exactly what the one process does, in order, forever:

1. **Pull the next URL.** The process holds "the list" as a table in its single database (`urls`, below), with a `status` column. It runs `SELECT ... WHERE status = 'pending' LIMIT 1`, and immediately updates that row's status to `in_progress` in the same operation (or the same transaction) so that if this were ever more than one process, two workers couldn't grab the same URL. In v0 there's only one process, so this matters less — but naming it now is what makes Chapter 2's queue an obvious next step rather than a surprise.
2. **Resolve the host to an IP address via DNS.** A plain OS-level DNS lookup, `example.com` → `93.184.216.34`. No caching yet — every fetch does a fresh lookup, which is one of several things that make this slow (revisited in Chapter 3).
3. **Open a connection and download the page.** A synchronous HTTP GET. The process blocks here until the remote server responds or the connection times out. There is no timeout configured yet in v0 — a hanging server hangs the whole loop. (Chapter 11 fixes this explicitly.)
4. **Parse the downloaded HTML.** A library like BeautifulSoup turns raw markup into a navigable Document Object Model (DOM), from which the process pulls the `<title>`, the body text, and every `<a href>` link on the page.
5. **Store and requeue.** The process writes the extracted title and a reference to the stored body into `content`, flips the original URL's row to `status = 'done'`, and — for every link found in step 4 — inserts a new row into `urls` with `status = 'pending'`, *if that exact URL string isn't already in the table* (a simple `SELECT` check before the `INSERT`, our only dedup mechanism at this stage).

Then it goes back to step 1 and repeats, forever, one URL at a time.

**A minimal schema:**

```sql
urls(id, host, url, status)   -- status: pending / in_progress / done / failed
content(id, url_id, title, body_blob_link)   -- body stored as a blob reference, not inline — pages can be large
```

`host` is stored as its own column (not re-parsed from `url` every time) specifically because Chapter 3 needs to group and look up by host constantly — pulling it out now costs nothing and saves rework later.

**Why this is a legitimate starting point, not a toy:** it's simple enough to reason about completely, it does everything the P0 requirements ask for at small scale, and — this is the part worth internalizing — every later iteration is a *deliberate* trade-off away from this simplicity, and naming exactly what's being traded away each time is what actually impresses an interviewer.

### Where It Breaks — Two Separate Problems, Not One

**Problem 1: One process is doing three unrelated jobs.** Fetching (steps 2-3) is I/O-bound and mostly *waiting* on slow, unpredictable remote servers. Parsing (step 4) is CPU-bound and fast. Storing (step 5) is a database write. Bundling all three into one loop means a slow or hanging fetch (a server that never responds, since step 3 has no timeout) blocks parsing and storage that have nothing to do with it — the whole pipeline stalls waiting on the one thing that's slow. This is a **separation-of-concerns** problem: three different failure modes and three different resource profiles, artificially coupled into one process, where one thing failing becomes a reason for everything to fail.

**Problem 2: It's single-threaded and single-machine**, so it caps out at whatever one process, doing DNS lookups and TCP handshakes serially, one URL at a time, can manage — nowhere near the ~120 URLs/sec (let alone the burst capacity) the system actually needs. If a typical fetch-plus-parse cycle takes even 200ms, this loop tops out around 5 URLs/sec — over 20x short of the target.

The fix for Problem 1 comes first, because it's the one that changes the shape of the architecture, not just its size.

---

## Chapter 2: Splitting Fetcher and Parser, Decoupled by a Queue

### The Instinct, and Why the Obvious Version Is Wrong

The natural fix for "one process doing too much" is: split it into a **Fetcher** service and a **Parser** service. But then an immediate question appears — how does Fetcher tell Parser "here's a page, go parse it"?

The naive answer is a **synchronous call**: Fetcher finishes downloading, calls Parser's API directly, waits for the response. This is a trap. If Parser is momentarily backed up — maybe it's mid-way through a burst of CPU-heavy DOM parsing — Fetcher now sits there waiting on Parser, which means Fetcher isn't fetching anything else during that time. We've recreated exactly the coupling problem we were trying to solve, just moved it from "one process" to "two processes blocking each other over a network call." Fetcher's whole reason to exist — keep hammering the network with new requests — is defeated the moment it has to wait on a downstream service that has nothing to do with networking.

### The Fix: A Queue in Between

```
[Fetcher Queue] → [Fetcher Worker Pool] → raw HTML to blob storage
                                          → message to [Parser Queue] with the blob's location
                                                              ↓
                                          [Parser Worker Pool] pulls, fetches raw HTML from blob storage,
                                          parses, extracts, writes structured content + new URLs
```

**Exactly what's in each message, so nothing is left implicit:**

- A message on the **fetcher queue** is small: `{ url, host, url_id }`. It does not carry any content — there isn't any yet.
- After a fetcher worker downloads a page, it writes the raw bytes to blob storage under a deterministic key — something like `raw/{url_id}/{fetch_timestamp}.html` — so that repeated fetches of the same URL don't collide and old versions aren't silently lost (useful later for Chapter 5's fingerprint comparison, which needs "the previous version").
- The worker then publishes a message on the **parser queue**: `{ url_id, blob_key, http_status, fetched_at }`. Still no content — just a pointer to where the content lives and the metadata needed to decide what to do with it (e.g. a parser worker sees `http_status: 404` and knows to mark the URL `failed` without ever touching blob storage).

Fetcher workers pull a URL off the fetcher queue, download it, drop the raw bytes into blob storage — not the database, since pages are large and mostly opaque blobs at this stage, exactly the kind of content that belongs in object storage rather than a relational row — and publish that small pointer message onto the parser queue. Fetcher's job ends there; it never waits on Parser and immediately grabs the next URL from the fetcher queue.

Parser workers, running as a completely independent pool that can scale up or down on its own schedule, consume from the parser queue whenever they're ready, fetch the raw HTML from blob storage using the `blob_key`, run it through DOM parsing, extract title/body/links, write the structured result to the document store (Chapter 7), and push newly discovered URLs back into the system — specifically, back into the frontier described in Chapters 3-4, not directly onto the fetcher queue (that distinction is the entire subject of the next chapter).

**What this buys us, stated explicitly:** Fetcher and Parser now scale independently — if parsing turns out to be the CPU-bound bottleneck, you add Parser workers without touching Fetcher at all, and vice versa. A crash or slowdown in one pool doesn't cascade into the other, because the queue absorbs the mismatch in speed between them, the same buffering role a message queue plays in a newsfeed design's fan-out stage — a burst on one side gets smoothed into a steady stream on the other, rather than slamming directly into the downstream service. And critically, both pools are now independently horizontally scalable — which is exactly the next problem, and where naive parallelism breaks politeness.

---

## Chapter 3: The URL Frontier — Who Writes To It, Who Reads From It, and How Politeness Actually Gets Enforced

### Naming the Failure Precisely

Once Fetcher is just "a pool of workers pulling from a shared queue," scaling it is trivial in principle — spin up more workers. But here's the failure that a naive multi-worker Fetcher pool creates: **nothing stops five different fetcher workers from independently pulling five different URLs that all happen to point at the same host, and hitting that host with five simultaneous requests.**

A single site — especially a smaller one not built for this kind of load — can interpret that as an attack. Best case, it starts rate-limiting your IP range. Worst case, it blocks you entirely, and your crawler has just permanently lost the ability to index that domain. At real scale, with hundreds of fetcher workers pulling from one shared queue, this isn't a rare edge case — it's the default outcome unless something explicitly prevents it.

This is the **politeness problem**, and a plain FIFO queue simply cannot solve it, because a FIFO queue has no concept of "host." It hands out whatever URL is next in line, with zero awareness that the URL two positions behind it in the queue points at the exact same server.

### What the Frontier Actually Is, As a Concrete System, Not a Metaphor

The structure that solves this is usually called the **URL frontier** (a well-known pattern from Google's original Mercator crawler design). It replaces both the fetcher queue *and* the ad-hoc "insert into `urls` table" logic from v0 with one coherent component that owns three jobs: deciding what's allowed to be fetched (politeness), deciding what's worth fetching first (priority, Chapter 4), and handing out exactly one URL at a time per eligible host.

Concretely, the frontier is made of two pieces of state, both of which need a real home — this is the part the earlier version left vague:

**1. Per-host cooldown state**, stored in a fast key-value store (Redis is the natural fit — this needs sub-millisecond reads on every single dispatch decision):

```
key:   host:example.com
value: { next_allowed_fetch_time: 14:32:05, crawl_delay: 2s, robots_disallow: [...], robots_fetched_at: ... }
```

**2. Per-host pending-URL queues.** Rather than one global FIFO, the frontier keeps one queue *per host* (or, at scale, a set of queue shards that many hosts map onto via consistent hashing — Chapter 12). A URL waiting to be crawled sits in its host's queue, not in some shared undifferentiated list.

### Who Writes Into the Frontier

Two producers, and only two:

- **The seed loader**, once, at crawl start — admins provide an initial list of seed URLs, which are inserted directly into their respective hosts' frontier queues with default priority.
- **Parser workers**, continuously — every time a Parser worker extracts links from a page (Chapter 2, step "push newly discovered URLs back into the system"), each extracted URL goes through normalization and dedup (Chapter 4) and, if it survives both, gets appended to its host's queue in the frontier. This is the only path by which new URLs enter the system after the seed load.

Nothing else writes to the frontier. Fetcher workers only ever *read* from it.

### Who Reads From It, and Exactly How a Dispatch Decision Gets Made

A **dispatcher** — logically a separate lightweight service, though it can be a client-side library each fetcher worker calls — sits between the frontier's stored state and the fetcher worker pool. When a fetcher worker asks for work, the dispatcher does this, every single time, with no exceptions:

1. Look at the set of hosts that currently have at least one pending URL in their queue.
2. Filter that set down to hosts where `now >= next_allowed_fetch_time` (read from Redis).
3. Pick one eligible host (Chapter 4 covers *which* one, by priority — for now, assume any eligible host).
4. Pop the front URL off that host's queue.
5. **Immediately** update that host's Redis entry: `next_allowed_fetch_time = now + crawl_delay`. This update happens at dispatch time, not at fetch-completion time — the cooldown starts the moment a fetch is handed out, not when it finishes. This matters: if it were set at completion time instead, a host with a slow-responding server would get *extra* unintended delay stacked on top of its `crawl_delay`, and if two dispatches somehow raced before either fetch finished, the second could dispatch too early.
6. Hand the URL to the requesting fetcher worker.

```
Per-host state (Redis):
  host: "example.com"
  next_allowed_fetch_time: 14:32:05
  crawl_delay: 2 seconds        -- from robots.txt, or a sane default if unspecified

Dispatch logic (concrete):
  eligible = hosts with a non-empty queue AND now >= next_allowed_fetch_time
  if eligible is empty: worker gets nothing this tick, retries shortly
  else: host = pick_highest_priority(eligible)   -- Chapter 4
        url = host.queue.pop_front()
        host.next_allowed_fetch_time = now + host.crawl_delay   -- set immediately
        return url to worker
```

### A Worked Example, So the Numbers Aren't Abstract

Say a Parser worker finishes parsing `example.com/home` and extracts 40 links, 12 of which point back to other pages on `example.com` and pass dedup. **All 12 get appended to `example.com`'s single frontier queue** — they do *not* each get their own `next_allowed_fetch_time`. There is exactly one cooldown timer per host, shared by every URL waiting in that host's queue, because the cooldown is a property of the host's tolerance for request rate, not a property of any individual URL.

So if `example.com`'s `crawl_delay` is 2 seconds and its queue now has 12 pending URLs, the dispatcher will drain that queue at one URL every 2 seconds — roughly 24 seconds to work through all 12 — regardless of how many fetcher workers are idle and available. The other fetcher workers spend that time serving *other* hosts' queues instead of sitting idle, which is the entire point of having many hosts' queues live in the frontier simultaneously.

### Where `crawl_delay` Comes From, and When It's Checked

Most sites publish a `robots.txt` at their root (`example.com/robots.txt`) that can specify a `Crawl-delay` directive directly, along with `Disallow` paths the crawler must not fetch at all. Respecting this file isn't optional politeness — for many crawlers it's the actual legal/contractual boundary of permitted access.

The concrete flow: the **first time** the frontier is about to add a URL for a host it has no cached policy for, it triggers a one-off fetch of `host/robots.txt` (via the same fetcher pool — this is just another fetch, tagged internally so it isn't itself infinitely deferred waiting on a robots.txt policy that doesn't exist yet), parses out the `Disallow` paths and `Crawl-delay`, and writes the result into that host's Redis entry (`robots_disallow`, `crawl_delay`, `robots_fetched_at`). Every subsequent URL for that host — whether from the seed list or newly discovered — is checked against the cached `robots_disallow` list *before* it's ever inserted into the host's queue; a disallowed path is dropped at insertion time, never queued at all. If a host specifies no `Crawl-delay`, a sane default (e.g. 1 second) is written instead of leaving the field empty.

The cached policy isn't fetched forever — it's re-fetched periodically (e.g. every 24-48 hours, itself just another low-priority entry in that host's queue) in case a site changes its policy, rather than trusting a policy snapshot indefinitely.

### The Counter-Intuitive Capacity Number This Creates

Here's the point flagged back in Chapter 0: politeness means each individual host can only be fetched from at a rate of roughly `1/crawl_delay` requests per second — often just 0.5–1 req/sec per host. To sustain our target of ~120 URLs/sec in aggregate while respecting that per-host ceiling, the system needs to be fetching from **many different hosts concurrently**, not fewer hosts faster.

If `crawl_delay` averages 1 second, sustaining 120 req/sec means the frontier needs on the order of 120+ *distinct hosts* with outstanding, in-flight requests at any given moment — which in turn means the fetcher worker pool needs enough concurrent connections (hundreds, easily) to keep that many hosts simultaneously in flight, even though each individual host is only being touched once a second or slower. This is why real crawler fetcher fleets look less like "a few fast workers" and more like "hundreds or thousands of lightweight, mostly-waiting connections" — the bottleneck is concurrency breadth across hosts, not raw per-host throughput.

### What We Gave Up

The frontier is meaningfully more complex than a plain queue — it needs per-host state in a fast store, a `robots.txt` cache with its own refresh policy, and a dispatcher that's aware of cooldowns rather than pure FIFO ordering. That's a real cost in implementation complexity, worth naming explicitly: we traded "simple queue" for "correctness that keeps the crawler from getting itself banned," which is a trade every real crawler has to make, not an optional refinement.

---

## Chapter 4: The URL Frontier, Completed — Duplicate URLs and Priority

Politeness (Chapter 3) solved *which host* gets a URL next, and established that the frontier — not a plain queue — is where new URLs land. Two more problems live in that same insertion path: **avoiding re-crawling URLs we've already seen**, and **deciding which pages matter more than others** when not everything can be crawled immediately.

### Where Exactly Dedup Happens in the Pipeline

To be precise about ordering, since this is easy to get subtly wrong: when a Parser worker extracts a raw link string from a page's HTML, it does **not** go straight into the frontier. It passes through two gates first, in this order:

**Gate 1 — Normalization**, applied to the raw string *before anything else touches it*: lowercase the host, strip known tracking query parameters (`utm_source`, `fbclid`, etc.), resolve relative paths against the page's base URL, remove default ports (`:80` on `http`), sort remaining query parameters alphabetically. This collapses a large fraction of "different-looking, same-page" URLs — `example.com/page?utm_source=twitter` and `example.com/page` — into one canonical string before either is ever compared to anything.

**Gate 2 — The seen-set check**, applied to the *normalized* string: has this exact normalized URL already been queued or crawled? At 1B+ URLs, a literal set of every seen URL is enormous to check against on every single discovered link, and this check happens on *every one* of the many links extracted from *every* parsed page — it needs to be fast and cheap or Parser workers stall on dedup instead of parsing.

The standard fix is a **Bloom filter** — a compact probabilistic structure, held in memory, that a Parser worker queries directly (or via a small lookup service if the filter is too large for one process, see Chapter 12) before ever handing a URL to the frontier:

- **"Definitely not seen"** → skip the expensive check entirely, insert into the frontier immediately.
- **"Possibly seen"** → the filter can have false positives but never false negatives, so this result needs a real lookup against the URL store (a simple keyed read: does a row for this normalized URL already exist?) to confirm before deciding.

A Bloom filter sized for a few billion entries at a reasonable false-positive rate (~1%) fits comfortably in memory on a modest cluster. Only URLs that pass *both* gates — normalized, and confirmed not-yet-seen — are inserted into the frontier's per-host queue (Chapter 3) and added to the Bloom filter so future duplicates of it are caught.

### Priority — Not Every URL Deserves the Same Urgency

A plain FIFO frontier treats a link discovered on a major news homepage the same as a link three hops deep on an obscure personal blog. In practice, some signals are worth weighting the frontier by:

- A page's estimated **PageRank-style importance** (how many other pages link to it).
- How frequently a page **historically changes** (news homepages should be recrawled far more often than a static "About Us" page — this feeds directly into Chapter 10's recrawl scheduling).
- **Domain diversity** — if the frontier is currently dominated by URLs from one enormous site, deliberately interleave in URLs from other hosts so the crawl doesn't spend an entire day exhaustively finishing one domain before starting any others.

### How Priority Actually Changes the Dispatch Logic

This becomes a **multi-level priority structure**, layered on top of everything Chapter 3 already built — not a replacement for it. Concretely: instead of one queue per host, each host has **several tiers** (high/medium/low), and a URL is assigned a tier at insertion time based on the signals above (computed from the linking page's own known importance and the target URL's prior change history, if any — a brand-new URL with no history gets a default middle tier).

The per-host cooldown state from Chapter 3 (`next_allowed_fetch_time`) is unchanged — it still governs the host as a whole, across all its tiers combined, since the host doesn't care which tier a request came from. What changes is step 4 of the dispatcher logic ("pop the front URL off that host's queue") — the dispatcher now checks the host's high tier first, then medium, then low, but does so with **weighted round-robin across tiers** so low-priority URLs are never fully starved, just deprioritized: for example, the dispatcher might be configured to pull from high:medium:low in a 5:3:1 ratio across successive dispatches for hosts that have URLs waiting in all three tiers.

```
Per-host queue structure (extended):
  host: "example.com"
  queues: { high: [...], medium: [...], low: [...] }
  next_allowed_fetch_time / crawl_delay: unchanged from Chapter 3, applies across all tiers

Dispatch logic (extended):
  eligible_hosts = hosts with now >= next_allowed_fetch_time AND at least one non-empty tier
  host = pick_highest_priority(eligible_hosts)
  tier = weighted_round_robin(host, ratio=5:3:1)   -- picks which tier to pull from this time
  url = host.queues[tier].pop_front()
  host.next_allowed_fetch_time = now + host.crawl_delay
```
## Chapter 5: Duplicate *Content* — When Different URLs Hide the Same Page

URL-level dedup (Chapter 4) catches the case where the *address* is redundant. It does nothing for the much more common real-world case: genuinely different URLs — different hosts, even — serving near-identical content. A press release syndicated across fifty news sites, a product description copy-pasted across a dozen resellers, or a page mirrored under both `www.` and non-`www.` hosts. Indexing all fifty copies wastes storage, wastes crawl budget that could have gone toward genuinely new content, and — worse — pollutes search results with near-duplicate entries competing for the same ranking slots.

### Where This Check Runs, Concretely

This entire check happens inside the **Parser worker**, immediately after content extraction and before the extracted content is written to the document store (Chapter 7). It is not a separate batch job — it has to happen per-page, synchronously within the parse step, because the decision it makes ("index this" vs. "skip, it's a duplicate") determines what gets written at all.

### Exact Duplicates, First — the Cheap Case

The Parser worker computes a plain cryptographic hash (SHA-256 is fine) over the extracted, cleaned text — not the raw HTML, since two pages with identical visible text but different `<div>` nesting or inline styling should still count as duplicates. This hash is checked against a **hash → canonical URL** lookup table (a simple keyed store, small enough to be a straightforward indexed table rather than needing Bloom-filter-style approximation, since it's one hash per unique piece of content rather than one entry per URL).

- **No match** → this is new content. Write the hash into the lookup table pointing at this URL, and proceed to indexing normally.
- **Exact match** → this page's text is byte-for-byte identical (post-cleaning) to a page already indexed. Record the URL (so it isn't endlessly rediscovered and reprocessed — it gets marked `done` in the URL store like any other crawled URL) but skip indexing it, storing instead a pointer from this URL to the canonical one already in the table.

A single-bit change anywhere in the text completely changes a normal cryptographic hash, so exact hashing catches copy-pasted duplicates but misses the far more common near-duplicate case: a syndicated article with a different byline, a different "related articles" sidebar, or a live-updated timestamp.

### Near-Duplicates — SimHash, Computed and Compared Concretely

The standard tool here is **SimHash** (or the closely related MinHash): instead of producing a hash that changes completely with any input change, SimHash produces a fixed-length fingerprint (commonly 64 bits) where **similar inputs produce fingerprints that differ in only a small number of bits**.

**How the Parser worker computes it, step by step:**

1. Tokenize the cleaned body text into words or short phrases (shingles), the same tokenization used later for indexing (Chapter 8), so this work is reusable rather than duplicated.
2. Hash each token individually into a fixed-width bit string (e.g. 64 bits) using a standard hash function.
3. For each of the 64 bit positions, maintain a running sum across all tokens: add +1 to that position's sum if the token's hash has a 1 in that position, subtract 1 if it has a 0 (optionally weighted by the token's frequency in the document, so a word appearing 20 times pulls the sum harder than a word appearing once).
4. After all tokens are processed, the final fingerprint bit at each position is 1 if that position's sum ended positive, 0 otherwise.

The result: two pages that are 95% identical text will have SimHash fingerprints with a small Hamming distance (the count of differing bits) between them — typically single digits out of 64 — while two genuinely unrelated pages will differ in roughly half their bits, as random noise would.

**Comparing against "recently seen" pages, without scanning everything:** the naive approach — comparing a new page's fingerprint against every other fingerprint ever computed — doesn't scale past a trivial corpus size. The practical fix is **LSH banding**: split each 64-bit fingerprint into several smaller bands (say, four 16-bit bands), and index pages by each band value in a lookup table (`band_value → [url_ids with that band value]`). Two fingerprints that are genuinely close will very likely share at least one band exactly, so the Parser worker only needs to look up "which other URLs share any of my four band values" — a small, indexed lookup — rather than a full-corpus scan, and only compute exact Hamming distance against that short candidate list.

A distance below a tuned threshold (commonly 3–4 bits out of 64) marks the page as a near-duplicate; the crawler still records the URL (so it doesn't get endlessly re-discovered and re-checked) but skips indexing the redundant content, or indexes it with a pointer to the canonical version instead — the same outcome as the exact-duplicate path above, just reached via an approximate rather than exact match.

The computed SimHash fingerprint is stored alongside the page's other extracted metadata in the document store regardless of the outcome — it's needed again in Chapter 10, to detect whether a *recrawled* version of a page has actually changed since last time.

---

## Chapter 6: JavaScript Rendering — An Expensive Tool Used Selectively

### Why This Can't Just Be "On by Default"

A meaningful fraction of the modern web renders its actual content client-side — the raw HTML Fetcher downloads is a near-empty shell plus a bundle of JavaScript that builds the real page in the browser. A plain HTTP fetch-and-parse pipeline (Chapters 1–2) sees only that empty shell and extracts essentially nothing useful.

The fix — running a real, headless browser (Chromium via a tool like Puppeteer/Playwright) that actually executes the page's JavaScript before handing the fully-rendered DOM to the parser — genuinely works. But it is **dramatically more expensive** than a plain HTTP fetch: a headless browser instance consumes far more CPU and memory per page, takes meaningfully longer to "finish" (you have to wait for scripts to execute and the DOM to settle, not just for bytes to arrive), and doesn't parallelize nearly as cheaply as thousands of lightweight HTTP connections do. Rendering every one of our ~120 URLs/sec through a headless browser farm by default would multiply the fetcher fleet's resource cost by an order of magnitude or more, for a benefit that most pages — plain server-rendered HTML — don't actually need.

### The Fix, as an Exact Three-Step Flow

**Step 1 — try the cheap path first, always.** Every URL, with no exceptions, gets the normal Chapter 1–2 plain-HTTP fetch and parse first. There is no upfront classification of "this domain probably needs JS" — the system always attempts the cheap path before ever considering the expensive one, because guessing wrong in the other direction (assuming a page needs rendering when it doesn't) is exactly the cost this whole chapter exists to avoid.

**Step 2 — detect whether rendering is likely needed, inside the same Parser worker that just extracted content from the cheap fetch.** Two cheap, purely-textual heuristics run on what was already extracted, with no new network calls:

- Is the extracted body text suspiciously short relative to the page's raw byte size (e.g. under some ratio threshold, tuned empirically)? A strong sign the real content is JS-rendered and the raw HTML is mostly an empty shell.
- Does the raw HTML contain telltale signatures of a client-rendered framework — a near-empty `<div id="root">` or `<div id="app">` typical of React/Vue/Angular apps, or heavy reliance on `<script src=...>` bundles with almost no server-rendered text in the body?

If either heuristic trips, the Parser worker does **not** treat this page as done. Instead of writing (thin, likely-useless) extracted content to the document store, it publishes a message onto a separate **render queue**: `{ url, url_id }`.

**Step 3 — a dedicated headless rendering worker pool** consumes from the render queue, provisioned and scaled completely independently from the main Fetcher and Parser pools — same separation-of-concerns principle as Chapter 2, since a slow, resource-heavy rendering job shouldn't be able to starve the plain-HTTP fetchers that handle the bulk of the web. Each rendering worker launches (or reuses from a warm pool) a headless browser instance, navigates to the URL, waits for the page to settle (a bounded wait — a few seconds, not indefinite, since some pages never fully "settle"), and extracts the fully-rendered DOM's `<title>`, body text, and links exactly as the normal Parser would. This result is written to the document store the same way a normal parse result is — from the document store's point of view, a rendered page and a plain-HTTP page look identical; the rendering step is invisible downstream.

This pool is deliberately sized for a small fraction of total crawl volume — commonly cited real-world estimates put the JS-rendering-required slice of the web in the 10–20% range, which changes the headless farm's required capacity from "as big as the entire fetcher fleet" to "a fraction of it."

**What we traded:** some pages that genuinely need rendering but don't trip the heuristics will occasionally get indexed with thin or missing content — an acceptable, tunable false-negative rate in exchange for not paying the full rendering cost on the ~80-90% of the web that never needed it in the first place.

---

## Chapter 7: Storage — Raw Pages, Extracted Content, and Where Each One Lives

Three genuinely different kinds of data come out of this pipeline, and — choosing storage by access pattern rather than using one store for everything — each deserves its own home, with its own concrete key scheme so "where does this live" is never a vague answer.

**Raw HTML** (the ~1MB-10MB-per-page blobs Fetcher downloads) → **blob/object storage** (S3-style), not a database. Keyed as introduced in Chapter 2: `raw/{url_id}/{fetch_timestamp}.html`. This data is opaque, large, written once per fetch, and read rarely after parsing — mainly for reprocessing if extraction logic changes, for debugging, or (Chapter 10) for SimHash comparison against a prior version when checking whether a page changed. At the ~10PB scale computed in Chapter 0, a relational database was never a realistic option for this tier regardless of design choices elsewhere.

**Extracted structured content** (title, cleaned body text, outbound links, SimHash fingerprint, metadata like `last_crawled_at` and `recrawl_interval`) → a **sharded document store**, keyed by `url_id` (a Snowflake-style ID assigned when a URL first enters the `urls` table), with the shard derived directly from a hash of `url_id` — the same shard-routing trick used to avoid a separate lookup step when routing a `tweets` table by ID in a newsfeed design. This is what the indexing pipeline (Chapter 8) actually consumes; it's dramatically smaller than the raw HTML tier (the ~50KB-extracted-text-per-page estimate from Chapter 0) and is read far more frequently — every batch indexing run scans it, and every recrawl-scheduling pass (Chapter 10) reads its `last_crawled_at` field.

**The inverted index itself** → a purpose-built, heavily sharded search-serving store (Chapters 8-9), keyed by *term* rather than by document, optimized for the very different access pattern of "find every document containing this term, fast" — which looks nothing like either of the two stores above and is covered on its own in the next chapter.

**Media references** — per Chapter 0's scoping decision — are stored as plain URL strings alongside the extracted content (an `<img src>` or `<video src>` value captured verbatim during parsing), never as downloaded bytes. This keeps the storage numbers above accurate and keeps the system honest about scope: a reverse-image-search feature would be built as its own system on top of these references, not folded into this one.

---

## Chapter 8: From Crawled Pages to a Searchable Index

### The Core Data Structure: The Inverted Index

A normal document store answers "what words are in document X?" A search engine needs the *opposite* question answered fast: "which documents contain the word 'python'?" An **inverted index** is exactly that — a mapping from each term to the list of documents (a **postings list**) containing it:

```
"python"    -> [doc_1043, doc_88291, doc_5, doc_920014, ...]
"crawler"   -> [doc_5, doc_71, doc_920014, ...]
"web"       -> [doc_1, doc_2, doc_3, ... ]   -- extremely common term, huge postings list
```

Each entry in a postings list typically carries more than just the document ID — term frequency within that document, and often the term's position(s) within the text, which is what allows phrase queries ("web crawler" as an exact phrase, not just both words present anywhere) to work later at query time.

### Building It: A Batch Pipeline, Not a Live Write Path — Triggered How, and When

Building the index isn't something that happens synchronously as each page is parsed — at billions of documents, that would mean every single crawl event contending for write access to a shared, correctness-critical structure. Instead, this is a natural fit for a **MapReduce-style batch pipeline**, run on a fixed schedule — for example, once every few hours — rather than triggered per-document.

**What feeds the batch job, concretely:** each run of the pipeline queries the document store (Chapter 7) for every row where `updated_at` falls after the previous run's cutoff timestamp — i.e., every document that's new or been re-parsed since the last indexing pass. This is the mechanism that ties the crawl pipeline to the indexing pipeline: Parser workers never talk to the indexing pipeline directly; they just keep the document store current, and the batch job picks up whatever's changed since it last ran.

**Map phase** — for each document in that batch, tokenize its extracted text (split into words, lowercase, strip punctuation, remove stopwords like "the"/"a"/"is" that carry little search value, and typically apply stemming so "crawling" and "crawl" match the same searches — the same tokenization logic the Parser worker already used for SimHash in Chapter 5) and emit `(term, doc_id, position)` tuples, one per occurrence of each term in the document.

**Shuffle phase** — group all the emitted tuples by term, so every occurrence of "python" across the entire batch of documents ends up together on one machine for the reduce step, regardless of which map worker originally processed the document it came from.

**Reduce phase** — for each term, merge all its tuples into a single, sorted postings list (sorted by doc_id, which is what makes later intersection across terms efficient at query time), and write that list into the sharded index store.

### Index Sharding, and Why It's the Opposite of a Feed System

With a term-space this large, a single index shard sharded by *term* (all postings for "python" always live on the same shard) is the natural choice — this is the inverse of a user-sharded feed system's approach, and the difference is worth naming explicitly: shard by whatever the dominant query pattern looks up by. Here, queries look up by term, so shard by term (typically via a hash of the term string onto N index shards), keeping every term's full postings list on one shard and avoiding the need to merge partial postings lists across shards for a single-term query. Multi-term queries (Chapter 9) do still require querying multiple shards and intersecting results — an unavoidable cost, but a much smaller one than fragmenting every single term's own postings list.

### Incremental Updates — Segments, Not Full Rebuilds

Rebuilding the entire multi-terabyte index from scratch every time new pages are crawled doesn't scale, and it would mean search results lag newly-crawled content by however long a full rebuild takes. Instead, each scheduled run of the Map→Shuffle→Reduce pipeline above produces a small, self-contained **index segment** — a postings-list structure covering only the documents in that run's batch. This segment is written alongside the existing index (not merged into it immediately), and query time (Chapter 9) checks both the large, stable main index *and* every small, fresh segment produced since, merging results across all of them for a given term.

Periodically, on a slower background schedule (e.g. daily), a separate merge process folds accumulated small segments into the main index, so the number of segments a query has to check doesn't grow without bound. This pattern is directly borrowed from how systems like Lucene/Elasticsearch structure segment merging, and it trades a slightly more complex read path (check N segments instead of 1 structure) for freshness that doesn't require rebuilding a 10-15TB structure on every crawl cycle.
## Chapter 9: Search Serving — Turning a Query Into Ranked Results

### The Read Path, Step by Step

```
GET /search?q=web+crawler+design
```

1. **Tokenize the query** exactly the way documents were tokenized during indexing (Chapter 8) — lowercase, stopword removal, stemming — so the query's terms match the same vocabulary the index was built with: `["web", "crawler", "design"]`. Using a different tokenizer here than the one used at index time is a common, subtle bug — a query for "crawling" would silently fail to match documents indexed under the stemmed form "crawl" if the two paths ever drift apart.
2. **Route each term to its index shard**, by applying the same hash-of-term function used when the index was built (Chapter 8), and fetch that term's postings list (or lists — one per matching segment, per Chapter 8's segment structure) from the shard(s) that own it. A three-term query like this one, if the terms hash to different shards, means three parallel lookups, not three sequential ones.
3. **Intersect/merge the postings lists** to find documents containing all (or most) of the query terms — a straightforward sorted-list intersection, which is exactly why postings lists were stored sorted by `doc_id` back in Chapter 8.
4. **Rank the candidate documents** (below).
5. **Return the top N**, hydrating title and a snippet for each result from the document store (Chapter 7) using each result's `doc_id` — the index itself never stores full titles or bodies, only postings data, keeping it as small as Chapter 0's numbers assumed.

### Ranking, Kept Deliberately Simple but Named Correctly

Full ML-based ranking is out of scope (Chapter 0), but a baseline relevance signal is worth naming precisely rather than hand-waved, because "how do you rank results" is one of the most reliably-asked follow-ups in this entire design.

The standard classical baseline is **TF-IDF** (term frequency–inverse document frequency) or its refinement, **BM25**: a document scores higher for a term the more often that term appears in it (term frequency), but terms that appear in *almost every* document in the corpus (like "web," in a corpus of technology pages) are down-weighted, since their presence carries little discriminating power (inverse document frequency, computed from how many total documents a term's postings list covers — information already sitting right there in the index). BM25 additionally accounts for document length, so a term appearing 3 times in a 50-word page counts for more than the same term appearing 3 times in a 5,000-word page.

This produces a purely lexical relevance score, computable directly from the postings lists' stored term-frequency data (Chapter 8) with no external ranking model needed and no extra data fetched beyond what step 2 above already pulled — a genuinely defensible, classical answer that predates and still underlies parts of modern search ranking.

### Serving at Scale — Caching, Load Balancing, Replicas, Made Concrete

The same patterns that keep a feed-serving system fast apply here with almost no modification, and it's worth stating exactly what each one does in this system's terms rather than assuming the analogy is self-explanatory.

**Popular queries get cached.** A plain key-value cache (e.g. Redis) keyed on the *normalized* query string (tokenized the same way as step 1 above, so `"Web Crawler"` and `"web crawler"` hit the same cache entry) with a short TTL — short specifically because Chapter 8's segments mean the index itself is updated incrementally, so a long-lived cached result set could go stale relative to freshly-merged segments. This catches the heavily-skewed head of query traffic: a small number of queries account for a disproportionate share of total search volume, the same power-law shape that drives caching decisions in most read-heavy systems.

**Stateless, load-balanced query servers** sit in front of the sharded index. A query server has no persistent state of its own — every request independently does steps 1-5 above, fanning out to whichever shards the query's terms hash to, merging results in that request's own memory. Because there's no per-server state, any query server can handle any request, and a load balancer distributes incoming requests across the pool with simple round-robin or least-connections routing.

**Read replicas of each index shard** absorb query load, since index shards are read overwhelmingly more than they're written — writes to a shard only happen when Chapter 8's batch pipeline produces a new segment for that shard, which is infrequent (hours) compared to query volume (constant). Each shard's primary handles segment-merge writes; multiple read replicas per shard, kept in sync via standard replication, handle the actual query traffic, and query servers round-robin their reads across a shard's available replicas the same way DB read replicas absorb read load in most read-heavy systems.

---

## Chapter 10: Freshness — Deciding What to Recrawl, and How Often

### Why "Crawl Once" Isn't Good Enough

A crawl that never revisits a page produces an index that quietly rots. A news homepage that changes every few minutes, indexed once, becomes actively wrong within the hour. A static documentation page, by contrast, might not change for a year — recrawling it daily would be pure waste of the same politeness-constrained crawl budget established in Chapter 3.

### Where This State Lives, and Who Updates It

Two fields live on every document's row in the document store (Chapter 7), alongside its extracted content: `recrawl_interval` and `next_recrawl_due_at`. Neither is set once and forgotten — both are updated every time the page is recrawled, by the Parser worker handling that recrawl, immediately after it computes the page's new SimHash fingerprint (Chapter 5).

### The Adaptive Algorithm, Concretely

**On a page's very first crawl**, there's no history to compare against, so the Parser worker assigns a default `recrawl_interval` (a moderate baseline — say, 7 days) and sets `next_recrawl_due_at = now + 7 days`.

**On every subsequent crawl of that same URL**, the Parser worker compares the newly computed SimHash fingerprint against the one stored from the previous crawl (both are just fields on the same document-store row, so this is a local comparison, not a lookup elsewhere):

- **Hamming distance below the near-duplicate threshold (unchanged, per Chapter 5)** → the content is effectively the same as last time. Lengthen `recrawl_interval`, commonly by doubling it, up to a capped maximum (e.g. 90 days) — a static page that hasn't changed across several consecutive checks converges toward that cap, freeing crawl budget for pages that actually change. Set `next_recrawl_due_at = now + new_interval`.
- **Hamming distance above the threshold** → the content changed. Shorten `recrawl_interval`, commonly by halving it, down to a floor (e.g. 15 minutes, for the most volatile pages) — a news homepage that changes on nearly every check converges toward that floor. Set `next_recrawl_due_at = now + new_interval`.

This is the same reasoning as TTL-based cache expiration, applied in the opposite direction: there, staleness is tolerated up to a fixed window; here, the *window itself* adapts based on observed behavior, converging independently per page rather than using one global setting for the entire crawl.

### How a Due Recrawl Actually Gets Back Into the Frontier

`next_recrawl_due_at` sitting on a row in the document store doesn't, by itself, cause anything to happen — something has to notice it's due and act on it. A separate lightweight scheduled job (running frequently, e.g. every few minutes) queries the document store for rows where `next_recrawl_due_at <= now`, and for each one, re-inserts that URL into the frontier (Chapter 3) exactly the way a newly-discovered URL would be — same per-host queue, same politeness cooldown applies, but assigned to the **high priority tier** (Chapter 4) specifically because a due recrawl is, by construction, a page the system already believes is worth checking again, not a brand-new unknown URL of default priority. This is the concrete mechanism behind Chapter 4's earlier claim that "a page's current recrawl due-time becomes another priority signal in the multi-level frontier" — it's this scheduled job that turns the signal into an actual queue insertion.

---

## Chapter 11: Fault Tolerance — What Happens When a Worker Dies Mid-Crawl

Every failure mode below is walked through as "what state exists, what breaks, what the fix reads/writes" rather than named and left there.

**A fetcher worker crashes mid-download.** The URL it was working on needs to become available for another worker to retry, not silently lost. This is enforced with **at-least-once delivery** from a durable queue (Kafka-style, with explicit acknowledgment): a fetcher worker does not ack a URL as consumed from the fetcher-facing side of the frontier until the fetch, the blob write, and the parser-queue publish (Chapter 2) have all genuinely succeeded, in that order. If the worker crashes at any point before that final ack, the message is redelivered to another worker after a visibility timeout expires. Because re-fetching the same URL twice is harmless — it just writes a new timestamped blob under the same `url_id` prefix (Chapter 7's key scheme already anticipates this) and re-triggers parsing, which is itself idempotent, since parsing the same HTML twice produces the same extracted content and the same SimHash — retries are safe by construction. This **idempotency** property is what makes "just redeliver it" a complete fix rather than something that risks double-counting or duplicate index entries.

**A host is unexpectedly slow or unresponsive.** Without a timeout, a fetcher worker can hang on one bad host indefinitely, holding a connection open and doing nothing useful — this is precisely the gap left open back in Chapter 1's v0 loop. The fix has two parts, both concrete: a hard per-request timeout (a few seconds, tuned empirically — long enough for slow-but-real servers, short enough that one bad host can't monopolize a worker) on every fetch; and a **circuit breaker** per host, tracked as another field on that host's Redis entry (Chapter 3) — after some threshold of consecutive failures (e.g. 5 in a row), the dispatcher stops handing out that host's URLs entirely for a cooldown period (e.g. 30 minutes), rather than continuing to burn worker capacity retrying a host that's clearly down. After the cooldown, the breaker resets to half-open — the next single request is allowed through as a probe, and its outcome decides whether the breaker fully closes (host recovered) or trips again.

**The indexing batch job fails partway through.** Because index building (Chapter 8) is a batch MapReduce-style pipeline that produces discrete, self-contained segments rather than writing directly into a single unbroken live structure, a failed run simply doesn't produce a segment that gets added to the set query servers check — the previous, still-valid main index and any earlier successfully-produced segments remain completely untouched, and search continues serving from them exactly as before. This is a meaningful structural advantage over a hypothetical live-write index: a batch pipeline's failures are naturally isolated to "this run didn't produce output," never "the index is now corrupted mid-write," because nothing downstream ever observes a partially-written segment.

---

## Chapter 12: Scaling to 1B+ URLs — What Actually Changes at 10x

Everything above was designed with 1B+ in mind from Chapter 0's capacity numbers, but it's worth being explicit about which pieces need genuinely more machinery at that scale versus which pieces just need more of the same.

**The frontier needs to be distributed, not just a bigger single structure.** At 1B+ URLs and hundreds of thousands of distinct hosts, a single frontier coordinator (Chapter 3's dispatcher plus Redis instance) becomes its own bottleneck — one process can't hold cooldown state and per-host queues for that many hosts, nor answer dispatch requests fast enough for the whole fetcher fleet. The fix: shard the frontier itself by a **consistent hash of the host string**, so every URL for a given host always routes to the same frontier shard, each running its own dispatcher and its own slice of Redis. This conveniently also means the per-host politeness state (Chapter 3) never needs to be coordinated *across* shards — one shard owns a host's cooldown timer completely and exclusively, so there's no risk of two shards independently believing they're allowed to dispatch for the same host at once.

**Fetcher workers become geographically distributed**, both to reduce latency to geographically distant hosts (the same speed-of-light constraint that governs multi-region deployments applies here in reverse — the crawler isn't serving distant users, it's *fetching from* distant servers, so a fetcher near a target host's region completes each fetch faster, which matters because Chapter 3's per-host cooldown means faster individual fetches translate into more total throughput per host over time) and to spread outbound request volume across a wider range of source IPs, which additionally helps avoid inadvertently looking like a distributed denial-of-service attack from a single IP block.

**The Bloom filter for seen-URL checks (Chapter 4) needs to be sharded too** — at several billion entries even a well-tuned single Bloom filter's memory footprint becomes awkward on one machine. It's partitioned the same way the frontier is, by a hash of the (normalized) URL string, with each shard handling seen-checks for its own slice of the URL space; a Parser worker checking a newly extracted link hashes the URL first to know which Bloom-filter shard to query.

**The index sharding count grows, but the sharding strategy (by term, Chapter 8) doesn't change** — this is worth noting as a point in the design's favor: growing from 100M to 1B+ documents means adding more index shards (re-hashing terms across a larger shard count, with the same term-based scheme), not rethinking the term-based sharding scheme itself, because postings lists for common terms simply grow longer rather than the access pattern changing shape.

---

## Final Recap — The Whole System in One Diagram

```
[Seed URLs] ──► [Frontier: sharded by host, politeness + priority aware] ──► [Fetcher Worker Pool]
                        ▲                                                          │
                        │ new URLs (post normalization + Bloom-filter dedup)       ▼
                        │                                              [Blob Storage: raw HTML]
                        │                                                          │
                        │                                                          ▼
                 [Parser Worker Pool] ◄─────────────────────────────── [Parser Queue]
                        │
          ┌─────────────┼──────────────────────┐
          ▼              ▼                       ▼
  [Extracted content  [SimHash dedup      [Headless render pool
   document store,     check, LSH-banded]  — only for flagged
   sharded by url_id]                        JS-heavy pages]
          │
          │  (recrawl-due scheduler polls next_recrawl_due_at, re-inserts into frontier)
          ▼
  [Batch Indexing Pipeline: Map → Shuffle → Reduce, scheduled every few hours]
          │
          ▼
  [Inverted Index — sharded by term, primary + replicas, main index + fresh segments]
          │
          ▼
  [Query Servers — stateless, load balanced] ◄── [Search API] ◄── [User query]
          │
          ▼
  [Ranked results (BM25), hydrated from document store]
```

**Crawl path:** Frontier's dispatcher hands a politeness-respecting, priority-ordered URL to a fetcher (setting that host's cooldown at dispatch time) → raw HTML written to blob storage under a `url_id`-keyed path → parser fetches from blob storage, extracts content, computes SimHash and dedup-checks it, escalates to headless rendering only if flagged → structured content written to the document store, new links normalized and Bloom-filter-checked before being fed back into the frontier.

**Index path:** Extracted content batched every few hours through MapReduce, driven by `updated_at` timestamps on the document store → term-sharded inverted index, updated via small fresh segments merged into a stable main index on a slower background schedule.

**Freshness path:** every recrawl compares fingerprints against the prior version, adaptively shortens or lengthens that page's own recrawl interval, and a separate scheduler polls for due pages and re-inserts them into the frontier at high priority.

**Search path:** Query tokenized the same way documents were tokenized at index time → relevant term shards queried in parallel → postings merged and ranked (BM25) → results hydrated from the document store and returned.

**The trade-off named once, applying everywhere:** crawling and indexing are eventually consistent with reality by design — a page can change and the index won't reflect it until the next scheduled recrawl (Chapter 10) and the next batch indexing run (Chapter 8) complete. This is the same AP-over-CP instinct that governs most large-scale read-heavy systems, applied to "freshness of the index" — and for a search engine, that's the correct trade: a slightly stale result beats a search engine that's unavailable while trying to stay perfectly current.

---

## The "Why Not X" Arsenal

- **"Why not crawl and index in one live pipeline instead of batching?"** — At billions of documents, a live write path into the index would mean every crawl event contending for write access to a correctness-critical, heavily-read structure. Batching decouples crawl throughput from index-build throughput, and isolates a failed indexing run to "this run didn't produce a segment," never a corrupted live index.
- **"Why not render every page with a headless browser to be safe?"** — Cost. Headless rendering is an order of magnitude more expensive per page than a plain HTTP fetch, and the majority of the web is still substantially server-rendered. Selective escalation based on cheap heuristics, run inside the same Parser worker that already did the cheap fetch, gets most of the benefit for a fraction of the cost.
- **"Why shard the index by term instead of by document?"** — Because the dominant query pattern is "find documents containing term X," sharding by term keeps a full postings list on one shard, avoiding cross-shard merges for single-term queries. Sharding by document would make every query touch every shard.
- **"How do you stop the crawler from getting itself banned?"** — Respect `robots.txt` unconditionally (checked at insertion time, before a disallowed URL ever reaches a queue), enforce a per-host cooldown derived from its crawl-delay (or a sane default) that's set the moment a URL is dispatched, and route via a frontier structure — one queue and one cooldown timer per host — that makes it structurally impossible for multiple workers to simultaneously hit the same host. Not just a policy applied after the fact, but a scheduling guarantee enforced by the dispatcher itself.
- **"What if a page's content changes but its URL doesn't — how do you know to reindex it?"** — Chapter 10's adaptive recrawl scheduler polls the document store for pages whose `next_recrawl_due_at` has passed, re-inserts them into the frontier at high priority, and the Parser worker handling that recrawl compares the new SimHash fingerprint (Chapter 5) against the stored one — only a genuine change triggers new content in the document store, which the next indexing batch run then picks up.

---

*Same arc as always, restated one more time because the repetition is the actual lesson: notice a real, concrete cost, name the trade-off made to fix it, and state plainly what was given up in exchange. Single-server v0 → broken by coupled responsibilities → fixed by fetcher/parser decoupling via a queue → broken by naive parallelism ignoring politeness → fixed by a host-aware, priority-ordered frontier with an explicit dispatcher and explicit cooldown state → hardened with URL and content dedup, selective JS rendering, purpose-built storage tiers, a batch indexing pipeline, adaptive freshness with its own scheduler, explicit fault tolerance, and distribution to 1B+ scale.*

---

## Glossary

- **URL frontier** — the structure that decides which URL a fetcher worker gets next, combining per-host politeness cooldowns, priority tiers, and dedup gates — not a plain FIFO queue. Written to only by the seed loader and Parser workers; read only by the dispatcher. Chapters 3-4.
- **Dispatcher** — the component that mediates between the frontier's stored state and the fetcher worker pool, running the eligibility → pick-host → pop-URL → set-cooldown sequence on every single dispatch. Chapter 3.
- **Politeness / crawl-delay** — the constraint that a crawler must not overwhelm any single host, enforced via one shared `next_allowed_fetch_time` per host, updated at dispatch time (not fetch-completion time). Chapter 3.
- **robots.txt** — a file hosts publish specifying which paths a crawler may not fetch and what crawl delay to respect; fetched once per host, cached, and periodically refreshed. Chapter 3.
- **Bloom filter** — a compact probabilistic structure for "have I seen this normalized URL before" checks at huge scale, queried by Parser workers before a new link is inserted into the frontier. Chapter 4.
- **SimHash** — a fingerprinting technique, computed per-document inside the Parser worker from the same tokens used for indexing, where similar inputs produce fingerprints differing in only a few bits; compared via LSH banding rather than full-corpus scans. Chapters 5, 10.
- **Headless browser rendering** — running a real browser engine to execute a page's JavaScript, triggered only when a Parser worker's cheap heuristics (thin text ratio, empty root `<div>`) flag a page after the normal fetch already ran. Chapter 6.
- **Inverted index** — a mapping from each term to the list of documents containing it (a postings list), sharded by term and built in periodic batch segments. Chapter 8.
- **Postings list** — the sorted-by-doc_id list of documents (plus term frequency/position data) associated with one term in the inverted index. Chapter 8.
- **MapReduce-style batch indexing** — building the index in periodic batch passes (map: tokenize documents; shuffle: group by term; reduce: build postings lists), triggered on a fixed schedule against whatever's changed in the document store since the last run. Chapter 8.
- **Index segment** — a small, freshly-built partial index produced by one batch run, checked alongside the main index at query time and merged into it on a slower background schedule. Chapter 8.
- **TF-IDF / BM25** — classical lexical relevance scoring: score up on term frequency within a document, score down on how common the term is across the whole corpus, computed directly from postings-list data already in the index. Chapter 9.
- **Adaptive recrawl scheduling** — per-page `recrawl_interval` and `next_recrawl_due_at` fields, adjusted (halved or doubled, within floor/cap bounds) by the Parser worker every time a page is recrawled and its new SimHash is compared to the old one; a separate scheduler polls for due pages and re-queues them at high priority. Chapter 10.
