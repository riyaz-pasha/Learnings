# VS Code Markdown Preview — dark theme + Nerd Font setup

Documents the setup applied to get a custom dark theme rendering correctly in
VS Code's Markdown Preview Enhanced, plus a ligature-enabled Nerd Font across
the editor/terminal. Re-run `./install.sh` on a fresh machine instead of
redoing this from scratch.

## Files in this folder

| File | Purpose |
|---|---|
| `install.sh` | Idempotent installer — merges `settings.snippet.json` into VS Code's `settings.json`, copies canonical theme to Crossnote's `style.less`, and backs up/installs `config.js` and `head.html`. |
| `settings.snippet.json` | The VS Code User settings this setup depends on. |
| `crossnote-style.less` | Byte-identical mirror of the canonical theme, kept in this folder so the whole setup is browsable in one place. |
| `crossnote-config.js` | Mermaid configuration for Crossnote (`mermaidTheme: "redux-dark-color"`, 20-color `themeVariables`, font metrics, curved connectors `curve: "basis"`, and spacing). |
| `head.html` | Lightweight deterministic post-render script injected into the preview webview for automatic semantic class tagging and shape classification. |
| `DIAGRAMMING_GUIDE.md` | Architectural cheat sheet with standard semantic classes, shape heuristics, connection styles, and templates. |

The canonical theme source lives at `../markdown-theme-2.css` (repo root) —
edit colors/spacing there, then copy the result over `crossnote-style.less`.
`install.sh` installs the canonical file and warns if the mirror has drifted.

## Semantic & Consistent Architecture Diagramming System
Upgraded from random rainbow cycling to an intelligent, multi-tier architectural diagramming system:
- **3-Tier Semantic Classification**:
  - **Tier 1 (Explicit Classes)**: `:::db`, `:::cache`, `:::queue`, `:::service`, `:::client`, `:::gateway`, `:::alert`, `:::success`, `:::external`.
  - **Tier 2 (Shape Heuristics)**: Functional shapes automatically styled with their architectural roles (Cylinder `[(...)]` $\rightarrow$ Emerald DB, Diamond `{...}` $\rightarrow$ Amber Decision, Hexagon `{{...}}` $\rightarrow$ 3rd-Party Slate, Subroutine `[[...]]` $\rightarrow$ Queue Purple).
  - **Tier 3 (Keyword Detection)**: Inbuilt regex classifier in `head.html` scans label text ("Postgres", "Redis", "Kafka", "Gateway", "DLQ") and tags semantic roles with zero author effort.
  - **Tier 4 (Fallback)**: General unannotated nodes continue to receive the vibrant 20-color Super Mermaid palette.
- **Card-Style Architectural Boundaries**: Subgraphs / VPCs styled with dashed architectural boundaries and uppercase tracked micro-badge headers.
- **Fluid Curved Connectors**: Flowcharts use `curve: "basis"` in `config.js` to eliminate harsh stair-step lines.
- **Smarter Sequence Diagrams**: Clear visual distinction between synchronous request calls and muted dashed return arrows (`-->>`), plus sleek translucent containers for `alt`, `opt`, and `loop` fragments.
- **State Diagrams (`stateDiagram-v2`)**: Styled with clean state capsules, green start states, and red end states.
- **Zero Text Clipping & Zero Shadows**: ForeignObject overflow reset and no blurry drop shadows.


## Why two theme files?

- `markdown-theme-2.css` (repo root) — **canonical source. Edit this one.**
- `crossnote-style.less` (this folder) — a byte-identical mirror kept so the
  whole setup is browsable in one folder. `install.sh` installs the canonical
  file and warns if the mirror has drifted.

Both files already contain the MPE-specific work: selectors are scoped under
`.markdown-preview.markdown-preview` (Markdown Preview Enhanced renders content
inside a div with that class, not `<body>` directly), and several properties
carry `!important` to beat the extension's own base CSS
(`crossnote/styles/style-template.css`, `crossnote/styles/preview.css`).

Note: `markdown.styles` in `settings.snippet.json` points at the same file for
VS Code's **built-in** preview, but those selectors do not match that preview's
DOM (`body.vscode-body > div.markdown-body`), so the theme effectively only
applies to Markdown Preview Enhanced. The built-in preview will still get its
own default styling.

After editing `markdown-theme-2.css`:

```bash
cp ../markdown-theme-2.css crossnote-style.less   # keep the mirror in sync
./install.sh
```

## Where things actually live (none of this is in the repo by default)

| What | Path | Why it's not just in VS Code's settings UI |
|---|---|---|
| VS Code user settings | `~/Library/Application Support/Code/User/settings.json` | Standard location, machine-specific. |
| Markdown Preview Enhanced's global CSS | `~/.local/state/crossnote/style.less` | This is **not** `~/.mume` (older docs/tutorials say that) — current versions of the extension use `~/.local/state/crossnote` on Mac/Linux (or `$XDG_CONFIG_HOME/crossnote` if that env var is set). It's managed by the extension itself, not VS Code — there's no `markdown-preview-enhanced.customCss` setting; that was a dead end. |

`install.sh` writes to both.

## Required extension + fonts

- Extension: **Markdown Preview Enhanced** (`shd101wyy.markdown-preview-enhanced`)
  — install via VS Code, or `code --install-extension shd101wyy.markdown-preview-enhanced`.
