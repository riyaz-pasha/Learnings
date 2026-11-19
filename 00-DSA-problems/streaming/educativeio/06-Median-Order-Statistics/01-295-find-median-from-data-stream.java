/**
 * 0. ## Problem Statement
 * The median is the middle value in an ordered integer list. If the size of the list is
 * even, there is no single middle value, and the median is the mean of the two middle values.
 *   - For [2, 3, 4], median is 3.0.
 *   - For [2, 3],    median is (2 + 3) / 2.0 = 2.5.
 *
 * Implement the `MedianFinder` class:
 * - `MedianFinder()`: Initializes the object.
 * - `void addNum(int num)`: Adds the integer `num` from the data stream.
 * - `double findMedian()`: Returns the median of all elements added so far
 *   (answers within 10^-5 of the actual answer are accepted).
 *
 * Constraints:
 * - -10^5 <= num <= 10^5
 * - At least one element exists before calling `findMedian()`.
 * - At most 5 * 10^4 total calls to `addNum` and `findMedian`.
 *
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "Can `findMedian()` ever be called when the stream is empty?"
 *    -> Prevents undefined return contracts (exception vs. 0.0 vs. NaN); constraints guarantee >= 1 element.
 * 2. "Can the stream contain duplicate or negative numbers, or full 32-bit `Integer.MIN_VALUE`/`MAX_VALUE`?"
 *    -> Prevents using a `TreeSet` (which drops duplicates), prevents `(b - a)` comparator overflow,
 *       and warns us to avoid `(a + b) / 2.0` int-addition overflow when averaging two extreme ints.
 * 3. "Will elements ever be removed from the stream, or is it strictly append-only?"
 *    -> Append-only allows pure `PriorityQueue`s; supporting removals would require lazy deletion
 *       maps (like LC 480) or balanced BSTs.
 * 4. "What is the expected read-to-write ratio (`findMedian` vs `addNum`), and is the value range bounded?"
 *    -> If values are bounded (e.g., `[0, 100]`), a counting array gives O(1) time and O(1) space;
 *       otherwise we optimize for balanced O(log N) writes and O(1) reads.
 * 5. "Is exact floating-point precision required, or is standard IEEE-754 `double` within 10^-5 sufficient?"
 *    -> Confirms `double` division (`2.0`) is sufficient and avoids `BigDecimal` overhead.
 * 6. "Can the stream grow indefinitely in production (e.g., billions of events)?"
 *    -> Clarifies whether exact median in O(N) RAM is feasible or if streaming approximation
 *       sketches (like t-digest / GK summary) would be needed in a system design extension.
 *
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: Up to 5 * 10^4 calls to `addNum` and `findMedian`.
 * Any approach that re-sorts on every `findMedian()` or shifts an array on every `addNum()`
 * degrades to O(N^2 log N) or O(N^2) (~10^9 operations), which falls over at scale.
 * We need O(log N) `addNum` and O(1) `findMedian`.
 *
 * --- Approach 1: Brute Force (Unsorted List + Re-Sort on Read) ---
 * 0. Idea/Intuition: Append every incoming number to an `ArrayList`. Whenever `findMedian()`
 *    is called, sort the entire list and read the middle element(s).
 * 1. What I'd naturally try: Literally mirror the definition of a median ("middle of sorted list").
 * 2. Why it works: Sorting all `N` elements before every read guarantees exact middle indices.
 * 3. Why it's too slow/costly: With 25,000 `addNum`s interleaved with 25,000 `findMedian`s,
 *    re-sorting O(N) elements on every read costs O(N^2 log N) (~10^10 comparisons -> TLE).
 * 4. What work is being repeated: The list was ALREADY sorted on the previous `findMedian()`!
 *    Re-sorting all `N` numbers just because one new element arrived throws away all prior work.
 * 5. Observation that removes the bottleneck: Keep the list sorted incrementally on every `addNum()`
 *    using binary search to find where the new number belongs.
 * 6. Leads to Approach 2: Maintained Sorted `ArrayList`.
 *    Time Complexity: `addNum`: O(1) amortized — appends to `ArrayList`.
 *    `findMedian`: O(N log N) — `Collections.sort()` sorts all N elements on every query.
 *    Space Complexity: O(N) — stores all N numbers in an `ArrayList`.
 *
 * --- Approach 2: Maintained Sorted `ArrayList` (Binary Search + Shift) ---
 * 0. Idea/Intuition: Keep `ArrayList<Integer>` sorted at all times. On `addNum(num)`, binary
 *    search for its insertion index in O(log N) and insert it there. On `findMedian()`, read
 *    the middle 1 or 2 indices directly in O(1).
 * 1. What I'd naturally try: "Don't re-sort — just binary-search the spot and insert!"
 * 2. Why it works: Maintaining sorted order makes `findMedian()` an instant O(1) array lookup.
 * 3. Why it's still too slow/costly: Finding the insertion index is O(log N), but
 *    `ArrayList.add(idx, num)` must shift up to `N` elements right in memory — O(N) per `addNum`!
 *    Across 5 * 10^4 calls, worst-case (reverse-sorted input) does ~1.25 * 10^9 array shifts.
 * 4. What work is being repeated: We are maintaining the FULL sorted order of EVERY element
 *    from rank 1 to rank N — even though `findMedian()` ONLY EVER looks at the 1 or 2 elements
 *    at the exact midpoint (`N/2`)! We don't care whether the 1st-smallest is before the 5th-smallest,
 *    or whether the 90th-largest is before the 99th-largest!
 * 5. Observation that removes the bottleneck: We only need to partition the stream into TWO halves:
 *      - `lowerHalf`: the smaller `ceil(N / 2)` elements.
 *        What is the ONLY element we ever need from `lowerHalf`? Its **MAXIMUM**!
 *      - `upperHalf`: the larger `floor(N / 2)` elements.
 *        What is the ONLY element we ever need from `upperHalf`? Its **MINIMUM**!
 *    If we maintain those two halves with sizes differing by at most 1, the median is ALWAYS
 *    sitting right at the two roots (`lowerHalf.peek()` and `upperHalf.peek()`)!
 * 6. Leads to Approach 3: Two Opposing Heaps (Max-Heap for `lowerHalf`, Min-Heap for `upperHalf`).
 *    Time Complexity: `addNum`: O(N) — O(log N) binary search, dominated by O(N) `ArrayList` shifts.
 *    `findMedian`: O(1) — direct index lookup at `N / 2` (and `N / 2 - 1`).
 *    Space Complexity: O(N) — stores all N elements in the `ArrayList`.
 *
 * --- Approach 3: Two Opposing Heaps (Optimal for General Streams) ---
 * 0. Idea/Intuition: Maintain two `PriorityQueue<Integer>`s:
 *    - `lowerHalf` (Max-Heap): holds the smaller half of the stream; root = largest of small half.
 *    - `upperHalf` (Min-Heap): holds the larger half of the stream; root = smallest of large half.
 *    Enforce two invariants after every `addNum(num)`:
 *      (1) Size invariant: `lowerHalf.size() == upperHalf.size()` (even N)
 *                       OR `lowerHalf.size() == upperHalf.size() + 1` (odd N).
 *      (2) Order invariant: `lowerHalf.peek() <= upperHalf.peek()`.
 *    How to preserve BOTH invariants in 3 lines with zero messy branching:
 *      1. `lowerHalf.offer(num);`             // Tentatively place `num` in lower half.
 *      2. `upperHalf.offer(lowerHalf.poll());` // Push lower's largest to upper (fixes order!).
 *      3. `if (lowerHalf.size() < upperHalf.size()) lowerHalf.offer(upperHalf.poll());` // Fix size!
 *    Then `findMedian()` is simply:
 *      - Odd N (`lowerHalf.size() > upperHalf.size()`): return `lowerHalf.peek()`.
 *      - Even N: return `((double) lowerHalf.peek() + upperHalf.peek()) / 2.0`.
 * 1. Why someone thinks of it: Whenever you only need fast access to the boundary between the
 *    bottom half and top half of a dynamic stream, two opposing heaps expose both sides of the
 *    cut in O(1) without sorting either half.
 * 2. Why it works: Step 1+2 guarantees every element in `lowerHalf` is `<=` every element in
 *    `upperHalf` (even if `num` was huge, it bounces straight through `lowerHalf` into `upperHalf`).
 *    Step 3 guarantees `lowerHalf` always has `ceil(N/2)` elements and `upperHalf` has `floor(N/2)`.
 *    Time Complexity:
 *    - `addNum`: O(log N) — at most 3 heap operations (`offer`/`poll`) on heaps of size `<= N/2`.
 *    - `findMedian`: O(1) — reads at most 2 heap roots (`peek()`).
 *    Space Complexity: O(N) — every added number is stored in exactly one of the two heaps.
 *
 * --- Approach 4: Counting Bucket Array (Optimal ONLY for Follow-up 1: Bounded `[0, 100]`) ---
 * 0. Idea/Intuition: If all numbers lie in `[0, 100]`, keep `int[] counts = new int[101]` and
 *    `int total`. `addNum(num)` just increments `counts[num]++` and `total++`. `findMedian()`
 *    walks `v = 0..100` accumulating counts until reaching ranks `(total + 1) / 2` and `(total + 2) / 2`.
 * 1. Trade-off vs. Two Heaps: Beats Two Heaps completely when the value domain `V` is tiny
 *    (O(1) time and O(1) space regardless of how many billions of numbers arrive!), but falls
 *    apart if `V` is unbounded (e.g., full 32-bit ints).
 *    Time Complexity: `addNum`: O(1); `findMedian`: O(V) = O(101) = O(1).
 *    Space Complexity: O(V) = O(101) = O(1) — constant memory even for 10^9 elements!
 *
 * Interview Verdict:
 * Write Approach 3 (Two Opposing Heaps) in a 45-minute interview. The 3-line balancing dance
 * (`lower -> upper -> rebalance if lower < upper`) eliminates every `if (num < peek)` null-check
 * bug, hits optimal O(log N) `addNum` and O(1) `findMedian`, and is the canonical solution
 * interviewers expect. Then bring up Approach 4 when tackling the `[0, 100]` follow-ups.
 *
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - Single element (`[42]`): `lowerHalf = [42]`, `upperHalf = []` -> returns `42.0` (must NOT peek empty `upperHalf`).
 * - Two elements (`[1, 2]`): Even size -> returns `1.5` (floating-point division, not integer truncation `1`).
 * - Negative numbers (`[-1, -2, -3, -4]`): Ordering works identically; `[-3, -2]` -> `-2.5`.
 * - All duplicate elements (`[5, 5, 5, 5]`): Both heaps hold `5`s cleanly without dropping duplicates.
 * - Strictly increasing (`[1, 2, 3, 4, 5]`) & strictly decreasing (`[5, 4, 3, 2, 1]`): Stress-tests
 *   the cross-heap bounce on every single insertion.
 * - Extreme 32-bit values (`[Integer.MAX_VALUE, Integer.MAX_VALUE]`): `(a + b) / 2.0` would
 *   overflow `int` addition BEFORE dividing by `2.0`! Casting `((double) a + b) / 2.0` (or
 *   `((long) a + b) / 2.0`) prevents silent overflow.
 *
 * =========================================================================================
 * 4. ## Also include
 *
 * -- Key insight --
 * Question to ask yourself: "To find the median, do I actually need the whole stream sorted,
 * or do I only need to know which half each number belongs to and what sits at the border?"
 * Correctness intuition: If `lowerHalf` has the smaller `ceil(N/2)` numbers and `upperHalf`
 * has the larger `floor(N/2)` numbers, then no matter how scrambled the elements are *inside*
 * each half, the middle 1 or 2 values of the globally sorted array MUST be `max(lowerHalf)`
 * and `min(upperHalf)`. Two opposing heaps maintain those exact two border values in O(log N).
 *
 * -- Examples & ASCII Diagram --
 * Stream: [6, 10, 2, 6, 5, 0] -> Sorted: [0, 2, 5, 6, 6, 10]
 *
 *   SMALLEST <------------------------------------------> LARGEST
 *
 *        lowerHalf (Max-Heap)           upperHalf (Min-Heap)
 *        holds smaller half             holds larger half
 *              [5]  <--- max border --->  [6]
 *             /   \                      /   \
 *           [0]   [2]                  [6]   [10]
 *
 *   Both sizes == 3 (even N = 6) -> Median = (5 + 6) / 2.0 = 5.5!
 *   Notice: Neither half is sorted internally — only the two roots facing the center matter.
 *
 * -- Dry run 1: Optimal Two Heaps on [1, 2, 3, 0, 5] --
 *   Step | Call       | 1. offer lower -> poll to upper | 2. rebalance (if L < U) | lower (Max) | upper (Min) | Median
 *   -----+------------+---------------------------------+-------------------------+-------------+-------------+-------
 *    1   | addNum(1)  | L:[1] -> U:[1]                  | L(0)<U(1): move 1 to L  | [1]         | []          | 1.0
 *    2   | addNum(2)  | L:[2,1] -> move 2 -> U:[2]      | L(1)==U(1): ok          | [1]         | [2]         | 1.5
 *    3   | addNum(3)  | L:[3,1] -> move 3 -> U:[2,3]    | L(1)<U(2): move 2 to L  | [2,1]       | [3]         | 2.0
 *    4   | addNum(0)  | L:[2,1,0] -> move 2 -> U:[2,3]  | L(2)==U(2): ok          | [1,0]       | [2,3]       | 1.5
 *    5   | addNum(5)  | L:[5,1,0] -> move 5 -> U:[2,3,5]| L(2)<U(3): move 2 to L  | [2,1,0]     | [3,5]       | 2.0
 *
 * -- Dry run 2: Edge Case — Strictly Decreasing Negative Stream [-1, -2, -3, -4] --
 *   Step | Call        | lowerHalf (Max-Heap) | upperHalf (Min-Heap) | findMedian()
 *   -----+-------------+----------------------+----------------------+-------------
 *    1   | addNum(-1)  | [-1]                 | []                   | -1.0
 *    2   | addNum(-2)  | [-2]                 | [-1]                 | -1.5
 *    3   | addNum(-3)  | [-2, -3]             | [-1]                 | -2.0
 *    4   | addNum(-4)  | [-3, -4]             | [-2, -1]             | -2.5
 *
 * -- Pitfalls --
 * 1. Integer division & addition overflow in Java:
 *    - `(lowerHalf.peek() + upperHalf.peek()) / 2` truncates `3 / 2` to `1.0` instead of `1.5`!
 *    - `(lowerHalf.peek() + upperHalf.peek()) / 2.0` STILL overflows if both are `Integer.MAX_VALUE`
 *      because Java evaluates `int + int` before widening to `double`!
 *    - Safe formula: `((double) lowerHalf.peek() + upperHalf.peek()) / 2.0` (or `lower / 2.0 + upper / 2.0`).
 * 2. Comparator overflow in Java Max-Heap:
 *    - Writing `new PriorityQueue<>((a, b) -> b - a)` overflows on `Integer.MAX_VALUE - (-1)`!
 *    - Always use `Collections.reverseOrder()` or `Comparator.reverseOrder()`.
 * 3. Forgetting the order invariant when balancing only by size:
 *    - Tempting bug: `if (lower.size() <= upper.size()) lower.offer(num); else upper.offer(num);`
 *    - Fails immediately on `[10, 1]`: `10` goes to `lower`, `1` goes to `upper`, violating `lower <= upper`!
 *    - Routing `num` through `lower -> upper` before rebalancing guarantees the order invariant EVERY time.
 *
 * -- Pattern recognition --
 * When you see: "Maintain the median (or a moving rank split) in an online data stream"
 * -> Think **Two Opposing Heaps** (Max-Heap for lower partition, Min-Heap for upper partition).
 * Transfers directly to:
 *   - Sliding Window Median (LeetCode 480) — Two Opposing Heaps + Lazy Deletion HashMaps
 *   - Sequentially Ordinal Rank Tracker (LeetCode 2102) — Two Opposing Heaps split at rank `i` instead of `N/2`
 *   - IPO (LeetCode 502) — Two heaps separating affordable vs. unaffordable projects
 *
 * -- Interview script --
 * "Sorting the stream on every `findMedian()` costs O(N log N) per read, and keeping a sorted
 * `ArrayList` still costs O(N) per `addNum()` due to shifting. The key observation is that we
 * never need the entire stream sorted — we only need to partition it into a smaller half and a
 * larger half so we can read the two border elements in O(1). I'll use a max-heap `lowerHalf`
 * for the smaller half and a min-heap `upperHalf` for the larger half, keeping `lowerHalf` equal
 * in size to `upperHalf` or larger by 1. On `addNum(num)`, I push `num` into `lowerHalf`, move
 * `lowerHalf`'s maximum over to `upperHalf` to preserve the ordering invariant, and if `lowerHalf`
 * becomes smaller than `upperHalf`, I move `upperHalf`'s minimum back to `lowerHalf`. Then
 * `findMedian()` just reads the heap root(s) in O(1). That gives us O(log N) time for `addNum`,
 * O(1) time for `findMedian`, and O(N) space."
 *
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q1 (Official Follow-up 1): If ALL integers from the stream are in `[0, 100]`, how would you optimize?
 * A1: Replace the two heaps with a fixed counting array `int[] counts = new int[101]` and a
 *     `total` counter (implemented below as `MedianFinderBoundedBuckets`).
 *     - `addNum(num)`: `counts[num]++; total++;` -> **O(1) time**.
 *     - `findMedian()`: Walk `0..100` accumulating prefix counts until hitting the middle rank(s)
 *       `(total + 1) / 2` and `(total + 2) / 2` -> **O(101) = O(1) time**.
 *     - Space: **O(101) = O(1)** even if 10 billion numbers stream in!
 *
 * Q2 (Official Follow-up 2): If 99% of integers are in `[0, 100]`, how would you optimize?
 * A2: Hybrid architecture! Keep `int[] counts = new int[101]` for `[0, 100]`, PLUS:
 *     - `int belowCount` (and optionally a `TreeMap<Integer, Integer>` or sorted list for `< 0`)
 *     - `int aboveCount` (and optionally a `TreeMap<Integer, Integer>` or sorted list for `> 100`)
 *     Notice a mathematical gem: if strictly > 50% (here 99%!) of elements lie in `[0, 100]`,
 *     the median is **mathematically guaranteed** to lie inside `[0, 100]`! So in the 99% case
 *     we don't even need to store the exact values of outliers — just `belowCount` and `aboveCount`
 *     (starting our cumulative count at `belowCount` before scanning `counts[0..100]`).
 *     To be 100% safe against adversarial prefixes where early elements are all outliers before the
 *     99% distribution kicks in, store the rare `< 0` and `> 100` outliers in two small `TreeMap`s
 *     (only ~1% of N memory) and fall back to them only if the target rank lands outside `[0, 100]`.
 *
 * Q3: What if we also need to support `removeNum(int num)` (e.g., Sliding Window Median, LC 480)?
 * A3: Java's `PriorityQueue.remove(obj)` is O(N). Add two lazy-deletion frequency maps
 *     (`lazyLower`, `lazyUpper`) plus logical valid-size counters, and prune stale roots before
 *     every `peek()` or `poll()` rebalance — keeping `addNum` and `removeNum` at O(log N) amortized.
 * =========================================================================================
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class FindMedianFromDataStreamStudyNote {

    // ----------------------------------------------------------------------
    // APPROACH 1: Brute Force (O(1) addNum, O(N log N) findMedian, O(N) space)
    // ----------------------------------------------------------------------
    static class MedianFinderBruteForce {
        private final List<Integer> nums = new ArrayList<>();

        public void addNum(int num) {
            nums.add(num);
        }

        public double findMedian() {
            Collections.sort(nums); // Bottleneck: re-sorts all N elements on every query.
            int n = nums.size();
            if (n % 2 == 1) {
                return nums.get(n / 2);
            }
            // Cast to double BEFORE adding so extreme 32-bit ints never overflow.
            return ((double) nums.get(n / 2 - 1) + nums.get(n / 2)) / 2.0;
        }
    }

    // ----------------------------------------------------------------------
    // APPROACH 2: Maintained Sorted ArrayList (O(N) addNum, O(1) findMedian, O(N) space)
    // ----------------------------------------------------------------------
    static class MedianFinderSortedList {
        // Always maintained in ascending sorted order.
        // Example after addNum(3), addNum(1), addNum(2): sorted = [1, 2, 3].
        private final List<Integer> sorted = new ArrayList<>();

        public void addNum(int num) {
            int idx = Collections.binarySearch(sorted, num);
            if (idx < 0) {
                // Convert binarySearch's negative encoding `-(insertionPoint) - 1` back to index.
                idx = -(idx + 1);
            }
            // Bottleneck: ArrayList.add(idx, num) shifts up to N elements right in O(N).
            sorted.add(idx, num);
        }

        public double findMedian() {
            int n = sorted.size();
            if (n % 2 == 1) {
                return sorted.get(n / 2);
            }
            return ((double) sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
        }
    }

    // ----------------------------------------------------------------------
    // APPROACH 3: Two Opposing Heaps (OPTIMAL: O(log N) addNum, O(1) findMedian, O(N) space)
    // ----------------------------------------------------------------------
    static class MedianFinderTwoHeaps {
        // Max-Heap holding the smaller half of all elements seen so far.
        // Example for [0, 2, 5, 6, 6, 10]: lowerHalf = [5 (root), 2, 0].
        // Using Collections.reverseOrder() avoids integer-overflow bugs in `(a, b) -> b - a`.
        private final PriorityQueue<Integer> lowerHalf = new PriorityQueue<>(Collections.reverseOrder());

        // Min-Heap holding the larger half of all elements seen so far.
        // Example for [0, 2, 5, 6, 6, 10]: upperHalf = [6 (root), 6, 10].
        private final PriorityQueue<Integer> upperHalf = new PriorityQueue<>();

        public void addNum(int num) {
            // Step 1: Tentatively insert `num` into lowerHalf.
            lowerHalf.offer(num);

            // Step 2: Restore ORDER invariant (`max(lowerHalf) <= min(upperHalf)`).
            // Pushing lowerHalf's largest element over to upperHalf guarantees that if `num`
            // actually belonged in the upper half, it immediately crosses over.
            upperHalf.offer(lowerHalf.poll());

            // Step 3: Restore SIZE invariant (`lowerHalf.size()` is `upperHalf.size()` or `+ 1`).
            // If upperHalf now has more elements, promote its smallest back to lowerHalf.
            if (lowerHalf.size() < upperHalf.size()) {
                lowerHalf.offer(upperHalf.poll());
            }
        }

        public double findMedian() {
            // Odd total count: lowerHalf holds the exact extra middle element at its root.
            if (lowerHalf.size() > upperHalf.size()) {
                return lowerHalf.peek();
            }
            // Even total count: average the two border roots (cast to double before summing).
            return ((double) lowerHalf.peek() + upperHalf.peek()) / 2.0;
        }
    }

    // ----------------------------------------------------------------------
    // APPROACH 4 (Follow-Up 1): Counting Array for Bounded Range [0, 100]
    // (O(1) addNum, O(101) = O(1) findMedian, O(101) = O(1) space)
    // ----------------------------------------------------------------------
    static class MedianFinderBoundedBuckets {
        // counts[v] = how many times value `v` (0 <= v <= 100) has appeared in the stream.
        // Example for [1, 2, 2, 3]: counts[1]=1, counts[2]=2, counts[3]=1, total=4.
        private final int[] counts = new int[101];
        private int total = 0;

        public void addNum(int num) {
            counts[num]++;
            total++;
        }

        public double findMedian() {
            // Unified 1-indexed middle ranks for both odd and even `total`:
            //   total=3 -> leftRank=2, rightRank=2 (same element!)
            //   total=4 -> leftRank=2, rightRank=3 (the two middle elements!)
            int leftRank = (total + 1) / 2;
            int rightRank = (total + 2) / 2;

            int seen = 0;
            int leftVal = -1;
            int rightVal = -1;

            for (int v = 0; v <= 100; v++) {
                seen += counts[v];
                if (leftVal == -1 && seen >= leftRank) {
                    leftVal = v;
                }
                if (seen >= rightRank) {
                    rightVal = v;
                    break; // Both middle values found; stop scanning early.
                }
            }
            return (leftVal + rightVal) / 2.0;
        }
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS — Cross-checking all approaches on representative + edge cases
    // ----------------------------------------------------------------------
    private static boolean approxEqual(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (Math.abs(a.get(i) - b.get(i)) > 1e-5) return false;
        }
        return true;
    }

    private static void verifyCase(String name, int[] stream, List<Double> expected, boolean testBuckets) {
        MedianFinderBruteForce brute = new MedianFinderBruteForce();
        MedianFinderSortedList sorted = new MedianFinderSortedList();
        MedianFinderTwoHeaps heaps = new MedianFinderTwoHeaps();
        MedianFinderBoundedBuckets buckets = testBuckets ? new MedianFinderBoundedBuckets() : null;

        List<Double> rBrute = new ArrayList<>();
        List<Double> rSorted = new ArrayList<>();
        List<Double> rHeaps = new ArrayList<>();
        List<Double> rBuckets = new ArrayList<>();

        for (int num : stream) {
            brute.addNum(num);
            sorted.addNum(num);
            heaps.addNum(num);
            rBrute.add(brute.findMedian());
            rSorted.add(sorted.findMedian());
            rHeaps.add(heaps.findMedian());
            if (buckets != null) {
                buckets.addNum(num);
                rBuckets.add(buckets.findMedian());
            }
        }

        boolean pass = approxEqual(rBrute, expected)
                && approxEqual(rSorted, expected)
                && approxEqual(rHeaps, expected)
                && (!testBuckets || approxEqual(rBuckets, expected));

        System.out.printf("%-34s | %s | Output: %s%n",
                name, pass ? "PASS" : "FAIL", rHeaps);

        if (!pass) {
            System.out.println("  Expected: " + expected);
            System.out.println("  Brute:    " + rBrute);
            System.out.println("  Sorted:   " + rSorted);
            System.out.println("  TwoHeaps: " + rHeaps);
            if (testBuckets) System.out.println("  Buckets:  " + rBuckets);
        }
    }

    public static void main(String[] args) {
        verifyCase("LC Example 1",
                new int[]{1, 2, 3},
                List.of(1.0, 1.5, 2.0), true);

        verifyCase("Typical mixed stream",
                new int[]{6, 10, 2, 6, 5, 0},
                List.of(6.0, 8.0, 6.0, 6.0, 6.0, 5.5), true);

        verifyCase("Edge: Single element",
                new int[]{42},
                List.of(42.0), true);

        verifyCase("Edge: All duplicates",
                new int[]{5, 5, 5, 5},
                List.of(5.0, 5.0, 5.0, 5.0), true);

        verifyCase("Edge: Negative decreasing stream",
                new int[]{-1, -2, -3, -4},
                List.of(-1.0, -1.5, -2.0, -2.5), false);

        verifyCase("Edge: Extreme 32-bit overflow",
                new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE},
                List.of((double) Integer.MAX_VALUE, (double) Integer.MAX_VALUE, (double) Integer.MAX_VALUE),
                false);
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 *
 * - Core pattern: Two Opposing Heaps (`lowerHalf` Max-Heap, `upperHalf` Min-Heap).
 * - Key observation: Finding the median never requires sorting the full stream — only
 *   maintaining the exact border between the smaller `ceil(N/2)` and larger `floor(N/2)` elements.
 * - Worth memorizing vs. understanding: Understand *why* routing every element `lower -> upper`
 *   then rebalancing `if (lower.size() < upper.size())` preserves both the order and size
 *   invariants simultaneously; memorize casting to `double` before summing two roots.
 * - Most common trap: Balancing solely by heap size (which breaks `max(lower) <= min(upper)`),
 *   or writing `(a, b) -> b - a` / `(a + b) / 2` (overflow & integer truncation traps in Java).
 * - One-line mental trigger: "Dynamic median from stream? Max-heap lower + Min-heap upper: offer lower, bounce max to upper, rebalance."
 * =========================================================================================
 */









































