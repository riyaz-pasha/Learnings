# Design a Web Crawler — The Story (narrative edition)

## What this file is

The reference file, `35-Web-Crawler-FAANG-Guide.md`, is the one to recite from. It has the requirements, the frontier data model, every trade-off table, and the master cheat sheet.

This file is a second way in: the same material told as one continuous story, in plain language.

Here's the setup. Engineers at a company keep hitting a wall. They patch it. The patch creates the next wall. Eventually they land on the exact same design the reference file documents.

The company is **ClearPath Search** — a small vertical search startup. Its crawler is nicknamed **Scout**. Both are fictional.

But every wall Scout hits, and every fix the team reaches for, is based on something a real, named system or paper actually does:

- The **Mercator** crawler paper (Heydon & Najork, DEC/Compaq, 1999)
- **robots.txt** (proposed informally by Martijn Koster in 1994, formalized as **RFC 9309** in 2022)
- **Googlebot**'s crawl-budget behavior
- **Common Crawl**'s published dataset size
- Google's own **SimHash** near-duplicate paper (Manku, Jain, Das Sarma, WWW 2007)
- **Bloom filters** (Burton Howard Bloom, 1970)
- **Consistent hashing** (Karger et al., 1997 — the idea behind Akamai and later Amazon's Dynamo)

I'll flag every time whether something is a documented fact or just a reasonable guess. Guesses get an `[illustrative]` tag.

## The trigger phrases

Watch for any of these in an interview:

- *"Design a web crawler."*
- *"Design the crawling layer for a search engine."*
- *"Design a service that discovers and archives millions of URLs without getting itself banned."*

Keep one sentence in your head as you read everything below:

> **A crawler is graph traversal at planet scale, over a graph that keeps generating new nodes while you walk it, under an etiquette contract you didn't write.**

Everything that follows is just this one idea, getting harder in small, honest steps.

---

## Chapter 1 — The script that fell into the calendar

### The starting point

It's 2013. ClearPath is two engineers and an idea: a vertical search engine for recipe blogs.

Scout, their crawler, is a single Python script. Its logic is simple:

1. Start at a seed URL.
2. Fetch the page.
3. Pull out every link on it.
4. Immediately follow the *first* new link it finds.
5. Repeat — recursively, depth-first, on one thread, with no bookkeeping.

Average round-trip per fetch is about **60ms** `[illustrative — a standard planning number, not this one page's actual measurement]`. This is roughly the same "typical HTTP fetch" figure used across most crawler capacity math.

### The trap

Scout's very first real seed is a farmers-market blog with an events page: `/events?date=2024-01-01`.

That page links to "next day" — `/events?date=2024-01-02` — which links to the next day after that, forever.

Depth-first, Scout follows it. Five hours later, at 60ms/fetch, Scout has made roughly:

```
18,000s / 0.06s ≈ 300,000 fetches
```

Every single one of those 300,000 fetches is a calendar page from that one blog. Zero progress has been made on the other 4,999,999 seed URLs sitting untouched in a list somewhere.

Scout isn't crawling the web. It's reading one infinite ledger, forever.

```mermaid
flowchart LR
    subgraph Trap["Scout, depth-first, five hours in"]
        direction LR
        d1["/events?date=2024-01-01"] --> d2["/events?date=2024-01-02"]
        d2 --> d3["/events?date=2024-01-03"]
        d3 --> d4["... 300,000 pages later,<br/>still on this one blog"]
    end
```

### Was this ever going to finish anyway?

Fair question: *even without the calendar trap, was this ever going to finish?*

No. Say ClearPath eventually wants a serious slice of the web — the same planning-scale number the reference guide uses, **5 billion pages**.

At 60ms/fetch, one single thread needs:

```
5,000,000,000 × 0.06s ≈ 300,000,000 seconds ≈ 9.5 years
```

A trap just makes an already-hopeless plan visible on day one, instead of on year nine.

### The fix — and the first analogy for this whole story

Think of the web as a **library where every book cites other books**.

Reading it depth-first means: you follow the *first* citation you see, into the book it cites, into the book *that one* cites — with no way back to the shelf you started on.

The fix is to **read shelf by shelf instead**: look at everything on the current shelf before pulling the next cited book.

That's **breadth-first traversal** — canvass every neighbor before going deeper into any one of them. It's coming in Chapter 2.

### How I'd say this in an interview

> "A naive depth-first crawler has two separate problems that look like one: it's mathematically too slow to cover the web on a single thread, and depth-first order means one cyclic or infinite page — like a calendar with a 'next day' link — can swallow the entire crawler before it ever reaches page two of anywhere else."

---

## Chapter 2 — The block-by-block canvass

### The fix

Switch from depth-first recursion to a **shared queue, walked breadth-first, drained by many workers at once**.

- Every worker pulls the next URL off the **front** of the queue.
- It fetches that page.
- It pushes any brand-new links onto the **back** of the queue.

The result: every domain gets a turn before any one domain gets a second turn.

This is the traversal order every serious web-scale crawler uses — for exactly the reason Chapter 1 just proved. Breadth-first spreads the damage from a bad page. Depth-first concentrates it.

### The numbers

ClearPath sets a real target: crawl **50 million recipe pages within 3 days**.

- Single-worker time at 60ms/fetch:
  ```
  50,000,000 × 0.06s = 3,000,000s ≈ 34.7 days
  ```
  That's over **11x too slow** for the 3-day target.
- Servers needed to hit the 3-day target:
  ```
  34.7 days / 3 days ≈ 12 workers
  ```

ClearPath spins up 12 worker processes, all pulling from one shared BFS queue. The math checks out. The crawl finishes on schedule.

```mermaid
flowchart LR
    subgraph V1["V1: BFS queue + 12 workers"]
        direction LR
        Q["Shared Queue<br/>(BFS order)"] --> W["12 Workers<br/>pulling in parallel"]
        W --> F["Fetch page +<br/>extract links"]
        F --> Q
    end
```

### The new problem, visible by day two

Nobody added dedup.

- A recipe page linked from three different category pages gets enqueued three times and fetched three times.
- Worse: ClearPath's food blogs love session-tracking query strings. `recipe/123?ref=email` and `recipe/123?ref=twitter` are the *same* page, byte for byte — but they look like two different URLs.

**Concrete cost:** with an average of 20 outlinks per page, 50M pages generates:

```
50,000,000 × 20 = 1,000,000,000 raw URL sightings
```

That's **1 billion sightings** before anything gets deduped against the real number of unique pages — a **20x amplification**. Storage and bandwidth are being spent 20 times over on work that should happen once.

### How I'd say this in an interview

> "BFS plus a worker pool solves the *time* problem — it's just dividing the single-worker number by however many workers you run. But a shared queue with no dedup means the same page gets refetched every time a new link to it turns up, and that amplification factor — outlinks per page — is exactly why URL dedup isn't an optional nice-to-have, it's load-bearing from day one."

---

## Chapter 3 — The bouncer with a fingerprint scanner

### The fix

Before enqueuing a URL, or storing content, check it against a checksum store first. There are **two separate checks**, because two different things repeat:

| Check | Catches |
|---|---|
| **URL checksum** | "We've seen this exact address before." |
| **Content checksum** | "Different address, byte-identical page." (the `?ref=email` vs `?ref=twitter` case from Chapter 2) |

### The analogy for the rest of this story

Think of dedup as a **bouncer checking IDs at a door**.

- The URL checksum is checking the **name on the list**.
- The content checksum is checking the **actual fingerprint underneath**.

So even someone who shows up with a fake name (a new query string) still gets caught, because their fingerprint matches someone already inside.

```mermaid
sequenceDiagram
    participant W as Worker
    participant D as Dedup ("the bouncer")
    participant Q as Queue
    participant B as Blob Store

    W->>D: checksum(url), checksum(content)
    D->>D: check against URL & content checksum stores
    alt duplicate (either checksum matches)
        D-->>W: discard
    else new
        D->>Q: enqueue new outlinks
        D->>B: store content
    end
```

### It works — then a new cost shows up

This fix works immediately: the 20x amplification from Chapter 2 collapses back down to roughly 50M unique fetches.

But as the checksum store grows toward hundreds of millions of entries, a new cost appears. Checking "have I seen this URL" now means a real lookup against a store that no longer fits comfortably in memory — and it's on the hot path of *every single fetch*.

**The standard trick** — the same one Mercator-style crawlers and most large-scale "seen URL" tests actually use — is a **Bloom filter** as a cheap first pass:

- It's a small bit array.
- It can say "definitely never seen" in one memory access.
- It falls back to the real on-disk store only on a "maybe seen" hit.

**Sizing example:** for 1 billion URLs at a 1% false-positive rate, the formula `m ≈ n × 9.6 bits` works out to about **1.2 GB of memory** `[illustrative — the formula is real, the exact sizing choice here is a stand-in]`. Compare that to gigabytes of random disk lookups otherwise.

### The next new problem: the fingerprint scanner is *too* exact

The bouncer's fingerprint scanner is *exact*. One changed byte produces a completely different checksum — that's by design, and it's exactly what makes MD5/SHA-1 useful as exact-match tools.

But food blogs syndicate content. The same recipe, reposted on a partner site with:

- a different photo,
- a different byline,
- a different ad banner,

...is a *different* set of bytes, even though a human would call it the same page.

ClearPath finds this is common: roughly **30% of the crawled recipe corpus** turns out to be syndicated near-duplicates that exact checksums never catch `[illustrative — a plausible share for a syndication-heavy niche, not a measured figure]`.

### How I'd say this in an interview

> "Dedup needs two checksums, not one — URL-level for repeated addresses, content-level for the same bytes at different addresses. At real scale, checking a checksum store on every fetch is itself expensive, so a Bloom filter as a fast probabilistic pre-filter is the standard move. But exact checksums are blind to near-duplicates by design — a one-byte change produces a totally different hash — and that's a separate problem."

---

## Chapter 4 — The article that looks familiar

### The fix

Add a second dedup pass that tolerates *small* differences instead of demanding an exact match.

**SimHash** is the tool for this — a real, documented technique from Google's own 2007 WWW paper (Manku, Jain, Das Sarma, *"Detecting Near-Duplicates for Web Crawling"*).

How it works:

- It builds a compact fingerprint (commonly 64 bits) out of a document's shingled features.
- *Similar* documents produce fingerprints that differ in only a *few* bits.
- An exact hash, by contrast, would have scattered similar documents' checksums randomly, with no relationship between them.

### Worked example

This uses the same shape of numbers the paper and the reference guide both use. Two fingerprints are treated as near-duplicates if they differ in **≤3 bits out of 64**.

| Scenario | Hamming distance | Verdict |
|---|---|---|
| Recipe reposted on a partner site — same steps, different byline and ad slot | **2 bits** | Near-duplicate → discard or merge |
| An original post that quotes one paragraph from another recipe | **24+ bits** | Not a duplicate → keep it |

```mermaid
flowchart TD
    A["Fetched content"] --> Ex{"Exact checksum match?<br/>(cheap, runs first)"}
    Ex -->|match| Discard1(["Exact duplicate — discard"])
    Ex -->|no match| Sim["Compute SimHash fingerprint<br/>(64-bit)"]
    Sim --> Cmp{"Hamming distance to<br/>nearest known fingerprint"}
    Cmp -->|"≤ 3 bits"| Discard2(["Near-duplicate — discard or merge"])
    Cmp -->|"> 3 bits"| Keep(["Genuinely new — store"])
```

The order matters: the exact checksum runs first because it's cheap and catches most repeats. SimHash runs second, only on what survives, because comparing fuzzy fingerprints costs more than a plain equality check.

### Dedup is solid now. Politeness isn't.

Dedup only decides *whether* to keep content. It says nothing about *how politely* Scout is allowed to go get it.

One specific food-blog network, running on cheap shared hosting, has an average time-to-first-byte of **3 seconds**. Scout's 12 workers, with no per-host throttle, hit it at roughly **100 requests/sec** combined.

Within **40 minutes**, that host's admin blocks ClearPath's entire IP range `[illustrative — a believable outcome, not a documented incident]`.

### How I'd say this in an interview

> "Exact checksums and SimHash are two tiers of the same idea, run in sequence — cheap exact match first, fuzzy fingerprint match second for the remainder. That closes dedup. It doesn't touch the completely separate problem of *how hard* you're allowed to hit any one host, and that's the very next wall."

---

## Chapter 5 — The mail carrier who wouldn't stop knocking

### The fix: two layers of politeness

ClearPath needs both of these, and they are different things:

1. **A published rulebook — `robots.txt`.** Every host can set this to say "don't touch this path." This isn't a ClearPath invention — it's a web-wide convention Martijn Koster proposed informally in 1994, obeyed voluntarily by crawlers for decades, and finally formalized as an official IETF standard, **RFC 9309**, in 2022.
2. **An adaptive throttle.** Scout enforces this on itself regardless of what the rulebook says, because `robots.txt` can permit a path and still not want it hit 100 times a second.

### The analogy

Think of Scout as a **delivery driver visiting a street full of shops**.

- `robots.txt` is the sign taped to each shop's door — "deliveries welcome," "staff entrance, keep out," or a posted delivery window (`Crawl-delay`).
- The throttle is the driver's own rule of thumb: one drop-off every couple of seconds at any single shop, slower still if that shop's staff visibly can't keep up — regardless of what the sign says. A sign saying "welcome" doesn't mean "welcome to be flooded."

```mermaid
sequenceDiagram
    participant W as Worker
    participant RC as robots.txt Cache
    participant TB as Per-host Token Bucket
    participant Host as Target Host

    W->>RC: robots.txt for this host? (TTL cached)
    RC-->>W: allowed? crawl-delay?
    alt path disallowed
        W->>W: skip, mark no-go
    else path allowed
        W->>TB: request a token for this host
        alt token available
            TB-->>W: proceed
            W->>Host: GET page (measure TTFB)
            W->>TB: slower TTFB -> fewer tokens/sec going forward
        else no token yet
            TB-->>W: wait / requeue for later
        end
    end
```

### The result

ClearPath sets one request per host every 2 seconds by default, adapting slower for hosts with high TTFB — in the same spirit as Google's own historically-reported practice of roughly one request per second per host.

The IP ban from Chapter 4 stops recurring.

### The new problem

Robots.txt and the throttle only govern *how* Scout is allowed to visit a page. They say nothing about *whether that page is a trap*.

Chapter 1's infinite calendar page was never disallowed by anyone's robots.txt. It's a perfectly legal, perfectly polite, perfectly infinite hallway. Politeness alone was never going to save Scout from it.

### How I'd say this in an interview

> "Politeness is two layers — the published rulebook, robots.txt, and a self-imposed adaptive throttle on top of it, because being *allowed* to fetch something fast doesn't mean you *should*. But robots.txt is a floor, not a shield — it stops disallowed pages, it does nothing against an allowed page that happens to be infinite, and that's a genuinely different problem."

---

## Chapter 6 — The hallway that keeps growing new doors

### The pattern behind the trap

The trap from Chapter 1 wasn't a one-off. Once ClearPath starts looking, it finds the same shape everywhere, in five recognizable flavors.

Here's a mnemonic worth reciting unprompted in an interview: **Q-I-C-D-C**.

| Letter | Trap shape | Example |
|---|---|---|
| **Q** | Query-param traps | `?id=1,2,3...` |
| **I** | Internal redirect loops | `/a → /b → /a` |
| **C** | Calendar pages | "next day" links, forever |
| **D** | Dynamic-content generators | `?seed=N` for any N |
| **C** | Cyclic directories | `/first/second/first/second/...` |

### The most damaging one isn't malicious at all

**Faceted navigation** is the single biggest offender.

One recipe-aggregator partner site lets visitors filter by cuisine × diet × cook-time × ingredient, all as combinable query parameters. This is a real, widely-observed pattern in e-commerce and content sites alike.

One category page can generate an effectively unbounded number of valid-looking, mostly-duplicate URLs — all differing only by which filters are toggled.

```mermaid
flowchart LR
    subgraph Shapes["The five classic trap shapes (Q-I-C-D-C)"]
        direction TB
        Q["Query params"]
        I["Internal loops"]
        C["Calendar pages"]
        D["Dynamic generators"]
        Cy["Cyclic directories"]
    end
    Shapes --> Detect["Detect:<br/>page-count-per-domain threshold breached,<br/>or near-dup ratio (SimHash, Ch.4) rising"]
    Detect --> Mitigate["Mitigate:<br/>per-domain page/time cap<br/>+ mark URL pattern as no-go"]
```

### The fix

Two parts, working together:

1. **A per-domain cap** — say, 50,000 pages per domain per day, tuned per host.
2. **Watch two statistical signals per domain**: page-count growth rate, and the near-duplicate ratio Chapter 4's SimHash already computes.

When either signal crosses a threshold, Scout stops crawling that domain further and flags it for review. This is the same circuit-breaker shape used everywhere else in distributed systems.

### The new problem

All of this — dedup, robots.txt, throttling, trap caps — lives behind **one shared queue and one database** tracking every known URL.

As ClearPath scales from 12 workers to 200 to keep pace with a growing seed list, that one database becomes the bottleneck.

- Benchmarked, a single database box sustains roughly **2,500 writes/sec** before falling behind `[illustrative — a stand-in "one box has a ceiling" number]`.
- Two hundred workers, each discovering new URLs constantly, need combined throughput closer to **10,000 writes/sec**.

The queue that was supposed to coordinate everyone is now the thing everyone's waiting on.

### How I'd say this in an interview

> "Traps fall into a handful of recognizable shapes, and the fix is the same for all of them — a per-domain resource cap plus statistical detection, not hand-written rules for every pattern you've personally seen. But none of that touches the frontier's own scaling limit — one queue and one database is still one machine's ceiling, same disease as Chapter 1's single thread, just one layer up."

---

## Chapter 7 — The one sorting desk that everyone lines up at

### The fix

Stop routing every worker through one shared queue and database. Split the frontier itself into **N independent shards**, and route each hostname to a shard by hashing it.

This is the exact same partitioning key Chapter 6 already uses for per-domain caps — now doing double duty for placement, too.

ClearPath picks **20 shards**, each backed by its own queue and database. Each shard handles roughly:

```
10,000 / 20 = 500 writes/sec
```

That's comfortably under any one box's ceiling.

### The analogy

Think of the frontier as a **postal system with sorting-office branches**, one branch per shard, arranged around a circular route.

- Every hostname gets mapped to a spot on that circle by hashing its name.
- The branch whose spot comes next clockwise owns it.

This is **consistent hashing** — the real, documented mechanism behind Akamai's original content-routing (Karger et al., 1997) and later Amazon's Dynamo (2007). Here it's applied to *which frontier shard owns a hostname*, instead of *which cache node owns a key*.

```mermaid
flowchart LR
    H["consistent_hash(hostname)"] --> Sh1["Shard 1<br/>+ its workers"]
    H --> Sh2["Shard 2<br/>+ its workers"]
    H --> ShN["Shard 20<br/>+ its workers"]
```

### Why this scales well

Add a 21st shard later, and only the small slice of hostnames that now fall nearest it move. It's not a full reshuffle of everything — the same property that made this trick worth adopting for cache sharding in the first place.

Contention on any single database disappears. Each shard is its own small, healthy queue.

### The new problem: not all hostnames are equal

Hashing hostnames to shards assumes hostnames are roughly even in size. They aren't.

One recipe-aggregator mega-site has 5 million pages on its own. It hashes to a single shard — and that shard alone now handles more traffic than three ordinary shards combined.

Domain-level partitioning made politeness simple (Chapter 5's per-host throttle needs exactly one owner per host). But it just created a hot spot.

### How I'd say this in an interview

> "Sharding the frontier by hostname — consistent-hashed, same key as domain-level worker assignment — turns one overloaded database into twenty healthy ones, and adding a shard later only moves a small slice of hostnames instead of reshuffling everything. But hashing assumes even load per hostname, and the web has mega-sites that break that assumption immediately."

---

## Chapter 8 — The warehouse that got too big for one team

### The fix isn't to abandon domain-level partitioning

Domain-level partitioning is still the right **default**. It's what keeps politeness simple: one worker (or worker group) owning a whole domain means the per-host throttle from Chapter 5 has exactly one place to live.

The fix for the mega-site problem is to **sub-shard just the mega-domains**:

- Split that one 5-million-page aggregator across, say, **8 workers**, by URL-path range within the domain.
- Every ordinary domain stays on one worker, as before.

```mermaid
flowchart TD
    Start{"Do most domains fit on<br/>one worker's bandwidth?"}
    Start -->|"yes, almost all"| Domain["Domain-level partitioning<br/>(default — simplest politeness story)"]
    Start -->|"no, a few mega-domains"| Sub["Domain-level partitioning<br/>+ sub-shard ONLY the mega-domains"]
    Domain --> Even{"Is load still uneven<br/>across workers?"}
    Even -->|"yes, for a specific slice"| Dynamic["Fall back to per-URL dynamic<br/>assignment for that imbalanced slice"]
    Even -->|"no"| Done(["Ship it"])
```

### The last resort: dynamic per-URL assignment

For the rare cases where even sub-sharding leaves load lumpy — say, a burst of URLs that don't map cleanly to any domain boundary — ClearPath falls back to **dynamic per-URL assignment**:

- Any free worker takes any queued URL from a shared pool.
- Cost: it loses the tidy "one worker owns this whole domain" locality that made politeness simple.

It's used sparingly, only where the other two strategies visibly fail.

### The new problem: DNS at scale

ClearPath now runs roughly 300 workers across 20 shards, several of them sub-sharded.

Every one of those workers resolves DNS before every fetch. Most of the web's traffic clusters on a relatively small set of popular hostnames. 300 workers, independently asking a public DNS resolver for the same handful of names over and over, starts looking — from the resolver's point of view — indistinguishable from an attack.

ClearPath's outbound DNS queries get rate-limited by their upstream provider. Cold lookups that used to take 20-200ms start queueing behind the throttle.

### How I'd say this in an interview

> "Partitioning strategy — who owns this URL — and priority — what order do they get crawled in — are two separate axes candidates often blur together. Domain-level is the right default because it keeps politeness simple; sub-shard only the mega-domains that actually break the assumption, and reach for fully dynamic assignment only as a last resort, because it gives up the locality that made per-host rate-limiting easy in the first place."

---

## Chapter 9 — The phone book you keep re-dialing

### The problem, in numbers

- A cold DNS lookup costs **20-200ms**.
- A cached one costs **under 1ms**.
- That's a **100-1000x difference**.

A public or ISP resolver has no way to tell "300 well-behaved crawler workers" from "a botnet hammering the same names." It just sees volume, and it throttles accordingly. That's exactly the wall Chapter 8 ended on.

### The fix

**Run your own resolver cache.** This is the same move any large-scale crawl operation makes once DNS volume gets real.

### The analogy — reusing DNS's own name for itself

It's like keeping your own well-worn phone book on your desk, instead of calling directory assistance for a number you looked up ten minutes ago. You still call directory assistance once — but never twice for the same name within its listed validity window.

```mermaid
flowchart TD
    Req["Worker needs IP for hostname"] --> Hit{"In our own cache,<br/>within TTL?"}
    Hit -->|yes| Fast["Return cached IP<br/>(under 1ms)"]
    Hit -->|no| Slow["Cold lookup to authoritative DNS<br/>(20-200ms)"]
    Slow --> Result{"Result?"}
    Result -->|"IP found"| Store["Cache with its real TTL"]
    Result -->|"NXDOMAIN"| Neg["Cache the NEGATIVE result too,<br/>with a short TTL"]
    Store --> Fast
```

### Two disciplines that matter as much as the cache itself

1. **Respect the published TTL exactly.** Caching past it risks serving a stale IP after a host migrates. Caching under it throws away the speedup.
2. **Cache negative results too.** A hostname that just returned `NXDOMAIN` (a dead link, often produced by the very trap patterns from Chapter 6) will get asked for again by the next worker that meets the same broken URL pattern. A short negative-cache TTL stops that from hammering authoritative nameservers for a name that was never going to resolve.

### Worked number, once the cache is warm

At a **95% cache-hit rate**, amortized DNS latency per fetch is:

```
0.95 × 0.5ms + 0.05 × 100ms ≈ 5.5ms
```

That's small next to a typical 50-100ms HTTP fetch — which is exactly why a *healthy* cache makes DNS a footnote.

It becomes a real bottleneck again the moment the hit rate drops: an undersized cache, too-aggressive TTL policy, or a fresh batch of never-seen domains from a new seed list can all do it.

### The new problem

DNS, dedup, traps, and politeness are all solid now. But every one of those fixes assumed a URL is worth visiting *once*.

Pages change at wildly different rates:

- A news-style recipe roundup updates daily.
- A static "About us" page never changes again.

Right now, ClearPath recrawls everything on the same fixed monthly schedule. That wastes a full re-fetch on pages that never changed, while pages that change daily go stale for weeks.

### How I'd say this in an interview

> "DNS at crawl scale isn't plumbing you can wave away — a public resolver rate-limits high-volume clients like an attacker, so you run your own cache, respect the TTL exactly, and cache negative results too so trap-generated dead links don't hammer authoritative servers. Once the cache is warm it's a rounding error next to the HTTP fetch itself; it only bites again if the hit rate drops."

---

## Chapter 10 — The book that never changes vs. the one that changes daily

### Two independent knobs

Every URL in ClearPath's store has two independent knobs:

- **Priority** — how important is this page?
- **Recrawl frequency** — how often should we check it again?

A daily-updated recipe roundup and a static "About us" page are wildly different on both axes.

```mermaid
quadrantChart
    title Where different pages land
    x-axis Low priority --> High priority
    y-axis Low recrawl frequency --> High recrawl frequency
    quadrant-1 Crawl often, high value
    quadrant-2 Rarely worth it
    quadrant-3 Crawl rarely, low value
    quadrant-4 Stable but important
    Daily recipe roundup: [0.85, 0.9]
    Trending seasonal recipe: [0.8, 0.85]
    Static About-us page: [0.15, 0.05]
    Classic reference recipe: [0.55, 0.05]
    Abandoned old blog: [0.1, 0.05]
```

### The fix has two independent levers

**Lever 1 — Make the recrawl interval adaptive, not fixed.**
Track how often a URL's *content checksum* (Chapter 3) actually changes across visits.

- A page that's changed on every one of its last 10 checks gets a shorter interval.
- A page that hasn't changed in 40 checks gets stretched out.

**Lever 2 — Make each individual recheck cheap using conditional GET.**
Send `If-Modified-Since` or `If-None-Match` (using a stored `ETag`) instead of blindly re-downloading.

- A `304 Not Modified` response costs one round-trip and zero bandwidth.
- Only a real `200 OK` triggers a full re-fetch, re-extract, and re-dedup.

```mermaid
flowchart TD
    T["Recrawl timer fires"] --> C{"Have a stored ETag /<br/>Last-Modified value?"}
    C -->|no| Full["Full GET<br/>(first crawl of this URL)"]
    C -->|yes| Cond["Send conditional GET"]
    Cond --> R{"Server response?"}
    R -->|"304 Not Modified"| Skip["Skip re-fetch.<br/>Stretch the recrawl interval."]
    R -->|"200 OK"| Changed["Re-fetch, re-extract, re-dedup.<br/>Shrink the recrawl interval."]
```

Together, this pairing is what makes running the adaptive loop *continuously* affordable, instead of only on a fixed monthly sweep. The interval decides *whether* to check soon. Conditional GET decides *how expensive* checking actually is.

### The new problem

Now that recrawls are frequent and adaptive, a worker is holding many more URLs "in flight" at once than before.

One afternoon, a worker process crashes mid-fetch. No error is logged anywhere — it just stops. The URL it was holding was never marked done, never marked failed.

As far as the frontier knows, that URL is simply gone — permanently stuck in a "someone's working on it" state that no one is actually working on anymore.

### How I'd say this in an interview

> "Freshness has two separate knobs — how often you check, and how expensive each check is — and you want both adaptive: stretch the interval on pages that never change, and use conditional GET so a due-for-check page that *hasn't* changed costs one round-trip, not a full re-download. That's what makes running this continuously cheap enough to actually do."

---

## Chapter 11 — The claim ticket that expires

### The fix

The frontier never hands a URL to a worker as a permanent claim. It hands out a **lease** — a time-boxed reservation with a TTL.

### The analogy

It's a **claim ticket at a dry cleaner**. You hand over your shirt, get a ticket with a pickup window. If you never come back within that window, the shirt goes back on the rack for someone else to claim. The shop doesn't wait for you forever, and it doesn't lose the shirt either.

```mermaid
stateDiagram-v2
    [*] --> Queued: passes URL dedup + robots.txt check
    Queued --> Fetching: worker leases it (lease + TTL granted)
    Fetching --> Extracted: fetch succeeds, CompleteURL(success)
    Fetching --> Queued: lease TTL expires, no CompleteURL call
    Extracted --> Stored: passes dedup, written to blob store
    Extracted --> Discarded: content is a duplicate
    Stored --> Queued: recrawl timer fires later
```

### The small internal API

This is a small internal API, not a public one — nobody outside ClearPath calls this crawler. So the "API" here is really the contract between the frontier and the worker pool:

| Call | What it does |
|---|---|
| `LeaseNextURL(worker_id)` | Hands out the highest-priority available URL, plus a lease. |
| `CompleteURL(lease_id, outcome, new_links[])` | Reports success or failure. On success, atomically enqueues every newly-discovered link through the same URL-dedup path from Chapter 3. |
| `RenewLease(lease_id)` | Lets a slow-but-alive fetch extend its window, instead of getting reclaimed mid-flight. |

```mermaid
sequenceDiagram
    participant W as Worker
    participant F as Frontier
    W->>F: LeaseNextURL(worker_id)
    F-->>W: (url, lease_id, ttl)
    Note over W: fetch, extract, dedup —<br/>full pipeline
    alt worker crashes mid-fetch
        Note over F: lease TTL expires, no CompleteURL —<br/>URL silently requeued
    else worker finishes normally
        W->>F: CompleteURL(lease_id, "success", outlinks[])
    end
```

A crashed worker no longer strands a URL forever — the lease just expires, and the next free worker picks it up.

A bounded retry count (say, 5 attempts) stops a permanently-broken URL from being retried into infinity — the same discipline Chapter 6 already applies to trap domains.

### One last piece — not a technical fix, but part of the checklist

Politeness on paper (`robots.txt`) was never the whole compliance story. ClearPath's checklist by this point:

- Send a descriptive `User-Agent` with a contact URL, so a site owner who wants to complain can reach a human instead of just banning an IP range.
- Honor page-level `<meta name="robots" content="noindex">` or `X-Robots-Tag` — a *per-page* signal that lives in the response itself, separate from the host-level `robots.txt`.
- Treat an unreachable `robots.txt` (timeouts, 5xx) as **disallow-all until it resolves**, never as allow-all. The cost of skipping a host briefly is far lower than the cost of an unpoliced crawl.

### How I'd say this in an interview

> "A lease is a lock with a timeout, not a permanent claim — the frontier hands one out when a worker dequeues a URL, and a crash just means the lease expires and someone else picks it up, the same way a claim ticket protects a dry cleaner from a customer who never comes back. And robots.txt compliance is table stakes, not the whole story — identifying your bot, honoring page-level noindex tags, and defaulting to disallow when robots.txt itself is unreachable are the rest of it."

---

## Where the story actually lands

```mermaid
flowchart TD
    A["Ch1: DFS, single thread<br/>(trap + too slow)"] -->|"fixes: order + parallelism<br/>breaks: duplicate work"| B["Ch2: BFS + workers"]
    B -->|"fixes: no repeats<br/>breaks: checksum store slow, near-dups"| C["Ch3-4: checksum dedup +<br/>Bloom filter + SimHash"]
    C -->|"fixes: dedup<br/>breaks: one host gets hammered"| D["Ch5: robots.txt + token bucket"]
    D -->|"fixes: host abuse<br/>breaks: legal-but-infinite pages"| E["Ch6: trap classification + caps"]
    E -->|"fixes: bounded traps<br/>breaks: one frontier DB is a ceiling"| F["Ch7: sharded frontier<br/>(consistent hash)"]
    F -->|"fixes: contention<br/>breaks: mega-domain hot spot"| G["Ch8: sub-shard + dynamic fallback"]
    G -->|"fixes: load balance<br/>breaks: DNS volume throttled"| H["Ch9: self-hosted DNS cache"]
    H -->|"fixes: DNS cost<br/>breaks: fixed recrawl wastes/staleness"| I["Ch10: adaptive recrawl +<br/>conditional GET"]
    I -->|"fixes: freshness cheaply<br/>breaks: crashed worker strands URL"| J["Ch11: lease + CompleteURL"]
```

```mermaid
erDiagram
    URL {
        string url_id PK
        int priority
        int recrawl_frequency_hours
        string status "queued|fetching|stored|discarded"
    }
    URL_CHECKSUM {
        string checksum PK
    }
    CONTENT_CHECKSUM {
        string checksum PK
    }
    CONTENT {
        string blob_ref PK
        string etag
        datetime last_modified
    }
    ROBOTS_CACHE {
        string hostname PK
        text disallow_rules
        int crawl_delay_sec
    }
    URL ||--o{ URL_CHECKSUM : "hashes to"
    URL ||--o| CONTENT : "resolves to"
    CONTENT ||--|| CONTENT_CHECKSUM : "hashes to"
    URL }o--|| ROBOTS_CACHE : "governed by"
```

```mermaid
mindmap
  root((Why a crawler needs<br/>all of this))
    Traversal order
      DFS concentrates damage in one trap
      BFS spreads it across every domain
    Dedup
      URL checksum: same address again
      Content checksum: different address, same bytes
      SimHash: near-identical bytes, different address
      Bloom filter: cheap "definitely not seen" pre-check
    Politeness
      robots.txt: the published rulebook
      Token bucket: the self-imposed throttle on top
    Traps
      Five shapes: Q-I-C-D-C
      Fix: per-domain caps + statistical detection
    Scaling the frontier
      One DB is a ceiling
      Consistent hashing shards it by hostname
      Mega-domains still need sub-sharding
    DNS
      Public resolver throttles high volume
      Self-hosted cache, respect TTL, cache negatives too
    Freshness
      Adaptive recrawl interval
      Conditional GET keeps each check cheap
    Fault tolerance
      Lease = lock with a timeout
      Crash just means requeue, not lost forever
```

### The skill isn't reciting all eleven chapters

Every real production crawler sits somewhere on this chain. The skill is knowing **where the stated requirements say to stop**.

- A one-domain scraper might reasonably stop around Chapter 5.
- A web-scale search crawler has to reach Chapter 7 through 11.

If nobody's mentioned billions of pages or multiple workers, walking all the way to consistent hashing unprompted reads as padding, not depth.

---

## Grill me — adversarial follow-ups

**Q1: "Why not just add more threads to the single-threaded script instead of building a whole queue-based system?"**

More threads only buy headroom up to whatever the frontier's coordination logic can handle. A single in-process script has no shared, durable state at all, so any two threads can still both grab the same URL. The queue isn't an optimization on top of the single-thread script — it's a completely different coordination model that a thread pool inside one process can't fake.

**Q2: "Doesn't sharding the frontier by hostname just move Chapter 1's single point of failure down to 20 smaller ones?"**

Yes, and that's a fair callout. Each shard is now its own smaller point of contention, rather than one giant one — which is strictly better, but not "solved." The actual fix for any one shard's database dying is the same durability story any sharded system needs: replication per shard. This design doesn't dwell on that, because the reference guide's own trade-off table treats checksum-store backup as a periodic-checkpoint problem, not full replication.

**Q3: "How do you detect a crawler trap without hand-written URL-pattern rules for every case you've personally seen?"**

Track statistical signals per domain instead of patterns: page-count growth rate, near-duplicate content ratio from SimHash, and URL length or parameter-count creeping upward across successive crawls. Trip a circuit breaker — stop crawling that domain further — when any signal crosses a threshold. It's the same shape as any circuit breaker anywhere else in distributed systems, and it generalizes to trap shapes nobody's seen yet.

**Q4: "Two workers could lease the same URL at the same time under a race — how do you avoid double-processing?"**

The lease mechanism makes double-leasing rare, not impossible. The real backstop is that content-checksum dedup already makes duplicate work merely wasteful rather than incorrect — a second worker fetching the same URL just gets discarded at the dedup step. Leases are an efficiency fix, not a correctness fix. Correctness comes from idempotent, dedup-checked writes.

**Q5: "Why bother with a Bloom filter if you still need the real checksum store behind it?"**

Because the Bloom filter's whole job is answering "definitely not seen" instantly, for the vast majority of lookups, without ever touching disk. It only defers to the real store on the rarer "maybe seen" case. It doesn't replace the checksum store; it protects it from being hit on every single fetch.

**Q6: "If robots.txt already lets a site set a Crawl-delay, why does the crawler need its own separate throttle on top?"**

Because a published Crawl-delay is a floor set by the site owner, and it says nothing about how that specific host is behaving *right now*. A host having a bad day can get overwhelmed even by traffic that respects its published delay. The self-imposed, TTFB-adaptive throttle can only ever be *more* conservative than the published delay, never less — precisely because the site's own number might not reflect today's reality.

**Q7: "Why treat an unreachable robots.txt as disallow-all instead of just skipping the check and crawling anyway?"**

Because the failure mode of guessing wrong runs in only one dangerous direction. Crawling a host that actually wanted to disallow you is a real cost — bans, complaints, occasionally legal exposure. Skipping a host briefly until its robots.txt comes back is nearly free. When the two failure directions aren't symmetric, you default toward the cheap mistake, not the expensive one.

**Q8: "SimHash has a threshold, not an equality check — how do you pick where to draw the line?"**

You tune it against labeled examples of things you do and don't want treated as duplicates. The guide's own worked numbers use distance ≤3 out of 64 bits as a starting point — catching byline/ad-banner-only reposts while letting genuinely original articles that merely quote a paragraph through. It's a threshold you validate empirically per corpus, not a universal constant.

**Q9: "Given this whole story, if someone just says 'design a web crawler' cold, where do you actually start?"**

Ask the two questions that decide almost everything downstream:

1. Is this one domain, or web-scale?
2. Is the deliverable just crawl-and-store, or does it drift into indexing and ranking?

Scope those out loud first. Then walk forward only as far as the stated scale demands. BFS and basic dedup are close to a given the moment it's more than a handful of pages. But sharded frontiers, SimHash, and adaptive recrawl are things you earn by naming a specific scale or freshness requirement — not defaults you bolt on for their own sake.

---

## Cheat sheet — one line per stop on the story

| Stop | One-line summary |
|---|---|
| **Naive DFS crawler** | Depth-first + single-threaded means one infinite page (a calendar, a redirect loop) can swallow the whole crawler, and it's mathematically too slow for web scale regardless. |
| **BFS + worker pool** | Canvass every neighbor before going deeper into any one — spreads a trap's damage instead of concentrating it, and divides single-worker time by however many workers you run. |
| **URL + content checksum dedup** | Two different repeats need two different checksums — same address again, versus different address with byte-identical content. |
| **Bloom filter** | A cheap probabilistic "definitely not seen" pre-check in front of the real checksum store, so most lookups never touch disk. |
| **SimHash** | Catches near-duplicates exact checksums are blind to, by comparing Hamming distance between compact fingerprints instead of demanding bit-for-bit equality. |
| **robots.txt + token bucket** | The published rulebook plus a self-imposed adaptive throttle on top — obeying the sign on the door doesn't mean you stop watching how the shop is actually coping. |
| **Crawler trap defense** | Five recognizable shapes (Q-I-C-D-C), detected by statistical signals (growth rate, near-dup ratio) rather than hand-written patterns, capped per domain. |
| **Sharded frontier (consistent hashing)** | One queue+DB is a ceiling; shard by hostname, same key as domain-level worker assignment, so adding a shard only moves a small slice later. |
| **Sub-sharding mega-domains** | Domain-level partitioning stays the default for politeness's sake; only the rare huge domain gets split further, and dynamic per-URL assignment is the last resort. |
| **Self-hosted DNS cache** | A public resolver throttles high-volume clients like an attacker; run your own cache, respect the TTL exactly, and cache negative (`NXDOMAIN`) results too. |
| **Adaptive recrawl + conditional GET** | Stretch the interval on pages that never change, shrink it on pages that always do, and use `If-Modified-Since`/`ETag` so a due check costs one round-trip when nothing changed. |
| **Lease-based frontier** | A lease is a lock with a timeout, not a permanent claim — a crashed worker just means the URL's lease expires and someone else picks it up. |
| **Legal/ethical floor** | Identify your bot in the `User-Agent`, honor page-level `noindex`/`X-Robots-Tag` on top of host-level robots.txt, and default to disallow-all when robots.txt itself is unreachable. |
| **The meta-lesson** | Every fix in this story buys one property (order, parallelism, correctness, politeness, boundedness, horizontal scale, load balance, cheap DNS, freshness, or fault tolerance) by spending something else — say the trade in the same sentence you propose the fix. |
