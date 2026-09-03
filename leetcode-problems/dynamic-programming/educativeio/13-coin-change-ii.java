import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Coin Change II
 * Given an integer 'amount' and an array of 'coins' (denominations), return 
 * the number of DISTINCT COMBINATIONS that sum exactly to the amount.
 * You can use each coin infinitely. The order of coins does not matter 
 * (i.e., [1, 2] is the same combination as [2, 1]).
 * 
 * Constraints:
 * 1 <= coins.length <= 300
 * 1 <= coins[i] <= 5000
 * All coins are unique.
 * 0 <= amount <= 5000
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * This is the combination-counting cousin of the "Coin Change" (minimum coins) 
 * problem. In an L4/L5 interview, call out the constraints and the difference:
 * 
 * Q: "Can the amount be 0?"
 * A: Yes. If the amount is 0, there is exactly 1 way to make it: by picking no coins.
 * 
 * Q: "Will the answer fit in a standard 32-bit integer?"
 * A: The problem usually guarantees the answer fits into a signed 32-bit integer. 
 *    If not, we would need to use `long`. (It's always good to ask this to show 
 *    senior-level system awareness).
 * 
 * CRITICAL SENIOR INSIGHT: Combinations vs. Permutations
 * "The prompt explicitly states the order of coins doesn't matter. This means 
 * we must structure our algorithm to only process coins in a fixed sequence. 
 * If we evaluate Coin 1, and then Coin 2, we must never go back and evaluate 
 * Coin 1 again. This naturally forces 'combinations' instead of 'permutations'."
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "At any given coin type 'i', I have two parallel paths to take to reach the amount:
 *  1. EXCLUDE the current coin type entirely. How many ways can I make the amount 
 *     using only the remaining coin types?
 *  2. INCLUDE the current coin type. How many ways can I make the reduced amount 
 *     (amount - coin_value)? Since I can use this coin infinitely, I stay on 
 *     the exact same coin type for the next decision.
 * 
 * The total number of valid combinations is the sum of the ways from both paths.
 * Since multiple paths will reach the same remaining amount using the same remaining 
 * coins, we have overlapping subproblems -> Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: coins = [1, 2, 5], amount = 5
 * 
 * Let's trace the 1D Space-Optimized Array. 
 * dp[j] means "Total combinations to make amount j".
 * Initially: dp[0] = 1 (empty set), all others 0.
 * 
 * Using coin 1:
 * dp[1] += dp[0] -> 1
 * dp[2] += dp[1] -> 1
 * dp[3] += dp[2] -> 1
 * dp[4] += dp[3] -> 1
 * dp[5] += dp[4] -> 1
 * (We can make every amount in exactly 1 way using only 1s).
 * 
 * Using coin 2:
 * dp[2] += dp[0] -> 1 + 1 = 2 (Ways: [1,1] and [2])
 * dp[3] += dp[1] -> 1 + 1 = 2 (Ways: [1,1,1] and [1,2])
 * dp[4] += dp[2] -> 1 + 2 = 3 (Ways: [1,1,1,1], [1,1,2], [2,2])
 * dp[5] += dp[3] -> 1 + 2 = 3 (Ways: [1,1,1,1,1], [1,1,1,2], [1,2,2])
 * 
 * Using coin 5:
 * dp[5] += dp[0] -> 3 + 1 = 4 (Ways: [Previous 3 ways] + [5])
 * 
 * Final Answer: 4.
 */
public class CoinChangeII {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Recursively traverse the coin array, summing up the successful 
     * combinations from the 'include' and 'exclude' branches.
     * 
     * Time Complexity: O(2^(amount/min_coin)) - Exponential branching.
     * Space Complexity: O(amount/min_coin) - Call stack depth.
     */
    public int changeRecursive(int amount, int[] coins) {
        if (coins == null || coins.length == 0) return 0;
        return solveRecursive(coins, coins.length - 1, amount);
    }

