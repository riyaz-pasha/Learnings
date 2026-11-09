# Course: Consistent Hashing in Multi-Region Distributed Systems

We'll build your understanding from the fundamentals of consistent hashing to designing a production-ready, multi-region architecture that handles low latency, replication, failover, data consistency, GDPR, and data residency.

The goal is not just to memorize concepts. You'll learn to reason about architectural decisions, understand the internals, and explain them confidently in system design interviews.

## Course roadmap

### Chapter 1 — Foundations of Consistent Hashing

- Why distributed systems need partitioning and sharding.
- The limitations of `hash(key) % N`.
- How consistent hashing works internally.
- Hash rings, virtual nodes, key placement, and node removal.
- Why consistent hashing minimizes remapping.
- Consistent hashing versus rendezvous hashing and other approaches.

### Chapter 2 — Implementing Consistent Hashing

- Build a hash ring step by step.
- Use virtual nodes and weighted distribution.
- Add and remove nodes safely.
- Handle hash collisions and uneven key distribution.
- Understand production considerations and common implementation mistakes.

### Chapter 3 — From a Single Region to Multiple Regions

- Single-region architecture versus multi-region architecture.
- Global hash rings versus regional hash rings.
- Region selection versus node selection.
- Home-region ownership and tenant-based routing.
- How DNS, global load balancers, and application routing fit together.

### Chapter 4 — Low-Latency Routing

- Geographic proximity versus actual network latency.
- Route users to nearby eligible regions.
- Local caches, regional databases, and cross-region requests.
- Hot keys, cache stampedes, and uneven regional traffic.
- Latency budgets, tail latency, and load-aware routing.

### Chapter 5 — Data Ownership, Replication, and Consistency

- Primary databases, read replicas, and multi-primary databases.
- Synchronous versus asynchronous replication.
- Eventual consistency, strong consistency, and read-your-writes.
- Write ownership and concurrent updates.
- Replication lag, conflict resolution, and stale reads.
- Why consistent hashing does not provide replication guarantees.

### Chapter 6 — GDPR and Data Residency

- GDPR versus strict data residency requirements.
- EU-only, India-only, and other region-restricted tenants.
- Tenant registries and policy-driven routing.
- Regional caches, logs, backups, encryption keys, and observability.
- Cross-border transfers and administrative access.
- How to enforce residency at multiple architectural layers.

### Chapter 7 — Regional Failover and Disaster Recovery

- Availability zones versus geographic regions.
- Regional outages and network partitions.
- Failover, failback, and data recovery.
- Split-brain prevention, fencing, and write ownership.
- RPO, RTO, replication lag, and recovery testing.
- What happens when every legally permitted region is unavailable?

### Chapter 8 — Advanced Multi-Region Architecture

- Tenant-based versus entity-based partitioning.
- Regional hash rings and global routing.
- Rendezvous hashing, virtual nodes, and weighted placement.
- Rebalancing with minimal disruption.
- Multi-region caches and distributed coordination.
- Handling users who travel or whose residency requirements change.

### Chapter 9 — Production Implementation on AWS

- Route 53, AWS Global Accelerator, regional application deployments, and AWS Regions.
- ElastiCache/Redis, PostgreSQL, RDS, and DynamoDB design choices.
- Infrastructure as code and policy enforcement.
- Metrics, tracing, replication monitoring, and alerting.
- Security, operational runbooks, and failure testing.

### Chapter 10 — End-to-End System Design

We'll design a multi-tenant SaaS platform from scratch.

It will include:

- Global request routing.
- Residency-aware tenant placement.
- Regional consistent-hashing rings.
- Database replication and consistency guarantees.
- Regional failover and disaster recovery.
- GDPR-related controls.
- Architecture diagrams, trade-offs, failure scenarios, and interview questions.

## How we'll learn

Each chapter will follow a consistent format:

1. Problem: What goes wrong without the concept?
2. Intuition: Build a mental model before learning terminology.
3. How it works: Step-by-step internals, with Mermaid diagrams.
4. Implementation: Practical examples and configuration where appropriate.
5. Trade-offs: When to use an approach and when to avoid it.
6. Production scenarios: Failures, edge cases, and operational concerns.
7. Interview preparation: Questions and strong, clear answers.

I'll explain one chapter at a time. You can type `next` whenever you're ready to continue.

## Chapter 1 — Foundations of Consistent Hashing

We'll start with a fundamental question:

Why can't we simply hash a key and use modulo to distribute data across servers?

Understanding this problem will make the need for consistent hashing feel natural rather than something you have to memorize.

Say your application has three cache servers:

```
             Application
                  |
          Hash the cache key
                  |
             hash(key) % 3
            /     |     \
       Server A Server B Server C
```

Suppose the application stores these keys:

| Key            | Hash value | Calculation | Server |
| -------------- | ---------- | ----------- | ------ |
| `customer:101` | 17         | 17 % 3 = 2  | C      |
| `customer:102` | 22         | 22 % 3 = 1  | B      |
| `customer:103` | 35         | 35 % 3 = 2  | C      |
| `customer:104` | 41         | 41 % 3 = 2  | C      |
| `customer:105` | 18         | 18 % 3 = 0  | A      |

These hash values are illustrative; real hash functions produce values according to their own algorithms.

Initially, this works perfectly well. Every application instance uses the same rule, so the same key maps to the same server.

### The problem: what happens when a server is added?

Suppose your application grows, and you add Server D.

Your calculation changes from:

```
int serverIndex = hash(key) % 3;
```

to:

```
int serverIndex = hash(key) % 4;
```

Let's calculate again:

| Key            | Hash value | With 3 servers | With 4 servers |
| -------------- | ---------- | -------------- | -------------- |
| `customer:101` | 17         | C              | B              |
| `customer:102` | 22         | B              | C              |
| `customer:103` | 35         | C              | D              |
| `customer:104` | 41         | C              | B              |
| `customer:105` | 18         | A              | C              |

Notice what happened: all five keys moved to different servers.

With a large number of keys, adding just one server can invalidate most of your cache placement.

For a cache, this means many cache misses. For a distributed datastore, blindly changing the formula can cause routing failures unless data is migrated and ownership is updated correctly.

The same issue occurs when a server is removed.

### Why does this matter in a real application?

Imagine that your application has 100 million cached entries.

You add another server to handle increasing traffic.

With modulo-based partitioning, a large proportion of entries may suddenly map to different servers. Requests start missing the cache, so the database receives a surge of traffic at exactly the time you're trying to improve capacity.

This can create a cascading failure:

\#chatgpt-mermaid-\_r_3bm\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3bm\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3bm\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3bm\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3bm\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3bm\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3bm\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3bm\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3bm\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3bm\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3bm\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3bm\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3bm\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3bm\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3bm\_ p{margin:0;}#chatgpt-mermaid-\_r_3bm\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3bm\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3bm\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3bm\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3bm\_ .label text,#chatgpt-mermaid-\_r_3bm\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3bm\_ .node rect,#chatgpt-mermaid-\_r_3bm\_ .node circle,#chatgpt-mermaid-\_r_3bm\_ .node ellipse,#chatgpt-mermaid-\_r_3bm\_ .node polygon,#chatgpt-mermaid-\_r_3bm\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3bm\_ .rough-node .label text,#chatgpt-mermaid-\_r_3bm\_ .node .label text,#chatgpt-mermaid-\_r_3bm\_ .image-shape .label,#chatgpt-mermaid-\_r_3bm\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3bm\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3bm\_ .rough-node .label,#chatgpt-mermaid-\_r_3bm\_ .node .label,#chatgpt-mermaid-\_r_3bm\_ .image-shape .label,#chatgpt-mermaid-\_r_3bm\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3bm\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3bm\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3bm\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3bm\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3bm\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3bm\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3bm\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3bm\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3bm\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3bm\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3bm\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3bm\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3bm\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3bm\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3bm\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3bm\_ .icon-shape,#chatgpt-mermaid-\_r_3bm\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3bm\_ .icon-shape p,#chatgpt-mermaid-\_r_3bm\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3bm\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3bm\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3bm\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3bm\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3bm\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3bm\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3bm\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3bm\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3bm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3bm\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3bm\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3bm\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3bm\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3bm\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3bm\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3bm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3bm\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3bm\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3bm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3bm\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3bm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3bm\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3bm\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3bm\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3bm\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3bm\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3bm\_ .node rect,#chatgpt-mermaid-\_r_3bm\_ .node circle,#chatgpt-mermaid-\_r_3bm\_ .node ellipse,#chatgpt-mermaid-\_r_3bm\_ .node polygon,#chatgpt-mermaid-\_r_3bm\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3bm\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3bm\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3bm\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3bm\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3bm\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Add a cache serverMany keys get remappedCache hit rate dropsDatabase traffic spikesDatabase latency increasesRequests time outRetries increase load

The architecture becomes less stable when you try to scale it.

### The key requirement

We want a partitioning algorithm with three properties:

1. Deterministic placement: The same key and the same cluster topology should produce the same destination.
2. Minimal remapping: Adding or removing a server should move as few keys as practical.
3. Reasonably balanced distribution: Servers should receive a fair share of the workload, taking their capacities into account.

Consistent hashing is designed to address the second requirement while supporting the others with an appropriate implementation.

There is one more important consideration for multi-region systems: even perfect key distribution doesn't guarantee low latency. A key can be assigned consistently to a server on the other side of the world.

We'll first understand how consistent hashing solves the partitioning problem. Then we'll introduce regional routing and residency constraints, which determine which servers are eligible to receive a key in the first place.

Type `next` to continue Chapter 1: How the consistent hash ring works internally.




















# Chapter 1: Foundations of Consistent Hashing

## Lesson 2 — How the consistent hash ring works internally

In the previous lesson, we saw why `hash(key) % N` causes massive remapping when the number of servers changes.

Now let's understand the central idea behind consistent hashing: instead of dividing hash values into a fixed number of server buckets, we place both servers and keys on a circular hash space.

We'll build this step by step.

## 1. Step 1: Create a hash space

Imagine a hash function that produces values from `0` to `999`.

```
0 -----------------------------------------------> 999
```

A hash function can map a string to a number:

```
hash("customer:101") = 245
hash("customer:102") = 720
hash("server-A")     = 100
hash("server-B")     = 400
hash("server-C")     = 800
```

These numbers are illustrative.

In ordinary modulo-based hashing, the number of servers determines how we divide this space.

In consistent hashing, we treat the space as a ring.

\#chatgpt-mermaid-\_r_3d0\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3d0\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3d0\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3d0\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3d0\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3d0\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3d0\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3d0\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3d0\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3d0\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3d0\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3d0\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3d0\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3d0\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3d0\_ p{margin:0;}#chatgpt-mermaid-\_r_3d0\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3d0\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3d0\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3d0\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3d0\_ .label text,#chatgpt-mermaid-\_r_3d0\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3d0\_ .node rect,#chatgpt-mermaid-\_r_3d0\_ .node circle,#chatgpt-mermaid-\_r_3d0\_ .node ellipse,#chatgpt-mermaid-\_r_3d0\_ .node polygon,#chatgpt-mermaid-\_r_3d0\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3d0\_ .rough-node .label text,#chatgpt-mermaid-\_r_3d0\_ .node .label text,#chatgpt-mermaid-\_r_3d0\_ .image-shape .label,#chatgpt-mermaid-\_r_3d0\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3d0\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3d0\_ .rough-node .label,#chatgpt-mermaid-\_r_3d0\_ .node .label,#chatgpt-mermaid-\_r_3d0\_ .image-shape .label,#chatgpt-mermaid-\_r_3d0\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3d0\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3d0\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3d0\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3d0\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3d0\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3d0\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3d0\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3d0\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3d0\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3d0\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3d0\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3d0\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3d0\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3d0\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3d0\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3d0\_ .icon-shape,#chatgpt-mermaid-\_r_3d0\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3d0\_ .icon-shape p,#chatgpt-mermaid-\_r_3d0\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3d0\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3d0\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3d0\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3d0\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3d0\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3d0\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3d0\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3d0\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3d0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3d0\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3d0\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3d0\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3d0\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3d0\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3d0\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3d0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3d0\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3d0\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3d0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3d0\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3d0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3d0\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3d0\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3d0\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3d0\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3d0\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3d0\_ .node rect,#chatgpt-mermaid-\_r_3d0\_ .node circle,#chatgpt-mermaid-\_r_3d0\_ .node ellipse,#chatgpt-mermaid-\_r_3d0\_ .node polygon,#chatgpt-mermaid-\_r_3d0\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3d0\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3d0\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3d0\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3d0\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3d0\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Linear hash space: 0 to 999Connect the end back to thebeginningCircular hash ringPlace servers and keys athash positions

