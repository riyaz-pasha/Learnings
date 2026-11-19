# **A practical framework for asking clarifying questions in DSA interviews**

The goal is not to ask the interviewer as many questions as possible. The goal is to **understand the problem deeply enough that you can eliminate unsuitable approaches and identify the right algorithm yourself.**

I would combine the strongest parts of your two documents, but make one important improvement: instead of treating the frameworks as checklists of questions, use them as a **step-by-step problem-solving process**.

Your first document uses R.I.O.C.E. (Restate, Input, Output, Constraints, Examples), while the second uses C.L.A.R.I.T.Y. (Constraints, Limits, Assumptions, Required output, Input properties, Transformation, Yield). Both cover useful ground. The missing piece is a clearer connection between understanding the problem, asking a question, and choosing an algorithm.&#x20;

Pasted text(1).txt

Pasted text (2).txt



I recommend following this process:

```
1. DECODE THE PROBLEM
   Understand what the statement is actually asking.

2. DEFINE THE CONTRACT
   Identify the exact input, output, and conditions for a valid answer.

3. EXTRACT CONSTRAINTS
   Understand input size, value ranges, special properties and restrictions.

4. IDENTIFY UNCERTAINTIES
   Find the missing details that could change your solution.

5. BUILD A BASIC APPROACH
   Think about how you would solve it without optimization.

6. IDENTIFY THE BOTTLENECK
   Find repeated work or exploitable properties.

7. ASK PATTERN-SPECIFIC QUESTIONS
   Confirm the properties needed by your potential approach.

8. SELECT AND JUSTIFY THE ALGORITHM
   Explain why it applies and what complexity it achieves.

9. VERIFY AND IMPLEMENT
   Test the reasoning, edge cases and invariants before coding.
```

There are three different activities happening here:

- **Clarification:** Asking the interviewer about something the problem statement does not specify.
- **Algorithm discovery:** Reasoning about which technique could solve the problem.
- **Validation:** Checking whether the selected technique is actually valid under the given constraints.

Do not confuse them. For example, you should not ask, "Can I use a sliding window?" You should reason about whether the input and problem properties make sliding window valid.

Let's develop each step.

# **1. Step 1 — Decode the problem statement**

Before asking anything, understand what the problem is actually asking.

A common mistake is to read the entire statement and immediately think:

- This looks like a sliding-window problem.
- This looks like backtracking.
- This looks like DP.
- This looks like binary search.

Instead, break the statement into **five components**.

| Component  | What to identify                                       | Why it matters                                           |
| ---------- | ------------------------------------------------------ | -------------------------------------------------------- |
| Input      | What data am I receiving?                              | Determines the data structure and available information. |
| Operation  | What must I do with the data?                          | Identifies the nature of the problem.                    |
| Objective  | What am I optimizing, finding, counting or generating? | Determines the algorithmic goal.                         |
| Conditions | What makes an answer valid?                            | Determines the rules and constraints of the algorithm.   |
| Output     | What exactly should I return?                          | Can completely change the implementation and algorithm.  |

## **1.1 Identify the important words**

Certain words in problem statements are algorithmic signals.

| Word or phrase       | What you should think about                                                      |
| -------------------- | -------------------------------------------------------------------------------- |
| Find                 | Are we looking for one element, a pair, a path or a solution?                    |
| Count                | Can we count without generating every solution?                                  |
| Maximum / Minimum    | Is this an optimization problem? Could DP or greedy help?                        |
| Longest / Shortest   | What is being measured? Can we use a window, DP, BFS or binary search?           |
| Subarray             | Contiguous elements.                                                             |
| Substring            | Contiguous characters.                                                           |
| Subsequence          | Elements retain relative order but need not be contiguous.                       |
| Subset               | Any selection of elements; order generally does not matter.                      |
| Permutation          | Order matters.                                                                   |
| Distinct / Unique    | How are duplicates supposed to be handled?                                       |
| At most K            | An upper-bound condition.                                                        |
| At least K           | A lower-bound condition.                                                         |
| Exactly K            | An exact constraint; sometimes related to `atMost(K) - atMost(K-1)`.             |
| In-place             | Usually an additional space restriction.                                         |
| Sorted               | Potentially enables binary search or two pointers.                               |
| Kth smallest/largest | Think about heaps, sorting or Quickselect.                                       |
| All possible         | Think about backtracking, output size and pruning.                               |
| Consecutive          | Could indicate a contiguous sequence or adjacent elements, depending on context. |

### **Example: Decode this problem**

> Find the length of the longest substring without repeating characters.

Break it down:

| Part       | Interpretation                             |
| ---------- | ------------------------------------------ |
| Input      | A string                                   |
| Operation  | Examine possible substrings                |
| Objective  | Find the maximum length                    |
| Conditions | No repeating characters                    |
| Output     | An integer representing the maximum length |

Now notice the important clues:

1. **Substring:** We need contiguous characters.
2. **Longest:** We need to maximize the length.
3. **Without repeating:** We have a validity condition that must hold inside the substring.
4. **Length:** We don't need to generate and return all valid substrings.

This suggests a possible sliding-window approach.

Why?

Because we can maintain a contiguous region, track which characters it contains, and move its boundaries when the condition is violated.

You have started discovering the algorithm without asking the interviewer anything.

**The lesson:** Extract the algorithmic clues before asking clarifying questions.

# **2. Step 2 — Define the exact problem contract**

Now establish what the program must guarantee.

I recommend thinking about the problem under four headings:

1. Input contract
2. Output contract
3. Constraint contract
4. Operation contract

These form your generic clarification framework.

## **2.1 Input contract — What exactly can I receive?**

These are the questions about the data you are given.

| Question                                 | Why ask it?                                                        | What might change?                                      |
| ---------------------------------------- | ------------------------------------------------------------------ | ------------------------------------------------------- |
| What is the maximum input size?          | Determines how much processing is feasible.                        | O(n²) versus O(n log n) or O(n).                        |
| Can values be negative?                  | Some algorithms rely on nonnegative values.                        | Sliding window versus prefix sums or another technique. |
| Can values be zero?                      | Zero can affect window movement, counting and boundary conditions. | Window logic and special cases.                         |
| Can duplicates exist?                    | Duplicates can create repeated answers or change comparisons.      | Hashing, frequency maps, duplicate skipping.            |
| Is the input sorted?                     | Sortedness provides exploitable ordering.                          | Binary search, two pointers, greedy approaches.         |
| Is the input guaranteed to be non-empty? | Determines the base cases and initial values.                      | Initialization and edge cases.                          |
| What is the value range?                 | Helps identify overflow and bounded-domain techniques.             | `int` versus `long`, counting arrays, buckets.          |
| Is the input a stream?                   | Can we revisit previous elements?                                  | Online algorithms, heaps, streaming techniques.         |

