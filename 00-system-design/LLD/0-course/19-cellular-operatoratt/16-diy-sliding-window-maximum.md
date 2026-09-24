# DIY: Sliding Window Maximum

## Problem statement

You are given an array of integers. A sub-array (window) of fixed size `k` slides from the extreme left to the extreme right of the array, shifting one position to the right at a time. Find the list of maximum values in each window.

### Input

```java
// array = [12,3,9,15,11,8,2,21,16,5]
// size = 10
// k = 5
```

### Output

```java
// [15, 15, 15, 21, 21, 21]
```

## Coding exercise

Implement the `winSlideMax(array, size, k)` function, where `array` is an integer array, `size` is the array's length, and `k` is the window size. The function returns an array containing the maximum value of each window.

This is exactly [Feature #6: Maximum Users](06-feature-6-maximum-users.md) — the same monotonic-deque sliding window maximum, just renamed away from the "users connected to a base station" framing to a plain integer array.

## Solution

```java
import java.util.*;

class Solution {
    public static int[] winSlideMax(int[] array, int size, int k) {
        Deque<Integer> mqueue = new ArrayDeque<>();
        int[] result = new int[size - k + 1];

        for (int i = 0; i < size; i++) {
            while (!mqueue.isEmpty() && array[mqueue.peekLast()] < array[i]) {
                mqueue.pollLast();
            }
            mqueue.addLast(i);

            if (mqueue.peekFirst() == i - k) {
                mqueue.pollFirst();
            }

            if (i >= k - 1) {
                result[i - k + 1] = array[mqueue.peekFirst()];
            }
        }
        return result;
    }

    public static void main(String[] args) {
        int[] array = {12, 3, 9, 15, 11, 8, 2, 21, 16, 5};
        System.out.println(Arrays.toString(winSlideMax(array, 10, 5)));
        // [15, 15, 15, 21, 21, 21]
    }
}
```

## Solution walkthrough

With `size = 10` and `k = 5`, there are `10 - 5 + 1 = 6` windows: `[12,3,9,15,11]`, `[3,9,15,11,8]`, `[9,15,11,8,2]`, `[15,11,8,2,21]`, `[11,8,2,21,16]`, `[8,2,21,16,5]`, with maxima `15, 15, 15, 21, 21, 21`. (The source material's own stated output for this input is the 5-entry list `[15,15,21,21,21]`, but that's short exactly one window — a live run of the algorithm above produces the 6-entry `[15, 15, 15, 21, 21, 21]`, matching the window-by-window trace above.) The monotonic deque keeps only indices that could still become a future window's maximum, discarding any index whose value is beaten by a later, still-in-window value.

## Complexity measures

Let **n** be the array size and **k** be the window size.

### Time Complexity

`O(n)` — each index enters and leaves the deque at most once across the entire scan.

### Space Complexity

`O(k)` — the deque holds at most `k` indices at any point.
