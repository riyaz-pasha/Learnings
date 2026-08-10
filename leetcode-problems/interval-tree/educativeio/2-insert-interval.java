import java.util.*;

/**
 * ============================================================================
 * LEETCODE 57 — INSERT INTERVAL
 * Mock Google-style DSA interview walkthrough, structured section-by-section.
 * ============================================================================
 */
class InsertInterval {

    /*
     * ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     * I'm given a list `intervals` of non-overlapping intervals, already
     * sorted in ascending order by start time. I'm also given a single new
     * interval `newInterval = [start, end]`.
     *
     * My job is to insert `newInterval` into the correct position in the
     * list so that:
     *   1. The resulting list is still sorted by start time.
     *   2. No two intervals in the resulting list overlap — any interval(s)
     *      that overlap with newInterval (or with each other as a result of
     *      the insertion) must be merged into a single combined interval.
     *
     * Inputs:
     *   - int[][] intervals: 0 to 10^4 non-overlapping intervals, each
     *     length 2, sorted ascending by start.
     *   - int[] newInterval: length 2, [start, end].
     *   - 0 <= start_i < end_i <= 10^4 (intervals are non-degenerate, i.e.
     *     start is strictly less than end).
     *
     * Output:
     *   - A new int[][] representing the merged, sorted, non-overlapping
     *     interval list. I do NOT need to mutate `intervals` in place.
     *
     * Key assumption to confirm: "overlap" here includes intervals that are
     * merely touching (e.g. [1,3] and [3,5] touch at 3) — I'll confirm this
     * explicitly in the clarifying questions below, since it changes the
     * merge condition (<= vs <).
     */

    /*
     * ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS
     * ========================================================================
     * 1. Q: Are the input intervals guaranteed to be non-overlapping and
     *       sorted by start, as stated, or should I defensively validate?
     *    A: Assume the guarantee holds; no need to validate/sort. (Given by
     *       constraints.)
     *
     * 2. Q: Do touching intervals count as overlapping? E.g. does [1,3] and
     *       [3,5] merge into [1,5], or stay separate?
     *    A: Yes, treat touching intervals as overlapping and merge them.
     *       This is the standard LeetCode convention for this problem.
     *
     * 3. Q: Can `intervals` be empty?
     *    A: Yes — in that case the answer is simply [newInterval].
     *
     * 4. Q: Can `newInterval` fall entirely before the first interval or
     *       entirely after the last interval (no overlap at all)?
     *    A: Yes, both are valid — newInterval should just be inserted at
     *       the correct sorted position with no merging.
     *
     * 5. Q: Are interval bounds always non-negative integers within
     *       [0, 10^4], with start strictly less than end (no zero-length
     *       or inverted intervals)?
     *    A: Yes, per constraints — I don't need to handle start > end or
     *       start == end.
     *
     * 6. Q: Should the output intervals be returned as a new array/list, or
     *       is in-place mutation of `intervals` acceptable/expected?
     *    A: Return a new array; no in-place requirement (explicitly stated
     *       in the problem).
     *
     * 7. Q: Is there any concurrency concern — e.g., could `intervals` be
     *       mutated by another thread while I process it?
     *    A: No, assume single-threaded, synchronous execution.
     *
     * 8. Q: What's the expected scale, and does it affect the approach?
     *       (intervals.length up to 10^4)
     *    A: 10^4 is small enough that even an O(n log n) sort-based approach
     *       is fine, but since input is already sorted, an O(n) single-pass
     *       approach is both simpler and strictly better — I'll aim for that.
     */

