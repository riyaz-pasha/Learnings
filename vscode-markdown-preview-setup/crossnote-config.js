/**
 * Crossnote / Markdown Preview Enhanced Configuration File
 * Location: ~/.local/state/crossnote/config.js
 * Backup / Repo Path: vscode-markdown-preview-setup/crossnote-config.js
 *
 * This file is evaluated by Crossnote (the engine powering Markdown Preview Enhanced)
 * when initializing mathematical renderers (KaTeX, MathJax) and diagram renderers (Mermaid).
 */

({
  /**
   * KaTeX Configuration
   * Used for fast inline and block LaTeX math rendering.
   */
  katexConfig: {
    "macros": {}
  },
  
  /**
   * MathJax Configuration
   * Alternative math rendering engine fallback.
   */
  mathjaxConfig: {
    "tex": {},
    "options": {
      "enableEnrichment": false
    },
    "loader": {}
  },

  /**
   * Mermaid Theme Selection:
   * Setting this to "redux-dark-color" instructs Mermaid's sequence diagram renderer
   * to activate palette cycling using `bkgColorArray` and `borderColorArray`.
   * Under standard "default" or "dark" themes, Mermaid renders every sequence actor
   * with a single monolithic background. Under "redux-dark-color", Mermaid natively
   * assigns distinct matching colors to both top and bottom participant boxes.
   */
  mermaidTheme: "redux-dark-color",
  
  /**
   * Mermaid Runtime Configuration:
   * Passed directly to `mermaid.initialize(config)` on preview initialization.
   */
  mermaidConfig: {
    // Prevent automatic execution until DOM is ready
    "startOnLoad": false,

    // NOTE: do not set `theme` here. MPE overwrites `MERMAID_CONFIG.theme`
    // with the top-level `mermaidTheme` value above before initializing.

    /**
     * Theme Variables:
     * Defines the 20-color Super Mermaid cyclic palette for sequence diagrams
     * and diagrams that consume theme palette arrays.
     *
     * - `bkgColorArray`: Rich tint fills (22% opacity) tuned for the dark
     *   #161b22 preview background.
     * - `borderColorArray`: High-contrast, vibrant stroke colors for participant borders.
     *
     * The scalar values below keep Mermaid's own text metrics and the colors
     * it paints outside the CSS palette in sync with the stylesheet. In
     * particular `fontFamily` / `fontSize` are set here (not in CSS) because
     * Mermaid measures labels with them before generating the SVG; overriding
     * them in CSS would make measurement and rendering disagree.
     */
    "themeVariables": {
      "darkMode": true,
      "fontFamily": "-apple-system, BlinkMacSystemFont, \"Segoe UI\", \"Noto Sans\", Helvetica, Arial, sans-serif",
      "fontSize": "14px",
      "lineColor": "#94A3B8",
      "primaryTextColor": "#E6EDF3",
      "nodeTextColor": "#E6EDF3",
      "edgeLabelBackground": "#1C2128",
      "actorLineColor": "#475569",
      "actorTextColor": "#E6EDF3",
      "noteBkgColor": "rgba(245, 158, 11, 0.22)",
      "noteTextColor": "#E6EDF3",
      "noteBorderColor": "#F59E0B",
      "activationBkgColor": "rgba(16, 185, 129, 0.22)",
      "activationBorderColor": "#10B981",
      "bkgColorArray": [
        "rgba(59, 130, 246, 0.22)",  // 1:  Blue
        "rgba(16, 185, 129, 0.22)",  // 2:  Emerald
        "rgba(245, 158, 11, 0.22)",  // 3:  Amber
        "rgba(139, 92, 246, 0.22)",  // 4:  Purple
        "rgba(244, 63, 94, 0.22)",   // 5:  Rose
        "rgba(6, 182, 212, 0.22)",   // 6:  Cyan
        "rgba(132, 204, 22, 0.22)",  // 7:  Lime
        "rgba(99, 102, 241, 0.22)",  // 8:  Indigo
        "rgba(217, 70, 239, 0.22)",  // 9:  Fuchsia
        "rgba(20, 184, 166, 0.22)",  // 10: Teal
        "rgba(249, 115, 22, 0.22)",  // 11: Orange
        "rgba(168, 85, 247, 0.22)",  // 12: Violet
        "rgba(14, 165, 233, 0.22)",  // 13: Sky
        "rgba(234, 179, 8, 0.22)",   // 14: Yellow
        "rgba(239, 68, 68, 0.22)",   // 15: Red
        "rgba(100, 116, 139, 0.22)", // 16: Slate
        "rgba(5, 150, 105, 0.22)",   // 17: Mint
        "rgba(251, 113, 133, 0.22)", // 18: Coral
        "rgba(217, 119, 6, 0.22)",   // 19: Gold
        "rgba(192, 132, 252, 0.22)"  // 20: Lavender
      ],
      "borderColorArray": [
        "#3B82F6", // 1:  Blue
        "#10B981", // 2:  Emerald
        "#F59E0B", // 3:  Amber
        "#8B5CF6", // 4:  Purple
        "#F43F5E", // 5:  Rose
        "#06B6D4", // 6:  Cyan
        "#84CC16", // 7:  Lime
        "#6366F1", // 8:  Indigo
        "#D946EF", // 9:  Fuchsia
        "#14B8A6", // 10: Teal
        "#F97316", // 11: Orange
        "#A855F7", // 12: Violet
        "#0EA5E9", // 13: Sky
        "#EAB308", // 14: Yellow
        "#EF4444", // 15: Red
        "#94A3B8", // 16: Slate
        "#059669", // 17: Mint
        "#FB7185", // 18: Coral
        "#D97706", // 19: Gold
        "#C084FC"  // 20: Lavender
      ]
    }
  },
})
