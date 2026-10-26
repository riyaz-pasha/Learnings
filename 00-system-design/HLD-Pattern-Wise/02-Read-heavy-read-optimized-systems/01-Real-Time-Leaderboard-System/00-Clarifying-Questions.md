## 1. Pattern and main design forks

**Interpretation:** A leaderboard for a game or contest. Players earn scores, and everyone sees the top N and their own rank, updated in near real time.

- **Pattern:** Ranking and leaderboard (a sorted aggregate with bursty writes and heavy reads). It is close to feed and analytics.
- **Fork 1:** Exact rank for every player vs exact for the top K only, with approximate rank below that.
- **Fork 2:** One global board vs many boards (per game, region, friends, daily, weekly, all-time).
- **Fork 3:** Score is "best ever" (idempotent) vs "running sum" (needs dedup).
- **Fork 4:** Push updates to viewers vs viewers poll.
- **Fork 5:** Ranking store is a rebuildable derived view vs the source of truth.

## 2. Clarifying questions

| # | Bucket | Priority | Question, as I would say it out loud | If A, then... | If B, then... | Design decision it drives |
|---|---|---|---|---|---|---|
| 1 | S | P0 | "I'll assume a game server submits a score after each match. Users see the top 100 and their own rank with a few neighbors. Is 'my rank' in scope?" | A: Yes, so I need rank lookup for any player. | B: Top N only, so I keep a small bounded top-K structure. | Whether I need a full ranking index |
| 2 | S | P1 | "Is this one global board, or many? For example per game mode, per region, friends only, and daily, weekly, all-time?" | A: One board gives one sorted structure. | B: Many boards means each score fans out to several boards, with partitioning by board ID and reset or rotation jobs. | Partitioning and write fan-out |
| 3 | C | P0 | "Is a player's score the best ever, the latest, or a running sum?" | A: Best or latest is idempotent, so a replay is safe. | B: A running sum needs idempotency keys and dedup, or events get double counted. | Ingest path and dedup |
| 4 | C | P0 | "The one thing that must never go wrong: an acknowledged score is never lost or counted twice. Do you agree? And how do we break ties, for example earliest achiever wins?" | A: Yes, so I need durable ingest before ack. | B: Small loss is OK, so a lighter in-memory path works. | Durability and ack point |
| 5 | A | P0 | "Do we need the exact rank for every player? Or exact for the top 1,000, and an approximate rank or percentile for everyone else?" | A: Exact for all needs a rank-aware structure and sharding with counts. | B: Approximate below the top uses score buckets and counts. | Ranking data structure |
| 6 | L | P1 | "I'll assume 50M daily users, 5M concurrent at peak, and about 50K score updates per second at peak. Reads are about 5x writes. Close?" | A: Yes, so one sharded cluster with caching. | B: 10x bigger, so I need heavy write batching and multi-level caches. | Sharding and capacity |
| 7 | L | P1 | "How real-time is 'real time'? Does a new score appear in under 2 seconds? And do viewers need pushed updates, or is polling every few seconds fine?" | A: Poll and a short cache TTL are enough. | B: Live push needs a pub/sub and connection layer. | Push vs pull, cache design |
| 8 | E | P1 | "Are players global? Do they share one board across regions, or is each region separate?" | A: One global board means a single write home, with regional read replicas. | B: Per-region boards are fully independent. | Multi-region topology |
| 9 | D | P0 | "If the ranking store dies, can we serve a stale board and rebuild from the score log? Or must the ranking store itself be durable?" | A: It is a derived view, so I keep a durable event log and rebuild. | B: It is the source of truth, so I need replication and a strict RPO. | Source of truth, recovery |

**Spoken block 1 (scope and correctness):**
"Let me confirm the basics. A game server submits a score after each match. Users see the top 100 and their own rank. Is that right? Is it one global board, or many, like daily and per region? Is the score best-ever or a running sum? And I'll treat this as the key rule: an acknowledged score is never lost or counted twice. Okay?"

