/*
 * You are given an m x n integer array grid. There is a robot initially located
 * at the top-left corner (i.e., grid[0][0]). The robot tries to move to the
 * bottom-right corner (i.e., grid[m - 1][n - 1]). The robot can only move
 * either down or right at any point in time.
 * 
 * An obstacle and space are marked as 1 or 0 respectively in grid. A path that
 * the robot takes cannot include any square that is an obstacle.
 * 
 * Return the number of possible unique paths that the robot can take to reach
 * the bottom-right corner.
 * 
 * The testcases are generated so that the answer will be less than or equal to
 * 2 * 109.
 * 
 * Input: obstacleGrid = [[0,0,0],[0,1,0],[0,0,0]]
 * Output: 2
 * Explanation: There is one obstacle in the middle of the 3x3 grid above.
 * There are two ways to reach the bottom-right corner:
 * 1. Right -> Right -> Down -> Down
 * 2. Down -> Down -> Right -> Right
 * 
 * Example 2:
 * Input: obstacleGrid = [[0,1],[0,0]]
 * Output: 1
 */

class GridUniquePaths2Memo {

    /*
     * Time Complexity: O(N*M)
     * Reason: At max, there will be N*M calls of recursion.
     * 
     * Space Complexity: O((M-1)+(N-1)) + O(N*M)
     * Reason: We are using a recursion stack space:O((M-1)+(N-1)), here (M-1)+(N-1)
     * is the path length and an external DP Array of size ‘N*M’.
     */
    public int uniquePathsWithObstacles(int[][] obstacleGrid) {
        int rows = obstacleGrid.length;
        int cols = obstacleGrid[0].length;
        Integer[][] memo = new Integer[rows][cols];
        return this.uniquePathsWithObstacles(obstacleGrid, memo, rows - 1, cols - 1);
    }

    private int uniquePathsWithObstacles(int[][] obstacleGrid, Integer[][] memo, int row, int col) {
        if (row < 0 || col < 0 || obstacleGrid[row][col] == -1) {
            return 0;
        }
        if (row == 0 && col == 0) {
            return 1;
        }
        if (memo[row][col] != null) {
            return memo[row][col];
        }
        return memo[row][col] = this.uniquePathsWithObstacles(obstacleGrid, memo, row - 1, col)
                + this.uniquePathsWithObstacles(obstacleGrid, memo, row, col - 1);
    }

}

class GridUniquePaths2DP {

    /*
     * Time Complexity: O(N*M)
     * Reason: There are two nested loops
     * 
     * Space Complexity: O(N*M)
     * Reason: We are using an external array of size ‘N*M’’.
     */
    public int uniquePathsWithObstacles(int[][] obstacleGrid) {
        int rows = obstacleGrid.length;
        int cols = obstacleGrid[0].length;
        int OBSTACLE = 1;

        if (obstacleGrid[0][0] == 1) {
            return 0;
        }

        int[][] dp = new int[rows][cols];
        dp[0][0] = obstacleGrid[0][0] == OBSTACLE ? 0 : 1;

        for (int row = 1; row < rows; row++) {
            dp[row][0] = obstacleGrid[row][0] == OBSTACLE ? 0 : dp[row - 1][0];
        }

        for (int col = 1; col < cols; col++) {
            dp[0][col] = obstacleGrid[0][col] == OBSTACLE ? 0 : dp[0][col - 1];
        }

        for (int row = 1; row < rows; row++) {
            for (int col = 1; col < cols; col++) {
                if (obstacleGrid[row][col] == OBSTACLE) {
                    dp[row][col] = 0;
                } else {
                    dp[row][col] = dp[row - 1][col] + dp[row][col - 1];
                }
            }
        }
        return dp[rows - 1][cols - 1];
    }

}

// Solution 1: 2D Dynamic Programming
// Time: O(m*n), Space: O(m*n)
class Solution {
    public int uniquePathsWithObstacles(int[][] obstacleGrid) {
        int m = obstacleGrid.length;
        int n = obstacleGrid[0].length;

        // If start or end is blocked, no path exists
        if (obstacleGrid[0][0] == 1 || obstacleGrid[m - 1][n - 1] == 1) {
            return 0;
        }

        // dp[i][j] represents number of ways to reach cell (i,j)
        int[][] dp = new int[m][n];

        // Initialize starting point
        dp[0][0] = 1;

        // Fill first row
        for (int j = 1; j < n; j++) {
            if (obstacleGrid[0][j] == 0) {
                dp[0][j] = dp[0][j - 1];
            } else {
                dp[0][j] = 0;
            }
        }

        // Fill first column
        for (int i = 1; i < m; i++) {
            if (obstacleGrid[i][0] == 0) {
                dp[i][0] = dp[i - 1][0];
            } else {
                dp[i][0] = 0;
            }
        }

        // Fill the rest of the grid
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                if (obstacleGrid[i][j] == 0) {
                    dp[i][j] = dp[i - 1][j] + dp[i][j - 1];
                } else {
                    dp[i][j] = 0;
                }
            }
        }

        return dp[m - 1][n - 1];
    }
}

