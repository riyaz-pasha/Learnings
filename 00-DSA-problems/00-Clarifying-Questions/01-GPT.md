Absolutely. DSA interviews have a similar framework to HLD interviews, but the objective is different.

In HLD, clarifying questions help you choose the **architecture**.

In DSA, clarifying questions help you identify the **problem's exact mathematical and computational requirements**, eliminate incorrect interpretations, and discover the right algorithmic approach.

The biggest mistake candidates make is immediately jumping into coding after reading the problem.

A strong candidate first understands:

1. What exactly is the input?
2. What output is expected?
3. What constraints must the solution satisfy?
4. What edge cases could break a naive solution?
5. What complexity is acceptable?
6. What properties of the problem can we exploit?

I'll give you a reusable framework, explain how to think of the questions, and then apply it to different DSA patterns.

# **1. The framework: C-L-A-R-I-T-Y**

Use **CLARITY** as your DSA problem-understanding framework.

## **C**

**Constraints**

What are the input limits? What complexity can work?

## **L**

**Limits and boundaries**

Can inputs be empty, negative, duplicated, or extremely large?

## **A**

**Assumptions**

What is guaranteed versus what am I assuming?

## **R**

**Required output**

What exactly must be returned? Is any valid answer acceptable?

## **I**

**Input properties**

Is the input sorted, unique, bounded, mutable, or guaranteed to have a solution?

## **T**

**Transformation and operations**

What operations are allowed? Can I modify the input?

## **Y**

**Yield and complexity**

What are the expected time and space requirements?

This is a thinking checklist, not a requirement to ask seven questions aloud. Many answers are already provided in the problem statement.

The important distinction is:

- **Clarify:** Ask the interviewer when something is genuinely ambiguous.
- **Confirm:** Restate an important interpretation.
- **Assume:** Make a reasonable assumption and explicitly mention it.
- **Infer:** Derive useful properties from the constraints.

Let's understand each one.

# **2. C — Constraints: understand the size of the problem**

This is arguably the most important part of DSA problem analysis.

The constraints often tell you which algorithms are possible.

Suppose the problem says:

```
Given an integer array nums, find two numbers
whose sum equals target.

1 <= nums.length <= 100000
-10^9 <= nums[i] <= 10^9
```

Before thinking about code, inspect the constraints.

Ask yourself:

- How large can the input become?
- Are negative values possible?
- Can values repeat?
- Can the answer overflow an `int`?
- What time complexity is likely acceptable?

### **Why does input size matter?**

Consider these rough complexity estimates:

| Input size             | Usually practical approaches                   |
| ---------------------- | ---------------------------------------------- |
| \\(n \leq 10\\)        | Exponential, backtracking, bitmask enumeration |
| \\(n \leq 20\\)        | \\(O(2^n)\\), sometimes \\(O(n2^n)\\)          |
| \\(n \leq 100\\)       | \\(O(n^3)\\) may work depending on constants   |
| \\(n \leq 1,000\\)     | \\(O(n^2)\\) may work                          |
| \\(n \leq 100,000\\)   | Usually \\(O(n\log n)\\) or \\(O(n)\\)         |
| \\(n \geq 1,000,000\\) | Usually \\(O(n)\\), or near-linear             |

These are interview heuristics, not hard limits. Runtime limits, language, constants, memory, and number of test cases all matter.

**How to think:** If you see \\(n=100,000\\), immediately question whether an \\(O(n^2)\\) solution is feasible.

This doesn't mean you must find \\(O(n)\\) immediately. It means you know what direction to investigate.

# **3. L — Limits and boundaries: discover edge cases**

Constraints tell you how big the input can be. Boundaries tell you what unusual inputs are allowed.

For almost every DSA problem, think about these categories:

| Category         | Questions to consider                                   |
| ---------------- | ------------------------------------------------------- |
| Empty input      | Can the array, string, list, or tree be empty?          |
| Single element   | What happens when there is only one element?            |
| Duplicates       | Can values or characters repeat?                        |
| Negative values  | Can numbers be negative?                                |
| Zero             | Can zero appear? Does it have special meaning?          |
| Sortedness       | Is the input sorted, partially sorted, or unsorted?     |
| Bounds           | Can values be very large or very small?                 |
| Overflow         | Can intermediate calculations exceed the integer range? |
| Missing answer   | Is there always a valid answer?                         |
| Multiple answers | Can several valid answers exist?                        |

