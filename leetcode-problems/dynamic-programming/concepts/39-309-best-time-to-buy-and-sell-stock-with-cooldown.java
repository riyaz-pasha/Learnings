/**
 * BEST TIME TO BUY AND SELL STOCK WITH COOLDOWN
 * 
 * PROBLEM UNDERSTANDING:
 * =====================
 * Given an array of stock prices:
 * - You can complete unlimited transactions (buy and sell multiple times)
 * - After selling, you must cooldown for one day (can't buy next day)
 * - You cannot hold multiple stocks simultaneously
 * - Goal: Maximize total profit
 * 
 * This is a MEDIUM-HARD Dynamic Programming problem requiring careful state management.
 * The cooldown constraint makes it more complex than Stock II (unlimited transactions).
 * 
 * DIFFICULTY: Medium
 * Companies: Google, Facebook, Amazon, Microsoft, Bloomberg, Uber
 * 
 * ============================================================================
 * HOW TO APPROACH THIS IN AN INTERVIEW:
 * ============================================================================
 * 
 * Step 1: CLARIFY THE PROBLEM (2-3 minutes)
 * -----------------------------------------
 * Ask questions:
 * - What exactly is cooldown? (Can't buy on day after selling)
 * - Can I sell and buy on same day? (No, must sell before buying)
 * - Can I skip buying/selling? (Yes, can hold cash or stock)
 * - Empty array? (Return 0)
 * - Single element? (Return 0, can't buy and sell)
 * 
 * Step 2: WORK THROUGH EXAMPLES (5 minutes)
 * -----------------------------------------
 * Example 1: [1,2,3,0,2]
 * - Day 0: Buy at 1 (cash = -1, holding stock)
 * - Day 1: Sell at 2 (cash = 1, profit = 1)
 * - Day 2: Cooldown (can't buy)
 * - Day 3: Buy at 0 (cash = 1, holding stock worth 0)
 * - Day 4: Sell at 2 (cash = 3, profit = 3)
 * Total profit = 3
 * 
 * Key insight: Cooldown forces us to track an additional state!
 * 
 * Step 3: IDENTIFY THE STATE MACHINE (Critical!)
 * ----------------------------------------------
 * At any day, we can be in one of THREE states:
 * 
 * State 0: HOLDING stock (have bought, not sold yet)
 * State 1: SOLD and in COOLDOWN (just sold, can't buy tomorrow)
 * State 2: RESTED (cooldown done or never bought, can buy)
 * 
 * STATE TRANSITIONS:
 * 
 *     REST ←-----------+
 *      ↓               |
 *     BUY             SELL
 *      ↓               ↑
 *    HOLD →-----------+
 *      ↓ (can hold)
 *    HOLD (stay)
 * 
 * From HOLD: Can sell (→ COOLDOWN) or hold (→ HOLD)
 * From COOLDOWN: Must rest (→ REST)
 * From REST: Can buy (→ HOLD) or rest (→ REST)
 * 
 * This is a STATE MACHINE problem → Dynamic Programming!
 * 
 * Step 4: SOLUTION PROGRESSION
 * ---------------------------
 * 1. Recursion: Try all possibilities - O(3^n) exponential
 * 2. Memoization: Cache states - O(n) time, O(n) space
 * 3. Bottom-up DP: Iterative - O(n) time, O(n) space
 * 4. Space-optimized: Variables - O(n) time, O(1) space ⭐ OPTIMAL
 * 
 * ============================================================================
 * THE 3-STATE INSIGHT:
 * ============================================================================
 * 
 * We track max profit in each state:
 * 
 * hold[i] = max profit on day i if HOLDING stock
 * sold[i] = max profit on day i if just SOLD (in cooldown)
 * rest[i] = max profit on day i if RESTING (can buy)
 * 
 * RECURRENCE RELATIONS:
 * 
 * hold[i] = max(hold[i-1], rest[i-1] - prices[i])
 *   Either: held from yesterday, OR buy today (from rest state)
 * 
 * sold[i] = hold[i-1] + prices[i]
 *   Must have held yesterday to sell today
 * 
 * rest[i] = max(rest[i-1], sold[i-1])
 *   Either: rested yesterday, OR cooldown from selling yesterday
 * 
 * ANSWER: max(sold[n-1], rest[n-1])
 *   We're either in cooldown or resting on last day
 *   (Not holding, since that would leave profit on table)
 * 
 * ============================================================================
 */