- Prose Font: **PT Serif** — preinstalled on macOS (`/System/Library/Fonts/Supplemental/PTSerif.ttc`) with Google Fonts web fallback in `style.less`.
- Code Font: **FiraCode Nerd Font Mono** — already tracked in this repo's `Brewfile`
  (`cask "font-fira-code-nerd-font"`), so `brew bundle` picks it up. Used across VS Code editor, terminal, and preview code blocks.

## Settings applied (`settings.snippet.json`)

```jsonc
"editor.fontFamily": "'FiraCode Nerd Font Mono', Menlo, Monaco, 'Courier New', monospace",
"editor.fontLigatures": true,
"terminal.integrated.fontFamily": "'FiraCode Nerd Font Mono'",
"debug.console.fontFamily": "'FiraCode Nerd Font Mono'",

"markdown.styles": ["/Users/riyaz/Personal/Learning/markdown-theme-2.css"],
"markdown.preview.markEditorSelection": true,

"markdown-preview-enhanced.previewTheme": "none.css",
"markdown-preview-enhanced.mermaidTheme": "dark",
"markdown-preview-enhanced.codeBlockTheme": "github-dark.css"
```

- `markdown.styles` targets VS Code's **built-in** preview (`Cmd+Shift+V`), but
  the file's `.markdown-preview.markdown-preview` selectors do not match that
  preview's DOM, so it has no visible effect there. The theme is applied by
  MPE via `style.less` instead.
- The `markdown-preview-enhanced.*` keys theme the **richer** preview (Mermaid,
  math, TOC, etc. — `Cmd+Shift+P` → "Markdown Preview Enhanced: Open Preview
  to the Side").
- `markdown-preview-enhanced.mermaidTheme` is overridden by the top-level
  `mermaidTheme` in `config.js` (MPE merges VS Code settings first, then the
  global config file). Change the theme in `config.js`, not here.
- `editor.fontFamily`/`fontLigatures` are for the actual code editor and are
  unrelated to the preview theming — Monaco's ligature rendering is solid and
  needed no workarounds (unlike Gotcha #4 below).

## Gotchas hit while building this (context for future edits)

1. **`markdown-preview-enhanced.customCss` is not a real setting.** It's easy
   to assume this extension follows the same pattern as `markdown.styles`, but
   it doesn't. Custom CSS is a file (`style.less`) in the extension's own
   config dir, edited via the command "Markdown Preview Enhanced: Customize
   CSS (Global)" — not a `settings.json` key.

2. **Full-bleed background required `previewTheme: "none.css"` + an explicit
   `html, body` rule.** The default preview theme (`github-light.css`) hard-codes
   `html body { background-color: #fff }` on the page itself, outside our
   styled content div — so only the inner content box went dark, leaving a
   white page around it. `none.css` ships empty, avoiding the fight entirely.

3. **A stale webview size can leave a blank gap on one side of the preview
   pane** even after the CSS is correct — a `Developer: Reload Window` forces
   the webview to resync to the pane's actual width. Worth trying first if
   width changes don't seem to apply.

4. **Missing/boxed glyphs in code blocks (`+`, `%`, `=`, `||`...) were not a
   font problem.** `codeBlockTheme` defaults to `"auto.css"` (a virtual value,
   not a real file) which guesses light vs. dark — it guessed light. The light
   Prism theme paints a translucent *white* background pill behind operator
   tokens (meant to help them pop on a white page); on our dark page that pill
   just reads as a blank box. Forcing an explicit dark theme
   (`codeBlockTheme: "github-dark.css"`) fixed it — no ligature/font change
   needed for this particular symptom, even though it looked like one at first.
   The `font-variant-ligatures: none` rule in `crossnote-style.less` is a
   separate, real ligature-tofu issue specific to the MPE webview's font
   rendering — it does **not** apply to the main editor, where FiraCode's
   ligatures render correctly natively (Monaco is a more mature rendering path
   than the preview's webview).

5. **Mermaid diagrams styled with Super Mermaid Colorful Theme**:
   Integrated the auto-colored aesthetic from the [Super Mermaid extension](https://marketplace.visualstudio.com/items?itemName=mark-ku.super-mermaid).
   It applies a **20-color** cyclic palette to flowcharts (nodes *and* clusters),
   uses `redux-dark-color` + `themeVariables.bkgColorArray` / `borderColorArray`
   for sequence-diagram participants, removes drop shadows, scales naturally with
   preview zoom, and fixes text clipping. The theme is **dark-only**; the page
   background and prose colors are hard-coded dark, so there is no light-mode
   variant. ER and class diagrams are not touched by the flowchart palette
   rules; ER row striping is restored with explicit overrides because the
   `redux-dark-color` theme otherwise paints every path inside an entity.

   Two Mermaid-specific traps worth remembering:
   - Mermaid measures label text at `themeVariables.fontFamily` / `fontSize`
     *before* generating the SVG. Overriding those in CSS makes the measured
     size disagree with the rendered size, causing clipped/overlapping labels.
     Set them in `config.js` instead of the stylesheet.
   - Sequence lifeline ids (`line#actor0`, …) use a module-global counter that
     is not reset between diagrams, so per-id coloring drifts on a page with
     more than one sequence diagram. Use `actorLineColor` instead.

## Re-applying after a theme edit

```bash
# 1. Edit ../markdown-theme-2.css
# 2. cp ../markdown-theme-2.css crossnote-style.less   # keep the mirror identical
# 3. Re-run:
./install.sh
# 4. In VS Code: Cmd+Shift+P -> Developer: Reload Window
```
