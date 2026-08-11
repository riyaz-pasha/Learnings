# Design an AI Code Assistant — The Story (narrative edition)

> **What this file is.** The reference file, `45-Design-an-AI-Code-Assistant-FAANG-Guide.md`, is
> the one to recite from — requirements, API shapes, every deep dive, the master cheat sheet. This
> file is a second way in: the same material as one continuous story, told in plain language.
> Engineers at a company keep hitting a wall, patch it, and the patch itself creates the next wall
> — until we land on the exact same design the reference file documents. The company, **CodeCove**
> (a small dev-tools startup building a VS Code ghost-text extension called **Cove**), is
> fictional. But every wall it hits is something a real, named system actually does: **GitHub
> Copilot's** documented sub-100ms ghost-text latency target and fill-in-the-middle prompting,
> **Cursor's** incremental codebase-indexing approach, and the secret-scanning/push-protection
> style gate real Copilot tooling ships. I'll flag any stand-in number with `[illustrative]`.

**The one-sentence core idea:** an AI code assistant is really three systems wearing one UI — a
disposable-request, sub-100ms prediction loop for inline ghost text; a repo-context retrieval
pipeline that must stay fresh without ever re-scanning the whole codebase; and a zero-tolerance
safety gate — and nearly every decision below exists to keep those three from fighting each other
for the same compute.

---

## Chapter 1 — The ghost text that arrives after you've already moved on

CodeCove is eight months old, 4,000 beta developers. Version one does the simplest thing possible:
every pause, even mid-word, it sends the whole current file to one large hosted model (frontier
chat-model weight class), synchronously, and waits. No caching, no cancellation, no fast path.

Real number: that large model's honest time-to-first-token is **~700-900ms** `[illustrative]`.
The bar this whole category is actually judged against — the one GitHub has publicly targeted for
Copilot's ghost text — is roughly **p50 < 60ms, p90 < 100ms**. CodeCove is **8-15x too slow**.

Concrete moment: a developer types `def calculate_monthly_total(orders):`, pauses to think, and by
the time a suggestion streams back 850ms later she's typed four more characters. The ghost text
renders anyway, for a cursor position that no longer exists. A third of the beta channel disables
the extension in week one.

*Why so slow?* Two stacked reasons: a model sized to write an essay is the wrong tool for
"predict six words" — bigger models are inherently slower to first token — and nothing
short-circuits anything, so every pause fires a full fresh request regardless of how small the
edit was.

**The fix, and the analogy for this whole story:** *right-size the kitchen.* A five-star kitchen
(the large model) can cook anything but takes twenty minutes to plate a dish. Ghost text needs a
**drive-thru window**: small, fast, limited menu, order out in under a minute. Swap inline
completion onto a small, fast model.

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant Ext as Cove extension
    participant Big as Large model (fine-dining kitchen)
    Dev->>Ext: pauses after typing
    Ext->>Big: full file + "what comes next?"
    Note over Big: 700-900ms to first token
    Big-->>Ext: suggestion, finally
    Note over Dev: already typed 4 more characters
    Ext-->>Dev: ghost text for a cursor position that no longer exists
