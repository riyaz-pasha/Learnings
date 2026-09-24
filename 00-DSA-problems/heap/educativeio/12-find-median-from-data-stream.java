import java.util.*;

/**
 * ============================================================================
 * GOOGLE-STYLE MOCK INTERVIEW: Find Median from Data Stream
 * ============================================================================
 * This file walks through the full interview reasoning arc for designing a
 * data structure that supports:
 *      insertNum(int num)  -> O(log n)
 *      findMedian()        -> O(1)
 *
 * Every section below is a required step in the interview narrative.
 * ============================================================================
 */
class MedianOfStream {

    /*
     * ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     * In my own words:
     *   I need to design a class that ingests integers one at a time from an
     *   unbounded stream, and at any point in time (interleaved with
     *   insertions) I must be able to report the median of ALL numbers seen
     *   so far.
     *
     * Inputs:
     *   - insertNum(int num): a single integer, -10^5 <= num <= 10^5.
     *   - findMedian(): no arguments; called after at least one insertion.
     *
     * Output:
     *   - findMedian() returns a double (since even-sized lists produce an
     *     average that may not be an integer).
     *
     * Key constraints called out explicitly:
     *   - The median must reflect the CURRENT state of the stream at the
     *     time findMedian() is called -- not a one-time batch computation.
     *   - findMedian() is called at most 500 times total, but insertNum()
     *     could be called far more often (stream could be large) -- so the
     *     *insert* path is the one that must scale, not just the query path.
     *   - "Constant time" median lookup implies we must do the expensive
     *     work (ordering) incrementally at insert time, not at query time.
     *   - Values are bounded in [-10^5, 10^5], which is a hint that a
     *     counting-sort / bucket approach is *possible*, but the classic
     *     and most general solution is paradigm-independent of that bound,
     *     so I will design for the general case first and mention the
     *     bucket optimization as a follow-up.
     *
     * Assumptions I will state and confirm:
     *   - Duplicate values are allowed and must be counted individually.
     *   - The stream is effectively unbounded in length (500 median queries,
     *     but insertions could be much larger, e.g. 10^5 - 10^6).
     *   - Single-threaded usage unless told otherwise (I will ask).
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (asked to interviewer) + ASSUMED ANSWERS
     * ========================================================================
     * 1. Q: What is the expected total number of insertNum() calls -- do we
     *       need to optimize for millions of elements, or is this small?
     *    A (assumed): Could be up to ~10^6 insertions; must be efficient
     *       (better than O(n) per insert).
     *
     * 2. Q: Can num values repeat (duplicates)?
     *    A (assumed): Yes, duplicates are allowed and each occurrence counts
     *       toward the median calculation independently.
     *
     * 3. Q: Is findMedian() ever called before any insertNum() call?
     *    A (assumed): No -- problem statement guarantees at least one
     *       element exists before findMedian() is invoked, so I don't need
     *       to handle the empty-structure case defensively (though I will
     *       add a defensive check anyway for robustness).
     *
     * 4. Q: Do we need thread-safety / concurrent insert & query support?
     *    A (assumed): No, single-threaded is fine for this interview; I'll
     *       mention how I'd adapt it (locks / concurrent structures) as a
     *       follow-up if asked.
     *
     * 5. Q: Should findMedian() return an int when possible, or always a
     *       double?
     *    A (assumed): Always return a double for consistency, since the
     *       even-count case can produce a non-integer average.
     *
     * 6. Q: Is there a hard bound on the values (given as -10^5 to 10^5)?
     *       Should I exploit that bound for a specialized solution?
     *    A (assumed): The bound exists, but I'll design the general,
     *       comparison-based solution first (it's the expected answer),
     *       and mention the bucket/counting-sort alternative as an
     *       optimization enabled by this bound if asked to extend.
     *
     * 7. Q: Can insertNum() be called with values outside the documented
     *       range, and should I validate/reject them?
     *    A (assumed): Not per constraints, but I'll add a light assertion /
     *       validation for defensive coding.
     *
     * 8. Q: Do we need to support deletion of previously inserted numbers?
     *    A (assumed): No -- this is an insert-only stream for this version
     *       of the problem. I'll note this as a stated limitation.
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     * Example 1 (Normal case, odd count):
     *   insertNum(4), insertNum(5), insertNum(6)
     *   Sorted view: [4, 5, 6]  -> odd size (3)
     *   findMedian() => 5.0 (the single middle element)
     *
     * Example 2 (Normal case, even count):
     *   insertNum(2), insertNum(4), insertNum(6), insertNum(8)
     *   Sorted view: [2, 4, 6, 8] -> even size (4)
     *   findMedian() => (4 + 6) / 2 = 5.0
     *
     * Example 3 (Edge / boundary case -- single element & interleaved calls):
     *   insertNum(41)
     *   findMedian() => 41.0   (only one element; trivially the median)
     *   insertNum(35)
     *   findMedian() => (35 + 41) / 2 = 38.0
     *   insertNum(62)
     *   findMedian() => 41.0   (sorted: [35, 41, 62], middle is 41)
     *   insertNum(4)
     *   findMedian() => (35 + 41) / 2 = 38.0  (sorted: [4,35,41,62])
     *   This example exercises the transition between odd and even sizes and
     *   confirms the "two middle elements" tie-break averaging rule.
     *
     * Additional edge cases to mention verbally:
     *   - All identical values inserted (e.g., five 7's) -> median stays 7.0.
     *   - Negative and positive values mixed, crossing zero.
     *   - Values at the extreme bounds (-100000 and 100000) inserted together.
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES (naive -> optimal)
     * ========================================================================
     * Paradigms considered and applicability:
     *   - Brute force / naive (unsorted list, sort on query)      -> APPLICABLE
     *   - Sorting-based (maintain sorted insert position)          -> APPLICABLE
     *   - Hashing-based                                            -> NOT APPLICABLE
     *       (hashing gives O(1) membership/frequency lookup, but the median
     *        requires ORDER information, which hashing discards entirely.)
     *   - Two pointer / sliding window                             -> NOT APPLICABLE
     *       (there is no "window" over a fixed array here; the data set only
     *        grows, so there's nothing to slide.)
     *   - Divide and conquer                                       -> NOT APPLICABLE
     *       (D&C shines on static, one-shot problems on a fixed array; here
     *        we need incremental maintenance of order under online inserts.)
     *   - Greedy                                                   -> NOT DIRECTLY APPLICABLE
     *       (there's no sequence of local choices to optimize toward a global
     *        objective; the heap approach is more "invariant maintenance"
     *        than greedy choice-making.)
     *   - Dynamic Programming                                      -> NOT APPLICABLE
     *       (no overlapping subproblems / optimal substructure to exploit;
     *        this is an online order-statistics problem.)
     *   - Tree / graph traversal (BST / order-statistics tree)      -> APPLICABLE
     *   - Heap / priority queue (two-heap technique)                -> APPLICABLE (OPTIMAL)
     *   - Binary search (binary search on insert position in array) -> APPLICABLE
     *   - Monotonic stack / deque                                   -> NOT APPLICABLE
     *       (monotonic structures maintain order along one sweep direction
     *        for problems like "next greater element"; they don't support
     *        efficient arbitrary insertion + median queries.)
     *   - Trie / segment tree / Fenwick tree (counting-sort style)   -> APPLICABLE
     *       (only because values are bounds -10^5..10^5; a specialized
     *        optimization, not the general solution.)
     * ========================================================================
     */

