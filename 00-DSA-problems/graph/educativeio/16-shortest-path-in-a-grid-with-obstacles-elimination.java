/**
 * ============================================================================
 * SHORTEST PATH IN A GRID WITH OBSTACLES ELIMINATION - STUDY NOTES
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ------------------------------------------------
 * Q: Can we move diagonally?
 *    -> Why: Determines the branching factor of our traversal. (Rule: No, only 4-directional).
 * Q: Are the start (0,0) and end (m-1,n-1) guaranteed to be empty (0)?
 *    -> Why: Prevents instant failure conditions at the exact boundaries. (Rule: Yes, guaranteed 0).
 * Q: Is k always positive, and does it have an upper bound?
 *    -> Why: If k is massively large, we might not need to search at all. (Rule: 1 <= k <= m*n).
 * Q: Can we revisit cells if we come back with a better remaining 'k'?
 *    -> Why: Clarifies how "visited" states work. A cell isn't just visited; it's visited *with a specific budget*.
 *
 *
 * 2. THE REASONING JOURNEY
 * ------------------------------------------------
 * The Core Challenge: We need the shortest path in an unweighted grid, which normally screams BFS. 
 * However, the binding constraint is the "budget" of k obstacles. A standard BFS fails because arriving 
 * at a cell via a longer path might actually be *better* if that path preserves more of the 'k' budget 
 * for future walls. 
 *
 * Approach 1: Depth-First Search (DFS) with Backtracking
 * - What I'd try: Start at (0,0). Recursively try all 4 directions. Pass along the `steps` taken and the 
 *   `k` remaining. Keep track of the minimum steps to reach the end.
 * - Why it works: It exhaustively explores every possible path and obstacle-breaking combination.
 * - Why it's too slow: It blindly explores the same cells repeatedly through different meandering paths.
 * - Time Complexity: O(4^(m*n)) — because at each of the m*n cells, we can branch in 4 directions, leading 
 *   to exponential path combinations.
 * - Space Complexity: O(m*n) — because the deepest the recursion stack can go is visiting every cell once 
 *   in a snake-like pattern.
 *
 * Approach 2: State-Space BFS (3D Boolean Array)
 * - What work is being repeated in DFS: We recalculate paths from a cell even if we've already reached 
 *   that exact cell with the exact same budget of `k`.
 * - What property removes the bottleneck: The graph state isn't just 2D `(row, col)`. It's 3D: `(row, col, k)`. 
 *   If we use a Queue for BFS and a `boolean[m][n][k+1]` visited array, the *first* time we reach any state 
 *   `(r, c, current_k)`, it is mathematically guaranteed to be the shortest path to that specific state.
 * - What I'd try: Push `(0, 0, initial_k)` to a Queue. Expand level by level. Mark `visited[r][c][current_k] = true`.
 * - Time Complexity: O(m * n * k) — because the total number of unique states in our 3D graph is m * n * k, 
 *   and we process each state exactly once (exploring 4 neighbors).
 * - Space Complexity: O(m * n * k) — because we store a 3D boolean array of this size, and the Queue can 
 *   hold up to O(m * n * k) elements in the worst-case wide graph.
 *
 * Approach 3: Optimized State-Space BFS (2D int Array & Manhattan Early Exit) - THE OPTIMUM
 * - What property removes the bottleneck: We don't actually care about reaching a cell with *every* possible `k`. 
 *   If we reach `(r, c)` with `k=3`, we absolutely don't care if we later reach it with `k=1` in the same number 
 *   of steps. We only ever want to process a cell if the `k` we bring to it is *strictly greater* than the best 
 *   `k` we've ever brought to it before. 
 *   Furthermore, if our starting `k` is large enough to blast through every wall in a straight line, we don't 
 *   need to search at all!
 * - What I'd try: Use an `int[][] visited` array initialized to -1. It stores the *maximum k* we had when we 
 *   arrived at `(r, c)`. We only push to the Queue if `next_k > visited[r][c]`. Also, add an O(1) early exit 
 *   if `k >= m + n - 2` (the Manhattan distance).
 * - Time Complexity: O(m * n * k) — because although heavily pruned, in a worst-case maze, we might visit a 
 *   cell multiple times if a longer path yields a strictly higher `k`.
 * - Space Complexity: O(m * n) — because we compressed the 3D boolean array into a 2D integer array, drastically 
 *   saving memory. The queue is also implicitly kept much smaller by the aggressive pruning.
 * - Decision: Approach 3 is what I'd write in an interview. The 2D array is much cleaner to initialize and 
 *   reason about, and the Manhattan shortcut shows strong problem-awareness.
 *
 *
 * 3. EDGE CASES
 * ------------------------------------------------
 * - Grid is 1x1: Start is the target. Needs 0 steps. Must be handled before any queue processing.
 * - Infinite Power (k >= m + n - 2): We have enough k to take the literal shortest geometric path, ignoring 
 *   all obstacles. Return `m + n - 2` instantly.
 * - Complete Blockade: A thick wall of 1s (e.g., 2 cells thick) and we only have k=1. Queue exhausts, returns -1.
 * - Zero Budget: k=0 initially. We can only walk on 0s. (Constraints say k>=1, but good to handle logically).
 *
 *
 * 4. KEY INSIGHT, DIAGRAM & DRY RUN
 * ------------------------------------------------
 * Key Insight: A standard BFS prevents revisiting a cell. A "State-Space" BFS prevents revisiting a *state*. 
 * By defining the state as "reaching cell (X, Y) with K power remaining", we map a complex conditional 
 * problem into a simple 3D unweighted graph. 
 *
 * ASCII Diagram (Why standard BFS fails):
 * [0, 1, 0, 0]
 * [0, 1, 0, 0]
 * [0, 0, 0, Target]
 * 
 * If k=1.
 * Path A (smash wall): (0,0) -> (0,1) -> (0,2) -> (0,3). Reaches (0,2) in 2 steps, k=0.
 * Path B (go around): (0,0) -> (1,0) -> (2,0) -> (2,1) -> (2,2) -> (1,2) -> (0,2). Reaches (0,2) in 6 steps, k=1.
 * 
 * If the target is heavily walled later, Path A fails because it has no `k` left. 
 * Path B succeeds. We MUST allow Path B to revisit (0,2) because it brings a better `k` (1 vs 0).
 *
 * Dry Run: 
 * Grid 3x3, k=1.
 * [0, 1, 1]
 * [1, 1, 1]
 * [1, 1, 0] Target is (2,2).
 * 
 * Init: visited = [[-1,-1,-1], [-1,-1,-1], [-1,-1,-1]]. Queue = [{r:0, c:0, k:1, steps:0}]. visited[0][0] = 1.
 * 1. Pop (0,0, k=1, steps=0). 
 *    - Go right to (0,1). It's a 1. next_k = 1 - 1 = 0. 0 > visited[0][1](-1). Push (0,1, k=0, steps=1). visited[0][1] = 0.
 *    - Go down to (1,0). It's a 1. next_k = 1 - 1 = 0. 0 > visited[1][0](-1). Push (1,0, k=0, steps=1). visited[1][0] = 0.
 * 2. Pop (0,1, k=0, steps=1).
 *    - Go right to (0,2). It's a 1. next_k = 0 - 1 = -1. Ignore (out of budget).
 * 3. Pop (1,0, k=0, steps=1).
 *    - Go down to (2,0). It's a 1. next_k = 0 - 1 = -1. Ignore.
 * Queue empty. Target not reached. Return -1.
 * 
 * 
 * 5. PITFALLS & PATTERN RECOGNITION
 * ------------------------------------------------
 * - Pitfall 1: Using `boolean[][] visited`. It wrongly assumes all visits to a cell are functionally equal.
 * - Pitfall 2: Using a `Set<String>` for state like `visited.add(r + "," + c + "," + k)`. String concatenation 
 *   in a BFS loop creates immense garbage collection overhead and easily causes Time Limit Exceeded (TLE).
 * - Pitfall 3: Not using `next_k > visited[r][c]`. If you use `next_k >= visited[r][c]`, you invite infinite 
 *   loops back and forth between empty cells with the same `k`.
 * 
 * Pattern Recognition: "Shortest path + constrained resource (budget, stamina, wall-breaks)" 
 * -> State-Space BFS.
 * - Similar Problems: "Cheapest Flights Within K Stops" (State = Node + Stops), "Path With Maximum Gold".
 *
 *
 * 6. FOLLOW-UPS
 * ------------------------------------------------
 * F1: What if the grid is massive (1000x1000), but obstacles are rare?
 *  -> Standard BFS explores radically in a circle. We should use A* Search (PriorityQueue). Our heuristic 
 *     would be the Manhattan distance to the target. This directs the search straight toward the end.
 * 
 * F2: What if breaking an obstacle isn't a hard limit 'k', but costs a specific amount of time/money?
 *  -> The graph becomes weighted. We drop the Queue and use Dijkstra's Algorithm (Min-Heap), where the state 
 *     is `(row, col)` and the weight is the total cost spent so far.
 * 
 * F3: How do we return the actual shortest path taken, not just the number of steps?
 *  -> We modify the queue payload to store a reference to its parent state. `class State { int r, c, k; State parent; }`.
 *     When we reach the end, we traverse `parent` references backward to (0,0) to reconstruct the path.
 */

