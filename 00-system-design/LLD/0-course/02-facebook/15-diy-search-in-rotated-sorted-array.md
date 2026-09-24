# DIY: Search in Rotated Sorted Array

## Problem statement

Search for a target in a sorted array of unique elements that has been rotated at an unknown pivot (e.g. `{0,1,2,4,5,6,7}` might become `{4,5,6,7,0,1,2}`). Return the index if found, else `-1`.

### Input

```java
arr = {4,5,6,7,0,1,2}
key = 2
```

### Output

```java
6
```

## Coding exercise

Implement `searchRotated(arr, key)`.

Exact same algorithm as [Feature #3: Find Story ID](03-feature-3-find-story-id.md) — modified binary search that identifies which half is sorted at each step.

## Solution

```java
class Solution {
    static int searchRotated(int[] arr, int key) {
        return search(arr, 0, arr.length - 1, key);
    }

    private static int search(int[] arr, int start, int end, int key) {
        if (start > end) {
            return -1;
        }

        int mid = start + (end - start) / 2;

        if (arr[mid] == key) {
            return mid;
        }

        if (arr[start] <= arr[mid]) {
            if (arr[start] <= key && key < arr[mid]) {
                return search(arr, start, mid - 1, key);
            }
            return search(arr, mid + 1, end, key);
        } else {
            if (arr[mid] < key && key <= arr[end]) {
                return search(arr, mid + 1, end, key);
            }
            return search(arr, start, mid - 1, key);
        }
    }

    public static void main(String[] args) {
        int[] arr = {4, 5, 6, 7, 0, 1, 2};
        System.out.println(searchRotated(arr, 2)); // 6
        System.out.println(searchRotated(arr, 3)); // -1
    }
}
```

## Complexity measures

Let **n** be the array's length.

- **Time:** `O(log n)`.
- **Space:** `O(1)` (or `O(log n)` counting the recursion stack).
