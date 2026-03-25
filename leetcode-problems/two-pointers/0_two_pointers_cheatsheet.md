# 🎯 Two Pointers — Complete Interview Cheat Sheet (Java)

> **One-stop reference. No need to Google. No confusion. Interview-ready.**

---

## 📌 Table of Contents

1. [What Is Two Pointers?](#1-what-is-two-pointers)
2. [Core Mental Model](#2-core-mental-model)
3. [When To Use (Decision Framework)](#3-when-to-use-decision-framework)
4. [All Pattern Types — Deep Dive](#4-all-pattern-types--deep-dive)
   - 4.1 Opposite Ends (Converging)
   - 4.2 Same Direction (Fast & Slow)
   - 4.3 Fixed + Moving (Read-Write / Anchor + Runner)
   - 4.4 Two Arrays / Two Lists
   - 4.5 Partition Pointer (Dutch National Flag)
   - 4.6 Linked List Specific Patterns
5. [Cue Words → Pattern Mapping](#5-cue-words--pattern-mapping)
6. [Complexity Reference](#6-complexity-reference)
7. [Common Mistakes & How to Avoid Them](#7-common-mistakes--how-to-avoid-them)
8. [Boundary Condition Checklist](#8-boundary-condition-checklist)
9. [Pattern-by-Pattern Code Templates (Java)](#9-pattern-by-pattern-code-templates-java)
10. [Classic Problems Mapped to Patterns](#10-classic-problems-mapped-to-patterns)
11. [Two Pointers vs Sliding Window — Clear Distinction](#11-two-pointers-vs-sliding-window--clear-distinction)
12. [Two Pointers vs Binary Search — When to Choose](#12-two-pointers-vs-binary-search--when-to-choose)
13. [Two Pointers vs HashMap — Trade-offs](#13-two-pointers-vs-hashmap--trade-offs)
14. [Interview Strategy & Communication Guide](#14-interview-strategy--communication-guide)
15. [Edge Cases to Always Test](#15-edge-cases-to-always-test)
16. [Quick-Fire Q&A (Likely Interview Questions)](#16-quick-fire-qa-likely-interview-questions)
17. [Cheat-Sheet Summary Table](#17-cheat-sheet-summary-table)

---

## 1. What Is Two Pointers?

Two Pointers is a **technique** — not a data structure — where you maintain **two index variables** (pointers) that traverse a sequence (array, string, linked list) in a coordinated way to reduce an O(n²) brute-force to **O(n)**.

**The key insight:**  
Instead of checking all pairs `(i, j)` with nested loops, use logical rules about your data (sorted order, window size, partition invariant) to **eliminate entire search spaces in one step**, so each pointer only moves forward (or backward) — never revisits.

```
Brute Force:   O(n²)  — nested loops, check every pair
Two Pointers:  O(n)   — each element visited at most once per pointer
```

---

## 2. Core Mental Model

Think of two pointers as **two workers** on a conveyor belt:

```
[ 1, 2, 3, 4, 5, 6, 7, 8 ]
  ↑                     ↑
 left                 right
```

- Each pointer starts at a **strategic position**
- They move based on a **condition** (sum too big? too small? window invalid?)
- They **never cross back** (that's the O(n) guarantee)
- They **stop** when they meet OR one exits the array

**The invariant** (what must always be true) is the heart of every two-pointer problem.  
Before you code, ask: *"What property must hold at every step?"*

---

## 3. When To Use (Decision Framework)

### ✅ Strong Signals — Almost Always Two Pointers

| Signal | Example Problem |
|--------|----------------|
| "Find a pair with target sum in **sorted** array" | Two Sum II |
| "Remove duplicates in-place" | Remove Duplicates |
| "Reverse array/string in-place" | Reverse String |
| "Check if palindrome" | Valid Palindrome |
| "Partition array around pivot" | Sort Colors |
| "Merge two sorted arrays/lists" | Merge Sorted Array |
| "Detect cycle in linked list" | Linked List Cycle |
| "Find middle of linked list" | Middle of Linked List |
| "Squaring a sorted array (need sorted result)" | Squares of Sorted Array |
| "Minimum window / longest substring with constraint" | (Sliding window variant) |
| "Triplets / Quadruplets summing to target" | 3Sum, 4Sum |

### ⚠️ Consider Two Pointers When

- Input is **sorted** (or can be sorted without ruining answer)
- Problem asks for **pairs, triplets, subarrays**
- Problem is about **in-place** modification
- You need O(1) extra space
- Brute force is O(n²) — try to reduce

### ❌ Two Pointers Likely Won't Work When

- Array is **unsorted** AND sorting changes the answer
- Need to find **all** combinations (→ backtracking)
- Need frequency counts (→ HashMap)
- Need range queries (→ Segment Tree, Prefix Sum)
- Need the **index** of original elements after sorting (use HashMap instead)

---

## 4. All Pattern Types — Deep Dive

---

### 4.1 Opposite Ends (Converging Pointers)

**Setup:** Left starts at index `0`, Right starts at index `n-1`. They move **toward each other**.

```
[ a, b, c, d, e, f ]
  L →              ← R
```

**Movement logic:**
- If current result is **too small** → move `L` right (increase value)
- If current result is **too large** → move `R` left (decrease value)
- If **found** → record answer, move both (or one, depending on problem)
- **Stop** when `L >= R`

**Why it works (the invariant):**  
In a sorted array, if `arr[L] + arr[R] < target`, every pair with `arr[L]` is too small (since `arr[R]` is the max), so we can safely skip `L` and move it right.

**Classic use cases:**
- Two Sum (sorted), 3Sum, 4Sum
- Container With Most Water
- Valid Palindrome
- Reverse Array/String
- Trapping Rain Water
- Squares of Sorted Array

**Template:**
```java
int[] converging(int[] arr, int target) {
    int L = 0, R = arr.length - 1;
    while (L < R) {
        int current = arr[L] + arr[R];
        if (current == target) {
            return new int[]{L, R};    // or record, then move both
        } else if (current < target) {
            L++;                        // need bigger sum
        } else {
            R--;                        // need smaller sum
        }
    }
    return new int[]{};
}
```

---

### 4.2 Same Direction (Fast & Slow Pointers)

**Setup:** Both start at the same position. One moves **faster** than the other.

```
[ a, b, c, d, e, f ]
  S  F
  ↓  ↓↓  (slow moves 1, fast moves 2)
```

**Sub-variants:**

#### 4.2a Floyd's Cycle Detection (Hare & Tortoise)
- Slow moves 1 step, Fast moves 2 steps
- If cycle exists → they **will meet inside the cycle**
- If no cycle → Fast hits `null`

#### 4.2b Middle of Linked List
- When Fast hits end → Slow is at middle
- **Odd length:** slow at exact middle
- **Even length:** slow at the **second** middle (e.g., `[1,2,3,4]` → slow = node 3)
- To get **first** middle in even length: use `fast.next != null && fast.next.next != null`

#### 4.2c Remove Nth Node From End
- Fast moves `n+1` steps first, then both move together
- When Fast hits `null`, Slow is at node **before** the target → `slow.next = slow.next.next`

---

### 4.3 Fixed + Moving (Read-Write / Anchor + Runner)

**Setup:** One pointer reads (`fast`), one pointer writes (`slow`). Used for in-place filtering/deduplication.

```
[ 1, 1, 2, 3, 3, 4 ]
  ↑  ↑
slow fast   (slow = write head, fast = read head)
```

**Logic:**
- `fast` scans every element
- `slow` only advances when a valid element is found
- `arr[0..slow]` is always the "cleaned" result

**Classic use cases:** Remove Duplicates, Move Zeroes, Remove Element, in-place filter

---

### 4.4 Two Arrays / Two Lists (Merge Pattern)

**Setup:** One pointer on each of two separate sequences. Advance whichever is smaller.

```
Array 1: [ 1, 3, 5, 7 ]   <- p1
Array 2: [ 2, 4, 6, 8 ]   <- p2
```

**Logic:**
- Compare `arr1[p1]` vs `arr2[p2]` → advance the smaller
- After one exhausts, append remaining of the other

**Classic use cases:** Merge Two Sorted Arrays, Intersection of Arrays, Compare Version Numbers

---

### 4.5 Partition Pointer (Dutch National Flag)

**Setup:** Three pointers divide array into regions with strict invariants.

```
|-- 0s --|  low  |-- 1s --|  mid  |-- unsorted --|  high  |-- 2s --|
```

- `low`: boundary of 0-region (exclusive)
- `mid`: current element being examined
- `high`: boundary of 2-region (exclusive)
- Region `[mid..high]` is unsorted

**Critical rule:** When swapping `nums[mid]` with `nums[high]`, do **NOT** advance `mid` — the swapped-in element is unchecked.

---

### 4.6 Linked List Specific Patterns

| Pattern | Pointer Setup | Use Case |
|---------|--------------|----------|
| Find middle | slow=1 step, fast=2 steps | Palindrome check, merge sort on LL |
| Detect cycle | slow=1, fast=2, check if equal | Cycle detection |
| Find cycle start | Phase 1: meet. Phase 2: reset slow to head | Floyd's algorithm |
| Nth from end | fast goes n+1 ahead, then both move | Remove Nth from end |
| Palindrome | Find middle → reverse second half → compare | Linked list palindrome |
| Intersection | Both pointers swap lists on exhaustion | Y-junction intersection |

---

## 5. Cue Words → Pattern Mapping

Use this as a **rapid lookup** during an interview.

```
"sorted array" + "pair/triplet" + "target sum"
→ CONVERGING POINTERS (Two Sum II, 3Sum)

"in-place" + "remove/filter/deduplicate"
→ READ-WRITE POINTERS (slow writes, fast reads)

"palindrome" (String)
→ CONVERGING from both ends, skip non-alphanumeric

"palindrome" (LinkedList)
→ FAST-SLOW (find middle) + REVERSE (second half) + CONVERGING

"cycle" / "loop" (LinkedList)
→ FLOYD'S FAST-SLOW

"middle of linked list"
→ FAST-SLOW (fast = 2x slow speed)

"merge two sorted"
→ TWO-ARRAY POINTERS

"sort 0s 1s 2s" / "partition around pivot" / "move negatives left"
→ DUTCH NATIONAL FLAG (3 pointers)

"container with most water" / "maximize area between bars"
→ CONVERGING (move shorter height pointer)

"squares of sorted array"
→ CONVERGING from both ends (largest squares at extremes)

"trap rainwater"
→ CONVERGING with running max from both sides

"remove nth node from end"
→ FAST-SLOW (fast goes n+1 ahead first)

"linked list intersection"
→ TWO-POINTER SWITCH (swap head on exhaustion)

"3Sum Closest"
→ CONVERGING + track minimum absolute difference
```

### Constraint Cues

| Constraint | Implication |
|-----------|-------------|
| "O(1) space" | Must be in-place → two pointers over HashMap |
| "O(n) time" | Single pass → two pointers |
| "No extra space" | Two pointers, not HashMap |
| "Sorted input" | Converging or read-write pointers |
| "Two sorted arrays" | Two-array merge pattern |
| "Return original indices" | Don't sort → use HashMap |

---

## 6. Complexity Reference

| Pattern | Time | Space | Notes |
|---------|------|-------|-------|
| Converging (Opposite Ends) | O(n) | O(1) | Single pass, no backtracking |
| Fast & Slow (Cycle) | O(n) | O(1) | Visits each node at most once |
| Read-Write (Filter) | O(n) | O(1) | Each element read exactly once |
| Two Arrays Merge | O(m+n) | O(1)–O(m+n) | Depends on in-place or not |
| Dutch Flag Partition | O(n) | O(1) | Each element processed once |
| 3Sum (sort + two-ptr) | O(n²) | O(1)* | Outer loop O(n) × inner O(n) |
| 4Sum (sort + two-ptr) | O(n³) | O(1)* | Two outer loops × inner O(n) |

*Output list space not counted

---

## 7. Common Mistakes & How to Avoid Them

### ❌ Mistake 1: Using Converging Pointers on Unsorted Array

The logic only works because sorting guarantees monotonic movement. Unsorted data has no such guarantee.

```java
// WRONG — array is unsorted
int[] arr = {4, 1, 3, 2};
int L = 0, R = arr.length - 1;
// Moving L or R has no predictable effect on sum!

// FIX: sort first (if original indices don't matter)
Arrays.sort(arr);
// Or use HashMap if original indices are needed
```

---

### ❌ Mistake 2: Forgetting Duplicate Skipping in 3Sum

```java
// WRONG — produces duplicate triplets
if (nums[i] + nums[L] + nums[R] == 0) {
    result.add(Arrays.asList(nums[i], nums[L], nums[R]));
    L++;
    R--;
    // [-1, 0, 1] may be added multiple times!
}

// CORRECT — skip duplicates after recording
if (nums[i] + nums[L] + nums[R] == 0) {
    result.add(Arrays.asList(nums[i], nums[L], nums[R]));
    while (L < R && nums[L] == nums[L + 1]) L++;   // skip left dups
    while (L < R && nums[R] == nums[R - 1]) R--;   // skip right dups
    L++;
    R--;
}

// ALSO skip duplicate outer pivots:
for (int i = 0; i < nums.length - 2; i++) {
    if (i > 0 && nums[i] == nums[i - 1]) continue;  // <- essential
    // ...
}
```

---

### ❌ Mistake 3: Infinite Loop — Not Advancing Both Pointers on Match

```java
// WRONG — only L moves on match; if new arr[L] == arr[R], loops forever
if (arr[L] + arr[R] == target) {
    result.add(...);
    L++;
}

// CORRECT
if (arr[L] + arr[R] == target) {
    result.add(...);
    L++;
    R--;   // always move both
}
```

---

### ❌ Mistake 4: Wrong Loop Condition for Middle of Even-Length List

```java
// Finds SECOND middle of even-length list (standard / LeetCode default)
while (fast != null && fast.next != null) {
    slow = slow.next;
    fast = fast.next.next;
}
// [1,2,3,4] -> slow lands on node 3

// To find FIRST middle of even-length list:
while (fast.next != null && fast.next.next != null) {
    slow = slow.next;
    fast = fast.next.next;
}
// [1,2,3,4] -> slow lands on node 2
```

Always clarify which middle is needed before writing the condition.

---

### ❌ Mistake 5: Off-By-One — `L < R` vs `L <= R`

```java
// For PAIRS (two distinct elements): always use L < R
while (L < R) { ... }

// L == R = same element touching itself
// Only use L <= R when single-element processing is intentional
// (e.g., Squares: L can equal R at the exact midpoint)
```

---

### ❌ Mistake 6: Dutch Flag — Advancing Mid After Swap With High

```java
// WRONG
if (nums[mid] == 2) {
    swap(nums, mid, high);
    high--;
    mid++;   // BUG! element swapped in from high is UNCHECKED
}

// CORRECT
if (nums[mid] == 2) {
    swap(nums, mid, high);
    high--;
    // do NOT increment mid
}
```

---

### ❌ Mistake 7: Integer Overflow in Sum (Java-Specific)

```java
// WRONG — may overflow when values are large ints
int sum = arr[L] + arr[R];

// CORRECT — cast to long before adding
long sum = (long) arr[L] + arr[R];
if (sum == target) { ... }
```

Think about overflow especially in 4Sum or when values approach `Integer.MAX_VALUE`.

---

### ❌ Mistake 8: Not Using a Dummy Node for Head Removal

```java
// WRONG — no dummy; head removal is a special case requiring extra logic
// CORRECT — dummy absorbs the edge case uniformly
ListNode dummy = new ListNode(0);
dummy.next = head;
ListNode fast = dummy, slow = dummy;
for (int i = 0; i <= n; i++) fast = fast.next;   // n+1 steps
while (fast != null) { fast = fast.next; slow = slow.next; }
slow.next = slow.next.next;
return dummy.next;   // NOT head — head may have been removed
```

---

### ❌ Mistake 9: Not Restoring Reversed Linked List After Palindrome Check

Always ask the interviewer: *"Do you need the original list structure preserved after the check?"* If yes, reverse the second half back before returning.

---

## 8. Boundary Condition Checklist

Before submitting, run through this mentally:

```
□  nums.length == 0           → return early / empty result
□  nums.length == 1           → single element — still works?
□  All same elements          → duplicates handled? (3Sum)
□  All negative               → works with negative numbers?
□  Target not achievable      → return empty / -1 / false?
□  L == R in loop             → condition L < R prevents processing single element
□  Integer overflow           → use long for sums of int arrays
□  Linked list: null head     → guard with if (head == null)
□  Linked list: 1 node        → fast.next == null at start?
□  Linked list: 2 nodes       → even-length middle check correct?
□  Cycle at head (pos=0)      → Floyd's still detects correctly
□  n == list length (remove)  → dummy node handles head removal correctly
```

---

## 9. Pattern-by-Pattern Code Templates (Java)

---

### Template 1: Converging — Two Sum II (Sorted)

```java
// LeetCode 167 — sorted array, 1-indexed output
public int[] twoSum(int[] numbers, int target) {
    int L = 0, R = numbers.length - 1;
    while (L < R) {
        int sum = numbers[L] + numbers[R];
        if (sum == target) {
            return new int[]{L + 1, R + 1};   // 1-indexed
        } else if (sum < target) {
            L++;    // sum too small -> move left pointer right
        } else {
            R--;    // sum too large -> move right pointer left
        }
    }
    return new int[]{-1, -1};   // guaranteed to have answer per problem
}
```

---

### Template 2: Converging — 3Sum

```java
// LeetCode 15
public List<List<Integer>> threeSum(int[] nums) {
    Arrays.sort(nums);                              // MUST sort first
    List<List<Integer>> result = new ArrayList<>();

    for (int i = 0; i < nums.length - 2; i++) {
        if (i > 0 && nums[i] == nums[i - 1]) continue;  // skip dup pivot

        int L = i + 1, R = nums.length - 1;
        while (L < R) {
            int sum = nums[i] + nums[L] + nums[R];
            if (sum == 0) {
                result.add(Arrays.asList(nums[i], nums[L], nums[R]));
                while (L < R && nums[L] == nums[L + 1]) L++;  // skip dups
                while (L < R && nums[R] == nums[R - 1]) R--;  // skip dups
                L++;
                R--;
            } else if (sum < 0) {
                L++;
            } else {
                R--;
            }
        }
    }
    return result;
}
```

---

### Template 3: Converging — 3Sum Closest

```java
// LeetCode 16 — no dup-skip needed; just track minimum absolute difference
public int threeSumClosest(int[] nums, int target) {
    Arrays.sort(nums);
    int closest = nums[0] + nums[1] + nums[2];

    for (int i = 0; i < nums.length - 2; i++) {
        int L = i + 1, R = nums.length - 1;
        while (L < R) {
            int sum = nums[i] + nums[L] + nums[R];
            if (Math.abs(sum - target) < Math.abs(closest - target)) {
                closest = sum;
            }
            if (sum < target) {
                L++;
            } else if (sum > target) {
                R--;
            } else {
                return sum;   // exact match — can't get closer
            }
        }
    }
    return closest;
}
```

---

### Template 4: Converging — 4Sum

```java
// LeetCode 18 — two outer loops + converging inner
public List<List<Integer>> fourSum(int[] nums, int target) {
    Arrays.sort(nums);
    List<List<Integer>> result = new ArrayList<>();
    int n = nums.length;

    for (int i = 0; i < n - 3; i++) {
        if (i > 0 && nums[i] == nums[i - 1]) continue;   // skip dup i

        for (int j = i + 1; j < n - 2; j++) {
            if (j > i + 1 && nums[j] == nums[j - 1]) continue;  // skip dup j

            int L = j + 1, R = n - 1;
            while (L < R) {
                long sum = (long) nums[i] + nums[j] + nums[L] + nums[R];
                if (sum == target) {
                    result.add(Arrays.asList(nums[i], nums[j], nums[L], nums[R]));
                    while (L < R && nums[L] == nums[L + 1]) L++;
                    while (L < R && nums[R] == nums[R - 1]) R--;
                    L++; R--;
                } else if (sum < target) {
                    L++;
                } else {
                    R--;
                }
            }
        }
    }
    return result;
}
```

---

### Template 5: Converging — Valid Palindrome (String)

```java
// LeetCode 125 — skip non-alphanumeric, case-insensitive compare
public boolean isPalindrome(String s) {
    int L = 0, R = s.length() - 1;
    while (L < R) {
        while (L < R && !Character.isLetterOrDigit(s.charAt(L))) L++;
        while (L < R && !Character.isLetterOrDigit(s.charAt(R))) R--;
        if (Character.toLowerCase(s.charAt(L)) !=
            Character.toLowerCase(s.charAt(R))) {
            return false;
        }
        L++;
        R--;
    }
    return true;
}
```

---

### Template 6: Converging — Container With Most Water

```java
// LeetCode 11 — always move the shorter wall (greedy proof)
public int maxArea(int[] height) {
    int L = 0, R = height.length - 1;
    int maxWater = 0;
    while (L < R) {
        int water = Math.min(height[L], height[R]) * (R - L);
        maxWater = Math.max(maxWater, water);
        if (height[L] < height[R]) {
            L++;   // move shorter wall — only hope to improve height
        } else {
            R--;
        }
    }
    return maxWater;
}
```

---

### Template 7: Converging — Squares of Sorted Array

```java
// LeetCode 977 — largest squares always at one of the two ends
public int[] sortedSquares(int[] nums) {
    int n = nums.length;
    int[] result = new int[n];
    int L = 0, R = n - 1;
    int pos = n - 1;                    // fill result from the end

    while (L <= R) {
        int sqL = nums[L] * nums[L];
        int sqR = nums[R] * nums[R];
        if (sqL > sqR) {
            result[pos--] = sqL;
            L++;
        } else {
            result[pos--] = sqR;
            R--;
        }
    }
    return result;
}
```

---

### Template 8: Converging — Trapping Rain Water

```java
// LeetCode 42 — track running max from both sides
public int trap(int[] height) {
    int L = 0, R = height.length - 1;
    int leftMax = 0, rightMax = 0, water = 0;

    while (L < R) {
        if (height[L] < height[R]) {
            if (height[L] >= leftMax) {
                leftMax = height[L];           // update running left max
            } else {
                water += leftMax - height[L];  // water above this bar
            }
            L++;
        } else {
            if (height[R] >= rightMax) {
                rightMax = height[R];
            } else {
                water += rightMax - height[R];
            }
            R--;
        }
    }
    return water;
}
```

---

### Template 9: Converging — Reverse Array / String

```java
// Reverse int array in-place
public void reverseArray(int[] arr) {
    int L = 0, R = arr.length - 1;
    while (L < R) {
        int tmp = arr[L];
        arr[L++] = arr[R];
        arr[R--] = tmp;
    }
}

// Reverse characters (via char array — strings are immutable in Java)
public void reverseString(char[] s) {
    int L = 0, R = s.length - 1;       // LeetCode 344 signature
    while (L < R) {
        char tmp = s[L];
        s[L++] = s[R];
        s[R--] = tmp;
    }
}
```

---

### Template 10: Fast-Slow — Cycle Detection (Floyd's)

```java
// LeetCode 141
public boolean hasCycle(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next;           // 1 step
        fast = fast.next.next;      // 2 steps
        if (slow == fast) return true;   // they meet -> cycle exists
    }
    return false;
}
```

---

### Template 11: Fast-Slow — Find Cycle Start (Floyd's Phase 2)

```java
// LeetCode 142 — return the node where cycle begins
public ListNode detectCycle(ListNode head) {
    ListNode slow = head, fast = head;

    // Phase 1: find meeting point
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
        if (slow == fast) {
            // Phase 2: reset slow to head, move both 1 step at a time
            slow = head;
            while (slow != fast) {
                slow = slow.next;
                fast = fast.next;
            }
            return slow;   // cycle start node
        }
    }
    return null;   // no cycle
}
// WHY: dist(head->start) == dist(meetPoint->start) [mod cycle length]
```

---

### Template 12: Fast-Slow — Middle of Linked List

```java
// LeetCode 876
public ListNode middleNode(ListNode head) {
    ListNode slow = head, fast = head;
    // Finds SECOND middle for even-length lists
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
    }
    return slow;
}

// To get FIRST middle of even-length list:
public ListNode firstMiddle(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast.next != null && fast.next.next != null) {
        slow = slow.next;
        fast = fast.next.next;
    }
    return slow;
}
```

---

### Template 13: Fast-Slow — Remove Nth Node From End

```java
// LeetCode 19 — dummy node absorbs head-removal edge case cleanly
public ListNode removeNthFromEnd(ListNode head, int n) {
    ListNode dummy = new ListNode(0);
    dummy.next = head;
    ListNode fast = dummy, slow = dummy;

    for (int i = 0; i <= n; i++) {     // fast goes n+1 steps ahead
        fast = fast.next;
    }
    while (fast != null) {             // slide both until fast exits
        fast = fast.next;
        slow = slow.next;
    }
    slow.next = slow.next.next;        // unlink target node
    return dummy.next;                 // NOT head — head may have changed
}
```

---

### Template 14: Read-Write — Remove Duplicates from Sorted Array

```java
// LeetCode 26 — slow = index of last confirmed unique element
public int removeDuplicates(int[] nums) {
    if (nums.length == 0) return 0;
    int slow = 0;                          // write pointer
    for (int fast = 1; fast < nums.length; fast++) {
        if (nums[fast] != nums[slow]) {    // new unique element found
            slow++;
            nums[slow] = nums[fast];       // write to next position
        }
    }
    return slow + 1;                       // new length
}

// Variant: Allow at most 2 duplicates (LeetCode 80)
public int removeDuplicatesII(int[] nums) {
    int slow = 0;
    for (int fast = 0; fast < nums.length; fast++) {
        // Keep if we have fewer than 2, or current != the one 2 slots back
        if (slow < 2 || nums[fast] != nums[slow - 2]) {
            nums[slow++] = nums[fast];
        }
    }
    return slow;
}
```

---

### Template 15: Read-Write — Move Zeroes

```java
// LeetCode 283 — maintain relative order of non-zero elements
public void moveZeroes(int[] nums) {
    int slow = 0;                                    // next write position
    for (int fast = 0; fast < nums.length; fast++) {
        if (nums[fast] != 0) {
            nums[slow] = nums[fast];
            if (slow != fast) nums[fast] = 0;        // clear original if shifted
            slow++;
        }
    }
}
```

---

### Template 16: Read-Write — Remove Element

```java
// LeetCode 27
public int removeElement(int[] nums, int val) {
    int slow = 0;
    for (int fast = 0; fast < nums.length; fast++) {
        if (nums[fast] != val) {
            nums[slow++] = nums[fast];
        }
    }
    return slow;   // new length
}
```

---

### Template 17: Dutch National Flag — Sort Colors

```java
// LeetCode 75 — sort 0s, 1s, 2s in a single pass
public void sortColors(int[] nums) {
    int low = 0, mid = 0, high = nums.length - 1;

    while (mid <= high) {
        if (nums[mid] == 0) {
            swap(nums, low++, mid++);   // 0 belongs at left; both pointers advance
        } else if (nums[mid] == 1) {
            mid++;                      // 1 is in correct middle region
        } else {                        // nums[mid] == 2
            swap(nums, mid, high--);    // 2 belongs at right
            // !! DO NOT mid++ — swapped element from high is UNCHECKED
        }
    }
}

private void swap(int[] nums, int i, int j) {
    int tmp = nums[i];
    nums[i] = nums[j];
    nums[j] = tmp;
}
```

---

### Template 18: Two-Array Merge — Merge Sorted Arrays (In-Place from End)

```java
// LeetCode 88 — merge nums2 into nums1 from the END to avoid overwriting
public void merge(int[] nums1, int m, int[] nums2, int n) {
    int p1 = m - 1, p2 = n - 1, write = m + n - 1;

    while (p1 >= 0 && p2 >= 0) {
        if (nums1[p1] >= nums2[p2]) {
            nums1[write--] = nums1[p1--];
        } else {
            nums1[write--] = nums2[p2--];
        }
    }
    // If nums2 still has elements, copy them
    // (If nums1 still has elements, they're already in place)
    while (p2 >= 0) {
        nums1[write--] = nums2[p2--];
    }
}
```

---

### Template 19: Two-Array — Intersection of Two Arrays

```java
// LeetCode 349 — sort both, then merge-scan
public int[] intersection(int[] nums1, int[] nums2) {
    Arrays.sort(nums1);
    Arrays.sort(nums2);
    List<Integer> result = new ArrayList<>();
    int p1 = 0, p2 = 0;

    while (p1 < nums1.length && p2 < nums2.length) {
        if (nums1[p1] == nums2[p2]) {
            // Add only if not already added (dedup in output)
            if (result.isEmpty() || result.get(result.size() - 1) != nums1[p1]) {
                result.add(nums1[p1]);
            }
            p1++;
            p2++;
        } else if (nums1[p1] < nums2[p2]) {
            p1++;
        } else {
            p2++;
        }
    }
    return result.stream().mapToInt(Integer::intValue).toArray();
}
```

---

### Template 20: Linked List Intersection (Y-Junction)

```java
// LeetCode 160 — both pointers travel lenA + lenB total steps
public ListNode getIntersectionNode(ListNode headA, ListNode headB) {
    ListNode a = headA, b = headB;
    while (a != b) {
        a = (a == null) ? headB : a.next;   // switch to B when A exhausted
        b = (b == null) ? headA : b.next;   // switch to A when B exhausted
    }
    return a;   // intersection node, or null if lists don't intersect
    // WHY: both travel lenA + lenB steps; they align at intersection
}
```

---

### Template 21: Linked List Palindrome

```java
// LeetCode 234 — find middle, reverse second half, compare
public boolean isPalindrome(ListNode head) {
    // Step 1: find middle using fast-slow
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
    }

    // Step 2: reverse second half in-place
    ListNode prev = null, curr = slow;
    while (curr != null) {
        ListNode next = curr.next;
        curr.next = prev;
        prev = curr;
        curr = next;
    }

    // Step 3: compare left half and reversed right half
    ListNode left = head, right = prev;
    while (right != null) {             // right is shorter or equal
        if (left.val != right.val) return false;
        left = left.next;
        right = right.next;
    }
    return true;
    // Optional Step 4: restore reversed half if needed (ask interviewer)
}
```

---

### Template 22: Two Pointers vs HashMap — Two Sum Decision

```java
// CASE 1: Unsorted array + need original indices → HashMap
public int[] twoSumUnsorted(int[] nums, int target) {
    Map<Integer, Integer> seen = new HashMap<>();
    for (int i = 0; i < nums.length; i++) {
        int complement = target - nums[i];
        if (seen.containsKey(complement)) {
            return new int[]{seen.get(complement), i};
        }
        seen.put(nums[i], i);
    }
    return new int[]{};
}

// CASE 2: Sorted array + O(1) space required → Two Pointers
public int[] twoSumSorted(int[] numbers, int target) {
    int L = 0, R = numbers.length - 1;
    while (L < R) {
        int sum = numbers[L] + numbers[R];
        if (sum == target) return new int[]{L + 1, R + 1};   // 1-indexed
        else if (sum < target) L++;
        else R--;
    }
    return new int[]{};
}
```

---

## 10. Classic Problems Mapped to Patterns

| Problem (LeetCode #) | Pattern | Key Insight |
|----------------------|---------|-------------|
| Two Sum II (167) | Converging | Move L if sum < target |
| 3Sum (15) | Converging + outer loop | Sort, fix i, two-ptr rest |
| 3Sum Closest (16) | Converging + track min diff | No dup-skip needed |
| 4Sum (18) | Converging + 2 outer loops | Two fixed + two-ptr; use long |
| Container With Most Water (11) | Converging | Move shorter wall |
| Trapping Rain Water (42) | Converging | leftMax, rightMax running totals |
| Valid Palindrome (125) | Converging | Skip non-alnum chars |
| Squares of Sorted Array (977) | Converging (fill from end) | Extremes hold largest squares |
| Reverse String (344) | Converging | Simple swap |
| Sort Colors (75) | Dutch National Flag | 3 regions, 3 pointers |
| Remove Duplicates (26) | Read-Write | slow=write head |
| Remove Duplicates II (80) | Read-Write | Compare with slow-2 |
| Remove Element (27) | Read-Write | Skip target value |
| Move Zeroes (283) | Read-Write | Swap non-zero forward |
| Linked List Cycle (141) | Fast-Slow | Meet → cycle exists |
| Linked List Cycle II (142) | Fast-Slow (2 phases) | Reset slow after meet |
| Middle of Linked List (876) | Fast-Slow | fast = 2× slow |
| Remove Nth From End (19) | Fast-Slow + dummy node | fast n+1 ahead |
| Merge Sorted Array (88) | Two-array (from end) | Fill from back; avoid overwrite |
| Intersection of Arrays (349) | Two-array | Advance smaller pointer |
| Linked List Intersection (160) | Pointer switch | Swap head on exhaustion |
| Palindrome Linked List (234) | Fast-Slow + reverse | Middle → compare halves |
| Minimum Size Subarray (209) | Sliding Window (close cousin) | Shrink when valid |
| Longest No-Repeat Substring (3) | Sliding Window | Expand R, shrink L on conflict |

---

## 11. Two Pointers vs Sliding Window — Clear Distinction

> **The most common interview confusion.** Settled here once and for all.

### They Are Related But Different

Both use two index variables (`L` and `R`). The difference is **what triggers movement** and **what you're optimizing**.

| Aspect | Two Pointers | Sliding Window |
|--------|-------------|----------------|
| **Core goal** | Find pair/partition satisfying condition | Find subarray optimizing a property |
| **Pointer directions** | Converging (toward each other) OR same-direction | Always same direction (both rightward) |
| **Window aggregate** | Sum of two elements, comparison | Running sum, frequency map, distinct count |
| **Extra state needed** | None — just indices | Yes — Map, count, or running sum |
| **Typical output** | Pair indices, boolean | Length, count, substring |
| **Shrinking** | L moves right OR R moves left | Only L shrinks; R never goes left |

### Decision Rule

```
Both pointers ONLY move rightward
AND you are tracking window state (sum, freq map, distinct count)?
→ SLIDING WINDOW

Pointers move toward each other,
OR problem is about pairs/triplets/partitioning/deduplication?
→ TWO POINTERS
```

### Concrete Examples

```
"Find pair with sum = target in sorted array"
→ TWO POINTERS (one moves left, one moves right)

"Minimum subarray with sum >= target"
→ SLIDING WINDOW (R expands, L shrinks when sum valid, track min length)

"Longest substring with at most k distinct characters"
→ SLIDING WINDOW (HashMap in window, shrink L on violation)

"Remove duplicates in sorted array"
→ TWO POINTERS read-write (not a window problem at all)

"3Sum"
→ TWO POINTERS (outer loop + converging inner — not a window)
```

---

## 12. Two Pointers vs Binary Search — When to Choose

| Scenario | Choose |
|----------|--------|
| Find **pair** summing to target in sorted array | Two Pointers — O(n) |
| Find **single value** or its position in sorted array | Binary Search — O(log n) |
| Check if a **value exists** | Binary Search |
| Find **optimal pair** with some combined property | Two Pointers |
| **Count** pairs satisfying a condition | Two Pointers (accumulate R - L per step) |
| Minimize/maximize a threshold value | Binary Search on Answer |

**Combined use:** 3Sum uses sort + two pointers (not sort + binary search) because binary search would cost O(log n) per inner step → O(n log n) inner total, vs O(n) for two pointers.

---

## 13. Two Pointers vs HashMap — Trade-offs

| | Two Pointers | HashMap |
|--|-------------|---------|
| **Space** | O(1) ✅ | O(n) ❌ |
| **Requires sorting** | Often yes — O(n log n) | No |
| **Unsorted input** | Sort first | Direct O(n) |
| **Returns original indices** | ❌ Sorting destroys them | ✅ Store value → index |
| **Negative numbers** | Works after sort | Works directly |
| **Multiple queries on same array** | Re-scan per query | O(1) per lookup after O(n) build |

**Rule of thumb:**
- Problem says "sorted" → Two Pointers
- Problem says "return indices" + unsorted → HashMap
- Problem says "O(1) space" → Two Pointers (sort if needed)

---

## 14. Interview Strategy & Communication Guide

### Step-by-Step Approach

**1. Read → Identify signals (30 seconds)**
```
Sorted? Pair/Triplet? In-place? Linked list + cycle/middle?
→ Say: "The array is sorted and we need pairs — two pointers is the right approach."
```

**2. Name the pattern**
```
"I'll use converging pointers — L at 0, R at end.
 I'll move L if sum is too small, R if sum is too large."
```

**3. State the invariant**
```
"At every step: all elements left of L produce sums too small to be useful;
 all elements right of R produce sums too large. So we never miss a valid pair."
```

**4. Write the loop condition first, then the body**
```java
while (L < R) {   // <- write this first and explain termination
    // then fill in the body
}
```

**5. Dry-run 2–3 iterations on the given example**

**6. Check edge cases aloud** (empty, single element, no solution, all duplicates)

---

### Phrases to Use in Interviews

| Situation | Say This |
|-----------|----------|
| Pattern recognition | "The sorted input is the key signal — converging two pointers give O(n) instead of O(n²)." |
| Movement logic | "Moving L right is the only way to increase the sum — so that's the only useful move when sum < target." |
| Invariant | "We never miss a valid pair because we only discard a pointer when we've proven no valid pair can include it." |
| Duplicates | "After recording a match, I'll skip duplicates on both sides to prevent duplicate triplets in the output." |
| Complexity | "Each pointer moves at most n steps total — O(n) time, O(1) space." |
| Linked list cycle | "Slow moves one step, fast moves two. If there's a cycle they must meet; if not, fast exits. That's Floyd's algorithm." |

---

### What NOT to Say / Avoid

```
❌ "I think we can use two pointers here... maybe?"
✅ "The sorted input is the key signal — two pointers is the right approach."

❌ Jumping straight to code without explaining the movement logic
✅ One sentence on when each pointer moves, then code

❌ Forgetting duplicate handling in 3Sum (caught during testing)
✅ Proactively: "I need to skip duplicates here, otherwise I'll get repeated triplets."

❌ Not stating complexity at the end
✅ "3Sum is O(n²) — O(n log n) to sort, O(n²) for the two-pointer loops."
```

---

## 15. Edge Cases to Always Test

### Arrays
```
{}                         -> empty array -> return early
{x}                        -> single element
{x, x}                     -> two identical elements
{x, x, x, ..., x}          -> all same -> dup skipping correct?
{-3, -2, 0, 1, 2}          -> negative numbers + zero
{1, 2}, target = 100       -> no valid pair -> return empty/false
{Integer.MIN_VALUE, ...}   -> overflow risk -> use long
```

### Linked Lists
```
null                       -> empty list -> guard at top
[1]                        -> single node -> fast.next == null
[1, 2]                     -> two nodes -> even-length middle edge case
[1, 1]                     -> same value nodes
cycle back to head         -> cycle at position 0 (Floyd's handles it)
cycle at last node only    -> smallest possible cycle
```

### 3Sum Specific
```
[-1, 0, 1, 2, -1, -4]      -> has duplicates
[0, 0, 0]                  -> one valid triplet [0,0,0]
[0, 0, 0, 0]               -> still only one unique triplet [0,0,0]
all positives               -> no triplet sums to 0 -> empty result
```

---

## 16. Quick-Fire Q&A (Likely Interview Questions)

**Q: Why does two pointers work on sorted arrays but not unsorted?**
> Sorted arrays guarantee **monotonic movement** — moving L right always increases the sum, moving R left always decreases it. This lets us eliminate entire search spaces in one step. Unsorted arrays have no such guarantee; moving a pointer could change the sum in any direction.

**Q: When should you sort first vs using a HashMap?**
> Sort first when original indices don't matter and O(n log n) time is acceptable. Use HashMap when the problem requires original indices, or when sorting would change the answer. Key rule: "return indices" + unsorted → HashMap.

**Q: What is the time complexity of 3Sum?**
> O(n²). Sorting is O(n log n). The outer loop is O(n). For each iteration, the two-pointer scan is O(n). Total: O(n log n + n²) = O(n²).

**Q: Why is Floyd's cycle detection correct?**
> Let F = distance from head to cycle start, C = cycle length, k = offset within cycle at meeting point. When they meet: slow traveled F + k, fast traveled F + k + mC. Since fast = 2×slow: F + k = mC → F = mC − k. So the distance from head to cycle start equals the distance from meeting point to cycle start (mod C). Resetting slow to head and moving both one step brings them to the cycle start.

**Q: Can two pointers find all valid pairs, not just one?**
> Yes. Instead of returning on first match, record it and continue (move both pointers). Use duplicate skipping if only unique pairs are required.

**Q: What if I need the count of pairs with sum less than target?**
> With a sorted array: when `arr[L] + arr[R] < target`, ALL pairs `(L, L+1), ..., (L, R)` satisfy the condition — that's `R - L` pairs. Add `R - L` to the count, then `L++`. Total time stays O(n).

**Q: 3Sum vs 3Sum Closest — what changes?**
> Replace the `== 0` check with tracking minimum absolute difference: `if (Math.abs(sum - target) < Math.abs(closest - target)) closest = sum`. Move L/R based on whether sum < target or > target. No duplicate skipping needed — you're tracking one minimum, not building a list.

**Q: Why move the shorter wall in Container With Most Water?**
> Area = `min(height[L], height[R]) * (R - L)`. If you move the taller wall, width decreases and height is still bounded by the shorter wall — area can only shrink. Moving the shorter wall is the only way to possibly find a taller wall, which could increase area.

**Q: Will fast and slow always meet inside a cycle?**
> Yes. Once both are inside the cycle, the gap between them decreases by exactly 1 each step (fast gains 1 on slow per iteration). Since the cycle is finite, the gap eventually hits 0 — they meet.

**Q: What does `slow` represent in Remove Duplicates?**
> `slow` is the index of the last confirmed unique element. `slow + 1` is the next write position. The subarray `nums[0..slow]` (inclusive) is always the deduplicated result. Return value is `slow + 1`.

**Q: Why use a dummy node in linked list problems?**
> A dummy node placed before `head` lets you handle the case where `head` itself is the node to remove — without a special if-check. With dummy: `slow.next = slow.next.next` works uniformly for all nodes, and `dummy.next` is always the correct new head.

**Q: Integer overflow — when does it matter in Java?**
> Whenever you add two `int` values that could together exceed `Integer.MAX_VALUE` (2,147,483,647). Typical cases: 4Sum with large values, checking sum against `Integer.MIN_VALUE`. Fix: `long sum = (long) nums[i] + nums[j] + nums[L] + nums[R];`

---

## 17. Cheat-Sheet Summary Table

| Pattern | Pointers Start | Movement Rule | Stop When | Java Loop Condition |
|---------|---------------|--------------|-----------|-------------------|
| **Converging** | `L=0, R=n-1` | Move based on comparison vs target | `L >= R` | `while (L < R)` |
| **Fast-Slow (cycle)** | Both at `head` | slow+1, fast+2 | fast or fast.next is null | `while (fast != null && fast.next != null)` |
| **Fast-Slow (middle)** | Both at `head` | slow+1, fast+2 | same as above | `while (fast != null && fast.next != null)` |
| **Read-Write** | `slow=0, fast=1` | fast always++, slow++ only if valid | fast exits array | `for (int fast = 1; fast < n; fast++)` |
| **Dutch Flag** | `low=mid=0, high=n-1` | Swap based on nums[mid] | `mid > high` | `while (mid <= high)` |
| **Two-Array Merge** | `p1=0, p2=0` | Advance smaller | both exhaust | `while (p1 < m && p2 < n)` |
| **LL Intersection** | Both at `headA`, `headB` | Swap list on null | `a == b` | `while (a != b)` |

---

## 🧠 Final Mental Checklist (Before You Write Code in Java)

```
1.  Is the array sorted? If not, can I sort without losing the answer?
2.  Which pattern fits?
      pairs/triplets + sorted       -> CONVERGING
      filter/deduplicate in-place   -> READ-WRITE
      cycle/middle in linked list   -> FAST-SLOW
      partition 0s/1s/2s            -> DUTCH FLAG
      merge two sorted sequences    -> TWO-ARRAY
3.  Write the loop condition first — and explain when it terminates.
4.  Comment what each pointer represents (write position, read position, etc.)
5.  Movement rule: when does L move? when does R move? (the invariant)
6.  Handle duplicates? (3Sum: skip after match AND at outer loop)
7.  Overflow risk? -> cast to long before arithmetic
8.  Dummy node needed? (linked list head-removal cases)
9.  State complexity: Time O(?) / Space O(?)
10. Test edge cases: empty, single element, all-same, no-solution, negatives
```

---

*Built for interview revision. All code in Java. Covers every two-pointer pattern used in FAANG/MAANG interviews.*
