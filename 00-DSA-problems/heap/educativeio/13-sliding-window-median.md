This is the classic **Sliding Window Median** problem (LeetCode 480).

The key difficulty is that when the window moves, we need to:

1. Insert the new number.
2. Remove the number leaving the window.
3. Get the median efficiently.

There are several possible solutions. For interviews, I'd know **3 approaches**, with the **two-heaps + lazy deletion** approach being the most important production/interview solution.

---

# 1. Brute Force — Sort Every Window

### Idea

For every window:

```text
window = [1, 3, -1]
sort → [-1, 1, 3]
median → 1
```

Then move the window and repeat.

### Java 24

```java
import java.util.*;

class Solution {

    public double[] medianSlidingWindow(int[] nums, int k) {
        int n = nums.length;
        double[] result = new double[n - k + 1];

        for (int i = 0; i <= n - k; i++) {

            // Copy current window
            int[] window = Arrays.copyOfRange(nums, i, i + k);

            // Sort the window
            Arrays.sort(window);

            // Find median
            if (k % 2 == 1) {
                result[i] = window[k / 2];
            } else {
                result[i] = ((long) window[k / 2 - 1] + window[k / 2]) / 2.0;
            }
        }

        return result;
    }
}
```

### Complexity

There are `n - k + 1` windows.

Sorting each window:

```text
O(k log k)
```

Therefore:

```text
Time:  O((n-k+1) * k log k)
Space: O(k)
```

This is simple, but not the approach I'd use in an interview if optimization is expected.

---

# 2. TreeMap / Multiset Approach

Java doesn't have a built-in `TreeMultiSet`, but we can simulate one using:

```java
TreeMap<Integer, Integer>
```

where:

```text
key   = number
value = frequency
```

However, finding the median still requires walking through the map unless we augment the structure.

So a plain `TreeMap` implementation has a potential:

```text
O(k)
```

median lookup.

We can improve it using **two TreeMaps** representing the two halves, while maintaining their sizes.

But there is an even cleaner heap-based solution.

---

# 3. Two Heaps + Lazy Deletion ⭐

This is the approach I'd recommend knowing for interviews.

## The core idea

Split every window into two halves:

```text
             median
                ↓
       ┌─────────────────┐
       │                 │
    smaller           larger
       │                 │
   Max Heap           Min Heap
```

For example:

```text
[1, 2, 3, 4, 5]

smaller: [1, 2, 3]
                 ↑
              max = 3

larger:  [4, 5]
          ↑
       min = 4
```

For an odd-sized window:

```text
median = maxHeap.peek()
```

For an even-sized window:

```text
median = (maxHeap.peek() + minHeap.peek()) / 2.0
```

### The problem

A heap doesn't support efficiently removing an arbitrary value.

Suppose:

```text
window = [1, 2, 3, 4, 5]
```

Then the window moves:

```text
[2, 3, 4, 5, 6]
```

We need to remove `1`.

But `1` could be buried somewhere inside the heap.

So instead of physically removing it immediately, we use **lazy deletion**.

---

# Lazy Deletion

Maintain a frequency map:

```java
Map<Integer, Integer> delayed
```

When a number leaves the window:

```java
delayed.merge(num, 1, Integer::sum);
```

We don't immediately remove it from the heap.

Later, when that number reaches the top of its heap, we remove it.

For example:

```text
Heap:

       1
      / \
     3   5
```

If `1` leaves the window:

```text
delayed[1] = 1
```

We leave the heap alone.

When `1` becomes the top:

```text
while delayed contains heap.peek():
    remove it
```

This technique is called **lazy deletion**.

---

# Important: We need logical sizes

Because deleted elements can still physically exist inside the heaps, we cannot use:

```java
maxHeap.size()
minHeap.size()
```

to determine the actual window partition.

Instead maintain:

```java
smallSize
largeSize
```

These represent the number of **valid** elements.

We maintain:

```text
smallSize == largeSize
```

or

