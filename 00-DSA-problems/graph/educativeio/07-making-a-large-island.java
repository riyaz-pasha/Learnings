import java.util.*;

/*
 * ==========================================================================================
 *                                   MAKING A LARGE ISLAND
 * ==========================================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ------------------------------------------------------------------------------------------
 * Restatement:
 * You have an N x N grid of water (0s) and land (1s). You are allowed a "magic spell" 
 * to turn exactly ONE water cell (0) into land (1). 
 * What is the largest island (connected group of 1s) you can create after doing this?
 * 
 * Idea, Intuition, and Key Observations:
 * - If we try the Brute Force way: change a 0 to 1, find the max island area (using DFS/BFS),
 *   change it back, and repeat for every 0. That's extremely slow.
 * - Key Observation: An island's area doesn't change until we connect it. If we pre-calculate 
 *   the area of all existing islands and give each island a "Name" or "ID", we can save time.
 * - New Approach (Two-Pass): 
 *   1. Paint the Islands: Walk through the grid. Every time we see an island, give it a 
 *      unique ID (like 2, 3, 4...) and find its total area. Save this in a Map (ID -> Area).
 *   2. Evaluate every 0: Look at the 4 neighbors of the 0. Note their unique Island IDs. 
 *      The new island size if we flip this 0 would be: 
 *      1 (the flipped 0 itself) + Sum of the areas of those unique neighboring islands.
 *
 * How to identify the relevant algorithm/data structure:
 * - Grid grouping/connectivity -> Graph Traversal (DFS/BFS).
 * - Looking up pre-computed values efficiently -> HashMap.
 * - Avoiding counting the same neighboring island twice -> HashSet.
 *
 *
 * 2. INTERVIEW CLARIFICATION
 * ------------------------------------------------------------------------------------------
 * Ask these questions before coding:
 * Q1: "Are we allowed to modify the input grid to store the Island IDs?"
 *     Why: Reusing the grid saves O(N^2) space. (We will assume Yes. IDs will start at 2).
 * Q2: "What if the grid has no 0s (it's entirely land)?"
 *     Why: We can't flip a 0. The answer is just the area of the whole grid (N * N).
 * Q3: "What if the grid has no 1s (it's entirely water)?"
 *     Why: We flip one 0 to 1, so the max island size is exactly 1.
 * Q4: "Do diagonal connections count?"
 *     Why: Confirms we only check 4-directional neighbors (up, down, left, right).
 * Q5: "How large can N get?"
 *     Why: N=500 means N^2 = 250,000. An O(N^4) brute force will Time Out. A deep recursion 
 *          could cause StackOverflow, so BFS or careful DFS is required.
 *
 *
 * 3. EXAMPLES & VISUALS
 * ------------------------------------------------------------------------------------------
 * Example Grid:
 * 1 0 1
 * 0 0 0
 * 0 1 1
 *
 * Phase 1: Painting and Area Calculation
 * Grid becomes:
 * 2 0 3      (Island 2 has area 1)
 * 0 0 0      (Island 3 has area 1)
 * 0 4 4      (Island 4 has area 2)
 *
 * Map: {2=1, 3=1, 4=2}
 *
 * Phase 2: Flipping 0s
 * Let's test the 0 at center (1, 1).
 * Neighbors: Top=0, Bottom=4, Left=0, Right=0. Unique IDs = {4}. Area = 1 + Map.get(4) = 3.
 *
 * Let's test the 0 at top-middle (0, 1).
 * Neighbors: Left=2, Right=3. Unique IDs = {2, 3}. Area = 1 + Map.get(2) + Map.get(3) = 3.
 *
 * Let's test the 0 at right-middle (1, 2).
 * Neighbors: Top=3, Bottom=4. Unique IDs = {3, 4}. Area = 1 + Map.get(3) + Map.get(4) = 4! (MAX)
 *
 *
 * 4. SOLUTIONS
 * ------------------------------------------------------------------------------------------
 */

public class MakingALargeIsland {

    /*
     * APPROACH 1: BRUTE FORCE (Conceptual & Sub-optimal)
     * --------------------------------------------------------------------------------------
     * Idea: For every 0, temporarily flip it to 1, run a full DFS/BFS to find the largest 
     * island, then flip it back to 0. 
     * 
     * Time Complexity: O(N^4). For N=500, N^2 = 250,000. We could have 250,000 zeros. 
     * Each zero triggers a full grid search (250,000 steps). 250,000 * 250,000 = 62.5 Billion 
     * operations. This will unequivocally result in Time Limit Exceeded (TLE).
     * 
     * (Not implemented to save space, but you should mention why you skip it in an interview).
     */

    /*
     * APPROACH 2: OPTIMAL TWO-PASS (Paint Islands + Map) [RECOMMENDED]
     * --------------------------------------------------------------------------------------
     * Idea & Intuition: 
     * Find areas beforehand. Assign unique IDs. Use a HashSet to deduplicate neighbor IDs 
     * so we don't accidentally count the same island twice if a 0 touches it from two sides.
     * 
     * Time Complexity: O(N^2)
     * - Phase 1 visits each cell a constant number of times (O(N^2)).
     * - Phase 2 visits each 0 and checks exactly 4 neighbors (O(N^2)).
     * 
     * Space Complexity: O(N^2)
     * - Recursion stack in DFS can go up to N^2 deep if the whole grid is one snaking island.
     * - HashMap stores at most (N^2 / 2) entries (if grid is a checkerboard).
     */
    
