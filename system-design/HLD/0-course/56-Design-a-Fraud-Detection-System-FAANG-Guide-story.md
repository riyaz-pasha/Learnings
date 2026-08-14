# Design a Fraud Detection System — The Story (narrative edition)

## What this file is

The reference file, `56-Design-a-Fraud-Detection-System-FAANG-Guide.md`, is the one to recite from. It has the requirements, the API shapes, every trade-off table, and the master cheat sheet.

This file is a second way in. It tells the same material as one continuous story, in plain language.

Here's the shape of the story: engineers at a company keep hitting a wall, patch it, and the patch itself creates the next wall. Eventually they land on the exact same hybrid rules-plus-ML design that the reference file documents.

The company, **Ledgerly** (a payments startup processing card transactions for small online merchants), is fictional. But every wall it hits is something a real, named system actually hit:

- PayPal's own documented shift from manual/rule-based review to statistical fraud models in its early years (the "Igor" fraud-ring story recounted by PayPal co-founder Max Levchin and in Eric M. Jackson's *The PayPal Wars*).
- Uber's Michelangelo feature store (built specifically to guarantee training and serving compute features identically).
- Card-network rules-plus-scoring engines like Visa Advanced Authorization.
- Documented card-network dispute-window rules.

I'll say clearly, every time, whether something is a documented fact or just a reasonable stand-in number.

## The one idea behind everything

**The trigger phrase** for this whole topic: *"design a real-time fraud/risk-scoring system for payments."*

Keep one sentence in your head as you read:

> The hard part isn't the model — it's making sure the feature the model sees at the moment of scoring was computed exactly the same way it was during training, and combining a fast deterministic rules engine with a slower-to-adapt ML score into one decision.

Everything below is just this one idea, getting harder in small, honest steps.

---

## Chapter 1 — The report that arrives after the money is already gone

### The starting point

It's 2018. Ledgerly is small — a few thousand transactions a day for a handful of online merchants.

Fraud detection is a nightly cron job:

- At 2am, a SQL job scans yesterday's transactions.
- It looks for suspicious patterns — unusual amounts, cards used across many merchants.
- It drops a report on the fraud team's desk in the morning.

### The day it fails

One Tuesday, the report flags **340 suspicious transactions** from the day before, totaling **$210,000** `[illustrative — Ledgerly-specific, but "batch fraud detection finds it too late" is a well-documented failure mode of early card-fraud systems]`.

Here's the timeline of what happens next:

1. By the time anyone calls a merchant, the money has already settled overnight.
2. Funds have already moved out to the fraudster's linked bank account.
3. The fraudster has already withdrawn the money.

Of the $210,000 flagged, **$180,000 turns out to be unrecoverable**. The batch job did its job perfectly — it just did it a full day too late to matter.

```mermaid
sequenceDiagram
    participant Txn as Fraudulent transaction<br/>(2:14pm)
    participant DB as Transactions table
    participant Batch as Nightly batch job<br/>(2am next day)
    participant Team as Fraud team

    Txn->>DB: Transaction recorded,<br/>money already settling
    Note over DB: 12+ hours pass —<br/>settlement completes overnight
    Batch->>DB: Scan yesterday's rows<br/>for suspicious patterns
    Batch->>Team: "340 suspicious transactions,<br/>review now"
    Note over Team: By now the money<br/>is already gone
```

### Why "just run it more often" doesn't fix it

The obvious next question: *why not just run the batch job every hour instead of once a night?*

Because even hourly is still too late. A stolen card can be used and drained within minutes of being stolen.

The real requirement isn't "check sooner." It's **check before the transaction is even approved** — in the same instant the payment is being authorized, before any money moves at all.

### The fix

Move the fraud check into the transaction's own critical path. Score it **before** you say APPROVE, not after.

**The analogy for the rest of this story:** think of this like a **bouncer checking ID at the door**, not a security guard reviewing last night's guest list over coffee. The door either opens or it doesn't, right then. There's no "we'll figure out who shouldn't have come in, tomorrow."

### The new problem this creates

Putting the fraud check inline means it now shares the payment's own latency budget — tens to low hundreds of milliseconds, not "whenever the nightly job gets to it."

The very first thing an engineer reaches for, to compute a feature like "how many transactions has this card made in the last hour," is to just query it live, per incoming transaction. That's the next wall.

### How I'd say this in an interview

"The naive version of fraud detection is a nightly batch scan, and the reason it fails isn't accuracy — it correctly finds the fraud — it's timing, because by the time it runs, the money's already moved. The real requirement is scoring the transaction inline, before it's approved, which immediately turns this into a latency-budgeted, real-time problem."

---

## Chapter 2 — The query that re-reads an hour of history, every single time

### The naive feature query

The fix from Chapter 1 works. Ledgerly builds a scoring step that runs during authorization.

For the feature "transactions by this card in the last hour," the simplest thing to write is exactly what it sounds like:

```sql
SELECT COUNT(*) FROM transactions
WHERE card_token = ? AND created_at > now() - interval '1 hour'
```

### How this falls apart as the company grows

Here's how the cost of this query grows over time:

| Stage | Table size | Query latency |
|---|---|---|
| Early days | small table | ~80ms `[illustrative]` — tolerable, if a little slow |
| One year later | 40 million rows | ~600ms |

And "transactions per card per hour" isn't the only rolling feature the model needs. There are roughly **15** of them — count per card, sum per merchant category, velocity across devices, and so on.

Do the math on what that means at peak load:

- Scanning for all 15 features, even in parallel, means **15 separate table scans per incoming transaction**.
- At a peak of 200 transactions/sec, that's **3,000 scanning queries/sec** landing on one already-busy OLTP table.

The database falls over well before the fraud check even finishes.

```mermaid
flowchart LR
    A["Incoming transaction"] --> B["Query: scan last hour's<br/>rows for this card"]
    B --> C["...times 15 features,<br/>each a separate table scan"]
    C --> D["600ms+ per feature,<br/>3,000 scans/sec at peak"]
    D --> X["Database falls over<br/>before scoring even starts"]
```

### Why re-scanning is the wrong approach

The obvious question: *why re-read the whole hour of history every single time, when almost none of it changed since the last transaction from this card?*

Because a scan recomputes everything from scratch. What you actually want is a number that:

- **Ticks up** as new events happen.
- **Ticks down** as old events fall outside the window.
- Is **never re-derived from the raw rows again**.

### The fix

**The analogy for the rest of this story:** an **odometer, not a trip log you re-read**. A car's odometer doesn't recalculate your total mileage by re-reading every past fuel receipt. It just adds one mile the instant one mile happens, and it's always instantly readable.

Ledgerly builds a **stream processor** that:

- Consumes the transaction stream.
- Maintains these rolling counts incrementally — the same architectural role Kafka Streams or Apache Flink play in production stream-processing systems.
- Writes the current value of each feature into a low-latency **online feature store** (a key-value store).

The scoring service then reads from that store with a simple point lookup — sub-10ms — instead of a scan.

```mermaid
flowchart LR
    Stream["Transaction stream"] --> SP["Stream processor:<br/>updates rolling counters<br/>(the odometer)"]
    SP --> Store[("Online feature store<br/>key → current value")]
    Store --> Score["Scoring service:<br/>simple point read, &lt;10ms"]
```

### The new problem, three weeks after launch

Ledgerly ships its first ML fraud model, trained offline on historical data using features with these exact same names — `txn_count_1h`, `amount_vs_merchant_avg`.

- Offline, on held-out historical data, it scores a strong **0.94 AUC** `[illustrative]`.
- In production, it visibly misses fraud patterns it supposedly learned to catch.

Nothing crashed. Nothing alerted. It's just quietly worse than advertised.

### How I'd say this in an interview

"A rolling feature like 'transactions in the last hour' should never be computed by scanning historical rows per incoming request — that's an O(window size) operation on the hot path. The fix is an incrementally updated aggregate in a stream processor, written to a low-latency feature store, so the read at scoring time is a cheap point lookup, not a scan."

---

## Chapter 3 — The model that aced the lab and flopped on the floor

### Two pipelines, one feature name, two different answers

Ledgerly's data scientists dig in. Here's what they find:

- The model was trained on features computed by an **offline** pipeline that scans the full historical record for a given time window.
- In production, the **same-named** feature is computed by the **online** stream processor from Chapter 2.

Nobody had reason to assume these were different — same feature name, same intent. But a side-by-side comparison for the same historical period turns up two specific bugs.

**Bug 1 — window boundaries don't match.**

- The offline pipeline computes "transactions in the last hour" using **calendar-hour boundaries** (e.g., 2:00–3:00pm).
- The online pipeline uses a **rolling 60-minute window**.
- For a transaction at 2:58pm, these can disagree by nearly an hour's worth of activity.

**Bug 2 — off-by-one on the current transaction.**

- The offline pipeline, when building training examples, accidentally **includes the transaction being scored in its own count**.
- Example: a transaction that's the 6th in the last hour sees `txn_count_1h = 6` in training.
- The online pipeline correctly excludes the current transaction and would compute `5` for the same case.

Neither of these throws an error. The model just sees a systematically different number online than the one it learned the meaning of during training. Its risk scores are miscalibrated, silently, and stay that way until someone thinks to check.

```mermaid
flowchart TD
    A["Feature: 'transactions per<br/>card in last hour'"] --> B{"Computed the SAME way<br/>online and offline?"}
    B -->|"yes"| C["Model sees in production<br/>the same signal it learned<br/>from in training"]
    B -->|"no — calendar-hour vs<br/>rolling window, off-by-one<br/>on the current txn"| D["TRAIN/SERVE SKEW:<br/>accuracy silently drops,<br/>no error thrown"]
```

