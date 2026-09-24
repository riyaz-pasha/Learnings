import java.util.*;

/**
 * =====================================================================================
 * GOOGLE-STYLE MOCK INTERVIEW: MERGE INTERVALS (LeetCode 56)
 * =====================================================================================
 *
 * This file is structured as a complete interview walkthrough, following the standard
 * 13-section deep-dive format:
 *   1. Problem Restatement
 *   2. Clarifying Questions
 *   3. Examples & Edge Cases
 *   4/5/6. All Possible Solutions (paradigm sweep)
 *   7. Comparison Table
 *   8. Recommended Interview Approach
 *   9. Deep Dive: Optimal Solution
 *  10. Dry Run / Trace
 *  11. Closing Summary
 *  12. Follow-Up Questions
 *  13. Common Candidate Mistakes
 *
 * Compile & run:
 *   javac MergeIntervals.java && java MergeIntervals
 * =====================================================================================
 */
class MergeIntervals {

    /*
     * =================================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * =================================================================================
     *
     * In plain language: I'm given a list of closed intervals [start, end], where
     * start <= end for each interval. Two intervals "overlap" if they share at least
     * one point in common -- this includes the boundary case where one interval's end
     * equals another's start (e.g. [1,4] and [4,5] overlap at the point 4). My job is
     * to collapse all overlapping intervals into the smallest possible set of
     * non-overlapping intervals that still covers exactly the same set of points as
     * the original input.
     *
     * Inputs:
     *   - int[][] intervals, where intervals[i] = {start_i, end_i}
     *
     * Outputs:
     *   - int[][] representing the merged, non-overlapping intervals
     *   - Order of the output is expected to be sorted by start time (this is the
     *     conventional LeetCode contract, and I'll confirm it as a clarifying question)
     *
     * Constraints (as given):
     *   - 1 <= intervals.length <= 10^3
     *   - intervals[i].length == 2
     *   - 0 <= start_i <= end_i <= 10^4
     *
     * Key implicit assumptions I should verify:
     *   - Intervals are closed on both ends (inclusive), so touching endpoints count
     *     as overlapping and must be merged.
     *   - The input is NOT guaranteed to be sorted.
     *   - Duplicate or fully-contained intervals are possible and must collapse
     *     correctly.
     */

    /*
     * =================================================================================
     * SECTION 2: CLARIFYING QUESTIONS
     * =================================================================================
     *
     * Q1: Are intervals closed (inclusive) on both endpoints, so that [1,4] and [4,5]
     *     count as overlapping and should merge into [1,5]?
     *     ASSUMED ANSWER: Yes, closed intervals; touching at a single point counts
     *     as an overlap and must be merged. This matches the given constraint
     *     start_i <= end_i (no invalid/empty intervals).
     *
     * Q2: Is the input array guaranteed to be sorted by start time, or in any
     *     particular order?
     *     ASSUMED ANSWER: No guarantee of any ordering. I must sort myself.
     *
     * Q3: Can intervals be duplicates, or can one interval be fully contained within
     *     another (e.g. [1,10] and [2,3])?
     *     ASSUMED ANSWER: Yes, both are possible and must be handled correctly by
     *     the merge logic (the containment case naturally falls out of taking a
     *     running max of the end time).
     *
     * Q4: What should the output order be -- sorted by start time, or does order not
     *     matter as long as the interval set is correct?
     *     ASSUMED ANSWER: Sorted ascending by start time, matching LeetCode's
     *     expected output format and general intuition for interval problems.
     *
     * Q5: Is the input array allowed to be empty, and if so what should I return?
     *     ASSUMED ANSWER: Per constraints, length >= 1, so I don't strictly need to
     *     handle empty input, but I will defensively return an empty array rather
     *     than throwing, since it costs nothing and is safer in production code.
     *
     * Q6: Are negative start/end values possible?
     *     ASSUMED ANSWER: No -- constraints guarantee 0 <= start_i <= end_i <= 10^4,
     *     so all values are small non-negative integers. This means I don't need
     *     `long` for overflow protection here (unlike some other interval/sum
     *     problems I've worked on), since 10^4 is nowhere near int overflow range.
     *
     * Q7: Should the original input array be mutated, or should I return a new
     *     array/list and leave the input untouched?
     *     ASSUMED ANSWER: Return a new structure; don't mutate the caller's array,
     *     since that's a common source of subtle bugs and surprises for callers.
     *
     * Q8: Is this a one-shot batch call, or do intervals arrive as a stream and I
     *     need to support incremental insertion (i.e. the "Insert Interval" variant)?
     *     ASSUMED ANSWER: One-shot batch call on a fixed input array. I'll mention
     *     the streaming/insert variant as a follow-up extension, not the core ask.
     */

