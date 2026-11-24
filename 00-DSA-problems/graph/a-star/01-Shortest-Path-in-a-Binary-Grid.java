import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.PriorityQueue;

/*
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Given an m x n grid of 0 (open) and 1 (wall), return the cost of the
 * shortest 4-directional path from (0, 0) to (m - 1, n - 1) (each step costs 1),
 * or -1 if unreachable. Use A* with the Manhattan heuristic.
 *
 * Constraints:
 *   - 1 <= m, n <= 1000  (up to 1,000,000 cells)
 *   - Start (0, 0) and goal (m - 1, n - 1) are open (0)
 *   - Target time complexity: O((m * n) log(m * n))
 *
 *
 * ============================================================================
 * 1. CLARIFYING QUESTIONS (Ask before writing a single line of code)
 * ============================================================================
 * 1. Does "cost" count the number of steps (edges) or the number of cells
 *    visited? (For a 1x1 grid, steps = 0 while cells = 1; prevents an off-by-1.)
 * 2. Are start (0, 0) and goal (m - 1, n - 1) guaranteed to be 0, or could
 *    either be a wall? (Determines whether we need an upfront guard returning -1.)
 * 3. Can we mutate the input grid (e.g., marking visited cells as 1), or must
 *    it remain read-only? (Controls whether we allocate a separate gScore/visited
 *    array or can save memory in-place — note: A* needs numeric g-scores anyway.)
 * 4. Is movement strictly 4-directional (N/S/E/W), or could diagonals ever be
 *    allowed? (Manhattan heuristic is admissible ONLY for 4-directional unit
 *    steps; diagonals would make Manhattan overestimate and break optimality.)
 * 5. Are all grid rows guaranteed to have the same length n (non-jagged), and
 *    is grid guaranteed non-null/non-empty? (Clarifies defensive input checks.)
 * 6. Do we only need the minimum cost, or will we also need to reconstruct and
 *    return the actual path? (Path reconstruction requires a parent[][] array.)
 * 7. Are all step costs strictly 1, or could cells have variable weights later?
 *    (If weights are all 1, BFS is actually O(mn) worst-case; if weights vary,
 *    A* / Dijkstra is strictly required and heuristic must be scaled by minCost.)
 *
 *
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * What's actually being asked & the binding constraint:
 *   We want the minimum-length path in an unweighted grid graph with up to
 *   V = m * n = 1,000,000 vertices and E <= 4V edges. Because V can be 10^6,
 *   any exponential exploration dies instantly, and even O(V^2) = 10^12 ops
 *   times out — we need near-linear O(V) or O(V log V) time.
 *
 * ----------------------------------------------------------------------------
 * APPROACH 1: Brute-Force Backtracking (DFS over all simple paths)
 * ----------------------------------------------------------------------------
 * 0. Idea / Intuition:
 *    Explore every simple (non-repeating) path from (0, 0) to (m - 1, n - 1)
 *    using recursion, tracking the minimum path length seen across all paths
 *    that reach the goal.
 * 1. What I'd naturally try:
 *    Mark current cell visited, recurse into all 4 open neighbors, unmark on
 *    return (backtrack) so other paths can use the cell, and take min + 1.
 * 2. Why it works:
 *    Every simple path is finite and explored exhaustively, so the global
 *    minimum is guaranteed to be found.
 * 3. Why it's too slow / costly:
 *    Even on a tiny 10 x 10 open grid, there are billions of simple paths.
 *    At 1000 x 1000, recursion depth alone hits 10^6 (instant StackOverflowError
 *    in Java, whose default stack holds ~5,000–10,000 frames), and the runtime
 *    is astronomical.
 * 4. What work is being repeated:
 *    We re-enter the exact same cell (r, c) millions of times via different
 *    prefixes, even when we already reached (r, c) in fewer steps earlier.
 * 5. What property / observation removes that bottleneck:
 *    Optimal substructure + uniform step cost: if we reach (r, c) in k steps,
 *    we NEVER need to re-explore (r, c) at any step count >= k. Even better,
 *    if we explore cells in non-decreasing order of distance from (0, 0), the
 *    very FIRST time we visit any cell is guaranteed to be its shortest path.
 * 6. Transition to Approach 2:
 *    Replace depth-first exploration with level-by-level (breadth-first)
 *    exploration using a FIFO queue.
 *    Time Complexity:  O(3^(m * n)) — because from each cell (after the first)
 *                      we can branch into up to 3 unvisited neighbors, exploring
 *                      exponentially many simple paths.
 *    Space Complexity: O(m * n) — because the visited matrix holds m * n
 *                      booleans and the recursion call stack can grow as deep
 *                      as m * n frames (risking StackOverflowError in Java).
 *
 * ----------------------------------------------------------------------------
 * APPROACH 2: Breadth-First Search (BFS)
 * ----------------------------------------------------------------------------
 * 0. Idea / Intuition:
 *    Expand outward from (0, 0) in concentric "waves" of distance 0, 1, 2, ...
 *    Since every edge costs 1, the first wave that touches (m - 1, n - 1) must
 *    be the shortest path.
 * 1. What I'd naturally try:
 *    Push (0, 0) into an ArrayDeque, maintain a dist[][] array initialized to
 *    -1, and relax unvisited neighbors with dist[r][c] + 1.
 * 2. Why it works:
 *    A FIFO queue preserves monotonically non-decreasing distances, so every
 *    cell is settled permanently on its first discovery.
 * 3. Why it's still suboptimal in practice (the motivation for A*):
 *    Wait — asymptotically, BFS is O(m * n), which is actually BETTER in the
 *    worst case than A*'s O((m * n) log(m * n))! So why does this problem ask
 *    for A*? Because on large open or sparsely blocked grids, BFS is *blind*:
 *    it expands equally in all directions, exploring cells that move directly
 *    AWAY from the goal. On a 1000 x 1000 open grid, BFS visits all ~1,000,000
 *    cells, whereas a goal-directed search visits only ~2,000 cells.
 * 4. What work is being wasted:
 *    Expanding cells whose best-case total distance to the goal is already
 *    worse than paths heading straight toward (m - 1, n - 1).
 * 5. What property / observation removes that waste:
 *    We know the exact coordinates of the goal (m - 1, n - 1). On a 4-way
 *    grid, you can never reach the goal in fewer than Manhattan distance
 *    h(r, c) = (m - 1 - r) + (n - 1 - c) steps.
 * 6. Transition to Approach 3 (and why it's a trap before Approach 4):
 *    Natural instinct: "Just use a PriorityQueue ordered by h(r, c) so we
 *    always walk toward the goal!" Let's see why that alone breaks.
 *    Time Complexity:  O(m * n) — because each of the m * n cells is enqueued
 *                      and dequeued at most once, checking 4 neighbors each.
 *    Space Complexity: O(m * n) — because dist[][] stores m * n ints and the
 *                      BFS frontier queue can hold up to O(min(m, n)) cells
 *                      (or O(m * n) in pathological mazes).
 *
 * ----------------------------------------------------------------------------
 * APPROACH 3: Greedy Best-First Search (Tempting Detour — INCORRECT!)
 * ----------------------------------------------------------------------------
 * 0. Idea / Intuition:
 *    Replace BFS's FIFO queue with a min-heap ordered purely by Manhattan
 *    distance h(r, c) to the goal.
 * 1. What I'd naturally try:
 *    Pop the cell closest to (m - 1, n - 1), push its unvisited neighbors.
 * 2. Why it SEEMS to work — and where it fails:
 *    On an open grid it sprints directly to the goal in m + n - 2 steps.
 *    HOWEVER, it completely ignores how many steps g(r, c) it already spent
 *    getting there! If a wall forces a long detour near the goal, Greedy stays
 *    fixated on the detour (small h) and ignores a much shorter alternative
 *    path whose start still has a larger h. (See Section 4 for the exact 5x5
 *    counterexample where Greedy returns 12 instead of 8!)
 * 3. Cost (even when it returns a wrong answer):
 *    Time Complexity:  O((m * n) log(m * n)) — because each cell is pushed and
 *                      popped from a binary heap at most once.
 *    Space Complexity: O(m * n) — because visited[][] and the heap store up to
 *                      m * n cells.
 * 4. What's missing:
 *    It optimizes "estimated remaining cost" h(n) instead of "estimated TOTAL
 *    path cost" f(n) = g(n) + h(n).
 * 5. What property / observation fixes correctness:
 *    If we prioritize by f(n) = g(n) + h(n), where g(n) is the exact distance
 *    from start and h(n) NEVER overestimates the true remaining distance
 *    (admissibility), then any detour increases g(n) enough to push its f(n)
 *    above the true shortest path's f-value — forcing the heap to switch back
 *    to the optimal path before the suboptimal path can finish!
 *
 * ----------------------------------------------------------------------------
 * APPROACH 4: A* Search with Manhattan Heuristic (REQUIRED OPTIMAL)
 * ----------------------------------------------------------------------------
 * 0. Idea / Intuition:
 *    Combine BFS/Dijkstra's exact cost-so-far g(r, c) with Greedy's lower-bound
 *    distance-to-goal h(r, c) = (m - 1 - r) + (n - 1 - c). Always expand the
 *    node with the smallest estimated total cost f(r, c) = g(r, c) + h(r, c).
 * 1. What I'd naturally try:
 *    Maintain int[][] gScore (initialized to INF) and a PriorityQueue<Node>
 *    ordered by f ascending (breaking ties by larger g / smaller h so we dive
 *    straight toward the goal on open regions). Use lazy deletion: when
 *    popping a node, skip it if curr.g > gScore[r][c].
 * 2. Why it works:
 *    Manhattan distance is both ADMISSIBLE (h(n) <= true remaining cost) and
 *    CONSISTENT (for every step u -> v of cost 1, h(u) <= 1 + h(v), because a
 *    single orthogonal step reduces Manhattan distance by at most 1).
 *    Consistency guarantees that f-values along any path are monotonically
 *    non-decreasing:
 *        f(v) = g(v) + h(v) = g(u) + 1 + h(v) >= g(u) + h(u) = f(u).
 *    Therefore, the FIRST time any cell (including the goal) is POPPED from
 *    the min-heap, its g-value is provably optimal.
 * 3. Trade-off / Cost at constraint scale (m, n <= 1000):
 *    With V <= 10^6, V log2(V) is ~2 * 10^7 heap operations in the absolute
 *    worst case (unreachable goal or full maze), which easily passes in Java
 *    (< 200 ms). On typical open/sparse grids, A* explores a tiny fraction of
 *    the grid compared to BFS.
 *    Time Complexity:  O((m * n) log(m * n)) — because each cell has at most 4
 *                      edges that can relax it, yielding O(m * n) heap pushes
 *                      and pops, each costing O(log(m * n)) in a binary heap.
 *    Space Complexity: O(m * n) — because gScore[][] holds m * n ints and the
 *                      PriorityQueue can hold up to O(m * n) entries (with
 *                      lazy deletion, at most O(E) = O(m * n) entries total).
 *
 * ----------------------------------------------------------------------------
 * 45-MINUTE INTERVIEW VERDICT: Which approach would I actually write?
 * ----------------------------------------------------------------------------
 * - If the interviewer explicitly asks for A* (as here) or hints at weighted
 *   terrain / huge open maps: write Approach 4 (A* with gScore[][] + lazy
 *   deletion in PriorityQueue). It's ~35 lines, clean, and handles weighted
 *   follow-ups with almost zero structural change.
 * - If the interviewer simply asks for shortest path in an unweighted grid
 *   without mentioning A*: write Approach 2 (BFS) first! BFS is strictly
 *   O(m * n) worst-case (no log factor), impossible to mess up with stale heap
 *   entries, and faster to code under pressure — then mention A* as an
 *   optimization for sparse/open grids or weighted follow-ups.
 *
 *
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * 1. 1 x 1 grid: [[0]] -> start == goal, 0 steps needed (return 0, not 1).
 * 2. Start or goal blocked: grid[0][0] == 1 or grid[m-1][n-1] == 1 -> return -1
 *    (even though constraints say open, guard defensively in 1 line).
 * 3. Single row (1 x n) or single column (m x 1): a single wall anywhere in
 *    between makes the goal unreachable -> return -1.
 * 4. Completely unreachable goal: wall cuts grid in half, or goal is boxed in
 *    -> PQ empties naturally, return -1.
 * 5. Detour required (must move AWAY from goal): e.g., spiral or U-shaped wall
 *    where optimal path temporarily increases h(r, c).
 * 6. Greedy trap: a path that initially decreases h rapidly, then winds back
 *    around a wall, while another path is shorter overall (see Section 4).
 * 7. Multiple equal-length shortest paths: tie-breaking in PriorityQueue
 *    (preferring higher g / lower h avoids expanding wide diamonds of equal f).
 * 8. Max constraints (1000 x 1000): up to 10^6 cells; max simple path length
 *    is < 10^6 (fits safely in 32-bit int; no long needed for g or f, though
 *    watch out for Integer.MAX_VALUE + 1 overflow when checking gScore).
 *
 *
 * ============================================================================
 * 4. KEY INSIGHT, EXAMPLES, DRY RUN, PITFALLS, PATTERNS & INTERVIEW SCRIPT
 * ============================================================================
 *
 * --- KEY INSIGHT ---
 * Question to ask yourself:
 *   "Standing at cell (r, c) after g steps, what is the ABSOLUTE CHEAPEST
 *    total path to the goal that could possibly pass through me?"
 * Answer:
 *   At least f(r, c) = g + (m - 1 - r) + (n - 1 - c).
 * Correctness intuition ("why this MUST work"):
 *   Suppose the goal is popped from the min-heap with cost g*. Since h(goal)=0,
 *   its priority is f(goal) = g*. Every other node u still sitting in the heap
 *   has f(u) >= g*. Because h(u) never overestimates the remaining distance
 *   from u to the goal, ANY complete path through u must have true total cost
 *   >= g(u) + h(u) = f(u) >= g*. Therefore, no unexplored path can ever beat g*.
 *
 * --- EXAMPLES (WITH ASCII DIAGRAMS) ---
 *
 * Example A (Typical 3 x 4 grid — used in Dry Run 1):
 *   S = (0,0), G = (2,3), # = wall (1), . = open (0)
 *
 *     col: 0 1 2 3
 *   row 0: S . # .
 *   row 1: . # . .
 *   row 2: . . . G
 *
 *   Optimal path: (0,0) -> (1,0) -> (2,0) -> (2,1) -> (2,2) -> (2,3) = 5 steps.
 *
 * Example B (5 x 5 Greedy Trap — used in Dry Run 2):
 *   S = (0,0), G = (4,4)
 *
 *     col: 0 1 2 3 4
 *   row 0: S . . . .    <-- Greedy rushes right across row 0 (h drops 8->4),
 *   row 1: . # # # .        goes down col 4 to (2,4) [h=2], hits wall at (3,4),
 *   row 2: . # . . .        then snakes LEFT to (2,2) and down to (4,4): cost 12!
 *   row 3: . # . # #
 *   row 4: . . . . G    <-- Optimal path goes down col 0 & across row 4: cost 8!
 *
 * --- DRY RUN 1: A* on Example A (3 x 4 grid, goal = (2, 3)) ---
 * Directions order: Right (0,1), Down (1,0), Left (0,-1), Up (-1,0).
 * Tie-breaker: smaller f first; if f ties, LARGER g (smaller h) first.
 *
 * Step | Popped (r,c, g,h,f) | Action / Neighbors Pushed               | PQ after step
 * -----+---------------------+-----------------------------------------+----------------------------------
 * Init | -                   | Push start (0,0): g=0, h=5, f=5         | [(0,0 f=5 g=0)]
 *   1  | (0,0, g=0, h=5, f=5)| Push (0,1 g=1 h=4 f=5), (1,0 g=1 h=4 f=5)| [(0,1 f=5 g=1), (1,0 f=5 g=1)]
 *   2  | (0,1, g=1, h=4, f=5)| (0,2)=#, (1,1)=#, (0,0) worse -> deadend| [(1,0 f=5 g=1)]
 *   3  | (1,0, g=1, h=4, f=5)| (1,1)=#, push (2,0 g=2 h=3 f=5)         | [(2,0 f=5 g=2)]
 *   4  | (2,0, g=2, h=3, f=5)| Push (2,1 g=3 h=2 f=5)                  | [(2,1 f=5 g=3)]
 *   5  | (2,1, g=3, h=2, f=5)| Push (2,2 g=4 h=1 f=5)                  | [(2,2 f=5 g=4)]
 *   6  | (2,2, g=4, h=1, f=5)| Push (2,3 g=5 h=0 f=5), (1,2 g=5 h=2 f=7)| [(2,3 f=5 g=5), (1,2 f=7 g=5)]
 *   7  | (2,3, g=5, h=0, f=5)| GOAL POPPED! Return 5. ((1,2) untouched)| -
 *
 * --- DRY RUN 2: Why Greedy Fails vs. Why A* Succeeds on Example B (5 x 5) ---
 * Goal = (4, 4), h(0,0) = 8.
 *
 * Greedy Best-First (orders ONLY by h):
 *   1. Pop (0,0 h=8) -> pushes (0,1 h=7), (1,0 h=7).
 *   2. Pop (0,1 h=7) -> chases right/down: (0,2 h=6) -> (0,3 h=5) -> (0,4 h=4)
 *      -> (1,4 h=3) -> (2,4 h=2). Notice (1,0 h=7) sits untouched in PQ!
 *   3. Pop (2,4 h=2) -> (3,4) is wall! Pushes (2,3 h=3).
 *      PQ has [(2,3 h=3), (1,0 h=7)]. Since 3 < 7, Greedy STILL ignores (1,0)!
 *   4. Follows (2,3 h=3) -> (2,2 h=4) -> (3,2 h=3) -> (4,2 h=2) -> (4,3 h=1)
 *      -> (4,4 h=0). Returns 12 (WRONG! Optimal is 8).
 *
 * A* (orders by f = g + h):
 *   1. Pop (0,0 g=0 h=8 f=8) -> pushes (0,1 g=1 h=7 f=8), (1,0 g=1 h=7 f=8).
 *   2. Follows top path to (2,4 g=6 h=2 f=8). PQ still has (1,0 g=1 h=7 f=8).
 *   3. Pop (2,4 g=6 h=2 f=8) -> pushes (2,3 g=7 h=3 f=10!).
 *      PQ is now [(1,0 g=1 h=7 f=8), (2,3 g=7 h=3 f=10)].
 *   4. THE PIVOT: f=8 < f=10! A* immediately abandons the trap and pops (1,0)!
 *   5. Walks straight down col 0 & right across row 4, all with f=8:
 *      (1,0)->(2,0)->(3,0)->(4,0)->(4,1)->(4,2)->(4,3)->(4,4 g=8 h=0 f=8).
 *   6. Goal popped with g = 8 (OPTIMAL!).
 *
 * --- PITFALLS ---
 * 1. Marking "visited" on PUSH instead of checking gScore on POP:
 *    Works in unweighted BFS, but in Dijkstra/A* a cell can be pushed with a
 *    worse g before a better path discovers it. Always relax via
 *    `if (tentativeG < gScore[nr][nc])` and discard stale heap entries on pop
 *    via `if (curr.g > gScore[curr.r][curr.c]) continue;`.
 * 2. Calling pq.remove(node) to simulate decrease-key:
 *    Java's PriorityQueue.remove(Object) is O(N), turning A* into O(V^2)!
 *    Always push a duplicate entry and skip stale ones on pop (lazy deletion).
 * 3. Using int[] as a HashSet/HashMap key in Java:
 *    Arrays inherit Object.equals/hashCode (reference identity, not contents!).
 *    Two `new int[]{1, 2}` objects will NOT match. Use a 2D primitive array
 *    `gScore[m][n]` (faster, zero autoboxing) or a `record` if using maps.
 * 4. Stopping when the goal is PUSHED instead of POPPED:
 *    In general A*\/Dijkstra, the first time you *see* the goal isn't always
 *    optimal (especially with weighted edges or inconsistent heuristics).
 *    Always return when the goal is *popped* from the PriorityQueue.
 * 5. Wrong heuristic for the movement model:
 *    Using Manhattan distance when diagonals are allowed overestimates true
 *    distance (inadmissible) and can return non-optimal paths.
 *
 * --- PATTERN RECOGNITION ---
 * "When you see shortest path from a known start to a single known target in
 *  a state space where you can compute a lower-bound distance to the target,
 *  think A* Search (f = g + h)."
 * Transfers directly to:
 *   1. Sliding Puzzle (8-puzzle / 15-puzzle): state = board config,
 *      h = sum of Manhattan distances of all tiles to their goal positions.
 *   2. Minimum Knight Moves / Game Pathfinding: state = (r, c),
 *      h = ceil(max(|dr|/2, |dc|/2, (|dr|+|dc|)/3)) or Octile distance.
 *   3. Weighted Terrain Shortest Path: same A* skeleton, replacing `+ 1` with
 *      `+ cost[nr][nc]` and scaling h by `minCellCost`.
 *
 * --- INTERVIEW SCRIPT (How to talk through this out loud) ---
 * "We want the shortest 4-directional path in an m x n grid up to 1000 x 1000.
 *  Backtracking DFS is exponential because it revisits cells along worse paths.
 *  Standard BFS solves this in O(m * n) by exploring level by level, but it
 *  expands blindly in all directions. Since we know the target is (m-1, n-1),
 *  we can guide the search with A*: we score each cell by f = g + h, where g
 *  is the exact steps taken so far and h is the Manhattan distance
 *  (m - 1 - r) + (n - 1 - c). Because every 4-directional step reduces
 *  Manhattan distance by at most 1, h is admissible and consistent — it never
 *  overestimates, so the first time we pop the goal from a min-heap ordered
 *  by f, its cost is guaranteed optimal. To avoid O(N) decrease-key in Java's
 *  PriorityQueue, I'll track the best g-score per cell in a 2D int array and
 *  use lazy deletion for stale heap entries. I'll also break f-ties by larger
 *  g so the search dives straight toward the goal on open sections. That gives
 *  us O((m * n) log(m * n)) time and O(m * n) space."
 *
 *
 * ============================================================================
 * 5. FOLLOW-UPS (Realistic interviewer extensions)
 * ============================================================================
 * Q1: What if 8-directional (diagonal) movement is allowed?
 * A1: Manhattan distance becomes INADMISSIBLE (e.g., (0,0)->(1,1) costs 1, but
 *     Manhattan says 2). If diagonals cost 1, switch h to Chebyshev distance:
 *     max(|dr|, |dc|). If diagonals cost sqrt(2), use Octile distance:
 *     max(|dr|, |dc|) + (sqrt(2) - 1) * min(|dr|, |dc|).
 *
 * Q2: How would you return the actual shortest path, not just its cost?
 * A2: Keep `int[][] parent = new int[m][n]` (initialized to -1). Whenever we
 *     update `gScore[nr][nc]`, store `parent[nr][nc] = curr.r * n + curr.c`.
 *     Once the goal is popped, walk backward from (m-1, n-1) to (0, 0) and
 *     reverse the list. Adds O(m * n) space, O(m * n) reconstruction time.
 *
 * Q3: What if cells have variable positive weights (e.g., road=1, mud=3)?
 * A3: BFS breaks completely, while A* needs only two changes:
 *     (1) `tentativeG = curr.g + weight[nr][nc]`, and
 *     (2) `h = minWeight * ((m - 1 - nr) + (n - 1 - nc))` so h stays admissible.
 *
 * Q4: What if we get thousands of (start, goal) queries on the SAME static grid?
 * A4: Use Bidirectional A* (search from both ends simultaneously), Jump Point
 *     Search (JPS) on uniform grids to skip open-space symmetry, or ALT
 *     (A* + Landmarks + Triangle inequality) by precomputing BFS distances from
 *     a small set of landmark cells.
 *
 * Q5: What if memory is tightly constrained on a massive grid?
 * A5: Use IDA* (Iterative Deepening A*), which runs depth-first searches bounded
 *     by an increasing f-threshold. Space drops to O(path_length), at the cost
 *     of re-expanding states across iterations.
 */
