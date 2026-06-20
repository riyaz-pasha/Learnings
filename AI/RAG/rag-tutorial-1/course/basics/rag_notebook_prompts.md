# RAG Learning Series — Notebook Generation Prompts

> Use each prompt below to generate the corresponding Jupyter notebook.
> Each prompt is self-contained. Paste it directly into Claude or any capable LLM.
> Output format for all: a **single `.ipynb`-ready markdown file** using `# %%` cell markers.

---

## How to use these prompts

Each prompt produces a notebook file. Use this cell format:

```
# %% [markdown]
Your explanation text here

# %%
your_python_code_here
```

Paste into VS Code with Jupyter extension or convert via `jupytext`.

---

---

# Prompt 00 — `00_why_rag_exists.ipynb`

---

```
You are an expert AI educator creating a Jupyter notebook for a RAG learning series called "StoryVerse AI".

This is Notebook 00: "Why RAG Exists"

Your audience: intermediate Python developers who know basic ML concepts but have never built RAG systems. They learn best through story-driven, failure-first explanations. They want deep intuition, not just tool usage.

---

NOTEBOOK GOAL:
Build the emotional and intellectual WHY before any RAG code is written. By the end, the student should feel the pain of LLM limitations so deeply that they are eager for RAG to exist.

---

NOTEBOOK THEME:
We are building "StoryVerse AI" — an AI assistant that knows movie plots, short stories, and character lore. Use this theme throughout all examples. Use references like Interstellar plot, Harry Potter characters, or custom short story snippets as demonstration data.

---

STRUCTURE TO FOLLOW (in this exact order):

1. **Opening Markdown Cell — The Problem Setup**
   - Tell a short story: "Imagine you built an AI assistant for a movie database startup. A user asks: 'What happens at the end of Kalki 2898 AD?' The AI confidently gives a completely wrong answer."
   - Set the stage: why does this happen? This notebook explains it.

2. **What Is an LLM? (Markdown)**
   - Explain in plain English: LLMs are trained on massive text datasets. They compress knowledge into weights (parametric memory). They don't "look things up" — they "remember" from training.
   - Analogy: like a student who studied textbooks for 6 months, then entered an exam room with no books allowed. They can only answer from memory.

3. **Problem 1: Training Cutoff (Code + Markdown)**
   - Setup: install/import groq, set up client
   - Code cell: ask the LLM "Who won IPL 2025?" or "What happened in [recent event]?"
   - Show the wrong/uncertain answer
   - Markdown explanation: LLMs have a training cutoff. Anything after that date is invisible to them. They either say "I don't know" or hallucinate confidently.

4. **Problem 2: No Private Knowledge (Code + Markdown)**
   - Create a string variable called `secret_story` containing a short 200-word custom story (make one up about a character named "Arjun" who discovers a portal in a Hyderabad bookshop)
   - Do NOT inject it into the prompt yet
   - Ask the LLM: "What happens to Arjun in the bookshop story?"
   - Show the hallucinated answer
   - Markdown: LLMs cannot know your private data — PDFs, internal docs, custom stories, databases. They were never trained on it.

5. **Problem 3: Hallucination (Code + Markdown)**
   - Ask the LLM a detailed question about a fictional movie you invent: "Explain the ending of the 1987 film 'The Crimson Algorithm'"
   - Show that it confidently fabricates a detailed answer
   - Markdown: hallucination is not a bug — it's a feature of how LLMs work. They always try to generate a plausible completion. Without grounding, plausible ≠ true.

6. **Problem 4: Context Window Limits (Markdown + Diagram)**
   - Explain context windows using a plain ASCII diagram:
     ```
     [ System Prompt | Your Documents | Conversation History | Answer ]
     |←————————————— 128k tokens max ————————————————————→|
     ```
   - Explain: 1 token ≈ 4 characters. A novel is ~500k tokens. You cannot fit everything.
   - Even if you could — it gets expensive and slow.

7. **Why Prompt Engineering Alone Fails (Code + Markdown)**
   - Show: manually pasting 3 story summaries into the prompt works fine
   - Then show: what happens when you have 500 stories? The prompt becomes enormous.
   - Code: demonstrate token count growing with a loop (just print lengths, no need for real tokenizer)
   - Markdown: prompt engineering is a bandage, not a solution.

8. **Why Fine-Tuning Is Not the Answer (Markdown)**
   - Explain fine-tuning in plain English: retraining the model on your data
   - Why it's problematic:
     - Expensive (GPU costs)
     - Takes days/weeks
     - Knowledge becomes stale the moment your data changes
     - Doesn't actually "remember" facts reliably — it changes behavior patterns, not stores facts
   - Analogy: fine-tuning is like retraining a chef from scratch every time the menu changes. RAG is like giving the chef a recipe card to read before cooking.

9. **Parametric vs Non-Parametric Memory (Markdown)**
   - Parametric memory: knowledge baked into model weights during training. Static. Expensive to update.
   - Non-parametric memory: external knowledge retrieved at inference time. Dynamic. Cheap to update.
   - Table comparison:
     | | Parametric (Fine-tune) | Non-Parametric (RAG) |
     |---|---|---|
     | Update cost | High | Low |
     | Freshness | Stale | Real-time |
     | Transparency | Black box | Citable |
     | Hallucination | Higher | Lower |

10. **Where RAG Fits (Markdown)**
    - RAG = Retrieval Augmented Generation
    - Idea: before answering, fetch relevant documents from an external store, inject them into the prompt, then generate
    - Simple flow diagram in ASCII:
      ```
      User Question
           ↓
      Search External Knowledge Base
           ↓
      Retrieve Relevant Chunks
           ↓
      [ System Prompt + Retrieved Context + Question ] → LLM → Answer
      ```
    - This notebook showed the problem. The rest of the series builds the solution.

11. **Closing Cell (Markdown)**
    - Summary of the 4 core LLM limitations discovered
    - Preview: "In the next notebook, we start solving this — manually, step by step."
    - One exercise: "Ask your LLM a question about something that happened last week. Notice what it says."

---

CODE REQUIREMENTS:
- Use `groq` Python client with `llama-3.1-8b-instant` model
- Show how to set up the client with: `client = Groq(api_key=os.environ.get("GROQ_API_KEY"))`
- Each code cell should be clean, readable, and have a comment header explaining what it does
- Print outputs clearly with labels like `print("LLM Response:", response)`

---

WRITING STYLE:
- Conversational, never dry or academic
- Use analogies constantly
- Every failure demo should feel surprising and a little funny
- Short paragraphs — never more than 4 lines per paragraph in markdown cells
- Bold key terms on first use

---

OUTPUT FORMAT:
- Single markdown file using `# %% [markdown]` and `# %%` cell markers
- Ready to open in VS Code with Jupyter extension or convert with jupytext
- Include a requirements cell at the top: `# %% — pip install groq python-dotenv`
```

---

---

# Prompt 01 — `01_basic_llm_calls.ipynb`

---

```
You are an expert AI educator creating a Jupyter notebook for a RAG learning series called "StoryVerse AI".

This is Notebook 01: "Basic LLM Calls — The Starting Point"

Your audience: intermediate Python developers. They just finished Notebook 00 and understand WHY RAG exists. Now they start building.

---

NOTEBOOK GOAL:
Understand raw LLM interaction deeply. Build the most basic version of context injection — manually pasting context into a prompt — and feel exactly why it breaks at scale.

---

NOTEBOOK THEME:
StoryVerse AI — an AI assistant for movie plots and short stories.
Use a custom short story throughout: "The Portal Bookshop" — a 300-word story about Arjun who discovers a portal in a Hyderabad bookshop. Write the full story in a markdown cell so students can use it.

---

STRUCTURE TO FOLLOW:

1. **Opening — Where We Are (Markdown)**
   - One-paragraph recap: we know LLMs hallucinate without context. Today we give it context manually.
   - What we'll build: a simple function that takes a question + context, calls the LLM, returns an answer
   - What we'll discover: it works, but it doesn't scale

2. **Setup Cell (Code)**
   - Import: os, groq
   - Load API key from environment
   - Create Groq client
   - Define model constant: `MODEL = "llama-3.1-8b-instant"`

3. **Step 1 — Bare LLM Call, No Context (Code + Markdown)**
   - Function: `ask_llm(question)` — just calls LLM with user question, no system prompt
   - Call it: `ask_llm("What happens to Arjun in The Portal Bookshop story?")`
   - Show the hallucinated/wrong answer
   - Markdown: "The model has never seen this story. It guesses. Confidently. This is the problem."

4. **Understanding Prompts (Markdown)**
   - Explain the anatomy of an LLM call:
     - **System prompt**: sets behavior and context. Persists across the conversation.
     - **User message**: the actual question.
     - **Assistant message**: the response.
   - Show the message structure as a Python dict before abstracting it
   - Explain tokens: 1 token ≈ 4 characters. Models have a max token budget. Input + output must fit.
   - Explain temperature: 0 = deterministic, 1 = creative. For factual QA, use 0 or 0.1.

