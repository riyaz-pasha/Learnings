/**
 * BEST TIME TO BUY AND SELL STOCK WITH TRANSACTION FEE
 * 
 * PROBLEM UNDERSTANDING:
 * =====================
 * Given an array of stock prices and a transaction fee:
 * - You can complete unlimited transactions (buy and sell multiple times)
 * - Must pay transaction fee for EACH transaction (buy-sell pair)
 * - Cannot hold multiple stocks simultaneously
 * - Goal: Maximize total profit after fees
 * 
 * This is a MEDIUM Dynamic Programming problem.
 * Simpler than cooldown, more complex than unlimited transactions (Stock II).
 * 
 * DIFFICULTY: Medium
 * Companies: Google, Facebook, Amazon, Bloomberg, Robinhood, Two Sigma
 * 
 * ============================================================================
 * HOW TO APPROACH THIS IN AN INTERVIEW:
 * ============================================================================
 * 
 * Step 1: CLARIFY THE PROBLEM (2 minutes)
 * -----------------------------------------
 * Ask questions:
 * - Is fee charged per transaction or per buy AND sell? (Per transaction = once)
 * - Can fee be 0? (Yes, reduces to Stock II)
 * - Can fee be larger than price? (Possible, edge case)
 * - When is fee charged - on buy, sell, or either? (Either works, be consistent)
 * - Empty array? (Return 0)
 * 
 * Step 2: WORK THROUGH EXAMPLES (3-5 minutes)
 * -------------------------------------------
 * Example 1: prices = [1,3,2,8,4,9], fee = 2
 * - Buy at 1, sell at 8: profit = 8-1-2 = 5
 * - Buy at 4, sell at 9: profit = 9-4-2 = 3
 * - Total = 8
 * 
 * Why not sell at 3? Because 3-1-2 = 0, then buy at 2 gives worse result.
 * 
 * Key insight: Fee affects whether a transaction is worth it!
 * 
 * Step 3: COMPARE TO SIMILAR PROBLEMS
 * -----------------------------------
 * Stock II (no fee): Sum all positive differences
 *   [1,3,2,8] → (3-1) + (8-2) = 7
 * 
 * With fee = 2:
 *   [1,3,2,8] → Should we sell at 3?
 *   Option A: Buy 1, sell 3 (profit -2), buy 2, sell 8 (profit 4) = 2
 *   Option B: Buy 1, sell 8 (profit 5) = 5 ✓ Better!
 * 
 * Fee discourages frequent trading → Need DP, not simple greedy!
 * 
 * Step 4: IDENTIFY THE STATE MACHINE
 * ----------------------------------
 * At any day, we can be in TWO states:
 * 
 * State 0: NOT HOLDING (cash state)
 * State 1: HOLDING stock (invested state)
 * 
 * STATE TRANSITIONS:
 * From NOT HOLDING: Can buy (→ HOLDING) or rest (→ NOT HOLDING)
 * From HOLDING: Can sell (→ NOT HOLDING) or hold (→ HOLDING)
 * 
 * KEY: Fee is charged when we COMPLETE a transaction.
 * We can charge it on buy OR sell (mathematically equivalent).
 * 
 * Step 5: SOLUTION PROGRESSION
 * ---------------------------
 * 1. Greedy (wrong): Doesn't work due to fee
 * 2. Recursion: Try all possibilities - O(2^n) exponential
 * 3. Memoization: Cache states - O(n) time, O(n) space
 * 4. Bottom-up DP: Iterative - O(n) time, O(n) space
 * 5. Space-optimized: Variables - O(n) time, O(1) space ⭐ OPTIMAL
 * 
 * ============================================================================
 * THE KEY INSIGHT:
 * ============================================================================
 * 
 * This is similar to Stock II (unlimited transactions) but with a twist:
 * - Stock II: Always buy at valley, sell at peak
 * - With Fee: Only transact if profit > fee
 * 
 * We use 2-state DP (like Stock II), but subtract fee when completing transaction.
 * 
 * hold[i] = max profit on day i if HOLDING stock
 * cash[i] = max profit on day i if NOT HOLDING stock
 * 
 * RECURRENCE RELATIONS:
 * 
 * cash[i] = max(cash[i-1], hold[i-1] + prices[i] - fee)
 *   Either: didn't hold yesterday, OR sell today (charge fee here)
 * 
 * hold[i] = max(hold[i-1], cash[i-1] - prices[i])
 *   Either: held yesterday, OR buy today
 * 
 * Alternative: Charge fee on buy instead:
 * cash[i] = max(cash[i-1], hold[i-1] + prices[i])
 * hold[i] = max(hold[i-1], cash[i-1] - prices[i] - fee)
 * 
 * Both are mathematically equivalent!
 * 
 * ============================================================================
 */

