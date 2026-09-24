import java.util.*;

/**
 * ============================================================================
 * GOOGLE-STYLE MOCK INTERVIEW: "Remove Covered Intervals" (LeetCode 1288)
 * ============================================================================
 * This single file walks through the full interview flow end-to-end, exactly
 * as it should be presented in a real onsite/virtual Google interview:
 * restatement -> clarifications -> examples -> paradigm sweep -> every
 * meaningful approach -> comparison -> recommendation -> polished optimal
 * solution -> dry run -> closing summary -> follow-ups -> common traps.
 * ============================================================================
 */
class RemoveCoveredIntervals {

    /* ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     * In plain English:
     *   We're given a list of intervals, each written [l, r) meaning "starts
     *   at l, ends just before r" (half-open, per the problem's own note that
     *   l is inclusive and r is exclusive -- despite the text loosely saying
     *   "inclusive of l, exclusive of r", the covering rule given is what
     *   actually matters operationally).
     *
     *   An interval [a, b) is "covered" by another interval [c, d) if that
     *   other interval fully contains it: c <= a AND b <= d. In other words,
     *   [c, d) starts no later and ends no earlier than [a, b).
     *
     *   We must remove every interval that is covered by at least one other
     *   interval in the list, and return the COUNT of intervals that remain
     *   (not the intervals themselves).
     *
     * Key constraints / inputs / outputs:
     *   - Input: int[][] intervals, 1 <= intervals.length <= 1000
     *   - Each intervals[i] has exactly two ints: [li, ri], with 0 <= li < ri <= 1e5
     *   - All intervals are guaranteed unique (no two identical [l, r] pairs)
     *   - Output: a single int -- the number of intervals remaining after
     *     removing all covered intervals
     *
     * Explicit assumptions I'm confirming before coding:
     *   - "Covered by another interval" means by ANY one other interval, not
     *     by the union of several intervals combined.
     *   - Equal intervals are excluded by the "uniqueness" guarantee, so I
     *     don't need to worry about an interval "covering itself".
     * ========================================================================
     */

    /* ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (asked to interviewer, with assumed answers)
     * ========================================================================
     * 1. Q: Can two intervals share the same start point (li) or same end
     *       point (ri)?
     *    A (assumed): Yes. Only the exact (l, r) pair is guaranteed unique;
     *       shared starts or shared ends independently are allowed and are
     *       actually a key edge case (e.g., [1,4) and [1,5)).
     *
     * 2. Q: Is "covered" strict containment, or does an interval count as
     *       covered by an identical interval?
     *    A (assumed): Since all intervals are unique, this can't happen, but
     *       conceptually the definition c <= a and b <= d would count an
     *       identical interval as "covering" -- moot here due to uniqueness.
     *
     * 3. Q: Should I return the count only, or also the surviving intervals
     *       themselves?
     *    A (assumed): Return only the count, per the stated signature.
     *
     * 4. Q: What's the expected time complexity given n <= 1000? Is an
     *       O(n^2) brute force acceptable, or do you want O(n log n)?
     *    A (assumed): n <= 1000 means O(n^2) brute force (up to ~1,000,000
     *       comparisons) is technically safe, but I should aim for O(n log n)
     *       to demonstrate the sorting insight -- that's clearly what this
     *       problem is testing.
     *
     * 5. Q: Can intervals have zero length, i.e., li == ri?
     *    A (assumed): No -- constraint explicitly states li < ri, so every
     *       interval has positive length.
     *
     * 6. Q: Do I need to handle negative coordinates?
     *    A (assumed): No -- constraint guarantees 0 <= li, ri <= 1e5.
     *
     * 7. Q: Is the input array mutable -- can I sort it in place, or should I
     *       preserve the original order/array?
     *    A (assumed): Fine to sort in place or on a copy; no requirement to
     *       preserve original ordering since we only return a count.
     *
     * 8. Q: Is this a single-threaded call, or do I need to worry about
     *       concurrent access to the same intervals array?
     *    A (assumed): Single-threaded; no concurrency concerns for this
     *       problem -- it's a pure computational function.
     * ========================================================================
     */

