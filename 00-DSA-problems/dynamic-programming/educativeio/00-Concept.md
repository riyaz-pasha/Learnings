# Dynamic Programming — From First Principles to Mastery (Java)

---

## PART 1 — THE CORE IDEA

### 1.1 What DP actually is

Dynamic Programming is **not a new algorithm**. It is **smart brute force**.

Every DP problem starts life as a plain recursive brute-force solution. DP is the
optimization you apply to that recursion once you notice it's doing **repeated work**.

> **DP = Recursion + Remembering answers you've already computed.**

That's it. Everything else (tabulation, state, transitions, dimensions) is
implementation detail on top of that one sentence.

### 1.2 The two mathematical conditions

A problem is solvable by DP **if and only if** it has both of these:

**1. Overlapping Subproblems**
The recursive solution calls the *same* subproblem multiple times.

**2. Optimal Substructure**
The optimal solution to the problem can be constructed from optimal solutions
of its subproblems. (i.e., you can't get a better answer by combining
sub-optimal pieces.)

If a problem has optimal substructure but **no** overlapping subproblems
(e.g., plain Divide & Conquer like Merge Sort), DP gives you nothing —
memoizing merge sort doesn't help because subproblems never repeat.

### 1.3 Seeing overlapping subproblems — Fibonacci

```
                    fib(5)
                 /          \
            fib(4)            fib(3)
           /      \           /     \
       fib(3)    fib(2)   fib(2)   fib(1)
       /    \     /   \    /   \
   fib(2) fib(1) fib(1)fib(0) fib(1)fib(0)
   /   \
 fib(1) fib(0)
```

Count `fib(3)` — it appears **2 times**. `fib(2)` appears **3 times**.
As `n` grows, this tree grows **exponentially (O(2^n))**, but the number of
*distinct* subproblems is only `n+1` (fib(0)...fib(n)).

That gap — exponential calls vs. linear distinct subproblems — **is** the
signal for DP. If you cache each distinct answer the first time you compute
it, every future call becomes O(1). That collapses O(2^n) into O(n).

### 1.4 The mental model

```
Brute Force Recursion  --(has overlapping subproblems?)-->  Memoization (Top-Down DP)
                                                                     |
                                                        (unroll recursion into iteration)
                                                                     v
                                                              Tabulation (Bottom-Up DP)
                                                                     |
                                                          (do we need old rows/states?)
                                                                     v
                                                              Space-Optimized DP
```

This is the **exact pipeline** you will follow for every DP problem in this
guide. Never start by trying to write a DP table. Start by writing the
brute-force recursion.

---

## PART 2 — RECURSION REFRESHER (the foundation DP sits on)

If recursion itself is shaky, DP will always feel like magic instead of logic.
Quick but important refresher.

A recursive function needs:
1. **A base case** — the smallest input you can answer directly, no recursion.
2. **A recursive case** — express the problem in terms of *smaller* versions
   of itself.
3. **Trust the recursion** (a.k.a. the "leap of faith") — when you call
   `solve(smallerInput)`, assume it *already* gives the correct answer. Don't
   mentally unroll the whole tree. Just ask: "if I trust the smaller answer,
   how do I build today's answer from it?"

```java
// Classic example: nth Fibonacci, pure recursion, no DP yet
static int fib(int n) {
    if (n <= 1) return n;                     // base case
    return fib(n - 1) + fib(n - 2);            // recursive case (trust it!)
}
```

Time complexity: **O(2^n)** — exponential, because of the repeated subtree
calls shown above. This is our starting point for every DP problem.

---

## PART 3 — HOW TO IDENTIFY A DP PROBLEM (the real skill)

This is the part people struggle with most. Here is the actual checklist,
in the order I use it myself.

### 3.1 Keyword / phrasing signals

| Phrase in the problem | Likely DP |
|---|---|
| "Find the **number of ways** to..." | Counting DP |
| "Find the **minimum/maximum** cost/sum/length to..." | Optimization DP |
| "Is it **possible** to reach/partition/form..." | Boolean/feasibility DP |
| "Find the **longest/shortest** subsequence/substring/path..." | Sequence DP |
| Choices at each step, each choice affects future choices | Decision DP |
| Problem mentions **subsequence** (not substring) | Almost always DP |
| Involves picking items with constraints (weight, budget) | Knapsack family |

### 3.2 The structural test (most reliable)

Ask yourself: **"Can I define this problem in terms of a smaller version of
the same problem?"**

Concretely, try to write:
```
answer(n) = some_combination_of( answer(smaller_n_1), answer(smaller_n_2), ... )
```
If you can write this recurrence — even messily — it's DP-shaped.

### 3.3 The "brute force first" test (the practical one)

1. Write the brute-force recursive solution (try every choice at every step).
2. Draw or imagine the recursion tree for a small input.
3. Ask: **do any nodes repeat with identical arguments?**
   - Yes → memoize it → DP.
   - No → it's plain recursion/backtracking/divide-and-conquer, not DP.

