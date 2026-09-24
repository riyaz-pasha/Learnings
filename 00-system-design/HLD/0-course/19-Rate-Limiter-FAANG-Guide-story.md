# Rate Limiter — The Story (narrative edition)

> **What this file is.**
> The reference file, `19-Rate-Limiter-FAANG-Guide.md`, is the one to study from. It has requirements, algorithms, data models, capacity math, and the master cheat sheet.
>
> This file is a second way to learn the same material. It tells the same story in plain, simple language. Engineers at a fictional company keep running into a wall. They fix it. That fix reveals the next wall. This continues until they land on the exact same design described in the reference file.
>
> The company is called **PulseAPI**. It is a communications API that lets developers send SMS and push notifications. Developers are billed per message.
>
> PulseAPI is fictional. But every wall it hits, and every fix it reaches for, is something a real, named system actually does:
> - GitHub's fixed-window API limits
> - Stripe's and AWS API Gateway's documented token buckets
> - NGINX's leaky-bucket `limit_req` module
> - Lyft's open-source `ratelimit` service, running inside Envoy
> - Google's open-source Doorman, for cross-region quotas
>
> Every time a number appears, I will clearly say whether it is a documented fact or just a reasonable stand-in. Stand-in numbers are tagged `[illustrative]`.

**The trigger phrases** for this whole topic: *"design a rate limiter,"* *"how do we stop one client from hammering our API,"* or *"how do we cap what we spend on a third-party call."*

Keep one sentence in your head as you read:

> **A rate limiter isn't a wall — it's a bouncer with a clipboard.**

It doesn't block traffic completely. Instead, it counts requests against a rule, and only lets through what the rule allows.

The clipboard — where that count lives, and how it stays correct when many machines are writing to it at the same time — is the entire hard part of this problem. Everything below is just this one idea, getting harder in small, honest steps.

---

## Chapter 1 — The retry loop that cost $11,900 in one afternoon

It's 2018. PulseAPI is small. Developers call `POST /messages/sms`, and PulseAPI forwards each one to a carrier that charges per message sent. There is **no rate limiting anywhere** in the system. Nobody has needed it yet.

One of PulseAPI's customers is an online retailer's checkout-confirmation service. They ship a bug.

Here is what the bug does: when the carrier is briefly slow to respond, their code retries the *same* SMS send with no backoff and no cap. It retries every 20 milliseconds, forever, until someone notices.

It runs unnoticed for about four hours.

Here is the math `[illustrative — PulseAPI's own pricing and retry rate, standing in for a well-documented failure category]`:

- About 110 requests/sec, nonstop, for 4 hours
- That's roughly 1,584,000 messages sent
- At $0.0075 per message
- Total: **$11,880** — just under twelve grand, for one customer's bug, in one afternoon

It gets worse. While that customer's traffic floods PulseAPI's shared web tier, *other* customers' legitimate SMS sends start timing out. One buggy client is starving everyone else on a shared resource.

This exact scenario has a name in the field: **friendly-fire DoS**.

```mermaid
flowchart LR
    Bug["Retry loop bug\n(no backoff, every 20ms)"] -->|"~110 req/sec for 4 hours"| API["PulseAPI gateway"]
    API -->|"each message billed"| Carrier["SMS carrier"]
    API -.->|"web tier saturated —\nother customers time out"| Other["Other customers\n❌ requests fail"]
```

Why did nothing stop this? Because nothing was counting. There was no rule, and nothing was enforcing one. Any request that reached the server got forwarded — no matter how many came from the same place, no matter how fast.

**The fix, and the analogy for the rest of this story:**

Put a rate limiter in front of everything. Think of it as **a bouncer with a clipboard**, not a wall. It counts requests against a rule and only lets through what the rule allows.

Where does the bouncer stand? Let's look at the options:

```mermaid
flowchart LR
    C1["Client-side limiter"] -->|"attacker owns the client\n— easy to bypass"| X1["❌ not trustworthy"]
    S1["Limiter inside each service"] -->|"works, but duplicated\nin every service"| X2["⚠️ hard to share policy"]
    G1["Limiter at the API gateway"] -->|"all traffic passes here\n— one enforcement point"| X3["✅ correct placement"]
```

- **Client-side?** A client SDK limiter is a courtesy. The attacker — or the buggy retry loop — controls that code and can just remove it.
- **Inside each backend service?** It works, but you have to duplicate the limiting logic into every service, everywhere.
- **At the gateway?** Yes. The gateway is the one place *all* traffic already has to pass through. The bouncer goes here.

**New problem, visible within the first week of running this in production:**

PulseAPI doesn't run one gateway box. It runs several, behind a load balancer, for redundancy. Which raises an immediate question:

Does each box keep its *own* clipboard? Or do they all share one?

**How I'd say this in an interview:**
> "A rate limiter is a bouncer with a clipboard — it counts, checks against a rule, and lets through only what's allowed. It has to sit somewhere all traffic actually passes through, which is why client-side limiting is a UX nicety at best. The gateway is where real enforcement lives. The next question is always where the clipboard's actual numbers live once you have more than one gateway box."

---

## Chapter 2 — Every bouncer keeping his own clipboard

PulseAPI stands up 4 identical gateway instances behind a round-robin load balancer. Each one runs its own in-process counter — a plain map from API key to a request count, reset every minute.

The rule: **100 requests per minute per API key.**

Six weeks later, a partner integration team notices something odd. A customer who is capped at 100/min is somehow pushing through closer to **400 requests in a single minute** during their peak.

Here is what is happening:

With 4 servers and round-robin routing, that customer's traffic gets split roughly evenly across all 4 boxes. Each box independently thinks: "You have sent 100 requests to *me* — that is the limit." But no box has any idea that the same client also sent 100 requests to each of the other three.

