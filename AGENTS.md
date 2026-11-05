# Repository Agent Notes

## ChatGPT Mermaid artifact cleanup

Use the `mermaid-cleanup` skill (`.opencode/skills/mermaid-cleanup/SKILL.md`) or
the toolkit at `mermaid-cleanup/`.

```bash
TOOL=mermaid-cleanup/mermaid_cleanup.py

python3 "$TOOL" scan FILE
# Create one <artifact-line>.mmd per artifact in a work directory.
python3 "$TOOL" validate-blocks WORKDIR
python3 "$TOOL" apply FILE --blocks WORKDIR --validate
python3 "$TOOL" validate FILE
python3 "$TOOL" check FILE
```

Never read or paste the giant artifact line. `scan` returns only the recovered
labels and local context. See `mermaid-cleanup/README.md` for reconstruction
heuristics and troubleshooting.