### **Why do these questions matter?**

Let's say the problem is:

> Find a contiguous subarray whose sum equals K.

Consider two situations.

**Situation A: All array elements are positive.**

```
nums = [2, 3, 1, 5, 4]
k = 9
```

We can use sliding window.

Why?

- Expanding the window increases the sum.
- Shrinking the window decreases the sum.
- Therefore, we can make a meaningful decision about which boundary to move.

**Situation B: Negative numbers are allowed.**

```
nums = [5, -3, 2, 7]
k = 4
```

Now adding an element can increase or decrease the sum.

Removing an element can also increase or decrease it.

The previous sliding-window reasoning is no longer valid.

A natural alternative is prefix sum + HashMap.

Therefore, a very important question is:

> Can the array contain negative numbers?

And you should understand exactly why you are asking it.

You aren't collecting information randomly. You are checking whether a particular algorithm's assumptions hold.

## **2.2 Output contract — What exactly should I return?**

This category is extremely important because candidates often understand the input correctly but misunderstand the required output.

Consider the same problem:

> Find two elements whose sum equals the target.

It can have many different requirements.

| Requirement                                      | Possible approach                               |
| ------------------------------------------------ | ----------------------------------------------- |
| Return whether a pair exists                     | HashSet or two pointers                         |
| Return the indices of one pair                   | HashMap or two pointers with index preservation |
| Return all unique pairs                          | Sorting + two pointers                          |
| Count all valid pairs                            | Frequency map or sorting                        |
| Return the lexicographically smallest valid pair | Additional ordering/tie-breaking logic          |

Ask:

- Do I need to return the value, index, count or actual elements?
- Do I need one answer or all possible answers?
- Is any valid answer acceptable?
- If multiple answers exist, is there a preference?
- Should duplicate answers be removed?
- Is output ordering important?
- What should I return if no answer exists?

### **Why does one answer versus all answers matter?**

Consider:

> Given an array, generate all subsets.

There are \\(2^n\\) possible subsets.

If the requirement is to return all subsets, backtracking is a natural solution.

However, if the requirement is only to return the number of subsets, the answer is simply:

\\[ 2^n \\]

You don't need backtracking.

The underlying input is identical. The output requirement changes the solution.

One more important point:

**Returning all answers can itself require exponential time and space.**

If there are exponentially many valid answers, the algorithm must spend time proportional to the output size just to produce them. Don't expect to optimize that away.

## **2.3 Constraint contract — What resources are available?**

Ask:

| Question                              | Why it matters                                                             |
| ------------------------------------- | -------------------------------------------------------------------------- |
| What is the maximum input size?       | Helps determine acceptable complexity.                                     |
| Is there an expected time complexity? | Clarifies performance expectations.                                        |
| Is extra memory allowed?              | Determines whether hashing, auxiliary arrays or DP tables are appropriate. |
| Is constant extra space required?     | May require an in-place technique.                                         |
| Is recursion allowed?                 | Relevant for recursive algorithms and stack depth.                         |
| Are there multiple test cases?        | Total processing time matters, not just one input.                         |

Remember that constraints are not just about `n`.

Value ranges matter too.

For example:

```
n = 100000
nums[i] = 1,000,000,000
```

Even though an individual value fits in Java's `int`, the sum of many values might not.

Use the maximum possible intermediate result to reason about overflow.

In Java, that may mean using `long` for prefix sums or accumulated values.

## **2.4 Operation contract — How is the data going to be used?**

This category is easy to overlook.

Ask:

- Can I modify the original input?
- Can I sort the input?
- Is additional memory permitted?
- Is this a single query or are there repeated queries?
- Will the input change between queries?
- Do I have to process incoming data without seeing the future elements?

### **Example: Range sum queries**

Suppose the interviewer says:

> Given an array, answer range sum queries.

There are three possibilities.

**Case 1: Only one query**

A straightforward traversal is sufficient.

**Case 2: Many queries, but the array never changes**

Precompute prefix sums.

- Preprocessing: O(n)
- Each query: O(1)

**Case 3: Many queries and frequent updates**

Prefix sums become inconvenient because updates would require maintaining subsequent prefix values.

A Fenwick Tree or Segment Tree may be more appropriate.

The important questions are:

1. How many queries will there be?
2. Can the underlying array change?
3. How frequently do updates happen?

Those three details can determine the data structure.

# **3. Step 3 — Identify what is guaranteed versus what is unknown**

This is where you should become selective.

**Do not ask everything in your checklist.**

First, extract information from the problem statement.

Create this mental table:

| Property               | Status         | Action                                 |
| ---------------------- | -------------- | -------------------------------------- |
| Array size             | Given          | Use it                                 |
| Values can be negative | Not specified  | Decide whether clarification matters   |
| Duplicates             | Given: allowed | Handle them                            |
| Input sorted           | Not specified  | Don't assume it                        |
| Need all valid pairs   | Given          | Account for output size                |
| Can modify input       | Not specified  | Clarify if your approach depends on it |

There are four ways to deal with information.

| Situation                     | What to do                                |
| ----------------------------- | ----------------------------------------- |
| Explicitly stated             | Accept it as a guarantee.                 |
| Implied by the statement      | Explain your interpretation.              |
| Unspecified but important     | Ask the interviewer.                      |
| Unspecified and not important | Make a reasonable assumption and proceed. |

### **The most important rule**

Ask a clarifying question only when its answer could change at least one of these:

1. Correctness of your solution.
2. Algorithm or data structure selection.
3. Time complexity.
4. Space complexity.
5. Output or boundary handling.

For example:

Weak question:

> Are there any edge cases?

Better question:

> Can the array contain negative numbers? I'm asking because a standard sliding-window approach for this sum condition relies on nonnegative values.

The second question shows that you understand the algorithm's assumptions.

# **4. Step 4 — Use a brute-force approach to discover the algorithm**