Four bouncers, each holding their own clipboard, each blind to the other three's count.

`4 × 100 = 400` — that is 4 times the intended limit. And it gets worse the more gateway boxes you add.

```mermaid
flowchart LR
    Client[Client, limit = 100/min] --> LB[Load Balancer]
    LB --> S1["Gateway 1\nown clipboard: 100"]
    LB --> S2["Gateway 2\nown clipboard: 100"]
    LB --> S3["Gateway 3\nown clipboard: 100"]
    LB --> S4["Gateway 4\nown clipboard: 100"]
    S1 -.-> Total["Actual total: 400\n(4x the intended limit)"]
    S2 -.-> Total
    S3 -.-> Total
    S4 -.-> Total
```

Why didn't anyone catch this in testing? Because a single developer testing from one machine almost always lands on the same gateway box (or close to it). The bug only shows up under real, spread-out production traffic. This is exactly why "it works on my machine" is a trap for this whole topic.

**The fix:**

Stop keeping separate clipboards. Move the count into a **shared store**. PulseAPI picks Redis — it is in-memory and fast. Now every gateway box checks and increments the *same* number for a given key, no matter which box happens to handle the request.

One clipboard at the front desk, instead of one per bouncer.

**New problem, immediately obvious once it's built:**

A shared clipboard answers "where does the number live." But it does not answer "what number, exactly, and counted over what window?" Right now, the counter just increments a raw integer with no notion of time at all. The very first algorithm question is still unanswered.

**How I'd say this in an interview:**
> "Per-server in-memory counters look fine until you have more than one server. Then the limit gets divided by however many servers are in the pool, because none of them can see each other's count. The standard fix is centralizing the counter in a shared store like Redis, so the limit means the same thing no matter which node handles the request."

---

## Chapter 3 — The clipboard that resets at the stroke of the minute

PulseAPI wires the shared Redis counter to the simplest possible algorithm: a **fixed window counter**.

Here is how it works: divide time into 1-minute buckets. Each bucket has its own counter. Reject a request once the counter hits 100.

This is exactly what GitHub's REST API does in production, at real scale — 5,000 requests per hour, fixed window. It is a real and citable example.

It works, mostly. Then a customer complains their "100/min" limit let through **196 requests in about 2 seconds**.

Here is the bug:

- 98 requests land at `0:59.5`, just before the minute boundary. The `0:00–1:00` window's counter was still under 100, so all 98 were allowed.
- 98 more requests land at `1:00.5`, just after the boundary. The `1:00–2:00` window starts its counter fresh at zero, so all 98 were allowed again.

Now look at any 60-second slice that *straddles* those two windows — say, from `0:30` to `1:30`. It actually saw up to 196 requests. That is nearly double the intended limit.

This is called the **edge-burst problem**. It is a structural flaw of fixed windows, not a bug in PulseAPI's code. GitHub tolerates this exact flaw because a bounded 2x edge case is an acceptable cost for a free, coarse limit that mostly protects idle capacity.

```mermaid
flowchart LR
    subgraph W1["Window A — 0:00 to 1:00  (limit: 100)"]
        E1["98 requests\nat 0:59.5 ✅"]
    end
    subgraph W2["Window B — 1:00 to 2:00  (limit: 100)"]
        E2["98 requests\nat 1:00.5 ✅"]
    end
    W1 -.->|"⚠️ a 60s slice straddling\nthe boundary sees 196 total"| W2
```

PulseAPI cannot shrug this off the way GitHub does. Every one of those extra requests is a real, billed carrier send.

**The next question:** How do we make the window actually roll, instead of snapping back to zero?

**First attempt:** A **sliding window log**.

Instead of one counter, keep every single request's timestamp in a sorted log per key. On every new request:
1. Evict anything older than 60 seconds
2. Compare the log's size to the limit

This is exact. There is no edge case left, because the window is computed relative to *right now*, for every single request.

**New problem, showing up the moment traffic climbs:**

PulseAPI is now doing about 4,000 requests/sec. A busy API key's log, over a 60-second rolling window, can hold **up to 240,000 timestamps** — one entry per request, even rejected ones, until each ages out.

That is real memory, per key, and it scales with *traffic volume* rather than with the *limit*. The log for a busy key can be dramatically bigger than the log for a quiet key with the same limit.

**The fix that actually ships:** The **sliding window counter** — the pragmatic middle ground.

Keep only *two* fixed-window counters per key:
1. The previous window's count
2. The current window's count

Then blend them by how much of the previous window has "bled into" the present:

```math
Rate = R_{prev} \times \frac{window - overlap}{window} + R_{curr}
```

Worked example straight from the math:
- Previous window: `R_prev = 88`
- Current window: `R_curr = 12`
- Window size: `60s`
- We are `15s` into the new window, so `overlap = 15s`

```
Rate = 88 × (60 − 15)/60 + 12
     = 88 × 0.75 + 12
     = 66 + 12
     = 78

78 < 100 → allow
```

Two counters, blended by overlap, instead of one giant ledger of every timestamp.

It assumes requests were spread evenly across the previous window. That is an approximation, not exact. But it closes the edge-burst hole to a rounding error instead of a 2x spike — at the memory cost of just two integers per key. This becomes PulseAPI's default algorithm going forward, and it is the same choice most production systems land on.

> **A trap worth knowing:** If someone asks "which algorithm smooths the fixed-window edge spike — token bucket, sliding log, or fixed window?" — the honest answer is *none of those three*.
> - Token bucket allows bursts rather than smoothing them
> - Sliding log has no edges to smooth because it is exact
> - Fixed window *causes* the edge-burst problem
>
> The sliding window counter is the one that actually fixes it. It is easy to accidentally leave off the option list.

