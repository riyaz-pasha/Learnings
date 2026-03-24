# Binary Search — Complete Interview Guide

> **One template. Every variant. Zero confusion.**

---

## The Universal Template

Before anything else, burn this into memory. Every single problem below is a specialization of this:

```java
int lo = 0, hi = <upper_bound>;
int ans = <safe_default>;           // never return lo or hi directly

while (lo <= hi) {
    int mid = lo + (hi - lo) / 2;  // prevents integer overflow (never use (lo+hi)/2)

    if (<condition is true>) {
        ans = mid;                  // capture a valid answer
        <shrink toward the answer>; // hi = mid - 1  OR  lo = mid + 1
    } else if (<too small>) {
        lo = mid + 1;
    } else {
        hi = mid - 1;
    }
}
return ans;
```

### Why `ans` instead of returning `lo`/`hi`?

Returning `lo` or `hi` at the end is a common pattern, but it causes three problems:
1. You have to re-derive *which* pointer holds the answer — it changes per variant.
2. Off-by-one errors creep in because the loop exits with `lo > hi`.
3. Edge cases (empty array, target not present) need extra guards.

With `ans`:
- Set a safe default before the loop (`-1`, `0`, `n`, `hi`).
- Update `ans` whenever you find a valid candidate.
- The loop ends, return `ans`. Done.

---

## Part 1 — Index-Space Binary Search

You're searching an array. `lo` and `hi` are indices.

---

### Variant 1 — Classic Search (exact match)

**Use when:** Find the index of a specific value. Return `-1` if absent.

**The clue:** The problem says "find", "search", "does it exist".

```java
public int search(int[] nums, int target) {
    int lo = 0, hi = nums.length - 1;
    int ans = -1;

    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (nums[mid] == target) {
            ans = mid;
            break;                  // exact match — stop immediately
        } else if (nums[mid] < target) {
            lo = mid + 1;
        } else {
            hi = mid - 1;
        }
    }
    return ans;
}
```

**Mental model:** Classic left-right squeeze. Hit the target, stop.

---

### Variant 2 — First Occurrence (leftmost match)

**Use when:** Find the *first* index where `nums[i] == target` in a sorted array with duplicates.

**The clue:** "first position", "leftmost index", "lower bound of exact value".

**Key shift from Variant 1:** On a match, do **not** stop. Record `ans`, then push left (`hi = mid - 1`) to see if an earlier occurrence exists.

```java
public int firstOccurrence(int[] nums, int target) {
    int lo = 0, hi = nums.length - 1;
    int ans = -1;

    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (nums[mid] == target) {
            ans = mid;
            hi = mid - 1;           // could there be an earlier one? keep looking left
        } else if (nums[mid] < target) {
            lo = mid + 1;
        } else {
            hi = mid - 1;
        }
    }
    return ans;
}
```

**Mental model:** "I found it, but I'm greedy — I want the *first* one."

---

### Variant 3 — Last Occurrence (rightmost match)

**Use when:** Find the *last* index where `nums[i] == target`.

**The clue:** "last position", "rightmost index".

**Key shift:** On a match, push right (`lo = mid + 1`) instead of left.

```java
public int lastOccurrence(int[] nums, int target) {
    int lo = 0, hi = nums.length - 1;
    int ans = -1;

    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (nums[mid] == target) {
            ans = mid;
            lo = mid + 1;           // could there be a later one? keep looking right
        } else if (nums[mid] < target) {
            lo = mid + 1;
        } else {
            hi = mid - 1;
        }
    }
    return ans;
}
```

**Mental model:** Mirror image of Variant 2. Same code, just flip the direction on match.

---

### Variant 4 — Count Occurrences

**Use when:** How many times does target appear?

**The clue:** "count", "frequency", "how many times".

**Don't reinvent the wheel.** This is just `lastOccurrence - firstOccurrence + 1`.

```java
public int countOccurrences(int[] nums, int target) {
    int first = firstOccurrence(nums, target);
    if (first == -1) return 0;
    int last = lastOccurrence(nums, target);
    return last - first + 1;
}
```

---

### Variant 5 — Lower Bound (search insert / first ≥ target)

**Use when:** Find the first index where `nums[i] >= target`. If not present, where would it be inserted?

