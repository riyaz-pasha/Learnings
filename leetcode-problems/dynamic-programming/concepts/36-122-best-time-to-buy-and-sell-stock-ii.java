/**
 * BEST TIME TO BUY AND SELL STOCK (Multiple Transactions)
 * 
 * PROBLEM UNDERSTANDING:
 * =====================
 * Given an array of stock prices where prices[i] is the price on day i:
 * - You can buy and sell multiple times
 * - You can only hold at most one share at any time
 * - You can buy and sell on the same day
 * - Goal: Maximize total profit
 * 
 * KEY INSIGHT: This is actually a GREEDY problem, not DP!
 * Many candidates overthink this as DP, but the optimal solution is simple.
 * 
 * ============================================================================
 * HOW TO APPROACH THIS IN AN INTERVIEW:
 * ============================================================================
 * 
 * Step 1: CLARIFY THE PROBLEM (1-2 minutes)
 * -----------------------------------------
 * Ask questions:
 * - Can I buy and sell on the same day? (Yes)
 * - Can I make unlimited transactions? (Yes)
 * - Must I buy before selling? (Yes, can only hold 1 share)
 * - Can prices be negative? (Usually no, but good to ask)
 * - Empty array? (Return 0)
 * 
 * Step 2: WORK THROUGH EXAMPLES (2-3 minutes)
 * -------------------------------------------
 * Example 1: [7,1,5,3,6,4]
 * - Buy at 1, sell at 5 → profit = 4
 * - Buy at 3, sell at 6 → profit = 3
 * - Total = 7
 * 
 * Example 2: [1,2,3,4,5]
 * - Buy at 1, sell at 5 → profit = 4
 * OR buy at 1 sell at 2 (+1), buy at 2 sell at 3 (+1), etc. = 4
 * 
 * Example 3: [7,6,4,3,1]
 * - Never buy (prices always decreasing) → profit = 0
 * 
 * Step 3: IDENTIFY THE KEY INSIGHT (Critical!)
 * --------------------------------------------
 * THE BREAKTHROUGH INSIGHT:
 * Since we can trade unlimited times, we can capture EVERY upward movement!
 * 
 * If prices go from 1 → 3 → 5:
 * - Option A: Buy at 1, sell at 5 = profit of 4
 * - Option B: Buy at 1, sell at 3 (+2), buy at 3, sell at 5 (+2) = profit of 4
 * 
 * Both give same result! So we can simply sum all positive differences!
 * 
 * Step 4: SOLUTION PROGRESSION
 * ---------------------------
 * 1. Brute Force: Try all possible transaction combinations
 * 2. Peak-Valley: Find all peaks and valleys
 * 3. Greedy (Optimal): Sum all positive price differences
 * 4. Dynamic Programming: State machine approach (overkill but good to know)
 * 
 * ============================================================================
 */

class StockTrading {
    
    /**
     * ========================================================================
     * APPROACH 1: BRUTE FORCE (Recursion)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(2^n) - Exponential, trying all possibilities
     * SPACE COMPLEXITY: O(n) - Recursion depth
     * 
     * WHY THIS WORKS:
     * At each day, we have two choices:
     * - If we don't own stock: buy or skip
     * - If we own stock: sell or hold
     * 
     * This creates a decision tree with 2^n possibilities.
     * 
     * INTERVIEW TIP: Mention this briefly to show you understand the problem,
     * but immediately say "this is exponential, we need a better approach."
     */
    public int maxProfitBruteForce(int[] prices) {
        if (prices == null || prices.length == 0) return 0;
        return bruteForceHelper(prices, 0, false);
    }
    