5. **Step 2 — Inject Context Into User Prompt (Code + Markdown)**
   - Write "The Portal Bookshop" story in a Python string variable
   - Function: `ask_llm_with_context(question, context)` — builds a prompt that includes context
     ```python
     prompt = f"""
     Use the following context to answer the question.
     
     Context:
     {context}
     
     Question:
     {question}
     """
     ```
   - Call it with the story as context and a specific question
   - Show the correct answer
   - Markdown: "It works! The model now has the story. But notice — we hardcoded the story into the code."

6. **Step 3 — Move Context to System Prompt (Code + Markdown)**
   - Refactor: move context to the system prompt instead of user message
   - Explain the difference:
     - System prompt = persistent instructions + background knowledge
     - User message = the actual question per turn
   - Show same question, same answer, cleaner separation
   - Markdown: "Better. But the story is still hardcoded in the script. What if we have 50 stories?"

7. **The Scaling Problem (Code + Markdown)**
   - Create 5 short story snippets as Python strings (make them up — 100 words each)
   - Manually combine them all into one big context string
   - Measure total length: `print(f"Context length: {len(combined_context)} characters")`
   - Show it still works — but then scale it up conceptually:
     - "What if we have 500 stories? 5000? A whole Netflix catalog?"
   - Show a quick simulation: generate fake content of 100 stories, measure character count, extrapolate token cost
   - Markdown: "This approach hits two walls: context window limits, and cost per call."

8. **SOLID Principle Violation (Markdown)**
   - Explain Single Responsibility Principle simply: "A function should do one thing."
   - Our current code does too many things:
     - Stores knowledge (the story strings)
     - Formats prompts
     - Calls the LLM
     - Returns answers
   - This is hard to maintain, test, or extend
   - "What if the story changes? You edit the Python file. That's wrong."
   - Preview: "Next notebook — we separate knowledge into files."

9. **What We Learned (Markdown)**
   - System prompt vs user message
   - How to inject context
   - Temperature and token concepts
   - Why hardcoded context breaks at scale
   - Why SOLID matters even in AI systems

10. **Exercises (Markdown)**
    - Try: change temperature to 1.0, ask the same question — notice creative drift
    - Try: remove context and ask a specific plot detail — hallucination returns
    - Try: add a second story and ask a question that spans both

---

CODE REQUIREMENTS:
- Use `groq` client, `llama-3.1-8b-instant`
- All functions cleanly typed with docstrings
- Print outputs with clear labels
- Show the full API response structure in one cell so students understand what `.choices[0].message.content` is

---

WRITING STYLE:
- Conversational and curious
- Each "step" should feel like a small experiment with a hypothesis and result
- Use "Notice that…" and "What if…" to drive curiosity
- Short paragraphs

---

OUTPUT FORMAT:
Single markdown file using `# %% [markdown]` and `# %%` cell markers.
```

---

---

# Prompt 02 — `02_naive_context_management.ipynb`

---

```
You are an expert AI educator creating a Jupyter notebook for a RAG learning series called "StoryVerse AI".

This is Notebook 02: "Naive Context Management — Files, Folders, and LangChain"

Your audience: intermediate Python developers continuing from Notebook 01. They know how to inject context manually. Now they learn to load it from files and understand LangChain as an abstraction layer.

---

NOTEBOOK GOAL:
- Move knowledge out of code and into files (separation of concerns)
- Load and combine documents with pure Python
- Then do the exact same thing with LangChain — and show that LangChain is just organized abstraction, not magic
- End by feeling the new problem: sending ALL docs every time is wasteful

---

STRUCTURE TO FOLLOW:

1. **Opening (Markdown)**
   - Recap: last notebook had stories hardcoded in Python. Bad for maintenance.
   - Today: knowledge lives in files. Code just reads and uses it.
   - Analogy: a librarian doesn't memorize every book. They know where the books are.

2. **Setup the Data Folder (Markdown + Code)**
   - Create folder structure in code:
     ```python
     import os
     os.makedirs("data/stories", exist_ok=True)
     ```
   - Write 5 short story files programmatically (use Python to write .txt files)
   - Stories to create (write 150-word summaries for each):
     - `interstellar.txt` — Interstellar plot summary
     - `harry_potter_sorcerers_stone.txt` — HP book 1 plot
     - `baahubali.txt` — Baahubali story
     - `portal_bookshop.txt` — Arjun's story from Notebook 01
     - `dark_knight.txt` — The Dark Knight plot
   - Show the folder structure after creation

3. **Pure Python: Load All Documents (Code + Markdown)**
   - Function: `load_documents(folder_path)` — reads all .txt files, returns list of dicts:
     ```python
     [{"filename": "interstellar.txt", "content": "..."}]
     ```
   - Function: `combine_documents(docs)` — joins all content into one string with separators
   - Show output: print combined length, preview first 200 chars
   - Markdown: "Clean. But notice — we're loading and combining EVERYTHING. Even if the user only asks about Harry Potter."

4. **Pure Python: Full QA Function (Code + Markdown)**
   - Function: `answer_question(question, docs_folder)`:
     1. Load all docs
     2. Combine
     3. Build prompt with context
     4. Call LLM
     5. Return answer
   - Test with 3 different questions across different stories
   - Show it works
   - Measure and print combined context length each time

5. **Introduce LangChain (Markdown)**
   - What is LangChain?
     - It's an orchestration framework. It does NOT make your LLM smarter.
     - It provides: document loaders, prompt templates, chains, retrievers — all as reusable abstractions
   - Analogy: LangChain is like Express.js for Node. You could write raw HTTP servers, but Express gives you structure.
   - WARNING: most beginners treat LangChain as magic. It isn't. We'll always show what it's doing underneath.

6. **LangChain: Document Loading (Code + Markdown)**
   - Use `DirectoryLoader` + `TextLoader` to load the same folder
   - Show the output: list of `Document` objects with `.page_content` and `.metadata`
   - Side by side comparison:
     ```python
     # Pure Python
     docs = load_documents("data/stories")
     
     # LangChain
     from langchain_community.document_loaders import DirectoryLoader, TextLoader
     loader = DirectoryLoader("data/stories", glob="*.txt", loader_cls=TextLoader)
     lc_docs = loader.load()
     ```
   - Print both outputs — they contain the same information
   - Markdown: "LangChain's Document object is just a wrapper around text + metadata. No magic."

7. **LangChain: Prompt Templates (Code + Markdown)**
   - Show our pure Python prompt:
     ```python
     prompt = f"Context:\n{context}\n\nQuestion:\n{question}"
     ```
   - Show LangChain `PromptTemplate` doing the same thing:
     ```python
     from langchain.prompts import PromptTemplate
     template = PromptTemplate(
         input_variables=["context", "question"],
         template="Context:\n{context}\n\nQuestion:\n{question}"
     )
     ```
   - Show `template.format(context=..., question=...)` produces identical output
   - Markdown: "PromptTemplate is a string formatter with validation. That's it."

8. **LangChain: Chains (Code + Markdown)**
   - Show a simple `LLMChain` (or LCEL pipe) connecting prompt template → LLM
   - Then show the pure Python equivalent: format prompt string → call API
   - Markdown:
     - "A chain is just: take input → transform it → pass to next step → return output."
     - "You could write this yourself in 10 lines of Python. LangChain gives you reusable, composable pieces."
   - Show both produce the same answer for the same question

9. **Why Use LangChain Then? (Markdown)**
   - Reusability: swap models without rewriting logic
   - Composability: chains can be nested, extended
   - Ecosystem: hundreds of integrations (DBs, APIs, tools) already built
   - Observability: easy to plug in LangSmith for tracing
   - "Use it as scaffolding, not as a crutch. Always understand what's underneath."

10. **The Problem We Introduced (Markdown + Code)**
    - We now load ALL documents for every question
    - Demonstrate the growing cost:
      ```python
      for num_docs in [1, 5, 10, 50, 100]:
          fake_context = "story content " * (num_docs * 100)
          print(f"{num_docs} docs → {len(fake_context)} chars → ~{len(fake_context)//4} tokens")
      ```
    - Markdown:
      - "100 stories = ~150,000 tokens per query. At $0.0001/1k tokens, that's $0.015 per question."
      - "1 million daily questions = $15,000/day. Just on context."
      - "And we're sending Batman trivia when someone asks about Harry Potter."
    - Preview: "We need to send only the relevant documents. That's what embeddings and retrieval solve."

11. **Summary + Exercises (Markdown)**
    - What we learned: file-based context, LangChain demystified, the cost problem
    - Exercises:
      - Add a new story file and verify it gets loaded automatically
      - Break the LangChain version by changing a variable name — read the error to understand what it's doing internally
      - Print `lc_docs[0].__dict__` to inspect the Document object

---

CODE REQUIREMENTS:
- Use `langchain-community`, `langchain`, `groq`
- Show `pip install` cell at top
- Use `ChatGroq` from `langchain_groq` for LangChain LLM calls
- All functions have clear docstrings
- Side-by-side comparisons must be in adjacent cells, not mixed together

---

OUTPUT FORMAT:
Single markdown file using `# %% [markdown]` and `# %%` cell markers.
```

---

---

# Prompt 03 — `03_context_window_problem.ipynb`

---

```
You are an expert AI educator creating a Jupyter notebook for a RAG learning series called "StoryVerse AI".

This is Notebook 03: "The Context Window Problem — Why Sending Everything Fails"

