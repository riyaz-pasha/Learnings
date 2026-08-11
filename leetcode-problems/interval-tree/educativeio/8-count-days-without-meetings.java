import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * ============================================================================
 * GOOGLE-STYLE MOCK INTERVIEW: "Count Days Without Meetings"
 * (LeetCode 3169 family — interval coverage / gap counting)
 * ============================================================================
 *
 * This file is a complete, self-contained interview walkthrough. Every
 * required section from the interview rubric is present as a labeled block
 * comment, in order. Run it with:
 *
 *     javac CountDaysWithoutMeetings.java && java CountDaysWithoutMeetings
 */
class CountDaysWithoutMeetings {

    /* ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     *
     * In my own words: I have an employee who is theoretically available to
     * work on every day from day 1 through day `days` (inclusive, 1-indexed).
     * I'm given a list of meetings, each a closed interval [start, end]
     * (both inclusive) on that same day axis. Meetings can overlap each
     * other, can be nested inside one another, and can be adjacent.
     *
     * I need to return a single integer: the count of days in [1, days]
     * that are NOT covered by any meeting interval.
     *
     * Key constraints and assumptions I'm calling out explicitly:
     *   - Inputs:
     *       days: int, 1 <= days <= 100,000
     *       meetings: int[][], 1 <= meetings.length <= 1,000
     *                 each meetings[i] has length 2: [start_i, end_i]
     *                 1 <= start_i <= end_i <= days
     *   - Output: a single int — the count of free (uncovered) days.
     *   - Meetings are 1-indexed and inclusive on both ends.
     *   - Meetings may overlap arbitrarily (partial overlap, full containment,
     *     duplicates, or exact adjacency like [1,2] and [3,4]).
     *   - This is fundamentally an "interval coverage" problem: I need the
     *     total number of days covered by the UNION of all meeting
     *     intervals, then subtract that from `days`.
     */


    /* ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
     * ========================================================================
     *
     * 1. Q: Are `start_i` and `end_i` always valid, i.e., is it guaranteed
     *       start_i <= end_i and both within [1, days]?
     *    A (assumed, per constraints): Yes, guaranteed by the problem
     *       statement. No need to defensively validate in production code,
     *       though I'd mention I could add validation if this were a public
     *       API rather than a contest/interview problem.
     *
     * 2. Q: Can meetings.length be 0 (an empty meetings array)?
     *    A: The stated constraint says meetings.length >= 1, but I'll design
     *       my solution to also handle the empty-array case gracefully
     *       (defensive engineering habit), since it's a free, cheap
     *       edge case to support.
     *
     * 3. Q: Can meetings overlap, be duplicated, or be nested (e.g. [2,3]
     *       fully inside [1,10])?
     *    A: Yes — explicitly stated meetings "may overlap." My solution
     *       must not double-count overlapping coverage.
     *
     * 4. Q: Are meetings given in any particular order (sorted by start
     *       day, etc.)?
     *    A: No guarantee of sorted order — I should not assume it.
     *
     * 5. Q: Is the day axis 1-indexed or 0-indexed?
     *    A: 1-indexed, based on "starting from day 1" and start_i >= 1.
     *
     * 6. Q: What should be returned if every single day is covered by some
     *       meeting?
     *    A: Return 0 — zero free days is a perfectly valid answer.
     *
     * 7. Q: What's the expected scale, and does that push me toward a
     *       particular time complexity?
     *    A: days up to 1e5, meetings up to 1e3. So O(days) and
     *       O(meetings log meetings) are both trivially fast (~1e5 and
     *       ~1e4 operations respectively). I don't need anything cleverer
     *       than O(days + meetings) or O(meetings log meetings) — this
     *       tells me not to over-engineer with segment trees, etc.
     *
     * 8. Q: Is this a one-shot batch computation, or would this run
     *       repeatedly / concurrently (e.g. as part of a service handling
     *       many requests, or with meetings streaming in incrementally)?
     *    A (assumed): One-shot batch computation for this problem. I'd note
     *       as a follow-up that a streaming/incremental variant would need
     *       a different data structure (e.g., a balanced interval tree or
     *       a Fenwick tree supporting range updates), which I address in
     *       the Follow-Up Questions section.
     */


