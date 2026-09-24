SECTION 4 — BASELINE DESIGN
Place this section 80px below the bottom edge of Section 3.

THINK FIRST (write in chat, do not draw yet):
  — Design the simplest possible architecture. Max 6 components.
  — Explain why it works at low scale (what's elegant about the simplicity)
  — State the capacity ceiling precisely: "breaks above ~X req/s because..."
  — Identify what will fail FIRST and why

When I say "draw it", draw the following:

LEFT COLUMN — DIAGRAM:
  Zone labeled "Baseline Design — V1":
  Draw the simple architecture with max 6 components
  Number every arrow: ① ② ③ ...
  Below the main diagram, draw two side-by-side callout boxes:
    Green box (#3FB950 at 20% opacity, border #3FB950):
      Header: "✅ Works Well Because..."
      Text: what's good about this simple design
    Red box (#FF7B72 at 20% opacity, border #FF7B72):
      Header: "⚠️ Breaks Above ~X req/s Because..."
      Text: the specific failure mode

RIGHT COLUMN — TEXT:
  Heading: "Why Start Simple" (14px bold)
  Paragraph on the value of establishing a baseline in an interview.
  Heading: "The Failure Cascade"
  Paragraph tracing exactly what happens when traffic exceeds the ceiling —
  which component fails first, what it causes downstream.
