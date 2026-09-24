/*
 * ============================================================
 *  GOOGLE MOCK INTERVIEW — Real-Time Player Matchmaking
 *  Candidate: [Your Name]
 *  Interviewer: Senior SWE, Google
 *  Language: Java 17
 * ============================================================
 *
 *  HOW TO READ THIS FILE
 *  ─────────────────────
 *  Each numbered section maps exactly to a step in the
 *  interview rubric. Read top-to-bottom, as you would
 *  present this live. Code in Sections 4 and 6 is
 *  fully compilable; a main() at the bottom drives
 *  the dry-run trace from Section 7.
 * ============================================================
 */

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.TreeMap;

class PlayerMatchmaking {

    /* ══════════════════════════════════════════════════════════════
     * SECTION 1 — RESTATE THE PROBLEM
     * ══════════════════════════════════════════════════════════════
     *
     *  "Let me restate the problem to make sure I understand it."
     *
     *  We are building the backend for a matchmaking queue.
     *  Players arrive one at a time and each carries a unique
     *  integer ID and an integer skill rating.
     *
     *  When player P joins, we search the waiting queue for the
     *  best opponent using a three-level priority:
     *
     *    Priority 1 (hard gate) — The candidate's rating must
     *      satisfy |candidate.rating − P.rating| ≤ 50.
     *
     *    Priority 2 (primary tiebreak) — Among all candidates
     *      that pass the gate, prefer the one whose rating is
     *      numerically closest to P.rating.
     *
     *    Priority 3 (secondary tiebreak) — If two candidates
     *      are equally close in rating, prefer the one who has
     *      been waiting longer (FIFO order within equal distance).
     *
     *  If a valid opponent is found, both players are immediately
     *  removed from the queue and we return the matched pair.
     *  If no one is within ±50, player P is added to the queue
     *  and we return an empty result.
     *
     *  Key constraints called out explicitly:
     *    • Each player ID is unique.
     *    • "Closest rating" means minimum absolute difference.
     *    • Ties on distance break by queue arrival order (oldest first).
     *    • Once matched, a player cannot appear in any future match.
     *    • The queue is persistent — players accumulate across calls.
     *    • We are NOT told the total number of players in advance.
     * ══════════════════════════════════════════════════════════════
     */


    /* ══════════════════════════════════════════════════════════════
     * SECTION 2 — CLARIFYING QUESTIONS & ASSUMED ANSWERS
     * ══════════════════════════════════════════════════════════════
     *
     *  Q1. What is the expected number of players concurrently
     *      in the queue? Could it reach millions?
     *      → Assume up to ~10 000 players at peak. Single-threaded
     *        for now; concurrency discussed in follow-ups.
     *
     *  Q2. Are skill ratings guaranteed to be integers, and is
     *      there a bounded range (e.g., 0–10 000 like many games)?
     *      → Yes, integers. Range is 0–10 000 inclusive.
     *
     *  Q3. Can two players share the same skill rating?
     *      → Yes — duplicate ratings are allowed. IDs remain unique.
     *
     *  Q4. What should we return from a match call?
     *       (a) a Pair/Match object, (b) just the opponent's ID,
     *       (c) void and mutate some external list?
     *      → Return a MatchResult containing both player IDs, or
     *        null/empty-Optional if no match is found.
     *
     *  Q5. Is the ±50 boundary inclusive? i.e., is a difference
     *      of exactly 50 valid?
     *      → Yes, inclusive (|diff| ≤ 50).
     *
     *  Q6. Can the same player call joinQueue twice (reconnect
     *      after disconnect)?
     *      → No; assume each ID appears at most once. If needed,
     *        the caller deduplicates before calling.
     *
     *  Q7. Should we support removing a player who cancels before
     *      being matched (voluntary leave)?
     *      → Yes — treat it as a natural extension, but the core
     *        API is just joinQueue(player). We'll architect for it.
     *
     *  Q8. Rating changes while in queue — can a player's rating
     *      change after they join but before they are matched?
     *      → Not in v1. Ratings are immutable once queued.
     *        Discussed in follow-ups.
     * ══════════════════════════════════════════════════════════════
     */


