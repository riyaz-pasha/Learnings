# rag-tutorial-1

Personal learning repo for Retrieval-Augmented Generation (RAG) — working through the fundamentals (chunking, embeddings, vector stores, retrieval), agentic RAG with LangGraph, and RAG evaluation, mostly as Jupyter notebooks.

## Project structure

| Path | What's in it |
|---|---|
| `course/basics/` | Core RAG basics course — notebooks, sample data, prompt notes |
| `chunking/` | Document parsing and chunking strategies (structural, semantic, topic-drift detection) |
| `notebook/` | RAG-over-PDF experiments, agentic RAG, multi-tool chatbot |
| `Agentic-LanggraphCrash-course/` | LangGraph crash course — basic chatbot → tools → multi-agent dev team |
| `Evaluation/` | RAG evaluation concepts (`0-tutorial/`) and a practical pass (`1-practical/`) |
| `extractions/` | PDF extraction experiments (PyMuPDF/Fitz) |
| `realworld-example/` | Applied example notebooks (procurement use case) |
| `data/` | Sample documents, FAISS index, Chroma vector store |
| `main.py` | Placeholder entry point (not the primary way this repo is used — see notebooks above) |

## Prerequisites

- [mise](https://mise.jdx.dev/) — manages the pinned Python version for this project (`.python-version` / `mise.toml` → Python 3.13)
- [uv](https://docs.astral.sh/uv/) — manages the virtualenv and dependencies (`pyproject.toml` / `uv.lock`)

Install both once, globally:

```bash
brew install mise
mise use -g uv
```

Make sure `mise activate` is wired into your shell (e.g. `eval "$(mise activate zsh)"` in `~/.zshrc`), then restart your terminal.

## Setup

From the repo root:

```bash
# 1. Install the pinned Python version (3.13) for this project
mise install

# 2. Create the .venv and install all dependencies from uv.lock
uv sync
```

This creates `.venv/` with Python 3.13, all runtime dependencies (LangChain, LangGraph, ChromaDB, FAISS, sentence-transformers, docling, ragas, deepeval, etc.), and dev dependencies (`jupytext`).

## Environment variables

Copy `.env` (or create one) with the API keys the notebooks expect:

```bash
GROQ_API_KEY=...
OPENAI_API_KEY=...
TAVILY_API_KEY=...
LANGSMITH_API_KEY=...
```

## Running notebooks

**Option A — VS Code**
1. Open any `.ipynb` file
2. "Select Kernel" (top right) → choose the `.venv` interpreter for this project (Python 3.13.15)

**Option B — Jupyter Lab in the browser**

```bash
uv run jupyter lab
```

### Markdown preview for notebooks

Notebooks under `course/basics/notebooks/` are paired with Jupytext (`.ipynb` ↔ `.md`) so their markdown cells can be edited/previewed as plain Markdown (e.g. with Markdown Preview Enhanced) while staying in sync with the notebook. To pair another notebook the same way:

```bash
uv run jupytext --set-formats ipynb,md --sync path/to/notebook.ipynb
```

Saving either the `.ipynb` or its paired `.md` keeps both in sync (via the "Jupytext Sync" VS Code extension, or by re-running the command above).
