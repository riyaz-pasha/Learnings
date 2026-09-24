import java.util.*;

/*
================================================================================
 MOCK GOOGLE ONSITE INTERVIEW — FULL TRANSCRIPT
 Problem: Employee Free Time  (LeetCode 759, Google-tagged, Hard)
 Language: Java 21+
================================================================================
*/

class EmployeeFreeTime {

    /*
    ============================================================================
    SECTION 1 — RESTATE THE PROBLEM
    ============================================================================
    In my own words:

        We are given the working schedule of several employees. Each employee's
        schedule is a list of Interval objects (each has a `start` and `end`,
        both ints) representing the times they are BUSY. Within one employee's
        own list, the intervals are already sorted by start time and are
        guaranteed non-overlapping.

        I need to find every stretch of time during which *every single
        employee* is simultaneously free — i.e., the common gaps across all
        employees' busy schedules. The answer must:
          - only include gaps that are "finite" (bounded on both sides — we
            never report anything before the first busy moment overall or
            after the last, since those are unbounded / not meaningful here),
          - exclude zero-length intervals (e.g., [3,3] is not a real gap),
          - be sorted in ascending order,
          - presumably not overlap with each other (they're gaps between
            merged busy time, so this falls out naturally).

    Key constraints:
        * 1 <= schedule.length <= 50            → up to 50 employees
        * 1 <= schedule[i].length <= 50         → up to 50 intervals/employee
        * 0 <= interval.start < interval.end <= 1e8
        So total intervals N <= 50 * 50 = 2500. This is a SMALL input — an
        important signal for how much I should over-engineer the solution.

    Inputs / Outputs:
        Input:  List<List<Interval>> schedule — outer list is per-employee,
                inner list is that employee's sorted, non-overlapping busy
                intervals.
        Output: List<Interval> — sorted, non-overlapping, positive-length
                intervals representing common free time across ALL employees.

    Assumptions I'm stating out loud:
        * Interval.start < Interval.end always holds per the constraints, so
          I don't need to defensively handle malformed single intervals.
        * A given employee's own intervals never overlap or touch in a way
          that needs merging *within* one employee — but I will NOT rely on
          that being true across different employees, since that's the whole
          point of the problem.
    */


    /*
    ============================================================================
    SECTION 2 — CLARIFYING QUESTIONS (asked to the interviewer, with the
    answers I will assume for the rest of this session if not corrected)
    ============================================================================
    1. Q: Can `schedule` or an individual employee's interval list ever be
          empty?
       A (assumed): No — constraints guarantee schedule.length >= 1 and
          schedule[i].length >= 1, so every employee has at least one busy
          interval. I will still code defensively for empty lists since it's
          cheap insurance.

    2. Q: Are intervals within one employee's list guaranteed sorted AND
          non-overlapping, or do I need to merge within an employee first?
       A (assumed): Guaranteed sorted & non-overlapping per problem statement,
          so I skip a per-employee merge step. (I'll still note this could be
          double-checked defensively.)

    3. Q: Should touching intervals like [1,3] and [3,5] be treated as
          overlapping (merged into [1,5]) when combining ACROSS employees?
       A (assumed): Yes — if one employee's busy time ends exactly when
          another's begins, there's zero free time at that boundary, so for
          the purpose of computing the union of busy time, touching intervals
          should be merged (using `<=` when comparing end/start, not `<`).
          This directly enforces "no zero-length gaps in the output."

    4. Q: Do we report free time before the very first busy moment across all
          employees, or after the very last one (i.e., unbounded free time)?
       A (assumed): No — the problem explicitly asks for "finite" intervals,
          so free time before the global minimum start or after the global
          maximum end is excluded/undefined and not part of the output.

    5. Q: What is the expected return type/format — array of int[2], or a
          custom Interval object?
       A (assumed): Per the problem statement, output is a List<Interval> of
          Interval objects (matching the input's object-based representation,
          not int arrays).

    6. Q: What's the realistic scale? Could this run at Google-search scale
          (millions of intervals), or is it small (calendar-app scale)?
       A (assumed): Small — constraints cap total intervals at 2500. I will
          design for correctness and clarity first, but I'll mention how the
          approach changes if N were, say, 10^7 or if K (employee count) were
          huge relative to N.

    7. Q: Can two different employees have identical intervals (duplicates)?
       A (assumed): Yes, duplicates across employees are allowed and should
          just be handled naturally by interval merging (a duplicate busy
          interval doesn't create or destroy any free time).

    8. Q: Is this a one-shot batch computation, or do employees' schedules
          change over time and I need to support incremental updates
          (concurrency)?
       A (assumed): One-shot batch computation for this problem. I'll address
          the "what if schedules update dynamically" angle in the follow-up
          questions section, since it changes the right data structure
          (e.g., a balanced BST / interval tree) quite a bit.
    */


