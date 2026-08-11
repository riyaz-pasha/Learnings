# Design an IP Allowlist/Blocklist Service — The Story (narrative edition)

> **What this file is.** The reference file, `46-Design-an-IP-Allowlist-Blocklist-Service-FAANG-Guide.md`, is the one to recite from — requirements, API shapes, every trade-off table, the master cheat sheet. This file is a second way in: the same material as one continuous story, told in plain language. Engineers at a company keep hitting a wall, patch it, and the patch itself creates the next wall — until we land on the exact same design the reference file documents. The company, **Anchorly** (a B2B SaaS that sells enterprise access-control gateways — customers use it to restrict who can hit their admin dashboards and APIs), is fictional. But every wall it hits, and every fix it reaches for, is something a real, named system actually does: CIDR-based IP range matching, trie-based longest-prefix-match the way routers and firewalls have done it for decades, Cloudflare's edge firewall (which matches IP/CIDR rules at global scale), AWS Security Groups and WAF IP sets (a documented, real feature for CIDR-based allow/deny), and the very real, well-documented category of government sanctions feeds (like OFAC's SDN list) that are slow, rate-limited, and outside anyone's control. I'll say clearly, every time, whether a number is a documented fact or just a reasonable stand-in — tagged `[illustrative]`.

**The trigger phrases** for this whole topic: *"we block/allow traffic by IP or CIDR range,"* *"the list comes from a government or regulatory body we don't control,"* or *"the source is slow and unreliable but we still need an answer in milliseconds."* Keep one sentence in your head as you read: **the slow, external, rate-limited job of keeping the list up to date has to be a completely different system from the fast, internal, all-local job of answering "is this IP allowed right now" — and the second one must never wait on the first.** Everything below is just this one idea, arrived at the hard way, one broken assumption at a time.

---

## Chapter 1 — The list that grew legs

It's early days at Anchorly. A customer configures which IPs are allowed to hit their admin API, and the check is about as simple as it gets: an array of exact IP strings, stored as a JSON blob, looped over on every single request — `for ip in allowlist: if request_ip == ip: allow`. One of Anchorly's first big customers, a bank we'll call **Meridian**, starts with 40 registered IPs. Fine. Fast. Nobody thinks about it again.

Three years later, Meridian has 4,000 employees, many of them remote, and Anchorly's only way to grant access is "add this exact IP." Every new hire, every employee whose home internet provider rotates their address, every new office — all of it becomes a support ticket that ends with one more string appended to Meridian's array. The array reaches **3,800 entries.** Meridian's integration runs at 220 requests/sec. Anchorly's internal target for this one check — is this IP allowed — is under 5ms, because it's supposed to be the cheapest part of the whole request. At 3,800 entries, the linear scan alone now costs measured p99 latency of **24ms** `[illustrative]`, blowing that budget by nearly 5x before the rest of the request has even started. During a traffic spike (a product launch doubles Meridian's request rate to 440/sec), CPU on the shared gateway pool spikes and **2% of Meridian's requests start timing out** during the peak ten minutes.

```mermaid
sequenceDiagram
    participant Req as Request (Meridian, IP X)
    participant Gate as Anchorly gateway
    participant List as allowlist[3800] (JSON array)

    Req->>Gate: check IP X
    Gate->>List: compare X to entry 1... entry 2... entry 3...
    Note over Gate,List: still comparing at entry 2,900 of 3,800
    List-->>Gate: match found (or not) after scanning most of the array
    Gate-->>Req: ALLOW / BLOCK, ~24ms later
```

The obvious question: *why does checking "is this IP on the list" get slower just because the list got longer?* Because a plain array scan is **O(n)** — every single entry costs one more comparison, and there's no shortcut, no index, nothing smarter than "read the next one."

**The fix, and an analogy worth keeping:** swap the array for a **hash set.** Think of it like the difference between reading a phone book page by page versus looking a name up by its index — a hash set turns "compare against every entry" into "jump straight to the one bucket that entry would live in." Lookup drops from O(n) to effectively O(1), regardless of list size.

**New problem, visible within the same week:** a hash set is *exact-match only.* It can tell you "is this precise IP in the set," but it can't tell you "is this IP inside this /24 office network," and that's actually what customers want — Meridian doesn't want to register 4,000 individual employee IPs, they want to register "our office network" and "our VPN's exit range" as single entries. The hash set fix solves the speed problem for the wrong shape of data.

