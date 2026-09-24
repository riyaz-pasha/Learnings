# DIY: Monotonic Array

## Problem statement

An array is **monotonic** if it's either monotone increasing (`arr[i] <= arr[i+1]` for every `i`) or monotone decreasing (`arr[i] >= arr[i+1]` for every `i`). Return `true` if the given array is monotonic.

### Input

```java
arr = {1, 2, 3, 3, 3, 3, 3, 3, 5, 5, 5, 5, 9}
```

### Output

```java
true
```

The array is increasing (flat runs are fine — `<=` allows equal neighbors).

## Coding exercise

Implement `isMonotonic(arr)`.

This is the bare pattern from [Feature #4: Popularity Analysis](04-feature-4-popularity-analysis.md) — track two flags in one pass, no story attached.

## Solution

```java
class Solution {
    public static boolean isMonotonic(int[] arr) {
        boolean increasing = true;
        boolean decreasing = true;

        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i] > arr[i + 1]) {
                increasing = false;
            }
            if (arr[i] < arr[i + 1]) {
                decreasing = false;
            }
        }

        return increasing || decreasing;
    }

    public static void main(String[] args) {
        System.out.println(isMonotonic(new int[]{1, 2, 3, 3, 3, 3, 3, 3, 5, 5, 5, 5, 9})); // true
        System.out.println(isMonotonic(new int[]{6, 5, 4, 4})); // true
        System.out.println(isMonotonic(new int[]{1, 3, 2}));    // false
    }
}
```

## Complexity measures

Let **n** be the array's length.

- **Time:** `O(n)` — single pass.
- **Space:** `O(1)` — two boolean flags.
