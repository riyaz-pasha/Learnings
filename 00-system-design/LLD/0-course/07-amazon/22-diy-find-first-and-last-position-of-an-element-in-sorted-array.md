# DIY: Find First and Last Position of an Element in Sorted Array

## Problem statement

You are given an integer array `numbers`, sorted in ascending order, and a `target` value. Find the starting and ending index of `target` within the array.

If `target` is not found, return `{-1, -1}`.

### Input

```java
numbers = {2, 4, 5, 5, 6, 6, 6, 7, 7, 8, 10, 10}
target = 6
```

### Output

```java
{4, 6}
```

(`6` first appears at index 4 and last appears at index 6.)

## Coding exercise

Implement `findRange(numbers, target)`, returning the first and last index of `target` in the sorted array.

This is the exact same pattern as [Feature #5: Order Processing Milestones](05-feature-5-order-processing-milestones.md) — there, Amazon needed the first and last timestamp matching a milestone within a sorted log; here it's the bare pattern with no story attached. Run binary search twice: once biased to keep narrowing left on a match (finds the first occurrence), once biased to keep narrowing right (finds the last).

## Solution

```java
import java.util.*;

class Solution {
    public static int[] findRange(int[] numbers, int target) {
        int first = findBound(numbers, target, true);
        if (first == -1) return new int[]{-1, -1};
        int last = findBound(numbers, target, false);
        return new int[]{first, last};
    }

    // findFirst = true keeps searching left after a match (first occurrence).
    // findFirst = false keeps searching right after a match (last occurrence).
    private static int findBound(int[] numbers, int target, boolean findFirst) {
        int lo = 0, hi = numbers.length - 1, result = -1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (numbers[mid] == target) {
                result = mid;
                if (findFirst) hi = mid - 1; else lo = mid + 1;
            } else if (numbers[mid] < target) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return result;
    }

    public static void main(String[] args) {
        int[] numbers = {2, 4, 5, 5, 6, 6, 6, 7, 7, 8, 10, 10};
        System.out.println(Arrays.toString(findRange(numbers, 6)));
        // [4, 6]
    }
}
```

## Complexity measures

Let **n** be the number of elements in `numbers`.

- **Time:** `O(log n)` — two independent binary searches over the array.
- **Space:** `O(1)` — only a handful of index variables are used.
