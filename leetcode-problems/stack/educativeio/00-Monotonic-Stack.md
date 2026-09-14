# Monotonic Stack — The Complete Mental Model (Java)

---

## PART 0 — Why this pattern exists at all

Before touching code, understand the *problem class* this pattern solves.

You constantly see questions shaped like:

> "For each element, find the **nearest** element to its left/right that is **greater/smaller** than it."

Naive approach: for every index `i`, scan left or right until you find the answer.
That's **O(n²)** in the worst case (e.g., a strictly increasing array — every element scans almost the whole array).

The monotonic stack is a technique to answer **all n queries in O(n) total time**, by being smart about *what work is reusable* between consecutive elements. That reusability is the entire soul of the pattern — everything else is mechanical detail.

---

## PART 1 — The Core Insight (read this twice)

Imagine you're scanning the array left to right, and you're maintaining a stack of "candidates" — elements that **might still be the answer** for some future element.

Key realization:

> **If a new element `x` arrives and it is greater than the element on top of the stack, that top element can NEVER be the "next greater element" for anyone else standing further right — because `x` itself is closer AND bigger. The top element's fate is sealed. It gets popped, its answer is finalized as `x`, and it's discarded forever.**

This is the entire trick. Each element is:
- pushed **once**
- popped **at most once**

→ Total operations ≤ 2n → **O(n) amortized**, even though there's a `while` loop inside a `for` loop that *looks* like O(n²).

This is the single most important fact to internalize. If you truly get *why* each element is pushed and popped only once, you understand monotonic stacks. Everything below is just applying this idea in 4 directions.

---

## PART 2 — What "monotonic" actually means

A **monotonic stack** is a stack where the elements, read from bottom to top, are always sorted — either:

- **Monotonically increasing** (bottom → top: small → large)
- **Monotonically decreasing** (bottom → top: large → small)

We **maintain** this order by popping elements that would violate it before pushing a new one.

```
Monotonic INCREASING stack (bottom→top: 2, 5, 9)
   top → [ 9 ]
         [ 5 ]
         [ 2 ]  ← bottom

Monotonic DECREASING stack (bottom→top: 9, 5, 2)
   top → [ 2 ]
         [ 5 ]
         [ 9 ]  ← bottom
```

**The type of stack you maintain determines what question you're answering.**

This is the mapping you must memorize:

| You want to find...              | Stack must be...        | You pop while...                     |
|-----------------------------------|--------------------------|----------------------------------------|
| **Next Greater Element**          | Decreasing (top→bottom: small→large is wrong — see below) | `arr[top] < arr[current]` |
| **Next Smaller Element**          | Increasing               | `arr[top] > arr[current]`             |
| **Previous Greater Element**      | Decreasing               | `arr[top] < arr[current]`             |
| **Previous Smaller Element**      | Increasing               | `arr[top] > arr[current]`             |

Notice: **Next-vs-Previous changes scan direction. Greater-vs-Smaller changes the pop condition.** The stack "shape" (increasing/decreasing) is actually the same for Next-Greater and Previous-Greater (both use a decreasing stack), and same for Next-Smaller/Previous-Smaller (increasing stack). Only the **direction you traverse the array** differs.

This single table is 80% of "identifying which variant to use."

---

## PART 3 — How to *identify* a monotonic stack problem (pattern recognition)

Ask yourself these questions when you see a new problem:

1. **Is it about "next/previous ... greater/smaller"?** → Obvious signal.
2. **Does it involve comparing each element to its neighbors in some direction, and you need this for ALL elements efficiently?** → Signal.
3. **Does the problem talk about "span," "width," "how far until," "days until warmer," "visible buildings," "rectangle in histogram," "trapping water"?** → These are *disguised* next/previous greater/smaller problems.
4. **Do you need to know, for each element, the boundary where the current "streak" breaks?** (e.g., largest rectangle bounded by shorter bars) → monotonic stack.
5. **Sliding window max/min in O(n)?** → Monotonic **deque**, a close cousin (Part 8).
6. **Do you need to remove elements to form the "best" (smallest/largest, lexicographically smallest, etc.) sequence while preserving relative order?** → Monotonic stack used as a *greedy construction tool* (e.g., "Remove K Digits", "132 Pattern").

