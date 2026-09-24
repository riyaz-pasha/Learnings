# DIY: Moving Average from a Data Stream

## Problem statement

Given a stream of integers arriving one at a time, and a fixed window size, return the moving average over the trailing window each time a new value arrives. Before the window has filled up, average over however many values have arrived so far.

### Input

```java
// values = [1, 10, 3, 5]
// size = 3

// MovingAverage obj = new MovingAverage(size);
// average = obj.findMovingAverage(1);
// average = obj.findMovingAverage(10);
// average = obj.findMovingAverage(3);
// average = obj.findMovingAverage(5);
```

### Output

```java
// 1.0
// 5.5
// 4.666666666666667
// 6.0
```

## Coding exercise

Implement the `MovingAverage` class with a `findMovingAverage(val)` method.

This is exactly [Feature #6: Incoming Tweets Predictor](06-feature-6-incoming-tweets-predictor.md) — the same sliding-window-with-running-sum trick, with a plain integer stream instead of 5-minute traffic readings.

## Solution

```java
import java.util.*;

class Solution {
    static class MovingAverage {
        private final Deque<Integer> queue = new ArrayDeque<>();
        private final int size;
        private int windowSum = 0;

        MovingAverage(int size) {
            this.size = size;
        }

        // Feeds in the next value and returns the average over the trailing
        // window (or over everything seen so far, if the window isn't full yet).
        double findMovingAverage(int val) {
            queue.addLast(val);
            windowSum += val;
            if (queue.size() > size) {
                windowSum -= queue.pollFirst();
            }
            return (double) windowSum / queue.size();
        }
    }

    public static void main(String[] args) {
        MovingAverage obj = new MovingAverage(3);
        System.out.println(obj.findMovingAverage(1));  // 1.0
        System.out.println(obj.findMovingAverage(10)); // 5.5
        System.out.println(obj.findMovingAverage(3));  // 4.666666666666667
        System.out.println(obj.findMovingAverage(5));  // 6.0
    }
}
```

## Solution walkthrough

With `size = 3`: after `1` arrives, the window is `{1}`, average `1.0`. After `10`, window `{1,10}`, average `11/2 = 5.5`. After `3`, window `{1,10,3}` is now full, average `14/3 = 4.666666666666667`. After `5` arrives, the window is already full, so the oldest value (`1`) is evicted first, leaving `{10,3,5}`, average `18/3 = 6.0`. The deque plus running sum means every step does constant work — no re-summing the window from scratch.

## Complexity measures

Let **m** be the window size.

### Time Complexity

`O(1)` per call to `findMovingAverage` — one push, at most one pop, constant arithmetic.

### Space Complexity

`O(m)` — the deque holds at most `m` values at any time.