**How I'd say this in an interview:**
> "Fixed window is the simplest thing that could work, but it has a structural edge-burst flaw — up to 2x the limit across a boundary. Sliding window log fixes that exactly but costs memory proportional to traffic, not to the limit. The sliding window counter is the production default because it keeps just two numbers per key and smooths the edge case down to an acceptable approximation."

---

## Chapter 4 — Two clipboards, two very different jobs

Two separate requirements land on PulseAPI's desk in the same sprint. They pull in opposite directions.

### Requirement 1: Allow short bursts

A mobile-app customer complains their users get throttled unfairly. Their app syncs in the background. Once it wakes up, it legitimately fires 20 requests in one second — well under their daily quota, just bunched up.

The sliding window counter would actually let this through fine since it is still counting correctly. But PulseAPI wants to **explicitly guarantee burst tolerance** as a first-class feature — not an accident of the math.

The named answer for "should short bursts be explicitly allowed, up to a cap" is the **token bucket**.

Picture an arcade token dispenser:
- It drops a token into a cup at a fixed rate
- The cup can hold up to a maximum number of tokens
- Every request spends one token
- If the cup is empty, the request is rejected

Worked example from the classic version of this algorithm:
- Capacity: `C = 3` tokens
- Refill rate: `R = 3 per minute`

Three requests land in the same minute and drain the cup. A fourth in that same minute is rejected. A minute later, the cup refills to 3.

This is real, documented, production behavior. **Stripe** runs a token bucket per API key (with separate buckets for reads vs. writes). **AWS API Gateway** explicitly documents its own limiter as a token bucket — a steady-state rate plus a burst capacity, configurable per account.

```mermaid
flowchart TD
    Refill["Token refill\n+1 per (1/R) sec, up to cap C"] --> Cup(("🪙 Token cup"))
    Req["Incoming request"] -->|"costs 1 token"| Cup
    Cup -->|"token available"| Allow["✅ forward request"]
    Cup -->|"cup is empty"| Reject["❌ reject (429)"]
```

### Requirement 2: Smooth traffic to a constant rate

PulseAPI's *own* outbound calls to the SMS carrier are contractually capped. The carrier's SLA says: no more than **50 sends/sec, sustained, no bursts** — or they start dropping PulseAPI's traffic.

Inbound customer traffic is bursty by nature. That is the whole point of the token bucket above. But the carrier does not care how bursty the input was. It only accepts a flat, constant rate.

The named answer here is the opposite mechanism: the **leaky bucket**.

Picture a bucket with a fixed-size hole in the bottom. No matter how fast you pour water in, it always drains at the same rate. This is exactly what **NGINX's `limit_req` module** implements, baked directly into the web server. It is also mathematically equivalent to what Redis's own `CELL` command implements via something called the Generic Cell Rate Algorithm.

```mermaid
flowchart TD
    In["Inbound requests\n(variable / bursty)"] --> Bucket(("🪣 Leaky bucket"))
    Bucket -->|"drains at fixed 50/sec, FIFO"| Out["Outbound to carrier\n(smooth, constant rate)"]
    Bucket -->|"bucket is full"| Reject["❌ discard"]
```

### The key distinction

These two algorithms use the same "bucket" vocabulary but answer opposite questions.

- **Token bucket** controls the **input**. It decides whether a request is *allowed through at all*. It lets the downstream see a burst up to the cup's capacity.
- **Leaky bucket** controls the **output**. It queues requests and lets the downstream see only a fixed, constant drip — no matter what the input looked like.

**New problem:**

Both of these buckets need more state per key than the sliding window counter's two integers. A token cup needs `{tokens, last_refill_timestamp}`. Taking a token means: read that state, do refill math, then write it back.

Under concurrency, that read-then-write is exactly the same shape of bug that a naive fixed-window increment has — except now it is explicit and unavoidable, because refill math genuinely requires a read before a write.

**How I'd say this in an interview:**
> "Token bucket and leaky bucket use the same bucket-and-cup vocabulary but answer opposite questions — token bucket lets a burst *through*, leaky bucket smooths a burst *out* to a fixed rate. I'd pick token bucket for a public-facing API that should tolerate short bursts, the way Stripe and AWS API Gateway both do. I'd pick leaky bucket for feeding a downstream system with a genuinely fixed capacity, the way NGINX's `limit_req` does."

---

## Chapter 5 — Two bouncers peeking at the same "4"

PulseAPI's counter-update code does the obvious thing until now: read the current count, check it against the limit in application code, then write the incremented value back.

One night, a monitoring dashboard flags a customer capped at 100/min who actually got **118 requests through** in a single minute.

The sliding-window-counter math itself is not wrong here. The bug is concurrency.

Here is what happened:
1. Two gateway processes handle two requests for the same API key at nearly the same instant.
2. Both do a `GET` on the counter and both see `4` — well under the limit.
3. Both compute `5` in their own memory.
4. Both do a `SET` back to `5`.
5. Both requests get admitted — but only one increment actually "happened" from the store's point of view.

This is the classic **get-then-set race**. It can happen dozens of times a minute under real concurrent load, quietly padding every customer's effective limit by a few percent.

```mermaid
sequenceDiagram
    participant R1 as Request A
    participant R2 as Request B
    participant Store as Redis (limit=100)

    rect rgb(255, 230, 230)
    Note over R1,Store: ❌ naive get-then-set — race condition
    R1->>Store: GET count → 4
    R2->>Store: GET count → 4
    R1->>R1: 4 < limit → allow, compute 5
    R2->>R2: 4 < limit → allow, compute 5
    R1->>Store: SET count = 5
    R2->>Store: SET count = 5
    Note over Store: both admitted — one increment silently lost
    end
```

**The obvious question:** Didn't centralizing the clipboard in Chapter 2 already fix this?