public class GridShortestPathAStar {

    // 4-directional movement vectors: Right, Down, Left, Up.
    // Putting Right and Down first is a subtle practical win when moving from
    // (0, 0) toward (m - 1, n - 1), though A*'s priority queue handles ordering.
    private static final int[][] DIRS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

    /**
     * Immutable search state stored in A*'s PriorityQueue.
     * Example: At cell (1, 0) on a 3x4 grid, after 1 step:
     *   r = 1, c = 0, g = 1, h = (2-1) + (3-0) = 4, f = 5.
     */
    private record Node(int r, int c, int g, int f) implements Comparable<Node> {
        @Override
        public int compareTo(Node other) {
            // Primary key: smallest estimated total cost f = g + h.
            int cmp = Integer.compare(this.f, other.f);
            if (cmp != 0) {
                return cmp;
            }
            // Tie-breaker: prefer LARGER g (equivalently SMALLER h).
            // Why? When many paths tie on f (common on open grids), preferring
            // cells closer to the goal dives straight to (m-1, n-1) instead of
            // expanding every cell in the equal-f diamond.
            return Integer.compare(other.g, this.g);
        }
    }

    // ========================================================================
    // APPROACH 4 (OPTIMAL / REQUIRED): A* Search with Manhattan Heuristic
    // ========================================================================
    /**
     * Finds the shortest 4-directional path cost from (0, 0) to (m - 1, n - 1)
     * using A* search with the Manhattan distance heuristic.
     *
     * Time Complexity:  O((m * n) log(m * n)) — at most 4 relaxations per cell,
     *                   each PriorityQueue offer/poll takes O(log(m * n)).
     * Space Complexity: O(m * n) — gScore[][] holds m * n ints and the PQ holds
     *                   O(m * n) nodes in the worst case.
     */
    public static int shortestPathAStar(int[][] grid) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) {
            return -1;
        }
        int m = grid.length;
        int n = grid[0].length;

        // Defensive guard: even though constraints state start & goal are open,
        // checking takes O(1) and prevents wrong answers on malformed inputs.
        if (grid[0][0] == 1 || grid[m - 1][n - 1] == 1) {
            return -1;
        }
        // Edge case: 1x1 grid — already at the goal, 0 steps needed.
        if (m == 1 && n == 1) {
            return 0;
        }

        // gScore[r][c] = lowest exact step count found so far from (0, 0) to (r, c).
        // Example on a 3x4 grid after step 1:
        //   gScore[0][0] = 0, gScore[0][1] = 1, gScore[1][0] = 1, all others = INF.
        int[][] gScore = new int[m][n];
        for (int[] row : gScore) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }
        gScore[0][0] = 0;

        // Open set: min-heap of frontier states ordered by f = g + h.
        // Example initial entry on a 3x4 grid: Node(r=0, c=0, g=0, f=5).
        PriorityQueue<Node> pq = new PriorityQueue<>();
        int startH = (m - 1) + (n - 1);
        pq.offer(new Node(0, 0, 0, startH));

        while (!pq.isEmpty()) {
            Node curr = pq.poll();

            // Lazy deletion: a shorter path to (curr.r, curr.c) may have been
            // found after this Node was pushed. Skip outdated heap entries so
            // we never pay O(N) for PriorityQueue.remove().
            if (curr.g > gScore[curr.r][curr.c]) {
                continue;
            }

            // Because Manhattan distance is consistent (h(u) <= 1 + h(v)),
            // the first time the goal is POPPED guarantees curr.g is optimal.
            if (curr.r == m - 1 && curr.c == n - 1) {
                return curr.g;
            }

            // Try all 4 orthogonal neighbors.
            int tentativeG = curr.g + 1;
            for (int[] dir : DIRS) {
                int nr = curr.r + dir[0];
                int nc = curr.c + dir[1];

                // Skip out-of-bounds cells and walls (1).
                if (nr < 0 || nr >= m || nc < 0 || nc >= n || grid[nr][nc] == 1) {
                    continue;
                }

                // Only relax and push if we found a strictly shorter path to (nr, nc).
                if (tentativeG < gScore[nr][nc]) {
                    gScore[nr][nc] = tentativeG;

                    // Admissible & consistent Manhattan heuristic to (m - 1, n - 1).
                    // No Math.abs needed since 0 <= nr <= m - 1 and 0 <= nc <= n - 1.
                    int h = (m - 1 - nr) + (n - 1 - nc);
                    pq.offer(new Node(nr, nc, tentativeG, tentativeG + h));
                }
            }
        }

        // Open set exhausted without popping the goal -> unreachable.
        return -1;
    }

    // ========================================================================
    // APPROACH 2 (INTERMEDIATE / CROSS-CHECK): Standard BFS
    // ========================================================================
    /**
     * Unweighted shortest path via BFS. Included to cross-check A* in main()
     * and to contrast worst-case O(m * n) time vs. A*'s goal-directed pruning.
     *
     * Time Complexity:  O(m * n) — every cell enters the queue at most once.
     * Space Complexity: O(m * n) — dist[][] array + BFS frontier queue.
     */
    public static int shortestPathBFS(int[][] grid) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) {
            return -1;
        }
        int m = grid.length;
        int n = grid[0].length;
        if (grid[0][0] == 1 || grid[m - 1][n - 1] == 1) {
            return -1;
        }
        if (m == 1 && n == 1) {
            return 0;
        }

        // dist[r][c] = shortest steps from (0, 0) to (r, c), or -1 if unvisited.
        // Example on 3x4 grid after wave 1: dist[0][0]=0, dist[0][1]=1, dist[1][0]=1.
        int[][] dist = new int[m][n];
        for (int[] row : dist) {
            Arrays.fill(row, -1);
        }

        // FIFO queue expands cells in strictly non-decreasing distance order.
        Deque<int[]> queue = new ArrayDeque<>();
        dist[0][0] = 0;
        queue.offer(new int[]{0, 0});

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int r = curr[0], c = curr[1];
            int nextDist = dist[r][c] + 1;

            for (int[] dir : DIRS) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                if (nr < 0 || nr >= m || nc < 0 || nc >= n
                        || grid[nr][nc] == 1 || dist[nr][nc] != -1) {
                    continue;
                }
                // In unweighted BFS, first discovery IS guaranteed shortest.
                if (nr == m - 1 && nc == n - 1) {
                    return nextDist;
                }
                dist[nr][nc] = nextDist;
                queue.offer(new int[]{nr, nc});
            }
        }

        return -1;
    }

    // ========================================================================
    // APPROACH 3 (DEMO ONLY — INCORRECT!): Greedy Best-First Search
    // ========================================================================
    /**
     * Intentionally flawed greedy search ordered ONLY by Manhattan distance h
     * (ignoring g). Included purely to demonstrate the Example B trap in main().
     */
    public static int shortestPathGreedyWrong(int[][] grid) {
        int m = grid.length, n = grid[0].length;
        if (grid[0][0] == 1 || grid[m - 1][n - 1] == 1) {
            return -1;
        }
        boolean[][] visited = new boolean[m][n];
        // BUG: Orders purely by h (stored in f field), completely ignoring g!
        PriorityQueue<Node> pq = new PriorityQueue<>((a, b) -> Integer.compare(a.f, b.f));
        visited[0][0] = true;
        pq.offer(new Node(0, 0, 0, (m - 1) + (n - 1)));

        while (!pq.isEmpty()) {
            Node curr = pq.poll();
            if (curr.r == m - 1 && curr.c == n - 1) {
                return curr.g;
            }
            for (int[] dir : DIRS) {
                int nr = curr.r + dir[0];
                int nc = curr.c + dir[1];
                if (nr < 0 || nr >= m || nc < 0 || nc >= n
                        || grid[nr][nc] == 1 || visited[nr][nc]) {
                    continue;
                }
                visited[nr][nc] = true;
                int h = (m - 1 - nr) + (n - 1 - nc);
                pq.offer(new Node(nr, nc, curr.g + 1, h));
            }
        }
        return -1;
    }

    // ========================================================================
    // TEST HARNESS & CROSS-CHECK
    // ========================================================================
    private static void verify(String name, int[][] grid, int expected) {
        int aStar = shortestPathAStar(grid);
        int bfs = shortestPathBFS(grid);
        boolean pass = (aStar == expected && bfs == expected);
        System.out.printf("%-30s | expected=%-4d | A*=%-4d | BFS=%-4d | %s%n",
                name, expected, aStar, bfs, pass ? "PASS" : "FAIL");
        if (!pass) {
            throw new AssertionError("Test failed: " + name);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Cross-Checking A* vs BFS ===");

        // 1. Dry Run 1 (3x4 typical grid)
        verify("Typical 3x4 (Dry Run 1)", new int[][]{
                {0, 0, 1, 0},
                {0, 1, 0, 0},
                {0, 0, 0, 0}
        }, 5);

        // 2. Dry Run 2 (5x5 Greedy trap)
        int[][] greedyTrap = {
                {0, 0, 0, 0, 0},
                {0, 1, 1, 1, 0},
                {0, 1, 0, 0, 0},
                {0, 1, 0, 1, 1},
                {0, 0, 0, 0, 0}
        };
        verify("Greedy Trap 5x5 (Dry Run 2)", greedyTrap, 8);
        System.out.println("  -> Note: Greedy Best-First on this 5x5 trap returns "
                + shortestPathGreedyWrong(greedyTrap) + " (WRONG; optimal is 8)");

        // 3. Edge case: 1x1 grid (start == goal)
        verify("Edge: 1x1 grid", new int[][]{{0}}, 0);

        // 4. Edge case: Single row open vs blocked
        verify("Edge: 1x5 open", new int[][]{{0, 0, 0, 0, 0}}, 4);
        verify("Edge: 1x5 blocked", new int[][]{{0, 0, 1, 0, 0}}, -1);

        // 5. Edge case: Single column open vs blocked
        verify("Edge: 4x1 open", new int[][]{{0}, {0}, {0}, {0}}, 3);
        verify("Edge: 4x1 blocked", new int[][]{{0}, {1}, {0}, {0}}, -1);

        // 6. Edge case: Unreachable (goal boxed in)
        verify("Edge: Unreachable", new int[][]{
                {0, 0, 0},
                {0, 1, 1},
                {0, 1, 0}
        }, -1);

        // 7. Edge case: Forced detour moving AWAY from goal (snake maze)
        verify("Edge: Snake detour (Up/Left)", new int[][]{
                {0, 1, 0, 0, 0},
                {0, 1, 0, 1, 0},
                {0, 1, 0, 1, 0},
                {0, 0, 0, 1, 0}
        }, 13);

        // 8. Stress test at max constraints: 1000 x 1000 open grid
        int[][] maxOpen = new int[1000][1000];
        long t0 = System.nanoTime();
        int aStarMax = shortestPathAStar(maxOpen);
        long t1 = System.nanoTime();
        int bfsMax = shortestPathBFS(maxOpen);
        long t2 = System.nanoTime();

        System.out.printf("Stress: 1000x1000 open grid    | expected=1998 | A*=%-4d (%.2f ms) | BFS=%-4d (%.2f ms)%n",
                aStarMax, (t1 - t0) / 1e6, bfsMax, (t2 - t1) / 1e6);
        if (aStarMax != 1998 || bfsMax != 1998) {
            throw new AssertionError("Stress test failed!");
        }
        System.out.println("All tests passed!");
    }
}