**The giveaway phrase:** if the brute-force solution is "for each element, look outward until a condition breaks," → monotonic stack replaces that outward-look with O(1) amortized work.

---

## PART 4 — The Four Fundamental Problems (fully worked)

We'll use one array throughout so you can compare all four side by side:

```
index:  0   1   2   3   4   5
arr :   2   1   2   4   3   1
```

### 4.1 Next Greater Element (NGE)

**Definition:** For each index `i`, find the nearest index `j > i` such that `arr[j] > arr[i]`. If none exists, `-1`.

**Direction to scan:** Right to Left (this is the "natural"/classic way — we'll also show left-to-right with reversal logic in a note).

**Why right to left?** Because when standing at index `i`, you want to know what's ahead of you (to the right) that is nearest and greater. If you scan right-to-left, the stack at any point contains exactly the "useful future" relative to the current index — elements that are *decreasing* as you go up, because anything smaller than what's already found close-by is irrelevant (it's *farther* and *not bigger*, so it can never be the answer over something closer and bigger... wait — actually let's be precise: it can never be the answer over something closer, period, regardless of magnitude, UNLESS the closer one is not greater than current).

**Stack invariant:** Monotonic **decreasing** (top→bottom, or equivalently we pop everything ≤ current before pushing).

**Intuition, step by step:**
- We scan from the rightmost element to the leftmost.
- The stack holds *candidates that could be the "next greater" for something to their left*.
- When we're at index `i`, we throw away (pop) every stack element that is **≤ arr[i]**, because such elements can never be the "next greater" for anything to the left of `i` — `arr[i]` itself is closer and at least as big, so it "blocks" them.
- After popping, whatever remains on top of the stack (if any) is the Next Greater Element for `arr[i]`.
- Then we push `arr[i]` onto the stack (it might be the answer for something further left).

**Dry Run** (arr = [2, 1, 2, 4, 3, 1], scanning right→left):

```
i=5, arr[5]=1
  stack: []            (empty, nothing to compare)
  NGE[5] = -1
  push 1 → stack: [1]

i=4, arr[4]=3
  stack top = 1, 1 <= 3 → pop
  stack: []
  NGE[4] = -1
  push 3 → stack: [3]

i=3, arr[3]=4
  stack top = 3, 3 <= 4 → pop
  stack: []
  NGE[3] = -1
  push 4 → stack: [4]

i=2, arr[2]=2
  stack top = 4, 4 > 2 → stop popping
  NGE[2] = 4
  push 2 → stack: [4, 2]

i=1, arr[1]=1
  stack top = 2, 2 > 1 → stop popping
  NGE[1] = 2
  push 1 → stack: [4, 2, 1]

i=0, arr[0]=2
  stack top = 1, 1 <= 2 → pop
  stack: [4, 2]
  stack top = 2, 2 <= 2 → pop   (strictly greater required, equal doesn't count)
  stack: [4]
  stack top = 4, 4 > 2 → stop
  NGE[0] = 4
  push 2 → stack: [4, 2]

Result: NGE = [4, 2, 4, -1, -1, -1]
```

**Java Code:**

```java
public int[] nextGreaterElement(int[] arr) {
    int n = arr.length;
    int[] result = new int[n];
    Deque<Integer> stack = new ArrayDeque<>(); // stores VALUES (or indices, see note)

    for (int i = n - 1; i >= 0; i--) {
        // Pop everything that cannot be the answer for arr[i]
        while (!stack.isEmpty() && stack.peek() <= arr[i]) {
            stack.pop();
        }
        result[i] = stack.isEmpty() ? -1 : stack.peek();
        stack.push(arr[i]);
    }
    return result;
}
```

> **Note on storing values vs indices:** Store **indices** instead of values whenever you need the actual position, or when array can have duplicates and you need to distinguish elements, or you need `j - i` (distance). Storing values is fine only for simple "what's the value" questions with no duplicate-ambiguity concerns. In interviews, **default to storing indices** — it's strictly more powerful and costs nothing extra.

Index-based version (the one you should actually memorize):

```java
public int[] nextGreaterElement(int[] arr) {
    int n = arr.length;
    int[] result = new int[n];
    Deque<Integer> stack = new ArrayDeque<>(); // stores INDICES

    for (int i = n - 1; i >= 0; i--) {
        while (!stack.isEmpty() && arr[stack.peek()] <= arr[i]) {
            stack.pop();
        }
        result[i] = stack.isEmpty() ? -1 : arr[stack.peek()];
        stack.push(i);
    }
    return result;
}
```

---

### 4.2 Previous Greater Element (PGE)

**Definition:** For each `i`, find nearest `j < i` such that `arr[j] > arr[i]`.

**Direction:** Left to Right (mirror image of NGE).

**Why:** Symmetric reasoning — now the stack holds candidates for things *to the right*, and we scan forward.

**Stack invariant:** Monotonic decreasing.

```java
public int[] previousGreaterElement(int[] arr) {
    int n = arr.length;
    int[] result = new int[n];
    Deque<Integer> stack = new ArrayDeque<>(); // indices

    for (int i = 0; i < n; i++) {
        while (!stack.isEmpty() && arr[stack.peek()] <= arr[i]) {
            stack.pop();
        }
        result[i] = stack.isEmpty() ? -1 : arr[stack.peek()];
        stack.push(i);
    }
    return result;
}
```

Same array dry run (arr = [2,1,2,4,3,1], left→right):

```
i=0 (2): stack empty → PGE=-1, push 0        stack idx:[0] vals:[2]
i=1 (1): top val=2 > 1, stop → PGE=2, push1  stack idx:[0,1] vals:[2,1]
i=2 (2): top val=1<=2 pop; top val=2<=2 pop; empty → PGE=-1, push2
                                              stack idx:[2] vals:[2]
i=3 (4): top val=2<=4 pop; empty → PGE=-1, push3
                                              stack idx:[3] vals:[4]
i=4 (3): top val=4>3, stop → PGE=4, push4    stack idx:[3,4] vals:[4,3]
i=5 (1): top val=3>1, stop → PGE=3, push5    stack idx:[3,4,5] vals:[4,3,1]

Result: PGE = [-1, 2, -1, -1, 4, 3]
```

---

### 4.3 Next Smaller Element (NSE)

**Definition:** For each `i`, nearest `j > i` with `arr[j] < arr[i]`.

**Direction:** Right to Left.
**Stack invariant:** Monotonic **increasing** (we pop while `stack top >= current`, i.e., pop values that are ≥, since we want strictly smaller ahead).

```java
public int[] nextSmallerElement(int[] arr) {
    int n = arr.length;
    int[] result = new int[n];
    Deque<Integer> stack = new ArrayDeque<>(); // indices

    for (int i = n - 1; i >= 0; i--) {
        while (!stack.isEmpty() && arr[stack.peek()] >= arr[i]) {
            stack.pop();
        }
        result[i] = stack.isEmpty() ? -1 : arr[stack.peek()];
        stack.push(i);
    }
    return result;
}
```

---

### 4.4 Previous Smaller Element (PSE)

**Definition:** For each `i`, nearest `j < i` with `arr[j] < arr[i]`.

**Direction:** Left to Right.
**Stack invariant:** Monotonic increasing.

```java
public int[] previousSmallerElement(int[] arr) {
    int n = arr.length;
    int[] result = new int[n];
    Deque<Integer> stack = new ArrayDeque<>(); // indices

    for (int i = 0; i < n; i++) {
        while (!stack.isEmpty() && arr[stack.peek()] >= arr[i]) {
            stack.pop();
        }
        result[i] = stack.isEmpty() ? -1 : arr[stack.peek()];
        stack.push(i);
    }
    return result;
}
```

---

## PART 5 — The Unified Mental Template (memorize THIS, not 4 separate codes)

All four problems are the **same 6 lines**, with 2 knobs you flip:

```java
int n = arr.length;
int[] result = new int[n];
Deque<Integer> stack = new ArrayDeque<>();

for (int i = START; RUNS; i += STEP) {              // knob 1: direction
    while (!stack.isEmpty() && COMPARATOR(arr[stack.peek()], arr[i])) {  // knob 2: comparator
        stack.pop();
    }
    result[i] = stack.isEmpty() ? -1 : arr[stack.peek()];
    stack.push(i);
}
```

**Knob 1 — Direction:**
- "Next ___" → scan **right to left** (`i = n-1 → 0`)
- "Previous ___" → scan **left to right** (`i = 0 → n-1`)

**Knob 2 — Comparator (what to pop):**
- Looking for **Greater** → pop while `arr[stack.peek()] <= arr[i]` (keep decreasing stack)
- Looking for **Smaller** → pop while `arr[stack.peek()] >= arr[i]` (keep increasing stack)

Memorize it as a sentence:

> **"Next/Previous decides which way I walk. Greater/Smaller decides what I throw away."**

That's it. That's the whole pattern reduced to one sentence. Once this sentence is automatic, you can derive all four variants in code within seconds, under interview pressure, without memorizing 4 separate snippets.

---

## PART 6 — Why does this run in O(n)? (Amortized analysis, proven properly)

Claim: total work across the whole algorithm is O(n), despite the nested while-loop.

**Proof by "potential"/accounting argument:**
- Every index gets pushed onto the stack **exactly once** (there's exactly one `push` call per loop iteration → n pushes total).
- Every index can be popped **at most once** ever (once popped, it's gone — never pushed again).
- So total pops ≤ total pushes ≤ n.
- The while loop's body executes once per pop → at most n times across the *entire* run of the algorithm (not per iteration — across ALL iterations combined).
- Total work = n (for-loop iterations) + n (pops across all iterations) + n (pushes) = O(n).

This is the classic "amortized O(1) per operation" argument — same idea as dynamic array resizing. **Individual iterations can be expensive** (one iteration might pop 1000 elements), but **summed across the whole run**, it's bounded by 2n.

```
Visual proof:
push count:  ██████████████████████████████  (exactly n, one per element)
pop count:   ████████████████░░░░░░░░░░░░░░  (at most n, since you can't pop
                                                more than what was pushed)
Total stack operations ≤ 2n  →  O(n)
```

---

## PART 7 — Classic Problems Mapped to This Pattern (this is where mastery comes from)

Once you see these mappings, you'll start recognizing the pattern everywhere.

### 7.1 Daily Temperatures (LeetCode 739)
**Ask:** for each day, how many days until a warmer temperature?
**Mapping:** This is **Next Greater Element**, but instead of returning the *value*, you return the **distance** (`j - i`). Use an index-based stack — that's why we always default to storing indices!

```java
public int[] dailyTemperatures(int[] temps) {
    int n = temps.length;
    int[] result = new int[n];
    Deque<Integer> stack = new ArrayDeque<>();
    for (int i = 0; i < n; i++) {
        while (!stack.isEmpty() && temps[stack.peek()] < temps[i]) {
            int idx = stack.pop();
            result[idx] = i - idx;
        }
        stack.push(i);
    }
    return result;
}
```
Notice: here we scan **left→right** but still solve "Next Greater" — because we resolve answers *as we push forward*, popping and finalizing on the way, rather than pre-scanning right-to-left. Both directions work for NGE; right-to-left is the "compute directly" style, left-to-right is the "resolve lazily on pop" style. **This lazy-resolve-on-pop style is extremely important — most real interview problems use this style**, not the direct right-to-left one. Internalize both.

### 7.2 Next Greater Element II — Circular Array (LC 503)
**Mapping:** Same as NGE, but array wraps around. Trick: simulate circularity by iterating `2n` times using `i % n`.
```java
public int[] nextGreaterElements(int[] arr) {
    int n = arr.length;
    int[] result = new int[n];
    Arrays.fill(result, -1);
    Deque<Integer> stack = new ArrayDeque<>();
    for (int i = 0; i < 2 * n; i++) {
        int idx = i % n;
        while (!stack.isEmpty() && arr[stack.peek()] < arr[idx]) {
            result[stack.pop()] = arr[idx];
        }
        if (i < n) stack.push(idx); // only push during first pass
    }
    return result;
}
```

### 7.3 Largest Rectangle in Histogram (LC 84) — the "boss level" problem
**Ask:** given bar heights, find the largest rectangle area.
**Mapping:** For each bar `i`, the maximal rectangle using height `arr[i]` extends from **Previous Smaller Element** to **Next Smaller Element** (exclusive boundaries). Width = `(nextSmaller[i] - prevSmaller[i] - 1)`. This is literally NSE + PSE combined, then a max-area sweep.

```java
public int largestRectangleArea(int[] heights) {
    int n = heights.length;
    int[] left = new int[n];   // distance to previous smaller (exclusive index, or -1)
    int[] right = new int[n];  // distance to next smaller (exclusive index, or n)
    Deque<Integer> stack = new ArrayDeque<>();

    for (int i = 0; i < n; i++) {
        while (!stack.isEmpty() && heights[stack.peek()] >= heights[i]) stack.pop();
        left[i] = stack.isEmpty() ? -1 : stack.peek();
        stack.push(i);
    }
    stack.clear();
    for (int i = n - 1; i >= 0; i--) {
        while (!stack.isEmpty() && heights[stack.peek()] >= heights[i]) stack.pop();
        right[i] = stack.isEmpty() ? n : stack.peek();
        stack.push(i);
    }

    int maxArea = 0;
    for (int i = 0; i < n; i++) {
        int width = right[i] - left[i] - 1;
        maxArea = Math.max(maxArea, heights[i] * width);
    }
    return maxArea;
}
```

**Single-pass version** (the "elegant" interview-favorite, using the lazy-resolve-on-pop style from 7.1):
```java
public int largestRectangleArea(int[] heights) {
    Deque<Integer> stack = new ArrayDeque<>();
    int maxArea = 0, n = heights.length;
    for (int i = 0; i <= n; i++) {
        int h = (i == n) ? 0 : heights[i]; // sentinel forces final flush
        while (!stack.isEmpty() && heights[stack.peek()] >= h) {
            int height = heights[stack.pop()];
            int width = stack.isEmpty() ? i : i - stack.peek() - 1;
            maxArea = Math.max(maxArea, height * width);
        }
        stack.push(i);
    }
    return maxArea;
}
```

### 7.4 Trapping Rain Water (LC 42)
**Mapping:** monotonic **decreasing** stack of indices. When you find a bar taller than the stack top, water is trapped between the new bar, the popped bar (the "bottom"), and whatever is now below on the stack (the other "wall"). This is a "two walls, one basin" application of the same eliminate-when-blocked logic.

```java
public int trap(int[] height) {
    Deque<Integer> stack = new ArrayDeque<>();
    int water = 0;
    for (int i = 0; i < height.length; i++) {
        while (!stack.isEmpty() && height[stack.peek()] < height[i]) {
            int bottom = stack.pop();
            if (stack.isEmpty()) break;
            int left = stack.peek();
            int boundedHeight = Math.min(height[left], height[i]) - height[bottom];
            int width = i - left - 1;
            water += boundedHeight * width;
        }
        stack.push(i);
    }
    return water;
}
```

### 7.5 Sum of Subarray Minimums (LC 907)
**Mapping:** For each element, count how many subarrays it is the **minimum** of — that requires PSE and NSE (with careful strict/non-strict handling to avoid double-counting duplicates). `count[i] = (i - PSE_index) * (NSE_index - i)`. Sum of `arr[i] * count[i]` over all `i`.

### 7.6 Remove K Digits (LC 402) / Create Maximum Number / Smallest Subsequence
**Mapping:** monotonic stack used **greedily** — not to answer next/previous queries, but to *build* the smallest/largest result by popping "worse" digits when a "better" digit arrives, as long as you still have removals budget. This is the "monotonic stack as a greedy construction tool" category from Part 3, point 6.

```java
public String removeKdigits(String num, int k) {
    Deque<Character> stack = new ArrayDeque<>();
    for (char c : num.toCharArray()) {
        while (!stack.isEmpty() && k > 0 && stack.peek() > c) {
            stack.pop();
            k--;
        }
        stack.push(c);
    }
    while (k-- > 0) stack.pop(); // if still removals left, remove from the end (largest tail)

    StringBuilder sb = new StringBuilder();
    while (!stack.isEmpty()) sb.append(stack.pop());
    sb.reverse();
    while (sb.length() > 1 && sb.charAt(0) == '0') sb.deleteCharAt(0); // strip leading zeros
    return sb.length() == 0 ? "0" : sb.toString();
}
```

### 7.7 132 Pattern (LC 456)
**Mapping:** scan right to left, maintain a decreasing stack, but track a "third element candidate" (`third`) whenever you pop — this is a clever twist where the *popped* values are remembered instead of discarded.

### 7.8 Online Stock Span (LC 901)
**Mapping:** streaming version of Previous Greater Element, using a stack of `(price, span)` pairs, collapsing spans as you pop.

### 7.9 Maximum Width Ramp (LC 962)
**Mapping:** a ramp `(i, j)` with `i<j` and `arr[i] <= arr[j]`. Build a decreasing stack of indices (candidates for the *left* side of a ramp), then scan right-to-left checking against the stack — a "two-pointer + monotonic stack" hybrid.

### 7.10 Asteroid Collision (LC 735)
**Mapping:** a stack simulation where positive/negative signs represent direction; each new asteroid may "pop" (destroy) opposing smaller ones — same core loop shape (`while condition: pop`) even though the domain (physics simulation) looks nothing like "next greater element" on the surface. **This is why pattern recognition matters more than memorizing problems** — the shape of the loop is what to recognize, not the story.

---

## PART 8 — Monotonic Deque (the sibling pattern: Sliding Window Maximum/Minimum)

**Problem:** Given array and window size `k`, find max (or min) of every window in O(n).

**Why a stack alone doesn't work:** A stack only removes from one end. Here we also need to remove **expired** elements (fallen out of the window) from the *other* end. So we use a **Deque** (double-ended queue) that is monotonic.

**Invariant for Sliding Window Maximum:** Deque stores indices, values monotonically **decreasing** from front to back. Front is always the max of the current window.

```
Window slides →. Deque holds candidate indices in DEcreasing value order.
front (max) → [ i_a, i_b, i_c ] ← back (most recent)
```

**Two removal rules (this is the key difference from a plain monotonic stack):**
1. **Back removal (monotonic upkeep):** while new element ≥ value at back, pop from back — same logic as before, the "blocked" reasoning.
2. **Front removal (window expiry):** while front index is out of the window range (`front <= i - k`), pop from front.

```java
public int[] maxSlidingWindow(int[] nums, int k) {
    int n = nums.length;
    int[] result = new int[n - k + 1];
    Deque<Integer> deque = new ArrayDeque<>(); // indices, values decreasing front→back

    for (int i = 0; i < n; i++) {
        // Rule 1: maintain decreasing order
        while (!deque.isEmpty() && nums[deque.peekLast()] <= nums[i]) {
            deque.pollLast();
        }
        deque.offerLast(i);

        // Rule 2: remove expired front
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

**Complexity:** still O(n) — same amortized argument: each index enters the deque once, leaves at most once (either via back-eviction or front-expiry).

For **sliding window minimum**, just flip the comparator to increasing order.

---

## PART 9 — Common Pitfalls (things that quietly break your solution)

1. **Strict (`<`/`>`) vs non-strict (`<=`/`>=`) comparators** — get this wrong and duplicates are handled incorrectly. Rule of thumb:
   - If you want **strictly greater/smaller**, use `<=`/`>=` inside the while-pop condition (pop equal elements too).
   - When solving "sum of subarray minimums"-style problems with duplicate values, you must break ties consistently (e.g., use `<=` on one side and `<` on the other) to avoid double-counting the same subarray.
2. **Storing values instead of indices** when you actually need position/distance — always default to indices.
3. **Off-by-one on empty stack** — always check `stack.isEmpty()` before `peek()`/pop, and decide your "sentinel" default (`-1`, `n`, etc.) up front.
4. **Forgetting the array is circular** — for circular variants, remember to only `push` during the *first* pass over the array (see 7.2), otherwise you double-push and corrupt indices.
5. **Confusing stack-based "next/previous" with deque-based "sliding window."** A stack only pops from one end; if the problem needs elements to expire based on *position* (not just value), you need a deque, not a stack.
6. **Using `Stack<Integer>` (the old `java.util.Stack`)** — it's legacy, synchronized (slower), and extends `Vector`. Always prefer `Deque<Integer> stack = new ArrayDeque<>();` and use `push()`/`pop()`/`peek()` (ArrayDeque implements these as stack operations too).

---

## PART 10 — The "Am I Even Right" Sanity Checklist (use this while coding)

Before submitting any monotonic stack solution, verify:

- [ ] Did I pick the correct **direction** (Next → right-to-left OR resolve-on-pop left-to-right; Previous → left-to-right)?
- [ ] Did I pick the correct **comparator** (Greater → decreasing stack; Smaller → increasing stack)?
- [ ] Am I storing **indices**, not values, unless I'm 100% sure I'll never need position/distance?
- [ ] Did I handle the **empty-stack** default correctly (`-1`, `n`, `0`, etc. — whatever fits the problem)?
- [ ] Did I decide strict vs non-strict comparators deliberately (not by accident)?
- [ ] If circular: am I only pushing indices during the first pass?
- [ ] If it's a sliding-window variant: am I using a **Deque with front-expiry**, not a plain stack?

---

## PART 11 — Practice Ladder (do these in order — this cements it permanently)

**Foundational (must be automatic):**
1. Next Greater Element I (LC 496)
2. Next Greater Element II — circular (LC 503)
3. Daily Temperatures (LC 739)
4. Final Prices With a Special Discount (LC 1475)

**Intermediate:**
5. Online Stock Span (LC 901)
6. Remove K Digits (LC 402)
7. Sum of Subarray Minimums (LC 907)
8. Next Greater Element III (LC 556) — different flavor, digit manipulation
9. Asteroid Collision (LC 735)
10. Car Fleet (LC 853) — disguised monotonic stack

**Advanced:**
11. Largest Rectangle in Histogram (LC 84)
12. Maximal Rectangle (LC 85) — histogram applied row-by-row on a matrix
13. Trapping Rain Water (LC 42)
14. 132 Pattern (LC 456)
15. Maximum Width Ramp (LC 962)
16. Sliding Window Maximum (LC 239) — the deque variant
17. Shortest Subarray with Sum at Least K (LC 862) — monotonic deque + prefix sums, genuinely hard

**Do them in this order.** By problem 10, the six-line template should be muscle memory. Problems 11–17 are where you prove you understand *why* it works, not just *how* to type it.

---

## PART 12 — The One-Paragraph Summary (say this to yourself until it's automatic)

> A monotonic stack keeps only the elements that *could still matter* for future queries, discarding ("popping") any element the moment a new one proves it can never be the answer. This guarantees each element is pushed once and popped at most once, giving O(n) total work despite nested loops. Direction of traversal (left→right vs right→left) determines whether you're solving "Previous" or "Next" queries; the pop comparator (`<=` vs `>=`) determines whether you're solving "Greater" or "Smaller" queries. Everything else — histograms, rain water, digit removal, sliding windows — is this same idea wearing a different costume.

---

## Appendix — Quick Reference Card

```
┌─────────────────────────────┬───────────────┬──────────────────────────┐
│ Problem                     │ Scan Direction │ Pop while (stack top)    │
├─────────────────────────────┼───────────────┼──────────────────────────┤
│ Next Greater Element        │ Right → Left   │ val <= current           │
│ Next Smaller Element        │ Right → Left   │ val >= current           │
│ Previous Greater Element    │ Left → Right   │ val <= current           │
│ Previous Smaller Element    │ Left → Right   │ val >= current           │
└─────────────────────────────┴───────────────┴──────────────────────────┘

Sliding Window Max (deque): pop BACK while val <= current; pop FRONT while expired.
Sliding Window Min (deque): pop BACK while val >= current; pop FRONT while expired.

Java stack of choice:  Deque<Integer> stack = new ArrayDeque<>();
                        stack.push(i);  stack.pop();  stack.peek();  stack.isEmpty();
```