// Solution 2: Space Optimized (1D DP)
// Time: O(m*n), Space: O(n)
class SolutionOptimized {
    public int uniquePathsWithObstacles(int[][] obstacleGrid) {
        int m = obstacleGrid.length;
        int n = obstacleGrid[0].length;

        // If start or end is blocked, no path exists
        if (obstacleGrid[0][0] == 1 || obstacleGrid[m - 1][n - 1] == 1) {
            return 0;
        }

        // Use 1D array to store current row values
        int[] dp = new int[n];
        dp[0] = 1;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (obstacleGrid[i][j] == 1) {
                    dp[j] = 0;
                } else if (j > 0) {
                    dp[j] += dp[j - 1];
                }
            }
        }

        return dp[n - 1];
    }
}

// Solution 3: In-place modification (if modifying input is allowed)
// Time: O(m*n), Space: O(1)
class SolutionInPlace {
    public int uniquePathsWithObstacles(int[][] obstacleGrid) {
        int m = obstacleGrid.length;
        int n = obstacleGrid[0].length;

        // If start or end is blocked, no path exists
        if (obstacleGrid[0][0] == 1 || obstacleGrid[m - 1][n - 1] == 1) {
            return 0;
        }

        // Convert obstacles (1) to 0 and free spaces (0) to path counts
        obstacleGrid[0][0] = 1;

        // Initialize first row
        for (int j = 1; j < n; j++) {
            obstacleGrid[0][j] = (obstacleGrid[0][j] == 0 && obstacleGrid[0][j - 1] == 1) ? 1 : 0;
        }

        // Initialize first column
        for (int i = 1; i < m; i++) {
            obstacleGrid[i][0] = (obstacleGrid[i][0] == 0 && obstacleGrid[i - 1][0] == 1) ? 1 : 0;
        }

        // Fill the rest
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                if (obstacleGrid[i][j] == 0) {
                    obstacleGrid[i][j] = obstacleGrid[i - 1][j] + obstacleGrid[i][j - 1];
                } else {
                    obstacleGrid[i][j] = 0;
                }
            }
        }

        return obstacleGrid[m - 1][n - 1];
    }
}

// Test class to verify solutions
class TestUniquePathsWithObstacles {
    public static void main(String[] args) {
        Solution sol = new Solution();
        SolutionOptimized solOpt = new SolutionOptimized();
        SolutionInPlace solInPlace = new SolutionInPlace();

        // Test case 1
        int[][] grid1 = { { 0, 0, 0 }, { 0, 1, 0 }, { 0, 0, 0 } };
        System.out.println("Test 1 - Expected: 2");
        System.out.println("2D DP: " + sol.uniquePathsWithObstacles(grid1));

        int[][] grid1Copy = { { 0, 0, 0 }, { 0, 1, 0 }, { 0, 0, 0 } };
        System.out.println("1D DP: " + solOpt.uniquePathsWithObstacles(grid1Copy));

        int[][] grid1Copy2 = { { 0, 0, 0 }, { 0, 1, 0 }, { 0, 0, 0 } };
        System.out.println("In-place: " + solInPlace.uniquePathsWithObstacles(grid1Copy2));

        // Test case 2
        int[][] grid2 = { { 0, 1 }, { 0, 0 } };
        System.out.println("\nTest 2 - Expected: 1");
        System.out.println("2D DP: " + sol.uniquePathsWithObstacles(grid2));

        int[][] grid2Copy = { { 0, 1 }, { 0, 0 } };
        System.out.println("1D DP: " + solOpt.uniquePathsWithObstacles(grid2Copy));

        int[][] grid2Copy2 = { { 0, 1 }, { 0, 0 } };
        System.out.println("In-place: " + solInPlace.uniquePathsWithObstacles(grid2Copy2));

        // Test case 3 - Edge case: start blocked
        int[][] grid3 = { { 1, 0 }, { 0, 0 } };
        System.out.println("\nTest 3 - Expected: 0 (start blocked)");
        System.out.println("2D DP: " + sol.uniquePathsWithObstacles(grid3));

        // Test case 4 - Single cell
        int[][] grid4 = { { 0 } };
        System.out.println("\nTest 4 - Expected: 1 (single free cell)");
        System.out.println("2D DP: " + sol.uniquePathsWithObstacles(grid4));
    }

}