Why a ring?

Because the space has no natural beginning or end for routing purposes. If a key lies near the maximum hash value and its next server is near zero, the ring allows us to wrap around.

## 2. Step 2: Place servers on the ring

Hash each server's identity to determine its position.

```
hash("server-A") = 100
hash("server-B") = 400
hash("server-C") = 800
```

The ring now looks like this:

```
0 ---- A(100) -------- B(400) ---------------- C(800) ---- 999
^                                                           |
|___________________________________________________________|
```

Each server occupies a position on the ring.

Notice that the servers are not necessarily evenly spaced. Their positions depend on the hash function.

## 3. Step 3: Place data keys on the ring

Next, hash each data key using the same hash space.

```
hash("customer:101") = 250
hash("customer:102") = 650
hash("customer:103") = 900
```

Now we have both servers and keys on the same ring.

```
0 ---- A(100) -- K1(250) -- B(400) -- K2(650) -- C(800) -- K3(900)
^                                                                      |
|______________________________________________________________________|
```

Here:

- `A`, `B`, and `C` represent servers.
- `K1`, `K2`, and `K3` represent data keys.

But how do we decide which server owns each key?

## 4. Step 4: Walk clockwise to find the owner

The rule is simple:

Starting from a key's hash position, move clockwise until you encounter the first server. That server owns the key.

Let's apply it.

### Key 1: `customer:101`

Its hash position is `250`.

```
A(100) ---- K1(250) ---- B(400)
                            ^
                            |
                       First clockwise server
```

The next server is B at position 400.

Therefore:

```
customer:101 → Server B
```

### Key 2: `customer:102`

Its hash position is `650`.

The next server clockwise is C at position 800.

```
customer:102 → Server C
```

### Key 3: `customer:103`

Its hash position is `900`.

There are no servers after 900 before reaching the end of the ring. We wrap around to zero.

The first server encountered is A at position 100.

```
customer:103 → Server A
```

Our final placement is:

| Key            | Hash position | Assigned server |
| -------------- | ------------- | --------------- |
| `customer:101` | 250           | B               |
| `customer:102` | 650           | C               |
| `customer:103` | 900           | A               |

The rule is deterministic: if two application instances use the same ring and hash function, they will select the same server for a given key.

## 5. Step 5: What happens when we add a server?

This is where consistent hashing becomes useful.

Suppose we add Server D, whose hash position is 600.

Before adding D:

```
A(100) ---- K1(250) ---- B(400) ---- K2(650) ---- C(800)
```

After adding D:

```
A(100) ---- K1(250) ---- B(400) ---- D(600) ---- K2(650) ---- C(800)
```

Let's recalculate the ownership.

| Key                   | Before | After |
| --------------------- | ------ | ----- |
| `customer:101` at 250 | B      | B     |
| `customer:102` at 650 | C      | C     |
| `customer:103` at 900 | A      | A     |

In this particular example, none of the three existing keys moves.

But some keys will move in a larger dataset. Specifically, keys that previously belonged to B and lie between B's position and D's position will now belong to D.

For example:

```
hash("customer:106") = 550
```

Before adding D:

```
550 → B at 400, because C at 800 is next
```

After adding D:

```
550 → D at 600
```

Only keys in the relevant interval need to move.

### Why this is better than modulo

With modulo hashing, changing the number of servers changes the bucket calculation for almost every key.

With consistent hashing, adding a server changes ownership only for keys in the portion of the ring newly assigned to that server.

Under an idealized, uniformly distributed ring with \\(N\\) equally capable servers, adding one server to \\(N\\) servers moves approximately:

\\[ \frac{1}{N+1} \\]

of the keys on average.

For example, going from 9 servers to 10 servers means approximately 10% of keys move under those assumptions, rather than most keys being remapped.

Real-world results depend on the hash distribution, virtual nodes, and server weights.

## 6. Step 6: What happens when a server is removed?

Suppose Server B fails or is removed from the ring.

```
Before:
A(100) ---- B(400) ---- C(800)

After:
A(100) -------------- C(800)
```

Keys previously assigned to B now move to the next eligible server clockwise, which is C.

Other keys remain assigned to their existing servers.

This is useful when:

- A server fails.
- You add capacity.
- You replace an instance.
- You rebalance a distributed cache.
- You change the number of storage partitions.

However, consistent hashing only changes the ownership calculation. If the key contains actual data, your system must still ensure that the data is available at the new destination. That requires caching behavior, replication, data migration, or another storage-specific mechanism.

## 7. What have we actually achieved?

Let's summarize the mechanics.

\#chatgpt-mermaid-\_r_3ff\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3ff\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3ff\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3ff\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3ff\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ff\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3ff\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3ff\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3ff\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3ff\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3ff\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3ff\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ff\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ff\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3ff\_ p{margin:0;}#chatgpt-mermaid-\_r_3ff\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ff\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ff\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ff\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3ff\_ .label text,#chatgpt-mermaid-\_r_3ff\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ff\_ .node rect,#chatgpt-mermaid-\_r_3ff\_ .node circle,#chatgpt-mermaid-\_r_3ff\_ .node ellipse,#chatgpt-mermaid-\_r_3ff\_ .node polygon,#chatgpt-mermaid-\_r_3ff\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3ff\_ .rough-node .label text,#chatgpt-mermaid-\_r_3ff\_ .node .label text,#chatgpt-mermaid-\_r_3ff\_ .image-shape .label,#chatgpt-mermaid-\_r_3ff\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3ff\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3ff\_ .rough-node .label,#chatgpt-mermaid-\_r_3ff\_ .node .label,#chatgpt-mermaid-\_r_3ff\_ .image-shape .label,#chatgpt-mermaid-\_r_3ff\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3ff\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3ff\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ff\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ff\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3ff\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3ff\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3ff\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3ff\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3ff\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3ff\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3ff\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ff\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ff\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3ff\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ff\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3ff\_ .icon-shape,#chatgpt-mermaid-\_r_3ff\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3ff\_ .icon-shape p,#chatgpt-mermaid-\_r_3ff\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3ff\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3ff\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3ff\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3ff\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3ff\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3ff\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3ff\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3ff\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3ff\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ff\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3ff\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3ff\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3ff\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ff\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3ff\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3ff\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ff\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3ff\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3ff\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ff\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3ff\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ff\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3ff\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3ff\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3ff\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3ff\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3ff\_ .node rect,#chatgpt-mermaid-\_r_3ff\_ .node circle,#chatgpt-mermaid-\_r_3ff\_ .node ellipse,#chatgpt-mermaid-\_r_3ff\_ .node polygon,#chatgpt-mermaid-\_r_3ff\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3ff\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3ff\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3ff\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3ff\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ff\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Hash server identitiesPlace servers on ringHash data keyFind key positionWalk clockwise to nextserverDetermine key ownerAdd or remove serverRecalculate affectedownershipOnly a portion of keys move

Consistent hashing provides:

- Deterministic key placement.
- Reduced remapping when the cluster changes.
- A natural way to add or remove nodes.
- A foundation for distributing keys across caches and storage nodes.

It does not inherently provide:

- Even load distribution.
- Replication or durability.
- Strong consistency.
- Geographic locality.
- GDPR compliance.
- Safe regional failover.

Those concerns require additional mechanisms.

## 8. A small exercise

Assume the ring contains three servers:

```
A = 100
B = 400
C = 800
```

And the keys are:

```
K1 = 150
K2 = 450
K3 = 850
K4 = 50
```

Using the clockwise ownership rule, which server owns each key?

Try answering before reading the solution.

## 9. Why this matters in multi-region systems

Imagine that A, B, and C are not individual servers but entire regions.

The same algorithm could distribute keys across them. However, a user in Germany might get a key assigned to a server in India, adding network latency. Worse, a European customer's data might be routed to a region where that data is not permitted to reside.

This gives us the next major problem to solve:

How do we distribute keys evenly while ensuring that requests go to the correct region and data never gets routed to an unauthorized region?

In the next lesson, we'll explore hash distribution, why consistent hashing can still produce hotspots, and how virtual nodes improve the situation.

Type `next` to continue.
























# Chapter 1: Foundations of Consistent Hashing

## Lesson 3 — Hash Distribution, Hotspots, and Virtual Nodes

In the previous lesson, we learned how a key is assigned to the first server clockwise on a hash ring.

Now we need to address a problem that appears in real distributed systems:

Even if consistent hashing minimizes key movement, it does not automatically distribute data and traffic evenly across servers.

Let's understand why, and how virtual nodes solve much of this problem.

## 1. The problem with uneven server distribution

Suppose you have four servers in your cluster.

```
Hash ring:

0 ───── A ───────────────── B ── C ───────────────────────── D ── 999
```

Notice that the servers are not evenly spaced.

Each server owns the keys between the previous server and its own position, following the clockwise ownership rule.

For example:

```
Server A owns: (D, A]   // wraps around zero
Server B owns: (A, B]
Server C owns: (B, C]
Server D owns: (C, D]
```

The intervals between servers determine how much of the hash space each server owns.

If the hash space is uniformly distributed, a server owning 50% of the ring will receive approximately 50% of the keys.

A server owning 5% of the ring will receive approximately 5%.

That is the problem: the amount of space a server owns depends on where its hash position happens to fall.

### Example

Imagine the ring has 1,000 possible positions.

| Server | Ring positions owned | Approximate share |
| ------ | -------------------- | ----------------- |
| A      | 100                  | 10%               |
| B      | 600                  | 60%               |
| C      | 100                  | 10%               |
| D      | 200                  | 20%               |
| Total  | 1,000                | 100%              |

If you have 10 million uniformly distributed keys, Server B could receive roughly 6 million keys while Server C receives only 1 million.

This creates an imbalance in:

- Memory consumption.
- CPU utilization.
- Network traffic.
- Request latency.
- Storage capacity.

Consistent hashing has solved the remapping problem, but we still have a distribution problem.

## 2. Why not just put every server exactly 250 positions apart?

We could manually arrange the four servers evenly:

```
A = 0
B = 250
C = 500
D = 750
```

That would produce equal-sized intervals initially.

But real distributed systems are dynamic.

What happens if you add a fifth server? What if one server has twice the memory of another? What if you replace a server, or the ring's membership changes repeatedly?

Maintaining perfectly balanced positions becomes more complicated.

We need a way to distribute each physical server across many positions on the ring.

That brings us to virtual nodes.

## 3. What are virtual nodes?

A virtual node, or vnode, is a logical position on the hash ring associated with a physical server.

Instead of placing each server on the ring once, we place it many times.

For example, instead of:

```
Server A → A
Server B → B
Server C → C
```

we create:

```
Server A → A1, A2, A3, A4
Server B → B1, B2, B3, B4
Server C → C1, C2, C3, C4
```

