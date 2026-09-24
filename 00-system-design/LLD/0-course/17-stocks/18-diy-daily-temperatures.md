# DIY: Daily Temperatures

## Problem statement

Given an array of daily temperatures, return an array `answer` such that `answer[i]` is the number of days you'd have to wait after day `i` to get a warmer temperature. If there's no future day for which this is possible, `answer[i] == 0` instead.

### Input

```java
temperatures = {73, 74, 75, 71, 69, 72, 76, 73}
```

### Output

```java
{1, 1, 4, 2, 1, 1, 0, 0}
```

## Coding exercise

Implement `dailyTemperatures(temperatures)`.

The closest match in this chapter is [Feature #8: Find Intervals](08-feature-8-find-intervals.md) — this exercise *is* Find Intervals, without the stock-price-prediction framing. Same monotonic decreasing stack of indices, same logic: pop and resolve every index whose price/temperature the current one beats, then push the current index.

## Solution

```java
import java.util.*;

class Solution {
    // For each day, returns how many days must pass before a warmer
    // temperature appears (0 if it never does).
    public static int[] dailyTemperatures(int[] temperatures) {
        int[] answer = new int[temperatures.length];
        Deque<Integer> stack = new ArrayDeque<>(); // Indices, decreasing temperatures.

        for (int today = 0; today < temperatures.length; today++) {
            while (!stack.isEmpty() && temperatures[today] > temperatures[stack.peek()]) {
                int coolerDay = stack.pop();
                answer[coolerDay] = today - coolerDay;
            }
            stack.push(today);
        }
        return answer;
    }

    public static void main(String[] args) {
        int[] temperatures = {73, 74, 75, 71, 69, 72, 76, 73};
        System.out.println(Arrays.toString(dailyTemperatures(temperatures)));
        // [1, 1, 4, 2, 1, 1, 0, 0]
    }
}
```

Days `6` and `7` (temperatures `76` and `73`) never see anything warmer for the rest of the array, so they're left at `answer[i] == 0` — they're pushed onto the stack but never popped.

## Complexity measures

Let **n** be the number of days.

### Time Complexity

`O(n)` — every index is pushed once and popped at most once.

### Space Complexity

`O(n)` — worst case (strictly decreasing temperatures), the stack holds all `n` indices at once.
