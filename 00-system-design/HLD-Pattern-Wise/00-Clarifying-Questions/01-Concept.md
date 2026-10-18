# Clarifying Questions for HLD Interviews: The Pressure-Proof Guide

A merged and expanded guide. Simple English, short paragraphs, built to be used when your mind goes blank.

---

## 0. The 30-second version (read this if you read nothing else)

**The rule:** Ask a question only if a different answer would make you draw a different design.

**The exception:** Always ask about correctness (what must never go wrong), even if the design looks the same either way. It decides if your design is *safe*.

**The panic routine:** If you freeze, ask these five. They work for almost every problem.

1. What are the 2-3 core features? (scope)
2. What must never go wrong? (invariant)
3. Read-heavy or write-heavy? (access pattern)
4. What scale? Users, peak requests per second, data size. (load)
5. How fresh must data be? Is a little staleness OK? (consistency)

**The exit:** Say your assumptions out loud, write them on the board, and start designing.

---

## 1. Why interviewers want clarifying questions

The interviewer gives you a vague prompt on purpose. “Design a notification system” could be a small internal tool or a system that sends a billion messages a day.

They are not testing whether you know the “right” questions. They are checking three things:

- **Judgment:** Can you find the few facts that matter?
- **Ownership:** Do you scope the problem, or wait to be told what to do?
- **Trade-off thinking:** Do you know *why* an answer changes the design?

A candidate who asks nothing looks like they will solve the wrong problem. A candidate who asks fifteen questions looks like they are stalling. The sweet spot is **4 to 7 sharp questions**, each tied to a design decision.

---

## 2. The one rule: the architecture-changing test

Before you ask anything, finish this sentence in your head:

> “If the answer is A, I will draw X. If the answer is B, I will draw Y.”

If X and Y are the same, do not ask. State an assumption instead.

**Example that passes the test:** “Can notifications be delayed by a few seconds?”

- Yes: use a queue, workers, retries. Simple and cheap.
- No: you need a direct, low-latency path and a very different design.

**Example that fails the test:** “Should the button be blue?” Both answers give the same architecture. Skip it.

### The exception: correctness questions

Some questions do not change your database choice, but they decide if your system is *safe*.

Example: “Can a payment run twice?” You may use the same database either way. But if the answer is “never,” you must add idempotency keys. If you never ask, you may ship a design that double-charges.

So: **ask correctness questions first, even when the architecture looks the same.**

### The five-question filter

Run every candidate question through these checks:

| # | Check | If yes |
| --- | --- | --- |
| 1 | Does it change the architecture? | Ask it |
| 2 | Does it change a correctness guarantee? | Ask it first |
| 3 | Does it change scale by 10x or more? | Ask it |
| 4 | Does it expose a hard constraint (region, compliance, existing system)? | Ask it |
| 5 | Can I make a reasonable assumption instead? | Assume it and move on |

---

## 3. The framework: S.C.A.L.E.D.

Six buckets, always in this order. Take 1-2 questions from each bucket. Skip a bucket if it does not apply. This is a thinking checklist, not a script to recite.

**Memory trick:** **S**cope it, protect **C**orrectness, understand **A**ccess, size the **L**oad, respect the **E**nvironment, plan for **D**egradation.

### S: Scope

**Why first:** If you skip scope, you will design everything and finish nothing.

**Ask:**

- “What are the 2-3 core features for this session?”
- “Who are the users? Are there different roles?”
- “What is out of scope?”

**What it decides:** Which components exist at all.

**Example:** For a ticket booking system, say: “I will focus on browse, seat selection, payment, and ticket. I will leave recommendations and reviews out. OK?”

### C: Correctness

**Why second:** This is the part that can sink your design. Find the invariant early.

**Ask:**

- “What is the one thing that must never go wrong?”
- “Can data be duplicated, reordered, or stale?”
- “Do we need atomic multi-record operations or unique constraints?”

**What it decides:** SQL vs NoSQL, locks, idempotency, delivery guarantees, sync vs async.

**Examples of invariants:**

- Booking: one seat is never sold twice.
- Payments: never charge twice, never lose a payment.
- Chat: a message is never lost after it is acknowledged.
- Notifications: a critical alert is never silently dropped.
- Auth/data: private data never leaks to another user.