Each virtual node has its own position, typically calculated by hashing a unique identifier.

For example:

```
hash("server-A#1") = 80
hash("server-A#2") = 340
hash("server-A#3") = 690

hash("server-B#1") = 150
hash("server-B#2") = 470
hash("server-B#3") = 920
```

These positions are illustrative.

Although a physical server appears multiple times, the actual requests still go to the same physical machine whenever one of its virtual nodes is selected.

### Visualizing the ring

```
Ring positions:

A1(80) -- B1(150) -- A2(340) -- B2(470) -- A3(690) -- B3(920)
```

Suppose a key hashes to position 200.

The next virtual node clockwise is A2 at 340.

Therefore:

```
key → A2 → physical Server A
```

A key at position 500 would go to A3 at 690, which also belongs to Server A.

The virtual nodes are logical routing points, not additional machines.

## 4. How virtual nodes improve distribution

Consider a ring with only three physical servers and one position per server.

A server's share depends heavily on its neighboring positions.

Now suppose each server has 100 virtual nodes distributed around the ring.

\#chatgpt-mermaid-\_r_3ic\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3ic\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3ic\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3ic\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3ic\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ic\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3ic\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3ic\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3ic\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3ic\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3ic\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3ic\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ic\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ic\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3ic\_ p{margin:0;}#chatgpt-mermaid-\_r_3ic\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ic\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ic\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ic\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3ic\_ .label text,#chatgpt-mermaid-\_r_3ic\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ic\_ .node rect,#chatgpt-mermaid-\_r_3ic\_ .node circle,#chatgpt-mermaid-\_r_3ic\_ .node ellipse,#chatgpt-mermaid-\_r_3ic\_ .node polygon,#chatgpt-mermaid-\_r_3ic\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3ic\_ .rough-node .label text,#chatgpt-mermaid-\_r_3ic\_ .node .label text,#chatgpt-mermaid-\_r_3ic\_ .image-shape .label,#chatgpt-mermaid-\_r_3ic\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3ic\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3ic\_ .rough-node .label,#chatgpt-mermaid-\_r_3ic\_ .node .label,#chatgpt-mermaid-\_r_3ic\_ .image-shape .label,#chatgpt-mermaid-\_r_3ic\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3ic\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3ic\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ic\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ic\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3ic\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3ic\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3ic\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3ic\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3ic\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3ic\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3ic\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ic\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ic\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3ic\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ic\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3ic\_ .icon-shape,#chatgpt-mermaid-\_r_3ic\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3ic\_ .icon-shape p,#chatgpt-mermaid-\_r_3ic\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3ic\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3ic\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3ic\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3ic\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3ic\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3ic\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3ic\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3ic\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3ic\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ic\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3ic\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3ic\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3ic\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ic\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3ic\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3ic\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ic\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3ic\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3ic\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ic\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3ic\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ic\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3ic\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3ic\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3ic\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3ic\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3ic\_ .node rect,#chatgpt-mermaid-\_r_3ic\_ .node circle,#chatgpt-mermaid-\_r_3ic\_ .node ellipse,#chatgpt-mermaid-\_r_3ic\_ .node polygon,#chatgpt-mermaid-\_r_3ic\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3ic\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3ic\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3ic\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3ic\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ic\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Physical Server AA1 through A100Physical Server BB1 through B100Physical Server CC1 through C100Shared hash ringKeys distributed across ringintervals

Each physical server now owns many smaller intervals scattered across the ring.

A single unusually large interval is less likely to dominate the server's total share.

The result is generally better distribution across the cluster.

### Important distinction

Virtual nodes improve statistical distribution, not guaranteed equality.

You can still have uneven key counts because of randomness, skewed key distributions, or hot keys.

For production systems, you should monitor actual load rather than assume the ring guarantees perfect balance.

## 5. Why more virtual nodes usually improve balance

Imagine distributing 1,000 keys across three servers.

With one vnode per server, the ring has only three ownership intervals. A large interval can give one server a disproportionate share.

With 100 vnodes per server, the ring has 300 positions. The ownership intervals are more numerous and scattered.

The server's total share is now the sum of many smaller intervals.

Under an idealized model with uniformly distributed vnode positions, independent placement, and equal-capacity servers, the relative standard deviation of a server's ring share decreases approximately in proportion to:

\\[ \frac{1}{\sqrt{V}} \\]

where \\(V\\) is the number of virtual nodes per server.

For example:

| Virtual nodes per server | Relative variability, compared with one vnode |
| ------------------------ | --------------------------------------------- |
| 1                        | 100%                                          |
| 4                        | 50%                                           |
| 25                       | 20%                                           |
| 100                      | 10%                                           |
| 400                      | 5%                                            |

These are approximate statistical relationships, not guarantees about actual production load.

The benefit has diminishing returns: increasing vnodes from 100 to 400 improves the theoretical variability, but also increases ring metadata and membership-management overhead.

## 6. How do we implement virtual nodes?

Here is a simplified Java implementation of the ring's core logic.

```java
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashRing {

    private final SortedMap<Long, String> ring = new TreeMap<>();
    private final int virtualNodes;

    public ConsistentHashRing(int virtualNodes) {
        if (virtualNodes <= 0) {
            throw new IllegalArgumentException(
                "Virtual nodes must be positive"
            );
        }

        this.virtualNodes = virtualNodes;
    }

    public void addServer(String serverId) {
        for (int i = 0; i < virtualNodes; i++) {
            long position = hash(serverId + "#" + i);
            ring.put(position, serverId);
        }
    }

    public String getServer(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("No servers available");
        }

        long position = hash(key);

        // Find the first ring position >= the key position.
        SortedMap<Long, String> tail = ring.tailMap(position);

        // Wrap around to the beginning when necessary.
        long ownerPosition = tail.isEmpty()
            ? ring.firstKey()
            : tail.firstKey();

        return ring.get(ownerPosition);
    }

    private long hash(String value) {
        try {
            byte[] bytes = MessageDigest
                .getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));

            long result = 0;
            for (int i = 0; i < 8; i++) {
                result = (result << 8) | (bytes[i] & 0xffL);
            }

            return result & Long.MAX_VALUE;

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
```

This example illustrates the basic idea, but it is not a complete production implementation. In particular, it does not implement node removal, collision handling, weighted capacity, or distributed membership synchronization.

Also, masking the result to a nonnegative 63-bit value reduces the hash space compared with a full unsigned 64-bit representation. A production implementation should use a well-defined hash representation consistently across every application instance.

### How the implementation works

`addServer()`

Creates multiple virtual nodes for one physical server.

```
ring.put(hash(serverId + "#" + i), serverId);
```

The ring stores:

```
virtual node position → physical server ID
```

`getServer()`

Hashes the requested key and looks for the first virtual node clockwise.

```
SortedMap<Long, String> tail = ring.tailMap(position);
```

If no virtual node exists at a greater position, it wraps around to the first ring position.

Because `TreeMap` maintains sorted keys, finding the next position is efficient: typically \\(O(\log M)\\), where \\(M\\) is the total number of virtual nodes.

One critical production requirement is that every instance must agree on the ring membership, hashing algorithm, key encoding, and vnode configuration. Otherwise, two application instances might route the same key to different servers.

## 7. What about servers with different capacities?

Not every server is identical.

Imagine this cluster:

| Server | Relative capacity |
| ------ | ----------------- |
| A      | 1×                |
| B      | 2×                |
| C      | 4×                |

Server C can handle approximately four times the capacity of Server A.

Giving each server the same number of virtual nodes may not be ideal.

Instead, you can assign vnode counts proportionally:

```
Server A → 100 virtual nodes
Server B → 200 virtual nodes
Server C → 400 virtual nodes
```

This biases the expected ownership share toward more capable servers.

For example, the approximate target shares would be:

- A: 14.3%
- B: 28.6%
- C: 57.1%

These percentages assume the relative capacities are representative of the intended distribution and that vnode placement is sufficiently well distributed.

However, vnode count should not blindly reflect CPU capacity alone. For a cache, memory might be the limiting resource. For a database, disk throughput or write capacity may be more important.

Also, changing vnode counts changes the ring and may cause keys to move. Capacity changes should therefore be treated as controlled membership changes.

## 8. Virtual nodes do not solve hot keys

Suppose you have one million customer keys, but a single key is requested 100,000 times per second.

```
customer:premium-plan
```

Consistent hashing always maps this key to its assigned owner.

Even if you use 10,000 virtual nodes, the same key still resolves to one owner in a conventional single-owner ring.

That server can become overloaded while other servers remain mostly idle.

This is called a hot-key problem.

Potential solutions include:

- Replicating popular, read-heavy cache entries.
- Using a local cache in each application instance.
- Splitting a large logical key into smaller partitions when the data model permits it.
- Applying request coalescing or rate limiting.
- Using load-aware routing for replicated reads.

Each solution has trade-offs. For example, replicating a key can improve read throughput but introduces cache invalidation and consistency challenges.

## 9. Now connect this to multi-region architecture

Virtual nodes solve a distribution problem inside a cluster. They do not determine which geographic region should receive a request.

Suppose you have:

```
EU region:
  EU-A → 100 vnodes
  EU-B → 100 vnodes

India region:
  IN-A → 100 vnodes
  IN-B → 100 vnodes
```

A residency-aware design uses two independent regional rings.

\#chatgpt-mermaid-\_r_3jr\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3jr\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3jr\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3jr\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3jr\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3jr\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3jr\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3jr\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3jr\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3jr\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3jr\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3jr\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3jr\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3jr\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3jr\_ p{margin:0;}#chatgpt-mermaid-\_r_3jr\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3jr\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3jr\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3jr\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3jr\_ .label text,#chatgpt-mermaid-\_r_3jr\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3jr\_ .node rect,#chatgpt-mermaid-\_r_3jr\_ .node circle,#chatgpt-mermaid-\_r_3jr\_ .node ellipse,#chatgpt-mermaid-\_r_3jr\_ .node polygon,#chatgpt-mermaid-\_r_3jr\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3jr\_ .rough-node .label text,#chatgpt-mermaid-\_r_3jr\_ .node .label text,#chatgpt-mermaid-\_r_3jr\_ .image-shape .label,#chatgpt-mermaid-\_r_3jr\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3jr\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3jr\_ .rough-node .label,#chatgpt-mermaid-\_r_3jr\_ .node .label,#chatgpt-mermaid-\_r_3jr\_ .image-shape .label,#chatgpt-mermaid-\_r_3jr\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3jr\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3jr\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3jr\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3jr\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3jr\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3jr\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3jr\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3jr\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3jr\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3jr\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3jr\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3jr\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3jr\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3jr\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3jr\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3jr\_ .icon-shape,#chatgpt-mermaid-\_r_3jr\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3jr\_ .icon-shape p,#chatgpt-mermaid-\_r_3jr\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3jr\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3jr\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3jr\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3jr\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3jr\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3jr\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3jr\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3jr\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3jr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3jr\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3jr\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3jr\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3jr\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3jr\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3jr\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3jr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3jr\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3jr\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3jr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3jr\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3jr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3jr\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3jr\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3jr\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3jr\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3jr\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3jr\_ .node rect,#chatgpt-mermaid-\_r_3jr\_ .node circle,#chatgpt-mermaid-\_r_3jr\_ .node ellipse,#chatgpt-mermaid-\_r_3jr\_ .node polygon,#chatgpt-mermaid-\_r_3jr\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3jr\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3jr\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3jr\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3jr\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3jr\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}RequestResolve tenant policyEligible regionEU regional ringIndia regional ringEU-A or EU-BIN-A or IN-B

The application first selects an eligible region, then uses that region's ring to select a node.

