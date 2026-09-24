import java.util.*;

/*
 * ============================================================================
 * MOCK GOOGLE ONSITE INTERVIEW TRANSCRIPT
 * Problem: Interval List Intersections   (LeetCode 986, Google-tagged, Medium)
 * ============================================================================
 *
 * This file is a full, self-contained mock interview walkthrough. Every
 * section below corresponds to a phase of a real onsite interview, in the
 * order a strong candidate would actually cover them.
 */

class IntervalListIntersections {

    /*
     * ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     *
     * In my own words:
     *   I'm given two separate lists of closed integer intervals,
     *   `intervalListA` and `intervalListB`. Within EACH list individually,
     *   the intervals are:
     *     - already sorted by start time, and
     *     - pairwise disjoint (no two intervals in the SAME list overlap or
     *       touch — endi < start(i+1) strictly).
     *
     *   I need to compute the intersection of the two lists — i.e. every
     *   maximal overlapping region between an interval from A and an
     *   interval from B — and return those overlap regions as a new list of
     *   closed intervals, in sorted order.
     *
     *   Two closed intervals [s1, e1] and [s2, e2] overlap iff
     *   s1 <= e2 AND s2 <= e1. Their overlap, if it exists, is exactly
     *   [max(s1, s2), min(e1, e2)].
     *
     * Key constraints & assumptions:
     *   - 0 <= A.length, B.length <= 1000
     *   - A.length + B.length >= 1 (at least one list is non-empty, but one
     *     list COULD be completely empty -> answer is empty list)
     *   - 0 <= start_i < end_i <= 1e9  (intervals are non-degenerate: a
     *     single point interval like [5,5] is NOT guaranteed to appear
     *     since start < end strictly — I'll confirm this in clarifying Qs)
     *   - end_i < start_(i+1) within each list -> strictly increasing,
     *     non-touching, so no need to merge within a single input list.
     *   - Output order does not need to be specified by me; I'll assume
     *     sorted-by-start, which falls out naturally from a linear merge.
     *
     * Input:  two int[][] arrays (each row is [start, end])
     * Output: an int[][] representing the intersection intervals, sorted
     *
     * This is fundamentally a MERGE problem over two sorted, non-overlapping
     * sequences — this framing should immediately suggest two-pointer merge,
     * the same paradigm as merging two sorted arrays / merge step of merge
     * sort, adapted to interval overlap logic instead of value comparison.
     */


    /*
     * ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
     * ========================================================================
     *
     * Q1: Can either input list be empty?
     *     A: Yes. Constraint says lengths can be 0, but their SUM is >= 1.
     *        If either is empty, the intersection is trivially empty.
     *
     * Q2: Are intervals closed on both ends, meaning a single shared boundary
     *     point (e.g. A=[1,5], B=[5,8]) counts as an overlap of size zero,
     *     i.e. [5,5]?
     *     A: Yes — problem explicitly says "closed interval", and the
     *        overlap formula [max(s1,s2), min(e1,e2)] is valid whenever
     *        max(s1,s2) <= min(e1,e2), including equality (a degenerate
     *        single-point intersection).
     *
     * Q3: Can start == end for a single interval (a zero-length / point
     *     interval)?
     *     A: No — constraint states start_i < end_i strictly, so every
     *        input interval has positive length. (The single-point RESULT
     *        of an intersection can still occur, per Q2.)
     *
     * Q4: Do I need to validate malformed input (e.g. null arrays, rows not
     *     of length 2, negative numbers)?
     *     A: No — assume well-formed input per constraints; I will still
     *        guard against null/empty defensively but won't over-engineer
     *        validation logic mid-interview.
     *
     * Q5: Should the result intervals be merged/coalesced if adjacent
     *     results happen to touch (e.g. two consecutive outputs [1,3],[3,5])?
     *     A: No merging needed — because each input list is internally
     *        disjoint and non-touching, it's provable that the intersection
     *        intervals produced are also automatically disjoint (though they
     *        CAN touch at a single point in edge cases like Q2 — that's
     *        still valid, distinct output per LeetCode's accepted answer key).
     *
     * Q6: What's the expected input size, and does performance matter beyond
     *     asymptotic complexity (i.e. do we need cache-friendliness, are
     *     these numbers streamed)?
     *     A: n, m <= 1000, so even O(n*m) = 1e6 brute force technically
     *        passes, but I should still aim for the asymptotically optimal
     *        O(n+m) approach to demonstrate proper technique.
     *
     * Q7: Is this a single-threaded, synchronous call, or do I need to worry
     *     about concurrent mutation of the input lists?
     *     A: Single-threaded, synchronous, no concurrency concerns.
     *
     * Q8: What should be returned if there is no overlap at all?
     *     A: An empty int[][] (length 0), not null.
     */