### How you'd even catch this

The obvious question: *how do you even catch this, given nothing crashes?*

By comparing the **distribution** of a feature's online-computed values against its offline-computed values, for the same historical period, and noticing they don't line up.

### The fix

**The analogy for the rest of this story:** **one measuring cup, not two kitchens each with their own.** If two different chefs each eyeball "one cup of flour" independently, their cups won't match exactly, and the recipe comes out subtly wrong every time — with no error message, just a worse result.

The actual fix is a **single shared feature-definition** — one piece of code, or a proper feature store's dual-materialization capability — that both the training pipeline and the live scoring pipeline call.

This is a documented, real reason feature stores exist. Uber's **Michelangelo** feature store was built specifically to guarantee that a feature computed for training and the same feature computed for online serving come from the same definition — not two independently-written approximations of "the same thing."

### The new problem, three weeks later

Once feature consistency is fixed and the model is working well, a brand-new fraud ring starts using a specific stolen card-number range nobody has seen before.

- The now-accurate model doesn't recognize it yet.
- It can only learn from labeled training examples, and there aren't any for this pattern yet.
- About **$40,000** `[illustrative]` of this specific fraud gets through before anyone notices — purely because the model's only way of learning anything new is retraining on data that doesn't exist yet.

### How I'd say this in an interview

"Train/serve skew is the classic silent failure mode in ML-serving systems — the model's offline metrics and its production behavior quietly diverge because online and offline computed 'the same' feature slightly differently. The fix is one shared feature definition materialized both ways, which is exactly the problem feature stores like Uber's Michelangelo were built to solve — never two independently maintained implementations of the same feature."

---

## Chapter 4 — The pattern everyone already knows about, and the model that hasn't heard yet

### The fraud team knows; the model doesn't

The fraud team investigates the $40,000 loss and finds the pattern in about a day: a specific card-number (BIN) range, `453910–453915` `[illustrative]`.

- They know it's bad **today**.
- The model won't know it's bad until its next retraining cycle picks up enough labeled examples.
- Per Chapter 3, labels for confirmed fraud don't even exist yet — they arrive later, via disputes.

In the meantime, more of this exact pattern keeps getting through.

### The historical echo

This is close to a documented piece of fraud-detection history. PayPal's own early fraud fight, as Max Levchin and others have recounted, started out largely manual/rule-based. PayPal's shift toward statistical, learned fraud models — against a persistent fraud operation PayPal engineers reportedly nicknamed "Igor," per accounts in Eric M. Jackson's *The PayPal Wars* — was a response to exactly this gap. Rules catch what's already known, but a determined, evolving attacker keeps finding what isn't `[illustrative framing of the specific historical detail, but the rules-to-ML shift itself is well documented]`.

Ledgerly is now living the mirror-image problem: it has the model, but not yet the fast-reacting rules layer that catches what's *already* known.

### Why waiting on the model is the wrong call

The obvious question: *if we already know this pattern is bad right now, why does the system wait a week to react to it?*

Because a model only changes through retraining, and retraining needs labeled data — days to weeks away. A **rule**, by contrast, is just a deterministic check that a human can write and deploy in minutes.

### The fix

**The analogy for the rest of this story:** **an instant memo versus a slow-learning gut instinct.**

Ledgerly adds a **rules engine**:

- Deterministic checks — this BIN range, this device fingerprint, this velocity threshold.
- Runs alongside the ML model.
- Can be updated in minutes.

When a rule fires, it **wins outright** over the model's score. Why? A rule represents a known, verified, human-confirmed pattern. A model score is a probabilistic estimate learned from inherently stale historical data. Higher-confidence, deterministic knowledge beats a probability estimate when both are on the table.

```mermaid
flowchart TD
    A["Transaction"] --> B["Rules engine<br/>(deterministic, instant)"]
    A --> C["ML model<br/>(learned, slower to adapt)"]
    B --> D{"Any rule fired?"}
    D -->|"yes"| E["DECLINE —<br/>model score irrelevant,<br/>rule wins"]
    D -->|"no"| F["Fall through to<br/>model's score"]
```

### The new problem, almost immediately

Now Ledgerly has both a rules engine and a model, running side by side. That raises two questions:

- What happens when the model says "high risk, score 0.85" on a transaction no rule flags?
- What happens when a rule fires on something the model scores as very low risk?

"Decline if any rule fires OR score is above some cutoff" sounds obviously right — and it is, mostly. But it turns out to be dangerously blunt when a rule itself is written a little too broadly. That's the next wall.

### How I'd say this in an interview

"Rules and the ML model aren't redundant — they cover different gaps. Rules catch known patterns instantly and explainably; the model catches the broader distribution of fraud nobody's written a rule for yet. When a rule fires, it should win, because it's higher-confidence, verified knowledge, not a probabilistic guess."

