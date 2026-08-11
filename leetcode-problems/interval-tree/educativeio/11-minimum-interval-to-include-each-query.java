import java.util.*;

/*
 * ============================================================================
 *  GOOGLE-STYLE MOCK ONSITE INTERVIEW
 *  Problem: Minimum Size Interval Containing a Query Value
 *  (LeetCode 1851 family — "Minimum Interval to Include Each Query")
 *
 *  Language target: Java 21+ syntax (compiles cleanly on Java 21, 24+).
 *  This single file walks through the FULL interview lifecycle end to end.
 * ============================================================================
 */
class MinimumIntervalToIncludeQuery {

    /*
     * ========================================================================
     * SECTION 1 — RESTATE THE PROBLEM
     * ========================================================================
     * In my own words:
     *
     *   I'm given a list of closed integer intervals [left_i, right_i], where
     *   each interval "covers" every integer from left_i to right_i inclusive.
     *   The "size" of an interval is (right_i - left_i + 1) — literally how
     *   many integers it covers.
     *
     *   I'm also given a list of query values. For every query value q, I need
     *   to find, among all intervals that contain q (i.e. left_i <= q <=
     *   right_i), the one with the SMALLEST size, and report that size. If no
     *   interval contains q, I report -1 for that query.
     *
     *   Output: an array `answer` of the same length as `queries`, where
     *   answer[j] corresponds to queries[j].
     *
     * Key constraints (given):
     *   - 1 <= intervals.length <= 1e5
     *   - 1 <= queries.length   <= 1e5
     *   - intervals[i].length == 2
     *   - 1 <= left_i <= right_i <= 1e7
     *   - 1 <= queries[j] <= 1e7
     *
     * Implicit takeaways from constraints:
     *   - Both n (intervals) and m (queries) can be up to 1e5, so anything
     *     O(n * m) (1e10 operations) will almost certainly TLE. I need an
     *     approach close to O((n + m) log n).
     *   - Values fit comfortably in `int` (up to 1e7), no overflow concerns
     *     for size (max ~1e7), though sums across many intervals could need
     *     `long` if I were aggregating sizes (I'm not, so `int` is fine).
     *
     * Assumptions I'll explicitly state and confirm in clarifying questions:
     *   - Intervals may overlap arbitrarily and may be given in any order.
     *   - Query order in the output must mirror the input query order.
     *   - Answer for each query is independent (no interaction between
     *     queries).
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 2 — CLARIFYING QUESTIONS (asked to interviewer, with assumed
     * answers I would proceed with if the interviewer says "your call")
     * ========================================================================
     * 1. Q: Can intervals overlap, be nested, or be identical to each other?
     *    A: Yes — assume arbitrary overlap, duplicates, and containment are
     *       all possible; no dedup is guaranteed.
     *
     * 2. Q: Are intervals guaranteed to be sorted by left or right endpoint?
     *    A: No — assume unsorted input; I will sort as part of my algorithm.
     *
     * 3. Q: Can queries repeat, and must repeated queries return the same
     *       answer (obviously yes logically, but should I dedupe/cache)?
     *    A: Queries can repeat. I don't need to dedupe for correctness, but
     *       I may optionally cache results per distinct value as an
     *       optimization.
     *
     * 4. Q: What should be returned when zero intervals contain a query?
     *    A: -1, as stated in the problem.
     *
     * 5. Q: Is the output required to preserve the original order of
     *       `queries`, or can it be grouped/sorted?
     *    A: Must preserve original order — answer[j] maps to queries[j].
     *
     * 6. Q: Are we optimizing for a single batch of queries (as given), or
     *       should I design for an online/streaming scenario where queries
     *       arrive one at a time after intervals are fixed?
     *    A: For this interview, assume the batch/offline scenario (all
     *       queries known up front) — that's what unlocks the optimal
     *       sweep-line + heap technique. I'll mention the online variant
     *       (segment tree / interval tree) as a follow-up extension.
     *
     * 7. Q: Can interval or query values be negative, or is 1 <= value <= 1e7
     *       strictly enforced (per constraints)?
     *    A: Per constraints, all values are positive and bounded by 1e7 — no
     *       negative numbers to handle. I will still defensively validate.
     *
     * 8. Q: Is thread-safety / concurrent access to intervals or queries a
     *       concern (e.g., is this feeding a production service)?
     *    A: Not for this problem — single-threaded, single-batch computation
     *       is sufficient. I'll note thread-safety as an extension question.
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 3 — EXAMPLES & EDGE CASES
     * ========================================================================
     * Example 1 (normal case):
     *   intervals = [[1,4],[2,4],[3,6],[4,4]]
     *   queries   = [2,3,4,5]
     *   sizes     = [1,4]->4, [2,4]->3, [3,6]->4, [4,4]->1
     *   query 2 -> contained in [1,4](4), [2,4](3)              -> min 3
     *   query 3 -> contained in [1,4](4), [2,4](3), [3,6](4)    -> min 3
     *   query 4 -> contained in all four intervals              -> min 1
     *   query 5 -> contained only in [3,6](4)                   -> min 4
     *   expected answer = [3, 3, 1, 4]
     *
     * Example 2 (edge case — no interval covers the query):
     *   intervals = [[2,3],[2,5],[1,8],[20,25]]
     *   queries   = [2,19,5,22]
     *   query 19 is not covered by [2,3],[2,5],[1,8] (all end <=8) nor
     *   [20,25] (starts at 20) -> answer = -1 for that query.
     *
     * Example 3 (boundary / tie-breaking case):
     *   intervals = [[1,1000000],[500000,500000]]
     *   queries   = [500000]
     *   Two intervals both contain 500000: size 1,000,000 and size 1.
     *   Boundary at the value extremes (1 and 1e7) still must work, and the
     *   tie between "which interval is smallest" is resolved simply by
     *   comparing sizes — [500000,500000] wins with size 1. This also stress
     *   tests the upper bound of the value range (1e7) and a single-point
     *   interval (left == right).
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 4, 5, 6 — ALL POSSIBLE APPROACHES
     * ========================================================================
     * Paradigms considered but NOT applicable (one-line reasons):
     *   - Two-pointer / sliding window (classic sense): queries and intervals
     *     don't form one contiguous, monotonically growing/shrinking window
     *     over a single array; containment isn't a "window sum/count"
     *     property, so a simple two-pointer window doesn't directly apply.
     *   - Divide & conquer: no natural way to split intervals/queries in half
     *     such that subproblem answers combine in less than the cost of just
     *     solving directly — no recursive structure to exploit.
     *   - Dynamic programming: no overlapping subproblems / optimal
     *     substructure — each query's answer is independent, there is no
     *     sequential decision process to memoize.
     *   - Graph traversal (BFS/DFS): the data isn't naturally a graph (no
     *     nodes/edges to traverse); modeling it as one would be artificial.
     *   - Trie: values are just integers on a line, not strings/bit-prefixes
     *     with meaningful shared-prefix structure.
     *   - Monotonic stack/deque: containment isn't a "next greater/smaller
     *     element" style problem; no monotonic invariant naturally emerges
     *     from a single linear scan.
     *   - Binary search ALONE (as a full solution): binary search is used as
     *     a *subroutine* inside the sweep-line approaches below, but on its
     *     own it can't resolve "smallest interval among many overlapping
     *     candidates" — it needs to be paired with a heap/DSU/segment tree.
     * ========================================================================
     */

