import java.util.*;

/**
 * ============================================================================
 * GOOGLE-STYLE MOCK INTERVIEW: Count Subarrays With Score Less Than K
 * (LeetCode 2302 family)
 * ============================================================================
 *
 * This single file walks through the full interview process end-to-end:
 * problem restatement, clarifying questions, examples, every viable
 * approach (naive -> optimal), a comparison table, the recommended
 * approach, a production-quality deep dive, a manual trace, follow-ups,
 * and common candidate mistakes.
 *
 * Run `main` to see the dry-run examples executed against all four
 * implemented approaches for cross-validation.
 */
class CountSubarraysScoreLessThanK {

    /*
     * ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     *
     * We are given:
     *   - an array `nums` of POSITIVE integers (no zeros, no negatives)
     *   - a positive integer `k`
     *
     * Define the "score" of any array (or subarray) as:
     *
     *      score(subarray) = (sum of its elements) * (length of subarray)
     *
     * Example: [2, 1, 5] -> score = (2 + 1 + 5) * 3 = 8 * 3 = 24
     *
     * We must count how many NON-EMPTY, CONTIGUOUS subarrays of `nums`
     * have a score that is STRICTLY LESS THAN k.
     *
     * Output: a single count (an integer/long — see clarifying questions
     * about why `long` matters).
     *
     * Key observations up front (before even discussing approaches):
     *   1. Because every element is POSITIVE, prefix sums are strictly
     *      increasing. This monotonicity is the single most important
     *      structural fact for this problem — it is what unlocks the
     *      O(n) sliding-window solution and the O(n log n) binary-search
     *      solution.
     *   2. For a FIXED left boundary, as the right boundary moves right,
     *      both the sum and the length only grow, so the score is
     *      monotonically non-decreasing (in fact strictly increasing,
     *      since elements are positive). This means: for each left
     *      index, the set of valid right indices forms a contiguous
     *      prefix range [left, someBoundary]. That's exactly the
     *      condition that makes sliding window / binary search valid.
     */

    /*
     * ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
     * ========================================================================
     *
     * Q1. What is the maximum length of `nums`?
     *     A: Assume up to 10^5 elements (typical competitive-programming
     *        bound). This rules out O(n^3) as a production solution but
     *        it's still fine to mention/derive as the naive baseline.
     *
     * Q2. What is the range of values in `nums`?
     *     A: Assume 1 <= nums[i] <= 10^5 (strictly positive, per problem
     *        statement — no zeros, no negatives).
     *
     * Q3. What is the range of `k`?
     *     A: Assume 1 <= k <= 10^15. Since sum can be up to 10^5 * 10^5 =
     *        10^10 and length up to 10^5, score can be up to ~10^15, so
     *        k must be represented as a `long`, and so must our running
     *        sums and score products (to avoid overflow if we used int).
     *
     * Q4. What should the return type be — int or long?
     *     A: `long`. The COUNT of subarrays itself can also exceed
     *        Integer.MAX_VALUE: for n = 10^5, the total number of
     *        subarrays is n*(n+1)/2 ≈ 5*10^9, which overflows a 32-bit
     *        int. So both the score arithmetic AND the final count must
     *        use `long`.
     *
     * Q5. Can `nums` contain duplicate values?
     *     A: Yes, duplicates are allowed and require no special handling
     *        — we only ever look at sums, not identities.
     *
     * Q6. Is the array sorted, or can it be in any order?
     *     A: Assume arbitrary order, unsorted. (Sorting would destroy
     *        the "contiguous subarray" semantics anyway, so it's not a
     *        valid preprocessing step here — this is a useful thing to
     *        say out loud in the interview to show you understand why
     *        sorting-based approaches don't apply.)
     *
     * Q7. Do we need to return the subarrays themselves, or just the count?
     *     A: Just the count.
     *
     * Q8. Should I worry about concurrency / thread safety / streaming
     *     input (i.e., is `nums` provided all at once)?
     *     A: No — `nums` is provided fully in memory upfront, single-
     *        threaded, no streaming constraints.
     */

