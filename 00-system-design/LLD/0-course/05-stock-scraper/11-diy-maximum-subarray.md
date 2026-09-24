# DIY: Maximum Subarray

## Problem statement

Given an integer array (possibly containing both positive and negative numbers, unsorted), return the largest sum achievable by any contiguous subarray.

### Input

```java
{-4, 2, -5, 1, 2, 3, 6, -5, 1}
```

### Output

```java
12
```

The subarray `1, 2, 3, 6` gives the largest sum, 12.

## Coding exercise

Implement `maxSubArray(arr)`, returning the sum of the largest contiguous subarray in `arr`.

This is the exact same algorithm as [Feature #4: Maximum Profit](04-feature-4-maximum-profit.md) — Kadane's running-max scan, applied to the raw array instead of a scraped stock-percentage array.

## Solution

```java
class Solution {

    static int maxSubArray(int[] arr) {
        int currentMax = arr[0];
        int globalMax = arr[0];

        for (int i = 1; i < arr.length; i++) {
            if (currentMax < 0) {
                currentMax = arr[i];
            } else {
                currentMax += arr[i];
            }

            if (currentMax > globalMax) {
                globalMax = currentMax;
            }
        }

        return globalMax;
    }

    public static void main(String[] args) {
        int[] arr = {-4, 2, -5, 1, 2, 3, 6, -5, 1};
        System.out.println(maxSubArray(arr)); // 12
    }
}
```

## Complexity measures

Let **n** be the length of the array.

- **Time:** `O(n)` — a single pass over the array.
- **Space:** `O(1)` — only two running variables are kept.