---

## Chapter 5 — The good customer declined by a rule that was too blunt

### The rule that ships without a trial run

Ledgerly writes a new velocity rule to stop card-testing bots:

> "More than 5 transactions from one card in 10 minutes → decline."

It ships straight to production, no trial period — after all, it's "just a rule," how risky can it be?

### What it actually catches, three weeks later

The rule catches two very different things that look the same on paper:

1. **Correctly:** an actual card-testing bot making rapid attempts.
2. **Incorrectly:** a legitimate small merchant's regular customer buying concert tickets, retrying six times in eight minutes because the merchant's checkout page kept timing out.

Over one weekend, this single rule wrongly declines an estimated **1,200 good transactions**, worth roughly **$85,000** in legitimate revenue `[illustrative]`, before anyone connects the spike in complaint tickets back to the new rule.

```mermaid
sequenceDiagram
    participant Rule as New velocity rule<br/>(shipped live, no trial)
    participant Good as Legit customer,<br/>slow checkout page
    participant Fraud as Actual<br/>card-testing bot

    Rule->>Fraud: 6 rapid attempts →<br/>DECLINE (correct)
    Rule->>Good: 6 retries in 8 min →<br/>DECLINE (wrong — same shape,<br/>different cause)
    Note over Rule: 1,200 good transactions declined<br/>this weekend — nobody notices<br/>until complaint tickets spike
```

### Why you need a rehearsal before deploying a rule

The obvious question: *how do you catch a bad rule before it does damage at real scale, instead of after?*

The same way you'd de-risk any high-blast-radius config change: never let it act on real traffic the first time it runs.

### The fix

**The analogy for the rest of this story:** **a dress rehearsal before opening night.**

Every new or changed rule goes into **shadow mode** first:

- It evaluates against real, live traffic.
- It logs what it *would* have decided.
- It does **not** actually decline anything yet.

The fraud team reviews that shadow log — especially checking it against known-good customers — before flipping the rule live. This is the same discipline as canarying any risky production change: you rehearse against the real audience before the curtain goes up for real.

```mermaid
flowchart LR
    A["New/changed rule"] --> B["Shadow mode:<br/>log decisions,<br/>don't act on them"]
    B --> C{"Reviewed against<br/>real traffic —<br/>looks safe?"}
    C -->|"yes"| D["Promote to live"]
    C -->|"no — too broad"| E["Fix and re-shadow"]
```

### The new problem, deeper and permanent

Even with careful rule rollout, a harder truth remains underneath everything so far: **every** decision boundary — a rule's threshold, the model's score cutoff — trades off missing real fraud against blocking a real customer.

There is no threshold that eliminates both at once. This tension doesn't get "fixed"; it gets **managed**, deliberately, as a business calibration.

### How I'd say this in an interview

"A rule that's individually cheap to write can still be expensive at scale if it's too broad — the fix is shadow-mode canarying for any rule change, the same discipline you'd apply to any risky config change. But shadow mode only catches badly-tuned rules; it doesn't remove the underlying tension between missing fraud and blocking good customers, which is the next thing to name."

---

## Chapter 6 — The line you can't move without moving it somewhere else

### The threshold trade-off, in numbers

Ledgerly tunes the model's decline threshold directly. Here's what happens at two different cutoffs `[illustrative]`:

| Score cutoff | Fraud caught | Legitimate transactions wrongly declined |
|---|---|---|
| 0.5 | 92% | 3.5% |
| 0.8 | 61% | 0.4% |

Read this table carefully:

- Lowering the cutoff to 0.8 (stricter — you need a higher score to get declined) does cut the false-decline rate from 3.5% down to 0.4%. Good customers get bothered far less.
- But the fraud catch-rate falls from 92% down to 61%. Far more real fraud gets through.

There is no cutoff that gives you both numbers at their best.

### Why this matters more than it looks

This is a well-documented tension in the payments industry generally. Multiple industry cost-of-fraud studies — for example, LexisNexis Risk Solutions' recurring "True Cost of Fraud" research — have found that false declines (turning away *good* customers) cost merchants a multiple of what actual fraud losses cost them `[illustrative framing of the exact multiple, but the direction — false declines being expensive, often more expensive than fraud itself — is a widely documented industry finding]`.

A model tuned only to "catch more fraud," without weighing this cost, is optimizing half the problem.

```mermaid
quadrantChart
    title Decline threshold: fraud caught vs. good customers turned away
    x-axis Loose threshold (catches less) --> Strict threshold (catches more)
    y-axis Fewer good customers declined --> More good customers declined
    quadrant-1 Strict and costly to good customers
    quadrant-2 Balanced middle ground
    quadrant-3 Loose, low collateral damage
    quadrant-4 Strict, low damage (rare, aspirational)
    "Cutoff 0.8": [0.25, 0.15]
    "Cutoff 0.65": [0.5, 0.45]
    "Cutoff 0.5": [0.8, 0.75]
```

