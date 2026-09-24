import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: House Robber II
 * A professional robber plans to rob houses arranged in a CIRCLE.
 * The first and last houses are neighbors. You cannot rob adjacent houses.
 * Given an integer array representing money in each house, return the max 
 * amount you can steal without alerting the police.
 * 
 * Constraints:
 * 1 <= money.length <= 10^3
 * 0 <= money[i] <= 10^3
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * Q: "What if there is only 1 house?"
 * A: The constraint says 1 <= money.length. If there's only 1 house, it has 
 *    no neighbors to trigger an alarm. We just rob it.
 * 
 * Q: "What if there are 2 houses?"
 * A: Since they are in a circle, they are adjacent to each other. We can only 
 *    rob one of them, so we pick the maximum of the two.
 * 
 * Q: "Are the values ever negative?"
 * A: Constraint says 0 <= money[i], so we don't need to worry about skipping 
 *    houses just to avoid negative debt. We always want to maximize profit.
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "The circular constraint introduces exactly one logical trap: If I choose to 
 * rob the very first house (index 0), I am strictly forbidden from robbing the 
 * very last house (index n-1), because they are physically connected. 
 * Conversely, if I rob the last house, I cannot rob the first.
 * 
 * Therefore, this problem elegantly breaks down into two separate, standard 
 * 'linear' House Robber scenarios:
 *  Scenario A: Ignore the last house. Rob the street linearly from index 0 to n-2.
 *  Scenario B: Ignore the first house. Rob the street linearly from index 1 to n-1.
 * 
 * The absolute maximum profit is simply the maximum result between Scenario A 
 * and Scenario B. This allows us to reuse standard Dynamic Programming principles."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: money = [2, 3, 2]
 * Circle means: 2 is connected to 3, 3 is connected to 2, and the 2s are connected.
 * 
 * Scenario A (Ignore last): Street becomes [2, 3] (indices 0 to 1)
 * -> Rob 3. Profit = 3.
 * 
 * Scenario B (Ignore first): Street becomes [3, 2] (indices 1 to 2)
 * -> Rob 3. Profit = 3.
 * 
 * Global Maximum = max(3, 3) = 3. 
 * (If we tried to rob both 2s, we'd get 4, but the alarm would ring since they touch).
 */
public class HouseRobberII {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Evaluate the tree from end to start for both scenarios.
     * Time Complexity: O(2^n) - Exponential branching for both paths.
     * Space Complexity: O(n) - Call stack depth.
     */
    public int robRecursive(int[] money) {
        int n = money.length;
        // BASE CASE REASONING (Global):
        // If the street only contains one single house, it is isolated. There are 
        // no neighbors to trigger an alarm. We just rob it and take the cash.
        if (n == 1) return money[0];

        // Scenario A: Start at index 0, end at index n-2
        int maxA = solveRecursive(money, 0, n - 2);
        // Scenario B: Start at index 1, end at index n-1
        int maxB = solveRecursive(money, 1, n - 1);

        return Math.max(maxA, maxB);
    }

    private int solveRecursive(int[] money, int start, int currentIndex) {
        // BASE CASE REASONING:
        // If we look backward past our starting boundary, there are no houses left 
        // in our permitted jurisdiction. We can't rob a house that isn't there, 
        // so the profit is 0.
        if (currentIndex < start) {
            return 0;
        }
        // BASE CASE REASONING:
        // If we have backed up exactly to our starting boundary, the only house 
        // available in this slice of reality is this very house. The max money 
        // is exactly what's inside it.
        if (currentIndex == start) {
            return money[start];
        }

        int skip = solveRecursive(money, start, currentIndex - 1);
        int rob = solveRecursive(money, start, currentIndex - 2) + money[currentIndex];

        return Math.max(skip, rob);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the results for both scenarios to prevent recalculation.
     * Time Complexity: O(n) - We calculate each index state once per scenario.
     * Space Complexity: O(n) - For the memo arrays and call stack.
     */
    public int robMemo(int[] money) {
        int n = money.length;
        if (n == 1) return money[0];

        int[] memoA = new int[n];
        int[] memoB = new int[n];
        Arrays.fill(memoA, -1);
        Arrays.fill(memoB, -1);

        int maxA = solveMemo(money, 0, n - 2, memoA);
        int maxB = solveMemo(money, 1, n - 1, memoB);

        return Math.max(maxA, maxB);
    }

    private int solveMemo(int[] money, int start, int currentIndex, int[] memo) {
        // BASE CASE REASONING (same physical logic as brute force)
        // No house exists beyond our boundary -> zero profit.
        if (currentIndex < start) return 0;
        // Reached the only available starting house -> its exact value.
        if (currentIndex == start) return money[start];

        if (memo[currentIndex] != -1) {
            return memo[currentIndex];
        }

        int skip = solveMemo(money, start, currentIndex - 1, memo);
        int rob = solveMemo(money, start, currentIndex - 2, memo) + money[currentIndex];

        memo[currentIndex] = Math.max(skip, rob);
        return memo[currentIndex];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation)
     * ========================================================================
     * Idea: Iteratively build the solution for both valid ranges using arrays.
     * Time Complexity: O(n)
     * Space Complexity: O(n) for the DP arrays.
     */
    public int robTabulation(int[] money) {
        int n = money.length;
        if (n == 1) return money[0];
        
        // Edge case for n = 2: We just take the max of the two
        if (n == 2) return Math.max(money[0], money[1]);

        return Math.max(
            solveTabulationLinear(money, 0, n - 2),
            solveTabulationLinear(money, 1, n - 1)
        );
    }

    private int solveTabulationLinear(int[] money, int start, int end) {
        int length = end - start + 1;
        int[] dp = new int[length];

        // BASE CASE REASONING:
        // dp[0] represents the first house in our permitted block. 
        // If our block is only 1 house long, the max is just that house.
        dp[0] = money[start];
        
        // dp[1] represents deciding between the first and second house in our block.
        // We pick the richer one.
        dp[1] = Math.max(money[start], money[start + 1]);

        for (int i = 2; i < length; i++) {
            int currentHouseMoney = money[start + i];
            dp[i] = Math.max(dp[i - 1], dp[i - 2] + currentHouseMoney);
        }

        return dp[length - 1];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: We only need the last two states for our recurrence relation. 
     * We don't need O(n) arrays. Just two variables per linear scan.
     * Time Complexity: O(n) - Two independent linear passes.
     * Space Complexity: O(1) - Constant space.
     */
    public int robSpaceOptimized(int[] money) {
        int n = money.length;
        
        // BASE CASE REASONING: 
        // Single isolated house on the street. Rob it.
        if (n == 1) return money[0];

        // We run our memory-efficient linear sweep on the two valid scenarios.
        var maxExcludingLast = solveSpaceOptimizedLinear(money, 0, n - 2);
        var maxExcludingFirst = solveSpaceOptimizedLinear(money, 1, n - 1);

        return Math.max(maxExcludingLast, maxExcludingFirst);
    }

    private int solveSpaceOptimizedLinear(int[] money, int start, int end) {
        // BASE CASE REASONING:
        // 'twoHousesBack' starts as 0 because before we look at any houses, we have 0 cash.
        // 'oneHouseBack' starts as 0 for the same reason.
        // They act as our empty pockets before we begin walking down the slice of the street.
        var twoHousesBack = 0;
        var oneHouseBack = 0;

        for (int i = start; i <= end; i++) {
            var currentMax = Math.max(oneHouseBack, twoHousesBack + money[i]);
            
            // Shift the sliding window of memory forward
            twoHousesBack = oneHouseBack;
            oneHouseBack = currentMax;
        }

        return oneHouseBack;
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new HouseRobberII();
        
        List<int[]> testCases = Arrays.asList(
            new int[]{2, 3, 2},             // Expected: 3
            new int[]{1, 2, 3, 1},          // Expected: 4
            new int[]{1, 2, 3},             // Expected: 3
            new int[]{5},                   // Expected: 5 (Base case check)
            new int[]{2, 7, 9, 3, 1},       // Expected: 11
            new int[]{0, 0}                 // Expected: 0
        );
        
        for (int i = 0; i < testCases.size(); i++) {
            int[] money = testCases.get(i);
            System.out.println("---- Test Case " + (i + 1) + ": " + Arrays.toString(money) + " ----");
            System.out.println("Recursive (Brute) : " + solver.robRecursive(money));
            System.out.println("Memoization       : " + solver.robMemo(money));
            System.out.println("Tabulation        : " + solver.robTabulation(money));
            System.out.println("Space Optimized   : " + solver.robSpaceOptimized(money));
            System.out.println();
        }
    }
}