This is the single most reliable method. Master this and you will correctly
identify DP problems even ones you've never seen before.

### 3.4 Counter-examples (to sharpen the boundary)

- **Merge Sort**: recursion + optimal substructure, but subproblems never
  repeat (each half is distinct data) → NOT DP, no benefit from memoizing.
- **Binary Search**: recursion, but no overlapping subproblems and nothing to
  "remember" → NOT DP.
- **Generating all subsets/permutations**: you *want* every distinct path, no
  repeated state to reuse → backtracking, not DP.

---

## PART 4 — THE 5-STEP FRAMEWORK (use this on every problem)

This is the exact procedure to solve **any** DP problem, in order:

**Step 1 — Define the state.**
What does `dp[...]` *mean* in plain English? E.g. "dp[i] = the minimum cost
to reach step i" or "dp[i][j] = length of LCS of first i chars of A and
first j chars of B." Write this sentence down before writing any code.

**Step 2 — Identify the choices at each state.**
At state `i`, what decisions can you make? (take item or skip it, move right
or down, match characters or don't, etc.)

**Step 3 — Write the recurrence (transition).**
Express `dp[state]` in terms of `dp[smaller/related states]`, combining the
choices from Step 2 (usually with min/max/sum/OR).

**Step 4 — Identify the base case(s).**
The smallest state(s) you can answer without recursion.

**Step 5 — Decide the order of computation.**
Which states must be solved before others? This tells you the loop order for
tabulation (or confirms memoization will naturally handle it).

Every single example below follows exactly these 5 steps. Internalize the
order — it never changes.

---

## PART 5 — TOP-DOWN (MEMOIZATION) vs BOTTOM-UP (TABULATION)

Both give the same answer. The difference is **direction of computation** and
**how state is stored**.

```
TOP-DOWN (Memoization)                 BOTTOM-UP (Tabulation)
------------------------               ------------------------
Start from the ORIGINAL question        Start from the BASE CASES
   fib(5)                                  fib(0), fib(1) known
     -> fib(4) -> fib(3) -> ... -> base     -> compute fib(2)
Uses recursion (call stack)                -> compute fib(3)
Cache = HashMap / array,                   -> compute fib(4)
        filled "on demand"                 -> compute fib(5)
                                            Uses a loop, cache filled
                                            in a fixed, planned order
```

### 5.1 Fibonacci — all four stages, side by side

**Stage 0: Brute force recursion — O(2^n) time, O(n) stack space**
```java
static int fibBrute(int n) {
    if (n <= 1) return n;
    return fibBrute(n - 1) + fibBrute(n - 2);
}
```

**Stage 1: Top-down memoization — O(n) time, O(n) space (array + stack)**
```java
static int fibMemo(int n, int[] memo) {
    if (n <= 1) return n;
    if (memo[n] != -1) return memo[n];      // <-- already solved? reuse it
    memo[n] = fibMemo(n - 1, memo) + fibMemo(n - 2, memo);
    return memo[n];
}
// driver
int n = 10;
int[] memo = new int[n + 1];
Arrays.fill(memo, -1);
int ans = fibMemo(n, memo);
```
Notice: **the code is almost identical to brute force.** We only added:
1. A cache (`memo[]`, initialized to a sentinel value like -1).
2. A check at the top: "have I solved this exact state before? If so, return
   it instantly instead of recomputing."
3. Storing the result before returning.

This is the *general recipe* for converting ANY brute-force recursion into
memoized DP — you almost never have to think about it differently.

**Stage 2: Bottom-up tabulation — O(n) time, O(n) space, no recursion**
```java
static int fibTab(int n) {
    if (n <= 1) return n;
    int[] dp = new int[n + 1];
    dp[0] = 0;                              // base case
    dp[1] = 1;                              // base case
    for (int i = 2; i <= n; i++) {
        dp[i] = dp[i - 1] + dp[i - 2];      // same recurrence, iterative
    }
    return dp[n];
}
```
Same recurrence as memoization — `dp[i] = dp[i-1] + dp[i-2]` — just computed
in a `for` loop from small `i` to large `i` instead of via recursive calls.

**Stage 3: Space-optimized — O(n) time, O(1) space**
```java
static int fibOptimized(int n) {
    if (n <= 1) return n;
    int prev2 = 0, prev1 = 1;
    for (int i = 2; i <= n; i++) {
        int curr = prev1 + prev2;
        prev2 = prev1;
        prev1 = curr;
    }
    return prev1;
}
```
Because `dp[i]` only ever needs `dp[i-1]` and `dp[i-2]` — never anything
older — we don't need the whole array. Just keep the last two values.
**This "do I need the whole history, or just the last row/few values?"
question is the general trick for space optimization.**

### 5.2 Top-down vs Bottom-up — how to choose

| | Top-Down (Memo) | Bottom-Up (Tabulation) |
|---|---|---|
| Easier to derive from brute force | ✅ Yes, minimal change | ❌ Requires figuring out iteration order |
| Computes only states actually needed | ✅ Yes (lazy) | ❌ Computes all states (even unused ones) |
| Risk of stack overflow (deep recursion) | ⚠️ Yes, for large n | ✅ No recursion |
| Easier to space-optimize | ❌ Harder (recursion needs full cache) | ✅ Easy (drop old rows) |
| Best for prototyping / interviews | ✅ Usually start here | Convert once logic is verified |

**Practical advice:** Always design the recurrence via recursion/memoization
first — it maps directly to your Step 1–5 framework and is far less
error-prone. Convert to tabulation afterward once the recurrence is proven
correct, especially if you need the extra speed or space savings.

---

## PART 6 — 1D DP PATTERNS

### 6.1 Climbing Stairs (decision DP)
*"You can climb 1 or 2 steps at a time. How many distinct ways to reach step n?"*

**Step 1 (state):** `dp[i]` = number of ways to reach step `i`.
**Step 2 (choices):** to reach `i`, your last move was either a 1-step (from
`i-1`) or a 2-step (from `i-2`).
**Step 3 (recurrence):** `dp[i] = dp[i-1] + dp[i-2]`
**Step 4 (base case):** `dp[0] = 1` (one way: do nothing), `dp[1] = 1`.
**Step 5 (order):** left to right, `i = 2..n`.

```java
static int climbStairs(int n) {
    if (n <= 1) return 1;
    int[] dp = new int[n + 1];
    dp[0] = 1; dp[1] = 1;
    for (int i = 2; i <= n; i++) dp[i] = dp[i - 1] + dp[i - 2];
    return dp[n];
}
```
Notice the recurrence is *identical in shape* to Fibonacci — same skeleton,
different story. Recognizing "this is secretly Fibonacci-shaped" is a real
skill that comes with practice.

### 6.2 House Robber (optimization DP — max with a constraint)
*"Rob houses in a row, adjacent houses can't both be robbed. Maximize loot."*

**State:** `dp[i]` = max money robbable from houses `0..i`.
**Choices at house i:** rob it (then add `dp[i-2]`, skipping neighbor) or
skip it (`dp[i-1]`).
**Recurrence:** `dp[i] = max(dp[i-1], dp[i-2] + nums[i])`
**Base case:** `dp[0] = nums[0]`, `dp[1] = max(nums[0], nums[1])`.

```java
static int rob(int[] nums) {
    int n = nums.length;
    if (n == 1) return nums[0];
    int prev2 = nums[0], prev1 = Math.max(nums[0], nums[1]);
    for (int i = 2; i < n; i++) {
        int curr = Math.max(prev1, prev2 + nums[i]);
        prev2 = prev1;
        prev1 = curr;
    }
    return prev1;
}
```
This is already space-optimized (only last two values kept) — same trick as
Fibonacci.

### 6.3 The general 1D DP shape

Almost all 1D DP problems reduce to: **"decide the answer at position i using
answers at a few earlier positions, combined with min/max/sum."**

```
dp[i] = f( dp[i-1], dp[i-2], ..., nums[i] )
```

Once you see a problem is 1-dimensional and about "sequence position", try
writing `dp[i] = f(dp[i-1], ...)` first — it resolves 60%+ of easy/medium
1D DP problems.

---

## PART 7 — 2D DP PATTERNS (grids)

### 7.1 Unique Paths
*"Robot at top-left of an m x n grid, can move only right or down. How many
distinct paths to bottom-right?"*

**State:** `dp[i][j]` = number of ways to reach cell `(i, j)`.
**Choices:** arrive from above (`i-1,j`) or from the left (`i,j-1`).
**Recurrence:** `dp[i][j] = dp[i-1][j] + dp[i][j-1]`
**Base case:** `dp[0][j] = 1` for all j (only one way: go right, right, right...),
`dp[i][0] = 1` for all i.

```java
static int uniquePaths(int m, int n) {
    int[][] dp = new int[m][n];
    for (int i = 0; i < m; i++) dp[i][0] = 1;
    for (int j = 0; j < n; j++) dp[0][j] = 1;
    for (int i = 1; i < m; i++)
        for (int j = 1; j < n; j++)
            dp[i][j] = dp[i - 1][j] + dp[i][j - 1];
    return dp[m - 1][n - 1];
}
```

Fill order visualized (arrows show dependency — each cell needs the cell
above and the cell to its left already computed):

```
dp[0][0] -> dp[0][1] -> dp[0][2] -> dp[0][3]
   |            |            |            |
   v            v            v            v
dp[1][0] -> dp[1][1] -> dp[1][2] -> dp[1][3]
   |            |            |            |
   v            v            v            v
dp[2][0] -> dp[2][1] -> dp[2][2] -> dp[2][3]
```
This is why the loop goes `for i (rows) { for j (cols) { ... } }` — row-major,
matching the arrows: left→right, top→bottom.

### 7.2 Minimum Path Sum (same grid shape, min instead of count)
```java
static int minPathSum(int[][] grid) {
    int m = grid.length, n = grid[0].length;
    int[][] dp = new int[m][n];
    dp[0][0] = grid[0][0];
    for (int j = 1; j < n; j++) dp[0][j] = dp[0][j - 1] + grid[0][j];
    for (int i = 1; i < m; i++) dp[i][0] = dp[i - 1][0] + grid[i][0];
    for (int i = 1; i < m; i++)
        for (int j = 1; j < n; j++)
            dp[i][j] = grid[i][j] + Math.min(dp[i - 1][j], dp[i][j - 1]);
    return dp[m - 1][n - 1];
}
```
Same skeleton as Unique Paths — swap `+` (counting) for `min` (optimization).
**Recognizing that "counting DP" and "optimization DP" on the same grid share
identical structure** is a huge time-saver.

### 7.3 Space optimization for 2D grids

Since `dp[i][j]` only needs the **current row** and the **previous row**, you
can collapse the 2D array into a single 1D array of size `n`:
```java
static int uniquePathsOptimized(int m, int n) {
    int[] dp = new int[n];
    Arrays.fill(dp, 1);                 // first row: all 1s
    for (int i = 1; i < m; i++)
        for (int j = 1; j < n; j++)
            dp[j] = dp[j] + dp[j - 1];  // dp[j] (old, = row above) + dp[j-1] (new, = same row, left)
    return dp[n - 1];
}
```
This "collapse 2D into 1D by overwriting in place" pattern is extremely
common once you're comfortable — always check: **does dp[i][j] depend only
on row i-1 and row i?** If yes, you can drop one dimension.

---

## PART 8 — THE KNAPSACK FAMILY (the most important pattern in DP)

If you master 0/1 Knapsack deeply, roughly 30-40% of all DP problems become
recognizable variants of it. This is the single highest-leverage pattern to
learn well.

### 8.1 0/1 Knapsack — the archetype
*"n items, each with a weight and a value. Capacity W. Each item can be taken
at most once. Maximize total value without exceeding W."*

**Step 1 (state):** `dp[i][w]` = max value achievable using the first `i`
items with capacity `w`.
**Step 2 (choices) at item i:** either **skip** it, or **take** it (only
possible if `weight[i] <= w`).
**Step 3 (recurrence):**
```
dp[i][w] = dp[i-1][w]                                    // skip item i
if weight[i] <= w:
   dp[i][w] = max(dp[i][w], value[i] + dp[i-1][w - weight[i]])   // take item i
```
**Step 4 (base case):** `dp[0][w] = 0` for all w (0 items -> 0 value).
**Step 5 (order):** i from 1 to n, w from 0 to W (both increasing).

```java
static int knapsack01(int[] weight, int[] value, int n, int W) {
    int[][] dp = new int[n + 1][W + 1];
    for (int i = 1; i <= n; i++) {
        for (int w = 0; w <= W; w++) {
            dp[i][w] = dp[i - 1][w];                          // skip
            if (weight[i - 1] <= w) {
                dp[i][w] = Math.max(dp[i][w],
                        value[i - 1] + dp[i - 1][w - weight[i - 1]]);  // take
            }
        }
    }
    return dp[n][W];
}
```

**Top-down version (often easier to derive first):**
```java
static int knap(int i, int w, int[] weight, int[] value, int[][] memo) {
    if (i == 0 || w == 0) return 0;
    if (memo[i][w] != -1) return memo[i][w];
    int skip = knap(i - 1, w, weight, value, memo);
    int take = weight[i - 1] <= w
              ? value[i - 1] + knap(i - 1, w - weight[i - 1], weight, value, memo)
              : Integer.MIN_VALUE;
    return memo[i][w] = Math.max(skip, take);
}
```

**Space optimization** — since `dp[i][*]` only depends on `dp[i-1][*]`,
collapse to 1D. **Critical detail:** iterate `w` from **high to low** so
you don't overwrite a value you still need this round (that would let an
item be "taken twice", breaking the 0/1 constraint):
```java
static int knapsack01Optimized(int[] weight, int[] value, int n, int W) {
    int[] dp = new int[W + 1];
    for (int i = 0; i < n; i++) {
        for (int w = W; w >= weight[i]; w--) {         // <-- right to left!
            dp[w] = Math.max(dp[w], value[i] + dp[w - weight[i]]);
        }
    }
    return dp[W];
}
```