**The clue:** "search insert position", "lower bound", "first element not less than".

**Key insight:** You're no longer looking for an *exact* match. The condition is `nums[mid] >= target`. On success, record and go left.

```java
public int lowerBound(int[] nums, int target) {
    int lo = 0, hi = nums.length - 1;
    int ans = nums.length;          // default: target is larger than everything

    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (nums[mid] >= target) {
            ans = mid;
            hi = mid - 1;
        } else {
            lo = mid + 1;
        }
    }
    return ans;
}
```

**Mental model:** "First index where I'm no longer too small."

---

### Variant 6 — Upper Bound (first > target)

**Use when:** Find the first index where `nums[i] > target`.

**The clue:** "upper bound", "first element strictly greater than".

**Key insight:** Change `>=` to `>`. Same skeleton.

```java
public int upperBound(int[] nums, int target) {
    int lo = 0, hi = nums.length - 1;
    int ans = nums.length;          // default: nothing is strictly greater

    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (nums[mid] > target) {
            ans = mid;
            hi = mid - 1;
        } else {
            lo = mid + 1;
        }
    }
    return ans;
}
```

**Bonus:** `lowerBound` and `upperBound` together give you `countOccurrences` as `upper - lower`.

---

### Variant 5 vs 6 — Side-by-side comparison

| | Lower Bound | Upper Bound |
|---|---|---|
| Condition | `nums[mid] >= target` | `nums[mid] > target` |
| Returns | First index of target (or insert pos) | First index after target |
| Default `ans` | `nums.length` | `nums.length` |
| Count formula | — | `upper - lower` |

---

### Variant 7 — Search in Rotated Sorted Array

**Use when:** A sorted array was rotated at some unknown pivot. Find a target.

**The clue:** "rotated", "sorted but shifted", "there was a pivot".

**Key insight:** After rotation, one half is always cleanly sorted. Ask:
1. Which half is sorted? (`nums[lo] <= nums[mid]` → left is sorted)
2. Does target fall inside the sorted half?
3. If yes, go there. If no, go to the other half.

```java
public int searchRotated(int[] nums, int target) {
    int lo = 0, hi = nums.length - 1;
    int ans = -1;

    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (nums[mid] == target) {
            ans = mid;
            break;
        }
        if (nums[lo] <= nums[mid]) {            // left half is sorted
            if (nums[lo] <= target && target < nums[mid]) {
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        } else {                                 // right half is sorted
            if (nums[mid] < target && target <= nums[hi]) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
    }
    return ans;
}
```

**Interview pitfall:** The condition for the left half is `nums[lo] <= nums[mid]` (use `<=` not `<`), because when `lo == mid` the left half has one element and is trivially sorted.

---

### Variant 8 — Minimum in Rotated Sorted Array

**Use when:** Find the smallest value after rotation (no duplicates).

**The clue:** "find minimum", "rotated sorted", "pivot point".

**Key insight:** The minimum is always in the *unsorted* half. If the left half is sorted (`nums[lo] <= nums[mid]`), the minimum can't be inside it — record `nums[lo]` as a candidate and move right.

```java
public int findMin(int[] nums) {
    int lo = 0, hi = nums.length - 1;
    int ans = nums[0];

    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (nums[lo] <= nums[mid]) {            // left half is sorted
            ans = Math.min(ans, nums[lo]);       // nums[lo] is the minimum of this half
            lo = mid + 1;                        // minimum is in the right (unsorted) half
        } else {                                 // right half is sorted
            ans = Math.min(ans, nums[mid]);      // nums[mid] could be the global minimum
            hi = mid - 1;
        }
    }
    return ans;
}
```

---

### Variant 9 — Find Peak Element

**Use when:** Find any index `i` such that `nums[i] > nums[i-1]` and `nums[i] > nums[i+1]`.

**The clue:** "peak", "local maximum", "mountain top".

**Key insight:** Always move toward the larger neighbor. A peak must exist in that direction (the array "has" to come down somewhere).

```java
public int findPeakElement(int[] nums) {
    int lo = 0, hi = nums.length - 1;
    int ans = 0;

    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        boolean leftOk  = (mid == 0)               || nums[mid] > nums[mid - 1];
        boolean rightOk = (mid == nums.length - 1)  || nums[mid] > nums[mid + 1];

        if (leftOk && rightOk) {
            ans = mid;
            break;
        } else if (mid > 0 && nums[mid - 1] > nums[mid]) {
            hi = mid - 1;           // slope ascends to the left
        } else {
            lo = mid + 1;           // slope ascends to the right
        }
    }
    return ans;
}
```