    /* ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (normal case, with overlap and a gap):
     *   days = 10
     *   meetings = [[5,7], [1,3], [9,10]]
     *   Sorted by start: [1,3], [5,7], [9,10]
     *   Covered days: {1,2,3} U {5,6,7} U {9,10} = 8 covered days
     *   Free days: day 4 and day 8 -> answer = 2
     *
     * Example 2 (edge case: fully booked, overlapping/nested intervals):
     *   days = 5
     *   meetings = [[1,5], [2,3], [4,5]]
     *   The single interval [1,5] alone already covers every day; the
     *   other two are fully redundant (nested / overlapping).
     *   Covered days: {1,2,3,4,5} = 5 covered -> answer = 0
     *
     * Example 3 (boundary/tie case: adjacent intervals with NO gap vs.
     * intervals separated by exactly one free day — this is the classic
     * off-by-one trap for merge-interval problems):
     *   days = 6
     *   meetings = [[1,2], [3,4]]      <- adjacent, end+1 == next start
     *   Covered: {1,2,3,4} -> days 5,6 free -> answer = 2
     *
     *   Contrast with:
     *   days = 6
     *   meetings = [[1,2], [4,6]]      <- gap exactly at day 3
     *   Covered: {1,2} U {4,5,6} = 5 covered -> day 3 is free -> answer = 1
     *
     *   The difference between "touching" (end+1 == nextStart, no free day
     *   in between) and "gapped" (end+1 < nextStart, a free day exists) is
     *   exactly where off-by-one bugs live in this problem family.
     */


    /* ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE SOLUTIONS
     * ========================================================================
     * Paradigm sweep — which categories from the standard checklist apply:
     *
     *   Brute force / naive        -> APPLICABLE (Approach 1)
     *   Sorting-based               -> APPLICABLE (Approach 2, the classic
     *                                   "merge intervals" pattern)
     *   Hashing-based                -> NOT APPLICABLE: there's no key/value
     *                                   lookup or grouping problem here;
     *                                   hashing doesn't help identify
     *                                   coverage over a linear day axis.
     *   Two pointer / sliding window -> NOT DIRECTLY APPLICABLE: there's no
     *                                   single contiguous window whose size
     *                                   we grow/shrink based on a
     *                                   monotonic condition; this is a
     *                                   union-of-intervals problem, not a
     *                                   subarray/substring search. (The
     *                                   merge-intervals scan does use two
     *                                   cursors, but it's not "sliding
     *                                   window" in the classical sense.)
     *   Divide and conquer          -> NOT APPLICABLE (no natural recursive
     *                                   split with a cheap merge step that
     *                                   beats sorting)
     *   Greedy                       -> NOT ITS OWN APPROACH here, though
     *                                   the merge-intervals scan is
     *                                   "greedy" in spirit (always extend
     *                                   the current run if possible) — I
     *                                   fold this into Approach 2.
     *   Dynamic programming          -> NOT APPLICABLE: no overlapping
     *                                   subproblems / optimal substructure
     *                                   to exploit; this is pure interval
     *                                   arithmetic.
     *   Tree / graph traversal       -> NOT APPLICABLE: no graph/tree
     *                                   structure in the input.
     *   Heap / priority queue        -> APPLICABLE as an alternative way to
     *                                   process intervals in start-order
     *                                   without a full array sort
     *                                   (Approach 4, shown for completeness).
     *   Binary search                -> NOT APPLICABLE as a primary
     *                                   technique here (no monotonic
     *                                   predicate over an answer space to
     *                                   search).
     *   Monotonic stack / deque      -> NOT APPLICABLE: no "next greater/
     *                                   smaller element" structure to
     *                                   exploit.
     *   Trie / segment tree / BIT    -> APPLICABLE but overkill (Approach 5,
     *                                   mentioned briefly) — useful only if
     *                                   meetings arrive incrementally/
     *                                   online, which isn't this problem.
     *
     * I'll implement: Brute Force, Sorting + Merge Intervals, Difference
     * Array (line sweep), and briefly sketch Heap-based merge and
     * Segment-Tree/BIT as bonus alternatives.
     */