**How I'd say this in an interview:** "A linear scan over a growing list is the first thing that breaks, and the textbook fix is a hash set for O(1) exact-match lookup. But that only works if you're matching exact IPs — the moment customers want to allow a whole network range instead of one address at a time, a hash set stops being the right tool entirely."

---

## Chapter 2 — The badge that works for the whole building

The fix: let customers register **CIDR ranges** instead of individual IPs — `203.0.113.0/24` covers all 256 addresses in Meridian's office network in one entry. Meridian collapses their 3,800 exact IPs down to **40 CIDR ranges** — one per office, one per VPN exit block, one per cloud NAT gateway. A roughly **95x reduction** in list size, and the DHCP-churn ticket flow (employees' home IPs rotating) mostly disappears, because most employees now route through a company VPN range instead of registering their personal IP.

**The analogy:** a CIDR range is like an access badge that works for an entire building floor instead of one that only opens a single office door — register the floor once, and everyone on it is covered, no matter which desk they're sitting at today.

```mermaid
flowchart LR
    subgraph Before["3,800 individual door keys"]
        K1["IP: 203.0.113.5"]
        K2["IP: 203.0.113.6"]
        K3["... 3,798 more ..."]
    end
    subgraph After["40 floor badges"]
        B1["CIDR: 203.0.113.0/24\n(whole office)"]
        B2["... 39 more ranges ..."]
    end
    Before -.->|"collapse into ranges"| After
```

This works beautifully for Meridian's own list. But checking "does this range contain that IP" isn't a hash lookup anymore — it's bit math (compute the network address, compare against the range) that has to run against **every range**, one at a time, because ranges can't be hashed the way exact values can. Separately, Anchorly's security team wants a **shared, platform-wide** blocklist too — known VPN providers, Tor exit nodes, and data-center ASNs that get abused for credential stuffing — checked on *every* request for *every one* of Anchorly's ~1,200 customers, not just Meridian's own list. Ops keeps adding newly-discovered bad ranges to this shared list. Over 18 months it grows to **46,000 CIDR ranges.**

At Anchorly's total peak traffic — 8,000 requests/sec across all customers — running a naive per-range loop against 46,000 entries for the shared blocklist alone measures p99 latency of **12ms** `[illustrative]` for this one check, on top of whatever each customer's own smaller list costs. The 5ms internal budget is blown again, by a wider margin than Chapter 1.

**How I'd say this in an interview:** "CIDR ranges fix the 'one badge per range instead of per address' problem, and it's a huge win for list size. But you've traded an O(1) exact-match problem for an O(n) range-match problem, because you can't hash a range the same way — and once that list is shared platform-wide instead of per-customer, it grows fast enough that the linear scan itself becomes the bottleneck again."

---

## Chapter 3 — Twenty questions, walking one branch at a time

The wrong instinct here is "just make the hash set smarter." It doesn't work, for a simple reason: the data is genuinely **ranges**, not points, and IPv4 alone has 4.3 billion addresses — you cannot and should not enumerate every address in a /16 into a set just to hash it.

**The fix:** a **binary trie over the IP's bits** (a Patricia/radix trie), used for **longest-prefix-match**. This is exactly the structure real routers and firewalls have used for this exact problem for decades, and it's the same mechanism behind Cloudflare's edge firewall matching IP/CIDR rules at global scale, and AWS Security Groups and WAF IP sets, which are documented, real features built on this same idea.

**The analogy — and one worth keeping for the rest of this story:** think of it as **twenty questions, but each question is one bit of the address**, and you only ever walk down one branch of the tree, bit by bit, from the root. Every branch you pass that has a rule attached, you remember as "the best match so far." When you run out of bits, whichever remembered match was **most specific** — the longest chain of correct bits — wins.

```mermaid
flowchart TD
    A["IP as bits: 203.0.113.7\n11001011.00000000.01110001.00000111"] --> B["Walk the trie\nbit by bit from the root"]
    B --> C{"Rule attached\nat this node?"}
    C -->|"yes"| D["Remember as current\nbest (longest) match"]
    C -->|"no"| E["Step to next bit's child"]
    D --> E
    E --> F["Out of bits, or\nno child exists"]
    F --> G["Return the LONGEST\nremembered match"]
```

**Why "longest" has to win:** the shared blocklist has `203.0.0.0/16` tagged `BLOCK` (a whole data-center ASN flagged for abuse), but Meridian's own cloud NAT gateway, `203.0.113.0/24`, happens to sit inside that exact /16 — and Meridian has that /24 explicitly allowed on their own list. A plain scan gives no principled way to decide which rule wins when two overlapping ranges disagree; the trie walk naturally returns the **more specific** one, so Meridian's carve-out correctly beats the broader block.

