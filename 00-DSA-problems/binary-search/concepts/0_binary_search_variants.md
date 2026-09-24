# Binary Search — Complete Variants Guide

> **Goal:** Never confuse which template to use in an interview again.  
> Every variant here has a clear *trigger*, a *template*, and a *gotcha* list.

---

## Mental Model First

Binary search always narrows a search space `[low, high]` by asking:
> *"Does the answer lie in the left half or the right half?"*

The confusion comes from **three independent decisions** you make:

| Decision | Options |
|---|---|
| **Loop condition** | `low < high` vs `low <= high` |
| **Mid calculation** | `low + (high - low) / 2` vs `low + (high - low + 1) / 2` |
| **What to return** | `low`, `high`, `mid`, or `-1` |

Getting these wrong by even one character causes infinite loops or off-by-one errors. This guide gives you a deterministic way to choose.

---

## The 4 Canonical Templates

---

### Template 1 — Classic Search (Find Exact Value)

**Use when:** You need to find if an *exact target* exists. Return its index or `-1`.

```python
def binary_search(arr, target):
    low, high = 0, len(arr) - 1

    while low <= high:                          # ✅ <= because both ends are valid candidates
        mid = low + (high - low) // 2

        if arr[mid] == target:
            return mid                          # Found it
        elif arr[mid] < target:
            low = mid + 1                       # mid is NOT the answer, skip it
        else:
            high = mid - 1                      # mid is NOT the answer, skip it

    return -1                                   # Not found
```

**Rules:**
- Use `low <= high` → loop ends when `low > high` (empty space), meaning all elements were checked.
- Always move `low = mid + 1` and `high = mid - 1` → you've already checked `mid`.
- Return `mid` when found, `-1` otherwise.

**Gotchas:**
- ❌ Never use `high = mid` here — causes infinite loop when `low == high`.
- ✅ Safe to use `mid = low + (high - low) // 2` (floors toward left).

---

### Template 2 — Find Leftmost (First Occurrence / Lower Bound)

**Use when:**
- First occurrence of target in array with duplicates
- Leftmost position where `arr[i] >= target` (lower bound)
- Minimum value satisfying a condition

```python
def find_leftmost(arr, target):
    low, high = 0, len(arr)                    # ✅ high = len(arr), not len-1

    while low < high:                           # ✅ strict < because high is an EXCLUSIVE sentinel
        mid = low + (high - low) // 2           # ✅ floor mid (biases left)

        if arr[mid] < target:
            low = mid + 1                       # mid is too small, go right
        else:
            high = mid                          # mid could BE the answer, keep it

    return low                                  # ✅ always return low (= high at exit)
```

**Why `high = len(arr)`?**  
If target is greater than all elements, the answer is "insert at end" → index `len(arr)`. Setting `high = len(arr)` handles this naturally.

**Why return `low`?**  
At loop exit, `low == high`, both pointing at the leftmost valid position.

**Verify the result:**
```python
# After calling find_leftmost:
idx = find_leftmost(arr, target)
if idx < len(arr) and arr[idx] == target:
    print("Found at", idx)
else:
    print("Not found")
```

**Classic problems:** LeetCode 34 (first position), 35 (search insert position), 278 (first bad version — condition variant).

---

### Template 3 — Find Rightmost (Last Occurrence / Upper Bound)

**Use when:**
- Last occurrence of target in array with duplicates
- Rightmost position where `arr[i] <= target` (upper bound)
- Maximum value satisfying a condition

```python
def find_rightmost(arr, target):
    low, high = 0, len(arr)                    # ✅ same sentinel setup

    while low < high:                           # ✅ strict <
        mid = low + (high - low + 1) // 2      # ✅ CEILING mid (biases right — critical!)

        if arr[mid] > target:
            high = mid - 1                      # mid is too big, go left
        else:
            low = mid                           # mid could BE the answer, keep it

    return low                                  # ✅ return low (= high at exit)
```

**Why ceiling mid here?**  
When `low = mid` is in the branch, if you use floor mid with `low == high - 1`, you get `mid == low`, then `low = mid = low` → **infinite loop**. Ceiling mid breaks this.

> 🔑 **Rule of thumb:**  
> - `high = mid` branch → use **floor** mid (`low + (high-low)//2`)  
> - `low = mid` branch → use **ceiling** mid (`low + (high-low+1)//2`)

**Verify the result:**
```python
idx = find_rightmost(arr, target)
if idx >= 0 and arr[idx] == target:
    print("Found at", idx)
else:
    print("Not found")
```

---

### Template 4 — Binary Search on Answer (Condition-Based)

**Use when:**  
You're not searching an array index — you're searching for a *value* in a range where a monotonic condition `f(x)` flips from `False` to `True`.

```
f(low) = False, False, ..., False, True, True, ..., True, f(high)
                                   ^
                              find this
```

```python
def search_on_answer(lo, hi):
    # lo = minimum possible answer, hi = maximum possible answer
    while lo < hi:
        mid = lo + (hi - lo) // 2

        if condition(mid):                      # if mid satisfies the requirement
            hi = mid                            # mid could be optimal, shrink right
        else:
            lo = mid + 1                        # mid too small, go right

    return lo                                   # smallest value where condition is True
```

