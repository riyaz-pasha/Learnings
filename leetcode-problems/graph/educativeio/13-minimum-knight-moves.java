/**
 * ============================================================================
 * MINIMUM KNIGHT MOVES - STUDY NOTES
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ------------------------------------------------
 * Q: Is the chessboard genuinely infinite, meaning I don't need to bounds-check against 8x8?
 *    -> Why: Confirms we only bounds-check for optimization, not for game rules.
 * Q: Can the coordinates (x, y) be negative? 
 *    -> Why: Dictates whether we need to handle all 4 quadrants or can simplify the space.
 * Q: Are we guaranteed that the target is reachable?
 *    -> Why: A knight can reach any square on an infinite board, but asking shows mathematical rigor.
 * Q: Do the x and y coordinates fit in standard integer limits?
 *    -> Why: Prevents integer overflow during distance or target calculations (Constraints say <= 300, so safe).
 *
 *
 * 2. THE REASONING JOURNEY
 * ------------------------------------------------
 * The Core Challenge: We need the *shortest path* on an *unweighted graph* (all moves cost 1). 
 * The binding constraint isn't the board size, but the explosive branching factor. A knight has 
 * 8 possible moves. At depth D, we have 8^D paths. Unchecked, this causes Time Limit Exceeded (TLE) 
 * and memory exhaustion.
 *
 * Approach 1: Naive Breadth-First Search (BFS)
 * - What I'd try: A standard queue-based BFS starting at (0,0), exploring all 8 directions, 
 *   using a `Set<String>` for visited coordinates.
 * - Why it works: BFS inherently finds the shortest path in unweighted graphs.
 * - Why it's too slow: It searches radially outwards in all 360 degrees. To reach (300, 300), 
 *   it explores hundreds of thousands of squares going in the completely wrong direction (e.g., into 
 *   the negative quadrants). 
 * - What work is repeated: We spend time calculating paths moving *away* from the target.
 *
 * Approach 2: Pruned First-Quadrant BFS (The Iterative Sweet Spot)
 * - The Observation (Symmetry): A chessboard is symmetric across both axes and diagonals. Reaching 
 *   (-5, 7) takes the exact same moves as reaching (5, 7). 
 * - What I'd try: Immediately take `x = Math.abs(x)` and `y = Math.abs(y)`. Now our target is strictly 
 *   in the first quadrant. During BFS, we only enqueue next positions if they are >= -2. 
 *   (Why -2? To reach (1,1) in 2 moves, the knight must temporarily step slightly "out of bounds" 
 *   to (-1, 2) before coming back).
 * - Why it works: We eliminate 3 out of 4 quadrants from our search space.
 * - Time & Space: O(x * y) because we only explore a rectangle bounded roughly by the target coordinates.
 *
 * Approach 3: Top-Down DP / Depth-First Search with Memoization (The Optimum)
 * - The Observation (Work Backwards): Instead of starting at (0,0) and branching out, what if we start 
 *   at (x,y) and work *backwards* to (0,0)? If we only allow moves that take us *closer* to the origin, 
 *   our 8 choices collapse into just 2 choices: `(x-1, y-2)` and `(x-2, y-1)`.
 * - What I'd try: A recursive function `dfs(x, y) = 1 + min(dfs(x-1, y-2), dfs(x-2, y-1))`. 
 *   To handle the edge case where a knight temporarily overshoots the axes (e.g., reaching 0,0 from 1,1), 
 *   we use absolute values on the recursive calls: `Math.abs(x-1)`.
 * - Time & Space: O(x * y) worst case, but the constant factor is minuscule because we strictly move 
 *   towards the origin. The branching factor drops from 8 to 2. Space is O(x * y) for the memoization array.
 * - Decision: I would write Approach 3. It's incredibly elegant, requires far less code than a bounded BFS, 
 *   and shows a deep understanding of mathematical problem simplification.
 *
 *
 * 3. ADDITIONAL INSIGHTS
 * ------------------------------------------------
 * Key Insight - "The Folding Board":
 * By using `Math.abs()` on the coordinates in our recursive DFS calls, we don't just handle negative 
 * targets—we dynamically "fold" the infinite chessboard over itself. If a knight steps into the negative 
 * coordinates to make an optimal turn, `Math.abs()` instantly mirrors it back into the first quadrant. 
 * We never have to represent negative array indices!
 *
 * Base Cases & The Infinite Loop Pitfall:
 * You must define base cases carefully to stop the recursion.
 * 1. Target reached: `if (x == 0 && y == 0) return 0;`
 * 2. The Danger Zone: `if (x + y == 2) return 2;`
 *    -> Why? Consider reaching (1, 1). The shortest path is (0,0) -> (2,1) -> (1,1) (2 moves). 
 *       If we let the recursion run `dfs(1,1)`, it calls `dfs(abs(1-2), abs(1-1))` = `dfs(1,0)`.
 *       Then `dfs(1,0)` calls `dfs(abs(1-1), abs(0-2))` = `dfs(0,2)`.
 *       Then `dfs(0,2)` calls `dfs(abs(0-2), abs(2-1))` = `dfs(2,1)`... which mirrors back to `(1,2)`.
 *       The knight dances around the origin forever! Forcing `x+y == 2` to return 2 breaks this loop.
 *
 * Dry Run: dfs(2, 1)
 * 1. dfs(2, 1): x+y = 3. Not a base case.
 * 2. Branch 1: dfs(|2-1|, |1-2|) -> dfs(1, 1). 
 * 3. Branch 2: dfs(|2-2|, |1-1|) -> dfs(0, 0).
 * 4. Evaluate dfs(1, 1): x+y == 2. Returns 2.
 * 5. Evaluate dfs(0, 0): x+y == 0. Returns 0.
 * 6. dfs(2, 1) = 1 + min(2, 0) = 1. (Correct! (0,0) to (2,1) is exactly 1 knight move).
 *
 * Pattern Recognition:
 * - Deterministic, symmetric game states -> Work backwards (Top-Down DP) to aggressively prune branches.
 * - Similar Problems: "Word Ladder II" (working backwards from target), "Target Sum" (memoization).
 *
 * Interview Script Snippet:
 * "A standard BFS will explore radially and waste massive amounts of time moving away from the target. 
 * Since the board is symmetric, reaching (-X, Y) is the same as reaching (X, Y). I'll take the absolute 
 * value of the target to stay in Quadrant 1. Furthermore, instead of branching out 8 ways from the start, 
 * I'll work backwards from the target. By only considering the 2 moves that bring us closer to the origin, 
 * I reduce the branching factor from 8 to 2. I'll memoize the states in a 2D array to achieve O(x*y) time."
 *
 * 5. SUMMARY
 * ------------------------------------------------
 * - Core pattern: Top-Down DP / DFS with Memoization (working backwards).
 * - Key observation: Absolute values "fold" the board, allowing us to safely bounce off axes without 
 *   tracking negative coordinates.
 * - Most common trap: Missing the `x+y == 2` base case, causing an infinite recursion cycle near the origin.
 * - Mental trigger: "Infinite symmetric board -> Work backwards, use absolute values."
 */

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class MinimumKnightMoves {

    /**
     * OPTIMAL APPROACH: Top-Down DP (Memoization)
     * Time Complexity: O(|x| * |y|) - bounded by the rectangle between origin and target.
     * Space Complexity: O(|x| * |y|) - for the memoization array.
     */
    public int minKnightMoves(int x, int y) {
        // Exploit symmetry: target is mirrored into the first quadrant.
        x = Math.abs(x);
        y = Math.abs(y);
        
        // Since max x and y are 300, our coordinates going backwards will practically 
        // never exceed 302. Size 310 is exceptionally safe to prevent out-of-bounds.
        int[][] memo = new int[310][310];
        
        // Fill memo with -1 to denote uncalculated states
        for (int[] row : memo) {
            Arrays.fill(row, -1);
        }
        
        return dfs(x, y, memo);
    }

    private int dfs(int x, int y, int[][] memo) {
        // Base Case 1: We've reached the origin
        if (x + y == 0) {
            return 0;
        }
        
        // Base Case 2: The "Danger Zone" ring around the origin
        // Coordinates (1,1), (0,2), and (2,0) all take exactly 2 moves to reach.
        // Intercepting them here prevents the knight from infinitely dancing around the origin.
        if (x + y == 2) {
            return 2;
        }
        
        // Return cached result if we've already solved for this coordinate
        if (memo[x][y] != -1) {
            return memo[x][y];
        }
        
        // Recursive Step: We only evaluate the 2 directions that pull us CLOSER to (0,0).
        // Using Math.abs allows the knight to temporarily step slightly negative 
        // (which mathematically just bounces/mirrors it back into our valid array space).
        int path1 = dfs(Math.abs(x - 1), Math.abs(y - 2), memo);
        int path2 = dfs(Math.abs(x - 2), Math.abs(y - 1), memo);
        
        // Store the minimum of the two paths + 1 (for the current move)
        memo[x][y] = 1 + Math.min(path1, path2);
        
        return memo[x][y];
    }

    /**
     * ALTERNATIVE APPROACH: First-Quadrant Pruned BFS
     * (Included for completeness - this is often the expected answer if DP doesn't click)
     * Time Complexity: O(|x| * |y|)
     * Space Complexity: O(|x| * |y|)
     */
    public int minKnightMovesBFS(int x, int y) {
        x = Math.abs(x);
        y = Math.abs(y);
        
        Queue<int[]> queue = new LinkedList<>();
        // Visited array needs a +2 offset because our bounds allow dipping to -2
        boolean[][] visited = new boolean[310][310];
        
        // Start at 0,0. Note: offset coordinates by +2 in the visited array.
        queue.offer(new int[]{0, 0});
        visited[2][2] = true; // (0+2, 0+2)
        
        // All 8 possible knight moves
        int[][] directions = {{1,2},{2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2}};
        int moves = 0;
        
        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                int[] curr = queue.poll();
                int currX = curr[0];
                int currY = curr[1];
                
                if (currX == x && currY == y) {
                    return moves;
                }
                
                for (int[] dir : directions) {
                    int nextX = currX + dir[0];
                    int nextY = currY + dir[1];
                    
                    // Pruning: We only allow coords >= -2 to prevent searching negative quadrants.
                    // Max bound is arbitrarily ~305 since target <= 300.
                    if (nextX >= -2 && nextY >= -2 && nextX < 305 && nextY < 305) {
                        if (!visited[nextX + 2][nextY + 2]) {
                            visited[nextX + 2][nextY + 2] = true;
                            queue.offer(new int[]{nextX, nextY});
                        }
                    }
                }
            }
            moves++;
        }
        return -1;
    }

    // ============================================================================
    // MAIN METHOD - TEST CASES
    // ============================================================================
    public static void main(String[] args) {
        MinimumKnightMoves solver = new MinimumKnightMoves();

        System.out.println("--- Testing Optimal Top-Down DP ---");

        // Test Case 1: Origin (0 moves)
        System.out.println("Target (0, 0) - Expected: 0, Got: " + solver.minKnightMoves(0, 0));

        // Test Case 2: One move away
        System.out.println("Target (2, 1) - Expected: 1, Got: " + solver.minKnightMoves(2, 1));

        // Test Case 3: The Danger Zone (Requires moving away to move closer)
        System.out.println("Target (1, 1) - Expected: 2, Got: " + solver.minKnightMoves(1, 1));

        // Test Case 4: General Example
        System.out.println("Target (5, 5) - Expected: 4, Got: " + solver.minKnightMoves(5, 5));

        // Test Case 5: Large Coordinates (Constraint limits)
        System.out.println("Target (300, 300) - Expected: 200, Got: " + solver.minKnightMoves(300, 300));
        
        // Test Case 6: Negative Coordinates (Symmetry handles this)
        System.out.println("Target (-2, -1) - Expected: 1, Got: " + solver.minKnightMoves(-2, -1));


        System.out.println("\n--- Testing Pruned BFS ---");
        System.out.println("Target (5, 5) - Expected: 4, Got: " + solver.minKnightMovesBFS(5, 5));
        System.out.println("Target (1, 1) - Expected: 2, Got: " + solver.minKnightMovesBFS(1, 1));
    }
}