    /*
     * ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (Normal case — partial overlap with multiple intervals):
     *   intervals   = [[1,3],[6,9]]
     *   newInterval = [2,5]
     *   newInterval overlaps [1,3] (since 2 <= 3) but not [6,9] (since 5 < 6).
     *   Merge [1,3] and [2,5] -> [1,5].
     *   Result: [[1,5],[6,9]]
     *
     * Example 2 (Edge case — newInterval overlaps and swallows several,
     * classic LeetCode example):
     *   intervals   = [[1,2],[3,5],[6,7],[8,10],[15,18]]
     *   newInterval = [4,8]
     *   [4,8] overlaps [3,5] (4<=5), [6,7] (fully inside), [8,10] (8<=10).
     *   Merged interval becomes [3,10].
     *   [1,2] is untouched (before), [15,18] is untouched (after).
     *   Result: [[1,2],[3,10],[15,18]]
     *
     * Example 3 (Boundary / touching case + empty list case):
     *   3a. Touching boundary:
     *       intervals   = [[1,3],[6,9]]
     *       newInterval = [3,6]
     *       [3,6] touches both [1,3] (3<=3) and [6,9] (6<=6) — under our
     *       "touching counts as overlap" rule, ALL merge into one interval.
     *       Result: [[1,9]]
     *   3b. Empty intervals list:
     *       intervals   = []
     *       newInterval = [5,7]
     *       Result: [[5,7]]
     *   3c. No overlap at all (insert at head or tail):
     *       intervals   = [[3,5],[12,15]]
     *       newInterval = [6,6]  (degenerate-looking but end>start not
     *                             required to differ much, still valid)
     *       Result: [[3,5],[6,6],[12,15]]  (inserted in the gap, untouched)
     */

    /*
     * ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE SOLUTIONS
     * ========================================================================
     * Paradigm sweep — which categories genuinely apply here:
     *
     *   - Brute force / naive          -> APPLICABLE (append + sort + merge)
     *   - Sorting-based                -> APPLICABLE (same as brute force,
     *                                     since sort is the naive lever)
     *   - Hashing-based                -> NOT APPLICABLE: hashing helps with
     *                                     membership/frequency lookups, not
     *                                     ordered range merging — there's no
     *                                     key/value relationship to exploit.
     *   - Two pointer / linear scan    -> APPLICABLE and OPTIMAL: input is
     *                                     already sorted, so a single
     *                                     left-to-right pass with a merge
     *                                     window works in O(n).
     *   - Sliding window               -> Conceptually this IS a one-pass
     *                                     "expanding window" merge, covered
     *                                     under the optimal approach below;
     *                                     not a separate distinct approach.
     *   - Divide and conquer           -> NOT APPLICABLE: no natural way to
     *                                     split the problem into independent
     *                                     subproblems that recombine cheaper
     *                                     than the linear scan already is.
     *   - Greedy                       -> The optimal linear scan IS a greedy
     *                                     one-pass merge (locally expand the
     *                                     overlap window); not distinct from
     *                                     Approach 2 below.
     *   - Dynamic programming          -> NOT APPLICABLE: no overlapping
     *                                     subproblems or optimal-substructure
     *                                     decision to make; this is pure
     *                                     merging, not optimization search.
     *   - Tree / graph traversal       -> NOT APPLICABLE: no tree/graph
     *                                     structure in the input.
     *   - Heap / priority queue        -> NOT APPLICABLE (well): a heap is
     *                                     useful when merging many *unsorted*
     *                                     interval streams (e.g. LC 253
     *                                     Meeting Rooms II). Here we already
     *                                     have one sorted list plus a single
     *                                     new interval, so a heap adds
     *                                     unneeded O(log n) overhead.
     *   - Binary search                -> USEFUL AS AN OPTIMIZATION to
     *                                     quickly locate the insertion point
     *                                     in O(log n) instead of O(n) scan
     *                                     for the "no overlap" region —
     *                                     worth mentioning as Approach 3.
     *   - Monotonic stack / deque      -> NOT APPLICABLE: monotonic stacks
     *                                     solve "next greater/smaller
     *                                     element" style problems; there's
     *                                     no such relation here.
     *   - Trie / segment tree          -> OVERKILL: a segment/interval tree
     *                                     would help with dynamic range
     *                                     queries over MANY insertions/
     *                                     deletions, but for a single
     *                                     insertion into a static sorted
     *                                     list, it's unnecessary complexity.
     */

