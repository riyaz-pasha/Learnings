/*
 * You are given an array prices where prices[i] is the price of a given stock
 * on the ith day.
 * 
 * You want to maximize your profit by choosing a single day to buy one stock
 * and choosing a different day in the future to sell that stock.
 * 
 * Return the maximum profit you can achieve from this transaction. If you
 * cannot achieve any profit, return 0.
 * 
 * Example 1:
 * Input: prices = [7,1,5,3,6,4]
 * Output: 5
 * Explanation: Buy on day 2 (price = 1) and sell on day 5 (price = 6), profit =
 * 6-1 = 5.
 * Note that buying on day 2 and selling on day 1 is not allowed because you
 * must buy before you sell.
 * 
 * Example 2:
 * Input: prices = [7,6,4,3,1]
 * Output: 0
 * Explanation: In this case, no transactions are done and the max profit = 0.
 */

class StockProfit {

    // Solution 1: Brute Force (Check all pairs)
    // Time: O(n²), Space: O(1)
    public int maxProfit1(int[] prices) {
        int maxProfit = 0;

        for (int i = 0; i < prices.length - 1; i++) {
            for (int j = i + 1; j < prices.length; j++) {
                int profit = prices[j] - prices[i];
                maxProfit = Math.max(maxProfit, profit);
            }
        }

        return maxProfit;
    }

    // Solution 2: One Pass - Track minimum price (Optimal)
    // Time: O(n), Space: O(1)
    public int maxProfit2(int[] prices) {
        if (prices == null || prices.length <= 1) {
            return 0;
        }

        int minPrice = prices[0];
        int maxProfit = 0;

        for (int i = 1; i < prices.length; i++) {
            // Update minimum price seen so far
            minPrice = Math.min(minPrice, prices[i]);

            // Calculate profit if we sell today
            int profit = prices[i] - minPrice;

            // Update maximum profit
            maxProfit = Math.max(maxProfit, profit);
        }

        return maxProfit;
    }

    // Solution 3: Dynamic Programming approach
    // Time: O(n), Space: O(1)
    public int maxProfit3(int[] prices) {
        if (prices == null || prices.length <= 1) {
            return 0;
        }

        // hold[i] = max profit if we hold stock on day i
        // sold[i] = max profit if we don't hold stock on day i
        int hold = -prices[0]; // We bought on first day
        int sold = 0; // We didn't buy anything

        for (int i = 1; i < prices.length; i++) {
            int newHold = Math.max(hold, -prices[i]); // Keep holding or buy today
            int newSold = Math.max(sold, hold + prices[i]); // Keep not holding or sell today

            hold = newHold;
            sold = newSold;
        }

        return sold; // We want to end without holding stock
    }

    // Solution 4: Kadane's Algorithm variation
    // Time: O(n), Space: O(1)
    public int maxProfit4(int[] prices) {
        if (prices == null || prices.length <= 1) {
            return 0;
        }

        int maxProfit = 0;
        int currentProfit = 0;

        for (int i = 1; i < prices.length; i++) {
            // Daily profit/loss
            int dailyProfit = prices[i] - prices[i - 1];

            // Either start new transaction or continue current one
            currentProfit = Math.max(dailyProfit, currentProfit + dailyProfit);

            // Update maximum profit seen so far
            maxProfit = Math.max(maxProfit, currentProfit);
        }

        return maxProfit;
    }

    // Helper method to demonstrate step-by-step calculation
    public int maxProfitWithExplanation(int[] prices) {
        if (prices == null || prices.length <= 1) {
            return 0;
        }

        int minPrice = prices[0];
        int maxProfit = 0;

        System.out.println("Day by day analysis:");
        System.out.println("Day 0: Price = " + prices[0] + ", Min Price = " + minPrice + ", Max Profit = " + maxProfit);

        for (int i = 1; i < prices.length; i++) {
            minPrice = Math.min(minPrice, prices[i]);
            int profit = prices[i] - minPrice;
            maxProfit = Math.max(maxProfit, profit);

            System.out.println("Day " + i + ": Price = " + prices[i] +
                    ", Min Price = " + minPrice +
                    ", Profit if sold today = " + profit +
                    ", Max Profit = " + maxProfit);
        }

        return maxProfit;
    }

    public static void main(String[] args) {
        StockProfit solution = new StockProfit();

        // Test case 1
        int[] prices1 = { 7, 1, 5, 3, 6, 4 };
        System.out.println("Example 1: " + java.util.Arrays.toString(prices1));
        System.out.println("Max Profit: " + solution.maxProfit2(prices1));
        System.out.println();

        // Detailed explanation for example 1
        solution.maxProfitWithExplanation(prices1);
        System.out.println();

        // Test case 2
        int[] prices2 = { 7, 6, 4, 3, 1 };
        System.out.println("Example 2: " + java.util.Arrays.toString(prices2));
        System.out.println("Max Profit: " + solution.maxProfit2(prices2));
        System.out.println();

        // Edge cases
        int[] prices3 = { 1 };
        System.out.println("Single day: " + solution.maxProfit2(prices3));

        int[] prices4 = { 1, 2 };
        System.out.println("Two days (profit possible): " + solution.maxProfit2(prices4));

        int[] prices5 = { 2, 1 };
        System.out.println("Two days (no profit): " + solution.maxProfit2(prices5));

        // Performance comparison for large array
        int[] largePrices = new int[10000];
        for (int i = 0; i < largePrices.length; i++) {
            largePrices[i] = (int) (Math.random() * 100);
        }

        long start = System.nanoTime();
        int result1 = solution.maxProfit1(largePrices);
        long time1 = System.nanoTime() - start;

        start = System.nanoTime();
        int result2 = solution.maxProfit2(largePrices);
        long time2 = System.nanoTime() - start;

        System.out.println("\nPerformance comparison (10,000 elements):");
        System.out.println("Brute Force: " + time1 + " ns, Result: " + result1);
        System.out.println("Optimal: " + time2 + " ns, Result: " + result2);
        System.out.println("Speedup: " + (time1 / (double) time2) + "x");
    }

}