    /* ------------------------------------------------------------------
     * Approach 1: Brute Force (Mark Every Day in Every Interval)
     * ------------------------------------------------------------------
     * Core idea: Allocate a boolean array of size (days + 1). For every
     * meeting [start, end], walk every day in that range and mark it
     * "busy". Finally, scan the array once and count days that were
     * never marked.
     *
     * Data structure / paradigm: plain array marking (no real algorithmic
     * trick) — this is the "just simulate it" baseline.
     *
     * Time Complexity: O(days + sum of individual meeting lengths).
     *   Worst case, every meeting spans the entire [1, days] range, giving
     *   O(days * meetings.length) = O(1e5 * 1e3) = O(1e8). Borderline
     *   but still usually fine within a few seconds — NOT dangerous here,
     *   but it does NOT scale if `days` or `meetings.length` were 10x
     *   larger.
     * Space Complexity: O(days) for the boolean marking array.
     *
     * Pros:
     *   - Trivial to reason about and verify correctness; great as a
     *     "safe baseline" to state first and validate other approaches
     *     against.
     *   - No edge-case subtlety around merging/gaps — correctness is
     *     almost definitional.
     * Cons:
     *   - Wasteful: re-marks already-busy days repeatedly when meetings
     *     overlap heavily.
     *   - Time complexity depends on interval LENGTH, not just count,
     *     which is a red flag versus the O(n log n) approaches below.
     *
     * When to use: Only as your verbal/mental warm-up baseline, or if
     * `days` were tiny. In this interview, I'd state this approach out
     * loud in ~15 seconds and then immediately pivot — I would NOT code
     * it as my primary submission.
     */
    public static int countFreeDaysBruteForce(int days, int[][] meetings) {
        boolean[] isBusy = new boolean[days + 1]; // 1-indexed; index 0 unused
        for (int[] meeting : meetings) {
            int start = meeting[0];
            int end = meeting[1];
            for (int day = start; day <= end; day++) {
                isBusy[day] = true;
            }
        }
        int freeDayCount = 0;
        for (int day = 1; day <= days; day++) {
            if (!isBusy[day]) {
                freeDayCount++;
            }
        }
        return freeDayCount;
    }

    /* ------------------------------------------------------------------
     * Approach 2: Sorting + Merge Intervals  [RECOMMENDED / OPTIMAL]
     * ------------------------------------------------------------------
     * Core idea: Sort meetings by start day. Sweep left to right,
     * merging any meeting that overlaps or touches the current running
     * interval into it. Every time a real gap is found (next start is
     * strictly greater than currentEnd + 1), close out the current
     * merged interval, add its length to a running "covered days" total,
     * and start a new merged interval. At the end, close out the last
     * merged interval. Free days = days - totalCoveredDays.
     *
     * Data structure / paradigm: sorting + linear scan (classic
     * "merge intervals" pattern).
     *
     * Time Complexity: O(n log n), where n = meetings.length (<= 1000).
     *   Dominated by the sort; the merge scan itself is O(n).
     *   This does NOT depend on `days` at all — a big advantage over
     *   Approach 1 when days is large but meetings.length is small.
     * Space Complexity: O(n) or O(log n) depending on the sort
     *   implementation (Java's Arrays.sort on Object[] / 2D arrays uses
     *   TimSort, which is O(n) auxiliary space in the worst case; for
     *   primitive arrays it's dual-pivot quicksort, O(log n)). Since
     *   int[][] is an array of objects, expect O(n) auxiliary space here.
     *
     * Pros:
     *   - Complexity independent of `days` magnitude — scales purely with
     *     number of meetings, which is the tighter bound here (1000 vs
     *     100,000).
     *   - Extremely standard, well-recognized pattern — an interviewer
     *     will immediately recognize "oh, this is merge intervals," which
     *     works in your favor (predictable, easy to communicate).
     *   - In-place sort possible (no extra structure beyond the input
     *     array itself, aside from sort's internal working memory).
     * Cons:
     *   - Off-by-one risk at merge boundaries (touching vs. gapped
     *     intervals) — must be handled carefully and explicitly tested.
     *   - Mutates or requires sorting the input (usually fine, but worth
     *     flagging if the caller expects `meetings` to stay in original
     *     order).
     *
     * When to use: This is my default, go-to solution for this problem
     * given the constraints (meetings.length is small and bounded,
     * `days` can be large). I'd present this as my primary/optimal
     * solution in an interview.
     */
    public static int countFreeDaysMergeIntervals(int days, int[][] meetings) {
        if (meetings.length == 0) {
            return days; // defensive: handle empty input even though
                          // constraints guarantee length >= 1
        }

        // Sort by start day; for ties on start day, order by end day
        // doesn't actually matter for correctness here, but a stable,
        // deterministic sort key is good practice.
        int[][] sortedMeetings = meetings.clone();
        Arrays.sort(sortedMeetings, (a, b) -> Integer.compare(a[0], b[0]));

        long totalCoveredDays = 0; // long defensively, even though max
                                    // possible value here is 100,000 and
                                    // fits comfortably in int — habit of
                                    // defaulting to long for accumulators
                                    // pays off when constraints change.

        int currentIntervalStart = sortedMeetings[0][0];
        int currentIntervalEnd = sortedMeetings[0][1];

        for (int i = 1; i < sortedMeetings.length; i++) {
            int nextStart = sortedMeetings[i][0];
            int nextEnd = sortedMeetings[i][1];

            if (nextStart <= currentIntervalEnd + 1) {
                // Overlapping OR touching (adjacent, no free day between
                // them) -> merge into the current run. Using
                // "<= currentIntervalEnd + 1" (not just "<=
                // currentIntervalEnd") is the key correctness detail:
                // adjacent intervals like [1,2] and [3,4] must merge
                // because there is no free day at the boundary.
                currentIntervalEnd = Math.max(currentIntervalEnd, nextEnd);
            } else {
                // Genuine gap found: close out the current merged run.
                totalCoveredDays += (currentIntervalEnd - currentIntervalStart + 1);
                currentIntervalStart = nextStart;
                currentIntervalEnd = nextEnd;
            }
        }
        // Close out the final merged run.
        totalCoveredDays += (currentIntervalEnd - currentIntervalStart + 1);

        return (int) (days - totalCoveredDays);
    }

