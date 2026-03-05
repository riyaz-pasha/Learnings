# 🕷️ The Story of Building a Distributed Web Crawler

Let's start at the very beginning. No jargon yet. Just a real problem.

---

## Chapter 1: The Problem — "I need to read the entire internet"

Imagine it's the early days of the internet. You've just built a search engine startup. Your pitch is simple:

> *"Type anything, find any webpage."*

But there's a massive problem. **You don't know what's on the internet.**

The internet is just millions of computers, each hosting webpages, and none of them are telling you what they contain. There's no central registry. No master list. You have to go **discover** them yourself.

So you think — *"What if I just... visit every webpage?"*

That's a **web crawler**. A program that visits webpages, reads their content, and follows the links on those pages to find more pages.

---

## 🧍 The Naive Approach — One Guy, One Computer

You sit down and write your first crawler. It's beautifully simple:

```
1. Start with a list of known URLs (called "seeds") — e.g., google.com, wikipedia.org
2. Visit the first URL
3. Download the HTML
4. Extract all links from that HTML
5. Add those links to your list
6. Go to step 2
```

In code terms, you have:

```
Queue: [wikipedia.org]

Visit wikipedia.org
  → Download HTML
  → Find links: [en.wikipedia.org/wiki/Python, en.wikipedia.org/wiki/Java ...]
  → Add to Queue

Queue: [en.wikipedia.org/wiki/Python, en.wikipedia.org/wiki/Java ...]

Visit Python page...
  → Find more links...
```

This works! On your laptop, you can crawl maybe **10-50 pages per second.**

---

## 🚨 The First Problem Hits — Scale

You're feeling great. Then you Google it.

> The internet has **~5 billion** indexed pages. New pages are added every second. Pages change. Pages die.

At 50 pages/second, to crawl 5 billion pages:

```
5,000,000,000 ÷ 50 = 100,000,000 seconds
                    = ~3.17 YEARS
```

And by the time you finish, the pages you visited on Day 1 are already **outdated.**

You need to crawl faster. Much faster. And you need to keep doing it — forever.

This is where the **distributed** part begins.

---

## 💡 The Insight — "What if many people did this together?"

Instead of one crawler, what if you had **hundreds of machines**, each crawling simultaneously?

But the moment you try this, three nasty questions appear:

1. **Who decides which machine crawls which URL?** (Coordination Problem)
2. **How do you make sure two machines don't crawl the same URL?** (Duplication Problem)
3. **How do you store all this data?** (Storage Problem)

These three questions are the heart of distributed web crawler design. Every fancy concept you'll hear in interviews — consistent hashing, distributed queues, deduplication — is just an answer to one of these three questions.

---

## 📐 The High-Level Blueprint (Before We Dive In)

Before going deep, here's the bird's eye view of what we'll build together:

```
┌─────────────────────────────────────────────────────┐
│                    URL Frontier                      │  ← "What to crawl next?"
│              (Distributed Priority Queue)            │
└─────────────────┬───────────────────────────────────┘
                  │
         ┌────────▼────────┐
         │  Fetcher Fleet  │  ← "Go get the page"
         │ (100s of nodes) │
         └────────┬────────┘
                  │
    ┌─────────────▼──────────────┐
    │        Parser / Extractor  │  ← "What's on the page? What links?"
    └──────┬──────────┬──────────┘
           │          │
    ┌──────▼──┐  ┌────▼──────────┐
    │New URLs │  │  Content Store │  ← "Save the page"
    │(back to │  │  (for indexing)│
    │frontier)│  └───────────────┘
    └─────────┘
         +
    ┌────▼──────────┐
    │ URL Seen Store │  ← "Have we visited this before?"
    └───────────────┘
```

---

## ✅ Where We Are

You now understand:
- **Why** a web crawler exists (search engines need to discover pages)
- **Why** a single machine fails (scale — billions of pages, always changing)
- **The core distributed problem** — coordination, deduplication, storage
- **The high-level components** we'll need to build

---

## 🎯 What's Next?

The very first thing we need to solve is: **"Where do we store the list of URLs to crawl, and how do we manage it across machines?"**

This is the **URL Frontier** — and it's way more interesting than it sounds. It involves priority queues, politeness policies, and your first taste of distributed systems thinking.

Ready to go into **Chapter 2: The URL Frontier**? Just say the word! 🚀

---

# 📬 Chapter 2: The URL Frontier — "The To-Do List That Runs the Internet"

---

## The Story Begins With a Simple Queue

So you've decided to use multiple machines. Each machine will be a **fetcher** — its only job is to download webpages.

You think: *"Easy. I'll just put all the URLs in a list, and each machine grabs the next one."*

So you create a shared queue:

```
Queue: [wikipedia.org, reddit.com, nytimes.com, bbc.com, ...]

Machine 1 → grabs wikipedia.org
Machine 2 → grabs reddit.com  
Machine 3 → grabs nytimes.com
...
```

This seems fine. Until reality punches you in the face — **three times.**

---

## 👊 Punch #1 — You're Being a Bad Internet Citizen

Your crawler is hammering `wikipedia.org`. Machine 1 grabs the homepage, finds 500 links, adds them all back to the queue. Now 500 Wikipedia URLs are in the queue. Your 100 machines collectively send **hundreds of requests per second** to Wikipedia's servers.

Wikipedia's servers start struggling. Their engineers see the traffic spike. They look at the logs.

> *"Who is this maniac?"*

They **ban your IP**. And they're right to do it.

This is called **crawl politeness**. Every real website has a file called `robots.txt` — a polite agreement between websites and crawlers:

```
# wikipedia.org/robots.txt

User-agent: *
Crawl-delay: 1        ← Wait 1 second between requests
Disallow: /wiki/Talk  ← Don't crawl these paths
Disallow: /wiki/User  
```

So your crawler **must** respect this. You cannot hit the same domain more than once every few seconds.

But here's the problem — your simple queue doesn't know anything about domains. It just pops URLs one by one. You might pop 10 Wikipedia URLs back to back.

**You need a smarter queue.**

---

## 👊 Punch #2 — Not All URLs Are Equal

You're crawling and you discover two new URLs:

```
URL A: cnn.com/breaking-news/president-announces-something
URL B: someobscureblog.com/my-cat-monday-2019
```

Which one should you crawl first?

Obviously CNN's breaking news — it's **fresh, important, highly linked-to.** The cat blog from 2019 can wait.

Your simple queue treats both equally. It has no concept of **priority.**

Real crawlers need to answer: *"Of all the URLs waiting, which one matters most?"*

Priority is determined by:
- **PageRank / importance** — how many other pages link to this URL?
- **Freshness** — when was this page last updated?
- **Domain authority** — is this a trusted, high-traffic site?
- **Topic relevance** — if you're building a news crawler, news sites first

**You need a priority queue, not a plain queue.**

---

## 👊 Punch #3 — One Queue Is a Single Point of Failure

Your one shared queue is sitting on one machine. That machine crashes at 3am.

Every fetcher sits idle, doing nothing, waiting for URLs that aren't coming.

**You need the queue itself to be distributed.**

---

## 🏗️ Building the Real URL Frontier

To solve all three problems, the URL Frontier has **two layers** inside it.

Think of it like a post office:

```
┌─────────────────────────────────────────────────────────┐
│                    URL FRONTIER                          │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │           FRONT QUEUES (Priority Layer)          │   │
│  │                                                  │   │
│  │   High Priority  ████████████████████ Queue 1   │   │
│  │   Mid Priority   ████████████         Queue 2   │   │
│  │   Low Priority   ████                 Queue 3   │   │
│  └──────────────────────┬───────────────────────────┘  │
│                         │                               │
│                   Prioritizer                           │
│              (Assigns URLs to queues                    │
│               based on PageRank etc)                    │
│                         │                               │
│  ┌──────────────────────▼───────────────────────────┐  │
│  │           BACK QUEUES (Politeness Layer)         │  │
│  │                                                  │  │
│  │   wikipedia.org  → Queue A  (one per domain)    │  │
│  │   reddit.com     → Queue B                      │  │
│  │   nytimes.com    → Queue C                      │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

Let's walk through both layers.

---

## Layer 1: Front Queues — The Priority System

When a new URL arrives at the frontier, a component called the **Prioritizer** scores it.

```
Incoming URL: cnn.com/breaking-news
  → PageRank score: 9.2/10
  → Last crawled: 2 hours ago
  → Priority Score: HIGH
  → Goes into Queue 1 (High Priority)

Incoming URL: someobscureblog.com/cat-monday
  → PageRank score: 1.1/10  
  → Last crawled: never
  → Priority Score: LOW
  → Goes into Queue 3 (Low Priority)
```

Now when a fetcher asks *"give me a URL"*, the system picks from Queue 1 most often, Queue 2 sometimes, Queue 3 rarely.

In practice this is done with **weighted random selection:**

```
Roll a random number 1-10:
  1-6  → Pick from Queue 1 (High Priority)   [60% of the time]
  7-9  → Pick from Queue 2 (Medium Priority) [30% of the time]
  10   → Pick from Queue 3 (Low Priority)    [10% of the time]
```

This ensures low-priority URLs eventually get crawled — just much later.

---

## Layer 2: Back Queues — The Politeness System

This is the clever part. Each **back queue is dedicated to exactly one domain.**

```
Back Queue A: [wikipedia.org/Python, wikipedia.org/Java, wikipedia.org/C++]
Back Queue B: [reddit.com/r/programming, reddit.com/r/tech]
Back Queue C: [nytimes.com/politics, nytimes.com/sports]
```

And there's a **heap** (a timer-based structure) that tracks: *"when is each domain allowed to be crawled again?"*

```
Heap (sorted by "earliest next-crawl time"):
  wikipedia.org → can crawl again at 10:00:01
  reddit.com    → can crawl again at 10:00:03
  nytimes.com   → can crawl again at 10:00:00  ← smallest, pick this next
```

The fetcher always picks the domain at the top of the heap (earliest allowed time).

```
It's 10:00:00 → Fetcher picks nytimes.com
  → Grabs nytimes.com/politics from Back Queue C
  → Crawls it
  → Updates heap: nytimes.com → can crawl again at 10:00:05
  
It's 10:00:01 → Fetcher picks wikipedia.org
  → Grabs wikipedia.org/Python from Back Queue A
  → Crawls it
  → Updates heap: wikipedia.org → can crawl again at 10:00:06
```

This guarantees you **never** hit the same domain too fast. You're being a polite internet citizen. Nobody bans you.

---

## 🌐 Making It Distributed — Assigning Domains to Machines

Now you have 100 fetcher machines. How do you split the work?

Naive idea: *"Each machine manages its own set of back queues."*

But how do you decide which machine handles `wikipedia.org`? You need a rule that every machine agrees on, without having to ask anyone.

The answer is **consistent hashing** — and this is one of the most important concepts in distributed systems.

```
Take the domain name: "wikipedia.org"
Hash it: hash("wikipedia.org") = 47382

You have 100 machines (0 to 99):
47382 % 100 = 82

→ Machine 82 is responsible for all wikipedia.org URLs
```

Every machine runs this same formula. No coordination needed. Machine 1 doesn't need to ask *"hey, who handles wikipedia?"* — it just computes and knows.

```
hash("reddit.com")   % 100 = 23  → Machine 23
hash("nytimes.com")  % 100 = 67  → Machine 67
hash("wikipedia.org")% 100 = 82  → Machine 82
```

But wait — what if Machine 82 crashes? Or you add a new machine?

With plain modulo hashing, adding one machine reshuffles everything:

```
Before (100 machines): hash("wikipedia.org") % 100 = 82 → Machine 82
After  (101 machines): hash("wikipedia.org") % 101 = 9  → Machine 9  ← everything moved!
```

This is catastrophic. You'd have to reassign almost every URL.

**Consistent hashing** solves this. Imagine all machines arranged in a circle (a "hash ring"):

```
                    0
                 ●  |  ●
              ●           ●
           ●                 ●
     Machine A          Machine B
           ●                 ●
              ●           ●
                 ●     ●
                   Machine C
```

Each URL hashes to a point on the ring, and is assigned to the **nearest machine clockwise.**

When you add or remove a machine, only the URLs near that machine on the ring get reassigned — not everything. Typically only `1/N` of URLs move, instead of all of them.

```
Add Machine D between A and B:
  → Only URLs that were going to B (but are now closer to D) move
  → Everything else stays put
```

This is why consistent hashing shows up in almost every distributed systems design interview.

---

## 💾 Where Does the Frontier Actually Live?

The frontier can't just live in RAM — you might have **billions** of URLs waiting to be crawled. You need persistent storage.

Real crawlers use a combination:

```
Hot URLs (crawl soon)    → In-memory queue (Redis)     Fast access
Warm URLs (crawl later)  → Disk-based queue (Kafka)    Large capacity  
Cold URLs (crawl someday)→ Database (Cassandra/DynamoDB) Persistent
```

**Kafka** deserves a special mention here. It's a distributed log — you can think of it as an infinitely long queue that multiple machines can write to and read from simultaneously, and it never loses data.

```
Fetcher Machines (Writers) → Kafka Topic: "urls-to-crawl" → Fetcher Machines (Readers)

Kafka automatically:
  - Partitions the topic across multiple machines
  - Replicates each partition (so no data is lost if one machine dies)
  - Lets multiple consumers read at their own pace
```

---

## 📊 Summary — What the URL Frontier Solves

| Problem | Solution |
|---|---|
| Too many requests to one domain | Back queues — one per domain, with timing |
| Not all URLs are equal | Front queues — priority-based selection |
| Single point of failure | Distributed queue (Kafka) across machines |
| Which machine handles which domain? | Consistent Hashing |
| Queue too big for RAM | Tiered storage (Redis → Kafka → DB) |

---

## 🎯 Likely Interview Questions on This

- *"How do you ensure politeness in a crawler?"* → Back queues + per-domain timing heap
- *"How do you prioritize URLs?"* → Front queues + weighted random selection
- *"How do you shard the URL frontier?"* → Consistent hashing on domain name
- *"What happens when a frontier node goes down?"* → Kafka replication + consistent hashing reassignment

---

You now deeply understand **the URL Frontier** — the brain of the crawler that decides *what* gets crawled and *when.*

Next up is **Chapter 3: The Fetcher Fleet** — the actual machines that go out, download pages, handle failures, timeouts, traps, and redirects. This is where things get surprisingly tricky. 

Ready? 🚀
---
# 🚀 Chapter 3: The Fetcher Fleet — "The Soldiers Who Actually Go to War"

---

## The Story So Far

You've built a smart URL Frontier. It hands out URLs in the right order, respects domain timing, and is distributed across machines. 

Now a fetcher machine picks up a URL — say `nytimes.com/politics` — and its job sounds dead simple:

> *"Go download that page. Come back with the HTML."*

Easy, right?

**Wrong.** The internet is a warzone. Pages lie. Servers crash. Links loop forever. Servers actively try to trap your crawler. And you have 100 machines doing this simultaneously.

Let's walk through every punch the internet throws at you.

---

## 👊 Punch #1 — The Page Takes Forever to Respond

Your fetcher sends a request to `somesite.com`. The server receives it... and just hangs. Doesn't respond. Doesn't close the connection. Just... sits there.

Your fetcher is now **frozen**, waiting. It can't crawl anything else.

```
Fetcher 1: Waiting for somesite.com... (5 seconds)
Fetcher 1: Still waiting...           (30 seconds)
Fetcher 1: Still waiting...           (2 minutes)
← This fetcher is now useless
```

**The Fix: Timeouts — Three of them**

You learn that there are actually **three different timeouts** you need:

```
1. CONNECTION TIMEOUT (~5 seconds)
   "If you can't even establish a connection in 5 seconds, give up."
   
   Your machine → [SYN] → Server
   Server       → [???] → No response
   After 5s → Abort. Mark URL as failed.

