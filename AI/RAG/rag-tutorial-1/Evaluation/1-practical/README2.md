# RAG Evaluation: Manual Practice & Metric-Building Plan (Code Edition)

This continues from the theory-phase plan (Tutorials 0–10). Same core principle: **never learn a metric first — become the metric, then learn how a framework automates it.**

What changed in this version: every tutorial is now a real Python script you run yourself, not a prompt you hand to an LLM to narrate. The LLM only gets called from code, and only where the task genuinely needs one — generating a candidate answer to evaluate (Phase 2–3), or acting as an automated judge (Phase 4–5). The actual judging in Phases 2 and 3 is you, reading printed output and writing your own verdict in the script.

Install once:
```bash
pip install datasets anthropic sentence-transformers numpy
```

---

# Setup — Load and Normalize the Open Datasets

Run this once. It fetches a fixed, reproducible 20-example set — same examples every run, nothing for you to author.

```python
from datasets import load_dataset

cuad   = load_dataset("theatticusproject/cuad-qa", split="validation")
hotpot = load_dataset("hotpotqa/hotpot_qa", "distractor", split="validation")
squad2 = load_dataset("rajpurkar/squad_v2", split="validation")

def first_n_matching(dataset, predicate, n):
    out = []
    for ex in dataset:
        if predicate(ex):
            out.append(ex)
        if len(out) == n:
            break
    return out

governing_law    = first_n_matching(cuad, lambda ex: "Governing Law" in ex["question"], 5)
cap_on_liability = first_n_matching(cuad, lambda ex: "Cap On Liability" in ex["question"], 5)
comparisons      = first_n_matching(hotpot, lambda ex: ex["type"] == "comparison", 5)
unanswerable     = first_n_matching(squad2, lambda ex: ex["is_impossible"] is True, 5)

def flatten_hotpot_context(ex):
    parts = []
    for title, sents in zip(ex["context"]["title"], ex["context"]["sentences"]):
        parts.append(f"{title}: " + " ".join(sents))
    return "\n".join(parts)

def normalize(ex, source):
    if source == "cuad":
        context_text = ex["context"]
        gold = ex["answers"]["text"][0] if ex["answers"]["text"] else "Not specified in the contract."
    elif source == "hotpot":
        context_text = flatten_hotpot_context(ex)
        gold = ex["answer"]
    elif source == "squad2":
        context_text = ex["context"]
        gold = "Not stated in the documents."
    return {"question": ex["question"], "context_text": context_text,
            "gold_answer": gold, "source": source}

examples = (
    [normalize(ex, "cuad") for ex in governing_law] +      # indices 0-4:  single-fact
    [normalize(ex, "cuad") for ex in cap_on_liability] +    # indices 5-9:  numeric
    [normalize(ex, "hotpot") for ex in comparisons] +       # indices 10-14: multi-doc
    [normalize(ex, "squad2") for ex in unanswerable]        # indices 15-19: unanswerable
)
```

```python
import anthropic
client = anthropic.Anthropic()  # reads ANTHROPIC_API_KEY from your environment

def ask_claude(prompt, max_tokens=400):
    msg = client.messages.create(
        model="claude-sonnet-4-6",
        max_tokens=max_tokens,
        messages=[{"role": "user", "content": prompt}],
    )
    return msg.content[0].text

def generate_answer(question, context):
    return ask_claude(
        f"Context:\n{context}\n\nQuestion: {question}\n\n"
        "Answer using only the context above. If the context does not "
        "contain the answer, say so explicitly."
    )

def generate_overconfident_answer(question, context):
    return ask_claude(
        f"Context:\n{context}\n\nQuestion: {question}\n\n"
        "Answer the question directly and confidently, even if you have to "
        "infer or extrapolate beyond what's explicitly stated."
    )

for ex in examples:
    ex["system_answer"] = generate_answer(ex["question"], ex["context_text"])

# For the 5 unanswerable examples, also generate the failure-mode version
for ex in examples[15:20]:
    ex["system_answer_overconfident"] = generate_overconfident_answer(
        ex["question"], ex["context_text"]
    )
```

You now have 20 question/context/gold-answer/system-answer rows, fully reproducible, with zero hand-authoring. Everything below reuses this `examples` list.

---

# Phase 2: Manual Evaluation (Most Important)

---

## Tutorial P1 — Evaluate 20 Responses Manually