### 8.2 Why direction of the inner loop matters (very common bug source)

```
0/1 Knapsack (each item once)  ->  loop w from HIGH to LOW
Unbounded Knapsack (infinite supply) -> loop w from LOW to HIGH
```
If you go low-to-high in 0/1 knapsack, `dp[w - weight[i]]` might already have
been updated *using item i itself* earlier in the same inner loop — meaning
item `i` gets counted twice. Going high-to-low guarantees `dp[w - weight[i]]`
still holds the value from the *previous* item iteration (the "old row").

### 8.3 Unbounded Knapsack
*"Same as above, but unlimited copies of each item allowed."*
```java
static int unboundedKnapsack(int[] weight, int[] value, int n, int W) {
    int[] dp = new int[W + 1];
    for (int w = 1; w <= W; w++) {
        for (int i = 0; i < n; i++) {
            if (weight[i] <= w) {
                dp[w] = Math.max(dp[w], value[i] + dp[w - weight[i]]);
            }
        }
    }
    return dp[W];
}
```
Because reuse is allowed, `dp[w - weight[i]]` is *supposed* to possibly
already include item `i` — hence low-to-high is correct here.

### 8.4 Knapsack variants — recognize these as the SAME pattern

| Problem | How it maps to Knapsack |
|---|---|
| **Subset Sum** ("can a subset sum to target?") | 0/1 Knapsack where value = weight, boolean dp instead of max |
| **Partition Equal Subset Sum** | Subset Sum with target = totalSum / 2 |
| **Target Sum** (+/- signs to reach target) | Transforms into a Subset Sum problem algebraically |
| **Coin Change (min coins)** | Unbounded Knapsack, minimizing instead of maximizing |
| **Coin Change 2 (count ways)** | Unbounded Knapsack, counting combinations |
| **Rod Cutting** | Unbounded Knapsack (cut length = "weight", price = "value") |

