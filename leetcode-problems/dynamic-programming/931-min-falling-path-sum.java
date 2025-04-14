class MinFallingPathSum {
    public int minFallingPathSum(int[][] matrix) {
        int n = matrix.length;
        int[][] dp = new int[n][n];
        for (int row = n - 2; row >= 0; row--) {
            for (int col = 0; col < n; col++) {
                if (col == 0) {
                    dp[row][col] = matrix[row][col] + Math.min(dp[row + 1][col], dp[row + 1][col + 1]);
                } else if (col == n - 1) {
                    dp[row][col] = matrix[row][col] + Math.min(dp[row + 1][col], dp[row + 1][col - 1]);
                } else {
                    dp[row][col] = matrix[row][col]
                            + Math.min(dp[row + 1][col], Math.min(dp[row + 1][col - 1], dp[row + 1][col + 1]));
                }
            }
        }
        int min = Integer.MAX_VALUE;
        for (int col : dp[0]) {
            if (col < min)
                min = col;
        }
        return min;
    }
}