No. "Shared" only means one clipboard exists. It does not mean that reading it and writing it happens as one indivisible step. Two bouncers can absolutely still both peek at the same "4" written on that one shared clipboard before either of them updates it.

**The fix:** Stop doing get-then-set. Do **set-then-get**, atomically.

Redis's `INCR` command increments the counter *and* returns the new value in a single, indivisible operation. There is no gap in the middle for a second request to sneak a read.

```mermaid
sequenceDiagram
    participant R1 as Request A
    participant R2 as Request B
    participant Store as Redis (limit=100)

    rect rgb(230, 255, 230)
    Note over R1,Store: ✅ atomic INCR
    R1->>Store: INCR count → returns 5
    R2->>Store: INCR count → returns 6
    R1->>R1: 5 ≤ 100 → allow
    R2->>R2: 6 ≤ 100 → allow (correctly, this time)
    end
```

For the token bucket from Chapter 4, a plain `INCR` is not enough. Refilling tokens, checking availability, decrementing, and setting a TTL are *multiple* steps. Each one individually atomic still leaves gaps between them.

The fix one level up: bundle the whole sequence into a **Lua script** that Redis runs server-side, atomically, as a single round trip. "Check refill, check availability, decrement, done" — the whole sequence happens as one indivisible unit, with nothing else able to interleave partway through.

**New problem:**

Atomic ops close the correctness gap. But they do not make the round trip to Redis free. Every single request, for every customer, now pays a real network hop before it can be forwarded. Is that actually sustainable at real production scale?

**How I'd say this in an interview:**
> "A shared counter doesn't automatically mean safe concurrent access — get-then-set has a race window that two nearly-simultaneous requests can both slip through. The standard fix is an atomic increment like Redis's `INCR`. For anything needing more than one step — like a token-bucket refill — a Lua script runs the whole sequence as one atomic round trip on the server."

---

## Chapter 6 — Checking the master clipboard on every single letter

PulseAPI's traffic has grown to a real number: **20,000 requests/sec** across all customers, at peak.

Each request now does one atomic Lua check-and-increment against Redis before being forwarded. Add roughly 2x headroom for retries and TTL housekeeping. That is about **40,000 ops/sec** the counter store needs to sustain — comfortably inside what a single Redis node can do (a simple-ops Redis node typically handles somewhere from 100K up to 1M ops/sec).

So throughput isn't the bottleneck here. **Latency is.**

Every one of those 20,000 requests/sec — even the ones nowhere near their limit — now pays a same-datacenter round trip to Redis before the request can even be forwarded to the actual backend. That is roughly 0.5–1ms per request.

It does not sound like much. But it is now sitting on the critical path of literally every request PulseAPI handles. It adds a fixed latency tax that has nothing to do with what the backend itself needs to do its job. At 20,000 req/sec, that is 20,000 extra round trips per second, permanently baked into p99 latency.

```mermaid
sequenceDiagram
    participant C as Client
    participant RL as Rate Limiter
    participant Store as Redis
    participant BE as Backend

    C->>RL: send request
    RL->>Store: INCR + check limit ⏱ 0.5–1ms round trip
    Store-->>RL: allowed
    Note over RL,Store: ⚠️ request is blocked here until Redis replies
    RL->>BE: forward request (only after Redis responds)
    BE-->>C: response
```

**The obvious question:** Does the bouncer really need to phone the back office before waving anyone through?

No. Split the work into two paths:

- **Online path:** Keep a short-lived **local cache** of counts at each gateway. Check *that* first and respond immediately — no round trip.
- **Offline path:** Update the real counter in Redis **asynchronously**, after the fact. This is off the critical path.

The bouncer does not run to the back office to log every single guest before letting them in. They wave people through off a running tally they keep themselves. The back office gets the full log a moment later.

```mermaid
sequenceDiagram
    participant C as Client
    participant RL as Rate Limiter
    participant Store as Redis

    C->>RL: send request
    Note over RL: check local in-memory count — no network hop
    RL->>C: ✅ respond immediately
    RL-)Store: async update (off the critical path)
```

**New problem:**

A locally cached count can drift from the real, shared count between sync cycles.

If the local cache syncs with Redis every 500ms `[illustrative sync interval]`, here is what can happen: a customer capped at 100/min (roughly 1.67/sec) hits *two different* gateway boxes right in that 500ms gap. Both boxes might briefly count that customer separately, before they reconcile.

It is a real, bounded overage — small, but real. PulseAPI accepts it deliberately. A little slack in exchange for taking Redis off the critical path of every single request. That is the exact same accuracy-vs-cost trade every algorithm in this story has made — just moved up a layer, from the algorithm to the architecture itself.

**How I'd say this in an interview:**
> "A synchronous check-and-increment on every request adds real, permanent latency, even when throughput isn't the bottleneck. Splitting into an online path — check a local cache, respond immediately — and an offline path — persist the real update asynchronously — takes the store off the critical path. The cost is a small, bounded window where a client can slip slightly past the limit."

---

## Chapter 7 — Whose watch actually says "now"

The token bucket's refill math needs to know how much time has passed since the last request. The sliding window counter needs to know exactly where "now" sits relative to a window boundary. Both depend on a timestamp.

PulseAPI's gateway fleet now spans multiple availability zones. Each box runs its own system clock.

One gateway box has a misconfigured NTP daemon. Its clock drifts **6 seconds ahead** of the rest of the fleet `[illustrative — the specific drift; clock skew from unsynced NTP is a real, documented failure mode]`.

For most systems, 6 seconds of drift is nothing. For a rate limiter, it is a real bug.

Here is what goes wrong on that one drifting box:

- **For token bucket:** That box computes "elapsed time since last refill" using its *own* clock. It thinks 6 extra seconds have passed. So it refills roughly 6 seconds' worth of extra tokens that should not exist yet. A customer can burst past their real quota — but only for requests that land on that specific box.