class StockWithFee {
    
    /**
     * ========================================================================
     * APPROACH 1: WRONG GREEDY (Educational)
     * ========================================================================
     * 
     * This shows why greedy doesn't work.
     * 
     * GREEDY IDEA: Sum all positive differences minus fees
     * 
     * WHY IT FAILS:
     * For [1,3,2,8], fee=2:
     * - Greedy: (3-1-2) + (8-2-2) = 0 + 4 = 4
     * - Optimal: (8-1-2) = 5
     * 
     * The fee makes it better to hold longer and avoid small trades!
     */
    public int maxProfitWrongGreedy(int[] prices, int fee) {
        int profit = 0;
        for (int i = 1; i < prices.length; i++) {
            int gain = prices[i] - prices[i - 1];
            if (gain > fee) {
                profit += gain - fee;
            }
        }
        return profit; // WRONG!
    }
    
    /**
     * ========================================================================
     * APPROACH 2: RECURSION (Brute Force)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(2^n) - Exponential
     * SPACE COMPLEXITY: O(n) - Recursion depth
     * 
     * At each day: buy, sell, or hold based on current state.
     * 
     * INTERVIEW TIP: Mention to show understanding, then say
     * "This is exponential, we need DP."
     */
    public int maxProfitRecursive(int[] prices, int fee) {
        if (prices == null || prices.length < 2) return 0;
        return recursiveHelper(prices, 0, false, fee);
    }
    
    private int recursiveHelper(int[] prices, int day, boolean holding, int fee) {
        if (day >= prices.length) return 0;
        
        if (holding) {
            // Can sell or hold
            int sell = prices[day] - fee + recursiveHelper(prices, day + 1, false, fee);
            int hold = recursiveHelper(prices, day + 1, true, fee);
            return Math.max(sell, hold);
        } else {
            // Can buy or skip
            int buy = -prices[day] + recursiveHelper(prices, day + 1, true, fee);
            int skip = recursiveHelper(prices, day + 1, false, fee);
            return Math.max(buy, skip);
        }
    }
    
    /**
     * ========================================================================
     * APPROACH 3: MEMOIZATION (Top-Down DP)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n) - Each state computed once
     * SPACE COMPLEXITY: O(n) - Memoization table + recursion stack
     */
    public int maxProfitMemoization(int[] prices, int fee) {
        if (prices == null || prices.length < 2) return 0;
        
        Integer[][] memo = new Integer[prices.length][2];
        return memoHelper(prices, 0, 0, fee, memo);
    }
    
    private int memoHelper(int[] prices, int day, int holding, int fee, Integer[][] memo) {
        if (day >= prices.length) return 0;
        
        if (memo[day][holding] != null) {
            return memo[day][holding];
        }
        
        int result;
        if (holding == 1) {
            int sell = prices[day] - fee + memoHelper(prices, day + 1, 0, fee, memo);
            int hold = memoHelper(prices, day + 1, 1, fee, memo);
            result = Math.max(sell, hold);
        } else {
            int buy = -prices[day] + memoHelper(prices, day + 1, 1, fee, memo);
            int skip = memoHelper(prices, day + 1, 0, fee, memo);
            result = Math.max(buy, skip);
        }
        
        memo[day][holding] = result;
        return result;
    }
    
