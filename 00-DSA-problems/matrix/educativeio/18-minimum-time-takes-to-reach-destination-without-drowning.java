/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an M x N grid representing a landscape.
 * - 'S' is the Source (starting position).
 * - 'D' is the Destination.
 * - '.' is an empty space.
 * - 'X' is a stone (impassable).
 * - '*' is a flooded space.
 * 
 * Every second, the flood expands 4-directionally to adjacent empty spaces. 
 * Simultaneously, you can move 1 step 4-directionally to an adjacent empty space.
 * You cannot step on a stone ('X'), an already flooded cell, or a cell that 
 * gets flooded at the exact same second you arrive.
 * The destination ('D') is immune to the flood and will never be flooded.
 * 
 * We need to find the minimum time (in seconds) to reach 'D' from 'S'. 
 * If it's impossible, return -1.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can there be multiple flooded cells ('*') initially?
 *    (Yes, the problem says "These cells are flooded", implying multiple. We 
 *     must handle multiple water sources expanding simultaneously).
 * 2. Can the flood spread THROUGH the destination cell 'D'?
 *    (The problem states 'D' will never be flooded. Usually, this means water 
 *     is blocked by it or ignores it. We will treat 'D' as a wall for the flood).
 * 3. What if the source 'S' gets flooded before I can leave?
 *    (If 'S' gets flooded at t=1, you must move to a safe adjacent cell at t=1. 
 *     You start at t=0, so you are initially safe).
 * 4. Are we guaranteed to have exactly one 'S' and one 'D'?
 *    (Yes, the constraints guarantee this).
 * 5. Can we move diagonally?
 *    (No, standard grid movement implies 4-directional: up, down, left, right).
 * 
 * ============================================================================
 * EDGE CASES & EXAMPLES TO THINK OF
 * ============================================================================
 * 1. Trapped by flood immediately: 
 *    ["S", "*", "D"] -> Output: -1. At t=1, you move to index 1, but it's 
 *    already flooded (or getting flooded). You drown.
 * 2. Trapped by stones:
 *    ["S", "X", "D"] -> Output: -1. No path exists.
 * 3. Race against the flood:
 *    ["D", ".", "*"]
 *    [".", ".", "."]
 *    ["S", ".", "."]
 *    You have to move quickly to avoid the expanding water from the top-right.
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Single BFS (Brute Force Simulation / Error Prone):
 *    - Push both the flood and the player into the same BFS queue. 
 *    - Process all flood expansions for a given second, then process player moves.
 *    - This is valid but highly prone to edge-case bugs (e.g., distinguishing 
 *      flood nodes from player nodes, ordering of execution).
 * 
 * 2. Two Independent BFS Passes (The Most Optimal & Clean Approach):
 *    - Step 1: Multi-Source BFS for the Flood.
 *      Find ALL '*' cells and push them into a queue. Run a BFS to populate a 
 *      `floodTime[m][n]` matrix. This matrix tells us exactly what second each 
 *      cell becomes flooded. Cells that never flood stay at `Integer.MAX_VALUE`.
 * 
 *    - Step 2: Single-Source BFS for the Player.
 *      Start a BFS from 'S' at time `t = 0`. 
 *      To step on an adjacent cell at time `t + 1`, we must check:
 *      a) Is it within bounds and not 'X'?
 *      b) Is `t + 1 < floodTime[next_row][next_col]`? (Strictly less, because 
 *         if they are equal, you drown at the exact moment you arrive).
 *      c) Has it not been visited by the player yet?
 *      If we reach 'D', we instantly return the time `t`.
 * 
 *    - Time Complexity: O(M * N). We visit each cell at most once in the flood 
 *      BFS and at most once in the player BFS.
 *    - Space Complexity: O(M * N) for the queues, visited arrays, and flood matrix.
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Two BFS Approach)
 * ============================================================================
 * Grid: 
 * D . *
 * . . .
 * S . .
 * 
 * Phase 1: Flood BFS
 * Initial: (0,2) is *. 
 * t=0: floodTime(0,2) = 0
 * t=1: floodTime(0,1) = 1, floodTime(1,2) = 1
 * t=2: floodTime(1,1) = 2, floodTime(2,2) = 2
 * t=3: floodTime(2,1) = 3, floodTime(1,0) = 3
 * t=4: floodTime(2,0) = 4 (S)
 * 'D' is skipped, so floodTime(0,0) = MAX_VALUE.
 * 
 * Phase 2: Player BFS (Starts at S=(2,0) at t=0)
 * t=0: Queue = [(2,0, t=0)]
 * t=1: From S, can go Right (2,1) or Up (1,0). 
 *      Time at (2,1) is 1. floodTime(2,1) is 3. (1 < 3) -> Safe! Push (2,1, 1).
 *      Time at (1,0) is 1. floodTime(1,0) is 3. (1 < 3) -> Safe! Push (1,0, 1).
 * t=2: From (1,0), can go Up to (0,0) which is 'D'.
 *      Return 2.
 */

