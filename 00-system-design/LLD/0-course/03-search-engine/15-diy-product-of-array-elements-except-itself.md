# DIY: Product of Array Elements Except Itself

## Problem statement

Given an array, return an array where each index holds the product of all the other elements (not itself).

### Input

```java
arr = {1, 2, 3, 4}
```

### Output

```java
{24, 12, 8, 6}
```

## Coding exercise

Implement `findProduct(arr)`.

This is exactly [Feature #5: Calculate the Search Ranking Factor](05-feature-5-calculate-the-search-ranking-factor.md) — left-product / right-product sweep, generalized to any array.

## Solution

```java
class Solution {
    public static int[] findProduct(int[] arr) {
        int length = arr.length;
        int[] result = new int[length];

        result[0] = 1;
        for (int i = 1; i < length; i++) {
            result[i] = arr[i - 1] * result[i - 1];
        }

        int right = 1;
        for (int i = length - 1; i >= 0; i--) {
            result[i] *= right;
            right *= arr[i];
        }

        return result;
    }

    public static void main(String[] args) {
        int[] arr = {1, 2, 3, 4};
        System.out.println(java.util.Arrays.toString(findProduct(arr))); // [24, 12, 8, 6]
    }
}
```

## Complexity measures

Let **n** be the array's length.

- **Time:** `O(n)`.
- **Space:** `O(n)` for the output array (`O(1)` extra).
