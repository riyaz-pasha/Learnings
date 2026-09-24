/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an m x n grid where '1' represents a friend's house and '0' is 
 * an empty space.
 * We need to find a "meeting point" (which can be any cell, including a 
 * friend's house) that minimizes the total travel distance for all friends.
 * The distance is measured using Manhattan Distance: |x2 - x1| + |y2 - y1|.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can the meeting point be on a cell where a friend already lives?
 *    (Yes, the meeting point can be any cell in the grid, including a '1').
 * 2. Is there always at least one friend?
 *    (The constraints guarantee there will be at least two friends).
 * 3. Does the grid contain any obstacles?
 *    (No, all 0s and 1s are passable, which is why we can use pure Manhattan 
 *     distance. If there were obstacles like walls, we'd have to use BFS 
 *     from every friend, changing the problem entirely).
 * 4. Are we optimizing for the worst-case travel time of a single friend, or 
 *    the SUM of all travel times?
 *    (The problem asks for the SUM of all travel distances to be minimized).
 * 
 * ============================================================================
 * EDGE CASES & EXAMPLES TO THINK OF
 * ============================================================================
 * 1. Two Friends: [[1, 0, 0, 1]]. 
 *    Any point between them (inclusive) yields the exact same total distance! 
 *    If meeting at index 0, distances are 0 + 3 = 3. If index 1, 1 + 2 = 3.
 * 2. All Friends in One Row/Col: 
 *    The meeting point will naturally fall exactly on that row/col.
 * 3. Three Friends (Odd number): 
 *    [[1, 0, 1, 0, 1]]. The meeting point MUST be the exact middle friend 
 *    to minimize distance.
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Brute Force (O(M^2 * N^2)):
 *    - Find the coordinates of all friends.
 *    - Iterate through every single cell in the m x n grid.
 *    - For each cell, calculate the Manhattan distance to all friends.
 *    - Keep a running minimum.
 *    - Time: O(M * N * K) where K is the number of friends. Worst case K = M*N, 
 *      so O(M^2 * N^2). Space: O(K).
 * 
 * 2. Median / 1D Separation (The Optimal "Aha!" Approach):
 *    - Manhattan distance allows us to completely decouple the X (row) and 
 *      Y (column) coordinates!
 *    - Total Distance = Sum of Row Distances + Sum of Col Distances.
 *    - In 1D, the point that minimizes the sum of absolute differences to a 
 *      set of points is always the **Median**.
 *    - Step 1: Collect all row coordinates of friends. If we iterate row by row, 
 *      this list is naturally sorted.
 *    - Step 2: Collect all col coordinates. If we iterate col by col, this 
 *      list is also naturally sorted!
 *    - Step 3: We don't even need to find the exact median index. For a sorted 
 *      list of points, the minimum distance to the median is simply pairing 
 *      the outermost points inward: `(points[right] - points[left])`.
 *    - Time: O(M * N) to scan the grid. Space: O(K) where K is number of friends.
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Median Approach)
 * ============================================================================
 * Grid:
 * [1, 0, 0, 0, 1]
 * [0, 0, 0, 0, 0]
 * [0, 0, 1, 0, 0]
 * 
 * Friends at: (0,0), (0,4), (2,2)
 * 
 * Extract Rows (Iterate row by row):
 * Rows = [0, 0, 2]
 * Two Pointers: left=0, right=2
 * Dist = Rows[2] - Rows[0] = 2 - 0 = 2.
 * Total Row Distance = 2.
 * 
 * Extract Cols (Iterate col by col):
 * Cols = [0, 2, 4]
 * Two Pointers: left=0, right=2
 * Dist = Cols[2] - Cols[0] = 4 - 0 = 4.
 * Total Col Distance = 4.
 * 
 * Total Min Distance = 2 + 4 = 6.
 * (The median point is row 0, col 2. Distances: (0,0)->2, (0,4)->2, (2,2)->2. Sum = 6).
 */

