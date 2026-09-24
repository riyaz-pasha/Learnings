
class OnesAndZeros {

    public int findMaxForm(String[] strs, int m, int n) {
        // Initialize 3D memo array: index x m x n
        int[][][] memo = new int[strs.length][m + 1][n + 1];
        for (int i = 0; i < strs.length; i++) {
            for (int j = 0; j <= m; j++) {
                for (int k = 0; k <= n; k++) {
                    memo[i][j][k] = -1;
                }
            }
        }
        return helper(strs, m, n, 0, memo);
    }

    private int helper(String[] strs, int m, int n, int idx, int[][][] memo) {
        if (idx >= strs.length)
            return 0;

        if (memo[idx][m][n] != -1) {
            return memo[idx][m][n];
        }

        int count0 = 0, count1 = 0;
        for (char ch : strs[idx].toCharArray()) {
            if (ch == '0')
                count0++;
            else
                count1++;
        }

        // Skip current string
        int skip = helper(strs, m, n, idx + 1, memo);

        // Pick current string if within limits
        int pick = 0;
        if (count0 <= m && count1 <= n) {
            pick = 1 + helper(strs, m - count0, n - count1, idx + 1, memo);
        }

        return memo[idx][m][n] = Math.max(pick, skip);
    }

}

class OnesAndZeros3 {

    // Time Complexity: O(L × m × n), where L is the number of strings
    // Space Complexity: O(m × n)
    public int findMaxForm(String[] strs, int m, int n) {
        int[][] dp = new int[m + 1][n + 1];
        for (String str : strs) {
            int zeros = 0, ones = 0;
            for (char ch : str.toCharArray()) {
                if (ch == '0')
                    zeros++;
                else
                    ones++;
            }
            for (int i = m; i >= zeros; i--) {
                for (int j = n; j >= ones; j--) {
                    dp[i][j] = Math.max(dp[i][j], 1 + dp[i - zeros][j - ones]);
                }
            }
        }
        return dp[m][n];
    }

        // Tracing with strs = {"10", "0001", "111001", "1", "0"}, m = 3, n = 3:

        // Initial dp array (all zeros):
        // dp = {
        //   {0, 0, 0, 0},  // m=0
        //   {0, 0, 0, 0},  // m=1
        //   {0, 0, 0, 0},  // m=2
        //   {0, 0, 0, 0}   // m=3
        // }
        // (rows represent 0s (i), columns represent 1s (j))


        // Process string "10": zeros = 1, ones = 1
        //   i loop from m (3) down to zeros (1):
        //     i = 3:
        //       j loop from n (3) down to ones (1):
        //         j = 3: dp[3][3] = Math.max(dp[3][3], 1 + dp[3-1][3-1]) = Math.max(0, 1 + dp[2][2]) = 1 (since dp[2][2] is 0)
        //         j = 2: dp[3][2] = Math.max(dp[3][2], 1 + dp[2][1]) = 1
        //         j = 1: dp[3][1] = Math.max(dp[3][1], 1 + dp[2][0]) = 1
        //     i = 2:
        //       j loop from n (3) down to ones (1):
        //         j = 3: dp[2][3] = Math.max(dp[2][3], 1 + dp[1][2]) = 1
        //         j = 2: dp[2][2] = Math.max(dp[2][2], 1 + dp[1][1]) = 1
        //         j = 1: dp[2][1] = Math.max(dp[2][1], 1 + dp[1][0]) = 1
        //     i = 1:
        //       j loop from n (3) down to ones (1):
        //         j = 3: dp[1][3] = Math.max(dp[1][3], 1 + dp[0][2]) = 1
        //         j = 2: dp[1][2] = Math.max(dp[1][2], 1 + dp[0][1]) = 1
        //         j = 1: dp[1][1] = Math.max(dp[1][1], 1 + dp[0][0]) = 1
        // dp array after "10":
        // {
        //   {0, 0, 0, 0},
        //   {0, 1, 1, 1},
        //   {0, 1, 1, 1},
        //   {0, 1, 1, 1}
        // }


        // Process string "0001": zeros = 3, ones = 1
        //   i loop from m (3) down to zeros (3):
        //     i = 3:
        //       j loop from n (3) down to ones (1):
        //         j = 3: dp[3][3] = Math.max(dp[3][3], 1 + dp[3-3][3-1]) = Math.max(1, 1 + dp[0][2]) = Math.max(1, 1 + 0) = 1
        //         j = 2: dp[3][2] = Math.max(dp[3][2], 1 + dp[0][1]) = Math.max(1, 1 + 0) = 1
        //         j = 1: dp[3][1] = Math.max(dp[3][1], 1 + dp[0][0]) = Math.max(1, 1 + 0) = 1
        // dp array after "0001" (no change as no new combinations):
        // {
        //   {0, 0, 0, 0},
        //   {0, 1, 1, 1},
        //   {0, 1, 1, 1},
        //   {0, 1, 1, 1}
        // }


