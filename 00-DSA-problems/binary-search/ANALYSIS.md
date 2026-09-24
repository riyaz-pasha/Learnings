> **Recognizing when a problem can be converted into a binary search problem**

---

## 1️⃣ Core Mental Model (Most Important)

### Binary Search applies when:

> **The answer space is ordered AND a yes/no (or monotonic) condition exists**

That’s it.

Most “hard” binary search problems are **NOT** about searching an array directly.

Instead:

* You search on **answers**
* You use a **predicate function**: `isPossible(x)`
* That function is **monotonic**

```
false false false true true true
```

Once this exists → Binary Search 🚀

---

## 2️⃣ The 4 Big Binary Search Patterns

### **Pattern 1: Classic Search on Sorted Data**

**Cue:**

* Sorted array / rotated array
* “First / last occurrence”
* “Lower bound / upper bound”

**Examples:**

* First & Last Position of Element
* Search in Rotated Sorted Array
* Find Minimum in Rotated Array
* Count of occurrences

**Key signal words:**

* *sorted*
* *index*
* *occurrence*
* *range*

👉 You already know these — these are **entry-level** BS problems.

---

### **Pattern 2: Binary Search on Answer (Most Important for Interviews)**

**Cue:**

* Problem asks for **minimum / maximum value**
* Brute force would try all values
* You can check feasibility for a given value

**Structure:**

```
min X such that condition(X) is true
max X such that condition(X) is true
```

**Examples:**

* Koko Eating Bananas
* Ship Packages in D Days
* Split Array Largest Sum
* Aggressive Cows
* Minimize Max Distance to Gas Station

**How to detect:**
Ask yourself:

> “If I fix an answer `X`, can I verify in O(n)?”

If yes → Binary Search on X.

**Monotonicity example (Koko):**

```
speed = 1   -> too slow ❌
speed = 5   -> too slow ❌
speed = 8   -> works ✅
speed = 10  -> works ✅
```

---

### **Pattern 3: Partition / Allocation Problems**

**Cue:**

* Divide something into `k` parts
* Minimize maximum / maximize minimum
* Constraints like “at most”, “at least”

**Common words:**

* partition
* allocate
* split
* distribute
* painters / students / books

**Examples:**

* Allocate Minimum Number of Pages
* Painter’s Partition
* Split Array Largest Sum
* Chocolate Distribution

**Why BS works:**

* Guess max workload `X`
* Check if partition is possible within `k`

```
X too small → impossible
X large enough → possible
```

Classic monotonic behavior.

---

### **Pattern 4: Optimization with Constraints (Hidden Binary Search)**

These look **NOT like binary search** at first.

**Cue:**

* “Minimum time”, “maximum distance”, “least capacity”
* Involves time, speed, capacity, distance
* Greedy + feasibility check

**Examples:**

* Minimum Time to Complete Trips
* Minimum Speed to Arrive on Time
* Capacity to Ship Packages
* Maximum Running Time of Computers

**Signal phrase:**

> “Find the minimum X such that…”

If you see this → STOP → Think Binary Search.

---

## 3️⃣ Advanced Binary Search Variants

### 🔹 Binary Search on Floating Point

**Cue:**

* Answer is decimal
* Precision is mentioned

**Examples:**

* Find square root
* Minimize max distance (gas station)
* Median of two sorted arrays (conceptually)

Binary search until `hi - lo < epsilon`.

---

### 🔹 Binary Search on Implicit Space

You’re not searching an array — you’re searching:

* time
* capacity
* distance
* value range

**Example:**

* Minimum Time to Make M Bouquets
* Time to repair cars

Array never sorted — **answer space is sorted**

---

## 4️⃣ Universal Detection Checklist (Very Important)

When reading a problem, ask these **5 questions**:

1. **Is the answer numeric?**
2. **Is there a range for the answer?** (low → high)
3. **Can I check validity for a fixed value?**
4. **Does validity change monotonically?**
5. **Brute force would be too slow?**

If **YES to all** → Binary Search.

---

## 5️⃣ Typical Traps (Interview Favorite)

### ❌ Mistake 1: Binary searching when monotonicity doesn’t exist

If `true → false → true` → ❌ BS not valid

---

### ❌ Mistake 2: Wrong boundaries

Always think:

```
low = minimum possible answer
high = maximum possible answer
```

Example:

* Koko: `low = 1`, `high = max(piles)`
* Ship: `low = max(weights)`, `high = sum(weights)`

---

### ❌ Mistake 3: Off-by-one in feasibility

Binary search code is easy
**Feasibility logic is the real challenge**

---

## 6️⃣ How Google Expects You to Think (Important for You)

Given your Google prep:

They expect you to say:

> “This is a binary search on answer problem because the feasibility function is monotonic.”

Not:

> “This looks like Koko Eating Bananas.”

Always **justify monotonicity verbally**.

---

## 7️⃣ Must-Solve Binary Search Problem List (Interview Grade)

### Tier 1 (Foundation)

* First/Last Position
* Search in Rotated Sorted Array
* Lower Bound / Upper Bound

### Tier 2 (Answer Space BS)

* Koko Eating Bananas
* Ship Packages in D Days
* Minimum Speed to Arrive on Time
* Split Array Largest Sum

### Tier 3 (Google-Level)

* Minimum Time to Complete Trips
* Aggressive Cows
* Maximum Running Time of N Computers
* Median of Two Sorted Arrays

---

## 8️⃣ Final Mental Shortcut (Gold Rule)

> If the problem says **minimum / maximum**
> and you can **check feasibility**
> → **Binary Search is probably intended**

---


---

# The Root Cause of Confusion (Important)

You’re mixing **two different binary search models** without realizing it:

1. **Binary search that tracks a valid answer explicitly**
2. **Binary search that converges boundaries to the answer**

Both are correct — but **they require different mental contracts**.

Once you commit to **one model per problem**, the confusion stops.

---

# Model 1️⃣: “Answer-Tracking” Binary Search (Safest, Most Intuitive)

### Idea:

> “Whenever I see a valid solution, I store it.”

### Template:

```java
while (low <= high) {
    mid = ...
    if (isValid(mid)) {
        ans = mid;          // save answer
        high = mid - 1;     // or low = mid + 1 depending on min/max
    } else {
        low = mid + 1;      // or high = mid - 1
    }
}
return ans;
```

---

## When SHOULD you use `ans`?

### ✅ Use `ans` when:

* You are **not 100% confident** about boundary correctness
* You are finding **minimum / maximum feasible value**
* You want **clarity over cleverness** (Google prefers this)

### Example:

Koko Eating Bananas
You want **minimum feasible speed**

```
valid: false false false true true true
```

So:

* When `mid` is valid → store it
* Keep shrinking toward minimum

👉 `ans` always holds the **best known valid answer**

---

## Why this always works

Because:

* You only assign `ans` when solution is valid
* You never lose the correct answer
* Boundaries can cross without fear

💡 **This is the model I strongly recommend for you**

---

# Model 2️⃣: “Boundary-Converging” Binary Search (No `ans`)

This is where confusion usually comes from.

### Idea:

> “I’ll design my loop so that low or high *must* end up at the answer.”

### Template (Lower Bound style):

```java
while (low < high) {
    mid = low + (high - low) / 2;
    if (condition(mid)) {
        high = mid;
    } else {
        low = mid + 1;
    }
}
return low; // or high
```

---

## When can you rely on `low` or `high`?

### Only when ALL of these are true:

1. You **define** what `low` and `high` mean
2. Your loop invariant is correct
3. The answer **cannot be lost**
4. You use `low < high`, not `low <= high`

If even **one** of these is shaky → don’t use this model.

---

# The Single Most Important Concept: Loop Invariant

Before writing code, you must say **out loud**:

> “At every point in the loop, what does `low` represent and what does `high` represent?”

---

## Example: Lower Bound (First `true`)

```
false false false true true true
```

Invariant:

* `low` → first possible index where answer *can be*
* `high` → also possible answer

Eventually:

```
low == high == first true
```

So returning `low` is safe.

---

# When to Return `low` vs `high` (Golden Rules)

## ✅ Return `low` when:

* You are searching for **first valid / minimum value**
* Your loop condition is `while (low < high)`
* You do `high = mid` on valid

### Examples:

* Lower bound
* First `true`
* Minimum feasible answer

---

## ✅ Return `high` when:

* You are searching for **last valid / maximum value**
* Your loop ensures `high` is always valid

### Example:

```
true true true false false
```

In this case:

* `high` converges to last true

But this is rarer and more error-prone.

---

# Why `low <= high` + no `ans` is dangerous

This is the **number one source of bugs**.

```java
while (low <= high) {
    ...
}
return low; // ❌ ambiguous
```

Why?

* `low` may overshoot
* `high` may undershoot
* No guarantee either is valid

👉 If you use `low <= high`, **always track `ans`**

---

# Decision Table (Bookmark This)

| Situation          | Use `ans`? | Return          |
| ------------------ | ---------- | --------------- |
| Min feasible value | ✅ Yes      | `ans`           |
| Max feasible value | ✅ Yes      | `ans`           |
| Lower bound        | ❌ Optional | `low`           |
| Upper bound        | ❌ Optional | `low - 1`       |
| Unsure / interview | ✅ Yes      | `ans`           |
| `low <= high` loop | ✅ Yes      | `ans`           |
| `low < high` loop  | ❌ No       | `low` or `high` |

