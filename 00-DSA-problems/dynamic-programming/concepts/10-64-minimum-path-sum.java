/*
 * Given a m x n grid filled with non-negative numbers, find a path from top
 * left to bottom right, which minimizes the sum of all numbers along its path.
 * 
 * Note: You can only move either down or right at any point in time.
 * 
 * Example 1:
 * Input: grid = [[1,3,1],[1,5,1],[4,2,1]]
 * Output: 7
 * Explanation: Because the path 1 → 3 → 1 → 1 → 1 minimizes the sum.
 * 
 * Example 2:
 * Input: grid = [[1,2,3],[4,5,6]]
 * Output: 12
 */

class MinimumPathSumMemo {

    public int minPathSum(int[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;
        Integer[][] memo = new Integer[rows][cols];
        return this.minPathSum(grid, memo, rows - 1, cols - 1);
    }

    private int minPathSum(int[][] grid, Integer[][] memo, int row, int col) {
        if (row < 0 || col < 0) {
            return Integer.MAX_VALUE;
        }
        if (row == 0 && col == 0) {
            return grid[0][0];
        }
        if (memo[row][col] != null) {
            return memo[row][col];
        }
        return memo[row][col] = grid[row][col] + Math
                .min(this.minPathSum(grid, memo, row - 1, col), this.minPathSum(grid, memo, row, col - 1));
    }

}

class MinimumPathSumDP {

    public int minPathSum(int[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;
        int[][] dp = new int[rows][cols];
        dp[0][0] = grid[0][0];

        for (int row = 1; row < rows; row++) {
            dp[row][0] = dp[row - 1][0] + grid[row][0];
        }

        for (int col = 1; col < cols; col++) {
            dp[0][col] = dp[0][col - 1] + grid[0][col];
        }

        for (int row = 1; row < rows; row++) {
            for (int col = 1; col < cols; col++) {
                dp[row][col] = grid[row][col] + Math.min(dp[row - 1][col], dp[row][col - 1]);
            }
        }

        return dp[rows - 1][cols - 1];
    }

}

// Solution 1: Top-Down Approach with Memoization (Recursive)
// Time: O(m*n), Space: O(m*n)
class Solution1_TopDownMemo {
    public int minPathSum(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        Integer[][] memo = new Integer[m][n];
        return dfs(grid, 0, 0, memo);
    }

    private int dfs(int[][] grid, int i, int j, Integer[][] memo) {
        int m = grid.length;
        int n = grid[0].length;

        // Base case: reached destination
        if (i == m - 1 && j == n - 1) {
            return grid[i][j];
        }

        // Out of bounds
        if (i >= m || j >= n) {
            return Integer.MAX_VALUE;
        }

        // Check memo
        if (memo[i][j] != null) {
            return memo[i][j];
        }

        // Explore both directions
        int right = dfs(grid, i, j + 1, memo);
        int down = dfs(grid, i + 1, j, memo);

        memo[i][j] = grid[i][j] + Math.min(right, down);
        return memo[i][j];
    }
}

// Solution 2: Bottom-Up Dynamic Programming (2D Table)
// Time: O(m*n), Space: O(m*n)
class Solution2_BottomUp2D {
    public int minPathSum(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;

        // Create DP table
        int[][] dp = new int[m][n];

        // Initialize starting point
        dp[0][0] = grid[0][0];

        // Fill first row (can only come from left)
        for (int j = 1; j < n; j++) {
            dp[0][j] = dp[0][j - 1] + grid[0][j];
        }

        // Fill first column (can only come from top)
        for (int i = 1; i < m; i++) {
            dp[i][0] = dp[i - 1][0] + grid[i][0];
        }

        // Fill the rest of the table
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                dp[i][j] = grid[i][j] + Math.min(dp[i - 1][j], dp[i][j - 1]);
            }
        }

        return dp[m - 1][n - 1];
    }
}

