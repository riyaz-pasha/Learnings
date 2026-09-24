# Monotonic Queue (Deque) — The Complete Mental Model (Java)

*Companion to your Monotonic Stack guide. Read that one first if you haven't — this pattern is its direct sibling, and half the intuition carries over.*

---

## PART 0 — Where this fits relative to Monotonic Stack

You already know: a **monotonic stack** answers "next/previous greater/smaller" by discarding elements from **one end** the moment they're proven useless.

Now the question changes shape:

> "Give me the max (or min) of **every sliding window of size k**, across the whole array, in O(n) total."

Brute force: for each window, scan all `k` elements → **O(n·k)**.

The catch that breaks a plain stack: elements don't just become "useless because something bigger showed up" — they can *also* become useless because **they fell out of the window** (too old, by position). A stack only ever removes from one end (the top). Here we need to remove from **both ends**:

- **Back** — same reason as monotonic stack: a new, better element makes old, worse candidates irrelevant.
- **Front** — a *new* reason, unique to this pattern: the element has expired by position, regardless of its value.

That's why we need a **Deque** (double-ended queue) instead of a plain `Stack`. Everything else — the "push once, pop at most once → O(n)" argument — still holds, just now counted across two ends instead of one.

---

## PART 1 — The Core Insight

Maintain a deque of **indices**, front to back, such that:
- The **values are monotonic** (decreasing for max-queries, increasing for min-queries).
- The **front always holds the answer** for the current window (the max or min).

Two independent maintenance rules run on every step:

1. **Value-eviction (back):** Before inserting a new index, pop from the **back** while the back's value is "dominated" by the new value (i.e., worse and now provably irrelevant forever — exactly the monotonic-stack argument).
2. **Position-eviction (front):** Before reading the answer, pop from the **front** while the front index has fallen outside the current window.

```
                back ←──────────────── front
Incoming element →  [ new ][ ... ][ candidate ][ CURRENT MAX/MIN ]
                       ↑                              ↑
              gets pushed here              this is your answer,
              after evicting worse          evicted if it's now
              values from the back          out of window range
```

**Why the front is always the answer:** because the deque is kept monotonic (say, decreasing for max), the front is the largest value that is *still within the window* — and we've proven every value that could beat it either already got evicted (because a later, bigger value dominates it) or hasn't arrived yet (irrelevant to the current window).

---

## PART 2 — "Next/Previous Greater/Smaller" — does that even apply here?

This is the question you're implicitly asking by pairing this with your stack guide. Here's the honest mapping:

- **Monotonic Stack** → answers point-queries: *"for THIS element, what's the next/previous greater/smaller element?"* — one answer per index, computed via one-directional elimination.
- **Monotonic Queue (Deque)** → answers **range-queries over a moving window**: *"for THIS window, what's the max/min?"* — one answer per window, computed via two-directional elimination (value AND position).

They are **not** the same four variants (Next Greater / Next Smaller / Previous Greater / Previous Smaller). Monotonic Deque only really has **two variants**:

| You want...                        | Deque stays...          | Evict from back while...      |
|-------------------------------------|---------------------------|-------------------------------|
| **Sliding Window Maximum**          | Decreasing (front→back)   | `val[back] <= val[new]`       |
| **Sliding Window Minimum**          | Increasing (front→back)   | `val[back] >= val[new]`       |

Front-eviction rule is **identical** in both cases: evict while `front index <= currentIndex - k`.

Memorize it the same way as before:

> **"Max or Min decides what I throw away from the back. The window size decides what I throw away from the front."**

---

## PART 3 — How to Identify a Monotonic Deque Problem

Ask:

1. **Does the problem involve a sliding/moving window** (fixed size `k`, or a dynamically growing/shrinking window) **and ask for max/min inside it, repeatedly, efficiently?** → Strong signal.
2. **Is the brute force "recompute max/min from scratch for every window"?** → That's exactly O(n·k); if `k` and `n` are both large, the problem is nudging you toward O(n).
3. **Does it mention "at most/at least sum in a window," "shortest subarray with sum ≥ K," "longest subarray with max-min ≤ limit"?** → These are monotonic deque problems in disguise, usually combined with prefix sums or two pointers.
4. **Keywords:** "sliding window," "maximum/minimum in every window of size k," "k consecutive," "constraint on window's max-min difference."
5. **Contrast with monotonic stack:** if the question is about a SINGLE nearest neighbor per element (not a moving window), you want a stack. If it's about repeated range max/min as a window slides, you want a deque.

