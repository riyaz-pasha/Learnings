/**
 * 0. ## Problem Statement
 * The median is the middle value in an ordered integer list. If the size of the list is
 * even, there is no single middle value, so the median is the mean of the two middle values.
 *   - For [2, 3, 4],    median is 3.0.
 *   - For [1, 2, 3, 4], median is (2 + 3) / 2.0 = 2.5.
 *
 * You are given an integer array `nums` and an integer `k`. A sliding window of size `k`
 * moves from the very left of `nums` to the very right, one position at a time.
 * Return a `double[]` array containing the median of each window position (length `n - k + 1`).
 * Answers within `10^-5` of the actual answer are accepted.
 *
 * Example:
 *   nums = [1, 3, -1, -3, 5, 3, 6, 7], k = 3
 *   Output: [1.0, -1.0, -1.0, 3.0, 5.0, 6.0]
 *
 * Constraints:
 * - 1 <= k <= nums.length <= 10^5
 * - -2^31 <= nums[i] <= 2^31 - 1   (FULL 32-bit signed integer range!)
 *
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "Can `nums[i]` span the full 32-bit signed integer range (`Integer.MIN_VALUE` to `Integer.MAX_VALUE`)?"
 *    -> Critical! Forces `Integer.compare()` in every comparator (never `a - b`) and widening
 *       to `double` BEFORE adding the two middle elements (never `(a + b) / 2.0`).
 * 2. "Can `nums` contain duplicate values inside the same window?"
 *    -> Prevents using a naive `TreeSet<Integer>` of values (which collapses duplicates); forces
 *       either storing unique INDICES in a `TreeSet` or frequency counts in a `TreeMap`.
 * 3. "Can `k` be 1 or equal to `nums.length`?"
 *    -> When `k == 1`, the upper partition is empty on every step, testing empty-partition guards.
 * 4. "Are we allowed to mutate the input `nums` array, or must it remain read-only?"
 *    -> Confirms we must copy/index into auxiliary structures rather than sorting `nums` in-place.
 * 5. "Is strict O(K) memory required (e.g., for an infinite stream), or is O(N) auxiliary memory acceptable?"
 *    -> Directly chooses between Two Balanced BSTs (strict O(K) space) vs. Two Heaps + Lazy Deletion
 *       (O(N) worst-case space if stale tombstones sit below heap roots).
 * 6. "What precision is required for even-length medians?"
 *    -> 10^-5 tolerance confirms IEEE-754 `double` is sufficient (no `BigDecimal` needed).
 *
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: `n <= 10^5` and `k` can be up to `n` (e.g., `k = 50,000`).
 * Unlike "Find Median from Data Stream" (LC 295), elements don't just ENTER — one element
 * LEAVES the window on every slide. Any approach that re-sorts or scans O(K) per slide hits
 * `~2.5 * 10^9` operations (TLE). We need O(log K) (or O(log N)) per slide.
 *
 * --- Approach 1: Brute Force (Copy & Sort Every Window) ---
 * 0. Idea/Intuition: For each of the `n - k + 1` windows, copy `nums[i .. i + k - 1]` into a
 *    temp array of length `k`, sort it with `Arrays.sort()`, and read the middle 1 or 2 values.
 * 1. What I'd naturally try: Literally apply the definition of median to each window independently.
 * 2. Why it works: Every window is freshly copied and sorted, so no stale elements can leak in.
 * 3. Why it's too slow/costly: When `n = 10^5` and `k = 5 * 10^4`, we run `50,000` sorts of
 *    `50,000` elements -> `~4 * 10^10` comparisons (massive TLE).
 * 4. What work is being repeated: Adjacent windows `[i .. i+k-1]` and `[i+1 .. i+k]` share
 *    `k - 1` identical elements that were ALREADY sorted! Throwing away the sorted window and
 *    re-sorting all `k` elements from scratch repeats O(K log K) work to handle 1 delete + 1 insert.
 * 5. Observation that removes the bottleneck: Maintain the window in sorted order across slides:
 *    binary search to remove the 1 outgoing element, binary search to insert the 1 incoming element.
 * 6. Leads to Approach 2: Maintained Sorted Window (`ArrayList`).
 *    Time Complexity: O((N - K + 1) * K log K) = O(N * K log K) — because each of the `N - K + 1`
 *    windows copies and sorts `K` elements.
 *    Space Complexity: O(K) auxiliary — for the temporary `int[k]` window copy (excluding output).
 *
 * --- Approach 2: Maintained Sorted `ArrayList` (Binary Search + Shift) ---
 * 0. Idea/Intuition: Keep an `ArrayList<Integer>` of size `k` sorted ascending at all times.
 *    On each slide: binary search for `nums[i - k]` and remove it; binary search for `nums[i]`
 *    and insert it; read index `k / 2` (and `k / 2 - 1` if even) in O(1).
 * 1. What I'd naturally try: "Don't re-sort `k - 1` already-sorted items — just delete 1 and insert 1!"
 * 2. Why it works: Maintaining sorted order as an invariant guarantees O(1) median lookup.
 * 3. Why it's still too slow/costly: Finding each position is O(log K), but `ArrayList.remove(idx)`
 *    and `ArrayList.add(idx, val)` shift up to `K` elements in memory -> O(K) per slide!
 *    For `n = 10^5, k = 5 * 10^4`, that's `~2.5 * 10^9` array shifts (TLE).
 * 4. What work is being repeated: Shifting elements in a contiguous array just to keep ALL `k`
 *    elements totally ordered — even though we ONLY ever read the 1 or 2 elements at the exact
 *    midpoint (`k / 2`)!
 * 5. Observation that removes the bottleneck: Recall "Find Median from Data Stream" (LC 295):
 *    Two Opposing Heaps (`lower` Max-Heap + `upper` Min-Heap) expose the exact midpoint in O(1)
 *    with O(log K) inserts. The ONLY catch is deleting an arbitrary outgoing element `nums[i - k]`
 *    that might be buried deep inside a heap — Java's `PriorityQueue.remove(obj)` is O(K)!
 *    How do we avoid O(K) mid-heap removal?
 *    Two distinct ways:
 *      (a) **Lazy Deletion** (Approach 3): Don't search the heap — record `nums[i - k]` in a
 *          tombstone frequency map and only `poll()` it when it naturally surfaces to `peek()`.
 *      (b) **Balanced BSTs** (Approach 4): Replace both heaps with Red-Black Trees (`TreeSet`
 *          storing indices), which support TRUE O(log K) arbitrary removal!
 * 6. Leads to Approach 3 (Two Heaps + Lazy Deletion) and Approach 4 (Two TreeSets of Indices).
 *    Time Complexity: O(N * K) — O(log K) binary searches, dominated by O(K) `ArrayList` shifting
 *    on every slide.
 *    Space Complexity: O(K) auxiliary — stores strictly the `K` active window elements.
 *
 * --- Approach 3: Two Opposing Heaps + Lazy Deletion ---
 * 0. Idea/Intuition: Maintain `lower` (Max-Heap) and `upper` (Min-Heap), plus two tombstone
 *    maps `delayedLower` and `delayedUpper` and logical valid-size counters `lowerSize`, `upperSize`.
 *    On each slide:
 *    (1) Insert `nums[i]` into `lower` or `upper`, incrementing its logical size.
 *    (2) Mark `nums[i - k]` as lazily deleted in whichever heap owns it (`<= lower.peek()` ->
 *        `delayedLower`, else `delayedUpper`), decrementing its logical size.
 *    (3) Rebalance logical sizes (`lowerSize == upperSize` or `upperSize + 1`), pruning any
 *        tombstones at a heap's root before every `poll()` or `peek()`.
 * 1. What I'd naturally try: Extend LC 295's Two Heaps to support sliding-window deletions
 *    without paying O(K) for `PriorityQueue.remove()`.
 * 2. Why it works: Tombstones buried below the root cannot affect the median; the moment one
 *    reaches `peek()`, `prune()` discards it in O(log N) before any decision or read uses it.
 *    Keeping separate `delayedLower` / `delayedUpper` maps prevents one heap from stealing a
 *    duplicate boundary tombstone meant for the other heap.
 * 3. Practical trade-off vs. Approach 4: Stale elements can pile up inside the heaps if they
 *    never surface to the root (e.g., tiny numbers buried deep in `lower` while the window slides
 *    through large numbers), so heap sizes can grow up to O(N) instead of O(K). Also, tracking
 *    logical sizes + separate tombstone maps + prune calls has more moving parts to get right.
 *    Time Complexity: O(N log N) — each of the N elements is pushed once and popped at most once
 *    from heaps that can physically hold up to N elements (including tombstones).
 *    Space Complexity: O(N) auxiliary — heaps and tombstone maps can retain up to N entries
 *    across the full array if stale elements stay buried below the roots.
 *
 * --- Approach 4: Two Balanced BSTs (`TreeSet<Integer>` of Indices) (Optimal in Java) ---
 * 0. Idea/Intuition: Replace the two heaps with two `TreeSet<Integer>`s (`lower` and `upper`)
 *    that store **array indices** ordered by `(nums[idx] ASC, idx ASC)`.
 *    - `lower` holds the smaller `ceil(k / 2)` indices (`lower.last()` is the max of lower half).
 *    - `upper` holds the larger `floor(k / 2)` indices (`upper.first()` is the min of upper half).
 *    Because indices are unique, duplicates in `nums` never collide! And because `TreeSet` is a
 *    Red-Black Tree, `add(idx)`, `remove(outIdx)`, `pollLast()`, and `pollFirst()` are ALL
 *    genuine **O(log K)** — zero lazy deletion, zero stale memory, exact `.size()`!
 *    On each index `i`:
 *    (1) `lower.add(i); upper.add(lower.pollLast());` (guarantees `lower <= upper` with zero `if`s!)
 *    (2) If `i >= k`, eagerly delete `outIdx = i - k`: `if (!lower.remove(outIdx)) upper.remove(outIdx);`
 *    (3) Rebalance: while `lower.size() < upper.size()`, move `upper.pollFirst()` to `lower`;
 *        while `lower.size() > upper.size() + 1`, move `lower.pollLast()` to `upper`.
 *    (4) If `i >= k - 1`, read `nums[lower.last()]` (and `nums[upper.first()]` if `k` is even).
 * 1. Why someone thinks of it: "Heaps forced lazy deletion ONLY because heaps can't delete from
 *    the middle in O(log K). A Red-Black Tree (`TreeSet`) CAN — and storing indices solves the
 *    duplicate-key problem cleanly!"
 * 2. Why it works:
 *    - Order invariant: Routing `i` through `lower -> upper` guarantees every element in `lower`
 *      is `<=` every element in `upper` (in `(value, index)` order).
 *    - Size invariant: Eager deletion keeps `lower.size() + upper.size() == k` exact at all times,
 *      and the two `while` loops restore `lower.size() == ceil(k / 2)`.
 *    Time Complexity: Strict O(N log K) — every slide performs a constant number (<= 6) of
 *    Red-Black Tree operations (`add`, `remove`, `pollFirst`, `pollLast`) on trees strictly
 *    bounded to size `<= K`.
 *    Space Complexity: Strict O(K) auxiliary — `lower` and `upper` together hold AT MOST `K + 1`
 *    indices at any instant, even if `N = 10^9`.
 *
 * Interview Verdict:
 * Write Approach 4 (Two `TreeSet<Integer>`s of Indices) in a 45-minute Java interview!
 * It achieves strictly better asymptotic bounds than Lazy-Deletion Heaps (O(N log K) time and
 * O(K) space vs. O(N log N) time and O(N) space), is half the code (~20 lines), and completely
 * eliminates the notorious tombstone-stealing and pruning-order bugs of Lazy Deletion.
 * (In Python, where there is no built-in `TreeSet`, Approach 3 is the standard choice — knowing
 * both lets you explain *why* Java's `TreeSet` wins here.)
 *
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - Full 32-bit int extremes (`[2147483647, 2147483647]`, `k = 2` and `[-2147483648, 2147483647]`, `k = 2`):
 *   Tests BOTH comparator overflow (`Integer.compare`, never `a - b`) AND sum overflow
 *   (`((double) a + b) / 2.0`, never `(a + b) / 2.0`).
 * - `k = 1`: `upper` is always empty; median is simply `(double) nums[i]`.
 * - `k = 2`: Smallest even window; every slide removes 1 from size 2 and inserts 1.
 * - `k = nums.length`: Single window covering the entire array; deletion branch never fires.
 * - All identical elements (`[5, 5, 5, 5]`, `k = 2` and `k = 3`): Stress-tests duplicate handling
 *   in `TreeSet` (index tie-breaker) and boundary tombstone routing in Two Heaps.
 * - Strictly increasing (`[1, 2, 3, 4, 5]`) & strictly decreasing (`[5, 4, 3, 2, 1]`): Stress-tests
 *   repeated one-sided evictions and cross-partition rebalancing.
 * - Duplicate straddling the boundary (`[5, 5, 10]`, `k = 2`): Outgoing `5` equals both
 *   `lower`'s max and `upper`'s min — tests that we don't corrupt counts or steal tombstones.
 *
 * =========================================================================================
 * 4. ## Also include
 *
 * -- Key insight --
 * Question to ask yourself: "How do I get O(log K) arbitrary deletion AND O(1) midpoint access
 * without losing duplicate values?"
 * Correctness intuition: A balanced BST (`TreeSet`) supports O(log K) deletion of any element,
 * `last()`/`first()` exposes the exact midpoint border between `lower` and `upper`, and storing
 * unique array INDICES ordered by `(nums[idx], idx)` turns a set into a multiset cleanly.
 *
 * -- Examples & ASCII Diagram --
 * nums = [1, 3, -1, -3, 5, 3, 6, 7], k = 3
 * Showing `idx:val` inside `lower` (size 2) and `upper` (size 1):
 *
 *   Window [1, 3, -1] (i = 2):
 *       lower (smaller ceil(3/2)=2)     upper (larger floor(3/2)=1)
 *       { 2:-1,  0:1 }   <--- border --->   { 1:3 }
 *                 ^
 *         lower.last() = 0:1  -> Median = 1.0
 *
 *   Slide to i = 3 (in = 3:-3, out = 0:1):
 *     1. Add 3:-3 to lower, bounce lower.last() (0:1) to upper:
 *          lower = { 3:-3, 2:-1 },  upper = { 0:1, 1:3 }
 *     2. Eagerly remove outIdx = 0 (found in upper!):
 *          lower = { 3:-3, 2:-1 },  upper = { 1:3 }
 *     3. Sizes are 2 and 1 -> already balanced!
 *          lower.last() = 2:-1 -> Median = -1.0!
 *
 * -- Dry run 1: Optimal Two TreeSets on Example 1: nums = [1, 3, -1, -3, 5, 3, 6, 7], k = 3 --
 *   i | val | After add + bounce       | Remove (i-3) | After Rebalance (lower | upper) | Median
 *   --+-----+--------------------------+--------------+---------------------------------+-------
 *   0 |  1  | L:{}         U:{0:1}     | -            | {0:1}            | {}           | -
 *   1 |  3  | L:{0:1}      U:{1:3}     | -            | {0:1}            | {1:3}        | -
 *   2 | -1  | L:{2:-1}     U:{0:1,1:3} | -            | {2:-1, 0:1}      | {1:3}        |  1.0
 *   3 | -3  | L:{3:-3,2:-1}U:{0:1,1:3} | 0:1 (from U) | {3:-3, 2:-1}     | {1:3}        | -1.0
 *   4 |  5  | L:{3:-3,2:-1}U:{1:3,4:5} | 1:3 (from U) | {3:-3, 2:-1}     | {4:5}        | -1.0
 *   5 |  3  | L:{3:-3,2:-1}U:{5:3,4:5} | 2:-1(from L) | {3:-3, 5:3}      | {4:5}        |  3.0
 *   6 |  6  | L:{3:-3,5:3} U:{4:5,6:6} | 3:-3(from L) | {5:3, 4:5}       | {6:6}        |  5.0
 *   7 |  7  | L:{5:3,4:5}  U:{6:6,7:7} | 4:5 (from L) | {5:3, 6:6}       | {7:7}        |  6.0
 *
 * -- Dry run 2: Edge Case — Duplicate Straddling Boundary nums = [5, 5, 10], k = 2 --
 *   i | val | After add + bounce   | Remove (i-2) | After Rebalance (lower | upper) | Median
 *   --+-----+----------------------+--------------+---------------------------------+-------
 *   0 |  5  | L:{}      U:{0:5}    | -            | {0:5}     | {}                  | -
 *   1 |  5  | L:{0:5}   U:{1:5}    | -            | {0:5}     | {1:5}               | 5.0
 *   2 | 10  | L:{0:5}   U:{1:5,2:10}| 0:5 (from L)| {1:5}     | {2:10}              | 7.5
 *   Notice: Indices `0` and `1` both hold value `5`, yet `TreeSet` keeps them completely distinct
 *   and removes exact index `0` in O(log K) without touching index `1`!
 *
 * -- Pitfalls --
 * 1. Java-specific gotcha — Comparator subtraction overflow:
 *    Writing `(a, b) -> nums[a] - nums[b]` SILENTLY OVERFLOWS on `nums = [-2147483648, 2147483647]`
 *    (it wraps around to `+1` instead of negative!), corrupting the Red-Black Tree invariants!
 *    ALWAYS use `Integer.compare(nums[a], nums[b])`.
 * 2. Java-specific gotcha — `int` addition overflow in even-length median:
 *    Writing `(nums[a] + nums[b]) / 2.0` overflows `int` BEFORE dividing by `2.0` when both are
 *    `Integer.MAX_VALUE` (returns `-1.0` instead of `2147483647.0`!).
 *    ALWAYS cast first: `((double) nums[a] + nums[b]) / 2.0`.
 * 3. `TreeSet` comparator returning `0` on duplicate values:
 *    If your `TreeSet` comparator only compares `nums[a]` and `nums[b]` without tie-breaking on
 *    `Integer.compare(a, b)`, `TreeSet` treats equal values as identical and silently refuses to
 *    insert the second index!
 * 4. Shared tombstone map bug in Lazy Deletion:
 *    Using a single `delayed` map for both heaps can cause `upper` to consume a tombstone meant
 *    for `lower` when a duplicate value equals both `lower.peek()` and `upper.peek()`, desyncing
 *    `lowerSize` and `upperSize`. Always use separate `delayedLower` and `delayedUpper` maps.
 * 5. Calling `PriorityQueue.remove(Object)` in Java:
 *    Looks O(log K), actually does an O(K) linear scan — silently degrades to O(N * K) TLE.
 *
 * -- Pattern recognition --
 * When you see: "Sliding window + dynamic order statistic (median / k-th element / percentile)
 * requiring BOTH insert and arbitrary delete"
 * -> Think **Two Balanced BSTs (`TreeSet` of indices in Java)** or **Two Heaps + Lazy Deletion**.
 * Transfers directly to:
 *   - Finding MK Average (LeetCode 1825) — Three balanced BSTs splitting bottom-k, middle, top-k
 *   - Find Median from Data Stream (LeetCode 295) — append-only version (no deletions needed)
 *   - Design an Array Statistics Tracker (LeetCode 3369) — Two Heaps + Lazy Deletion + FIFO queue
 *
 * -- Interview script --
 * "A brute-force copy-and-sort takes O(N * K log K), and maintaining a sorted `ArrayList` still
 * pays O(K) per slide for shifting. In Find Median from Data Stream, two opposing heaps expose
 * the median in O(1), but here we also need to delete the outgoing element `nums[i - k]` on every
 * slide — and `PriorityQueue.remove()` is O(K) in Java. Instead of heaps with lazy deletion
 * (which can grow to O(N) space with stale elements), I'll use two `TreeSet<Integer>`s storing
 * array indices ordered by `(nums[idx], idx)` — `lower` holding the smaller `ceil(k / 2)` indices
 * and `upper` holding the larger `floor(k / 2)` indices. Indexing by `idx` handles duplicates
 * cleanly while giving true O(log K) deletion. Each slide routes `i` through `lower` into `upper`
 * to maintain ordering, eagerly removes `i - k`, rebalances sizes, and reads the border(s) using
 * `double` addition to avoid 32-bit overflow. That gives us strict O(N log K) time and O(K) space."
 *
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q1: What if the window size `k` is not fixed — we have a dynamic stream with `add(val)` and
 *     `removeOldest()` (arbitrary variable-size FIFO window)?
 * A1: Keep a monotonically increasing `long id` (or `int id`) per element + a `Deque<Item>` for
 *     FIFO eviction, and store `Item(val, id)` objects directly in the two `TreeSet`s instead of
 *     indexing into `nums[]`. Still strict O(log W) per op and O(W) space where `W` is the live window size.
 *
 * Q2: What if instead of the median (50th percentile), we want the `p`-th percentile (e.g., p95
 *     latency over a sliding window of `k` requests)?
 * A2: Exact same two `TreeSet`s! Just change the target size of `lower` from `ceil(k / 2)` to
 *     `targetLower = (int) Math.ceil(k * p)` (e.g., `ceil(0.95 * k)`), and rebalance `lower` to
 *     `targetLower` on every slide. `nums[lower.last()]` is always the exact `p`-th percentile
 *     in O(N log K) time and O(K) space!
 *
 * Q3: What if `nums[i]` values are restricted to a small known range (e.g., `[0, 1000]`) and
 *     `k` is huge (e.g., `k = 10^6`)?
 * A3: Replace the two `TreeSet`s with a **Fenwick Tree (Binary Indexed Tree)** over `[0, 1000]`
 *     (or even a flat `int[1001]` count array!). Updating counts is O(log V) (or O(1)), and
 *     binary lifting finds the `(k+1)/2` and `(k+2)/2` ranks in O(log V) with O(V) space —
 *     completely independent of `k`!
 * =========================================================================================
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeSet;

public class SlidingWindowMedianStudyNote {

    // ----------------------------------------------------------------------
    // APPROACH 1: Brute Force (O(N * K log K) time, O(K) auxiliary space)
    // ----------------------------------------------------------------------
    public static double[] medianSlidingWindowBruteForce(int[] nums, int k) {
        int n = nums.length;
        double[] result = new double[n - k + 1];
        int[] window = new int[k];

        for (int i = 0; i <= n - k; i++) {
            // Copy current k-element window and sort from scratch.
            System.arraycopy(nums, i, window, 0, k);
            Arrays.sort(window);

            if (k % 2 == 1) {
                result[i] = window[k / 2];
            } else {
                // Cast to double BEFORE adding so Integer.MAX_VALUE + Integer.MAX_VALUE never overflows.
                result[i] = ((double) window[k / 2 - 1] + window[k / 2]) / 2.0;
            }
        }
        return result;
    }

    // ----------------------------------------------------------------------
    // APPROACH 2: Maintained Sorted ArrayList (O(N * K) time, O(K) auxiliary space)
    // ----------------------------------------------------------------------
    public static double[] medianSlidingWindowSortedList(int[] nums, int k) {
        int n = nums.length;
        double[] result = new double[n - k + 1];

        // Holds the active k window elements in ascending sorted order.
        // Example for window [1, 3, -1]: sorted = [-1, 1, 3].
        List<Integer> sorted = new ArrayList<>(k);

        for (int i = 0; i < n; i++) {
            // 1. Insert incoming nums[i] at its binary-searched position (O(K) shift).
            int insIdx = Collections.binarySearch(sorted, nums[i]);
            if (insIdx < 0) {
                insIdx = -(insIdx + 1);
            }
            sorted.add(insIdx, nums[i]);

            // 2. Remove outgoing nums[i - k] once window exceeds size k (O(K) shift).
            if (i >= k) {
                int remIdx = Collections.binarySearch(sorted, nums[i - k]);
                sorted.remove(remIdx);
            }

            // 3. Read median directly in O(1) once the first full window has formed.
            if (i >= k - 1) {
                result[i - k + 1] = (k % 2 == 1)
                        ? sorted.get(k / 2)
                        : ((double) sorted.get(k / 2 - 1) + sorted.get(k / 2)) / 2.0;
            }
        }
        return result;
    }

    // ----------------------------------------------------------------------
    // APPROACH 3: Two Opposing Heaps + Lazy Deletion (O(N log N) time, O(N) space)
    // ----------------------------------------------------------------------
    private static void prune(PriorityQueue<Integer> heap, Map<Integer, Integer> delayed) {
        // Discard any tombstoned (logically deleted) values currently exposed at the root.
        while (!heap.isEmpty() && delayed.getOrDefault(heap.peek(), 0) > 0) {
            int stale = heap.poll();
            int remaining = delayed.get(stale) - 1;
            if (remaining == 0) {
                delayed.remove(stale);
            } else {
                delayed.put(stale, remaining);
            }
        }
    }

    public static double[] medianSlidingWindowTwoHeaps(int[] nums, int k) {
        int n = nums.length;
        double[] result = new double[n - k + 1];

        // lower (Max-Heap) holds smaller ceil(k / 2) valid values; upper (Min-Heap) holds rest.
        // Collections.reverseOrder() prevents comparator overflow on extreme 32-bit ints.
        PriorityQueue<Integer> lower = new PriorityQueue<>(Collections.reverseOrder());
        PriorityQueue<Integer> upper = new PriorityQueue<>();

        // Separate tombstone maps per heap so duplicate boundary values never steal each other's deletions.
        // Example: if 5 is in both lower and upper and leaves lower, delayedLower={5=1}, delayedUpper={}.
        Map<Integer, Integer> delayedLower = new HashMap<>();
        Map<Integer, Integer> delayedUpper = new HashMap<>();

        // Logical counts of VALID (non-deleted) elements currently in each heap.
        int lowerSize = 0;
        int upperSize = 0;

        for (int i = 0; i < n; i++) {
            int inVal = nums[i];

            // 1. Insert incoming element FIRST (keeps `lower` non-empty during step 2).
            prune(lower, delayedLower);
            if (lower.isEmpty() || inVal <= lower.peek()) {
                lower.offer(inVal);
                lowerSize++;
            } else {
                upper.offer(inVal);
                upperSize++;
            }

            // 2. Lazily delete outgoing element once window exceeds size k.
            if (i >= k) {
                int outVal = nums[i - k];
                prune(lower, delayedLower);
                if (!lower.isEmpty() && outVal <= lower.peek()) {
                    delayedLower.merge(outVal, 1, Integer::sum);
                    lowerSize--;
                } else {
                    delayedUpper.merge(outVal, 1, Integer::sum);
                    upperSize--;
                }
            }

            // 3. Rebalance logical sizes: lowerSize must be upperSize (even) or upperSize + 1 (odd).
            //    Always prune the source heap before polling so we only migrate VALID elements.
            while (lowerSize > upperSize + 1) {
                prune(lower, delayedLower);
                upper.offer(lower.poll());
                lowerSize--;
                upperSize++;
            }
            while (lowerSize < upperSize) {
                prune(upper, delayedUpper);
                lower.offer(upper.poll());
                upperSize--;
                lowerSize++;
            }

            // 4. Record median once full window [i - k + 1 .. i] is populated.
            if (i >= k - 1) {
                prune(lower, delayedLower);
                prune(upper, delayedUpper);
                result[i - k + 1] = (k % 2 == 1)
                        ? lower.peek()
                        : ((double) lower.peek() + upper.peek()) / 2.0;
            }
        }
        return result;
    }

    // ----------------------------------------------------------------------
    // APPROACH 4: Two Balanced BSTs (`TreeSet<Integer>` of Indices)
    // (OPTIMAL IN JAVA: Strict O(N log K) time, Strict O(K) auxiliary space)
    // ----------------------------------------------------------------------
    public static double[] medianSlidingWindowTreeSets(int[] nums, int k) {
        int n = nums.length;
        double[] result = new double[n - k + 1];

        // Order array indices by their value ascending; tie-break by index ascending so
        // duplicate values in `nums` stay distinct inside the TreeSet.
        // Integer.compare() avoids signed 32-bit overflow on MIN_VALUE / MAX_VALUE.
        Comparator<Integer> byValThenIdx = (a, b) -> {
            int cmp = Integer.compare(nums[a], nums[b]);
            return cmp != 0 ? cmp : Integer.compare(a, b);
        };

        // lower holds the smaller ceil(k / 2) indices; lower.last() is the max of lower half.
        // upper holds the larger floor(k / 2) indices; upper.first() is the min of upper half.
        // Example for [1, 3, -1] (k=3): lower = {2(-1), 0(1)}, upper = {1(3)} -> median = nums[0] = 1.0.
        TreeSet<Integer> lower = new TreeSet<>(byValThenIdx);
        TreeSet<Integer> upper = new TreeSet<>(byValThenIdx);

        for (int i = 0; i < n; i++) {
            // 1. Route index `i` through `lower` -> `upper` to guarantee max(lower) <= min(upper)
            //    without any empty-set checks.
            lower.add(i);
            upper.add(lower.pollLast());

            // 2. Eagerly delete the outgoing index in O(log K) once window exceeds size k.
            //    Since indices are unique, `outIdx` lives in exactly one of the two sets.
            if (i >= k) {
                int outIdx = i - k;
                if (!lower.remove(outIdx)) {
                    upper.remove(outIdx);
                }
            }

            // 3. Restore exact size invariant: lower.size() == upper.size() or upper.size() + 1.
            while (lower.size() < upper.size()) {
                lower.add(upper.pollFirst());
            }
            while (lower.size() > upper.size() + 1) {
                upper.add(lower.pollLast());
            }

            // 4. Read exact median in O(log K) (first()/last()) once first full window forms.
            if (i >= k - 1) {
                result[i - k + 1] = (k % 2 == 1)
                        ? nums[lower.last()]
                        : ((double) nums[lower.last()] + nums[upper.first()]) / 2.0;
            }
        }
        return result;
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS — Cross-checking all 4 approaches on representative + edge cases
    // ----------------------------------------------------------------------
    private static boolean approxEqual(double[] a, double[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (Math.abs(a[i] - b[i]) > 1e-5) return false;
        }
        return true;
    }

    private static void verifyCase(String name, int[] nums, int k, double[] expected) {
        double[] rBrute = medianSlidingWindowBruteForce(nums, k);
        double[] rSorted = medianSlidingWindowSortedList(nums, k);
        double[] rHeaps = medianSlidingWindowTwoHeaps(nums, k);
        double[] rTrees = medianSlidingWindowTreeSets(nums, k);

        boolean pass = approxEqual(rBrute, expected)
                && approxEqual(rSorted, expected)
                && approxEqual(rHeaps, expected)
                && approxEqual(rTrees, expected);

        System.out.printf("%-34s | %s | Output: %s%n",
                name, pass ? "PASS" : "FAIL", Arrays.toString(rTrees));

        if (!pass) {
            System.out.println("  Expected:  " + Arrays.toString(expected));
            System.out.println("  Brute:     " + Arrays.toString(rBrute));
            System.out.println("  Sorted:    " + Arrays.toString(rSorted));
            System.out.println("  TwoHeaps:  " + Arrays.toString(rHeaps));
            System.out.println("  TreeSets:  " + Arrays.toString(rTrees));
        }
    }

    public static void main(String[] args) {
        verifyCase("LC Example 1 (odd k=3)",
                new int[]{1, 3, -1, -3, 5, 3, 6, 7}, 3,
                new double[]{1.0, -1.0, -1.0, 3.0, 5.0, 6.0});

        verifyCase("LC Example 2 (duplicates)",
                new int[]{1, 2, 3, 4, 2, 3, 1, 4, 2}, 3,
                new double[]{2.0, 3.0, 3.0, 3.0, 2.0, 3.0, 2.0});

        verifyCase("Edge: k = 1 (upper always empty)",
                new int[]{5, -2, 10}, 1,
                new double[]{5.0, -2.0, 10.0});

        verifyCase("Edge: k = 2 (even, dup boundary)",
                new int[]{5, 5, 10, 5}, 2,
                new double[]{5.0, 7.5, 7.5});

        verifyCase("Edge: k = n (single window)",
                new int[]{4, 1, 3, 2}, 4,
                new double[]{2.5});

        verifyCase("Edge: All duplicates (k=3)",
                new int[]{7, 7, 7, 7, 7}, 3,
                new double[]{7.0, 7.0, 7.0});

        verifyCase("Edge: Strictly decreasing",
                new int[]{5, 4, 3, 2, 1}, 3,
                new double[]{4.0, 3.0, 2.0});

        verifyCase("Edge: 32-bit MAX/MIN overflow",
                new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE}, 2,
                new double[]{2147483647.0, -0.5, -0.5});
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 *
 * - Core pattern: Two Balanced BSTs (`TreeSet<Integer>` of indices) partitioning the window
 *   into `lower` (`ceil(k / 2)`) and `upper` (`floor(k / 2)`).
 * - Key observation: Heaps struggle with sliding-window deletion (`PriorityQueue.remove` is O(K)),
 *   forcing lazy-deletion tombstones and O(N) space; Java's `TreeSet` keyed by `(nums[idx], idx)`
 *   supports true O(log K) deletion AND duplicate values in strict O(K) space.
 * - Worth memorizing vs. understanding: Understand *why* storing indices turns `TreeSet` into
 *   a multiset; memorize using `Integer.compare()` in the comparator and `((double) a + b) / 2.0`
 *   for even medians (full 32-bit `int` constraints will break subtraction/addition!).
 * - Most common trap: `nums[a] - nums[b]` or `(a + b) / 2.0` overflowing on `Integer.MAX_VALUE`
 *   / `Integer.MIN_VALUE`, or using a single shared tombstone map in Lazy-Deletion Heaps.
 * - One-line mental trigger: "Sliding window median in Java? Two TreeSets of indices ordered by (val, idx): bounce, remove `i-k`, rebalance."
 * =========================================================================================
 */









































