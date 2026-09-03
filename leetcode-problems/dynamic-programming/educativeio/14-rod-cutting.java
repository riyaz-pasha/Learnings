import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Rod Cutting Problem
 * You are given a rod of length 'n' and an array 'price' where price[i] 
 * represents the value of a piece of length (i + 1).
 * Determine the maximum total profit you can obtain by cutting the rod and 
 * selling the pieces. You can cut the rod into any number of pieces.
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, point out how this maps to known patterns:
 * 
 * Q: "Do I have to cut the rod at all?"
 * A: No, you can sell the entire rod as one piece if that yields the highest 
 *    profit (i.e., if price[n-1] is strictly greater than any combination of cuts).
 * 
 * Q: "Can a piece be of length 0?"
 * A: No, a piece must have a minimum length of 1. A piece of length 0 has 0 value.
 * 
 * CRITICAL SENIOR INSIGHT: 
 * "This is structurally identical to the 'Unbounded Knapsack' (Coin Change) problem!
 * - The 'knapsack capacity' is the total length of the rod 'n'.
 * - The 'items' we can pick are the possible cuts (lengths 1 through n).
 * - The 'weight' of an item is its cut length.
 * - The 'value' of an item is its price.
 * - Because we can cut multiple pieces of the same length, we have an INFINITE 
 *   supply of each 'item'."
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "At any given cut length 'i' (where the physical length is i + 1), I have two choices:
 *  1. EXCLUDE: I decide not to make any more cuts of this length. I move on to 
 *     considering smaller/different cut lengths.
 *  2. INCLUDE: I slice off a piece of this length. I gain its price. Because I 
 *     might want to cut ANOTHER piece of this exact same length, I stay on the 
 *     exact same choice for my next step, just with a reduced remaining rod length.
 * 
 * Overlapping subproblems exist (e.g., cutting 2 then 1 leaves the same remaining 
 * rod as cutting 1 then 2), meaning we must use Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: length = 4, price = [1, 5, 8, 9] (Lengths: 1, 2, 3, 4)
 * 
 * Let's trace the optimal cuts:
 * - Sell whole rod of length 4: Profit = 9
 * - Cut into two pieces of length 2: Profit = price[1] + price[1] = 5 + 5 = 10
 * - Cut into four pieces of length 1: Profit = 1 + 1 + 1 + 1 = 4
 * 
 * Max profit is 10 (by cutting the rod into two pieces of length 2).
 */
public class RodCutting {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Traverse the possible cut lengths. For each, try excluding it 
     * entirely or including it (if the rod is long enough) and staying on it.
     * 
     * Time Complexity: O(2^n) - Exponential, as we explore all combinations of cuts.
     * Space Complexity: O(n) - Maximum depth of the recursion tree.
     */
    public int cutRodRecursive(int[] price, int n) {
        if (price == null || price.length == 0 || n <= 0) return 0;
        
        // We start considering from the maximum possible cut piece (index n - 1)
        return solveRecursive(price, n - 1, n);
    }

