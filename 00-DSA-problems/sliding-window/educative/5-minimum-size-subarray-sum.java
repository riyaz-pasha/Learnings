import java.util.*;

/**
 * ============================================================================
 *  GOOGLE-STYLE MOCK INTERVIEW WALKTHROUGH
 *  Problem: Minimum Size Subarray Sum   (LeetCode 209 family)
 * ============================================================================
 *
 * Given an array of POSITIVE integers `nums` and a positive integer `target`,
 * find the length of the shortest contiguous subarray whose sum is
 * >= target. If no such subarray exists, return 0.
 *
 * This single file is organized as 13 clearly labeled sections, mirroring
 * exactly how this problem should be presented, reasoned about, and coded
 * in a real Google onsite/phone-screen interview.
 * ============================================================================
 */
class MinimumSizeSubarraySum {

    private MinimumSizeSubarraySum() {
        // Utility/demo class — not meant to be instantiated.
    }

    /*
     * ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     *
     * In my own words:
     *   "I'm given an array of strictly positive integers and a positive
     *    target value. I need to find the smallest window (contiguous run
     *    of elements) whose sum is at least the target. I return the
     *    length of that window. If every possible window's sum falls short
     *    of the target (including the sum of the whole array), I return 0
     *    to signal 'no such subarray exists'."
     *
     * Inputs:
     *   - int[] nums  : array of positive integers (nums[i] >= 1)
     *   - int target  : a positive integer threshold
     *
     * Output:
     *   - int : minimum length of a contiguous subarray with sum >= target,
     *           or 0 if none exists.
     *
     * Key constraints & assumptions called out explicitly:
     *   - All elements are POSITIVE (not just non-negative). This is the
     *     single most important constraint in this problem — it's what
     *     makes the sliding-window approach valid, because the running
     *     window sum is strictly monotonic as we move either pointer.
     *   - We want length, not the subarray itself or its sum.
     *   - "Contiguous" — not a subsequence; elements must be adjacent.
     *   - Empty subarray is never a valid answer (sum would be 0, and
     *     target is positive, so it can never qualify anyway).
     */

    /*
     * ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (asked to interviewer, with assumed
     *            answers so I can proceed without blocking)
     * ========================================================================
     *
     * Q1: Can `nums` contain zero or negative numbers?
     *     A: No — problem states "positive integers," so every element is
     *        >= 1. I will code defensively but will document that behavior
     *        is only guaranteed under this assumption.
     *
     * Q2: What is the expected size of `nums`? Do we need to worry about
     *     very large inputs (10^5, 10^6+)?
     *     A: Assume up to ~10^5 elements, so O(n log n) and O(n) solutions
     *        are both acceptable, but O(n^2) brute force risks TLE on the
     *        upper end — still worth presenting for correctness/baseline.
     *
     * Q3: Can `target` be larger than the sum of the entire array?
     *     A: Yes — in that case, no subarray qualifies, so we return 0.
     *
     * Q4: Are duplicate values allowed in nums?
     *     A: Yes, duplicates are fine and don't affect the algorithm since
     *        we operate on sums, not distinct values.
     *
     * Q5: If multiple subarrays tie for the minimum length, do we care
     *     which one (e.g., leftmost) we report?
     *     A: No — we only need the minimum LENGTH, not the actual subarray
     *        or its starting index, so ties don't matter for the return
     *        value.
     *
     * Q6: Is nums guaranteed non-null and non-empty?
     *     A: Assume nums is non-null; explicitly handle the empty-array
     *        case by returning 0 defensively.
     *
     * Q7: Should the solution be thread-safe / handle concurrent calls?
     *     A: Not required — this is a pure, stateless function operating
     *        on its inputs; no shared mutable state is involved.
     *
     * Q8: Is there a bound on individual element values or on target that
     *     could cause integer overflow when summing?
     *     A: Assume values fit comfortably in int range individually, but
     *        I will still use `long` for the running/prefix sums as a
     *        defensive habit in case of large arrays with large elements.
     */