**Subset Sum (boolean DP) — worked example:**
```java
static boolean subsetSum(int[] nums, int target) {
    boolean[] dp = new boolean[target + 1];
    dp[0] = true;                                     // sum 0 always reachable (empty subset)
    for (int num : nums) {
        for (int t = target; t >= num; t--) {          // 0/1 -> high to low
            dp[t] = dp[t] || dp[t - num];
        }
    }
    return dp[target];
}
```

**Coin Change — minimum coins (unbounded, minimize):**
```java
static int coinChange(int[] coins, int amount) {
    int[] dp = new int[amount + 1];
    Arrays.fill(dp, Integer.MAX_VALUE - 1);   // -1 to avoid overflow on +1 below
    dp[0] = 0;
    for (int a = 1; a <= amount; a++) {
        for (int coin : coins) {
            if (coin <= a) dp[a] = Math.min(dp[a], dp[a - coin] + 1);
        }
    }
    return dp[amount] >= Integer.MAX_VALUE - 1 ? -1 : dp[amount];
}
```

**The master recognition rule:** whenever a problem says "n items, pick a
subset/combination under a weight/sum/capacity constraint, optimize or count
or check feasibility" — it's Knapsack. Then just decide: 0/1 or unbounded?
Max, count, or boolean?

---

## PART 9 — STRING DP: LCS, LIS, and EDIT DISTANCE FAMILY

