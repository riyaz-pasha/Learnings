import java.util.Arrays;

class BuySellStocksWithCooldown {

    public int maxProfit(int[] prices) {
        int n = prices.length;
        int[][] memo = new int[n][2];
        for (int i = 0; i < n; i++) {
            Arrays.fill(memo[i], -1);
        }
        return findMaxProfit(prices, 0, 0, memo);
    }

    private int findMaxProfit(int[] prices, int day, int hasStock, int[][] memo) {
        if (day >= prices.length) {
            return 0;
        }
        if (memo[day][hasStock] != -1) {
            return memo[day][hasStock];
        }
        int res;
        if (hasStock == 1) {
            int sellProfit = prices[day] + findMaxProfit(prices, day + 2, 0, memo);
            int doNothing = findMaxProfit(prices, day + 1, 1, memo);
            res = Math.max(sellProfit, doNothing);
        } else {
            int buyProfit = -prices[day] + findMaxProfit(prices, day + 1, 1, memo);
            int doNothing = findMaxProfit(prices, day + 1, 0, memo);
            res = Math.max(buyProfit, doNothing);
        }
        memo[day][hasStock] = res;
        return res;
    }

    public int maxProfit2(int[] prices) {
        int n = prices.length;
        if (n == 0)
            return 0;

        int[][] dp = new int[n + 2][2]; // +2 to safely handle cooldown case (day+2)

        for (int day = n - 1; day >= 0; day--) {
            // Case 0: Not holding a stock
            int buy = -prices[day] + dp[day + 1][1]; // Buy today
            int skipBuy = dp[day + 1][0]; // Do nothing
            dp[day][0] = Math.max(buy, skipBuy);

            // Case 1: Holding a stock
            int sell = prices[day] + dp[day + 2][0]; // Sell today and cooldown tomorrow
            int skipSell = dp[day + 1][1]; // Do nothing
            dp[day][1] = Math.max(sell, skipSell);
        }

        return dp[0][0]; // Start at day 0, not holding a stock
    }

    public int maxProfit3(int[] prices) {
        int n = prices.length;
        if (n == 0)
            return 0;

        int[] buy = new int[n]; // Max profit on day i if we buy/hold
        int[] sell = new int[n]; // Max profit on day i if we sell
        int[] cooldown = new int[n]; // Max profit on day i if we cooldown

        // Base cases
        buy[0] = -prices[0]; // If we buy on day 0
        sell[0] = 0; // Can't sell on day 0
        cooldown[0] = 0; // No profit on cooldown

        for (int i = 1; i < n; i++) {
            buy[i] = Math.max(buy[i - 1], cooldown[i - 1] - prices[i]); // buy or continue holding
            sell[i] = buy[i - 1] + prices[i]; // sell today
            cooldown[i] = Math.max(cooldown[i - 1], sell[i - 1]); // stay in cooldown or just sold
        }

        // The max profit will be the best of cooldown or sell on last day
        return Math.max(sell[n - 1], cooldown[n - 1]);
    }

}

// ✅ Key Idea: DP with States
// At every day i, we maintain 3 states:
// Hold: Max profit if you currently hold a stock.
// Sold: Max profit if you just sold today.
// Rest: Max profit if you're in cooldown (or resting, no stock).

class StockWithCooldown {

    // Time: O(n) — one pass
    // Space: O(1) — constant space (we don’t need to store arrays)
    public int maxProfit(int[] prices) {
        if (prices.length == 0)
            return 0;

        int n = prices.length;
        int hold = -prices[0]; // We bought stock on day 0
        int sold = 0; // We sold nothing yet
        int rest = 0; // Doing nothing

        for (int i = 1; i < n; i++) {
            int prevHold = hold;
            int prevSold = sold;
            int prevRest = rest;

            // Either keep holding or buy today (only if we rested yesterday)
            hold = Math.max(prevHold, prevRest - prices[i]);

            // Sell today: we must have been holding stock
            sold = prevHold + prices[i];

            // Rest today: either we rested yesterday or just sold
            rest = Math.max(prevRest, prevSold);
        }

        // The result must be in either rest or sold (can't end on a buy)
        return Math.max(sold, rest);
    }

    public int maxProfit2(int[] prices) {
        int n = prices.length;
        if (n == 0)
            return 0;

        int[][] dp = new int[n][3];

        // Base case: day 0
        dp[0][0] = -prices[0]; // Bought stock
        dp[0][1] = 0; // Can't sell on day 0
        dp[0][2] = 0; // Resting, no action

        for (int i = 1; i < n; i++) {
            // Hold: either we were already holding or we buy today from cooldown
            dp[i][0] = Math.max(dp[i - 1][0], dp[i - 1][2] - prices[i]);

            // Sell: only if we were holding yesterday
            dp[i][1] = dp[i - 1][0] + prices[i];

            // Rest: either we were resting or just sold
            dp[i][2] = Math.max(dp[i - 1][1], dp[i - 1][2]);
        }

        // On the last day, we can’t end in 'holding' state
        return Math.max(dp[n - 1][1], dp[n - 1][2]);
    }

}
