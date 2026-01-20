/**
 * BEST TIME TO BUY AND SELL STOCK III - At Most 2 Transactions
 * 
 * PROBLEM UNDERSTANDING:
 * =====================
 * Given array of stock prices where prices[i] is the price on day i:
 * - You can complete at most TWO transactions
 * - You must sell before buying again (no overlapping transactions)
 * - Goal: Maximize total profit
 * 
 * This is SIGNIFICANTLY harder than Stock I (1 transaction) and Stock II (unlimited).
 * This is a CLASSIC Dynamic Programming problem requiring careful state management.
 * 
 * DIFFICULTY: Hard
 * Companies: Google, Facebook, Amazon, Microsoft, Bloomberg, Uber
 * 
 * ============================================================================
 * HOW TO APPROACH THIS IN AN INTERVIEW:
 * ============================================================================
 * 
 * Step 1: CLARIFY THE PROBLEM (2-3 minutes)
 * -----------------------------------------
 * Ask questions:
 * - Exactly 2 or AT MOST 2? (At most - can do 0, 1, or 2)
 * - Must complete both transactions? (No, can do fewer)
 * - Can transactions overlap? (No, must sell before buying again)
 * - Empty array? (Return 0)
 * - Can prices be negative? (Usually no)
 * 
 * Step 2: WORK THROUGH EXAMPLES (5 minutes)
 * -----------------------------------------
 * Example 1: [3,3,5,0,0,3,1,4]
 * - Transaction 1: Buy at 0, sell at 3 → profit = 3
 * - Transaction 2: Buy at 1, sell at 4 → profit = 3
 * - Total = 6
 * 
 * Example 2: [1,2,3,4,5]
 * - Best is single transaction: buy at 1, sell at 5 → profit = 4
 * - Two transactions wouldn't help here
 * 
 * Example 3: [7,6,4,3,1]
 * - All decreasing → no profitable transactions → profit = 0
 * 
 * Step 3: IDENTIFY THE KEY CHALLENGE (Critical!)
 * ----------------------------------------------
 * The challenge: How to optimally split profit between two transactions?
 * 
 * KEY INSIGHT: We need to track multiple states!
 * - State after first buy
 * - State after first sell
 * - State after second buy
 * - State after second sell
 * 
 * This is a STATE MACHINE problem → Dynamic Programming!
 * 
 * Step 4: SOLUTION PROGRESSION
 * ---------------------------
 * 1. Brute Force: Try all possible splits O(n²)
 * 2. Divide and Conquer: Split array at each point O(n²)
 * 3. Dynamic Programming (4 states): O(n) time, O(1) space ⭐ OPTIMAL
 * 4. General k transactions: Extend to k transactions
 * 
 * ============================================================================
 * THE STATE MACHINE INSIGHT:
 * ============================================================================
 * 
 * At any day, we can be in one of 5 states:
 * 0. Not started (no transaction)
 * 1. First buy completed
 * 2. First sell completed  
 * 3. Second buy completed
 * 4. Second sell completed
 * 
 * Transitions:
 * State 0 → State 1 (buy first stock)
 * State 1 → State 2 (sell first stock)
 * State 2 → State 3 (buy second stock)
 * State 3 → State 4 (sell second stock)
 * 
 * We track the maximum profit in each state!
 * 
 * ============================================================================
 */

class StockTradingTwo {
    
    /**
     * ========================================================================
     * APPROACH 1: BRUTE FORCE (Try All Splits)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n²) - Try each split point
     * SPACE COMPLEXITY: O(1)
     * 
     * INTUITION:
     * Try splitting the array at each point:
     * - Find best single transaction in left part [0...i]
     * - Find best single transaction in right part [i+1...n]
     * - Sum them up
     * 
     * This works but is inefficient.
     * 
     * INTERVIEW TIP: Mention this approach to show understanding,
     * but immediately say "This is O(n²), we can do better with DP."
     */
    public int maxProfitBruteForce(int[] prices) {
        if (prices == null || prices.length < 2) return 0;
        
        int n = prices.length;
        int maxProfit = 0;
        
        // Try each possible split point
        for (int split = 0; split < n; split++) {
            // Best profit in left part [0...split]
            int leftProfit = maxProfitInRange(prices, 0, split);
            
            // Best profit in right part [split+1...n-1]
            int rightProfit = maxProfitInRange(prices, split + 1, n - 1);
            
            maxProfit = Math.max(maxProfit, leftProfit + rightProfit);
        }
        
        return maxProfit;
    }
    
