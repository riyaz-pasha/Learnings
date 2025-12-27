# 📈 Stock Span — Monotonic Stack (Clear Intuition)

---

## 1️⃣ What is the span asking?

For today’s price `P`:

> “How many consecutive days (including today), going backward, had prices **≤ P**?”

That means:

* You keep moving left
* Stop **only when you hit a higher price**

So span = distance to the **previous greater price**.

---

## 2️⃣ The BIG CUE 🧠

Ask this question:

> “When does a previous day stop contributing to today’s span?”

Answer:

> When its price is **greater than today’s price**

That tells us:

* Smaller or equal prices are **useless blockers**
* Larger prices are **hard stops**

👉 This is **Previous Greater Element** thinking.

---

## 3️⃣ Why a stack?

Because:

* Today’s price can invalidate **many previous days**
* Once invalidated, those days will **never matter again**

That’s the exact situation where a **monotonic stack** shines.

---

## 4️⃣ What does the stack store?

Each stack entry stores:

```
(price, span)
```

Why span?

* Instead of counting days one by one
* We **compress history** into chunks

This is the key optimization.

---

## 5️⃣ Stack invariant (VERY IMPORTANT)

The stack is **monotonically decreasing by price**:

```
top
 ↓
(100, 6)
(120, 2)
(150, 1)
```

Meaning:

* Prices strictly decrease from bottom → top
* Each entry represents a block of days

---

## 6️⃣ Core logic (heart of the solution)

When today’s price = `price`:

1. Start span = `1` (today itself)
2. While stack not empty AND `stack.top.price <= price`:

   * Add `stack.top.span` to current span
   * Pop the stack
3. Push `(price, span)`
4. Return `span`

---

## 7️⃣ Why does popping work?

If:

```
previousPrice <= todayPrice
```

Then:

* Today’s span includes **all the days** that previous price covered
* That previous price will **never be useful again**
  (because today blocks it permanently)

So popping is safe and optimal.

---

## 8️⃣ Dry Run (Example)

Input prices:

```
[100, 80, 60, 70, 60, 75, 85]
```

---

### Day 1: 100

```
stack = [(100,1)]
span = 1
```

---

### Day 2: 80

```
80 < 100 → no pop
stack = [(100,1), (80,1)]
span = 1
```

---

### Day 3: 60

```
60 < 80 → no pop
stack = [(100,1), (80,1), (60,1)]
span = 1
```

---

### Day 4: 70

```
70 > 60 → pop (60,1) → span = 2
70 < 80 → stop
stack = [(100,1), (80,1), (70,2)]
span = 2
```

---

### Day 5: 60

```
60 < 70 → no pop
stack = [(100,1), (80,1), (70,2), (60,1)]
span = 1
```

---

### Day 6: 75

```
75 > 60 → pop (60,1) → span = 2
75 > 70 → pop (70,2) → span = 4
75 < 80 → stop
stack = [(100,1), (80,1), (75,4)]
span = 4
```

---

### Day 7: 85

```
85 > 75 → pop (75,4) → span = 5
85 > 80 → pop (80,1) → span = 6
85 < 100 → stop
stack = [(100,1), (85,6)]
span = 6
```

---

### ✅ Output

```
[1,1,1,2,1,4,6]
```

---

## 9️⃣ Java Implementation (Interview-Ready)

```java
class StockSpanner {

    private Stack<int[]> stack;

    public StockSpanner() {
        stack = new Stack<>();
    }

    public int next(int price) {
        int span = 1;

        while (!stack.isEmpty() && stack.peek()[0] <= price) {
            span += stack.pop()[1];
        }

        stack.push(new int[]{price, span});
        return span;
    }
}
```

---

## 🔟 Time & Space Complexity

* **Time:** `O(1)` amortized per call
  (each element pushed & popped once)
* **Space:** `O(n)` in worst case

---

## 🔑 How to IDENTIFY this problem in interviews

### Look for these cues:

* “Consecutive days”
* “Going backward”
* “Less than or equal”
* “Online / streaming input”

👉 Immediately think:

> **Previous Greater Element + Monotonic Stack**

---

## 🧠 Relation to other problems

| Problem              | Stack Type         |
| -------------------- | ------------------ |
| Stock Span           | Decreasing         |
| Next Greater Element | Decreasing         |
| Histogram            | Increasing         |
| Rain Water           | Decreasing         |
| Sliding Window Max   | Deque (decreasing) |

---

## 🧠 One-line memory hook

> **“Pop all prices that are smaller or equal — they belong to today.”**

---
