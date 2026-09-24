# DIY: Find Median from a Data Stream

## Problem statement

Design a data structure that stores a dynamically growing stream of integers and gives efficient access to their median at any point.

## Coding exercise

Implement `insertNum(num)` (add a new number) and `findMedian()` (return the median of everything inserted so far).

This is the exact pattern from [Feature #3: Find Median Age](03-feature-3-find-median-age.md), minus the Netflix framing — a max heap for the smaller half, a min heap for the larger half, rebalanced after every insert.

## Solution

```java
import java.util.*;

class MedianOfAStream {
    private final PriorityQueue<Integer> smallList; // max heap: smaller half
    private final PriorityQueue<Integer> largeList; // min heap: larger half

    public MedianOfAStream() {
        smallList = new PriorityQueue<>(Collections.reverseOrder());
        largeList = new PriorityQueue<>();
    }

    public void insertNum(int num) {
        if (smallList.isEmpty() || num <= smallList.peek()) {
            smallList.offer(num);
        } else {
            largeList.offer(num);
        }

        if (smallList.size() > largeList.size() + 1) {
            largeList.offer(smallList.poll());
        } else if (largeList.size() > smallList.size() + 1) {
            smallList.offer(largeList.poll());
        }
    }

    public double findMedian() {
        if (smallList.size() == largeList.size()) {
            return (smallList.peek() + largeList.peek()) / 2.0;
        }
        return smallList.size() > largeList.size() ? smallList.peek() : largeList.peek();
    }

    public static void main(String[] args) {
        MedianOfAStream stream = new MedianOfAStream();
        int[] nums = {5, 15, 1, 3};

        for (int n : nums) {
            stream.insertNum(n);
            System.out.println("median after inserting " + n + ": " + stream.findMedian());
        }
        // median after inserting 5: 5.0
        // median after inserting 15: 10.0
        // median after inserting 1: 5.0
        // median after inserting 3: 4.0
    }
}
```

## Complexity measures

Let **n** be the number of values inserted so far.

- **`insertNum`:** `O(log n)` — heap insertion/rebalancing.
- **`findMedian`:** `O(1)` — just peek the top(s).
- **Space:** `O(n)` — every inserted value lives in one of the two heaps.
