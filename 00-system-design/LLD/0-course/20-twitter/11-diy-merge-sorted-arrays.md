# DIY: Merge Sorted Arrays

## Problem statement

You're given two sorted integer arrays, `nums1` and `nums2`, along with `m` and `n`, the number of initialized elements in each. Merge `nums2` into `nums1` in place, so `nums1` ends up sorted and holding all `m + n` values.

Note: `nums1`'s underlying array already has size `m + n` — the trailing slots are reserved space for `nums2`'s elements, not meaningful data.

### Input

```java
// nums1 = {3, 4, 9, 0, 0, 0}
// m = 3
// nums2 = {1, 2, 7}
// n = 3
```

### Output

```java
// {1, 2, 3, 4, 7, 9}
```

## Coding exercise

Implement `mergeSorted(nums1, m, nums2, n)`, modifying `nums1` in place.

This is exactly [Feature #2: Merge Tweets In Twitter Feed](02-feature-2-merge-tweets-in-twitter-feed.md) — same problem, generic integer arrays instead of a Twitter feed and a follow's Tweets. Fill from the back so the reserved trailing space in `nums1` is used as scratch space and nothing ever needs to be shifted.

## Solution

```java
import java.util.Arrays;

class Solution {
    public static void mergeSorted(int[] nums1, int m, int[] nums2, int n) {
        int p1 = m - 1;
        int p2 = n - 1;
        int p = m + n - 1;

        while (p2 >= 0) {
            if (p1 >= 0 && nums1[p1] > nums2[p2]) {
                nums1[p] = nums1[p1];
                p1--;
            } else {
                nums1[p] = nums2[p2];
                p2--;
            }
            p--;
        }
    }

    public static void main(String[] args) {
        int[] nums1 = {3, 4, 9, 0, 0, 0};
        int[] nums2 = {1, 2, 7};
        mergeSorted(nums1, 3, nums2, 3);
        System.out.println(Arrays.toString(nums1)); // [1, 2, 3, 4, 7, 9]
    }
}
```

## Solution walkthrough

Starting from the back of both arrays (`p1` at `nums1`'s last real element, `p2` at `nums2`'s last element, `p` at `nums1`'s last slot overall), we repeatedly place the **larger** of the two current candidates into `nums1[p]` and step that side's pointer back. Since we always write the current largest remaining value into the current largest remaining empty slot, nothing already placed ever needs to move again. The loop stops once every element of `nums2` has been placed — whatever's left at the front of `nums1` is already smaller than everything placed and already sits in the right spot.

## Complexity measures

Let **m** and **n** be the number of real elements in `nums1` and `nums2`.

### Time Complexity

`O(m + n)` — every element from both arrays is read and written exactly once.

### Space Complexity

`O(1)` — the merge happens directly inside `nums1`'s existing storage.