    /* ------------------------------------------------------------------ *
     * Approach 1: Brute Force (Nested Loop)
     * ------------------------------------------------------------------
     * Idea: For every query, scan every interval, check containment, and
     * track the minimum size among matches.
     *
     * Data structure / paradigm: none — pure nested iteration.
     *
     * Time Complexity: O(n * m) — for each of m queries, scan all n
     * intervals. With n, m up to 1e5, worst case is ~1e10 comparisons.
     * Space Complexity: O(1) extra (excluding the output array).
     *
     * Pros:
     *   - Trivial to write correctly under interview pressure; great as a
     *     warm-up / correctness baseline to validate faster solutions
     *     against.
     *   - No preprocessing, works on unsorted, streaming-friendly input.
     * Cons:
     *   - Far too slow for the given constraints (guaranteed TLE at 1e5 x
     *     1e5).
     * When to use: only for tiny inputs (n, m <= ~1000), as a correctness
     * oracle in tests, or as a first "let me get something working" step
     * before optimizing live in the interview.
     * ------------------------------------------------------------------ */
    static int[] bruteForce(int[][] intervals, int[] queries) {
        int[] answer = new int[queries.length];
        for (int queryIndex = 0; queryIndex < queries.length; queryIndex++) {
            int queryValue = queries[queryIndex];
            int bestSize = -1;
            for (int[] interval : intervals) {
                int left = interval[0];
                int right = interval[1];
                if (left <= queryValue && queryValue <= right) {
                    int size = right - left + 1;
                    if (bestSize == -1 || size < bestSize) {
                        bestSize = size;
                    }
                }
            }
            answer[queryIndex] = bestSize;
        }
        return answer;
    }

    /* ------------------------------------------------------------------ *
     * Approach 2: Offline Sweep Line + Min-Heap (Sorting + Priority Queue)
     *  *** This is the RECOMMENDED / OPTIMAL approach — see Section 8 ***
     * ------------------------------------------------------------------
     * Idea:
     *   1. Sort intervals by their left endpoint ascending.
     *   2. Sort queries ascending by value, but remember each query's
     *      original index so we can place the answer back correctly.
     *   3. Sweep query values from smallest to largest. As the sweep value
     *      grows, push every interval whose left endpoint is now <= the
     *      current query value into a min-heap keyed by interval SIZE
     *      (tie-broken by right endpoint, though size alone suffices).
     *   4. Before answering, pop any heap-top intervals whose right
     *      endpoint is already < the current query value — those intervals
     *      can never satisfy this or any future (larger) query, since the
     *      sweep value only increases. This keeps the heap "alive" and
     *      bounded.
     *   5. The heap top (if any remain) is the smallest interval that
     *      currently contains the query.
     *
     * Data structure / paradigm: sorting + min-heap (priority queue) +
     * two-pointer sweep over sorted intervals.
     *
     * Time Complexity: O((n + m) log n)
     *   - Sorting intervals: O(n log n). Sorting queries: O(m log m).
     *   - Each interval is pushed onto the heap at most once: O(n log n).
     *   - Each interval is popped from the heap at most once across the
     *     entire sweep (not once per query!): O(n log n) total, amortized.
     *   - Each query does O(1) amortized heap-top peeks plus the pops
     *     already accounted for above.
     * Space Complexity: O(n + m) — heap holds up to n intervals, plus
     * arrays for sorted order and index remapping.
     *
     * Pros:
     *   - Meets the required complexity bound comfortably.
     *   - Conceptually clean "sweep + heap" pattern that's extremely common
     *     in interviews (reusable for many interval problems).
     *   - Straightforward to prove correct: monotonic sweep value means
     *     once an interval's right end is behind us, it's dead forever.
     * Cons:
     *   - Requires careful index bookkeeping to map sorted-query answers
     *     back to original query order — a common source of bugs.
     *   - Heap operations carry a real (log n) constant factor versus,
     *     say, a Union-Find approach.
     * When to use: this is my default choice for "batch of queries against
     * a batch of intervals" problems — general purpose, easy to explain,
     * and well within time limits.
     * ------------------------------------------------------------------ */
    static int[] heapSweep(int[][] intervals, int[] queries) {
        int intervalCount = intervals.length;
        int queryCount = queries.length;

        // Sort a *copy* of intervals by left endpoint ascending.
        int[][] sortedIntervals = intervals.clone();
        Arrays.sort(sortedIntervals, Comparator.comparingInt(interval -> interval[0]));

        // Sort query indices by query value ascending, keeping original index.
        Integer[] queryOrder = new Integer[queryCount];
        for (int i = 0; i < queryCount; i++) queryOrder[i] = i;
        Arrays.sort(queryOrder, Comparator.comparingInt(index -> queries[index]));

        // Min-heap ordered by interval size; store {size, rightEndpoint}.
        PriorityQueue<int[]> minHeapBySize =
                new PriorityQueue<>(Comparator.comparingInt(entry -> entry[0]));

        int[] answer = new int[queryCount];
        int intervalPointer = 0;

        for (int orderedQueryIndex : queryOrder) {
            int queryValue = queries[orderedQueryIndex];

            // Push all intervals whose left endpoint has come into range.
            while (intervalPointer < intervalCount
                    && sortedIntervals[intervalPointer][0] <= queryValue) {
                int left = sortedIntervals[intervalPointer][0];
                int right = sortedIntervals[intervalPointer][1];
                int size = right - left + 1;
                minHeapBySize.offer(new int[] {size, right});
                intervalPointer++;
            }

            // Discard intervals that have already ended before this query.
            while (!minHeapBySize.isEmpty() && minHeapBySize.peek()[1] < queryValue) {
                minHeapBySize.poll();
            }

            answer[orderedQueryIndex] = minHeapBySize.isEmpty() ? -1 : minHeapBySize.peek()[0];
        }

        return answer;
    }