### Is there a "correct" cutoff?

The obvious question: *is there a "correct" cutoff, then?*

No. It's a calibration between:

- The dollar cost of missed fraud, and
- The dollar-plus-reputation cost of falsely declining good customers.

Reasonably, this calibration should vary by merchant or region — it shouldn't be one frozen global number.

But the deeper insight is: **you don't have to force every score into just two outcomes.**

### The fix

**The analogy for the rest of this story:** a **manager who can say "let me look at this one"** instead of only "yes" or "no" at the door.

Add a third decision: **APPROVE / DECLINE / REVIEW.**

Scores that land in a genuinely ambiguous middle band get routed to a human analyst instead of being forced into an automated binary call. This absorbs exactly the cases where an automatic threshold does the most collateral damage.

```mermaid
stateDiagram-v2
    [*] --> Scored: features read,<br/>rules evaluated,<br/>model scored
    Scored --> Approved: no rule fired,<br/>score below low threshold
    Scored --> Declined: rule fired, or score<br/>above high threshold
    Scored --> UnderReview: score in between,<br/>no rule fired
    UnderReview --> Approved: analyst clears
    UnderReview --> Declined: analyst confirms risk
```

### The new problem

Routing to human review only works if the human — or the customer disputing a decline — can actually see **why** the system thinks this transaction is risky.

Right now the API returns a bare number, `modelScore: 0.71`, and nothing else. A reviewer can't act on a number alone.

### How I'd say this in an interview

"There's no threshold that's simultaneously best for fraud catch-rate and false-decline rate — it's a calibrated business trade-off, and it's genuinely different from a legal absolute like sanctions screening. The three-way APPROVE/DECLINE/REVIEW shape exists specifically to absorb the ambiguous middle band into human judgment instead of forcing every score through one brittle cutoff."

---

## Chapter 7 — The decline nobody could explain

### The dispute nobody can answer

A customer disputes a declined **$340** purchase and files a complaint.

Ledgerly's dispute team pulls up the record and finds exactly one thing: `modelScore: 0.71`.

That's it. Specifically, there's:

- No indication of which feature drove that number.
- No record of whether a rule was even evaluated.

There's nothing to tell the customer, nothing to hand to the card network's dispute process, and nothing for an internal auditor asking "why was this declined."

### Why explainability isn't optional

Explainability here isn't just good manners. Adverse-action-style reasoning for a financial decline is a real, documented regulatory expectation in several jurisdictions and products — the same underlying idea as the long-standing adverse-action-notice requirement in consumer lending: if you're going to say no to someone's money, you generally need to be able to say why.

### How to make a decision explainable without slowing it down

The obvious question: *how do you make a decision explainable after the fact, without slowing down the actual scoring path?*

You don't reconstruct it after the fact at all. You **capture it at the moment it's made**, once, and store it.

### The fix

**The analogy for the rest of this story:** **an itemized receipt, not just a total.**

Every scored transaction logs:

- `rulesFired` — which rules, if any, evaluated true.
- `topFeatures` — the specific features and their computed values that contributed most to the score.
- `featureComputeVersion` — a tag linking back to the exact feature-computation logic in effect at that moment.

Now a decision from three weeks ago can be reproduced exactly, not just gestured at.

```mermaid
flowchart LR
    A["Transaction scored"] --> B["Log: rulesFired[],<br/>topFeatures[],<br/>featureComputeVersion"]
    B --> C[("Decision audit log")]
    C --> D["Dispute resolution:<br/>reconstruct exactly<br/>what drove this decision"]
```

### The new problem

Decisions are explainable now, and auditable in real time. But there's a slower, quieter issue underneath everything so far: an APPROVE from today and a DECLINE from today don't actually know yet whether they were **right**.

The only way to know for sure that a transaction really was fraud is if the cardholder disputes it — and that dispute doesn't happen instantly.

### How I'd say this in an interview

"The response should expose which features and rules drove the decision, not just a bare score — an unexplainable decline is both a poor customer experience and a real compliance liability. The fix is capturing feature and rule attribution at scoring time, tied to a versioned snapshot, not trying to reconstruct it after the fact."

---

## Chapter 8 — The fraud that confirms itself twelve days later

### The delayed confirmation, step by step

Here's the actual timeline of one transaction:

1. **Day 0:** the transaction gets scored low-risk (0.12) and approved.
2. It turns out to actually be fraud — but nobody knows that yet.
3. The cardholder doesn't check their statement carefully until **Day 12**.
4. On Day 12, the cardholder disputes the charge.
5. Only then does a chargeback confirming fraud land on Ledgerly's desk — twelve days after the original transaction `[illustrative specific number, but broadly consistent with documented card-network dispute rules, which generally give cardholders a window measured in weeks to months to file a dispute — Visa and Mastercard's own published dispute-resolution rules allow well beyond a same-day window]`.

```mermaid
sequenceDiagram
    participant Txn as Original transaction<br/>(Day 0)
    participant Model as Model:<br/>scores low-risk, APPROVE
    participant Card as Cardholder disputes<br/>charge (Day 12)
    participant Label as Confirmed-fraud<br/>label recorded
    participant Train as Next scheduled<br/>retraining run

    Txn->>Model: Scored, approved
    Note over Card: 12 days pass before the<br/>cardholder even notices
    Card->>Label: Chargeback confirms<br/>this WAS fraud
    Note over Train: Label feeds into<br/>the next weekly retrain
    Train->>Model: Updated model, hopefully<br/>catches similar patterns
```

### Why the model can't just retrain sooner

The obvious question: *can the model just retrain on same-day feedback, then?*

No — there's nothing to retrain on. The ground-truth label for this transaction doesn't exist on Day 0. It doesn't exist until Day 12.

Retraining cadence is bounded by **how fast confirmed labels physically arrive**, not by any infrastructure decision Ledgerly could make.

### The fix

**The analogy for the rest of this story:** **grading an exam weeks after it was taken.**

Ledgerly runs a **batch retraining pipeline** on a cadence matched to how fast enough new confirmed labels actually accumulate — weekly, in Ledgerly's case. This pipeline feeds the offline feature store plus these delayed labels into the next training run.

There is no faster honest option, because the feedback itself doesn't exist any faster.

### The realization this creates

This isn't really a new bug — it's a realization. This delayed feedback loop is exactly *why* the rules engine from Chapter 4 was never a legacy stopgap sitting awkwardly next to the "real" ML system. **It's the system's only fast-response mechanism.**

- A newly discovered pattern, investigated and confirmed by a human today, can be encoded as a rule and take effect in minutes.
- The model's reaction time to that same new pattern is bounded by this same days-to-weeks label delay, every single time.

### How I'd say this in an interview

"Model retraining cadence here is bounded by how fast confirmed labels arrive — days to weeks via chargebacks — not by infrastructure. That's exactly why the rules engine isn't a stopgap sitting next to the model, it's the system's only fast-response mechanism for anything newly discovered, because the model literally cannot react faster than its labels do."

---

## Chapter 9 — The night the feature store went dark

### The outage, step by step

It's 2am on a Saturday. The online feature store from Chapter 2 — the key-value store the scoring service reads on every transaction — has a hardware issue.

- Feature-read latency jumps from **5ms to 3,000ms** `[illustrative]`.
- Two bad options present themselves immediately.

**Option one:** the scoring service just waits for the feature store, the way it always has.

- Payment authorizations across the board start timing out too.
- A fraud-check outage becomes a full checkout outage, because the fraud check is inline on the critical path (per Chapter 1's whole reason for existing).

**Option two:** a panicking on-call engineer hardcodes "if the feature store is unreachable, just approve everything, don't block payments."

- Ledgerly is now wide open to fraud for the entire duration of the outage.
- Worse — nobody watching the dashboards would even know coverage had silently dropped to zero, because "approved" looks identical whether or not a real check happened.

```mermaid
sequenceDiagram
    participant Txn as Incoming transaction
    participant Store as Online feature store<br/>(degraded, 3,000ms)
    participant Breaker as Circuit breaker
    participant Rules as Rules engine<br/>(no feature-store dependency)
    participant Combiner as Decision combiner

    Txn->>Store: Read features
    Store--xTxn: Timeout
    Breaker->>Breaker: Error rate over threshold —<br/>circuit OPENS
    Breaker->>Combiner: Skip model scoring,<br/>rules-only mode
    Txn->>Rules: Evaluate deterministic rules
    Rules-->>Combiner: No rule fired
    Combiner-->>Txn: APPROVE, logged as<br/>"degraded-mode decision"
    Note over Breaker: Never silently fails open to<br/>always-approve, and never stalls<br/>the payment indefinitely
