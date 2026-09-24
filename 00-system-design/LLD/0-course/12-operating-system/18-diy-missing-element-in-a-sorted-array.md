# DIY: Missing Element in a Sorted Array

## Problem statement

Given a sorted array of integers, find the `k`th missing number from it.

**Note:** the missing number can also lie beyond the array's last element.

### Input

```java
arr = {4, 7, 9, 10}
k = 1
```

### Output

```java
5
```

(With `k = 1`, the first missing value after `4` is `5`.)

## Coding exercise

Implement `missingElement(arr, k)`.

This is the exact same pattern as [Feature #2: Resume Process](02-feature-2-resume-process.md) — there, the OS needed to find the `n`th process ID missing from memory so it could resume it; here it's the bare pattern with no story attached. The approach is identical: recursively binary search over the array, using the identity `missing(left, right) = (arr[right] - arr[left]) - (right - left)` to count how many values are missing in a half-range, and narrowing down until the gap is localized between two adjacent array elements.

## Solution

```java
class Solution {
    public static int missingElement(int[] arr, int k) {
        return getMissingID(arr, 0, arr.length - 1, k);
    }

    private static int getMissingID(int[] arr, int left, int right, int k) {
        if (left + 1 == right) {
            return arr[left] + k;
        }
        int middle = (left + right) / 2;
        int missingInLeftHalf = (arr[middle] - arr[left]) - (middle - left);
        if (k <= missingInLeftHalf) {
            return getMissingID(arr, left, middle, k);
        } else {
            return getMissingID(arr, middle, right, k - missingInLeftHalf);
        }
    }

    public static void main(String[] args) {
        int[] arr = {4, 7, 9, 10};
        System.out.println(missingElement(arr, 1));
        // 5
    }
}
```

Each recursive call halves the search range while tracking how many values are missing in the left half. If the `k`th missing value falls within that count, we recurse left; otherwise we subtract the left half's missing count from `k` and recurse right. Once the range narrows to two adjacent array elements, the missing value we want is simply `arr[left] + k`.

## Complexity measures

Let **n** be the length of `arr`.

- **Time:** `O(log n)` — each recursive call halves the search range.
- **Space:** `O(log n)` — recursion depth grows logarithmically; no extra data structures are used.
