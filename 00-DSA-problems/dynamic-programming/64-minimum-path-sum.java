class MinimumPathSum {

    // Time: O(m * n) — each cell is visited once.
    // Space: O(m * n) for full DP table
    public int minPathSum(int[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;
        int[][] dp = new int[rows][cols];

        dp[0][0] = grid[0][0];

        // fill first row
        for (int col = 1; col < cols; col++) {
            dp[0][col] = dp[0][col - 1] + grid[0][col];
        }

        // fill first col
        for (int row = 1; row < rows; row++) {
            dp[row][0] = dp[row - 1][0] + grid[row][0];
        }

        for (int row = 1; row < rows; row++) {
            for (int col = 1; col < cols; col++) {
                dp[row][col] = grid[row][col] + Math.min(dp[row - 1][col], dp[row][col - 1]);
            }
        }

        return dp[rows - 1][cols - 1];
    }

    // Time: O(m * n) — each cell is visited once.
    // Space: O(n) for optimized version.
    public int minPathSum2(int[][] grid) {
        int m = grid.length, n = grid[0].length;
        int[] dp = new int[n];

        dp[0] = grid[0][0];

        // Fill first row
        for (int j = 1; j < n; j++) {
            dp[j] = dp[j - 1] + grid[0][j];
        }

        for (int i = 1; i < m; i++) {
            dp[0] += grid[i][0]; // update first column
            for (int j = 1; j < n; j++) {
                dp[j] = grid[i][j] + Math.min(dp[j], dp[j - 1]);
            }
        }

        return dp[n - 1];
    }

}
