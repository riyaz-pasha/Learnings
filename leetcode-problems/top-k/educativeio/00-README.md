Great—this is one of the **highest ROI patterns for interviews**. If you master *Top K Elements*, you’ll solve **heaps, sorting optimizations, streaming, frequency problems, and K-way merge variants**.

I’ll teach you **from zero → advanced**, with **clear intuition + Java code + interview thinking**.

---

# 🧠 1. What is “Top K Elements”?

👉 Problem type:

> “Find the top K (largest/smallest/most frequent/closest/etc.) elements”

### Examples:

* K largest numbers
* K smallest numbers
* K most frequent elements
* K closest points to origin
* Kth largest element

---

# 🎯 2. Brute Force (Baseline Thinking)

### Approach:

1. Sort array
2. Pick top K

```java
Arrays.sort(arr); // O(n log n)
```

👉 Time Complexity: **O(n log n)**
👉 Works, but **not optimal**

---

# 🚀 3. Core Insight (MOST IMPORTANT)

👉 We don’t need ALL elements sorted
👉 We only care about **K elements**

💡 So instead of sorting entire array:
👉 Maintain only **K useful elements**

---

# 🔥 4. The Weapon: Heap (PriorityQueue)

### Types:

* **Min Heap** → smallest at top
* **Max Heap** → largest at top

In Java:

```java
// Min Heap (default)
PriorityQueue<Integer> minHeap = new PriorityQueue<>();

// Max Heap
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
```

---

# ⚡ 5. Key Pattern (VERY IMPORTANT)

👉 When finding **Top K Largest**:

* Use **Min Heap of size K**

👉 When finding **Top K Smallest**:

* Use **Max Heap of size K**

---

# 🧩 6. Example 1: K Largest Elements

## Problem

Find K largest numbers in array

---

## 🧠 Intuition

👉 Keep only K largest seen so far
👉 If something smaller comes → ignore
👉 If bigger → replace smallest in heap

---

## ✅ Algorithm

1. Create **minHeap**
2. Add elements
3. If size > K → remove smallest

---

## 💻 Java Code

```java
import java.util.*;

class Solution {
    public List<Integer> findKLargest(int[] nums, int k) {
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();

        for (int num : nums) {
            minHeap.add(num);

            if (minHeap.size() > k) {
                minHeap.poll(); // remove smallest
            }
        }

        return new ArrayList<>(minHeap);
    }
}
```

---

## ⏱ Complexity

* Time: **O(n log k)** ✅
* Space: **O(k)**

👉 MUCH better than sorting

---

# 🧩 7. Example 2: K Smallest Elements

👉 Just flip logic

---

## 💻 Java

```java
PriorityQueue<Integer> maxHeap =
    new PriorityQueue<>(Collections.reverseOrder());

for (int num : nums) {
    maxHeap.add(num);

    if (maxHeap.size() > k) {
        maxHeap.poll(); // remove largest
    }
}
```

---

# 🧩 8. Example 3: Kth Largest Element

👉 Same logic, just return top

```java
public int findKthLargest(int[] nums, int k) {
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();

    for (int num : nums) {
        minHeap.add(num);
        if (minHeap.size() > k) {
            minHeap.poll();
        }
    }

    return minHeap.peek(); // kth largest
}
```

---

# 🧩 9. Example 4: Top K Frequent Elements

## 🔥 Very Common Interview Question

---

## 🧠 Idea

1. Count frequency
2. Keep top K by frequency

---

## 💻 Java

```java
import java.util.*;

class Solution {
    public List<Integer> topKFrequent(int[] nums, int k) {
        Map<Integer, Integer> freqMap = new HashMap<>();

        // Step 1: Count frequency
        for (int num : nums) {
            freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
        }

        // Step 2: Min Heap based on frequency
        PriorityQueue<Integer> minHeap =
            new PriorityQueue<>((a, b) -> freqMap.get(a) - freqMap.get(b));

        for (int num : freqMap.keySet()) {
            minHeap.add(num);

            if (minHeap.size() > k) {
                minHeap.poll();
            }
        }

        return new ArrayList<>(minHeap);
    }
}
```