    // Helper: Find max profit for single transaction in range
    private int maxProfitInRange(int[] prices, int start, int end) {
        if (start >= end) return 0;
        
        int minPrice = Integer.MAX_VALUE;
        int maxProfit = 0;
        
        for (int i = start; i <= end && i < prices.length; i++) {
            minPrice = Math.min(minPrice, prices[i]);
            maxProfit = Math.max(maxProfit, prices[i] - minPrice);
        }
        
        return maxProfit;
    }
    
    /**
     * ========================================================================
     * APPROACH 2: OPTIMIZED TWO-PASS (Left-Right DP Arrays)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n) - Two passes
     * SPACE COMPLEXITY: O(n) - Two arrays
     * 
     * INTUITION:
     * Precompute:
     * - leftProfit[i] = max profit using prices[0...i]
     * - rightProfit[i] = max profit using prices[i...n-1]
     * 
     * Then for each split point i:
     * maxProfit = max(leftProfit[i] + rightProfit[i+1])
     * 
     * This is better but still uses O(n) space.
     * 
     * INTERVIEW TIP: This is a good intermediate solution showing
     * optimization skills. Mention you can optimize further to O(1) space.
     */
    public int maxProfitTwoPass(int[] prices) {
        if (prices == null || prices.length < 2) return 0;
        
        int n = prices.length;
        
        // leftProfit[i] = max profit achievable in [0...i]
        int[] leftProfit = new int[n];
        
        // Compute left profits (same as Stock I)
        int minPrice = prices[0];
        for (int i = 1; i < n; i++) {
            minPrice = Math.min(minPrice, prices[i]);
            leftProfit[i] = Math.max(leftProfit[i - 1], prices[i] - minPrice);
        }
        
        // rightProfit[i] = max profit achievable in [i...n-1]
        int[] rightProfit = new int[n];
        
        // Compute right profits (reverse of Stock I)
        int maxPrice = prices[n - 1];
        for (int i = n - 2; i >= 0; i--) {
            maxPrice = Math.max(maxPrice, prices[i]);
            rightProfit[i] = Math.max(rightProfit[i + 1], maxPrice - prices[i]);
        }
        
        // Find best split point
        int maxProfit = 0;
        for (int i = 0; i < n; i++) {
            maxProfit = Math.max(maxProfit, leftProfit[i] + 
                        (i + 1 < n ? rightProfit[i + 1] : 0));
        }
        
        return maxProfit;
    }
    
    /**
     * ========================================================================
     * APPROACH 3: STATE MACHINE DP (OPTIMAL - Most Important!)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n) - Single pass
     * SPACE COMPLEXITY: O(1) - Only 4 variables
     * 
     * THE CORE DP INSIGHT:
     * ====================
     * Track 4 states representing maximum profit at each stage:
     * 
     * buy1  = max profit after BUYING first stock
     * sell1 = max profit after SELLING first stock
     * buy2  = max profit after BUYING second stock
     * sell2 = max profit after SELLING second stock
     * 
     * STATE TRANSITIONS:
     * ==================
     * For each price on day i:
     * 
     * buy1 = max(buy1, -prices[i])
     *   Either: keep previous buy1, OR buy stock today (cost -prices[i])
     * 
     * sell1 = max(sell1, buy1 + prices[i])
     *   Either: keep previous sell1, OR sell first stock today
     * 
     * buy2 = max(buy2, sell1 - prices[i])
     *   Either: keep previous buy2, OR buy second stock after first sell
     * 
     * sell2 = max(sell2, buy2 + prices[i])
     *   Either: keep previous sell2, OR sell second stock today
     * 
     * INITIAL VALUES:
     * ===============
     * buy1 = -∞ (haven't bought yet, so profit is very negative)
     * sell1 = 0 (no transaction yet)
     * buy2 = -∞ (haven't bought second stock)
     * sell2 = 0 (no second transaction yet)
     * 
     * We use -prices[0] for buy states on first iteration.
     * 
     * VISUALIZATION for [3,3,5,0,0,3,1,4]:
     * 
     * Day  Price  buy1   sell1  buy2   sell2
     *  0     3     -3      0     -3      0
     *  1     3     -3      0     -3      0
     *  2     5     -3      2     -1      2
     *  3     0     -0      2      2      2
     *  4     0     -0      2      2      2
     *  5     3     -0      3      2      5
     *  6     1     -0      3      2      5
     *  7     4     -0      4      3      6
     * 
     * Answer: sell2 = 6
     * 
     * WHY THIS WORKS:
     * ===============
     * Each state maintains the BEST possible profit at that stage.
     * By the end, sell2 contains the maximum profit from at most 2 transactions.
     * 
     * INTERVIEW TIP: This is the EXPECTED solution for senior positions.
     * Practice explaining the state machine clearly!
     */
    public int maxProfit(int[] prices) {
        if (prices == null || prices.length < 2) {
            return 0;
        }
        
        // Initialize states
        // buy1: max profit after first buy (negative cost)
        // sell1: max profit after first sell
        // buy2: max profit after second buy (after first sell)
        // sell2: max profit after second sell
        int buy1 = Integer.MIN_VALUE;
        int sell1 = 0;
        int buy2 = Integer.MIN_VALUE;
        int sell2 = 0;
        
        for (int price : prices) {
            // State 1: After first buy
            // Either keep previous state or buy today
            buy1 = Math.max(buy1, -price);
            
            // State 2: After first sell
            // Either keep previous state or sell today
            sell1 = Math.max(sell1, buy1 + price);
            
            // State 3: After second buy
            // Either keep previous state or buy second stock
            // We can only buy second after selling first
            buy2 = Math.max(buy2, sell1 - price);
            
            // State 4: After second sell
            // Either keep previous state or sell second stock
            sell2 = Math.max(sell2, buy2 + price);
        }
        
        // Maximum profit is after completing 0, 1, or 2 transactions
        // sell2 will contain the best result
        return sell2;
    }
    