### **Example: Maximum subarray sum**

Suppose the problem is:

> Find the maximum sum of a contiguous subarray.

A candidate might immediately write Kadane's algorithm.

But first, consider:

**Question 1: Can all numbers be negative?**

If yes, the answer might be the largest individual element.

For example:

```
nums = [-8, -3, -5, -9]

Answer = -3
```

An incorrect implementation that initializes the maximum sum to zero would return zero.

**Question 2: Must the subarray contain at least one element?**

If yes, an empty subarray with sum zero is not a valid answer.

**Question 3: Can the answer exceed the 32-bit integer range?**

If yes, use `long` in Java.

Notice how a few boundary questions prevent incorrect implementations.

### **A useful habit**

For every problem, mentally construct these three inputs:

1. The smallest valid input.
2. An input containing the most difficult edge case.
3. An input that breaks your naive approach.

You don't necessarily need to ask the interviewer about all three. Use them to test your understanding.

# **4. A — Assumptions: distinguish guarantees from guesses**

This is where many candidates silently make incorrect assumptions.

Consider:

> Given an array, find an element that appears more than half the time.

There are two possible interpretations:

- An element guaranteed to exist.
- An element that may not exist.

That changes whether you need a verification step after applying Boyer-Moore voting.

Other important assumptions:

| Problem statement                 | Potential ambiguity                                         |
| --------------------------------- | ----------------------------------------------------------- |
| Find a pair with target sum       | Is there exactly one pair, at least one, or possibly none?  |
| Find a peak element               | Can multiple peaks exist? What happens at array boundaries? |
| Find shortest path                | Are edge weights nonnegative? Are all nodes reachable?      |
| Find a cycle                      | Is the graph directed or undirected?                        |
| Find kth largest                  | Are duplicate values counted separately?                    |
| Merge intervals                   | Do touching intervals overlap?                              |
| Generate permutations             | Are duplicate input values possible?                        |
| Find intersection of linked lists | Is intersection based on node identity or equal values?     |

### **How to ask**

Instead of asking a vague question like:

"Are there any edge cases?"

Ask something specific:

"Can the input contain duplicate values, and should duplicate occurrences be treated as separate elements?"

This tells the interviewer exactly what you need to know.

**Rule:** Never turn an assumption into a guarantee without checking the statement or clarifying it.

# **5. R — Required output: understand exactly what to return**

Two problems can have almost identical inputs but require different algorithms because their outputs differ.

Consider these variants:

| Problem asks for                   | Possible approach                           |
| ---------------------------------- | ------------------------------------------- |
| Return whether a pair exists       | HashSet / two pointers                      |
| Return the indices of a pair       | HashMap                                     |
| Return all unique pairs            | Sorting + two pointers                      |
| Count all valid pairs              | Frequency map / sorting                     |
| Return the maximum number of pairs | Greedy / matching, depending on constraints |

The underlying condition may be the same, but the output requirement changes the implementation.

Ask:

- Do I need to return a value, index, count, or actual elements?
- Do I need one answer or all answers?
- Is any valid answer acceptable?
- Is a particular ordering required?
- Should duplicate answers be removed?
- What should I return when no solution exists?

### **Example: Subsets**

Given:

```
nums = [1, 2, 3]
```

Possible requirements:

1. Return all subsets.
2. Return the number of subsets.
3. Return subsets whose sum equals a target.
4. Return the subset with maximum sum under a constraint.

These are different algorithmic tasks.

For the first, backtracking is natural. For the second, the answer is simply \\(2^n\\). For the third, backtracking or dynamic programming may be appropriate depending on constraints.

**Think about the output before selecting the algorithm.**

# **6. I — Input properties: look for exploitable structure**

This is where algorithmic intuition begins.

The most important question is:

> What property of the input can help me avoid checking every possibility?

Look for these properties:

| Input property            | What it may enable                              |
| ------------------------- | ----------------------------------------------- |
| Sorted array              | Binary search, two pointers                     |
| Nearly sorted array       | Modified binary search, insertion-based methods |
| Positive numbers only     | Certain sliding-window approaches               |
| Nonnegative values        | Monotonicity in prefix sums or window sums      |
| Small bounded value range | Counting array, bucket sort                     |
| Many repeated values      | Frequency maps, compression                     |
| Tree structure            | DFS, BFS, recursion, tree DP                    |
| Directed acyclic graph    | Topological sorting, DAG DP                     |
| Nonnegative edge weights  | Dijkstra                                        |
| Negative edge weights     | Bellman-Ford or other suitable algorithms       |
| Connected components      | DFS/BFS, Union-Find                             |
| Overlapping intervals     | Sorting and greedy processing                   |
| Monotonic condition       | Binary search on answer                         |
| Repeated subproblems      | Memoization / dynamic programming               |

### **Example: Two Sum**

Problem:

```
nums = [2, 7, 11, 15]
target = 9
```

Without additional properties, the brute-force approach is:

- Choose one element.
- Check every other element.
- Repeat.

Time complexity: \\(O(n^2)\\).

Now suppose the interviewer says the array is sorted.

That new property allows:

- Start with the smallest and largest values.
- If their sum is too small, move the left pointer right.
- If their sum is too large, move the right pointer left.

Time complexity: \\(O(n)\\).

If the array is unsorted, a HashMap can provide an expected \\(O(n)\\) solution instead.

The important insight is that **input properties often reveal the algorithmic pattern**.

# **7. T — Transformation and operations: understand what you are allowed to do**

Sometimes the problem's constraints are not about the input itself, but about what operations are allowed.

Ask:

- Can I modify the original input?
- Can I sort the array?
- Can I use extra memory?
- Must I solve it in-place?
- Is recursion allowed?
- Is the input stream-based, meaning I cannot revisit previous elements?
- Are there restrictions on data structures or operations?

### **Example: Find duplicates**

Suppose you have an array containing integers from 1 to n.

There are several possible approaches:

| Approach     | Time                | Extra space     | Modifies input? |
| ------------ | ------------------- | --------------- | --------------- |
| HashSet      | \\(O(n)\\) expected | \\(O(n)\\)      | No              |
| Sorting      | \\(O(n\log n)\\)    | Depends on sort | Yes             |
| Sign marking | \\(O(n)\\)          | \\(O(1)\\)      | Yes             |
| Cyclic sort  | \\(O(n)\\)          | \\(O(1)\\)      | Yes             |

If the interviewer requires constant extra space and prohibits modifying the input, several of these approaches are eliminated.

**Why this matters:** You may know a perfectly valid algorithm, but it may violate the problem's constraints.

# **8. Y — Yield: determine the complexity requirements**

Here, you bring together the constraints and output requirements.

Ask yourself:

- What is the maximum possible input size?
- What is the expected time complexity?
- Is additional memory allowed?
- Are there many queries on the same input?
- Is preprocessing acceptable?
- Is the solution expected to be online or can it process everything first?

### **Example: Range sum queries**

Suppose you need to answer:

```
sum(2, 5)
sum(1, 7)
sum(4, 9)
...
```

There are two important pieces of information:

- How many array elements exist?
- How many queries will be asked?

If there is only one query, a simple loop is sufficient.

If there are 100,000 queries on an immutable array, prefix sums are a much better choice.

If updates are also frequent, a Fenwick Tree or Segment Tree may be appropriate.

The key is that **the number of operations matters just as much as the size of the input**.

# **9. Pattern-specific clarifying questions**

The universal CLARITY framework helps you understand any problem. But once you recognize a possible pattern, there are additional questions that help you choose the correct variant of that pattern.

This is particularly important because knowing a pattern is not enough. You must know when it applies and which assumptions it needs.

## **A. Arrays, strings, and two pointers**

