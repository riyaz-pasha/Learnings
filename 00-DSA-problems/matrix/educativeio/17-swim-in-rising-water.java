/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We have an N x N grid where each cell represents an elevation.
 * Water rises over time; at time `t`, the water level is `t`.
 * We can only swim into a cell if its elevation is less than or equal to `t`.
 * We want to find the minimum time `t` it takes to find a valid path from the 
 * top-left cell (0, 0) to the bottom-right cell (N-1, N-1).
 * 
 * In simpler terms: We need to find a path from start to end where the 
 * MAXIMUM elevation along that path is as small as possible.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can the grid be 1x1? 
 *    (Yes, constraints say 1 <= N <= 30. If N=1, the answer is just grid[0][0]).
 * 2. Does the starting cell or ending cell restrict the minimum time?
 *    (Yes! You cannot even start swimming until time t = grid[0][0], and you 
 *     cannot finish until t >= grid[N-1][N-1]. The answer must be at least the 
 *     maximum of these two).
 * 3. Are the elevation values unique?
 *    (Yes, the problem states each value is unique and ranges from 0 to N^2 - 1. 
 *     This implies every number in that range appears exactly once).
 * 4. Can we move diagonally?
 *    (No, only 4-directionally: up, down, left, right).
 * 
 * ============================================================================
 * EDGE CASES & EXAMPLES TO THINK OF
 * ============================================================================
 * 1. 1x1 Grid: [[5]] -> Minimum time is 5.
 * 2. 2x2 Grid:
 *    [0, 2]
 *    [1, 3]
 *    Path options: (0,0)->(0,1)->(1,1) [max is 3]. OR (0,0)->(1,0)->(1,1) [max is 3].
 *    Output: 3.
 * 3. Snake Path (Counter-Intuitive):
 *    [ 0,  1,  2]
 *    [ 7,  8,  3]
 *    [ 6,  5,  4]
 *    The shortest path by distance goes straight down and right, but hits 7 and 8. 
 *    The optimal path by TIME goes around the outer edge (0->1->2->3->4).
 *    Output: 4.
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Binary Search + BFS/DFS (The Intuitive Approach):
 *    - The time `t` must be between `max(grid[0][0], grid[N-1][N-1])` and `N^2 - 1`.
 *    - We can Binary Search for the optimal time `t`.
 *    - For a guessed time `mid`, perform a standard BFS or DFS from (0,0). 
 *      We are only allowed to step on cells where `grid[r][c] <= mid`.
 *    - If we reach (N-1, N-1), `mid` is valid, so we try a smaller time (high = mid).
 *    - If we cannot reach it, `mid` is too small (low = mid + 1).
 *    - Time: O(N^2 * log(N^2)) -> O(N^2 * log N). Space: O(N^2) for visited array.
 * 
 * 2. Dijkstra's Algorithm / Min-Heap (The Most Optimal/Professional Approach):
 *    - Treat this as a shortest-path graph problem where the "weight" of a path 
 *      is the MAXIMUM elevation encountered on it.
 *    - Use a Min-Heap (PriorityQueue). Start at (0,0). 
 *    - The heap stores `(row, col, max_time_so_far)`, prioritized by smallest `max_time`.
 *    - Pop the cell with the smallest `max_time`. If it's the destination, we are done!
 *    - Otherwise, explore its 4 neighbors. For each neighbor, the new `max_time` is 
 *      `Math.max(current_max_time, grid[neighbor_row][neighbor_col])`.
 *    - Add unvisited neighbors to the heap.
 *    - Time: O(N^2 * log N) due to heap operations. Space: O(N^2).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Dijkstra's Approach)
 * ============================================================================
 * Grid: 
 * [0, 2]
 * [1, 3]
 * 
 * Min-Heap initialized with start: [(r=0, c=0, maxTime=0)]
 * Visited: (0,0)
 * 
 * Iteration 1:
 * - Pop (0, 0, 0). Not destination.
 * - Neighbors of (0,0): 
 *   - (0, 1) -> value 2. new maxTime = max(0, 2) = 2. Push (0, 1, 2).
 *   - (1, 0) -> value 1. new maxTime = max(0, 1) = 1. Push (1, 0, 1).
 * - Heap: [(1, 0, 1), (0, 1, 2)] (sorted by maxTime)
 * 
 * Iteration 2:
 * - Pop (1, 0, 1). Not destination.
 * - Neighbors of (1,0):
 *   - (1, 1) -> value 3. new maxTime = max(1, 3) = 3. Push (1, 1, 3).
 * - Heap: [(0, 1, 2), (1, 1, 3)]
 * 
 * Iteration 3:
 * - Pop (0, 1, 2). Not destination.
 * - Neighbors of (0,1):
 *   - (1, 1) -> value 3. Already visited/in queue, but if we process, new max = 3.
 * - Heap: [(1, 1, 3)]
 * 
 * Iteration 4:
 * - Pop (1, 1, 3). It IS the destination (r=1, c=1)!
 * - Return maxTime = 3.
 */