    /*
     * =================================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * =================================================================================
     *
     * Example 1 (Normal case, unsorted input, multiple overlaps):
     *   Input:  [[1,3],[8,10],[2,6],[15,18]]
     *   Sorted: [[1,3],[2,6],[8,10],[15,18]]
     *   Trace:  [1,3] and [2,6] overlap (2 <= 3) -> merge to [1,6]
     *           [1,6] and [8,10] do NOT overlap (8 > 6) -> emit [1,6], start new [8,10]
     *           [8,10] and [15,18] do NOT overlap (15 > 10) -> emit [8,10], start new [15,18]
     *   Output: [[1,6],[8,10],[15,18]]
     *
     * Example 2 (Boundary / touching case -- the trap most candidates miss):
     *   Input:  [[1,4],[4,5]]
     *   Trace:  4 <= 4 is true -> intervals TOUCH at the single point x=4, so they
     *           must merge, even though neither interval is "inside" the other.
     *   Output: [[1,5]]
     *   This is the classic off-by-one trap: using strict `<` instead of `<=` in the
     *   overlap check would incorrectly leave these as two separate intervals.
     *
     * Example 3 (Edge case: single interval, and full containment):
     *   Input:  [[1,4]]                      -> Output: [[1,4]]  (nothing to merge)
     *   Input:  [[1,10],[2,3],[4,5]]          -> Output: [[1,10]] (both inner intervals
     *           are fully swallowed by the first; this is why I take max(end) rather
     *           than just overwriting the end -- otherwise [4,5] would incorrectly
     *           shrink the merged interval's end back down)
     */

    /*
     * =================================================================================
     * SECTION 4-6: ALL POSSIBLE SOLUTIONS (Paradigm Sweep)
     * =================================================================================
     *
     * Applicable paradigms:
     *   - Brute force (pairwise merge-and-restart)      -> Approach 1
     *   - Sorting-based sweep                            -> Approach 2 (OPTIMAL)
     *   - Hashing / coordinate marking                   -> Approach 3
     *
     * Paradigms explicitly ruled out (naming them signals structured thinking):
     *   - Two pointer / sliding window: doesn't apply -- there's no single array of
     *     values to slide a window over; intervals must first be ordered by start
     *     time before any linear scan makes sense, which is exactly what the sorting
     *     approach already does. Sliding window is for contiguous subarray/substring
     *     problems with a monotonic cost function, not interval merging.
     *   - Divide and conquer: technically possible (split, recursively merge halves,
     *     merge the boundary), but it only matches sort's O(n log n) at best with
     *     added implementation complexity -- no asymptotic win, so not worth it here.
     *   - Greedy: the sort-and-sweep approach IS a greedy algorithm (greedily extend
     *     the current merged interval whenever possible) -- I'll fold this into
     *     Approach 2 rather than listing it separately.
     *   - Dynamic programming: no overlapping subproblems or optimal substructure to
     *     exploit here; there's no "choice" to optimize over, just a merge condition.
     *   - Tree / graph traversal: no natural graph structure unless you explicitly
     *     build an interval overlap graph (see Approach 4 as a theoretical curiosity).
     *   - Heap / priority queue: relevant for the *streaming* variant (see follow-ups)
     *     where intervals arrive over time and you need the next-earliest-start
     *     efficiently, but adds unnecessary O(log n) overhead for a static batch
     *     input that we can just sort once.
     *   - Binary search: useful for the "Insert Interval" variant to locate insertion
     *     point in an already-sorted list, but the base merge problem doesn't have a
     *     pre-sorted structure to binary search into.
     *   - Monotonic stack/deque: actually a valid *implementation detail* for the
     *     sweep -- I use an explicit stack-like "last merged interval" pattern in
     *     Approach 2, which is conceptually a monotonic stack of non-overlapping
     *     intervals ordered by start time.
     *   - Trie / segment tree: massive overkill for this problem size (n <= 10^3,
     *     values <= 10^4); no prefix/range-query structure is being asked for.
     */

