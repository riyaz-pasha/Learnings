# ➕ Prefix Sum — Complete Interview Cheat Sheet
> **One document. Zero confusion. Interview-ready.**

---

## 📋 Table of Contents
1. [What Is Prefix Sum?](#1-what-is-prefix-sum)
2. [The Core Intuition](#2-the-core-intuition)
3. [Recognition Cues — When to Use It](#3-recognition-cues--when-to-use-it)
4. [Building the Prefix Sum Array](#4-building-the-prefix-sum-array)
5. [The Golden Formula](#5-the-golden-formula)
6. [Types of Prefix Sum Problems](#6-types-of-prefix-sum-problems)
7. [Master Templates (Java)](#7-master-templates-java)
8. [All Patterns with Problems & Code](#8-all-patterns-with-problems--code)
9. [2D Prefix Sum (Matrix)](#9-2d-prefix-sum-matrix)
10. [Prefix Sum + HashMap (The Power Combo)](#10-prefix-sum--hashmap-the-power-combo)
11. [Prefix XOR / Prefix Product](#11-prefix-xor--prefix-product)
12. [Difference Array (Inverse of Prefix Sum)](#12-difference-array-inverse-of-prefix-sum)
13. [Complexity Cheat Sheet](#13-complexity-cheat-sheet)
14. [Common Mistakes & How to Avoid Them](#14-common-mistakes--how-to-avoid-them)
15. [Decision Flowchart](#15-decision-flowchart)
16. [Edge Cases Checklist](#16-edge-cases-checklist)
17. [Interview Communication Guide](#17-interview-communication-guide)
18. [Quick-Fire Problem to Pattern Mapping](#18-quick-fire-problem-to-pattern-mapping)

---

## 1. What Is Prefix Sum?

Prefix Sum is a **preprocessing technique** that converts an O(n) range query into O(1) by precomputing cumulative sums. You trade **O(n) space and O(n) build time** for **O(1) query time**.

```
Array:       [3,  1,  4,  1,  5,  9,  2,  6]
Index:        0   1   2   3   4   5   6   7

Prefix Sum:  [0,  3,  4,  8,  9, 14, 23, 25, 31]
Index:        0   1   2   3   4   5   6   7   8
              ^
          prefix[0] = 0  (sentinel -- makes formula clean)
```

> `prefix[i]` = sum of all elements from index `0` to `i-1` (with 1-indexed prefix)

---

## 2. The Core Intuition

### Brute Force vs Prefix Sum

```
Problem: Sum of subarray from index L to R (multiple queries)

BRUTE FORCE -- O(n) per query, O(n*q) total:
for each query (L, R):
    sum = 0
    for i in L..R:
        sum += arr[i]          <- recomputes every time

PREFIX SUM -- O(n) build, O(1) per query:
Build once:  prefix[i+1] = prefix[i] + arr[i]
Query:       sum(L, R) = prefix[R+1] - prefix[L]
```

### Why the Subtraction Works

```
prefix[R+1] = arr[0] + arr[1] + ... + arr[L-1] + arr[L] + ... + arr[R]
prefix[L]   = arr[0] + arr[1] + ... + arr[L-1]
              -----------------------------------------------------------
Difference  =                                   arr[L] + ... + arr[R]
                                                = sum(L, R) correct!
```

### Visual

```
Array:   [ 3,  1,  4,  1,  5,  9]
Prefix:  [ 0,  3,  4,  8,  9, 14, 23]

Query: sum(2, 4) = prefix[5] - prefix[2] = 14 - 4 = 10
                   arr[2]+arr[3]+arr[4]  = 4+1+5  = 10 correct
```

---

## 3. Recognition Cues — When to Use It

### Strong Signals (Use Prefix Sum)

| Signal in Problem Statement | Example Problem |
|---|---|
| "sum of subarray from i to j" | Range Sum Query |
| "multiple range queries" | "answer q queries on array" |
| "subarray sum equals k" | LC 560 |
| "number of subarrays with sum..." | count-based problems |
| "sum divisible by k" | LC 974 |
| "balance point / pivot index" | LC 724 |
| "count of subarrays with even/odd sum" | parity problems |
| "prefix" literally in the problem | almost always |
| Range update + range query | Difference Array |
| 2D matrix region sum | 2D Prefix Sum |
| XOR of range | Prefix XOR |

### Signals It's NOT Prefix Sum

| Signal | Use Instead |
|---|---|
| Non-contiguous elements | DP / Greedy |
| Need actual elements, not sum | Sliding Window / Two Pointers |
| Online updates to array | Segment Tree / BIT (Fenwick Tree) |
| k-th smallest/largest | Heap / QuickSelect |
| Substrings matching pattern | KMP / Rabin-Karp |
| Sum of non-overlapping subarrays | DP |

### Tricky Cases

- **Prefix sum + HashMap** -- when you need count of subarrays with a specific sum/property
- **Prefix sum with modulo** -- when divisibility is the condition
- **Prefix XOR** -- bitwise range queries (swap + for ^)
- **Prefix product** -- product queries, but watch for zeros
- **Difference array** -- when you have range updates (add x to [L, R]) instead of queries

---

## 4. Building the Prefix Sum Array

### 1-Indexed Prefix (Recommended -- Cleaner Formula)

```java
int[] arr = {3, 1, 4, 1, 5, 9, 2, 6};
int n = arr.length;
int[] prefix = new int[n + 1];   // size n+1, prefix[0] = 0 (sentinel)

for (int i = 0; i < n; i++) {
    prefix[i + 1] = prefix[i] + arr[i];
}
// prefix = [0, 3, 4, 8, 9, 14, 23, 25, 31]
// Query sum(L, R) = prefix[R+1] - prefix[L]   (0-indexed L, R)
```

### 0-Indexed Prefix (Common but error-prone)

```java
int[] prefix = new int[n];
prefix[0] = arr[0];
for (int i = 1; i < n; i++) {
    prefix[i] = prefix[i - 1] + arr[i];
}
// prefix = [3, 4, 8, 9, 14, 23, 25, 31]
// Query sum(L, R) = prefix[R] - (L > 0 ? prefix[L-1] : 0)
//                               ^ extra check needed -- error-prone!
```

> Always prefer 1-indexed prefix (size n+1 with prefix[0]=0). It eliminates the L > 0 edge case check and makes all formulas clean.

---

## 5. The Golden Formula

```
Given: 0-indexed array arr[], 1-indexed prefix[] where prefix[0] = 0

Sum of arr[L..R] (inclusive, 0-indexed) = prefix[R+1] - prefix[L]

Equivalently:
  prefix[i] = arr[0] + arr[1] + ... + arr[i-1]
  prefix[i] = sum of first i elements
```

### Formula Table -- Never Mix These Up

| prefix[] style | Formula for sum(L, R) | Notes |
|---|---|---|
| 1-indexed (size n+1) | `prefix[R+1] - prefix[L]` | Recommended |
| 0-indexed (size n) | `prefix[R] - (L>0 ? prefix[L-1] : 0)` | Needs guard |
| In-place on arr | Same as 0-indexed | Modifies original |

### Checking Your Formula
```
Always verify with a small example:
arr = [2, 3, 1], prefix = [0, 2, 5, 6]
sum(0, 2) = prefix[3] - prefix[0] = 6 - 0 = 6  correct (2+3+1=6)
sum(1, 2) = prefix[3] - prefix[1] = 6 - 2 = 4  correct (3+1=4)
sum(0, 0) = prefix[1] - prefix[0] = 2 - 0 = 2  correct (single element)
```

---

## 6. Types of Prefix Sum Problems

```
                         PREFIX SUM
                        /    |    \
              ----------     |     ----------
             /               |               \
        1D Array           MATRIX           COMBO
       /   |   \          2D Prefix        Techniques
      /    |    \
  Range  Count  Diff
  Query  Subarray Array
         Sum=k
              \
           + HashMap
           + Modulo
           + XOR/Product
```

### Type Overview

| Type | Problem Kind | Key Technique |
|---|---|---|
| Range Query | Sum of [L, R] | Direct prefix lookup |
| Count Subarrays | # subarrays with sum = k | prefix + HashMap |
| Modulo | Subarrays divisible by k | prefix % k + HashMap |
| Pivot Index | Left sum == right sum | prefix formula |
| 2D Matrix | Rectangle sum | 2D prefix |
| Difference Array | Range update, point query | inverse prefix |
| Prefix XOR | XOR of range | XOR prefix |
| Prefix Product | Product of range | Product prefix (careful with 0) |

---

## 7. Master Templates (Java)

### Template 1 -- Build + Range Query

```java
// Build 1-indexed prefix sum (ALWAYS use this style)
public int[] buildPrefix(int[] arr) {
    int n = arr.length;
    int[] prefix = new int[n + 1];
    for (int i = 0; i < n; i++)
        prefix[i + 1] = prefix[i] + arr[i];
    return prefix;
}

// Query: sum of arr[L..R] inclusive (0-indexed)
public int rangeSum(int[] prefix, int L, int R) {
    return prefix[R + 1] - prefix[L];
}
```

### Template 2 -- Count Subarrays with Sum = k (Prefix + HashMap)

```java
public int countSubarraysWithSum(int[] nums, int k) {
    Map<Integer, Integer> prefixCount = new HashMap<>();
    prefixCount.put(0, 1);   // CRITICAL: empty prefix has sum 0, seen once

    int prefixSum = 0, count = 0;
    for (int num : nums) {
        prefixSum += num;
        // If (prefixSum - k) was seen before, those subarrays sum to k
        count += prefixCount.getOrDefault(prefixSum - k, 0);
        prefixCount.merge(prefixSum, 1, Integer::sum);
    }
    return count;
}
```

### Template 3 -- Count Subarrays with Sum Divisible by k (Prefix + Modulo)

```java
public int countDivisibleByK(int[] nums, int k) {
    Map<Integer, Integer> remainderCount = new HashMap<>();
    remainderCount.put(0, 1);   // CRITICAL: same sentinel

    int prefixSum = 0, count = 0;
    for (int num : nums) {
        prefixSum += num;
        int rem = ((prefixSum % k) + k) % k;   // handle negative modulo!
        count += remainderCount.getOrDefault(rem, 0);
        remainderCount.merge(rem, 1, Integer::sum);
    }
    return count;
}
```

### Template 4 -- Maximum Length Subarray with Sum = k

```java
// Works with negative numbers (unlike sliding window)
public int maxLenSubarrayWithSum(int[] nums, int k) {
    Map<Integer, Integer> firstSeen = new HashMap<>();
    firstSeen.put(0, -1);   // prefix sum 0 seen at index -1 (before array)

    int prefixSum = 0, maxLen = 0;
    for (int i = 0; i < nums.length; i++) {
        prefixSum += nums[i];
        if (firstSeen.containsKey(prefixSum - k)) {
            maxLen = Math.max(maxLen, i - firstSeen.get(prefixSum - k));
        }
        // Only store FIRST occurrence (we want maximum length -> earliest left)
        firstSeen.putIfAbsent(prefixSum, i);
    }
    return maxLen;
}
```

### Template 5 -- Difference Array (Range Update)

```java
// Apply delta to all arr[L..R] += delta, then query point values
public int[] differenceArray(int[] arr, int[][] updates) {
    int n = arr.length;
    int[] diff = new int[n + 1];   // difference array

    // Apply all updates in O(1) each
    for (int[] update : updates) {
        int L = update[0], R = update[1], delta = update[2];
        diff[L] += delta;
        diff[R + 1] -= delta;
    }

    // Reconstruct final array using prefix sum of diff
    int[] result = new int[n];
    int running = 0;
    for (int i = 0; i < n; i++) {
        running += diff[i];
        result[i] = arr[i] + running;
    }
    return result;
}
```

### Template 6 -- 2D Prefix Sum

```java
// Build 2D prefix sum
public int[][] build2DPrefix(int[][] matrix) {
    int m = matrix.length, n = matrix[0].length;
    int[][] prefix = new int[m + 1][n + 1];   // 1-indexed

    for (int i = 1; i <= m; i++)
        for (int j = 1; j <= n; j++)
            prefix[i][j] = matrix[i-1][j-1]
                         + prefix[i-1][j]
                         + prefix[i][j-1]
                         - prefix[i-1][j-1];   // subtract overlap counted twice
    return prefix;
}

// Query: sum of rectangle from (r1,c1) to (r2,c2) inclusive (0-indexed)
public int query2D(int[][] prefix, int r1, int c1, int r2, int c2) {
    return prefix[r2+1][c2+1]
         - prefix[r1][c2+1]
         - prefix[r2+1][c1]
         + prefix[r1][c1];   // add back the corner subtracted twice
}
```

---

## 8. All Patterns with Problems & Code

---

### Pattern 1: Range Sum Query (Static Array)

**Cues:** "multiple queries", "sum from index i to j", "immutable array"

#### Range Sum Query -- Immutable (LC 303)
```java
class NumArray {
    private int[] prefix;

    public NumArray(int[] nums) {
        prefix = new int[nums.length + 1];
        for (int i = 0; i < nums.length; i++)
            prefix[i + 1] = prefix[i] + nums[i];
    }

    public int sumRange(int left, int right) {
        return prefix[right + 1] - prefix[left];
    }
}
// Build: O(n), Query: O(1), Space: O(n)
```

---

### Pattern 2: Subarray Sum Equals K -- Count

**Cues:** "number of subarrays", "count subarrays with sum = k", "consecutive elements sum to k"

#### Subarray Sum Equals K (LC 560) -- Most Important
```java
public int subarraySum(int[] nums, int k) {
    Map<Integer, Integer> freq = new HashMap<>();
    freq.put(0, 1);   // prefix sum of 0 seen once (empty prefix)

    int prefixSum = 0, count = 0;
    for (int num : nums) {
        prefixSum += num;
        // How many past prefixes, when subtracted, give sum k?
        count += freq.getOrDefault(prefixSum - k, 0);
        freq.merge(prefixSum, 1, Integer::sum);
    }
    return count;
}
// Time: O(n), Space: O(n)
```

**Trace through example:**
```
nums = [1, 1, 1], k = 2
i=0: prefixSum=1, look for (1-2)=-1 -> 0,   freq={0:1, 1:1}
i=1: prefixSum=2, look for (2-2)=0  -> 1,   freq={0:1, 1:1, 2:1}, count=1
i=2: prefixSum=3, look for (3-2)=1  -> 1,   freq={0:1,1:1,2:1,3:1}, count=2
Answer: 2  (subarrays [1,1] at index 0-1 and 1-2)
```

> Why `freq.put(0, 1)` before the loop? It handles the case where the subarray starts from index 0. Without it, a subarray summing exactly to k from the beginning would be missed.

---

### Pattern 3: Longest Subarray with Sum = k

**Cues:** "longest subarray with sum k", "maximum length", "find one subarray"

#### Longest Subarray with Sum = k (Handles Negatives)
```java
public int longestSubarrayWithSumK(int[] nums, int k) {
    Map<Integer, Integer> firstSeen = new HashMap<>();
    firstSeen.put(0, -1);   // empty prefix at index -1

    int prefixSum = 0, maxLen = 0;
    for (int i = 0; i < nums.length; i++) {
        prefixSum += nums[i];
        if (firstSeen.containsKey(prefixSum - k)) {
            maxLen = Math.max(maxLen, i - firstSeen.get(prefixSum - k));
        }
        firstSeen.putIfAbsent(prefixSum, i);   // ONLY first occurrence!
    }
    return maxLen;
}
// Time: O(n), Space: O(n)
```

> Critical difference from count problem:
> - Count subarrays: Use freq map, count ALL occurrences -> merge(key, 1, Integer::sum)
> - Max length subarray: Use firstSeen map, store only FIRST index -> putIfAbsent
> - Min length subarray: sliding window for non-negatives; deque for negatives

---

### Pattern 4: Pivot Index / Equal Partition

**Cues:** "pivot index", "center index", "left sum equals right sum", "split array into equal sum"

#### Find Pivot Index (LC 724)
```java
public int pivotIndex(int[] nums) {
    int total = 0;
    for (int n : nums) total += n;

    int leftSum = 0;
    for (int i = 0; i < nums.length; i++) {
        // rightSum = total - leftSum - nums[i]
        if (leftSum == total - leftSum - nums[i]) return i;
        leftSum += nums[i];
    }
    return -1;
}
// Time: O(n), Space: O(1) -- no extra array needed!
```

> Space optimization: You don't need to build the prefix array if you precompute total and scan left-to-right. rightSum = total - leftSum - nums[i].

#### Check if Array Can Be Split Into Equal Sum Parts
```java
public boolean canPartitionEqual(int[] nums) {
    int total = 0;
    for (int n : nums) total += n;
    if (total % 2 != 0) return false;

    int target = total / 2, prefixSum = 0;
    for (int num : nums) {
        prefixSum += num;
        if (prefixSum == target) return true;
    }
    return false;
}
```

---

### Pattern 5: Subarray Sum Divisible by K

**Cues:** "divisible by k", "sum is multiple of k", "remainder"

#### Subarray Sums Divisible by K (LC 974)
```java
public int subarraysDivByK(int[] nums, int k) {
    Map<Integer, Integer> remainderCount = new HashMap<>();
    remainderCount.put(0, 1);

    int prefixSum = 0, count = 0;
    for (int num : nums) {
        prefixSum += num;
        // Normalize remainder to be non-negative
        int rem = ((prefixSum % k) + k) % k;   // handles Java's negative modulo
        count += remainderCount.getOrDefault(rem, 0);
        remainderCount.merge(rem, 1, Integer::sum);
    }
    return count;
}
// Time: O(n), Space: O(k) -- at most k distinct remainders
```

**Why this works:**
```
If prefix[i] % k == prefix[j] % k, then (prefix[i] - prefix[j]) % k == 0
-> subarray arr[j..i-1] has sum divisible by k.

Two prefix sums with the SAME remainder -> their difference is divisible by k.
Count pairs with same remainder -> use frequency map.
```

**Java's negative modulo gotcha:**
```java
-7 % 3 = -1   in Java (not 2!)
Fix: ((n % k) + k) % k   -> always returns non-negative remainder
```

#### Continuous Subarray Sum (LC 523) -- Sum divisible by k, length >= 2
```java
public boolean checkSubarraySum(int[] nums, int k) {
    Map<Integer, Integer> remToIndex = new HashMap<>();
    remToIndex.put(0, -1);   // remainder 0 seen before index 0

    int prefixSum = 0;
    for (int i = 0; i < nums.length; i++) {
        prefixSum += nums[i];
        int rem = prefixSum % k;

        if (remToIndex.containsKey(rem)) {
            if (i - remToIndex.get(rem) >= 2)   // subarray length >= 2
                return true;
        } else {
            remToIndex.put(rem, i);   // store first occurrence only
        }
    }
    return false;
}
// Time: O(n), Space: O(k)
```

> Why use else (not update map)? We want the longest possible subarray to satisfy length >= 2. Store only the first time a remainder is seen.

---

### Pattern 6: Binary Array -- Count Subarrays with Equal 0s and 1s

**Cues:** "binary array", "equal number of 0s and 1s", "balanced subarray"

#### Contiguous Array (LC 525)
> Trick: Replace 0 with -1. Then "equal 0s and 1s" becomes "subarray sum = 0".

```java
public int findMaxLength(int[] nums) {
    Map<Integer, Integer> firstSeen = new HashMap<>();
    firstSeen.put(0, -1);

    int prefixSum = 0, maxLen = 0;
    for (int i = 0; i < nums.length; i++) {
        prefixSum += (nums[i] == 0) ? -1 : 1;   // transform: 0->-1, 1->+1
        if (firstSeen.containsKey(prefixSum)) {
            maxLen = Math.max(maxLen, i - firstSeen.get(prefixSum));
        } else {
            firstSeen.put(prefixSum, i);
        }
    }
    return maxLen;
}
// Time: O(n), Space: O(n)
```

**Why it works:**
```
Replacing 0 with -1:
  Equal 0s and 1s -> sum of -1s and +1s = 0
  Find longest subarray with sum = 0
  Two indices with same prefix sum -> subarray between them sums to 0
```

---

### Pattern 7: Prefix XOR

**Cues:** "XOR of subarray", "XOR queries", "number of subarrays with XOR = k"

#### XOR Queries of a Subarray (LC 1310)
```java
public int[] xorQueries(int[] arr, int[][] queries) {
    int n = arr.length;
    int[] prefixXor = new int[n + 1];
    for (int i = 0; i < n; i++)
        prefixXor[i + 1] = prefixXor[i] ^ arr[i];

    int[] result = new int[queries.length];
    for (int i = 0; i < queries.length; i++) {
        int L = queries[i][0], R = queries[i][1];
        result[i] = prefixXor[R + 1] ^ prefixXor[L];   // same formula, ^ instead of -
    }
    return result;
}
// Build: O(n), Query: O(1)
```

#### Count Subarrays with XOR = k
```java
public int countSubarraysWithXOR(int[] nums, int k) {
    Map<Integer, Integer> freq = new HashMap<>();
    freq.put(0, 1);

    int prefixXor = 0, count = 0;
    for (int num : nums) {
        prefixXor ^= num;
        // Looking for prefixXor ^ previousXor = k -> previousXor = prefixXor ^ k
        count += freq.getOrDefault(prefixXor ^ k, 0);
        freq.merge(prefixXor, 1, Integer::sum);
    }
    return count;
}
// Same structure as sum = k, just replace + with ^ and - with ^
```

> XOR Property: a ^ b = c  means  a ^ c = b  and  b ^ c = a
> So prefixXor[R] ^ prefixXor[L-1] = k means look for prefixXor[L-1] = prefixXor[R] ^ k

---

### Pattern 8: Prefix Product

**Cues:** "product of array except self", "product of range"

#### Product of Array Except Self (LC 238)
```java
public int[] productExceptSelf(int[] nums) {
    int n = nums.length;
    int[] result = new int[n];

    // Left pass: result[i] = product of all elements LEFT of i
    result[0] = 1;
    for (int i = 1; i < n; i++)
        result[i] = result[i-1] * nums[i-1];

    // Right pass: multiply by product of all elements RIGHT of i
    int rightProduct = 1;
    for (int i = n - 1; i >= 0; i--) {
        result[i] *= rightProduct;
        rightProduct *= nums[i];
    }
    return result;
}
// Time: O(n), Space: O(1) extra (output array does not count)
```

> Never use division for range product if zeros are possible. Use left/right prefix products or segment tree.

---

### Pattern 9: Subarray with Sum <= k (Max Sum Close to k)

#### Max Sum of Subarray with Sum <= k
```java
// For non-negative: Sliding Window
// For negatives: Sort prefix sums + binary search via TreeSet
public int maxSumLessThanOrEqualK(int[] nums, int k) {
    TreeSet<Integer> sortedPrefix = new TreeSet<>();
    sortedPrefix.add(0);

    int prefixSum = 0, result = Integer.MIN_VALUE;
    for (int num : nums) {
        prefixSum += num;
        // We want: prefixSum - prev <= k -> prev >= prefixSum - k
        Integer prev = sortedPrefix.ceiling(prefixSum - k);
        if (prev != null) result = Math.max(result, prefixSum - prev);
        sortedPrefix.add(prefixSum);
    }
    return result;
}
// Time: O(n log n), Space: O(n)
```

---

### Pattern 10: Running Balance / Altitude Problems

**Cues:** "running sum", "altitude", "water level", "can you reach"

#### Find the Highest Altitude (LC 1732)
```java
public int largestAltitude(int[] gain) {
    int maxAlt = 0, alt = 0;
    for (int g : gain) {
        alt += g;
        maxAlt = Math.max(maxAlt, alt);
    }
    return maxAlt;
}
// This IS a prefix sum -- computed on-the-fly without storing the array
```

#### Running Sum of 1d Array (LC 1480)
```java
public int[] runningSum(int[] nums) {
    for (int i = 1; i < nums.length; i++)
        nums[i] += nums[i - 1];   // in-place prefix sum
    return nums;
}
```

---

### Pattern 11: Difference Array -- Range Updates

**Cues:** "range update", "add to all elements in range", "apply operations to intervals"

#### Corporate Flight Bookings (LC 1109)
```java
public int[] corpFlightBookings(int[][] bookings, int n) {
    int[] diff = new int[n + 1];   // 1-indexed, extra slot at end

    for (int[] booking : bookings) {
        int first = booking[0] - 1;  // convert to 0-indexed
        int last  = booking[1] - 1;
        int seats = booking[2];
        diff[first] += seats;
        if (last + 1 < n) diff[last + 1] -= seats;
    }

    int[] result = new int[n];
    int running = 0;
    for (int i = 0; i < n; i++) {
        running += diff[i];
        result[i] = running;
    }
    return result;
}
// Time: O(b + n), Space: O(n)
```

#### Car Pooling (LC 1094)
```java
public boolean carPooling(int[][] trips, int capacity) {
    int[] diff = new int[1001];   // max location = 1000

    for (int[] trip : trips) {
        diff[trip[1]] += trip[0];     // passengers board at from
        diff[trip[2]] -= trip[0];     // passengers alight at to
    }

    int passengers = 0;
    for (int loc = 0; loc <= 1000; loc++) {
        passengers += diff[loc];
        if (passengers > capacity) return false;
    }
    return true;
}
// Time: O(n + max_location), Space: O(max_location)
```

> Difference Array Pattern:
> - Add delta to range [L, R]: diff[L] += delta, diff[R+1] -= delta
> - Reconstruct final values: result[i] = result[i-1] + diff[i] (prefix sum of diff)
> - Think of it as the "inverse" of prefix sum: encode range updates, decode with prefix sum

---

### Pattern 12: Prefix Sum + Binary Search

**Cues:** "kth sum", "weighted random", "online range queries"

#### Random Pick with Weight (LC 528)
```java
class Solution {
    private int[] prefix;
    private Random rand;

    public Solution(int[] w) {
        rand = new Random();
        prefix = new int[w.length + 1];
        for (int i = 0; i < w.length; i++)
            prefix[i + 1] = prefix[i] + w[i];
    }

    public int pickIndex() {
        int target = rand.nextInt(prefix[prefix.length - 1]) + 1;
        int lo = 1, hi = prefix.length - 1;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (prefix[mid] < target) lo = mid + 1;
            else hi = mid;
        }
        return lo - 1;
    }
}
// Build: O(n), Pick: O(log n)
```

---

### Pattern 13: Prefix Sum in Trees

**Cues:** "path sum in tree", "root-to-leaf sum = k", "number of paths with sum k"

#### Path Sum III (LC 437)
```java
class Solution {
    private Map<Long, Integer> prefixCount = new HashMap<>();
    private int target, result = 0;

    public int pathSum(TreeNode root, int targetSum) {
        target = targetSum;
        prefixCount.put(0L, 1);
        dfs(root, 0L);
        return result;
    }

    private void dfs(TreeNode node, long currentSum) {
        if (node == null) return;

        currentSum += node.val;
        result += prefixCount.getOrDefault(currentSum - target, 0);
        prefixCount.merge(currentSum, 1, Integer::sum);

        dfs(node.left, currentSum);
        dfs(node.right, currentSum);

        // Backtrack: remove current node's contribution
        prefixCount.merge(currentSum, -1, Integer::sum);
    }
}
// Time: O(n), Space: O(n)
```

> Backtracking in tree prefix sum: When DFS returns from a subtree, MUST decrement the count. Otherwise, sibling subtrees would incorrectly see paths from other branches.

---

## 9. 2D Prefix Sum (Matrix)

### Building the 2D Prefix Sum -- Step by Step

```
matrix = [[3, 0, 1, 4],
           [5, 6, 3, 2],
           [1, 2, 0, 1]]

Step 1: Initialize prefix[m+1][n+1] with all zeros (extra row and col)

Step 2: Fill using:
  prefix[i][j] = matrix[i-1][j-1] + prefix[i-1][j] + prefix[i][j-1] - prefix[i-1][j-1]
```

### Query Formula Derivation (Inclusion-Exclusion)

```
Want: sum of rectangle with corners (r1,c1) and (r2,c2)

prefix[r2+1][c2+1] = entire rectangle from (0,0) to (r2,c2)
prefix[r1][c2+1]   = rectangle above target (rows 0 to r1-1)
prefix[r2+1][c1]   = rectangle left of target (cols 0 to c1-1)
prefix[r1][c1]     = top-left corner (subtracted twice, so add back once)

Answer = prefix[r2+1][c2+1] - prefix[r1][c2+1] - prefix[r2+1][c1] + prefix[r1][c1]
```

```
+-------+---------------+
|       |               |
|  TL   |      T        |    TL = prefix[r1][c1]
|       |               |    T  = prefix[r1][c2+1]
+-------+-------+-------+    L  = prefix[r2+1][c1]
|       |       |            F  = prefix[r2+1][c2+1]
|  L    | TARGET|
|       |       |    Answer = F - T - L + TL
+-------+-------+
```

#### Range Sum Query 2D -- Immutable (LC 304)
```java
class NumMatrix {
    private int[][] prefix;

    public NumMatrix(int[][] matrix) {
        int m = matrix.length, n = matrix[0].length;
        prefix = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                prefix[i][j] = matrix[i-1][j-1]
                             + prefix[i-1][j]
                             + prefix[i][j-1]
                             - prefix[i-1][j-1];
    }

    public int sumRegion(int r1, int c1, int r2, int c2) {
        return prefix[r2+1][c2+1]
             - prefix[r1][c2+1]
             - prefix[r2+1][c1]
             + prefix[r1][c1];
    }
}
// Build: O(m*n), Query: O(1)
```

#### Number of Submatrices That Sum to Target (LC 1074)
```java
public int numSubmatrixSumTarget(int[][] matrix, int target) {
    int m = matrix.length, n = matrix[0].length;
    int count = 0;

    // Build row-wise prefix sum
    int[][] rowPrefix = new int[m][n + 1];
    for (int r = 0; r < m; r++)
        for (int c = 0; c < n; c++)
            rowPrefix[r][c + 1] = rowPrefix[r][c] + matrix[r][c];

    // Fix left and right column boundaries
    for (int c1 = 0; c1 < n; c1++) {
        for (int c2 = c1 + 1; c2 <= n; c2++) {
            // Reduce to 1D: count subarrays with sum = target
            Map<Integer, Integer> freq = new HashMap<>();
            freq.put(0, 1);
            int colSum = 0;
            for (int r = 0; r < m; r++) {
                colSum += rowPrefix[r][c2] - rowPrefix[r][c1];
                count += freq.getOrDefault(colSum - target, 0);
                freq.merge(colSum, 1, Integer::sum);
            }
        }
    }
    return count;
}
// Time: O(m * n^2), Space: O(m)
```

---

## 10. Prefix Sum + HashMap (The Power Combo)

This is the most powerful and frequently asked prefix sum pattern. Understand it deeply.

### The Core Insight

```
We want: arr[L] + arr[L+1] + ... + arr[R] = target
Which is: prefix[R+1] - prefix[L] = target
Rewritten: prefix[L] = prefix[R+1] - target

So at each index R:
  "How many previous prefix sums equal (currentPrefixSum - target)?"
  -> Look up (currentPrefixSum - target) in HashMap
```

### Three Variants

#### Variant 1: Count Subarrays (All occurrences)
```java
Map<Integer, Integer> freq = new HashMap<>();
freq.put(0, 1);         // sentinel: empty prefix
int prefixSum = 0, count = 0;
for (int num : nums) {
    prefixSum += num;
    count += freq.getOrDefault(prefixSum - k, 0);   // all past occurrences
    freq.merge(prefixSum, 1, Integer::sum);          // count ALL occurrences
}
```

#### Variant 2: Maximum Length Subarray
```java
Map<Integer, Integer> firstSeen = new HashMap<>();
firstSeen.put(0, -1);   // sentinel: index -1
int prefixSum = 0, maxLen = 0;
for (int i = 0; i < nums.length; i++) {
    prefixSum += nums[i];
    if (firstSeen.containsKey(prefixSum - k))
        maxLen = Math.max(maxLen, i - firstSeen.get(prefixSum - k));
    firstSeen.putIfAbsent(prefixSum, i);    // ONLY first occurrence (maximize gap)
}
```

#### Variant 3: Minimum Length Subarray
```java
// For minimum length with negatives -> use Deque (LC 862)
// For minimum length with non-negatives -> use Sliding Window (O(n), O(1))
```

### The Sentinel Value -- Never Forget This

| Purpose | Sentinel | Why |
|---|---|---|
| Count subarrays | `freq.put(0, 1)` | Empty prefix has sum 0, seen once |
| Max length subarray | `firstSeen.put(0, -1)` | Empty prefix at "index -1" (before array) |
| Divisibility | `remainderCount.put(0, 1)` | Prefix with remainder 0 seen once |

> The sentinel is the number 1 source of bugs. Always add it before the loop, and always think: "what does the subarray starting from index 0 look like?"

### When Prefix Sum + HashMap Breaks Down

| Situation | Problem | Solution |
|---|---|---|
| Need min length with negatives | HashMap gives max not min | Prefix sum + monotonic deque (LC 862) |
| Need to find actual subarray indices | HashMap gives count, not indices | Store index in HashMap, reconstruct |
| Updates to the array | Prefix needs rebuild | Segment Tree / BIT |

---

## 11. Prefix XOR / Prefix Product

### Prefix XOR vs Prefix Sum -- Comparison

| Property | Prefix Sum | Prefix XOR |
|---|---|---|
| Build formula | `prefix[i+1] = prefix[i] + arr[i]` | `prefix[i+1] = prefix[i] ^ arr[i]` |
| Query formula | `prefix[R+1] - prefix[L]` | `prefix[R+1] ^ prefix[L]` |
| Inverse operation | Subtraction | XOR (self-inverse!) |
| "Sum = k" analog | Look for `prefixSum - k` | Look for `prefixXor ^ k` |
| Sentinel | `prefix[0] = 0` | `prefix[0] = 0` |

### Why XOR Uses ^ Instead of - in Query
```
prefix[R+1] ^ prefix[L]
  = (arr[0]^...^arr[L-1]^arr[L]^...^arr[R]) ^ (arr[0]^...^arr[L-1])
  = arr[L] ^ ... ^ arr[R]   because x^x=0 and x^0=x

XOR cancels itself, unlike addition which requires subtraction.
```

### Prefix Product -- Careful with Zeros!

```java
// Safe: two-pass left/right prefix products (no division needed)
public int[] productExceptSelf(int[] nums) {
    int n = nums.length;
    int[] left = new int[n], right = new int[n];
    left[0] = right[n-1] = 1;

    for (int i = 1; i < n; i++)       left[i] = left[i-1] * nums[i-1];
    for (int i = n-2; i >= 0; i--)    right[i] = right[i+1] * nums[i+1];

    int[] result = new int[n];
    for (int i = 0; i < n; i++) result[i] = left[i] * right[i];
    return result;
}
```

---

## 12. Difference Array (Inverse of Prefix Sum)

### Concept

```
Prefix Sum:    range QUERY in O(1),  point UPDATE in O(n)
Diff Array:    range UPDATE in O(1), final QUERY  in O(n) [via prefix sum]

They are inverses of each other.
```

### How It Works

```
Difference array diff[i] = arr[i] - arr[i-1]  (with arr[-1] = 0)

Property: arr[i] = diff[0] + diff[1] + ... + diff[i]  <- prefix sum of diff!

Range update [L, R] += delta:
  diff[L]   += delta   <- delta starts affecting from index L
  diff[R+1] -= delta   <- delta stops affecting after index R

Reconstruct: take prefix sum of diff
```

### Step-by-Step Example

```
Initial arr: [0, 0, 0, 0, 0]
diff:        [0, 0, 0, 0, 0, 0]  (size n+1)

Add 5 to [1, 3]:  diff[1]+=5, diff[4]-=5  ->  [0, 5, 0, 0, -5, 0]
Add 3 to [2, 4]:  diff[2]+=3, diff[5]-=3  ->  [0, 5, 3, 0, -5, -3]

Prefix sum of diff:
  result[0] = 0
  result[1] = 0+5 = 5
  result[2] = 5+3 = 8
  result[3] = 8+0 = 8
  result[4] = 8-5 = 3
  Correct: arr=[0, 5, 8, 8, 3]
```

### When to Use Which

| Scenario | Best Structure |
|---|---|
| Multiple range queries, no updates | Prefix Sum |
| Multiple range updates, one final query | Difference Array |
| Mix of updates and queries online | Segment Tree / BIT |
| 2D range updates | 2D Difference Array |

---

## 13. Complexity Cheat Sheet

| Technique | Build Time | Query Time | Space |
|---|---|---|---|
| 1D Prefix Sum | O(n) | O(1) | O(n) |
| 1D Prefix Sum + HashMap | O(n) | O(1) avg | O(n) |
| 2D Prefix Sum | O(m*n) | O(1) | O(m*n) |
| Prefix XOR | O(n) | O(1) | O(n) |
| Prefix Product (left/right) | O(n) | O(1) | O(n) |
| Difference Array | O(n+updates) | O(n) final | O(n) |
| Prefix Sum + Binary Search | O(n log n) | O(log n) | O(n) |
| Prefix Sum in Tree (DFS) | O(n) | -- | O(n) |

---

## 14. Common Mistakes & How to Avoid Them

### Mistake 1: Forgetting the Sentinel in HashMap

```java
// WRONG: misses subarrays starting from index 0
Map<Integer, Integer> freq = new HashMap<>();
// Missing: freq.put(0, 1);

// CORRECT:
Map<Integer, Integer> freq = new HashMap<>();
freq.put(0, 1);   // ALWAYS add this before the loop
```

### Mistake 2: Using Wrong Sentinel Index (Count vs Max Length)

```java
// For MAX LENGTH: sentinel is put(0, -1) -- index -1 means "before array"
// For COUNT:      sentinel is put(0, 1)  -- empty prefix counted once

// WRONG: max length with count-style sentinel
firstSeen.put(0, 1);   // 1 is a count, not an index!

// CORRECT:
firstSeen.put(0, -1);  // -1 is the "virtual" index before the array
```

### Mistake 3: Overwriting firstSeen Map (for Max Length)

```java
// WRONG: overwrites first occurrence with later one -> shorter subarray
for (int i = 0; ...) {
    ...
    firstSeen.put(prefixSum, i);   // BUG: overwrites earlier index
}

// CORRECT:
firstSeen.putIfAbsent(prefixSum, i);  // only store FIRST occurrence
```

### Mistake 4: Negative Modulo in Java

```java
// WRONG: negative input gives negative remainder in Java
int rem = prefixSum % k;   // can be negative if prefixSum < 0!

// CORRECT: normalize to [0, k-1]
int rem = ((prefixSum % k) + k) % k;
```

### Mistake 5: 0-indexed vs 1-indexed Confusion

```java
// WRONG: mixing styles
int[] prefix = new int[n];   // 0-indexed style
prefix[0] = arr[0];
return prefix[R] - prefix[L];   // BUG: wrong for L=0

// CORRECT: always 1-indexed (size n+1, prefix[0]=0)
int[] prefix = new int[n + 1];
for (int i = 0; i < n; i++) prefix[i+1] = prefix[i] + arr[i];
return prefix[R+1] - prefix[L];   // clean, no edge cases
```

### Mistake 6: Integer Overflow

```java
// WRONG:
int prefixSum = 0;
prefixSum += nums[i];   // overflow if sum > 2^31 - 1

// CORRECT:
long prefixSum = 0;
Map<Long, Integer> freq = new HashMap<>();  // key must also be Long
```

### Mistake 7: Wrong 2D Formula (Forgetting Corner)

```java
// WRONG: subtracts corner region twice, never adds back
return prefix[r2+1][c2+1] - prefix[r1][c2+1] - prefix[r2+1][c1];

// CORRECT: inclusion-exclusion -- add corner back!
return prefix[r2+1][c2+1] - prefix[r1][c2+1] - prefix[r2+1][c1] + prefix[r1][c1];
```

### Mistake 8: Using Prefix Sum for Range Updates

```
WRONG: Using prefix sum array when problem has range updates.
  Each update would require O(n) rebuild.

CORRECT: Use Difference Array for range updates.
  Each update is O(1), final reconstruction is O(n).
```

### Mistake 9: Using Sliding Window for Negative Numbers + Variable Length

```
Problem: "Minimum length subarray with sum >= k" with negative numbers
WRONG: Sliding window (breaks with negatives -- can't know when to shrink)
CORRECT: Prefix sum + monotonic deque (LC 862)
```

### Mistake 10: Not Using getOrDefault

```java
// WRONG: NullPointerException if key not in map
count += freq.get(prefixSum - k);

// CORRECT:
count += freq.getOrDefault(prefixSum - k, 0);
```

### Mistake 11: Not Backtracking in Tree DFS

```java
// WRONG: sibling subtrees see each other's prefix sums
dfs(node.left, currentSum);
dfs(node.right, currentSum);
// Missing: prefixCount.merge(currentSum, -1, Integer::sum);

// CORRECT: backtrack after both children
dfs(node.left, currentSum);
dfs(node.right, currentSum);
prefixCount.merge(currentSum, -1, Integer::sum);  // undo this node
```

---

## 15. Decision Flowchart

```
Does the problem involve a 1D array or string?
+-- YES ->
|     Is it a RANGE QUERY (sum of [L, R]) with multiple queries?
|     +-- YES -> BUILD PREFIX ARRAY, query O(1)
|
|     Do you need COUNT of subarrays with some property?
|     +-- sum = k?               -> Prefix + HashMap, sentinel put(0,1)
|     +-- sum divisible by k?    -> Prefix + HashMap (modulo), normalize rem
|     +-- XOR = k?               -> Prefix XOR + HashMap
|
|     Do you need MAX LENGTH subarray with sum = k?
|     +-- All non-negative?      -> Sliding Window, O(n) O(1)
|     +-- Has negatives?         -> Prefix + HashMap firstSeen, sentinel put(0,-1)
|
|     Do you need MIN LENGTH subarray with sum >= k?
|     +-- All non-negative?      -> Sliding Window, O(n) O(1)
|     +-- Has negatives?         -> Prefix + Monotonic Deque (LC 862)
|
|     Do you need RANGE UPDATES (add delta to [L, R])?
|     +-- YES -> DIFFERENCE ARRAY

Does the problem involve a 2D MATRIX?
+-- YES ->
|     Single rectangle sum?           -> 2D PREFIX ARRAY, query O(1)
|     Count submatrices sum = k?      -> Fix rows, reduce to 1D count problem
|     Max sum submatrix <= k?         -> Fix rows, reduce to 1D max-sum<=k (TreeSet)

Does the problem involve a TREE?
+-- YES -> DFS + PREFIX HASHMAP + BACKTRACK on return!

Is the problem about WEIGHTS or PROBABILITIES?
+-- YES -> PREFIX SUM + BINARY SEARCH (weighted random pick)
```

---

## 16. Edge Cases Checklist

Before finalizing your solution, verify these:

- [ ] **Empty array** -- `nums.length == 0` -> return 0 or handle explicitly
- [ ] **Single element** -- prefix has 2 elements: [0, nums[0]]
- [ ] **k = 0** -- subarray sum = 0; modulo would be divide-by-zero! Guard it
- [ ] **Negative numbers** -- ensure prefix sum uses `long`; sliding window won't work for variable-length
- [ ] **All same elements** -- stress-test your formula with [2,2,2,2]
- [ ] **No valid subarray** -- result stays `Integer.MAX_VALUE` or 0; handle the return
- [ ] **Entire array is the answer** -- L=0, R=n-1; check sentinel handles this
- [ ] **Integer overflow** -- use `long` for sums of large arrays
- [ ] **Negative modulo** -- always normalize: `((n % k) + k) % k`
- [ ] **2D single row/col** -- r1=r2 or c1=c2 (valid single row/col query)
- [ ] **Difference array boundary** -- `diff[R+1] -= delta` where R=n-1 needs diff[n], so array must be size n+1
- [ ] **Tree backtracking** -- are you undoing the prefix count after each DFS node?

---

## 17. Interview Communication Guide

### How to Verbalize Your Thought Process

**Step 1 -- Recognize the pattern:**
> "This asks for the count/length of subarrays with a specific sum property. Brute force is O(n^2). I can use prefix sums to get O(n)."

**Step 2 -- Explain prefix sum:**
> "I'll precompute prefix sums where prefix[i] is the sum of the first i elements. Then sum of arr[L..R] = prefix[R+1] - prefix[L]."

**Step 3 -- Explain the HashMap combo (if needed):**
> "I need to count pairs where prefix[j] - prefix[i] = k, so I look for how many previous prefix sums equal currentPrefix - k. I'll use a HashMap for O(1) lookup."

**Step 4 -- Address the sentinel:**
> "I initialize the map with {0: 1} to handle subarrays starting at index 0 that sum exactly to k."

**Step 5 -- Walk through a small example:**
> "Let me trace through [1, 2, 3] with k = 3..."

**Step 6 -- State complexity:**
> "Single pass over the array with O(1) HashMap operations per step gives O(n) time. The HashMap holds at most n distinct prefix sums, so O(n) space."

### Questions the Interviewer Might Ask

| Question | Answer |
|---|---|
| "Why initialize the map with {0:1}?" | To handle subarrays starting from index 0 -- without it, when prefix[i] == k the lookup for 0 would fail |
| "Does this work with negative numbers?" | Yes for count/max-length. No for sliding window approaches |
| "Can you reduce space to O(1)?" | Only if k=0 or pivot-style problems; generally O(n) is needed |
| "What if k=0?" | Works fine -- looking for prefix sums that repeat |
| "Why putIfAbsent for max length?" | We want earliest first occurrence to maximize the gap; later entries only shrink it |
| "Count vs max-length -- what differs?" | Count: merge all. Max-length: putIfAbsent + use index not frequency |
| "Why is 2D prefix query O(1)?" | Just 4 lookups + 3 arithmetic ops, regardless of rectangle size |
| "How is this different from Segment Tree?" | Prefix sum is static (no updates). Segment Tree handles dynamic updates in O(log n) |

---

## 18. Quick-Fire Problem to Pattern Mapping

| Problem | Pattern | Key Insight |
|---|---|---|
| Range Sum Query (LC 303) | 1D Prefix | `prefix[R+1] - prefix[L]` |
| Subarray Sum Equals K (LC 560) | Prefix + HashMap | Look for `prefixSum - k` |
| Continuous Subarray Sum (LC 523) | Prefix + Modulo | Same remainder = divisible difference |
| Subarrays Div by K (LC 974) | Prefix + Modulo count | Normalize negative modulo |
| Find Pivot Index (LC 724) | Prefix formula | `leftSum == total - leftSum - nums[i]` |
| Contiguous Array (LC 525) | Prefix + 0 to -1 trick | Equal 0s,1s -> sum=0 |
| Longest Subarray Sum=k (negatives) | Prefix + firstSeen | putIfAbsent, sentinel (-1) |
| Max Sum Subarray <= k | Prefix + TreeSet ceiling | TreeSet.ceiling(prefixSum - k) |
| Range Sum Query 2D (LC 304) | 2D Prefix | Inclusion-exclusion formula |
| Count Submatrices sum=target (LC 1074) | 2D to 1D reduction | Fix rows, 1D count problem |
| Product Except Self (LC 238) | Left+Right prefix product | Two-pass, no division |
| XOR Queries (LC 1310) | Prefix XOR | `prefix[R+1] ^ prefix[L]` |
| Count Subarrays XOR=k | Prefix XOR + HashMap | Look for `prefixXor ^ k` |
| Random Pick Weight (LC 528) | Prefix + Binary Search | Weighted random via prefix |
| Corporate Flight Bookings (LC 1109) | Difference Array | Range update O(1), rebuild O(n) |
| Car Pooling (LC 1094) | Difference Array | Add/remove at stop points |
| Path Sum III (LC 437) | DFS + Prefix HashMap | Backtrack on return! |
| Shortest Subarray Sum>=k negatives (LC 862) | Prefix + Monotonic Deque | Cannot use sliding window |
| Max Sum Rectangle <=k (LC 363) | Fix rows + 1D <=k | TreeSet ceiling trick |
| Running Sum of 1d Array (LC 1480) | In-place prefix sum | nums[i] += nums[i-1] |

---

## One-Page Quick Reference

```
===========================================================================
                      PREFIX SUM QUICK REF
===========================================================================
BUILD (always 1-indexed, size n+1):
  prefix[0] = 0
  prefix[i+1] = prefix[i] + arr[i]

RANGE QUERY:   sum(L, R) = prefix[R+1] - prefix[L]
RANGE XOR:     xor(L, R) = prefixXor[R+1] ^ prefixXor[L]
===========================================================================
COUNT SUBARRAYS (sum = k):
  freq = {0: 1}  <- SENTINEL: put BEFORE the loop
  for each num:
    prefixSum += num
    count += freq.getOrDefault(prefixSum - k, 0)
    freq.merge(prefixSum, 1, Integer::sum)
===========================================================================
MAX LENGTH SUBARRAY (sum = k):
  firstSeen = {0: -1}  <- SENTINEL: index -1 (before array)
  for i, num:
    prefixSum += num
    if firstSeen has (prefixSum - k): maxLen = max(maxLen, i - firstSeen.get(...))
    firstSeen.putIfAbsent(prefixSum, i)  <- ONLY first occurrence
===========================================================================
MODULO (divisible by k):
  rem = ((prefixSum % k) + k) % k  <- always normalize!
  Same HashMap pattern as count subarrays
===========================================================================
DIFFERENCE ARRAY (range updates):
  Update [L, R] += delta:
    diff[L] += delta;  diff[R+1] -= delta
  Reconstruct: take prefix sum of diff[]
===========================================================================
2D PREFIX (build):
  p[i][j] = mat[i-1][j-1] + p[i-1][j] + p[i][j-1] - p[i-1][j-1]
2D QUERY (r1,c1) to (r2,c2):
  p[r2+1][c2+1] - p[r1][c2+1] - p[r2+1][c1] + p[r1][c1]
===========================================================================
NEGATIVES + SUM >= K  -> Prefix + Monotonic Deque (not sliding window)
BINARY ARRAY equal 0s,1s -> Replace 0 with -1, find sum=0 subarray
TREE PATH SUM -> DFS + Prefix HashMap + BACKTRACK on return!
XOR RANGE -> Replace - with ^, look for prefixXor ^ k in HashMap
===========================================================================
```

---

*Last revised: 2026 | Patterns cover LeetCode Easy to Hard | Java throughout*