import java.util.*;

public class BestMeetingPoint {

    public static void main(String[] args) {
        int[][] grid1 = {
            {1, 0, 0, 0, 1},
            {0, 0, 0, 0, 0},
            {0, 0, 1, 0, 0}
        };

        int[][] grid2 = {
            {1, 1}
        };

        System.out.println("--- Test Case 1 ---");
        System.out.println("Output: " + minTotalDistance(grid1)); // Expected: 6

        System.out.println("\n--- Test Case 2 ---");
        System.out.println("Output: " + minTotalDistance(grid2)); // Expected: 1
    }

    /**
     * SOLUTION: 1D Median Separation (Optimal)
     * 
     * Idea: Decouple rows and columns. Collect sorted rows and sorted columns. 
     * Use a two-pointer approach to calculate the minimum 1D distance sums 
     * independently, then add them together.
     * 
     * Time Complexity: O(M * N) - We traverse the grid twice (once for rows, once for cols).
     * Space Complexity: O(K) - Where K is the number of friends, to store the coordinates.
     */
    public static int minTotalDistance(int[][] grid) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) return 0;
        
        int m = grid.length;
        int n = grid[0].length;
        
        List<Integer> rows = new ArrayList<>();
        List<Integer> cols = new ArrayList<>();
        
        // 1. Collect Row Coordinates (Sorted naturally by iterating rows first)
        for (int r = 0; r < m; r++) {
            for (int c = 0; c < n; c++) {
                if (grid[r][c] == 1) {
                    rows.add(r);
                }
            }
        }
        
        // 2. Collect Column Coordinates (Sorted naturally by iterating cols first)
        for (int c = 0; c < n; c++) {
            for (int r = 0; r < m; r++) {
                if (grid[r][c] == 1) {
                    cols.add(c);
                }
            }
        }
        
        // 3. Calculate and sum the minimum 1D distances for both axes
        return getMin1DDistance(rows) + getMin1DDistance(cols);
    }
    
    /**
     * Helper method to calculate the minimum total distance to the median point 
     * in a 1D sorted list of coordinates.
     * 
     * Using two pointers, the distance from an outer pair of points to ANY point 
     * between them is exactly `right_point - left_point`. 
     * By moving inwards, we calculate the sum to the exact median without needing 
     * to explicitly find the median itself.
     */
    private static int getMin1DDistance(List<Integer> points) {
        int distance = 0;
        int left = 0;
        int right = points.size() - 1;
        
        while (left < right) {
            // Add the distance spanning the two outermost unprocessed friends
            distance += points.get(right) - points.get(left);
            left++;
            right--;
        }
        
        return distance;
    }

    /**
     * ============================================================================
     * FOLLOW-UPS TO PREPARE FOR
     * ============================================================================
     * 1. What if the distance was Euclidean (straight line) instead of Manhattan?
     *    Answer: The median property no longer holds for Euclidean distance. 
     *    Minimizing the sum of Euclidean distances is known as the "Geometric Median" 
     *    or "Fermat-Weber point". It has no explicit algebraic formula and must 
     *    be approximated using numerical methods like Weiszfeld's algorithm or 
     *    Gradient Descent.
     * 
     * 2. What if the grid contains obstacles (e.g., walls you cannot pass through)?
     *    Answer: Manhattan distance is no longer valid because a wall might force 
     *    a detour. You would need to run a Multi-Source BFS. From each friend's 
     *    location, run a BFS to find the shortest path to all empty cells. Keep a 
     *    running sum of distances for every cell in a separate 2D `distanceSum` array. 
     *    Finally, find the minimum value in `distanceSum`. 
     *    Time complexity becomes O(K * M * N).
     * 
     * 3. What if multiple friends can live in the same cell (weighted homes)?
     *    Answer: The current algorithm handles this perfectly! You would just 
     *    add the coordinate to the `rows` and `cols` lists multiple times based 
     *    on the number of friends there. The two-pointer median logic inherently 
     *    balances the weights correctly.
     */
}