    // ---------------------------------------------------------------------------------
    // Approach 1: Brute Force (Repeated Pairwise Merge)
    // ---------------------------------------------------------------------------------
    /*
     * Core idea: Repeatedly scan all pairs of intervals. Whenever two overlap, merge
     * them into one and restart the scan. Keep going until a full pass finds no more
     * overlapping pairs. This is the "prove correctness with the dumbest possible
     * method" baseline -- I would state this first in an interview purely to lock in
     * a mental model of correctness, not to actually code it out fully unless asked.
     *
     * Paradigm: brute force / fixed-point iteration.
     *
     * Time Complexity: O(n^3) worst case -- O(n^2) pairs scanned per pass, and up to
     *   O(n) passes/merges can occur before reaching a fixed point.
     * Space Complexity: O(n) for the working list of intervals (ignoring output).
     *
     * Pros:
     *   - Trivial to reason about; obviously correct since it directly implements the
     *     "merge two intervals if they overlap" definition with no cleverness.
     * Cons:
     *   - Cubic time is unacceptable even for n = 10^3 (up to ~10^9 operations).
     *   - Ugly to implement cleanly (mutating a list while iterating it).
     *
     * When to use: Never in production or as a final interview answer -- only as a
     * sanity-check baseline stated verbally, or for tiny n in a non-performance-
     * critical script.
     */
    public static int[][] mergeBruteForce(int[][] intervals) {
        if (intervals.length == 0) return new int[0][];

        // Use a mutable list so we can merge-and-restart easily.
        List<int[]> working = new ArrayList<>();
        for (int[] interval : intervals) {
            working.add(new int[]{interval[0], interval[1]});
        }

        boolean mergedSomething = true;
        while (mergedSomething) {
            mergedSomething = false;

            outerLoop:
            for (int i = 0; i < working.size(); i++) {
                for (int j = i + 1; j < working.size(); j++) {
                    int[] intervalA = working.get(i);
                    int[] intervalB = working.get(j);

                    // Overlap condition for closed intervals: they share a point iff
                    // A.start <= B.end AND B.start <= A.end.
                    boolean overlaps = intervalA[0] <= intervalB[1] && intervalB[0] <= intervalA[1];

                    if (overlaps) {
                        int mergedStart = Math.min(intervalA[0], intervalB[0]);
                        int mergedEnd = Math.max(intervalA[1], intervalB[1]);

                        // Remove B first (higher index) to keep index i valid.
                        working.remove(j);
                        working.remove(i);
                        working.add(new int[]{mergedStart, mergedEnd});

                        mergedSomething = true;
                        break outerLoop; // restart the scan from a clean state
                    }
                }
            }
        }

        // Sort final result by start time for a deterministic, conventional output.
        working.sort((a, b) -> Integer.compare(a[0], b[0]));
        return working.toArray(new int[0][]);
    }