/*
 * ============================================================================
 * 7. SHORT SUMMARY
 * ============================================================================
 * - Core pattern:       Informed Shortest Path (A* Search with f = g + h).
 * - Key observation:    Manhattan distance h = (m-1-r) + (n-1-c) never
 *                       overestimates 4-directional cost and changes by at most
 *                       1 per step (admissible + consistent), so popping the
 *                       goal first guarantees optimality.
 * - Memorize vs. Know:  Don't memorize the boilerplate — understand WHY f=g+h
 *                       fixes Greedy (penalizing detours via g) and WHY we
 *                       use lazy deletion with gScore[][] in Java's PQ.
 * - Most common trap:   Marking cells visited on PUSH (breaks optimality when
 *                       a better path arrives later) or calling pq.remove()
 *                       (degrades runtime to O(V^2)).
 * - One-line trigger:   "Known goal + lower-bound distance -> A* with f = g + h,
 *                       relax on gScore, settle on pop."
 */



















import java.util.*;

class Solution {

    /*
     * A search state represents one candidate cell.
     *
     * row, col = Current cell coordinates.
     * g        = Actual cost from start to this cell.
     * f        = g + h, estimated total path cost.
     *
     * Records are useful because search states are immutable
     * and we want a concise data carrier.
     */
    private record State(
            int row,
            int col,
            int g,
            int f
    ) {}