import java.util.*;

class MedianFinder {

    /*
     * MAX HEAP:
     *
     * Stores the smaller half of all incoming numbers.
     *
     * The largest element of the smaller half is always
     * available at the root.
     *
     * This element is one of the two possible median candidates.
     */
    private final PriorityQueue<Integer> maxHeap =
            new PriorityQueue<>(Comparator.reverseOrder());

    /*
     * MIN HEAP:
     *
     * Stores the larger half of all incoming numbers.
     *
     * The smallest element of the larger half is always
     * available at the root.
     *
     * This element is the other possible median candidate.
     */
    private final PriorityQueue<Integer> minHeap =
            new PriorityQueue<>();

    public MedianFinder() {

        /*
         * Initially, both heaps are empty.
         *
         * We maintain two invariants throughout the algorithm:
         *
         * 1. ORDERING INVARIANT:
         *
         *    Every element in maxHeap must be <= every element
         *    in minHeap.
         *
         *    Therefore:
         *
         *    maxHeap.peek() <= minHeap.peek()
         *
         *    whenever both heaps are non-empty.
         *
         * 2. SIZE INVARIANT:
         *
         *    maxHeap.size() == minHeap.size()
         *
         *    OR
         *
         *    maxHeap.size() == minHeap.size() + 1
         *
         *    We intentionally keep the extra element in maxHeap.
         */
    }