class StockCooldown {
    
    /**
     * ========================================================================
     * APPROACH 1: RECURSION (Brute Force)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(3^n) - Exponential, three choices at each step
     * SPACE COMPLEXITY: O(n) - Recursion depth
     * 
     * WHY THIS WORKS:
     * At each day, we have different choices based on current state:
     * - If holding: sell or hold
     * - If in cooldown: must rest
     * - If resting: buy or rest
     * 
     * This creates a decision tree with exponential possibilities.
     * 
     * INTERVIEW TIP: Mention briefly to show understanding, then
     * immediately say "This is exponential, we need DP."
     */
    public int maxProfitRecursive(int[] prices) {
        if (prices == null || prices.length < 2) return 0;
        return recursiveHelper(prices, 0, false, false);
    }
    
    // holding: currently holding stock
    // cooldown: in cooldown period (just sold)
    private int recursiveHelper(int[] prices, int day, boolean holding, boolean cooldown) {
        // Base case: past last day
        if (day >= prices.length) {
            return 0;
        }
        
        int maxProfit = 0;
        
        if (cooldown) {
            // In cooldown, must rest
            maxProfit = recursiveHelper(prices, day + 1, false, false);
        } else if (holding) {
            // Holding stock: sell or hold
            // Option 1: Sell today
            int sellProfit = prices[day] + recursiveHelper(prices, day + 1, false, true);
            // Option 2: Hold
            int holdProfit = recursiveHelper(prices, day + 1, true, false);
            maxProfit = Math.max(sellProfit, holdProfit);
        } else {
            // Not holding: buy or rest
            // Option 1: Buy today
            int buyProfit = -prices[day] + recursiveHelper(prices, day + 1, true, false);
            // Option 2: Rest
            int restProfit = recursiveHelper(prices, day + 1, false, false);
            maxProfit = Math.max(buyProfit, restProfit);
        }
        
        return maxProfit;
    }
    
    /**
     * ========================================================================
     * APPROACH 2: MEMOIZATION (Top-Down DP)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n) - Each state computed once
     * SPACE COMPLEXITY: O(n) - Memoization table + recursion stack
     * 
     * We cache results for each (day, holding, cooldown) combination.
     */
    public int maxProfitMemoization(int[] prices) {
        if (prices == null || prices.length < 2) return 0;
        
        // memo[day][holding][cooldown]
        Integer[][][] memo = new Integer[prices.length][2][2];
        return memoHelper(prices, 0, 0, 0, memo);
    }
    
    private int memoHelper(int[] prices, int day, int holding, int cooldown, 
                          Integer[][][] memo) {
        if (day >= prices.length) return 0;
        
        if (memo[day][holding][cooldown] != null) {
            return memo[day][holding][cooldown];
        }
        
        int maxProfit;
        
        if (cooldown == 1) {
            maxProfit = memoHelper(prices, day + 1, 0, 0, memo);
        } else if (holding == 1) {
            int sell = prices[day] + memoHelper(prices, day + 1, 0, 1, memo);
            int hold = memoHelper(prices, day + 1, 1, 0, memo);
            maxProfit = Math.max(sell, hold);
        } else {
            int buy = -prices[day] + memoHelper(prices, day + 1, 1, 0, memo);
            int rest = memoHelper(prices, day + 1, 0, 0, memo);
            maxProfit = Math.max(buy, rest);
        }
        
        memo[day][holding][cooldown] = maxProfit;
        return maxProfit;
    }
    