This is the most important addition I would make to your original frameworks.

Instead of trying to guess the optimal pattern immediately, ask yourself:

> If I had to solve this problem in the simplest possible way, how would I do it?

This gives you a baseline.

Then ask:

> What makes my current approach inefficient?

That question often reveals the correct pattern.

## **4.1 Start with the simplest solution**

Examples:

| Problem                              | Basic approach                                         |
| ------------------------------------ | ------------------------------------------------------ |
| Two Sum                              | Check every pair: O(n²).                               |
| Maximum subarray sum                 | Enumerate possible subarrays and calculate their sums. |
| Find duplicate                       | Compare elements or use nested loops.                  |
| Longest substring without repetition | Generate substrings and check uniqueness.              |
| Kth largest                          | Sort the entire array.                                 |
| All combinations                     | Explore all possible choices.                          |
| Shortest path                        | Explore paths and compare distances.                   |
| Range sum queries                    | Traverse the range for each query.                     |

You don't necessarily need to implement brute force. You need to understand it.

## **4.2 Identify the bottleneck**

Now examine why the brute-force approach is expensive.

Ask the following questions:

### **A. Am I repeating the same work?**

Example:

You repeatedly search for a complement or check whether an element exists.

Possible improvement:

- HashSet
- HashMap
- Frequency map

This often reduces repeated searching from O(n) to expected O(1) per lookup.

### **B. Am I repeatedly calculating the same range information?**

Example:

You repeatedly calculate subarray sums.

Possible improvement:

- Prefix sums
- Difference arrays, for suitable update/query problems
- Fenwick Tree or Segment Tree when updates are required

### **C. Is there an ordering or monotonic property?**

Example:

You need to find a target in a sorted array.

Possible improvement:

- Binary search
- Two pointers

### **D. Am I exploring many possible choices?**

Example:

You need to generate combinations or permutations.

Possible improvement:

- Backtracking
- Pruning
- Dynamic programming, if repeated states exist

### **E. Are there repeated subproblems?**

Example:

The same state is reached through multiple recursive paths.

Possible improvement:

- Memoization
- Dynamic programming

### **F. Can I make a locally optimal decision?**

Example:

You need to select intervals or schedule activities.

Possible improvement:

\- Greedy algorithms

But remember: a greedy choice must be justified. Being able to make a locally optimal decision does not automatically mean that the overall solution is optimal.

## **4.3 Map the observation to a possible pattern**

| Observation                                              | Pattern to investigate      |
| -------------------------------------------------------- | --------------------------- |
| Need fast lookup                                         | HashMap / HashSet           |
| Input is sorted and elements are compared from both ends | Two pointers                |
| Need to process a contiguous region                      | Sliding window              |
| Need repeated prefix/range calculations                  | Prefix sum                  |
| Need to find a boundary in sorted data                   | Binary search               |
| Answer feasibility is monotonic                          | Binary search on answer     |
| Need next greater/smaller element                        | Monotonic stack             |
| Need top K / Kth element                                 | Heap / Quickselect          |
| Need all possible selections                             | Backtracking                |
| Repeated recursive states                                | DP                          |
| Need shortest path in an unweighted graph                | BFS                         |
| Need shortest path with nonnegative weights              | Dijkstra                    |
| Need connected components                                | DFS / BFS / Union-Find      |
| Need dependency ordering                                 | Topological sorting         |
| Need efficient range queries with updates                | Fenwick Tree / Segment Tree |
| Need to merge or process overlapping ranges              | Sorting + intervals         |
| Need prefix-based string searching                       | Trie                        |

These are directions to investigate, not automatic algorithm selections.

For example, seeing the word "subarray" does not automatically mean sliding window. You must verify whether the necessary properties hold.

# **5. Step 5 — Ask pattern-specific questions**

Once you have a potential approach, ask the questions that determine whether that approach is valid.

This is where generic clarification ends and pattern-specific clarification begins.

**You don't need to ask every question in every pattern.** First recognize a possible pattern, then investigate only its important preconditions.

## **5.1 Arrays, subarrays and two pointers**

| Question                                   | Why it matters                                                      |
| ------------------------------------------ | ------------------------------------------------------------------- |
| Is the input sorted?                       | Sortedness may allow two pointers or binary search.                 |
| Is the required section contiguous?        | Determines whether we are dealing with a subarray or a subsequence. |
| Can negative numbers exist?                | Important for sum-based sliding windows.                            |
| Can zeros exist?                           | Can affect window boundaries and counting.                          |
| Are duplicates allowed?                    | Determines duplicate handling.                                      |
| Do I need indices or values?               | Determines what information must be preserved.                      |
| Do I need one valid answer or all answers? | Changes how we search and handle duplicates.                        |

### **Sliding window — Questions you should ask**

This pattern deserves special attention because it is commonly misapplied.

Ask yourself:

1. Is the required region contiguous?
2. Is the window fixed-size or variable-size?
3. What makes a window valid?
4. Does expanding the window change the condition predictably?
5. Does shrinking the window change the condition predictably?
6. Can negative values destroy this behavior?
7. Do I need the longest, shortest, maximum sum, minimum sum or count?

The central idea is not simply that we are working with a subarray.

It is that **we can maintain a window and move its boundaries without having to reconsider every possible window.**

Example:

> Find the longest subarray whose sum is at most K.

If elements are nonnegative:

- Expanding the window cannot decrease its sum.
- Shrinking the window cannot increase its sum.
- We can adjust the boundaries in a controlled way.

If negative elements are allowed, those properties break.

You must investigate a different approach, potentially based on prefix sums and additional data structures.

### **Binary search — Questions you should ask**

1. Is the array sorted?
2. Are there duplicates?
3. Do I need any occurrence, the first occurrence or the last occurrence?
4. Am I searching for an element or for an answer value?
5. If searching over an answer range, can I define a feasibility check?
6. Does feasibility change monotonically?

The most important question for binary search on answer is:

> If a particular answer is feasible, are all answers on one side of it also feasible (or infeasible)?

For example:

If a capacity of 100 is sufficient, is every capacity greater than 100 also sufficient?

If yes, we may have the monotonicity needed for binary search on the answer.

### **Prefix sums — Questions you should ask**

1. Do we have many range-sum queries?
2. Are there negative values?
3. Are updates required?
4. Do we need the sum of a fixed range or to find a range satisfying a condition?
5. Do we need to count such ranges or just determine whether one exists?