import java.util.*;

/**
 * ============================================================
 *              MINIMUM KNIGHT MOVES (ALL APPROACHES)
 * ============================================================
 *
 * Problem:
 * --------
 * Given (x, y), find minimum knight moves from (0,0)
 *
 * Knight moves:
 * (±2, ±1), (±1, ±2)
 *
 * Constraints:
 * -300 ≤ x, y ≤ 300
 *
 * ============================================================
 * 🧠 INTERVIEW THINKING FLOW
 * ============================================================
 *
 * Step 1: This is a graph problem
 *   → Each coordinate = node
 *   → Each move = edge
 *
 * Step 2: All edges have equal weight (1 move)
 *   → Use BFS for shortest path
 *
 * Step 3: Infinite board → optimize
 *   → Use symmetry (reduce to 1 quadrant)
 *   → Bound the search space
 *
 * Step 4: Advanced optimizations
 *   → Bidirectional BFS
 *   → Mathematical pattern
 *   → A* search
 *
 * ============================================================
 * 🚀 INCLUDED APPROACHES
 * ============================================================
 *
 * 1. BFS (Standard)
 * 2. Bidirectional BFS (Faster)
 * 3. Mathematical (O(1))
 * 4. A* Search (Heuristic based)
 *
 * ============================================================
 */
