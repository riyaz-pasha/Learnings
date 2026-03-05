━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 3 — EXCALIDRAW DRAWING
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

[PASTE PHASE 2 LAYOUT PLAN HERE]

─────────────────────────────────────────────────────────

Execute the layout plan above in Excalidraw exactly as specified.
Follow these rules without exception:

COLOR PALETTE (the only colors allowed):
  Users / Clients       → #4F8EF7
  API / Gateway         → #4CD964
  Core Services         → #2ECC71
  Queues / Async        → #F4D03F
  Databases             → #F39C12
  Cache                 → #1ABC9C
  External / 3rd Party  → #AF7AC5
  Infrastructure        → #7F8C8D
  Error / Failure       → #E74C3C
  Explanation Notes     → #44475A
  Bottleneck Marker     → #FF5555
  Improvement Box       → #50FA7B
  Zone Background       → #1E1E2E
  All Text              → #F8F8F2

SHAPES:
  service/API     → rounded rectangle, strokeWidth 2
  database        → use 🗄️ label, rectangle
  queue           → parallelogram shape or rectangle with 📨
  cache           → rounded rectangle labeled ⚡CACHE
  external        → rectangle with strokeStyle "dashed"
  zone            → large rectangle, fill opacity 20%, bold label top-left
  note            → rectangle, background #44475A, italic text
  bottleneck      → triangle or ⚠️ text box, color #FF5555
  improvement     → rectangle, background #50FA7B, bold text

SPACING:
  Horizontal gap between components: 140px minimum
  Vertical gap between components: 120px minimum
  Section gap: 280px
  Zone internal padding: 60px
  Canvas origin: x=100, y=100

ARROW RULES (execute in this priority order):
  Step 1: Calculate source EXIT point (right-center or bottom-center)
  Step 2: Calculate target ENTRY point (left-center or top-center)
  Step 3: Check if straight line passes through any other element
           → If NO: draw straight arrow
           → If YES: go to Step 4
  Step 4: Try elbow routing (go out from source, turn 90°, enter target)
           → If elbow still conflicts: go to Step 5  
  Step 5: Use curved arrow routed along zone boundary edges
  Step 6: For parallel arrows on same source→target path:
           offset attachment points by 20px (top arrow: -20px, bottom: +20px)
  Step 7: For bidirectional flows: 
           NEVER use double-headed arrow
           Draw two arrows, request offset -15px, response offset +15px
  
  Every arrow:
  - Has a text label (italic, size 11)
  - Color matches type: blue=user req, green=success, 
    yellow=async, gray=internal, red=error, dashed=response
  - startArrowhead: null
  - endArrowhead: "arrow"

TYPOGRAPHY:
  Section header:    size 28, bold, #F8F8F2
  Zone label:        size 18, bold, accent color
  Component name:    size 14, bold, #F8F8F2
  Arrow label:       size 11, italic, matches arrow color
  Note text:         size 12, regular, #F8F8F2
  Badge numbers:     size 16, bold, white on colored circle

BUILD ORDER:
  1. Draw all zone bounding boxes first
  2. Place all component shapes inside zones
  3. Add all text labels
  4. Draw arrows last (routing is easier when all elements exist)
  5. Add explanation notes after arrows
  6. Add section headers and dividers
  7. Final pass: scan for unlabeled arrows → label them
  8. Final pass: scan for overlapping elements → move them
  9. Group each section
  10. Report canvas dimensions

QUALITY GATE — before finishing, verify:
  [ ] Zero unlabeled arrows
  [ ] Zero overlapping elements  
  [ ] Zero arrows passing through unrelated components
  [ ] Every zone has a visible label
  [ ] Color matches palette for every element
  [ ] All 12 sections present
  [ ] Section headers have numbered badge circles
  [ ] Evolution section shows V1→VN progression
  [ ] Multi-region visible in final architecture
  [ ] Interview cheat sheet present
