import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Min Cost Climbing Stairs
 * You are given an integer array cost where cost[i] is the cost of the i-th stair.
 * After paying the cost, you can climb 1 or 2 steps.
 * You can start from step 0 or step 1.
 * Return the minimum total cost to reach the top (the step after the last index).
 * 
 * Constraints:
 * 2 <= cost.length <= 1000
 * 0 <= cost[i] <= 999
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * Before writing code, demonstrate senior-level foresight by asking:
 * 
 * Q: "Does the 'top' mean the last element of the array, or beyond it?"
 * A: The problem states "just beyond the last stair". So if the array size 
 *    is n, the top is at index 'n'.
 * 
 * Q: "Can costs be negative?"
 * A: The constraints say 0 <= cost[i] <= 999. Since costs are non-negative, 
 *    we don't have to worry about taking extra steps just to accumulate 
 *    'negative cost' (which would complicate the DAG).
 * 
 * Q: "Will the total cost exceed a 32-bit integer?"
 * A: Max stairs = 1000, Max cost = 999. Max total cost = 999,000. 
 *    This easily fits within a standard integer.
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "My goal is to reach index 'n' (the top) with the minimum accumulated cost.
 * To physically land on step 'n', my last move was either:
 *  1. A 1-step from step 'n-1'. To do this, I must pay cost[n-1].
 *  2. A 2-step from step 'n-2'. To do this, I must pay cost[n-2].
 * 
 * Therefore, the absolute minimum cost to reach step 'n' is the minimum of 
 * those two paths:
 * minCost(n) = Math.min( minCost(n-1) + cost[n-1], minCost(n-2) + cost[n-2] )
 * 
 * This decision depends on subproblems that overlap, which points directly 
 * to Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: cost = [10, 15, 20], Target Top = index 3
 * 
 * i | Cost to step ON i | Min cost to REACH i | Calculation
 * --|-------------------|---------------------|---------------------------------
 * 0 |        10         |          0          | (Base Case: can start here free)
 * 1 |        15         |          0          | (Base Case: can start here free)
 * 2 |        20         |         10          | min(reach(1)+cost[1], reach(0)+cost[0]) -> min(0+15, 0+10) = 10
 * 3 |       (TOP)       |         15          | min(reach(2)+cost[2], reach(1)+cost[1]) -> min(10+20, 0+15) = 15
 * 
 * Global Minimum to reach top: 15 (Start at 1, step twice to top).
 */
public class MinCostClimbingStairs {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Evaluate the recurrence relation backwards from the top (index n).
     * 
     * Time Complexity: O(2^n) - Exponential branching.
     * Space Complexity: O(n) - Call stack depth.
     */
    public int minCostClimbingStairsRecursive(int[] cost) {
        // We want to reach the "top", which is exactly at index 'cost.length'
        return solveRecursive(cost, cost.length);
    }

