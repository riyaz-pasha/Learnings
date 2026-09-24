/*
 * You are given an integer array prices where prices[i] is the price of a given
 * stock on the ith day.
 * 
 * On each day, you may decide to buy and/or sell the stock. You can only hold
 * at most one share of the stock at any time. However, you can buy it then
 * immediately sell it on the same day.
 * 
 * Find and return the maximum profit you can achieve.
 * 
 * Example 1:
 * Input: prices = [7,1,5,3,6,4]
 * Output: 7
 * Explanation: Buy on day 2 (price = 1) and sell on day 3 (price = 5), profit =
 * 5-1 = 4.
 * Then buy on day 4 (price = 3) and sell on day 5 (price = 6), profit = 6-3 =
 * 3.
 * Total profit is 4 + 3 = 7.
 * 
 * Example 2:
 * Input: prices = [1,2,3,4,5]
 * Output: 4
 * Explanation: Buy on day 1 (price = 1) and sell on day 5 (price = 5), profit =
 * 5-1 = 4.
 * Total profit is 4.
 * 
 * Example 3:
 * Input: prices = [7,6,4,3,1]
 * Output: 0
 * Explanation: There is no way to make a positive profit, so we never buy the
 * stock to achieve the maximum profit of 0.
 */

class StockProfitMultiple {

    // Solution 1: Greedy - Capture all positive price differences (Optimal)
    // Time: O(n), Space: O(1)
    public int maxProfit1(int[] prices) {
        if (prices == null || prices.length <= 1) {
            return 0;
        }

        int totalProfit = 0;

        for (int i = 1; i < prices.length; i++) {
            // If price goes up, capture the profit
            if (prices[i] > prices[i - 1]) {
                totalProfit += prices[i] - prices[i - 1];
            }
        }

        return totalProfit;
    }

    // Solution 2: Peak and Valley approach
    // Time: O(n), Space: O(1)
    public int maxProfit2(int[] prices) {
        if (prices == null || prices.length <= 1) {
            return 0;
        }

        int totalProfit = 0;
        int i = 0;

        while (i < prices.length - 1) {
            // Find valley (local minimum)
            while (i < prices.length - 1 && prices[i + 1] <= prices[i]) {
                i++;
            }
            int valley = prices[i];

            // Find peak (local maximum)
            while (i < prices.length - 1 && prices[i + 1] >= prices[i]) {
                i++;
            }
            int peak = prices[i];

            // Add profit from this valley-peak pair
            totalProfit += peak - valley;
        }

        return totalProfit;
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
            int newHold = Math.max(hold, sold - prices[i]); // Keep holding or buy today
            int newSold = Math.max(sold, hold + prices[i]); // Keep not holding or sell today

            hold = newHold;
            sold = newSold;
        }

        return sold; // We want to end without holding stock
    }

    // Solution 4: State Machine approach (More explicit DP)
    // Time: O(n), Space: O(1)
    public int maxProfit4(int[] prices) {
        if (prices == null || prices.length <= 1) {
            return 0;
        }

        int buy = -prices[0]; // Max profit after buying
        int sell = 0; // Max profit after selling

        for (int i = 1; i < prices.length; i++) {
            // To buy today: we must not be holding stock, so previous state was sell
            int newBuy = Math.max(buy, sell - prices[i]);

            // To sell today: we must be holding stock, so previous state was buy
            int newSell = Math.max(sell, buy + prices[i]);

            buy = newBuy;
            sell = newSell;
        }

        return sell;
    }

    // Helper method to show detailed transaction analysis
    public int maxProfitWithTransactions(int[] prices) {
        if (prices == null || prices.length <= 1) {
            return 0;
        }

        int totalProfit = 0;
        System.out.println("Transaction Analysis:");

        for (int i = 1; i < prices.length; i++) {
            if (prices[i] > prices[i - 1]) {
                int dailyProfit = prices[i] - prices[i - 1];
                totalProfit += dailyProfit;
                System.out.println("Day " + (i - 1) + " → Day " + i +
                        ": Buy at " + prices[i - 1] +
                        ", Sell at " + prices[i] +
                        ", Profit = " + dailyProfit);
            }
        }

        System.out.println("Total Profit: " + totalProfit);
        return totalProfit;
    }

    // Method to find actual buy/sell days
    public void findBuySellDays(int[] prices) {
        if (prices == null || prices.length <= 1) {
            System.out.println("No transactions possible");
            return;
        }

        System.out.println("Optimal Buy/Sell Strategy:");
        int i = 0;

        while (i < prices.length - 1) {
            // Find valley (buy point)
            while (i < prices.length - 1 && prices[i + 1] <= prices[i]) {
                i++;
            }

            if (i == prices.length - 1) {
                break; // No more profitable transactions
            }

            int buyDay = i;
            int buyPrice = prices[i];

            // Find peak (sell point)
            while (i < prices.length - 1 && prices[i + 1] >= prices[i]) {
                i++;
            }

            int sellDay = i;
            int sellPrice = prices[i];

            System.out.println("Buy on day " + buyDay + " at price " + buyPrice +
                    ", Sell on day " + sellDay + " at price " + sellPrice +
                    ", Profit = " + (sellPrice - buyPrice));
        }
    }

    public static void main(String[] args) {
        StockProfitMultiple solution = new StockProfitMultiple();

        // Test case 1
        int[] prices1 = { 7, 1, 5, 3, 6, 4 };
        System.out.println("Example 1: " + java.util.Arrays.toString(prices1));
        System.out.println("Max Profit: " + solution.maxProfit1(prices1));
        solution.maxProfitWithTransactions(prices1);
        solution.findBuySellDays(prices1);
        System.out.println();

        // Test case 2
        int[] prices2 = { 1, 2, 3, 4, 5 };
        System.out.println("Example 2: " + java.util.Arrays.toString(prices2));
        System.out.println("Max Profit: " + solution.maxProfit1(prices2));
        solution.findBuySellDays(prices2);
        System.out.println();

        // Test case 3
        int[] prices3 = { 7, 6, 4, 3, 1 };
        System.out.println("Example 3: " + java.util.Arrays.toString(prices3));
        System.out.println("Max Profit: " + solution.maxProfit1(prices3));
        solution.findBuySellDays(prices3);
        System.out.println();

        // Edge cases
        int[] prices4 = { 1 };
        System.out.println("Single day: " + solution.maxProfit1(prices4));

        int[] prices5 = { 3, 3, 3, 3 };
        System.out.println("All same prices: " + solution.maxProfit1(prices5));

        int[] prices6 = { 1, 5, 2, 6, 3, 7 };
        System.out.println("\nZigzag pattern: " + java.util.Arrays.toString(prices6));
        System.out.println("Max Profit: " + solution.maxProfit1(prices6));
        solution.findBuySellDays(prices6);

        // Verify all solutions give same result
        System.out.println("\nVerifying all solutions:");
        int[] testPrices = { 7, 1, 5, 3, 6, 4 };
        System.out.println("Greedy: " + solution.maxProfit1(testPrices));
        System.out.println("Peak-Valley: " + solution.maxProfit2(testPrices));
        System.out.println("DP: " + solution.maxProfit3(testPrices));
        System.out.println("State Machine: " + solution.maxProfit4(testPrices));
    }

}
