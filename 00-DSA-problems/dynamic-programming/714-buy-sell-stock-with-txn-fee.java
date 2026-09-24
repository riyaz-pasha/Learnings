import java.util.Arrays;

class BuySellStockWithTxFee {

    public int maxProfit(int[] prices, int fee) {
        int n = prices.length;
        int[][] memo = new int[n][2];
        for (int i = 0; i < n; i++) {
            Arrays.fill(memo[i], -1);
        }

        return findMaxProfit(prices, fee, 0, 0, memo);
    }

    private int findMaxProfit(int[] prices, int fee, int day, int hasStock, int[][] memo) {
        if (day >= prices.length) {
            return 0;
        }
        if (memo[day][hasStock] != -1) {
            return memo[day][hasStock];
        }
        int res;
        if (hasStock == 1) {
            int sellProfit = (prices[day] - fee) + findMaxProfit(prices, fee, day + 1, 0, memo);
            int doNothing = findMaxProfit(prices, fee, day + 1, 1, memo);
            res = Math.max(sellProfit, doNothing);
        } else {
            int buyProfit = (-prices[day]) + findMaxProfit(prices, fee, day + 1, 1, memo);
            int doNothing = findMaxProfit(prices, fee, day + 1, 0, memo);
            res = Math.max(buyProfit, doNothing);
        }
        memo[day][hasStock] = res;
        return res;
    }

    // Time: O(n)
    // Space: O(1) (only two variables)
    // Use Dynamic Programming with 2 states:
    // hold: max profit if we’re currently holding a stock
    // profit: max profit if we’re not holding a stock
    public int maxProfit2(int[] prices, int fee) {
        int n = prices.length;
        if (n == 0)
            return 0;

        int hold = -prices[0]; // Holding a stock after day 0
        int profit = 0; // No stock in hand after day 0

        for (int i = 1; i < n; i++) {
            // Save previous state
            int prevHold = hold;
            int prevProfit = profit;

            // Update state
            hold = Math.max(prevHold, prevProfit - prices[i]); // buy
            profit = Math.max(prevProfit, prevHold + prices[i] - fee); // sell
        }

        return profit; // Max profit should be when not holding any stock
    }
}
