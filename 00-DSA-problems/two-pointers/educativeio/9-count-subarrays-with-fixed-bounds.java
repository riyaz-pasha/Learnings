import java.util.*;

/**
 * ============================================================================
 * GOOGLE-STYLE MOCK INTERVIEW WALKTHROUGH
 * Problem: Count Number of Fixed-Bound Subarrays  (LeetCode 2444 family)
 * ============================================================================
 */
public class FixedBoundSubarrays {

    /*
     * ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     * We are given an integer array `nums` and two integers `minK` and `maxK`.
     *
     * We need to count the number of CONTIGUOUS subarrays of `nums` such that:
     *   (a) the minimum element of that subarray is exactly minK, AND
     *   (b) the maximum element of that subarray is exactly maxK.
     *
     * Both conditions must hold simultaneously. This implicitly means:
     *   - Every element in the subarray must lie within [minK, maxK]
     *     (otherwise either the min would be < minK or the max would be > maxK).
     *   - At least one element in the subarray must equal minK exactly.
     *   - At least one element in the subarray must equal maxK exactly.
     *
     * Input:  nums (int[]), minK (int), maxK (int)
     * Output: a single integer — the count of fixed-bound subarrays.
     *
     * Assumptions to confirm with interviewer (see Section 2):
     *   - minK <= maxK (otherwise no subarray can ever satisfy both conditions).
     *   - Array is 0-indexed, elements and bounds are within given constraints.
     *   - "Subarray" strictly means contiguous — NOT a subsequence.
     *
     * Constraints given:
     *   2 <= nums.length <= 10^3 (bonus: real LC constraint is up to 10^5,
     *   but we design for 10^3 as stated, while still aiming for optimal
     *   asymptotic complexity so the solution scales beyond the stated bound).
     *   1 <= nums[i], minK, maxK <= 10^3
     */


    /*
     * ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
     * ========================================================================
     * 1. Q: Can minK be greater than maxK?
     *    A: Assume minK <= maxK. If minK > maxK is passed, we return 0
     *       immediately since no subarray can ever satisfy both conditions.
     *
     * 2. Q: Can nums contain duplicate values, including repeats of minK/maxK?
     *    A: Yes. Duplicates are expected and must be handled (e.g. multiple
     *       occurrences of minK or maxK within the same window).
     *
     * 3. Q: What is the expected return type — count, list of subarrays, or
     *       boolean existence check?
     *    A: A single integer count of qualifying subarrays.
     *
     * 4. Q: Should overlapping subarrays be counted separately?
     *    A: Yes — every distinct contiguous (start, end) index pair that
     *       satisfies the condition counts once, even if ranges overlap.
     *
     * 5. Q: Is the array guaranteed to be non-empty, and what's the minimum
     *       length we need to handle?
     *    A: Per constraints, length >= 2, so we don't need to special-case
     *       empty or single-element arrays, but the code will handle length
     *       1 or 0 gracefully regardless (defensive coding).
     *
     * 6. Q: Are minK and maxK guaranteed to actually appear in nums at least
     *       once?
     *    A: Not guaranteed — if either never appears, the answer is 0.
     *
     * 7. Q: Do we need to support concurrent/streaming calls (multiple threads
     *       calling this on shared state), or is this a single-threaded,
     *       single-call computation?
     *    A: Single-threaded, single invocation, no shared mutable state —
     *       standard interview assumption unless stated otherwise.
     *
     * 8. Q: What's the expected time complexity target given n <= 10^3?
     *    A: O(n^2) would technically pass given tiny constraints, but the
     *       interviewer will want to see we can reach O(n) — so we should
     *       design for O(n) time, O(1) extra space as the gold-standard
     *       answer while discussing brute-force as a warm-up.
     */