This separation prevents a vnode belonging to an Indian server from being considered for a tenant whose data is restricted to the EU.

If you instead put every server into one global ring, a key could map to any region unless you build additional placement constraints into the design.

The general rule is: use consistent hashing to choose among eligible destinations, not to decide which destinations are legally eligible.

## 10. What you should remember

- A consistent hash ring can distribute keys unevenly when it has too few node positions.
- Virtual nodes give each physical server multiple positions on the ring.
- More virtual nodes generally improve statistical balance, with diminishing returns.
- Weighted vnode counts can account for unequal server capacities.
- Hot keys remain a problem even when the overall distribution is balanced.
- Regional rings separate local key placement from global geographic routing.
- Consistent hashing does not provide replication, durability, or GDPR compliance.

### Interview question

Why do distributed systems use virtual nodes in consistent hashing?

A strong answer:

> Virtual nodes allow each physical server to occupy multiple positions on the hash ring. This reduces the impact of random ring placement, improves distribution, and lets us represent servers with different capacities by assigning different vnode counts. When a server is added or removed, only the keys associated with the affected ring intervals generally need to be remapped. However, virtual nodes don't guarantee perfect load balance and don't solve hot keys or replication.

In the next lesson, we'll compare consistent hashing with rendezvous hashing and modulo hashing, understand their trade-offs, and identify where each approach fits in a multi-region architecture.

Type `next` to continue.







# Chapter 1: Foundations of Consistent Hashing

## Lesson 4 — Consistent Hashing vs. Modulo Hashing vs. Rendezvous Hashing

So far, we've learned how a hash ring works, how keys get assigned to servers, and why virtual nodes improve distribution.

Now let's answer an important system design question:

Is consistent hashing always the best way to distribute data across servers?

No. There are multiple partitioning strategies, and the right choice depends on how frequently your cluster changes, how you want to distribute load, and what your system needs to guarantee.

We'll compare three approaches:

1. Modulo hashing
2. Ring-based consistent hashing
3. Rendezvous hashing, also called highest-random-weight hashing

## 1. Modulo hashing

This is the simplest approach.

```
int serverIndex = Math.floorMod(hash(key), serverCount);
```

Suppose you have four servers:

```
Server 0
Server 1
Server 2
Server 3
```

For each key, you calculate a hash and take its remainder when divided by the number of servers.

```
hash("customer:101") % 4 → Server 2
hash("customer:102") % 4 → Server 0
hash("customer:103") % 4 → Server 3
```

The exact assignments depend on the hash function.

### What happens when a server is added?

Suppose you add Server 4.

The calculation changes from:

```
hash(key) % 4
```

to:

```
hash(key) % 5
```

Many keys now map to different servers.

For uniformly distributed hashes, when increasing the number of servers from \\(N\\) to \\(N+1\\), the expected fraction of keys that retain their previous server is approximately \\(1/(N+1)\\).

Therefore, the fraction remapped is approximately:

\\[ 1-\frac{1}{N+1}=\frac{N}{N+1} \\]

For 4 servers becoming 5, this is approximately 80% remapping.

That can be expensive for a distributed cache or sharded storage system.

### When is modulo hashing useful?

Modulo hashing is appropriate when:

- The number of buckets is fixed.
- Bucket identities are stable.
- You don't frequently add or remove physical destinations.
- You want a simple way to partition a fixed number of logical shards.

A useful refinement is to hash keys into a fixed number of logical partitions, then assign those partitions to physical servers separately. This reduces the coupling between key hashing and the number of physical servers, although moving logical partitions still requires careful migration.

## 2. Ring-based consistent hashing

Consistent hashing separates the key's hash value from the number of physical servers.

Each server is assigned positions on a ring, and each key maps to the next eligible server clockwise.

\#chatgpt-mermaid-\_r_3m0\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3m0\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3m0\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3m0\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3m0\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3m0\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3m0\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3m0\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3m0\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3m0\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3m0\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3m0\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3m0\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3m0\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3m0\_ p{margin:0;}#chatgpt-mermaid-\_r_3m0\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3m0\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3m0\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3m0\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3m0\_ .label text,#chatgpt-mermaid-\_r_3m0\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3m0\_ .node rect,#chatgpt-mermaid-\_r_3m0\_ .node circle,#chatgpt-mermaid-\_r_3m0\_ .node ellipse,#chatgpt-mermaid-\_r_3m0\_ .node polygon,#chatgpt-mermaid-\_r_3m0\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3m0\_ .rough-node .label text,#chatgpt-mermaid-\_r_3m0\_ .node .label text,#chatgpt-mermaid-\_r_3m0\_ .image-shape .label,#chatgpt-mermaid-\_r_3m0\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3m0\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3m0\_ .rough-node .label,#chatgpt-mermaid-\_r_3m0\_ .node .label,#chatgpt-mermaid-\_r_3m0\_ .image-shape .label,#chatgpt-mermaid-\_r_3m0\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3m0\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3m0\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3m0\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3m0\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3m0\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3m0\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3m0\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3m0\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3m0\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3m0\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3m0\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3m0\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3m0\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3m0\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3m0\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3m0\_ .icon-shape,#chatgpt-mermaid-\_r_3m0\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3m0\_ .icon-shape p,#chatgpt-mermaid-\_r_3m0\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3m0\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3m0\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3m0\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3m0\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3m0\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3m0\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3m0\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3m0\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3m0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3m0\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3m0\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3m0\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3m0\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3m0\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3m0\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3m0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3m0\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3m0\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3m0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3m0\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3m0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3m0\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3m0\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3m0\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3m0\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3m0\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3m0\_ .node rect,#chatgpt-mermaid-\_r_3m0\_ .node circle,#chatgpt-mermaid-\_r_3m0\_ .node ellipse,#chatgpt-mermaid-\_r_3m0\_ .node polygon,#chatgpt-mermaid-\_r_3m0\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3m0\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3m0\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3m0\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3m0\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3m0\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Hash keyFind key position on ringFind next eligible serverRoute request to serverServer added or removedUpdate ring membershipOnly affected key ownershipchanges

### Main advantage

Adding or removing a server typically remaps only a fraction of keys.

Under the usual idealized assumptions of \\(N\\) equal-capacity servers and a well-distributed ring, adding one server moves approximately \\(1/(N+1)\\) of the keys.

This is why consistent hashing is popular for distributed caches and certain distributed storage systems.

### Main disadvantages

Ring-based consistent hashing requires you to manage:

- Ring membership.
- Virtual-node placement.
- Hash collisions.
- Uneven partition sizes.
- Membership changes across application instances.
- Data movement when ownership changes.

A particularly important detail is that changing ownership does not automatically move the data. Your application or datastore must provide the necessary migration, replication, or cache-miss behavior.

## 3. Rendezvous hashing

Rendezvous hashing takes a different approach.

Instead of placing servers on a ring, it calculates a score for every eligible server using the key and server identity.

The server with the highest score becomes the owner.

For a key \\(k\\) and server \\(s\\):

\\[ \operatorname{score}(k,s)=H(k,s) \\]

Then choose:

\\[ \operatorname{owner}(k) = \underset{s\in S}{\operatorname{argmax}}\ H(k,s) \\]

Here, \\(S\\) is the set of eligible servers.

For example, imagine these scores:

| Key            | Server A | Server B | Server C | Winner |
| -------------- | -------- | -------- | -------- | ------ |
| `customer:101` | 12       | 87       | 43       | B      |
| `customer:102` | 94       | 21       | 63       | A      |
| `customer:103` | 35       | 44       | 92       | C      |

The highest-scoring server owns the key.

When a new server is added, you calculate its score for each key. A key moves only when the new server's score exceeds the previous winner's score, assuming the scoring scheme is appropriately designed.

### Why is this useful?

Rendezvous hashing doesn't require you to maintain a sorted ring of virtual nodes.

The basic algorithm is conceptually simple:

```
def choose_server(key, servers):    return max(        servers,        key=lambda server: hash_key(key, server)    )
```

`hash_key()` here represents a suitable deterministic hash function.

This simple version evaluates every server, so choosing an owner costs \\(O(N)\\) score evaluations per key for \\(N\\) servers.

That can be practical for a modest number of destinations, while optimized implementations and specialized variants can reduce the cost for larger systems.

Rendezvous hashing can also support weighted placement and selecting multiple ranked destinations.

## 4. Comparing the three approaches

| Property                                  | Modulo hashing              | Ring-based consistent hashing                                     | Rendezvous hashing                        |
| ----------------------------------------- | --------------------------- | ----------------------------------------------------------------- | ----------------------------------------- |
| Basic mechanism                           | Hash modulo server count    | Next server clockwise                                             | Highest server score                      |
| Minimal remapping on membership changes   | Generally no                | Yes                                                               | Yes                                       |
| Needs a ring data structure               | No                          | Yes                                                               | No                                        |
| Basic owner selection                     | \\(O(1)\\) after hashing    | Typically \\(O(\log M)\\) with a sorted ring of \\(M\\) positions | \\(O(N)\\) score evaluations              |
| Uneven distribution controls              | Depends on hash and buckets | Virtual nodes, weights                                            | Hash quality, weights, selection strategy |
| Membership consistency required           | Yes                         | Yes                                                               | Yes                                       |
| Automatically replicates data             | No                          | No                                                                | No                                        |
| Automatically provides strong consistency | No                          | No                                                                | No                                        |
| Automatically handles geographic locality | No                          | No                                                                | No                                        |

These complexity figures describe the basic implementations. Optimized implementations may differ.

## 5. Which approach should you use in a multi-region system?

Let's apply these algorithms to a real architecture.

Suppose you're building a multi-tenant SaaS application with deployments in:

- Europe
- India
- The United States

You want to achieve three things:

1. Low latency for users.
2. Minimal key movement when scaling servers.
3. Compliance with each tenant's data residency policy.

Neither consistent hashing nor rendezvous hashing can decide all three things by itself.

Instead, separate the architecture into two decisions.

Decision 1: Select the eligible region.

Use the tenant's residency policy and regional health to determine which regions are allowed to serve the request.

Decision 2: Select the server within that region.

Use consistent hashing or rendezvous hashing to choose the server from the eligible pool.

\#chatgpt-mermaid-\_r_3mu\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3mu\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3mu\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3mu\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3mu\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3mu\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3mu\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3mu\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3mu\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3mu\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3mu\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3mu\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3mu\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3mu\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3mu\_ p{margin:0;}#chatgpt-mermaid-\_r_3mu\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3mu\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3mu\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3mu\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3mu\_ .label text,#chatgpt-mermaid-\_r_3mu\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3mu\_ .node rect,#chatgpt-mermaid-\_r_3mu\_ .node circle,#chatgpt-mermaid-\_r_3mu\_ .node ellipse,#chatgpt-mermaid-\_r_3mu\_ .node polygon,#chatgpt-mermaid-\_r_3mu\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3mu\_ .rough-node .label text,#chatgpt-mermaid-\_r_3mu\_ .node .label text,#chatgpt-mermaid-\_r_3mu\_ .image-shape .label,#chatgpt-mermaid-\_r_3mu\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3mu\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3mu\_ .rough-node .label,#chatgpt-mermaid-\_r_3mu\_ .node .label,#chatgpt-mermaid-\_r_3mu\_ .image-shape .label,#chatgpt-mermaid-\_r_3mu\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3mu\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3mu\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3mu\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3mu\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3mu\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3mu\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3mu\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3mu\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3mu\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3mu\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3mu\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3mu\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3mu\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3mu\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3mu\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3mu\_ .icon-shape,#chatgpt-mermaid-\_r_3mu\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3mu\_ .icon-shape p,#chatgpt-mermaid-\_r_3mu\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3mu\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3mu\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3mu\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3mu\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3mu\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3mu\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3mu\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3mu\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3mu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3mu\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3mu\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3mu\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3mu\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3mu\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3mu\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3mu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3mu\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3mu\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3mu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3mu\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3mu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3mu\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3mu\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3mu\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3mu\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3mu\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3mu\_ .node rect,#chatgpt-mermaid-\_r_3mu\_ .node circle,#chatgpt-mermaid-\_r_3mu\_ .node ellipse,#chatgpt-mermaid-\_r_3mu\_ .node polygon,#chatgpt-mermaid-\_r_3mu\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3mu\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3mu\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3mu\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3mu\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3mu\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Incoming requestResolve tenant policyDetermine eligible regionsChoose suitable regionGet eligible regional serversConsistent hashing orrendezvous hashingSelected regional serverLocal cache or authorizeddatastore

