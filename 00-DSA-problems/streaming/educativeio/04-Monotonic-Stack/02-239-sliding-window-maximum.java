/**
 * 0. ## Problem Statement
 * You are given an array of integers `nums` and a sliding window of size `k` moving from
 * the very left of the array to the very right, one position at a time.
 * You can only see the `k` numbers currently inside the window.
 * Return an array of the maximum value in each window position (length `n - k + 1`).
 *
 * Example:
 *   nums = [1, 3, -1, -3, 5, 3, 6, 7], k = 3
 *   Output: [3, 3, 5, 5, 6, 7]
 *
 * Constraints:
 * - 1 <= nums.length <= 10^5
 * - -10^4 <= nums[i] <= 10^4
 * - 1 <= k <= nums.length
 *
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "Can `k` be 0, negative, or larger than `nums.length`?"
 *    -> Prevents invalid output array sizing (`n - k + 1` would be negative or > `n`) and empty-window edge cases.
 * 2. "Can `nums` contain negative numbers or full 32-bit `Integer.MIN_VALUE` / `Integer.MAX_VALUE`?"
 *    -> Prevents initializing a window max to `0` (bug on all-negative arrays) and prevents integer overflow in `b - a` heap comparators.
 * 3. "Is the entire array available upfront in memory, or do elements arrive one-by-one as an online stream?"
 *    -> Rules in/out offline block-partitioning (Prefix/Suffix Max requires knowing `n` and scanning right-to-left) vs. the online Deque approach.
 * 4. "Are we allowed to mutate the input array `nums`, or must it be treated as read-only?"
 *    -> Clarifies whether we can reuse input space or must allocate strictly separate auxiliary structures.
 * 5. "How should duplicate maximum values inside the same window be handled when one of them expires?"
 *    -> Critical for correctness: if `[5, 5]` is in the window and the first `5` slides out, the second `5` must still be recognized as the maximum.
 *
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: `n <= 10^5` and `k` can be up to `n` (e.g., `k = 50,000`).
 * Any approach that does O(k) work per window takes `(n - k + 1) * k ≈ 2.5 * 10^9` operations,
 * which falls over (TLE). We need O(1) amortized (or O(log k)) work per slide.
 *
 * --- Approach 1: Brute Force Window Scan ---
 * 0. Idea/Intuition: For each of the `n - k + 1` window start positions `i`, loop over
 *    `nums[i .. i + k - 1]` and track the maximum.
 * 1. What I'd naturally try: Direct translation of the problem statement using two nested loops.
 * 2. Why it works: Every window is explicitly inspected from scratch, so no stale values can leak in.
 * 3. Why it's too slow/costly: When `n = 10^5` and `k = 5 * 10^4`, we perform ~2.5 billion
 *    comparisons, blowing past the ~10^8 ops/sec budget.
 * 4. What work is being repeated: Consecutive windows `[i .. i+k-1]` and `[i+1 .. i+k]` share
 *    `k - 1` identical elements! We re-compare those same `k - 1` elements every single step.
 * 5. Observation that removes the bottleneck: Instead of rescanning all `k` elements, maintain
 *    a dynamic data structure that supports "insert incoming element", "evict outgoing element",
 *    and "query current maximum" faster than O(k).
 * 6. Leads to Approach 2: Put window elements into a Max-Heap (`PriorityQueue`) so the maximum
 *    is always at the top.
 *    Time Complexity: O(N * K) — because for each of the `N - K + 1` windows, we scan all `K` elements.
 *    Space Complexity: O(1) auxiliary — because we only keep a scalar `windowMax` variable
 *    (excluding the mandatory `N - K + 1` output array).
 *
 * --- Approach 2: Max-Heap with Lazy Deletion ---
 * 0. Idea/Intuition: Store `(value, index)` pairs in a Max-Heap. On each step `i`, push
 *    `(nums[i], i)`. Before reading the maximum, check the heap's root: if its index is `<= i - k`
 *    (outside the window), pop it and repeat until the root is inside the window.
 * 1. What I'd naturally try: "I need the max of a changing set -> use a Max-Heap!" Since
 *    `PriorityQueue.remove(val)` is O(k), lazily discard expired elements only when they reach the top.
 * 2. Why it works: Expired elements buried deeper in the heap cannot affect the answer as long
 *    as a valid larger element sits above them; the moment an expired element surfaces to the root,
 *    the `while` loop discards it.
 * 3. Why it's still suboptimal: Every push/pop costs O(log N), and on a strictly decreasing
 *    array `[N, N-1, ..., 1]`, expired elements never get popped until they surface, or even if
 *    you use a `TreeMap` (O(N log K) time, O(K) space), you still pay a `log` factor per element.
 * 4. What work is being repeated: The heap carefully maintains a sorted hierarchy of *every*
 *    element in the window — even elements that are strictly useless!
 *    Suppose the window has `[..., 3, 5]`. Can `3` EVER be the maximum of this window or any
 *    future window? NO! `5` is both LARGER than `3` AND NEWER than `3` (so `5` will stay in
 *    the window longer than `3`). Keeping `3` in the heap wastes both memory and log-time work.
 * 5. Observation that removes the bottleneck: Whenever a new element `nums[i]` enters the window,
 *    every earlier element in the window that is `<= nums[i]` is permanently dominated (both
 *    smaller/equal AND expires sooner). We can immediately discard them!
 * 6. Leads to Approach 3: If we discard all dominated elements from the back as new ones arrive,
 *    the surviving candidates are ALWAYS in strictly decreasing order of value (and strictly
 *    increasing order of index). The oldest survivor — at the FRONT — is automatically the maximum!
 *    We only need O(1) push/pop at the back and O(1) peek/pop at the front -> a **Deque**.
 *    Time Complexity: O(N log N) — because each of the N elements is pushed once and popped at
 *    most once from a heap that can grow up to size N (e.g., strictly decreasing input).
 *    Space Complexity: O(N) — because lazy deletion can leave up to N - K stale entries sitting
 *    below the root in the `PriorityQueue`.
 *
 * --- Approach 3: Monotonic Decreasing Deque (Optimal) ---
 * 0. Idea/Intuition: Maintain a `Deque<Integer>` of **indices** whose corresponding values in
 *    `nums` are strictly decreasing from front to back. For each index `i`:
 *    (a) If the front index is `<= i - k`, pop it from the front (it slid out of the window).
 *    (b) While the back's value is `<= nums[i]`, pop it from the back (dominated forever).
 *    (c) Push `i` to the back.
 *    (d) Once `i >= k - 1`, `nums[deque.peekFirst()]` is the window maximum.
 * 1. Why someone thinks of it: It's the exact data structure forced by the two operations we need:
 *    evict expired candidates from the oldest end (front), and evict dominated candidates from
 *    the newest end (back).
 * 2. Why it works: Two invariants hold after every iteration `i`:
 *    - Every index in the deque is within `[i - k + 1, i]` (valid window).
 *    - Values are strictly decreasing from front to back, and no un-dominated candidate was ever
 *      dropped. Therefore, the front index MUST hold the maximum of `[i - k + 1, i]`.
 *    Time Complexity: O(N) — because each of the N indices enters the deque once (at the back)
 *    and leaves at most once (either from the front on expiration or from the back on domination),
 *    yielding at most 2N deque operations total.
 *    Space Complexity: O(K) auxiliary — because the deque never holds more than K indices at any
 *    moment (the window size caps surviving indices).
 *
 * --- Approach 4: Block Partitioning / Prefix-Suffix Max DP (Distinct O(N) Alternative) ---
 * 0. Idea/Intuition: Divide `nums` into fixed blocks of size `k`: `[0..k-1]`, `[k..2k-1]`, ...
 *    Precompute two arrays:
 *    - `leftMax[i]`: max from the start of `i`'s block up to `i`.
 *    - `rightMax[i]`: max from `i` to the end of `i`'s block.
 *    Any sliding window `[i, i + k - 1]` either matches one block exactly or straddles the
 *    boundary between two adjacent blocks — split into a suffix of the left block (`rightMax[i]`)
 *    and a prefix of the right block (`leftMax[i + k - 1]`).
 *    So `windowMax = Math.max(rightMax[i], leftMax[i + k - 1])`.
 * 1. Why it works: Every element in `[i, i + k - 1]` lies either in the suffix starting at `i`
 *    or the prefix ending at `i + k - 1`, and neither extends outside the window!
 * 2. Trade-off vs. Deque: Zero dynamic pointer structures (pure primitive arrays, super cache-
 *    friendly and trivially parallelizable), BUT it is offline-only (must know the whole array
 *    upfront) and uses O(N) auxiliary space instead of O(K).
 *    Time Complexity: O(N) — two linear passes over `nums` (can be fused into one loop + one output loop).
 *    Space Complexity: O(N) auxiliary — for the `leftMax` and `rightMax` arrays of length N.
 *
 * Interview Verdict:
 * Write Approach 3 (Monotonic Decreasing Deque) in a 45-minute interview. It achieves optimal
 * O(N) time AND optimal O(K) space, works online on streams, is ~15 lines of clean code, and
 * directly demonstrates the monotonic deque pattern interviewers test with this problem.
 * (Briefly mentioning Approach 4 as a cache-friendly offline alternative is a great bonus.)
 *
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - `k = 1`: Every element is its own window -> output is an exact copy of `nums`.
 * - `k = nums.length`: Only 1 window covering the whole array -> output has length 1 (global max).
 * - All negative numbers (`[-7, -2, -9, -1]`, `k = 2`): Must return `[-2, -2, -1]` (never `0`).
 * - All equal / duplicate maxes (`[5, 5, 5, 5]`, `k = 2`): Popping `<=` from the back keeps the
 *   *newest* duplicate `5`, which expires later — ensuring the max stays `5` as the window slides.
 * - Strictly increasing (`[1, 2, 3, 4, 5]`, `k = 3`): Deque size stays 1; every new element wipes
 *   the previous one.
 * - Strictly decreasing (`[5, 4, 3, 2, 1]`, `k = 3`): Nothing is ever popped from the back;
 *   every eviction happens at the front (stress-tests the expiration check `peekFirst() <= i - k`).
 * - Max sits at the exact expiring index (`[9, 1, 2, 3]`, `k = 3`): At `i = 3`, index `0` (`9`)
 *   must be evicted BEFORE recording the max, yielding `[9, 3]` (not `[9, 9]`).
 *
 * =========================================================================================
 * 4. ## Also include
 *
 * -- Key insight --
 * Question to ask yourself: "When a new element enters my window, which older elements inside
 * the window have ZERO chance of ever being the maximum again?"
 * Correctness intuition: If `j > i` and `nums[j] >= nums[i]`, then for every future window that
 * contains `i`, it MUST also contain `j` (since `j` arrived later and expires later). Since
 * `nums[j]` is at least as large as `nums[i]`, `nums[i]` can never beat `nums[j]` in any window.
 * Deleting `i` immediately is 100% lossless.
 *
 * -- Examples & ASCII Diagram --
 * Typical case: nums = [1, 3, -1, -3, 5, 3, 6, 7], k = 3
 *
 * Window sliding + Monotonic Decreasing Deque (showing `index:value`, front -> back):
 *
 *   i=0 (val= 1): [1]  3  -1  -3  5  3  6  7   | Deque: [0:1]
 *   i=1 (val= 3): [1   3] -1  -3  5  3  6  7   | 3 >= 1 -> pop back 0:1 | Deque: [1:3]
 *   i=2 (val=-1): [1   3  -1] -3  5  3  6  7   | Deque: [1:3, 2:-1]           -> max = 3
 *   i=3 (val=-3):  1  [3  -1  -3] 5  3  6  7   | Deque: [1:3, 2:-1, 3:-3]     -> max = 3
 *   i=4 (val= 5):  1   3 [-1  -3  5] 3  6  7   | front 1 <= 4-3 -> pop front!
 *                                              | 5 >= -3, -1 -> pop back!
 *                                              | Deque: [4:5]                 -> max = 5
 *   i=5 (val= 3):  1   3  -1 [-3  5  3] 6  7   | Deque: [4:5, 5:3]            -> max = 5
 *   i=6 (val= 6):  1   3  -1  -3 [5  3  6] 7   | 6 >= 3, 5 -> pop back!
 *                                              | Deque: [6:6]                 -> max = 6
 *   i=7 (val= 7):  1   3  -1  -3  5 [3  6  7]  | 7 >= 6 -> pop back!
 *                                              | Deque: [7:7]                 -> max = 7
 *
 * -- Dry run 1: Optimal Deque on Typical Case [1, 3, -1, -3, 5, 3, 6, 7], k = 3 --
 *   i | nums[i] | Front Evict (<= i-3) | Back Pops (<= nums[i]) | Deque After (idx:val)     | Output
 *   --+---------+----------------------+------------------------+---------------------------+-------
 *   0 |    1    | none                 | none                   | [0:1]                     | -
 *   1 |    3    | none                 | 0:1                    | [1:3]                     | -
 *   2 |   -1    | none                 | none                   | [1:3, 2:-1]               | 3
 *   3 |   -3    | none (1 > 0)         | none                   | [1:3, 2:-1, 3:-3]         | 3
 *   4 |    5    | 1:3 (1 <= 1)         | 3:-3, 2:-1             | [4:5]                     | 5
 *   5 |    3    | none (4 > 2)         | none                   | [4:5, 5:3]                | 5
 *   6 |    6    | none (4 > 3)         | 5:3, 4:5               | [6:6]                     | 6
 *   7 |    7    | none (6 > 4)         | 6:6                    | [7:7]                     | 7
 *
 * -- Dry run 2: Edge Case — Strictly Decreasing [5, 4, 3, 2, 1], k = 3 --
 * (Exercises front expiration on every single step once full — no back pops ever occur!)
 *   i | nums[i] | Front Evict (<= i-3) | Back Pops | Deque After (idx:val)     | Output
 *   --+---------+----------------------+-----------+---------------------------+-------
 *   0 |    5    | none                 | none      | [0:5]                     | -
 *   1 |    4    | none                 | none      | [0:5, 1:4]                | -
 *   2 |    3    | none                 | none      | [0:5, 1:4, 2:3]           | 5
 *   3 |    2    | 0:5 (0 <= 0)         | none      | [1:4, 2:3, 3:2]           | 4
 *   4 |    1    | 1:4 (1 <= 1)         | none      | [2:3, 3:2, 4:1]           | 3
 *
 * -- Pitfalls --
 * 1. Storing VALUES instead of INDICES in the deque: Tempting because you want max values,
 *    but without indices you can't tell *when* the front element has fallen out of the window
 *    (especially with duplicates like `[5, 1, 5, 2]` where the outgoing value equals the front!).
 * 2. Using `<` instead of `<=` when popping from the back: Technically works if you store indices,
 *    but keeping an older equal value behind a newer equal value wastes deque space and leaves an
 *    unnecessary duplicate that expires sooner. Always pop `<=` so the newer duplicate replaces it.
 * 3. Java-specific gotcha — Comparator overflow in `PriorityQueue`: Writing `(a, b) -> b.val - a.val`
 *    overflows if `b.val = Integer.MAX_VALUE` and `a.val = -1`. Always use `Integer.compare(b.val, a.val)`.
 * 4. Java-specific gotcha — `PriorityQueue.remove(obj)`: Tempting for eager eviction in the heap
 *    approach, but `PriorityQueue.remove(Object)` is O(K) linear scan in Java, degrading the
 *    whole algorithm back to O(N * K) TLE!
 *
 * -- Pattern recognition --
 * When you see: "Fixed/bounded sliding window + need the MAX or MIN at every step in O(1) amortized"
 * -> Think **Monotonic Deque** (decreasing for max, increasing for min), always storing **indices**.
 * Transfers directly to:
 *   - Shortest Subarray with Sum at Least K (LeetCode 862) — monotonic increasing deque of prefix sums
 *   - Jump Game VI (LeetCode 1696) — DP transition `dp[i] = nums[i] + max(dp[i-k .. i-1])`
 *   - Constrained Subsequence Sum (LeetCode 1425) — same DP + sliding window max optimization
 *
 * -- Interview script --
 * "A brute-force scan checks all `k` elements per window, which is O(N * K) and too slow when
 * `N = 10^5`. A max-heap with lazy deletion improves that to O(N log N), but it still keeps
 * elements around that can never be the maximum. The key observation is domination: if a newer
 * element `nums[i]` is greater than or equal to an older element in the window, that older
 * element is both smaller and expires sooner — so it can never be the max of any current or
 * future window. I'll maintain a monotonic decreasing deque of indices: each step, I evict
 * the front if its index is `<= i - k`, pop from the back while `nums[back] <= nums[i]`, push
 * `i` to the back, and read `nums[front]` as the window max. Since each index is pushed once
 * and popped at most once, that gives us O(N) time and O(K) auxiliary space."
 *
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q1: What if we need BOTH the sliding window maximum AND minimum simultaneously (e.g., to check
 *     if `max - min <= limit`, like LeetCode 1438)?
 * A1: Maintain two monotonic deques in parallel — one decreasing (front = max) and one increasing
 *     (front = min). Both still run in O(N) total time and O(K) space.
 *
 * Q2: What if the input is a 2D matrix (`M x N`) and we want the max in every `R x C` submatrix?
 * A2: Decompose into two 1D passes! First run 1D sliding window max of width `C` on every row
 *     (producing an `M x (N - C + 1)` intermediate grid). Then run 1D sliding window max of height
 *     `R` down every column of that intermediate grid. Total time: O(M * N), space: O(M * N).
 *
 * Q3: What if elements arrive as an infinite online stream and we only have `O(K)` memory?
 * A3: The Monotonic Deque works out-of-the-box with a tiny tweak: instead of indexing into an
 *     array `nums[]` (which would grow to O(N)), store `(value, streamIndex)` pairs directly in
 *     the deque. Memory stays strictly O(K) forever, and each `next(val)` is amortized O(1).
 *
 * Q4: What if the window size `k` is not fixed — we get arbitrary range-max queries `[L, R]`?
 * A4: A monotonic deque only works when both window endpoints move monotonically. For arbitrary
 *     `[L, R]` queries on a static array, use a **Sparse Table** (O(N log N) build, O(1) query);
 *     if elements can also be updated dynamically, use a **Segment Tree** (O(log N) query & update).
 * =========================================================================================
 */

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.PriorityQueue;

