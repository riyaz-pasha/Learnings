Let's break down the **time and space complexities** of the final optimized solution step by step.

---

## ✅ Problem Recap

Given a list of rectangles (with possible overlaps), we want to find a **vertical line `x = value`** that divides the **total union area** of these rectangles into **two equal parts**.

---

## 📦 Definitions

Let:

* `r = number of rectangles`
* `k = number of unique x-coordinates` (at most `2r` because each rectangle contributes 2 x-edges)

---

## ⏱ Time Complexity

### **Step 1: Collect Unique x-coordinates**

```java
for (int[] rect : rectangles) {
    xSet.add(rect[0]);
    xSet.add(rect[2]);
}
```

* `O(r)` inserts → HashSet insertion: `O(1)`
* Sorting: `O(k log k)`
* ⏱ **Time: `O(r + k log k)`**

---

### **Step 2: Map x-values to indices**

```java
for (int i = 0; i < n; i++) {
    xIndexMap.put(xList.get(i), i);
}
```

* `O(k)` operations → `O(k)`
* ⏱ **Time: `O(k)`**

---

### **Step 3: Populate Vertical Strips**

```java
for each rectangle:
    loop from leftIndex to rightIndex:
        add y-interval to strip
```

* Worst-case, a rectangle spans **many x-intervals**
* In total, there are at most `2r` edges and thus `O(r)` vertical strips
* So total number of `Event` insertions ≈ **sum of all widths across rectangles** = O(r) to O(r \* k)
* In practice: each rectangle covers only a small number of strips
* ⏱ **Time: `O(r * avgStripsPerRect)` → worst-case: `O(r * k)`**

---

### **Step 4: Compute Area of Each Strip**

```java
for each strip:
    merge y-intervals
    compute area
```

Let `s = total number of y-intervals (Events)` = O(r \* k)

* Sorting y-intervals per strip: `O(m log m)` where `m = #intervals in that strip`
* Total time over all strips: ≈ `O(s log m)` where `m` is small (e.g., constant or log-scale)
* ⏱ **Time: `O(r log r)` to `O(rk log r)`** depending on density

---

### **Step 5: Accumulate Areas & Interpolate**

```java
for i = 0 to n-1:
    accumulate area and interpolate to find x
```

* One pass through `O(k)` values
* ⏱ **Time: `O(k)`**

---

### ✅ Total Time Complexity Summary

| Step                      | Time Complexity               |
| ------------------------- | ----------------------------- |
| Unique x-collection       | `O(r + k log k)`              |
| x-index map creation      | `O(k)`                        |
| Sweep-strip preparation   | `O(r * k)` worst-case         |
| Y-interval merging        | `O(r log r)` \~ `O(rk log r)` |
| Area accumulation/interp. | `O(k)`                        |
| **Total**                 | ✅ `O(rk log r)` worst-case    |

> In most practical cases: **`O(r log r + k log k)`**

---

## 🧠 Space Complexity

### **xList, xSet, xIndexMap**

* `O(k)` storage for unique x-coordinates

### **Sweep (verticalStrips)**

* `O(k)` strips
* Each contains a list of events (total ≈ O(rk) worst-case)

### **Area array**

* `O(k)` size

### **Merged y-intervals**

* Temporary list per strip: O(r) worst-case, but discarded after use

---

### ✅ Total Space Complexity

| Component     | Space                |
| ------------- | -------------------- |
| x-coordinates | `O(k)`               |
| xIndexMap     | `O(k)`               |
| Event strips  | `O(rk)`              |
| Area array    | `O(k)`               |
| **Total**     | ✅ `O(rk)` worst-case |

---

## 🧠 TL;DR Summary

| Complexity Type     | Big-O         |
| ------------------- | ------------- |
| ⏱ Time (worst-case) | `O(rk log r)` |
| 💾 Space            | `O(rk)`       |

* Where:

  * `r = number of rectangles`
  * `k = number of unique x-coordinates ≤ 2r`

