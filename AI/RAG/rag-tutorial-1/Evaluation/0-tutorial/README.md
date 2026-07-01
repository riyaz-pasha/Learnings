# RAG Evaluation Learning Plan (Revised)

This is a revised version of a 12-day RAG evaluation curriculum. The original structure was good — debug the pipeline stage by stage before touching metrics or frameworks. This version keeps that backbone and adds:

- A hands-on exercise (Tutorial 2.5) so you're not just reading explanations
- Chunk + Context evaluation merged into one tutorial (they were overlapping concerns)
- A judge-bias caveat in the LLM-as-a-Judge tutorial
- Cost/latency as an explicit evaluation dimension
- Synthetic eval-dataset generation, since that's how most real datasets get built at scale
- A short safety/bias footnote in the production strategy tutorial

---

# Learning Goal

By the end, you should be able to answer:

```text
Why did this answer happen?

Was retrieval the problem?
Was chunking the problem?
Was reranking the problem?
Was the LLM the problem?
Was it slow or expensive for no good reason?

How can I prove it?
```

Once you can answer that, every evaluation framework becomes easy.

---

# Tutorial 0 — What is Evaluation?

### Goal
Understand why evaluation exists.

### Learn
```text
Question: "What are the payment terms?"
Answer: "Net 30 days"
```
How do we know if this answer is correct, lucky, hallucinated, retrieved from documents, or generated from model knowledge? Evaluation exists to answer these questions.

### Prompt
```text
Teach me RAG evaluation from first principles.

Do not discuss tools, frameworks, metrics, RAGAs, DeepEval, or code.

Explain:
1. Why evaluation exists
2. What problems evaluation solves
3. Why humans cannot manually evaluate everything
4. What a production team wants to know when a RAG answer is generated

Use simple examples.
```

---

# Tutorial 1 — Thinking Like a Detective

### Goal
Understand that every answer comes from a pipeline.

### Learn
```text
Question → Retrieval → Context → LLM → Answer
```
If the answer is wrong, the question is: where did it fail? This is the core evaluation mindset.

### Prompt
```text
Teach me how to think like a RAG evaluator.

Do not discuss metrics yet.

Show how a RAG answer is produced step by step.

For each stage explain:
- What goes in
- What comes out
- What can go wrong

Use detective-style reasoning and simple examples.
```

---

# Tutorial 2 — Retrieval Evaluation

### Goal
Understand retrieval failures.

### Learn
```text
Question: "What is refund policy?"

Retriever returns:
Vacation Policy
Holiday Policy
Dress Code Policy
```
The LLM never had a chance. This is a retrieval failure, not a generation failure.

### Prompt
```text
Teach me retrieval evaluation.

Do not discuss generation or LLM evaluation.

Explain:
- What retrieval is
- What makes retrieval good
- What makes retrieval bad
- How retrieval failures appear in answers

Use many examples.

Focus on understanding rather than metrics.
```

---

# Tutorial 2.5 — Build and Break a Toy Retriever (NEW)

### Goal
Stop reading about retrieval failures and actually cause one. Concepts plateau fast without this step — you can follow an explanation of "bad retrieval" and still not recognize it the first time it happens in your own system.

### Learn
You don't need a real vector database for this. Ten short text documents and a basic embedding similarity search is enough to reproduce every failure mode from Tutorial 2: near-duplicate documents that confuse ranking, a query worded differently than the source text, and a document that's relevant but ranked too low to matter.

### Prompt
```text
Help me build a minimal toy retriever to practice RAG evaluation.

1. Write a short Python Notebook script using sentence embeddings and cosine similarity
   over 10 short text documents (mix of similar-topic and
   distinct-topic documents).
2. Have me ask 5 questions against it (you only come up with questions and select randomly everytime).
3. For each question, show me the ranked results and ask me to judge:
   - Did the right document come back at all?
   - Did it come back in the top 1-2 results, or buried lower?
4. Then deliberately help me construct a question designed to break
   retrieval (e.g. paraphrased terminology, a question that matches two
   documents almost equally) and walk through why it failed.

Keep the code minimal — the goal is to see retrieval fail firsthand, not
build production infrastructure.
```

---

# Tutorial 3 — Chunk and Context Evaluation (merged)

### Goal
Understand why chunking and "usable context" are really the same failure category — both are about whether the right information made it in front of the LLM in a usable form.

### Learn
Retrieval can succeed at the document level and still fail you in two distinct ways:

```text
Correct document: employee_handbook.pdf
Wrong chunk:       Page 5
Correct answer on: Page 43
```
Retrieval succeeded. Chunk selection failed.

Or chunking can succeed and context can still be poor:

```text
Correct document ✓
Correct chunk ✓
But the chunk is:
  Incomplete (cuts off mid-sentence)
  Noisy (surrounded by irrelevant boilerplate)
  Confusing (lacks the heading/section context to be unambiguous)
```
Both are "the right document didn't translate into a usable answer." Treat them as one debugging question: *was what reached the LLM actually sufficient?*

### Prompt
```text
Teach me chunk and context evaluation together, as one debugging category.

Explain:
- Why chunking exists and what makes a good vs. bad chunk
- Why a correctly retrieved chunk can still produce a poor answer
  (incompleteness, noise, missing surrounding context)
- How to tell, when an answer is wrong, whether the problem is "wrong
  chunk" vs. "right chunk but unusable"

Use realistic document examples for both failure types.
```

---

# Tutorial 4 — Generation Evaluation

### Goal
Understand LLM behavior independent of retrieval.

### Learn
```text
Context: Refund period = 30 days
Answer:  Refund period = 60 days
```
Retrieval succeeded. Generation failed — this is hallucination, not a retrieval bug.

