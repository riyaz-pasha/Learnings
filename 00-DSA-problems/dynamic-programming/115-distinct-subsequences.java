class DistinctSubsequences {

    public int numDistinct(String s, String t) {
        int m = s.length();
        int n = t.length();
        int[][] memo = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                memo[i][j] = -1;
            }
        }
        return helper(s, t, 0, 0, memo);
    }

    private int helper(String s, String t, int i, int j, int[][] memo) {
        if (memo[i][j] != -1)
            return memo[i][j];
        if (j == t.length()) {
            return 1;
        }
        if (i >= s.length()) {
            return 0;
        }
        if (s.charAt(i) == t.charAt(j)) {
            memo[i][j] = helper(s, t, i + 1, j + 1, memo) + helper(s, t, i + 1, j, memo);
        } else {
            memo[i][j] = helper(s, t, i + 1, j, memo);
        }
        return memo[i][j];
    }

    // Time: O(m * n)
    // Space: O(m * n)
    public int numDistinct2(String s, String t) {
        int m = s.length();
        int n = t.length();
        int[][] dp = new int[m + 1][n + 1];

        // Base case: An empty t can be formed by any prefix of s
        for (int i = 0; i <= m; i++) {
            dp[i][0] = 1;
        }

        // Fill the DP table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s.charAt(i - 1) == t.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + dp[i - 1][j];
                } else {
                    dp[i][j] = dp[i - 1][j];
                }
            }
        }

        return dp[m][n];
    }

    public int numDistinct3(String s, String t) {
        int m = s.length(), n = t.length();
        int[][] dp = new int[m + 1][n + 1];

        // An empty t can always be formed from any prefix of s
        for (int i = 0; i <= m; i++) {
            dp[i][n] = 1;
        }

        // Fill the table bottom-up
        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 0; j--) {
                dp[i][j] = dp[i + 1][j]; // Skip s[i]
                if (s.charAt(i) == t.charAt(j)) {
                    dp[i][j] += dp[i + 1][j + 1]; // Match s[i] with t[j]
                }
            }
        }

        return dp[0][0];
    }

}

class DistinctSubsequencesV1 {

    public int numDistinct(String s, String t) {
        return helper(s, t, 0, 0);
    }

    private int helper(String s, String t, int i, int j) {
        if (j == t.length())
            return 1;
        if (i >= s.length())
            return 0;
        if (s.charAt(i) == t.charAt(j)) {
            return helper(s, t, i + 1, j + 1) + helper(s, t, i + 1, j);
        }
        return helper(s, t, i + 1, j);
    }

}