### A: Access pattern

**Why:** The shape of traffic decides caching, replicas, and storage.

**Ask:**

- “Is this read-heavy or write-heavy? Roughly what ratio?”
- “Are there hot keys, huge tenants, or skewed users (celebrities)?”
- “Who reads what, and how often?”

**What it decides:** Cache layers, read replicas, fan-out strategy, indexing, storage engine.

**Quick map:**

- Read-heavy: cache, replicas, CDN, precompute.
- Write-heavy: queues, batching, log-structured storage, partitioning.
- Skewed: special handling for hot keys, hybrid fan-out.

### L: Load and latency

**Why:** Numbers tell you if one machine is enough.

**Ask:**

- “How many daily users and peak requests per second?”
- “How much data per day, and for how long do we keep it?”
- “What response time do users expect (p95/p99)?”
- “What is the peak-to-average ratio?”

**What it decides:** Single DB vs sharding, queues, CDN, replication, sync vs async.

**Tip:** Propose numbers. Do not ask “how many users?” Say “I will assume 10 million daily users. Fair?”

### E: Environment

**Why:** Some designs are great but not allowed.

**Ask:**

- “Single region or global? Mobile or web?”
- “Any existing systems or providers we must use?”
- “Any compliance, data residency, or budget limits?”

**What it decides:** Which valid designs are actually feasible.

### D: Degradation and lifecycle

**Why:** Every system fails. Interviewers love hearing how yours fails *gracefully*.

**Ask:**

- “If a dependency is down, what is acceptable? Fall back, delay, or fail?”
- “Can work be delayed or retried? Can the user see a ‘pending’ state?”
- “What availability target do we need? Can we lose any data? (RPO/RTO)”
- “How are deletes, retries, and recovery handled?”

**What it decides:** Fallbacks, retries, idempotency, replication, recovery plan.

---

## 4. Priorities: P0, P1, P2

Not all questions are equal. Use three levels so you do not spend ten minutes interrogating.

| Level | Meaning | Examples |
| --- | --- | --- |
| **P0: must ask** | Could invalidate your design or break a critical invariant | Can a seat be sold twice? Can a payment be charged twice? Must a message be delivered, or is best-effort fine? Must users see their own writes at once? |
| **P1: ask if relevant** | Changes scale, latency, availability, or cost a lot | Peak vs average traffic, latency target, read/write ratio, can work be async, multi-region needed? |
| **P2: assume or defer** | Matters only after the core design exists | UI layout, field names, exact retention, infra tuning |

Priorities depend on the problem. Retention is P2 for a URL shortener but P0 if there is a legal deletion rule.

**Order of asking:** all P0 first, then the P1s that matter, then stop.

---

## 5. Think in design forks

A fork is a big either-or in your architecture. If you can name the forks, you know which question decides each one.

| Fork | The deciding question |
| --- | --- |
| SQL vs NoSQL | Do we need transactions and relations, or key lookups at huge scale? |
| Sync vs async | Does the user need the result right now? |
| Cache or not | How read-heavy is it, and is staleness OK? |
| Push vs pull (fan-out) | How many recipients per event? Is it skewed? |
| Strong vs eventual consistency | What breaks if two users briefly see different data? |
| Single vs multi-region | What are the availability and latency goals? |
| Exactly-once vs at-least-once | Is a duplicate harmful, or can the consumer be idempotent? |
| Batch vs stream | How soon must results appear? |

### Worked fork example: file sharing

| Unknown | Answer A | Answer B | Design impact |
| --- | --- | --- | --- |
| File size | Small files | Multi-GB files | Simple upload vs multipart upload to object storage |
| Access | Private files | Public, heavy downloads | Authorization vs CDN and caching |
| Visibility | Short delay OK | Must be visible instantly | Read-after-write strategy |
| Durability | Temporary files | Never lose after ack | Replication and backups |
| Sharing | Owner only | Public links, revocable | Token design, cache invalidation |

Notice that every row is a question that *changes a decision*. That is the pattern to copy.

---

## 6. Pattern-specific killer questions

After the core questions, add 1-3 from the row that matches your problem. Learn the decision behind each, not the wording.