---

# 🧩 10. Example 5: K Closest Points

## 🧠 Trick:

Use distance as priority

---

```java
class Point {
    int x, y;

    Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

public List<Point> kClosest(Point[] points, int k) {
    PriorityQueue<Point> maxHeap =
        new PriorityQueue<>((a, b) ->
            distance(b) - distance(a));

    for (Point p : points) {
        maxHeap.add(p);

        if (maxHeap.size() > k) {
            maxHeap.poll();
        }
    }

    return new ArrayList<>(maxHeap);
}

private int distance(Point p) {
    return p.x * p.x + p.y * p.y;
}
```

---

# 🧠 11. How to Identify Top K Problems (INTERVIEW GOLD)

👉 Look for keywords:

* “Top K”
* “K largest / smallest”
* “K most frequent”
* “K closest”
* “Kth element”
* “Stream + K”

---

# 🔁 12. Pattern Summary (SUPER IMPORTANT)

| Problem Type | Heap Used | Why               |
| ------------ | --------- | ----------------- |
| K largest    | Min Heap  | Remove smallest   |
| K smallest   | Max Heap  | Remove largest    |
| K frequent   | Min Heap  | Keep highest freq |
| K closest    | Max Heap  | Remove farthest   |
| Kth largest  | Min Heap  | Track K elements  |

---

# ⚔️ 13. Heap vs QuickSelect (Advanced)

## QuickSelect (Advanced Optimization)

👉 Average: **O(n)**
👉 No extra space

---

### 💻 Java (QuickSelect)

```java
public int findKthLargest(int[] nums, int k) {
    int target = nums.length - k;
    return quickSelect(nums, 0, nums.length - 1, target);
}

private int quickSelect(int[] nums, int left, int right, int k) {
    int pivot = nums[right];
    int p = left;

    for (int i = left; i < right; i++) {
        if (nums[i] <= pivot) {
            swap(nums, i, p++);
        }
    }

    swap(nums, p, right);

    if (p == k) return nums[p];
    else if (p < k) return quickSelect(nums, p + 1, right, k);
    else return quickSelect(nums, left, p - 1, k);
}

private void swap(int[] nums, int i, int j) {
    int temp = nums[i];
    nums[i] = nums[j];
    nums[j] = temp;
}
```

---

# 🧠 14. When to Use What?

| Scenario              | Best Approach |
| --------------------- | ------------- |
| Need top K elements   | Heap          |
| Need Kth element only | QuickSelect   |
| Streaming data        | Heap          |
| Large data (online)   | Heap          |
| One-time computation  | QuickSelect   |

---

# 🔥 15. Interview Strategy (VERY IMPORTANT)

When you see a problem:

### Step 1:

👉 Ask: “Do I need full sorting?”

If NO → think **heap**

---

### Step 2:

👉 Ask: “Do I need top K?”

If YES → use **size K heap**

---

### Step 3:

👉 Decide heap type:

* Want largest → min heap
* Want smallest → max heap

---

# 🧪 16. Common Mistakes

❌ Using max heap for K largest
❌ Not limiting heap size
❌ Sorting unnecessarily
❌ Wrong comparator logic
❌ Forgetting map for frequency problems

---

# 🧠 Final Mental Model

> “I only care about K elements → so I will maintain ONLY K elements → using a heap.”

---

# 🚀 If you want next level

I can take you through:

* 🔁 **Sliding window + Top K (very advanced)**
* 🌊 **Streaming median (two heaps pattern)**
* 🔗 **Top K + K-way merge combined problems**
* 💣 **Hard Leetcode problems from this pattern**

Just say: **“go deeper”** and I’ll level you up.

---

