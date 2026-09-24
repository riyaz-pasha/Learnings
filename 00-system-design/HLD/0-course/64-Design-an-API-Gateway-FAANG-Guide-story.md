# Design an API Gateway — The Story (narrative edition)

## What this file is

The reference file, `64-Design-an-API-Gateway-FAANG-Guide.md`, is the one to recite from. It has
the requirements, the API shapes, every trade-off table, and the master cheat sheet.

This file is a second way in: the same material told as one continuous story, in plain language.

Here's the shape of the story:

- Engineers at a company keep hitting a wall.
- They patch it.
- The patch itself creates the next wall.
- ...until we land on exactly the pipeline the reference file documents: **TLS → auth → rate
  limit → route → circuit breaker**, all local, fronted by a fleet, with config pulled from one
  versioned source.

The company, **Loopline** (a ride-hailing and delivery app), is fictional. But every wall it hits
is something a real, named system actually hit:

- The well-documented "N clients calling M microservices directly" problem.
- Steve Yegge's leaked 2011 Amazon platform memo — Bezos's mandate that every team expose
  functionality only through service interfaces. This is real, documented, and widely discussed.
- Netflix's **Zuul** gateway — Netflix's own "front door" terminology, documented on Netflix's
  engineering blog.
- Netflix's **Hystrix** circuit breaker — documented, since sunset in favor of resilience4j, but
  the pattern it popularized is still exactly what production gateways run today.
- Kong / AWS API Gateway's plugin-and-authorizer pipeline model — documented in their own docs.

I'll say clearly, every time, whether something is a documented fact or just a reasonable guess.

## The one sentence to hold onto

**The trigger phrases** for this whole topic are things like:

- *"Design the front door for our microservices."*
- *"How do we stop every service from reimplementing its own auth?"*
- *"What happens if the gateway itself goes down?"*

Keep this one sentence in your head as you read:

> An API gateway's whole job is to be the one shared front door every request passes through, so
> authentication, rate limiting, routing, and failure handling get done once, correctly, and in
> the right order — instead of forty different services each doing it slightly differently. And
> because the gateway now sits in front of literally everything, its own speed and its own uptime
> become everyone's speed and everyone's uptime.

Nothing in this story is a new algorithm. It's the same handful of ideas you already know —
auth, rate limiting, routing, circuit breaking — learning to stand in the right line, one behind
the other.

---

## Chapter 1 — Forty services, three clients, nobody agreed on how to knock

### The setup

It's 2017. Loopline started two years ago as one monolith handling rides. By now it has split
into **40 independent microservices** — pricing, driver-matching, trip-tracking, payments,
promotions, notifications. This is an ordinary outcome of decomposing a monolith.

Three clients call these 40 services **directly**:

- The **Rider app**
- The **Driver app**
- An internal **Ops dashboard**

Each client holds its own hardcoded list of service hostnames. Each client also independently
decides how to attach an auth token. Nothing ties these decisions together.

```mermaid
flowchart TB
    subgraph Clients["Three clients — each with its own hardcoded host list"]
        Rider["Rider app"]
        Driver["Driver app"]
        Ops["Ops dashboard"]
    end
    subgraph Services["40 independent microservices"]
        Pricing["FareEngine<br/>(was PricingService)"]
        Match["Driver-Matching"]
        Etc["...38 more services..."]
    end
    Rider --> Pricing
    Rider --> Match
    Driver --> Pricing
    Driver --> Etc
    Ops --> Pricing
```

### The break

The pricing team renames `PricingService` to `FareEngine` and moves it during a planned
migration. Here's the problem: all three clients have the *old* hostname baked into their own
code — in three separate places, owned by three teams that don't routinely talk to each other.

The old hosts get decommissioned on schedule, as planned. The result:

- For **22 minutes** `[illustrative]`, ride pricing is broken for every client at once.
- Not because pricing itself was broken — but because "who talks to pricing" was never owned in
  one place. It was silently duplicated three times, and only two of those three copies got
  updated (in this story, none did).