**The IPv6 beat, worth raising before anyone asks:** the same trie works for IPv6, just walking 128 bits instead of 32. Several of Anchorly's newer enterprise customers run cloud NAT gateways that are **IPv6-only**, and an IPv4-only trie would silently leave every one of their requests unmatched — not erroring, just quietly falling through to whatever the "no match" default is, which is a much scarier failure than a loud one.

**Sizing check:** 46,000 ranges compiled into a trie lands around a few hundred thousand nodes — low single-digit megabytes, comfortably fits in memory on every server. Lookup cost is now bounded by IP bit-length (32 or 128), not list size — microseconds, not milliseconds. The 12ms check from Chapter 2 drops to **under 1ms** `[illustrative]`.

**New problem, and the real pivot of this whole story:** the *structure* is solved. But Anchorly is about to launch a payments feature, and compliance drops a requirement that has nothing to do with structure at all: *"screen every transaction's IP against the government's list of sanctioned-country IP ranges."* That list isn't Anchorly's. Anchorly can't move faster than the government publishes it, can't call it more often than it allows, and can't make it more reliable than it is.

**How I'd say this in an interview:** "CIDR ranges need a prefix trie for longest-prefix-match, not a hash set and not a linear scan — it's the same structure routers, Cloudflare's edge firewall, and AWS WAF IP sets all use, and it naturally resolves overlapping ranges by letting the most specific one win. That solves the shape of the data. The much harder problem is next: what do you do when the data itself comes from a source you don't control and can't make faster?"

---

## Chapter 4 — The ministry doesn't do webhooks

The naive first move: when a payment request comes in, call the government's sanctions API directly, synchronously, to check the IP right then. It looks like it should just work — it's "one more API call," same shape as any other dependency.

Here's the math that kills it. The government's feed publishes roughly **500,000 IP ranges** `[illustrative]` and enforces a rate limit of **10 requests/minute** — a real, common constraint for this class of regulatory data portal, even if the exact number here is a stand-in. Anchorly's payments feature needs to screen at a target scale of **300,000 requests/sec** `[illustrative]`. Convert the rate limit to the same unit: 10 requests/minute is about **0.17 requests/sec.** Compare that to the 300,000/sec actually needed, and the gap is roughly **1.8 million times.**

```mermaid
sequenceDiagram
    participant Req as Payment request
    participant Anchorly
    participant Gov as Government sanctions API (10 req/min limit)

    Req->>Anchorly: check IP against sanctions list
    Anchorly->>Gov: is this IP sanctioned? (synchronous)
    Note over Anchorly,Gov: this call now happens on EVERY payment,\nagainst a source that allows 10 calls PER MINUTE
    Gov--xAnchorly: rate limited after the first few calls this minute
    Anchorly-xReq: request stalls, then times out
```

The break is immediate and dramatic during soft launch: the checkout endpoint's p99 latency goes from a normal ~40ms to **over 30 seconds**, and then requests start timing out outright. Within the hour, the sanctions-feed vendor's own ops team emails Anchorly asking why they're being hit with thousands of calls a minute against a 10-per-minute limit — and warns that continued abuse will get the API key revoked entirely.

The obvious next question: *isn't this just a slow dependency — can't a cache fix it?* That's the next thing every engineer reaches for, and it's a reasonable instinct — it's just not enough on its own, as the next chapter shows.

**How I'd say this in an interview:** "Calling a rate-limited external source synchronously, per request, isn't just slow — it's capacity-bounded by someone else's quota, and the gap here is about six orders of magnitude. No amount of 'let's just make the call faster' closes a gap that size; the call has to come out of the request path entirely."

---

## Chapter 5 — The cache that still calls home for strangers

The fix everyone reaches for next: a shared cache in front of the government API. Cache hit, answer instantly. Cache miss, call the government API once, populate the cache, move on. This is the standard cache-aside pattern that solves most "slow external dependency" problems — and it looks like it should solve this one too.

It doesn't, for two reasons, and both show up on the same bad day. A credential-stuffing bot wave hits Anchorly's checkout endpoint from IPs in a fresh /20 block nobody has ever seen before. Every single one of those IPs is a cache miss — **1,400 concurrent misses in the same second** `[illustrative]` — and every miss fires its own synchronous call to the government API. That's a thundering herd blowing through the 10-requests/minute limit by roughly **140x in one second**, which is more than enough to get the API key throttled or banned for the next hour, on top of the timeouts from Chapter 4.