---

# Google Interview Recommendation (Important)

At Google, **clarity beats cleverness**.

I would explicitly say:

> “I’m using an answer variable to avoid boundary ambiguity.”

That’s a **positive signal**, not a weakness.

---

# Final Mental Rule (This will remove confusion)

> 🔥 **If you ever ask yourself “should I return low or high?” → you should be using `ans`.**

No exceptions.

---

---
I’ll give you **one universal binary search template** that:

* Works for **90–95% of BS problems**
* Removes **low/high/ans confusion**
* Is **Google-interview safe**
* Matches how you already think

We’ll also cover **why it works**, **how to adapt it**, and **when not to use it**.

---

# ✅ The Universal Binary Search Template (Answer-Tracking)

### 🔑 Core idea

> **Binary search on the answer space + explicit feasibility check + store best answer**

This eliminates all ambiguity.

---

## 🧩 Universal Template (Java)

```java
int low = MIN_POSSIBLE;
int high = MAX_POSSIBLE;
int ans = DEFAULT_INVALID; // usually -1 or high

while (low <= high) {
    int mid = low + (high - low) / 2;

    if (isValid(mid)) {
        ans = mid;          // mid is a valid answer
        high = mid - 1;     // shrink towards minimum
        // OR low = mid + 1 for maximum problems
    } else {
        low = mid + 1;      // need bigger value
        // OR high = mid - 1 depending on problem
    }
}

return ans;
```

---

# 🧠 How to Customize This Template (Only 3 Decisions)

Every binary search problem boils down to answering **three questions**.

---

## 1️⃣ What am I optimizing?

Choose ONE:

* ✅ **Minimum feasible value**
* ✅ **Maximum feasible value**

---

## 2️⃣ How do I move boundaries?

### A. Minimum Feasible Value

```
false false false true true true
```

```java
if (isValid(mid)) {
    ans = mid;
    high = mid - 1;
} else {
    low = mid + 1;
}
```

📌 Examples:

* Koko Eating Bananas
* Ship Packages in D Days
* Minimum Time to Complete Trips
* Lower bound (value-based)

---

### B. Maximum Feasible Value

```
true true true false false
```

```java
if (isValid(mid)) {
    ans = mid;
    low = mid + 1;
} else {
    high = mid - 1;
}
```

📌 Examples:

* Aggressive Cows (max min distance)
* Maximum Running Time of Computers
* Capacity maximization

---

## 3️⃣ What are `low` and `high`?

### 🔹 Always define them as:

```
low  = smallest possible answer
high = largest possible answer
```

Never use array indices blindly.

---

# 🧪 Example 1: Koko Eating Bananas (Minimum)

```java
low = 1;
high = maxPile;
ans = -1;

if (isValid(mid)) {
    ans = mid;
    high = mid - 1;
} else {
    low = mid + 1;
}
```

Return `ans`.

---

# 🧪 Example 2: Aggressive Cows (Maximum)

```java
low = 1;
high = maxDistance;
ans = -1;

if (canPlaceCows(mid)) {
    ans = mid;
    low = mid + 1;
} else {
    high = mid - 1;
}
```

Return `ans`.

---

# 🧪 Example 3: Lower Bound (Using Universal Template)

Find **first index where `arr[i] >= target`**

```java
low = 0;
high = n - 1;
ans = -1;

if (arr[mid] >= target) {
    ans = mid;
    high = mid - 1;
} else {
    low = mid + 1;
}
```

Yes — even classical BS fits this template.

---

# ❌ What This Template Avoids Completely

* ❌ “Return low or high?”
* ❌ `low < high` vs `low <= high` confusion
* ❌ Off-by-one disasters
* ❌ Losing the answer when bounds cross

---

# 🏆 Why This Is Interview-Grade

You can say:

> “I’m using a binary search on answer with an explicit feasibility check and tracking the best valid solution.”

That’s **exactly what Google expects**.

---

# 🚦 When NOT to Use This Template

Only avoid it when:

* You are **100% sure** the problem is pure lower/upper bound
* You want ultra-compact code (CP, not interviews)

Even then, this template still works — it’s just slightly longer.

---

# 🧠 One-Line Mental Rule (Memorize This)

> 🔥 **Binary search = optimize answer + check feasibility + store best result**

---

# 📌 Your Final Reusable Checklist

Before coding, write this on paper:

1. What am I minimizing / maximizing?
2. What is the answer range?
3. What does `isValid(x)` mean?
4. When valid → move left or right?
5. Store `ans` when valid

If you can answer these, you will **never mess up BS again**.

---
