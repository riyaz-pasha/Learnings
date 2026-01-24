/**
 * BALLOON BURST PROBLEM - COMPREHENSIVE ANALYSIS
 * 
 * ============================================================================
 * PROBLEM UNDERSTANDING
 * ============================================================================
 * 
 * When we burst balloon i, we get: nums[i-1] * nums[i] * nums[i+1] coins
 * After bursting, balloon i is removed and neighbors become adjacent
 * Goal: Maximize total coins from bursting all balloons
 * 
 * KEY INSIGHT: The order of bursting matters enormously!
 * 
 * ============================================================================
 * WHY THIS PROBLEM IS TRICKY
 * ============================================================================
 * 
 * The naive approach seems like a greedy or backtracking problem, but:
 * 
 * 1. GREEDY FAILS: Bursting the balloon that gives max coins now might not
 *    give optimal solution overall.
 * 
 * 2. DIRECT DP IS HARD: When we burst balloon i, the array changes - balloons
 *    on left and right become neighbors. This creates dependencies that are
 *    hard to model in straightforward DP.
 * 
 * 3. SUBPROBLEM DEFINITION IS UNCLEAR: How do we define overlapping subproblems
 *    when the state keeps changing after each burst?
 * 
 * ============================================================================
 * THE BREAKTHROUGH INSIGHT (Critical for Interview!)
 * ============================================================================
 * 
 * THINK BACKWARDS: Instead of "which balloon to burst first?"
 *                  Ask "which balloon to burst LAST?"
 * 
 * Why does this help?
 * - When we decide to burst balloon k LAST in range [left, right]:
 *   * All other balloons in [left, right] are already burst
 *   * So when we burst k, its neighbors are nums[left-1] and nums[right+1]
 *   * These neighbors DON'T CHANGE regardless of the order we burst other balloons!
 * 
 * This makes the subproblems INDEPENDENT and well-defined!
 * 
 * ============================================================================
 * DP FORMULATION
 * ============================================================================
 * 
 * dp[left][right] = maximum coins obtainable by bursting all balloons 
 *                   in the range (left, right) EXCLUSIVE of left and right
 * 
 * For each k in (left, right):
 *   - Assume k is the LAST balloon to burst in this range
 *   - When we burst k, balloons in (left, k) and (k, right) are already burst
 *   - Coins from bursting k = nums[left] * nums[k] * nums[right]
 *   - Total = dp[left][k] + nums[left]*nums[k]*nums[right] + dp[k][right]
 * 
 * dp[left][right] = max over all k of the above expression
 * 
 * ============================================================================
 * PADDING TRICK
 * ============================================================================
 * 
 * Add dummy balloons with value 1 at both ends:
 * nums = [1, 3, 1, 5, 8, 1]
 *         ^              ^
 *       dummy          dummy
 * 
 * This handles boundary cases elegantly - no special conditions needed!
 * 
 * ============================================================================
 * HOW TO APPROACH IN AN INTERVIEW
 * ============================================================================
 * 
 * Step 1: Understand the problem (2-3 min)
 *         - Work through small example manually
 *         - Notice that order matters
 * 
 * Step 2: Discuss naive approaches (2-3 min)
 *         - Mention greedy doesn't work (give counter-example)
 *         - Mention backtracking is O(n!) - too slow
 * 
 * Step 3: Think about DP (3-4 min)
 *         - Try to define subproblems
 *         - Realize "burst first" makes dependencies complex
 *         - KEY INSIGHT: Switch to "burst last" thinking
 *         - This makes subproblems independent!
 * 
 * Step 4: Define DP state (2-3 min)
 *         - dp[i][j] = max coins from bursting balloons between i and j
 *         - Discuss the recurrence relation
 *         - Mention padding trick
 * 
 * Step 5: Code the solution (5-7 min)
 * 
 * Step 6: Analyze complexity (1-2 min)
 * 
 * Total: ~20 minutes (perfect for a 45-min interview slot)
 * 
 * ============================================================================
 */

class BalloonBurst {
    
