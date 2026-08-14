# "-story" readability rewrite — progress tracker

Tracks a readability rewrite pass over the narrative `-story.md` companion
files in this folder (see `STORY-PROGRESS.md` for how they were generated).
Goal: same length or longer, simpler language, better structure (subheadings/
bullets/tables), step-by-step worked examples, and cleaned-up Mermaid diagrams
that render well in both light and dark themes. No facts/numbers/sections
dropped.

**Rewrite requirements (apply verbatim each time):**
- Simplify language: short sentences, plain English, reword dense phrases.
- Improve structure: subheadings, bullet lists, tables for dense info; short
  paragraphs, one idea each.
- Keep length: do not shorten/summarize — same length or longer. Sanity-check
  before finishing that nothing was dropped.
- Expand examples: break worked examples into explicit steps, same numbers.
- Fix Mermaid diagrams: clean layout, clear labels, no overlap/cramped/wrapped
  text, colors that work in both light and dark themes.

**How to resume:** find the first unchecked box below, run one subagent to
rewrite that file in place via the requirements above, check the box, commit,
move to the next. One file per subagent call. Stop whenever a session's
context is getting full and report status — no need to finish the whole list
in one sitting.

## Rollout (31 → 78)
- [x] 31 — 31-Design Twitter-FAANG-Guide-story.md
- [x] 32 — 32-Design Newsfeed System-FAANG-Guide-story.md
- [x] 33 — 33-Design-Instagram-FAANG-Guide-story.md
- [x] 34 — 34-Design a URL Shortening Service - TinyURL-FAANG-Guide-story.md
- [x] 35 — 35-Web-Crawler-FAANG-Guide-story.md
- [x] 36 — 36-WhatsApp-FAANG-Guide-story.md
- [x] 37 — 37-Typeahead-Suggestion-FAANG-Guide-story.md
- [x] 38 — 38-Design-a-Collaborative-Document-Editing-Service-Google-Docs-FAANG-Guide-story.md
- [x] 39 — 39-Spectacular-Failures-FAANG-Guide-story.md
- [x] 40 — 40-Design-a-Deployment-System-FAANG-Guide-story.md
- [x] 41 — 41-Design-a-Payment-System-FAANG-Guide-story.md
- [x] 42 — 42-Design-a-ChatGPT-System-FAANG-Guide-story.md
- [x] 43 — 43-Design-a-Data-Infrastructure-System-FAANG-Guide-story.md
- [x] 44 — 44-Design-a-LLM-Customer-Support-Bot-FAANG-Guide-story.md
- [ ] 45 — 45-Design-an-AI-Code-Assistant-FAANG-Guide-story.md
- [ ] 46 — 46-Design-an-IP-Allowlist-Blocklist-Service-FAANG-Guide-story.md
- [ ] 47 — 47-Sanctions-Watchlist-Screening-System-FAANG-Guide-story.md
- [ ] 48 — 48-Legal-Takedown-Propagation-System-FAANG-Guide-story.md
- [ ] 49 — 49-Toll-Vehicle-Insurance-Validity-Check-FAANG-Guide-story.md
- [ ] 50 — 50-National-ID-KYC-Verification-System-FAANG-Guide-story.md
- [ ] 51 — 51-Multi-Source-Sanctioned-Country-Payment-Blocking-FAANG-Guide-story.md
- [ ] 52 — 52-Design-Airbnb-Search-Ranking-FAANG-Guide-story.md
- [ ] 53 — 53-Design-Uber-Surge-Pricing-Engine-FAANG-Guide-story.md
- [ ] 54 — 54-Design-an-AR-Virtual-Furniture-Placement-System-FAANG-Guide-story.md
- [ ] 55 — 55-Design-a-Real-Time-Collaborative-Canvas-Figma-FAANG-Guide-story.md
- [ ] 56 — 56-Design-a-Fraud-Detection-System-FAANG-Guide-story.md
- [ ] 57 — 57-Design-a-Distributed-Tracing-System-FAANG-Guide-story.md
- [ ] 58 — 58-Design-Google-Photos-FAANG-Guide-story.md
- [ ] 59 — 59-Design-a-Recommendation-Engine-Netflix-YouTube-FAANG-Guide-story.md
- [ ] 60 — 60-Design-a-Flash-Sale-System-FAANG-Guide-story.md
- [ ] 61 — 61-Design-a-Global-Distributed-Lock-Service-FAANG-Guide-story.md
- [ ] 62 — 62-Design-Ubers-Driver-Dispatch-System-FAANG-Guide-story.md
- [ ] 63 — 63-Design-an-Ad-Click-Aggregation-System-FAANG-Guide-story.md
- [ ] 64 — 64-Design-an-API-Gateway-FAANG-Guide-story.md
- [ ] 65 — 65-Design-a-Feature-Store-for-ML-FAANG-Guide-story.md
- [ ] 66 — 66-Design-a-Code-Execution-Engine-LeetCode-Replit-FAANG-Guide-story.md
- [ ] 67 — 67-Design-a-Live-Auction-System-FAANG-Guide-story.md
- [ ] 68 — 68-Design-a-Concurrent-Stream-Device-Limiter-FAANG-Guide-story.md
- [ ] 69 — 69-Design-a-Price-Threshold-Alert-System-FAANG-Guide-story.md
- [ ] 70 — 70-Design-an-ETA-and-Location-Sharing-System-FAANG-Guide-story.md
- [ ] 71 — 71-Design-a-Distributed-IoT-Sensor-Telemetry-System-FAANG-Guide-story.md
- [ ] 72 — 72-Design-a-Shared-Internal-Client-SDK-Distribution-System-FAANG-Guide-story.md
- [ ] 73 — 73-Design-a-Webhook-Delivery-System-FAANG-Guide-story.md
- [ ] 74 — 74-Design-an-OTP-2FA-Verification-Service-FAANG-Guide-story.md
- [ ] 75 — 75-Design-a-Warehouse-Task-Assignment-and-Inventory-System-FAANG-Guide-story.md
- [ ] 76 — 76-Design-a-Coupon-Promo-Code-Redemption-System-FAANG-Guide-story.md
- [ ] 77 — 77-Design-a-Video-Conferencing-System-FAANG-Guide-story.md
- [ ] 78 — 78-Design-a-Content-Moderation-System-FAANG-Guide-story.md

## Status: IN PROGRESS