Your audience: developers continuing the series. They can load docs and inject context. Now they need to feel why it breaks before they'll care about embeddings.

---

NOTEBOOK GOAL:
Make the student feel the real cost and quality degradation of sending too much context. This notebook is the turning point — after this, embeddings and retrieval feel necessary, not optional.

---

STRUCTURE TO FOLLOW:

1. **Opening — The Setup (Markdown)**
   - "We can load all our stories and send them to the LLM. It works! So what's the problem?"
   - "Let's find out by actually measuring what happens as we add more documents."
   - Frame this as an experiment: we'll test answer quality and cost across document set sizes.

2. **Setup (Code)**
   - Imports: os, time, groq
   - Create Groq client
   - Generate a larger dataset: write a function that creates 50 fake story files programmatically
   - Each fake story: 300 words about a made-up movie (generate these inline — a simple template like "Movie {i}: Set in {place}, protagonist {name} must {goal}...")
   - Plus the 5 real stories from Notebook 02

3. **Experiment 1: Quality With 5 Documents (Code + Markdown)**
   - Load 5 real stories, combine, ask a specific question about one of them
   - Measure: response time, context length in chars, answer quality (print and eyeball)
   - Print a clean summary:
     ```
     Documents: 5
     Context length: X chars (~Y tokens)
     Response time: Z seconds
     Answer quality: [we'll judge manually]
     ```

4. **Experiment 2: Quality With 25 Documents (Code + Markdown)**
   - Add 20 fake stories to the context
   - Ask the SAME question about the same real story
   - Measure same metrics
   - Markdown: "Notice the context is now mostly irrelevant noise. The model has to find the needle in a haystack."

5. **Experiment 3: Quality With 55 Documents (Code + Markdown)**
   - All 55 documents combined
   - Same question
   - Compare answer — likely degraded or slower
   - Markdown:
     - Introduce: **"Lost in the Middle" problem** — LLMs pay more attention to the start and end of context. Middle content gets ignored.
     - Introduce: **Attention Dilution** — with more tokens, attention is spread thinner. Relevant content competes with noise.

6. **Visualize the Cost Problem (Code + Markdown)**
   - Create a simple table (print formatted) showing:
     ```
     Docs | Context Chars | ~Tokens | Est. Cost/Query | Cost/1M queries
     5    | 8,000         | 2,000   | $0.0002         | $200
     25   | 40,000        | 10,000  | $0.001          | $1,000
     55   | 88,000        | 22,000  | $0.0022         | $2,200
     ```
   - Use rough estimates (Groq Llama pricing or just say "at $0.1/M tokens")
   - Markdown: "This isn't hypothetical. Real apps have millions of queries per day."

7. **The Latency Problem (Code + Markdown)**
   - Time 3 calls with increasing context sizes using `time.time()`
   - Plot the results as a simple ASCII bar chart printed to console:
     ```python
     def ascii_bar(label, value, max_val, width=40):
         filled = int((value / max_val) * width)
         bar = "█" * filled + "░" * (width - filled)
         print(f"{label:20} | {bar} | {value:.2f}s")
     ```
   - Markdown: "Users notice latency above 2 seconds. With large context, you're already there."

8. **The Irrelevance Problem (Markdown + Code)**
   - Demonstrate: ask a question where the answer is in Document 1
   - Version A: send only Document 1 → correct, fast
   - Version B: send all 55 documents → slower, potentially less precise
   - Markdown:
     - "More context isn't just expensive — it actively makes answers worse."
     - "The model gets confused by irrelevant details. It might blend facts from unrelated stories."
     - "This is called **context poisoning** or **retrieval noise**."

9. **What We Actually Need (Markdown)**
   - We don't need to send everything. We need to send only what's relevant to the question.
   - For "What happens at the end of Interstellar?" → send only the Interstellar doc
   - For "Who is Hermione?" → send only the Harry Potter doc
   - How do we know which documents are relevant? We need to measure **relevance**.
   - Coming up: embeddings — a way to represent meaning as numbers so we can measure similarity.

10. **Summary Diagram (Markdown)**
    - ASCII diagram showing the two approaches:
      ```
      Current Approach:
      Question → ALL 55 Documents → LLM → Answer
                        ↑
                   Expensive. Slow. Noisy.
      
      What We Want:
      Question → [Find Relevant Docs] → 2-3 Documents → LLM → Answer
                                                ↑
                                         Fast. Cheap. Accurate.
      ```

11. **Exercises (Markdown)**
    - Experiment: does answer quality actually degrade for you? Try different questions.
    - Try: place the relevant document LAST in a long context — does the answer change?
    - Calculate: if your app gets 10,000 queries/day with 55-doc context, what's the monthly cost?

---

CODE STYLE:
- All experiments in clean, labeled functions
- Use `time.time()` for timing, not external libraries
- No external visualization libraries — use ASCII charts to avoid dependency issues
- Make measurements real — actually call the API, show real timing

---

OUTPUT FORMAT:
Single markdown file using `# %% [markdown]` and `# %%` cell markers.
```

---

---

# Prompt 04 — `04_embeddings_intuition.ipynb`

---

```
You are an expert AI educator creating a Jupyter notebook for a RAG learning series called "StoryVerse AI".

This is Notebook 04: "Embeddings — Teaching Meaning to Machines"

This is the most conceptually important notebook in the series. Take your time. Use lots of analogies. Visualize everything possible. This notebook should feel like a revelation.

---

NOTEBOOK GOAL:
Build deep, lasting intuition for what embeddings are, why they exist, and how they capture meaning. By the end, the student should understand why "Hogwarts" and "wizard school" are similar even though they share no words.

---

STRUCTURE TO FOLLOW:

1. **Opening — The Problem With Words (Markdown)**
   - Story: "Imagine you're building a search engine for StoryVerse AI. A user searches for 'wizard school'. Your database has a document about Hogwarts. How do you match them?"
   - Keyword search fails: "wizard school" ≠ "Hogwarts" — zero word overlap
   - We need a way to represent MEANING, not just letters.
   - That's what embeddings are.

2. **What Is an Embedding? (Markdown)**
   - An embedding is a list of numbers (a vector) that represents the meaning of a piece of text.
   - Example (simplified):
     ```
     "king"   → [0.2, 0.8, 0.1, 0.9, ...]  (768 numbers)
     "queen"  → [0.2, 0.8, 0.1, 0.85, ...]  (768 numbers, very similar)
     "table"  → [0.9, 0.1, 0.8, 0.2, ...]   (768 numbers, very different)
     ```
   - Similar meanings → similar numbers → close in space
   - Analogy: GPS coordinates. Mumbai and Pune have coordinates that are close. Mumbai and London are far apart. Embeddings are like GPS for meaning.

3. **The Vector Space Idea (Markdown)**
   - In 2D space, we can plot points and measure distance
   - Embeddings work the same way but in 768+ dimensions
   - We can't visualize 768 dimensions, but the math works the same
   - Analogy: you can't visualize 4D space, but you can calculate distances in it. Same here.
   - Show a simplified 2D analogy with manual coordinates:
     ```
     "magic"       → (0.9, 0.8)
     "wizard"      → (0.85, 0.82)
     "sorcerer"    → (0.88, 0.79)
     "database"    → (0.1, 0.2)
     "SQL query"   → (0.12, 0.18)
     ```
   - Plot these conceptually in a markdown ASCII grid

4. **Cosine Similarity Intuition (Markdown)**
   - We don't measure straight-line distance between vectors. We measure the angle between them.
   - Why? Because direction matters more than magnitude.
   - Analogy: two arrows pointing in the same direction are "similar" regardless of length. One arrow pointing left and one pointing right are "different".
   - Formula: `cosine_similarity = dot(A, B) / (|A| * |B|)`
   - Range: -1 (opposite) to 1 (identical)
   - For text: typically 0.7+ = similar, below 0.4 = unrelated

5. **Build Cosine Similarity From Scratch (Code + Markdown)**
   - Implement in pure Python:
     ```python
     import math
     
     def dot_product(a, b):
         return sum(x * y for x, y in zip(a, b))
     
     def magnitude(v):
         return math.sqrt(sum(x**2 for x in v))
     
     def cosine_similarity(a, b):
         return dot_product(a, b) / (magnitude(a) * magnitude(b))
     ```
   - Test with simple 3D vectors first (manually chosen to be similar/different)
   - Print similarity scores
   - Markdown: "This is the heart of all vector search. Everything else is optimization."

6. **Generate Real Embeddings (Code + Markdown)**
   - Install: `sentence-transformers`
   - Load model: `all-MiniLM-L6-v2` (fast, small, good quality)
   - Embed 10 sentences related to StoryVerse theme:
     ```python
     sentences = [
         "A young wizard attends a school of magic",
         "Harry Potter studies at Hogwarts",
         "A boy learns spells and potion-making",
         "A spacecraft travels through a wormhole",
         "An astronaut explores distant galaxies",
         "A soldier fights in medieval war",
         "Kings and queens battle for a throne",
         "Database query optimization techniques",
         "SQL joins and indexing strategies",
         "Python programming for beginners",
     ]
     ```
   - Show embedding shape: `embedding.shape` → (384,)
   - Markdown: "Each sentence is now 384 numbers. Similar sentences will have similar numbers."