    /* ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     * Example 1 (normal case):
     *   intervals = [[1,4], [3,6], [2,8]]
     *   - [1,4) vs [2,8): is [1,4) covered? need c<=1 and 4<=d for the
     *     covering interval; [2,8) has c=2 which is NOT <= 1, so no.
     *   - [3,6) vs [2,8): c=2<=3 and d=8, 6<=8 -> TRUE. [3,6) is covered.
     *   - [1,4) vs [3,6) or others: not covered by any.
     *   - [2,8) is not covered by anything (it's the largest span here).
     *   Remaining: [1,4) and [2,8) -> answer = 2.
     *
     * Example 2 (edge case -- single interval):
     *   intervals = [[1,4]]
     *   Nothing to compare against -> trivially not covered.
     *   Answer = 1.
     *
     * Example 3 (boundary / tie-breaking case -- shared start points):
     *   intervals = [[1,4], [1,5], [1,3]]
     *   - [1,3): c=1<=1, d=5, 3<=5 -> covered by [1,5). Also covered by [1,4).
     *   - [1,4): c=1<=1, d=5, 4<=5 -> covered by [1,5).
     *   - [1,5): is it covered by [1,4)? need d=4 >= 5 -> false. Not covered.
     *   Remaining: only [1,5) -> answer = 1.
     *   This is the critical tie-breaking scenario: when starts are equal,
     *   the interval with the LARGEST end "wins" and covers all others with
     *   that same start. Any sorting-based solution must handle this by
     *   sorting ties on start in DESCENDING order of end -- otherwise a
     *   shorter interval processed before a longer one with the same start
     *   will be incorrectly counted as "not yet covered".
     * ========================================================================
     */

    /* ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES (paradigm sweep)
     * ========================================================================
     * Applicable paradigms:
     *   - Brute force / naive (pairwise comparison)               -> APPLIES
     *   - Sorting-based (sort by start, sweep with running max)   -> APPLIES (optimal)
     *   - Greedy (the sweep after sorting is itself a greedy pass)-> APPLIES, folded into sorting approach
     *
     * Paradigms considered and explicitly ruled out:
     *   - Hashing-based: There's no "lookup by key" structure here; coverage
     *     depends on ORDER and RANGE relationships between intervals, not
     *     equality/identity lookups, so hashing doesn't reduce the work.
     *   - Two pointer / sliding window: These techniques operate on a single
     *     sequence with a contiguous "window" notion. Here we're comparing
     *     arbitrary pairs of intervals for containment, not scanning a
     *     window of fixed/variable size over one array -- doesn't fit.
     *   - Divide and conquer: You could split the interval set, solve each
     *     half, and merge -- but the merge step still needs to check
     *     cross-half containment, which degenerates back to sorting/sweeping.
     *     No asymptotic win over the sorting approach, so not worth the
     *     added complexity.
     *   - Dynamic programming: There's no overlapping-subproblem / optimal
     *     substructure recurrence here -- coverage is a pairwise geometric
     *     relationship, not a sequence of decisions with reusable state.
     *   - Tree / graph traversal: No explicit tree/graph structure in the
     *     input; you could model containment as a DAG and do topological
     *     reasoning, but that's strictly more expensive than sorting and
     *     adds no benefit.
     *   - Heap / priority queue: A heap would help if we needed the
     *     "currently max-reaching interval" dynamically extracted across
     *     an unsorted stream, but since we control the full sort up front,
     *     a heap adds overhead (O(log n) per op) with no benefit over
     *     tracking a single running max after sorting.
     *   - Binary search: Useful for point/range QUERIES against a sorted
     *     structure, but here we need to process every interval once
     *     against a running aggregate -- there's no search target that
     *     binary search would accelerate.
     *   - Monotonic stack / deque: This is actually a reasonable alternative
     *     encoding of the same idea (push intervals, pop when covered) --
     *     included below as its own approach since it's a genuinely
     *     different implementation lens even though it converges to the
     *     same complexity as the sorting+sweep approach.
     *   - Trie / segment tree: Segment trees shine for range queries/updates
     *     over a mutable structure queried many times; here we do a single
     *     pass with O(1) aggregate state (a running max), so a segment tree
     *     is pure overhead for this problem.
     * ========================================================================
     */