```

**New problem:** the drive-thru is fast per order now — but a customer still shouts a new order
through the window on *every keystroke*, including ones overwritten a second later. Cheaper
kitchen, same habit of cooking meals nobody eats.

**Say this in an interview:** "The naive version calls one big model synchronously on every
keystroke — wrong model, full stop. First fix: route inline completion to a small model sized for
a 100ms budget. That alone doesn't stop wasted work, it just makes each wasted order cheaper."

---

## Chapter 2 — Five orders placed for one meal

Small model in place, per-request latency looks fine (~70ms median). But nothing changed *when* a
request fires. Typing `"total"` — five keystrokes, ~200ms apart — fires **five separate
requests**, one per keystroke.

Number: 4,000 beta developers, roughly one request per keystroke while typing, produce
**~15,000-20,000 requests/sec** at peak `[illustrative]` against a fleet sized for far less. GPUs
pin at 100% by lunchtime on launch day; real completions queue behind requests for keystrokes
already overwritten.

*Why cook an order for every keystroke, including ones the customer immediately changes?* Because
nothing waits to see if the customer is actually done ordering first.

**The fix:** *don't submit the order until the customer stops talking.* **Debounce** — a timer
(CodeCove picks 300ms) starts on the first keystroke; every keystroke before it fires **resets**
it. Only a real pause sends a request at all.

```mermaid
stateDiagram-v2
    [*] --> Idle
    Idle --> Waiting: keystroke
    Waiting --> Waiting: another keystroke before timer fires (RESET)
    Waiting --> Fired: 300ms pass, no new keystroke
    Fired --> Idle: request sent
```

Worked example: typing `"total"` at ~200ms/keystroke — every gap is shorter than the 300ms window,
so the timer keeps resetting and never fires mid-word: five keystrokes, **zero** requests during
the burst. Only the pause afterward fires **one** request for the whole word.

**New problem:** debounce stops the client from *sending* a wasted order. It says nothing about an
order already **cooking** when the customer changes their mind mid-flight — a very ordinary thing
for a typist to do.

**Say this in an interview:** "Debounce is 'wait for a pause before you place the order' — kills
most wasted requests before they're sent. It can't help a request that already left before the
next keystroke landed. That's a separate mechanism, and it's next."

---

## Chapter 3 — The order that's already cooking when you change your mind

GPU spend drops after debounce, but not as much as the "~90% fewer requests" math predicted.
Digging in: of the requests that *do* fire, roughly **two-thirds** answer a prefix that's already
stale by the time the model replies, because the developer resumed typing within the ~70-100ms the
model takes. The kitchen doesn't know the customer walked away — it keeps cooking regardless.

*If debounce already stopped most bad requests, why is spend still high?* Because debounce only
protects the moment *before* sending. Once sent, the kitchen has started cooking, and stopping a
dish mid-cook needs someone to actively say "stop" — nothing does that yet.

**The fix:** the customer calls back and cancels the order already cooking. Every request carries
a **cancellation token**; the instant a newer keystroke fires a new request, the extension tells
the server to abort the old one — not ignore its reply, actually stop generating tokens, now.

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant Ext as Cove extension
    participant Fast as Fast-tier model (drive-thru)
    Dev->>Ext: pause after "total = " -- debounce fires
    Ext->>Fast: request r1 ("total = ")
    Note over Dev,Ext: developer resumes before r1 replies
    Dev->>Ext: types "0"
    Ext->>Fast: cancel(r1) -- stop cooking now
    Ext->>Fast: request r2 ("total = 0")
    Fast--xExt: r1 aborted, no more tokens
    Fast-->>Ext: r2 completes
```

**Two mechanisms, two jobs:** debounce stops sending; cancellation stops finishing an already-sent
request. Skip cancellation and a fast typist who pauses-then-resumes quietly costs you back the
GPU time debounce was supposed to save.

**New problem:** the kitchen now stops on cancel, but everything it *does* finish only knows code
**before** the cursor. Inserting a line in the middle of an existing function ignores what's
already written after it — and starts duplicating it.

**Say this in an interview:** "Debounce and cancellation are two mechanisms, not one feature with
two names. Debounce cuts what's sent; cancellation cuts what's finished once sent. You need both,
or the capacity math from Chapter 2 quietly comes back."

---

## Chapter 4 — The suggestion that duplicates a line two lines down

Demo goes sideways. A prospect places the cursor inside `for order in orders:`, with
`return total` already two lines below, untouched. Cove suggests `total += order.amount` followed
by **another** `return total` — duplicating code that already exists, on camera. Cove has been
sending only text **before** the cursor, like reading a book with your hand covering every page
after the one you're on.

