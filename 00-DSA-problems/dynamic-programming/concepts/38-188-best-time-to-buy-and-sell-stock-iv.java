/**
 * BEST TIME TO BUY AND SELL STOCK IV - At Most k Transactions
 * 
 * PROBLEM UNDERSTANDING:
 * =====================
 * Given an array of stock prices and integer k:
 * - You can complete at most k transactions (k buy-sell pairs)
 * - You must sell before buying again (no overlapping)
 * - Goal: Maximize total profit
 * 
 * This is the HARDEST in the stock trading series.
 * This is a CLASSIC Dynamic Programming problem with state optimization.
 * 
 * DIFFICULTY: Hard
 * Companies: Google, Facebook, Amazon, Microsoft, Bloomberg, Two Sigma
 * 
 * ============================================================================
 * HOW TO APPROACH THIS IN AN INTERVIEW:
 * ============================================================================
 * 
 * Step 1: CLARIFY THE PROBLEM (2-3 minutes)
 * -----------------------------------------
 * Ask questions:
 * - Exactly k or AT MOST k? (At most - can do fewer)
 * - What if k is very large? (Important optimization!)
 * - Can k be 0? (Yes, return 0)
 * - Empty prices? (Return 0)
 * - What if k > n/2? (Key optimization insight!)
 * 
 * Step 2: RECOGNIZE THE PATTERN (Critical!)
 * -----------------------------------------
 * This builds on Stock III (k=2):
 * - Stock I: k = 1 (track min price)
 * - Stock II: k = ∞ (greedy - sum all gains)
 * - Stock III: k = 2 (4 states: buy1, sell1, buy2, sell2)
 * - Stock IV: k = variable (2k states!)
 * 
 * KEY INSIGHT #1: If k >= n/2, it's equivalent to unlimited transactions!
 * Why? You can't make more than n/2 non-overlapping transactions from n days.
 * 
 * KEY INSIGHT #2: We need k buy states and k sell states.
 * buy[i] = max profit after buying i-th stock
 * sell[i] = max profit after selling i-th stock
 * 
 * Step 3: SOLUTION PROGRESSION
 * ---------------------------
 * 1. Naive DP: 3D array [day][k][holding] - O(nk) time, O(nk) space
 * 2. Optimized DP: 2D array [k][2] - O(nk) time, O(k) space
 * 3. State arrays: buy[] and sell[] - O(nk) time, O(k) space ⭐ BEST
 * 4. Handle k >= n/2: Reduce to unlimited - Critical optimization!
 * 
 * ============================================================================
 * THE CRITICAL OPTIMIZATION:
 * ============================================================================
 * 
 * If k >= n/2 (number of days / 2):
 * - We can make as many transactions as we want
 * - Reduce to Stock II problem: Sum all positive differences
 * - This avoids TLE (Time Limit Exceeded) for large k!
 * 
 * Example: If n=10 days, max possible transactions = 5
 * If k >= 5, treat as unlimited transactions.
 * 
 * ============================================================================
 */

class StockTradingK {
    
