import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class GridUniquePathsMemo {

    /*
     * Time Complexity: O(M*N)
     * Reason: At max, there will be M*N calls of recursion.
     * 
     * Space Complexity: O((N-1)+(M-1)) + O(M*N)
     * Reason: We are using a recursion stack space: O((N-1)+(M-1)), here
     * (N-1)+(M-1) is the path length and an external DP Array of size ‘M*N’.
     */
    public int uniquePaths(int m, int n) {
        Integer[][] memo = new Integer[m][n];
        return this.uniquePaths(memo, m - 1, n - 1);
    }

    private int uniquePaths(Integer[][] memo, int m, int n) {
        if (m < 0 || n < 0) {
            return 0;
        }
        if (m == 0 && n == 0) {
            return 1;
        }
        if (memo[m][n] != null) {
            return memo[m][n];
        }
        return memo[m][n] = this.uniquePaths(memo, m - 1, n) + this.uniquePaths(memo, m, n - 1);
    }

}

class GridUniquePathsDp {

    /*
     * Time Complexity: O(M*N)
     * Reason: There are two nested loops
     * 
     * Space Complexity: O(M*N)
     * Reason: We are using an external array of size ‘M*N’’.
     */
    public int uniquePaths(int rows, int cols) {
        int[][] dp = new int[rows][cols];

        for (int row = 0; row < rows; row++) {
            dp[row][0] = 1;
        }

        for (int col = 0; col < cols; col++) {
            dp[0][col] = 1;
        }

        for (int row = 1; row < rows; row++) {
            for (int col = 1; col < cols; col++) {
                dp[row][col] = dp[row - 1][col] + dp[row][col - 1];
            }
        }

        return dp[rows - 1][cols - 1];
    }

}

class UniquePaths {