import java.util.LinkedList;
import java.util.Queue;
import java.util.Arrays;

public class ShortestPathObstacles {

    // Using a record to keep the Queue payload clean and readable
    record State(int row, int col, int k, int steps) {}

    /**
     * OPTIMAL APPROACH: State-Space BFS with 2D Visited Array & Early Exit
     * Time Complexity: O(m * n * k)
     * Space Complexity: O(m * n)
     */
    public int shortestPath(int[][] grid, int k) {
        int m = grid.length;
        int n = grid[0].length;
        
        // Edge Case: 1x1 grid. We are already at the target.
        if (m == 1 && n == 1) {
            return 0;
        }
        
        // Optimization: Manhattan Distance Early Exit
        // The absolute shortest path on an empty grid takes (m - 1) + (n - 1) = m + n - 2 steps.
        // If our budget 'k' is greater than or equal to this distance, we can just walk blindly 
        // in an L-shape or stair-step directly to the target, smashing every obstacle in our way.
        if (k >= m + n - 2) {
            return m + n - 2;
        }
        
        // visited[r][c] stores the MAXIMUM 'k' (budget) remaining when we arrived at cell (r, c).
        // Initialized to -1 because a valid 'k' can be 0.
        int[][] visited = new int[m][n];
        for (int[] row : visited) {
            Arrays.fill(row, -1);
        }
        
        Queue<State> queue = new LinkedList<>();
        queue.offer(new State(0, 0, k, 0));
        visited[0][0] = k;
        
        // Standard 4-directional offsets: Up, Down, Left, Right
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        while (!queue.isEmpty()) {
            State curr = queue.poll();
            
            // Explore all 4 neighbors
            for (int[] dir : directions) {
                int nextR = curr.row() + dir[0];
                int nextC = curr.col() + dir[1];
                
                // Bounds check
                if (nextR >= 0 && nextR < m && nextC >= 0 && nextC < n) {
                    
                    // Deduct 1 from budget if the next cell is an obstacle (1), else deduct 0
                    int nextK = curr.k() - grid[nextR][nextC];
                    
                    // Are we at the target? 
                    // (Checked here before adding to queue saves one full queue pop cycle)
                    if (nextR == m - 1 && nextC == n - 1 && nextK >= 0) {
                        return curr.steps() + 1;
                    }
                    
                    // Pruning Condition:
                    // 1. nextK >= 0 -> We haven't overspent our budget.
                    // 2. nextK > visited[nextR][nextC] -> This is the magic check. We only process this 
                    //    path if it brings a STRICTLY BETTER budget to this cell than any previous path.
                    if (nextK >= 0 && nextK > visited[nextR][nextC]) {
                        visited[nextR][nextC] = nextK; // Record our new best budget for this cell
                        queue.offer(new State(nextR, nextC, nextK, curr.steps() + 1));
                    }
                }
            }
        }
        
        // Queue exhausted, never reached bottom-right
        return -1;
    }