    /* ══════════════════════════════════════════════════════════════
     * SECTION 3 — EXAMPLES & EDGE CASES
     * ══════════════════════════════════════════════════════════════
     *
     *  ── Example 1: Normal case, single clear winner ──
     *
     *  Queue state before new arrival:
     *    Player A (id=1, rating=200) — arrived first
     *    Player B (id=2, rating=230) — arrived second
     *    Player C (id=3, rating=280) — arrived third
     *
     *  New player D (id=4, rating=220) joins.
     *  Window of validity: [170, 270]
     *    A → |200−220| = 20  ✓
     *    B → |230−220| = 10  ✓ ← closest
     *    C → |280−220| = 60  ✗ (outside ±50)
     *
     *  Match: D vs B. Both removed. A and C remain queued.
     *
     *  ── Example 2: Tie-breaking by wait time ──
     *
     *  Queue state:
     *    Player A (id=1, rating=200, joined at t=1)
     *    Player B (id=2, rating=240, joined at t=2)
     *
     *  New player C (id=3, rating=220) joins.
     *  Window: [170, 270]
     *    A → |200−220| = 20  ✓
     *    B → |240−220| = 20  ✓
     *  Both equal distance. A waited longer (t=1 < t=2).
     *  Match: C vs A.
     *
     *  ── Example 3 (Edge Case): No match found ──
     *
     *  Queue state:
     *    Player A (id=1, rating=100)
     *    Player B (id=2, rating=500)
     *
     *  New player C (id=3, rating=300) joins.
     *  Window: [250, 350]
     *    A → |100−300| = 200  ✗
     *    B → |500−300| = 200  ✗
     *  No valid opponent. C is added to queue.
     *  Queue now: A, B, C.
     *
     *  ── Edge Case: Empty queue ──
     *  Player A joins an empty queue → no candidates at all →
     *  A is enqueued, return empty result. (Trivial but must handle.)
     *
     *  ── Edge Case: Candidate at exactly ±50 boundary ──
     *  New player rating = 300. Candidate rating = 350.
     *  |350−300| = 50 → valid (inclusive boundary). Must not exclude.
     * ══════════════════════════════════════════════════════════════
     */


    /* ══════════════════════════════════════════════════════════════
     * SECTION 4 — BRUTE FORCE / NAIVE SOLUTION
     * ══════════════════════════════════════════════════════════════
     *
     *  Plain-English Description
     *  ─────────────────────────
     *  Maintain a simple ArrayList of queued players, in insertion
     *  order. When a new player P arrives, do a linear scan over
     *  every queued player, applying the three-level priority rule
     *  manually to track the best candidate. If found, remove the
     *  winner from the list.
     *
     *  Time Complexity:
     *    joinQueue → O(n) for the linear scan (n = queue size).
     *    Removal from ArrayList is O(n) in the worst case (shifting).
     *    Overall per call: O(n).
     *
     *  Space Complexity:
     *    O(n) for the list.
     *
     *  Why it's insufficient:
     *    With 10 000 players and high call frequency this is
     *    10 000 iterations per join. At Google scale (millions of
     *    joins per minute) this is untenable.
     * ══════════════════════════════════════════════════════════════
     */

    // ── Shared data model used by both solutions ──────────────────

    /**
     * Immutable value object representing a player.
     * arrivalOrder is set once by the matchmaking system,
     * never by the caller.
     */
    static class Player {
        final int id;
        final int rating;
        final long arrivalOrder; // monotonically increasing, set by system

        Player(int id, int rating, long arrivalOrder) {
            this.id = id;
            this.rating = rating;
            this.arrivalOrder = arrivalOrder;
        }

        @Override public String toString() {
            return String.format("Player{id=%d, rating=%d, arrival=%d}",
                    id, rating, arrivalOrder);
        }
    }

    /**
     * Return type for a successful match.
     * empty() signals "no match found".
     */
    static class MatchResult {
        final int playerOneId;
        final int playerTwoId;
        final boolean matched;

        private MatchResult(int p1, int p2) {
            this.playerOneId = p1;
            this.playerTwoId = p2;
            this.matched = true;
        }

        private MatchResult() {
            this.playerOneId = -1;
            this.playerTwoId = -1;
            this.matched = false;
        }

        static MatchResult of(int p1, int p2) { return new MatchResult(p1, p2); }
        static MatchResult empty()             { return new MatchResult(); }

        @Override public String toString() {
            return matched
                    ? String.format("MATCH  → Player %d  vs  Player %d", playerOneId, playerTwoId)
                    : "NO MATCH — player added to queue";
        }
    }

    // ── Brute Force Implementation ────────────────────────────────

    static class BruteForceMatchmaker {

        private final List<Player> queue = new ArrayList<>();
        private long clock = 0; // arrival-order counter

        static final int RATING_WINDOW = 50;

