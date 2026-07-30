# DIY: Interval Lists Intersection

## Problem statement

Given two lists of disjoint intervals, find their intersection.

### Input

```java
A = {{1,3}, {5,6}, {7,9}}
B = {{2,4}, {6,7}, {8,10}}
```

### Output

```java
{{2,3}, {8,9}}
```

Note that `{5,6}` and `{6,7}` only *touch* at the single point `6` — that degenerate zero-length overlap doesn't count here, and neither does the touch at `7` between `{7,9}` and `{6,7}`.

## Coding exercise

Implement `intervalsIntersection(intervalsA, intervalsB)`.

Same two-pointer sweep as [Feature #5: Find Common Meeting Times](05-feature-5-find-common-meeting-times.md), with one small but important difference: the overlap test here is **strict** (`start < end`, not `start <= end`), so touching-at-a-point intervals are correctly excluded rather than reported as a zero-length intersection.

## Solution

```java
import java.util.ArrayList;
import java.util.List;

class Solution {
    public static int[][] intervalsIntersection(int[][] intervalsA, int[][] intervalsB) {
        List<int[]> result = new ArrayList<>();
        int i = 0, j = 0;

        while (i < intervalsA.length && j < intervalsB.length) {
            int start = Math.max(intervalsA[i][0], intervalsB[j][0]);
            int end = Math.min(intervalsA[i][1], intervalsB[j][1]);

            if (start < end) {
                result.add(new int[]{start, end});
            }

            if (intervalsA[i][1] < intervalsB[j][1]) {
                i++;
            } else {
                j++;
            }
        }

        return result.toArray(new int[0][]);
    }

    public static void main(String[] args) {
        int[][] a = {{1, 3}, {5, 6}, {7, 9}};
        int[][] b = {{2, 4}, {6, 7}, {8, 10}};

        for (int[] interval : intervalsIntersection(a, b)) {
            System.out.println("[" + interval[0] + ", " + interval[1] + "]");
        }
        // [2, 3]
        // [8, 9]
    }
}
```

## Complexity measures

Let **n** and **m** be the sizes of the two interval lists.

- **Time:** `O(n + m)`.
- **Space:** `O(min(n, m))` for the output.
