# DIY: Single Element in a Sorted Array

## Problem statement

Given a sorted array of integers in which every integer appears exactly twice except one, which appears once, find that single integer. The solution must run in `O(log n)` time and constant space.

### Input

```java
nums = [1, 1, 2, 3, 3, 4, 4, 8, 8]
```

### Output

```java
2
```

## Coding exercise

Implement `singleNonDuplicate(nums)`.

This is the exact same pattern as [Feature #14: Releasing Process Lock](14-feature-14-releasing-process-lock.md) — there, the OS needed to find the one process ID that appeared once (never released its lock) among IDs that otherwise appeared in acquire/release pairs; here it's the bare pattern with no story attached. The approach is identical: binary search restricted to even indices, since a properly paired prefix keeps every pair's first element on an even index.

## Solution

```java
class Solution {
    public static int singleNonDuplicate(int[] nums) {
        int lo = 0, hi = nums.length - 1;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (mid % 2 == 1) mid--; // Keep mid on an even index.

            if (nums[mid] == nums[mid + 1]) {
                lo = mid + 2; // This pair is intact - look further right.
            } else {
                hi = mid; // Pairing already broken by here.
            }
        }
        return nums[lo];
    }

    public static void main(String[] args) {
        int[] nums = {1, 1, 2, 3, 3, 4, 4, 8, 8};
        System.out.println(singleNonDuplicate(nums));
        // 2
    }
}
```

Every time `nums[mid] == nums[mid+1]` (with `mid` forced even), the pairing is still intact up through that point, so the singleton must lie further right — move `lo` past this pair. Otherwise, the pairing already broke down by `mid`, so the singleton is at or before it — move `hi` down to `mid`. The loop ends with `lo == hi` pointing straight at the singleton.

## Complexity measures

Let **n** be the length of the array.

- **Time:** `O(log n)` — each iteration halves the search range.
- **Space:** `O(1)` — only a few integer variables track the search boundaries.
