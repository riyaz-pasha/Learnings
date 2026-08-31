import java.util.List;
import java.util.stream.IntStream;

/**
 * ============================================================================
 * INTERVIEW GUIDE: BEST TIME TO BUY AND SELL STOCK II
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "Are there any transaction fees?" 
 *      (Assumption: No. If there were, we couldn't just capture every single 
 *      tiny profit; we'd need a Dynamic Programming approach).
 *    - "Can I short sell? (Sell a stock before I buy it)" 
 *      (Assumption: No, you must buy before you sell).
 *    - "Can prices go down indefinitely?" 
 *      (Assumption: Yes. If so, the max profit is simply 0, we just don't buy).
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Goal: Maximize profit across as many transactions as we want.
 *    - Observation 1 (The Magic of Future Knowledge): Since we can see the 
 *      entire array, we know exactly when the stock goes up and when it goes down.
 *    - Observation 2 (Peak-Valley equivalence): You might think we need to find 
 *      a "local minimum" (valley) to buy and the next "local maximum" (peak) 
 *      to sell. For example, in [1, 2, 5], buying at 1 and selling at 5 yields 4.
 *    - Observation 3 (The Greedy Shortcut): Buying at 1 and selling at 5 is 
 *      mathematically IDENTICAL to buying at 1, selling at 2 (profit 1), then 
 *      buying at 2 and selling at 5 (profit 3). 1 + 3 = 4! 
 *    - Strategy: We don't need to track peaks and valleys at all. We just 
 *      compare today's price with yesterday's price. If today is higher, we 
 *      add the difference to our total profit. We simply capture every single 
 *      upward slope in the graph.
 * 
 * 3. VISUAL EXPLANATION:
 *    Prices: [7, 1, 5, 3, 6, 4]
 *    
 *    Day 0 to 1 (7 to 1): -6 (Price dropped. Do nothing.)
 *    Day 1 to 2 (1 to 5): +4 (Price went up! Capture this profit. Total = 4)
 *    Day 2 to 3 (5 to 3): -2 (Price dropped. Do nothing.)
 *    Day 3 to 4 (3 to 6): +3 (Price went up! Capture this profit. Total = 4 + 3 = 7)
 *    Day 4 to 5 (6 to 4): -2 (Price dropped. Do nothing.)
 *    
 *    Max Profit = 7.
 * 
 * ============================================================================
 */
public class BestTimeToBuyAndSellStockII {

    /**
     * APPROACH 1: Greedy Accumulation (Standard & Optimal)
     * 
     * Time Complexity: O(N) where N is the number of days. We iterate through the array once.
     * Space Complexity: O(1) auxiliary space.
     */
    public int maxProfitOptimal(int[] prices) {
        int maxProfit = 0;
        
        // Start from day 1 (index 1) and compare with the previous day
        for (int i = 1; i < prices.length; i++) {
            // If the price increased, we virtually "bought yesterday, sold today"
            if (prices[i] > prices[i - 1]) {
                maxProfit += (prices[i] - prices[i - 1]);
            }
        }
        
        return maxProfit;
    }

    /**
     * APPROACH 2: Modern Java Streams (Expressive)
     * 
     * In an interview, writing the classic loop is best, but mentioning you can 
     * do this functionally in one line shows deep API mastery.
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(1) (Stream overhead is minimal)
     */
    public int maxProfitStreams(int[] prices) {
        if (prices == null || prices.length <= 1) return 0;
        
        // Create a stream of indices from 1 to length - 1
        return IntStream.range(1, prices.length)
                        // Map each index to the profit made from the previous day
                        .map(i -> prices[i] - prices[i - 1])
                        // Filter out negative or zero profits
                        .filter(profit -> profit > 0)
                        // Sum them all up
                        .sum();
    }

    /**
     * Modern Java Feature: Using Records to organize test cases cleanly.
     * Records (introduced in Java 14) provide a concise way to create immutable data carriers.
     */
    record TestCase(int[] prices, int expected) {}

    public static void main(String[] args) {
        BestTimeToBuyAndSellStockII solver = new BestTimeToBuyAndSellStockII();
        
        // Defining test cases using our Record
        var testCases = List.of(
            new TestCase(new int[]{7, 1, 5, 3, 6, 4}, 7),
            new TestCase(new int[]{1, 2, 3, 4, 5}, 4),   // Continuous increase
            new TestCase(new int[]{7, 6, 4, 3, 1}, 0),   // Continuous decrease (no profit)
            new TestCase(new int[]{2, 2, 2, 2}, 0),      // Flatline
            new TestCase(new int[]{1}, 0)                // Edge case: Only one day
        );
        
        System.out.println("--- Running Approach 1 (Greedy O(N)) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.maxProfitOptimal(tc.prices());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
        
        System.out.println("\n--- Running Approach 2 (Java Streams) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.maxProfitStreams(tc.prices());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
    }
}