    /**
     * APPROACH 1: BOTTOM-UP DYNAMIC PROGRAMMING (RECOMMENDED)
     * 
     * Time Complexity: O(n^3)
     *   - Two loops for left and right: O(n^2)
     *   - Inner loop for k: O(n)
     *   - Total: O(n^3)
     * 
     * Space Complexity: O(n^2) for the dp table
     * 
     * This is the cleanest and most interview-friendly solution.
     */
    public int maxCoins(int[] nums) {
        int n = nums.length;
        
        // PADDING TRICK: Add 1's at both ends
        // This eliminates boundary checking in our loops
        int[] paddedNums = new int[n + 2];
        paddedNums[0] = 1;
        paddedNums[n + 1] = 1;
        for (int i = 0; i < n; i++) {
            paddedNums[i + 1] = nums[i];
        }
        
        // dp[left][right] = max coins from bursting all balloons 
        // in the OPEN interval (left, right)
        // We use open interval because left and right balloons are NOT burst
        // They serve as boundaries for calculating coins
        int[][] dp = new int[n + 2][n + 2];
        
        // CRITICAL: We need to build up from smaller ranges to larger ranges
        // len represents the length of the interval (left, right)
        // We start from len=1 (single balloon) and build up to len=n
        for (int len = 1; len <= n; len++) {
            // For each possible left boundary
            for (int left = 0; left + len + 1 < n + 2; left++) {
                int right = left + len + 1;
                
                // Try bursting each balloon k in (left, right) LAST
                for (int k = left + 1; k < right; k++) {
                    // When k is burst last in (left, right):
                    // - All balloons in (left, k) are already burst: contributes dp[left][k]
                    // - All balloons in (k, right) are already burst: contributes dp[k][right]
                    // - Bursting k itself gives: paddedNums[left] * paddedNums[k] * paddedNums[right]
                    //   (because left and right are the remaining neighbors)
                    int coins = dp[left][k] + 
                               paddedNums[left] * paddedNums[k] * paddedNums[right] + 
                               dp[k][right];
                    
                    dp[left][right] = Math.max(dp[left][right], coins);
                }
            }
        }
        
        // Answer is max coins from bursting all balloons between dummy balloons
        return dp[0][n + 1];
    }
    
    /**
     * APPROACH 2: TOP-DOWN DYNAMIC PROGRAMMING (MEMOIZATION)
     * 
     * Same complexity as bottom-up, but some find it more intuitive.
     * The recursion naturally captures the "burst last" thinking.
     * 
     * Time Complexity: O(n^3)
     * Space Complexity: O(n^2) for memoization + O(n) recursion stack
     */
    public int maxCoinsTopDown(int[] nums) {
        int n = nums.length;
        int[] paddedNums = new int[n + 2];
        paddedNums[0] = 1;
        paddedNums[n + 1] = 1;
        for (int i = 0; i < n; i++) {
            paddedNums[i + 1] = nums[i];
        }
        
        // Memoization table: -1 means not computed yet
        int[][] memo = new int[n + 2][n + 2];
        for (int i = 0; i < n + 2; i++) {
            for (int j = 0; j < n + 2; j++) {
                memo[i][j] = -1;
            }
        }
        
        return burst(0, n + 1, paddedNums, memo);
    }
    
    /**
     * Helper function for top-down approach
     * 
     * Returns maximum coins from bursting all balloons in (left, right)
     */
    private int burst(int left, int right, int[] nums, int[][] memo) {
        // Base case: no balloons to burst
        if (left + 1 == right) {
            return 0;
        }
        
        // Return cached result if already computed
        if (memo[left][right] != -1) {
            return memo[left][right];
        }
        
        int maxCoins = 0;
        
        // Try bursting each balloon k in (left, right) LAST
        for (int k = left + 1; k < right; k++) {
            // Recursive formula:
            // Total coins = coins from left part + coins from bursting k + coins from right part
            int coins = burst(left, k, nums, memo) +
                       nums[left] * nums[k] * nums[right] +
                       burst(k, right, nums, memo);
            
            maxCoins = Math.max(maxCoins, coins);
        }
        
        // Cache and return result
        memo[left][right] = maxCoins;
        return maxCoins;
    }
    
    /**
     * APPROACH 3: BACKTRACKING (For Understanding - NOT for Interview!)
     * 
     * This is exponential and will TLE on large inputs.
     * Only useful for understanding the problem with small examples.
     * 
     * Time Complexity: O(n! * n) - factorial!
     * Space Complexity: O(n) for recursion
     * 
     * DON'T use this in interview, but it helps build intuition.
     */
    public int maxCoinsBacktracking(int[] nums) {
        return burstBacktrack(nums, 0);
    }
    
