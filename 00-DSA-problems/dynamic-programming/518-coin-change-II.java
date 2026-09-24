import java.util.Arrays;

class CoinChangeWays {

    public int change(int amount, int[] coins) {
        int[][] memo = new int[amount + 1][coins.length + 1];
        for (int[] row : memo) {
            Arrays.fill(row, -1);
        }
        return helper(amount, coins, 0, memo);
    }

    private int helper(int amount, int[] coins, int index, int[][] memo) {
        if (amount == 0) {
            return 1; // Found one valid combination
        }
        if (amount < 0 || index == coins.length) {
            return 0; // Invalid combination
        }
        if (memo[amount][index] != -1) {
            return memo[amount][index];
        }

        // Option 1: Don't use the current coin at index
        int waysWithoutCurrent = helper(amount, coins, index + 1, memo);

        // Option 2: Use the current coin at index (if possible)
        int waysWithCurrent = 0;
        if (amount >= coins[index]) {
            waysWithCurrent = helper(amount - coins[index], coins, index, memo);
        }

        return memo[amount][index] = waysWithoutCurrent + waysWithCurrent;
    }

}

class CoinChangeWays2 {

    // Time Complexity: O(amount × coins.length)
    // Space Complexity: O(amount)
    public int change(int amount, int[] coins) {
        int[] dp = new int[amount + 1];
        dp[0] = 1;

        for (int coin : coins) {
            for (int i = coin; i <= amount; i++) {
                dp[i] += dp[i - coin];
            }
        }
        return dp[amount];
    }

    // Tracing for amount = 5, coins = {1, 2, 5}:

    // Initial dp array: [1, 0, 0, 0, 0, 0]

    // Iterating through coins:

    // coin = 1:
    // i = 1: dp[1] += dp[1 - 1] = dp[0] = 1. dp[1] = 1.
    // (Combinations for amount 1: {1})
    // i = 2: dp[2] += dp[2 - 1] = dp[1] = 1. dp[2] = 1.
    // (Combinations for amount 2 using only coin 1: {1,1})
    // i = 3: dp[3] += dp[3 - 1] = dp[2] = 1. dp[3] = 1.
    // (Combinations for amount 3 using only coin 1: {1,1,1})
    // i = 4: dp[4] += dp[4 - 1] = dp[3] = 1. dp[4] = 1.
    // (Combinations for amount 4 using only coin 1: {1,1,1,1})
    // i = 5: dp[5] += dp[5 - 1] = dp[4] = 1. dp[5] = 1.
    // (Combinations for amount 5 using only coin 1: {1,1,1,1,1})
    // dp array after coin 1: [1, 1, 1, 1, 1, 1]

    // coin = 2:
    // i = 2: dp[2] += dp[2 - 2] = dp[0] = 1. dp[2] = 1 (old value) + 1 = 2.
    // (Combinations for amount 2: {1,1}, {2})
    // i = 3: dp[3] += dp[3 - 2] = dp[1] = 1. dp[3] = 1 (old value) + 1 = 2.
    // (Combinations for amount 3: {1,1,1}, {1,2})
    // i = 4: dp[4] += dp[4 - 2] = dp[2] = 2. dp[4] = 1 (old value) + 2 = 3.
    // (Combinations for amount 4: {1,1,1,1}, {1,1,2}, {2,2})
    // i = 5: dp[5] += dp[5 - 2] = dp[3] = 2. dp[5] = 1 (old value) + 2 = 3.
    // (Combinations for amount 5: {1,1,1,1,1}, {1,1,1,2}, {1,2,2})
    // dp array after coin 2: [1, 1, 2, 2, 3, 3]

    // coin = 5:
    // i = 5: dp[5] += dp[5 - 5] = dp[0] = 1. dp[5] = 3 (old value) + 1 = 4.
    // (Combinations for amount 5: {1,1,1,1,1}, {1,1,1,2}, {1,2,2}, {5})
    // dp array after coin 5: [1, 1, 2, 2, 3, 4]

}