    // ============================================================================
    // MAIN METHOD - TEST CASES
    // ============================================================================
    public static void main(String[] args) {
        ShortestPathObstacles solver = new ShortestPathObstacles();

        System.out.println("--- Testing Shortest Path with Obstacles Elimination ---");

        // Test Case 1: Standard routing around and through walls
        // Path can blast through one wall to save steps.
        int[][] grid1 = {
            {0, 1, 0, 0},
            {0, 1, 0, 1},
            {0, 0, 0, 0}
        };
        int k1 = 1;
        System.out.println("Test Case 1 (Expected 5): " + solver.shortestPath(grid1, k1));

        // Test Case 2: Insufficient budget
        // Requires breaking 2 walls, but k=1. Unreachable.
        int[][] grid2 = {
            {0, 1, 1},
            {1, 1, 1},
            {1, 1, 0}
        };
        int k2 = 1;
        System.out.println("Test Case 2 (Expected -1): " + solver.shortestPath(grid2, k2));

        // Test Case 3: Early Exit Manhattan logic
        // Heavily walled, but we have massive budget k=10.
        int[][] grid3 = {
            {0, 1, 1},
            {1, 1, 1},
            {1, 1, 0}
        };
        int k3 = 10; 
        System.out.println("Test Case 3 (Expected 4): " + solver.shortestPath(grid3, k3));
        
        // Test Case 4: Re-visiting cell with better 'k' requirement
        // (Diagrammed in the study notes)
        int[][] grid4 = {
            {0, 1, 0, 0, 0},
            {0, 1, 0, 1, 0},
            {0, 0, 0, 1, 0}
        };
        int k4 = 1;
        // Optimal is actually walking around the first wall, saving k=1 to blast the second wall.
        // If we blasted the first wall, we'd fail at the second.
        System.out.println("Test Case 4 (Expected 6): " + solver.shortestPath(grid4, k4));

        // Test Case 5: 1x1 Edge Case
        int[][] grid5 = {{0}};
        int k5 = 1;
        System.out.println("Test Case 5 (Expected 0): " + solver.shortestPath(grid5, k5));
    }
}

import java.util.*;

