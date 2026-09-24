import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Minimum Cost For Tickets
 * You are given an array of strictly increasing 'days' you plan to travel on.
 * You can buy 1-day, 7-day, or 30-day passes with prices listed in 'costs'.
 * A pass covers travel for its full duration starting from the day it is bought.
 * Return the minimum total cost needed to cover all travel days.
 * 
 * Constraints:
 * 1 <= days.length <= 365
 * 1 <= days[i] <= 365
 * costs.length == 3
 * 1 <= costs[i] <= 1000
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, recognizing the directionality of the DP is key:
 * 
 * Q: "Do I have to buy a pass on a day I am NOT traveling?"
 * A: No. If day 'd' is not in the 'days' array, buying a pass on that day 
 *    would just waste money and days. We should only consider buying passes 
 *    ON the actual days we intend to travel.
 * 
 * Q: "What if a 7-day pass extends beyond my last travel day?"
 * A: That's completely fine. Sometimes buying a 7-day pass for your final 
 *    2 days of travel is cheaper than buying two 1-day passes. Our algorithm 
 *    will simply naturally truncate any "unused" coverage at the end of the year.
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "At any given travel day, I am forced to make a choice between three passes:
 *  1. Buy a 1-day pass: I pay costs[0], and I must figure out the cost for 
 *     the very next travel day.
 *  2. Buy a 7-day pass: I pay costs[1], and I skip evaluating any travel days 
 *     that fall within the next 6 days. I resume evaluation at the first 
 *     travel day >= (current_day + 7).
 *  3. Buy a 30-day pass: I pay costs[2], and I skip evaluating any travel days 
 *     that fall within the next 29 days. I resume evaluation at the first 
 *     travel day >= (current_day + 30).
 * 
 * Because evaluating later travel days requires knowing the optimal cost of 
 * the days that come after them, we have overlapping subproblems -> Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: days = [1, 4, 6, 7, 8, 20], costs = [2, 7, 15]
 * 
 * Let's trace the Bottom-Up Tabulation (Day-by-Day approach):
 * dp[i] = minimum cost to travel up to day 'i'.
 * 
 * Day 1 (Travel): 
 *   - 1-day pass covers day 1: cost = dp[0] + 2 = 2
 *   - 7-day pass covers day 1: cost = dp[0] + 7 = 7
 *   - 30-day pass covers day 1: cost = dp[0] + 15 = 15
 *   dp[1] = min(2, 7, 15) = 2
 * 
 * Days 2 & 3 (No Travel):
 *   We don't need a pass here. We just inherit the cost from the previous day.
 *   dp[2] = dp[1] = 2
 *   dp[3] = dp[2] = 2
 * 
 * Day 4 (Travel):
 *   - 1-day: dp[3] + 2 = 2 + 2 = 4
 *   - 7-day: dp[max(0, 4-7)] + 7 -> dp[0] + 7 = 7  (Bought on day 1, covers 1 to 7)
 *   - 30-day: dp[max(0, 4-30)] + 15 -> dp[0] + 15 = 15
 *   dp[4] = min(4, 7, 15) = 4
 */