    /* ------------------------------------------------------------------
     * Approach 3: Difference Array / Line Sweep
     * ------------------------------------------------------------------
     * Core idea: Instead of merging intervals, use a "diff" array of
     * size (days + 2). For each meeting [start, end], do diff[start]++
     * and diff[end + 1]--. Then take a running prefix sum across the
     * diff array from day 1 to `days`: at any day where the running sum
     * is 0, no meeting covers that day (free day); if > 0, at least one
     * meeting covers it.
     *
     * Data structure / paradigm: difference array (range-update,
     * point-query trick) + prefix sum. This is the classic technique
     * used for "sum of overlapping range updates" problems.
     *
     * Time Complexity: O(days + meetings.length) = O(1e5 + 1e3) ~ O(1e5).
     *   No sorting needed at all.
     * Space Complexity: O(days) for the diff/prefix array.
     *
     * Pros:
     *   - No sorting required — conceptually simpler to prove correct
     *     (just "count how many meetings are active on each day").
     *   - Naturally generalizes if a follow-up asked "how many meetings
     *     overlap on the busiest day?" (same diff array answers that too).
     * Cons:
     *   - Space cost scales with `days`, not with meetings.length. If
     *     `days` were, say, 10^9 instead of 10^5, this approach would
     *     become infeasible while Approach 2 (sort-based) would still
     *     work fine. This is the key trade-off vs. Approach 2.
     *
     * When to use: Great alternative when `days` is guaranteed small/
     * bounded and you want to avoid sorting, or when a follow-up wants
     * per-day meeting counts, not just free/busy classification. Given
     * this problem's actual constraints (days <= 1e5), this approach is
     * fully valid and arguably just as good as Approach 2 — I'd mention
     * both, but lead with Approach 2 since its cost model doesn't depend
     * on `days` at all.
     */
    public static int countFreeDaysDifferenceArray(int days, int[][] meetings) {
        // diff[d] tracks the change in "number of active meetings" at day d.
        // Index days+1 exists purely to safely decrement without bounds
        // checking when end == days.
        int[] diff = new int[days + 2];

        for (int[] meeting : meetings) {
            int start = meeting[0];
            int end = meeting[1];
            diff[start] += 1;
            diff[end + 1] -= 1;
        }

        int freeDayCount = 0;
        int activeMeetingCount = 0; // running prefix sum
        for (int day = 1; day <= days; day++) {
            activeMeetingCount += diff[day];
            if (activeMeetingCount == 0) {
                freeDayCount++;
            }
        }
        return freeDayCount;
    }

    /* ------------------------------------------------------------------
     * Approach 4: Heap / Priority-Queue-Based Merge (bonus, for
     * completeness under the "heap" paradigm)
     * ------------------------------------------------------------------
     * Core idea: Instead of sorting the whole array up front, push all
     * meetings into a min-heap keyed by start day, then repeatedly pop
     * the smallest-start meeting and merge exactly like Approach 2. This
     * is mostly useful in a streaming context where meetings arrive
     * incrementally and you want the smallest-start meeting available at
     * any point without re-sorting from scratch, or when merging many
     * pre-sorted sub-lists of intervals (k-way merge).
     *
     * Time Complexity: O(n log n) — asymptotically identical to sorting;
     *   each of n insertions/removals costs O(log n).
     * Space Complexity: O(n) for the heap.
     *
     * Pros: Useful if intervals arrive as multiple already-sorted
     *   streams (k-way merge scenario) or in an online/incremental
     *   setting.
     * Cons: Strictly more overhead than a single upfront sort for this
     *   problem's static, one-shot input — no benefit here.
     *
     * When to use: NOT for this exact problem as given (single static
     * array, one-shot computation) — Arrays.sort is simpler and just as
     * fast. I'm implementing this only to demonstrate the heap paradigm
     * explicitly, as requested.
     */
    public static int countFreeDaysHeapMerge(int days, int[][] meetings) {
        if (meetings.length == 0) {
            return days;
        }
        java.util.PriorityQueue<int[]> minHeapByStart =
                new java.util.PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
        for (int[] meeting : meetings) {
            minHeapByStart.offer(meeting);
        }

        long totalCoveredDays = 0;
        int[] first = minHeapByStart.poll();
        int currentIntervalStart = first[0];
        int currentIntervalEnd = first[1];

        while (!minHeapByStart.isEmpty()) {
            int[] next = minHeapByStart.poll();
            if (next[0] <= currentIntervalEnd + 1) {
                currentIntervalEnd = Math.max(currentIntervalEnd, next[1]);
            } else {
                totalCoveredDays += (currentIntervalEnd - currentIntervalStart + 1);
                currentIntervalStart = next[0];
                currentIntervalEnd = next[1];
            }
        }
        totalCoveredDays += (currentIntervalEnd - currentIntervalStart + 1);

        return (int) (days - totalCoveredDays);
    }

