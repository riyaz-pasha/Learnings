import java.util.*;

/*
 * ============================================================================
 * GOOGLE-STYLE MOCK INTERVIEW: "FIND RIGHT INTERVAL" (LeetCode 436)
 * ============================================================================
 *
 * This single file is structured as a complete interview walkthrough.
 * Each major phase of the interview is a labeled block comment. All
 * approaches are implemented as self-contained, runnable static methods
 * inside FindRightInterval, and validated against a shared test harness
 * in main().
 *
 * Run with: java FindRightInterval.java   (single-file source-launch mode)
 * ============================================================================
 */

/*
 * ============================================================================
 * SECTION 1: RESTATE THE PROBLEM
 * ============================================================================
 *
 * In plain language:
 *   - We're given n intervals, intervals[i] = [start_i, end_i].
 *   - All start_i values are guaranteed to be distinct (no two intervals
 *     start at the same point).
 *   - For each interval i, we want to find another interval j (j == i is
 *     allowed) whose start value is the SMALLEST start value that is still
 *     >= end_i. In other words: among all intervals that begin at or after
 *     interval i ends, pick the one that begins earliest. That's the
 *     "tightest fitting" successor interval.
 *   - If no such interval exists (i.e., no interval starts at or after
 *     end_i), the answer for i is -1.
 *   - Output: an int[] `answer` of length n, where answer[i] is the ORIGINAL
 *     INDEX (not the value) of the right interval for intervals[i].
 *
 * Key constraints/inputs/outputs:
 *   - 1 <= intervals.length <= 1000
 *   - intervals[i].length == 2
 *   - -1e6 <= start_i <= end_i <= 1e6   (end_i is always >= start_i)
 *   - All start_i are unique -> this is important: it means we can safely
 *     use starts as keys in a map or as a sort key without worrying about
 *     ties among starts.
 *   - Output is indices into the ORIGINAL array, not the sorted array, so
 *     we must track index mapping carefully once we sort.
 *
 * Assumptions I'll state explicitly (and confirm in clarifying questions):
 *   - "Right interval" is defined purely by start >= end_i; overlapping,
 *     containment, or order in the original array does not matter beyond
 *     that inequality.
 *   - i == j is valid: an interval CAN be its own right interval if
 *     start_i >= end_i (i.e., a zero/negative-length interval, though given
 *     end_i >= start_i, this only happens when start_i == end_i).
 */

/*
 * ============================================================================
 * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
 * ============================================================================
 *
 * 1. Q: Can `intervals` be empty?
 *    A: Constraints say length >= 1, so no need to handle the empty-array
 *       case, but I'll defensively guard against it anyway.
 *
 * 2. Q: Are start_i values guaranteed unique, or could duplicates appear?
 *    A: Problem statement guarantees uniqueness of start_i. This is a load-
 *       bearing assumption -- it lets me use a TreeMap<Integer,Integer> or a
 *       sorted array of starts without worrying about which duplicate to
 *       pick.
 *
 * 3. Q: Can end_i values repeat, or be equal to start_i?
 *    A: Yes, end_i can repeat across intervals and end_i can equal start_i
 *       (zero-length interval). No special handling needed beyond normal
 *       comparison logic.
 *
 * 4. Q: Should the answer be indices into the original input array, or
 *       into some sorted/transformed version?
 *    A: Original input array indices -- this is explicit in the problem
 *       (“right interval indexes”), so I must preserve a mapping from
 *       sorted start values back to original index.
 *
 * 5. Q: What's the expected input scale? Does it justify an O(n^2) or do we
 *       need O(n log n)?
 *    A: n <= 1000, so O(n^2) (1,000,000 ops) would technically pass, but
 *       I'll still aim for O(n log n) since it's the "expected" solution
 *       and demonstrates stronger technique -- and it's not meaningfully
 *       harder to write.
 *
 * 6. Q: Are interval bounds always integers, and could they overflow int?
 *    A: Bounds are within [-1e6, 1e6], comfortably within int range, no
 *       overflow concerns even with basic arithmetic.
 *
 * 7. Q: Is thread-safety / concurrent access a concern (e.g., is this run
 *       in a multi-threaded service)?
 *    A: No -- treat this as a single-threaded, in-memory computation on an
 *       immutable input snapshot.
 *
 * 8. Q: If multiple intervals could tie for "smallest start >= end_i" (not
 *       possible here since starts are unique, but worth confirming),
 *       which one wins?
 *    A: Not applicable given the uniqueness guarantee, but I'd note that if
 *       it were possible, we'd need a tie-breaking rule (e.g., lowest
 *       original index).
 */