These details help you distinguish between:

- Prefix sum array
- Prefix sum + HashMap
- Prefix sum + additional ordering techniques
- Fenwick Tree
- Segment Tree

## **5.2 Strings**

| Question                                                   | Why it matters                                          |
| ---------------------------------------------------------- | ------------------------------------------------------- |
| Are we finding a substring or subsequence?                 | Contiguous versus non-contiguous.                       |
| Is comparison case-sensitive?                              | `"A"` and `"a"` may be different.                       |
| Can spaces and special characters occur?                   | Determines character handling.                          |
| Is the character set limited to lowercase English letters? | A 26-element array may be sufficient.                   |
| Can Unicode characters occur?                              | A basic ASCII-based approach may be insufficient.       |
| Are duplicate characters important?                        | Frequency maps or sets may be needed.                   |
| Are there many prefix searches?                            | Trie may be appropriate.                                |
| Do we need matching, editing or transformation?            | Could suggest string DP or pattern matching algorithms. |

Examples of pattern clues:

- Longest substring without repetition → Sliding window.
- Anagrams → Frequency counting / HashMap.
- Common subsequence → DP.
- Repeated prefix searches → Trie.
- Palindromic substrings → Expand around center or DP, depending on requirements.

## **5.3 Recursion and backtracking**

For combinations, permutations, subsets and constraint-based search, ask:

| Question                                         | Why it matters                                |
| ------------------------------------------------ | --------------------------------------------- |
| Do I need to find one solution or all solutions? | Determines whether to stop after success.     |
| Does order matter?                               | Distinguishes permutations from combinations. |
| Can an element be reused?                        | Changes the recursive index.                  |
| Can the input contain duplicates?                | May cause duplicate results.                  |
| Should duplicate results be removed?             | May require sorting and duplicate skipping.   |
| Are there positive and negative values?          | Affects pruning conditions.                   |
| Is there a target or validity constraint?        | Helps define pruning.                         |
| Is there a maximum number of choices?            | Influences complexity and depth.              |

### **Understand how the answers affect recursion**

Consider Combination Sum.

```
candidates = [2, 3, 6, 7]
target = 7
```

You should clarify:

**Question 1: Can I reuse an element?**

If yes, after selecting an element, the next recursive call can start from the same index.

If no, it must start from the next index.

```
// Reuse allowed
backtrack(i);

// Reuse not allowed
backtrack(i + 1);
```

**Question 2: Does order matter?**

For combinations, `[2, 2, 3]` and `[3, 2, 2]` represent the same combination.

For permutations, order matters.

This changes how the search space is constructed.

**Question 3: Can duplicate values occur?**

If duplicate results are not allowed, sorting and skipping duplicate choices at the same recursion depth may be necessary.

**Question 4: Are all candidates positive?**

This matters for pruning.

For example:

```
if (target < 0) return;
```

This pruning is valid when future selections cannot reduce the accumulated sum. It is not generally safe when negative candidates are allowed.

Notice the difference between memorizing backtracking code and understanding the conditions that determine its structure.

## **5.4 Dynamic programming**

For DP, your most important job is to understand the decisions and state dependencies.

Ask yourself:

1. Is the problem asking for optimization, counting or possibility?
2. What decisions can I make at each step?
3. What information from previous steps affects the current decision?
4. Can the same state be reached through different paths?
5. What defines a unique state?
6. What are the base cases?
7. Can the current state be calculated using smaller states?
8. Does the problem require reconstructing the actual path or just the optimal value?

Pattern-specific questions:

| Problem                        | Important question                                              |
| ------------------------------ | --------------------------------------------------------------- |
| 0/1 Knapsack                   | Can each item be used only once?                                |
| Unbounded Knapsack             | Can items be reused?                                            |
| Subset Sum                     | Are negative values possible? Is the target bounded?            |
| Longest Increasing Subsequence | Strictly increasing or non-decreasing?                          |
| String DP                      | Which operations are allowed: insertion, deletion, replacement? |
| Grid DP                        | Which movements are allowed? Are obstacles present?             |
| Interval DP                    | Does the answer depend on smaller intervals?                    |
| Tree DP                        | Can a node's answer be constructed from its children's answers? |

### **Example: 0/1 versus Unbounded Knapsack**

This distinction changes the DP implementation.

- 0/1 Knapsack: each item can be selected once.
- Unbounded Knapsack: an item can be selected repeatedly.

In a typical one-dimensional DP:

- 0/1 Knapsack uses backward capacity iteration.
- Unbounded Knapsack uses forward capacity iteration.

A seemingly small input property changes the transition and iteration order.

## **5.5 Trees and BSTs**

Ask:

| Question                                          | Why it matters                                                |
| ------------------------------------------------- | ------------------------------------------------------------- |
| Is this a binary tree or BST?                     | BST ordering can eliminate unnecessary traversal.             |
| Is the tree balanced?                             | Affects height and recursion depth.                           |
| Can duplicate values exist?                       | Affects comparison and search rules.                          |
| Can the tree be empty?                            | Determines base cases.                                        |
| Are we dealing with node identity or node values? | Important for intersection, LCA and reference-based problems. |
| Do we need a path, subtree, ancestor or level?    | Determines traversal strategy.                                |
| Are multiple queries expected?                    | Preprocessing may be useful.                                  |

Common clues:

| Problem requirement    | Possible direction                 |
| ---------------------- | ---------------------------------- |
| Level-order processing | BFS                                |
| Root-to-leaf paths     | DFS / Backtracking                 |
| Height or diameter     | Postorder DFS                      |
| Kth smallest in BST    | Inorder traversal                  |
| BST search             | Use BST ordering                   |
| Repeated LCA queries   | LCA preprocessing / Binary lifting |
| Path optimization      | Tree DP                            |

One subtle point: two linked-list or tree nodes with equal values are not necessarily the same node. When the problem involves intersection or node identity, clarify the intended meaning.

## **5.6 Graphs**

Graph questions can completely change the algorithm. Make sure you understand the graph structure before selecting a solution.

Ask:

1. Is the graph directed or undirected?
2. Is it weighted or unweighted?
3. Can edge weights be negative?
4. Can cycles exist?
5. Is the graph guaranteed to be connected?
6. Can multiple edges exist between two nodes?
7. Can self-loops exist?
8. How many vertices and edges are there?
9. Are we dealing with one source or all pairs?
10. Is the graph static, or can edges change?