    // ---------------------------------------------------------------------------------
    // Approach 2: Sort by Start Time + Linear Sweep (OPTIMAL)
    // ---------------------------------------------------------------------------------
    /*
     * Core idea: Sort intervals by start time. Then walk through them once, keeping
     * a "current merged interval" (conceptually the top of a monotonic stack of
     * finalized, non-overlapping intervals). For each new interval, if its start is
     * <= the current merged interval's end, they overlap (or touch) -- extend the
     * current merged interval's end to the max of the two ends. Otherwise, the
     * current merged interval is finalized; push it to the result and start a new
     * "current" with this interval.
     *
     * Paradigm: sorting + greedy linear sweep (equivalently, a monotonic stack of
     * non-overlapping intervals ordered by start time).
     *
     * Time Complexity: O(n log n), dominated by the sort. The sweep itself is O(n).
     * Space Complexity: O(n) for the sorted copy and the output list. (O(log n) to
     *   O(n) additional for the sort's internal recursion/temp arrays depending on
     *   the sort algorithm used, which is implementation detail, not asymptotically
     *   significant here.)
     *
     * Pros:
     *   - Optimal time complexity for this problem (you provably cannot merge
     *     intervals without at least sorting them -- the problem is sorting-hard).
     *   - Simple, linear, easy to trace and explain -- ideal for whiteboard coding.
     *   - Naturally handles containment (max of ends) and touching (<=) correctly.
     * Cons:
     *   - None significant for this problem's constraints; if the input were already
     *     guaranteed sorted, you could drop to O(n) time by skipping the sort.
     *
     * When to use: This is the approach I would write in a real interview -- it's
     * optimal, clean, and demonstrates the key insight (sort first, then a single
     * greedy pass suffices) without unnecessary machinery.
     */
    public static int[][] mergeOptimal(int[][] intervals) {
        if (intervals.length == 0) return new int[0][];

        // Defensive copy + sort by start time; never mutate the caller's array.
        int[][] sortedIntervals = intervals.clone();
        Arrays.sort(sortedIntervals, Comparator.comparingInt(interval -> interval[0]));

        List<int[]> mergedResult = new ArrayList<>();
        int[] currentMerged = sortedIntervals[0].clone();

        for (int index = 1; index < sortedIntervals.length; index++) {
            int[] candidate = sortedIntervals[index];

            if (candidate[0] <= currentMerged[1]) {
                // Overlaps or touches the current merged interval -- extend it.
                // Must take max(), not just overwrite, to correctly handle full
                // containment (e.g. current=[1,10], candidate=[2,3]).
                currentMerged[1] = Math.max(currentMerged[1], candidate[1]);
            } else {
                // No overlap -- finalize the current merged interval and start fresh.
                mergedResult.add(currentMerged);
                currentMerged = candidate.clone();
            }
        }
        mergedResult.add(currentMerged); // don't forget the last one!

        return mergedResult.toArray(new int[0][]);
    }

    // ---------------------------------------------------------------------------------
    // Approach 3: Hashing / Coordinate Marking (Bucket Sweep)
    // ---------------------------------------------------------------------------------
    /*
     * Core idea: Since endpoints are bounded (0 <= value <= 10^4), we can allocate a
     * boolean array covering the entire value range, mark every integer point covered
     * by any interval, then scan left to right and extract maximal contiguous runs of
     * marked points as the merged intervals.
     *
     * Paradigm: hashing/bucketing by coordinate value (a form of counting-sort-like
     * marking, not true hashing, but grouped here since it uses direct-address
     * mapping the way a hash table would for small bounded keys).
     *
     * Time Complexity: O(n * R + R), where R is the coordinate range (up to 10^4).
     *   Marking each interval's points costs O(interval length), which sums to O(n*R)
     *   in the worst case (e.g. n intervals all spanning [0, 10^4]).
     * Space Complexity: O(R) for the marking array.
     *
     * Pros:
     *   - Conceptually simple; easy to get right for integer-bounded inputs.
     * Cons:
     *   - Only works because constraints bound values to <= 10^4 -- breaks
     *     immediately if endpoints were floating point or unbounded (e.g. 10^9),
     *     unlike Approach 2 which is agnostic to value range.
     *   - Strictly worse than Approach 2 in the general case: O(n*R) can be far
     *     worse than O(n log n) when R >> n (e.g. n=10, R=10^4).
     *
     * When to use: Only pedagogically, or if you specifically need per-point
     * coverage information as a byproduct. Not something I'd choose over Approach 2
     * in a real interview -- I include it here to show the paradigm was considered
     * and consciously ruled out, not overlooked.
     */
    public static int[][] mergeCoordinateMarking(int[][] intervals) {
        if (intervals.length == 0) return new int[0][];

        int maxEnd = 0;
        for (int[] interval : intervals) {
            maxEnd = Math.max(maxEnd, interval[1]);
        }

        boolean[] covered = new boolean[maxEnd + 2]; // +2 for a safe sentinel boundary
        for (int[] interval : intervals) {
            for (int point = interval[0]; point <= interval[1]; point++) {
                covered[point] = true;
            }
        }

        List<int[]> mergedResult = new ArrayList<>();
        int pointer = 0;
        while (pointer < covered.length) {
            if (covered[pointer]) {
                int runStart = pointer;
                while (pointer < covered.length && covered[pointer]) {
                    pointer++;
                }
                mergedResult.add(new int[]{runStart, pointer - 1});
            } else {
                pointer++;
            }
        }

        return mergedResult.toArray(new int[0][]);
    }