    /*
     * ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (Normal case):
     *   nums = [2, 1, 4, 3, 5], k = 10
     *   Length-1 subarrays: [2],[1],[4],[3],[5] -> scores 2,1,4,3,5, all < 10
     *     => 5 valid subarrays
     *   Length-2 subarrays: [2,1]->6, [1,4]->10 (NOT < 10), [4,3]->14, [3,5]->16
     *     => only [2,1] valid => 1 more valid subarray
     *   Length-3+: all scores >= 10 (e.g. [2,1,4]->21)
     *   TOTAL = 5 + 1 = 6
     *
     * Example 2 (Edge case — single element array):
     *   nums = [5], k = 1
     *   Only subarray is [5], score = 5*1 = 5, which is NOT < 1.
     *   TOTAL = 0
     *   (Tests: smallest possible input size, and the "no valid answer"
     *   case — count can legitimately be zero.)
     *
     * Example 3 (Boundary / tie-breaking case — score exactly equals k):
     *   nums = [1, 1, 1], k = 3
     *   Length-1: [1],[1],[1] -> scores 1,1,1, all < 3 => 3 valid
     *   Length-2: [1,1] -> sum=2, score=2*2=4, NOT < 3
     *   Length-3: [1,1,1] -> sum=3, score=3*3=9, NOT < 3
     *   TOTAL = 3
     *   This demonstrates the STRICT inequality: a subarray whose score
     *   is exactly k (or would tie some other boundary) is EXCLUDED —
     *   candidates commonly get this off-by-one wrong by using <= instead
     *   of <.
     */

    /*
     * ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES
     * (naive -> optimal, annotated with paradigm)
     * ========================================================================
     *
     * Paradigms considered and explicitly RULED OUT (per interview best
     * practice of doing a full paradigm sweep out loud):
     *
     *   - Sorting-based: Sorting destroys contiguity/order, which is
     *     essential to the definition of "subarray." Not applicable.
     *   - Dynamic Programming (classic tabulation): There's no useful
     *     overlapping-subproblem recurrence beyond what a simple running
     *     sum already gives us; sliding window already captures the only
     *     "state" that matters (current window sum). Introducing a DP
     *     table would add space/complexity with no benefit. Not applicable
     *     in a way that beats the two-pointer solution.
     *   - Heap / Priority Queue: There's no "top-k" or ordering-extraction
     *     requirement here — we need ALL qualifying subarrays counted, not
     *     the k smallest/largest scores. Not applicable.
     *   - Tree / Graph traversal: There's no graph or hierarchical
     *     structure implied by the problem. Not applicable.
     *   - Monotonic stack/deque: Monotonic stacks solve "next greater/
     *     smaller element" style problems; this problem's structure
     *     (contiguous window with a monotonic score function) is a
     *     better fit for two pointers. Not applicable.
     *   - Trie / Segment tree: These shine for range queries with updates
     *     or prefix-matching over strings. Here, a simple prefix-sum array
     *     already gives O(1) range-sum queries with no updates needed —
     *     a segment tree would be strictly more machinery for zero benefit.
     *     Not applicable.
     *   - Divide & Conquer: A D&C formulation IS possible in theory (solve
     *     left half, right half, then count "crossing" subarrays using a
     *     merge-style two-pointer sweep, similar to counting inversions),
     *     yielding O(n log n). It's mentioned here for completeness but
     *     not implemented in full, because it is strictly more complex
     *     to code correctly under interview time pressure than the
     *     sliding-window solution, for the same or worse complexity.
     *
     * Paradigms that ARE genuinely applicable and implemented below:
     *   - Brute Force (nested loops)
     *   - Brute Force + running sum (a hashing/prefix-sum flavored cleanup)
     *   - Binary Search (exploiting monotonicity of prefix sums)
     *   - Two Pointer / Sliding Window (the optimal, greedy-flavored solution)
     */

