# A Better Approach: Dissect → Restate → Ask → Break → Lock

Both documents list good questions, but neither shows you how to generate the questions from the problem in front of you. RICE and CLARITY are checklists to recite. This approach starts from the problem statement itself, because the statement tells you what to ask.

Rule for everything below: **ask only when the answer could change your algorithm or your edge-case handling. Otherwise, state an assumption and move on.**

---

## Step 1: Dissect the statement (silently, 30-60 seconds)

Split the statement into four parts:

| Part | Question to yourself | Example: "longest subarray with sum at most K" |
|---|---|---|
| **Data** | What am I given? | An integer array and an integer K |
| **Goal** | What exactly is asked? | The *length* (not the subarray, not indices) |
| **Condition** | What rule must the answer satisfy? | Contiguous, sum ≤ K |
| **Scale** | How big can things get? | Not stated, so I must ask |

Then circle the **keywords**. Each one carries a hidden ambiguity, and the next section shows how to decode them.

**Why this step matters:** most unasked questions come from not reading carefully. If Data, Goal, Condition, and Scale are each clear in your head, you only ask about the gaps.

---

## Step 2: Restate in your own words, with one example

> "So I'm given an array of integers and K, and I need to return the length of the longest contiguous subarray whose sum is at most K. For [1, 2, 1, 3] and K = 4, that would be [1, 2, 1], length 3. Is that right?"

**Why:** it catches misunderstandings in 10 seconds, buys you thinking time, and shows the interviewer you can communicate. A concrete example exposes misreadings that abstract wording hides.

---

## Step 3: Ask the generic questions (four buckets)

These apply to almost every problem. Skip whatever the statement already answers.

### Bucket A: Data (what can the input look like?)

| Question | Why we ask | What it changes |
|---|---|---|
| How large can n be? | Constraints limit which complexities are feasible | O(n²) vs O(n log n) vs O(n) |
| Value range? Negatives? Zeros? | Negatives break many "monotonic" tricks, and large values cause overflow | Sliding window vs prefix sum, `int` vs `long` |
| Sorted? Partially sorted? | Order is structure you can exploit | Binary search, two pointers vs hashing |
| Duplicates? | Duplicates change counting, hashing, and backtracking | Set vs map, skip-duplicate logic |
| Empty / null / single element? | Base cases and crash prevention | Early returns |
| Data type, character set (strings)? | Decides the data structure | `int[26]` vs `int[128]` vs HashMap |

### Bucket B: Goal (what exactly is the answer?)

| Question | Why we ask | What it changes |
|---|---|---|
| Value, index, or actual elements? | Reconstructing the answer costs more than computing its value | DP needs parent pointers only if elements are required |
| One answer, any answer, or all answers? | "All" usually means exponential search | Greedy/DP vs backtracking |
| Count or list? | Counting often has a polynomial solution, listing does not | DP vs enumeration |
| Which answer if several exist (smallest, lexicographic, first)? | Tie-breaking changes sorting and comparison | Comparator, iteration order |
| What if no answer exists? | You must pick a return value | -1, 0, empty list, or exception |

### Bucket C: Rules and operations (what am I allowed to do?)

