# ✅ When should you think “Binary Search on Answer”?

## 🎯 Cue 1: The problem says

> “minimize the maximum”
> or
> “maximize the minimum”

This is the #1 giveaway.

Examples:

* **minimize the largest subarray sum** ✅ (this problem)
* minimize maximum distance
* maximize minimum sweetness
* minimize maximum pages assigned
* maximize minimum capacity

Whenever you see:

### **minimize the maximum**

or

### **maximize the minimum**

Immediately suspect BSOA.

---

# ✅ Cue 2: The answer is numeric and has a range

Here answer is:

* some integer sum value

And you can easily compute bounds:

* **low = max(nums)**
* **high = sum(nums)**

If you can define a clear numeric search space → BSOA is likely.

---

# ✅ Cue 3: You can check feasibility for a given value X

Ask yourself:

> If I guess the answer is X, can I verify if it's possible?

In this problem:

* Guess max allowed subarray sum = X
* Can we split into ≤ k subarrays?

That check is easy in **O(n)** using greedy.

If feasibility check exists → BSOA candidate.

---

# ✅ Cue 4: Monotonic property exists (MOST IMPORTANT)

Meaning:

### If X works, then X+1 also works.

Here:

* If you can split with max sum 15,
* then you can definitely split with max sum 16.

So feasible region looks like:

```
false false false true true true true
```

This “first true” pattern screams binary search.

---

# 🧠 How to detect monotonicity quickly?

Ask:

### If I loosen the constraint, does it become easier?

If yes → monotonic.

In this problem:

* Larger max sum allowed → easier to split.

So monotonic exists.

---

# ✅ Cue 5: Greedy naturally fits feasibility

Many BSOA problems have this structure:

* Binary search picks candidate X
* Greedy tells if X possible

Examples:

* shipping packages within D days
* allocate books
* aggressive cows
* split array largest sum

---

# 🔥 Quick mental checklist (memorize)

When you see an optimization problem, ask:

### 1) Is the answer numeric?

### 2) Can I compute low/high bounds?

### 3) If I guess an answer X, can I check validity fast?

### 4) If X works, does X+1 always work?

If all 4 are YES → **Binary Search on Answer**

---

# 🎯 Applied to this problem

We want:

> minimize the maximum subarray sum

So guess:

> maximum sum = X

Check:

> can we split into at most k subarrays?

Monotonic:

* X small → impossible
* X big → possible

So binary search.

---

# ⚡ Interview one-liner

> “This is a minimize-the-maximum problem with a monotonic feasibility check, so we binary search the answer.”

That line is gold.

---

---

# ✅ 20 Must-Know Binary Search on Answer Problems

## ⭐ Group A: “Minimize the Maximum” (Most common)

### 1) Split Array Largest Sum (LC 410)

* **Search**: maximum allowed subarray sum `X`
* **Check**: can split into ≤ k subarrays?

---

### 2) Capacity To Ship Packages Within D Days (LC 1011)

* **Search**: ship capacity `X`
* **Check**: can ship within D days?

---

### 3) Divide Chocolate / Maximize Sweetness (LC 1231)

* **Search**: minimum sweetness per person `X`
* **Check**: can we cut into ≥ k+1 pieces each ≥ X?

---

### 4) Allocate Minimum Number of Pages (GFG classic)

* **Search**: max pages assigned to one student `X`
* **Check**: can allocate within k students?

---

### 5) Painter’s Partition Problem (classic)

* **Search**: max time per painter `X`
* **Check**: can paint with k painters?

---

### 6) Minimize Maximum Distance to Gas Station (LC 774)

* **Search**: max allowed distance between stations `X`
* **Check**: how many new stations needed?

---

### 7) Koko Eating Bananas (LC 875)

* **Search**: eating speed `X`
* **Check**: can finish within H hours?

---

### 8) Minimum Time to Repair Cars (LC 2594)

* **Search**: time `X`
* **Check**: how many cars can be repaired in X time?

---

### 9) Minimum Number of Days to Make m Bouquets (LC 1482)

* **Search**: minimum days `X`
* **Check**: can make m bouquets?

---

### 10) Minimum Time to Complete Trips (LC 2187)

* **Search**: time `X`
* **Check**: total trips completed in X time?

---

---

## ⭐ Group B: “Maximize the Minimum” (Second most common)

### 11) Aggressive Cows (classic)

* **Search**: minimum distance between cows `X`
* **Check**: can place k cows with spacing ≥ X?

---

### 12) Magnetic Force Between Two Balls (LC 1552)

* **Search**: minimum force/distance `X`
* **Check**: can place m balls?

---

### 13) Maximum Tastiness of Candy Basket (LC 2517)

* **Search**: minimum difference `X`
* **Check**: can choose k candies with gap ≥ X?

---

### 14) Maximum Minimum Path (LC 1102)

* **Search**: minimum value threshold `X`
* **Check**: is there a path where all cells ≥ X?

---

### 15) Path With Minimum Effort (LC 1631)

* **Search**: maximum effort allowed `X`
* **Check**: can reach end if edges diff ≤ X?

---

---

## ⭐ Group C: “Find smallest X such that condition becomes true”

### 16) Find Minimum in Rotated Sorted Array II (LC 154)

* **Search**: pivot location indirectly
* **Check**: monotonic half selection

*(Not exactly answer search, but same monotonic reasoning)*

