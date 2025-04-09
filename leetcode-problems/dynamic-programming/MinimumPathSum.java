public class MinimumPathSum {
    public int minPathSum(int[][] grid) {
        int rows = grid.length, cols = grid[0].length;
        int[][] dp = new int[rows][cols];
        dp[0][0] = grid[0][0];

        for (int col = 1; col < cols; col++) {
            dp[0][col] = grid[0][col] + dp[0][col - 1];
        }

        for (int row = 1; row < rows; row++) {
            dp[row][0] = grid[row][0] + dp[row - 1][0];
        }

        for (int row = 1; row < rows; row++) {
            for (int col = 1; col < cols; col++) {
                dp[row][col] = grid[row][col] + Math.min(dp[row - 1][col], dp[row][col - 1]);
            }
        }

        return dp[rows - 1][cols - 1];
    }
}