    /**
     * ========================================================================
     * APPROACH 4: BOTTOM-UP DP WITH ARRAYS (Clear and Recommended)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n) - Single pass
     * SPACE COMPLEXITY: O(n) - Two arrays
     * 
     * THE CORE DP INSIGHT:
     * ====================
     * We track max profit in two states for each day:
     * 
     * cash[i] = max profit on day i if NOT holding stock
     * hold[i] = max profit on day i if HOLDING stock
     * 
     * STATE TRANSITIONS:
     * ==================
     * cash[i] = max(cash[i-1], hold[i-1] + prices[i] - fee)
     *   Either: didn't hold yesterday, OR sell today and pay fee
     * 
     * hold[i] = max(hold[i-1], cash[i-1] - prices[i])
     *   Either: held yesterday, OR buy today
     * 
     * INITIAL VALUES:
     * ==============
     * Day 0:
     * cash[0] = 0 (no transaction)
     * hold[0] = -prices[0] (buy on day 0)
     * 
     * ANSWER: cash[n-1]
     *   On last day, we should be in cash state for max profit
     * 
     * INTERVIEW TIP: This is a clear solution to code.
     * Easy to explain and understand!
     */
    public int maxProfitDPArrays(int[] prices, int fee) {
        if (prices == null || prices.length < 2) {
            return 0;
        }
        
        int n = prices.length;
        
        // State arrays
        int[] cash = new int[n]; // Not holding
        int[] hold = new int[n]; // Holding
        
        // Day 0 initialization
        cash[0] = 0;              // Do nothing
        hold[0] = -prices[0];     // Buy on day 0
        
        // Fill DP arrays
        for (int i = 1; i < n; i++) {
            // To be in cash on day i: either was cash, or sell today
            cash[i] = Math.max(cash[i - 1], hold[i - 1] + prices[i] - fee);
            
            // To hold on day i: either held, or buy today
            hold[i] = Math.max(hold[i - 1], cash[i - 1] - prices[i]);
        }
        
        // Answer is cash state on last day
        return cash[n - 1];
    }
    
    /**
     * ========================================================================
     * APPROACH 5: SPACE-OPTIMIZED DP (OPTIMAL - Most Important!)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n) - Single pass
     * SPACE COMPLEXITY: O(1) - Only 2 variables
     * 
     * OPTIMIZATION:
     * We only need previous day's states, not entire arrays.
     * Use two variables: cash and hold.
     * 
     * This is the EXPECTED optimal solution in interviews.
     * 
     * INTERVIEW TIP: Start with array version to show understanding,
     * then optimize to this. Shows progression of thought!
     */
    public int maxProfit(int[] prices, int fee) {
        if (prices == null || prices.length < 2) {
            return 0;
        }
        
        // State variables
        int cash = 0;              // Max profit if not holding
        int hold = -prices[0];     // Max profit if holding
        
        for (int i = 1; i < prices.length; i++) {
            // Update cash: either stay in cash or sell stock
            int newCash = Math.max(cash, hold + prices[i] - fee);
            
            // Update hold: either keep holding or buy stock
            int newHold = Math.max(hold, cash - prices[i]);
            
            cash = newCash;
            hold = newHold;
        }
        
        return cash;
    }
    
    /**
     * ========================================================================
     * APPROACH 6: ALTERNATIVE - FEE ON BUY
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n)
     * SPACE COMPLEXITY: O(1)
     * 
     * Alternative: Charge fee when buying instead of selling.
     * Mathematically equivalent but may be clearer in some contexts.
     */
    public int maxProfitFeeOnBuy(int[] prices, int fee) {
        if (prices == null || prices.length < 2) {
            return 0;
        }
        
        int cash = 0;
        int hold = -prices[0] - fee; // Charge fee when buying
        
        for (int i = 1; i < prices.length; i++) {
            int newCash = Math.max(cash, hold + prices[i]); // No fee on sell
            int newHold = Math.max(hold, cash - prices[i] - fee); // Fee on buy
            
            cash = newCash;
            hold = newHold;
        }
        
        return cash;
    }
    