    /*
     * =================================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * =================================================================================
     *
     * | Approach                        | Time         | Space | Best For                | Limitations                              |
     * |----------------------------------|--------------|-------|--------------------------|-------------------------------------------|
     * | 1. Brute Force (pairwise merge)  | O(n^3)       | O(n)  | Verbal baseline only     | Far too slow; awkward mutation logic      |
     * | 2. Sort + Linear Sweep (OPTIMAL) | O(n log n)   | O(n)  | General-purpose, interview| None significant for given constraints   |
     * | 3. Coordinate Marking (bucketing)| O(n*R + R)   | O(R)  | Small bounded ranges only| Breaks for large/unbounded/float endpoints|
     *
     * (R = value range of endpoints, here bounded by 10^4 per the problem constraints)
     */

    /*
     * =================================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * =================================================================================
     *
     * I would present Approach 2 (Sort by Start Time + Linear Sweep) as my final
     * answer, following the standard "safe-then-optimal" interview flow:
     *
     *   1. State the brute-force pairwise-merge idea verbally in ~20 seconds to show
     *      I understand the problem's correctness condition, but explicitly say I
     *      won't code it because it's cubic and clearly not what's being tested.
     *   2. Immediately pivot to the key insight: "If I sort by start time first, I
     *      only ever need to compare each interval to the *most recently finalized*
     *      merged interval -- a single linear pass suffices." This is the moment
     *      that demonstrates the core algorithmic insight interviewers are listening
     *      for.
     *   3. Code Approach 2 directly as the final solution.
     *
     * Why this is the right call:
     *   - Optimality: O(n log n) is provably optimal here -- you cannot merge
     *     intervals without effectively sorting them, so this is the best possible
     *     asymptotic complexity for the general (unbounded value range) version.
     *   - Clarity: the greedy sweep is easy to state, easy to code without bugs, and
     *     easy to trace live on a whiteboard -- exactly what's wanted in a 30-45
     *     minute interview slot.
     *   - Coding speed: this is maybe 15-20 lines of core logic, leaving time for
     *     clarifying questions, testing, and follow-ups.
     *   - I would explicitly mention Approach 3 (coordinate marking) exists but is
     *     strictly worse in the general case and only viable because of this
     *     problem's specific small bound on endpoint values -- this shows awareness
     *     of the paradigm without wasting interview time coding an inferior solution.
     */

    /*
     * =================================================================================
     * SECTION 9: DEEP DIVE -- OPTIMAL SOLUTION (Production-Quality)
     * =================================================================================
     */

