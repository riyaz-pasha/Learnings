SECTION 8 — COMPLETE ARCHITECTURE
Place this section 80px below the bottom edge of Section 7.
This is the most important section. Allow extra height — minimum 1200px.

THINK FIRST (write in chat, do not draw yet):
  — List every component in the final system grouped by zone
  — Describe the complete request lifecycle end to end
  — Describe the failover sequence when the primary region dies
  — Identify which cross-zone connections are most latency-sensitive

When I say "draw it", draw the following:

LEFT COLUMN — FULL ARCHITECTURE DIAGRAM:
  
  Arrange zones in this spatial grid (adjust sizes to content):
  
  Row 1: [Client Zone, x=60]          [API/Gateway Zone, x=700]
  Row 2: [Core Services Zone, x=60]   [Message Bus Zone, x=900]  
  Row 3: [Worker Zone, x=60]          [Storage Zone, x=700]
  Row 4: [External Zone, x=60]        [Observability Zone, x=700]
  
  Wrap ALL zones in two large outer region boxes:
    Left outer box:  "☁ Region A (Primary)"   — dashed border #BC8CFF
    Right outer box: "☁ Region B (Secondary)" — dashed border #8B949E
  
  Above both regions: "🌐 Global Load Balancer" box (#BC8CFF)
  with routing arrows going down into each region
  
  Between regions, draw replication arrows:
    DB replication: dashed #BC8CFF, label "async replication ~100ms"
    Broker mirror:  dashed #D29922, label "topic mirroring"
  
  ARROW ROUTING RULES FOR THIS DIAGRAM:
    Cross-zone arrows: always curved, routed along zone borders
    Same-zone arrows: straight or elbow
    No arrow passes through a zone it doesn't connect
    Show named topics explicitly (not just "queue" — e.g., "feed.fanout")
    Show DB read replicas as separate boxes from primary

RIGHT COLUMN — TEXT:
  Heading: "Complete Request Lifecycle" (14px bold)
  Numbered walkthrough of a full request, naming every component.
  
  Heading: "Failover Runbook"
  Exact sequence when Region A fails:
  Step 1 → Step 2 → ... written as an ops procedure.
  
  Heading: "Most Latency-Sensitive Path"
  Which cross-component call adds the most latency and why it 
  cannot be made async.