*How do we let the model see what's after the cursor without shipping the whole file?* Split the
file at the cursor and hand over both halves, telling the model explicitly: only fill this gap.

**The fix, a new analogy:** **fill-in-the-middle (FIM)** — exactly a **cloze test**, the
fill-in-the-blank exercise from school: given the sentence before *and* after the blank, supply
only the missing word. GitHub's own reporting attributes a real, documented **~10% relative
quality lift** to FIM.

```mermaid
flowchart LR
    A["Raw file, cursor at |"] --> B["Split at cursor"]
    B --> P["Prefix -- before"]
    B --> S["Suffix -- after"]
    P --> T["Cloze prompt:\n&lt;PREFIX&gt;...&lt;SUFFIX&gt;...&lt;MIDDLE&gt;"]
    S --> T
    T --> M["Model fills ONLY the middle"]
    M --> R["Splice prefix + middle + suffix"]
```

With the suffix visible, the model knows `return total` already exists downstream and generates
just `total += order.amount` — no duplicate, no dead code.

**New problem:** FIM fixes blindness *inside* the open file. It does nothing for a helper function
that lives in a teammate's file the developer doesn't have open — the model has never seen it and
never will.

**Say this in an interview:** "FIM is prefix plus suffix plus a token telling the model 'fill in
exactly this gap' — why completions insert cleanly instead of only ever appending. It fixes
blindness within one file, not blindness across files — that's the next wall."

---

## Chapter 5 — The helper function nobody told the model about

First serious customer, real multi-service codebase. The right completion for
`def calculate_total(orders):` calls a `discount_rate()` helper living in a different file the
developer doesn't have open, written by a teammate three days ago. Cove's model has never seen it
and reimplements discount math from scratch, badly — the customer notices in week one.

*How does the model see code it's never had open, across a repo that changes daily, without
stuffing the whole codebase into every request?* Build a searchable map ahead of time; pull in
only what's relevant per request.

**The fix, a new analogy:** a **repo-wide vector index** — a **librarian who's read every book on
every shelf** and points you to the right paragraph by *meaning*, not exact words. Every
function-sized chunk becomes an embedding; a query searches the nearest matches in milliseconds.

```mermaid
flowchart LR
    subgraph Sources["Context merged per request"]
        OF["Open files (free)"]
        RE["Recently edited (cheap)"]
        VI[("Librarian's index --\nrepo-wide vector search")]
    end
    OF --> CTX["Merged, bounded context"]
    RE --> CTX
    VI --> CTX
    CTX --> P["FIM prompt: prefix + suffix\n+ librarian's top matches"]
```

Context now merges three sources: open file (free), recently edited files (cheap), librarian's top
repo-wide matches (expensive). The top match for `calculate_total` is now correctly
`discount_rate()`.

**New problem:** a librarian who read every book once, on day one, is only useful until someone
writes a new book. This repo gets pushed dozens of times a day — an index built once goes stale
within a week.

**Say this in an interview:** "Prefix/suffix solves blindness inside one file. A repo-wide vector
index — embed every chunk, search by meaning, merge top hits into context — solves blindness
across the whole codebase. An index built once and never updated is stale the moment the next
commit lands."

---

## Chapter 6 — The librarian who re-reads the whole library every night

First fix for staleness: re-embed the entire repo nightly. This customer's repo is **2 million
lines**; a full re-embed takes **~40 minutes** `[illustrative]` and costs real money every single
night, changed or not — and code written *that day* isn't in the index until the next run.

*If only a handful of files change per hour, why re-read the entire library nightly?* Because the
librarian can't tell which pages changed without re-reading them — unless given a shortcut.

**The fix:** give every chunk a **content hash**. On save, re-chunk only the touched file; for each
chunk, check the fingerprint — seen before, skip it; new or changed, re-embed just that chunk. Same
idea as Cursor's actual codebase-indexing approach, and the same as **sticky notes only on the
pages that changed**.

