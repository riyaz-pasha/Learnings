# Mermaid Cleanup Agent Notes

When asked to clean or convert ChatGPT-rendered Mermaid artifacts in a Markdown
file, use this folder's CLI. The matching OpenCode skill is
`.opencode/skills/mermaid-cleanup/SKILL.md`.

Do not read the giant artifact lines directly.

```bash
TOOL=mermaid-cleanup/mermaid_cleanup.py

python3 "$TOOL" scan FILE
# Reconstruct one <artifact-line>.mmd per artifact in a work directory.
python3 "$TOOL" validate-blocks WORKDIR
python3 "$TOOL" apply FILE --blocks WORKDIR --validate
python3 "$TOOL" validate FILE
python3 "$TOOL" check FILE
```

Rules:

- Preserve all labels recovered by `scan`; do not invent replacements.
- Use `<br/>` for line-wrapped labels.
- Use subgraphs for partitions and dotted labeled edges for partition links.
- Quote labels containing spaces or punctuation.
- Do not finish until both `validate` and `check` pass.

See `README.md` for the full workflow, reconstruction heuristics, and
troubleshooting.