    /**
     * ========================================================================
     * APPROACH 7: GREEDY-LIKE OPTIMIZATION (Advanced)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n)
     * SPACE COMPLEXITY: O(1)
     * 
     * CLEVER APPROACH:
     * Track minimum buy price and maximum profit.
     * When we find a better sell point, update.
     * When price drops significantly, reset.
     * 
     * This is harder to understand but elegant!
     */
    public int maxProfitGreedyOptimized(int[] prices, int fee) {
        if (prices == null || prices.length < 2) {
            return 0;
        }
        
        int profit = 0;
        int minPrice = prices[0]; // Minimum price to buy
        
        for (int i = 1; i < prices.length; i++) {
            // If we can sell for profit (considering fee)
            if (prices[i] < minPrice) {
                // Found cheaper buy point
                minPrice = prices[i];
            } else if (prices[i] > minPrice + fee) {
                // Profitable to sell
                profit += prices[i] - minPrice - fee;
                // New "buy" price is current price minus fee
                // (allows us to change decision if price goes higher)
                minPrice = prices[i] - fee;
            }
        }
        
        return profit;
    }
    
    /**
     * ========================================================================
     * VISUALIZATION HELPER
     * ========================================================================
     */
    public void visualizeStates(int[] prices, int fee) {
        if (prices == null || prices.length == 0) return;
        
        System.out.println("\nState Evolution (Fee = " + fee + "):");
        System.out.println("Day  Price  Cash   Hold   Action");
        System.out.println("---  -----  -----  -----  ------");
        
        int cash = 0;
        int hold = -prices[0];
        
        System.out.printf("%2d   %4d   %5d  %5d  Buy\n", 0, prices[0], cash, hold);
        
        for (int i = 1; i < prices.length; i++) {
            int prevCash = cash;
            int prevHold = hold;
            
            cash = Math.max(prevCash, prevHold + prices[i] - fee);
            hold = Math.max(prevHold, prevCash - prices[i]);
            
            // Determine action
            String action;
            if (cash > prevCash && cash == prevHold + prices[i] - fee) {
                action = "Sell";
            } else if (hold > prevHold && hold == prevCash - prices[i]) {
                action = "Buy";
            } else {
                action = "Hold";
            }
            
            System.out.printf("%2d   %4d   %5d  %5d  %s\n", 
                            i, prices[i], cash, hold, action);
        }
        
        System.out.println("\nFinal Profit: " + cash);
    }
    
    /**
     * ========================================================================
     * HELPER: Show transactions
     * ========================================================================
     */
    public void showTransactions(int[] prices, int fee) {
        if (prices == null || prices.length < 2) {
            System.out.println("No transactions possible");
            return;
        }
        
        System.out.println("\nOptimal Transactions:");
        
        int n = prices.length;
        int[] cash = new int[n];
        int[] hold = new int[n];
        
        cash[0] = 0;
        hold[0] = -prices[0];
        
        for (int i = 1; i < n; i++) {
            cash[i] = Math.max(cash[i - 1], hold[i - 1] + prices[i] - fee);
            hold[i] = Math.max(hold[i - 1], cash[i - 1] - prices[i]);
        }
        
        // Simple transaction tracking
        boolean holding = false;
        int buyPrice = 0;
        
        for (int i = 0; i < n; i++) {
            if (!holding && (i == 0 || (hold[i] > hold[i-1] && hold[i] == cash[i-1] - prices[i]))) {
                System.out.println("Day " + i + ": BUY at " + prices[i]);
                holding = true;
                buyPrice = prices[i];
            } else if (holding && cash[i] > cash[i-1] && cash[i] == hold[i-1] + prices[i] - fee) {
                int profit = prices[i] - buyPrice - fee;
                System.out.println("Day " + i + ": SELL at " + prices[i] + 
                                 " (profit: " + profit + ")");
                holding = false;
            }
        }
    }
    