    // Direction arrays for clean up/down/left/right traversal
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public int largestIsland(int[][] grid) {
        if (grid == null || grid.length == 0) return 0;

        int n = grid.length;
        // Map to store Island ID -> Area of that island
        Map<Integer, Integer> islandAreas = new HashMap<>();
        
        // Island IDs start from 2 (since 0 is water and 1 is unvisited land)
        int islandId = 2;
        int maxArea = 0;

        // ====================================================================
        // PHASE 1: Discover all islands, paint them with an ID, calculate area
        // ====================================================================
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                if (grid[r][c] == 1) {
                    // Start DFS to find the area and paint the island
                    int area = paintIslandDFS(grid, r, c, islandId);
                    islandAreas.put(islandId, area);
                    // Update global max in case the grid is entirely 1s (no 0s to flip)
                    maxArea = Math.max(maxArea, area);
                    islandId++;
                }
            }
        }

        // ====================================================================
        // PHASE 2: Evaluate every 0, calculate potential max area if flipped
        // ====================================================================
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                if (grid[r][c] == 0) {
                    // Find all unique adjacent island IDs
                    Set<Integer> neighborIds = new HashSet<>();
                    for (int[] dir : DIRECTIONS) {
                        int nr = r + dir[0];
                        int nc = c + dir[1];
                        
                        // Bounds check
                        if (nr >= 0 && nr < n && nc >= 0 && nc < n) {
                            int neighborId = grid[nr][nc];
                            if (neighborId > 1) { // It's part of a painted island
                                neighborIds.add(neighborId);
                            }
                        }
                    }

                    // Sum the area of all unique adjacent islands + 1 for the flipped zero
                    int currentPossibleArea = 1;
                    for (int id : neighborIds) {
                        currentPossibleArea += islandAreas.get(id);
                    }

                    maxArea = Math.max(maxArea, currentPossibleArea);
                }
            }
        }

        // If maxArea is 0, it means the grid was entirely 0s. 
        // Flipping one 0 makes the max area exactly 1.
        return maxArea == 0 ? 1 : maxArea;
    }

    /**
     * Helper method to explore an island, paint it with a specific ID, and return its area.
     */
    private int paintIslandDFS(int[][] grid, int r, int c, int islandId) {
        int n = grid.length;
        // Base cases: out of bounds or not unvisited land (1)
        if (r < 0 || r >= n || c < 0 || c >= n || grid[r][c] != 1) {
            return 0;
        }

        // Paint the current cell
        grid[r][c] = islandId;
        
        // Start area at 1 for current cell
        int area = 1;

        // Traverse all 4 directions recursively
        for (int[] dir : DIRECTIONS) {
            area += paintIslandDFS(grid, r + dir[0], c + dir[1], islandId);
        }

        return area;
    }


    /*
     * 6. EDGE CASES & MISTAKES
     * ------------------------------------------------------------------------------------------
     * Edge Cases Handled:
     * - Grid is all 1s: Phase 2 loop does nothing (no 0s found). The global `maxArea` variable 
     *   was updated in Phase 1 to N^2. Returns N^2.
     * - Grid is all 0s: Phase 1 does nothing. Phase 2 tries to flip 0s but finds no neighbors. 
     *   `maxArea` remains 1. (Or handled by `maxArea == 0 ? 1 : maxArea`).
     * - Matrix size 1x1: N=1, handled naturally.
     * 
     * Common Mistakes:
     * - NOT USING A HASHSET: If a 0 touches the SAME island from the top and the right, 
     *   an array or list will sum that island's area twice! A `HashSet` prevents this.
     * - Using ID 1: Grid cells are already 1. If you try to mark visited cells as 1, you 
     *   create an infinite loop. IDs must start at 2.
     * - Stack Overflow: N=500 can cause a recursion depth of 250,000. In strict enterprise 
     *   environments, the DFS function above should be converted to an Iterative BFS using 
     *   a Queue to protect the thread stack. (For LeetCode, DFS passes).
     *
     *
     * 7. INTERVIEW STRATEGY
     * ------------------------------------------------------------------------------------------
     * 1. Understand -> Explain the "Two-Pass" intuition clearly. "We don't want to recompute 
     *    island sizes. We should cache them."
     * 2. Clarify -> Ask if you can mutate the grid (to place IDs 2, 3, etc.).
     * 3. Observe -> Emphasize the trap: "A single 0 might touch the SAME island twice. I must 
     *    use a Set to deduplicate neighbor IDs."
     * 4. Code -> Write cleanly. Extract the DFS into a helper method. Use `DIRECTIONS` array 
     *    to avoid repeating code 4 times.
     * 5. Dry Run -> Use a small 2x2 grid to trace the mapping and the flipping.
     * 6. Complexity -> State O(N^2) time and O(N^2) space explicitly.
     *
     *
     * 8. SOLUTION COMPARISON
     * ------------------------------------------------------------------------------------------
     * Approach       | Time   | Space  | Trade-offs                     | Recommendation
     * ---------------|--------|--------|--------------------------------|-----------------
     * Brute Force    | O(N^4) | O(N^2) | Too slow, will TLE             | Do not code
     * Two-Pass DFS   | O(N^2) | O(N^2) | Elegant, clean, but stack risk | 🥇 Primary Choice
     * Two-Pass BFS   | O(N^2) | O(N^2) | Safe from StackOverflow        | Mention as an alt
     *
     *
     * 9. FOLLOW-UP QUESTIONS & ANSWERS
     * ------------------------------------------------------------------------------------------
     * F1: "What if you are not allowed to modify the input grid?"
     * A1: I would create an auxiliary 2D array `int[][] islandIds = new int[n][n];` and store 
     *     the IDs there. It takes an extra O(N^2) memory but leaves the original grid intact.
     *
     * F2: "What if N is extremely large (e.g., 100,000), but the grid is sparse (mostly 0s)?"
     * A2: A 2D array would exceed memory limits. We could represent the grid using a Sparse 
     *     Matrix (e.g., `Map<Point, Integer>`) or use Disjoint Sets (Union-Find) to group 
     *     land coordinates dynamically.
     * 
     * F3: "What if you are allowed to flip 'K' zeros instead of just 1?"
     * A3: This changes the problem dramatically into a Sliding Window or complex Graph problem 
     *     (like finding a path of length K connecting components). It is NP-hard or requires 
     *     BFS depending on the exact constraints.
     *
     *
     * 10. FINAL TAKEAWAYS
     * ------------------------------------------------------------------------------------------
     * - Graph Coloring / Labeling is a powerful technique. Changing state values (0/1 -> IDs) 
     *   allows O(1) lookups later.
     * - "What if I remove/add one node?" problems usually require precomputing the static 
     *   state first.
     * - Whenever you combine neighboring components, ALWAYS guard against duplicates (HashSet).
     */

    // Test Runner
    public static void main(String[] args) {
        MakingALargeIsland solution = new MakingALargeIsland();

        // Normal Case
        int[][] grid1 = {
            {1, 0, 1},
            {0, 0, 0},
            {0, 1, 1}
        };
        System.out.println("Test 1 Expected: 4 | Actual: " + solution.largestIsland(grid1));

        // Edge Case: All 1s
        int[][] grid2 = {
            {1, 1},
            {1, 1}
        };
        System.out.println("Test 2 Expected: 4 | Actual: " + solution.largestIsland(grid2));

        // Edge Case: All 0s
        int[][] grid3 = {
            {0, 0},
            {0, 0}
        };
        System.out.println("Test 3 Expected: 1 | Actual: " + solution.largestIsland(grid3));
        
        // Edge Case: Touching same island twice
        // 1 1
        // 1 0
        int[][] grid4 = {
            {1, 1},
            {1, 0}
        };
        System.out.println("Test 4 Expected: 4 | Actual: " + solution.largestIsland(grid4));
    }
}

