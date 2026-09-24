/**
 * MINIMUM COST TO CUT A STICK - COMPREHENSIVE ANALYSIS
 * =====================================================
 * 
 * PROBLEM UNDERSTANDING:
 * ---------------------
 * Given:
 * - A wooden stick of length n (labeled 0 to n)
 * - An array cuts[] representing positions where we can cut
 * - Cost of a cut = current length of the stick being cut
 * - We can perform cuts in ANY order
 * 
 * Find: Minimum total cost to make all cuts
 * 
 * CRITICAL INSIGHT:
 * The order of cuts matters! Different orders lead to different costs.
 * 
 * EXAMPLE WALKTHROUGH:
 * n = 7, cuts = [1, 3, 4, 5]
 * Stick: 0---1---2---3---4---5---6---7
 * 
 * Order 1: [1, 3, 4, 5] (given order)
 * - Cut at 1: length=7, cost=7 → sticks: [0-1] and [1-7]
 * - Cut at 3: length=6, cost=6 → sticks: [0-1], [1-3], [3-7]
 * - Cut at 4: length=4, cost=4 → sticks: [0-1], [1-3], [3-4], [4-7]
 * - Cut at 5: length=3, cost=3 → sticks: [0-1], [1-3], [3-4], [4-5], [5-7]
 * Total cost = 7 + 6 + 4 + 3 = 20
 * 
 * Order 2: [3, 5, 1, 4] (optimal)
 * - Cut at 3: length=7, cost=7 → sticks: [0-3] and [3-7]
 * - Cut at 5: length=4, cost=4 → sticks: [0-3], [3-5], [5-7]
 * - Cut at 1: length=3, cost=3 → sticks: [0-1], [1-3], [3-5], [5-7]
 * - Cut at 4: length=2, cost=2 → sticks: [0-1], [1-3], [3-4], [4-5], [5-7]
 * Total cost = 7 + 4 + 3 + 2 = 16 (OPTIMAL!)
 * 
 * WHY THIS IS A DP PROBLEM:
 * - We have choices (order of cuts)
 * - Choices affect future subproblems
 * - Overlapping subproblems (same stick segments appear multiple times)
 * - Optimal substructure (optimal solution uses optimal subsolutions)
 * 
 * INTERVIEW APPROACH - HOW TO THINK ABOUT THIS:
 * ==============================================
 * 
 * STEP 1: RECOGNIZE THE PROBLEM TYPE
 * "This looks like an interval DP problem. We have a range and we're deciding
 * where to partition it. Similar to Matrix Chain Multiplication or Burst Balloons."
 * 
 * STEP 2: IDENTIFY THE STATE
 * Think backwards! Instead of "which cut to make first", think:
 * "If I have a stick from position i to j, what's the min cost to make all cuts in between?"
 * 
 * State: dp[i][j] = minimum cost to make all cuts between positions i and j
 * 
 * STEP 3: DERIVE THE RECURRENCE
 * For a stick from i to j, we must choose WHICH cut to make LAST.
 * Why last? Because when we make a cut, it splits into independent subproblems.
 * 
 * For each possible last cut k between i and j:
 * - Cost of this cut = j - i (length of current stick)
 * - Cost of left part = dp[i][k] (all cuts between i and k)
 * - Cost of right part = dp[k][j] (all cuts between k and j)
 * - Total = (j - i) + dp[i][k] + dp[k][j]
 * 
 * dp[i][j] = min over all k of: (j - i) + dp[i][k] + dp[k][j]
 * 
 * STEP 4: HANDLE BOUNDARIES
 * Problem: cuts array doesn't include 0 and n
 * Solution: Add 0 at start and n at end of cuts array
 * This way, we can use indices directly
 * 
 * WHAT TO SAY IN INTERVIEW:
 * "This is a classic interval DP problem. I'll think of it as: given a segment,
 * I try every possible last cut. The cost is the segment length plus the costs
 * of the two resulting subsegments. I'll add 0 and n to the cuts array as
 * boundaries to simplify indexing."
 * 
 * COMPLEXITY ANALYSIS:
 * -------------------
 * Time Complexity: O(m³) where m = cuts.length
 * - We have O(m²) states (all pairs i, j)
 * - For each state, we try O(m) possible cuts
 * 
 * Space Complexity: O(m²) for the DP table
 * 
 * WHY NOT GREEDY?
 * We can't use greedy (e.g., "always cut in the middle" or "cut smallest first")
 * because the optimal choice depends on ALL future cuts in complex ways.
 * 
 * SIMILAR PROBLEMS:
 * - Matrix Chain Multiplication (LeetCode 312)
 * - Burst Balloons (LeetCode 312)
 * - Minimum Cost Tree From Leaf Values (LeetCode 1130)
 * - Palindrome Partitioning II (LeetCode 132)
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class MinimumCostToCutStick {
    
    /**
     * METHOD 1: TOP-DOWN DP WITH MEMOIZATION - MOST INTUITIVE
     * =========================================================
     * 
     * DETAILED WALKTHROUGH with n=7, cuts=[1,3,4,5]:
     * 
     * Step 1: Prepare cuts array
     * Original: [1, 3, 4, 5]
     * After sort and adding boundaries: [0, 1, 3, 4, 5, 7]
     * 
     * Step 2: Call minCost(0, 5) → "What's min cost for stick from index 0 to 5?"
     * This represents the full stick from position 0 to 7
     * 
     * Step 3: Try each cut as the LAST cut:
     * 
     * Try cut at index 1 (position 1):
     *   Cost = (7-0) + minCost(0,1) + minCost(1,5)
     *        = 7 + 0 + minCost(1,5)
     *   
     *   minCost(1,5) → stick from 1 to 7, must cut at 3,4,5
     *     Try cut at index 2 (position 3):
     *       Cost = (7-1) + minCost(1,2) + minCost(2,5)
     *            = 6 + 0 + minCost(2,5)
     *       
     *       minCost(2,5) → stick from 3 to 7, must cut at 4,5
     *         Try cut at index 3 (position 4):
     *           Cost = (7-3) + minCost(2,3) + minCost(3,5)
     *                = 4 + 0 + minCost(3,5)
     *           
     *           minCost(3,5) → stick from 4 to 7, must cut at 5
     *             Try cut at index 4 (position 5):
     *               Cost = (7-4) + minCost(3,4) + minCost(4,5)
     *                    = 3 + 0 + 0 = 3
     *           
     *           So minCost(3,5) = 3
     *           Back to minCost(2,5) = 4 + 0 + 3 = 7
     *         
     *         ... (try other cuts for minCost(2,5))
     *         Best minCost(2,5) = 4 (cutting at 5 first, then 4)
     *       
     *       Back to minCost(1,5) = 6 + 0 + 4 = 10
     *     
     *     ... (try other cuts for minCost(1,5))
     *   
     *   ... (continue for all possible first cuts)
     * 
     * The algorithm explores all possibilities and memoizes results.
     */
    public static int minCost(int n, int[] cuts) {
        // Step 1: Add boundaries and sort
        int m = cuts.length;
        int[] newCuts = new int[m + 2];
        newCuts[0] = 0;
        newCuts[m + 1] = n;
        System.arraycopy(cuts, 0, newCuts, 1, m);
        Arrays.sort(newCuts);
        
        // Step 2: Create memoization table
        // memo[i][j] = min cost to make all cuts between index i and j
        // -1 means not computed yet
        int[][] memo = new int[m + 2][m + 2];
        for (int[] row : memo) {
            Arrays.fill(row, -1);
        }
        
        // Step 3: Solve
        return minCostHelper(newCuts, 0, m + 1, memo);
    }
    
    /**
     * RECURSIVE HELPER WITH MEMOIZATION
     * 
     * @param cuts - sorted array with boundaries [0, cut1, cut2, ..., n]
     * @param left - left index in cuts array
     * @param right - right index in cuts array
     * @param memo - memoization table
     * @return minimum cost to make all cuts between cuts[left] and cuts[right]
     */
    private static int minCostHelper(int[] cuts, int left, int right, int[][] memo) {
        // Base case: no cuts between left and right
        // This happens when right - left <= 1 (adjacent positions in cuts array)
        if (right - left <= 1) {
            return 0;
        }
        
        // Check memo
        if (memo[left][right] != -1) {
            return memo[left][right];
        }
        
        // Try each possible LAST cut
        int minCost = Integer.MAX_VALUE;
        for (int mid = left + 1; mid < right; mid++) {
            // If we make the cut at cuts[mid] as the LAST cut:
            // - Current stick length = cuts[right] - cuts[left]
            // - Cost of left part = minCostHelper(left, mid)
            // - Cost of right part = minCostHelper(mid, right)
            int cost = (cuts[right] - cuts[left]) + 
                       minCostHelper(cuts, left, mid, memo) + 
                       minCostHelper(cuts, mid, right, memo);
            minCost = Math.min(minCost, cost);
        }
        
        // Memoize and return
        memo[left][right] = minCost;
        return minCost;
    }
    
    /**
     * METHOD 2: BOTTOM-UP DP - MORE EFFICIENT
     * ========================================
     * 
     * ADVANTAGE: No recursion overhead, better cache locality
     * 
     * KEY IDEA: Build solutions from smaller intervals to larger ones
     * 
     * ITERATION ORDER:
     * We need to compute dp[i][j] using dp[i][k] and dp[k][j] where i < k < j
     * So we must compute smaller intervals before larger ones.
     * 
     * Strategy: Iterate by interval length (len = right - left)
     * - len = 2: intervals of 2 elements (base case, = 0)
     * - len = 3: intervals of 3 elements
     * - ...
     * - len = m+2: full interval
     */
    public static int minCostBottomUp(int n, int[] cuts) {
        // Step 1: Prepare cuts array
        int m = cuts.length;
        int[] newCuts = new int[m + 2];
        newCuts[0] = 0;
        newCuts[m + 1] = n;
        System.arraycopy(cuts, 0, newCuts, 1, m);
        Arrays.sort(newCuts);
        
        // Step 2: Create DP table
        int[][] dp = new int[m + 2][m + 2];
        
        // Step 3: Fill DP table
        // Iterate by interval length
        for (int len = 2; len <= m + 1; len++) {
            for (int left = 0; left + len <= m + 1; left++) {
                int right = left + len;
                
                // Base case handled by initialization (dp[i][i+1] = 0)
                if (len == 2) {
                    dp[left][right] = 0;
                    continue;
                }
                
                // Try each cut as the last cut
                dp[left][right] = Integer.MAX_VALUE;
                for (int mid = left + 1; mid < right; mid++) {
                    int cost = (newCuts[right] - newCuts[left]) + 
                               dp[left][mid] + dp[mid][right];
                    dp[left][right] = Math.min(dp[left][right], cost);
                }
            }
        }
        
        return dp[0][m + 1];
    }
    
    /**
     * METHOD 3: WITH DETAILED TRACKING (FOR UNDERSTANDING/DEBUGGING)
     * ==============================================================
     */
    public static int minCostWithTracking(int n, int[] cuts) {
        int m = cuts.length;
        int[] newCuts = new int[m + 2];
        newCuts[0] = 0;
        newCuts[m + 1] = n;
        System.arraycopy(cuts, 0, newCuts, 1, m);
        Arrays.sort(newCuts);
        
        System.out.println("Prepared cuts array: " + Arrays.toString(newCuts));
        
        int[][] dp = new int[m + 2][m + 2];
        
        for (int len = 2; len <= m + 1; len++) {
            System.out.println("\n=== Processing intervals of length " + len + " ===");
            
            for (int left = 0; left + len <= m + 1; left++) {
                int right = left + len;
                
                if (len == 2) {
                    dp[left][right] = 0;
                    System.out.println("dp[" + left + "][" + right + "] = 0 (base case)");
                    continue;
                }
                
                System.out.println("\nComputing dp[" + left + "][" + right + "] " +
                                 "(stick from " + newCuts[left] + " to " + newCuts[right] + "):");
                
                dp[left][right] = Integer.MAX_VALUE;
                int bestMid = -1;
                
                for (int mid = left + 1; mid < right; mid++) {
                    int stickLength = newCuts[right] - newCuts[left];
                    int leftCost = dp[left][mid];
                    int rightCost = dp[mid][right];
                    int cost = stickLength + leftCost + rightCost;
                    
                    System.out.println("  Try last cut at " + newCuts[mid] + ": " +
                                     stickLength + " + " + leftCost + " + " + rightCost + 
                                     " = " + cost);
                    
                    if (cost < dp[left][right]) {
                        dp[left][right] = cost;
                        bestMid = mid;
                    }
                }
                
                System.out.println("  Best: cut at " + newCuts[bestMid] + 
                                 " with cost = " + dp[left][right]);
            }
        }
        
        System.out.println("\n=== FINAL RESULT ===");
        System.out.println("Minimum cost: " + dp[0][m + 1]);
        
        return dp[0][m + 1];
    }
    
    /**
     * METHOD 4: RECONSTRUCT OPTIMAL CUTTING ORDER
     * ===========================================
     * 
     * Shows not just the minimum cost, but the actual order to make cuts
     */
    public static void printOptimalCutOrder(int n, int[] cuts) {
        int m = cuts.length;
        int[] newCuts = new int[m + 2];
        newCuts[0] = 0;
        newCuts[m + 1] = n;
        System.arraycopy(cuts, 0, newCuts, 1, m);
        Arrays.sort(newCuts);
        
        int[][] dp = new int[m + 2][m + 2];
        int[][] choice = new int[m + 2][m + 2]; // Store which cut to make
        
        for (int len = 2; len <= m + 1; len++) {
            for (int left = 0; left + len <= m + 1; left++) {
                int right = left + len;
                
                if (len == 2) {
                    dp[left][right] = 0;
                    continue;
                }
                
                dp[left][right] = Integer.MAX_VALUE;
                for (int mid = left + 1; mid < right; mid++) {
                    int cost = (newCuts[right] - newCuts[left]) + 
                               dp[left][mid] + dp[mid][right];
                    if (cost < dp[left][right]) {
                        dp[left][right] = cost;
                        choice[left][right] = mid;
                    }
                }
            }
        }
        
        System.out.println("Optimal cutting order:");
        System.out.println("Minimum cost: " + dp[0][m + 1]);
        System.out.println("\nCutting sequence:");
        reconstructCuts(newCuts, choice, 0, m + 1, 1);
    }
    
    private static void reconstructCuts(int[] cuts, int[][] choice, 
                                       int left, int right, int step) {
        if (right - left <= 1) {
            return;
        }
        
        int mid = choice[left][right];
        System.out.println("Step " + step + ": Cut at position " + cuts[mid] + 
                         " (stick from " + cuts[left] + " to " + cuts[right] + 
                         ", cost = " + (cuts[right] - cuts[left]) + ")");
        
        // The cut splits into two independent subproblems
        // Process them in order (though they can be done independently)
        reconstructCuts(cuts, choice, left, mid, step + 1);
        reconstructCuts(cuts, choice, mid, right, step + 1);
    }
    
    /**
     * TEST CASES
     * ==========
     */
    public static void main(String[] args) {
        // Test Case 1: LeetCode Example 1
        System.out.println("=== Test Case 1: n=7, cuts=[1,3,4,5] ===");
        int n1 = 7;
        int[] cuts1 = {1, 3, 4, 5};
        System.out.println("Result: " + minCost(n1, cuts1));
        System.out.println("\nBottom-up: " + minCostBottomUp(n1, cuts1));
        System.out.println("\n--- Detailed Tracking ---");
        minCostWithTracking(n1, cuts1);
        System.out.println("\n--- Optimal Order ---");
        printOptimalCutOrder(n1, cuts1);
        
        // Test Case 2: LeetCode Example 2
        System.out.println("\n\n=== Test Case 2: n=9, cuts=[5,6,1,4,2] ===");
        int n2 = 9;
        int[] cuts2 = {5, 6, 1, 4, 2};
        System.out.println("Result: " + minCost(n2, cuts2));
        
        // Test Case 3: Single cut
        System.out.println("\n=== Test Case 3: n=10, cuts=[5] ===");
        int n3 = 10;
        int[] cuts3 = {5};
        System.out.println("Result: " + minCost(n3, cuts3));
        
        // Test Case 4: Two cuts
        System.out.println("\n=== Test Case 4: n=10, cuts=[2,8] ===");
        int n4 = 10;
        int[] cuts4 = {2, 8};
        System.out.println("Result: " + minCost(n4, cuts4));
        printOptimalCutOrder(n4, cuts4);
        
        // Test Case 5: Many cuts
        System.out.println("\n=== Test Case 5: n=20, cuts=[2,4,6,8,10,12,14,16,18] ===");
        int n5 = 20;
        int[] cuts5 = {2, 4, 6, 8, 10, 12, 14, 16, 18};
        System.out.println("Result: " + minCost(n5, cuts5));
        
        // Test Case 6: Edge case - cuts at boundaries
        System.out.println("\n=== Test Case 6: n=10, cuts=[1,9] ===");
        int n6 = 10;
        int[] cuts6 = {1, 9};
        System.out.println("Result: " + minCost(n6, cuts6));
    }
}

