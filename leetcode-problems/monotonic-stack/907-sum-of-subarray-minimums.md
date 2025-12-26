# 🔢 Sum of Subarray Minimums

## Problem Restated (Key Insight)

Instead of:

> “Find min of every subarray”

Rephrase it as:

> **“For each element `arr[i]`, count how many subarrays where it is the minimum.”**

Then:

```
answer = Σ (arr[i] × number of subarrays where arr[i] is minimum)
```

This reframing is the **biggest cue** for monotonic stack.

---

## 1️⃣ Why brute force fails

Total subarrays = `O(n²)`
Finding min per subarray = `O(n)`

❌ `O(n³)` → impossible

Even optimized:

* Still `O(n²)` → TLE

---

## 2️⃣ Key CUE to think Monotonic Stack 🧠

Ask yourself:

> “For an element arr[i], how far can it expand left and right while staying the minimum?”

This immediately implies:

* Look for **smaller element on the left**
* Look for **smaller element on the right**

👉 This is a **Previous Smaller Element (PSE)** + **Next Smaller Element (NSE)** problem.

---

## 3️⃣ Why “smaller”, not “greater”?

Because:

* We want arr[i] to be the **minimum**
* Expansion stops when a **smaller** value appears

So:

* Left boundary = Previous Smaller
* Right boundary = Next Smaller

---

## 4️⃣ Handling duplicates (VERY IMPORTANT)

To avoid double counting:

* On **left** → use **strictly smaller (`<`)**
* On **right** → use **smaller or equal (`<=`)**

This guarantees **each subarray minimum is counted exactly once**.

---

## 5️⃣ What the stack stores

* Stack stores **indices**
* Stack is **monotonic increasing** (values increase from bottom to top)

Why increasing?

* Smaller elements break expansion

---

## 6️⃣ Contribution Formula (CORE)

Let:

* `left[i]` = distance to Previous Smaller
* `right[i]` = distance to Next Smaller or Equal

Then:

```
arr[i] contributes in (left[i] × right[i]) subarrays
Contribution = arr[i] × left[i] × right[i]
```

---

## 7️⃣ How to compute left[] (Previous Smaller)

Traverse **left → right**

```java
while stack not empty AND arr[stack.peek()] > arr[i]:
    pop
```

After popping:

* If stack empty → no smaller on left
* Else → stack.peek() is PSE

---

## 8️⃣ How to compute right[] (Next Smaller or Equal)

Traverse **right → left**

```java
while stack not empty AND arr[stack.peek()] >= arr[i]:
    pop
```

After popping:

* If stack empty → no smaller on right
* Else → stack.peek() is NSE

---

## 9️⃣ Full Dry Run Example

### arr = [3, 1, 2, 4]

### Step 1: Compute left[]

| i | arr[i] | PSE index | left[i] |
| - | ------ | --------- | ------- |
| 0 | 3      | none      | 1       |
| 1 | 1      | none      | 2       |
| 2 | 2      | 1         | 1       |
| 3 | 4      | 2         | 1       |

---

### Step 2: Compute right[]

| i | arr[i] | NSE index | right[i] |
| - | ------ | --------- | -------- |
| 3 | 4      | none      | 1        |
| 2 | 2      | none      | 2        |
| 1 | 1      | none      | 3        |
| 0 | 3      | 1         | 1        |

---

### Step 3: Contribution

| i | value | left | right | contribution |
| - | ----- | ---- | ----- | ------------ |
| 0 | 3     | 1    | 1     | 3            |
| 1 | 1     | 2    | 3     | 6            |
| 2 | 2     | 1    | 2     | 4            |
| 3 | 4     | 1    | 1     | 4            |

```
Total = 17 ✅
```

---

## 🔟 Java Implementation (Interview-Ready)

```java
class Solution {
    public int sumSubarrayMins(int[] arr) {
        int n = arr.length;
        int MOD = 1_000_000_007;

        int[] left = new int[n];
        int[] right = new int[n];

        Stack<Integer> stack = new Stack<>();

        // Previous Smaller (strict)
        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && arr[stack.peek()] > arr[i]) {
                stack.pop();
            }
            left[i] = stack.isEmpty() ? i + 1 : i - stack.peek();
            stack.push(i);
        }

        stack.clear();

        // Next Smaller or Equal
        for (int i = n - 1; i >= 0; i--) {
            while (!stack.isEmpty() && arr[stack.peek()] >= arr[i]) {
                stack.pop();
            }
            right[i] = stack.isEmpty() ? n - i : stack.peek() - i;
            stack.push(i);
        }

        long result = 0;
        for (int i = 0; i < n; i++) {
            result = (result + (long) arr[i] * left[i] * right[i]) % MOD;
        }

        return (int) result;
    }
}
```

---

## 1️⃣1️⃣ How to IDENTIFY this pattern in interviews

### Cues that scream “Monotonic Stack”:

* “Sum of minimums / maximums of subarrays”
* “Contribution of each element”
* “Range until smaller/larger element”
* “Contiguous subarrays”

---

## 1️⃣2️⃣ Pattern Classification

| Problem                  | Stack Type |
| ------------------------ | ---------- |
| Sum of Subarray Minimums | Increasing |
| Sum of Subarray Maximums | Decreasing |
| Largest Rectangle        | Increasing |
| Trapping Rain Water      | Decreasing |

---

## 🧠 One-line Memory Hook

> **“Count how many subarrays each element dominates as the minimum.”**

---