7. **Keyword Search vs Semantic Search — The Big Demo (Code + Markdown)**
   - This is the emotional core of the notebook.
   - Query: "wizard school"
   - Keyword search: check which sentences contain "wizard" AND "school"
     - Show results: only exact matches (probably just sentence 1)
   - Semantic search: embed query, compute cosine similarity against all 10 sentences
     - Show results: sentences 1, 2, 3 all score high — "Hogwarts", "Harry Potter", "spells" all match
   - Print a ranked table:
     ```
     Rank | Score | Sentence
     1    | 0.91  | A young wizard attends a school of magic
     2    | 0.87  | Harry Potter studies at Hogwarts
     3    | 0.82  | A boy learns spells and potion-making
     ...
     9    | 0.12  | SQL joins and indexing strategies
     ```
   - Markdown: "Hogwarts appears highly ranked even though the word 'school' never appears in it. The model understands meaning."

8. **Visualize the Embedding Space (Code + Markdown)**
   - Use UMAP or PCA to reduce 384D embeddings to 2D (use sklearn PCA — it's simpler)
   - Print a conceptual ASCII cluster diagram:
     ```
     Semantic Space (2D projection):
     
          ·  "wizard school"         Magic/Fantasy cluster
          ·  "Harry Potter"          · · ·
          ·  "Hogwarts"              · · ·
     
                              ·  "astronaut"      Sci-Fi cluster
                              ·  "wormhole"       · · ·
     
                                          ·  "SQL"     Tech cluster
                                          ·  "Python"  · · ·
     ```
   - If matplotlib is available, generate a real scatter plot with labels
   - Markdown: "Stories about magic cluster together. Space stories cluster together. They never overlap. The model learned this from reading the internet."

9. **Store Embeddings (Code)**
   - Embed all 5 StoryVerse story files from Notebook 02
   - Store as a Python list of dicts:
     ```python
     embedded_docs = [
         {
             "filename": "interstellar.txt",
             "content": "...",
             "embedding": [0.12, 0.34, ...]
         },
         ...
     ]
     ```
   - Save to a JSON file: `data/embeddings.json`
   - Markdown: "This is the most primitive vector store. It's just a JSON file. Next notebook — we search it."

10. **Embedding Models Comparison (Markdown)**
    - Brief table:
      | Model | Dimensions | Speed | Quality | Best For |
      |---|---|---|---|---|
      | all-MiniLM-L6-v2 | 384 | Fast | Good | Prototyping |
      | all-mpnet-base-v2 | 768 | Medium | Better | Production |
      | BGE-large | 1024 | Slow | Best | High accuracy |
      | OpenAI text-embedding-3-small | 1536 | API | Excellent | Cloud apps |
    - Recommendation: start with MiniLM, upgrade when needed

11. **Summary (Markdown)**
    - Embeddings = meaning as numbers
    - Similar meaning → similar vectors → low angle → high cosine similarity
    - Semantic search beats keyword search for natural language
    - We now have embeddings stored. Next: build search on top of them.

12. **Exercises (Markdown)**
    - Try: embed "Baahubali" and "epic war drama" — what's the similarity score?
    - Try: embed your own name and a random word — what's the score?
    - Try: find two sentences that sound different but should be semantically similar

---

CODE REQUIREMENTS:
- sentence-transformers for embeddings
- Pure Python for cosine similarity (no scipy yet)
- sklearn PCA for dimensionality reduction (optional but preferred)
- matplotlib for scatter plot (wrap in try/except if not available)
- Save embeddings to JSON so Notebook 05 can load them

---

WRITING STYLE:
- This notebook should have the most explanation of all notebooks
- Every code cell should be preceded by a markdown cell that explains WHAT you're about to do and WHY
- Use lots of "Notice that...", "This is surprising because...", "Here's the key insight..."
- Make the keyword vs semantic comparison feel dramatic

---

OUTPUT FORMAT:
Single markdown file using `# %% [markdown]` and `# %%` cell markers.
```

---

---

# Prompt 05 — `05_vector_search_from_scratch.ipynb`

---

```
You are an expert AI educator creating a Jupyter notebook for a RAG learning series called "StoryVerse AI".

This is Notebook 05: "Build Vector Search From Scratch — Then Use FAISS"

This is the notebook where everything clicks. Students build a mini vector database themselves, then see FAISS as a faster version of exactly what they built.

---

NOTEBOOK GOAL:
Build a complete vector search engine in pure Python. Then replicate it with FAISS. Students should finish this notebook thinking: "A vector DB is just optimized similarity search. I could build one."

---

STRUCTURE TO FOLLOW:

1. **Opening (Markdown)**
   - "We have embeddings stored in a JSON file. Now we need to search them."
   - "Before we use any vector database library, let's build one ourselves. In pure Python. It'll take about 50 lines."
   - "This is the most important notebook in the series. Everything in production RAG is just an optimized version of what we build here."

2. **Load Embeddings (Code)**
   - Load `data/embeddings.json` from Notebook 04
   - Print count and preview first document (filename, content preview, embedding first 5 values)

3. **Build The Vector Store Class — Pure Python (Code + Markdown)**
   - Build it step by step, one method at a time:
   ```python
   class SimpleVectorStore:
       def __init__(self):
           self.documents = []  # list of {"content": str, "metadata": dict, "embedding": list}
       
       def add_document(self, content, embedding, metadata=None):
           """Add a document with its embedding."""
           ...
       
       def similarity_search(self, query_embedding, top_k=3):
           """Find top-k most similar documents."""
           ...
       
       def _cosine_similarity(self, a, b):
           """Pure Python cosine similarity."""
           ...
   ```
   - After each method: explain what it does and why
   - Show the complete class once assembled

4. **Test The Vector Store (Code + Markdown)**
   - Instantiate, add all 5 story documents
   - Embed a query: "space travel through wormhole"
   - Call `similarity_search(query_embedding, top_k=3)`
   - Print results with scores:
     ```
     Result 1 (score: 0.89): interstellar.txt
     Result 2 (score: 0.61): dark_knight.txt
     Result 3 (score: 0.43): portal_bookshop.txt
     ```
   - Markdown: "Interstellar ranked first. The model understands the question is about space travel, even though we didn't say 'Interstellar'."

5. **Test Multiple Queries (Code)**
   - Test 5 different queries covering all 5 stories
   - For each: show top result and score
   - Make sure each story surfaces as top result for a relevant query
   - Markdown: "Our 50-line vector store works correctly. Now — what's wrong with it?"

6. **What's Wrong With Our Implementation? (Markdown)**
   - Time complexity: for each query, we compare against EVERY document: O(n)
   - Space: we store all embeddings in memory as Python lists
   - No persistence: restart the program = rebuild everything
   - No batching: we embed one document at a time
   - "For 5 documents this is fine. For 5 million? It would take minutes per query."
   - Preview: "FAISS solves all of this."

7. **What Is FAISS? (Markdown)**
   - Facebook AI Similarity Search — open source library for efficient vector search
   - Uses approximate nearest neighbor algorithms (ANN) — faster than exact search, slightly less accurate
   - Supports indexing structures: Flat, IVF, HNSW — each with different speed/accuracy tradeoffs
   - Core insight: "FAISS does the same thing we built, but 1000x faster on large datasets"
   - For < 10,000 docs: our pure Python version is honestly fine. FAISS matters at scale.

8. **Build The Same Store With FAISS (Code + Markdown)**
   - Install faiss-cpu
   - Replicate the same interface:
   ```python
   import faiss
   import numpy as np
   
   class FAISSVectorStore:
       def __init__(self, dimension=384):
           self.index = faiss.IndexFlatIP(dimension)  # Inner Product = cosine if normalized
           self.documents = []
       
       def add_document(self, content, embedding, metadata=None):
           embedding_np = np.array([embedding], dtype=np.float32)
           faiss.normalize_L2(embedding_np)  # normalize for cosine similarity
           self.index.add(embedding_np)
           self.documents.append({"content": content, "metadata": metadata})
       
       def similarity_search(self, query_embedding, top_k=3):
           query_np = np.array([query_embedding], dtype=np.float32)
           faiss.normalize_L2(query_np)
           scores, indices = self.index.search(query_np, top_k)
           return [
               {"document": self.documents[i], "score": scores[0][j]}
               for j, i in enumerate(indices[0])
           ]
   ```
   - Explain each line: what is IndexFlatIP, why normalize, what does search return

9. **Side By Side Comparison (Code + Markdown)**
   - Run the same 5 queries on both stores
   - Show results are identical (or very close)
   - Time both on the 5-document set: probably similar
   - Then simulate scale:
     ```python
     # Generate 10,000 random embeddings
     fake_embeddings = np.random.rand(10000, 384).astype(np.float32)
     
     # Time pure Python search vs FAISS search
     # (add all to both stores, time one query each)
     ```
   - Show FAISS is dramatically faster at scale

10. **Introduce Chroma (Markdown + Code)**
    - FAISS is great but requires manual embedding management
    - Chroma: a vector DB that handles embeddings + documents + metadata + persistence together
    - Show the same operation in Chroma:
      ```python
      import chromadb
      client = chromadb.Client()
      collection = client.create_collection("storyverse")
      collection.add(documents=[...], embeddings=[...], ids=[...])
      results = collection.query(query_embeddings=[...], n_results=3)
      ```
    - Markdown: "Chroma is essentially our FAISSVectorStore class, but with persistence, metadata filtering, and a nicer API."

11. **The Progression Summary (Markdown)**
    - ASCII diagram:
      ```
      Python List → SimpleVectorStore (ours) → FAISS → Chroma → Pinecone/Qdrant
      
      Increasing: Scale, Features, Persistence, Cost
      Decreasing: Simplicity, Control, Transparency
      ```
    - "Start with what you understand. Scale when you need to."

12. **Save the FAISS Index (Code)**
    - Show how to save and reload a FAISS index:
      ```python
      faiss.write_index(store.index, "data/storyverse.faiss")
      loaded_index = faiss.read_index("data/storyverse.faiss")
      ```
    - This will be used in Notebook 07.

13. **Exercises (Markdown)**
    - Add a new story, search for it, verify it surfaces correctly
    - Modify SimpleVectorStore to also support `delete_document` — how would you implement it?
    - Try `faiss.IndexIVFFlat` — how does it differ from `IndexFlatIP`?

---

CODE REQUIREMENTS:
- faiss-cpu, numpy, sentence-transformers, chromadb
- All classes fully implemented, not pseudocode
- Actual timing comparisons with real numbers
- Comments on every non-obvious line

---

OUTPUT FORMAT:
Single markdown file using `# %% [markdown]` and `# %%` cell markers.
```

---

---

# Prompt 06 — `06_chunking_strategies.ipynb`

---

```
You are an expert AI educator creating a Jupyter notebook for a RAG learning series called "StoryVerse AI".

This is Notebook 06: "Chunking — The Art of Slicing Documents Right"

---

NOTEBOOK GOAL:
Show why document chunking exists, what happens without it, and how different chunking strategies affect retrieval quality. This should feel like a Goldilocks problem: chunks too small = lose meaning. Chunks too large = noisy retrieval.

---

STRUCTURE TO FOLLOW:

1. **Opening — A New Problem (Markdown)**
   - Our vector store works great for 5 short story summaries (150 words each)
   - But what about real documents?
     - Full movie screenplay: 20,000 words
     - A PDF book: 200,000 words
     - A Wikipedia article: 5,000 words
   - If we embed the entire screenplay as one document, what gets retrieved? The entire screenplay — even if the user only asked about one scene.
   - We need to split documents into smaller pieces. That's chunking.

2. **Demo: Retrieve Without Chunking (Code + Markdown)**
   - Create a long document: combine all 5 StoryVerse stories into one 1000-word "StoryVerse Bible" text
   - Embed the whole thing as one document
   - Ask a specific question: "What does Hermione do in the library?"
   - Show what gets retrieved: the entire 1000-word blob
   - Inject it into LLM: the answer might be correct, but the context is wasteful
   - Markdown: "We're sending 900 irrelevant words to get 10 relevant ones."

3. **What Is Chunking? (Markdown)**
   - Chunking = splitting a long document into smaller overlapping pieces
   - Each chunk gets its own embedding
   - When searching, we retrieve specific chunks — not whole documents
   - Analogy: a book has chapters, paragraphs, sentences. We don't search for books — we search for paragraphs.
   - Key question: how big should a chunk be?

4. **Strategy 1: Fixed-Size Chunking (Code + Markdown)**
   - Split by character count: every 200 characters = one chunk
   - Implement pure Python:
     ```python
     def fixed_size_chunks(text, chunk_size=200, overlap=0):
         chunks = []
         for i in range(0, len(text), chunk_size - overlap):
             chunks.append(text[i:i + chunk_size])
         return chunks
     ```
   - Apply to Interstellar story, print chunks
   - Problem demo: a chunk that cuts mid-sentence:
     ```
     Chunk 7: "...Cooper enters the wormhole. The gravitational force pulls him toward"
     Chunk 8: " the singularity. He realizes he's in the tesseract..."
     ```
   - Markdown: "Fixed size is simple but brutally ignores sentence and paragraph boundaries. Meaning gets sliced apart."

5. **Adding Overlap (Code + Markdown)**
   - Modify function to add overlap: each chunk shares N characters with the previous chunk
   - Show the same example with overlap=50:
     ```
     Chunk 7: "...Cooper enters the wormhole. The gravitational force pulls him toward"
     Chunk 8: "force pulls him toward the singularity. He realizes he's in the tesseract..."
     ```
   - Markdown: "Overlap ensures context isn't lost at boundaries. But how much overlap is right?"
   - Show: too little overlap = disconnected chunks. Too much = duplicated content in retrieval.
   - Rule of thumb: 10-20% overlap.

6. **Strategy 2: Recursive Character Splitting (Code + Markdown)**
   - The smarter approach: try to split at natural boundaries — paragraphs first, then sentences, then words, then characters
   - Implement simplified version:
     ```python
     def recursive_split(text, chunk_size=300, separators=["\n\n", "\n", ". ", " ", ""]):
         if len(text) <= chunk_size:
             return [text]
         for sep in separators:
             if sep in text:
                 parts = text.split(sep)
                 # recombine parts that are too small
                 ...
         return [text]  # fallback
     ```
   - Show the difference: chunks now end at paragraph/sentence boundaries
   - Markdown: "This is what LangChain's RecursiveCharacterTextSplitter does internally."

7. **LangChain Text Splitters (Code + Markdown)**
   - Show `RecursiveCharacterTextSplitter`:
     ```python
     from langchain.text_splitter import RecursiveCharacterTextSplitter
     
     splitter = RecursiveCharacterTextSplitter(
         chunk_size=300,
         chunk_overlap=50,
         separators=["\n\n", "\n", ". ", " ", ""]
     )
     chunks = splitter.split_text(interstellar_story)
     ```
   - Show `CharacterTextSplitter` for comparison
   - Print chunks, highlight how they respect sentence boundaries
   - Reminder: "This is our pure Python implementation with a nicer API. No magic."

8. **Strategy 3: Semantic Chunking (Markdown + Code)**
   - What if we split at meaning boundaries, not character boundaries?
   - Approach: embed consecutive sentences, find where embedding similarity drops sharply — that's a topic boundary
   - Simplified implementation:
     ```python
     def semantic_chunk(sentences, model, threshold=0.7):
         embeddings = model.encode(sentences)
         chunks = []
         current_chunk = [sentences[0]]
         for i in range(1, len(sentences)):
             sim = cosine_similarity(embeddings[i-1], embeddings[i])
             if sim < threshold:  # topic shift
                 chunks.append(" ".join(current_chunk))
                 current_chunk = []
             current_chunk.append(sentences[i])
         chunks.append(" ".join(current_chunk))
         return chunks
     ```
   - Apply to a story with clear topic shifts (use Interstellar: Earth section → Space section → Tesseract section)
   - Show how chunks align with actual plot sections
   - Markdown: "Semantic chunking is more powerful but slower. Use for documents with clear topic sections."

9. **The Goldilocks Experiment (Code + Markdown)**
   - This is the payoff cell.
   - Take the Interstellar story, create 3 chunk sets: small (100 chars), medium (300 chars), large (800 chars)
   - Embed all, add to separate vector stores
   - Ask: "What does Cooper say before entering the wormhole?"
   - Retrieve top chunk from each store, print and compare:
     - Small: might cut the quote mid-sentence
     - Medium: captures the full dialogue + context
     - Large: captures the scene but also 3 unrelated paragraphs
   - Markdown: "Medium wins. Not because 300 is magic — because it respects sentence boundaries AND keeps enough context."
   - Print a clear summary table:
     ```
     Chunk Size | Retrieved Content Quality | Context Efficiency
     Too Small  | Loses meaning             | High (cheap)
     Just Right | Captures full context     | Medium
     Too Large  | Noisy, off-topic          | Low (expensive)
     ```

10. **Metadata Enrichment (Code + Markdown)**
    - Each chunk should carry metadata: source file, chunk index, character offset
    - Show:
      ```python
      chunks_with_metadata = [
          {
              "content": chunk_text,
              "metadata": {
                  "source": "interstellar.txt",
                  "chunk_index": i,
                  "char_start": i * chunk_size
              }
          }
          for i, chunk_text in enumerate(chunks)
      ]
      ```
    - Markdown: "Metadata lets us cite sources, filter by story, and debug bad retrievals."

11. **Summary + What's Next (Markdown)**
    - Key rules:
      - Use RecursiveCharacterTextSplitter as default
      - 200-500 chars chunk size for short story content
      - 10-20% overlap
      - Always attach metadata
    - Next: take our chunked, embedded documents and build the full retrieval pipeline.

12. **Exercises**
    - Try chunk sizes 50, 100, 200, 500, 1000 on the same document — observe how quality changes
    - Add your own text (a Wikipedia article) and find the best chunk size for it
    - Implement a chunk that always ends at a period — never mid-sentence

---

OUTPUT FORMAT:
Single markdown file using `# %% [markdown]` and `# %%` cell markers.
```

---

---

# Prompt 07 — `07_retrieval_pipeline.ipynb`

---

```
You are an expert AI educator creating a Jupyter notebook for a RAG learning series called "StoryVerse AI".

This is Notebook 07: "The Retrieval Pipeline — Connecting All the Pieces"

---

NOTEBOOK GOAL:
For the first time, connect chunking + embedding + vector search into a complete retrieval pipeline. The student should see all previous concepts unite into one clean flow.

---

STRUCTURE TO FOLLOW:

1. **Opening — Assembly Day (Markdown)**
   - "We've built each component separately. Today we assemble them."
   - List what we have: chunking (Notebook 06), embeddings (Notebook 04), vector store (Notebook 05)
   - What we're building: a pipeline that takes a raw folder of stories and a question, and returns the most relevant chunks
   - ASCII pipeline preview:
     ```
     Raw Text Files
          ↓
     [Load Documents]
          ↓
     [Chunk Documents]
          ↓
     [Generate Embeddings]
          ↓
     [Store in Vector DB]
          ↓  ← (above happens once at index time)
     ─────────────────
          ↓  ← (below happens per query)
     User Question
          ↓
     [Embed Question]
          ↓
     [Similarity Search]
          ↓
     Retrieved Chunks
     ```

2. **Setup (Code)**
   - All imports: os, json, faiss, numpy, sentence_transformers, langchain pieces
   - Load model: `SentenceTransformer("all-MiniLM-L6-v2")`
   - Define paths

3. **Phase 1: Indexing Pipeline (Code + Markdown)**
   - Build `index_documents(folder_path)` function that:
     1. Loads all .txt files
     2. Chunks each with RecursiveCharacterTextSplitter (chunk_size=300, overlap=50)
     3. Embeds all chunks
     4. Adds to FAISS index with metadata
     5. Returns the index + document store
   - Run it on `data/stories/`
   - Print: total files, total chunks, embedding dimensions
   - Markdown: "This runs ONCE when you set up the system. Not on every query."

4. **Phase 2: Retrieval Function (Code + Markdown)**
   - Build `retrieve(query, index, doc_store, top_k=3)`:
     1. Embed the query
     2. Search FAISS
     3. Return top-k chunks with scores and metadata
   - Test with 5 queries
   - Print results in formatted table:
     ```
     Query: "what happens in the wormhole"
     
     Rank 1 (score: 0.87) | interstellar.txt
     "Cooper is pulled through the wormhole, experiencing time dilation..."
     
     Rank 2 (score: 0.54) | portal_bookshop.txt  
     "Arjun stepped through the portal, feeling the same gravitational pull..."
     ```

5. **Understanding Score Thresholds (Code + Markdown)**
   - Show what happens with a completely unrelated query: "What is the best Python ORM?"
   - Even though it's irrelevant, FAISS returns results (it always returns top-k)
   - The scores will be low: 0.1, 0.15, 0.2
   - Introduce score filtering:
     ```python
     def retrieve_with_threshold(query, index, doc_store, top_k=3, min_score=0.5):
         results = retrieve(query, index, doc_store, top_k)
         return [r for r in results if r["score"] >= min_score]
     ```
   - Show: irrelevant query now returns empty list
   - Markdown: "Without thresholds, RAG will hallucinate based on irrelevant context. Always filter low-confidence retrievals."

6. **LangChain Retriever (Code + Markdown)**
   - Rebuild the same pipeline using LangChain:
     - `Chroma` vector store (simpler API than FAISS for LangChain)
     - `HuggingFaceEmbeddings` wrapper
     - `.as_retriever(search_kwargs={"k": 3})`
   - Show: `retriever.get_relevant_documents(query)` returns same chunks
   - Side-by-side comparison with our pure Python version
   - Markdown: "LangChain's retriever interface standardizes this so you can swap vector stores without changing query code."

7. **Retrieval Quality Analysis (Code + Markdown)**
   - Run 10 test queries (2 per story)
   - For each: manually verify if the top result is actually relevant (print and inspect)
   - Identify at least one failure case where retrieval is wrong or borderline
   - Markdown: "Perfect retrieval is hard. Later (Notebook 09) we'll see techniques to fix bad retrieval."

8. **Persist the Index (Code)**
   - Save FAISS index + doc store to disk
   - Save Chroma collection to disk (persistent client)
   - Show how to reload without re-indexing
   - Markdown: "Indexing takes time. Always persist. Only re-index when documents change."

9. **What We Have So Far (Markdown)**
   - Full pipeline diagram now filled in:
     ```
     Stories → Chunks → Embeddings → FAISS Index  [build once]
     Question → Embed → Search → Top-3 Chunks      [per query]
     ```
   - Missing piece: we retrieve chunks but don't answer questions yet
   - Next notebook: connect retrieval to LLM to generate answers

10. **Exercises**
    - Try top_k=1, 3, 5, 10 — how does answer context change?
    - Try min_score=0.3, 0.5, 0.7 — what gets filtered?
    - Add a new story to the folder. Re-index. Verify it's retrievable.

---

OUTPUT FORMAT:
Single markdown file using `# %% [markdown]` and `# %%` cell markers.
```

---

---

# Prompt 08 — `08_complete_rag_pipeline.ipynb`

---

```
You are an expert AI educator creating a Jupyter notebook for a RAG learning series called "StoryVerse AI".

This is Notebook 08: "Complete RAG — From Question to Grounded Answer"

This is the payoff notebook. Everything comes together.

---

NOTEBOOK GOAL:
Build a complete, working RAG system that takes a question, retrieves relevant chunks, and generates a grounded answer with source attribution. Compare RAG answers vs non-RAG answers on the same questions to feel the quality difference.

---

STRUCTURE TO FOLLOW:

1. **Opening — The Final Assembly (Markdown)**
   - "We have a retrieval pipeline. We have an LLM. Today we connect them."
   - Simple ASCII showing what's new:
     ```
     [Retrieval Pipeline] → Retrieved Chunks
                                    ↓
                            [Prompt Builder]
                                    ↓
                               [Groq LLM]
                                    ↓
                         Grounded Answer + Sources
     ```

2. **Load Everything (Code)**
   - Load persisted FAISS index + doc store from Notebook 07
   - Initialize embedding model and Groq client
   - Quick sanity check: retrieve one query, print results

3. **Build the RAG Prompt (Code + Markdown)**
   - This is the most important function in RAG:
     ```python
     def build_rag_prompt(question, retrieved_chunks):
         context_parts = []
         for i, chunk in enumerate(retrieved_chunks):
             source = chunk["metadata"].get("source", "unknown")
             context_parts.append(f"[Source {i+1}: {source}]\n{chunk['content']}")
         
         context = "\n\n".join(context_parts)
         
         prompt = f"""You are StoryVerse AI, an assistant that answers questions about movies and stories.

     Answer the question using ONLY the provided context. If the answer is not in the context, say "I don't have enough information about this in my knowledge base."

     Context:
     {context}

     Question: {question}

     Answer:"""
         return prompt
     ```
   - Explain each part: why "ONLY the provided context", why the fallback instruction, why source labels

4. **Build the Complete RAG Function (Code)**
   - Assemble everything:
     ```python
     def rag_answer(question, index, doc_store, top_k=3, min_score=0.5):
         # Step 1: Retrieve
         chunks = retrieve_with_threshold(question, index, doc_store, top_k, min_score)
         
         if not chunks:
             return {
                 "answer": "I don't have information about this topic.",
                 "sources": [],
                 "retrieved_chunks": 0
             }
         
         # Step 2: Build prompt
         prompt = build_rag_prompt(question, chunks)
         
         # Step 3: Generate
         response = client.chat.completions.create(
             model=MODEL,
             messages=[{"role": "user", "content": prompt}],
             temperature=0.1
         )
         
         answer = response.choices[0].message.content
         sources = list(set(c["metadata"]["source"] for c in chunks))
         
         return {
             "answer": answer,
             "sources": sources,
             "retrieved_chunks": len(chunks),
             "chunks_used": chunks
         }
     ```

5. **The Big Demo — RAG vs No-RAG (Code + Markdown)**
   - This is the emotional core of the notebook.
   - Run 5 questions, each time showing:
     - WITHOUT RAG (bare LLM call): the hallucinated or uncertain answer
     - WITH RAG: the grounded, correct answer with sources
   - Format output clearly:
     ```
     ═══════════════════════════════════════════════
     Question: "What is the name of Cooper's daughter in Interstellar?"
     
     ❌ Without RAG:
     "Cooper's daughter in Interstellar is named Jessica." [WRONG]
     
     ✅ With RAG:
     "Cooper's daughter is named Murph (Murphy). [Source: interstellar.txt]"
     ═══════════════════════════════════════════════
     ```
   - Choose questions where hallucination is obvious so the difference is dramatic

6. **Source Attribution (Code + Markdown)**
   - Show the retrieved chunks alongside the answer
   - Highlight which part of which chunk was used
   - Markdown: "In production systems, you'd show 'Based on: [filename, page X]' to users. This builds trust."
   - Show how to extract and format source citations cleanly

7. **The "I Don't Know" Behavior (Code + Markdown)**
   - Ask a question about something NOT in the knowledge base: "What happens in Oppenheimer?"
   - With threshold filtering: no chunks returned → graceful fallback message
   - Without threshold: low-confidence chunks retrieved → LLM might hallucinate despite instructions
   - Markdown: "Teaching an LLM to say 'I don't know' is one of the hardest parts of production RAG. Score thresholds are your first line of defense."

8. **LangChain RetrievalQA Version (Code + Markdown)**
   - Now implement the same RAG system with LangChain:
     ```python
     from langchain.chains import RetrievalQA
     from langchain_groq import ChatGroq
     
     llm = ChatGroq(model="llama-3.1-8b-instant", temperature=0.1)
     qa_chain = RetrievalQA.from_chain_type(
         llm=llm,
         retriever=chroma_retriever,
         return_source_documents=True
     )
     result = qa_chain.invoke({"query": question})
     ```
   - Show same question, same answer
   - Markdown: "Now you can appreciate what `RetrievalQA.from_chain_type` actually does — because you built it yourself in the previous 60 lines."
   - This is the payoff for teaching pure Python first.

9. **Hallucination Comparison Table (Markdown + Code)**
   - Build a structured comparison for 5 questions:
     ```python
     test_cases = [
         {
             "question": "...",
             "correct_answer": "...",  # ground truth
         },
         ...
     ]
     ```
   - For each: call both bare LLM and RAG, print comparison
   - Manually label: correct / hallucinated / partial
   - Markdown: "RAG doesn't eliminate hallucination. It reduces it significantly by grounding the model."

10. **What Makes a Good RAG Prompt? (Markdown)**
    - Key prompt engineering rules for RAG:
      - Always include "Answer ONLY using the provided context"
      - Always include a fallback for missing information
      - Include source labels in context
      - Keep system instructions short
      - Use low temperature (0.0–0.1) for factual tasks
    - Common mistakes:
      - No fallback → model makes up answers when context is insufficient
      - High temperature → model "interprets" context too freely
      - Too many chunks → context dilution returns

11. **StoryVerse AI — Interactive Mode (Code)**
    - Build a simple REPL loop:
      ```python
      print("StoryVerse AI — Ask me about movies and stories!")
      print("Type 'quit' to exit\n")
      
      while True:
          question = input("You: ").strip()
          if question.lower() == "quit":
              break
          result = rag_answer(question, index, doc_store)
          print(f"\nStoryVerse AI: {result['answer']}")
          print(f"Sources: {', '.join(result['sources'])}\n")
      ```
    - Markdown: "This is a real RAG chatbot. It's not conversational yet (no memory between turns), but it retrieves and answers correctly."

12. **Summary — What We've Built (Markdown)**
    - Full system diagram with all components labeled
    - What we learned: grounding, source attribution, "I don't know", prompt engineering
    - What's still missing: bad retrieval cases, conversational memory, evaluation

13. **Exercises**
    - Add 3 new stories, re-index, verify they're retrievable
    - Try asking the same question with different top_k values — how does the answer change?
    - Try removing the "ONLY the provided context" instruction — what happens?

---

OUTPUT FORMAT:
Single markdown file using `# %% [markdown]` and `# %%` cell markers.
```

---

---

# Prompt 09 — `09_rag_problems_and_fixes.ipynb`

---

```
You are an expert AI educator creating a Jupyter notebook for a RAG learning series called "StoryVerse AI".

This is Notebook 09: "RAG Problems in the Wild — And How to Fix Them"

This notebook separates your content from shallow RAG tutorials. Demonstrate real failure modes and real fixes.

---

NOTEBOOK GOAL:
Show 6 real RAG failure modes, explain why they happen, and introduce the standard fix for each. Students should leave feeling like they've seen production-level thinking.

---

STRUCTURE TO FOLLOW:

1. **Opening (Markdown)**
   - "Our RAG system works. But 'works on 5 clean story files' is not the same as 'works in production'."
   - "Every RAG system eventually hits these problems. Let's see them now, so you're not surprised later."
   - List the 6 problems we'll cover (preview)

2. **Problem 1: Bad Retrieval — Wrong Chunks Returned (Code + Markdown)**
   - Demonstrate: add a new story with ambiguous content (e.g., a story about a "dark knight" chess piece — not the movie)
   - Ask: "Who is the Dark Knight?"
   - Show: retrieval returns the chess story, not the Batman story — because both are semantically close
   - Root cause: embedding models don't have domain-specific knowledge
   - Fix: **Metadata Filtering**
     ```python
     # Add genre/type metadata when indexing
     # Filter at retrieval time
     results = collection.query(
         query_embeddings=[...],
         where={"genre": "superhero"}  # Chroma metadata filter
     )
     ```
   - Markdown: "When your corpus has multiple domains, metadata filters let users scope their search."

3. **Problem 2: The Lost in the Middle Problem (Code + Markdown)**
   - Empirically demonstrate with a controlled experiment:
     - Create a prompt with 7 chunks: relevant chunk placed at position 1, 4, and 7
     - Ask a question that requires the relevant chunk
     - Show answer quality varies by position
   - Explanation: LLMs have primacy and recency bias. Middle content gets less attention.
   - Fix: **Reranking** — retrieve more candidates, reorder so the best chunks are at the start/end
     ```python
     def rerank_chunks(query, chunks, top_n=3):
         # Simple reranking: re-score with a cross-encoder or just put highest-scoring first
         sorted_chunks = sorted(chunks, key=lambda x: x["score"], reverse=True)
         # Put best match first, second best last, rest in middle
         if len(sorted_chunks) >= 3:
             reranked = [sorted_chunks[0]] + sorted_chunks[2:] + [sorted_chunks[1]]
             return reranked[:top_n]
         return sorted_chunks
     ```
   - Mention: production systems use cross-encoders (Cohere Rerank, BGE reranker) for this

4. **Problem 3: Query-Document Mismatch (Code + Markdown)**
   - Demonstrate: user asks "What's the twist in Interstellar?" (short, informal)
   - The relevant chunk is: "The film's central revelation is that the tesseract was built by future humans..." (formal, dense)
   - Embedding similarity between short informal query and long formal chunk can be low
   - Fix 1: **Query Expansion** — rewrite the query to be more descriptive before embedding:
     ```python
     def expand_query(question):
         expansion_prompt = f"""Rewrite this question to be more detailed and descriptive for document search. 
     Return only the rewritten question.
     
     Original: {question}
     Expanded:"""
         # Call LLM to expand
         ...
     ```
   - Fix 2: **HyDE (Hypothetical Document Embeddings)** — generate a hypothetical answer, embed that instead of the query:
     ```python
     def hyde_retrieve(question):
         hypothetical_answer = generate_hypothetical_answer(question)  # LLM call
         return retrieve(hypothetical_answer, index, doc_store)  # retrieve using generated answer
     ```
   - Show both approaches improve retrieval on the mismatch case

5. **Problem 4: Duplicate and Redundant Chunks (Code + Markdown)**
   - Create a scenario: load the same story file twice (simulating a common indexing bug)
   - Retrieve and show: top 3 results are all duplicates of the same chunk
   - The LLM answer uses the same information 3 times — wastes context
   - Fix: **Deduplication**
     ```python
     def deduplicate_chunks(chunks, similarity_threshold=0.95):
         seen = []
         unique = []
         for chunk in chunks:
             is_duplicate = any(
                 cosine_similarity(chunk["embedding"], s["embedding"]) > similarity_threshold
                 for s in seen
             )
             if not is_duplicate:
                 unique.append(chunk)
                 seen.append(chunk)
         return unique
     ```
   - Also: deduplication during indexing (hash-based: skip identical content)

6. **Problem 5: Stale Embeddings (Markdown + Code)**
   - Scenario: you indexed 100 stories. You then update 10 of them (e.g., corrected plot summaries). Your index still has the old embeddings.
   - The LLM answers based on outdated information.
   - Fix strategies:
     - **Delete + Re-add**: remove old document embeddings by ID, add new ones
     - **Versioning**: tag each embedding with a version or timestamp
     - **Full re-index**: simplest but slowest — rebuild from scratch nightly
   - Show delete + re-add in Chroma:
     ```python
     collection.delete(ids=["interstellar_chunk_1", "interstellar_chunk_2"])
     collection.add(documents=[new_content], embeddings=[new_embedding], ids=[...])
     ```
   - Markdown: "In production: track document modification timestamps. Re-embed on change."

7. **Problem 6: Noisy and Messy Documents (Code + Markdown)**
   - Simulate a badly formatted document: add HTML tags, OCR artifacts, repeated headers
     ```
     <html><body>INTERSTELLAR\nINTERSTELLAR\n\n
     C00per (sic) is a f0rmer NASA pil0t...
     \x00\x00 PAGE 1 OF 22 \x00\x00
     ```
   - Show: embeddings of noisy text are semantically polluted
   - Fix: **Document Cleaning Pipeline**
     ```python
     import re
     
     def clean_document(text):
         text = re.sub(r'<[^>]+>', '', text)        # remove HTML
         text = re.sub(r'\x00+', '', text)           # remove null bytes
         text = re.sub(r'\n{3,}', '\n\n', text)      # collapse excessive newlines
         text = re.sub(r' {2,}', ' ', text)          # collapse spaces
         text = text.strip()
         return text
     ```
   - Show before/after on the noisy document
   - Markdown: "Always clean before chunking. Garbage in = garbage embeddings."

8. **Problem Summary Table (Markdown)**
   - Clean reference table:
     ```
     Problem                | Root Cause              | Fix
     ─────────────────────────────────────────────────────────────────
     Wrong chunks returned  | Corpus ambiguity        | Metadata filtering
     Lost in the middle     | LLM attention bias      | Reranking
     Query-doc mismatch     | Style/length mismatch   | Query expansion / HyDE
     Duplicate chunks       | Indexing bug            | Deduplication
     Stale embeddings       | No update tracking      | Delete + re-add by ID
     Noisy documents        | Bad source quality      | Cleaning pipeline
     ```

9. **Hybrid Search — Bonus Fix (Code + Markdown)**
   - Semantic search alone misses exact keyword matches (names, IDs, codes)
   - Example: user asks "What happens in scene 42B?" — no semantic meaning to "42B"
   - Fix: **Hybrid Search** = semantic score + BM25 keyword score combined
   - Show BM25 with `rank_bm25`:
     ```python
     from rank_bm25 import BM25Okapi
     tokenized = [doc["content"].split() for doc in doc_store]
     bm25 = BM25Okapi(tokenized)
     keyword_scores = bm25.get_scores(query.split())
     ```
   - Combine scores: `final_score = alpha * semantic_score + (1-alpha) * keyword_score`
   - Markdown: "Most production RAG systems use hybrid search. Pure semantic misses exact matches. Pure keyword misses meaning."

10. **Summary + Exercises**
    - "Now you know the 6 most common production RAG failures. Most tutorials don't show you these."
    - Exercises:
      - Introduce a new noise pattern (e.g., repeated page numbers) and extend the cleaner
      - Try different alpha values for hybrid search — when does keyword search help more?
      - Create a deliberate stale embedding scenario and verify the fix works

---

OUTPUT FORMAT:
Single markdown file using `# %% [markdown]` and `# %%` cell markers.
```

---

---

# Prompt 10 — `10_evaluation_and_langsmith.ipynb`

---

```
You are an expert AI educator creating a Jupyter notebook for a RAG learning series called "StoryVerse AI".

This is Notebook 10: "RAG Evaluation and Observability with LangSmith"

---

NOTEBOOK GOAL:
Show why informal "looks good to me" testing fails at scale, introduce structured RAG evaluation metrics, and set up LangSmith tracing to observe and debug the full pipeline.

---

STRUCTURE TO FOLLOW:

1. **Opening — The Measurement Problem (Markdown)**
   - "Our RAG system works. How do we KNOW it works?"
   - "Right now we're eyeballing answers. That's fine for 10 questions. It breaks for 1000."
   - "What if we change the chunk size? How do we know if it got better or worse?"
   - "Evaluation gives us numbers. Numbers let us make decisions."

2. **The 3 Things That Can Go Wrong in RAG (Markdown)**
   - Diagram:
     ```
     Question → [Retrieval] → Chunks → [Generation] → Answer
     
     Failure Mode 1: Wrong chunks retrieved (Retrieval failure)
     Failure Mode 2: Right chunks, wrong answer (Generation failure)
     Failure Mode 3: Right answer, not grounded in context (Hallucination)
     ```
   - Each needs its own metric.

3. **Build an Evaluation Dataset (Code + Markdown)**
   - Create a ground truth dataset manually: 10 question-answer pairs, each with the expected source
     ```python
     eval_dataset = [
         {
             "question": "What is the name of Cooper's daughter?",
             "expected_answer": "Murph (Murphy)",
             "expected_source": "interstellar.txt",
             "relevant_chunks_contain": ["Murph", "Murphy"]
         },
         ...
     ]
     ```
   - Markdown: "This is your test suite. In production, you'd have hundreds of these. Build it before changing anything."
   - Create 2 per story (10 total)

4. **Metric 1: Retrieval Precision (Code + Markdown)**
   - Question: did we retrieve the right chunks?
   - Measure: for each eval question, check if the expected source appears in top-k results
     ```python
     def retrieval_precision(question, expected_source, index, doc_store, top_k=3):
         results = retrieve(question, index, doc_store, top_k)
         retrieved_sources = [r["metadata"]["source"] for r in results]
         return 1 if expected_source in retrieved_sources else 0
     ```
   - Run across all 10 eval questions, print precision score: X/10
   - Markdown: "If precision is low, fix retrieval (chunk size, embedding model, metadata). Don't touch the LLM yet."

5. **Metric 2: Answer Faithfulness (Code + Markdown)**
   - Question: is the answer actually based on the retrieved context, or did the LLM make things up?
   - Approach: use the LLM itself as a judge
     ```python
     def check_faithfulness(answer, context):
         prompt = f"""Given this context:
     {context}
     
     And this answer:
     {answer}
     
     Does the answer contain ONLY information that is present in the context?
     Answer with just: FAITHFUL or UNFAITHFUL, then a one-line reason."""
         
         return call_llm(prompt)
     ```
   - Run on 5 answers, show results
   - Markdown: "LLM-as-judge is imperfect but scalable. For critical systems, add human review on top."

6. **Metric 3: Answer Correctness (Code + Markdown)**
   - Question: is the answer actually right?
   - Compare against ground truth using LLM-as-judge:
     ```python
     def check_correctness(answer, expected_answer):
         prompt = f"""Expected answer: {expected_answer}
     Actual answer: {answer}
     
     Is the actual answer correct and equivalent to the expected answer?
     Answer: CORRECT, PARTIALLY_CORRECT, or INCORRECT. Then one-line reason."""
         
         return call_llm(prompt)
     ```
   - Run on all 10 eval questions
   - Print a summary:
     ```
     Correct:          7/10
     Partially correct: 2/10
     Incorrect:         1/10
     ```

7. **Build a Regression Test Runner (Code + Markdown)**
   - Combine all metrics into one function:
     ```python
     def run_evaluation(eval_dataset, index, doc_store):
         results = []
         for item in eval_dataset:
             retrieved = retrieve(item["question"], index, doc_store)
             answer_result = rag_answer(item["question"], index, doc_store)
             
             precision = retrieval_precision(...)
             faithfulness = check_faithfulness(...)
             correctness = check_correctness(...)
             
             results.append({
                 "question": item["question"],
                 "precision": precision,
                 "faithfulness": faithfulness,
                 "correctness": correctness
             })
         
         return results
     ```
   - Print results as a clean table
   - Markdown: "Run this before and after every change. If the numbers go down, revert."

8. **Set Up LangSmith (Code + Markdown)**
   - What is LangSmith?
     - Observability platform for LangChain applications
     - Traces every step of your pipeline: which chunks were retrieved, what prompt was sent, what the LLM returned, how long each step took
     - Think: Datadog for RAG
   - Setup:
     ```python
     import os
     os.environ["LANGCHAIN_TRACING_V2"] = "true"
     os.environ["LANGCHAIN_API_KEY"] = "your_key_here"
     os.environ["LANGCHAIN_PROJECT"] = "storyverse-ai"
     ```
   - Run the LangChain RetrievalQA chain from Notebook 08
   - Show: trace appears in LangSmith dashboard
   - Walk through what a trace shows:
     - Input question
     - Retriever call + results
     - Prompt sent to LLM
     - LLM output
     - Latency at each step

9. **Debug a Failure With LangSmith (Markdown)**
   - Walk through a hypothetical bad answer:
     - "The model said X but the correct answer is Y"
     - Open LangSmith trace
     - Check retrieved chunks: was the right chunk even retrieved?
     - If no: retrieval problem → fix embedding/chunking
     - If yes: generation problem → fix prompt
   - Markdown: "This is how you debug RAG in production. Not by printing. By tracing."

10. **What to Measure in Production (Markdown)**
    - Summary table:
      ```
      Metric                | What It Catches            | How to Measure
      ──────────────────────────────────────────────────────────────────
      Retrieval Precision   | Wrong chunks               | Source match in top-k
      Answer Faithfulness   | Hallucination              | LLM-as-judge on context
      Answer Correctness    | Wrong answers              | LLM-as-judge vs ground truth
      Latency               | Slow responses             | time.time() / LangSmith
      Token Cost            | Expensive context          | response.usage.total_tokens
      ```
    - "Start with Precision and Correctness. Add Faithfulness when hallucination is a concern."

11. **Series Wrap-Up (Markdown)**
    - What we built, notebook by notebook:
      - 00: Why RAG exists
      - 01-02: Manual context injection
      - 03: Why it fails at scale
      - 04-05: Embeddings and vector search
      - 06: Chunking strategies
      - 07: Retrieval pipeline
      - 08: Complete RAG
      - 09: Production problems
      - 10: Evaluation and observability
    - What's next (teaser for advanced series):
      - Conversational RAG with memory
      - Agentic RAG with tool use
      - Multi-modal RAG
    - "You now understand RAG from the inside out. Most people using LangChain don't."

12. **Exercises**
    - Add 5 more eval questions and re-run the evaluator
    - Deliberately break your chunking (very small chunks) and measure how precision drops
    - Try changing the embedding model — does correctness improve?

---

OUTPUT FORMAT:
Single markdown file using `# %% [markdown]` and `# %%` cell markers.
```

---

*End of all prompts. 10 notebooks. Run each prompt independently to generate the full series.*