import java.util.*;

class Solution {

    /*
     * SMALL:
     *
     * Max heap containing the smaller half of the values.
     * Its root is the largest element in the smaller half.
     */
    private final PriorityQueue<Integer> small =
        new PriorityQueue<>(Comparator.reverseOrder());

    /*
     * LARGE:
     *
     * Min heap containing the larger half of the values.
     * Its root is the smallest element in the larger half.
     */
    private final PriorityQueue<Integer> large =
        new PriorityQueue<>();

    /*
     * DELAYED:
     *
     * Tracks values that must eventually be removed.
     *
     * Example:
     * {5 -> 2} means two occurrences of 5
     * should be discarded when they reach a heap root.
     *
     * Frequencies are necessary because duplicates are allowed.
     */
    private final Map<Integer, Integer> delayed =
        new HashMap<>();

    /*
     * Logical sizes:
     *
     * These count only valid elements.
     *
     * They are deliberately different from small.size()
     * and large.size(), because physical heaps may contain
     * elements waiting for lazy deletion.
     */
    private int smallSize = 0;
    private int largeSize = 0;

    public double[] medianSlidingWindow(int[] nums, int k) {

        int n = nums.length;

        double[] result = new double[n - k + 1];

        /*
         * Initialize the first window.
         */
        for (int i = 0; i < k; i++) {
            addNum(nums[i]);
        }

        result[0] = getMedian(k);

        /*
         * Slide the window.
         *
         * At each step:
         * 1. Add the incoming element.
         * 2. Remove the outgoing element.
         * 3. Calculate the new median.
         */
        for (int right = k; right < n; right++) {

            int incoming = nums[right];
            int outgoing = nums[right - k];

            addNum(incoming);

            removeNum(outgoing);

            result[right - k + 1] = getMedian(k);
        }

        return result;
    }

