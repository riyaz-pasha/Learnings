/*
 * You are given an integer array coins representing coins of different
 * denominations and an integer amount representing a total amount of money.
 * 
 * Return the number of combinations that make up that amount. If that amount of
 * money cannot be made up by any combination of the coins, return 0.
 * 
 * You may assume that you have an infinite number of each kind of coin.
 * 
 * The answer is guaranteed to fit into a signed 32-bit integer.
 * 
 * Example 1:
 * Input: amount = 5, coins = [1,2,5]
 * Output: 4
 * Explanation: there are four ways to make up the amount:
 * 5=5
 * 5=2+2+1
 * 5=2+1+1+1
 * 5=1+1+1+1+1
 * 
 * Example 2:
 * Input: amount = 3, coins = [2]
 * Output: 0
 * Explanation: the amount of 3 cannot be made up just with coins of 2.
 * 
 * Example 3:
 * Input: amount = 10, coins = [10]
 * Output: 1
 */

class CoinChangeIISolution {

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

}

class CoinChangeII {

    // Solution 1: 1D Dynamic Programming (Most Efficient)
    // Time Complexity: O(amount * n) where n is number of coins
    // Space Complexity: O(amount)
    public int change(int amount, int[] coins) {
        // dp[i] represents number of ways to make amount i
        int[] dp = new int[amount + 1];
        dp[0] = 1; // One way to make 0: use no coins

        // CRITICAL: Iterate coins in outer loop to avoid counting permutations
        // This ensures we only count combinations, not permutations
        for (int coin : coins) {
            for (int i = coin; i <= amount; i++) {
                dp[i] += dp[i - coin];
            }
        }

        return dp[amount];
    }

