---
jupyter:
  jupytext:
    formats: ipynb,md
    text_representation:
      extension: .md
      format_name: markdown
      format_version: '1.3'
      jupytext_version: 1.19.5
  kernelspec:
    display_name: Python 3 (rag-tutorial-1)
    language: python
    name: python3
---

# 03 — The Context Window Problem

Notebook 02 ended with a claim: "loading everything, every time, doesn't scale." Claims are cheap. Let's actually measure it.

We're going to ask the exact same question against 5 documents, then 25, then 55, and watch what happens to cost, speed, and — this is the important one — whether the answer is even still correct.


```python
import os
import time
from dotenv import load_dotenv
from groq import Groq

load_dotenv()

client = Groq(api_key=os.environ["GROQ_API_KEY"])
MODEL = "openai/gpt-oss-20b"

DATA_DIR = os.path.join("..", "data")
STORIES_DIR = os.path.join(DATA_DIR, "stories")


def load_documents(folder_path: str) -> list[dict]:
    docs = []
    for filename in sorted(os.listdir(folder_path)):
        if filename.endswith(".txt"):
            with open(os.path.join(folder_path, filename)) as f:
                docs.append({"filename": filename, "content": f.read()})
    return docs


def combine_documents(docs: list[dict]) -> str:
    return "\n\n".join(f"[{d['filename']}]\n{d['content']}" for d in docs)


def ask_with_context(question: str, context: str) -> str:
    system_prompt = f"""You are StoryVerse AI. Answer using ONLY the context below.

Context:
{context}"""
    response = client.chat.completions.create(
        model=MODEL,
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": question},
        ],
        temperature=0.2,
        reasoning_effort="low",
    )
    return response.choices[0].message.content

```

## Padding out the library

Our 5 real StoryVerse stories aren't enough to feel the wall. Let's generate 50 filler "movies" — nonsense plots, but the same size and shape as a real story summary — so we can simulate a bigger catalog without writing 50 real stories by hand.


```python
import random

random.seed(7)

PLACES = ["Mumbai", "a space station", "a medieval kingdom", "Tokyo", "a desert outpost",
          "an underwater city", "a Martian colony", "London", "a jungle temple", "a frozen tundra"]
GOALS = ["stop a war", "find a lost sibling", "steal a stolen relic", "escape a collapsing world",
         "expose a conspiracy", "break a family curse", "win a forbidden tournament",
         "save a dying planet", "outrun a bounty hunter", "solve their own murder"]

def generate_filler_movies(n: int) -> dict:
    filler = {}
    for i in range(n):
        place = random.choice(PLACES)
        goal = random.choice(GOALS)
        name = f"Protagonist{i}"
        filler[f"filler_{i:03}.txt"] = (
            f"Movie {i}: Set in {place}, {name} must {goal}. Along the way, "
            f"{name} gathers allies, faces a betrayal, and confronts the antagonist "
            f"in a final act that changes {place} forever."
        )
    return filler


real_docs = load_documents(STORIES_DIR)
filler_docs = generate_filler_movies(50)

print(f"Real stories: {len(real_docs)}, filler stories available: {len(filler_docs)}")

```

## Round 1 — just the real stories

Start clean: only our 5 real StoryVerse stories, nothing else. The question only *Interstellar* can answer.


```python
QUESTION = "Who is Cooper's daughter in Interstellar, and how does the story resolve?"


def run_experiment(question: str, docs: list[dict], label: str):
    context = combine_documents(docs)
    start = time.time()
    answer = ask_with_context(question, context)
    elapsed = time.time() - start

    print(f"--- {label} ---")
    print(f"Documents:      {len(docs)}")
    print(f"Context length: {len(context):,} chars (~{len(context)//4:,} tokens)")
    print(f"Response time:  {elapsed:.2f}s")
    print(f"Answer:         {answer}")
    print()
    return {"label": label, "num_docs": len(docs), "chars": len(context), "time": elapsed, "answer": answer}


result_5 = run_experiment(QUESTION, real_docs, "5 documents")

```

That should look fast and clean — 5 documents is nothing. Now let's make StoryVerse AI's library grow, without changing the question at all.



## Round 2 — now bury it a little

Same question, word for word. This time it's sitting under 20 filler movies about places and characters that have nothing to do with Interstellar.


```python
filler_20 = [{"filename": k, "content": v} for k, v in list(filler_docs.items())[:20]]
docs_25 = real_docs + filler_20

result_25 = run_experiment(QUESTION, docs_25, "25 documents")

```

Notice the context is now mostly noise the model has to wade through to find the one relevant story. It's the same needle, in a much bigger haystack.



## Round 3 — bury it completely

Same question a third time. Now it's the real 5, plus every one of the 50 filler movies.


```python
filler_50 = [{"filename": k, "content": v} for k, v in filler_docs.items()]
docs_55 = real_docs + filler_50

result_55 = run_experiment(QUESTION, docs_55, "55 documents")

```

Compare the three answers above with your own eyes. Don't be surprised if all three stay fully correct — 55 documents is still only a few thousand tokens, nowhere near the size where a capable model actually buckles. That's a real, useful data point, not a failed demo: it tells you *this specific risk* needs real scale (thousands of documents, or a weaker model) before it reliably shows up as a wrong answer. The risk is still there, waiting for that scale, under two names:

