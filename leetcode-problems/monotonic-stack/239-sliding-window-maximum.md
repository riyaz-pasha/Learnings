This is the **Sliding Window Maximum** — a **textbook monotonic deque problem**.
I’ll explain it in the way interviewers expect you to *discover* it, not just apply it.

---

# 🪟 Sliding Window Maximum — Monotonic Deque (Clear & Intuitive)

---

## 1️⃣ Why brute force fails

For every window of size `k`:

* Scan `k` elements to find max → `O(k)`
* Total windows ≈ `n`

⛔ Time = `O(n × k)` → TLE when `n = 10⁵`

We need **O(n)**.

---

## 2️⃣ Key Observation (The BIG CUE 🧠)

Ask yourself:

> “When a new element enters the window, which old elements become useless?”

Answer:

* Any element **smaller than the new element**
* Because the new element will dominate them for all future windows

👉 This immediately suggests:

> **Maintain elements in decreasing order**

That is the **monotonic deque** trigger.

---

## 3️⃣ Why DEQUE, not stack?

We need to:

* Remove elements from the **front** (when they go out of window)
* Remove elements from the **back** (when they are smaller)

So we need:

```
push back
pop back
pop front
peek front
```

👉 Use **Deque (double-ended queue)**

---

## 4️⃣ What the deque stores (VERY IMPORTANT)

The deque stores **indices**, not values.

Why?

* To check if an element is **out of the window**
* To compare values using `nums[index]`

---

## 5️⃣ Invariant of the deque

At all times:

```
nums[deque[0]] >= nums[deque[1]] >= nums[deque[2]] ...
```

So:

* Front of deque = **maximum of current window**

---

## 6️⃣ Algorithm (Step-by-step)

For each index `i`:

### Step 1: Remove out-of-window indices

```java
if deque.front() == i - k → remove front
```

---

### Step 2: Maintain decreasing order

```java
while deque not empty AND nums[deque.back()] < nums[i]:
    pop back
```

Why?

* Smaller elements can never be max again

---

### Step 3: Add current index

```java
deque.addLast(i)
```

---

### Step 4: Record answer

If window is formed (`i >= k - 1`):

```java
answer.add(nums[deque.front()])
```

---

## 7️⃣ Dry Run (Example 1)

### nums = `[1,3,-1,-3,5,3,6,7]`, k = 3

We show deque as **indices(values)**

---

### i = 0 → 1

```
deque = [0(1)]
```

---

### i = 1 → 3

```
pop 1 < 3
deque = [1(3)]
```

---

### i = 2 → -1

```
deque = [1(3), 2(-1)]
window formed → max = 3
```

---

### i = 3 → -3

```
out of window? no
deque = [1(3), 2(-1), 3(-3)]
max = 3
```

---

### i = 4 → 5

```
pop -3, -1, 3
deque = [4(5)]
max = 5
```

---

### i = 5 → 3

```
deque = [4(5), 5(3)]
max = 5
```

---

### i = 6 → 6

```
pop 3, 5
deque = [6(6)]
max = 6
```

---

### i = 7 → 7

```
pop 6
deque = [7(7)]
max = 7
```

---

### ✅ Output

```
[3,3,5,5,6,7]
```

---

## 8️⃣ Java Implementation (Interview-Ready)

```java
class Solution {
    public int[] maxSlidingWindow(int[] nums, int k) {
        int n = nums.length;
        int[] result = new int[n - k + 1];
        Deque<Integer> deque = new ArrayDeque<>();

        int idx = 0;

        for (int i = 0; i < n; i++) {

            // Remove indices out of window
            if (!deque.isEmpty() && deque.peekFirst() == i - k) {
                deque.pollFirst();
            }

            // Maintain decreasing order
            while (!deque.isEmpty() && nums[deque.peekLast()] < nums[i]) {
                deque.pollLast();
            }

            deque.addLast(i);

            // Add to result when window is ready
            if (i >= k - 1) {
                result[idx++] = nums[deque.peekFirst()];
            }
        }
        return result;
    }
}
```

---

## 9️⃣ Time & Space Complexity

* **Time:** `O(n)`

  * Each index added and removed once
* **Space:** `O(k)` (deque size)

---

## 🔑 How to IDENTIFY this problem in interviews

### Cues:

* “Sliding window”
* “Max / min in window”
* “Window moves one step at a time”
* “Need fast updates”

👉 Immediate thought:

> **Monotonic deque**

---

## 🧠 Relation to other problems

| Problem                | Structure        |
| ---------------------- | ---------------- |
| Sliding Window Maximum | Decreasing deque |
| Sliding Window Minimum | Increasing deque |
| Remove K Digits        | Increasing stack |
| Histogram              | Increasing stack |
| Rain Water             | Decreasing stack |

---

## 🧠 One-line Memory Hook

> **“Keep only useful candidates for max, in decreasing order.”**