    /**
     * ========================================================================
     * APPROACH 1: NAIVE 3D DP
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n * k)
     * SPACE COMPLEXITY: O(n * k * 2) = O(nk)
     * 
     * STATE DEFINITION:
     * dp[i][j][0] = max profit on day i with at most j transactions, not holding
     * dp[i][j][1] = max profit on day i with at most j transactions, holding stock
     * 
     * RECURRENCE:
     * dp[i][j][0] = max(dp[i-1][j][0], dp[i-1][j][1] + prices[i])
     *   Either: didn't hold yesterday, OR sell today
     * 
     * dp[i][j][1] = max(dp[i-1][j][1], dp[i-1][j-1][0] - prices[i])
     *   Either: held yesterday, OR buy today (uses one transaction)
     * 
     * This works but uses O(nk) space unnecessarily.
     */
    public int maxProfitNaiveDP(int k, int[] prices) {
        if (prices == null || prices.length < 2 || k == 0) {
            return 0;
        }
        
        int n = prices.length;
        
        // Optimization: if k >= n/2, unlimited transactions
        if (k >= n / 2) {
            return maxProfitUnlimited(prices);
        }
        
        // dp[i][j][0] = max profit on day i, j transactions, not holding
        // dp[i][j][1] = max profit on day i, j transactions, holding
        int[][][] dp = new int[n][k + 1][2];
        
        // Initialize: can't hold stock with 0 transactions
        for (int i = 0; i < n; i++) {
            dp[i][0][1] = Integer.MIN_VALUE / 2; // Invalid state
        }
        
        // Initialize: day 0
        for (int j = 1; j <= k; j++) {
            dp[0][j][0] = 0;
            dp[0][j][1] = -prices[0];
        }
        
        // Fill DP table
        for (int i = 1; i < n; i++) {
            for (int j = 1; j <= k; j++) {
                // Not holding: either didn't hold, or sell today
                dp[i][j][0] = Math.max(dp[i - 1][j][0], 
                                       dp[i - 1][j][1] + prices[i]);
                
                // Holding: either held, or buy today
                dp[i][j][1] = Math.max(dp[i - 1][j][1], 
                                       dp[i - 1][j - 1][0] - prices[i]);
            }
        }
        
        return dp[n - 1][k][0];
    }
    
    /**
     * ========================================================================
     * APPROACH 2: SPACE-OPTIMIZED 2D DP
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n * k)
     * SPACE COMPLEXITY: O(k)
     * 
     * OPTIMIZATION: We only need previous day's states, not all days.
     * Use rolling arrays or just two arrays: prev and curr.
     */
    public int maxProfit2DOptimized(int k, int[] prices) {
        if (prices == null || prices.length < 2 || k == 0) {
            return 0;
        }
        
        int n = prices.length;
        
        if (k >= n / 2) {
            return maxProfitUnlimited(prices);
        }
        
        // prev[j][0/1] and curr[j][0/1]
        int[][] prev = new int[k + 1][2];
        int[][] curr = new int[k + 1][2];
        
        // Initialize
        for (int j = 0; j <= k; j++) {
            prev[j][0] = 0;
            prev[j][1] = -prices[0];
        }
        prev[0][1] = Integer.MIN_VALUE / 2;
        
        // Process each day
        for (int i = 1; i < n; i++) {
            curr[0][1] = Integer.MIN_VALUE / 2;
            for (int j = 1; j <= k; j++) {
                curr[j][0] = Math.max(prev[j][0], prev[j][1] + prices[i]);
                curr[j][1] = Math.max(prev[j][1], prev[j - 1][0] - prices[i]);
            }
            
            // Swap
            int[][] temp = prev;
            prev = curr;
            curr = temp;
        }
        
        return prev[k][0];
    }
    