    /* ------------------------------------------------------------------
     * Approach 1: Brute Force — Append, Sort, Merge
     * ------------------------------------------------------------------
     * Core idea: Treat this exactly like the general "Merge Intervals"
     * problem (LC 56). Append newInterval to the existing list, sort the
     * combined list by start time, then do a standard linear merge pass.
     *
     * Data structures / paradigm: Sorting + linear merge sweep.
     *
     * Why it's naive: It completely ignores the fact that `intervals` is
     * ALREADY sorted — we pay an unnecessary O(n log n) sort.
     *
     * Time Complexity: O(n log n) — dominated by the sort.
     * Space Complexity: O(n) — for the combined list and result.
     *
     * Pros:
     *   - Very simple to reason about and implement quickly.
     *   - Reuses the well-known "merge intervals" pattern verbatim.
     * Cons:
     *   - Wastes the pre-sorted-ness of the input; not optimal.
     *   - Extra sort step is unnecessary work at scale.
     *
     * When to use: Only if I hadn't noticed/confirmed the sorted-input
     * guarantee, or as a fallback baseline to state first in the interview
     * to "lock in" a correct answer before optimizing.
     * ------------------------------------------------------------------ */
    static int[][] approach1_BruteForceSort(int[][] intervals, int[] newInterval) {
        List<int[]> combined = new ArrayList<>(Arrays.asList(intervals));
        combined.add(newInterval);
        // Sort by start time — this is the step we don't actually need,
        // since `intervals` was already sorted; done here for generality.
        combined.sort((a, b) -> Integer.compare(a[0], b[0]));

        List<int[]> merged = new ArrayList<>();
        for (int[] current : combined) {
            if (merged.isEmpty() || merged.get(merged.size() - 1)[1] < current[0]) {
                // No overlap with the last merged interval; start a new one.
                merged.add(current.clone());
            } else {
                // Overlaps (or touches) the last merged interval; extend it.
                int[] last = merged.get(merged.size() - 1);
                last[1] = Math.max(last[1], current[1]);
            }
        }
        return merged.toArray(new int[0][]);
    }

    /* ------------------------------------------------------------------
     * Approach 2: Optimal Linear Scan (Three-Phase One-Pass Merge)
     * ------------------------------------------------------------------
     * Core idea: Since `intervals` is already sorted, walk through it once
     * in three logical phases:
     *   Phase A: Copy all intervals that end strictly before newInterval
     *            starts (they come entirely "before" — no overlap possible).
     *   Phase B: While the current interval's start is <= newInterval's end,
     *            it overlaps (or touches) newInterval — absorb it by
     *            expanding newInterval's bounds (min of starts, max of
     *            ends). Continue until no more intervals overlap.
     *   Phase C: Copy all remaining intervals as-is (they start strictly
     *            after the now-expanded newInterval ends).
     *
     * Data structures / paradigm: Two-pointer / single-pass linear scan
     * (greedy expanding-window merge). No sorting needed since input order
     * is already guaranteed.
     *
     * Time Complexity: O(n) — every interval is visited exactly once.
     * Space Complexity: O(n) — for the output list (required regardless,
     * since we must return a new array of up to n+1 intervals).
     *
     * Pros:
     *   - Optimal time complexity; no wasted sort.
     *   - Single pass, easy to trace and reason about correctness.
     *   - No auxiliary data structures beyond the output list.
     * Cons:
     *   - Slightly more bookkeeping (three phases) than the brute force
     *     merge, so a bit more prone to off-by-one bugs if rushed.
     *
     * When to use: This is the production-quality answer — always prefer
     * this when the sorted-input guarantee holds (which it does here).
     * ------------------------------------------------------------------ */
    static int[][] approach2_OptimalLinearScan(int[][] intervals, int[] newInterval) {
        List<int[]> result = new ArrayList<>();
        int index = 0;
        int n = intervals.length;
        int mergedStart = newInterval[0];
        int mergedEnd = newInterval[1];

        // Phase A: intervals entirely before newInterval (no overlap).
        while (index < n && intervals[index][1] < mergedStart) {
            result.add(intervals[index]);
            index++;
        }

        // Phase B: intervals overlapping (or touching) newInterval —
        // absorb them by expanding [mergedStart, mergedEnd].
        while (index < n && intervals[index][0] <= mergedEnd) {
            mergedStart = Math.min(mergedStart, intervals[index][0]);
            mergedEnd = Math.max(mergedEnd, intervals[index][1]);
            index++;
        }
        result.add(new int[]{mergedStart, mergedEnd});

        // Phase C: intervals entirely after the merged interval.
        while (index < n) {
            result.add(intervals[index]);
            index++;
        }

        return result.toArray(new int[0][]);
    }

