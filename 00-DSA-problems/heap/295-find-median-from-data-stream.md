# 1️⃣ What is the REAL problem?

You are getting numbers **one by one**:

```
1 → ?
1,2 → ?
1,2,3 → ?
1,2,3,4 → ?
```

After **every insertion**, you must return the **median**.

### Median definition

* Odd count → middle element
* Even count → average of two middle elements

### The difficulty

If you **sort every time**:

* Too slow (`O(n log n)` each time)

So we need a structure that:

* Keeps numbers **partially ordered**
* Gives the **middle** quickly

---

# 2️⃣ Key Insight (This is the “aha” moment)

> **You do NOT need the entire array sorted.
> You only need to know what is immediately left and right of the middle.**

That’s it.

So we divide numbers into **two halves**:

```
Smaller half | Larger half
```

And we only care about:

* Largest of smaller half
* Smallest of larger half

Because **the median lives exactly there**.

---

# 3️⃣ Why TWO heaps?

Let’s name the halves:

### Left half (smaller numbers)

We need to quickly know:

```
largest of the smaller half
```

👉 **Max-heap**

### Right half (larger numbers)

We need to quickly know:

```
smallest of the larger half
```

👉 **Min-heap**

So:

```text
left  = max-heap (lower half)
right = min-heap (upper half)
```

---

# 4️⃣ The two rules (INVARIANTS)

These rules must **always be true**:

### Rule 1 — Size balance

```
left.size() == right.size()
OR
left.size() == right.size() + 1
```

Why?

* If odd → left holds the extra (median lives there)
* If even → both halves equal

---

### Rule 2 — Order

```
Every element in left ≤ every element in right
```

Meaning:

```java
left.peek() <= right.peek()
```

---

# 5️⃣ If these rules hold, median is trivial

### Case 1: Odd count

```
left has one extra element
median = left.peek()
```

### Case 2: Even count

```
median = (left.peek() + right.peek()) / 2
```

👉 **No sorting needed**

---

# 6️⃣ Now let’s understand addNum() step-by-step

Here’s the code again:

```java
public void addNum(int num) {

    // Step 1: Add to max-heap
    left.offer(num);

    // Step 2: Ensure ordering property
    right.offer(left.poll());

    // Step 3: Balance sizes
    if (right.size() > left.size()) {
        left.offer(right.poll());
    }
}
```

Let’s explain **why this works**, not just what it does.

---

## 🔹 Step 1: Add to left (max-heap)

```java
left.offer(num);
```

We **temporarily assume**:

> “This number belongs to the smaller half.”

Even if that’s wrong — we’ll fix it.

---

## 🔹 Step 2: Fix ordering

```java
right.offer(left.poll());
```

What happens here?

* `left.poll()` removes the **largest element** from `left`
* That element is pushed into `right`

So after this:

* All elements in `left` ≤ all elements in `right`

👉 **Order invariant is restored**

---

## 🔹 Step 3: Fix size balance

```java
if (right.size() > left.size()) {
    left.offer(right.poll());
}
```

Why?

After step 2:

* `right` might have more elements

We want:

```
left >= right
```

So:

* Move smallest from `right` back to `left`

Now:

* Size invariant is restored

---

# 7️⃣ Walkthrough Example (This is important)

### Insert: 1

```
left:  [1]
right: []
median = 1
```

---

### Insert: 2

Step 1:

```
left: [2,1]
```

Step 2:

```
move max(left)=2 to right
left:  [1]
right: [2]
```

Step 3:

```
sizes equal → OK
```

Median:

```
(1 + 2) / 2 = 1.5
```

---

### Insert: 3

Step 1:

```
left: [3,1]
right: [2]
```

Step 2:

```
move 3 → right
left:  [1]
right: [2,3]
```

Step 3:

```
right > left → move 2 back
left:  [2,1]
right: [3]
```

Median:

```
left.peek() = 2
```

---

# 8️⃣ Why this strange insertion works (intuition)

This pattern:

```java
left.offer(num);
right.offer(left.poll());
```

Means:

> “I always push new numbers across the boundary once,
> so the boundary between halves stays correct.”

It’s a **controlled swap across the median boundary**.

---

# 9️⃣ Think of it visually

```
left (max-heap) | right (min-heap)
-------------------------------
1, 2, 3         | 4, 5, 6
       ↑ median lives here
```

We are **always adjusting the boundary**, never sorting the inside.

---

# 🔟 One-sentence mental model (MEMORIZE THIS)

> “I split the numbers into two halves and keep the boundary balanced so the median is always at the top.”

---