**Spoken block 2 (scale, freshness, failure):**
"For scale, I'll assume 50M daily users and about 50K score updates per second at peak. Does that sound right? Should the top 100 update in under 2 seconds, and is polling fine or do we need push? Do we need exact rank for everyone, or exact for the top 1,000 and approximate beyond? Are players global, with one shared board? And if the ranking store fails, can we serve stale data and rebuild from a log?"

## 3. Assumptions to state out loud

- **Scores come from trusted game servers.** If clients submit scores, I need a validation or anti-cheat layer.
- **Read:write is about 5:1, and the top 100 is the hot read.** If the skew is much higher, I cache the top N more aggressively.
- **Board size is up to about 100M players.** If it is far larger, I need an approximate rank for the lower tiers.
- **History and old seasons are kept for 90 days.** If we need long retention, I add cold storage and a separate history store.
- **Auth and rate limits exist upstream.** If not, I add them at the edge.
- **Players can be removed (cheaters, account deletion) within minutes.** If removal must be instant, I need a delete path through the cache.
- **Budget is moderate and the cloud is managed.** If on-prem or tight, my choices of components change.
- **No special compliance beyond standard privacy rules.** If a region needs data residency, boards must stay in-region.
- **Clients are mobile and web.** Only the push option changes if they are not.

## 4. Questions I deliberately did NOT ask

- **"Which database or Redis?"** This is the candidate's decision (Rule 3).
- **"How many entries do we show, 10 or 100?"** It is the same design either way. I assume 100.
- **"Mobile or web?"** It is the same backend either way. It only affects the push option, which is covered in Q7.
- **"Should it be secure and authenticated?"** It is obvious, so it is an assumption (Rule 4).

## 5. Interviewer cheat sheet

| Q | Good answer | Tricky answer | How the design changes |
|---|---|---|---|
| 1 | Top 100 plus my rank with neighbors | "Show my rank among friends only" | Add a friend-graph lookup and a per-user board |
| 2 | Global plus daily and weekly | "Per guild, per country, per season, all at once" | Heavy fan-out, so partition by board ID and do async fan-out |
| 3 | Best score wins | "Sum of points over a season" | Add idempotency keys and a dedup store |
| 4 | No loss, no double count, earliest wins ties | "Ties share the same rank" | Use dense rank logic instead of unique positions |
| 5 | Exact top 1K, approximate beyond | "Exact for everyone, always" | Need sharded structures with subtree counts and scatter-gather |
| 6 | 50M DAU, 50K writes/s peak | "A viral event gives 10x spikes in 1 minute" | Add a queue to absorb bursts and batch updates |
| 7 | Under 2 seconds, polling is fine | "Live push to 1M viewers" | Add a fan-out and connection layer, and coalesce updates |
| 8 | Global board, single write region | "Active-active in 3 regions" | Need merge of per-region boards, accept eventual consistency |
| 9 | Rebuild from the event log, stale OK | "Zero data loss, no stale reads" | Synchronous replication and a quorum on the ranking store |

## 6. Board summary template

**FUNCTIONAL**
- Submit score after a match
- View the top 100
- View my rank plus neighbors
- Daily, weekly and all-time boards

**NON-FUNCTIONAL**
- 50M DAU, 5M concurrent, 50K writes/s peak, about 5x reads
- Score visible in under 2s, read p99 under 100ms
- 99.95% availability for reads
- Stale reads are OK during failures

**INVARIANT**
- An acknowledged score is never lost or double counted
- Ties are broken deterministically (earliest wins)

**ASSUMPTIONS**
- Trusted game servers submit scores
- Score is best-ever (idempotent)
- Exact rank for the top 1K, approximate beyond
- One global board with regional reads
- Ranking store is a derived view, rebuilt from a durable event log
- Auth and rate limiting are handled upstream
- Anti-cheat is out of scope
