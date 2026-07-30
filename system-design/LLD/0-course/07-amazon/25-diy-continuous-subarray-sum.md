# DIY: Continuous Subarray Sum

## Problem statement

You are given a list of non-negative integers and an integer `k`. Determine whether the array has a continuous subarray of size at least 2 whose sum is a multiple of `k` (that is, the sum equals `n * k` for some integer `n`).

### Input

```java
numbers = [5, 2, 4, 6, 7]
k = 6
```

### Output

```java
true
```

(The subarray `[2, 4]` sums to `6`, which is `1 * 6`.)

## Coding exercise

Implement `checkSubarraySum(nums, k)`, returning whether such a subarray exists.

This pattern also appears earlier in this chapter, in [Feature #7: Optimize Delivery Cost](07-feature-7-optimize-delivery-cost.md) — there, Amazon's logistics division needed to spot delivery segments whose cost landed on an exact multiple of a contractor's billing unit; here it's the bare pattern with no story attached. The trick is prefix sums with a remainder map: if two prefix sums share the same remainder mod `k`, the subarray between them is a multiple of `k`. Track only the *first* index each remainder was seen at, so the gap between repeats is as large as possible, and seed remainder `0` at index `-1` to catch subarrays that start at index `0`.

## Solution

```java
import java.util.*;

class Solution {
    public static boolean checkSubarraySum(int[] nums, int k) {
        // Maps a remainder (sum so far mod k) to the earliest index it was seen at.
        Map<Integer, Integer> firstIndexOfRemainder = new HashMap<>();
        firstIndexOfRemainder.put(0, -1); // handles subarrays starting at index 0

        int runningSum = 0;
        for (int i = 0; i < nums.length; i++) {
            runningSum += nums[i];
            int remainder = runningSum % k;

            if (firstIndexOfRemainder.containsKey(remainder)) {
                // Same remainder seen before means everything between is a multiple of k.
                if (i - firstIndexOfRemainder.get(remainder) >= 2) {
                    return true;
                }
            } else {
                firstIndexOfRemainder.put(remainder, i);
            }
        }
        return false;
    }

    public static void main(String[] args) {
        int[] numbers = {5, 2, 4, 6, 7};
        System.out.println(checkSubarraySum(numbers, 6));
        // true
    }
}
```

## Complexity measures

Let **n** be the length of the array.

- **Time:** `O(n)` — a single pass, with `O(1)` hash map operations per element.
- **Space:** `O(min(n, k))` — the remainder map holds at most `k` distinct remainders (and never more than `n` entries).