### **Why do these matter?**

| Graph property / requirement       | Algorithm direction                           |
| ---------------------------------- | --------------------------------------------- |
| Unweighted shortest path           | BFS                                           |
| Nonnegative weighted shortest path | Dijkstra                                      |
| Negative edge weights              | Bellman-Ford or another appropriate algorithm |
| DAG shortest/longest path          | Topological sorting + DP                      |
| Connected components               | DFS / BFS / Union-Find                        |
| Cycle detection                    | DFS / Union-Find, depending on graph type     |
| Minimum spanning tree              | Kruskal / Prim                                |
| Strongly connected components      | Kosaraju / Tarjan                             |
| Dependency ordering                | Topological sorting                           |

### **Example: Shortest path**

The statement says:

> Find the shortest path between node A and node B.

Don't immediately decide to use Dijkstra.

First, identify:

- Is it weighted?
- If weighted, can weights be negative?
- Is it a DAG?
- Do we need only the distance or the actual path?
- Are we finding paths from one source or between all pairs?

Then select the appropriate algorithm.

For example:

```
Unweighted graph       -> BFS

Weighted, nonnegative  -> Dijkstra

Negative edge weights  -> Bellman-Ford may be needed

DAG                    -> Topological sort + DP
```

The number of vertices and edges also matters because it influences the data structure and implementation complexity.

For sparse graphs, adjacency lists are usually preferable to adjacency matrices.

## **5.7 Heaps and priority queues**

Ask:

1. Do I need the Kth largest/smallest or the top K elements?
2. Is the input static or streaming?
3. Can elements be inserted continuously?
4. Do duplicates count separately?
5. Do I need the result sorted?
6. Is K small relative to n?

| Requirement                            | Possible approach                  |
| -------------------------------------- | ---------------------------------- |
| Kth largest in unsorted array          | Min-heap of size K / Quickselect   |
| Top K frequent                         | Frequency map + heap / bucket sort |
| Running median                         | Two heaps                          |
| Merge K sorted lists                   | Min-heap                           |
| Continuously retrieve smallest element | Min-heap                           |

A particularly useful question:

> Do I need to sort the entire input, or do I only need the K best elements?

That can change a full sorting solution into a heap-based solution of approximately O(n log K).

## **5.8 Linked lists**

Ask:

- Is the list singly or doubly linked?
- Can it contain a cycle?
- Can the head be null?
- Can I modify the original links?
- Do I need to preserve the original list?
- Are we comparing node references or node values?
- Do I need to return a new head?

Common clues:

| Requirement        | Possible approach                                            |
| ------------------ | ------------------------------------------------------------ |
| Detect cycle       | Fast and slow pointers                                       |
| Find middle node   | Fast and slow pointers                                       |
| Reverse list       | Pointer manipulation                                         |
| Find intersection  | Two-pointer technique                                        |
| Merge sorted lists | Two pointers                                                 |
| Reorder nodes      | Pointer manipulation, often with middle finding and reversal |

## **5.9 Intervals and monotonic stacks**

**Intervals**

Ask:

- Are intervals already sorted?
- Are endpoints inclusive or exclusive?
- Do touching intervals overlap?
- Can intervals contain other intervals?
- Are duplicate intervals allowed?

For example, do these overlap?

```
[1, 3]
[3, 5]
```

If endpoints are inclusive, they overlap at 3. If they are half-open intervals, the answer may differ.

That changes the comparison condition during merging.

**Monotonic stack**

Ask yourself:

- Do I need the next greater or smaller element?
- Is it strictly greater or greater than or equal?
- Are equal values treated specially?
- Do I need the nearest previous or next element?

These questions determine whether you pop using `<`, `<=`, `>` or `>=`.

# **6. Step 6 — A better way to formulate questions**

There is a difference between asking questions and demonstrating reasoning.

Compare these approaches.

| Weak question               | Better question                                                                                                                           |
| --------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| Are there negative numbers? | Can the array contain negative values? I'm checking because that affects whether a standard sliding-window approach is valid.             |
| Is the array sorted?        | Is the input guaranteed to be sorted? If so, I can potentially use two pointers or binary search instead of additional lookup structures. |
| Can duplicates exist?       | Can duplicate values occur, and should repeated values generate separate answers?                                                         |
| Can I use extra space?      | Is there a space constraint? I'm considering a HashMap-based approach that would trade additional memory for linear expected time.        |
| Can I modify the array?     | Is the original array required to remain unchanged? Sorting would simplify the approach, but it modifies the input.                       |
| Do I need all answers?      | Should I return any valid solution or enumerate every valid solution? This affects both the search strategy and complexity.               |
| Is the graph weighted?      | Are edges weighted? If so, are weights nonnegative? That determines which shortest-path algorithm is applicable.                          |

## **Use this four-part structure when asking**

Whenever possible, formulate your question like this:

**1. Property:** What do I need to know?

**2. Reason:** Why am I asking?

**3. Consequence:** What changes based on the answer?

**4. Decision:** What will I do with the answer?

Example:

> Are all array elements nonnegative? I'm asking because the sliding-window approach relies on the window sum behaving predictably when expanding or shrinking. If negative values are possible, I'll need a different approach, such as prefix sums with an appropriate data structure.

This communicates your reasoning to the interviewer.

### **How many questions should you ask?**

Do not mechanically ask 15–20 questions.

Usually, start with 3–5 important questions that are not already answered by the statement.

Prioritize:

1. Anything affecting what constitutes a valid answer.
2. Anything affecting the algorithm's correctness.
3. Anything affecting time or space complexity.
4. Anything affecting input modification or output behavior.

Everything else can be handled through stated assumptions or edge-case testing.

# **7. Step 7 — A complete problem breakdown: from statement to algorithm**

Let's put all the ideas together.

## **Example 1: Find a subarray whose sum equals K**

Suppose the interviewer gives you:

> Given an integer array and an integer K, find a contiguous subarray whose sum equals K.

### **A. Decode the statement**

| Signal        | Meaning                      |
| ------------- | ---------------------------- |
| Integer array | Sequential input             |
| Subarray      | Contiguous elements          |
| Sum           | Aggregate operation          |
| Equals K      | Exact matching condition     |
| Find          | Need to locate a valid range |

