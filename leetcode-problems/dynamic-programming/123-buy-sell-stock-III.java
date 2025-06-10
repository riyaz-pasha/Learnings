import java.util.Arrays;

class BuySellStockMax3 {

    public int maxProfit(int[] prices) {
        int n = prices.length;
        int[][][] memo = new int[n][2][3];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < 2; j++) {
                Arrays.fill(memo[i][j], -1);
            }
        }
        return findMaxProfit(prices, 0, 0, 0, memo);
    }

    private int findMaxProfit(int[] prices, int day, int hasStock, int count, int[][][] memo) {
        if (day >= prices.length || count >= 2) {
            return 0;
        }
        if (memo[day][hasStock][count] != -1) {
            return memo[day][hasStock][count];
        }
        if (hasStock == 1) {
            int sellProfit = prices[day] + findMaxProfit(prices, day + 1, 0, count + 1, memo);
            int doNothing = findMaxProfit(prices, day + 1, 1, count, memo);
            memo[day][hasStock][count] = Math.max(sellProfit, doNothing);
        } else {
            int buyProfit = (-prices[day]) + findMaxProfit(prices, day + 1, 1, count, memo);
            int doNothing = findMaxProfit(prices, day + 1, 0, count, memo);
            memo[day][hasStock][count] = Math.max(buyProfit, doNothing);
        }
        return memo[day][hasStock][count];
    }

}

class BuySellStockMax3_1 {

    public int maxProfit(int[] prices) {
        int n = prices.length;
        int[][][] dp = new int[n + 1][2][3];

        for (int day = n - 1; day >= 0; day--) {
            for (int hasStock = 0; hasStock <= 1; hasStock++) {
                for (int txnCount = 0; txnCount < 2; txnCount++) {
                    if (hasStock == 1) {
                        int sell = prices[day] + dp[day + 1][0][txnCount + 1];
                        int hold = dp[day + 1][1][txnCount];
                        dp[day][hasStock][txnCount] = Math.max(
                                sell, hold);
                    } else {
                        int buy = -prices[day] + dp[day + 1][1][txnCount];
                        int skip = dp[day + 1][0][txnCount];
                        dp[day][hasStock][txnCount] = Math.max(
                                buy, skip);
                    }
                }
            }
        }
        return dp[0][0][0];
    }
}

// If we have a stock:
// 💰 Sell it today → gain + prices[day], go to hasStock=0,txnCount+1
// 😐 Hold it → stay at hasStock=1, same txnCount

// If we don’t have a stock:
// 💸 Buy it today → spend-prices[day], go to hasStock=1, same txnCount
// 🛑 Do nothing → stay at hasStock=0, same txnCount

class BuySellStockMax3_3 {
    public int maxProfit(int[] prices) {
        int buy1 = Integer.MAX_VALUE, sell1 = 0;
        int buy2 = Integer.MAX_VALUE, sell2 = 0;
        for (int price : prices) {
            buy1 = Math.min(buy1, price);
            sell1 = Math.max(sell1, price - buy1);
            buy2 = Math.min(buy2, price - sell1);
            sell2 = Math.max(sell2, price - buy2);
        }
        return sell2;
    }
}

class BuySellStockMax3_4 {
    public int maxProfit(int[] prices) {
        int buy1 = Integer.MIN_VALUE, sell1 = 0;
        int buy2 = Integer.MIN_VALUE, sell2 = 0;
        for (int price : prices) {
            buy1 = Math.max(buy1, -price);
            sell1 = Math.max(sell1, buy1 + price);
            buy2 = Math.max(buy2, sell1 - price);
            sell2 = Math.max(sell2, buy2 + price);
        }
        return sell2;
    }
}

// 🧠 Understanding the Problem Deeply
// You can make at most two transactions, and you can’t overlap them.
// This is equivalent to:
// “Pick two non-overlapping windows (buy low, sell high) to maximize total
// profit.”
// If you were allowed only 1 transaction, the best strategy is simple:
// Track the lowest price so far, and at each point check price - minPrice.

// 🔄 Break the Problem into Two Transactions
// Now extend this:
// First, track the best 1st transaction profit you can make up to today.

// Then, use the profit from the 1st transaction as an offset to do the 2nd
// transaction.

// Let’s walk through this idea using variables:

// ✅ Step-by-Step Intuition
// Step 1: Track the best profit from the first transaction
// buy1 = max(-price): we want to buy low
// sell1 = max(sell1, price + buy1): sell for max profit

// buy1 = lowest price so far (but as -price, because we eventually want to add
// it during sell)

// sell1 = maximum profit we could get from the first sell

// Step 2: Use the profit from the first sell to do the second buy
// Now the trick: we use the profit from the first transaction as a discount for
// our second buy:
// buy2 = max(buy2, sell1 - price)

// Here’s the intuition:
// I already made sell1 profit.

// Now I want to buy again — effectively, I’m reducing the cost of this buy by
// my previous gain.

// Step 3: Track second sell
// sell2 = max(sell2, buy2 + price)

// Just like before — we sell to lock in the second profit.

// 🧱 Putting It All Together
// int buy1 = Integer.MIN_VALUE;
// int sell1 = 0;
// int buy2 = Integer.MIN_VALUE;
// int sell2 = 0;

// for (int price : prices) {
// buy1 = Math.max(buy1, -price); // Best price to buy first stock
// sell1 = Math.max(sell1, buy1 + price); // Best profit from first sell
// buy2 = Math.max(buy2, sell1 - price); // Use profit from 1st to buy 2nd
// sell2 = Math.max(sell2, buy2 + price); // Final sell to get total profit
// }
// return sell2;

// 🔁 Analogy
// Think of it as reinvesting:
// First investment: buy low, sell high → pocket some profit.

// Take the profit, reinvest into second stock.

// Sell again.

// It’s greedy in that at each day you're just updating the best scenario so far
// for each phase of the two transactions.

// ✨ Summary
// This is a distilled version of DP:
// Instead of using a table to track all states, we only track the best state at
// each stage using 4 variables.

// It works because the states are linear and transitions are simple.
