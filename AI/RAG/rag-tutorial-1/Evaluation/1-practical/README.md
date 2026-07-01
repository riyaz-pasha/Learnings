# RAG Evaluation: Manual Practice & Metric-Building Plan (Revised)

This continues from the theory-phase plan (Tutorials 0–10). The core principle here is excellent and unchanged: **never learn a metric first — become the metric, then learn how a framework automates it.**

What's fixed in this version:
- Metric-building tutorials now follow pipeline order (retrieval → context → faithfulness → correctness) instead of starting mid-pipeline
- Added a calibration tutorial comparing your manual judgments against the LLM's simulated judgments — this is where you actually find out if an LLM judge can be trusted on your data
- Added explicit handling of "the documents don't contain this" — a common blind spot that silently passes as a hallucination nobody flags
- P1's example set is now stratified for procurement documents specifically (multi-doc comparison, numeric/date extraction, unanswerable questions) instead of generic single-fact lookups
- Fixed the final "ordering for your project" section so it actually matches the plan's own structure
- Every manual-evaluation tutorial now pulls a fixed, reproducible slice of open datasets instead of asking you to supply real questions, contexts, or answers — your only job is to practice the judgment itself

---

# Open Datasets Used in This Plan

Every tutorial below pulls from one of these four datasets using a fixed selection rule — same examples every time, nothing to author or curate.

| Dataset | Hugging Face ID | Used for | Selection rule |
|---|---|---|---|
| CUAD (commercial contracts) | `theatticusproject/cuad-qa` | Single-fact lookup, numeric extraction, faithfulness, answer correctness, context relevance | Validation split, default order, first N examples matching a named clause category |
| SQuAD 2.0 | `rajpurkar/squad_v2` | Unanswerable-question handling | Validation split, default order, first N examples where `is_impossible == True` |
| HotpotQA | `hotpotqa/hotpot_qa` (config `distractor`) | Multi-document comparison | Validation split, default order, first N examples where `type == "comparison"` |
| MS MARCO v2.1 | `microsoft/ms_marco` (config `v2.1`) | Retrieval metrics — ships with ground-truth relevance via `is_selected` | Validation split, default order, first N queries |

CUAD is the closest open analog to procurement and vendor documents — it's commercial contracts labeled for clauses like governing law, liability caps, termination, and audit rights, which is structurally close to tender and bid terms.

One-time loading pattern (same shape for all four):
```python
from datasets import load_dataset

cuad   = load_dataset("theatticusproject/cuad-qa", split="validation")
squad2 = load_dataset("rajpurkar/squad_v2", split="validation")
hotpot = load_dataset("hotpotqa/hotpot_qa", "distractor", split="validation")
marco  = load_dataset("microsoft/ms_marco", "v2.1", split="validation")
```
You won't need to write the filtering code yourself — each prompt below tells your LLM tutor exactly which filter and how many examples to pull.

---

# Phase 2: Manual Evaluation (Most Important)

Forget DeepEval. Forget RAGAs. Forget TruLens. For a week, become the evaluator.

---

## Tutorial P1 — Evaluate 20 Responses Manually

No need to bring your own RAG outputs. Pull a fixed, stratified set of 20 examples straight from the datasets above, then have the LLM itself play the role of "the RAG system" generating an answer from the given context — so the whole exercise is self-contained:

```text
5 single-fact lookup       — CUAD, first 5 matching "Governing Law"
5 numeric/date extraction  — CUAD, first 5 matching "Cap On Liability"
5 multi-document compare   — HotpotQA, first 5 "comparison"-type examples
5 unanswerable              — SQuAD 2.0, first 5 with is_impossible == True
```
Same 20 examples every time, zero authoring on your end.

For each answer ask:
```text
Did the answer answer the question?
Did the answer use the given context?
Did the answer miss important information?
Did the answer hallucinate?
If the question was unanswerable, did the answer correctly say so —
  or did it confidently make something up?
```

### Learning Goal
Understand what "good" actually means — across question types, not just easy ones.

### Prompt
```text
Act as a RAG evaluation mentor. I don't have my own data — use these open
datasets instead:

1. Load "theatticusproject/cuad-qa" (validation split, default order).
   Take the first 5 examples matching clause category "Governing Law"
   (single-fact lookup) and the first 5 matching "Cap On Liability"
   (numeric extraction).
2. Load "hotpotqa/hotpot_qa" (distractor config, validation split,
   default order). Take the first 5 examples where type == "comparison".
3. Load "rajpurkar/squad_v2" (validation split, default order). Take
   the first 5 examples where is_impossible is True.

For each of these 20, first generate an answer yourself as if you were
the RAG system being evaluated. For the 5 unanswerable ones, sometimes
correctly say the documents don't address the question, and sometimes
deliberately generate a plausible-but-unsupported answer instead, so I
can practice catching both.

Then switch roles: act as an experienced human reviewer and walk me
through evaluating each answer, paying particular attention to whether
it correctly handles the cases where the context doesn't actually
contain the answer.

Explain every observation.
```

---

## Tutorial P2 — Root Cause Analysis

Every bad answer becomes: **why?**