    /* ------------------------------------------------------------------------
     * Approach 1: Brute Force (Pairwise Comparison)
     * ------------------------------------------------------------------------
     * Core idea: For every interval i, scan every other interval j and check
     * if j fully covers i. If any such j exists, mark i as covered.
     *
     * Data structure / paradigm: None beyond the raw array -- pure nested
     * iteration (naive/brute force paradigm).
     *
     * Time Complexity: O(n^2) -- for each of n intervals, we scan up to n-1
     * others.
     * Space Complexity: O(n) -- a boolean array to track which intervals are
     * covered (O(1) extra if we just keep a running counter instead, but the
     * boolean array is clearer for explanation).
     *
     * Pros:
     *   - Trivial to reason about and verify correctness against.
     *   - No sorting or tie-breaking subtlety to get wrong.
     * Cons:
     *   - Quadratic time; with n = 1000 that's up to ~1,000,000 comparisons
     *     -- acceptable here but doesn't scale, and won't impress in an
     *     interview as a final answer.
     *   - Does not showcase algorithmic insight expected for this problem.
     *
     * When to use: Only as a warm-up/baseline to lock in correctness before
     * pivoting to the optimal approach, or if n were tiny and code simplicity
     * mattered more than performance.
     * ------------------------------------------------------------------------
     */
    public int removeCoveredIntervalsBruteForce(int[][] intervals) {
        int intervalCount = intervals.length;
        boolean[] isCovered = new boolean[intervalCount];

        for (int i = 0; i < intervalCount; i++) {
            int startA = intervals[i][0];
            int endA = intervals[i][1];
            for (int j = 0; j < intervalCount; j++) {
                if (i == j) {
                    continue;
                }
                int startC = intervals[j][0];
                int endD = intervals[j][1];
                // [startA, endA) is covered by [startC, endD) if
                // startC <= startA AND endA <= endD
                if (startC <= startA && endA <= endD) {
                    isCovered[i] = true;
                    break; // one covering interval is enough
                }
            }
        }

        int remainingCount = 0;
        for (boolean covered : isCovered) {
            if (!covered) {
                remainingCount++;
            }
        }
        return remainingCount;
    }

    /* ------------------------------------------------------------------------
     * Approach 2: Sorting + Single Pass with Running Max End (OPTIMAL)
     * ------------------------------------------------------------------------
     * Core idea: Sort intervals by start ascending; break ties by end
     * DESCENDING (this is the crucial trick -- see Example 3 above). Then
     * sweep left to right, maintaining the maximum end seen so far among
     * intervals already confirmed "not covered". Since starts are
     * non-decreasing as we sweep, any interval whose end does not exceed
     * the running max end is necessarily covered by some earlier interval
     * (which has an equal-or-smaller start and an equal-or-larger end).
     *
     * Data structure / paradigm: Sorting + greedy linear sweep.
     *
     * Time Complexity: O(n log n) -- dominated by the sort; the sweep itself
     * is O(n).
     * Space Complexity: O(log n) to O(n) depending on the sort algorithm's
     * internal stack/buffer (Java's Arrays.sort on objects uses TimSort,
     * which is O(n) auxiliary space in the worst case); O(1) extra beyond
     * the sort itself for the sweep.
     *
     * Pros:
     *   - Optimal achievable complexity for this problem (you can't avoid at
     *     least sorting-level work since the answer depends on relative
     *     order of starts/ends).
     *   - Simple, easy-to-verify invariant (running max end).
     *   - No auxiliary data structures beyond the sort.
     * Cons:
     *   - The tie-breaking rule (descending end on equal start) is a subtle
     *     detail that's easy to get wrong under interview pressure.
     *
     * When to use: This is the approach to present as the final answer in
     * an interview -- it's optimal, clean, and demonstrates the key insight.
     * ------------------------------------------------------------------------
     */
    public int removeCoveredIntervalsOptimal(int[][] intervals) {
        // Sort by start ascending; for equal starts, sort by end descending
        // so that the "widest" interval at a given start is processed first
        // and correctly covers the narrower ones that share that start.
        Arrays.sort(intervals, (intervalA, intervalB) -> {
            if (intervalA[0] != intervalB[0]) {
                return Integer.compare(intervalA[0], intervalB[0]);
            }
            return Integer.compare(intervalB[1], intervalA[1]); // descending end
        });

        int remainingCount = 0;
        int runningMaxEnd = Integer.MIN_VALUE;

        for (int[] interval : intervals) {
            int currentEnd = interval[1];
            // Because of the sort order, if currentEnd > runningMaxEnd, no
            // previously seen interval can cover this one (any interval that
            // could cover it would need an end >= currentEnd, which would
            // have already raised runningMaxEnd at least that high).
            if (currentEnd > runningMaxEnd) {
                remainingCount++;
                runningMaxEnd = currentEnd;
            }
            // else: currentEnd <= runningMaxEnd means some earlier interval
            // (with start <= this start, due to sort order) has an end that
            // reaches at least as far -- this interval is covered; skip it.
        }

        return remainingCount;
    }