2. READ TIMEOUT (~30 seconds)
   "Connection established, but if the first byte of data 
    doesn't arrive in 30 seconds, give up."
   
   Your machine → [GET /politics] → Server
   Server starts processing... takes forever
   After 30s → Abort.

3. TOTAL TIMEOUT (~2 minutes)
   "Even if data IS flowing, if the whole page takes more 
    than 2 minutes to download, give up."
   
   Catches pages that send 1 byte per second to stall you.
```

With timeouts, your fetcher is never stuck. It fails fast and moves on.

---

## 👊 Punch #2 — The Page Redirects You... and Redirects... and Redirects

Your fetcher requests `http://nytimes.com/politics`.

Server responds: *"301 Moved Permanently → https://nytimes.com/politics"*

Okay, you follow it. New server responds: *"302 Found → https://www.nytimes.com/politics"*

Okay, you follow that. New response: *"301 → https://www.nytimes.com/us/politics"*

This is normal — websites redirect a lot (HTTP → HTTPS, www → non-www, etc.). But now imagine a malicious or broken site:

```
a.com → b.com → c.com → a.com → b.com → c.com → ...
                                ↑ infinite loop
```

Your fetcher chases this forever.

**The Fix: Redirect limit + cycle detection**

```
Rule 1: Maximum 5 redirects. After that, give up and mark as error.

Rule 2: Track visited URLs in THIS request chain.
  
  Visit a.com
    → redirects to b.com  (seen: [a.com])
    → redirects to c.com  (seen: [a.com, b.com])
    → redirects to a.com  ← ALREADY IN SEEN LIST → STOP
```

Simple, but saves you from infinite loops.

---

## 👊 Punch #3 — The Spider Trap 🕷️

This is the sneakiest attack. Imagine a website with URLs like this:

```
example.com/page?id=1
example.com/page?id=2
example.com/page?id=3
...
example.com/page?id=999999999999
```

Or even worse — a website that **generates new URLs dynamically:**

```
example.com/calendar/2024/01/01
example.com/calendar/2024/01/02
...
example.com/calendar/2024/01/01?ref=home
example.com/calendar/2024/01/01?ref=nav
example.com/calendar/2024/01/01?ref=footer
← Same page, infinite unique URLs
```

Every URL looks new. Your crawler keeps adding them. Your frontier explodes. You've been **trapped** — your crawler is spending 100% of its time on one useless website.

**The Fix: Multiple layers of protection**

```
LAYER 1 — URL Normalization
Before storing any URL, normalize it:

  https://Example.COM/Page?b=2&a=1#section
  
  → lowercase domain:      example.com
  → sort query params:     ?a=1&b=2
  → remove fragment:       (remove #section)
  → remove tracking params: (remove utm_source, ref, etc.)
  
  Result: example.com/page?a=1&b=2
  
  Now "?ref=home" and "?ref=nav" versions map to the SAME URL.

LAYER 2 — Per-domain URL limit
  
  "Never crawl more than 10,000 URLs from a single domain."
  
  This caps the damage any one site can do.

LAYER 3 — Path depth limit
  
  "Never follow links more than 5 directories deep."
  
  example.com/a/b/c/d/e/f/g/h ← skip this (too deep)

LAYER 4 — Content fingerprinting
  
  Even with different URLs, if the PAGE CONTENT is the same,
  don't store it again (more on this in the Deduplication chapter).
```

---

## 👊 Punch #4 — DNS Is Slow and Fails

To visit `nytimes.com`, your fetcher first has to look up its IP address via DNS:

```
nytimes.com → ??? → DNS Server → 151.101.193.164
```

This DNS lookup happens **every single request** by default. With 100 machines making thousands of requests per second, you're hammering DNS servers. And DNS lookups can take **50-200ms each** — that's your biggest latency bottleneck.

**The Fix: Local DNS Cache**

```
Each fetcher maintains its own DNS cache:

First request:
  nytimes.com → [DNS lookup: 150ms] → 151.101.193.164
  Cache: {nytimes.com: 151.101.193.164, expires: +1hour}

All requests for next 1 hour:
  nytimes.com → [Cache hit: 0ms] → 151.101.193.164
```

But be careful — cache entries must expire (TTL — Time To Live). If a site changes its IP and you keep using the old one, all your requests fail silently.

```
DNS Cache entry:
  {
    domain: "nytimes.com",
    ip: "151.101.193.164",
    cached_at: "10:00:00",
    ttl: 3600  ← expire after 1 hour
  }
```

---

## 👊 Punch #5 — The Server Says "Slow Down" (Rate Limiting & Bans)

Even with polite crawling, some servers respond with:

```
HTTP 429 Too Many Requests
Retry-After: 120   ← come back in 120 seconds
```

Or they just silently start returning garbage — broken HTML, empty pages, CAPTCHAs — without telling you they've detected you.

**The Fix: Exponential Backoff + Detection**

```
EXPONENTIAL BACKOFF:
  
  First failure   → wait 1 second, retry
  Second failure  → wait 2 seconds, retry
  Third failure   → wait 4 seconds, retry
  Fourth failure  → wait 8 seconds, retry
  Fifth failure   → wait 16 seconds, retry
  Give up after 5 retries → mark URL as permanently failed
  
  Why exponential? 
  → If a server is overloaded, hitting it repeatedly makes it worse.
  → Backing off gives it time to recover.
  → If many crawlers use this, traffic naturally spreads out.

SILENT BAN DETECTION:
  
  Keep a health score per domain:
  
  {
    domain: "example.com",
    success_rate: 0.12,  ← 12% of requests succeeding
    avg_content_size: 48  ← pages returning only 48 bytes (suspiciously small)
  }
  
  If success_rate drops below 20% → slow down crawling this domain
  If avg_content_size is tiny → probably getting blocked, stop and investigate
```

---

## 👊 Punch #6 — JavaScript-Rendered Pages

You fetch `twitter.com`. The HTML you get back is:

```html
<html>
  <body>
    <div id="root"></div>
    <script src="app.js"></script>
  </body>
</html>
```

Empty. Because Twitter's content is rendered by JavaScript in the browser. Your fetcher just downloads raw HTML — it doesn't run JavaScript.

```
What a browser sees:     Thousands of tweets, profiles, links
What your fetcher sees:  An empty div and a script tag
```

**The Fix: Two-tier fetching**

```
TIER 1 — Simple HTTP Fetcher (fast, cheap)
  Use for: news sites, blogs, Wikipedia — anything with real HTML
  Tool: Simple HTTP library (like curl)
  Speed: Very fast, thousands per second per machine

TIER 2 — Headless Browser Fetcher (slow, expensive)
  Use for: React/Angular/Vue apps, Twitter, Instagram, SPAs
  Tool: Headless Chrome (a real browser with no screen)
  Speed: Much slower, maybe 10-50 per second per machine
  
Detection: 
  If fetched HTML content < 500 bytes AND contains <script> tags
  → Re-queue with "needs JS rendering" flag
  → Route to Tier 2 fetcher
```

Headless Chrome is a real browser running on your servers — it loads the page exactly like a user would, runs all the JavaScript, then gives you the final rendered HTML.

---

## 🏗️ The Fetcher Architecture — Putting It All Together

Now let's look at how a single fetcher machine actually works internally:

```
┌─────────────────────────────────────────────────────────┐
│                    FETCHER NODE                          │
│                                                          │
│  ┌─────────────┐                                        │
│  │ URL Frontier│ ← Picks up next URL to crawl           │
│  │  Consumer   │                                        │
│  └──────┬──────┘                                        │
│         │                                               │
│  ┌──────▼──────┐                                        │
│  │DNS Resolver │ ← Local cache, avoids repeated lookups │
│  │  + Cache    │                                        │
│  └──────┬──────┘                                        │
│         │                                               │
│  ┌──────▼───────────────────────────────────┐          │
│  │         Fetch Engine                     │          │
│  │                                          │          │
│  │  ┌─────────────┐   ┌──────────────────┐ │          │
│  │  │  HTTP Pool  │   │ Headless Browser │ │          │
│  │  │(simple HTML)│   │  Pool (JS sites) │ │          │
│  │  └──────┬──────┘   └────────┬─────────┘ │          │
│  │         └─────────┬─────────┘           │          │
│  │              Timeout/Retry               │          │
│  │              Rate Limit Handler          │          │
│  └──────────────────┬───────────────────────┘          │
│                     │                                   │
│  ┌──────────────────▼──────────────────┐               │
│  │           Response Handler          │               │
│  │  - Check status code                │               │
│  │  - Follow redirects (max 5)         │               │
│  │  - Detect spider traps              │               │
│  │  - Validate content                 │               │
│  └──────────────────┬──────────────────┘               │
│                     │                                   │
│         ┌───────────┴───────────┐                      │
│         ▼                       ▼                      │
│  ┌─────────────┐       ┌─────────────────┐             │
│  │  Raw HTML   │       │  Error Queue    │             │
│  │  to Parser  │       │  (for retries)  │             │
│  └─────────────┘       └─────────────────┘             │
└─────────────────────────────────────────────────────────┘
```

---

## ⚡ How Do You Make One Fetcher Node Fast?

One fetcher machine shouldn't crawl URLs **one at a time** — that's slow. It should crawl **hundreds simultaneously.**

But not using threads! Threads are expensive. You use **async I/O:**

```
SYNCHRONOUS (slow, one at a time):

  Fetch URL 1 → wait 200ms → done
                                   Fetch URL 2 → wait 200ms → done
                                                                    Fetch URL 3...
  Total time for 3 URLs: 600ms


ASYNC I/O (fast, all at once):

  Send request for URL 1 ─────────────────────► receive (200ms)
  Send request for URL 2 ──────────────────► receive (180ms)
  Send request for URL 3 ────────────────────────────► receive (220ms)
  
  Total time for 3 URLs: 220ms  ← limited by slowest, not sum
```

With async I/O, one fetcher machine can handle **500-1000 concurrent requests**, waiting on all of them simultaneously, processing each response as it arrives.

This is exactly what Node.js or Python's asyncio are built for.

---

## 📊 Summary — What the Fetcher Handles

| Problem | Solution |
|---|---|
| Server hangs forever | Connection + Read + Total timeouts |
| Infinite redirect loops | Max 5 redirects + cycle detection |
| Spider traps | URL normalization + per-domain limits + depth limits |
| Slow DNS | Local DNS cache with TTL |
| Getting banned | Exponential backoff + health score monitoring |
| JavaScript pages | Two-tier fetching (HTTP + Headless Chrome) |
| Slow sequential fetching | Async I/O — 500+ concurrent requests per node |

---

## 🎯 Likely Interview Questions on This

- *"What happens if a server never responds?"* → Three-layer timeout system
- *"How do you handle rate limiting?"* → Exponential backoff with max retries
- *"What is a spider trap and how do you avoid it?"* → URL normalization, depth limits, per-domain caps
- *"How do you crawl JavaScript-heavy sites?"* → Headless browser tier
- *"How do you make a single fetcher node efficient?"* → Async I/O, concurrent requests

---

You now deeply understand **the Fetcher Fleet** — the machines that brave the chaos of the real internet.

But now your fetchers are returning raw HTML. Mountains of it. Next, we need to answer:

*"How do you know if you've already seen this page before? And how do you extract the links and content from it?"*

That's **Chapter 4: Deduplication & The Parser** — where we tackle one of the most elegant problems in computer science: how do you check if you've seen a page before... when you have **billions** of pages and almost no time to check?

Ready? 🚀
---# 🔍 Chapter 4: Deduplication & The Parser — "Have We Been Here Before?"

---

## The Story So Far

Your fetchers are downloading pages at massive scale. Thousands of pages per second, across hundreds of machines.

But a new crisis is emerging. You start noticing things like:

```
Fetcher 12 downloaded: wikipedia.org/Python
Fetcher 47 downloaded: wikipedia.org/Python  ← same page!
Fetcher 83 downloaded: wikipedia.org/Python  ← again!
```

And it's not just exact duplicates. The internet is **full of near-duplicates:**

```
example.com/article?page=1
example.com/article?print=true    ← same article, printer-friendly version
example.com/article?lang=en-us    ← same article, slightly different header
syndicated-news.com/article        ← same article, copy-pasted to another site
```

You're wasting enormous resources — fetching, storing, and indexing the same content over and over.

You need to answer **two questions**, extremely fast, billions of times:

> 1. "Have we crawled this **URL** before?"
> 2. "Have we seen this **content** before — even from a different URL?"

These are two separate problems. Let's solve them one by one.

---

## Problem 1: URL Deduplication — "Have We Seen This URL?"

### The Naive Approach — A Big Set

Your first instinct: keep a set of all crawled URLs. Before crawling, check if the URL is in the set.

```python
seen_urls = set()

def should_crawl(url):
    if url in seen_urls:
        return False
    seen_urls.add(url)
    return True
```

This works perfectly — on your laptop. But at scale:

```
Average URL length: ~100 characters = 100 bytes

5 billion URLs × 100 bytes = 500 GB

← You need half a terabyte of RAM just for URLs
```

That's impossibly expensive. You need something smarter.

---

### The Clever Solution — Bloom Filters 🌸

This is one of the most beautiful ideas in computer science. Let me build it up from scratch.

**The Core Idea:** What if instead of storing the URL itself, you just stored a **fingerprint** of whether you've seen it?

Imagine a giant array of bits — all starting at 0:

```
Position: 0  1  2  3  4  5  6  7  8  9  10 ...
Bits:      0  0  0  0  0  0  0  0  0  0  0  ...
```