public class SlidingWindowMaximumStudyNote {

    // ----------------------------------------------------------------------
    // APPROACH 1: Brute Force (O(N * K) time, O(1) auxiliary space)
    // ----------------------------------------------------------------------
    public static int[] maxSlidingWindowBruteForce(int[] nums, int k) {
        int n = nums.length;
        // Total windows for array of length n and window size k is (n - k + 1).
        // Example: n = 8, k = 3 -> 6 windows starting at indices 0, 1, 2, 3, 4, 5.
        int[] result = new int[n - k + 1];

        for (int i = 0; i <= n - k; i++) {
            // Initialize to the first element in the window (never 0, since nums can be negative).
            int windowMax = nums[i];
            for (int j = i + 1; j < i + k; j++) {
                if (nums[j] > windowMax) {
                    windowMax = nums[j];
                }
            }
            result[i] = windowMax;
        }
        return result;
    }

    // ----------------------------------------------------------------------
    // APPROACH 2: Max-Heap with Lazy Deletion (O(N log N) time, O(N) space)
    // ----------------------------------------------------------------------
    private record Entry(int val, int idx) {}

    public static int[] maxSlidingWindowHeap(int[] nums, int k) {
        int n = nums.length;
        int[] result = new int[n - k + 1];

        // Max-heap ordered by value descending. Use Integer.compare to avoid overflow.
        // Example at i=3 for [1,3,-1,-3], k=3: heap root is Entry(3, 1); Entry(1, 0) is still
        // sitting lazily below it until it ever surfaces to the top.
        PriorityQueue<Entry> maxHeap = new PriorityQueue<>((a, b) -> Integer.compare(b.val(), a.val()));

        for (int i = 0; i < n; i++) {
            maxHeap.offer(new Entry(nums[i], i));

            // Wait until the first full window [0 .. k-1] has formed.
            if (i >= k - 1) {
                // Lazy deletion: discard any root element whose index fell outside [i - k + 1, i].
                while (maxHeap.peek().idx() <= i - k) {
                    maxHeap.poll();
                }
                result[i - k + 1] = maxHeap.peek().val();
            }
        }
        return result;
    }