/**
 * Shortest Path in a Grid with Obstacle Elimination
 *
 * Problem:
 * --------
 * We are given a grid where:
 *
 *     0 = empty cell
 *     1 = obstacle
 *
 * We start at:
 *
 *     (0, 0)
 *
 * and need to reach:
 *
 *     (rows - 1, cols - 1)
 *
 * We can move up, down, left, or right.
 *
 * We are allowed to eliminate at most `k` obstacles.
 *
 * Return the minimum number of steps required to reach the destination.
 *
 * If the destination cannot be reached, return -1.
 *
 *
 * ---------------------------------------------------------------
 * Key Observation
 * ---------------------------------------------------------------
 *
 * A normal BFS on `(row, col)` is NOT enough.
 *
 * Why?
 *
 * Consider reaching the same cell:
 *
 *     (2, 3)
 *
 * in two different ways:
 *
 *     Path A -> reached (2, 3) with 0 eliminations remaining
 *
 *     Path B -> reached (2, 3) with 2 eliminations remaining
 *
 * These are NOT equivalent states.
 *
 * From Path B, we can still eliminate obstacles.
 *
 * From Path A, we cannot.
 *
 *
 * Therefore, our BFS state must contain:
 *
 *     (row, col, remainingEliminations)
 *
 *
 * So instead of thinking:
 *
 *     visited[row][col]
 *
 * we need:
 *
 *     visited[row][col][remainingEliminations]
 *
 *
 * ---------------------------------------------------------------
 * Why BFS?
 * ---------------------------------------------------------------
 *
 * Every move has exactly the same cost:
 *
 *     1 step
 *
 * BFS explores the grid level by level:
 *
 *     Level 0 -> starting position
 *     Level 1 -> positions reachable in 1 step
 *     Level 2 -> positions reachable in 2 steps
 *     ...
 *
 * Therefore, the first time we reach the destination,
 * we have found the shortest possible path.
 *
 *
 * ---------------------------------------------------------------
 * State Representation
 * ---------------------------------------------------------------
 *
 * State:
 *
 *     (row, col, remainingEliminations)
 *
 * Example:
 *
 *     (2, 3, 1)
 *
 * means:
 *
 *     "We are at row 2, column 3 and can still eliminate
 *      1 obstacle."
 *
 *
 * ---------------------------------------------------------------
 * Time Complexity
 * ---------------------------------------------------------------
 *
 * Number of possible states:
 *
 *     rows * cols * (k + 1)
 *
 * Each state has at most 4 neighbors.
 *
 * Therefore:
 *
 *     Time:  O(rows * cols * k)
 *
 *     Space: O(rows * cols * k)
 *
 * ---------------------------------------------------------------
 */
public class Solution {

    /**
     * Represents one BFS state.
     *
     * A state is more than just a grid position.
     *
     * We also need to know how many obstacle eliminations
     * are still available.
     */
    record State(
            int row,
            int col,
            int remainingEliminations
    ) {}

    /**
     * Four possible movement directions:
     *
     *     up
     *     down
     *     right
     *     left
     */
    private static final int[][] DIRECTIONS = {
            {-1, 0}, // up
            {1, 0},  // down
            {0, 1},  // right
            {0, -1}  // left
    };