    /**
     * ========================================================================
     * TESTING AND VALIDATION
     * ========================================================================
     */
    public static void main(String[] args) {
        StockWithFee solution = new StockWithFee();
        
        System.out.println("=== STOCK TRADING WITH TRANSACTION FEE ===\n");
        
        // Test Case 1: Example 1 from problem
        System.out.println("Test 1: prices=[1,3,2,8,4,9], fee=2");
        int[] prices1 = {1, 3, 2, 8, 4, 9};
        int fee1 = 2;
        System.out.println("Expected: 8");
        System.out.println("Recursive: " + solution.maxProfitRecursive(prices1, fee1));
        System.out.println("Memoization: " + solution.maxProfitMemoization(prices1, fee1));
        System.out.println("DP Arrays: " + solution.maxProfitDPArrays(prices1, fee1));
        System.out.println("Optimized: " + solution.maxProfit(prices1, fee1));
        System.out.println("Fee on Buy: " + solution.maxProfitFeeOnBuy(prices1, fee1));
        System.out.println("Greedy Opt: " + solution.maxProfitGreedyOptimized(prices1, fee1));
        solution.visualizeStates(prices1, fee1);
        solution.showTransactions(prices1, fee1);
        
        // Test Case 2: Example 2 from problem
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 2: prices=[1,3,7,5,10,3], fee=3");
        int[] prices2 = {1, 3, 7, 5, 10, 3};
        int fee2 = 3;
        System.out.println("Expected: 6");
        System.out.println("Optimized: " + solution.maxProfit(prices2, fee2));
        solution.visualizeStates(prices2, fee2);
        
        // Test Case 3: High fee (no profitable trades)
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 3: prices=[1,3,2,8], fee=10");
        int[] prices3 = {1, 3, 2, 8};
        int fee3 = 10;
        System.out.println("Expected: 0 (fee too high)");
        System.out.println("Optimized: " + solution.maxProfit(prices3, fee3));
        
        // Test Case 4: Zero fee (should match Stock II)
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 4: prices=[1,2,3,4,5], fee=0");
        int[] prices4 = {1, 2, 3, 4, 5};
        int fee4 = 0;
        System.out.println("Expected: 4 (same as unlimited)");
        System.out.println("Optimized: " + solution.maxProfit(prices4, fee4));
        
        // Test Case 5: All decreasing
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 5: prices=[5,4,3,2,1], fee=1");
        int[] prices5 = {5, 4, 3, 2, 1};
        int fee5 = 1;
        System.out.println("Expected: 0");
        System.out.println("Optimized: " + solution.maxProfit(prices5, fee5));
        
        // Test Case 6: Single transaction worth it
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 6: prices=[1,10], fee=2");
        int[] prices6 = {1, 10};
        int fee6 = 2;
        System.out.println("Expected: 7");
        System.out.println("Optimized: " + solution.maxProfit(prices6, fee6));
        
        // Test Case 7: Fee equals potential profit
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 7: prices=[1,3,2,4], fee=2");
        int[] prices7 = {1, 3, 2, 4};
        int fee7 = 2;
        System.out.println("Expected: 1 (buy 1, sell 4)");
        System.out.println("Optimized: " + solution.maxProfit(prices7, fee7));
        solution.visualizeStates(prices7, fee7);
        
        // Test Case 8: Why greedy fails
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 8: Demonstrating why simple greedy fails");
        int[] prices8 = {1, 3, 2, 8};
        int fee8 = 2;
        System.out.println("prices=[1,3,2,8], fee=2");
        System.out.println("Wrong Greedy: " + solution.maxProfitWrongGreedy(prices8, fee8));
        System.out.println("Correct DP: " + solution.maxProfit(prices8, fee8));
        System.out.println("Greedy tries: (3-1-2) + (8-2-2) = 0 + 4 = 4");
        System.out.println("Optimal: (8-1-2) = 5");
        
        // Performance comparison
        System.out.println("\n" + "=".repeat(60));
        System.out.println("PERFORMANCE COMPARISON");
        System.out.println("=".repeat(60));
        
        int[] largePrices = new int[10000];
        for (int i = 0; i < largePrices.length; i++) {
            largePrices[i] = (int)(Math.random() * 100);
        }
        int largeFee = 5;
        
        long start = System.nanoTime();
        int result1 = solution.maxProfit(largePrices, largeFee);
        long time1 = System.nanoTime() - start;
        
        start = System.nanoTime();
        int result2 = solution.maxProfitDPArrays(largePrices, largeFee);
        long time2 = System.nanoTime() - start;
        
        start = System.nanoTime();
        int result3 = solution.maxProfitGreedyOptimized(largePrices, largeFee);
        long time3 = System.nanoTime() - start;
        
        System.out.println("Array size: 10,000 elements, fee: " + largeFee);
        System.out.println("Space-optimized time: " + time1 + " ns");
        System.out.println("DP Arrays time: " + time2 + " ns");
        System.out.println("Greedy Optimized time: " + time3 + " ns");
        System.out.println("All produce same result: " + 
                         (result1 == result2 && result2 == result3));
    }
}

