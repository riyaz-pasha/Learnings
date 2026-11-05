---
name: Mermaid Cleanup
description: Convert ChatGPT-rendered Mermaid artifacts in Markdown into clean, validated ```mermaid blocks using the repo's mermaid-cleanup CLI. Use when a Markdown file contains giant `chatgpt-mermaid` CSS lines, broken/invisible diagrams, or leftover inline SVG styling.
---

# Mermaid Cleanup

ChatGPT exports Mermaid diagrams as one enormous inline-CSS line plus concatenated
labels. The diagram structure is lost. This skill rebuilds the blocks with the
repo toolkit at `mermaid-cleanup/`.

## Rules

- Never `read`, `cat`, `grep`, or `sed` the artifact line. It is 5,000–10,000
  characters of CSS; reading it wastes context and reveals nothing useful.
- Use `scan` for labels, diagram type, and nearby prose.
- Rebuild every recovered label. Do not invent, rename, or drop labels.
- Do not finish until `validate` renders every block and `check` reports zero
  artifacts.

## Workflow

Run from the repository root.

```bash
TOOL=mermaid-cleanup/mermaid_cleanup.py
FILE=path/to/chapter.md
WORK=/tmp/mermaid-work/$(basename "$FILE" .md)

python3 "$TOOL" scan "$FILE"
# exit 0. Read: artifact line numbers, kind, labels, participant_prefix, context.

mkdir -p "$WORK"
# Write one file per artifact: $WORK/<artifact-line-number>.mmd
# Body only, no ``` fences.

python3 "$TOOL" validate-blocks "$WORK"
# Fix every FAIL before continuing.

python3 "$TOOL" apply "$FILE" --blocks "$WORK" --validate
python3 "$TOOL" validate "$FILE"
python3 "$TOOL" check "$FILE"
```

`scan` exits 0 even when artifacts exist. `check` exits 1 on errors.

## Rebuilding a block

- **flowchart**: `flowchart TD` for pipelines/layers, `flowchart LR` for
  left-to-right flows. Quote labels with spaces or punctuation:
  `A["Node A: Primary"]`.
- **Partitions**: use subgraphs `subgraph PA["Partition A"] ... end` and dotted
  labeled edges `PA -.->|Network Partition| PB`.
- **Decisions**: `Q{Was A fenced?}` with `-->|Yes|` / `-->|No|`.
- **Wrapped labels**: concatenated text like `Replication and conflicthandling`
  means `Replication and conflict<br/>handling`.
- **sequenceDiagram**: order participants left-to-right using
  `participant_prefix` plus labels; `->>` request, `-->>` response,
  `Note over X:` for notes.
- **stateDiagram-v2 / mindmap / timeline**: follow the recovered label order and
  the surrounding prose.

If a recovered label is a standalone claim, attach it with a dotted edge to the
node it describes rather than creating a fake node.

## Reference

- `mermaid-cleanup/README.md` — full human documentation, command reference,
  artifact anatomy, troubleshooting.
- `mermaid-cleanup/mermaid_cleanup.py` — the CLI (`scan`, `check`, `extract`,
  `apply`, `validate`, `validate-blocks`, `doctor`).
- `mermaid-cleanup/AGENTS.md` — condensed agent notes.
