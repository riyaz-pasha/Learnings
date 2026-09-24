import java.util.Arrays;

/*
 * You are given an integer array coins representing coins of different
 * denominations and an integer amount representing a total amount of money.
 * 
 * Return the fewest number of coins that you need to make up that amount. If
 * that amount of money cannot be made up by any combination of the coins,
 * return -1.
 * 
 * You may assume that you have an infinite number of each kind of coin.
 * 
 * Example 1:
 * Input: coins = [1,2,5], amount = 11
 * Output: 3
 * Explanation: 11 = 5 + 5 + 1
 * 
 * Example 2:
 * Input: coins = [2], amount = 3
 * Output: -1
 * 
 * Example 3:
 * Input: coins = [1], amount = 0
 * Output: 0
 */

class CoinChangeSolution {

    public int coinChange(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        int max = amount + 1;
        Arrays.fill(dp, max);
        dp[0] = 0;

        for (int i = 1; i <= amount; i++) {
            for (int coin : coins) {
                if (i >= coin) {
                    dp[i] = Math.min(dp[i], dp[i - coin] + 1);
                }
            }
        }

        return dp[amount] == max ? -1 : dp[amount];
    }

}

class CoinChange {

    // Solution 1: Dynamic Programming (Bottom-Up) - Most Efficient
    // Time Complexity: O(amount * n) where n is number of coins
    // Space Complexity: O(amount)
    public int coinChange(int[] coins, int amount) {
        if (amount == 0)
            return 0;

        // dp[i] represents minimum coins needed to make amount i
        int[] dp = new int[amount + 1];

        // Initialize with amount + 1 (impossible value)
        for (int i = 1; i <= amount; i++) {
            dp[i] = amount + 1;
        }

        dp[0] = 0; // Base case: 0 coins needed for amount 0

        // For each amount from 1 to target amount
        for (int i = 1; i <= amount; i++) {
            // Try each coin
            for (int coin : coins) {
                if (coin <= i) {
                    dp[i] = Math.min(dp[i], dp[i - coin] + 1);
                }
            }
        }

        return dp[amount] > amount ? -1 : dp[amount];
    }

    // Solution 2: Dynamic Programming (Top-Down with Memoization)
    // Time Complexity: O(amount * n) where n is number of coins
    // Space Complexity: O(amount) for memo array + O(amount) for recursion stack
    public int coinChangeTopDown(int[] coins, int amount) {
        if (amount == 0)
            return 0;
        int[] memo = new int[amount + 1];
        return coinChangeHelper(coins, amount, memo);
    }

    private int coinChangeHelper(int[] coins, int remaining, int[] memo) {
        if (remaining < 0)
            return -1;
        if (remaining == 0)
            return 0;
        if (memo[remaining] != 0)
            return memo[remaining];

        int minCoins = Integer.MAX_VALUE;

        for (int coin : coins) {
            int result = coinChangeHelper(coins, remaining - coin, memo);
            if (result >= 0 && result < minCoins) {
                minCoins = result + 1;
            }
        }

        memo[remaining] = (minCoins == Integer.MAX_VALUE) ? -1 : minCoins;
        return memo[remaining];
    }

    // Solution 3: BFS Approach (Alternative)
    // Time Complexity: O(amount * n) where n is number of coins
    // Space Complexity: O(amount)
    public int coinChangeBFS(int[] coins, int amount) {
        if (amount == 0)
            return 0;

        boolean[] visited = new boolean[amount + 1];
        java.util.Queue<Integer> queue = new java.util.LinkedList<>();
        queue.offer(0);
        visited[0] = true;
        int level = 0;

        while (!queue.isEmpty()) {
            int size = queue.size();
            level++;

            for (int i = 0; i < size; i++) {
                int current = queue.poll();

                for (int coin : coins) {
                    int next = current + coin;

                    if (next == amount)
                        return level;

                    if (next < amount && !visited[next]) {
                        visited[next] = true;
                        queue.offer(next);
                    }
                }
            }
        }

        return -1;
    }

    // Test cases
    public static void main(String[] args) {
        CoinChange solution = new CoinChange();

        // Test Case 1
        int[] coins1 = { 1, 2, 5 };
        int amount1 = 11;
        System.out.println("Example 1:");
        System.out.println("Input: coins = [1,2,5], amount = 11");
        System.out.println("Output: " + solution.coinChange(coins1, amount1));
        System.out.println("Expected: 3 (5 + 5 + 1)\n");

        // Test Case 2
        int[] coins2 = { 2 };
        int amount2 = 3;
        System.out.println("Example 2:");
        System.out.println("Input: coins = [2], amount = 3");
        System.out.println("Output: " + solution.coinChange(coins2, amount2));
        System.out.println("Expected: -1\n");

        // Test Case 3
        int[] coins3 = { 1 };
        int amount3 = 0;
        System.out.println("Example 3:");
        System.out.println("Input: coins = [1], amount = 0");
        System.out.println("Output: " + solution.coinChange(coins3, amount3));
        System.out.println("Expected: 0\n");

        // Additional Test Case
        int[] coins4 = { 1, 3, 4 };
        int amount4 = 6;
        System.out.println("Additional Test:");
        System.out.println("Input: coins = [1,3,4], amount = 6");
        System.out.println("Output: " + solution.coinChange(coins4, amount4));
        System.out.println("Expected: 2 (3 + 3)\n");

        // Test with Top-Down approach
        System.out.println("Testing Top-Down approach for Example 1:");
        System.out.println("Output: " + solution.coinChangeTopDown(coins1, amount1));

        // Test with BFS approach
        System.out.println("Testing BFS approach for Example 1:");
        System.out.println("Output: " + solution.coinChangeBFS(coins1, amount1));
    }
}

/*
 * COMPLEXITY ANALYSIS:
 * 
 * Solution 1: Bottom-Up DP (RECOMMENDED)
 * - Time Complexity: O(amount * n)
 * We iterate through all amounts from 1 to target amount
 * For each amount, we try all n coins
 * - Space Complexity: O(amount)
 * We use a DP array of size (amount + 1)
 * 
 * Solution 2: Top-Down DP with Memoization
 * - Time Complexity: O(amount * n)
 * Each unique subproblem is solved once and cached
 * For each amount, we try all n coins
 * - Space Complexity: O(amount) + O(amount)
 * O(amount) for memoization array
 * O(amount) for recursion call stack in worst case
 * 
 * Solution 3: BFS Approach
 * - Time Complexity: O(amount * n)
 * In worst case, we visit all amounts from 0 to target
 * For each amount, we try all n coins
 * - Space Complexity: O(amount)
 * Queue can contain up to O(amount) elements
 * Visited array is O(amount)
 * 
 * KEY INSIGHTS:
 * 1. This is a classic unbounded knapsack problem
 * 2. Bottom-up DP is usually preferred for better space efficiency (no
 * recursion stack)
 * 3. The DP state: dp[i] = minimum coins needed to make amount i
 * 4. Transition: dp[i] = min(dp[i], dp[i - coin] + 1) for all valid coins
 * 5. We initialize dp array with amount + 1 (impossible value) to distinguish
 * from valid solutions
 */
