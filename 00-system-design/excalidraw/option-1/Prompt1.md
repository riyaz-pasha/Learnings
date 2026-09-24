━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 1 — ARCHITECTURE THINKING
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

You are a Staff-level Distributed Systems Architect.
Design: [SYSTEM NAME]
Goal: deep reasoning for interview prep. No diagrams. Pure thinking.

For each step below, go deep. Don't summarize. Reason out loud.
Show your thinking, not just conclusions.

─────────────────────────────────────────────────────────
STEP 1 — PROBLEM FRAMING
- What problem does this solve? What breaks without it?
- Who are the users? What do they actually experience?
- What are the key operations? (reads vs writes vs events)
- Real-world examples of companies running this at scale

─────────────────────────────────────────────────────────
STEP 2 — FUNCTIONAL REQUIREMENTS
List 8–12. For each one write:
  • Requirement description
  • Concrete example scenario (make it real)
  • Priority: P0 (system breaks without it) | P1 (degrades without) | P2 (nice to have)
  • Hidden complexity: what makes this harder than it looks?

─────────────────────────────────────────────────────────
STEP 3 — NON-FUNCTIONAL REQUIREMENTS
For each NFR, provide:
  • Target metric with numbers (not "low latency" — say "P99 < 200ms")
  • What breaks if this is violated (user impact)
  • Which part of the architecture is most responsible for meeting it

Cover: Availability, Latency (P50 + P99), Throughput, Durability,
Consistency model, Idempotency, Security, Observability, Multi-region.

─────────────────────────────────────────────────────────
STEP 4 — CAPACITY ESTIMATION
Show all math. Explain every assumption.

Calculate:
  1. Daily volume = DAU × actions/user/day → show DAU assumption
  2. Avg QPS = daily volume ÷ 86,400
  3. Peak QPS = avg × peak multiplier → explain WHY this multiplier
  4. Read/write ratio → explain what drives it
  5. Storage per record → list every field + byte size
  6. Total storage = volume × size × retention → add replication factor
  7. Bandwidth = peak QPS × avg payload
  8. Message broker throughput = peak QPS × msg size → MB/s

Then size infrastructure:
  | Component    | Count | Why this many |
  For each: API servers, workers, broker nodes, cache, DB replicas.

Final: list the 5 most important numbers from this estimation.

─────────────────────────────────────────────────────────
STEP 5 — SIMPLEST POSSIBLE DESIGN
Design the smallest system that technically works.
Max 6 components.

Explain:
  • Why it works at low scale
  • What the rough capacity ceiling is (e.g., "breaks above ~500 req/s because...")
  • What you'd tell an interviewer: "I'm starting simple to establish a baseline"

─────────────────────────────────────────────────────────
STEP 6 — BOTTLENECK IDENTIFICATION
Take the simple design. Be brutal. Find everything that breaks at scale.

For each bottleneck:
  • Name it precisely (not "DB is slow" → "single-writer PostgreSQL 
    hits ~5k TPS ceiling under write-heavy notification insert workload")
  • Scale threshold: at what volume does it appear?
  • Root cause: technical reason it fails
  • Category: CPU-bound | I/O-bound | Network-bound | State-bound | 
    Dependency-bound | Coordination-bound

─────────────────────────────────────────────────────────
STEP 7 — PROGRESSIVE ARCHITECTURE EVOLUTION
Fix bottlenecks one at a time. 5–7 versions.

For each version:
  • Version label + one-line summary
  • What changed (specific components added/removed/changed)
  • Which bottleneck from Step 6 this fixes
  • The exact mechanism: WHY does this fix it technically?
  • New trade-off introduced (every improvement has a cost)
  • New failure mode introduced (every addition can fail)
  • Scale ceiling of THIS version before next evolution is needed

─────────────────────────────────────────────────────────
STEP 8 — DEEP DIVES (3 most important FRs)
Pick the 3 hardest/most interesting FRs from Step 2.

For each:
  A. User Journey
     - What does the user actually do?
     - What do they experience during failure?
     - Latency budget: how long should each step take?

  B. Internal Flow (step by step)
     - Every service touched, in order
     - Every DB read/write (specify table + operation)
     - Every cache interaction (hit vs miss path)
     - What happens on failure at each step?
     - Critical path: which steps are on the latency-critical path?

  C. Data model for this FR
     - Tables/collections needed
     - Key fields + types + constraints
     - Most important indexes and why
     - Most common query pattern (write it as actual SQL/query)
     - Schema decision: why this structure over alternatives?

─────────────────────────────────────────────────────────
STEP 9 — FAILURE HANDLING
For each failure category, be specific:

  Client Errors → validate where? return what? no retry.
  Provider/External Failures → circuit breaker thresholds, fallback providers
  Internal Service Failures → saga pattern? compensating transactions?
  Infrastructure Failures → Kafka partition down, DB primary fails, cache eviction

For retries specifically:
  • Max attempts + backoff formula (write the actual formula)
  • What errors are retryable vs not (give examples of each)
  • Idempotency: how exactly is duplicate detection implemented?
    (hash of what fields? stored where? TTL?)
  • DLQ: what triggers DLQ? what does ops do with it?

─────────────────────────────────────────────────────────
STEP 10 — NFR COVERAGE ANALYSIS
For each NFR from Step 3, walk through the final architecture and explain:
  • Which component(s) are responsible for meeting this NFR
  • What would cause it to be violated in production
  • Any remaining gap (honest assessment)

─────────────────────────────────────────────────────────
STEP 11 — MULTI-REGION STRATEGY
  • Active-active vs active-passive — which and why for THIS system?
  • Which data is globally consistent vs eventually consistent?
  • Replication: what is replicated, what protocol, what lag is acceptable?
  • Failover: exact sequence of events when Region A dies
  • Data sovereignty / compliance implications

─────────────────────────────────────────────────────────
STEP 12 — TRADE-OFF DECISIONS
For each major technology choice, use this format:

  Decision: [what was chosen]
  Over: [main alternative]
  Because: [3 specific technical reasons]
  Costs: [what you give up]
  Revisit if: [condition that would make you switch]

Cover: DB choice, async vs sync, cache strategy, 
consistency model, broker choice, retry strategy.

─────────────────────────────────────────────────────────
STEP 13 — INTERVIEW STRATEGY
  • 30-second verbal summary of this system (practice saying this)
  • The 3 hardest questions an interviewer will ask + your answer
  • What most candidates miss about this system
  • What separates a Senior answer from a Staff answer for this design
  • Key numbers to memorize

─────────────────────────────────────────────────────────
OUTPUT FORMAT FOR PHASE 1:
Structure your output with clear headers matching Steps 1–13.
Be verbose. Depth matters more than brevity here.
This document will be fed into Phase 2, so make every section 
self-contained and precise.