```mermaid
flowchart TD
    A["1,400 never-seen IPs\narrive in the same second"] --> B["1,400 cache misses,\nsimultaneously"]
    B --> C["1,400 synchronous calls\nfired at the government API"]
    C --> D["10 requests/minute limit\nblown through instantly"]
    D --> E["Key throttled —\neverything now fails, not just the miss"]
```

The second, quieter problem is worse for a compliance system specifically: a lazy, request-driven cache only ever learns about an IP **after** someone has actually sent a request from it. Any sanctioned IP that hasn't shown up yet is invisibly "unknown" — and for a sanctions block-list, you need to know an IP is sanctioned **before** the first request arrives from it, not after the fact. The whole point of screening is defeated if the very first request from a bad actor is the one that "teaches" the cache what it is.

**How I'd say this in an interview:** "Cache-aside is the trap here, and it's a good trap because it looks like the right pattern from every other slow-dependency problem. It breaks for two reasons specific to this one: a burst of never-before-seen IPs still fires a thundering herd of calls at the exact rate limit that broke the naive version, and a lazily-populated cache has no way to know an IP is sanctioned until someone has already requested from it — which is backwards for a block-list."

---

## Chapter 6 — One archivist, one noticeboard

The real fix: stop treating this like a caching problem, and split it into two systems that never talk to each other on the request path. One small, dedicated **ingestion pipeline** is the *only* thing in all of Anchorly that ever calls the government API — on its own schedule, respecting the rate limit. It pulls the full list, builds a compiled, versioned snapshot (the same trie structure from Chapter 3), and publishes it to Anchorly's own internal storage. Every payment-checking server just reads *that* — never the government, never a peer server's cache.

**The analogy, and one to keep for the rest of this story:** picture one licensed archivist who has the only visitor badge into the ministry's records office. They go in on a schedule, photocopy the pages, and pin dated, numbered copies to a public **noticeboard** back at Anchorly. Every other server just walks past the noticeboard and reads whatever's currently pinned up. Nobody else ever gets a badge into the records office — there's no reason to, and every additional badge-holder would just be another way to accidentally get the whole office banned.

```mermaid
flowchart LR
    subgraph Slow["Touched by ONE component, on a schedule"]
        GOV[("Government\nsanctions API")]
    end
    subgraph Archivist["Ingestion pipeline (the only caller)"]
        PULL["Rate-limit-aware\npaginated puller"]
        BUILD["Build versioned\nsnapshot (trie)"]
    end
    subgraph Board["Noticeboard — every server reads this, only this"]
        SNAP[("Versioned snapshot,\nin-memory trie")]
    end
    GOV -.->|"pull, respecting\nthe rate limit"| PULL
    PULL --> BUILD --> SNAP
    REQ["Payment request"] --> SNAP --> DEC["ALLOW / BLOCK,\nzero external calls"]
```

This immediately kills both of Chapter 5's problems: no request ever triggers a call to the government, so there's no thundering herd, and because the archivist pulls the **entire** list proactively — not just IPs someone happened to ask about — there's no such thing as an "unseen IP" gap anymore.

**New problem:** the archivist only has one visitor badge and the ministry only allows 10 visits a minute. How often can they realistically go back for a fresh stack of papers, and how stale does the noticeboard get in between trips?

**How I'd say this in an interview:** "The fix isn't a better cache, it's decoupling entirely — one component owns talking to the slow external source, on its own schedule, and everyone else reads a versioned snapshot that component publishes to infrastructure Anchorly owns. That single split is what makes the request path immune to the government API's rate limit and downtime at the same time."

---

## Chapter 7 — How many trips can the archivist make

Numbers, worked the same way the reference guide does it. The government feed publishes **500,000 ranges**, at roughly **40 bytes** per entry (start IP, prefix length, decision code, source rule id, overhead) — a raw snapshot size of about **20 MB.** Trivial to hold fully in memory on every server; the hard part was never "does it fit."

The rate limit is 10 requests/minute, and the feed returns **1,000 rows per page.** Pulling the full 500,000 rows takes 500 pages. At 10 pages/minute, that's **500 / 10 = 50 minutes** for one complete refresh — even running as a dedicated background job with nothing else competing for that quota. That 50-minute number is the one that makes Chapter 4's "just call it per request" obviously impossible before you even get to the QPS math.