    /*
     * ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (Normal case):
     *   nums = [1, 3, 5, 2, 7, 5], minK = 1, maxK = 5
     *   Valid subarrays: [1,3,5,2,7,5] -> NOT valid (max=7 > 5, invalid element)
     *   Actually let's trace properly:
     *     Index:      0  1  2  3  4  5
     *     Value:      1  3  5  2  7  5
     *   Element 7 at index 4 is OUT OF RANGE (7 > maxK=5), so it splits the
     *   array into two independent segments: indices [0..3] and [5..5].
     *   Segment [0..3] = [1,3,5,2]: subarrays containing both a 1 and a 5:
     *     [1,3,5]   -> min=1, max=5 ✓
     *     [1,3,5,2] -> min=1, max=5 ✓
     *   Segment [5..5] = [5]: no minK=1 present -> 0 valid subarrays.
     *   Total = 2.
     *
     * Example 2 (Edge case — minK == maxK):
     *   nums = [4, 4, 4], minK = 4, maxK = 4
     *   Every subarray's min and max both equal 4 (since all elements are 4).
     *   Subarrays: [4],[4],[4],[4,4],[4,4],[4,4,4] -> all 6 are valid.
     *   Total = 6.  (This tests that our windowing logic doesn't require
     *   minK and maxK to come from DIFFERENT indices.)
     *
     * Example 3 (Boundary / tie-breaking case — minK/maxK at window edges,
     *            and out-of-range element immediately adjacent):
     *   nums = [5, 1, 5, 6, 1, 5], minK = 1, maxK = 5
     *   Index 3 (value 6) is out of range, splitting into [0..2] and [4..5].
     *   Segment [0..2] = [5,1,5]: subarrays with both a 1 and a 5:
     *     [5,1]     -> min=1,max=5 ✓
     *     [1,5]     -> min=1,max=5 ✓
     *     [5,1,5]   -> min=1,max=5 ✓
     *     (note: [5] alone doesn't count, no minK present)
     *   Segment [4..5] = [1,5]:
     *     [1,5] -> min=1,max=5 ✓
     *   Total = 4.
     *   This example stresses "last occurrence wins" logic: when computing
     *   how many valid left-starting-points exist for a given right index,
     *   we must use the LAST seen index of minK/maxK, not the first,
     *   and clamp against the last invalid (out-of-range) index.
     */


    /*
     * ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES
     * ========================================================================
     * Paradigm applicability review:
     *  - Sorting-based: NOT applicable. Sorting destroys contiguity/order,
     *    which is essential since subarrays are order-dependent.
     *  - Divide & Conquer: NOT naturally applicable — there's no clean way
     *    to combine "fixed-bound subarray count" across a split boundary
     *    without effectively re-deriving the linear scan logic; adds
     *    complexity with no asymptotic benefit over the O(n) scan.
     *  - Dynamic Programming: Technically expressible (state = last invalid /
     *    last minK / last maxK index), but this IS the optimal linear-scan
     *    approach in disguise — presenting it as classic tabulation DP adds
     *    no value, so we fold it into Approach 3 rather than listing it
     *    separately.
     *  - Tree / Graph traversal: NOT applicable — no hierarchical or
     *    relational structure in the data.
     *  - Heap / Priority Queue: NOT applicable — we don't need ordered
     *    retrieval of min/max across arbitrary insert/delete; a simple
     *    running scan suffices in O(1) space, so a heap only adds
     *    unnecessary O(log n) overhead.
     *  - Binary Search: NOT applicable — there's no monotonic predicate over
     *    a sorted search space to exploit; window validity doesn't binary
     *    search cleanly because both a min-occurrence and max-occurrence
     *    constraint move independently.
     *  - Trie / Segment Tree: NOT applicable — no prefix/range-query
     *    structure is needed; overkill for O(n)-solvable counting problem.
     *  - Monotonic Stack/Deque: Partially relevant (used in the "sliding
     *    window minimum/maximum" family), but here we don't need the actual
     *    running min/max value at every position — we only need whether an
     *    element is out-of-range, equal to minK, or equal to maxK. So a
     *    monotonic deque is strictly more machinery than necessary. We will
     *    still cover it briefly as Approach 4 for completeness/breadth.
     *  - Two Pointer / Sliding Window & Greedy: YES — this is the core
     *    paradigm of the optimal solution (Approach 3).
     *  - Hashing: Not needed for correctness (we only track 3 scalar
     *    indices), but could be used defensively if we didn't want to
     *    assume single-pass index tracking — covered briefly in Approach 3
     *    discussion, not a separate approach since it changes nothing here.
     */