// Solution 3: Space Optimized Bottom-Up (1D Array)
// Time: O(m*n), Space: O(n)
class Solution3_SpaceOptimized1D {
    public int minPathSum(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;

        // Use 1D array to represent current row
        int[] dp = new int[n];

        // Initialize first row
        dp[0] = grid[0][0];
        for (int j = 1; j < n; j++) {
            dp[j] = dp[j - 1] + grid[0][j];
        }

        // Process each subsequent row
        for (int i = 1; i < m; i++) {
            // Update first column
            dp[0] += grid[i][0];

            // Update rest of the row
            for (int j = 1; j < n; j++) {
                dp[j] = grid[i][j] + Math.min(dp[j], dp[j - 1]);
            }
        }

        return dp[n - 1];
    }
}

// Solution 4: In-Place Modification (Constant Space)
// Time: O(m*n), Space: O(1)
class Solution4_InPlace {
    public int minPathSum(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;

        // Fill first row
        for (int j = 1; j < n; j++) {
            grid[0][j] += grid[0][j - 1];
        }

        // Fill first column
        for (int i = 1; i < m; i++) {
            grid[i][0] += grid[i - 1][0];
        }

        // Fill the rest
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                grid[i][j] += Math.min(grid[i - 1][j], grid[i][j - 1]);
            }
        }

        return grid[m - 1][n - 1];
    }
}

// Solution 5: Bottom-Up with Path Reconstruction
// Time: O(m*n), Space: O(m*n)
class Solution5_WithPath {
    public class Result {
        int minSum;
        String path;

        Result(int sum, String path) {
            this.minSum = sum;
            this.path = path;
        }
    }

    public Result minPathSumWithPath(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;

        int[][] dp = new int[m][n];
        String[][] path = new String[m][n];

        // Initialize
        dp[0][0] = grid[0][0];
        path[0][0] = "(" + 0 + "," + 0 + ")";

        // First row
        for (int j = 1; j < n; j++) {
            dp[0][j] = dp[0][j - 1] + grid[0][j];
            path[0][j] = path[0][j - 1] + " -> (" + 0 + "," + j + ")";
        }

        // First column
        for (int i = 1; i < m; i++) {
            dp[i][0] = dp[i - 1][0] + grid[i][0];
            path[i][0] = path[i - 1][0] + " -> (" + i + "," + 0 + ")";
        }

        // Fill rest
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                if (dp[i - 1][j] < dp[i][j - 1]) {
                    dp[i][j] = dp[i - 1][j] + grid[i][j];
                    path[i][j] = path[i - 1][j] + " -> (" + i + "," + j + ")";
                } else {
                    dp[i][j] = dp[i][j - 1] + grid[i][j];
                    path[i][j] = path[i][j - 1] + " -> (" + i + "," + j + ")";
                }
            }
        }

        return new Result(dp[m - 1][n - 1], path[m - 1][n - 1]);
    }
}

// Solution 6: Iterative with Queue (BFS-like approach)
// Time: O(m*n), Space: O(m*n)
class Solution6_Iterative {
    public int minPathSum(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;

        int[][] dp = new int[m][n];

        // Initialize with max values
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                dp[i][j] = Integer.MAX_VALUE;
            }
        }

        dp[0][0] = grid[0][0];

        // Process each cell level by level
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (dp[i][j] == Integer.MAX_VALUE)
                    continue;

                // Update right neighbor
                if (j + 1 < n) {
                    dp[i][j + 1] = Math.min(dp[i][j + 1], dp[i][j] + grid[i][j + 1]);
                }

                // Update down neighbor
                if (i + 1 < m) {
                    dp[i + 1][j] = Math.min(dp[i + 1][j], dp[i][j] + grid[i + 1][j]);
                }
            }
        }

        return dp[m - 1][n - 1];
    }
}