/**
 * INTERVIEW STRATEGY AND TIPS:
 * =============================
 * 
 * HOW TO APPROACH IN 45-MINUTE INTERVIEW:
 * 
 * 1. CLARIFY (3 minutes):
 *    Q: "Can I change the order of cuts?"
 *    A: Yes! (Critical to understand)
 *    
 *    Q: "What's the cost formula again?"
 *    A: Cost = length of stick being cut
 *    
 *    Q: "Can cuts be at position 0 or n?"
 *    A: Usually no, but clarify with interviewer
 * 
 * 2. EXAMPLES (5 minutes):
 *    - Draw the stick on whiteboard
 *    - Show TWO different orders with different costs
 *    - This helps YOU see the pattern and shows interviewer you understand
 * 
 * 3. IDENTIFY PATTERN (5 minutes):
 *    - "This reminds me of Matrix Chain Multiplication"
 *    - "It's an interval DP problem"
 *    - "We're deciding which cut to make LAST"
 *    - Explain why greedy won't work
 * 
 * 4. DEFINE STATE (5 minutes):
 *    - "dp[i][j] = min cost to make all cuts between positions i and j"
 *    - Explain why we add 0 and n to cuts array
 *    - Write out recurrence relation
 * 
 * 5. CODE (20 minutes):
 *    - Start with top-down (easier to explain)
 *    - Mention bottom-up as optimization
 *    - Handle edge cases
 * 
 * 6. TEST (5 minutes):
 *    - Walk through small example
 *    - Check base cases
 * 
 * 7. OPTIMIZE (2 minutes):
 *    - Discuss bottom-up approach
 *    - Time/space complexity
 * 
 * COMMON MISTAKES TO AVOID:
 * =========================
 * 
 * 1. THINKING OF FIRST CUT INSTEAD OF LAST CUT:
 *    ❌ "Which cut should I make first?"
 *    ✓ "Which cut should I make LAST?"
 *    
 *    Why? Because the last cut naturally divides into independent subproblems.
 * 
 * 2. FORGETTING TO SORT CUTS:
 *    ❌ Using cuts array as-is
 *    ✓ Sort the cuts array first
 * 
 * 3. NOT ADDING BOUNDARIES:
 *    ❌ cuts = [1, 3, 4, 5]
 *    ✓ cuts = [0, 1, 3, 4, 5, 7]
 *    
 *    Without boundaries, indexing becomes a nightmare!
 * 
 * 4. WRONG BASE CASE:
 *    ❌ if (left == right) return 0;
 *    ✓ if (right - left <= 1) return 0;
 *    
 *    Adjacent indices mean no cuts in between!
 * 
 * 5. WRONG ITERATION ORDER IN BOTTOM-UP:
 *    ❌ for (int i = 0; i < n; i++)
 *         for (int j = 0; j < n; j++)
 *    
 *    ✓ for (int len = 2; len <= n; len++)
 *         for (int left = 0; left + len <= n; left++)
 *    
 *    Must compute smaller intervals before larger ones!
 * 
 * 6. OFF-BY-ONE ERRORS:
 *    - Be careful with indices vs positions
 *    - cuts[i] is a POSITION, i is an INDEX
 * 
 * WHY THIS APPROACH WORKS:
 * ========================
 * 
 * INTUITION:
 * Think of it like this: If I know the optimal way to cut the left part
 * and the optimal way to cut the right part, and I make this particular
 * cut last, then I have the optimal solution for the whole segment.
 * 
 * This is OPTIMAL SUBSTRUCTURE - a key property of DP problems.
 * 
 * PROOF SKETCH:
 * Suppose dp[i][j] gives the minimum cost for cuts between i and j.
 * If we make cut k last:
 * - Left and right parts are independent
 * - We must solve them optimally (otherwise we could improve total cost)
 * - Therefore, optimal solution uses optimal subsolutions
 * 
 * FOLLOW-UP QUESTIONS:
 * ====================
 * 
 * Q: "Can you optimize space?"
 * A: "Not easily. We need O(m²) states and there's no clear pattern
 *     to reduce it. Unlike 1D DP, we can't use rolling array."
 * 
 * Q: "What if n is very large but cuts is small?"
 * A: "No problem! Our complexity depends on m (cuts.length), not n.
 *     Time is O(m³), space is O(m²)."
 * 
 * Q: "How would you handle duplicate cuts?"
 * A: "Remove duplicates first. Each position should be cut only once."
 * 
 * Q: "Can you print the actual cutting sequence?"
 * A: "Yes, I'd store choices in a 2D array during DP, then reconstruct
 *     the sequence with backtracking." (See Method 4)
 * 
 * RELATED PROBLEMS:
 * =================
 * 1. Matrix Chain Multiplication (Classic DP)
 * 2. Burst Balloons (LeetCode 312)
 * 3. Minimum Cost Tree From Leaf Values (LeetCode 1130)
 * 4. Guess Number Higher or Lower II (LeetCode 375)
 * 5. Stone Game VII (LeetCode 1690)
 * 
 * All use the "interval DP" pattern where we try each possible split point.
 */