    /**
     * ------------------------------------------------------------------
     * Approach 1: Brute Force — Triple Nested Loop
     * ------------------------------------------------------------------
     * Core idea: Literally try every (start, end) pair, and for each pair
     * recompute the subarray sum from scratch by an inner loop.
     *
     * Paradigm: Exhaustive enumeration (no data structure beyond the
     * array itself).
     *
     * Time Complexity: O(n^3)
     *   - O(n^2) pairs of (start, end)
     *   - O(n) to recompute the sum for each pair
     * Space Complexity: O(1) extra space.
     *
     * Pros:
     *   - Trivial to reason about and verify correct; good as a
     *     "correctness oracle" to cross-validate faster solutions.
     * Cons:
     *   - Far too slow for n = 10^5 (10^15 operations) — would never
     *     pass real constraints.
     *
     * When to use: Only for tiny inputs, unit-test oracles, or as a
     * warm-up while thinking out loud in the interview before diving
     * into something smarter. Never in production.
     */
    public static long bruteForceTripleLoop(int[] nums, long k) {
        int n = nums.length;
        long count = 0;
        for (int start = 0; start < n; start++) {
            for (int end = start; end < n; end++) {
                long sum = 0;
                // Recompute sum from scratch every single time — the
                // wasteful part that the next approach eliminates.
                for (int index = start; index <= end; index++) {
                    sum += nums[index];
                }
                long length = end - start + 1;
                long score = sum * length;
                if (score < k) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * ------------------------------------------------------------------
     * Approach 2: Brute Force + Running Sum (Prefix-Sum Flavored Cleanup)
     * ------------------------------------------------------------------
     * Core idea: Still try every (start, end) pair, but avoid recomputing
     * the sum from scratch each time — maintain a running sum as `end`
     * advances for a fixed `start`. This is the natural first optimization
     * an interviewer expects you to make instantly.
     *
     * Paradigm: Prefix-sum bookkeeping (conceptually equivalent to using
     * a precomputed prefix-sum array, just computed incrementally inline).
     *
     * Time Complexity: O(n^2) — one pass over `end` for every `start`.
     * Space Complexity: O(1) extra space.
     *
     * Pros:
     *   - Much better than triple-loop; simple to code correctly.
     *   - A completely reasonable "first working solution" to present
     *     verbally before optimizing further.
     * Cons:
     *   - Still O(n^2) — will not scale to n = 10^5 (10^10 operations).
     *
     * When to use: Medium-sized inputs (n up to ~10^4), or as a safety-net
     * solution to state and verify correctness before pivoting to the
     * optimal approach — mirrors the "present a safe solution, then push
     * for the optimal one" interview strategy.
     */
    public static long bruteForcePrefixSum(int[] nums, long k) {
        int n = nums.length;
        long count = 0;
        for (int start = 0; start < n; start++) {
            long runningSum = 0;
            for (int end = start; end < n; end++) {
                runningSum += nums[end]; // extend the window by one element
                long length = end - start + 1;
                long score = runningSum * length;
                if (score < k) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * ------------------------------------------------------------------
     * Approach 3: Binary Search on Prefix Sums
     * ------------------------------------------------------------------
     * Core idea: Precompute prefix sums (prefix[i] = sum of nums[0..i-1]).
     * Because all elements are positive, for a FIXED `start`, the score
     * as a function of `end` is STRICTLY INCREASING as `end` grows. That
     * means the set of valid `end` values for a fixed `start` is a
     * contiguous prefix range [start, boundary]. We binary search for the
     * largest `end` such that the score is still < k.
     *
     * Paradigm: Binary search over a monotonic predicate, backed by a
     * prefix-sum array for O(1) range-sum queries.
     *
     * Time Complexity: O(n log n) — for each of n starting points, binary
     * search over up to n ending points.
     * Space Complexity: O(n) for the prefix-sum array.
     *
     * Pros:
     *   - Demonstrates strong grasp of monotonicity and binary search
     *     over "search space", a very Google-favored technique.
     *   - Asymptotically close to optimal.
     * Cons:
     *   - Strictly more complex to implement correctly than the sliding
     *     window (must get binary-search boundary conditions right).
     *   - Slower than the O(n) sliding window solution for no added
     *     benefit — mentioning it shows depth, but it should not be your
     *     final answer once the interviewer confirms O(n) is expected.
     *
     * When to use: Great to mention as an alternative to demonstrate
     * breadth, or if for some reason the sliding-window invariant were
     * harder to maintain (e.g., if the "window shrink" step had extra
     * side effects). Not preferred here since O(n) is achievable.
     */
    public static long binarySearchOnPrefixSums(int[] nums, long k) {
        int n = nums.length;
        long[] prefix = new long[n + 1]; // prefix[i] = sum of nums[0..i-1]
        for (int index = 0; index < n; index++) {
            prefix[index + 1] = prefix[index] + nums[index];
        }

        long count = 0;
        for (int start = 0; start < n; start++) {
            int low = start;
            int high = n - 1;
            int bestEnd = start - 1; // sentinel: "no valid end found yet"

            // Binary search for the rightmost `end` such that
            // score(start, end) < k.
            while (low <= high) {
                int mid = low + (high - low) / 2;
                long sum = prefix[mid + 1] - prefix[start];
                long length = mid - start + 1;
                long score = sum * length;
                if (score < k) {
                    bestEnd = mid;   // mid is valid; try to extend further right
                    low = mid + 1;
                } else {
                    high = mid - 1;  // mid is too big; shrink search space left
                }
            }

            if (bestEnd >= start) {
                count += (bestEnd - start + 1); // all ends in [start, bestEnd] are valid
            }
        }
        return count;
    }

    /**
     * ------------------------------------------------------------------
     * Approach 4 (OPTIMAL): Two Pointer / Sliding Window
     * ------------------------------------------------------------------
     * Core idea: Maintain a window [left, right] and its running sum.
     * Expand `right` one step at a time. Whenever the current window's
     * score (sum * length) is >= k, shrink from the left (subtract
     * nums[left], advance left) until the score is valid again. Because
     * all elements are positive, both sum and length only ever grow as
     * `right` advances and only ever shrink as `left` advances — this
     * monotonic behavior is EXACTLY what makes the two-pointer technique
     * correct and efficient: `left` only ever moves forward, never resets.
     *
     * Crucial counting insight: for each fixed `right`, once the window
     * [left, right] is valid (score < k), EVERY subarray ending at
     * `right` with a start in [left, right] is also valid (because
     * shrinking the window from the left only decreases sum and length,
     * so the score only decreases further below k). That means we can
     * add (right - left + 1) to our running count at each step, instead
     * of re-checking each of those subarrays individually.
     *
     * Paradigm: Two-pointer / sliding window (greedy expansion + greedy
     * shrink, exploiting monotonicity).
     *
     * Time Complexity: O(n). Although there's a nested while-loop inside
     * the for-loop, `left` only ever increases and can move at most n
     * times total across the ENTIRE run of the algorithm (not per
     * iteration of `right`) — so the amortized total work is O(n), not
     * O(n^2). This is the classic "amortized/aggregate analysis" argument
     * for two-pointer algorithms.
     * Space Complexity: O(1) extra space (only a few running variables).
     *
     * Pros:
     *   - Optimal time complexity, minimal space.
     *   - Clean, short, and easy to explain/trace live in an interview.
     * Cons:
     *   - Requires recognizing the monotonicity property up front; if
     *     elements could be negative or zero, this technique would break
     *     (sum would no longer be monotonic in window size), so it's
     *     important to state this assumption explicitly.
     *
     * When to use: This is the production-quality answer for this
     * problem given the "positive integers" constraint. Always prefer
     * this once you've confirmed the constraint holds.
     */
    public static long slidingWindowOptimal(int[] nums, long k) {
        int n = nums.length;
        int left = 0;
        long windowSum = 0;
        long count = 0;

        for (int right = 0; right < n; right++) {
            windowSum += nums[right]; // expand the window to include nums[right]

            long windowLength = right - left + 1;
            // Shrink from the left while the current window's score is
            // NOT strictly less than k. Since nums are positive, removing
            // nums[left] strictly decreases both sum and length, so this
            // loop is guaranteed to terminate (worst case: left == right + 1,
            // i.e., empty window, score = 0 < k trivially once k > 0).
            while (windowSum * windowLength >= k) {
                windowSum -= nums[left];
                left++;
                windowLength = right - left + 1;
            }

            // At this point, [left, right] is the largest valid window
            // ending at `right`. Every subarray [start, right] for
            // start in [left, right] is also valid, so add all of them.
            count += windowLength;
        }
        return count;
    }

    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * | Approach                          | Time       | Space | Best For                          | Limitations                                   |
     * |------------------------------------|------------|-------|-----------------------------------|------------------------------------------------|
     * | 1. Brute Force (triple loop)       | O(n^3)     | O(1)  | Tiny n, correctness oracle         | Unusable beyond n ~ few hundred                 |
     * | 2. Brute Force + running sum       | O(n^2)     | O(1)  | Medium n (~10^4), quick first pass | Still too slow for n = 10^5                     |
     * | 3. Binary search on prefix sums    | O(n log n) | O(n)  | Showing depth/breadth of technique | More complex than needed; strictly slower than #4 |
     * | 4. Sliding window (two pointer)    | O(n)       | O(1)  | Production / large n, THE answer   | Requires positive-elements assumption to hold   |
     */

    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * Recommended: Approach 4 — Sliding Window (Two Pointer), O(n) / O(1).
     *
     * Why:
     *   - It is asymptotically optimal — you cannot do better than O(n)
     *     since you must at least read every element once.
     *   - It is SHORT to code correctly under interview time pressure
     *     (roughly 15 lines of core logic), which matters a lot for
     *     "coding speed" evaluation.
     *   - It cleanly demonstrates the single most important insight in
     *     the problem (monotonicity from positive elements), which is
     *     exactly what a Google interviewer is listening for — not just
     *     "does it work" but "did the candidate spot WHY it works."
     *   - Interview strategy: present Approach 2 (brute force + running
     *     sum) FIRST as a safe, obviously-correct baseline in ~2 minutes,
     *     explicitly state its O(n^2) complexity, then say "given that
     *     elements are strictly positive, I can exploit monotonicity to
     *     get this down to O(n) with a sliding window" and pivot to
     *     Approach 4. This mirrors the "safe solution first, then push
     *     for optimal" pattern that reads well to an interviewer.
     */

    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE — PRODUCTION-QUALITY OPTIMAL SOLUTION
     * ========================================================================
     */

    /**
     * Counts the number of non-empty contiguous subarrays of {@code nums}
     * whose score — defined as (sum of elements) multiplied by (length) —
     * is strictly less than {@code k}.
     *
     * <p>Algorithm: two-pointer sliding window. Exploits the fact that all
     * elements of {@code nums} are strictly positive, which guarantees that
     * for a fixed left boundary, the score is a strictly increasing function
     * of the right boundary. This means the window can be maintained with a
     * monotonically advancing left pointer, giving an overall O(n) runtime.
     *
     * @param nums array of strictly positive integers (1 <= nums[i])
     * @param k    strictly positive threshold; count subarrays with score < k
     * @return the number of qualifying subarrays, as a {@code long} (the
     *         count can exceed {@code Integer.MAX_VALUE} for large inputs)
     * @throws IllegalArgumentException if {@code nums} is null/empty, if any
     *                                  element is not positive, or if
     *                                  {@code k} is not positive
     */
    public static long countSubarraysWithScoreLessThanK(int[] nums, long k) {
        // Defensive checks: production code should never silently trust
        // its inputs, especially when constraints (positivity) are load-
        // bearing for algorithm correctness.
        if (nums == null || nums.length == 0) {
            throw new IllegalArgumentException("nums must be a non-empty array");
        }
        if (k <= 0) {
            throw new IllegalArgumentException("k must be a positive integer");
        }
        for (int value : nums) {
            if (value <= 0) {
                throw new IllegalArgumentException(
                        "All elements of nums must be strictly positive integers");
            }
        }

        final int n = nums.length;
        int left = 0;              // left boundary of the current window (inclusive)
        long windowSum = 0;        // sum of nums[left..right]
        long totalCount = 0;       // running count of valid subarrays

        for (int right = 0; right < n; right++) {
            windowSum += nums[right];

            // Shrink the window from the left while its score is too big.
            // Correctness relies on positivity: removing nums[left] always
            // strictly decreases both windowSum and the window length, so
            // the score strictly decreases too, guaranteeing termination.
            while (windowSum * (long) (right - left + 1) >= k) {
                windowSum -= nums[left];
                left++;
            }

            // Every subarray ending at `right` with a start anywhere in
            // [left, right] has score < k (shrinking the start only
            // reduces sum and length further, so the score only drops).
            // That's (right - left + 1) newly-counted valid subarrays.
            totalCount += (right - left + 1);
        }

        return totalCount;
    }

    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     *
     * Tracing countSubarraysWithScoreLessThanK on nums = [2, 1, 4, 3, 5], k = 10.
     *
     * Initial state: left = 0, windowSum = 0, totalCount = 0
     *
     * right = 0 (nums[0] = 2):
     *   windowSum = 0 + 2 = 2
     *   length = right - left + 1 = 1; score = 2 * 1 = 2 -> 2 < 10, no shrink
     *   totalCount += (0 - 0 + 1) = 1  -> totalCount = 1
     *   state: left=0, windowSum=2
     *
     * right = 1 (nums[1] = 1):
     *   windowSum = 2 + 1 = 3
     *   length = 2; score = 3 * 2 = 6 -> 6 < 10, no shrink
     *   totalCount += (1 - 0 + 1) = 2  -> totalCount = 3
     *   state: left=0, windowSum=3
     *
     * right = 2 (nums[2] = 4):
     *   windowSum = 3 + 4 = 7
     *   length = 3; score = 7 * 3 = 21 -> 21 >= 10, SHRINK:
     *     remove nums[0]=2 -> windowSum = 5, left = 1
     *     length = 2; score = 5 * 2 = 10 -> still >= 10, SHRINK:
     *       remove nums[1]=1 -> windowSum = 4, left = 2
     *       length = 1; score = 4 * 1 = 4 -> 4 < 10, stop shrinking
     *   totalCount += (2 - 2 + 1) = 1  -> totalCount = 4
     *   state: left=2, windowSum=4
     *
     * right = 3 (nums[3] = 3):
     *   windowSum = 4 + 3 = 7
     *   length = 2; score = 7 * 2 = 14 -> 14 >= 10, SHRINK:
     *     remove nums[2]=4 -> windowSum = 3, left = 3
     *     length = 1; score = 3 * 1 = 3 -> 3 < 10, stop shrinking
     *   totalCount += (3 - 3 + 1) = 1  -> totalCount = 5
     *   state: left=3, windowSum=3
     *
     * right = 4 (nums[4] = 5):
     *   windowSum = 3 + 5 = 8
     *   length = 2; score = 8 * 2 = 16 -> 16 >= 10, SHRINK:
     *     remove nums[3]=3 -> windowSum = 5, left = 4
     *     length = 1; score = 5 * 1 = 5 -> 5 < 10, stop shrinking
     *   totalCount += (4 - 4 + 1) = 1  -> totalCount = 6
     *   state: left=4, windowSum=5
     *
     * Final answer: totalCount = 6  (matches Example 1's expected result)
     */

    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * - The naive O(n^3) and O(n^2) brute-force approaches are correct but
     *   do not scale; they're useful as verification oracles and as a
     *   safe first solution to state out loud before optimizing.
     * - Binary search on prefix sums (O(n log n)) is a solid alternative
     *   that demonstrates the same monotonicity insight in a different
     *   form, but adds implementation complexity without improving on
     *   the sliding window's O(n).
     * - The sliding window / two-pointer approach (O(n) time, O(1) space)
     *   is optimal and is the recommended final answer.
     * - Known assumption/limitation of the final solution: it relies
     *   entirely on every element of `nums` being strictly positive. If
     *   zero or negative values were allowed, the monotonicity argument
     *   breaks and this exact technique would no longer be correct (see
     *   Follow-Up Questions below for how the problem would need to
     *   change).
     * - The count itself, and all intermediate sum/score computations,
     *   must use `long` — this is a real production concern, not just an
     *   academic one, given realistic constraint sizes (n, nums[i] ~ 10^5).
     */

    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     *
     * 1. "What if `nums` could contain zero or negative integers?"
     *    -> The sliding window breaks because sum is no longer monotonic
     *       in window size; you'd likely need a different technique (e.g.,
     *       a Fenwick/BIT-based counting approach over prefix sums, or
     *       accept O(n^2) since the strict monotonicity that enables
     *       O(n)/O(n log n) is lost).
     *
     * 2. "Can you count subarrays with score STRICTLY GREATER THAN k instead?"
     *    -> Compute total subarrays (n*(n+1)/2) and subtract the count of
     *       subarrays with score <= k (a small modification: change the
     *       shrink condition's strict/non-strict boundary).
     *
     * 3. "What if you needed to answer many queries with different values
     *    of k on the same fixed `nums`?"
     *    -> Precompute prefix sums once; for each query, either rerun the
     *       O(n) sliding window per query (O(n * q) total) or explore an
     *       offline approach (e.g., sort queries by k and use a two-
     *       pointer sweep across queries + array simultaneously) to
     *       amortize work across queries.
     *
     * 4. "How would you handle `nums` that is too large to fit in memory
     *    (streamed input)?"
     *    -> The sliding window is actually stream-friendly for computing
     *       a running "count so far ending at each new element", since it
     *       only needs O(1) state (left pointer position, current sum) —
     *       though supporting arbitrary historical queries would need
     *       more thought.
     *
     * 5. "Can you return the actual subarrays instead of just the count?"
     *    -> Modify the sliding window to append (left, right) index pairs
     *       to a result list instead of just incrementing a counter; note
     *       this changes space complexity to O(number of valid subarrays),
     *       which can be O(n^2) in the worst case.
     *
     * 6. "What's the largest possible value the score can take, and does
     *    that ever risk overflow?"
     *    -> With nums[i], n up to 10^5, sum can reach ~10^10 and score
     *       ~10^15 — safely within `long` range (~9.2 * 10^18), but would
     *       overflow `int` (max ~2.1 * 10^9) immediately. Always use long.
     */

    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Using `<=` instead of `<` in the shrink condition or the final
     *    comparison — the problem asks for STRICTLY less than k, so a
     *    score exactly equal to k must be excluded. Getting this
     *    backwards is the single most common bug on this problem.
     *
     * 2. Using `int` instead of `long` for sums, scores, or the running
     *    count — with realistic constraints, both the score and the
     *    total subarray count can overflow a 32-bit int, producing
     *    silently wrong (often negative!) answers rather than a crash.
     *
     * 3. Forgetting that the "add (right - left + 1) to count" step is
     *    only valid AFTER the window has been fully shrunk to satisfy the
     *    invariant for the current `right`. Adding to the count before
     *    finishing the shrink loop produces an overcount.
     *
     * 4. Assuming the two-pointer technique generalizes to arrays with
     *    zero or negative values without re-deriving why it works. The
     *    correctness argument here depends entirely on strict positivity
     *    guaranteeing monotonic growth of both sum and length — candidates
     *    who don't say this out loud often can't answer the "what if
     *    negatives are allowed?" follow-up cleanly.
     */

    /*
     * ========================================================================
     * MAIN METHOD — Demonstrates all four approaches against the worked
     * examples from Section 3, cross-validating that they agree.
     * ========================================================================
     */
    public static void main(String[] args) {
        runExample("Example 1 (normal case)", new int[]{2, 1, 4, 3, 5}, 10L);
        runExample("Example 2 (edge case: single element)", new int[]{5}, 1L);
        runExample("Example 3 (boundary/tie case: score == k excluded)", new int[]{1, 1, 1}, 3L);
    }

    private static void runExample(String label, int[] nums, long k) {
        long resultBruteTriple = bruteForceTripleLoop(nums, k);
        long resultBrutePrefix = bruteForcePrefixSum(nums, k);
        long resultBinarySearch = binarySearchOnPrefixSums(nums, k);
        long resultSlidingWindow = slidingWindowOptimal(nums, k);
        long resultProduction = countSubarraysWithScoreLessThanK(nums, k);

        System.out.println(label);
        System.out.println("  nums = " + Arrays.toString(nums) + ", k = " + k);
        System.out.println("  Brute Force (triple loop):   " + resultBruteTriple);
        System.out.println("  Brute Force (running sum):   " + resultBrutePrefix);
        System.out.println("  Binary Search on prefixes:   " + resultBinarySearch);
        System.out.println("  Sliding Window (optimal):    " + resultSlidingWindow);
        System.out.println("  Production method:           " + resultProduction);

        boolean allAgree = resultBruteTriple == resultBrutePrefix
                && resultBrutePrefix == resultBinarySearch
                && resultBinarySearch == resultSlidingWindow
                && resultSlidingWindow == resultProduction;
        System.out.println("  All approaches agree: " + allAgree);
        System.out.println();
    }
}
