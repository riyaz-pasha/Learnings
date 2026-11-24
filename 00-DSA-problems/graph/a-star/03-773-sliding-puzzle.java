import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/*
 * ============================================================================
 * 0. PROBLEM STATEMENT (LeetCode 773 — Sliding Puzzle)
 * ============================================================================
 * On a 2 x 3 board, there are five tiles labeled 1..5 and one empty square
 * labeled 0. A move consists of swapping 0 with a 4-directionally adjacent
 * tile. The board is solved iff it equals [[1, 2, 3], [4, 5, 0]].
 *
 * Given the initial 2 x 3 board, return the minimum number of moves required
 * to reach the solved state, or -1 if impossible.
 *
 * Constraints:
 *   - board.length == 2, board[i].length == 3
 *   - Values 0..5 each appear exactly once (a permutation of 6 elements)
 *
 *
 * ============================================================================
 * 1. CLARIFYING QUESTIONS (Ask before coding)
 * ============================================================================
 * 1. Is the board strictly fixed at 2 x 3, or could it generalize to m x n
 *    (e.g., 3 x 3 8-puzzle or 4 x 4 15-puzzle)? (2 x 3 has only 6! = 720
 *    states so plain BFS is instant; 3 x 3 has 9! = 362,880 states and 4 x 4
 *    has 16! ~ 2e13 states, which forces A* or IDA*.)
 * 2. Should we return 0 if the board is already solved? (Confirms boundary
 *    condition before entering the search loop.)
 * 3. Can we mutate the input 2D array, or must we leave `board` untouched?
 *    (Encoding into an immutable String or packed int avoids mutating input.)
 * 4. Do we only need the minimum move count, or will we also need to return
 *    the sequence of moves / board states? (Path reconstruction requires a
 *    parent map.)
 * 5. Are there multiple queries against different starting boards in one run?
 *    (If yes, we can run ONE reverse BFS from "123450" to precompute all 360
 *    solvable boards in O(1) lookup time per query.)
 *
 *
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * What's actually being asked & the binding constraint:
 *   This is NOT a grid-pathfinding problem where a player walks on 6 cells —
 *   it is a SHORTEST PATH problem on an IMPLICIT STATE GRAPH where each
 *   VERTEX is an entire board configuration (one of 6! = 720 permutations),
 *   and each EDGE is a single valid swap of '0' with a neighbor (degree 2 or 3,
 *   cost 1). Because states can loop back to earlier configurations, cycles
 *   exist and naive exploration blows up.
 *
 * ----------------------------------------------------------------------------
 * APPROACH 1: Brute-Force DFS / Backtracking over All Simple Paths
 * ----------------------------------------------------------------------------
 * 0. Idea / Intuition:
 *    Treat the puzzle like a recursive maze: from the current board, try
 *    every valid swap of 0, recurse while tracking visited boards along the
 *    current path, unmark on backtrack, and return the minimum depth that
 *    reaches [[1,2,3],[4,5,0]].
 * 1. What I'd naturally try:
 *    Maintain a `Set<String> onPath`, try all 2–3 swaps of 0, take
 *    `1 + min(dfs(next))`, and remove from `onPath` on return.
 * 2. Why it works:
 *    Every simple path in the 720-node state graph is finite, so exploring
 *    all simple paths eventually finds the shortest one.
 * 3. Why it's too slow / costly:
 *    Unlike a tree, a state graph has huge numbers of cycles and alternative
 *    paths between the same two boards. Even though there are only 720 states,
 *    there are exponentially many simple paths (up to ~2^360) of length up to
 *    360! It will TLE instantly even on a 2 x 3 board.
 * 4. What work is being repeated:
 *    We reach the exact same board configuration (e.g., "123405") via millions
 *    of different move histories, re-solving the rest of the puzzle from
 *    scratch every single time.
 * 5. What property / observation removes that bottleneck:
 *    Markov property + unit edge weights: the minimum moves from a board state
 *    S to the goal depends ONLY on S, not on how we got to S. And since every
 *    swap costs 1, exploring states level-by-level guarantees the VERY FIRST
 *    time we discover any board state is via its shortest path.
 * 6. Transition to Approach 2:
 *    Replace DFS with Breadth-First Search (BFS) over the state graph, using
 *    a global `visited` set so each of the 6! states is expanded at most once.
 *    Time Complexity:  O(b^V) where V = (R*C)! = 720 and branching factor
 *                      b ~ 2 — because backtracking explores exponentially
 *                      many simple paths across the state graph.
 *    Space Complexity: O((R*C)! * (R*C)) — because the recursion call stack
 *                      and `onPath` set can hold up to all V = 720 distinct
 *                      board strings of length R*C = 6 simultaneously.
 *
 * ----------------------------------------------------------------------------
 * APPROACH 2: State-Space BFS (Flatten 2x3 -> 6-char String + Neighbor Table)
 * ----------------------------------------------------------------------------
 * 0. Idea / Intuition:
 *    Flatten the 2 x 3 board into a 6-character String (e.g., "412503") so it
 *    is immutable and directly hashable in Java. Precompute the 4-directional
 *    neighbor indices for each of the 6 slots. Run BFS from `start` until we
 *    reach "123450".
 * 1. What I'd naturally try:
 *    Queue stores board strings, `visited` HashSet stores seen strings,
 *    process level-by-level incrementing `moves`.
 * 2. Why it works:
 *    Every move has uniform cost 1. BFS visits all states at distance d before
 *    any state at distance d + 1, so the first time "123450" appears, `moves`
 *    is guaranteed minimal. If the queue empties, the goal is in a different
 *    connected component -> return -1.
 * 3. Where it starts to struggle (motivation for Approach 3):
 *    For 2 x 3 (R*C = 6), V = 6! = 720 (only 360 reachable), so BFS finishes
 *    in < 1 millisecond! However, if an interviewer scales the board to 3 x 3
 *    (9! = 362,880 states) or 4 x 4 (16! ~ 2 * 10^13 states), blind BFS
 *    expands states moving AWAY from the goal and runs out of time/memory.
 * 4. What work is being wasted (at larger board sizes):
 *    BFS treats a board where 4 tiles are already in place identically to a
 *    completely scrambled board at the same move depth.
 * 5. What property / observation removes that waste:
 *    Every single swap moves exactly ONE numbered tile (1..5) by 1 cell
 *    orthogonally. Therefore, the sum of Manhattan distances of tiles 1..5
 *    from their current positions to their target positions is a strict
 *    lower bound (admissible AND consistent heuristic) on remaining moves!
 *    Additionally, for any odd-width board (W = 3), the parity of tile
 *    inversions (ignoring 0) is invariant — allowing O(N^2) instant detection
 *    of unsolvable boards without searching at all!
 * 6. Transition to Approach 3:
 *    Guide the search with A* using f(state) = g(state) + h(state), where
 *    h(state) = sum of Manhattan distances of tiles 1..5 (NOT tile 0!).
 *    Time Complexity:  O((R*C)! * (R*C)) — because there are at most (R*C)!
 *                      = 720 distinct board states, each dequeued once, and
 *                      building each of its <= 3 neighbor strings takes O(R*C)
 *                      time (effectively O(1) for fixed 2 x 3).
 *    Space Complexity: O((R*C)! * (R*C)) — because `visited` and the BFS queue
 *                      store up to (R*C)! = 720 strings of length R*C = 6.
 *
 * ----------------------------------------------------------------------------
 * APPROACH 3: A* Search with Sum-of-Manhattan-Distances Heuristic (+ Parity)
 * ----------------------------------------------------------------------------
 * 0. Idea / Intuition:
 *    First check inversion parity: if odd, return -1 immediately without
 *    expanding a single state! Otherwise, run A* with a PriorityQueue ordered
 *    by f = g + h, where g = moves so far and h = sum of Manhattan distances
 *    of tiles 1..5 to their solved coordinates.
 * 1. What I'd naturally try:
 *    Map `gScore<String, Integer>` tracks the best known move count per board,
 *    PriorityQueue<State> pops lowest f (tie-breaking by larger g / smaller h),
 *    using lazy deletion (`if (curr.g > gScore.get(curr.board)) continue;`).
 * 2. Why it works:
 *    Each move shifts one numbered tile by 1 step, changing that tile's
 *    Manhattan distance by at most +-1 and leaving all other numbered tiles
 *    unchanged. Thus h(u) <= 1 + h(v) (consistent) and h(goal) = 0, so the
 *    first time "123450" is POPPED from the min-heap, its g-value is optimal.
 * 3. Trade-off vs. BFS:
 *    Worst-case complexity has an extra log((R*C)!) heap factor, but in
 *    practice A* expands dramatically fewer states than BFS on solvable boards
 *    (and 0 states on unsolvable boards thanks to the parity check!).
 *    Time Complexity:  O((R*C)! * (R*C) * log((R*C)!)) worst-case — because
 *                      up to (R*C)! states can be pushed/popped from the heap
 *                      (log((R*C)!) each), with O(R*C) string & heuristic work
 *                      per neighbor. (Far faster in practice on larger boards.)
 *    Space Complexity: O((R*C)! * (R*C)) — because `gScore` map and the PQ
 *                      store up to O((R*C)!) board strings of length R*C.
 *
 * ----------------------------------------------------------------------------
 * 45-MINUTE INTERVIEW VERDICT: Which approach would I actually write?
 * ----------------------------------------------------------------------------
 * Write Approach 2 (BFS with String encoding + precomputed neighbor table).
 * Why? Because for a fixed 2 x 3 board, there are at most 720 total states
 * (360 reachable). BFS is ~25 lines, impossible to mess up with heuristic
 * admissibility bugs or stale PQ entries, and runs in < 1 ms. Then proactively
 * tell the interviewer: "If you scale this to 3 x 3 or 4 x 4, I'd switch to
 * A* with Sum-of-Manhattan-Distances (excluding 0) and an upfront inversion
 * parity check to reject unsolvable boards in O(N^2)."
 *
 *
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * 1. Already solved: [[1,2,3],[4,5,0]] -> must return 0 immediately.
 * 2. Unsolvable board (odd inversion count): e.g., [[1,2,3],[5,4,0]] -> -1.
 *    (Half of all 6! = 720 permutations are unreachable from "123450"!)
 * 3. One move away: [[1,2,3],[4,0,5]] or [[1,2,0],[4,5,3]] -> 1.
 * 4. Position of '0': corner indices (0, 2, 3, 5) have 2 neighbors, while
 *    middle indices (1, 4) have 3 neighbors — a wrap-around bug if you do
 *    naive `idx +- 1` without checking row boundaries!
 * 5. Cycles: swapping '0' back to its previous cell creates a 2-cycle; without
 *    a `visited` set, BFS loops forever and OOMs.
 * 6. Deepest solvable states: some 2 x 3 boards require up to 20 moves (God's
 *    number for 2 x 3) — ensures we don't accidentally cap search depth.
 *
 *
 * ============================================================================
 * 4. KEY INSIGHT, EXAMPLES, DRY RUN, PITFALLS, PATTERNS & INTERVIEW SCRIPT
 * ============================================================================
 *
 * --- KEY INSIGHT ---
 * Question to ask yourself:
 *   "What IS a node in this graph?"
 * Answer:
 *   NOT a cell (r, c) — the ENTIRE 2 x 3 board configuration!
 * Correctness intuition:
 *   Once we represent the entire board as a 6-character String, the puzzle
 *   collapses into standard unweighted shortest path on a tiny 720-node graph
 *   where each edge is a single swap of '0' with an adjacent index.
 *
 * --- ASCII DIAGRAM: 2x3 Board -> 1D Index Mapping & Adjacency ---
 *
 *   2x3 Grid Slots:       Flattened String Index:    Adjacency List (NEIGHBORS):
 *   +---+---+---+         +---+---+---+              0: {1, 3}
 *   | 0 | 1 | 2 |   ==>   " c0 c1 c2 c3 c4 c5 "      1: {0, 2, 4}
 *   +---+---+---+                                    2: {1, 5}
 *   | 3 | 4 | 5 |         Goal: "123450"             3: {0, 4}
 *   +---+---+---+                                    4: {1, 3, 5}
 *                                                    5: {2, 4}
 *
 *   Notice index 2 and index 3 are adjacent in the 1D string, but NOT on the
 *   2x3 board! Precomputing `NEIGHBORS` completely eliminates 2D boundary bugs.
 *
 * --- DRY RUN 1: BFS on Example 3 (board = [[4,1,2],[5,0,3]]) ---
 * Start = "412503", Target = "123450".
 *
 * Level (moves) | Queue Popped | '0' idx | New Unvisited Neighbors Pushed
 * --------------+--------------+---------+-------------------------------------
 *   0           | "412503"     |   4     | "402513" (swap 1), "412053" (swap 3),
 *               |              |         | "412530" (swap 5)
 *   1           | "402513"     |   1     | "042513" (swap 0), "420513" (swap 2)
 *               | "412053"     |   3     | "012453" (swap 0)   <-- optimal path!
 *               | "412530"     |   5     | "410532" (swap 2)
 *   2           | ...          |         | (other branches expand...)
 *               | "012453"     |   0     | "102453" (swap 1)   <-- optimal path!
 *   3           | ...          |         |
 *               | "102453"     |   1     | "120453" (swap 2), "152403" (swap 4)
 *   4           | ...          |         |
 *               | "120453"     |   2     | "123450" (swap 5)   <-- TARGET FOUND!
 *
 * Target discovered while expanding level 4 -> return moves + 1 = 5.
 *
 * --- DRY RUN 2: Edge Case — Why [[1,2,3],[5,4,0]] is Unsolvable (Parity) ---
 * Flatten ignoring '0':
 *   Solved state "123450" -> [1, 2, 3, 4, 5] -> 0 inversions (EVEN).
 *   Example 2    "123540" -> [1, 2, 3, 5, 4] -> (5, 4) = 1 inversion (ODD).
 *
 * Why can NO sequence of moves ever change parity on width W = 3?
 *   - Moving '0' left/right: doesn't change the order of the other 5 tiles
 *     -> inversions change by 0 (EVEN).
 *   - Moving '0' up/down: shifts one tile across exactly W - 1 = 2 other tiles
 *     -> inversions change by +2, 0, or -2 (ALWAYS EVEN).
 * Therefore, an ODD-inversion board can NEVER reach the EVEN-inversion goal!
 * BFS explores all 360 states in the odd component, empties the queue, and
 * returns -1. A* + parity check rejects it in O(1) before even starting!
 *
 * --- PITFALLS ---
 * 1. Putting `int[][]` into a `HashSet` in Java:
 *    Java arrays do NOT override `equals()` or `hashCode()`! Two distinct
 *    `int[][]` with identical contents will be treated as DIFFERENT states,
 *    causing an infinite loop. Always encode into an immutable `String` (or
 *    bit-packed `int`).
 * 2. Including tile '0' in A*'s Manhattan heuristic:
 *    CLASSIC TRAP! Suppose the board is "123405" (1 move from "123450").
 *    Manhattan(tile 5) = 1, Manhattan(tile 0) = 1 -> sum = 2 > true cost 1!
 *    Including '0' double-counts every move, making h INADMISSIBLE and
 *    breaking A*'s optimality guarantee! Only sum Manhattan distances of 1..5.
 * 3. Naive 1D wrap-around (`idx - 1` / `idx + 1`):
 *    From index 2 (top-right), `idx + 1 = 3` (bottom-left) is NOT adjacent!
 *    Always use a hardcoded `NEIGHBORS` table or convert `(idx / 3, idx % 3)`.
 * 4. Mutating the board array in-place while pushing references to the queue:
 *    All queue entries end up pointing to the same mutated array.
 *
 * --- PATTERN RECOGNITION ---
 * "When you see 'minimum number of moves/operations to transform a small
 *  configuration into a target configuration', think Implicit State-Space BFS
 *  (encode state as String/int -> shortest path)."
 * Transfers directly to:
 *   1. LeetCode 752 (Open the Lock): 4-digit String state, 8 neighbors per state.
 *   2. LeetCode 127 (Word Ladder): String word state, 1-letter transitions.
 *   3. 8-Puzzle / 15-Puzzle: Exact same problem on 3x3 / 4x4 -> upgrade BFS to A*.
 *
 * --- INTERVIEW SCRIPT (How to talk through this out loud) ---
 * "Instead of thinking about moving on a 6-cell grid, I'll model this as a
 *  shortest-path problem on an implicit state graph where each node is a full
 *  2 x 3 board configuration and each edge is a single swap of 0 with an
 *  adjacent tile. Since all moves cost 1, BFS guarantees the minimum number of
 *  moves the first time we see the target state. To avoid 2D array hashing
 *  pitfalls in Java, I'll flatten the board into a 6-character String like
 *  '412503' and precompute the valid swap indices for each of the 6 positions
 *  so there's no row-wrap bug. There are at most 6! = 720 permutations (only
 *  360 reachable), so BFS visits at most 720 states. If this scaled to 3 x 3
 *  or 4 x 4, we'd upgrade to A* using the sum of Manhattan distances of tiles
 *  1..5 (excluding 0 so it stays admissible) plus an upfront inversion-parity
 *  check. For this 2 x 3 board, BFS gives us O((R*C)! * (R*C)) time and
 *  O((R*C)! * (R*C)) space — effectively O(1) since R*C = 6 is constant."
 *
 *
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q1: What if the board is 3 x 3 (8-puzzle) or 4 x 4 (15-puzzle)?
 * A1: 3 x 3 has 9! = 362,880 states (BFS still works in ~200 ms, A* is much
 *     faster). 4 x 4 has 16! ~ 2 * 10^13 states — BFS and even standard A*
 *     will OOM. You must use IDA* (Iterative Deepening A*) with Manhattan
 *     distance + Linear Conflict heuristic to keep memory at O(depth).
 *
 * Q2: How can you detect an unsolvable board WITHOUT running the search?
 * A2: Count inversions in the flattened board ignoring 0. For odd width W
 *     (like 2 x 3 or 3 x 3), solvable iff inversions % 2 == 0. For even width
 *     W (like 4 x 4), solvable iff (inversions + blankRowFromBottom) is odd
 *     (when 1-indexed from bottom, matching the solved state's parity).
 *
 * Q3: How would you return the actual sequence of moves instead of the count?
 * A3: Maintain `Map<String, String> parent` (childBoard -> parentBoard) and
 *     optionally `Map<String, Character> movedTile`. Once "123450" is found,
 *     walk backward from "123450" to `start` and reverse the list.
 *
 * Q4: What if you receive 100,000 queries of different 2 x 3 boards?
 * A4: Run ONE reverse BFS starting from "123450" to build a `Map<String, Integer>`
 *     of distances to all 360 solvable boards. Each query then becomes an O(1)
 *     `map.getOrDefault(start, -1)` lookup!
 *
 * Q5: Can we optimize Java memory/GC overhead of String allocations?
 * A5: Yes — since each tile is 0..5 (3 bits), the entire 6-tile board fits in
 *     18 bits of a single primitive `int` (or 6 base-6 digits < 6^6 = 46,656).
 *     Bit-packing eliminates String/char[] allocations completely.
 */