    private int bruteForceHelper(int[] prices, int day, boolean holding) {
        // Base case: no more days
        if (day >= prices.length) {
            return 0;
        }
        
        int maxProfit = 0;
        
        if (holding) {
            // Currently holding stock - can sell or hold
            // Option 1: Sell today
            int sellProfit = prices[day] + bruteForceHelper(prices, day + 1, false);
            // Option 2: Hold (do nothing)
            int holdProfit = bruteForceHelper(prices, day + 1, true);
            maxProfit = Math.max(sellProfit, holdProfit);
        } else {
            // Not holding stock - can buy or skip
            // Option 1: Buy today
            int buyProfit = -prices[day] + bruteForceHelper(prices, day + 1, true);
            // Option 2: Skip
            int skipProfit = bruteForceHelper(prices, day + 1, false);
            maxProfit = Math.max(buyProfit, skipProfit);
        }
        
        return maxProfit;
    }
    
    /**
     * ========================================================================
     * APPROACH 2: PEAK-VALLEY APPROACH
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n) - Single pass through array
     * SPACE COMPLEXITY: O(1) - Only variables
     * 
     * INTUITION:
     * Profit is maximized by buying at every valley and selling at every peak.
     * Valley = local minimum (prices[i-1] > prices[i] < prices[i+1])
     * Peak = local maximum (prices[i-1] < prices[i] > prices[i+1])
     * 
     * Example: [1, 7, 2, 3, 6, 4, 9]
     * Valleys: 1, 2
     * Peaks: 7, 6, 9
     * Transactions: Buy 1 sell 7 (+6), Buy 2 sell 6 (+4), Buy 4 sell 9 (+5) = 15
     * 
     * This approach is correct but more complex than needed.
     */
    public int maxProfitPeakValley(int[] prices) {
        if (prices == null || prices.length < 2) return 0;
        
        int maxProfit = 0;
        int valley = prices[0];
        int peak = prices[0];
        int i = 0;
        
        while (i < prices.length - 1) {
            // Find valley (local minimum)
            while (i < prices.length - 1 && prices[i] >= prices[i + 1]) {
                i++;
            }
            valley = prices[i];
            
            // Find peak (local maximum)
            while (i < prices.length - 1 && prices[i] <= prices[i + 1]) {
                i++;
            }
            peak = prices[i];
            
            // Add profit from this transaction
            maxProfit += peak - valley;
        }
        
        return maxProfit;
    }
    
    /**
     * ========================================================================
     * APPROACH 3: SIMPLE GREEDY (OPTIMAL SOLUTION - MOST ELEGANT)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n) - Single pass
     * SPACE COMPLEXITY: O(1) - No extra space
     * 
     * THE KEY INSIGHT (This is what makes you stand out in interview):
     * ================================================================
     * Since we can make unlimited transactions, we can capture EVERY price increase!
     * 
     * Mathematical proof:
     * If prices go: a → b → c where a < b < c
     * - Single transaction: profit = c - a
     * - Multiple transactions: (b - a) + (c - b) = c - a
     * They're EQUAL!
     * 
     * So we simply sum all positive differences between consecutive days.
     * 
     * Visual example: [1, 3, 2, 5, 4, 7]
     * Differences: +2, -1, +3, -1, +3
     * Sum positive: 2 + 3 + 3 = 8
     * 
     * This works because:
     * - Positive difference = profitable trade (buy yesterday, sell today)
     * - Negative difference = skip (don't trade)
     * 
     * INTERVIEW TIP: This is the EXPECTED solution. It's simple, elegant, and optimal.
     * Explain the insight clearly - this demonstrates deep understanding.
     */
    public int maxProfit(int[] prices) {
        // Edge case: empty or single day
        if (prices == null || prices.length < 2) {
            return 0;
        }
        
        int maxProfit = 0;
        
        // Sum all positive price differences
        for (int i = 1; i < prices.length; i++) {
            // If price increased, we could have bought yesterday and sold today
            if (prices[i] > prices[i - 1]) {
                maxProfit += prices[i] - prices[i - 1];
            }
        }
        
        return maxProfit;
    }
    
