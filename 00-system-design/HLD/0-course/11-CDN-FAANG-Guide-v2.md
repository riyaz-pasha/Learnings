# CDN (Content Delivery Network) — FAANG Interview Guide (v2)

> **v2 — learning-experience pass.** The v1 content (requirements, capacity math, push/pull, DNS-vs-anycast, TTL/lease, buy-vs-build, and the earlier 🆕 additions — thundering-herd handling, purge fan-out) was technically strong and is preserved almost word-for-word below. This pass fixed how it *teaches*, not what it teaches:
> - Added **§0: How to use this guide** — difficulty badges (🟢/🟡/🔴), a roadmap diagram, a clickable table of contents, and a glossary that points every abbreviation back to where it's actually explained.
> - Added a **TL;DR line under every section heading** — skim top to bottom in under two minutes and you have the whole shape of the guide before reading anything in depth.
> - Expanded abbreviations **in context, on first use per section** — `DNS`, `TTL`, `RTT`, `QPS`, `BGP`, `DDoS`, `SPOF`, `ISP`, `IXP`, `API`, `LRU`/`LFU`, `HLS`, `ALB`/`WAF`, `OCA` were all used at some point without being spelled out where they first appear in a given section; each now gets a brief, why-it-matters expansion the first time that section needs it.
> - Added three **"where people get this backwards" callouts** — the CDN-is-static-content-only myth (§8), "nearest" meaning geography instead of network distance (§10), and treating TTL expiry and cache invalidation as the same lever (§11.3).
> - Added diagrams to sections that were previously pure prose/tables but describe something spatial or sequential: a CDN-vs-reverse-proxy visual (§1.1), a capacity funnel (§3), a deployment-placement + split-TCP visual (§12), an API-call sequence diagram (§14), and a scrubber attack-path flow (§15).
> - **Relocated and extended §6.2** ("Architecture evolution v1 → v2 → v3") into a new capstone, **§18 "Putting it all together — v1 through v4"**, positioned *after* every concept it composes has actually been introduced (push/pull, dynamic content, multi-tier + shield + coalescing, routing, consistency, security, buy-vs-build). The old placement — right after the components section — told the build-up story before half its cast existed. Extended the story to a **v4** step and folded in a **consolidated "common mistakes" table** gathering gotchas that were otherwise scattered across §6–§13.
> - Renumbered **Active Recall (§18 → §19)** and **Golden Rules (§19 → §20)** to make room for the capstone; added a spaced-repetition note and two new recall questions.
> - Added a **worked, verbatim sample answer** to §17 (the interview playbook), so the section shows what a good spoken answer sounds like, not just a list of steps.
> - No technical claims changed, and nothing else was renumbered — every other cross-reference in the original (§7, §9.1–9.4, §11.1–11.5, §17) still points at the same section it always did.
>
> <details>
> <summary>v1 enhancement notes (content-completeness pass, preserved for history)</summary>
>
> This pass targeted the gaps a FAANG interviewer would probe that the original draft covered only thinly — everything else (requirements, capacity math, push/pull, DNS-vs-anycast, TTL/lease, buy-vs-build, the recall/cheat-sheet layers) was already strong and left untouched.
> - Added **§6.2 Architecture evolution v1 → v2 → v3** — a naive-single-origin → DNS-routed-regional-caches → anycast+shield+pub/sub-purge diagram sequence, for narrating the design as a build-up instead of presenting it finished.
> - Added **§9.3 Preventing thundering herd on a cold cache** (request-coalescing/single-flight flowchart + an "if X then Y" splitting it from origin-shield's job) and **§9.4** dedicated cache-hit and cache-miss-with-origin-shield sequence diagrams — the combined §6.1 diagram existed, but the two paths weren't isolated anywhere.
> - Added an **anycast-vs-DNS-geo-routing visual** in §10 (side-by-side graph) — the trade-off table and flowchart existed, but not a picture of the two routing paths themselves.
> - Added **§11.5 How purge propagation reaches thousands of edges** — a pub/sub fan-out sequence diagram and illustrative propagation-time numbers; the guide named "purge APIs" and cited Fastly's sub-second purge before, but never showed the fan-out mechanism.
> - Added Active Recall questions 13–14 and Golden Rule 9 covering request-coalescing-vs-origin-shield and purge fan-out, and extended the Master Cheat Sheet with three new lines summarizing the additions above.
> - No existing section was rewritten or reordered — this is additive; all new headings are marked with 🆕.
> </details>

---

## 0. How to use this guide 🟢

**TL;DR:** Read top to bottom once for the shape of the whole topic; come back section-by-section for depth. Difficulty badges tell you what's safe to skim on pass one.

Every section heading below carries a badge:

| Badge | Meaning |
|---|---|
| 🟢 Beginner | No prior CDN knowledge assumed — read these first, in order |
| 🟡 Intermediate | Assumes you're comfortable with §1–§6 |
| 🔴 Advanced | Interview-depth material — the stuff that separates "knows what a CDN is" from "would design one" |

### Table of contents

- [1. What it is, in one mental model](#1-what-it-is-in-one-mental-model-)
- [1.1 Disambiguation: CDN vs. reverse proxy](#11-disambiguation-cdn-vs-reverse-proxy-)
- [2. Why it exists — the problem, quantified](#2-why-it-exists-the-problem-quantified-)
- [3. Capacity estimation — a worked example](#3-capacity-estimation-a-worked-example-)
- [4. Requirements](#4-requirements-how-to-open-the-interview-)
- [5. Building blocks it's built from](#5-building-blocks-its-built-from-)
- [6. Components — the anatomy of a CDN](#6-components-the-anatomy-of-a-cdn-)
- [7. Push vs. Pull CDN](#7-push-vs-pull-cdn-the-central-design-decision-)
- [8. Dynamic content caching](#8-dynamic-content-caching-the-gotcha-most-candidates-miss-)
- [9. Multi-tier (layered) CDN architecture](#9-multi-tier-layered-cdn-architecture-)
- [10. Finding the nearest proxy server](#10-finding-the-nearest-proxy-server-routing-mechanisms-)
- [11. Content consistency](#11-content-consistency-cache-invalidation-is-still-the-hard-problem-)
- [12. Deployment: where do you physically put proxy servers?](#12-deployment-where-do-you-physically-put-proxy-servers-)
- [13. CDN as a service vs. specialized (build) CDN](#13-cdn-as-a-service-vs-specialized-build-cdn-the-classic-buy-vs-build-)
- [14. API design](#14-api-design-how-to-talk-through-the-interfaces-)
- [15. Security: scrubber servers and DDoS](#15-security-scrubber-servers-and-ddos-)
- [16. Evaluating the design against non-functional requirements](#16-evaluating-the-design-against-non-functional-requirements-)
- [17. How this shows up in a FAANG interview](#17-how-this-shows-up-in-a-faang-interview-)
- [18. Putting it all together — v1 through v4](#18-putting-it-all-together-v1-through-v4-)
- [19. Active Recall — Test Yourself](#19-active-recall-test-yourself-)
- [20. Golden Rules](#20-golden-rules-)
- [Master Cheat Sheet](#master-cheat-sheet)

*(If your viewer doesn't jump correctly, anchors depend on how it strips the emoji badges — falls back gracefully to a plain scroll/search either way.)*

### The roadmap

```mermaid
flowchart TD
    S1["§1 Mental model +<br/>1.1 vs reverse proxy<br/>🟢"] --> S2["§2 Why it exists<br/>🟢"]
    S2 --> S3["§3 Capacity estimation<br/>🔴"]
    S1 --> S4["§4 Requirements<br/>🟢"]
    S4 --> S5["§5 Building blocks<br/>🟢"]
    S5 --> S6["§6 Components<br/>🟢"]
    S6 --> S7["§7 Push vs Pull<br/>🟡"]
    S7 --> S8["§8 Dynamic content<br/>🟡"]
    S6 --> S9["§9 Multi-tier + shield<br/>+ thundering herd<br/>🔴"]
    S6 --> S10["§10 Routing mechanisms<br/>🟡🔴"]
    S7 --> S11["§11 Content consistency<br/>🟡🔴"]
    S9 --> S12["§12 Deployment<br/>🟡"]
    S8 --> S13["§13 Buy vs build<br/>🟡"]
    S9 --> S13
    S6 --> S14["§14 API design<br/>🟡"]
    S6 --> S15["§15 Security<br/>🟡"]
    S10 --> S15
    S3 --> S16["§16 Evaluate vs NFRs<br/>🟢"]
    S11 --> S16
    S12 --> S16
    S13 --> S16
    S14 --> S16
    S15 --> S16
    S16 --> S17["§17 Interview playbook<br/>🟡"]
    S17 --> S18["§18 Putting it all together<br/>v1→v4 capstone<br/>🟢🟡🔴"]
    S18 --> S19["§19 Active Recall<br/>🟢🟡🔴"]
    S19 --> S20["§20 Golden Rules<br/>🟢"]
    S20 --> MCS["Master Cheat Sheet<br/>🟢"]

    classDef beginner fill:#d4f4dd,stroke:#2f9e44,color:#1a3d1f
    classDef intermediate fill:#fff3bf,stroke:#e8a409,color:#3d3313
    classDef advanced fill:#ffe0e0,stroke:#e03131,color:#3d1a1a
    class S1,S2,S4,S5,S6,S16,S20,MCS beginner
    class S7,S8,S10,S12,S13,S14,S15,S17 intermediate
    class S3,S9,S11,S18,S19 advanced
```

**First read (never touched CDN internals before)?** Do the green path: §1 → 1.1 → §2 → §4 → §5 → §6 → §16 → §20. That alone gives you a correct, coherent picture of what a CDN is, why it exists, and what it's made of. Everything else is depth layered on top.

**Revising for an interview?** Skim the TL;DR line under every heading first (two minutes, gives you the whole shape), then jump straight to §19 (Active Recall) and only go back to a section when you can't answer its question cold.

### Quick glossary (look up, don't memorize — memorizing happens naturally by §9)

| Term | One-line meaning | Defined in |
|---|---|---|
| **CDN (Content Delivery Network)** | A geographically distributed cache + routing + security layer sitting between users and an origin — the subject of this whole guide | §1 |
| **PoP (Point of Presence)** | One of the CDN's physical edge locations — a "store" in the franchising analogy | §1 |
| **Origin (server)** | The source-of-truth server the CDN sits in front of — the "factory" that only gets shipped to on a cache miss | §1 |
| **Push vs. Pull** | Push = content is proactively shipped to the edge before anyone asks; pull = the edge fetches lazily on first request | §7 |
| **DNS (Domain Name System)** | The system that maps a hostname to an IP — the mechanism a CDN typically uses to send a client to its *nearest* PoP | §5, §10 |
| **TTL (Time To Live)** | How long a cached object may be served before it's treated as stale and re-checked | §4, §11 |
| **Cache invalidation / purge** | An active, operator-initiated "evict this now" — a different mechanism from TTL expiry, not a faster version of it | §11.3 |
| **Lease** | A push-style consistency contract: the origin promises to actively notify the proxy of changes, instead of the proxy polling on a timer | §11 |
| **RTT (Round-Trip Time)** | How long it takes a packet to reach its destination and the reply to come back — the natural unit for network latency | §2 |
| **QPS (Queries/Requests Per Second)** | The standard unit for request-rate capacity math | §3 |
| **DAU (Daily Active Users)** | Distinct users active in a day — the usual starting input for a capacity estimate | §3 |
| **LRU / LFU (Least Recently / Frequently Used)** | The two standard cache-eviction policies — which cached object to throw out first when space runs out | §4 |
| **Origin shield** | A single consolidated caching tier between every edge PoP and the origin, that collapses many edges' misses into one origin fetch | §9.2 |
| **Request coalescing (single-flight)** | At one proxy: the first miss for a key becomes the "leader" and fetches; concurrent misses for the same key wait on it instead of each firing their own fetch | §9.3 |
| **Anycast** | Many edge servers announce the *same* IP address; BGP routes each client to the topologically nearest one | §10 |
| **BGP (Border Gateway Protocol)** | The internet's inter-network routing protocol — the actual mechanism that makes anycast's "nearest instance" behavior happen | §10 |
| **DDoS (Distributed Denial-of-Service)** | An attack that floods a target with traffic from many sources at once to overwhelm it | §2, §15 |
| **SPOF (Single Point of Failure)** | Any one component whose failure alone can take the whole system down — the thing redundancy exists to remove | §2 |
| **IXP (Internet Exchange Point) / ISP (Internet Service Provider)** | An IXP is a physical facility where many networks interconnect; an ISP is the company that gets a user's traffic onto the internet | §12 |
| **ESI (Edge Side Includes)** | Markup letting a proxy cache 95% of a page and only re-fetch the changed 5% | §8 |
| **DASH / HLS (adaptive streaming protocols)** | Manifest-based video delivery where the client picks a bitrate/resolution per network conditions, instead of one fixed file | §8 |
| **VCL (Varnish Configuration Language) / Wasm (WebAssembly)** | VCL is Fastly's programmable caching/routing config language; Wasm is the binary format Fastly's newer Compute@Edge runs at the edge | §13.1 |
| **OCA (Open Connect Appliance)** | Netflix's own edge caching box, placed inside ISPs/IXPs, powering its private CDN (Open Connect) | §13 |
| **API (Application Programming Interface)** | The set of callable operations a system exposes — here, the six operations a CDN's components use to talk to each other | §14 |

---

## 1. What it is, in one mental model 🟢

**TL;DR:** A CDN is a geographically distributed cache layer that also routes traffic to the right place and shields the origin from load/attacks — miss any one of those three jobs and you haven't described a real CDN.

A CDN is a **geographically distributed cache layer sitting between users and your origin**. Think of it as franchising: instead of every customer flying to your one factory (origin data center), you open small stores (**PoPs** — Points of Presence, the CDN's physical edge locations) close to where customers live, stock them with your most popular products (cached content), and only ship from the factory when a store doesn't have what's needed (cache miss → origin fetch).

The core insight interviewers want to hear: **a CDN doesn't just cache — it also does traffic routing (send the user to the *right* store) and traffic shielding (protect the factory from being overwhelmed or attacked)**. Caching, routing, and security are the three pillars — miss any one and you haven't described a real CDN.

```mermaid
graph LR
    U[User] -->|1 DNS lookup| R[Routing System]
    R -->|2 nearest PoP IP| U
    U -->|3 HTTP request| S[Scrubber<br/>DDoS filter]
    S --> P[Edge Proxy Server<br/>RAM/SSD cache]
    P -->|cache hit| U
    P -->|cache miss| O[(Origin Server)]
    O --> P
```

*(DNS = Domain Name System, the system that translates a hostname into an IP address — full treatment in a dedicated guide; here it's just "the thing that hands the client an IP for the nearest PoP," covered in depth in §10.)*

> **Memory hook — the one idea that repeats three times in this chapter:** **Push = someone proactively ships you something before you ask. Pull = you ask, then someone fetches it.** Watch for it in (1) how content reaches the edge (§7), and (2) how the edge learns content went stale (§11). It's also the exact same trade-off as *fan-out-on-write vs. fan-out-on-read* in feed systems (Twitter/Instagram). Learn the duality once here and you can reuse it in half the other system design chapters.

### 1.1 Disambiguation: CDN vs. reverse proxy 🟢

**TL;DR:** A CDN's edge proxy *is* a reverse proxy — a CDN is just many reverse proxies, deployed globally, with routing and security bolted on top.

Interviewers will sometimes ask "isn't a CDN just a reverse proxy?" — the honest answer is "yes, scaled out geographically, with routing and security bolted on."

| | CDN | Reverse Proxy |
|---|---|---|
| **Scope** | Many geographically distributed edge locations (PoPs) | Typically one site / one data center in front of one set of origin servers |
| **Primary goal** | Cut latency for a globally distributed user base + absorb load/attacks at internet scale | Load balancing, SSL termination, hiding origin topology for a single deployment |
| **Routing** | Global routing to the nearest PoP (DNS redirection / anycast) | Local routing/load-balancing within one location |
| **Caching** | Core feature, tuned for global fan-out and long-tail popularity | Often does caching too (Nginx, Varnish), but scoped to one location, not globally replicated |
| **Relationship** | An edge proxy inside a CDN *is* essentially a reverse proxy | A reverse proxy is one of the CDN's building blocks, not the whole CDN |

**The same idea, drawn instead of described** — a single reverse proxy has one scope; a CDN is the same box, replicated globally, with a routing decision added in front:

```mermaid
graph TB
    subgraph RP["Reverse proxy — one site, one scope"]
        direction LR
        C1[Client] --> Proxy1[Reverse Proxy] --> BE1[Backend servers<br/>one data center]
    end
    subgraph CDN2["CDN — many reverse proxies + global routing"]
        direction TB
        Ru[Users — US / EU / Asia] --> Rt{Routing system:<br/>pick nearest PoP}
        Rt --> P1[Edge Proxy<br/>US PoP]
        Rt --> P2[Edge Proxy<br/>EU PoP]
        Rt --> P3[Edge Proxy<br/>Asia PoP]
        P1 -->|miss| Or[(Origin)]
        P2 -->|miss| Or
        P3 -->|miss| Or
    end
```

Each edge proxy box in the CDN diagram is doing exactly what the single reverse proxy is doing on the left — the only new things a CDN adds are the routing decision ("which PoP?") and doing it at many locations instead of one.

**Cheat-sheet:**
- CDN = geographically distributed cache + routing + security — not "a cache," and not "a reverse proxy," though it's built from both ideas.
- Three pillars: caching, routing, security. Always name all three, every time.
- Mental model: franchising — PoPs are stores, origin is the factory, cache miss = a shipment from the factory.
- A CDN edge proxy is a reverse proxy; a CDN is many reverse proxies deployed globally with routing on top.
- The push/pull duality introduced here reappears in push-vs-pull CDNs (§7) and TTL-vs-lease consistency (§11) — learn it once.

---

## 2. Why it exists — the problem, quantified 🟢

**TL;DR:** Without a CDN, one origin data center has to serve every user on Earth over the raw internet — and physics (latency), bandwidth economics, and blast radius all break at that scale, independent of how well the origin itself is engineered.

Without a CDN, a single origin data center serving a global user base hits three walls:

| Problem | Root cause | Concrete cost |
|---|---|---|
| **High latency** | Propagation delay ∝ distance; transmission delay ∝ bandwidth; queuing delay ∝ congestion; + nodal processing delay | US-East ↔ US-West RTT ≈ 63ms; US-East ↔ Africa RTT ≈ 226ms. VoIP needs <150ms one-way; interactive apps <200ms; video streaming tolerates a few seconds (buffered) |
| **Data-intensive traffic** | Origin must send a full copy to *every* requester individually; small-MTU links along the path throttle throughput | Streaming is both data-heavy *and* dynamic — worst of both worlds |
| **Resource scarcity / SPOF** | Compute + bandwidth at one DC don't scale infinitely; one DC = one blast radius | A regional outage or fiber cut takes down 100% of users |

*(RTT = Round-Trip Time, how long it takes a packet to reach its destination and the reply to come back — the natural unit for network latency, which is why the table above and the "numbers to know" section measure things in RTTs rather than raw distance. SPOF = Single Point of Failure — the one component whose failure alone can take the whole system down; a lone origin data center is the textbook example.)*

**Numbers worth quoting in an interview:**
- Real-time/interactive apps: latency budget **< 200ms**; VoIP **< 150ms**.
- Netflix + YouTube + Amazon Prime Video = **~80% of internet traffic** (2020).
- Akamai historically served **15–30% of all web traffic** (~30 Tbps), and was **one network hop away for 90% of internet users**.
- Netflix's own CDN (Open Connect) achieves a **~95% cache hit ratio**.

**Making the trade-off concrete — CDN vs. no CDN:**

| Dimension | Without a CDN | With a CDN |
|---|---|---|
| **Latency** | Full WAN RTT to one origin region, for every user, globally (e.g., 226ms US↔Africa) | Sub-few-ms to tens-of-ms to the nearest PoP for cached content; only misses pay the WAN RTT |
| **Origin load** | 100% of requests hit origin — must be provisioned for peak *global* traffic | Only cache misses (often <5-10%) reach origin — origin sized far smaller |
| **Bandwidth cost** | Origin pays full egress bandwidth for every byte, to every user | CDN absorbs the bulk of egress; origin egress shrinks roughly by the cache hit ratio |
| **Availability** | Single region/DC = single blast radius; one fiber cut or regional outage takes everyone down | Cached content survives origin outages entirely; many PoPs = no single blast radius |
| **DDoS resilience** | Attack traffic hits origin directly — origin capacity is the only defense | Anycast + scrubber servers absorb/disperse attack traffic before it nears origin |
| **Cost profile** | Lower fixed cost, poor scaling under global/spiky load | Ongoing CDN spend (or capex if built), but far cheaper than over-provisioning origin for global peak |

*(DDoS = Distributed Denial-of-Service, an attack that floods a target with traffic from many sources at once to overwhelm it — full mitigation treatment in §15.)*

---

## 3. Capacity estimation — a worked example 🔴

**TL;DR:** A 95% cache hit ratio isn't just "faster" — run the numbers and it turns a ~350K QPS problem into a ~17.5K QPS problem for the origin, which is the actual economic argument for building or buying a CDN.

Interviewers expect you to turn the "why it exists" numbers into an actual back-of-envelope sizing exercise. Walk through it out loud — the exact numbers matter far less than showing which lever moves which output.

**Given:**
- 500M daily active users (**DAU** — distinct users active in a day, the usual starting input for this kind of estimate)
- ~20 content requests/user/day (images, API fragments, page assets — state this assumption explicitly, it's the one the interviewer will push on)
- Average object size: 2KB
- Edge cache hit ratio: 95% (matches Netflix Open Connect's real-world number from §2)

**Step 1 — total request volume:**
- Requests/day = 500M × 20 = **10B requests/day**
- Average **QPS** (Queries/Requests Per Second — the standard unit for this kind of math) = 10B / 86,400s ≈ **116K QPS**
- Peak QPS (assume 3× average for a typical diurnal curve) ≈ **350K QPS**

**Step 2 — origin QPS after CDN offload:**
- Only cache misses reach origin: 5% of traffic
- Origin avg QPS = 116K × 0.05 ≈ **5.8K QPS**
- Origin peak QPS = 350K × 0.05 ≈ **17.5K QPS**
- **Say this number out loud:** the CDN turns a 116K–350K QPS problem into a ~6K–17.5K QPS problem for the origin — a **>20x reduction**, which is the entire economic argument for a CDN.

**Step 3 — bandwidth:**
- Total daily bytes = 10B requests × 2KB ≈ **20TB/day**
- Without a CDN: origin serves all 20TB/day ⇒ ≈1.85 Gbps sustained average egress (≈5.6 Gbps at peak)
- With CDN (95% hit ratio): origin only serves 5% = 1TB/day ⇒ ≈93 Mbps average egress — cheap enough for modest origin infrastructure
- Bandwidth **saved** from the origin's bill ≈ 19TB/day, now absorbed by the CDN's edge network instead

**Step 4 — rough PoP count and storage sizing:**
- To keep RTT low for 500M globally distributed users, aim for wide coverage: **100–300 PoPs** is a reasonable target (real-world anchors: Fastly runs ~100 larger PoPs, Cloudflare ~300 cities, Akamai thousands of smaller ones — a philosophy trade-off discussed later in this guide)
- Per-PoP average load ≈ 116K QPS / 150 PoPs ≈ **~770 QPS/PoP** (uneven in practice — weight toward population-dense regions)
- Storage: content popularity typically follows a power-law/long-tail distribution, so the "hot set" responsible for most hits is a small fraction of the total catalog. A hot working set of, say, 10M objects × 2KB ≈ **20GB** — trivially fits in RAM on a modern edge box; a much larger long-tail catalog (100M+ objects, hundreds of GB) lives on SSD at a parent/shield tier instead of being replicated to every edge PoP.

**The whole pipeline as one funnel — this is the picture worth drawing on a whiteboard:**

```mermaid
flowchart LR
    Q["~116K avg / ~350K peak QPS<br/>hits the edge, globally"] --> Cache{"Edge cache<br/>(95% hit ratio)"}
    Cache -->|"~95% — cache hit"| Served["Served from RAM/SSD at the edge<br/>~ms latency, origin never touched"]
    Cache -->|"~5% — cache miss"| Origin["Reaches origin:<br/>~5.8K avg / ~17.5K peak QPS"]

    style Served fill:#d4f4dd,stroke:#2f9e44,color:#1a3d1f
    style Origin fill:#ffe0e0,stroke:#e03131,color:#3d1a1a
```

**Read the funnel left to right:** everything entering on the left is the traffic *users* generate; the cache is the one divider that decides how much of it the *origin* ever has to see. A 95% hit ratio means the origin's real job is roughly 1/20th the size of the edge's job — that ratio is the single number that justifies the entire CDN's existence, cost included.

**Takeaway to say in the room:** *"A CDN doesn't just make things faster — at a 95% hit ratio it cuts origin QPS and bandwidth by roughly 20x, which is the number that actually justifies the infrastructure spend."*

---

## 4. Requirements (how to open the interview) 🟢

**TL;DR:** Restate the design goal as "minimize distance between bytes and eyeballs without serving stale/unsafe data and without the origin ever being a SPOF" — that one sentence anchors every decision that follows.

### Functional
- **Retrieve** — proxy pulls content from origin.
- **Deliver** — origin pushes content to proxies (push model).
- **Request** — client asks a proxy for content.
- **Search** — a proxy checks peer proxies in the same PoP for content it doesn't have locally.
- **Update** — propagate changes to peer proxies (relevant when edge scripts/serverless functions mutate content).
- **Delete/evict** — expire stale or cold content (standard cache eviction: **LRU/LFU** — Least Recently Used / Least Frequently Used, the two standard policies for which cached object gets thrown out first when space runs out — plus **TTL**, Time To Live, how long a cached object is trusted before it's re-checked; both get the full treatment in §11).

### Non-functional
- **Performance** — minimize latency (the #1 KPI).
- **Availability** — must survive origin failure, proxy failure, and active attacks (**DDoS** — Distributed Denial-of-Service, an attack flooding a target with traffic from many sources at once).
- **Scalability** — horizontal scale-out as request volume grows.
- **Reliability & security** — no single point of failure (**SPOF**); protect hosted content from abuse.

**Interview framing tip:** when asked "design a CDN," restate the goal as *"minimize the distance (and hops) between bytes and eyeballs, without serving stale or unsafe data, and without the origin ever being a single point of failure."* That one sentence anchors every design decision that follows.

---

## 5. Building blocks it's built from 🟢

**TL;DR:** A CDN isn't a new primitive — it's DNS, load balancers, and cache-eviction theory, composed and deployed geographically; naming those explicitly signals systems thinking.

A CDN is a composite of building blocks you've likely already covered:
- **DNS** (Domain Name System — maps a hostname to the IP of an appropriate proxy server; this is *how* routing is implemented, covered in depth in §10).
- **Load balancers** — spread requests across the proxies within a chosen location.
- **Caching (LRU/LFU/TTL eviction)** — same theory as an application cache, applied at the edge.
- **Consistent hashing** — often used to shard *which* content lives on *which* proxy within a PoP.

Name-dropping "this reuses the DNS and Load Balancer building blocks, plus cache eviction theory" signals systems thinking to the interviewer.

---

## 6. Components — the anatomy of a CDN 🟢

**TL;DR:** Six named components, each with one job — routing decides *which* PoP, distribution decides *what* gets there, and everything else (scrubbing, serving, shielding, reporting) hangs off those two decisions.

```mermaid
graph TB
    Client[Clients] --> RS[Routing System]
    RS -->|nearest PoP IP| Client
    Client --> Scrub[Scrubber Servers<br/>DDoS filtering]
    Scrub --> Proxy[Edge Proxy Servers<br/>RAM hot data / SSD cold data]
    Dist[Distribution System] --> Proxy
    Proxy -.feedback.-> RS
    Proxy --> Mgmt[Management System<br/>metrics, billing, health]
    Mgmt --> Origin[Origin Servers]
    Origin --> Dist
    Proxy -->|on miss| Origin
```

| Component | Job | Notes |
|---|---|---|
| **Routing system** | Decides *which* PoP/proxy a client should hit | Inputs: content placement, request volume, server load, URI namespace |
| **Scrubber servers** | Separate malicious traffic from legitimate traffic | Only activated when an attack is detected — traffic is "scrubbed" then forwarded |
| **Proxy (edge) servers** | Serve content, mostly from RAM; SSD/HDD for cold/long-tail content | This is where cache hit/miss happens |
| **Distribution system** | Pushes content from origin out to all edge proxies | Uses tree/broadcast-like fan-out, not naive 1:1 |
| **Origin servers** | Source of truth; serve on cache miss | Out of scope to design internally, but must be shielded |
| **Management system** | Observability + billing | Tracks latency, downtime, packet loss, server load; feeds routing decisions back |

**End-to-end workflow** (say this out loud in an interview to show you understand data *and* control planes):
1. Origin registers its URI namespace with the routing system.
2. Origin publishes content to the distribution system.
3. Distribution system fans content out to edge proxies and reports back to the routing system (which proxy has which content).
4. Client asks the routing system for a proxy → gets back an IP.
5. Client's request passes through scrubber servers → forwarded to the edge proxy.
6. Proxy serves the content (or forwards up the hierarchy / to origin on a miss) and reports accounting data to the management system.

### 6.1 One diagram to remember it all by 🟢

**TL;DR:** This sequence diagram is the entire chapter compressed into one picture — if you can redraw it from memory, you can redraw the whole guide.

This sequence diagram fuses routing, security, tiered caching, and the management feedback loop into a single walkthrough. If you can redraw this from memory, you can redraw the whole chapter.

```mermaid
sequenceDiagram
    autonumber
    participant U as User
    participant D as DNS (routing system)
    participant Sc as Scrubber
    participant E as Edge Proxy
    participant Pa as Parent Proxy (Tier-1)
    participant O as Origin
    participant M as Management System

    U->>D: Resolve hostname
    D->>U: IP of nearest healthy edge proxy
    opt Attack detected
        U->>Sc: Request routed through scrubber
        Sc->>E: Clean traffic forwarded
    end
    U->>E: HTTP request (direct path if no attack)
    alt Cache hit at edge
        E->>U: Serve from RAM/SSD
    else Cache miss at edge
        E->>Pa: Escalate to parent tier
        alt Parent has it
            Pa->>E: Return content
        else Parent also misses
            Pa->>O: Fetch from origin
            O->>Pa: Return content
            Pa->>E: Forward content
        end
        E->>U: Serve + cache locally for next time
    end
    E->>M: Report accounting/health stats
    M->>D: Feed back load info to improve future routing
```

**Cheat-sheet:**
- Six components to name in order: routing system → scrubber servers → edge proxies → distribution system → origin servers → management system.
- Routing system decides *which* PoP; distribution system decides *what* content gets there — don't conflate the two.
- Scrubber servers only sit in the hot path during an attack — not overhead on every normal request.
- Proxy servers serve RAM-first, SSD/HDD for cold/long-tail — mirrors any LRU-based cache design.
- Management system is the feedback loop — it's what lets routing get smarter over time (load-aware, not just distance-aware).
- The distribution system fans out tree-style (§9), never origin-to-every-edge-directly — that would defeat the point of having a CDN.
- Redraw §6.1's sequence diagram from memory — it's the single diagram that ties every component together.

---

## 7. Push vs. Pull CDN — the central design decision 🟡

**TL;DR:** Push means the origin ships content to the edge before anyone asks; pull means the edge fetches lazily on first request — real systems pick per content type, not CDN-wide.

This is the single most-asked CDN design question. Know it cold.

```mermaid
sequenceDiagram
    participant O as Origin
    participant D as Distribution System
    participant P as Proxy/PoP
    participant U as User
    Note over O,P: PUSH MODEL
    O->>D: Publish new/updated content
    D->>P: Proactively fan out to PoPs
    U->>P: Request
    P->>U: Serve (already cached)
```

```mermaid
sequenceDiagram
    participant U as User
    participant P as Proxy/PoP
    participant O as Origin
    Note over U,O: PULL MODEL
    U->>P: Request
    P->>P: Cache miss
    P->>O: Fetch on demand
    O->>P: Return content
    P->>U: Serve + cache for next time
    U->>P: Next request (same content)
    P->>U: Serve (cache hit)
```

> **Duality callback:** push is the origin acting in the *future tense* ("I'll ship you what I think you'll need"); pull is the edge acting in the *present tense* ("I'll fetch exactly what was just asked for"). Same shape you'll see again in §11.

| | Push CDN | Pull CDN |
|---|---|---|
| **Who decides what's cached** | Origin (proactively ships it) | CDN (lazily fetches on first request) |
| **Best for** | Static content, predictable popularity | Dynamic / rapidly-changing / long-tail content |
| **Replica count** | Higher (content pushed broadly) → better availability | Lower — pulled only where/when requested → lower storage cost |
| **Failure mode** | Redundant pushes if content changes fast; wasted bandwidth | Origin gets hit on every unique first-access + on expiry |
| **Storage cost** | Higher | Lower |
| **Real-world default** | Software downloads, images, video segments known in advance | API responses, personalized fragments, breaking content |

**Interview line:** *"Most real systems use both — push for known-popular static assets, pull with TTL-based expiry for everything else."* This is literally what Netflix, Akamai, and most CDNs do in practice.

---

## 8. Dynamic content caching (the "gotcha" most candidates miss) 🟡

**TL;DR:** "CDN = static content only" is the single most common wrong assumption in this whole topic — real CDNs accelerate dynamic and personalized content too, via edge compute, compression, and partial-page caching.

Naively, candidates think "CDN = static content only." Strong answers cover **dynamic content acceleration**:

> **Where people get this backwards:** the instinct is "this response is personalized/dynamic, so it can't be cached — skip the CDN entirely and go straight to origin." That's the wrong question. The right question is *"how little of this request actually needs the origin?"* — not *"can I cache the whole thing?"* Even a fully personalized response benefits from work happening at the edge instead of round-tripping to origin: TLS termination, auth/token checks, geo-based header rewriting, A/B-test bucket assignment, and compression can all run in an **edge script** milliseconds from the user, with only the truly personal computation (or nothing at all) actually reaching origin. Treating "dynamic" and "no CDN benefit" as synonyms is exactly the gap this section exists to close.

- **Edge compute / edge scripting** — run small scripts *at the proxy* instead of round-tripping to origin (e.g., generate content based on user geo, time-of-day, or a third-party weather API). This is the ancestor of modern **Cloudflare Workers / Lambda@Edge**.
- **Compression** — reduce origin↔proxy chatter and storage footprint (e.g., Cloudflare's **Railgun**).
- **ESI (Edge Side Includes)** — a markup language letting you cache 95% of a page and only re-fetch the 5% that changed, instead of invalidating the whole page. Not W3C-standardized but widely implemented.
- **DASH (Dynamic Adaptive Streaming over HTTP)** — a manifest file lists the same video at multiple bitrates/resolutions; the client picks based on current network conditions. Netflix runs a proprietary DASH variant using **byte-range requests** within a single URL for finer-grained optimization.

**Decision flow — how much of a page/response can actually be cached?**

```mermaid
flowchart TD
    A[Incoming content] --> B{Fully static?}
    B -->|Yes| C[Push to edge, long TTL<br/>images, JS/CSS bundles, video segments]
    B -->|No| D{Mostly static, with a<br/>small changing region?}
    D -->|Yes| E[Cache the whole page,<br/>patch changed fragments with ESI]
    D -->|No| F{Can it be computed<br/>at the edge itself?}
    F -->|Yes| G[Run edge script/serverless function<br/>geo content, A/B tests, header rewriting]
    F -->|No — fully personalized| H[Pull from origin,<br/>no cache or very short TTL]
```

**Cheat-sheet:**
- Static → push, long TTL, high replication.
- Dynamic/personalized → pull, edge scripting, short TTL or event-driven invalidation.
- Video → DASH/HLS manifests + byte-range + adaptive bitrate, not "cache the whole file." (**HLS** = HTTP Live Streaming, Apple's competing adaptive-streaming protocol — same manifest-plus-segments idea as DASH, different format; mentioning both by name signals breadth.)
- Even a fully personalized response can have *part* of its handling (auth, TLS, geo-routing) run at the edge — "dynamic" doesn't mean "no CDN involvement at all."

---

## 9. Multi-tier (layered) CDN architecture 🔴

**TL;DR:** Production CDNs are never a flat set of edge proxies talking straight to origin — content fans out tree-style through one or two parent tiers, which is what lets thousands of edges scale without turning every cache miss into a direct hit on origin.

```mermaid
graph TD
    Origin[(Origin Server)] --> T1a[Tier-1 Parent Proxy A]
    Origin --> T1b[Tier-1 Parent Proxy B]
    T1a --> E1[Edge Proxy 1]
    T1a --> E2[Edge Proxy 2]
    T1b --> E3[Edge Proxy 3]
    T1b --> E4[Edge Proxy 4]
    E1 -.miss, ask parent.-> T1a
```

### 9.1 The tree structure 🔴

- Content fans out **tree-style**: origin → parent/tier-1 proxies → edge proxies. This avoids the origin having to push to thousands of edges directly (fan-out burden), and lets you scale by adding tree nodes, not by scaling the origin.
- Typically **1–2 tiers** of proxies (caches) in practice.
- A new proxy joining the tree registers with the **control core**, which hands it configuration + seed content.
- **Long-tail justification**: content popularity follows a power law (long-tail distribution) — a small set of objects gets most requests, and a huge long tail gets occasional requests. Multi-tier lets edge proxies hold only the hot set in RAM while a parent tier (or SSD at the edge) absorbs the long tail, instead of every edge proxy needing to hold everything.
- **Failure handling**: if an edge proxy misses, escalate to its parent; if the parent misses too, fall back to origin. If a parent or origin fails outright, that's where redundancy (multiple parents, cached data outliving a dead origin) keeps things available — this is exactly the kind of failure-mode question ("what if a child/parent/origin fails?") interviewers probe on.

### 9.2 Origin shield — the origin's bodyguard 🔴

**TL;DR:** An origin shield is one consolidated tier every edge PoP's misses funnel through, so N PoPs missing on the same object still results in exactly one origin fetch.

An **origin shield** is a single, designated caching tier — often just one specific PoP or region — that sits between *every* edge PoP and the origin. Every edge PoP's cache miss is routed to the shield first, and only the shield is allowed to talk to origin directly.

What it buys you:
- **Request coalescing at scale** — if 200 edge PoPs all miss on the same viral object at once, only the shield fetches origin once and serves the other 199 waiting edges, turning an N-way thundering herd into a single origin request.
- **A second, higher-hit-ratio cache tier** — the shield sees the aggregate traffic of every edge PoP behind it, so an object that's cold at any one edge is often already warm at the shield.
- **A simpler, smaller "known clients" list at origin** — origin only ever needs to trust/scale for requests from the shield, not from every edge PoP in the world.

This is the same tree/multi-tier idea from §9.1, just given a product name: **AWS CloudFront calls this "Origin Shield," Fastly calls it "shielding,"** and Akamai's tiered-distribution parent layer serves the same purpose. Mentioning the concept *and* one real product name is what separates a strong answer from a generic one here.

### 9.3 Preventing thundering herd on a cold cache 🔴

**TL;DR:** Two different fixes solve two different scopes of the same herd problem — request coalescing stops it *within* one PoP, an origin shield stops it *across* PoPs, and production systems need both, stacked.

"Cold cache" shows up two ways: (a) one specific object suddenly goes viral and every edge misses on it at once, or (b) a whole PoP is cold — just spun up, empty cache, first wave of traffic. Both produce the same failure mode if unhandled: hundreds of concurrent requests for the same key each fire their own origin fetch.

The fix at a single proxy is **request coalescing** (a.k.a. "single-flight" or a cache-fill lock): the first request for a missing key becomes the *leader* and actually fetches; every other concurrent request for that same key *waits* on the leader's in-flight fetch instead of starting its own.

```mermaid
flowchart TD
    A[Request arrives at edge<br/>for key K] --> B{Is K in cache<br/>and fresh?}
    B -->|Yes| C[Serve from cache<br/>cache hit]
    B -->|No| D{Is there already an<br/>in-flight fetch for K?}
    D -->|Yes| E[Join the wait list for K<br/>no new fetch started]
    D -->|No| F[Become the leader for K<br/>mark K as in-flight]
    F --> G[Fetch K from<br/>origin shield / origin]
    G --> H[Populate cache with K]
    H --> I[Serve leader + release<br/>every waiter with the result]
    E --> I
```

**Illustrative example** (numbers are for intuition, not measured): a promo image goes viral and 200 edge PoPs each get 50 concurrent requests for it in the same second — 10,000 requests total. Without coalescing, that's up to 10,000 origin fetches. With per-PoP request coalescing, each PoP fetches at most once, so origin sees at most 200 requests — and with an origin shield in front of those 200 PoPs (§9.2), the shield coalesces *those* down to a single origin fetch. The two mechanisms stack: coalescing kills the herd *within* a PoP, the shield kills the herd *across* PoPs.

**If X then Y:**
- Many concurrent requests miss on the *same key* at the *same edge* → request coalescing — one fetch, everyone else waits.
- Many *different edges* miss on the *same key* around the *same time* → origin shield — collapses their fetches into one.
- A whole PoP is cold (freshly deployed, empty cache) → let it warm from a neighboring PoP or the shield tier first, rather than hammering origin directly.

### 9.4 Cache-hit vs. cache-miss (with origin shield) — sequence diagrams 🔴

**TL;DR:** §6.1's combined diagram is the one to know cold; here the hit path and the miss-with-shield path are pulled apart so you can draw either one in isolation when asked.

§6.1's combined diagram is the one to know cold, but interviewers often ask for the hit and miss paths in isolation — here they are, split out.

**Cache hit:**
```mermaid
sequenceDiagram
    participant U as User
    participant E as Edge Proxy
    U->>E: GET /object.jpg
    E->>E: Lookup in RAM/SSD cache
    Note over E: Fresh, TTL not expired
    E->>U: 200 OK — served from edge, origin never contacted
```

**Cache miss, with origin shield:**
```mermaid
sequenceDiagram
    participant U as User
    participant E as Edge Proxy
    participant Sh as Origin Shield
    participant O as Origin
    U->>E: GET /object.jpg
    E->>E: Lookup in cache — miss
    E->>Sh: Forward request (edge never talks to origin directly)
    alt Shield already warm (another edge missed on this earlier)
        Sh->>E: Return content
    else Shield also misses
        Sh->>Sh: Coalesce with any other in-flight<br/>edge requests for the same key
        Sh->>O: Single fetch from origin
        O->>Sh: Return content
        Sh->>Sh: Cache at shield tier
        Sh->>E: Return content
    end
    E->>E: Cache locally
    E->>U: 200 OK
```

---

## 10. Finding the nearest proxy server (routing mechanisms) 🟡🔴

**TL;DR:** "Nearest" means lowest network distance plus current load, never plain geographic distance — DNS redirection and anycast are the two real mechanisms for getting a client there.

Two factors define "nearest":
1. **Network distance** = path length × available bandwidth (NOT geographic distance — a nearby proxy over a congested link can be "farther" than a distant one over a fat pipe).
2. **Request load** — route away from an overloaded proxy even if it's otherwise closest.

> **Where people get this backwards:** "nearest" sounds like it should mean geographic distance, so the natural first instinct is to reach for lat/long math — pick whichever PoP is physically closest on a map. Real routing systems don't do that. They route on *network* distance (path length × available bandwidth) plus *live load*, and those two don't always agree with the map: a proxy 2,000 miles away over an uncongested backbone can legitimately be "nearer" than one 200 miles away across a congested peering link, or one that's simply overloaded right now. Say "network distance and load, not geography" out loud — it's a one-sentence answer that immediately signals you're not just describing a straight-line-distance lookup.

| Mechanism | How it works | Pros | Cons |
|---|---|---|---|
| **DNS redirection** | Client resolves a hostname → CDN's authoritative DNS resolves again → returns IP of nearest proxy, using **short TTLs** so it can re-balance quickly | Considers both network distance and load; industry standard (Akamai) | Two-step resolution adds latency; granularity limited to the *resolver's* location, not the actual client (can misroute if client uses a distant public resolver) |
| **Anycast** | Many edge servers share **one IP address**; **BGP** (Border Gateway Protocol, the internet's inter-network routing protocol) naturally routes the client to the topologically nearest one | Simple client-side (no redirect logic); leverages internet routing infrastructure | Coarser control over load-balancing; BGP convergence can be slow on failure; harder to do fine-grained content-aware routing |
| **Client multiplexing** | Server returns a list of candidate proxies; client picks one | No server-side computation | Client lacks visibility into load/distance → poor choices, possible pile-on to one server |
| **HTTP redirection** | Origin responds with a redirect URL pointing at the CDN (classic `<img src="cdn.example.com/...">`) | Dead simple, works everywhere | Extra round trip; redirect logic still needs to pick *a* CDN endpoint |

**DNS redirection two-step model** (a favorite whiteboard sequence):
```mermaid
sequenceDiagram
    participant C as Client
    participant LDNS as Local/ISP DNS
    participant ADNS as CDN Authoritative DNS
    participant P as Nearest Proxy
    C->>LDNS: Resolve example.com
    LDNS->>C: CNAME → cdn.xyz.com
    C->>ADNS: Resolve cdn.xyz.com
    ADNS->>C: IP of nearest proxy (short TTL)
    C->>P: HTTP request
    P->>C: Content
```

### Anycast vs. DNS geo-routing, visualized 🔴

```mermaid
graph TB
    subgraph DNS["DNS geo-routing"]
        direction LR
        U1[User in Tokyo] -->|"1 resolve hostname"| DNS1[CDN Authoritative DNS]
        DNS1 -->|"2 looks up resolver's location,<br/>returns Tokyo PoP IP"| U1
        U1 -->|"3 HTTP to that IP"| P1[Tokyo PoP]
    end
    subgraph ANY["Anycast routing"]
        direction LR
        U2[User in Tokyo] -->|"1 HTTP to the one<br/>global anycast IP"| BGP2{Internet BGP routing}
        BGP2 -->|"2 shortest AS path<br/>happens to be Tokyo"| P2[Tokyo PoP]
    end
```

With DNS geo-routing, the *DNS layer* makes the decision — one extra lookup, but the CDN can factor in real-time load per request. With anycast, every PoP announces the *same IP*, and ordinary internet routing (BGP) picks the topologically nearest one — no extra lookup, but the CDN gives up per-request control once BGP has converged.

**Which one would you actually pick?**

```mermaid
flowchart TD
    A[Choosing a routing mechanism] --> B{Need load-aware,<br/>fine-grained routing?}
    B -->|Yes| C[DNS Redirection<br/>short TTL, weighs distance + load<br/>Akamai's approach]
    B -->|No — want simplicity +<br/>built-in DDoS resilience| D[Anycast<br/>one IP, BGP routes to nearest]
    D --> E{Also need app-layer<br/>load balancing within a PoP?}
    E -->|Yes| F[Anycast to nearest PoP,<br/>then a local load balancer inside it]
    E -->|No| G[Anycast alone]
```

**Interview cheat-sheet:**
- DNS redirection = most common, used by Akamai; short TTL lets it re-route as load shifts; the cost is two DNS round trips before the first byte.
- Anycast = one IP globally, BGP-routed, simplifies client config, and doubles as free DDoS resilience (attack traffic disperses across every server advertising that IP) — Cloudflare's default.
- "Nearest" = network distance (path × bandwidth) + current load — never plain geographic distance.
- DNS redirection can misroute when a client uses a distant public resolver (e.g., 8.8.8.8) instead of its ISP's — "nearest to the resolver" isn't always "nearest to the client."
- Anycast trades fine-grained load control for simplicity — BGP knows network topology, not proxy CPU/queue depth.
- Client multiplexing and HTTP redirect are simpler but strictly worse — mention them for breadth, then explain why real systems avoid them (no server-side visibility into load/distance).
- If asked "how would you pick," lead with DNS redirection when load-aware routing matters most, anycast when DDoS resilience and client-side simplicity matter most.

---

## 11. Content consistency (cache invalidation is still the hard problem) 🟡🔴

**TL;DR:** TTL, leases, and purge are three different answers to "how does a cached copy learn it's stale," ranging from passive/cheap (TTL) to active/expensive-but-fast (purge) — and confusing them is the single easiest way to lose points in this section.

| Technique | Mechanism | Trade-off |
|---|---|---|
| **Periodic polling (TTR)** | Proxy asks origin "anything new?" on a fixed time-to-refresh interval | Simple; wastes bandwidth if content rarely changes; can serve stale data between polls |
| **TTL (time-to-live)** | Origin stamps each object with an expiry; proxy serves it as-is until expiry, then re-validates | Reduces refresh chatter vs. polling; still a window of staleness up to TTL |
| **Leases** | Origin promises to *actively notify* the proxy of changes for a lease duration; proxy renews before expiry; duration can adapt to observed load (**adaptive lease**) | Fewest messages exchanged; more complex (origin must track lease state per proxy); best when writes are rare but must propagate fast when they happen |

> **Duality callback #2:** TTL is the proxy *pulling* for freshness ("has this expired? let me go check"). A lease is the origin *pushing* freshness ("I'll tell you the moment something changes"). It's the same push/pull shape from §7, just applied to invalidation instead of delivery — once you see it, the two topics reinforce each other instead of being two things to memorize separately.

```mermaid
sequenceDiagram
    Note over P,O: TTL — "pull-style" consistency (proxy checks)
    O->>P: Content + TTL = 60s
    Note over P: Serve freely, no questions asked, for 60s
    P->>O: TTL expired — anything new?
    O->>P: Unchanged (or here's v2)
```

```mermaid
sequenceDiagram
    Note over P,O: Lease — "push-style" consistency (origin notifies)
    P->>O: Request content + lease
    O->>P: Content + lease (e.g. 60s)
    Note over O: Content changes at t = 30s
    O-->>P: Unsolicited push: content changed!
    P->>O: Lease renewal request (at expiry)
    O->>P: Renewed lease
```

**This maps directly to classic cache-invalidation theory** — TTL is the CDN analogue of a Redis `EXPIRE`, and leases are the analogue of a **write-through invalidation / pub-sub cache invalidation** pattern. If you know one, you know the other.

**Interview line:** *"Pick TTL when staleness tolerance is well understood and writes are infrequent; pick leases when you need near-real-time propagation and can afford the bookkeeping; pick polling only when the origin can't push notifications at all."*

### 11.1 Stale-while-revalidate 🔴

**TL;DR:** SWR lets a proxy serve a stale copy immediately at expiry while it refreshes in the background — nobody has to pay for a synchronous origin round trip just because a timer ran out.

Plain TTL forces a binary choice at expiry: serve stale, or make the user wait on a synchronous origin round trip. **Stale-while-revalidate (SWR)** avoids both: once an object expires, the proxy immediately serves the stale copy to the requesting user (zero added latency), while it *asynchronously* refetches the fresh version from origin in the background for the *next* request. This is standard, production behavior in **CloudFront, Fastly, and Cloudflare** (all support a `stale-while-revalidate` Cache-Control directive), and it's a strong answer to "how do you avoid every user paying the cost of an origin round trip right at expiry."

```mermaid
sequenceDiagram
    participant U as User
    participant P as Proxy
    participant O as Origin
    Note over P,O: TTL + stale-while-revalidate
    O->>P: Content + TTL=60s, stale-while-revalidate=30s
    Note over P: Serve freely, no questions asked, for 60s
    U->>P: Request at t=75s (stale, but within SWR window)
    P->>U: Serve stale content immediately, no wait
    par Background, async
        P->>O: Revalidate (conditional GET)
        O->>P: 304 Not Modified, or fresh content
        Note over P: Cache updated for the next request
    end
```

### 11.2 Cache entry lifecycle 🔴

**TL;DR:** Every cached object moves through exactly four states — Fresh, Stale, Expired, Purged — and every mechanism in this section is really just describing what triggers a transition between them.

```mermaid
stateDiagram-v2
    [*] --> Fresh: Origin serves object + TTL/lease
    Fresh --> Stale: TTL expires (or lease not renewed)
    Stale --> Fresh: Revalidation succeeds (conditional GET -> 304 Not Modified)
    Stale --> Expired: Revalidation returns new content (200 OK, old bytes invalid)
    Fresh --> Purged: Explicit purge/invalidate API (bypasses TTL entirely)
    Stale --> Purged: Explicit purge/invalidate API (push-based, out-of-band)
    Expired --> [*]: Old bytes evicted, new version cached as Fresh
    Purged --> [*]: Evicted immediately, next request is a forced miss
```

### 11.3 Cache invalidation vs. TTL expiry 🔴

**TL;DR:** Purge is an active, out-of-band "evict this now"; TTL expiry is a passive clock every cached object already has — they're two different mechanisms, not two speeds of the same one.

Candidates often use these two terms interchangeably. They're not the same mechanism:

> **Where people get this backwards:** it's tempting to treat TTL and invalidation as the same lever at different speeds — "just lower the TTL and it'll invalidate faster." It won't, not the way you need it to. TTL is a clock every cached object already carries, checked passively whenever it happens to expire — there's no way to make *already-cached* copies check sooner than their existing TTL without reaching out to each of them individually, which is exactly what a purge does and TTL doesn't. Cache invalidation (purge) is a completely separate, active, operator-initiated push that reaches out and evicts specific objects immediately, regardless of what TTL they were cached with. If a takedown or emergency fix needs to happen *now*, across every edge, the answer is a purge API (§11.5) — not "we'll just wait for TTLs to expire," and not "we lowered the TTL" (that only helps the *next* time this content is fetched, not the copies already sitting in caches right now).

| | Cache invalidation (purge) | TTL expiry |
|---|---|---|
| **Trigger** | Active, operator/origin-initiated push ("purge this now") | Passive, time-based — the proxy just checks the clock |
| **Speed** | Near-instant (propagation-dependent, e.g. Fastly's ~150ms) | Bounded by however long the TTL was set to |
| **Use case** | Emergency fix, legal takedown, urgent content correction | Routine, expected content refresh cycle |
| **Cost** | Requires a fan-out purge API/infrastructure reaching every edge | Free — built into normal cache bookkeeping |
| **Failure mode** | Purge message lost/delayed → some edges keep serving old content | Content silently stale until the next expiry check — no urgency signal |

### 11.4 How long should the TTL be? 🟡

**TL;DR:** Short TTL buys freshness at the cost of origin load; long TTL buys cheap origin load at the cost of staleness — versioned URLs let you dodge the trade-off entirely for content that changes by replacement, not by mutation.

| TTL length | Freshness | Origin load / cost | Best for |
|---|---|---|---|
| **Short** (seconds–minutes) | High — near real-time | Higher — frequent revalidation/refetch hits origin | Rapidly changing data: prices, news, near-live content |
| **Long** (hours–days–weeks) | Lower — tolerates staleness | Lower — origin rarely bothered | Static/immutable assets: versioned JS/CSS bundles, images, video segments |
| **Best practice** | — | — | Pair a long TTL with versioned/cache-busted URLs — you get the cost savings of a long TTL *without* the staleness risk, by bumping the URL on change instead of shortening the TTL |

### 11.5 How does a purge actually reach thousands of edges? 🔴

**TL;DR:** Never loop over PoPs one at a time to purge them — publish the invalidation once to a pub/sub channel and let a fan-out tree deliver it to every edge in parallel.

§11.3 says purge is "near-instant, propagation-dependent" — here's the mechanism that makes that true. A naive implementation (control plane opens a connection to each edge one at a time and tells it to evict) doesn't scale past a few dozen PoPs — that's O(number of PoPs) sequential round trips. Production CDNs instead fan a purge out through a **pub/sub invalidation channel**: publish once, and a tree of subscribers (regional aggregators, then edge PoPs) receive and apply it in parallel.

```mermaid
sequenceDiagram
    participant Op as Operator / API
    participant Ctrl as Control plane
    participant Pub as Pub/sub channel
    participant R1 as Regional aggregator A
    participant R2 as Regional aggregator B
    participant E1 as Edge PoP (under A)
    participant E2 as Edge PoP (under B)
    Op->>Ctrl: Purge /object.jpg (or surrogate key "campaign-42")
    Ctrl->>Pub: Publish invalidation event once
    par Fan-out, parallel
        Pub->>R1: Deliver event
        Pub->>R2: Deliver event
    end
    par Parallel evict
        R1->>E1: Forward event
        R2->>E2: Forward event
    end
    E1->>E1: Evict / mark stale locally
    E2->>E2: Evict / mark stale locally
```

**Illustrative numbers** (label as illustrative — real figures vary by vendor): publishing once and fanning out through 2 tiers to 1,000 edge PoPs, with each hop adding tens of milliseconds, gets full propagation in the low single-digit seconds. That's roughly the ballpark real CDNs target (Fastly advertises sub-second for its instant-purge path; other vendors run tens-of-seconds). The number matters less than the **shape**: one publish, fan-out tree, parallel delivery — never "loop over every edge one at a time," which is O(PoPs) sequential calls instead of O(log PoPs) parallel hops.

**If X then Y:**
- Need to purge one specific known URL → purge-by-URL through the pub/sub channel.
- Need to purge a whole category (e.g., every image in a campaign) without knowing every URL → tag/surrogate-key purge, so one publish evicts every object sharing that tag.
- Propagation can't wait even a few seconds → don't rely on purge at all — use versioned URLs (§11.4) so there's nothing to invalidate in the first place.

**Cheat-sheet:**
- TTL = pull-style consistency (proxy checks); lease = push-style (origin notifies). Same push/pull duality as §7.
- Cache invalidation (purge) ≠ TTL expiry: purge is active and urgent, TTL is passive and routine.
- Short TTL = fresher but hammers the origin; long TTL = cheap but stale — versioned URLs get you both.
- Stale-while-revalidate serves stale content instantly while refreshing in the background — never make the user wait on a near-fresh object.
- A cached object's lifecycle: fresh → stale → revalidated (back to fresh), or expired/purged (evicted).
- Versioned/cache-busted URLs are the most scalable invalidation strategy — the old URL's TTL becomes irrelevant once nothing links to it.
- Bring up cache invalidation unprompted — it's the "hard problem" callback interviewers listen for.

---

## 12. Deployment: where do you physically put proxy servers? 🟡

**TL;DR:** A proxy either sits in a CDN-owned facility near an internet exchange, or it sits physically inside an ISP's own network — the second option is "closer" but requires a business relationship the first doesn't.

| Placement | Description | Trade-off |
|---|---|---|
| **On-premises** | Small CDN-owned data centers near major **IXPs** (Internet Exchange Points — physical facilities where many networks interconnect) | Full control, but not literally inside the ISP — one more hop than off-premises |
| **Off-premises** | Proxy servers embedded **inside ISP networks** (**ISP** = Internet Service Provider, the company that gets a user's traffic onto the internet in the first place) | Content is genuinely "one hop away" from the user (Akamai's model); requires ISP partnerships |

**The two placements, and what "one hop away" actually means, drawn out:**

```mermaid
flowchart LR
    subgraph OnPrem["On-premises: near an IXP"]
        direction TB
        U1[User] --> ISP1[User's ISP network] --> IXP1{IXP} --> PoP1[CDN PoP<br/>CDN-owned facility]
    end
    subgraph OffPrem["Off-premises: inside the ISP"]
        direction TB
        U2[User] --> ISP2["User's ISP network<br/>(CDN box lives HERE)"]
        ISP2 --> PoP2[CDN PoP<br/>embedded in ISP]
    end
```

Off-premises genuinely removes a hop — the proxy is inside the same network the user's traffic already has to traverse to reach *anything*, instead of being one exchange-point hop further out. That's the literal meaning of Akamai's "one hop away for 90% of internet users" claim from §2.

- **Google's split-TCP trick**: terminate the client's TCP connection at IXP-level infrastructure (avoiding a fresh 3-way handshake + slow-start all the way to a distant primary data center), then forward over an already-warm, high-bandwidth persistent connection to the real data center. This is a very "senior engineer" detail to drop — it shows you understand that latency isn't just about *content* placement, it's about *connection* placement.

```mermaid
flowchart TD
    subgraph Without["Without split-TCP"]
        direction LR
        C1[Client] -->|"fresh handshake + slow-start,<br/>full WAN RTT"| DC1[(Distant primary DC)]
    end
    subgraph With["With split-TCP"]
        direction LR
        C2[Client] -->|"handshake terminates<br/>at nearby IXP infra"| IXP2[IXP-level infrastructure]
        IXP2 -->|"already-warm,<br/>persistent connection"| DC2[(Distant primary DC)]
    end
```

The client's handshake and slow-start ramp-up happen against something *nearby* either way; only the already-warmed-up middle leg has to cross the long WAN distance — that's the whole trick.

- **Placement algorithms**: tools like **ProxyTeller** optimize for hit ratio, bandwidth, and response time; greedy/random/hotspot heuristics are alternatives.
- Predictive push (deciding *what* to pre-position where, before it's even requested) is an active research/ML area — mention it if the interviewer pushes on "how do you decide what to cache where."

---

## 13. CDN as a service vs. specialized (build) CDN — the classic buy-vs-build 🟡

**TL;DR:** Buy unless delivery is your core product at massive, predictable, and cost-justifying scale — Netflix Open Connect is the canonical "we built" example, and it's a hybrid, not all-or-nothing.

| | Public CDN (buy) | Specialized/Private CDN (build) |
|---|---|---|
| **Examples** | Akamai, Cloudflare, Fastly, CloudFront | Netflix Open Connect (OCA), Google's private CDN, Facebook's |
| **Cost profile** | Lower upfront, scales with usage (can get very expensive at massive scale) | High upfront capex, cost decreases over time / at scale |
| **Control** | Limited — can't fix an outage you don't own; regional gaps if provider lacks PoPs there; some countries block specific CDN IP ranges/domains | Full control over routing, protocol, hardware, security posture |
| **Content risk** | Content sits on third-party infrastructure | Provider fully owns data-leakage risk |
| **When it makes sense** | Content delivery isn't your core differentiator; traffic is moderate | Content delivery IS the business (streaming video); traffic is massive and predictable enough to justify capex |

```mermaid
flowchart TD
    A[Buy vs. build a CDN?] --> B{Is content delivery<br/>your core product?}
    B -->|No| Buy[Buy: Akamai / Cloudflare /<br/>Fastly / CloudFront]
    B -->|Yes| C{Is traffic massive<br/>AND predictable?}
    C -->|No| Buy
    C -->|Yes| D{Need control over licensing,<br/>leak-prevention, custom protocol?}
    D -->|No| Buy
    D -->|Yes| Build[Build: Netflix Open Connect style<br/>high capex, full control]
```

### 13.1 Vendor deep-dive: CloudFront and Fastly 🟡

Naming a CDN vendor is table stakes; knowing one real, specific feature per vendor is what separates a strong answer.

**Amazon CloudFront:**
- **Lambda@Edge / CloudFront Functions** — run custom logic (auth checks, header rewriting, A/B-test routing, redirects) at the edge without a round trip to origin. Lambda@Edge runs full Node.js/Python Lambdas at a subset of edge locations; CloudFront Functions run a lighter, faster JS-only runtime at every edge location for simple, high-volume logic. This is AWS's answer to "dynamic content at the edge" from §8.
- **Regional edge caches** — an intermediate tier (fewer, larger locations than CloudFront's 400+ edge PoPs) sitting between edge and origin — functionally the same origin-shield/multi-tier-parent idea from §9.2: it holds a bigger, longer-tail cache so a miss at a small edge PoP often still resolves without touching origin.
- Deep AWS integration (**S3**, **ALB** — Application Load Balancer, AWS's L7 load balancer — **WAF** — Web Application Firewall, filters malicious HTTP requests — and **Shield**, AWS's managed DDoS-protection service) — the natural choice when the origin already lives in AWS.

**Fastly:**
- **Instant purge** — Fastly's signature feature: a purge (by single URL or by "surrogate key"/tag) propagates globally in well under a second, versus the tens-of-seconds-to-minutes typical of other CDNs. This is a real product name to cite when answering "how do you invalidate instantly across thousands of edges" (§17).
- **VCL (Varnish Configuration Language)** — Fastly exposes VCL so customers write real caching/routing logic (custom cache-key construction, request/response manipulation) instead of picking from a fixed set of dashboard toggles — more "programmable Varnish at global scale" than a black-box CDN.
- **Compute@Edge** — a **Wasm** (WebAssembly — a portable binary instruction format, here used to run compiled code safely and fast at the edge) -based edge compute platform (the successor to VCL-based edge logic) that runs Rust/JS/Go-compiled-to-Wasm at the edge for full application logic, not just caching rules — Fastly's answer to Cloudflare Workers/Lambda@Edge.
- Fastly deliberately runs **fewer, larger PoPs** than Akamai/Cloudflare — a philosophy trade-off (bigger caches, more compute per node, at the cost of being physically farther from some users) worth naming if asked "how many PoPs should a CDN have."

### Case study: why Netflix built Open Connect (a favorite interview deep-dive) 🟡

1. Commercial CDNs struggled to expand fast enough for Netflix's growth.
2. Cost of buying CDN capacity at Netflix's scale became larger than building it.
3. Video is Netflix's core product — protecting it and controlling delivery quality is existential, not incidental.
4. Netflix wanted end-to-end control: the player, the network path, and the server — impossible when renting from a third party.
5. Custom HTTP + TCP stack on OCA lets Netflix detect and troubleshoot network issues directly.
6. Netflix wanted to cache popular titles for a long time — cost-prohibitive on a public CDN at their retention/volume needs.

**How Open Connect actually works:** **OCAs** (Open Connect Appliances — Netflix's own edge caching boxes) are placed inside ISPs / at IXPs. They **do not store user data** — they (a) report health, learned routes, and cached-content info to a control plane hosted in AWS, and (b) serve cached video to users. Netflix achieves a **~95% cache hit ratio** this way. Public CDNs remain a fallback when OCA capacity is insufficient or during failures — this is a hybrid architecture, not all-or-nothing.

**Interview cheat-sheet:** buy when delivery isn't your differentiator; build when (a) delivery IS the product, (b) traffic is astronomically large and predictable, (c) you need control over data protection/licensing (Netflix's content contracts require strict leak prevention), or (d) commercial CDN costs at your scale exceed the capex+opex of building.

---

## 14. API design (how to talk through the interfaces) 🟡

**TL;DR:** Six operations, each owned by a specific pair of components — knowing which pair "talks" via which call is what turns a vague "the proxy asks the origin for stuff" into a concrete API surface.

If asked to sketch **APIs** (Application Programming Interfaces — the callable operations a system exposes) for a CDN, six core operations map cleanly onto components:

```
retrieveContent(proxyserver_id, content_type, content_version, description)   // proxy → origin
deliverContent(origin_id, server_list, content_type, content_version, description)  // origin → proxies (push)
requestContent(user_id, content_type, description)                            // client → proxy
searchContent(proxyserver_id, content_type, description)                      // proxy → peer proxies in same PoP
updateContent(proxyserver_id, content_type, description)                      // proxy → peer proxies (e.g., after edge script mutation)
// deleteContent — reuses standard cache eviction policy (LRU/LFU/TTL), not a bespoke API
```

**The same five calls, as who-talks-to-whom-and-when — this is the picture to hold in your head before you write any signature:**

```mermaid
sequenceDiagram
    participant U as User (client)
    participant E as Proxy (edge)
    participant Pe as Peer proxy (same PoP)
    participant O as Origin

    U->>E: requestContent(user_id, content_type)
    alt Cache hit
        E->>U: Serve directly
    else Cache miss, check peers first
        E->>Pe: searchContent(proxyserver_id, content_type)
        alt Peer has it
            Pe->>E: Return content
        else No peer has it
            E->>O: retrieveContent(proxyserver_id, content_type, content_version)
            O->>E: Return content
        end
        E->>U: Serve + cache locally
    end
    Note over O,E: Independently, on publish/change:
    O->>E: deliverContent(origin_id, server_list, content_type, content_version)
    Note over E,Pe: After an edge script mutates content:
    E->>Pe: updateContent(proxyserver_id, content_type)
```

Note the **searchContent** design trade-off: flooding the query to every peer proxy in a PoP is simple but wasteful; maintaining a shared PoP-local index (a small distributed metadata store of "what's cached where") avoids the flood at the cost of extra consistency bookkeeping. This is a nice micro system-design-within-a-design-question to discuss if pushed.

---

## 15. Security: scrubber servers and DDoS 🟡

**TL;DR:** Scrubbers only sit in the request path during a detected attack, and anycast disperses attack traffic across every PoP advertising that IP as a free side-effect of routing — neither costs anything on a normal request.

- Scrubber servers sit in the request path and are activated specifically **when an attack is detected** — traffic gets scrubbed/cleaned, then forwarded to the real edge proxy. This is exactly the pattern behind **Cloudflare's DDoS mitigation** and **AWS Shield**.
- **Anycast is itself a DDoS defense**: attack traffic naturally disperses across every edge server advertising the same IP (BGP spreads the load), instead of concentrating on one target.
- Heartbeat/health-checks let the routing system silently route around a proxy that's unhealthy or under attack.
- Because a CDN masks the origin's real IP, it also reduces the attack surface directly exposed to the internet — origin-shielding is a security feature, not just a performance one.

**The same three bullets, as a decision path — this is what actually happens to a request, attack or no attack:**

```mermaid
flowchart TD
    R[Request arrives at the CDN's<br/>anycast IP] --> Detect{Attack signature<br/>detected on this traffic?}
    Detect -->|No — normal traffic| Direct[Straight to edge proxy<br/>zero extra latency]
    Detect -->|Yes| Scrub[Routed through scrubber servers<br/>malicious traffic filtered out]
    Scrub --> Clean[Cleaned traffic forwarded<br/>to edge proxy]
    Direct --> Edge[Edge proxy serves<br/>from cache or escalates on miss]
    Clean --> Edge

    style Direct fill:#d4f4dd,stroke:#2f9e44,color:#1a3d1f
    style Scrub fill:#ffe0e0,stroke:#e03131,color:#3d1a1a
```

The point of drawing it this way: scrubbing is a *conditional* branch, not a permanent stage every request pays for — say that explicitly if asked "doesn't filtering every request add latency?"

---

## 16. Evaluating the design against non-functional requirements 🟢

**TL;DR:** Walk the same four non-functional requirements from §4 back through the finished design — each one maps to a specific, nameable mechanism, not a vague "the CDN handles that."

| Requirement | How the design satisfies it |
|---|---|
| **Performance** | RAM-first serving at proxies; proxies placed physically near users/inside ISPs; long-tail content on SSD/HDD (still far faster than a WAN round trip to origin); layered proxy hierarchy avoids one giant fan-out |
| **Availability** | Cached content survives origin outages; unhealthy proxies are routed around; redundant replicas across proxies remove single points of failure; load balancers spread load among healthy proxies |
| **Scalability** | Horizontal scale-out by adding edge proxies (read replicas, essentially); multi-tier hierarchy absorbs storage/fan-out limits of any single proxy |
| **Reliability & security** | No SPOF via redundancy + maintenance rotation; scrubber servers + heartbeat health checks; specialized/private CDNs for content-leakage-sensitive businesses |

---

## 17. How this shows up in a FAANG interview 🟡

**TL;DR:** Recognize the signal phrases that mean "the interviewer wants CDN knowledge," open with the three pillars, and don't skip cache invalidation or security just because they weren't asked about directly.

### 17.1 Interview playbook — how to open and structure the discussion 🟡

```mermaid
flowchart TD
    A[Interviewer describes the system] --> B{Any signal phrase?<br/>global users / static+video assets /<br/>protect origin from load / traffic spike}
    B -->|No| Z[CDN probably isn't the focus —<br/>mention briefly if relevant, move on]
    B -->|Yes| C[Open with the 3-pillar definition:<br/>caching + routing + security]
    C --> D[State push vs. pull per content type<br/>not CDN-wide]
    D --> E[Explain routing:<br/>DNS redirection vs. anycast]
    E --> F{Content includes<br/>dynamic/personalized data?}
    F -->|Yes| G[Cover edge compute / ESI / DASH —<br/>the 'not just static' differentiator]
    F -->|No| H[Stick to push + long TTL,<br/>say so explicitly]
    G --> I[Bring up cache invalidation<br/>unprompted: TTL vs. lease]
    H --> I
    I --> J{Interviewer probes buy vs. build,<br/>or scale is extreme?}
    J -->|Yes| K[Discuss buy vs. build trade-off<br/>Netflix Open Connect as reference]
    J -->|No| L[Skip — assume public CDN]
    K --> M[Close with security:<br/>scrubbers, anycast's DDoS side-benefit]
    L --> M
    M --> N[Validate against non-functional reqs:<br/>performance, availability, scalability, security]
```

**Signal phrases that mean "the interviewer wants CDN knowledge":**
- "Users are global / worldwide" + "minimize latency"
- "Serve static assets / images / video" at scale
- "Protect the origin from being overwhelmed"
- Any **video streaming** system (YouTube, Netflix, Twitch) — always triggers a CDN + adaptive bitrate discussion
- Any **social feed with images** system (Instagram, Facebook, Twitter) — triggers CDN for media, not for the feed itself
- "How would you reduce load on your database/origin during a traffic spike" — CDN + cache is a valid partial answer even outside a "design a CDN" question

**What a strong candidate does that a mediocre one doesn't:**
1. Distinguishes **push vs. pull** and picks per content type, not CDN-wide.
2. Explains routing (**DNS redirection vs anycast**) instead of hand-waving "it finds the nearest server."
3. Brings up **cache invalidation** (TTL vs. lease) unprompted — this is the classic hard-caching-problem callback.
4. Knows CDNs help with **dynamic** content too (edge scripting, ESI), not just static files — this is the biggest differentiator.
5. Can articulate the **buy vs. build** trade-off with a real example (Netflix Open Connect) instead of assuming everyone just uses Cloudflare/Akamai.
6. Mentions **security** (scrubber servers, DDoS, anycast's side-benefit) without being asked.

**Common follow-up traps:**
- *"What if the nearest proxy doesn't have the content?"* → escalate to parent proxy in the tier, then to origin; discuss cold-start / cache-miss storm risk on a brand-new popular object (thundering herd — mention **request coalescing** at the proxy, or an **origin shield** (§9.2), so N simultaneous misses for the same key become 1 origin fetch).
- *"How do you invalidate a cached object across thousands of edge proxies instantly?"* → there's no free lunch: either short TTL (accept some staleness), a purge API that fans out invalidation messages (used by real CDNs for "purge by URL/tag," e.g. Fastly's sub-second purge), or versioned URLs (cache-bust by changing the URL itself — most scalable, since the old URL's TTL is irrelevant once nobody links to it).
- *"How would you handle a viral/hot object overwhelming one proxy?"* → replicate that specific object to more proxies dynamically (popularity-aware replication), or route requests for it across a wider anycast/load-balanced set.

### What this actually sounds like out loud (worked sample answer) 🟡

The playbook above is a map of *when* to bring up each idea; here's an actual ~90-second answer to a common prompt, so you have a model to imitate rather than just a checklist.

> **Prompt: "How would you speed up image and video delivery for a globally distributed photo-sharing app?"**
>
> "I'd put a CDN in front of the media storage, and I'd frame it around three jobs, not just 'caching' — caching, routing, and shielding the origin.
>
> For caching: images and video segments are close to write-once, read-many, so this is a push-friendly workload — I'd push newly uploaded, popularity-predicted-high content out to edge PoPs proactively, with a long TTL since the bytes themselves never change, and pair that with versioned URLs so a re-upload or edit is a new URL, not an invalidation problem. Anything long-tail or unpredictable just falls back to pull-on-first-request, which is the CDN's default behavior anyway.
>
> For routing, I'd lean on anycast — one IP, BGP routes each user to their nearest PoP, no extra DNS round trip, and it doubles as free DDoS resilience since attack traffic disperses across every PoP advertising that IP. If I needed finer load-aware control per request I'd reach for DNS redirection with short TTLs instead, but for media delivery I don't think I need that level of control.
>
> For shielding the origin: I'd put an origin shield tier between every edge PoP and the actual media store, so if a photo suddenly goes viral, a hundred edge PoPs missing on it at once collapses into one fetch from the shield, and the shield itself coalesces concurrent misses into a single origin fetch — that's the thundering-herd fix, and it's two distinct mechanisms stacked, not one.
>
> Video specifically, I'd serve as DASH or HLS — a manifest listing multiple bitrates, with the client adapting per its measured bandwidth — rather than caching one fixed-quality file, since that's the actual production pattern at Netflix/YouTube scale.
>
> One thing I'd flag unprompted: if a photo gets deleted or reported and needs to come down immediately, that's not a TTL problem — I'd need an actual purge API fanned out through a pub/sub channel to every edge, because waiting on TTL expiry isn't fast enough for a takedown."

Notice the shape: name all three pillars explicitly up front, pick push vs. pull *by content type* rather than for the whole system, name the specific routing mechanism and why, name the thundering-herd fix as two stacked mechanisms rather than one, and flag the purge/takedown case unprompted at the end.

---

## 18. Putting it all together — v1 through v4 🟢🟡🔴

**TL;DR:** Every mechanism in this guide exists because a simpler version broke — here's the whole progression in one place, v1 through v4, now that everything it depends on (routing, push/pull, dynamic content, shielding, security) has actually been introduced, plus every mistake this guide has flagged, gathered into one table.

Interviewers often want you to *build up* the design rather than present the finished architecture cold. Narrating it as a progression of versions — each one only adding the complexity the previous version couldn't handle — is more convincing than jumping straight to the final picture. This is also the "if you only remember one picture" payoff for the whole guide: every earlier section is *why* one of these arrows exists.

**v1 — naive: one origin, no CDN at all.**
```mermaid
graph LR
    U1[User - US] --> O[(Single Origin<br/>us-east-1)]
    U2[User - Europe] --> O
    U3[User - Asia] --> O
```
Every user, everywhere, pays the full WAN RTT to one data center (§2's "without a CDN" row). Works for a small user base, falls over at global scale.

**v2 — a handful of regional edge caches + DNS routing.**
```mermaid
graph LR
    U1[User - US] --> D{DNS routing}
    U2[User - Europe] --> D
    U3[User - Asia] --> D
    D -->|nearest region| E1[Edge Cache<br/>US]
    D -->|nearest region| E2[Edge Cache<br/>EU]
    D -->|nearest region| E3[Edge Cache<br/>Asia]
    E1 -->|miss| O[(Origin)]
    E2 -->|miss| O
    E3 -->|miss| O
```
Most requests now get answered close to the user, and DNS decides which region to send them to (§10). But each of these 3 edge caches independently hits origin on a miss — fine at 3 PoPs, but this doesn't survive a jump to hundreds of PoPs (§3): origin would see hundreds of independent "first miss" spikes instead of one.

**v3 — anycast + origin shield + pub/sub purge.**
```mermaid
graph TB
    U1[Users worldwide] -->|1 anycast IP| BGP{BGP routes to<br/>nearest PoP}
    BGP --> E1[Edge PoP 1]
    BGP --> E2[Edge PoP 2]
    BGP --> E3[Edge PoP ...N]
    E1 -->|miss| SH[Origin Shield]
    E2 -->|miss| SH
    E3 -->|miss| SH
    SH -->|shield miss| O[(Origin)]
    Pub[Purge pub/sub channel] -.fan-out invalidation.-> E1
    Pub -.-> E2
    Pub -.-> E3
```
At hundreds-to-thousands of PoPs, three things become necessary that v2 didn't need: **(1) anycast** (§10), so clients skip the two-step DNS resolution and BGP does the nearest-PoP math for free; **(2) an origin shield** (§9.2), so N edges missing on the same object collapses into one origin fetch instead of N; **(3) a pub/sub invalidation channel** (§11.5), so a purge reaches every PoP in roughly the same few seconds whether there are 3 PoPs or 3,000.

**Memory hook:** each version fixes exactly one gap the previous one exposed — v1→v2 gets you off a single origin and onto DNS-routed regions; v2→v3 makes both routing (anycast) and invalidation (pub/sub) scale to hundreds/thousands of PoPs instead of a handful. Say the progression out loud in an interview — it demonstrates you know *why* each piece exists, not just that it exists.

### v4 — a real production CDN: security, dynamic content, and business logic layered on top 🟢🟡🔴

v3 gets the *distribution and invalidation* shape right at scale, but it's silent on everything §8, §9.3, and §15 covered: what happens to a request that isn't a simple static-file GET, and what happens when traffic is actually hostile. v4 doesn't replace anything in v3 — it wraps it:

```mermaid
graph TB
    U1[Users worldwide] -->|1 anycast IP| BGP{BGP routes to<br/>nearest PoP}
    BGP --> Detect{Attack signature<br/>detected? §15}
    Detect -->|Yes| Scrub[Scrubber servers<br/>clean the traffic]
    Detect -->|No| E1
    Scrub --> E1[Edge PoP]

    E1 --> Static{Fully static<br/>content? §8}
    Static -->|Yes, cache hit| ServeHit[Serve from RAM/SSD<br/>request coalesced if a miss-storm §9.3]
    Static -->|No — dynamic/personalized| EdgeCompute[Edge script/serverless §8<br/>auth, geo, header rewriting]
    EdgeCompute --> ServeHit

    Static -->|Cache miss| SH[Origin Shield §9.2]
    SH -->|shield miss, coalesced §9.3| O[(Origin)]
    O -->|push, known-popular content §7| Dist[Distribution system]
    Dist --> E1

    Pub[Purge pub/sub channel §11.5] -.fan-out invalidation.-> E1

    style ServeHit fill:#d4f4dd,stroke:#2f9e44,color:#1a3d1f
    style Scrub fill:#ffe0e0,stroke:#e03131,color:#3d1a1a
```

**Why this is v4, not "v3 with extra boxes":** every mechanism from v1–v3 still does exactly the job it always did — anycast still routes, the shield still coalesces, pub/sub still purges. v4 adds the two axes v3 never touched: **what kind of request is this** (static → cache; dynamic → edge compute; §8) and **is this traffic hostile** (§15) — plus the reminder that content arrives at the edge by *both* push (known-popular, §7) and pull (everything else) simultaneously, not one or the other CDN-wide.

**The one-sentence version of the whole guide, if you need to say it in ten seconds:** *"A request hits an anycast IP, gets silently scrubbed if it's part of an attack, is served straight from an edge cache if it's a static-content hit (with request coalescing protecting against a miss-storm), runs at the edge if it's dynamic, escalates through an origin shield on a genuine miss, and any of it can be evicted instantly, everywhere, through a pub/sub purge — that's caching, routing, and security, working together, at every single step."*

### Common mistakes, consolidated

Individual gotchas are called out inline throughout this guide; here they are in one place, because they're also the fastest way to lose credibility if you say the wrong side of them out loud:

| Mistake | Why it's wrong | Say instead |
|---|---|---|
| "CDN = static content only" | Dynamic acceleration (edge compute, ESI, DASH) is a first-class use case, not an edge case (§8) | "Ask how *little* of a response needs the origin, not whether the whole thing can be cached" |
| "The scrubber sits in every request's path" | It's a conditional branch, only active during a detected attack (§6, §15) | "Scrubbing adds zero latency to normal traffic — it's activated per-attack, not per-request" |
| "'Nearest' means geographically closest" | Real routing uses network distance (path × bandwidth) + live load (§10) | "Nearest = lowest network distance and load, never plain geography" |
| "Lowering the TTL invalidates what's already cached" | TTL only affects *future* cache-fill decisions; already-cached copies keep serving until their existing TTL expires (§11.3) | "TTL expiry and purge are different mechanisms — for an urgent fix, purge; TTL changes don't retroactively evict anything" |
| "Request coalescing and an origin shield solve the same problem" | Coalescing is single-PoP scope; a shield is cross-PoP scope — they stack, they don't substitute (§9.2, §9.3) | "Coalescing kills the herd within a PoP; the shield kills it across PoPs — name both" |
| "Purge fans out by looping over every edge one at a time" | That's O(PoPs) sequential round trips — doesn't scale past a few dozen edges (§11.5) | "Publish once to a pub/sub channel, fan out through a tree in parallel — O(log PoPs)" |
| "Push CDN is just 'the better' option" | It's a content-type decision, not a CDN-wide one — push suits predictable/static, pull suits dynamic/long-tail (§7) | "Most real systems use both, split by content type, not by picking one model for everything" |
| "Buy is always cheaper than build" | Only true below the scale where a private CDN's capex pays for itself (§13) | "Buy unless delivery is the core product *and* traffic is massive and predictable enough to justify the capex" |
| "A CDN replaces the origin" | The origin still exists, still gets hit on every miss, and still needs its own resilience story (Golden Rule 8) | "A CDN shields the origin, it doesn't retire it" |

---

## 19. Active Recall — Test Yourself 🟢🟡🔴

**TL;DR:** Answer these cold, from a blank page, before checking each answer — recognizing an explanation and reproducing it unprompted are different skills, and only the second one holds up in an interview.

Reading a diagram and being able to redraw it from a blank page are different skills. Cover the guide and answer these; expand each to check. This is what actually makes the material stick.

> **On "never forget": do this more than once.** One pass through these questions tells you whether you understood the material *today* — it doesn't mean you'll still have it in three weeks. That takes spaced repetition, not one good read. Concretely: answer all 16 now; three days from now, answer only the ones you got wrong or hesitated on; a week after that, do the same again. If you want it outside this doc, these 16 Q&As convert directly into flashcards (front = the bolded question, back = the answer) for a spaced-repetition tool like Anki.

<details>
<summary>1. What are the three pillars of a CDN? (Not "caching" alone.)</summary>
Caching, routing, and security/traffic-shielding. Miss one and it's just "a cache," not a CDN.
</details>

<details>
<summary>2. Name the one idea that shows up in both §7 (push/pull CDN) and §11 (TTL/lease). Where else does it show up outside this chapter?</summary>
Push = proactive, someone ships before being asked. Pull = reactive, fetch on demand. Outside this chapter: fan-out-on-write vs. fan-out-on-read in feed systems (Twitter/Instagram).
</details>

<details>
<summary>3. A proxy misses on a request. Trace the fallback path, in order.</summary>
Edge proxy → (origin shield, if present) → parent (tier-1) proxy → origin server. Each hop only happens if the previous one also misses.
</details>

<details>
<summary>4. Why is "nearest" not the same as "geographically closest"?</summary>
Nearest = shortest network path × available bandwidth, plus current request load. A geographically close proxy on a congested link can be "farther" than a distant one on a fat, unloaded pipe.
</details>

<details>
<summary>5. Why does anycast double as a DDoS defense, without anyone designing it that way on purpose?</summary>
BGP naturally spreads traffic to the topologically nearest of many servers sharing one IP — so attack traffic gets dispersed across all of them instead of concentrating on a single target.
</details>

<details>
<summary>6. Give one concrete reason Netflix built Open Connect instead of buying CDN capacity, and name the one thing Netflix's OCA servers explicitly do NOT store.</summary>
Reason: e.g. video is Netflix's core revenue source, so protecting it and controlling delivery quality end-to-end was worth the capex (any one of the six listed reasons works). OCAs do not store user data — only cached content + health/route reporting.
</details>

<details>
<summary>7. Why would you choose a lease over a plain TTL, and what's the cost of that choice?</summary>
Choose a lease when you need near-real-time propagation of changes and writes are infrequent — it minimizes wasted messages versus polling. Cost: more bookkeeping, since the origin must track per-proxy lease state.
</details>

<details>
<summary>8. What's the fastest way to invalidate one specific object across thousands of edge proxies "instantly," and why is it the most scalable option?</summary>
Versioned/cache-busted URLs (change the URL, not the cached object). It's the most scalable because you never have to touch the old cached copies at all — their TTL becomes irrelevant the moment nothing links to that URL anymore.
</details>

<details>
<summary>9. A single object suddenly goes viral and is overwhelming one proxy. What do you do — and is this the same fix as a normal load-balancing problem?</summary>
Dynamically re-replicate that specific hot object to more proxies (popularity-aware replication), or spread requests for it across a wider anycast/load-balanced set. Not quite the same as generic load balancing — it's content-aware, targeting one key, not just spreading connections evenly.
</details>

<details>
<summary>10. Redraw the full request lifecycle diagram (§6.1) from memory: which 7 components does a request pass through, in order, on a cache miss?</summary>
User → DNS/routing system → (scrubber, only if under attack) → edge proxy (miss) → parent/tier-1 proxy (miss) → origin → back down through parent → edge proxy → user, with accounting reported to the management system, which feeds back into routing.
</details>

<details>
<summary>11. What's the difference between cache invalidation and TTL expiry, and which one is "the hard problem"?</summary>
TTL expiry is passive and time-based — the proxy just checks the clock. Cache invalidation (purge) is active and operator-initiated — "purge this now," regardless of remaining TTL. Invalidation is "the hard problem" because it requires reliably reaching every edge that might hold a stale copy, fast, without a free/automatic clock to fall back on. Lowering the TTL does NOT retroactively invalidate what's already cached — it only changes how long the *next* fetch will be trusted for.
</details>

<details>
<summary>12. A cached object is "stale." What are its two possible next states, and what causes each?</summary>
Revalidated (back to Fresh) if a conditional GET returns 304 Not Modified; Expired/Purged if revalidation returns new content, or an explicit purge is issued. Stale-while-revalidate lets the proxy serve the stale copy immediately while this check happens in the background.
</details>

<details>
<summary>13. Request coalescing and an origin shield both stop origin overload on a cold cache — don't they solve the same problem?</summary>
No. Request coalescing handles many concurrent misses for the *same key at the same edge* — the first request becomes the leader and fetches, the rest wait, one fetch total per PoP. An origin shield handles many concurrent misses for the *same key across many different edges* — it collapses all of their fetches into one. They stack: coalescing kills the herd within a PoP, the shield kills the herd across PoPs.
</details>

<details>
<summary>14. How does a purge reach thousands of edge PoPs without the control plane looping over them one at a time?</summary>
Publish the invalidation once to a pub/sub channel; a fan-out tree (regional aggregators → edge PoPs) delivers and applies it in parallel. That's roughly O(log PoPs) parallel hops instead of O(PoPs) sequential round trips — the same shape as the distribution system's tree fan-out for content (§9.1), just applied to invalidation.
</details>

<details>
<summary>15. A candidate says "this content is personalized, so the CDN can't help here." What's wrong with that framing, and what should they say instead?</summary>
It conflates "can't cache the bytes" with "the CDN adds no value." Even fully personalized responses can run auth checks, TLS termination, geo-based header rewriting, and A/B-bucket assignment at the edge via edge compute — only the truly personal computation (or nothing) needs to reach origin. The right question is "how little of this needs the origin," not "can I cache the whole thing."
</details>

<details>
<summary>16. Redraw the v4 diagram (§18) from memory: name the four things a single request has to pass through or past, in order, before it's served.</summary>
(1) Anycast/BGP routing to the nearest PoP, (2) a conditional scrubber check (only active if an attack is detected), (3) a static-vs-dynamic branch (cache hit/coalesced-fetch for static, edge compute for dynamic), and (4) on a genuine cache miss, escalation through the origin shield (itself coalescing across PoPs) to origin — with a pub/sub purge channel able to evict any of it, at any point, independent of that flow.
</details>

---

## 20. Golden Rules 🟢

**TL;DR:** Nine non-negotiables — if any one of these is missing from your answer, the interviewer will probe until it surfaces.

Non-negotiables — if any of these is missing from your answer, the interviewer will probe until it surfaces.

1. Always name all three CDN pillars — caching, routing, security — never just "cache."
2. Never say "CDN = static content only" — dynamic acceleration (edge compute, ESI, DASH) is a first-class use case, not an edge case.
3. Cache invalidation is the hard problem, not caching itself — bring it up unprompted.
4. "Nearest" means lowest network distance + load, never plain geographic distance.
5. Push and pull are picked per content type, not CDN-wide — and the same duality reappears as TTL vs. lease.
6. Anycast isn't just routing — it's also a free DDoS defense; mention both when it comes up.
7. Buy vs. build is a real trade-off, not a foregone conclusion — cite Netflix Open Connect, not just "everyone uses Cloudflare."
8. A CDN shields the origin, it doesn't replace it — the origin still exists, still gets hit on misses, and still needs its own resilience story.
9. Thundering herd has two distinct fixes at two distinct scopes — request coalescing (same key, same edge) and an origin shield (same key, many edges) — know both, and know they stack rather than substitute for each other.

---

## Master Cheat Sheet 🟢

**Definition:** CDN = distributed proxy/cache layer at the network edge + intelligent request routing + traffic shielding, sitting between clients and an origin. It is not "just a reverse proxy" — it's the reverse-proxy pattern applied at global, geographically-distributed scale.

**Three pillars:** caching, routing, security. Say all three.

**Numbers to know:**
- Latency budgets: VoIP < 150ms, interactive < 200ms, video streaming = seconds (buffered) OK.
- US East↔West RTT ≈ 63ms; US↔Africa RTT ≈ 226ms.
- Netflix + YouTube + Prime Video ≈ 80% of internet traffic.
- Akamai historically: 15–30% of web traffic, ~30 Tbps, one hop from 90% of users.
- Netflix Open Connect: ~95% cache hit ratio.
- Capacity math (500M DAU, 20 req/user/day, 2KB objects, 95% hit ratio): ~116K avg / ~350K peak QPS at the edge → only ~5.8K–17.5K QPS reaches origin (>20x reduction); ~20TB/day total, but only ~1TB/day actually hits origin egress.

**Push vs Pull:** push = static, proactive, higher replicas/availability, higher storage cost. Pull = dynamic, reactive, lower storage cost, origin hit on miss/expiry. Real systems use both.

**Routing:** DNS redirection (two-step, short TTL, considers distance+load — Akamai's approach) > Anycast (one IP, BGP-routed, also a DDoS defense) > client multiplexing / HTTP redirect (simple but naive). "Nearest" = network distance + load, never plain geography.

**Consistency:** polling (TTR, wasteful) → TTL (expiry-based, standard) → leases (origin pushes notifications, fewest messages, adaptive). Stale-while-revalidate serves stale instantly while refreshing async in the background. Invalidation (active purge) ≠ TTL expiry (passive clock) — lowering TTL doesn't retroactively evict what's already cached. Short TTL = fresh but costly; long TTL = cheap but stale — versioned URLs get both.

**Placement:** on-premises (near IXPs) vs off-premises (inside ISP, Akamai/Netflix style, "one hop away"). Google uses split-TCP at IXP-level infra to avoid slow-start/handshake to distant origin. Origin shield = single consolidated tier between edge and origin that coalesces thundering-herd misses into one origin fetch (CloudFront: Origin Shield; Fastly: shielding).

**Thundering herd, two scopes:** request coalescing = one edge, one key, first request leads, rest wait (single-flight). Origin shield = many edges, one key, shield collapses all their fetches into one. They stack, they don't substitute.

**Purge propagation:** never loop over edges one at a time (O(PoPs) sequential) — publish once to a pub/sub channel, fan out through a tree of regional aggregators to every edge in parallel (O(log PoPs)). Same tree-fan-out shape as content distribution (§9.1), applied to invalidation instead.

**Architecture evolution (how to narrate a build-up — full version in §18):** v1 single origin → v2 a few regional edge caches + DNS routing → v3 anycast + origin shield + pub/sub purge, once PoP count grows from a handful to hundreds/thousands → v4 wraps v3 with the request-type branch (static cache vs. dynamic edge compute) and a conditional security scrubber — every earlier section is *why* one arrow in that final diagram exists.

**Buy vs build:** buy (Akamai/Cloudflare/Fastly/CloudFront) unless delivery is your core product at massive, predictable scale and you need full control (Netflix Open Connect). CloudFront = Lambda@Edge/CloudFront Functions + regional edge caches. Fastly = instant (sub-second) purge via VCL + Compute@Edge (Wasm-based edge compute).

**Dynamic content:** edge scripting/serverless at the proxy, compression (Railgun), ESI (partial page caching), DASH/HLS + byte-range for adaptive video. "Dynamic" doesn't mean "no CDN benefit" — ask how little of the response needs origin, not whether the whole thing is cacheable.

**Components to name in order:** clients → routing system → scrubber servers → edge proxies → distribution system → origin servers → management system.

**Common mistakes (full table in §18):** CDN=static-only myth; scrubber-in-every-request myth; nearest=geography myth; TTL-lowering-invalidates-existing-copies myth; coalescing-and-shield-are-redundant myth; purge-by-looping-over-edges; push-is-just-better myth; buy-is-always-cheaper myth; CDN-replaces-origin myth.

**Failure/edge-case answers:** hierarchical fallback (edge → parent/shield → origin) for misses; request coalescing for thundering herd; versioned URLs or purge APIs for instant invalidation; popularity-aware re-replication for hot-object overload; anycast + scrubbers for DDoS.
