Teach me everything about REST API version. Why we need it? How to Version it properly? How to not? How does new version should be introduced? What happens to the old one? How long we should keep the old one? Should we have a global version or version at API level? What are the production best practices. Teach me everything about from scratch. Basic to advanced. Teach me chapter by chapter if required.

---

Great topic — this is one of those things everyone does badly at least once before they learn it properly. Given how deep this goes, I'll do it chapter by chapter so it actually sticks, rather than dumping everything at once. Let's start from first principles.

---

## Chapter 1: Why Do We Even Need API Versioning?

### The core problem

An API is a **contract**. The moment you publish it, other people (or other teams, or your own mobile app that's already in users' hands) start writing code against exactly what you exposed — field names, types, status codes, behavior.

The problem: software evolves. You will eventually need to:

- Rename a field (`user_name` → `username`)
- Change a data type (`price: "9.99"` string → `price: 9.99` number)
- Remove a field nobody should be using anymore
- Change business logic (e.g., tax now calculated differently)
- Restructure a whole resource

If you make *any* of these changes to a live endpoint, every existing client breaks the moment you deploy — with zero warning. Imagine:

- A mobile app in production, already installed on 2 million phones, parsing a JSON field that no longer exists → app crashes
- A partner's billing system misreading a changed price format → wrong invoices go out

This is called a **breaking change**. Versioning exists to let you make breaking changes *without breaking your existing consumers immediately*.

### The analogy I'll use throughout

Think of your API like a **restaurant menu that's also a legal contract**. Once a customer orders "Pasta v1" and it arrives with tomato sauce, you can't secretly start serving it with pesto tomorrow — regulars will be furious. But you *can* put "Pasta v2 (now with pesto)" on the menu as a new item, while v1 still exists for a while, then eventually retire v1 with plenty of notice.

### Not all changes need a new version

This is the first big lesson people miss: **versioning is only for breaking changes.** If you version on every tiny change, you'll have version fatigue (v47, anyone?) and nobody will trust or track it properly.

