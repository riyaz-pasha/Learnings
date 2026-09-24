import java.util.Arrays;

/**
 * ============================================================================
 * PROBLEM STATEMENT: House Robber
 * As a skilled thief, you are planning to rob multiple houses on a street.
 * You cannot rob adjacent houses due to connected security systems.
 * Given an integer array nums representing the amount of money in each house,
 * return the maximum amount of money you can successfully steal.
 * 
 * Constraints: 1 <= nums.length <= 10^3, 0 <= nums[i] <= 1000
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * For a senior engineering interview, start by clarifying edge cases and system 
 * constraints before writing any code:
 * 
 * Q: "Can the array be empty?"
 * A: Constraint says 1 <= nums.length, so we are guaranteed at least one house.
 * 
 * Q: "Can house values be negative?"
 * A: Constraint says 0 <= nums[i], so no debt houses. We always want to 
 *    maximize positive gains.
 * 
 * Q: "Will the maximum total sum fit in a standard 32-bit integer?"
 * A: The maximum possible sum is roughly taking every other house.
 *    500 houses * 1000 money = 500,000. This easily fits inside a standard 
 *    Java `int` (up to ~2.1 billion). No need for `long`.
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "At any given house 'i', I have exactly two mutually exclusive choices:
 *  1. I ROB house 'i': This means I cannot have robbed house 'i-1'. My total 
 *     is the money at house 'i' plus the maximum money I could rob up to 'i-2'.
 *  2. I SKIP house 'i': My total is simply the maximum money I could rob 
 *     up to 'i-1'.
 * 
 * Since I want the maximum possible haul, I take the max of these two choices.
 * Because the decision at 'i' relies on overlapping subproblems (computing 
 * 'i-1' and 'i-2'), this is a classic Dynamic Programming problem."
 *
 * ----------------------------------------------------------------------------
 * 3. KEY OBSERVATIONS & INTUITION
 * ----------------------------------------------------------------------------
 * Recurrence Relation:
 * rob(i) = Math.max( rob(i - 1), rob(i - 2) + nums[i] )
 * 
 * Base Cases:
 * - rob(0): Only one house to rob -> nums[0]
 * - rob(1): Two houses available -> Math.max(nums[0], nums[1])
 *
 * ----------------------------------------------------------------------------
 * 4. VISUALIZATION & TRACING (Example: nums = [2, 7, 9, 3, 1])
 * ----------------------------------------------------------------------------
 * Let's trace the space-optimized DP approach:
 * 
 * Index | Money | Choice 1 (Skip: take i-1) | Choice 2 (Rob: i-2 + nums[i]) | Max
 * --------------------------------------------------------------------------------
 *   0   |   2   |            0              |              2                |  2
 *   1   |   7   |            2              |              7                |  7
 *   2   |   9   |            7              |           2 + 9 = 11          | 11
 *   3   |   3   |           11              |           7 + 3 = 10          | 11
 *   4   |   1   |           11              |          11 + 1 = 12          | 12
 * 
 * Final Answer: 12 (Robbing houses at index 0, 2, and 4: 2 + 9 + 1)
 */
public class HouseRobber {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Directly evaluate the "Rob vs Skip" decision tree from the end 
     * of the array backwards.
     * 
     * Time Complexity: O(2^n) - The recursion tree branches twice at each step.
     * Space Complexity: O(n) - Maximum depth of the recursion call stack.
     * 
     * NOTE: This will Time Limit Exceed (TLE) on large inputs, but forms 
     * the critical baseline for DP.
     */
    public int robRecursive(int[] nums) {
        return solveRecursive(nums, nums.length - 1);
    }

    private int solveRecursive(int[] nums, int index) {
        // Base cases
        if (index < 0) return 0;
        if (index == 0) return nums[0];

        // Decision: Skip current house vs Rob current house
        int skip = solveRecursive(nums, index - 1);
        int rob = solveRecursive(nums, index - 2) + nums[index];

        return Math.max(skip, rob);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: We are recalculating the same indexes repeatedly in the brute force
     * approach. Let's cache the maximum amount we can rob up to each index.
     * 
     * Time Complexity: O(n) - We calculate the answer for each index exactly once.
     * Space Complexity: O(n) - Call stack depth + memoization array.
     */
    public int robMemo(int[] nums) {
        // Use an array instead of HashMap for primitive types for better performance
        int[] memo = new int[nums.length];
        Arrays.fill(memo, -1); // -1 indicates uncalculated state
        
        return solveMemo(nums, nums.length - 1, memo);
    }

    private int solveMemo(int[] nums, int index, int[] memo) {
        if (index < 0) return 0;
        if (index == 0) return nums[0];

        // Pruning: Return cached result if we already computed it
        if (memo[index] != -1) {
            return memo[index];
        }

        int skip = solveMemo(nums, index - 1, memo);
        int rob = solveMemo(nums, index - 2, memo) + nums[index];

        // Cache the result before returning
        memo[index] = Math.max(skip, rob);
        return memo[index];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation)
     * ========================================================================
     * Idea: Avoid the recursion stack overhead entirely. Build an array where 
     * dp[i] stores the max money robbed up to house i.
     * 
     * Time Complexity: O(n) - Single pass through the array.
     * Space Complexity: O(n) - For the DP array.
     */
    public int robTabulation(int[] nums) {
        int n = nums.length;
        if (n == 1) return nums[0];
        
        int[] dp = new int[n];
        
        // Base cases initialized in the DP table
        dp[0] = nums[0];
        dp[1] = Math.max(nums[0], nums[1]);
        
        // Iteratively build the optimal solutions for larger subarrays
        for (int i = 2; i < n; i++) {
            dp[i] = Math.max(dp[i - 1], dp[i - 2] + nums[i]);
        }
        
        return dp[n - 1];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In our tabulation approach, calculating dp[i] only requires dp[i-1] 
     * and dp[i-2]. We don't need the entire array history, just the last two 
     * variables.
     * 
     * Time Complexity: O(n) - Single pass.
     * Space Complexity: O(1) - Constant auxiliary space.
     */
    public int robSpaceOptimized(int[] nums) {
        int n = nums.length;
        if (n == 1) return nums[0];

        // Utilizing 'var' (Java 10+) for cleaner variable declarations
        var twoHousesBack = 0;
        var oneHouseBack = 0;
        
        for (int money : nums) {
            // temp represents the decision at current house 'i'
            var currentMax = Math.max(oneHouseBack, twoHousesBack + money);
            
            // Shift our two-variable window forward
            twoHousesBack = oneHouseBack;
            oneHouseBack = currentMax;
        }
        
        // oneHouseBack eventually holds the max for the entire array
        return oneHouseBack;
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new HouseRobber();
        
        // Use Java Collections/Streams just for clean test case execution
        // Arrays.asList allows us to test multiple scenarios easily
        var testCases = Arrays.asList(
            new int[]{1, 2, 3, 1},          // Expected: 4
            new int[]{2, 7, 9, 3, 1},       // Expected: 12
            new int[]{0},                   // Expected: 0
            new int[]{2, 1, 1, 2}           // Expected: 4
        );
        
        for (int i = 0; i < testCases.size(); i++) {
            int[] nums = testCases.get(i);
            System.out.println("---- Test Case " + (i + 1) + ": " + Arrays.toString(nums) + " ----");
            System.out.println("Recursive (Brute) : " + solver.robRecursive(nums));
            System.out.println("Memoization       : " + solver.robMemo(nums));
            System.out.println("Tabulation        : " + solver.robTabulation(nums));
            System.out.println("Space Optimized   : " + solver.robSpaceOptimized(nums));
            System.out.println();
        }
    }
}