    /**
     * Merges all overlapping intervals in the input and returns the minimal set of
     * non-overlapping intervals that covers the same set of points, sorted by start
     * time.
     * <p>
     * Two closed intervals {@code [a, b]} and {@code [c, d]} are considered
     * overlapping (and are merged) if they share at least one point in common,
     * including the boundary case where {@code c == b} (i.e. they merely touch).
     * <p>
     * Algorithm: sort by start time, then perform a single greedy linear sweep,
     * extending a running "current merged interval" whenever the next interval's
     * start falls within (or at) its end, and finalizing it otherwise.
     *
     * @param intervals array of {@code [start, end]} pairs; not mutated by this
     *                  method. Each interval must satisfy {@code start <= end}.
     * @return a new array of merged, non-overlapping intervals sorted by start time.
     *         Returns an empty array if {@code intervals} is empty.
     * @throws IllegalArgumentException if any interval is malformed (wrong length,
     *                                  or start > end), since silently "fixing" bad
     *                                  input would hide caller bugs.
     */
    public static int[][] mergeIntervalsOptimal(int[][] intervals) {
        // Defensive validation -- production code shouldn't silently trust input,
        // even though the stated constraints guarantee well-formed intervals.
        Objects.requireNonNull(intervals, "intervals must not be null");
        for (int[] interval : intervals) {
            if (interval == null || interval.length != 2) {
                throw new IllegalArgumentException("Each interval must be a length-2 array.");
            }
            if (interval[0] > interval[1]) {
                throw new IllegalArgumentException(
                    "Invalid interval: start (" + interval[0] + ") > end (" + interval[1] + ")"
                );
            }
        }

        if (intervals.length == 0) {
            return new int[0][];
        }
        if (intervals.length == 1) {
            // Trivial fast path: nothing to merge, but still return a fresh copy.
            return new int[][]{intervals[0].clone()};
        }

        // Step 1: Defensive copy + sort by start time. We never mutate the caller's
        // array -- copying each row protects both the array-of-arrays structure and
        // the individual interval contents from later in-place mutation below.
        int[][] sortedIntervals = new int[intervals.length][];
        for (int index = 0; index < intervals.length; index++) {
            sortedIntervals[index] = intervals[index].clone();
        }
        Arrays.sort(sortedIntervals, Comparator.comparingInt(interval -> interval[0]));

        // Step 2: Greedy linear sweep. `currentMerged` plays the role of the top of a
        // monotonic stack of finalized, non-overlapping intervals -- we only ever
        // need to compare against it, never against older finalized intervals,
        // because the sort guarantees start times are non-decreasing.
        List<int[]> mergedResult = new ArrayList<>(sortedIntervals.length);
        int[] currentMerged = sortedIntervals[0];

        for (int index = 1; index < sortedIntervals.length; index++) {
            int[] candidateInterval = sortedIntervals[index];

            // Overlap/touch check: since sortedIntervals is sorted by start,
            // candidateInterval.start >= currentMerged.start is already guaranteed.
            // We only need to check whether it starts at or before currentMerged's
            // end to know if it overlaps or touches.
            if (candidateInterval[0] <= currentMerged[1]) {
                // Extend using max(), NOT direct assignment -- this correctly
                // handles full containment, e.g. currentMerged=[1,10],
                // candidateInterval=[2,3] must leave the end at 10, not shrink to 3.
                currentMerged[1] = Math.max(currentMerged[1], candidateInterval[1]);
            } else {
                // No overlap: currentMerged is final. Emit it and start fresh.
                mergedResult.add(currentMerged);
                currentMerged = candidateInterval;
            }
        }
        // The last currentMerged never gets a chance to be "finalized" inside the
        // loop, since finalization only happens when a *later* non-overlapping
        // interval is found -- so we must add it explicitly after the loop ends.
        mergedResult.add(currentMerged);

        return mergedResult.toArray(new int[0][]);
    }

    /*
     * =================================================================================
     * SECTION 10: DRY RUN / TRACE
     * =================================================================================
     *
     * Tracing mergeIntervalsOptimal on Example 1: [[1,3],[8,10],[2,6],[15,18]]
     *
     * Step 0 (after sort by start time):
     *   sortedIntervals = [[1,3], [2,6], [8,10], [15,18]]
     *   currentMerged   = [1,3]
     *   mergedResult    = []
     *
     * Step 1 (index=1, candidateInterval=[2,6]):
     *   Check: candidate.start (2) <= currentMerged.end (3)? YES -> overlap.
     *   currentMerged.end = max(3, 6) = 6
     *   currentMerged   = [1,6]
     *   mergedResult    = []
     *
     * Step 2 (index=2, candidateInterval=[8,10]):
     *   Check: candidate.start (8) <= currentMerged.end (6)? NO -> no overlap.
     *   Finalize currentMerged: mergedResult = [[1,6]]
     *   currentMerged   = [8,10]
     *
     * Step 3 (index=3, candidateInterval=[15,18]):
     *   Check: candidate.start (15) <= currentMerged.end (10)? NO -> no overlap.
     *   Finalize currentMerged: mergedResult = [[1,6], [8,10]]
     *   currentMerged   = [15,18]
     *
     * Step 4 (loop ends):
     *   Add final currentMerged: mergedResult = [[1,6], [8,10], [15,18]]
     *
     * Final Output: [[1,6], [8,10], [15,18]]   <-- matches expected result.
     */

    /*
     * =================================================================================
     * SECTION 11: CLOSING SUMMARY
     * =================================================================================
     *
     * - Brute force (O(n^3)) proves correctness conceptually but is never viable to
     *   actually code or ship.
     * - Sort + linear sweep (O(n log n) time, O(n) space) is optimal for the general
     *   problem and is what I'd write in an interview or in production.
     * - Coordinate marking (O(n*R)) is a valid alternative ONLY because this specific
     *   problem bounds endpoint values to <= 10^4; it does not generalize and is
     *   strictly worse when the value range is large relative to n.
     *
     * Known limitations / assumptions of the final solution:
     *   - Assumes closed intervals where touching endpoints (start == previous end)
     *     count as overlapping, per the problem's implicit convention.
     *   - Assumes int is sufficient for interval bounds (constraints guarantee
     *     values <= 10^4, far below Integer overflow range, so no `long` needed here
     *     -- unlike accumulation-heavy problems where I'd default to `long`).
     *   - Throws on malformed input (start > end, wrong array shape) rather than
     *     silently normalizing it, which is the right call for a public API surface.
     */