import java.util.*;

public class SwimInRisingWater {

    public static void main(String[] args) {
        int[][] grid1 = {
            {0, 2},
            {1, 3}
        };

        int[][] grid2 = {
            { 0,  1, 23, 24, 25},
            {16, 15, 14, 13, 26},
            {17,  2,  3, 12, 27},
            {18, 19,  4, 11, 28},
            {20, 21,  5,  6, 29} // The optimal path spirals inwards!
        };

        System.out.println("--- Solution 1: Binary Search + BFS ---");
        System.out.println("Grid 1 Min Time: " + swimInWaterBS(grid1));
        System.out.println("Grid 2 Min Time: " + swimInWaterBS(grid2));

        System.out.println("\n--- Solution 2: Dijkstra's with Min-Heap (Optimal) ---");
        System.out.println("Grid 1 Min Time: " + swimInWaterDijkstra(grid1));
        System.out.println("Grid 2 Min Time: " + swimInWaterDijkstra(grid2));
    }

    /**
     * SOLUTION 1: Binary Search + BFS
     * 
     * Idea: Guess a time 'T'. Check if we can reach the end using only cells <= T.
     * If yes, try a smaller T. If no, try a larger T.
     * 
     * Time Complexity: O(N^2 log N) - Binary search space is N^2. Log(N^2) = 2 log N.
     *                  Inside, BFS visits N^2 cells.
     * Space Complexity: O(N^2) - For the visited array and BFS queue.
     */
    public static int swimInWaterBS(int[][] grid) {
        int n = grid.length;
        
        // The minimum possible answer is constrained by the start and end cells
        int low = Math.max(grid[0][0], grid[n - 1][n - 1]);
        int high = n * n - 1; // Maximum possible elevation in the grid
        
        while (low < high) {
            int mid = low + (high - low) / 2;
            
            if (canReachDestination(grid, mid, n)) {
                high = mid; // Try to find an even smaller time
            } else {
                low = mid + 1; // Time was too small, increase it
            }
        }
        
        return low;
    }
    
    /**
     * Helper for Binary Search: Standard BFS to check reachability.
     */
    private static boolean canReachDestination(int[][] grid, int maxAllowedTime, int n) {
        boolean[][] visited = new boolean[n][n];
        Queue<int[]> queue = new LinkedList<>();
        
        queue.offer(new int[]{0, 0});
        visited[0][0] = true;
        
        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};
        
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int r = curr[0];
            int c = curr[1];
            
            if (r == n - 1 && c == n - 1) {
                return true;
            }
            
