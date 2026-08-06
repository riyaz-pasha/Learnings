#!/usr/bin/env bash
# Re-applies the VS Code + Markdown Preview Enhanced dark theme setup documented
# in ./README.md. Safe to re-run — merges settings rather than overwriting them.
#
# Usage: ./install.sh

set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

VSCODE_SETTINGS="$HOME/Library/Application Support/Code/User/settings.json"

# Markdown Preview Enhanced's own config dir (holds the *actually applied* style.less).
# It follows $XDG_CONFIG_HOME/crossnote if set, otherwise ~/.local/state/crossnote on
# Mac/Linux. See README.md's "Gotchas" section for why this isn't ~/.mume.
if [ -n "${XDG_CONFIG_HOME:-}" ]; then
  CROSSNOTE_DIR="$XDG_CONFIG_HOME/crossnote"
else
  CROSSNOTE_DIR="$HOME/.local/state/crossnote"
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "error: jq is required (brew install jq)" >&2
  exit 1
fi

if [ ! -f "$VSCODE_SETTINGS" ]; then
  echo "error: VS Code settings.json not found at $VSCODE_SETTINGS" >&2
  echo "       Open VS Code at least once, then re-run this script." >&2
  exit 1
fi

echo "-> Backing up $VSCODE_SETTINGS"
cp "$VSCODE_SETTINGS" "$VSCODE_SETTINGS.bak.$(date +%Y%m%d%H%M%S)"

echo "-> Merging settings.snippet.json into VS Code User settings"
jq -s '.[0] * .[1]' "$VSCODE_SETTINGS" settings.snippet.json > "$VSCODE_SETTINGS.tmp"
mv "$VSCODE_SETTINGS.tmp" "$VSCODE_SETTINGS"

echo "-> Installing crossnote-style.less to $CROSSNOTE_DIR/style.less"
mkdir -p "$CROSSNOTE_DIR"
cp crossnote-style.less "$CROSSNOTE_DIR/style.less"

echo
echo "Done. In VS Code, run:"
echo "  Cmd+Shift+P -> Developer: Reload Window"
echo
echo "Requires the 'Markdown Preview Enhanced' extension"
echo "(shd101wyy.markdown-preview-enhanced) and the FiraCode Nerd Font Mono font"
echo "(brew install --cask font-fira-code-nerd-font — already in this repo's Brewfile)."