### A second, quieter incident

The same month, a new `PromotionsService` ships. It has no shared auth check to plug into —
because there **is** no shared auth check. Every client just assumes "someone" validates the
bearer token.

A security review finds the service has been accepting **unauthenticated requests for 9 days**
`[illustrative]`, leaking active promo codes to anyone who guessed the URL.

### Why this keeps happening

Ask yourself: *why does every client need all 40 addresses, and why does every service reinvent
its own auth?*

Because nothing **owns** "how do you get into this system." That job got smeared across every
client and every service, instead of living in one place.

This is close to the real scenario in Steve Yegge's leaked 2011 Amazon memo (widely discussed,
publicly archived), describing Bezos's mandate that every team's functionality be exposed *only*
through a service interface — no direct pipes. Point-to-point sprawl like Loopline's doesn't get
better on its own as the service count grows. It gets worse.

### The fix: one shared front door

Put one **shared front door** in front of all 40 services. Every client talks only to *that*.

**The analogy for the rest of this story:** think of an **apartment building's lobby and
doorman**, replacing 40 apartments that each have their own lock and buzzer. The doorman checks
IDs once, the same way every time, no matter which apartment the visitor is headed to.

This is literally what Netflix built and named **Zuul** — their own documented term for it is
"the front door for all requests" into Netflix's infrastructure, rolled out as they scaled into
hundreds of microservices.

```mermaid
flowchart LR
    Rider2["Rider app"] --> GW["Gateway<br/>(the doorman)"]
    Driver2["Driver app"] --> GW
    Ops2["Ops dashboard"] --> GW
    GW --> Pricing2["FareEngine"]
    GW --> Match2["Driver-Matching"]
    GW --> Etc2["...38 more services..."]
```

### New problem, visible on day one

Loopline stands up exactly **one** gateway process. Three weeks in, that box's NIC card fails at
6:14pm on a Friday — peak demand.

Every client, for every service, goes down **at once**. None of the 40 backends are actually
broken. The single front door to all of them just stopped existing.

This is the exact single point of failure this fix was meant to prevent — just moved one level
up.

### How I'd say this in an interview

> "Forty services and multiple clients calling them directly is the classic N-times-M problem —
> every client needs every address, every service reinvents auth slightly differently, which is
> exactly what Amazon's internal 'everything must be a service interface' mandate was written to
> stop. A shared gateway fixes the duplication, but the instant everything centralizes into one
> door, that door becomes the new single point of failure."

---

## Chapter 2 — Many doormen, same lobby

### The fix

This is the fix you'd reach for with any stateless service that became a SPOF: run a **fleet** of
identical gateway instances behind a load balancer, instead of one box.