        // Process string "111001": zeros = 2, ones = 4 (This string needs 2 zeros and 4 ones. Since n=3, it cannot be formed)
        //   i loop from m (3) down to zeros (2):
        //     i = 3:
        //       j loop from n (3) down to ones (4): Loop does not run as j (max 3) cannot be >= ones (4).
        //     i = 2:
        //       j loop from n (3) down to ones (4): Loop does not run.
        // dp array after "111001" (no change):
        // {
        //   {0, 0, 0, 0},
        //   {0, 1, 1, 1},
        //   {0, 1, 1, 1},
        //   {0, 1, 1, 1}
        // }


        // Process string "1": zeros = 0, ones = 1
        //   i loop from m (3) down to zeros (0):
        //     i = 3:
        //       j loop from n (3) down to ones (1):
        //         j = 3: dp[3][3] = Math.max(dp[3][3], 1 + dp[3-0][3-1]) = Math.max(1, 1 + dp[3][2]) = Math.max(1, 1 + 1) = 2 (using "10" and "1")
        //         j = 2: dp[3][2] = Math.max(dp[3][2], 1 + dp[3][1]) = Math.max(1, 1 + 1) = 2
        //         j = 1: dp[3][1] = Math.max(dp[3][1], 1 + dp[3][0]) = Math.max(1, 1 + 0) = 1 (dp[3][0] is 0)
        //     i = 2:
        //       j = 3: dp[2][3] = Math.max(dp[2][3], 1 + dp[2][2]) = Math.max(1, 1 + 1) = 2
        //       j = 2: dp[2][2] = Math.max(dp[2][2], 1 + dp[2][1]) = Math.max(1, 1 + 1) = 2
        //       j = 1: dp[2][1] = Math.max(dp[2][1], 1 + dp[2][0]) = Math.max(1, 1 + 0) = 1
        //     i = 1:
        //       j = 3: dp[1][3] = Math.max(dp[1][3], 1 + dp[1][2]) = Math.max(1, 1 + 1) = 2
        //       j = 2: dp[1][2] = Math.max(dp[1][2], 1 + dp[1][1]) = Math.max(1, 1 + 1) = 2
        //       j = 1: dp[1][1] = Math.max(dp[1][1], 1 + dp[1][0]) = Math.max(1, 1 + 0) = 1
        //     i = 0:
        //       j = 3: dp[0][3] = Math.max(dp[0][3], 1 + dp[0][2]) = Math.max(0, 1 + 0) = 1
        //       j = 2: dp[0][2] = Math.max(dp[0][2], 1 + dp[0][1]) = Math.max(0, 1 + 0) = 1
        //       j = 1: dp[0][1] = Math.max(dp[0][1], 1 + dp[0][0]) = Math.max(0, 1 + 0) = 1
        // dp array after "1":
        // {
        //   {0, 1, 1, 1},
        //   {0, 1, 2, 2},
        //   {0, 1, 2, 2},
        //   {0, 1, 2, 2}
        // }


        // Process string "0": zeros = 1, ones = 0
        //   i loop from m (3) down to zeros (1):
        //     i = 3:
        //       j loop from n (3) down to ones (0):
        //         j = 3: dp[3][3] = Math.max(dp[3][3], 1 + dp[3-1][3-0]) = Math.max(2, 1 + dp[2][3]) = Math.max(2, 1 + 2) = 3 (using "10", "1", "0")
        //         j = 2: dp[3][2] = Math.max(dp[3][2], 1 + dp[2][2]) = Math.max(2, 1 + 2) = 3
        //         j = 1: dp[3][1] = Math.max(dp[3][1], 1 + dp[2][1]) = Math.max(2, 1 + 1) = 2
        //         j = 0: dp[3][0] = Math.max(dp[3][0], 1 + dp[2][0]) = Math.max(0, 1 + 0) = 1
        //     i = 2:
        //       j = 3: dp[2][3] = Math.max(dp[2][3], 1 + dp[1][3]) = Math.max(2, 1 + 2) = 3
        //       j = 2: dp[2][2] = Math.max(dp[2][2], 1 + dp[1][2]) = Math.max(2, 1 + 2) = 3
        //       j = 1: dp[2][1] = Math.max(dp[2][1], 1 + dp[1][1]) = Math.max(1, 1 + 1) = 2
        //       j = 0: dp[2][0] = Math.max(dp[2][0], 1 + dp[1][0]) = Math.max(0, 1 + 0) = 1
        //     i = 1:
        //       j = 3: dp[1][3] = Math.max(dp[1][3], 1 + dp[0][3]) = Math.max(2, 1 + 1) = 2
        //       j = 2: dp[1][2] = Math.max(dp[1][2], 1 + dp[0][2]) = Math.max(2, 1 + 1) = 2
        //       j = 1: dp[1][1] = Math.max(dp[1][1], 1 + dp[0][1]) = Math.max(1, 1 + 1) = 2
        //       j = 0: dp[1][0] = Math.max(dp[1][0], 1 + dp[0][0]) = Math.max(0, 1 + 0) = 1
        // dp array after "0":
        // {
        //   {0, 1, 1, 1},
        //   {1, 2, 2, 2},
        //   {1, 2, 3, 3},
        //   {1, 2, 3, 3}
        // }

        // Final result: dp[m][n] = dp[3][3] = 3

}
