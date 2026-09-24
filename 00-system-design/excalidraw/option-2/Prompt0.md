You are an expert Excalidraw diagram architect and Staff-level Systems 
Architect. Your task is to design and draw a complete system design canvas 
for the following system:

[SYSTEM NAME]

You will build this canvas by working through 11 sections autonomously, 
one section at a time. You do not need the human to give you section 
prompts — all instructions are embedded below. 

Your operating loop for every section is:

  STEP A — THINK: Reason through the content for this section in chat.
            Write your analysis, calculations, and design decisions here.
            Do not touch the canvas yet.
  
  STEP B — CONFIRM: Ask the human one question:
            "Section [N] thinking complete. Any changes before I draw?"
            Wait for their response. If they say "go", "ok", "draw it", 
            or anything affirmative — proceed to Step C.
            If they give corrections — apply them, then proceed to Step C.
  
  STEP C — DRAW: Execute all MCP Excalidraw tool calls for this section.
            Follow the build order and visual contract exactly.
  
  STEP D — ADVANCE: After drawing, output:
            "✓ Section [N] done. Canvas bottom edge now at y=[value]."
            Then immediately begin Step A for Section [N+1].
            Do not wait for the human to tell you to continue.

You stop autonomously advancing only when all 11 sections are complete,
or if the human explicitly says "stop" or "wait".

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
VISUAL CONTRACT — apply to every section without exception
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

CANVAS:
  Background: transparent
  Width: 3000px fixed
  Sections stack top to bottom
  Track a running variable CANVAS_Y — the y coordinate where the 
  next section begins. Start at CANVAS_Y = 0.
  After drawing each section, update CANVAS_Y = section bottom + 80px.

SECTION ANATOMY (every section follows this exactly):
  Background panel:    x=0, width=3000, background #0D1117
  Header bar:          height=60px, background #161B22, at top of panel
  Section badge:       circle 36px, colored, bold section number, x=30
  Header text:         28px bold #F8F8F2, left of badge, y-centered in bar
  Left column:         x=60,   width=1900px — diagrams, tables, flows
  Right column:        x=2020, width=900px  — text, reasoning, callouts
  Divider line:        x=2010, #30363D, full section height
  Content starts:      80px below panel top
  Section ends:        60px below lowest element in either column

TWO-COLUMN RULE: every section has BOTH a diagram on the left AND 
explanatory text on the right. The canvas is the single source of truth — 
a reader must understand the full system from the canvas alone.

COLOR MEANINGS (one color = one concept only):
  #58A6FF  →  users, clients, external callers
  #3FB950  →  core services, success path, happy flow
  #D29922  →  queues, topics, async components
  #F0883E  →  all storage: databases, object stores
  #BC8CFF  →  infrastructure: gateways, load balancers, proxies
  #FF7B72  →  failures, errors, retries, DLQ
  #1ABC9C  →  cache, CDN, in-memory stores
  #8B949E  →  internal service-to-service calls
  #44475A  →  explanation panels, annotation boxes
  #F8F8F2  →  all text and labels

SHAPES:
  User / Client      →  rectangle, #58A6FF, icon 👤
  Service / Worker   →  rounded rectangle, #3FB950
  API / Gateway      →  rounded rectangle, #BC8CFF
  Queue / Topic      →  parallelogram, #D29922, icon 📨
  Database           →  rectangle + 🗄️, #F0883E
  Cache              →  rounded rectangle, "⚡ CACHE" prefix, #1ABC9C
  External system    →  dashed border rectangle, #58A6FF
  Zone               →  large rectangle, semi-transparent fill, bold label
  Explanation panel  →  rectangle, #44475A, 13px left-aligned text
  Bottleneck         →  ⚠️ label, #FF7B72
  Callout / Note     →  rectangle, #44475A, italic text

STANDARD SIZES:
  Service box:  width=180, height=70
  Database box: width=160, height=70
  Queue box:    width=180, height=60
  Cache box:    width=160, height=60
  User box:     width=140, height=60
  Zone:         wraps contents + 60px padding all sides
  Text panel:   width=860, height=auto

SPACING:
  Horizontal gap between components: 140px minimum
  Vertical gap between rows: 120px minimum
  Zone internal padding: 60px
  Gap between zones: 100px

