class Solution {

    // Time Complexity: O(m * n) — You visit each cell exactly once.
    // Space Complexity: O(m * n) — for the dp array.
    public int maximalSquare(char[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        int[][] dp = new int[rows][cols];
        int max = 0;

        for (int col = 0; col < cols; col++) {
            dp[0][col] = matrix[0][col] == '1' ? 1 : 0;
            max = Math.max(max, dp[0][col]);
        }

        for (int row = 0; row < rows; row++) {
            dp[row][0] = matrix[row][0] == '1' ? 1 : 0;
            max = Math.max(max, dp[row][0]);
        }

        for (int row = 1; row < rows; row++) {
            for (int col = 1; col < cols; col++) {
                if (matrix[row][col] == '0') {
                    dp[row][col] = 0;
                    continue;
                }
                dp[row][col] = Math.min(dp[row][col - 1],
                        Math.min(dp[row - 1][col], dp[row - 1][col - 1])) + 1;
                max = Math.max(max, dp[row][col]);
            }
        }

        return max * max;
    }

    public int maximalSquare2(char[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0)
            return 0;

        int rows = matrix.length;
        int cols = matrix[0].length;

        int[] dp = new int[cols + 1]; // 1 extra column for simpler boundary handling
        int max = 0;
        int prev = 0; // stores dp[j-1] from the previous row

        for (int i = 1; i <= rows; i++) {
            prev = 0;
            for (int j = 1; j <= cols; j++) {
                int temp = dp[j]; // save current dp[j] before overwriting
                if (matrix[i - 1][j - 1] == '1') {
                    dp[j] = Math.min(Math.min(dp[j - 1], dp[j]), prev) + 1;
                    max = Math.max(max, dp[j]);
                } else {
                    dp[j] = 0;
                }
                prev = temp; // update prev for next j
            }
        }

        return max * max;
    }

}