---

### Variant 10 — Mountain Array Search

**Use when:** Find a target in a mountain array (strictly increases then strictly decreases).

**The clue:** "mountain array", "first increases then decreases".

**Three-step approach:**
1. Find the peak.
2. Binary search ascending half (normal comparators).
3. Binary search descending half (flipped comparators).

```java
public int findInMountainArray(int target, int[] mountain) {
    int peak = findPeak(mountain);

    int ans = binarySearchAsc(mountain, target, 0, peak);
    if (ans != -1) return ans;
    return binarySearchDesc(mountain, target, peak + 1, mountain.length - 1);
}

private int findPeak(int[] arr) {
    int lo = 0, hi = arr.length - 1, ans = 0;
    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (mid < arr.length - 1 && arr[mid] < arr[mid + 1]) { lo = mid + 1; }
        else { ans = mid; hi = mid - 1; }
    }
    return ans;
}

private int binarySearchAsc(int[] arr, int target, int lo, int hi) {
    int ans = -1;
    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (arr[mid] == target)     { ans = mid; break; }
        else if (arr[mid] < target) { lo = mid + 1; }
        else                        { hi = mid - 1; }
    }
    return ans;
}

private int binarySearchDesc(int[] arr, int target, int lo, int hi) {
    int ans = -1;
    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (arr[mid] == target)     { ans = mid; break; }
        else if (arr[mid] > target) { lo = mid + 1; } // reversed: array descends
        else                        { hi = mid - 1; }
    }
    return ans;
}
```

---

### Variant 11 — Next Greatest Letter (Circular)

**Use when:** In a sorted circular array of chars, find the smallest letter strictly greater than target.

**The clue:** "next greater letter", "circular wrap", "smallest char >".

```java
public char nextGreatestLetter(char[] letters, char target) {
    int lo = 0, hi = letters.length - 1;
    int ans = -1;

    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (letters[mid] > target) {
            ans = mid;
            hi = mid - 1;
        } else {
            lo = mid + 1;
        }
    }
    return ans == -1 ? letters[0] : letters[ans]; // wrap around
}
```

---

## Part 2 — Value-Space Binary Search

You're not searching an array index. You're searching for a numeric *answer* in a range `[lo..hi]`. For each candidate `mid`, ask: "Is this value *feasible*?" and narrow accordingly.

**Recognition pattern:** "Find the minimum X such that..." or "Find the maximum X such that..."

---

### Variant 12 — Integer Square Root

**Use when:** Find the floor of √x (largest integer k where k² ≤ x).

```java
public int mySqrt(int x) {
    if (x < 2) return x;
    int lo = 1, hi = x / 2;
    int ans = 1;

    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        long sq = (long) mid * mid;     // cast to long to avoid overflow
        if (sq == x) {
            ans = mid;
            break;
        } else if (sq < x) {
            ans = mid;                  // mid is a valid floor sqrt so far
            lo = mid + 1;
        } else {
            hi = mid - 1;
        }
    }
    return ans;
}
```

**Interview note:** Always cast to `long` before multiplying — `mid * mid` overflows `int` for large x.

---

### Variant 13 — First Bad Version

**Use when:** Given n versions, find the first bad one using `isBadVersion(v)` API.

**The clue:** "first bad", "leftmost true", essentially a "first occurrence" but with a boolean function.

```java
public int firstBadVersion(int n) {
    int lo = 1, hi = n;
    int ans = n;                        // default: assume the last is bad

    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (isBadVersion(mid)) {
            ans = mid;
            hi = mid - 1;               // mid is bad, but could there be an earlier one?
        } else {
            lo = mid + 1;
        }
    }
    return ans;
}
```

**Mental model:** This is Variant 2 (First Occurrence) applied to a boolean function instead of an array value.

---

### Variant 14 — Koko Eating Bananas

**Use when:** Minimize a rate/speed subject to a time/capacity constraint.

**The clue:** "minimum speed/rate", "finish in k hours/days", "feasibility check".