    public void addNum(int num) {

        /*
         * STEP 1: Insert the incoming number into maxHeap.
         *
         * Why maxHeap?
         *
         * We want maxHeap to contain the smaller half.
         * Its root should represent the largest element
         * among the smaller numbers.
         */
        maxHeap.offer(num);

        /*
         * STEP 2: Restore the ordering invariant.
         *
         * Consider:
         *
         * maxHeap = [10, 5, 3]
         * minHeap = [7, 12]
         *
         * Here:
         *
         * maxHeap.peek() = 10
         * minHeap.peek() = 7
         *
         * This violates our ordering invariant.
         *
         * We move the largest element of maxHeap into minHeap.
         *
         * After moving:
         *
         * maxHeap = [7, 5, 3]
         * minHeap = [10, 12]
         *
         * Now every element in maxHeap is <= every element
         * in minHeap.
         *
         * Notice that moving the root is sufficient because
         * the heaps already maintain their internal ordering.
         */
        if (!minHeap.isEmpty()
                && maxHeap.peek() > minHeap.peek()) {

            minHeap.offer(maxHeap.poll());
        }

        /*
         * STEP 3: Restore the size invariant.
         *
         * Our desired condition is:
         *
         * maxHeap.size() == minHeap.size()
         *
         * OR
         *
         * maxHeap.size() == minHeap.size() + 1
         *
         * If minHeap becomes larger, move its smallest
         * element into maxHeap.
         *
         * This ensures that:
         *
         * - Both heaps have equal sizes, OR
         * - maxHeap has exactly one extra element.
         */
        if (maxHeap.size() < minHeap.size()) {

            maxHeap.offer(minHeap.poll());
        }
    }