### **B. Identify the ambiguities**

Ask:

1. Can the array contain negative numbers?
2. Do zeros and duplicates occur?
3. Do I need to return any one subarray, its indices, or all valid subarrays?
4. Is there guaranteed to be a valid subarray?
5. What is the maximum array size?

### **C. Think of brute force**

Try every possible subarray and calculate its sum.

Naively, this can take O(n³).

With running sums, it can be reduced to O(n²).

### **D. Identify the bottleneck**

We repeatedly calculate or examine overlapping ranges.

Can we reuse previous calculations?

Yes. Prefix sums.

Define:

\\[ prefix[i] = nums[0] + nums[1] + \dots + nums[i] \\]

The sum of a range from `j + 1` to `i` is:

\\[ prefix[i] - prefix[j] \\]

For the range sum to equal K:

\\[ prefix[i] - prefix[j] = K \\]

Rearranging:

\\[ prefix[j] = prefix[i] - K \\]

Now we need to check whether a previous prefix sum equal to `prefix[i] - K` exists.

That suggests a HashMap.

### **E. Connect the clarification to the algorithm**

| Answer to clarification     | Algorithm direction                    |
| --------------------------- | -------------------------------------- |
| All values positive         | Sliding window                         |
| Negative numbers allowed    | Prefix sum + HashMap                   |
| Need to count all subarrays | Prefix sum + frequency HashMap         |
| Need longest subarray       | Prefix sum + earliest-index HashMap    |
| Need any valid range        | Prefix sum + appropriate index storage |

Notice something important.

Even after identifying prefix sums, the required output determines what information we store in the HashMap.

For example:

- Existence: track whether a prefix sum has appeared.
- Count: track its frequency.
- Longest range: remember its earliest index.

### **F. Explain your approach**

You can say:

> I'll use prefix sums because they allow me to express every subarray sum as the difference between two prefix sums. Since negative values are possible, a standard sliding-window approach is not reliable. I'll maintain previously seen prefix sums in a HashMap and check whether the required complementary prefix exists.

This is a strong interview explanation because it connects the input property, mathematical observation and data structure choice.

## **Example 2: Find the shortest path between two nodes**

Problem:

> Given a graph, find the shortest path from source to destination.

### **A. Decode the statement**

| Signal               | Meaning                                     |
| -------------------- | ------------------------------------------- |
| Graph                | Vertices and edges                          |
| Source / destination | Path-search problem                         |
| Shortest             | Optimization based on path length or weight |

### **B. Identify the ambiguities**

Ask:

1. Is the graph directed or undirected?
2. Are edges weighted or unweighted?
3. Can weights be negative?
4. Is the graph connected?
5. Do I need the distance or the actual path?
6. How large are V and E?

### **C. Explore the basic approach**

A basic approach could explore possible paths and compare their lengths.

This quickly becomes expensive.

We need to avoid examining unnecessary paths.

### **D. Use the answers to identify the algorithm**

| Condition                            | Algorithm                         |
| ------------------------------------ | --------------------------------- |
| Unweighted graph                     | BFS                               |
| Weighted, nonnegative edges          | Dijkstra                          |
| Negative edges                       | Bellman-Ford may be needed        |
| DAG                                  | Topological order + DP            |
| All-pairs shortest path, small graph | Floyd-Warshall may be appropriate |

### **E. Additional decisions**

Graph size also influences representation:

- Sparse graph: typically adjacency list.
- Dense graph: adjacency matrix may be reasonable.

If the interviewer asks for the actual path, not just the distance, we also need to maintain predecessor information.

### **F. Explain the approach**

> First I'll determine whether the graph is weighted and whether negative weights are possible, because that determines the shortest-path algorithm. For an unweighted graph, BFS finds the shortest path in terms of number of edges. For nonnegative weighted edges, I would use Dijkstra. I'll also maintain parent information if the actual path needs to be returned.

## **Example 3: Generate all combinations with a target sum**

Problem:

> Given an array of candidates and a target, generate all combinations whose sum equals the target.

### **A. Decode the statement**

| Signal           | Meaning                                   |
| ---------------- | ----------------------------------------- |
| Generate         | We need to construct answers.             |
| All combinations | Order does not distinguish answers.       |
| Target sum       | We have a constraint for valid solutions. |

This suggests backtracking.

### **B. Identify ambiguities**

Ask:

1. Can the same element be used multiple times?
2. Can duplicate candidate values exist?
3. Should duplicate combinations be removed?
4. Are candidates positive or can they be negative?
5. Do we need all combinations or just one?

### **C. Construct the recursive decisions**

At each position, we decide whether to select an element.

Then determine:

- Can we stay at the same index?
- Must we advance?
- Should we skip duplicate values?
- Can we prune when the remaining target becomes negative?

### **D. Connect answers to recursion**

| Answer                                               | Design decision                                      |
| ---------------------------------------------------- | ---------------------------------------------------- |
| Reuse allowed                                        | Recurse using the same index                         |
| Reuse not allowed                                    | Recurse using the next index                         |
| Order does not matter                                | Use a starting index to avoid permutation duplicates |
| Duplicate inputs allowed, duplicate outputs unwanted | Sort and skip duplicates at each recursion depth     |
| All candidates positive                              | Remaining target becoming negative allows pruning    |
| Need one solution only                               | Stop after finding one valid answer                  |
| Need all solutions                                   | Continue exploring remaining branches                |

This is how you derive recursive structure from the problem statement rather than memorizing a template.

## **Example 4: Design a data structure for range queries**

Problem:

> Design a data structure that supports range sum queries.

### **A. Decode the statement**

We need a data structure that accepts an array and answers queries about ranges.

### **B. Ask the deciding questions**

1. How many elements?
2. How many queries?
3. Are updates required?
4. Are updates frequent?
5. Are queries expected to be fast?

### **C. Identify the solution**

| Requirement                               | Possible data structure |
| ----------------------------------------- | ----------------------- |
| Few queries                               | Simple traversal        |
| Many queries, no updates                  | Prefix sum array        |
| Many queries and point updates            | Fenwick Tree            |
| More general range operations and updates | Segment Tree            |

The problem is no longer just about processing an array. It is about selecting a data structure based on the workload.

This is an important habit: **understand the operations the problem requires, not just the input it gives you.**