This separation makes the architecture easier to reason about and test.

For example, an EU-only tenant should never have an Indian server included in its eligible server set for residency-restricted data.

The same principle applies if you use rendezvous hashing: filter the candidate servers first, then calculate the winning score among those servers.

## 6. A subtle issue: server selection is not the same as data availability

Suppose rendezvous hashing selects Server B for `customer:101`.

Later, Server B fails.

The algorithm may select Server C as the new owner.

But what happens to the data that was previously stored on B?

The new owner may not have that data.

You need a separate strategy, such as:

- Replicating the data to additional servers.
- Rebuilding cache entries from an authoritative database.
- Migrating data when ownership changes.
- Using a storage system that manages partition ownership and replication internally.

This is why you must distinguish placement from replication.

A placement algorithm answers:

> Which server should handle this key?

A replication strategy answers:

> Which copies of this key exist, where are they stored, and how up to date are they?

A consistency protocol answers:

> Which reads and writes are allowed to succeed, and what guarantees do they provide?

These are separate responsibilities.

## 7. Interview-ready answer

Question: How would you choose between consistent hashing and rendezvous hashing?

A strong answer:

> Both algorithms minimize unnecessary key remapping when cluster membership changes. Ring-based consistent hashing uses ordered hash positions and typically employs virtual nodes to improve distribution. Rendezvous hashing scores each eligible server for a key and chooses the highest-scoring one, avoiding the need to maintain a hash ring. A basic rendezvous implementation evaluates all servers, whereas a ring implementation can find an owner efficiently through an ordered lookup.
>
> In a multi-region system, I would first enforce tenant residency and select an eligible region, then use one of these algorithms to distribute keys within that region. I would handle replication, data migration, consistency, and failover separately because neither algorithm provides those guarantees.

## 8. Check your understanding

Before continuing, make sure you can explain these questions in your own words:

1. Why can modulo hashing remap many keys when a server is added?
2. How does a hash ring reduce remapping?
3. What problem do virtual nodes solve?
4. How does rendezvous hashing select a server?
5. Why must residency filtering happen before regional server selection?
6. Why doesn't selecting a new owner guarantee that the new owner has the data?

In the next lesson, we'll bring these ideas together and explore the difference between consistent hashing, sharding, partitioning, and replication—four concepts that are frequently confused in distributed system design.

Type `next` to continue.















# Chapter 1: Foundations of Consistent Hashing

## Lesson 5 — Consistent Hashing vs. Sharding vs. Partitioning vs. Replication

These four concepts are closely related, but they solve different problems. Understanding their boundaries is essential before designing a multi-region system.

Let's build the intuition using a real example: a SaaS application storing 100 million customer records.

## 1. The problem: one database is no longer enough

Initially, your application has one database.

\#chatgpt-mermaid-\_r_3o8\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3o8\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3o8\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3o8\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3o8\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3o8\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3o8\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3o8\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3o8\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3o8\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3o8\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3o8\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3o8\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3o8\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3o8\_ p{margin:0;}#chatgpt-mermaid-\_r_3o8\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3o8\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3o8\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3o8\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3o8\_ .label text,#chatgpt-mermaid-\_r_3o8\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3o8\_ .node rect,#chatgpt-mermaid-\_r_3o8\_ .node circle,#chatgpt-mermaid-\_r_3o8\_ .node ellipse,#chatgpt-mermaid-\_r_3o8\_ .node polygon,#chatgpt-mermaid-\_r_3o8\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3o8\_ .rough-node .label text,#chatgpt-mermaid-\_r_3o8\_ .node .label text,#chatgpt-mermaid-\_r_3o8\_ .image-shape .label,#chatgpt-mermaid-\_r_3o8\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3o8\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3o8\_ .rough-node .label,#chatgpt-mermaid-\_r_3o8\_ .node .label,#chatgpt-mermaid-\_r_3o8\_ .image-shape .label,#chatgpt-mermaid-\_r_3o8\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3o8\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3o8\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3o8\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3o8\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3o8\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3o8\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3o8\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3o8\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3o8\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3o8\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3o8\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3o8\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3o8\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3o8\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3o8\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3o8\_ .icon-shape,#chatgpt-mermaid-\_r_3o8\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3o8\_ .icon-shape p,#chatgpt-mermaid-\_r_3o8\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3o8\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3o8\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3o8\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3o8\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3o8\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3o8\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3o8\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3o8\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3o8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3o8\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3o8\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3o8\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3o8\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3o8\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3o8\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3o8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3o8\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3o8\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3o8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3o8\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3o8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3o8\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3o8\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3o8\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3o8\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3o8\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3o8\_ .node rect,#chatgpt-mermaid-\_r_3o8\_ .node circle,#chatgpt-mermaid-\_r_3o8\_ .node ellipse,#chatgpt-mermaid-\_r_3o8\_ .node polygon,#chatgpt-mermaid-\_r_3o8\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3o8\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3o8\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3o8\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3o8\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3o8\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ApplicationSingle Database

As the application grows, the database struggles with:

- Storage capacity.
- Read and write throughput.
- CPU and memory pressure.
- Query latency.
- Availability during failures.

You decide to distribute the data across multiple database servers.

But how should you divide the records? And what happens if a server fails?

These questions lead to four different concepts.

## 2. Partitioning: divide data into smaller pieces

Partitioning means dividing a dataset into smaller, manageable subsets.

For example, you might divide customers by their IDs:

```
Partition 1: customer IDs 1–1,000,000
Partition 2: customer IDs 1,000,001–2,000,000
Partition 3: customer IDs 2,000,001–3,000,000
```

Each partition contains only part of the overall dataset.

Partitioning does not necessarily mean using multiple physical machines. A single PostgreSQL database, for example, can use table partitioning to divide one logical table into multiple physical partitions.

Common partitioning strategies include:

- Range partitioning.
- List partitioning.
- Hash partitioning.
- Composite partitioning.

### Why partition?

Smaller partitions can improve data management, query performance for suitable workloads, and operational flexibility.

However, partitioning alone does not guarantee scalability. If all partitions remain on one machine, that machine can still become the bottleneck.

## 3. Sharding: distribute partitions across machines

Sharding is a form of horizontal partitioning in which different subsets of data are assigned to different database instances or independent storage nodes.

For example:

\#chatgpt-mermaid-\_r_3om\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3om\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3om\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3om\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3om\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3om\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3om\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3om\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3om\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3om\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3om\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3om\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3om\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3om\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3om\_ p{margin:0;}#chatgpt-mermaid-\_r_3om\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3om\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3om\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3om\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3om\_ .label text,#chatgpt-mermaid-\_r_3om\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3om\_ .node rect,#chatgpt-mermaid-\_r_3om\_ .node circle,#chatgpt-mermaid-\_r_3om\_ .node ellipse,#chatgpt-mermaid-\_r_3om\_ .node polygon,#chatgpt-mermaid-\_r_3om\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3om\_ .rough-node .label text,#chatgpt-mermaid-\_r_3om\_ .node .label text,#chatgpt-mermaid-\_r_3om\_ .image-shape .label,#chatgpt-mermaid-\_r_3om\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3om\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3om\_ .rough-node .label,#chatgpt-mermaid-\_r_3om\_ .node .label,#chatgpt-mermaid-\_r_3om\_ .image-shape .label,#chatgpt-mermaid-\_r_3om\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3om\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3om\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3om\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3om\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3om\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3om\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3om\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3om\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3om\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3om\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3om\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3om\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3om\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3om\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3om\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3om\_ .icon-shape,#chatgpt-mermaid-\_r_3om\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3om\_ .icon-shape p,#chatgpt-mermaid-\_r_3om\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3om\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3om\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3om\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3om\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3om\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3om\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3om\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3om\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3om\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3om\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3om\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3om\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3om\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3om\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3om\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3om\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3om\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3om\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3om\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3om\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3om\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3om\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3om\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3om\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3om\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3om\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3om\_ .node rect,#chatgpt-mermaid-\_r_3om\_ .node circle,#chatgpt-mermaid-\_r_3om\_ .node ellipse,#chatgpt-mermaid-\_r_3om\_ .node polygon,#chatgpt-mermaid-\_r_3om\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3om\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3om\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3om\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3om\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3om\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ApplicationShard RouterShard AShard BShard C

Each shard stores a subset of the customer records.

```
Shard A → customers 1–10 million
Shard B → customers 10–20 million
Shard C → customers 20–30 million
```

Now the system can distribute storage and query load across multiple machines.

Sharding introduces additional complexity:

- Routing queries to the correct shard.
- Rebalancing data when shards are added.
- Handling transactions spanning multiple shards.
- Managing shard failures.
- Keeping shard metadata consistent.

### Where does consistent hashing fit?

Consistent hashing is one way to determine which shard should own a key.

```
customer:101
     |
  Hash key
     |
  Hash ring
     |
  Shard B
```

If the cluster changes, consistent hashing can reduce how many keys need to move to different shards.

But consistent hashing is not sharding itself. It is a placement and routing technique that can be used to implement sharding.

## 4. Replication: keep multiple copies of data

Partitioning and sharding distribute different pieces of data.

Replication creates additional copies of data.

Suppose Shard A contains customer records 1–10 million.

You might maintain a primary and a replica:

\#chatgpt-mermaid-\_r_3p9\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3p9\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3p9\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3p9\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3p9\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3p9\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3p9\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3p9\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3p9\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3p9\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3p9\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3p9\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3p9\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3p9\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3p9\_ p{margin:0;}#chatgpt-mermaid-\_r_3p9\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3p9\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3p9\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3p9\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3p9\_ .label text,#chatgpt-mermaid-\_r_3p9\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3p9\_ .node rect,#chatgpt-mermaid-\_r_3p9\_ .node circle,#chatgpt-mermaid-\_r_3p9\_ .node ellipse,#chatgpt-mermaid-\_r_3p9\_ .node polygon,#chatgpt-mermaid-\_r_3p9\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3p9\_ .rough-node .label text,#chatgpt-mermaid-\_r_3p9\_ .node .label text,#chatgpt-mermaid-\_r_3p9\_ .image-shape .label,#chatgpt-mermaid-\_r_3p9\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3p9\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3p9\_ .rough-node .label,#chatgpt-mermaid-\_r_3p9\_ .node .label,#chatgpt-mermaid-\_r_3p9\_ .image-shape .label,#chatgpt-mermaid-\_r_3p9\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3p9\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3p9\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3p9\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3p9\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3p9\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3p9\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3p9\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3p9\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3p9\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3p9\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3p9\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3p9\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3p9\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3p9\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3p9\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3p9\_ .icon-shape,#chatgpt-mermaid-\_r_3p9\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3p9\_ .icon-shape p,#chatgpt-mermaid-\_r_3p9\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3p9\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3p9\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3p9\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3p9\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3p9\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3p9\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3p9\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3p9\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3p9\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3p9\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3p9\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3p9\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3p9\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3p9\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3p9\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3p9\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3p9\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3p9\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3p9\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3p9\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3p9\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3p9\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3p9\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3p9\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3p9\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3p9\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3p9\_ .node rect,#chatgpt-mermaid-\_r_3p9\_ .node circle,#chatgpt-mermaid-\_r_3p9\_ .node ellipse,#chatgpt-mermaid-\_r_3p9\_ .node polygon,#chatgpt-mermaid-\_r_3p9\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3p9\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3p9\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3p9\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3p9\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3p9\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Write requestPrimary: Shard AReplica: Shard ARead requestReplicate changes

