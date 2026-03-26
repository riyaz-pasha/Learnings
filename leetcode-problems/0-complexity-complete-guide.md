# Time & Space Complexity — Complete Derivation Guide

> **Philosophy of this guide:** Never memorize a complexity. Instead, learn to *read* the code and derive it. Every complexity here is derived from first principles with a worked example.

---

## Table of Contents

1. [How to Think About Complexity](#1-how-to-think-about-complexity)
2. [How to Calculate Time Complexity — The Framework](#2-how-to-calculate-time-complexity--the-framework)
3. [How to Calculate Space Complexity](#3-how-to-calculate-space-complexity)
4. [Recurrence Relations — Divide & Conquer](#4-recurrence-relations--divide--conquer)
5. [Data Structures](#5-data-structures)
6. [Sorting Algorithms](#6-sorting-algorithms)
7. [Searching Algorithms](#7-searching-algorithms)
8. [Graph Algorithms](#8-graph-algorithms)
9. [Tree Algorithms](#9-tree-algorithms)
10. [Dynamic Programming Patterns](#10-dynamic-programming-patterns)
11. [String Algorithms](#11-string-algorithms)
12. [Common Mistakes & Traps](#12-common-mistakes--traps)
13. [Master Quick Reference](#13-master-quick-reference)

---

## 1. How to Think About Complexity

### Big-O is a Question: "How does runtime grow as input grows?"

Big-O does NOT measure exact runtime in seconds. It measures the **growth rate**.

```
n = 10       → O(n²) runs 100 ops, O(n log n) runs ~33 ops
n = 1,000    → O(n²) runs 1,000,000 ops, O(n log n) runs ~10,000 ops
n = 1,000,000 → O(n²) is unusable, O(n log n) is fine
```

### The Hierarchy (slowest growth → fastest growth)

```
O(1) < O(log n) < O(√n) < O(n) < O(n log n) < O(n²) < O(n³) < O(2ⁿ) < O(n!)

Constant   Log     Root    Linear  Linearithmic  Quadratic  Cubic  Exponential  Factorial
```

### The Three Notations

```
O(f(n))  — Big-O    — upper bound (worst case) — what you use 95% of the time
Ω(f(n))  — Omega    — lower bound (best case)
Θ(f(n))  — Theta    — tight bound (best = worst)
```

In interviews and practice, "time complexity = O(f(n))" almost always means worst case.

### Dropping Constants and Lower-Order Terms

```
3n² + 7n + 100  →  O(n²)       (drop 3, drop 7n, drop 100)
5n log n + 2n   →  O(n log n)  (drop 5, drop 2n — lower order)
n + n/2 + n/4   →  O(n)        (geometric series, still linear)
```

**Rule:** Keep only the fastest-growing term. Drop all constants.

---

## 2. How to Calculate Time Complexity — The Framework

### Step 1: Identify what "n" is

This is the most important step and where most mistakes start.

```java
// Example: process all elements of an array
// → n = array length

// Example: traverse a binary tree
// → n = number of nodes

// Example: nested loops over a grid
// → n = rows × cols, or separately n = rows, m = cols

// Example: graph traversal
// → n = vertices (V), m or E = edges
```

### Step 2: Count operations as a function of n

Three patterns cover almost every problem:

**Pattern A — Sequential code: add the complexities**

```java
// O(n)
for (int i = 0; i < n; i++) { ... }        // n ops

// O(n²) total? NO — they're sequential, so O(n) + O(n²) = O(n²)
for (int i = 0; i < n; i++) { ... }         // O(n)
for (int i = 0; i < n; i++) {               // O(n²)
    for (int j = 0; j < n; j++) { ... }
}
// Total: O(n) + O(n²) = O(n²) — keep the dominant term
```

**Pattern B — Nested loops: multiply the complexities**

```java
for (int i = 0; i < n; i++) {          // runs n times
    for (int j = 0; j < n; j++) {      // runs n times per i
        doWork();                        // O(1) work
    }
}
// Total: n × n × O(1) = O(n²)
```

**Pattern C — Recursive code: use a recurrence relation**

```java
void recurse(int n) {
    if (n == 0) return;     // base case
    recurse(n - 1);         // one recursive call, n shrinks by 1
}
// T(n) = T(n-1) + O(1)  →  T(n) = O(n)
```

### Step 3: Apply the rules

**Rule 1 — Constant work inside a loop doesn't change the loop's complexity**

```java
for (int i = 0; i < n; i++) {
    int x = arr[i] * 2 + arr[i-1];   // 3 operations — still O(1) per iteration
    System.out.println(x);
}
// Total: O(n), not O(3n)
```

**Rule 2 — The loop bound determines the loop's cost, not the body**

```java
for (int i = 0; i < n; i++) {
    for (int j = i; j < n; j++) {    // j starts at i, NOT 0
        doWork();
    }
}
// Inner loop runs: n + (n-1) + (n-2) + ... + 1 = n(n+1)/2 ≈ n²/2 → O(n²)
```

**Rule 3 — Log appears when you halve (or multiply) the search space**

```java
while (n > 1) {
    n = n / 2;   // halving each step
}
// How many halvings until n reaches 1?
// n / 2^k = 1  →  k = log₂(n) → O(log n)
```

**Rule 4 — Amortized analysis: spread occasional expensive ops over many cheap ones**

```java
// Dynamic array (ArrayList): occasionally doubles capacity
// Most appends: O(1)
// Doubling append: O(n) — copies all elements
// But doublings happen at sizes 1, 2, 4, 8, ... n
// Total copy work: 1 + 2 + 4 + ... + n = 2n → O(n) total for n appends
// Amortized per append: O(n) / n = O(1)
```

### Worked Examples: Deriving from Code

**Example 1 — Two-pointer**

```java
int left = 0, right = n - 1;
while (left < right) {
    left++;    // or right-- or both move
    right--;
}
// Together left+right move at most n steps total → O(n)
```

**Example 2 — Nested loop with non-obvious bound**

```java
for (int i = 1; i < n; i *= 2) {    // i = 1, 2, 4, 8, ... n → log n iterations
    for (int j = 0; j < i; j++) {   // j runs i times
        doWork();
    }
}
// Total work: 1 + 2 + 4 + ... + n = 2n - 1 → O(n)
// (geometric series: first term 1, ratio 2, n terms → sum ≈ 2n)
```

**Example 3 — Tricky inner loop**

```java
for (int i = 0; i < n; i++) {
    for (int j = 1; j < n; j *= 2) {   // inner loop is log n
        doWork();
    }
}
// Outer: n, inner: log n → O(n log n)
```

**Example 4 — String concatenation (common trap)**

```java
String result = "";
for (int i = 0; i < n; i++) {
    result += chars[i];           // creates a NEW string each time!
}
// Iteration 1: copy 1 char
// Iteration 2: copy 2 chars
// ...
// Iteration n: copy n chars
// Total: 1 + 2 + ... + n = n(n+1)/2 → O(n²) NOT O(n)
// Fix: use StringBuilder → O(n)
```

**Example 5 — HashMap operations inside a loop**

```java
Map<Integer, Integer> map = new HashMap<>();
for (int x : arr) {
    map.put(x, map.getOrDefault(x, 0) + 1);   // O(1) average per op
}
// Total: O(n) × O(1) = O(n)
// But worst case HashMap is O(n) per op (all keys hash-collide) → O(n²) worst
// In interviews: say O(n) average, mention O(n²) worst if relevant
```

---

## 3. How to Calculate Space Complexity

### What counts as space?

```
Input space      — usually NOT counted (it's given to you)
Auxiliary space  — extra space YOUR algorithm uses beyond the input
                   THIS is what "space complexity" usually means

Stack space      — recursion depth × frame size
Heap space       — arrays, lists, maps you allocate
```

### Rule: Only count what your algorithm CREATES

```java
// O(1) space — just a few variables, no matter how large n is
public int sum(int[] arr) {
    int total = 0;                  // 1 variable
    for (int x : arr) total += x;
    return total;
}

// O(n) space — allocates an array proportional to input
public int[] doubled(int[] arr) {
    int[] result = new int[arr.length];   // n slots allocated
    for (int i = 0; i < arr.length; i++) result[i] = arr[i] * 2;
    return result;
}

// O(n) space from recursion — stack depth grows with n
public int factorial(int n) {
    if (n == 0) return 1;
    return n * factorial(n - 1);   // n frames on the call stack simultaneously
}
```

### Recursion Stack Space

```
Recursion depth × space per frame = total stack space

Linear recursion (factorial, DFS on a line):
  depth = n, frame = O(1) → stack space = O(n)

Binary recursion (merge sort):
  depth = log n, frame = O(1) per frame
  → stack space = O(log n)
  (NOTE: the merge step allocates O(n) heap space separately)

Tree recursion (Fibonacci naive):
  depth = n (longest branch), frame = O(1) → O(n) stack space
```

### Worked Example: Merge Sort Space

```java
void mergeSort(int[] arr, int left, int right) {
    if (left >= right) return;
    int mid = (left + right) / 2;
    mergeSort(arr, left, mid);        // left half
    mergeSort(arr, mid + 1, right);   // right half
    merge(arr, left, mid, right);     // O(n) temporary array here
}

// Stack space:
//   Recursion depth = log n (halving each time)
//   Each frame: O(1) variables (left, right, mid)
//   Stack total: O(log n)

// Heap space:
//   merge() allocates a temp array of size (right - left + 1)
//   At any moment, only ONE merge is executing (after recursion returns)
//   Largest merge: O(n) at the top level
//   Total heap: O(n)

// Total space = O(n) — the heap dominates
```

---

## 4. Recurrence Relations — Divide & Conquer

### How to write and solve them

A recurrence describes T(n) in terms of T(smaller n).

**General form:**
```
T(n) = a × T(n/b) + f(n)

a = number of recursive subproblems
b = factor by which input shrinks
f(n) = work done outside the recursive calls (combine step)
```

### Master Theorem

Given T(n) = a·T(n/b) + O(n^d):

```
Compare a vs b^d:

Case 1: a < b^d  →  T(n) = O(n^d)              (combine dominates)
Case 2: a = b^d  →  T(n) = O(n^d · log n)       (balanced)
Case 3: a > b^d  →  T(n) = O(n^(log_b a))       (recursion dominates)
```

### Applying It — Worked Examples

**Merge Sort: T(n) = 2·T(n/2) + O(n)**

```
a=2, b=2, d=1
b^d = 2^1 = 2
a = 2 = b^d  →  Case 2  →  O(n^1 · log n) = O(n log n) ✓
```

**Binary Search: T(n) = 1·T(n/2) + O(1)**

```
a=1, b=2, d=0
b^d = 2^0 = 1
a = 1 = b^d  →  Case 2  →  O(n^0 · log n) = O(log n) ✓
```

**Strassen's Matrix Multiply: T(n) = 7·T(n/2) + O(n²)**

```
a=7, b=2, d=2
b^d = 2^2 = 4
a=7 > 4  →  Case 3  →  O(n^(log₂ 7)) = O(n^2.807) ✓
(better than naïve O(n³))
```

**Naive Fibonacci: T(n) = T(n-1) + T(n-2) + O(1)**

```
Approximate: T(n) ≈ 2·T(n-1)
Solving: T(n) = 2^n → O(2^n)
(Master theorem doesn't directly apply when b < 1, solve by expansion)
```

### Solving by Expansion (when Master Theorem doesn't apply)

**Example: T(n) = T(n-1) + O(n)**  (e.g., selection sort inner loop)

```
T(n) = T(n-1) + n
     = T(n-2) + (n-1) + n
     = T(n-3) + (n-2) + (n-1) + n
     = ...
     = T(0) + 1 + 2 + ... + n
     = O(1) + n(n+1)/2
     = O(n²)
```

**Example: T(n) = T(n/2) + O(1)** (binary search)

```
T(n) = T(n/2) + 1
     = T(n/4) + 1 + 1
     = T(n/8) + 1 + 1 + 1
     = ...
After k steps: T(n/2^k) + k
When n/2^k = 1 → k = log₂ n
T(n) = T(1) + log₂ n = O(log n) ✓
```

---

## 5. Data Structures

### 5.1 Array

```
Access   arr[i]          → O(1)   — direct memory address calculation
Search   linear scan     → O(n)   — check each element
Search   binary (sorted) → O(log n) — halve search space each step
Insert   at end          → O(1)   — if space available
Insert   at index i      → O(n)   — shift all elements right of i
Delete   at index i      → O(n)   — shift all elements left
Delete   at end          → O(1)

Space: O(n)
```

**Why insert at index i is O(n):**

```
arr = [1, 2, 3, 4, 5]
Insert 9 at index 2:
  shift 5 → position 5
  shift 4 → position 4
  shift 3 → position 3
  place 9 at position 2
Result: [1, 2, 9, 3, 4, 5]
Worst case: insert at index 0 → shift all n elements → O(n)
```

---

### 5.2 Dynamic Array (ArrayList / Vector)

```
Access   → O(1)          — same as array
Append   → O(1) amortized — occasionally O(n) when resizing
Insert   at index i → O(n) — shift + possible resize
Delete   at index i → O(n) — shift
Search   → O(n)

Space: O(n)
```

**Amortized append derivation:**

```
Capacity doubles when full: 1 → 2 → 4 → 8 → ... → n
Copy costs at doublings: 1 + 2 + 4 + ... + n/2 = n - 1 < n
Total copy work for n appends: O(n)
Per-append amortized: O(n)/n = O(1)
```

---

### 5.3 Linked List

```
                         Singly Linked    Doubly Linked
Access by index          O(n)             O(n)
Search                   O(n)             O(n)
Insert at head           O(1)             O(1)
Insert at tail           O(1)*            O(1)*
Insert at position i     O(n)             O(n)
Delete head              O(1)             O(1)
Delete tail              O(n) singly**    O(1) doubly
Delete node (given ptr)  O(1)             O(1)

* if tail pointer maintained
** singly linked can't go backwards to update prev

Space: O(n)
```

**Why access by index is O(n):**

```
To get element at index 5:
  start → node0 → node1 → node2 → node3 → node4 → node5
Must traverse all preceding nodes. No direct address calculation.
```

---

### 5.4 Stack

Implemented with dynamic array or linked list.

```
Push  → O(1)    — add to top
Pop   → O(1)    — remove from top
Peek  → O(1)    — view top
Search → O(n)   — must pop everything to find it

Space: O(n)
```

**Call stack space** — when recursion is used, the OS call stack acts as an implicit stack. Each frame costs O(1), depth costs O(depth).

---

### 5.5 Queue

Implemented with doubly linked list or circular array.

```
Enqueue (add)   → O(1)
Dequeue (remove) → O(1)
Peek/Front       → O(1)
Search           → O(n)

Space: O(n)
```

**ArrayDeque vs LinkedList for Queue in Java:**

```
ArrayDeque:   O(1) amortized for all ops, better cache performance
LinkedList:   O(1) worst case, but pointer overhead and cache misses
→ Prefer ArrayDeque
```

---

### 5.6 HashMap / HashSet

```
Insert   → O(1) average,  O(n) worst (all keys collide to one bucket)
Search   → O(1) average,  O(n) worst
Delete   → O(1) average,  O(n) worst
Iterate  → O(n + capacity) — iterates over all buckets

Space: O(n)
```

**How hashing works and why worst case is O(n):**

```
Key → hash function → bucket index

If all n keys hash to the same bucket:
  bucket = [k1 → k2 → k3 → ... → kn]  (linked list in bucket)
  Search for kn = traverse all n nodes → O(n)

Java's HashMap mitigates this: when a bucket's linked list exceeds 8 elements,
it converts to a Red-Black tree → O(log n) worst case per bucket.
So realistic worst case in Java: O(log n) per op, not O(n).
```

**Load factor and rehashing:**

```
Load factor = (number of entries) / (number of buckets)
Default in Java: 0.75
When load factor exceeded: rehash (double capacity, re-insert all)
Rehash cost: O(n), but amortized across many inserts → O(1) per insert
```

---

### 5.7 TreeMap / TreeSet (Red-Black Tree)

```
Insert   → O(log n)
Search   → O(log n)
Delete   → O(log n)
Min/Max  → O(log n)
Floor/Ceiling → O(log n)
Iterate in order → O(n)

Space: O(n)
```

**Why log n — deriving from BST height:**

```
A balanced BST with n nodes has height = log₂ n
(Each level doubles the nodes: 1, 2, 4, ..., n → log₂ n levels)
Every operation goes from root to leaf → O(log n)
Red-Black tree stays balanced by rotation → guarantees O(log n) always
```

**HashMap vs TreeMap:**

```
HashMap:  O(1) avg all ops, no ordering
TreeMap:  O(log n) all ops, keys always sorted
→ Use TreeMap when you need sorted keys, range queries, floor/ceiling
→ Use HashMap otherwise
```

---

### 5.8 PriorityQueue (Binary Heap)

```
Insert (offer)   → O(log n)   — add to end, bubble up
Extract min/max  → O(log n)   — swap root with last, bubble down
Peek min/max     → O(1)       — root is always min/max
Search arbitrary → O(n)       — heap property only guarantees root
Build heap       → O(n)       — heapify (NOT O(n log n))
Delete arbitrary → O(log n)   — if position known; O(n) to find it first

Space: O(n)
```

**Why insert is O(log n):**

```
Heap is a complete binary tree → height = log n
Insert: place at next leaf position, bubble up (swap with parent if smaller)
At most log n swaps → O(log n)
```

**Why build heap is O(n) not O(n log n):**

```
Naïve: insert n elements one by one = n × O(log n) = O(n log n)

Heapify (bottom-up): start from last internal node, sift down
Work at height h: nodes at that height = n/2^(h+1), sift-down cost = O(h)
Total = Σ (n/2^(h+1)) × h  for h = 0 to log n
      = n × Σ h/2^h
      = n × 2  (geometric series)
      = O(n)
```

---

### 5.9 Binary Search Tree (BST)

```
                    Average        Worst (unbalanced / sorted input)
Insert              O(log n)       O(n)   — becomes linked list
Search              O(log n)       O(n)
Delete              O(log n)       O(n)
Min / Max           O(log n)       O(n)
Inorder traversal   O(n)           O(n)

Space: O(n)
```

**When BST degrades to O(n):**

```
Insert: 1, 2, 3, 4, 5 in order
          1
           \
            2
             \
              3
               \
                4
                 \
                  5
→ Tree becomes a right-skewed linked list
→ Search for 5: traverse all n nodes → O(n)
Solution: use self-balancing BST (AVL, Red-Black) → always O(log n)
```

---

### 5.10 Trie (Prefix Tree)

```
Insert word of length L    → O(L)
Search word of length L    → O(L)
Prefix search              → O(L + matches)
Delete                     → O(L)

Space: O(ALPHABET_SIZE × N × L) worst case
       where N = number of words, L = average length
       In practice much less due to shared prefixes
```

**Why O(L) not O(n):**

```
Trie doesn't compare against all n stored words.
Each character of the query descends one level in the trie.
L characters → L steps → O(L) regardless of how many words are stored.
```

---

### 5.11 Graph Representations

```
                        Adjacency List      Adjacency Matrix
Space                   O(V + E)            O(V²)
Add edge                O(1)                O(1)
Remove edge             O(degree)           O(1)
Check if (u,v) exists   O(degree(u))        O(1)
Get all neighbors of u  O(degree(u))        O(V)
Best for                Sparse graphs       Dense graphs
```

---

## 6. Sorting Algorithms

### 6.1 Bubble Sort

```java
for (int i = 0; i < n-1; i++) {
    for (int j = 0; j < n-i-1; j++) {
        if (arr[j] > arr[j+1]) swap(arr, j, j+1);
    }
}
```

```
Outer loop: n-1 times
Inner loop: n-1-i times (decreasing, but average n/2)
Total comparisons: (n-1) + (n-2) + ... + 1 = n(n-1)/2 → O(n²)

Best case:  O(n)   — with early-exit flag when no swaps occur
Worst case: O(n²)  — reverse sorted
Space:      O(1)   — in-place
Stable:     Yes
```

---

### 6.2 Selection Sort

```java
for (int i = 0; i < n-1; i++) {
    int minIdx = i;
    for (int j = i+1; j < n; j++) {
        if (arr[j] < arr[minIdx]) minIdx = j;
    }
    swap(arr, i, minIdx);
}
```

```
Always runs: (n-1) + (n-2) + ... + 1 = n(n-1)/2 comparisons
Best/Worst: always O(n²)  — no early exit possible
Space: O(1)
Stable: No  — a swap can move elements past each other
```

---

### 6.3 Insertion Sort

```java
for (int i = 1; i < n; i++) {
    int key = arr[i], j = i - 1;
    while (j >= 0 && arr[j] > key) {
        arr[j+1] = arr[j];
        j--;
    }
    arr[j+1] = key;
}
```

```
Best case:  O(n)   — already sorted, inner while never runs
Worst case: O(n²)  — reverse sorted, inner while runs i times each

Derivation for worst case:
  i=1: inner runs 1 time
  i=2: inner runs 2 times
  ...
  i=n-1: inner runs n-1 times
  Total: 1+2+...+(n-1) = n(n-1)/2 → O(n²)

Space: O(1)
Stable: Yes
Great for: nearly sorted arrays, small arrays (n < 20 or so)
```

---

### 6.4 Merge Sort

```java
void mergeSort(int[] arr, int l, int r) {
    if (l >= r) return;
    int mid = (l + r) / 2;
    mergeSort(arr, l, mid);
    mergeSort(arr, mid+1, r);
    merge(arr, l, mid, r);    // O(r-l+1) = O(n) at top level
}
```

```
Recurrence: T(n) = 2T(n/2) + O(n)
Master theorem: a=2, b=2, d=1 → a = b^d → Case 2 → O(n log n)

Intuitive: log n levels of recursion, each level does O(n) total merge work

Time:  O(n log n) always (best, worst, average — never degrades)
Space: O(n) for merge temp arrays + O(log n) stack = O(n) total
Stable: Yes
```

**Merge step derivation:**

```
Merging two sorted halves of sizes n/2 each:
  Compare front elements, copy smaller to temp array
  At most n-1 comparisons for n elements total → O(n)

Total at level 0 (top): 1 merge of size n → O(n)
Total at level 1: 2 merges of size n/2 → 2 × O(n/2) = O(n)
Total at level 2: 4 merges of size n/4 → 4 × O(n/4) = O(n)
...
log n levels, each O(n) → total O(n log n)
```

---

### 6.5 Quick Sort

```java
void quickSort(int[] arr, int low, int high) {
    if (low < high) {
        int pi = partition(arr, low, high);   // O(n) partition
        quickSort(arr, low, pi - 1);
        quickSort(arr, pi + 1, high);
    }
}
```

```
Best / Average case: T(n) = 2T(n/2) + O(n) → O(n log n)
  (pivot splits array roughly in half each time)

Worst case: T(n) = T(n-1) + O(n) → O(n²)
  (pivot always smallest/largest element — sorted/reverse-sorted input)

Derivation of worst case:
  T(n) = T(n-1) + n
       = T(n-2) + (n-1) + n
       = ... = n + (n-1) + ... + 1 = O(n²)

Time:  O(n log n) average,  O(n²) worst
Space: O(log n) avg stack,  O(n) worst stack (skewed recursion)
Stable: No
In-place: Yes (only stack space)

Mitigation: random pivot selection makes worst case astronomically unlikely
```

---

### 6.6 Heap Sort

```java
// Build max-heap: O(n)
// Extract max n times: n × O(log n) = O(n log n)
```

```
Time:  O(n log n) always (best/worst/average)
Space: O(1)  — in-place (no auxiliary arrays like merge sort)
Stable: No

Not used much in practice: poor cache performance (heap jumps around memory)
Good theoretical properties but quicksort is faster in practice
```

---

### 6.7 Counting Sort

```java
// Works on integers in range [0, k]
int[] count = new int[k+1];
for (int x : arr) count[x]++;
// Reconstruct from count array
```

```
Time:  O(n + k)  — n to count, k to output
Space: O(k)      — count array of size k+1

When to use: k = O(n) → total O(n), beats comparison-based sorts
When NOT to: k >> n → wastes space and time
```

---

### 6.8 Radix Sort

```java
// Sort by least significant digit first, using counting sort per digit
// d = number of digits, each digit pass is O(n + base)
```

```
Time:  O(d × (n + base))  — e.g., 32-bit ints: d=10 digits, base=10 → O(10n) = O(n)
Space: O(n + base)

Beats O(n log n) when d is constant (bounded integers)
```

---

### 6.9 Comparison Lower Bound

```
Any comparison-based sort is Ω(n log n) in the worst case.

Proof idea: there are n! possible orderings of n elements.
A sorting algorithm must distinguish all n! orderings.
Each comparison gives 1 bit of info → log₂(n!) bits needed.
log₂(n!) = Σ log₂(i) for i=1 to n ≈ n log n by Stirling's approximation.
So at least n log n comparisons needed → Ω(n log n) lower bound.

Non-comparison sorts (counting, radix) bypass this by using element values directly.
```

---

### Sorting Summary

| Algorithm      | Best       | Average    | Worst      | Space     | Stable |
|----------------|------------|------------|------------|-----------|--------|
| Bubble         | O(n)       | O(n²)      | O(n²)      | O(1)      | Yes    |
| Selection      | O(n²)      | O(n²)      | O(n²)      | O(1)      | No     |
| Insertion      | O(n)       | O(n²)      | O(n²)      | O(1)      | Yes    |
| Merge          | O(n log n) | O(n log n) | O(n log n) | O(n)      | Yes    |
| Quick          | O(n log n) | O(n log n) | O(n²)      | O(log n)  | No     |
| Heap           | O(n log n) | O(n log n) | O(n log n) | O(1)      | No     |
| Counting       | O(n+k)     | O(n+k)     | O(n+k)     | O(k)      | Yes    |
| Radix          | O(dn)      | O(dn)      | O(dn)      | O(n+b)    | Yes    |

---

## 7. Searching Algorithms

### 7.1 Linear Search

```
Time:  O(n) — check each element one by one
Space: O(1)
Works on: unsorted, any data
```

### 7.2 Binary Search

```java
int left = 0, right = n - 1;
while (left <= right) {
    int mid = left + (right - left) / 2;   // avoid overflow vs (left+right)/2
    if (arr[mid] == target) return mid;
    else if (arr[mid] < target) left = mid + 1;
    else right = mid - 1;
}
```

```
Derivation:
  After 1 step: search space = n/2
  After 2 steps: n/4
  After k steps: n/2^k
  When n/2^k = 1 → k = log₂ n

Time:  O(log n)
Space: O(1) iterative,  O(log n) recursive (stack frames)
Requires: sorted array
```

**Common binary search variants:**

```java
// Find first position where arr[i] >= target (leftmost)
int left = 0, right = n;
while (left < right) {
    int mid = (left + right) / 2;
    if (arr[mid] < target) left = mid + 1;
    else right = mid;
}
// left = first index where arr[i] >= target

// Find last position where arr[i] <= target (rightmost)
int left = -1, right = n - 1;
while (left < right) {
    int mid = (left + right + 1) / 2;   // +1 prevents infinite loop
    if (arr[mid] > target) right = mid - 1;
    else left = mid;
}
```

---

## 8. Graph Algorithms

### 8.1 BFS

```
Time:  O(V + E)
  — V: each vertex enqueued once (dequeued once)
  — E: each edge examined once (when processing its source vertex)

Space: O(V)  — queue holds at most V nodes at once
              — visited array is O(V)

Derivation:
  Each vertex: enqueue once = O(1), dequeue once = O(1) → O(V) total
  Each edge (u,v): examined when u is dequeued → each edge examined once → O(E) total
  Total: O(V + E)
```

### 8.2 DFS

```
Time:  O(V + E)  — same reasoning as BFS (visit each vertex/edge once)
Space: O(V)      — recursion stack depth up to V in worst case (long chain)
                 — visited array O(V)

Worst case stack depth: O(V) when graph is a long chain (linked list shape)
Best case stack depth:  O(1) when graph is a star (all connected to one center)
```

### 8.3 Dijkstra's

```
Time:  O((V + E) log V)
  — Each vertex extracted from heap once: V extractions × O(log V) = O(V log V)
  — Each edge may cause a heap push: E pushes × O(log V) = O(E log V)
  — Total: O((V + E) log V)

Space: O(V + E)
  — dist array: O(V)
  — heap can hold up to O(E) entries (one per edge relaxation)
  — adjacency list: O(V + E)

With Fibonacci heap (theoretical): O(E + V log V)
  — decrease-key in O(1), delete-min in O(log V)
  — Not used in practice due to implementation complexity
```

**Why E entries in the heap:**

```
Each time we relax an edge (u,v), we push (new_cost, v) into the heap.
Each edge can be relaxed at most once per direction.
So heap has at most E pushes → O(E) entries at peak.
Each push/pop is O(log(heap_size)) ≤ O(log E) ≤ O(log V²) = O(2 log V) = O(log V)
```

### 8.4 Bellman-Ford

```
Time:  O(V × E)
  — V-1 outer iterations (relax all edges V-1 times)
  — E inner work (check all edges per iteration)
  — Total: (V-1) × E ≈ O(VE)

Space: O(V)  — just the dist array

Why V-1 iterations?
  A shortest path in a graph with V nodes has at most V-1 edges.
  After i iterations, all shortest paths using ≤ i edges are found.
  After V-1 iterations, all shortest paths (no negative cycle) are found.
```

### 8.5 Floyd-Warshall

```
Time:  O(V³)
  — 3 nested loops each from 0 to V-1 → exactly V³ iterations
  — Each iteration: O(1) work

Space: O(V²)  — the distance matrix

Derivation:
  for k in 0..V-1:         ← V iterations
    for i in 0..V-1:       ← V iterations
      for j in 0..V-1:     ← V iterations
        dist[i][j] = min(dist[i][j], dist[i][k] + dist[k][j])  ← O(1)
  Total: V × V × V = V³ → O(V³)
```

### 8.6 Topological Sort — Kahn's

```
Time:  O(V + E)
  — Build in-degree array: O(V + E)  (iterate all edges once)
  — BFS-like processing: each vertex enqueued once = O(V)
  — Each edge processed once = O(E)
  — Total: O(V + E)

Space: O(V)  — in-degree array, queue
```

### 8.7 Kruskal's MST

```
Time:  O(E log E)
  — Sort edges: O(E log E)
  — Process each edge with Union-Find: O(E × α(V)) ≈ O(E)
  — Total: O(E log E)

Note: O(E log E) = O(E log V²) = O(2E log V) = O(E log V)
  So O(E log E) and O(E log V) are equivalent

Space: O(V + E)  — Union-Find: O(V), sorted edges: O(E)
```

### 8.8 Prim's MST

```
With binary heap:
  Time:  O(E log V)
    — Each vertex extracted once: O(V log V)
    — Each edge may cause heap update: O(E log V)
    — Total: O((V + E) log V) = O(E log V) for connected graphs (E ≥ V-1)

With adjacency matrix (dense graphs):
  Time:  O(V²)
    — Find minimum key each time by linear scan: O(V) × V times = O(V²)
    — Better when E ≈ V² (dense graphs) because O(V²) < O(E log V) = O(V² log V)

Space: O(V) with heap,  O(V²) with matrix
```

### 8.9 Union-Find

```
Operations: find(x), union(x, y), connected(x, y)

Without optimization: O(n) per operation (tree can become chain)

With Path Compression only: O(log n) amortized
With Union by Rank only:    O(log n) worst case
With both:                  O(α(n)) amortized ≈ O(1) practical

α(n) = inverse Ackermann function
  α(n) < 5 for any n that could exist in the observable universe
  Treat as O(1) in practice

Space: O(V)  — parent and rank arrays
```

---

## 9. Tree Algorithms

### 9.1 Binary Tree Traversals (Inorder, Preorder, Postorder)

```
Time:  O(n)  — visit each node exactly once
Space: O(h)  — recursion stack = tree height h

h = O(log n) for balanced trees
h = O(n)     for skewed trees (worst case linked list)

So space = O(log n) balanced,  O(n) worst case
```

**Iterative traversals use O(h) explicit stack** — same space, no recursion overhead.

### 9.2 Level-Order Traversal (BFS on tree)

```
Time:  O(n)  — visit each node once
Space: O(w)  — w = maximum width of tree

w = O(1)     for skewed tree (linked list)
w = O(n/2) = O(n)  for perfect binary tree (last level has n/2 nodes)
So space: O(n) worst case
```

### 9.3 BST Operations

```
                    Balanced (AVL / RB)    Unbalanced BST
Insert              O(log n)               O(n) worst
Search              O(log n)               O(n) worst
Delete              O(log n)               O(n) worst
Successor/Predecessor O(log n)             O(n) worst
Range query [a,b]   O(log n + k)           O(n)
  where k = number of elements in range
```

### 9.4 Lowest Common Ancestor (LCA)

```
Naive (walk up from both nodes): O(h) per query
  h = height of tree = O(log n) balanced, O(n) worst

Binary Lifting (preprocessing):
  Preprocess: O(n log n) time, O(n log n) space
  Query: O(log n)

Euler Tour + Sparse Table (RMQ-based):
  Preprocess: O(n log n) time, O(n log n) space
  Query: O(1)
```

### 9.5 Segment Tree

```
Build:   O(n)     — build from array bottom-up
Query:   O(log n) — range query (sum, min, max)
Update:  O(log n) — point update, propagate up

Space: O(4n) ≈ O(n)  — typically allocate 4× input size

Lazy Propagation (range updates):
  Range update + range query: O(log n) each
```

### 9.6 Fenwick Tree (Binary Indexed Tree)

```
Build:   O(n)     — or O(n log n) with repeated updates
Query:   O(log n) — prefix sum query
Update:  O(log n) — point update

Space: O(n)
Simpler implementation than segment tree, but less flexible
```

---

## 10. Dynamic Programming Patterns

### How to derive DP complexity

```
Time  = (number of unique states) × (work per state)
Space = (number of unique states) × (space per state)
```

### 10.1 1D DP

```java
// Fibonacci with memoization
int[] dp = new int[n+1];
// State: dp[i] = fib(i)
// Transition: dp[i] = dp[i-1] + dp[i-2]  → O(1) per state

States: n+1
Work per state: O(1)
Total time: O(n)
Space: O(n)   — can optimize to O(1) with rolling variables
```

### 10.2 2D DP

```java
// Longest Common Subsequence
int[][] dp = new int[m+1][n+1];
// dp[i][j] = LCS of first i chars of s1 and first j chars of s2
// Transition: O(1) per cell

States: (m+1) × (n+1)
Work per state: O(1)
Total time: O(m × n)
Space: O(m × n)  — can optimize to O(min(m,n)) with rolling array
```

### 10.3 Knapsack

```java
// 0/1 Knapsack
// dp[i][w] = max value using first i items with weight limit w
int[][] dp = new int[n+1][W+1];

States: n × W
Work per state: O(1)
Total time: O(n × W)
Space: O(n × W)  — or O(W) with 1D rolling

Note: this is pseudopolynomial — depends on W (the value), not just n (the input size)
If W = 2^n, this is exponential in terms of real input size (bit length)
```

### 10.4 DP on Intervals

```java
// Matrix chain multiplication, burst balloons, etc.
// dp[i][j] = answer for subproblem on interval [i, j]

States: O(n²)  — all pairs (i, j)
Work per state: O(n)  — try each split point k in [i, j]
Total time: O(n³)
Space: O(n²)
```

### 10.5 DP on Trees

```java
// dp[node] = some value computed from subtree
// Each node processed once, each edge traversed once
Time: O(n)   — same as DFS
Space: O(n)  — dp array + recursion stack O(h)
```

### 10.6 DP with Bitmask

```java
// dp[mask][i] = answer for subset represented by 'mask', ending at node i
// Traveling Salesman Problem (TSP)

States: 2^n × n
Work per state: O(n)  — try all possible previous nodes
Total time: O(n² × 2^n)
Space: O(n × 2^n)

This is exponential — only feasible for n ≤ 20 or so
```

### Common DP Space Optimizations

```
1D DP (Fibonacci, climbing stairs):
  Use 2 variables instead of array → O(1) space

2D DP (LCS, edit distance):
  Only need current row and previous row → O(min(m,n)) space
  (process shorter string as columns)

Knapsack:
  Process weights in reverse in 1D array → O(W) space
```

---

## 11. String Algorithms

### 11.1 Naive String Search

```
Time:  O(n × m)  — n = text length, m = pattern length
  Worst case: "aaaaaaaab" searching for "aaaab" — tries n-m+1 positions,
  each taking m comparisons

Space: O(1)
```

### 11.2 KMP (Knuth-Morris-Pratt)

```
Time:  O(n + m)
  — Build failure function: O(m)
  — Search with failure function: O(n) (pointer never goes back more than it advances)

Space: O(m)  — failure function array
```

### 11.3 Rabin-Karp (Rolling Hash)

```
Time:  O(n + m) average,  O(nm) worst (hash collisions)
Space: O(1)
```

### 11.4 Edit Distance (Levenshtein)

```
DP with dp[i][j] = edit distance between s1[0..i] and s2[0..j]
Time:  O(m × n)
Space: O(m × n)  — or O(min(m,n)) with rolling
```

### 11.5 Longest Common Subsequence (LCS)

```
Time:  O(m × n)
Space: O(m × n)  — or O(min(m,n))
```

### 11.6 Anagram Check

```
Time:  O(n) — count character frequencies
Space: O(1) — fixed alphabet size (26 for lowercase letters)
```

---

## 12. Common Mistakes & Traps

### Mistake 1 — Counting the wrong "n"

```java
// Problem: given a binary tree, find height
int height(TreeNode root) {
    if (root == null) return 0;
    return 1 + Math.max(height(root.left), height(root.right));
}

// WRONG: "this is O(2^n) because each call branches twice"
// RIGHT: n here is number of NODES. Each node visited once → O(n)
// The recursion tree has n leaves total (one per node), not 2^n
```

### Mistake 2 — String concatenation in a loop

```java
// WRONG: appears to be O(n) but is actually O(n²)
String s = "";
for (int i = 0; i < n; i++) s += chars[i];   // new String created each time

// RIGHT: use StringBuilder → O(n)
StringBuilder sb = new StringBuilder();
for (int i = 0; i < n; i++) sb.append(chars[i]);
String s = sb.toString();
```

### Mistake 3 — Forgetting recursion stack space

```java
// Space appears to be O(1) — no data structures allocated
void dfs(TreeNode root) {
    if (root == null) return;
    dfs(root.left);
    dfs(root.right);
}

// WRONG: O(1) space
// RIGHT: O(h) stack space, O(n) worst case (skewed tree)
```

### Mistake 4 — Assuming HashMap is always O(1)

```
HashMap average: O(1) per operation
HashMap worst case: O(n) per operation (all keys to same bucket)

In interviews:
  Say "O(1) average, O(n) worst case for HashMap operations"
  Java's HashMap converts to tree at 8 entries/bucket → O(log n) practical worst
```

### Mistake 5 — Double-counting loop iterations

```java
// This looks like O(n²) but is O(n)
int i = 0, j = 0;
while (i < n) {
    while (j < n && arr[j] < arr[i]) j++;
    i++;
}
// j only moves FORWARD — j goes from 0 to n at most once total
// Total iterations of inner while across all outer iterations: at most n
// Total: O(n), not O(n²)
// KEY: j never resets → amortized O(n)
```

### Mistake 6 — Misreading the recursion

```java
void f(int n) {
    if (n <= 0) return;
    f(n/2);    // NOT f(n-1)
    f(n/2);
}
// Recurrence: T(n) = 2T(n/2) + O(1)
// Master: a=2, b=2, d=0 → b^d = 1 < a=2 → Case 3 → O(n^(log₂ 2)) = O(n)
// NOT O(log n) just because we're doing n/2!
```

### Mistake 7 — Space of output not counted in auxiliary

```java
// This has O(n) time but what about space?
List<Integer> allNodes(TreeNode root) {
    List<Integer> result = new ArrayList<>();
    // DFS and add every node...
    return result;   // O(n) output
}
// Output space: O(n) — but this is REQUIRED to answer the question
// Auxiliary space: O(h) for recursion stack
// In interviews: distinguish between output space and auxiliary space
```

### Mistake 8 — O(log n) doesn't always mean halving

```java
// Digit counting — how many digits in n?
int digits = 0;
while (n > 0) { n /= 10; digits++; }
// Divides by 10 each time → log₁₀(n) iterations → O(log n) ✓

// But this is O(log n) in terms of the VALUE of n
// In terms of INPUT SIZE (bits): n has log₂(n) bits
// So this is actually O(bits in n) = O(input size) — essentially O(1) per bit
// Rarely matters but good to know for bit complexity discussions
```

### Mistake 9 — Confusing "worst case" inputs

```java
// Quick sort is O(n²) for sorted input (bad pivot choice)
// Binary search requires sorted input (pre-condition)
// HashMap degrades on adversarial hash inputs

Always specify: is your analysis average case or worst case?
```

### Mistake 10 — O(n log n) sort inside a loop

```java
for (int i = 0; i < n; i++) {
    Collections.sort(list);   // O(n log n) inside O(n) loop!
}
// Total: O(n) × O(n log n) = O(n² log n) NOT O(n log n)
```

---

## 13. Master Quick Reference

### Big-O Rules Summary

```
Sequential code:    add complexities      O(A) + O(B) = O(max(A,B))
Nested loops:       multiply              O(A) × O(B) = O(A×B)
Drop constants:     O(3n) = O(n)
Drop lower terms:   O(n² + n) = O(n²)
Log base doesn't matter: O(log₂n) = O(log₁₀n) = O(log n)
```

### Data Structures

```
Structure          Access   Search   Insert    Delete    Space
────────────────────────────────────────────────────────────────
Array              O(1)     O(n)     O(n)      O(n)      O(n)
Dynamic Array      O(1)     O(n)     O(1)*     O(n)      O(n)
Linked List        O(n)     O(n)     O(1)**    O(1)**    O(n)
Stack              O(n)     O(n)     O(1)      O(1)      O(n)
Queue              O(n)     O(n)     O(1)      O(1)      O(n)
HashMap            O(1)†    O(1)†    O(1)†     O(1)†     O(n)
TreeMap            O(log n) O(log n) O(log n)  O(log n)  O(n)
PriorityQueue      O(1)‡    O(n)     O(log n)  O(log n)  O(n)
BST (balanced)     O(log n) O(log n) O(log n)  O(log n)  O(n)
Trie               —        O(L)     O(L)      O(L)      O(ΣNSL)

* amortized   ** at head/tail with pointer   † average   ‡ peek only
L = word length
```

### Sorting

```
Algorithm   Best         Average      Worst        Space    Stable
──────────────────────────────────────────────────────────────────
Bubble      O(n)         O(n²)        O(n²)        O(1)     Yes
Selection   O(n²)        O(n²)        O(n²)        O(1)     No
Insertion   O(n)         O(n²)        O(n²)        O(1)     Yes
Merge       O(n log n)   O(n log n)   O(n log n)   O(n)     Yes
Quick       O(n log n)   O(n log n)   O(n²)        O(log n) No
Heap        O(n log n)   O(n log n)   O(n log n)   O(1)     No
Counting    O(n+k)       O(n+k)       O(n+k)       O(k)     Yes
Radix       O(dn)        O(dn)        O(dn)        O(n+b)   Yes
```

### Graph Algorithms

```
Algorithm          Time             Space     Notes
───────────────────────────────────────────────────────────────────
BFS                O(V + E)         O(V)      Shortest hops, unweighted
DFS                O(V + E)         O(V)      Traversal, cycles
Dijkstra's         O((V+E) log V)   O(V+E)    Shortest path, non-negative weights
Bellman-Ford       O(V × E)         O(V)      Shortest path, negative weights
Floyd-Warshall     O(V³)            O(V²)     All-pairs shortest path
Topological Sort   O(V + E)         O(V)      DAGs only
Kruskal's MST      O(E log E)       O(V+E)    Sparse graphs
Prim's MST         O(E log V)       O(V)      Dense graphs
Union-Find         O(α(n)) ≈ O(1)   O(V)      Dynamic connectivity
0-1 BFS            O(V + E)         O(V)      Weights ∈ {0, 1}
```

### Searching

```
Algorithm           Time        Space     Requirement
──────────────────────────────────────────────────────
Linear search       O(n)        O(1)      None
Binary search       O(log n)    O(1)      Sorted array
BST search          O(log n)    O(1)      Balanced BST
HashMap lookup      O(1) avg    O(1)      Hash function
Trie search         O(L)        O(1)      Trie built
```

### Recursion & DP

```
Pattern                    Time          Space
──────────────────────────────────────────────────────
Linear recursion (n→n-1)   O(n)          O(n) stack
Binary recursion (n→n/2)   O(log n)      O(log n) stack
Tree recursion (naive fib) O(2^n)        O(n) stack
Merge sort                 O(n log n)    O(n) heap + O(log n) stack
1D DP                      O(n)          O(n) → O(1) optimized
2D DP                      O(m×n)        O(m×n) → O(min(m,n)) optimized
Interval DP                O(n³)         O(n²)
Bitmask DP                 O(2^n × n²)   O(2^n × n)
```

### Master Theorem Quick Reference

```
T(n) = a·T(n/b) + O(n^d)

a < b^d  →  O(n^d)
a = b^d  →  O(n^d · log n)
a > b^d  →  O(n^(log_b a))

Common cases:
  T(n) = T(n/2) + O(1)     → O(log n)          [Binary Search]
  T(n) = 2T(n/2) + O(n)    → O(n log n)         [Merge Sort]
  T(n) = 2T(n/2) + O(1)    → O(n)               [Tree size]
  T(n) = T(n-1) + O(1)     → O(n)               [Linear recursion]
  T(n) = T(n-1) + O(n)     → O(n²)              [Insertion sort]
  T(n) = 2T(n-1) + O(1)    → O(2^n)             [Naive Fibonacci]
```

### Common Complexities in Plain English

```
O(1)       — Does not grow with input. Index access, hash lookup, push/pop.
O(log n)   — Halves the problem each step. Binary search, heap ops, BST ops.
O(√n)      — Loop up to square root. Primality test, some factorization.
O(n)       — One pass through input. Linear search, BFS, DFS, simple DP.
O(n log n) — Sort the input. Merge sort, heap sort, Dijkstra's.
O(n²)      — Nested loops over input. Bubble/insertion/selection sort, naive pairs.
O(n³)      — Three nested loops. Floyd-Warshall, matrix multiply, interval DP.
O(2^n)     — Try all subsets. Naive Fibonacci, power set, bitmask DP.
O(n!)      — Try all permutations. Brute-force TSP, generate all permutations.
```
