# DIY: Find Interval Sets

## Problem statement

Given a list of intervals, organize them into sets where only one interval can occupy a set at a time. Find the minimum number of sets needed.

### Input

```java
{{1,4}, {2,5}, {4,8}, {5,6}, {5,8}, {6,7}}
```

### Output

```java
3
```

## Coding exercise

Implement `findSets(intervals)`.

Exactly [Feature #1: Find Meeting Rooms](01-feature-1-find-meeting-rooms.md) — "sets" are "rooms," minimum number needed is tracked with a min heap of end times.

## Solution

```java
import java.util.Arrays;
import java.util.PriorityQueue;

class Solution {
    public static int findSets(int[][] intervals) {
        if (intervals.length == 0) {
            return 0;
        }

        Arrays.sort(intervals, (a, b) -> a[0] - b[0]);

        PriorityQueue<Integer> endTimes = new PriorityQueue<>();
        endTimes.offer(intervals[0][1]);

        for (int i = 1; i < intervals.length; i++) {
            if (endTimes.peek() <= intervals[i][0]) {
                endTimes.poll();
            }
            endTimes.offer(intervals[i][1]);
        }

        return endTimes.size();
    }

    public static void main(String[] args) {
        int[][] intervals = {{1, 4}, {2, 5}, {4, 8}, {5, 6}, {5, 8}, {6, 7}};
        System.out.println(findSets(intervals)); // 3
    }
}
```

## Complexity measures

Let **n** be the number of intervals.

- **Time:** `O(n log n)`.
- **Space:** `O(n)`.