    /**
     * ========================================================================
     * APPROACH 4: GENERALIZED k TRANSACTIONS
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(n * k)
     * SPACE COMPLEXITY: O(k)
     * 
     * This generalizes to k transactions (Stock IV problem).
     * For k=2, this is the same as approach 3 but more general.
     * 
     * KEY INSIGHT FOR k TRANSACTIONS:
     * We maintain buy[i] and sell[i] for each transaction i.
     * 
     * buy[i] = max profit after buying i-th stock
     * sell[i] = max profit after selling i-th stock
     * 
     * INTERVIEW TIP: Only implement if asked about generalizing to k transactions.
     * Shows you understand the pattern.
     */
    public int maxProfitKTransactions(int[] prices, int k) {
        if (prices == null || prices.length < 2 || k == 0) {
            return 0;
        }
        
        // Optimization: if k >= n/2, it's the same as unlimited transactions
        if (k >= prices.length / 2) {
            return maxProfitUnlimited(prices);
        }
        
        // buy[i] = max profit after i-th buy
        // sell[i] = max profit after i-th sell
        int[] buy = new int[k];
        int[] sell = new int[k];
        
        // Initialize all buy states to negative infinity
        for (int i = 0; i < k; i++) {
            buy[i] = Integer.MIN_VALUE;
        }
        
        // Process each price
        for (int price : prices) {
            // Update each transaction in order
            for (int i = 0; i < k; i++) {
                if (i == 0) {
                    buy[i] = Math.max(buy[i], -price);
                } else {
                    buy[i] = Math.max(buy[i], sell[i - 1] - price);
                }
                sell[i] = Math.max(sell[i], buy[i] + price);
            }
        }
        
        return sell[k - 1];
    }
    
    // Helper for unlimited transactions (Stock II)
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
     * APPROACH 5: WITH TRANSACTION TRACKING
     * ========================================================================
     * 
     * Track actual buy/sell days for the two transactions.
     * This is harder and usually not asked, but good to know.
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
    
    public static class DetailedResult {
        int totalProfit;
        java.util.List<Transaction> transactions;
        
        DetailedResult(int totalProfit, java.util.List<Transaction> transactions) {
            this.totalProfit = totalProfit;
            this.transactions = transactions;
        }
    }
    