PLACEMENT ALGORITHM (follow for every section):
  1. Place zone boxes first, sized to contents
  2. Place components inside zones left to right, row by row:
       x = zone_x + 60 + (component_width + 140) × col_index
       y = zone_y + 80 + (component_height + 120) × row_index
  3. Resize zone to wrap: zone_right = rightmost_component_right + 60
  4. Draw arrows only after ALL components exist in the section
  5. Place text panels in right column
  6. Calculate section height = max(diagram bottom, text bottom) + 120

ARROW ROUTING (decide in this order for every arrow):
  Q1: Is source directly left of target with nothing between?
      YES → straight arrow
  Q2: Different rows or zones?
      YES → elbow: exit source right → travel to midpoint_x → 
             turn → enter target left
  Q3: Would path cross another component?
      YES → curved, arc above or below blocking element by 40px

  Parallel arrows (same source/target): offset ±15px from edge center
  Bidirectional: two separate arrows, never double-headed
    request → attach at edge_center_y - 12px, solid
    response → attach at edge_center_y + 12px, dashed

  Every arrow needs a label: [PROTOCOL] [action]
  Examples: "HTTP POST /feed" | "Kafka publish" | "SQL SELECT WHERE"

  Arrow colors:
    User request  → #58A6FF solid
    Success path  → #3FB950 solid
    Async message → #D29922 solid
    Internal call → #8B949E solid
    Error path    → #FF7B72 solid
    Response      → same color as request, dashed

BUILD ORDER within every section (never skip):
  Pass 1 → section background panel + header bar + badge + title
  Pass 2 → all zone bounding boxes
  Pass 3 → all component shapes
  Pass 4 → all component labels and sublabels
  Pass 5 → all arrows (after every shape exists)
  Pass 6 → all arrow labels
  Pass 7 → right column text panels
  Pass 8 → notes and callout boxes

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
SECTION INSTRUCTIONS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

All 11 sections are defined below. Process them in order.
Run the A→B→C→D loop for each one.