import java.util.*;

public class EscapeTheFlood {

    // Record is a modern Java feature (Java 14+) that automatically creates 
    // immutable data carriers with getters, equals, hashCode, and toString.
    // Extremely clean for BFS coordinate tracking!
    record Cell(int r, int c) {}
    record State(int r, int c, int time) {}

    // 4-directional movement vectors: Up, Down, Left, Right
    private static final int[] dr = {-1, 1, 0, 0};
    private static final int[] dc = {0, 0, -1, 1};

    public static void main(String[] args) {
        String[] grid1Str = {
            "D.*",
            "...",
            "S.."
        };
        
        String[] grid2Str = {
            "D.X",
            "X.X",
            "S.*"
        };
        
        String[] grid3Str = {
            "S.*",
            "X.X",
            "D.."
        };

        System.out.println("--- Test Case 1: Standard Race ---");
        System.out.println("Output: " + minTimeToReachDest(convertGrid(grid1Str))); // Expected: 2

        System.out.println("\n--- Test Case 2: Trapped by Flood ---");
        System.out.println("Output: " + minTimeToReachDest(convertGrid(grid2Str))); // Expected: -1

        System.out.println("\n--- Test Case 3: Forced to run around ---");
        System.out.println("Output: " + minTimeToReachDest(convertGrid(grid3Str))); // Expected: 4
    }

    /**
     * SOLUTION: Two Independent BFS Passes
     * 
     * Idea: Pre-calculate the danger (flood time) for every cell using a Multi-Source BFS. 
     * Then, use a standard BFS to navigate the player, validating every step against 
     * the pre-calculated flood times.
     * 
     * Time Complexity: O(M * N)
     * Space Complexity: O(M * N)
     */
    public static int minTimeToReachDest(char[][] land) {
        if (land == null || land.length == 0) return -1;

        int m = land.length;
        int n = land[0].length;

        Cell startNode = null;
        Cell destNode = null;

        // Queue for the multi-source flood BFS
        Queue<Cell> floodQueue = new LinkedList<>();
        
        // Matrix to store the earliest time water reaches each cell
        int[][] floodTime = new int[m][n];
        
        // Initialize all flood times to infinity
        for (int[] row : floodTime) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }

        // 1. Scan the grid to find S, D, and all initial flood sources '*'
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (land[i][j] == 'S') {
                    startNode = new Cell(i, j);
                } else if (land[i][j] == 'D') {
                    destNode = new Cell(i, j);
                } else if (land[i][j] == '*') {
                    floodQueue.offer(new Cell(i, j));
                    floodTime[i][j] = 0; // Water is here at t = 0
                }
            }
        }

        // 2. PHASE 1: Multi-Source BFS to compute flood expansion times
        while (!floodQueue.isEmpty()) {
            Cell curr = floodQueue.poll();

            for (int i = 0; i < 4; i++) {
                int nr = curr.r() + dr[i];
                int nc = curr.c() + dc[i];

                // Water can expand if it's within bounds, not a stone ('X'), 
                // and NOT the destination ('D').
                // It also must be a cell we haven't flooded yet (or can flood sooner).
                if (isValid(nr, nc, m, n) && land[nr][nc] != 'X' && land[nr][nc] != 'D') {
                    if (floodTime[curr.r()][curr.c()] + 1 < floodTime[nr][nc]) {
                        floodTime[nr][nc] = floodTime[curr.r()][curr.c()] + 1;
                        floodQueue.offer(new Cell(nr, nc));
                    }
                }
            }
        }

        // 3. PHASE 2: Single-Source BFS for the Player
        Queue<State> playerQueue = new LinkedList<>();
        boolean[][] playerVisited = new boolean[m][n];
        
        // Start the player at 'S' at time 0
        playerQueue.offer(new State(startNode.r(), startNode.c(), 0));
        playerVisited[startNode.r()][startNode.c()] = true;

        while (!playerQueue.isEmpty()) {
            State curr = playerQueue.poll();
            
            // If we've reached the Destination, return the time taken!
            if (curr.r() == destNode.r() && curr.c() == destNode.c()) {
                return curr.time();
            }

            for (int i = 0; i < 4; i++) {
                int nr = curr.r() + dr[i];
                int nc = curr.c() + dc[i];
                int nextTime = curr.time() + 1;

                // Validate the move:
                // a) Must be within grid and not a stone
                // b) Must not be previously visited by the player
                // c) Player's arrival time (nextTime) MUST BE strictly less than 
                //    the time the flood reaches this cell.
                if (isValid(nr, nc, m, n) && land[nr][nc] != 'X' && !playerVisited[nr][nc]) {
                    if (nextTime < floodTime[nr][nc]) {
                        playerVisited[nr][nc] = true;
                        playerQueue.offer(new State(nr, nc, nextTime));
                    }
                }
            }
        }

        // Queue exhausted and 'D' was not reached
        return -1;
    }

    /**
     * Helper method to verify boundaries.
     * Extracts repetitive index checking for cleaner code.
     */
    private static boolean isValid(int r, int c, int m, int n) {
        return r >= 0 && r < m && c >= 0 && c < n;
    }

    /**
     * Helper method to convert an array of Strings into a 2D char array.
     */
    private static char[][] convertGrid(String[] gridStr) {
        char[][] grid = new char[gridStr.length][gridStr[0].length()];
        for (int i = 0; i < gridStr.length; i++) {
            grid[i] = gridStr[i].toCharArray();
        }
        return grid;
    }

    /**
     * ============================================================================
     * FOLLOW-UPS TO PREPARE FOR
     * ============================================================================
     * 1. What if 'D' is replaced by multiple safehouses, and you just need to 
     *    reach ANY of them?
     *    Answer: The logic remains almost exactly the same. When the player BFS 
     *    pops a cell, we just check `if (land[r][c] == 'D')`. Since BFS guarantees 
     *    the shortest path is found first, the first 'D' we pop is the closest one 
     *    we can reach safely.
     * 
     * 2. What if water recedes after a certain amount of time?
     *    Answer: The `floodTime` matrix would need to be replaced with a 3D boolean 
     *    visited array for the player `visited[r][c][time]`, because waiting in place 
     *    or looping back to a previously flooded cell (once the water recedes) 
     *    becomes a valid strategy. This drastically changes the problem into a 
     *    more complex state-space search.
     * 
     * 3. What if you could step on 'X' (stones) but it took 2 seconds instead of 1?
     *    Answer: A standard Queue (BFS) would no longer work because the edge weights 
     *    are uneven (1 for empty, 2 for stone). We would have to swap the `Queue` 
     *    for a `PriorityQueue` (Min-Heap) sorted by elapsed time, essentially 
     *    upgrading our BFS to Dijkstra's Algorithm.
     */
}



/**
 * ============================================================
 * 🚀 ESCAPE FLOOD — DETAILED INTERVIEW VERSION
 * ============================================================
 *
 * PROBLEM SUMMARY:
 * ----------------
 * - You start from 'S' and want to reach 'D'
 * - Each second:
 *      1. YOU move
 *      2. FLOOD spreads
 *
 * CONSTRAINT:
 * ----------
 * You CANNOT enter a cell if:
 *  - It is stone ('X')
 *  - It is already flooded
 *  - It gets flooded AT THE SAME TIME you arrive
 *
 * 🔥 KEY INSIGHT:
 * ------------------------------------------------
 * This is a "SIMULTANEOUS EVENTS" problem:
 * - Flood spreads globally
 * - You move locally
 *
 * 👉 So we MUST precompute:
 *      "When does flood reach each cell?"
 *
 * Then ensure:
 *      person_arrival_time < flood_arrival_time
 *
 * ------------------------------------------------
 * APPROACH:
 * ------------------------------------------------
 * STEP 1: Multi-source BFS → Flood timing
 * STEP 2: BFS from 'S' → Safe shortest path
 *
 * ------------------------------------------------
 * WHY BFS?
 * ------------------------------------------------
 * - Grid → Unweighted graph
 * - Shortest path → BFS guarantees minimum time
 *
 * ------------------------------------------------
 * TIME COMPLEXITY:
 * O(m * n)
 *
 * SPACE COMPLEXITY:
 * O(m * n)
 */