If the ministry supports **delta pulls** ("give me changes since timestamp X") instead of always a full dump, the picture changes a lot: at an illustrative **2,000 changed ranges/day**, that's only 2 pages, pullable in about **12 seconds.** Delta support turns "refresh a few times a day" into "refresh hourly, or more, for almost free against the rate-limit budget."

```mermaid
quadrantChart
    title Refresh strategy: rate-limit cost vs freshness
    x-axis "Low cost against rate limit" --> "High cost against rate limit"
    y-axis "Stale" --> "Fresh"
    quadrant-1 "Fresh but expensive"
    quadrant-2 "Sweet spot, if deltas exist"
    quadrant-3 "Cheap and stale — fine if allowed"
    quadrant-4 "Overkill for full-dump-only"
    "Full pull every few hours": [0.75, 0.55]
    "Full pull once a day": [0.35, 0.25]
    "Delta pull hourly": [0.1, 0.85]
```

**The rule that falls out of this:** refresh cadence is bounded by what the *source* can sustain, not by how fresh Anchorly would like the data to be. Push the archivist to pull too aggressively, and the realistic outcome isn't fresher data — it's a throttled or revoked API key, which is a self-inflicted, total outage of the freshness pipeline, not a performance tweak.

**How I'd say this in an interview:** "Compute the pull time from the source's own rate limit, page size, and dataset size — don't just assert a refresh interval. At 10 requests a minute and 500,000 rows, a full pull costs about 50 minutes; if delta pulls exist, that same freshness costs seconds instead, which is the whole argument for asking the source, upfront, whether they support incremental queries."

---

## Chapter 8 — The new server that inherits an empty desk

New problem, unrelated to freshness: Anchorly deploys a routine update, and 40 payment-checking servers restart at roughly the same time. Each one boots with... nothing. No trie, no snapshot, nothing loaded yet. What happens to a payment request that arrives in that gap?

Two bad options present themselves immediately. Error every request until the first snapshot loads — a self-inflicted outage during every deploy. Or, worse, silently default to "allow everything" until data shows up — which, for a sanctions screen, means every deploy opens a real, if brief, window where sanctioned traffic passes through completely unchecked.

```mermaid
sequenceDiagram
    participant Orchestrator
    participant Server as New payment server
    participant Board as Noticeboard (internal store)
    participant LB as Load balancer

    Orchestrator->>Server: start container
    Server->>Board: fetch LATEST snapshot version
    Board-->>Server: snapshot blob (~20MB) + version id
    Server->>Server: load into in-memory trie, validate
    Server->>Server: readiness probe = healthy (ONLY now)
    Orchestrator->>LB: register as ready
    LB->>Server: begin sending live payment traffic
```

**The fix:** every server's boot sequence fetches the **latest known-good snapshot** from the noticeboard first, and the readiness probe depends on that load actually succeeding — not on the process simply having started. No server ever joins the traffic pool empty, and crucially, no server calls the government API directly on boot either — that would turn "we deployed 40 servers" into "the government API just got hit by 40 simultaneous pulls," reintroducing the exact rate-limit stampede this whole design exists to avoid.

**How I'd say this in an interview:** "Startup is a scaling event too — a fleet restart has to get its data the same way normal traffic does, from Anchorly's own infrastructure, never from a peer server and never from the government directly. Gate the readiness probe on 'snapshot loaded and validated,' not 'process started,' because the silent failure mode — defaulting to allow-everything — is far more dangerous than being slow to come up."

---

## Chapter 9 — When the ministry goes dark for a weekend

New problem, and the one that separates a real design from a lucky one: the government's sanctions API goes down for **30 hours** one weekend `[illustrative — real regulatory portals do have unplanned multi-hour outages, though this exact duration is a stand-in]`. No warning, no SLA to hold anyone to. What do Anchorly's payment servers do while it's down?

```mermaid
flowchart TD
    A["Scheduled refresh fails"] --> B{"Is a previous\nsnapshot still loaded?"}
    B -->|"yes"| C["KEEP serving it.\nDon't block everyone.\nDon't allow everyone.\nStaleness clock ticks."]
    B -->|"no, first boot ever"| D["No safe default exists —\nthis is a launch-readiness gap,\nfix before go-live"]
    C --> E{"Staleness past\nthe stated bound?"}
    E -->|"no"| F["Normal — log and monitor"]
    E -->|"yes"| G["Escalate to a human:\npage on-call, notify compliance.\nNot an engineering default to invent."]
```