            for (int i = 0; i < 4; i++) {
                int nr = r + dr[i];
                int nc = c + dc[i];
                
                // If neighbor is valid, unvisited, and its elevation <= maxAllowedTime
                if (nr >= 0 && nr < n && nc >= 0 && nc < n 
                    && !visited[nr][nc] && grid[nr][nc] <= maxAllowedTime) {
                    visited[nr][nc] = true;
                    queue.offer(new int[]{nr, nc});
                }
            }
        }
        
        return false;
    }

    /**
     * SOLUTION 2: Modified Dijkstra's Algorithm (Most Professional)
     * 
     * Idea: Use a Min-Heap to continually expand the path that currently has the 
     * lowest maximum elevation. Utilizing Java 16+ `record` makes this incredibly clean.
     * 
     * Time Complexity: O(N^2 log N) - Every cell is processed and pushed to heap once.
     * Space Complexity: O(N^2) - For visited array and Min-Heap.
     */
    public static int swimInWaterDijkstra(int[][] grid) {
        int n = grid.length;
        
        // Record to store grid coordinates and the maximum time encountered on the path so far.
        // Implements Comparable to allow the PriorityQueue to sort naturally by maxTime.
        record Cell(int r, int c, int maxTime) implements Comparable<Cell> {
            @Override
            public int compareTo(Cell other) {
                return Integer.compare(this.maxTime, other.maxTime);
            }
        }
        
        PriorityQueue<Cell> minHeap = new PriorityQueue<>();
        boolean[][] visited = new boolean[n][n];
        
        // Start at (0, 0)
        minHeap.offer(new Cell(0, 0, grid[0][0]));
        visited[0][0] = true;
        
        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};
        
        while (!minHeap.isEmpty()) {
            Cell curr = minHeap.poll();
            
            // If we reached the bottom-right, since we used a Min-Heap, this is 
            // GUARANTEED to be the path with the minimum peak elevation!
            if (curr.r() == n - 1 && curr.c() == n - 1) {
                return curr.maxTime();
            }
            
            // Explore 4 neighbors
            for (int i = 0; i < 4; i++) {
                int nr = curr.r() + dr[i];
                int nc = curr.c() + dc[i];
                
                if (nr >= 0 && nr < n && nc >= 0 && nc < n && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    
                    // The new peak time is the max of our current peak and the neighbor's elevation
                    int nextMaxTime = Math.max(curr.maxTime(), grid[nr][nc]);
                    minHeap.offer(new Cell(nr, nc, nextMaxTime));
                }
            }
        }
        
        return -1; // Should never be reached given constraints
    }

    /**
     * ============================================================================
     * FOLLOW-UPS TO PREPARE FOR
     * ============================================================================
     * 1. What if you also need to return the actual path taken?
     *    Answer: You would maintain a `Cell[][] parent = new Cell[n][n]` array. 
     *    When visiting a neighbor `(nr, nc)`, set `parent[nr][nc] = curr`. 
     *    Once the destination is reached, backtrack using the `parent` array from 
     *    `(n-1, n-1)` to `(0, 0)` and reverse it to get the path.
     * 
     * 2. Is it possible to solve this using Union-Find (Disjoint Set)?
     *    Answer: Yes! We can flatten the grid and sort all cells by their elevation.
     *    Iterate through the sorted cells, adding them one by one. As we add a cell, 
     *    we union it with any of its 4 neighbors that have already been added.
     *    We stop the moment cell (0,0) and cell (n-1, n-1) belong to the same 
     *    connected component (i.e., `find(0) == find(n*n - 1)`). 
     *    The elevation of the last cell added is our answer. Time: O(N^2 log N).
     */
}



/**
 * =====================================================================================
 * 🏊‍♂️ SWIM IN RISING WATER — BINARY SEARCH + BFS (INTERVIEW MASTER FILE)
 * =====================================================================================
 *
 * 🧠 PROBLEM UNDERSTANDING (VERY IMPORTANT)
 * -------------------------------------------------------------------------------------
 * We are given a grid where:
 * - Each cell has an elevation
 * - Water level rises with time t
 * - At time t, we can ONLY step on cells where elevation <= t
 *
 * Goal:
 * 👉 Reach from (0,0) → (n-1,n-1) in MINIMUM time
 *
 *
 * 🔥 KEY OBSERVATION (CORE IDEA)
 * -------------------------------------------------------------------------------------
 * We are NOT minimizing steps.
 * We are minimizing the MAX elevation encountered along the path.
 *
 * Rephrase problem:
 *
 * 👉 Find a path such that:
 *    max(grid[i][j] along path) is MINIMUM
 *
 *
 * 🎯 WHY BINARY SEARCH?
 * -------------------------------------------------------------------------------------
 * Think in terms of TIME (t):
 *
 * At time t:
 *   - You can walk only on cells <= t
 *   - Check: Can we reach destination?
 *
 * This gives a monotonic property:
 *
 * ❌ If NOT reachable at t → NOT reachable at smaller t
 * ✅ If reachable at t → reachable at larger t
 *
 * 👉 This is PERFECT for Binary Search
 *
 *
 * 💡 HOW TO THINK IN INTERVIEW
 * -------------------------------------------------------------------------------------
 * 1. Question says "minimum time"
 * 2. Convert time → constraint (max allowed elevation)
 * 3. Ask: "Can I reach if allowed height = t?"
 * 4. That's a YES/NO question → BFS/DFS
 * 5. Monotonic → Binary Search
 *
 *
 * =====================================================================================
 * 🧩 APPROACH
 * =====================================================================================
 *
 * 1. Binary search on answer (time t)
 * 2. For each t:
 *      - Run BFS
 *      - Only move to cells where grid[i][j] <= t
 * 3. If reachable → try smaller t
 * 4. Else → increase t
 *
 *
 * =====================================================================================
 * ⏱ COMPLEXITY
 * -------------------------------------------------------------------------------------
 * Binary Search: log(n^2) = log(n)
 * BFS: O(n^2)
 *
 * 👉 Total: O(n^2 log n)
 * 👉 Space: O(n^2)
 *
 *
 * =====================================================================================
 * 🔍 DRY RUN (IMPORTANT FOR INTERVIEW)
 * -------------------------------------------------------------------------------------
 * grid =
 * [0, 2]
 * [1, 3]
 *
 * Start:
 * low = max(0,3) = 3
 * high = 3
 *
 * mid = 3
 *
 * BFS with t = 3:
 *   (0,0)=0 → allowed
 *   (1,0)=1 → allowed
 *   (0,1)=2 → allowed
 *   (1,1)=3 → allowed
 *
 * ✅ Reach destination → answer = 3
 *
 * =====================================================================================
 */