    /* ------------------------------------------------------------------------
     * Approach 3: Sorting + Monotonic Stack
     * ------------------------------------------------------------------------
     * Core idea: Same sort order as Approach 2, but instead of tracking just
     * a running max end as a single variable, explicitly maintain a stack of
     * "surviving" intervals. For each new interval, compare against the top
     * of the stack: if the new interval is covered by the top, skip it; the
     * new interval can never cover something already on the stack given the
     * sort order, so we never need to pop for coverage reasons here (unlike
     * classic monotonic-stack problems where you pop repeatedly). This
     * collapses to behavior equivalent to Approach 2, but is included since
     * it's a genuinely different implementation lens (stack of surviving
     * intervals) that some interviewers like to see reasoned through
     * explicitly, and it generalizes more visibly if you also needed to
     * RETURN the surviving intervals themselves (not just the count).
     *
     * Data structure / paradigm: Sorting + monotonic stack.
     *
     * Time Complexity: O(n log n) -- dominated by the sort; each interval is
     * pushed at most once (O(n) total stack operations).
     * Space Complexity: O(n) for the stack in the worst case (no intervals
     * covered).
     *
     * Pros:
     *   - Directly generalizes to "return the surviving intervals" variant
     *     without restructuring the algorithm.
     *   - Still O(n log n), same as the optimal approach.
     * Cons:
     *   - More code and more state (an explicit stack) than Approach 2 for
     *     no asymptotic benefit when we only need a count.
     *   - Slightly higher constant-factor overhead (stack push/peek vs a
     *     single int comparison).
     *
     * When to use: If the interviewer extends the problem to "return the
     * actual list of surviving intervals" rather than just the count, this
     * approach's structure adapts more naturally than Approach 2.
     * ------------------------------------------------------------------------
     */
    public int removeCoveredIntervalsMonotonicStack(int[][] intervals) {
        Arrays.sort(intervals, (intervalA, intervalB) -> {
            if (intervalA[0] != intervalB[0]) {
                return Integer.compare(intervalA[0], intervalB[0]);
            }
            return Integer.compare(intervalB[1], intervalA[1]);
        });

        Deque<int[]> survivingStack = new ArrayDeque<>();

        for (int[] interval : intervals) {
            if (survivingStack.isEmpty()) {
                survivingStack.push(interval);
                continue;
            }
            int[] topOfStack = survivingStack.peek();
            // topOfStack has start <= interval's start (sort order).
            // If topOfStack's end also reaches >= interval's end, this
            // interval is covered by topOfStack -- skip it.
            if (topOfStack[1] >= interval[1]) {
                continue; // covered, discard
            }
            survivingStack.push(interval);
        }

        return survivingStack.size();
    }

    /* ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     * Approach                    | Time       | Space   | Best For                          | Limitations
     * ---------------------------- | ---------- | ------- | --------------------------------- | ----------------------------------------
     * 1. Brute Force               | O(n^2)     | O(n)    | Baseline correctness check, tiny n| Too slow to present as final answer
     * 2. Sorting + Running Max     | O(n log n) | O(n)*   | The interview-optimal solution    | Requires correct tie-break on equal starts
     * 3. Sorting + Monotonic Stack | O(n log n) | O(n)    | Variant returning surviving list  | Extra code/overhead vs Approach 2 for count-only
     *
     * *Approach 2's O(n) space is due to Java's TimSort auxiliary array on
     * Integer/object arrays in the worst case; the sweep itself is O(1).
     * ========================================================================
     */

