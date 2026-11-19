# Clarifying Questions in DSA Interviews

Yes, there is a framework. It follows the same rule as in HLD, but the questions are different.

> **Rule:** Ask only if the answer could change your algorithm. Otherwise, state an assumption and move on.

In HLD you ask about **scale, consistency, and failures**. In DSA you ask about **the input, the output, the constraints, and edge cases**. The reason is simple: in DSA, the **constraints almost choose the algorithm for you**.

---

## 1. Why DSA clarification matters

You ask questions for three reasons:

1. **To solve the right problem.** Many problems have hidden twists (duplicates, negatives, sorted or not).
2. **To pick the right technique.** The same problem needs different algorithms depending on a single fact.
3. **To show maturity.** Interviewers watch whether you think about edge cases before coding.

Example: "Find a subarray with sum K."
- Only positive numbers? Use **sliding window**, O(n).
- Negatives allowed? Sliding window breaks. Use **prefix sum + hashmap**, O(n).

One question changed the whole solution. That is what a good clarifying question does.

---

## 2. The framework: R.I.O.C.E.

Go through five steps in order. The whole phase should take **2-4 minutes**.

| Letter | Step | Goal |
|---|---|---|
| **R** | **Restate** | Confirm you understood the problem in your own words |
| **I** | **Input** | Type, size, range, order, duplicates, special values |
| **O** | **Output** | What exactly to return, how many answers, what if none |
| **C** | **Constraints and context** | Time/space expectations, can I modify the input, one query or many |
| **E** | **Examples and edge cases** | Walk through a sample and test the tricky cases |

Memory trick: **R**estate, **I**nput, **O**utput, **C**onstraints, **E**xamples. Like rice, simple and always on the plate.

---

## 3. Each step in detail

### R: Restate

Say the problem back in one or two sentences.

> "So given an array of integers and a target, I need to return the indices of two numbers that add up to the target. Is that right?"

This catches misunderstandings early. It also gives you thinking time.

### I: Input questions