- **For sliding window counter:** The same drift can place a request in the *wrong* window right near a boundary. This quietly reintroduces the exact double-counting problem the sliding window design exists to prevent.

**The obvious question:** Whose clock should actually decide "now"?

Not each gateway box's own clock. There is no reason to trust that any two boxes agree. The whole point of a shared limit is that it means the same thing everywhere.

The answer is the **shared store's** clock. Every gateway box can agree it is the same clock — because there is only one of it.

**Fixes, cheapest first:**

1. **Compute "now" at the store, not on the gateway node.** Use Redis's own `TIME` command. Or call it from inside the same Lua script already doing the atomic check. This way, every caller — regardless of which box they are on — agrees on the same clock.

2. **Run NTP/chrony on every node**, as an actual production dependency, not an afterthought. Properly synced servers typically drift by single-digit milliseconds from each other. That is negligible against windows measured in seconds to minutes.

3. **Prefer coarser windows** where sub-second precision does not matter. A 60-second window absorbs a few milliseconds of drift for free. Only sub-second limits are meaningfully sensitive to clock skew.

**How I'd say this in an interview:**
> "Any timestamp-based algorithm — token bucket refill, sliding window boundaries — is only as correct as the clock it is computed against. In a distributed setup, the fix is to compute 'now' using the shared store's clock, like Redis's own `TIME` command, instead of trusting each gateway node's local clock, which can drift. The memory hook I use: if correctness depends on 'now,' ask whose clock 'now' actually is."

---

## Chapter 8 — The one client who floods a single clipboard

A food-delivery app — one of PulseAPI's biggest customers — goes viral during a televised sports final. Their traffic on a single API key spikes from a typical 200 req/sec to **9,000 req/sec** `[illustrative spike magnitude — the celebrity-key pattern itself is real and well-documented]`.

That one API key hashes to one specific Redis shard. That shard's node starts timing out under the load — not just for the viral customer, but for **every other customer** whose key happens to hash to that same shard, purely by bad luck of the hash function.

One key hammering one resource, taking down everyone sharing that resource with it. It is the same shape of problem as Chapter 1's retry loop — just one layer further down the stack. The resource being hammered is the counter store itself instead of the SMS carrier.

```mermaid
flowchart LR
    Viral["Viral customer's key\n9,000 req/sec"] --> Shard["Redis shard #3\n(hosts this key + 40 others)"]
    Shard -.->|"shard saturates, times out"| Others["Other 40 customers on\nthe same shard also degrade"]
```

**The fix:** Once the pattern is named, split that one key's counter across **multiple sub-counters**.

Here is how it works:
- On each request, write to a randomly-chosen sub-key (e.g., `api_key:0`, `api_key:1`, ..., `api_key:7`)
- When checking the total, sum all the sub-keys together

Instead of one clipboard tracking this VIP guest's headcount, split it across 8 clipboards at 8 different desks. Add them up whenever anyone asks for the total.

More sub-keys means less write contention on any single one. The cost: reads (the check itself) get slightly slower — summing 8 numbers instead of reading 1. And slightly less precise — a check can race slightly against a write landing on a *different* sub-key at the same instant.

---

**A separate but related problem** also shows up once PulseAPI relies on the per-node local caching from Chapter 6.

If a given key's requests can land on *any* gateway node, and each node keeps its own local cache, that is Chapter 2's every-bouncer-has-his-own-clipboard problem — just reintroduced through a different door.

The fix for *this* is routing, not counting. Use **consistent hashing** (or sticky sessions at the load balancer) so a given key's requests reliably land on the *same* node.

As a bonus over a plain `hash(key) % N`: if a node dies or gets replaced, only the fraction of keys owned by that node needs to remap. With a plain modulo, every key in the system reshuffles.

```mermaid
flowchart LR
    Key["hash(api_key)"] --> Ring(("Hash ring"))
    Ring --> N1["Node A"]
    Ring --> N2["Node B"]
    Ring --> N3["Node C"]
    N2 -.->|"Node B dies →\nonly its slice remaps"| N3
```

**How I'd say this in an interview:**
> "A single hot key can saturate the shard it lands on and degrade every neighbor sharing that shard. The fix is sharded sub-counters, summed at read time — trading a little read latency and precision for a lot less write contention. That is a different problem from routing a key to the same node consistently for local caching — that one is solved with consistent hashing, not counter sharding."

---

## Chapter 9 — Waving everyone through when the clipboard office is unreachable

One night, PulseAPI's Redis cluster has a real network partition to one availability zone. It lasts **90 seconds**.

Every gateway node is still synchronously depending on Redis for its offline-sync check-ins from Chapter 6. Now those calls start timing out. Each timeout takes the full connection-timeout window — 2 seconds `[illustrative timeout config]` — to fail before the request can even be decided.

The result: for those 90 seconds, PulseAPI's throughput across the *entire platform* drops to under 10% of normal. Every request is stuck waiting out a doomed call to a store that is not answering. It is not just customers near their limit — it is everyone.

The rate limiter, built to protect the system, became the outage.

**The obvious question:** When the clipboard office genuinely cannot be reached, should the bouncer wave everyone through, or turn everyone away?

This is not a technical question. It is a **policy question**. The honest answer is: "it depends on what is being protected." And it must be decided per rule, in advance — not discovered live during an incident.

**The fix: a circuit breaker.**

After some number of consecutive failures or timeouts — say 5 in a row — the breaker "opens." Gateway nodes immediately stop even attempting to reach Redis. Instead, they fall back to a pre-decided policy, rather than hanging on every single request waiting to find out.

For PulseAPI, that policy is different depending on what is at stake:

- **General API traffic → fails open.** Let it through unthrottled for now. Losing the whole platform for a few minutes is worse than losing precise limiting for a few minutes.
- **Login endpoint → fails closed.** Reject or queue. A Redis blip during an active credential-stuffing attempt is exactly the worst possible moment to go permissive.

The breaker periodically "half-opens" — it lets a trickle of traffic through to test whether Redis has actually recovered, before fully closing again.

```mermaid
stateDiagram-v2
    [*] --> Healthy : normal operation
    Healthy --> Open : N consecutive failures or timeouts

    state Open {
        [*] --> FailOpen : general API traffic → allow through
        [*] --> FailClosed : login / security → reject immediately
    }

    Open --> HalfOpen : wait period elapsed — probe Redis
    HalfOpen --> Healthy : probe succeeds
    HalfOpen --> Open : probe fails — back to open
```

Put together, this is the real production shape PulseAPI ends up running:
- **Local cache for fast checks** (Ch6)
- **Async sync to Redis** (Ch6)
- **A circuit breaker that knows when to stop asking and fall back to a pre-decided policy** (this chapter)

It is the same accuracy-vs-cost, availability-vs-consistency trade that has run through this whole story — now wired all the way through as an actual production architecture, not just a single algorithm choice.

**How I'd say this in an interview:**
> "Fail-open versus fail-closed is a policy decision, not a default — I would decide it per rule, before an incident, not during one. A circuit breaker automates *when* that policy kicks in: after N consecutive failures it opens, applies the pre-decided fallback immediately instead of hanging every request on a dead store, and periodically half-opens to check if the store is back."

---

## Chapter 10 — Opening a second country without calling headquarters on every request

PulseAPI expands into the EU. They stand up a second regional gateway cluster with its own local Redis.

Most customers only ever hit one region and don't care. But a handful of PulseAPI's largest enterprise customers have a **single global quota** — say, 1,000,000 requests per day, total, no matter which region it is sent from.

**The obvious first idea:** Have the EU cluster check the US cluster's Redis on every request, to keep one truly global, always-consistent count.

**The obvious problem:** A cross-region round trip runs **50–150ms**. That gets added to *every single request* from either region — just to protect one shared counter for a handful of enterprise accounts. That is an unacceptable latency tax on all EU traffic, for a feature most customers do not even use.

```mermaid
flowchart TB
    subgraph US["US Region (lease: 600K/day)"]
        URQ["Requests served\nagainst local lease"]
    end
    subgraph EU["EU Region (lease: 400K/day)"]
        ERQ["Requests served\nagainst local lease"]
    end
    GC["Global Coordinator\n(async, low-frequency)"]

    URQ -->|"periodic usage report"| GC
    ERQ -->|"periodic usage report"| GC
    GC -->|"rebalance lease\n(if US is starved, EU is idle)"| URQ
    GC -->|"rebalance lease"| ERQ
```

**The fix:** Split the global quota into **local leases per region**. Enforce each lease fast and locally, with no per-request cross-region call at all.

PulseAPI's 1,000,000/day enterprise quota becomes:
- 600,000/day US lease
- 400,000/day EU lease

Each region enforces its own lease independently.

A separate, async, low-frequency process periodically compares actual usage across regions and **rebalances the leases**. If the EU region is running quiet and the US region is close to exhausting its lease, some of the unused EU allotment gets reassigned to US.

This is the real, documented model behind Google's open-source **Doorman**: distributed, lease-based fair sharing across regions, reconciled asynchronously instead of checked synchronously.

**The trade-off, stated plainly:**

PulseAPI gives up perfect, real-time global precision. For a few minutes, a customer could theoretically burst slightly past 1,000,000 if both regions happen to use their full local lease right before a rebalance.

In exchange: every single request stays fast and regional.

Precisely the same shape of trade as every earlier chapter — just at the scale of continents instead of servers.

**How I'd say this in an interview:**
> "Real-time global limits across regions would need a cross-region round trip on every request. At 50 to 150 milliseconds, that is a latency non-starter. The standard trick — the same idea behind Google's Doorman — is to split the global budget into local leases per region, enforce each lease fast and locally, and reconcile the leases periodically and asynchronously. You trade perfect global precision for regional low latency."

---

## Chapter 11 — Telling the well-behaved clients how to behave

One thing has been quietly wrong since Chapter 1: when a request gets rejected, PulseAPI's gateway just drops it. It returns a generic error with no explanation.

Support tickets pile up: *"Why was I blocked? For how long? What is my actual limit?"*

Worse — some customer SDKs, seeing a vague failure with no guidance, just retry immediately and repeatedly. This turns a well-behaved client into an accidental version of Chapter 1's own bug.

**The fix:** Always respond to a throttled request with the standard, expected shape:

- **HTTP `429 Too Many Requests`** — the correct status code
- **`Retry-After`** header — tells the client exactly how long to back off
- **`X-RateLimit-Limit`** — the client's configured limit
- **`X-RateLimit-Remaining`** — how many requests are left in the current window
- **`X-RateLimit-Reset`** — when the window resets

A well-written client can see its own quota state and self-adjust *before* it even hits the wall. Discord, for a real comparison, returns exactly `Retry-After` on its own `429`s.

Silent drops don't protect anyone. They just turn cooperative clients into unwitting attackers.

---

Along the way, PulseAPI also settles the granularity question for good — not one limit, but several tiers running simultaneously:

| Limit tier | Rule | Why it exists separately |
|---|---|---|
| Per-API-key | Freemium: 100/min · Paid: 10,000/min | The main business-facing quota, tied to what a customer pays for |
| Per-IP | 20 req/min/IP on unauthenticated endpoints (signup, login) | A blunt net for traffic that hasn't even proven who it is yet |
| Global | 5,000 outbound sends/sec company-wide, regardless of who's asking | Protects PulseAPI's own carrier connection itself, not any one customer — the rare case where the limit isn't "per someone," it's on the shared resource directly |

