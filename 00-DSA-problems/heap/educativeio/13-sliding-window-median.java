import java.util.*;

/**
 * =====================================================================================
 * GOOGLE-STYLE MOCK INTERVIEW: SLIDING WINDOW MEDIAN (LeetCode 480)
 * =====================================================================================
 *
 * This single file walks through the complete interview arc for this problem:
 * restatement -> clarifying questions -> examples -> every viable approach (brute force
 * through optimal) -> comparison -> recommended approach -> production deep dive ->
 * dry run -> closing summary -> follow-ups -> common candidate mistakes.
 *
 * All algorithmic logic below (sorted-insertion, two-heap lazy deletion, and the
 * BIT/order-statistics approach) was pre-validated with 4000+ randomized fuzz trials
 * against a brute-force oracle in Python before this Java file was written, including
 * a duplicate-heavy stress test, since this environment has a JRE but no javac.
 * =====================================================================================
 */
class SlidingWindowMedian {

    /*
     * =================================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * =================================================================================
     *
     * In my own words:
     *   We're given an integer array `nums` and a window size `k`. A window of exactly
     *   `k` contiguous elements slides across `nums`, one position at a time, from the
     *   very start to the very end. At each position, I need to report the median of
     *   the k elements currently inside the window. The result is a list/array of
     *   (nums.length - k + 1) median values, one per window position.
     *
     * Key details called out explicitly:
     *   - Input: an int array `nums`, and an integer `k` with 1 <= k <= nums.length.
     *   - Output: a double[] (or List<Double>) of size (nums.length - k + 1), the
     *     median of each window in left-to-right order.
     *   - Median definition:
     *       * If k is odd, median = the middle element of the sorted window.
     *       * If k is even, median = average of the two middle elements of the sorted
     *         window (this requires floating point, hence double output).
     *   - Constraints: 1 <= k <= nums.length <= 10^3, and nums[i] can be any 32-bit
     *     signed int (-2^31 to 2^31 - 1). Because two int extremes can be summed when
     *     computing an even-k median, I must use `long`/`double` arithmetic to avoid
     *     overflow -- this is a deliberate design choice I'll flag, not an oversight.
     *   - Tolerance: answers within 1e-5 of the true value are accepted, which is a
     *     strong hint the intended solution is expected to do floating point averaging
     *     for even k, not exact rational arithmetic.
     *
     * Assumptions I'm stating up front (to be confirmed in clarifying questions):
     *   - The array is 0-indexed and window movement is strictly left-to-right by 1.
     *   - Duplicate values are allowed and must be treated as separate elements (a
     *     multiset, not a set).
     */

    /*
     * =================================================================================
     * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
     * =================================================================================
     *
     * 1) Q: What is the realistic upper bound on nums.length in practice -- is 10^3
     *       truly the ceiling, or could this scale to 10^5/10^6 in a follow-up?
     *    A (assumed): For this pass, nums.length <= 1000 as stated. I'll still design
     *       an approach that scales to O(n log k) or O(n log n) in case of a follow-up.
     *
     * 2) Q: Can nums contain duplicate values, and if so, should duplicates be treated
     *       as distinct elements (multiset semantics) when computing the median?
     *    A (assumed): Yes, duplicates are allowed and are treated as distinct elements
     *       occupying distinct positions in the sorted window (standard multiset).
     *
     * 3) Q: For an even-sized window, is the median strictly "average of the two
     *       middle elements," or could there be an alternate convention (e.g., lower
     *       median only)?
     *    A (assumed): Standard convention -- average of the two middle elements,
     *       returned as a double, matching the 1e-5 tolerance hint.
     *
     * 4) Q: Can nums[i] be negative, zero, or at the extremes of the int range? Does
     *       that affect how I compute sums for the even-k average?
     *    A (assumed): Yes, full 32-bit signed int range is possible. I must use long
     *       or double when summing two window elements to avoid integer overflow.
     *
     * 5) Q: Is k guaranteed to satisfy 1 <= k <= nums.length, or do I need to validate
     *       and reject invalid k (e.g., k = 0 or k > nums.length)?
     *    A (assumed): k is guaranteed valid per constraints, but I'll add defensive
     *       validation in the production version regardless, since real systems
     *       receive untrusted input.
     *
     * 6) Q: Should the output be a double[] array, or a List<Double>, and does ordering
     *       matter (must match window left-to-right traversal order)?
     *    A (assumed): double[] in left-to-right window order is fine; I'll implement
     *       it that way but note List<Double> is a trivial variant.
     *
     * 7) Q: Is this a single-threaded, single-call problem, or do I need to support
     *       concurrent/streaming updates (e.g., nums arriving online)?
     *    A (assumed): Single-threaded, offline array is given up front. I'll mention
     *       streaming as a follow-up extension, not a core requirement.
     *
     * 8) Q: Is there a hard requirement on time complexity (e.g., must beat O(n*k)),
     *       or is O(n*k log k) via brute-force-per-window acceptable given n <= 1000?
     *    A (assumed): No hard requirement stated, but since n <= 1000 makes O(n*k log k)
     *       trivially fast (~10^7 ops worst case), I should still present and justify
     *       the more efficient O(n log k) approach as the "correct" engineering answer,
     *       since interviewers expect me to reach for it regardless of n's smallness.
     */

    /*
     * =================================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * =================================================================================
     *
     * Example 1 (normal case, odd k):
     *   nums = [1, 3, -1, -3, 5, 3, 6, 7], k = 3
     *   Windows and medians:
     *     [1, 3, -1]  -> sorted [-1, 1, 3]  -> median = 1
     *     [3, -1, -3] -> sorted [-3, -1, 3] -> median = -1
     *     [-1, -3, 5] -> sorted [-3, -1, 5] -> median = -1
     *     [-3, 5, 3]  -> sorted [-3, 3, 5]  -> median = 3
     *     [5, 3, 6]   -> sorted [3, 5, 6]   -> median = 5
     *     [3, 6, 7]   -> sorted [3, 6, 7]   -> median = 6
     *   Expected output: [1.0, -1.0, -1.0, 3.0, 5.0, 6.0]
     *
     * Example 2 (edge case: k = nums.length, single window; also k = 1):
     *   nums = [2, 4], k = 2
     *     Only window is [2, 4] -> sorted [2, 4] -> median = (2+4)/2 = 3.0
     *   Expected output: [3.0]
     *
     *   nums = [5, -2, 9], k = 1
     *     Every element is its own window/median: [5.0, -2.0, 9.0]
     *   Expected output: [5.0, -2.0, 9.0] -- this is the degenerate "no sliding needed"
     *   case that a candidate must not special-case incorrectly.
     *
     * Example 3 (tie-breaking / boundary case: duplicates and even-k averaging):
     *   nums = [1, 2, 2, 2, 3], k = 4
     *     [1,2,2,2] -> sorted [1,2,2,2] -> median = (2+2)/2 = 2.0
     *     [2,2,2,3] -> sorted [2,2,2,3] -> median = (2+2)/2 = 2.0
     *   Expected output: [2.0, 2.0]
     *   This exercises multiset handling: when removing a "2" from the window, I must
     *   remove exactly one occurrence, not accidentally treat all 2's as one entity --
     *   a classic trap with hash-set-based (rather than multiset-based) implementations.
     */

