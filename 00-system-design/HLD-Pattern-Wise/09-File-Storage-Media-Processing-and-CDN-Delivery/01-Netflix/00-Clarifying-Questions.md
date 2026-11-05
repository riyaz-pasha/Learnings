**Interpretation:** "Design Netflix" means a global on-demand video streaming service with a licensed catalog, personalized browsing and playback on many devices.

## 1. Pattern and main design forks

- **Pattern:** Video streaming and media delivery (CDN-heavy), with a feed-style recommendation layer.
- **Forks:**
  - VOD only vs VOD plus live streaming
  - Pre-computed recommendations vs real-time recommendations
  - Strict entitlement checks vs cached, relaxed checks
  - One catalog worldwide vs per-country catalogs and licensing windows
  - Gradual title release vs exact-time global launch

## 2. Clarifying questions

| # | Bucket | Priority | Question, as I would say it out loud | If A, then... | If B, then... | Design decision it drives |
|---|---|---|---|---|---|---|
| 1 | S | P0 | "I'll focus on browse, search, play, resume watching and a personalized home page. I'll treat studio upload as a pipeline we need, but not go deep. Billing, ads and social features are out. Okay?" | Draw the playback, catalog and recommendation paths. Ingest is one box. | If ingest is the focus, draw a full transcoding and publishing pipeline. If billing is in, add a payments subsystem. | Which subsystems get depth |
| 2 | S | P0 | "Is this on-demand only, or do we also need live events?" | VOD: pre-transcode everything and cache at the edge. | Live: add a low-latency ingest and segmenter, with no pre-warmed cache. | Delivery architecture |
| 3 | C | P0 | "The thing that must never go wrong: a user without a valid subscription or license must not get playable content, and a paying user must not be wrongly blocked. Should we also enforce a hard cap on concurrent streams per account?" | Strict: check at every play start, keep a central session counter, and use short-lived signed tokens. | Soft: cache entitlement for minutes and skip the central counter. | Entitlement and session service |
| 4 | C | P1 | "For resume position across devices, I'll assume a few seconds of staleness is fine and last write wins. Okay?" | Async, batched heartbeats and an eventually consistent store. | Needs exact cross-device sync: synchronous writes and stronger consistency. | Progress write path |
| 5 | A | P1 | "I'll assume reads dominate by a huge margin. Popularity is very skewed, with new releases getting a large share of traffic and a long tail of rarely watched titles. Fair?" | Push popular titles to edge caches ahead of time and use a tiered cache for the tail. | Flat demand: more origin fetches and fewer pre-positioned copies. | Cache and CDN strategy |
| 6 | A | P1 | "Should the home page be personalized per user from a daily batch, or should it react to what I just watched?" | Batch precompute and serve from a cache. | Near-real-time: add streaming features and online ranking. | Recommendation architecture |
| 7 | L | P1 | "I'll assume 200M subscribers, 100M daily users, a 3x evening peak, about 10M concurrent streams at about 5 Mbps. Playback should start in under 2 seconds at p99, with 99.99% availability for playback. Reasonable?" | Edge delivery is the main cost and design driver. Control plane QPS is modest. | If much smaller, a third-party CDN and a single region may be enough. | Scale, CDN and availability |
| 8 | E | P1 | "Is the catalog the same in every country, or does it differ by region and licensing windows? Does it need to be served from multiple regions?" | Same catalog: simple global metadata. | Per-country: region-aware catalog and entitlement filtering, with regional data replication. | Catalog and metadata model |
| 9 | D | P0 | "If personalization is down, I'll show generic popular rows. If entitlement checks are down, existing streams continue but new plays use a short grace window. Agree?" | Tiered graceful degradation with fallbacks. | Must fail closed: stricter replication and no grace window. | Failure modes |
| 10 | D | P1 | "When a new title is added, can it stay hidden until every version is transcoded and cached? Or must it go live at an exact time worldwide?" | Publish when ready, asynchronously. | Exact launch time: pre-position content to edge caches and add a scheduled release gate. | Ingest and release flow |

**How I'd say it out loud (two blocks):**
- *Block 1 (scope and correctness):* Questions 1, 2, 3, 4.
- *Block 2 (scale, environment, failure):* Questions 5 to 10.

## 3. Assumptions to state out loud

- **Offline downloads are out of scope.** If they're in, add license expiry and a device-bound DRM flow.
- **Ads and a free tier are out.** If they're in, add ad insertion and decisioning.
- **Billing and signup are external.** If not, add a payments subsystem.
- **Standard DRM and adaptive bitrate streaming are used.** If custom, the key service design changes.
- **Multiple profiles per account are supported.** If not, personalization is simpler.
- **Subtitles and audio tracks are part of each title's renditions.** If not, the catalog model changes slightly.
- **Content is retained indefinitely.** If expiry is required, add lifecycle and takedown flows.
- **Compliance is basic privacy law.** Strict data residency would mean region-pinned data stores.
- **Budget is large but cost-aware.** If tight, a third-party CDN and fewer renditions.
- **Only studios and operators upload content.** If user uploads are allowed, add moderation.

## 4. Questions I deliberately did NOT ask

- **"Which CDN or cloud should we use?"** It is answer-seeking. The candidate decides.
- **"Which codecs and bitrate ladder?"** This is low-level, and the architecture is the same either way.
- **"Do we need subtitles?"** Either answer gives the same architecture, with extra files per title.
- **"Should users be able to log in and see profiles?"** This is obvious or UI detail, so it belongs in assumptions.

## 5. Interviewer cheat sheet