    /* ------------------------------------------------------------------ *
     * Approach 3: Sort by Size + Greedy Assignment via Union-Find (DSU)
     * ------------------------------------------------------------------
     * Idea:
     *   Process intervals in ascending order of SIZE (smallest first).
     *   For each interval, we want to assign its size to every query point
     *   that falls inside it AND hasn't already been assigned an answer
     *   (because a smaller interval, processed earlier, already "claimed"
     *   it — and smaller is always better, so never overwrite).
     *
     *   To efficiently "jump" from one unassigned query point to the next
     *   unassigned query point within an interval's range (skipping
     *   already-claimed ones in near O(1)), we use a Disjoint Set Union
     *   structure over the SORTED, DEDUPED query values: find(i) returns
     *   the smallest index >= i that is still unclaimed. Once claimed, we
     *   union it forward to i + 1.
     *
     * Data structure / paradigm: greedy + Union-Find (Disjoint Set Union)
     * with path compression, over coordinate-compressed query values.
     *
     * Time Complexity: O((n + m) log(n + m))
     *   - Sorting intervals by size: O(n log n).
     *   - Sorting/deduping queries: O(m log m).
     *   - Each query point is "claimed" and unioned forward exactly once
     *     across the whole run, and each DSU find is near O(1) amortized
     *     (inverse-Ackermann with path compression) — so the sweep itself
     *     is O((n + m) alpha(n + m)), dominated by the sorting cost.
     * Space Complexity: O(n + m) for DSU parent array, sorted arrays, and
     * coordinate maps.
     *
     * Pros:
     *   - Avoids heap overhead entirely; DSU find/union are extremely fast
     *     in practice.
     *   - Elegant "greedy claim, never revisit" correctness argument:
     *     because we process smallest intervals first, the first interval
     *     to reach a query point is provably its optimal (minimum-size)
     *     answer.
     * Cons:
     *   - Requires coordinate compression bookkeeping and careful DSU
     *     index arithmetic (off-by-one prone: indices vs. values).
     *   - Less immediately intuitive to an interviewer than the heap sweep;
     *     needs a clear verbal correctness argument.
     * When to use: strong alternative when you want to avoid heap constant
     * factors, or you're already comfortable with DSU "next free slot"
     * patterns (same trick used in "first available room" style problems).
     * ------------------------------------------------------------------ */
    static int[] unionFindGreedy(int[][] intervals, int[] queries) {
        int queryCount = queries.length;

        // Coordinate-compress distinct query values, sorted ascending.
        int[] sortedUniqueQueries = Arrays.stream(queries).distinct().sorted().toArray();
        int distinctCount = sortedUniqueQueries.length;

        // DSU parent array: parent[i] points to the next unclaimed slot.
        // Size distinctCount + 1 so "claim the last slot" can union to a
        // sentinel out-of-range index (self-terminating).
        int[] parent = new int[distinctCount + 1];
        for (int i = 0; i <= distinctCount; i++) parent[i] = i;

        int[] answerAtCompressedIndex = new int[distinctCount];
        Arrays.fill(answerAtCompressedIndex, -1);

        // Process intervals smallest-size first.
        int[][] sortedBySize = intervals.clone();
        Arrays.sort(sortedBySize, Comparator.comparingInt(interval -> interval[1] - interval[0]));

        for (int[] interval : sortedBySize) {
            int left = interval[0];
            int right = interval[1];
            int size = right - left + 1;

            // First compressed index whose value is >= left.
            int startIndex = lowerBound(sortedUniqueQueries, left);
            int currentIndex = find(parent, startIndex);

            while (currentIndex < distinctCount && sortedUniqueQueries[currentIndex] <= right) {
                answerAtCompressedIndex[currentIndex] = size; // first (smallest) claim wins
                union(parent, currentIndex, currentIndex + 1);
                currentIndex = find(parent, currentIndex + 1);
            }
        }

        int[] answer = new int[queryCount];
        for (int i = 0; i < queryCount; i++) {
            int compressedIndex = lowerBound(sortedUniqueQueries, queries[i]);
            answer[i] = answerAtCompressedIndex[compressedIndex];
        }
        return answer;
    }