public class MinimumKnightMovesAll {

    // All 8 knight moves
    private static final int[][] DIRS = {
        {2, 1}, {1, 2}, {-1, 2}, {-2, 1},
        {-2, -1}, {-1, -2}, {1, -2}, {2, -1}
    };

    /* ============================================================
     * 🟢 APPROACH 1: BFS (Most Important for Interviews)
     * ============================================================
     *
     * Idea:
     * -----
     * Treat grid as graph and run BFS from (0,0)
     *
     * Optimizations:
     * --------------
     * 1. Symmetry: convert (x, y) → (abs(x), abs(y))
     * 2. Bound search space → avoid infinite expansion
     *
     * Time:  O(x * y)
     * Space: O(x * y)
     *
     * ============================================================
     */
    public int bfs(int x, int y) {

        // ---- Step 1: Symmetry ----
        x = Math.abs(x);
        y = Math.abs(y);

        Queue<int[]> queue = new ArrayDeque<>();
        queue.offer(new int[]{0, 0});

        Set<String> visited = new HashSet<>();
        visited.add("0,0");

        int steps = 0;

        while (!queue.isEmpty()) {

            int size = queue.size();

            // Process one BFS level (all nodes at same distance)
            for (int i = 0; i < size; i++) {

                int[] curr = queue.poll();
                int cx = curr[0];
                int cy = curr[1];

                // 🎯 Target reached
                if (cx == x && cy == y) {
                    return steps;
                }

                // Explore all 8 moves
                for (int[] d : DIRS) {
                    int nx = cx + d[0];
                    int ny = cy + d[1];

                    // ---- Pruning ----
                    // Don't go too far negative
                    if (nx < -2 || ny < -2) continue;

                    // Don't go too far positive
                    if (nx > x + 2 || ny > y + 2) continue;

                    String key = nx + "," + ny;

                    if (!visited.contains(key)) {
                        visited.add(key);
                        queue.offer(new int[]{nx, ny});
                    }
                }
            }

            steps++;
        }

        return -1;
    }