### Prompt
```text
Teach me generation evaluation.

Explain:
- What the LLM is responsible for
- What retrieval is responsible for
- What hallucination means
- What answer quality means beyond factual accuracy (completeness, tone, format)

Use examples.
```

---

# Tutorial 5 — Building Evaluation Datasets (+ synthetic generation)

### Goal
Understand test sets — both how they're written by hand and how they're generated at scale.

### Learn
Production teams create:
```text
Question
Expected Answer
Expected Documents
```
This becomes the benchmark. Hand-writing these doesn't scale past a few dozen examples, so most real eval datasets are generated synthetically: an LLM reads source documents and produces question/answer pairs, which are then filtered for quality (duplicates, ambiguous questions, unanswerable questions removed) before becoming the benchmark.

### Prompt
```text
Teach me how evaluation datasets are created, covering both manual and
synthetic approaches.

Explain:
- Why production teams create test questions
- What good test questions look like vs. bad ones
- How synthetic generation works: prompting an LLM against source
  documents to generate question/answer pairs at scale
- What quality filtering looks like (removing ambiguous, duplicate, or
  unanswerable generated questions)
- Where manual review is still necessary even with synthetic generation

Use practical examples.
```

---

# Tutorial 6 — Metrics (First Time, including cost/latency)

### Goal
Finally learn metrics — including the ones that aren't about correctness.

### Learn
Metrics answer questions:
```text
Did we retrieve the right docs?
Did we retrieve them early (ranked highly)?
Did the answer match the expected answer?
Did the model hallucinate?
Was the answer worth what it cost to produce?
```
That last question matters more than most learning plans admit. A reranking step might raise faithfulness 5% while doubling latency and tripling cost per query — in production, that's a real tradeoff decision, not a side note.

### Prompt
```text
Teach me RAG metrics.

Start from the questions metrics try to answer.

Then explain:
- Recall
- Precision
- Ranking quality
- Faithfulness
- Answer correctness
- Latency and cost-per-query as evaluation dimensions, and how teams
  weigh quality improvements against latency/cost tradeoffs

Use simple examples and avoid formulas initially.
```

---

# Tutorial 7 — LLM-as-a-Judge (with bias caveat)

### Goal
Understand modern evaluation — and its main failure mode.

### Learn
Instead of humans reviewing everything:
```text
Strong LLM → Judge
```
But judges aren't neutral. A judge model tends to favor answers that resemble its own generation style (self-preference bias), and weaker judges miss subtle factual errors that a domain expert would catch immediately. Treat judge output as a strong signal, not ground truth.

### Prompt
```text
Teach me LLM-as-a-Judge from scratch.

Explain:
- Why it exists and how it works
- What it evaluates well vs. poorly
- Self-preference bias: why a judge model tends to favor outputs that
  resemble its own style, and what that means if your judge and
  generator are similar models
- When human review is still necessary despite having a judge

Use practical examples.
```

---

# Tutorial 8 — Evaluating Complex RAG

### Goal
Understand evaluation when the pipeline has more stages — this is probably where your own system sits.

### Learn
```text
Query → Rewrite → Retrieve → Rerank → Tools → LLM → Answer
```
Evaluation becomes: evaluate each stage separately, the same detective mindset from Tutorial 1, just with more suspects.

### Prompt
```text
Teach me how complex RAG systems are evaluated.

Explain:
- Query rewriting evaluation
- Retrieval evaluation
- Reranking evaluation
- Tool usage evaluation
- Final answer evaluation

Use a realistic enterprise RAG example.
```

---

# Tutorial 9 — Evaluation Frameworks

### Goal
Now that the concepts are solid, learn the tools as implementation details.

### Prompt
```text
I already understand RAG evaluation concepts.

Now teach me:
- RAGAs
- DeepEval
- TruLens
- LangSmith evaluations

Compare them.

Focus on what problem each solves rather than feature lists.
```

---

# Tutorial 10 — Production Evaluation Strategy (+ safety footnote)

### Goal
Think like a staff engineer responsible for what ships.

### Learn
How real companies evaluate RAG in production — offline eval, online eval, human review, A/B testing, regression testing, release validation. Worth a footnote even if it's not your main focus: production RAG eval often also includes checks for toxic output, PII leakage, and prompt-injection resistance via retrieved content. Not every system needs deep safety eval, but it's worth knowing it exists as a category before deciding you don't need it.

### Prompt
```text
Teach me how mature companies evaluate RAG systems in production.

Explain:
- Offline evaluation
- Online evaluation
- Human review
- A/B testing
- Regression testing
- Release validation
- Where safety checks (toxicity, PII leakage, prompt injection via
  retrieved documents) fit into this, even briefly

Show a complete workflow.
```

---

## Recommended Pace

```text
Day 1  → Tutorial 0
Day 2  → Tutorial 1
Day 3  → Tutorial 2
Day 4  → Tutorial 2.5 (hands-on — give this a full day, don't rush it)
Day 5  → Tutorial 3
Day 6  → Tutorial 4
Day 7  → Tutorial 5
Day 8  → Tutorial 6
Day 9  → Tutorial 7
Day 10 → Tutorial 8
Day 11 → Tutorial 9
Day 12 → Tutorial 10
```

After Tutorial 4, try evaluating a few responses from your own RAG system manually — you'll have enough vocabulary by then to say *which* stage failed, not just that something's wrong.

After Tutorial 8, you should be able to look at your own LangGraph/Bedrock/Qdrant pipeline and identify exactly where failures originate.

After Tutorial 10, frameworks like RAGAs and DeepEval will feel simpler because you'll already understand the problems they're trying to solve — and you'll know which problems they *don't* solve (cost, latency, safety) without bringing in something else.