| Pattern                 | Questions to ask                                                                                                        |
| ----------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| Two pointers            | Is the array sorted? Are duplicates allowed? Do I need indices or values?                                               |
| Sliding window          | Is the window contiguous? Are values positive/nonnegative? Is the condition monotonic as the window expands or shrinks? |
| Prefix sum              | Are there multiple range queries? Are updates required? Can negative values occur?                                      |
| Binary search           | Is the array sorted? Is there a monotonic condition? Do I need first occurrence, last occurrence, or any occurrence?    |
| Binary search on answer | Can I define a feasibility check? Does feasibility change monotonically as the answer increases?                        |
| Kadane's algorithm      | Must the subarray be nonempty? Can all elements be negative?                                                            |
| Sorting / intervals     | Do touching intervals overlap? Do duplicate intervals matter? Must original indices be preserved?                       |
| Monotonic stack         | Do I need the next/previous greater or smaller element? Should equal elements be included or excluded?                  |

### **Particularly important: sliding window**

Many candidates incorrectly apply sliding window whenever they see a contiguous subarray problem.

Consider:

> Find the longest subarray whose sum is at most K.

If all elements are positive, expanding the window increases its sum and shrinking it decreases its sum. This gives the monotonic behavior needed by a standard sliding window.

But if negative values are allowed, that behavior disappears.

For example:

```
nums = [5, -10, 8]
K = 5
```

Adding `-10` reduces the window sum. Removing elements no longer has a predictable effect.

You may need prefix sums and a different algorithm.

**Question to develop the right intuition:** Does expanding or shrinking the window change the condition in a predictable direction?

## **B. Recursion and backtracking**

This is particularly important for problems involving combinations, permutations, subsets, and constraint-based search.

Ask:

1. Are we generating all possible solutions or just finding one?
2. Can the same element be selected multiple times?
3. Does order matter?
4. Are duplicate input values possible?
5. Can we reuse an element?
6. Are there constraints that allow early pruning?
7. What is the stopping condition?
8. Should we return immediately after finding one solution?

These questions help determine the recursive structure.

| Problem variation                                  | Design implication                                          |
| -------------------------------------------------- | ----------------------------------------------------------- |
| Order matters                                      | Permutations-style exploration                              |
| Order does not matter                              | Combinations-style exploration                              |
| Each element can be used once                      | Advance the starting index                                  |
| Elements can be reused                             | Keep the current index                                      |
| Duplicate values must not create duplicate answers | Sort and skip duplicate choices at the same recursion depth |
| Find any one solution                              | Stop once a valid solution is found                         |
| Find all solutions                                 | Continue exploring other branches                           |
| Large search space with constraints                | Use pruning and constraint propagation                      |

### **Example: Combination Sum**

Suppose:

```
candidates = [2, 3, 6, 7]
target = 7
```

Before coding, clarify:

- Can I use the same candidate more than once?
- Should `[2, 2, 3]` and `[3, 2, 2]` be considered the same combination?
- Can candidate values be duplicated?
- Are all candidate values positive?

These determine:

- Whether recursion uses `i` or `i + 1`.
- Whether you need to sort.
- Whether you need duplicate skipping.
- Whether `target < 0` is a valid pruning condition.

For example, `target < 0` pruning is safe when all candidate values are positive. It is not generally safe if negative candidates are allowed.

This is why clarifying input properties is so important.

## **C. Dynamic programming**

DP questions are slightly different. You often need to understand the problem structure rather than ask the interviewer many questions.

Ask:

1. Are we optimizing, counting, or checking whether something is possible?
2. Are there repeated subproblems?
3. What decisions can be made at each position or state?
4. Does the current decision depend on previous states?
5. Can the input be processed in one direction?
6. Is there a dependency between states that determines iteration order?
7. Can memory be optimized because only a few previous states are needed?

| Problem type                   | What to investigate                                                    |
| ------------------------------ | ---------------------------------------------------------------------- |
| 0/1 Knapsack                   | Can each item be used only once?                                       |
| Unbounded Knapsack             | Can an item be reused?                                                 |
| Subset Sum                     | Are negative values possible? Is the target bounded?                   |
| Longest Increasing Subsequence | Strictly increasing or non-decreasing?                                 |
| String DP                      | Are insertions, deletions, substitutions allowed?                      |
| Grid DP                        | Are movements restricted to certain directions? Are obstacles present? |
| Interval DP                    | Does the solution depend on smaller subintervals?                      |
| Tree DP                        | Can a node's answer be computed from its children's answers?           |

