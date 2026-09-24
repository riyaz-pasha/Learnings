# DIY: Random Pick with Weight

## Problem statement

You're given an array of positive integers `w`, where `w[i]` is the weight of index `i`. Implement `pickIndex()`, which randomly returns an index from `w`, where the probability of returning index `i` is proportional to `w[i]`.

For example, if `w = [10, 90]`, index `0` should be returned with probability `10 / (10 + 90) = 10%`, and index `1` with `90%` probability. There's no guarantee the heaviest index is picked every time — just that it's picked *more often*.

### Input

```java
w = {1, 2, 3}
```

### Output

An index in `{0, 1, 2}`, returned with probability proportional to its weight — index `2` (weight 3) is the most likely single result, but not the only possible one.

## Coding exercise

Implement the `pickIndex()` method, where the weights array is passed to the constructor.

This is the exact same pattern as [Feature #5: Uber Pool](05-feature-5-uber-pool.md) — there, Uber picked a route for a driver weighted by likelihood metrics; here it's the bare pattern with no story attached. Precompute cumulative sums in the constructor, then binary-search for a uniformly random draw within the total.

## Solution

```java
import java.util.*;

class Solution {
    private int[] cumSums;
    private int total;
    private Random random = new Random();

    public Solution(int[] w) {
        cumSums = new int[w.length];
        int sum = 0;
        for (int i = 0; i < w.length; i++) {
            sum += w[i];
            cumSums[i] = sum;
        }
        total = sum;
    }

    public int pickIndex() {
        double target = random.nextDouble() * total;

        int lo = 0, hi = cumSums.length - 1;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (cumSums[mid] <= target) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    public static void main(String[] args) {
        Solution picker = new Solution(new int[]{1, 2, 3});

        int[] counts = new int[3];
        for (int i = 0; i < 60000; i++) {
            counts[picker.pickIndex()]++;
        }
        System.out.println(Arrays.toString(counts));
        // roughly [10000, 20000, 30000] -- a 1:2:3 ratio, matching the weights
    }
}
```

## Complexity measures

Let **n** be the size of the weights array.

- **Time:** Constructor `O(n)`; `pickIndex()` is `O(log n)` per call, thanks to the binary search.
- **Space:** Constructor `O(n)` for the cumulative-sums array; `pickIndex()` is `O(1)` extra space per call.