    // Solution 2: 2D Dynamic Programming (More Intuitive)
    // Time Complexity: O(amount * n) where n is number of coins
    // Space Complexity: O(amount * n)
    public int change2D(int amount, int[] coins) {
        int n = coins.length;
        // dp[i][j] = ways to make amount j using first i coins
        int[][] dp = new int[n + 1][amount + 1];

        // Base case: one way to make amount 0 (use no coins)
        for (int i = 0; i <= n; i++) {
            dp[i][0] = 1;
        }

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= amount; j++) {
                // Don't use current coin
                dp[i][j] = dp[i - 1][j];

                // Use current coin (can use it multiple times)
                if (j >= coins[i - 1]) {
                    dp[i][j] += dp[i][j - coins[i - 1]];
                }
            }
        }

        return dp[n][amount];
    }

    // Solution 3: 2D DP with Space Optimization (Two Rows)
    // Time Complexity: O(amount * n)
    // Space Complexity: O(amount)
    public int changeTwoRows(int amount, int[] coins) {
        int[] prev = new int[amount + 1];
        int[] curr = new int[amount + 1];
        prev[0] = 1;

        for (int coin : coins) {
            curr[0] = 1;
            for (int j = 1; j <= amount; j++) {
                // Don't use current coin
                curr[j] = prev[j];

                // Use current coin
                if (j >= coin) {
                    curr[j] += curr[j - coin];
                }
            }
            // Swap arrays
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }

        return prev[amount];
    }

    // Solution 4: Recursion with Memoization (Top-Down)
    // Time Complexity: O(amount * n)
    // Space Complexity: O(amount * n) for memo + O(n) recursion stack
    public int changeRecursive(int amount, int[] coins) {
        Integer[][] memo = new Integer[coins.length][amount + 1];
        return helper(coins, 0, amount, memo);
    }

    private int helper(int[] coins, int index, int remaining, Integer[][] memo) {
        // Base cases
        if (remaining == 0)
            return 1;
        if (remaining < 0 || index >= coins.length)
            return 0;

        if (memo[index][remaining] != null) {
            return memo[index][remaining];
        }

        // Use current coin (stay at same index - infinite supply)
        int use = helper(coins, index, remaining - coins[index], memo);

        // Skip current coin (move to next coin)
        int skip = helper(coins, index + 1, remaining, memo);

        memo[index][remaining] = use + skip;
        return memo[index][remaining];
    }

    // Solution 5: Pure Recursion (Brute Force - for understanding)
    // Time Complexity: O(2^amount) - exponential
    // Space Complexity: O(amount) for recursion stack
    public int changeBruteForce(int amount, int[] coins) {
        return bruteForceDFS(coins, 0, amount);
    }

    private int bruteForceDFS(int[] coins, int index, int remaining) {
        if (remaining == 0)
            return 1;
        if (remaining < 0 || index >= coins.length)
            return 0;

        // Use current coin
        int use = bruteForceDFS(coins, index, remaining - coins[index]);

        // Skip current coin
        int skip = bruteForceDFS(coins, index + 1, remaining);

        return use + skip;
    }

    // Helper method to demonstrate the difference between combinations and
    // permutations
    public void demonstrateCombinationsVsPermutations(int amount, int[] coins) {
        System.out.println("\n=== COMBINATIONS vs PERMUTATIONS Demo ===");
        System.out.println("Amount: " + amount + ", Coins: " + java.util.Arrays.toString(coins));

        // Count combinations (correct approach)
        int combinations = change(amount, coins);
        System.out.println("\nCombinations (correct): " + combinations);

        // Count permutations (wrong approach - coins in inner loop)
        int[] dpWrong = new int[amount + 1];
        dpWrong[0] = 1;
        for (int i = 1; i <= amount; i++) {
            for (int coin : coins) {
                if (i >= coin) {
                    dpWrong[i] += dpWrong[i - coin];
                }
            }
        }
        System.out.println("Permutations (wrong approach): " + dpWrong[amount]);
        System.out.println("\nNote: Permutations count [1,2] and [2,1] as different,");
        System.out.println("while combinations count them as the same.");
    }

    // Test cases
    public static void main(String[] args) {
        CoinChangeII solution = new CoinChangeII();

        // Test Case 1
        int amount1 = 5;
        int[] coins1 = { 1, 2, 5 };
        System.out.println("Example 1:");
        System.out.println("Input: amount = 5, coins = [1,2,5]");
        System.out.println("Output (1D DP): " + solution.change(amount1, coins1));
        System.out.println("Output (2D DP): " + solution.change2D(amount1, coins1));
        System.out.println("Output (Recursive): " + solution.changeRecursive(amount1, coins1));
        System.out.println("Expected: 4");
        System.out.println("Combinations: 5=5, 5=2+2+1, 5=2+1+1+1, 5=1+1+1+1+1\n");

        // Test Case 2
        int amount2 = 3;
        int[] coins2 = { 2 };
        System.out.println("Example 2:");
        System.out.println("Input: amount = 3, coins = [2]");
        System.out.println("Output: " + solution.change(amount2, coins2));
        System.out.println("Expected: 0\n");

        // Test Case 3
        int amount3 = 10;
        int[] coins3 = { 10 };
        System.out.println("Example 3:");
        System.out.println("Input: amount = 10, coins = [10]");
        System.out.println("Output: " + solution.change(amount3, coins3));
        System.out.println("Expected: 1\n");

        // Additional Test Cases
        int amount4 = 0;
        int[] coins4 = { 1, 2, 5 };
        System.out.println("Edge Case (amount = 0):");
        System.out.println("Input: amount = 0, coins = [1,2,5]");
        System.out.println("Output: " + solution.change(amount4, coins4));
        System.out.println("Expected: 1 (one way: use no coins)\n");

        int amount5 = 4;
        int[] coins5 = { 1, 2, 3 };
        System.out.println("Additional Test:");
        System.out.println("Input: amount = 4, coins = [1,2,3]");
        System.out.println("Output: " + solution.change(amount5, coins5));
        System.out.println("Combinations: 4=3+1, 4=2+2, 4=2+1+1, 4=1+1+1+1");
        System.out.println("Expected: 4\n");

        // Demonstrate combinations vs permutations
        solution.demonstrateCombinationsVsPermutations(3, new int[] { 1, 2 });

        // Performance comparison
        System.out.println("\n=== Performance Comparison ===");
        int largeAmount = 500;
        int[] largeCoins = { 1, 2, 5, 10, 20, 50, 100 };

        long start = System.nanoTime();
        int result1 = solution.change(largeAmount, largeCoins);
        long end = System.nanoTime();
        System.out.println("1D DP: " + result1 + " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result2 = solution.change2D(largeAmount, largeCoins);
        end = System.nanoTime();
        System.out.println("2D DP: " + result2 + " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result3 = solution.changeRecursive(largeAmount, largeCoins);
        end = System.nanoTime();
        System.out.println("Recursive: " + result3 + " (Time: " + (end - start) / 1000 + " μs)");
    }
}

/*
 * COMPLEXITY ANALYSIS:
 * 
 * Solution 1: 1D Dynamic Programming (RECOMMENDED)
 * - Time Complexity: O(amount * n)
 * n = number of different coins
 * We iterate through each coin and for each coin, through all amounts
 * - Space Complexity: O(amount)
 * Single 1D array of size (amount + 1)
 * 
 * Solution 2: 2D Dynamic Programming
 * - Time Complexity: O(amount * n)
 * - Space Complexity: O(amount * n)
 * 2D table of size (n+1) × (amount+1)
 * More intuitive but uses more space
 * 
 * Solution 3: Space Optimized 2D DP
 * - Time Complexity: O(amount * n)
 * - Space Complexity: O(amount)
 * Uses only two rows instead of full table
 * 
 * Solution 4: Recursion with Memoization
 * - Time Complexity: O(amount * n)
 * Each subproblem solved once and cached
 * - Space Complexity: O(amount * n) + O(n)
 * Memoization table + recursion stack
 * 
 * Solution 5: Pure Recursion (Brute Force)
 * - Time Complexity: O(2^amount)
 * Exponential - explores all possibilities
 * - Space Complexity: O(amount)
 * Recursion stack depth
 * 
 * KEY INSIGHTS:
 * 
 * 1. CRITICAL DIFFERENCE from Coin Change I:
 * - Coin Change I: Find MINIMUM coins (use min)
 * - Coin Change II: Count COMBINATIONS (use sum)
 * 
 * 2. Why Outer Loop Must Be Coins:
 * - Coins outer loop → Combinations: [1,2] counted once
 * - Amount outer loop → Permutations: [1,2] and [2,1] counted separately
 * 
 * Example with amount=3, coins=[1,2]:
 * Correct (combinations): {1+1+1, 1+2} = 2 ways
 * Wrong (permutations): {1+1+1, 1+2, 2+1} = 3 ways
 * 
 * 3. DP State Transition:
 * dp[i] += dp[i - coin]
 * - Current ways to make i += ways to make (i - coin)
 * - We add because we're counting combinations
 * 
 * 4. Unbounded Knapsack Pattern:
 * - Each coin can be used unlimited times
 * - When using coin, stay at same coin index (don't move to next)
 * - Different from 0/1 knapsack where each item used once
 * 
 * 5. Base Case:
 * - dp[0] = 1 (one way to make 0: use no coins)
 * - All other values start at 0
 * 
 * 6. Why Solution 1 is Best:
 * - Minimal space usage (O(amount))
 * - Clean and efficient code
 * - Easy to understand once you grasp the pattern
 */
