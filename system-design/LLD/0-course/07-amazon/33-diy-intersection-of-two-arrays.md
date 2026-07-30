# DIY: Intersection of Two Arrays

## Problem statement

You are given two integer arrays, `nums1` and `nums2`. Return an array of their intersection. Each element in the result must be unique, and the result can be returned in any order.

### Input

```java
nums1 = [1, 2, 3, 2, 5]
nums2 = [9, 2, 3, 7, 1]
```

### Output

```java
[2, 3, 1]
```

(Order doesn't matter — any array containing exactly `1`, `2`, and `3`, with no duplicates, is a valid answer.)

## Coding exercise

Implement `intersection(nums1, nums2)`.

This is the exact same pattern as [Feature #14: Find Similar Products](14-feature-14-find-similar-products.md) — there, Amazon found the products two or more users had both purchased; here it's the bare pattern with no story attached. Dump one array into a hash set for `O(1)` membership checks, then scan the other array, keeping any element that's in the set. A second set (or a `LinkedHashSet`) on the output side de-duplicates the result.

## Solution

```java
import java.util.*;

class Solution {
    public static List<Integer> intersection(int[] nums1, int[] nums2) {
        Set<Integer> seen = new HashSet<>();
        for (int n : nums1) {
            seen.add(n);
        }

        // LinkedHashSet both de-duplicates and keeps first-seen order, purely
        // for a deterministic demo output — the problem allows any order.
        Set<Integer> matches = new LinkedHashSet<>();
        for (int n : nums2) {
            if (seen.contains(n)) {
                matches.add(n);
            }
        }

        return new ArrayList<>(matches);
    }

    public static void main(String[] args) {
        int[] nums1 = {1, 2, 3, 2, 5};
        int[] nums2 = {9, 2, 3, 7, 1};
        System.out.println(intersection(nums1, nums2));
        // [2, 3, 1]
    }
}
```

## Complexity measures

Let **n** and **m** be the lengths of `nums1` and `nums2`.

- **Time:** `O(n + m)` — building the set from `nums1` and scanning `nums2` are both linear.
- **Space:** `O(n + m)` — the lookup set and the result set can each hold up to that many distinct elements.