    public DetailedResult maxProfitWithDetails(int[] prices) {
        if (prices == null || prices.length < 2) {
            return new DetailedResult(0, new java.util.ArrayList<>());
        }
        
        int n = prices.length;
        
        // Find all possible single transactions
        java.util.List<Transaction> allTransactions = new java.util.ArrayList<>();
        
        for (int buy = 0; buy < n; buy++) {
            for (int sell = buy + 1; sell < n; sell++) {
                if (prices[sell] > prices[buy]) {
                    allTransactions.add(new Transaction(buy, sell, 
                                       prices[sell] - prices[buy]));
                }
            }
        }
        
        // Find best two non-overlapping transactions
        int maxProfit = 0;
        java.util.List<Transaction> bestPair = new java.util.ArrayList<>();
        
        // Try all pairs of non-overlapping transactions
        for (int i = 0; i < allTransactions.size(); i++) {
            Transaction t1 = allTransactions.get(i);
            int profit1 = t1.profit;
            
            // Try single transaction
            if (profit1 > maxProfit) {
                maxProfit = profit1;
                bestPair = new java.util.ArrayList<>();
                bestPair.add(t1);
            }
            
            // Try adding second transaction
            for (int j = 0; j < allTransactions.size(); j++) {
                if (i == j) continue;
                
                Transaction t2 = allTransactions.get(j);
                
                // Check if non-overlapping
                if (t1.sellDay < t2.buyDay || t2.sellDay < t1.buyDay) {
                    int profit2 = profit1 + t2.profit;
                    if (profit2 > maxProfit) {
                        maxProfit = profit2;
                        bestPair = new java.util.ArrayList<>();
                        bestPair.add(t1);
                        bestPair.add(t2);
                    }
                }
            }
        }
        
        return new DetailedResult(maxProfit, bestPair);
    }
    
    /**
     * ========================================================================
     * VISUALIZATION HELPER
     * ========================================================================
     */
    public void visualizeStates(int[] prices) {
        if (prices == null || prices.length == 0) return;
        
        System.out.println("\nState Machine Evolution:");
        System.out.println("Day  Price  buy1   sell1  buy2   sell2");
        System.out.println("---  -----  -----  -----  -----  -----");
        
        int buy1 = Integer.MIN_VALUE;
        int sell1 = 0;
        int buy2 = Integer.MIN_VALUE;
        int sell2 = 0;
        
        for (int i = 0; i < prices.length; i++) {
            buy1 = Math.max(buy1, -prices[i]);
            sell1 = Math.max(sell1, buy1 + prices[i]);
            buy2 = Math.max(buy2, sell1 - prices[i]);
            sell2 = Math.max(sell2, buy2 + prices[i]);
            
            System.out.printf("%2d   %4d   %5d  %5d  %5d  %5d\n",
                            i, prices[i], buy1, sell1, buy2, sell2);
        }
        
        System.out.println("\nFinal Answer: " + sell2);
    }
    