# **8. Step 8 — Explain why your chosen algorithm works**

Once you select a pattern, don't jump straight into code.

You should be able to answer three things.

### **1. Why is this algorithm applicable?**

Identify the property that makes it work.

Examples:

- Binary search: sorted input or monotonic feasibility.
- Sliding window: the window can be adjusted predictably.
- Dijkstra: nonnegative edge weights.
- DP: meaningful states with reusable subproblems.
- Greedy: a locally optimal decision can be shown to preserve global optimality.

### **2. What is the key invariant?**

An invariant is a condition that remains true throughout the algorithm.

Examples:

| Pattern         | Important invariant                                                                                  |
| --------------- | ---------------------------------------------------------------------------------------------------- |
| Sliding window  | The current window satisfies the required validity condition at the relevant point in the algorithm. |
| Binary search   | The answer remains within the maintained search range.                                               |
| Two pointers    | The unprocessed search region contains every remaining possible candidate.                           |
| Monotonic stack | Stack elements maintain the required increasing/decreasing order.                                    |
| BFS             | Nodes are processed in nondecreasing distance from the source in an unweighted graph.                |
| Backtracking    | The current path represents exactly the choices made along the current recursion branch.             |
| DP              | Every computed state represents the correctly solved subproblem defined by its state variables.      |

### **3. What is the complexity?**

Explain:

- Time complexity.
- Auxiliary space complexity.
- Preprocessing costs, if any.
- Recursion stack, if any.
- Output space, if applicable.

Don't confuse auxiliary space with the memory required to store the returned result.

# **9. A complete interview conversation you can follow**

Let's put this into a practical script.

Imagine you're given an unfamiliar problem.

You can follow this sequence and adapt it to the problem.

### **Phase 1: Restate**

Start by explaining your understanding.

> Let me restate the problem. We are given [input], and we need to [operation/objective], subject to [conditions]. The expected output is [output]. Is that interpretation correct?

**Why?** To catch misunderstandings before investing time in a solution.

### **Phase 2: Clarify the deciding details**

Ask only what isn't already specified.

> I have a few questions about the input and expected output.

For an array problem, for example:

> Can negative values and duplicates occur?

> What is the maximum input size?

> Do I need to return one valid answer or all valid answers?

> Is there a space restriction, and can I modify the input?

**Why?** These facts eliminate unsuitable solutions.

### **Phase 3: Establish a baseline**

> I'll first think about a straightforward solution. We could [describe brute force]. However, this would take [complexity], which may not be sufficient for the given constraints.

**Why?** It demonstrates that you understand the underlying problem before optimizing it.

### **Phase 4: Identify the useful property**

> The important observation is that [explain the property]. We can exploit this instead of repeatedly [explain the bottleneck].

**Why?** This is where you demonstrate algorithmic thinking.

### **Phase 5: Propose the algorithm**

> Based on that observation, I would use [pattern / algorithm / data structure]. It is suitable because [reason]. The expected time complexity is [complexity], and the auxiliary space is [complexity].

**Why?** The interviewer can evaluate your reasoning before you commit to code.

### **Phase 6: Validate**

> Before coding, let me check the approach against a normal example and a few boundary cases.

Consider:

- Smallest valid input.
- Empty input, if allowed.
- Duplicates.
- Negative or zero values, where relevant.
- No valid answer.
- Multiple valid answers.
- Maximum-value or overflow scenarios.

### **Phase 7: Implement and review**

Explain important variables, state and invariants while coding.

After implementation, dry-run the code and state the final complexities.

# **10. How to think of clarifying questions for unfamiliar problems**

This is perhaps the most important skill to develop.

You will encounter problems you have never seen before. You cannot memorize a question list for every possible problem.

Instead, learn to derive questions from your potential solution.

Use this mental process:

```
                    Read the problem
                           |
                           v
              Understand the required output
                           |
                           v
                Imagine a simple solution
                           |
                           v
             What assumptions does it make?
                           |
                           v
             What input could break those
                    assumptions?
                           |
                           v
              Turn that into a question
                           |
                           v
             Does the answer change the
                 algorithm or complexity?
                      /        \
                    Yes         No
                     |           |
                     v           v
                Clarify it    Assume or
                             state default
```

### **Examples of deriving questions from an algorithm**

**Example A: You are considering sliding window.**

Your approach assumes that expanding and shrinking the window changes the condition predictably.

What could break that?

Negative numbers.

Therefore, ask:

> Can negative numbers occur?

**Example B: You are considering binary search.**

Your approach assumes sortedness or a monotonic condition.

What could break that?

An unsorted array or a non-monotonic feasibility condition.

Therefore, investigate those properties.

**Example C: You are considering backtracking.**

Your approach assumes that the search space can be explored with particular choices.

What could change that?

- Elements may be reused.
- Order may matter.
- Duplicate values may create duplicate results.
- Pruning may not be safe.

Therefore, ask those questions.

**Example D: You are considering DP.**

Your state and transition assume particular dependencies.

What could change that?

- Each item can be used once versus repeatedly.
- Strict versus non-strict comparison.
- The allowed operations.
- The required output: count, minimum cost or maximum value.

Therefore, clarify those details.

**The key habit:** Think of your potential solution first, identify its assumptions, and then ask the questions that verify those assumptions.

# **11. Common mistakes to avoid**

| Mistake                                                  | Better approach                                    |
| -------------------------------------------------------- | -------------------------------------------------- |
| Immediately jumping to a pattern                         | Decode the statement first.                        |
| Asking every question in a checklist                     | Ask only algorithm-changing questions.             |
| Asking questions already answered                        | Extract the guarantees from the statement.         |
| Asking "Any edge cases?"                                 | Identify specific boundary cases.                  |
| Assuming all numbers are positive                        | Explicitly verify sign constraints when relevant.  |
| Assuming all answers are unique                          | Clarify duplicate handling.                        |
| Assuming a solution always exists                        | Check the guarantee.                               |
| Choosing sliding window for every subarray problem       | Verify the required monotonic behavior.            |
| Choosing binary search just because an array is involved | Verify sortedness or monotonicity.                 |
| Writing DP before defining the state                     | Identify decisions, states and dependencies first. |
| Using greedy because it looks efficient                  | Explain why the greedy choice is optimal.          |
| Coding before explaining the approach                    | Describe the idea and complexity first.            |
| Optimizing without understanding brute force             | Identify the actual bottleneck.                    |