    /* ------------------------------------------------------------------
     * Approach 1: Brute Force -- Unsorted List, Sort on Every Query
     * ------------------------------------------------------------------
     * Core idea: Just append every incoming number to a list. When
     * findMedian() is called, sort a COPY of the list and read off the
     * middle element(s).
     *
     * Data structure: ArrayList<Integer>.
     *
     * Time Complexity:
     *   insertNum(): O(1) amortized (simple append).
     *   findMedian(): O(n log n) -- must sort every time it's called.
     * Space Complexity: O(n) for storage, plus O(n) transient for the sort
     *   copy (or O(1) extra if sorting in place, but that destroys original
     *   insertion order which we may still want).
     *
     * Pros:
     *   - Trivial to write correctly; low bug risk.
     *   - Good baseline to state out loud before optimizing.
     * Cons:
     *   - findMedian() is NOT O(1) -- directly violates the problem's
     *     stated requirement.
     *   - Repeated full sorts are wasteful when queries are frequent.
     *
     * When to use: Only as a warm-up/baseline explanation in an interview,
     * or in throwaway scripts where performance truly doesn't matter.
     * ------------------------------------------------------------------ */
    static class BruteForceMedian {
        private final List<Integer> numbers = new ArrayList<>();

        public void insertNum(int num) {
            numbers.add(num); // O(1) amortized append
        }

        public double findMedian() {
            // Defensive check even though problem guarantees non-empty.
            if (numbers.isEmpty()) {
                throw new IllegalStateException("No elements inserted yet.");
            }
            List<Integer> sorted = new ArrayList<>(numbers);
            Collections.sort(sorted); // O(n log n) every single call
            int size = sorted.size();
            int mid = size / 2;
            if (size % 2 == 1) {
                return sorted.get(mid);
            } else {
                return (sorted.get(mid - 1) + sorted.get(mid)) / 2.0;
            }
        }
    }