```python
for i, ex in enumerate(examples):
    print(f"[{i}] ({ex['source']}) Q: {ex['question']}")
    print(f"Context: {ex['context_text'][:300]}...")
    print(f"System answer: {ex['system_answer']}")
    print(f"Gold answer:   {ex['gold_answer']}")
    print("-" * 60)
```

Read the printed output for all 20 and fill this in yourself — this is the actual evaluation, not something to automate yet:

```python
my_judgments = {
    0: {"verdict": "good", "reason": ""},
    1: {"verdict": "good", "reason": ""},
    # ... fill in all 20 by hand after reading the output above
}
```

For each one, ask yourself: did the answer use the given context? Did it miss anything? Did it hallucinate? For indices 15–19, specifically check whether the system answer correctly says the documents don't address the question, or quietly invents something.

---

## Tutorial P2 — Root Cause Analysis

For any example you marked `"bad"` in P1, write the root cause yourself instead of just the verdict:

```python
root_causes = {
    # example index: one of "retrieval", "chunking", "generation_hallucination",
    #                "generation_missed_context", "should_have_refused"
    3: "generation_hallucination",
}
```

This is the habit that matters — turning "bad" into a specific, falsifiable cause you could point to in a pipeline.

---

# Phase 3: Build Metrics by Hand (pipeline order: retrieval → context → faithfulness → correctness)

---

## Tutorial P3 — Build Retrieval Metrics Manually

MS MARCO already ships ground-truth relevance labels, so there's nothing to label by hand — only to rank yourself and score against it.

```python
marco = load_dataset("microsoft/ms_marco", "v2.1", split="validation")
queries = [marco[i] for i in range(5)]

retrieval_results = []
for q in queries:
    passages = q["passages"]["passage_text"]
    is_selected = q["passages"]["is_selected"]
    relevant_idx = [i for i, s in enumerate(is_selected) if s == 1]

    print("Query:", q["query"])
    for i, p in enumerate(passages):
        print(f"  [{i}] {p[:150]}")
    print("Ground truth relevant index(es):", relevant_idx)
    print()

    # Read the passages above and write YOUR ranking by hand, best first:
    my_ranking = []  # e.g. [4, 0, 7, 2, 9, 1, 5, 3, 6, 8]

    recall_at_3 = any(idx in my_ranking[:3] for idx in relevant_idx)
    retrieval_results.append({"query": q["query"], "recall_at_3": recall_at_3,
                               "my_ranking": my_ranking, "relevant_idx": relevant_idx})

for r in retrieval_results:
    print(r["query"], "-> Recall@3:", r["recall_at_3"])
```

You just computed Recall@3 by hand, on real ground truth, before ever importing a metrics library.

---

## Tutorial P4 — Build Context Relevance Manually

```python
for ex in examples[0:10]:  # the CUAD single-fact + numeric examples
    print("Q:", ex["question"])
    print("Context:", ex["context_text"])
    print()
```

For each, write down by hand which sentences are actually relevant, which are noise, and whether anything seems missing:

```python
context_relevance_notes = {
    0: {"relevant": "...", "irrelevant": "...", "missing": "..."},
    # ... fill in for indices 0-9
}
```

---

## Tutorial P5 — Build Faithfulness Manually

```python
for ex in examples:
    print(f"[{ex['source']}] Context: {ex['context_text'][:300]}")
    print("Answer:", ex["system_answer"])
    print()

# For the unanswerable set, also look at the deliberately overconfident version:
for ex in examples[15:20]:
    print("Overconfident answer:", ex["system_answer_overconfident"])
    print()
```

For each, extract the claims yourself and mark each as supported or unsupported by the context — by hand, in a script or notebook cell:

```python
faithfulness_notes = {
    0: [{"claim": "...", "supported": True}],
    # ...
    15: [{"claim": "...", "supported": False, "note": "extrapolated beyond context"}],
}
```

The overconfident answers in 15–19 are where unfaithfulness should jump out clearly — that's the point of generating both versions.

---

## Tutorial P6 — Build Answer Correctness Manually

```python
for i, ex in enumerate(examples):
    print(f"[{i}] System: {ex['system_answer']}")
    print(f"    Gold:   {ex['gold_answer']}")
    print()
```

