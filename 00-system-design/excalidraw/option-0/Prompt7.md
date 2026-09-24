SECTION 7 — DEEP DIVES
Place this section 80px below the bottom edge of Section 6.
This section has THREE subsections, one per FR. Each subsection is tall.

Pick the 3 most important/complex FRs from Section 2.

For FR [N] (repeat this prompt 3 times, once per FR):

THINK FIRST (write in chat, do not draw yet):
  A. User Journey:
     — What does the user do? What do they perceive?
     — What do they experience when it fails?
     — Latency budget per step: T+0ms, T+50ms, etc.
  
  B. Internal Flow:
     — Every service touched in order
     — Every DB operation: specify table + operation + key fields
     — Every cache interaction: specify hit path and miss path separately
     — What happens on failure at each step?
     — Which steps are on the latency-critical path?
  
  C. Data Model:
     — Tables/collections with every field, type, constraint
     — Most important indexes and the query they serve
     — The single most common query written as actual SQL
     — Why this schema over the obvious alternative?

When I say "draw it", draw the following:

LEFT COLUMN — THREE DIAGRAMS stacked vertically:

  DIAGRAM A — Swim Lane (user journey):
    Horizontal lanes, one per actor
    Actors: User | Client App | API Layer | Core Service | Storage | External
    Each step = numbered box inside correct lane
    Happy path arrows: #3FB950 solid
    Failure path arrows: #FF7B72 dashed (at least one failure)
    Time axis labels on left side: T+0ms, T+Xms, T+Yms...
    Lane separator lines: #30363D

  DIAGRAM B — Internal Flow:
    Components as vertical column headers (14px bold, underlined)
    Horizontal arrows left to right, numbered ①②③...
    Solid arrow = sync | Dashed arrow = async | Dotted return = response
    DB annotations below arrow label: "WRITE posts(id, user_id, content)"
    Cache annotations: "CACHE GET feed:{user_id} TTL=5min"
    Critical path highlighted: green (#3FB950) thick arrows, strokeWidth=3
    Latency annotation next to each step: "[~20ms]"

  DIAGRAM C — Data Model:
    Entity boxes with fields listed inside:
      field_name: TYPE [constraint icon]
      🔑 PK | 🔗 FK | ★ INDEX | ◆ UNIQUE
    Relationship lines with cardinality: 1:N, M:N
    Below each entity, a "Hot Query" box (monospace, #D29922 text):
      "SELECT ... WHERE ... ORDER BY ... LIMIT ..."

RIGHT COLUMN — TEXT:
  Heading: "[FR Name] — How It Actually Works"
  Walk through internal flow in plain language, referencing numbered steps.
  
  Heading: "What Can Go Wrong"
  The most likely production failure and exact recovery mechanism.
  
  Heading: "Data Model Decision"
  Why this schema over the simpler alternative, with honest trade-offs.
  
  Heading: "Latency Budget"
  Table: step | budget | if exceeded →