    /*
     * ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (normal case):
     *   nums = [2, 3, 1, 2, 4, 3], target = 7
     *   The subarray [4, 3] sums to 7, length 2. [2,3,1,2] sums to 8 (len 4).
     *   Answer: 2
     *
     * Example 2 (edge case — no subarray meets target):
     *   nums = [1, 1, 1, 1, 1, 1, 1, 1], target = 11
     *   Total sum of array = 8, which is less than 11, so no subarray can
     *   ever reach the target.
     *   Answer: 0
     *
     * Example 3 (boundary / tie-breaking case):
     *   nums = [1, 2, 3, 4, 5], target = 15
     *   The ONLY subarray reaching sum 15 is the entire array itself
     *   (1+2+3+4+5 = 15). No shorter window works because every prefix
     *   sums to less than 15.
     *   Answer: 5
     *
     * Additional edge cases worth mentioning out loud in interview:
     *   - Single-element array where nums[0] >= target -> answer is 1.
     *   - Single-element array where nums[0] <  target -> answer is 0.
     *   - target exactly equal to the sum of the whole array -> answer is
     *     the full array length (unless a shorter subarray also reaches it).
     *   - Very large target compared to all individual elements -> may
     *     require scanning most/all of the array; still O(n) overall with
     *     sliding window since each pointer moves forward only.
     */

    /*
     * ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE SOLUTIONS (with paradigm coverage)
     * ========================================================================
     *
     * Paradigms considered and whether they apply:
     *
     *   - Brute force            -> APPLICABLE (Approach 1, baseline/oracle)
     *   - Sorting-based          -> NOT APPLICABLE. Sorting destroys the
     *                               contiguity requirement — subarrays must
     *                               preserve original order/adjacency, so
     *                               sorting nums would give wrong answers.
     *   - Hashing-based          -> NOT NATURALLY APPLICABLE. Hashing is
     *                               great for "subarray sums equal to X"
     *                               (via a prefix-sum-seen-so-far HashMap,
     *                               as in LeetCode 560), but that pattern
     *                               finds EXACT sums efficiently, not
     *                               "sum >= target" minimized over length.
     *                               A HashMap doesn't give us an efficient
     *                               way to query "smallest index j such
     *                               that prefix[j] >= prefix[i] + target"
     *                               the way a sorted structure does. Since
     *                               prefix sums are already monotonically
     *                               increasing (positive-only array), a
     *                               binary search over the implicit sorted
     *                               prefix array dominates hashing here.
     *   - Two pointer / sliding
     *     window                 -> APPLICABLE and OPTIMAL (Approach 3).
     *                               Positive-only elements guarantee the
     *                               window sum only grows when we expand
     *                               and only shrinks when we contract,
     *                               enabling a classic O(n) two-pointer
     *                               technique.
     *   - Divide and conquer     -> NOT NATURALLY APPLICABLE. You *could*
     *                               engineer a D&C that finds min-length
     *                               qualifying subarray by combining left
     *                               half, right half, and crossing sums
     *                               (similar to Kadane's D&C variant), but
     *                               it adds O(n log n) complexity with no
     *                               benefit over the O(n) sliding window —
     *                               included only for completeness, skipped
     *                               in code.
     *   - Greedy                 -> Sliding window IS essentially a greedy
     *                               strategy (greedily shrink from the left
     *                               whenever the window is "sufficient"),
     *                               so it's covered under Approach 3 rather
     *                               than treated separately.
     *   - Dynamic programming    -> NOT NATURALLY APPLICABLE. There's no
     *                               useful overlapping-subproblem/optimal-
     *                               substructure recurrence here that beats
     *                               O(n) sliding window; DP would just be
     *                               a slower disguised brute force.
     *   - Tree / graph traversal -> NOT APPLICABLE. No graph/tree structure
     *                               underlies this problem.
     *   - Heap / priority queue  -> NOT APPLICABLE. We don't need ordered
     *                               retrieval of max/min elements; we need
     *                               contiguous run sums, which heaps don't
     *                               model well.
     *   - Binary search          -> APPLICABLE (Approach 2), via prefix
     *                               sums. Since nums is all-positive, the
     *                               prefix sum array is strictly increasing,
     *                               so for each starting index we can binary
     *                               search for the smallest ending index
     *                               whose prefix sum is large enough.
     *   - Monotonic stack/deque  -> NOT APPLICABLE. Monotonic deques shine
     *                               for sliding-window MIN/MAX problems
     *                               (e.g., LeetCode 239). Here we need a
     *                               running SUM, which is trivially
     *                               maintained with a simple accumulator —
     *                               no deque needed.
     *   - Trie / segment tree    -> NOT APPLICABLE / OVERKILL. A Fenwick
     *                               tree or segment tree could maintain
     *                               prefix sums with range queries, but
     *                               that's solving a harder problem
     *                               (e.g., dynamic updates to nums) that
     *                               isn't asked for here.
     */