```mermaid
flowchart TD
    A["File saved"] --> B["Re-chunk this file only"]
    B --> C{"hash(chunk) already\nin the index?"}
    C -->|"yes"| D["Skip -- reuse embedding"]
    C -->|"no"| E["Embed just this chunk"]
    D --> F["Index fresh within seconds"]
    E --> F
```

Worked number: a 2,000-line file with one function edited triggers **one** re-embedding call, not
2,000 lines of rework. Freshness moves from "up to 24h stale" to "seconds-to-minutes stale," at a
fraction of the cost, plus a periodic full rebuild as a drift safety-net.

**New problem:** the librarian is fresh now — but she's built for "point to a relevant paragraph,"
not "answer a multi-file reasoning question" or "make a change and run tests." The same small
drive-thru model is the wrong tool again, for the same reason it was wrong in Chapter 1.

**Say this in an interview:** "Content-hash each chunk, re-embed only chunks whose hash changed, on
save — never a nightly full scan, never a re-embed on every keystroke either, since mid-edit code
is often invalid anyway."

---

## Chapter 7 — Asking the drive-thru window to plan a three-course meal

CodeCove ships chat, wired straight into the existing fast-tier model because it's already
deployed. First real question: *"Where do we retry failed payment webhooks, and is the backoff
exponential?"* The drive-thru model — small, distilled, built to blurt six words in 70ms — names
the wrong file and invents backoff behavior that isn't in the code.

*Why would a model great at finishing a line be bad at a multi-file reasoning question?* Because
"fast at short local predictions" and "good at reasoning across files" are close to opposite design
goals — the model was distilled specifically to be small and quick.

**The fix, same analogy extended:** a restaurant needs a dining room too. Drive-thru (small/fast)
stays for ghost text. Chat and, later, agent mode route to the **dining room**: a larger, more
capable model, slower and more relaxed, because a real question is "wait for a proper answer," not
"flow-state typing."

```mermaid
flowchart LR
    Req["Incoming request"] --> Route{"Which mode?"}
    Route -->|"inline"| Fast["DRIVE-THRU\nsmall/fast, many cheap replicas"]
    Route -->|"chat / agent"| Big["DINING ROOM\nlarger, capable, fewer replicas"]
    Fast --> FO["&lt;100ms first token"]
    Big --> BO["1-2s+ first token"]
```

The two fleets scale on different signals — drive-thru on keystroke volume, dining room on
concurrent chat/agent sessions — and stay on **separately-scaled fleets** so a lunch rush in one
never starves the other.

**New problem:** the dining room reasons well enough that the natural next ask is "don't just
answer, *make the change*" — a much bigger trust problem than a wrong ghost-text suggestion ever
was.

**Say this in an interview:** "Never let one model own both latency budgets — route by mode, not
load, and scale the two fleets independently, because their bottlenecks — keystroke volume versus
reasoning depth — are different problems."

---

## Chapter 8 — The intern who pushes straight to main

Agent mode: give it a goal, it reads files, writes a patch, runs tests, autonomously, on the
dining-room model. First dogfood run does what was asked — and also, unasked, "cleans up" an
unrelated function two files away and pushes the whole thing straight into a feature branch, no
review. Nobody notices until CI turns red on an unrelated PR.

*If the reasoning was correct, why is this dangerous?* Because "reasoning correctly most of the
time" and "safe to apply unreviewed" are different bars — even a good agent occasionally does
something unasked, and the cost of that landing unreviewed is much higher than a wrong
ghost-text suggestion.

