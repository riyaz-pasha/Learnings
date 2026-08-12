# "Interview-Version" Prompt — P0-scoped, version-ladder companion doc

A reusable prompt for turning any system-design problem into an **Interview-Version** companion doc: not a narrative, not a learning rewrite of an existing guide — a doc built specifically so you can (a) evaluate whether a proposed architecture actually satisfies the P0 requirements, and (b) answer "how does that work under the hood" in an interview with no assumptions left unstated.

**Core mental model:** scope to P0 functional requirements only, then build the architecture as a strict version ladder — v1 (naive) → v2 → ... → vN — where **every version exists because the previous version's drawback demanded it**. The version count is **not fixed** — evolve as many versions as the problem's P0 flows genuinely require, don't pad and don't truncate. Each version must be detailed enough to survive not just an interviewer's follow-up but your own later "wait, how does that actually work?" — which is where most real gaps in a first pass get found (that's what the audit pass below exists for).

**Style lesson baked into this version:** the first real run of this prompt produced technically correct but dense output — long sentences chained together with multiple em-dashes, parentheticals nested inside parentheticals, jargon strung end to end. It read as "hard English," not something a person could scan quickly. Depth and plain language aren't actually in tension; that first draft was optimizing for cramming information into fewer sentences instead of writing sentences a person can read at normal speed. The style rule below is now the first instruction inside the prompt, not an afterthought — it outranks everything else if they ever conflict.

---

## The prompt (verbatim, swap `{PROBLEM}` and `{OUTPUT}`)