/*
 * ============================================================================
 * SECTION 3: EXAMPLES & EDGE CASES
 * ============================================================================
 *
 * Example 1 (normal case):
 *   intervals = [[3,4],[2,3],[1,2]]
 *   Sorted by start: [1,2](idx2), [2,3](idx1), [3,4](idx0)
 *   - idx0 [3,4]: need start >= 4 -> none exists -> -1
 *   - idx1 [2,3]: need start >= 3 -> smallest such start is 3 (idx0) -> 0
 *   - idx2 [1,2]: need start >= 2 -> smallest such start is 2 (idx1) -> 1
 *   answer = [-1, 0, 1]
 *
 * Example 2 (edge case: single interval, self-reference):
 *   intervals = [[1,2]]
 *   - idx0 [1,2]: need start >= 2. Only candidate start is 1, which is < 2.
 *     -> -1
 *   answer = [-1]
 *
 * Example 3 (boundary / tie-breaking-adjacent case: zero-length interval
 * and an exact-match boundary):
 *   intervals = [[5,5],[7,7],[5,9]]
 *   Sorted by start: [5,5](idx0), [5,9]... wait -- starts must be unique,
 *   so let's correct this to respect constraints:
 *   intervals = [[5,5],[7,7],[6,9]]
 *   Sorted by start: [5,5](idx0), [6,9](idx2), [7,7](idx1)
 *   - idx0 [5,5]: need start >= 5 -> smallest start >= 5 is 5 itself
 *     (idx0) -> answer is 0 (i == j is valid here since start_0 == end_0)
 *   - idx1 [7,7]: need start >= 7 -> smallest start >= 7 is 7 (idx1) -> 1
 *     (again i == j)
 *   - idx2 [6,9]: need start >= 9 -> no start reaches 9 -> -1
 *   answer = [0, 1, -1]
 *   This example demonstrates the exact-boundary match (start == end) and
 *   confirms that self-reference (i == j) is legitimate output.
 */

class FindRightInterval {

    /*
     * ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE SOLUTIONS (with paradigm sweep)
     * ========================================================================
     *
     * Paradigm sweep -- which categories apply, which don't, and why:
     *
     *   - Brute force / naive............... APPLICABLE (baseline correctness anchor)
     *   - Sorting-based...................... APPLICABLE (core to the optimal solution)
     *   - Hashing-based....................... APPLICABLE (TreeMap variant; a plain
     *                                          HashMap alone can't answer "smallest
     *                                          start >= x" range queries, but a sorted
     *                                          map / balanced BST can)
     *   - Binary search....................... APPLICABLE (searching sorted starts for
     *                                          the lower bound of end_i)
     *   - Two pointer / sliding window........ NOT APPLICABLE: there's no contiguous
     *                                          window property to exploit here; each
     *                                          query (end_i) needs an independent
     *                                          "smallest start >= x" lookup, not a
     *                                          monotonically advancing window.
     *   - Divide and conquer.................. NOT MEANINGFULLY DIFFERENT: sorting +
     *                                          binary search already IS a D&C-flavored
     *                                          approach (binary search is D&C); no
     *                                          separate D&C algorithm adds value here.
     *   - Greedy.............................. NOT APPLICABLE: there's no sequential
     *                                          decision-making with local choices
     *                                          building a global solution -- this is a
     *                                          direct lookup problem per interval.
     *   - Dynamic programming.................. NOT APPLICABLE: no overlapping
     *                                          subproblems or optimal substructure --
     *                                          each interval's answer is independent.
     *   - Tree / graph traversal............... NOT APPLICABLE: no explicit graph
     *                                          structure to traverse (though a TreeMap
     *                                          is backed by a red-black tree, we use it
     *                                          for its ordered-map API, not traversal).
     *   - Heap / priority queue................ NOT NEEDED: heaps excel at "repeatedly
     *                                          extract min/max," but here we need
     *                                          arbitrary "smallest key >= x" queries per
     *                                          interval, which a sorted structure with
     *                                          binary search / ceiling lookup answers
     *                                          more directly and just as fast.
     *   - Monotonic stack / deque.............. NOT APPLICABLE: no "next greater
     *                                          element in array order" pattern here;
     *                                          the relevant order is by start value,
     *                                          which we control via sorting, not by
     *                                          scanning in original array order.
     *   - Trie / segment tree / advanced....... OVERKILL: a segment tree could answer
     *                                          "min start >= x" over a coordinate-
     *                                          compressed range, but that's strictly
     *                                          more machinery for the same O(n log n)
     *                                          result that sorting + binary search (or
     *                                          a TreeMap) already achieves.
     */