**The fix, a new analogy:** treat the agent like **a junior engineer who drafts a pull request,
never merges it themselves.** A bounded loop with a fixed toolset —
`read_file → propose_diff → run_tests → re-plan on failure → report` — and by default every
`propose_diff` is a **pause point**: shown to the developer, nothing touches disk until approved.

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant Agent as Agent (dining-room model)
    participant Diff as propose_diff tool
    participant Test as run_tests tool
    Dev->>Agent: goal = "add validation, add a test"
    Agent->>Diff: propose_diff(patch)
    Diff-->>Dev: diff shown -- nothing applied yet
    Dev->>Agent: approve
    Agent->>Test: run_tests()
    Test-->>Agent: 1 failed -- re-plan
    Agent->>Diff: propose_diff(revised patch)
    Dev->>Agent: approve
    Agent->>Test: run_tests()
    Test-->>Agent: all passed
    Agent-->>Dev: report -- session complete
```

An opt-in `auto-apply-with-rollback` mode exists for teams that want it, but every step is still
recorded so a whole session reverts atomically if a later step fails — the intern still can't merge
without earned trust, and even then every commit is a revertible unit.

**New problem:** review-before-apply stops a *bad idea* from landing. It does nothing to stop a
*dangerous string* from landing — a human skimming a diff for logic doesn't reliably notice one
line quietly containing a real, live-looking API key.

**Say this in an interview:** "Agent mode is a bounded tool-use loop, not an open shell — read,
propose, test, re-plan, report. By default nothing touches disk without human approval per diff,
because reasoning well most of the time isn't the same as being safe unreviewed."

---

## Chapter 9 — The suggestion that quietly contains a real API key

A security engineer flags a ghost-text suggestion (shown, never accepted) containing a string in
the exact shape of a real cloud-provider API key — reproduced from something the model saw in
training. Never accepted doesn't matter; a screenshot of that moment is a headline, and CodeCove
has zero systematic protection against it happening again, on any tier.

*Can we just tell the model not to output secrets?* No — a model can't be instructed into perfect
reliability on a zero-tolerance requirement. The only dependable fix is a separate, deterministic
check on every output.

**The fix, a new analogy:** **airport security scanning every bag, no matter which gate** — fast
drive-thru or slow dining room. Before any suggestion reaches the editor, a synchronous, blocking
scan checks: known secret-token shape? known dangerous pattern (string-concatenated SQL, disabled
TLS, weak crypto)? This is exactly the gate real Copilot tooling ships — secret scanning/push
protection plus a security-review-style pass.

```mermaid
flowchart TD
    A["Candidate suggestion,\neither tier"] --> B{"Matches a known\nsecret-token shape?"}
    B -->|"yes"| C["BLOCK -- never render as-is"]
    B -->|"no"| D{"Matches a known\nvulnerable pattern?"}
    D -->|"yes, high confidence"| E["Block or warn before accept"]
    D -->|"no / low"| F["Render normally"]
```

Two tolerances, stated out loud: a suspected **secret** — block, always, zero tolerance for false
negatives, never "show with a warning." A suspected **vulnerable pattern** — often warn and let a
human decide, since some flagged patterns are legitimate in context; this gate is
defense-in-depth, not a CI-time replacement.

**New problem:** the scanner catches a stranger's leaked secret. It says nothing about whether
*this customer's own* proprietary code stays safe from crossing into another customer's
suggestions or training data — a different kind of leak entirely.

**Say this in an interview:** "This gate is synchronous, blocking, on 100% of outputs from either
tier — the one place 'skip it for latency' is never acceptable. Secrets get zero tolerance;
vulnerable patterns get warn-and-let-a-human-decide, on purpose."

---

## Chapter 10 — The mailbox with no lock on it

Second enterprise customer, a direct competitor of the first. The vector index is one shared
table, filtered by `WHERE tenant_id = ?` in application code. A load test proves the fear right: a
retry path drops the filter, and for **four minutes**, Customer B's chat queries retrieve chunks
from Customer A's proprietary billing code. Nothing was *shown* to a human — but being
*retrievable at all* is the kind of finding that ends a contract on disclosure alone.

*Isn't `WHERE tenant_id = ?` supposed to prevent this?* Only if every code path remembers it,
forever, including retries, caches, and every future engineer who touches the query. An
application-code filter is one mistake from silently missing.

**The fix, a new analogy:** **an apartment building with a locked mailbox per unit**, not a shared
mail room trusting a clerk. `tenant_id` (and `repo_id`) becomes part of the storage
**namespace/partition key**, enforced at the storage layer — the wrong mailbox simply doesn't open
with your key, missing filter or not.

```mermaid
flowchart LR
    subgraph Before["Shared mail room (app-code filter)"]
        Q1["Query"] -->|"WHERE tenant_id = ?"| T[("One shared table")]
        T -.->|"filter missed on one path"| Leak["Wrong tenant's data handed over"]
    end
    subgraph After["Locked mailboxes (storage-layer partition)"]
        Q2["Query, tenant key baked in"] --> NS["Tenant A's namespace"]
        Q2 -.->|"physically cannot reach"| NS2["Tenant B's namespace"]
    end
