import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Subset Sum
 * Given an integer array nums and an integer target, determine if there exists 
 * a subset of the array whose sum equals the target.
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * For an L4/L5 level interview (like EPAM, Google, etc.), this is a classic 
 * 0/1 Knapsack variation. Before writing code, you MUST ask these questions:
 * 
 * Q: "Can the array contain negative numbers?"
 * A: This is the most critical question. If negative numbers are allowed, we 
 *    cannot simply bound our target in a standard DP array because the running 
 *    sum could fluctuate wildly. Assuming standard constraints for this pattern, 
 *    let's assume nums[i] >= 0. (If negatives are allowed, we'd need a 
 *    backtracking approach or a HashMap with an offset, completely changing the complexity).
 * 
 * Q: "Can the target be 0 or negative?"
 * A: If the target is 0, the answer is always true (the empty subset). If 
 *    target is negative and we only have positive integers, the answer is false.
 * 
 * Q: "Can we use an element more than once?"
 * A: The prompt says "subset", which mathematically implies picking a specific 
 *    element at index 'i' either 0 or 1 times. We cannot reuse the same index.
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "At any given element 'i', I have a choice to make:
 *  1. EXCLUDE it from my subset: The required target remains the same, and I 
 *     move on to the remaining elements.
 *  2. INCLUDE it in my subset: I can only do this if the element is not larger 
 *     than my remaining target. If I include it, my new required target becomes 
 *     (target - nums[i]), and I move on to the remaining elements.
 * 
 * If ANY of these choices eventually reduce the target to exactly 0, I have 
 * found a valid subset. Because we are making choices that lead to overlapping 
 * subproblems (e.g., reaching a remaining target of 5 via different subsets), 
 * this is a perfect candidate for Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: nums = [3, 34, 4, 12, 5, 2], target = 9
 * We want to see if we can make 9.
 * 
 * - Start from end: 2. Include 2? Target becomes 7. Exclude? Target is 9.
 * - From 7, include 5? Target becomes 2. Exclude? Target is 7.
 * - From 2, include 12? Too big, skip. 
 * - From 2, include 4? Too big, skip.
 * - ... wait, let's look at [4, 5]. 4 + 5 = 9!
 * 
 * Let's trace the Space-Optimized DP array (boolean[] dp) where dp[j] means 
 * "can we make sum j?".
 * Initially, dp[0] = true (we can always make sum 0). All others false.
 * 
 * After processing 3: dp[0]=T, dp[3]=T
 * After processing 4: dp[0]=T, dp[3]=T, dp[4]=T, dp[7]=T
 * After processing 5: dp[0]=T, dp[3]=T, dp[4]=T, dp[5]=T, dp[7]=T, dp[8]=T, dp[9]=T 
 * Target 9 is hit!
 */
public class SubsetSum {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Traverse the array and recursively check both "Include" and 
     * "Exclude" paths.
     * 
     * Time Complexity: O(2^n) - We make 2 decisions for each of the n elements.
     * Space Complexity: O(n) - Maximum depth of the recursion tree.
     */
    public boolean subsetSumRecursive(int[] nums, int target) {
        if (nums == null || nums.length == 0) return false;
        return solveRecursive(nums, nums.length - 1, target);
    }