**Rule of thumb:** *Monotonic stack = one-sided elimination for point queries. Monotonic deque = two-sided elimination for range queries.*

---

## PART 4 — Sliding Window Maximum, Fully Worked (LC 239)

**Problem:** Given `nums` and window size `k`, return the max of every contiguous window of size `k`.

Example: `nums = [1, 3, -1, -3, 5, 3, 6, 7]`, `k = 3`

**Java Code (the one to memorize):**

```java
public int[] maxSlidingWindow(int[] nums, int k) {
    int n = nums.length;
    int[] result = new int[n - k + 1];
    Deque<Integer> deque = new ArrayDeque<>(); // stores INDICES, values decreasing front→back

    for (int i = 0; i < n; i++) {
        // 1) Back-eviction: maintain decreasing order of values
        while (!deque.isEmpty() && nums[deque.peekLast()] <= nums[i]) {
            deque.pollLast();
        }
        deque.offerLast(i);

        // 2) Front-eviction: drop indices that fell out of the window
        if (deque.peekFirst() <= i - k) {
            deque.pollFirst();
        }

        // 3) Record answer once the first full window is formed
        if (i >= k - 1) {
            result[i - k + 1] = nums[deque.peekFirst()];
        }
    }
    return result;
}
```

**Full Dry Run** (`nums = [1, 3, -1, -3, 5, 3, 6, 7]`, `k = 3`):

```
i=0, val=1
  back-evict: deque empty, nothing to do
  push 0 → deque(idx): [0]        deque(val): [1]
  front-evict: front=0, 0 <= 0-3=-3? no
  i < k-1 (0<2) → no output yet

i=1, val=3
  back-evict: back val=1 <= 3 → pop. deque: []
  push 1 → deque(idx): [1]        deque(val): [3]
  front-evict: front=1, 1<=1-3=-2? no
  i < k-1 → no output

i=2, val=-1
  back-evict: back val=3 <= -1? no → stop
  push 2 → deque(idx): [1,2]      deque(val): [3,-1]
  front-evict: front=1, 1<=2-3=-1? no
  i==k-1 (2==2) → output! result[0] = nums[front=1] = 3
  window [1,3,-1] → max=3 ✓

i=3, val=-3
  back-evict: back val=-1 <= -3? no → stop
  push 3 → deque(idx): [1,2,3]    deque(val): [3,-1,-3]
  front-evict: front=1, 1<=3-3=0? no
  output: result[1] = nums[1] = 3
  window [3,-1,-3] → max=3 ✓

i=4, val=5
  back-evict: back val=-3<=5 pop; back val=-1<=5 pop; back val=3<=5 pop → deque: []
  push 4 → deque(idx): [4]        deque(val): [5]
  front-evict: front=4, 4<=4-3=1? no
  output: result[2] = nums[4] = 5
  window [-1,-3,5] → max=5 ✓

i=5, val=3
  back-evict: back val=5<=3? no → stop
  push 5 → deque(idx): [4,5]      deque(val): [5,3]
  front-evict: front=4, 4<=5-3=2? no
  output: result[3] = nums[4] = 5
  window [-3,5,3] → max=5 ✓

i=6, val=6
  back-evict: back val=3<=6 pop; back val=5<=6 pop → deque: []
  push 6 → deque(idx): [6]        deque(val): [6]
  front-evict: front=6, 6<=6-3=3? no
  output: result[4] = nums[6] = 6
  window [5,3,6] → max=6 ✓

i=7, val=7
  back-evict: back val=6<=7 pop → deque: []
  push 7 → deque(idx): [7]        deque(val): [7]
  front-evict: front=7, 7<=7-3=4? no
  output: result[5] = nums[7] = 7
  window [3,6,7] → max=7 ✓

Final result: [3, 3, 5, 5, 6, 7]
```

Every single eviction and push checks out against the actual window contents — walk through it once by hand yourself with a fresh array, and the mechanism will lock into memory permanently.

**Why front-eviction uses `<=` (not `<`) against `i - k`:** the window covering `result[i-k+1]` is `[i-k+1, i]`. An index `idx` is *outside* this window if `idx < i-k+1`, i.e., `idx <= i-k`. That's exactly the condition used.

---