**For "find maximum where condition is True" (reverse):**
```python
def search_on_answer_max(lo, hi):
    while lo < hi:
        mid = lo + (hi - lo + 1) // 2          # ceiling mid (because lo = mid branch)

        if condition(mid):
            lo = mid                            # mid works, try larger
        else:
            hi = mid - 1                        # mid too large, go left

    return lo                                   # largest value where condition is True
```

**Classic problems:** Koko eating bananas (LC 875), ship packages in D days (LC 1011), split array largest sum (LC 410), capacity to ship.

---

## Decision Flowchart

```
Start: What am I searching for?
│
├── Exact value (exists or not)?
│   └── Template 1: low <= high, return mid or -1
│
├── Leftmost index / minimum satisfying condition?
│   └── Template 2: low < high, floor mid, high=mid, return low
│
├── Rightmost index / maximum satisfying condition?
│   └── Template 3: low < high, ceiling mid, low=mid, return low
│
└── Searching a value range (not array index)?
    ├── Minimum valid value?  → Template 4a (floor mid, hi=mid)
    └── Maximum valid value?  → Template 4b (ceiling mid, lo=mid)
```

---

## Side-by-Side Comparison

| | Template 1 | Template 2 | Template 3 | Template 4a | Template 4b |
|---|---|---|---|---|---|
| **Purpose** | Exact match | First / leftmost | Last / rightmost | Min answer | Max answer |
| **Loop** | `low <= high` | `low < high` | `low < high` | `low < high` | `low < high` |
| **mid** | floor | floor | **ceiling** | floor | **ceiling** |
| **When condition true** | `return mid` | `high = mid` | `low = mid` | `high = mid` | `low = mid` |
| **When condition false** | move both | `low = mid+1` | `high = mid-1` | `low = mid+1` | `high = mid-1` |
| **Return** | `mid` or `-1` | `low` | `low` | `lo` | `lo` |
| **Initial high** | `n - 1` | `n` | `n` | problem max | problem max |

---

## The One Rule That Prevents Infinite Loops

> When you write `low = mid`, you MUST use **ceiling mid**.  
> When you write `high = mid`, you MUST use **floor mid**.  
> In Template 1, you never write either — always `mid ± 1` — so floor is fine.

**Why?** With two elements `[low, high]` where `high = low + 1`:
- Floor mid = `low` → if branch is `low = mid`, then `low = low` → no progress → infinite loop ♻️
- Ceiling mid = `high` → `low = mid = high` → loop exits ✅

---

## `low` vs `high` — What to Return?

At loop exit in Templates 2, 3, 4: **`low == high` always**. So returning either is equivalent. But use `low` by convention — it's clearer.

```
Loop exit guarantee (low < high):
    When loop exits → low == high
    So: return low  ≡  return high
```

In Template 1: `low > high` at exit. You've exhausted all candidates → return `-1`.

---

## Handling Edge Cases

### Empty Array
```python
if not arr:
    return -1  # or 0 for insertion index
```

### All Elements Smaller Than Target
- Template 2 returns `len(arr)` (correct insert position at end)
- Template 1 returns `-1`

### All Elements Larger Than Target
- Template 2 returns `0` (insert at start)
- Template 3 returns `0` but `arr[0] != target` → verify after

### Single Element
- All templates handle correctly — always trace through manually when in doubt.

---

## Off-By-One Cheat Sheet

| Mistake | Symptom | Fix |
|---|---|---|
| `low + high // 2` | Integer overflow (C++/Java) | Use `low + (high - low) // 2` |
| `low = mid` with floor mid | Infinite loop on 2 elements | Switch to ceiling mid |
| `high = len(arr) - 1` in Template 2/3 | Misses insert-at-end case | Use `high = len(arr)` |
| `low <= high` in Template 2/3 | Extra iteration, `low` overshoots | Use `low < high` |
| Returning `high` when `high != low` at exit | Wrong answer | Only return after verifying `low == high` |
| Forgetting to verify `arr[low] == target` | False positives | Always validate after Template 2/3/4 |

---

## Quick Interview Recipe

1. **Identify the search space** — array indices or a value range?
2. **Identify what you're finding** — exact / leftmost / rightmost / min-max?
3. **Pick the template** from the table above.
4. **Write the condition** in the `if` block — the rest is mechanical.
5. **Verify the result** — after Templates 2/3/4, always check `arr[low] == target` if looking for exact value.
6. **Trace 2-element example** — `[x, y]` — to confirm no infinite loop.

---

## Full Example: LeetCode 34 — Find First and Last Position

```python
def searchRange(nums, target):
    def first():
        lo, hi = 0, len(nums)
        while lo < hi:
            mid = lo + (hi - lo) // 2
            if nums[mid] < target:
                lo = mid + 1
            else:
                hi = mid
        return lo

    def last():
        lo, hi = 0, len(nums)
        while lo < hi:
            mid = lo + (hi - lo + 1) // 2
            if nums[mid] > target:
                hi = mid - 1
            else:
                lo = mid
        return lo

    f = first()
    if f == len(nums) or nums[f] != target:
        return [-1, -1]
    return [f, last()]
```

---

## Summary Card (Memorize This)

```
EXACT MATCH          → low <= high,  mid±1,          return mid/-1
LEFTMOST / MIN       → low < high,   floor mid,  high=mid,  lo=mid+1,  return low
RIGHTMOST / MAX      → low < high,   ceil  mid,  low=mid,   hi=mid-1,  return low
```

> **The golden rule:** `low = mid` ↔ ceiling mid. `high = mid` ↔ floor mid. Always.