**Non-breaking (safe, no new version needed):**
- Adding a new optional field to a response
- Adding a new endpoint entirely
- Adding a new optional query parameter
- Relaxing a validation rule (accepting more than before)
- Adding new enum values *if clients are expected to handle unknowns gracefully* (this one's debatable — more in a later chapter)

**Breaking (needs a new version, or careful handling):**
- Removing or renaming a field
- Changing a field's type or meaning
- Changing required parameters
- Changing status codes or error formats
- Changing the URL structure of a resource
- Changing default behavior

### Quick mental model to end Chapter 1

```
Client's expectation of the contract  ≠  Server's new behavior
                → BREAKING CHANGE → needs versioning strategy
```

Versioning is fundamentally a way to manage **change over time while multiple contract expectations exist simultaneously in the wild**.

---

That's Chapter 1 — the "why." Next up in Chapter 2, I'll cover the actual **strategies for how to version** (URI path, query param, header-based, content negotiation) with the real tradeoffs of each, including which ones are considered anti-patterns today.

Want me to continue to Chapter 2, or do you want to sit with Chapter 1 / ask questions first?

---

## Chapter 2: How to Actually Version — The Strategies

There are four real approaches you'll encounter in the wild. Let's go through each with the tradeoffs, using the restaurant menu analogy from Chapter 1.

### 1. URI Path Versioning

```
GET /v1/users/42
GET /v2/users/42
```

**How it works:** The version is baked directly into the URL path.

**Pros:**
- Dead simple to understand and explore in a browser
- Trivially cacheable (different URL = different cache entry, no special cache-key logic needed)
- Easy to route at the infrastructure level — load balancer or API gateway can send `/v1/*` to one set of servers and `/v2/*` to another, with zero application code involved
- Self-documenting in logs, curl commands, Postman collections

**Cons:**
- Purists argue it violates REST's idea that a URI identifies a *resource*, not a *version of a resource* — `/v1/users/42` and `/v2/users/42` are "the same resource" philosophically, but now have different identifiers
- Encourages "copy the whole controller" duplication if not architected carefully
- URLs multiply — every doc, every SDK, every bookmark now has a version baked in

**Verdict:** This is by far the most common approach in production (Stripe, Twitter/X historically, GitHub). It's pragmatic and wins because of how well it plays with infrastructure.

### 2. Query Parameter Versioning

```
GET /users/42?version=1
GET /users/42?version=2
```

**Pros:** Same URL "root," easy to default (`no version param = latest` or `= v1` for safety)

**Cons:**
- Easy to forget/omit accidentally → client silently gets a different version than intended
- Caching gets messier (some caches don't key on query params consistently)
- Feels bolted-on rather than a first-class concept

**Verdict:** Rarely used as the *primary* strategy today. Sometimes used as a fallback/override alongside another method.

### 3. Header-Based Versioning (Custom Header)

```
GET /users/42
Headers: X-API-Version: 2
```

**Pros:**
- Keeps URLs clean — `/users/42` always means the same resource, philosophically more "correct" REST
- Version is metadata about *how* to interpret the resource, not part of its identity

**Cons:**
- Invisible in browser address bars, harder to explore/debug manually (you need curl/Postman, can't just paste a link)
- Caching by URL alone breaks — intermediary caches (CDNs, proxies) need to be told to vary cache by this header (`Vary: X-API-Version`), which many are misconfigured to ignore
- Easy for clients to forget to set it, silently getting default/latest behavior

**Verdict:** Used by companies who care about REST purity (Microsoft's API guidelines lean this way for some services). Common in enterprise/internal APIs where clients are well-governed.

### 4. Content Negotiation / Media Type Versioning (the "correct REST" way)

```
GET /users/42
Headers: Accept: application/vnd.myapp.v2+json
```

**How it works:** You version the *representation* of the resource via the `Accept` header, using a custom media type — this is what the original REST/HATEOAS purists (like Roy Fielding) consider "doing it right," because the URL identifies the resource, and content negotiation determines its *representation format*.

**Pros:**
- Philosophically the "correct" REST approach
- Same URL, resource identity never changes
- Can version individual *resources* independently (not the whole API at once) — e.g. `vnd.myapp.user.v2+json` for just the user resource

**Cons:**
- Significantly harder for consumers to use — they must know to set this exact header correctly, no browser-friendly way to try it
- Poor tooling support historically (though better now)
- Steep learning curve for API consumers, especially external/public ones
- GitHub actually uses this for parts of their API and it's considered a masterclass example, but even they layer it with other hints

**Verdict:** Technically the "purest," but the operational/DX (developer experience) cost is high. Mostly seen in APIs with sophisticated, well-resourced client bases (GitHub, some banking/enterprise APIs).

### Quick comparison table

| Strategy | Explorable in browser | Caching friendliness | REST purity | Real-world adoption |
|---|---|---|---|---|
| URI Path (`/v1/`) | ✅ Excellent | ✅ Excellent | ❌ Debated | ⭐⭐⭐⭐⭐ Most common |
| Query Param (`?v=1`) | ✅ Good | ⚠️ Mixed | ❌ Weak | ⭐⭐ Rare as primary |
| Custom Header | ❌ Poor | ⚠️ Needs `Vary` header | ✅ Good | ⭐⭐⭐ Enterprise-common |
| Media Type / Accept header | ❌ Poor | ✅ Good (with `Vary`) | ✅✅ Best | ⭐⭐ Niche but respected |

### The practical answer

Almost every large-scale, widely-adopted public API you've used (Stripe, Twilio, GitHub's v3, Shopify) uses **URI path versioning** as the primary mechanism, sometimes layered with a header for finer-grained control (Stripe actually does date-based versions via header, which we'll cover in a later chapter as an advanced pattern).

**Rule of thumb for you to remember:** if you're building a public API with external consumers who'll curl/explore/debug manually — use URI path versioning. It's the pragmatic industry default for good reason.

---

Chapter 2 done — the **how**. Next in Chapter 3, I'll cover **what NOT to do** (the classic versioning anti-patterns and mistakes people make), before we move into the harder stuff: how to introduce a new version safely, deprecation strategy, how long to support old versions, and global-vs-per-resource versioning.

Continue to Chapter 3?

---

## Chapter 3: How NOT to Version — Classic Anti-Patterns

This chapter is arguably more valuable than the "how to" — most real-world API pain comes from these mistakes.

### Anti-pattern 1: No versioning strategy at all ("we'll just be careful")

Teams say "we'll just never break anything." This works for about six months. Then someone needs to fix a genuinely wrong field type, or the business changes a rule, and now you're stuck either:
- Breaking everyone silently, or
- Never fixing the mistake, forever, compounding technical debt

**Lesson:** Decide your versioning strategy *before* you have your first external consumer, not after.

### Anti-pattern 2: Versioning too granularly (churn)

```
/v1/users
/v1.1/users
/v1.2/users
/v2/users
```

If every small change bumps a version, consumers can't tell what's actually different, and you end up maintaining a combinatorial explosion of near-identical versions. Nobody can reason about "what changed between v1.2 and v1.3" without reading a changelog line by line.

**Lesson:** Only bump the *major, public-facing version* for breaking changes (Chapter 1's definition). Everything else ships silently into the current version.

### Anti-pattern 3: Mixing breaking and non-breaking changes in one release

A team ships v2 with 15 changes bundled — 2 are breaking, 13 are just nice improvements. Now every client that *only* cares about the 2 breaking changes still has to absorb and test against all 15, delaying their migration and increasing risk.

**Lesson:** Keep version bumps minimal and focused. Ship non-breaking improvements continuously into the *current* version; only bump major version for the breaking part.

### Anti-pattern 4: "v2" that's a totally different API in disguise

Sometimes teams use a version bump as an excuse to redesign everything — different auth model, different pagination style, different error format, different resource naming. Now migrating isn't "update a few fields," it's "rewrite your entire integration." This kills migration rates — nobody moves, and you end up supporting "v1" forever anyway.

**Lesson:** Version bumps should be as small and targeted as possible. Big redesigns should still preserve as much shared shape as feasible.

### Anti-pattern 5: No deprecation communication

Team ships v2, and three weeks later just shuts off v1 with no warning. Every integrated partner's system breaks in production, support tickets flood in, trust in your API is damaged permanently.

**Lesson (preview of Chapter 4):** Deprecation needs advance notice, a clear timeline, and multiple communication channels (docs, email, response headers, dashboards).

### Anti-pattern 6: Version in the body instead of anywhere discoverable

```json
POST /users
{ "version": 2, "name": "Alice" }
```

This is bad because HTTP-level tooling (gateways, caches, logs, monitoring) can't see the version without parsing every request body. Routing decisions, rate limiting, and analytics all become harder.

**Lesson:** Version must live somewhere HTTP-native — URL, header, or query param — never buried in the payload.

### Anti-pattern 7: Silently changing behavior for the "same" version

The worst offender: keeping `/v1/users` at the same URL, but changing its actual behavior over time without bumping anything. This completely defeats the purpose of versioning — the contract is a lie. This happens most often with subtle things: rounding behavior, default sort order, timezone handling, pagination defaults.

**Lesson:** If it changes the shape or meaning of the response in a way that could break a reasonable client's assumptions, it needs a version bump — even if it feels "minor" to you.

### Anti-pattern 8: Treating internal and external APIs the same way

Internal microservice-to-microservice APIs (owned by the same team, deployed together) often don't need the same heavyweight versioning ceremony as a public API used by thousands of unknown third parties. Over-engineering internal API versioning slows teams down for little benefit; under-engineering *public* API versioning causes chaos.

**Lesson:** Calibrate the rigor of your versioning strategy to your actual blast radius and consumer trust level.

### Quick self-check list

Before shipping any API change, ask:
1. Does this change what a well-behaved existing client would receive or expect? → breaking, needs version bump
2. Is this bundled with unrelated changes? → split it
3. Do I have a way to tell consumers this is happening and by when? → need deprecation plan
4. Is the version visible/discoverable via HTTP-native means? → not buried in body

---

Chapter 3 done — the **pitfalls**. Chapter 4 is where it gets genuinely interesting: **how to introduce a new version safely** in a live system (parallel running, internal routing, contract testing) and **deprecation strategy** — how to retire the old one without breaking trust, including how long you should actually keep it alive.

Continue to Chapter 4?

---

## Chapter 4: Introducing a New Version Safely, and Retiring the Old One

This is the operational heart of versioning — the part most tutorials skip.

### Part A: How to introduce a new version

**Step 1 — Design in isolation, but share what you can**

Build v2's routes/controllers as a distinct code path, but wherever possible, have both versions call down into the *same* underlying business logic/service layer. Only the "edges" (request parsing, response shaping) should differ.

```
        ┌─────────────┐        ┌─────────────┐
        │  v1 Handler │        │  v2 Handler │
        │ (shape in/  │        │ (shape in/  │
        │  out only)  │        │  out only)  │
        └──────┬──────┘        └──────┬──────┘
               │                      │
               └──────────┬───────────┘
                           ▼
                 ┌───────────────────┐
                 │  Shared Business   │
                 │  Logic / Service   │
                 │       Layer        │
                 └───────────────────┘
```

This avoids the classic trap of duplicating business logic per version, which means every bugfix has to be applied twice (and inevitably one version gets forgotten).

**Step 2 — Run both versions in production simultaneously**

Never do a hard cutover. Both v1 and v2 should be live and serving traffic at the same time. This is non-negotiable for anything with external consumers.

**Step 3 — Contract testing**

Before releasing v2, write tests that assert v1's contract *hasn't* changed (regression protection) and that v2 matches its new documented contract. Tools: Pact, Postman/Newman collections, OpenAPI schema validation in CI.

**Step 4 — Canary/staged rollout for your own internal consumers first**

If you have internal teams consuming your API, migrate them to v2 first — they're easier to coordinate with and will surface bugs before external partners do.

**Step 5 — Documentation and changelog, published *before* general availability**

A public, dated changelog entry, a migration guide (ideally with a "v1 → v2 field mapping" table), and updated OpenAPI/Swagger specs should all exist before you announce v2 is available.

### Part B: Deprecating the old version

**Step 1 — Announce deprecation, don't just imply it**

The moment v2 is stable, mark v1 as deprecated — but *deprecated ≠ removed*. Communicate through every channel available:
- Response headers on every v1 call (industry standard: `Deprecation: true` and `Sunset: <date>` headers, per RFC 8594)
- Dashboard banners for API-key holders
- Email to registered developers/API consumers
- Changelog and docs update

Example response headers:
```
HTTP/1.1 200 OK
Deprecation: true
Sunset: Sat, 31 Jan 2027 00:00:00 GMT
Link: <https://api.example.com/docs/migration-v1-to-v2>; rel="deprecation"
```

**Step 2 — Give a real, generous timeline**

This is the "how long should we keep the old one" question — there's no single universal number, but here's the practical framework:

| API type | Typical deprecation window |
|---|---|
| Internal microservice API (same org, few consumers) | 2–6 weeks |
| B2B/partner API (known consumers, direct relationship) | 3–6 months |
| Public API with large developer ecosystem | 6–12 months minimum |
| Mission-critical public API (payments, infra) | 12–24 months, sometimes longer |

Stripe is the industry gold standard here — they've kept API versions alive for *years* per account, because breaking a payments integration is catastrophic for their customers' businesses. GitHub gave roughly a year's notice for major API version sunsets.

**Step 3 — Track actual usage, don't guess**

Instrument v1 endpoints with metrics: which API keys/clients are still calling it, how often, which specific fields they're consuming (if feasible). Don't sunset until usage has meaningfully dropped or you've directly contacted remaining heavy users.

**Step 4 — Escalate communication as sunset approaches**

- 6 months out: soft notice in docs + headers
- 3 months out: direct email to active v1 consumers
- 1 month out: urgent email + dashboard warnings
- 1 week out: final warning
- Sunset day: v1 either shuts off or returns `410 Gone` with a clear error message pointing to migration docs — never just silently start failing with confusing errors

**Step 5 — After sunset, keep the door open briefly for emergencies**

Some mature APIs return a helpful `410 Gone` with migration instructions rather than a generic 404, for a grace period after the "hard" cutoff — this softens the landing for the stragglers who missed every warning.

### Visual timeline

```
v2 Released ──► v1 marked Deprecated ──► Active migration push ──► v1 Sunset (410 Gone)
     │                    │                        │                        │
  Both live          Headers + docs          Direct outreach          Old code removed
  in parallel         updated                to remaining users        from codebase
```

---

Chapter 4 done — **safe rollout + deprecation lifecycle**, including realistic timelines. 

Chapter 5 will tackle the two questions you asked that are genuinely debated among senior engineers: **global API version vs. per-resource versioning**, and then Chapter 6 will be the **production best-practices checklist** (including advanced patterns like Stripe's date-based versioning) to tie everything together.

Continue to Chapter 5?

---

## Chapter 5: Global Version vs. Per-Resource (Per-API) Versioning

This is the debate that splits engineering orgs, so let's be precise about what's actually being compared.

### Global versioning

```
/v1/users
/v1/orders
/v1/products
```

One version number governs the *entire API surface*. When you bump to v2, in principle *everything* moves to v2 together — even resources that didn't change.

**Pros:**
- Simple mental model for consumers — "I'm on v1 of the API," full stop
- Simple to document — one set of docs per version
- Simple to route at the infrastructure level — one gateway rule per version

**Cons:**
- Forces a full-API version bump even if only one resource changed — massive blast radius for a small change
- Consumers who only use `/orders` still have to worry about a v2 bump that only affected `/products`
- Encourages the "big bang redesign" anti-pattern from Chapter 3, because bumping versions feels expensive, so teams batch up many changes into one big v2 release

### Per-resource versioning

```
/users/v1/42       (or via header: vnd.myapp.user.v1+json)
/orders/v3/99       (or via header: vnd.myapp.order.v3+json)
```

Each resource type evolves and versions *independently*.

**Pros:**
- Blast radius is minimal — only consumers of the changed resource are affected
- Encourages small, frequent, low-risk changes rather than big rare ones
- Matches how real systems actually evolve — `orders` might change 5 times while `users` barely changes at all

**Cons:**
- Harder for consumers to reason about "what version am I on" — there's no single answer
- More complex documentation and tooling (your OpenAPI spec generation, SDKs, and client libraries need to track versions per-resource)
- Routing/infrastructure logic is more complex — can't just say "route all `/v2/*` traffic here"

### The real-world compromise most companies land on

Most mature APIs actually do a **hybrid**: a global version number for coordination/marketing/major contract shifts (auth model, pagination style, error format) — but individual endpoints can evolve additively (new optional fields, new endpoints) *without* forcing a global bump, reserving the global bump for genuinely cross-cutting breaking changes.

```
                    Global "v2" boundary
        ┌─────────────────────────────────────────┐
        │  /v2/users   /v2/orders   /v2/products   │
        │       (all share auth, error format,     │
        │        pagination conventions)           │
        └─────────────────────────────────────────┘
              Within v2: individual endpoints can
              still add non-breaking changes freely
              without a new global version
```

Stripe is the most instructive real example here, and it's advanced enough to deserve its own explanation:

**Stripe's approach (date-based, header-driven, per-account):**
- Every account is pinned to a specific API version, identified by a *date string* (e.g. `2024-06-20`), not a `v1`/`v2` integer
- Set via header: `Stripe-Version: 2024-06-20`
- When Stripe makes a breaking change to *any* endpoint, they cut a new dated version, but your account keeps using whichever dated version you're pinned to until you explicitly upgrade
- This is effectively **global versioning by date, but each account can be on a different global version simultaneously**, and Stripe internally maintains a "changeset" system that translates between versions per request

This is considered one of the most sophisticated versioning systems in the industry, but it exists specifically because payments APIs cannot tolerate any surprise breakage — it's not necessarily the right complexity level for a typical company's API.

### So which should *you* pick?

Ask these questions:

| Question | Leans toward... |
|---|---|
| Do most consumers use most of your API surface? | Global |
| Do different resources evolve at very different speeds? | Per-resource |
| Is your team small, API surface small (<15 endpoints)? | Global (simpler, lower overhead) |
| Do you have many independent teams owning different resources? | Per-resource (decouples their release cycles) |
| Do you need strict infra-level routing by version? | Global (much easier to route) |
| Is trust/stability paramount (payments, healthcare, infra)? | Consider Stripe's hybrid dated-global approach |

**Practical recommendation for most teams:** Start with **global URI-path versioning** (`/v1/`, `/v2/`). It's simpler to build, document, route, and explain to consumers. Only graduate to per-resource versioning once you've *actually felt the pain* of the full-API-bump blast radius — don't pre-optimize for a problem you don't have yet.

---

Chapter 5 done. Chapter 6 — the finale — is the **production best-practices checklist**, tying together everything: versioning strategy selection, deprecation headers, changelogs, SDK considerations, monitoring, and a few advanced patterns (like Stripe's changesets) worth knowing about even if you don't need them yet.

Continue to Chapter 6?

---

## Chapter 6: Production Best Practices — The Complete Checklist

Let's tie everything together into something you can actually apply.

### 1. Choosing your strategy (decision recap)

- **Public API, external consumers who'll explore manually** → URI path versioning (`/v1/`)
- **Internal API, well-governed clients** → header-based is fine, less URL churn
- **Payments/critical infra with account-level stability needs** → consider Stripe's dated, per-account pinned model (advanced, high engineering cost — don't adopt this lightly)
- **Small API surface** → global version
- **Many independently-evolving resources, multiple owning teams** → hybrid: global version for cross-cutting contract, per-resource additive evolution within it

### 2. What belongs in every version bump

- [ ] Updated OpenAPI/Swagger spec, versioned alongside the code
- [ ] Migration guide with an explicit old-field → new-field mapping table
- [ ] Changelog entry with a real date and a clear "breaking" label
- [ ] Automated contract tests confirming the old version's behavior is untouched
- [ ] SDKs/client libraries regenerated or updated to support the new version
- [ ] Sandbox/staging environment where consumers can test the new version before committing

### 3. Deprecation hygiene

- [ ] `Deprecation` and `Sunset` headers (RFC 8594) on every deprecated-version response
- [ ] A `Link` header pointing to migration docs
- [ ] Usage analytics per API key/client so you know who's still on the old version
- [ ] A tiered communication timeline (soft notice → direct outreach → urgent warnings → sunset)
- [ ] `410 Gone` (not a raw 404 or 500) after sunset, with a body explaining what happened and where to go

### 4. Error handling consistency across versions

Your error response *shape* is part of the contract too — and it's one of the most commonly forgotten versioning surfaces. Decide early:

```json
{
  "error": {
    "code": "invalid_request",
    "message": "The 'email' field is required",
    "field": "email"
  }
}
```

If you change this shape, that's a breaking change like any other — it needs the same versioning discipline as a response body change.

### 5. Backward-compatible techniques that *avoid* needing a new version

Sometimes you can dodge a version bump entirely with good API design:

- **Additive-only changes**: new fields are always optional and ignorable by old clients
- **Tolerant reader pattern**: instruct/require clients to ignore unknown fields rather than fail on them (document this explicitly — it's a two-way contract)
- **Expand/contract pattern** (aka "parallel change"): when renaming a field, *add* the new field while *keeping* the old one populated with the same value for a transition period, then remove the old one only after confirming no one reads it anymore — this can sometimes avoid a version bump entirely for internal APIs
- **Feature flags/opt-in headers** for experimental behavior, rather than a full version

### 6. Monitoring & observability

- [ ] Dashboard breaking down traffic by API version — you should be able to answer "what % of traffic is still on v1?" instantly
- [ ] Alerting when an old version's traffic doesn't decline as expected before a sunset date
- [ ] Per-version error rate tracking (a spike on the new version right after release is your canary signal)

### 7. Governance — the human side

- [ ] A documented internal policy: what counts as breaking, who approves a version bump, what the minimum deprecation window is
- [ ] A single source of truth for "which versions are currently live" (internal wiki, or better, a machine-readable `/versions` endpoint)
- [ ] Ownership: someone is accountable for eventually removing dead version code — otherwise it lives forever as debt

### 8. A minimal "versions" meta-endpoint (nice touch, seen in mature APIs)

```
GET /versions

{
  "versions": [
    { "version": "v1", "status": "deprecated", "sunset": "2027-01-31" },
    { "version": "v2", "status": "current" },
    { "version": "v3", "status": "beta" }
  ]
}
```

This gives consumers (and your own internal tooling) a programmatic way to check status rather than relying on tribal knowledge or stale docs.

---

### Full-picture summary (all 6 chapters)

```
1. WHY:      Contracts break clients → version only breaking changes
2. HOW:      URI path (most common) > header > media-type (purest) > query param (weakest)
3. AVOID:    No strategy, over-versioning, big-bang redesigns, silent breakage, hidden versions
4. ROLLOUT:  Build in parallel → shared business logic → contract tests → staged migration
             → deprecate loudly (headers/docs/email) → generous timeline → 410 Gone at sunset
5. SCOPE:    Global (simple, bigger blast radius) vs per-resource (complex, smaller blast radius)
             → most teams start global, graduate to hybrid only when it hurts
6. PRACTICE: Checklists for spec/docs/tests, deprecation headers, error-shape discipline,
             expand/contract pattern, monitoring by version, governance ownership
```

That's the complete arc from first principles to production-grade practice. If you want, a good next step would be to actually **design a versioning strategy for a specific API you're working on** — walk me through what it looks like (public/internal, current traffic, how many consumers) and I can help you apply this concretely rather than abstractly.

---

---
---
---

Absolutely. The best way to learn REST API versioning is to start with **why it exists**, then build toward **production-grade API lifecycle management**.

 I’ll treat this like a mini-course, from beginner → intermediate → advanced, with concrete examples and decision rules.

 # REST API Versioning — From Zero to Production

 ## 1\. First: What is API versioning?

 Suppose you build this API:

```
GET /api/users/123
```

 Today it returns:

```
{
  "id": 123,
  "name": "John",
  "email": "john@example.com"
}
```

 A year later, you decide the response should instead be:

```
{
  "id": 123,
  "fullName": "John Smith",
  "emailAddress": "john@example.com"
}
```

 You have a problem.

 Your existing clients were written expecting:

```
response.name
response.email
```

 If you simply change the API, old applications can break.

 **API versioning is the mechanism that allows you to evolve an API while preserving compatibility for existing consumers.**

 Conceptually:

```
                  API evolution
                       │
          ┌────────────┴────────────┐
          │                         │
       v1 clients                v2 clients
          │                         │
          ▼                         ▼
     /api/v1/users             /api/v2/users
          │                         │
          └──────────┬──────────────┘
                     ▼
                Same system
```

 The important idea is:

 > **Versioning is not primarily about putting `v1` in a URL. It is about managing compatibility over time.**

---

 # 2\. Why do we need versioning?

 Imagine you have:

```
Mobile App
Web App
Partner A
Partner B
Partner C
Internal Services
Third-party developers
```

 All of them consume:

```
GET /users/123
```

 You cannot assume everyone upgrades simultaneously.

 For example:

```
Monday

Mobile App ────────┐
Web App ───────────┤
Partner A ─────────┤──> API
Partner B ─────────┤
Partner C ─────────┘
```

 You release a breaking change.

```
Tuesday

Mobile App ────────┐
Web App ───────────┤
Partner A ─────────┤──> 💥 API changed
Partner B ─────────┤
Partner C ─────────┘
```

 Versioning gives you:

```
                    API
                     │
             ┌───────┴───────┐
             │               │
            v1              v2
             │               │
       old clients       new clients
```

 This gives clients time to migrate.

---

 # 3\. The most important concept: breaking vs non-breaking changes

 This is the foundation of API versioning.

 Not every API change requires a new version.

 ## Non-breaking change

 Suppose v1 returns:

```
{
  "id": 10,
  "name": "Alice"
}
```

 You add:

```
{
  "id": 10,
  "name": "Alice",
  "age": 30
}
```

 Existing clients can generally continue working.

 That's potentially **non-breaking**.

---

 ## Breaking change

 Suppose you change:

```
{
  "name": "Alice"
}
```

 to:

```
{
  "fullName": "Alice"
}
```

 Existing clients doing:

```
user.name
```

 break.

 That's a **breaking change**.

---

 Another breaking change:

 ### Before

```
GET /users/123
```

 ### After

```
GET /customers/123
```

 Another:

 ### Before

```
{
  "id": 123,
  "status": "ACTIVE"
}
```

 ### After

```
{
  "id": 123,
  "status": 1
}
```

 Another:

```
POST /orders
```

 Previously:

```
{
  "productId": 10,
  "quantity": 2
}
```

 Now requiring:

```
{
  "productId": 10,
  "quantity": 2,
  "warehouseId": 5
}
```

 If `warehouseId` is mandatory, existing requests may fail.

 Breaking.

---

 # 4\. A useful mental model

 Think about an API contract like a programming interface.

 If you have:

```
interface PaymentService {
    Payment pay(Order order);
}
```

 and suddenly change it to:

```
Payment pay(Order order, String currency, String country);
```

 every caller may need to change.

 REST APIs are similar.

 The contract includes things like:

 - URL
- HTTP method
- request structure
- response structure
- field names
- field types
- required/optional fields
- status codes
- error structure
- authentication behavior
- pagination behavior
- semantics

 Versioning protects consumers from incompatible contract changes.

---

 # 5\. When should you create a new API version?

 A good rule:

 > **Create a new major API version when you need a breaking contract change that cannot reasonably be introduced compatibly.**

 For example:

```
v1
```

 has:

```
{
  "name": "John"
}
```

 You want:

```
{
  "firstName": "John",
  "lastName": "Smith"
}
```

 You could create:

```
v2
```

 while leaving v1 intact.

---

 # 6\. What does NOT necessarily require a new version?

 This is where many teams over-version.

 Suppose you have:

```
{
  "id": 10,
  "name": "John"
}
```

 Adding:

```
{
  "id": 10,
  "name": "John",
  "createdAt": "2026-09-15T10:00:00Z"
}
```

 doesn't necessarily require v2.

 Similarly:

 - fixing server bugs while preserving contract
- improving database performance
- changing internal implementation
- adding optional request fields
- adding optional response fields, if your compatibility policy allows it
- adding new endpoints
- adding new resources

 generally don't require a new major version.

---

 # 7\. The big four API versioning strategies

 There are four common approaches.

 ## Strategy 1 — URL path versioning

 Most commonly:

```
GET /api/v1/users/123
GET /api/v2/users/123
```

 Or:

```
GET /v1/users/123
GET /v2/users/123
```

 ### Advantages

 Very obvious.

 A developer can immediately see:

```
/v1/
```

 versus:

```
/v2/
```

 It's easy to:

 - document
- test
- route
- monitor
- cache
- debug

 For many organizations, this is the simplest production choice.

 ### Disadvantages

 The URL represents both:

```
resource
+
version
```

 Some people argue that:

```
/users/123
```

 is cleaner REST-wise.

 But in practical enterprise APIs, URL versioning is perfectly workable.

---

 # 8\. Strategy 2 — Query parameter versioning

 Example:

```
GET /users/123?version=1
```

 or:

```
GET /users/123?api-version=2
```

 ### Advantages

 The resource URL stays:

```
/users/123
```

 ### Problems

 Now versioning becomes part of request parameters.

 Caching, routing, documentation, and tooling can become less intuitive.

 For example:

```
GET /users/123?version=1
GET /users/123?version=2
```

 They look like the same resource with different parameters.

 It's usable, but generally less popular than path/header approaches.

---

 # 9\. Strategy 3 — Header versioning

 For example:

```
GET /users/123
Accept: application/vnd.company.user-v2+json
```

 or:

```
API-Version: 2
```

 The URL stays:

```
/users/123
```

 while the version is expressed through headers.

 This is elegant from a content-negotiation perspective.

 But it is less obvious for humans.

 If you look at:

```
GET /users/123
```

 you don't immediately know whether it's v1 or v2.

 You need to inspect headers.

---

 # 10\. Strategy 4 — Media type versioning

 This is a more HTTP-oriented approach:

```
Accept: application/vnd.company.v2+json
```

 The server examines the requested representation.

 For example:

```
GET /users/123

Accept: application/vnd.company.user.v2+json
```

 The response might be:

```
Content-Type: application/vnd.company.user.v2+json
```

 This is conceptually elegant because you're saying:

 > "Give me representation version 2 of this resource."

 But it has a steeper learning curve and tooling/documentation can be less straightforward.

---

 # 11\. Which strategy should you use?

 For most enterprise teams:

 ### My default recommendation

 Use:

```
/api/v1/...
```

 and:

```
/api/v2/...
```

 unless you have a strong reason to use another strategy.

 Why?

 Because it's:

 - explicit
- easy to understand
- easy to route
- easy to document
- easy to monitor
- easy to debug
- easy for external consumers
- easy for developers unfamiliar with your system

 Don't optimize for theoretical REST purity at the expense of operational simplicity.

---

 # 12\. Global version vs API/resource-level version

 This is a very important architectural question.

 Imagine your system has:

```
Users API
Orders API
Payments API
Products API
Inventory API
```

 Should you do:

```
/api/v2/users
/api/v2/orders
/api/v2/payments
/api/v2/products
```

 or:

```
/users/v2
/orders/v1
/payments/v3
/products/v1
```

 ?

 ## Global versioning

 Example:

```
/api/v1/users
/api/v1/orders
/api/v1/payments
```

 Then:

```
/api/v2/users
/api/v2/orders
/api/v2/payments
```

 ### Advantage

 Very simple mental model.

 But there's a significant problem.

 Suppose only Payments changes.

 Why should you create:

```
v2 Users
v2 Orders
v2 Products
```

 if they didn't change?

 You potentially force unrelated APIs into a new version.

---

 # 13\. Resource/API-level versioning

 Instead:

```
/users/v1
/orders/v1
/payments/v2
/products/v1
```

 Only Payments changes.

 This gives you independent evolution.

 Conceptually:

```
                 APIs
                  │
       ┌──────────┼──────────┐
       │          │          │
     Users      Orders    Payments
       │          │          │
      v1         v1        v1/v2
```

 This is generally more scalable for a large API platform.

---

 # 14\. But don't go too granular

 There's a trap here.

 Don't create:

```
/users/v17
/orders/v9
/payments/v13
/products/v8
```

 with every tiny change.

 Versioning should represent **meaningful contract generations**, not individual deployments.

 Think:

```
Version = compatibility boundary
```

 not:

```
Version = release number
```

---

 # 15\. Major version vs minor version

 You should generally distinguish:

```
Major version
```

 from:

```
minor/patch evolution
```

 For example:

```
v1
```

 can evolve internally through:

```
v1.1
v1.2
v1.3
```

 without necessarily exposing these versions in the public URL.

 A common API strategy is:

```
/v1
```

 and continuously make backward-compatible changes.

 You don't necessarily need:

```
/v1.1
/v1.2
/v1.3
```

---

 # 16\. Don't use versions for every deployment

 Bad:

```
v1 = January deployment
v2 = February deployment
v3 = March deployment
v4 = April deployment
```

 This is not API versioning.

 That's deployment/release versioning.

 Your API could have:

```
API v1
```

 while internally having:

```
application release 2026.09.1
2026.09.2
2026.09.3
```

 These are completely different concepts.

---

 # 17\. How should v2 actually be introduced?

 Let's walk through a real production scenario.

 You currently have:

```
GET /api/v1/customers/123
```

 Response:

```
{
  "id": 123,
  "name": "John Smith",
  "phone": "123456789"
}
```

 You want to redesign it:

```
{
  "id": 123,
  "firstName": "John",
  "lastName": "Smith",
  "phoneNumber": "123456789"
}
```

 That's breaking.

 So you introduce:

```
GET /api/v2/customers/123
```

 Now:

```
                    Customer Service
                           │
                 ┌─────────┴─────────┐
                 │                   │
                v1                  v2
                 │                   │
         old representation   new representation
```

---

 # 18\. Should v1 and v2 have separate databases?

 Usually **no**.

 This is a common misunderstanding.

 You might have:

```
                  Database
                     │
             ┌───────┴───────┐
             │               │
            v1              v2
             │               │
       representation   representation
```

 Both versions can use the same underlying domain model/database.

 For example:

```
                    Domain Model
                         │
             ┌───────────┴───────────┐
             │                       │
        v1 mapper                v2 mapper
             │                       │
        v1 response              v2 response
```

 This is often much cleaner.

---

 # 19\. A very good architecture

 Suppose you have:

```
HTTP Request
     │
     ▼
API Controller
     │
     ▼
Application Service
     │
     ▼
Domain Model
     │
     ▼
Database
```

 Versioning ideally stays near the **API boundary**.

 For example:

```
/v1/customer
       │
       ▼
 V1 Controller
       │
       ▼
Application Service
       │
       ▼
 Domain Model
```

 And:

```
/v2/customer
       │
       ▼
 V2 Controller
       │
       ▼
Application Service
       │
       ▼
 Domain Model
```

 The business logic should not necessarily be duplicated.

---

 # 20\. Avoid this architecture

 Bad:

```
V1 Controller
     │
 V1 Business Logic
     │
 V1 Database Logic

V2 Controller
     │
 V2 Business Logic
     │
 V2 Database Logic
```

 Now you have:

```
Business logic × versions
```

 Eventually:

```
v1
v2
v3
v4
```

 become four separate systems.

 That's a maintenance nightmare.

 Prefer:

```
                 API Layer
              /             \
            v1               v2
             \               /
              \             /
             Application
                 │
               Domain
                 │
              Database
```

---

 # 21\. What happens to the old version?

 This is where API lifecycle management comes in.

 You generally have:

```
Active
   ↓
Deprecated
   ↓
Sunset
   ↓
Removed
```

 For example:

```
v1
│
├── Active
│
├── Deprecated
│
├── Sunset announced
│
└── Removed
```

---

 # 22\. What does "deprecated" mean?

 It means:

 > "This API still works, but you should stop using it because it will eventually be removed."

 This distinction matters.

 **Deprecated ≠ immediately unavailable.**

 For example:

```
GET /api/v1/users
```

 still works.

 But documentation says:

```
Deprecated.
Migrate to v2.
Sunset: 2027-06-01.
```

---

 # 23\. What does "sunset" mean?

 Sunset means the old version is scheduled to stop being available.

 Example:

```
v1 introduced:     2025-01-01
v2 introduced:     2026-01-01
v1 deprecated:     2026-01-01
v1 sunset:         2027-01-01
v1 removed:        2027-01-01
```

 You don't necessarily have to wait exactly one year.

 The correct duration depends on your consumers.

---

 # 24\. How long should you keep an old API?

 There is no universal number.

 The correct answer depends on:

 - number of consumers
- consumer type
- whether consumers are internal/external
- release frequency
- mobile app upgrade behavior
- contractual obligations
- regulatory requirements
- migration complexity
- business criticality
- traffic volume

 For an internal API:

```
30–90 days
```

 might be enough.

 For an external partner API:

```
6–24 months
```

 might be more appropriate.

 For public APIs with many independent consumers:

 possibly much longer.

 The important principle is:

 > **Don't choose a sunset period based only on engineering convenience. Choose it based on consumer migration reality.**

---

 # 25\. Mobile apps make this harder

 Imagine you release:

```
MyApp v10 → API v1
```

 Then you release:

```
MyApp v11 → API v2
```

 Not everyone immediately upgrades the mobile app.

 You may have:

```
40% → v11
60% → v10
```

 If you immediately kill API v1:

```
60% users → 💥
```

 Therefore mobile APIs often need longer compatibility windows.

 You should measure:

```
API v1 traffic by client version
```

 before removing v1.

---

 # 26\. Production rule: never guess whether v1 is still used

 Instrument it.

 For every API request, capture something like:

```
API version
client application
client version
consumer
endpoint
timestamp
status code
latency
```

 Then you can answer:

```
Who still uses v1?
```

 Instead of guessing.

 Example:

```
v1 traffic

Mobile 10.2       2.1%
Partner A         0.3%
Internal Service  0.0%
Mobile 10.1       0.1%
```

 Now you have evidence.

---

 # 27\. Deprecation should be observable

 You can expose deprecation information through documentation and, where appropriate, HTTP headers.

 For example:

```
Deprecation: true
Sunset: Wed, 30 Jun 2027 00:00:00 GMT
```

 Clients can potentially detect that their API version is approaching retirement.

 But don't rely only on headers.

 You should also provide:

 - migration documentation
- announcement
- dashboard
- timeline
- replacement API
- examples
- support channel

---

 # 28\. The API lifecycle should look like this

 A mature organization might have:

```
                Design
                  │
                  ▼
                v1.0
                  │
                  ▼
                GA
                  │
                  ▼
              Maintenance
                  │
                  ▼
             Deprecation
                  │
                  ▼
           Sunset announced
                  │
                  ▼
               Disabled
                  │
                  ▼
               Removed
```

 This is much more important than simply putting `/v2` into the URL.

---

 # 29\. How should you decide whether a change is breaking?

 Create a **compatibility policy**.

 For example:

 ### Usually safe

 - Add a new endpoint
- Add an optional request field
- Add a new optional response field
- Add a new enum value, depending on client parsing behavior
- Improve performance
- Fix implementation bugs without changing documented behavior

 ### Usually breaking

 - Remove endpoint
- Rename field
- Remove field
- Change field type
- Make optional field required
- Change semantics
- Change authentication requirements
- Change status code behavior
- Change error format
- Change pagination contract
- Change ordering guarantees
- Change meaning of an existing enum
- Restrict previously accepted inputs

 The word **usually** matters.

 Even "adding a field" can break poorly written clients.

---

 # 30\. The enum trap

 Suppose:

```
{
  "status": "ACTIVE"
}
```

 Your client has:

```
switch(status) {
    case ACTIVE:
        ...
    case INACTIVE:
        ...
}
```

 You add:

```
SUSPENDED
```

 Technically you're adding information.

 But a client with exhaustive enum handling might crash.

 Therefore:

 > **Backward compatibility is about consumer behavior, not just what looks safe from the server's perspective.**

 This is a major production lesson.

---

 # 31\. The "optional field" trap

 You have:

```
{
  "name": "John"
}
```

 You add:

```
{
  "name": "John",
  "middleName": null
}
```

 Some clients may deserialize this fine.

 Others may not.

 Therefore your compatibility policy should explicitly define:

```
Are unknown response fields allowed?
Are null values allowed?
Are enum extensions allowed?
```

 Don't leave these assumptions undocumented.

---

 # 32\. Consumer-driven contracts

 For serious API platforms, use automated contract testing.

 Imagine:

```
Provider
   │
   │ API
   ▼
Consumer A
Consumer B
Consumer C
```

 Consumers define expectations.

 For example:

```
Consumer A expects:

GET /users/1

{
   "id": number,
   "name": string
}
```

 Your CI pipeline checks whether the provider still satisfies those expectations.

 This can catch breaking changes **before production**.

---

 # 33\. OpenAPI becomes extremely important

 Define your API contract using OpenAPI.

 Conceptually:

```
paths:
  /api/v1/users/{id}:
    get:
      ...
```

 Then:

```
OpenAPI
   │
   ├── documentation
   ├── client generation
   ├── validation
   ├── testing
   └── compatibility checking
```

 Your API specification becomes part of your source-controlled contract.

---

 # 34\. Treat API contracts like code

 A mature API organization treats:

```
OpenAPI specification
```

 almost like source code.

 You should have:

```
Pull Request
     │
     ▼
OpenAPI diff
     │
     ▼
Breaking-change detection
     │
     ▼
CI
     │
     ▼
Review
```

 Example:

```
❌ Removed response property: email
❌ Changed type: id integer → string
❌ Made customerId required
```

 The PR should potentially fail automatically.

---

 # 35\. Versioning at the edge

 A useful production architecture is:

```
                 Internet
                    │
                    ▼
              API Gateway
                    │
          ┌─────────┴─────────┐
          │                   │
        /v1                  /v2
          │                   │
          ▼                   ▼
       Service              Service
          │                   │
          └─────────┬─────────┘
                    ▼
                 Domain
                    │
                 Database
```

 The gateway can handle:

 - routing
- authentication
- rate limiting
- logging
- metrics
- version routing
- deprecation headers

 But don't put all version-specific business logic in the gateway.

---

 # 36\. A subtle question: should v1 and v2 use the same implementation?

 There are several possibilities.

 ### Option A

```
v1 ──┐
     ├──> same business logic
v2 ──┘
```

 Best when differences are primarily representation.

 ### Option B

```
v1 ───> adapter ───> domain
v2 ─────────────────> domain
```

 Excellent when v1 needs compatibility translation.

 ### Option C

```
v1 ───> completely separate implementation
v2 ───> completely separate implementation
```

 Sometimes necessary for major architectural changes, but expensive.

 Prefer A/B when possible.

---

 # 37\. The adapter pattern is incredibly useful

 Suppose your domain model is:

```
{
  "firstName": "John",
  "lastName": "Smith"
}
```

 v2 directly exposes:

```
{
  "firstName": "John",
  "lastName": "Smith"
}
```

 But v1 expects:

```
{
  "name": "John Smith"
}
```

 You can have:

```
Domain
  │
  ▼
┌──────────────┐
│              │
v1 Adapter   v2 Adapter
│              │
▼              ▼
"name"       "firstName"
             "lastName"
```

 This allows the internal domain model to evolve independently.

---

 # 38\. Don't version your database just because you version your API

 This is another common mistake.

 You don't necessarily need:

```
DB v1
DB v2
```

 because:

```
API v1
API v2
```

 exist.

 The API contract is a consumer-facing abstraction.

 The database is an implementation detail.

 Keep that boundary strong.

---

 # 39\. API versioning and database migrations

 Suppose v2 needs:

```
first_name
last_name
```

 but v1 currently relies on:

```
full_name
```

 You might use an expand/contract migration.

 ### Step 1 — Expand

 Add:

```
first_name
last_name
```

 while keeping:

```
full_name
```

 ### Step 2

 Application writes both.

```
first_name
last_name
full_name
```

 ### Step 3

 Deploy v2.

 ### Step 4

 Migrate consumers.

 ### Step 5

 Stop needing v1.

 ### Step 6 — Contract

 Remove old database structures when safe.

 This is much safer than:

```
DROP full_name
```

 on day one.

---

 # 40\. Versioning should not leak into your domain model

 Avoid things like:

```
class UserV1
class UserV2
class UserV3
```

 everywhere in your application.

 Instead:

```
API representation
        ↓
Domain User
        ↓
Database
```

 Only introduce version-specific DTOs/models where the external contract actually differs.

---

 # 41\. What about authentication changes?

 Suppose v1 uses:

```
Authorization: Basic ...
```

 and v2 requires:

```
Authorization: Bearer ...
```

 That's potentially a breaking change.

 You need to decide whether authentication is:

```
global platform concern
```

 or:

```
version-specific contract
```

 In mature systems, it's often better to decouple authentication evolution from API resource versions where possible.

 For example:

```
API v1
API v2
     │
     ▼
Same OAuth/OIDC authentication layer
```

 rather than:

```
v1 → auth mechanism A
v2 → auth mechanism B
```

 unless there is a compelling reason.

---

 # 42\. Don't confuse API versioning with backward compatibility

 These are related but different.

 You can have:

```
v1
```

 that remains backward compatible for years.

 And you can accidentally introduce a breaking change into:

```
v1
```

 without creating v2.

 Versioning is the **mechanism**.

 Compatibility is the **property you are trying to preserve**.

---

 # 43\. The best API is one that needs fewer versions

 This sounds counterintuitive.

 A mature API team doesn't say:

 > "We need v2 because it's been two years."

 Instead:

 > "We need v2 because we have a breaking contract change."

 Good API design reduces the need for breaking changes.

 For example, instead of:

```
{
  "type": "CARD"
}
```

 designing extensibly from the beginning can help avoid future breaking changes.

---

 # 44\. Design for extensibility

 Good API design often includes:

 ### Optional fields

```
{
  "id": 123,
  "name": "John",
  "metadata": {}
}
```

 ### Flexible metadata

 Where appropriate:

```
{
  "id": 123,
  "attributes": {
    "department": "engineering"
  }
}
```

 ### Stable identifiers

 Don't make identifiers dependent on implementation details.

 ### Explicit semantics

 Document what fields mean.

 ### Predictable errors

 For example:

```
{
  "code": "INVALID_CUSTOMER",
  "message": "Customer does not exist"
}
```

 Stable error contracts make evolution easier.

---

 # 45\. Error responses need versioning too

 Suppose v1 returns:

```
{
  "error": "Invalid user"
}
```

 v2 returns:

```
{
  "code": "USER_NOT_FOUND",
  "message": "User does not exist"
}
```

 That's a contract difference.

 Don't forget:

```
Success response
+
Error response
```

 are both API contracts.

---

 # 46\. Pagination can cause versioning problems

 Suppose v1:

```
GET /users?page=1&limit=20
```

 returns:

```
{
  "users": [...],
  "page": 1,
  "totalPages": 10
}
```

 Later you want cursor pagination:

```
GET /users?cursor=abc
```

 with:

```
{
  "users": [...],
  "nextCursor": "xyz"
}
```

 If clients depend heavily on the old pagination semantics, this may warrant v2.

 Better yet, design pagination deliberately from the beginning.

---

 # 47\. URL versioning and resource identity

 There is a philosophical objection to:

```
/api/v1/users/123
```

 because people argue:

 > "User 123 is still the same resource, so why does its URL change?"

 That's a legitimate REST-design concern.

 But in enterprise API design, practical concerns often win.

 You can instead use:

```
/users/123
```

 with:

```
Accept: application/vnd.company.user.v2+json
```

 This preserves resource identity.

 However, the complexity is pushed into content negotiation.

 There is no universally correct answer.

---

 # 48\. What I'd recommend for a typical enterprise

 I'd choose:

```
/api/v1/...
```

 for major versions.

 Then:

```
v1
```

 gets backward-compatible additions.

 When a genuine breaking change is unavoidable:

```
/api/v2/...
```

 is introduced.

 Keep:

```
v1 + v2
```

 running simultaneously.

 Then:

```
v1 → deprecated → sunset → removed
```

 with measured consumer migration.

 This gives you a very predictable operational model.

---

 # 49\. How should v2 development happen?

 Don't just code v2.

 Run an explicit process.

 ### Phase 1 — Identify the problem

 Document:

```
Why is v1 insufficient?
```

 For example:

```
name cannot represent first/last names separately.
```

 ### Phase 2 — Define v2 contract

 Create:

```
OpenAPI v2
```

 ### Phase 3 — Compatibility review

 Identify:

```
What breaks?
```

 ### Phase 4 — Consumer analysis

 Find:

```
Who uses v1?
```

 ### Phase 5 — Build v2

 Implement:

```
/v2
```

 ### Phase 6 — Test

 Run:

```
unit tests
integration tests
contract tests
load tests
security tests
```

 ### Phase 7 — Release

 Make v2 available.

 ### Phase 8 — Migration

 Consumers move:

```
v1 → v2
```

 ### Phase 9 — Deprecate v1

 Announce:

```
v1 is deprecated.
```

 ### Phase 10 — Sunset

 After sufficient migration:

```
v1 → disabled
```

---

 # 50\. Consumer migration should be explicit

 Give consumers a migration guide.

 For example:

```
v1:

name
email

↓

v2:

firstName
lastName
emailAddress
```

 Then provide:

```
v1 field       v2 field
---------      ------------
name           firstName + lastName
email          emailAddress
```

 Also provide before/after examples.

 This can dramatically reduce migration time.

---

 # 51\. Don't silently redirect v1 → v2

 This is a common temptation:

```
/v1/users
     │
     ▼
redirect
     │
     ▼
/v2/users
```

 Be careful.

 A v2 response may be incompatible with v1 clients.

 If v1 expects:

```
{
  "name": "John"
}
```

 and v2 returns:

```
{
  "firstName": "John",
  "lastName": "Smith"
}
```

 a redirect doesn't solve the compatibility problem.

 If you want to reuse implementation, use an **adapter**, not a blind HTTP redirect.

---

 # 52\. Don't duplicate everything

 Bad:

```
v1/
 ├── controllers
 ├── services
 ├── repositories
 └── models

v2/
 ├── controllers
 ├── services
 ├── repositories
 └── models
```

 You now maintain two applications.

 Better:

```
API
├── v1 controller
├── v2 controller
│
└── shared application layer
       │
       └── domain
              │
              └── database
```

 Version the **contract**, not necessarily the entire application.

---

 # 53\. What about API gateways?

 Suppose:

```
                    Gateway
                       │
          ┌────────────┴────────────┐
          │                         │
      /api/v1                    /api/v2
          │                         │
          ▼                         ▼
      Service                    Service
```

 The gateway can route versions.

 But don't let the gateway become your business-logic layer.

 A good rule:

 > **Infrastructure concerns at the gateway; business semantics in the service.**

---

 # 54\. Version discovery

 For public APIs, you may expose:

```
GET /api
```

 or documentation showing:

```
Available versions:

v1 — deprecated
v2 — current
```

 Your developer portal might show:

```
Customers API

v2 — Current
v1 — Deprecated
```

 This makes lifecycle status obvious.

---

 # 55\. Documentation must clearly identify versions

 Avoid documentation like:

```
Customers API
```

 with no indication of version.

 Instead:

```
Customers API v2
```

 and:

```
Customers API v1 — Deprecated
```

 Each version should have:

 - OpenAPI spec
- examples
- authentication requirements
- error definitions
- pagination behavior
- migration guide where applicable
- lifecycle status

---

 # 56\. Monitoring versions

 This is one of the most important production practices.

 Track:

```
requests by API version
```

 For example:

```
API Version    Requests/day
-----------    ------------
v2             92,000,000
v1              8,000,000
```

 Then:

```
v1
 ↓
5%
 ↓
2%
 ↓
0.3%
 ↓
0.01%
```

 Now you can confidently sunset it.

---

 # 57\. Track consumers, not just versions

 Even better:

```
API v1
├── Mobile App
├── Partner A
├── Partner B
└── Internal Service X
```

 You want to know:

```
Which consumers are preventing v1 shutdown?
```

 This is particularly important for B2B APIs.

---

 # 58\. Set a migration deadline

 A strong deprecation program looks like:

```
2026-01-01
v2 released

2026-03-01
v1 deprecated

2026-06-01
migration reminder

2026-09-01
final warning

2026-10-01
v1 sunset
```

 Consumers shouldn't discover the shutdown because their production requests suddenly return:

```
410 Gone
```

---

 # 59\. What HTTP status should you use after removal?

 Once an endpoint/version is intentionally gone, you might return:

```
410 Gone
```

 to indicate it has been permanently removed.

 You could also use:

```
404 Not Found
```

 depending on your API behavior and infrastructure.

 The important thing is to make the migration path clear before removal.

---

 # 60\. Should old APIs receive bug fixes?

 Usually:

 **Yes, for critical correctness/security issues.**

 But don't necessarily continue adding features.

 Think:

```
v1 deprecated
   │
   ├── security fixes      ✅
   ├── critical bugs       ✅
   ├── operational fixes   ✅
   └── new features        ❌
```

 This is essentially a maintenance mode.

---

 # 61\. Security is special

 Never say:

 > "It's an old API, so we don't need to secure it."

 If v1 is still accessible:

```
/v1
```

 it remains part of your attack surface.

 You must maintain appropriate:

 - authentication
- authorization
- input validation
- rate limiting
- security patches
- logging
- monitoring

 A deprecated API is still a production API until it's actually removed.

---

 # 62\. Versioning and caching

 If you use:

```
/api/v1/users
/api/v2/users
```

 caches naturally distinguish them by URL.

 With header-based versioning:

```
GET /users
Accept: application/vnd.company.v1+json
```

 you need to ensure caches correctly vary by the relevant header.

 For example:

```
Vary: Accept
```

 may be necessary depending on the design.

 This is one practical reason path versioning is operationally attractive.

---

 # 63\. Versioning and observability

 Your logs should answer:

```
What version?
What endpoint?
Which consumer?
What client version?
What response?
How long?
Why did it fail?
```

 For example:

```
api_version=v1
endpoint=/customers/{id}
consumer=partner-a
status=200
latency=48ms
```

 This makes migration and incident investigation much easier.

---

 # 64\. Versioning and feature flags are different

 Don't use:

```
feature flag
```

 as a replacement for API versioning.

 Feature flags answer:

 > "Which behavior is enabled?"

 Versioning answers:

 > "Which contract does this consumer depend on?"

 You can use both.

 For example:

```
API v2
   │
   └── feature flag: new-pricing-engine
```

---

 # 65\. Versioning and semantic versioning

 You may see:

```
1.4.2
```

 in libraries.

 That's semantic versioning.

 For APIs, you don't necessarily need to expose:

```
/v1.4.2
```

 A cleaner model is:

```
Public API contract: v1
Implementation release: 2026.09.4
```

 The implementation can change many times without changing the API version.

---

 # 66\. A practical compatibility matrix

 Create a table like:

 | Change | Breaking? | New major version? |
| --- | --- | --- |
| Add endpoint | No | No |
| Add optional field | Usually no | No |
| Remove field | Yes | Yes |
| Rename field | Yes | Yes |
| Change type | Yes | Yes |
| Add required request field | Yes | Yes |
| Remove endpoint | Yes | Yes |
| Fix internal bug | No | No |
| Improve performance | No | No |
| Add enum value | Potentially | Depends |
| Change error schema | Potentially | Depends |
| Change authentication | Potentially | Depends |
| Change pagination semantics | Potentially | Often |

The exact rules should be part of your organization's API governance.

---

 # 67\. The most important production principle

 Here's the principle I'd put on your team's wall:

 > **Prefer additive, backward-compatible API evolution. Introduce a new major version only when a breaking change is genuinely necessary.**

 In other words:

```
                 API change
                     │
          ┌──────────┴──────────┐
          │                     │
   Can it be additive?      Is it breaking?
          │                     │
         YES                   YES
          │                     │
          ▼                     ▼
       v1 stays              New version
       unchanged              v2
       contract               │
                              ▼
                         Migration period
                              │
                              ▼
                           Deprecate
                              │
                              ▼
                            Sunset
```

---

 # 68\. A production-grade API versioning policy

 If I were designing a policy for an organization from scratch, I'd start with something like this:

 ### Versioning scheme

```
/api/v1/...
/api/v2/...
```

 ### Version semantics

 Only **breaking contract changes** create a new major version.

 ### Backward compatibility

 Existing versions remain compatible for their documented lifecycle.

 ### Additive changes

 Prefer:

```
additive changes
```

 over:

```
breaking changes
```

 ### Deprecation

 Every deprecated API gets:

```
deprecation date
sunset date
migration guide
replacement API
```

 ### Monitoring

 Track:

```
version
consumer
client version
endpoint
traffic
errors
```

 ### Removal

 Don't remove a version until:

```
consumer migration
+
traffic analysis
+
business approval
```

 are complete.

---

 # 69\. An example from start to finish

 Let's build one.

 ## Day 1

 You release:

```
GET /api/v1/orders/123
```

 Response:

```
{
  "id": 123,
  "customer": "John",
  "total": 100
}
```

---

 ## Day 300

 You realize you need:

```
{
  "customer": {
    "id": 10,
    "firstName": "John",
    "lastName": "Smith"
  },
  "subtotal": 90,
  "tax": 10,
  "total": 100
}
```

 This is a breaking redesign.

 Create:

```
GET /api/v2/orders/123
```

---

 ## Day 310

 v2 is released.

 Now:

```
v1 → 70%
v2 → 30%
```

---

 ## Day 350

 You announce:

```
v1 deprecated
```

 with migration documentation.

---

 ## Day 400

 Traffic:

```
v1 → 5%
v2 → 95%
```

 You contact remaining consumers.

---

 ## Day 430

 Traffic:

```
v1 → 0.01%
v2 → 99.99%
```

 Final warning.

---

 ## Day 450

 Disable v1.

```
GET /api/v1/orders/123
```

 returns an appropriate retirement response.

 Then eventually remove the v1 code.

---

 # 70\. Advanced topic: Can one API have multiple versions simultaneously?

 Absolutely.

 You might have:

```
Customers v1
Customers v2

Orders v1
Orders v2

Payments v3
```

 That's normal.

 But this is why **governance** becomes important.

 You need to know:

```
Which versions exist?
Which are active?
Which are deprecated?
When will they sunset?
Who owns them?
Who consumes them?
```

 Without governance, version sprawl becomes painful.

---

 # 71\. Create an API catalog

 For a large organization:

```
API Catalog

Customers
  v1 — Deprecated — sunset 2027-01-01
  v2 — Current

Orders
  v1 — Current

Payments
  v1 — Sunset
  v2 — Current
  v3 — Beta
```

 This is much better than relying on tribal knowledge.

---

 # 72\. API version ownership

 Every API/version should have an owner.

 For example:

```
API: Payments
Owner: Payments Platform Team

v1
Status: Deprecated
Sunset: 2027-01-01
Consumers: 12
```

 Without ownership, deprecated APIs tend to live forever.

---

 # 73\. Avoid version explosion

 If your system looks like:

```
v1
v2
v3
v4
v5
v6
```

 ask:

 > "Why are we creating so many breaking changes?"

 Usually the underlying problem is API design.

 A healthy API should evolve mostly through:

```
v1
  │
  ├── additive change
  ├── additive change
  ├── additive change
  ├── additive change
  │
  └── eventually v2
```

 not:

```
v1 → v2 → v3 → v4 → v5
```

 every few months.

---

 # 74\. A subtle but important point: version semantics

 Don't make v2 merely:

 > "v1 + random improvements."

 Define what v2 represents.

 For example:

```
v1:
Legacy customer representation

v2:
Normalized customer representation
```

 A version should have a coherent contract.

 Otherwise you create:

```
v2 = collection of unrelated breaking changes
```

 which makes migration harder.

---

 # 75\. Should APIs ever have a `latest` version?

 You might see:

```
/api/latest/users
```

 I'd generally avoid this for consumers.

 Why?

 Today:

```
latest = v2
```

 Tomorrow:

```
latest = v3
```

 The exact same URL suddenly has a different contract.

 That's dangerous.

 For stable integrations, prefer:

```
/api/v2/users
```

 so the contract is explicit.

---

 # 76\. What about `/api/current`?

 Same problem.

 A stable client should not depend on a moving target.

 It's fine for:

```
documentation
```

 or perhaps a human-facing API explorer.

 But not ideal as the contractual integration endpoint.

---

 # 77\. What about date-based versions?

 Some APIs use:

```
2024-01-01
2025-01-01
2026-01-01
```

 instead of:

```
v1
v2
v3
```

 This can be useful for APIs with frequent evolution.

 It communicates:

 > "This API contract is associated with a specific version date."

 But it introduces another lifecycle model.

 For a typical enterprise REST API, I'd start with:

```
v1
v2
```

 unless your platform has a strong reason to adopt date-based versions.

---

 # 78\. A mature API platform separates three concepts

 This is worth remembering:

```
              API lifecycle
                   │
        ┌──────────┼──────────┐
        │          │          │
    Contract    Deployment   Database
    version       release    schema
        │          │          │
       v1        2026.09.3    migration 42
```

 Don't mix them.

 A database migration does not mean API v2.

 A deployment does not mean API v2.

 A feature release does not mean API v2.

 A breaking **consumer contract** change means API v2.

---

 # 79\. The golden architecture

 If you remember only one architecture, remember this:

```
                     Clients
                        │
            ┌───────────┴───────────┐
            │                       │
        API v1                   API v2
            │                       │
            ▼                       ▼
       Version adapter         Version adapter
            │                       │
            └───────────┬───────────┘
                        ▼
                Application Layer
                        │
                     Domain
                        │
                    Repository
                        │
                     Database
```

 And around it:

```
          ┌───────────────────────────────┐
          │        API Platform           │
          │                               │
          │ Auth                          │
          │ Rate limiting                 │
          │ Observability                 │
          │ Documentation                 │
          │ Contract testing              │
          │ Version routing               │
          │ Deprecation                   │
          └───────────────────────────────┘
```

---

 # 80\. My recommended production checklist

 Before introducing v2:

 - [ ] Is the change genuinely breaking?
- [ ] Can we make it backward compatible instead?
- [ ] Have we documented exactly what changes?
- [ ] Have we created an OpenAPI contract?
- [ ] Have we run compatibility checks?
- [ ] Have we identified all v1 consumers?
- [ ] Have we defined migration instructions?
- [ ] Have we decided the deprecation period?
- [ ] Have we decided the sunset date?
- [ ] Have we implemented monitoring by version?
- [ ] Have we implemented v2 without duplicating business logic?
- [ ] Have we tested both versions?
- [ ] Have we documented lifecycle status?
- [ ] Have we communicated the change?
- [ ] Do we have a rollback plan?

 Before deleting v1:

 - [ ] v1 traffic is near zero
- [ ] known consumers have migrated
- [ ] migration deadline has passed
- [ ] final notification sent
- [ ] security concerns reviewed
- [ ] dashboards confirm no important traffic
- [ ] support/business owners approve removal
- [ ] v1 disabled
- [ ] v1 code eventually removed

---

 # 81\. The five rules I'd want you to remember

 If you're building APIs professionally, remember these:

 ### Rule 1

 **Version for breaking changes, not deployments.**

```
breaking contract → new major version
```

 ### Rule 2

 **Prefer backward-compatible evolution.**

```
add > change > remove
```

 ### Rule 3

 **Version at the API/resource boundary, not your entire system by default.**

 Don't force:

```
Users v2
Orders v2
Payments v2
```

 just because Payments changed.

 ### Rule 4

 **Never kill an old API without measuring who still uses it.**

```
deprecated ≠ unused
```

 ### Rule 5

 **API versioning is a lifecycle, not a URL.**

 The URL is only one small part:

```
Design
  ↓
Release
  ↓
Adoption
  ↓
Deprecation
  ↓
Migration
  ↓
Sunset
  ↓
Removal
```

---

 # 82\. The mental model to take away

 Think of an API as a **promise**.

 When you publish:

```
GET /api/v1/users/123
```

 you're effectively promising consumers:

 > "This contract will continue to behave according to these documented rules."

 When you need to break that promise:

```
v1
 ↓
"I can no longer maintain this contract as-is."
 ↓
v2
```

 But you don't immediately destroy the old promise.

 You give consumers a migration path:

```
                  ┌─────────────┐
                  │     v1      │
                  │   Current   │
                  └──────┬──────┘
                         │
                    v2 released
                         │
                         ▼
                  ┌─────────────┐
                  │     v1      │
                  │ Deprecated  │
                  └──────┬──────┘
                         │
                  migration period
                         │
                         ▼
                  ┌─────────────┐
                  │     v1      │
                  │   Sunset    │
                  └──────┬──────┘
                         │
                         ▼
                    Removed
```

 **That's API versioning.**

 And the deepest lesson is this:

 > **The goal of versioning isn't to have multiple versions. The goal is to let your API evolve without breaking the people who depend on it.**

 If you're learning this for **backend/system-design interviews or real production development**, the next layer I'd recommend is **API compatibility in depth**: exactly which changes are breaking/non-breaking (including JSON, enums, pagination, errors, HTTP status codes, headers, authentication, and database migrations), followed by a **real v1 → v2 implementation architecture** in Spring Boot/Java or Node.js.

 