```

### Neither extreme is acceptable

The obvious question: *what's the actual safe fallback, if both extremes are bad?*

Neither "block everything" nor "approve everything" is acceptable. The fix is to **degrade to whatever check can still run without the broken dependency**, and make that degradation **visible**, not silent.

### The fix

**The analogy for the rest of this story:** **emergency lighting, not total darkness and not a full evacuation.**

When main power fails in a building, you don't sit in the dark, and you don't declare the building unsafe and clear everyone out — a pre-agreed, automatic fallback kicks in.

Ledgerly puts a **circuit breaker** on the feature-store read path:

- When the store's error rate crosses a threshold, the model-scoring step is skipped entirely.
- The decision falls back to **rules-only**, since rules don't depend on the feature store at all.
- Every one of these decisions is explicitly tagged as "degraded mode" in the audit log from Chapter 7.

This means reduced coverage shows up in monitoring, instead of getting mistaken for full coverage.

### Where this actually lands

Rules-only coverage is narrower than the full hybrid decision. But it's a real, bounded, known trade-off — a business policy decided calmly in advance, not an engineering improvisation invented at 2am during an incident.

### How I'd say this in an interview

"A fraud-check outage should never silently become 'no fraud checking happened' by failing open to always-approve, and it shouldn't stall payments by blocking on a dead dependency either. The fix is a circuit breaker that degrades to a rules-only decision — narrower coverage, but bounded and visible — decided as a pre-agreed policy, not improvised during the incident."

---

## Where the story actually lands

```mermaid
flowchart LR
    A["Ch1: Nightly batch<br/>(finds fraud a day late)"] -->|"fixes: check before approval,<br/>in the critical path<br/>breaks: query-time scan too slow"| B["Ch2: Rolling aggregates<br/>+ online feature store"]
    B -->|"fixes: fast feature reads<br/>breaks: online/offline<br/>compute differs"| C["Ch3: Shared feature<br/>definition (feature store)"]
    C -->|"fixes: train/serve consistency<br/>breaks: model can't react to<br/>new patterns fast"| D["Ch4: Rules engine,<br/>rule wins over model"]
    D -->|"fixes: instant known-pattern block<br/>breaks: a broad rule declines<br/>good customers"| E["Ch5: Shadow-mode<br/>rule canarying"]
    E -->|"fixes: safe rule rollout<br/>breaks: no threshold beats<br/>both costs at once"| F["Ch6: Three-way decision<br/>(APPROVE/DECLINE/REVIEW)"]
    F -->|"fixes: absorbs the ambiguous<br/>middle<br/>breaks: reviewers can't act<br/>on a bare score"| G["Ch7: Per-decision<br/>explainability logging"]
    G -->|"fixes: auditable decisions<br/>breaks: labels for today's decision<br/>don't exist yet"| H["Ch8: Batch retraining,<br/>bounded by label delay"]
    H -->|"realization: this is why<br/>rules are the fast-response layer"| I["Ch9: Circuit breaker,<br/>rules-only degraded mode"]
```

```mermaid
mindmap
  root((Why a fraud system<br/>needs all of this))
    Timing
      batch = too late, money already moved
      inline scoring = before you approve
    Feature computation
      per-request scan doesn't scale
      rolling aggregate + online feature store
    Train/serve consistency
      two independent "same" features drift apart
      one shared feature definition, materialized both ways
    Known vs novel patterns
      model only learns from labeled data, with a delay
      rules block a known pattern in minutes, not weeks
    Rollout safety
      a rule too broad declines good customers at scale
      shadow mode before it acts on real traffic
    The permanent trade-off
      no threshold beats both costs at once
      three-way decision absorbs the ambiguous middle
    Explainability
      a bare score can't be disputed or audited
      log feature + rule attribution at scoring time
    Feedback loop
      confirmed labels arrive days to weeks late
      retraining cadence is bounded by that delay
    Degraded mode
      neither fail-open nor fail-closed is acceptable
      circuit-break to rules-only, visibly