    /**
     * ========================================================================
     * APPROACH 4: DYNAMIC PROGRAMMING (State Machine)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n)
     * SPACE COMPLEXITY: O(n) - Can be optimized to O(1)
     * 
     * WHY USE DP WHEN GREEDY WORKS?
     * This approach is important because it generalizes to variations:
     * - Limited number of transactions (Stock III, IV)
     * - Transaction fees
     * - Cooldown periods
     * 
     * STATE DEFINITION:
     * At each day, we're in one of two states:
     * - hold[i] = max profit on day i if we're holding stock
     * - sold[i] = max profit on day i if we're not holding stock
     * 
     * STATE TRANSITIONS:
     * hold[i] = max(hold[i-1], sold[i-1] - prices[i])
     *   Either: we held from yesterday, OR we buy today
     * 
     * sold[i] = max(sold[i-1], hold[i-1] + prices[i])
     *   Either: we didn't hold from yesterday, OR we sell today
     * 
     * INTERVIEW TIP: Mention this if asked about variations or how to extend
     * the solution. Shows you understand the problem deeply.
     */
    public int maxProfitDP(int[] prices) {
        if (prices == null || prices.length < 2) return 0;
        
        int n = prices.length;
        int[] hold = new int[n];  // Max profit if holding stock on day i
        int[] sold = new int[n];  // Max profit if not holding stock on day i
        
        // Base case: Day 0
        hold[0] = -prices[0];  // Buy stock on day 0
        sold[0] = 0;           // Don't buy on day 0
        
        // Fill DP table
        for (int i = 1; i < n; i++) {
            // To hold on day i: either held from before, or buy today
            hold[i] = Math.max(hold[i - 1], sold[i - 1] - prices[i]);
            
            // To not hold on day i: either didn't hold before, or sell today
            sold[i] = Math.max(sold[i - 1], hold[i - 1] + prices[i]);
        }
        
        // Answer is max profit when not holding stock on last day
        return sold[n - 1];
    }
    
    /**
     * ========================================================================
     * APPROACH 5: SPACE-OPTIMIZED DP
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n)
     * SPACE COMPLEXITY: O(1)
     * 
     * Since we only need previous day's state, we can use variables instead of arrays.
     */
    public int maxProfitDPOptimized(int[] prices) {
        if (prices == null || prices.length < 2) return 0;
        
        // State variables
        int hold = -prices[0];  // Max profit if holding stock
        int sold = 0;           // Max profit if not holding stock
        
        for (int i = 1; i < prices.length; i++) {
            int prevHold = hold;
            int prevSold = sold;
            
            // Update states
            hold = Math.max(prevHold, prevSold - prices[i]);
            sold = Math.max(prevSold, prevHold + prices[i]);
        }
        
        return sold;
    }
    