    // ------------------------------------------------------------------
    // Approach 1: Brute Force (Naive Double Loop)
    // ------------------------------------------------------------------
    /*
     * Core idea:
     *   For every possible starting index i, extend the window one
     *   element at a time to the right, accumulating the sum. The
     *   moment the running sum reaches target, record the window length
     *   and stop extending further for this start index (since we're
     *   only counting positive numbers, extending further only makes the
     *   window longer without decreasing the answer for this start).
     *
     * Data structure / paradigm:
     *   None beyond simple accumulation — pure brute force enumeration
     *   of all O(n^2) subarrays.
     *
     * Time complexity: O(n^2)
     *   - Outer loop runs n times (each start index).
     *   - Inner loop can run up to n times in the worst case (e.g., when
     *     no subarray qualifies, or the qualifying subarray is long).
     *
     * Space complexity: O(1)
     *   - Only a few scalar accumulator/best-length variables are used.
     *
     * Pros:
     *   - Extremely simple to reason about and verify by hand.
     *   - Zero risk of subtle bugs — great as a "correctness oracle" to
     *     cross-validate optimized approaches against.
     *
     * Cons:
     *   - Too slow for large inputs (n = 10^5 would be ~10^10 operations
     *     in the worst case) — would TLE in almost any real system.
     *
     * When to use:
     *   - Use only as a warm-up/starting point in the interview, or to
     *     validate test cases for the optimal solution. Never ship this
     *     as the final production answer for non-trivial input sizes.
     */
    public static int minSubArrayLenBruteForce(int[] nums, int target) {
        if (nums == null || nums.length == 0) {
            return 0;
        }

        int arrayLength = nums.length;
        int minimumLength = Integer.MAX_VALUE;

        for (int startIndex = 0; startIndex < arrayLength; startIndex++) {
            long runningSum = 0;
            for (int endIndex = startIndex; endIndex < arrayLength; endIndex++) {
                runningSum += nums[endIndex];
                if (runningSum >= target) {
                    int currentWindowLength = endIndex - startIndex + 1;
                    minimumLength = Math.min(minimumLength, currentWindowLength);
                    break; // Extending further can't help this start index.
                }
            }
        }

        return (minimumLength == Integer.MAX_VALUE) ? 0 : minimumLength;
    }