| Question | Why it matters (what it changes) |
|---|---|
| **What is the size of the input (n)?** | Decides the time complexity you need (see the table in section 4) |
| **What is the value range?** | Decides overflow risk, whether counting arrays or bucket sort work |
| **Can there be negative numbers?** | Breaks sliding window, Dijkstra, some greedy ideas. Allows Kadane or prefix sum + hashmap |
| **Can there be duplicates?** | Changes hashing, backtracking (skip duplicates), sets vs lists |
| **Is the input sorted?** | Enables binary search, two pointers, and merge-style approaches |
| **Can the input be empty or null?** | Edge case handling, base cases |
| **What is the data type?** (int, long, double, char) | Overflow, precision, comparison issues |
| **For strings: what character set? Case-sensitive? Spaces?** | Fixed array of 26 vs 128 vs hashmap |
| **Is the input a stream?** (can't see all of it) | Online algorithms, heaps, reservoir sampling |

### O: Output questions

| Question | Why it matters |
|---|---|
| **Return the value, the index, or the actual elements?** | Changes what you store (DP only needs a value, reconstruction needs parents) |
| **One answer or all answers?** | One = greedy, DP, or early exit. All = backtracking, which is exponential |
| **If multiple answers exist, which one?** (any, smallest, lexicographically smallest, first found) | Changes tie-breaking and sorting |
| **What if no answer exists?** (return -1, empty list, null, exception) | Return value and edge case |
| **Is the order of output important?** | Sorting cost, data structure choice |
| **Count the answers or list them?** | Counting can often use DP in polynomial time, listing cannot |

### C: Constraints and context

| Question | Why it matters |
|---|---|
| **Is there a time or space target?** (e.g., O(n) time, O(1) space) | Tells you whether the brute force is enough or you must optimize |
| **Can I modify the input?** | Allows in-place tricks, sorting, marking visited by changing values |
| **Is this a one-time call or many queries on the same data?** | Many queries justify preprocessing (prefix sums, sparse table, segment tree, sorting once) |
| **Does the data change between queries?** (static vs updates) | Static: prefix sum. With updates: Fenwick tree / segment tree |
| **Is memory limited?** | Choose in-place or streaming approaches over extra arrays |
| **Is it single-threaded?** (for design questions) | Thread safety is a big extension |

### E: Examples and edge cases

- Ask for or create **one small example** and trace it with the interviewer.
- Then name 2-3 edge cases yourself and confirm them:
  - Empty input, one element, all same elements
  - Very large values (overflow), negatives, zeros
  - Already sorted or reverse sorted
  - No valid answer, or every element valid

> "I'll assume an empty array returns an empty result, and if no valid pair exists I return an empty array. Does that work?"

---

## 4. Constraints reveal the algorithm

This is the most useful DSA trick. **Look at n and guess the allowed complexity.** (Assume roughly 10^8 simple operations per second.)

| n (input size) | Likely allowed complexity | Likely techniques |
|---|---|---|
| n ≤ 10-12 | O(n!) | Brute-force permutations, backtracking |
| n ≤ 20-25 | O(2^n) | Subsets, bitmask, backtracking, meet in the middle |
| n ≤ 100 | O(n^3) | Floyd-Warshall, 3D DP, triple loops |
| n ≤ 1,000-5,000 | O(n^2) | 2D DP, nested loops, LCS/edit distance |
| n ≤ 10^5 to 10^6 | O(n log n) or O(n) | Sorting, heap, binary search, two pointers, sliding window, hashing, segment tree |
| n ≤ 10^9 or more | O(log n) or O(1) | Binary search on the answer, math, formula, fast power |

```
        n is tiny      n is small        n is medium         n is huge
       (<= 20)         (<= 5000)         (<= 10^6)           (>= 10^9)
          |               |                  |                   |
     exponential       O(n^2)            O(n log n)           O(log n)
     backtracking      DP tables         sort / heap /        math or
     bitmask           nested loops      window / hash        binary search
```

**How to use it:** If the interviewer says "n up to 10^5" and your idea is O(n^2), you know to look for something better. Ask for the constraints early. If they are not given, ask: "How large can the input be?"

---

## 5. Pattern-specific killer questions

After R.I.O.C.E., add the questions that matter for the **data structure or pattern**.

| Problem type | Killer questions |
|---|---|
| **Array / subarray** | Contiguous or not? Negatives? Sorted? Sum, product, or count? Can I use extra space? |
| **Two pointers / sliding window** | Contiguous? All positive? Fixed or variable window? |
| **String** | Case-sensitive? Only letters or any character? Unicode? Empty string valid? Substring (contiguous) or subsequence? |
| **Hashing / frequency** | Duplicates allowed? Is the value range small (use array instead of map)? |
| **Binary search** | Sorted? Duplicates (first, last, any occurrence)? Is the answer monotonic (can I binary search on the answer)? |
| **Linked list** | Singly or doubly linked? Possible cycle? Can I modify pointers? Return new head or modify in place? |
| **Tree** | Binary tree or BST? Balanced? Parent pointers? Duplicate values? Very deep (recursion stack risk)? |
| **Graph** | Directed or undirected? Weighted? Negative weights? Cycles? Connected or multiple components? Self-loops, multi-edges? Dense or sparse? Representation (list, matrix, edges)? |
| **Intervals** | Sorted? Open or closed ends? Do touching intervals (end = start) overlap? Can they be nested? |
| **Heap / Top K** | Is K small compared to n? Streaming data? Ties? Need sorted result? |
| **Backtracking / combinations** | Duplicates in input? Can elements be reused? Order matter (permutation vs combination)? Return all or count? |
| **DP** | Is each item usable once or many times? Need the value or the actual path? Constraints small enough for O(n^2) or O(n*W)? |
| **Matrix / grid** | 4 or 8 directions? Obstacles? Can I mutate the grid? Size limits? |
| **Greedy** | Is there a proof of optimality? Ties? Can I sort first? |
| **Design (LRU, iterator, etc.)** | Required complexity per operation? Capacity? Thread safety? Edge behavior (miss, eviction, empty)? |
| **Union Find / connectivity** | Edges added over time (dynamic)? Need components count? Removal of edges? |
| **Sweep line / ranges** | Event ordering at ties? Inclusive or exclusive endpoints? |

---

## 6. How to think of the questions (mental method)

Use these five lenses on any problem:

1. **What could be special about the data?** (sorted, negative, duplicate, empty, huge)
2. **What does "answer" mean exactly?** (value, index, all, any, smallest)
3. **How big can it get?** (the constraints decide complexity)
4. **How will it be used?** (one call or many, static or changing)
5. **Where could my first idea break?** (that tells you which edge case to confirm)

A powerful habit: **imagine your first solution, then ask what input would break it.** Turn that into a question.

- Idea: sliding window for a subarray sum. Break: negatives. Question: "Can numbers be negative?"
- Idea: use recursion on a tree. Break: depth of 10^5. Question: "How deep can the tree be?"
- Idea: sum into an int. Break: overflow. Question: "What is the range of values?"

```
Think of an approach
        |
        v
"What input breaks this?"
        |
        v
Turn that into a clarifying question
        |
        v
Confirm or state assumption
```

---

## 7. How to ask (delivery)

1. **Propose a default and confirm.**
   Weak: "Are there negative numbers?" (fine, but passive).
   Stronger: "I'll assume the numbers can be negative, so I won't rely on a sliding window. Is that OK?"
2. **Group your questions.** Ask 3-5 together, not one at a time.
3. **Say why you ask.** "I'm asking about duplicates because it changes whether I need to skip repeated values."
4. **Think aloud.** Interviewers grade your reasoning, not only the final code.
5. **Time-box to 2-4 minutes.** If the interviewer says "you decide", state your assumptions out loud and continue.
6. **Do not ask for the algorithm.** "Should I use a heap?" asks for the answer. Decide yourself.
7. **Do not ask things already in the statement.** Re-read the problem first.
8. **Write assumptions in a comment or on the board.** Revisit them when you code.

---

## 8. Where clarification fits in the whole interview

Clarifying is step 1 of the larger flow. A common structure is **U.M.P.I.R.E.**:

| Step | What you do |
|---|---|
| **U**nderstand | R.I.O.C.E. (the framework above) |
| **M**atch | Identify the pattern (two pointers, DP, BFS, heap, etc.) |
| **P**lan | Explain brute force first, then the optimized idea, with complexity |
| **I**mplement | Write clean code and narrate |
| **R**eview | Dry-run on the sample and edge cases |
| **E**valuate | State time and space complexity, discuss alternatives and trade-offs |

---

## 9. Worked examples

### Example 1: "Find a subarray with sum equal to K"

**Questions:**
- Restate: "Return whether a contiguous subarray sums to K, or the indices?"
- Input: "Can numbers be negative? How large is n? Any zeros?"
- Output: "One subarray or all? What if none exists?"
- Constraints: "Is there an O(n) expectation?"

**Why:**
- Positive only: sliding window, O(n), O(1) space.
- Negatives: prefix sum + hashmap, O(n), O(n) space.

```java
// Negatives allowed: prefix sum + hashmap
public static boolean hasSubarraySum(int[] nums, int k) {
    Map<Long, Integer> seen = new HashMap<>();
    seen.put(0L, -1);                 // empty prefix
    long sum = 0;                     // long: avoids int overflow
    for (int i = 0; i < nums.length; i++) {
        sum += nums[i];
        if (seen.containsKey(sum - k)) return true;
        seen.putIfAbsent(sum, i);
    }
    return false;
}
```

Notice how the answers shaped the code: the `long` type came from the range question, the hashmap from the negatives question.

### Example 2: "Merge intervals"

**Questions:**
- "Are the intervals sorted?" (If not, I sort first: O(n log n).)
- "Are endpoints inclusive? Do `[1,3]` and `[3,5]` count as overlapping?" (Changes the comparison `<` vs `<=`.)
- "Can the input be empty? Can an interval have start > end?"
- "Should I return a new list or modify in place?"

The inclusive/exclusive question alone can flip your answer, so it is a deciding question.

### Example 3: "Shortest path in a graph"

**Questions:**
- "Weighted or unweighted?" (Unweighted: BFS. Weighted: Dijkstra.)
- "Can weights be negative?" (Negative: Bellman-Ford.)
- "Directed or undirected? Can there be cycles?"
- "How many nodes and edges?" (Dense or sparse decides adjacency matrix vs list.)
- "Single source or all pairs?" (All pairs with small n: Floyd-Warshall.)

```
Unweighted?            -> BFS
Weights >= 0?          -> Dijkstra
Negative weights?      -> Bellman-Ford
All pairs, small n?    -> Floyd-Warshall
```

### Example 4: "Design an LRU cache"

**Questions:**
- "What time complexity do get and put need?" (Expect O(1), which means hashmap + doubly linked list.)
- "What is the capacity? What happens at capacity zero?"
- "Does `get` on a missing key return -1 or throw?"
- "Does updating an existing key count as a use?"
- "Does it need to be thread-safe?"

---

## 10. Common mistakes

| Mistake | Better |
|---|---|
| Starting to code immediately | Spend 2-4 minutes on R.I.O.C.E. first |
| Asking 10+ questions | Pick the 4-6 that change the algorithm |
| Asking questions the problem already answers | Re-read the statement |
| Asking "which algorithm should I use?" | Reason it out and propose it |
| Never asking about constraints | Always get n, value range, and negatives |
| Ignoring edge cases until the end | State them upfront, test them in the dry run |
| Staying silent while thinking | Narrate your thought process |
| Assuming without saying it | Say your assumption out loud and get a nod |
| Forgetting overflow and empty input | Ask about value range and empty input every time |

---

## 11. One-page cheat sheet

**Rule:** Ask only if the answer changes the algorithm. Otherwise, assume and state it.

**Framework: R.I.O.C.E.**
- **R**estate the problem.
- **I**nput: size, range, sorted, duplicates, negatives, empty, type.
- **O**utput: what to return, one or all, tie-breaking, no-answer case.
- **C**onstraints: time/space target, modify input?, one query or many, static or updates.
- **E**xamples: trace one, name edge cases.

**The always-ask five:**
1. How large can n be?
2. What is the range of values (negatives? overflow)?
3. Is the input sorted? Duplicates? Empty?
4. What exactly should I return, and what if there is no answer?
5. Any time or space requirement? Can I modify the input?

**Mental trick:** think of your first approach, ask "what input breaks it?", and turn that into a question.

**Constraint to complexity:** n ≤ 20 means exponential. n ≤ 5000 means O(n^2). n ≤ 10^6 means O(n log n) or O(n). n ≥ 10^9 means O(log n) or math.

**Delivery:** restate, propose defaults, group questions, explain why you ask, time-box to 2-4 minutes, think aloud.

**HLD vs DSA in one line:** HLD questions are about scale, consistency, and failure. DSA questions are about input, output, constraints, and edge cases.

---

I can add this as a short guide next to your DSA notes, or I can play the interviewer and give you problems one by one so you practice asking questions. If you want the drill, tell me which topics (arrays, graphs, DP, and so on) and I'll start.