```

### Where to stop

Every real production fraud system sits somewhere on this chain. The skill isn't reciting all nine chapters — it's stopping where the stated requirements say to stop.

- A low-stakes internal tool might reasonably stop around Chapter 4 (rules plus a model).
- A real payments system that has to survive disputes, audits, and 2am outages has to reach Chapter 7, 8, and 9.

If nobody's mentioned regulatory explainability, walking all the way to Chapter 7 unprompted reads as padding, not depth.

---

## Grill me — adversarial follow-ups

**Q1: "Why not just run the batch job every five minutes instead of building all this real-time infrastructure?"**

Because "every five minutes" still means the transaction was already approved and the money may already be moving before the batch job even looks at it. It doesn't change the fundamental timing problem — it just shrinks the window. The requirement isn't "check soon," it's "check before you say yes," which forces the check into the payment's own critical path no matter how frequently a batch job runs.

**Q2: "Isn't a rules engine just a worse, more manual version of the ML model? Why not drop it once the model is good?"**

No — they cover genuinely different gaps. A model can only act on patterns it's seen enough labeled examples of, and per Chapter 8, those labels arrive days to weeks late. A rule can encode a pattern a human investigator confirmed an hour ago and take effect in minutes. Dropping rules would mean every newly discovered fraud pattern goes unblocked for as long as it takes labels to accumulate and a retrain to run.

**Q3: "Walk me through exactly how train/serve skew could exist even if the code looks identical on both sides."**

It usually isn't the code that's identical, it's the *intent*. "Transactions in the last hour" sounds like one definition, but it can be implemented as calendar-hour buckets in one pipeline and a rolling 60-minute window in the other — or one pipeline can accidentally include the transaction being scored in its own count. Neither difference throws an error. You only catch it by comparing the actual distribution of computed values for the same historical window, online versus offline.

**Q4: "If a fired rule always wins over the model, doesn't that mean one badly-written rule can override a model that's actually right?"**

Yes, and that's exactly the failure Chapter 5 walks through — a rule that's too broad can override a model's correct low-risk score and decline a legitimate customer at scale. That's not an argument against rules winning in general, it's the argument for never letting a new or changed rule act on real traffic before it's been shadow-tested against real transactions first.

**Q5: "You route ambiguous scores to human review — doesn't that just not scale once volume gets big enough?"**

It has a real ceiling, yes — but the fix is tuning the review band to be genuinely narrow, not making review the default outcome. The point of a three-way decision isn't "let humans decide most things," it's "let automation handle the confident majority on both ends, and reserve a deliberately small band in the middle for the cases where an automatic cutoff does the most collateral damage."

**Q6: "Why can't you just retrain the model daily on whatever labels you have so far, even if they're incomplete?"**

You can, and many systems do retrain on a regular cadence regardless — the point isn't that retraining can't happen daily, it's that today's decisions still can't be evaluated against today's ground truth, because most of today's true labels literally don't exist yet. A daily retrain on Day 0 simply won't contain Day 0's own fraud, no matter how often you run it.

**Q7: "What's actually wrong with failing closed — declining everything — during a feature-store outage, instead of degrading to rules-only?"**

It trades one outage for another: instead of undetected fraud during the incident, you get every legitimate customer's payment blocked, which for most businesses is at least as costly and is guaranteed harm instead of probabilistic risk. Rules-only degraded mode keeps the known-pattern coverage running and only sacrifices the model's broader-but-slower-to-compute coverage, which is a much smaller and more defensible gap.

**Q8: "Given everything you've said about explainability, isn't logging topFeatures and rulesFired for every transaction expensive at scale?"**

It's additional write volume, yes, but it's the same write path already logging the transaction and its decision — appending feature attribution to a record you're already writing is a small marginal cost compared to the alternative, which is being unable to answer a dispute or a regulator's question about a specific decline months later.

**Q9: "If someone just says 'design a fraud detection system' cold, where do you actually start?"**

Say the two things that decide almost everything downstream: what's the latency budget for the check within the overall payment flow, and are confirmed fraud labels available immediately or with a delay. The first tells you whether feature computation has to be a real-time rolling aggregate; the second tells you whether you need a rules engine as a fast-response layer alongside the model, or whether the model alone might suffice.

**Q10: "What's the single biggest thing candidates get wrong on this topic?"**

Treating it as a pure ML-serving problem and spending all their time on model architecture, when the actual hard parts are the real-time feature-computation pipeline, the train/serve consistency guarantee, and the fact that rules and the model have to combine deliberately rather than one simply replacing the other. The model itself is almost the easy part.

---

## Cheat sheet — one line per stop on the story

- **Nightly batch fraud scan**: finds fraud accurately but a day too late — the money's already moved, so the real requirement is scoring inline, before approval.
- **Query-time feature scan**: recomputing a rolling feature by scanning historical rows per transaction doesn't scale — maintain it as an incrementally updated rolling aggregate instead (the odometer, not the trip log).
- **Online feature store**: low-latency key-value store fed by a stream processor, so a feature read at scoring time is a point lookup, not a scan.
- **Train/serve skew**: the most commonly silently-broken thing in ML-serving systems — fix it with one shared feature definition materialized identically online and offline, never two independently maintained "same" features.
- **Rules engine**: catches known patterns instantly and explainably, deployable in minutes — a fired rule should win over a probabilistic model score, because it represents higher-confidence, verified knowledge.
- **Shadow-mode rule rollout**: never let a new or changed rule act on real traffic the first time it runs — log what it would have decided first, promote it only after review.
- **Precision/recall trade-off**: no single threshold minimizes both missed fraud and false declines at once — it's a calibrated business decision, not a fixed engineering constant.
- **Three-way decision (APPROVE/DECLINE/REVIEW)**: absorbs the genuinely ambiguous middle band into human judgment instead of forcing every score through one brittle cutoff.
- **Per-decision explainability**: log feature attribution and rules fired at scoring time, tied to a versioned snapshot — a bare score can't be disputed, audited, or reproduced later.
- **Delayed-label feedback loop**: confirmed fraud labels arrive days to weeks later via chargebacks, which bounds retraining cadence — and is exactly why the rules engine is the system's only fast-response mechanism, not a legacy stopgap.
- **Circuit breaker to rules-only**: never fail open to always-approve, and never stall payments waiting on a dead dependency — degrade to a defined, pre-agreed, visibly-logged fallback instead.
- **The meta-lesson**: every fix in this story buys one property (timeliness, throughput, feature correctness, fast reaction to known patterns, safe rollout, calibrated trade-off, auditability, honest retraining cadence, or graceful degradation) by spending something else — say the trade in the same sentence you propose the fix.