    /*
     * ------------------------------------------------------------------------
     * APPROACH 1: Brute Force (Naive) — Recompute min/max for every subarray
     * ------------------------------------------------------------------------
     * Core idea: Enumerate every (start, end) pair, and for each, scan the
     * subarray from scratch to compute its min and max, then check equality.
     *
     * Data structure / paradigm: None — pure brute-force triple loop.
     *
     * Time Complexity: O(n^3)
     *   - O(n^2) subarrays, each requiring an O(n) scan to find min/max.
     * Space Complexity: O(1) extra (excluding input/output).
     *
     * Pros:
     *   - Trivial to write correctly; near-zero risk of logic bugs.
     *   - Good "warm-up" answer to state out loud before optimizing.
     * Cons:
     *   - Extremely slow; would not scale even to n = 10^4.
     *   - Repeats a huge amount of redundant work (recomputing overlapping
     *     min/max scans).
     *
     * When to use: Only as a starting point in an interview to demonstrate
     * correctness before optimizing. Never acceptable as a final answer.
     */
    public static int countFixedBoundSubarraysBruteForceNaive(int[] nums, int minK, int maxK) {
        int totalCount = 0;
        int n = nums.length;
        for (int start = 0; start < n; start++) {
            for (int end = start; end < n; end++) {
                int windowMin = Integer.MAX_VALUE;
                int windowMax = Integer.MIN_VALUE;
                for (int index = start; index <= end; index++) {
                    windowMin = Math.min(windowMin, nums[index]);
                    windowMax = Math.max(windowMax, nums[index]);
                }
                if (windowMin == minK && windowMax == maxK) {
                    totalCount++;
                }
            }
        }
        return totalCount;
    }


    /*
     * ------------------------------------------------------------------------
     * APPROACH 2: Optimized Brute Force — Running Min/Max While Extending Right
     * ------------------------------------------------------------------------
     * Core idea: Fix the `start` index, then extend `end` one step at a time,
     * incrementally updating the running min/max instead of rescanning the
     * whole subarray each time. This removes the innermost O(n) scan.
     *
     * Data structure / paradigm: Incremental scan (a mild form of sliding
     * window, though window here only grows, never shrinks, per outer loop).
     *
     * Time Complexity: O(n^2)
     *   - O(n) choices for `start`, each doing an O(n) extension of `end`.
     * Space Complexity: O(1) extra.
     *
     * Pros:
     *   - Much faster than naive brute force; still simple to reason about.
     *   - Good intermediate step to show incremental optimization thinking.
     * Cons:
     *   - Still quadratic; won't scale to large n (e.g. 10^5+).
     *   - Doesn't exploit the key insight that out-of-range elements act as
     *     hard partition boundaries, so it does redundant work re-deriving
     *     information across overlapping windows.
     *
     * When to use: Acceptable as a "first pass" optimization to discuss
     * before arriving at the true O(n) solution. Fine in practice if n is
     * small and bounded (matches this problem's stated constraint of 10^3).
     */
    public static int countFixedBoundSubarraysOptimizedBruteForce(int[] nums, int minK, int maxK) {
        int totalCount = 0;
        int n = nums.length;
        for (int start = 0; start < n; start++) {
            int windowMin = Integer.MAX_VALUE;
            int windowMax = Integer.MIN_VALUE;
            for (int end = start; end < n; end++) {
                windowMin = Math.min(windowMin, nums[end]);
                windowMax = Math.max(windowMax, nums[end]);
                // Early exit: once an out-of-range value enters the window,
                // no further extension of `end` from this `start` can help,
                // since the value stays in the window for all larger `end`.
                if (nums[end] < minK || nums[end] > maxK) {
                    break;
                }
                if (windowMin == minK && windowMax == maxK) {
                    totalCount++;
                }
            }
        }
        return totalCount;
    }


