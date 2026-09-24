import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Partition Equal Subset Sum
 * Given a non-empty array of positive integers, determine if the array can be 
 * divided into two subsets so that the sum of both subsets is equal.
 * 
 * Constraints:
 * 1 <= nums.length <= 200
 * 1 <= nums[i] <= 100
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, recognizing the core pattern instantly is crucial.
 * Before writing code, you should ask/state:
 * 
 * Q: "Do the subsets need to be contiguous?"
 * A: No, the prompt says 'subsets', not 'subarrays'. We can pick elements 
 *    from anywhere in the array.
 * 
 * Q: "Are there any negative numbers?"
 * A: The constraints specify positive integers. This guarantees our sum will 
 *    only grow as we add elements, which is essential for bounding our target 
 *    in standard Dynamic Programming.
 * 
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "If we are splitting the array into two equal halves, the total sum of the 
 * array MUST be an even number. If the total sum is odd, it is mathematically 
 * impossible to split it into two integer subsets of equal value.
 * 
 * If the sum is even, the problem elegantly reduces to the exact same logic as 
 * the 'Subset Sum' problem. We simply calculate `target = total_sum / 2`. 
 * If we can find ANY subset that equals this target, the remaining elements 
 * in the array will naturally sum to the other half!
 * 
 * Therefore, this is fundamentally a 0/1 Knapsack problem where our knapsack 
 * capacity is exactly half the total sum."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example 1: nums = [1, 5, 11, 5]
 * Total Sum = 22. 
 * Is it even? Yes. 
 * Target = 22 / 2 = 11.
 * 
 * We trace to see if we can make 11:
 * - Can we make 11 using [1]? No.
 * - Can we make 11 using [1, 5]? No (sum is 6).
 * - Can we make 11 using [1, 5, 11]? YES. We just pick the 11.
 * Result: True. (The remaining elements are 1 and 5 and 5, which sum to 11).
 * 
 * Example 2: nums = [1, 2, 3, 5]
 * Total Sum = 11.
 * Is it even? No. 
 * Result: False. It is impossible to split.
 */
public class PartitionEqualSubsetSum {

    /**
     * Helper to calculate the target sum using Java Streams.
     * Returns -1 if the total sum is odd (impossible to partition).
     */
    private int getTargetSum(int[] nums) {
        int totalSum = Arrays.stream(nums).sum();
        if (totalSum % 2 != 0) {
            return -1;
        }
        return totalSum / 2;
    }

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Traverse the array and recursively try both including and 
     * excluding each number until we hit our half-sum target.
     * 
     * Time Complexity: O(2^n) - Exponential branching for each element.
     * Space Complexity: O(n) - Maximum depth of the recursion tree.
     */
    public boolean canPartitionRecursive(int[] nums) {
        int target = getTargetSum(nums);
        if (target == -1) return false;
        
        return solveRecursive(nums, nums.length - 1, target);
    }

