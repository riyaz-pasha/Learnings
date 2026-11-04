# 1. Pattern and main design forks

**Interpretation:** A system that takes live match data from a feed or scorers and shows it to millions of fans within seconds (like ESPN or Cricbuzz live scores).

- **Pattern:** Real-time fan-out, read-heavy broadcast with a feed ingestion front end.
- **Biggest forks:**
  1. Push (persistent connections) vs pull (short-TTL cached polling)
  2. Latest-score snapshot vs ordered append-only event timeline
  3. One trusted provider vs multiple providers needing reconciliation
  4. Uniform load vs a few hot matches taking most traffic
  5. Single region vs multi-region/edge delivery

# 2. Clarifying questions

| # | Bucket | Priority | Question (spoken) | If A, then... | If B, then... | Design decision it drives |
|---|---|---|---|---|---|---|
| 1 | S | P0 | "I'll assume we show live score, match status and a timeline of key events like goals and wickets. No video, no betting, no long commentary. Is the timeline needed, or just the current score?" | Timeline: append-only event log per match, with replay for late joiners. | Score only: overwrite a single latest-state record. | Event log vs snapshot store |
| 2 | S | P1 | "Do users follow teams and get push alerts for goals and results, or only see scores inside the app?" | Alerts: follower lookup plus a notification pipeline. | In-app only: no follower fan-out. | Whether a notification path exists |
| 3 | S | P0 | "Where does the data come from: a third-party feed, or our own scorers entering it?" | Feed: ingestion adapters, dedupe, and a provider-failure story. | Own scorers: a write API with auth, edits and corrections. | Ingestion design and trust model |
| 4 | C | P0 | "The one thing that must never happen: a user sees a wrong or backward score. I'd accept a few seconds of delay, but never out-of-order or retracted-then-forgotten events, such as a goal removed by VAR. Agree?" | Per-match sequence numbers, versioned events, and correction events pushed to everyone. | Best-effort is fine: last write wins, no sequencing. | Ordering and correction model |
| 5 | A | P0 | "I'll assume a few big matches get most of the traffic, say 1,000x a normal match. Is that right?" | Per-match fan-out tier, CDN and cache for hot matches, and isolation so one match can't hurt others. | Even load: one uniform serving path. | Hot-key handling |
| 6 | A | P1 | "Do fans need updates pushed within about 2 to 5 seconds on an open screen, or is polling every 10 to 15 seconds OK?" | Push: connection gateways and pub/sub. | Polling: cheap, cache-friendly CDN reads. | Delivery mechanism |
| 7 | L | P1 | "Let me propose: 50M daily users, peak 5M concurrent at a final, about 2,000 live matches at once, provider to screen under 5s at p99, and 99.99% availability during live events. Reasonable?" | Matches: size for these numbers with edge delivery. | 10x smaller: one region and a simpler stack is enough. | Capacity, connection count, cost |
| 8 | E | P1 | "Is the audience global, mostly mobile, across several continents? Any regional restrictions on showing scores?" | Global: multi-region or edge, with region rules on content. | One country: single region. | Geo-distribution |
| 9 | D | P0 | "If the provider feed lags or dies mid-match, do we show the last known score with a 'delayed' label, or switch to a backup provider?" | Show stale with a label: simple health tracking. | Backup provider: multi-source reconciliation and conflict rules. | Failure handling and source strategy |

**Spoken block 1 (scope and correctness):**
"Let me confirm what we're building. I'll assume live score, status and key events, no video or betting. Do we need a full timeline or just the current score? Do users get push alerts for goals? Is the data from a third-party feed or our own scorers? And the one thing that must not happen is a wrong or out-of-order score. A few seconds of delay is okay. Agree?"

**Spoken block 2 (scale, environment and failure):**
"On load, I'll assume 50M daily users, 5M concurrent at a final, a few hot matches, and updates in under 5 seconds at p99 with 99.99% availability. Is that reasonable? Is the audience global and mostly mobile? And if the provider fails mid-match, do we show the last score as 'delayed', or fail over to a backup?"

# 3. Assumptions to state out loud