> Design `{PROBLEM}` as an **Interview-Version** companion doc — not a narrative, not a learning rewrite. The audience is me, about to walk into an interview, and I need to be able to (a) evaluate whether a proposed architecture actually satisfies the P0 requirements, and (b) answer "how does that work under the hood" with no assumptions left unstated.
>
> **Write it in plain, human English — this rule outranks every other instruction below if they ever conflict.** Short sentences. One idea per sentence. Explain it the way you'd talk someone through it out loud, not the way a spec is written. Concretely:
> - If a sentence needs two em-dashes and a parenthetical to say one thing, that's not "precise," it's overloaded — split it into two or three plain sentences instead.
> - Never nest a parenthetical inside another parenthetical. If the aside needs its own aside, it's a new sentence.
> - Say a stacked string of qualifiers once, plainly, instead of piling on ("genuinely," "explicitly," "deliberately," "necessarily" in the same sentence is a sign to cut, not season further).
> - Prefer the everyday word over the formal one — "use" not "utilize," "show" not "demonstrate," "because" not "given that."
> - Read each paragraph out loud in your head as you write it. If you'd run out of breath before the sentence ends, it's too long — break it.
> - Being exact and being easy to read are not in tension. Cut the sentence, don't cut the accuracy.
>
> **Scope discipline:** open by scoping to **P0 functional requirements only**. State the scoping line the way you'd say it to an interviewer. Answer every clarifying question the interviewer would reasonably ask (limits, consistency needs, single-region vs. global, read:write ratio, etc.) with a concrete assumed answer up front — these answers must then be honored consistently by every later version, not contradicted, and later derived numbers (e.g. a chunk-size policy, a partition count) should visibly trace back to them rather than appearing as disconnected magic numbers.
>
> **Requirements → capacity estimation → API design**, worked with real formulas and real numbers (state assumptions before crunching, show the formula symbolically then plug numbers), before any architecture. Fill in every requirement completely — no empty parenthetical placeholders, no "TBD." Sanity-check headline numbers against what later versions imply — e.g. if a "request rate" figure is really a session-start rate and a later version reveals each session fans out into many downstream requests, say so explicitly; a number that quietly undercounts by an order of magnitude is worse than not having it, because it hands the interviewer a free "are you sure about that?" you should have pre-empted.
>
> **Then build the version ladder, v1 → vN — however many versions the problem genuinely needs:**
> - v1 is always the naive/no-scaling version.
> - Every subsequent version changes **exactly one thing**, motivated by an explicit drawback stated at the end of the previous version. **Every non-final version must end with an explicit drawback** — if a version's tradeoff paragraph doesn't naturally produce one, that's a sign the version boundary is wrong, not a reason to skip it.
> - Group versions into coherent threads (e.g. fully close out the "write path" story before moving to the "read path" story, then scale, then optimize) rather than interleaving unrelated concerns — call out the grouping logic once, up front.
> - **When a later version changes the meaning or behavior of something an earlier version already established** (a status that used to be terminal now isn't, an endpoint's response shape changes, a step that used to be synchronous becomes async), say so explicitly as a deliberate redefinition — name what changed and why — rather than letting the two versions quietly disagree. This is the single most common defect in a first draft: two versions each internally consistent, contradicting each other.
> - For each version, include only what actually changed from the prior version (say "unchanged" rather than re-deriving from scratch):
>   1. **Architecture diagram** — a mermaid flowchart, cumulative across versions (reuse the same node names/IDs version to version), with new/changed components and edges visually distinguished (e.g. a `new` classDef highlight + 🆕-labeled edges) from carried-over ones (`existing`/greyed). This is the single most important diagram — it must make "what did this version actually add" visually obvious at a glance, not just describable in prose.
>   2. **Exact call flow** — one or more mermaid sequence diagrams for the relevant flow(s), every hop named (client → gateway → service → queue/DB/blob/cache/CDN), not abstracted away. Once a version establishes an access pattern (e.g. "the client talks to storage directly, not through our service"), every later sequence diagram must keep honoring it unless a version explicitly changes it — don't let a later diagram quietly collapse back to an abstracted "the server fetches it and returns it" shape out of convenience.
>   3. **DB/queue/cache schema delta** — only the new/changed tables, columns, queue message shape, or cache key format for *this* version; keep it minimal, not a full re-dump. Any uniqueness/idempotency claim made in prose ("this is an idempotent upsert on X") must be backed by an actual constraint in the schema table (e.g. `UNIQUE (video_id, resolution)`) — a schema table that only lists columns with no keys/constraints/indexes is incomplete, not simplified.
>   4. **For anything generated/produced during this version** (a derived file, a manifest, a computed artifact) **state explicitly where it physically lives, how it gets there mechanically, and how it's discovered later** — don't leave "where does this actually get stored" implicit just because a pointer to it exists somewhere in the schema.
>   5. **Topic/message detail whenever a queue or event stream is introduced** — name the actual topic/queue, the producer, the consumer(s)/consumer group(s), delivery semantics (at-least-once vs. exactly-once) and what that implies for the consumer's write (idempotent upsert, dedup-by-event-id), the actual broker-level routing/partition key (and if using a log-broker, keep this distinct from any field inside the payload — don't put a "partition_key" field inside a JSON value when the real partitioning key is set at the record level), and a retry/dead-letter policy for anything that can fail. Distinguish **command topics** (imperative — "do this job") from **event topics** (past-tense fan-out — "this happened," multiple independent consumer groups) if both exist; they have different retention/consumer semantics.
>   6. **"Under the hood" depth** — for any mechanism a real interviewer would drill into, spell out the concrete mechanics and real numbers/constraints — not just the name of the concept. This is the part that separates "I know the buzzword" from "I can answer confidently." If you're not sure whether a real system's constant/limit is exact, mark it as an approximation rather than stating it as fact.
>   7. **Tradeoff and drawback** — one line on why this version is worth its added complexity, and one line stating the concrete drawback/limitation that forces the *next* version (skip only for the final version).
> - Use mermaid diagram **variety where it fits the content**, not sequence diagrams everywhere: a `stateDiagram-v2` for any lifecycle/status field once it's introduced, an `erDiagram` for a consolidated schema view, a `flowchart` for architecture and for decision logic, `sequenceDiagram` for call order. Pick the diagram type the content actually calls for.
>
> **After the version ladder:**
> - A brief "if time permits" section for the P1+ features scoped out at the start — one paragraph each on how they'd extend this architecture, not full designs. Wire it to any event topic already established (e.g. "the search indexer just subscribes to the lifecycle-events topic from vN") rather than inventing a parallel, disconnected mechanism.
> - A **consolidated schema** (one `erDiagram`) combining every table introduced across all versions.
> - A **technology-choices section** — for every distinct storage/messaging/caching tier used across the whole ladder (DB, queue, cache, blob store, and anything else), name the actual real-world technology (not "a relational DB" — say which one), and justify it against **this problem's actual access patterns as built**, not a generic reflex ("use Postgres for relational data"). Explicitly state what write/read volume actually is per the capacity estimate, and let that drive whether scaling mechanisms (sharding, clustering) are justified by throughput or by something else (row-count growth, geographic replication, etc.) — don't reach for a scaling story the numbers don't support. Name one thing deliberately *not* chosen and why, to preempt a common pattern-matching mistake for this problem's domain.
> - A **wrap-up cheat-sheet table**: one row per version — what changed / why / the drawback that motivated the next version — as a 30-second-before-the-interview refresher.
>
> Write the whole thing to `{OUTPUT}` (new file, don't touch any existing guide for this problem). Ask me whether the version ordering and grouping make sense before finalizing if the natural thread grouping is ambiguous; otherwise state your recommended ordering and proceed.

---

## The three-pass process this actually takes (don't try to do it in one shot)

**Pass 1 — structure.** Produce the full version ladder per the prompt above. This gets the shape right but will still be one level too abstract and will contain real cross-version inconsistencies — that's expected, not a failure; it's what passes 2 and 3 exist to fix.

**Pass 2 — mechanical depth.** Mine any existing reference guides/story docs for the same problem for concrete algorithms, real API constraints, real physics/latency numbers, exact thresholds — fold these into the existing versions' "under the hood" callouts. Stay disciplined about **not expanding scope** past the P0 flows already chosen.

**Pass 3 — consistency + best-practices audit.** Re-read the entire doc end to end (not just skim section headers) specifically hunting for these defect classes:

- **Cross-version contradictions** — does a later version's diagram/prose ever disagree with an earlier version's about the same fact (a status transition, a response field, an access pattern)? If a later version changes something, is that change *named* as deliberate, or does it read like an accident?
- **Prose claims a constraint the schema doesn't have** — any "idempotent," "unique," "exactly one" claim in prose needs an actual `UNIQUE`/PK constraint or index in the schema table next to it.
- **An established pattern silently broken** — if version N established "the client talks to X directly," does every later sequence diagram still honor that, or did one collapse back into an abstracted proxy shape for convenience?
- **A cache/pointer conflated with its source of truth** — when two different things are both "cached" or "referenced," do they each have their own correct fallback/source-of-truth path, or did the doc quietly merge two sources of truth into one arrow?
- **Queue/topic modeling errors** — is a partition/routing key modeled as an actual broker-level key, or mistakenly placed as a field inside the payload? Is there a retry/DLQ story for anything that can fail? Do at-least-once consumers have a dedup mechanism?
- **A headline number that undercounts** — does an early capacity estimate quietly ignore a multiplier a later version reveals (e.g. one logical request fans out into many downstream requests)?
- **Missing explicit drawback** — does every non-final version actually end with one, or did one slip through without it?
- **A derived constant not tied back to its source** — does a chosen size/count/threshold trace back to an earlier stated limit, or does it look picked out of thin air?

Fix everything found, then re-verify the doc is structurally sound (matched mermaid code fences — `grep -c '^```'` should be even — and matched `<details>`/`<summary>` tags if any are used).

**Signal to watch for in any later conversation about the doc:** when a pointed clarifying question comes up about a specific mechanism ("does X need to know Y upfront?", "where does Z actually get stored?", "I got confused because section A said one thing and section B implied another") — treat that as a live signal there's a real, fixable gap in the doc, not just a question to answer in chat. Answer it directly and completely first, then locate and fix the corresponding gap in the doc itself, then re-verify fence balance. This is how most of the real depth in a finished doc actually gets built — the first pass gets you ~70% there, and a handful of "wait, how does that work" questions catch the rest.

---

## Naming convention

`<N>-<Problem-Name>-FAANG-Guide-Interview-Version.md`, sibling to the existing `<N>-<Problem-Name>-FAANG-Guide.md` in this folder, if that numbered guide already exists for the problem — otherwise `Interview-Version.md` in the problem's own folder under `system-design/HLD/`.

This is a **separate, non-overlapping treatment** from the other prompt families in this course:
- `Prompt.md` / `Prompt_v2.md`–`Prompt_v6.md` (repo root of `HLD/`) — general HLSD-answer prompts, not version-ladder-specific.
- The "-v2" rewrite-for-learning treatment and the "-story" narrative problem→fix treatment used elsewhere in `0-course/` — both make an *existing* guide easier to learn from; this prompt instead produces a *new*, interview-recital-focused doc from scratch.