    /* ============================================================
     * 🔵 APPROACH 2: BIDIRECTIONAL BFS
     * ============================================================
     *
     * Idea:
     * -----
     * Start BFS from:
     *   → source (0,0)
     *   → target (x,y)
     *
     * Stop when both meet
     *
     * Why Faster?
     * ----------
     * Instead of exploring O(N²), we explore O(N)
     *
     * Time:  O(smaller search space)
     * Space: O(x * y)
     *
     * ============================================================
     */
    public int bidirectionalBFS(int x, int y) {

        x = Math.abs(x);
        y = Math.abs(y);

        Set<String> start = new HashSet<>();
        Set<String> end = new HashSet<>();
        Set<String> visited = new HashSet<>();

        start.add("0,0");
        end.add(x + "," + y);

        int steps = 0;

        while (!start.isEmpty()) {

            // Always expand smaller frontier
            if (start.size() > end.size()) {
                Set<String> temp = start;
                start = end;
                end = temp;
            }

            Set<String> next = new HashSet<>();

            for (String pos : start) {

                if (end.contains(pos)) return steps;

                String[] parts = pos.split(",");
                int cx = Integer.parseInt(parts[0]);
                int cy = Integer.parseInt(parts[1]);

                for (int[] d : DIRS) {
                    int nx = cx + d[0];
                    int ny = cy + d[1];

                    if (nx < -2 || ny < -2) continue;
                    if (nx > x + 2 || ny > y + 2) continue;

                    String key = nx + "," + ny;

                    if (!visited.contains(key)) {
                        visited.add(key);
                        next.add(key);
                    }
                }
            }

            start = next;
            steps++;
        }

        return -1;
    }