| # | Good answer | Tricky answer | How the design changes |
|---|---|---|---|
| 1 | "Browse, play, resume, recommend. Ingest at high level." | "Focus on the ingest and transcoding pipeline." | Deep dive on the pipeline: chunking, parallel encoding, QA, publishing. |
| 2 | "VOD only." | "VOD now, live sports next quarter." | Add a live path with a segmenter and a small buffer. |
| 3 | "Strict on entitlement. Limit is 4 streams." | "Limit must hold across devices in different regions, in real time." | Global session store with cross-region coordination and a conflict policy. |
| 4 | "A few seconds stale is fine." | "Must resume exactly where I left off, even if I switch devices mid-scene." | Sync writes on pause and stop, plus fast reads on play. |
| 5 | "Yes, hot new releases dominate." | "Nearly flat, because of the huge catalog." | Larger tiered caches and more origin capacity. |
| 6 | "Daily batch is fine." | "Should update within a minute of watching." | Streaming feature pipeline and an online ranker. |
| 7 | "Your numbers are fine." | "Ten times that, with 99.999% availability." | Multi-region active-active control plane and more edge capacity. |
| 8 | "Mostly the same catalog." | "Hundreds of licensing windows per country." | Rules engine for availability, plus region-specific catalog views. |
| 9 | "Fallbacks are fine." | "Never serve an unlicensed stream, even during an outage." | Fail closed, with replicated entitlement data and a stricter availability design. |
| 10 | "Publish when ready." | "Global launch at 00:00 local time in each country." | Pre-warm edge caches, add a scheduled release gate and a thundering-herd plan. |

## 6. Board summary template

**FUNCTIONAL**
- Browse and search the catalog
- Play video with adaptive quality
- Resume watching across devices
- Personalized home page
- Studios or operators ingest and publish titles

**NON-FUNCTIONAL**
- 200M subscribers, 100M daily users, about 10M peak concurrent streams
- Playback start under 2 seconds at p99
- 99.99% availability for playback
- Eventual consistency for resume position (a few seconds of staleness is fine)
- Per-region catalog, with global delivery

**INVARIANT**
- No playback without valid entitlement and license
- Paying users are not wrongly blocked
- Concurrent stream cap is enforced per account

**ASSUMPTIONS**
- VOD only, no live
- No offline downloads, no ads, billing external
- Daily batch recommendations with a popular-titles fallback
- Skewed demand, so popular titles are pre-positioned at the edge
- Titles publish asynchronously when ready
- Standard DRM and adaptive streaming








## 1. Inputs

| Input | Value |
|---|---|
| DAU | 100M |
| Watch time per user | 100 min/day |
| Stream bitrate | 5 Mbps |
| Other API calls | 50 per user per day |
| Content size | 50 GB per title (I treat this as total storage for all renditions of one title) |
| Peak-to-average ratio | 3x (my assumption) |

## 2. Why we need bitrate

Bitrate converts **time watched** into **data moved**. Without it, "100 min per day" tells you nothing about the system. It drives:

- **CDN and network capacity:** the bandwidth we must serve at peak.
- **Cost:** egress is the biggest bill in a video product.
- **Storage per title:** each quality level (240p to 4K) has its own bitrate, so more levels mean more storage.
- **Adaptive streaming design:** the player switches between bitrate levels when the network changes.
- **Segment size:** 5 Mbps × 4 s = 20 Mb ≈ 2.5 MB per video segment.

One note: bitrate is in **bits** per second, while storage is in **bytes**. Divide by 8.

## 3. Estimates

**Watch time and concurrency**
- Total watch time: 100M × 100 min = 10 billion min/day ≈ **167M hours/day**
- Average concurrent streams: 10B min ÷ 1,440 min/day ≈ **7M**
- Peak concurrent streams (3x): ≈ **21M**

**Data per user**
- 5 Mbps × 6,000 s = 30,000 Mb ≈ **3.75 GB per user per day**

**Egress (the big number)**
- Daily: 100M × 3.75 GB ≈ **375 PB/day**
- Average bandwidth: 7M × 5 Mbps ≈ **35 Tbps**
- Peak bandwidth: 21M × 5 Mbps ≈ **105 Tbps**
- Monthly: ≈ **11 EB**

**API traffic (non-video)**
- Daily: 100M × 50 = **5B requests/day**
- Average QPS: 5B ÷ 86,400 ≈ **58K QPS**
- Peak QPS (3x): ≈ **175K QPS**

**Playback-related traffic (hidden but important)**
- Segment requests at 4 s each: 7M streams × 0.25/s ≈ **1.7M req/s average**, about 5M at peak. These go to the CDN, not the app servers.
- Progress heartbeats every 30 s: 7M ÷ 30 ≈ **230K writes/s average**, about 700K at peak. This is the real write load on the backend.

**Storage**
- I assume a catalog of about 15,000 titles. It is not in your inputs, so state it out loud.
- 15,000 × 50 GB ≈ **750 TB** per copy
- With 3x replication at origin: ≈ **2.25 PB**
- CDN copies are extra. Popular titles sit on thousands of edge servers.

## 4. What the numbers tell us

1. **Video delivery dwarfs everything else.** Metadata traffic is about 5B × 10 KB ≈ 50 TB/day, which is roughly 0.01% of 375 PB. So the CDN is the main design problem, and the control plane is the smaller one.
2. **Origin must be shielded.** A catalog of 750 TB is small, but 105 Tbps cannot come from origin. Edge caches must serve over 95% of bytes.
3. **Heartbeats can be the hottest backend write path.** That is why resume position should be async and batched.
4. **Storage is cheap compared to egress.** Extra renditions cost little next to bandwidth.

## 5. Correction to my earlier answer

Earlier I assumed 10M peak concurrent streams. With 100 min per user per day, the average is already 7M, so a peak of about **20M** is more realistic. Use 20M and about 100 Tbps on the board.
