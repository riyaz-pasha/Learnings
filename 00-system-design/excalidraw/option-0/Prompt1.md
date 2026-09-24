SECTION 1 — TITLE & OVERVIEW
Place this section starting at y=0.

THINK FIRST (write in chat, do not draw yet):
  — Write a 2–3 paragraph description of what [SYSTEM NAME] does,
    why it is hard to build, and who uses it
  — List 4–5 key challenges that make this system non-trivial
  — State the 5 most important numbers for this system:
    DAU, peak QPS, storage estimate, availability target, latency target

When I say "draw it", draw the following:

LEFT COLUMN — DIAGRAM:
  Large title text: "System Design: [SYSTEM NAME]" (48px bold #F8F8F2)
  Below title, a 3-box overview:
    [Producers] ──→ [SYSTEM NAME Platform] ──→ [Consumers]
    Label each arrow with approximate daily volume
  Below overview, draw 5 "Key Numbers" boxes in a horizontal row:
    Each box: width=200, height=80
    Large number (28px bold) on top line
    Small label (12px) below
    Background alternates #161B22 / #0D1117

RIGHT COLUMN — TEXT:
  Draw an explanation panel (background #44475A) containing:
  Heading: "What This System Does" (14px bold)
  Body: the 2–3 paragraphs you wrote above (13px regular)
  Heading: "Key Challenges" (14px bold)
  Body: the 4–5 challenges as a written list
