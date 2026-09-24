# DIY: Best Time to Buy and Sell Stock

## Problem statement

You have an array where the `i`th element is a stock's price on day `i`. You may complete at most one transaction — buy on one day, sell on a later day. Find the maximum profit achievable. (You can't sell before you buy.)

### Input

```java
{7, 1, 5, 3, 6, 4}
```

### Output

```java
5
```

Buying on day two (price 1) and selling on day five (price 6) gives a profit of `6 - 1 = 5`.

## Coding exercise

Implement `max_profit(arr)`, returning the maximum achievable profit from a single buy/sell transaction.

Same family as [Feature #4: Maximum Profit](04-feature-4-maximum-profit.md) — instead of summing a contiguous run of daily changes, track the lowest price seen so far as you scan, and at each day check the profit from selling today against that running minimum. It's the single-transaction cousin of Kadane's running-max idea: one running minimum in place of a running sum.

## Solution

```java
class Solution {

    public static int maxProfit(int[] arr) {
        if (arr.length < 2) {
            return 0;
        }

        int minPriceSoFar = arr[0];
        int bestProfit = 0;

        for (int i = 1; i < arr.length; i++) {
            int profitIfSoldToday = arr[i] - minPriceSoFar;
            bestProfit = Math.max(bestProfit, profitIfSoldToday);
            minPriceSoFar = Math.min(minPriceSoFar, arr[i]);
        }

        return bestProfit;
    }

    public static void main(String[] args) {
        int[] arr = {7, 1, 5, 3, 6, 4};
        System.out.println(maxProfit(arr)); // 5
    }
}
```

## Complexity measures

Let **n** be the length of the array.

- **Time:** `O(n)` — a single pass over the array.
- **Space:** `O(1)` — only the running minimum and best profit are tracked.