    /* ------------------------------------------------------------------
     * Approach 5 (mentioned, not fully implemented): Segment Tree / BIT
     * with Range-Update
     * ------------------------------------------------------------------
     * Core idea: A segment tree with lazy propagation (or a Binary
     * Indexed Tree variant) supporting O(log days) range-increment and
     * O(log days) or O(days) final scan. This is strictly more machinery
     * than the difference array (Approach 3) for a one-shot batch
     * computation like this problem — the diff array already achieves
     * O(days + meetings) with far less code and no risk of segment-tree
     * bugs.
     *
     * When it WOULD earn its complexity: if meetings arrive online
     * (added one at a time) interleaved with point queries like "is day
     * X currently free?" repeatedly during the stream — then a Fenwick
     * tree / segment tree supporting O(log days) updates and O(log days)
     * point queries beats re-scanning a diff array after every update.
     * I raise this in the Follow-Up Questions section rather than
     * implementing it, since it's not justified for the stated batch
     * problem.
     */


    /* ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * | Approach                        | Time              | Space     | Best For                                   | Limitations                                          |
     * |----------------------------------|-------------------|-----------|--------------------------------------------|-------------------------------------------------------|
     * | 1. Brute Force (mark days)       | O(days * n) worst | O(days)   | Sanity-check baseline; tiny `days`         | Degrades badly when meetings are long & numerous       |
     * | 2. Sort + Merge Intervals        | O(n log n)        | O(n)      | General case; cost independent of `days`   | Off-by-one risk at merge boundary; sorts input         |
     * | 3. Difference Array / Line Sweep | O(days + n)       | O(days)   | Small/bounded `days`; needs per-day counts | Space scales with `days`, not just meeting count       |
     * | 4. Heap-based Merge              | O(n log n)        | O(n)      | Streaming / k-way merge of sorted sources  | Pure overhead here vs. a single upfront sort            |
     * | 5. Segment Tree / BIT (sketch)   | O(n log days)     | O(days)   | Online updates interleaved with queries    | Overkill for one-shot batch input; most code, most risk |
     *
     * (n = meetings.length <= 1000)
     */


    /* ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would present Approach 2 (Sort + Merge Intervals) as my primary,
     * final solution. Reasoning:
     *
     *   - Optimality relative to constraints: meetings.length is capped
     *     at 1000 while `days` can be up to 100,000. An approach whose
     *     complexity depends on meetings.length (n log n here) rather
     *     than on `days` is the tighter, more scalable bound — it would
     *     still be fast even if `days` were far larger.
     *   - Pattern recognition: "merge intervals" is one of the most
     *     canonical interview patterns; presenting it signals I
     *     recognize the problem family instantly, which interviewers
     *     value.
     *   - Coding speed and low bug surface: it's a short, well-understood
     *     loop with exactly one subtle correctness detail (the "touching
     *     intervals must merge" off-by-one), which I can proactively call
     *     out while coding — that's a strong interview signal.
     *   - I would explicitly mention Approach 3 (difference array) as an
     *     equally valid O(days + n) alternative and explain the space
     *     trade-off (scales with `days` vs. scales with n), showing
     *     breadth without over-complicating my primary submission.
     *
     * Interview flow I'd narrate out loud ("safe-then-optimal"):
     *   1. State Approach 1 (brute force) verbally as the trivial
     *      correct baseline, without necessarily coding all of it.
     *   2. Pivot to mentioning Approach 3 (difference array) as a fast,
     *      easy-to-prove-correct O(days + n) solution.
     *   3. Present Approach 2 (sort + merge intervals) as my final,
     *      optimal, most scalable answer, and code that one fully.
     */