public class SwimInRisingWater_BinarySearch {

    static int n;
    static int[][] grid;

    // 4-directional movement
    static final int[][] DIRECTIONS = {
            {0, 1},   // right
            {1, 0},   // down
            {0, -1},  // left
            {-1, 0}   // up
    };

    public static void main(String[] args) {

        int[][] g = {
                {0, 2},
                {1, 3}
        };

        System.out.println(swimInWater(g)); // Expected: 3
    }

    /**
     * =================================================================================
     * 🔥 MAIN FUNCTION
     * =================================================================================
     */
    public static int swimInWater(int[][] input) {

        grid = input;
        n = grid.length;

        /**
         * 🧠 LOWER BOUND
         * ------------------------------------------------------------
         * We MUST at least wait until:
         * - starting cell is accessible
         * - ending cell is accessible
         *
         * So:
         * low = max(grid[0][0], grid[n-1][n-1])
         */
        int low = Math.max(grid[0][0], grid[n - 1][n - 1]);

        /**
         * 🧠 UPPER BOUND
         * ------------------------------------------------------------
         * Max possible elevation = n^2 - 1 (given constraint)
         */
        int high = n * n - 1;

        /**
         * 🎯 Explicit answer variable (your preferred pattern)
         */
        int answer = -1;

        /**
         * =================================================================================
         * 🔥 BINARY SEARCH TEMPLATE (FIRST TRUE)
         * =================================================================================
         *
         * Invariant:
         * - Left side → NOT possible
         * - Right side → possible
         *
         * Goal: find smallest t where reachable
         */
        while (low <= high) {

            int mid = low + (high - low) / 2;

            /**
             * 🔍 Check feasibility
             */
            if (canReach(mid)) {

                answer = mid;     // store candidate answer

                /**
                 * Try to find smaller time
                 */
                high = mid - 1;

            } else {

                /**
                 * Need more water → increase time
                 */
                low = mid + 1;
            }
        }

        return answer;
    }

    /**
     * =================================================================================
     * 🔍 FEASIBILITY CHECK (BFS)
     * =================================================================================
     *
     * Can we reach bottom-right if water level = t ?
     *
     * Rules:
     * - Move only if grid[i][j] <= t
     * - Standard BFS traversal
     */
    private static boolean canReach(int t) {

        /**
         * 🚫 If starting cell itself is not reachable
         */
        if (grid[0][0] > t) return false;

        boolean[][] visited = new boolean[n][n];

        /**
         * BFS Queue → stores {row, col}
         */
        Queue<int[]> queue = new LinkedList<>();

        queue.offer(new int[]{0, 0});
        visited[0][0] = true;

        while (!queue.isEmpty()) {

            int[] cell = queue.poll();
            int r = cell[0];
            int c = cell[1];

            /**
             * 🎯 Destination reached
             */
            if (r == n - 1 && c == n - 1) return true;

            /**
             * Explore all 4 directions
             */
            for (int[] dir : DIRECTIONS) {

                int nr = r + dir[0];
                int nc = c + dir[1];

                /**
                 * Boundary + visited + constraint check
                 */
                if (nr >= 0 && nc >= 0 && nr < n && nc < n
                        && !visited[nr][nc]
                        && grid[nr][nc] <= t) {

                    visited[nr][nc] = true;
                    queue.offer(new int[]{nr, nc});
                }
            }
        }

        /**
         * ❌ Could not reach destination
         */
        return false;
    }
}