    /*
     * INSERT OPERATION
     *
     * Insert into the appropriate heap based on the
     * maximum element of the smaller half.
     */
    private void addNum(int num) {

        /*
         * Clean stale elements before comparing against
         * the root of the smaller heap.
         */
        prune(small);

        if (small.isEmpty() || num <= small.peek()) {

            small.offer(num);
            smallSize++;

        } else {

            large.offer(num);
            largeSize++;
        }

        rebalance();
    }

    /*
     * REMOVE OPERATION
     *
     * We cannot efficiently remove an arbitrary element
     * from PriorityQueue.
     *
     * Therefore, we:
     * 1. Mark the element in delayed.
     * 2. Decrease the logical size of its heap.
     * 3. Physically remove it later when it reaches the root.
     */
    private void removeNum(int num) {

        prune(small);

        /*
         * Determine which logical heap owns this occurrence.
         *
         * If num <= small.peek(), classify it as belonging
         * to small. Otherwise, it belongs to large.
         *
         * This classification works with duplicates because
         * equal values can safely belong to either side.
         */
        delayed.merge(num, 1, Integer::sum);

        if (!small.isEmpty() && num <= small.peek()) {

            smallSize--;

            /*
             * If the element is already at the root,
             * remove it immediately.
             */
            if (num == small.peek()) {
                prune(small);
            }

        } else {

            largeSize--;

            /*
             * Similarly, remove immediately if it is at
             * the root of the larger heap.
             */
            if (!large.isEmpty() && num == large.peek()) {
                prune(large);
            }
        }

        /*
         * Removing an element can break the size invariant.
         */
        rebalance();
    }