public class SlidingPuzzleStudyNote {

    private static final String TARGET = "123450";

    // Precomputed 4-directional neighbor indices for a flattened 2 x 3 board:
    //   0 1 2
    //   3 4 5
    // Example: if '0' is at index 4 (row 1, col 1), it can swap with
    // index 1 (up), index 3 (left), or index 5 (right).
    private static final int[][] NEIGHBORS = {
        {1, 3},       // index 0 (top-left)
        {0, 2, 4},    // index 1 (top-middle)
        {1, 5},       // index 2 (top-right)
        {0, 4},       // index 3 (bottom-left)
        {1, 3, 5},    // index 4 (bottom-middle)
        {2, 4}        // index 5 (bottom-right)
    };

    // Target (row, col) for each tile '1'..'5' in "123450" (used by A* heuristic).
    // Example: tile 4 belongs at (1, 0), tile 5 belongs at (1, 1).
    private static final int[][] GOAL_POS = {
        {1, 2}, // '0' (unused in heuristic — NEVER include 0 in Manhattan sum!)
        {0, 0}, // '1'
        {0, 1}, // '2'
        {0, 2}, // '3'
        {1, 0}, // '4'
        {1, 1}  // '5'
    };

    // ========================================================================
    // APPROACH 2 (RECOMMENDED FOR 2x3): State-Space BFS
    // ========================================================================
    /**
     * Finds the minimum number of moves to solve the 2 x 3 sliding puzzle
     * using level-by-level BFS over the implicit state graph.
     *
     * Time Complexity:  O((R*C)! * (R*C)) — at most 6! = 720 states dequeued,
     *                   each generating <= 3 strings of length 6.
     * Space Complexity: O((R*C)! * (R*C)) — visited set and queue hold at most
     *                   720 strings of length 6.
     */
    public static int slidingPuzzleBFS(int[][] board) {
        String start = encode(board);

        // Edge case: board is already in the solved configuration -> 0 moves.
        if (start.equals(TARGET)) {
            return 0;
        }

        // FIFO queue stores frontier board states at the current BFS depth.
        // Example after level 0 on [[4,1,2],[5,0,3]]:
        //   queue = ["402513", "412053", "412530"].
        Deque<String> queue = new ArrayDeque<>();

        // Tracks every board configuration already discovered so cycles never
        // cause infinite loops or redundant re-expansions.
        Set<String> visited = new HashSet<>();

        queue.offer(start);
        visited.add(start);
        int moves = 0;

        while (!queue.isEmpty()) {
            // Snapshot current level size so we increment `moves` once per wave.
            int levelSize = queue.size();
            moves++;

            for (int i = 0; i < levelSize; i++) {
                String curr = queue.poll();
                int zeroIdx = curr.indexOf('0');

                // Try swapping '0' with each valid 4-directional neighbor.
                for (int nextIdx : NEIGHBORS[zeroIdx]) {
                    String next = swap(curr, zeroIdx, nextIdx);

                    // In unweighted BFS, first discovery of TARGET is guaranteed optimal.
                    if (next.equals(TARGET)) {
                        return moves;
                    }

                    // Set.add() returns true iff `next` was not already present,
                    // combining the contains-check and insertion in one lookup.
                    if (visited.add(next)) {
                        queue.offer(next);
                    }
                }
            }
        }

        // Exhausted the entire reachable connected component without seeing TARGET.
        return -1;
    }