The primary accepts writes, while the replica receives copies of those changes.

Replication can improve:

- Read throughput.
- Availability.
- Disaster recovery.
- Geographic read performance, where permitted.

But replication creates consistency questions.

If a write succeeds on the primary and replication is asynchronous, a read from the replica might temporarily return old data.

Replication also consumes additional storage and network bandwidth.

Replication is not the same as partitioning: partitioning divides the dataset, while replication duplicates data.

## 5. Putting the concepts together

Consider a system with three shards.

Each shard contains a different subset of customers, and each shard has a replica.

\#chatgpt-mermaid-\_r_3pi\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3pi\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3pi\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3pi\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3pi\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3pi\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3pi\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3pi\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3pi\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3pi\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3pi\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3pi\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3pi\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3pi\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3pi\_ p{margin:0;}#chatgpt-mermaid-\_r_3pi\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3pi\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3pi\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3pi\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3pi\_ .label text,#chatgpt-mermaid-\_r_3pi\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3pi\_ .node rect,#chatgpt-mermaid-\_r_3pi\_ .node circle,#chatgpt-mermaid-\_r_3pi\_ .node ellipse,#chatgpt-mermaid-\_r_3pi\_ .node polygon,#chatgpt-mermaid-\_r_3pi\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3pi\_ .rough-node .label text,#chatgpt-mermaid-\_r_3pi\_ .node .label text,#chatgpt-mermaid-\_r_3pi\_ .image-shape .label,#chatgpt-mermaid-\_r_3pi\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3pi\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3pi\_ .rough-node .label,#chatgpt-mermaid-\_r_3pi\_ .node .label,#chatgpt-mermaid-\_r_3pi\_ .image-shape .label,#chatgpt-mermaid-\_r_3pi\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3pi\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3pi\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3pi\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3pi\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3pi\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3pi\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3pi\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3pi\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3pi\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3pi\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3pi\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3pi\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3pi\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3pi\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3pi\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3pi\_ .icon-shape,#chatgpt-mermaid-\_r_3pi\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3pi\_ .icon-shape p,#chatgpt-mermaid-\_r_3pi\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3pi\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3pi\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3pi\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3pi\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3pi\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3pi\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3pi\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3pi\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3pi\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3pi\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3pi\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3pi\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3pi\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3pi\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3pi\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3pi\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3pi\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3pi\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3pi\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3pi\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3pi\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3pi\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3pi\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3pi\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3pi\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3pi\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3pi\_ .node rect,#chatgpt-mermaid-\_r_3pi\_ .node circle,#chatgpt-mermaid-\_r_3pi\_ .node ellipse,#chatgpt-mermaid-\_r_3pi\_ .node polygon,#chatgpt-mermaid-\_r_3pi\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3pi\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3pi\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3pi\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3pi\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3pi\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ApplicationConsistent Hash RouterShard A PrimaryShard B PrimaryShard C PrimaryShard A ReplicaShard B ReplicaShard C ReplicaReplicationReplicationReplication

Each mechanism has a separate responsibility:

| Concept            | Responsibility                                      |
| ------------------ | --------------------------------------------------- |
| Partitioning       | Divide a dataset into smaller subsets               |
| Sharding           | Distribute subsets across independent storage nodes |
| Consistent hashing | Help assign keys to nodes with limited remapping    |
| Replication        | Maintain additional copies of data                  |

The combination allows you to scale horizontally while improving availability, provided the replication and recovery mechanisms are designed correctly.

## 6. Now extend this to multiple regions

Suppose your SaaS application runs in Europe and India.

Each region has several shards.

\#chatgpt-mermaid-\_r_3pr\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3pr\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3pr\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3pr\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3pr\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3pr\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3pr\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3pr\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3pr\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3pr\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3pr\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3pr\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3pr\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3pr\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3pr\_ p{margin:0;}#chatgpt-mermaid-\_r_3pr\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3pr\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3pr\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3pr\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3pr\_ .label text,#chatgpt-mermaid-\_r_3pr\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3pr\_ .node rect,#chatgpt-mermaid-\_r_3pr\_ .node circle,#chatgpt-mermaid-\_r_3pr\_ .node ellipse,#chatgpt-mermaid-\_r_3pr\_ .node polygon,#chatgpt-mermaid-\_r_3pr\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3pr\_ .rough-node .label text,#chatgpt-mermaid-\_r_3pr\_ .node .label text,#chatgpt-mermaid-\_r_3pr\_ .image-shape .label,#chatgpt-mermaid-\_r_3pr\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3pr\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3pr\_ .rough-node .label,#chatgpt-mermaid-\_r_3pr\_ .node .label,#chatgpt-mermaid-\_r_3pr\_ .image-shape .label,#chatgpt-mermaid-\_r_3pr\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3pr\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3pr\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3pr\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3pr\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3pr\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3pr\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3pr\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3pr\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3pr\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3pr\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3pr\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3pr\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3pr\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3pr\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3pr\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3pr\_ .icon-shape,#chatgpt-mermaid-\_r_3pr\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3pr\_ .icon-shape p,#chatgpt-mermaid-\_r_3pr\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3pr\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3pr\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3pr\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3pr\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3pr\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3pr\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3pr\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3pr\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3pr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3pr\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3pr\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3pr\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3pr\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3pr\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3pr\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3pr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3pr\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3pr\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3pr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3pr\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3pr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3pr\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3pr\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3pr\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3pr\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3pr\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3pr\_ .node rect,#chatgpt-mermaid-\_r_3pr\_ .node circle,#chatgpt-mermaid-\_r_3pr\_ .node ellipse,#chatgpt-mermaid-\_r_3pr\_ .node polygon,#chatgpt-mermaid-\_r_3pr\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3pr\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3pr\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3pr\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3pr\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3pr\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Global Request RouterEU ApplicationIndia ApplicationEU Hash RingIndia Hash RingEU Shard AEU Shard BIndia Shard AIndia Shard B

This design introduces an additional decision: which region is eligible to own or serve the data?

Imagine that a tenant's personal data is restricted to the EU.

You must not simply place that tenant's data on whichever server wins a global hash-ring lookup. Instead, you first select the permitted region, then select a shard within that region.

The sequence is:

1. Resolve the tenant's authoritative residency policy.
2. Identify eligible regions.
3. Select a healthy, appropriate region.
4. Use consistent hashing within that region to select a shard.
5. Apply the database's replication and consistency rules.

This is the foundation of a residency-aware multi-region architecture.

## 7. An important design choice: what exactly are you hashing?

In a production system, the hash key should match the ownership boundary of your data.

### Option A: Hash by customer ID

```
hash(customerId) → shard
```

Useful when customers can be independently distributed.

### Option B: Hash by tenant ID

```
hash(tenantId) → shard
```

Useful when most operations are tenant-scoped and you want a tenant's records colocated.

The downside is that a very large tenant can become a hotspot.

### Option C: Hash by tenant ID and entity ID

```
hash(tenantId + ":" + entityId) → shard
```

Useful when you need to distribute a large tenant's data across several shards.

However, queries that need all entities for a tenant may now span multiple shards.

The partition key is an application data-model decision, not merely a hashing decision.

You should choose it based on query patterns, transaction boundaries, tenant isolation, workload distribution, and residency requirements.

## 8. Interview scenario

Question: You have 100 million customer records, 10 database shards, and three geographic regions. How would you distribute the data and handle failures?

A strong answer would be:

> First, I would establish the data ownership and residency requirements. I would then assign each tenant or partition to a permitted home region. Within that region, I would use a partitioning strategy and potentially consistent hashing to distribute keys across database shards.
>
> I would design replication separately, selecting synchronous or asynchronous replication based on consistency and recovery requirements. If a shard fails, its replica or recovery mechanism would restore service. If an entire region fails, failover would be permitted only to an approved recovery region, with explicit handling for write ownership, replication lag, and split-brain prevention.
>
> Finally, I would test rebalancing, failure recovery, and residency enforcement rather than assuming consistent hashing solves those problems automatically.

That answer demonstrates that you understand the responsibilities of the different mechanisms instead of treating consistent hashing as a complete distributed database solution.

## 9. Check your understanding

Before moving forward, make sure you can answer:

1. Can a database be partitioned without being sharded?
2. Why is replication different from sharding?
3. What role does consistent hashing play in a sharded system?
4. Why might hashing by tenant ID create a hotspot?
5. Why must region eligibility be established before selecting a shard?
6. Why does having a replica not automatically guarantee strong consistency?

## Next lesson

We'll finish Chapter 1 by examining the limitations and failure modes of consistent hashing: collisions, ring membership disagreements, stale routing metadata, rebalancing, hot partitions, and why adding a node can still cause production incidents.

Type `next` to continue.









# Chapter 1: Foundations of Consistent Hashing

## Lesson 6: Limitations, Failure Modes, and Production Pitfalls

So far, we've learned how consistent hashing assigns keys to nodes and reduces the amount of data that needs to move when the cluster changes.

But here's the important distinction:

Consistent hashing solves a placement problem. It does not automatically solve a distributed-systems problem.

A system can use consistent hashing correctly and still experience outages, inconsistent writes, overloaded nodes, or data loss.

Let's understand why.

## 1. Hash collisions

A hash function maps an input to a finite range of possible outputs. Therefore, different inputs can sometimes produce the same hash.

For example:

```
hash("server-A") = 120
hash("server-B") = 120
```

Both servers occupy the same position on the ring.

### Why is this a problem?

Imagine a ring implementation that stores positions in a map:

```
TreeMap<Long, String> ring = new TreeMap<>();

ring.put(120L, "server-A");
ring.put(120L, "server-B");
```

The second insertion overwrites the first because a map cannot store two values under the same key.

Your ring now silently loses one server position.

### How do production systems handle this?

There are two common kinds of collisions to consider:

- Hash collisions between physical nodes or virtual nodes: Two nodes produce the same ring position.
- Hash collisions between data keys: Two different keys produce the same hash value.

Practical strategies include:

1. Use a hash function with a sufficiently large output space.
2. Make ring positions unique by including a deterministic tie-breaker, such as the node ID and virtual-node index.
3. Represent ring positions so that collisions can be stored explicitly rather than overwritten.
4. Ensure all application instances use the same hashing algorithm and input encoding.

For example, instead of treating a ring position as only a number, use a composite ordering:

```
(hash_value, node_id, virtual_node_index)
```

The ring compares the hash first, then the other fields to establish a deterministic order.

The key lesson: a strong hash reduces collision probability, but your data structure must still define collision behavior.

## 2. Inconsistent ring membership

This is one of the most dangerous operational problems.

Suppose a service has three nodes:

```
Node A
Node B
Node C
```

Two application instances maintain their own copies of the ring.

Then a new node, `Node D`, is added.

During a deployment, one application instance learns about the new node before the other.