    /**
     * ========================================================================
     * APPROACH 3: STATE ARRAYS (OPTIMAL - Most Elegant)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n * k)
     * SPACE COMPLEXITY: O(k)
     * 
     * THE CORE INSIGHT:
     * ================
     * We maintain two arrays:
     * - buy[i] = max profit after buying i-th stock (i = 1 to k)
     * - sell[i] = max profit after selling i-th stock (i = 1 to k)
     * 
     * STATE TRANSITIONS:
     * ==================
     * For each price on each day:
     * 
     * buy[i] = max(buy[i], sell[i-1] - price)
     *   Either: keep previous buy state, OR buy after (i-1)th sell
     * 
     * sell[i] = max(sell[i], buy[i] + price)
     *   Either: keep previous sell state, OR sell i-th stock
     * 
     * INITIALIZATION:
     * ==============
     * buy[i] = -∞ (haven't bought yet)
     * sell[i] = 0 (no transaction completed)
     * 
     * Special case: sell[0] = 0 (no transaction at all)
     * 
     * WHY THIS WORKS:
     * ==============
     * Each buy[i] tracks "max profit after buying i-th stock"
     * Each sell[i] tracks "max profit after completing i transactions"
     * 
     * By updating in order (1 to k), we ensure:
     * - buy[i] can only happen after sell[i-1]
     * - sell[i] can only happen after buy[i]
     * 
     * After processing all days, sell[k] has the answer!
     * 
     * INTERVIEW TIP: This is the EXPECTED solution. Practice explaining
     * the state transitions clearly!
     */
    public int maxProfit(int k, int[] prices) {
        if (prices == null || prices.length < 2 || k == 0) {
            return 0;
        }
        
        int n = prices.length;
        
        // CRITICAL OPTIMIZATION: If k >= n/2, it's unlimited transactions
        // This prevents TLE for large k!
        if (k >= n / 2) {
            return maxProfitUnlimited(prices);
        }
        
        // buy[i] = max profit after buying i-th stock (1-indexed)
        // sell[i] = max profit after selling i-th stock (1-indexed)
        int[] buy = new int[k + 1];
        int[] sell = new int[k + 1];
        
        // Initialize: all buy states start at negative infinity
        // (can't have bought without seeing prices)
        for (int i = 1; i <= k; i++) {
            buy[i] = Integer.MIN_VALUE;
        }
        
        // Process each price
        for (int price : prices) {
            // Update all k transactions
            // IMPORTANT: Must update in order from k down to 1
            // or use temporary variables
            for (int i = k; i >= 1; i--) {
                // Sell i-th stock: either keep previous, or sell today
                sell[i] = Math.max(sell[i], buy[i] + price);
                
                // Buy i-th stock: either keep previous, or buy today
                // Can only buy i-th after selling (i-1)th
                buy[i] = Math.max(buy[i], sell[i - 1] - price);
            }
        }
        
        // Answer is max profit after at most k transactions
        return sell[k];
    }
    
    /**
     * ========================================================================
     * HELPER: UNLIMITED TRANSACTIONS (Stock II)
     * ========================================================================
     * 
     * When k >= n/2, we can make as many transactions as we want.
     * Use greedy approach: sum all positive price differences.
     * 
     * TIME COMPLEXITY: O(n)
     * SPACE COMPLEXITY: O(1)
     */
    private int maxProfitUnlimited(int[] prices) {
        int profit = 0;
        for (int i = 1; i < prices.length; i++) {
            if (prices[i] > prices[i - 1]) {
                profit += prices[i] - prices[i - 1];
            }
        }
        return profit;
    }
    
    /**
     * ========================================================================
     * APPROACH 4: FORWARD-BACKWARD ALTERNATIVE
     * ========================================================================
     * 
     * Alternative approach: compute separately then combine.
     * Less elegant but worth knowing.
     * 
     * TIME COMPLEXITY: O(n * k)
     * SPACE COMPLEXITY: O(k)
     */
    public int maxProfitForwardBackward(int k, int[] prices) {
        if (prices == null || prices.length < 2 || k == 0) {
            return 0;
        }
        
        if (k >= prices.length / 2) {
            return maxProfitUnlimited(prices);
        }
        
        int n = prices.length;
        
        // forward[i][j] = max profit using first i prices, j transactions
        int[][] forward = new int[n][k + 1];
        
        for (int j = 1; j <= k; j++) {
            int maxDiff = -prices[0];
            for (int i = 1; i < n; i++) {
                forward[i][j] = Math.max(forward[i - 1][j], prices[i] + maxDiff);
                maxDiff = Math.max(maxDiff, forward[i][j - 1] - prices[i]);
            }
        }
        
        return forward[n - 1][k];
    }
    
    /**
     * ========================================================================
     * APPROACH 5: WITH TRANSACTION TRACKING
     * ========================================================================
     * 
     * Track actual buy/sell days (more complex, usually not asked).
     */
    public static class Transaction {
        int buyDay;
        int sellDay;
        int profit;
        
