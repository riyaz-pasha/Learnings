The key idea is: **don't try to keep the entire stream sorted**. We only need the **middle one or two elements**.

The standard interview solution is the **Two Heaps approach**.

## 1. Intuition — Two Heaps

Split the numbers into two halves:

```text
Small half              Large half
(max heap)              (min heap)

[1, 2, 3] | [4, 5, 6]
       ↑       ↑
      max     min
```

We maintain:

* `maxHeap` → contains the **smaller half**
* `minHeap` → contains the **larger half**

And maintain two rules:

1. `maxHeap.size()` is either equal to `minHeap.size()`
2. Or `maxHeap.size()` is exactly **one larger**

Therefore:

### Odd number of elements

```text
maxHeap = [1, 2, 3]
minHeap = [4, 5]

median = maxHeap.peek()
       = 3
```

### Even number of elements

```text
maxHeap = [1, 2, 3]
minHeap = [4, 5, 6]

median = (3 + 4) / 2.0
       = 3.5
```

So `findMedian()` becomes **O(1)** because we only call `peek()`.

---

# 2. Java 24 — Two Heaps

```java
import java.util.Collections;
import java.util.PriorityQueue;

public class MedianOfStream {

    // Max heap -> stores the smaller half of the numbers
    private final PriorityQueue<Integer> maxHeap;

    // Min heap -> stores the larger half of the numbers
    private final PriorityQueue<Integer> minHeap;

    public MedianOfStream() {
        maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        minHeap = new PriorityQueue<>();
    }

    public void insertNum(int num) {

        /*
         * Step 1:
         * Decide which half the number belongs to.
         *
         * If num <= largest element of smaller half,
         * put it in maxHeap.
         *
         * Otherwise put it in minHeap.
         */
        if (maxHeap.isEmpty() || num <= maxHeap.peek()) {
            maxHeap.offer(num);
        } else {
            minHeap.offer(num);
        }

        /*
         * Step 2:
         * Rebalance the heaps.
         *
         * We always want:
         *
         * maxHeap.size() == minHeap.size()
         * OR
         * maxHeap.size() == minHeap.size() + 1
         */

        if (maxHeap.size() > minHeap.size() + 1) {
            minHeap.offer(maxHeap.poll());
        } else if (minHeap.size() > maxHeap.size()) {
            maxHeap.offer(minHeap.poll());
        }
    }

    public double findMedian() {

        // Odd number of elements
        if (maxHeap.size() > minHeap.size()) {
            return maxHeap.peek();
        }

        // Even number of elements
        return ((long) maxHeap.peek() + minHeap.peek()) / 2.0;
    }
}
```

Notice this:

```java
((long) maxHeap.peek() + minHeap.peek()) / 2.0
```

I deliberately cast to `long` before adding so that the addition cannot overflow an `int` if the constraints are later increased.

---

# 3. Let's walk through an example

Suppose:

```text
insert: 5, 2, 8, 3, 1
```

### Insert 5

```text
maxHeap: [5]
minHeap: []

median = 5
```

### Insert 2

2 belongs to the smaller half:

```text
maxHeap: [5, 2]
minHeap: []
```

Too many elements in `maxHeap`, so move its maximum:

```text
maxHeap: [2]
minHeap: [5]
```

Median:

```text
(2 + 5) / 2 = 3.5
```

---

### Insert 8

8 belongs to the larger half:

```text
maxHeap: [2]
minHeap: [5, 8]
```

Rebalance:

```text
maxHeap: [5, 2]
minHeap: [8]
```

Median:

```text
5
```

---

### Insert 3

3 <= 5:

```text
maxHeap: [5, 2, 3]
minHeap: [8]
```

Rebalance:

```text
maxHeap: [3, 2]
minHeap: [5, 8]
```

Median:

```text
(3 + 5) / 2 = 4
```

---

### Insert 1

```text
maxHeap: [3, 2, 1]
minHeap: [5, 8]
```

Median:

```text
3
```

Sorted array would be:

```text
[1, 2, 3, 5, 8]
         ↑
       median
```

We never actually sort the data.

---

# 4. Why does this work?

The important invariant is:

```text
          maxHeap       minHeap
        ┌─────────┐   ┌─────────┐
        │ smaller │   │  larger │
        │  half   │   │  half   │
        └─────────┘   └─────────┘
             ↑             ↑
          maximum       minimum
```

We guarantee:

```text
every element in maxHeap
        <=
every element in minHeap
```

Therefore the only elements we need to know are:

```java
maxHeap.peek()
minHeap.peek()
```

Those are exactly the elements around the median.

That's the **interview insight**:

> **If I only need the middle elements, I don't need to maintain complete ordering. I only need ordering around the boundary between the two halves.**

---

# 5. Complexity

Let `n` be the number of inserted elements.

| Operation      |   Complexity |
| -------------- | -----------: |
| `insertNum()`  | **O(log n)** |
| `findMedian()` |     **O(1)** |
| Space          |     **O(n)** |

Why is insertion `O(log n)`?

Heap operations:

```java
offer()  -> O(log n)
poll()   -> O(log n)
peek()   -> O(1)
```

At most a couple of heap operations happen during insertion.

---

# 6. Other possible solutions

There are several approaches worth knowing for interviews.

### Approach 1 — Store + sort every time

```java
List<Integer> nums = new ArrayList<>();

public void insertNum(int num) {
    nums.add(num);
}

public double findMedian() {
    Collections.sort(nums);

    int n = nums.size();

    if (n % 2 == 1) {
        return nums.get(n / 2);
    }

    return ((long) nums.get(n / 2 - 1) + nums.get(n / 2)) / 2.0;
}
```

Complexity:

```text
insertNum = O(1)
findMedian = O(n log n)
space = O(n)
```

Very simple, but doesn't satisfy the required `O(1)` median lookup efficiently.

---

### Approach 2 — Keep the list sorted

Using binary search to find insertion position:

```text
[1, 3, 5, 8, 10]
       ↑
    insert 6
```

Find position using binary search:

```text
O(log n)
```

But inserting into an `ArrayList` requires shifting elements:

```text
O(n)
```

So:

```text
insertNum = O(n)
findMedian = O(1)
space = O(n)
```

This technically gives constant-time median, but insertion is slower than two heaps.

---

### Approach 3 — Balanced BST / Tree-based structure

A balanced BST can maintain sorted elements in:

```text
O(log n)
```

But Java's standard `TreeSet` has an important problem: it doesn't support duplicate values naturally, and it doesn't provide direct "get kth element" operations.

You could build an augmented balanced tree containing subtree sizes, but that's considerably more complicated.

For this problem, **two heaps are the cleanest solution**.

---

# 7. What I would say in an interview

A concise explanation would be:

> "I need the median, so I only care about the boundary between the smaller and larger halves of the numbers. I'll maintain two heaps: a max heap for the smaller half and a min heap for the larger half. I'll keep their sizes balanced so the max heap either has the same number of elements or exactly one more. Then the median is either the top of the max heap for an odd count, or the average of the two heap tops for an even count. Insertion takes O(log n), while finding the median takes O(1)."

### The mental pattern to remember

Whenever you see:

> **"Numbers are continuously coming in + find median repeatedly"**

Think immediately:

```text
                 MEDIAN
                   │
          ┌────────┴────────┐
          │                 │
     smaller half       larger half
     MAX HEAP            MIN HEAP
```

This is one of the most important **heap patterns** for interviews.
