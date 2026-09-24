# DIY: Merge Intervals

## Problem statement

Given a list of intervals, merge every pair that overlaps or is adjacent (one starts exactly when another ends).

### Input

```java
{{1,4}, {2,5}, {6,7}, {7,10}, {11,12}}
```

### Output

```java
{{1,5}, {6,10}, {11,12}}
```

## Coding exercise

Implement `mergeIntervals(intervals)`.

Exactly [Feature #2: Show Busy Schedule](02-feature-2-show-busy-schedule.md) — sort by start, then sweep, merging into the last interval whenever the current one overlaps or touches it.

## Solution

```java
import java.util.ArrayList;
import java.util.Arrays;

class Solution {
    public static int[][] mergeIntervals(int[][] intervals) {
        if (intervals.length == 0) {
            return new int[0][0];
        }

        Arrays.sort(intervals, (a, b) -> a[0] - b[0]);

        ArrayList<int[]> merged = new ArrayList<>();
        merged.add(intervals[0]);

        for (int i = 1; i < intervals.length; i++) {
            int[] last = merged.get(merged.size() - 1);
            int[] current = intervals[i];

            if (current[0] <= last[1]) {
                last[1] = Math.max(last[1], current[1]);
            } else {
                merged.add(current);
            }
        }

        return merged.toArray(new int[0][]);
    }

    public static void main(String[] args) {
        int[][] intervals = {{1, 4}, {2, 5}, {6, 7}, {7, 10}, {11, 12}};
        int[][] result = mergeIntervals(intervals);
        for (int[] r : result) {
            System.out.println("[" + r[0] + ", " + r[1] + "]");
        }
        // [1, 5]
        // [6, 10]
        // [11, 12]
    }
}
```

## Complexity measures

Let **n** be the number of intervals.

- **Time:** `O(n log n)`.
- **Space:** `O(1)` extra (excluding output).