```text
Wrong Answer
  ↓
Wrong retrieval?
Wrong chunk?
Wrong reranking?
Wrong reasoning?
Hallucination?
Should have said "not specified in the documents" but didn't?
```

### Prompt
```text
Act as a senior RAG engineer.

Using the same 20 examples from Tutorial P1 (the question, context, and
the answer you generated for each), help me identify the actual root
cause for any I marked as bad.

Do not score anything.

Instead explain where the pipeline likely failed and why — and if the
answer fabricated information that wasn't in any document, say so
explicitly rather than filing it under generic "hallucination."
```

---

# Phase 3: Learn Metrics by Building Them (reordered to follow the pipeline)

This is where understanding becomes permanent. The order below matters: you can't judge whether an answer is faithful to its context until you've confirmed the context itself was right.

---

## Tutorial P3 — Build Retrieval Metrics Manually

Before learning Recall@K, use MS MARCO v2.1 — it already ships with ground-truth relevance judgments, so there's nothing to label by hand. Each query comes with 10 candidate passages and an `is_selected` flag marking which one is actually relevant:

```text
Query (from MS MARCO, fixed index)

Passages returned: 10
is_selected = True on passage #2 only

Simulated retriever task: re-rank these 10 passages by relevance and see
how close your ranking gets to where is_selected says the relevant
passage actually is.
```
Ask: did retrieval surface the relevant passage at all? Was it ranked early or buried?

### Prompt
```text
Teach me retrieval evaluation manually using real ground-truth data.

Load the first 5 queries from "microsoft/ms_marco" (config "v2.1",
validation split, default order). Each has 10 candidate passages with
an is_selected flag.

For each of the 5 queries:
- Show me the passages in their original order with is_selected marked
- Ask me to manually re-rank them by relevance, as if I were judging
  retrieval quality myself
- Then compare my re-ranking to the ground-truth is_selected flag and
  explain whether retrieval "succeeded," and whether it was ranked
  early or buried

Do not discuss formulas like Recall@K yet — just walk through the
judgment itself.
```

---

## Tutorial P4 — Build Context Relevance Manually

Question:
```text
What are payment terms?
```
Context:
```text
Company history
CEO biography
Office locations
```
Ask: how useful is this context, given retrieval already returned *a* document?

### Prompt
```text
Act as a context relevance evaluator.

Use the same 10 CUAD examples from Tutorial P1's single-fact and
numeric categories ("theatticusproject/cuad-qa", same "Governing Law"
and "Cap On Liability" examples).

For each one, show me the full context paragraph and explain:
- Which sentences are actually relevant to the question
- Which are irrelevant or noise
- Whether anything seems to be missing for a complete answer

Think aloud like an evaluator.
```

---

## Tutorial P5 — Build Faithfulness Manually

Context:
```text
Refunds allowed within 30 days
```
Answer:
```text
Refunds allowed within 30 days.
Premium users receive priority support.
```
Ask: where did "priority support" come from?

Also test the inverse case — when context genuinely doesn't contain an answer:
```text
Context: Refunds allowed within 30 days. No mention of international orders.
Question: What is the refund policy for international orders?
Answer: International orders follow the same 30-day policy.
```
That answer is unfaithful even though it sounds plausible — nothing in the context supports extending the policy to international orders.

### Prompt
```text
Teach me faithfulness manually using the same 20 examples from Tutorial P1.

For each one:
1. Extract claims from the answer you generated earlier
2. Check whether each claim is supported by the context
3. Explain why the claim is supported or unsupported
4. Specifically flag any claim that extends or generalizes from the
   context rather than being directly stated in it — this matters most
   for the 5 unanswerable (SQuAD 2.0) examples, where a faithful answer
   should say the documents don't address the question

Do not discuss frameworks. Teach me exactly how humans evaluate faithfulness.
```

---

## Tutorial P6 — Build Answer Correctness Manually

Given:
```text
Expected Answer
Generated Answer
```
Judge: fully correct, partially correct, or incorrect.

### Prompt
```text
Teach me answer correctness evaluation manually using the same 20
examples from Tutorial P1.

Each dataset example already has a gold answer (CUAD's "answers" field,
HotpotQA's "answer" field, or — for the SQuAD 2.0 unanswerable
examples — the expected answer is "not stated in the documents").

Compare each gold answer to the answer you generated earlier and explain:
- Missing information
- Incorrect information
- Extra information
- Whether "not specified" should be scored as fully correct when that's
  genuinely the gold answer

Think like an experienced evaluator.
```

---

# Phase 4: Become the Metric

This is where people suddenly understand DeepEval — and where you find out if you can trust an LLM judge on your own data.

---

## Tutorial P7 — Simulate Faithfulness Metric

Use the same 20 examples from Tutorial P1 (or pull 10 more CUAD examples with the same selection rule — indices 6 through 15 of the "Governing Law" matches — if you want a larger batch). You already calculated supported/unsupported claims by hand in P5; now have the LLM do the same and compare the two in Tutorial P9.

