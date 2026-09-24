# "-story" companion rollout — progress tracker

Tracks the narrative "-story" companion files (see prompt template in memory:
`system-design-guide-story-prompt`) across the numbered `*-FAANG-Guide.md`
problem guides in this folder. Only the main problem guide gets a story file —
sibling deep-dive/production-guide docs are skipped (see decision note below).

**How to resume:** find the first unchecked box below, generate its `-story.md`
via a single subagent using the prompt in memory, check the box, commit, move
to the next. Stop whenever a session's context is getting full and report
status — no need to finish the whole list in one sitting.

**Decision log:**
- 2026-08-10: scope = main `*-FAANG-Guide.md` only, skip `-Deep-Dive`/`-Production-Guide` companions.
- 2026-08-10: continuation = manual, tracker-file based. No cron/background automation.

**Known minor issue (cosmetic, low priority):** each file's subagent picks its own fictional company name independently, so a name has repeated by coincidence: "Ledgerly" is used in both #23 (fintech batch-job scheduler) and #56 (payments fraud detection). Not a correctness problem, just a naming collision across unrelated stories. Worth a quick rename pass at the very end of the rollout if it matters — not worth pausing the pipeline for now.

## Already done (before this rollout)
- [x] 12 — Sequencer
- [x] 16 — Distributed Cache
- [x] 17 — Distributed Messaging Queue

## Rollout (18 → 78)
- [x] 18 — 18-Pub-sub-FAANG-Guide.md
- [x] 19 — 19-Rate-Limiter-FAANG-Guide.md
- [x] 20 — 20-Blob-Store-FAANG-Guide.md
- [x] 21 — 21-Distributed-Search-FAANG-Guide.md
- [x] 22 — 22-Distributed-Logging-FAANG-Guide.md
- [x] 23 — 23-Distributed-Task-Scheduler-FAANG-Guide.md
- [x] 24 — 24-Sharded-Counters-FAANG-Guide.md
- [x] 26 — 26-Design YouTube-FAANG-Guide.md
- [x] 27 — 27-Design Quora-FAANG-Guide.md
- [x] 28 — 28-Design Google Maps-FAANG-Guide.md
- [x] 29 — 29-Design a Proximity Service - Yelp-FAANG-Guide.md
- [x] 30 — 30-Design Uber-FAANG-Guide.md
- [x] 31 — 31-Design Twitter-FAANG-Guide.md
- [x] 32 — 32-Design Newsfeed System-FAANG-Guide.md
- [x] 33 — 33-Design-Instagram-FAANG-Guide.md
- [x] 34 — 34-Design a URL Shortening Service - TinyURL-FAANG-Guide.md
- [x] 35 — 35-Web-Crawler-FAANG-Guide.md
- [x] 36 — 36-WhatsApp-FAANG-Guide.md
- [x] 37 — 37-Typeahead-Suggestion-FAANG-Guide.md
- [x] 38 — 38-Design-a-Collaborative-Document-Editing-Service-Google-Docs-FAANG-Guide.md
- [x] 39 — 39-Spectacular-Failures-FAANG-Guide.md
- [x] 40 — 40-Design-a-Deployment-System-FAANG-Guide.md
- [x] 41 — 41-Design-a-Payment-System-FAANG-Guide.md
- [x] 42 — 42-Design-a-ChatGPT-System-FAANG-Guide.md
- [x] 43 — 43-Design-a-Data-Infrastructure-System-FAANG-Guide.md
- [x] 44 — 44-Design-a-LLM-Customer-Support-Bot-FAANG-Guide.md
- [x] 45 — 45-Design-an-AI-Code-Assistant-FAANG-Guide.md
- [x] 46 — 46-Design-an-IP-Allowlist-Blocklist-Service-FAANG-Guide.md
- [x] 47 — 47-Sanctions-Watchlist-Screening-System-FAANG-Guide.md
- [x] 48 — 48-Legal-Takedown-Propagation-System-FAANG-Guide.md
- [x] 49 — 49-Toll-Vehicle-Insurance-Validity-Check-FAANG-Guide.md
- [x] 50 — 50-National-ID-KYC-Verification-System-FAANG-Guide.md
- [x] 51 — 51-Multi-Source-Sanctioned-Country-Payment-Blocking-FAANG-Guide.md
- [x] 52 — 52-Design-Airbnb-Search-Ranking-FAANG-Guide.md
- [x] 53 — 53-Design-Uber-Surge-Pricing-Engine-FAANG-Guide.md
- [x] 54 — 54-Design-an-AR-Virtual-Furniture-Placement-System-FAANG-Guide.md
- [x] 55 — 55-Design-a-Real-Time-Collaborative-Canvas-Figma-FAANG-Guide.md
- [x] 56 — 56-Design-a-Fraud-Detection-System-FAANG-Guide.md
- [x] 57 — 57-Design-a-Distributed-Tracing-System-FAANG-Guide.md
- [x] 58 — 58-Design-Google-Photos-FAANG-Guide.md
- [x] 59 — 59-Design-a-Recommendation-Engine-Netflix-YouTube-FAANG-Guide.md
- [x] 60 — 60-Design-a-Flash-Sale-System-FAANG-Guide.md
- [x] 61 — 61-Design-a-Global-Distributed-Lock-Service-FAANG-Guide.md
- [x] 62 — 62-Design-Ubers-Driver-Dispatch-System-FAANG-Guide.md
- [x] 63 — 63-Design-an-Ad-Click-Aggregation-System-FAANG-Guide.md
- [x] 64 — 64-Design-an-API-Gateway-FAANG-Guide.md
- [x] 65 — 65-Design-a-Feature-Store-for-ML-FAANG-Guide.md
- [x] 66 — 66-Design-a-Code-Execution-Engine-LeetCode-Replit-FAANG-Guide.md
- [x] 67 — 67-Design-a-Live-Auction-System-FAANG-Guide.md
- [x] 68 — 68-Design-a-Concurrent-Stream-Device-Limiter-FAANG-Guide.md
- [x] 69 — 69-Design-a-Price-Threshold-Alert-System-FAANG-Guide.md
- [x] 70 — 70-Design-an-ETA-and-Location-Sharing-System-FAANG-Guide.md
- [x] 71 — 71-Design-a-Distributed-IoT-Sensor-Telemetry-System-FAANG-Guide.md
- [x] 72 — 72-Design-a-Shared-Internal-Client-SDK-Distribution-System-FAANG-Guide.md
- [x] 73 — 73-Design-a-Webhook-Delivery-System-FAANG-Guide.md
- [x] 74 — 74-Design-an-OTP-2FA-Verification-Service-FAANG-Guide.md
- [x] 75 — 75-Design-a-Warehouse-Task-Assignment-and-Inventory-System-FAANG-Guide.md
- [x] 76 — 76-Design-a-Coupon-Promo-Code-Redemption-System-FAANG-Guide.md
- [x] 77 — 77-Design-a-Video-Conferencing-System-FAANG-Guide.md
- [x] 78 — 78-Design-a-Content-Moderation-System-FAANG-Guide.md

## Status: ROLLOUT COMPLETE (2026-08-11)
All 60 main `*-FAANG-Guide.md` problem guides from 18 through 78 (25 doesn't exist, so 59 actual files + the 3 pre-existing ones = every guide in the folder) now have a `-story.md` companion. Two subagent calls hit transient API errors mid-generation (34, 44) and one hit a session-limit cutoff mid-report (66) — all three were verified against disk and either retried cleanly or confirmed already complete before moving on. No original guide file was modified.
