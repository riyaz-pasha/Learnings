# DIY: Find K Closest Elements

## Problem statement

Given a sorted integer array `arr`, and two integers `k` and `x`, return the `k` closest integers to `x` in this array. You must ensure the result is sorted in ascending order.

An integer `a` is closer to `x` than `b` if `|a - x| < |b - x|`, or if `|a - x| == |b - x|` and `a < b`.

### Input

```java
// Sample Input 1
arr = [1, 2, 3, 4, 5], k = 4, x = 3

// Sample Input 2
arr = [1, 2, 3, 4, 5], k = 4, x = -1

// Sample Input 3
arr = [-29, -11, -3, 0, 5, 10, 50, 63, 198], k = 6, x = 8
```

### Output

```java
// Sample Output 1
[1, 2, 3, 4]

// Sample Output 2
[1, 2, 3, 4]

// Sample Output 3
[-29, -11, -3, 0, 5, 10]
```

## Coding exercise

Implement `findClosestElements(arr, k, x)`.

This is the exact same pattern as [Feature #5: Eligible Candidates](05-feature-5-eligible-candidates.md) — there, a cluster needed the `k` machine IDs closest to a randomly drawn number to shortlist candidates before a leader election; here it's the bare "k closest elements in a sorted array" problem with no story attached. Binary search locates the split point closest to `x`, then two pointers expand a window outward from that split, always pulling in whichever side is closer, until the window holds exactly `k` elements.

## Solution

```java
import java.util.*;

class Solution {
    public static List<Integer> findClosestElements(int[] arr, int k, int x) {
        int n = arr.length;
        if (k == n) {
            List<Integer> all = new ArrayList<>();
            for (int v : arr) all.add(v);
            return all;
        }

        int lo = 0, hi = n - 1;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (arr[mid] < x) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }

        int left = lo - 1;
        int right = lo;

        while (right - left - 1 < k) {
            if (left < 0) {
                right++;
            } else if (right >= n) {
                left--;
            } else if (x - arr[left] <= arr[right] - x) {
                left--;
            } else {
                right++;
            }
        }

        List<Integer> result = new ArrayList<>();
        for (int i = left + 1; i < right; i++) {
            result.add(arr[i]);
        }
        return result;
    }

    public static void main(String[] args) {
        System.out.println(findClosestElements(new int[]{1, 2, 3, 4, 5}, 4, 3));
        // [1, 2, 3, 4]
        System.out.println(findClosestElements(new int[]{1, 2, 3, 4, 5}, 4, -1));
        // [1, 2, 3, 4]
        System.out.println(findClosestElements(
            new int[]{-29, -11, -3, 0, 5, 10, 50, 63, 198}, 6, 8));
        // [-29, -11, -3, 0, 5, 10]
    }
}
```

Binary search finds the leftmost index whose value is `>= x`, splitting the array into "candidates approaching from below" and "candidates approaching from above." From there, `left` and `right` grow the window outward, always pulling in whichever neighboring candidate is closer to `x` (ties favor the smaller value, which is always the left candidate). Once the window holds `k` elements, the values strictly between `left` and `right` are the answer — already ascending, since `arr` was sorted to begin with.

## Complexity measures

Let **n** be the length of `arr`.

- **Time:** `O(log n + k)` — the binary search costs `O(log n)`, and growing the window to size `k` costs `O(k)`.
- **Space:** `O(1)` beyond the output list — only a constant number of index variables are used.