class MinCostSolution {

    public int minCost(int n, int[] cuts) {

        // Step 1: Build cuts list with boundaries
        List<Integer> newCuts = new ArrayList<>();
        for (int cut : cuts) {
            newCuts.add(cut);
        }
        newCuts.add(0);
        newCuts.add(n);

        // Step 2: Sort
        Collections.sort(newCuts);

        // 🔴 FIX: Use number of cut points, not stick length
        int len = newCuts.size();

        // memo[left][right] = min cost to cut segment (newCuts[left], newCuts[right])
        Integer[][] memo = new Integer[len][len];

        return minCost(newCuts, memo, 0, len - 1);
    }

    private int minCost(List<Integer> newCuts, Integer[][] memo, int left, int right) {

        // Base case: no cut possible in this interval
        if (right - left <= 1) {
            return 0;
        }

        // Memo hit
        if (memo[left][right] != null) {
            return memo[left][right];
        }

        int minCost = Integer.MAX_VALUE;

        // Try every possible LAST cut in (left, right)
        for (int mid = left + 1; mid < right; mid++) {

            int cost =
                    (newCuts.get(right) - newCuts.get(left))  // cost of current cut
                    + minCost(newCuts, memo, left, mid)       // left segment
                    + minCost(newCuts, memo, mid, right);     // right segment

            minCost = Math.min(minCost, cost);
        }

        memo[left][right] = minCost;
        return minCost;
    }
}