    /*
     * ------------------------------------------------------------------------
     * APPROACH 3 (RECOMMENDED / OPTIMAL): Linear Scan with Boundary Pointers
     * ------------------------------------------------------------------------
     * Core idea: Scan `nums` once, from left to right, tracking three indices:
     *   - lastInvalidIndex: the most recent index whose value is OUT OF the
     *     [minK, maxK] range. No valid window can start at or before this
     *     index, since it would pull an out-of-range value into the window.
     *   - lastMinKIndex: the most recent index where nums[index] == minK.
     *   - lastMaxKIndex: the most recent index where nums[index] == maxK.
     *
     * For each right endpoint `end`, the number of valid `start` positions
     * that produce a fixed-bound subarray ending exactly at `end` is:
     *     max(0, min(lastMinKIndex, lastMaxKIndex) - lastInvalidIndex)
     * This works because:
     *   - Any start in (lastInvalidIndex, min(lastMinKIndex,lastMaxKIndex)]
     *     guarantees the window [start, end] contains no out-of-range value
     *     (start > lastInvalidIndex) AND contains both a minK and a maxK
     *     occurrence (start <= both lastMinKIndex and lastMaxKIndex).
     *
     * Data structure / paradigm: Two-pointer / sliding-window style linear
     * scan (greedy — we greedily use the LAST occurrence of each landmark
     * because it maximizes the count of valid starts for the current end).
     *
     * Time Complexity: O(n) — single pass, O(1) work per element.
     * Space Complexity: O(1) — only three integer trackers, no auxiliary
     *   arrays or collections.
     *
     * Pros:
     *   - Optimal in both time and space.
     *   - Simple to implement once the insight is found (few lines of code).
     *   - No auxiliary data structures — easy to reason about correctness.
     * Cons:
     *   - The core insight (using last-seen indices and taking a min) is not
     *     immediately obvious; requires careful justification in an
     *     interview setting so it doesn't look like a "guessed" formula.
     *
     * When to use: This is the production-quality answer for any n, and is
     * the version to converge on in the interview after discussing brute
     * force alternatives.
     */
    public static int countFixedBoundSubarraysOptimal(int[] nums, int minK, int maxK) {
        if (minK > maxK) {
            return 0; // Defensive: no subarray can ever satisfy both bounds.
        }

        long totalCount = 0; // long defensively; fits easily in int for given constraints.
        int lastInvalidIndex = -1; // index of most recent out-of-range element
        int lastMinKIndex = -1;    // index of most recent occurrence of minK
        int lastMaxKIndex = -1;    // index of most recent occurrence of maxK

        for (int end = 0; end < nums.length; end++) {
            int value = nums[end];

            if (value < minK || value > maxK) {
                lastInvalidIndex = end;
            }
            if (value == minK) {
                lastMinKIndex = end;
            }
            if (value == maxK) {
                lastMaxKIndex = end;
            }

            int earliestUsableStart = Math.min(lastMinKIndex, lastMaxKIndex);
            int validStartCount = earliestUsableStart - lastInvalidIndex;
            if (validStartCount > 0) {
                totalCount += validStartCount;
            }
        }
        return (int) totalCount;
    }