/**
 * ============================================================
 * 🏠 BEST MEETING POINT (Manhattan Distance Optimization)
 * ============================================================
 *
 * Problem:
 * Given a grid with 0s and 1s, find a meeting point such that
 * the total Manhattan distance from all 1s is minimized.
 *
 * Manhattan Distance:
 * |x1 - x2| + |y1 - y2|
 *
 * ============================================================
 * 🧠 CORE OBSERVATION (VERY IMPORTANT)
 * ============================================================
 *
 * Manhattan distance can be separated into 2 independent parts:
 *
 *      |x - xi| + |y - yi|
 *
 * So instead of solving in 2D, we solve TWO 1D problems:
 *
 *      1. Minimize sum of |row - ri|
 *      2. Minimize sum of |col - ci|
 *
 * 👉 KEY RESULT:
 * The value that minimizes sum of absolute differences is MEDIAN.
 *
 * So:
 *      optimalRow = median of all row indices
 *      optimalCol = median of all col indices
 *
 * ============================================================
 * 🧠 INTERVIEW INTUITION
 * ============================================================
 *
 * If interviewer asks "how did you think?"
 *
 * 1. Manhattan distance → separable
 * 2. Absolute difference sum → think MEDIAN
 * 3. Solve rows + cols independently
 *
 * ============================================================
 * ⏱ TIME & SPACE
 * ============================================================
 *
 * Time  : O(m * n)
 * Space : O(k)  (k = number of friends)
 *
 * ============================================================
 */
public class BestMeetingPoint {

    public static int minTotalDistance(int[][] grid) {

        int m = grid.length;
        int n = grid[0].length;

        // ----------------------------------------------------
        // Step 1: Collect all row indices where grid[i][j] == 1
        // ----------------------------------------------------
        // IMPORTANT:
        // We traverse row-wise → rows list will be SORTED already
        List<Integer> rows = new ArrayList<>();

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {

                if (grid[i][j] == 1) {
                    rows.add(i);   // collect row index
                }
            }
        }

        // ----------------------------------------------------
        // Step 2: Collect all column indices
        // ----------------------------------------------------
        // IMPORTANT:
        // Traverse column-wise → cols list will be SORTED already
        List<Integer> cols = new ArrayList<>();

        for (int j = 0; j < n; j++) {
            for (int i = 0; i < m; i++) {

                if (grid[i][j] == 1) {
                    cols.add(j);   // collect column index
                }
            }
        }

        // ----------------------------------------------------
        // Step 3: Find MEDIAN
        // ----------------------------------------------------
        // Why median?
        // Because median minimizes sum of |x - xi|
        int rowMedian = rows.get(rows.size() / 2);
        int colMedian = cols.get(cols.size() / 2);

        // ----------------------------------------------------
        // Step 4: Compute total Manhattan distance
        // ----------------------------------------------------
        int totalDistance = 0;

        // Distance in row direction
        for (int r : rows) {
            totalDistance += Math.abs(r - rowMedian);
        }

        // Distance in column direction
        for (int c : cols) {
            totalDistance += Math.abs(c - colMedian);
        }

        return totalDistance;
    }

    // --------------------------------------------------------
    // 🔥 Dry Run Example
    // --------------------------------------------------------
    public static void main(String[] args) {

        int[][] grid = {
                {1, 0, 0, 0, 1},
                {0, 0, 0, 0, 0},
                {0, 0, 1, 0, 0}
        };

        /*
         * Friends at:
         * (0,0), (0,4), (2,2)
         *
         * rows = [0, 0, 2]
         * cols = [0, 2, 4]
         *
         * median row = 0
         * median col = 2
         *
         * Meeting point = (0,2)
         *
         * Distances:
         * (0,0) → 2
         * (0,4) → 2
         * (2,2) → 2
         *
         * Total = 6
         */

        System.out.println(minTotalDistance(grid)); // Output: 6
    }
}