    /*
     * ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (normal case):
     *   A = [[0,2],[5,10],[13,23],[24,25]]
     *   B = [[1,5],[8,12],[15,24],[25,26]]
     *   Walkthrough:
     *     [0,2] vs [1,5]   -> overlap [1,2]
     *     [0,2] vs [1,5] ends, advance A -> [5,10] vs [1,5] -> overlap [5,5]
     *     [5,10] vs [8,12] -> overlap [8,10]
     *     [5,10] ends (10<12), advance A -> [13,23] vs [8,12] -> no overlap,
     *        B ends first (12<23), advance B -> [13,23] vs [15,24] -> [15,23]
     *     [13,23] ends (23<24), advance A -> [24,25] vs [15,24] -> [24,24]
     *     [15,24] ends, advance B -> [24,25] vs [25,26] -> [25,25]
     *   Result: [[1,2],[5,5],[8,10],[15,23],[24,24],[25,25]]
     *   (This matches the canonical LeetCode 986 example.)
     *
     * Example 2 (edge case — one list empty):
     *   A = []
     *   B = [[1,3],[5,9]]
     *   Result: [] — nothing to intersect against.
     *
     * Example 3 (boundary / touching case):
     *   A = [[1,3],[5,7]]
     *   B = [[3,5]]
     *   Walkthrough:
     *     [1,3] vs [3,5] -> touch at boundary -> overlap [3,3] (valid,
     *        single-point closed interval)
     *     [1,3] ends (3 == 3, tie -> advance A since end_A <= end_B),
     *        advance A -> [5,7] vs [3,5] -> touch at [5,5]
     *   Result: [[3,3],[5,5]]
     *   This is the tie-breaking case: when end_A == end_B, BOTH pointers
     *   have "finished" that interval simultaneously, so it is safe (and
     *   necessary, to avoid re-comparing a stale pointer) to advance either
     *   one — conventionally I advance whichever has the smaller end, and
     *   on a tie I advance A (arbitrary but must be consistent).
     */


    /*
     * ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES
     * ========================================================================
     *
     * Paradigms considered but ruled out (one-liners, per instructions):
     *   - Sorting-based: N/A — both lists are already sorted and disjoint;
     *     no sorting step is needed or would change the algorithm.
     *   - Hashing-based: N/A — intervals are ranges over up to 1e9 values;
     *     hashing individual points is infeasible and offers no benefit
     *     over direct comparison.
     *   - Divide & conquer: N/A — no natural way to split that beats a
     *     linear merge; would only add overhead (like merge sort applied
     *     to already-sorted, already-linear data).
     *   - Dynamic Programming: N/A — no overlapping subproblems / optimal
     *     substructure to exploit; this is a direct scan/merge, not an
     *     optimization-over-choices problem.
     *   - Tree / graph traversal: N/A — no hierarchical or graph structure
     *     in the input.
     *   - Monotonic stack/deque: N/A — we're not tracking a running
     *     max/min over a sliding window of unknown boundary; direct
     *     pairwise comparison suffices.
     *   - Trie: N/A — not a string/prefix problem.
     *   - Segment tree: technically COULD answer "does point/interval X
     *     overlap anything in list B" as a range-query structure, but it's
     *     wild overkill here since we need every overlap, not sparse
     *     point queries, and construction cost dominates for n,m <= 1000.
     *
     * Approaches actually worth presenting, from naive to optimal:
     *   Approach 1: Brute Force (all pairs)
     *   Approach 2: Binary Search per interval
     *   Approach 3: Two-Pointer Merge (OPTIMAL)
     */