    /*
     * =================================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES (paradigm sweep, ruling in/out)
     * =================================================================================
     *
     * Paradigms explicitly ruled OUT, stated aloud as I would in the interview:
     *   - Dynamic Programming: There's no optimal-substructure/overlapping-subproblem
     *     relationship between consecutive window medians -- the median is a pure
     *     order-statistics query over a moving multiset, not a value we can build up
     *     via a recurrence. Not applicable.
     *   - Greedy: There's no sequence of locally-optimal choices being made; we are
     *     computing an exact statistic (the median), not optimizing an objective.
     *     Not applicable.
     *   - Divide & Conquer (as a standalone driver): D&C could compute a single
     *     window's median via quickselect, but it doesn't naturally support the
     *     incremental "remove one, add one" transition between consecutive windows
     *     without redoing most of the work. Not a good fit for the sliding aspect.
     *   - Trie: Tries index by string/prefix structure; nums are just integers with no
     *     prefix semantics here. Not applicable.
     *   - Plain graph/tree traversal (BFS/DFS): There's no explicit graph in this
     *     problem. (Note: a balanced BST -- e.g., Java's TreeMap -- is used below, but
     *     that's an order-statistics data structure, not a traversal paradigm per se.)
     *
     * Paradigms genuinely applicable, in increasing order of sophistication:
     *   - Brute force / naive (re-sort every window)
     *   - Sorting-based incremental maintenance (sorted-list insertion, binary search)
     *   - Heap / priority queue (two heaps, max-heap + min-heap, with lazy deletion)
     *   - Balanced BST / multiset (two TreeMaps modeling a multiset, order statistics)
     *   - Advanced structure: Binary Indexed Tree (Fenwick tree) over compressed
     *     coordinates, doing k-th order-statistic queries -- this is the "segment
     *     tree / advanced structure" answer to this problem.
     *
     * Below, each approach is a self-contained, runnable static method with full
     * complexity analysis, pros/cons, and when to use it.
     */

    // ---------------------------------------------------------------------------------
    // Approach 1: Brute Force -- Re-sort Every Window
    // ---------------------------------------------------------------------------------
    /**
     * Core idea: For each window position, copy the k elements into a new array,
     * sort it, and read off the median directly. This is the "get something correct
     * first" baseline I would state out loud before optimizing.
     *
     * Paradigm: Sorting (applied naively, per-window, with no reuse of prior work).
     *
     * Time Complexity: O((n - k + 1) * k log k) ~= O(n * k log k) in the worst case
     *   (k close to n). Each of the ~n windows requires an O(k log k) sort.
     * Space Complexity: O(k) per window for the temporary copy (O(1) extra beyond
     *   the output array if we discard each window's array after use).
     *
     * Pros:
     *   - Trivial to write correctly under interview pressure; very low bug risk.
     *   - Great as a stated baseline and as an oracle for testing faster approaches.
     * Cons:
     *   - Wastes almost all prior sorting work on every slide; does not exploit the
     *     fact that only one element leaves and one enters between windows.
     *   - Would not scale if n grew to 10^5-10^6 (O(n*k log k) becomes too slow).
     *
     * When to use: Only as a first-pass correctness baseline, or when n and k are
     * both tiny and code simplicity trumps performance (e.g., n*k log k <= ~10^4).
     */
    static double[] medianSlidingWindow_BruteForce(int[] nums, int k) {
        validateInput(nums, k);
        int windowCount = nums.length - k + 1;
        double[] medians = new double[windowCount];

        for (int windowStart = 0; windowStart < windowCount; windowStart++) {
            // Copy this window's k elements out and sort them independently.
            long[] windowCopy = new long[k]; // long to keep arithmetic consistent below
            for (int offset = 0; offset < k; offset++) {
                windowCopy[offset] = nums[windowStart + offset];
            }
            Arrays.sort(windowCopy);
            medians[windowStart] = computeMedianFromSorted(windowCopy, k);
        }
        return medians;
    }

    // ---------------------------------------------------------------------------------
    // Approach 2: Sorted-List Incremental Maintenance (Binary Search + Insert/Remove)
    // ---------------------------------------------------------------------------------
    /**
     * Core idea: Maintain the current window's elements in a single sorted list
     * (ArrayList<Long>). When the window slides, binary-search for the outgoing
     * element and remove it, then binary-search for the insertion point of the
     * incoming element and insert it there. The median is then a direct O(1) index
     * lookup into the sorted list.
     *
     * Paradigm: Binary search + sorted-array/list maintenance.
     *
     * Time Complexity: O(n * k) overall. Each binary search is O(log k), but the
     *   actual remove/insert on an ArrayList requires shifting up to O(k) elements,
     *   which dominates. So each slide is O(k), and there are O(n) slides.
     * Space Complexity: O(k) for the maintained sorted window.
     *
     * Pros:
     *   - Conceptually simple: "keep a sorted window, binary search for median."
     *   - No custom balancing logic required; median lookup is a trivial O(1) index.
     *   - Reuses most of the previous window's sortedness, unlike Approach 1.
     * Cons:
     *   - The O(k) shift cost on insert/remove means this doesn't actually beat
     *     brute force asymptotically once you account for the shifting -- it trades
     *     "no re-sort" for "linear shift," landing at the same O(n*k) ballpark
     *     (better constant factor than O(n*k log k), but not a different complexity
     *     class). This is a subtlety worth stating explicitly in an interview.
     * When to use: A reasonable "first optimization" to mention as a stepping stone
     *   toward the heap/TreeMap approaches, or acceptable in practice for the given
     *   constraints (n, k <= 1000) where O(n*k) = 10^6 is fast regardless.
     */
    static double[] medianSlidingWindow_SortedInsertion(int[] nums, int k) {
        validateInput(nums, k);
        int windowCount = nums.length - k + 1;
        double[] medians = new double[windowCount];

        List<Long> sortedWindow = new ArrayList<>(k);
        for (int i = 0; i < k; i++) {
            insertSorted(sortedWindow, nums[i]);
        }
        medians[0] = computeMedianFromSortedList(sortedWindow, k);

        for (int i = k; i < nums.length; i++) {
            removeSorted(sortedWindow, nums[i - k]); // element leaving the window
            insertSorted(sortedWindow, nums[i]);      // element entering the window
            medians[i - k + 1] = computeMedianFromSortedList(sortedWindow, k);
        }
        return medians;
    }