    // ========================================================================
    // APPROACH 3 (SCALES TO 3x3+): A* Search with Manhattan Heuristic + Parity
    // ========================================================================
    /**
     * Immutable search state for A*'s PriorityQueue.
     * Example: board = "412053", g = 1 move so far, h = 4, f = 5.
     */
    private record State(String board, int g, int f) implements Comparable<State> {
        @Override
        public int compareTo(State other) {
            int cmp = Integer.compare(this.f, other.f);
            if (cmp != 0) {
                return cmp;
            }
            // Tie-breaker: prefer larger g (smaller h) to dive straight toward goal.
            return Integer.compare(other.g, this.g);
        }
    }

    /**
     * Solves the 2 x 3 sliding puzzle using A* search with the sum of Manhattan
     * distances of tiles 1..5, preceded by an O(1) inversion-parity check.
     *
     * Time Complexity:  O((R*C)! * (R*C) * log((R*C)!)) worst-case; expands
     *                   far fewer states than BFS in practice on larger boards.
     * Space Complexity: O((R*C)! * (R*C)) — gScore map and PQ entries.
     */
    public static int slidingPuzzleAStar(int[][] board) {
        String start = encode(board);
        if (start.equals(TARGET)) {
            return 0;
        }

        // Fast mathematical pruning: for any odd-width board (width = 3),
        // an odd number of inversions (ignoring '0') is provably unsolvable.
        if (!isSolvableOddWidth(start)) {
            return -1;
        }

        // gScore maps each discovered board -> minimum moves found to reach it.
        // Example: gScore.get("412503") = 0, gScore.get("412053") = 1.
        Map<String, Integer> gScore = new HashMap<>();
        gScore.put(start, 0);

        PriorityQueue<State> pq = new PriorityQueue<>();
        pq.offer(new State(start, 0, manhattanHeuristic(start)));

        while (!pq.isEmpty()) {
            State curr = pq.poll();

            // Lazy deletion: skip stale heap entries if a shorter path to
            // curr.board was already recorded in gScore.
            if (curr.g > gScore.getOrDefault(curr.board, Integer.MAX_VALUE)) {
                continue;
            }

            // Consistent heuristic guarantees optimality when TARGET is popped.
            if (curr.board.equals(TARGET)) {
                return curr.g;
            }

            int zeroIdx = curr.board.indexOf('0');
            int nextG = curr.g + 1;

            for (int nextIdx : NEIGHBORS[zeroIdx]) {
                String next = swap(curr.board, zeroIdx, nextIdx);

                // Relax edge only if we found a strictly shorter move count.
                if (nextG < gScore.getOrDefault(next, Integer.MAX_VALUE)) {
                    gScore.put(next, nextG);
                    int nextF = nextG + manhattanHeuristic(next);
                    pq.offer(new State(next, nextG, nextF));
                }
            }
        }

        return -1;
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

    /** Flattens a 2 x 3 board into a 6-character String, e.g. [[4,1,2],[5,0,3]] -> "412503". */
    private static String encode(int[][] board) {
        StringBuilder sb = new StringBuilder(6);
        for (int[] row : board) {
            for (int val : row) {
                sb.append(val);
            }
        }
        return sb.toString();
    }

    /** Returns a new String with characters at index `i` and `j` swapped. */
    private static String swap(String s, int i, int j) {
        char[] chars = s.toCharArray();
        char tmp = chars[i];
        chars[i] = chars[j];
        chars[j] = tmp;
        return new String(chars);
    }

    /**
     * Admissible & consistent heuristic: sum of Manhattan distances of tiles
     * '1'..'5' from their current (r, c) to GOAL_POS.
     * CRITICAL: Tile '0' MUST be skipped! Including '0' double-counts swaps
     * and makes the heuristic overestimate (inadmissible).
     */
    private static int manhattanHeuristic(String state) {
        int h = 0;
        for (int idx = 0; idx < 6; idx++) {
            int tile = state.charAt(idx) - '0';
            if (tile == 0) {
                continue;
            }
            int r = idx / 3;
            int c = idx % 3;
            h += Math.abs(r - GOAL_POS[tile][0]) + Math.abs(c - GOAL_POS[tile][1]);
        }
        return h;
    }

    /**
     * Parity invariant for odd-width boards (width = 3):
     * Counts pairs (i < j) with state[i] > state[j], ignoring '0'.
     * Since horizontal moves change inversions by 0 and vertical moves jump
     * across 2 tiles (changing inversions by +-2 or 0), parity never changes.
     */
    private static boolean isSolvableOddWidth(String state) {
        int inversions = 0;
        for (int i = 0; i < 6; i++) {
            char a = state.charAt(i);
            if (a == '0') continue;
            for (int j = i + 1; j < 6; j++) {
                char b = state.charAt(j);
                if (b != '0' && a > b) {
                    inversions++;
                }
            }
        }
        return inversions % 2 == 0;
    }

    // ========================================================================
    // TEST HARNESS & CROSS-CHECK (BFS vs A*)
    // ========================================================================
    private static void verify(String name, int[][] board, int expected) {
        int bfs = slidingPuzzleBFS(board);
        int aStar = slidingPuzzleAStar(board);
        boolean pass = (bfs == expected && aStar == expected);
        System.out.printf("%-32s | expected=%-3d | BFS=%-3d | A*=%-3d | %s%n",
                name, expected, bfs, aStar, pass ? "PASS" : "FAIL");
        if (!pass) {
            throw new AssertionError("Test failed: " + name);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Cross-Checking BFS vs A* on Sliding Puzzle ===");

        // Official LeetCode examples
        verify("Example 1 (1 move)", new int[][]{{1, 2, 3}, {4, 0, 5}}, 1);
        verify("Example 2 (Unsolvable)", new int[][]{{1, 2, 3}, {5, 4, 0}}, -1);
        verify("Example 3 (5 moves, Dry Run 1)", new int[][]{{4, 1, 2}, {5, 0, 3}}, 5);

        // Edge cases
        verify("Edge: Already solved (0 moves)", new int[][]{{1, 2, 3}, {4, 5, 0}}, 0);
        verify("Edge: 2 moves away", new int[][]{{1, 2, 3}, {0, 4, 5}}, 2);
        verify("Edge: 5 moves (tight h=5)", new int[][]{{0, 5, 2}, {1, 4, 3}}, 5);
        verify("Edge: Another unsolvable", new int[][]{{2, 1, 3}, {4, 5, 0}}, -1);

        // Deep solvable case: cross-check BFS == A* on reversed board
        int[][] reversed = {{0, 5, 4}, {3, 2, 1}};
        int bfsDeep = slidingPuzzleBFS(reversed);
        int aStarDeep = slidingPuzzleAStar(reversed);
        System.out.printf("%-32s | BFS=%-3d | A*=%-3d | %s%n",
                "Deep board [[0,5,4],[3,2,1]]", bfsDeep, aStarDeep,
                (bfsDeep == aStarDeep && bfsDeep > 0) ? "PASS" : "FAIL");
        if (bfsDeep != aStarDeep || bfsDeep <= 0) {
            throw new AssertionError("Deep board cross-check failed!");
        }

        // Verify input board was never mutated
        int[][] original = {{4, 1, 2}, {5, 0, 3}};
        int[][] snapshot = {{4, 1, 2}, {5, 0, 3}};
        slidingPuzzleBFS(original);
        slidingPuzzleAStar(original);
        if (!Arrays.deepEquals(original, snapshot)) {
            throw new AssertionError("Input board was mutated!");
        }

        System.out.println("All tests passed!");
    }
}

/*
 * ============================================================================
 * 7. SHORT SUMMARY
 * ============================================================================
 * - Core pattern:       Implicit State-Space BFS (upgrade to A* for 3x3+).
 * - Key observation:    Each node in the graph is the ENTIRE 2 x 3 board
 *                       flattened to a 6-char String; precomputing the 6-slot
 *                       neighbor table eliminates 2D boundary bugs.
 * - Memorize vs. Know:  Don't memorize the neighbor array — derive it from
 *                       sketching `0 1 2 / 3 4 5`. Understand WHY String
 *                       encoding is needed in Java and WHY tile '0' is
 *                       excluded from A*'s Manhattan heuristic.
 * - Most common trap:   Using `int[][]` in a HashSet (reference equality bug)
 *                       or including '0' in Manhattan distance (breaks A*).
 * - One-line trigger:   "Minimum moves to transform a small board/state ->
 *                       encode state as immutable String/int + BFS."
 */


































import java.util.*;

class Solution {

    private static final String TARGET = "123450";

    /*
     * For each index, store the indices that can be
     * reached by moving the empty cell (0).
     *
     * Board:
     *
     * 0 1 2
     * 3 4 5
     */
    private static final int[][] NEIGHBORS = {
        {1, 3},
        {0, 2, 4},
        {1, 5},
        {0, 4},
        {1, 3, 5},
        {2, 4}
    };

    public int slidingPuzzle(int[][] board) {

        /*
         * Convert the 2D board into a single string.
         *
         * Example:
         *
         * 1 2 3
         * 4 0 5
         *
         * becomes "123405".
         */
        String start = encode(board);

        /*
         * If the initial configuration is already the target,
         * no moves are required.
         */
        if (start.equals(TARGET)) {
            return 0;
        }

        /*
         * Queue stores board configurations.
         *
         * BFS guarantees that states are processed
         * in increasing order of their distance
         * from the initial configuration.
         */
        Queue<String> queue = new ArrayDeque<>();

        /*
         * Visited prevents processing the same
         * configuration multiple times.
         *
         * Mark states visited when ENQUEUING them,
         * not when removing them.
         *
         * This guarantees that each state enters
         * the queue at most once.
         */
        Set<String> visited = new HashSet<>();

        queue.offer(start);
        visited.add(start);

        /*
         * Number of moves required to reach the
         * configurations in the current BFS level.
         *
         * Initially, the starting configuration
         * requires zero moves.
         */
        int moves = 0;

        while (!queue.isEmpty()) {

            /*
             * Process only the configurations belonging
             * to the current BFS level.
             *
             * Any newly generated configurations belong
             * to the next level.
             */
            int size = queue.size();

            for (int i = 0; i < size; i++) {

                String current = queue.poll();

                /*
                 * We could check the target here because
                 * BFS processes states in increasing distance.
                 *
                 * The target's current BFS level is
                 * therefore the minimum number of moves.
                 */
                if (current.equals(TARGET)) {
                    return moves;
                }

                /*
                 * Locate the empty cell.
                 *
                 * Example:
                 * "123405" -> zeroIndex = 4.
                 */
                int zeroIndex = current.indexOf('0');

                /*
                 * Generate all valid moves of the empty cell.
                 */
                for (int nextIndex : NEIGHBORS[zeroIndex]) {

                    /*
                     * Swap the empty cell with one of
                     * its adjacent tiles.
                     *
                     * This produces one neighboring state.
                     */
                    String nextState =
                            swap(current, zeroIndex, nextIndex);

                    /*
                     * Only process configurations
                     * that have not been visited before.
                     */
                    if (visited.add(nextState)) {

                        /*
                         * Add the new configuration
                         * to the queue for the next level.
                         */
                        queue.offer(nextState);
                    }
                }
            }

            /*
             * All configurations at the current distance
             * have been processed.
             *
             * Move to the next BFS level.
             */
            moves++;
        }

        /*
         * If BFS exhausts all reachable configurations
         * without finding the target, the puzzle is impossible.
         */
        return -1;
    }

    /*
     * Converts a 2D board into a string representation.
     *
     * Example:
     *
     * [[1, 2, 3],
     *  [4, 0, 5]]
     *
     * becomes "123405".
     *
     * Time: O(6) = O(1), because the board has fixed size.
     * Space: O(6) = O(1).
     */
    private String encode(int[][] board) {

        StringBuilder sb = new StringBuilder();

        for (int[] row : board) {
            for (int value : row) {
                sb.append(value);
            }
        }

        return sb.toString();
    }

    /*
     * Generates a new state by swapping two positions.
     *
     * We create a character array because Java Strings
     * are immutable.
     *
     * Example:
     *
     * state = "123405"
     * i = 4
     * j = 5
     *
     * result = "123450".
     *
     * Time: O(6) = O(1).
     * Space: O(6) = O(1).
     */
    private String swap(String state, int i, int j) {

        char[] chars = state.toCharArray();

        char temp = chars[i];

        chars[i] = chars[j];
        chars[j] = temp;

        return new String(chars);
    }
}