| Pattern | Killer questions | What they drive |
| --- | --- | --- |
| **Booking / ticketing** | Can two users compete for one item? Is there a temporary hold, and for how long? What if payment fails? How hot is one item (flash sale)? | Atomic reservation, hold expiry, idempotency, waiting room |
| **Feed / read-heavy** | Max followers per author? Ranked or chronological? How stale is OK? | Fan-out on write, read, or hybrid; ranking; caching |
| **Write-heavy ingestion** | Can we lose events? Real-time or batch results? How long do we keep data? | Log-based pipeline, batching, storage tiers |
| **Async jobs** | How long can a job run? Need progress or notification? Safe to run twice? | Queues, leases, idempotency, job state machine |
| **Payments** | Are we the system of record or using a provider? Refunds? Multi-currency? How are retries and reconciliation handled? | Ledger, idempotency keys, state machine, webhooks |
| **Search** | Typo tolerance? Filters and facets? How soon must new items be searchable? | Index pipeline, search engine, freshness |
| **Chat / real-time** | 1:1 or groups (max size)? Durable history? Ordering? Offline delivery? Multi-device? | Connection layer, persistence, partitioning, delivery semantics |
| **Notifications** | Which channels? User preferences? Retries? Are duplicates OK? | Queues, provider integration, dedup |
| **Files / media** | Max size? Streaming or transcoding? Public or private? | Object storage, multipart, CDN, authorization |
| **Geospatial** | How often do locations update? Search radius? One-to-one or one-to-many matching? | Geo index (geohash, quadtree), write rate |
| **Rate limiter** | Per user, IP, key, or tenant? Hard or soft limit? Globally strict? Fail-open or fail-closed? | Counter strategy, coordination |
| **Scheduler** | How precise (seconds or minutes)? Exactly once? Recurring? What if a worker dies mid-job? | Leases, fencing, retries |
| **Multi-tenant** | How different are tenant sizes? Isolation or compliance needs? Noisy neighbors? | Partitioning, quotas, isolation |
| **Analytics** | Real-time or batch? Is approximate OK? Late-arriving data? | Stream vs batch, windows, dedup |
| **URL shortener** | Redirect vs create ratio? Custom aliases unique? Expiry? | Read optimization, uniqueness, cache |
| **Key-value store** | Which consistency level? Which failures to tolerate? Workload mix? | Partitioning, quorum, conflict resolution |
| **Config / lookup** | Read latency target? How fast must changes spread? Safe default if store is down? | Caching, push vs poll, fallback |
| **Recommendation** | How personal? How fresh? Latency and quality targets? | Candidate generation, ranking, feature serving |

---

## 7. How to ask (delivery under pressure)

Good questions asked badly still look weak. These habits fix that.

### Habit 1: Propose a default, then confirm

- Weak: “How many users do we have?”
- Strong: “I will assume 10 million daily users, read-heavy at about 100 to 1. Does that sound reasonable?”

Why it works: you save time, you show judgment, and the interviewer only has to say yes or correct you.

### Habit 2: Group your questions

Ask one short block, not one question per turn.

> “Before choosing an architecture, a few quick questions on correctness and scale. What are the core features, is it read- or write-heavy, what scale are we targeting, and what must never go wrong?”

### Habit 3: Say why you are asking

> “Do we need strong consistency on the seat count? That decides between a relational database with transactions and an eventually consistent store.”

This one sentence shows you understand the trade-off. It is worth more than the question itself.

### Habit 4: Time-box

3 to 5 minutes total. Aim for 4 to 7 questions. This is a guide, not a quota. Money-critical systems may need a few more.

### Habit 5: Revisit assumptions at decision points

> “Because we assumed reads are 100 times writes, I am adding a cache. If writes were heavy instead, I would switch to a queue plus batched writes.”

This is a strong senior signal.

### Handling what the interviewer says back

| Interviewer says | What you do |
| --- | --- |
| “You decide.” | State your assumption clearly, write it on the board, move on. |
| “I do not know.” | Same. Say: “I will assume X, and I can adapt if it changes.” |
| “What do you think?” | Give a reasonable number or choice and one line of why. |
| Gives a surprising answer | Pause, repeat it back, say how it changes the design. |
| Looks impatient | Stop asking. Summarize assumptions. Start designing. |
| Says “good question” | Do not relax. Tie the answer to a design choice right away. |