    /*
     * LAZY DELETION
     *
     * Physically remove invalid elements from the heap root.
     *
     * Why only the root?
     *
     * PriorityQueue exposes its highest-priority element
     * efficiently, but does not expose arbitrary elements
     * efficiently.
     *
     * An invalid element buried inside the heap does not
     * affect the median until it reaches the root.
     */
    private void prune(PriorityQueue<Integer> heap) {

        while (!heap.isEmpty()) {

            int num = heap.peek();

            Integer count = delayed.get(num);

            /*
             * No pending deletion for this root.
             * Therefore, it is valid.
             */
            if (count == null) {
                break;
            }

            /*
             * Remove the invalid root physically.
             */
            heap.poll();

            /*
             * Consume exactly one pending deletion.
             */
            if (count == 1) {

                delayed.remove(num);

            } else {

                delayed.put(num, count - 1);
            }
        }
    }

    /*
     * REBALANCING
     *
     * Required invariant:
     *
     * smallSize == largeSize
     *
     * OR
     *
     * smallSize == largeSize + 1
     *
     * Therefore, the smaller heap can contain at most
     * one additional valid element.
     */
    private void rebalance() {

        /*
         * Too many valid elements in small.
         *
         * Move its largest element into large.
         */
        if (smallSize > largeSize + 1) {

            prune(small);

            large.offer(small.poll());

            smallSize--;
            largeSize++;

            /*
             * The moved element might be a stale entry
             * only if pruning was insufficient, but pruning
             * above guarantees the root is valid.
             */
            prune(small);
        }

        /*
         * Too many valid elements in large.
         *
         * Move its smallest element into small.
         */
        else if (smallSize < largeSize) {

            prune(large);

            small.offer(large.poll());

            largeSize--;
            smallSize++;

            prune(large);
        }
    }