```

Paired with market-standard posture: enterprise/org repos are, by contractual default, **excluded**
from training any shared model; any per-tenant fine-tuning stays scoped to that tenant's own
served model, never mixed into a shared base. Regulated prospects get an **on-prem/VPC** option —
code and embeddings never leave their network.

**New problem:** every mechanism so far — cancellation, FIM, retrieval, indexing, routing, the
gate, isolation — was solved one at a time. The real test is all of them holding up
*simultaneously*, under real load, for the first time.

**Say this in an interview:** "Tenant isolation has to live at the storage/namespace layer, not an
application-level `WHERE` clause — a filter is one missed code path from a leak; a locked mailbox
physically can't open with the wrong key. Pair it with a no-train-on-enterprise-code default and
an on-prem option."

---

## Chapter 11 — The morning 2 million developers show up at once

CodeCove lands a license covering **2,000,000 developers**. Launch morning, ~20% concurrently
active — 400,000 typing at once. At ~0.5 requests fired per active developer per second, that's
**200,000 QPS** hitting the drive-thru window, and the fleet sized for the old customer base falls
over in minutes.

*Provision for the 200,000 QPS fired, or the smaller number that renders?* The trap: roughly
**65-70%** of that get cancelled server-side before finishing (Chapter 3 working as designed) —
only **~64,000 QPS** ever render. But the kitchen still has to **start** cooking all 200,000 QPS
before any of it can be told to stop. Provisioning for 64,000 is exactly what fell over.

**The fix:** provision for requests **fired**, not **rendered** — "staff the kitchen for every
order that walks in, even the cancelled ones," as a hard capacity rule. Layer a **circuit
breaker**: past a queue-depth threshold, fail open — no suggestion — rather than queue past budget.

```mermaid
quadrantChart
    title Inline request volume: fired vs. rendered
    x-axis "Fewer requests" --> "More requests"
    y-axis "Never reaches editor" --> "Reaches editor"
    quadrant-1 "Rendered -- tempting to provision for"
    quadrant-2 "Fired -- what you MUST provision for"
    quadrant-3 "n/a"
    quadrant-4 "n/a"
    "200,000 QPS fired": [0.85, 0.25]
    "64,000 QPS rendered": [0.35, 0.75]