    // DSU find with path compression: returns the smallest unclaimed index >= x.
    private static int find(int[] parent, int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]]; // path halving
            x = parent[x];
        }
        return x;
    }

    // DSU union: mark index `claimed` as used by pointing it to `next`.
    private static void union(int[] parent, int claimed, int next) {
        parent[claimed] = next;
    }

    // Returns index of first element >= target in a sorted array (binary search).
    private static int lowerBound(int[] sortedArray, int target) {
        int lo = 0, hi = sortedArray.length;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (sortedArray[mid] < target) lo = mid + 1; else hi = mid;
        }
        return lo;
    }

    /* ------------------------------------------------------------------ *
     * Approach 4: Coordinate Compression + Segment Tree with Pruned
     *             Range "Assign-If-Empty" Updates
     * ------------------------------------------------------------------
     * Idea:
     *   Compress all distinct query values into leaves of a segment tree
     *   (indices 0..distinctCount-1). Process intervals in ascending order
     *   of size. For each interval, perform a range update over the
     *   compressed index range covered by [left, right]: "assign this size
     *   to every leaf that doesn't already have a value." Each internal
     *   node tracks whether its entire subtree is already fully assigned;
     *   if so, we prune that branch immediately instead of descending —
     *   this keeps total work bounded even though a naive range-assign
     *   would be O(n * range).
     *
     * Data structure / paradigm: segment tree (advanced structure) with a
     * "beats"-style pruning flag, over compressed coordinates.
     *
     * Time Complexity: O((n + m) log m)
     *   - Building/compressing coordinates: O(m log m).
     *   - Each interval performs a range update; thanks to the "fully
     *     assigned" pruning, each leaf is visited for a *successful* write
     *     exactly once (O(m) total amortized writes), and each update call
     *     additionally costs O(log m) for the tree descent structure even
     *     when pruned early — giving O(n log m + m log m) overall.
     *   - Point queries for each of the m queries: O(log m) each, O(m log m)
     *     total (or O(1) if we flatten straight into a leaf-value array,
     *     which is what the implementation below does).
     * Space Complexity: O(m) for the segment tree arrays and coordinate
     * maps.
     *
     * Pros:
     *   - Generalizes well: if this were a *series* of problems (e.g. the
     *     interval set changes over time, or we need range queries later),
     *     the segment tree scaffolding is directly reusable/extensible.
     *   - Very clear "range update, point query" mental model.
     * Cons:
     *   - Most code to write and the easiest to introduce subtle bugs in
     *     under interview time pressure (tree indices, recursion bounds,
     *     compression mapping).
     *   - For this specific one-shot problem, it's strictly more machinery
     *     than the heap or DSU approaches for no asymptotic benefit —
     *     overkill unless the interviewer explicitly wants to see advanced
     *     data structure design.
     * When to use: when the interviewer asks a follow-up like "what if
     * intervals could be added/removed dynamically and queries interleave
     * with updates?" — the segment tree is the natural extension point.
     * ------------------------------------------------------------------ */
    static int[] segmentTreeApproach(int[][] intervals, int[] queries) {
        int[] sortedUniqueQueries = Arrays.stream(queries).distinct().sorted().toArray();
        int distinctCount = sortedUniqueQueries.length;

        SegmentTree segmentTree = new SegmentTree(distinctCount);

        int[][] sortedBySize = intervals.clone();
        Arrays.sort(sortedBySize, Comparator.comparingInt(interval -> interval[1] - interval[0]));

        for (int[] interval : sortedBySize) {
            int left = interval[0];
            int right = interval[1];
            int size = right - left + 1;

            int startIndex = lowerBound(sortedUniqueQueries, left);
            int endIndexExclusive = lowerBound(sortedUniqueQueries, right + 1); // first index > right
            int endIndexInclusive = endIndexExclusive - 1;

            if (startIndex <= endIndexInclusive) {
                segmentTree.assignIfEmpty(0, 0, distinctCount - 1, startIndex, endIndexInclusive, size);
            }
        }

        int[] answer = new int[queries.length];
        for (int i = 0; i < queries.length; i++) {
            int compressedIndex = lowerBound(sortedUniqueQueries, queries[i]);
            int value = segmentTree.leafValue[compressedIndex];
            answer[i] = (value == 0) ? -1 : value;
        }
        return answer;
    }

    /** Minimal segment tree supporting "assign value to empty leaves in range" + point read. */
    private static final class SegmentTree {
        final int[] leafValue;      // 0 means "unassigned"
        final boolean[] fullyAssigned; // true if every leaf in this node's range is assigned

        SegmentTree(int leafCount) {
            leafValue = new int[Math.max(leafCount, 1)];
            fullyAssigned = new boolean[4 * Math.max(leafCount, 1)];
        }

        void assignIfEmpty(int node, int nodeStart, int nodeEnd,
                            int rangeStart, int rangeEnd, int value) {
            if (fullyAssigned[node] || nodeStart > rangeEnd || nodeEnd < rangeStart) return;

            if (nodeStart == nodeEnd) {
                if (leafValue[nodeStart] == 0) leafValue[nodeStart] = value;
                fullyAssigned[node] = true;
                return;
            }

            int mid = nodeStart + (nodeEnd - nodeStart) / 2;
            assignIfEmpty(2 * node + 1, nodeStart, mid, rangeStart, rangeEnd, value);
            assignIfEmpty(2 * node + 2, mid + 1, nodeEnd, rangeStart, rangeEnd, value);
            fullyAssigned[node] = fullyAssigned[2 * node + 1] && fullyAssigned[2 * node + 2];
        }
    }

    /*
     * ========================================================================
     * SECTION 7 — APPROACHES COMPARISON TABLE
     * ========================================================================
     * Approach                  | Time            | Space  | Best For                         | Limitations
     * --------------------------|-----------------|--------|----------------------------------|-----------------------------------------
     * 1. Brute Force            | O(n*m)          | O(1)   | Tiny n,m; correctness oracle      | TLE at given constraints (1e5 x 1e5)
     * 2. Heap Sweep (RECOMMEND) | O((n+m) log n)  | O(n+m) | General-purpose interview answer  | Heap constant factor; index bookkeeping
     * 3. Union-Find Greedy      | O((n+m) log(n+m))| O(n+m)| Avoiding heap overhead             | DSU index arithmetic is bug-prone
     * 4. Segment Tree           | O((n+m) log m)  | O(m)   | Extensible to dynamic/online queries | Most code; overkill for one-shot batch
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 8 — RECOMMENDED APPROACH FOR THE INTERVIEW
     * ========================================================================
     * I would present Approach 2 (Offline Sweep Line + Min-Heap):
     *
     *   - Clarity: the correctness argument is short and intuitive — sort
     *     queries, sweep left-to-right, maintain "all intervals that could
     *     still be relevant" in a heap keyed by size, and lazily evict dead
     *     intervals. Easy to explain on a whiteboard in under two minutes.
     *   - Coding speed: it's built entirely from standard library pieces
     *     (Arrays.sort, PriorityQueue) — no custom data structure to
     *     implement from scratch, which matters a lot under interview time
     *     pressure.
     *   - Interviewer expectations: this is the canonical accepted solution
     *     pattern for this exact problem class ("offline queries against
     *     overlapping intervals") — interviewers recognize and expect the
     *     sweep + heap technique here.
     *   - Optimality: O((n + m) log n) comfortably fits 1e5-scale inputs
     *     within typical time limits, and is asymptotically tied with the
     *     other advanced approaches, so there's no complexity trade-off to
     *     justify the extra implementation risk of DSU or a segment tree.
     *
     * I'd mention Union-Find and segment tree variants verbally as
     * "here's how I'd push further if needed" — showing range, without
     * spending limited coding time implementing the riskier version first.
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 9 — DEEP DIVE: OPTIMAL SOLUTION (PRODUCTION QUALITY)
     * ========================================================================
     */

    /**
     * Computes, for every query value, the size of the smallest interval
     * that contains it, or -1 if no interval contains it.
     *
     * <p>Algorithm: offline sweep-line over queries sorted ascending, using
     * a min-heap (keyed by interval size) to track all intervals whose left
     * endpoint has come into range and whose right endpoint has not yet
     * been passed by the current query value.
     *
     * @param intervals array of [left, right] pairs, 1 <= left <= right <= 1e7
     * @param queries   array of query values, 1 <= value <= 1e7
     * @return array `answer` where answer[j] is the result for queries[j];
     *         never null, always the same length as {@code queries}
     * @throws IllegalArgumentException if inputs are null, empty, or violate
     *                                   the documented constraints (left > right)
     */
    static int[] minSizeIntervalForEachQuery(int[][] intervals, int[] queries) {
        // --- Defensive validation -------------------------------------------------
        if (intervals == null || intervals.length == 0) {
            throw new IllegalArgumentException("intervals must be non-null and non-empty");
        }
        if (queries == null || queries.length == 0) {
            throw new IllegalArgumentException("queries must be non-null and non-empty");
        }
        for (int[] interval : intervals) {
            if (interval == null || interval.length != 2) {
                throw new IllegalArgumentException("each interval must be a [left, right] pair");
            }
            if (interval[0] > interval[1]) {
                throw new IllegalArgumentException(
                        "left endpoint must be <= right endpoint: " + Arrays.toString(interval));
            }
        }

        int intervalCount = intervals.length;
        int queryCount = queries.length;

        // --- Step 1: sort a defensive copy of intervals by left endpoint ----------
        // We copy so we never mutate the caller's array — a good production habit.
        int[][] intervalsSortedByLeft = intervals.clone();
        Arrays.sort(intervalsSortedByLeft, Comparator.comparingInt(interval -> interval[0]));

        // --- Step 2: sort query INDICES by query value, preserving originals ------
        // We sort indices (not values) so we can write answers back to the
        // correct position in the output array, which must match input order.
        Integer[] queryIndicesSortedByValue = new Integer[queryCount];
        for (int i = 0; i < queryCount; i++) {
            queryIndicesSortedByValue[i] = i;
        }
        Arrays.sort(queryIndicesSortedByValue,
                Comparator.comparingInt(index -> queries[index]));

        // --- Step 3: min-heap of candidate intervals, keyed by size ---------------
        // Each heap entry is {size, rightEndpoint}. We only need the right
        // endpoint (to know when an interval expires) and the size (to know
        // the answer) — the left endpoint is no longer needed once pushed.
        PriorityQueue<int[]> candidatesBySize =
                new PriorityQueue<>(Comparator.comparingInt(entry -> entry[0]));

        int[] answer = new int[queryCount];
        int nextIntervalToConsider = 0; // pointer into intervalsSortedByLeft

        // --- Step 4: sweep queries in ascending order ------------------------------
        for (int sortedPosition = 0; sortedPosition < queryCount; sortedPosition++) {
            int originalQueryIndex = queryIndicesSortedByValue[sortedPosition];
            int currentQueryValue = queries[originalQueryIndex];

            // 4a. Admit every interval whose left endpoint is now <= current query.
            //     Once admitted, an interval stays a "candidate" until its right
            //     endpoint falls behind the sweep value.
            while (nextIntervalToConsider < intervalCount
                    && intervalsSortedByLeft[nextIntervalToConsider][0] <= currentQueryValue) {
                int left = intervalsSortedByLeft[nextIntervalToConsider][0];
                int right = intervalsSortedByLeft[nextIntervalToConsider][1];
                int size = right - left + 1;
                candidatesBySize.offer(new int[] {size, right});
                nextIntervalToConsider++;
            }

            // 4b. Evict intervals that have already expired (right end behind us).
            //     Safe because the sweep value only increases, so an expired
            //     interval can never satisfy any future query either.
            while (!candidatesBySize.isEmpty() && candidatesBySize.peek()[1] < currentQueryValue) {
                candidatesBySize.poll();
            }

            // 4c. The smallest surviving candidate (heap top) is our answer.
            answer[originalQueryIndex] =
                    candidatesBySize.isEmpty() ? -1 : candidatesBySize.peek()[0];
        }

        return answer;
    }

    /*
     * ========================================================================
     * SECTION 10 — DRY RUN / TRACE (using minSizeIntervalForEachQuery)
     * ========================================================================
     * Input:
     *   intervals = [[1,4],[2,4],[3,6],[4,4]]
     *   queries   = [2,3,4,5]
     *
     * Step 1: intervalsSortedByLeft (already sorted by left):
     *   [ [1,4], [2,4], [3,6], [4,4] ]   (sizes: 4, 3, 4, 1)
     *
     * Step 2: queryIndicesSortedByValue -> indices sorted by value:
     *   values by original index: idx0=2, idx1=3, idx2=4, idx3=5
     *   sorted order of indices: [0, 1, 2, 3]  (already ascending here)
     *
     * Step 3: candidatesBySize starts EMPTY. nextIntervalToConsider = 0.
     *
     * --- sortedPosition=0, originalQueryIndex=0, currentQueryValue=2 ---
     *   4a. admit [1,4] (left=1<=2) -> push {4,4}; pointer=1
     *       admit [2,4] (left=2<=2) -> push {3,4}; pointer=2
     *       [3,6] left=3 > 2 -> stop admitting
     *   heap contents (by size): {3,4}, {4,4}
     *   4b. peek={3,4}, right=4, not < 2 -> no eviction
     *   4c. answer[0] = 3   (matches expected)
     *
     * --- sortedPosition=1, originalQueryIndex=1, currentQueryValue=3 ---
     *   4a. admit [3,6] (left=3<=3) -> push {4,6}; pointer=3
     *       [4,4] left=4 > 3 -> stop
     *   heap contents: {3,4}, {4,4}, {4,6}
     *   4b. peek={3,4}, right=4, not < 3 -> no eviction
     *   4c. answer[1] = 3   (matches expected)
     *
     * --- sortedPosition=2, originalQueryIndex=2, currentQueryValue=4 ---
     *   4a. admit [4,4] (left=4<=4) -> push {1,4}; pointer=4 (end of intervals)
     *   heap contents: {1,4}, {3,4}, {4,4}, {4,6}  (heap top is now {1,4})
     *   4b. peek={1,4}, right=4, not < 4 -> no eviction
     *   4c. answer[2] = 1   (matches expected)
     *
     * --- sortedPosition=3, originalQueryIndex=3, currentQueryValue=5 ---
     *   4a. no more intervals to admit (pointer already at 4)
     *   4b. peek={1,4}, right=4 < 5 -> evict; peek={3,4}, right=4 < 5 -> evict;
     *       peek={4,4}, right=4 < 5 -> evict; peek={4,6}, right=6, not < 5 -> stop
     *   heap contents: {4,6}
     *   4c. answer[3] = 4   (matches expected)
     *
     * Final answer (in original query order): [3, 3, 1, 4]  ✔ matches Example 1.
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 11 — CLOSING SUMMARY
     * ========================================================================
     * - Brute force is correct and trivial but scales as O(n*m); useful only
     *   as a baseline/test oracle, not for the given 1e5-scale constraints.
     * - The recommended heap-sweep solution achieves O((n+m) log n) time and
     *   O(n+m) space by exploiting two facts: (1) once queries are sorted,
     *   only intervals with left <= current query are ever relevant, and
     *   (2) once an interval's right end falls behind the sweep, it is dead
     *   forever, so each interval is pushed and popped at most once overall.
     * - Union-Find greedy achieves the same asymptotic class via a different
     *   mechanism (claim-and-skip over sorted-by-size intervals) and can be
     *   marginally faster in practice due to lower constant factors, at the
     *   cost of trickier index bookkeeping.
     * - The segment tree approach is the most extensible (naturally supports
     *   interleaved updates/queries) but is the most code for a one-shot
     *   batch problem — I would only reach for it if a follow-up requires
     *   dynamic interval insertion or online queries.
     * - Known assumptions/limitations of the final solution: assumes all
     *   constraints as given (positive ints, left <= right, up to 1e5/1e7
     *   scale); does not need `long` arithmetic since max size is ~1e7,
     *   comfortably within `int` range; not thread-safe as written (no
     *   shared mutable state is exposed externally, however, so this is
     *   fine for single-call usage).
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 12 — FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     * 1. "What if intervals or queries arrive as a stream and you must
     *     answer each query online, without seeing future queries?"
     *     -> Motivates the segment tree (or an interval tree / balanced BST
     *        keyed by left endpoint with augmented min-size lookups) since
     *        the offline sort-by-query-value trick no longer applies.
     * 2. "What if intervals can be inserted or removed dynamically between
     *     queries?"
     *     -> Segment tree over compressed coordinates (or a balanced BST /
     *        interval tree) supporting insert/delete + range-min queries
     *        becomes necessary; the heap-sweep and DSU approaches assume a
     *        fixed, known-in-advance interval set.
     * 3. "Can you support this at a much larger scale (n, m ~ 1e7 or more),
     *     possibly distributed across machines?"
     *     -> Discuss partitioning intervals/queries by coordinate range
     *        across shards, merging partial results, and whether an
     *        external-sort-based offline pipeline (e.g., MapReduce-style)
     *        could replace the in-memory heap.
     * 4. "What if you need the actual interval (not just its size) that
     *     produced the answer, or the K smallest containing intervals
     *     instead of just the single smallest?"
     *     -> Heap entries would carry interval identity, and for top-K,
     *        we'd need each query to snapshot/copy relevant portions of the
     *        heap rather than just peek — changes the complexity analysis.
     * 5. "How would you handle floating-point or open/half-open intervals
     *     instead of closed integer intervals?"
     *     -> Discuss changing containment checks and size computation
     *        (right - left instead of right - left + 1 for half-open), and
     *        whether coordinate compression via sorting still works
     *        (it does, but tie-breaking on boundaries needs care).
     * 6. "Multiple threads issuing queries concurrently against a shared,
     *     mutable interval set — how would you make this thread-safe?"
     *     -> Discuss read-write locks around the segment tree, or a
     *        copy-on-write immutable snapshot approach for read-heavy
     *        workloads.
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 13 — WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     * 1. Forgetting that the output must match the ORIGINAL query order.
     *    Sorting queries for the sweep is essential for efficiency, but a
     *    very common bug is writing answers into the sorted position
     *    instead of mapping back to the original index — always sort
     *    INDICES, not values, or keep a parallel index array.
     * 2. Off-by-one in size computation: forgetting the "+1" — size is
     *    (right - left + 1), not (right - left), because intervals are
     *    inclusive on both ends. This single mistake silently produces
     *    wrong answers that can still "look plausible" in casual testing.
     * 3. Evicting from the heap using the WRONG condition. It must be
     *    "right endpoint < current query value" (strict less-than) so that
     *    an interval whose right endpoint EQUALS the query value is still
     *    considered a valid, containing interval. Off-by-one here silently
     *    drops valid boundary matches.
     * 4. Assuming intervals arrive sorted, or forgetting to sort them by
     *    LEFT endpoint (not by size or right endpoint) before the sweep —
     *    the admission step specifically depends on left-endpoint order to
     *    correctly determine when an interval "becomes visible" to the
     *    sweep.
     * ========================================================================
     */

    /*
     * ========================================================================
     * TEST HARNESS — validates all four implementations against each other
     * and against hand-verified expected outputs (normal, edge, boundary).
     * ========================================================================
     */
    public static void main(String[] args) {
        List<TestCase> testCases = List.of(
                new TestCase(
                        "Example 1 - normal case",
                        new int[][] {{1, 4}, {2, 4}, {3, 6}, {4, 4}},
                        new int[] {2, 3, 4, 5},
                        new int[] {3, 3, 1, 4}),
                new TestCase(
                        "Example 2 - edge case: some queries unmatched",
                        new int[][] {{2, 3}, {2, 5}, {1, 8}, {20, 25}},
                        new int[] {2, 19, 5, 22},
                        new int[] {2, -1, 4, 6}),
                new TestCase(
                        "Example 3 - boundary case: extreme values + tie candidates",
                        new int[][] {{1, 1_000_000}, {500_000, 500_000}},
                        new int[] {500_000},
                        new int[] {1}),
                new TestCase(
                        "Single interval, single point query, no match",
                        new int[][] {{10, 10}},
                        new int[] {5},
                        new int[] {-1}),
                new TestCase(
                        "All queries fully outside all intervals",
                        new int[][] {{100, 200}, {300, 400}},
                        new int[] {1, 250, 500},
                        new int[] {-1, -1, -1}),
                new TestCase(
                        "Maximum boundary values (left=right=1e7)",
                        new int[][] {{10_000_000, 10_000_000}, {1, 10_000_000}},
                        new int[] {10_000_000, 1},
                        new int[] {1, 10_000_000})
        );

        boolean allPassed = true;
        for (TestCase testCase : testCases) {
            int[] bruteForceResult = bruteForce(testCase.intervals, testCase.queries);
            int[] heapSweepResult = heapSweep(testCase.intervals, testCase.queries);
            int[] unionFindResult = unionFindGreedy(testCase.intervals, testCase.queries);
            int[] segmentTreeResult = segmentTreeApproach(testCase.intervals, testCase.queries);
            int[] optimalResult = minSizeIntervalForEachQuery(testCase.intervals, testCase.queries);

            boolean pass = Arrays.equals(bruteForceResult, testCase.expected)
                    && Arrays.equals(heapSweepResult, testCase.expected)
                    && Arrays.equals(unionFindResult, testCase.expected)
                    && Arrays.equals(segmentTreeResult, testCase.expected)
                    && Arrays.equals(optimalResult, testCase.expected);

            allPassed &= pass;
            System.out.println((pass ? "PASS" : "FAIL") + " - " + testCase.name);
            if (!pass) {
                System.out.println("  expected:    " + Arrays.toString(testCase.expected));
                System.out.println("  bruteForce:  " + Arrays.toString(bruteForceResult));
                System.out.println("  heapSweep:   " + Arrays.toString(heapSweepResult));
                System.out.println("  unionFind:   " + Arrays.toString(unionFindResult));
                System.out.println("  segmentTree: " + Arrays.toString(segmentTreeResult));
                System.out.println("  optimal:     " + Arrays.toString(optimalResult));
            }
        }

        System.out.println();
        System.out.println(allPassed ? "ALL TEST CASES PASSED" : "SOME TEST CASES FAILED");
    }

    /** Simple immutable holder for a test case (Java 21 record). */
    private record TestCase(String name, int[][] intervals, int[] queries, int[] expected) {}
}


