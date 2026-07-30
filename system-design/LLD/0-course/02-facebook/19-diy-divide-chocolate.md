# DIY: Divide Chocolate

## Problem statement

A chocolate bar has chunks with sweetness values in `sweetness`. You have `K` friends, so you make `K` cuts to get `K + 1` consecutive pieces. You (the host) eat the piece with the **minimum** total sweetness and give the rest away. Find the maximum possible value of that minimum piece, choosing the cuts optimally.

### Input

```java
sweetness = {1, 2, 3, 4, 5}
K = 3
```

### Output

```java
3
```

Split into `{1,2}, {3}, {4}, {5}` — the host gets either `{1,2}` (sum 3) or `{3}` (sum 3).

## Coding exercise

Implement `maximizeSweetness(sweetness, K)`.

This is exactly [Feature #7: Divide Posts](07-feature-7-divide-posts.md) — maximize the minimum contiguous piece sum, splitting into `K + 1` pieces instead of `k` (the "host" is the master node from that feature).

## Solution

```java
import java.util.Arrays;

class Solution {

    public static int maximizeSweetness(int[] sweetness, int k) {
        int pieces = k + 1;
        int low = 1;
        int high = Arrays.stream(sweetness).sum() / pieces;

        while (low < high) {
            int mid = (low + high + 1) / 2;

            int target = 0;
            int divisions = 0;
            for (int sw : sweetness) {
                target += sw;
                if (target >= mid) {
                    divisions++;
                    target = 0;
                }
            }

            if (divisions >= pieces) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }

        return low;
    }

    public static void main(String[] args) {
        int[] sweetness = {1, 2, 3, 4, 5};
        System.out.println(maximizeSweetness(sweetness, 3)); // 3
    }
}
```

## Complexity measures

Let **n** be the number of chunks and **m** be the total sweetness.

- **Time:** `O(n × log m)`.
- **Space:** `O(1)`.
