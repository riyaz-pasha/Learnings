import java.util.Arrays;

class UniquePaths {
    // O(m x n) time and space complexity
    public int uniquePaths(int m, int n) {
        int[][] dp = new int[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (i == 0 || j == 0) {
                    dp[i][j] = 1;
                } else {
                    dp[i][j] = dp[i - 1][j] + dp[i][j - 1];
                }
            }
        }
        return dp[m - 1][n - 1];
    }

    public int uniquePaths1(int m, int n) {
        int[] dp = new int[n];
        Arrays.fill(dp, 1);
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                dp[j] = dp[j] + dp[j - 1];
            }
        }
        return dp[n - 1];
    }

    // Time Complexity: O(m * n)
    // Space Complexity: O(m * n)
    public int uniquePaths2(int m, int n) {
        int[][] dp = new int[m][n];

        // Initialize the first column (i.e., dp[i][0])
        for (int i = 0; i < m; i++) {
            dp[i][0] = 1;
        }

        // Initialize the first row (i.e., dp[0][j])
        for (int j = 0; j < n; j++) {
            dp[0][j] = 1;
        }

        // Fill the rest of the dp table
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                dp[i][j] = dp[i - 1][j] + dp[i][j - 1];
            }
        }

        return dp[m - 1][n - 1];
    }

    public int uniquePathsRec(int m, int n) {
        if (m <= 0 || n <= 0)
            return 0;
        if (m == 1 || n == 1)
            return 1;
        return uniquePathsRec(m - 1, n) + uniquePathsRec(m, n - 1);
    }
}
