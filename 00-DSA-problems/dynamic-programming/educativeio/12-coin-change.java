import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Coin Change (Unbounded Knapsack)
 * Given an integer 'total' and a list of 'coins' representing denominations, 
 * find the MINIMUM number of coins required to make up the total.
 * Return -1 if it's impossible.
 * 
 * Note: You have an INFINITE number of each kind of coin.
 * 
 * Constraints:
 * 1 <= coins.length <= 12
 * 1 <= coins[i] <= 10^4
 * 0 <= total <= 900
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, immediately recognize this as an "Unbounded Knapsack" 
 * problem. Before writing code, clarify the following:
 * 
 * Q: "Can the total be 0?"
 * A: Yes, constraint says 0 <= total <= 900. If total is 0, we need exactly 
 *    0 coins. This is a critical base case.
 * 
 * Q: "How should I represent an 'impossible' state during calculations?"
 * A: We are looking for a MINIMUM. If we initialize our variables to Integer.MAX_VALUE, 
 *    and then we try to add 1 to it (because we took a coin), it will integer-overflow 
 *    into a massive negative number, completely destroying our Math.min() logic. 
 *    We must use a safe "infinity" value, like `total + 1` or `1e9`.
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "This is similar to the 0/1 Knapsack problem, but with one massive difference: 
 * we can reuse the same item (coin) infinitely. 
 * 
 * At any given coin 'i', I have two choices to make the amount 'j':
 *  1. EXCLUDE the coin: I move on to the next coin, and try to make the same amount 'j'.
 *  2. INCLUDE the coin: I use 1 coin, my required amount decreases to 'j - coin_value', 
 *     BUT I DO NOT move to the next coin. I stay at the exact same coin 'i' because 
 *     I am allowed to use it again!
 * 
 * Because we are exploring combinations that overlap (e.g., reaching an amount 
 * of 5 using different permutations of coins), this requires Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: coins = [1, 2, 5], total = 11
 * 
 * Let's trace the Space-Optimized 1D DP array. 
 * dp[j] means "minimum coins to make amount j".
 * Initially: dp[0] = 0, all others = Infinity (or total + 1 = 12).
 * 
 * Using coin 1:
 * dp[1] = min(dp[1], 1 + dp[0]) = 1
 * dp[2] = min(dp[2], 1 + dp[1]) = 2
 * ...
 * dp[11] = 11 (using eleven 1s)
 * 
 * Using coin 2:
 * dp[2] = min(dp[2], 1 + dp[0]) = 1  (Replaced the two 1s with a single 2)
 * dp[3] = min(dp[3], 1 + dp[1]) = 2  (A 2 and a 1)
 * dp[4] = min(dp[4], 1 + dp[2]) = 2  (Two 2s)
 * ...
 * 
 * Using coin 5:
 * dp[5] = min(dp[5], 1 + dp[0]) = 1
 * dp[10] = min(dp[10], 1 + dp[5]) = 2 (Two 5s)
 * dp[11] = min(dp[11], 1 + dp[6]) = min(dp[11], 1 + dp[10 + 1]) -> wait, 1 + dp[6] which is 1 + 3 = 4?
 * Actually, dp[11] = min(dp[11], 1 + dp[6]) = min(6, 1 + 2) = 3 (Two 5s and one 1).
 */
public class CoinChange {