# **12. Your final interview cheat sheet**

This is the version I recommend practicing until it becomes natural.

## **A. Generic questions — Ask across most DSA problems**

| Priority                | Question                                                    | Purpose                                                        |
| ----------------------- | ----------------------------------------------------------- | -------------------------------------------------------------- |
| Essential               | What exactly should I return?                               | Understand the output contract.                                |
| Essential               | What is the maximum input size?                             | Determine acceptable complexity.                               |
| Essential when relevant | Can negative values, zeros or duplicates occur?             | Verify algorithm assumptions.                                  |
| Essential when relevant | Is the input sorted or does it have some special structure? | Identify exploitable properties.                               |
| Essential               | Is a solution guaranteed to exist?                          | Handle missing answers.                                        |
| Essential when relevant | Do I need one answer, all answers or just a count?          | Determine search/output strategy.                              |
| Conditional             | Can I modify the input?                                     | Determine in-place versus extra-space solutions.               |
| Conditional             | Are there repeated queries or updates?                      | Decide whether preprocessing or advanced data structures help. |
| Conditional             | Are there time or space restrictions?                       | Determine the optimization target.                             |

## **B. Pattern-specific questions**

| Pattern                 | Most important questions                                                                                |
| ----------------------- | ------------------------------------------------------------------------------------------------------- |
| Two pointers            | Is the input sorted? Are duplicates allowed?                                                            |
| Sliding window          | Is the region contiguous? Are values nonnegative? Can the window condition be maintained monotonically? |
| Prefix sum              | Are range queries frequent? Are updates required? Can negatives occur?                                  |
| Binary search           | Is the input sorted or the condition monotonic? First, last or any occurrence?                          |
| Binary search on answer | Can I define a monotonic feasibility check?                                                             |
| Hashing                 | Are duplicates important? What is the lookup requirement?                                               |
| Monotonic stack         | Next/previous greater or smaller? Strict or non-strict?                                                 |
| Backtracking            | One or all answers? Does order matter? Reuse allowed? Duplicates?                                       |
| DP                      | What are the decisions and states? Can items be reused? What is the optimization/counting objective?    |
| Trees                   | BST or binary tree? Balanced? Path, subtree or level?                                                   |
| Graphs                  | Directed? Weighted? Negative weights? Connected? Single-source or all-pairs?                            |
| Heap                    | Top K or Kth? Streaming? Do ties and duplicates matter?                                                 |
| Intervals               | Sorted? Inclusive endpoints? Do touching intervals overlap?                                             |
| Linked lists            | Cycle? Singly/doubly linked? In-place? Node identity or value?                                          |
| Trie                    | Prefix search or exact search? Character set? Dynamic inserts/deletes?                                  |
| Fenwick / Segment Tree  | How many queries and updates? What range operations?                                                    |

## **C. Algorithm discovery questions — Ask yourself**

These are primarily internal thinking prompts, not questions you need to ask the interviewer.

1. What is the simplest solution?
2. What is its time and space complexity?
3. What makes it inefficient?
4. Am I repeating calculations?
5. Can I exploit sortedness, ordering or monotonicity?
6. Can I maintain a window instead of examining every range?
7. Am I exploring choices? Can I prune?
8. Are there repeated subproblems?
9. Can I make a provably optimal local decision?
10. What algorithmic pattern addresses this bottleneck?
11. What assumptions does that algorithm require?
12. Have I verified those assumptions?

## **D. Before coding**

- Can I explain why the algorithm works?
- What is the key invariant?
- What happens at the boundaries?
- Have I considered overflow?
- Have I dry-run the approach?
- What is the time complexity?
- What is the auxiliary space complexity?
- Am I accounting for preprocessing and output size?

# **13. The most useful mental model to remember**

I want you to distinguish three layers whenever you solve an interview problem.

## **Layer 1: Understand the problem**

**What exactly am I solving?**

- Input
- Output
- Operation
- Objective
- Validity conditions
- Constraints
- Guarantees and assumptions

Use this layer to formulate generic clarification questions.

## **Layer 2: Discover the algorithm**

**What property can I exploit to solve it efficiently?**

- Start with brute force.
- Identify repeated work.
- Identify ordering and monotonicity.
- Identify constraints and special properties.
- Determine whether you need searching, optimization, counting or generation.
- Match these observations to a pattern.
- Verify the pattern's preconditions.

Use this layer to formulate pattern-specific questions.

## **Layer 3: Prove and implement**

**Why does my solution work?**

- Explain the algorithm.
- Identify its invariant or correctness argument.
- Dry-run examples.
- Test edge cases.
- Calculate complexity.
- Implement cleanly.

This is where you demonstrate that you can turn an idea into a correct solution.

## **My strongest recommendation**

Do not practice memorizing clarifying questions. Practice the following habit with every DSA problem:

**1. Read the statement and identify the important words.**

**2. Translate those words into an exact problem contract.**

**3. Extract all guarantees and constraints.**

**4. Think of the simplest possible solution.**

**5. Identify what makes that solution inefficient.**

**6. Think of a better algorithm and identify its assumptions.**

**7. Ask only the questions needed to verify those assumptions.**

**8. Explain why the algorithm works before coding.**

Here is the distinction I want you to remember:

| Average candidate                             | Strong candidate                                                            |
| --------------------------------------------- | --------------------------------------------------------------------------- |
| Sees a subarray and thinks sliding window.    | Checks whether the window can be maintained correctly.                      |
| Sees a sorted array and thinks binary search. | Identifies the exact monotonic property and required boundary.              |
| Sees combinations and thinks backtracking.    | Derives the recursive choices, reuse rules, duplicate handling and pruning. |
| Sees an optimization problem and thinks DP.   | Identifies states, decisions, transitions and overlapping subproblems.      |
| Asks whether a HashMap can be used.           | Identifies repeated lookup as the bottleneck and justifies hashing.         |
| Memorizes algorithm templates.                | Understands why an algorithm is applicable and when it fails.               |

**The ultimate objective is to move from:**

"What questions should I ask for this problem?"

**To:**

"What information do I need to establish, and what property of the problem will help me derive the solution?"

That is the skill that makes it possible to solve unfamiliar DSA interview problems rather than just recognize previously practiced ones.