    /**
     * ========================================================================
     * APPROACH 3: BOTTOM-UP DP WITH ARRAYS (Clear and Recommended)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n) - Single pass
     * SPACE COMPLEXITY: O(n) - Three arrays
     * 
     * THE CORE DP INSIGHT:
     * ====================
     * We maintain three states for each day:
     * 
     * hold[i] = max profit on day i if holding stock
     * sold[i] = max profit on day i if just sold (entering cooldown)
     * rest[i] = max profit on day i if resting (can buy tomorrow)
     * 
     * STATE TRANSITIONS:
     * ==================
     * hold[i] = max(hold[i-1], rest[i-1] - prices[i])
     *   Keep holding OR buy from rest state
     * 
     * sold[i] = hold[i-1] + prices[i]
     *   Sell stock we were holding
     * 
     * rest[i] = max(rest[i-1], sold[i-1])
     *   Continue resting OR enter rest from cooldown
     * 
     * INITIAL VALUES:
     * ==============
     * Day 0:
     * hold[0] = -prices[0] (buy on day 0)
     * sold[0] = 0 (can't sell on day 0)
     * rest[0] = 0 (doing nothing)
     * 
     * ANSWER: max(sold[n-1], rest[n-1])
     *   On last day, we're either in cooldown or resting
     *   (If holding, we haven't maximized profit)
     * 
     * INTERVIEW TIP: This is a great solution to code. Clear states,
     * easy to explain, and correct!
     */
    public int maxProfitDPArrays(int[] prices) {
        if (prices == null || prices.length < 2) {
            return 0;
        }
        
        int n = prices.length;
        
        // State arrays
        int[] hold = new int[n]; // Holding stock
        int[] sold = new int[n]; // Just sold, in cooldown
        int[] rest = new int[n]; // Resting, can buy
        
        // Day 0 initialization
        hold[0] = -prices[0];  // Buy on day 0
        sold[0] = 0;           // Can't sell on day 0
        rest[0] = 0;           // Do nothing on day 0
        
        // Fill DP arrays
        for (int i = 1; i < n; i++) {
            // To hold on day i: either held yesterday or buy today from rest
            hold[i] = Math.max(hold[i - 1], rest[i - 1] - prices[i]);
            
            // To be in cooldown on day i: must sell today (were holding yesterday)
            sold[i] = hold[i - 1] + prices[i];
            
            // To rest on day i: either rested yesterday or cooldown ended
            rest[i] = Math.max(rest[i - 1], sold[i - 1]);
        }
        
        // Answer: maximum of sold or rest on last day
        // (We're definitely not holding stock on last day for max profit)
        return Math.max(sold[n - 1], rest[n - 1]);
    }
    
    /**
     * ========================================================================
     * APPROACH 4: SPACE-OPTIMIZED DP (OPTIMAL)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n) - Single pass
     * SPACE COMPLEXITY: O(1) - Only 6 variables
     * 
     * OPTIMIZATION:
     * We only need previous day's states, not entire arrays.
     * Use variables: hold, sold, rest (and prevHold, prevSold, prevRest).
     * 
     * INTERVIEW TIP: This is the EXPECTED optimal solution.
     * Shows you can optimize space after getting the logic right.
     */
    public int maxProfit(int[] prices) {
        if (prices == null || prices.length < 2) {
            return 0;
        }
        
        // Previous day's states
        int prevHold = -prices[0];  // Held on day 0
        int prevSold = 0;           // Can't sell on day 0
        int prevRest = 0;           // Resting on day 0
        
        for (int i = 1; i < prices.length; i++) {
            int price = prices[i];
            
            // Current day's states
            int hold = Math.max(prevHold, prevRest - price);
            int sold = prevHold + price;
            int rest = Math.max(prevRest, prevSold);
            
            // Update for next iteration
            prevHold = hold;
            prevSold = sold;
            prevRest = rest;
        }
        
        // Return max of sold or rest on last day
        return Math.max(prevSold, prevRest);
    }
    