        /**
         * Attempts to match newPlayer against the current queue.
         *
         * Time:  O(n) scan + O(n) removal = O(n)
         * Space: O(1) extra beyond the list itself
         */
        MatchResult joinQueue(int id, int rating) {
            Player newPlayer = new Player(id, rating, clock++);

            Player bestOpponent = null;
            int    bestDistance = Integer.MAX_VALUE;

            // Linear scan — evaluate every waiting player
            for (Player candidate : queue) {
                int distance = Math.abs(candidate.rating - newPlayer.rating);

                // Hard gate: must be within ±50
                if (distance > RATING_WINDOW) continue;

                // Primary: prefer smaller distance
                if (distance < bestDistance) {
                    bestOpponent = candidate;
                    bestDistance = distance;
                }
                // Secondary: on equal distance, prefer earlier arrival
                else if (distance == bestDistance
                        && candidate.arrivalOrder < bestOpponent.arrivalOrder) {
                    bestOpponent = candidate;
                }
            }

            if (bestOpponent == null) {
                // No valid match — enqueue the new player
                queue.add(newPlayer);
                return MatchResult.empty();
            }

            // Found a match — remove opponent from queue
            queue.remove(bestOpponent); // O(n) shift, acceptable in brute force
            return MatchResult.of(newPlayer.id, bestOpponent.id);
        }
    }


    /* ══════════════════════════════════════════════════════════════
     * SECTION 5 — OPTIMIZATION BRAINSTORM
     * ══════════════════════════════════════════════════════════════
     *
     *  "Let me think out loud about why brute force falls short
     *   and what structure exploits the problem's geometry."
     *
     *  The bottleneck is: given a target rating R, find the
     *  candidate in [R−50, R+50] that is closest to R, breaking
     *  ties by arrival time.
     *
     *  This is a SORTED NEAREST-NEIGHBOR + RANGE QUERY problem.
     *  That's the exact pattern where TreeMap shines:
     *
     *    • Keys   = rating (Integer, sorted automatically by Red-Black tree)
     *    • Values = a queue of players at that rating, ordered by arrival
     *
     *  Why Queue per rating?
     *    Multiple players can share the same rating. The Queue
     *    preserves insertion order, giving us FIFO for Priority 3
     *    for free.
     *
     *  Key TreeMap operations we will use:
     *    floorKey(R)   → largest rating ≤ R  (closest candidate from below)
     *    ceilingKey(R) → smallest rating ≥ R (closest candidate from above)
     *
     *  After identifying the best rating bucket, we peek the head
     *  of its Queue (O(1)) to get the longest-waiting player at
     *  that rating.
     *
     *  Removal (cancellation / post-match cleanup):
     *    We also maintain a HashMap<Integer, Player> indexed by
     *    player ID. This gives O(1) lookup when we need to remove
     *    a specific player by ID (e.g., if a player cancels or
     *    disconnects). Without this index, we'd need another O(log n)
     *    lookup into the TreeMap plus O(k) scan of the bucket.
     *
     *  Why not PriorityQueue?
     *    PriorityQueue has O(n) remove and no efficient range
     *    query. It could find the global minimum but not the
     *    minimum within [R−50, R+50].
     *
     *  Complexity of optimal solution:
     *    joinQueue  → O(log n)  (two floorKey/ceilingKey calls)
     *    removeById → O(log n)  (HashMap lookup + TreeMap remove)
     *    Space      → O(n)
     *
     *  This is a ×n speedup over brute force.
     * ══════════════════════════════════════════════════════════════
     */


    /* ══════════════════════════════════════════════════════════════
     * SECTION 6 — OPTIMAL SOLUTION
     * ══════════════════════════════════════════════════════════════
     *
     *  Data structures:
     *
     *    TreeMap<Integer, Queue<Player>> ratingBuckets
     *      key   = skill rating
     *      value = FIFO queue of players at that rating
     *              (Queue preserves arrival order → Priority 3 for free)
     *
     *    HashMap<Integer, Player> playerIndex
     *      key   = player ID
     *      value = Player object
     *      purpose: O(1) ID-based lookup for cancellation / eviction
     *
     *    long clock  — monotonically increments; assigned as arrivalOrder
     *
     *  Time Complexity:
     *    joinQueue  → O(log n) — two TreeMap navigations + constant work
     *    removeById → O(log n) — HashMap get + TreeMap remove
     *    Space      → O(n)    — linear in queue size
     * ══════════════════════════════════════════════════════════════
     */

    static class OptimalMatchmaker {

        // Sorted by rating; each bucket is a FIFO queue for tie-breaking
        private final TreeMap<Integer, Queue<Player>> ratingBuckets = new TreeMap<>();

        // O(1) player lookup for voluntary removal (cancel / disconnect)
        private final HashMap<Integer, Player> playerIndex = new HashMap<>();

        private long clock = 0;

        static final int RATING_WINDOW = 50;

        // ── Public API ────────────────────────────────────────────

