SECTION 6 — ARCHITECTURE EVOLUTION
Place this section 80px below the bottom edge of Section 5.
Note: this section is taller than others. Allow 200px per version snapshot.

THINK FIRST (write in chat, do not draw yet):
  For each evolution version (4–6 versions):
    — Version label + one-line summary
    — Exactly what component was added, removed, or changed
    — Which bottleneck from Section 5 this fixes
    — The precise technical mechanism — WHY does this fix it?
    — New trade-off introduced
    — New failure mode introduced
    — Scale ceiling of this version

When I say "draw it", draw the following:

LEFT COLUMN — DIAGRAM:
  Stack version snapshots vertically, each separated by 60px.
  Each snapshot is a mini diagram showing ONLY changed components
  (not the full system each time — just the delta):
  
    Version badge circle (36px, colored, bold V1/V2/V3 label)
    Changed components with same shapes/colors as visual contract
    ✅ green box (border #3FB950): "Added: [X] — Fixes: [bottleneck]"
    ⚠️ yellow box (border #D29922): "Trade-off: [new complexity]"
    Thin downward arrow between versions labeled "evolves to ↓"
  
  After all versions, draw a "What We Learned" box spanning full width:
    3–4 key engineering insights from the evolution

RIGHT COLUMN — TEXT:
  For each version transition, write one paragraph:
    What triggered this change | What was chosen | 
    What was rejected and why | What the interviewer wants to see here
  End with:
  Heading: "The Pattern Behind the Evolution"
  One paragraph describing the general principle that drove every 
  upgrade decision (e.g., "every improvement trades consistency for scale").
