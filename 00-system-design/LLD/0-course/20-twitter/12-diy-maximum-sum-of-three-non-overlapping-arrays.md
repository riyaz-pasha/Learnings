# DIY: Maximum Sum of Three Non-Overlapping Arrays

## Problem statement

You're given an array of integers. Find three non-overlapping subarrays, each of size `k`, whose combined sum (across all `3 * k` entries) is as large as possible. Return the starting index of each of the three subarrays.

If more than one answer ties for the maximum sum, return the lexicographically smallest triple of starting indices.

### Input

```java
// numbers = {0, 2, 1, 3, 1, 7, 11, 5, 5}
// k = 2
```

### Output

```java
// {2, 5, 7}
```

## Coding exercise

Implement `threeSubarrayMaxSum(numbers, k)`.

This is exactly [Feature #3: Identify Peak Interaction Times](03-feature-3-identify-peak-interaction-times.md) — the same problem with generic integers instead of hourly interaction counts, and `k` in place of `hours`.

## Solution

```java
import java.util.Arrays;

class Solution {
    public static int[] threeSubarrayMaxSum(int[] numbers, int k) {
        int len = numbers.length;
        int sumsLen = len - k + 1;
        int[] sums = new int[sumsLen];

        int windowSum = 0;
        for (int i = 0; i < len; i++) {
            windowSum += numbers[i];
            if (i >= k) {
                windowSum -= numbers[i - k];
            }
            if (i >= k - 1) {
                sums[i - k + 1] = windowSum;
            }
        }

        // left[i]: index of the largest sums value in sums[0..i], earliest wins ties.
        int[] left = new int[sumsLen];
        int best = 0;
        for (int i = 0; i < sumsLen; i++) {
            if (sums[i] > sums[best]) {
                best = i;
            }
            left[i] = best;
        }

        // right[i]: index of the largest sums value in sums[i..end], earliest wins ties.
        int[] right = new int[sumsLen];
        best = sumsLen - 1;
        for (int i = sumsLen - 1; i >= 0; i--) {
            if (sums[i] >= sums[best]) {
                best = i;
            }
            right[i] = best;
        }

        int[] result = {-1, -1, -1};
        int maxTotal = -1;
        for (int b = k; b + k < sumsLen; b++) {
            int a = left[b - k];
            int c = right[b + k];
            int total = sums[a] + sums[b] + sums[c];
            if (total > maxTotal) {
                maxTotal = total;
                result = new int[]{a, b, c};
            }
        }
        return result;
    }

    public static void main(String[] args) {
        int[] numbers = {0, 2, 1, 3, 1, 7, 11, 5, 5};
        System.out.println(Arrays.toString(threeSubarrayMaxSum(numbers, 2))); // [2, 5, 7]
    }
}
```

## Solution walkthrough

First collapse the array into `sums[i]` = the sum of the length-`k` window starting at `i`, computed with a single sliding-window pass. Then fix a middle window `b` and independently find its best non-overlapping partner on the left (`left[b-k]`, the index of the largest `sums` value at or before `b-k`) and on the right (`right[b+k]`, the largest at or after `b+k`), sweeping `b` across every position that leaves room on both sides. Both `left` and `right` are built once in linear time, so checking every candidate `b` only costs constant time each, keeping the whole search linear. With `numbers = {0,2,1,3,1,7,11,5,5}` and `k=2`, the window sums are `{2,3,4,4,8,18,16,10}` (indices 0-7), and the best non-overlapping trio turns out to start at indices 2, 5, and 7 — the windows `{1,3}`, `{7,11}`, and `{5,5}`.

## Complexity measures

Let **n** be the length of `numbers`.

### Time Complexity

`O(n)` — computing `sums`, `left`, `right`, and sweeping candidate middle windows are each a single linear pass.

### Space Complexity

`O(n)` — the `sums`, `left`, and `right` arrays are each proportional in size to the input.
