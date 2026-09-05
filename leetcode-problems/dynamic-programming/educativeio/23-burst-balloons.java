import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Burst Balloons
 * You are given 'n' balloons, each with a number on it.
 * Bursting balloon 'i' earns you nums[i-1] * nums[i] * nums[i+1] coins.
 * Out of bounds indices are treated as balloons with the number 1.
 * Return the maximum coins you can obtain by bursting all balloons.
 * 
 * Constraints:
 * n == nums.length
 * 1 <= n <= 300
 * 0 <= nums[i] <= 100
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, this problem is famous for being a "trap" if you try 
 * to solve it moving forward. 
 * 
 * Q: "Can there be balloons with a value of 0?"
 * A: Yes. Bursting a 0 yields 0 coins, but more importantly, if it is adjacent 
 *    to another balloon you burst later, it forces that multiplication to be 0! 
 *    Therefore, we usually want to burst 0-value balloons as early as possible.
 * 
 * Q: "Do the out-of-bounds 1s ever get burst?"
 * A: No. The problem states we only burst the given 'n' balloons. The 1s at 
 *    the boundaries are permanent anchors.
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "If I try to pick the FIRST balloon to burst, say at index 'k', the array 
 * splits into two halves. However, these two halves are NOT independent! 
 * A balloon on the left half will eventually become adjacent to a balloon on 
 * the right half as the middle balloons disappear. Standard DP breaks down here.
 * 
 * CRITICAL SENIOR INSIGHT: THE REVERSE THINKING TRICK
 * Instead of picking the FIRST balloon to burst, what if we pick the LAST 
 * balloon to burst in a given range?
 * 
 * If balloon 'k' is the absolute LAST balloon we burst between boundaries 'left' 
 * and 'right', it means every other balloon inside that range has already popped!
 * Therefore, right before 'k' pops, its left adjacent is strictly the boundary 
 * 'left', and its right adjacent is strictly the boundary 'right'.
 * 
 * Furthermore, the subproblems (bursting everything between 'left' and 'k', and 
 * bursting everything between 'k' and 'right') are now COMPLETELY INDEPENDENT, 
 * because the permanent anchors 'left', 'k', and 'right' isolate them.
 * 
 * This is Interval Dynamic Programming (similar to Matrix Chain Multiplication)."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: nums = [3, 1, 5, 8]
 * We pad the array with 1s at both ends: padded = [1, 3, 1, 5, 8, 1]
 * Indices:                                         0  1  2  3  4  5
 * 
 * We want to find the max coins for bursting everything strictly BETWEEN index 0 and 5.
 * 
 * Let's try picking '8' (index 4) as the LAST balloon to burst in this range.
 * This splits our problem into:
 * 1. Burst everything between 0 and 4 (the [3, 1, 5]).
 * 2. Burst everything between 4 and 5 (Empty).
 * 3. The final burst of '8' itself, which will yield: padded[0] * padded[4] * padded[5]
 *                                                   = 1 * 8 * 1 = 8.
 * 
 * By recursively finding the best "last balloon" for every sub-interval, 
 * we guarantee the maximum possible score.
 */
public class BurstBalloons {

    /**
     * Helper method to create our padded array with permanent 1s on the ends.
     */
    private int[] createPaddedArray(int[] nums) {
        int n = nums.length;
        int[] padded = new int[n + 2];
        padded[0] = 1;
        padded[n + 1] = 1;
        for (int i = 0; i < n; i++) {
            padded[i + 1] = nums[i];
        }
        return padded;
    }

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Test every single balloon as the "last one to burst" in a given range.
     * 
     * Time Complexity: O(N!) or Catalan number exponential without memoization.
     * Space Complexity: O(N) - Maximum depth of the recursion tree.
     */
    public int maxCoinsRecursive(int[] nums) {
        if (nums == null || nums.length == 0) return 0;
        int[] padded = createPaddedArray(nums);
        
        // We evaluate the exclusive range between index 0 and index n + 1
        return solveRecursive(padded, 0, padded.length - 1);
    }

