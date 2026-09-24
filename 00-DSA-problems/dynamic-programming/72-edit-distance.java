import java.util.Arrays;

class EditDistance {
    public int minDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] memo = new int[m][n];
        for (int i = 0; i < s1.length(); i++) {
            Arrays.fill(memo[i], -1);
        }
        return helper(s1, s2, 0, 0, memo);
    }

    private int helper(String s1, String s2, int i, int j, int[][] memo) {
        if (memo[i][j] != -1)
            return memo[i][j];
        if (i >= s1.length()) {
            return s2.length() - j;
        }
        if (j >= s2.length()) {
            return s1.length() - i;
        }
        if (s1.charAt(i) == s2.charAt(j)) {
            memo[i][j] = helper(s1, s2, i + 1, j + 1, memo);
        } else {
            int insert = helper(s1, s2, i, j + 1, memo);
            int delete = helper(s1, s2, i + 1, j, memo);
            int replace = helper(s1, s2, i + 1, j + 1, memo);
            memo[i][j] = Math.min(insert, Math.min(delete, replace));
        }
        return memo[i][j];
    }

    // 🧠 Time and Space Complexity:
    // Time: O(m * n)
    // Space: O(m * n) (can be optimized to O(n) with rolling arrays)
    public int minDistance2(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();
        int[][] dp = new int[m + 1][n + 1];

        // Fill base cases
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i; // Deleting all characters from word1
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j; // Inserting all characters into word1
        }

        // Build the table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1]; // No operation
                } else {
                    dp[i][j] = 1 + Math.min(
                            dp[i - 1][j], // Delete
                            Math.min(dp[i][j - 1], // Insert
                                    dp[i - 1][j - 1]) // Replace
                    );
                }
            }
        }
        return dp[m][n];
    }
}