    private boolean solveRecursive(int[] nums, int index, int currentTarget) {
        // BASE CASE REASONING:
        // If the remaining target is exactly 0, it means the subset of numbers 
        // we've picked so far perfectly sums to our goal. We've successfully 
        // found a valid subset.
        if (currentTarget == 0) {
            return true;
        }
        
        // BASE CASE REASONING:
        // If we run out of numbers to pick from (index < 0), and our target is 
        // still greater than 0, it is physically impossible to reach the target 
        // because we have no more tools left to use.
        if (index < 0) {
            return false;
        }

        // Choice 1: Exclude the current number
        boolean exclude = solveRecursive(nums, index - 1, currentTarget);
        
        // Choice 2: Include the current number (only if it doesn't overshoot target)
        boolean include = false;
        if (nums[index] <= currentTarget) {
            include = solveRecursive(nums, index - 1, currentTarget - nums[index]);
        }

        // If either path finds the sum, we bubble up 'true'
        return exclude || include;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: We might ask "Can we make a sum of 5 using elements up to index 3?" 
     * multiple times from different branches. Cache this state.
     * 
     * Time Complexity: O(n * target) - State space is index * remaining target.
     * Space Complexity: O(n * target) - For the 2D memo array + call stack.
     */
    public boolean subsetSumMemo(int[] nums, int target) {
        if (nums == null || nums.length == 0) return false;
        
        // Using Boolean object wrapper so we can initialize with null 
        // (meaning uncalculated) instead of defaulting to false.
        Boolean[][] memo = new Boolean[nums.length][target + 1];
        
        return solveMemo(nums, nums.length - 1, target, memo);
    }

    private boolean solveMemo(int[] nums, int index, int currentTarget, Boolean[][] memo) {
        // BASE CASE REASONING (same physical logic as brute force):
        // Target perfectly depleted -> success.
        if (currentTarget == 0) return true;
        // Ran out of elements -> failure.
        if (index < 0) return false;

        if (memo[index][currentTarget] != null) {
            return memo[index][currentTarget];
        }

        boolean exclude = solveMemo(nums, index - 1, currentTarget, memo);
        boolean include = false;
        if (nums[index] <= currentTarget) {
            include = solveMemo(nums, index - 1, currentTarget - nums[index], memo);
        }

        memo[index][currentTarget] = exclude || include;
        return memo[index][currentTarget];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation)
     * ========================================================================
     * Idea: Build a 2D grid where dp[i][j] answers "can we make sum 'j' using 
     * the first 'i' elements?".
     * 
     * Time Complexity: O(n * target)
     * Space Complexity: O(n * target) for the 2D array.
     */
    public boolean subsetSumTabulation(int[] nums, int target) {
        if (nums == null || nums.length == 0) return false;
        int n = nums.length;
        
        // dp[i][j] will be true if a subset of nums[0..i-1] has sum equal to j
        boolean[][] dp = new boolean[n + 1][target + 1];
        
        // BASE CASE REASONING:
        // If the required target is 0, we can ALWAYS achieve it by simply 
        // picking an empty subset (picking 0 elements). So the entire first 
        // column is true.
        for (int i = 0; i <= n; i++) {
            dp[i][0] = true;
        }
        
        // Note: dp[0][j] (where j > 0) is false by default in Java, which makes 
        // sense because we can't make a positive sum with 0 elements.

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= target; j++) {
                int currentElement = nums[i - 1];
                
                if (currentElement <= j) {
                    // We can either exclude it (take the answer from previous row) 
                    // or include it (check if we could make the remaining sum)
                    dp[i][j] = dp[i - 1][j] || dp[i - 1][j - currentElement];
                } else {
                    // Element is too large, we MUST exclude it
                    dp[i][j] = dp[i - 1][j];
                }
            }
        }

        return dp[n][target];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In tabulation, row 'i' ONLY depends on row 'i-1'. We can condense 
     * the 2D array into a 1D array. 
     * CRITICAL SENIOR INSIGHT: We MUST traverse the target loop backwards. 
     * If we traverse forwards, updating dp[j] would mean we might use the SAME 
     * element again when calculating a larger target dp[j + nums[i]], effectively 
     * turning this into an "Unbounded Knapsack" problem (allowing duplicates). 
     * Traversing backward ensures we only use the current element once per target.
     * 
     * Time Complexity: O(n * target)
     * Space Complexity: O(target) - We reduced 2D space down to a single row!
     */
    public boolean subsetSumSpaceOptimized(int[] nums, int target) {
        if (nums == null || nums.length == 0) return false;
        
        boolean[] dp = new boolean[target + 1];
        
        // BASE CASE REASONING:
        // We can always form a sum of 0 using an empty subset.
        dp[0] = true;

        for (int num : nums) {
            // Traverse backwards to prevent reusing the same 'num'
            for (int j = target; j >= num; j--) {
                // If we could make the sum without 'num' (dp[j] is already true)
                // OR if we could make the sum (j - num), then we can make 'j'
                dp[j] = dp[j] || dp[j - num];
            }
        }

        return dp[target];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new SubsetSum();
        
        // Helper record for clean test case structuring
        record TestCase(int[] nums, int target, boolean expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new int[]{3, 34, 4, 12, 5, 2}, 9, true),  // 4 + 5 = 9
            new TestCase(new int[]{3, 34, 4, 12, 5, 2}, 30, false),
            new TestCase(new int[]{1, 5, 11, 5}, 11, true),        // Just pick 11
            new TestCase(new int[]{2, 3, 5, 6}, 10, true),         // 2 + 3 + 5 = 10
            new TestCase(new int[]{2, 2, 3}, 5, true)              // 2 + 3 = 5
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Array   : " + Arrays.toString(tc.nums));
            System.out.println("Target  : " + tc.target);
            System.out.println("Expected: " + tc.expected);
            
            System.out.println("Recursive (Brute) : " + solver.subsetSumRecursive(tc.nums, tc.target));
            System.out.println("Memoization       : " + solver.subsetSumMemo(tc.nums, tc.target));
            System.out.println("Tabulation        : " + solver.subsetSumTabulation(tc.nums, tc.target));
            System.out.println("Space Optimized   : " + solver.subsetSumSpaceOptimized(tc.nums, tc.target));
            System.out.println();
        }
    }
}