When you want to "store" a URL, you run it through **multiple hash functions**, each giving you a different position in the array, and you flip those bits to 1:

```
URL: "wikipedia.org/Python"

hash1("wikipedia.org/Python") = 3  → flip bit 3 to 1
hash2("wikipedia.org/Python") = 7  → flip bit 7 to 1
hash3("wikipedia.org/Python") = 11 → flip bit 11 to 1

Position: 0  1  2  3  4  5  6  7  8  9  10 11 ...
Bits:      0  0  0  1  0  0  0  1  0  0  0  1  ...
                   ↑              ↑              ↑
```

Now, to **check** if a URL has been seen:

```
URL: "wikipedia.org/Python"  ← checking this

hash1 → position 3  → bit is 1 ✓
hash2 → position 7  → bit is 1 ✓
hash3 → position 11 → bit is 1 ✓

All bits are 1 → "Probably seen before" → SKIP
```

```
URL: "wikipedia.org/Java"  ← new URL

hash1 → position 5  → bit is 0 ✗

Even one 0 → "Definitely NOT seen before" → CRAWL IT
```

**The magic: If even ONE bit is 0 → definitely not seen. If ALL bits are 1 → probably seen.**

Why "probably"? Because of **hash collisions** — different URLs might accidentally flip the same bits:

```
"wikipedia.org/Python" flips bits: 3, 7, 11
"some-other-url.com"   flips bits: 3, 9, 14

Now a NEW URL hashes to positions 7, 11, 9
All those bits are 1 (set by different URLs)
→ Bloom filter says "seen before" — but it hasn't been!
← This is a FALSE POSITIVE
```

