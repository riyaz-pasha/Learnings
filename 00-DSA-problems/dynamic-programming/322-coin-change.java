import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

class CoinChangeI {

    // ✅ Time & Space Complexity
    // Time: O(amount × n), where n = number of coins
    // Space: O(amount)
    public int coinChange(int[] coins, int amount) {
        int max = amount + 1;
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, max); // Fill with a value larger than any possible answer
        dp[0] = 0; // Base case: 0 coins needed to make amount 0

        for (int i = 1; i <= amount; i++) {
            for (int coin : coins) {
                if (i - coin >= 0) {
                    dp[i] = Math.min(dp[i], dp[i - coin] + 1);
                }
            }
        }

        return dp[amount] == max ? -1 : dp[amount];
    }

    // Tracing the coinChange method with `coins = {1, 2, 5}` and `amount = 11`

    // 1. Initialization:
    // - `amount` = 11
    // - `max` = `amount + 1` = 12
    // - `dp` array is created with size `amount + 1` (i.e., 12 elements).
    // - `Arrays.fill(dp, max)`: All elements are initialized to 12.
    // `dp = [12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12]`
    // - `dp[0] = 0`: Base case set.
    // `dp = [0, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12]`

    // 2. Outer loop: `for (int i = 1; i <= amount; i++)`

    // --- `i = 1` (Calculating `dp[1]`) ---
    // - Inner loop: `for (int coin : coins)`
    // - `coin = 1`:
    // - `if (1 - 1 >= 0)` is true.
    // - `dp[1] = Math.min(dp[1], dp[1 - 1] + 1)`
    // - `dp[1] = Math.min(12, dp[0] + 1)`
    // - `dp[1] = Math.min(12, 0 + 1) = 1`
    // `dp = [0, **1**, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12]`
    // - `coin = 2`: `if (1 - 2 >= 0)` is false.
    // - `coin = 5`: `if (1 - 5 >= 0)` is false.
    // - `dp[1]` is now 1.

    // --- `i = 2` (Calculating `dp[2]`) ---
    // - Inner loop:
    // - `coin = 1`:
    // - `if (2 - 1 >= 0)` is true.
    // - `dp[2] = Math.min(dp[2], dp[2 - 1] + 1)`
    // - `dp[2] = Math.min(12, dp[1] + 1)`
    // - `dp[2] = Math.min(12, 1 + 1) = 2`
    // `dp = [0, 1, **2**, 12, 12, 12, 12, 12, 12, 12, 12, 12]`
    // - `coin = 2`:
    // - `if (2 - 2 >= 0)` is true.
    // - `dp[2] = Math.min(dp[2], dp[2 - 2] + 1)`
    // - `dp[2] = Math.min(2, dp[0] + 1)`
    // - `dp[2] = Math.min(2, 0 + 1) = 1` (Using one '2' coin is better than two '1'
    // coins)
    // `dp = [0, 1, **1**, 12, 12, 12, 12, 12, 12, 12, 12, 12]`
    // - `coin = 5`: `if (2 - 5 >= 0)` is false.
    // - `dp[2]` is now 1.

    // --- `i = 3` (Calculating `dp[3]`) ---
    // - Inner loop:
    // - `coin = 1`: `dp[3] = Math.min(12, dp[2] + 1) = Math.min(12, 1 + 1) = 2`
    // `dp = [0, 1, 1, **2**, 12, 12, 12, 12, 12, 12, 12, 12]`
    // - `coin = 2`: `dp[3] = Math.min(2, dp[1] + 1) = Math.min(2, 1 + 1) = 2` (No
    // change)
    // - `coin = 5`: `if (3 - 5 >= 0)` is false.
    // - `dp[3]` is now 2.

    // --- `i = 4` (Calculating `dp[4]`) ---
    // - Inner loop:
    // - `coin = 1`: `dp[4] = Math.min(12, dp[3] + 1) = Math.min(12, 2 + 1) = 3`
    // - `coin = 2`: `dp[4] = Math.min(3, dp[2] + 1) = Math.min(3, 1 + 1) = 2`
    // `dp = [0, 1, 1, 2, **2**, 12, 12, 12, 12, 12, 12, 12]`
    // - `coin = 5`: `if (4 - 5 >= 0)` is false.
    // - `dp[4]` is now 2.

    // --- `i = 5` (Calculating `dp[5]`) ---
    // - Inner loop:
    // - `coin = 1`: `dp[5] = Math.min(12, dp[4] + 1) = Math.min(12, 2 + 1) = 3`
    // - `coin = 2`: `dp[5] = Math.min(3, dp[3] + 1) = Math.min(3, 2 + 1) = 3`
    // - `coin = 5`: `dp[5] = Math.min(3, dp[0] + 1) = Math.min(3, 0 + 1) = 1`
    // `dp = [0, 1, 1, 2, 2, **1**, 12, 12, 12, 12, 12, 12]`
    // - `dp[5]` is now 1.

    // --- `i = 6` (Calculating `dp[6]`) ---
    // - Inner loop:
    // - `coin = 1`: `dp[6] = Math.min(12, dp[5] + 1) = Math.min(12, 1 + 1) = 2`
    // - `coin = 2`: `dp[6] = Math.min(2, dp[4] + 1) = Math.min(2, 2 + 1) = 2`
    // - `coin = 5`: `dp[6] = Math.min(2, dp[1] + 1) = Math.min(2, 1 + 1) = 2`
    // `dp = [0, 1, 1, 2, 2, 1, **2**, 12, 12, 12, 12, 12]`
    // - `dp[6]` is now 2.

    // --- `i = 7` (Calculating `dp[7]`) ---
    // - Inner loop:
    // - `coin = 1`: `dp[7] = Math.min(12, dp[6] + 1) = Math.min(12, 2 + 1) = 3`
    // - `coin = 2`: `dp[7] = Math.min(3, dp[5] + 1) = Math.min(3, 1 + 1) = 2`
    // - `coin = 5`: `dp[7] = Math.min(2, dp[2] + 1) = Math.min(2, 1 + 1) = 2`
    // `dp = [0, 1, 1, 2, 2, 1, 2, **2**, 12, 12, 12, 12]`
    // - `dp[7]` is now 2.

    // --- `i = 8` (Calculating `dp[8]`) ---
    // - Inner loop:
    // - `coin = 1`: `dp[8] = Math.min(12, dp[7] + 1) = Math.min(12, 2 + 1) = 3`
    // - `coin = 2`: `dp[8] = Math.min(3, dp[6] + 1) = Math.min(3, 2 + 1) = 3`
    // - `coin = 5`: `dp[8] = Math.min(3, dp[3] + 1) = Math.min(3, 2 + 1) = 3`
    // `dp = [0, 1, 1, 2, 2, 1, 2, 2, **3**, 12, 12, 12]`
    // - `dp[8]` is now 3.

    // --- `i = 9` (Calculating `dp[9]`) ---
    // - Inner loop:
    // - `coin = 1`: `dp[9] = Math.min(12, dp[8] + 1) = Math.min(12, 3 + 1) = 4`
    // - `coin = 2`: `dp[9] = Math.min(4, dp[7] + 1) = Math.min(4, 2 + 1) = 3`
    // - `coin = 5`: `dp[9] = Math.min(3, dp[4] + 1) = Math.min(3, 2 + 1) = 3`
    // `dp = [0, 1, 1, 2, 2, 1, 2, 2, 3, **3**, 12, 12]`
    // - `dp[9]` is now 3.

    // --- `i = 10` (Calculating `dp[10]`) ---
    // - Inner loop:
    // - `coin = 1`: `dp[10] = Math.min(12, dp[9] + 1) = Math.min(12, 3 + 1) = 4`
    // - `coin = 2`: `dp[10] = Math.min(4, dp[8] + 1) = Math.min(4, 3 + 1) = 4`
    // - `coin = 5`: `dp[10] = Math.min(4, dp[5] + 1) = Math.min(4, 1 + 1) = 2`
    // `dp = [0, 1, 1, 2, 2, 1, 2, 2, 3, 3, **2**, 12]`
    // - `dp[10]` is now 2.

    // --- `i = 11` (Calculating `dp[11]`) ---
    // - Inner loop:
    // - `coin = 1`: `dp[11] = Math.min(12, dp[10] + 1) = Math.min(12, 2 + 1) = 3`
    // - `coin = 2`: `dp[11] = Math.min(3, dp[9] + 1) = Math.min(3, 3 + 1) = 3`
    // - `coin = 5`: `dp[11] = Math.min(3, dp[6] + 1) = Math.min(3, 2 + 1) = 3`
    // `dp = [0, 1, 1, 2, 2, 1, 2, 2, 3, 3, 2, **3**]`
    // - `dp[11]` is now 3.

    // 3. Final Return Value:
    // - `dp[amount]` is `dp[11]`, which is 3.
    // - `dp[amount] == max` (i.e., `3 == 12`) is false.
    // - Therefore, the method returns `dp[amount]`, which is **3**.

    // Conclusion: The minimum number of coins to make amount 11 using {1, 2, 5} is
    // 3 (e.g., using two 5-dollar coins and one 1-dollar coin).

}