    /* ------------------------------------------------------------------
     * Approach 2: Sorting-Based -- Maintain a Perpetually Sorted List
     * ------------------------------------------------------------------
     * Core idea: Instead of sorting on every query, keep the list ALWAYS
     * sorted by inserting each new number at its correct position (binary
     * search to find the position, then array shift to insert).
     *
     * Data structure: ArrayList<Integer> kept sorted, or a plain array with
     * manual shifting.
     *
     * Time Complexity:
     *   insertNum(): O(log n) to find position via binary search, but
     *                O(n) to physically shift elements to make room in an
     *                array-backed list -> O(n) overall per insert.
     *   findMedian(): O(1) -- direct index access into the sorted list.
     * Space Complexity: O(n).
     *
     * Pros:
     *   - findMedian() truly is O(1) as required.
     *   - Conceptually simple: "keep it sorted, then indexing is free."
     * Cons:
     *   - insertNum() is O(n) due to array shifting, which dominates for
     *     large streams -- this is the bottleneck the interviewer wants
     *     you to notice and improve upon.
     *
     * When to use: Acceptable when insertions are rare relative to queries,
     * or n is small. Not ideal for a high-throughput stream.
     * ------------------------------------------------------------------ */
    static class SortedListMedian {
        private final List<Integer> sortedNumbers = new ArrayList<>();

        public void insertNum(int num) {
            // Binary search for insertion point -- O(log n).
            int insertPosition = Collections.binarySearch(sortedNumbers, num);
            if (insertPosition < 0) {
                // binarySearch returns -(insertionPoint) - 1 when not found.
                insertPosition = -(insertPosition + 1);
            }
            // ArrayList.add(index, element) shifts subsequent elements -> O(n).
            sortedNumbers.add(insertPosition, num);
        }

        public double findMedian() {
            if (sortedNumbers.isEmpty()) {
                throw new IllegalStateException("No elements inserted yet.");
            }
            int size = sortedNumbers.size();
            int mid = size / 2;
            if (size % 2 == 1) {
                return sortedNumbers.get(mid);
            } else {
                return (sortedNumbers.get(mid - 1) + sortedNumbers.get(mid)) / 2.0;
            }
        }
    }

    /* ------------------------------------------------------------------
     * Approach 3: Self-Balancing BST / Order-Statistics Tree
     * ------------------------------------------------------------------
     * Core idea: Maintain a balanced BST (e.g., an augmented TreeMap where
     * each node also tracks subtree size) so we can find the k-th smallest
     * element in O(log n). The median is just the k-th order statistic
     * where k = n/2 (or an average of two such statistics).
     *
     * Data structure: TreeMap<Integer, Integer> (value -> count) plus a
     * manually maintained size-augmented structure, OR a language library
     * that supports order-statistics (Java's TreeMap alone does NOT give
     * O(log n) rank/select out of the box -- you'd need to hand-roll an
     * augmented AVL/Red-Black tree, which is a lot of code for an interview).
     *
     * Time Complexity:
     *   insertNum(): O(log n) with an augmented balanced BST.
     *   findMedian(): O(log n) to find the k-th order statistic (NOT O(1)
     *                 unless we cache the median pointer after every
     *                 insert, which adds complexity).
     * Space Complexity: O(n).
     *
     * Pros:
     *   - Generalizes well: also supports arbitrary k-th order statistic
     *     queries (not just median), range queries, predecessor/successor.
     * Cons:
     *   - Significant implementation complexity for a plain interview
     *     (augmenting a self-balancing tree by hand is a lot of code and a
     *     high bug surface in 45 minutes).
     *   - Without extra bookkeeping, findMedian() is O(log n), not O(1),
     *     so it doesn't strictly satisfy the problem's stated requirement.
     *
     * When to use: When you need more than just the median -- e.g., a
     * general order-statistics service. Overkill for this specific ask.
     * I will show a simplified illustrative sketch below using TreeMap for
     * counting, with a linear rank scan noted as its limitation (not a
     * full augmented-tree implementation, to keep this interview-scoped).
     * ------------------------------------------------------------------ */
    static class OrderStatisticsTreeMedianSketch {
        // value -> frequency count (handles duplicates)
        private final TreeMap<Integer, Integer> countByValue = new TreeMap<>();
        private int totalCount = 0;

        public void insertNum(int num) {
            countByValue.merge(num, 1, Integer::sum); // O(log n)
            totalCount++;
        }

        public double findMedian() {
            if (totalCount == 0) {
                throw new IllegalStateException("No elements inserted yet.");
            }
            // NOTE: This illustrative version walks the map in order to find
            // the k-th element, which is O(n) worst case without a proper
            // size-augmented tree. A production version would augment each
            // TreeMap node with subtree counts for true O(log n) rank
            // queries. Shown here only to demonstrate the paradigm.
            int lowerMidTarget = (totalCount - 1) / 2 + 1; // 1-indexed rank
            int upperMidTarget = totalCount / 2 + 1;       // 1-indexed rank
            int runningCount = 0;
            Integer lowerMidValue = null;
            Integer upperMidValue = null;
            for (Map.Entry<Integer, Integer> entry : countByValue.entrySet()) {
                runningCount += entry.getValue();
                if (lowerMidValue == null && runningCount >= lowerMidTarget) {
                    lowerMidValue = entry.getKey();
                }
                if (runningCount >= upperMidTarget) {
                    upperMidValue = entry.getKey();
                    break;
                }
            }
            return (lowerMidValue + upperMidValue) / 2.0;
        }
    }

