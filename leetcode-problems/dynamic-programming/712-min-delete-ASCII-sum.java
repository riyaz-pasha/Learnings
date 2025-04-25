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
}