    /*
     * MEDIAN OPERATION
     *
     * Odd k:
     * The smaller heap has one extra valid element.
     * Its root is the median.
     *
     * Even k:
     * Both heaps have equal valid elements.
     * Their roots are the two middle values.
     */
    private double getMedian(int k) {

        prune(small);
        prune(large);

        if (k % 2 == 1) {

            return small.peek();

        }

        return ((double) small.peek() + large.peek()) / 2.0;
    }
}










































import java.util.*;

class Solution {

    /*
     * SMALL:
     *
     * Stores the smaller half of the window.
     *
     * lastKey() gives the largest value in this partition.
     */
    private final TreeMap<Integer, Integer> small =
        new TreeMap<>();

    /*
     * LARGE:
     *
     * Stores the larger half of the window.
     *
     * firstKey() gives the smallest value in this partition.
     */
    private final TreeMap<Integer, Integer> large =
        new TreeMap<>();

    /*
     * Logical sizes count total occurrences, not distinct keys.
     *
     * For example:
     * {2 -> 3, 5 -> 1} has size 4, not 2.
     */
    private int smallSize = 0;
    private int largeSize = 0;

    public double[] medianSlidingWindow(int[] nums, int k) {

        int n = nums.length;

        double[] result = new double[n - k + 1];

        // Build the initial window.
        for (int i = 0; i < k; i++) {
            addNum(nums[i]);
        }

        result[0] = getMedian(k);

        // Slide the window one position at a time.
        for (int right = k; right < n; right++) {

            int incoming = nums[right];
            int outgoing = nums[right - k];

            // Insert the new element.
            addNum(incoming);

            // Remove the element leaving the window.
            removeNum(outgoing);

            // Calculate the median of the updated window.
            result[right - k + 1] = getMedian(k);
        }

        return result;
    }