    public double findMedian() {

        /*
         * CASE 1: Odd number of elements.
         *
         * Since maxHeap is allowed to contain one extra element,
         * its root represents the middle element.
         *
         * Example:
         *
         * Sorted: [1, 2, 3, 4, 5]
         *
         * maxHeap: [3, 2, 1]
         * minHeap: [4, 5]
         *
         * Median = maxHeap.peek() = 3.
         */
        if (maxHeap.size() > minHeap.size()) {

            return maxHeap.peek();
        }

        /*
         * CASE 2: Even number of elements.
         *
         * Both heaps contain the same number of elements.
         *
         * The two middle elements are:
         *
         * maxHeap.peek() -> Largest element in smaller half.
         * minHeap.peek() -> Smallest element in larger half.
         *
         * Median = Average of these two elements.
         *
         * Cast to long BEFORE addition to prevent integer overflow.
         *
         * Example:
         *
         * maxHeap.peek() = 3
         * minHeap.peek() = 4
         *
         * Median = (3 + 4) / 2.0 = 3.5.
         */
        return ((long) maxHeap.peek()
                + minHeap.peek()) / 2.0;
    }
}

/*
 * ==========================================
 * COMPLEXITY ANALYSIS
 * ==========================================
 *
 * Let n = number of elements currently in the stream.
 *
 * addNum():
 *
 * 1. maxHeap.offer(num):
 *    O(log n).
 *
 * 2. maxHeap.poll() / minHeap.offer():
 *    O(log n) in the worst case.
 *
 * 3. minHeap.poll() / maxHeap.offer():
 *    O(log n) in the worst case.
 *
 * There are only a constant number of heap operations.
 *
 * Overall: O(log n).
 *
 *
 * findMedian():
 *
 * Only accesses the roots and compares heap sizes.
 *
 * PriorityQueue.peek() = O(1).
 *
 * Overall: O(1).
 *
 *
 * SPACE:
 *
 * Every incoming number is stored in exactly one heap.
 *
 * maxHeap + minHeap = n elements.
 *
 * Overall: O(n).
 *
 *
 * FINAL:
 *
 * addNum()     -> O(log n)
 * findMedian() -> O(1)
 * Space        -> O(n)
 */