    public int shortestPath(int[][] grid, int k) {

        int rows = grid.length;
        int columns = grid[0].length;


        /*
         * ============================================================
         * STEP 1: HANDLE THE START == DESTINATION CASE
         * ============================================================
         *
         * If the grid contains only one cell:
         *
         *     [0]
         *
         * we are already at the destination.
         *
         * Therefore, the number of steps is 0.
         */
        if (rows == 1 && columns == 1) {
            return 0;
        }


        /*
         * ============================================================
         * STEP 2: CREATE THE VISITED ARRAY
         * ============================================================
         *
         * This is a 3-dimensional array:
         *
         *     visited[row][column][remainingEliminations]
         *
         *
         * Why 3 dimensions?
         *
         * Because the following two states are different:
         *
         *     (2, 3, 0)
         *     (2, 3, 2)
         *
         * Both are at the same cell, but the second state has
         * more resources available.
         *
         *
         * Example:
         *
         *     visited[2][3][2] = true
         *
         * means:
         *
         *     "We have already reached (2,3) with 2 eliminations
         *      remaining."
         *
         *
         * We use `boolean` because we only need to know whether
         * a particular state has already been explored.
         */
        boolean[][][] visited =
                new boolean[rows][columns][k + 1];


        /*
         * ============================================================
         * STEP 3: CREATE THE BFS QUEUE
         * ============================================================
         *
         * BFS uses a FIFO queue:
         *
         *     First In -> First Out
         *
         * Each element in the queue represents:
         *
         *     (row, column, remaining eliminations)
         */
        Queue<State> queue = new ArrayDeque<>();


        /*
         * ============================================================
         * STEP 4: ADD THE STARTING STATE
         * ============================================================
         *
         * We start at:
         *
         *     (0, 0)
         *
         * and initially have all `k` eliminations available.
         *
         * So the starting state is:
         *
         *     (0, 0, k)
         */
        queue.offer(new State(0, 0, k));

        /*
         * Mark the starting state as visited.
         *
         * This means:
         *
         *     "We have already reached (0,0) with k eliminations
         *      remaining."
         */
        visited[0][0][k] = true;


        /*
         * `steps` represents the BFS level.
         *
         * Initially:
         *
         *     steps = 0
         *
         * because we have not moved anywhere yet.
         */
        int steps = 0;


        /*
         * ============================================================
         * STEP 5: START BFS
         * ============================================================
         */
        while (!queue.isEmpty()) {

            /*
             * ========================================================
             * PROCESS ONE BFS LEVEL
             * ========================================================
             *
             * Every state currently in the queue represents a
             * position reachable in exactly `steps` moves.
             *
             * We capture the current queue size so that we process
             * exactly one BFS level at a time.
             */
            int statesInCurrentLevel = queue.size();


            /*
             * Process every state at this distance.
             */
            for (int i = 0; i < statesInCurrentLevel; i++) {

                State currentState = queue.poll();

                int currentRow = currentState.row();
                int currentColumn = currentState.col();
                int remainingEliminations =
                        currentState.remainingEliminations();


                /*
                 * ====================================================
                 * STEP 6: CHECK DESTINATION
                 * ====================================================
                 *
                 * Because BFS processes states level by level,
                 * the first time we reach the destination,
                 * `steps` is guaranteed to be the shortest distance.
                 */
                if (currentRow == rows - 1 &&
                        currentColumn == columns - 1) {

                    return steps;
                }


                /*
                 * ====================================================
                 * STEP 7: EXPLORE FOUR DIRECTIONS
                 * ====================================================
                 *
                 * From the current cell, try:
                 *
                 *     up
                 *     down
                 *     left
                 *     right
                 */
                for (int[] direction : DIRECTIONS) {

                    int nextRow =
                            currentRow + direction[0];

                    int nextColumn =
                            currentColumn + direction[1];


                    /*
                     * =================================================
                     * STEP 8: CHECK BOUNDS
                     * =================================================
                     *
                     * Ignore positions outside the grid.
                     *
                     * Valid row:
                     *
                     *     0 <= row < rows
                     *
                     * Valid column:
                     *
                     *     0 <= column < columns
                     */
                    if (nextRow < 0 ||
                            nextColumn < 0 ||
                            nextRow >= rows ||
                            nextColumn >= columns) {

                        continue;
                    }


                    /*
                     * =================================================
                     * STEP 9: CALCULATE REMAINING ELIMINATIONS
                     * =================================================
                     *
                     * The value of the next cell is either:
                     *
                     *     0 -> empty cell
                     *     1 -> obstacle
                     *
                     *
                     * If it is an empty cell:
                     *
                     *     newRemaining = remaining - 0
                     *                    = remaining
                     *
                     *
                     * If it is an obstacle:
                     *
                     *     newRemaining = remaining - 1
                     *
                     *
                     * So we can calculate both cases with:
                     *
                     *     remaining - grid[nextRow][nextColumn]
                     */
                    int newRemainingEliminations =
                            remainingEliminations
                                    - grid[nextRow][nextColumn];


                    /*
                     * If the value becomes negative, we don't have
                     * enough eliminations to enter this cell.
                     *
                     * Example:
                     *
                     *     remaining = 0
                     *     next cell = obstacle (1)
                     *
                     *     newRemaining = 0 - 1 = -1
                     *
                     * Therefore, this move is invalid.
                     */
                    if (newRemainingEliminations < 0) {
                        continue;
                    }


                    /*
                     * =================================================
                     * STEP 10: CHECK WHETHER THIS STATE WAS VISITED
                     * =================================================
                     *
                     * IMPORTANT:
                     *
                     * We do NOT check only:
                     *
                     *     visited[nextRow][nextColumn]
                     *
                     *
                     * We check:
                     *
                     *     visited[nextRow]
                     *             [nextColumn]
                     *             [newRemainingEliminations]
                     *
                     *
                     * Because reaching the same cell with different
                     * numbers of remaining eliminations represents
                     * different states.
                     *
                     * Example:
                     *
                     *     (2,3,0)
                     *     (2,3,2)
                     *
                     * These must be treated separately.
                     */
                    if (visited[nextRow]
                            [nextColumn]
                            [newRemainingEliminations]) {

                        continue;
                    }


                    /*
                     * =================================================
                     * STEP 11: MARK STATE AS VISITED
                     * =================================================
                     *
                     * Mark it BEFORE adding it to the queue.
                     *
                     * This prevents the same state from being added
                     * multiple times by different paths.
                     */
                    visited[nextRow]
                            [nextColumn]
                            [newRemainingEliminations] = true;


                    /*
                     * Add the newly discovered state to BFS.
                     *
                     * The next state contains:
                     *
                     *     new row
                     *     new column
                     *     updated remaining eliminations
                     */
                    queue.offer(
                            new State(
                                    nextRow,
                                    nextColumn,
                                    newRemainingEliminations
                            )
                    );
                }
            }


            /*
             * ========================================================
             * STEP 12: MOVE TO THE NEXT BFS LEVEL
             * ========================================================
             *
             * We have finished processing every state that was
             * reachable in `steps` moves.
             *
             * Everything we just added to the queue requires
             * exactly one additional move.
             *
             * Therefore:
             *
             *     steps++
             */
            steps++;
        }


        /*
         * ============================================================
         * STEP 13: DESTINATION WAS NEVER REACHED
         * ============================================================
         *
         * If the queue becomes empty, there are no more states
         * to explore.
         *
         * Therefore, there is no valid path from start to destination
         * using at most `k` obstacle eliminations.
         */
        return -1;
    }
}

import java.util.*;