### **Example: 0/1 Knapsack versus Unbounded Knapsack**

The distinction is simple but fundamental:

- Can an item be selected only once?
- Or can it be selected repeatedly?

For 0/1 Knapsack, a typical one-dimensional DP iterates capacity backward.

For Unbounded Knapsack, it typically iterates capacity forward.

A single clarification changes the DP transition and iteration order.

## **D. Binary trees and BSTs**

Ask:

- Is the input a general binary tree or a Binary Search Tree?
- Can nodes contain duplicate values?
- Is the tree balanced?
- Can it be empty?
- Do we need node values or node references?
- Are we looking for a path, subtree, ancestor, level, or relationship?
- Is the answer required for one query or multiple queries?

| Problem clue              | Possible direction                                |
| ------------------------- | ------------------------------------------------- |
| Level-wise processing     | BFS                                               |
| Root-to-leaf paths        | DFS / backtracking                                |
| Lowest common ancestor    | Tree traversal, with BST properties if applicable |
| Kth smallest in BST       | Inorder traversal                                 |
| Height or diameter        | Postorder DFS                                     |
| Repeated ancestor queries | Binary lifting / LCA preprocessing                |
| Tree path optimization    | Tree DP                                           |

One subtle distinction: If the question asks whether two nodes are equal in value or whether they are the exact same node, clarify that. Node identity and node value are not interchangeable.

## **E. Graphs**

Graph problems have some of the most important clarifying questions.

Ask:

1. Is the graph directed or undirected?
2. Is it weighted or unweighted?
3. Can edge weights be negative?
4. Can cycles exist?
5. Is the graph connected?
6. Can multiple edges exist between two nodes?
7. Can self-loops exist?
8. Do we need shortest path, connectivity, cycle detection, or ordering?
9. Is the graph static or can edges change?
10. Are we dealing with a small graph or a very large sparse graph?

| Graph property / requirement       | Algorithm direction                       |
| ---------------------------------- | ----------------------------------------- |
| Unweighted shortest path           | BFS                                       |
| Nonnegative weighted shortest path | Dijkstra                                  |
| Negative edge weights              | Bellman-Ford                              |
| DAG shortest/longest path          | Topological ordering + DP                 |
| Cycle detection                    | DFS / Union-Find, depending on graph type |
| Connected components               | DFS / BFS / Union-Find                    |
| Minimum spanning tree              | Kruskal / Prim                            |
| Strongly connected components      | Kosaraju / Tarjan                         |
| Dependency ordering                | Topological sort                          |

### **Example: Shortest path**

If the interviewer says:

"Find the shortest path between two nodes."

You should immediately clarify:

**Are all edges unweighted, or do they have weights?**

Because:

- Unweighted graph → BFS.
- Nonnegative weights → Dijkstra.
- Negative weights → Bellman-Ford may be needed.
- DAG → Topological sorting with DP can work even with negative weights.

Just saying "shortest path" is not enough to select an algorithm.

## **F. Heaps and priority queues**

Ask:

- Do we need the smallest or largest K elements?
- Do we need the Kth element or the top K elements?
- Is the input static or streaming?
- Are there frequent insertions and removals?
- Do duplicates count separately?
- Do we need sorted output?

Examples:

| Requirement                         | Possible approach                  |
| ----------------------------------- | ---------------------------------- |
| Kth largest in an unsorted array    | Min-heap of size K or Quickselect  |
| Top K frequent elements             | Frequency map + heap / bucket sort |
| Running median                      | Two heaps                          |
| Merge K sorted lists                | Min-heap                           |
| Continuously retrieve smallest item | Min-heap                           |

A very useful question is whether you need the entire sorted result or only a small portion of it. That can change \\(O(n\log n)\\) sorting into \\(O(n\log k)\\) heap processing.

## **G. Linked lists**

Ask:

- Is the list singly or doubly linked?
- Can it contain a cycle?
- Can the head be null?
- Do we need to preserve the original list?
- Is the operation based on node identity or value equality?
- Do we need to perform the operation in-place?

