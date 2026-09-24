# DIY: K Closest Points to Origin

## Problem statement

You have a list of points on a 2D plane. Find the **k** points closest to the origin `(0, 0)`, using Euclidean distance.

### Input

```java
points = {{1, 3}, {-2, 2}}
k = 1
```

### Output

```java
{{-2, 2}}
```

(`(-2, 2)` has distance `sqrt(8) ≈ 2.83`, closer to the origin than `(1, 3)`'s distance of `sqrt(10) ≈ 3.16`.)

## Coding exercise

Implement `kClosest(points, k)`, returning the k points closest to the origin.

This is the exact same pattern as [Feature #1: Select Closest Drivers](01-feature-1-select-closest-drivers.md) — there, Uber found the k nearest drivers to a user; here it's the bare pattern with no story attached. Keep a max-heap of size k, keyed by squared distance, evicting the farthest point whenever a closer one shows up.

## Solution

```java
import java.util.*;

class Solution {
    public static int[][] kClosest(int[][] points, int k) {
        PriorityQueue<int[]> maxHeap = new PriorityQueue<>(
            (a, b) -> (b[0] * b[0] + b[1] * b[1]) - (a[0] * a[0] + a[1] * a[1])
        );

        for (int[] point : points) {
            maxHeap.offer(point);
            if (maxHeap.size() > k) {
                maxHeap.poll();
            }
        }

        return maxHeap.toArray(new int[0][]);
    }

    public static void main(String[] args) {
        int[][] points = {{1, 3}, {-2, 2}};
        int[][] result = kClosest(points, 1);
        for (int[] p : result) {
            System.out.println(Arrays.toString(p));
        }
        // [-2, 2]
    }
}
```

## Complexity measures

Let **n** be the number of points and **k** the number to return.

- **Time:** `O(n × log k)` — each point does an `O(1)` distance comparison, with an `O(log k)` heap update when it qualifies.
- **Space:** `O(k)` — the heap never holds more than k points.