/**
 * ============================================================================
 * INTERVIEW TIPS SUMMARY:
 * ============================================================================
 * 
 * 1. THE KEY INSIGHT (What makes you stand out):
 *    ===========================================
 *    "This is similar to Stock II (unlimited transactions), but the fee
 *    changes the strategy. We can't use simple greedy because small trades
 *    might not be worth the fee. We need DP to decide optimally.
 *    
 *    I'll use 2-state DP:
 *    - cash: max profit if not holding
 *    - hold: max profit if holding
 *    
 *    The fee is subtracted when we sell (complete a transaction):
 *    cash = max(cash, hold + price - fee)"
 * 
 * 2. SOLUTION PROGRESSION IN INTERVIEW:
 *    ==================================
 *    - "Let me work through [1,3,2,8], fee=2..." (2 min)
 *    - "Fee prevents profitable small trades..." (1 min)
 *    - "I'll use 2-state DP like Stock II, but subtract fee..." (2 min)
 *    - Code DP with arrays (6-8 min)
 *    - Test with example (3 min)
 *    - Optimize to O(1) space (2-3 min)
 *    - Discuss complexity (2 min)
 * 
 * 3. COMMON MISTAKES TO AVOID:
 *    ========================
 *    ✗ Using simple greedy (sum positive diffs minus fees)
 *    ✗ Charging fee twice (on both buy and sell)
 *    ✗ Not handling fee=0 edge case
 *    ✗ Forgetting fee can be larger than price difference
 *    ✗ Confusing this with cooldown (different constraint!)
 * 
 * 4. WHY GREEDY FAILS:
 *    ================
 *    Greedy: Sum all (price[i] - price[i-1] - fee) if positive
 *    
 *    Fails because:
 *    [1,3,2,8], fee=2
 *    Greedy: (3-1-2) + (8-2-2) = 0 + 4 = 4
 *    Optimal: (8-1-2) = 5
 *    
 *    Fee makes holding longer better than frequent small trades!
 * 
 * 5. STATE TRANSITIONS EXPLAINED:
 *    ============================
 *    cash[i] = max(cash[i-1], hold[i-1] + prices[i] - fee)
 *      "Stay in cash OR sell and pay fee"
 *    
 *    hold[i] = max(hold[i-1], cash[i-1] - prices[i])
 *      "Keep holding OR buy from cash"
 *    
 *    Note: Fee charged on sell, not buy (though either works)
 * 
 * 6. COMPARISON WITH OTHER PROBLEMS:
 *    ===============================
 *    Stock II (no fee):
 *    - cash = max(cash, hold + price)
 *    - hold = max(hold, cash - price)
 *    
 *    With Fee:
 *    - cash = max(cash, hold + price - fee) ← Fee here!
 *    - hold = max(hold, cash - price)
 *    
 *    Just one line changes!
 * 
 * 7. COMPLEXITY ANALYSIS:
 *    ====================
 *    Space-optimized (Optimal):
 *    - Time: O(n) - Single pass
 *    - Space: O(1) - Two variables
 *    
 *    DP Arrays:
 *    - Time: O(n)
 *    - Space: O(n) - Two arrays
 *    
 *    Can't improve time beyond O(n) - must see all prices
 * 
 * 8. FOLLOW-UP QUESTIONS:
 *    ====================
 *    Q: What if fee varies by day?
 *    A: cash = max(cash, hold + price - fee[i])
 *    
 *    Q: What if fee on buy instead of sell?
 *    A: hold = max(hold, cash - price - fee) (shown in code)
 *    
 *    Q: What if both fee and cooldown?
 *    A: Need 3 states (hold, sold, rest) and subtract fee
 *    
 *    Q: What if fee is percentage of price?
 *    A: cash = max(cash, hold + price * (1 - fee%))
 *    
 *    Q: What if at most k transactions with fee?
 *    A: Combine with Stock IV approach, subtract fee
 * 
 * 9. TIME MANAGEMENT (45 min interview):
 *    ==================================
 *    - Problem understanding: 2-3 min
 *    - Identify why greedy fails: 2-3 min
 *    - Explain 2-state DP approach: 2-3 min
 *    - Code DP with arrays: 6-8 min
 *    - Test thoroughly: 3-5 min
 *    - Optimize to O(1) space: 2-3 min
 *    - Discuss complexity: 2 min
 *    - Follow-ups: Remaining time
 * 
 * 10. WHAT MAKES A STRONG PERFORMANCE:
 *     ================================
 *     ✓ Recognize similarity to Stock II
 *     ✓ Explain why simple greedy fails
 *     ✓ Use 2-state DP approach
 *     ✓ Handle fee correctly (once per transaction)
 *     ✓ Write clean, bug-free code
 *     ✓ Test edge cases (fee=0, fee>price)
 *     ✓ Optimize space to O(1)
 *     ✓ Discuss fee on buy vs sell
 *     ✓ Handle follow-up variations
 * 
 * ============================================================================
 * ADDITIONAL INSIGHTS:
 * ============================================================================
 * 
 * 1. FEE ON BUY VS FEE ON SELL:
 *    ==========================
 *    Mathematically equivalent:
 *    
 *    Fee on sell:
 *    cash = max(cash, hold + price - fee)
 *    hold = max(hold, cash - price)
 *    
 *    Fee on buy:
 *    cash = max(cash, hold + price)
 *    hold = max(hold, cash - price - fee)
 *    
 *    Both give same result!
 *    Choose based on problem statement clarity.
 * 
 * 2. RELATIONSHIP TO REAL TRADING:
 *    ============================
 *    Transaction fees in real markets:
 *    - Brokerage fees (per trade)
 *    - Exchange fees
 *    - SEC fees (in US)
 *    - Taxes (capital gains)
 *    
 *    This models real trading costs!
 * 
 * 3. WHY DP IS NECESSARY:
 *    ====================
 *    Fee creates path dependency:
 *    - Small gains might not cover fee
 *    - Better to hold and combine gains
 *    - Need to look ahead (DP) not just greedy
 *    
 *    Example: [1,2,3,4], fee=2
 *    Greedy each day: No trades (none profitable)
 *    Optimal: Buy 1, sell 4 (profit = 1)
 * 
 * 4. GREEDY OPTIMIZATION TRICK:
 *    ==========================
 *    Advanced approach uses "effective buy price":
 *    When we sell, set minPrice = price - fee
 *    This allows reconsidering if price rises more
 *    
 *    Harder to understand but very efficient!
 * 
 * 5. EXTENSION TO MULTIPLE FEES:
 *    ===========================
 *    If different fees for buy and sell:
 *    cash = max(cash, hold + price - sellFee)
 *    hold = max(hold, cash - price - buyFee)
 *    
 *    If fee depends on profit:
 *    fee = (price - buyPrice) * feePercent
 *    More complex, might need actual transaction tracking
 * 
 * 6. MATHEMATICAL FORMULATION:
 *    =========================
 *    This is an optimal control problem:
 *    - State: {cash, hold}
 *    - Action: {buy, sell, hold}
 *    - Reward: price difference
 *    - Cost: transaction fee
 *    
 *    Goal: max Σ(rewards - costs)
 *    Subject to: can't hold multiple shares
 * 
 * ============================================================================
 */