## PART 5 — Sliding Window Minimum (the mirror)

Literally flip the back-eviction comparator:

```java
public int[] minSlidingWindow(int[] nums, int k) {
    int n = nums.length;
    int[] result = new int[n - k + 1];
    Deque<Integer> deque = new ArrayDeque<>(); // values increasing front→back

    for (int i = 0; i < n; i++) {
        while (!deque.isEmpty() && nums[deque.peekLast()] >= nums[i]) {
            deque.pollLast();
        }
        deque.offerLast(i);

        if (deque.peekFirst() <= i - k) {
            deque.pollFirst();
        }

        if (i >= k - 1) {
            result[i - k + 1] = nums[deque.peekFirst()];
        }
    }
    return result;
}
```

Only the `<=` became `>=`. Nothing else changes. This mirrors the "Greater vs Smaller flips the comparator" rule from the monotonic stack guide exactly.

---

## PART 6 — Why This Is O(n): The Same Proof, Extended to Two Ends

Claim: total work is O(n) despite the two nested while-loops (back-evict and front-evict).

- Each index is pushed to the **back** exactly once → n pushes total.
- Each index can be removed **at most once**, either:
  - popped from the back (value-evicted), **or**
  - popped from the front (position-evicted) — never both, since once it's gone, it's gone.
- So total removals ≤ total pushes ≤ n.
- Therefore total deque operations ≤ 2n → **O(n)** for the whole array, not per window.

```
Each index's lifecycle (mutually exclusive outcomes):

   pushed to back
        │
        ▼
   ┌──────────┐        ┌────────────────────┐
   │ survives │──or──▶ │ evicted from BACK   │  (a bigger/smaller value
   │ at front │        │ (value dominated)   │   arrived before its position
   │ (used as │        └────────────────────┘   expired)
   │  answer) │
   └────┬─────┘
        │
        ▼
   ┌────────────────────┐
   │ evicted from FRONT  │  (its position expired
   │ (position expired)  │   out of the window)
   └────────────────────┘

Every index takes exactly ONE path out. No double-counting → O(n) total.
```

---

## PART 7 — Beyond Fixed Windows: Where This Gets Powerful

The magic of monotonic deque isn't limited to fixed-size `k`. It generalizes beautifully to **variable-size windows** governed by some constraint — this is where it gets combined with **two pointers**.

### 7.1 Longest Subarray with Absolute Difference ≤ Limit (LC 1438)

**Problem:** Find the longest subarray where `max(subarray) - min(subarray) <= limit`.

**Mapping:** Maintain **two** monotonic deques simultaneously — one tracking the max (decreasing), one tracking the min (increasing) — over a **variable window** controlled by a left pointer. When `max - min > limit`, shrink from the left (advance left pointer, and evict from the *front* of both deques whenever the front index falls behind the new left boundary).

```java
public int longestSubarray(int[] nums, int limit) {
    Deque<Integer> maxDeque = new ArrayDeque<>(); // decreasing
    Deque<Integer> minDeque = new ArrayDeque<>(); // increasing
    int left = 0, best = 0;

    for (int right = 0; right < nums.length; right++) {
        while (!maxDeque.isEmpty() && nums[maxDeque.peekLast()] <= nums[right]) {
            maxDeque.pollLast();
        }
        maxDeque.offerLast(right);

        while (!minDeque.isEmpty() && nums[minDeque.peekLast()] >= nums[right]) {
            minDeque.pollLast();
        }
        minDeque.offerLast(right);

        while (nums[maxDeque.peekFirst()] - nums[minDeque.peekFirst()] > limit) {
            left++;
            if (maxDeque.peekFirst() < left) maxDeque.pollFirst();
            if (minDeque.peekFirst() < left) minDeque.pollFirst();
        }

        best = Math.max(best, right - left + 1);
    }
    return best;
}
```

Notice the front-eviction condition changed from "index fell out of a *fixed* window" to "index fell out of the *current* `[left, right]` window" — same mechanism, now driven by a two-pointer instead of a fixed offset `i - k`.

### 7.2 Shortest Subarray with Sum at Least K (LC 862) — genuinely hard, combines 3 ideas