    /* ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     * I would present Approach 2 (Sorting + Single Pass with Running Max End)
     * as the final answer, for these reasons:
     *   - It's asymptotically optimal for this problem -- O(n log n) time,
     *     which is what any experienced interviewer expects for an interval
     *     problem of this shape.
     *   - It's the fastest to CODE correctly under time pressure: one sort
     *     call with a two-key comparator, then a single linear pass with one
     *     integer variable tracking state. Minimal surface area for bugs.
     *   - It clearly demonstrates the key insight (the tie-breaking rule on
     *     equal starts), which is exactly what distinguishes a candidate who
     *     understands the problem from one who's pattern-matching blindly.
     *   - I'd mention Approach 3 (monotonic stack) as a natural extension if
     *     asked to also return the surviving intervals, showing I've thought
     *     ahead about variants without over-engineering the base solution.
     *   - I would NOT lead with the brute force as my final answer, but I
     *     would state it first verbally as the "safe baseline" before pivoting
     *     to the optimal solution -- this is the safe-then-optimal narrative
     *     flow that signals structured thinking under interview conditions.
     * ========================================================================
     */

    /* ========================================================================
     * SECTION 9: DEEP DIVE -- PRODUCTION-QUALITY OPTIMAL SOLUTION
     * ========================================================================
     */

    /**
     * Returns the count of intervals remaining after removing every interval
     * that is completely covered by another interval in the input.
     *
     * <p>An interval {@code [a, b)} is covered by {@code [c, d)} if and only
     * if {@code c <= a} and {@code b <= d}.
     *
     * <p><b>Algorithm:</b> Sort intervals by start ascending; break ties by
     * end descending. Sweep left to right while tracking the maximum end
     * seen among intervals already confirmed not covered. An interval
     * survives if and only if its end strictly exceeds that running maximum.
     *
     * <p><b>Correctness invariant:</b> After processing index {@code i} in
     * sorted order, {@code runningMaxEnd} equals the maximum end among all
     * surviving (non-covered) intervals in {@code sortedIntervals[0..i]}.
     * Because starts are non-decreasing in this order, any later interval
     * whose end does not exceed {@code runningMaxEnd} is guaranteed to be
     * covered by the specific earlier interval that set that maximum.
     *
     * @param intervals array of {@code [start, end)} pairs; must satisfy
     *                  {@code 0 <= start < end <= 100_000} and be pairwise
     *                  unique, per problem constraints
     * @return the number of intervals not covered by any other interval
     * @throws IllegalArgumentException if intervals is null or empty
     */
    public int removeCoveredIntervals(int[][] intervals) {
        if (intervals == null || intervals.length == 0) {
            throw new IllegalArgumentException("intervals must be non-null and non-empty");
        }

        // Defensive copy so we don't mutate the caller's array ordering --
        // good production hygiene even though the problem doesn't require it.
        int[][] sortedIntervals = intervals.clone();

        // Comparator: start ascending, then end descending on ties.
        // The descending-end tie-break is THE critical correctness detail:
        // it guarantees that when multiple intervals share a start, the
        // widest one is visited first and sets runningMaxEnd high enough
        // to correctly mark the narrower same-start intervals as covered.
        Arrays.sort(sortedIntervals, (first, second) -> {
            if (first[0] != second[0]) {
                return Integer.compare(first[0], second[0]);
            }
            return Integer.compare(second[1], first[1]);
        });

        int survivingIntervalCount = 0;
        int runningMaxEnd = Integer.MIN_VALUE;

        for (int[] currentInterval : sortedIntervals) {
            int currentStart = currentInterval[0]; // unused directly beyond sort order, kept for clarity
            int currentEnd = currentInterval[1];

            if (currentEnd > runningMaxEnd) {
                // Not covered by anything seen so far: no earlier interval
                // (all of which have start <= currentStart) reaches this far.
                survivingIntervalCount++;
                runningMaxEnd = currentEnd;
            }
            // else: currentEnd <= runningMaxEnd -> some earlier interval with
            // an equal-or-smaller start already reaches at least this far,
            // so currentInterval is covered. Skip without incrementing.
        }

        return survivingIntervalCount;
    }

    /* ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     * Tracing removeCoveredIntervals on intervals = [[1,4], [3,6], [2,8]]
     *
     * Step 1: Sort by start ascending, end descending on ties.
     *   Sorted order: [1,4], [2,8], [3,6]
     *   (starts 1 < 2 < 3, no ties to break here)
     *
     * Step 2: Initialize survivingIntervalCount = 0, runningMaxEnd = MIN_VALUE
     *
     * Step 3: Iterate:
     *   i=0: currentInterval = [1,4], currentEnd = 4
     *        4 > MIN_VALUE -> survives.
     *        survivingIntervalCount = 1, runningMaxEnd = 4
     *
     *   i=1: currentInterval = [2,8], currentEnd = 8
     *        8 > 4 -> survives.
     *        survivingIntervalCount = 2, runningMaxEnd = 8
     *
     *   i=2: currentInterval = [3,6], currentEnd = 6
     *        6 > 8? No -> covered (by [2,8), since start 2 <= 3 and end 8 >= 6).
     *        survivingIntervalCount stays 2, runningMaxEnd stays 8
     *
     * Step 4: Return survivingIntervalCount = 2.
     *
     * Final answer: 2 -- matches Example 1's expected result ([1,4) and
     * [2,8) survive; [3,6) is covered).
     * ========================================================================
     */