    private int solveRecursive(int[] coins, int index, int currentAmount) {
        // BASE CASE REASONING:
        // If the amount hits exactly 0, the specific path of coins we took to 
        // get here forms a perfectly valid combination. We return 1 to count 
        // this successful path.
        if (currentAmount == 0) {
            return 1;
        }

        // BASE CASE REASONING:
        // 1. If currentAmount < 0: The last coin we added pushed us over the target. 
        //    This combination is a failure. Return 0.
        // 2. If index < 0: We ran out of coin types to check, but we still haven't 
        //    reached our target amount. This is a failure. Return 0.
        if (currentAmount < 0 || index < 0) {
            return 0;
        }

        // Choice 1: Exclude the coin. Move to the next coin (index - 1).
        int exclude = solveRecursive(coins, index - 1, currentAmount);

        // Choice 2: Include the coin. Keep the same coin (index), reduce amount.
        int include = solveRecursive(coins, index, currentAmount - coins[index]);

        // The total combinations is the sum of both universes.
        return exclude + include;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache states of [Current Coin Index][Remaining Amount] so we 
     * don't re-calculate the number of combinations for known states.
     * 
     * Time Complexity: O(n * amount)
     * Space Complexity: O(n * amount)
     */
    public int changeMemo(int amount, int[] coins) {
        if (coins == null || coins.length == 0) return 0;
        int n = coins.length;

        // Use Integer object to allow null (uncalculated) vs 0 (calculated as 0 ways)
        Integer[][] memo = new Integer[n][amount + 1];

        return solveMemo(coins, n - 1, amount, memo);
    }

    private int solveMemo(int[] coins, int index, int currentAmount, Integer[][] memo) {
        // BASE CASES (same physical logic as brute force)
        if (currentAmount == 0) return 1;
        if (currentAmount < 0 || index < 0) return 0;

        if (memo[index][currentAmount] != null) {
            return memo[index][currentAmount];
        }

        int exclude = solveMemo(coins, index - 1, currentAmount, memo);
        int include = solveMemo(coins, index, currentAmount - coins[index], memo);

        memo[index][currentAmount] = exclude + include;
        return memo[index][currentAmount];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Build a 2D spreadsheet. dp[i][j] answers: "How many distinct 
     * combinations sum to amount 'j' using only a subset of the first 'i' coins?"
     * 
     * Time Complexity: O(n * amount)
     * Space Complexity: O(n * amount)
     */
    public int changeTabulation(int amount, int[] coins) {
        int n = coins.length;
        if (n == 0) return 0;

        int[][] dp = new int[n + 1][amount + 1];

        // BASE CASE REASONING:
        // dp[i][0] represents: "How many ways can I make an amount of 0?"
        // No matter how many coins (i) we are allowed to use, there is always 
        // exactly ONE way to make an amount of 0: by simply picking an empty set.
        for (int i = 0; i <= n; i++) {
            dp[i][0] = 1;
        }

        // We iterate through every coin type we have available (i)
        for (int i = 1; i <= n; i++) {
            
            // This is the physical value of the coin we are holding in our hands.
            int currentCoin = coins[i - 1];

            // We try to construct every possible amount from 1 up to our target (j)
            for (int j = 1; j <= amount; j++) {
                
                // PHYSICAL CHECK: Is this coin small enough to fit inside our target amount 'j'?
                if (currentCoin <= j) {
                    
                    // YES, it fits! We can reach this amount via two parallel universes:

                    // UNIVERSE 1 (Exclude): I completely ignore this coin.
                    // How many combinations successfully made amount 'j' using ONLY 
                    // the previous coin types? 
                    // (We look directly UP one row in our DP spreadsheet).
                    int combinationsByExcluding = dp[i - 1][j];

                    // UNIVERSE 2 (Include): I force 1 of these coins into my combination.
                    // This eats up 'currentCoin' amount of my target.
                    // Because I am allowed to use this coin infinitely, I DO NOT look up.
                    // I look left on the EXACT SAME ROW.
                    // "How many combinations successfully made the SMALLER remaining amount 
                    // (j - currentCoin) while still allowing the use of this exact same coin?"
                    int combinationsByIncluding = dp[i][j - currentCoin];

                    // The total valid combinations is the sum of both universes.
                    dp[i][j] = combinationsByExcluding + combinationsByIncluding;

                } else {
                    // NO, it doesn't fit. The coin is strictly heavier than our target amount 'j'.
                    // Our ONLY option is to exclude it. The total number of combinations 
                    // is strictly equal to however many combinations we had using the previous coins.
                    // (We just copy the value from directly above).
                    dp[i][j] = dp[i - 1][j];
                }
            }
        }

        return dp[n][amount];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (1D Array - L4/L5 Target)
     * ========================================================================
     * Idea: In Tabulation, calculating row 'i' only requires reading from the 
     * row directly above it (i-1) and the values to its left in the same row.
     * We can collapse the 2D grid into a 1D array representing just the amounts.
     * 
     * Since this is the Unbounded Knapsack (infinite reuse), we traverse the 
     * amounts FORWARDS. This guarantees that when we look back at `dp[j - currentCoin]`, 
     * that smaller amount has ALREADY been updated with combinations that use 
     * the current coin.
     * 
     * Time Complexity: O(n * amount)
     * Space Complexity: O(amount) - Massively reduced memory footprint.
     */
    public int changeSpaceOptimized(int amount, int[] coins) {
        if (coins == null || coins.length == 0) return 0;

        int[] dp = new int[amount + 1];
        
        // BASE CASE REASONING:
        // There is exactly 1 way to make an amount of 0 (the empty set).
        dp[0] = 1;

        // CRITICAL: The outer loop MUST be the coins, and the inner loop MUST be the amount.
        // If we swapped these, we would calculate Permutations (order matters) instead 
        // of Combinations. By locking the coin type on the outside, we guarantee that 
        // once we finish evaluating Coin 1, we never add a Coin 1 on top of a Coin 2.
        for (int currentCoin : coins) {
            
            // Traverse FORWARDS from the coin's value up to the target amount.
            for (int j = currentCoin; j <= amount; j++) {
                
                // The new total combinations for amount 'j' is:
                // (The combinations we already had WITHOUT this coin) 
                // PLUS 
                // (The combinations for the remaining amount 'j - currentCoin')
                dp[j] = dp[j] + dp[j - currentCoin];
                
            }
        }

        return dp[amount];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new CoinChangeII();
        
        record TestCase(int[] coins, int amount, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new int[]{1, 2, 5}, 5, 4),   // 4 combinations: [5], [2,2,1], [2,1,1,1], [1,1,1,1,1]
            new TestCase(new int[]{2}, 3, 0),         // Impossible to make 3 with only 2s
            new TestCase(new int[]{10}, 10, 1),       // 1 combination: [10]
            new TestCase(new int[]{1, 2, 5}, 0, 1),   // 1 combination: []
            new TestCase(new int[]{3, 5, 7, 8, 9, 10, 11}, 500, 35502874) // Large DP stress test
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Coins   : " + Arrays.toString(tc.coins));
            System.out.println("Amount  : " + tc.amount);
            System.out.println("Expected: " + tc.expected);
            
            // Limit brute-force recursion to very small amounts to avoid TLE during testing
            if (tc.amount <= 20) {
                System.out.println("Recursive (Brute) : " + solver.changeRecursive(tc.amount, tc.coins));
            } else {
                System.out.println("Recursive (Brute) : Skipped (Target too large for O(2^N))");
            }
            
            System.out.println("Memoization       : " + solver.changeMemo(tc.amount, tc.coins));
            System.out.println("Tabulation        : " + solver.changeTabulation(tc.amount, tc.coins));
            System.out.println("Space Optimized   : " + solver.changeSpaceOptimized(tc.amount, tc.coins));
            System.out.println();
        }
    }
}
