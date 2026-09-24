━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 2 — LAYOUT PLANNING (the step GPT skips)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

[PASTE PHASE 1 OUTPUT HERE]

─────────────────────────────────────────────────────────

You are a technical diagram architect. Do NOT draw anything yet.
Your job is to produce a precise layout plan that Phase 3 will 
execute in Excalidraw without ambiguity.

For each of the 12 canvas sections, output a structured spec:

SECTION [N] — [NAME]
  Canvas Y position: [calculated from previous section height]
  Estimated height: [px]
  
  Components:
    - ID: [unique-kebab-id]
      Label: [display text]
      Type: service | queue | database | cache | external | user | zone | note
      Color: [hex from palette]
      Position: [LEFT | CENTER-LEFT | CENTER | CENTER-RIGHT | RIGHT]
      Row: [1 | 2 | 3] (vertical row within section)
      
  Connections:
    - From: [component-id]
      To: [component-id]  
      Label: [protocol + action]
      Type: sync | async | response | error
      Route: straight | elbow | curved
      Conflict: [yes/no — would this arrow cross another element?]
      If conflict: [describe detour path]

  Zones:
    - Zone name: [label]
      Contains: [list of component IDs]
      
  Notes:
    - Anchor: [component-id it explains]
      Text: [the explanation text from Phase 1]
      Position: [above | below | right | left]

─────────────────────────────────────────────────────────

Do this for ALL 12 sections.

This layout plan must be COMPLETE enough that Phase 3 can draw 
the entire board mechanically without making any layout decisions.

Flag any section where content is complex enough to need 
sub-diagrams (swim lanes, sequence diagrams, evolution snapshots).
For those, produce a NESTED component spec within that section.

At the end, output:
  TOTAL CANVAS HEIGHT: [sum of all section heights + gaps]
  TOTAL CANVAS WIDTH: [2800px unless content requires more]
  SECTIONS WITH HIGHEST OVERLAP RISK: [list which sections need 
  most careful arrow routing in Phase 3]