Both obvious defaults are wrong. **Fail-open** (let every payment through) defeats the entire point of a sanctions screen the moment the feed hiccups — the outage window becomes exactly when a sanctioned actor could slip through undetected. **Fail-closed** (block every payment) turns a third-party's bad weekend into a complete, self-inflicted outage of Anchorly's own payments product — the government going dark should degrade *freshness*, not *availability*.

**The actual answer:** keep serving the last known-good noticeboard snapshot indefinitely — it was correct as of a real point in time, and it remains the best available approximation of the truth. Layer a **staleness SLA** on top: below some stated bound (say, 24 hours, set by legal/compliance, not invented by an engineer at 2am), this is just a normal, monitored degradation. Past that bound, escalate to a human — "is serving 30-hour-old sanctions data still acceptable" is a policy question, not something code should decide silently.

**How I'd say this in an interview:** "Default to serving the last known-good snapshot through an outage — staleness is almost always a smaller harm than a false allow-everyone or a self-inflicted block-everyone. But say out loud that 'how stale is too stale to keep serving' is a policy decision with a real, stated number, not something to leave undefined until an incident forces the question."

---

## Chapter 10 — Six noticeboards, still one archivist

Anchorly expands to six regions for latency. The first instinct from a new engineer on the team: sync each region's cache to its neighbors, so every region has a fresh copy. It sounds reasonable — it's exactly the wrong move, for the same reason cache-to-cache sync is the wrong move anywhere in distributed systems.

```mermaid
flowchart LR
    R1["Region 1"] <-.->|"sync?"| R2["Region 2"]
    R2 <-.->|"sync?"| R3["Region 3"]
    R3 <-.->|"sync?"| R1
```

Three concrete problems. **No leader.** If Region 1 updates first and pushes to the rest, Region 1 has quietly become a hidden single point of failure and bottleneck — exactly what going multi-region was supposed to avoid. **Latency compounds.** Chained syncing means the last region's freshness depends on every hop before it being healthy. **It doesn't touch the actual bottleneck** — the problem was never "how do regions talk to each other," it was "how does data get from the government into many places," and if each region's cache miss still falls back to calling the government directly, six regions means six times the load on a 10-requests/minute source, for zero benefit.

**The actual answer — the same noticeboard from Chapter 6, restated for six regions:** every region independently pulls the **same immutable, versioned snapshot** from Anchorly's own internal store, on its own schedule. No region ever syncs from another region, and no region ever calls the government directly.

```mermaid
flowchart TD
    GOV[("Government API")] --> PIPE["ONE ingestion pipeline\n(the only caller, ever)"]
    PIPE --> STORE[("Anchorly's own store\n(the noticeboard)")]
    STORE -->|"pull independently"| R1["Region 1"]
    STORE -->|"pull independently"| R2["Region 2"]
    STORE -->|"pull independently"| R3["Region 6"]
```

Consistency becomes one boring, explicit number to monitor: how far behind the newest snapshot version any given region is allowed to lag — e.g., "no region serves a snapshot more than 15 minutes older than the newest one" — instead of an open-ended cache-sync consistency problem with no natural leader.

**How I'd say this in an interview:** "Going multi-region doesn't mean syncing N caches to each other — it means N independent readers pulling the same canonical, versioned artifact from infrastructure you own. There's no leader, no mesh, and the government API is still called exactly once per refresh cycle, from exactly one place, no matter how many regions you add."

---

## Chapter 11 — The override that can't wait for the next noticeboard

Last problem, and it's a business one, not a scaling one. A real, six-figure Anchorly customer's cloud NAT gateway happens to fall inside a broad `/16` the government flagged as a sanctioned data-center ASN. Every one of that customer's legitimate payments starts getting blocked. Support escalates within the hour. Waiting for the next scheduled snapshot — a minimum of the ~50-minute full-pull cost from Chapter 7, possibly hours depending on cadence — is not an acceptable answer for revenue actively being blocked right now.

**The fix:** a small, separate **override layer** — manual allow/block entries, checked *before* the government-sourced snapshot, most-specific-match wins, same longest-prefix-match logic from Chapter 3 applied across both layers together. An ops engineer adds one override entry, and the customer's payments start flowing again in seconds — without touching the immutable, government-sourced snapshot at all.

```mermaid
sequenceDiagram
    participant Req as Payment request
    participant Svc as Payment server
    participant Ovr as Override store
    participant Trie as Government snapshot trie

    Req->>Svc: check IP
    Svc->>Ovr: any manual override for this IP/range?
    Ovr-->>Svc: yes — ALLOW (ticket INFRA-4521)
    Svc-->>Req: ALLOW (override wins, snapshot never consulted)
```