    /*
     * INSERT
     *
     * Insert into the appropriate partition.
     *
     * Every value in small must be <= every value in large.
     */
    private void addNum(int num) {

        if (small.isEmpty() || num <= small.lastKey()) {

            add(small, num);
            smallSize++;

        } else {

            add(large, num);
            largeSize++;
        }

        rebalance();
    }

    /*
     * REMOVE
     *
     * Unlike PriorityQueue, TreeMap supports direct key removal
     * in O(log k).
     *
     * Since duplicates are interchangeable, if num equals the
     * boundary, we can safely remove one occurrence from small.
     */
    private void removeNum(int num) {

        if (!small.isEmpty() && num <= small.lastKey()) {

            remove(small, num);
            smallSize--;

        } else {

            remove(large, num);
            largeSize--;
        }

        rebalance();
    }

    /*
     * ADD ONE OCCURRENCE
     *
     * merge handles duplicate values by incrementing frequency.
     */
    private void add(TreeMap<Integer, Integer> map, int num) {

        map.merge(num, 1, Integer::sum);
    }

    /*
     * REMOVE ONE OCCURRENCE
     *
     * If frequency is greater than one, decrement it.
     * Otherwise, remove the key completely.
     */
    private void remove(TreeMap<Integer, Integer> map, int num) {

        int count = map.get(num);

        if (count == 1) {

            map.remove(num);

        } else {

            map.put(num, count - 1);
        }
    }