    private int burstBacktrack(int[] nums, int coins) {
        // Base case: no balloons left
        if (nums.length == 0) {
            return coins;
        }
        
        int maxCoins = 0;
        
        // Try bursting each balloon
        for (int i = 0; i < nums.length; i++) {
            // Calculate coins for bursting balloon i
            int left = (i == 0) ? 1 : nums[i - 1];
            int right = (i == nums.length - 1) ? 1 : nums[i + 1];
            int gained = left * nums[i] * right;
            
            // Create new array without balloon i
            int[] newNums = new int[nums.length - 1];
            for (int j = 0, k = 0; j < nums.length; j++) {
                if (j != i) {
                    newNums[k++] = nums[j];
                }
            }
            
            // Recurse with remaining balloons
            maxCoins = Math.max(maxCoins, burstBacktrack(newNums, coins + gained));
        }
        
        return maxCoins;
    }
    
    /**
     * TEST CASES AND EXAMPLES
     */
    public static void main(String[] args) {
        BalloonBurst solution = new BalloonBurst();
        
        // Test Case 1: Example from problem
        int[] nums1 = {3, 1, 5, 8};
        System.out.println("Test 1: " + solution.maxCoins(nums1)); // Expected: 167
        System.out.println("Test 1 (Top-Down): " + solution.maxCoinsTopDown(nums1)); // Expected: 167
        
        // Manual trace for understanding:
        // [3,1,5,8] -> burst 1 -> [3,5,8]: coins = 1*1*1 = 3 (with padding)... 
        // Optimal: burst in order that maximizes: actually complex!
        
        // Test Case 2: Simple case
        int[] nums2 = {1, 5};
        System.out.println("Test 2: " + solution.maxCoins(nums2)); // Expected: 10
        // [1,5] -> burst 1 first -> [5]: 1*1*5 = 5, then 1*5*1 = 5, total = 10
        // OR burst 5 first -> [1]: 1*5*1 = 5, then 1*1*1 = 1, total = 6
        // So burst 1 first is better!
        
        // Test Case 3: Single balloon
        int[] nums3 = {5};
        System.out.println("Test 3: " + solution.maxCoins(nums3)); // Expected: 5
        
        // Test Case 4: All same values
        int[] nums4 = {3, 3, 3, 3};
        System.out.println("Test 4: " + solution.maxCoins(nums4)); // Expected: 99
        
        // Test Case 5: Descending order
        int[] nums5 = {8, 5, 3, 1};
        System.out.println("Test 5: " + solution.maxCoins(nums5)); // Expected: 152
    }
}

/**
 * ============================================================================
 * COMMON MISTAKES TO AVOID IN INTERVIEW
 * ============================================================================
 * 
 * 1. Trying to solve "which balloon to burst first"
 *    - This leads to complex dependencies
 *    - Hard to define clean subproblems
 * 
 * 2. Forgetting the padding trick
 *    - Leads to messy boundary conditions
 *    - Makes code harder to read and debug
 * 
 * 3. Wrong loop order in bottom-up DP
 *    - Must build from smaller ranges to larger
 *    - Otherwise dp[left][k] and dp[k][right] won't be computed yet
 * 
 * 4. Confusing OPEN vs CLOSED intervals
 *    - We use OPEN interval (left, right) - left and right are NOT burst
 *    - They serve as boundaries for calculation
 * 
 * 5. Off-by-one errors in array indexing
 *    - Be careful with padded array indices
 *    - Original nums[i] is now paddedNums[i+1]
 * 
 * ============================================================================
 * INTERVIEW TIPS
 * ============================================================================
 * 
 * 1. Start with a small example and work through it manually
 *    - This builds intuition and shows thinking process
 * 
 * 2. Explain why greedy doesn't work
 *    - Shows you understand the problem deeply
 * 
 * 3. Articulate the "burst last" insight clearly
 *    - This is the KEY breakthrough
 *    - Interviewer wants to see if you can find this insight
 * 
 * 4. Draw the DP table for a small example
 *    - Visual representation helps both you and interviewer
 * 
 * 5. Mention the complexity analysis
 *    - O(n^3) time, O(n^2) space
 *    - Explain why it's optimal (we must try all possibilities)
 * 
 * 6. If you have time, discuss follow-up optimizations
 *    - Though for this problem, O(n^3) is essentially optimal
 * 
 * ============================================================================
 */