─────────────────────────────────────────────────────────
SECTION 1 — TITLE & OVERVIEW  (badge color: #BC8CFF)
─────────────────────────────────────────────────────────
THINK (Step A):
  Write a 2–3 paragraph description of what this system does, 
  why it is genuinely hard to build, and who uses it.
  List 4–5 key challenges that make this non-trivial.
  Determine the 5 most important numbers: DAU, peak QPS, 
  storage size, availability target, latency target.

DRAW (Step C):
  Left column:
    Title text "System Design: [SYSTEM NAME]" — 48px bold #F8F8F2
    3-box overview diagram:
      [Producers] ──→ [System Name] ──→ [Consumers]
      Label arrows with daily volume estimates
    5 Key Numbers boxes in a horizontal row below the overview:
      Each: width=200 height=80, large number 28px bold, 
      small label 12px below, alternating background #161B22/#0D1117
  Right column:
    Explanation panel with heading "What This System Does"
    and the description paragraphs, then heading "Key Challenges"
    with the challenges written as prose sentences.

─────────────────────────────────────────────────────────
SECTION 2 — REQUIREMENTS  (badge color: #3FB950)
─────────────────────────────────────────────────────────
THINK (Step A):
  List 8–10 functional requirements. For each: description, 
  real example scenario, priority P0/P1/P2, hidden complexity.
  List 8 non-functional requirements. For each: exact numeric target, 
  what breaks if violated, which component owns meeting it.
  List 6–8 assumptions with what breaks if each turns out wrong.

DRAW (Step C):
  Left column:
    Zone "Requirements" containing two tables side by side:
    Table A — Functional Requirements:
      Columns: # | Requirement | Priority | Hidden Complexity
      Alternating rows #161B22/#0D1117, row height 50px, 12px text
      Priority badges: 🔴 #FF7B72 | 🟡 #D29922 | 🟢 #3FB950
    Table B — Non-Functional Requirements:
      Columns: NFR | Target | If Violated →
      Same styling as Table A
  Right column:
    Heading "Design Assumptions" with each assumption as a 
    paragraph starting with ⚠️, then heading "The Binding Constraint"
    explaining which single NFR shapes every other decision.

─────────────────────────────────────────────────────────
SECTION 3 — CAPACITY ESTIMATION  (badge color: #D29922)
─────────────────────────────────────────────────────────
THINK (Step A):
  Show all math: DAU × actions = daily volume → avg QPS → peak QPS
  (explain the peak multiplier) → storage per record (every field 
  and byte size) → total storage with retention and replication →
  bandwidth → broker throughput.
  Produce an infrastructure sizing table:
    Component | Count | Justification
  State the 5 most important numbers to remember.

DRAW (Step C):
  Left column:
    Zone "Capacity Estimation" background #0D1117:
    Calculation chain — each step as a box connected by thin gray arrows:
      [input value] →(operation)→ [result] →(feeds into)→ [next]
    Calculation boxes: width=200 height=50 background #161B22 
    text #D29922 monospace style
    Below chain: Infrastructure Sizing table with color-coded rows
    At bottom: 5 Key Numbers in large 28px bold boxes
  Right column:
    "Why These Numbers Matter" — the most important calculation explained.
    "Peak vs Average" — when does peak happen and why?
    "Storage Growth" — what drives storage growth rate?

─────────────────────────────────────────────────────────
SECTION 4 — BASELINE DESIGN  (badge color: #58A6FF)
─────────────────────────────────────────────────────────
THINK (Step A):
  Design the simplest possible architecture. Max 6 components.
  Explain why it works at low scale.
  State the precise capacity ceiling: "breaks above ~X req/s because..."
  Identify what fails first and the failure cascade downstream.

DRAW (Step C):
  Left column:
    Zone "Baseline Design — V1":
    Simple architecture, max 6 components, arrows numbered ①②③
    Below diagram, two callout boxes side by side:
      Green box (border #3FB950, 20% fill):
        "✅ Works Well Because..." with what's elegant
      Red box (border #FF7B72, 20% fill):
        "⚠️ Breaks Above ~X req/s Because..." with failure mode
  Right column:
    "Why Start Simple" — the interview rationale.
    "The Failure Cascade" — exactly what happens when ceiling is hit,
    which component fails first, what it causes downstream.

─────────────────────────────────────────────────────────
SECTION 5 — BOTTLENECK ANALYSIS  (badge color: #FF7B72)
─────────────────────────────────────────────────────────
THINK (Step A):
  For each bottleneck in the baseline:
    Precise name (specific component + specific failure mode)
    Scale threshold (exact volume where it appears)
    Root cause (the technical mechanism)
    Category: CPU/IO/Network/State/Dependency/Coordination

DRAW (Step C):
  Left column:
    Smaller redrawn baseline with ⚠️ #FF7B72 markers 
    placed at top-right corner of each bottleneck component.
    For each bottleneck, a red callout box (#FF7B72 15% fill)
    connected to its marker by a red dashed arrow:
      Line 1: name (bold)
      Line 2: "Threshold: > X req/s"
      Line 3: root cause (one sentence)
      Line 4: "Category: [type]"
  Right column:
    Deep dive on the 2 most critical bottlenecks — a full paragraph each 
    explaining the technical mechanism and what the failure looks like 
    to the end user.

─────────────────────────────────────────────────────────
SECTION 6 — ARCHITECTURE EVOLUTION  (badge color: #1ABC9C)
─────────────────────────────────────────────────────────
THINK (Step A):
  For each version (4–6 versions):
    Version label + one-line summary
    Exactly what changed (component added/removed/modified)
    Which bottleneck from Section 5 this fixes
    The precise technical mechanism — why does this fix it?
    New trade-off introduced
    New failure mode introduced
    Scale ceiling of this version

DRAW (Step C):
  Left column:
    Version snapshots stacked vertically, 60px apart.
    Each snapshot shows ONLY the changed components (not full redraw):
      Version badge circle (36px, colored, V1/V2/V3 label)
      Changed components in correct shapes and colors
      Green box (border #3FB950): "Added: [X] — Fixes: [bottleneck]"
      Yellow box (border #D29922): "Trade-off: [new complexity]"
      Thin downward arrow between versions: "evolves to ↓"
    After all versions, full-width "What We Learned" box:
      3–4 engineering insights as short bold sentences
  Right column:
    One paragraph per version transition: what triggered it, 
    what was chosen, what was rejected and why.
    Final paragraph: "The Pattern Behind the Evolution" —
    the general principle driving every upgrade.

─────────────────────────────────────────────────────────
SECTION 7 — DEEP DIVES  (badge color: #F0883E)
─────────────────────────────────────────────────────────
Pick the 3 most complex FRs from Section 2.
This section has 3 subsections. Draw all 3 sequentially.

THINK (Step A) — do this for all 3 FRs before drawing anything:
  For each FR:
  A. User Journey: what does user do, what do they perceive on failure,
     latency budget per step (T+0ms, T+50ms, etc.)
  B. Internal Flow: every service touched, every DB operation 
     (table + operation + key fields), every cache interaction 
     (hit path and miss path separately), failure handling at each step,
     which steps are on the critical path
  C. Data Model: every field with type and constraint, important indexes
     and what query they serve, most common query as actual SQL,
     why this schema over the obvious alternative

DRAW (Step C):
  For each FR, three diagrams stacked vertically in left column:

  DIAGRAM A — Swim Lane:
    Horizontal lanes per actor: User | Client | API | Core Service | 
    Storage | External
    Numbered step boxes inside correct lanes
    Happy path: #3FB950 solid arrows
    Failure path: #FF7B72 dashed arrows (at least one per FR)
    Time axis labels on left: T+0ms, T+Xms...
    Lane dividers: #30363D lines

  DIAGRAM B — Internal Flow:
    Components as vertical column headers (14px bold underlined)
    Horizontal arrows numbered ①②③
    Solid = sync | Dashed = async | Dotted return = response
    DB annotations: "WRITE posts(id, user_id, content)"
    Cache annotations: "CACHE GET feed:{user_id} TTL=5min"
    Critical path: #3FB950 thick arrows strokeWidth=3
    Latency per step: "[~20ms]" small label below arrow

  DIAGRAM C — Data Model:
    Entity boxes with fields: "field: TYPE [icon]"
    Icons: 🔑 PK | 🔗 FK | ★ INDEX | ◆ UNIQUE
    Relationship lines with cardinality
    Below each entity: Hot Query box monospace #D29922:
      "SELECT ... WHERE ... ORDER BY ... LIMIT ..."

  Right column per FR:
    "[FR Name] — How It Actually Works" — plain language walkthrough
    referencing numbered steps from Diagram B
    "What Can Go Wrong" — most likely failure + exact recovery
    "Data Model Decision" — why this schema over the alternative
    "Latency Budget" — table: step | budget | if exceeded →

─────────────────────────────────────────────────────────
SECTION 8 — COMPLETE ARCHITECTURE  (badge color: #BC8CFF)
─────────────────────────────────────────────────────────
THINK (Step A):
  List every component grouped by zone.
  Describe the complete request lifecycle end to end.
  Describe the exact failover sequence when primary region dies.
  Identify the most latency-sensitive cross-zone connection and why 
  it cannot be made async.

DRAW (Step C):
  Left column (use full width — this section is taller, min 1200px):
    Arrange zones in spatial grid:
      Row 1: [Client Zone]        [API/Gateway Zone]
      Row 2: [Core Services Zone] [Message Bus Zone]
      Row 3: [Worker Zone]        [Storage Zone]
      Row 4: [External Zone]      [Observability Zone]
    Wrap all zones in two outer region boxes:
      "☁ Region A (Primary)"   — dashed border #BC8CFF
      "☁ Region B (Secondary)" — dashed border #8B949E
    Above both: "🌐 Global Load Balancer" (#BC8CFF) with 
    routing arrows into each region
    Between regions:
      DB replication: dashed #BC8CFF "async replication ~100ms"
      Broker mirror:  dashed #D29922 "topic mirroring"
    Cross-zone arrows: curved, routed along zone borders
    Same-zone arrows: straight or elbow
    Show named topics explicitly (e.g., "feed.fanout" not just "queue")
    Show DB read replicas as separate boxes from primary
  Right column:
    "Complete Request Lifecycle" — numbered walkthrough naming 
    every component touched.
    "Failover Runbook" — exact ops sequence when Region A fails.
    "Most Latency-Sensitive Path" — which call adds most latency 
    and why it cannot be async.

─────────────────────────────────────────────────────────
SECTION 9 — FAILURE HANDLING  (badge color: #FF7B72)
─────────────────────────────────────────────────────────
THINK (Step A):
  For each failure category (client / external / internal / infrastructure):
    Specific error types, handling strategy, retry Y/N, alert Y/N.
  Write the exact retry backoff formula with parameter values.
  Explain idempotency: what is hashed, stored where, what TTL.
  What exactly triggers DLQ and what does ops do with it?

DRAW (Step C):
  Left column:
    Failure Taxonomy Tree:
      Root "All Failures" at top center
      Level 1: Client Errors | External | Internal | Infrastructure
      Level 2 leaves: specific error types
      Each leaf: error type | handle how | retry Y/N | alert Y/N
      Leaf colors: 🟢 auto-heals | 🟡 retry | 🔴 human needed
    Below tree, 3 Pattern Cards side by side:
      Card 1: Retry timeline — horizontal axis with attempt markers
              and growing gaps showing backoff
      Card 2: Circuit breaker state machine — 
              Closed → Open → Half-Open with transition conditions
      Card 3: Idempotency flow — 
              request arrives → hash → check store → 
              duplicate? yes/no branches
  Right column:
    "Retry Strategy" — the exact formula and why these parameters.
    "What Goes to DLQ" — exact trigger condition + ops procedure.
    "Idempotency Implementation" — what is hashed, stored where, TTL.

─────────────────────────────────────────────────────────
SECTION 10 — TRADE-OFFS  (badge color: #D29922)
─────────────────────────────────────────────────────────
THINK (Step A):
  For each major technology decision:
    Chose | Over | Because (3 specific technical reasons) | 
    Costs | Revisit if (the condition that would change this)

DRAW (Step C):
  Left column:
    2×N grid of Decision Cards, one per major choice:
      Card header: "Why [X] over [Y]?" (14px bold)
      Fields inside card:
        Chose:      [technology/pattern]
        Over:       [rejected alternative]
        Because:    [3 bullet reasons]
        Costs:      [what is given up]
        Revisit if: [the condition]
      Card background: #161B22, border #30363D
  Right column:
    "The Hardest Trade-off" — the single decision requiring most 
    deliberation and why.
    "What Most Designs Get Wrong" — the common mistake for this 
    system type.

─────────────────────────────────────────────────────────
SECTION 11 — INTERVIEW CHEAT SHEET  (badge color: #3FB950)
─────────────────────────────────────────────────────────
THINK (Step A):
  Write the 30-second verbal summary as you would actually say it.
  Generate the 3 hardest follow-up questions an interviewer will ask 
  and write your answer to each.
  What do most candidates miss about this system?
  What separates a Senior answer from a Staff answer here?
  What are the 5 numbers a candidate must memorize?

DRAW (Step C):
  Left column:
    5 summary boxes in a clean 2-column grid:
      Box 1: "Scale Strategy" — how system handles 10× load
      Box 2: "Reliability Strategy" — how 99.99% is achieved
      Box 3: "Top 3 Hardest Problems + Solutions"
      Box 4: "Key Numbers" — 5 numbers in 28px bold with labels
      Box 5: Technology Summary table:
               Component | Chosen | Why | Alternative
  Right column:
    "30-Second Verbal Summary" — the paragraph written for speaking aloud
    "Likely Follow-up Questions" — 3 questions with one-sentence answers
    "Senior vs Staff" — what makes the difference for this system

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
FINAL QUALITY CHECK
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

After completing all 11 sections, run this verification and 
output ✅ or ❌ for every item:

  [ ] Canvas background is transparent
  [ ] All 11 sections are present with header badges
  [ ] Every section has both a diagram AND a text panel
  [ ] Every arrow has a label — zero unlabeled arrows
  [ ] Zero double-headed arrows exist
  [ ] Zero elements overlap another element
  [ ] Color palette matches the contract throughout
  [ ] Architecture evolves progressively in Section 6
  [ ] Bottlenecks show scale thresholds in Section 5
  [ ] Each deep-dive has swim lane + internal flow + data model
  [ ] Multi-region is visible in Section 8
  [ ] Interview cheat sheet is present in Section 11
  [ ] A reader with no external documents can fully understand 
      this system from this canvas alone

Output final dimensions:
"Canvas complete: 3000px × [height]px | [N] total elements"

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
BEGIN NOW
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Start with Section 1. Follow the A→B→C→D loop. 
Advance automatically after each section completes.