    /*
     * ------------------------------------------------------------------------
     * Approach 1: Brute Force (Nested Linear Scan)
     * ------------------------------------------------------------------------
     * Core idea: For every interval i, scan every interval j and track the
     * one with the minimum start_j such that start_j >= end_i.
     *
     * Data structure / paradigm: none beyond raw arrays -- pure brute force.
     *
     * Time Complexity: O(n^2) -- for each of n intervals, we scan all n
     *   intervals looking for the best candidate.
     * Space Complexity: O(1) extra (excluding output array).
     *
     * Pros:
     *   - Trivial to write correctly under interview pressure.
     *   - Zero risk of off-by-one errors in index mapping (no sorting, no
     *     re-mapping needed).
     *   - Great as a correctness oracle to test faster approaches against.
     * Cons:
     *   - O(n^2) -- won't scale past a few thousand intervals.
     *   - Wasteful: repeatedly re-scans the same data with no reuse of work
     *     across iterations.
     *
     * When to use: Only as a warm-up / correctness baseline, or if n is
     * tiny and guaranteed to stay tiny. Not what I'd ship or leave as final
     * in an interview, but a great opening move to establish correctness
     * before optimizing.
     */
    public static int[] findRightIntervalBruteForce(int[][] intervals) {
        int intervalCount = intervals.length;
        int[] answer = new int[intervalCount];

        for (int i = 0; i < intervalCount; i++) {
            int targetEnd = intervals[i][1];
            int bestStart = Integer.MAX_VALUE;
            int bestIndex = -1;

            for (int j = 0; j < intervalCount; j++) {
                int candidateStart = intervals[j][0];
                if (candidateStart >= targetEnd && candidateStart < bestStart) {
                    bestStart = candidateStart;
                    bestIndex = j;
                }
            }
            answer[i] = bestIndex;
        }
        return answer;
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 2: Sorting Starts + Binary Search (Lower Bound)
     * ------------------------------------------------------------------------
     * Core idea: Create an array of (start value, original index) pairs and
     * sort it by start value. Then, for each interval i, binary-search the
     * sorted starts array for the leftmost start >= end_i (a classic
     * "lower bound" / "ceiling" binary search). The paired original index
     * tells us which interval that start belongs to.
     *
     * Data structure / paradigm: sorting + binary search (lower-bound
     * search over a sorted array).
     *
     * Time Complexity: O(n log n) -- O(n log n) to sort the starts, then
     *   O(log n) binary search per interval across n intervals -> O(n log n)
     *   total.
     * Space Complexity: O(n) for the sorted (start, originalIndex) array.
     *
     * Pros:
     *   - Optimal asymptotic complexity.
     *   - No dependency on library ordered-map internals -- binary search
     *     over a primitive array is fast and has predictable, low constant
     *     factors.
     *   - Easy to reason about and trace by hand (good for interview
     *     whiteboarding).
     * Cons:
     *   - Slightly more bookkeeping than the TreeMap version (must manually
     *     track the "original index" alongside each start when sorting).
     *   - Manual binary search is a common source of off-by-one bugs if
     *     rushed.
     *
     * When to use: This is the version I'd actually write and finalize in
     * a real Google interview -- it's optimal, uses only core language
     * features (arrays + Arrays.sort + hand-rolled binary search), and
     * shows I understand the lower-bound binary search pattern rather than
     * leaning entirely on a library's ordered map.
     */
    public static int[] findRightIntervalBinarySearch(int[][] intervals) {
        int intervalCount = intervals.length;
        int[] answer = new int[intervalCount];

        // startWithIndex[k] = {start value, original index}, sorted by start value.
        int[][] startWithIndex = new int[intervalCount][2];
        for (int i = 0; i < intervalCount; i++) {
            startWithIndex[i][0] = intervals[i][0];
            startWithIndex[i][1] = i;
        }
        Arrays.sort(startWithIndex, (a, b) -> Integer.compare(a[0], b[0]));

        for (int i = 0; i < intervalCount; i++) {
            int targetEnd = intervals[i][1];
            int resultOriginalIndex = lowerBoundOriginalIndex(startWithIndex, targetEnd);
            answer[i] = resultOriginalIndex;
        }
        return answer;
    }

    // Binary search for the leftmost entry in sortedStarts whose start value
    // is >= target. Returns the ORIGINAL index stored alongside that start,
    // or -1 if no such start exists.
    private static int lowerBoundOriginalIndex(int[][] sortedStarts, int target) {
        int lowInclusive = 0;
        int highExclusive = sortedStarts.length;

        while (lowInclusive < highExclusive) {
            int mid = lowInclusive + (highExclusive - lowInclusive) / 2;
            if (sortedStarts[mid][0] >= target) {
                highExclusive = mid;       // mid is a valid candidate; look further left
            } else {
                lowInclusive = mid + 1;    // mid's start too small; search right half
            }
        }

        if (lowInclusive == sortedStarts.length) {
            return -1; // no start value >= target exists
        }
        return sortedStarts[lowInclusive][1];
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 3: TreeMap Ceiling Lookup (Ordered Map)
     * ------------------------------------------------------------------------
     * Core idea: Insert every (start_i -> original index i) pair into a
     * TreeMap<Integer, Integer>, which keeps keys sorted internally
     * (backed by a red-black tree). For each interval, use
     * TreeMap.ceilingEntry(end_i), which directly returns the entry with
     * the smallest key >= end_i -- exactly the semantics we need, with no
     * manual binary search code required.
     *
     * Data structure / paradigm: hashing/ordered-map based (TreeMap), which
     * is a self-balancing BST under the hood.
     *
     * Time Complexity: O(n log n) -- n insertions at O(log n) each, plus n
     *   ceiling lookups at O(log n) each.
     * Space Complexity: O(n) for the TreeMap.
     *
     * Pros:
     *   - Extremely concise and readable -- ceilingEntry() expresses intent
     *     directly ("give me the smallest key >= x"), minimizing bug
     *     surface area compared to hand-rolled binary search.
     *   - Same asymptotic complexity as Approach 2.
     * Cons:
     *   - Higher constant factors than a flat sorted array + binary search
     *     (tree node pointer-chasing vs. contiguous array access / cache
     *     locality).
     *   - Relies on library internals -- less demonstrative of raw
     *     algorithmic skill, which some interviewers may want to see you
     *     produce by hand.
     *
     * When to use: Great in production code or once you've already
     * demonstrated the manual binary-search approach and want to show a
     * cleaner alternative. I'd mention this as a follow-up/alternative
     * after coding Approach 2, to show breadth without spending extra
     * interview time re-deriving binary search.
     */
    public static int[] findRightIntervalTreeMap(int[][] intervals) {
        int intervalCount = intervals.length;
        int[] answer = new int[intervalCount];

        TreeMap<Integer, Integer> startToIndex = new TreeMap<>();
        for (int i = 0; i < intervalCount; i++) {
            startToIndex.put(intervals[i][0], i);
        }

        for (int i = 0; i < intervalCount; i++) {
            int targetEnd = intervals[i][1];
            Map.Entry<Integer, Integer> ceilingEntry = startToIndex.ceilingEntry(targetEnd);
            answer[i] = (ceilingEntry == null) ? -1 : ceilingEntry.getValue();
        }
        return answer;
    }

    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * Approach                  | Time       | Space | Best For                        | Limitations
     * --------------------------|------------|-------|---------------------------------|------------------------------------------
     * 1. Brute Force            | O(n^2)     | O(1)  | Correctness baseline, tiny n    | Quadratic blowup; not scalable
     * 2. Sort + Binary Search   | O(n log n) | O(n)  | Interview-optimal, low overhead | Manual binary search bug risk if rushed
     * 3. TreeMap Ceiling        | O(n log n) | O(n)  | Clean production code, clarity  | Higher constants (tree vs array access)
     *
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would present Approach 2 (Sorting + Binary Search) as the final
     * solution:
     *   - It's asymptotically optimal at O(n log n), matching the best
     *     possible complexity for this problem (we must at least look at
     *     every interval once, and sorting or an equivalent ordering step
     *     is unavoidable given arbitrary input order).
     *   - It demonstrates hands-on mastery of the lower-bound binary search
     *     pattern, which is a core primitive interviewers want to see
     *     coded correctly and confidently -- rather than delegating that
     *     logic entirely to a library data structure.
     *   - It has excellent constant factors (contiguous array access,
     *     cache-friendly), and is straightforward to trace/debug live on a
     *     whiteboard or shared editor.
     *   - After coding it, I'd proactively mention Approach 3 (TreeMap) as
     *     a "one-liner-per-query" alternative, showing awareness of
     *     library tools without leaning on them as a crutch for the core
     *     solution.
     */

    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE - OPTIMAL SOLUTION (PRODUCTION-QUALITY)
     * ========================================================================
     */

    /**
     * Computes, for each interval in {@code intervals}, the index of its
     * "right interval": the interval whose start value is the smallest
     * start value that is greater than or equal to this interval's end
     * value. If no such interval exists, the result for that position is
     * {@code -1}.
     *
     * <p>Algorithm: pairs each start value with its original array index,
     * sorts those pairs by start value, and then performs a lower-bound
     * binary search per interval to find the smallest start >= end_i.</p>
     *
     * @param intervals array of [start, end] pairs; start values are
     *                  assumed unique per problem constraints.
     * @return int array where result[i] is the original index of the right
     *         interval for intervals[i], or -1 if none exists.
     * @throws IllegalArgumentException if intervals is null, empty, or any
     *         entry does not have exactly two elements, or if start > end
     *         for any interval.
     */
    public static int[] findRightIntervalOptimal(int[][] intervals) {
        // --- Defensive validation ---
        if (intervals == null || intervals.length == 0) {
            throw new IllegalArgumentException("intervals must be non-null and non-empty");
        }
        int intervalCount = intervals.length;
        for (int i = 0; i < intervalCount; i++) {
            if (intervals[i] == null || intervals[i].length != 2) {
                throw new IllegalArgumentException("Each interval must have exactly 2 elements; bad entry at index " + i);
            }
            if (intervals[i][0] > intervals[i][1]) {
                throw new IllegalArgumentException("start must be <= end; violated at index " + i);
            }
        }

        // --- Step 1: Pair each start value with its original index, then sort by start. ---
        // We use a primitive int[][] rather than a boxed object array purely for
        // memory/cache efficiency; sortedStarts[k] = {startValue, originalIndex}.
        int[][] sortedStarts = new int[intervalCount][2];
        for (int i = 0; i < intervalCount; i++) {
            sortedStarts[i][0] = intervals[i][0];
            sortedStarts[i][1] = i;
        }
        // Since start values are guaranteed unique, there's no tie-breaking concern here.
        Arrays.sort(sortedStarts, (pairA, pairB) -> Integer.compare(pairA[0], pairB[0]));

        // --- Step 2: For each interval, binary search for the lower bound of end_i. ---
        int[] rightIntervalIndex = new int[intervalCount];
        for (int i = 0; i < intervalCount; i++) {
            rightIntervalIndex[i] = lowerBoundOriginalIndexOptimal(sortedStarts, intervals[i][1]);
        }

        return rightIntervalIndex;
    }

    /**
     * Finds the original index paired with the smallest start value in
     * {@code sortedStarts} that is {@code >= target}, using a standard
     * half-open-interval lower-bound binary search.
     *
     * @param sortedStarts array of {startValue, originalIndex} pairs, sorted
     *                     ascending by startValue.
     * @param target       the end_i value we need a start >= to.
     * @return the original index of the matching interval, or -1 if none.
     */
    private static int lowerBoundOriginalIndexOptimal(int[][] sortedStarts, int target) {
        // Half-open search range [lowInclusive, highExclusive).
        int lowInclusive = 0;
        int highExclusive = sortedStarts.length;

        while (lowInclusive < highExclusive) {
            // Avoids (low + high) overflow, though not a real risk at n <= 1000 -
            // still good habit for production code.
            int mid = lowInclusive + (highExclusive - lowInclusive) / 2;

            if (sortedStarts[mid][0] >= target) {
                // mid satisfies the condition; it's a candidate, but a smaller
                // index might also satisfy it, so keep searching the left half
                // (including mid itself, hence highExclusive = mid, not mid - 1).
                highExclusive = mid;
            } else {
                // mid's start is too small; the answer must be strictly to the right.
                lowInclusive = mid + 1;
            }
        }

        // After the loop, lowInclusive == highExclusive, pointing at the first
        // index satisfying start >= target, or == sortedStarts.length if none do.
        if (lowInclusive == sortedStarts.length) {
            return -1;
        }
        return sortedStarts[lowInclusive][1];
    }

    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     *
     * Tracing findRightIntervalOptimal on Example 1:
     *   intervals = [[3,4],[2,3],[1,2]]   (original indices 0, 1, 2)
     *
     * Step 1: Build sortedStarts pairs (start, originalIndex):
     *   Before sort: [[3,0],[2,1],[1,2]]
     *   After sort by start: [[1,2],[2,1],[3,0]]
     *   sortedStarts = { {1,2}, {2,1}, {3,0} }
     *
     * Step 2: For each original interval i, binary search lowerBound(end_i):
     *
     *   i = 0, intervals[0] = [3,4], target = end_0 = 4
     *     lowInclusive=0, highExclusive=3
     *     mid=1 -> sortedStarts[1][0]=2, 2 >= 4? No  -> lowInclusive=2
     *     mid=2 -> sortedStarts[2][0]=3, 3 >= 4? No  -> lowInclusive=3
     *     loop ends (lowInclusive==highExclusive==3) -> lowInclusive == length -> return -1
     *     rightIntervalIndex[0] = -1
     *
     *   i = 1, intervals[1] = [2,3], target = end_1 = 3
     *     lowInclusive=0, highExclusive=3
     *     mid=1 -> sortedStarts[1][0]=2, 2 >= 3? No  -> lowInclusive=2
     *     mid=2 -> sortedStarts[2][0]=3, 3 >= 3? Yes -> highExclusive=2
     *     loop ends (lowInclusive==highExclusive==2) -> sortedStarts[2][1] = 0
     *     rightIntervalIndex[1] = 0
     *
     *   i = 2, intervals[2] = [1,2], target = end_2 = 2
     *     lowInclusive=0, highExclusive=3
     *     mid=1 -> sortedStarts[1][0]=2, 2 >= 2? Yes -> highExclusive=1
     *     mid=0 -> sortedStarts[0][0]=1, 1 >= 2? No  -> lowInclusive=1
     *     loop ends (lowInclusive==highExclusive==1) -> sortedStarts[1][1] = 1
     *     rightIntervalIndex[2] = 1
     *
     * Final result: rightIntervalIndex = [-1, 0, 1]  -- matches Example 1's
     * expected answer.
     */

    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * - Brute force (O(n^2)) is a fine correctness anchor but doesn't scale;
     *   it's included purely to validate the faster approaches, not as a
     *   final answer.
     * - Sorting + binary search (O(n log n)) is the recommended, presented
     *   solution: optimal complexity, minimal space overhead relative to
     *   the TreeMap version, and demonstrates a hand-coded lower-bound
     *   binary search, which is a technique interviewers value seeing done
     *   correctly.
     * - TreeMap ceilingEntry (O(n log n)) is functionally equivalent and
     *   more concise, offered as an alternative/follow-up to show breadth.
     * - Known assumptions/limitations of the final solution:
     *     * Assumes start values are unique, per problem constraints -- if
     *       that guarantee were removed, we'd need a tie-breaking rule
     *       (e.g., smallest original index among equal starts) and the
     *       sorted-pairs structure would need a secondary sort key.
     *     * Assumes intervals is non-null/non-empty and each entry is a
     *       valid [start, end] pair with start <= end; these are validated
     *       defensively and will throw IllegalArgumentException otherwise.
     *     * All comparisons use primitive int, which is safe given bounds
     *       of [-1e6, 1e6] -- no overflow risk.
     */

    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     *
     * 1. "What if start values were NOT guaranteed unique?"
     *    -> Response: Sort by (start, originalIndex) as a composite key, and
     *       when multiple intervals share the same start, apply a defined
     *       tie-break (e.g., lowest original index) at the ceiling/lower-
     *       bound step. The binary search logic itself barely changes.
     *
     * 2. "Can you solve this with O(1) extra space (excluding output and
     *    input)?"
     *    -> Response: Not while preserving original-index output in-place
     *       without extra bookkeeping, because sorting scrambles order and
     *       we must remember where each start came from. We could sort
     *       indices via an index array in-place, but we still need O(n)
     *       auxiliary space for the index-to-start mapping in the worst
     *       case; true O(1) extra space isn't achievable without a
     *       fundamentally different structure.
     *
     * 3. "What if the intervals array were streamed / built incrementally,
     *    and you had to answer 'right interval' queries as intervals
     *    arrive?"
     *    -> Response: Switch to a balanced BST / TreeMap that supports
     *       O(log n) insertion and O(log n) ceiling queries dynamically --
     *       Approach 3 generalizes naturally to this streaming scenario,
     *       whereas Approach 2's static sorted array would need periodic
     *       rebuilding or a different self-balancing structure.
     *
     * 4. "What if n could be up to 10^7 instead of 1000 -- does your
     *    approach still hold?"
     *    -> Response: Yes, O(n log n) with a flat sorted array (Approach 2)
     *       remains efficient and is preferable at that scale specifically
     *       because of its better cache locality versus a tree-based
     *       structure.
     *
     * 5. "Could you parallelize this?"
     *    -> Response: The sort step can use a parallel sort
     *       (Arrays.parallelSort), and the binary-search-per-interval step
     *       is embarrassingly parallel since each query is independent --
     *       both stages parallelize cleanly.
     *
     * 6. "How would you test this solution thoroughly?"
     *    -> Response: Cross-validate against the brute-force oracle on
     *       randomized inputs (as done in main() below), plus targeted
     *       edge cases: single interval, all intervals needing -1, all
     *       intervals resolving to themselves (start_i == end_i for all i),
     *       and negative-value bounds near -1e6/1e6.
     */

    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Returning the VALUE or the SORTED-ARRAY POSITION instead of the
     *    ORIGINAL ARRAY INDEX. This is the single most common bug: after
     *    sorting, candidates forget to carry the original index along with
     *    each start value, and end up returning indices into the sorted
     *    array instead of the input array.
     *
     * 2. Off-by-one errors in the binary search bounds -- especially
     *    confusing "find first element >= target" (lower bound) with
     *    "find first element > target" (upper bound). Using a consistent
     *    half-open interval [low, high) convention, as done here, avoids
     *    most of these mistakes.
     *
     * 3. Forgetting that i == j is a VALID answer. Some candidates add
     *    logic to explicitly skip or exclude interval i itself as a
     *    candidate for its own right interval, which is incorrect -- the
     *    problem allows self-reference when start_i itself is the smallest
     *    start >= end_i.
     *
     * 4. Assuming a plain HashMap suffices. A HashMap gives O(1) exact-key
     *    lookups but cannot answer "smallest key >= x" range-style queries
     *    -- that requires an ordered structure (sorted array + binary
     *    search, or TreeMap/balanced BST). Candidates sometimes reach for
     *    HashMap out of habit and get stuck when they realize it can't
     *    support the ceiling query they actually need.
     */

    /*
     * ========================================================================
     * TEST HARNESS: CROSS-VALIDATION ACROSS ALL APPROACHES
     * ========================================================================
     */
    public static void main(String[] args) {
        List<int[][]> testCases = new ArrayList<>();
        List<String> testNames = new ArrayList<>();

        testNames.add("Example 1 - normal case");
        testCases.add(new int[][]{{3, 4}, {2, 3}, {1, 2}});

        testNames.add("Example 2 - single interval, no valid right interval");
        testCases.add(new int[][]{{1, 2}});

        testNames.add("Example 3 - boundary/self-reference case");
        testCases.add(new int[][]{{5, 5}, {7, 7}, {6, 9}});

        testNames.add("All zero-length intervals, all self-referencing");
        testCases.add(new int[][]{{0, 0}, {-5, -5}, {10, 10}});

        testNames.add("Extreme bounds (-1e6 to 1e6)");
        testCases.add(new int[][]{{-1000000, -1000000}, {1000000, 1000000}, {0, 500000}});

        testNames.add("No interval has a valid right interval");
        testCases.add(new int[][]{{5, 100}, {6, 200}, {7, 300}});

        testNames.add("Larger mixed case");
        testCases.add(new int[][]{{1, 4}, {2, 3}, {3, 4}, {4, 5}, {5, 5}, {-1, 0}});

        boolean allPassed = true;
        for (int caseIndex = 0; caseIndex < testCases.size(); caseIndex++) {
            int[][] intervals = testCases.get(caseIndex);

            int[] bruteForceResult = findRightIntervalBruteForce(intervals);
            int[] binarySearchResult = findRightIntervalBinarySearch(intervals);
            int[] treeMapResult = findRightIntervalTreeMap(intervals);
            int[] optimalResult = findRightIntervalOptimal(intervals);

            boolean matches = Arrays.equals(bruteForceResult, binarySearchResult)
                    && Arrays.equals(bruteForceResult, treeMapResult)
                    && Arrays.equals(bruteForceResult, optimalResult);

            allPassed &= matches;

            System.out.println("Test: " + testNames.get(caseIndex));
            System.out.println("  Input          : " + deepToString(intervals));
            System.out.println("  BruteForce     : " + Arrays.toString(bruteForceResult));
            System.out.println("  BinarySearch   : " + Arrays.toString(binarySearchResult));
            System.out.println("  TreeMap        : " + Arrays.toString(treeMapResult));
            System.out.println("  Optimal        : " + Arrays.toString(optimalResult));
            System.out.println("  All approaches agree: " + matches);
            System.out.println();
        }

        // Randomized stress test: brute force vs. optimal on random inputs.
        Random random = new Random(42);
        for (int trial = 0; trial < 200; trial++) {
            int n = 1 + random.nextInt(50);
            int[][] randomIntervals = generateRandomIntervalsWithUniqueStarts(n, random);

            int[] expected = findRightIntervalBruteForce(randomIntervals);
            int[] actual = findRightIntervalOptimal(randomIntervals);

            if (!Arrays.equals(expected, actual)) {
                allPassed = false;
                System.out.println("MISMATCH on random trial " + trial + ": " + deepToString(randomIntervals));
                System.out.println("  expected=" + Arrays.toString(expected));
                System.out.println("  actual  =" + Arrays.toString(actual));
            }
        }

        System.out.println("Randomized stress test (200 trials) complete.");
        System.out.println(allPassed ? "ALL TESTS PASSED" : "SOME TESTS FAILED");
    }

    // Generates n random intervals with guaranteed-unique start values, as
    // required by the problem constraints.
    private static int[][] generateRandomIntervalsWithUniqueStarts(int n, Random random) {
        int[][] intervals = new int[n][2];
        Set<Integer> usedStarts = new HashSet<>();
        for (int i = 0; i < n; i++) {
            int start;
            do {
                start = random.nextInt(2001) - 1000; // range [-1000, 1000] for readability
            } while (!usedStarts.add(start));
            int end = start + random.nextInt(50); // end >= start
            intervals[i][0] = start;
            intervals[i][1] = end;
        }
        return intervals;
    }

    private static String deepToString(int[][] arr) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            builder.append(Arrays.toString(arr[i]));
            if (i < arr.length - 1) builder.append(", ");
        }
        builder.append("]");
        return builder.toString();
    }
}