        Transaction(int buyDay, int sellDay, int profit) {
            this.buyDay = buyDay;
            this.sellDay = sellDay;
            this.profit = profit;
        }
        
        @Override
        public String toString() {
            return String.format("Buy day %d (price=%d), Sell day %d (price=%d), Profit: %d",
                               buyDay, -1, sellDay, -1, profit);
        }
    }
    
    /**
     * ========================================================================
     * VISUALIZATION HELPER
     * ========================================================================
     */
    public void visualizeStates(int k, int[] prices) {
        if (prices == null || prices.length == 0) return;
        
        int n = prices.length;
        if (k >= n / 2) {
            System.out.println("k >= n/2, using unlimited transactions approach");
            System.out.println("Result: " + maxProfitUnlimited(prices));
            return;
        }
        
        System.out.println("\nState Evolution for k=" + k + ":");
        System.out.print("Day  Price  ");
        for (int i = 1; i <= k; i++) {
            System.out.printf("buy%-2d sell%-2d ", i, i);
        }
        System.out.println();
        System.out.println("=".repeat(10 + k * 12));
        
        int[] buy = new int[k + 1];
        int[] sell = new int[k + 1];
        
        for (int i = 1; i <= k; i++) {
            buy[i] = Integer.MIN_VALUE;
        }
        
        for (int day = 0; day < prices.length; day++) {
            int price = prices[day];
            
            // Update states (using temporary to show before/after)
            int[] newBuy = buy.clone();
            int[] newSell = sell.clone();
            
            for (int i = k; i >= 1; i--) {
                newSell[i] = Math.max(sell[i], buy[i] + price);
                newBuy[i] = Math.max(buy[i], sell[i - 1] - price);
            }
            
            buy = newBuy;
            sell = newSell;
            
            System.out.printf("%2d   %4d   ", day, price);
            for (int i = 1; i <= k; i++) {
                System.out.printf("%5d  %5d ", 
                    buy[i] == Integer.MIN_VALUE ? -9999 : buy[i], sell[i]);
            }
            System.out.println();
        }
        
        System.out.println("\nFinal Answer: " + sell[k]);
    }
    
