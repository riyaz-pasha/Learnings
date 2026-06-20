# Final RAG Learning Series Plan

## *StoryVerse AI — Evolution of Context-Aware AI Systems*

> Build a production-style RAG system step by step while deeply understanding **why every component exists**, what problems it solves, where it fails, and how real-world systems evolve.

---

# Core Philosophy

This series focuses on:

* intuition before abstraction
* failures before fixes
* pure Python before frameworks
* internal workings before APIs
* incremental evolution instead of magic

The goal is NOT just to build a chatbot.

The goal is to understand:

* how RAG systems evolved
* why vector search exists
* why chunking matters
* how retrieval actually works
* why many RAG systems fail
* how production systems think

---

# Project Theme — StoryVerse AI

Build an AI assistant that understands:

* movie plots
* short stories
* screenplay snippets
* character lore
* custom fictional worlds

Example datasets:

* Harry Potter
* Interstellar
* Baahubali
* Marvel stories
* custom short stories
* Telugu movie plots

Why this is perfect:

* hallucinations are obvious
* retrieval quality becomes visible
* chunking problems are easy to notice
* semantic similarity is intuitive
* conversations become fun

---

# Tech Stack

## LLMs

* [Groq](https://groq.com?utm_source=chatgpt.com) (free + fast)
* Llama models initially

Later optional:

* [OpenAI](https://openai.com?utm_source=chatgpt.com)

---

## Embeddings

Start with:

* sentence-transformers

Later:

* BGE models
* OpenAI embeddings

---

## Vector Databases Progression

Learn progressively:

1. Python list
2. NumPy similarity search
3. FAISS
4. Chroma
5. Qdrant/Pinecone (optional later)

---

## Frameworks

* LangChain
* LangSmith
* LangGraph (advanced series only)

---

# Teaching Principles

## 1. Always Show Failure First

Example:

* show naive prompt injection failing
* THEN introduce retrieval

People remember pain.

---

## 2. Pure Python Before Frameworks

Never start with:

```python
RetrievalQA.from_chain_type(...)
```

Always:

1. build manually
2. THEN show abstraction

---

## 3. Explain Internals

Every abstraction must answer:

* what problem does it solve?
* how does it work internally?
* what are the tradeoffs?
* what breaks without it?

---

## 4. Visualize Everything

Especially:

* embeddings
* similarity scores
* chunking
* vector search
* retrieval quality

---

## 5. Use One Continuous Dataset

Keep StoryVerse throughout the series.

This builds intuition naturally.

---

# Repository Structure

```text
rag-learning-series/
│
├── notebooks/
│   ├── 00_why_rag_exists.ipynb
│   ├── 01_basic_llm_calls.ipynb
│   ├── 02_manual_context_management.ipynb
│   ├── 03_context_window_problem.ipynb
│   ├── 04_traditional_vs_semantic_search.ipynb
│   ├── 05_embeddings_intuition.ipynb
│   ├── 06_vector_search_from_scratch.ipynb
│   ├── 07_chunking_strategies.ipynb
│   ├── 08_retrieval_pipeline.ipynb
│   ├── 09_complete_rag_pipeline.ipynb
│   ├── 10_rag_problems_and_fixes.ipynb
│   └── 11_evaluation_and_langsmith.ipynb
│
├── data/
│   ├── stories/
│   ├── movie_scripts/
│   └── custom_stories/
│
├── utils/
│
├── requirements.txt
│
└── README.md
```

---

# Standard Notebook Structure

Every notebook should follow this pattern.

---

## 1. Problem Statement

What problem are we facing?

---

## 2. Failure Demonstration

Show what breaks.

---

## 3. Intuition

Use real-world analogies.

---

## 4. Internal Working

Explain how it actually works.

---

## 5. Pure Python Implementation

Build from scratch.

---

## 6. Framework Equivalent

Show LangChain abstraction.

---

## 7. Advantages

Why this approach helps.

---

## 8. Limitations

Where it still fails.

---

## 9. Production Considerations

Real-world tradeoffs.

---

## 10. Exercises

Things learners can try.

---

# PHASE 0 — Why RAG Exists

# Notebook 00 — `00_why_rag_exists.ipynb`

## Goal

Build deep intuition BEFORE coding.

## Topics

* What LLMs are
* What LLMs are NOT
* Hallucinations
* Training cutoff problem
* No private knowledge
* Context window limitations
* Why prompt engineering alone fails
* Why fine-tuning is expensive
* Parametric vs non-parametric memory
* Why RAG emerged

## Demonstrations

Ask:

* latest information
* custom fictional story
* private document content

Show failures.

## Outcome

Learner understands:

> WHY RAG exists.

---

# PHASE 1 — Manual Context Injection

# Notebook 01 — `01_basic_llm_calls.ipynb`

## Goal

Understand raw LLM interaction.

## Topics

* calling Groq API
* prompts
* tokens
* temperature
* system vs user prompts
* hallucinations

## Flow

1. ask story question
2. wrong answer
3. manually inject story context
4. answer improves

## Key Insight

LLMs need relevant context.

---

# Notebook 02 — `02_manual_context_management.ipynb`

## Goal

Show why naive context handling becomes messy.

## Build

* `/data/stories`
* load `.txt` files
* combine documents
* inject into prompt

## Two Implementations

### Pure Python

### LangChain version

## Key Lessons

* separation of concerns
* maintainability
* SOLID intuition
* LangChain = orchestration, not magic

## Important

Show raw equivalent for every abstraction.

---

# PHASE 2 — Why Sending Everything Fails

# Notebook 03 — `03_context_window_problem.ipynb`

## Goal

Make learners FEEL the scaling problem.

## Demonstrations

### Small dataset

works well

### Large dataset

* slower
* expensive
* confused answers
* irrelevant context

## Teach

* context windows
* attention dilution
* lost in the middle
* token explosion

## Key Insight

> More context != better answers

---

# PHASE 3 — Search Evolution

# Notebook 04 — `04_traditional_vs_semantic_search.ipynb`

## Goal

Understand why embeddings became necessary.

## Teach

### Traditional Search

* keyword matching
* TF-IDF
* BM25 intuition

### Problems

```text
Query: wizard school
Document: Hogwarts is a magical academy
```

Keyword search struggles.

---

## Introduce Semantic Search

Meaning-based retrieval.

## Demonstrations

* keyword search fails
* semantic search succeeds

## Key Insight

Embeddings solve semantic understanding.

---

# PHASE 4 — Embeddings

# Notebook 05 — `05_embeddings_intuition.ipynb`

## Goal

Build deep embeddings intuition.

## Topics

* vectors
* semantic space
* dimensions
* cosine similarity
* dense embeddings

## Visualizations

* semantic closeness
* similarity heatmaps
* vector distance examples

## Build

Generate embeddings using sentence-transformers.

Store:

```python
[
  {
    "text": "...",
    "embedding": [...]
  }
]
```

NO vector DB yet.

## Key Insight

Text can be represented numerically by meaning.

---

# Notebook 06 — `06_vector_search_from_scratch.ipynb`

## Goal

Understand vector retrieval deeply.

## Build

### Pure Python

* cosine similarity
* top-k retrieval
* ranking

## Then

Introduce:

* NumPy optimization
* FAISS

## Benchmark

Compare:

* pure Python
* FAISS

## Key Insight

> Vector DBs are optimized similarity search systems.

---

# PHASE 5 — Chunking

# Notebook 07 — `07_chunking_strategies.ipynb`

## Goal

Understand retrieval granularity.

## Demonstrate Problems

Without chunking:

* huge screenplay retrieved
* noisy context

Bad chunking:

```text
Chunk 1:
Harry lifted the wand and shouted—

Chunk 2:
—Expelliarmus!
```

---

## Teach

### Chunking Types

* fixed-size
* recursive
* semantic

### Overlap

Why overlap exists.

---

## Tradeoffs

### Small chunks

lose meaning

### Large chunks

add noise

---

## Then

LangChain TextSplitters.

## Key Insight

Chunking quality strongly affects RAG quality.

---

# PHASE 6 — Retrieval Pipeline

# Notebook 08 — `08_retrieval_pipeline.ipynb`

## Goal

Connect all pieces together.

## Pipeline

```text
Question
→ Embed Question
→ Similarity Search
→ Retrieve Top-K Chunks
→ Inject into Prompt
→ Generate Answer
```

## Build

### Pure Python

### LangChain Retriever

## Teach

* top-k tuning
* retrieval scores
* thresholds
* bad retrieval examples

## Key Insight

Retrieval is the heart of RAG.

---

# PHASE 7 — Complete RAG

# Notebook 09 — `09_complete_rag_pipeline.ipynb`

## Goal

Build end-to-end RAG system.

## Features

* ingest pipeline
* chunking
* embeddings
* retrieval
* answer generation
* citations
* source attribution
* “I don’t know” behavior

## Compare

### Without RAG

vs

### With RAG

## Then

Introduce:

* LangChain RetrievalQA
* Chroma

## Key Insight

RAG grounds LLM responses using external knowledge.

---

# PHASE 8 — Real-World Problems

# Notebook 10 — `10_rag_problems_and_fixes.ipynb`

## Goal

Learn why many RAG systems fail.

## Problems

* wrong retrieval
* semantically close but incorrect chunks
* duplicate chunks
* stale embeddings
* noisy PDFs
* OCR issues
* lost in the middle
* retrieval drift

---

## Solutions

### Reranking

* cross-encoders
* Cohere rerankers

### Hybrid Search

* keyword + semantic

### Metadata Filtering

### Query Rewriting

### HyDE

---

## Key Insight

Good RAG is mostly retrieval quality engineering.

---

# PHASE 9 — Evaluation & Observability

# Notebook 11 — `11_evaluation_and_langsmith.ipynb`

## Goal

Make RAG production trustworthy.

## Teach

* why vibes-based testing fails
* retrieval precision
* retrieval recall
* answer faithfulness
* relevance scoring

## Introduce

* LangSmith tracing
* debugging chains
* evaluating retrieval quality
* comparing runs

## Build

Simple evaluation dataset using StoryVerse.

## Key Insight

You cannot improve what you cannot measure.

---

# Advanced Series (Future)

Separate advanced series later.

---

## Conversational RAG

* memory
* chat history
* history-aware retrieval

---

## Agentic RAG

* tools
* routing
* planning
* LangGraph

---

## Multi-modal RAG

* images
* PDFs
* OCR
* audio/video

---

# Biggest Differentiator of This Series

This series teaches:

> “How RAG systems evolved and why every abstraction exists.”

instead of:

* copy-pasting frameworks
* black-box tutorials
* shallow demos

The focus is:

* intuition
* internals
* engineering tradeoffs
* production thinking
* debugging ability

That makes this series significantly more valuable for:

* interviews
* real-world systems
* senior engineering understanding
* teaching others
* building production AI systems