    /* ------------------------------------------------------------------
     * Approach 4: Two-Heap Technique (Max-Heap + Min-Heap)  -- OPTIMAL
     * ------------------------------------------------------------------
     * Core idea: Split the stream into two halves at all times:
     *   - A MAX-HEAP ("lowerHalf") holding the smaller half of the numbers,
     *     so its root is the LARGEST of the small numbers.
     *   - A MIN-HEAP ("upperHalf") holding the larger half of the numbers,
     *     so its root is the SMALLEST of the large numbers.
     * We keep the two heaps balanced in size (differing by at most 1). The
     * median is then always derivable from the root(s) in O(1):
     *   - If sizes are equal: median = average of both roots.
     *   - If lowerHalf has one more element: median = lowerHalf's root.
     *
     * Data structure: Two java.util.PriorityQueue instances (one reversed
     * for max-heap behavior).
     *
     * Time Complexity:
     *   insertNum(): O(log n) -- one heap push + possibly one pop/push to
     *                rebalance, both O(log n).
     *   findMedian(): O(1) -- just peek() the root(s).
     * Space Complexity: O(n) to hold all inserted numbers across both heaps.
     *
     * Pros:
     *   - Meets the problem's O(1) findMedian() requirement exactly.
     *   - insertNum() is efficient at O(log n), well-suited to large,
     *     high-throughput streams.
     *   - Conceptually clean and a well-known, interview-expected pattern.
     *   - Easy to reason about correctness via the balance invariant.
     * Cons:
     *   - Slightly more bookkeeping than a single structure (two heaps to
     *     keep balanced and correctly ordered relative to each other).
     *   - Doesn't support deletion of arbitrary elements without extra
     *     machinery (e.g., lazy deletion with hash-based counters).
     *
     * When to use: This is the canonical, expected solution for "median of
     * a data stream" style problems -- use this in the interview as the
     * primary answer.
     * ------------------------------------------------------------------ */
    static class TwoHeapMedian {
        // Max-heap for the smaller half: root = largest of the small numbers.
        private final PriorityQueue<Integer> lowerHalf =
                new PriorityQueue<>(Collections.reverseOrder());
        // Min-heap for the larger half: root = smallest of the large numbers.
        private final PriorityQueue<Integer> upperHalf = new PriorityQueue<>();

        public void insertNum(int num) {
            // Step 1: Always insert into lowerHalf first as a default landing spot.
            lowerHalf.offer(num);

            // Step 2: Maintain the ORDERING invariant -- every element in
            // lowerHalf must be <= every element in upperHalf. Move
            // lowerHalf's max into upperHalf to enforce this.
            upperHalf.offer(lowerHalf.poll());

            // Step 3: Maintain the SIZE invariant -- lowerHalf may have at
            // most one more element than upperHalf (never the reverse, and
            // never differ by more than 1).
            if (upperHalf.size() > lowerHalf.size()) {
                lowerHalf.offer(upperHalf.poll());
            }
        }

        public double findMedian() {
            if (lowerHalf.isEmpty()) {
                throw new IllegalStateException("No elements inserted yet.");
            }
            if (lowerHalf.size() > upperHalf.size()) {
                // Odd total count -> lowerHalf holds the extra middle element.
                return lowerHalf.peek();
            }
            // Even total count -> average the two middle roots.
            return (lowerHalf.peek() + upperHalf.peek()) / 2.0;
        }
    }

    /* ------------------------------------------------------------------
     * Approach 5: Counting Sort / Bucket Array (Bound-Exploiting Optimization)
     * ------------------------------------------------------------------
     * Core idea: Because num is guaranteed to be within [-10^5, 10^5] (a
     * range of only 200,001 possible values), we can maintain a frequency
     * array (bucket per possible value) plus a running total count. Finding
     * the median means walking the bucket array from one end, accumulating
     * counts until we reach the target rank(s).
     *
     * Data structure: int[] frequency array of fixed size 200001 (offset by
     * +100000 to handle negative indices), plus a running total counter.
     *
     * Time Complexity:
     *   insertNum(): O(1) -- direct array increment.
     *   findMedian(): O(V) worst case, where V = value range (200,001) --
     *                 NOT O(1) in the strict sense unless we maintain a
     *                 cached pointer/cursor that we incrementally adjust as
     *                 elements are inserted (an advanced refinement).
     * Space Complexity: O(V) -- fixed 200,001-entry array regardless of n,
     *   which can be wasteful if n is small but V is large in general.
     *
     * Pros:
     *   - insertNum() is truly O(1), even better than the heap approach.
     *   - Simple array indexing -- no comparator logic, no heap bookkeeping.
     * Cons:
     *   - findMedian() is O(V) unless we add pointer-tracking complexity,
     *     which reintroduces bookkeeping similar to the two-heap approach.
     *   - Only works because the problem gives us a bounded value range;
     *     doesn't generalize to arbitrary integers or other data types
     *     (e.g., doubles, strings).
     *   - Memory cost is fixed and can be wasteful for very few elements.
     *
     * When to use: When you're told the value domain is small and fixed,
     * insert throughput matters more than query throughput, and you're
     * willing to trade a fixed memory footprint for O(1) inserts. Good
     * follow-up answer to mention this given the problem's stated bounds.
     * ------------------------------------------------------------------ */
    static class BucketCountingMedian {
        private static final int MIN_VALUE = -100_000;
        private static final int MAX_VALUE = 100_000;
        private static final int RANGE_SIZE = MAX_VALUE - MIN_VALUE + 1;