- **Auth and security:** Standard auth, TLS and rate limits. If wrong: add anti-scraping and paid API tiers.
- **Sports covered:** Soccer, cricket and basketball, with different event shapes. If wrong: one sport means a simpler event model.
- **Late joiner or reconnect:** Gets the current state plus recent events. If wrong: with no catch-up, reconnects are far simpler.
- **Overload:** We may slow updates (say to 10s) or drop to polling under extreme load. If wrong: we must overprovision or shed other features.
- **History:** Completed matches are archived for stats and later lookup. Not on the live path. If wrong: add analytics and search.
- **Provider update rate:** About one event every few seconds per match. If wrong: sub-second tick data changes the ingest and batching design.
- **Data size:** Small (KBs per match). If wrong: rich stats and tracking data need separate storage.
- **RPO and RTO:** Zero event loss after the provider acknowledges, and recovery in under 1 minute. If wrong: a looser RPO allows simpler, cheaper storage.
- **Cost:** Moderate budget. If wrong: a tight budget favors polling and CDN over persistent connections.

# 4. Questions I deliberately did NOT ask

- **"Which database or queue should we use?"** This is answer-seeking. The candidate must decide.
- **"Should we use WebSockets?"** This is also answer-seeking. I ask about the latency need (Q6) and decide myself.
- **"What does the score card look like? What fields?"** UI and schema detail. It doesn't change the architecture.
- **"Should it be secure and scalable?"** Obvious. These are assumptions, not questions.
- **"Do we support dark mode or multiple languages?"** The same design either way.

# 5. Interviewer cheat sheet

| # | Good answer | Tricky answer | How design changes |
|---|---|---|---|
| 1 | "Timeline needed, with last ~50 events." | "Full ball-by-ball with text commentary, searchable." | Add a text store and search, and paginate history. |
| 2 | "Yes, goal alerts for followed teams." | "Yes, and 20M followers of one team must be alerted within 5s." | Need sharded, batched fan-out and a push-provider rate-limit strategy. |
| 3 | "One licensed provider, push webhooks." | "Three providers with different formats and clocks, plus our own scorers for small leagues." | Normalize into a canonical event, add conflict resolution and source priority. |
| 4 | "Never go backward. Corrections are rare." | "VAR can reverse a goal minutes later, and betting partners consume our data, so no wrong state ever." | Stronger consistency, an explicit retraction event, and an audit trail. |
| 5 | "Top 1% of matches take 80% of traffic." | "All users watch one match, a single global final." | The design is a one-key broadcast problem: heavy CDN, edge fan-out, pre-warmed capacity. |
| 6 | "2 to 5s push is fine." | "Polling is fine for most, but premium users need sub-second." | Two tiers: cached polling for the many, a persistent push path for the few. |
| 7 | "Numbers close to the proposal." | "Peak is 50M concurrent and bursts happen within 30 seconds." | Pre-scaling and autoscaling aren't enough, so we need pre-provisioned capacity and queued connection admission. |
| 8 | "Global, mobile-heavy." | "Some countries must not see certain leagues because of broadcast rights." | A per-region content filter at the edge, not just a global cache. |
| 9 | "Show last score as delayed, alert ops." | "Wrong score for more than 10s is a contract breach. Must auto-fail over." | Run an active/standby or active/active provider feed with health scoring. |

# 6. Board summary template

**FUNCTIONAL**
- Ingest live match events from a provider.
- Show live score and status.
- Show a key-event timeline.
- Push goal and result alerts to followers.
- Late joiners see the current state and recent events.

**NON-FUNCTIONAL**
- 50M DAU, 5M peak concurrent, about 2,000 live matches.
- p99 under 5s from provider to screen.
- 99.99% availability during live events.
- Hot-match skew handled.
- Global audience, mostly mobile.

**INVARIANT**
- Users never see a wrong or out-of-order score. Corrections must propagate. A few seconds of delay is acceptable.

**ASSUMPTIONS**
- Single licensed provider (backup optional).
- Timeline of recent events, history archived off the live path.
- Standard auth and TLS.
- Degrade by slowing updates under overload.
- Provider failure shows the last score labeled "delayed".