class CoinChangeBFS {

    // Time Complexity: O(amount × n), where n is number of coin types
    // Space Complexity: O(amount) for queue and visited set
    public int coinChange(int[] coins, int amount) {
        if (amount == 0)
            return 0;

        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        queue.offer(0); // Start from amount 0
        visited.add(0);
        int level = 0; // Represents number of coins used

        while (!queue.isEmpty()) {
            int size = queue.size();
            level++; // One more coin used
            for (int i = 0; i < size; i++) {
                int curr = queue.poll();
                for (int coin : coins) {
                    int next = curr + coin;
                    if (next == amount) {
                        return level; // Found shortest path
                    }
                    if (next < amount && !visited.contains(next)) {
                        visited.add(next);
                        queue.offer(next);
                    }
                }
            }
        }

        return -1; // No combination found
    }

}

class CoinChange2 {

    public int coinChange(int[] coins, int amount) {
        Map<Integer, Integer> memo = new HashMap<>();
        int result = dp(amount, coins, memo);
        return result == Integer.MAX_VALUE ? -1 : result;
    }

    private int dp(int amount, int[] coins, Map<Integer, Integer> memo) {
        // Base cases
        if (amount == 0)
            return 0;
        if (amount < 0)
            return Integer.MAX_VALUE;
        if (memo.containsKey(amount))
            return memo.get(amount);

        int min = Integer.MAX_VALUE;
        for (int coin : coins) {
            int res = dp(amount - coin, coins, memo);
            if (res != Integer.MAX_VALUE) {
                min = Math.min(min, res + 1); // add current coin
            }
        }

        memo.put(amount, min);
        return min;
    }

}
