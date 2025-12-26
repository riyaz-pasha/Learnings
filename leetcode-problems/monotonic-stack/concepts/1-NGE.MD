# 1️⃣ What is a Monotonic Stack?

A **monotonic stack** is a stack that maintains its elements in a **specific order**:

* **Monotonically increasing stack**

  * Elements increase from bottom → top
  * Example: `2, 5, 8`
* **Monotonically decreasing stack**

  * Elements decrease from bottom → top
  * Example: `9, 6, 3`

👉 The stack is **not fully sorted**, but it preserves monotonic order based on how we **push and pop**.

---

# 2️⃣ Next Greater Element (NGE) — Problem Restated

> For every element in the array, find the **first element to its right** that is **strictly greater**.

Example:

```
Input:  [4, 5, 2, 10, 8]
Output: [5, 10, 10, -1, -1]
```

---

# 3️⃣ Brute Force (Why We Need Monotonic Stack)

Brute force idea:

* For each element, scan right side until you find a greater element.

⏱ Time Complexity:

```
O(n^2)
```

This is **too slow** for large inputs.

---

# 4️⃣ Key Insight Behind Monotonic Stack

### Instead of repeatedly scanning right,

### 👉 **use a stack to remember "useful candidates"**

When processing elements:

* If a **new element is greater**, it becomes the **next greater element** for all **smaller elements waiting in the stack**.

So:

* Stack holds elements **whose NGE is not yet found**
* Once we find a greater element → resolve them

---

# 5️⃣ Which Monotonic Stack for NGE?

### 🔥 We use a **Monotonically Decreasing Stack**

Why?

* We want the **next greater element**
* Smaller elements should wait for a bigger one
* So stack keeps elements in **decreasing order**

Example stack state:

```
Top
 ↓
[5]
[7]
[10]
```

(From bottom → top: 10 > 7 > 5)

---

# 6️⃣ Step-by-Step Algorithm (Left → Right)

For each element `arr[i]`:

1. While stack is **not empty** AND
   `stack.top() < arr[i]`

   * Pop the stack
   * For the popped element, `arr[i]` is its **Next Greater Element**
2. Push `arr[i]` into the stack
3. After traversal:

   * Remaining stack elements → no greater element → `-1`

---

# 7️⃣ Dry Run Example

Input:

```
arr = [4, 5, 2, 10, 8]
```

### Initialize:

```
Stack = empty
Result = [-1, -1, -1, -1, -1]
```

---

### i = 0 → 4

```
Stack empty → push 4
Stack: [4]
```

---

### i = 1 → 5

```
5 > 4 → pop 4 → NGE[4] = 5
Push 5
Stack: [5]
```

---

### i = 2 → 2

```
2 < 5 → push 2
Stack: [5, 2]
```

---

### i = 3 → 10

```
10 > 2 → pop 2 → NGE[2] = 10
10 > 5 → pop 5 → NGE[5] = 10
Push 10
Stack: [10]
```

---

### i = 4 → 8

```
8 < 10 → push 8
Stack: [10, 8]
```

---

### End of array

```
Remaining elements → NGE = -1
```

✅ Final result:

```
[5, 10, 10, -1, -1]
```

---

# 8️⃣ Why This Works (Core Logic)

Each element is:

* **Pushed once**
* **Popped once**

So:

```
Total operations = O(n)
```

This is the magic of monotonic stacks.

---

# 9️⃣ Java Implementation (Index-based – Interview Friendly)

```java
public int[] nextGreaterElements(int[] arr) {
    int n = arr.length;
    int[] nge = new int[n];
    Arrays.fill(nge, -1);

    Stack<Integer> stack = new Stack<>(); // stores indices

    for (int i = 0; i < n; i++) {
        while (!stack.isEmpty() && arr[stack.peek()] < arr[i]) {
            int idx = stack.pop();
            nge[idx] = arr[i];
        }
        stack.push(i);
    }

    return nge;
}
```

---

# 🔟 Why Store Indices Instead of Values?

Because:

* We need to **update result array**
* Indices let us map values back to positions

---

# 1️⃣1️⃣ Pattern Recognition (Very Important for Interviews)

| Problem                | Stack Type               |
| ---------------------- | ------------------------ |
| Next Greater Element   | Monotonic **Decreasing** |
| Next Smaller Element   | Monotonic **Increasing** |
| Previous Greater       | Reverse traversal        |
| Stock Span             | Decreasing               |
| Largest Histogram Area | Increasing               |

---

# 1️⃣2️⃣ One-Line Memory Trick 🧠

> **“If I’m looking for something greater → keep stack decreasing.”**
> **“If I’m looking for something smaller → keep stack increasing.”**

---

# 1️⃣3️⃣ Common Interview Follow-ups

* Circular NGE → traverse array twice (`i % n`)
* Previous Greater → traverse from **right to left**
* Count days / distances → store indices
* Handle duplicates → use `<=` vs `<` carefully

---