import java.util.*;

/**
 * ============================================================================
 *                    LEETCODE 827 - MAKING A LARGE ISLAND
 * ============================================================================
 *
 * Problem:
 * You are given an n x n binary grid:
 *
 *     0 = water
 *     1 = land
 *
 * You may change AT MOST ONE 0 into 1.
 *
 * Return the largest possible island area after the change.
 *
 * An island is a group of 1s connected in 4 directions:
 *
 *                 UP
 *                  |
 *             LEFT--X--RIGHT
 *                  |
 *                DOWN
 *
 * Diagonal cells are NOT connected.
 *
 *
 * Example:
 *
 *     Input:
 *
 *         1 0
 *         0 1
 *
 *     If we flip the top-right 0:
 *
 *         1 1
 *         0 1
 *
 *     Largest island = 3
 *
 *
 * ============================================================================
 * BIG PICTURE
 * ============================================================================
 *
 * The key observation is:
 *
 *     "If I flip a water cell, which existing islands will become connected?"
 *
 * A zero cell has at most 4 neighbors.
 *
 * Example:
 *
 *         A A 0 B
 *         A 0 0 B
 *         0 0 B B
 *
 * Flipping the center 0 can connect:
 *
 *     - island A
 *     - island B
 *     - the flipped cell itself
 *
 * So:
 *
 *     newArea = 1 + area(island A) + area(island B) + ...
 *
 * BUT:
 *
 * The same island can touch the zero from multiple directions.
 *
 * Example:
 *
 *         A A
 *         A 0
 *
 * The zero has TWO neighbors belonging to island A.
 *
 * We must count island A only once.
 *
 * That is why every optimal solution uses:
 *
 *     Set<Integer> seen
 *
 * for the neighboring island IDs / DSU component roots.
 *
 *
 * ============================================================================
 * APPROACHES INCLUDED IN THIS FILE
 * ============================================================================
 *
 * 1. Brute Force
 *    - Flip every 0.
 *    - Recalculate the largest island from scratch.
 *    - Easy to understand, but expensive.
 *
 * 2. DFS + Island Labeling
 *    - Find every island once.
 *    - Give each island a unique ID: 2, 3, 4, ...
 *    - Store island area in a map.
 *    - For every 0, combine adjacent unique island IDs.
 *
 * 3. BFS + Island Labeling
 *    - Exactly the same high-level idea as DFS.
 *    - BFS avoids recursion-depth issues on very large grids.
 *
 * 4. DSU / Union-Find
 *    - Treat every land cell as a node.
 *    - Union adjacent land cells.
 *    - Track component sizes.
 *    - For every 0, combine the unique neighboring DSU components.
 *
 *
 * ============================================================================
 * WHICH APPROACH SHOULD I USE IN AN INTERVIEW?
 * ============================================================================
 *
 * Best progression:
 *
 *     Brute force
 *         |
 *         v
 *     Observe repeated work
 *         |
 *         v
 *     Precompute island areas
 *         |
 *         +----------------------+
 *         |                      |
 *         v                      v
 *       DFS                     BFS
 *     labeling                 labeling
 *
 *     DSU is an alternative advanced representation of connected components.
 *
 * For THIS problem:
 *
 *     DFS/BFS labeling is usually the cleanest solution.
 *
 * DSU is useful when:
 *
 *     - connectivity changes dynamically,
 *     - you have many union operations,
 *     - you want a reusable connected-component data structure.
 *
 *
 * ============================================================================
 * IMPORTANT JAVA NOTE
 * ============================================================================
 *
 * This file contains multiple solution classes.
 *
 * You can run the main() method to compare them.
 *
 * If your online judge expects:
 *
 *     class Solution
 *
 * simply copy the implementation you want into class Solution,
 * or rename one of the solution classes.
 *
 *
 * ============================================================================
 */

public class LargestIslandAllApproaches {

    // ========================================================================
    // Common directions
    // ========================================================================
    //
    //                 (-1,0)
    //                    ^
    //                    |
    //       (0,-1) <--- (r,c) ---> (0,+1)
    //                    |
    //                    v
    //                 (+1,0)
    //
    // ========================================================================

    private static final int[][] FOUR_DIRS = {
        {1, 0},
        {-1, 0},
        {0, 1},
        {0, -1}
    };

    // ========================================================================
    // Helper methods used by the demo / tracing code
    // ========================================================================

    private static void printGrid(int[][] grid) {
        for (int[] row : grid) {
            for (int value : row) {
                System.out.print(value + " ");
            }
            System.out.println();
        }
    }

    private static int[][] copyGrid(int[][] grid) {
        int[][] copy = new int[grid.length][];
        for (int i = 0; i < grid.length; i++) {
            copy[i] = grid[i].clone();
        }
        return copy;
    }