    // ----------------------------------------------------------------------
    // APPROACH 3: Monotonic Decreasing Deque (OPTIMAL: O(N) time, O(K) space)
    // ----------------------------------------------------------------------
    public static int[] maxSlidingWindowDeque(int[] nums, int k) {
        int n = nums.length;
        int[] result = new int[n - k + 1];

        // Stores INDICES (not values) in strictly increasing index order,
        // whose values in `nums` are strictly decreasing from front to back.
        // Example at i=3 for [1, 3, -1, -3], k=3:
        //   deque = [1, 2, 3]  -> corresponding values [3, -1, -3], front (1) is the max!
        Deque<Integer> deque = new ArrayDeque<>();

        for (int i = 0; i < n; i++) {
            // 1. Expiration check: if the oldest surviving index (at the front) has slid
            //    out of the window [i - k + 1, i], evict it.
            //    An `if` suffices because `i` advances by 1 each step, so at most 1 index expires.
            if (!deque.isEmpty() && deque.peekFirst() <= i - k) {
                deque.pollFirst();
            }

            // 2. Domination check: any index at the back whose value is <= nums[i] is BOTH
            //    smaller/equal AND older than `i`, so it can never be the max again. Discard it.
            while (!deque.isEmpty() && nums[deque.peekLast()] <= nums[i]) {
                deque.pollLast();
            }

            // 3. Append current index as the newest surviving candidate.
            deque.offerLast(i);

            // 4. Once we have processed at least k elements, the front of the deque is
            //    guaranteed to be the index of the maximum in [i - k + 1, i].
            if (i >= k - 1) {
                result[i - k + 1] = nums[deque.peekFirst()];
            }
        }
        return result;
    }