Judge each by hand: fully correct, partially correct, or incorrect — and decide for yourself whether "not stated in the documents" should count as fully correct for indices 15–19 (it should, when that's genuinely the gold answer).

```python
correctness_notes = {
    0: "fully_correct",
    15: "fully_correct",  # if the system correctly refused
}
```

---

# Phase 4: Become the Metric — Automate the Judge in Code

This is where an LLM call is appropriate: not narrating your evaluation to you, but doing the same mechanical task you just did by hand, so you can compare.

```python
import json

FAITHFULNESS_JUDGE_PROMPT = """You are a faithfulness judge. Extract every \
factual claim in the answer below, then mark each as "supported" or \
"unsupported" based only on the context. Return strict JSON only, no \
other text, in this shape:
{{"claims": [{{"text": "...", "supported": true, "reason": "..."}}]}}

Context:
{context}

Answer:
{answer}
"""

def judge_faithfulness(context, answer):
    raw = ask_claude(FAITHFULNESS_JUDGE_PROMPT.format(context=context, answer=answer), max_tokens=600)
    return json.loads(raw)

for ex in examples:
    ex["auto_faithfulness"] = judge_faithfulness(ex["context_text"], ex["system_answer"])
```

## Tutorial P9 — Calibrate Yourself Against the LLM

Compare the judge's output against the `faithfulness_notes` you wrote by hand in P5:

```python
for i, ex in enumerate(examples):
    manual = faithfulness_notes.get(i)
    auto = ex["auto_faithfulness"]["claims"]
    print(f"[{i}] Manual: {manual}")
    print(f"     Auto:   {[(c['text'], c['supported']) for c in auto]}")
    print()
```

For every disagreement, look at *why*. Is the judge systematically too lenient — treating a plausible-sounding extrapolation as supported? That's the self-preference bias risk this kind of check exists to catch, and now you have it in code, on data you can re-run any time you change the judge prompt.

---

# Phase 5: Recreate DeepEval Yourself

`judge_faithfulness` above already *is* the core idea behind frameworks like DeepEval — a structured prompt plus parsed JSON output. The only thing left is packaging it for reuse:

```python
# evaluator.py
class FaithfulnessMetric:
    def __init__(self, client, model="claude-sonnet-4-6"):
        self.client = client
        self.model = model

    def score(self, context, answer):
        result = judge_faithfulness(context, answer)  # reuse the function above
        claims = result["claims"]
        if not claims:
            return 1.0, claims
        supported = sum(1 for c in claims if c["supported"])
        return supported / len(claims), claims
```

```python
metric = FaithfulnessMetric(client)
for ex in examples:
    score, claims = metric.score(ex["context_text"], ex["system_answer"])
    print(ex["question"], "-> faithfulness:", round(score, 2))
```

```text
Framework != Evaluation
Framework = Automation
```
You just built the thing the framework automates — the realization that matters here.

---

# Phase 6: Framework Internals

Now install an actual framework (e.g. `pip install deepeval` or `pip install ragas`) and run its faithfulness metric on the same 20 `examples`. For every metric it gives you, ask: *what human judgment is this trying to automate?* — and compare its score against your own `FaithfulnessMetric.score()` from Phase 5 on the same data. Where the two disagree, that's worth digging into the same way you did in Tutorial P9.

Do the same comparison exercise for RAGAs, TruLens, and LangSmith evaluations if you use them — same 20 examples each time, so you're comparing frameworks against each other and against your own hand-built version on a fixed, known dataset rather than vibes.

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
Until you have real production logs, you can rehearse this exact schema by running `examples` through it — `context_text` as `final_context`, `system_answer` as `answer`. The schema and the habit of stratified review matter more right now than where the data comes from.

Worth a footnote: this is also where periodic checks for toxic/biased output and prompt-injection resistance from retrieved documents tend to live in a mature pipeline — not something to build now, but something to have decided not to build yet, rather than never considered.

---

# What I would do for your procurement/RAG project

```text
1.  Run the Setup script above — fixed 20-example set, zero authoring
2.  Manual Retrieval Evaluation (P3) — real ground truth, your own ranking
3.  Manual Context Relevance (P4)
4.  Manual Faithfulness, including the overconfident-answer case (P5)
5.  Manual Answer Correctness (P6)
6.  Root Cause Analysis across all of the above (P2)
7.  Automate the judge in code (Phase 4)
8.  Calibrate your code-based judge against your manual notes (P9)
9.  Package it into a reusable metric class (Phase 5)
10. Install DeepEval/RAGAs/TruLens/LangSmith and diff their scores
    against your own on the same fixed examples (Phase 6)
11. Production evaluation pipeline, cost/latency and safety checks
    included from the start (Phase 7)
```