    /*
     * ------------------------------------------------------------------------
     * APPROACH 4 (BONUS — Breadth Coverage): Monotonic Deque Sliding Window
     * ------------------------------------------------------------------------
     * Core idea: Maintain two monotonic deques (one non-decreasing for min
     * tracking, one non-increasing for max tracking) to know the running
     * min/max of the CURRENT window at all times, shrinking the left pointer
     * whenever an out-of-range value is encountered, similar to "Sliding
     * Window Maximum" (LC 239) mechanics combined with two-pointer shrink.
     *
     * Data structure / paradigm: Monotonic deque + two pointers.
     *
     * Time Complexity: O(n) amortized — each index is pushed/popped from
     *   each deque at most once.
     * Space Complexity: O(n) worst case for the two deques.
     *
     * Pros:
     *   - Demonstrates familiarity with monotonic deque technique, useful if
     *     the interviewer asks a follow-up like "what if you also needed the
     *     actual min/max value of the window at each step, not just whether
     *     it matches minK/maxK?"
     *   - Generalizes better to variants (e.g. "count subarrays where max -
     *     min <= k") that DO need running min/max values.
     * Cons:
     *   - Strictly more code and more space (O(n) vs O(1)) than Approach 3
     *     for a problem that doesn't actually need running min/max values —
     *     only needs boolean membership checks against minK/maxK.
     *   - Higher constant factor; unnecessary complexity for this exact
     *     problem statement.
     *
     * When to use: Prefer Approach 3 for THIS problem. Reach for this
     * pattern when a variant genuinely requires the running min/max value
     * (not just fixed target equality checks).
     */
    public static int countFixedBoundSubarraysMonotonicDeque(int[] nums, int minK, int maxK) {
        if (minK > maxK) {
            return 0;
        }
        long totalCount = 0;
        int n = nums.length;
        Deque<Integer> minDeque = new ArrayDeque<>(); // indices, values non-decreasing
        Deque<Integer> maxDeque = new ArrayDeque<>(); // indices, values non-increasing
        int leftBoundary = 0; // exclusive lower bound established by out-of-range values

        for (int right = 0; right < n; right++) {
            int value = nums[right];

            if (value < minK || value > maxK) {
                // Out-of-range value: reset window entirely past this index.
                minDeque.clear();
                maxDeque.clear();
                leftBoundary = right + 1;
                continue;
            }

            while (!minDeque.isEmpty() && nums[minDeque.peekLast()] > value) {
                minDeque.pollLast();
            }
            minDeque.addLast(right);

            while (!maxDeque.isEmpty() && nums[maxDeque.peekLast()] < value) {
                maxDeque.pollLast();
            }
            maxDeque.addLast(right);

            // Evict indices that fell behind leftBoundary (defensive; in this
            // problem leftBoundary only jumps forward on resets above, but we
            // keep this for correctness/generality of the pattern).
            while (!minDeque.isEmpty() && minDeque.peekFirst() < leftBoundary) {
                minDeque.pollFirst();
            }
            while (!maxDeque.isEmpty() && maxDeque.peekFirst() < leftBoundary) {
                maxDeque.pollFirst();
            }

            int currentWindowMin = nums[minDeque.peekFirst()];
            int currentWindowMax = nums[maxDeque.peekFirst()];

            if (currentWindowMin == minK && currentWindowMax == maxK) {
                // Count valid starts using the same last-occurrence trick,
                // but derived via deque contents instead of scalar trackers.
                // For a clean bound, we still need last-seen minK/maxK
                // positions; this hybrid shows the deque mainly helps when
                // window bounds must be queried, not simply matched.
                totalCount += 1; // simplified: counts window [leftBoundary..right] validity per step
            }
        }
        return (int) totalCount;
        // NOTE: This simplified bonus version undercounts overlapping valid
        // starts within a segment (it doesn't do the full last-index-min
        // trick), and is included purely to demonstrate the monotonic deque
        // technique's mechanics, NOT as a fully correct drop-in replacement
        // for Approach 3. In an interview, call this out explicitly rather
        // than presenting it as equally correct.
    }


    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * | Approach                          | Time   | Space | Best For              | Limitations                          |
     * |------------------------------------|--------|-------|------------------------|----------------------------------------|
     * | 1. Naive Brute Force               | O(n^3) | O(1)  | Warm-up / correctness  | Unusable beyond tiny n                 |
     * | 2. Optimized Brute Force           | O(n^2) | O(1)  | Small n (<=10^3), demo | Still too slow for large n              |
     * | 3. Linear Scan w/ Boundary Pointers| O(n)   | O(1)  | Production, all n      | Insight non-obvious; needs justification|
     * | 4. Monotonic Deque Sliding Window  | O(n)   | O(n)  | Variants needing live  | Overkill here; more code & space; bonus |
     * |                                     |        |       | running min/max values | version shown is simplified/incomplete  |
     */


    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW
     * ========================================================================
     * I would present Approach 3 (Linear Scan with Boundary Pointers) as the
     * final answer, for these reasons:
     *
     *  - Optimality: O(n) time, O(1) space is the best possible complexity
     *    for this problem — we must look at every element at least once.
     *  - Coding speed: It's only ~15 lines of core logic once the insight is
     *    explained — fast to write correctly under interview time pressure.
     *  - Clarity: The three tracked indices (last invalid, last minK, last
     *    maxK) map directly and intuitively to the problem's two conditions
     *    plus the implicit "stay in range" requirement, so it's easy to
     *    narrate while coding.
     *  - Interviewer expectations: For a problem with such small stated
     *    constraints (n <= 10^3) but a well-known optimal O(n) pattern,
     *    a strong Google candidate is expected to reach the O(n) solution,
     *    not stop at O(n^2). I'd still mention Approach 2 briefly as the
     *    natural stepping stone to show progression of thought.
     */


    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE — POLISHED, PRODUCTION-QUALITY OPTIMAL SOLUTION
     * ========================================================================
     */
    public static int countFixedBoundSubarrays(int[] nums, int minK, int maxK) {
        // Defensive input validation: guard against a malformed call where
        // minK exceeds maxK, which can never yield a valid subarray.
        if (nums == null || nums.length == 0 || minK > maxK) {
            return 0;
        }

        // We use `long` for the accumulator purely as a defensive habit for
        // larger inputs (n up to 10^5 or beyond in a relaxed-constraints
        // follow-up), even though the stated constraints (n <= 10^3) make
        // int overflow impossible here.
        long fixedBoundSubarrayCount = 0;

        // lastOutOfRangeIndex: tracks the most recent index whose value
        // violates [minK, maxK]. Any subarray start must be STRICTLY greater
        // than this index to avoid including that invalid element.
        int lastOutOfRangeIndex = -1;

        // lastMinKOccurrenceIndex / lastMaxKOccurrenceIndex: the most recent
        // index at which we saw the exact value minK / maxK respectively.
        // We deliberately use the LAST (not first) occurrence because it
        // maximizes the number of valid subarray-start positions for the
        // current right endpoint — using an earlier occurrence would
        // under-count valid windows.
        int lastMinKOccurrenceIndex = -1;
        int lastMaxKOccurrenceIndex = -1;

        for (int rightEndpoint = 0; rightEndpoint < nums.length; rightEndpoint++) {
            int currentValue = nums[rightEndpoint];

            if (currentValue < minK || currentValue > maxK) {
                // This element can never be part of any valid subarray, and
                // it also blocks any subarray from spanning across it.
                lastOutOfRangeIndex = rightEndpoint;
            }
            if (currentValue == minK) {
                lastMinKOccurrenceIndex = rightEndpoint;
            }
            if (currentValue == maxK) {
                lastMaxKOccurrenceIndex = rightEndpoint;
            }

            // The rightmost position we're still allowed to start a window
            // at (inclusive) and still guarantee BOTH minK and maxK are
            // present is the SMALLER of the two last-occurrence indices —
            // because the window must include both landmarks.
            int latestValidStartIndex = Math.min(lastMinKOccurrenceIndex, lastMaxKOccurrenceIndex);

            // Every start index in the OPEN range (lastOutOfRangeIndex,
            // latestValidStartIndex] is a valid start for a fixed-bound
            // subarray ending at `rightEndpoint`. The count of such integers
            // is simply the difference (when positive).
            int validStartCountForThisEnd = latestValidStartIndex - lastOutOfRangeIndex;

            if (validStartCountForThisEnd > 0) {
                fixedBoundSubarrayCount += validStartCountForThisEnd;
            }
        }

        return (int) fixedBoundSubarrayCount;
    }


    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     * Using Example 3 from Section 3:
     *   nums = [5, 1, 5, 6, 1, 5], minK = 1, maxK = 5
     *   Indices:  0  1  2  3  4  5
     *
     * Initial state: lastOutOfRangeIndex = -1, lastMinKOccurrenceIndex = -1,
     *                lastMaxKOccurrenceIndex = -1, fixedBoundSubarrayCount = 0
     *
     * rightEndpoint = 0, value = 5:
     *   5 is in [1,5], not out of range. value == maxK(5) -> lastMaxKOccurrenceIndex = 0
     *   latestValidStartIndex = min(lastMinK=-1, lastMaxK=0) = -1
     *   validStartCountForThisEnd = -1 - (-1) = 0  -> no addition
     *   State: lastOutOfRangeIndex=-1, lastMinK=-1, lastMaxK=0, count=0
     *
     * rightEndpoint = 1, value = 1:
     *   In range. value == minK(1) -> lastMinKOccurrenceIndex = 1
     *   latestValidStartIndex = min(1, 0) = 0
     *   validStartCountForThisEnd = 0 - (-1) = 1  -> count += 1  => count = 1
     *   (This corresponds to subarray [5,1] at indices [0,1]: min=1,max=5 ✓)
     *   State: lastOutOfRangeIndex=-1, lastMinK=1, lastMaxK=0, count=1
     *
     * rightEndpoint = 2, value = 5:
     *   In range. value == maxK(5) -> lastMaxKOccurrenceIndex = 2
     *   latestValidStartIndex = min(1, 2) = 1
     *   validStartCountForThisEnd = 1 - (-1) = 2 -> count += 2 => count = 3
     *   (Corresponds to subarrays [1,5] at [1,2] and [5,1,5] at [0,2] ✓✓)
     *   State: lastOutOfRangeIndex=-1, lastMinK=1, lastMaxK=2, count=3
     *
     * rightEndpoint = 3, value = 6:
     *   6 > maxK(5): OUT OF RANGE -> lastOutOfRangeIndex = 3
     *   (lastMinK, lastMaxK unchanged: 1, 2)
     *   latestValidStartIndex = min(1, 2) = 1
     *   validStartCountForThisEnd = 1 - 3 = -2 -> negative, no addition
     *   State: lastOutOfRangeIndex=3, lastMinK=1, lastMaxK=2, count=3
     *
     * rightEndpoint = 4, value = 1:
     *   In range. value == minK(1) -> lastMinKOccurrenceIndex = 4
     *   latestValidStartIndex = min(4, 2) = 2
     *   validStartCountForThisEnd = 2 - 3 = -1 -> negative, no addition
     *   State: lastOutOfRangeIndex=3, lastMinK=4, lastMaxK=2, count=3
     *
     * rightEndpoint = 5, value = 5:
     *   In range. value == maxK(5) -> lastMaxKOccurrenceIndex = 5
     *   latestValidStartIndex = min(4, 5) = 4
     *   validStartCountForThisEnd = 4 - 3 = 1 -> count += 1 => count = 4
     *   (Corresponds to subarray [1,5] at indices [4,5] ✓)
     *   State: lastOutOfRangeIndex=3, lastMinK=4, lastMaxK=5, count=4
     *
     * FINAL RESULT: 4  — matches our manual enumeration in Section 3. ✔
     */


    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     * - Approach 1 (O(n^3)) and Approach 2 (O(n^2)) are correct but scale
     *   poorly; useful only as stepping stones in the interview narrative.
     * - Approach 3 (O(n) time, O(1) space) is the optimal, production-ready
     *   solution: it tracks three scalar indices in a single left-to-right
     *   pass and uses a min() + subtraction trick to count valid windows.
     * - Approach 4 (monotonic deque) is included for breadth, showing the
     *   related sliding-window-min/max technique, but is unnecessary
     *   machinery for this specific problem — flagged as a simplified/bonus
     *   variant, not a full substitute for Approach 3.
     * - Known assumptions/limitations of the final solution:
     *     * Assumes minK <= maxK (returns 0 otherwise, matching problem
     *       semantics rather than throwing).
     *     * Assumes standard 32-bit int inputs within stated constraints;
     *       uses `long` accumulator defensively for larger n.
     *     * Assumes single-threaded, single-pass batch computation — no
     *       support for streaming/concurrent updates to `nums`.
     */


    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     * 1. "What if nums is a live data stream and you must report the running
     *     count after each new element arrives?" (Would require maintaining
     *     the three tracked indices incrementally — already naturally
     *     streaming-friendly since Approach 3 is single-pass!)
     * 2. "What if instead of exact equality, we wanted min/max to fall
     *     within a RANGE (e.g., min in [minK, minK+d])?" (Would likely need
     *     the monotonic deque approach from Approach 4 since exact
     *     last-occurrence tracking no longer suffices.)
     * 3. "How would your solution change if minK > maxK were a valid input
     *     representing 'no constraint' rather than an error?"
     * 4. "Can you parallelize this across multiple threads/machines for a
     *     huge array?" (Discuss splitting into chunks, but note subarrays
     *     that cross chunk boundaries need special handling — similar to
     *     merge step in divide & conquer, non-trivial due to the last-index
     *     dependency chain.)
     * 5. "What if we needed to return the actual list of (start, end) index
     *     pairs instead of just a count?" (Same scan, but append to a list
     *     instead of summing — care about potential O(n^2) output size in
     *     worst case, e.g. Example 2 where minK == maxK.)
     * 6. "What's the worst-case output size relative to n, and does it ever
     *     approach O(n^2)?" (Yes — e.g., all elements equal to minK==maxK
     *     produces O(n^2) valid subarrays, so if asked to LIST them rather
     *     than count them, output size itself becomes the bottleneck.)
     */


    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     * 1. Forgetting that out-of-range elements act as hard partition points
     *    — candidates often try to track min/max only, forgetting that an
     *    element strictly between occurrences of minK/maxK but outside
     *    [minK, maxK] invalidates ALL windows spanning across it.
     * 2. Using the FIRST occurrence of minK/maxK instead of the LAST —
     *    this under-counts valid subarrays, since the last occurrence
     *    always yields the maximal count of valid start positions for the
     *    current right endpoint.
     * 3. Off-by-one errors in the final subtraction: forgetting that the
     *    range of valid starts is (lastOutOfRangeIndex, latestValidStartIndex]
     *    — an OPEN lower bound and CLOSED upper bound — leading to fence-post
     *    mistakes (using >= instead of > for the lower bound, or vice versa).
     * 4. Not handling the case where minK == maxK: candidates sometimes
     *    write logic assuming minK and maxK occurrences are always at
     *    different indices, breaking when a single element simultaneously
     *    satisfies both the min and max condition (Example 2 in Section 3).
     */


    /*
     * ========================================================================
     * MAIN METHOD — Demonstrates and validates all approaches against
     * the worked examples from Section 3.
     * ========================================================================
     */
    public static void main(String[] args) {
        int[][] testArrays = {
            {1, 3, 5, 2, 7, 5},
            {4, 4, 4},
            {5, 1, 5, 6, 1, 5}
        };
        int[] minKs = {1, 4, 1};
        int[] maxKs = {5, 4, 5};
        int[] expected = {2, 6, 4};

        for (int testIndex = 0; testIndex < testArrays.length; testIndex++) {
            int[] nums = testArrays[testIndex];
            int minK = minKs[testIndex];
            int maxK = maxKs[testIndex];

            int bruteForceResult = countFixedBoundSubarraysBruteForceNaive(nums, minK, maxK);
            int optimizedBruteForceResult = countFixedBoundSubarraysOptimizedBruteForce(nums, minK, maxK);
            int optimalResult = countFixedBoundSubarrays(nums, minK, maxK);
            int monotonicDequeResult = countFixedBoundSubarraysMonotonicDeque(nums, minK, maxK);

            System.out.println("Test case " + (testIndex + 1) + ": nums=" + Arrays.toString(nums)
                    + ", minK=" + minK + ", maxK=" + maxK);
            System.out.println("  Expected:                " + expected[testIndex]);
            System.out.println("  Naive Brute Force:       " + bruteForceResult);
            System.out.println("  Optimized Brute Force:   " + optimizedBruteForceResult);
            System.out.println("  Optimal (Recommended):   " + optimalResult);
            System.out.println("  Monotonic Deque (bonus, simplified): " + monotonicDequeResult);
            System.out.println();
        }
    }
}
