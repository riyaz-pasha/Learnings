SECTION 3 — CAPACITY ESTIMATION
Place this section 80px below the bottom edge of Section 2.

THINK FIRST (write in chat, do not draw yet):
  Show all math step by step:
    DAU × actions/user = daily volume
    daily volume ÷ 86400 = avg QPS
    avg QPS × peak multiplier = peak QPS (explain the multiplier)
    read/write ratio and what drives it
    storage per record (list every field and its byte size)
    total storage = volume × record size × retention × replication factor
    bandwidth = peak QPS × avg payload size
  
  Then produce infrastructure sizing:
    Component | Count | Justification
  
  Finally state the 5 most important numbers to remember.

When I say "draw it", draw the following:

LEFT COLUMN — DIAGRAM:
  Zone labeled "Capacity Estimation" (dark background #0D1117):
  Draw each calculation as a chain of connected boxes:
    [input] → (operation label) → [result] → feeds into → [next input]
  Use thin gray (#8B949E) arrows between calculation boxes
  Calculation boxes: width=200, height=50, background #161B22, text #D29922 monospace
  
  Below the chain, draw the Infrastructure Sizing table:
    Columns: Component | Count | Justification
    Color-code rows by component type using the palette
  
  At the bottom, draw 5 "Key Numbers" highlight boxes (larger, 28px bold numbers)

RIGHT COLUMN — TEXT:
  Heading: "Why These Numbers Matter"
  Paragraph walking through the most important calculation and its assumption.
  Heading: "Peak vs Average — When Does Peak Happen?"
  Paragraph explaining the peak multiplier with a real scenario.
  Heading: "The Storage Challenge"
  Paragraph explaining the storage growth rate and retention decision.
