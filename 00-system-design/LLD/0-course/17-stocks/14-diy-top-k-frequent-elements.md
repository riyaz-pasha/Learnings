# DIY: Top K Frequent Elements

## Problem statement

You are provided with an array of integers. Return the `k` most frequent elements.

### Input

```java
nums = {1, 3, 5, 14, 18, 14, 5}
k = 2
```

### Output

```java
{5, 14}
```

## Coding exercise

Implement `topKFrequent(nums, k)`.

The closest match in this chapter is [Feature #5: Top Brokers](05-feature-5-top-brokers.md) — this is the same problem stripped of its "brokers get promoted" framing: build a frequency map, then keep the top `k` counts using a size-bounded min-heap.

## Solution

```java
import java.util.*;

class Solution {
    // Returns the k most frequent values in nums.
    public static int[] topKFrequent(int[] nums, int k) {
        Map<Integer, Integer> freq = new HashMap<>();
        for (int num : nums) {
            freq.merge(num, 1, Integer::sum);
        }

        PriorityQueue<int[]> minHeap = new PriorityQueue<>((a, b) -> a[1] - b[1]);
        for (Map.Entry<Integer, Integer> entry : freq.entrySet()) {
            minHeap.offer(new int[]{entry.getKey(), entry.getValue()});
            if (minHeap.size() > k) {
                minHeap.poll();
            }
        }

        int[] result = new int[k];
        for (int i = k - 1; i >= 0; i--) {
            result[i] = minHeap.poll()[0];
        }
        return result;
    }

    public static void main(String[] args) {
        int[] nums = {1, 3, 5, 14, 18, 14, 5};
        System.out.println(Arrays.toString(topKFrequent(nums, 2)));
        // [5, 14]
    }
}
```

`5` and `14` both occur twice — the most of any value in the array — so they're the two survivors once the min-heap has been trimmed down to size `k = 2`.

## Complexity measures

Let **n** be the number of elements in `nums` and **k** be the number of frequent elements requested.

### Time Complexity

`O(n log k)` — building the frequency map takes `O(n)`; each of the (at most `n`) heap insertions costs `O(log k)` since the heap never grows past size `k`.

### Space Complexity

`O(n)` — the frequency map can hold up to `n` distinct values, even though the heap itself only ever holds `k`.