        /**
         * Main entry point.
         * Returns a MatchResult with both IDs if a match is found,
         * or MatchResult.empty() if the new player is added to queue.
         *
         * Time: O(log n)
         */
        public MatchResult joinQueue(int id, int rating) {
            Player newPlayer = new Player(id, rating, clock++);

            Player bestOpponent = findBestOpponent(newPlayer);

            if (bestOpponent == null) {
                // No match — park this player in the queue
                enqueue(newPlayer);
                return MatchResult.empty();
            }

            // Valid match found — evict the opponent and return
            removeFromQueue(bestOpponent);
            return MatchResult.of(newPlayer.id, bestOpponent.id);
        }

        /**
         * Voluntary removal — player cancelled while waiting.
         * Returns true if the player was found and removed.
         *
         * Time: O(log n)
         */
        public boolean cancelQueue(int playerId) {
            Player player = playerIndex.get(playerId);
            if (player == null) return false; // not in queue
            removeFromQueue(player);
            return true;
        }

        // ── Core logic ────────────────────────────────────────────

        /**
         * Finds the best opponent in [newPlayer.rating - 50,
         *                              newPlayer.rating + 50]
         * using floorKey / ceilingKey to zero in from both sides.
         *
         * The winner is chosen by:
         *   1. Minimum |rating difference| (Primary)
         *   2. Earliest arrivalOrder among ties (Secondary)
         */
        private Player findBestOpponent(Player newPlayer) {
            int lo = newPlayer.rating - RATING_WINDOW; // inclusive lower bound
            int hi = newPlayer.rating + RATING_WINDOW; // inclusive upper bound

            // Candidate from the lower half: largest rating ≤ newPlayer.rating
            Player lowerCandidate  = peekBestInDirection(newPlayer.rating,
                    ratingBuckets.floorKey(newPlayer.rating),   lo, true);

            // Candidate from the upper half: smallest rating ≥ newPlayer.rating
            Player upperCandidate  = peekBestInDirection(newPlayer.rating,
                    ratingBuckets.ceilingKey(newPlayer.rating), hi, false);

            return chooseBetter(newPlayer.rating, lowerCandidate, upperCandidate);
        }

        /**
         * Given a starting key (which may be null), finds the head of
         * the nearest bucket that is still within the window boundary.
         *
         * @param targetRating  the incoming player's rating
         * @param startKey      floorKey or ceilingKey result (may be null)
         * @param windowBound   lo or hi of the ±50 window
         * @param searchBelow   true = walking downward (floor side),
         *                      false = walking upward (ceiling side)
         *
         * We only need to look at the single nearest key in each direction
         * because the TreeMap is sorted — the first key encountered in
         * either direction is always the closest in that half.
         */
        private Player peekBestInDirection(int targetRating,
                                           Integer startKey,
                                           int windowBound,
                                           boolean searchBelow) {
            if (startKey == null) return null;

            // Check if this key is within the window
            if (searchBelow && startKey < windowBound) return null;  // too far below
            if (!searchBelow && startKey > windowBound) return null; // too far above

            Queue<Player> bucket = ratingBuckets.get(startKey);
            // Bucket should never be empty (we clean up empties on removal),
            // but guard defensively.
            return (bucket == null || bucket.isEmpty()) ? null : bucket.peek();
        }

        /**
         * Applies the three-level priority rule to choose between
         * the best candidate from the lower half and the upper half.
         *
         * Priority 1: filter by window (already done in peekBestInDirection)
         * Priority 2: prefer smaller |rating - targetRating|
         * Priority 3: on tie, prefer smaller arrivalOrder (earlier arrival)
         */
        private Player chooseBetter(int targetRating, Player lower, Player upper) {
            if (lower == null) return upper;
            if (upper == null) return lower;

            int distLower = Math.abs(lower.rating - targetRating);
            int distUpper = Math.abs(upper.rating - targetRating);

            if (distLower != distUpper) {
                // Clear winner on distance
                return distLower < distUpper ? lower : upper;
            }

            // Equal distance — FIFO: pick the one who arrived first
            return lower.arrivalOrder < upper.arrivalOrder ? lower : upper;
        }

        // ── Queue management helpers ──────────────────────────────

        /**
         * Adds a player to ratingBuckets and playerIndex.
         * Creates the bucket if this is the first player at that rating.
         */
        private void enqueue(Player player) {
            ratingBuckets
                    .computeIfAbsent(player.rating, k -> new LinkedList<>())
                    .offer(player);                   // offer = enqueue at tail, O(1)
            playerIndex.put(player.id, player);
        }

