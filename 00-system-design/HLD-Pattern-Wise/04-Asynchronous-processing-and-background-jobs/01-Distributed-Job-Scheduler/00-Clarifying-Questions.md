## 1. Pattern and main design forks

My interpretation: a service where clients register one-off (delayed) and recurring (cron) jobs, and the system fires each one at the right time, reliably, at scale. It is not a full workflow/DAG engine unless the interviewer says so.

- **Pattern:** Scheduler (timer + dispatch), with multi-tenant traits
- **Main forks:**
  - Trigger-only (call a webhook or queue) vs execute user code on our workers
  - At-least-once + idempotent consumers vs exactly-once effect
  - Independent jobs vs jobs with dependencies (DAG)
  - Second-level precision vs minute-level tolerance
  - Single-leader scheduler vs sharded, partitioned schedulers (and single vs multi-region)

## 2. Clarifying questions

| # | Bucket | Priority | Question, as I would say it out loud | If A, then... | If B, then... | Design decision it drives |
|---|---|---|---|---|---|---|
| 1 | S | P0 | "I'll assume core features are: create one-off and cron jobs, cancel or update them, and check status. No job dependencies or DAGs. Is that right?" | A: no DAGs. Jobs are independent rows, and I draw a simple timer + dispatcher. | B: DAGs needed. I add a dependency graph and a run-state machine. | Scope of the data model and orchestration |
| 2 | S | P0 | "Does the scheduler run the job code itself, or only trigger something, like an HTTP callback or a queue message?" | A: trigger-only. I draw a thin dispatcher with retries. | B: runs code. I add a worker fleet, isolation, resource limits and log capture. | Whether I build an executor tier |
| 3 | C | P0 (invariant) | "The one thing that must never go wrong: a job must never be lost, and I'll assume a duplicate run is tolerable if jobs are idempotent. Or is a double run harmful, like charging twice?" | A: duplicates OK. I use at-least-once delivery with leases and retries. | B: duplicates harmful. I add idempotency keys, fencing tokens and an atomic claim-and-commit. | Delivery semantics and locking |
| 4 | C | P0 | "If a recurring job is still running when its next time arrives, should we skip, queue, or run in parallel? And after downtime, do we catch up on missed runs or skip them?" | A: skip, no catch-up. I keep minimal per-job state. | B: queue or catch-up. I track the last-fired time and a backlog per job. | Per-job state and misfire policy |
| 5 | A | P1 | "Many jobs will fire at the same moment, like midnight or the top of the hour. Is that a real spike, and is any one tenant much heavier than the others?" | A: yes, heavy spikes. I spread load with time-bucket partitions and jitter, and pre-load upcoming buckets. | B: smooth load. A simple partitioned poll is enough. | Hot partitions and spike handling |
| 6 | L | P1 | "I'll assume 100M stored jobs, about 1K fires per second on average, and a 10x peak. Does that sound right?" | A: that scale. I shard the job store by time and job ID, with many scheduler nodes. | B: much smaller. One DB and a few nodes are enough. | Sharding vs single store |
| 7 | L | P1 | "How exact must the timing be? I'll assume a job fires within about 1 second of its schedule." | A: ~1s. Schedulers pre-load the next window into memory and dispatch from it. | B: minutes OK means plain DB polling. Sub-100ms means an in-memory timer wheel on pinned shards. | Polling vs in-memory timers |
| 8 | L | P1 | "For availability, I'll assume the scheduler is 99.99% available, with no single point of failure and failover under 30 seconds. Okay?" | A: yes. Leases or leader election per shard, plus replicated storage. | B: relaxed. A single active node with a standby is fine. | Failover and replication design |
| 9 | E | P1 | "Is this a shared platform for many teams or customers? If so, do we need quotas and fairness so one tenant can't starve the rest?" | A: multi-tenant. I add per-tenant rate limits and fair queues. | B: single tenant. A single global queue. | Isolation and fairness |
| 10 | D | P1 | "I'll assume jobs run seconds to minutes, up to an hour. If a worker dies or the target fails, we retry about 3 times with backoff, then move the job to a dead-letter queue. Fine?" | A: yes. Visibility timeout with retries plus a DLQ. | B: long-running or hours-long jobs. I add heartbeats, lease renewal and resumable state. | Retry and failure handling |

**How I'd say it, in two spoken blocks:**

- **Block 1 (scope and correctness, Q1–4):** "Let me confirm scope first. I'll assume users create one-off and cron jobs, and can cancel and check status, with no DAGs. Does the system run the job itself or just trigger a callback? The key rule is that no job is lost, and I'll tolerate duplicates if jobs are idempotent. Is that okay? And if a run overlaps the next one, or we miss runs during downtime, skip or catch up?"
- **Block 2 (scale, environment, failure, Q5–10):** "On scale, I'll assume 100M jobs, 1K fires per second on average, 10x at peak, and about 1-second accuracy, with spikes at the top of the hour. I'll aim for 99.99% availability. Is it multi-tenant? And I'll assume 3 retries then a dead-letter queue. Does that match what you have in mind?"