**Problem:** Find the shortest subarray with sum ≥ K (array can have negative numbers, so plain two-pointer sliding window doesn't work).

**Mapping:** Build **prefix sums** `P[0..n]`. You need, for each `i`, the **smallest** `j < i` such that `P[i] - P[j] >= K`, minimizing `i - j`. Maintain an **increasing** monotonic deque of indices over the prefix sum array:

- **Back-eviction:** if `P[back] >= P[i]`, pop it — because for any future query, `i` is both a smaller prefix sum (or equal) AND a later index, making `back` strictly worse than `i` as a left-boundary candidate forever. (Same domination argument as always.)
- **Front-check (not just eviction — an active query):** while `P[i] - P[front] >= K`, that's a valid candidate window — record its length, **then pop the front** (since any future `i' > i` would only produce a longer window with this same front, so it's no longer useful either).

```java
public int shortestSubarray(int[] nums, int k) {
    int n = nums.length;
    long[] prefix = new long[n + 1];
    for (int i = 0; i < n; i++) prefix[i + 1] = prefix[i] + nums[i];

    Deque<Integer> deque = new ArrayDeque<>(); // increasing prefix-sum order
    int best = Integer.MAX_VALUE;

    for (int i = 0; i <= n; i++) {
        while (!deque.isEmpty() && prefix[i] - prefix[deque.peekFirst()] >= k) {
            best = Math.min(best, i - deque.pollFirst());
        }
        while (!deque.isEmpty() && prefix[deque.peekLast()] >= prefix[i]) {
            deque.pollLast();
        }
        deque.offerLast(i);
    }
    return best == Integer.MAX_VALUE ? -1 : best;
}
```

This is the deepest application of the pattern you'll typically meet — study this one last, after everything else is automatic.

---

## PART 8 — Monotonic Deque vs Other Structures (know when NOT to use it)

| Need                                              | Right tool                          |
|----------------------------------------------------|--------------------------------------|
| Max/min of every fixed-size sliding window          | **Monotonic Deque** — O(n)          |
| Max/min of a window with **arbitrary insert/remove** (not strictly sliding) | **Balanced BST / TreeMap / two heaps** — O(log n) per op |
| Just the running max/min of the *whole* array so far (no window shrinking) | Simple variable, O(1)               |
| Next/previous greater/smaller for a **single element** (not a window) | **Monotonic Stack**                 |
| Median in a sliding window                          | Two heaps (max-heap + min-heap), not a deque |

**Why not a heap (PriorityQueue) for sliding window max?** A heap gives you the max in O(log n), but **removing an arbitrary expired element** (one that's not on top) requires O(n) or a lazy-deletion trick — messier and slower than the deque's O(1) amortized approach. This is a very common "why not just use a heap" interview follow-up — know the answer cold: **heaps are bad at removing arbitrary/old elements; monotonic deques are built exactly for that.**

---

## PART 9 — Common Pitfalls

1. **Storing values instead of indices.** You almost always need the index — for front-eviction (position-based) and for computing window bounds/lengths. Default to indices, always.
2. **Wrong front-eviction condition.** For a fixed window of size `k` ending at `i`, the valid range is `[i-k+1, i]`. An index is expired if `idx < i-k+1`, i.e., `idx <= i-k`. Off-by-one here is the #1 bug source — write it out on paper before coding.
3. **Evicting from the front too early / checking front before back.** Order matters: **push and back-evict first**, *then* check front-eviction, *then* read the answer. If you check front-eviction before pushing the current element, you might pop something you still need, or read a stale answer.
4. **Forgetting the deque tracks positions of a monotonic *subsequence* of values, not literally the last `k` elements.** The deque can be much shorter than `k` — that's the entire point (it discards dominated values). Don't confuse "deque size" with "window size."
5. **Using `LinkedList` instead of `ArrayDeque`.** `ArrayDeque` is faster (array-backed, no node allocation) and is the idiomatic choice in Java. `LinkedList` works but is objectively worse for this use case — avoid it in interviews unless asked.
6. **Applying the fixed-window front-eviction formula (`i - k`) to a variable-window (two-pointer) problem.** In variable-window problems (Part 7.1), front-eviction must compare against the *current* `left` pointer, not a fixed offset.

---

## PART 10 — The Sanity Checklist

Before submitting any monotonic deque solution:

- [ ] Am I storing **indices**, not values?
- [ ] Is my deque's monotonic direction correct (decreasing for max, increasing for min)?
- [ ] Back-eviction condition uses the **non-strict** comparator (`<=` / `>=`) so equal values also get evicted (keeps deque as small as possible — pure optimization, doesn't change correctness but is good practice)?
- [ ] Front-eviction condition correctly reflects the window boundary — fixed (`idx <= i - k`) or variable (`idx < left`)?
- [ ] Am I evicting from the back and pushing **before** checking front-eviction and reading the answer?
- [ ] Am I only recording an answer once a **full window** has formed (`i >= k - 1` for fixed windows)?
- [ ] Using `ArrayDeque<Integer>`, not `LinkedList` or the legacy `Stack`?

---

## PART 11 — Practice Ladder

**Foundational:**
1. Sliding Window Maximum (LC 239) — the canonical problem, do this until it's automatic
2. Sliding Window Minimum (variant — not always a separate LC number, but implement it yourself as a drill)

**Intermediate:**
3. Constrained Subsequence Sum (LC 1425) — max in sliding window feeding into a DP
4. Jump Game VI (LC 1696) — same DP + monotonic deque combo
5. Longest Continuous Subarray With Absolute Diff ≤ Limit (LC 1438)

**Advanced:**
6. Shortest Subarray with Sum at Least K (LC 862)
7. Sliding Window Median (LC 480) — to understand *why* a deque alone is NOT enough here (needs heaps/BST instead — good contrast exercise)
8. Max Value of Equation (LC 1499) — monotonic deque on a transformed value, with a "window" defined by a coordinate constraint rather than array index

**Do 1 and 2 until you can write them from memory with zero bugs.** Problems 3–4 are the most common "hard" interview pattern: monotonic deque optimizing a DP transition. Problems 6–8 prove full mastery.

---

## PART 12 — The One-Paragraph Summary

> A monotonic deque maintains a shrinking pool of "still-possibly-relevant" candidates for a moving window's max or min, evicting from the back whenever a new element proves an old one can never win again (value-domination — same idea as monotonic stack), and evicting from the front whenever a candidate's position falls outside the current window (position-expiry — the new idea unique to this pattern). Because every index is pushed once and removed at most once across the whole run, total work is O(n) regardless of window size. It generalizes past fixed-size windows into two-pointer-driven variable windows, and into prefix-sum-driven problems, wherever you need "smallest/largest boundary satisfying a shrinking/growing range constraint, efficiently, again and again."

---

## Appendix — Quick Reference Card

```
┌───────────────────────────┬───────────────────────────┬──────────────────────────────┐
│ Goal                       │ Deque order (front→back)  │ Evict BACK while...           │
├───────────────────────────┼───────────────────────────┼──────────────────────────────┤
│ Sliding Window Maximum     │ Decreasing                 │ val[back] <= val[new]         │
│ Sliding Window Minimum     │ Increasing                 │ val[back] >= val[new]         │
└───────────────────────────┴───────────────────────────┴──────────────────────────────┘

Evict FRONT (fixed window k):     while deque.peekFirst() <= i - k: pollFirst()
Evict FRONT (variable window):    while deque.peekFirst() <  left : pollFirst()

Order of operations each iteration:
  1. back-evict (value domination)
  2. push current index to back
  3. front-evict (position expiry)
  4. read front = answer (if window is fully formed)

Java structure of choice:  Deque<Integer> dq = new ArrayDeque<>();
                            dq.offerLast(i); dq.pollLast(); dq.pollFirst();
                            dq.peekFirst(); dq.peekLast(); dq.isEmpty();
```

---

## How This Connects Back to Monotonic Stack

| Aspect                     | Monotonic Stack                            | Monotonic Deque                                          |
|-------------------------------|-----------------------------------------------|----------------------------------------------------------|
| Structure                    | Single-ended (Stack via ArrayDeque)              | Double-ended (ArrayDeque used fully)                       |
| Answers                      | One value per **index** (point query)             | One value per **window** (range query)                     |
| Eviction reasoning            | Value-domination only                             | Value-domination (back) + position-expiry (front)          |
| Complexity                    | O(n), each index pushed/popped once                | O(n), each index pushed once, popped once (either end)     |
| Typical output                | Next/Previous Greater/Smaller Element               | Max/Min of every sliding window                             |
| Classic hard problem          | Largest Rectangle in Histogram                      | Shortest Subarray with Sum ≥ K                               |

Same DNA, one extra degree of freedom (the front end). Once both of these are internalized together, you'll recognize this entire family of problems on sight.