This isn't gateway-specific. It's the same horizontal-scaling-plus-failover discipline any shared
piece of infrastructure gets — and it's exactly how real gateway products are actually deployed
(AWS API Gateway, Kong, Netflix's Zuul fleet).

**The analogy, continued:** the lobby gets a rotating team of doormen, all trained identically,
standing behind a reception desk (the load balancer) that hands each visitor to whoever's free.
Lose one doorman, the desk routes the next visitor elsewhere. Nobody notices.

```mermaid
flowchart TB
    Client["Clients"] --> LB["Load balancer"]
    LB --> G1["Gateway instance 1"]
    LB --> G2["Gateway instance 2"]
    LB --> G3["Gateway instance N"]
    G1 --> Backends["40 backend services"]
    G2 --> Backends
    G3 --> Backends
```

### Doing the math on fleet size

Here's the sizing, step by step:

| Quantity | Value |
|---|---|
| Throughput per gateway instance before CPU becomes the bottleneck | ~**10,000 requests/sec** `[illustrative]` |
| Peak platform traffic | **200,000 requests/sec** `[illustrative]` |
| Instances needed (200,000 / 10,000) | **20–25 instances** |

This is a boring, straightforward horizontal-scaling number. It works cleanly because each
instance's work is stateless and independent — no instance needs to know what any other instance
is doing.

### New problem, the next sprint

Someone finally looks at what order the pipeline runs its checks in.

The engineer who built v1, shipping fast, wired **rate limiting before authentication**. Rate
limiting by IP was the fastest thing to bolt on; auth felt like it could come "whenever." Nobody
flagged the ordering as a real decision — it just happened that way.

### How I'd say this in an interview

> "The SPOF problem has the boring, standard answer — a horizontally scaled, load-balanced,
> stateless fleet. What's not boring, and what actually causes bugs, is the *order* the pipeline
> runs its checks in inside each instance — that's next."

---

## Chapter 3 — Checking the boarding pass before checking the passport

### Loopline's early pipeline

Rate limit by raw IP, **then** authenticate.

The reasoning at the time: rate limiting felt like "just infrastructure," and auth felt like "the
real security check" — so why not do the cheap one first?

### Two real bugs, same week

**Bug 1 — false positives from shared IPs.**

- Mobile carriers put large numbers of customers behind a few shared IPs, via carrier-grade NAT.
  This is a real, well-known fact about how mobile networks work.
- Loopline's limit is **100 req/min per IP**.
- One evening, roughly **600 riders** on the same carrier share one NAT'd IP `[illustrative]`.
- Combined, those 600 riders blow past 100/min in under 90 seconds.
- The gateway throttles **all 600 together** — 599 innocent riders get punished for the crime of
  sharing a stranger's IP address.

**Bug 2 — false negatives from IP rotation.**

- The same week, a bot is brute-forcing promo codes against `PromotionsService`.
- It simply rotates across **5,000 IPs** `[illustrative]`.
- It stays under the per-IP limit from any single IP, even though its *total* volume is enormous.
- The limit is keyed on something trivially rotatable, and not tied to a real identity.

```mermaid
flowchart TB
    A["Rate limit BY RAW IP<br/>(identity not checked yet)"]
    A -->|"600 riders share one NAT'd IP"| B["ALL 600 throttled together<br/>-- false positive"]
    A -->|"bot rotates across 5,000 IPs"| C["Bot never hits the limit<br/>-- false negative"]
```

### Is the rate limiter broken?

No — the rate limiter itself is fine. The problem is **what it's keyed on**: an identity decision
made before identity actually exists.

It's like a gate agent deciding your upgrade history by which shuttle bus dropped you off —
strangers share a shuttle, and anyone can just take a different one next time.

### The fix: flip the order

**Authenticate first, then rate-limit by that validated identity.** Check the passport before
anything downstream — bag limits, lounge access, flight history — gets decided.

```mermaid
flowchart LR
    A["Request"] --> TLS["TLS termination"]
    TLS --> Auth["Authenticate<br/>(validate identity)"]
    Auth --> RL["Rate limit BY<br/>VALIDATED IDENTITY"]
    RL --> Route["Route to backend"]
```

### What the fix actually changes

Walk through both bugs again, with the new order:

- **The 600 riders:** each of them now authenticates with their own ID first. Each rider gets
  their **own** 100-req/min budget. The false-positive throttling disappears, because "sharing an
  IP" is no longer what the limiter looks at.
- **The bot:** it still needs *some* identity to authenticate as before it can even reach the rate
  limiter. That identity — not a rotatable IP — is what gets flagged. Security can actually act on
  an identity; they can't meaningfully act on "one of 5,000 IPs."

### New problem, once everyone's happy

Benchmarking the corrected pipeline shows gateway-added latency has crept up to **28ms/request**
`[illustrative]`. At 200,000 requests/sec, that 28ms gets taxed onto every single request, to
every one of the 40 services, platform-wide.

### How I'd say this in an interview

> "Stage order is a correctness bug, not a style choice — rate-limiting before auth keys the
> limit on something spoofable or shareable. Authenticate first, then everything downstream uses
> the real identity. Fixing the order doesn't make the pipeline fast, though — that's separate."

---

## Chapter 4 — The auth check that calls home before answering the door

### Where the 28ms comes from

The 28ms traces to exactly one thing: auth validates every token by looking it up in a **shared
session database** — a real network round-trip, on every single request, taking **20–25ms**
`[illustrative]`.

Everything else in the pipeline — the routing lookup, the rate-limit counter — is under 1ms,
purely local. One stage that leaves the box dominates the whole latency budget by roughly
**20–40x**.

```mermaid
flowchart TB
    A["Every pipeline stage"] --> B{"Does this stage make<br/>an external call per request?"}
    B -->|"Yes -- auth does a session DB lookup"| C["20-25ms<br/>dominates everything else"]
    B -->|"No -- purely local/in-memory"| D["Under 1ms each"]
```

### The fix: stop calling home

Stop asking a database "is this still valid" on every request. Instead:

- Issue **short-lived, signed tokens** (JWT-shaped).
- The gateway verifies the signature **locally** — no network call — because the signature *is*
  the proof.
- A short expiry (say **15 minutes**) bounds how long a stolen token stays dangerous, since
  there's no live revocation check.

This is exactly what real gateways do:

- AWS API Gateway's Lambda authorizers cache their result for a TTL, specifically to skip a
  per-request call.
- Kong's JWT plugin verifies signatures locally — no DB round-trip.

Both of these are documented behaviors, not guesses.

The same idea applies to rate limiting: use local or tightly-bounded in-memory counters, never a
slow centralized store checked synchronously per request.

### The reworked latency budget

With every stage local, here's what the pipeline costs, stage by stage:

| Stage | Cost |
|---|---|
| TLS | ~0.5ms |
| Local auth (signature check) | ~0.5–1ms |
| Local rate-limit check | ~0.5–1ms |
| Local routing lookup | ~0.2ms |
| Local circuit-breaker check | ~0.1ms |
| **Total** | **~2–3ms** `[illustrative, matching the reference guide's estimate]` |

That's a **10x+ improvement**, purely from removing the one non-local stage.

### New problem, immediately

If routing and policy checks are local, in-memory lookups, they're checking against a **routing
table** and a **policy table**. Where does that table actually come from?

If the answer is "ask a config service per request," that's the exact same forbidden external
call — just smuggled back in through a different door.

### How I'd say this in an interview

> "Once stage order is fixed, audit every stage for external calls per request — the gateway sits
> in front of every request to every backend, so one non-local stage doesn't cost itself, it costs
> the whole platform, multiplied. Short-lived signed tokens and local counters get you back to
> low-single-digit milliseconds. But 'local' only works if the data those checks depend on gets to
> every instance some other way — that's next."

---

## Chapter 5 — Restocking the vending machine on a schedule, not calling the warehouse per sale

### The problem to solve

Every gateway instance needs its own local copy of a **routing table** and a **policy table**.

These tables do change — new services ship, tiers get upgraded — but only a handful of times a
day. Not per request.

### How does a local table stay both fast and current?

This is the same shape as any config-distribution problem. The answer:

- One **authoritative, versioned config store**.
- Every gateway instance **pulls its own copy on its own schedule** — say, every **10 seconds**.
- Never a synchronous per-request call.
- Never gateway-to-gateway sync.

**The analogy:** a vending machine doesn't call the warehouse before every purchase to check
today's price. It sells off whatever's on the shelf, and a delivery truck restocks it and updates
the price sheet on a schedule. Every machine in the building restocks independently from the same
warehouse. The machines never coordinate with each other directly.

```mermaid
flowchart TB
    A["Route or policy change happens"] --> B[("ONE authoritative,<br/>versioned config store")]
    B --> C1["Instance 1<br/>pulls every 10s"]
    B --> C2["Instance 2<br/>pulls every 10s"]
    B --> C3["Instance N<br/>pulls every 10s"]
    C1 -.->|"NEVER syncs directly"| C2
```

### How big is this data, really?

The whole routing table, plus every tier's policy, is a few megabytes — even at 40 services and
thousands of accounts `[illustrative, matching the reference guide]`. That's tiny.

The real engineering effort here isn't about size. It's about **propagation correctness** — making
sure every instance actually gets the update, and knowing when one doesn't.

### New problem, three months later, during a migration

Loopline cuts `FareEngine` over from v1 to v2 hosts.

- One instance's config-pull job silently stalls for **40 minutes** `[illustrative]`.
- That instance keeps serving its stale, pre-cutover table the whole time.
- During that window, roughly **8% of the fleet's traffic** `[illustrative]` keeps routing to the
  now-decommissioned v1 hosts.
- Those v1 hosts return connection-refused.
- Nobody notices until complaints show up.

### The fix

The fix here isn't a new mechanism — it's discipline. Monitor each instance's **config version
lag** against the latest published version as an alertable metric. Do this the same way you'd
watch replication lag on a database replica.

### How I'd say this in an interview

> "Config distribution to a gateway fleet is the same pattern as distributing any small,
> frequently-changing config to a fleet — one versioned source, independent pulls, never
> node-to-node sync. What actually needs discipline is monitoring version lag per instance,
> because a silently-stuck puller is what turns a routine migration into an outage nobody sees."

---

## Chapter 6 — One big customer's burst, drained from everyone else's tank

### The incident

Loopline signs a corporate fleet partner. A bug in their integration fires **12,000
pricing-lookup requests in 8 seconds** `[illustrative]` — far more volume than any individual
rider generates.

Here's the problem: the rate limiter has a single **shared counter for the whole `FareEngine`
route**, not one counter per customer.

- The burst blows straight through that shared budget.
- **Every regular rider's** pricing requests get throttled too.
- Not because riders did anything wrong — but because one customer's spike consumed a budget that
  was never theirs alone to spend.

```mermaid
sequenceDiagram
    participant Partner as Fleet partner (burst)
    participant Rider as Regular rider
    participant GW as Gateway
    participant RL as Rate limiter<br/>(ONE shared counter)

    Partner->>GW: 12,000 requests in 8 seconds
    GW->>RL: check limit, key = "FareEngine" (shared)
    RL->>RL: shared budget depletes
    Rider->>GW: one normal pricing request
    GW->>RL: check limit, SAME shared key
    RL-->>GW: throttled -- shared budget already gone
    Note over RL: the rider did nothing wrong
```

### Naming the problem

This is the classic **noisy-neighbor** problem.

### The fix

**Scope every counter by tenant/customer identity — never by route alone.**

Once the key is `tenant:fleet-partner-42` instead of just `"FareEngine"`, the partner's burst can
only exhaust *its own* bucket. Every other tenant's budget — including every rider's — stays
untouched.

### New problem, once isolation is solid

A different kind of failure shows up next. This one isn't about limits at all — it's a backend
simply getting slow, with the gateway not reacting to that fact in any useful way.

### How I'd say this in an interview

> "A shared, unscoped rate-limit counter is a real multi-tenant bug, not a minor inefficiency —
> one tenant's burst can starve everyone sharing that bucket. Every policy needs a mandatory
> tenant-scoped key, not an optional convention someone can forget."

---

## Chapter 7 — The doorman who keeps knocking on a door nobody's answering

### The incident

`DriverMatchService` has a bad afternoon. Its own database gets overloaded, and its response time
climbs from a normal **80ms** to **30 seconds** `[illustrative]` — without ever returning an
error. It's just slow.

The gateway has no special handling for this: it forwards every request and waits.

Here's how that cascades, step by step:

1. Traffic to this backend is **500 requests/sec** `[illustrative]`.
2. Each of those requests now hangs for 30 seconds instead of 80ms.
3. Connections pile up across the fleet faster than they drain.
4. Within roughly **90 seconds**, the fleet's own connection pools saturate — **because of this
   one backend**.
5. Unrelated, perfectly healthy requests — payments, notifications — start queuing up behind them
   too, because they share the same exhausted gateway resources.

### Why does the gateway keep hammering a dead backend?

Because nothing tracks "how has this backend been behaving lately." Each request is handled
statelessly, with no memory of the last 100 calls to that same backend.

### The fix: a circuit breaker

Add a **circuit breaker**, per backend, tracking a rolling failure/slowness rate. This is the
pattern Netflix's **Hystrix** made famous — documented, and even though the library itself has
since been retired in favor of newer tooling, the shape it popularized is still what production
gateways run today.

Three states:

- **Closed** — normal operation.
- **Open** — the failure/slowness threshold has been crossed. Stop forwarding requests. Fail fast
  instead of waiting on a request that's doomed anyway.
- **Half-open** — after a cooldown period, send exactly one trial request to check whether the
  backend has recovered.

```mermaid
stateDiagram-v2
    [*] --> Closed: normal operation

    Closed --> Open: failure/slowness rate exceeds threshold
    Open --> HalfOpen: after cooldown, send one trial request
    HalfOpen --> Closed: trial request succeeds
    HalfOpen --> Open: trial request fails
    Open --> Open: fast-fail every other request while open
```

```mermaid
sequenceDiagram
    participant Client
    participant GW as Gateway
    participant CB as Circuit breaker
    participant Match as DriverMatchService<br/>(degraded)

    Note over CB: threshold already exceeded -- circuit is OPEN
    Client->>GW: ride-match request
    GW->>CB: check circuit state
    CB-->>GW: OPEN -- do NOT forward
    GW-->>Client: fast-fail in milliseconds, no 30s wait
```

Once the circuit is open, the gateway stops forwarding requests and returns a fast failure in
milliseconds. Every client benefits immediately — instead of each one independently
rediscovering the same 30-second hang for itself.

### New problem, once the pipeline is solid all around

The pipeline is now fast, correctly ordered, config-distributed, tenant-isolated, and
circuit-broken. But Loopline's internal services have migrated to gRPC for performance. The Rider
and Driver apps, and every partner integration, still speak plain REST/JSON — and always will.

### How I'd say this in an interview

> "A circuit breaker per backend stops one degraded service from draining capacity that every
> *other* service on the same fleet needs — the Hystrix pattern, still the shape every production
> gateway uses even though the library itself is retired. Composing it into the gateway means
> every client benefits the moment it trips, not just whoever discovers the timeout first."

---

## Chapter 8 — Translating at the door so nobody inside has to speak two languages

### The mismatch

External callers speak REST/JSON. Internal services, after the migration, speak gRPC.

*Should every external client learn gRPC just because the backends switched?*

No. That would push an internal implementation detail onto integrations Loopline doesn't even
control.

### The fix: protocol translation as a pipeline stage

Add **protocol translation** as an explicit stage in the pipeline:

- REST/JSON comes in from the client.
- The gateway translates it to gRPC for the backend call.
- The gateway translates the response back to JSON before returning it.

The gateway is the natural place to do this, since it's already the one spot every request
passes through. Translating once, there, beats making all 40 services support two protocols
each.

```mermaid
flowchart LR
    Client["Client<br/>(REST/JSON)"] --> GW["Gateway"]
    GW -->|"translate JSON -> gRPC"| Backend["Backend<br/>(gRPC)"]
    Backend -->|"translate gRPC -> JSON"| GW
    GW --> Client
```

### The new problem, immediately

Chapter 4's rule doesn't get a pass here just because translation "feels different."

Translation costs real CPU — it's parsing and re-serializing every single request and response,
sitting in the same hot path as every other stage.

Skip the latency-budget discipline on this one because it "feels like plumbing, not security,"
and it quietly regresses the platform back toward the tens of milliseconds that Chapter 4 worked
to eliminate.

That's exactly why the reference guide treats translation as a stretch-goal stage with its own
explicit budget line — not a free add-on.

### How I'd say this in an interview

> "Protocol translation belongs at the gateway because it's the one place that already sees every
> request. But it's still a pipeline stage — it needs its own latency budget, monitored the same
> way as auth, rate limiting, and routing, not exempted because it feels like plumbing."

---

## Where the story actually lands

```mermaid
flowchart LR
    A["Ch1: N clients, M services,<br/>calling each other directly"]
    B["Ch2: stateless<br/>fleet + load balancer"]
    C["Ch3: auth BEFORE<br/>rate limit"]
    D["Ch4: every stage<br/>runs locally"]
    E["Ch5: versioned config,<br/>independent pull"]
    F["Ch6: rate limits<br/>scoped per tenant"]
    G["Ch7: circuit breaker<br/>per backend"]
    H["Ch8: protocol translation,<br/>with its own budget"]

    A -->|"fixes duplication<br/>breaks: gateway is a SPOF"| B
    B -->|"fixes the SPOF<br/>breaks: wrong stage order"| C
    C -->|"fixes real identity<br/>breaks: auth calls a DB, 28ms"| D
    D -->|"fixes latency, ~2-3ms<br/>breaks: where do tables come from?"| E
    E -->|"fixes fast + current<br/>breaks: shared counter, noisy neighbor"| F
    F -->|"fixes isolation<br/>breaks: hammers a dead backend"| G
    G -->|"fixes fast-fail<br/>breaks: REST clients, gRPC backends"| H
```

```mermaid
mindmap
  root((Why a gateway<br/>needs all of this))
    Composition, not invention
      auth, rate limit, routing, circuit breaking
      all borrowed -- this topic is ORDER
    One front door
      N x M direct calls = unmanageable duplication
    The gateway's own risk
      one box = new SPOF
      stateless fleet + LB fixes it
    Stage order is correctness
      IP-before-auth is spoofable or shareable
      auth first, then rate limit by real identity
    Latency budget
      one non-local stage dominates everything
      signed tokens + in-memory counters keep it local
    Config distribution
      local tables need a source
      versioned snapshot, independent pull, never sync
    Multi-tenant isolation
      shared counter = noisy neighbor
      key must include tenant identity
    Backend health
      gateway hammers a dead backend forever otherwise
      closed / open / half-open, fail fast
    Protocol translation
      REST clients, gRPC backends
      still needs its own latency budget
```

Every real gateway you design in an interview sits *somewhere* on this chain:

- A small internal platform with one client and five services might reasonably stop around
  Chapter 4.
- A large, multi-tenant, external-facing platform has to reach Chapter 6 and Chapter 7.
- If nobody's mentioned mixed protocols, walking all the way to Chapter 8 unprompted reads as
  padding, not depth.

---

## Grill me — adversarial follow-ups

**Q1: "Why not have each of the 40 services validate auth itself — isn't that more secure,
defense in depth?"**

It sounds safer, but it means 40 slightly different implementations of the same check — and
history (the `PromotionsService` incident) shows "slightly different" quickly becomes "forgot it
entirely." Centralizing doesn't forbid a service from double-checking something service-specific;
it just makes the baseline impossible to accidentally skip.

**Q2: "What actually breaks if routing happens before rate limiting instead of after?"**

Not correctness, mostly wasted work — you'd have spent a routing lookup on a request that was
about to get rejected anyway. The real rule is narrower than "this exact order": cheaper
rejection checks before expensive ones, and each stage depending only on information an earlier
stage established.

**Q3: "Isn't 'add more gateway instances' just delaying the SPOF, not fixing it — what if the
load balancer dies?"**

Fair — the load balancer needs the same no-single-point-of-failure treatment, recursively, one
layer up (multiple LB nodes, health-checked, DNS or anycast failover). You do bottom out at
physical redundancy somewhere; the goal isn't zero single nodes, it's no *one* node's failure
taking down the platform.

**Q4: "Doesn't local signature verification skip checking if the token's been revoked?"**

Yes, deliberately — it trades instant revocation for speed. A compromised token stays valid for
at most its short expiry, not forever. Genuine instant-revocation needs are a real requirement to
name explicitly, because that means accepting a slow, non-local check per request on purpose.

**Q5: "Rate-limit counters need to agree across 20+ instances — doesn't that reintroduce the
external-call problem Chapter 4 just eliminated?"**

It's the one stage that genuinely can't stay purely local, because a user's real limit must hold
regardless of which instance handles any given request. The fix is a fast, tightly-bounded shared
cache, not a full database — a small, deliberate exception, because a per-instance-siloed counter
would let anyone evade their limit just by getting load-balanced around.

**Q6: "If config propagation takes 10 seconds, isn't that a security hole for revoking a
compromised key instantly?"**

For routine changes, 10 seconds is a fine trade for simplicity. For an emergency revocation,
that's a separate, faster "kill switch" path — the reference guide calls this out as a stretch
goal precisely because routine and emergency changes have different latency needs, and conflating
them either over-engineers the routine case or makes emergencies too slow.

**Q7: "Why scope rate limits per tenant instead of just giving the big partner their own
dedicated gateway?"**

You could, for a large enough partner — but it doesn't scale as a general policy, since you'd
stand up a dedicated gateway per big customer. Tenant-scoped counters solve isolation for
arbitrarily many tenants sharing one fleet, without new infrastructure every time you sign
someone new.

**Q8: "The circuit breaker opens per backend — what stops a brief 5-second blip from getting
treated like a full outage?"**

The half-open state: after a cooldown, exactly one trial request checks recovery, and if it
succeeds the circuit closes right away. Threshold and cooldown are tunable specifically so a blip
doesn't get treated the same as a sustained failure.

**Q9: "Given this whole story, if someone says 'design an API gateway' cold, where do you
start?"**

Reframe it out loud first: not a new algorithm, but correctly composing auth, rate limiting,
routing, and circuit breaking, in order, without the gateway's own latency or availability
becoming the platform's bottleneck. Then walk only as far as the interviewer points — ordering and
latency budget are near-universal; config distribution, multi-tenancy, and protocol translation
are earned by a specific stated requirement, not defaults.

**Q10: "What's the single biggest thing separating a good gateway design from a bad one here?"**

Catching, unprompted, that the gateway sits on every request to every backend — so anything that
would be a minor detail inside one service (a DB call, a wrong check order) becomes a
platform-wide multiplier the moment it's inside the gateway. Everything else in this story is
that one observation, applied five times.

---

## Cheat sheet — one line per stop on the story

| Stop | One-line takeaway |
|---|---|
| **N clients calling M services directly** | Every client needs every address, every service reinvents auth — the sprawl Amazon's internal "everything must be a service interface" mandate was written to stop. |
| **Shared gateway** | One front door (Netflix's own term for Zuul) replacing duplicated per-service auth/routing/logging. |
| **Gateway fleet + load balancer** | The gateway is a new SPOF the moment it's introduced — fix it like any other critical stateless service, with horizontal scale plus failover. |
| **Stage order — auth before rate limit** | Limiting by raw IP before auth keys the limit on something spoofable or shareable; authenticate first, then limit by the real identity. |
| **Latency budget — every stage local** | One non-local stage can dominate the pipeline by 20-40x; short-lived signed tokens and in-memory counters keep the total in low single-digit milliseconds. |
| **Config distribution** | One authoritative versioned source, every instance pulls independently on its own schedule — never instance-to-instance sync — and monitor version lag as an alertable metric. |
| **Multi-tenant rate-limit isolation** | Every counter key must include tenant identity, or one tenant's burst starves everyone else — a real isolation bug, not a minor inefficiency. |
| **Circuit breaker per backend** | Closed / open / half-open, fail fast instead of hanging — the Hystrix pattern, still the shape every production gateway uses. |
| **Protocol translation** | Belongs at the gateway because it already sees every request, but it's still a pipeline stage with its own latency budget, not a free add-on. |
| **The meta-lesson** | Nothing here is a new mechanism — every fix is auth, rate limiting, routing, or circuit breaking, borrowed from elsewhere, learning to run in the right order, fully local, on a fleet that isn't itself a single point of failure. |