---

### 17) Find the Smallest Divisor Given a Threshold (LC 1283)

* **Search**: divisor `X`
* **Check**: sum(ceil(nums[i]/X)) ≤ threshold?

---

### 18) Minimize the Maximum Product Distribution (LC 2064)

* **Search**: max products per store `X`
* **Check**: stores needed ≤ n?

---

### 19) Smallest Rectangle Enclosing Black Pixels (LC 302)

* **Search**: boundary indices (rows/cols)
* **Check**: does row/col contain black pixel?

*(Binary search on boundaries)*

---

### 20) Search a 2D Matrix II variant (binary search on answer threshold)

* **Search**: kth smallest element `X`
* **Check**: count ≤ X in matrix

(Leads to:)

✅ **Kth Smallest in Sorted Matrix (LC 378)**

* Search answer X
* Check count of elements ≤ X

---

# 🔥 Super Pattern Recognition Table (memorize)

| Problem Statement Phrase | You Search      | Feasibility Check           |
| ------------------------ | --------------- | --------------------------- |
| minimize maximum         | maximum limit X | can we do it under X?       |
| maximize minimum         | minimum value X | can we achieve at least X?  |
| minimum time             | time X          | how much can be done by X?  |
| minimum capacity         | capacity X      | can finish with X?          |
| smallest divisor         | divisor X       | sum constraint ≤ threshold? |
| kth smallest             | value X         | count ≤ X?                  |

---

# 🧠 The 3 Strongest Cues (Fast detection)

### ✅ Cue #1

**“Minimize the maximum”** / **“maximize the minimum”**

### ✅ Cue #2

Answer lies in a numeric range `[low..high]`

### ✅ Cue #3

You can write a function:

```java
boolean feasible(X)
```

and feasible(X) is monotonic.

---

# 🎯 Interview-ready one-liner

> “This is an optimization problem with a monotonic feasibility condition, so binary search the answer.”

---


---

# ✅ Template 1: Minimize the Maximum (find FIRST TRUE)

### Pattern:

> Find minimum `X` such that `feasible(X) == true`

```java
class BinarySearchOnAnswerTemplate {

    public int solve(int[] nums) {

        int low = getLowerBound(nums);   // minimum possible answer
        int high = getUpperBound(nums);  // maximum possible answer

        while (low < high) {
            int mid = low + (high - low) / 2;

            if (feasible(nums, mid)) {
                // mid works, try smaller answer
                high = mid;
            } else {
                // mid doesn't work, need bigger answer
                low = mid + 1;
            }
        }

        return low; // low == high = minimum feasible answer
    }

    /**
     * Returns true if we can solve the problem with answer <= candidate.
     */
    private boolean feasible(int[] nums, int candidate) {
        // Implement problem-specific check
        return true;
    }

    private int getLowerBound(int[] nums) {
        // Example for "minimize max":
        // often max(nums)
        return 0;
    }

    private int getUpperBound(int[] nums) {
        // Example:
        // often sum(nums)
        return 0;
    }
}
```

📌 Used in:

* Split Array Largest Sum
* Ship Packages
* Koko Eating Bananas
* Minimum days to make bouquets
* Minimum time problems

---

# ✅ Template 2: Maximize the Minimum (find LAST TRUE)

### Pattern:

> Find maximum `X` such that `feasible(X) == true`

⚠️ Here we must bias mid upward to avoid infinite loop.

```java
class BinarySearchOnAnswerTemplate {

    public int solve(int[] nums) {

        int low = getLowerBound(nums);
        int high = getUpperBound(nums);

        while (low < high) {
            int mid = low + (high - low + 1) / 2; // bias upward

            if (feasible(nums, mid)) {
                // mid works, try bigger answer
                low = mid;
            } else {
                // mid doesn't work, try smaller
                high = mid - 1;
            }
        }

        return low; // maximum feasible answer
    }

    private boolean feasible(int[] nums, int candidate) {
        return true;
    }

    private int getLowerBound(int[] nums) {
        return 0;
    }

    private int getUpperBound(int[] nums) {
        return 0;
    }
}
```

📌 Used in:

* Aggressive cows
* Magnetic force between balls
* Maximize minimum sweetness
* Maximum tastiness

---

# ✅ Template 3: Generic LONG version (safest)

Many problems overflow `int` (`sum`, `time` etc).
Use this always in interviews unless you’re 100% sure.

```java
class BinarySearchOnAnswerTemplate {

    public long solve(int[] nums) {

        long low = getLowerBound(nums);
        long high = getUpperBound(nums);

        while (low < high) {
            long mid = low + (high - low) / 2;

            if (feasible(nums, mid)) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }

        return low;
    }

    private boolean feasible(int[] nums, long candidate) {
        return true;
    }

    private long getLowerBound(int[] nums) {
        return 0;
    }

    private long getUpperBound(int[] nums) {
        return 0;
    }
}
```

---

# 🧠 Universal Interview Rule

### If the question says:

✅ minimize maximum → use **FIRST TRUE template**
✅ maximize minimum → use **LAST TRUE template**

---

# 🔥 Common Bounds Cheat Sheet

### Minimize maximum subarray sum

```java
low = max(nums)
high = sum(nums)
```

### Minimum time problems

```java
low = 1
high = maxTimePossible
```

### Maximize minimum distance

```java
low = 0
high = maxDistance
```

---

