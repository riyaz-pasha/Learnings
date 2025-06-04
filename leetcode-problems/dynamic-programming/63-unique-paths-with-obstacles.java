class UniquePathsWithObstacles {

    public int uniquePathsWithObstacles(int[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;
        int[][] dp = new int[rows][cols];
        dp[0][0] = grid[0][0] == 1 ? 0 : 1;

        for (int row = 1; row < rows; row++) {
            dp[row][0] = grid[row][0] == 1 ? 0 : dp[row - 1][0];
        }

        for (int col = 1; col < cols; col++) {
            dp[0][col] = grid[0][col] == 1 ? 0 : dp[0][col - 1];
        }

        for (int row = 1; row < rows; row++) {
            for (int col = 1; col < cols; col++) {
                dp[row][col] = grid[row][col] == 1 ? 0 : dp[row - 1][col] + dp[row][col - 1];
            }
        }

        return dp[rows - 1][cols - 1];
    }

    public int uniquePathsWithObstacles2(int[][] grid) {
        int rows = grid.length, cols = grid[0].length;
        int[] dp = new int[cols];
        dp[0] = grid[0][0] == 1 ? 0 : 1;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (grid[row][col] == 1) {
                    dp[col] = 0;
                } else if (col > 0) {
                    dp[col] += dp[col - 1];
                }
            }
        }

        return dp[cols - 1];
    }

    // 📦 Complexity:
    // Time: O(m * n)
    // Space: O(n) (space-optimized) or O(m * n) (full DP table)

}