### Ready-made assumption lines

- “Reads far outnumber writes. I will adapt if that changes.”
- “Duplicate delivery is possible, so consumers must be idempotent.”
- “Staleness is fine for the feed but not for confirmed seat ownership.”
- “Single region first. I will explain how to extend to multi-region.”
- “Eventual consistency is fine for counts. Money uses strong consistency.”

---

## 8. What not to ask

| Avoid | Why | Do instead |
| --- | --- | --- |
| “Should I use Kafka?” or “Which database?” | You are asking for the answer | Decide and justify |
| Obvious questions like “Should it be secure?” | Wastes time | Assume yes, mention it later |
| Ten questions in a row | Looks like stalling | Pick the 5 to 8 that matter |
| Questions that do not change the design | No impact | Assume and move on |
| Low-level details (schema, field names) | Too early | Wait for the data model step |
| Asking nothing | You may solve the wrong problem | Always ask the core five |
| Continuing because the checklist has more items | A checklist is a tool, not a quota | Stop when the forks are resolved |

---

## 9. Worked examples (what you actually say)

### Example 1: Movie ticket booking

**You:** “Core flows are browse shows, pick seats, pay, get a ticket. Is cancellation and refund in scope?” *(S)*

**You:** “The main rule is a seat can never be sold twice. Should I hold a seat while the user pays, and for how long?” *(C)*

**You:** “I will assume browsing is far heavier than booking, but a hot release spikes bookings on one show. Correct?” *(A)*

**You:** “About 10 million users. A big release might see 100K people fighting for 1,000 seats?” *(L)*

**You:** “Many cities and theaters, one region to start?” *(E)*

**You:** “If payment fails or times out, the seat is released and the user is told. OK?” *(D)*

**Decisions these drive:**

- Relational DB with an atomic conditional update (the invariant lives in the database).
- A hold with expiry.
- Cache only for display, never for the final seat check.
- Waiting room to absorb spikes.
- Idempotent payment flow with a saga for failures.

### Example 2: News feed

**You:** “Posts with text and images, plus a follow graph. Ranked or chronological?” *(S)*

**You:** “Nothing is money-critical here. Is a few seconds to minutes of staleness fine?” *(C)*

**You:** “Reads far outweigh writes?” *(A)*

**You:** “500 million users, 200 followers on average, but some accounts have millions?” *(L)*

**You:** “Global, mobile-heavy?” *(E)*

**You:** “If the ranking service is down, can we fall back to chronological?” *(D)*

**Decisions these drive:** eventual consistency, heavy caching, hybrid fan-out (push for normal users, pull for celebrities), CDN for media, graceful fallback.

### Example 3: Payment system

**You:** “What must never happen? Charging twice or losing a payment. Can I assume an external provider like Stripe processes the charge?” *(C)*

**You:** “Low volume but critical, with many client retries?” *(A)*

**You:** “If the provider times out, the outcome is unknown. Is a pending state with reconciliation acceptable?” *(D)*

**You:** “About 1,000 payments per second at peak?” *(L)*

**Decisions these drive:** idempotency keys, a payment state machine, webhooks plus reconciliation, a ledger, strong consistency in one database.

### Example 4: Notification service

**You:** “Which channels: push, email, SMS? Do users set preferences?” *(S)*

**You:** “Is a critical alert (like an OTP) different from a marketing message?” *(C)*

**You:** “Can a notification be delayed by a few seconds? Are duplicates tolerable?” *(D)*

**You:** “Do we have bursts, like a campaign to 50 million users at once?” *(L)*

**Decisions these drive:** queues with priority lanes, provider fallbacks, retries with dedup keys, rate limits per provider, separate pipelines for OTP and bulk.

---

## 10. Closing the clarification phase

Before you move on, write this on the board:

```
FUNCTIONAL:      core features (2-3), out of scope
NON-FUNCTIONAL:  scale, latency, consistency, availability, durability
INVARIANT:       what must never go wrong
ASSUMPTIONS:     everything not confirmed
```

Then say: **“Here is what I am designing for. Stop me if anything is off.”**

This does three things. It shows structure, it gives the interviewer a chance to correct you early, and it gives you a reference to return to when you make decisions.