// Solution 7: Alternative Top-Down (starting from end)
// Time: O(m*n), Space: O(m*n)
class Solution7_TopDownFromEnd {
    public int minPathSum(int[][] grid) {
        int m = grid.length;
        int n = grid[0].length;
        Integer[][] memo = new Integer[m][n];
        return dfsFromEnd(grid, m - 1, n - 1, memo);
    }

    private int dfsFromEnd(int[][] grid, int i, int j, Integer[][] memo) {
        // Base case: reached start
        if (i == 0 && j == 0) {
            return grid[0][0];
        }

        // Out of bounds
        if (i < 0 || j < 0) {
            return Integer.MAX_VALUE;
        }

        if (memo[i][j] != null) {
            return memo[i][j];
        }

        // Come from top or left
        int fromTop = dfsFromEnd(grid, i - 1, j, memo);
        int fromLeft = dfsFromEnd(grid, i, j - 1, memo);

        memo[i][j] = grid[i][j] + Math.min(fromTop, fromLeft);
        return memo[i][j];
    }
}

// Test class with comprehensive testing
class TestMinPathSum {
    public static void main(String[] args) {
        // Test all solutions
        Solution1_TopDownMemo sol1 = new Solution1_TopDownMemo();
        Solution2_BottomUp2D sol2 = new Solution2_BottomUp2D();
        Solution3_SpaceOptimized1D sol3 = new Solution3_SpaceOptimized1D();
        Solution4_InPlace sol4 = new Solution4_InPlace();
        Solution5_WithPath sol5 = new Solution5_WithPath();
        Solution6_Iterative sol6 = new Solution6_Iterative();
        Solution7_TopDownFromEnd sol7 = new Solution7_TopDownFromEnd();

        // Test case 1
        int[][] grid1 = { { 1, 3, 1 }, { 1, 5, 1 }, { 4, 2, 1 } };
        System.out.println("Test 1 - Expected: 7");
        System.out.println("Top-Down Memo: " + sol1.minPathSum(deepCopy(grid1)));
        System.out.println("Bottom-Up 2D: " + sol2.minPathSum(deepCopy(grid1)));
        System.out.println("Space Optimized: " + sol3.minPathSum(deepCopy(grid1)));
        System.out.println("In-Place: " + sol4.minPathSum(deepCopy(grid1)));
        System.out.println("Iterative: " + sol6.minPathSum(deepCopy(grid1)));
        System.out.println("Top-Down from End: " + sol7.minPathSum(deepCopy(grid1)));

        // Show path
        Solution5_WithPath.Result result1 = sol5.minPathSumWithPath(deepCopy(grid1));
        System.out.println("Min Sum: " + result1.minSum);
        System.out.println("Path: " + result1.path);

        // Test case 2
        int[][] grid2 = { { 1, 2, 3 }, { 4, 5, 6 } };
        System.out.println("\nTest 2 - Expected: 12");
        System.out.println("Top-Down Memo: " + sol1.minPathSum(deepCopy(grid2)));
        System.out.println("Bottom-Up 2D: " + sol2.minPathSum(deepCopy(grid2)));

        // Edge case: single cell
        int[][] grid3 = { { 5 } };
        System.out.println("\nTest 3 - Expected: 5 (single cell)");
        System.out.println("Bottom-Up 2D: " + sol2.minPathSum(deepCopy(grid3)));

        // Edge case: single row
        int[][] grid4 = { { 1, 2, 3, 4, 5 } };
        System.out.println("\nTest 4 - Expected: 15 (single row)");
        System.out.println("Bottom-Up 2D: " + sol2.minPathSum(deepCopy(grid4)));

        // Edge case: single column
        int[][] grid5 = { { 1 }, { 2 }, { 3 } };
        System.out.println("\nTest 5 - Expected: 6 (single column)");
        System.out.println("Bottom-Up 2D: " + sol2.minPathSum(deepCopy(grid5)));
    }

    private static int[][] deepCopy(int[][] original) {
        int[][] copy = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = original[i].clone();
        }
        return copy;
    }
}