    /**
     * ========================================================================
     * APPROACH 6: WITH TRANSACTION TRACKING
     * ========================================================================
     * 
     * Sometimes interviewers ask: "Show me the actual transactions"
     * This tracks when to buy and sell.
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
            return String.format("Buy day %d, Sell day %d, Profit: %d", 
                               buyDay, sellDay, profit);
        }
    }
    
    public static class Result {
        int totalProfit;
        java.util.List<Transaction> transactions;
        
        Result(int totalProfit, java.util.List<Transaction> transactions) {
            this.totalProfit = totalProfit;
            this.transactions = transactions;
        }
    }
    
    public Result maxProfitWithTransactions(int[] prices) {
        if (prices == null || prices.length < 2) {
            return new Result(0, new java.util.ArrayList<>());
        }
        
        java.util.List<Transaction> transactions = new java.util.ArrayList<>();
        int totalProfit = 0;
        
        int i = 0;
        while (i < prices.length - 1) {
            // Find valley (buy point)
            while (i < prices.length - 1 && prices[i] >= prices[i + 1]) {
                i++;
            }
            int buyDay = i;
            
            // Find peak (sell point)
            while (i < prices.length - 1 && prices[i] <= prices[i + 1]) {
                i++;
            }
            int sellDay = i;
            
            // Record transaction
            if (buyDay < sellDay) {
                int profit = prices[sellDay] - prices[buyDay];
                transactions.add(new Transaction(buyDay, sellDay, profit));
                totalProfit += profit;
            }
        }
        
        return new Result(totalProfit, transactions);
    }
    
    /**
     * ========================================================================
     * VISUALIZATION HELPER
     * ========================================================================
     */
    public void visualizeStrategy(int[] prices) {
        if (prices == null || prices.length == 0) return;
        
        System.out.println("\nPrice Chart:");
        System.out.print("Day:   ");
        for (int i = 0; i < prices.length; i++) {
            System.out.printf("%4d ", i);
        }
        System.out.println();
        
        System.out.print("Price: ");
        for (int price : prices) {
            System.out.printf("%4d ", price);
        }
        System.out.println();
        
        System.out.print("Action:");
        for (int i = 0; i < prices.length; i++) {
            if (i > 0) {
                if (prices[i] > prices[i - 1]) {
                    System.out.print(" ↑ B/S"); // Buy previous, sell now
                } else if (prices[i] < prices[i - 1]) {
                    System.out.print(" ↓ -- "); // Hold or skip
                } else {
                    System.out.print(" → -- "); // No change
                }
            } else {
                System.out.print("     ");
            }
        }
        System.out.println("\n");
        
        Result result = maxProfitWithTransactions(prices);
        System.out.println("Transactions:");
        for (Transaction t : result.transactions) {
            System.out.println("  " + t);
        }
        System.out.println("Total Profit: " + result.totalProfit);
    }
    
    /**
     * ========================================================================
     * TESTING AND VALIDATION
     * ========================================================================
     */
    public static void main(String[] args) {
        StockTrading solution = new StockTrading();
        
        System.out.println("=== STOCK TRADING - UNLIMITED TRANSACTIONS ===\n");
        
        // Test Case 1: Multiple profitable transactions
        System.out.println("Test 1: prices = [7,1,5,3,6,4]");
        int[] prices1 = {7, 1, 5, 3, 6, 4};
        System.out.println("Expected: 7");
        System.out.println("Greedy: " + solution.maxProfit(prices1));
        System.out.println("Peak-Valley: " + solution.maxProfitPeakValley(prices1));
        System.out.println("DP: " + solution.maxProfitDP(prices1));
        System.out.println("DP Optimized: " + solution.maxProfitDPOptimized(prices1));
        solution.visualizeStrategy(prices1);
        
        // Test Case 2: Monotonically increasing
        System.out.println("\nTest 2: prices = [1,2,3,4,5]");
        int[] prices2 = {1, 2, 3, 4, 5};
        System.out.println("Expected: 4");
        System.out.println("Greedy: " + solution.maxProfit(prices2));
        solution.visualizeStrategy(prices2);
        
        // Test Case 3: Monotonically decreasing
        System.out.println("\nTest 3: prices = [7,6,4,3,1]");
        int[] prices3 = {7, 6, 4, 3, 1};
        System.out.println("Expected: 0");
        System.out.println("Greedy: " + solution.maxProfit(prices3));
        solution.visualizeStrategy(prices3);
        
        // Test Case 4: Single element
        System.out.println("\nTest 4: prices = [5]");
        int[] prices4 = {5};
        System.out.println("Expected: 0");
        System.out.println("Greedy: " + solution.maxProfit(prices4));
        
        // Test Case 5: Two elements ascending
        System.out.println("\nTest 5: prices = [1, 5]");
        int[] prices5 = {1, 5};
        System.out.println("Expected: 4");
        System.out.println("Greedy: " + solution.maxProfit(prices5));
        
        // Test Case 6: Two elements descending
        System.out.println("\nTest 6: prices = [5, 1]");
        int[] prices6 = {5, 1};
        System.out.println("Expected: 0");
        System.out.println("Greedy: " + solution.maxProfit(prices6));
        
        // Test Case 7: Complex pattern
        System.out.println("\nTest 7: prices = [1,7,2,3,6,4,9]");
        int[] prices7 = {1, 7, 2, 3, 6, 4, 9};
        System.out.println("Expected: 15");
        System.out.println("Greedy: " + solution.maxProfit(prices7));
        solution.visualizeStrategy(prices7);
        
        // Test Case 8: All same prices
        System.out.println("\nTest 8: prices = [5,5,5,5]");
        int[] prices8 = {5, 5, 5, 5};
        System.out.println("Expected: 0");
        System.out.println("Greedy: " + solution.maxProfit(prices8));
        
        // Performance comparison
        System.out.println("\n=== PERFORMANCE COMPARISON ===");
        int[] largePrices = new int[10000];
        for (int i = 0; i < largePrices.length; i++) {
            largePrices[i] = (int)(Math.random() * 100);
        }
        
        long start = System.nanoTime();
        int result1 = solution.maxProfit(largePrices);
        long time1 = System.nanoTime() - start;
        
        start = System.nanoTime();
        int result2 = solution.maxProfitDP(largePrices);
        long time2 = System.nanoTime() - start;
        
        start = System.nanoTime();
        int result3 = solution.maxProfitDPOptimized(largePrices);
        long time3 = System.nanoTime() - start;
        
        System.out.println("Array size: 10,000 elements");
        System.out.println("Greedy time: " + time1 + " ns");
        System.out.println("DP time: " + time2 + " ns");
        System.out.println("DP Optimized time: " + time3 + " ns");
        System.out.println("All produce same result: " + 
                         (result1 == result2 && result2 == result3));
    }
}

