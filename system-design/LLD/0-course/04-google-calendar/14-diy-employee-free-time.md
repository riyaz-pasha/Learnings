# DIY: Employee Free Time

## Problem statement

Given the schedules of multiple employees (each employee's own schedule is sorted and non-overlapping), find the intervals of time when **everyone** is free. Ignore the time before the first meeting and after the last.

**Constraints:** `1 <= schedule.length, schedule[i].length <= 50`, `0 <= start < end <= 10^8`.

### Input

```java
schedule = [[[2,3], [7,9]], [[1,4], [6,7]]]
```

### Output

```java
[[4, 6]]
```

## Coding exercise

Implement `employeeFreeTime(schedule)`.

Builds directly on [Feature #2: Show Busy Schedule](02-feature-2-show-busy-schedule.md): flatten every employee's meetings into one list, merge overlapping intervals exactly as before, and then the **gaps between consecutive merged intervals** are exactly the times when nobody is busy.

## Solution

```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Solution {

    public static List<int[]> employeeFreeTime(List<List<int[]>> schedule) {
        List<int[]> allMeetings = new ArrayList<>();
        for (List<int[]> employee : schedule) {
            allMeetings.addAll(employee);
        }
        allMeetings.sort((a, b) -> a[0] - b[0]);

        List<int[]> merged = new ArrayList<>();
        merged.add(allMeetings.get(0));

        for (int i = 1; i < allMeetings.size(); i++) {
            int[] last = merged.get(merged.size() - 1);
            int[] current = allMeetings.get(i);

            if (current[0] <= last[1]) {
                last[1] = Math.max(last[1], current[1]);
            } else {
                merged.add(current);
            }
        }

        List<int[]> freeTime = new ArrayList<>();
        for (int i = 1; i < merged.size(); i++) {
            int gapStart = merged.get(i - 1)[1];
            int gapEnd = merged.get(i)[0];
            if (gapStart < gapEnd) {
                freeTime.add(new int[]{gapStart, gapEnd});
            }
        }

        return freeTime;
    }

    public static void main(String[] args) {
        List<List<int[]>> schedule = List.of(
                List.of(new int[]{2, 3}, new int[]{7, 9}),
                List.of(new int[]{1, 4}, new int[]{6, 7})
        );

        for (int[] free : employeeFreeTime(schedule)) {
            System.out.println(Arrays.toString(free));
        }
        // [4, 6]
    }
}
```

## Complexity measures

Let **n** be the total number of meetings across all employees.

- **Time:** `O(n log n)` — dominated by sorting the flattened list.
- **Space:** `O(n)`.
