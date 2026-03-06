SECTION 9 — FAILURE HANDLING
Place 80px below Section 8.

THINK FIRST:
  — For each failure category (client / external / internal / infrastructure):
      specific error types | handling strategy | retry? | alert?
  — Write the exact retry backoff formula
  — Explain idempotency: what is hashed, stored where, TTL?
  — What triggers DLQ and what does ops do with it?

When I say "draw it":
LEFT: Failure taxonomy tree (root at top, branches to leaves)
  Each leaf: error type | handle how | retry Y/N | alert Y/N
  Color: 🟢 auto-heals | 🟡 retry | 🔴 human needed
  Below tree: 3 pattern cards side by side:
    Card 1: Retry timeline diagram
    Card 2: Circuit breaker state machine (Closed→Open→Half-Open)
    Card 3: Idempotency duplicate detection flow
RIGHT: Retry formula | DLQ policy | Idempotency implementation detail

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

SECTION 10 — TRADE-OFFS
Place 80px below Section 9.

THINK FIRST:
  For each major technology decision:
    Chose | Over | Because (3 reasons) | Costs | Revisit if

When I say "draw it":
LEFT: 2×N grid of Decision Cards, one per major choice
  Each card: "Why [X] over [Y]?" as header
  Fields: Chose / Over / Because / Costs / Revisit if
RIGHT: "The Hardest Trade-off" — paragraph on the single 
  most difficult decision | "What Most Designs Get Wrong" —
  the common mistake for this system type

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

SECTION 11 — INTERVIEW CHEAT SHEET
Place 80px below Section 10.

THINK FIRST:
  — 30-second verbal summary (write it out, practice saying it)
  — 3 hardest interviewer questions + your answer
  — What most candidates miss
  — What separates Senior from Staff for this system
  — 5 key numbers to memorize

When I say "draw it":
LEFT: 5 summary boxes in a clean grid:
  Box 1: Scale Strategy
  Box 2: Reliability Strategy  
  Box 3: Top 3 Hardest Problems + Solutions
  Box 4: Key Numbers (large bold text, 28px)
  Box 5: Technology Summary table
RIGHT: 30-second verbal summary | Follow-up questions | 
  Senior vs Staff answer comparison
