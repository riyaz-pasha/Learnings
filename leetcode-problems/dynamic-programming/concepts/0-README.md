# ⚡ Dynamic Programming — Advanced Interview Cheat Sheet

> **For 100+ problem veterans.** Skip the theory. Use this to wire your instincts before a session.

---

## 🧠 THE 4-STEP BREAKDOWN (Every Problem)

| Step | Question to Ask | Common Mistake |
|------|----------------|----------------|
| **1. State** | What minimum info describes a subproblem uniquely? | Over-parameterizing (extra dimensions you don't need) |
| **2. Transition** | How does `dp[i]` relate to smaller subproblems? | Missing a case; off-by-one in index |
| **3. Base Case** | What's the smallest valid subproblem? | Forgetting `dp[0]` vs `dp[1]` initialization |
| **4. Order** | Which subproblems must be solved first? | Bottom-up order violating dependency |

**State design heuristic:** Start with the most obvious parameters. Then ask — *"Is knowing these values sufficient to solve this subproblem without looking at the rest of the input?"*. If yes, that's your state.

---

## 🗂️ ALL MAJOR DP PATTERNS

---

### 1. 🔢 Linear DP (1D)

**Identification:** Sequence, array, string — optimal value at each index depends only on previous indices.

**Clues:**
- "Max/min sum/product ending at index i"
- "Number of ways to reach index i"
- Decisions: include/skip current element

**State:** `dp[i]` = answer for first `i` elements (or ending at `i`)

**Key distinction:** `dp[i]` = answer for `i` elements ≠ `dp[i]` = answer ending at index `i`. The latter forces you to take the `i`th element; the former doesn't. Choose based on problem structure.

**Examples:** Climbing Stairs, House Robber, Max Subarray (Kadane's), Jump Game II

**Pitfall:** For "ending at i" formulation, the global answer is `max(dp[0..n])`, not `dp[n]`.

---

### 2. 📐 2D Grid DP

**Identification:** Matrix, grid traversal with constraints on direction.

**Clues:**
- Movement restricted (right/down only → usually no memoization needed, just bottom-up)
- "Unique paths", "min cost path", "max gold collected"
- Obstacles or forbidden cells

**State:** `dp[i][j]` = answer to reach/at cell `(i, j)`

**Transition template:**
```
dp[i][j] = f(dp[i-1][j], dp[i][j-1])  // or diagonals, etc.
```

**Pitfall:** Initialize boundary rows/columns carefully. When there's an obstacle, the entire row/col beyond it may be 0 (unreachable), not just that cell.

**Space opt:** Compress to 1D rolling array since you only need the previous row.

---

### 3. 🧵 Subsequence / Substring DP

**Two key variants:**

#### 3a. Two-sequence (LCS-type)
**Identification:** Two strings/arrays; aligning, matching, editing.
- LCS, Edit Distance, Shortest Common Supersequence, Wildcard/Regex Matching

**State:** `dp[i][j]` = answer for `s1[0..i]` and `s2[0..j]`

**Transition pattern:**
```
if s1[i] == s2[j]:
    dp[i][j] = dp[i-1][j-1] + (something)
else:
    dp[i][j] = f(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
```

#### 3b. Single-sequence interval (Palindrome-type)
**Identification:** "Optimal on a substring `s[i..j]`".
- Palindrome Partitioning, Burst Balloons, MCM, Zuma Game

**State:** `dp[i][j]` = answer for `s[i..j]`

**Order:** **Fill by length** (l = 2, 3, ..., n). Never by row/column — dependencies go across diagonals.

```
for length in range(2, n+1):
    for i in range(n - length + 1):
        j = i + length - 1
        # compute dp[i][j]
```

**Pitfall:** Forgetting to iterate by length. Iterating by `i` then `j` can reference uncomputed states.

---

### 4. 🎒 Knapsack Variants

**Master identification:** Items + capacity/budget + optimal value.

#### 4a. 0/1 Knapsack
Each item used at most once. 
`dp[i][w]` = best value using first `i` items, capacity `w`
- **Space opt:** Iterate `w` from right to left in 1D.

#### 4b. Unbounded Knapsack
Each item usable infinitely.
- **Space opt:** Iterate `w` from left to right in 1D.

#### 4c. Bounded Knapsack
Each item has a count limit. Reduce to 0/1 via binary splitting, or use monotone deque for O(NW).

#### 4d. Subset Sum / Partition
"Can we achieve exactly target T?" → Boolean DP.
"Count subsets with sum T?" → Counting DP.

**Key pattern recognition:**
```
"Partition into two equal subsets" → Subset sum to total/2
"Last stone weight"               → Subset sum variant
"Target sum with +/-"             → Subset sum to (target + total) / 2
```

#### 4e. Multiple Knapsack Dimensions
e.g., "Ones and Zeroes" (2D capacity: count of 0s and 1s)
→ Each extra constraint adds a dimension. Watch for space explosion.

---

### 5. 🌲 Tree DP

**Identification:** Tree structure, optimal value using subtree rooted at each node.

**Clues:**
- "Max path sum in tree", "Min camera cover", "Robbing houses on a tree"
- State often captures multiple scenarios at each node (take/skip, covered/uncovered)

**State:** `dp[node][state]` where state is a small enum (e.g., 0 = not selected, 1 = selected)

**Order:** Post-order DFS (children before parent).

**Template:**
```
def dfs(node):
    for child in node.children:
        child_result = dfs(child)
    dp[node] = combine(dp[children], node.val)
```

**Pitfall:** Rerooting technique — when the answer at every node depends on the whole tree (not just its subtree), do two DFS passes: one down, one up. (e.g., "Sum of distances in tree")

---

### 6. 🔲 Bitmask DP

**Identification:** Small set of items (n ≤ 20), need to track subsets.

**Clues:**
- "Visit all cities" / "Assign all tasks" / "Cover all elements"
- Exponential states are acceptable (2^n × n)

**State:** `dp[mask][i]` = answer having visited the set encoded by `mask`, currently at `i`

**Transition:** Iterate over all bits not set in `mask` for the next step.

**Canonical problem:** TSP, Minimum Cost to Connect All Points (masked), Stickers to Spell Word

**Popcount trick:** `mask & (mask - 1)` removes lowest set bit. Useful for enumerating submasks.

**Submask enumeration:** `for sub = mask; sub > 0; sub = (sub-1) & mask` — O(3^n) total.

---

### 7. 📊 Digit DP

**Identification:** Count integers in `[L, R]` satisfying some digit-level property.

**State:** `dp[pos][tight][...extra state...]`
- `pos`: current digit position
- `tight`: are we still bounded by the original number?
- Extra: sum so far, last digit, count of a digit, etc.

**Template thinking:**
```
f(pos, tight, ...):
    limit = digit[pos] if tight else 9
    for d in 0..limit:
        ans += f(pos+1, tight and d==limit, ...)
```

**Pitfall:** Handle leading zeros separately if the problem distinguishes them (e.g., "non-decreasing digits").

---

### 8. 🔀 DP on DAGs / Shortest Paths

**Identification:** Counting paths, longest path, min-cost path in a DAG.

**Key insight:** Any DP on a sequence IS DP on a DAG (each index is a node).

**Clues:**
- "Number of ways to reach destination"
- "Longest increasing subsequence" (DAG of valid transitions)

**LIS specifically:** Classic O(n²) is DP; O(n log n) is DP + binary search (patience sorting). The patience sort insight: `tails[i]` = smallest tail of all increasing subsequences of length `i+1`.

---

### 9. 📅 Interval / Scheduling DP

**Identification:** Non-overlapping intervals, scheduling, merging.

**Clues:**
- Jobs with start/end times, select max-weight non-overlapping subset
- "Weighted Job Scheduling" → sort by end time + binary search for last non-conflicting job

**State:** `dp[i]` = max profit using first `i` jobs (sorted by end time)

**Transition:** `dp[i] = max(dp[i-1], val[i] + dp[last_non_conflict(i)])`

**Binary search hook:** `last_non_conflict(i)` via upper_bound on start times.

---

### 10. 🎲 Probability / Expected Value DP

**Identification:** "Expected number of steps/moves", dice games, random walks.

**State:** Expected value of being in state `s`

**Direction:** Often goes "backwards" — define `E[state]` and express in terms of future states.

**Pitfall:** Circular dependencies in non-DAG state spaces require solving a system of linear equations, not just memoization.

---

### 11. 🔄 DP with Monotone Queue / Deque Optimization

**Identification:** Transition of form `dp[i] = min(dp[j] + cost(j, i))` where the cost has a sliding window structure.

**Clues:**
- Window-constrained transitions: `j ∈ [i-k, i-1]`
- Reducing O(n²) DP to O(n)

**Template:** Maintain a deque of candidates. Pop from front when out of window. Pop from back when new candidate dominates.

**Examples:** Sliding Window Maximum (base case), Jump Game VI, Constrained Subset Sum

---

### 12. 📉 Convex Hull Trick (CHT)

**Identification:** `dp[i] = min over j of (dp[j] + b[j] * a[i])` — linear function in `a[i]`.

**Clues:**
- Transition is a linear function of the current index
- "Divide cost across segments optimally"

**When usable:** If `a[i]` is monotone → simple stack. If not → Li Chao tree (O(n log n)).

**Examples:** Optimal BST (variant), Buying and Selling Stock (slope trick), Minimum Cost to Cut a Stick (interval DP + CHT)

---

### 13. 🌀 Slope Trick

**Identification:** DP where the value function is piecewise linear and convex/concave.

**Clues:**
- "Minimum cost to make array non-decreasing/non-increasing"
- Operations that shift or clip the DP function

**Core idea:** Instead of tracking dp values, track the slopes of the piecewise linear function using a priority queue.

**Examples:** Minimum Number of Moves to Make Array Palindrome, Minimum Cost to Make Array Non-decreasing

---

## ⚡ PATTERN RECOGNITION HEURISTICS

```
"Count ways"              → Counting DP (often modular arithmetic)
"Max/min value"           → Optimization DP
"Is it possible?"         → Boolean DP (early termination possible)
"k operations/steps"      → Add k as a state dimension
"At most k"               → State: (position, remaining_k)
"Two pointers on string"  → Interval DP
"Circular array"          → Break circle: solve [0,n-1] and [1,n], take best
"Grid with direction"     → 2D DP, check if DAG (no cycles)
"Subset of items"         → Knapsack or bitmask
"Partition array"         → Think about split points; interval or prefix DP
"Tree + select nodes"     → Tree DP with state per node
"Digits of a number"      → Digit DP
"All cities/nodes visited"→ Bitmask DP (if n ≤ 20)
```

---

## 🚩 COMMON PITFALLS & EDGE CASES

| Pitfall | Fix |
|--------|-----|
| Off-by-one in 1-indexed vs 0-indexed | Decide convention early; add dummy row/col for LCS-type |
| Modular arithmetic overflow | Use `(a + b) % MOD` carefully; avoid `%` on differences |
| Integer overflow in product DP | Use `long`, cap early, or use log-space |
| "Exactly K" vs "at most K" | "Exactly K" = `atMost(K) - atMost(K-1)` trick |
| Unreachable states initialized wrong | Use `-inf` for max-DP, `+inf` for min-DP, never `0` |
| Circular dependency in top-down | Detect cycles; may need iterative fixed-point computation |
| Forgetting the "do nothing" transition | Sometimes the best move is to stay |
| Double-counting in combinatorics DP | Ensure each configuration counted exactly once |
| Interval DP filled in wrong order | Always fill by increasing length |

---

## 🔧 OPTIMIZATION TECHNIQUES

### Space Optimization
- **Rolling array:** If `dp[i]` only depends on `dp[i-1]`, use two rows or a 1D array
- **Direction trick for 0/1 knapsack:** Right-to-left inner loop eliminates the 2D table
- **Direction trick for unbounded:** Left-to-right allows reuse

### State Compression
- Replace large state with hash (top-down) when state space is sparse
- **Profile DP:** For grid problems with row-by-row transitions, encode the "interface" between processed and unprocessed rows as a bitmask

### Transition Optimization
| Technique | When | Complexity gain |
|-----------|------|-----------------|
| Binary search on transitions | Monotone last valid `j` | O(n²) → O(n log n) |
| Monotone deque | Sliding window transitions | O(n²) → O(n) |
| Convex Hull Trick | Linear-function transitions | O(n²) → O(n) |
| Divide & Conquer DP | Optimal split point is monotone (`opt[i][j] ≤ opt[i][j+1]`) | O(n²) → O(n log n) |
| Matrix exponentiation | Linear recurrence, large n | O(n·states²) → O(states³ · log n) |

### Divide & Conquer DP Condition (Knuth's Optimization)
Applicable when:
1. `cost(i, j)` satisfies the **quadrangle inequality**: `cost(a,c) + cost(b,d) ≤ cost(a,d) + cost(b,c)` for `a ≤ b ≤ c ≤ d`
2. The optimal split for `dp[i][j]` is monotone in `j`

Reduces interval DP from O(n³) to O(n² log n) or even O(n²).

---

## ❌ WHEN NOT TO USE DP

### Use Greedy Instead When:
- **Greedy choice property holds:** Locally optimal choices lead to global optimum
- No "future impact" from current decision (Interval scheduling, Huffman, Dijkstra)
- Exchange argument proves greedy is correct

**Quick test:** Can you prove that taking the locally best option never prevents a better global outcome? If yes → greedy.

### Greedy vs DP Decision Table:
| Scenario | Verdict |
|----------|---------|
| Fractional Knapsack | Greedy (sort by value/weight) |
| 0/1 Knapsack | DP (selections interact) |
| Activity Selection (unweighted) | Greedy |
| Weighted Job Scheduling | DP |
| Coin Change (canonical systems) | Greedy |
| Coin Change (arbitrary denominations) | DP |
| Single-source shortest path (no neg) | Dijkstra (Greedy) |
| Shortest path with negative edges | Bellman-Ford (DP-like) |

### Also Avoid DP When:
- State space too large and sparse → BFS/DFS with pruning
- Problem has no overlapping subproblems → Divide & Conquer
- Constraints too large for even O(n log n) → Mathematical formula / observation needed

---

## 🔀 HYBRID PATTERNS

### DP + Binary Search
- **LIS (O(n log n)):** patience sort — binary search on `tails` array
- **Weighted Job Scheduling:** binary search for last non-overlapping job
- **DP on sorted structure:** When valid transitions form a monotone range

### DP + BFS/Dijkstra
- **Shortest path in layered graph:** State = `(node, extra_dimension)`, run Dijkstra
- **0-1 BFS:** Edge weights 0 or 1, use deque — effectively DP on a graph
- **Multi-source BFS + DP:** Precompute distances, then DP on results

### DP + Graphs (SCC / Topo Sort)
- DP on DAGs naturally follows topological order
- For graphs with cycles: Condense SCCs → DAG → DP on condensed graph
- Longest path in DAG: DP in topological order

### DP + Segment Tree / BIT (Fenwick)
- **LIS O(n log n) via BIT:** `dp[i] = 1 + max(dp[j] for j < i, arr[j] < arr[i])`; use BIT for range max query
- **2D DP range queries:** When transition queries a range of previous states
- Pattern: `dp[i] = f(query(range)) + g(i)` → augment with BIT/segment tree

### DP + Matrix Exponentiation
- Linear recurrence with large `n` (Fibonacci-style, tiling, path counting on fixed graphs)
- State vector × transition matrix, exponentiate by squaring in O(k³ log n)
- Key: Identify the recurrence, encode state as vector

### DP + Geometry (Convex Hull Trick / Li Chao Tree)
- When `dp[i]` is a minimum of linear functions evaluated at `i`
- Li Chao tree handles arbitrary query order; monotone stack handles sorted order

---

## 🧮 TIME COMPLEXITY PATTERNS

| Pattern | Typical Complexity | Note |
|--------|--------------------|------|
| 1D linear DP | O(n) | |
| 2D grid DP | O(n·m) | |
| Two-sequence DP | O(n·m) | LCS, Edit Distance |
| Interval DP | O(n³) | O(n²) states × O(n) transitions |
| Interval DP + Knuth | O(n²) | Quadrangle inequality holds |
| Knapsack | O(n·W) | Pseudo-polynomial |
| Bitmask DP | O(2ⁿ · n) | n ≤ 20 |
| Digit DP | O(log(N) · states) | |
| Tree DP | O(n) | DFS once |
| Tree DP (subtree pairs) | O(n²) | Can optimize with small-to-large |
| DP + BIT/Seg tree | O(n log n) | LIS, range transitions |
| DP + CHT | O(n) or O(n log n) | |
| Matrix exponentiation | O(k³ log n) | k = state count |

---

## 🎯 ADVANCED INSIGHTS (What Experienced Candidates Miss)

### 1. The "Push" vs "Pull" Formulation
- **Pull:** `dp[i] = f(dp[i-1], dp[i-2], ...)` — standard; compute when you arrive at state `i`
- **Push:** `dp[i]` contributes to `dp[i+1], dp[i+2], ...` — useful when contributions are irregular

### 2. Offline DP
Sometimes you can sort input and run DP in a non-input order. Weighted job scheduling is an example. Offline reordering can expose monotone structures enabling optimization.

### 3. DP on Permutations
When counting permutations with constraints, think about inserting elements one by one. The state tracks properties of the current partial permutation (last element, number of inversions, etc.).

### 4. "Meet in the Middle" DP
For n ≈ 40 (too large for 2ⁿ, too small for polynomial): split input in half, DP each half independently, combine. Complexity: O(2^(n/2) · something).

### 5. Broken Profile DP
For grid tiling problems, process cell by cell (not row by row). State = bitmask of the "broken" boundary between processed and unprocessed cells. Key for counting tilings.

### 6. The "Dummy Dimension" Trick
When a constraint feels like it adds a dimension, sometimes you can embed it implicitly. E.g., "at most one transaction" in stock problems — instead of `dp[i][k]`, observe that with k=2, you can just track `buy1, profit1, buy2, profit2`.

### 7. DP State = Suffix/Prefix Invariant
If `dp[i]` represents "best answer for suffix starting at i" — transitions go forward. If it represents "best answer using prefix ending at i" — transitions look backward. Choose based on what's easier to express.

### 8. Counting Paths in Disguise
Many counting problems (number of BSTs, number of valid sequences) are counting paths in an implicit DAG. Visualizing the DAG helps design the DP.

### 9. The "Contribution" Technique
Instead of computing total cost, compute each element's contribution to the total. Often transforms O(n²) DP to O(n) math. (Common in sum-over-subsets problems.)

### 10. Memoization Pitfall: Partial State
A subtle bug: memoizing a function where some inputs come from outer scope (closure variables) rather than parameters. The cache key must encode ALL inputs that affect output.

---

## ✅ INTERVIEW MENTAL CHECKLIST

```
□ Is there optimal substructure? (Can I trust subproblem answers?)
□ Are there overlapping subproblems? (Otherwise → D&C)
□ What are my state variables? (Minimum info to define a subproblem)
□ What's my transition? (Try all choices, take optimal)
□ What are base cases? (Empty, zero, single element)
□ What order do I fill the table? (Ensure dependencies computed first)
□ Can I optimize space? (Rolling array, 1D compression)
□ Is greedy sufficient? (Exchange argument, no future impact)
□ Should I use top-down or bottom-up?
    → Top-down: sparse states, easier to reason about
    → Bottom-up: tight loops, cache-friendly, easier to optimize space
□ Edge cases: empty input, k=0, single element, all same values
□ Overflow: use long, check for negative numbers in min-DP
□ Is n small enough for bitmask (≤20) or brute force (≤15)?
□ Any monotone structure I can exploit? (Binary search, deque, CHT)
```

---

## 🔑 QUICK PATTERN LOOKUP

| Problem hint keywords | Pattern |
|----------------------|---------|
| Max subarray, contiguous | Kadane's (linear DP) |
| Longest common subsequence | 2-sequence DP |
| Edit distance, regex match | 2-sequence DP |
| Palindrome partition/substring | Interval DP |
| Burst balloons, matrix chain | Interval DP (split point) |
| Coin change, unbounded items | Unbounded Knapsack |
| 0/1 item selection with capacity | 0/1 Knapsack |
| Subset with exact sum | Subset Sum / Boolean DP |
| Assign tasks to workers | Bitmask DP |
| Visit all nodes in graph | Bitmask DP (TSP-like) |
| Count integers with property | Digit DP |
| Tree path / node selection | Tree DP (post-order DFS) |
| K non-overlapping intervals | Interval scheduling DP + binary search |
| Minimize cost of splits | Interval DP or D&C DP |
| Large n, linear recurrence | Matrix exponentiation |
| Min cost with linear transition | Convex Hull Trick |
| Jump with window constraint | Monotone Deque DP |
| Probability / expected value | Expectation DP |
| Circular array optimization | Break into two linear DPs |