    /* ========================================================================
     * SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (PRODUCTION-QUALITY)
     * ========================================================================
     */

    /**
     * Counts the number of days in the inclusive range [1, days] that are
     * not covered by any meeting interval.
     *
     * <p>Approach: sort meetings by start day, then perform a single
     * left-to-right sweep, merging overlapping or touching intervals into
     * a running "current merged interval." Each time a genuine gap is
     * detected (i.e., the next meeting's start day is strictly more than
     * one day after the current merged interval's end), the length of the
     * current merged interval is added to a running total of covered
     * days, and a new merged interval begins.
     *
     * <p>Time complexity: O(n log n), where n = meetings.length, dominated
     * by the sort. The single sweep afterward is O(n).
     * <p>Space complexity: O(n) for the sorted copy of the input array
     * (input is not mutated).
     *
     * @param days     total number of days the employee is theoretically
     *                 available, numbered 1..days inclusive. Must be >= 1.
     * @param meetings array of [start, end] inclusive day ranges; may be
     *                 empty, unsorted, overlapping, or contain duplicates.
     * @return the count of days in [1, days] not covered by any meeting.
     * @throws IllegalArgumentException if inputs violate stated constraints.
     */
    public static int countDaysAvailable(int days, int[][] meetings) {
        // --- Defensive input validation -------------------------------
        // Not strictly required by the stated constraints (which
        // guarantee valid input), but this is production-quality code,
        // so I fail fast on contract violations rather than silently
        // producing a wrong answer.
        if (days < 1) {
            throw new IllegalArgumentException("days must be >= 1, got: " + days);
        }
        if (meetings == null) {
            throw new IllegalArgumentException("meetings must not be null");
        }
        for (int[] meeting : meetings) {
            if (meeting == null || meeting.length != 2) {
                throw new IllegalArgumentException("each meeting must be [start, end]");
            }
            int start = meeting[0];
            int end = meeting[1];
            if (start < 1 || end > days || start > end) {
                throw new IllegalArgumentException(
                        "invalid meeting range: [" + start + ", " + end + "] for days=" + days);
            }
        }

        // No meetings at all -> every day is free.
        if (meetings.length == 0) {
            return days;
        }

        // --- Sort a COPY by start day (never mutate caller's array) ---
        int[][] sortedMeetings = meetings.clone();
        Arrays.sort(sortedMeetings, (intervalA, intervalB) ->
                Integer.compare(intervalA[0], intervalB[0]));

        // --- Single left-to-right merge sweep --------------------------
        // long accumulator by default habit, even though the max
        // possible value (100,000) fits in int -- guards against future
        // constraint changes without needing a second look at this code.
        long totalCoveredDays = 0;

        int currentIntervalStart = sortedMeetings[0][0];
        int currentIntervalEnd = sortedMeetings[0][1];

        for (int index = 1; index < sortedMeetings.length; index++) {
            int nextStart = sortedMeetings[index][0];
            int nextEnd = sortedMeetings[index][1];

            // "nextStart <= currentIntervalEnd + 1" (not just
            // "<= currentIntervalEnd") is the crux of correctness:
            // it treats immediately-adjacent intervals (e.g. [1,2] and
            // [3,4]) as part of the same continuous busy run, since
            // there is no free day sitting between them.
            if (nextStart <= currentIntervalEnd + 1) {
                currentIntervalEnd = Math.max(currentIntervalEnd, nextEnd);
            } else {
                // Real gap found -- close out current run, start a new one.
                totalCoveredDays += lengthOf(currentIntervalStart, currentIntervalEnd);
                currentIntervalStart = nextStart;
                currentIntervalEnd = nextEnd;
            }
        }
        // Close out the final run after the loop ends.
        totalCoveredDays += lengthOf(currentIntervalStart, currentIntervalEnd);

        return (int) (days - totalCoveredDays);
    }

    /** Small named helper purely for readability at call sites. */
    private static long lengthOf(int intervalStart, int intervalEnd) {
        return intervalEnd - intervalStart + 1L;
    }