**Template:**
- Search space: `[1 .. max(piles)]`
- Feasibility: can Koko finish all piles at speed `mid` within `h` hours?
- On feasible: record `ans`, try smaller (`hi = mid - 1`)
- On infeasible: need more speed (`lo = mid + 1`)

```java
public int minEatingSpeed(int[] piles, int h) {
    int lo = 1, hi = 0;
    for (int p : piles) hi = Math.max(hi, p);
    int ans = hi;

    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (canFinish(piles, mid, h)) {
            ans = mid;
            hi = mid - 1;
        } else {
            lo = mid + 1;
        }
    }
    return ans;
}

private boolean canFinish(int[] piles, int speed, int h) {
    long hours = 0;
    for (int p : piles) hours += (p + speed - 1) / speed; // ceil division
    return hours <= h;
}
```

---

### Variant 15 — Ship Packages Within D Days

**Use when:** Minimize capacity given that all work must complete in a fixed number of steps.

**The clue:** "minimum capacity", "within D days", "fit everything in N passes".

**Template:**
- Search space: `[max(weights) .. sum(weights)]`
  - Lower bound: ship must carry at least the heaviest package.
  - Upper bound: ship everything in one day.
- Same feasibility-check skeleton as Variant 14.

```java
public int shipWithinDays(int[] weights, int days) {
    int lo = 0, hi = 0;
    for (int w : weights) { lo = Math.max(lo, w); hi += w; }
    int ans = hi;

    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (canShip(weights, mid, days)) {
            ans = mid;
            hi = mid - 1;
        } else {
            lo = mid + 1;
        }
    }
    return ans;
}

private boolean canShip(int[] weights, int cap, int days) {
    int usedDays = 1, load = 0;
    for (int w : weights) {
        if (load + w > cap) { usedDays++; load = 0; }
        load += w;
    }
    return usedDays <= days;
}
```

---

### Value-Space Problems — Generalised Template

All "minimize X such that condition holds" or "maximize X such that condition holds" follow this pattern:

```java
// --- MINIMIZE X (find smallest valid value) ---
int lo = minPossible, hi = maxPossible;
int ans = hi;                           // start with worst case

while (lo <= hi) {
    int mid = lo + (hi - lo) / 2;
    if (isFeasible(mid)) {
        ans = mid;                      // mid works, but try smaller
        hi = mid - 1;
    } else {
        lo = mid + 1;                   // mid doesn't work, need larger
    }
}
return ans;

// --- MAXIMIZE X (find largest valid value) ---
int lo = minPossible, hi = maxPossible;
int ans = lo;                           // start with worst case

while (lo <= hi) {
    int mid = lo + (hi - lo) / 2;
    if (isFeasible(mid)) {
        ans = mid;                      // mid works, but try larger
        lo = mid + 1;
    } else {
        hi = mid - 1;                   // mid doesn't work, need smaller
    }
}
return ans;
```

---

## Part 3 — Quick Recognition Guide

When you see these words in a problem, think binary search:

| Signal phrase | Variant |
|---|---|
| "sorted array", "find target" | Classic (V1) |
| "first/leftmost position of X" | First Occurrence (V2) |
| "last/rightmost position of X" | Last Occurrence (V3) |
| "count occurrences" | V2 + V3 combined |
| "insert position", "lower bound" | Lower Bound (V5) |
| "upper bound", "first element > X" | Upper Bound (V6) |
| "rotated sorted array", "find target" | Rotated Search (V7) |
| "rotated sorted array", "find min" | Rotated Min (V8) |
| "peak element", "local max" | Peak (V9) |
| "mountain array", "find target" | Mountain Search (V10) |
| "minimum speed/rate/capacity" | Value-Space Minimize (V14/V15) |
| "maximum X such that Y is true" | Value-Space Maximize |
| "feasibility check", "can we do it in k steps" | Value-Space |

---

## Part 4 — Interview Pitfall Checklist

Go through this before writing a single line of code in an interview.

### 1. Integer overflow in `mid`
```java
// ❌ WRONG — can overflow when lo + hi > Integer.MAX_VALUE
int mid = (lo + hi) / 2;

// ✅ CORRECT — always
int mid = lo + (hi - lo) / 2;
```