    private int solveRecursive(int[] padded, int left, int right) {
        // BASE CASE REASONING:
        // If left + 1 == right, there are strictly ZERO balloons between them.
        // E.g., range (0, 1) means we evaluate balloons between index 0 and 1.
        // Since no balloons exist to pop in an empty range, we earn 0 coins.
        if (left + 1 == right) {
            return 0;
        }

        int maxCoins = 0;

        // We iterate through every balloon strictly BETWEEN 'left' and 'right'.
        // We pretend 'k' is the absolute LAST balloon to pop in this interval.
        for (int k = left + 1; k < right; k++) {
            
            // The coins we get for bursting 'k' last.
            // Since everything else inside the interval is already gone, 
            // its adjacent balloons are purely our boundaries 'left' and 'right'.
            int coinsForBurstingKLast = padded[left] * padded[k] * padded[right];
            
            // The max coins we could get from the left sub-interval
            int leftSubInterval = solveRecursive(padded, left, k);
            
            // The max coins we could get from the right sub-interval
            int rightSubInterval = solveRecursive(padded, k, right);
            
            int totalCoins = coinsForBurstingKLast + leftSubInterval + rightSubInterval;
            
            maxCoins = Math.max(maxCoins, totalCoins);
        }

        return maxCoins;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the max coins obtained for the interval strictly between 
     * 'left' and 'right' to avoid massive re-computation.
     * 
     * Time Complexity: O(N^3) - State space is N^2, and we run a loop of size N inside.
     * Space Complexity: O(N^2) - For the memo array + call stack.
     */
    public int maxCoinsMemo(int[] nums) {
        if (nums == null || nums.length == 0) return 0;
        
        int[] padded = createPaddedArray(nums);
        int n = padded.length;
        
        int[][] memo = new int[n][n];
        for (int[] row : memo) Arrays.fill(row, -1);
        
        return solveMemo(padded, 0, n - 1, memo);
    }

    private int solveMemo(int[] padded, int left, int right, int[][] memo) {
        // BASE CASE (Same physical logic as brute force: empty interval)
        if (left + 1 == right) return 0;

        if (memo[left][right] != -1) {
            return memo[left][right];
        }

        int maxCoins = 0;
        for (int k = left + 1; k < right; k++) {
            int coins = padded[left] * padded[k] * padded[right]
                      + solveMemo(padded, left, k, memo)
                      + solveMemo(padded, k, right, memo);
            maxCoins = Math.max(maxCoins, coins);
        }

        memo[left][right] = maxCoins;
        return memo[left][right];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Build an N x N grid. dp[left][right] signifies the maximum coins 
     * obtained by bursting all balloons strictly between 'left' and 'right'.
     * 
     * Time Complexity: O(N^3)
     * Space Complexity: O(N^2)
     */
    public int maxCoinsTabulation(int[] nums) {
        if (nums == null || nums.length == 0) return 0;
        
        int[] padded = createPaddedArray(nums);
        int n = padded.length; // n is now the original length + 2
        
        int[][] dp = new int[n][n];

        // BASE CASE REASONING (Implicit in Java):
        // dp[i][i+1] is always 0 because there are no balloons strictly between 
        // two adjacent indices. Our array is initialized to 0, handling this perfectly.

        // Outer loop (left boundary): 
        // We must process smaller intervals before larger ones. 
        // By looping 'left' backwards from the end towards the start...
        for (int left = n - 2; left >= 0; left--) {
            
            // Inner loop (right boundary):
            // ...and looping 'right' forwards starting just past 'left'...
            // We guarantee that the interval length (right - left) grows steadily.
            // This ensures that when we evaluate a large interval, all of its smaller 
            // sub-intervals have ALREADY been calculated!
            for (int right = left + 2; right < n; right++) {
                
                // We test every balloon 'k' strictly between our boundaries
                // to see which one acts as the best "final balloon to pop".
                for (int k = left + 1; k < right; k++) {
                    
                    // --- DETAILED TABULATION EXPLANATION ---
                    // 1. We burst everything in the left sub-interval.
                    //    We look up dp[left][k], which was already computed because 
                    //    its length (k - left) is strictly smaller than (right - left).
                    int leftSubInterval = dp[left][k];
                    
                    // 2. We burst everything in the right sub-interval.
                    //    We look up dp[k][right], which was also already computed.
                    int rightSubInterval = dp[k][right];
                    
                    // 3. We burst 'k' itself last.
                    //    Because everything else between 'left' and 'right' is gone, 
                    //    k's neighbors are perfectly 'left' and 'right'.
                    int coinsForK = padded[left] * padded[k] * padded[right];
                    
                    int totalCoins = leftSubInterval + rightSubInterval + coinsForK;
                    
                    // Keep the maximum value across all choices of 'k'
                    dp[left][right] = Math.max(dp[left][right], totalCoins);
                }
            }
        }

        // The answer is the maximum coins strictly between the two permanent 1s.
        return dp[0][n - 1];
    }

    /**
     * ========================================================================
     * APPROACH 4: Note on Space Optimization (L4/L5 Target Knowledge)
     * ========================================================================
     * In standard DP problems (like Knapsack or LCS), we can optimize 2D space 
     * down to 1D because computing a cell in row `i` ONLY relies on row `i-1`.
     * 
     * CRITICAL INSIGHT:
     * This problem is an "Interval DP" problem. To compute the answer for 
     * `dp[left][right]`, we must look up `dp[left][k]` and `dp[k][right]` for 
     * EVERY 'k' between 'left' and 'right'. 
     * 
     * This means we are querying cells all over the matrix (across multiple rows 
     * and columns simultaneously). We CANNOT discard previous rows because they 
     * represent the smaller intervals that make up the foundations of our larger 
     * intervals.
     * 
     * Therefore, O(N^2) space is the strict mathematical lower bound for this 
     * DP approach. An L4/L5 engineer must explicitly state this during an 
     * interview rather than blindly trying to collapse it to 1D.
     */

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new BurstBalloons();
        
        record TestCase(int[] nums, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new int[]{3, 1, 5, 8}, 167),
            // Optimal sequence for [3, 1, 5, 8]:
            // Burst 1: [3, 5, 8] -> 3*1*5 = 15
            // Burst 5: [3, 8]    -> 3*5*8 = 120
            // Burst 3: [8]       -> 1*3*8 = 24
            // Burst 8: []        -> 1*8*1 = 8
            // Total = 15 + 120 + 24 + 8 = 167
            
            new TestCase(new int[]{1, 5}, 10), // Burst 1 (1*1*5 = 5), then 5 (1*5*1 = 5). Total 10.
            new TestCase(new int[]{4, 2, 3, 7}, 148), // General test
            new TestCase(new int[]{8, 2, 6, 8, 9, 8, 1, 4, 1, 5, 3, 0, 7, 7, 0, 4, 2, 2, 5}, 3446) // Stress test
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Balloons: " + Arrays.toString(tc.nums));
            System.out.println("Expected: " + tc.expected);
            
            // Skip pure recursion for anything larger than 10 balloons
            if (tc.nums.length <= 10) {
                System.out.println("Recursive (Brute): " + solver.maxCoinsRecursive(tc.nums));
            } else {
                System.out.println("Recursive (Brute): Skipped (Interval DP is O(N!) without memo)");
            }
            
            System.out.println("Memoization      : " + solver.maxCoinsMemo(tc.nums));
            System.out.println("Tabulation 2D    : " + solver.maxCoinsTabulation(tc.nums));
            System.out.println();
        }
    }
}