```text
smallSize == largeSize + 1
```

So the max heap always contains the median when `k` is odd.

---

# Java 24 Solution

```java
import java.util.*;

class Solution {

    /*
     * Max heap:
     * Contains the smaller half of the current window.
     *
     * Min heap:
     * Contains the larger half of the current window.
     *
     * delayed:
     * Lazy-deletion map.
     * delayed[x] = number of x's that should eventually be removed.
     */
    private final PriorityQueue<Integer> small =
            new PriorityQueue<>(Comparator.reverseOrder());

    private final PriorityQueue<Integer> large =
            new PriorityQueue<>();

    private final Map<Integer, Integer> delayed =
            new HashMap<>();

    // Number of VALID elements in each heap.
    private int smallSize = 0;
    private int largeSize = 0;

    public double[] medianSlidingWindow(int[] nums, int k) {

        int n = nums.length;
        double[] result = new double[n - k + 1];

        // Build the first window.
        for (int i = 0; i < k; i++) {
            add(nums[i]);
        }

        result[0] = getMedian(k);

        // Slide the window.
        for (int right = k; right < n; right++) {

            int entering = nums[right];
            int leaving = nums[right - k];

            // Add the new number.
            add(entering);

            // Remove the number leaving the window.
            remove(leaving);

            // Clean invalid elements from heap tops.
            prune(small);
            prune(large);

            // Restore heap size invariant.
            rebalance();

            result[right - k + 1] = getMedian(k);
        }

        return result;
    }

    /*
     * Add a number to the correct half.
     */
    private void add(int num) {

        if (small.isEmpty() || num <= small.peek()) {
            small.offer(num);
            smallSize++;
        } else {
            large.offer(num);
            largeSize++;
        }

        rebalance();
    }

    /*
     * Mark a number for lazy deletion.
     */
    private void remove(int num) {

        delayed.merge(num, 1, Integer::sum);

        /*
         * We need to decide which logical half the number belongs to.
         *
         * If num <= small.peek(), it belongs to the smaller half.
         */
        if (num <= small.peek()) {
            smallSize--;
        } else {
            largeSize--;
        }

        /*
         * If the number we're deleting is currently at
         * the top of a heap, remove it immediately.
         *
         * Otherwise leave it there and prune it later.
         */
        prune(small);
        prune(large);

        rebalance();
    }

    /*
     * Restore:
     *
     * smallSize == largeSize
     *
     * OR
     *
     * smallSize == largeSize + 1
     */
    private void rebalance() {

        if (smallSize > largeSize + 1) {

            // Move largest element from small -> large.
            large.offer(small.poll());

            smallSize--;
            largeSize++;

            prune(small);

        } else if (smallSize < largeSize) {

            // Move smallest element from large -> small.
            small.offer(large.poll());

            largeSize--;
            smallSize++;

            prune(large);
        }
    }

    /*
     * Remove elements from the top of the heap
     * that have already left the sliding window.
     */
    private void prune(PriorityQueue<Integer> heap) {

        while (!heap.isEmpty()) {

            int num = heap.peek();

            Integer count = delayed.get(num);

            if (count == null) {
                break;
            }

            // Remove the physically stale element.
            heap.poll();

            if (count == 1) {
                delayed.remove(num);
            } else {
                delayed.put(num, count - 1);
            }
        }
    }

    /*
     * Get the median from the two heaps.
     */
    private double getMedian(int k) {

        if ((k & 1) == 1) {

            // Odd window:
            // small has one extra element.
            return small.peek();

        } else {

            // Even window:
            // Average of the two middle elements.
            //
            // Cast to long first to avoid integer overflow.
            return ((long) small.peek() + large.peek()) / 2.0;
        }
    }
}
```

---

# But there's an important subtlety

The most common mistake in this solution is this line:

```java
if (num <= small.peek())
```

when removing an element.

Why does this work with duplicates?

Consider:

```text
small = [2, 2]
large = [2, 3]
```

Suppose we're removing `2`.