    /**
     * ========================================================================
     * APPROACH 5: ALTERNATIVE 2-STATE FORMULATION
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n)
     * SPACE COMPLEXITY: O(1)
     * 
     * ALTERNATIVE VIEW:
     * Instead of 3 states, we can think of 2 states with cooldown logic:
     * 
     * buy[i] = max profit on day i ending with buying
     * sell[i] = max profit on day i ending with selling
     * 
     * But we need to track sell[i-2] for cooldown.
     * 
     * This is less intuitive but mathematically equivalent.
     */
    public int maxProfitAlternative(int[] prices) {
        if (prices == null || prices.length < 2) {
            return 0;
        }
        
        // buy: max profit if last operation was buy
        // sell: max profit if last operation was sell
        // prevSell: sell state from 2 days ago (for cooldown)
        int buy = -prices[0];
        int sell = 0;
        int prevSell = 0;
        
        for (int i = 1; i < prices.length; i++) {
            int newBuy = Math.max(buy, prevSell - prices[i]);
            int newSell = Math.max(sell, buy + prices[i]);
            
            prevSell = sell;
            buy = newBuy;
            sell = newSell;
        }
        
        return sell;
    }
    
    /**
     * ========================================================================
     * VISUALIZATION HELPER
     * ========================================================================
     */
    public void visualizeStates(int[] prices) {
        if (prices == null || prices.length == 0) return;
        
        System.out.println("\nState Machine Evolution:");
        System.out.println("Day  Price  Hold   Sold   Rest   Action");
        System.out.println("---  -----  -----  -----  -----  ------");
        
        int hold = -prices[0];
        int sold = 0;
        int rest = 0;
        
        System.out.printf("%2d   %4d   %5d  %5d  %5d  Buy\n", 0, prices[0], hold, sold, rest);
        
        for (int i = 1; i < prices.length; i++) {
            int prevHold = hold;
            int prevSold = sold;
            int prevRest = rest;
            
            hold = Math.max(prevHold, prevRest - prices[i]);
            sold = prevHold + prices[i];
            rest = Math.max(prevRest, prevSold);
            
            // Determine action
            String action = "";
            if (hold > prevHold && hold == prevRest - prices[i]) {
                action = "Buy";
            } else if (sold > prevSold) {
                action = "Sell";
            } else if (rest > prevRest && rest == prevSold) {
                action = "Cool";
            } else {
                action = "Hold/Rest";
            }
            
            System.out.printf("%2d   %4d   %5d  %5d  %5d  %s\n", 
                            i, prices[i], hold, sold, rest, action);
        }
        
        System.out.println("\nFinal Answer: " + Math.max(sold, rest));
    }
    
    /**
     * ========================================================================
     * HELPER: Show optimal transactions
     * ========================================================================
     */
    public void showOptimalPath(int[] prices) {
        if (prices == null || prices.length < 2) {
            System.out.println("No transactions possible");
            return;
        }
        
        System.out.println("\nOptimal Transaction Path:");
        
        int n = prices.length;
        int[] hold = new int[n];
        int[] sold = new int[n];
        int[] rest = new int[n];
        
        hold[0] = -prices[0];
        sold[0] = 0;
        rest[0] = 0;
        
        for (int i = 1; i < n; i++) {
            hold[i] = Math.max(hold[i - 1], rest[i - 1] - prices[i]);
            sold[i] = hold[i - 1] + prices[i];
            rest[i] = Math.max(rest[i - 1], sold[i - 1]);
        }
        
        // Backtrack to find transactions
        int i = n - 1;
        boolean inCooldown = false;
        
        while (i >= 0) {
            if (sold[i] > rest[i] && sold[i] > hold[i]) {
                System.out.println("Day " + i + ": SELL at " + prices[i]);
                inCooldown = true;
                i--;
            } else if (inCooldown) {
                System.out.println("Day " + i + ": COOLDOWN");
                inCooldown = false;
                i--;
            } else if (i > 0 && hold[i] == rest[i - 1] - prices[i] && 
                      hold[i] > hold[i - 1]) {
                System.out.println("Day " + i + ": BUY at " + prices[i]);
                i--;
            } else {
                i--;
            }
        }
    }
    
