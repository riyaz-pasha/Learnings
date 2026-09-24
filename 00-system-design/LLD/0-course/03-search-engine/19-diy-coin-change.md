# DIY: Coin Change

## Problem statement

Given an integer `total` and a list of coin denominations `coins` (unlimited supply of each), find the minimum number of coins that sum to `total`. Return `-1` if impossible, `0` if `total` is `0`.

### Input / Output

```java
coins = [1, 2, 5], total = 11  ->  3   (5+5+1)
coins = [2],        total = 4  ->  2   (2+2)
coins = [5],        total = 3  ->  -1  (impossible)
coins = [1, 2, 5], total = 0   ->  0
```

## Coding exercise

Implement `coinChange(coins, total)`.

This is the exact problem behind [Feature #9: Finding Minimum Servers](09-feature-9-finding-minimum-servers.md) — memoized recursion, minimizing count, unbounded supply of each denomination.

## Solution

```java
import java.util.Arrays;

class Solution {

    public static int coinChange(int[] coins, int total) {
        if (total < 1) {
            return 0;
        }

        int[] memo = new int[total + 1];
        Arrays.fill(memo, -2); // -2 = not yet computed
        return calculate(coins, total, memo);
    }

    private static int calculate(int[] coins, int remaining, int[] memo) {
        if (remaining == 0) {
            return 0;
        }
        if (remaining < 0) {
            return -1;
        }
        if (memo[remaining] != -2) {
            return memo[remaining];
        }

        int best = -1;
        for (int coin : coins) {
            int subResult = calculate(coins, remaining - coin, memo);
            if (subResult != -1 && (best == -1 || subResult + 1 < best)) {
                best = subResult + 1;
            }
        }

        memo[remaining] = best;
        return best;
    }

    public static void main(String[] args) {
        System.out.println(coinChange(new int[]{1, 2, 5}, 11)); // 3
        System.out.println(coinChange(new int[]{2}, 4));         // 2
        System.out.println(coinChange(new int[]{5}, 3));         // -1
        System.out.println(coinChange(new int[]{1, 2, 5}, 0));   // 0
    }
}
```

## Complexity measures

Let **n** be `total` and **m** be the number of coin denominations.

- **Time:** `O(n × m)`.
- **Space:** `O(n)`.