Which `2` are we removing?

We don't actually care about the physical copy.

We only care about maintaining the **logical count** of the two halves.

Because all `2`s are equivalent from the perspective of the median, assigning the removed `2` to `small` is valid as long as the heap invariants are restored afterward.

That's why this approach works with duplicates.

---

# Let's Walk Through an Example

Consider:

```text
nums = [1, 3, -1, -3, 5, 3, 6, 7]
k = 3
```

First window:

```text
[1, 3, -1]
```

Split:

```text
small (max heap): [-1, 1]
large (min heap): [3]
```

Median:

```text
1
```

Move window:

```text
[3, -1, -3]
```

Remove:

```text
1
```

Add:

```text
-3
```

After rebalancing:

```text
small: [-1, -3]
large: [3]
```

Median:

```text
-1
```

Next:

```text
[-1, -3, 5]
```

Median:

```text
-1
```

And eventually:

```text
[1, 3, -1] → 1
[3, -1, -3] → -1
[-1, -3, 5] → -1
[-3, 5, 3] → 3
[5, 3, 6] → 5
[3, 6, 7] → 6
```

Result:

```text
[1, -1, -1, 3, 5, 6]
```

---

# How to Think of This in an Interview

Don't start by thinking:

> "I need to calculate a median."

Instead think:

> **"I need to dynamically maintain the middle element while elements are both inserted and removed."**

Then ask:

### 1. How do I get the median quickly?

Two sorted halves.

```text
LEFT              RIGHT
-----             ------
1 2 3 | 4 5 6
      ↑
     middle
```

Heaps give us the boundary of each half:

```text
MaxHeap → largest element of LEFT
MinHeap → smallest element of RIGHT
```

Therefore:

```text
             MaxHeap       MinHeap
                 ↓             ↓
             [1 2 3] | [4 5 6]
                   ↑
                median
```

### 2. How do I insert?

Easy:

```text
num <= maxHeap.peek()
        ↓
     MaxHeap

otherwise
        ↓
     MinHeap
```

### 3. How do I remove?

That's the hard part.

Heaps don't support:

```text
remove(any arbitrary element)
```

efficiently.

So:

> **Lazy deletion.**

Mark it as deleted:

```text
delayed[num]++
```

and physically remove it when it reaches the heap top.

### 4. How do I keep the median at the top?

Rebalance:

```text
smallSize == largeSize
```

or

```text
smallSize == largeSize + 1
```

That's the entire idea.

---

# Complexity

For every element:

### Insert

Heap insertion:

```text
O(log k)
```

### Remove

We mark it:

```text
O(1)
```

and eventually remove it from a heap:

```text
O(log k)
```

Amortized over all elements:

```text
O(log k)
```

### Median

Just peek:

```text
O(1)
```

Therefore:

```text
Time:  O(n log k)
Space: O(k)
```

This is the **optimal practical approach** for arbitrary integer values using standard Java data structures.

---

# Comparison of Approaches

| Approach                           |     Insert |             Remove |           Median |           Overall |
| ---------------------------------- | ---------: | -----------------: | ---------------: | ----------------: |
| Sort every window                  | O(k log k) |                  — |             O(1) |   **O(nk log k)** |
| Balanced Tree / augmented multiset |   O(log k) |           O(log k) |             O(1) |    **O(n log k)** |
| Two Heaps + Lazy Deletion          |   O(log k) | O(log k) amortized |             O(1) |    **O(n log k)** |
| Counting/Frequency array           |       O(1) |               O(1) | depends on range | Can be O(n·range) |

## What I'd give in an interview

**Two Heaps + Lazy Deletion.**

The mental template is:

```text
Dynamic Median
      ↓
Two halves
      ↓
MaxHeap + MinHeap
      ↓
Arbitrary deletion problem
      ↓
Lazy deletion
      ↓
Rebalance
      ↓
Median = heap tops
```

This same pattern is extremely useful for other **dynamic order-statistics** problems too.
