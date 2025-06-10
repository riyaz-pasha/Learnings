import java.util.Arrays;

class MinInsertionsForPalindrome {

    public int minInsertions(String s) {
        int n = s.length();
        int[][] memo = new int[n][n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(memo[i], -1);
        }
        return helper(s, 0, s.length() - 1, memo);
    }

    private int helper(String s, int start, int end, int[][] memo) {
        if (start >= end) {
            return 0;
        }
        if (memo[start][end] != -1) {
            return memo[start][end];
        }
        if (s.charAt(start) == s.charAt(end)) {
            memo[start][end] = helper(s, start + 1, end - 1, memo);
        } else {
            memo[start][end] = Math.min(
                    1 + helper(s, start + 1, end, memo),
                    1 + helper(s, start, end - 1, memo));
        }
        return memo[start][end];
    }

}

class MinInsertionsToPalindrome2 {

    public int minInsertions(String s) {
        int n = s.length();
        // Reverse of the original string
        String rev = new StringBuilder(s).reverse().toString();

        // LCS between s and reversed s = Longest Palindromic Subsequence
        return n - longestCommonSubsequence(s, rev);
    }

    private int longestCommonSubsequence(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];

        // Standard LCS DP table fill
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        return dp[m][n];
    }

}

class MinInsertionsToPalindrome3 {

    public int minInsertions(String s) {
        int n = s.length();
        int[][] dp = new int[n][n];

        // dp[i][j] = minimum insertions to make s[i..j] a palindrome
        for (int len = 2; len <= n; len++) {
            for (int i = 0; i <= n - len; i++) {
                int j = i + len - 1;
                if (s.charAt(i) == s.charAt(j)) {
                    dp[i][j] = dp[i + 1][j - 1]; // no insertion needed
                } else {
                    dp[i][j] = Math.min(dp[i + 1][j], dp[i][j - 1]) + 1;
                }
            }
        }

        return dp[0][n - 1];
    }

}