        /**
         * Removes a specific player from ratingBuckets and playerIndex.
         *
         * Because we always pick from the HEAD of the bucket (via peek),
         * removal of the matched player is Queue.poll() — O(1).
         *
         * For cancellation (mid-queue removal), we use Queue.remove(player)
         * which is O(k) where k is the bucket size (players at same rating).
         * k is bounded and typically tiny, so this is effectively O(1) in
         * practice. We could make it strict O(1) with a LinkedHashSet but
         * at the cost of extra complexity — not worth it here.
         */
        private void removeFromQueue(Player player) {
            Queue<Player> bucket = ratingBuckets.get(player.rating);
            if (bucket == null) return;

            // If this player is at the head (the common matched-player case),
            // poll() is O(1). Otherwise remove(player) walks the bucket.
            if (bucket.peek() == player) {
                bucket.poll();
            } else {
                bucket.remove(player); // identity comparison (reference equality)
            }

            // Prune the bucket if it's now empty to keep TreeMap clean
            if (bucket.isEmpty()) {
                ratingBuckets.remove(player.rating);
            }

            playerIndex.remove(player.id);
        }

        // ── Diagnostic helper (for dry-run / testing) ─────────────

        /** Returns a snapshot of queued (rating, playerId) pairs for debugging. */
        String queueSnapshot() {
            StringBuilder sb = new StringBuilder("Queue: [");
            ratingBuckets.forEach((rating, bucket) ->
                    bucket.forEach(p ->
                            sb.append(String.format("(id=%d,r=%d,t=%d) ",
                                    p.id, p.rating, p.arrivalOrder))));
            sb.append("]");
            return sb.toString();
        }
    }


    /* ══════════════════════════════════════════════════════════════
     * SECTION 7 — DRY RUN / TRACE
     * ══════════════════════════════════════════════════════════════
     *
     *  We trace the tie-breaking example from Section 3 step by step.
     *
     *  Initial state: queue is empty, clock = 0
     *
     *  ── Step 1: Player A (id=1, rating=200) joins ──────────────
     *  findBestOpponent:
     *    lo = 150, hi = 250
     *    floorKey(200) = null  (tree is empty) → lowerCandidate = null
     *    ceilingKey(200) = null                 → upperCandidate = null
     *    chooseBetter(null, null) → null
     *  No match. Enqueue A.
     *  ratingBuckets: {200 → [A(t=0)]}
     *  playerIndex:   {1 → A}
     *  clock: 1
     *
     *  ── Step 2: Player B (id=2, rating=240) joins ──────────────
     *  findBestOpponent:
     *    lo = 190, hi = 290
     *    floorKey(240) = 200  → 200 ≥ 190 ✓ → lowerCandidate = A(t=0)
     *    ceilingKey(240) = 200 → 200 ≤ 290 ✓ → upperCandidate = A(t=0)
     *      (same bucket! both floor and ceiling hit 200 since it's the only key)
     *    chooseBetter(240, A, A):
     *      distLower = |200−240| = 40
     *      distUpper = |200−240| = 40 (same player)
     *      equal → arrivalOrder tie → returns A (t=0)
     *
     *  Wait — should we match B with A here? Let's check: |240−200|=40 ≤ 50. YES.
     *  Match: B (id=2) vs A (id=1). Both removed.
     *  Result: MATCH → Player 2 vs Player 1
     *  ratingBuckets: {}
     *  playerIndex:   {}
     *  clock: 2
     *
     *  (To demonstrate the tie-break scenario properly, we reset and
     *   replay with two opponents that are equidistant — see main().)
     *
     *  ── Tie-break replay (see main() output) ───────────────────
     *  Queue: A(r=200, t=0), B(r=240, t=1)
     *  New: C(r=220)
     *  lo=170, hi=270
     *  floorKey(220)  = 200 → 200 ≥ 170 ✓ → lowerCandidate = A(t=0), dist=20
     *  ceilingKey(220)= 240 → 240 ≤ 270 ✓ → upperCandidate = B(t=1), dist=20
     *  chooseBetter: distLower==distUpper==20 → compare arrivals: 0 < 1 → A wins
     *  Match: C vs A.
     *  ratingBuckets after: {240 → [B]}
     * ══════════════════════════════════════════════════════════════
     */