/**
 * ============================================================================
 * INTERVIEW TIPS SUMMARY:
 * ============================================================================
 * 
 * 1. THE KEY INSIGHT (What separates good from great candidates):
 *    ========================================================
 *    "Since we can make unlimited transactions, we can capture every price
 *    increase. This means we simply sum all positive differences between
 *    consecutive days. This works because buying and selling multiple times
 *    in an upward trend gives the same profit as a single transaction."
 *    
 *    If you can articulate this clearly, you've demonstrated deep understanding.
 * 
 * 2. SOLUTION PROGRESSION IN INTERVIEW:
 *    ==================================
 *    - "Let me work through an example first..." (2 min)
 *    - "I see this is about maximizing profit with unlimited transactions..." (1 min)
 *    - "The key insight is we can capture every upward movement..." (2 min)
 *    - "So the solution is to sum all positive price differences" (1 min)
 *    - Code the greedy solution (5 min)
 *    - Test with examples (3 min)
 *    - Discuss alternatives if time permits (5 min)
 * 
 * 3. COMMON MISTAKES TO AVOID:
 *    ========================
 *    ✗ Overthinking as complex DP when greedy works
 *    ✗ Trying to track actual buy/sell days (unless asked)
 *    ✗ Missing the insight that multiple small trades = one big trade
 *    ✗ Forgetting edge cases (empty array, single element)
 *    ✗ Not handling same consecutive prices correctly
 * 
 * 4. WHAT INTERVIEWERS LOOK FOR:
 *    ==========================
 *    ✓ Can you identify the key insight quickly?
 *    ✓ Can you explain WHY the greedy approach works?
 *    ✓ Do you write clean, bug-free code?
 *    ✓ Do you test edge cases?
 *    ✓ Can you discuss alternative approaches?
 * 
 * 5. TIME MANAGEMENT (45 min interview):
 *    ==================================
 *    - Problem understanding: 2-3 min
 *    - Work through examples: 2-3 min
 *    - Identify approach: 2-3 min
 *    - Code solution: 5-7 min
 *    - Test: 3-5 min
 *    - Discuss complexity: 2-3 min
 *    - Follow-up variations: Remaining time
 * 
 * 6. FOLLOW-UP QUESTIONS YOU MIGHT GET:
 *    ==================================
 *    Q: What if you can only make one transaction?
 *    A: Different problem - track min price seen so far, max profit at each day
 *    
 *    Q: What if you can make at most k transactions?
 *    A: Need DP with state [day][transactions][holding]
 *    
 *    Q: What if there's a transaction fee?
 *    A: Subtract fee when selling: profit += prices[i] - prices[i-1] - fee
 *    
 *    Q: What if there's a cooldown period after selling?
 *    A: Need DP with cooldown state
 *    
 *    Q: Can you show the actual buy/sell days?
 *    A: Yes, track valleys and peaks (see maxProfitWithTransactions)
 *    
 *    Q: What if you can hold multiple shares?
 *    A: Different problem - becomes a scheduling problem
 * 
 * 7. COMPLEXITY DISCUSSION:
 *    =====================
 *    - Greedy: O(n) time, O(1) space - OPTIMAL
 *    - Peak-Valley: O(n) time, O(1) space - Correct but more complex
 *    - DP: O(n) time, O(n) or O(1) space - Generalizes to variations
 *    - Brute Force: O(2^n) - Too slow, mention only for completeness
 * 
 * 8. VARIATIONS OF THIS PROBLEM:
 *    ==========================
 *    - Stock I: At most 1 transaction (easier)
 *    - Stock II: Unlimited transactions (this problem)
 *    - Stock III: At most 2 transactions (harder - need DP)
 *    - Stock IV: At most k transactions (harder - need DP)
 *    - With transaction fee (medium - adjust greedy)
 *    - With cooldown (medium - need DP)
 * 
 * 9. TESTING STRATEGY:
 *    ================
 *    Always test these cases:
 *    ✓ Empty array / null
 *    ✓ Single element
 *    ✓ Two elements (ascending and descending)
 *    ✓ All ascending (maximum profit)
 *    ✓ All descending (zero profit)
 *    ✓ All same prices (zero profit)
 *    ✓ Multiple peaks and valleys
 * 
 * 10. RED FLAGS IN INTERVIEW:
 *     ======================
 *     ✗ Not asking clarifying questions
 *     ✗ Jumping to code without explaining approach
 *     ✗ Can't explain why the solution works
 *     ✗ Missing obvious edge cases in testing
 *     ✗ Writing buggy code and not catching it
 *     ✗ Not considering time/space complexity
 * 
 * ============================================================================
 * ADDITIONAL INSIGHTS:
 * ============================================================================
 * 
 * 1. MATHEMATICAL PROOF OF GREEDY APPROACH:
 *    ======================================
 *    For prices: p1 < p2 < p3
 *    Single transaction: profit = p3 - p1
 *    Multiple transactions: (p2 - p1) + (p3 - p2) = p3 - p1
 *    
 *    This telescoping property means we can decompose any profitable
 *    sequence into individual day-to-day gains.
 * 
 * 2. WHY DP APPROACH IS STILL VALUABLE:
 *    ==================================
 *    Even though greedy works for unlimited transactions, understanding
 *    the DP approach is crucial because:
 *    - It generalizes to limited transactions
 *    - It handles transaction fees
 *    - It handles cooldown periods
 *    - Shows deeper algorithmic thinking
 * 
 * 3. REAL-WORLD CONSIDERATIONS:
 *    ==========================
 *    In actual trading:
 *    - Transaction fees matter (not free trades)
 *    - Can't execute unlimited trades instantly
 *    - Market impact (large orders move prices)
 *    - Short selling has different rules
 *    - Tax implications (wash sales, capital gains)
 * 
 * 4. ALTERNATIVE INTERPRETATION:
 *    ==========================
 *    The greedy solution can be thought of as:
 *    "Go long (buy) every time the price will increase tomorrow"
 *    
 *    This is equivalent to always being in the market during uptrends
 *    and out during downtrends - the perfect market timing strategy!
 * 
 * 5. CONNECTION TO OTHER CS CONCEPTS:
 *    ===============================
 *    - Greedy algorithms (local optimal → global optimal)
 *    - Dynamic programming (when constraints added)
 *    - State machines (holding vs not holding)
 *    - Kadane's algorithm (similar sum-of-positive idea)
 * 
 * ============================================================================
 */

