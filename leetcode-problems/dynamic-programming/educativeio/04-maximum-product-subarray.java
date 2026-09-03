import java.util.Arrays;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Maximum Product Subarray
 * Given an integer array nums, find a contiguous non-empty subarray that has
 * the largest product, and return the product.
 * 
 * Constraints:
 * 1 <= nums.length <= 10^3
 * -10 <= nums[i] <= 10
 * Product of any prefix/suffix fits in a 32-bit integer.
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In a senior-level interview, acknowledge the trickiness of multiplication:
 * 
 * Q: "Are there zeros in the array?"
 * A: Yes. Zero acts as a 'circuit breaker'. Any product multiplied by zero 
 *    becomes zero, effectively resetting our contiguous subarray.
 * 
 * Q: "Are there negative numbers?"
 * A: Yes. This is the core catch of the problem. A massive negative product 
 *    can instantly become the maximum positive product if we multiply it by 
 *    another negative number.
 * 
 * Q: "Will we face integer overflow?"
 * A: Constraint guarantees any prefix/suffix product fits in a 32-bit signed int.
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "Unlike a sum where a negative number always brings our total down, in 
 * multiplication, a negative number flips the sign. If I have a current running
 * minimum (a large negative number), and the next element is negative, their
 * product suddenly becomes a large positive number.
 * 
 * Therefore, at every index 'i', to know the maximum possible product ending 
 * at 'i', I must track TWO things from index 'i-1':
 * 1. The maximum product ending at 'i-1'
 * 2. The minimum product ending at 'i-1'
 * 
 * The max product at 'i' will be the maximum of:
 * - The current number itself (starting fresh)
 * - The current number * previous max
 * - The current number * previous min"
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: nums = [2, 3, -2, 4]
 * 
 * i | num | prevMax | prevMin | currentMax                 | currentMin                 | Global Max
 * --|-----|---------|---------|----------------------------|----------------------------|-----------
 * 0 |  2  |   -     |   -     | 2                          | 2                          | 2
 * 1 |  3  |   2     |   2     | max(3, 3*2, 3*2) = 6       | min(3, 3*2, 3*2) = 3       | 6
 * 2 | -2  |   6     |   3     | max(-2, -2*6, -2*3) = -2   | min(-2, -2*6, -2*3) = -12  | 6
 * 3 |  4  |  -2     | -12     | max(4, 4*-2, 4*-12) = 4    | min(4, 4*-2, 4*-12) = -48  | 6
 * 
 * Notice at i=2, our max drops to -2, but we save the -12 as the min. If index 3 
 * was -1 instead of 4, our new max would jump to (-1 * -12) = 12!
 */
public class MaximumProductSubarray {

