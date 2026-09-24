import java.util.Arrays;

class UncrossedLines {

    public int maxUncrossedLines(int[] nums1, int[] nums2) {
        int m = nums1.length, n = nums2.length;
        int[][] memo = new int[m][n];
        for (int i = 0; i < m; i++) {
            Arrays.fill(memo[i], -1);
        }
        return helper(nums1, nums2, 0, 0, memo);
    }

    private int helper(int[] nums1, int[] nums2, int i, int j, int[][] memo) {
        if (i >= nums1.length || j >= nums2.length) {
            return 0;
        }
        if (memo[i][j] != -1) {
            return memo[i][j];
        }

        if (nums1[i] == nums2[j]) {
            memo[i][j] = 1 + helper(nums1, nums2, i + 1, j + 1, memo);
        } else {
            memo[i][j] = Math.max(
                    helper(nums1, nums2, i + 1, j, memo),
                    helper(nums1, nums2, i, j + 1, memo));
        }
        return memo[i][j];
    }

}

class UncrossedLines2 {

    // Time: O(m × n)
    // Space: O(m × n)
    public int maxUncrossedLines(int[] nums1, int[] nums2) {
        int m = nums1.length, n = nums2.length;

        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (nums1[i - 1] == nums2[j - 1]) {
                    // Match found, extend the line (like LCS)
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    // Skip from either array and take the best
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[m][n];
    }

}