    /* ------------------------------------------------------------------
     * Approach 3: Binary Search to Locate Boundaries, Then Merge
     * ------------------------------------------------------------------
     * Core idea: Use binary search to find (a) the first index whose
     * interval's end is >= newInterval.start (start of the possible
     * overlap region) and (b) the last index whose interval's start is
     * <= newInterval.end (end of the possible overlap region). Copy the
     * "before" region, merge the located overlap region with newInterval,
     * copy the "after" region.
     *
     * Data structures / paradigm: Binary search over a sorted array.
     *
     * Time Complexity: O(log n) to locate boundaries + O(n) to copy the
     * before/after regions and build the result array = O(n) overall.
     * The binary search doesn't change the asymptotic complexity here
     * because we still must copy up to n elements into the output.
     * Space Complexity: O(n) for the output.
     *
     * Pros:
     *   - Demonstrates binary search fluency, which interviewers like.
     *   - Marginally fewer comparisons than the linear scan in the
     *     "no overlap" regions for very large n.
     * Cons:
     *   - Same overall O(n) complexity as Approach 2 (copying dominates),
     *     so the binary search doesn't actually improve asymptotic
     *     performance — added complexity for no real Big-O win.
     *   - More edge-case-prone to implement correctly (boundary indices
     *     for binary search are a common source of off-by-one bugs).
     *
     * When to use: Mention it as an alternative to show breadth, but I
     * would NOT choose this in practice — Approach 2 achieves the same
     * O(n) bound with much simpler, less bug-prone code. This is
     * genuinely useful only if intervals were static and queried/inserted
     * against repeatedly in a way that could amortize the search cost
     * (which isn't the case for a single insertion here).
     * ------------------------------------------------------------------ */
    static int[][] approach3_BinarySearchAssisted(int[][] intervals, int[] newInterval) {
        int n = intervals.length;
        if (n == 0) {
            return new int[][]{newInterval.clone()};
        }

        // Find first index where intervals[index][1] >= newInterval[0]
        // (first interval that could possibly overlap or come after).
        int lo = 0, hi = n; // hi is an exclusive upper bound
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (intervals[mid][1] < newInterval[0]) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        int overlapStartIndex = lo; // first index NOT entirely before newInterval

        // Find first index where intervals[index][0] > newInterval[1]
        // (first interval that definitely starts after newInterval ends).
        lo = 0;
        hi = n;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (intervals[mid][0] <= newInterval[1]) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        int afterStartIndex = lo; // first index entirely after newInterval

        List<int[]> result = new ArrayList<>();
        // Copy everything before the overlap region untouched.
        for (int i = 0; i < overlapStartIndex; i++) {
            result.add(intervals[i]);
        }

        // Merge newInterval with everything in [overlapStartIndex, afterStartIndex).
        int mergedStart = newInterval[0];
        int mergedEnd = newInterval[1];
        for (int i = overlapStartIndex; i < afterStartIndex; i++) {
            mergedStart = Math.min(mergedStart, intervals[i][0]);
            mergedEnd = Math.max(mergedEnd, intervals[i][1]);
        }
        result.add(new int[]{mergedStart, mergedEnd});

        // Copy everything after the overlap region untouched.
        for (int i = afterStartIndex; i < n; i++) {
            result.add(intervals[i]);
        }

        return result.toArray(new int[0][]);
    }

    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     * Approach                         | Time       | Space | Best For                              | Limitations
     * ----------------------------------|------------|-------|---------------------------------------|--------------------------------------------
     * 1. Brute Force (Append+Sort+Merge)| O(n log n) | O(n)  | Quick baseline; when sorted-ness is   | Wastes the sorted-input guarantee; slower
     *                                    |            |       | not guaranteed/confirmed              | than necessary at scale
     * 2. Optimal Linear Scan            | O(n)       | O(n)  | Production use; the correct answer    | None significant — this IS the optimal
     *    (Three-Phase One-Pass Merge)    |            |       | given sorted, non-overlapping input   | approach for this problem's constraints
     * 3. Binary Search Assisted         | O(n)       | O(n)  | Demonstrating binary search fluency;  | No real Big-O improvement over Approach 2;
     *                                    | (search    |       | scenarios with repeated static queries| more edge cases to get right for no gain
     *                                    | O(log n))  |       |                                        | in this single-insertion problem
     */

    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     * I would present Approach 2 (Optimal Linear Scan) as my final answer.
     *
     * Rationale:
     *   - Clarity: Three clearly named phases (before / overlapping / after)
     *     map directly onto the problem's structure and are easy to narrate
     *     out loud while coding — interviewers can follow the logic without
     *     needing to see the whole solution first.
     *   - Coding speed: No sorting, no binary search edge cases — just a
     *     single index and two accumulator variables (mergedStart,
     *     mergedEnd). This is fast to write correctly under time pressure.
     *   - Interviewer expectations: For "Insert Interval," interviewers
     *     specifically expect candidates to notice and exploit the
     *     "already sorted" constraint rather than defaulting to a sort.
     *     Reaching for Approach 1 first (and explicitly stating it's a
     *     stepping stone) shows structured thinking; landing on Approach 2
     *     shows I found the optimal solution.
     *   - Optimality: O(n) time is the best possible here, since every
     *     interval must be examined at least once to confirm it doesn't
     *     overlap; O(n) space is required for the output itself.
     *
     * My "safe-then-optimal" narration in the room:
     *   1. State Approach 1 (brute force sort+merge) out loud as a
     *      guaranteed-correct baseline.
     *   2. Note that the input is already sorted, so sorting again is
     *      wasted work — pivot to Approach 2.
     *   3. Mention Approach 3 (binary search) briefly as an alternative
     *      that doesn't actually improve the asymptotic bound, to show
     *      awareness without over-engineering.
     *   4. Implement Approach 2 as the final, polished solution.
     */

    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (PRODUCTION QUALITY)
     * ========================================================================
     */