    /*
     * REBALANCE
     *
     * Invariant:
     *
     * smallSize == largeSize
     *
     * OR
     *
     * smallSize == largeSize + 1
     *
     * Move the boundary element between partitions when needed.
     */
    private void rebalance() {

        // SMALL has more than one extra element.
        if (smallSize > largeSize + 1) {

            int value = small.lastKey();

            remove(small, value);
            add(large, value);

            smallSize--;
            largeSize++;
        }

        // LARGE has more elements than SMALL.
        else if (smallSize < largeSize) {

            int value = large.firstKey();

            remove(large, value);
            add(small, value);

            largeSize--;
            smallSize++;
        }
    }

    /*
     * MEDIAN
     *
     * Odd k:
     * The largest value in small is the middle element.
     *
     * Even k:
     * The largest value in small and smallest value in large
     * are the two middle elements.
     */
    private double getMedian(int k) {

        if (k % 2 == 1) {

            return small.lastKey();

        }

        return ((double) small.lastKey() + large.firstKey()) / 2.0;
    }
}

/*
 * TIME COMPLEXITY:
 *
 * addNum:
 * O(log k)
 *
 * removeNum:
 * O(log k)
 *
 * rebalance:
 * O(log k), because at most one element is transferred.
 *
 * getMedian:
 * O(log k) for TreeMap key access in Java.
 *
 * Number of windows:
 * n - k + 1.
 *
 * Overall:
 * O(n log k)
 *
 * SPACE COMPLEXITY:
 *
 * Both TreeMaps together contain exactly k valid elements
 * after every completed window update.
 *
 * Therefore:
 * O(k) auxiliary space.
 *
 * No lazy-deletion entries accumulate.
 */









































import java.util.*;

class Solution {

    /*
     * MULTISET
     *
     * TreeMap stores:
     *     number -> frequency
     *
     * Example:
     *     [1, 2, 2, 3, 3, 3]
     *
     *     map = {
     *         1 -> 1,
     *         2 -> 2,
     *         3 -> 3
     *     }
     *
     * Unlike a normal Set, this supports duplicates.
     *
     * We also maintain a separate size variable because
     * map.size() returns the number of DISTINCT keys,
     * whereas we need the total number of elements.
     */
    private static class MultiSet {

        private final TreeMap<Integer, Integer> frequency = new TreeMap<>();

        private int size = 0;

        /*
         * Insert one occurrence of num.
         *
         * Time: O(log k)
         */
        void add(int num) {

            frequency.merge(num, 1, Integer::sum);

            size++;
        }

        /*
         * Remove exactly one occurrence of num.
         *
         * If multiple occurrences exist, decrease the frequency.
         * Otherwise, remove the key completely.
         *
         * Precondition: num must exist in the multiset.
         *
         * Time: O(log k)
         */
        void remove(int num) {

            int count = frequency.get(num);

            if (count == 1) {

                frequency.remove(num);

            } else {

                frequency.put(num, count - 1);
            }

            size--;
        }

        void remove2(int num) {
            frequency.computeIfPresent(num, (key, count) ->
                count == 1 ? null : count - 1
            );
            size--;
        }

        /*
         * Return the smallest element.
         *
         * Time: O(log k) worst case.
         */
        int min() {
            return frequency.firstKey();
        }

        /*
         * Return the largest element.
         *
         * Time: O(log k) worst case.
         */
        int max() {
            return frequency.lastKey();
        }

        /*
         * Total number of elements, including duplicates.
         *
         * Time: O(1)
         */
        int size() {
            return size;
        }

        /*
         * Check whether the multiset contains any elements.
         *
         * Time: O(1)
         */
        boolean isEmpty() {
            return size == 0;
        }

        /*
         * Check whether at least one occurrence exists.
         *
         * Time: O(log k)
         */
        boolean contains(int num) {
            return frequency.containsKey(num);
        }
    }