```

**The analogy holds:** a revolving door that loses power doesn't lock people out, it stops
spinning and people push it open manually. CodeCove's breaker works the same way — trip it, and
the editor shows no ghost text, exactly as if the assistant were off. Losing the assistant never
means losing the editor.

**Where this lands:** debounce/cancellation bound fast-tier work; two-tier routing splits
completion from reasoning; the vector index plus incremental re-embedding keeps context fresh
without a full re-scan; the gate and storage-layer isolation keep outputs safe and tenants walled
off; and provisioning for fired (not rendered) requests with fail-open on top is what keeps the
stack standing on launch morning. This is the real system.

**Say this in an interview:** "The number that matters for capacity is requests fired, not
rendered — cancellation saves finishing wasted work, not starting it. The fast path always fails
open: no suggestion beats a stale one, and a stale one beats a frozen editor."

---

## Where the story actually lands

```mermaid
flowchart LR
    A["Ch1: one big model,\nsynchronous"] -->|"fixes latency\nbreaks: fires every keystroke"| B["Ch2: debounce"]
    B -->|"fixes: fewer sent\nbreaks: in-flight still cooks"| C["Ch3: server-side cancel"]
    C -->|"fixes: no wasted GPU\nbreaks: blind past cursor"| D["Ch4: FIM"]
    D -->|"fixes: within-file\nbreaks: blind across files"| E["Ch5: repo vector index"]
    E -->|"fixes: repo context\nbreaks: index goes stale"| F["Ch6: incremental re-embed"]
    F -->|"fixes: freshness\nbreaks: one model can't also chat"| G["Ch7: two-tier routing"]
    G -->|"fixes: chat quality\nbreaks: agent edits unreviewed"| H["Ch8: propose-diff loop"]
    H -->|"fixes: human review\nbreaks: secrets still leak"| I["Ch9: security gate"]
    I -->|"fixes: safe outputs\nbreaks: cross-tenant leakage"| J["Ch10: storage-layer isolation"]
    J -->|"fixes: tenant safety\nbreaks: scale overwhelms it"| K["Ch11: fail-open capacity plan"]
```

```mermaid
mindmap
  root((Why an AI code assistant\nneeds all of this))
    Latency
      big model synchronous -- ghost text arrives too late
      small fast tier, its own budget
    Wasted work
      every keystroke fires a request
      debounce cuts sending, cancel cuts finishing
    Blindness in-file
      prefix-only duplicates code
      FIM -- cloze-test style
    Blindness across files
      helper functions elsewhere invisible
      librarian -- repo-wide vector index
    Staleness
      full re-index slow and costly
      content-hash, re-embed changed chunks only
    One model, two jobs
      fast completion vs deep reasoning conflict
      two independently-scaled tiers
    Trust in autonomy
      agent edits unreviewed
      propose-diff loop, human approves
    Safety
      secrets and vulnerable patterns leak
      synchronous scan on 100% of outputs
    Multi-tenant privacy
      app-code filter can be missed
      tenant_id baked into storage namespace
    Scale
      cancellation saves rendering, not dispatch
      provision for fired, fail open always