    /*
     * =================================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS
     * =================================================================================
     *
     * 1. "Insert Interval" variant: given an already-sorted, already-merged list of
     *    intervals, insert one new interval and merge as needed in O(n) time without
     *    re-sorting. (This is LeetCode 57 -- a natural extension.)
     *
     * 2. What if intervals arrive as a continuous stream and you need to query the
     *    current merged state at any time? (Suggests a balanced BST / TreeMap keyed
     *    by start time, or a heap-based approach for efficient incremental updates.)
     *
     * 3. What if n could be up to 10^7 and endpoint values up to 10^9 -- does your
     *    approach still hold? (Sort + sweep still works at O(n log n); coordinate
     *    marking would become infeasible due to memory.)
     *
     * 4. How would you parallelize this across multiple machines for a very large
     *    interval set (e.g. merge intervals within shards, then merge across shard
     *    boundaries)?
     *
     * 5. Can you find the minimum number of intervals to remove to make the rest
     *    non-overlapping, instead of merging them? (This is a related but distinct
     *    greedy interval-scheduling problem, LeetCode 435.)
     *
     * 6. What if instead of merging, you needed to find all intervals that overlap
     *    with a given query interval efficiently, across many repeated queries?
     *    (Suggests an interval tree or segment tree for O(log n + k) query time.)
     */

    /*
     * =================================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * =================================================================================
     *
     * 1. Using strict `<` instead of `<=` in the overlap check. Since intervals are
     *    closed, touching at a single shared point (e.g. [1,4] and [4,5]) still
     *    counts as an overlap and must be merged. This is the single most common bug
     *    on this problem.
     *
     * 2. Overwriting the merged interval's end directly instead of taking
     *    max(currentEnd, candidateEnd). This silently produces wrong answers when a
     *    later interval is fully contained within the current merged interval (e.g.
     *    current=[1,10], next=[2,3] -- overwriting would incorrectly shrink the end
     *    to 3).
     *
     * 3. Forgetting to sort the input first. Candidates sometimes jump straight to a
     *    single linear pass assuming input order reflects start-time order, which
     *    fails the moment the input isn't pre-sorted (and nothing in the problem
     *    statement guarantees that it is).
     *
     * 4. Forgetting to append the final "currentMerged" interval after the loop ends.
     *    Since finalization inside the loop only triggers when a *later*
     *    non-overlapping interval is found, the very last merged interval never gets
     *    flushed unless you explicitly add it once the loop completes -- an easy
     *    off-by-one to miss under interview pressure.
     */