    /* ══════════════════════════════════════════════════════════════
     * SECTION 8 — CLOSING SUMMARY & TRADE-OFFS
     * ══════════════════════════════════════════════════════════════
     *
     *  Approach Comparison
     *  ─────────────────────────────────────────────────────────────
     *  | Approach          | joinQueue    | Space | When to choose     |
     *  |-------------------|--------------|-------|--------------------|
     *  | Brute Force List  | O(n)         | O(n)  | n < 100, prototype |
     *  | Optimal TreeMap   | O(log n)     | O(n)  | Production, n≥1k   |
     *
     *  Key Design Decisions
     *  ─────────────────────────────────────────────────────────────
     *  • Queue<Player> per bucket — gives O(1) FIFO tie-breaking
     *    without a secondary sort; the natural arrival order of
     *    offer() is the arrival order of the players.
     *
     *  • HashMap playerIndex — enables O(1) player lookup for
     *    cancellation, critical in production systems where
     *    disconnect events are common.
     *
     *  • Pruning empty buckets — keeps TreeMap size == unique rating
     *    count (≤ n), preventing memory drift and keeping navigation
     *    fast.
     *
     *  Limitations of the Current Solution
     *  ─────────────────────────────────────────────────────────────
     *  1. Not thread-safe. In a real matchmaking server, multiple
     *     goroutines/threads call joinQueue concurrently, creating
     *     TOCTOU races on the TreeMap. Solution: wrap in
     *     ReentrantReadWriteLock or use ConcurrentSkipListMap.
     *
     *  2. Fixed ±50 window. Real games use dynamic windows
     *     (expand the window if a player waits > 30 s).
     *
     *  3. Rating immutability. If ratings change (e.g., from an
     *     ongoing match finishing), the wrong bucket holds the player.
     *     Solution: atomic remove-update-reinsert.
     *
     *  4. No persistence. If the server restarts, the queue is lost.
     *     Production systems use Redis Sorted Sets (ZRANGEBYSCORE)
     *     which mirrors the TreeMap semantics across processes.
     * ══════════════════════════════════════════════════════════════
     */


    /* ══════════════════════════════════════════════════════════════
     * SECTION 9 — FOLLOW-UP QUESTIONS
     * ══════════════════════════════════════════════════════════════
     *
     *  FU-1. CONCURRENCY
     *    "How would you make joinQueue thread-safe for a server
     *     handling 10 000 concurrent connections?"
     *    Answer sketch: Replace TreeMap with ConcurrentSkipListMap
     *    (same O(log n) ops, lock-free reads). Guard critical sections
     *    with a ReentrantLock or use optimistic CAS on the bucket.
     *    Alternatively, shard by rating range and have per-shard locks
     *    to reduce contention.
     *
     *  FU-2. DYNAMIC WINDOW EXPANSION
     *    "A player has been waiting 60 seconds with no match.
     *     How do you widen the window to ±100?"
     *    Answer sketch: Add a background thread or scheduled task
     *    that iterates long-waiting players (track via a separate
     *    FIFO wait-list), expands their search window, and re-runs
     *    findBestOpponent. Window stored per-player, not global.
     *
     *  FU-3. MILLIONS OF PLAYERS
     *    "Scale to 10 million concurrent players."
     *    Answer sketch: Horizontal sharding by rating range (e.g.,
     *    0–2000 on shard A, 2001–4000 on shard B). Cross-shard
     *    matching at boundaries handled by a routing layer.
     *    Alternatively use Redis ZRANGEBYSCORE for distributed
     *    sorted-set semantics without local in-process state.
     *
     *  FU-4. RATING CHANGES MID-QUEUE
     *    "A player's rating changes because a previous match
     *     result is finalized while they're in the queue."
     *    Answer sketch: Treat as an atomic remove + re-insert.
     *    cancelQueue(id) then joinQueue(id, newRating). Since
     *    both ops are O(log n), the overhead is minimal.
     *    Need care around ABA problem in concurrent setting.
     *
     *  FU-5. RANKED TIERS (e.g., Bronze / Silver / Gold)
     *    "Players should only match within their tier."
     *    Answer sketch: Maintain one OptimalMatchmaker instance
     *    per tier. joinQueue first routes to the correct instance
     *    based on the tier enum. Cross-tier matching can be a
     *    scheduled promotion if intra-tier queue is starved.
     *
     *  FU-6. FAIRNESS & STARVATION
     *    "Could a player wait forever if their rating is unique
     *     (e.g., no one within ±50 ever joins)?"
     *    Answer sketch: Yes — starvation is possible. Mitigation:
     *    exponential window expansion (cap at ±200), or a priority
     *    queue sorted by wait time so the matchmaker actively
     *    prefers long-waiters even at the cost of rating accuracy.
     * ══════════════════════════════════════════════════════════════
     */


