# DIY: Find Two Non-Overlapping Subarrays Each with Target Sum

## Problem statement

Given an array `arr` and an integer `target`, find two **non-overlapping** subarrays, each summing to `target`, minimizing the sum of their lengths. Return `-1` if no such pair exists.

### Input

```java
arr = {3, 1, 1, 1, 5, 1, 2, 1}
target = 3
```

### Output

```java
3
```

## Coding exercise

Implement `minSumLength(arr, target)`.

The exact same problem as [Feature #6: Find Two Sets of Consecutive Days](06-feature-6-find-two-sets-of-consecutive-days.md), without the calendar framing — sliding window (since all values here are also non-negative) plus a `dp[i]` tracking the shortest valid window ending at or before index `i`.

## Solution

```java
import java.util.Arrays;

class Solution {
    public static int minSumLength(int[] arr, int target) {
        int n = arr.length;
        int[] dp = new int[n];
        Arrays.fill(dp, Integer.MAX_VALUE / 2);

        int left = 0;
        int sum = 0;
        int minLen = Integer.MAX_VALUE / 2;
        int ans = Integer.MAX_VALUE;

        for (int right = 0; right < n; right++) {
            sum += arr[right];
            while (sum > target) {
                sum -= arr[left];
                left++;
            }

            if (sum == target) {
                int curLen = right - left + 1;
                if (left - 1 >= 0 && dp[left - 1] < Integer.MAX_VALUE / 2) {
                    ans = Math.min(ans, dp[left - 1] + curLen);
                }
                minLen = Math.min(minLen, curLen);
            }

            dp[right] = minLen;
        }

        return ans == Integer.MAX_VALUE ? -1 : ans;
    }

    public static void main(String[] args) {
        int[] arr = {3, 1, 1, 1, 5, 1, 2, 1};
        System.out.println(minSumLength(arr, 3)); // 3
    }
}
```

## Complexity measures

Let **n** be the length of `arr`.

- **Time:** `O(n)`.
- **Space:** `O(n)`.