    /**
     * ========================================================================
     * TESTING AND VALIDATION
     * ========================================================================
     */
    public static void main(String[] args) {
        StockCooldown solution = new StockCooldown();
        
        System.out.println("=== STOCK TRADING WITH COOLDOWN ===\n");
        
        // Test Case 1: Example 1 from problem
        System.out.println("Test 1: prices = [1,2,3,0,2]");
        int[] prices1 = {1, 2, 3, 0, 2};
        System.out.println("Expected: 3");
        System.out.println("Recursive: " + solution.maxProfitRecursive(prices1));
        System.out.println("Memoization: " + solution.maxProfitMemoization(prices1));
        System.out.println("DP Arrays: " + solution.maxProfitDPArrays(prices1));
        System.out.println("Optimized: " + solution.maxProfit(prices1));
        System.out.println("Alternative: " + solution.maxProfitAlternative(prices1));
        solution.visualizeStates(prices1);
        
        // Test Case 2: Example 2 from problem
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 2: prices = [1]");
        int[] prices2 = {1};
        System.out.println("Expected: 0");
        System.out.println("Optimized: " + solution.maxProfit(prices2));
        
        // Test Case 3: All increasing
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 3: prices = [1,2,3,4,5]");
        int[] prices3 = {1, 2, 3, 4, 5};
        System.out.println("Expected: 4 (buy 1, sell 2, cool, buy 3, sell 4, cool, buy 4, sell 5 = no)");
        System.out.println("Actually: buy 1, sell 5 = 4 (cooldown prevents multiple)");
        System.out.println("Optimized: " + solution.maxProfit(prices3));
        solution.visualizeStates(prices3);
        
        // Test Case 4: All decreasing
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 4: prices = [5,4,3,2,1]");
        int[] prices4 = {5, 4, 3, 2, 1};
        System.out.println("Expected: 0");
        System.out.println("Optimized: " + solution.maxProfit(prices4));
        
        // Test Case 5: Multiple peaks
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 5: prices = [1,4,2,7,5,9]");
        int[] prices5 = {1, 4, 2, 7, 5, 9};
        System.out.println("Optimized: " + solution.maxProfit(prices5));
        solution.visualizeStates(prices5);
        
        // Test Case 6: Two elements
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 6: prices = [1,5]");
        int[] prices6 = {1, 5};
        System.out.println("Expected: 4");
        System.out.println("Optimized: " + solution.maxProfit(prices6));
        
        // Test Case 7: Alternating high-low
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 7: prices = [6,1,6,1,6,1,6]");
        int[] prices7 = {6, 1, 6, 1, 6, 1, 6};
        System.out.println("Optimized: " + solution.maxProfit(prices7));
        solution.visualizeStates(prices7);
        
        // Test Case 8: Empty array
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 8: prices = []");
        System.out.println("Expected: 0");
        System.out.println("Optimized: " + solution.maxProfit(new int[]{}));
        
        // Performance comparison
        System.out.println("\n" + "=".repeat(60));
        System.out.println("PERFORMANCE COMPARISON");
        System.out.println("=".repeat(60));
        
        int[] largePrices = new int[10000];
        for (int i = 0; i < largePrices.length; i++) {
            largePrices[i] = (int)(Math.random() * 100);
        }
        
        long start = System.nanoTime();
        int result1 = solution.maxProfit(largePrices);
        long time1 = System.nanoTime() - start;
        
        start = System.nanoTime();
        int result2 = solution.maxProfitDPArrays(largePrices);
        long time2 = System.nanoTime() - start;
        
        start = System.nanoTime();
        int result3 = solution.maxProfitAlternative(largePrices);
        long time3 = System.nanoTime() - start;
        
        System.out.println("Array size: 10,000 elements");
        System.out.println("Space-optimized time: " + time1 + " ns");
        System.out.println("DP Arrays time: " + time2 + " ns");
        System.out.println("Alternative time: " + time3 + " ns");
        System.out.println("All produce same result: " + 
                         (result1 == result2 && result2 == result3));
    }
}