    /* ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     * - Brute force (O(n^2)) is a correct, easy-to-verify baseline but too
     *   slow to present as a final answer for n up to 1000 in a Google
     *   interview context -- useful only as a stated starting point.
     * - The sorting + running-max sweep (O(n log n)) is the optimal and
     *   recommended solution: minimal code, minimal state, and it directly
     *   exposes the key insight (tie-break by descending end on equal
     *   starts).
     * - The monotonic stack variant is complexity-equivalent but carries
     *   more implementation overhead; it's valuable mainly as a stepping
     *   stone if the problem is extended to require the surviving intervals
     *   themselves, not just their count.
     * - Known limitations / assumptions of the final solution:
     *     * Assumes input intervals are well-formed per constraints
     *       (0 <= start < end <= 1e5, all pairs unique); no defensive
     *       validation of individual interval shape beyond null/empty checks.
     *     * Uses half-open interval semantics consistently ([start, end)),
     *       matching the covering rule given, even though the prose
     *       description is slightly inconsistent about inclusivity.
     *     * Sorts a cloned array rather than mutating the caller's input --
     *       a minor production-hygiene choice, not required by the problem.
     * ========================================================================
     */

    /* ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     * 1. "Can you return the surviving intervals themselves instead of just
     *     the count?" -> Adapt Approach 3 (monotonic stack): push survivors,
     *     return the stack contents instead of its size.
     *
     * 2. "What if intervals can be added/removed dynamically and you need to
     *     answer 'how many survive' after each update?" -> This pushes toward
     *     a balanced BST / order-statistics structure (e.g., a TreeMap keyed
     *     by start, maintaining max-end info) to support incremental updates
     *     in better than O(n) per update, rather than re-sorting from scratch.
     *
     * 3. "What if n could be up to 10^7 instead of 1000?" -> O(n log n)
     *     sorting-based approach still applies and remains the right choice;
     *     I'd discuss using primitive-array-friendly sorting (e.g., sorting
     *     an index array via encoded long keys to avoid autoboxing overhead
     *     from Integer[]/comparator-based sorts) to reduce constant factors.
     *
     * 4. "What if intervals were closed on both ends [l, r] instead of
     *     half-open?" -> The covering condition and sort/sweep logic are
     *     unaffected in form (c <= a and b <= d still works identically);
     *     only the interpretation of boundary touching changes, not the code.
     *
     * 5. "Can you also remove intervals covered by the UNION of several other
     *     intervals, not just a single one?" -> That's a materially different
     *     and harder problem (akin to interval merging / coverage by a set),
     *     requiring a merge-based sweep tracking merged coverage ranges
     *     rather than a single running max.
     *
     * 6. "How would you test this solution?" -> Unit tests covering: single
     *     interval, fully nested chains, disjoint intervals (no covering),
     *     equal-start ties, equal-end ties, and large randomized inputs
     *     cross-validated against the brute-force approach.
     * ========================================================================
     */

    /* ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     * 1. Forgetting the tie-break rule entirely: sorting only by start (with
     *    no secondary key) and using a running-max sweep will silently
     *    produce WRONG answers whenever two intervals share a start point
     *    (see Example 3) -- this is the single most common bug on this
     *    problem.
     *
     * 2. Getting the tie-break direction backwards (ascending end instead of
     *    descending end) -- this looks superficially reasonable but processes
     *    the narrower interval before the wider one at a shared start,
     *    causing the narrower one to be incorrectly counted as "surviving"
     *    before the wider one arrives and would have covered it.
     *
     * 3. Off-by-one/boundary confusion between "covered" (<=, <=) and merely
     *    "overlapping" -- candidates sometimes implement classic interval-
     *    merge logic (checking for any overlap) instead of the strict
     *    containment check this problem actually asks for.
     *
     * 4. Initializing the running max end to 0 instead of a true sentinel
     *    (e.g., Integer.MIN_VALUE or the first interval's end before the
     *    loop). Since ends can be as low as 1 (with starts >= 0), a 0
     *    initial value happens to work for this problem's constraints, but
     *    it's fragile reasoning -- an interviewer may probe on whether the
     *    candidate understands WHY the sentinel choice is safe rather than
     *    just getting lucky with the given bounds.
     * ========================================================================
     */