    private boolean solveRecursive(int[] nums, int index, int currentTarget) {
        // BASE CASE REASONING:
        // If the remaining target is exactly 0, it means the numbers we have 
        // picked so far perfectly weigh half of the total sum. The remaining 
        // unpicked numbers will naturally weigh the other half. We successfully 
        // split the array.
        if (currentTarget == 0) {
            return true;
        }
        
        // BASE CASE REASONING:
        // If we run out of numbers to evaluate (index < 0), and our target 
        // hasn't reached 0, it means the path of choices we took failed to 
        // reach exactly half the sum. We have no numbers left to add.
        if (index < 0) {
            return false;
        }

        // Choice 1: Do not include the current number in our half
        boolean exclude = solveRecursive(nums, index - 1, currentTarget);
        
        // Choice 2: Include it, provided it doesn't push us over the half-sum
        boolean include = false;
        if (nums[index] <= currentTarget) {
            include = solveRecursive(nums, index - 1, currentTarget - nums[index]);
        }

        return exclude || include;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: We cache our state to avoid re-evaluating the same subset combinations 
     * that result in the same remaining target.
     * 
     * Time Complexity: O(n * target) - State space is index * remaining target.
     * Space Complexity: O(n * target) - For the 2D memo array + call stack.
     */
    public boolean canPartitionMemo(int[] nums) {
        int target = getTargetSum(nums);
        if (target == -1) return false;

        Boolean[][] memo = new Boolean[nums.length][target + 1];
        return solveMemo(nums, nums.length - 1, target, memo);
    }

    private boolean solveMemo(int[] nums, int index, int currentTarget, Boolean[][] memo) {
        // BASE CASE REASONING (same physical logic as brute force):
        // Reached exactly half the sum -> success.
        if (currentTarget == 0) return true;
        // Ran out of elements -> failure.
        if (index < 0) return false;

        // Return cached result if we've seen this exact state before
        if (memo[index][currentTarget] != null) {
            return memo[index][currentTarget];
        }

        boolean exclude = solveMemo(nums, index - 1, currentTarget, memo);
        boolean include = false;
        if (nums[index] <= currentTarget) {
            include = solveMemo(nums, index - 1, currentTarget - nums[index], memo);
        }

        // Cache before returning
        memo[index][currentTarget] = exclude || include;
        return memo[index][currentTarget];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation)
     * ========================================================================
     * Idea: Build a 2D boolean array where dp[i][j] signifies if a subset 
     * of the first 'i' items can sum up to 'j'.
     * 
     * Time Complexity: O(n * target)
     * Space Complexity: O(n * target)
     */
    public boolean canPartitionTabulation(int[] nums) {
        int target = getTargetSum(nums);
        if (target == -1) return false;
        
        int n = nums.length;
        boolean[][] dp = new boolean[n + 1][target + 1];

        // BASE CASE REASONING:
        // If the required target is 0, we can always achieve it by picking 
        // 0 elements. Thus, a sum of 0 is always possible regardless of how 
        // many items we are allowed to consider. The entire first column is true.
        for (int i = 0; i <= n; i++) {
            dp[i][0] = true;
        }

        // We iterate through every item we have available (i)
        for (int i = 1; i <= n; i++) {
            
            // For this specific item, we try to form every possible target sum from 1 up to our goal (j)
            for (int j = 1; j <= target; j++) {
                
                // This is the actual weight/value of the item we are holding right now.
                // (i - 1 because our DP array is 1-indexed, but the nums array is 0-indexed)
                int currentItem = nums[i - 1];

                // QUESTION: Is the item I am holding small enough to even fit into my target sum 'j'?
                if (currentItem <= j) {
                    
                    // YES, it fits! I now have two choices. 
                    // I can either make the sum 'j' by:
                    
                    // CHOICE 1 (Exclude): 
                    // I don't use 'currentItem'. I just look directly up one row. 
                    // "Was I able to make the sum 'j' using only the previous items?"
                    boolean exclude = dp[i - 1][j];
                    
                    // CHOICE 2 (Include):
                    // I DO use 'currentItem'. This eats up 'currentItem' amount of my target sum.
                    // I look up one row, but shift left by the weight of my item.
                    // "Was I able to make the REMAINING sum (j - currentItem) using the previous items?"
                    boolean include = dp[i - 1][j - currentItem];
                    
                    // If EITHER choice worked, then I can successfully make sum 'j' with my current items.
                    dp[i][j] = exclude || include;
                    
                } else {
                    // NO, the item is strictly heavier than my target sum 'j'. 
                    // It is physically impossible to include it.
                    // My ONLY choice is to exclude it and hope the previous items could make the sum.
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
     * Idea: To calculate row 'i', we only ever look at row 'i-1'. Therefore, 
     * we can compress the 2D grid down to a single 1D array.
     * We MUST iterate backwards through the targets to ensure we don't reuse 
     * the current item multiple times (which would be Unbounded Knapsack).
     * 
     * Time Complexity: O(n * target)
     * Space Complexity: O(target) - Significantly reduced memory footprint.
     */
    public boolean canPartitionSpaceOptimized(int[] nums) {
        int target = getTargetSum(nums);
        if (target == -1) return false;

        boolean[] dp = new boolean[target + 1];
        
        // BASE CASE REASONING:
        // We can always form a sum of 0 using an empty subset.
        dp[0] = true;

        for (int num : nums) {
            // Traverse backwards to guarantee each 'num' is only used once
            for (int j = target; j >= num; j--) {
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
        var solver = new PartitionEqualSubsetSum();
        
        // Helper record for clear and maintainable test cases
        record TestCase(int[] nums, boolean expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new int[]{1, 5, 11, 5}, true),  // Sum: 22 -> Target 11. [11] and [1,5,5]
            new TestCase(new int[]{1, 2, 3, 5}, false),  // Sum: 11 -> Odd sum, impossible.
            new TestCase(new int[]{2, 2, 2, 2}, true),   // Sum: 8 -> Target 4. [2,2] and [2,2]
            new TestCase(new int[]{100}, false),         // Sum: 100 -> Target 50. Cannot split a single item.
            new TestCase(new int[]{14, 9, 8, 4, 3, 2}, true) // Sum: 40 -> Target 20. [14, 4, 2] and [9, 8, 3]
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Array   : " + Arrays.toString(tc.nums));
            System.out.println("Expected: " + tc.expected);
            
            System.out.println("Recursive (Brute) : " + solver.canPartitionRecursive(tc.nums));
            System.out.println("Memoization       : " + solver.canPartitionMemo(tc.nums));
            System.out.println("Tabulation        : " + solver.canPartitionTabulation(tc.nums));
            System.out.println("Space Optimized   : " + solver.canPartitionSpaceOptimized(tc.nums));
            System.out.println();
        }
    }
}