    /*
    ============================================================================
    SECTION 3 — EXAMPLES & EDGE CASES
    ============================================================================

    Example 1 (Normal case):
        Employee A: [[1,2],[5,6]]
        Employee B: [[1,3]]
        Employee C: [[4,10]]
        Flattened busy time: [1,2],[5,6],[1,3],[4,10]
        Sorted:               [1,2],[1,3],[4,10],[5,6]
        Merged busy union:    [1,3],[4,10]
        Free time (finite, between first merged busy start and last merged
        busy end): the gap between [1,3] and [4,10] → [3,4].
        Expected output: [[3,4]]

    Example 2 (Edge case — single employee, single interval):
        Employee A: [[5,8]]
        Only one busy block exists overall. There is no *second* busy block to
        form a bounded gap against, so there is NO finite common free
        interval at all.
        Expected output: [] (empty list)

    Example 3 (Boundary / tie-breaking case — touching intervals & zero-length
    trap):
        Employee A: [[1,3],[6,7]]
        Employee B: [[2,4]]
        Employee C: [[3,3+1],[7,8]]   // i.e. [3,4] and [7,8]
        Flattened & sorted busy:  [1,3],[2,4],[3,4],[6,7],[7,8]
        Merging step by step:
          start with [1,3]
          [2,4]: 2 <= 3 (touches/overlaps) → merge → [1,4]
          [3,4]: 3 <= 4 → merge → [1,4] (no change, fully contained)
          [6,7]: 6 > 4  → gap found: [4,6] is free → push [1,4] result,
                 start new merged interval [6,7]
          [7,8]: 7 <= 7 (touches) → merge → [6,8]
        Merged busy union: [1,4], [6,8]
        Free time: [4,6]
        Note: because [6,7] and [7,8] TOUCH at 7, they merge into [6,8] with
        NO gap at 7 — this is exactly why I merge using "<=" rather than "<".
        If I had incorrectly used strict "<" I would have produced a bogus
        zero-length free interval [7,7], which the problem explicitly forbids.
        Expected output: [[4,6]]
    */