/**
 * Problem: Minimum Interval to Include Each Query
 *
 * Key Idea:
 * For each query, find the smallest interval that covers it.
 *
 * This file contains:
 * 1. Brute Force (for understanding)
 * 2. Optimized using Sorting + Min Heap (Interview Optimal)
 *
 * -------------------------------------------------------
 * THINKING PROCESS (IMPORTANT FOR INTERVIEWS):
 * -------------------------------------------------------
 * 1. Brute force → check every interval for every query
 * 2. Too slow → need faster lookup
 * 3. Sort queries and intervals
 * 4. Sweep line + heap to maintain valid intervals
 *
 * WHY HEAP?
 * We want the SMALLEST interval size → Min Heap
 */
class MinimumIntervalQuery {

    // =====================================================
    // 1. BRUTE FORCE SOLUTION
    // =====================================================
    /**
     * Idea:
     * For each query:
     *   - Check all intervals
     *   - If interval contains query → compute size
     *   - Take minimum
     *
     * Time Complexity: O(N * Q)
     * Space Complexity: O(1)
     *
     * Why bad?
     * N = 1e5, Q = 1e5 → 1e10 operations ❌
     */
    public static int[] bruteForce(int[][] intervals, int[] queries) {
        int[] result = new int[queries.length];

        for (int i = 0; i < queries.length; i++) {
            int q = queries[i];
            int minSize = Integer.MAX_VALUE;

            for (int[] interval : intervals) {
                int left = interval[0];
                int right = interval[1];

                // Check if query is inside interval
                if (left <= q && q <= right) {
                    int size = right - left + 1;
                    minSize = Math.min(minSize, size);
                }
            }

            result[i] = (minSize == Integer.MAX_VALUE) ? -1 : minSize;
        }

        return result;
    }