    // ------------------------------------------------------------------
    // Approach 2: Prefix Sums + Binary Search
    // ------------------------------------------------------------------
    /*
     * Core idea:
     *   Precompute a prefix-sum array `prefix`, where prefix[k] is the sum
     *   of the first k elements (prefix[0] = 0). Because all elements are
     *   positive, `prefix` is STRICTLY INCREASING. For every start index i
     *   (0-indexed into prefix), we want the smallest end index j > i such
     *   that prefix[j] - prefix[i] >= target, i.e. prefix[j] >= prefix[i] +
     *   target. Since prefix is sorted, we binary search for that
     *   threshold instead of scanning linearly.
     *
     * Data structure / paradigm:
     *   Prefix sum array + binary search (paradigm: binary search on a
     *   monotonic sequence).
     *
     * Time complexity: O(n log n)
     *   - Building the prefix array is O(n).
     *   - For each of the n starting indices, a binary search over the
     *     prefix array costs O(log n).
     *
     * Space complexity: O(n)
     *   - The prefix sum array itself requires O(n) extra space.
     *
     * Pros:
     *   - Demonstrates strong grasp of prefix sums and binary search.
     *   - Generalizes more easily if the problem were tweaked to ask
     *     range-sum-threshold queries repeatedly, or if we needed the
     *     prefix array for other purposes anyway.
     *
     * Cons:
     *   - Strictly dominated by the O(n) sliding window approach for this
     *     exact problem — it's asymptotically worse and uses more space.
     *   - More code/complexity than necessary given a simpler O(n) exists.
     *
     * When to use:
     *   - Good as a "middle" answer to show depth (prefix sums + binary
     *     search fluency) before pivoting to the fully optimal solution.
     *   - Also the natural approach if the array could contain zeros
     *     (prefix sums would be non-decreasing, not strictly increasing —
     *     binary search still works with a small tweak, whereas plain
     *     two-pointer needs a little more care but still works too).
     */
    public static int minSubArrayLenPrefixBinarySearch(int[] nums, int target) {
        if (nums == null || nums.length == 0) {
            return 0;
        }

        int arrayLength = nums.length;
        long[] prefixSums = new long[arrayLength + 1];
        for (int index = 0; index < arrayLength; index++) {
            prefixSums[index + 1] = prefixSums[index] + nums[index];
        }

        int minimumLength = Integer.MAX_VALUE;

        for (int startIndex = 0; startIndex <= arrayLength; startIndex++) {
            long neededPrefixValue = prefixSums[startIndex] + target;
            int endIndex = lowerBound(prefixSums, startIndex, neededPrefixValue);
            if (endIndex <= arrayLength) {
                minimumLength = Math.min(minimumLength, endIndex - startIndex);
            }
        }

        return (minimumLength == Integer.MAX_VALUE) ? 0 : minimumLength;
    }