The **login endpoint** specifically stays fail-closed, echoing Chapter 9's decision. A Redis blip during a live credential-stuffing attempt against customer accounts is exactly the wrong moment to go permissive — even though general API traffic fails open.

Paid enterprise customers get **soft throttling** — a 5% grace over their cap (10,000 → 10,500) — rather than a hard cliff. A momentary overage from a paying customer is a worse look than a slightly generous limit.

Freemium customers get **hard throttling**, no exceptions. There is no revenue relationship to protect against a hard cutoff.

```mermaid
flowchart LR
    Req["Rejected\nrequest"] --> Resp["HTTP 429\nToo Many Requests"]
    Resp --> H1["`Retry-After`\nhow long to wait"]
    Resp --> H2["`X-RateLimit-Limit`\n`X-RateLimit-Remaining`\n`X-RateLimit-Reset`"]
    H1 --> Client["✅ Client backs off\nand retries correctly"]
    H2 --> Client
```

**How I'd say this in an interview:**
> "A rejection without an explanation just turns well-behaved clients into accidental attackers — always return 429, Retry-After, and the X-RateLimit headers so a client can self-correct. And one limit is almost never enough in practice — I would expect per-user, per-IP, and often one limit protecting the shared downstream resource itself. All of them run at once. Each one has its own fail-open/fail-closed and hard/soft policy — decided deliberately, not by accident."

---

## Where the story actually lands

```mermaid
flowchart TD
    A["Ch 1 — No limiter\none bug = $11,900"]
    B["Ch 2 — Shared Redis counter"]
    C["Ch 3 — Sliding window counter"]
    D["Ch 4 — Token bucket + Leaky bucket"]
    E["Ch 5 — Atomic INCR / Lua script"]
    F["Ch 6 — Online/offline split"]
    G["Ch 7 — Clock at the store"]
    H["Ch 8 — Sharded counters + Consistent hashing"]
    I["Ch 9 — Circuit breaker (fail-open / fail-closed)"]
    J["Ch 10 — Regional leases (Doorman)"]
    K["Ch 11 — 429 + headers + tiered limits"]

    A -->|"✅ add a bouncer\n❌ each box has its own clipboard"| B
    B -->|"✅ one shared clipboard\n❌ which window? what count?"| C
    C -->|"✅ edge-burst fixed\n❌ burst vs. smoothing are different jobs"| D
    D -->|"✅ right tool per job\n❌ read-then-write race condition"| E
    E -->|"✅ concurrency safe\n❌ Redis on critical path of every request"| F
    F -->|"✅ latency removed\n❌ whose clock is 'now'?"| G
    G -->|"✅ timestamps correct\n❌ one hot key floods its shard"| H
    H -->|"✅ hot keys handled\n❌ store outage = platform outage"| I
    I -->|"✅ resilient to failures\n❌ global quota needs cross-region call"| J
    J -->|"✅ low-latency global limits\n❌ silent 429s turn clients into attackers"| K
```

```mermaid
mindmap
  root((Why a rate limiter\nneeds all of this))
    Placement
      client SDK = a courtesy, not enforcement
      gateway = the one place all traffic passes
    Counting correctly
      per-server counters divide the limit by server count
      shared store fixes that
    Choosing a window
      fixed window snaps to zero, edge-burst risk
      sliding log is exact but memory-heavy
      sliding window counter blends two numbers
    Shaping traffic
      token bucket lets bursts through
      leaky bucket smooths to a constant rate
    Concurrency
      get-then-set races
      atomic INCR / Lua scripts fix it
    Speed
      sync check on every request is a latency tax
      local cache plus async sync takes it off the critical path
    Correctness of now
      each node's clock can drift
      trust the shared store's clock instead
    Hot keys
      one key can saturate a shard
      sharded sub-counters, summed at read time
      consistent hashing keeps a key on one node
    Failure policy
      a dead store shouldn't hang every request
      circuit breaker opens, applies fail-open/closed per rule
    Going global
      real-time cross-region checks are too slow
      local leases, reconciled asynchronously
    Talking to clients
      silent drops turn good clients into attackers
      429, Retry-After, X-RateLimit headers
```

Every real rate limiter you'll design in an interview sits *somewhere* on this chain. The skill is not reciting all eleven chapters. It is stopping where the stated requirements say to stop.

- A single-service internal API with modest scale might reasonably stop around Chapter 6.
- A public, multi-region, billed-per-call platform has to reach Chapter 9 and 10.
- If nobody has mentioned multiple regions, walking all the way to Chapter 10 unprompted reads as padding, not depth.

---

## Grill me — adversarial follow-ups

**Q1: "Why not just make the rate limit generous enough that Chapter 2's per-server overcounting never matters in practice?"**

Because the overcounting scales with however many gateway boxes you add. It is not a fixed slack you can absorb once. It is a multiplier that gets worse every time you scale out for unrelated reasons.

You would be tying your rate-limit correctness to your gateway fleet size. That is exactly the kind of hidden coupling that bites you the next time ops adds capacity for a totally different reason.

---

**Q2: "Isn't the sliding window counter just... wrong, since it assumes even distribution within the previous window?"**

Yes, honestly. It is an approximation, not an exact count. It can be fooled by a client who deliberately clusters requests at one end of a window.

But the error is small and bounded. And the cost of the exact alternative — a sliding window log — is memory proportional to traffic instead of to the limit. For almost every real system, that trade is worth making. You would only reach for the log if you had a small key space and truly needed exactness.

---

**Q3: "You picked token bucket for inbound and leaky bucket for the outbound carrier call — could you have used the same algorithm for both?"**