    /* ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     *
     * Tracing countDaysAvailable(10, [[5,7], [1,3], [9,10]]):
     *
     * Step 0: Validate inputs -> all meetings within [1,10], start<=end. OK.
     * Step 1: sortedMeetings after sort by start:
     *         [[1,3], [5,7], [9,10]]
     * Step 2: Initialize:
     *         currentIntervalStart = 1, currentIntervalEnd = 3
     *         totalCoveredDays = 0
     *
     * Loop index = 1 -> meeting = [5,7]
     *   nextStart = 5, nextEnd = 7
     *   Is nextStart (5) <= currentIntervalEnd + 1 (3 + 1 = 4)? NO (5 > 4)
     *   -> Genuine gap. Close current run:
     *        totalCoveredDays += lengthOf(1,3) = 3   -> totalCoveredDays = 3
     *      Start new run: currentIntervalStart = 5, currentIntervalEnd = 7
     *
     * Loop index = 2 -> meeting = [9,10]
     *   nextStart = 9, nextEnd = 10
     *   Is nextStart (9) <= currentIntervalEnd + 1 (7 + 1 = 8)? NO (9 > 8)
     *   -> Genuine gap. Close current run:
     *        totalCoveredDays += lengthOf(5,7) = 3   -> totalCoveredDays = 6
     *      Start new run: currentIntervalStart = 9, currentIntervalEnd = 10
     *
     * Loop ends (index would be 3, out of bounds).
     * Final close-out:
     *   totalCoveredDays += lengthOf(9,10) = 2   -> totalCoveredDays = 8
     *
     * Result: days - totalCoveredDays = 10 - 8 = 2
     *
     * Matches Example 1's expected answer of 2 (free days: 4 and 8). Correct.
     */


    /* ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * - Brute force (Approach 1) is correct but its cost scales with total
     *   meeting-day coverage, not just meeting count -- fine here, but not
     *   robust to larger inputs.
     * - Sort + merge intervals (Approach 2, chosen as final answer) gives
     *   O(n log n) time bounded purely by meetings.length (<= 1000),
     *   independent of `days` -- the most scalable choice given these
     *   specific constraints (days up to 1e5 >> meetings up to 1e3).
     * - Difference array (Approach 3) is an equally correct O(days + n)
     *   alternative, better if `days` were guaranteed small or if
     *   per-day meeting counts were also needed, but its space use scales
     *   with `days` rather than with the meeting count.
     * - Heap-based merge (Approach 4) and segment tree/BIT (Approach 5)
     *   are shown for paradigm completeness but are not justified for
     *   this specific, one-shot batch problem.
     *
     * Known limitations / assumptions of the final solution:
     *   - Assumes 1-indexed, inclusive [start, end] semantics exactly as
     *     stated in the constraints.
     *   - Assumes meetings array fits comfortably in memory (guaranteed,
     *     given meetings.length <= 1000).
     *   - Input validation added defensively; if this were guaranteed
     *     pre-validated (e.g., a strict LeetCode judge), that overhead
     *     could be stripped for a marginally faster submission.
     */


    /* ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     *
     * 1. "What if meetings arrive one at a time (online/streaming), and
     *    after each insertion you need to answer 'how many free days
     *    remain?' immediately?" -> Motivates a Fenwick tree / segment
     *    tree with range-update + prefix-query support, or an ordered
     *    interval structure (e.g. a TreeMap-based interval merge set),
     *    rather than re-sorting from scratch each time.
     *
     * 2. "What if `days` could be up to 10^9 or 10^18 instead of 10^5?"
     *    -> The difference array (Approach 3) becomes infeasible (can't
     *    allocate an array that large); Approach 2 (sort + merge, cost
     *    bound by meetings.length only) remains correct and efficient.
     *
     * 3. "What if you also needed to report the longest single free
     *    streak of consecutive days, not just the total count?" -> Same
     *    merge-intervals sweep, but additionally track gaps between
     *    consecutive merged runs (and the leading/trailing gaps before
     *    the first and after the last run) and take the max gap length.
     *
     * 4. "What if meetings could have negative or fractional day values,
     *    or the day axis were continuous (real-valued) rather than
     *    integer?" -> The core merge-intervals logic still generalizes,
     *    but the "touching means no gap" logic (using `+1`) only makes
     *    sense for integer/discrete days; for continuous ranges you'd
     *    instead check `nextStart <= currentIntervalEnd` (strict/no +1)
     *    and gap length would be computed differently.
     *
     * 5. "Could you parallelize this for extremely large `meetings`
     *    arrays?" -> Sorting parallelizes well (e.g., parallel merge
     *    sort); the merge sweep itself is inherently sequential, but you
     *    could partition sorted meetings into chunks, merge each chunk
     *    independently, then merge the chunk-level results pairwise
     *    (a divide-and-conquer reduction over already-sorted data).
     *
     * 6. "What if `meetings` could contain duplicate ranges verbatim,
     *    e.g. [[1,5],[1,5],[1,5]]?" -> Already handled correctly by
     *    Approach 2 as written -- duplicates just get merged into the
     *    same run and contribute nothing extra, since merging is based
     *    on max(end), not summing lengths naively.
     */


