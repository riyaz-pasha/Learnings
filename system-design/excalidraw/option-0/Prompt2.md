SECTION 2 — REQUIREMENTS
Place this section 80px below the bottom edge of Section 1.

THINK FIRST (write in chat, do not draw yet):
  — List 8–10 functional requirements. For each:
      description | real example scenario | priority P0/P1/P2 | 
      hidden complexity (what makes it harder than it looks)
  — List 8 non-functional requirements. For each:
      exact numeric target | what breaks if violated | 
      which component is responsible for meeting it
  — List 6–8 assumptions. For each:
      the assumption + what breaks in the design if it turns out to be wrong

When I say "draw it", draw the following:

LEFT COLUMN — DIAGRAM:
  Zone labeled "Requirements" containing two tables side by side:
  
  Table A — Functional Requirements (width=920px):
    Header row: # | Requirement | Priority | Hidden Complexity
    One row per FR, alternating background #161B22 / #0D1117
    Priority badges colored: 🔴 P0 (#FF7B72) | 🟡 P1 (#D29922) | 🟢 P2 (#3FB950)
    Row height: 50px, text 12px
  
  Table B — Non-Functional Requirements (width=820px, placed right of Table A):
    Header row: NFR | Target | If Violated →
    One row per NFR
    Same alternating rows, same sizing

RIGHT COLUMN — TEXT:
  Heading: "Design Assumptions" (14px bold)
  Each assumption as a paragraph starting with ⚠️
  Below assumptions:
  Heading: "The Binding Constraint"
  One paragraph explaining which single NFR shapes every other 
  decision in this design and why.
