# DIY: Exclusive Time of Functions

## Problem statement

Given `n` functions and a list of logs formatted as `"{function_id}:{start|end}:{timestamp}"`, compute each function's **exclusive time** — the total time it spent executing, not counting time spent inside nested/recursive calls. Functions can be called multiple times, including recursively.

### Input

```java
n = 2
logs = {"0:start:0", "1:start:3", "1:end:6", "0:end:10"}
```

### Output

```java
{7, 4}
```

Function 0 ran ticks 0-2 and ticks 7-10 (7 ticks total); function 1 ran ticks 3-6 (4 ticks).

## Coding exercise

Implement `exclusiveTime(n, logs)`.

Exactly [Feature #7: Find Searching Time](07-feature-7-find-searching-time.md) — a stack tracks the currently-running function, and a `prevTime` marker lets each start/end event credit the right amount of time to whichever function was running.

## Solution

```java
import java.util.*;

class Solution {

    public static int[] exclusiveTime(int n, String[] logs) {
        int[] result = new int[n];
        Deque<Integer> stack = new ArrayDeque<>();
        int prevTime = 0;

        for (String log : logs) {
            String[] parts = log.split(":");
            int id = Integer.parseInt(parts[0]);
            String type = parts[1];
            int time = Integer.parseInt(parts[2]);

            if (type.equals("start")) {
                if (!stack.isEmpty()) {
                    result[stack.peek()] += time - prevTime;
                }
                stack.push(id);
                prevTime = time;
            } else {
                result[stack.pop()] += time - prevTime + 1;
                prevTime = time + 1;
            }
        }

        return result;
    }

    public static void main(String[] args) {
        String[] logs = {"0:start:0", "1:start:3", "1:end:6", "0:end:10"};
        System.out.println(Arrays.toString(exclusiveTime(2, logs))); // [7, 4]
    }
}
```

## Complexity measures

Let **m** be the number of log entries.

- **Time:** `O(m)`.
- **Space:** `O(n)` for the output, plus `O(m)` worst-case stack depth.