String DP problems are almost always **2D DP over two indices (i, j)**, one
per string — or **1D DP over positions with a nested search**, for
subsequence problems on a single string (LIS).

### 9.1 Longest Common Subsequence (LCS) — the archetype for 2-string DP

**Step 1 (state):** `dp[i][j]` = length of LCS of `text1[0..i-1]` and
`text2[0..j-1]`.
**Step 2/3 (choices & recurrence):**
```
if text1[i-1] == text2[j-1]:
    dp[i][j] = 1 + dp[i-1][j-1]              // characters match, extend both
else:
    dp[i][j] = max(dp[i-1][j], dp[i][j-1])   // skip a char from either string
```
**Step 4 (base case):** `dp[0][j] = 0`, `dp[i][0] = 0` (empty string -> LCS 0).

```java
static int lcs(String text1, String text2) {
    int m = text1.length(), n = text2.length();
    int[][] dp = new int[m + 1][n + 1];
    for (int i = 1; i <= m; i++) {
        for (int j = 1; j <= n; j++) {
            if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                dp[i][j] = 1 + dp[i - 1][j - 1];
            } else {
                dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
    }
    return dp[m][n];
}
```

Grid intuition (rows = text1, cols = text2) — each cell only needs the cell
diagonally up-left, directly above, and directly left:
```
        ""  a   c   e
    ""   0  0   0   0
    a    0  1   1   1
    b    0  1   1   1
    c    0  1   2   2
    d    0  1   2   2
    e    0  1   2   3   <- dp[m][n] = LCS length = 3 ("ace")
```

### 9.2 Variants that reuse the LCS skeleton

| Problem | Change from LCS |
|---|---|
| **Longest Common Substring** | Only extend on match; reset to 0 (not carry max) on mismatch |
| **Edit Distance** | On match: `dp[i-1][j-1]`. On mismatch: `1 + min(insert, delete, replace)` = `1 + min(dp[i][j-1], dp[i-1][j], dp[i-1][j-1])` |
| **Shortest Common Supersequence length** | `m + n - LCS(text1, text2)` |
| **Delete Operations for Two Strings** | `m + n - 2*LCS(text1, text2)` |

