# DIY: Kth Largest Element in an Array

## Problem statement

Find the **k**th largest element in an unsorted array.

Note: this means the kth largest in sorted order, not the kth *distinct* value.

### Input

```java
arr = {3, 2, 1, 5, 6, 4}
k = 2
```

### Output

```java
5
```

(Sorted descending: `[6, 5, 4, 3, 2, 1]` — the 2nd largest is `5`.)

## Coding exercise

Implement `findKthLargest(arr, k)`, returning the kth largest value.

This is the exact same pattern as [Feature #7: Highest Rank](07-feature-7-highest-rank.md) — there, Uber found the kth highest driver rank to keep ride allocation fair; here it's the bare pattern with no story attached. Keep a min-heap of size k, evicting the smallest whenever a bigger value shows up; the root ends up being the kth largest.

## Solution

```java
import java.util.*;

class Solution {
    public static int findKthLargest(int[] arr, int k) {
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();

        for (int i = 0; i < k; i++) {
            minHeap.offer(arr[i]);
        }

        for (int i = k; i < arr.length; i++) {
            if (arr[i] > minHeap.peek()) {
                minHeap.poll();
                minHeap.offer(arr[i]);
            }
        }

        return minHeap.peek();
    }

    public static void main(String[] args) {
        int[] arr = {3, 2, 1, 5, 6, 4};
        System.out.println(findKthLargest(arr, 2));
        // 5
    }
}
```

## Complexity measures

Let **n** be the size of the array and **k** the number kept in the heap.

- **Time:** `O(n × log k)` — each element does an `O(1)` comparison, with an `O(log k)` heap update when it qualifies.
- **Space:** `O(k)` — the heap never holds more than k elements.