Not without giving something up.

The inbound side explicitly wants to let legitimate bursts through — that is what token bucket is for. The outbound side has a hard contractual ceiling with the carrier that must never spike — and only leaky bucket's constant-drain guarantee gives you that.

Using leaky bucket inbound would unnecessarily delay legitimate bursty clients. Using token bucket outbound would let PulseAPI itself violate the carrier's SLA.

---

**Q4: "Atomic INCR fixed your race condition — so why do you ever need a Lua script instead of just INCR everywhere?"**

Because `INCR` only makes *one* operation atomic. The moment your logic needs more than one step to make a decision — check a token bucket's refill math, then decide, then decrement, then set a TTL — there is a gap between those steps where another request can interleave. This is true even if each individual step is atomic on its own.

A Lua script closes that gap by running the whole sequence as one indivisible unit on the Redis server itself.

---

**Q5: "If the online/offline split from Chapter 6 lets a client slip slightly past the limit during the sync gap, isn't that just... a bug you're choosing to ship?"**

It is a deliberate, bounded trade-off — not an oversight.

The alternative is checking the real, authoritative counter synchronously on every request. That puts a real network hop on the critical path of every single request, which does not scale.

A short, bounded overage during a sync gap is a much smaller cost than adding permanent latency to 100% of traffic, just to close a gap that is usually milliseconds wide anyway.

---

**Q6: "Why does clock skew matter for a fixed window counter but you didn't mention it in Chapter 3?"**

A pure fixed-window counter only counts requests. It has no refill math and no rolling boundary computed relative to "now." It only needs to know which coarse bucket a timestamp falls into — not a precise elapsed duration.

That makes it the one algorithm that is least sensitive to clock skew.

Token bucket and sliding window counter, on the other hand, both depend on a *precise* notion of elapsed time or "now." That is exactly why clock skew bites them and not a plain fixed window.

---

**Q7: "Sharded sub-counters fixed your hot key — doesn't that just mean every key should always be sharded, all the time?"**

No. Sharding trades write contention for read cost and precision. Most keys never see enough traffic to need that trade.

Sharding every key by default means every single rate-limit check now sums N sub-keys instead of reading one — for no benefit on the 99% of keys that were never going to be hot.

The right move is detecting or predicting which keys are actually hot, and only sharding those.

---

**Q8: "Your circuit breaker fails open for general traffic — doesn't that mean an attacker could deliberately knock Redis over just to bypass the rate limiter entirely?"**

That is a real risk worth naming. It is exactly why the policy is not uniform.

The login endpoint and anything security-critical fail *closed* specifically because that attack path exists.

For general API traffic, the calculation is that losing the whole platform to a Redis outage is worse than a short window of reduced throttling. But that is a judgment call made explicitly per rule — not a blanket default. It is worth saying that trade-off out loud, unprompted.

---

**Q9: "Given this whole story, if someone just says 'design a rate limiter' cold, where do you actually start?"**

I would ask the two or three questions that decide almost everything downstream:

1. Rate limit per *what* — user, IP, or API key?
2. Is this one service, or a policy shared across many services and regions?
3. What is the cost of a client slightly exceeding the limit versus the cost of enforcing it exactly?

Then I would walk forward from a shared counter and an algorithm choice — only as far as those answers actually require. Sharding, circuit breakers, and multi-region leases are things you earn by naming a real requirement, not defaults you bolt on to sound thorough.

---

## Cheat sheet — one line per stop on the story

- **No limiter at all**: one bug or one attacker can cost real money and starve every other client on a shared resource — the whole reason a rate limiter exists.
- **Placement**: gateway/middleware, because it's the one place all traffic already passes through — client-side is a courtesy, never the enforcement point.
- **Per-server counters**: divide your real limit by however many servers you have — fix by centralizing the count in a shared store.
- **Fixed window counter**: simplest option, but can let up to 2x the limit through across a window boundary — the edge-burst problem.
- **Sliding window log**: exact, no edge case, but memory scales with traffic volume, not with the limit — expensive at real scale.
- **Sliding window counter**: two blended numbers instead of a full log — the production default, an approximation good enough for almost everything.
- **Token bucket**: lets bursts *through*, up to a cap — the right tool when the input side should tolerate spikes (Stripe, AWS API Gateway).
- **Leaky bucket**: smooths bursts *out* to a fixed rate — the right tool when the output side has a hard, constant capacity (NGINX `limit_req`).
- **Get-then-set race**: two requests can both read the same count and both get admitted — fixed by an atomic `INCR`, or a Lua script for anything multi-step.
- **Critical path**: a synchronous check on every request is a permanent latency tax — split into a fast local-cache check plus an async persisted update.
- **Clock skew**: token-bucket refill and sliding-window boundaries both depend on "now" — compute it at the shared store's clock, not each node's own.
- **Hot key**: one client can saturate the shard it happens to land on — sharded sub-counters, summed at read time, fix write contention at some read cost.
- **Consistent hashing**: keeps a key's requests landing on the same node for local caching, and only remaps a small slice when a node changes.
- **Circuit breaker**: a dead store shouldn't hang every request — trip after N failures, apply a pre-decided fail-open/fail-closed policy per rule, then probe to recover.
- **Multi-region**: real-time cross-region checks cost 50-150ms per request — split the quota into local leases, reconcile asynchronously instead (Doorman model).
- **Talking to clients**: always return `429` + `Retry-After` + `X-RateLimit-*` — a silent drop just turns a good client into an accidental attacker.
- **The meta-lesson**: every fix in this story buys one property — correctness, low memory, burst control, concurrency safety, low latency, timestamp correctness, shard fairness, resilience, or global scale — by spending a different one. Say the trade in the same sentence you propose the fix.