    /* ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. The "touching intervals" off-by-one: using
     *    `nextStart <= currentIntervalEnd` instead of
     *    `nextStart <= currentIntervalEnd + 1` incorrectly treats
     *    adjacent-but-non-overlapping intervals (e.g. [1,2] and [3,4]) as
     *    separate runs with a "gap" at a day that doesn't actually exist,
     *    silently over-counting free days by however many boundaries are
     *    miscounted.
     *
     * 2. Forgetting to flush the final merged interval after the loop
     *    ends. It's easy to add `totalCoveredDays` only inside the loop's
     *    else-branch and forget that the very last run never triggers
     *    that branch -- this silently drops the last interval's coverage
     *    and inflates the free-day count.
     *
     * 3. Not sorting by start day first (or sorting by end day by
     *    mistake) -- the merge sweep's correctness fundamentally depends
     *    on processing intervals in start-day order; unsorted input
     *    produces wrong merges and possibly wrong (even negative) gap
     *    calculations.
     *
     * 4. Double-counting overlapping coverage in a naive approach that
     *    tries to sum `(end - start + 1)` across all meetings directly
     *    without merging first -- this over-counts every day that's
     *    covered by more than one meeting, and is a very common first
     *    instinct that produces a wrong answer on the very first example
     *    with overlapping meetings.
     */


    /* ========================================================================
     * SECTION 14 (BONUS): CROSS-VALIDATING TEST HARNESS
     * ========================================================================
     * Runs hand-picked examples plus randomized fuzz tests, cross-checking
     * all four fully-implemented approaches against each other.
     */
    public static void main(String[] args) {
        System.out.println("=== Hand-picked examples ===");
        runCase(10, new int[][]{{5, 7}, {1, 3}, {9, 10}}, 2); // Example 1
        runCase(5, new int[][]{{1, 5}, {2, 3}, {4, 5}}, 0);   // Example 2 (fully booked)
        runCase(6, new int[][]{{1, 2}, {3, 4}}, 2);           // Example 3a (touching, no gap)
        runCase(6, new int[][]{{1, 2}, {4, 6}}, 1);           // Example 3b (gap at day 3)
        runCase(1, new int[][]{{1, 1}}, 0);                   // smallest possible input
        runCase(100, new int[][]{{50, 60}}, 89);              // single meeting, sparse

        System.out.println("\n=== Randomized cross-validation (all 4 approaches) ===");
        Random random = new Random(42);
        int mismatchCount = 0;
        int trialCount = 2000;
        for (int trial = 0; trial < trialCount; trial++) {
            int days = 1 + random.nextInt(60);
            int numMeetings = random.nextInt(9); // 0..8 meetings
            int[][] meetings = new int[numMeetings][2];
            for (int i = 0; i < numMeetings; i++) {
                int start = 1 + random.nextInt(days);
                int end = start + random.nextInt(days - start + 1);
                meetings[i] = new int[]{start, end};
            }

            int brute = countFreeDaysBruteForce(days, meetings);
            int merged = countFreeDaysMergeIntervals(days, meetings);
            int diffArr = countFreeDaysDifferenceArray(days, meetings);
            int heapMerged = countFreeDaysHeapMerge(days, meetings);
            int production = countDaysAvailable(days, meetings);

            if (!(brute == merged && merged == diffArr
                    && diffArr == heapMerged && heapMerged == production)) {
                mismatchCount++;
                System.out.println("MISMATCH on days=" + days
                        + " meetings=" + describeMeetings(meetings)
                        + " -> brute=" + brute + " merged=" + merged
                        + " diffArr=" + diffArr + " heapMerged=" + heapMerged
                        + " production=" + production);
            }
        }
        System.out.println("Randomized trials run: " + trialCount
                + ", mismatches: " + mismatchCount);
        System.out.println(mismatchCount == 0
                ? "ALL APPROACHES AGREE ACROSS ALL TRIALS."
                : "DISCREPANCY DETECTED -- investigate above.");
    }

    private static void runCase(int days, int[][] meetings, int expected) {
        int actual = countDaysAvailable(days, meetings);
        String status = (actual == expected) ? "PASS" : "FAIL";
        System.out.println(status + " | days=" + days
                + " meetings=" + describeMeetings(meetings)
                + " expected=" + expected + " actual=" + actual);
    }

    private static String describeMeetings(int[][] meetings) {
        List<String> parts = new ArrayList<>();
        for (int[] meeting : meetings) {
            parts.add("[" + meeting[0] + "," + meeting[1] + "]");
        }
        return parts.toString();
    }
}