    /* ============================================================================
       Interval class — matches the problem's object-based representation.
       (Not int[]; fields are `start` and `end`, e.g. schedule[1][1].start)
       ============================================================================ */
    static class Interval {
        int start;
        int end;
        Interval(int start, int end) {
            this.start = start;
            this.end = end;
        }
        @Override
        public String toString() {
            return "[" + start + ", " + end + "]";
        }
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Interval)) return false;
            Interval that = (Interval) other;
            return this.start == that.start && this.end == that.end;
        }
        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }
    }


    /*
    ============================================================================
    SECTION 4, 5, 6 — ALL POSSIBLE APPROACHES
    ============================================================================
    Dimensions explicitly considered, with irrelevant ones ruled out up front:

      - Brute force / naive .......... APPLICABLE  → Approach 1
      - Sorting-based ................ APPLICABLE  → Approach 2 (also the
                                         optimal / recommended approach)
      - Hashing-based ................ NOT APPLICABLE — intervals need
                                         ORDER (start/end comparisons), not
                                         key→value lookup. Hashing doesn't
                                         help find adjacency or gaps between
                                         ranges.
      - Two pointer / sliding window . APPLICABLE  → Approach 4 (pairwise
                                         merge of two employees' busy time
                                         at a time, à la "Interval List
                                         Intersection")
      - Divide and conquer ........... APPLICABLE  → Approach 5
      - Greedy ........................ APPLICABLE, but not as a standalone
                                         approach — the sweep in Approach 2
                                         *is* a greedy interval-merge
                                         algorithm (always extend the current
                                         merged interval greedily instead of
                                         starting a new one, whenever
                                         possible).
      - Dynamic programming .......... NOT APPLICABLE — there's no optimal
                                         substructure / overlapping
                                         subproblem being optimized over
                                         (e.g., no "choose or skip" decision
                                         with a value to maximize/minimize).
                                         This is a pure interval-geometry
                                         merge/scan problem.
      - Tree / graph traversal ....... NOT APPLICABLE — employees/intervals
                                         don't form a graph or tree to
                                         traverse. (A balanced BST / interval
                                         tree DOES show up as an ADVANCED
                                         DATA STRUCTURE for the "dynamic
                                         updates" follow-up — see Section 12
                                         — but that's a structure choice, not
                                         a traversal algorithm here.)
      - Heap / priority queue ........ APPLICABLE  → Approach 3 (classic
                                         "merge K sorted lists" pattern)
      - Binary search ................. NOT APPLICABLE — there's no
                                         monotonic predicate over a single
                                         sorted search space to binary search
                                         on for the core problem. (It CAN
                                         appear in a follow-up variant, e.g.
                                         "does employee X have a free slot of
                                         length >= D?", answered by binary
                                         searching over gap sizes — mentioned
                                         in follow-ups.)
      - Monotonic stack / deque ...... APPLICABLE AS AN IMPLEMENTATION
                                         VARIANT of Approach 2 — instead of
                                         tracking "current merged interval"
                                         in two local variables, you can push
                                         merged intervals onto an explicit
                                         Deque<Interval> and merge with the
                                         top of the stack. Same complexity,
                                         same idea, more machinery — I show
                                         it as a short note rather than a
                                         full separate approach since it adds
                                         no algorithmic value here.
      - Trie / segment tree / advanced NOT NEEDED for the static, one-shot
        structures                      version of this problem given N <=
                                         2500. Segment trees / interval trees
                                         become relevant if schedules update
                                         dynamically and we need repeated
                                         free-time queries — covered in
                                         follow-ups.
    */

    /* ---------------------------------------------------------------------
       APPROACH 1: Brute Force — Candidate Boundary Point Scan
       ---------------------------------------------------------------------
       Core idea (plain English):
         Don't try to be clever about merging. Instead, gather every interval
         boundary (every start and every end) from every employee into one
         list, sort the unique boundary points, and treat each consecutive
         pair of boundary points [p1, p2] as a CANDIDATE free segment. For
         each candidate, brute-force scan ALL intervals to check whether any
         of them overlaps [p1, p2]. If none do, it's a free segment. Finally,
         stitch together any adjacent free segments (this can happen because
         boundary points from *different* employees can coincide without a
         busy interval actually spanning the point) and drop zero-length /
         unbounded ends.

       Data structure / paradigm: plain array/list scanning — no merging
       algorithm, just direct verification. This is the "prove correctness
       from first principles" version.

       Time Complexity: O(N^2 log N)
         - N = total interval count (<= 2500). Sorting boundaries is
           O(N log N). For each of the O(N) candidate segments we scan all
           N intervals → O(N^2) dominates.
       Space Complexity: O(N) for the boundary list and output.

       Pros:
         - Very easy to convince yourself (and an interviewer) it's correct;
           almost a direct restatement of the definition of "free time."
         - No merging bugs possible — it's a pure verification approach.
       Cons:
         - Quadratic; would not scale if N were large.
         - More code than the optimal approach, for no benefit once N grows.
       When to use / not use:
         - Use it only as a sanity-check "oracle" to stress-test the optimal
           solution (which is exactly how I use it in this file's test
           harness) — never as the delivered solution in an interview once
           you've identified the sweep-line approach.
       --------------------------------------------------------------------- */
    static List<Interval> bruteForceApproach(List<List<Interval>> schedule) {
        List<Interval> allIntervals = new ArrayList<>();
        for (List<Interval> employee : schedule) {
            allIntervals.addAll(employee);
        }
        if (allIntervals.isEmpty()) return new ArrayList<>();

        // Collect every distinct boundary point.
        TreeSet<Integer> boundarySet = new TreeSet<>();
        for (Interval interval : allIntervals) {
            boundarySet.add(interval.start);
            boundarySet.add(interval.end);
        }
        List<Integer> boundaries = new ArrayList<>(boundarySet);

        List<Interval> freeSegments = new ArrayList<>();
        for (int i = 0; i + 1 < boundaries.size(); i++) {
            int segmentStart = boundaries.get(i);
            int segmentEnd = boundaries.get(i + 1);
            if (segmentStart >= segmentEnd) continue; // guard zero-length

            boolean isBusySomewhere = false;
            for (Interval interval : allIntervals) {
                // Overlap test: interval overlaps [segmentStart, segmentEnd)
                // if interval.start < segmentEnd && interval.end > segmentStart
                if (interval.start < segmentEnd && interval.end > segmentStart) {
                    isBusySomewhere = true;
                    break;
                }
            }
            if (!isBusySomewhere) {
                freeSegments.add(new Interval(segmentStart, segmentEnd));
            }
        }

        // Stitch together any adjacent free segments (touching boundaries
        // that are both free, e.g. two employees' boundaries lined up
        // without anyone actually being busy across the join point).
        List<Interval> merged = new ArrayList<>();
        for (Interval segment : freeSegments) {
            if (!merged.isEmpty() && merged.get(merged.size() - 1).end == segment.start) {
                merged.get(merged.size() - 1).end = segment.end;
            } else {
                merged.add(segment);
            }
        }

        // "Finite" requirement: free time strictly between the very first
        // and very last boundary is already all we produced (candidates
        // only exist between boundary points), so unbounded ends before the
        // first / after the last busy moment are automatically excluded.
        return merged;
    }

    /* ---------------------------------------------------------------------
       APPROACH 2: Sorting + Merge (Sweep Line)  — ⭐ RECOMMENDED / OPTIMAL
       ---------------------------------------------------------------------
       Core idea (plain English):
         Flatten every employee's busy intervals into one big list, ignoring
         which employee owns which interval (ownership doesn't matter — we
         just need the UNION of all busy time). Sort that list by start time.
         Sweep left to right, greedily merging any interval that overlaps or
         touches the current merged interval. Every time we're FORCED to
         start a new merged interval (because the next interval's start is
         strictly after the current merged interval's end), the gap between
         them is a free interval.

       Data structure / paradigm: sorting + greedy linear sweep ("merge
       intervals" pattern).

       Time Complexity: O(N log N) — dominated by the sort; the sweep itself
         is O(N).
       Space Complexity: O(N) — for the flattened list and output.

       Pros:
         - Simple, very fast to write correctly under interview pressure.
         - Easy to explain and reason about — this is the textbook "merge
           intervals" pattern most interviewers immediately recognize.
         - Optimal time complexity given we must at least look at every
           interval and there's an Ω(N log N) lower bound from
           interval-scheduling-style problems in the comparison model
           (equivalent to sorting).
       Cons:
         - Discards structure of the original per-employee grouping. Fine
           here (we don't need it), but would not directly help if a
           follow-up asked "which pair of employees caused this busy
           overlap?"
       When to use / not use:
         - Use this as the primary interview answer: it hits the complexity
           optimum with the least code and the least chance of a bug.
         - Wouldn't change it unless a follow-up specifically demands
           per-employee attribution or streaming/incremental updates.
       --------------------------------------------------------------------- */
    static List<Interval> sortAndMergeApproach(List<List<Interval>> schedule) {
        List<Interval> allIntervals = new ArrayList<>();
        for (List<Interval> employee : schedule) {
            allIntervals.addAll(employee);
        }
        if (allIntervals.isEmpty()) return new ArrayList<>();

        allIntervals.sort((a, b) -> Integer.compare(a.start, b.start));

        List<Interval> freeTime = new ArrayList<>();
        int mergedEnd = allIntervals.get(0).end;
        for (int i = 1; i < allIntervals.size(); i++) {
            Interval current = allIntervals.get(i);
            if (current.start > mergedEnd) {
                // Gap found between mergedEnd and current.start.
                freeTime.add(new Interval(mergedEnd, current.start));
                mergedEnd = current.end;
            } else {
                // Overlaps or touches (<=) the running merged block — extend it.
                mergedEnd = Math.max(mergedEnd, current.end);
            }
        }
        return freeTime;
    }

    /* ---------------------------------------------------------------------
       APPROACH 3: Min-Heap — K-way Merge
       ---------------------------------------------------------------------
       Core idea (plain English):
         Each employee's own list is already sorted, so this is structurally
         identical to "merge K sorted lists." Push each employee's FIRST
         interval into a min-heap keyed by start time. Repeatedly pop the
         smallest, compare it against a running "current merged end," extend
         or record a gap exactly like Approach 2, then push that employee's
         NEXT interval (if any) into the heap.

       Data structure / paradigm: priority queue / heap, K-way merge pattern.

       Time Complexity: O(N log K), K = number of employees (<= 50). Each of
         the N intervals is pushed/popped from a heap of size <= K once.
       Space Complexity: O(K) for the heap (plus O(N) for output).

       Pros:
         - Strictly better than sorting all N intervals when K << N (many
           intervals per employee, few employees) since log K < log N.
         - Naturally "streams": you never need all intervals in memory
           simultaneously — useful if data arrived incrementally per
           employee.
       Cons:
         - More code / bookkeeping (per-employee index tracking) than
           Approach 2 for the same asymptotic ballpark, given this
           problem's small constraints (N <= 2500, K <= 50 → log K vs
           log N barely matters here).
         - Heap operations have larger constant factors than a plain sort.
       When to use / not use:
         - Use in an interview as your "here's an alternative, and here's
           when it would actually win" follow-up discussion point — good for
           showing depth after delivering Approach 2.
         - Wouldn't lead with it: given the tiny constraints in THIS
           problem, the constant-factor overhead isn't worth it.
       --------------------------------------------------------------------- */
    static List<Interval> minHeapKWayMergeApproach(List<List<Interval>> schedule) {
        // heapEntry = {employeeIndex, intervalIndexWithinEmployee}
        PriorityQueue<int[]> minHeap = new PriorityQueue<>(
            (a, b) -> Integer.compare(
                schedule.get(a[0]).get(a[1]).start,
                schedule.get(b[0]).get(b[1]).start
            )
        );

        for (int employeeIndex = 0; employeeIndex < schedule.size(); employeeIndex++) {
            if (!schedule.get(employeeIndex).isEmpty()) {
                minHeap.offer(new int[]{employeeIndex, 0});
            }
        }

        List<Interval> freeTime = new ArrayList<>();
        if (minHeap.isEmpty()) return freeTime;

        int[] firstEntry = minHeap.poll();
        int mergedEnd = schedule.get(firstEntry[0]).get(firstEntry[1]).end;
        pushNext(minHeap, schedule, firstEntry[0], firstEntry[1]);

        while (!minHeap.isEmpty()) {
            int[] entry = minHeap.poll();
            Interval current = schedule.get(entry[0]).get(entry[1]);

            if (current.start > mergedEnd) {
                freeTime.add(new Interval(mergedEnd, current.start));
                mergedEnd = current.end;
            } else {
                mergedEnd = Math.max(mergedEnd, current.end);
            }
            pushNext(minHeap, schedule, entry[0], entry[1]);
        }
        return freeTime;
    }

    // Helper: push the next interval for a given employee onto the heap, if any.
    private static void pushNext(PriorityQueue<int[]> minHeap, List<List<Interval>> schedule,
                                  int employeeIndex, int intervalIndex) {
        if (intervalIndex + 1 < schedule.get(employeeIndex).size()) {
            minHeap.offer(new int[]{employeeIndex, intervalIndex + 1});
        }
    }

    /* ---------------------------------------------------------------------
       APPROACH 4: Two-Pointer Pairwise Calendar Merge
       ---------------------------------------------------------------------
       Core idea (plain English):
         Reduce K employees down to ONE combined "busy calendar" by merging
         two employees' busy-interval lists at a time using the classic
         two-pointer technique (the same one used in "Interval List
         Intersections," but here we union busy time rather than intersect
         free time). After folding all K employees into a single sorted,
         merged busy-interval list, extract the gaps exactly like Approach 2.

       Data structure / paradigm: two-pointer merge (like the merge step of
       merge sort, but on interval lists).

       Time Complexity: O(N * K) in the worst case — merging employee i's
         (already-merged) running list of size up to N against employee i+1's
         list happens K-1 times, and each merge is linear in the sizes
         involved. With N <= 2500 and K <= 50 this is comfortably fast in
         practice, but asymptotically worse than O(N log N) or O(N log K)
         for large K.
       Space Complexity: O(N) for the running merged list.

       Pros:
         - Conceptually clean pairwise reduction; reuses a very well-known
           two-pointer subroutine (merge two sorted interval lists) that's
           useful in its own right (e.g., LC 986 Interval List
           Intersections).
         - No custom comparator/heap machinery needed.
       Cons:
         - Worse asymptotic complexity than Approaches 2 and 3 when K grows
           large, since the running merged list can grow up to N long and we
           re-scan it K-1 times.
       When to use / not use:
         - Good to mention as "the two-pointer angle" to show breadth, and
           genuinely useful if employees must be merged incrementally one at
           a time as new employees are added to a live calendar app.
         - Not the interview's final answer given it's strictly dominated by
           Approach 2 here.
       --------------------------------------------------------------------- */
    static List<Interval> twoPointerPairwiseMergeApproach(List<List<Interval>> schedule) {
        if (schedule.isEmpty()) return new ArrayList<>();

        List<Interval> combinedBusy = new ArrayList<>(schedule.get(0));
        for (int employeeIndex = 1; employeeIndex < schedule.size(); employeeIndex++) {
            combinedBusy = mergeTwoSortedBusyLists(combinedBusy, schedule.get(employeeIndex));
        }
        return extractGapsFromMergedBusy(combinedBusy);
    }

    // Two-pointer merge of two SORTED (but possibly mutually overlapping)
    // busy-interval lists into one sorted, non-overlapping busy list.
    private static List<Interval> mergeTwoSortedBusyLists(List<Interval> listA, List<Interval> listB) {
        List<Interval> combined = new ArrayList<>(listA.size() + listB.size());
        int pointerA = 0, pointerB = 0;
        // Merge-sort-style interleave by start time.
        while (pointerA < listA.size() && pointerB < listB.size()) {
            if (listA.get(pointerA).start <= listB.get(pointerB).start) {
                combined.add(listA.get(pointerA++));
            } else {
                combined.add(listB.get(pointerB++));
            }
        }
        while (pointerA < listA.size()) combined.add(listA.get(pointerA++));
        while (pointerB < listB.size()) combined.add(listB.get(pointerB++));

        // Now collapse overlaps/touches in the interleaved list.
        List<Interval> merged = new ArrayList<>();
        for (Interval interval : combined) {
            if (!merged.isEmpty() && interval.start <= merged.get(merged.size() - 1).end) {
                Interval last = merged.get(merged.size() - 1);
                last.end = Math.max(last.end, interval.end);
            } else {
                merged.add(new Interval(interval.start, interval.end));
            }
        }
        return merged;
    }

    private static List<Interval> extractGapsFromMergedBusy(List<Interval> mergedBusy) {
        List<Interval> freeTime = new ArrayList<>();
        for (int i = 0; i + 1 < mergedBusy.size(); i++) {
            int gapStart = mergedBusy.get(i).end;
            int gapEnd = mergedBusy.get(i + 1).start;
            if (gapEnd > gapStart) {
                freeTime.add(new Interval(gapStart, gapEnd));
            }
        }
        return freeTime;
    }

    /* ---------------------------------------------------------------------
       APPROACH 5: Divide and Conquer
       ---------------------------------------------------------------------
       Core idea (plain English):
         Instead of folding employees one-by-one left to right (Approach 4),
         recursively split the list of employees in half, solve (merge) each
         half independently into its own combined busy list, then merge the
         two resulting busy lists together with the same two-pointer merge
         subroutine from Approach 4. This is exactly "merge sort" applied at
         the level of employees instead of individual elements.

       Data structure / paradigm: divide and conquer, reusing the pairwise
       merge subroutine from Approach 4.

       Time Complexity: O(N log K) — this is the classic "merge K sorted
         lists via divide and conquer" recurrence: T(K) = 2T(K/2) + O(size),
         which works out to O(N log K) total merge work across log K levels
         of recursion, each level doing O(N) total merge work.
       Space Complexity: O(N) for intermediate merged lists + O(log K)
         recursion stack.

       Pros:
         - Same complexity class as the heap approach, O(N log K), but often
           easier to reason about / parallelize (independent subtrees can be
           computed concurrently).
         - Demonstrates comfort with recursion and the merge-sort mental
           model, which interviewers like to see reused.
       Cons:
         - Recursion adds constant-factor overhead and stack usage versus
           the flat iterative sweep of Approach 2.
         - For this problem's tiny K (<= 50), the log K improvement over
           log N is negligible in practice.
       When to use / not use:
         - Good "if I had to parallelize this across machines/cores" answer.
         - Not the primary answer for this exact problem's constraints.
       --------------------------------------------------------------------- */
    static List<Interval> divideAndConquerApproach(List<List<Interval>> schedule) {
        if (schedule.isEmpty()) return new ArrayList<>();
        List<Interval> mergedBusy = divideAndConquerHelper(schedule, 0, schedule.size() - 1);
        return extractGapsFromMergedBusy(mergedBusy);
    }

    private static List<Interval> divideAndConquerHelper(List<List<Interval>> schedule, int left, int right) {
        if (left == right) {
            // Base case: a single employee's list is already sorted & non-overlapping.
            return new ArrayList<>(schedule.get(left));
        }
        int mid = left + (right - left) / 2;
        List<Interval> leftMerged = divideAndConquerHelper(schedule, left, mid);
        List<Interval> rightMerged = divideAndConquerHelper(schedule, mid + 1, right);
        return mergeTwoSortedBusyLists(leftMerged, rightMerged);
    }


    /*
    ============================================================================
    SECTION 7 — APPROACHES COMPARISON TABLE
    ============================================================================
    Approach                          | Time         | Space  | Best For                                  | Limitations
    -----------------------------------|--------------|--------|-------------------------------------------|--------------------------------------------
    1. Brute Force (boundary scan)     | O(N^2 log N) | O(N)   | Correctness oracle / stress-test baseline  | Quadratic, never the delivered answer
    2. Sort + Merge (sweep line) ⭐     | O(N log N)   | O(N)   | THE interview answer — simplest & optimal  | Loses per-employee attribution (not needed)
    3. Min-Heap K-way merge            | O(N log K)   | O(K)   | K << N, streaming/incremental input        | More code, worse constants than sort for small N
    4. Two-Pointer pairwise merge      | O(N * K)     | O(N)   | Employees merged in one at a time (online) | Asymptotically worse for large K
    5. Divide and Conquer              | O(N log K)   | O(N)   | Parallelizable merges                      | Recursion overhead; no benefit at this K
    ============================================================================
    */


    /*
    ============================================================================
    SECTION 8 — RECOMMENDED APPROACH FOR THE INTERVIEW
    ============================================================================
    I would present Approach 2 (Sort + Merge / Sweep Line) as my primary,
    coded solution, for these reasons:

      * Clarity: it's the "merge intervals" pattern almost every interviewer
        will instantly recognize and be able to verify by eye — fewer places
        for a subtle bug to hide (no heap comparator, no recursion, no
        per-employee index bookkeeping).
      * Coding speed: ~15-20 lines, one sort call, one linear sweep. I can
        write, dry-run, and test this comfortably within interview time.
      * Optimality: O(N log N) is optimal given we must inspect every
        interval and effectively sort them (an Ω(N log N) lower bound
        applies via a standard reduction from sorting/element-uniqueness).
      * Interviewer expectations: for a "Hard" interval-merging problem, the
        expected bar is exactly this pattern. I'd mention Approaches 3 and 5
        (O(N log K)) proactively afterward to show I understand when K
        matters and demonstrate range, without over-engineering the primary
        submission.

    I would explicitly walk through the brute-force idea first out loud (as
    the baseline), then pivot: "since all we actually need is the UNION of
    busy time, sorting and sweeping once is strictly better — let me code
    that," which mirrors real interview signaling of going from naive to
    optimal deliberately rather than jumping straight to the answer.
    */


    /*
    ============================================================================
    SECTION 9 — DEEP DIVE: OPTIMAL SOLUTION (production-quality)
    ============================================================================
    Same algorithm as Approach 2, restated as the "final" polished method
    with full inline reasoning on every decision, as I would actually submit
    it in the interview / in production code.
    */
    static List<Interval> findEmployeeFreeTime(List<List<Interval>> schedule) {
        // Defensive guard: constraints promise schedule is non-empty with
        // non-empty employee lists, but I code defensively anyway — cheap
        // insurance against malformed input in production.
        if (schedule == null || schedule.isEmpty()) {
            return new ArrayList<>();
        }

        // Step 1: Flatten. Employee identity is irrelevant to the answer —
        // we only care about the UNION of everyone's busy time — so throw
        // every interval into one flat list.
        List<Interval> allBusyIntervals = new ArrayList<>();
        for (List<Interval> employeeSchedule : schedule) {
            allBusyIntervals.addAll(employeeSchedule);
        }
        if (allBusyIntervals.isEmpty()) {
            return new ArrayList<>();
        }

        // Step 2: Sort by start time. This is what lets a single linear
        // sweep correctly merge everything — after sorting, any interval
        // that overlaps the "current" merged block MUST appear before we
        // move past it, so we never miss an overlap.
        allBusyIntervals.sort(Comparator.comparingInt(interval -> interval.start));

        // Step 3: Sweep and merge, recording gaps as they appear.
        List<Interval> freeTime = new ArrayList<>();
        // Track only the END of the current merged busy block — we never
        // need its start again, since free time only cares about the gap
        // AFTER this block and BEFORE the next one.
        int currentMergedEnd = allBusyIntervals.get(0).end;

        for (int i = 1; i < allBusyIntervals.size(); i++) {
            Interval nextInterval = allBusyIntervals.get(i);

            if (nextInterval.start > currentMergedEnd) {
                // A genuine gap: nobody is busy in [currentMergedEnd, nextInterval.start).
                // This also automatically satisfies "no zero-length intervals,"
                // since we only get here when start is STRICTLY greater than
                // currentMergedEnd.
                freeTime.add(new Interval(currentMergedEnd, nextInterval.start));
                currentMergedEnd = nextInterval.end;
            } else {
                // Overlaps OR touches (<=) the current block — using <= here
                // (by falling into this else branch whenever start <= end)
                // is what correctly treats touching intervals like [1,3] and
                // [3,5] as ONE continuous busy block with no gap at 3.
                currentMergedEnd = Math.max(currentMergedEnd, nextInterval.end);
            }
        }

        // Note on "finite" requirement: we never emit anything before the
        // first interval's start or after the last currentMergedEnd, so
        // unbounded free time on either side is naturally excluded — we
        // simply never construct those intervals in the first place.
        return freeTime;
    }


    /*
    ============================================================================
    SECTION 10 — DRY RUN / TRACE
    ============================================================================
    Using Example 3 from Section 3:
        Employee A: [1,3],[6,7]
        Employee B: [2,4]
        Employee C: [3,4],[7,8]

    Step 1 — Flatten:
        allBusyIntervals = [ [1,3], [6,7], [2,4], [3,4], [7,8] ]

    Step 2 — Sort by start:
        allBusyIntervals = [ [1,3], [2,4], [3,4], [6,7], [7,8] ]

    Step 3 — Sweep, currentMergedEnd initialized to allBusyIntervals[0].end = 3:

        i=1, nextInterval=[2,4]:
            nextInterval.start (2) > currentMergedEnd (3)?  NO (2 <= 3)
            → merge: currentMergedEnd = max(3, 4) = 4
            state: currentMergedEnd=4, freeTime=[]

        i=2, nextInterval=[3,4]:
            3 > 4?  NO
            → merge: currentMergedEnd = max(4, 4) = 4
            state: currentMergedEnd=4, freeTime=[]

        i=3, nextInterval=[6,7]:
            6 > 4?  YES → gap!
            → freeTime.add([4, 6])
            → currentMergedEnd = 7
            state: currentMergedEnd=7, freeTime=[[4,6]]

        i=4, nextInterval=[7,8]:
            7 > 7?  NO (touches, not strictly greater)
            → merge: currentMergedEnd = max(7, 8) = 8
            state: currentMergedEnd=8, freeTime=[[4,6]]

    Loop ends. Return freeTime = [[4,6]]

    Matches the expected output derived by hand in Section 3. ✔
    */


    /*
    ============================================================================
    SECTION 11 — CLOSING SUMMARY
    ============================================================================
    * All 5 approaches are functionally correct (verified below via
      randomized cross-validation against the brute-force oracle).
    * The delivered answer is Approach 2 (Sort + Merge): O(N log N) time,
      O(N) space, minimal code surface, and the pattern interviewers expect
      for this exact problem class.
    * Approaches 3 and 5 offer O(N log K) instead of O(N log N), which only
      matters when K (employee count) is much smaller than N (total
      intervals) — true in THIS problem's constraints (K<=50, N<=2500) but
      not enough to outweigh their extra code complexity and constant
      factors at this scale.
    * Known limitations / assumptions of the final solution:
        - Assumes each employee's own interval list is pre-sorted and
          internally non-overlapping (guaranteed by the problem; not
          re-validated).
        - Uses `int` for start/end, matching the stated bound of 1e8 (fits
          comfortably; would need `long` if bounds were relaxed toward
          2^31).
        - Mutates nothing from the input — all Interval objects in the
          output are newly constructed, so the caller's original schedule is
          left untouched (important in production code / when the input may
          be reused elsewhere).
    */


    /*
    ============================================================================
    SECTION 12 — FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
    ============================================================================
    1. "What if employee schedules change dynamically (adds/removals) and
        you need to answer free-time queries repeatedly?"
        → Move to a self-balancing interval structure (e.g., an order-
          statistics/interval-augmented balanced BST, or a segment tree over
          coordinate-compressed time) so each update/query is O(log N)
          instead of recomputing from scratch.

    2. "What if you only need to know whether there's ANY common free slot
        of at least duration D, not all of them?"
        → Same sweep, but short-circuit and return true the first time a gap
          >= D is found — O(N log N) worst case, often faster in practice.
          Could also binary search on D if asked "what's the maximum D such
          that a free slot of length D exists?" against precomputed gaps.

    3. "How would this change if you had to find free time for just a SUBSET
        of employees (e.g., only 5 of the 50), chosen at query time?"
        → With repeated arbitrary subset queries, precomputing a single
          global merge doesn't directly help; you'd likely merge on-the-fly
          per query (Approach 2 restricted to the subset) — still O(n_subset
          log n_subset) per query, or maintain a heap/segment-tree-per-
          employee structure enabling faster K-way merges (Approach 3).

    4. "Can you parallelize this across multiple machines if N were huge
        (e.g., billions of intervals across thousands of employees)?"
        → Yes — this is exactly what Approach 5 (Divide and Conquer) sets up
          nicely: partition employees across machines, compute each
          partition's merged busy intervals independently (map phase), then
          merge the sorted partial results pairwise (reduce phase), similar
          to a distributed merge sort / external sort.

    5. "What if intervals could have negative or floating-point times, or
        the equality/touching semantics were reversed (touching does NOT
        count as continuous)?"
        → Floating point: works the same, just watch for epsilon comparison
          issues instead of exact equality. Reversed touching semantics:
          flip the merge condition from `start > currentMergedEnd` /
          `start <= currentMergedEnd` to strict `<` everywhere, which would
          then legitimately allow zero-length "touch point" gaps like [3,3]
          unless explicitly filtered.

    6. "How would you test this solution?"
        → Exactly as done in this file: unit/named-assertion cases covering
          normal, edge (single interval overall → no free time), and
          boundary/touching cases, PLUS randomized cross-validation of every
          approach against the brute-force oracle across many random
          schedules and seeds.
    */


    /*
    ============================================================================
    SECTION 13 — WHAT CANDIDATES TYPICALLY MISS
    ============================================================================
    1. Using strict `<` instead of `<=` when deciding whether to merge vs.
       start a new interval. This is THE classic bug here: touching
       intervals like [1,3] and [3,5] must be merged (no gap at 3), but with
       a strict `<` comparison many candidates incorrectly emit a bogus
       zero-length interval [3,3] — which the problem explicitly forbids.

    2. Forgetting to take the MAX when extending the merged interval's end.
       If the next overlapping interval's end is actually SMALLER than the
       current merged end (e.g., current block is [1,10] and next is [2,3],
       fully nested inside), naively overwriting `currentMergedEnd =
       nextInterval.end` would incorrectly shrink the merged block to 3,
       causing a phantom free interval to appear right after where real busy
       time continues to 10.

    3. Reporting free time BEFORE the first interval's start or AFTER the
       last interval's end. The problem explicitly asks for FINITE
       intervals only — many candidates' first instinct is to also report
       "unbounded" free time on the edges, which is wrong here.

    4. Forgetting to sort before sweeping (or sorting by the wrong key —
       e.g., sorting by end instead of start), which silently breaks the
       core invariant that makes a single linear pass sufficient. This
       typically doesn't crash, it just produces a subtly wrong (usually
       too-short or duplicated) list of gaps that's easy to miss without a
       stress test against a brute-force oracle.
    */


    /*
    ============================================================================
    BONUS — VERIFICATION: NAMED ASSERTIONS + RANDOMIZED STRESS TEST
    ============================================================================
    Following the same "brute force as correctness oracle" discipline used
    throughout this problem set: every optimized approach is cross-validated
    against Approach 1 (bruteForceApproach) across many randomized schedules.
    */
    public static void main(String[] args) {
        runNamedAssertions();
        runRandomizedStressTest(3000, 42L);
        System.out.println("ALL TESTS PASSED across all 5 approaches.");
    }

    private static void runNamedAssertions() {
        // --- Named case: normalCaseThreeEmployeesOneGap (Example 1) ---
        List<List<Interval>> example1 = List.of(
            List.of(new Interval(1, 2), new Interval(5, 6)),
            List.of(new Interval(1, 3)),
            List.of(new Interval(4, 10))
        );
        assertEqualsIntervals("normalCaseThreeEmployeesOneGap",
            List.of(new Interval(3, 4)), findEmployeeFreeTime(example1));

        // --- Named case: edgeCaseSingleEmployeeNoFreeTime (Example 2) ---
        List<List<Interval>> example2 = List.of(
            List.of(new Interval(5, 8))
        );
        assertEqualsIntervals("edgeCaseSingleEmployeeNoFreeTime",
            List.of(), findEmployeeFreeTime(example2));

        // --- Named case: boundaryTouchingIntervalsNoZeroLengthGap (Example 3) ---
        List<List<Interval>> example3 = List.of(
            List.of(new Interval(1, 3), new Interval(6, 7)),
            List.of(new Interval(2, 4)),
            List.of(new Interval(3, 4), new Interval(7, 8))
        );
        assertEqualsIntervals("boundaryTouchingIntervalsNoZeroLengthGap",
            List.of(new Interval(4, 6)), findEmployeeFreeTime(example3));

        // --- Named case: fullyNestedIntervalMustNotShrinkMergedEnd ---
        // Regression test for candidate mistake #2 above.
        List<List<Interval>> nestedCase = List.of(
            List.of(new Interval(1, 10)),
            List.of(new Interval(2, 3)),
            List.of(new Interval(12, 15))
        );
        assertEqualsIntervals("fullyNestedIntervalMustNotShrinkMergedEnd",
            List.of(new Interval(10, 12)), findEmployeeFreeTime(nestedCase));

        // --- Named case: duplicateIntervalsAcrossEmployees ---
        List<List<Interval>> duplicatesCase = List.of(
            List.of(new Interval(1, 3)),
            List.of(new Interval(1, 3)),
            List.of(new Interval(5, 6))
        );
        assertEqualsIntervals("duplicateIntervalsAcrossEmployees",
            List.of(new Interval(3, 5)), findEmployeeFreeTime(duplicatesCase));

        System.out.println("Named assertions: PASSED");
    }

    private static void assertEqualsIntervals(String caseName, List<Interval> expected, List<Interval> actual) {
        if (!intervalListsEqual(expected, actual)) {
            throw new AssertionError("Named test failed [" + caseName + "]: expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean intervalListsEqual(List<Interval> a, List<Interval> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).start != b.get(i).start || a.get(i).end != b.get(i).end) return false;
        }
        return true;
    }

    private static void runRandomizedStressTest(int trials, long seed) {
        Random random = new Random(seed);
        for (int trial = 0; trial < trials; trial++) {
            int employeeCount = 1 + random.nextInt(6);        // small so brute force stays fast
            int maxTimePoint = 20;                              // small domain → lots of overlap/touch cases
            List<List<Interval>> schedule = new ArrayList<>();

            for (int e = 0; e < employeeCount; e++) {
                int intervalCount = 1 + random.nextInt(4);
                TreeSet<Integer> pointSet = new TreeSet<>();
                while (pointSet.size() < intervalCount * 2) {
                    pointSet.add(random.nextInt(maxTimePoint));
                }
                List<Integer> points = new ArrayList<>(pointSet);
                List<Interval> employeeIntervals = new ArrayList<>();
                for (int i = 0; i + 1 < points.size(); i += 2) {
                    int start = points.get(i);
                    int end = points.get(i + 1);
                    if (start < end) {
                        employeeIntervals.add(new Interval(start, end));
                    }
                }
                if (employeeIntervals.isEmpty()) {
                    employeeIntervals.add(new Interval(0, 1)); // ensure non-empty per constraints
                }
                schedule.add(employeeIntervals);
            }

            List<Interval> expected = bruteForceApproach(schedule);
            List<Interval> resultSortMerge = sortAndMergeApproach(schedule);
            List<Interval> resultHeap = minHeapKWayMergeApproach(schedule);
            List<Interval> resultTwoPointer = twoPointerPairwiseMergeApproach(schedule);
            List<Interval> resultDivideConquer = divideAndConquerApproach(schedule);
            List<Interval> resultProduction = findEmployeeFreeTime(schedule);

            if (!intervalListsEqual(expected, resultSortMerge)) {
                throw new AssertionError("Mismatch (sortAndMerge) trial " + trial
                    + " schedule=" + schedule + " expected=" + expected + " actual=" + resultSortMerge);
            }
            if (!intervalListsEqual(expected, resultHeap)) {
                throw new AssertionError("Mismatch (minHeap) trial " + trial
                    + " schedule=" + schedule + " expected=" + expected + " actual=" + resultHeap);
            }
            if (!intervalListsEqual(expected, resultTwoPointer)) {
                throw new AssertionError("Mismatch (twoPointer) trial " + trial
                    + " schedule=" + schedule + " expected=" + expected + " actual=" + resultTwoPointer);
            }
            if (!intervalListsEqual(expected, resultDivideConquer)) {
                throw new AssertionError("Mismatch (divideAndConquer) trial " + trial
                    + " schedule=" + schedule + " expected=" + expected + " actual=" + resultDivideConquer);
            }
            if (!intervalListsEqual(expected, resultProduction)) {
                throw new AssertionError("Mismatch (production findEmployeeFreeTime) trial " + trial
                    + " schedule=" + schedule + " expected=" + expected + " actual=" + resultProduction);
            }
        }
        System.out.println("Randomized stress test (" + trials + " trials): PASSED across all 5 approaches");
    }
}