**Edit Distance (full code, since the recurrence has 3 branches):**
```java
static int editDistance(String a, String b) {
    int m = a.length(), n = b.length();
    int[][] dp = new int[m + 1][n + 1];
    for (int i = 0; i <= m; i++) dp[i][0] = i;   // delete all i chars
    for (int j = 0; j <= n; j++) dp[0][j] = j;   // insert all j chars
    for (int i = 1; i <= m; i++) {
        for (int j = 1; j <= n; j++) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                dp[i][j] = dp[i - 1][j - 1];               // no operation needed
            } else {
                dp[i][j] = 1 + Math.min(dp[i - 1][j - 1],  // replace
                             Math.min(dp[i - 1][j],        // delete
                                       dp[i][j - 1]));      // insert
            }
        }
    }
    return dp[m][n];
}
```

### 9.3 Longest Increasing Subsequence (LIS) — single-string DP, O(n²)

**State:** `dp[i]` = length of the LIS **ending exactly at index i**.
**Recurrence:** `dp[i] = 1 + max(dp[j])` for all `j < i` where `nums[j] < nums[i]`
(if no such j, `dp[i] = 1`, the element alone).
**Base case:** every `dp[i]` starts at 1 (element alone is a subsequence).

```java
static int lengthOfLIS(int[] nums) {
    int n = nums.length;
    int[] dp = new int[n];
    Arrays.fill(dp, 1);
    int maxLen = 1;
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < i; j++) {
            if (nums[j] < nums[i]) {
                dp[i] = Math.max(dp[i], dp[j] + 1);
            }
        }
        maxLen = Math.max(maxLen, dp[i]);
    }
    return maxLen;
}
```
Notice the key difference from LCS: **`dp[i]` is not "the answer up to i" —
it's "the answer ENDING at i."** Then you take the max over all `dp[i]` at
the end. This "ending exactly at index i" state definition is a very
important and reusable trick for subsequence problems on one array (also
used in Maximum Sum Increasing Subsequence, Longest Bitonic Subsequence, etc.)

**Advanced O(n log n) LIS** (patience sorting with binary search — good to
know exists, more of an algorithmic trick than a DP pattern):
```java
static int lengthOfLISFast(int[] nums) {
    int[] tails = new int[nums.length];
    int size = 0;
    for (int num : nums) {
        int lo = 0, hi = size;
        while (lo < hi) {                       // binary search for insertion point
            int mid = (lo + hi) / 2;
            if (tails[mid] < num) lo = mid + 1; else hi = mid;
        }
        tails[lo] = num;
        if (lo == size) size++;
    }
    return size;
}
```

---

## PART 10 — ADVANCED PATTERNS

### 10.1 Interval DP (DP on ranges `[i, j]`)

Used when the problem asks about **an entire range/interval** and the answer
for a range depends on **splitting it at some point k**.

**Signal:** "Find min/max cost to fully process/merge/remove a range,"
especially with a **splitting or partitioning** flavor (matrix chain
multiplication, burst balloons, merging stones, palindrome partitioning).

**Matrix Chain Multiplication** — *"Given dimensions of matrices, find the
cheapest order to multiply them all."*

**State:** `dp[i][j]` = min cost to multiply matrices from `i` to `j`.
**Recurrence:** try every split point `k` between i and j:
```
dp[i][j] = min over k in [i, j-1] of:
             dp[i][k] + dp[k+1][j] + cost(i, k, j)
```
**Base case:** `dp[i][i] = 0` (single matrix, no multiplication needed).
**Order:** by increasing interval length (you need smaller intervals fully
solved before bigger ones that contain them).

```java
static int matrixChainOrder(int[] p) {  // p[i-1] x p[i] is dims of matrix i
    int n = p.length - 1;               // number of matrices
    int[][] dp = new int[n + 1][n + 1];
    for (int len = 2; len <= n; len++) {              // interval length
        for (int i = 1; i <= n - len + 1; i++) {
            int j = i + len - 1;
            dp[i][j] = Integer.MAX_VALUE;
            for (int k = i; k < j; k++) {
                int cost = dp[i][k] + dp[k + 1][j] + p[i - 1] * p[k] * p[j];
                dp[i][j] = Math.min(dp[i][j], cost);
            }
        }
    }
    return dp[1][n];
}
```
This "loop by increasing length, then loop start index, then loop split
point k" triple-loop structure is the **standard skeleton for all interval
DP** — memorize this shape, not just this specific problem.

```
Fill order for interval DP (n=4), diagonal by diagonal:
     j=1  j=2  j=3  j=4
i=1  [0]  <1>  <2>  <3>     <- numbers show FILL ORDER (by diagonal = interval length)
i=2   .   [0]  <1>  <2>
i=3   .    .   [0]  <1>
i=4   .    .    .   [0]
```