**The full interview flow:**

requirements, then estimation, then API and data model, then architecture, then the hardest design decision, then failure recovery, then trade-offs and evolution.

Clarifying questions are the start of the requirements step, not all of it.

---

## 11. Practice: how to drill this skill

You get good at this by repeating a small loop many times. Do not just read problems. Practice *asking*.

### The 5-minute drill (do this for every problem)

1. Read only the one-line prompt (for example, “Design a rate limiter”).
2. Set a 3-minute timer. Write your questions in S.C.A.L.E.D. order.
3. Next to each question, write: “If A, I draw \_\_\_. If B, I draw \_\_\_.” Delete any question where A and B give the same drawing.
4. Write your assumptions for everything you skipped.
5. Write the board summary (section 10).
6. Compare against the checks below.

### Self-scoring checklist

| Check | Pass? |
| --- | --- |
| I found the invariant (what must never go wrong) |  |
| I asked read/write ratio and scale |  |
| I asked about freshness or consistency |  |
| Every question had an A/B design impact |  |
| I stayed within 4 to 7 questions |  |
| I stated assumptions out loud |  |
| I asked at least one pattern-specific killer question |  |
| I finished in under 5 minutes |  |

### Practice problems, grouped by pattern

The goal is to learn the pattern, not memorize each problem. For each one, there is a prompt and the *one question that decides the design*.

| Pattern | Problem | The question that decides the design |
| --- | --- | --- |
| Resource allocation | Hotel or ticket booking | Can two users compete for the same item, and is there a hold? |
| Resource allocation | Flash sale inventory | How hot is a single item, and is overselling ever allowed? |
| Read-heavy | News feed | How many followers can one author have? |
| Read-heavy | URL shortener | What is the redirect to create ratio, and do links expire? |
| Write-heavy | Metrics or log ingestion | Can we lose events, and is real-time needed? |
| Write-heavy | Location tracking | How often does each device update? |
| Async / jobs | Job scheduler | Can a job run twice, and what if a worker dies mid-job? |
| Async / jobs | Video upload and transcoding | How long can processing take, and does the user need progress? |
| Transactional | Payment system | Are we the system of record or a provider integration? |
| Transactional | Digital wallet / ledger | What must always balance, and how are retries handled? |
| Real-time | Chat / messaging | Group size, ordering, and offline delivery? |
| Real-time | Live comments or presence | How many viewers per stream, and is loss acceptable? |
| Notifications | Notification service | Can messages be delayed or duplicated? |
| Search | Typeahead / search | How fresh must the index be? |
| Geospatial | Ride matching | One-to-one or one-to-many matching, and update frequency? |
| Primitives | Rate limiter | Per user or per tenant, and fail-open or fail-closed? |
| Primitives | Distributed cache / KV store | Which consistency, and which failures to tolerate? |
| Multi-tenant | SaaS platform | How different are tenant sizes, and what isolation is needed? |
| Files | File storage / sharing | Max file size, and public or private? |
| Analytics | Real-time dashboard / counters | Is approximate OK, and how late can data arrive? |

### How to progress

- **Week 1:** Do the 5-minute drill on 2 problems per pattern. Focus only on finding the invariant and the scale.
- **Week 2:** Add the killer questions and the A/B design impact for each question.
- **Week 3:** Practice out loud with a timer or a partner. Delivery matters as much as content.
- **Always:** After each design, check if any question you skipped would have changed the result.

---

## 12. One-page cheat sheet

**Rule:** Ask only if the answer changes the design. Otherwise state an assumption.

**Exception:** Always ask about correctness.

**Order:** Scope, Correctness, Access, Load and latency, Environment, Degradation.

**Core five (panic routine):** core features, what must never go wrong, read or write heavy, scale, freshness.

**Then add:** latency, availability, ordering, retry tolerance, and 1-3 pattern-specific questions.

**Delivery:** propose defaults, group questions, explain why, time-box to 3-5 minutes, revisit assumptions at decision points.

**If stuck:** state assumptions out loud, write them down, start designing.

**Final test:** “If the interviewer gave the opposite answer, would I change a meaningful design decision?” If no, do not ask yet.

**Close the phase with:** functional, non-functional, invariant, assumptions, then “Stop me if anything is off.”