### Prompt
```text
Act as a faithfulness metric.

Using the same 20 examples from Tutorial P1, for each one show:
1. Claims extracted from the answer
2. Supported claims
3. Unsupported claims
4. Final reasoning

Do not hide intermediate steps.
```

---

## Tutorial P8 — Simulate Context Relevance

### Prompt
```text
Act as a context relevance evaluator.

Using the same 20 examples from Tutorial P1, for each one explain:
- Relevant information
- Irrelevant information
- Missing information

Show your full reasoning, not just a final score.
```

---

## Tutorial P9 — Calibrate Yourself Against the LLM (NEW)

This is the tutorial most learning plans skip entirely, and it's the one that actually tells you whether an LLM judge is trustworthy for your project.

Take the same 20 examples from Tutorial P1 that you scored by hand in Tutorials P5 and P6. Run them through the LLM simulation from P7 and P8. Then compare, side by side:

```text
Your judgment       LLM's judgment       Agree?
Unsupported claim    Supported claim       ✗  ← investigate why
Fully correct         Partially correct     ✗  ← investigate why
```

For every disagreement, dig into *why* — did the LLM miss a subtle unsupported claim? Did it generalize the same way an answer-writer would, making it too lenient on exactly the failure mode you're trying to catch? This tells you how much you can trust the automated judge before you scale it to 500 examples instead of 15.

### Prompt
```text
I have manually scored these examples for faithfulness/correctness:
[paste your manual judgments]

Here is what an LLM judge scored on the same examples:
[paste the LLM's scores and reasoning]

Compare the two judgment sets and identify:
- Where they agree
- Where they disagree, and the most likely reason for the disagreement
- Whether the LLM judge appears systematically too lenient, too strict,
  or inconsistent in a particular direction
- Whether this LLM judge seems reliable enough to trust on my full
  dataset, or only as a first pass that still needs human spot-checks
```

---

# Phase 5: Recreate DeepEval Yourself

Create a tiny evaluator:
```text
Question
Context
Answer
```
Send to LLM:
```text
Evaluate faithfulness.
Explain reasoning.
Return score.
```
You just built the core idea behind many evaluation frameworks.

### Learning Goal
```text
Framework != Evaluation
Framework = Automation
```
This realization is huge.

### Prompt
```text
Help me write a minimal Python script that takes a question, context,
and answer, sends a structured prompt to an LLM asking it to extract
claims and judge faithfulness, and parses the response into a score
plus reasoning.

Keep it small enough that I can read the entire prompt and understand
exactly what judgment it's automating — this should feel like the
P7 tutorial, just running as code instead of by hand.
```

---

# Phase 6: Framework Internals

Now learn the actual frameworks — but keep asking the same question of each one.

### DeepEval
For every metric ask: *what human judgment is this trying to automate?* Not: *what class should I import?*

### RAGAs
Ask: *which human reviewer is being simulated?*

### TruLens
Ask: *which evaluation dimensions are being captured?*

### LangSmith Evaluations
Ask: *what does this give me for free that my Phase 5 tiny evaluator didn't — tracing, dataset versioning, regression comparison over time?*

---

# Phase 7: Production Evaluation

For each query, store:
```python
{
    "question": "...",
    "question_type": "single_fact | multi_doc_compare | numeric | unanswerable",
    "retrieved_chunks": [...],
    "final_context": "...",
    "answer": "...",
    "expected_answer": "...",
    "latency_ms": ...,
    "cost_usd": ...,
}
```
Then manually evaluate 50 examples, stratified across question types — not 50 random ones, which will overrepresent whatever your most common query type is. Until you have real production logs, you can rehearse this exact schema using the same fixed dataset batch from Tutorial P1 (treat the dataset's context as `final_context` and the LLM's generated answer as `answer`) — the schema and the habit of stratified review matter more right now than where the data comes from. You'll learn more from those 50 than from reading every DeepEval metric description.

Worth a footnote even if it's not your immediate focus: production evaluation at this stage is also where you'd add periodic checks for toxic or biased output and prompt-injection resistance from retrieved documents — not because every system needs it on day one, but because it's the kind of thing you want to have decided *not* to do yet, rather than never having considered.

---

# What I would do for your procurement/RAG project

Given your LangGraph + Bedrock + Qdrant architecture, in order:

```text
1.  Pull the fixed 20-example set from open datasets (defined above) —
    no authoring needed
2.  Manual Retrieval Evaluation (P3)
3.  Manual Context Relevance (P4)
4.  Manual Faithfulness, including the unanswerable case (P5)
5.  Manual Answer Correctness (P6)
6.  Root Cause Analysis across all of the above (P2)
7.  Simulate Metrics with an LLM (P7, P8)
8.  Calibrate yourself against the LLM judge (P9)
9.  Build a tiny evaluator yourself (Phase 5)
10. DeepEval / RAGAs / TruLens / LangSmith (Phase 6)
11. Production evaluation pipeline, with cost/latency and safety
    checks included from the start, not bolted on later (Phase 7)
```

The key principle is unchanged: never learn a metric first. First become the metric. Then learn how the framework automates it — and now you'll also know exactly how much to trust that automation before you let it run unsupervised on 500 examples.