**The last piece, tying back to why any of this matters:** every decision — from the trie, or from an override — gets logged with the **exact rule that matched and the exact snapshot version** it was checked against, not just a timestamp. A timestamp says roughly when; the version id says precisely which set of rules was live, which is what "reproduce this decision during a compliance audit" actually needs. Re-deriving "which snapshot was active at 14:32:07" from timestamps alone falls apart across deploys, clock skew, and overlapping refresh windows.

**How I'd say this in an interview:** "Overrides sit in a small, separately-audited layer checked before the government snapshot, not as edits to the snapshot itself — that keeps the government data an untouched, versioned artifact, and gives you a false-positive fix that takes seconds instead of waiting on the next refresh cycle. And every decision, override or not, needs to carry the exact snapshot version it was checked against — that's what makes it explainable months later, not just logged."

---

## Where the story actually lands

```mermaid
flowchart LR
    A["Ch1: linear scan\n(list grows, gets slow)"] -->|"fixes: O(1) lookup\nbreaks: exact-match only"| B["Ch2: CIDR ranges"]
    B -->|"fixes: fewer entries\nbreaks: range scan is O(n) again"| C["Ch3: trie / LPM"]
    C -->|"fixes: fast, correct overlap resolution\nbreaks: data now from a slow external source"| D["Ch4: naive sync call"]
    D -->|"fixes: nothing — 1.8M x gap\nbreaks: obviously impossible"| E["Ch5: cache-aside"]
    E -->|"fixes: nothing — thundering herd + unseen-IP gap"| F["Ch6: decouple ingest/serve"]
    F -->|"fixes: request path never calls source\nbreaks: how often can we pull"| G["Ch7: refresh cadence math"]
    G -->|"fixes: real cadence number\nbreaks: fleet restart boots empty"| H["Ch8: warm-start boot"]
    H -->|"fixes: never boot empty\nbreaks: source goes dark for hours"| I["Ch9: fail-open vs fail-closed"]
    I -->|"fixes: serve last-known-good + staleness SLA\nbreaks: naive multi-region sync"| J["Ch10: N independent pulls"]
    J -->|"fixes: no leader, no mesh\nbreaks: false positive can't wait for refresh"| K["Ch11: overrides + audit"]
```

```mermaid
mindmap
  root((Why an IP allow/block\nservice needs all of this))
    Matching structure
      linear scan = slower as list grows
      CIDR ranges need range math, not hashing
      trie / longest-prefix-match resolves overlaps correctly
    The external source problem
      synchronous call = capacity-bounded by someone else's quota
      cache-aside still stampedes on unseen IPs
      decouple ingestion from serving, always
    Keeping it fresh
      refresh cadence bounded by the source's rate limit, not your wishes
      delta pulls change the math completely
    Staying up
      never boot with an empty structure
      never fail fully open or fully closed on a long outage
    Scaling out
      multi-region is N independent pulls, never cache-to-cache sync
    Staying correct
      overrides fix false positives fast, without touching the source data
      every decision needs an exact rule + snapshot version, not just a timestamp
```

The skill isn't reciting all eleven chapters — it's knowing which one the interviewer's constraints actually demand. A simple per-customer allowlist might reasonably stop around Chapter 3. Anything involving an external regulatory feed has to reach Chapter 6 at minimum, and a real compliance answer needs Chapter 9 and 11 too.

---

## Grill me — adversarial follow-ups

**Q1: "Why not just make the trie lookup itself faster instead of building this whole separate ingestion pipeline?"**
Because the trie was never the bottleneck once it existed — a trie lookup is already microseconds. The actual bottleneck is the government API's rate limit, and no amount of optimizing the read path changes how many times per minute you're allowed to ask an external system a question.

**Q2: "Isn't a versioned snapshot on a noticeboard just... a cache with extra steps?"**
The difference is who populates it and when. A cache is populated lazily, by requests, which is exactly what caused the unseen-IP gap in Chapter 5. A snapshot is populated proactively and completely, on a schedule, by one dedicated pipeline — it's push-the-whole-dataset, not learn-as-you-go, which is the property that actually matters for a block-list.

**Q3: "What happens if the ingestion pipeline itself crashes, not the government API?"**
Nothing happens to serving at all — that's the entire point of decoupling. Every payment server keeps answering from the last snapshot it already loaded; only freshness degrades, the same as if the government API itself were down. The two failure domains are deliberately kept separate.

