# 🪟 Sliding Window — Complete Interview Cheat Sheet
> **One document. Zero confusion. Interview-ready.**

---

## 📋 Table of Contents
1. [What Is Sliding Window?](#1-what-is-sliding-window)
2. [The Core Intuition](#2-the-core-intuition)
3. [Recognition Cues — When to Use It](#3-recognition-cues--when-to-use-it)
4. [Types of Sliding Window](#4-types-of-sliding-window)
5. [Fixed-Size Window — Deep Dive](#5-fixed-size-window--deep-dive)
6. [Variable-Size Window — Deep Dive](#6-variable-size-window--deep-dive)
7. [The Master Templates (Java)](#7-the-master-templates-java)
8. [All Patterns with Problems & Code](#8-all-patterns-with-problems--code)
9. [Data Structures Inside the Window](#9-data-structures-inside-the-window)
10. [Complexity Cheat Sheet](#10-complexity-cheat-sheet)
11. [Common Mistakes & How to Avoid Them](#11-common-mistakes--how-to-avoid-them)
12. [Decision Flowchart](#12-decision-flowchart)
13. [Edge Cases Checklist](#13-edge-cases-checklist)
14. [Interview Communication Guide](#14-interview-communication-guide)
15. [Quick-Fire Problem → Pattern Mapping](#15-quick-fire-problem--pattern-mapping)

---

## 1. What Is Sliding Window?

Sliding Window is an **optimization technique** that converts a nested-loop O(n²) brute force into a single-pass O(n) solution by maintaining a "window" — a contiguous subarray or substring — and **sliding** it across the input without reprocessing elements.

```
Array:  [2, 1, 5, 1, 3, 2]
         ↑_____↑            ← window of size 3 starts here
            ↑_____↑         ← slides right by 1
               ↑_____↑      ← slides right again
```

**Key insight:** When the window moves right by one:
- You **add** the new right element
- You **remove** the old left element
- You **do NOT recompute** everything inside

---

## 2. The Core Intuition

### Brute Force vs Sliding Window

```
Problem: Max sum of subarray of size k

BRUTE FORCE — O(n·k):
for i in 0..n-k:
    sum = 0
    for j in i..i+k:      ← recomputes sum every time
        sum += arr[j]

SLIDING WINDOW — O(n):
Compute first window sum.
Then for each step:
    sum = sum - arr[left] + arr[right]  ← just swap one element
```

### The "Window Contract"
Every sliding window maintains an **invariant** — a condition the window must always satisfy:
- Fixed: window size == k always
- Variable: the window is the *largest* (or smallest) valid window ending at `right`

---

## 3. Recognition Cues — When to Use It

### ✅ Strong Signals (Use Sliding Window)

| Signal in Problem Statement | Example |
|---|---|
| "subarray" / "substring" | "longest substring with…" |
| "contiguous" | "contiguous subarray sum" |
| "window of size k" | "max average subarray of size k" |
| "at most k distinct" | "at most 2 distinct characters" |
| "minimum length subarray" | "min window to cover…" |
| "no repeating characters" | "longest without repeating" |
| Input is array or string | almost always |
| Linear scan makes sense | ordering matters |

### ❌ Signals It's NOT Sliding Window

| Signal | Use Instead |
|---|---|
| Non-contiguous elements | DP / Greedy |
| Subsets / combinations | Backtracking |
| Sorted order doesn't matter | HashMap / Set |
| 2D grid | BFS / DFS |
| Input can be negative + you want subarray sum | Kadane's / Prefix Sum |

### 🟡 Tricky Cases
- **Circular arrays** → Sliding window on doubled array
- **Negative numbers + fixed window** → Still OK (fixed window works with negatives)
- **Negative numbers + variable window (sum ≥ k)** → Sliding window BREAKS; use prefix sum
- **Multiple constraints** → Usually still sliding window, just track more state

---

## 4. Types of Sliding Window

```
                    SLIDING WINDOW
                   /              \
            FIXED SIZE          VARIABLE SIZE
           (k is given)        (find optimal k)
                               /              \
                          EXPAND          SHRINK
                        to satisfy      to satisfy
                        (maximize)       (minimize)
```

### Fixed vs Variable — Quick Comparison

| Property | Fixed Size | Variable Size |
|---|---|---|
| Window size | Always exactly k | Changes dynamically |
| Pointers move | Both move together | `right` expands, `left` shrinks |
| When to shrink? | Every step (left = right - k + 1) | When constraint is violated |
| Goal | Usually max/min of some metric | Longest or shortest valid window |

---

## 5. Fixed-Size Window — Deep Dive

### How It Works
```
Step 0: Build first window [0..k-1]
Step i: left = i, right = i + k - 1
        → remove arr[left-1], add arr[right]
```

### Pointer Movement
```
right moves: every iteration (right++)
left moves:  every iteration (left = right - k + 1)
```
They always move **in lockstep**. The window is always exactly size k.

### Template
```java
// Fixed window of size k
int left = 0, windowSum = 0;
int result = Integer.MIN_VALUE;

for (int right = 0; right < nums.length; right++) {
    windowSum += nums[right];               // EXPAND: add new right element

    if (right >= k - 1) {                   // window is full
        result = Math.max(result, windowSum); // RECORD answer
        windowSum -= nums[left];            // SHRINK: remove leftmost
        left++;
    }
}
```

### Classic Fixed-Window Problems

#### Problem 1: Maximum Sum Subarray of Size K
```java
public int maxSumSubarray(int[] nums, int k) {
    int left = 0, windowSum = 0, maxSum = Integer.MIN_VALUE;

    for (int right = 0; right < nums.length; right++) {
        windowSum += nums[right];

        if (right >= k - 1) {
            maxSum = Math.max(maxSum, windowSum);
            windowSum -= nums[left++];
        }
    }
    return maxSum;
}
// Time: O(n), Space: O(1)
```

#### Problem 2: Maximum Average Subarray of Length K (LC 643)
```java
public double findMaxAverage(int[] nums, int k) {
    double windowSum = 0;
    for (int i = 0; i < k; i++) windowSum += nums[i]; // seed first window

    double maxSum = windowSum;
    for (int i = k; i < nums.length; i++) {
        windowSum += nums[i] - nums[i - k]; // slide: add new, remove old
        maxSum = Math.max(maxSum, windowSum);
    }
    return maxSum / k;
}
// Time: O(n), Space: O(1)
```

#### Problem 3: Contains Duplicate Within K Distance (LC 219)
```java
public boolean containsNearbyDuplicate(int[] nums, int k) {
    Set<Integer> window = new HashSet<>();

    for (int right = 0; right < nums.length; right++) {
        if (window.contains(nums[right])) return true;
        window.add(nums[right]);

        if (window.size() > k) {          // maintain fixed window size
            window.remove(nums[right - k]);
        }
    }
    return false;
}
// Time: O(n), Space: O(k)
```

#### Problem 4: Find All Anagrams in String (LC 438)
```java
public List<Integer> findAnagrams(String s, String p) {
    List<Integer> result = new ArrayList<>();
    if (s.length() < p.length()) return result;

    int[] pCount = new int[26], wCount = new int[26];
    int k = p.length();

    // Seed first window
    for (int i = 0; i < k; i++) {
        pCount[p.charAt(i) - 'a']++;
        wCount[s.charAt(i) - 'a']++;
    }
    if (Arrays.equals(pCount, wCount)) result.add(0);

    for (int i = k; i < s.length(); i++) {
        wCount[s.charAt(i) - 'a']++;            // add new right
        wCount[s.charAt(i - k) - 'a']--;        // remove old left
        if (Arrays.equals(pCount, wCount)) result.add(i - k + 1);
    }
    return result;
}
// Time: O(n), Space: O(1) — fixed 26-char alphabet
```

> 💡 **Optimization tip for frequency comparison:** Instead of `Arrays.equals()` every step, maintain a `matches` counter. Increment/decrement when a char frequency goes from "wrong" to "right" or vice versa. Reduces inner work from O(26) to O(1).

---

## 6. Variable-Size Window — Deep Dive

### How It Works
```
right expands freely (right++)
left shrinks ONLY when constraint is violated

Two sub-modes:
  MAXIMIZE: find longest window satisfying condition
  MINIMIZE: find shortest window satisfying condition
```

### The Two Sub-Modes in Detail

#### Mode A: MAXIMIZE — Longest Valid Window
**Pattern:** Expand right. When constraint breaks, shrink from left until valid again. Answer = `right - left + 1` at each step.

```
While (right < n):
    Add nums[right] to window
    While (window is INVALID):
        Remove nums[left] from window
        left++
    Update answer = max(answer, right - left + 1)
    right++
```

#### Mode B: MINIMIZE — Shortest Valid Window
**Pattern:** Expand right. When constraint is *met*, try to shrink from left as much as possible. Answer updated inside the shrink loop.

```
While (right < n):
    Add nums[right] to window
    While (window is VALID):            ← opposite condition!
        Update answer = min(answer, right - left + 1)
        Remove nums[left] from window
        left++
    right++
```

### ⚠️ Key Difference: Where to Update Answer
| Goal | Update answer | Condition to shrink |
|---|---|---|
| Maximize length | **Outside** inner while (after shrinking) | Window is invalid |
| Minimize length | **Inside** inner while (while valid) | Window is valid |

---

## 7. The Master Templates (Java)

### Template 1 — Fixed Window
```java
public int fixedWindow(int[] nums, int k) {
    int left = 0;
    int windowState = 0;     // could be sum, count, etc.
    int result = 0;

    for (int right = 0; right < nums.length; right++) {
        // 1. Expand: incorporate nums[right]
        windowState += nums[right];

        // 2. Check if window reached size k
        if (right - left + 1 == k) {
            // 3. Record result
            result = Math.max(result, windowState);

            // 4. Shrink: remove nums[left]
            windowState -= nums[left];
            left++;
        }
    }
    return result;
}
```

### Template 2 — Variable Window, Maximize Length
```java
public int variableWindowMaximize(int[] nums) {
    int left = 0;
    int result = 0;
    // window state: HashMap, int[], counter, etc.
    Map<Integer, Integer> freq = new HashMap<>();

    for (int right = 0; right < nums.length; right++) {
        // 1. Expand: add nums[right] to window
        freq.merge(nums[right], 1, Integer::sum);

        // 2. Shrink while INVALID
        while (!isValid(freq)) {           // define your own isValid()
            freq.merge(nums[left], -1, Integer::sum);
            if (freq.get(nums[left]) == 0) freq.remove(nums[left]);
            left++;
        }

        // 3. Record: window is now valid, update max
        result = Math.max(result, right - left + 1);
    }
    return result;
}
```

### Template 3 — Variable Window, Minimize Length
```java
public int variableWindowMinimize(int[] nums, int target) {
    int left = 0;
    int result = Integer.MAX_VALUE;
    int windowSum = 0;

    for (int right = 0; right < nums.length; right++) {
        // 1. Expand: add nums[right]
        windowSum += nums[right];

        // 2. Shrink while VALID (minimize)
        while (windowSum >= target) {
            // 3. Record inside the shrink loop
            result = Math.min(result, right - left + 1);
            windowSum -= nums[left];
            left++;
        }
    }
    return result == Integer.MAX_VALUE ? 0 : result;
}
```

### Template 4 — Variable Window with "At Most K" Trick
> **Used for:** "Exactly K" problems, solved as `atMost(k) - atMost(k-1)`

```java
// "Subarrays with exactly K distinct integers"
public int exactlyK(int[] nums, int k) {
    return atMost(nums, k) - atMost(nums, k - 1);
}

private int atMost(int[] nums, int k) {
    int left = 0, count = 0;
    Map<Integer, Integer> freq = new HashMap<>();

    for (int right = 0; right < nums.length; right++) {
        freq.merge(nums[right], 1, Integer::sum);

        while (freq.size() > k) {
            freq.merge(nums[left], -1, Integer::sum);
            if (freq.get(nums[left]) == 0) freq.remove(nums[left]);
            left++;
        }
        count += right - left + 1;  // all subarrays ending at right
    }
    return count;
}
```

---

## 8. All Patterns with Problems & Code

---

### Pattern 1: Fixed Window — Sum / Average

**Cues:** "subarray of size k", "consecutive k elements", "sliding average"

#### Max Sum of Size K Subarray
```java
// Already shown above — see Section 5, Problem 1
```

#### Sliding Window Maximum (Deque-based, LC 239)
> Hardest fixed-window problem. Uses a **monotonic deque**.

```java
public int[] maxSlidingWindow(int[] nums, int k) {
    int n = nums.length;
    int[] result = new int[n - k + 1];
    Deque<Integer> deque = new ArrayDeque<>(); // stores INDICES, front = max

    for (int right = 0; right < n; right++) {
        // Remove indices outside the window
        while (!deque.isEmpty() && deque.peekFirst() < right - k + 1)
            deque.pollFirst();

        // Maintain decreasing order: remove smaller elements from back
        while (!deque.isEmpty() && nums[deque.peekLast()] < nums[right])
            deque.pollLast();

        deque.offerLast(right);

        // Window is full
        if (right >= k - 1)
            result[right - k + 1] = nums[deque.peekFirst()];
    }
    return result;
}
// Time: O(n) — each element added/removed from deque at most once
// Space: O(k) for the deque
```

> 💡 **Monotonic Deque intuition:** The deque always holds candidates for the maximum, in decreasing order. Smaller elements behind a larger one can NEVER be the max while that larger element is in the window — so discard them.

---

### Pattern 2: Fixed Window — Character Frequency / Anagram

**Cues:** "anagram", "permutation of string", "rearrangement"

#### Permutation in String (LC 567)
```java
public boolean checkInclusion(String s1, String s2) {
    if (s1.length() > s2.length()) return false;
    int[] need = new int[26], have = new int[26];
    int k = s1.length();

    for (char c : s1.toCharArray()) need[c - 'a']++;
    for (int i = 0; i < k; i++) have[s2.charAt(i) - 'a']++;
    if (Arrays.equals(need, have)) return true;

    for (int i = k; i < s2.length(); i++) {
        have[s2.charAt(i) - 'a']++;
        have[s2.charAt(i - k) - 'a']--;
        if (Arrays.equals(need, have)) return true;
    }
    return false;
}
// Time: O(n), Space: O(1)
```

---

### Pattern 3: Variable Window — Longest With Constraint

**Cues:** "longest substring", "at most k replacements", "at most k distinct"

#### Longest Substring Without Repeating Characters (LC 3)
```java
public int lengthOfLongestSubstring(String s) {
    Map<Character, Integer> lastSeen = new HashMap<>();
    int left = 0, result = 0;

    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        // Jump left past the duplicate if it's inside current window
        if (lastSeen.containsKey(c) && lastSeen.get(c) >= left)
            left = lastSeen.get(c) + 1;

        lastSeen.put(c, right);
        result = Math.max(result, right - left + 1);
    }
    return result;
}
// Time: O(n), Space: O(min(n, alphabet))

// ALTERNATIVE — with frequency map (more general template):
public int lengthOfLongestSubstringV2(String s) {
    int[] freq = new int[128];
    int left = 0, result = 0;

    for (int right = 0; right < s.length(); right++) {
        freq[s.charAt(right)]++;
        while (freq[s.charAt(right)] > 1) {   // constraint broken
            freq[s.charAt(left++)]--;
        }
        result = Math.max(result, right - left + 1);
    }
    return result;
}
```

#### Longest Substring with At Most K Distinct Characters (LC 340)
```java
public int lengthOfLongestSubstringKDistinct(String s, int k) {
    Map<Character, Integer> freq = new HashMap<>();
    int left = 0, result = 0;

    for (int right = 0; right < s.length(); right++) {
        freq.merge(s.charAt(right), 1, Integer::sum);

        while (freq.size() > k) {              // too many distinct
            char lc = s.charAt(left++);
            freq.merge(lc, -1, Integer::sum);
            if (freq.get(lc) == 0) freq.remove(lc);
        }
        result = Math.max(result, right - left + 1);
    }
    return result;
}
// Time: O(n), Space: O(k)
```

#### Longest Repeating Character Replacement (LC 424)
> Tricky. Key insight: `windowLen - maxFreq <= k` means we need at most k replacements.

```java
public int characterReplacement(String s, int k) {
    int[] freq = new int[26];
    int left = 0, maxFreq = 0, result = 0;

    for (int right = 0; right < s.length(); right++) {
        freq[s.charAt(right) - 'A']++;
        maxFreq = Math.max(maxFreq, freq[s.charAt(right) - 'A']);

        // Window size - most frequent char count > k → invalid
        int windowLen = right - left + 1;
        if (windowLen - maxFreq > k) {
            freq[s.charAt(left) - 'A']--;
            left++;
            // Note: maxFreq is NOT decreased — we never shrink the answer window
        }
        result = Math.max(result, right - left + 1);
    }
    return result;
}
// Time: O(n), Space: O(1)
```

> ⚠️ **Why don't we update maxFreq when shrinking?** Because we only care if we can find a *bigger* window than our current best. If maxFreq were to decrease, the window stays the same size — it can't make `result` worse. This is a subtle but intentional optimization.

#### Longest Subarray of 1s After Deleting One Element (LC 1493)
```java
public int longestSubarray(int[] nums) {
    int left = 0, zeros = 0, result = 0;

    for (int right = 0; right < nums.length; right++) {
        if (nums[right] == 0) zeros++;

        while (zeros > 1) {               // at most 1 zero allowed
            if (nums[left] == 0) zeros--;
            left++;
        }
        result = Math.max(result, right - left); // right - left (not +1) because we delete one
    }
    return result;
}
```

#### Max Consecutive Ones III (LC 1004) — At most K zeros
```java
public int longestOnes(int[] nums, int k) {
    int left = 0, zeros = 0, result = 0;

    for (int right = 0; right < nums.length; right++) {
        if (nums[nums[right] == 0) zeros++;

        while (zeros > k) {
            if (nums[left] == 0) zeros--;
            left++;
        }
        result = Math.max(result, right - left + 1);
    }
    return result;
}
```

---

### Pattern 4: Variable Window — Minimum Length

**Cues:** "minimum length subarray", "minimum window", "shortest subarray"

#### Minimum Size Subarray Sum (LC 209)
```java
public int minSubArrayLen(int target, int[] nums) {
    int left = 0, windowSum = 0;
    int result = Integer.MAX_VALUE;

    for (int right = 0; right < nums.length; right++) {
        windowSum += nums[right];

        while (windowSum >= target) {             // valid: try to shrink
            result = Math.min(result, right - left + 1);
            windowSum -= nums[left++];
        }
    }
    return result == Integer.MAX_VALUE ? 0 : result;
}
// Time: O(n), Space: O(1)
```

#### Minimum Window Substring (LC 76) — HARD
> Classic hard problem. Two frequency maps: `need` (from t) and `have` (from window).

```java
public String minWindow(String s, String t) {
    if (s.isEmpty() || t.isEmpty()) return "";

    Map<Character, Integer> need = new HashMap<>();
    for (char c : t.toCharArray()) need.merge(c, 1, Integer::sum);

    int required = need.size();   // number of UNIQUE chars needed
    int formed = 0;               // number of unique chars currently satisfied
    Map<Character, Integer> have = new HashMap<>();

    int left = 0, minLen = Integer.MAX_VALUE, minLeft = 0;

    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        have.merge(c, 1, Integer::sum);

        // Check if current char satisfies its requirement
        if (need.containsKey(c) && have.get(c).equals(need.get(c)))
            formed++;

        // Try to shrink while window is valid
        while (formed == required) {
            if (right - left + 1 < minLen) {
                minLen = right - left + 1;
                minLeft = left;
            }
            char lc = s.charAt(left++);
            have.merge(lc, -1, Integer::sum);
            if (need.containsKey(lc) && have.get(lc) < need.get(lc))
                formed--;
        }
    }
    return minLen == Integer.MAX_VALUE ? "" : s.substring(minLeft, minLeft + minLen);
}
// Time: O(|s| + |t|), Space: O(|s| + |t|)
```

> 💡 **Key trick:** Track `formed` — how many unique characters in `t` have been satisfied (count in window ≥ count in t). This avoids comparing full maps every step. Only when `formed == required` is the window valid.

---

### Pattern 5: Count Subarrays (At Most K Trick)

**Cues:** "number of subarrays with exactly K", "count subarrays where…"

#### Subarrays with K Different Integers (LC 992) — HARD
```java
public int subarraysWithKDistinct(int[] nums, int k) {
    return atMost(nums, k) - atMost(nums, k - 1);
}

private int atMost(int[] nums, int k) {
    Map<Integer, Integer> freq = new HashMap<>();
    int left = 0, count = 0;

    for (int right = 0; right < nums.length; right++) {
        freq.merge(nums[right], 1, Integer::sum);

        while (freq.size() > k) {
            freq.merge(nums[left], -1, Integer::sum);
            if (freq.get(nums[left]) == 0) freq.remove(nums[left]);
            left++;
        }
        count += right - left + 1;  // all valid subarrays ending at right
    }
    return count;
}
// Time: O(n), Space: O(n)
```

> 💡 **Why `count += right - left + 1`?** Every right endpoint can pair with any left in `[left, right]` to form a valid subarray. The number of such subarrays = `right - left + 1`.

#### Number of Subarrays with Bounded Maximum (LC 795)
```java
// Count subarrays where max is in [left, right] range
public int numSubarrayBoundedMax(int[] nums, int left, int right) {
    return count(nums, right) - count(nums, left - 1);
}

private int count(int[] nums, int bound) {
    int result = 0, cur = 0;
    for (int num : nums) {
        cur = (num <= bound) ? cur + 1 : 0;
        result += cur;
    }
    return result;
}
```

---

### Pattern 6: Two Pointers vs Sliding Window

> ⚠️ **Don't confuse them!** They look similar but differ in purpose.

| | Two Pointers | Sliding Window |
|---|---|---|
| Pointers on | Same array or two arrays | Same array (subarray) |
| Direction | Often opposite (L←→R) | Always same direction (→→) |
| Goal | Find a pair/triplet | Find subarray |
| Input sorted? | Often needs sorting | Doesn't require sorting |
| Window contiguous? | Not necessarily | Always contiguous |

**Example:** "Two Sum II" → Two Pointers. "Max sum subarray" → Sliding Window.

---

### Pattern 7: Sliding Window with Product

#### Subarray Product Less Than K (LC 713)
> Window where product < k. Count all valid subarrays.

```java
public int numSubarrayProductLessThanK(int[] nums, int k) {
    if (k <= 1) return 0;
    int left = 0, product = 1, count = 0;

    for (int right = 0; right < nums.length; right++) {
        product *= nums[right];

        while (product >= k) {
            product /= nums[left++];
        }
        count += right - left + 1;  // subarrays ending at right
    }
    return count;
}
// Time: O(n), Space: O(1)
```

---

### Pattern 8: Sliding Window on Strings — Advanced

#### Minimum Window with All Distinct Characters
```java
// Variant: window must contain all chars of pattern
// → Same as Minimum Window Substring (Pattern 4)
```

#### Longest Substring with At Most 2 Distinct Characters (LC 159)
```java
public int lengthOfLongestSubstringTwoDistinct(String s) {
    return lengthOfLongestSubstringKDistinct(s, 2); // reuse Pattern 3
}
```

#### Check if String Contains All Binary Codes of Size K (LC 1461)
```java
public boolean hasAllCodes(String s, int k) {
    int need = 1 << k;          // 2^k unique codes needed
    Set<String> seen = new HashSet<>();

    for (int i = 0; i + k <= s.length(); i++) {
        seen.add(s.substring(i, i + k));
        if (seen.size() == need) return true;
    }
    return false;
}
```

---

### Pattern 9: Circular Array Window
> **Trick:** Double the array. Run standard sliding window on array of length 2n.

```java
// Max sum circular subarray of size k
public int maxCircularSum(int[] nums, int k) {
    int n = nums.length;
    int[] doubled = new int[2 * n];
    for (int i = 0; i < 2 * n; i++) doubled[i] = nums[i % n];

    // Now run fixed-window of size k on doubled, but window can't span full circle
    int windowSum = 0, maxSum = Integer.MIN_VALUE;
    for (int i = 0; i < k; i++) windowSum += doubled[i];
    maxSum = windowSum;

    for (int i = k; i < n + k; i++) {  // only up to n+k to avoid full wrap
        windowSum += doubled[i] - doubled[i - k];
        maxSum = Math.max(maxSum, windowSum);
    }
    return maxSum;
}
```

---

### Pattern 10: Sliding Window with Prefix Sum Optimization
> When condition involves **running difference** or **sum ≥ target** with negatives.

#### Shortest Subarray with Sum ≥ K (LC 862) — Negative Numbers!
> Sliding window FAILS here (negatives). Use prefix sum + monotonic deque.

```java
public int shortestSubarray(int[] nums, int k) {
    int n = nums.length;
    long[] prefix = new long[n + 1];
    for (int i = 0; i < n; i++) prefix[i + 1] = prefix[i] + nums[i];

    Deque<Integer> deque = new ArrayDeque<>(); // indices into prefix
    int result = Integer.MAX_VALUE;

    for (int i = 0; i <= n; i++) {
        // Try to find valid subarray ending at i
        while (!deque.isEmpty() && prefix[i] - prefix[deque.peekFirst()] >= k) {
            result = Math.min(result, i - deque.pollFirst());
        }
        // Maintain increasing prefix sums in deque
        while (!deque.isEmpty() && prefix[i] <= prefix[deque.peekLast()]) {
            deque.pollLast();
        }
        deque.offerLast(i);
    }
    return result == Integer.MAX_VALUE ? -1 : result;
}
// Time: O(n), Space: O(n)
```

> ⚠️ **When array has negative numbers:** "Variable window sum ≥ k" breaks sliding window because adding a negative right element doesn't expand the sum, and you can't predict when to shrink. Use prefix sum + deque instead.

---

## 9. Data Structures Inside the Window

| Use Case | Data Structure | Why |
|---|---|---|
| Sum / product | `int` variable | O(1) update |
| Character frequency | `int[26]` or `int[128]` | O(1) update, O(1) lookup |
| General frequency | `HashMap<T, Integer>` | O(1) average |
| Distinct count | `HashSet<T>` | O(1) contains |
| Sliding maximum | `ArrayDeque` (monotonic) | O(1) amortized max |
| Sliding median | Two `PriorityQueue`s | O(log k) update |
| Sorted window | `TreeMap` | O(log k) update |

### Sliding Median (LC 480) — Two Heaps
```java
public double[] medianSlidingWindow(int[] nums, int k) {
    double[] result = new double[nums.length - k + 1];
    // maxHeap: lower half, minHeap: upper half
    PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();

    for (int i = 0; i < nums.length; i++) {
        // Add to correct heap
        if (maxHeap.isEmpty() || nums[i] <= maxHeap.peek()) maxHeap.offer(nums[i]);
        else minHeap.offer(nums[i]);

        // Balance
        if (maxHeap.size() > minHeap.size() + 1) minHeap.offer(maxHeap.poll());
        else if (minHeap.size() > maxHeap.size()) maxHeap.offer(minHeap.poll());

        // Window is full
        if (i >= k - 1) {
            result[i - k + 1] = k % 2 == 0
                ? ((double) maxHeap.peek() + minHeap.peek()) / 2
                : maxHeap.peek();

            // Remove outgoing element
            int outgoing = nums[i - k + 1];
            if (outgoing <= maxHeap.peek()) maxHeap.remove(outgoing);
            else minHeap.remove(outgoing);

            // Re-balance after removal
            if (maxHeap.size() > minHeap.size() + 1) minHeap.offer(maxHeap.poll());
            else if (minHeap.size() > maxHeap.size()) maxHeap.offer(minHeap.poll());
        }
    }
    return result;
}
// Time: O(n·k) due to heap remove; O(n log k) with lazy deletion + TreeMap
```

---

## 10. Complexity Cheat Sheet

| Window Type | Time | Space | Notes |
|---|---|---|---|
| Fixed — sum/avg | O(n) | O(1) | Pure arithmetic |
| Fixed — char freq | O(n) | O(1) | Fixed alphabet size |
| Fixed — HashMap | O(n) | O(k) | k = window size |
| Fixed — Monotonic Deque | O(n) | O(k) | Each element touched twice |
| Variable — basic | O(n) | O(1) | Two pointer linear scan |
| Variable — with HashMap | O(n) | O(n) | At most n distinct entries |
| Variable — with TreeMap | O(n log k) | O(k) | Sorted window |
| Variable — two heaps | O(n log k) | O(k) | Sliding median |
| Prefix sum + deque | O(n) | O(n) | Negative numbers case |

> **All sliding window solutions are O(n) time** because each pointer (`left` and `right`) moves at most n steps total, making the two-pointer movement O(n) amortized, regardless of the inner while loop.

---

## 11. Common Mistakes & How to Avoid Them

### Mistake 1: Using Sliding Window with Negative Numbers (Variable Window)
```
❌ Problem: "Min subarray with sum ≥ k" when array has negatives
   Sliding window fails: adding a negative element shrinks the sum,
   so you can't know when to expand vs shrink.

✅ Fix: Use prefix sum + monotonic deque (LC 862)
   Or Kadane's algorithm variant
   Or Binary search on prefix sums
```

### Mistake 2: Off-by-One in Window Size Check
```java
// ❌ WRONG: window becomes size k+1 before recording
if (right - left == k) { ... }   // this is k+1 elements!

// ✅ CORRECT:
if (right - left + 1 == k) { ... }
// OR equivalently:
if (right >= k - 1) { ... }   // when right is 0-indexed
```

### Mistake 3: Forgetting to Shrink in Variable Window
```java
// ❌ WRONG: if instead of while
if (freq.size() > k) {         // only removes one element — might still be invalid
    ...
    left++;
}

// ✅ CORRECT:
while (freq.size() > k) {      // keep removing until valid
    ...
    left++;
}
```

> **Exception:** For "maximize" windows where you only expand window size by 1 each step (LC 424 style), `if` is actually correct by design. Know why before using it.

### Mistake 4: Not Checking `left > right` After Shrinking
```java
// After extreme shrinking, left can pass right
// Make sure your shrink condition prevents this, or add:
while (left <= right && condition) { ... }
```

### Mistake 5: Returning Substring with Wrong Indices
```java
// ❌ WRONG
return s.substring(minLeft, minLeft + minLen - 1);  // off by one

// ✅ CORRECT
return s.substring(minLeft, minLeft + minLen);  // end is exclusive in Java
```

### Mistake 6: Integer Overflow in Sum
```java
// ❌ WRONG: int overflow for large arrays
int sum = 0;
sum += nums[right];  // overflow if nums has large values × large n

// ✅ CORRECT:
long sum = 0;
```

### Mistake 7: Stale `maxFreq` After Shrinking (LC 424)
```
In "Longest Repeating Character Replacement":
maxFreq is INTENTIONALLY not decreased when shrinking.
This is correct — we want the BEST window we've seen so far.
Many candidates write a loop to recompute maxFreq, which is unnecessary
and makes the solution O(26n) instead of O(n).
```

### Mistake 8: Comparing HashMaps with `==`
```java
// ❌ WRONG: reference equality
if (have == need) { ... }

// ✅ CORRECT: structural equality
if (have.equals(need)) { ... }
// OR better: maintain a 'formed' counter (see Minimum Window Substring)
```

### Mistake 9: Forgetting the "Exactly K = AtMost(K) - AtMost(K-1)" Trick
```
When you see: "count subarrays with EXACTLY K distinct"
→ Direct sliding window is hard (can't maintain "exactly")
→ Use: f(exactly k) = f(at most k) - f(at most k-1)
```

### Mistake 10: Not Handling Empty Result
```java
// Always check if result was ever updated
return result == Integer.MAX_VALUE ? 0 : result;
return result == Integer.MAX_VALUE ? "" : s.substring(...);
```

---

## 12. Decision Flowchart

```
Is the input an array or string?
└─ NO  → Probably not sliding window
└─ YES →
    Does the problem involve a contiguous subarray/substring?
    └─ NO  → Probably two pointers, DP, or backtracking
    └─ YES →
        Is the window size fixed (k given)?
        └─ YES → FIXED WINDOW TEMPLATE
                  ├─ Need max/min in window? → Monotonic Deque
                  ├─ Need frequency? → int[26] or HashMap
                  └─ Need median? → Two Heaps / TreeMap

        └─ NO  → Is the array ALL non-negative?
                  └─ YES →
                      Do you want LONGEST valid window?
                      └─ YES → VARIABLE MAXIMIZE TEMPLATE
                      Do you want SHORTEST valid window?
                      └─ YES → VARIABLE MINIMIZE TEMPLATE
                      Do you want COUNT of valid windows?
                      └─ YES → AtMost(K) - AtMost(K-1) TRICK

                  └─ NO (has negatives) →
                      Sum-based condition?
                      └─ YES → PREFIX SUM + MONOTONIC DEQUE
                      Frequency/distinct-based condition?
                      └─ YES → Standard variable window still works!
                                (negatives only hurt sum-based conditions)
```

---

## 13. Edge Cases Checklist

Before finalizing your solution, verify these:

- [ ] **Empty array/string** — `nums.length == 0` or `s.isEmpty()`
- [ ] **Single element** — window of size 1, k=1 edge
- [ ] **k > n** — window larger than array (return 0, -1, or "")
- [ ] **All same elements** — frequency map has one entry
- [ ] **All distinct elements** — frequency map is full
- [ ] **Window never becomes valid** — result stays `Integer.MAX_VALUE`
- [ ] **Entire array is the answer** — left stays at 0
- [ ] **Negative numbers** — sum can decrease; use long; check if SW works
- [ ] **Integer overflow** — use `long` for products and large sums
- [ ] **Unicode vs ASCII** — use `int[128]` or `HashMap` (not `int[26]`) if chars beyond a-z
- [ ] **Left crosses right** — `left > right` after aggressive shrinking

---

## 14. Interview Communication Guide

### How to Verbalize Your Thought Process

**Step 1 — Recognize the pattern:**
> "This asks for the longest/shortest contiguous subarray with some property, which suggests a sliding window approach."

**Step 2 — Identify window type:**
> "The window size isn't fixed, so I'll use a variable window. Since I'm maximizing length, I'll expand right and shrink from left only when the constraint is violated."

**Step 3 — Define validity:**
> "My window is valid when [condition]. I'll track [data structure] to check this in O(1)."

**Step 4 — State the invariant:**
> "At every step, `[left, right]` is the longest valid window ending at `right`."

**Step 5 — Walk through example:**
> "Let me trace through `[a, b, c, a, b]` with k=2…"

**Step 6 — State complexity:**
> "Each element is added and removed from the window at most once, so it's O(n) time. The HashMap holds at most k entries, so O(k) space — and since k ≤ n, that's O(n) worst case."

### Questions the Interviewer Might Ask

| Question | Answer |
|---|---|
| "Can the window size be 0?" | Handle empty array edge case |
| "What if k > n?" | Return 0 or appropriate default |
| "Why is this O(n) and not O(n²)?" | Each pointer moves at most n steps; inner while is amortized |
| "Can you do it without extra space?" | For freq: use int[26] instead of HashMap |
| "What if there are duplicates?" | Frequency map handles it — count, don't just store presence |
| "Does it work with negative numbers?" | Depends — if sum-based, no; if frequency-based, yes |

---

## 15. Quick-Fire Problem → Pattern Mapping

| Problem | Pattern | Key Trick |
|---|---|---|
| Max sum subarray of size k | Fixed | Sum += right, -= left |
| Max average subarray size k | Fixed | Same, divide at end |
| Sliding window max | Fixed + Deque | Monotonic decreasing deque |
| Sliding window median | Fixed + Heaps | Two heaps, lazy deletion |
| Find all anagrams | Fixed + freq | int[26] comparison |
| Permutation in string | Fixed + freq | int[26], formed counter |
| Longest no-repeat substring | Variable max | HashMap last-seen index |
| Longest with K distinct | Variable max | freq.size() <= k |
| Longest ones with K flips | Variable max | zeros count <= k |
| Longest char replacement | Variable max | len - maxFreq <= k |
| Min size subarray sum | Variable min | sum >= target → shrink |
| Minimum window substring | Variable min | formed == required |
| Exactly K distinct subarrays | AtMost trick | atMost(k) - atMost(k-1) |
| Product less than K | Variable + count | product < k, count += right-left+1 |
| Shortest subarray sum K (negatives) | Prefix + Deque | Monotonic prefix sum deque |
| Contains duplicate within K | Fixed + Set | HashSet of size k |
| Max consecutive 1s after delete | Variable max | At most 1 zero |

---

## 🔖 One-Page Quick Reference

```
╔══════════════════════════════════════════════════════════════════════╗
║                    SLIDING WINDOW QUICK REF                         ║
╠══════════════════════════════════════════════════════════════════════╣
║  FIXED (size k):                                                     ║
║    for right in 0..n:                                                ║
║      add nums[right]                                                 ║
║      if right >= k-1:                                                ║
║        record answer                                                 ║
║        remove nums[left++]                                           ║
╠══════════════════════════════════════════════════════════════════════╣
║  VARIABLE — MAXIMIZE:                                                ║
║    for right in 0..n:                                                ║
║      add nums[right]                                                 ║
║      while INVALID: remove nums[left++]                              ║
║      result = max(result, right - left + 1)  ← OUTSIDE while        ║
╠══════════════════════════════════════════════════════════════════════╣
║  VARIABLE — MINIMIZE:                                                ║
║    for right in 0..n:                                                ║
║      add nums[right]                                                 ║
║      while VALID:                                                    ║
║        result = min(result, right - left + 1) ← INSIDE while        ║
║        remove nums[left++]                                           ║
╠══════════════════════════════════════════════════════════════════════╣
║  EXACTLY K = atMost(k) - atMost(k-1)                                ║
║  count += right - left + 1  (subarrays ending at right)             ║
╠══════════════════════════════════════════════════════════════════════╣
║  NEGATIVES + SUM ≥ K → Prefix Sum + Monotonic Deque                 ║
║  SLIDING MAX → Monotonic Decreasing Deque (store indices)           ║
║  SLIDING MEDIAN → Two Heaps (max for lower, min for upper half)     ║
╚══════════════════════════════════════════════════════════════════════╝
```

---

*Last revised: 2026 | Patterns cover LeetCode Easy → Hard | Java throughout*