## 3. Assumptions to state out loud

- Times are stored in UTC, and cron jobs carry a timezone. *If wrong (e.g., DST-exact local-time rules matter): I need a timezone-aware next-run calculator.*
- Job payload is small (under 64 KB). *If large: store payloads in blob storage and keep only references.*
- All priorities are equal. *If priorities matter: I add priority queues and preemption.*
- Run history is kept for 30 days. *If longer: I add archival storage and a cold query path.*
- Single region, with cross-region disaster recovery (RPO of minutes, RTO under 15 minutes). *If active-active multi-region: I need a global ownership scheme to avoid double fires.*
- Authentication and authorization are handled by a standard gateway. *If not: I add an identity and permission layer.*
- Read traffic (status lookups) is small compared with fire traffic. *If it is heavy: I add a read replica or cache.*
- Jobs are not deleted mid-run. Cancel only stops future runs. *If hard-kill is needed: I need a cancel signal to workers.*

## 4. Questions I deliberately did NOT ask

- **"Which database or queue should we use?"** It is answer-seeking, so I must decide it myself, and it breaks Rule 3.
- **"Do we need cron syntax support?"** It is obvious from "scheduler", and it doesn't change the architecture. The next-run calculation is the same box either way.
- **"What are the API request fields and response format?"** This is low-level detail for a later step, and it doesn't change the high-level design.
- **"Should it be secure and have monitoring?"** These are obvious and belong in assumptions.

## 5. Interviewer cheat sheet

| # | Good answer | Tricky answer | How the design changes |
|---|---|---|---|
| 1 | "No DAGs, just independent jobs." | "Some jobs depend on others finishing first." | Add a run-state machine and a dependency tracker that releases downstream jobs on success. |
| 2 | "We just call the customer's HTTP endpoint." | "We run arbitrary customer code or containers." | Add a sandboxed worker pool, a resource scheduler, and log and artifact capture. |
| 3 | "Duplicates are fine; consumers are idempotent." | "Payment jobs. A double run costs money." | Use idempotency keys, fenced leases, and an atomic claim-then-commit so one run's effect is recorded once. |
| 4 | "Skip if still running, and skip missed runs." | "Run every missed slot in order after an outage." | Keep a per-job cursor and a backlog queue with ordering and catch-up rate limits. |
| 5 | "Spikes at midnight, about 5x." | "Everyone fires at 00:00:00, a 100x spike." | Add jitter or opt-in spreading, pre-warmed worker capacity, and sub-sharding of the hot bucket. |
| 6 | "Matches your numbers." | "10x larger: 1B jobs, 100K fires per second." | More shards, a tiered store (hot window in memory, cold on disk), and a larger scheduler fleet. |
| 7 | "Within 1 to 5 seconds." | "Under 100 ms for some jobs." | Move to an in-memory timer wheel per shard with pinned ownership, and tighten failover. |
| 8 | "99.99%, failover in 30 seconds." | "99.999%, no missed fires even during failover." | Use replicated state with warm standbys per shard and overlap-safe takeover, which needs strong dedupe. |
| 9 | "Multi-tenant, with fair share." | "A big tenant wants a guaranteed throughput SLA." | Add reserved capacity per tenant, weighted queues, and admission control. |
| 10 | "Seconds to minutes, 3 retries, then DLQ." | "Jobs run for 6 hours and can't restart from scratch." | Add heartbeats, lease renewal, and checkpointing, with resume on another worker. |

## 6. Board summary template

**FUNCTIONAL**
- Create one-off and recurring (cron) jobs
- Cancel or update jobs
- Check job status and run history
- Dispatch jobs at their scheduled time (trigger a callback or queue message)

**NON-FUNCTIONAL**
- Scale: 100M stored jobs, about 1K fires per second on average, 10x peak
- Timing: fires within about 1 second of schedule
- Availability: 99.99%, failover under 30 seconds
- Durability: no scheduled job lost (RPO of minutes in disaster recovery, RTO under 15 minutes)
- Multi-tenant fairness through per-tenant quotas

**INVARIANT**
- A scheduled job is never lost.
- Delivery is at-least-once, and jobs must be idempotent.
- No two workers run the same slot at the same time (lease-based).

**ASSUMPTIONS**
- No DAGs
- Trigger-only dispatch
- Skip overlapping or missed runs for recurring jobs
- Jobs run seconds to minutes, up to 1 hour
- 3 retries with backoff, then a dead-letter queue
- UTC storage with a per-job timezone
- Payload under 64 KB
- 30-day history retention
- Single region with DR