**Q4: "Why not enumerate every IP in a range into a hash set instead of building a trie?"**
Because a /16 alone is 65,536 addresses, and the full published list can be hundreds of thousands of ranges — enumerating would blow up storage for no benefit, and it still wouldn't solve overlapping ranges at different specificities the way longest-prefix-match naturally does.

**Q5: "Your fail-open/fail-closed answer is 'serve last known good' — isn't that just fail-open by another name, since sanctioned traffic from before the outage could still be flowing?"**
No — it's serving data that was true as of a real, known point in time, not fabricating a permissive default. The distinction matters: a snapshot from an hour ago reflects the actual published rules then; "allow everyone because the feed is down" reflects no rules at all. One is stale truth, the other is no truth.

**Q6: "If overrides can bypass the government snapshot, isn't that a compliance hole — someone could override away a real sanction?"**
That's exactly why overrides need their own access control, a required reason, a named approver, and full audit logging — the same rigor as the automated decisions, arguably more, since a human is making a judgment call that overrides government-sourced data. It's a deliberate, logged exception path, not an unguarded backdoor.

**Q7: "Why does refresh cadence matter if you're serving from an in-memory snapshot anyway — just refresh as often as possible?"**
Because every refresh consumes the source's own rate-limit budget, not yours. Refreshing too aggressively risks getting throttled or having the API key revoked entirely, which would take the freshness pipeline down completely — the cadence has to be bounded by what the source can sustain, computed from the numbers, not just wished for.

**Q8: "For the multi-region design, why not have Region 1 be the leader and push to the others directly — wouldn't that be faster than every region pulling independently?"**
It would shave a little latency, but it reintroduces exactly the hidden-leader problem the design exists to avoid — Region 1 becomes a single point of failure and a bottleneck for everyone else's freshness. Independent pulls from a shared store cost a bit more in aggregate pull traffic against your own infrastructure, which is cheap, in exchange for having no leader at all.

**Q9: "Given all this, if someone says 'design an IP allow/block-list service' cold, where do you start?"**
The first question that changes everything downstream is: who owns the source data, and how fast and reliable is it. If it's fully internal and fast, this collapses to a matching-structure problem — CIDR trie, done. If it's external, slow, and rate-limited — like a government feed — the entire design has to be built around decoupling ingestion from serving before anything else, because that's the one decision every other chapter here falls out of.

---

## Cheat sheet — one line per stop on the story

- **Linear scan over a growing list**: O(n) cost that gets worse as the list grows — the first thing that breaks, fixed by a hash set for exact matches only.
- **CIDR ranges**: let one entry cover a whole network instead of one IP at a time, but ranges can't be hashed the way exact values can — you're back to a scan, just over fewer, bigger entries.
- **Trie / longest-prefix-match**: the real fix for range matching at scale — same structure routers, Cloudflare's edge firewall, and AWS WAF IP sets all use; naturally resolves overlapping ranges by letting the most specific one win. Works the same for IPv4 (32 bits) and IPv6 (128 bits) — don't silently assume v4-only.
- **Calling a rate-limited external source per request**: not slow, capacity-bounded — the gap between what the source sustains and what you must serve is usually orders of magnitude, closed only by removing the call from the request path, not by optimizing it.
- **Cache-aside in front of a slow source**: looks like the standard fix, isn't — a burst of unseen IPs still stampedes the rate limit, and a lazily-populated cache can't know about a sanctioned IP before someone's already requested from it.
- **Decoupled ingestion pipeline**: one component, on a schedule, is the only thing that ever talks to the external source; it builds a versioned snapshot that every server reads locally — the actual fix, not a bigger cache.
- **Refresh cadence**: bounded by the source's own rate limit and page size, not by how fresh you'd like the data — compute the real pull time before promising a number.
- **Warm-start boot**: every server loads the latest known-good snapshot before serving anything; readiness gates on that load, never on "process started" alone — a fleet restart must never stampede the external source either.
- **Fail-open vs. fail-closed**: neither is the default — keep serving the last known-good snapshot through an outage, with a monitored staleness SLA that escalates to a human decision past a stated bound.
- **Multi-region distribution**: N independent pulls of one canonical, versioned snapshot from infrastructure you own — never cache-to-cache sync, which has no natural leader and doesn't touch where the data originates.
- **Overrides**: a small, separately-audited layer checked before the source-derived snapshot, for fixing false positives in seconds without touching the immutable government data.
- **Audit trail**: every decision logged with the exact matched rule and exact snapshot version, not just a timestamp — that's what makes a decision reproducible during a compliance review months later.
