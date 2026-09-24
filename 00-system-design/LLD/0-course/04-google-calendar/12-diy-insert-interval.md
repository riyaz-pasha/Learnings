# DIY: Insert Interval

## Problem statement

Given a list of non-overlapping intervals, insert a new interval and merge where necessary. Return the resulting mutually exclusive intervals.

### Input

```java
intervals = {{1,3}, {4,5}, {7,9}, {9,15}, {10,14}}
newInterval = [2, 8]
```

### Output

```java
{{1,9}, {9,15}, {10,14}}
```

## Coding exercise

Implement `insertInterval(intervals, newInterval)`.

Same three-phase scan as [Feature #4: Schedule a New Meeting](04-feature-4-schedule-a-new-meeting.md) — before / merge / after. Note the merge condition here uses a **strict** `<` rather than `<=`: intervals that merely *touch* the new interval's boundary (like `{9,15}` touching the merged interval's end at `9`) are left separate rather than being folded in, matching this problem's expected output.

## Solution

```java
import java.util.ArrayList;
import java.util.List;

class Solution {
    public static int[][] insertInterval(int[][] intervals, int[] newInterval) {
        List<int[]> output = new ArrayList<>();
        int i = 0;
        int n = intervals.length;
        int start = newInterval[0];
        int end = newInterval[1];

        while (i < n && intervals[i][1] < start) {
            output.add(intervals[i]);
            i++;
        }

        while (i < n && intervals[i][0] < end) {
            start = Math.min(start, intervals[i][0]);
            end = Math.max(end, intervals[i][1]);
            i++;
        }
        output.add(new int[]{start, end});

        while (i < n) {
            output.add(intervals[i]);
            i++;
        }

        return output.toArray(new int[0][]);
    }

    public static void main(String[] args) {
        int[][] intervals = {{1, 3}, {4, 5}, {7, 9}, {9, 15}, {10, 14}};
        int[][] result = insertInterval(intervals, new int[]{2, 8});
        for (int[] r : result) {
            System.out.println("[" + r[0] + ", " + r[1] + "]");
        }
        // [1, 9]
        // [9, 15]
        // [10, 14]
    }
}
```

## Complexity measures

Let **n** be the number of intervals.

- **Time:** `O(n)`.
- **Space:** `O(n)` for the output.