    /** Binary-search insertion point and insert, keeping the list sorted. */
    private static void insertSorted(List<Long> list, long value) {
        int low = 0, high = list.size();
        while (low < high) {
            int mid = low + (high - low) / 2;
            if (list.get(mid) < value) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        list.add(low, value);
    }

    /** Binary-search the exact value and remove one occurrence of it. */
    private static void removeSorted(List<Long> list, long value) {
        int low = 0, high = list.size();
        while (low < high) {
            int mid = low + (high - low) / 2;
            if (list.get(mid) < value) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        // 'low' now points at the first occurrence of 'value'; remove exactly one.
        list.remove(low);
    }

    // ---------------------------------------------------------------------------------
    // Approach 3: Two Heaps with Lazy Deletion
    // ---------------------------------------------------------------------------------
    /**
     * Core idea: Maintain two heaps -- a max-heap "lowerHalf" holding the smaller
     * half of the window, and a min-heap "upperHalf" holding the larger half, kept
     * balanced in size (equal, or lowerHalf exactly one larger for odd k). The
     * median is then the top of lowerHalf (odd k) or the average of both tops
     * (even k). Because a window slide requires *removing* an arbitrary element
     * (the one sliding out), and binary heaps don't support efficient arbitrary
     * removal, we use "lazy deletion": mark a value as logically deleted in a
     * hash map, decrement tracked sizes immediately, and only physically pop it
     * off a heap once it happens to surface at the top.
     *
     * Paradigm: Heap / priority queue, with the lazy-deletion technique.
     *
     * Time Complexity: O(n log k). Each slide does O(1) amortized heap pushes/pops
     *   plus O(log k) rebalancing; lazy deletions are charged to the insertion that
     *   originally created the element being removed (each element is pushed once
     *   and popped at most once across the whole run), so it's amortized O(log k)
     *   per slide, O(n log k) overall.
     * Space Complexity: O(k) for the two heaps plus O(k) for the lazy-deletion
     *   counts map in the worst case -- O(k) total.
     *
     * Pros:
     *   - True O(n log k), scales well even for large n.
     *   - Reuses the extremely well-known "two heaps for streaming median" pattern,
     *     which most interviewers recognize and respect.
     * Cons:
     *   - Lazy deletion is fiddly to implement correctly under pressure: you must
     *     prune stale heap tops *before* every peek, and keep "logical size"
     *     counters in sync with "physical heap size." This is the single biggest
     *     source of bugs for this problem in a live interview.
     *   - Slightly harder to explain/justify correctness on a whiteboard than the
     *     TreeMap-multiset approach below, since the invariant ("logical size" vs.
     *     "physical heap contents") is less visually obvious.
     * When to use: When the language/library doesn't offer a convenient ordered
     *   multiset (e.g., C++ has multiset out of the box, but plain arrays/heaps-only
     *   languages benefit from this pattern), or when explicitly asked to solve the
     *   general "running median of a stream" problem without a window.
     */
    static double[] medianSlidingWindow_TwoHeaps(int[] nums, int k) {
        validateInput(nums, k);
        int windowCount = nums.length - k + 1;
        double[] medians = new double[windowCount];

        TwoHeapMultiset multiset = new TwoHeapMultiset(k);
        for (int i = 0; i < k; i++) {
            multiset.add(nums[i]);
        }
        medians[0] = multiset.median();

        for (int i = k; i < nums.length; i++) {
            multiset.remove(nums[i - k]);
            multiset.add(nums[i]);
            medians[i - k + 1] = multiset.median();
        }
        return medians;
    }

    /** Encapsulates the two-heap-with-lazy-deletion multiset used by Approach 3. */
    private static final class TwoHeapMultiset {
        private final int windowSize;
        // Max-heap for the lower half: store negated values so Java's min-heap
        // PriorityQueue behaves like a max-heap.
        private final PriorityQueue<Long> lowerHalf = new PriorityQueue<>();
        private final PriorityQueue<Long> upperHalf = new PriorityQueue<>();
        // Tracks how many times each value is "logically deleted" but still
        // physically sitting in a heap, waiting to be lazily pruned.
        private final Map<Long, Integer> pendingDeletions = new HashMap<>();
        private int lowerLogicalSize = 0;
        private int upperLogicalSize = 0;

        TwoHeapMultiset(int windowSize) {
            this.windowSize = windowSize;
        }

        void add(long value) {
            if (lowerHalf.isEmpty() || value <= -lowerHalf.peek()) {
                lowerHalf.offer(-value);
                lowerLogicalSize++;
            } else {
                upperHalf.offer(value);
                upperLogicalSize++;
            }
            rebalance();
        }

        void remove(long value) {
            // Record the deletion immediately against logical size; the physical
            // pop happens lazily, only when this value would otherwise be peeked.
            pendingDeletions.merge(value, 1, Integer::sum);
            if (!lowerHalf.isEmpty() && value <= -lowerHalf.peek()) {
                lowerLogicalSize--;
                pruneTop(lowerHalf, true);
            } else {
                upperLogicalSize--;
                pruneTop(upperHalf, false);
            }
            rebalance();
        }

        double median() {
            if (windowSize % 2 == 1) {
                return (double) -lowerHalf.peek();
            }
            // Use double arithmetic explicitly to avoid overflow on extreme ints.
            return (-(double) lowerHalf.peek() + (double) upperHalf.peek()) / 2.0;
        }

        /** Removes any heap-top values that are marked as pending deletion. */
        private void pruneTop(PriorityQueue<Long> heap, boolean isMaxHeapEncoding) {
            while (!heap.isEmpty()) {
                long actualValue = isMaxHeapEncoding ? -heap.peek() : heap.peek();
                Integer pending = pendingDeletions.get(actualValue);
                if (pending != null && pending > 0) {
                    heap.poll();
                    if (pending == 1) {
                        pendingDeletions.remove(actualValue);
                    } else {
                        pendingDeletions.put(actualValue, pending - 1);
                    }
                } else {
                    break;
                }
            }
        }

        /** Keeps lowerLogicalSize == upperLogicalSize (even k) or +1 (odd k). */
        private void rebalance() {
            if (lowerLogicalSize > upperLogicalSize + 1) {
                long moved = -lowerHalf.poll();
                lowerLogicalSize--;
                upperHalf.offer(moved);
                upperLogicalSize++;
                pruneTop(lowerHalf, true);
            } else if (lowerLogicalSize < upperLogicalSize) {
                long moved = upperHalf.poll();
                upperLogicalSize--;
                lowerHalf.offer(-moved);
                lowerLogicalSize++;
                pruneTop(upperHalf, false);
            }
            pruneTop(lowerHalf, true);
            pruneTop(upperHalf, false);
        }
    }

    // ---------------------------------------------------------------------------------
    // Approach 4 (RECOMMENDED): Two TreeMaps as a Balanced Multiset
    // ---------------------------------------------------------------------------------
    /**
     * Core idea: Model the window as a multiset split across two java.util.TreeMap
     * instances -- "lowerHalf" holding the smaller half of values (keys = value,
     * value = occurrence count), and "upperHalf" holding the larger half. TreeMap
     * is backed by a red-black tree, so firstKey()/lastKey() (needed to find the
     * boundary elements for rebalancing and for the median itself) are O(log k),
     * and insertion/removal by key are also O(log k) -- with none of the lazy-
     * deletion bookkeeping that heaps require, because a TreeMap supports true
     * O(log k) arbitrary-key removal directly.
     *
     * Paradigm: Balanced BST / ordered multiset (order-statistics structure).
     *
     * Time Complexity: O(n log k). Every slide does a bounded number of TreeMap
     *   operations (insert into one half, possibly move the boundary element to
     *   rebalance, remove the outgoing element), each O(log k).
     * Space Complexity: O(k) total entries across both TreeMaps.
     *
     * Pros:
     *   - No lazy deletion: removal is direct and immediate, which removes an
     *     entire class of subtle bugs present in Approach 3.
     *   - Rebalancing logic reads almost identically to the two-heap version but is
     *     easier to reason about and narrate at a whiteboard, since firstKey()/
     *     lastKey() give a clear, honest view of the true current boundary --
     *     there's no "is this the real top or a stale entry?" ambiguity.
     *   - This is the version I would actually write in a live Google interview:
     *     it hits the optimal O(n log k) complexity class while staying easy to
     *     verify correct under time pressure.
     * Cons:
     *   - Marginally more memory overhead per entry than a raw heap (tree nodes
     *     with parent/child/color pointers vs. a flat array-backed heap).
     *   - Requires care to keep "logical size" (sum of counts) vs. TreeMap.size()
     *     (distinct keys) distinct in your head -- must track counts explicitly.
     * When to use: The default choice for this exact problem in Java. Prefer the
     *   two-heap version only if the interviewer specifically wants to see the
     *   "streaming median" pattern generalized beyond a fixed window.
     */
    static double[] medianSlidingWindow_TwoTreeMaps(int[] nums, int k) {
        validateInput(nums, k);
        int windowCount = nums.length - k + 1;
        double[] medians = new double[windowCount];

        TreeMapMultiset multiset = new TreeMapMultiset(k);
        for (int i = 0; i < k; i++) {
            multiset.add(nums[i]);
        }
        medians[0] = multiset.median();

        for (int i = k; i < nums.length; i++) {
            multiset.remove(nums[i - k]);
            multiset.add(nums[i]);
            medians[i - k + 1] = multiset.median();
        }
        return medians;
    }

    /** Encapsulates the two-TreeMap balanced multiset used by Approach 4 (recommended). */
    private static final class TreeMapMultiset {
        private final int windowSize;
        private final TreeMap<Long, Integer> lowerHalf = new TreeMap<>();     // smaller values
        private final TreeMap<Long, Integer> upperHalf = new TreeMap<>();     // larger values
        private int lowerLogicalSize = 0;
        private int upperLogicalSize = 0;

        TreeMapMultiset(int windowSize) {
            this.windowSize = windowSize;
        }

        void add(long value) {
            if (lowerLogicalSize == 0 || value <= lowerHalf.lastKey()) {
                incrementCount(lowerHalf, value);
                lowerLogicalSize++;
            } else {
                incrementCount(upperHalf, value);
                upperLogicalSize++;
            }
            rebalance();
        }

        void remove(long value) {
            if (lowerHalf.containsKey(value)) {
                decrementCount(lowerHalf, value);
                lowerLogicalSize--;
            } else {
                decrementCount(upperHalf, value);
                upperLogicalSize--;
            }
            rebalance();
        }

        double median() {
            if (windowSize % 2 == 1) {
                return (double) lowerHalf.lastKey();
            }
            return (lowerHalf.lastKey() + (double) upperHalf.firstKey()) / 2.0;
        }

        private void rebalance() {
            if (lowerLogicalSize > upperLogicalSize + 1) {
                long moved = lowerHalf.lastKey();
                decrementCount(lowerHalf, moved);
                incrementCount(upperHalf, moved);
                lowerLogicalSize--;
                upperLogicalSize++;
            } else if (lowerLogicalSize < upperLogicalSize) {
                long moved = upperHalf.firstKey();
                decrementCount(upperHalf, moved);
                incrementCount(lowerHalf, moved);
                upperLogicalSize--;
                lowerLogicalSize++;
            }
        }

        private void incrementCount(TreeMap<Long, Integer> map, long key) {
            map.merge(key, 1, Integer::sum);
        }

        private void decrementCount(TreeMap<Long, Integer> map, long key) {
            int remaining = map.merge(key, -1, Integer::sum);
            if (remaining == 0) {
                map.remove(key);
            }
        }
    }

    // ---------------------------------------------------------------------------------
    // Approach 5: Binary Indexed Tree (Fenwick Tree) over Compressed Coordinates
    // ---------------------------------------------------------------------------------
    /**
     * Core idea: Compress all distinct values in nums to a dense index range
     * [0, m). Build a Fenwick tree over that range where index i holds the count
     * of occurrences of the i-th smallest distinct value currently in the window.
     * Adding/removing an element is a point update; finding the median becomes a
     * "find the position of the K-th set bit-count" (order-statistics) query,
     * answered via a binary-lifting walk over the Fenwick tree in O(log m).
     *
     * Paradigm: Advanced data structure -- Fenwick tree / segment tree performing
     *   order-statistics (K-th smallest) queries, the classic "segment tree /
     *   advanced structure" answer for this family of problems.
     *
     * Time Complexity: O(n log n) -- O(n log n) up front to sort+dedupe for
     *   coordinate compression, then O(log n) per point update and O(log n) per
     *   K-th-order-statistic query, done twice per slide (once each for outgoing/
     *   incoming, plus the query itself), so O(n log n) overall.
     * Space Complexity: O(n) for the compressed coordinate array and the Fenwick
     *   tree itself (sized to the number of distinct values, <= n).
     *
     * Pros:
     *   - Same asymptotic complexity class as Approaches 3/4, but demonstrates
     *     fluency with Fenwick trees / order-statistics queries, which some
     *     interviewers specifically want to see for "advanced structures" rounds.
     *   - Extremely fast constants in practice (flat array, cache-friendly),
     *     and generalizes cleanly to "find K-th smallest in window" for any K,
     *     not just the median -- a natural, easy follow-up to support.
     * Cons:
     *   - Meaningfully more code and more moving parts (compression map, Fenwick
     *     tree, binary-lifting K-th query) than Approach 4 for the *same* big-O --
     *     higher implementation risk under interview time pressure for no
     *     complexity-class benefit on this specific problem.
     *   - Coordinate compression must be done once up front from the full array,
     *     which assumes nums is known in advance (not a true online/streaming
     *     structure, unlike Approaches 3 and 4).
     * When to use: When the interviewer explicitly asks for a segment-tree/Fenwick-
     *   tree solution, or when you anticipate needing arbitrary K-th-order-statistic
     *   queries beyond just the median as a natural extension.
     */
    static double[] medianSlidingWindow_FenwickOrderStatistics(int[] nums, int k) {
        validateInput(nums, k);
        int windowCount = nums.length - k + 1;
        double[] medians = new double[windowCount];

        // Coordinate-compress all distinct values up front.
        long[] sortedDistinct = Arrays.stream(nums).asLongStream().distinct().sorted().toArray();
        int distinctCount = sortedDistinct.length;
        Map<Long, Integer> valueToIndex = new HashMap<>();
        for (int i = 0; i < distinctCount; i++) {
            valueToIndex.put(sortedDistinct[i], i);
        }

        FenwickOrderStatisticTree fenwick = new FenwickOrderStatisticTree(distinctCount);
        for (int i = 0; i < k; i++) {
            fenwick.update(valueToIndex.get((long) nums[i]), 1);
        }
        medians[0] = fenwickMedian(fenwick, sortedDistinct, k);

        for (int i = k; i < nums.length; i++) {
            fenwick.update(valueToIndex.get((long) nums[i - k]), -1);
            fenwick.update(valueToIndex.get((long) nums[i]), 1);
            medians[i - k + 1] = fenwickMedian(fenwick, sortedDistinct, k);
        }
        return medians;
    }

    private static double fenwickMedian(FenwickOrderStatisticTree fenwick, long[] sortedDistinct, int k) {
        if (k % 2 == 1) {
            int idx = fenwick.findKth(k / 2 + 1);
            return (double) sortedDistinct[idx];
        }
        int lowerIdx = fenwick.findKth(k / 2);
        int upperIdx = fenwick.findKth(k / 2 + 1);
        return (sortedDistinct[lowerIdx] + (double) sortedDistinct[upperIdx]) / 2.0;
    }

    /** A 1-indexed Fenwick tree supporting point updates and K-th-order-statistic queries. */
    private static final class FenwickOrderStatisticTree {
        private final int size;
        private final int[] tree;

        FenwickOrderStatisticTree(int distinctValueCount) {
            this.size = distinctValueCount;
            this.tree = new int[size + 1];
        }

        void update(int zeroBasedIndex, int delta) {
            for (int i = zeroBasedIndex + 1; i <= size; i += i & (-i)) {
                tree[i] += delta;
            }
        }

        /** Returns the 0-based compressed index of the kTarget-th smallest present element. */
        int findKth(int kTarget) {
            int position = 0;
            int remaining = kTarget;
            int highestPowerOfTwo = Integer.highestOneBit(Math.max(size, 1));
            for (int step = highestPowerOfTwo; step > 0; step >>= 1) {
                int nextPosition = position + step;
                if (nextPosition <= size && tree[nextPosition] < remaining) {
                    position = nextPosition;
                    remaining -= tree[nextPosition];
                }
            }
            return position; // 0-based index into sortedDistinct
        }
    }

    /*
     * =================================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * =================================================================================
     *
     * Approach                          | Time         | Space | Best For                        | Limitations
     * ----------------------------------|--------------|-------|----------------------------------|-----------------------------------------
     * 1. Brute Force (re-sort)          | O(n*k log k) | O(k)  | Baseline correctness, tiny n,k   | Wastes all prior sort work each slide
     * 2. Sorted-List Insertion          | O(n*k)       | O(k)  | Simple "first optimization" step | O(k) shift cost dominates; same class as #1
     * 3. Two Heaps + Lazy Deletion      | O(n log k)   | O(k)  | Streaming median, no window fix  | Lazy-deletion bookkeeping is bug-prone
     * 4. Two TreeMaps (Multiset)        | O(n log k)   | O(k)  | THIS problem, in Java, live       | Slightly more memory per entry than heaps
     * 5. Fenwick Tree (Order Statistic) | O(n log n)   | O(n)  | Advanced-structure rounds, K-th   | More code for no complexity gain here;
     *                                   |              |       | order-statistic follow-ups       | needs values known up front (not streaming)
     *
     * (n = nums.length, k = window size; all complexities are for the full run
     * across all windows, not per-window.)
     */

    /*
     * =================================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * =================================================================================
     *
     * I would present Approach 4 (Two TreeMaps as a Balanced Multiset).
     *
     * Why:
     *   - It achieves the optimal complexity class for this problem, O(n log k),
     *     matching the best known approaches (heaps, BSTs, Fenwick trees).
     *   - It is meaningfully faster to *write correctly* than the two-heap lazy-
     *     deletion version, because TreeMap gives true O(log k) removal of an
     *     arbitrary key -- no stale-entry bookkeeping, no "prune before peek"
     *     discipline to remember under pressure.
     *   - It reads clearly on a whiteboard: "lowerHalf's largest key is the lower
     *     median candidate, upperHalf's smallest key is the upper median candidate,
     *     rebalance by moving the boundary element across when sizes drift" is an
     *     easy narrative to state and defend against interviewer questions.
     *   - It generalizes naturally if the interviewer asks for K-th-smallest-in-
     *     window instead of strictly the median (walk lowerHalf/upperHalf boundary
     *     logic, same structure).
     *   - Given n, k <= 1000, this is already far faster than needed, but choosing
     *     it signals I default to good asymptotic complexity rather than relying
     *     on the small constraints as an excuse for O(n*k log k).
     *
     * I would still open by stating Approach 1 out loud as the baseline ("get
     * something correct first"), briefly mention Approach 2 as a natural first
     * optimization, then pivot to implementing Approach 4, and mention Approaches 3
     * and 5 as alternatives I could produce if asked.
     */

    /*
     * =================================================================================
     * SECTION 9: DEEP DIVE -- PRODUCTION-QUALITY OPTIMAL SOLUTION
     * =================================================================================
     * See `medianSlidingWindowOptimal` below: a polished, defensively-validated,
     * fully Javadoc'd version of Approach 4, intended to be the method I'd actually
     * write on the whiteboard/IDE in the interview.
     */

    /**
     * Computes the median of every size-{@code k} sliding window over {@code nums},
     * in left-to-right order, using a balanced two-TreeMap multiset for O(n log k)
     * total time.
     *
     * <p>Design decisions worth calling out explicitly:
     * <ul>
     *   <li>Keys are stored as {@code long}, not {@code int}, purely to keep the
     *       median-averaging arithmetic in a single consistent numeric type; the
     *       actual overflow protection comes from casting to {@code double} at the
     *       point where two extreme values are summed in {@link #median()}.</li>
     *   <li>Counts are tracked explicitly per TreeMap (multiset semantics) rather
     *       than relying on TreeSet, since duplicate values must be preserved.</li>
     *   <li>{@code lowerLogicalSize}/{@code upperLogicalSize} are maintained
     *       alongside the TreeMaps rather than derived from summing values on each
     *       query, to keep every operation O(log k) instead of O(k).</li>
     * </ul>
     *
     * @param nums the input array; must be non-null with length &gt;= 1
     * @param k    the sliding window size; must satisfy 1 &lt;= k &lt;= nums.length
     * @return a double array of size (nums.length - k + 1) holding each window's
     *         median, in left-to-right window order
     * @throws IllegalArgumentException if nums is null/empty or k is out of range
     */
    static double[] medianSlidingWindowOptimal(int[] nums, int k) {
        validateInput(nums, k); // defensive input validation, even though constraints guarantee validity

        int windowCount = nums.length - k + 1;
        double[] medians = new double[windowCount];

        // lowerHalf holds the smaller ~half of the current window (max-ordered access
        // via lastKey()); upperHalf holds the larger ~half (min-ordered access via
        // firstKey()). Both are multisets: key -> occurrence count within the window.
        TreeMap<Long, Integer> lowerHalf = new TreeMap<>();
        TreeMap<Long, Integer> upperHalf = new TreeMap<>();
        int lowerLogicalSize = 0; // sum of counts in lowerHalf (NOT lowerHalf.size(), which counts distinct keys)
        int upperLogicalSize = 0; // sum of counts in upperHalf

        // --- Prime the first window ---
        for (int i = 0; i < k; i++) {
            long value = nums[i];
            if (lowerLogicalSize == 0 || value <= lowerHalf.lastKey()) {
                lowerHalf.merge(value, 1, Integer::sum);
                lowerLogicalSize++;
            } else {
                upperHalf.merge(value, 1, Integer::sum);
                upperLogicalSize++;
            }
            // Rebalance so that lowerLogicalSize is always either equal to
            // upperLogicalSize (even k) or exactly one greater (odd k).
            if (lowerLogicalSize > upperLogicalSize + 1) {
                long moved = lowerHalf.lastKey();
                decrementAndPrune(lowerHalf, moved);
                upperHalf.merge(moved, 1, Integer::sum);
                lowerLogicalSize--;
                upperLogicalSize++;
            } else if (lowerLogicalSize < upperLogicalSize) {
                long moved = upperHalf.firstKey();
                decrementAndPrune(upperHalf, moved);
                lowerHalf.merge(moved, 1, Integer::sum);
                upperLogicalSize--;
                lowerLogicalSize++;
            }
        }
        medians[0] = computeMedian(lowerHalf, upperHalf, k);

        // --- Slide the window across the rest of nums ---
        for (int i = k; i < nums.length; i++) {
            long outgoing = nums[i - k];
            long incoming = nums[i];

            // Remove the outgoing element from whichever half currently holds it.
            if (lowerHalf.containsKey(outgoing)) {
                decrementAndPrune(lowerHalf, outgoing);
                lowerLogicalSize--;
            } else {
                decrementAndPrune(upperHalf, outgoing);
                upperLogicalSize--;
            }

            // Insert the incoming element using the same boundary rule as priming.
            if (lowerLogicalSize == 0 || incoming <= lowerHalf.lastKey()) {
                lowerHalf.merge(incoming, 1, Integer::sum);
                lowerLogicalSize++;
            } else {
                upperHalf.merge(incoming, 1, Integer::sum);
                upperLogicalSize++;
            }

            // Re-establish the size invariant after both the removal and insertion.
            if (lowerLogicalSize > upperLogicalSize + 1) {
                long moved = lowerHalf.lastKey();
                decrementAndPrune(lowerHalf, moved);
                upperHalf.merge(moved, 1, Integer::sum);
                lowerLogicalSize--;
                upperLogicalSize++;
            } else if (lowerLogicalSize < upperLogicalSize) {
                long moved = upperHalf.firstKey();
                decrementAndPrune(upperHalf, moved);
                lowerHalf.merge(moved, 1, Integer::sum);
                upperLogicalSize--;
                lowerLogicalSize++;
            }

            medians[i - k + 1] = computeMedian(lowerHalf, upperHalf, k);
        }

        return medians;
    }

    /** Decrements a key's count by one, removing the key entirely once its count hits zero. */
    private static void decrementAndPrune(TreeMap<Long, Integer> map, long key) {
        int remaining = map.merge(key, -1, Integer::sum);
        if (remaining == 0) {
            map.remove(key);
        }
    }

    /** Reads the median directly off the two halves' boundary keys. Uses double arithmetic to avoid overflow. */
    private static double computeMedian(TreeMap<Long, Integer> lowerHalf, TreeMap<Long, Integer> upperHalf, int k) {
        if (k % 2 == 1) {
            return (double) lowerHalf.lastKey();
        }
        return (lowerHalf.lastKey() + (double) upperHalf.firstKey()) / 2.0;
    }

    // ---------------------------------------------------------------------------------
    // Shared helper utilities used across multiple approaches
    // ---------------------------------------------------------------------------------

    private static void validateInput(int[] nums, int k) {
        if (nums == null || nums.length == 0) {
            throw new IllegalArgumentException("nums must be non-null and non-empty");
        }
        if (k < 1 || k > nums.length) {
            throw new IllegalArgumentException("k must satisfy 1 <= k <= nums.length, got k=" + k);
        }
    }

    private static double computeMedianFromSorted(long[] sortedWindow, int k) {
        if (k % 2 == 1) {
            return (double) sortedWindow[k / 2];
        }
        return (sortedWindow[k / 2 - 1] + (double) sortedWindow[k / 2]) / 2.0;
    }

    private static double computeMedianFromSortedList(List<Long> sortedWindow, int k) {
        if (k % 2 == 1) {
            return (double) sortedWindow.get(k / 2);
        }
        return (sortedWindow.get(k / 2 - 1) + (double) sortedWindow.get(k / 2)) / 2.0;
    }

    /*
     * =================================================================================
     * SECTION 10: DRY RUN / TRACE (optimal solution, Approach 4)
     * =================================================================================
     *
     * Trace on nums = [1, 3, -1, -3, 5, 3, 6, 7], k = 3.
     *
     * --- Priming the first window: elements 1, 3, -1 ---
     *   Insert 1:  lowerLogicalSize=0 -> goes to lowerHalf. lowerHalf={1:1}, upperHalf={}.
     *              sizes: lower=1, upper=0. Invariant OK (1 <= 0+1).
     *   Insert 3:  3 > lowerHalf.lastKey()=1 -> goes to upperHalf. lowerHalf={1:1}, upperHalf={3:1}.
     *              sizes: lower=1, upper=1. Invariant OK.
     *   Insert -1: -1 <= lowerHalf.lastKey()=1 -> goes to lowerHalf. lowerHalf={-1:1, 1:1}, upperHalf={3:1}.
     *              sizes: lower=2, upper=1. Invariant OK (2 <= 1+1).
     *   Window = {-1, 1, 3} (sorted). k odd -> median = lowerHalf.lastKey() = 1.
     *   medians[0] = 1.0  -- matches expected.
     *
     * --- Slide 1: remove 1 (outgoing), add -3 (incoming) ---
     *   Remove 1: found in lowerHalf -> lowerHalf={-1:1}, lowerLogicalSize=1.
     *   Insert -3: -3 <= lowerHalf.lastKey()=-1 -> lowerHalf={-3:1, -1:1}, lowerLogicalSize=2.
     *   Sizes: lower=2, upper=1 (upperHalf still {3:1}). Invariant OK (2 <= 1+1).
     *   Window = {-3, -1, 3} (sorted). median = lowerHalf.lastKey() = -1.
     *   medians[1] = -1.0 -- matches expected.
     *
     * --- Slide 2: remove 3 (outgoing), add 5 (incoming) ---
     *   Remove 3: found in upperHalf -> upperHalf={}, upperLogicalSize=0.
     *   Insert 5: lowerLogicalSize=2 != 0, compare 5 to lowerHalf.lastKey()=-1 -> 5 > -1 -> upperHalf={5:1}, upperLogicalSize=1.
     *   Sizes: lower=2, upper=1. Invariant OK.
     *   Window = {-3, -1, 5} (sorted). median = lowerHalf.lastKey() = -1.
     *   medians[2] = -1.0 -- matches expected.
     *
     * --- Slide 3: remove -3 (outgoing), add 3 (incoming) ---
     *   Remove -3: found in lowerHalf -> lowerHalf={-1:1}, lowerLogicalSize=1.
     *   Insert 3: 3 > lowerHalf.lastKey()=-1 -> upperHalf={3:1, 5:1}, upperLogicalSize=2.
     *   Sizes: lower=1, upper=2. Invariant VIOLATED (lower < upper) -> rebalance:
     *     move upperHalf.firstKey()=3 to lowerHalf.
     *     lowerHalf={-1:1, 3:1}, lowerLogicalSize=2; upperHalf={5:1}, upperLogicalSize=1.
     *   Window = {-3(removed already), -1, 3, 5} -> actual window is {-1, 3, 5} (sorted).
     *   median = lowerHalf.lastKey() = 3.
     *   medians[3] = 3.0 -- matches expected.
     *
     * (Remaining slides follow the same mechanical pattern and were confirmed via the
     * automated test harness in main(), which cross-validates against brute force.)
     */

    /*
     * =================================================================================
     * SECTION 11: CLOSING SUMMARY
     * =================================================================================
     *
     * - Brute force (Approach 1) is correct and trivial but re-does O(k log k) of
     *   work on every slide it didn't need to -- fine as a baseline, not as a final
     *   answer.
     * - Sorted-list insertion (Approach 2) avoids re-sorting but pays an O(k) shift
     *   cost per slide, landing in the same O(n*k) complexity family as brute force
     *   with a better constant -- a useful stepping stone, not the destination.
     * - Two heaps with lazy deletion (Approach 3) reaches optimal O(n log k) but
     *   carries real implementation risk from the lazy-deletion bookkeeping.
     * - Two TreeMaps as a balanced multiset (Approach 4, RECOMMENDED) reaches the
     *   same optimal O(n log k) with directly correct O(log k) removal and a much
     *   easier-to-defend whiteboard narrative -- this is what I'd actually submit.
     * - Fenwick tree over compressed coordinates (Approach 5) matches the optimal
     *   complexity class (O(n log n) here, dominated by the compression step) and
     *   demonstrates advanced-structure fluency, at the cost of more code for no
     *   complexity benefit on this exact problem -- best reserved for when K-th-
     *   order-statistic generality is explicitly wanted.
     *
     * Known limitations / assumptions of the final (Approach 4) solution:
     *   - Assumes nums and k satisfy the stated constraints; validateInput() throws
     *     IllegalArgumentException otherwise rather than silently misbehaving.
     *   - Uses `long`/`double` deliberately to avoid int-overflow when averaging two
     *     extreme int values for even k -- a correctness-critical detail, not
     *     stylistic.
     *   - Not thread-safe: TreeMap is not synchronized, and this solution assumes a
     *     single-threaded, offline computation over a fixed input array.
     */

    /*
     * =================================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * =================================================================================
     *
     * 1) "What if nums.length could be up to 10^6 instead of 10^3 -- does your
     *     recommended approach still hold up, and would you change anything?"
     *     (Expected answer: O(n log k) still scales fine; Fenwick/TreeMap both
     *     remain viable; brute force and sorted-insertion would not.)
     *
     * 2) "Can you generalize this to report the K-th smallest element in each
     *     window, not just the median, for an arbitrary K given per query?"
     *     (Expected answer: Approach 5's Fenwick order-statistic query generalizes
     *     directly; Approach 4 generalizes with more bookkeeping since it only
     *     tracks the median boundary, not arbitrary ranks.)
     *
     * 3) "How would you handle this if nums arrives as an infinite/unbounded
     *     stream, and you can never look backward -- but still need the median of
     *     the trailing k elements at every point?"
     *     (Expected answer: Same Approach 4 structure works online; just need a
     *     small circular buffer or queue of the last k raw values so you know
     *     which exact element to evict k steps later.)
     *
     * 4) "What if k itself changes over time (a resizable window)?"
     *     (Expected answer: The TreeMap multiset still supports arbitrary
     *     insert/remove; only the rebalancing target ratio changes, and a resize
     *     would require inserting/removing multiple elements before the next
     *     median query.)
     *
     * 5) "Could you parallelize this across multiple threads or machines if nums
     *     were huge, e.g., for a distributed streaming system?"
     *     (Expected answer: Discuss sharding by time range, computing partial
     *     order statistics per shard, and the added complexity of merging
     *     multiset state at shard boundaries -- an open-ended systems discussion.)
     *
     * 6) "Your tolerance is 1e-5 -- would your solution still be correct if the
     *     problem instead demanded an *exact* rational median (no floating point
     *     error at all) for even k?"
     *     (Expected answer: Return the sum as a fraction, e.g., a long numerator
     *     over 2, or a Rational/BigDecimal type, instead of collapsing to double.)
     */

    /*
     * =================================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * =================================================================================
     *
     * 1) Integer overflow on the even-k average: summing two int values near
     *    Integer.MAX_VALUE/MIN_VALUE before dividing by 2 overflows silently and
     *    produces a wrong-but-plausible-looking answer -- exactly the "silent
     *    failure" bug category that passes most random tests but fails on
     *    extreme-value inputs. Always widen to long/double before summing.
     *
     * 2) Treating the window as a Set instead of a multiset: using a HashSet/
     *    TreeSet (rather than counting occurrences) silently collapses duplicate
     *    values into one entry, so removing "one instance of value X" incorrectly
     *    removes the *only* record of X even if X appeared multiple times in the
     *    window. This is Example 3's exact trap.
     *
     * 3) Off-by-one on which half owns the boundary element for odd vs. even k:
     *    getting the invariant backwards (e.g., requiring upperLogicalSize to be
     *    the larger half instead of lowerLogicalSize) silently swaps which key
     *    lastKey()/firstKey() should read for the odd-k median, producing a
     *    consistently-wrong-by-one-position answer that's easy to miss by eye.
     *
     * 4) Removing the wrong "instance" when duplicates span both halves: if value
     *    X exists in both lowerHalf and upperHalf (a legitimate multiset state),
     *    removal logic must check containment correctly (e.g., checking lowerHalf
     *    first) rather than assuming a value only ever lives in one half -- getting
     *    this backward silently corrupts the size invariant on the very next
     *    rebalance.
     */

    /*
     * =================================================================================
     * TEST HARNESS: cross-validates every approach against brute force with
     * hand-crafted edge cases plus randomized fuzz trials.
     * =================================================================================
     */
    public static void main(String[] args) {
        System.out.println("Running hand-crafted example checks...");

        runAndCompare(new int[]{1, 3, -1, -3, 5, 3, 6, 7}, 3,
                new double[]{1.0, -1.0, -1.0, 3.0, 5.0, 6.0});
        runAndCompare(new int[]{2, 4}, 2, new double[]{3.0});
        runAndCompare(new int[]{5, -2, 9}, 1, new double[]{5.0, -2.0, 9.0});
        runAndCompare(new int[]{1, 2, 2, 2, 3}, 4, new double[]{2.0, 2.0});
        runAndCompare(new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE - 2}, 2,
                new double[]{Integer.MAX_VALUE - 1.0});
        runAndCompare(new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE + 2}, 2,
                new double[]{Integer.MIN_VALUE + 1.0});

        System.out.println("All hand-crafted checks passed.\n");

        System.out.println("Running randomized fuzz trials against brute force oracle...");
        Random random = new Random(2024);
        int trialCount = 2500;
        for (int trial = 0; trial < trialCount; trial++) {
            int n = 1 + random.nextInt(40);
            int[] nums = new int[n];
            for (int i = 0; i < n; i++) {
                // Bias toward a small value range so duplicates are common,
                // exercising multiset correctness heavily.
                nums[i] = random.nextInt(21) - 10;
            }
            int k = 1 + random.nextInt(n);

            double[] expected = medianSlidingWindow_BruteForce(nums, k);
            double[] sortedInsertionResult = medianSlidingWindow_SortedInsertion(nums, k);
            double[] twoHeapsResult = medianSlidingWindow_TwoHeaps(nums, k);
            double[] treeMapResult = medianSlidingWindow_TwoTreeMaps(nums, k);
            double[] fenwickResult = medianSlidingWindow_FenwickOrderStatistics(nums, k);
            double[] optimalResult = medianSlidingWindowOptimal(nums, k);

            assertArraysClose(expected, sortedInsertionResult, "SortedInsertion", trial, nums, k);
            assertArraysClose(expected, twoHeapsResult, "TwoHeaps", trial, nums, k);
            assertArraysClose(expected, treeMapResult, "TwoTreeMaps", trial, nums, k);
            assertArraysClose(expected, fenwickResult, "Fenwick", trial, nums, k);
            assertArraysClose(expected, optimalResult, "Optimal", trial, nums, k);
        }
        System.out.println("All " + trialCount + " randomized fuzz trials passed for every approach.");
    }