    /* ========================================================================
     * TEST HARNESS -- cross-validates all three approaches against each other
     * and against hand-verified expected outputs from the examples above.
     * ========================================================================
     */
    public static void main(String[] args) {
        RemoveCoveredIntervals solution = new RemoveCoveredIntervals();

        List<TestCase> testCases = new ArrayList<>();
        testCases.add(new TestCase("Normal case", new int[][]{{1, 4}, {3, 6}, {2, 8}}, 2));
        testCases.add(new TestCase("Single interval", new int[][]{{1, 4}}, 1));
        testCases.add(new TestCase("Shared-start tie-break", new int[][]{{1, 4}, {1, 5}, {1, 3}}, 1));
        testCases.add(new TestCase("Disjoint intervals", new int[][]{{1, 2}, {3, 4}, {5, 6}}, 3));
        testCases.add(new TestCase("Fully nested chain", new int[][]{{1, 10}, {2, 9}, {3, 8}, {4, 7}}, 1));
        testCases.add(new TestCase("Shared end, different start", new int[][]{{1, 5}, {2, 5}, {3, 5}}, 1));

        int passedCount = 0;
        for (TestCase testCase : testCases) {
            // Each approach gets its own cloned array since sorting mutates input.
            int bruteForceResult = solution.removeCoveredIntervalsBruteForce(cloneIntervals(testCase.intervals));
            int optimalResult = solution.removeCoveredIntervals(cloneIntervals(testCase.intervals));
            int stackResult = solution.removeCoveredIntervalsMonotonicStack(cloneIntervals(testCase.intervals));

            boolean allMatchExpected = bruteForceResult == testCase.expected
                    && optimalResult == testCase.expected
                    && stackResult == testCase.expected;

            System.out.printf(
                    "[%s] expected=%d brute=%d optimal=%d stack=%d -> %s%n",
                    testCase.description, testCase.expected, bruteForceResult, optimalResult, stackResult,
                    allMatchExpected ? "PASS" : "FAIL"
            );

            if (allMatchExpected) {
                passedCount++;
            }
        }

        System.out.printf("%n%d / %d test cases passed.%n", passedCount, testCases.size());

        // Randomized cross-validation: brute force vs optimal on random inputs.
        runRandomizedCrossValidation(solution, 2000);
    }

    private static void runRandomizedCrossValidation(RemoveCoveredIntervals solution, int trialCount) {
        Random random = new Random(2026);
        int mismatchCount = 0;

        for (int trial = 0; trial < trialCount; trial++) {
            int intervalCount = 1 + random.nextInt(10);
            Set<Long> seenPairs = new HashSet<>();
            List<int[]> generated = new ArrayList<>();

            while (generated.size() < intervalCount) {
                int start = random.nextInt(15);
                int end = start + 1 + random.nextInt(15);
                long encodedKey = (long) start * 100_000L + end;
                if (seenPairs.add(encodedKey)) {
                    generated.add(new int[]{start, end});
                }
            }

            int[][] intervalsArray = generated.toArray(new int[0][]);
            int bruteForceResult = solution.removeCoveredIntervalsBruteForce(cloneIntervals(intervalsArray));
            int optimalResult = solution.removeCoveredIntervals(cloneIntervals(intervalsArray));

            if (bruteForceResult != optimalResult) {
                mismatchCount++;
                System.out.println("MISMATCH on: " + Arrays.deepToString(intervalsArray)
                        + " brute=" + bruteForceResult + " optimal=" + optimalResult);
            }
        }

        System.out.printf("Randomized cross-validation: %d trials, %d mismatches.%n", trialCount, mismatchCount);
    }

    private static int[][] cloneIntervals(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    /**
     * Simple record capturing a named test case with its expected output.
     * Uses a Java record (Java 16+, idiomatic in Java 21+) for concise,
     * immutable test data.
     */
    private record TestCase(String description, int[][] intervals, int expected) {
    }
}
