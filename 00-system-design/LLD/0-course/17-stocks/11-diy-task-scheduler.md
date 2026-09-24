# DIY: Task Scheduler

## Problem statement

You are given a character array of tasks, where each character represents a distinct type of task the CPU needs to run. After the CPU executes a task, it must wait a given number of intervals (a "cooldown") before it can run the same task type again. Executing any task takes exactly one time interval. Calculate the minimum total time needed to complete all the tasks (idling if necessary).

### Input

```java
tasks = ['X', 'Y', 'Y', 'Y', 'Z', 'X']
T = 2
```

### Output

```java
7
```

## Coding exercise

Implement `leastInterval(tasks, T)`.

The closest match in this chapter is [Feature #2: Settling Period](02-feature-2-settling-period.md) — this is the exact same problem, just with "settling period" renamed to "cooldown" and "stock letters" renamed to "task types." The solution is identical: find the busiest task, count the unavoidable idle slots around its repetitions, and see how much of that idle time the other tasks can absorb.

## Solution

```java
import java.util.*;

class Solution {
    // Returns the minimum number of intervals needed to run every task in
    // `tasks`, given that the same task type must be separated by at least
    // T intervening intervals (idle or otherwise).
    public static int leastInterval(char[] tasks, int T) {
        int[] freq = new int[26];
        for (char t : tasks) {
            freq[t - 'A']++;
        }
        Arrays.sort(freq);

        int fMax = freq[25];
        int idleIntervals = (fMax - 1) * T;

        for (int i = 24; i >= 0 && freq[i] > 0; i--) {
            idleIntervals -= Math.min(fMax - 1, freq[i]);
        }

        return idleIntervals > 0 ? idleIntervals + tasks.length : tasks.length;
    }

    public static void main(String[] args) {
        char[] tasks = {'X', 'Y', 'Y', 'Y', 'Z', 'X'};
        System.out.println(leastInterval(tasks, 2));
        // 7
    }
}
```

Tracing the example: `Y` is busiest at 3 occurrences, so `fMax = 3`, giving `(3-1) * 2 = 4` idle slots to fill around the two gaps between `Y`'s three runs. `X` (frequency 2) and `Z` (frequency 1) can absorb up to `min(fMax - 1, freq) = min(2, 2) = 2` and `min(2, 1) = 1` of those slots respectively, leaving `4 - 2 - 1 = 1` unavoidable idle interval. Total time is `1 (idle) + 6 (tasks) = 7`, which matches — one valid schedule is `Y X Z Y X idle Y`.

## Complexity measures

Let **n** be the number of tasks.

### Time Complexity

`O(n)` — building the 26-entry frequency array takes `O(n)`; sorting that fixed-size array is `O(1)`.

### Space Complexity

`O(1)` — the frequency array always has exactly 26 slots, regardless of `n`.
