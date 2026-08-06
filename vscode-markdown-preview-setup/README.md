# VS Code Markdown Preview — dark theme + Nerd Font setup

Documents the setup applied to get a custom dark theme rendering correctly in
VS Code's Markdown Preview Enhanced, plus a ligature-enabled Nerd Font across
the editor/terminal. Re-run `./install.sh` on a fresh machine instead of
redoing this from scratch.

## Files in this folder

| File | Purpose |
|---|---|
| `install.sh` | Idempotent installer — merges `settings.snippet.json` into VS Code's `settings.json` and copies `crossnote-style.less` into place. |
| `settings.snippet.json` | The VS Code User settings this setup depends on. |
| `crossnote-style.less` | The **actually applied** stylesheet for Markdown Preview Enhanced (checked in as a backup/reference — see "Why two theme files?" below). |

The canonical theme source lives at `../markdown-theme-2.css` (repo root) —
edit colors/spacing there.

## Why two theme files?

- `markdown-theme-2.css` (repo root) — the theme in plain CSS, as if styling
  `<body>`, `<h1>`, `<code>`, etc. directly. **Edit this one.**
- `crossnote-style.less` (this folder) — the same rules, but transformed to
  actually work inside Markdown Preview Enhanced. It's not just a copy:
  - Every selector is scoped under `.markdown-preview.markdown-preview`
    (Markdown Preview Enhanced renders content inside a div with that class,
    not `<body>` directly — a bare `body { ... }` rule never matches anything
    in the preview).
  - Several properties carry `!important` because the extension's own base
    CSS (`crossnote/styles/style-template.css`,
    `crossnote/styles/preview.css`) targets the same elements with a more
    specific selector chain (e.g. `.preview-container .crossnote[data-for=preview]`)
    and otherwise wins the cascade even though it loads first.
  - It includes a font-ligature-disabling rule for code blocks that
    `markdown-theme-2.css` doesn't need (see Gotcha #4).

If you change `markdown-theme-2.css`, manually port the change into
`crossnote-style.less` (keep the selector scoping / `!important`s), then
re-run `./install.sh`.

## Where things actually live (none of this is in the repo by default)

| What | Path | Why it's not just in VS Code's settings UI |
|---|---|---|
| VS Code user settings | `~/Library/Application Support/Code/User/settings.json` | Standard location, machine-specific. |
| Markdown Preview Enhanced's global CSS | `~/.local/state/crossnote/style.less` | This is **not** `~/.mume` (older docs/tutorials say that) — current versions of the extension use `~/.local/state/crossnote` on Mac/Linux (or `$XDG_CONFIG_HOME/crossnote` if that env var is set). It's managed by the extension itself, not VS Code — there's no `markdown-preview-enhanced.customCss` setting; that was a dead end. |

`install.sh` writes to both.

## Required extension + font

- Extension: **Markdown Preview Enhanced** (`shd101wyy.markdown-preview-enhanced`)
  — install via VS Code, or `code --install-extension shd101wyy.markdown-preview-enhanced`.
- Font: **FiraCode Nerd Font Mono** — already tracked in this repo's `Brewfile`
  (`cask "font-fira-code-nerd-font"`), so `brew bundle` picks it up.

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

- `markdown.styles` themes VS Code's **built-in** preview (`Cmd+Shift+V`) — no
  extension required, works standalone.
- The `markdown-preview-enhanced.*` keys theme the **richer** preview (Mermaid,
  math, TOC, etc. — `Cmd+Shift+P` → "Markdown Preview Enhanced: Open Preview
  to the Side").
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

5. **Mermaid diagrams default to a light palette** (dark text assumed to sit
   on a white node fill) — invisible on a dark page. Fixed via
   `mermaidTheme: "dark"`.

## Re-applying after a theme edit

```bash
# 1. Edit ../markdown-theme-2.css
# 2. Port the change into crossnote-style.less (keep scoping/!important)
# 3. Re-run:
./install.sh
# 4. In VS Code: Cmd+Shift+P -> Developer: Reload Window
```