public class Solution {

    private static final int INF = (int) 1e9;

    // 4-directional movement
    private static final int[][] DIRS = {
        {0, 1},   // Right
        {1, 0},   // Down
        {0, -1},  // Left
        {-1, 0}   // Up
    };

    public static int minimumSeconds(List<List<String>> land) {

        int rows = land.size();
        int cols = land.get(0).size();

        /**
         * floodTime[r][c] =
         *      earliest second when flood reaches this cell
         *
         * Initially:
         *      INF → means flood never reaches here
         */
        int[][] floodTime = new int[rows][cols];

        for (int[] row : floodTime) {
            Arrays.fill(row, INF);
        }

        /**
         * Multi-source BFS queue:
         * All '*' cells start flooding at time = 0
         */
        Queue<int[]> floodQueue = new ArrayDeque<>();

        int sr = -1, sc = -1; // Source position

        // ------------------------------------------------
        // STEP 0: Initialize flood sources and find 'S'
        // ------------------------------------------------
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                String cell = land.get(r).get(c);

                if (cell.equals("*")) {
                    floodTime[r][c] = 0; // Flood starts here
                    floodQueue.offer(new int[]{r, c});
                }

                if (cell.equals("S")) {
                    sr = r;
                    sc = c;
                }
            }
        }

        // ============================================================
        // 🔥 STEP 1: MULTI-SOURCE BFS → Compute Flood Spread Timing
        // ============================================================
        //
        // Think:
        // "Water spreads layer by layer (like wave expansion)"
        //
        // INVARIANT:
        // When processing a cell:
        //      floodTime[r][c] is already minimum possible
        //
        while (!floodQueue.isEmpty()) {

            int[] curr = floodQueue.poll();
            int r = curr[0];
            int c = curr[1];

            for (int[] dir : DIRS) {

                int nr = r + dir[0];
                int nc = c + dir[1];

                // Boundary check
                if (!isValid(nr, nc, rows, cols)) continue;

                // Flood CANNOT pass through stones
                if (land.get(nr).get(nc).equals("X")) continue;

                /**
                 * Relaxation condition:
                 * If this neighbor hasn't been flooded yet,
                 * assign earliest possible time.
                 */
                if (floodTime[nr][nc] == INF) {
                    floodTime[nr][nc] = floodTime[r][c] + 1;
                    floodQueue.offer(new int[]{nr, nc});
                }
            }
        }

        // ============================================================
        // 🚶 STEP 2: BFS FROM SOURCE → Find Safe Path
        // ============================================================

        boolean[][] visited = new boolean[rows][cols];

        /**
         * Queue stores:
         * [row, col, currentTime]
         */
        Queue<int[]> personQueue = new ArrayDeque<>();

        personQueue.offer(new int[]{sr, sc, 0});
        visited[sr][sc] = true;

        /**
         * INVARIANT:
         * - We only push SAFE states into queue
         * - First time reaching a cell = shortest time
         */
        while (!personQueue.isEmpty()) {

            int[] curr = personQueue.poll();

            int r = curr[0];
            int c = curr[1];
            int time = curr[2];

            // 🎯 If we reached destination → answer found
            if (land.get(r).get(c).equals("D")) {
                return time;
            }

            for (int[] dir : DIRS) {

                int nr = r + dir[0];
                int nc = c + dir[1];

                if (!isValid(nr, nc, rows, cols)) continue;
                if (visited[nr][nc]) continue;
                if (land.get(nr).get(nc).equals("X")) continue;

                int nextTime = time + 1;

                /**
                 * 🔥 CRITICAL SAFETY CHECK:
                 *
                 * We CANNOT enter if:
                 *      flood arrives BEFORE or AT SAME TIME
                 *
                 * Allowed only if:
                 *      nextTime < floodTime[nr][nc]
                 *
                 * ❗ IMPORTANT:
                 * Destination 'D' is NEVER flooded (given)
                 * so we don't need this check for 'D'
                 */
                if (!land.get(nr).get(nc).equals("D")
                        && floodTime[nr][nc] <= nextTime) {
                    continue;
                }

                visited[nr][nc] = true;
                personQueue.offer(new int[]{nr, nc, nextTime});
            }
        }

        // 🚫 No path exists
        return -1;
    }

    // Utility function
    private static boolean isValid(int r, int c, int rows, int cols) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }
}