/**
 * Shortest Path in a Grid with Obstacle Elimination
 * --------------------------------------------------
 *
 * A* Search Solution
 *
 * Problem:
 * --------
 * We have a grid where:
 *
 *     0 = empty cell
 *     1 = obstacle
 *
 * We start at (0, 0) and want to reach:
 *
 *     (rows - 1, columns - 1)
 *
 * We can move:
 *
 *     up
 *     down
 *     left
 *     right
 *
 * We can eliminate at most `k` obstacles.
 *
 * Return the minimum number of steps required.
 *
 * If the destination cannot be reached, return -1.
 *
 *
 * ================================================================
 * 1. WHY NOT JUST USE visited[row][col]?
 * ================================================================
 *
 * Because the remaining number of obstacle eliminations matters.
 *
 * Consider:
 *
 *     State A = (2, 3, 0)
 *     State B = (2, 3, 2)
 *
 * Both states are at the same cell.
 *
 * But they are NOT equivalent.
 *
 * State A:
 *     No obstacle eliminations left.
 *
 * State B:
 *     Two obstacle eliminations still available.
 *
 * Therefore our state is:
 *
 *     (row, column, remainingEliminations)
 *
 * and visited must be:
 *
 *     visited[row][column][remainingEliminations]
 *
 *
 * ================================================================
 * 2. BFS VS A*
 * ================================================================
 *
 * BFS explores states according to:
 *
 *     "How far have I travelled?"
 *
 * So BFS effectively prioritizes:
 *
 *     smaller g(n)
 *
 * where:
 *
 *     g(n) = distance from start
 *
 *
 * A* uses:
 *
 *     f(n) = g(n) + h(n)
 *
 * where:
 *
 *     g(n) = cost from start to current state
 *
 *     h(n) = estimated cost from current state to destination
 *
 *     f(n) = estimated total cost of the complete path
 *
 *
 * A* therefore tries to answer:
 *
 *     "Which state looks most promising if I consider
 *      both the distance I've already travelled and the
 *      estimated distance remaining?"
 *
 *
 * ================================================================
 * 3. OUR HEURISTIC
 * ================================================================
 *
 * For a grid where we can move only up/down/left/right,
 * the Manhattan distance is:
 *
 *     |currentRow - targetRow|
 *     +
 *     |currentColumn - targetColumn|
 *
 * Example:
 *
 *     Current = (2, 3)
 *     Target  = (5, 7)
 *
 * Manhattan distance:
 *
 *     |2 - 5| + |3 - 7|
 *     = 3 + 4
 *     = 7
 *
 *
 * Why is this a useful heuristic?
 *
 * Because even in the BEST possible case, we need at least
 * 7 moves to reach the target.
 *
 * We cannot reach the target in fewer than 7 moves because
 * every move changes either the row or column by exactly 1.
 *
 *
 * Obstacles can only make the actual path longer.
 *
 * Therefore:
 *
 *     Manhattan distance <= actual remaining distance
 *
 * This means the heuristic is ADMISSIBLE.
 *
 *
 * ================================================================
 * 4. IMPORTANT: OBSTACLE ELIMINATION DOES NOT CHANGE THE HEURISTIC
 * ================================================================
 *
 * We might think:
 *
 *     "Should h(n) also consider how many obstacles remain?"
 *
 * For this simple A* implementation, no.
 *
 * Manhattan distance is still a valid LOWER BOUND.
 *
 * It ignores obstacles completely.
 *
 * Therefore it may underestimate the actual cost, but it will
 * never overestimate it.
 *
 * Example:
 *
 *     Manhattan distance = 5
 *
 * Actual shortest path might be:
 *
 *     5 moves
 *
 * or:
 *
 *     8 moves because obstacles force a detour
 *
 * But it can never be:
 *
 *     4 moves
 *
 * So Manhattan distance remains admissible.
 *
 *
 * ================================================================
 * 5. WHY A* CAN RETURN WHEN IT POPS THE DESTINATION
 * ================================================================
 *
 * The priority queue is ordered by:
 *
 *     f(n) = g(n) + h(n)
 *
 * When we remove the destination from the priority queue,
 * its heuristic is:
 *
 *     h(destination) = 0
 *
 * Therefore:
 *
 *     f(destination) = g(destination)
 *
 * With an admissible heuristic, the first destination state
 * removed from the priority queue has the optimal shortest-path
 * cost.
 *
 *
 * ================================================================
 * Complexity
 * ================================================================
 *
 * Number of possible states:
 *
 *     rows * columns * (k + 1)
 *
 * Every state has at most 4 transitions.
 *
 * Therefore the number of states is:
 *
 *     O(rows * columns * k)
 *
 * Each state may be inserted into the priority queue.
 *
 * Priority queue operations cost approximately:
 *
 *     O(log(rows * columns * k))
 *
 * So a practical bound is:
 *
 *     Time:
 *     O(rows * columns * k * log(rows * columns * k))
 *
 *     Space:
 *     O(rows * columns * k)
 *
 *
 * Note:
 * A* does NOT have a universally better worst-case complexity
 * than BFS for this problem. Its advantage is that a good heuristic
 * can make it explore fewer states in practice.
 */
public class Solution {

    /**
     * Represents one state in the A* search.
     *
     * row:
     *     Current row.
     *
     * column:
     *     Current column.
     *
     * remainingEliminations:
     *     Number of obstacles we can still eliminate.
     *
     * stepsFromStart:
     *     Actual number of moves made from the starting cell.
     *
     * estimatedTotalCost:
     *     f(n) = g(n) + h(n)
     *
     *     where:
     *
     *         g(n) = stepsFromStart
     *         h(n) = Manhattan distance to target
     */
    record State(
            int row,
            int column,
            int remainingEliminations,
            int stepsFromStart,
            int estimatedTotalCost
    ) {}

