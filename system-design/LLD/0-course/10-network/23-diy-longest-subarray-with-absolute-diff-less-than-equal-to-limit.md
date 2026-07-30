# DIY: Longest Subarray With Absolute Diff Less Than Equal to Limit

## Problem statement

Given an array of integers `nums` and an integer `limit`, return the size of the longest non-empty subarray such that the absolute difference between any two elements of it is less than or equal to `limit`.

### Input

```java
nums = {10, 1, 2, 4, 7, 2}
limit = 5
```

### Output

```
4
```

The subarray `{2, 4, 7, 2}` has length 4, and the absolute difference between its max (`7`) and min (`2`) is `5`, exactly at the limit.

## Coding exercise

Implement `longestSubarray(nums, limit)`, returning the length of the longest such subarray.

This is the exact same pattern as [Feature #10: Minimum Variation](10-feature-10-minimum-variation.md) — there, we found the longest stretch of days whose traffic variation stayed within a threshold; here it's the bare pattern, no networking story. Slide a window with two pointers, and maintain a pair of monotonic deques so the window's current max and min are always available in O(1); shrink the window from the left whenever its variation exceeds `limit`.

## Solution

```java
import java.util.*;

class Solution {
    public static int longestSubarray(int[] nums, int limit) {
        Deque<Integer> maxDeque = new ArrayDeque<>();
        Deque<Integer> minDeque = new ArrayDeque<>();
        int start = 0, end = 0, longest = 0;

        while (end < nums.length) {
            while (!minDeque.isEmpty() && nums[end] < nums[minDeque.peekLast()]) {
                minDeque.pollLast();
            }
            while (!maxDeque.isEmpty() && nums[end] > nums[maxDeque.peekLast()]) {
                maxDeque.pollLast();
            }
            minDeque.addLast(end);
            maxDeque.addLast(end);

            while (nums[maxDeque.peekFirst()] - nums[minDeque.peekFirst()] > limit) {
                start++;
                if (minDeque.peekFirst() < start) {
                    minDeque.pollFirst();
                }
                if (maxDeque.peekFirst() < start) {
                    maxDeque.pollFirst();
                }
            }

            longest = Math.max(longest, end - start + 1);
            end++;
        }
        return longest;
    }

    public static void main(String[] args) {
        int[] nums = {10, 1, 2, 4, 7, 2};
        System.out.println(longestSubarray(nums, 5));
        // 4
    }
}
```

## Complexity measures

Let **n** be the length of `nums`.

- **Time:** `O(n)` — each index enters and leaves each deque at most once, so all deque operations amortize to `O(1)` per step.
- **Space:** `O(n)` — in the worst case, one of the deques holds close to n indices.