| Question | Why we ask | What it changes |
|---|---|---|
| Can I modify the input? | In-place tricks are only valid if allowed | Sorting, marking visited, cyclic sort |
| Extra space allowed? | Some optimal solutions use O(n) memory | HashSet vs O(1)-space approaches |
| One call or many queries? | Many queries justify preprocessing | Prefix sums, sorting once, sparse table |
| Does data change between queries? | Static data allows simple preprocessing | Prefix sum vs Fenwick/segment tree |
| Streaming (can't see everything)? | You can't re-scan or sort | Heap, running stats, reservoir sampling |

### Bucket D: Guarantees (what can I rely on?)

| Question | Why we ask |
|---|---|
| Is a solution guaranteed to exist? | Decides whether you need a verification step (e.g., Boyer-Moore majority needs one if the majority may not exist) |
| Is the input guaranteed valid? | Decides whether you need validation code |

**Practical limit:** pick the 4-6 questions that matter for this problem. Ten questions look like stalling.

---

## Step 4: Break your first idea (this finds the pattern-specific questions)

This is the most useful habit in the whole guide:

```
Think of a likely approach
        ↓
"What input would break it?"
        ↓
Turn that input into a question
        ↓
Answer decides: keep the approach, or switch
```

Examples:

| First idea | What breaks it | Question to ask |
|---|---|---|
| Sliding window for subarray sum | Negative numbers | "Can values be negative?" |
| Recursion on a tree | Depth of 10⁵ causes stack overflow | "How deep can the tree be? Is it balanced?" |
| `int` sum | Overflow | "What's the value range?" |
| BFS for shortest path | Weighted edges | "Are edges weighted?" |
| Binary search | Not sorted or not monotonic | "Is it sorted?" |
| Backtracking for combinations | Duplicates make duplicate answers | "Can the input contain duplicates?" |

Notice that you ask these questions without asking the interviewer for the algorithm. You hold the hypothesis privately and probe the one fact it depends on.

---

## Step 5: Lock your assumptions out loud

> "I'll assume values can be negative, n is up to 10⁵, and I'll return 0 if no valid subarray exists. Let me know if any of that is wrong."

**Why:** if the interviewer says "you decide," you're covered, and if you're wrong they'll correct you now instead of after 20 minutes of coding. Write the assumptions as a comment at the top of your code.

---

## Decoding the statement: keywords to questions

This is how to break down the problem text. Look for these words:

| Word or phrase in the statement | Hidden ambiguity | Question | Algorithm impact |
|---|---|---|---|
| **subarray / substring** | Must it be contiguous? | "Contiguous, yes?" | Contiguous suggests window, prefix sum, Kadane. Non-contiguous suggests DP, greedy, subsets |
| **subsequence / subset** | Order kept? Duplicates distinct? | "Does relative order matter?" | DP vs backtracking vs bitmask |
| **longest / shortest / min / max** | Is there an optimization structure? | "Is there a size limit? Is this monotonic?" | Window, DP, greedy, binary search on answer |
| **at most K / at least K / exactly K** | Is the condition monotonic? | "Are values nonnegative?" | "At most" often works with a window. "Exactly K" becomes (at most K) minus (at most K-1) |
| **distinct / unique** | Unique values or unique results? | "Should duplicate answers be removed?" | Set, sort + skip duplicates |
| **minimum number of steps / moves / operations** | Are all steps equal cost? | "Does every move cost the same?" | Yes: BFS. Weighted: Dijkstra. Otherwise: DP |
| **path** | Simple path? Must it be shortest? Directed? | "Directed or undirected? Weights?" | BFS / DFS / Dijkstra / DP |
| **any / all / count** | Output size and style | "One, all, or just the count?" | Early exit vs enumeration vs DP |
| **in-place / constant space** | Is O(1) extra space required? | "May I use extra memory?" | Two pointers, cyclic sort, marking |
| **adjacent / consecutive / neighbors** | Which neighbors? | "4 or 8 directions? Wraparound?" | Grid traversal, DP transitions |
| **sorted / ordered** | Strictly? Ascending only? | "Duplicates in sorted data?" | First/last occurrence in binary search |
| **intervals / ranges** | Do endpoints touch? | "Is [1,3] and [3,5] an overlap?" | `<` vs `<=` |
| **k-th / top K** | Duplicates count separately? K vs n? | "Is K small relative to n? Streaming?" | Heap vs quickselect vs sort |
| **queries** | How many? Do updates happen? | "How many queries? Do updates happen?" | Preprocess vs per-query work |
| **cycle / connected** | Directed or undirected? | "Which kind of graph?" | DFS colors vs Union-Find |

**How to use this table in the interview:** read the statement once, circle every word on the left, and write the question next to it. You now have a statement-specific question list.

---

## Worked example: full walkthrough

**Problem:** "Given an integer array `nums` and an integer `k`, return the length of the longest subarray with sum at most `k`."

**1. Dissect**
- Data: integer array, integer k
- Goal: length (a number, not the subarray)
- Condition: **subarray** (contiguous), sum **at most** k
- Scale: unknown

**2. Keywords decoded**
- "subarray" means contiguous, so think window or prefix sum
- "longest" means maximize
- "at most" means a window works only if the sum moves predictably, which needs nonnegative values

**3. First idea:** sliding window. **What breaks it?** Negative numbers, since adding an element could reduce the sum.

**4. Questions to ask**
- "Can numbers be negative?" (the deciding question)
- "How large can n be?" (confirms we need O(n) or O(n log n))
- "If no valid subarray exists, do I return 0?" (e.g., all elements > k)
- "Is an empty subarray allowed?" (relates to the previous one)
- "What's the value range?" (use `long` for the sum if large)

**5. How the answers decide the algorithm**

| Answer | Algorithm |
|---|---|
| All nonnegative | Sliding window, O(n) time, O(1) space |
| Negatives allowed | Prefix sums with a monotonic structure and binary search, O(n log n). This is much harder, which is why the question matters |

Without asking, you could write a window solution that passes the examples and silently fails on negatives.

**Short mock dialogue:**

> **You:** "Just to confirm, contiguous subarray, return the maximum length. Can the numbers be negative?"
> **Interviewer:** "Yes, they can."
> **You:** "Then a sliding window won't work, since shrinking the window doesn't reliably reduce the sum. I'll use prefix sums instead. If no subarray qualifies, I'll return 0. And n up to around 10⁵ is fine?"

**A second example, quick:** "Minimum number of moves to reach the target cell in a grid."
- "minimum moves" → shortest path. Ask: "Does each move cost the same? Can I move diagonally? Are there obstacles? Is the target always reachable?"
- Uniform cost and 4 directions → BFS. Different costs → Dijkstra.

---

## Pattern-specific questions

Use these after you spot a candidate pattern, as the "what breaks it" probe from Step 4.

| Pattern | Questions | Why |
|---|---|---|
| **Sliding window** | All positive? Fixed or variable window? Is the condition monotonic when the window grows? | Window logic needs predictable behavior as you expand or shrink |
| **Two pointers** | Sorted? Duplicates? Indices or values? | Sorted input gives the pointer-move rule. Returning indices means you can't sort freely |
| **Binary search** | Sorted? Duplicates (first, last, any)? Can I binary search on the answer (is feasibility monotonic)? | Decides the boundary condition and loop template |
| **Prefix sum** | Many queries? Updates? Negatives? | Static data uses prefix sums. With updates use Fenwick/segment tree |
| **Hashing / frequency** | Duplicates? Small value range? | A small range lets you use an array instead of a map |
| **Stack / monotonic stack** | Next greater or smaller? Equal elements included or excluded? | Equality handling changes `<` vs `<=` |
| **Intervals** | Sorted? Touching intervals overlap? Nested? | Boundary comparison and whether to sort first |
| **Heap / Top K** | K small vs n? Streaming? Ties? Sorted output? | Heap of size K gives O(n log K) vs full sort O(n log n) |
| **Linked list** | Cycle? Singly or doubly? Return new head? Node identity vs value? | Affects Floyd's cycle, dummy-node usage, intersection logic |
| **Tree** | Binary tree or BST? Balanced? Duplicates? Depth? | BST allows ordered tricks. Deep trees risk stack overflow |
| **Graph** | Directed? Weighted? Negative weights? Cycles? Connected? Dense or sparse? | Picks BFS, Dijkstra, Bellman-Ford, topological sort, or Union-Find |
| **Grid** | 4 or 8 directions? Obstacles? Can I mutate the grid? | Neighbor loop, visited handling |
| **Backtracking** | Reuse elements? Order matter? Duplicates in input? All or one answer? | Decides `i` vs `i+1`, sorting, skip-duplicate logic |
| **DP** | Use items once or unlimited? Need value or the actual path? Strictly or non-strictly increasing (LIS)? | Changes transitions and iteration direction, and whether you store parents |
| **Greedy** | Is there a proof? Ties? Can I sort first? | Greedy needs a justification, so confirm the structure supports it |
| **Union-Find** | Edges only added, or removed too? | Union-Find handles adds, not removals |
| **Bit manipulation** | Signed or unsigned? Width? Negative numbers? | Java `int` is 32-bit signed, so shifts and overflow matter |
| **Design (LRU, iterator)** | Required complexity per operation? Capacity? Thread safety? Behavior on miss or empty? | Decides the data structures, e.g., hashmap + doubly linked list for O(1) LRU |

---

## Constraints as an algorithm hint

If constraints aren't given in the statement, ask for n. This is the highest-value question.

| n | Allowed complexity | Typical techniques |
|---|---|---|
| ≤ 10-12 | O(n!) | Permutations, brute force |
| ≤ 20-25 | O(2ⁿ) | Subsets, bitmask, meet in the middle |
| ≤ 100 | O(n³) | Floyd-Warshall, 3D DP |
| ≤ 1,000-5,000 | O(n²) | 2D DP, nested loops |
| ≤ 10⁵-10⁶ | O(n log n) or O(n) | Sort, heap, window, hashing, binary search |
| ≥ 10⁹ | O(log n) or O(1) | Math, binary search on answer |

These are heuristics for about 10⁸ simple operations per second, and language, constants, and test count also matter.

---

## Time budget

| Phase | Time |
|---|---|
| Dissect (silent) | 30-60 sec |
| Restate + example | 30 sec |
| Generic + pattern questions | 2-3 min |
| Lock assumptions | 30 sec |
| **Total** | **3-5 min** |

---

## Mistakes to avoid

- **Asking for the algorithm** ("Should I use a heap?"). Probe properties instead and reach the algorithm yourself.
- **Asking what the statement already says.** Re-read first.
- **Asking "any edge cases?"** Ask about specific ambiguities.
- **Silently assuming** sorted, positive, or non-empty input. Say it out loud.
- **Asking 10+ questions.** Prioritize the ones that change the algorithm.
- **Going quiet.** Think aloud, because interviewers grade reasoning.

---

## One-page recap

1. **Dissect:** Data, Goal, Condition, Scale. Circle keywords.
2. **Restate** with one example.
3. **Generic questions:** Data (size, range, order, duplicates, empty), Goal (value/index/all, none-case), Rules (modify, space, queries), Guarantees.
4. **Break your first idea:** what input kills it? Ask that.
5. **Lock assumptions** out loud, then move on to brute force, pattern, optimize, code, dry run, complexity.

I can also run a mock drill where I act as the interviewer and give you problems one at a time, so you practice dissecting and asking. Tell me which topics you want (arrays, graphs, DP, and so on) and I'll start. If you'd prefer this as a file for your DSA notes, I can make that too.







































# The Complete DSA Clarification Playbook

This keeps the structure you liked: decode, contract, constraints, uncertainties, brute force, bottleneck, pattern questions, justify, verify. It expands the two parts you asked about: **observation → pattern mapping** and **pattern-specific questions**.

**Core rule:** ask a question only if its answer changes correctness, the algorithm, the complexity, or the output handling. Otherwise state an assumption and move on.

**Three activities, never mixed:**
- **Clarify:** ask the interviewer about something the statement leaves open.
- **Discover:** reason privately about which technique fits.
- **Validate:** check that the technique's preconditions hold. This is where pattern-specific questions come from.

---

## Part 0: The spine (quick version)

| Step | What you do | Output |
|---|---|---|
| 1. Decode | Split the statement into Input, Operation, Objective, Condition, Output. Circle signal words. | A one-line restatement |
| 2. Contract | Fix the exact input, output, constraints, and allowed operations | Generic questions |
| 3. Extract | Separate what is **stated**, **implied**, **unknown-and-important**, and **unknown-and-harmless** | A short list of real questions |
| 4. Brute force | Describe the simplest correct solution and its complexity | A baseline |
| 5. Bottleneck | Find what is repeated, unordered, or recomputed | An observation |
| 6. Map | Match observations to candidate patterns (Part A) | 1-3 candidates |
| 7. Pattern questions | Verify each candidate's preconditions (Part B) | A chosen algorithm |
| 8. Justify | Explain why it applies, the invariant, and the complexity | Your approach speech |
| 9. Verify | Dry-run, edge cases, overflow | Then code |

---

## Part A: Map the observation to a pattern

You make observations from **six sources**. Train yourself to scan all six on every problem.

1. The words in the statement
2. The structure of the input
3. The constraints (n and value ranges)
4. The type of output
5. The workload (how the thing is used)
6. The bottleneck in your brute force

### A1. Observations from the words in the statement

| Word or phrase | What it signals | Candidate patterns | What to verify |
|---|---|---|---|
| **subarray / substring / contiguous / window** | Elements form one unbroken range | Sliding window, prefix sum, Kadane, monotonic deque | Negatives? Is the condition monotonic? |
| **subsequence** | Order kept, gaps allowed | DP (LIS, LCS, knapsack-like), greedy + binary search | Strict or non-strict? Need the sequence or the length? |
| **subset / combination** | Order irrelevant, choose some | Backtracking, bitmask, subset-sum DP | Size of n, reuse, duplicates |
| **permutation / arrangement** | Order matters | Backtracking with a used[] array, next-permutation | n ≤ 10? Duplicate values? |
| **partition / split into groups** | Divide into parts with a rule | DP, binary search on answer, greedy | Contiguous parts or arbitrary groups? |
| **longest / maximum length** | Maximize a size | Window, DP, binary search on answer, hashmap of first index | Can it be framed as a valid-window check? |
| **shortest / minimum length** | Minimize a size | Window (shrink while valid), BFS, DP | Nonnegative values? Unweighted steps? |
| **minimum number of moves / steps / operations** | Shortest path in a state graph | BFS, DP, Dijkstra | Are all moves equal cost? |
| **maximum / minimum cost or value** | Optimization | DP, greedy, binary search on answer, heap | Is there optimal substructure? Does greedy have a proof? |
| **count the number of ways** | Counting, not listing | DP, combinatorics, prefix sum + frequency map | Modulo required? Overlapping subproblems? |
| **return all / generate all / list every** | Output is exponential or large | Backtracking, BFS over states | Output size, pruning, duplicate results |
| **is it possible / can we / does there exist** | Boolean feasibility | DP (boolean), greedy, graph search, hashing | Can you exit early? |
| **at most K** | Upper bound on a quantity | Sliding window (if monotone), DP with a K dimension | Nonnegative? |
| **at least K** | Lower bound | Window with a "satisfied" flag, binary search | Monotone when the window grows? |
| **exactly K** | Equality | `atMost(K) − atMost(K−1)`, prefix sum + hashmap | Counting or existence? |
| **distinct / unique** | Dedup rule | HashSet, sorting + skip, window with a map | Unique values, or unique result sets? |
| **kth smallest / largest** | Rank query | Heap of size k, quickselect, binary search on value, counting | k vs n, streaming, duplicates counted separately? |
| **top K / most frequent** | Selection by score | Frequency map + heap, bucket sort | Ties? Need sorted output? |
| **median / running median** | Order statistic over a stream | Two heaps, balanced BST, counting if the range is small | Static or streaming? |
| **next greater / smaller / span / nearest** | Nearest element with a property | Monotonic stack | Strict or non-strict? Circular? |
| **sorted / rotated / mountain / peak** | Order structure | Binary search variants | Duplicates? Strictness? |
| **in-place / O(1) space** | Memory restriction | Two pointers, cyclic sort, index marking, Floyd | May I mutate the input? |
| **overlap / merge / schedule / meeting rooms** | Interval logic | Sort by start or end, sweep line, heap | Touching endpoints? |
| **adjacent / neighbor / cell / island / region** | Grid connectivity | DFS/BFS, Union-Find, grid DP | 4 or 8 directions? Mutable grid? |
| **path** | Route between nodes | BFS, DFS, Dijkstra, DP on DAG, backtracking | Directed? Weighted? Simple path? |
| **dependency / prerequisite / order of tasks** | Precedence relation | Topological sort | Cycle possible? Any valid order or a specific one? |
| **connected / group / component / friend circle** | Equivalence relation | Union-Find, DFS/BFS | Dynamic edges? |
| **cycle** | Loop detection | Floyd (linked list), DFS colors (directed), Union-Find (undirected) | Directed or undirected? |
| **prefix / dictionary / autocomplete / starts with** | Prefix queries | Trie | Many queries? Deletions? |
| **palindrome** | Symmetry | Two pointers, expand around center, DP, Manacher | Substring or subsequence? Count or longest? |
| **anagram / permutation of string / same characters** | Frequency equality | Frequency array or map, fixed window | Character set? |
| **XOR / bits / power of two / single number** | Bit-level structure | Bit manipulation | Signed? Width? |
| **range query / sum between i and j** | Repeated range work | Prefix sum, Fenwick, segment tree, sparse table | How many queries? Updates? |
| **design / implement a class / O(1) operations** | Data structure composition | HashMap + linked list (LRU), HashMap + array (RandomizedSet), heap, deque | Required complexity per operation? |
| **modulo 10^9+7** | The answer is huge | DP / combinatorics | The count is large, so no enumeration |
| **minimize the maximum / maximize the minimum** | Min-max structure | Binary search on answer + feasibility check | Is feasibility monotone? |
| **stream / online / one pass** | Can't revisit | Heap, running stats, reservoir sampling, Boyer-Moore | Memory limit? |

### A2. Observations from the structure of the input

| Observation | What it enables | Pattern |
|---|---|---|
| Sorted array | Discard half or move pointers by comparison | Binary search, two pointers, merge |
| Sorted + find pair/triple | Narrow from both ends | Two pointers (3Sum = sort + pointers) |
| Rotated sorted array | One half is always sorted | Modified binary search |
| Unsorted, need fast "have I seen X?" | O(1) lookup | HashSet / HashMap |
| Values are in [1..n] or [0..n−1] | Values double as indices | Cyclic sort, index marking, Floyd on the array |
| Small value range (say ≤ 10⁵) or lowercase letters | Index by value | Counting array, bucket sort, `int[26]` |
| All elements positive or nonnegative | Sums only grow when you extend | Sliding window, Dijkstra-like greedy |
| Negatives or zeros present | Sums can shrink when you extend | Prefix sum + map, Kadane, deque on prefix sums |
| Many repeated values | Compress | Frequency map, run-length encoding |
| Binary array (0/1) | Reduce to counting | Map 0 → −1 then prefix sum, window on count |
| Tree structure | Recursion on children | DFS, BFS, tree DP |
| BST | Left < node < right | Inorder gives sorted order, guided search |
| Linked list | No random access | Fast/slow pointers, dummy node, reversal |
| DAG | No cycles | Topological order, DP over the order |
| Graph with unit edges | Distance = edge count | BFS |
| Graph with nonnegative weights | Closest-first greedy is valid | Dijkstra |
| Graph with negative weights | Dijkstra invalid | Bellman-Ford, or DP if a DAG |
| Weights only 0 and 1 | Special case | 0-1 BFS with a deque |
| Grid with obstacles | Implicit graph | BFS/DFS, DP |
| Intervals | Order by one endpoint | Sort + sweep, greedy, heap |
| Matrix with sorted rows and columns | Staircase search | Two pointers from a corner, or binary search per row |
| Strings with a small alphabet | Fixed-size count | `int[26]`/`int[128]` window |
| Circular array | Wraparound | Double the array, or modulo indexing |
| Parent pointers present | Walk upward | LCA via two pointers, path logic |
| Input is a function or API (e.g., `isBad(version)`) | Monotone predicate | Binary search on the answer |

### A3. Observations from the constraints

Assume roughly 10⁸ simple operations per second. These are heuristics only.

| Constraint | Feasible complexity | What to think about |
|---|---|---|
| n ≤ 10 | O(n!) | Permutations, brute force |
| n ≤ 20 | O(2ⁿ), O(n·2ⁿ) | Subsets, bitmask DP, backtracking with pruning |
| n ≤ 40 | O(2^(n/2)) | Meet in the middle |
| n ≤ 100 | O(n³), sometimes O(n⁴) | Floyd-Warshall, interval DP, 3D DP |
| n ≤ 500 | O(n³) | Floyd-Warshall, cubic DP |
| n ≤ 5,000 | O(n²) | 2D DP, LCS, edit distance, nested loops |
| n ≤ 10⁵ | O(n log n) | Sorting, heap, binary search, segment tree, window |
| n ≤ 10⁶ | O(n), O(n log n) with a small constant | Linear scan, hashing, two pointers, counting |
| n ≤ 10⁷ | O(n) | Single pass, sieve |
| n up to 10⁹ or 10¹⁸ | O(log n), O(√n), O(1) | Math, fast power, binary search on the answer |
| Value of target or W ≤ 10³–10⁴ | O(n·W) | Knapsack / subset-sum DP (pseudo-polynomial) |
| Grid of size m×n up to ~1000×1000 | O(mn) | BFS/DFS, grid DP |
| Many queries (q up to 10⁵) | O(log n) or O(1) per query | Preprocess: prefix sum, sparse table, Fenwick, segment tree |
| Values up to 10⁹, n up to 10⁵ | Sum can exceed 2³¹ | Use `long` in Java |

### A4. Observations from the type of output

| Output | Implication |
|---|---|
| Boolean | Early exit allowed. Boolean DP or greedy may suffice. |
| A single number (count, length, min, max) | You rarely need to reconstruct. DP stores values only. |
| An index or pair of indices | Don't sort without tracking original indices. |
| The actual subarray, path, or sequence | You must store parents or predecessors, or backtrack the DP table. |
| One valid answer (any) | Stop at the first success. |
| All valid answers | Exponential output possible. Pruning and dedup matter. |
| A count of answers | Avoid enumeration. DP or combinatorics. |
| A lexicographically smallest or specific answer | Tie-breaking: sorting order, greedy choice order, comparator. |
| A modified input in-place | Pointer discipline. Careful with overwrites. |
| A new data structure (design) | Per-operation complexity is the real requirement. |

### A5. Observations from the workload

| Workload | Implication |
|---|---|
| One query on static data | A single scan is enough. |
| Many queries on static data | Preprocess once: prefix sums, sorted array + binary search, sparse table. |
| Many queries with point updates | Fenwick tree or segment tree. |
| Many queries with range updates | Difference array (if offline), lazy segment tree. |
| Offline queries (all known upfront) | Sort queries, Mo's algorithm, offline Union-Find. |
| Online queries (answer immediately) | Persistent or incremental structures. |
| Only insertions | Union-Find, heaps, running structures. |
| Insertions and deletions | Balanced BST / TreeMap, heap with lazy deletion, linked structures. |
| Stream where you can't store everything | Heap of size k, reservoir sampling, Boyer-Moore, running stats. |

### A6. Observations from the bottleneck in brute force

This is the most powerful source for unfamiliar problems. Write the brute force, find the expensive part, and name it.

| Bottleneck you notice | Fix | Pattern |
|---|---|---|
| "I search for a matching element repeatedly" | Store seen elements | HashMap / HashSet |
| "I recompute sums over overlapping ranges" | Store cumulative values | Prefix sum |
| "I recompute the window from scratch each step" | Update incrementally | Sliding window |
| "I compare every pair, but order tells me which can't work" | Use ordering to prune | Sort + two pointers |
| "I scan to find a position in sorted data" | Halve the range | Binary search |
| "I test every candidate answer value" | Feasibility is monotone | Binary search on answer |
| "I look back to find the nearest bigger/smaller element" | Maintain a candidate stack | Monotonic stack |
| "I keep finding the max/min of a changing set" | Use a priority structure | Heap |
| "I sort everything but need only k items" | Partial selection | Heap of size k, quickselect |
| "The same recursive call appears with the same arguments" | Cache results | Memoization → DP |
| "I explore many branches and many are clearly doomed" | Cut early | Backtracking with pruning |
| "At each step one choice is clearly best, and I can argue why" | Commit to it | Greedy |
| "I recompute the shortest distance to the same node" | Process closest first, once | BFS, Dijkstra |
| "I repeatedly check whether two things are in the same group" | Track groups | Union-Find |
| "I repeatedly check whether a string starts with a prefix" | Share prefixes | Trie |
| "I rescan a range for min or max on every query" | Precompute | Sparse table, segment tree |
| "I re-sum a range after each point update" | Logarithmic update | Fenwick / segment tree |
| "I compare overlapping substrings character by character" | Reuse matches | KMP, Z-function, rolling hash |
| "I try all pairs of intervals" | Sort and sweep | Sort by start, sweep line |
| "My recursion recomputes subtrees" | Return a tuple or use the post-order result | Tree DP |
| "I treat each state independently but states form a graph" | Use graph search | BFS over states |
| "O(n²) with n = 10⁵" | Look for monotonicity or a data structure | Window, deque, heap, segment tree |

### A7. The "trap" list: observations that look like a pattern but aren't

| Looks like | Why it can mislead | Check |
|---|---|---|
| Subarray → sliding window | Fails with negatives | Is expanding monotone? |
| Sorted → binary search | Sortedness alone isn't enough if you need all pairs | Does a comparison discard a half? |
| Shortest path → Dijkstra | Fails with negative edges | Are weights nonnegative? |
| Shortest path → BFS | Fails on weighted edges | Are all edges equal cost? |
| Optimization → DP | Might be greedy or math | Are there overlapping subproblems? |
| Local best choice → greedy | Local best may be globally wrong | Can you prove an exchange argument? |
| "All subsets" → always 2ⁿ | If you only need a count or sum, math or DP is faster | Count or list? |
| Tree → recursion | Depth 10⁵ can overflow the stack | Max depth? |
| Interval → sort by start | Some problems need sort by end (max non-overlapping) | What does the greedy choose? |
| Two sum → hashmap | If sorted and O(1) space is required, use pointers | Space limit? |
| Frequency problem → hashmap | A small alphabet needs only an array | Value range? |
| Kth largest → heap | Quickselect gives O(n) average | Streaming? Allowed to mutate? |

---

## Part B: Pattern-specific questions

For every pattern: **when it applies**, **preconditions to verify**, **questions with the reason**, and **traps and variants**. Ask only what the statement leaves open.

### B1. Hashing (HashMap / HashSet / frequency)

**Applies when:** you repeatedly ask "have I seen X?", need counts, or need to match complements.

| Question | Why | What changes |
|---|---|---|
| Can duplicates exist, and do they count separately? | Affects set vs map and pair counting | HashSet → frequency map |
| Do I return values or indices? | Indices need storage | Map value → index |
| If duplicates exist, which index (first/last)? | Overwrite policy | `put` vs `putIfAbsent` |
| Is the value range small? | Array beats map | `int[]` indexing |
| Can I use extra space? | Hash needs O(n) | Else sort + pointers |

**Variants:** existence (set), count (frequency map), longest (store first index), grouping (key = canonical form, such as a sorted string or a count signature).
**Traps:** the same element used twice in Two Sum; Java `Integer` boxing equality; hash collisions on adversarial input (rare in interviews).

### B2. Two pointers

**Applies when:** sorted data, pair/triple targets, in-place partition, merging, or palindromic comparison.

| Question | Why | What changes |
|---|---|---|
| Is the input sorted, or may I sort it? | The pointer-move rule depends on order | Sorting costs O(n log n) |
| Do I need original indices? | Sorting destroys them | Store (value, index) pairs |
| Duplicates: should duplicate results be removed? | Skip logic | `while (nums[l]==nums[l+1]) l++` |
| One pair or all pairs? | Stop vs continue | Loop structure |
| Is in-place O(1) space required? | Same-direction (read/write) pointers | Fast-slow write pointer |

**Types:** opposite ends (pair sum, container with most water, palindrome), same direction (remove duplicates, move zeroes), fast-slow (cycle, middle), two arrays (merge, intersection).
**Invariant:** the unexplored region still contains every possible answer.
**Trap:** pointers on an unsorted array with no justification.

### B3. Sliding window

**Applies when:** the answer is a contiguous range and validity changes predictably as you grow or shrink it.

| Question | Why | What changes |
|---|---|---|
| Fixed size k or variable size? | Fixed: slide by one. Variable: expand/shrink loop | Template choice |
| Can values be negative (sum conditions)? | Without nonnegativity, shrinking doesn't reliably fix the condition | Window → prefix sum + map / deque |
| Is the condition monotone when growing? | Needed to justify shrinking | Otherwise window fails |
| Longest or shortest valid window? | Longest: record after fixing. Shortest: record while valid | Where you update the answer |
| Count windows or find the best one? | Counting: add `right − left + 1` per step | Counting formula |
| Exactly K? | Convert: `atMost(K) − atMost(K−1)` | Two window passes |
| Character set size? | Map vs `int[128]` | Memory |

**Variants:** fixed (max sum of k), longest with at most k distinct, shortest covering window (minimum window substring), count of subarrays with at most K, permutation-in-string (fixed window + frequency match).
**Trap:** applying it to "sum equals K" with negatives. Use prefix sum + HashMap.

### B4. Prefix sum / difference array

**Applies when:** repeated range sums, or "subarray sum equals / divisible by K".

| Question | Why | What changes |
|---|---|---|
| How many queries? | One query doesn't need preprocessing | Scan vs prefix array |
| Are there updates? | Prefix sums break on updates | Fenwick / segment tree |
| Negatives allowed? | Decides window vs prefix + map | Algorithm |
| Existence, count, or longest/shortest? | Map stores boolean, frequency, or earliest index | HashMap contents |
| Is the target a sum, a remainder (mod K), or a balance (0 = −1)? | Remainder: store `sum % k`, normalize negatives | `((s % k) + k) % k` |
| Range of values and n? | Overflow | `long` prefix |
| 2D matrix? | 2D prefix, inclusion-exclusion | `P[r2][c2] − P[r1−1][c2] − P[r2][c1−1] + P[r1−1][c1−1]` |
| Range **updates**, point queries offline? | Difference array | Add at l, subtract at r+1 |

**Trap:** forgetting to seed the map with `{0: −1}` or `{0: 1}`.

### B5. Binary search

Three distinct flavors. Identify which one you're in.

**(a) Classic search in sorted data**

| Question | Why |
|---|---|
| Sorted ascending or descending? | Comparison direction |
| Duplicates? First, last, or any occurrence? | Choose `lower_bound` or `upper_bound` style |
| What to return if not found? | −1 or the insertion position |
| Rotated or mountain-shaped? | Compare to the pivot or the neighbor |
| Possible overflow in `mid`? | `lo + (hi − lo) / 2` |

**(b) Boundary search (first true in a monotone predicate)**
Question: *"Is there a predicate that flips from false to true exactly once?"* Use the `while (lo < hi)` template.

**(c) Binary search on the answer**

| Question | Why |
|---|---|
| What is the smallest and largest possible answer? | Defines `lo` and `hi` |
| Can I write `feasible(x)` in O(n) or O(n log n)? | Total cost = `log(range) × cost(feasible)` |
| If `x` is feasible, is `x+1` (or `x−1`) also feasible? | Monotonicity is the entire justification |
| Minimize the max, or maximize the min? | Determines which side to shrink |
| Integer or real answer? | Loop termination and precision |

**Classic problems:** Koko eating bananas, capacity to ship, split array largest sum, aggregate cows, sqrt(x).
**Trap:** infinite loops from `lo = mid` with floor division.

### B6. Sorting + greedy

**Applies when:** a canonical order exposes the best choice.

| Question | Why |
|---|---|
| Can I reorder or sort the input? | Index-sensitive problems may forbid it |
| What do I sort by? | Interval scheduling sorts by end time; merging sorts by start |
| Ties: how are they broken? | Affects correctness |
| Is there an exchange argument? | Without a proof, greedy is a guess |
| Is the greedy choice safe, or can it paint me into a corner? | If not, use DP |

**Classic:** activity selection, jump game, gas station, assign cookies, task scheduler, minimum platforms.

### B7. Intervals / sweep line

| Question | Why | What changes |
|---|---|---|
| Are they sorted? | Sorting costs O(n log n) | Preprocessing |
| Closed `[a,b]` or half-open `[a,b)`? Do touching intervals overlap? | `<` vs `<=` | Core comparison |
| Can intervals be nested or identical? | Merge logic | `max(end, ...)` |
| Is `start > end` possible? | Validation | Pre-check |
| Merge, insert, count overlaps, or find max concurrency? | Different tools | Sort+merge / sweep with events / heap of ends |
| Tie order at the same coordinate (end before start)? | Sweep events | Event sorting |
| New list or in place? | Output handling | |

**Variants:** merge intervals, insert interval, meeting rooms I/II (heap of end times, or sweep), non-overlapping intervals (sort by end), employee free time, skyline.

### B8. Stack and monotonic stack / deque

**Plain stack applies to:** matching parentheses, expression evaluation, undo, DFS simulated iteratively, nested structure decoding.

| Question | Why |
|---|---|
| Which bracket types? Can the input be empty? | Edge cases |
| Operator precedence? Unary minus? Spaces? | Expression parsing |
| Integer division semantics (truncate toward zero)? | Java `/` matches truncation toward zero |

**Monotonic stack applies to:** next greater/smaller element, daily temperatures, largest rectangle in histogram, trapping rain water, stock span, sum of subarray minimums.

| Question | Why | What changes |
|---|---|---|
| Next or previous? Greater or smaller? | Decides the stack order | Increasing vs decreasing stack |
| Strict or non-strict with equals? | Duplicates | `<` vs `<=` when popping |
| Circular array? | Wraparound | Iterate `2n` with `% n` |
| Need values or indices? | Width/distance needs indices | Store indices |
| What if none exists? | Default | −1 or `n` |

**Monotonic deque** applies to sliding window max/min, and shortest subarray with sum ≥ K **with negatives** (deque over prefix sums).

### B9. Heap / priority queue / top-K

| Question | Why | What changes |
|---|---|---|
| Kth element or top K elements? | Different outputs | Heap top vs heap contents |
| K small relative to n? | O(n log K) beats O(n log n) | Heap size K |
| Streaming or static? | Static allows quickselect | Algorithm |
| Largest or smallest? | Heap type | Min-heap of size K for K largest |
| Ties? Duplicates counted separately? | Comparator | Frequency then value |
| Sorted output? | Extra sort of K | O(K log K) |
| Need deletions of arbitrary elements? | Java `PriorityQueue.remove` is O(n) | Lazy deletion or TreeMap |

**Variants:** merge K sorted lists (heap of heads), running median (two heaps), task scheduling (max-heap of counts), Dijkstra (min-heap of distance), meeting rooms II.
**Trap:** Java comparator overflow when writing `a - b`. Use `Integer.compare`.

### B10. Quickselect and counting selection

Ask: Is average O(n) acceptable, or is worst-case O(n log n) required? May I mutate the array? Is the value range small (counting or bucket)? Static data?

### B11. Linked lists

| Question | Why | What changes |
|---|---|---|
| Singly or doubly linked? | Pointer ops | Reversal logic |
| Can the head be null? Single node? | Edge cases | Dummy node |
| May I modify links, or must I preserve the list? | In-place vs copy | Reversal allowed? |
| Possible cycle? | Infinite loops | Floyd's algorithm |
| Node identity or value equality (intersection)? | Identity needs reference comparison | `==` vs `.equals` |
| Return a new head? | Head may change | Dummy node |
| Is the list sorted? | Merge logic | Two pointers |

**Techniques:** dummy head, fast/slow (middle, cycle start), reversal, split + reverse + merge (reorder list), merge two sorted, k-group reversal.

### B12. Recursion and backtracking

| Question | Why | What changes |
|---|---|---|
| One, all, or count? | Early exit; count may be DP | Return type |
| Can elements be reused? | Reuse → same index. No reuse → `i + 1` | Recursive call |
| Does order matter? | Permutation vs combination | `used[]` vs start index |
| Duplicates in input? Duplicates in output? | Dedup rule | Sort + `if (i > start && a[i]==a[i-1]) continue` |
| All positive? | Safe to prune when the sum exceeds the target | Pruning rule |
| Fixed length or variable? | Termination | Base case |
| Maximum n and depth? | Feasibility | Pruning needed? |
| Constraints between choices (N-Queens, Sudoku)? | Validity check | Constraint sets |

**Template thinking:** state = (position, current path, remaining), choices, constraints, goal, undo.
**Variants:** subsets, subsets II, permutations I/II, combination sum I/II/III, palindrome partitioning, N-Queens, word search, letter combinations, generate parentheses.
**Complexity note:** output size sets the lower bound.

### B13. Dynamic programming

**Step zero (private):** define the **state**, the **decision**, the **transition**, the **base case**, and the **order of evaluation**.

**Generic questions**

| Question | Why |
|---|---|
| Optimize, count, or decide feasibility? | Max/min vs sum vs boolean |
| Do I need only the value or the actual path? | Parent pointers |
| Modulo required? | Overflow and mod placement |
| Constraint sizes (n, W, k)? | Dimensions of the table |

**By DP family**

| Family | Key questions | What changes |
|---|---|---|
| **1D / linear (house robber, climbing stairs, decode ways)** | What are the allowed steps? Adjacent restrictions? | Transition set |
| **0/1 knapsack / subset sum / partition** | Each item once? Negative values? Target bounded? | 1D loop **backward** over capacity |
| **Unbounded knapsack / coin change** | Unlimited reuse? Count combinations or permutations? | Loop **forward**. Order of loops (coins outer = combinations, amount outer = permutations) |
| **LIS** | Strictly or non-strictly increasing? n large? | O(n²) DP vs O(n log n) patience sorting (`lower_bound` vs `upper_bound`) |
| **LCS / edit distance / string DP** | Which operations and costs? Substring or subsequence? | 2D state `(i, j)` |
| **Grid DP** | Allowed directions? Obstacles? Min cost or count? | Transition and boundary |
| **Interval DP (burst balloons, matrix chain)** | Does the answer depend on splitting a range? | State `(l, r)`, loop by length |
| **Bitmask DP (TSP, assignment)** | n ≤ 20? Visit each once? | State `(mask, last)` |
| **Digit DP** | Count numbers ≤ N with a property? | State `(pos, tight, ...)` |
| **Tree DP** | Can a node's answer come from its children? Include/exclude a node? | Post-order, return a pair |
| **DP on DAG / state machines (stocks)** | Number of transactions? Cooldown? Fee? | Extra state dimension |
| **Partition DP (split into k parts)** | Contiguous? k as a dimension? | State `(i, k)`; often also binary-searchable |
| **Counting paths / ways** | Mod? Walls? | Add instead of max |

**Space optimization question:** do I only need the previous row or two values? If so, roll the array.
**Trap:** writing code before defining the state.

### B14. Greedy

| Question | Why |
|---|---|
| Is there optimal substructure plus a greedy-choice property? | Without both, it's a guess |
| Can I state an exchange argument or a counterexample check? | Interview credibility |
| What ordering or heap drives the choice? | Sort key or priority |
| Are choices independent or does a choice constrain future ones? | If constrained, DP |
| Ties? | Tie-breaking can matter |

**Test habit:** try a small counterexample (3-4 elements) before committing.

### B15. Trees (binary trees and BSTs)

| Question | Why | What changes |
|---|---|---|
| Binary tree or BST? | BST ordering prunes search | Guided traversal / inorder |
| Duplicates allowed (in BST, left or right)? | Comparison rule | `<` vs `<=` |
| Balanced? Maximum depth? | Recursion stack safety | Iterative DFS, Morris traversal |
| Empty tree? Single node? | Base case | Null check |
| Node values or node references? | Identity vs value | LCA, subtree problems |
| Path: root-to-leaf, any-to-any, downward only? | Different DP/DFS | Global max updated at each node |
| Negative node values? | Max path sum logic | Clamp at zero |
| Parent pointers? | Upward walk | LCA like linked-list intersection |
| One query or many? | Preprocessing | Binary lifting / Euler tour |
| Return a value, a list, or a modified tree? | | |

**Traversal choice:** level-wise → BFS; path-sum / root-to-leaf → DFS with backtracking; height/diameter/balance → post-order returning info; BST validity → inorder or bounds; serialization → pre-order with null markers; kth smallest in BST → inorder.
**Trap:** BST validity by comparing only to the parent, instead of using bounds.

### B16. Graphs

**Universal graph questions (ask these every time)**

| Question | Why |
|---|---|
| Directed or undirected? | Cycle detection, connectivity, edge storage |
| Weighted? Negative weights? | Algorithm family |
| Cycles possible? DAG? | Topological order, DP feasibility |
| Connected? Multiple components? | Loop over all start nodes |
| Self-loops or parallel edges? | Duplicates in adjacency |
| V and E sizes? Dense or sparse? | Adjacency list vs matrix, complexity |
| Input format (edge list, matrix, adjacency)? | Conversion cost |
| Nodes labeled 0..n−1 or arbitrary? | Map labels to indices |
| Distance, path, existence, count, or ordering? | Output |
| Static or changing edges? | Union-Find vs recomputing |

**Algorithm chooser**

| Need | Condition | Algorithm | Complexity |
|---|---|---|---|
| Shortest path | Unweighted | BFS | O(V+E) |
| Shortest path | Weights 0/1 | 0-1 BFS | O(V+E) |
| Shortest path | Nonnegative weights | Dijkstra (heap) | O((V+E) log V) |
| Shortest path | Negative weights, one source | Bellman-Ford | O(VE) |
| Shortest path | Negative weights, detect negative cycle | Bellman-Ford | O(VE) |
| Shortest path | DAG | Topological order + relax | O(V+E) |
| Shortest path | All pairs, small V | Floyd-Warshall | O(V³) |
| Shortest path | Bounded stops (at most K edges) | Bellman-Ford with K rounds, or BFS with state | |
| Reachability / components | Static | DFS / BFS | O(V+E) |
| Components | Edges arriving over time | Union-Find | ~O(α(n)) per op |
| Cycle | Undirected | Union-Find or DFS with parent | |
| Cycle | Directed | DFS with colors, or Kahn count | |
| Ordering by dependency | DAG | Kahn's BFS or DFS post-order | O(V+E) |
| Minimum spanning tree | Weighted undirected | Kruskal (sort + UF) or Prim (heap) | |
| Bipartite check | Undirected | BFS/DFS 2-coloring | |
| Strongly connected components | Directed | Kosaraju or Tarjan | |
| Bridges / articulation points | Undirected | Tarjan low-link | |
| Max flow / matching | Capacities / pairing | Ford-Fulkerson / Hopcroft-Karp | |
| Shortest path in a state space (puzzles, word ladder) | Each move cost 1 | BFS over states (consider bidirectional BFS) | |

**Extra questions per task**
- **Word ladder / state-space BFS:** What defines a state? Size of the state space?
- **Topological sort:** Is any valid order acceptable? If there is a cycle, what do I return?
- **Union-Find:** Are edges only added or also removed? (Deletions mean offline reverse processing.) Need component sizes?
- **Dijkstra:** Could the same node enter the heap twice (lazy deletion)? Overflow with `INF`?
- **Clone graph / copy:** Use a map from old to new node. Cycles?

### B17. Grid / matrix

| Question | Why | What changes |
|---|---|---|
| 4 or 8 directions? | Neighbor array | `dirs` |
| Obstacles / walls / visited marking? | State | Mark in place or `visited[][]` |
| May I mutate the grid? | Sink the island vs extra memory | O(1) space |
| Rows and columns bounds? | DFS recursion depth up to m·n | BFS or iterative DFS |
| Single source, multi-source, or any-to-any? | BFS start set | Multi-source BFS (rotting oranges) |
| Shortest distance or just reachability? | BFS vs DFS | |
| Diagonal / spiral / rotate / transpose? | Index arithmetic | In-place layer logic |
| Search in a sorted matrix? | Row/col sorted vs fully flattened sorted | Staircase vs binary search |
| Boundary conditions (edge cells, 1×n)? | Edge cases | |

### B18. Strings

| Question | Why |
|---|---|
| Substring or subsequence? | Contiguity |
| Case-sensitive? Spaces, punctuation, Unicode? | Alphabet |
| Alphabet size? | `int[26]` vs `int[128]` vs map |
| Empty string valid? | Base case |
| Immutable? Build with `StringBuilder` | Java performance (string concatenation in a loop is O(n²)) |
| Many pattern queries? | Trie / suffix structures |
| Matching a pattern in text? | KMP / Z / rolling hash vs `indexOf` |

**Pattern map**

| Need | Pattern |
|---|---|
| Longest with a constraint on characters | Window |
| Anagram / permutation check | Frequency array, fixed window |
| Palindromes | Expand around center, DP, Manacher |
| Compare, transform, count subsequences | DP |
| Pattern search | KMP, Z, Rabin-Karp |
| Prefix queries | Trie |
| Repeated substring | KMP failure function, hashing |
| Parsing / decoding | Stack |

### B19. Trie

Ask: Exact word search or prefix search? Alphabet size (array children vs map)? Insert-only or deletions too? Many queries? Memory limits? Need autocomplete (store counts or top results)? Wildcard `.`? (DFS through children.) Bitwise trie for max XOR (alphabet = {0,1}).

### B20. Range queries (Prefix / Fenwick / Segment tree / Sparse table)

| Situation | Structure |
|---|---|
| Static, sum | Prefix array |
| Static, min/max, many queries | Sparse table (O(1) query, idempotent ops) |
| Point updates, range sum | Fenwick tree |
| Point updates, range min/max/gcd | Segment tree |
| Range updates + range queries | Lazy segment tree |
| Offline, add ranges then read | Difference array |
| Count of smaller elements to the right, inversions | Fenwick with coordinate compression, or merge sort |
| Offline queries on static data | Mo's algorithm |

Ask: How many elements, queries, and updates? What is the operation (sum, min, gcd)? Are updates point or range? Are queries online? Do values need coordinate compression?

### B21. Bit manipulation

| Question | Why |
|---|---|
| Signed or unsigned? Width (32/64)? | `>>` vs `>>>`, overflow |
| Negative numbers allowed? | Two's complement behavior |
| All duplicates except one (frequency known)? | XOR trick |
| Subset enumeration with n ≤ 20? | Bitmask loops |
| Need a count of set bits? | `n & (n−1)` loop, `Integer.bitCount` |
| Missing / repeated number? | XOR or sum identities |

**Java note:** `Math.abs(Integer.MIN_VALUE)` is still negative. Use `long` for 32-bit extremes.

### B22. Math / number theory

Ask: Range (does it fit in `long`)? Modulo and which prime? Is the number prime-related (sieve for many, trial division for one up to √n)? GCD/LCM needed? Negative numbers and modulo semantics? Power with large exponent (fast power)? Can floating point cause precision issues (prefer integers)? Is there a closed-form formula instead of simulation?

### B23. Divide and conquer / merge-sort-based

Applies to: counting inversions, merge sort, closest pair, maximum subarray (alt), kth element. Ask: Can the problem split into independent halves with a combine step? What is the combine cost (gives the recurrence)? Is recursion depth fine?

### B24. Cyclic sort / index-as-hash

Applies when values lie in [1..n] or [0..n]. Ask: May I mutate the array? Are there duplicates or missing numbers (one or many)? Is O(1) extra space required? If mutation is disallowed, use Floyd's cycle detection on the value→index graph (find duplicate).

### B25. Streaming and randomized

| Need | Technique | Key question |
|---|---|---|
| Random sample of size k from a stream | Reservoir sampling | Stream length unknown? |
| Majority element | Boyer-Moore voting | Is the majority guaranteed? (If not, verify with a second pass.) |
| Running median | Two heaps | Static or streaming? |
| Frequency estimation with tiny memory | Count-min sketch | Is approximate acceptable? |
| Random pick with weights | Prefix sums + binary search | Static weights? |
| Random element with O(1) insert/delete | HashMap + ArrayList (swap-with-last) | Duplicates allowed? |

### B26. Design problems (LRU, iterator, stack with min, etc.)

| Question | Why |
|---|---|
| Required time per operation? | Decides the structure |
| Capacity and eviction policy? | LRU vs LFU |
| What does a miss return? | −1, null, exception |
| Does updating an existing key count as a use? | LRU recency |
| Duplicates allowed? | Set vs multiset |
| Thread safety? | Locks, concurrent structures |
| Memory limits? | Structure choice |
| Are operations interleaved or batched? | |

**Common compositions:** LRU = HashMap + doubly linked list; RandomizedSet = HashMap + ArrayList; Min stack = stack of pairs; Time-based map = HashMap + binary search; Hit counter = queue or circular buffer; Trie-based autocomplete.

---

## Part C: Putting it together on a new problem

**Problem:** *"Return the length of the shortest subarray with sum at least K."*

1. **Decode.** Input: integer array, K. Objective: minimize length. Condition: contiguous, sum ≥ K. Output: an integer.
2. **Words → candidates.** "Subarray" + "shortest" → window; "sum" → prefix sum.
3. **Brute force.** All subarrays, O(n²) with running sums.
4. **Bottleneck.** Re-examining overlapping ranges.
5. **First idea (window).** What breaks it? Negative numbers.
6. **Ask.** "Can values be negative?" "Max n and value range?" "If no subarray qualifies, do I return −1?"
7. **Branch on the answer.**
   - Nonnegative: variable window (shrink while sum ≥ K, record length), O(n).
   - Negatives allowed: prefix sums + **monotonic deque**, O(n). Reason: you need the nearest earlier prefix with `P[j] ≤ P[i] − K`, and the deque keeps candidates in increasing prefix order.
8. **Justify and verify.** Use `long` for prefixes. Test the empty array, a single element ≥ K, all negative, and K ≤ 0.

---

## Part D: Delivery script

1. **Restate** with one example. *"So given … I return … For [example] that gives … Right?"*
2. **Group your questions (3-5):** *"Can values be negative? I'm asking because a sliding window needs predictable sums. What's the max n? What do I return if none exists?"*
3. **State defaults for the rest.** *"I'll assume non-empty input and that I may not mutate it."*
4. **Brute force, then the bottleneck.**
5. **Name the pattern and its precondition.** *"This works because …"*
6. **Complexity, then code, then dry-run.**

**Question template:** property → reason → consequence → decision. *"Is the array sorted? If so I can use two pointers in O(n) and O(1) space; if not, a hash map or sorting."*

**Budget:** 3-5 minutes of clarification. More than 6 questions looks like stalling.

---

## Cheat sheet: the always-ask questions

1. How large can n be?
2. Value range, negatives, zeros (overflow)?
3. Sorted? Duplicates? Empty?
4. Exactly what do I return (value, index, all, count), and what if no answer exists?
5. Can I modify the input? Space limit?
6. One query or many? Updates?

Then add pattern questions from Part B. If you want to keep this as a reference for your DSA notes, I can turn it into a document. I can also run a mock drill where you receive a problem, ask your questions, and I answer as the interviewer, then grade your questions and the pattern you chose.