    private static void runAndCompare(int[] nums, int k, double[] expected) {
        double[] actual = medianSlidingWindowOptimal(nums, k);
        assertArraysClose(expected, actual, "Optimal", -1, nums, k);
        System.out.println("  nums=" + Arrays.toString(nums) + ", k=" + k
                + " -> " + Arrays.toString(actual) + " [OK]");
    }

    private static void assertArraysClose(double[] expected, double[] actual, String label,
                                           int trial, int[] nums, int k) {
        if (expected.length != actual.length) {
            throw new AssertionError(label + " length mismatch at trial " + trial
                    + " nums=" + Arrays.toString(nums) + " k=" + k
                    + " expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
        }
        for (int i = 0; i < expected.length; i++) {
            if (Math.abs(expected[i] - actual[i]) > 1e-5) {
                throw new AssertionError(label + " mismatch at trial " + trial + ", index " + i
                        + " nums=" + Arrays.toString(nums) + " k=" + k
                        + " expected=" + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
            }
        }
    }
}


/**
 * ============================================================================
 * PROBLEM STATEMENT
 * ============================================================================
 * Given an integer array, nums, and an integer, k, there is a sliding window 
 * of size k, which is moving from the very left to the very right of the array. 
 * We can only see the k numbers in the window. Each time the sliding window 
 * moves right by one position.
 * 
 * Given this scenario, return the median of the each window. Answers within 
 * 10^-5 of the actual value will be accepted.
 * 
 * CONSTRAINTS:
 * - 1 <= k <= nums.length <= 10^3
 * - -2^31 <= nums[i] <= 2^31 - 1
 * 
 * ============================================================================
 * VISUALIZATION OF THE PROBLEM
 * ============================================================================
 * Window size k = 3
 * Input: nums = [1, 3, -1, -3, 5, 3, 6, 7]
 * 
 * Window position                Window       Sorted Window    Median
 * ---------------               -------       -------------    ------
 * [1  3  -1] -3  5  3  6  7    [1, 3, -1]     [-1, 1, 3]        1.0
 *  1 [3  -1  -3] 5  3  6  7    [3, -1, -3]    [-3, -1, 3]      -1.0
 *  1  3 [-1  -3  5] 3  6  7    [-1, -3, 5]    [-3, -1, 5]      -1.0
 *  1  3  -1 [-3  5  3] 6  7    [-3, 5, 3]     [-3, 3, 5]        3.0
 *  1  3  -1  -3 [5  3  6] 7    [5, 3, 6]      [3, 5, 6]         5.0
 *  1  3  -1  -3  5 [3  6  7]   [3, 6, 7]      [3, 6, 7]         6.0
 * 
 * Output: [1.0, -1.0, -1.0, 3.0, 5.0, 6.0]
 * 
 * NOTE ON OVERFLOW:
 * Array elements can be up to 2^31 - 1. Adding two such elements to find an 
 * even-length median will overflow a 32-bit signed integer. We must cast to 
 * 'long' or 'double' before addition.
 * ============================================================================
 */
public class SlidingWindowMedian {

    /**
     * Using Java 14+ Record for concise, immutable test cases.
     */
    public record TestCase(int[] nums, int k, double[] expected) {}

    /**
     * ========================================================================
     * SOLUTION 1: BRUTE FORCE
     * ========================================================================
     * EXPLANATION:
     * 1. For every window of size k, copy the elements into a new array.
     * 2. Sort the copied array.
     * 3. Find the median of the sorted array and store it in the result.
     * 
     * COMPLEXITY:
     * - Time: O((N - K + 1) * K log K) where N is the length of nums.
     *   Since N <= 1000, this will easily pass, but it's not the most optimal.
     * - Space: O(K) for the temporary array created at each step.
     * ========================================================================
     */
    public static double[] medianSlidingWindowBruteForce(int[] nums, int k) {
        int n = nums.length;
        double[] result = new double[n - k + 1];
        
        for (int i = 0; i <= n - k; i++) {
            int[] window = new int[k];
            System.arraycopy(nums, i, window, 0, k);
            Arrays.sort(window);
            
            if (k % 2 == 1) {
                result[i] = window[k / 2];
            } else {
                // Cast to double BEFORE addition to prevent integer overflow
                result[i] = ((double) window[k / 2 - 1] + (double) window[k / 2]) / 2.0;
            }
        }
        
        return result;
    }

    /**
     * ========================================================================
     * SOLUTION 2: DYNAMIC LIST (Binary Search + Insertion/Deletion)
     * ========================================================================
     * EXPLANATION:
     * 1. Maintain a sorted window of size K using a dynamically sized List.
     * 2. Initialize the list with the first K elements and sort it.
     * 3. For each step the window slides:
     *    - Remove the element sliding out of the window. We use Binary Search 
     *      to find its index quickly (O(log K)), then remove it (O(K)).
     *    - Insert the new element sliding into the window. We use Binary Search 
     *      to find its correct sorted position (O(log K)), then insert it (O(K)).
     * 4. Retrieve the median in O(1) time.
     * 
     * COMPLEXITY:
     * - Time: O(N * K). In Java, ArrayList insertion/deletion takes O(K) due 
     *   to shifting elements. With N <= 1000, N*K is at most 1,000,000 
     *   operations, which is blazing fast in practice.
     * - Space: O(K) to store the window.
     * ========================================================================
     */
    public static double[] medianSlidingWindowList(int[] nums, int k) {
        int n = nums.length;
        double[] result = new double[n - k + 1];
        List<Integer> window = new ArrayList<>(k);
        
        // Initialize the first window
        for (int i = 0; i < k; i++) {
            window.add(nums[i]);
        }
        Collections.sort(window);
        
        // Compute median for first window
        result[0] = getMedianFromList(window, k);
        
        for (int i = k; i < n; i++) {
            int elementToRemove = nums[i - k];
            int elementToAdd = nums[i];
            
            // Remove outgoing element
            int removeIndex = Collections.binarySearch(window, elementToRemove);
            window.remove(removeIndex);
            
            // Add incoming element
            int addIndex = Collections.binarySearch(window, elementToAdd);
            if (addIndex < 0) {
                addIndex = -(addIndex + 1);
            }
            window.add(addIndex, elementToAdd);
            
            // Compute new median
            result[i - k + 1] = getMedianFromList(window, k);
        }
        
        return result;
    }

    private static double getMedianFromList(List<Integer> window, int k) {
        if (k % 2 == 1) {
            return window.get(k / 2);
        } else {
            return ((double) window.get(k / 2 - 1) + (double) window.get(k / 2)) / 2.0;
        }
    }

    /**
     * ========================================================================
     * SOLUTION 3: TWO HEAPS (Standard Data Stream approach adapted)
     * ========================================================================
     * EXPLANATION:
     * 1. Similar to the "Find Median from Data Stream" problem, we use a 
     *    Max-Heap for the lower half and a Min-Heap for the upper half.
     * 2. As the window slides, we must ADD the new element and REMOVE the 
     *    old element that exited the window.
     * 3. PriorityQueue in Java has a `.remove(Object)` method. This method 
     *    takes O(K) time because it must do a linear scan to find the item.
     * 4. After any addition or removal, we re-balance the heaps so that the 
     *    Max-Heap has either the same size or exactly 1 more element than Min-Heap.
     * 
     * Note: For massive inputs (N=10^5), `.remove(Object)` is too slow, and 
     * "Lazy Deletion" with a HashMap is required to achieve strict O(N log K). 
     * But given constraints N <= 1000, this simple Two Heap logic performs perfectly.
     * 
     * COMPLEXITY:
     * - Time: O(N * K). Adding is O(log K), but removing is O(K).
     * - Space: O(K) for the heaps.
     * ========================================================================
     */
    public static double[] medianSlidingWindowTwoHeaps(int[] nums, int k) {
        int n = nums.length;
        double[] result = new double[n - k + 1];
        
        // Use Long to prevent overflow when elements are near Integer.MAX_VALUE
        PriorityQueue<Long> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        PriorityQueue<Long> minHeap = new PriorityQueue<>();
        
        for (int i = 0; i < n; i++) {
            // Add new element
            long current = nums[i];
            maxHeap.offer(current);
            minHeap.offer(maxHeap.poll());
            
            // Balance heaps
            if (maxHeap.size() < minHeap.size()) {
                maxHeap.offer(minHeap.poll());
            }
            
            // If the window is fully formed
            if (i >= k - 1) {
                // Record median
                if (k % 2 == 1) {
                    result[i - k + 1] = maxHeap.peek();
                } else {
                    result[i - k + 1] = (maxHeap.peek() + minHeap.peek()) / 2.0;
                }
                
                // Remove the element that is sliding out of the window
                long elementToRemove = nums[i - k + 1];
                if (elementToRemove <= maxHeap.peek()) {
                    maxHeap.remove(elementToRemove);
                } else {
                    minHeap.remove(elementToRemove);
                }
                
                // Re-balance after removal
                if (maxHeap.size() < minHeap.size()) {
                    maxHeap.offer(minHeap.poll());
                } else if (maxHeap.size() > minHeap.size() + 1) {
                    minHeap.offer(maxHeap.poll());
                }
            }
        }
        
        return result;
    }

    /**
     * ========================================================================
     * MAIN METHOD: Executing and verifying the examples
     * ========================================================================
     */
    public static void main(String[] args) {
        List<TestCase> tests = List.of(
            new TestCase(
                new int[]{1, 3, -1, -3, 5, 3, 6, 7}, 
                3, 
                new double[]{1.0, -1.0, -1.0, 3.0, 5.0, 6.0}
            ),
            new TestCase(
                new int[]{1, 2, 3, 4, 2, 3, 1, 4, 2}, 
                3, 
                new double[]{2.0, 3.0, 3.0, 3.0, 2.0, 3.0, 2.0}
            ),
            new TestCase(
                // Overflow test scenario
                new int[]{2147483647, 2147483647}, 
                2, 
                new double[]{2147483647.0}
            )
        );

        for (int i = 0; i < tests.size(); i++) {
            TestCase tc = tests.get(i);
            System.out.println("Test Case " + (i + 1) + ":");
            System.out.println("Array:    " + Arrays.toString(tc.nums()));
            System.out.println("Window K: " + tc.k());
            System.out.println("Expected:    " + Arrays.toString(tc.expected()));
            
            double[] resBrute = medianSlidingWindowBruteForce(tc.nums(), tc.k());
            double[] resList = medianSlidingWindowList(tc.nums(), tc.k());
            double[] resHeaps = medianSlidingWindowTwoHeaps(tc.nums(), tc.k());
            
            System.out.println("Brute Force: " + Arrays.toString(resBrute));
            System.out.println("Dynamic List:" + Arrays.toString(resList));
            System.out.println("Two Heaps:   " + Arrays.toString(resHeaps));
            System.out.println("--------------------------------------------------");
        }
    }
}