public class MinimumCostForTickets {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force - Index Based)
     * ========================================================================
     * Idea: We iterate through the 'days' array. For each travel day, we branch 
     * out into 3 universes (1-day, 7-day, 30-day passes) and fast-forward the 
     * index to the next uncovered travel day.
     * 
     * Time Complexity: O(3^N) - We make 3 branches for every travel day.
     * Space Complexity: O(N) - Maximum depth of the recursion tree.
     */
    public int mincostTicketsRecursive(int[] days, int[] costs) {
        if (days == null || days.length == 0) return 0;
        return solveRecursive(days, costs, 0);
    }

    private int solveRecursive(int[] days, int[] costs, int index) {
        // BASE CASE REASONING:
        // If our index surpasses the number of travel days we have, our trip 
        // is completely over. It costs 0 dollars to travel on 0 remaining days.
        if (index >= days.length) {
            return 0;
        }

        // Universe 1: Buy a 1-day pass. 
        // It strictly covers today, so we evaluate from the very next index.
        int oneDayCost = costs[0] + solveRecursive(days, costs, index + 1);

        // Universe 2: Buy a 7-day pass.
        // We skip all indices that fall within the 7-day coverage window.
        int nextIndex7 = index;
        while (nextIndex7 < days.length && days[nextIndex7] < days[index] + 7) {
            nextIndex7++;
        }
        int sevenDayCost = costs[1] + solveRecursive(days, costs, nextIndex7);

        // Universe 3: Buy a 30-day pass.
        // We skip all indices that fall within the 30-day coverage window.
        int nextIndex30 = index;
        while (nextIndex30 < days.length && days[nextIndex30] < days[index] + 30) {
            nextIndex30++;
        }
        int thirtyDayCost = costs[2] + solveRecursive(days, costs, nextIndex30);

        // We are thrifty. Greedily pick the cheapest of the three universes.
        return Math.min(oneDayCost, Math.min(sevenDayCost, thirtyDayCost));
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the minimum cost calculated starting from a specific index 
     * in the 'days' array.
     * 
     * Time Complexity: O(N) - We evaluate each of the N travel days exactly once.
     * Space Complexity: O(N) - For the memo array + call stack.
     */
    public int mincostTicketsMemo(int[] days, int[] costs) {
        if (days == null || days.length == 0) return 0;
        
        int[] memo = new int[days.length];
        Arrays.fill(memo, -1);
        
        return solveMemo(days, costs, 0, memo);
    }

    private int solveMemo(int[] days, int[] costs, int index, int[] memo) {
        // BASE CASE (Same physical logic as brute force)
        if (index >= days.length) return 0;

        if (memo[index] != -1) {
            return memo[index];
        }

        int oneDay = costs[0] + solveMemo(days, costs, index + 1, memo);

        int next7 = index;
        while (next7 < days.length && days[next7] < days[index] + 7) next7++;
        int sevenDay = costs[1] + solveMemo(days, costs, next7, memo);

        int next30 = index;
        while (next30 < days.length && days[next30] < days[index] + 30) next30++;
        int thirtyDay = costs[2] + solveMemo(days, costs, next30, memo);

        memo[index] = Math.min(oneDay, Math.min(sevenDay, thirtyDay));
        return memo[index];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 1D)
     * ========================================================================
     * Idea: Instead of jumping between indices of the 'days' array, we walk 
     * through every calendar day from 1 up to the last travel day.
     * dp[i] signifies the minimum cost to travel up to calendar day 'i'.
     * 
     * Time Complexity: O(max_day) - Where max_day is the last value in 'days' (up to 365).
     * Space Complexity: O(max_day) - For the DP array and boolean lookup array.
     */
    public int mincostTicketsTabulation(int[] days, int[] costs) {
        if (days == null || days.length == 0) return 0;

        int lastTravelDay = days[days.length - 1];
        
        // O(1) lookup table to quickly ask: "Are we traveling on day 'i'?"
        boolean[] isTravelDay = new boolean[lastTravelDay + 1];
        for (int day : days) {
            isTravelDay[day] = true;
        }

        // dp[i] answers: "What is the absolute minimum cost to travel legally 
        // from day 1 up to calendar day 'i'?"
        int[] dp = new int[lastTravelDay + 1];

        // BASE CASE REASONING:
        // dp[0] represents day 0 (before we even start traveling). Cost is 0.
        dp[0] = 0;

        for (int i = 1; i <= lastTravelDay; i++) {
            
            // PHYSICAL CHECK: Are we even traveling today?
            if (!isTravelDay[i]) {
                // If we aren't traveling, we don't need a new pass. 
                // The total cost up to today is strictly identical to the cost 
                // up to yesterday. We just carry the value forward.
                dp[i] = dp[i - 1];
                continue;
            }
            
            // --- DETAILED TABULATION EXPLANATION ---
            // If we ARE traveling today, we MUST hold a valid pass. 
            // Let's look backward in time to see when we could have bought it:

            // UNIVERSE 1: We buy a 1-day pass today.
            // We look back exactly 1 day. We take the optimal cost up to yesterday 
            // and add the cost of a 1-day pass.
            int cost1 = dp[i - 1] + costs[0];

            // UNIVERSE 2: We bought a 7-day pass 7 days ago.
            // It would perfectly cover us up through today. We look back 7 days.
            // (Math.max prevents us from looking into negative days before the trip began).
            int cost7 = dp[Math.max(0, i - 7)] + costs[1];

            // UNIVERSE 3: We bought a 30-day pass 30 days ago.
            // It would perfectly cover us up through today. We look back 30 days.
            int cost30 = dp[Math.max(0, i - 30)] + costs[2];

            // We greedily register whichever universe gives us the cheapest total cost.
            dp[i] = Math.min(cost1, Math.min(cost7, cost30));
        }

        // The answer sits at the very last travel day of our trip.
        return dp[lastTravelDay];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: Look closely at the Tabulation loop above. To calculate `dp[i]`, 
     * the FURTHEST we ever look backward is 30 days (`dp[i - 30]`). 
     * If the trip is 365 days long, the first 335 days of history are dead memory!
     * 
     * We can collapse the array into a fixed 31-day sliding window using Modulo 
     * arithmetic. (Size 31 accommodates the current day + 30 previous days).
     * 
     * Time Complexity: O(max_day)
     * Space Complexity: O(31) = O(1) strictly constant space.
     */
    public int mincostTicketsSpaceOptimized(int[] days, int[] costs) {
        if (days == null || days.length == 0) return 0;

        int lastTravelDay = days[days.length - 1];
        
        boolean[] isTravelDay = new boolean[lastTravelDay + 1];
        for (int day : days) {
            isTravelDay[day] = true;
        }

        // A rolling circular array of strictly size 31.
        int[] dp = new int[31];
        
        // BASE CASE REASONING:
        // Arrays automatically initialize to 0. dp[0] correctly acts as our 
        // 0-cost baseline for the Math.max(0, i - X) calls early in the loop.

        for (int i = 1; i <= lastTravelDay; i++) {
            
            if (!isTravelDay[i]) {
                // We use Modulo 31 to perfectly map the calendar day 'i' into our rolling window.
                dp[i % 31] = dp[(i - 1) % 31];
            } else {
                int cost1 = dp[(i - 1) % 31] + costs[0];
                int cost7 = dp[Math.max(0, i - 7) % 31] + costs[1];
                int cost30 = dp[Math.max(0, i - 30) % 31] + costs[2];

                dp[i % 31] = Math.min(cost1, Math.min(cost7, cost30));
            }
        }

        // Return the boolean value mapped to the absolute end of the trip.
        return dp[lastTravelDay % 31];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new MinimumCostForTickets();
        
        record TestCase(int[] days, int[] costs, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new int[]{1, 4, 6, 7, 8, 20}, new int[]{2, 7, 15}, 11),
            // Optimal: 1-day pass on day 1 (2), 7-day pass on day 3 (7), 1-day pass on day 20 (2). Total 11.
            
            new TestCase(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 30, 31}, new int[]{2, 7, 15}, 17),
            // Optimal: 30-day pass on day 1 (15), 1-day pass on day 31 (2). Total 17.
            
            new TestCase(new int[]{1}, new int[]{2, 7, 15}, 2)
            // Edge Case: Just one day of travel.
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Days    : " + Arrays.toString(tc.days));
            System.out.println("Costs   : " + Arrays.toString(tc.costs));
            System.out.println("Expected: " + tc.expected);
            
            System.out.println("Recursive (Brute) : " + solver.mincostTicketsRecursive(tc.days, tc.costs));
            System.out.println("Memoization       : " + solver.mincostTicketsMemo(tc.days, tc.costs));
            System.out.println("Tabulation 1D     : " + solver.mincostTicketsTabulation(tc.days, tc.costs));
            System.out.println("Space Optimized   : " + solver.mincostTicketsSpaceOptimized(tc.days, tc.costs));
            System.out.println();
        }
    }
}
