# DIY: Find Peak Element

## Problem statement

Given an integer array `nums`, find a peak element and return its index. If the array has multiple peaks, return the index of any one of them. A peak element is strictly greater than its neighbors (imagine `nums[-1] = nums[n] = -infinity` at the boundaries). Adjacent elements are never equal, and your algorithm must run in `O(log n)` time.

### Input

```java
// Example 1
{1, 2, 3, 4, 5}

// Example 2
{2, 3, 4, 5, 1, 6}
```

### Output

```
// Example 1
4

// Example 2
3 or 5
```

## Coding exercise

Implement `findPeakElement(nums)`, returning the index of a peak element in `O(log n)` time.

This is the exact same pattern as [Feature #12: Peak Signal Strength](12-feature-12-peak-signal-strength.md) — there, we located a locally strongest access point along an expressway; here it's the bare pattern, no networking story. Binary search using the slope at the midpoint: if it's falling, a peak is guaranteed to the left (inclusive); if it's rising, a peak is guaranteed to the right.

## Solution

```java
class Solution {
    public static int findPeakElement(int[] nums) {
        int left = 0, right = nums.length - 1;

        while (left < right) {
            int mid = (left + right) / 2;
            if (nums[mid] > nums[mid + 1]) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }
        return left;
    }

    public static void main(String[] args) {
        System.out.println(findPeakElement(new int[]{1, 2, 3, 4, 5}));
        // 4

        System.out.println(findPeakElement(new int[]{2, 3, 4, 5, 1, 6}));
        // 5 (one of two valid peaks, 3 or 5)
    }
}
```

## Complexity measures

Let **n** be the size of `nums`.

- **Time:** `O(log n)` — the search space is halved at every step.
- **Space:** `O(1)` — only the two pointers and the midpoint are used.