### 10.2 DP on Trees

**Signal:** "tree" + optimize/count something that depends on subtree
results (max path sum, house robber on a tree, diameter, independent set).

The state is naturally defined by **subtree rooted at node `x`**, and you
recurse into children first (post-order), combining their results — this is
just memoization where the "smaller subproblem" is a child subtree.

**House Robber III (rob houses arranged as a binary tree, no two adjacent
i.e. parent+child, allowed):**

**State:** for node `x`, compute **two values**: `rob` = max money if we
rob `x`, `notRob` = max money if we don't rob `x`.
**Recurrence:**
```
rob(x)    = x.val + notRob(left) + notRob(right)
notRob(x) = max(rob(left), notRob(left)) + max(rob(right), notRob(right))
```

```java
static int rob(TreeNode root) {
    int[] result = robHelper(root);
    return Math.max(result[0], result[1]);
}
// returns {withNode, withoutNode}
static int[] robHelper(TreeNode node) {
    if (node == null) return new int[]{0, 0};
    int[] left = robHelper(node.left);
    int[] right = robHelper(node.right);
    int withNode = node.val + left[1] + right[1];
    int withoutNode = Math.max(left[0], left[1]) + Math.max(right[0], right[1]);
    return new int[]{withNode, withoutNode};
}
```
This "return a small array/pair of states per node, computed post-order" is
the general template for tree DP.

### 10.3 Bitmask DP

**Signal:** small `n` (typically **n ≤ 20**), and the problem is about
"visiting all elements / trying every subset of elements" — e.g. Traveling
Salesman Problem (TSP), assigning tasks to workers.

The trick: represent **"which elements have been used/visited"** as an
integer bitmask (bit `i` = 1 means element `i` is used). State becomes
`dp[mask][i]`.

**TSP — state:** `dp[mask][i]` = min cost to have visited exactly the set of
cities in `mask`, currently standing at city `i`.
**Recurrence:**
```
dp[mask][i] = min over j in mask, j != i of:
                dp[mask without i][j] + cost(j, i)
```

```java
static int tsp(int[][] cost, int n) {
    int FULL = (1 << n) - 1;
    int[][] dp = new int[1 << n][n];
    for (int[] row : dp) Arrays.fill(row, Integer.MAX_VALUE / 2);
    dp[1][0] = 0;                                    // start at city 0, mask={0}
    for (int mask = 1; mask <= FULL; mask++) {
        for (int i = 0; i < n; i++) {
            if ((mask & (1 << i)) == 0) continue;      // i not visited in this mask
            if (dp[mask][i] == Integer.MAX_VALUE / 2) continue;
            for (int j = 0; j < n; j++) {
                if ((mask & (1 << j)) != 0) continue;  // j already visited
                int newMask = mask | (1 << j);
                dp[newMask][j] = Math.min(dp[newMask][j], dp[mask][i] + cost[i][j]);
            }
        }
    }
    int ans = Integer.MAX_VALUE;
    for (int i = 1; i < n; i++) ans = Math.min(ans, dp[FULL][i] + cost[i][0]);
    return ans;
}
```
Why `n <= 20` in practice: `2^20 ≈ 10^6` masks, times `n` states, times `n`
transitions — still fast enough. `2^30` would not be.

### 10.4 DP with an explicit State Machine (Stock Buy/Sell family)

**Signal:** problem has explicit "phases" or "modes" you can be in (holding
stock / not holding, cooldown, at most k transactions) — model each mode as
a separate DP array/dimension.

**Best Time to Buy/Sell Stock with Cooldown:**
**States:** `hold[i]` = max profit on day i while holding a stock,
`sold[i]` = max profit on day i, just sold today,
`rest[i]` = max profit on day i, not holding, not just sold (free to buy).

```
hold[i] = max(hold[i-1], rest[i-1] - price[i])     // keep holding, or buy today
sold[i] = hold[i-1] + price[i]                     // sell what we held
rest[i] = max(rest[i-1], sold[i-1])                // stay resting, or cooldown ends
```

```java
static int maxProfit(int[] prices) {
    int n = prices.length;
    if (n == 0) return 0;
    int hold = -prices[0], sold = 0, rest = 0;
    for (int i = 1; i < n; i++) {
        int prevHold = hold, prevSold = sold, prevRest = rest;
        hold = Math.max(prevHold, prevRest - prices[i]);
        sold = prevHold + prices[i];
        rest = Math.max(prevRest, prevSold);
    }
    return Math.max(sold, rest);
}
```
Drawing the state machine makes this trivial to derive:
```
        buy              sell
  REST -------> HOLD -------> SOLD
   ^                            |
   |________ cooldown __________|
```
Whenever a problem has "modes" like this, **draw the state machine first**,
then each arrow becomes one line of the recurrence.

---

## PART 11 — COMMON PITFALLS (debugging DP)