    /**
     * Inserts {@code newInterval} into the sorted, non-overlapping list
     * {@code intervals}, merging any overlapping intervals as needed.
     *
     * <p>Algorithm: single left-to-right pass over {@code intervals} in
     * three phases — copy intervals strictly before the overlap region,
     * absorb (merge) all intervals overlapping {@code newInterval} by
     * expanding its bounds, then copy intervals strictly after. Because
     * the input is guaranteed sorted by start time and non-overlapping,
     * a single pass suffices; no sorting or auxiliary search is required.
     *
     * @param intervals   non-overlapping intervals sorted ascending by
     *                    start time; each element is {@code [start, end]}
     *                    with {@code start < end}. May be empty but not
     *                    null.
     * @param newInterval the interval {@code [start, end]} to insert;
     *                    must not be null.
     * @return a new 2D array containing the merged, sorted,
     *         non-overlapping result. The input array is not mutated.
     * @throws IllegalArgumentException if either argument is null, or if
     *         any interval has {@code start >= end} (malformed interval).
     */
    static int[][] insertInterval(int[][] intervals, int[] newInterval) {
        // --- Defensive validation -------------------------------------
        // In a real production system I wouldn't trust constraints blindly;
        // I validate the shape of the input even though the problem
        // guarantees well-formed data, since interview-quality code should
        // fail loudly on contract violations rather than silently misbehave.
        if (intervals == null || newInterval == null) {
            throw new IllegalArgumentException("intervals and newInterval must not be null");
        }
        if (newInterval.length != 2 || newInterval[0] >= newInterval[1]) {
            throw new IllegalArgumentException("newInterval must be [start, end) with start < end");
        }

        int intervalCount = intervals.length;
        // Pre-size the result list generously: at most intervalCount + 1
        // intervals can exist in the output (every original interval stays
        // separate in the worst case, plus the new one).
        List<int[]> result = new ArrayList<>(intervalCount + 1);

        int index = 0;
        // Track the running merged bounds, seeded with newInterval itself.
        // These expand as we absorb overlapping intervals in Phase B.
        int mergedStart = newInterval[0];
        int mergedEnd = newInterval[1];

        // --- Phase A: intervals entirely before the overlap region -----
        // An interval [s, e] is entirely before newInterval when its end
        // is strictly less than newInterval's start — i.e., no touching,
        // no overlap. These pass through completely untouched.
        while (index < intervalCount && intervals[index][1] < mergedStart) {
            result.add(intervals[index]);
            index++;
        }

        // --- Phase B: intervals overlapping or touching newInterval ----
        // An interval [s, e] overlaps (or touches) the current merged
        // window when its start is <= mergedEnd. We use <= (not <) because
        // touching intervals (e.g. [1,3] and [3,6]) are treated as
        // overlapping per our confirmed clarifying-question assumption.
        // Each absorbed interval can only grow the window, never shrink
        // it, which is why a simple min/max expansion is correct here.
        while (index < intervalCount && intervals[index][0] <= mergedEnd) {
            mergedStart = Math.min(mergedStart, intervals[index][0]);
            mergedEnd = Math.max(mergedEnd, intervals[index][1]);
            index++;
        }
        // Commit the fully-expanded merged interval exactly once.
        result.add(new int[]{mergedStart, mergedEnd});

        // --- Phase C: intervals entirely after the overlap region ------
        // Everything remaining starts strictly after mergedEnd (otherwise
        // Phase B would have absorbed it), so it passes through untouched.
        while (index < intervalCount) {
            result.add(intervals[index]);
            index++;
        }

        return result.toArray(new int[0][]);
    }

    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     * Tracing insertInterval() on the classic example:
     *   intervals   = [[1,2],[3,5],[6,7],[8,10],[15,18]]
     *   newInterval = [4,8]
     *
     * Initial state:
     *   index = 0, mergedStart = 4, mergedEnd = 8, result = []
     *
     * --- Phase A (copy intervals ending before mergedStart=4) ---
     *   index=0: intervals[0] = [1,2], end=2 < 4 -> copy.
     *            result = [[1,2]], index = 1
     *   index=1: intervals[1] = [3,5], end=5 < 4? NO (5 is not < 4) -> stop Phase A.
     *
     * --- Phase B (absorb overlapping/touching intervals) ---
     *   index=1: intervals[1] = [3,5], start=3 <= mergedEnd=8 -> absorb.
     *            mergedStart = min(4,3) = 3
     *            mergedEnd   = max(8,5) = 8
     *            index = 2
     *   index=2: intervals[2] = [6,7], start=6 <= mergedEnd=8 -> absorb.
     *            mergedStart = min(3,6) = 3
     *            mergedEnd   = max(8,7) = 8
     *            index = 3
     *   index=3: intervals[3] = [8,10], start=8 <= mergedEnd=8 -> absorb (touching).
     *            mergedStart = min(3,8) = 3
     *            mergedEnd   = max(8,10) = 10
     *            index = 4
     *   index=4: intervals[4] = [15,18], start=15 <= mergedEnd=10? NO -> stop Phase B.
     *   Commit merged interval: result = [[1,2],[3,10]]
     *
     * --- Phase C (copy remaining intervals untouched) ---
     *   index=4: intervals[4] = [15,18] -> copy.
     *            result = [[1,2],[3,10],[15,18]], index = 5
     *   index=5: loop ends (index == intervalCount).
     *
     * Final result: [[1,2],[3,10],[15,18]]  ✓ matches expected output.
     */

    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     * - Approach 1 (sort+merge) is a correct but suboptimal O(n log n)
     *   baseline that ignores the sorted-input guarantee.
     * - Approach 2 (three-phase linear scan) is optimal at O(n) time /
     *   O(n) space, and is the approach I'd actually ship and present as
     *   my final answer.
     * - Approach 3 (binary search assisted) demonstrates an alternative
     *   technique but provides no real asymptotic benefit here since
     *   output construction is already O(n); it's more valuable to
     *   mention verbally than to fully implement under time pressure.
     *
     * Known limitations / assumptions of the final solution:
     *   - Assumes `intervals` is genuinely sorted ascending by start and
     *     non-overlapping, as guaranteed by the problem constraints; the
     *     algorithm does NOT re-validate or re-sort, so malformed input
     *     (unsorted or overlapping) would silently produce an incorrect
     *     result rather than throwing an error.
     *   - Treats touching intervals (end == next start) as overlapping and
     *     merges them; a variant of this problem might require strict (<)
     *     overlap, which would just mean flipping <= to < in Phase B.
     *   - Assumes 32-bit int bounds are sufficient (constraint caps values
     *     at 10^4), so no overflow concerns with int arithmetic here.
     */

    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     * 1. "What if `intervals` is NOT guaranteed to be sorted?" — Then we'd
     *    need to fall back to Approach 1 (sort first), making it O(n log n)
     *    unavoidably, since sorting is required before any linear merge.
     *
     * 2. "What if there are millions of insertions to perform one after
     *    another against the same growing interval list?" — A single O(n)
     *    pass per insertion becomes O(n * m) for m insertions; at that
     *    point a balanced BST / interval tree (e.g. a TreeMap keyed by
     *    start time) supporting O(log n) insertion and neighbor lookups
     *    would be worth the added complexity.
     *
     * 3. "How would you adapt this to support interval DELETION as well as
     *    insertion?" — Similar three-phase logic: locate overlapping
     *    interval(s), and instead of expanding bounds, potentially split
     *    or shrink the surrounding intervals to remove the deleted range.
     *
     * 4. "What if intervals could have equal start and end (zero-length
     *    intervals / point events)?" — Need to explicitly clarify whether
     *    a point event at exactly newInterval's boundary counts as
     *    touching/overlapping; likely would keep the <= convention.
     *
     * 5. "Can you solve this if intervals aren't just numeric ranges but,
     *    say, time ranges with time zones, or multi-dimensional (2D)
     *    rectangles?" — The 1D interval logic generalizes to time via
     *    consistent normalization (e.g. convert all to UTC epoch first);
     *    2D rectangle merging is fundamentally harder and typically needs
     *    sweep-line + segment tree techniques, not a simple linear scan.
     *
     * 6. "What's the maximum number of intervals in the output relative to
     *    the input?" — At most intervals.length + 1 (when newInterval
     *    doesn't overlap anything and is inserted standalone); at minimum
     *    it could collapse everything down to a single interval.
     */

    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     * 1. Using strict `<` instead of `<=` when checking for overlap/touch
     *    in Phase B (or the equivalent Phase A boundary). This causes
     *    touching intervals like [1,3] and [3,6] to NOT merge when they
     *    should, producing [[1,3],[3,6]] instead of the correct [[1,6]].
     *    This is the single most common bug on this problem.
     *
     * 2. Forgetting to actually commit the merged interval to the result
     *    list after Phase B — candidates sometimes only append it inside
     *    the loop conditionally, missing the case where newInterval never
     *    overlaps anything (loop body never executes) and the interval
     *    never gets added at all.
     *
     * 3. Mutating the input array's inner int[] references directly (e.g.
     *    `intervals[index][1] = newEnd`) instead of building fresh int[]
     *    objects for the result — this can silently corrupt the caller's
     *    original data even though the problem says mutation isn't
     *    required (it also isn't forbidden, but it's a code-smell most
     *    interviewers will flag).
     *
     * 4. Off-by-one / empty-input mishandling — not special-casing an
     *    empty `intervals` array (though the three-phase loop actually
     *    handles it gracefully by construction, many hand-rolled binary
     *    search variants like Approach 3 need an explicit early return).
     *    Similarly, forgetting that newInterval could need to be inserted
     *    at the very front or very back (no overlap at all) trips up
     *    candidates who only test "middle insertion" cases.
     */

    /*
     * ========================================================================
     * TEST HARNESS — cross-validates all three approaches against each
     * other and against expected outputs for every example above.
     * ========================================================================
     */
    public static void main(String[] args) {
        record TestCase(String name, int[][] intervals, int[] newInterval, int[][] expected) {}

        List<TestCase> testCases = List.of(
            new TestCase(
                "Example 1: partial overlap",
                new int[][]{{1, 3}, {6, 9}},
                new int[]{2, 5},
                new int[][]{{1, 5}, {6, 9}}
            ),
            new TestCase(
                "Example 2: swallow multiple intervals",
                new int[][]{{1, 2}, {3, 5}, {6, 7}, {8, 10}, {15, 18}},
                new int[]{4, 8},
                new int[][]{{1, 2}, {3, 10}, {15, 18}}
            ),
            new TestCase(
                "Example 3a: touching boundaries merge everything",
                new int[][]{{1, 3}, {6, 9}},
                new int[]{3, 6},
                new int[][]{{1, 9}}
            ),
            new TestCase(
                "Example 3b: empty intervals list",
                new int[][]{},
                new int[]{5, 7},
                new int[][]{{5, 7}}
            ),
            new TestCase(
                "Example 3c: insert into a gap, no overlap",
                new int[][]{{3, 5}, {12, 15}},
                new int[]{6, 6 + 0}, // degenerate-looking but end(6) > start not required to differ
                new int[][]{{3, 5}, {6, 6}, {12, 15}}
            ),
            new TestCase(
                "Insert before everything",
                new int[][]{{5, 7}, {8, 9}},
                new int[]{1, 2},
                new int[][]{{1, 2}, {5, 7}, {8, 9}}
            ),
            new TestCase(
                "Insert after everything",
                new int[][]{{1, 2}, {3, 4}},
                new int[]{6, 8},
                new int[][]{{1, 2}, {3, 4}, {6, 8}}
            ),
            new TestCase(
                "newInterval swallows entire list",
                new int[][]{{2, 3}, {4, 5}, {6, 7}},
                new int[]{1, 10},
                new int[][]{{1, 10}}
            )
        );

        int passCount = 0;
        for (TestCase testCase : testCases) {
            int[][] resultBruteForce = approach1_BruteForceSort(testCase.intervals(), testCase.newInterval());
            int[][] resultOptimal = approach2_OptimalLinearScan(testCase.intervals(), testCase.newInterval());
            int[][] resultBinarySearch = approach3_BinarySearchAssisted(testCase.intervals(), testCase.newInterval());
            int[][] resultProduction = insertInterval(testCase.intervals(), testCase.newInterval());

            boolean allMatch =
                Arrays.deepEquals(resultBruteForce, testCase.expected()) &&
                Arrays.deepEquals(resultOptimal, testCase.expected()) &&
                Arrays.deepEquals(resultBinarySearch, testCase.expected()) &&
                Arrays.deepEquals(resultProduction, testCase.expected());

            System.out.println((allMatch ? "PASS" : "FAIL") + " — " + testCase.name());
            if (!allMatch) {
                System.out.println("  expected:      " + Arrays.deepToString(testCase.expected()));
                System.out.println("  bruteForce:    " + Arrays.deepToString(resultBruteForce));
                System.out.println("  optimal:       " + Arrays.deepToString(resultOptimal));
                System.out.println("  binarySearch:  " + Arrays.deepToString(resultBinarySearch));
                System.out.println("  production:    " + Arrays.deepToString(resultProduction));
            } else {
                passCount++;
            }
        }
        System.out.println();
        System.out.println(passCount + "/" + testCases.size() + " test cases passed across all four implementations.");
    }
}