- **Lost in the middle** — models pay more attention to the very start and the very end of the context they're given. Whatever's buried in the middle gets relatively less attention, even if it's the answer. At 55 short documents there's barely a "middle" to get lost in; at 5,000 there very much is.
- **Attention dilution** — the more tokens you throw at the model, the more its attention has to spread out to cover all of them. Right now the relevant sentence only has to compete with fifty others. In a real catalog, it competes with fifty thousand.



## The bill for all that noise

Speed and quality are only two-thirds of the story — someone's paying for every one of those extra tokens. Here's the money side, at Groq's rough published pricing for open models (illustrative — check current pricing yourself).


```python
PRICE_PER_MILLION_TOKENS = 0.10  # illustrative, not a live quote

print(f"{'Docs':>6} | {'Context chars':>14} | {'~Tokens':>10} | {'Cost/query':>12} | {'Cost/1M queries':>17}")
for label, docs in [("5", real_docs), ("25", docs_25), ("55", docs_55)]:
    context = combine_documents(docs)
    tokens = len(context) // 4
    cost_per_query = tokens / 1_000_000 * PRICE_PER_MILLION_TOKENS
    cost_per_million = cost_per_query * 1_000_000
    print(f"{label:>6} | {len(context):>14,} | {tokens:>10,} | ${cost_per_query:>11.5f} | ${cost_per_million:>16,.0f}")

```

This isn't hypothetical — real apps run millions of queries a day. The jump from 5 to 55 documents alone multiplies your token bill by roughly the same factor, for a question that only ever needed a fifth of one document.



## What about speed?

Cost isn't even the part users feel first — speed is. Same three results, timed, plotted as a plain-text bar so you don't need a charting library for something this simple. Fair warning: a single API call's timing is noisy — network hops and server queueing can easily dwarf the actual difference a few thousand extra tokens make, so don't be surprised if the middle bar isn't neatly between the other two. That noise is itself the point: at this modest scale, latency is dominated by infrastructure, not by document count. It stops being noise and starts being a real, visible cost once you're sending sustained traffic at real scale.


```python
def ascii_bar(label: str, value: float, max_val: float, width: int = 40) -> None:
    filled = int((value / max_val) * width) if max_val else 0
    bar = "█" * filled + "░" * (width - filled)
    print(f"{label:16} | {bar} | {value:.2f}s")


all_results = [result_5, result_25, result_55]
max_time = max(r["time"] for r in all_results)

for r in all_results:
    ascii_bar(r["label"], r["time"], max_time)

```

Users start noticing lag past roughly 2 seconds — if your 55-document run crossed that line, that's the real cost showing up despite the noise. If it didn't, the token math from the previous cell is still the honest, guaranteed-to-scale number: it doesn't depend on any one API call being lucky or unlucky.



## What actually settles it: accuracy, not speed

Cost is guaranteed math. Speed is noisy at this scale. The one thing left worth checking directly: does all that extra, irrelevant context make the *answer* itself worse, not just slower or pricier? One more run. Version A: hand the model *only* the Interstellar file. Version B: hand it all 55, same as Round 3.


```python
interstellar_only = [d for d in real_docs if d["filename"] == "interstellar.txt"]

result_only = run_experiment(QUESTION, interstellar_only, "1 document (just Interstellar)")

```

Compare `result_only` to `result_55` above. Cheaper, definitely — a fraction of the tokens, guaranteed. Faster isn't guaranteed on any single noisy call, as you've now seen. Accuracy is the interesting one: a capable model at this modest scale will often stay just as correct either way, because it's genuinely not confused by fifty short, clearly-unrelated plots. That's not the demo failing — that's the actual boundary of the risk. This failure mode has a name — **context poisoning** or **retrieval noise** — and it's real: it shows up reliably once the irrelevant documents are *semantically closer* to the question (two different "Dark Knight"s, say — notebook 09 builds exactly that case), or once the sheer document count is large enough to genuinely dilute attention. Fifty generic filler movies about a wormhole question isn't quite that case; a wrong-genre near-miss is.



## What we actually need

We don't need to send everything. We need a way to look at a question and know, cheaply, which one or two documents actually matter — *before* we build the prompt. That means being able to measure **relevance** between a question and a document, automatically.

```
What we have now:
Question -> ALL 55 Documents -> Model -> Answer
                  ^
            Expensive. Slow. Noisy.

What we actually want:
Question -> [Find the relevant ones] -> 1-3 Documents -> Model -> Answer
                        ^
                 Fast. Cheap. Accurate.
```

Measuring "relevance" between a question and a pile of text by *meaning*, not by matching exact words, is what **embeddings** are for — and that's the very next notebook.



## What we learned

**Terms to keep, in plain English:**

| Term | Plain meaning |
|---|---|
| Lost in the middle | Models pay less attention to text buried in the middle of a long context |
| Attention dilution | More tokens means attention is spread thinner across all of them |
| Context poisoning / retrieval noise | Irrelevant context actively making an answer worse, not just slower |
| Latency | How long a response takes to come back |

We didn't just claim "more context is worse" — we measured slower responses, higher token bills, and a real risk of worse answers, all from the same cause: sending everything instead of sending what's relevant.

**Exercises:**

- Run these same three experiments with a question about a *different* real story (e.g. Baahubali) — does the pattern hold?
- Place `interstellar.txt` at the very end of the 55-document context instead of the start — does the answer change?
- At 10,000 queries/day with the 55-document context, what's your estimated monthly token cost using the table above?