\#chatgpt-mermaid-\_r_3s8\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3s8\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3s8\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3s8\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3s8\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3s8\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3s8\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3s8\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3s8\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3s8\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3s8\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3s8\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3s8\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3s8\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3s8\_ p{margin:0;}#chatgpt-mermaid-\_r_3s8\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3s8\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3s8\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3s8\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3s8\_ .label text,#chatgpt-mermaid-\_r_3s8\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3s8\_ .node rect,#chatgpt-mermaid-\_r_3s8\_ .node circle,#chatgpt-mermaid-\_r_3s8\_ .node ellipse,#chatgpt-mermaid-\_r_3s8\_ .node polygon,#chatgpt-mermaid-\_r_3s8\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3s8\_ .rough-node .label text,#chatgpt-mermaid-\_r_3s8\_ .node .label text,#chatgpt-mermaid-\_r_3s8\_ .image-shape .label,#chatgpt-mermaid-\_r_3s8\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3s8\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3s8\_ .rough-node .label,#chatgpt-mermaid-\_r_3s8\_ .node .label,#chatgpt-mermaid-\_r_3s8\_ .image-shape .label,#chatgpt-mermaid-\_r_3s8\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3s8\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3s8\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3s8\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3s8\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3s8\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3s8\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3s8\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3s8\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3s8\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3s8\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3s8\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3s8\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3s8\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3s8\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3s8\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3s8\_ .icon-shape,#chatgpt-mermaid-\_r_3s8\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3s8\_ .icon-shape p,#chatgpt-mermaid-\_r_3s8\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3s8\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3s8\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3s8\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3s8\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3s8\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3s8\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3s8\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3s8\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3s8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3s8\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3s8\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3s8\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3s8\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3s8\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3s8\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3s8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3s8\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3s8\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3s8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3s8\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3s8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3s8\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3s8\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3s8\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3s8\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3s8\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3s8\_ .node rect,#chatgpt-mermaid-\_r_3s8\_ .node circle,#chatgpt-mermaid-\_r_3s8\_ .node ellipse,#chatgpt-mermaid-\_r_3s8\_ .node polygon,#chatgpt-mermaid-\_r_3s8\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3s8\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3s8\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3s8\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3s8\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3s8\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Request for customer-123Application Instance 1Ring: A, B, C, DApplication Instance 2Ring: A, B, CRoutes key to Node DRoutes key to Node B

Both instances may be following the consistent-hashing algorithm correctly. Yet they disagree about where the same key belongs.

### What can happen?

Imagine the request is:

```
Update customer-123's account balance
```

Instance 1 routes it to Node D, while Instance 2 routes it to Node B.

Possible consequences include:

- The same logical data is written to different nodes.
- Reads return different versions of the data.
- A write appears to disappear because subsequent reads go to another node.
- Retries duplicate operations if idempotency is not implemented correctly.

This is not a hash-function problem. It is a cluster-membership coordination problem.

### How do we prevent it?

Treat ring membership as versioned configuration.

For example:

```
Ring configuration version: 42
Nodes: A, B, C, D
```

Every application instance should know which ring configuration it is using.

A simplified routing request might include:

```
key: customer-123
ring_version: 42
```

The storage service can then reject or redirect requests that use obsolete routing information.

A production design may use a control plane that publishes membership changes, with mechanisms such as:

- Versioned and immutable ring configurations.
- Atomic replacement of the active ring configuration.
- Monitoring of which configuration version each application instance uses.
- Explicit readiness checks before new membership becomes active.
- Compatibility handling during configuration transitions.

A configuration version alone is not sufficient. The system must also define what happens to requests and data during a transition.

Important: Ring membership updates should not be treated as independent local decisions made by each application instance.

## 3. Stale routing metadata

Even when all instances eventually agree on the ring, their routing information may be temporarily stale.

Suppose the ring changes from version 42 to version 43:

```
Version 42:
customer-123 → Node B

Version 43:
customer-123 → Node D
```

An application instance still using version 42 sends a request to Node B.

But what if the data has already moved to Node D?

New Node DOld Node BApplicationNew Node DOld Node BApplication#chatgpt-mermaid-\_r_3t8\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3t8\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3t8\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3t8\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3t8\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3t8\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3t8\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3t8\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3t8\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3t8\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3t8\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3t8\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3t8\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3t8\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3t8\_ p{margin:0;}#chatgpt-mermaid-\_r_3t8\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_3t8\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3t8\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3t8\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3t8\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3t8\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_3t8\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3t8\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3t8\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3t8\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_3t8\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3t8\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3t8\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3t8\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_3t8\_ .labelText,#chatgpt-mermaid-\_r_3t8\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3t8\_ .loopText,#chatgpt-mermaid-\_r_3t8\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3t8\_ .sectionTitle,#chatgpt-mermaid-\_r_3t8\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3t8\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_3t8\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3t8\_ .noteText,#chatgpt-mermaid-\_r_3t8\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_3t8\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_3t8\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_3t8\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_3t8\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_3t8\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_3t8\_ .actor-man circle,#chatgpt-mermaid-\_r_3t8\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_3t8\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3t8\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3t8\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3t8\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3t8\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3t8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3t8\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3t8\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3t8\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3t8\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3t8\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3t8\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3t8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3t8\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3t8\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3t8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3t8\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3t8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3t8\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Ring changes from v42 to v43Write customer-123 using v42Reject stale ownership or redirectRetry using v43Write acknowledged

This sequence illustrates one possible protocol, not a behavior that consistent hashing provides automatically.

### Possible strategies

Strategy A: Reject stale requests

The old node detects that it no longer owns the key and rejects the request. The application refreshes its routing metadata and retries.

This is relatively simple, but retries must be safe.

Strategy B: Forward requests

The old node forwards requests to the new owner.

This can smooth transitions, but forwarding loops, extra latency, and duplicate operations must be prevented.

Strategy C: Support a migration period

The old and new owners coordinate while the data moves. Reads and writes follow an explicit migration protocol until ownership is finalized.

This is more complex but often necessary for stateful databases.

### Why not simply retry everything?

Consider a payment request:

1. The application sends a payment command to Node B.
2. Node B processes it, but its response is lost.
3. The application retries against Node D.
4. Node D processes it again.

If the operation isn't idempotent or deduplicated, the customer might be charged twice.

Routing retries therefore need to work with idempotency keys, operation tracking, and well-defined ownership transitions.

## 4. Adding a node does not move data automatically

This is a very common misunderstanding.

Consistent hashing determines the node that should own a key under a particular ring configuration. It does not physically transfer the key's data.

Suppose a cluster has:

```
Node A: 1 TB
Node B: 1 TB
Node C: 1 TB
```

You add Node D.

The new ring assigns some keys to Node D, but Node D initially has no data for those keys.

If the application immediately starts routing all of those requests to Node D, it might receive missing-record errors or incorrectly treat existing records as new.

### A safer migration process

\#chatgpt-mermaid-\_r_3tm\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3tm\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3tm\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3tm\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3tm\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3tm\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3tm\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3tm\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3tm\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3tm\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3tm\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3tm\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3tm\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3tm\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3tm\_ p{margin:0;}#chatgpt-mermaid-\_r_3tm\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3tm\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3tm\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3tm\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3tm\_ .label text,#chatgpt-mermaid-\_r_3tm\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3tm\_ .node rect,#chatgpt-mermaid-\_r_3tm\_ .node circle,#chatgpt-mermaid-\_r_3tm\_ .node ellipse,#chatgpt-mermaid-\_r_3tm\_ .node polygon,#chatgpt-mermaid-\_r_3tm\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3tm\_ .rough-node .label text,#chatgpt-mermaid-\_r_3tm\_ .node .label text,#chatgpt-mermaid-\_r_3tm\_ .image-shape .label,#chatgpt-mermaid-\_r_3tm\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3tm\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3tm\_ .rough-node .label,#chatgpt-mermaid-\_r_3tm\_ .node .label,#chatgpt-mermaid-\_r_3tm\_ .image-shape .label,#chatgpt-mermaid-\_r_3tm\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3tm\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3tm\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3tm\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3tm\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3tm\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3tm\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3tm\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3tm\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3tm\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3tm\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3tm\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3tm\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3tm\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3tm\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3tm\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3tm\_ .icon-shape,#chatgpt-mermaid-\_r_3tm\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3tm\_ .icon-shape p,#chatgpt-mermaid-\_r_3tm\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3tm\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3tm\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3tm\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3tm\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3tm\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3tm\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3tm\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3tm\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3tm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3tm\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3tm\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3tm\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3tm\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3tm\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3tm\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3tm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3tm\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3tm\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3tm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3tm\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3tm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3tm\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3tm\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3tm\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3tm\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3tm\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3tm\_ .node rect,#chatgpt-mermaid-\_r_3tm\_ .node circle,#chatgpt-mermaid-\_r_3tm\_ .node ellipse,#chatgpt-mermaid-\_r_3tm\_ .node polygon,#chatgpt-mermaid-\_r_3tm\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3tm\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3tm\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3tm\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3tm\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3tm\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}1. Add Node D to proposedring2. Calculate keys that willmove3. Copy existing data to NodeD4. Verify copied data andtrack changes5. Coordinate reads andwrites during migration6. Switch ownership to thenew ring7. Retire old copies aftersafety checks

Each step requires concrete implementation decisions.

For example:

- How do you enumerate the records that belong to the moving ranges?
- How do you capture writes that happen while the initial copy is running?
- How do you prevent an old owner from accepting writes after ownership changes?
- How do you roll back if migration fails?
- When is it safe to delete the old copy?

A database may provide native partition movement and replication mechanisms. An application-managed key-value store may need its own migration coordinator.

The essential distinction is:

- Placement change: The ring says a different node should own the key.
- Data migration: The data is physically copied to the new node.
- Ownership cutover: The system safely changes which node can accept authoritative operations.

These are separate operations.

## 5. Hot keys and hot partitions

Consistent hashing can distribute keys evenly while the workload remains uneven.

Imagine a social media application:

```
Key: post:popular-celebrity
```

This single post receives millions of reads per minute.

All requests for that key may route to the same owner.

\#chatgpt-mermaid-\_r_3u4\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3u4\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3u4\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3u4\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3u4\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u4\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3u4\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3u4\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3u4\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3u4\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3u4\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3u4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3u4\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3u4\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3u4\_ p{margin:0;}#chatgpt-mermaid-\_r_3u4\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u4\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u4\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u4\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3u4\_ .label text,#chatgpt-mermaid-\_r_3u4\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u4\_ .node rect,#chatgpt-mermaid-\_r_3u4\_ .node circle,#chatgpt-mermaid-\_r_3u4\_ .node ellipse,#chatgpt-mermaid-\_r_3u4\_ .node polygon,#chatgpt-mermaid-\_r_3u4\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3u4\_ .rough-node .label text,#chatgpt-mermaid-\_r_3u4\_ .node .label text,#chatgpt-mermaid-\_r_3u4\_ .image-shape .label,#chatgpt-mermaid-\_r_3u4\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3u4\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3u4\_ .rough-node .label,#chatgpt-mermaid-\_r_3u4\_ .node .label,#chatgpt-mermaid-\_r_3u4\_ .image-shape .label,#chatgpt-mermaid-\_r_3u4\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3u4\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3u4\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3u4\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3u4\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3u4\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3u4\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3u4\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3u4\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3u4\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3u4\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3u4\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u4\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u4\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3u4\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u4\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3u4\_ .icon-shape,#chatgpt-mermaid-\_r_3u4\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3u4\_ .icon-shape p,#chatgpt-mermaid-\_r_3u4\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3u4\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3u4\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3u4\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3u4\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3u4\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3u4\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3u4\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3u4\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3u4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3u4\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3u4\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3u4\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3u4\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3u4\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3u4\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3u4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3u4\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3u4\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3u4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3u4\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3u4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3u4\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3u4\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3u4\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3u4\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3u4\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3u4\_ .node rect,#chatgpt-mermaid-\_r_3u4\_ .node circle,#chatgpt-mermaid-\_r_3u4\_ .node ellipse,#chatgpt-mermaid-\_r_3u4\_ .node polygon,#chatgpt-mermaid-\_r_3u4\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3u4\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3u4\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3u4\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3u4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3u4\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Millions of usersPopular post keyNode BHigh CPU, network, andlatency