    /**
     * Four possible movement directions:
     *
     *     up
     *     down
     *     right
     *     left
     */
    private static final int[][] DIRECTIONS = {
            {-1, 0}, // up
            {1, 0},  // down
            {0, 1},  // right
            {0, -1}  // left
    };

    public int shortestPath(int[][] grid, int k) {

        int rows = grid.length;
        int columns = grid[0].length;

        int targetRow = rows - 1;
        int targetColumn = columns - 1;


        /*
         * ============================================================
         * STEP 1: START == DESTINATION
         * ============================================================
         *
         * If the grid has only one cell:
         *
         *     [0]
         *
         * we are already at the destination.
         *
         * Therefore:
         *
         *     answer = 0
         */
        if (rows == 1 && columns == 1) {
            return 0;
        }


        /*
         * ============================================================
         * STEP 2: TRACK THE BEST DISTANCE FOR EACH STATE
         * ============================================================
         *
         * We need to distinguish between:
         *
         *     (row, column, remainingEliminations)
         *
         * states.
         *
         * Therefore:
         *
         *     bestSteps[row][column][remainingEliminations]
         *
         * represents the minimum number of steps with which we
         * have reached this exact state.
         *
         *
         * Why not simply use boolean visited[][][]?
         *
         * We could.
         *
         * But A* naturally works with the idea:
         *
         *     "Have we already reached this state with a
         *      cheaper g(n)?"
         *
         * So storing the best distance makes the logic explicit.
         */
        int[][][] bestSteps =
                new int[rows][columns][k + 1];

        /*
         * Initialize all distances to Integer.MAX_VALUE.
         *
         * MAX_VALUE means:
         *
         *     "We haven't reached this state yet."
         */
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                Arrays.fill(
                        bestSteps[row][column],
                        Integer.MAX_VALUE
                );
            }
        }


        /*
         * ============================================================
         * STEP 3: CREATE THE A* PRIORITY QUEUE
         * ============================================================
         *
         * A* prioritizes:
         *
         *     f(n) = g(n) + h(n)
         *
         * Therefore the state with the LOWEST estimated total cost
         * should be processed first.
         *
         * This means we use a MIN-HEAP.
         */
        PriorityQueue<State> priorityQueue =
                new PriorityQueue<>(
                        Comparator.comparingInt(
                                State::estimatedTotalCost
                        )
                );


        /*
         * ============================================================
         * STEP 4: INITIALIZE THE START STATE
         * ============================================================
         *
         * At the starting position:
         *
         *     row = 0
         *     column = 0
         *     remaining eliminations = k
         *
         * We have made:
         *
         *     g(n) = 0
         *
         * steps.
         *
         *
         * Calculate:
         *
         *     h(n) = Manhattan distance from start to target.
         */
        int startHeuristic =
                manhattanDistance(
                        0,
                        0,
                        targetRow,
                        targetColumn
                );

        /*
         * Therefore:
         *
         *     f(n) = g(n) + h(n)
         *
         *           = 0 + startHeuristic
         */
        int startEstimatedTotalCost =
                startHeuristic;


        /*
         * The starting state requires zero steps.
         */
        bestSteps[0][0][k] = 0;


        /*
         * Add the starting state to A*.
         */
        priorityQueue.offer(
                new State(
                        0,
                        0,
                        k,
                        0,
                        startEstimatedTotalCost
                )
        );


        /*
         * ============================================================
         * STEP 5: A* SEARCH
         * ============================================================
         */
        while (!priorityQueue.isEmpty()) {

            /*
             * Remove the state with the LOWEST:
             *
             *     f(n) = g(n) + h(n)
             *
             * from the priority queue.
             */
            State currentState = priorityQueue.poll();

            int currentRow = currentState.row();
            int currentColumn = currentState.column();

            int remainingEliminations =
                    currentState.remainingEliminations();

            int currentSteps =
                    currentState.stepsFromStart();


            /*
             * ========================================================
             * STEP 6: IGNORE OUTDATED STATES
             * ========================================================
             *
             * The same state can potentially be inserted multiple
             * times.
             *
             * Example:
             *
             *     (2, 3, 1)
             *
             * might first be reached in:
             *
             *     10 steps
             *
             * and later in:
             *
             *     8 steps
             *
             * We only care about the better value:
             *
             *     8
             *
             * Therefore, if the state currently being processed
             * is worse than our recorded best distance, ignore it.
             */
            if (currentSteps >
                    bestSteps
                            [currentRow]
                            [currentColumn]
                            [remainingEliminations]) {

                continue;
            }


            /*
             * ========================================================
             * STEP 7: CHECK DESTINATION
             * ========================================================
             *
             * This is different from BFS.
             *
             * A* doesn't process states simply in increasing number
             * of steps.
             *
             * It processes them according to:
             *
             *     f(n) = g(n) + h(n)
             *
             *
             * When we reach the destination:
             *
             *     h(destination) = 0
             *
             * Therefore:
             *
             *     f(destination) = g(destination)
             *
             *
             * Since our Manhattan heuristic is admissible, the first
             * destination state removed from the priority queue has
             * the shortest possible path.
             */
            if (currentRow == targetRow &&
                    currentColumn == targetColumn) {

                return currentSteps;
            }


            /*
             * ========================================================
             * STEP 8: EXPLORE NEIGHBORS
             * ========================================================
             */
            for (int[] direction : DIRECTIONS) {

                int nextRow =
                        currentRow + direction[0];

                int nextColumn =
                        currentColumn + direction[1];


                /*
                 * ----------------------------------------------------
                 * Check whether the next position is inside the grid.
                 * ----------------------------------------------------
                 */
                if (nextRow < 0 ||
                        nextColumn < 0 ||
                        nextRow >= rows ||
                        nextColumn >= columns) {

                    continue;
                }


                /*
                 * ====================================================
                 * STEP 9: CALCULATE REMAINING ELIMINATIONS
                 * ====================================================
                 *
                 * grid[nextRow][nextColumn] is:
                 *
                 *     0 -> empty cell
                 *     1 -> obstacle
                 *
                 *
                 * Therefore:
                 *
                 *     newRemaining =
                 *         remaining - grid[nextRow][nextColumn]
                 *
                 *
                 * Example 1:
                 *
                 *     remaining = 2
                 *     next cell = 0
                 *
                 *     newRemaining = 2 - 0 = 2
                 *
                 *
                 * Example 2:
                 *
                 *     remaining = 2
                 *     next cell = 1
                 *
                 *     newRemaining = 2 - 1 = 1
                 */
                int newRemainingEliminations =
                        remainingEliminations
                                - grid[nextRow][nextColumn];


                /*
                 * If this becomes negative, we don't have enough
                 * eliminations to enter the obstacle.
                 */
                if (newRemainingEliminations < 0) {
                    continue;
                }


                /*
                 * ====================================================
                 * STEP 10: CALCULATE g(n)
                 * ====================================================
                 *
                 * Moving from the current cell to the next cell
                 * costs exactly one step.
                 *
                 * Therefore:
                 *
                 *     newG = currentG + 1
                 */
                int newStepsFromStart =
                        currentSteps + 1;


                /*
                 * ====================================================
                 * STEP 11: CHECK WHETHER THIS STATE IS BETTER
                 * ====================================================
                 *
                 * Our state is:
                 *
                 *     (nextRow,
                 *      nextColumn,
                 *      newRemainingEliminations)
                 *
                 *
                 * We only care about this state if we have found
                 * a shorter way to reach it.
                 */
                if (newStepsFromStart >=
                        bestSteps
                                [nextRow]
                                [nextColumn]
                                [newRemainingEliminations]) {

                    continue;
                }


                /*
                 * We found a better way to reach this exact state.
                 */
                bestSteps
                        [nextRow]
                        [nextColumn]
                        [newRemainingEliminations]
                        = newStepsFromStart;


                /*
                 * ====================================================
                 * STEP 12: CALCULATE h(n)
                 * ====================================================
                 *
                 * Estimate the remaining distance using Manhattan
                 * distance.
                 *
                 *     h(n) =
                 *
                 *     |row - targetRow|
                 *     +
                 *     |column - targetColumn|
                 */
                int heuristicCost =
                        manhattanDistance(
                                nextRow,
                                nextColumn,
                                targetRow,
                                targetColumn
                        );


                /*
                 * ====================================================
                 * STEP 13: CALCULATE f(n)
                 * ====================================================
                 *
                 * A* uses:
                 *
                 *     f(n) = g(n) + h(n)
                 *
                 *
                 * g(n):
                 *     Actual cost from start.
                 *
                 * h(n):
                 *     Estimated cost to destination.
                 *
                 * f(n):
                 *     Estimated total cost of a path through
                 *     this state.
                 */
                int estimatedTotalCost =
                        newStepsFromStart + heuristicCost;


                /*
                 * Add the improved state to the priority queue.
                 *
                 * A* will decide when to process it based on
                 * its estimated total cost.
                 */
                priorityQueue.offer(
                        new State(
                                nextRow,
                                nextColumn,
                                newRemainingEliminations,
                                newStepsFromStart,
                                estimatedTotalCost
                        )
                );
            }
        }


        /*
         * ============================================================
         * STEP 14: NO VALID PATH
         * ============================================================
         *
         * If the priority queue becomes empty, every reachable
         * state has been explored and the destination was never
         * reached.
         */
        return -1;
    }


    /**
     * Calculates Manhattan distance between two cells.
     *
     * Manhattan distance is appropriate because movement is limited
     * to the four cardinal directions:
     *
     *     up
     *     down
     *     left
     *     right
     *
     * Formula:
     *
     *     |row1 - row2| + |column1 - column2|
     *
     * Example:
     *
     *     current = (2, 3)
     *     target  = (5, 7)
     *
     *     distance =
     *
     *         |2 - 5| + |3 - 7|
     *         = 3 + 4
     *         = 7
     */
    private int manhattanDistance(
            int row1,
            int column1,
            int row2,
            int column2) {

        return Math.abs(row1 - row2)
                + Math.abs(column1 - column2);
    }
}