    /**
     * ========================================================================
     * TESTING AND VALIDATION
     * ========================================================================
     */
    public static void main(String[] args) {
        StockTradingTwo solution = new StockTradingTwo();
        
        System.out.println("=== STOCK TRADING - AT MOST 2 TRANSACTIONS ===\n");
        
        // Test Case 1: Example from problem
        System.out.println("Test 1: prices = [3,3,5,0,0,3,1,4]");
        int[] prices1 = {3, 3, 5, 0, 0, 3, 1, 4};
        System.out.println("Expected: 6");
        System.out.println("Brute Force: " + solution.maxProfitBruteForce(prices1));
        System.out.println("Two Pass: " + solution.maxProfitTwoPass(prices1));
        System.out.println("State Machine: " + solution.maxProfit(prices1));
        System.out.println("k=2 General: " + solution.maxProfitKTransactions(prices1, 2));
        solution.visualizeStates(prices1);
        
        DetailedResult details1 = solution.maxProfitWithDetails(prices1);
        System.out.println("\nTransactions:");
        for (Transaction t : details1.transactions) {
            System.out.println("  " + t);
        }
        
        // Test Case 2: Single transaction optimal
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 2: prices = [1,2,3,4,5]");
        int[] prices2 = {1, 2, 3, 4, 5};
        System.out.println("Expected: 4");
        System.out.println("State Machine: " + solution.maxProfit(prices2));
        solution.visualizeStates(prices2);
        
        // Test Case 3: All decreasing
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 3: prices = [7,6,4,3,1]");
        int[] prices3 = {7, 6, 4, 3, 1};
        System.out.println("Expected: 0");
        System.out.println("State Machine: " + solution.maxProfit(prices3));
        
        // Test Case 4: Two clear transactions
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 4: prices = [1,2,4,2,5,7,2,4,9,0]");
        int[] prices4 = {1, 2, 4, 2, 5, 7, 2, 4, 9, 0};
        System.out.println("State Machine: " + solution.maxProfit(prices4));
        solution.visualizeStates(prices4);
        
        // Test Case 5: Empty and single element
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 5: prices = []");
        System.out.println("Expected: 0");
        System.out.println("State Machine: " + solution.maxProfit(new int[]{}));
        
        System.out.println("\nTest 6: prices = [5]");
        System.out.println("Expected: 0");
        System.out.println("State Machine: " + solution.maxProfit(new int[]{5}));
        
        // Test Case 7: Two elements
        System.out.println("\nTest 7: prices = [1, 5]");
        int[] prices7 = {1, 5};
        System.out.println("Expected: 4");
        System.out.println("State Machine: " + solution.maxProfit(prices7));
        
        // Test Case 8: All same prices
        System.out.println("\nTest 8: prices = [3,3,3,3,3]");
        int[] prices8 = {3, 3, 3, 3, 3};
        System.out.println("Expected: 0");
        System.out.println("State Machine: " + solution.maxProfit(prices8));
        
        // Test Case 9: Testing k=3 transactions
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 9: k=3 transactions on [1,2,4,2,5,7,2,4,9,0]");
        System.out.println("k=3: " + solution.maxProfitKTransactions(prices4, 3));
        
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
        int result2 = solution.maxProfitTwoPass(largePrices);
        long time2 = System.nanoTime() - start;
        
        start = System.nanoTime();
        int result3 = solution.maxProfitKTransactions(largePrices, 2);
        long time3 = System.nanoTime() - start;
        
        System.out.println("Array size: 10,000 elements");
        System.out.println("State Machine time: " + time1 + " ns");
        System.out.println("Two Pass time: " + time2 + " ns");
        System.out.println("k=2 General time: " + time3 + " ns");
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
 *    "This is a state machine problem. We need to track 4 states:
 *     - Maximum profit after first buy
 *     - Maximum profit after first sell
 *     - Maximum profit after second buy
 *     - Maximum profit after second sell
 *    
 *    At each day, we update all states by choosing the best action:
 *    keep the previous state or make a transaction."
 *    
 *    Clear articulation of this shows deep understanding!
 * 
 * 2. SOLUTION PROGRESSION IN INTERVIEW:
 *    ==================================
 *    - "Let me work through the example..." (3 min)
 *    - "I notice we need to track multiple transaction states..." (2 min)
 *    - "I'll use a state machine with 4 states..." (2 min)
 *    - "Let me code this approach..." (10 min)
 *    - Test with examples (5 min)
 *    - Discuss space/time complexity (3 min)
 *    - Mention generalization to k transactions (5 min)
 * 
 * 3. COMMON MISTAKES TO AVOID:
 *    ========================
 *    ✗ Trying to find actual split point (complicates unnecessarily)
 *    ✗ Not initializing buy states to MIN_VALUE
 *    ✗ Updating states in wrong order (must be sequential)
 *    ✗ Thinking you need to track actual days (unless asked)
 *    ✗ Forgetting that you can do 0 or 1 transaction (not just 2)
 *    ✗ Not handling edge cases (empty, single element)
 * 
 * 4. WHY STATE MACHINE WORKS:
 *    ========================
 *    Each state maintains the BEST profit possible at that stage.
 *    - buy1: Best profit if we've bought once (negative initially)
 *    - sell1: Best profit if we've completed one transaction
 *    - buy2: Best profit if we've bought second time (after sell1)
 *    - sell2: Best profit if we've completed two transactions
 *    
 *    By processing left to right, sell2 eventually contains
 *    the maximum profit from at most 2 transactions.
 * 
 * 5. COMPLEXITY ANALYSIS:
 *    ====================
 *    State Machine (Optimal):
 *    - Time: O(n) - Single pass through array
 *    - Space: O(1) - Only 4 variables
 *    
 *    Two Pass:
 *    - Time: O(n) - Two passes
 *    - Space: O(n) - Two arrays
 *    
 *    Brute Force:
 *    - Time: O(n²) - Try all splits
 *    - Space: O(1)
 *    
 *    General k transactions:
 *    - Time: O(n * k)
 *    - Space: O(k)
 * 
 * 6. FOLLOW-UP QUESTIONS:
 *    ====================
 *    Q: What if k transactions instead of 2?
 *    A: Generalize to k buy/sell state pairs - O(n*k) time
 *    
 *    Q: What if k is very large (k >= n/2)?
 *    A: Reduce to unlimited transactions problem (Stock II)
 *    
 *    Q: What if there's a transaction fee?
 *    A: Subtract fee when selling: sell = max(sell, buy + price - fee)
 *    
 *    Q: What if there's a cooldown?
 *    A: Need additional cooldown state in state machine
 *    
 *    Q: Can you track actual transaction days?
 *    A: Yes, but complex - need backtracking through states
 *    
 *    Q: Space optimize the two-pass approach?
 *    A: Can compute on the fly, but state machine is already O(1)
 * 
 * 7. TIME MANAGEMENT (45 min interview):
 *    ==================================
 *    - Problem understanding: 3-5 min
 *    - Work through example: 3-5 min
 *    - Identify state machine approach: 3-5 min
 *    - Code state machine solution: 10-12 min
 *    - Test thoroughly: 5-7 min
 *    - Discuss complexity: 2-3 min
 *    - Generalization to k: 5-10 min
 *    - Follow-up questions: Remaining time
 * 
 * 8. WHAT MAKES A STRONG PERFORMANCE:
 *    ================================
 *    ✓ Identify this as state machine problem quickly
 *    ✓ Draw state diagram if helpful
 *    ✓ Explain state transitions clearly
 *    ✓ Write clean, bug-free code
 *    ✓ Test edge cases (empty, decreasing, single transaction)
 *    ✓ Explain why O(1) space is possible
 *    ✓ Generalize to k transactions
 *    ✓ Discuss optimization for large k
 * 
 * 9. TESTING STRATEGY:
 *    ================
 *    Must test:
 *    ✓ Example cases from problem
 *    ✓ Empty array
 *    ✓ Single element
 *    ✓ Two elements (ascending, descending)
 *    ✓ All decreasing (profit = 0)
 *    ✓ All increasing (one transaction optimal)
 *    ✓ Two clear separate peaks
 *    ✓ All same prices
 * 
 * 10. VARIATIONS:
 *     ===========
 *     - Stock I: At most 1 transaction (easier)
 *     - Stock II: Unlimited transactions (easier - greedy)
 *     - Stock III: At most 2 transactions (this problem)
 *     - Stock IV: At most k transactions (harder generalization)
 *     - With transaction fee (medium)
 *     - With cooldown (medium)
 * 
 * ============================================================================
 * ADDITIONAL INSIGHTS:
 * ============================================================================
 * 
 * 1. WHY THIS IS HARD:
 *    ================
 *    - Multiple transactions create interdependencies
 *    - Can't use simple greedy (need to consider future)
 *    - State space is non-obvious
 *    - Easy to make off-by-one errors
 * 
 * 2. RELATIONSHIP TO OTHER PROBLEMS:
 *    ==============================
 *    - Similar to "House Robber" (state machine)
 *    - Similar to "Best Time to Buy/Sell with Cooldown"
 *    - General framework for "at most k" problems
 *    - State machine pattern appears in many DP problems
 * 
 * 3. OPTIMIZATION INSIGHTS:
 *    =====================
 *    - If k >= n/2, unlimited transactions = greedy
 *    - Can't do better than O(n) time (must see all prices)
 *    - O(1) space is achievable (vs O(n) naive DP)
 *    - State machine more elegant than explicit DP table
 * 
 * 4. REAL-WORLD CONSIDERATIONS:
 *    ==========================
 *    In actual trading:
 *    - Pattern Day Trading rules (limit trades)
 *    - Transaction costs and taxes
 *    - Bid-ask spread
 *    - Market impact
 *    - Short selling restrictions
 * 
 * 5. MATHEMATICAL INTERPRETATION:
 *    ============================
 *    Each state represents a "portfolio value":
 *    - buy1: cash - stock_price (holding position)
 *    - sell1: cash after selling (all cash)
 *    - buy2: cash - stock_price (holding second position)
 *    - sell2: cash after final sell (maximum wealth)
 * 
 * ============================================================================
 */

class StockProfitTwoTransactions {

    /**
     * APPROACH 1: BIDIRECTIONAL DYNAMIC PROGRAMMING
     * ---------------------------------------------------------
     * Interview Reasoning:
     * "Since we need two non-overlapping transactions, let's pick a split point 'i'.
     * We calculate the best single transaction on the left of 'i' and the best on the right.
     * We then find the best 'i' to maximize the sum."
     * * * Time Complexity: O(N) - We traverse the array 3 times.
     * * Space Complexity: O(N) - We use two extra arrays of length N.
     */
    public int maxProfitBidirectional(int[] prices) {
        if (prices == null || prices.length <= 1) return 0;
        int n = prices.length;

        // 1. Calculate Max Profit for the first transaction (Left to Right)
        int[] leftProfits = new int[n];
        int minPrice = prices[0];
        for (int i = 1; i < n; i++) {
            // Keep track of the lowest price seen so far
            minPrice = Math.min(minPrice, prices[i]);
            // Max profit is either what we had before, or selling today vs minPrice
            leftProfits[i] = Math.max(leftProfits[i - 1], prices[i] - minPrice);
        }

        // 2. Calculate Max Profit for the second transaction (Right to Left)
        int[] rightProfits = new int[n];
        int maxPrice = prices[n - 1]; // Start tracking max from the end
        for (int i = n - 2; i >= 0; i--) {
            // Keep track of the highest sell price seen in the future
            maxPrice = Math.max(maxPrice, prices[i]);
            // Max profit is either what we had later, or selling at maxPrice vs buying today
            rightProfits[i] = Math.max(rightProfits[i + 1], maxPrice - prices[i]);
        }

        // 3. Combine the two
        int maxProfit = 0;
        for (int i = 0; i < n; i++) {
            // Profit if we split the transactions at day 'i'
            maxProfit = Math.max(maxProfit, leftProfits[i] + rightProfits[i]);
        }

        return maxProfit;
    }

    /**
     * APPROACH 2: ONE-PASS SIMULATION (OPTIMAL)
     * ---------------------------------------------------------
     * Interview Reasoning:
     * "We can view the problem as a sequence of dependent actions: 
     * Buy1 -> Sell1 -> Buy2 -> Sell2.
     * We can iterate through the days and update the threshold for each action."
     * * Key Intuition:
     * Think of 'secondBuy' as reinvesting the money from the first trade.
     * If I made $10 in trade 1, and the stock costs $15 for trade 2, 
     * effectively I am buying it for $5 out of my original pocket.
     * * * Time Complexity: O(N) - Single pass.
     * * Space Complexity: O(1) - Only 4 variables used.
     */
    public int maxProfitOptimal(int[] prices) {
        // buy1 initialized to lowest possible integer because we want to maximize it 
        // (remember buy price is considered negative in this logic)
        // Alternatively, think of it as "min cost" initialized to MAX_VALUE.
        // Here we use the "Balance Sheet" approach:
        
        int buy1 = Integer.MIN_VALUE;
        int sell1 = 0;
        int buy2 = Integer.MIN_VALUE;
        int sell2 = 0;

        for (int price : prices) {
            // 1. FIRST TRANSACTION
            // We want to maximize our balance after buying. 
            // Since buying costs money, it's (0 - price). 
            // We take the max of (previous buy1) or (buying today).
            // Effectively, this tracks the LOWEST price seen so far (as a negative number).
            buy1 = Math.max(buy1, -price);

            // We want to maximize balance after selling.
            // It's (buy1 balance + today's price).
            sell1 = Math.max(sell1, buy1 + price);

            // 2. SECOND TRANSACTION
            // This is the clever part. We treat the profit from sell1 as the starting budget.
            // Balance after 2nd buy = (Profit from 1st trade - price of 2nd stock).
            buy2 = Math.max(buy2, sell1 - price);

            // Balance after 2nd sell = (Balance after 2nd buy + price of 2nd stock).
            sell2 = Math.max(sell2, buy2 + price);
        }

        return sell2;
    }

    public static void main(String[] args) {
        StockProfitTwoTransactions solver = new StockProfitTwoTransactions();
        
        int[] prices = {3, 3, 5, 0, 0, 3, 1, 4};
        
        System.out.println("Prices: [3, 3, 5, 0, 0, 3, 1, 4]");
        System.out.println("Bidirectional DP: " + solver.maxProfitBidirectional(prices));
        System.out.println("One-Pass Optimal: " + solver.maxProfitOptimal(prices));
        
        // Explain Example 3: [7,6,4,3,1]
        // buy1 will maximize to -1 (buying at 1).
        // sell1 will be 0 (never found a price > cost).
        // buy2 will be -1 (using 0 profit to buy at 1).
        // sell2 will be 0.
    }
}
