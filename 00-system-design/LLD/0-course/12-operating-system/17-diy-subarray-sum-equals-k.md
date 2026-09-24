# DIY: Subarray Sum Equals K

## Problem statement

Given an array of integers `arr` and an integer `k`, find the total number of contiguous subarrays whose sum equals `k`.

### Input

```java
arr = {1, 2, 3, 4, 5, 6, 7, 1, 23, 21, 3, 1, 2, 1, 1, 1, 1, 1, 12, 2, 3, 2, 3, 2, 2}
k = 2
```

### Output

```java
10
```

## Coding exercise

Implement `subarraySum(arr, k)`.

This is the exact same pattern as [Feature #1: Allocate Space](01-feature-1-allocate-space.md) — there, the OS needed to count contiguous runs of process memory allocations summing to a target size; here it's the bare pattern with no story attached. The approach is identical: track a running prefix sum while scanning left to right, and use a hashmap of `(prefix sum -> count seen so far)` to count, in constant time at each step, how many earlier prefixes are exactly `k` less than the current one.

## Solution

```java
import java.util.*;

class Solution {
    public static int subarraySum(int[] arr, int k) {
        int count = 0;
        int sum = 0;
        Map<Integer, Integer> seen = new HashMap<>();
        seen.put(0, 1);

        for (int x : arr) {
            sum += x;
            if (seen.containsKey(sum - k)) {
                count += seen.get(sum - k);
            }
            seen.put(sum, seen.getOrDefault(sum, 0) + 1);
        }
        return count;
    }

    public static void main(String[] args) {
        int[] arr = {1, 2, 3, 4, 5, 6, 7, 1, 23, 21, 3, 1, 2, 1, 1, 1, 1, 1, 12, 2, 3, 2, 3, 2, 2};
        System.out.println(subarraySum(arr, 2));
        // 10
    }
}
```

We walk the array once, keeping a running `sum`. At each index, any earlier index whose prefix sum was exactly `sum - k` marks the start of a subarray ending here that sums to `k` — the hashmap tells us how many such earlier indices exist in `O(1)`. We seed the map with `(0, 1)` so subarrays starting at index `0` are counted correctly.

## Complexity measures

Let **n** be the length of `arr`.

- **Time:** `O(n)` — a single left-to-right pass with constant-time hashmap operations.
- **Space:** `O(n)` — the hashmap can hold up to one entry per distinct prefix sum.