For example, in an intersection problem, two lists intersect only when they share the same node reference, not merely because two nodes have the same value.

## **H. Tries and strings**

Ask:

- Are we dealing with lowercase English letters, all ASCII, or Unicode?
- Are prefixes important?
- Are searches exact or prefix-based?
- Are there many repeated queries?
- Can words be inserted and deleted dynamically?
- Are empty strings possible?
- Is case sensitivity important?

This helps decide whether a Trie, HashMap, sorted array, or another string-processing approach is appropriate.

## **I. Bit manipulation**

Ask:

- Are numbers signed or unsigned?
- Can values be negative?
- What is the integer width?
- Are we considering all 32 bits or only a subset?
- Are duplicates guaranteed or is there a special frequency property?
- Can bit operations overflow or change sign?

This is especially important in Java, where `int` is 32-bit signed and `long` is 64-bit signed.

# **10. The actual DSA interview workflow**

Now let's combine everything into a process you can follow for almost any DSA problem.

I recommend this sequence:

```

1. UNDERSTAND
   |
   |-- What is the input?
   |-- What output is required?
   |-- What are the constraints?
   |-- What is guaranteed?
   |
2. CLARIFY
   |
   |-- Resolve important ambiguities
   |-- Confirm edge-case behavior
   |-- State reasonable assumptions
   |
3. EXPLORE
   |
   |-- Think of brute force
   |-- Identify why it is inefficient
   |-- Find useful input properties
   |
4. IDENTIFY THE PATTERN
   |
   |-- Hashing / Two Pointers / Sliding Window
   |-- Binary Search / Stack / Heap
   |-- Recursion / Backtracking / DP
   |-- Trees / Graphs / Greedy
   |
5. OPTIMIZE
   |
   |-- Can we reduce time complexity?
   |-- Can we reduce space complexity?
   |-- What trade-offs are acceptable?
   |
6. VERIFY
   |
   |-- Walk through an example
   |-- Test edge cases
   |-- Check correctness
   |
7. IMPLEMENT
   |
   |-- Write clean code
   |-- Handle boundaries
   |-- Explain important decisions
   |
8. ANALYZE
   |
   |-- Time complexity
   |-- Auxiliary space complexity
   |-- Explain why the algorithm works

```

## **11. How to communicate with the interviewer**

You don't have to ask questions mechanically. Make your reasoning visible.

Let's take a concrete example.

**Problem: Find the longest substring without repeating characters.**

### **Step 1: Understand the problem**

You can say:

"Let me confirm that we need a contiguous substring, not a subsequence, and we need its maximum length."

This establishes an important distinction.

### **Step 2: Clarify relevant details**

Ask:

1. "Can the string contain spaces, punctuation, or arbitrary characters?"
2. "Should character comparison be case-sensitive?"
3. "Can the input be empty?"

If the statement already answers these, don't ask again.

### **Step 3: Think about a brute-force solution**

You can say:

"A straightforward approach would enumerate all substrings and check whether each has unique characters. That would be expensive, so I'd like to avoid repeatedly checking overlapping substrings."

### **Step 4: Identify the pattern**

Now recognize:

- The substring must be contiguous.
- We need a longest valid window.
- The condition is that no character appears more than once.
- We can maintain the state of the current window.

This suggests **sliding window + a HashMap or character-index array**.

### **Step 5: Explain the optimization**

"Instead of restarting the search at every position, I'll maintain a window and move its left boundary whenever a repeated character violates the condition."

### **Step 6: Verify**

Use:

```
s = "abcabcbb"
```

Expected answer:

```
3
```

Then test:

```
""
"a"
"aaaa"
"abba"
```

### **Step 7: Analyze**

- Time: \\(O(n)\\) expected with a HashMap.
- Space: \\(O(\min(n, |\Sigma|))\\), where \\(\Sigma\\) is the character set.

Notice something important: you did not ask 15 questions. You asked a few meaningful ones and spent the rest of the time demonstrating your algorithmic reasoning.

# **12. Common mistakes when asking DSA questions**