    /**
     * Finds the leftmost index within prefixSums[fromIndex..] whose value
     * is >= target. Classic binary-search "lower bound" helper.
     */
    private static int lowerBound(long[] prefixSums, int fromIndex, long targetValue) {
        int low = fromIndex;
        int high = prefixSums.length; // exclusive upper bound
        while (low < high) {
            int mid = low + (high - low) / 2;
            if (prefixSums[mid] >= targetValue) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }

    // ------------------------------------------------------------------
    // Approach 3: Sliding Window / Two Pointer  (OPTIMAL)
    // ------------------------------------------------------------------
    /*
     * Core idea:
     *   Maintain a window [left, right] and a running `windowSum`. Expand
     *   the window by moving `right` forward, adding nums[right] to the
     *   sum. Whenever windowSum >= target, the window is "sufficient" —
     *   record its length, then greedily shrink from the left (moving
     *   `left` forward and subtracting nums[left] from the sum) as long as
     *   the window remains sufficient, since a shorter sufficient window
     *   is always at least as good. This greedy shrink is only correct
     *   because every element is positive: removing an element from the
     *   left can only decrease the sum, and we stop the instant the window
     *   is no longer sufficient.
     *
     * Data structure / paradigm:
     *   Two pointers (sliding window), greedy shrink-from-left.
     *
     * Time complexity: O(n)
     *   - `right` advances from 0 to n-1 exactly once.
     *   - `left` also advances at most n times total across the entire
     *     run (it never resets backward), so total pointer movement is
     *     O(n) + O(n) = O(n), not O(n^2), even though there's a nested
     *     while-loop inside the for-loop.
     *
     * Space complexity: O(1)
     *   - Only a handful of scalar variables (two pointers, running sum,
     *     best length) — no auxiliary arrays needed.
     *
     * Pros:
     *   - Optimal time AND space complexity.
     *   - Simple to code correctly under interview time pressure once the
     *     "expand then shrink" pattern is internalized.
     *   - Directly demonstrates the key insight (positive-only array =>
     *     monotonic window sum) that the interviewer is testing for.
     *
     * Cons:
     *   - Relies critically on all elements being positive. If negative
     *     numbers were allowed, windowSum would no longer be monotonic
     *     with respect to window expansion/contraction, and this
     *     technique would silently give WRONG answers rather than merely
     *     being slow — this is the sharpest edge of the approach.
     *
     * When to use:
     *   - This is the production-quality answer for the stated problem
     *     (positive integers only). Always prefer this in an interview
     *     once you've confirmed the positive-only constraint.
     */
    public static int minSubArrayLenSlidingWindow(int[] nums, int target) {
        if (nums == null || nums.length == 0) {
            return 0;
        }

        int leftPointer = 0;
        long windowSum = 0;
        int minimumLength = Integer.MAX_VALUE;

        for (int rightPointer = 0; rightPointer < nums.length; rightPointer++) {
            windowSum += nums[rightPointer];

            // Shrink from the left greedily while the window still qualifies.
            while (windowSum >= target) {
                int currentWindowLength = rightPointer - leftPointer + 1;
                minimumLength = Math.min(minimumLength, currentWindowLength);
                windowSum -= nums[leftPointer];
                leftPointer++;
            }
        }

        return (minimumLength == Integer.MAX_VALUE) ? 0 : minimumLength;
    }

    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * | Approach                          | Time       | Space | Best For                                   | Limitations                                          |
     * |------------------------------------|-----------|-------|--------------------------------------------|-------------------------------------------------------|
     * | 1. Brute Force                     | O(n^2)    | O(1)  | Baseline correctness oracle, tiny inputs   | TLE on large n; not production-viable                 |
     * | 2. Prefix Sum + Binary Search       | O(n log n)| O(n)  | Demonstrating prefix-sum/binary-search fluency; generalizes to sum queries | Dominated by Approach 3 for this exact problem; extra space |
     * | 3. Sliding Window / Two Pointer     | O(n)      | O(1)  | Production-quality final answer            | Requires strictly positive elements to remain correct |
     */

    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would present the solutions in this order:
     *
     *   1. State the brute force O(n^2) approach briefly out loud (to show
     *      I understand the problem space and have a fallback), but I
     *      would NOT fully code it unless asked, since it's obviously
     *      correct and time is better spent on the optimal solution.
     *
     *   2. Immediately point out the "positive integers only" constraint
     *      as the key unlock, and pivot to the Sliding Window / Two
     *      Pointer approach (Approach 3) as my primary, fully-coded
     *      answer.
     *
     *   3. If asked "can you do this without exploiting the positive-only
     *      constraint, or in a way that generalizes better?", I'd mention
     *      the Prefix Sum + Binary Search approach (Approach 2) as the
     *      natural fallback — e.g., useful if zeros were allowed (window
     *      sum becomes non-decreasing but not strictly increasing, and a
     *      slightly adjusted binary search still works cleanly), or if we
     *      needed to answer many such queries efficiently.
     *
     * Why Approach 3 is the right primary answer:
     *   - Optimal in both time (O(n)) and space (O(1)) — nothing beats it
     *     asymptotically for this exact problem.
     *   - Fast to code correctly under interview time pressure: it's a
     *     single pass with a simple invariant ("shrink while sufficient").
     *   - Directly showcases the core algorithmic insight the interviewer
     *     is testing (recognizing when two-pointer techniques are valid),
     *     which is exactly what differentiates a strong signal from a
     *     merely-correct answer.
     */

    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE — PRODUCTION-QUALITY OPTIMAL SOLUTION
     * ========================================================================
     *
     * This is the polished version of Approach 3, with full Javadoc,
     * named constants, and defensive input validation, suitable to
     * present as the final answer.
     */

    /** Sentinel indicating that no qualifying subarray was found. */
    private static final int NO_QUALIFYING_SUBARRAY = 0;

    /**
     * Finds the length of the shortest contiguous subarray of {@code nums}
     * whose sum is greater than or equal to {@code target}.
     *
     * <p>Algorithm: sliding window / two pointers. Because every element
     * of {@code nums} is strictly positive, the running sum of the window
     * increases monotonically whenever the window expands (moving the
     * right pointer) and decreases monotonically whenever the window
     * shrinks (moving the left pointer). This monotonicity is what makes
     * the greedy "shrink while still sufficient" strategy correct: once a
     * window's sum meets the target, we know removing elements from its
     * left side can only ever reduce the sum, so we shrink until it no
     * longer qualifies, recording the shortest qualifying length seen so
     * far along the way.
     *
     * @param nums   array of strictly positive integers; may be empty or
     *               {@code null}, in which case {@link #NO_QUALIFYING_SUBARRAY}
     *               is returned
     * @param target strictly positive threshold sum to reach or exceed
     * @return the minimum length of a contiguous subarray with sum
     *         {@code >= target}, or {@code 0} if no such subarray exists
     * @throws IllegalArgumentException if {@code target} is not positive
     */
    public static int minSubArrayLen(int[] nums, int target) {
        if (target <= 0) {
            throw new IllegalArgumentException("target must be a positive integer, got: " + target);
        }
        if (nums == null || nums.length == 0) {
            return NO_QUALIFYING_SUBARRAY;
        }

        int leftPointer = 0;          // Left edge of the current window (inclusive).
        long windowSum = 0;           // Sum of nums[leftPointer..rightPointer], using long
                                       // defensively to guard against overflow on large inputs.
        int shortestQualifyingLength = Integer.MAX_VALUE;

        for (int rightPointer = 0; rightPointer < nums.length; rightPointer++) {
            windowSum += nums[rightPointer]; // Expand the window to include nums[rightPointer].

            // While the window is "sufficient" (sum >= target), it's always
            // beneficial to try shrinking it from the left: a shorter
            // sufficient window can never be a worse answer than a longer one.
            while (windowSum >= target) {
                int currentWindowLength = rightPointer - leftPointer + 1;
                if (currentWindowLength < shortestQualifyingLength) {
                    shortestQualifyingLength = currentWindowLength;
                }
                windowSum -= nums[leftPointer]; // Remove the leftmost element...
                leftPointer++;                  // ...and advance the left edge.
            }
        }

        return (shortestQualifyingLength == Integer.MAX_VALUE)
                ? NO_QUALIFYING_SUBARRAY
                : shortestQualifyingLength;
    }

    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     *
     * Tracing minSubArrayLen(nums = [2, 3, 1, 2, 4, 3], target = 7):
     *
     * Initial state: leftPointer = 0, windowSum = 0, shortestQualifyingLength = MAX
     *
     * rightPointer=0 (nums[0]=2): windowSum = 0+2 = 2.        2 < 7, no shrink.
     *   State: left=0, sum=2, best=MAX
     *
     * rightPointer=1 (nums[1]=3): windowSum = 2+3 = 5.        5 < 7, no shrink.
     *   State: left=0, sum=5, best=MAX
     *
     * rightPointer=2 (nums[2]=1): windowSum = 5+1 = 6.        6 < 7, no shrink.
     *   State: left=0, sum=6, best=MAX
     *
     * rightPointer=3 (nums[3]=2): windowSum = 6+2 = 8.        8 >= 7 -> qualifies!
     *   currentWindowLength = 3-0+1 = 4. best = min(MAX,4) = 4.
     *   Shrink: windowSum -= nums[0]=2 -> windowSum = 6. leftPointer = 1.
     *   6 < 7, stop shrinking.
     *   State: left=1, sum=6, best=4
     *
     * rightPointer=4 (nums[4]=4): windowSum = 6+4 = 10.       10 >= 7 -> qualifies!
     *   currentWindowLength = 4-1+1 = 4. best = min(4,4) = 4.
     *   Shrink: windowSum -= nums[1]=3 -> windowSum = 7. leftPointer = 2.
     *   7 >= 7 -> still qualifies!
     *   currentWindowLength = 4-2+1 = 3. best = min(4,3) = 3.
     *   Shrink: windowSum -= nums[2]=1 -> windowSum = 6. leftPointer = 3.
     *   6 < 7, stop shrinking.
     *   State: left=3, sum=6, best=3
     *
     * rightPointer=5 (nums[5]=3): windowSum = 6+3 = 9.        9 >= 7 -> qualifies!
     *   currentWindowLength = 5-3+1 = 3. best = min(3,3) = 3.
     *   Shrink: windowSum -= nums[3]=2 -> windowSum = 7. leftPointer = 4.
     *   7 >= 7 -> still qualifies!
     *   currentWindowLength = 5-4+1 = 2. best = min(3,2) = 2.
     *   Shrink: windowSum -= nums[4]=4 -> windowSum = 3. leftPointer = 5.
     *   3 < 7, stop shrinking.
     *   State: left=5, sum=3, best=2
     *
     * Loop ends (rightPointer exhausted). shortestQualifyingLength = 2.
     * Return 2.  <-- matches expected answer (the subarray [4, 3]).
     */

    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * - Brute force (O(n^2)/O(1)) is the simplest to reason about and
     *   useful as a correctness oracle, but too slow for real inputs.
     * - Prefix sum + binary search (O(n log n)/O(n)) is a solid middle
     *   ground that showcases algorithmic range, and is the natural
     *   fallback if the positive-only constraint were relaxed to allow
     *   zeros, or if repeated range-sum queries were needed.
     * - Sliding window / two pointer (O(n)/O(1)) is the fully optimal,
     *   production-quality solution for this exact problem, and is what
     *   I'd present as my primary, fully-coded answer.
     *
     * Known limitations / assumptions of the final solution:
     *   - Assumes every element of `nums` is strictly positive; if
     *     negative numbers or zeros can appear, the sliding-window
     *     monotonicity assumption breaks and a different technique
     *     (e.g., prefix sums with a monotonic deque, as in LeetCode 862)
     *     is required instead.
     *   - Assumes `target` is a positive integer (validated defensively
     *     via an IllegalArgumentException).
     *   - Uses `long` for the running sum defensively to avoid integer
     *     overflow on large arrays with large element values, even though
     *     the problem doesn't explicitly guarantee this is necessary.
     */

    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     *
     * 1. "What if `nums` could contain negative numbers or zeros?"
     *    -> Sliding window breaks down (sum isn't monotonic); would need
     *       prefix sums combined with a monotonic deque to maintain the
     *       minimum prefix seen in a valid range (LeetCode 862 pattern).
     *
     * 2. "What if you needed the actual subarray (start/end indices), not
     *     just its length?"
     *    -> Trivial extension: track leftPointer/rightPointer at the
     *       moment the best length is updated, instead of just the length.
     *
     * 3. "What if we needed to answer this query repeatedly for many
     *     different `target` values on the same fixed `nums` array?"
     *    -> Precompute the prefix sum array once (O(n)), then answer each
     *       query in O(n) (or O(log n) per query using binary search over
     *       the sorted prefix array for the two-pointer approach adapted
     *       per query) — amortizing the preprocessing cost across queries.
     *
     * 4. "How would you scale this to a distributed / streaming setting
     *     where `nums` arrives one element at a time and you must report
     *     the running answer continuously?"
     *    -> The sliding window approach naturally adapts to streaming:
     *       maintain leftPointer, windowSum, and shortestQualifyingLength
     *       as persistent state, updating them incrementally as each new
     *       element arrives, without needing to re-scan prior elements.
     *
     * 5. "What if you needed the MAXIMUM length subarray with sum <=
     *     target instead of the minimum length with sum >= target?"
     *    -> Still solvable with a similar two-pointer template, but the
     *       shrink condition and what's tracked as "best" flip: expand
     *       while sum <= target, tracking max length, and shrink only
     *       when sum exceeds target.
     *
     * 6. "Can you solve this with O(1) extra space but without the
     *     positive-only guarantee, in O(n log(sum)) time instead?"
     *    -> Yes, conceptually: binary search on the candidate WINDOW
     *       LENGTH itself, using a fixed-size sliding window (or deque)
     *       to check feasibility for each candidate length — though this
     *       is more naturally suited to different variants of the problem
     *       (e.g., "maximum average subarray of length >= k").
     */

    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Forgetting to greedily shrink the window fully. A common bug is
     *    using an `if (windowSum >= target)` instead of a `while
     *    (windowSum >= target)` — this only shrinks by one element and
     *    misses shorter qualifying windows that exist further inside the
     *    current window.
     *
     * 2. Off-by-one errors in window length calculation. It's easy to
     *    write `rightPointer - leftPointer` instead of the correct
     *    `rightPointer - leftPointer + 1` (window boundaries are
     *    inclusive on both ends).
     *
     * 3. Silently assuming the technique generalizes to arrays with
     *    zeros or negative numbers. Candidates often apply the same
     *    sliding-window template to a variant problem without noticing
     *    that monotonicity has been broken, producing subtly wrong
     *    answers rather than an obvious crash.
     *
     * 4. Integer overflow on the running/prefix sum for large inputs with
     *    large element values — using `int` instead of `long` for the
     *    accumulator can silently wrap around and produce incorrect
     *    results without any exception being thrown.
     */

    /*
     * ========================================================================
     * TEST HARNESS — cross-validates all three approaches against a shared
     * set of test cases, including the walked-through examples and edge
     * cases from Section 3.
     * ========================================================================
     */
    public static void main(String[] args) {
        record TestCase(int[] nums, int target, int expected, String description) {}

        List<TestCase> testCases = List.of(
                new TestCase(new int[]{2, 3, 1, 2, 4, 3}, 7, 2,
                        "Normal case (Example 1)"),
                new TestCase(new int[]{1, 1, 1, 1, 1, 1, 1, 1}, 11, 0,
                        "No qualifying subarray (Example 2)"),
                new TestCase(new int[]{1, 2, 3, 4, 5}, 15, 5,
                        "Only the full array qualifies (Example 3)"),
                new TestCase(new int[]{1, 4, 4}, 4, 1,
                        "Single element meets target exactly"),
                new TestCase(new int[]{5}, 5, 1,
                        "Single-element array, nums[0] == target"),
                new TestCase(new int[]{4}, 5, 0,
                        "Single-element array, nums[0] < target"),
                new TestCase(new int[]{10, 2, 3}, 6, 1,
                        "First element alone exceeds target"),
                new TestCase(new int[]{1, 2, 3, 4, 5}, 100, 0,
                        "Target far exceeds total array sum"),
                new TestCase(new int[]{}, 5, 0,
                        "Empty array"),
                new TestCase(new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, 3, 3,
                        "Many small equal elements")
        );

        int passCount = 0;
        for (TestCase testCase : testCases) {
            int bruteForceResult = minSubArrayLenBruteForce(testCase.nums(), testCase.target());
            int binarySearchResult = minSubArrayLenPrefixBinarySearch(testCase.nums(), testCase.target());
            int slidingWindowResult = minSubArrayLenSlidingWindow(testCase.nums(), testCase.target());
            int productionResult = minSubArrayLen(testCase.nums(), testCase.target());

            boolean allMatch = bruteForceResult == testCase.expected()
                    && binarySearchResult == testCase.expected()
                    && slidingWindowResult == testCase.expected()
                    && productionResult == testCase.expected();

            if (allMatch) {
                passCount++;
            }

            System.out.printf(
                    "[%s] %-45s nums=%-30s target=%-4d expected=%-3d brute=%-3d binSearch=%-3d slidingWindow=%-3d production=%-3d%n",
                    allMatch ? "PASS" : "FAIL",
                    testCase.description(),
                    Arrays.toString(testCase.nums()),
                    testCase.target(),
                    testCase.expected(),
                    bruteForceResult,
                    binarySearchResult,
                    slidingWindowResult,
                    productionResult
            );
        }

        System.out.printf("%n%d / %d test cases passed.%n", passCount, testCases.size());
    }
}