### 2. Integer overflow in value-space problems
```java
// ❌ WRONG — mid * mid can overflow for large x
if (mid * mid <= x)

// ✅ CORRECT — cast one operand to long
if ((long) mid * mid <= x)
```

### 3. Infinite loop — always move the pointer
```java
// ❌ WRONG — if condition is always true, lo stays stuck
while (lo <= hi) {
    int mid = ...;
    if (condition) { hi = mid; } // NOT mid-1, loop never shrinks
}

// ✅ CORRECT — always move by at least 1
if (condition) { hi = mid - 1; }
```

### 4. Off-by-one in loop condition
```java
// lo <= hi  →  inclusive on both ends (recommended — matches this guide)
// lo < hi   →  leaves one element unexamined (requires special handling after loop)

// Stick to lo <= hi with the ans-capture pattern — zero confusion.
```

### 5. Wrong condition for rotated array
```java
// ❌ WRONG — misses the lo == mid case (single-element left half)
if (nums[lo] < nums[mid])

// ✅ CORRECT
if (nums[lo] <= nums[mid])
```

### 6. Missing default for `ans`
Think about what the answer should be if the target is never found, or if no feasible value exists. Set that as the default *before* the loop.

| Problem type | Safe default |
|---|---|
| Find exact index | `-1` |
| Lower / upper bound | `nums.length` |
| Value-space minimize | `hi` (largest candidate) |
| Value-space maximize | `lo` (smallest candidate) |
| First bad version | `n` |

### 7. Descending array comparators
For the descending half of a mountain array:
```java
// Ascending: go right when arr[mid] < target
// Descending: go right when arr[mid] > target  ← reversed
```

### 8. Ceil division in feasibility checks
When simulating "how many steps does mid take":
```java
// ❌ WRONG — integer division truncates
hours += pile / speed;

// ✅ CORRECT — ceiling division without floating point
hours += (pile + speed - 1) / speed;
```

---

## Part 5 — Decision Tree

```
Is the input sorted (or can the search space be defined as a range)?
│
├── YES → Binary search applies.
│         │
│         ├── Searching an array?
│         │   │
│         │   ├── Is array fully sorted (no rotation)?
│         │   │   ├── Exact match only?      → Variant 1
│         │   │   ├── Leftmost occurrence?   → Variant 2
│         │   │   ├── Rightmost occurrence?  → Variant 3
│         │   │   ├── Count?                 → Variants 2 + 3
│         │   │   ├── First index >= target? → Variant 5 (lower bound)
│         │   │   └── First index > target?  → Variant 6 (upper bound)
│         │   │
│         │   ├── Array is rotated?
│         │   │   ├── Find a target?         → Variant 7
│         │   │   └── Find minimum?          → Variant 8
│         │   │
│         │   └── Array is a mountain?
│         │       ├── Find peak?             → Variant 9
│         │       └── Find target?           → Variant 10
│         │
│         └── Searching for a numeric answer (not an index)?
│             ├── "Minimum X such that condition"  → Value-space minimize
│             └── "Maximum X such that condition"  → Value-space maximize
│
└── NO → Binary search probably does not apply.
```

---

## Part 6 — Complexity Reference

| | Time | Space |
|---|---|---|
| All binary search variants | O(log n) | O(1) |
| Value-space search | O(log(range) × feasibility check) | O(1) |
| Count occurrences | O(log n) | O(1) |
| Mountain array search | O(log n) | O(1) |
| Koko / Ship packages | O(n log(max)) | O(1) |

---

## Part 7 — Patterns at a Glance

| Variant | On match, move | `ans` default |
|---|---|---|
| Classic search | `break` | `-1` |
| First occurrence | `hi = mid - 1` | `-1` |
| Last occurrence | `lo = mid + 1` | `-1` |
| Lower bound (≥ target) | `hi = mid - 1` | `nums.length` |
| Upper bound (> target) | `hi = mid - 1` | `nums.length` |
| Min in rotated | `lo = mid + 1` | `nums[0]` |
| Minimize (value-space) | `hi = mid - 1` | `hi` |
| Maximize (value-space) | `lo = mid + 1` | `lo` |
| First bad version | `hi = mid - 1` | `n` |

> If you always move toward the answer and always capture `ans` before moving,
> you will never have an off-by-one again.
