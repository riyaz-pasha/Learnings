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
}