    /* ══════════════════════════════════════════════════════════════
     * SECTION 10 — WHAT CANDIDATES TYPICALLY MISS
     * ══════════════════════════════════════════════════════════════
     *
     *  TRAP 1 — Off-by-one on the boundary (most common mistake)
     *    Candidates write `distance < 50` instead of `<= 50`,
     *    silently excluding valid opponents at exactly ±50.
     *    Always confirm with the interviewer: "Is the boundary
     *    inclusive?" and then encode it as a named constant with
     *    a comment. Never magic-number the check.
     *
     *  TRAP 2 — Forgetting to remove the matched player from
     *    ALL data structures.
     *    After a match, candidates remove from the TreeMap bucket
     *    but forget to remove from playerIndex (or vice versa).
     *    This causes phantom evictions or memory leaks. Any time
     *    you have parallel data structures, write one removeFromQueue
     *    helper that touches ALL of them atomically.
     *
     *  TRAP 3 — Mishandling the tie-breaking rule.
     *    Many candidates pick the head of the bucket at the nearest
     *    rating, which is correct for single-rating ties. But they
     *    miss the cross-bucket tie: if floorKey gives distance=20
     *    AND ceilingKey gives distance=20, you must compare the
     *    heads of BOTH buckets by arrivalOrder. Skipping chooseBetter
     *    and just returning the floor winner violates Priority 3.
     *
     *  TRAP 4 — Not pruning empty buckets from the TreeMap.
     *    After all players at a given rating leave, the bucket is an
     *    empty Queue. floorKey / ceilingKey will still return that
     *    key, leading peekBestInDirection to return null from an
     *    existing bucket — a silent correctness bug that is almost
     *    impossible to spot in a quick code review.
     *    Fix: always call ratingBuckets.remove(rating) when a
     *    bucket becomes empty.
     * ══════════════════════════════════════════════════════════════
     */


    // ════════════════════════════════════════════════════════════════
    //  SECTION 7 LIVE TRACE — main() drives the dry-run scenarios
    // ════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("════════════════════════════════════════════");
        System.out.println("  DRY RUN — Optimal Matchmaker Trace");
        System.out.println("════════════════════════════════════════════\n");

        OptimalMatchmaker mm = new OptimalMatchmaker();

        System.out.println("── Scenario 1: Normal case (clear winner) ──");
        // A(200), B(230), C(280) already in queue, D(220) joins
        System.out.println(mm.queueSnapshot()); // empty initially
        System.out.println("join A(id=1, r=200) → " + mm.joinQueue(1, 200));
        System.out.println(mm.queueSnapshot());
        System.out.println("join B(id=2, r=230) → " + mm.joinQueue(2, 230));
        System.out.println(mm.queueSnapshot());
        System.out.println("join C(id=3, r=280) → " + mm.joinQueue(3, 280));
        System.out.println(mm.queueSnapshot());
        System.out.println("join D(id=4, r=220) → " + mm.joinQueue(4, 220));
        // Expected: D matches B (dist=10), not A (dist=20), not C (outside window)
        System.out.println(mm.queueSnapshot()); // A and C remain

        System.out.println("\n── Scenario 2: Tie-breaking by arrival time ──");
        OptimalMatchmaker mm2 = new OptimalMatchmaker();
        System.out.println("join A(id=1, r=200) → " + mm2.joinQueue(1, 200));
        System.out.println("join B(id=2, r=240) → " + mm2.joinQueue(2, 240));
        System.out.println(mm2.queueSnapshot());
        System.out.println("join C(id=3, r=220) → " + mm2.joinQueue(3, 220));
        // Expected: C matches A — both equidistant (dist=20 each),
        //           A arrived first (arrivalOrder 0 < 1)
        System.out.println(mm2.queueSnapshot()); // B(240) remains

        System.out.println("\n── Scenario 3: No match, then eventual match ──");
        OptimalMatchmaker mm3 = new OptimalMatchmaker();
        System.out.println("join A(id=1, r=100) → " + mm3.joinQueue(1, 100));
        System.out.println("join B(id=2, r=500) → " + mm3.joinQueue(2, 500));
        System.out.println("join C(id=3, r=300) → " + mm3.joinQueue(3, 300));
        // C is 200 away from both — no match, gets enqueued
        System.out.println(mm3.queueSnapshot());
        System.out.println("join D(id=4, r=320) → " + mm3.joinQueue(4, 320));
        // D is 20 away from C — valid match!
        System.out.println(mm3.queueSnapshot()); // A and B remain

        System.out.println("\n── Scenario 4: Cancellation then join ──");
        OptimalMatchmaker mm4 = new OptimalMatchmaker();
        mm4.joinQueue(1, 500);
        mm4.joinQueue(2, 510);
        System.out.println("Before cancel: " + mm4.queueSnapshot());
        System.out.println("cancel player 1 → " + mm4.cancelQueue(1));
        System.out.println("After cancel:  " + mm4.queueSnapshot());
        System.out.println("join P3(id=3, r=505) → " + mm4.joinQueue(3, 505));
        // Only player 2 (r=510) remains — dist=5, valid match
        System.out.println(mm4.queueSnapshot());

        System.out.println("\n════════════════════════════════════════════");
        System.out.println("  All scenarios completed.");
        System.out.println("════════════════════════════════════════════");
    }
}



class MatchmakingSystem {

    class Player {
        int id;
        int rating;
        long timestamp;
    
        public Player(int id, int rating, long timestamp) {
            this.id = id;
            this.rating = rating;
            this.timestamp = timestamp;
        }
    }

    private TreeMap<Integer, Queue<Player>> map = new TreeMap<>();
    private static final int RANGE = 50;

