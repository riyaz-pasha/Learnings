# DIY: Sliding Window Median

## Problem statement

Given an integer array `nums` and an integer `k`, a sliding window of size `k` moves from the left of the array to the right, one step at a time. Return the median of each window.

The median is the middle value of a sorted list; for an even-sized list, it's the average of the two middle values (e.g. `[1,2,3]` → median `2`; `[1,2,3,4]` → median `2.5`). Answers within `1e-5` of the actual value are accepted.

**Constraints:** `1 <= k <= nums.length <= 10^5`, `-2^31 <= nums[i] <= 2^31 - 1`.

### Input

```java
nums = {1, 3, -1, -3, 5, 3, 6, 7}, k = 3
```

### Output

```java
{1.0, -1.0, -1.0, 3.0, 5.0, 6.0}
```

## Coding exercise

Implement `medianSlidingWindow(nums, k)`.

This is exactly [Feature #10: Calculate Median of Buffering Events](10-feature-10-calculate-median-of-buffering-events.md) without the Netflix story — two heaps for the running median, plus lazy deletion (via a hash map of invalidated values) so outgoing elements don't need to be found and removed from the middle of a heap.

## Solution

```java
import java.util.*;

class Solution {
    public static double[] medianSlidingWindow(int[] nums, int k) {
        List<Double> medians = new ArrayList<>();
        HashMap<Integer, Integer> invalid = new HashMap<>();
        PriorityQueue<Integer> smallList = new PriorityQueue<>(Collections.reverseOrder()); // max heap
        PriorityQueue<Integer> largeList = new PriorityQueue<>(); // min heap

        for (int i = 0; i < k; i++) {
            smallList.offer(nums[i]);
        }
        for (int i = 0; i < k / 2; i++) {
            largeList.offer(smallList.poll());
        }

        for (int i = k; ; i++) {
            medians.add(k % 2 == 1
                    ? (double) smallList.peek()
                    : ((double) smallList.peek() + largeList.peek()) / 2.0);

            if (i >= nums.length) {
                break;
            }

            int outNum = nums[i - k];
            int inNum = nums[i];
            int balance = 0;

            balance += (outNum <= smallList.peek()) ? -1 : 1;
            invalid.merge(outNum, 1, Integer::sum);

            if (inNum <= smallList.peek()) {
                smallList.offer(inNum);
                balance++;
            } else {
                largeList.offer(inNum);
                balance--;
            }

            if (balance < 0) {
                smallList.offer(largeList.poll());
            } else if (balance > 0) {
                largeList.offer(smallList.poll());
            }

            while (invalid.getOrDefault(smallList.peek(), 0) > 0) {
                invalid.merge(smallList.peek(), -1, Integer::sum);
                smallList.poll();
            }
            while (!largeList.isEmpty() && invalid.getOrDefault(largeList.peek(), 0) > 0) {
                invalid.merge(largeList.peek(), -1, Integer::sum);
                largeList.poll();
            }
        }

        double[] result = new double[medians.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = medians.get(i);
        }
        return result;
    }

    public static void main(String[] args) {
        int[] nums = {1, 3, -1, -3, 5, 3, 6, 7};
        System.out.println(Arrays.toString(medianSlidingWindow(nums, 3)));
        // [1.0, -1.0, -1.0, 3.0, 5.0, 6.0]
    }
}
```

## Complexity measures

Let **n** be the length of `nums` and **k** the window size.

- **Time:** `O(n log k)` — every value is inserted into a heap once, and each rebalance/prune step costs `O(log k)`.
- **Space:** `O(n)` — `O(k)` for the heaps, plus up to `O(n - k)` for the invalidation map.