    /*
     * SLIDING WINDOW MEDIAN
     *
     * We maintain two ordered partitions:
     *
     * LEFT:
     *     Contains the smaller half of the elements.
     *     max() gives the largest element in this partition.
     *
     * RIGHT:
     *     Contains the larger half of the elements.
     *     min() gives the smallest element in this partition.
     *
     * IMPORTANT INVARIANTS:
     *
     * 1. Every element in LEFT <= every element in RIGHT.
     *
     * 2. LEFT contains either:
     *      - The same number of elements as RIGHT, OR
     *      - Exactly one more element than RIGHT.
     *
     * Therefore:
     *
     * Odd k:
     *     Median = LEFT.max()
     *
     * Even k:
     *     Median = average(LEFT.max(), RIGHT.min())
     */
    public double[] medianSlidingWindow(int[] nums, int k) {

        int n = nums.length;

        // Number of windows = n - k + 1.
        double[] result = new double[n - k + 1];

        MultiSet left = new MultiSet();
        MultiSet right = new MultiSet();

        /*
         * STEP 1: Construct the first window.
         *
         * Each insertion is followed by rebalancing,
         * so both partition invariants remain valid.
         */
        for (int i = 0; i < k; i++) {

            add(left, right, nums[i]);
        }

        // The first complete window is ready.
        result[0] = getMedian(left, right, k);

        /*
         * STEP 2: Slide the window.
         *
         * For every new position:
         *
         * 1. Remove the element leaving the window.
         * 2. Insert the incoming element.
         * 3. Calculate the new median.
         *
         * At every iteration, both partitions represent
         * exactly the current window after the update.
         */
        for (int rightIndex = k; rightIndex < n; rightIndex++) {

            int outgoing = nums[rightIndex - k];

            int incoming = nums[rightIndex];

            // Remove the element that is no longer in the window.
            remove(left, right, outgoing);

            // Insert the new element entering the window.
            add(left, right, incoming);

            // Store the median of the updated window.
            result[rightIndex - k + 1] =
                getMedian(left, right, k);
        }

        return result;
    }

    /*
     * INSERT OPERATION
     *
     * Decide which partition should receive num.
     *
     * If LEFT is empty or num <= LEFT.max():
     *     Insert into LEFT.
     *
     * Otherwise:
     *     Insert into RIGHT.
     *
     * Why?
     *
     * LEFT.max() represents the boundary between
     * the smaller and larger partitions.
     *
     * After insertion, rebalance to restore the size invariant.
     *
     * Time: O(log k)
     */
    private void add(MultiSet left, MultiSet right, int num) {

        if (left.isEmpty() || num <= left.max()) {

            left.add(num);

        } else {

            right.add(num);
        }

        rebalance(left, right);
    }

    /*
     * REMOVE OPERATION
     *
     * Find which partition contains num and remove
     * exactly one occurrence.
     *
     * Why is checking LEFT first sufficient?
     *
     * LEFT and RIGHT together contain every element
     * of the current window.
     *
     * If LEFT contains num, we remove one occurrence
     * from LEFT.
     *
     * Otherwise, num must belong to RIGHT.
     *
     * Duplicates are handled correctly because removing
     * either identical occurrence produces the same
     * multiset of values.
     *
     * After removal, rebalance the partitions.
     *
     * Time: O(log k)
     */
    private void remove(MultiSet left, MultiSet right, int num) {

        if (left.contains(num)) {

            left.remove(num);

        } else {

            right.remove(num);
        }

        rebalance(left, right);
    }

    /*
     * REBALANCE OPERATION
     *
     * Required size invariant:
     *
     *     left.size() == right.size()
     *
     * OR
     *
     *     left.size() == right.size() + 1
     *
     * Case 1:
     * LEFT has more than one extra element.
     *
     * Move LEFT.max() to RIGHT.
     *
     * Case 2:
     * RIGHT has more elements than LEFT.
     *
     * Move RIGHT.min() to LEFT.
     *
     * Why do these moves preserve ordering?
     *
     * LEFT.max() is the largest element in LEFT.
     * Moving it to RIGHT cannot violate the partition order.
     *
     * RIGHT.min() is the smallest element in RIGHT.
     * Moving it to LEFT cannot violate the partition order.
     *
     * We use while instead of if for defensive balancing.
     *
     * In this implementation, because every insertion/removal
     * is followed immediately by rebalancing, at most one
     * transfer is needed per call.
     *
     * Time: O(log k)
     */
    private void rebalance(MultiSet left, MultiSet right) {

        /*
         * LEFT has too many elements.
         *
         * Transfer its largest element to RIGHT.
         */
        while (left.size() > right.size() + 1) {

            int boundary = left.max();

            left.remove(boundary);

            right.add(boundary);
        }

        /*
         * RIGHT has more elements than LEFT.
         *
         * Transfer its smallest element to LEFT.
         */
        while (right.size() > left.size()) {

            int boundary = right.min();

            right.remove(boundary);

            left.add(boundary);
        }
    }

    /*
     * MEDIAN OPERATION
     *
     * Because the partitions are balanced:
     *
     * Odd k:
     *     LEFT has one additional element.
     *     LEFT.max() is the middle element.
     *
     * Even k:
     *     Both partitions have equal sizes.
     *     LEFT.max() and RIGHT.min() are the
     *     two middle elements.
     *
     * We cast to long before addition to prevent
     * integer overflow.
     *
     * Time: O(log k) worst case for TreeMap key access.
     */
    private double getMedian(MultiSet left, MultiSet right, int k) {

        if (k % 2 == 1) {

            return left.max();
        }

        return ((long) left.max() + right.min()) / 2.0;
    }
}

/*
 * ==========================================
 * COMPLEXITY ANALYSIS
 * ==========================================
 *
 * Let:
 *     n = nums.length
 *     k = window size
 *
 * MULTISET OPERATIONS:
 *
 * add():
 *     TreeMap insertion = O(log k)
 *
 * remove():
 *     TreeMap deletion = O(log k)
 *
 * min()/max():
 *     TreeMap firstKey()/lastKey()
 *     O(log k) worst case.
 *
 * rebalance():
 *     At most one element transfer after each
 *     individual insertion or removal.
 *     Each transfer costs O(log k).
 *
 * getMedian():
 *     O(log k) worst case.
 *
 *
 * TOTAL TIME:
 *
 * Initial window construction:
 *     O(k log k)
 *
 * Remaining windows:
 *     O((n - k) log k)
 *
 * Total:
 *     O(n log k)
 *
 *
 * AUXILIARY SPACE:
 *
 * LEFT + RIGHT together contain exactly k elements
 * after every completed window update.
 *
 * TreeMap stores at most k distinct keys overall.
 *
 * Therefore:
 *     O(k)
 *
 * Output array:
 *     O(n - k + 1)
 *
 * TOTAL SPACE INCLUDING OUTPUT:
 *     O(n)
 */