    // Solution 1: 2D Dynamic Programming (Most Intuitive)
    public static int uniquePaths2D(int m, int n) {
        if (m <= 0 || n <= 0)
            return 0;
        if (m == 1 || n == 1)
            return 1;

        // dp[i][j] = number of unique paths to reach cell (i,j)
        int[][] dp = new int[m][n];

        // Initialize first row and first column
        // There's only one way to reach any cell in first row (keep going right)
        for (int j = 0; j < n; j++) {
            dp[0][j] = 1;
        }

        // There's only one way to reach any cell in first column (keep going down)
        for (int i = 0; i < m; i++) {
            dp[i][0] = 1;
        }

        // Fill the DP table
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                // Paths to (i,j) = paths to (i-1,j) + paths to (i,j-1)
                dp[i][j] = dp[i - 1][j] + dp[i][j - 1];
            }
        }

        return dp[m - 1][n - 1];
    }

    // Solution 2: Space Optimized DP - O(min(m,n)) space
    public static int uniquePathsOptimized(int m, int n) {
        if (m <= 0 || n <= 0)
            return 0;
        if (m == 1 || n == 1)
            return 1;

        // Use smaller dimension for space optimization
        if (m > n) {
            return uniquePathsOptimized(n, m);
        }

        // Only need previous row to calculate current row
        int[] dp = new int[m];
        Arrays.fill(dp, 1); // First column all 1s

        for (int j = 1; j < n; j++) {
            for (int i = 1; i < m; i++) {
                dp[i] += dp[i - 1]; // dp[i] = dp[i] + dp[i-1]
            }
        }

        return dp[m - 1];
    }

    // Solution 3: Mathematical Approach - Combinatorics
    public static int uniquePathsMath(int m, int n) {
        if (m <= 0 || n <= 0)
            return 0;
        if (m == 1 || n == 1)
            return 1;

        // Total moves needed: (m-1) down + (n-1) right = (m+n-2) moves
        // We need to choose (m-1) positions for down moves out of (m+n-2) total moves
        // This is C(m+n-2, m-1) = (m+n-2)! / ((m-1)! * (n-1)!)

        long result = 1;
        int totalMoves = m + n - 2;
        int downMoves = m - 1;

        // Calculate C(totalMoves, downMoves) efficiently
        // C(n,k) = C(n,n-k), so choose smaller k
        if (downMoves > totalMoves - downMoves) {
            downMoves = totalMoves - downMoves;
        }

        for (int i = 0; i < downMoves; i++) {
            result = result * (totalMoves - i) / (i + 1);
        }

        return (int) result;
    }

    // Solution 4: Recursive with Memoization
    public static int uniquePathsRecursive(int m, int n) {
        if (m <= 0 || n <= 0)
            return 0;

        Integer[][] memo = new Integer[m][n];
        return helper(0, 0, m, n, memo);
    }

    private static int helper(int i, int j, int m, int n, Integer[][] memo) {
        // Base cases
        if (i == m - 1 && j == n - 1)
            return 1; // Reached destination
        if (i >= m || j >= n)
            return 0; // Out of bounds

        if (memo[i][j] != null)
            return memo[i][j];

        // Move down + Move right
        int paths = helper(i + 1, j, m, n, memo) + helper(i, j + 1, m, n, memo);
        memo[i][j] = paths;

        return paths;
    }

    // Solution 5: Bottom-up recursive (alternative approach)
    public static int uniquePathsBottomUp(int m, int n) {
        if (m <= 0 || n <= 0)
            return 0;

        Integer[][] memo = new Integer[m][n];
        return helperBottomUp(m - 1, n - 1, memo);
    }

    private static int helperBottomUp(int i, int j, Integer[][] memo) {
        // Base cases
        if (i == 0 || j == 0)
            return 1; // First row or first column
        if (i < 0 || j < 0)
            return 0; // Out of bounds

        if (memo[i][j] != null)
            return memo[i][j];

        // Come from top + Come from left
        int paths = helperBottomUp(i - 1, j, memo) + helperBottomUp(i, j - 1, memo);
        memo[i][j] = paths;

        return paths;
    }

    // Method to print one possible path (for demonstration)
    public static List<String> getOnePath(int m, int n) {
        List<String> path = new ArrayList<>();

        // One simple path: go all the way down, then all the way right
        for (int i = 0; i < m - 1; i++) {
            path.add("Down");
        }
        for (int j = 0; j < n - 1; j++) {
            path.add("Right");
        }

        return path;
    }

    // Method to visualize the DP table (for small grids)
    public static void printDPTable(int m, int n) {
        if (m > 10 || n > 10) {
            System.out.println("Grid too large to visualize");
            return;
        }

        int[][] dp = new int[m][n];

        // Fill first row and column
        for (int j = 0; j < n; j++)
            dp[0][j] = 1;
        for (int i = 0; i < m; i++)
            dp[i][0] = 1;

        // Fill the rest
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                dp[i][j] = dp[i - 1][j] + dp[i][j - 1];
            }
        }

        System.out.println("DP Table (paths to each cell):");
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                System.out.printf("%4d ", dp[i][j]);
            }
            System.out.println();
        }
    }

    // Method to calculate all paths with obstacles (bonus)
    public static int uniquePathsWithObstacles(int[][] obstacleGrid) {
        if (obstacleGrid == null || obstacleGrid.length == 0 ||
                obstacleGrid[0].length == 0 || obstacleGrid[0][0] == 1) {
            return 0;
        }

        int m = obstacleGrid.length;
        int n = obstacleGrid[0].length;
        int[][] dp = new int[m][n];

        // Initialize first cell
        dp[0][0] = 1;

        // Fill first row
        for (int j = 1; j < n; j++) {
            dp[0][j] = (obstacleGrid[0][j] == 1) ? 0 : dp[0][j - 1];
        }

        // Fill first column
        for (int i = 1; i < m; i++) {
            dp[i][0] = (obstacleGrid[i][0] == 1) ? 0 : dp[i - 1][0];
        }

        // Fill the rest
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                if (obstacleGrid[i][j] == 1) {
                    dp[i][j] = 0;
                } else {
                    dp[i][j] = dp[i - 1][j] + dp[i][j - 1];
                }
            }
        }

        return dp[m - 1][n - 1];
    }

    public static void main(String[] args) {
        // Test cases
        int[][] testCases = {
                { 3, 7 }, // Expected: 28
                { 3, 2 }, // Expected: 3
                { 7, 3 }, // Expected: 28
                { 1, 1 }, // Expected: 1
                { 1, 10 }, // Expected: 1
                { 10, 1 }, // Expected: 1
                { 3, 3 }, // Expected: 6
                { 4, 4 } // Expected: 20
        };

        System.out.println("Unique Paths in Grid - Java Solutions:");
        System.out.println("=".repeat(50));

        for (int i = 0; i < testCases.length; i++) {
            int m = testCases[i][0];
            int n = testCases[i][1];

            System.out.println("Test Case " + (i + 1) + ": Grid " + m + "x" + n);

            int result1 = uniquePaths2D(m, n);
            int result2 = uniquePathsOptimized(m, n);
            int result3 = uniquePathsMath(m, n);
            int result4 = uniquePathsRecursive(m, n);
            int result5 = uniquePathsBottomUp(m, n);

            System.out.println("2D DP Solution: " + result1);
            System.out.println("Optimized DP: " + result2);
            System.out.println("Mathematical: " + result3);
            System.out.println("Recursive (Top-Down): " + result4);
            System.out.println("Recursive (Bottom-Up): " + result5);

            // Verify all solutions match
            if (result1 == result2 && result2 == result3 &&
                    result3 == result4 && result4 == result5) {
                System.out.println("✓ All solutions match!");
            } else {
                System.out.println("✗ Solutions don't match!");
            }

            if (m <= 5 && n <= 5) {
                printDPTable(m, n);
            }

            System.out.println("-".repeat(30));
        }

        // Edge cases
        System.out.println("\nEdge Cases:");
        System.out.println("Grid 0x5: " + uniquePathsOptimized(0, 5));
        System.out.println("Grid 5x0: " + uniquePathsOptimized(5, 0));

        // Example with obstacles
        System.out.println("\nBonus: Unique Paths with Obstacles:");
        int[][] obstacleGrid = {
                { 0, 0, 0 },
                { 0, 1, 0 },
                { 0, 0, 0 }
        };
        System.out.println("Grid with obstacle at (1,1): " +
                uniquePathsWithObstacles(obstacleGrid));

        // Performance comparison
        System.out.println("\nPerformance Comparison (Grid 20x20):");
        int m = 20, n = 20;

        long start = System.nanoTime();
        int result = uniquePathsOptimized(m, n);
        long end = System.nanoTime();
        System.out.println("Optimized DP: " + result +
                " (Time: " + (end - start) / 1_000_000.0 + " ms)");

        start = System.nanoTime();
        result = uniquePathsMath(m, n);
        end = System.nanoTime();
        System.out.println("Mathematical: " + result +
                " (Time: " + (end - start) / 1_000_000.0 + " ms)");

        // Detailed walkthrough for small example
        System.out.println("\n" + "=".repeat(40));
        System.out.println("DETAILED WALKTHROUGH: 3x3 Grid");
        System.out.println("=".repeat(40));

        System.out.println("Moving from (0,0) to (2,2)");
        System.out.println("Allowed moves: Right (R), Down (D)");
        System.out.println("Total moves needed: 2 Right + 2 Down = 4 moves");
        System.out.println("Need to choose 2 positions for Down moves out of 4");
        System.out.println("This gives us C(4,2) = 6 unique paths:");
        System.out.println("1. RRDD  2. RDRD  3. RDDR");
        System.out.println("4. DRRD  5. DRDR  6. DDRR");

        printDPTable(3, 3);
    }
}

/*
 * Algorithm Explanation:
 * 
 * The key insight is that to reach any cell (i,j), you can only come from:
 * 1. The cell above: (i-1, j) - by moving down
 * 2. The cell to the left: (i, j-1) - by moving right
 * 
 * Therefore: paths[i][j] = paths[i-1][j] + paths[i][j-1]
 * 
 * Base cases:
 * - First row: Only 1 way (keep going right)
 * - First column: Only 1 way (keep going down)
 * 
 * Time Complexity Analysis:
 * 1. 2D DP: O(m*n) time, O(m*n) space
 * 2. Optimized DP: O(m*n) time, O(min(m,n)) space
 * 3. Mathematical: O(min(m,n)) time, O(1) space
 * 4. Recursive: O(m*n) time, O(m*n) space
 * 
 * Mathematical Formula:
 * Total paths = C(m+n-2, m-1) = (m+n-2)! / ((m-1)! * (n-1)!)
 * 
 * This is because we need to make exactly (m-1) down moves and (n-1) right
 * moves,
 * and we need to choose which (m-1) positions out of (m+n-2) total moves are
 * down moves.
 */