Bloom filters can have false positives (saying "seen" when it hasn't been) — **but never false negatives** (they never say "not seen" for something that was seen).

In a crawler, a false positive just means you skip a URL you could have crawled. Slightly suboptimal but totally acceptable.

**The size advantage is incredible:**

```
Storing 5 billion URLs:
  Raw set:      500 GB   (100 bytes per URL)
  Bloom filter: ~5 GB    (just ~8-10 bits per URL)
  
  100x smaller, with ~1% false positive rate
```

```
┌────────────────────────────────────────────────────────┐
│                    BLOOM FILTER                        │
│                                                        │
│  Bit Array: [0,1,0,1,0,0,0,1,0,0,0,1,0,1,0,0...]    │
│                                                        │
│  New URL arrives                                       │
│       │                                               │
│       ├──► hash1 ──► position X ──► bit = 0? ──► NEW │
│       ├──► hash2 ──► position Y ──►  ↓                │
│       └──► hash3 ──► position Z ──► all 1s? ──► SKIP │
│                                                        │
│  Fits in RAM on ONE machine for billions of URLs      │
└────────────────────────────────────────────────────────┘
```

---

### But What About Distributed Bloom Filters?

One machine's Bloom filter can't be shared with 100 fetcher machines in real time.

**Option 1 — Centralized Bloom Filter Service**

```
All 100 fetcher machines → query → Central Bloom Filter Service
                                         ↓
                                    Redis (in-memory)
                                    
Pros: Always consistent
Cons: Single point of failure, network latency on every check
```

**Option 2 — Replicated Bloom Filter**

```
Master Bloom Filter
  → Sync copy to all 100 fetcher machines every 60 seconds

Each fetcher checks its LOCAL copy (zero network cost)
Accepts slight staleness: might crawl a URL twice in a 60-second window
← Totally acceptable tradeoff
```

Most real crawlers use Option 2. Eventual consistency is fine here.

---

## Problem 2: Content Deduplication — "Have We Seen This Page?"

URL deduplication isn't enough. Even with unique URLs, you get:

```
cnn.com/article/xyz       ← Original article
foxnews.com/story/abc     ← Different URL, 95% same content (syndicated)
msn.com/news/copied       ← Different URL, 100% same content (scraped)
```

You need to detect duplicate **content**, not just URLs.

### Attempt 1 — Hash the Entire Page

Simple idea: compute a hash (like MD5 or SHA256) of the HTML content. If two pages have the same hash, they're identical.

```
MD5(wikipedia.org/Python HTML) = "a3f4b2c1..."
MD5(mirror.org/Python HTML)    = "a3f4b2c1..."  ← exact match → duplicate!
```

Works great for **exact duplicates**. But fails for **near-duplicates:**

```
Article with typo fixed:
  Original:  "The president anounced today..."
  Fixed:     "The president announced today..."
  
  MD5(original) = "a3f4b2c1..."
  MD5(fixed)    = "99d2e8f7..."  ← completely different hash, 1 character changed!
```

One character difference = totally different hash. Near-duplicates are invisible.

---

### The Elegant Solution — Simhash 🧠

Simhash is the algorithm Google actually uses. The genius of it: **similar documents produce similar hashes.**

Let me show you how it works, step by step.

**Step 1: Break the page into features (words/shingles)**

```
Page content: "Python is a great programming language"

Features (individual words):
["Python", "is", "a", "great", "programming", "language"]
```

**Step 2: Hash each feature into a 64-bit number**

```
hash("Python")      = 1001011010...  (64 bits)
hash("is")          = 0110100101...
hash("a")           = 1100010110...
hash("great")       = 0101101001...
hash("programming") = 1010010110...
hash("language")    = 0011010101...
```

**Step 3: Build a 64-position vector, starting at 0**

```
Vector: [0, 0, 0, 0, 0, 0, ... 0]  (64 positions)
```

**Step 4: For each feature's hash, for each bit:**
- If bit is 1 → add +1 to that vector position
- If bit is 0 → add -1 to that vector position

```
After processing all features, vector might look like:
[+3, -1, +2, -4, +1, -2, +3, ...]
```

**Step 5: Convert back to bits:**
- Positive number → 1
- Negative number → 0

```
[+3, -1, +2, -4, +1, -2, +3, ...]
 →1   →0   →1   →0   →1   →0   →1 ...

Final Simhash: "1010101..."
```

**Why is this magic?** Because similar documents share most of their words, so their vectors end up similarly weighted, so their final bit strings are similar:

```
"Python is a great programming language"
Simhash: 1001011010110010...

"Python is a wonderful programming language"  ← one word changed
Simhash: 1001011010110110...  ← only 2 bits different!
         ^^^^^^^^^^^^^^  ^^
         same            slightly different
```

To measure similarity, you count how many bits differ — called the **Hamming distance:**

```
Hamming distance 0  → identical content
Hamming distance 1-3  → near-duplicate (same article, tiny changes)
Hamming distance 10+  → different content
```

**The storage advantage:**

```
Storing full HTML: megabytes per page × billions = petabytes
Storing Simhash:   8 bytes per page  × billions = ~40 GB total

Fits in memory!
```

---

### Finding Near-Duplicates Efficiently

You have billions of Simhashes stored. A new page comes in with a Simhash. You need to find any stored hash within Hamming distance 3 — fast.

Naively comparing against all billions of stored hashes is too slow.

**The trick: Banding**

Split each 64-bit Simhash into **bands** of bits:

```
Simhash: [bits 1-16] [bits 17-32] [bits 33-48] [bits 49-64]
              Band 1      Band 2       Band 3       Band 4
```

If two Simhashes are similar (differ by ≤3 bits), at least one of their bands is **likely to be identical**.

```
Hash A: 1001 0110 | 1010 0011 | 1100 1001 | 0110 1010
Hash B: 1001 0110 | 1010 0011 | 1100 1001 | 0111 1010
                                                ^ 1 bit different

Band 1: identical ← will match in the index!
Band 2: identical
Band 3: identical
Band 4: different
```

Store each band in a hash table:

```
Band 1 index: {"1001 0110" → [doc_id_1, doc_id_5, doc_id_23...]}
Band 2 index: {"1010 0011" → [doc_id_1, doc_id_7, ...]}
```

To find duplicates of a new document:

```
1. Compute Simhash of new document
2. Split into 4 bands
3. Look up each band in the index
4. Candidates = any doc that matches ANY band
5. For each candidate, compute exact Hamming distance
6. If distance ≤ 3 → duplicate!
```

This turns a "compare against everything" problem into a "look up in 4 hash tables" problem. Blazingly fast.

---

## The Parser — "What's Actually On This Page?"

Once a page passes deduplication, it goes to the **Parser**. The parser has two jobs:

```
Raw HTML in
    │
    ├──► Extract Links  ──► Back to URL Frontier
    │
    └──► Extract Content ──► To Content Store (for search indexing)
```

### Job 1: Link Extraction

```html
<!-- Raw HTML -->
<html>
  <a href="/wiki/Java">Java</a>
  <a href="https://python.org">Python</a>
  <a href="mailto:admin@site.com">Contact</a>  ← NOT a web URL
  <a href="javascript:void(0)">Click</a>        ← NOT a web URL
</html>
```

The parser must:

```
1. Extract all href attributes from <a> tags

2. Resolve relative URLs:
   Current page: wikipedia.org/wiki/Python
   Relative link: /wiki/Java
   → Resolved: wikipedia.org/wiki/Java  ✓

3. Filter out non-HTTP links:
   mailto:... → discard
   javascript:... → discard
   ftp:... → discard

4. Normalize (same rules as before):
   → lowercase, sort params, remove fragments

5. Send valid URLs to Frontier (after Bloom filter check)
```

### Job 2: Content Extraction

The parser strips away HTML boilerplate and extracts meaningful content:

```
Raw HTML (50KB):                      Extracted content (5KB):
<html>                                {
<head>...</head>                        title: "Python Programming",
<nav>Home | About | Contact</nav>       url: "wikipedia.org/wiki/Python",
<div class="ad">Buy stuff!</div>        content: "Python is a high-level...",
<article>                               links: ["wikipedia.org/wiki/Java"...],
  <h1>Python Programming</h1>           language: "en",
  <p>Python is a high-level...</p>      crawled_at: "2024-01-15T10:00:00"
</article>                            }
<footer>Copyright 2024</footer>
</html>
```

This cleaned content goes into the **Content Store** for the search engine to index.

---

## 🏗️ The Full Deduplication + Parsing Pipeline

```
Raw HTML arrives from Fetcher
          │
          ▼
┌─────────────────────┐
│   URL Dedup Check   │ ← Bloom Filter (was this URL already processed?)
│                     │   If YES → discard
└─────────┬───────────┘
          │ (new URL)
          ▼
┌─────────────────────┐
│  Content Simhash    │ ← Compute Simhash of HTML content
│  Dedup Check        │   If near-duplicate found → discard
└─────────┬───────────┘
          │ (unique content)
          ▼
┌─────────────────────┐
│      Parser         │
│                     │
│  Extract links ─────┼──────────► URL Frontier
│  Extract content ───┼──────────► Content Store
└─────────────────────┘
```

---

## 📊 Summary — Deduplication Toolbox

| Problem | Solution | Size |
|---|---|---|
| URL seen before? | Bloom Filter | ~5GB for 5B URLs |
| Exact content duplicate? | MD5/SHA hash comparison | 16 bytes per page |
| Near-duplicate content? | Simhash + Hamming distance | 8 bytes per page |
| Finding near-dupes fast? | Banding technique | 4 hash table lookups |

---

## 🎯 Likely Interview Questions on This

- *"How do you check if a URL has been crawled before at scale?"* → Bloom Filter, explain false positives are acceptable
- *"What's the difference between exact and near-duplicate detection?"* → MD5 for exact, Simhash for near-dupes
- *"How does Simhash work?"* → Feature hashing → vector → single hash, similar docs = similar hash
- *"How do you find near-duplicates efficiently among billions of docs?"* → Banding technique on Simhash
- *"Why not just store all URLs in a database?"* → Memory cost — Bloom filter is 100x smaller

---

You now understand how we avoid wasting work on pages we've already seen, and how we extract meaning from raw HTML.

But we're generating **enormous** amounts of data. Crawled content needs to be stored somewhere reliable, fast, and scalable. And our whole system has single points of failure we haven't addressed yet.

That brings us to **Chapter 5: Storage, Replication & Fault Tolerance** — where we ask: *"What happens when machines die? And where does all this data actually live?"*

Ready? 🚀
---
# 💾 Chapter 5: Storage, Replication & Fault Tolerance — "What Happens When Things Break?"

---

## The Story So Far

Your crawler is humming. Fetchers are downloading thousands of pages per second. The parser is extracting links and content. Deduplication is filtering duplicates.

But then, at 2am on a Tuesday, you get a phone call.

> *"Hey... the storage server just crashed. We lost everything."*

Three weeks of crawling. **Gone.**

You rebuild. A month later:

> *"Two fetcher machines died mid-crawl. We don't know which URLs they were processing. There are gaps in our index."*

Then:

> *"Traffic spiked. The content database can't handle the read load. Search is timing out."*

Every one of these is a **different kind of failure.** And in a distributed system running hundreds of machines, failure isn't an exception — it's the **default state.** At any moment, some machine somewhere is dying.

This chapter is about designing a system where **failure is expected, handled, and invisible to the outside world.**

---

## First — What Data Are We Actually Storing?

Before designing storage, let's map out exactly what our crawler generates:

```
┌─────────────────────────────────────────────────────────┐
│              DATA GENERATED BY CRAWLER                  │
│                                                         │
│  1. URL Frontier State                                  │
│     "Which URLs are queued, their priority, status"     │
│     Size: Billions of entries, ~200 bytes each          │
│     Access: Very high write + read (every second)       │
│                                                         │
│  2. URL Seen Store                                      │
│     "Which URLs have been crawled"                      │
│     Size: ~5GB (Bloom Filter) + metadata DB             │
│     Access: Extremely high read (every new URL)         │
│                                                         │
│  3. Raw HTML Store                                      │
│     "The actual downloaded HTML of every page"          │
│     Size: ~50KB avg × 5B pages = 250 TB                 │
│     Access: Write-heavy, occasional re-read             │
│                                                         │
│  4. Extracted Content Store                             │
│     "Cleaned text, metadata, for search indexing"       │
│     Size: ~5KB avg × 5B pages = 25 TB                   │
│     Access: Read-heavy (search engine queries)          │
│                                                         │
│  5. Crawl Metadata                                      │
│     "When was each URL last crawled, status, errors"    │
│     Size: ~500 bytes × 5B pages = 2.5 TB               │
│     Access: High read + write                           │
└─────────────────────────────────────────────────────────┘
```

Each of these has **different access patterns** — and that's the key insight. You don't use one database for everything. You match the storage system to the access pattern.

---

## The Storage Layer — Right Tool For Right Job

### Store 1: URL Frontier → Apache Kafka

You already know this from Chapter 2. Kafka is a **distributed commit log.**

```
Why Kafka for the Frontier?
  ✓ Handles millions of writes per second
  ✓ Durable — persists to disk, survives crashes
  ✓ Partitioned — splits load across machines automatically
  ✓ Replayable — if a consumer crashes, it can re-read from where it left off
  
The "replayable" property is crucial:
  
  Fetcher 12 crashes mid-processing
  → Kafka remembers it hadn't acknowledged those URLs
  → Another fetcher automatically picks them up
  → No URLs are lost or skipped
```

This is called **at-least-once delivery** — Kafka guarantees every URL will be processed by at least one fetcher, even through failures.

---

### Store 2: URL Seen Store → Redis + Cassandra

Two layers:

```
LAYER 1 — Redis (in-memory, blazing fast)
  Stores: Bloom Filter bits
  Why Redis: Bloom filter needs sub-millisecond access
             Redis keeps everything in RAM
             Can handle 1 million lookups/second per node
  
LAYER 2 — Cassandra (distributed database)
  Stores: Actual URL metadata
  {
    url: "wikipedia.org/Python",
    first_crawled: "2024-01-10",
    last_crawled: "2024-03-15",
    crawl_frequency: "weekly",
    status: "success",
    http_status: 200,
    content_hash: "a3f4b2..."
  }
  
  Why Cassandra:
  ✓ Designed for massive write throughput
  ✓ Distributed — data automatically spread across nodes
  ✓ No single point of failure
  ✓ Scales horizontally — add more nodes, get more capacity
```

---

### Store 3: Raw HTML → Object Storage (S3-like)

Raw HTML is **large and write-once.** You write it once, rarely read it, and need to store petabytes of it.

```
Perfect fit: Object Storage (Amazon S3, Google Cloud Storage)

How it works:
  Each crawled page gets a unique key:
  
  Key:   "crawl/2024/01/15/wikipedia.org/Python/raw.html.gz"
  Value: <compressed raw HTML bytes>
  
Why object storage?
  ✓ Infinitely scalable (add storage by paying more, not managing servers)
  ✓ 11 nines of durability — Amazon internally replicates 3× across data centers
  ✓ Cheap — pennies per GB per month
  ✓ Content is compressed (gzip) before storing → 70% smaller
  
Access pattern:
  Write: Once, when page is crawled
  Read: Occasionally, when re-processing or re-indexing is needed
```

---

### Store 4: Extracted Content → Elasticsearch / Bigtable

This is what the search engine actually queries. It needs to be:
- Fast to read (search queries come in constantly)
- Structured (support filtering by date, domain, language)
- Full-text searchable

```
Two options depending on scale:

OPTION A — Elasticsearch (for search-focused access)
  - Inverted index: "python" → [doc1, doc5, doc23...]
  - Supports complex queries, ranking, filtering
  - Horizontally scalable

OPTION B — Google Bigtable / HBase (for large-scale structured access)
  Row key: reverse-domain + timestamp
  "org.wikipedia/Python/2024-01-15"
  
  Why reverse domain?
  
  Normal:   wikipedia.org/Python
            wikipedia.org/Java
            nytimes.com/politics
  
  Reversed: org.wikipedia/Python    ← these cluster together on disk!
            org.wikipedia/Java      ← fast range scans by domain
            com.nytimes/politics
  
  Benefit: "Give me all Wikipedia pages crawled this week"
           → one fast sequential disk scan
           instead of random lookups scattered everywhere
```

---

## Replication — "Never Lose Data"

Now the critical question: how do you make sure data survives machine failures?

The answer is **replication** — storing the same data on multiple machines.

But replication introduces a new problem: **consistency.** If you write to Machine A, and Machine B has a copy, but the network between them is slow — Machine B might be behind. Which one is "correct"?

This is the famous **CAP Theorem** — and it's almost guaranteed to come up in interviews.

---

### The CAP Theorem — Explained Through a Story

Imagine you have two database nodes, Node A and Node B, both storing the same data:

```
Node A ←——— network ———→ Node B
```

One night, the network cable between them gets cut. They can't talk to each other. This is called a **Network Partition.** In distributed systems, partitions **always happen** eventually. So CAP says:

> *When a partition happens, you must choose: do you want Consistency or Availability?*

**Consistency (C):** Every read gets the most recent write.
```
Network is cut.
User writes "Python = awesome" to Node A.
Node A can't tell Node B.
User then reads from Node B.

CONSISTENT choice: Node B refuses to answer.
"I might be out of date. I won't respond until 
 the network is restored."
→ System is unavailable but never wrong.
```

**Availability (A):** Every request gets a response (might be stale).
```
AVAILABLE choice: Node B answers with its old data.
"I'll give you what I have, even if it's stale."
→ System is always up but might be wrong.
```

**For a web crawler, which do you choose?**

```
URL Frontier (Kafka) → Availability
  "I'd rather process a URL twice than miss it."
  
URL Seen Store (Cassandra) → Availability  
  "I'd rather have a 0.001% false positive than be down."
  
Crawl Metadata (Cassandra) → Availability
  "Slightly stale crawl timestamps are fine."
  
Content Store (Elasticsearch) → Consistency
  "Search results must be accurate."
```

Different components make different CAP choices. **That's the real insight** — not "pick one for your whole system."

---

### Replication Strategies — How Do You Actually Copy Data?

#### Strategy 1: Leader-Follower Replication

```
                    ┌──────────────┐
    All Writes ────►│   LEADER     │
                    │   (Node A)   │
                    └──────┬───────┘
                           │ replicates to
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │FOLLOWER 1│ │FOLLOWER 2│ │FOLLOWER 3│
        └──────────┘ └──────────┘ └──────────┘
    All Reads can go to any follower (spreads load)
```

**How it works:**
- All writes go to the Leader
- Leader records the write in a **replication log**
- Followers continuously apply that log to stay in sync

```
Replication Log on Leader:
  [timestamp: 10:00:01] SET url="wikipedia.org/Python" status="crawled"
  [timestamp: 10:00:02] SET url="reddit.com/r/tech" status="queued"
  ...

Follower 1 is at position 847 in the log
→ It asks leader: "Give me everything after position 847"
→ Applies those changes
→ Now in sync
```

**Failure scenarios:**

```
FOLLOWER DIES:
  → No problem. Other followers still serving reads.
  → Dead follower restarts, catches up from the log.
  → System never went down.

LEADER DIES:
  → Problem! Who takes over?
  → Automatic leader election happens:
  
  Followers vote among themselves:
  "I'm at log position 1052"
  "I'm at log position 1050" 
  "I'm at log position 1052"
  
  → Follower with highest log position wins
  → Becomes new leader
  → Other followers point to new leader
  
  This process takes ~10-30 seconds (automated)
  During this time: system may be briefly unavailable
```

---

#### Strategy 2: Leaderless Replication (Cassandra's approach)

No single leader. Any node can accept writes. This is what Cassandra uses.

```
You have 5 nodes: N1, N2, N3, N4, N5

Write "url=wikipedia.org/Python":
  → Send to ALL 5 nodes simultaneously
  → Wait for ANY 3 to confirm (this is called quorum)
  → If 3 confirmed → write is "successful"
  
Read "url=wikipedia.org/Python":
  → Send to ALL 5 nodes simultaneously  
  → Wait for ANY 3 to respond
  → If responses match → return value
  → If responses conflict → return most recent (by timestamp)
```

The formula that makes this work:

```
W + R > N

W = nodes that must confirm a write   (3)
R = nodes that must confirm a read    (3)
N = total nodes                       (5)

3 + 3 > 5  ✓

This guarantees that the READ set and WRITE set 
always overlap by at least one node.
That overlapping node has the latest data.
→ You always read what was last written.
```

```
What if 2 nodes are down?

Write still succeeds (only need 3 of 5)
Read still succeeds (only need 3 of 5)
System stays available even with 2 node failures
```

---

## Fault Tolerance — "Designing for Failure"

Now let's think about every component of our crawler and ask: *"What happens when this fails?"*

### Failure Map

```
┌─────────────────────────────────────────────────────────────┐
│  COMPONENT          FAILURE           SOLUTION              │
├─────────────────────────────────────────────────────────────┤
│  Fetcher Node       Machine crash     Stateless design       │
│                                       → any node can         │
│                                         replace it          │
├─────────────────────────────────────────────────────────────┤
│  URL Frontier       Kafka node dies   Kafka replication      │
│  (Kafka)                              → 3 replicas per       │
│                                         partition           │
├─────────────────────────────────────────────────────────────┤
│  URL Seen Store     Redis crash       Redis Sentinel         │
│  (Redis)                              → auto-failover to     │
│                                         replica             │
├─────────────────────────────────────────────────────────────┤
│  Content Store      Node failure      Cassandra quorum       │
│  (Cassandra)                          → data on 3 nodes,     │
│                                         survive 2 failures  │
├─────────────────────────────────────────────────────────────┤
│  Raw HTML           Data center fire  S3 cross-region        │
│  (Object Store)                       replication           │
│                                       → 3 data centers      │
├─────────────────────────────────────────────────────────────┤
│  Entire data        Natural disaster  Multi-region           │
│  center down                          deployment            │
└─────────────────────────────────────────────────────────────┘
```

---

### The Stateless Fetcher — The Most Important Design Decision

The single best thing you can do for fault tolerance: **make your fetchers stateless.**

A stateless fetcher stores **nothing** on its own disk. It holds no state in memory between requests. It's just a function:

```
Input:  URL from Kafka
Output: HTML to storage + new URLs to Kafka

That's it. Nothing stored locally.
```

Why is this so powerful?

```
STATEFUL fetcher crashes:
  → All in-progress URLs are lost
  → All cached state is lost
  → Need to figure out what it was doing
  → Complex recovery logic
  → Data gaps possible

STATELESS fetcher crashes:
  → Kafka still has the URLs (they weren't acknowledged)
  → Another fetcher picks them up automatically
  → Zero data loss
  → Zero special recovery logic needed
  → Just restart the machine (or don't — a new one spins up)
```

This is why cloud-native systems love stateless services. You can kill and replace them like they're disposable — because they are.

---

### Checkpointing — "Saving Your Progress"

For longer operations (like parsing a giant HTML file), you need **checkpoints** — periodic saves of progress so that if a crash happens, you don't start from scratch.

```
Without checkpointing:
  Parser starts processing 10,000 page batch
  Crashes at page 9,847
  → Must restart from page 1
  → 9,847 pages of wasted work

With checkpointing:
  Parser processes 100 pages → writes checkpoint to DB:
    {batch_id: "xyz", last_processed: 100, timestamp: ...}
  Processes 100 more → checkpoint: {last_processed: 200}
  ...
  Crashes at page 9,847
  → Restarts, reads checkpoint: {last_processed: 9,800}
  → Resumes from page 9,801
  → Only 47 pages of wasted work
```

---

### Circuit Breakers — "Failing Fast, Not Slowly"

Imagine a downstream service (say, your content store) is overloaded and responding slowly. Your fetchers keep sending requests, waiting 30 seconds each time, piling up:

```
WITHOUT circuit breaker:
  100 fetchers × 30 second waits = system gridlock
  Content store gets MORE overloaded
  Everything slows down together
  Cascading failure → total outage

WITH circuit breaker:
  
  ┌─────────────────────────────────────────┐
  │           CIRCUIT BREAKER               │
  │                                         │
  │  CLOSED (normal): requests flow through │
  │  ↓ (5 failures in 10 seconds)           │
  │  OPEN (failing): requests blocked       │
  │    "Content store is down, skip it"     │
  │    Returns error immediately (no wait)  │
  │  ↓ (after 30 seconds)                   │
  │  HALF-OPEN: let one request through     │
  │    If it succeeds → back to CLOSED      │
  │    If it fails → back to OPEN           │
  └─────────────────────────────────────────┘
  
  Fetchers fail fast → move on to next URL
  Content store gets breathing room to recover
  System degrades gracefully instead of collapsing
```

---

## Putting It All Together — The Full Storage Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    CRAWLER STORAGE LAYER                    │
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │    Kafka     │    │    Redis     │    │  Cassandra   │  │
│  │  (Frontier)  │    │  (URL Seen)  │    │  (Metadata)  │  │
│  │  3 replicas  │    │  Sentinel    │    │  Quorum=3    │  │
│  │  per topic   │    │  failover    │    │  RF=3        │  │
│  └──────────────┘    └──────────────┘    └──────────────┘  │
│                                                             │
│  ┌──────────────────────────┐    ┌───────────────────────┐ │
│  │    Object Storage (S3)   │    │    Elasticsearch      │ │
│  │      (Raw HTML)          │    │  (Extracted Content)  │ │
│  │  Cross-region replicated │    │   3-node cluster      │ │
│  │  11 nines durability     │    │   1 primary + 2 rep   │ │
│  └──────────────────────────┘    └───────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 Summary

| Concern | Solution |
|---|---|
| Node crashes | Stateless services — anything can replace anything |
| Data loss | Replication — leader/follower or leaderless quorum |
| Consistency vs Availability | CAP theorem — choose per component |
| Cascading failures | Circuit breakers — fail fast |
| Long operation crashes | Checkpointing — save progress periodically |
| Storage scale | Right tool per access pattern — Kafka, Redis, Cassandra, S3 |

---

## 🎯 Likely Interview Questions on This

- *"How do you handle a fetcher node crashing mid-crawl?"* → Stateless design + Kafka at-least-once delivery
- *"Explain CAP theorem"* → Story of network partition, C vs A tradeoff, different choices per component
- *"How does Cassandra survive node failures?"* → Leaderless replication, quorum writes/reads, W+R>N
- *"What is a circuit breaker?"* → Closed/Open/Half-open states, prevents cascading failures
- *"How do you store petabytes of raw HTML?"* → Object storage, compressed, cross-region replicated

---

You now understand how the system survives failures gracefully.

But we've been building a system that handles today's load. What about tomorrow, when your index grows 10x? Or when a major news event causes a traffic spike and everyone wants to crawl the same news sites simultaneously?

That's **Chapter 6: Scaling — Horizontal Scaling, Sharding, and Handling Hot Spots** — where we ask: *"How do you grow the system without rebuilding it?"*

Ready? 🚀
---
# 📈 Chapter 6: Scaling — "How Do You Grow Without Breaking Everything?"

---

## The Story So Far

Your crawler is fault-tolerant, well-structured, and humming along. You're crawling 10,000 pages per second across 100 machines.

Then your startup gets acquired. The new boss walks in and says:

> *"Great work. We need to crawl 10x more pages. By next quarter."*

You can't just "try harder." You need to **scale the system.** And scaling a distributed system is full of surprises — what worked at 100 machines breaks at 1,000.

Then a second problem: it's election night. Every crawler in the world is trying to fetch `cnn.com`, `bbc.com`, `nytimes.com` simultaneously. These domains become **scorching hot** — and your system, designed for even load, starts melting.

This chapter is about two things:
1. **Scaling up** — handling 10x more load systematically
2. **Hot spots** — handling uneven load without collapsing

---

## Part 1: The Scaling Mindset

First, a crucial distinction that interviewers love to probe:

```
VERTICAL SCALING (Scale Up):
  "Make the machine bigger"
  
  Your server: 16 cores, 64GB RAM
  → Upgrade to: 64 cores, 256GB RAM
  
  ✓ Simple — no code changes
  ✗ Expensive — premium hardware costs exponentially more
  ✗ Hard limit — there's a biggest machine you can buy
  ✗ Single point of failure — one big machine, one big crash


HORIZONTAL SCALING (Scale Out):
  "Add more machines"
  
  Your servers: 10 machines × 16 cores
  → Add more: 100 machines × 16 cores
  
  ✓ Cheap commodity hardware
  ✓ No theoretical limit — keep adding machines
  ✓ Better fault tolerance — losing 1 of 100 machines is fine
  ✗ Complex — now you have a distributed system problem
```

Web crawlers **must** scale horizontally. The internet is too big for any single machine, no matter how powerful. Every design decision we've made so far — Kafka, Cassandra, stateless fetchers — was secretly setting us up for this moment.

---

## Part 2: How Each Component Scales

Let's walk through every component and answer: *"How do you add capacity when it's not enough?"*

---

### Scaling the Fetchers — The Easy One

Fetchers are stateless (remember Chapter 5?). That makes them trivially scalable.

```
Need more crawling throughput?

Step 1: Spin up more fetcher machines
Step 2: Point them at Kafka (the URL Frontier)
Step 3: They automatically start consuming URLs and crawling

That's it. No configuration changes. No resharding. No migration.

Before: 100 fetchers × 500 req/sec = 50,000 pages/sec
After:  200 fetchers × 500 req/sec = 100,000 pages/sec
```

This is the payoff of stateless design. Horizontal scaling becomes trivial.

In cloud environments, this can even be **automatic** — called autoscaling:

```
AUTOSCALING RULES:

  IF Kafka queue depth > 1,000,000 URLs
    → Spin up 20 more fetcher machines automatically

  IF Kafka queue depth < 10,000 URLs  
    → Shut down 20 fetcher machines (save money)

  This happens without any human intervention, in ~2 minutes
```

---

### Scaling Kafka — Partitioning

Kafka scales through **partitions.** A partition is a slice of a Kafka topic.

```
WITHOUT partitioning:
  One topic "urls-to-crawl" on one machine
  → One machine handles all writes and reads
  → Bottleneck: ~100MB/s throughput limit

WITH partitioning:
  Topic "urls-to-crawl" split into 100 partitions
  → Each partition on a different machine
  → Each partition handles a fraction of traffic
  → Total throughput: 100 × 100MB/s = 10GB/s
  
  ┌─────────────────────────────────────────────┐
  │         Topic: "urls-to-crawl"              │
  │                                             │
  │  Partition 0  [url1, url4, url7, url10...]  │← Machine A
  │  Partition 1  [url2, url5, url8, url11...]  │← Machine B
  │  Partition 2  [url3, url6, url9, url12...]  │← Machine C
  └─────────────────────────────────────────────┘
```

How does a URL get assigned to a partition?

```
hash(domain) % num_partitions = partition_id

hash("wikipedia.org") % 100 = 23 → Partition 23
hash("reddit.com")    % 100 = 67 → Partition 67

Benefit: All wikipedia.org URLs go to the same partition
→ One fetcher handles all of wikipedia
→ Politeness (per-domain timing) is naturally maintained
```

Adding more capacity:

```
Before: 100 partitions → 100 fetcher machines consuming them
After:  200 partitions → 200 fetcher machines

When you increase partitions, Kafka redistributes automatically.
Each fetcher machine gets reassigned to new partitions.
```

---

### Scaling Cassandra — Sharding

Cassandra stores billions of URL metadata records. As data grows, you need to spread it across more machines. This is **sharding** — splitting data across nodes.

Cassandra uses consistent hashing (sound familiar from Chapter 2?) to decide which node owns which data.

```
CASSANDRA RING — 6 nodes:

              Node A (token: 0)
           /                      \
    Node F                          Node B
  (token: 850)                   (token: 170)
         |                              |
    Node E                          Node C
  (token: 680)                   (token: 340)
           \                      /
              Node D (token: 510)


URL "wikipedia.org/Python":
  hash = 412
  → Falls between Node C (340) and Node D (510)
  → Assigned to Node D

URL "reddit.com/r/tech":
  hash = 205
  → Falls between Node B (170) and Node C (340)
  → Assigned to Node C
```

**Adding a new node:**

```
Current ring: Nodes A, B, C, D, E, F

Add Node G (token: 255) between Node B (170) and Node C (340):

Before:  URLs with hash 170-340 → Node C
After:   URLs with hash 170-255 → Node G  (moved to new node)
         URLs with hash 255-340 → Node C  (stayed on Node C)

Only 1/6th of data moved. Everything else untouched.
← This is consistent hashing's superpower
```

**Replication Factor (RF):**

```
RF = 3 means each piece of data lives on 3 consecutive nodes:

URL hashes to Node D → stored on D, E, F (next 2 clockwise)

If Node D dies:
  → Data still on E and F
  → Reads/writes continue uninterrupted
  → System automatically re-replicates to maintain RF=3
```

---

### Scaling the Content Store — Read Replicas

The content store (Elasticsearch) is **read-heavy.** Every search query reads from it. As traffic grows, read throughput becomes the bottleneck.

```
Problem:
  10,000 search queries/second
  Each query hits Elasticsearch
  3-node cluster → overloaded

Solution: Read Replicas
  
  ┌─────────────────────────────────────────┐
  │           ELASTICSEARCH CLUSTER         │
  │                                         │
  │  Primary Shard 1 ──────► Replica 1A    │
  │                   ──────► Replica 1B    │
  │                                         │
  │  Primary Shard 2 ──────► Replica 2A    │
  │                   ──────► Replica 2B    │
  │                                         │
  │  Primary Shard 3 ──────► Replica 3A    │
  │                   ──────► Replica 3B    │
  └─────────────────────────────────────────┘
  
  Writes: go to Primary shards only
  Reads:  can go to ANY replica
  
  With 2 replicas per shard:
  Read throughput = 3× (primary + 2 replicas all serving reads)
  
  Add more replicas → more read throughput, linearly
```

---

## Part 3: The Hot Spot Problem 🔥

Now the harder problem. Everything above assumes **even load** — all domains equally popular, all machines equally busy.

Reality is brutal. Load is never even.

---

### What Is a Hot Spot?

Election night. Breaking news. A celebrity dies. The internet **collectively** tries to read the same 10 websites.

```
Your crawlers' domain distribution — normally:
  wikipedia.org  →  2% of crawls
  reddit.com     →  1.5% of crawls
  nytimes.com    →  1% of crawls
  [997 other domains share the remaining 95.5%]

Election night:
  cnn.com       → 40% of crawls  ← HOT
  foxnews.com   → 25% of crawls  ← HOT
  nytimes.com   → 20% of crawls  ← HOT
  [everything else: 15%]
```

Since you used `hash(domain) % partitions` to assign domains to machines, all CNN URLs go to the same partition, the same machine.

```
Machine 47 (handles cnn.com):
  CPU: 100% 🔥
  Queue: 500,000 URLs backed up
  Response time: 45 seconds

Machine 23 (handles obscure-blog.com):
  CPU: 3%
  Queue: 12 URLs
  Response time: 0.1 seconds
```

Machine 47 is melting. Machine 23 is bored. **Load is catastrophically uneven.**

---

### Hot Spot Solution 1 — Virtual Nodes

Instead of one hash position per machine, give each machine **many positions** on the ring:

```
WITHOUT virtual nodes (each machine = 1 position):
  Machine A owns: hash range 0-250
  Machine B owns: hash range 251-500
  Machine C owns: hash range 501-750
  Machine D owns: hash range 751-999
  
  If cnn.com hashes to 175 → always Machine A → hot spot

WITH virtual nodes (each machine = many positions):
  Machine A owns: ranges 0-50, 300-350, 600-650, 800-850
  Machine B owns: ranges 51-100, 351-400, 651-700, 851-900
  Machine C owns: ranges 101-150, 401-450, 701-750, 901-950
  Machine D owns: ranges 151-200, 451-500, 751-800, 951-999
  
  cnn.com (175)    → Machine D
  bbc.com (325)    → Machine A
  nytimes.com (675)→ Machine B
  
  Hot domains now spread across ALL machines!
```

Virtual nodes also make **adding machines smoother:**

```
Add Machine E:
  Instead of one big range transfer,
  Machine E takes a few small ranges from A, B, C, D each.
  Load redistributes evenly and gradually.
```

---

### Hot Spot Solution 2 — Domain Sharding

For ultra-popular domains, split the domain itself across multiple partitions:

```
Normal assignment:
  cnn.com → Partition 23 (one machine handles all of CNN)

Domain sharding:
  cnn.com → Partition 23, 24, 25, 26, 27  (5 machines share CNN)
  
  How? Add a shard suffix to the key:
  
  "cnn.com#0" → hash → Partition 23
  "cnn.com#1" → hash → Partition 47
  "cnn.com#2" → hash → Partition 12
  "cnn.com#3" → hash → Partition 89
  "cnn.com#4" → hash → Partition 56
  
  Incoming CNN URL → randomly assign to "cnn.com#0" through "cnn.com#4"
  → 5 machines now share the load
```

But now you've broken the politeness guarantee — multiple machines hitting CNN simultaneously.

```
Fix: Each shard has its own politeness timer:
  
  "cnn.com#0" → last crawled at 10:00:00, next allowed: 10:00:01
  "cnn.com#1" → last crawled at 10:00:00, next allowed: 10:00:01
  
  Combined rate: 5 shards × 1 req/sec = 5 req/sec to CNN
  
  Still need to check cnn.com/robots.txt's Crawl-delay!
  If Crawl-delay = 1 and you have 5 shards → one request every 200ms per shard
  Total = 5/sec ← verify this is within CNN's limits
```

---

### Hot Spot Solution 3 — Adaptive Rate Limiting

Instead of fixed politeness timers, dynamically adjust based on server response:

```
ADAPTIVE CRAWL RATE:

  Monitor server response time for each domain:
  
  cnn.com response times:
    10:00:00 → 120ms  (healthy)
    10:00:10 → 180ms  (slight load)
    10:00:20 → 450ms  (getting busy)
    10:00:30 → 900ms  (stressed)
    10:00:40 → 2000ms (overloaded)
  
  Adaptive logic:
    Response < 200ms  → Increase crawl rate by 10%
    Response 200-500ms → Maintain current rate
    Response > 500ms  → Decrease crawl rate by 25%
    Response > 1000ms → Decrease crawl rate by 50%
    Timeout           → Back off for 60 seconds
  
  Result: You automatically throttle when a server is struggling
  CNN never gets overloaded. They never ban you.
  You get maximum throughput they can sustainably handle.
```

---

### Hot Spot Solution 4 — Caching at the DNS and Content Level

For extremely hot domains, many URLs end up being the same page revisited:

```
During a breaking news event:
  cnn.com/live-updates → crawled 10 seconds ago
  → Page probably hasn't changed
  → Don't re-crawl it yet!

SOLUTION: Conditional HTTP Requests

  First crawl:
    GET cnn.com/live-updates
    Response: 200 OK
              Last-Modified: Mon, 15 Jan 2024 09:58:00 GMT
              ETag: "abc123"
    
  Re-crawl 60 seconds later:
    GET cnn.com/live-updates
    If-Modified-Since: Mon, 15 Jan 2024 09:58:00 GMT
    If-None-Match: "abc123"
    
    Response: 304 Not Modified  ← Server says "nothing changed"
    
    → Skip parsing. Skip storage. Skip deduplication.
    → Used almost zero resources.
    → Bandwidth saved: 50KB → 200 bytes
```

This is called **conditional crawling.** For hot domains during news events, it reduces actual work by ~80%.

---

## Part 4: The Thundering Herd Problem

Related to hot spots but different. Imagine your crawler restarts after maintenance.

All 500 fetcher machines come online simultaneously. All 500 immediately ask Kafka: *"Give me URLs!"* All 500 simultaneously start hitting the same popular domains. All 500 simultaneously query Cassandra. All 500 simultaneously query Redis.

```
t=0: System offline
t=1: System comes back
t=1: 500 machines simultaneously hammer every downstream service
→ Cassandra: overloaded
→ Redis: overloaded
→ Popular domains: hammered
→ System crashes again immediately after restart
```

This is the **Thundering Herd** — a restart causes the exact crash you were trying to recover from.

**Solutions:**

```
SOLUTION 1 — Staggered Startup:
  Don't start all machines at once.
  Start 10 machines, wait 30 seconds.
  Start 10 more, wait 30 seconds.
  ...
  Takes 25 minutes to fully restart, but system never gets overwhelmed.

SOLUTION 2 — Jitter (Randomized Delays):
  Each machine adds a random delay before its first request:
  
  Machine 1:  wait random(0-30) seconds → starts at t=17s
  Machine 2:  wait random(0-30) seconds → starts at t=3s
  Machine 3:  wait random(0-30) seconds → starts at t=24s
  
  Load spreads out naturally. No coordination needed.

SOLUTION 3 — Cache Warming:
  Before restarting fetchers, pre-populate caches:
  → DNS cache: pre-load popular domains
  → Redis: pre-load bloom filter
  → Then start fetchers
  
  First requests hit warm caches instead of cold databases.
```

---

## Part 5: Measuring Scale — The Metrics You Need

You can't scale what you don't measure. In interviews, knowing **what to monitor** shows maturity.

```
CRAWL THROUGHPUT:
  Pages crawled per second (target: 10,000+)
  Tracked per: total system, per fetcher, per domain

URL FRONTIER DEPTH:
  How many URLs are waiting to be crawled
  If growing: crawlers too slow → scale up fetchers
  If shrinking: crawlers too fast → might be hitting politeness limits

FRESHNESS:
  Average age of crawled content
  "What % of our index was crawled in the last 24 hours?"
  Target: news sites < 1 hour, static content < 1 week

ERROR RATES:
  % of fetches resulting in: timeout, 404, 500, ban
  Spike in 429s → we're crawling too fast
  Spike in timeouts → network issue or server problems

STORAGE GROWTH:
  GB/day added to raw HTML store
  Helps forecast: "When do we need more storage?"

QUEUE LAG (Kafka):
  Difference between messages produced and consumed
  Consumer lag > 1M → fetchers falling behind → scale up
```

---

## 🏗️ The Complete Scaled Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                   DISTRIBUTED WEB CRAWLER                        │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              URL FRONTIER (Kafka)                       │    │
│  │  100 partitions × 3 replicas = 300 Kafka nodes         │    │
│  │  hash(domain)#shard → partition (hot domain sharding)  │    │
│  └───────────────────────┬─────────────────────────────────┘    │
│                          │                                       │
│  ┌───────────────────────▼─────────────────────────────────┐    │
│  │           FETCHER FLEET (Autoscaling)                   │    │
│  │  100-500 machines (scales with queue depth)             │    │
│  │  Stateless, async I/O, 500 concurrent req each          │    │
│  │  Adaptive rate limiting per domain                      │    │
│  └────────┬──────────────────────────────┬─────────────────┘    │
│           │                              │                       │
│  ┌────────▼──────────┐      ┌────────────▼──────────────┐       │
│  │  DEDUP LAYER      │      │      CONTENT PIPELINE     │       │
│  │  Redis Cluster    │      │  Parser → Simhash →       │       │
│  │  (Bloom Filter)   │      │  Elasticsearch            │       │
│  │  Virtual nodes    │      │  (sharded + replicated)   │       │
│  └────────┬──────────┘      └───────────────────────────┘       │
│           │                                                      │
│  ┌────────▼──────────────────────────────────────────────┐      │
│  │           METADATA STORE (Cassandra)                  │      │
│  │  Virtual nodes, RF=3, quorum reads/writes             │      │
│  │  Automatic rebalancing on node add/remove             │      │
│  └───────────────────────────────────────────────────────┘      │
└──────────────────────────────────────────────────────────────────┘
```

---

## 📊 Summary — Scaling Toolbox

| Problem | Solution |
|---|---|
| Need more crawl throughput | Add stateless fetcher machines (autoscaling) |
| Kafka bottleneck | Add partitions, consistent hashing for assignment |
| Database read bottleneck | Read replicas, spread reads across all replicas |
| Hot spot domains | Virtual nodes + domain sharding + adaptive rate limiting |
| Thundering herd on restart | Staggered startup + jitter + cache warming |
| Uneven load distribution | Virtual nodes on consistent hash ring |
| Wasted re-crawls | Conditional HTTP (304 Not Modified) |

---

## 🎯 Likely Interview Questions on This

- *"How does horizontal scaling differ from vertical scaling?"* → Cost, failure modes, limits
- *"How do you handle hot spots in a distributed system?"* → Virtual nodes, domain sharding, adaptive throttling
- *"Explain consistent hashing and why it's useful for scaling"* → Hash ring, minimal redistribution on node change
- *"What is the thundering herd problem?"* → Mass restart → simultaneous requests → cascade failure
- *"How do you scale Kafka?"* → Partitioning, consumer groups, each partition = one consumer
- *"How do you know when to scale?"* → Metrics — queue depth, consumer lag, response times

---

You now understand how to grow the system gracefully and handle uneven load without collapsing.

But we've been assuming our system is one giant cluster. What if you need to crawl the **entire global internet** from multiple locations? Or your system needs to serve search results in **milliseconds** to users in Tokyo, London, and São Paulo simultaneously?

That's **Chapter 7: Multi-Region Deployment & Politeness at Global Scale** — where we ask: *"How do you run this thing across continents?"*

Ready? 🚀
---
# 🌍 Chapter 7: Multi-Region Deployment — "Running This Thing Across Continents"

---

## The Story So Far

Your crawler is fast, fault-tolerant, and scales beautifully. You're crawling millions of pages per day from your data center in Virginia, USA.

Then three problems walk in together:

**Problem 1 — Your lawyer calls:**
> *"Hey, we're crawling European websites from the US. The round-trip latency is 150ms per request. We're 6x slower on European sites than American ones."*

**Problem 2 — A geography lesson:**
> *"Did you know 60% of the world's websites are hosted in Asia and Europe? You're crawling them from 10,000 miles away."*

**Problem 3 — A compliance officer calls:**
> *"GDPR says user data from European users must stay in Europe. If we're crawling and storing European web content in Virginia, we might have a legal problem."*

One data center isn't enough. You need to go **global.**

But the moment you do, you face the hardest problem in distributed systems:

> *"How do you keep multiple data centers — on different continents, connected by unreliable undersea cables — in sync with each other?"*

---

## First — Why Is Multi-Region Hard?

On a single machine, everything happens in nanoseconds. Across a data center, microseconds. But across continents:

```
SPEED OF LIGHT PROBLEM:

Virginia (USA) → London (UK):
  Physical distance: ~5,500 km
  Speed of light in fiber: ~200,000 km/s
  Minimum round-trip time: ~55ms
  Real-world RTT (with routing): ~80-100ms

Virginia (USA) → Tokyo (Japan):
  Physical distance: ~10,800 km
  Minimum round-trip time: ~108ms
  Real-world RTT: ~150-180ms

You cannot make this faster.
Physics is not negotiable.
```

This latency means:
- A database write in Virginia takes 100ms to reach Tokyo
- During those 100ms, Tokyo might accept a conflicting write
- Now you have two conflicting "truths" on different continents

This is the **distributed consistency problem** at its hardest.

---

## The Architecture Decision — Active-Passive vs Active-Active

Before building anything, you make one fundamental choice:

### Option A — Active-Passive (One region does the work)

```
┌─────────────────────────────────────────────────────┐
│                                                     │
│   VIRGINIA (Active) ──── replicates ────► LONDON   │
│   "Does all the work"                   (Passive)  │
│                                         "Standby"  │
│                                                     │
│   VIRGINIA ──── replicates ────► TOKYO             │
│                                  (Passive)          │
│                                  "Standby"          │
└─────────────────────────────────────────────────────┘

Normal operation: Virginia handles everything
If Virginia dies: Promote London to Active (failover)
```

**Pros:** Simple. No consistency conflicts. One source of truth.

**Cons:** European websites still crawled from Virginia (slow). London and Tokyo sit idle — wasted money.

---

### Option B — Active-Active (Every region does work)

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│   VIRGINIA ◄──── sync ────► LONDON ◄──── sync ────►   │
│   "Crawls                   "Crawls                    │
│    American                  European                  │
│    websites"                 websites"                 │
│                                                         │
│                    TOKYO                               │
│                   "Crawls                              │
│                    Asian                               │
│                    websites"                           │
└─────────────────────────────────────────────────────────┘

All regions active simultaneously.
Each region crawls geographically close websites.
Data is shared across regions.
```

**Pros:** Low latency for all regions. Resources fully utilized. Resilient — any region can absorb others' load.

**Cons:** Complex. Consistency nightmares. Conflicts are constant.

For a web crawler, **Active-Active is the right choice** — but with careful boundaries. Let's build it.

---

## The Key Insight — Partition Responsibility by Geography

The secret to making Active-Active work for a crawler: **each region owns a subset of domains, exclusively.**

```
DOMAIN OWNERSHIP:

Virginia (US-East):
  Owns: .com, .net, .org, .edu, .gov (US-registered domains)
  Crawls: amazon.com, facebook.com, wikipedia.org...

London (EU-West):
  Owns: .co.uk, .de, .fr, .eu, .nl, .se... (European TLDs)
  Crawls: bbc.co.uk, spiegel.de, lemonde.fr...

Tokyo (Asia-Pacific):
  Owns: .jp, .cn, .in, .au, .kr, .sg... (APAC TLDs)
  Crawls: asahi.com, baidu.com, timesofindia.com...
```

Now each region has **exclusive write ownership** of its domains:

```
WRITE RULE:
  Virginia NEVER writes crawl data for bbc.co.uk
  London NEVER writes crawl data for amazon.com
  
  This eliminates write conflicts entirely.
  ← The most important architectural decision in multi-region design
```

But reads can come from anywhere:

```
READ RULE:
  Virginia needs a link from bbc.co.uk?
  → Reads London's data (cross-region read, ~80ms)
  → OR London replicates its data to Virginia (local read, ~1ms)
  
  We choose: REPLICATION (data flows to all regions)
  Tradeoff: slightly stale data, but fast reads everywhere
```

---

## Data Replication Across Regions

### What Gets Replicated and How

Not all data needs to be everywhere. You classify data by access pattern:

```
┌──────────────────────────────────────────────────────────────┐
│              REPLICATION STRATEGY PER DATA TYPE              │
│                                                              │
│  URL Seen Store (Bloom Filter)                               │
│  → FULLY REPLICATED to all regions                          │
│  → Why: Every region needs to know what's been crawled      │
│  → How: Gossip protocol syncs every 60 seconds              │
│  → Staleness OK: might re-crawl a URL once, not a disaster  │
│                                                              │
│  Crawl Metadata (Cassandra)                                  │
│  → REPLICATED with eventual consistency                     │
│  → Each region owns its domains, replicates to others       │
│  → ~5 minute lag acceptable                                 │
│                                                              │
│  Raw HTML (Object Storage)                                   │
│  → STORED LOCALLY only (too big to replicate everywhere)    │
│  → Cross-region accessible via URL if needed                │
│  → Exception: EU data stays in EU (GDPR compliance)         │
│                                                              │
│  URL Frontier (Kafka)                                        │
│  → NOT REPLICATED (each region has its own frontier)        │
│  → Virginia's queue: US domains only                        │
│  → London's queue: EU domains only                          │
│  → Complete separation = zero conflicts                     │
└──────────────────────────────────────────────────────────────┘
```

---

### The Gossip Protocol — How Nodes Stay In Sync

How does Virginia know what London has crawled, without a central coordinator?

**Gossip protocol** — named after how humans spread gossip:

```
ROUND 1 (t=0):
  London Node L1 has new info: "crawled bbc.co.uk at 10:00:01"
  L1 randomly picks 3 nodes to tell:
  → tells L2, L3, Virginia-V1

ROUND 2 (t=1 second):
  L2, L3, V1 each received the info
  Each randomly picks 3 nodes to tell:
  L2  → tells L4, Tokyo-T1, V2
  L3  → tells L5, V3, T2
  V1  → tells V2, V4, T3

ROUND 3 (t=2 seconds):
  Now 9 more nodes know...

After ~log(N) rounds: EVERY node in the cluster knows
For 1000 nodes: ~10 rounds = ~10 seconds to full propagation
```

```
WHY GOSSIP OVER CENTRAL BROADCAST?

Central broadcast:
  L1 → sends to all 999 other nodes → network explosion
  L1 becomes bottleneck
  L1 crashes → propagation stops

Gossip:
  Load distributed across all nodes
  No single bottleneck
  Even if half the nodes crash, info still spreads
  Scales to thousands of nodes effortlessly
```

Cassandra uses gossip protocol for exactly this — nodes continuously whisper to each other about the state of the ring.

---

## Handling the Hardest Case — Cross-Region URL Discovery

Here's a tricky scenario. London is crawling `bbc.co.uk` and finds a link to `amazon.com`. Amazon is Virginia's domain. What happens?

```
NAIVE APPROACH (wrong):
  London → adds amazon.com/new-page to London's frontier
  London crawls amazon.com/new-page (from Europe, slow)
  Virginia also discovers amazon.com/new-page
  Virginia adds it to Virginia's frontier
  Virginia crawls it too
  ← DUPLICATE CRAWL across regions
  ← POLITENESS VIOLATED (two regions hitting Amazon)

CORRECT APPROACH:
  London → discovers amazon.com/new-page
  London checks: "amazon.com is a .com domain → owned by Virginia"
  London → sends URL to Virginia's Kafka frontier (cross-region message)
  Virginia → crawls it from US (fast, polite)
  London → moves on
```

This requires a **cross-region URL router:**

```
┌─────────────────────────────────────────────┐
│           CROSS-REGION URL ROUTER           │
│                                             │
│  Input: Any discovered URL                  │
│                                             │
│  Logic:                                     │
│  1. Extract TLD from URL                    │
│  2. Look up ownership table:                │
│     .com/.net/.org → Virginia               │
│     .co.uk/.de/.fr → London                 │
│     .jp/.cn/.in    → Tokyo                  │
│                                             │
│  3. If URL belongs to MY region:            │
│     → Add to local Kafka frontier           │
│                                             │
│  4. If URL belongs to ANOTHER region:       │
│     → Send to that region's Kafka frontier  │
│     → Via cross-region message queue        │
└─────────────────────────────────────────────┘
```

---

## Politeness at Global Scale

Politeness gets complicated when multiple regions might crawl the same domain.

Consider `google.com` — it has servers all over the world. Different subdomains might be "owned" by different regions:

```
google.com      → Virginia (US)
google.co.uk    → London (EU)
google.co.jp    → Tokyo (APAC)
```

But they're all Google's servers. If Virginia crawls google.com aggressively AND London crawls google.co.uk aggressively, Google sees combined hammering from both.

**Solution: Global Politeness Registry**

```
A lightweight, globally-replicated service that tracks:

{
  "google.com (and all subdomains)": {
    global_crawl_rate: 10/second,   ← total across ALL regions
    virginia_allocation: 4/second,
    london_allocation: 3/second,
    tokyo_allocation: 3/second,
    last_updated: "2024-01-15T10:00:01Z"
  }
}

Before any region crawls a Google URL:
  Check: "Is my region's rate under its allocation?"
  If yes → crawl
  If no  → wait

This is stored in a globally-replicated key-value store
with strong consistency (uses Paxos/Raft consensus)
← One of the few places we need strong consistency
```

---

## Consensus Protocols — Paxos and Raft

"Strong consistency" means every region agrees on the same value. Achieving this across continents requires a **consensus protocol.**

This is where **Raft** comes in (the more understandable version of Paxos).

### How Raft Works — The Story

Imagine 5 nodes (N1-N5) need to agree on a value. One is elected **Leader.**

```
NORMAL OPERATION:

Client: "Set global_rate for google.com = 10/sec"
  
  → Request goes to Leader (N1)
  → N1 writes to its own log: [Entry 47: google.com = 10/sec]
  → N1 sends to all followers: "Please write Entry 47"
  → N2, N3, N4, N5 write it to their logs
  → N2, N3, N4 send ACK to N1 (N5 is slow, hasn't responded yet)
  → N1 has majority (3 of 4 followers = quorum)
  → N1 commits: "Entry 47 is official"
  → N1 tells client: "Done"
  → N1 tells followers: "Entry 47 is committed"
  → N5 eventually catches up

KEY RULE: Committed = majority agreed. Minority being slow is fine.
```

```
LEADER ELECTION (when leader dies):

N1 (leader) crashes.

N2, N3, N4, N5 notice: "No heartbeat from N1 for 150ms"

Each starts election timer (random duration to avoid tie):
  N3's timer fires first (83ms)
  N3 → "I want to be leader! Vote for me!"
  N2, N4, N5 check: "Is N3's log at least as up-to-date as mine?"
  → Yes → "I vote for N3"
  
  N3 gets 3 votes → majority → becomes leader
  
  Time to new leader: ~150-300ms
  During this time: writes are briefly paused
  After election: writes resume, system continues
```

```
WHY DOES THIS MATTER FOR THE CRAWLER?

The Global Politeness Registry uses Raft.
5 nodes, 1 in each major region:
  N1: Virginia
  N2: London  
  N3: Tokyo
  N4: Singapore
  N5: São Paulo

"google.com rate = 10/sec" must be agreed by 3 of 5 nodes.
Even if Virginia-London link is slow → Singapore + Tokyo + São Paulo form quorum.
System keeps working.
```

---

## Failure Scenarios — What Happens When a Region Dies

### Scenario 1: Network Partition Between Regions

```
Virginia ←——— CABLE CUT ———→ London
                                ↕ (still connected)
                              Tokyo

Virginia and London cannot communicate.
Tokyo can reach both.

WHAT HAPPENS:
  Virginia → continues crawling .com/.net/.org (its domains)
  London   → continues crawling .co.uk/.de/.fr (its domains)
  
  Cross-region URL routing temporarily broken:
  London discovers amazon.com → can't send to Virginia
  → Buffers in local queue: "send to Virginia when reconnected"
  
  Tokyo → acts as relay:
  London → Tokyo → Virginia (indirect path)
  
  Global Politeness Registry → Raft still has quorum (Tokyo can reach both)
  
  After cable restored:
  Virginia ← receives buffered URLs from London
  Both sides sync their metadata
  No data lost. ~5 minutes of duplicated crawling at worst.
```

### Scenario 2: Entire Region Goes Down

```
Tokyo data center → FIRE → completely offline

IMMEDIATE IMPACT:
  All .jp, .cn, .au websites → nobody crawling them

AUTOMATED RESPONSE (within 5 minutes):

1. Health check system detects Tokyo is down

2. Ownership rebalancing triggers:
   Virginia takes over: .au, .nz (English-language APAC)
   London takes over:   .jp, .kr (geographically, London is closer than Virginia)
   Singapore (backup):  .cn, .sg, .in

3. Virginia + London spin up extra fetcher machines (autoscaling)

4. Tokyo's Kafka frontier was replicated to Singapore (warm standby)
   → Singapore promoted to handle Tokyo's queue

5. Tokyo-owned data in Cassandra:
   → Still accessible (RF=3, copies in all regions)
   → Virginia and London can read Tokyo's historical crawl data

RECOVERY TIME OBJECTIVE: < 10 minutes
DATA LOSS: Zero (everything was replicated)
```

---

## The Global Architecture — Full Picture

```
┌─────────────────────────────────────────────────────────────────┐
│                    GLOBAL CRAWLER NETWORK                       │
│                                                                 │
│  ┌─────────────────┐    ┌─────────────────┐                    │
│  │  VIRGINIA (US)  │    │  LONDON (EU)    │                    │
│  │                 │    │                 │                    │
│  │ Owns: .com .net │◄──►│ Owns: .uk .de  │                    │
│  │ Fetchers: 200   │    │ .fr .eu .nl     │                    │
│  │ Kafka: US URLs  │    │ Fetchers: 150   │                    │
│  │ Cassandra node  │    │ Kafka: EU URLs  │                    │
│  │ S3: US content  │    │ S3: EU content  │                    │
│  │                 │    │   (GDPR zone)   │                    │
│  └────────┬────────┘    └────────┬────────┘                    │
│           │                      │                             │
│           │   Cross-region       │                             │
│           │   URL routing        │                             │
│           │   + Gossip sync      │                             │
│           │                      │                             │
│           └──────────┬───────────┘                             │
│                      │                                         │
│             ┌────────▼────────┐                               │
│             │  TOKYO (APAC)   │                               │
│             │                 │                               │
│             │ Owns: .jp .cn   │                               │
│             │ .in .au .kr     │                               │
│             │ Fetchers: 180   │                               │
│             │ Kafka: APAC URLs│                               │
│             │ S3: APAC content│                               │
│             └─────────────────┘                               │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │         GLOBAL SERVICES (Raft consensus, 5 nodes)       │  │
│  │  - Politeness Registry (per-domain global rate limits)  │  │
│  │  - Domain Ownership Table (which region owns what TLD)  │  │
│  │  - System Health Dashboard                              │  │
│  └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Compliance — GDPR and Data Residency

One more real-world concern that interviewers increasingly ask about:

```
GDPR RULE: Personal data of EU residents must stay in the EU.

For a web crawler, this means:
  Raw HTML from EU websites → stored ONLY in London's S3
  Never copied to Virginia or Tokyo

IMPLEMENTATION:

  London fetcher crawls bbc.co.uk:
    → Stores raw HTML in London S3 bucket (EU-WEST-1)
    → Metadata (URL, timestamp, status) replicated globally ✓
    → Raw HTML: EU-only flag set, NEVER leaves EU region

  Virginia needs bbc.co.uk content:
    → Cannot get the raw HTML (EU-only)
    → Can get extracted text/metadata (not personal data, OK to replicate)
    → If needs raw HTML → must make cross-region API call to London

  Data tagging:
  {
    url: "bbc.co.uk/news/article",
    region_owner: "EU",
    data_residency: "EU-ONLY",     ← flag that prevents cross-region copy
    gdpr_applicable: true
  }
```

---

## 📊 Summary — Multi-Region Toolbox

| Problem | Solution |
|---|---|
| Latency to distant websites | Active-Active, each region crawls nearby sites |
| Write conflicts across regions | Domain ownership — each region exclusively owns its TLDs |
| Keeping regions in sync | Gossip protocol for eventual consistency |
| Cross-region URL handoff | Cross-region URL router, buffered queues |
| Global politeness enforcement | Global Politeness Registry with Raft consensus |
| Region goes down | Domain ownership rebalancing + warm standby |
| Network partition | Buffer + relay + auto-recovery on reconnect |
| Data compliance (GDPR) | Data residency flags, raw HTML never leaves EU |

---

## 🎯 Likely Interview Questions on This

- *"How would you deploy this crawler globally?"* → Active-Active, domain ownership partitioning, gossip sync
- *"How do you handle a network partition between data centers?"* → Buffer cross-region URLs, Raft for consensus services, regions continue independently
- *"Explain Raft/Paxos in simple terms"* → Leader election, quorum writes, majority agreement = committed
- *"How do you ensure politeness when multiple regions might crawl same domain?"* → Global Politeness Registry with Raft consensus, allocated rates per region
- *"What is GDPR and how does it affect your design?"* → Data residency, EU content stays in EU, metadata vs raw content distinction

---

We've now covered the full distributed web crawler — from a single loop on a laptop all the way to a globally distributed, fault-tolerant, auto-scaling system spanning three continents.

**Chapter 8 will be our final chapter — the Capstone.** We'll zoom out and do what interviewers love most: a complete **end-to-end system walkthrough**, covering how all pieces talk to each other, what numbers to quote confidently (throughput, storage, latency estimates), and the **top 10 interview questions** with crisp, confident answers.

Ready for the finale? 🚀

---
# 🎓 Chapter 8: The Capstone — "Ace the Interview"

---

## The Story Comes Full Circle

You started with one person, one laptop, one loop.

```python
while True:
    url = queue.pop()
    html = fetch(url)
    links = parse(html)
    queue.extend(links)
```

Seven chapters later, you've built a system that:
- Crawls **billions of pages** across three continents
- Survives **machine failures, region outages, and network partitions**
- Respects every website's **politeness rules** automatically
- **Never loses a URL**, even through crashes
- Scales from 100 to 10,000 machines **without redesigning anything**

Now let's make sure you can **walk into an interview and own this topic.**

---

## Part 1: The Complete System — One Final Walkthrough

Let's trace exactly what happens when your crawler discovers a brand new URL — say, a breaking news article on `bbc.co.uk/news/election-2024` — from the moment it's found to the moment it's in the search index.

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 1: URL DISCOVERY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

London fetcher is crawling bbc.co.uk/news (homepage)
Parser extracts link: bbc.co.uk/news/election-2024

                    ↓

STEP 2: CROSS-REGION ROUTING CHECK
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

URL Router: "bbc.co.uk → .co.uk TLD → owned by London ✓"
→ Stay in London. No cross-region handoff needed.

                    ↓

STEP 3: URL DEDUPLICATION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Normalize URL:
  bbc.co.uk/news/election-2024?ref=homepage
  → bbc.co.uk/news/election-2024  (remove tracking param)

Check Bloom Filter (Redis, local to London):
  hash1(url) → bit 0? No → bit is 1
  hash2(url) → bit 0? No → bit is 1
  hash3(url) → bit 0? No → bit is 0  ← ZERO FOUND
  
  → "Definitely not seen before" → proceed

                    ↓

STEP 4: PRIORITIZATION → URL FRONTIER
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Prioritizer scores URL:
  Domain authority of bbc.co.uk: 9.4/10
  Keyword "election" in path: +priority boost
  URL never crawled before: neutral
  → Priority Score: HIGH

→ Added to Front Queue 1 (High Priority)
→ Mapped to Back Queue for bbc.co.uk
→ Written to Kafka Partition 34 (hash("bbc.co.uk") % 100)
  With replication factor 3: also on Partitions 35, 36

                    ↓

STEP 5: POLITENESS CHECK
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Timing heap check:
  bbc.co.uk last crawled: 10:00:01.000
  bbc.co.uk robots.txt crawl-delay: 1 second
  Next allowed crawl: 10:00:02.000
  Current time: 10:00:01.800
  → Wait 200ms

  Global Politeness Registry check:
  bbc.co.uk global limit: 5 req/sec
  London's allocation: 2 req/sec
  Current London rate: 1.8 req/sec → under limit ✓

                    ↓

STEP 6: FETCHING
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

At 10:00:02.000, fetcher picks up URL from Kafka.

DNS lookup:
  Cache check: bbc.co.uk → 151.101.1.200 (cached, TTL: 3200s left) ✓
  → Skip DNS query, use cached IP

HTTP Request:
  GET /news/election-2024 HTTP/1.1
  Host: bbc.co.uk
  If-Modified-Since: [not set, first crawl]
  Connection-Timeout: 5s
  Read-Timeout: 30s

Response:
  HTTP 200 OK
  Content-Type: text/html
  Last-Modified: Mon, 15 Jan 2024 09:58:00 GMT
  ETag: "election2024-v1"
  Content-Length: 48291 bytes

  → Response received in 85ms (London → BBC London CDN)
  → Mark Kafka message as acknowledged
     (URL officially "processed", won't be retried)

                    ↓

STEP 7: CONTENT DEDUPLICATION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Compute Simhash of HTML content:
  simhash = 1001011010110010110101001011...

Check against Simhash store:
  Query banding index: no match within Hamming distance 3
  → "Unique content" → proceed to parse

                    ↓

STEP 8: PARSING & EXTRACTION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Parser extracts:
  Title: "UK Election 2024: Results as they happen"
  Content: "Polls have closed across the United Kingdom..."
  Language: "en-GB"
  Published: "2024-01-15T22:00:00Z"
  New links found: 23 URLs (bbc.co.uk/..., twitter.com/..., etc.)

New links → back to Step 2 (for each link)

                    ↓

STEP 9: STORAGE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Three writes happen in parallel:

  A) Raw HTML → London S3 (EU-ONLY flag, GDPR compliant)
     Key: "crawl/2024/01/15/co.uk.bbc/news/election-2024/raw.html.gz"
     Size: 48KB compressed to ~11KB

  B) Crawl Metadata → Cassandra (London node, RF=3)
     {
       url: "bbc.co.uk/news/election-2024",
       first_crawled: "2024-01-15T10:00:02Z",
       last_modified: "2024-01-15T09:58:00Z",
       etag: "election2024-v1",
       http_status: 200,
       simhash: "1001011010...",
       region_owner: "EU"
     }
     Replicated via gossip to Virginia + Tokyo within 60s

  C) Extracted Content → Elasticsearch (London cluster)
     {
       url: "bbc.co.uk/news/election-2024",
       title: "UK Election 2024: Results as they happen",
       content: "Polls have closed across the United Kingdom...",
       domain_authority: 9.4,
       crawled_at: "2024-01-15T10:00:02Z"
     }
     → Now searchable in the index

  D) Bloom Filter → updated in Redis
     Set bits for bbc.co.uk/news/election-2024
     → Synced to other regions within 60s

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL TIME: ~200ms from URL discovery to fully indexed
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Part 2: The Numbers — Back-of-Envelope Calculations

Interviewers **love** when you pull out numbers confidently. Here's exactly what to say.

---

### Scale Requirements (establish these first)

```
Target index size:    5 billion pages
Recrawl frequency:    Important pages every 24h, others weekly
Crawl throughput:     ~50,000 pages/second (to crawl 5B in ~28 hours)
```

---

### Fetcher Fleet Sizing

```
One fetcher machine:
  Async I/O: 500 concurrent requests
  Avg response time: 200ms
  Throughput per machine: 500 / 0.2s = 2,500 pages/sec

Machines needed:
  50,000 pages/sec ÷ 2,500 pages/machine = 20 machines

In practice: multiply by 3-4x for:
  - Failed requests, retries
  - JS rendering machines (10x slower)
  - Headroom for traffic spikes

Realistic fetcher fleet: ~100-200 machines
```

---

### Storage Sizing

```
RAW HTML STORE:
  Avg HTML page size: 200KB uncompressed
  After gzip compression: ~50KB
  5 billion pages × 50KB = 250 TB

  Growth rate:
  50,000 pages/sec × 50KB = 2.5 GB/sec = ~200 TB/day
  (recrawls of existing pages, not all new)
  
  Use: Object storage (S3), ~$5,000/month at AWS pricing

EXTRACTED CONTENT STORE:
  Avg extracted content: 10KB per page
  5 billion pages × 10KB = 50 TB
  Use: Elasticsearch cluster, ~50 nodes

URL METADATA (Cassandra):
  Avg metadata per URL: 500 bytes
  5 billion URLs × 500 bytes = 2.5 TB
  Use: Cassandra cluster, ~20 nodes (with RF=3 → 60 TB total storage)

BLOOM FILTER (Redis):
  ~10 bits per URL (1% false positive rate)
  5 billion URLs × 10 bits = 6.25 GB
  Comfortably fits in RAM on ONE Redis node
  Replicate to 3 nodes for fault tolerance

URL FRONTIER (Kafka):
  URLs queued at any time: ~100 million
  Avg URL size: 100 bytes
  100M × 100 bytes = 10 GB
  Kafka easily handles this across 10 partitions
```

---

### Bandwidth

```
INBOUND (downloading pages):
  50,000 pages/sec × 200KB avg = 10 GB/sec inbound bandwidth
  Per region (3 regions): ~3.3 GB/sec each
  ≈ 33 Gbps per region → needs multiple 10Gbps links

CROSS-REGION SYNC (metadata):
  Cassandra gossip: ~100 MB/sec per region pair
  Bloom filter sync: ~500 MB every 60 seconds = ~8 MB/sec
  Total cross-region: ~300 MB/sec → manageable
```

---

### Latency Targets

```
URL discovery → Kafka:           < 10ms
Politeness check:                < 5ms   (local Redis)
DNS lookup (cached):             < 1ms
HTTP fetch (same region):        50-200ms
HTTP fetch (cross-region):       100-400ms
Parsing + deduplication:         < 50ms
Storage writes (all parallel):   < 100ms

Total end-to-end (happy path):   ~300-500ms per page
```

---

## Part 3: The Complete Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                    DISTRIBUTED WEB CRAWLER                          │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                      URL FRONTIER                           │   │
│  │                                                             │   │
│  │  ┌────────────────┐      ┌────────────────────────────┐    │   │
│  │  │  Front Queues  │      │      Back Queues            │    │   │
│  │  │  (Priority)    │─────►│  (Per-domain, Politeness)  │    │   │
│  │  │  High/Med/Low  │      │  + Timing Heap             │    │   │
│  │  └────────────────┘      └─────────────┬──────────────┘    │   │
│  │                                         │                   │   │
│  │              Kafka (100 partitions, RF=3, domain-sharded)   │   │
│  └─────────────────────────────────────────┬───────────────────┘   │
│                                            │                        │
│  ┌─────────────────────────────────────────▼───────────────────┐   │
│  │                    FETCHER FLEET                            │   │
│  │                                                             │   │
│  │  100-500 stateless machines (autoscaling)                  │   │
│  │  Each: async I/O, 500 concurrent, DNS cache                │   │
│  │  Tier 1: HTTP fetcher  │  Tier 2: Headless Chrome          │   │
│  │  Timeout: conn(5s) + read(30s) + total(2min)               │   │
│  │  Retry: exponential backoff, circuit breaker               │   │
│  └──────────────────┬──────────────────────────────────────────┘   │
│                     │                                               │
│  ┌──────────────────▼──────────────────────────────────────────┐   │
│  │                  PROCESSING PIPELINE                        │   │
│  │                                                             │   │
│  │  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐  │   │
│  │  │ URL Dedup   │  │Content Dedup │  │     Parser       │  │   │
│  │  │(Bloom/Redis)│  │(Simhash+Band)│  │Link + Content    │  │   │
│  │  └─────────────┘  └──────────────┘  └────────┬─────────┘  │   │
│  └──────────────────────────────────────────────┼─────────────┘   │
│                                ┌─────────────────┘                  │
│              ┌─────────────────┼──────────────────┐                │
│              ▼                 ▼                  ▼                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐       │
│  │  New URLs Back  │  │  Raw HTML Store │  │   Content    │       │
│  │  to Frontier    │  │  (S3, 250TB)    │  │   Store      │       │
│  │                 │  │  EU-ONLY flag   │  │(Elasticsearch│       │
│  └─────────────────┘  └─────────────────┘  │   50TB)      │       │
│                                             └──────────────┘       │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                    METADATA LAYER                            │  │
│  │  Cassandra (RF=3, virtual nodes, leaderless, quorum W+R>N)  │  │
│  │  URL status, crawl history, ETag, Simhash index             │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │              GLOBAL COORDINATION (Raft, 5 nodes)            │  │
│  │  Politeness Registry │ Domain Ownership │ Health Monitor     │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
         Virginia (US)        London (EU)         Tokyo (APAC)
         .com .net .org       .uk .de .fr          .jp .cn .in
```

---

## Part 4: Top 10 Interview Questions — With Crisp Answers

These are the questions that actually get asked. Read each answer until it feels natural to say out loud.

---

**Q1: "Design a distributed web crawler."**

> *"Let me start with requirements. We want to crawl ~5 billion pages, recrawl important ones daily, hit ~50,000 pages/second throughput, and respect robots.txt. The system has five core components: URL Frontier, Fetcher Fleet, Processing Pipeline, Deduplication Layer, and Storage. Let me walk through each..."*

Then use our story structure — URL Frontier → Fetchers → Dedup → Storage. You now know all of it cold.

---

**Q2: "How do you avoid crawling the same page twice?"**

> *"Two layers. First, URL deduplication — before adding any URL to the frontier, we check a Bloom Filter stored in Redis. It holds 5 billion URLs in ~6GB of RAM with ~1% false positive rate. False positives just mean we occasionally skip a URL we could have crawled — acceptable tradeoff. Second, content deduplication — even with unique URLs, the internet has near-duplicates. We compute a Simhash for every page's content. Similar pages produce similar hashes, measurable by Hamming distance. We use banding to find near-duplicates efficiently across billions of stored hashes."*

---

**Q3: "How do you handle a machine crashing mid-crawl?"**

> *"Stateless fetcher design is the key. Fetchers store nothing locally — no state in RAM, no data on disk. Every URL comes from Kafka, which uses at-least-once delivery. A URL is only acknowledged in Kafka after the page is fully fetched, parsed, and stored. If a fetcher crashes mid-crawl, Kafka sees no acknowledgment, and the URL is automatically reassigned to another fetcher. Zero data loss, zero special recovery logic."*

---

**Q4: "What is consistent hashing and why do you use it?"**

> *"Consistent hashing places both machines and data keys on a circular ring using hash functions. Each URL is assigned to the nearest machine clockwise on the ring. The power is in scaling: when you add or remove a machine, only the keys near that machine on the ring get reassigned — typically 1/N of total keys, not all of them. We use it for Kafka partition assignment, Cassandra node assignment, and routing domains to fetcher machines. Without it, adding one machine would force reshuffling every URL."*

---

**Q5: "Explain CAP theorem."**

> *"CAP says a distributed system can only guarantee two of three: Consistency, Availability, Partition Tolerance. But since network partitions always happen in practice, the real choice is: when a partition occurs, do you choose Consistency or Availability? In our crawler, we make this choice per component. The URL Frontier and metadata store choose Availability — we'd rather process a URL twice than be unavailable. The Global Politeness Registry chooses Consistency via Raft consensus — every region must agree on crawl rates, or we risk hammering servers."*

---

**Q6: "What is a spider trap? How do you handle it?"**

> *"A spider trap is a website that generates infinite unique URLs — like calendar pages, session IDs, or infinite scroll parameters. Your crawler keeps discovering new URLs forever and gets stuck. We handle it with four layers: URL normalization (strip tracking params, sort query strings — so ?ref=home and ?ref=footer map to the same URL), per-domain URL caps (never crawl more than 10,000 URLs from one domain), path depth limits (skip URLs more than 5 directories deep), and content fingerprinting (if the content is the same as another URL, skip it regardless)."*

---

**Q7: "How does your system scale when load increases 10x?"**

> *"Each component scales independently. Fetchers are stateless — spin up more machines, they automatically start consuming from Kafka. No configuration changes needed; we use autoscaling triggered by Kafka consumer lag. Kafka scales by adding partitions — each partition handles a fraction of URLs and can be consumed by a separate machine. Cassandra scales by adding nodes to the consistent hash ring — only 1/N of data migrates. The Bloom Filter in Redis is 6GB and fits in one machine, but we can shard it by URL prefix if needed. The key is that nothing shares state, so nothing needs coordination to scale."*

---

**Q8: "How do you ensure politeness across hundreds of machines?"**

> *"Two-level politeness. At the local level, each fetcher's URL Frontier has a per-domain back queue with a timing heap — we track when each domain was last hit and enforce the crawl-delay from robots.txt. Since we use consistent hashing to assign domains to machines, each domain is handled by exactly one machine, so there's no coordination needed for local politeness. At the global level, for multi-region deployments, we have a Global Politeness Registry backed by Raft consensus — each region gets a rate allocation per domain, preventing combined cross-region hammering of the same servers."*

---

**Q9: "How would you prioritize which URLs to crawl first?"**

> *"We use a two-layer priority queue system. The front queues have three tiers — high, medium, low — assigned by a Prioritizer that scores URLs on PageRank of the source page, domain authority, keyword signals in the URL path, and freshness of the domain. Selection is weighted random: 60% from high priority, 30% medium, 10% low — ensuring low-priority URLs eventually get crawled. Within each domain, we further prioritize by last-modified time and how frequently the domain publishes new content. News sites get recrawled hourly; static reference sites weekly."*

---

**Q10: "What happens if an entire data center goes down?"**

> *"Since we partition by domain ownership — each region owns specific TLDs exclusively — losing a region means those TLDs temporarily stop being crawled. The automated response: health checks detect the outage in ~30 seconds, trigger domain ownership rebalancing where surviving regions split the dead region's TLDs, and autoscaling spins up extra fetcher capacity to absorb the load. The dead region's Kafka frontier was replicated as a warm standby. Cassandra data is accessible because it's replicated across all regions with RF=3. Recovery time objective is under 10 minutes, and data loss is zero."*

---

## Part 5: The Concepts Map — Everything You Now Know

```
DISTRIBUTED WEB CRAWLER
│
├── URL FRONTIER
│   ├── Front Queues (Priority — weighted random selection)
│   ├── Back Queues (Politeness — per-domain timing heap)
│   ├── Consistent Hashing (domain → machine assignment)
│   └── Kafka (distributed, durable, replayable queue)
│
├── FETCHER FLEET
│   ├── Stateless design (crash-safe, trivially scalable)
│   ├── Async I/O (500 concurrent per machine)
│   ├── Three-layer timeouts (conn + read + total)
│   ├── Exponential backoff (retry with increasing delay)
│   ├── Spider trap defenses (normalization, depth limits)
│   ├── DNS caching (avoid repeated lookups)
│   ├── Circuit breakers (fail fast, prevent cascades)
│   └── Two-tier fetching (HTTP + Headless Chrome)
│
├── DEDUPLICATION
│   ├── Bloom Filter (URL dedup, 100x smaller than raw set)
│   ├── Simhash (content fingerprint, similar → similar hash)
│   ├── Hamming Distance (measure similarity)
│   └── Banding (find near-dupes fast, O(1) lookups)
│
├── STORAGE
│   ├── Kafka (frontier — high write throughput)
│   ├── Redis (bloom filter — sub-ms RAM access)
│   ├── Cassandra (metadata — leaderless, quorum, W+R>N)
│   ├── S3/Object Storage (raw HTML — petabyte scale, cheap)
│   └── Elasticsearch (content — full-text search, replicas)
│
├── FAULT TOLERANCE
│   ├── Replication (leader-follower vs leaderless)
│   ├── CAP Theorem (C vs A tradeoff per component)
│   ├── Checkpointing (save progress, resume after crash)
│   └── Circuit Breakers (Closed → Open → Half-Open)
│
├── SCALING
│   ├── Horizontal vs Vertical scaling
│   ├── Kafka partitioning (add partitions = add throughput)
│   ├── Cassandra virtual nodes (smooth rebalancing)
│   ├── Read replicas (multiply read throughput)
│   ├── Autoscaling (queue depth triggers machine add/remove)
│   ├── Hot spot solutions (virtual nodes, domain sharding)
│   ├── Adaptive rate limiting (throttle by server health)
│   └── Thundering herd prevention (jitter, staggered start)
│
└── MULTI-REGION
    ├── Active-Active (all regions crawl simultaneously)
    ├── Domain ownership (each region owns TLDs exclusively)
    ├── Gossip Protocol (eventual sync without central broker)
    ├── Cross-region URL routing (hand off to owning region)
    ├── Raft Consensus (agreement on global shared state)
    ├── Data residency (GDPR — EU data stays in EU)
    └── Region failover (ownership rebalancing + warm standby)
```

---

## Part 6: How to Structure Your Answer in an Interview

Interviewers aren't testing whether you memorize components. They're testing **how you think.** Here's the framework:

```
MINUTE 0-2: REQUIREMENTS CLARIFICATION
  "Before I design, let me clarify scope."
  
  Functional:
  - What scale? (5B pages? 500M?)
  - Recrawl frequency? (real-time news vs static content?)
  - What to store? (raw HTML? just links? full text?)
  - Any domain restrictions? (whole web vs specific sites?)
  
  Non-functional:
  - Consistency vs availability preference?
  - Latency requirements on the index?
  - Geographic distribution needed?

MINUTE 2-5: HIGH-LEVEL DESIGN
  Draw the 5 components and their connections.
  One sentence on what each does.
  "I'll go deep on whichever area you want."

MINUTE 5-15: DEEP DIVES (interviewer-directed)
  They'll say: "Tell me more about deduplication."
  → Bloom filters → Simhash → banding
  
  Or: "How do you scale this?"
  → Stateless fetchers → Kafka partitions → consistent hashing

MINUTE 15-20: TRADEOFFS + BOTTLENECKS
  Always volunteer your system's weaknesses:
  "The bottleneck I'm most worried about is..."
  "The tradeoff I made here was... because..."
  
  This signals senior-level thinking.
```

---

## The Single Most Important Thing

Every concept in this system exists to solve a specific problem. When you answer interview questions, **always connect the solution back to the problem:**

```
❌ Weak answer: "We use Bloom Filters for URL deduplication."

✓ Strong answer: "We need to check if a URL has been seen before,
  billions of times per day. A naive set would need 500GB of RAM.
  Bloom Filters solve this by storing only a fingerprint —
  ~6GB for 5 billion URLs — with a tradeoff of ~1% false positives,
  meaning we occasionally skip a crawlable URL. That's acceptable
  because our goal is coverage, not perfection."
```

The pattern: **Problem → Solution → Tradeoff → Why tradeoff is acceptable.**

That's what separates a candidate who memorized a system from one who understands it.

---

## You're Ready 🎓

You started knowing nothing about web crawlers. You now understand:

- Why they exist and why scale forces them to be distributed
- Every component, why it was built that way, and what problem it solves
- The tradeoffs behind every decision
- The numbers to quote confidently
- How to structure your thinking under pressure

The story approach wasn't just to make it interesting — it was to make it **stick.** When an interviewer asks about spider traps, you'll remember the website with infinite calendar URLs. When they ask about consistent hashing, you'll remember the day you added Machine 101 and everything reshuffled.

**Good luck. You've got this.** 🕷️
---