    /*
     * Four possible movement directions:
     * Right, Left, Down, Up.
     */
    private static final int[][] DIRS = {
            {0, 1},
            {0, -1},
            {1, 0},
            {-1, 0}
    };

    public int shortestPath(int[][] grid) {

        int m = grid.length;
        int n = grid[0].length;

        /*
         * WHY: We need the best known actual distance from
         * the start to every cell.
         *
         * Initially, every cell is considered unreachable.
         */
        int[][] dist = new int[m][n];

        for (int[] row : dist) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }

        /*
         * WHY: A* must always expand the state with the
         * smallest estimated total cost f = g + h.
         *
         * Java PriorityQueue is a min-heap when we use
         * Comparator.comparingInt.
         *
         * The comparison is O(log V) per insertion/removal.
         */
        PriorityQueue<State> pq = new PriorityQueue<>(
                Comparator.comparingInt(State::f)
        );

        /*
         * WHY: Starting cell has actual cost 0.
         *
         * Manhattan distance provides the initial heuristic.
         */
        dist[0][0] = 0;

        pq.offer(new State(
                0,
                0,
                0,
                heuristic(0, 0, m, n)
        ));

        while (!pq.isEmpty()) {

            /*
             * WHY: Expand the state having the smallest f value.
             */
            State current = pq.poll();

            int r = current.row();
            int c = current.col();
            int g = current.g();

            /*
             * WHY: PriorityQueue does not support decrease-key.
             *
             * Therefore, when we discover a better path to a cell,
             * we insert a new state instead of updating the old one.
             *
             * This can leave outdated states in the queue.
             *
             * We skip them here.
             */
            if (g != dist[r][c]) {
                continue;
            }

            /*
             * WHY: Manhattan distance is admissible and consistent.
             * With these properties, when the destination is removed
             * from the priority queue with its current best g value,
             * that g value is the shortest possible path cost.
             *
             * Therefore, we can return immediately.
             */
            if (r == m - 1 && c == n - 1) {
                return g;
            }

            /*
             * Explore all four neighboring cells.
             */
            for (int[] dir : DIRS) {

                int nr = r + dir[0];
                int nc = c + dir[1];

                /*
                 * WHY: Ignore coordinates outside the grid.
                 */
                if (nr < 0 || nr >= m ||
                    nc < 0 || nc >= n) {
                    continue;
                }

                /*
                 * WHY: Walls cannot be traversed.
                 */
                if (grid[nr][nc] == 1) {
                    continue;
                }

                /*
                 * WHY: Every movement costs exactly 1.
                 *
                 * So the candidate distance is current g + 1.
                 */
                int newG = g + 1;

                /*
                 * WHY: Only update a cell if we found a strictly
                 * better path to it.
                 *
                 * This avoids unnecessary queue insertions.
                 */
                if (newG < dist[nr][nc]) {

                    dist[nr][nc] = newG;

                    /*
                     * Calculate:
                     *
                     * h = Manhattan distance to destination.
                     * f = g + h.
                     */
                    int h = heuristic(nr, nc, m, n);

                    int f = newG + h;

                    /*
                     * Insert the improved state.
                     */
                    pq.offer(new State(
                            nr,
                            nc,
                            newG,
                            f
                    ));
                }
            }
        }

        /*
         * WHY: If the queue becomes empty, every reachable cell
         * has been explored and the destination was never reached.
         */
        return -1;
    }

    /*
     * Manhattan distance:
     *
     * h(r, c) = |r - goalRow| + |c - goalCol|
     *
     * WHY is this admissible?
     *
     * We can only move horizontally or vertically.
     *
     * Therefore, we must make at least:
     * - |r - goalRow| vertical moves.
     * - |c - goalCol| horizontal moves.
     *
     * Obstacles can increase the required distance,
     * but cannot decrease it.
     *
     * Hence, h never overestimates the actual shortest distance.
     *
     * WHY is it consistent?
     *
     * For every neighboring cell u and v:
     *
     * h(u) <= cost(u, v) + h(v)
     *
     * Since one move changes Manhattan distance by at most 1,
     * this condition always holds.
     */
    private int heuristic(
            int r,
            int c,
            int m,
            int n
    ) {

        return Math.abs(r - (m - 1))
                + Math.abs(c - (n - 1));
    }
}