    private static String formatCell(int r, int c) {
        return "(" + r + "," + c + ")";
    }

    // ========================================================================
    // APPROACH 1: BRUTE FORCE
    // ========================================================================
    //
    // Idea:
    //
    // For every 0:
    //
    //     1. Temporarily change it to 1.
    //     2. Run DFS/BFS over the entire grid.
    //     3. Find the largest island.
    //     4. Restore the 0.
    //
    //
    // Why this is slow:
    //
    // If there are O(n^2) zeroes, and every flip requires scanning O(n^2)
    // cells, total work becomes O(n^4).
    //
    // This is useful as a learning baseline and as a correctness reference.
    //
    // ========================================================================

    static class SolutionBruteForce {

        public int largestIsland(int[][] grid) {
            int n = grid.length;
            int answer = 0;

            /*
             * Special case:
             *
             * If the grid has no zero, we cannot "improve" anything.
             * The whole grid is already one island.
             *
             * Returning n*n is also naturally handled if we initialize
             * answer with the existing maximum island, but this explicit
             * check makes the intent very clear.
             */
            boolean hasZero = false;

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (grid[i][j] == 0) {
                        hasZero = true;

                        // Flip this cell.
                        grid[i][j] = 1;

                        // Recalculate the largest island from scratch.
                        answer = Math.max(answer, largestExistingIsland(grid));

                        // Undo the experiment.
                        grid[i][j] = 0;
                    }
                }
            }

