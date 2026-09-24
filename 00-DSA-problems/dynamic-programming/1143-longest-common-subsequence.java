import java.util.Arrays;

class LongestCommonSubsequence {

    public int longestCommonSubsequence(String text1, String text2) {
        int[][] memo = new int[text1.length()][text2.length()];
        for (int i = 0; i < text1.length(); i++) {
            Arrays.fill(memo[i], -1);
        }
        return helper(text1, text2, 0, 0, memo);
    }

    // 💡 How it works:
    // The recursion checks if the characters at indices i and j match.
    // If they do → include the character in LCS and move both pointers.
    // If not → try skipping one character from either string and take the maximum.
    // Memoization prevents recomputation of subproblems.
    // 🧠 Time & Space:
    // Time: O(m × n) (because we memoize each (i, j) pair)
    // Space: O(m × n) for memo + O(m + n) for recursion stack
    private int helper(String text1, String text2, int i, int j, int[][] memo) {
        if (i >= text1.length() || j >= text2.length())
            return 0;
        if (memo[i][j] != -1)
            return memo[i][j];
        if (text1.charAt(i) == text2.charAt(j)) {
            // Characters match → move both pointers back
            memo[i][j] = 1 + helper(text1, text2, i + 1, j + 1, memo);
        } else {
            // Try skipping one character from either string
            memo[i][j] = Math.max(
                    helper(text1, text2, i + 1, j, memo),
                    helper(text1, text2, i, j + 1, memo));
        }
        return memo[i][j];
    }

}

class LongestCommonSubsequence2 {

    // Time complexity: O(m × n)
    // Space complexity: O(m × n)
    public int longestCommonSubsequence(String text1, String text2) {
        int m = text1.length();
        int n = text2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[m][n];
    }

}

class LongestCommonSubsequence3 {

    // Time complexity: O(m × n)
    // Space complexity: O(m × n)
    public int longestCommonSubsequence(String text1, String text2) {
        int m = text1.length(), n = text2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 0; j--) {
                if (text1.charAt(i) == text2.charAt(j)) {
                    dp[i][j] = 1 + dp[i + 1][j + 1];
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }
        return dp[0][0];
    }

}
