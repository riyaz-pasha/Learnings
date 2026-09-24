class MinASCIIDeleteSum {

    public int minimumDeleteSum(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] memo = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                memo[i][j] = -1;
            }
        }
        return helper(s1, s2, 0, 0, memo);
    }

    private int helper(String s1, String s2, int i, int j, int[][] memo) {
        if (memo[i][j] != -1)
            return memo[i][j];
        if (i >= s1.length()) {
            int sum = 0;
            for (int k = j; k < s2.length(); k++) {
                sum += s2.charAt(k);
            }
            return memo[i][j] = sum;
        }
        if (j >= s2.length()) {
            int sum = 0;
            for (int k = i; k < s1.length(); k++) {
                sum += s1.charAt(k);
            }
            return memo[i][j] = sum;
        }
        if (s1.charAt(i) == s2.charAt(j)) {
            memo[i][j] = helper(s1, s2, i + 1, j + 1, memo);
        } else {
            int deleteS1 = s1.charAt(i) + helper(s1, s2, i + 1, j, memo);
            int deleteS2 = s2.charAt(j) + helper(s1, s2, i, j + 1, memo);
            memo[i][j] = Math.min(deleteS1, deleteS2);
        }
        return memo[i][j];
    }

    public int minimumDeleteSum2(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] dp = new int[m + 1][n + 1];

        // Base cases: if one string is empty, delete all characters from the other
        for (int i = m - 1; i >= 0; i--) {
            dp[i][n] = dp[i + 1][n] + s1.charAt(i);
        }
        for (int j = n - 1; j >= 0; j--) {
            dp[m][j] = dp[m][j + 1] + s2.charAt(j);
        }

        // Fill DP table
        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 0; j--) {
                if (s1.charAt(i) == s2.charAt(j)) {
                    dp[i][j] = dp[i + 1][j + 1]; // Characters match, no deletion
                } else {
                    dp[i][j] = Math.min(
                            s1.charAt(i) + dp[i + 1][j], // Delete from s1
                            s2.charAt(j) + dp[i][j + 1] // Delete from s2
                    );
                }
            }
        }

        return dp[0][0];
    }

    public int minimumDeleteSum3(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] dp = new int[m + 1][n + 1];

        // Base cases
        for (int i = 1; i <= m; i++) {
            dp[i][0] = dp[i - 1][0] + s1.charAt(i - 1);
        }
        for (int j = 1; j <= n; j++) {
            dp[0][j] = dp[0][j - 1] + s2.charAt(j - 1);
        }

        // Fill the table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1]; // Match, no delete
                } else {
                    dp[i][j] = Math.min(
                            s1.charAt(i - 1) + dp[i - 1][j], // Delete from s1
                            s2.charAt(j - 1) + dp[i][j - 1] // Delete from s2
                    );
                }
            }
        }

        return dp[m][n];
    }

}
