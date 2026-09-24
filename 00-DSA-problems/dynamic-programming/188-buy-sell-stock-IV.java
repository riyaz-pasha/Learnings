import java.util.Arrays;

class BuySellStockWithTxLimit {

    public int maxProfit(int k, int[] prices) {
        int n = prices.length;
        int[][][] memo = new int[n][2][k + 1];
        for (int[][] row : memo) {
            Arrays.fill(row[0], -1);
            Arrays.fill(row[1], -1);
        }
        return findProfit(k, prices, 0, 0, memo);
    }

    private int findProfit(int k, int[] prices, int day, int hasStock, int[][][] memo) {
        if (day >= prices.length || k == 0) {
            return 0;
        }
        if (memo[day][hasStock][k] != -1) {
            return memo[day][hasStock][k];
        }
        if (hasStock == 1) {
            int sellProfit = prices[day] + findProfit(k - 1, prices, day + 1, 0, memo);
            int hold = findProfit(k, prices, day + 1, 1, memo);

            memo[day][hasStock][k] = Math.max(sellProfit, hold);
        } else {
            int buyProfit = (-prices[day]) + findProfit(k, prices, day + 1, 1, memo);
            int skip = findProfit(k, prices, day + 1, 0, memo);
            memo[day][hasStock][k] = Math.max(buyProfit, skip);
        }
        return memo[day][hasStock][k];
    }

}

class BuySellStock4_2 {

    public int maxProfit(int k, int[] prices) {
        int n = prices.length;
        int[][][] dp = new int[n + 1][2][k + 1];

        for (int day = n - 1; day >= 0; day--) {
            for (int hasStock = 0; hasStock <= 1; hasStock++) {
                for (int t = 1; t <= k; t++) {
                    if (hasStock == 1) {
                        int sell = prices[day] + dp[day + 1][0][t - 1];
                        int hold = dp[day + 1][1][t];
                        dp[day][1][t] = Math.max(sell, hold);
                    } else {
                        int buy = (-prices[day]) + dp[day + 1][1][t];
                        int skip = dp[day + 1][0][t];
                        dp[day][0][t] = Math.max(buy, skip);
                    }
                }
            }
        }
        return dp[0][0][k];
    }

}

class BuySellStock4 {

    public int maxProfit(int k, int[] prices) {
        int n = prices.length;
        int[][][] dp = new int[n + 1][2][k + 1];

        for (int day = n - 1; day >= 0; day--) {
            for (int t = 1; t <= k; t++) {

                int sell = prices[day] + dp[day + 1][0][t - 1];
                int hold = dp[day + 1][1][t];
                dp[day][1][t] = Math.max(sell, hold);

                int buy = (-prices[day]) + dp[day + 1][1][t];
                int skip = dp[day + 1][0][t];
                dp[day][0][t] = Math.max(buy, skip);

            }
        }
        return dp[0][0][k];

    }

}