    // ----------------------------------------------------------------------
    // APPROACH 4: Block Partitioning / Prefix-Suffix Max (O(N) time, O(N) space)
    // Offline alternative — cache-friendly, no dynamic structures.
    // ----------------------------------------------------------------------
    public static int[] maxSlidingWindowBlockDP(int[] nums, int k) {
        int n = nums.length;
        // Divide `nums` into blocks of size k: [0..k-1], [k..2k-1], ...
        // Example for [1, 3, -1 | -3, 5, 3 | 6, 7], k=3:
        //   leftMax  = [1, 3, 3 | -3, 5, 5 | 6, 7]  (running max left-to-right within each block)
        //   rightMax = [3, 3,-1 |  5, 5, 3 | 7, 7]  (running max right-to-left within each block)
        int[] leftMax = new int[n];
        int[] rightMax = new int[n];

        for (int i = 0; i < n; i++) {
            // Reset running max at the start of every k-sized block (`i % k == 0`).
            leftMax[i] = (i % k == 0) ? nums[i] : Math.max(leftMax[i - 1], nums[i]);

            // Mirror index `j` sweeps right-to-left; reset at the end of every block.
            int j = n - 1 - i;
            rightMax[j] = (j == n - 1 || (j + 1) % k == 0)
                    ? nums[j]
                    : Math.max(rightMax[j + 1], nums[j]);
        }

        int[] result = new int[n - k + 1];
        for (int i = 0; i <= n - k; i++) {
            // Window [i, i + k - 1] splits at the block boundary into:
            //   suffix of left block (`rightMax[i]`) + prefix of right block (`leftMax[i + k - 1]`).
            result[i] = Math.max(rightMax[i], leftMax[i + k - 1]);
        }
        return result;
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS — Cross-checking all 4 approaches on representative + edge cases
    // ----------------------------------------------------------------------
    private static void verifyCase(String name, int[] nums, int k, int[] expected) {
        int[] rBrute = maxSlidingWindowBruteForce(nums, k);
        int[] rHeap = maxSlidingWindowHeap(nums, k);
        int[] rDeque = maxSlidingWindowDeque(nums, k);
        int[] rBlock = maxSlidingWindowBlockDP(nums, k);

        boolean pass = Arrays.equals(rBrute, expected)
                && Arrays.equals(rHeap, expected)
                && Arrays.equals(rDeque, expected)
                && Arrays.equals(rBlock, expected);

        System.out.printf("%-32s | %s | Output: %s%n",
                name, pass ? "PASS" : "FAIL", Arrays.toString(rDeque));

        if (!pass) {
            System.out.println("  Expected: " + Arrays.toString(expected));
            System.out.println("  Brute:    " + Arrays.toString(rBrute));
            System.out.println("  Heap:     " + Arrays.toString(rHeap));
            System.out.println("  Deque:    " + Arrays.toString(rDeque));
            System.out.println("  BlockDP:  " + Arrays.toString(rBlock));
        }
    }

    public static void main(String[] args) {
        verifyCase("LC Example 1",
                new int[]{1, 3, -1, -3, 5, 3, 6, 7}, 3,
                new int[]{3, 3, 5, 5, 6, 7});

        verifyCase("LC Example 2 (k=1, n=1)",
                new int[]{1}, 1,
                new int[]{1});

        verifyCase("Edge: k = 1 (multiple)",
                new int[]{4, -2, 7, 1}, 1,
                new int[]{4, -2, 7, 1});

        verifyCase("Edge: k = n (single window)",
                new int[]{4, -2, 7, 1}, 4,
                new int[]{7});

        verifyCase("Edge: All negative numbers",
                new int[]{-7, -2, -9, -1, -5}, 2,
                new int[]{-2, -2, -1, -1});

        verifyCase("Edge: All equal / duplicates",
                new int[]{5, 5, 5, 5}, 2,
                new int[]{5, 5, 5});

        verifyCase("Edge: Strictly increasing",
                new int[]{1, 2, 3, 4, 5}, 3,
                new int[]{3, 4, 5});

        verifyCase("Edge: Strictly decreasing",
                new int[]{5, 4, 3, 2, 1}, 3,
                new int[]{5, 4, 3});

        verifyCase("Edge: Max at expiring boundary",
                new int[]{9, 1, 2, 3}, 3,
                new int[]{9, 3});
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 *
 * - Core pattern: Monotonic Decreasing Deque (storing indices).
 * - Key observation: Any older element in the window that is <= a newer element is permanently
 *   dominated (smaller/equal AND expires sooner) — discard it from the back immediately.
 * - Worth memorizing vs. understanding: Understand the two-ended eviction rule (front = expired
 *   by index, back = dominated by value); memorize storing INDICES rather than values so
 *   expiration checks are exact even with duplicates.
 * - Most common trap: Storing raw values in the deque (breaks expiration on duplicates), or
 *   calling O(K) `PriorityQueue.remove()` instead of lazy deletion.
 * - One-line mental trigger: "Max/min of every sliding window? Monotonic deque of indices: pop expired front, pop dominated back."
 * =========================================================================================
 */







































import java.util.ArrayDeque;
import java.util.Deque;

class Solution {

    public int[] maxSlidingWindow(int[] nums, int k) {

        int n = nums.length;

        // There are exactly n - k + 1 sliding windows.
        int[] result = new int[n - k + 1];

        // Stores array indices, NOT array values.
        //
        // Invariant:
        // nums[deque[0]] > nums[deque[1]] > ...
        //
        // Therefore, the front always represents the maximum.
        Deque<Integer> deque = new ArrayDeque<>();

        // Tracks the next position in the result array.
        int resultIndex = 0;

        // Process each element exactly once.
        for (int i = 0; i < n; i++) {

            /*
             * STEP 1: Remove expired indices.
             *
             * Current window:
             * [i - k + 1, i]
             *
             * Any index smaller than i - k + 1
             * is outside the current window.
             *
             * Since indices are inserted in increasing order,
             * expired indices can only be at the front.
             */
            int windowStart = i - k + 1;

            while (!deque.isEmpty()
                    && deque.peekFirst() < windowStart) {

                deque.pollFirst();
            }

            /*
             * STEP 2: Maintain decreasing order.
             *
             * Remove elements from the back while their
             * values are <= the current element.
             *
             * Why <= instead of < ?
             *
             * If two values are equal, the newer index is
             * preferable because it remains in the window
             * longer.
             *
             * Example:
             * Existing: [5 at index 0]
             * Current:  [5 at index 2]
             *
             * Index 0 expires earlier, so retain index 2.
             */
            while (!deque.isEmpty()
                    && nums[deque.peekLast()] <= nums[i]) {

                deque.pollLast();
            }

            /*
             * STEP 3: Insert current index.
             *
             * The deque now maintains decreasing values.
             */
            deque.offerLast(i);

            /*
             * STEP 4: Calculate maximum.
             *
             * A complete window exists when i >= k - 1.
             *
             * The front contains the index of the maximum.
             */
            if (i >= k - 1) {

                result[resultIndex++] = nums[deque.peekFirst()];
            }
        }

        return result;
    }
}

/*
TIME COMPLEXITY: O(n) -- OPTIMAL

Why?

The outer loop:
- Runs exactly n times.

Deque operations:
- Every index is inserted exactly once.
- Every index is removed at most once.
- An index can be removed either:
    1. From the front because it expired.
    2. From the back because a larger or equal value arrived.

Although we have while loops inside the for loop,
the total number of deque removals across the entire
algorithm cannot exceed n.

Therefore:
- Total insertions = n.
- Total removals <= n.
- Total deque operations = O(n).

Hence, overall time complexity = O(n).

SPACE COMPLEXITY: O(k) AUXILIARY SPACE

Why?
- The deque contains indices belonging to the current
  window or useful candidates from that window.
- It can contain at most k indices.
- Therefore, deque space = O(k).

Output space:
- The result array contains n - k + 1 elements.
- Output space = O(n - k + 1).

Total space including output = O(n).
Auxiliary space excluding output = O(k).

This is the optimal solution.
*/