    // A safe infinity value that won't overflow when we add 1 to it.
    private final int INF = (int) 1e9;

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force 2D)
     * ========================================================================
     * Idea: Traverse the coin array. For each coin, try taking it (and staying 
     * on the same index) or leaving it (and moving to the next index).
     * 
     * Time Complexity: O(2^(total/min_coin)) - Exponential.
     * Space Complexity: O(total/min_coin) - Maximum recursion depth.
     */
    public int coinChangeRecursive(int[] coins, int total) {
        if (coins == null || coins.length == 0) return -1;
        
        int result = solveRecursive(coins, coins.length - 1, total);
        return result >= INF ? -1 : result;
    }

    private int solveRecursive(int[] coins, int index, int amount) {
        // BASE CASE REASONING:
        // If the amount we need to make drops to exactly 0, it means the coins 
        // we've selected add up perfectly. How many *more* coins do we need to 
        // make 0? Exactly 0 coins.
        if (amount == 0) {
            return 0;
        }

        // BASE CASE REASONING:
        // If we run out of coins to look at (index < 0), but we still have a 
        // positive amount left to make, it's physically impossible. We return 
        // our "infinity" value to signal a dead end.
        if (index < 0) {
            return INF;
        }

        // Choice 1: Exclude the coin. 
        // We move to index - 1, amount stays the same.
        int exclude = solveRecursive(coins, index - 1, amount);

        // Choice 2: Include the coin (if it fits).
        int include = INF;
        if (coins[index] <= amount) {
            // CRITICAL SENIOR INSIGHT: Notice we pass 'index', NOT 'index - 1'!
            // Because we have infinite coins, if we use a coin, we are allowed 
            // to use it again. We stay at the same index but reduce the amount.
            include = 1 + solveRecursive(coins, index, amount - coins[index]);
        }

        return Math.min(exclude, include);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache states of [Current Coin Index][Remaining Amount].
     * 
     * Time Complexity: O(n * total)
     * Space Complexity: O(n * total) for the memo array + call stack.
     */
    public int coinChangeMemo(int[] coins, int total) {
        if (coins == null || coins.length == 0) return -1;
        int n = coins.length;

        int[][] memo = new int[n][total + 1];
        for (int[] row : memo) Arrays.fill(row, -1);

        int result = solveMemo(coins, n - 1, total, memo);
        return result >= INF ? -1 : result;
    }

    private int solveMemo(int[] coins, int index, int amount, int[][] memo) {
        // BASE CASES (same physical logic as brute force)
        if (amount == 0) return 0;
        if (index < 0) return INF;

        if (memo[index][amount] != -1) {
            return memo[index][amount];
        }

        int exclude = solveMemo(coins, index - 1, amount, memo);
        
        int include = INF;
        if (coins[index] <= amount) {
            include = 1 + solveMemo(coins, index, amount - coins[index], memo);
        }

        memo[index][amount] = Math.min(exclude, include);
        return memo[index][amount];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Build a 2D grid where dp[i][j] is the minimum coins to make amount 
     * 'j' using the first 'i' types of coins.
     * 
     * Time Complexity: O(n * total)
     * Space Complexity: O(n * total)
     */
    public int coinChangeTabulation(int[] coins, int total) {
        int n = coins.length;
        if (n == 0) return -1;

        // dp[i][j] signifies: "The absolute MINIMUM number of coins required 
        // to make exactly the amount 'j', if I am only allowed to use a subset 
        // of the first 'i' coins."
        int[][] dp = new int[n + 1][total + 1];

        // BASE CASE REASONING:
        // We initialize the entire grid with our safe "infinity" value.
        // We assume it is physically impossible to make any amount until proven otherwise.
        for (int i = 0; i <= n; i++) {
            Arrays.fill(dp[i], INF);
        }

        // BASE CASE REASONING:
        // How many coins does it take to make an amount of 0?
        // Exactly 0 coins. No matter how many coin types we are allowed to look at, 
        // if the target is 0, the answer is 0. So the entire first column is 0.
        for (int i = 0; i <= n; i++) {
            dp[i][0] = 0;
        }

        // We iterate through every coin type we have available (i)
        for (int i = 1; i <= n; i++) {
            
            // This is the literal value of the coin we are holding right now.
            int currentCoin = coins[i - 1];

            // We try to form every possible amount from 1 up to our target total (j)
            for (int j = 1; j <= total; j++) {
                
                // PHYSICAL CHECK: Is the coin I am holding small enough to even 
                // fit into my target amount 'j'?
                if (currentCoin <= j) {
                    
                    // YES, it fits! I have two choices to make this amount:
                    
                    // CHOICE 1 (Exclude): I completely ignore this coin type.
                    // To find my min coins, I look directly UP one row in my spreadsheet. 
                    // "What was the minimum coins needed to make amount 'j' using ONLY 
                    // the previous coin types?"
                    int exclude = dp[i - 1][j];
                    
                    // CHOICE 2 (Include): I decide to use 1 of these coins.
                    // This costs me 1 coin (+1), and eats up 'currentCoin' amount of my target.
                    // CRITICAL UNBOUNDED KNAPSACK INSIGHT:
                    // Because I can use this coin AGAIN, I do NOT look up a row. 
                    // I look left on the EXACT SAME ROW.
                    // "What is the minimum coins needed to make the REMAINING amount 
                    // (j - currentCoin) allowing the use of this exact same coin type?"
                    int include = 1 + dp[i][j - currentCoin];
                    
                    // I am greedy. I want whichever choice required fewer coins.
                    dp[i][j] = Math.min(exclude, include);
                    
                } else {
                    // NO, the coin is strictly larger than my target amount 'j'. 
                    // It is physically impossible to use it.
                    // My ONLY choice is to exclude it and hope the previous coin 
                    // types were able to make this amount.
                    dp[i][j] = dp[i - 1][j];
                }
            }
        }

        int result = dp[n][total];
        return result >= INF ? -1 : result;
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (1D Array - L4/L5 Target)
     * ========================================================================
     * Idea: In our tabulation loop, computing row 'i' required looking up at row 
     * 'i-1' (exclude) and looking left in the same row 'i' (include).
     * We can collapse this into a single 1D array representing the target amounts!
     * 
     * CRITICAL DIFFERENCE FROM 0/1 KNAPSACK:
     * In 0/1 Knapsack, we traversed the array BACKWARDS to avoid reusing the item.
     * Here, in Unbounded Knapsack, we WANT to reuse the item! So we traverse the 
     * array FORWARDS. This way, when we check `dp[j - currentCoin]`, it already 
     * contains the result of potentially using the current coin.
     * 
     * Time Complexity: O(n * total)
     * Space Complexity: O(total) - Massively reduced memory footprint.
     */
    public int coinChangeSpaceOptimized(int[] coins, int total) {
        if (coins == null || coins.length == 0) return -1;

        int[] dp = new int[total + 1];
        
        // BASE CASE REASONING:
        // Initialize all amounts to "infinity" (impossible).
        // Except for amount 0, which takes exactly 0 coins.
        Arrays.fill(dp, INF);
        dp[0] = 0;

        for (int currentCoin : coins) {
            
            // We traverse FORWARDS from the coin's value up to the total.
            // Going forwards allows the current coin to geometrically compound 
            // its own results (e.g., finding the answer for 4 looks at the 
            // answer for 2, which already factored in this coin).
            for (int j = currentCoin; j <= total; j++) {
                
                // dp[j] on the right side acts as the 'exclude' choice (old value).
                // 1 + dp[j - currentCoin] acts as the 'include' choice.
                dp[j] = Math.min(dp[j], 1 + dp[j - currentCoin]);
                
            }
        }

        return dp[total] >= INF ? -1 : dp[total];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new CoinChange();
        
        record TestCase(int[] coins, int total, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new int[]{1, 2, 5}, 11, 3),    // 5 + 5 + 1 = 11 (3 coins)
            new TestCase(new int[]{2}, 3, -1),          // Impossible
            new TestCase(new int[]{1}, 0, 0),           // 0 amount needs 0 coins
            new TestCase(new int[]{186, 419, 83, 408}, 6249, 20), // Large test case
            new TestCase(new int[]{3, 7, 405, 436}, 8839, 25)
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Coins   : " + Arrays.toString(tc.coins));
            System.out.println("Total   : " + tc.total);
            System.out.println("Expected: " + tc.expected);
            
            // Skip pure recursive brute force for large totals to avoid hours of execution time
            if (tc.total <= 30) {
                System.out.println("Recursive (Brute) : " + solver.coinChangeRecursive(tc.coins, tc.total));
            } else {
                System.out.println("Recursive (Brute) : Skipped (Total too large for O(2^N))");
            }
            
            System.out.println("Memoization       : " + solver.coinChangeMemo(tc.coins, tc.total));
            System.out.println("Tabulation        : " + solver.coinChangeTabulation(tc.coins, tc.total));
            System.out.println("Space Optimized   : " + solver.coinChangeSpaceOptimized(tc.coins, tc.total));
            System.out.println();
        }
    }
}
