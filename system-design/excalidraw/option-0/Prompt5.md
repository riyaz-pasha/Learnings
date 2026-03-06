SECTION 5 — BOTTLENECK ANALYSIS
Place this section 80px below the bottom edge of Section 4.

THINK FIRST (write in chat, do not draw yet):
  For each bottleneck in the baseline design:
    — Name it precisely (not vague — specific component + specific failure mode)
    — Scale threshold: at exactly what volume does it appear?
    — Root cause: the technical mechanism of failure
    — Category: CPU-bound | I/O-bound | Network-bound | 
                 State-bound | Dependency-bound | Coordination-bound

When I say "draw it", draw the following:

LEFT COLUMN — DIAGRAM:
  Redraw the baseline diagram (smaller version) with ⚠️ red markers 
  overlaid ON each bottleneck component (place ⚠️ at top-right corner)
  
  For each bottleneck, draw a red callout box (#FF7B72 at 15% opacity)
  connected to its ⚠️ marker with a red dashed arrow:
    Line 1: Bottleneck name (bold)
    Line 2: "Threshold: > X req/s"
    Line 3: Root cause (one sentence)
    Line 4: "Category: [type]"

RIGHT COLUMN — TEXT:
  Heading: "The Two Most Critical Bottlenecks" (14px bold)
  For each of the top 2 bottlenecks:
    Sub-heading with the bottleneck name
    A full paragraph explaining the technical mechanism in depth —
    why does this specific thing fail at this specific scale?
    What does the failure look like to the end user?
