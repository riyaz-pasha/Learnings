class MinFallingPathSum {

    public int minFallingPathSum(int[][] matrix) {
        int n = matrix.length;
        int[][] dp = new int[n][n];
        for (int row = n - 2; row >= 0; row--) {
            for (int col = 0; col < n; col++) {
                if (col == 0) {
                    dp[row][col] = matrix[row][col]
                            + Math.min(dp[row + 1][col], dp[row + 1][col + 1]);
                } else if (col == n - 1) {
                    dp[row][col] = matrix[row][col]
                            + Math.min(dp[row + 1][col], dp[row + 1][col - 1]);
                } else {
                    dp[row][col] = matrix[row][col]
                            + Math.min(dp[row + 1][col],
                                    Math.min(dp[row + 1][col - 1], dp[row + 1][col + 1]));
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

    // Time: O(n^2) — visit each cell once
    // Space: O(n^2) — for the DP table
    public int minFallingPathSum2(int[][] matrix) {
        int n = matrix.length;
        int[][] dp = new int[n][n];

        // Base case: first row is the same
        for (int j = 0; j < n; j++) {
            dp[0][j] = matrix[0][j];
        }

        // Fill the dp table
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int minAbove = dp[i - 1][j]; // directly above

                if (j > 0) {
                    minAbove = Math.min(minAbove, dp[i - 1][j - 1]); // diagonally left
                }

                if (j < n - 1) {
                    minAbove = Math.min(minAbove, dp[i - 1][j + 1]); // diagonally right
                }

                dp[i][j] = matrix[i][j] + minAbove;
            }
        }

        // The answer is the min value in the last row
        int result = Integer.MAX_VALUE;
        for (int j = 0; j < n; j++) {
            result = Math.min(result, dp[n - 1][j]);
        }

        return result;
    }

    // Time: O(n²) – All n x n elements are processed once.
    // Space: O(n) – Only two rows are stored at a time: prev and curr.
    public int minFallingPathSum3(int[][] matrix) {
        int n = matrix.length;
        int[] prev = new int[n];

        // Initialize with the first row
        for (int j = 0; j < n; j++) {
            prev[j] = matrix[0][j];
        }

        // DP for each row starting from the second
        for (int i = 1; i < n; i++) {
            int[] curr = new int[n];
            for (int j = 0; j < n; j++) {
                int minAbove = prev[j];

                if (j > 0) {
                    minAbove = Math.min(minAbove, prev[j - 1]);
                }

                if (j < n - 1) {
                    minAbove = Math.min(minAbove, prev[j + 1]);
                }

                curr[j] = matrix[i][j] + minAbove;
            }
            prev = curr; // Move to the next row
        }

        // Final answer is the min in the last processed row
        int result = Integer.MAX_VALUE;
        for (int val : prev) {
            result = Math.min(result, val);
        }

        return result;
    }

}