    private int solveRecursive(int[] cost, int i) {
        // BASE CASE REASONING:
        // The problem explicitly states we can begin our climb from step 0 or 
        // step 1 *without incurring any initial cost*. We only pay when we step 
        // OFF of them. Therefore, the total accumulated cost to simply *arrive* 
        // at step 0 or step 1 is exactly 0.
        if (i == 0 || i == 1) {
            return 0;
        }

        // To reach step i, we could come from i-1 (paying cost[i-1]) 
        // or from i-2 (paying cost[i-2]).
        int costFromOneStepBack = solveRecursive(cost, i - 1) + cost[i - 1];
        int costFromTwoStepsBack = solveRecursive(cost, i - 2) + cost[i - 2];

        return Math.min(costFromOneStepBack, costFromTwoStepsBack);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the minimum cost to reach each step to eliminate redundant 
     * recursive branches.
     * 
     * Time Complexity: O(n) - Compute each step once.
     * Space Complexity: O(n) - Call stack + Memo array.
     */
    public int minCostClimbingStairsMemo(int[] cost) {
        int n = cost.length;
        // Array size is n+1 because we need to calculate up to index 'n' (the top)
        int[] memo = new int[n + 1];
        Arrays.fill(memo, -1);
        
        return solveMemo(cost, n, memo);
    }

    private int solveMemo(int[] cost, int i, int[] memo) {
        // BASE CASE REASONING:
        // Just like plain recursion, physically standing on step 0 or step 1 
        // initially costs nothing because we are placed there for free at the start.
        if (i == 0 || i == 1) {
            return 0;
        }

        if (memo[i] != -1) {
            return memo[i];
        }

        int costFromOneStepBack = solveMemo(cost, i - 1, memo) + cost[i - 1];
        int costFromTwoStepsBack = solveMemo(cost, i - 2, memo) + cost[i - 2];

        memo[i] = Math.min(costFromOneStepBack, costFromTwoStepsBack);
        return memo[i];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation)
     * ========================================================================
     * Idea: Iteratively build the solution from the ground up, avoiding the 
     * overhead of the recursion stack.
     * 
     * Time Complexity: O(n) - Single pass.
     * Space Complexity: O(n) - DP array.
     */
    public int minCostClimbingStairsTabulation(int[] cost) {
        int n = cost.length;
        int[] dp = new int[n + 1]; // dp[i] is the min cost to reach step i
        
        // BASE CASE REASONING:
        // We are allowed to bypass the "ground" and magically teleport to step 0 
        // or step 1 to begin our journey. Since we didn't climb to get there, 
        // the minimum cost to reach step 0 is 0, and the min cost to reach step 1 is 0.
        dp[0] = 0;
        dp[1] = 0;

        for (int i = 2; i <= n; i++) {
            int costFromOneStepBack = dp[i - 1] + cost[i - 1];
            int costFromTwoStepsBack = dp[i - 2] + cost[i - 2];
            
            dp[i] = Math.min(costFromOneStepBack, costFromTwoStepsBack);
        }

        return dp[n];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: Notice in the tabulation loop that calculating dp[i] strictly 
     * depends on dp[i-1] and dp[i-2]. The rest of the array history is dead 
     * memory. We can reduce our space to O(1) by keeping just two variables.
     * 
     * Time Complexity: O(n) - Single pass.
     * Space Complexity: O(1) - Constant space.
     */
    public int minCostClimbingStairsSpaceOptimized(int[] cost) {
        // BASE CASE REASONING:
        // 'twoStepsBehind' tracks the cost to reach step 0 (which is 0).
        // 'oneStepBehind' tracks the cost to reach step 1 (which is 0).
        // We are initializing our sliding window exactly at the starting blocks.
        var twoStepsBehind = 0;
        var oneStepBehind = 0;

        // Iterate up to 'n' (the top). At each iteration 'i', we decide the 
        // cheapest way to get here based on the two preceding steps.
        for (int i = 2; i <= cost.length; i++) {
            var costFromOneStepBack = oneStepBehind + cost[i - 1];
            var costFromTwoStepsBack = twoStepsBehind + cost[i - 2];
            
            var currentCost = Math.min(costFromOneStepBack, costFromTwoStepsBack);
            
            // Shift the window forward for the next iteration
            twoStepsBehind = oneStepBehind;
            oneStepBehind = currentCost;
        }

        // 'oneStepBehind' has shifted all the way to represent step 'n'
        return oneStepBehind;
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new MinCostClimbingStairs();
        
        List<int[]> testCases = Arrays.asList(
            new int[]{10, 15, 20}, // Expected: 15 (Start at 1, step twice)
            new int[]{1, 100, 1, 1, 1, 100, 1, 1, 100, 1}, // Expected: 6
            new int[]{0, 0, 0, 0}, // Expected: 0
            new int[]{999, 999} // Expected: 999 (Start at 0 or 1, pay 999 to jump off)
        );
        
        for (int i = 0; i < testCases.size(); i++) {
            int[] cost = testCases.get(i);
            System.out.println("---- Test Case " + (i + 1) + ": " + Arrays.toString(cost) + " ----");
            System.out.println("Recursive (Brute) : " + solver.minCostClimbingStairsRecursive(cost));
            System.out.println("Memoization       : " + solver.minCostClimbingStairsMemo(cost));
            System.out.println("Tabulation        : " + solver.minCostClimbingStairsTabulation(cost));
            System.out.println("Space Optimized   : " + solver.minCostClimbingStairsSpaceOptimized(cost));
            System.out.println();
        }
    }
}