    /**
     * ========================================================================
     * TESTING AND VALIDATION
     * ========================================================================
     */
    public static void main(String[] args) {
        StockTradingK solution = new StockTradingK();
        
        System.out.println("=== STOCK TRADING - AT MOST k TRANSACTIONS ===\n");
        
        // Test Case 1: Example 1 from problem
        System.out.println("Test 1: k=2, prices=[2,4,1]");
        int[] prices1 = {2, 4, 1};
        System.out.println("Expected: 2");
        System.out.println("State Arrays: " + solution.maxProfit(2, prices1));
        System.out.println("Naive DP: " + solution.maxProfitNaiveDP(2, prices1));
        System.out.println("2D Optimized: " + solution.maxProfit2DOptimized(2, prices1));
        solution.visualizeStates(2, prices1);
        
        // Test Case 2: Example 2 from problem
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 2: k=2, prices=[3,2,6,5,0,3]");
        int[] prices2 = {3, 2, 6, 5, 0, 3};
        System.out.println("Expected: 7");
        System.out.println("State Arrays: " + solution.maxProfit(2, prices2));
        solution.visualizeStates(2, prices2);
        
        // Test Case 3: k=1 (should match Stock I)
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 3: k=1, prices=[7,1,5,3,6,4]");
        int[] prices3 = {7, 1, 5, 3, 6, 4};
        System.out.println("Expected: 5 (buy at 1, sell at 6)");
        System.out.println("State Arrays: " + solution.maxProfit(1, prices3));
        
        // Test Case 4: k=3
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 4: k=3, prices=[1,2,4,2,5,7,2,4,9,0]");
        int[] prices4 = {1, 2, 4, 2, 5, 7, 2, 4, 9, 0};
        System.out.println("State Arrays: " + solution.maxProfit(3, prices4));
        solution.visualizeStates(3, prices4);
        
        // Test Case 5: k >= n/2 (should use unlimited)
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 5: k=1000, prices=[1,2,3,4,5]");
        int[] prices5 = {1, 2, 3, 4, 5};
        System.out.println("Expected: 4 (k is large, so unlimited)");
        System.out.println("State Arrays: " + solution.maxProfit(1000, prices5));
        
        // Test Case 6: All decreasing
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 6: k=2, prices=[5,4,3,2,1]");
        int[] prices6 = {5, 4, 3, 2, 1};
        System.out.println("Expected: 0");
        System.out.println("State Arrays: " + solution.maxProfit(2, prices6));
        
        // Test Case 7: Edge case - k=0
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 7: k=0, prices=[1,2,3,4,5]");
        System.out.println("Expected: 0");
        System.out.println("State Arrays: " + solution.maxProfit(0, prices5));
        
        // Test Case 8: Edge case - empty prices
        System.out.println("\nTest 8: k=2, prices=[]");
        System.out.println("Expected: 0");
        System.out.println("State Arrays: " + solution.maxProfit(2, new int[]{}));
        
        // Test Case 9: Complex pattern
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 9: k=2, prices=[3,3,5,0,0,3,1,4]");
        int[] prices9 = {3, 3, 5, 0, 0, 3, 1, 4};
        System.out.println("Expected: 6");
        System.out.println("State Arrays: " + solution.maxProfit(2, prices9));
        solution.visualizeStates(2, prices9);
        
        // Performance comparison
        System.out.println("\n" + "=".repeat(60));
        System.out.println("PERFORMANCE COMPARISON");
        System.out.println("=".repeat(60));
        
        int[] largePrices = new int[1000];
        for (int i = 0; i < largePrices.length; i++) {
            largePrices[i] = (int)(Math.random() * 100);
        }
        
        int testK = 10;
        
        long start = System.nanoTime();
        int result1 = solution.maxProfit(testK, largePrices);
        long time1 = System.nanoTime() - start;
        
        start = System.nanoTime();
        int result2 = solution.maxProfitNaiveDP(testK, largePrices);
        long time2 = System.nanoTime() - start;
        
        start = System.nanoTime();
        int result3 = solution.maxProfit2DOptimized(testK, largePrices);
        long time3 = System.nanoTime() - start;
        
        System.out.println("Array size: 1,000 elements, k=" + testK);
        System.out.println("State Arrays time: " + time1 + " ns");
        System.out.println("Naive 3D DP time: " + time2 + " ns");
        System.out.println("2D Optimized time: " + time3 + " ns");
        System.out.println("All produce same result: " + 
                         (result1 == result2 && result2 == result3));
        
        // Test large k optimization
        System.out.println("\n" + "=".repeat(60));
        System.out.println("LARGE k OPTIMIZATION TEST");
        System.out.println("=".repeat(60));
        
        int[] testPrices = {1, 3, 2, 5, 4, 7};
        System.out.println("Prices: [1,3,2,5,4,7]");
        System.out.println("k=1: " + solution.maxProfit(1, testPrices));
        System.out.println("k=2: " + solution.maxProfit(2, testPrices));
        System.out.println("k=3: " + solution.maxProfit(3, testPrices) + 
                         " (should match unlimited)");
        System.out.println("k=100: " + solution.maxProfit(100, testPrices) + 
                         " (k >= n/2, uses unlimited)");
        System.out.println("Unlimited: " + solution.maxProfitUnlimited(testPrices));
    }
}