    /**
     * Helper Record to hold multiple return values cleanly.
     * Records (Java 14+) are immutable data carriers, perfect for DP states.
     */
    private record MinMaxState(int min, int max) {}

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Finding Min/Max ending at each index)
     * ========================================================================
     * Idea: Evaluate the state (min and max product ending at i) recursively.
     * We need a global variable to track the absolute maximum seen across all 
     * valid subarray endings.
     * 
     * Time Complexity: O(2^n) without memoization in some setups, but here we 
     * are just linearly traversing backward doing O(1) work. Still, it forces 
     * a deep call stack.
     * Space Complexity: O(n) for the recursion stack.
     */
    private int globalMaxRecursive = Integer.MIN_VALUE;

    public int maxProductRecursive(int[] nums) {
        if (nums == null || nums.length == 0) return 0;
        globalMaxRecursive = Integer.MIN_VALUE;
        getMinMaxRecursive(nums, nums.length - 1);
        return globalMaxRecursive;
    }

    private MinMaxState getMinMaxRecursive(int[] nums, int index) {
        // BASE CASE REASONING: 
        // If we have reached the very first element (index 0), the only possible 
        // contiguous subarray ending here is just the element itself.
        // Therefore, both the maximum and minimum product ending at index 0 
        // is exactly nums[0].
        if (index == 0) {
            globalMaxRecursive = Math.max(globalMaxRecursive, nums[0]);
            return new MinMaxState(nums[0], nums[0]);
        }

        MinMaxState prevState = getMinMaxRecursive(nums, index - 1);

        int current = nums[index];
        int prod1 = current * prevState.max();
        int prod2 = current * prevState.min();

        // The current max could be starting fresh from current, or extending previous
        int currentMax = Math.max(current, Math.max(prod1, prod2));
        int currentMin = Math.min(current, Math.min(prod1, prod2));

        globalMaxRecursive = Math.max(globalMaxRecursive, currentMax);

        return new MinMaxState(currentMin, currentMax);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the MinMaxState for each index so we don't recalculate it.
     * 
     * Time Complexity: O(n)
     * Space Complexity: O(n) for the recursion stack + memo array.
     */
    private int globalMaxMemo = Integer.MIN_VALUE;

    public int maxProductMemo(int[] nums) {
        if (nums == null || nums.length == 0) return 0;
        globalMaxMemo = Integer.MIN_VALUE;
        
        MinMaxState[] memo = new MinMaxState[nums.length];
        getMinMaxMemo(nums, nums.length - 1, memo);
        
        return globalMaxMemo;
    }

    private MinMaxState getMinMaxMemo(int[] nums, int index, MinMaxState[] memo) {
        // BASE CASE REASONING:
        // Same as recursion. At the beginning of the array, a subarray ending 
        // at index 0 can only contain nums[0]. The highest and lowest it can be
        // is nums[0].
        if (index == 0) {
            globalMaxMemo = Math.max(globalMaxMemo, nums[0]);
            return new MinMaxState(nums[0], nums[0]);
        }

        if (memo[index] != null) return memo[index];

        MinMaxState prevState = getMinMaxMemo(nums, index - 1, memo);

        int current = nums[index];
        int prod1 = current * prevState.max();
        int prod2 = current * prevState.min();

        int currentMax = Math.max(current, Math.max(prod1, prod2));
        int currentMin = Math.min(current, Math.min(prod1, prod2));

        globalMaxMemo = Math.max(globalMaxMemo, currentMax);
        
        memo[index] = new MinMaxState(currentMin, currentMax);
        return memo[index];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation)
     * ========================================================================
     * Idea: Build two DP arrays: maxDP[] and minDP[]. Process left to right.
     * 
     * Time Complexity: O(n) - Single pass.
     * Space Complexity: O(n) - Because we allocate two arrays of size n.
     */
    public int maxProductTabulation(int[] nums) {
        int n = nums.length;
        if (n == 0) return 0;

        int[] maxDP = new int[n];
        int[] minDP = new int[n];

        // BASE CASE REASONING:
        // A subarray ending at the very first element (index 0) has a length of 1.
        // Therefore, the max product and min product ending here are both just nums[0].
        maxDP[0] = nums[0];
        minDP[0] = nums[0];
        
        int result = nums[0];

        for (int i = 1; i < n; i++) {
            int current = nums[i];
            int p1 = current * maxDP[i - 1];
            int p2 = current * minDP[i - 1];

            maxDP[i] = Math.max(current, Math.max(p1, p2));
            minDP[i] = Math.min(current, Math.min(p1, p2));

            result = Math.max(result, maxDP[i]);
        }

        return result;
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In tabulation, calculating index 'i' only requires the states 
     * from index 'i-1'. We can discard everything else and just keep track 
     * of the previous max and previous min in two variables.
     * 
     * Time Complexity: O(n) - Single pass.
     * Space Complexity: O(1) - Constant auxiliary space.
     */
    public int maxProductSpaceOptimized(int[] nums) {
        if (nums == null || nums.length == 0) return 0;

        // BASE CASE REASONING:
        // Before we iterate through the rest of the array, we 'seed' our variables 
        // with the first element. The max/min product of a subarray ending at 
        // the first element is the element itself. Our global best is also this element.
        int currentMax = nums[0];
        int currentMin = nums[0];
        int globalMax = nums[0];

        // Start from index 1 since index 0 is our base case initialization
        for (int i = 1; i < nums.length; i++) {
            int current = nums[i];

            // If the current number is negative, it will swap the magnitudes 
            // of our max and min when multiplied. A trick is to literally swap 
            // the max and min variables before multiplying.
            if (current < 0) {
                int temp = currentMax;
                currentMax = currentMin;
                currentMin = temp;
            }

            // Do we start a new subarray here, or extend the previous one?
            currentMax = Math.max(current, currentMax * current);
            currentMin = Math.min(current, currentMin * current);

            // Update the absolute best we've seen so far
            globalMax = Math.max(globalMax, currentMax);
        }

        return globalMax;
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new MaximumProductSubarray();
        
        // Edge cases and standard cases mapped out
        int[][] testCases = {
            {2, 3, -2, 4},       // Expected: 6  (Subarray: [2,3])
            {-2, 0, -1},         // Expected: 0  (Subarray: [0])
            {-2, 3, -4},         // Expected: 24 (Subarray: [-2,3,-4])
            {0, 2},              // Expected: 2  (Subarray: [2])
            {-2}                 // Expected: -2 (Subarray: [-2])
        };
        
        for (int i = 0; i < testCases.length; i++) {
            int[] nums = testCases[i];
            System.out.println("---- Test Case " + (i + 1) + ": " + Arrays.toString(nums) + " ----");
            System.out.println("Recursive (Brute) : " + solver.maxProductRecursive(nums));
            System.out.println("Memoization       : " + solver.maxProductMemo(nums));
            System.out.println("Tabulation        : " + solver.maxProductTabulation(nums));
            System.out.println("Space Optimized   : " + solver.maxProductSpaceOptimized(nums));
            System.out.println();
        }
    }
}