    public Player match(Player newPlayer) {
        int rating = newPlayer.rating;

        Integer lower = map.floorKey(rating);
        Integer higher = map.ceilingKey(rating);

        Player bestMatch = null;
        Integer bestKey = null;

        // Check lower candidate
        if (lower != null && Math.abs(lower - rating) <= RANGE) {
            bestMatch = map.get(lower).peek();
            bestKey = lower;
        }

        // Check higher candidate
        if (higher != null && Math.abs(higher - rating) <= RANGE) {
            Player higherPlayer = map.get(higher).peek();

            if (bestMatch == null ||
                Math.abs(higher - rating) < Math.abs(bestKey - rating) ||
                (Math.abs(higher - rating) == Math.abs(bestKey - rating)
                 && higherPlayer.timestamp < bestMatch.timestamp)) {

                bestMatch = higherPlayer;
                bestKey = higher;
            }
        }

        if (bestMatch != null) {
            Queue<Player> queue = map.get(bestKey);
            queue.poll();
            if (queue.isEmpty()) {
                map.remove(bestKey);
            }
            return bestMatch;
        }

        // No match → add player
        map.computeIfAbsent(rating, k -> new LinkedList<>()).offer(newPlayer);
        return null;
    }
}


/**
 * Represents a player in the matchmaking system.
 * Using record (Java 16+) for immutability and cleaner syntax.
 */
record Player(int id, int rating, long timestamp) {}

/**
 * Real-time matchmaking system:
 * - Matches players within ±RANGE rating
 * - Picks closest rating
 * - If tie → picks earliest (FIFO via timestamp)
 */
class MatchmakingSystem2 {

    private static final int RANGE = 50;

    /**
     * Key: rating
     * Value: queue of players with same rating (FIFO for fairness)
     */
    private final NavigableMap<Integer, Deque<Player>> playersByRating = new TreeMap<>();

    /**
     * Attempts to find a match for the incoming player.
     *
     * @param incoming new player joining matchmaking
     * @return matched opponent OR null if no match found
     */
    public Player match(Player incoming) {
        int rating = incoming.rating();

        // Find closest lower and higher rating buckets
        Integer lowerKey = playersByRating.floorKey(rating);
        Integer higherKey = playersByRating.ceilingKey(rating);

        MatchCandidate best = chooseBestCandidate(rating, lowerKey, higherKey);

        if (best != null) {
            return removeAndReturn(best);
        }

        // No match → add player to waiting pool
        addPlayer(incoming);
        return null;
    }

    /**
     * Encapsulates candidate selection logic.
     */
    private MatchCandidate chooseBestCandidate(int rating, Integer lowerKey, Integer higherKey) {
        MatchCandidate lowerCandidate = buildCandidate(rating, lowerKey);
        MatchCandidate higherCandidate = buildCandidate(rating, higherKey);

        if (lowerCandidate == null) return higherCandidate;
        if (higherCandidate == null) return lowerCandidate;

        // Compare by:
        // 1. Smaller rating difference
        // 2. Earlier timestamp (FIFO fairness)
        if (lowerCandidate.diff < higherCandidate.diff) return lowerCandidate;
        if (higherCandidate.diff < lowerCandidate.diff) return higherCandidate;

        return lowerCandidate.player.timestamp() <= higherCandidate.player.timestamp()
                ? lowerCandidate
                : higherCandidate;
    }

    /**
     * Builds a candidate if within valid range.
     */
    private MatchCandidate buildCandidate(int rating, Integer key) {
        if (key == null) return null;

        int diff = Math.abs(key - rating);
        if (diff > RANGE) return null;

        Deque<Player> queue = playersByRating.get(key);
        if (queue == null || queue.isEmpty()) return null;

        return new MatchCandidate(key, queue.peekFirst(), diff);
    }

    /**
     * Removes matched player from data structure.
     */
    private Player removeAndReturn(MatchCandidate candidate) {
        Deque<Player> queue = playersByRating.get(candidate.ratingKey);
        Player matched = queue.pollFirst();

        // Clean up empty buckets
        if (queue.isEmpty()) {
            playersByRating.remove(candidate.ratingKey);
        }

        return matched;
    }

    /**
     * Adds player to waiting pool.
     */
    private void addPlayer(Player player) {
        playersByRating
                .computeIfAbsent(player.rating(), r -> new ArrayDeque<>())
                .offerLast(player);
    }

    /**
     * Internal helper class to simplify comparison logic.
     */
    private static class MatchCandidate {
        int ratingKey;
        Player player;
        int diff;

        MatchCandidate(int ratingKey, Player player, int diff) {
            this.ratingKey = ratingKey;
            this.player = player;
            this.diff = diff;
        }
    }
}