| Mistake                                            | Better approach                                |
| -------------------------------------------------- | ---------------------------------------------- |
| Asking questions already answered in the statement | Read carefully before asking                   |
| Asking "Any edge cases?"                           | Ask about specific ambiguities                 |
| Asking too many questions                          | Prioritize questions that change the algorithm |
| Assuming sorted input                              | Explicitly verify sortedness                   |
| Assuming positive numbers                          | Check whether zero and negatives are possible  |
| Assuming a solution always exists                  | Verify the guarantee                           |
| Ignoring duplicate values                          | Determine whether duplicates affect the output |
| Jumping to an optimized pattern immediately        | First establish a correct baseline             |
| Asking the interviewer which algorithm to use      | Derive the algorithm from properties           |
| Coding before explaining the approach              | Communicate the idea and complexity first      |

One more mistake deserves special attention:

**Don't confuse clarifying questions with algorithmic discovery.**

For example, if you're solving a binary search problem, you don't need to ask the interviewer:

"Can I use binary search?"

Instead, inspect the input and condition. If they are sorted or monotonic, explain why binary search applies.

The interviewer wants to see that you can discover the pattern yourself.

# **13. Your reusable DSA interview checklist**

This is the compact version I would memorize as a mental process, not as a list of questions to recite.

## **DSA interview checklist**

**Understand**

- What exactly is the input and expected output?
- Are we finding, counting, optimizing, or generating?
- Do we need one answer or all answers?

**Constraints**

- What is the maximum input size?
- Are negatives, zero, duplicates, or empty inputs possible?
- Is there a guarantee that a solution exists?
- Can integer overflow occur?

**Properties**

- Is the input sorted or monotonic?
- Is it contiguous or non-contiguous?
- Is it a tree, graph, or sequence?
- Are there repeated subproblems?
- Can I exploit bounded values or special structure?

**Restrictions**

- Can I modify the input?
- Is extra space allowed?
- Are there restrictions on operations or data structures?

**Algorithm**

- What is the brute-force solution?
- Why is it inefficient?
- Which pattern can improve it?
- What assumptions does that pattern require?

**Verification**

- Can I dry-run a normal example?
- Have I tested boundary cases?
- Can I explain why the algorithm is correct?
- Can I state time and auxiliary space complexity?

# **14. My strongest recommendation: learn to recognize algorithm-changing questions**

This is the skill that will make you better at unfamiliar problems.

Consider these pairs:

| If the answer is...                    | You may move toward...                    |
| -------------------------------------- | ----------------------------------------- |
| Sorted array                           | Two pointers / Binary search              |
| Unsorted array with fast lookup needed | Hashing                                   |
| Positive values and contiguous window  | Sliding window                            |
| Negative values in range-sum problems  | Prefix sums / different window strategy   |
| All combinations required              | Backtracking                              |
| Repeated subproblems                   | Dynamic programming                       |
| Unweighted shortest path               | BFS                                       |
| Nonnegative weighted shortest path     | Dijkstra                                  |
| Need top K elements                    | Heap / Quickselect                        |
| Many range queries                     | Prefix sums / Fenwick Tree / Segment Tree |
| Need next greater element              | Monotonic stack                           |
| Small bounded value range              | Counting / Bucket techniques              |
| Monotonic feasibility condition        | Binary search on answer                   |

These are starting directions, not automatic algorithm selections. You still need to check the exact problem requirements.

## **Final mental model**

I want you to remember three layers:

**Layer 1 — Understand the problem**

Use CLARITY:

- Constraints
- Limits and boundaries
- Assumptions
- Required output
- Input properties
- Transformation restrictions
- Yield (complexity)

**Layer 2 — Discover the algorithm**

Ask:

- What is the brute-force approach?
- What makes it inefficient?
- What property can I exploit?
- Which algorithmic pattern uses that property?
- What are the preconditions of that pattern?

**Layer 3 — Prove and implement**

Ask:

- Why does this algorithm always work?
- What happens at the boundaries?
- What is the complexity?
- Does the implementation preserve the invariant?

**The most important principle:**

Don't ask questions to collect information. Ask questions to eliminate incorrect approaches and narrow down the correct one.

That is the difference between someone who memorizes LeetCode patterns and someone who can solve a problem they have never seen before.