    // =====================================================
    // 2. OPTIMAL SOLUTION (SORT + MIN HEAP)
    // =====================================================
    /**
     * 🔥 CORE IDEA:
     *
     * 1. Sort intervals by start
     * 2. Sort queries
     * 3. Sweep queries from left → right
     *
     * Maintain a Min Heap:
     *   - Stores intervals that could cover current query
     *   - Heap ordered by interval size
     *
     * Heap stores: [size, end]
     *
     * Steps:
     *   For each query:
     *     1. Add all intervals with start <= query
     *     2. Remove intervals whose end < query
     *     3. Top of heap = smallest valid interval
     *
     * Time Complexity: O((N + Q) log N)
     * Space Complexity: O(N)
     *
     * THIS IS THE INTERVIEW ANSWER ⭐
     */
    public static int[] optimal(int[][] intervals, int[] queries) {

        // Step 1: Sort intervals by start
        Arrays.sort(intervals, Comparator.comparingInt(a -> a[0]));

        // Step 2: Store queries with index (to restore order later)
        int n = queries.length;
        int[][] qWithIndex = new int[n][2];

        for (int i = 0; i < n; i++) {
            qWithIndex[i][0] = queries[i];
            qWithIndex[i][1] = i;
        }

        // Sort queries
        Arrays.sort(qWithIndex, Comparator.comparingInt(a -> a[0]));

        // Result array
        int[] result = new int[n];

        // Min Heap: [size, end]
        PriorityQueue<int[]> minHeap = new PriorityQueue<>(
                (a, b) -> a[0] - b[0]
        );

        int i = 0; // pointer for intervals

        // Step 3: Process each query
        for (int[] q : qWithIndex) {
            int query = q[0];
            int originalIndex = q[1];

            // Add all intervals that start <= query
            while (i < intervals.length && intervals[i][0] <= query) {
                int left = intervals[i][0];
                int right = intervals[i][1];
                int size = right - left + 1;

                // Push [size, end]
                minHeap.offer(new int[]{size, right});
                i++;
            }

            // Remove intervals that cannot cover query
            while (!minHeap.isEmpty() && minHeap.peek()[1] < query) {
                minHeap.poll();
            }

            // Top of heap is smallest valid interval
            if (minHeap.isEmpty()) {
                result[originalIndex] = -1;
            } else {
                result[originalIndex] = minHeap.peek()[0];
            }
        }

        return result;
    }

    // =====================================================
    // MAIN METHOD (TESTING)
    // =====================================================
    public static void main(String[] args) {
        int[][] intervals = {
                {1, 4},
                {2, 4},
                {3, 6},
                {4, 4}
        };

        int[] queries = {2, 3, 4, 5};

        System.out.println("Brute Force:");
        System.out.println(Arrays.toString(bruteForce(intervals, queries)));

        System.out.println("Optimal:");
        System.out.println(Arrays.toString(optimal(intervals, queries)));
    }
}