    /*
     * =================================================================================
     * TEST HARNESS: cross-validates all three approaches against each other and
     * against hand-verified expected outputs.
     * =================================================================================
     */
    public static void main(String[] args) {
        List<TestCase> testCases = List.of(
            new TestCase(
                "Normal case: multiple overlaps, unsorted input",
                new int[][]{{1, 3}, {8, 10}, {2, 6}, {15, 18}},
                new int[][]{{1, 6}, {8, 10}, {15, 18}}
            ),
            new TestCase(
                "Boundary case: touching intervals must merge",
                new int[][]{{1, 4}, {4, 5}},
                new int[][]{{1, 5}}
            ),
            new TestCase(
                "Edge case: single interval, nothing to merge",
                new int[][]{{1, 4}},
                new int[][]{{1, 4}}
            ),
            new TestCase(
                "Edge case: full containment requires max(), not overwrite",
                new int[][]{{1, 10}, {2, 3}, {4, 5}},
                new int[][]{{1, 10}}
            ),
            new TestCase(
                "Non-overlapping intervals stay separate",
                new int[][]{{1, 4}, {5, 6}},
                new int[][]{{1, 4}, {5, 6}}
            ),
            new TestCase(
                "Duplicate intervals collapse to one",
                new int[][]{{1, 4}, {1, 4}},
                new int[][]{{1, 4}}
            ),
            new TestCase(
                "Single-point intervals",
                new int[][]{{0, 0}, {0, 0}},
                new int[][]{{0, 0}}
            ),
            new TestCase(
                "Everything merges into one giant interval",
                new int[][]{{5, 7}, {1, 3}, {2, 4}, {6, 8}, {9, 10}, {10, 11}},
                new int[][]{{1, 4}, {5, 8}, {9, 11}}
            )
        );

        int passCount = 0;
        for (TestCase testCase : testCases) {
            int[][] bruteResult = mergeBruteForce(testCase.input());
            int[][] optimalResult = mergeIntervalsOptimal(testCase.input());
            int[][] markingResult = mergeCoordinateMarking(testCase.input());

            boolean bruteOk = deepEqualsUnordered(bruteResult, testCase.expected());
            boolean optimalOk = deepEqualsUnordered(optimalResult, testCase.expected());
            boolean markingOk = deepEqualsUnordered(markingResult, testCase.expected());

            boolean allOk = bruteOk && optimalOk && markingOk;
            passCount += allOk ? 1 : 0;

            System.out.printf(
                "[%s] %s%n  expected=%s%n  bruteForce=%s (%s)%n  optimal=%s (%s)%n  coordinateMarking=%s (%s)%n%n",
                allOk ? "PASS" : "FAIL",
                testCase.description(),
                render(testCase.expected()),
                render(bruteResult), bruteOk ? "ok" : "MISMATCH",
                render(optimalResult), optimalOk ? "ok" : "MISMATCH",
                render(markingResult), markingOk ? "ok" : "MISMATCH"
            );
        }

        System.out.printf("Summary: %d/%d test cases passed across all three approaches.%n",
            passCount, testCases.size());

        // Randomized fuzz test cross-validating the optimal solution against brute
        // force, mirroring the standard validation workflow used throughout this
        // problem set (Python fuzzing wasn't needed here since this problem is
        // small enough to fuzz directly in Java, but the discipline is the same).
        runFuzzTest(2000);
    }

    /** Simple record representing a single named test case with expected output. */
    private record TestCase(String description, int[][] input, int[][] expected) {
    }

    /**
     * Compares two interval arrays for equality, treating them as sets of intervals
     * (order-independent) since some approaches may not naturally sort their output
     * even though mergeIntervalsOptimal always does.
     */
    private static boolean deepEqualsUnordered(int[][] actual, int[][] expected) {
        if (actual.length != expected.length) return false;
        int[][] sortedActual = actual.clone();
        int[][] sortedExpected = expected.clone();
        Arrays.sort(sortedActual, Comparator.comparingInt(interval -> interval[0]));
        Arrays.sort(sortedExpected, Comparator.comparingInt(interval -> interval[0]));
        for (int index = 0; index < sortedActual.length; index++) {
            if (!Arrays.equals(sortedActual[index], sortedExpected[index])) return false;
        }
        return true;
    }

    private static String render(int[][] intervals) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < intervals.length; index++) {
            builder.append(Arrays.toString(intervals[index]));
            if (index != intervals.length - 1) builder.append(", ");
        }
        return builder.append("]").toString();
    }

    /**
     * Randomized fuzz test: generates random interval sets and verifies that the
     * optimal solution agrees with the brute-force reference implementation.
     */
    private static void runFuzzTest(int trialCount) {
        Random random = new Random(42); // fixed seed for reproducibility
        int mismatchCount = 0;

        for (int trial = 0; trial < trialCount; trial++) {
            int intervalCount = 1 + random.nextInt(12);
            int[][] randomIntervals = new int[intervalCount][2];
            for (int index = 0; index < intervalCount; index++) {
                int start = random.nextInt(21);
                int end = start + random.nextInt(21 - start);
                randomIntervals[index] = new int[]{start, end};
            }

            int[][] bruteResult = mergeBruteForce(randomIntervals);
            int[][] optimalResult = mergeIntervalsOptimal(randomIntervals);

            if (!deepEqualsUnordered(bruteResult, optimalResult)) {
                mismatchCount++;
                System.out.println("FUZZ MISMATCH on input " + render(randomIntervals)
                    + " -> brute=" + render(bruteResult) + " optimal=" + render(optimalResult));
            }
        }

        System.out.printf("Fuzz test: %d/%d trials matched brute force reference.%n",
            trialCount - mismatchCount, trialCount);
    }
}