            // If no zero exists, the answer is the whole grid.
            return hasZero ? answer : n * n;
        }

        private int largestExistingIsland(int[][] grid) {
            int n = grid.length;
            boolean[][] visited = new boolean[n][n];

            int maxArea = 0;

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {

                    if (grid[i][j] == 1 && !visited[i][j]) {
                        maxArea = Math.max(
                            maxArea,
                            floodFill(grid, visited, i, j)
                        );
                    }
                }
            }

            return maxArea;
        }

        private int floodFill(
            int[][] grid,
            boolean[][] visited,
            int sr,
            int sc
        ) {
            int n = grid.length;

            Queue<int[]> queue = new ArrayDeque<>();
            queue.offer(new int[]{sr, sc});
            visited[sr][sc] = true;

            int area = 0;

            while (!queue.isEmpty()) {
                int[] cell = queue.poll();
                int r = cell[0];
                int c = cell[1];

                area++;

                for (int[] d : FOUR_DIRS) {
                    int nr = r + d[0];
                    int nc = c + d[1];

                    if (isValid(nr, nc, n)
                        && grid[nr][nc] == 1
                        && !visited[nr][nc]) {

                        visited[nr][nc] = true;
                        queue.offer(new int[]{nr, nc});
                    }
                }
            }

            return area;
        }
    }

    // ========================================================================
    // APPROACH 2: DFS + ISLAND LABELING
    // ========================================================================
    //
    // This is usually the cleanest solution to explain.
    //
    // ------------------------------------------------------------------------
    // STEP 1: Label each island
    // ------------------------------------------------------------------------
    //
    // Original:
    //
    //     1 1 0
    //     1 0 0
    //     0 0 1
    //
    // After labeling:
    //
    //     2 2 0
    //     2 0 0
    //     0 0 3
    //
    // areaMap:
    //
    //     2 -> 3
    //     3 -> 1
    //
    // Why start IDs at 2?
    //
    //     0 = water
    //     1 = original/unvisited land
    //
    // After labeling:
    //
    //     >= 2 = island IDs
    //
    // So:
    //
    //     if (grid[nr][nc] > 1)
    //
    // means "this neighbor belongs to a labeled island".
    //
    //
    // ------------------------------------------------------------------------
    // STEP 2: For every 0, calculate what happens if we flip it
    // ------------------------------------------------------------------------
    //
    // Suppose:
    //
    //     2 2 0 3
    //     2 0 0 3
    //     0 0 3 3
    //
    // Consider some 0 whose neighbors touch islands 2 and 3.
    //
    // Then:
    //
    //     newArea = 1 + area[2] + area[3]
    //
    // The +1 is the flipped cell itself.
    //
    // We use a Set because an island might touch the zero on multiple sides.
    //
    // ========================================================================

    static class SolutionDFS {

        public int largestIsland(int[][] grid) {
            int n = grid.length;

            /*
             * island ID -> island area
             *
             * Example:
             *
             *     2 -> 5
             *     3 -> 2
             *     4 -> 7
             */
            Map<Integer, Integer> areaMap = new HashMap<>();

            int islandId = 2;

            // ----------------------------------------------------------------
            // STEP 1: Label every island and record its area.
            // ----------------------------------------------------------------
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {

                    if (grid[i][j] == 1) {

                        int area = dfs(
                            grid,
                            i,
                            j,
                            islandId
                        );

                        areaMap.put(islandId, area);
                        islandId++;
                    }
                }
            }

            /*
             * maxArea = 0 is important.
             *
             * If the grid contains at least one 0, maxArea will be updated
             * while processing zeroes.
             *
             * If the grid contains no 0:
             *
             *     grid = all 1s
             *
             * no zero will be processed, so maxArea remains 0.
             *
             * In that case the answer is n*n.
             */
            int maxArea = 0;

            // ----------------------------------------------------------------
            // STEP 2: Try flipping every zero.
            // ----------------------------------------------------------------
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {

                    if (grid[i][j] != 0) {
                        continue;
                    }

                    /*
                     * We are imagining:
                     *
                     *     grid[i][j] = 1
                     *
                     * instead of actually changing the grid.
                     *
                     * Start with 1 for this new land cell.
                     */
                    int newArea = 1;

                    /*
                     * We may see the same island from multiple directions.
                     *
                     * Example:
                     *
                     *     2 2
                     *     2 0
                     *
                     * The zero has two neighbors belonging to island 2.
                     *
                     * Without 'seen':
                     *
                     *     newArea = 1 + area[2] + area[2]
                     *
                     * WRONG.
                     *
                     * With 'seen':
                     *
                     *     newArea = 1 + area[2]
                     *
                     * CORRECT.
                     */
                    Set<Integer> seen = new HashSet<>();

                    for (int[] d : FOUR_DIRS) {
                        int ni = i + d[0];
                        int nj = j + d[1];

                        if (!isValid(ni, nj, n)) {
                            continue;
                        }

                        // > 1 means this neighbor belongs to a labeled island.
                        if (grid[ni][nj] > 1) {

                            int id = grid[ni][nj];

                            if (seen.add(id)) {
                                newArea += areaMap.get(id);
                            }
                        }
                    }

                    maxArea = Math.max(maxArea, newArea);
                }
            }

            /*
             * Case:
             *
             *     1 1
             *     1 1
             *
             * There is no zero to flip.
             *
             * Answer = 4.
             */
            return maxArea == 0 ? n * n : maxArea;
        }

        private int dfs(
            int[][] grid,
            int r,
            int c,
            int islandId
        ) {
            int n = grid.length;

            /*
             * Base case:
             *
             * Stop when:
             *
             *     1. outside the grid
             *     2. current cell is not an unvisited land cell
             *
             * Once a cell has been relabeled from 1 to islandId,
             * future DFS calls will not visit it again.
             */
            if (!isValid(r, c, n) || grid[r][c] != 1) {
                return 0;
            }

            /*
             * Mark first.
             *
             * This is crucial to avoid revisiting this cell through another path.
             */
            grid[r][c] = islandId;

            int area = 1;

            /*
             * Explore all 4 neighbors.
             */
            for (int[] d : FOUR_DIRS) {
                area += dfs(
                    grid,
                    r + d[0],
                    c + d[1],
                    islandId
                );
            }

            return area;
        }
    }

    // ========================================================================
    // APPROACH 3: BFS + ISLAND LABELING
    // ========================================================================
    //
    // Same mathematical idea as DFS.
    //
    // Difference:
    //
    // DFS:
    //
    //     recursion / call stack
    //
    // BFS:
    //
    //     explicit Queue
    //
    // Why BFS may be preferable:
    //
    // A very large connected island can make recursive DFS very deep.
    // In languages/runtimes with limited call-stack size, this can cause
    // StackOverflowError.
    //
    // BFS avoids recursion.
    //
    // ========================================================================

    static class SolutionBFS {

        public int largestIsland(int[][] grid) {
            int n = grid.length;

            Map<Integer, Integer> areaMap = new HashMap<>();

            int islandId = 2;

            // ----------------------------------------------------------------
            // STEP 1: Label each island using BFS.
            // ----------------------------------------------------------------
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {

                    if (grid[i][j] == 1) {

                        int area = bfs(
                            grid,
                            i,
                            j,
                            islandId
                        );

                        areaMap.put(islandId, area);
                        islandId++;
                    }
                }
            }

            int maxArea = 0;

            // ----------------------------------------------------------------
            // STEP 2: Try flipping every zero.
            // ----------------------------------------------------------------
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {

                    if (grid[i][j] != 0) {
                        continue;
                    }

                    int newArea = 1;
                    Set<Integer> seen = new HashSet<>();

                    for (int[] d : FOUR_DIRS) {

                        int ni = i + d[0];
                        int nj = j + d[1];

                        if (!isValid(ni, nj, n)) {
                            continue;
                        }

                        if (grid[ni][nj] > 1) {

                            int id = grid[ni][nj];

                            if (seen.add(id)) {
                                newArea += areaMap.get(id);
                            }
                        }
                    }

                    maxArea = Math.max(maxArea, newArea);
                }
            }

            /*
             * No zero means the grid is already one complete island.
             */
            return maxArea == 0 ? n * n : maxArea;
        }

        private int bfs(
            int[][] grid,
            int sr,
            int sc,
            int islandId
        ) {
            int n = grid.length;

            Queue<int[]> queue = new ArrayDeque<>();

            /*
             * Mark the starting cell BEFORE enqueueing.
             *
             * This is the BFS equivalent of "mark before recursive DFS".
             */
            grid[sr][sc] = islandId;
            queue.offer(new int[]{sr, sc});

            int area = 0;

            while (!queue.isEmpty()) {

                int[] cell = queue.poll();

                int r = cell[0];
                int c = cell[1];

                area++;

                for (int[] d : FOUR_DIRS) {

                    int nr = r + d[0];
                    int nc = c + d[1];

                    if (isValid(nr, nc, n)
                        && grid[nr][nc] == 1) {

                        /*
                         * Mark BEFORE adding to queue.
                         *
                         * Otherwise two different neighbors could both
                         * enqueue the same cell before either one processes it.
                         */
                        grid[nr][nc] = islandId;

                        queue.offer(new int[]{nr, nc});
                    }
                }
            }

            return area;
        }
    }

    // ========================================================================
    // APPROACH 4: DSU / UNION-FIND
    // ========================================================================
    //
    // DSU represents connected components using a forest.
    //
    // Each cell gets a one-dimensional index:
    //
    //     index = row * n + col
    //
    // Example for n = 3:
    //
    //     (0,0) (0,1) (0,2)       0 1 2
    //     (1,0) (1,1) (1,2)  =>   3 4 5
    //     (2,0) (2,1) (2,2)       6 7 8
    //
    //
    // DSU supports:
    //
    //     find(x)
    //         -> representative/root of x's component
    //
    //     union(a, b)
    //         -> connect two components
    //
    // Optimizations:
    //
    //     1. Path compression
    //     2. Union by size
    //
    // These make operations almost constant amortized time:
    //
    //     O(alpha(N))
    //
    // where alpha is the inverse Ackermann function.
    //
    // For practical purposes, this is effectively constant.
    //
    //
    // Why only RIGHT and DOWN during the build phase?
    //
    // When connecting all adjacent land cells, we don't need to check all
    // four directions.
    //
    // Example:
    //
    //     X -- X
    //     |
    //     X
    //
    // If we process left-to-right, top-to-bottom, every edge can be found
    // exactly once using:
    //
    //     RIGHT
    //     DOWN
    //
    // Later, when evaluating a zero, we DO check all four directions because
    // the zero can touch an island from any side.
    //
    // ========================================================================

    static class SolutionDSU {

        private static final int[][] BUILD_DIRS = {
            {1, 0},  // DOWN
            {0, 1}   // RIGHT
        };

        public int largestIsland(int[][] grid) {
            int n = grid.length;

            DSU dsu = new DSU(n * n);

            // ----------------------------------------------------------------
            // STEP 1: Build connected components among existing land cells.
            // ----------------------------------------------------------------

            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {

                    if (grid[r][c] != 1) {
                        continue;
                    }

                    int current = getIndex(r, c, n);

                    for (int[] d : BUILD_DIRS) {

                        int nr = r + d[0];
                        int nc = c + d[1];

                        if (isValid(nr, nc, n)
                            && grid[nr][nc] == 1) {

                            int neighbor = getIndex(nr, nc, n);

                            dsu.union(current, neighbor);
                        }
                    }
                }
            }

            int maxArea = 0;

            // ----------------------------------------------------------------
            // STEP 2: Try flipping every zero.
            // ----------------------------------------------------------------
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {

                    if (grid[r][c] != 0) {
                        continue;
                    }

                    int newArea = 1;

                    /*
                     * Store DSU roots, not raw cell indexes.
                     *
                     * Two different neighboring cells may belong to the
                     * same component.
                     *
                     * Example:
                     *
                     *     A A
                     *     A 0
                     *
                     * Both neighboring A cells can have different indexes,
                     * but after find(), they have the same root.
                     */
                    Set<Integer> seenRoots = new HashSet<>();

                    for (int[] d : FOUR_DIRS) {

                        int nr = r + d[0];
                        int nc = c + d[1];

                        if (!isValid(nr, nc, n)) {
                            continue;
                        }

                        if (grid[nr][nc] == 1) {

                            int neighborIndex = getIndex(nr, nc, n);
                            int root = dsu.find(neighborIndex);

                            /*
                             * Count each connected component only once.
                             */
                            if (seenRoots.add(root)) {
                                newArea += dsu.size[root];
                            }
                        }
                    }

                    maxArea = Math.max(maxArea, newArea);
                }
            }

            /*
             * If maxArea == 0, there was no zero.
             *
             * Then all cells are land.
             *
             * We could simply return n*n.
             *
             * Doing so is clearer than scanning DSU roots.
             */
            if (maxArea == 0) {
                return n * n;
            }

            return maxArea;
        }

        private int getIndex(int r, int c, int n) {
            return r * n + c;
        }

        private boolean isValid(int r, int c, int n) {
            return r >= 0 && r < n && c >= 0 && c < n;
        }

        // --------------------------------------------------------------------
        // DSU implementation
        // --------------------------------------------------------------------

        static class DSU {

            int[] parent;
            int[] size;

            DSU(int n) {
                parent = new int[n];
                size = new int[n];

                for (int i = 0; i < n; i++) {
                    parent[i] = i;

                    /*
                     * Initially every node is its own component.
                     *
                     * IMPORTANT:
                     *
                     * Water cells are also initialized here, but we NEVER
                     * union them and never use them when processing a zero.
                     *
                     * Therefore their component sizes do not affect answers.
                     */
                    size[i] = 1;
                }
            }

            int find(int x) {

                /*
                 * Path compression:
                 *
                 * Before:
                 *
                 *     x -> p -> q -> root
                 *
                 * After:
                 *
                 *     x -------> root
                 *
                 * This makes future find() operations faster.
                 */
                if (parent[x] != x) {
                    parent[x] = find(parent[x]);
                }

                return parent[x];
            }

            void union(int a, int b) {

                int rootA = find(a);
                int rootB = find(b);

                /*
                 * Already connected.
                 */
                if (rootA == rootB) {
                    return;
                }

                /*
                 * Union by size:
                 *
                 * Attach the smaller tree under the larger tree.
                 *
                 * This keeps the tree shallow.
                 */
                if (size[rootA] < size[rootB]) {

                    parent[rootA] = rootB;
                    size[rootB] += size[rootA];

                } else {

                    parent[rootB] = rootA;
                    size[rootA] += size[rootB];
                }
            }
        }
    }

    // ========================================================================
    // COMMON HELPER
    // ========================================================================

    private static boolean isValid(int r, int c, int n) {
        return r >= 0 && r < n && c >= 0 && c < n;
    }

    // ========================================================================
    // DETAILED TRACE
    // ========================================================================
    //
    // This method is intentionally verbose.
    //
    // It demonstrates the DFS-labeling solution on:
    //
    //     1 0
    //     0 1
    //
    // Expected:
    //
    //     3
    //
    // ------------------------------------------------------------------------
    // TRACE
    // ------------------------------------------------------------------------
    //
    // Initial:
    //
    //     1 0
    //     0 1
    //
    // Find first island:
    //
    //     cell (0,0)
    //
    // Label it as island 2:
    //
    //     2 0
    //     0 1
    //
    // areaMap:
    //
    //     2 -> 1
    //
    // Next unvisited island:
    //
    //     cell (1,1)
    //
    // Label it as island 3:
    //
    //     2 0
    //     0 3
    //
    // areaMap:
    //
    //     2 -> 1
    //     3 -> 1
    //
    //
    // Now process zero (0,1):
    //
    // neighbors:
    //
    //     DOWN  -> (1,1) = island 3
    //     LEFT  -> (0,0) = island 2
    //     UP    -> outside
    //     RIGHT -> outside
    //
    // newArea:
    //
    //     1                // flipped cell
    //   + 1                // island 3
    //   + 1                // island 2
    //   = 3
    //
    // Process zero (1,0):
    //
    // neighbors are also islands 2 and 3.
    //
    // newArea = 3
    //
    // Answer = 3.
    //
    // ========================================================================

    private static void detailedTraceDFS(int[][] original) {

        System.out.println();
        System.out.println("=======================================================");
        System.out.println("DETAILED DFS TRACE");
        System.out.println("=======================================================");

        int[][] grid = copyGrid(original);

        printGrid(grid);

        int n = grid.length;
        Map<Integer, Integer> areaMap = new HashMap<>();

        int islandId = 2;

        System.out.println();
        System.out.println("STEP 1: Label islands");
        System.out.println();

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {

                if (grid[i][j] == 1) {

                    System.out.println(
                        "Found unvisited land at " + formatCell(i, j)
                            + " -> start island ID " + islandId
                    );

                    int area = traceDFS(
                        grid,
                        i,
                        j,
                        islandId
                    );

                    areaMap.put(islandId, area);

                    System.out.println(
                        "Island " + islandId + " area = " + area
                    );

                    System.out.println("Grid after labeling:");
                    printGrid(grid);
                    System.out.println();

                    islandId++;
                }
            }
        }

        System.out.println("Area map = " + areaMap);

        System.out.println();
        System.out.println("STEP 2: Try flipping every zero");
        System.out.println();

        int answer = 0;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {

                if (grid[i][j] != 0) {
                    continue;
                }

                System.out.println(
                    "Considering flip at " + formatCell(i, j)
                );

                int newArea = 1;
                Set<Integer> seen = new HashSet<>();

                for (int[] d : FOUR_DIRS) {

                    int ni = i + d[0];
                    int nj = j + d[1];

                    if (!isValid(ni, nj, n)) {
                        System.out.println(
                            "  Neighbor "
                                + formatCell(ni, nj)
                                + " -> outside grid"
                        );
                        continue;
                    }

                    int value = grid[ni][nj];

                    if (value <= 1) {
                        System.out.println(
                            "  Neighbor "
                                + formatCell(ni, nj)
                                + " -> not a labeled island"
                        );
                        continue;
                    }

                    int id = value;

                    System.out.println(
                        "  Neighbor "
                            + formatCell(ni, nj)
                            + " -> island " + id
                            + " (area=" + areaMap.get(id) + ")"
                    );

                    if (seen.add(id)) {
                        newArea += areaMap.get(id);
                    } else {
                        System.out.println(
                            "      Already counted island " + id
                                + " -> skip duplicate"
                        );
                    }
                }

                System.out.println(
                    "  Result if flipped = " + newArea
                );

                answer = Math.max(answer, newArea);
            }
        }

        System.out.println();
        System.out.println("FINAL ANSWER = " + answer);
    }

    private static int traceDFS(
        int[][] grid,
        int r,
        int c,
        int islandId
    ) {
        int n = grid.length;

        if (!isValid(r, c, n) || grid[r][c] != 1) {
            return 0;
        }

        System.out.println(
            "    Visit " + formatCell(r, c)
                + " -> label as " + islandId
        );

        grid[r][c] = islandId;

        int area = 1;

        for (int[] d : FOUR_DIRS) {
            area += traceDFS(
                grid,
                r + d[0],
                c + d[1],
                islandId
            );
        }

        return area;
    }

    // ========================================================================
    // VISUAL / ASCII EXPLANATION
    // ========================================================================
    //
    // Consider:
    //
    //     1 1 0
    //     1 0 1
    //     0 1 1
    //
    // Step 1: Label connected islands.
    //
    //     A A 0
    //     A 0 B
    //     0 B B
    //
    // Therefore:
    //
    //     area(A) = 3
    //     area(B) = 3
    //
    // Now consider the center:
    //
    //     A A 0
    //     A X B
    //     0 B B
    //
    // If X becomes land:
    //
    //     area = 1 + area(A) + area(B)
    //          = 1 + 3 + 3
    //          = 7
    //
    //
    // Why do we need a Set?
    //
    // Consider:
    //
    //     A A
    //     A X
    //
    // X touches A from:
    //
    //     UP
    //     LEFT
    //
    // Without a Set:
    //
    //     1 + 3 + 3 = 7   <-- WRONG
    //
    // Correct:
    //
    //     1 + 3 = 4
    //
    //
    // DSU visual:
    //
    //     cells:
    //
    //     A A 0
    //     A 0 B
    //     0 B B
    //
    // Convert coordinates to indexes:
    //
    //     0 1 2
    //     3 4 5
    //     6 7 8
    //
    // Land nodes:
    //
    //     0,1,3   belong to one component
    //
    //     5,7,8   belong to another component
    //
    // DSU may internally look like:
    //
    //         0
    //       /   \
    //      1     3
    //
    // and:
    //
    //         5
    //        / \
    //       7   8
    //
    // The exact tree shape depends on union operations, but the root
    // represents the component and size[root] stores its area.
    //
    // ========================================================================

    // ========================================================================
    // COMPLEXITY SUMMARY
    // ========================================================================
    //
    // Let:
    //
    //     N = grid dimension
    //     Total cells = N^2
    //
    // ------------------------------------------------------------------------
    // Brute force
    // ------------------------------------------------------------------------
    //
    // Number of zeroes:
    //
    //     O(N^2)
    //
    // Work per flip:
    //
    //     O(N^2)
    //
    // Total:
    //
    //     O(N^4)
    //
    // Space:
    //
    //     O(N^2)
    //
    // for the visited matrix.
    //
    //
    // ------------------------------------------------------------------------
    // DFS labeling
    // ------------------------------------------------------------------------
    //
    // Every cell is visited a constant number of times.
    //
    // Labeling:
    //
    //     O(N^2)
    //
    // Processing all zeroes:
    //
    //     each zero checks at most 4 neighbors
    //
    //     O(N^2)
    //
    // Total:
    //
    //     O(N^2)
    //
    // Extra space:
    //
    //     O(N^2)
    //
    // recursion stack + area map + temporary Set.
    //
    //
    // ------------------------------------------------------------------------
    // BFS labeling
    // ------------------------------------------------------------------------
    //
    // Time:
    //
    //     O(N^2)
    //
    // Extra space:
    //
    //     O(N^2)
    //
    // queue + area map + temporary Set.
    //
    //
    // ------------------------------------------------------------------------
    // DSU
    // ------------------------------------------------------------------------
    //
    // There are O(N^2) union/find operations.
    //
    // With path compression + union by size:
    //
    //     O(N^2 * alpha(N^2))
    //
    // which is effectively:
    //
    //     O(N^2)
    //
    // in practical terms.
    //
    // Space:
    //
    //     O(N^2)
    //
    // parent + size arrays.
    //
    // ========================================================================

    // ========================================================================
    // COMMON PITFALLS
    // ========================================================================
    //
    // 1. Counting the same neighboring island more than once
    //
    //    WRONG:
    //
    //        newArea += areaMap.get(grid[nr][nc]);
    //
    //    with no Set.
    //
    //    FIX:
    //
    //        Set<Integer> seen = new HashSet<>();
    //
    //
    // 2. Forgetting the +1
    //
    //    The flipped water cell becomes land.
    //
    //    So:
    //
    //        newArea = 1 + neighboring island areas
    //
    //
    // 3. Forgetting the all-land case
    //
    //        1 1
    //        1 1
    //
    //    There is no zero to process.
    //
    //    Answer is n*n.
    //
    //
    // 4. Using 4 directions for DSU build is unnecessary
    //
    //    For connecting existing land cells, RIGHT + DOWN is sufficient
    //    during a row-major traversal.
    //
    //    This avoids processing every undirected edge twice.
    //
    //
    // 5. DSU must use the ROOT when reading component size
    //
    //    Wrong:
    //
    //        size[neighborIndex]
    //
    //    Correct:
    //
    //        int root = find(neighborIndex);
    //        size[root]
    //
    //
    // 6. In BFS, mark a cell when enqueueing, not when dequeuing
    //
    //    Otherwise the same cell can be added to the queue multiple times.
    //
    //
    // 7. In DFS, mark a cell before exploring neighbors
    //
    //    Otherwise neighboring recursive calls can revisit the same cell.
    //
    //
    // 8. Mutating the input grid
    //
    //    DFS/BFS labeling intentionally changes:
    //
    //        1 -> island ID
    //
    //    This is fine when mutation is allowed.
    //
    //    If the original grid must remain unchanged, make a copy or use a
    //    separate visited/component array.
    //
    //
    // 9. Java recursion depth
    //
    //    DFS can theoretically throw StackOverflowError for a huge connected
    //    region.
    //
    //    BFS avoids that specific issue.
    //
    //
    // 10. Diagonal cells are NOT neighbors
    //
    //    For:
    //
    //        1 0
    //        0 1
    //
    //    the cells are separate islands.
    //
    // ========================================================================

    // ========================================================================
    // INTERVIEW REASONING SCRIPT
    // ========================================================================
    //
    // A strong interview explanation can sound like this:
    //
    // "A brute-force solution would flip every zero and run a flood fill, which
    // is O(n^4). The repeated work is recalculating the same islands over and
    // over.
    //
    // So I will first identify every existing island once. I assign each island
    // a unique ID and store its area.
    //
    // Then for every zero, I look at its four neighbors. Each neighboring
    // island can be added at most once, so I store the island IDs in a Set.
    // The candidate size is:
    //
    //     1 + sum(area of unique neighboring islands)
    //
    // The final answer is the maximum candidate, or n*n if the grid is already
    // all land.
    //
    // This reduces the total complexity to O(n^2)."
    //
    // This is usually the most important part of the solution:
    //
    //     DON'T recompute connectivity after every flip.
    //
    //     PRECOMPUTE the connectivity once.
    //
    // ========================================================================

    // ========================================================================
    // MAIN - DEMONSTRATION
    // ========================================================================

    public static void main(String[] args) {

        int[][] example = {
            {1, 0},
            {0, 1}
        };

        System.out.println("=======================================================");
        System.out.println("INPUT GRID");
        System.out.println("=======================================================");
        printGrid(example);

        /*
         * IMPORTANT:
         *
         * DFS/BFS solutions MODIFY the grid while labeling islands.
         * Therefore, use a copy when comparing multiple implementations.
         */

        int dfsAnswer =
            new SolutionDFS().largestIsland(copyGrid(example));

        int bfsAnswer =
            new SolutionBFS().largestIsland(copyGrid(example));

        int dsuAnswer =
            new SolutionDSU().largestIsland(copyGrid(example));

        int bruteAnswer =
            new SolutionBruteForce().largestIsland(copyGrid(example));

        System.out.println();
        System.out.println("Brute Force = " + bruteAnswer);
        System.out.println("DFS          = " + dfsAnswer);
        System.out.println("BFS          = " + bfsAnswer);
        System.out.println("DSU          = " + dsuAnswer);

        /*
         * Expected:
         *
         *     Brute Force = 3
         *     DFS          = 3
         *     BFS          = 3
         *     DSU          = 3
         */

        detailedTraceDFS(example);

        // --------------------------------------------------------------------
        // Additional edge cases
        // --------------------------------------------------------------------

        int[][] allLand = {
            {1, 1},
            {1, 1}
        };

        int[][] allWater = {
            {0, 0},
            {0, 0}
        };

        int[][] largerExample = {
            {1, 1, 0},
            {1, 0, 1},
            {0, 1, 1}
        };

        System.out.println();
        System.out.println("=======================================================");
        System.out.println("EDGE CASES / EXTRA TESTS");
        System.out.println("=======================================================");

        System.out.println(
            "All land expected 4 -> "
                + new SolutionDFS().largestIsland(copyGrid(allLand))
        );

        System.out.println(
            "All water expected 1 -> "
                + new SolutionDFS().largestIsland(copyGrid(allWater))
        );

        System.out.println(
            "Larger example expected 7 -> "
                + new SolutionDFS().largestIsland(copyGrid(largerExample))
        );
    }
}