Even if Node B owns only 5% of the keys, it might handle 60% of the traffic.

Equal key distribution does not mean equal traffic distribution.

### Hot partitions versus hot keys

These are related but different problems.

| Problem       | Meaning                                              | Possible solution                                            |
| ------------- | ---------------------------------------------------- | ------------------------------------------------------------ |
| Hot key       | One key receives excessive traffic                   | Cache, replicate reads, split the logical key where possible |
| Hot partition | Many busy keys belong to the same partition          | Repartition, redistribute ranges, adjust weights             |
| Hot tenant    | One customer generates much more traffic than others | Tenant isolation, tenant-specific partitioning, quotas       |
| Hot node      | One node has excessive overall load                  | Rebalance ownership, scale capacity, change placement        |

### Why adding more nodes might not help

Suppose a single key is assigned to Node B.

Adding Node E to the ring may move some other keys, but the hot key might remain on Node B.

Even if the hot key moves to Node E, its workload still concentrates on one node.

For read-heavy workloads, you might replicate the data and distribute reads among replicas.

For workloads that can be decomposed, you might split a logical key into smaller independently processed keys. For example, instead of maintaining one counter:

```
likes:post-123
```

you could maintain several counters:

```
likes:post-123:shard-0
likes:post-123:shard-1
likes:post-123:shard-2
...
```

The total is calculated by combining the shards.

This approach adds complexity: aggregation, consistency, and update coordination must be handled.

For a single strongly consistent mutable record, splitting requests across replicas is much harder because concurrent writes need coordination.

Rule of thumb: Use consistent hashing to distribute ownership; use caching, replication, partitioning, and workload-aware design to address traffic hotspots.

## 6. Node failure and unsafe failover

Consider a three-node cluster:

```
Node A
Node B
Node C
```

Node B becomes unreachable.

A naive implementation removes B from the ring and routes its keys to the next eligible node.

That may be enough for a stateless computation. It is not necessarily safe for a database.

### The split-brain problem

Suppose Node B is not actually dead. It has merely lost network connectivity to the cluster coordinator.

The coordinator removes B and assigns its keys to Node C.

But Node B is still running and accepting writes from clients that have stale routing information.

\#chatgpt-mermaid-\_r_3us\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3us\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3us\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3us\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3us\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3us\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3us\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3us\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3us\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3us\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3us\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3us\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3us\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3us\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3us\_ p{margin:0;}#chatgpt-mermaid-\_r_3us\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3us\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3us\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3us\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3us\_ .label text,#chatgpt-mermaid-\_r_3us\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3us\_ .node rect,#chatgpt-mermaid-\_r_3us\_ .node circle,#chatgpt-mermaid-\_r_3us\_ .node ellipse,#chatgpt-mermaid-\_r_3us\_ .node polygon,#chatgpt-mermaid-\_r_3us\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3us\_ .rough-node .label text,#chatgpt-mermaid-\_r_3us\_ .node .label text,#chatgpt-mermaid-\_r_3us\_ .image-shape .label,#chatgpt-mermaid-\_r_3us\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3us\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3us\_ .rough-node .label,#chatgpt-mermaid-\_r_3us\_ .node .label,#chatgpt-mermaid-\_r_3us\_ .image-shape .label,#chatgpt-mermaid-\_r_3us\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3us\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3us\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3us\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3us\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3us\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3us\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3us\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3us\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3us\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3us\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3us\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3us\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3us\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3us\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3us\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3us\_ .icon-shape,#chatgpt-mermaid-\_r_3us\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3us\_ .icon-shape p,#chatgpt-mermaid-\_r_3us\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3us\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3us\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3us\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3us\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3us\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3us\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3us\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3us\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3us\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3us\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3us\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3us\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3us\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3us\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3us\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3us\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3us\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3us\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3us\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3us\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3us\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3us\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3us\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3us\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3us\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3us\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3us\_ .node rect,#chatgpt-mermaid-\_r_3us\_ .node circle,#chatgpt-mermaid-\_r_3us\_ .node ellipse,#chatgpt-mermaid-\_r_3us\_ .node polygon,#chatgpt-mermaid-\_r_3us\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3us\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3us\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3us\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3us\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3us\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Network partitionNode B still accepts writesNode C becomes new ownerWrite version XWrite version YConflicting authoritativestate

Now both nodes may accept conflicting writes for the same logical data.

This is called a split-brain scenario.

### How can we prevent it?

For systems that require a single authoritative writer, routing changes should be combined with an ownership protocol.

Possible mechanisms include:

- Leases: An owner may act only while its lease remains valid under the system's timing and coordination assumptions.
- Fencing tokens: Every ownership grant receives a monotonically increasing token. The storage layer rejects writes carrying older tokens.
- Quorum-based coordination: Ownership or write decisions require sufficient agreement from participating nodes.
- Consensus-backed metadata: A coordination system maintains authoritative membership and ownership information.

A fencing-token example:

```
Node B's ownership token = 41
Node C's ownership token = 42
```

After ownership moves to Node C, the storage system rejects writes from Node B carrying token 41.

Crucially, the storage layer must enforce the fencing rule. Merely attaching a token to a request without validating it does not prevent stale writes.

Consistent hashing can tell you where to route a key after a failure. It cannot, by itself, establish which node has the right to accept authoritative writes.

## 7. Virtual nodes are not free

Virtual nodes improve distribution by assigning multiple positions on the ring to each physical node.

For example:

```
Node A → 100 virtual nodes
Node B → 100 virtual nodes
Node C → 100 virtual nodes
```

This creates 300 logical positions on the ring.

But increasing the virtual-node count has tradeoffs:

- More ring metadata must be stored and distributed.
- Ring construction and membership updates require more work.
- More ownership ranges can complicate migration.
- More metadata may need to be tracked for diagnostics and rebalancing.

The right number depends on the number of physical nodes, workload, hash distribution, implementation, and operational requirements.

For heterogeneous hardware, consider weighted placement: a node with twice the effective capacity may receive more virtual nodes or a larger share of ownership.

However, capacity-based weighting alone does not account for every workload difference. A node serving CPU-heavy queries may need different placement from one serving lightweight reads.

## 8. Non-deterministic hashing and key encoding

Every application instance that calculates the owner for a key must calculate the same result from the same ring configuration.

Suppose one instance hashes:

```
customer-123
```

Another hashes a differently encoded representation of that identifier. Even if both use the same hash algorithm, the outputs may differ.

Other sources of disagreement include:

- Different character encodings.
- Different key normalization rules.
- Different hash algorithms or versions.
- Different serialization formats.
- Accidental use of process-randomized hash functions.
- Different virtual-node naming conventions.

For example, these are distinct byte sequences:

```
"Customer-123"
"customer-123"
```

If the application treats identifiers as case-insensitive but hashes them without normalization, logically identical identifiers could route differently.

### Production rule

Define the routing key as a stable, canonical representation.

```
logical identifier
       ↓
canonical encoding
       ↓
versioned hash algorithm
       ↓
ring lookup using a specific membership version
       ↓
owner
```

If you change the hash algorithm, key encoding, or virtual-node naming scheme, treat it as a potentially disruptive placement migration—not as a harmless code refactor.

## 9. Putting the failure modes together

Imagine you're designing a multi-region application with PostgreSQL, Redis, and application services.

A customer requests an update to their account.

A robust routing process might look like this:

\#chatgpt-mermaid-\_r_3vu\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3vu\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3vu\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3vu\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3vu\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vu\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3vu\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3vu\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3vu\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3vu\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3vu\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3vu\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3vu\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3vu\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3vu\_ p{margin:0;}#chatgpt-mermaid-\_r_3vu\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vu\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vu\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vu\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3vu\_ .label text,#chatgpt-mermaid-\_r_3vu\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vu\_ .node rect,#chatgpt-mermaid-\_r_3vu\_ .node circle,#chatgpt-mermaid-\_r_3vu\_ .node ellipse,#chatgpt-mermaid-\_r_3vu\_ .node polygon,#chatgpt-mermaid-\_r_3vu\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3vu\_ .rough-node .label text,#chatgpt-mermaid-\_r_3vu\_ .node .label text,#chatgpt-mermaid-\_r_3vu\_ .image-shape .label,#chatgpt-mermaid-\_r_3vu\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3vu\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3vu\_ .rough-node .label,#chatgpt-mermaid-\_r_3vu\_ .node .label,#chatgpt-mermaid-\_r_3vu\_ .image-shape .label,#chatgpt-mermaid-\_r_3vu\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3vu\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3vu\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3vu\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3vu\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3vu\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3vu\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3vu\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3vu\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3vu\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3vu\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3vu\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vu\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vu\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3vu\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vu\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3vu\_ .icon-shape,#chatgpt-mermaid-\_r_3vu\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3vu\_ .icon-shape p,#chatgpt-mermaid-\_r_3vu\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3vu\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3vu\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3vu\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3vu\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3vu\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3vu\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3vu\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3vu\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3vu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3vu\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3vu\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3vu\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3vu\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3vu\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3vu\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3vu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3vu\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3vu\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3vu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3vu\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3vu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3vu\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3vu\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3vu\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3vu\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3vu\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3vu\_ .node rect,#chatgpt-mermaid-\_r_3vu\_ .node circle,#chatgpt-mermaid-\_r_3vu\_ .node ellipse,#chatgpt-mermaid-\_r_3vu\_ .node polygon,#chatgpt-mermaid-\_r_3vu\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3vu\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3vu\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3vu\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3vu\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3vu\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Receive requestResolve tenant and residencypolicySelect an eligible regionLoad current routingconfigurationConsistent-hash lookupwithin regionValidate ownership andfencing tokenIs this node the valid owner?Execute idempotentoperationRefresh routing or safelyredirectReturn resultYesNo

Notice the responsibilities:

1. Residency policy decides where the data is allowed to live.
2. Regional routing selects an eligible region.
3. Consistent hashing selects a node within that region.
4. Ownership validation ensures the selected node is authorized to handle the operation.
5. Idempotency and storage consistency protect the operation's correctness.
6. Replication and disaster recovery provide availability and recovery capabilities.

Consistent hashing handles only one part of this flow.

It also does not automatically make a multi-region design compliant with GDPR or other data-residency requirements. Those policies must be enforced separately.

## 10. Interview-ready summary

If an interviewer asks, “What are the limitations of consistent hashing?”, a strong answer is:

Consistent hashing reduces key remapping when the cluster membership changes, but it does not guarantee perfectly balanced traffic, migrate data automatically, coordinate cluster membership, or provide replication and consistency. Production systems must handle hash collisions, stale routing metadata, membership disagreement, hot keys, data migration, and node failures. During failover, they also need an ownership protocol to prevent split-brain writes. In multi-region systems, region eligibility and residency rules must be enforced before hashing keys within the selected region.

Remember these five distinctions:

- Consistent hashing → where a key should be placed.
- Rebalancing → how ownership is redistributed.
- Migration → how existing data is moved.
- Replication and consistency → how copies are maintained and coordinated.
- Failure detection and ownership coordination → which node is allowed to act as the authoritative owner.

## Chapter 1 complete

You now understand the core algorithm, virtual nodes, alternative placement strategies, partitioning, sharding, and the operational pitfalls.

Next: Chapter 2 — Implementing Consistent Hashing.

We'll build the algorithm step by step, covering the ring data structure, virtual nodes, key lookup, adding and removing nodes, complexity analysis, and a more complete Java implementation. We'll also examine the design decisions you'd need to make before using it in production.

Type `next` to continue.