    /*
     * ------------------------------------------------------------------------
     * Approach 1: Brute Force (All Pairs)
     * ------------------------------------------------------------------------
     * Core idea:
     *   For every interval in A, compare it against every interval in B.
     *   If they overlap, compute and record [max(starts), min(ends)].
     *   This completely ignores the fact that both lists are sorted.
     *
     * Data structure / paradigm: nested loops, no auxiliary structure.
     *
     * Time complexity: O(n * m) — n = |A|, m = |B|; every pair is checked.
     * Space complexity: O(n * m) worst case for output storage (same as any
     *   correct approach's output size), O(1) extra working space beyond
     *   a dynamic result list.
     *
     * Pros:
     *   - Trivial to write correctly under pressure; low bug risk.
     *   - Doesn't rely on noticing/using the sorted-disjoint property.
     * Cons:
     *   - Wastes the given structure of the input entirely.
     *   - Quadratic — won't scale, and signals to the interviewer that you
     *     didn't use the problem's stated invariants.
     * When to use:
     *   - Only as a verbal warm-up baseline / correctness oracle for
     *     stress-testing the optimal solution. Never as a final answer in
     *     a Google onsite once n, m can be up to 1000.
     */
    public static int[][] intersectBruteForce(int[][] intervalListA, int[][] intervalListB) {
        List<int[]> results = new ArrayList<>();
        for (int[] intervalA : intervalListA) {
            for (int[] intervalB : intervalListB) {
                int overlapStart = Math.max(intervalA[0], intervalB[0]);
                int overlapEnd = Math.min(intervalA[1], intervalB[1]);
                if (overlapStart <= overlapEnd) {
                    results.add(new int[]{overlapStart, overlapEnd});
                }
            }
        }
        // Brute force does not naturally emit results in sorted order across
        // both lists (it's sorted only by A first, then B), so we must sort.
        results.sort((intervalX, intervalY) -> Integer.compare(intervalX[0], intervalY[0]));
        return results.toArray(new int[0][]);
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 2: Binary Search per Interval
     * ------------------------------------------------------------------------
     * Core idea:
     *   For each interval in the shorter list A, binary-search into B (which
     *   is sorted by start) to find the first interval in B whose end is
     *   >= A's start (i.e. the first candidate that could possibly overlap).
     *   Then walk forward from that point through B, checking/collecting
     *   overlaps, until B's start exceeds A's end (no more overlap possible).
     *
     * Data structure / paradigm: binary search + local linear scan.
     *
     * Time complexity: O(n log m + K) where K is total overlap-candidates
     *   actually scanned across all binary search anchor points. In the
     *   worst case (e.g. one huge interval in A overlapping every interval
     *   in B), this degrades toward O(n log m + m) per big interval, so
     *   worst case is O(n * m) again if A has intervals that each span all
     *   of B. On average/typical inputs it beats brute force by pruning the
     *   search start point.
     * Space complexity: O(1) extra (excluding output).
     *
     * Pros:
     *   - Demonstrates binary search skill; genuinely faster than brute
     *     force when A's intervals are narrow relative to B's spread.
     *   - Good "middle" answer to discuss trade-offs if two-pointer isn't
     *     immediately obvious.
     * Cons:
     *   - More complex to implement correctly (index math, off-by-ones in
     *     locating the search anchor) than the two-pointer approach, for
     *     WORSE worst-case complexity — strictly dominated by Approach 3.
     *   - Doesn't exploit the fact that as A advances, the "starting point"
     *     into B only moves forward — binary search restarts blind each
     *     time instead of remembering where it left off.
     * When to use:
     *   - Rarely, in practice, for THIS problem. Worth mentioning to show
     *     breadth, but I'd flag immediately that two-pointer dominates it
     *     both in simplicity and worst-case guarantees, since both lists
     *     are sorted and we scan them in lockstep anyway.
     */
    public static int[][] intersectBinarySearch(int[][] intervalListA, int[][] intervalListB) {
        List<int[]> results = new ArrayList<>();
        if (intervalListA.length == 0 || intervalListB.length == 0) {
            return new int[0][];
        }
        for (int[] intervalA : intervalListA) {
            // Binary search for the first interval in B whose END is >= A's start.
            // Anything before this index in B ends too early to ever overlap A.
            int lowIndex = 0;
            int highIndex = intervalListB.length - 1;
            int firstCandidateIndex = intervalListB.length; // default: none found
            while (lowIndex <= highIndex) {
                int midIndex = lowIndex + (highIndex - lowIndex) / 2;
                if (intervalListB[midIndex][1] >= intervalA[0]) {
                    firstCandidateIndex = midIndex;
                    highIndex = midIndex - 1; // look further left for an earlier valid start
                } else {
                    lowIndex = midIndex + 1;
                }
            }
            // Walk forward from the anchor while B's start hasn't exceeded A's end.
            for (int scanIndex = firstCandidateIndex;
                 scanIndex < intervalListB.length && intervalListB[scanIndex][0] <= intervalA[1];
                 scanIndex++) {
                int overlapStart = Math.max(intervalA[0], intervalListB[scanIndex][0]);
                int overlapEnd = Math.min(intervalA[1], intervalListB[scanIndex][1]);
                if (overlapStart <= overlapEnd) {
                    results.add(new int[]{overlapStart, overlapEnd});
                }
            }
        }
        results.sort((intervalX, intervalY) -> Integer.compare(intervalX[0], intervalY[0]));
        return results.toArray(new int[0][]);
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 3: Two-Pointer Merge (OPTIMAL — the one I'd present)
     * ------------------------------------------------------------------------
     * Core idea:
     *   Walk both lists simultaneously with pointers pointerA and pointerB,
     *   starting at 0. At each step, compute the overlap of the current pair
     *   [A[pointerA], B[pointerB]]. If it's a valid overlap, record it.
     *   Then advance WHICHEVER interval ends first — that interval can no
     *   longer overlap anything further in the other list (since the other
     *   list is sorted and increasing), so it's "used up" and we move on.
     *   This is exactly the merge step of merge sort, but instead of
     *   picking the smaller element, we're retiring the interval with the
     *   smaller end time.
     *
     * Data structure / paradigm: two-pointer / greedy linear merge.
     *
     * Time complexity: O(n + m) — each pointer advances at most once per
     *   comparison and only moves forward, monotonically, until one list is
     *   exhausted. Every interval in A and B is visited a constant number
     *   of times.
     * Space complexity: O(1) extra working space (excluding the output
     *   list, which is O(n + m) in the worst case — e.g. every interval in
     *   A partially overlaps a distinct interval in B).
     *
     * Pros:
     *   - Asymptotically optimal — you must at minimum look at every input
     *     interval once, so O(n+m) is the theoretical floor.
     *   - Simple, well-known pattern (merge step) — low bug surface, easy
     *     to explain and verify on a whiteboard.
     *   - No extra data structures beyond two integer indices.
     * Cons:
     *   - Requires correctly identifying which pointer to advance (subtle:
     *     must compare END times, not start times, to decide advancement).
     * When to use:
     *   - Always, for this exact problem shape: two sorted, internally
     *     disjoint interval lists. This is the canonical, expected solution
     *     in a Google onsite.
     */
    public static int[][] intersectTwoPointer(int[][] intervalListA, int[][] intervalListB) {
        List<int[]> results = new ArrayList<>();
        int pointerA = 0;
        int pointerB = 0;

        while (pointerA < intervalListA.length && pointerB < intervalListB.length) {
            int overlapStart = Math.max(intervalListA[pointerA][0], intervalListB[pointerB][0]);
            int overlapEnd = Math.min(intervalListA[pointerA][1], intervalListB[pointerB][1]);

            // Valid closed-interval overlap, including single-point touches.
            if (overlapStart <= overlapEnd) {
                results.add(new int[]{overlapStart, overlapEnd});
            }

            // Retire whichever interval ends first — it cannot overlap any
            // future interval in the other list, since the other list's
            // starts only increase from here on.
            if (intervalListA[pointerA][1] < intervalListB[pointerB][1]) {
                pointerA++;
            } else {
                // Covers both "B ends first" and the tie case (ends equal),
                // where advancing either pointer is safe; we choose B here
                // for a consistent, deterministic rule.
                pointerB++;
            }
        }
        return results.toArray(new int[0][]);
    }


    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * | Approach                | Time         | Space       | Best For                          | Limitations                                  |
     * |--------------------------|--------------|-------------|------------------------------------|-----------------------------------------------|
     * | 1. Brute Force            | O(n*m)       | O(n*m)      | Correctness oracle / stress testing| Ignores sorted+disjoint structure; too slow   |
     * | 2. Binary Search per item | O(n log m + K)| O(1) extra | Showing search breadth in interview| Worse worst-case than two-pointer; more code  |
     * | 3. Two-Pointer Merge      | O(n + m)     | O(1) extra  | THE canonical solution here        | None significant for this problem's shape     |
     *
     * (n = |intervalListA|, m = |intervalListB|, K = candidates scanned)
     */


    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would present Approach 3 (Two-Pointer Merge).
     *
     * Why:
     *   - It is asymptotically optimal: O(n+m) is the theoretical lower
     *     bound since every interval must be examined at least once.
     *   - It's fast to code correctly under interview time pressure — a
     *     handful of lines, no nested loops, no index math for binary
     *     search bounds.
     *   - It directly exploits the two invariants the problem statement
     *     hands you for free (both lists sorted, both lists internally
     *     disjoint) — recognizing and using given invariants is exactly
     *     what interviewers are evaluating.
     *   - It matches a pattern (merge step of merge sort) the interviewer
     *     will instantly recognize, which makes the explanation crisp and
     *     the code easy to verify by eye.
     *
     * My interview delivery strategy:
     *   1. State the O(n*m) brute force verbally as a baseline (don't code
     *      it fully unless asked) to show I understand the naive solution.
     *   2. Note the sorted/disjoint invariant and pivot straight to the
     *      two-pointer idea — mention binary search as an alternative I
     *      considered and explain briefly why it's dominated.
     *   3. Code the two-pointer solution cleanly.
     *   4. Proactively walk through the pointer-advancement edge case
     *      (the tie on equal end times) to preempt the interviewer asking
     *      about it.
     */


    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (PRODUCTION QUALITY)
     * ========================================================================
     * Polished, defensively-written version of the two-pointer merge,
     * with full inline reasoning for every decision.
     */
    public static int[][] intervalIntersection(int[][] intervalListA, int[][] intervalListB) {
        // Defensive null handling: treat null as "empty list" rather than
        // throwing, since the problem guarantees well-formed non-null input
        // per our clarifying questions, but this costs nothing and avoids
        // a surprise NPE if called from elsewhere.
        if (intervalListA == null) intervalListA = new int[0][];
        if (intervalListB == null) intervalListB = new int[0][];

        // Upper bound on output size is |A| + |B| (each interval from either
        // list can contribute to at most... actually more precisely, the
        // number of intersection intervals is bounded by n + m - 1 in the
        // worst case, but n + m is a safe, simple upper bound). Using an
        // ArrayList avoids needing to precompute the exact count.
        List<int[]> intersections = new ArrayList<>(Math.min(intervalListA.length, intervalListB.length) + 1);

        int pointerA = 0; // current index into intervalListA
        int pointerB = 0; // current index into intervalListB

        while (pointerA < intervalListA.length && pointerB < intervalListB.length) {
            int[] currentA = intervalListA[pointerA];
            int[] currentB = intervalListB[pointerB];

            // The overlap of two closed intervals is [max(starts), min(ends)],
            // valid (non-empty, possibly a single point) iff start <= end.
            int overlapStart = Math.max(currentA[0], currentB[0]);
            int overlapEnd = Math.min(currentA[1], currentB[1]);

            if (overlapStart <= overlapEnd) {
                intersections.add(new int[]{overlapStart, overlapEnd});
            }

            // Decide which pointer to advance: always retire the interval
            // with the smaller end time, because — since the OTHER list's
            // remaining intervals only have larger and larger starts from
            // here on (sorted, disjoint) — this interval cannot possibly
            // overlap anything else in the other list going forward.
            //
            // On a tie (currentA[1] == currentB[1]), both intervals are
            // simultaneously "used up" against each other; advancing either
            // one alone is correct (the other will simply produce no further
            // useful overlap against the next element and get skipped
            // naturally), but we must advance AT LEAST one to guarantee
            // termination. We deterministically advance pointerB on ties.
            if (currentA[1] < currentB[1]) {
                pointerA++;
            } else {
                pointerB++;
            }
        }

        return intersections.toArray(new int[0][]);
    }


    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     * Tracing intervalIntersection() on Example 1:
     *   A = [[0,2],[5,10],[13,23],[24,25]]
     *   B = [[1,5],[8,12],[15,24],[25,26]]
     *
     * Step | pointerA | pointerB | currentA | currentB | overlapStart | overlapEnd | added?     | advance
     * -----|----------|----------|----------|----------|--------------|------------|------------|--------
     *   1  |    0     |    0     | [0,2]    | [1,5]    |      1       |     2      | [1,2]      | A (2<5)
     *   2  |    1     |    0     | [5,10]   | [1,5]    |      5       |     5      | [5,5]      | B (5==5,tie->B)
     *   3  |    1     |    1     | [5,10]   | [8,12]   |      8       |     10     | [8,10]     | A (10<12)
     *   4  |    2     |    1     | [13,23]  | [8,12]   |      13      |     12     | (none, 13>12)| B (12<23)
     *   5  |    2     |    2     | [13,23]  | [15,24]  |      15      |     23     | [15,23]    | A (23<24)
     *   6  |    3     |    2     | [24,25]  | [15,24]  |      24      |     24     | [24,24]    | B (24==24,tie->B)
     *   7  |    3     |    3     | [24,25]  | [25,26]  |      25      |     25     | [25,25]    | A (25<26)
     *   8  |    4     |    3     | pointerA == intervalListA.length -> loop ends
     *
     * Final intersections list:
     *   [[1,2], [5,5], [8,10], [15,23], [24,24], [25,25]]
     *
     * This matches Example 1's expected output exactly.
     */


    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * - Brute force (O(n*m)) is correct but ignores the problem's given
     *   structure; useful only as a stress-test oracle.
     * - Binary search per interval (O(n log m + K)) is a reasonable middle
     *   ground showing search technique, but is strictly dominated by the
     *   two-pointer approach for this problem's shape, both in simplicity
     *   and worst-case guarantees.
     * - Two-pointer merge (O(n + m)) is the optimal and expected solution:
     *   it is asymptotically tight, uses O(1) extra space, and directly
     *   exploits the sorted + disjoint invariants given in the problem.
     *
     * Assumptions/limitations of the final solution:
     *   - Assumes well-formed input per constraints (start < end within
     *     each interval, each list internally sorted and disjoint). If
     *     these invariants are violated, the two-pointer logic is no
     *     longer guaranteed correct.
     *   - Assumes 32-bit int range is sufficient (endpoints <= 1e9 fits
     *     comfortably in int).
     *   - Output order is sorted by start time ascending, which is implied
     *     but not explicitly mandated by the problem — worth a quick
     *     confirmation with the interviewer if it matters for grading.
     */


    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     *
     * 1. "What if the input lists were NOT sorted?" 
     *    -> Would need to sort first: O(n log n + m log m), then run the
     *       same two-pointer merge; total O(n log n + m log m).
     *
     * 2. "What if intervals within a single list COULD overlap each other?"
     *    -> Would first need to merge overlapping intervals within each
     *       list individually (classic "Merge Intervals" problem) before
     *       running this two-pointer intersection.
     *
     * 3. "How would you extend this to intersect K lists instead of 2?"
     *    -> Could generalize with a min-heap of pointers (one per list),
     *       or apply two-pointer merge pairwise/iteratively across lists,
     *       accumulating the running intersection. Complexity becomes
     *       O(total_intervals * log K) with a heap, or O(total_intervals * K)
     *       with naive pairwise reduction.
     *
     * 4. "What if the lists were extremely large and didn't fit in memory
     *     (streamed from disk/network)?"
     *    -> Since the algorithm only ever needs the "current" interval from
     *       each stream and only moves forward, it's naturally streamable/
     *       externalizable — read one interval at a time from each source
     *       with an iterator/cursor abstraction instead of loading full
     *       arrays into memory.
     *
     * 5. "Can you make this thread-safe / parallelize it?"
     *    -> The sequential dependency between pointer advances makes true
     *       parallelism awkward, but you could partition by value range
     *       (e.g. split both lists at a pivot value found via binary
     *       search) and intersect each partition independently in
     *       parallel, then concatenate results.
     *
     * 6. "What if we also wanted the intervals that DON'T overlap (i.e. the
     *     symmetric difference or the 'gap' regions)?"
     *    -> Track the pointer that advances at each step; the retired
     *       interval's non-overlapping portion(s) relative to the current
     *       partner can be derived from the same pass with extra bookkeeping.
     */


    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Advancing the WRONG pointer: comparing start times instead of end
     *    times to decide which pointer to move. It must be the interval
     *    with the smaller END that gets retired — advancing based on start
     *    time breaks correctness immediately.
     *
     * 2. Forgetting the tie case (currentA[1] == currentB[1]): candidates
     *    often write `if (currentA[1] < currentB[1]) pointerA++; else if
     *    (currentA[1] > currentB[1]) pointerB++;` and forget the equals
     *    case entirely, causing an infinite loop since neither pointer
     *    advances.
     *
     * 3. Off-by-one on overlap validity: using strict `<` instead of `<=`
     *    when checking `overlapStart <= overlapEnd`. Since intervals are
     *    CLOSED, a single shared boundary point IS a valid (degenerate)
     *    overlap and must be included — using strict `<` silently drops
     *    legitimate single-point intersections.
     *
     * 4. Assuming the output must be merged/coalesced further: candidates
     *    sometimes add unnecessary post-processing to merge adjacent
     *    output intervals, not realizing that the disjoint+sorted
     *    invariant on the INPUTS guarantees the outputs are already
     *    correctly separated (touching at a point is a valid distinct
     *    result, not something to merge away).
     */


    /*
     * ========================================================================
     * VERIFICATION: main() — runs named assertions + randomized stress test
     * cross-validating brute force, binary search, and two-pointer approaches
     * against each other.
     * ========================================================================
     */
    public static void main(String[] args) {
        runNamedAssertions();
        runRandomizedStressTest(3000, new Random(42));
        System.out.println("All tests passed.");
    }

    private static void runNamedAssertions() {
        // Assertion 1: canonical example from the problem statement.
        int[][] a1 = {{0,2},{5,10},{13,23},{24,25}};
        int[][] b1 = {{1,5},{8,12},{15,24},{25,26}};
        int[][] expected1 = {{1,2},{5,5},{8,10},{15,23},{24,24},{25,25}};
        assertIntervalsEqual("Example 1 - canonical case", expected1, intervalIntersection(a1, b1));
        assertIntervalsEqual("Example 1 - brute force cross-check", expected1, intersectBruteForce(a1, b1));
        assertIntervalsEqual("Example 1 - binary search cross-check", expected1, intersectBinarySearch(a1, b1));

        // Assertion 2: one list empty.
        int[][] a2 = {};
        int[][] b2 = {{1,3},{5,9}};
        assertIntervalsEqual("Example 2 - A empty", new int[0][], intervalIntersection(a2, b2));

        // Assertion 3: boundary touching / tie-breaking case.
        int[][] a3 = {{1,3},{5,7}};
        int[][] b3 = {{3,5}};
        int[][] expected3 = {{3,3},{5,5}};
        assertIntervalsEqual("Example 3 - boundary touching", expected3, intervalIntersection(a3, b3));

        // Assertion 4: both lists empty.
        assertIntervalsEqual("Both empty", new int[0][], intervalIntersection(new int[0][], new int[0][]));

        // Assertion 5: no overlap at all (disjoint ranges).
        int[][] a5 = {{0,1}};
        int[][] b5 = {{5,6}};
        assertIntervalsEqual("No overlap", new int[0][], intervalIntersection(a5, b5));

        System.out.println("Named assertions: PASSED");
    }

    private static void assertIntervalsEqual(String testName, int[][] expected, int[][] actual) {
        if (expected.length != actual.length) {
            throw new AssertionError(testName + " FAILED: length mismatch, expected "
                    + expected.length + " but got " + actual.length
                    + " (actual=" + Arrays.deepToString(actual) + ")");
        }
        for (int index = 0; index < expected.length; index++) {
            if (!Arrays.equals(expected[index], actual[index])) {
                throw new AssertionError(testName + " FAILED at index " + index
                        + ": expected " + Arrays.toString(expected[index])
                        + " but got " + Arrays.toString(actual[index]));
            }
        }
    }

    // Generates random sorted, disjoint interval lists and cross-validates
    // all three approaches against each other for `trialCount` trials.
    private static void runRandomizedStressTest(int trialCount, Random random) {
        for (int trial = 0; trial < trialCount; trial++) {
            int[][] listA = generateRandomDisjointIntervals(random, random.nextInt(15));
            int[][] listB = generateRandomDisjointIntervals(random, random.nextInt(15));

            int[][] bruteResult = intersectBruteForce(listA, listB);
            int[][] binarySearchResult = intersectBinarySearch(listA, listB);
            int[][] twoPointerResult = intervalIntersection(listA, listB);

            assertIntervalsEqual("Stress trial " + trial + " (brute vs two-pointer)", bruteResult, twoPointerResult);
            assertIntervalsEqual("Stress trial " + trial + " (binarySearch vs two-pointer)", binarySearchResult, twoPointerResult);
        }
        System.out.println("Randomized stress test (" + trialCount + " trials): PASSED");
    }

    // Builds a random list of `count` intervals that are sorted ascending
    // and strictly disjoint (end_i < start_(i+1)), matching the problem's
    // stated invariants for a single input list.
    private static int[][] generateRandomDisjointIntervals(Random random, int count) {
        int[][] intervals = new int[count][2];
        int cursor = random.nextInt(5); // small random starting offset
        for (int index = 0; index < count; index++) {
            int start = cursor;
            int length = 1 + random.nextInt(5); // ensures start < end strictly
            int end = start + length;
            intervals[index][0] = start;
            intervals[index][1] = end;
            // Next interval's start must be strictly greater than this end,
            // with a random gap to also exercise non-touching cases.
            cursor = end + 1 + random.nextInt(4);
        }
        return intervals;
    }
}