/**
 * ============================================================================
 * INTERVIEW TIPS SUMMARY:
 * ============================================================================
 * 
 * 1. THE CRITICAL INSIGHT (Must mention in interview!):
 *    ==================================================
 *    "If k >= n/2, we can make unlimited transactions because we can't
 *    possibly make more than n/2 non-overlapping buy-sell pairs from n days.
 *    This optimization is CRITICAL to avoid TLE!"
 *    
 *    "For k < n/2, I'll use state arrays: buy[i] and sell[i] representing
 *    max profit after buying/selling the i-th stock."
 * 
 * 2. SOLUTION PROGRESSION IN INTERVIEW:
 *    ==================================
 *    - "Let me check if k >= n/2 first..." (1 min)
 *    - "For general k, I'll track k buy states and k sell states..." (2 min)
 *    - "At each price, I update all states in reverse order..." (2 min)
 *    - "Let me code this approach..." (10-12 min)
 *    - Test with examples (5-7 min)
 *    - Discuss why reverse order matters (3 min)
 *    - Discuss time/space complexity (2 min)
 * 
 * 3. COMMON MISTAKES TO AVOID:
 *    ========================
 *    ✗ Not handling k >= n/2 optimization (causes TLE!)
 *    ✗ Updating states in wrong order (forward vs backward)
 *    ✗ Not initializing buy states to MIN_VALUE
 *    ✗ Off-by-one errors in array indexing
 *    ✗ Forgetting edge cases (k=0, empty array)
 *    ✗ Using Integer.MIN_VALUE without /2 (overflow in arithmetic)
 * 
 * 4. WHY REVERSE ORDER IN UPDATE?
 *    ============================
 *    When updating buy[i] and sell[i], we use sell[i-1].
 *    If we update forward (1 to k), we might use the NEW sell[i-1]
 *    instead of the OLD sell[i-1] from previous iteration.
 *    
 *    Solution: Either update backward (k to 1) or use temp arrays.
 *    Backward is simpler and uses less space.
 * 
 * 5. COMPLEXITY ANALYSIS:
 *    ====================
 *    State Arrays (Optimal):
 *    - Time: O(n*k) if k < n/2, O(n) if k >= n/2
 *    - Space: O(k) for arrays
 *    
 *    Naive 3D DP:
 *    - Time: O(n*k)
 *    - Space: O(n*k) - wasteful
 *    
 *    Key insight: Can't do better than O(n*k) in general case
 *    But k >= n/2 optimization is crucial!
 * 
 * 6. FOLLOW-UP QUESTIONS:
 *    ====================
 *    Q: Why does k >= n/2 mean unlimited transactions?
 *    A: Each transaction needs at least 2 days (buy, sell).
 *       With n days, max possible = n/2 transactions.
 *       If k >= this, constraint doesn't matter.
 *    
 *    Q: What if there's a transaction fee?
 *    A: Subtract fee in sell state: sell[i] = max(sell[i], buy[i] + price - fee)
 *    
 *    Q: What if there's a cooldown?
 *    A: Need additional states to track cooldown period
 *    
 *    Q: Can you do it with O(1) space?
 *    A: No, we fundamentally need O(k) space for k transaction states
 *       Unless k >= n/2, then yes (unlimited transactions uses O(1))
 *    
 *    Q: What if we want actual transaction days?
 *    A: Need to track parent pointers in DP table, then backtrack
 * 
 * 7. TIME MANAGEMENT (45 min interview):
 *    ==================================
 *    - Problem understanding: 2-3 min
 *    - Identify k >= n/2 optimization: 2 min
 *    - Explain state array approach: 3-5 min
 *    - Code state array solution: 10-12 min
 *    - Test thoroughly: 5-7 min
 *    - Explain update order: 3 min
 *    - Discuss complexity: 2-3 min
 *    - Handle follow-ups: Remaining time
 * 
 * 8. WHAT MAKES A STRONG PERFORMANCE:
 *    ================================
 *    ✓ Immediately recognize k >= n/2 optimization
 *    ✓ Clearly explain state array approach
 *    ✓ Handle update order correctly
 *    ✓ Write clean, bug-free code
 *    ✓ Test edge cases (k=0, k=1, k>>n, empty)
 *    ✓ Explain time/space complexity
 *    ✓ Discuss relationship to other stock problems
 *    ✓ Handle follow-up questions confidently
 * 
 * 9. TESTING STRATEGY:
 *    ================
 *    Must test:
 *    ✓ Given examples from problem
 *    ✓ k=1 (reduces to Stock I)
 *    ✓ k >= n/2 (should match unlimited)
 *    ✓ k=0 (profit = 0)
 *    ✓ Empty array
 *    ✓ All decreasing prices
 *    ✓ All increasing prices
 *    ✓ Single element
 * 
 * 10. RELATIONSHIP TO OTHER PROBLEMS:
 *     ==============================
 *     Stock I (k=1):
 *     - Track min price seen so far
 *     - O(n) time, O(1) space
 *     
 *     Stock II (k=∞):
 *     - Greedy: sum all positive differences
 *     - O(n) time, O(1) space
 *     
 *     Stock III (k=2):
 *     - 4 states: buy1, sell1, buy2, sell2
 *     - O(n) time, O(1) space
 *     
 *     Stock IV (k=variable):
 *     - k pairs of states: buy[1..k], sell[1..k]
 *     - O(n*k) time, O(k) space (or O(n) if k >= n/2)
 *     
 *     All use state machine DP pattern!
 * 
 * ============================================================================
 * ADDITIONAL INSIGHTS:
 * ============================================================================
 * 
 * 1. WHY THIS PROBLEM IS HARD:
 *    ========================
 *    - Combines multiple concepts: DP, state machines, optimization
 *    - Easy to miss k >= n/2 optimization (causes TLE)
 *    - Update order matters (forward vs backward)
 *    - Edge cases are tricky (k=0, k>>n, empty)
 *    - Generalizes simpler problems (Stock I, II, III)
 * 
 * 2. THE k >= n/2 OPTIMIZATION IN DETAIL:
 *    ===================================
 *    Consider n=6 days with prices [1,2,3,4,5,6]:
 *    
 *    Max possible transactions:
 *    - Buy day 0, sell day 1
 *    - Buy day 2, sell day 3
 *    - Buy day 4, sell day 5
 *    = 3 transactions = n/2
 *    
 *    If k >= 3, we can capture all profit anyway.
 *    So k=3, k=4, k=100 all give same result.
 *    
 *    This optimization turns O(n*k) into O(n) for large k!
 * 
 * 3. STATE MACHINE INTERPRETATION:
 *    ============================
 *    Think of buy[i] and sell[i] as "portfolio values":
 *    
 *    sell[0] = 0 (no investment, cash = 0)
 *    buy[1] = -price (bought first stock, cash negative)
 *    sell[1] = profit (sold first stock, cash positive)
 *    buy[2] = sell[1] - price (bought second, reinvested)
 *    sell[2] = sell[1] + profit2 (sold second, total profit)
 *    ...
 *    
 *    Each state represents maximum "wealth" at that stage.
 * 
 * 4. WHY WE CAN'T IMPROVE TIME COMPLEXITY:
 *    ====================================
 *    - Must see all n prices: Ω(n)
 *    - Must consider all k transactions: Ω(k)
 *    - Therefore: Ω(n*k) lower bound
 *    
 *    Our O(n*k) solution is optimal!
 *    (Except for k >= n/2 where we optimize to O(n))
 * 
 * 5. PRACTICAL APPLICATIONS:
 *    ======================
 *    - Algorithmic trading with transaction limits
 *    - Resource allocation with capacity constraints
 *    - Job scheduling with limited switches
 *    - Investment portfolio optimization
 *    - Pattern recognition in time series
 * 
 * 6. CONNECTION TO THEORY:
 *    ====================
 *    - State machine design pattern
 *    - Dynamic programming with constraints
 *    - Space-time tradeoff optimization
 *    - Greedy vs DP decision making
 *    - Amortized analysis (when k is large)
 * 
 * ============================================================================
 */