    private int solveRecursive(int[] price, int index, int remainingLength) {
        // BASE CASE REASONING:
        // If the remaining length of the rod is 0, we physically have no rod 
        // left to cut. We can't make any more money. Return 0 profit.
        if (remainingLength == 0) {
            return 0;
        }

        // BASE CASE REASONING:
        // If we have considered all possible cut pieces (index drops below 0), 
        // we have no more tools/options left to cut the remaining rod. 
        // Our profit accumulation from this specific path stops here.
        if (index < 0) {
            return 0;
        }

        // The actual physical length of the piece we are currently considering.
        // (Because arrays are 0-indexed, index 0 represents length 1)
        int cutLength = index + 1;

        // Choice 1: Exclude this cut length. 
        // We move to the next smaller cut piece (index - 1).
        int exclude = solveRecursive(price, index - 1, remainingLength);

        // Choice 2: Include this cut length (if we have enough rod left).
        int include = 0;
        if (cutLength <= remainingLength) {
            // UNBOUNDED KNAPSACK MAGIC: 
            // We gain the price of this cut, AND we keep the index the SAME, 
            // allowing us to recursively cut another piece of this exact same length!
            include = price[index] + solveRecursive(price, index, remainingLength - cutLength);
        }

        // We are trying to maximize profit, so return the better of the two choices.
        return Math.max(exclude, include);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache states of [Current Piece Index][Remaining Rod Length].
     * 
     * Time Complexity: O(n^2) - We evaluate each state exactly once.
     * Space Complexity: O(n^2) - For the memo array + call stack.
     */
    public int cutRodMemo(int[] price, int n) {
        if (price == null || price.length == 0 || n <= 0) return 0;
        
        int[][] memo = new int[n][n + 1];
        for (int[] row : memo) Arrays.fill(row, -1);
        
        return solveMemo(price, n - 1, n, memo);
    }

    private int solveMemo(int[] price, int index, int remainingLength, int[][] memo) {
        // BASE CASES (Same physical logic as brute force)
        if (remainingLength == 0) return 0;
        if (index < 0) return 0;

        if (memo[index][remainingLength] != -1) {
            return memo[index][remainingLength];
        }

        int cutLength = index + 1;

        int exclude = solveMemo(price, index - 1, remainingLength, memo);
        
        int include = 0;
        if (cutLength <= remainingLength) {
            include = price[index] + solveMemo(price, index, remainingLength - cutLength, memo);
        }

        memo[index][remainingLength] = Math.max(exclude, include);
        return memo[index][remainingLength];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Build a 2D grid. Rows represent the piece lengths we are allowed to use. 
     * Columns represent the total rod length we are trying to optimally cut.
     * 
     * Time Complexity: O(n^2)
     * Space Complexity: O(n^2)
     */
    public int cutRodTabulation(int[] price, int n) {
        if (price == null || price.length == 0 || n <= 0) return 0;

        // dp[i][j] signifies: "The absolute maximum profit I can make from a rod 
        // of total length 'j', if I am only allowed to cut pieces up to length 'i'."
        int[][] dp = new int[n + 1][n + 1];

        // BASE CASE REASONING:
        // dp[i][0] (Col 0): If the total rod length 'j' is 0, we have no rod to cut, 
        // so max profit is always 0.
        // dp[0][j] (Row 0): If we are allowed to use 0 types of cuts, we can't do 
        // anything, so profit is 0.
        // Java initializes integer arrays to 0 by default, so we get these for free!

        // Outer loop: We slowly unlock the ability to cut pieces of length 'i' (1 to n)
        for (int i = 1; i <= n; i++) {
            
            // The physical length of the cut we are considering.
            int currentCutLength = i;
            
            // The price of making this specific cut (0-indexed array mapping).
            int currentCutPrice = price[i - 1];

            // Inner loop: We test this cut on every possible total rod length 'j' (1 to n)
            for (int j = 1; j <= n; j++) {
                
                // PHYSICAL CHECK: Is the piece I want to cut (currentCutLength) 
                // actually smaller than or equal to the total rod I have right now (j)?
                if (currentCutLength <= j) {
                    
                    // YES, the cut is physically possible! I can maximize my profit by 
                    // comparing two alternate realities:
                    
                    // REALITY 1 (Exclude): I refuse to make this cut.
                    // I look directly UP one row in my spreadsheet. 
                    // "What was the max profit for this same rod length 'j', using ONLY 
                    // the smaller cut pieces from previous rows?"
                    int profitByExcluding = dp[i - 1][j];
                    
                    // REALITY 2 (Include): I slice off a piece of this length!
                    // This instantly puts 'currentCutPrice' cash in my pocket.
                    // BUT, my rod is now shorter by 'currentCutLength'. 
                    // Because I am allowed to make this exact same cut again (Unbounded Knapsack),
                    // I look LEFT on the EXACT SAME ROW to find the best way to cut the remaining rod.
                    int profitByIncluding = currentCutPrice + dp[i][j - currentCutLength];
                    
                    // Record the absolute best choice.
                    dp[i][j] = Math.max(profitByExcluding, profitByIncluding);
                    
                } else {
                    // NO, the cut is impossible. The piece I want to cut is longer than 
                    // the entire rod 'j' I currently hold.
                    // My ONLY option is to look up and copy the best strategy that didn't 
                    // rely on this oversized piece.
                    dp[i][j] = dp[i - 1][j];
                }
            }
        }

        return dp[n][n];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (1D Array - L4/L5 Target)
     * ========================================================================
     * Idea: Notice in the Tabulation loops, calculating row 'i' only requires 
     * looking at the row directly above (i-1) and values to the left on the same row.
     * We can flatten this 2D grid into a single 1D array representing rod lengths!
     * 
     * Since this is an Unbounded Knapsack pattern, we traverse the array FORWARDS. 
     * Going forwards ensures that when we look at the remaining rod length 
     * (`j - currentCutLength`), that state has ALREADY been updated with the possibility 
     * of using the current piece, allowing us to use the same cut multiple times.
     * 
     * Time Complexity: O(n^2)
     * Space Complexity: O(n) - Massively reduced memory footprint!
     */
    public int cutRodSpaceOptimized(int[] price, int n) {
        if (price == null || price.length == 0 || n <= 0) return 0;

        // dp[j] represents the maximum profit for a rod of length 'j'.
        int[] dp = new int[n + 1];
        
        // BASE CASE REASONING:
        // dp[0] = 0. A rod of length 0 yields 0 profit. 
        // Automatically initialized to 0 by Java.

        // We iterate over every possible cut length we can make (1 to n)
        for (int i = 1; i <= n; i++) {
            
            int currentCutLength = i;
            int currentCutPrice = price[i - 1];

            // Traverse FORWARDS from the length of the cut up to the total rod length 'n'.
            for (int j = currentCutLength; j <= n; j++) {
                
                // We overwrite our single array in-place.
                // dp[j] on the right side acts as the 'Exclude' choice (best profit so far).
                // (currentCutPrice + dp[j - currentCutLength]) acts as the 'Include' choice.
                dp[j] = Math.max(dp[j], currentCutPrice + dp[j - currentCutLength]);
                
            }
        }

        return dp[n];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new RodCutting();
        
        record TestCase(int[] price, int n, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            // Length:   1  2  3  4  5   6   7   8
            new TestCase(new int[]{1, 5, 8, 9, 10, 17, 17, 20}, 8, 22), // Best: Two length-2 (5+5) and one length-4 (9) -> Wait! Actually, length 2 (5) + length 6 (17) = 22.
            new TestCase(new int[]{3, 5, 8, 9, 10, 17, 17, 20}, 8, 24), // Best: Eight length-1 (3*8 = 24)
            new TestCase(new int[]{1, 10, 11, 12}, 4, 20),              // Best: Two length-2 (10+10 = 20)
            new TestCase(new int[]{2}, 1, 2)                            // Base case mapping check
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Prices : " + Arrays.toString(tc.price));
            System.out.println("Length : " + tc.n);
            System.out.println("Expected Profit: " + tc.expected);
            
            System.out.println("Recursive (Brute) : " + solver.cutRodRecursive(tc.price, tc.n));
            System.out.println("Memoization       : " + solver.cutRodMemo(tc.price, tc.n));
            System.out.println("Tabulation        : " + solver.cutRodTabulation(tc.price, tc.n));
            System.out.println("Space Optimized   : " + solver.cutRodSpaceOptimized(tc.price, tc.n));
            System.out.println();
        }
    }
}
