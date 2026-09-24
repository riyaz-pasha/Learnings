class LongestPalindromicSubsequence {

    public int longestPalindromeSubseq(String s) {
        int n = s.length();
        int[][] dp = new int[n][n];
        return helper(s, 0, n - 1, dp);
    }

    private int helper(String s, int left, int right, int[][] dp) {
        if (left > right)
            return 0;
        if (left == right)
            return 1;
        if (dp[left][right] != 0)
            return dp[left][right];
        if (s.charAt(left) == s.charAt(right)) {
            dp[left][right] = 2 + helper(s, left + 1, right - 1, dp);
        } else {
            dp[left][right] = Math.max(
                    helper(s, left + 1, right, dp),
                    helper(s, left, right - 1, dp));
        }
        return dp[left][right];
    }

}

class LongestPalindromicSubsequence2 {

    public int longestPalindromeSubseq(String s) {
        int n = s.length();

        // dp[i][j] will store the length of the longest palindromic subsequence in
        // s[i..j]
        int[][] dp = new int[n][n];

        // Every single character is a palindrome of length 1
        for (int i = 0; i < n; i++) {
            dp[i][i] = 1;
        }

        // Fill the table for substrings of length 2 to n
        for (int len = 2; len <= n; len++) {
            for (int i = 0; i <= n - len; i++) {
                int j = i + len - 1; // endpoint of substring

                if (s.charAt(i) == s.charAt(j)) {
                    if (len == 2) {
                        dp[i][j] = 2; // e.g., "aa"
                    } else {
                        dp[i][j] = dp[i + 1][j - 1] + 2;
                    }
                } else {
                    // Take the max by excluding either end
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j - 1]);
                }
            }
        }

        // Final result: longest palindromic subsequence from s[0..n-1]
        return dp[0][n - 1];
    }

}