1. **Off-by-one in indices.** Especially with strings: `dp[i][j]` usually
   maps to `text.charAt(i-1)`, not `text.charAt(i)`, because `dp[0]` reserves
   the "empty prefix" state. This is the #1 source of bugs.

2. **Wrong loop direction in space-optimized 0/1 knapsack.** Covered above —
   must go high→low, or items get reused illegally.

3. **Forgetting to initialize base cases correctly**, especially boolean DP
   (`dp[0] = true`, not `false`) or min-DP (`dp[0] = 0`, others =
   `Integer.MAX_VALUE`, not 0 — or you'll wrongly think "0 cost" is
   reachable everywhere).

4. **Confusing "ending at i" vs "using first i elements."** In LIS, `dp[i]`
   means "subsequence ending exactly at i" — NOT "best LIS in the first i
   elements." Mixing these up breaks the recurrence.

5. **Recomputing states in memoization due to mutable/wrong cache keys.**
   Make sure your memo key captures the *entire* state (all dimensions that
   affect the answer) — a common bug is memoizing on `i` alone when the
   answer also depends on remaining capacity, a boolean flag, etc.

6. **Stack overflow in top-down recursion** for large `n` (Java's default
   stack is fairly small). If `n` is large (>10⁴–10⁵ with deep recursion),
   switch to bottom-up/iterative, or increase stack size via a new Thread
   with a larger stack.

7. **Not verifying with a tiny hand-traced example before scaling up.**
   Always trace `dp` for `n=2,3,4` by hand and check it matches
   brute-force output before trusting the code on large input.

---

## PART 12 — THE MASTER CHEAT SHEET

```
IDENTIFY:
  "num ways / min / max / longest / can we reach" + choices at each step
  + brute force shows repeated subproblems  =>  DP

FRAMEWORK (always in this order):
  1. Define dp[state] in one sentence
  2. List the choices available at that state
  3. Write the recurrence combining those choices
  4. Identify base case(s)
  5. Decide computation order (loop direction / recursion)

CHOOSE DIRECTION:
  Prototype the recurrence  -> Top-down memoization (easiest to derive)
  Need speed / no recursion -> Bottom-up tabulation
  Need to save memory       -> Check: does dp[i] need only last k states?
                                If yes -> collapse array to O(k) space.

PATTERN MAP:
  Fibonacci-shaped (dp[i] from dp[i-1], dp[i-2])   -> 1D sequence DP
  Grid, move right/down                             -> 2D grid DP
  Pick items under capacity                          -> Knapsack family
  Two strings, compare chars                         -> LCS / Edit Distance family
  One array, subsequence, ending at i                -> LIS family
  Range/interval, split into two parts               -> Interval DP
  Tree, combine children's results                   -> Tree DP
  Small n (<=20), track visited set                  -> Bitmask DP
  Explicit modes/phases (hold/sold/cooldown, etc.)    -> State Machine DP
```

---

## PART 13 — PRACTICE ROADMAP (Java, ordered by difficulty)

**Tier 1 — Build recursion → DP intuition**
1. Fibonacci Number
2. Climbing Stairs
3. House Robber / House Robber II
4. Min Cost Climbing Stairs

**Tier 2 — 2D grids + basic knapsack**
5. Unique Paths / Unique Paths II (with obstacles)
6. Minimum Path Sum
7. Triangle (min path sum, triangle shape)
8. 0/1 Knapsack (implement from scratch, don't look it up)
9. Partition Equal Subset Sum
10. Coin Change (min coins) & Coin Change II (count ways)

**Tier 3 — String DP**
11. Longest Common Subsequence
12. Longest Palindromic Subsequence
13. Edit Distance
14. Longest Increasing Subsequence (both O(n²) and O(n log n))
15. Word Break

**Tier 4 — Advanced**
16. Matrix Chain Multiplication
17. Burst Balloons (interval DP, trickier split logic)
18. Palindrome Partitioning II (min cuts)
19. House Robber III (tree DP)
20. Best Time to Buy/Sell Stock with Cooldown / with k transactions
21. Traveling Salesman Problem (bitmask DP)
22. Longest Common Subsequence of 3 strings (extend 2D to 3D DP)

**How to practice each one:** don't jump to the DP solution. Every time:
write brute-force recursion first → identify overlapping subproblems by
hand or by adding a print statement to count calls → memoize → convert to
tabulation → space-optimize if possible. Doing all 5 steps every time, even
when you "already know" the answer, is what builds the intuition that lets
you solve **novel** DP problems later, not just memorized ones.

---

## Summary

DP is not a bag of memorized templates — it is one repeatable thinking
process (brute force → spot repeated subproblems → cache them → optionally
flip to iteration → optionally shrink memory) applied to increasingly
elaborate *state definitions* (a single index, two indices, an index + a
boolean, an index + a bitmask, a subtree...). Once the 5-step framework in
Part 4 is automatic, every "pattern" in Parts 6-10 is just a different
flavor of the same five questions.