    /* ============================================================
     * 🟣 APPROACH 3: MATHEMATICAL SOLUTION (O(1))
     * ============================================================
     *
     * Key Insight:
     * ------------
     * Knight movement follows pattern
     *
     * Special Cases:
     * --------------
     * (1,0) → 3
     * (2,2) → 4
     *
     * Formula:
     * --------
     * d = max( (x+1)/2 , (x+y+2)/3 )
     *
     * Then adjust parity
     *
     * Time:  O(1)
     * Space: O(1)
     *
     * ============================================================
     */
    public int math(int x, int y) {

        x = Math.abs(x);
        y = Math.abs(y);

        // Ensure x >= y
        if (x < y) {
            int temp = x;
            x = y;
            y = temp;
        }

        // Special edge cases
        if (x == 1 && y == 0) return 3;
        if (x == 2 && y == 2) return 4;

        int d = Math.max((x + 1) / 2, (x + y + 2) / 3);

        // Parity correction
        if ((d % 2) != ((x + y) % 2)) {
            d++;
        }

        return d;
    }

    /* ============================================================
     * 🟡 APPROACH 4: A* SEARCH (HEURISTIC BASED)
     * ============================================================
     *
     * Idea:
     * -----
     * Use priority queue with heuristic
     *
     * f(n) = g(n) + h(n)
     *
     * g(n) = steps so far
     * h(n) = estimated distance to target
     *
     * Heuristic:
     * ----------
     * Use manhattan / max(dx, dy)
     *
     * Time: Faster than BFS in practice
     *
     * ============================================================
     */
    public int aStar(int x, int y) {

        x = Math.abs(x);
        y = Math.abs(y);

        record Node(int x, int y, int steps, int cost) {}

        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.cost));

        pq.offer(new Node(0, 0, 0, heuristic(0, 0, x, y)));

        Set<String> visited = new HashSet<>();

        while (!pq.isEmpty()) {

            Node curr = pq.poll();

            int cx = curr.x;
            int cy = curr.y;

            if (cx == x && cy == y) return curr.steps;

            String key = cx + "," + cy;
            if (visited.contains(key)) continue;
            visited.add(key);

            for (int[] d : DIRS) {
                int nx = cx + d[0];
                int ny = cy + d[1];

                if (nx < -2 || ny < -2) continue;
                if (nx > x + 2 || ny > y + 2) continue;

                int newSteps = curr.steps + 1;
                int cost = newSteps + heuristic(nx, ny, x, y);

                pq.offer(new Node(nx, ny, newSteps, cost));
            }
        }

        return -1;
    }

    // Heuristic function for A*
    private int heuristic(int x, int y, int tx, int ty) {
        int dx = Math.abs(tx - x);
        int dy = Math.abs(ty - y);

        // Simple heuristic
        return Math.max(dx, dy) / 2;
    }

    /* ============================================================
     * 🧪 TRACE EXAMPLE (VERY IMPORTANT FOR INTERVIEWS)
     * ============================================================
     *
     * Example: x = 2, y = 1
     *
     * Level 0:
     *   (0,0)
     *
     * Level 1:
     *   (2,1) ← FOUND
     *
     * Answer = 1
     *
     * ------------------------------------------------------------
     *
     * Example: x = 5, y = 5
     *
     * Level 0:
     *   (0,0)
     *
     * Level 1:
     *   8 positions
     *
     * Level 2:
     *   32 positions
     *
     * ...
     *
     * Eventually reaches (5,5)
     *
     * ============================================================
     */

    /* ============================================================
     * 🧾 FINAL NOTES
     * ============================================================
     *
     * BFS → safest for interview
     * Bidirectional BFS → optimization
     * Math → best performance (but harder to derive)
     * A* → good for large grids
     *
     * ============================================================
     */
}