        private final int[] frequency = new int[RANGE_SIZE];
        private int totalCount = 0;

        public void insertNum(int num) {
            frequency[num - MIN_VALUE]++; // O(1) direct bucket increment
            totalCount++;
        }

        public double findMedian() {
            if (totalCount == 0) {
                throw new IllegalStateException("No elements inserted yet.");
            }
            // O(V) scan to accumulate counts up to the target rank(s).
            int lowerMidTarget = (totalCount - 1) / 2 + 1; // 1-indexed rank
            int upperMidTarget = totalCount / 2 + 1;       // 1-indexed rank
            int runningCount = 0;
            int lowerMidValue = Integer.MIN_VALUE;
            int upperMidValue = Integer.MIN_VALUE;
            for (int bucketIndex = 0; bucketIndex < RANGE_SIZE; bucketIndex++) {
                if (frequency[bucketIndex] == 0) {
                    continue;
                }
                runningCount += frequency[bucketIndex];
                int actualValue = bucketIndex + MIN_VALUE;
                if (lowerMidValue == Integer.MIN_VALUE && runningCount >= lowerMidTarget) {
                    lowerMidValue = actualValue;
                }
                if (runningCount >= upperMidTarget) {
                    upperMidValue = actualValue;
                    break;
                }
            }
            return (lowerMidValue + upperMidValue) / 2.0;
        }
    }

    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     * Approach                     | Insert Time  | Query Time | Space  | Best For                                  | Limitations
     * ------------------------------------------------------------------------------------------------------------------------------------
     * 1. Brute Force (sort/query)  | O(1)          | O(n log n) | O(n)   | Baseline explanation only                | Query is nowhere near O(1)
     * 2. Sorted List (array shift) | O(n)          | O(1)       | O(n)   | Small n, infrequent inserts               | Insert dominated by array shifting
     * 3. Order-Stat BST (sketch)   | O(log n)      | O(log n)*  | O(n)   | Need general k-th order statistics too    | Complex to hand-roll; not O(1) query
     * 4. Two-Heap (max+min)        | O(log n)      | O(1)       | O(n)   | THE canonical / expected interview answer | None significant for this problem
     * 5. Bucket / Counting Array   | O(1)          | O(V)**     | O(V)   | Small bounded value range, insert-heavy   | Doesn't generalize; fixed memory cost
     *
     *  * O(log n) shown here is for a properly augmented balanced BST;
     *    the illustrative TreeMap sketch above is O(n) without augmentation.
     * ** V = size of the value domain (200,001 for this problem's bounds);
     *    can be reduced to amortized O(1) with an incrementally maintained
     *    cursor, at the cost of extra bookkeeping similar to the two-heap
     *    approach's complexity.
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW
     * ========================================================================
     * I would present Approach 4: Two-Heap Technique (max-heap + min-heap).
     *
     * Why:
     *   - It is the textbook, universally expected solution for "median of
     *     a data stream" -- interviewers are specifically testing whether
     *     you know this pattern.
     *   - It cleanly satisfies BOTH complexity requirements implied by the
     *     problem: O(log n) insert (efficient for large streams) and truly
     *     O(1) findMedian() (exactly as required).
     *   - It's fast to code correctly in an interview (roughly 15-20 lines
     *     of core logic) with a simple, provable invariant: "sizes differ
     *     by at most 1, and every element in lowerHalf <= every element in
     *     upperHalf."
     *   - It generalizes cleanly to follow-up questions (e.g., "what if we
     *     also need to remove elements?" or "what if we want the k-th
     *     percentile instead of median?").
     *   - Unlike the bucket approach, it doesn't rely on the value range
     *     being small, so it remains correct and efficient even if that
     *     constraint were relaxed later (a common interview follow-up).
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE -- PRODUCTION-QUALITY OPTIMAL IMPLEMENTATION
     * ========================================================================
     * This is the polished, final version of the Two-Heap approach, with
     * full Javadoc, defensive checks, and named constants where relevant.
     * ========================================================================
     */

    /**
     * A data structure that supports adding integers from a stream and
     * retrieving the running median in O(1) time per query.
     *
     * <p>Internally, the structure partitions all inserted numbers into two
     * heaps:
     * <ul>
     *   <li>{@code lowerHalf} -- a max-heap containing the smaller half of
     *       the numbers seen so far. Its root is the largest "small" number.</li>
     *   <li>{@code upperHalf} -- a min-heap containing the larger half of
     *       the numbers seen so far. Its root is the smallest "large" number.</li>
     * </ul>
     *
     * <p>Invariants maintained after every {@link #insertNum(int)} call:
     * <ol>
     *   <li>Every element in {@code lowerHalf} is less than or equal to
     *       every element in {@code upperHalf}.</li>
     *   <li>{@code lowerHalf.size()} is either equal to
     *       {@code upperHalf.size()}, or exactly one greater.</li>
     * </ol>
     *
     * <p>Given these invariants, the median is always derivable in O(1):
     * the root of {@code lowerHalf} alone (odd total count), or the average
     * of both roots (even total count).
     *
     * <p><b>Thread-safety:</b> This class is NOT thread-safe. Concurrent
     * access requires external synchronization or a concurrent redesign
     * (see Follow-Up Questions section).
     */
    public static final class MedianFinder {

        /** Max-heap holding the smaller half of all inserted numbers. */
        private final PriorityQueue<Integer> lowerHalf;

        /** Min-heap holding the larger half of all inserted numbers. */
        private final PriorityQueue<Integer> upperHalf;

        /**
         * Initializes an empty median-tracking structure. Both heaps start
         * empty; the first insertion will seed {@code lowerHalf}.
         */
        public MedianFinder() {
            // Reverse natural ordering to get max-heap behavior from
            // Java's PriorityQueue, which is a min-heap by default.
            this.lowerHalf = new PriorityQueue<>(Collections.reverseOrder());
            this.upperHalf = new PriorityQueue<>();
        }

        /**
         * Inserts a new number into the stream and rebalances the two heaps
         * so the median invariants continue to hold.
         *
         * @param num the integer to insert; expected within
         *            [-100000, 100000] per problem constraints, though this
         *            method does not depend on that bound for correctness.
         */
        public void insertNum(int num) {
            // Always land the new number in lowerHalf first. This is a
            // deliberate, simple default; the two follow-up steps below
            // correct any invariant violation this might cause.
            lowerHalf.offer(num);

            // Enforce ORDERING invariant: lowerHalf's max must not exceed
            // upperHalf's min. Moving lowerHalf's current root into
            // upperHalf guarantees this after every insert, because we
            // always route the largest "small" candidate across.
            upperHalf.offer(lowerHalf.poll());

            // Enforce SIZE invariant: lowerHalf must have >= elements than
            // upperHalf, and never more than exactly one extra. If the
            // previous step tipped upperHalf into having more elements,
            // rebalance by moving its root back.
            if (upperHalf.size() > lowerHalf.size()) {
                lowerHalf.offer(upperHalf.poll());
            }
        }

        /**
         * Returns the median of all numbers inserted so far.
         *
         * @return the median as a {@code double}: the middle element for an
         *         odd total count, or the average of the two middle
         *         elements for an even total count.
         * @throws IllegalStateException if no numbers have been inserted
         *         yet. The problem guarantees this won't happen, but the
         *         check is included as defensive coding practice.
         */
        public double findMedian() {
            if (lowerHalf.isEmpty()) {
                throw new IllegalStateException(
                        "findMedian() called with no elements inserted.");
            }

            // Because of our size invariant, lowerHalf.size() is either
            // equal to upperHalf.size() (even total) or exactly one more
            // (odd total) -- never less.
            if (lowerHalf.size() > upperHalf.size()) {
                return lowerHalf.peek(); // odd count: single middle element
            }

            // Even count: average both middle elements. Using 2.0 (not 2)
            // forces floating-point division deliberately.
            return (lowerHalf.peek() + upperHalf.peek()) / 2.0;
        }
    }

    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     * Tracing through Example 3 from Section 3:
     *   insertNum(41), findMedian(),
     *   insertNum(35), findMedian(),
     *   insertNum(62), findMedian(),
     *   insertNum(4),  findMedian()
     *
     * Notation: lowerHalf shown as a max-heap set {..}, upperHalf as a
     * min-heap set {..}. Root is always the first element listed.
     *
     * Step 0 (initial): lowerHalf = {}, upperHalf = {}
     *
     * --- insertNum(41) ---
     *   1. lowerHalf.offer(41)          -> lowerHalf = {41}
     *   2. upperHalf.offer(lowerHalf.poll()) -> lowerHalf = {}, upperHalf = {41}
     *   3. upperHalf.size()(1) > lowerHalf.size()(0) -> rebalance:
     *      lowerHalf.offer(upperHalf.poll()) -> lowerHalf = {41}, upperHalf = {}
     *   Final state: lowerHalf = {41}, upperHalf = {}
     * findMedian(): lowerHalf.size()(1) > upperHalf.size()(0) -> return 41.0
     *   Matches expected: 41.0 ✓
     *
     * --- insertNum(35) ---
     *   1. lowerHalf.offer(35) -> lowerHalf = {41, 35} (max-heap root = 41)
     *   2. upperHalf.offer(lowerHalf.poll()) -> lowerHalf.poll() removes 41
     *      lowerHalf = {35}, upperHalf = {41}
     *   3. upperHalf.size()(1) == lowerHalf.size()(1) -> no rebalance needed
     *   Final state: lowerHalf = {35}, upperHalf = {41}
     * findMedian(): sizes equal (1 == 1) -> return (35 + 41) / 2.0 = 38.0
     *   Matches expected: 38.0 ✓
     *
     * --- insertNum(62) ---
     *   1. lowerHalf.offer(62) -> lowerHalf = {62, 35} (max-heap root = 62)
     *   2. upperHalf.offer(lowerHalf.poll()) -> removes 62 from lowerHalf
     *      lowerHalf = {35}, upperHalf = {41, 62} (min-heap root = 41)
     *   3. upperHalf.size()(2) > lowerHalf.size()(1) -> rebalance:
     *      lowerHalf.offer(upperHalf.poll()) -> removes 41 from upperHalf
     *      lowerHalf = {41, 35} (root 41), upperHalf = {62}
     *   Final state: lowerHalf = {41, 35}, upperHalf = {62}
     * findMedian(): lowerHalf.size()(2) > upperHalf.size()(1) -> return 41.0
     *   Matches expected: 41.0 ✓  (sorted view [35,41,62], middle is 41)
     *
     * --- insertNum(4) ---
     *   1. lowerHalf.offer(4) -> lowerHalf = {41, 35, 4} (max-heap root = 41)
     *   2. upperHalf.offer(lowerHalf.poll()) -> removes 41 from lowerHalf
     *      lowerHalf = {35, 4} (root 35), upperHalf = {41, 62} (root 41)
     *   3. upperHalf.size()(2) == lowerHalf.size()(2) -> no rebalance needed
     *   Final state: lowerHalf = {35, 4}, upperHalf = {41, 62}
     * findMedian(): sizes equal (2 == 2) -> return (35 + 41) / 2.0 = 38.0
     *   Matches expected: 38.0 ✓  (sorted view [4,35,41,62])
     *
     * All four traced findMedian() calls match the hand-computed expected
     * values from Section 3's Example 3. Trace confirms correctness.
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     * - Brute force (sort-on-query) is simple but violates the O(1) median
     *   requirement outright -- useful only to anchor the conversation.
     * - Keeping a perpetually sorted list gives O(1) queries but pays for
     *   it with O(n) inserts due to array shifting -- a real bottleneck at
     *   scale.
     * - An augmented balanced BST generalizes further (arbitrary order
     *   statistics) but is heavy to hand-roll correctly under interview
     *   time pressure, and without augmentation isn't truly O(1) or even
     *   O(log n) for the median query.
     * - The two-heap technique is the sweet spot: O(log n) insert, O(1)
     *   query, moderate code complexity, and it's the pattern interviewers
     *   expect for this exact problem class.
     * - The bucket/counting approach trades on the problem's bounded value
     *   range for O(1) insert but pays for it with O(V) query (or added
     *   cursor-maintenance complexity) and doesn't generalize.
     *
     * Known limitations / assumptions of the final (two-heap) solution:
     *   - No support for deleting previously inserted numbers.
     *   - Not thread-safe as written.
     *   - Assumes values fit in a Java int (consistent with stated bounds).
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     * 1. "How would you support removing an arbitrary previously-inserted
     *     number from the stream?" (Requires lazy deletion with a hash map
     *     of pending removals, checked/cleaned when a heap's root matches
     *     a pending removal.)
     * 2. "How would this change if we needed the k-th percentile instead of
     *     just the median?" (Generalize the size invariant so lowerHalf
     *     maintains a k-proportional fraction of all elements rather than
     *     exactly half.)
     * 3. "What if insertNum() and findMedian() are called concurrently from
     *     multiple threads?" (Discuss synchronized blocks around both
     *     heaps, or a lock-free/concurrent skip-list-based redesign.)
     * 4. "What if the stream is so large it doesn't fit in memory?"
     *     (Discuss external/streaming approximate median algorithms, e.g.
     *     t-digest or reservoir sampling with approximate quantiles.)
     * 5. "Given the numbers are bounded to [-10^5, 10^5], can you get O(1)
     *     insert AND O(1) query simultaneously?" (Discuss maintaining a
     *     bucket array plus an incrementally-adjusted median cursor that
     *     shifts left/right by at most one bucket per insertion on
     *     average -- an amortized O(1) hybrid.)
     * 6. "How would you unit test this class thoroughly?" (Discuss
     *     randomized fuzz testing against a brute-force oracle, boundary
     *     values at -100000/100000, all-duplicates streams, and
     *     interleaved insert/query call patterns.)
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     * 1. Forgetting to route every new number through BOTH the "always
     *    insert into lowerHalf" step AND the "then move lowerHalf's max
     *    into upperHalf" step -- some candidates try to decide up front
     *    which heap a new number "belongs" to by comparing it against a
     *    root, which is more error-prone than the simpler "insert then
     *    shuffle" pattern shown here.
     * 2. Getting the size-invariant direction backwards -- i.e., allowing
     *    upperHalf to be the one that can hold the extra element instead
     *    of lowerHalf. This silently flips which heap you read from in the
     *    odd-count case and produces subtly wrong medians that only show
     *    up on odd-sized inputs during testing.
     * 3. Using integer division (num1 + num2) / 2 instead of floating-point
     *    division (num1 + num2) / 2.0 for the even-count average -- a
     *    classic silent-failure bug that passes on inputs where the sum
     *    happens to be even but fails for odd sums (e.g., (4+7)/2 = 5 in
     *    integer division instead of the correct 5.5).
     * 4. Not handling PriorityQueue's default min-heap behavior correctly
     *    -- forgetting to pass Collections.reverseOrder() (or an explicit
     *    comparator) when constructing the max-heap for lowerHalf, which
     *    silently makes lowerHalf behave like another min-heap and breaks
     *    the entire ordering invariant in a way that may not be obvious
     *    from small test cases.
     * ========================================================================
     */

    /*
     * ========================================================================
     * MAIN METHOD: Cross-validating test harness
     * ========================================================================
     * Runs hand-crafted edge cases against the optimal MedianFinder, then
     * cross-validates against the BruteForceMedian oracle using randomized
     * fuzz trials to catch any subtle discrepancies.
     * ========================================================================
     */
    public static void main(String[] args) {
        runHandCraftedTests();
        runRandomizedFuzzTests(3000);
        System.out.println("All tests passed.");
    }

    private static void runHandCraftedTests() {
        // --- Test 1: Example 1 from Section 3 (odd count) ---
        MedianFinder finder1 = new MedianFinder();
        finder1.insertNum(4);
        finder1.insertNum(5);
        finder1.insertNum(6);
        assertEquals(5.0, finder1.findMedian(), "Example 1 (odd count)");

        // --- Test 2: Example 2 from Section 3 (even count) ---
        MedianFinder finder2 = new MedianFinder();
        finder2.insertNum(2);
        finder2.insertNum(4);
        finder2.insertNum(6);
        finder2.insertNum(8);
        assertEquals(5.0, finder2.findMedian(), "Example 2 (even count)");

        // --- Test 3: Example 3 from Section 3 (interleaved calls) ---
        MedianFinder finder3 = new MedianFinder();
        finder3.insertNum(41);
        assertEquals(41.0, finder3.findMedian(), "Example 3, step 1");
        finder3.insertNum(35);
        assertEquals(38.0, finder3.findMedian(), "Example 3, step 2");
        finder3.insertNum(62);
        assertEquals(41.0, finder3.findMedian(), "Example 3, step 3");
        finder3.insertNum(4);
        assertEquals(38.0, finder3.findMedian(), "Example 3, step 4");

        // --- Test 4: All identical values ---
        MedianFinder finder4 = new MedianFinder();
        for (int i = 0; i < 5; i++) {
            finder4.insertNum(7);
        }
        assertEquals(7.0, finder4.findMedian(), "All identical values");

        // --- Test 5: Extreme bounds together ---
        MedianFinder finder5 = new MedianFinder();
        finder5.insertNum(-100_000);
        finder5.insertNum(100_000);
        assertEquals(0.0, finder5.findMedian(), "Extreme bounds average");

        System.out.println("Hand-crafted tests passed.");
    }

    private static void runRandomizedFuzzTests(int trialCount) {
        Random random = new Random(42); // fixed seed for reproducibility
        for (int trial = 0; trial < trialCount; trial++) {
            MedianFinder optimal = new MedianFinder();
            BruteForceMedian oracle = new BruteForceMedian();

            int operationCount = 1 + random.nextInt(50);
            for (int operation = 0; operation < operationCount; operation++) {
                int randomValue = -100_000 + random.nextInt(200_001);
                optimal.insertNum(randomValue);
                oracle.insertNum(randomValue);

                double optimalMedian = optimal.findMedian();
                double oracleMedian = oracle.findMedian();
                if (Math.abs(optimalMedian - oracleMedian) > 1e-9) {
                    throw new AssertionError(String.format(
                            "Mismatch on trial %d, operation %d: optimal=%f, oracle=%f",
                            trial, operation, optimalMedian, oracleMedian));
                }
            }
        }
        System.out.println("Randomized fuzz tests (" + trialCount + " trials) passed.");
    }

    private static void assertEquals(double expected, double actual, String testName) {
        if (Math.abs(expected - actual) > 1e-9) {
            throw new AssertionError(String.format(
                    "%s FAILED: expected=%f, actual=%f", testName, expected, actual));
        }
    }
}