/**
 * ============================================================================
 * INTERVIEW TIPS SUMMARY:
 * ============================================================================
 * 
 * 1. THE KEY INSIGHT (What separates great from good):
 *    =================================================
 *    "The cooldown constraint requires tracking THREE states:
 *     - HOLD: Currently holding stock
 *     - SOLD: Just sold, in cooldown
 *     - REST: Resting, can buy tomorrow
 *    
 *    Each state transitions based on actions:
 *     - HOLD → can sell (→SOLD) or hold (→HOLD)
 *     - SOLD → must rest (→REST)
 *     - REST → can buy (→HOLD) or rest (→REST)
 *    
 *    This state machine naturally leads to DP solution."
 * 
 * 2. SOLUTION PROGRESSION IN INTERVIEW:
 *    ==================================
 *    - "Let me work through [1,2,3,0,2]..." (3 min)
 *    - "The cooldown adds complexity - I need extra state..." (2 min)
 *    - "I'll track hold, sold, rest states..." (2 min)
 *    - Draw state machine diagram (2 min)
 *    - Code DP solution (8-10 min)
 *    - Test with examples (5 min)
 *    - Optimize space to O(1) (3 min)
 * 
 * 3. COMMON MISTAKES TO AVOID:
 *    ========================
 *    ✗ Trying to solve like Stock II (ignoring cooldown)
 *    ✗ Only using 2 states (buy/sell) - needs 3!
 *    ✗ Forgetting that cooldown comes AFTER sell
 *    ✗ Not handling single element edge case
 *    ✗ Returning hold state on last day (should be sold or rest)
 *    ✗ Wrong state transitions
 * 
 * 4. WHY 3 STATES ARE NEEDED:
 *    ========================
 *    Without cooldown (Stock II): 2 states (holding, not holding)
 *    With cooldown: 3 states needed because:
 *    - "Not holding" splits into "just sold" and "can buy"
 *    - Just sold → can't buy next day
 *    - Can buy → free to buy
 *    
 *    This distinction is crucial!
 * 
 * 5. STATE TRANSITIONS EXPLAINED:
 *    ============================
 *    hold[i] = max(hold[i-1], rest[i-1] - prices[i])
 *      "To hold today: either held yesterday OR buy from rest"
 *      Note: Can't buy from sold state (cooldown!)
 *    
 *    sold[i] = hold[i-1] + prices[i]
 *      "To be in cooldown: must sell today (were holding)"
 *    
 *    rest[i] = max(rest[i-1], sold[i-1])
 *      "To rest: either rested yesterday OR cooldown ended"
 * 
 * 6. COMPLEXITY ANALYSIS:
 *    ====================
 *    All DP approaches:
 *    - Time: O(n) - Single pass through prices
 *    - Space: O(n) for arrays, O(1) for optimized
 *    
 *    Recursive without memo:
 *    - Time: O(3^n) - Exponential
 *    - Space: O(n) - Recursion depth
 * 
 * 7. FOLLOW-UP QUESTIONS:
 *    ====================
 *    Q: What if cooldown is k days instead of 1?
 *    A: Track sold[i-k] instead of sold[i-1] in rest transition
 *    
 *    Q: What if there's also a transaction fee?
 *    A: Subtract fee in sold state: sold[i] = hold[i-1] + prices[i] - fee
 *    
 *    Q: What if we can only do k transactions?
 *    A: Add transaction count dimension: state[i][j] for i days, j transactions
 *    
 *    Q: Can you show actual transaction days?
 *    A: Yes, backtrack through DP table (shown in code)
 *    
 *    Q: What if cooldown only applies after profit > threshold?
 *    A: Add condition in state transition from sold to rest
 * 
 * 8. TIME MANAGEMENT (45 min interview):
 *    ==================================
 *    - Problem understanding: 3-5 min
 *    - Identify 3-state machine: 3-5 min
 *    - Draw state diagram: 2-3 min
 *    - Code DP with arrays: 8-10 min
 *    - Test thoroughly: 5-7 min
 *    - Optimize to O(1) space: 3-5 min
 *    - Discuss complexity: 2-3 min
 *    - Follow-ups: Remaining time
 * 
 * 9. WHAT MAKES A STRONG PERFORMANCE:
 *    ================================
 *    ✓ Identify need for 3 states quickly
 *    ✓ Draw clear state machine diagram
 *    ✓ Explain state transitions clearly
 *    ✓ Write bug-free code
 *    ✓ Test edge cases (single element, decreasing)
 *    ✓ Optimize space to O(1)
 *    ✓ Explain why answer is max(sold, rest) not hold
 *    ✓ Handle follow-up variations
 * 
 * 10. TESTING STRATEGY:
 *     ================
 *     Must test:
 *     ✓ Given examples from problem
 *     ✓ Single element (profit = 0)
 *     ✓ Two elements (can transact once)
 *     ✓ All increasing (cooldown limits profit)
 *     ✓ All decreasing (profit = 0)
 *     ✓ Multiple peaks and valleys
 *     ✓ Alternating high-low (tests cooldown)
 *     ✓ Empty array
 * 
 * ============================================================================
 * ADDITIONAL INSIGHTS:
 * ============================================================================
 * 
 * 1. COMPARISON WITH OTHER STOCK PROBLEMS:
 *    ====================================
 *    Stock I (k=1): 2 states (min price, max profit)
 *    Stock II (k=∞): 2 states (holding, not holding)
 *    Stock III (k=2): 4 states (buy1, sell1, buy2, sell2)
 *    Stock IV (k=var): 2k states (buy[i], sell[i])
 *    With Cooldown: 3 states (hold, sold, rest) ← This problem
 *    With Fee: 2 states but modified transitions
 * 
 * 2. WHY COOLDOWN IS DIFFERENT:
 *    ==========================
 *    Cooldown is a TIME constraint, not a TRANSACTION COUNT constraint.
 *    - Stock IV limits number of transactions
 *    - Cooldown limits when you can transact
 *    
 *    This requires tracking temporal state (just sold vs can buy).
 * 
 * 3. STATE MACHINE AS FINITE AUTOMATON:
 *    ==================================
 *    This problem is a classic Finite State Machine:
 *    - States: {HOLD, SOLD, REST}
 *    - Alphabet: {buy, sell, rest}
 *    - Transitions: defined by rules
 *    - Accept state: max(SOLD, REST)
 *    
 *    Understanding as FSM helps with correctness.
 * 
 * 4. OPTIMIZATION INSIGHTS:
 *    =====================
 *    Space optimization: O(n) → O(1)
 *    - Only need previous day's states
 *    - Use 6 variables instead of 3 arrays
 *    
 *    Can't improve time beyond O(n):
 *    - Must see every price at least once
 *    - Each price affects all states
 * 
 * 5. REAL-WORLD ANALOGY:
 *    ===================
 *    T+2 settlement rule in stock markets:
 *    - When you sell, cash isn't available immediately
 *    - Must wait for settlement period
 *    - Similar to cooldown constraint
 *    
 *    Day trading restrictions:
 *    - Pattern day trader rules limit frequent trading
 *    - Similar constraint modeling
 * 
 * 6. MATHEMATICAL FORMULATION:
 *    =========================
 *    This is an optimal control problem with state constraints:
 *    - State space: {HOLD, SOLD, REST}
 *    - Action space: {buy, sell, rest}
 *    - Reward: price difference
 *    - Constraint: can't buy day after sell
 *    
 *    DP solves via Bellman optimality principle.
 * 
 * 7. EXTENSION TO MULTIPLE COOLDOWNS:
 *    ================================
 *    If cooldown = k days:
 *    - Need k+2 states: {HOLD, SOLD1, SOLD2, ..., SOLDk, REST}
 *    - SOLD1 → SOLD2 → ... → SOLDk → REST
 *    - REST can buy
 *    
 *    Or track cooldown counter in state.
 * 
 * ============================================================================
 */