class StockProfitSolutions {

    /**
     * APPROACH 1: GREEDY (OPTIMAL)
     * ---------------------------------------------------------
     * Interview Reasoning:
     * We want to capture every single instance where the price rises.
     * If prices[i] > prices[i-1], that difference adds to our profit.
     * * Why this works:
     * Even if the price rises for 3 days straight (1 -> 5 -> 10),
     * (5-1) + (10-5) = 4 + 5 = 9.
     * This is mathematically equivalent to buying at 1 and selling at 10.
     * * * Time Complexity: O(N) - One pass through the array.
     * * Space Complexity: O(1) - No extra space needed.
     */
    public int maxProfitGreedy(int[] prices) {
        int maxProfit = 0;
        
        for (int i = 1; i < prices.length; i++) {
            // If the price today is higher than yesterday, 
            // we capture that profit immediately.
            if (prices[i] > prices[i - 1]) {
                maxProfit += prices[i] - prices[i - 1];
            }
        }
        
        return maxProfit;
    }

    /**
     * APPROACH 2: DYNAMIC PROGRAMMING (STATE MACHINE)
     * ---------------------------------------------------------
     * Interview Reasoning:
     * This is useful if the interviewer adds constraints (like transaction fees).
     * We track two states for every day:
     * 1. 'hold': The max profit we can have if we currently HOLD a stock.
     * 2. 'cash': The max profit we can have if we currently have NO stock (CASH).
     * * Transitions:
     * - hold[i] = max(hold[i-1], cash[i-1] - prices[i]) 
     * (Keep holding vs Buy new stock)
     * - cash[i] = max(cash[i-1], hold[i-1] + prices[i]) 
     * (Keep cash vs Sell current stock)
     * * * Time Complexity: O(N)
     * * Space Complexity: O(1) (Variables updated in place)
     */
    public int maxProfitDP(int[] prices) {
        if (prices == null || prices.length == 0) return 0;

        // Initial state for day 0:
        // If we hold a stock on day 0, we must have bought it, so profit is -prices[0]
        int hold = -prices[0];
        // If we have cash on day 0, we did nothing, profit is 0
        int cash = 0;

        for (int i = 1; i < prices.length; i++) {
            // We need to use values from the previous iteration,
            // so we store 'cash' temporarily or calculate carefully.
            // Since calculation of 'hold' uses old 'cash', and 'cash' uses old 'hold',
            // we typically need a temp variable if doing it in one loop. 
            // However, logic dictates:
            
            int prevCash = cash;
            
            // Should I sell today?
            // (Money I had holding + today's price) VS (Money I already had in cash)
            cash = Math.max(prevCash, hold + prices[i]);
            
            // Should I buy today?
            // (Money I had in cash - today's price) VS (Money I already had holding)
            hold = Math.max(hold, prevCash - prices[i]);
        }

        // We return 'cash' because ending with a stock in hand (hold) is always
        // less profitable than selling it (cash).
        return cash;
    }

    // Main method for testing
    public static void main(String[] args) {
        StockProfitSolutions solver = new StockProfitSolutions();
        
        int[] prices1 = {7, 1, 5, 3, 6, 4};
        System.out.println("Input: [7, 1, 5, 3, 6, 4]");
        System.out.println("Greedy Output: " + solver.maxProfitGreedy(prices1)); // Expected: 7
        System.out.println("DP Output:     " + solver.maxProfitDP(prices1));     // Expected: 7

        System.out.println("---");

        int[] prices2 = {1, 2, 3, 4, 5};
        System.out.println("Input: [1, 2, 3, 4, 5]");
        System.out.println("Greedy Output: " + solver.maxProfitGreedy(prices2)); // Expected: 4
        System.out.println("DP Output:     " + solver.maxProfitDP(prices2));     // Expected: 4
    }
}