```

Every real production code assistant sits somewhere on this chain. The skill isn't reciting all
eleven chapters — it's stopping where the requirements say to stop. A single-player, local tool
might reasonably stop around Chapter 4 or 5. A multi-tenant SaaS product has to reach 9, 10, and
11. If nobody's mentioned agent mode, walking to Chapter 8 unprompted reads as padding, not depth.

---

## Grill me — adversarial follow-ups

**Q1: "Why not always use the small, fast model — for chat and agent too?"**
Chat and agent need to reason across files and use tools correctly, and a small distilled model
measurably underperforms there — it was shrunk specifically to be fast at short, local
predictions, a different skill from depth. You'd get fast, wrong answers instead of slower, right
ones.

**Q2: "Why not always use the large model everywhere — simpler architecture?"**
It can't hit 100ms at 200,000 QPS without a GPU fleet sized for the wrong problem — you'd be
provisioning your most expensive compute for the highest-volume, lowest-value-per-request traffic,
and every suggestion renders after the developer's already typed past it.

**Q3: "Debounce already stops most bad requests — why also cancel server-side?"**
Debounce only protects the moment before sending; it can't reach a request that already left. A
developer who pauses, triggers a request, then resumes before the reply produces exactly this
case, and only an explicit cancellation token stops the GPU work.

**Q4: "If FIM already stops duplicate code, why also need a vector index?"**
FIM only sees inside one open file. A helper in a file the developer doesn't have open is
invisible no matter how good the prefix/suffix split is — a completely separate blindness only a
repo-wide index fixes.

**Q5: "Why re-embed on save instead of on every keystroke for max freshness?"**
Mid-edit code is often syntactically invalid, so keystroke-level re-embedding is mostly wasted
work on garbage input, at enormous cost relative to benefit. Save-triggered, hash-scoped
re-embedding gets seconds-to-minutes freshness for a fraction of that cost.

**Q6: "Doesn't pausing for human approval on every diff defeat the point of an autonomous agent?"**
The point of the agent is doing multi-file reasoning and drafting automatically; the point of the
pause is making sure a wrong change never lands unreviewed. Skipping the pause is opt-in once a
team's earned that trust, and even then every step is recorded so a bad session rolls back
atomically.

**Q7: "Vulnerability false positives are annoying — won't developers just disable the gate?"**
That's exactly why secrets and vulnerabilities get different tolerances: secrets are always
blocked, zero tolerance. Vulnerable patterns are often just a warning, precisely to keep the
false-positive rate low enough that nobody learns to ignore or disable the whole gate.

**Q8: "Isn't `WHERE tenant_id = ?` good enough if the code is reviewed carefully?"**
It's good enough until exactly one path — a retry, a cache, a future one-line change — forgets it,
and then it silently isn't. Baking the tenant key into the storage namespace means no code path
can physically retrieve the wrong tenant's data, missed filter or not.

**Q9: "Provisioning for 200,000 QPS fired when only 64,000 render — isn't that wasteful?"**
It looks wasteful until you remember cancellation only stops a request from finishing, not from
starting — the model's already generating tokens before a newer keystroke could cancel it.
Under-provisioning for the rendered number is exactly what took the fast tier down at launch.

**Q10: "Given all this, if someone says 'design a code assistant' cold, where do you start?"**
Ask the two questions that decide everything downstream: single-player/local or multi-tenant SaaS
across many private repos, and is agent mode actually in scope or just completion and chat? Then
walk forward only as far as those answers require — the two-tier split and the security gate are
close to non-negotiable, agent mode and on-prem are things you earn by naming a requirement.

---

## Cheat sheet — one line per stop on the story

- **One big model, synchronous**: wrong model for a 100ms job — the reason a separate fast tier
  exists.
- **Debounce**: wait for a typing pause before sending — kills most wasted requests before
  they're sent.
- **Server-side cancellation**: abort a request already cooking the instant it's superseded —
  debounce alone can't catch what already left.
- **Fill-in-the-middle (FIM)**: prefix + suffix, cloze-test style — fixes blindness to code
  already written after the cursor, within one file.
- **Repo-wide vector index**: a librarian who retrieves by meaning — fixes blindness to code in
  files that were never open.
- **Content-hash incremental re-embedding**: sticky notes only on changed pages — fresh in
  seconds-to-minutes without a costly full re-scan.
- **Two-tier model routing**: drive-thru for inline, dining room for chat/agent — never let one
  model own both latency budgets; scale the fleets independently.
- **Agent mode as a propose-diff loop**: read, propose, test, re-plan, report — a junior engineer
  who drafts a PR, never merges without approval by default.
- **Security gate**: airport security on every bag from every gate — synchronous, 100% of
  outputs, zero tolerance for secrets, warn-and-decide for vulnerable patterns.
- **Storage-layer tenant isolation**: locked mailboxes, not a shared mail room with a filter —
  `tenant_id` belongs in the namespace key, not a `WHERE` clause.
- **Capacity for requests fired, fail open**: provision for what's dispatched, not what renders —
  show no suggestion rather than a stale one or a frozen editor.
- **The meta-lesson**: every fix buys one property (latency, no wasted work, in-file context,
  repo-wide context, freshness, reasoning depth, safe autonomy, safety, privacy, or resilience) by
  spending a different one — say the trade in the same breath you propose the fix.
