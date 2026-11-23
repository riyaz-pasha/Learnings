/**
 * 0. ## Problem Statement
 * Design a data structure that tracks statistics of an array supporting FIFO additions and removals.
 *
 * Implement the `StatisticsTracker` class:
 * - `StatisticsTracker()`: Initializes the object with an empty array.
 * - `void addNumber(int number)`: Appends `number` to the data structure.
 * - `void removeFirstAddedNumber()`: Removes the earliest added number (FIFO order).
 * - `int getMean()`: Returns the floored integer mean of all current numbers.
 * - `int getMedian()`: Returns the median of all current numbers. If the count is even,
 *   return the LARGER of the two middle values.
 * - `int getMode()`: Returns the mode (most frequent number). If multiple numbers tie for
 *   the highest frequency, return the SMALLEST one.
 *
 * Example:
 *   add(4), add(4), add(2), add(3) -> [4, 4, 2, 3]
 *   getMean()   -> floor((4+4+2+3)/4) = 3
 *   getMedian() -> sorted [2, 3, 4, 4] -> larger middle = 4
 *   getMode()   -> 4 (freq 2)
 *   removeFirstAddedNumber() -> removes 4 -> [4, 2, 3]
 *   getMode()   -> all freq 1 -> smallest tied value = 2
 *
 * Constraints:
 * - 1 <= number <= 10^9
 * - At most 10^5 total calls to all methods combined.
 * - `removeFirstAddedNumber`, `getMean`, `getMedian`, and `getMode` are only called when
 *   at least one element is present.
 *
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "For an even number of elements, does `getMedian()` return the average, the smaller middle,
 *    or the larger middle?"
 *    -> Problem specifies the LARGER middle (`sorted[n / 2]`). This dictates our exact partition
 *       sizing (`lowerSize = n / 2`, `upperSize = n - n / 2`) so the median is always `min(upper)`.
 * 2. "How should ties in `getMode()` be broken — smallest value, largest value, or earliest added?"
 *    -> Smallest value. Determines the secondary comparator (`val ASC`) or `TreeSet.first()` lookup.
 * 3. "Can the running sum of elements overflow a 32-bit signed `int`?"
 *    -> Yes! `10^5` elements up to `10^9` sum to `10^14 >> Integer.MAX_VALUE` (~2.1 * 10^9).
 *       Forces `long runningSum` for `getMean()`.
 * 4. "Are values strictly positive (meaning integer division truncates == floor), or can they be negative?"
 *    -> Constraints guarantee `number >= 1`, so `runningSum / size` equals `Math.floor`. If negatives
 *       were allowed, Java's `/` truncates toward zero (e.g., `-3 / 2 == -1`, whereas `floor(-1.5) == -2`),
 *       requiring `Math.floorDiv(runningSum, size)`.
 * 5. "Can the stream experience long-running churn (millions of add/remove pairs while keeping only
 *    a small live window `N`), or is `O(total adds)` memory acceptable?"
 *    -> Directly distinguishes Lazy Deletion Heaps (O(M) memory where M = total adds) from
 *       Eager Balanced BSTs (`TreeMap`, strict O(N) live memory).
 * 6. "Can `removeFirstAddedNumber()` or any getter ever be called on an empty tracker?"
 *    -> Constraints guarantee non-empty, avoiding sentinel/exception return contracts.
 *
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: Up to `10^5` total calls, where values span up to `10^9` (too wide for a
 * flat counting array). Any query or removal taking O(N) or O(N log N) hits `~10^10` operations
 * (TLE). We need O(1) or O(log N) per operation across all 5 methods.
 *
 * Decompose the problem into 4 independent sub-requirements sharing one FIFO `Queue`:
 *   (A) FIFO eviction -> `ArrayDeque<Integer>` (O(1)).
 *   (B) Mean          -> `long sum` + `queue.size()` (O(1)).
 *   (C) Median        -> Dynamic midpoint with both insert AND arbitrary delete.
 *   (D) Mode          -> Dynamic max-frequency element (tie-break smallest value) with freq +1/-1.
 *
 * --- Approach 1: Brute Force (FIFO Queue + Recompute on Every Query) ---
 * 0. Idea/Intuition: Store active elements in an `ArrayDeque`. `addNumber` and
 *    `removeFirstAddedNumber` are O(1). On `getMean`, sum the queue; on `getMedian`, copy to a
 *    list, sort, and return `list.get(n / 2)`; on `getMode`, build a frequency `HashMap` and scan.
 * 1. What I'd naturally try: Keep the state dead-simple and compute each statistic on demand.
 * 2. Why it works: Every query inspects the exact live snapshot, so zero sync bugs are possible.
 * 3. Why it's too slow/costly: Interleaving 50,000 `add`s and 50,000 `getMedian`s sorts ~50,000
 *    elements 50,000 times -> `~3.8 * 10^10` operations (massive TLE).
 * 4. What work is being repeated: Every `add`/`remove` changes ONLY ONE element, yet we throw
 *    away the entire sum, sorted order, and frequency table and rebuild them from scratch!
 * 5. Observation that removes the bottleneck: Maintain incremental state for each statistic:
 *    - Mean: a running `long sum`.
 *    - Median: Two opposing heaps (`lower` Max-Heap, `upper` Min-Heap) just like LC 295.
 *    - Mode: a `freqMap` + a Max-Heap of `(freq, val)`.
 *    The only snag is `removeFirstAddedNumber()`: Java's `PriorityQueue.remove(obj)` is O(N)!
 *    We bypass O(N) mid-heap removal with **Lazy Deletion** (tombstone maps).
 * 6. Leads to Approach 2: Multi-Heap with Lazy Deletion.
 *    Time Complexity: `add`/`remove`: O(1); `getMean`: O(N); `getMode`: O(N); `getMedian`: O(N log N)
 *    — because each getter scans/sorts all N live elements from scratch.
 *    Space Complexity: O(N) — stores the N live elements in the queue (plus temporary O(N) copy/map on reads).
 *
 * --- Approach 2: Multi-Heap + Lazy Deletion (Amortized O(log M)) ---
 * 0. Idea/Intuition:
 *    - Mean: `long sum` updated in O(1) on add/remove.
 *    - Median: `lowerHeap` (Max-Heap) + `upperHeap` (Min-Heap) + two tombstone maps
 *      (`lazyLower`, `lazyUpper`) + logical sizes (`validLower`, `validUpper`). On remove,
 *      increment the value's tombstone count instead of calling `PriorityQueue.remove()`.
 *      Before every `peek()` or `poll()`, prune any tombstones sitting at the root.
 *    - Mode: `HashMap<Integer, Integer> freq` + Max-Heap of `(freq, val)` ordered by
 *      `(freq DESC, val ASC)`. Whenever `val`'s frequency changes (up OR down), simply push
 *      the new `(newFreq, val)` snapshot without deleting the old one. On `getMode()`, discard
 *      any heap root whose recorded `freq != freqMap.get(val)`.
 * 1. What I'd naturally try: Reach for the familiar Two-Heaps median trick + Max-Heap mode trick,
 *    and patch Java's O(N) heap deletion with lazy deletion.
 * 2. Why it works: Tombstones buried below a heap root cannot affect `peek()`; the moment a
 *    stale entry surfaces, it is discarded in O(log M) before affecting any answer or rebalance.
 * 3. Why it's still suboptimal (space & bug risk):
 *    - Stale entries can stay buried indefinitely! If a service adds and removes 1 element
 *      50,000 times (live size `N = 1`), the heaps can accumulate O(M) tombstones (where `M` =
 *      total operations, not live size `N`).
 *    - Tracking separate tombstone maps + logical sizes + remembering to `cleanHeap()` before
 *      EVERY `peek()`/`poll()` is notoriously bug-prone under interview pressure.
 * 4. What work/storage is wasted: Keeping dead tombstones in heaps solely because heaps cannot
 *    delete arbitrary elements in O(log N).
 * 5. Observation that removes the bottleneck: Java's `TreeMap` (Red-Black Tree) DOES support
 *    genuine **O(log N) eager deletion** of arbitrary keys!
 *    - Median: Replace the two heaps with two frequency `TreeMap<Integer, Integer>`s (`lower`, `upper`).
 *    - Mode: Replace `modeHeap` with `TreeMap<Integer, TreeSet<Integer>> freqToVals` mapping
 *      `frequency -> sorted set of values with that exact frequency`.
 *      Then `getMode()` is literally `freqToVals.lastEntry().getValue().first()`!
 * 6. Leads to Approach 3: Balanced BSTs (`TreeMap`s) with Eager Deletion.
 *    Time Complexity: `add`/`remove`: O(log M) — pushes to heaps of physical size up to M (total adds).
 *    `getMean`: O(1). `getMedian`/`getMode`: Amortized O(log M) (each stale entry is pushed once
 *    and popped at most once across the entire run, though a single read can trigger a burst of pops).
 *    Space Complexity: O(M) — heaps and tombstone maps can grow proportional to total `add` calls M,
 *    even when the live element count N is tiny.
 *
 * --- Approach 3: Balanced BSTs (`TreeMap`s) with Eager Deletion (Optimal in Java) ---
 * 0. Idea/Intuition: Maintain 3 clean, eager-deletion structures synced with the FIFO `queue`:
 *    - **Mean**: `long sum` -> `getMean()` is `(int) (sum / queue.size())` in O(1).
 *    - **Median**: Two frequency `TreeMap<Integer, Integer>`s (`lower`, `upper`) with exact counts
 *      `lowerSize` and `upperSize`. Enforce `lowerSize == total / 2` and `upperSize == total - total / 2`
 *      and `max(lower) <= min(upper)`.
 *      - Notice why `lowerSize = total / 2` is magic:
 *          total=1 -> lower=0, upper=1 -> `upper.firstKey()` is index 0 (only element)
 *          total=2 -> lower=1, upper=1 -> `upper.firstKey()` is index 1 (LARGER middle!)
 *          total=3 -> lower=1, upper=2 -> `upper.firstKey()` is index 1 (exact middle!)
 *          total=4 -> lower=2, upper=2 -> `upper.firstKey()` is index 2 (LARGER middle!)
 *        `getMedian()` is ALWAYS `upper.firstKey()` — zero odd/even branching!
 *    - **Mode**: `HashMap<Integer, Integer> valToFreq` + `TreeMap<Integer, TreeSet<Integer>> freqToVals`.
 *      Whenever `val` moves from `oldFreq` to `newFreq`, eagerly remove `val` from `freqToVals[oldFreq]`
 *      (deleting the bucket if empty) and add `val` to `freqToVals[newFreq]` (if `newFreq > 0`).
 *      `getMode()` is `freqToVals.lastEntry().getValue().first()` (max freq, smallest val).
 * 1. Why someone thinks of it: Whenever Java's `PriorityQueue` forces messy lazy deletion,
 *    switching to `TreeMap` gives true O(log N) eager deletion, strict O(N) live memory, and
 *    zero tombstone edge cases.
 * 2. Why it works: Every mutation eagerly updates all three structures before returning, so
 *    invariants hold 100% of the time with zero stale state.
 *    Time Complexity:
 *    - `addNumber` / `removeFirstAddedNumber`: Strict O(log N) — constant number of `TreeMap`/`TreeSet`
 *      operations on trees bounded strictly by `N` (the number of currently live distinct elements).
 *    - `getMean`: Strict O(1).
 *    - `getMedian` / `getMode`: Strict O(log N) worst-case (never suffers O(M) lazy-cleanup bursts!).
 *    Space Complexity: Strict O(N) — stores ONLY the `N` currently live elements across the queue,
 *    maps, and trees; removed elements are freed immediately.
 *
 * Interview Verdict:
 * Write Approach 3 (`TreeMap`s with Eager Deletion) in a 45-minute Java interview!
 * Compared to Approach 2 (Lazy-Deletion Heaps), it has strictly better worst-case time per query
 * (no bursty tombstone drain), strictly better space (O(N) live elements vs O(M) historical ops),
 * and eliminates the two hardest bugs of Lazy Deletion (forgetting to prune before comparing, and
 * tombstone stealing on duplicate boundary values). Mention Approach 2 if the interviewer asks
 * how you'd implement this in a language without a built-in balanced BST (like Python).
 *
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - Single element (`[42]`): `lower` is empty (`lowerSize = 0`), `upper = {42:1}` (`upperSize = 1`).
 *   `getMean() = 42`, `getMedian() = 42`, `getMode() = 42`.
 * - Draining down to 0 elements then refilling (`add(5), remove(), add(10)`): `freqToVals`,
 *   `lower`, `upper`, and `sum` must all cleanly return to empty/0 without NPE on the next `add()`.
 * - Even vs. odd median sizing: `[1, 2]` -> `2` (larger middle, NOT `1` or `1.5`); `[1, 2, 3]` -> `2`.
 * - Mode tie-breaking after frequency drop: `[4, 4, 2, 3]` (mode = `4`), then remove `4` ->
 *   `[4, 2, 3]` (all freq 1 -> mode must jump to smallest tied value `2`, NOT `4` or `3`).
 * - Duplicate value straddling the median boundary (`[5, 5, 5, 5]` -> `lower={5:2}, upper={5:2}`):
 *   Removing `5` from `lower` (or `upper`) and rebalancing must preserve exact counts and never
 *   delete the key `5` prematurely while its count > 1.
 * - Running sum overflow (`10^5` copies of `10^9`): `sum = 10^14` overflows `int` -> requires `long`.
 *
 * =========================================================================================
 * 4. ## Also include
 *
 * -- Key insight --
 * Question to ask yourself: "How do I support BOTH O(log N) insertion AND O(log N) arbitrary
 * deletion for median and mode without letting stale tombstones leak memory or corrupt bounds?"
 * Correctness intuition:
 *   1. For Median: Partition into `lower` (`total / 2`) and `upper` (`total - total / 2`) using
 *      two frequency `TreeMap`s. Because `total / 2` puts the extra element (on odd `total`) AND
 *      the larger middle element (on even `total`) at the exact front of `upper`, `upper.firstKey()`
 *      is ALWAYS the median!
 *   2. For Mode: Invert the frequency map into `TreeMap<Freq, TreeSet<Val>>`. Because keys are
 *      sorted by frequency and values inside each bucket are sorted ascending, the highest-frequency
 *      smallest-value mode is ALWAYS `lastEntry().getValue().first()`.
 *
 * -- Examples & ASCII Diagram --
 * State after `add(4), add(4), add(2), add(3)` (total = 4):
 *
 *   Queue (FIFO): [4, 4, 2, 3]    sum = 13 -> Mean = 13 / 4 = 3
 *
 *   Median TreeMaps (targetLower = 4/2 = 2, targetUpper = 2):
 *     lower: { 2:1, 3:1 } (size 2)  |  upper: { 4:2 } (size 2)
 *                                               ^
 *                                      upper.firstKey() = 4 (Median!)
 *
 *   Mode Inverted Map (`freq -> TreeSet<val>`):
 *     freq 1 -> { 2, 3 }
 *     freq 2 -> { 4 }   <-- lastEntry().first() = 4 (Mode!)
 *
 *   Now `removeFirstAddedNumber()` pops `4` (total becomes 3):
 *     - Queue: [4, 2, 3], sum = 9 -> Mean = 9 / 3 = 3
 *     - Median: remove `4` from `upper` -> upper = {4:1} (size 1), lower = {2:1, 3:1} (size 2).
 *       targetLower = 3/2 = 1 -> lower too big! Move `lower.lastKey()` (3) to `upper`:
 *       lower: { 2:1 } (size 1)  |  upper: { 3:1, 4:1 } (size 2) -> Median = 3!
 *     - Mode: move `4` from freq 2 -> freq 1 (delete empty freq 2 bucket!):
 *       freq 1 -> { 2, 3, 4 }  <-- lastEntry().first() = 2 (Mode!)
 *
 * -- Dry run 1: Optimal TreeMap Approach on [4, 4, 2, 3] then removeFirst() --
 *   Step | Op          | Queue      | lower (sz)      | upper (sz)      | freqToVals           | Mean/Med/Mode
 *   -----+-------------+------------+-----------------+-----------------+----------------------+--------------
 *    1   | add(4)      | [4]        | {} (0)          | {4:1} (1)       | {1:[4]}              | 4 / 4 / 4
 *    2   | add(4)      | [4,4]      | {4:1} (1)       | {4:1} (1)       | {2:[4]}              | 4 / 4 / 4
 *    3   | add(2)      | [4,4,2]    | {2:1} (1)       | {4:2} (2)       | {1:[2], 2:[4]}       | 3 / 4 / 4
 *    4   | add(3)      | [4,4,2,3]  | {2:1, 3:1} (2)  | {4:2} (2)       | {1:[2,3], 2:[4]}     | 3 / 4 / 4
 *    5   | remove()=4  | [4,2,3]    | {2:1} (1)       | {3:1, 4:1} (2)  | {1:[2,3,4]}          | 3 / 3 / 2
 *
 * -- Dry run 2: Edge Case — Drain to Empty then Refill: add(10), remove(), add(5) --
 *   Step | Op          | Queue | lower (sz) | upper (sz) | freqToVals | Mean/Med/Mode
 *   -----+-------------+-------+------------+------------+------------+--------------
 *    1   | add(10)     | [10]  | {} (0)     | {10:1} (1) | {1:[10]}   | 10 / 10 / 10
 *    2   | remove()=10 | []    | {} (0)     | {} (0)     | {}         | (empty)
 *    3   | add(5)      | [5]   | {} (0)     | {5:1} (1)  | {1:[5]}    | 5 / 5 / 5
 *
 * -- Pitfalls --
 * 1. Forgetting to delete EMPTY frequency buckets in `freqToVals`: If `freq 2`'s `TreeSet`
 *    becomes empty and you don't call `freqToVals.remove(2)`, then `freqToVals.lastEntry()`
 *    still returns `2 -> []` and `.first()` throws `NoSuchElementException`!
 * 2. Wrong median partition split (`upperSize = total / 2` instead of `lowerSize = total / 2`):
 *    On even `total = 4` (`[1, 2, 3, 4]`), both give size 2, so you'd accidentally return
 *    `lower.lastKey() = 2` (the SMALLER middle) instead of `upper.firstKey() = 3` (the LARGER middle)!
 * 3. Using `int` for `runningSum`: Silently overflows when `10^5` numbers around `10^9` are added.
 * 4. Calling `PriorityQueue.remove(obj)` in Approach 2: Looks O(log N), actually O(N) linear scan -> TLE.
 * 5. In Approach 2 (Lazy Deletion), peeking `lowerHeap.peek()` BEFORE calling `cleanHeap(lowerHeap)`:
 *    Comparing against a ghost root routes new deletions to the wrong heap and permanently
 *    corrupts `validLowerSize` and `validUpperSize`.
 *
 * -- Pattern recognition --
 * When you see: "Maintain multiple live order/frequency statistics (mean, median, mode) under
 * BOTH insertions and arbitrary/FIFO deletions"
 * -> Decompose by statistic and combine:
 *    - Running `long sum` (mean)
 *    - Two frequency `TreeMap`s (median / percentile)
 *    - Inverted `TreeMap<Freq, TreeSet<Val>>` (mode / top-frequency with tie-break)
 * Transfers directly to:
 *   - Sliding Window Median (LeetCode 480) — same two balanced BSTs for median
 *   - All O`one Data Structure (LeetCode 432) — same `val -> freq` + `freq -> bucket of vals` inversion!
 *   - Finding MK Average (LeetCode 1825) — three frequency `TreeMap`s + running middle sum
 *
 * -- Interview script --
 * "Because we can have up to 10^5 operations and values up to 10^9, recomputing median or mode
 * on each query would be O(N log N) and TLE. I'll decouple the four requirements around a single
 * FIFO `ArrayDeque`: for the mean, a `long` running sum gives O(1). For the median, instead of
 * heaps with lazy deletion (which can accumulate O(total ops) stale entries), I'll use two
 * frequency `TreeMap`s — `lower` of size `n / 2` and `upper` of size `n - n / 2` — so that for
 * both odd and even counts, `upper.firstKey()` is always the exact median (larger middle on ties)
 * with true O(log N) eager deletion. For the mode, I'll pair a `val -> freq` `HashMap` with an
 * inverted `TreeMap<Integer, TreeSet<Integer>>` mapping each frequency to the sorted set of
 * values with that frequency, deleting empty buckets eagerly so `lastEntry().getValue().first()`
 * always gives the highest-frequency smallest value. That gives us strict O(log N) time for add,
 * remove, median, and mode, O(1) time for mean, and strict O(N) space."
 *
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q1: What if we also need `removeNumber(int x)` to delete an ARBITRARY existing value `x`
 *     (not just the oldest FIFO element)?
 * A1: Our `TreeMap` median and mode structures ALREADY support arbitrary deletion in O(log N)!
 *     The only thing tied to FIFO is `queue`. Replacing `queue` with a simple `totalCount` (or
 *     checking `valToFreq.containsKey(x)`) makes arbitrary `removeNumber(x)` work in O(log N)
 *     with zero changes to the median/mode logic.
 *
 * Q2: What if `getMean()`, `getMedian()`, and `getMode()` must ALL run in strict O(1) time
 *     (not O(log N))?
 * A2: - `getMean()` is already O(1).
 *     - `getMedian()` can be O(1) by caching `int cachedMedian = upper.firstKey()` at the end of
 *       every `add`/`remove` (still O(log N) per mutation, O(1) per read).
 *     - `getMode()` can ALSO be O(1) by caching `int cachedMode = freqToVals.lastEntry().getValue().first()`
 *       after each mutation — or by replacing `TreeMap<Freq, TreeSet>` with a Doubly-Linked List
 *       of frequency buckets (like LC 432 All O`one) if you don't need tie-breaking by smallest value.
 *
 * Q3: What if values are bounded to a tiny range (e.g., `1 <= number <= 1000`)?
 * A3: We can drop all `TreeMap`s! A flat `int[1001]` frequency array handles `add`/`remove` in
 *     O(1) and answers `getMedian()` and `getMode()` by scanning 1001 buckets in O(1000) = O(1)
 *     with O(1) memory and zero GC/object overhead.
 * =========================================================================================
 */

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.TreeSet;

public class ArrayStatisticsTrackerStudyNote {

    // Common interface so our test harness can cross-check all 3 approaches identically.
    interface Tracker {
        void addNumber(int number);
        void removeFirstAddedNumber();
        int getMean();
        int getMedian();
        int getMode();
    }

    // ----------------------------------------------------------------------
    // APPROACH 1: Brute Force (O(1) add/remove, O(N log N) median, O(N) mean/mode)
    // ----------------------------------------------------------------------
    static class StatisticsTrackerBruteForce implements Tracker {
        private final Deque<Integer> queue = new ArrayDeque<>();

        @Override
        public void addNumber(int number) {
            queue.offerLast(number);
        }

        @Override
        public void removeFirstAddedNumber() {
            queue.pollFirst();
        }

        @Override
        public int getMean() {
            long sum = 0; // Use long to prevent 32-bit overflow on large values.
            for (int x : queue) {
                sum += x;
            }
            return (int) (sum / queue.size());
        }

        @Override
        public int getMedian() {
            List<Integer> sorted = new ArrayList<>(queue);
            Collections.sort(sorted);
            // Integer division `n / 2` hits the exact middle for odd n (e.g., 3/2 = 1)
            // and the LARGER of the two middle elements for even n (e.g., 4/2 = 2).
            return sorted.get(sorted.size() / 2);
        }

        @Override
        public int getMode() {
            Map<Integer, Integer> freq = new HashMap<>();
            int bestVal = Integer.MAX_VALUE;
            int bestFreq = 0;
            for (int x : queue) {
                int f = freq.merge(x, 1, Integer::sum);
                if (f > bestFreq || (f == bestFreq && x < bestVal)) {
                    bestFreq = f;
                    bestVal = x;
                }
            }
            return bestVal;
        }
    }

    // ----------------------------------------------------------------------
    // APPROACH 2: Multi-Heap + Lazy Deletion (Amortized O(log M) time, O(M) space)
    // ----------------------------------------------------------------------
    static class StatisticsTrackerLazyHeaps implements Tracker {
        private final Deque<Integer> queue = new ArrayDeque<>();
        private long sum = 0;

        // Mode state: exact live frequencies + max-heap of stale/fresh (freq, val) snapshots.
        private final Map<Integer, Integer> freqMap = new HashMap<>();
        private final PriorityQueue<int[]> modeHeap = new PriorityQueue<>((a, b) -> {
            int cmp = Integer.compare(b[0], a[0]); // Higher frequency first.
            return cmp != 0 ? cmp : Integer.compare(a[1], b[1]); // Smaller value breaks ties.
        });

        // Median state: lower (Max-Heap) of size total/2, upper (Min-Heap) of size total - total/2.
        private final PriorityQueue<Integer> lowerHeap = new PriorityQueue<>(Collections.reverseOrder());
        private final PriorityQueue<Integer> upperHeap = new PriorityQueue<>();

        // Separate tombstone maps prevent one heap from stealing a duplicate deletion meant for the other.
        private final Map<Integer, Integer> lazyLower = new HashMap<>();
        private final Map<Integer, Integer> lazyUpper = new HashMap<>();
        private int validLower = 0;
        private int validUpper = 0;

        private static void prune(PriorityQueue<Integer> heap, Map<Integer, Integer> lazy) {
            while (!heap.isEmpty() && lazy.getOrDefault(heap.peek(), 0) > 0) {
                int stale = heap.poll();
                int rem = lazy.get(stale) - 1;
                if (rem == 0) lazy.remove(stale);
                else lazy.put(stale, rem);
            }
        }

        private void balance() {
            int targetLower = (validLower + validUpper) / 2;
            while (validLower > targetLower) {
                prune(lowerHeap, lazyLower);
                upperHeap.offer(lowerHeap.poll());
                validLower--;
                validUpper++;
            }
            while (validLower < targetLower) {
                prune(upperHeap, lazyUpper);
                lowerHeap.offer(upperHeap.poll());
                validUpper--;
                validLower++;
            }
        }

        @Override
        public void addNumber(int number) {
            queue.offerLast(number);
            sum += number;

            // Mode: push new frequency snapshot lazily; older snapshots are discarded on read.
            int newFreq = freqMap.merge(number, 1, Integer::sum);
            modeHeap.offer(new int[]{newFreq, number});

            // Median: prune before peeking so we never compare against a ghost element.
            prune(upperHeap, lazyUpper);
            if (validUpper == 0 || number >= upperHeap.peek()) {
                upperHeap.offer(number);
                validUpper++;
            } else {
                lowerHeap.offer(number);
                validLower++;
            }
            balance();
        }

        @Override
        public void removeFirstAddedNumber() {
            int number = queue.pollFirst();
            sum -= number;

            // Mode: decrement live frequency; push updated snapshot if still present.
            int newFreq = freqMap.get(number) - 1;
            if (newFreq == 0) {
                freqMap.remove(number);
            } else {
                freqMap.put(number, newFreq);
                modeHeap.offer(new int[]{newFreq, number});
            }

            // Median: record tombstone in whichever partition conceptually owns `number`.
            prune(lowerHeap, lazyLower);
            if (validLower > 0 && number <= lowerHeap.peek()) {
                lazyLower.merge(number, 1, Integer::sum);
                validLower--;
            } else {
                lazyUpper.merge(number, 1, Integer::sum);
                validUpper--;
            }
            balance();
        }

        @Override
        public int getMean() {
            return (int) (sum / queue.size());
        }

        @Override
        public int getMedian() {
            prune(upperHeap, lazyUpper);
            return upperHeap.peek();
        }

        @Override
        public int getMode() {
            // Discard stale snapshots whose recorded frequency no longer matches `freqMap`.
            while (!modeHeap.isEmpty()) {
                int[] top = modeHeap.peek();
                if (freqMap.getOrDefault(top[1], 0) == top[0]) {
                    return top[1];
                }
                modeHeap.poll();
            }
            return -1;
        }
    }

    // ----------------------------------------------------------------------
    // APPROACH 3: Balanced BSTs (`TreeMap`s) with Eager Deletion
    // (OPTIMAL IN JAVA: Strict O(log N) add/remove/median/mode, O(1) mean, Strict O(N) space)
    // ----------------------------------------------------------------------
    static class StatisticsTrackerTreeMaps implements Tracker {
        // FIFO queue of live elements so we know which value to evict on removeFirstAddedNumber().
        private final Deque<Integer> queue = new ArrayDeque<>();

        // Running sum of all live elements (long prevents overflow up to 10^5 * 10^9 = 10^14).
        private long sum = 0;

        // --- MEDIAN STATE ---
        // Two frequency TreeMaps partitioning all live elements:
        //   lower: smaller `total / 2` elements;      upper: larger `total - total / 2` elements.
        // Example for live [2, 3, 4, 4]:
        //   lower = {2=1, 3=1} (lowerSize=2), upper = {4=2} (upperSize=2) -> median = upper.firstKey() = 4.
        private final TreeMap<Integer, Integer> lower = new TreeMap<>();
        private final TreeMap<Integer, Integer> upper = new TreeMap<>();
        private int lowerSize = 0;
        private int upperSize = 0;

        // --- MODE STATE ---
        // valToFreq:  maps each live value -> its exact current frequency.
        // freqToVals: inverted index mapping frequency -> sorted TreeSet of values with that frequency.
        // Example for live [2, 3, 4, 4]:
        //   valToFreq  = {2=1, 3=1, 4=2}
        //   freqToVals = {1=[2, 3], 2=[4]} -> mode = freqToVals.lastEntry().getValue().first() = 4.
        private final Map<Integer, Integer> valToFreq = new HashMap<>();
        private final TreeMap<Integer, TreeSet<Integer>> freqToVals = new TreeMap<>();

        @Override
        public void addNumber(int number) {
            queue.offerLast(number);
            sum += number;

            // 1. Update Mode structures eagerly.
            updateModeFrequency(number, +1);

            // 2. Insert into Median partitions, preserving max(lower) <= min(upper).
            if (upperSize == 0 || number >= upper.firstKey()) {
                increment(upper, number);
                upperSize++;
            } else {
                increment(lower, number);
                lowerSize++;
            }
            rebalanceMedian();
        }

        @Override
        public void removeFirstAddedNumber() {
            int number = queue.pollFirst();
            sum -= number;

            // 1. Update Mode structures eagerly (removing empty frequency buckets).
            updateModeFrequency(number, -1);

            // 2. Eagerly remove `number` from whichever median partition contains it.
            //    If `number` straddles both partitions (duplicate), removing from either is valid
            //    and rebalanceMedian() restores the exact target sizes.
            if (lower.containsKey(number)) {
                decrement(lower, number);
                lowerSize--;
            } else {
                decrement(upper, number);
                upperSize--;
            }
            rebalanceMedian();
        }

        @Override
        public int getMean() {
            // Constraints guarantee number >= 1, so Java's integer division matches floor().
            return (int) (sum / queue.size());
        }

        @Override
        public int getMedian() {
            // Because lowerSize == total / 2, upper.firstKey() is ALWAYS the exact median
            // for odd totals AND the larger of the two middle elements for even totals!
            return upper.firstKey();
        }

        @Override
        public int getMode() {
            // lastEntry() has the highest frequency; .first() is the smallest value in that bucket.
            return freqToVals.lastEntry().getValue().first();
        }

        // --- HELPER METHODS ---

        private void updateModeFrequency(int number, int delta) {
            int oldFreq = valToFreq.getOrDefault(number, 0);
            int newFreq = oldFreq + delta;

            // Remove `number` from its old frequency bucket; delete the bucket if it becomes empty
            // so `freqToVals.lastEntry()` never points to an empty TreeSet.
            if (oldFreq > 0) {
                TreeSet<Integer> oldBucket = freqToVals.get(oldFreq);
                oldBucket.remove(number);
                if (oldBucket.isEmpty()) {
                    freqToVals.remove(oldFreq);
                }
            }

            if (newFreq > 0) {
                valToFreq.put(number, newFreq);
                freqToVals.computeIfAbsent(newFreq, k -> new TreeSet<>()).add(number);
            } else {
                valToFreq.remove(number);
            }
        }

        private void rebalanceMedian() {
            int targetLower = (lowerSize + upperSize) / 2;
            while (lowerSize > targetLower) {
                int moveVal = lower.lastKey();
                decrement(lower, moveVal);
                lowerSize--;
                increment(upper, moveVal);
                upperSize++;
            }
            while (lowerSize < targetLower) {
                int moveVal = upper.firstKey();
                decrement(upper, moveVal);
                upperSize--;
                increment(lower, moveVal);
                lowerSize++;
            }
        }

        private static void increment(TreeMap<Integer, Integer> map, int key) {
            map.merge(key, 1, Integer::sum);
        }

        private static void decrement(TreeMap<Integer, Integer> map, int key) {
            int count = map.get(key);
            if (count == 1) {
                map.remove(key); // Remove key cleanly so firstKey()/lastKey() stay accurate.
            } else {
                map.put(key, count - 1);
            }
        }
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS — Cross-checking all 3 approaches on representative + edge cases
    // ----------------------------------------------------------------------
    private record Op(String type, int val) {
        static Op add(int v) { return new Op("add", v); }
        static Op rem()      { return new Op("rem", 0); }
        static Op mean()     { return new Op("mean", 0); }
        static Op med()      { return new Op("med", 0); }
        static Op mode()     { return new Op("mode", 0); }
    }

    private static List<Integer> runOps(Tracker t, List<Op> ops) {
        List<Integer> out = new ArrayList<>();
        for (Op op : ops) {
            switch (op.type()) {
                case "add"  -> t.addNumber(op.val());
                case "rem"  -> t.removeFirstAddedNumber();
                case "mean" -> out.add(t.getMean());
                case "med"  -> out.add(t.getMedian());
                case "mode" -> out.add(t.getMode());
            }
        }
        return out;
    }

    private static void verifyCase(String name, List<Op> ops, List<Integer> expected) {
        List<Integer> rBrute = runOps(new StatisticsTrackerBruteForce(), ops);
        List<Integer> rHeaps = runOps(new StatisticsTrackerLazyHeaps(), ops);
        List<Integer> rTrees = runOps(new StatisticsTrackerTreeMaps(), ops);

        boolean pass = rBrute.equals(expected)
                && rHeaps.equals(expected)
                && rTrees.equals(expected);

        System.out.printf("%-36s | %s | Output: %s%n",
                name, pass ? "PASS" : "FAIL", rTrees);

        if (!pass) {
            System.out.println("  Expected:  " + expected);
            System.out.println("  Brute:     " + rBrute);
            System.out.println("  LazyHeaps: " + rHeaps);
            System.out.println("  TreeMaps:  " + rTrees);
        }
    }

    public static void main(String[] args) {
        // 1. LC Example 1: [4, 4, 2, 3] -> mean=3, med=4, mode=4; remove 4 -> [4,2,3] -> med=3, mode=2
        verifyCase("LC Example 1",
                List.of(
                        Op.add(4), Op.add(4), Op.add(2), Op.add(3),
                        Op.mean(), Op.med(), Op.mode(),
                        Op.rem(),
                        Op.mean(), Op.med(), Op.mode()
                ),
                List.of(3, 4, 4, 3, 3, 2));

        // 2. Edge: Single element + drain to empty + refill
        verifyCase("Edge: Single -> drain -> refill",
                List.of(
                        Op.add(10), Op.mean(), Op.med(), Op.mode(),
                        Op.rem(),
                        Op.add(5), Op.mean(), Op.med(), Op.mode()
                ),
                List.of(10, 10, 10, 5, 5, 5));

        // 3. Edge: Even-size larger middle & mode tie-breaking
        verifyCase("Edge: Even median & mode ties",
                List.of(
                        Op.add(4), Op.add(2), Op.add(1),
                        Op.mean(), Op.med(), Op.mode(),
                        Op.rem(), // removes 4 -> [2, 1]
                        Op.mean(), Op.med(), Op.mode()
                ),
                List.of(2, 2, 1, 1, 2, 1));

        // 4. Edge: Duplicate boundary straddling
        verifyCase("Edge: All duplicates + removals",
                List.of(
                        Op.add(5), Op.add(5), Op.add(5), Op.add(5),
                        Op.med(), Op.mode(),
                        Op.rem(), Op.rem(),
                        Op.med(), Op.mode(),
                        Op.add(10), Op.med(), Op.mode()
                ),
                List.of(5, 5, 5, 5, 5, 5));

        // 5. Edge: Overflow test (sum exceeds Integer.MAX_VALUE)
        verifyCase("Edge: 10^9 sum overflow",
                List.of(
                        Op.add(1_000_000_000), Op.add(1_000_000_000), Op.add(1_000_000_000),
                        Op.mean(), Op.med(), Op.mode()
                ),
                List.of(1_000_000_000, 1_000_000_000, 1_000_000_000));
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 *
 * - Core pattern: Composed Balanced BSTs (`TreeMap`s) with Eager Deletion:
 *   `ArrayDeque` + `long sum` (mean) + Two Frequency `TreeMap`s (median) +
 *   Inverted `TreeMap<Freq, TreeSet<Val>>` (mode).
 * - Key observation: Java's `PriorityQueue` lacks O(log N) arbitrary deletion, forcing error-prone
 *   lazy-deletion tombstones and O(M) memory; `TreeMap` gives true O(log N) eager deletion and
 *   strict O(N) live memory across both median and mode.
 * - Worth memorizing vs. understanding: Understand *why* `lowerSize = total / 2` makes
 *   `upper.firstKey()` the exact median for BOTH odd and even sizes (when even takes the larger
 *   middle); memorize deleting empty `TreeSet` buckets from `freqToVals` immediately.
 * - Most common trap: Leaving empty `TreeSet`s inside `freqToVals` (`NoSuchElementException` on
 *   `.first()`), overflowing `int` in `sum`, or forgetting to prune before `peek()` in Lazy Heaps.
 * - One-line mental trigger: "Dynamic FIFO mean + median + mode in Java? `long sum`, two `TreeMap`s (`lower = n/2`), and `TreeMap<Freq, TreeSet<Val>>`."
 * =========================================================================================
 */


















































import java.util.*;

class StatisticsTracker {

    /*
     * Stores numbers in insertion order.
     *
     * Required for removeFirstAddedNumber().
     */
    private final Deque<Integer> queue = new ArrayDeque<>();

    /*
     * Running sum of all currently active numbers.
     *
     * long avoids integer overflow.
     */
    private long sum = 0;


    /*
     * Frequency of every currently active number.
     *
     * Example:
     *
     * 2 -> 3
     * 5 -> 2
     * 7 -> 1
     */
    private final Map<Integer, Integer> frequency = new HashMap<>();


    /*
     * Median data structures.
     *
     * LOWER:
     * Max heap containing the smaller half.
     *
     * UPPER:
     * Min heap containing the larger half.
     *
     * UPPER always contains either:
     * - The same number of valid elements as LOWER.
     * - One more valid element than LOWER.
     *
     * Therefore, upper.peek() is always the required median.
     */
    private final PriorityQueue<Integer> lower =
            new PriorityQueue<>(Comparator.reverseOrder());

    private final PriorityQueue<Integer> upper =
            new PriorityQueue<>();


    /*
     * Numbers marked for lazy deletion.
     *
     * Example:
     *
     * 5 -> 2
     *
     * Means two occurrences of 5 have been logically removed
     * but have not yet been physically removed from their heaps.
     */
    private final Map<Integer, Integer> delayed = new HashMap<>();


    /*
     * Logical sizes of the heaps.
     *
     * IMPORTANT:
     * These are not necessarily equal to lower.size()
     * and upper.size(), because those heaps may contain
     * stale elements.
     */
    private int lowerSize = 0;
    private int upperSize = 0;


    /*
     * ModeEntry represents:
     *
     * number    -> actual number
     * frequency -> its current frequency
     */
    private record ModeEntry(int number, int frequency) {}


    /*
     * Mode ordering:
     *
     * 1. Higher frequency first.
     * 2. Smaller number first when frequencies are equal.
     *
     * Therefore, modeSet.first() always gives the mode.
     */
    private static final Comparator<ModeEntry> MODE_ORDER =
            Comparator.comparingInt(ModeEntry::frequency)
                    .reversed()
                    .thenComparingInt(ModeEntry::number);


    /*
     * Ordered set containing one entry per active distinct number.
     */
    private final TreeSet<ModeEntry> modeSet =
            new TreeSet<>(MODE_ORDER);


    /*
     * ---------------------------------------------------------
     * ADD NUMBER
     * ---------------------------------------------------------
     *
     * Time Complexity: O(log n)
     *
     * Queue insertion: O(1)
     * HashMap operations: O(1) expected
     * Heap insertion: O(log n)
     * Heap balancing: O(log n)
     * TreeSet updates: O(log d)
     *
     * Overall: O(log n)
     */
    public void addNumber(int number) {

        // 1. Maintain insertion order.
        queue.offerLast(number);

        // 2. Maintain running sum.
        sum += number;


        // 3. Update the frequency and mode set.
        updateFrequency(number, 1);


        // 4. Insert the number into the correct median heap.
        //
        // If it belongs to the smaller half, insert into LOWER.
        // Otherwise, insert into UPPER.
        if (lower.isEmpty() || number <= lower.peek()) {

            lower.offer(number);
            lowerSize++;

        } else {

            upper.offer(number);
            upperSize++;
        }


        // 5. Restore the median heap invariants.
        rebalance();
    }


    /*
     * ---------------------------------------------------------
     * REMOVE FIRST ADDED NUMBER
     * ---------------------------------------------------------
     *
     * Time Complexity: O(log n) amortized
     *
     * We do not search inside either heap.
     * Instead, we use lazy deletion.
     */
    public void removeFirstAddedNumber() {

        // 1. Remove the oldest inserted number.
        int number = queue.pollFirst();


        // 2. Update running sum.
        sum -= number;


        // 3. Update the frequency and mode set.
        updateFrequency(number, -1);


        // 4. Mark this number for lazy deletion.
        delayed.merge(number, 1, Integer::sum);


        /*
         * 5. Decide which heap's logical size to decrease.
         *
         * If number <= lower.peek(), logically remove it
         * from LOWER.
         *
         * Otherwise, logically remove it from UPPER.
         *
         * When duplicate values exist at the boundary,
         * their instances are interchangeable.
         */
        prune(lower);

        if (!lower.isEmpty() && number <= lower.peek()) {

            lowerSize--;

        } else {

            upperSize--;
        }


        /*
         * 6. Remove stale elements if they appear at heap tops.
         *
         * This is necessary before moving elements between heaps.
         */
        prune(lower);
        prune(upper);


        // 7. Restore the heap size invariant.
        rebalance();
    }


    /*
     * ---------------------------------------------------------
     * UPDATE FREQUENCY AND MODE
     * ---------------------------------------------------------
     *
     * delta = +1 -> insertion
     * delta = -1 -> deletion
     *
     * Time Complexity: O(log d)
     *
     * We remove the previous entry before inserting the updated
     * frequency because TreeSet ordering depends on frequency.
     */
    private void updateFrequency(int number, int delta) {

        int oldFrequency = frequency.getOrDefault(number, 0);


        // Remove the old mode entry if this number already exists.
        if (oldFrequency > 0) {

            modeSet.remove(new ModeEntry(number, oldFrequency));
        }


        // Calculate the updated frequency.
        int newFrequency = oldFrequency + delta;


        if (newFrequency == 0) {

            // The number no longer exists in our collection.
            frequency.remove(number);

        } else {

            // Store its new frequency.
            frequency.put(number, newFrequency);

            // Insert the updated entry into the ordered set.
            modeSet.add(new ModeEntry(number, newFrequency));
        }
    }


    /*
     * ---------------------------------------------------------
     * PRUNE STALE ELEMENTS
     * ---------------------------------------------------------
     *
     * Time Complexity: O(k log n)
     *
     * k = number of stale elements removed in this invocation.
     *
     * Each stale element is physically removed only once
     * throughout its lifetime.
     */
    private void prune(PriorityQueue<Integer> heap) {

        while (!heap.isEmpty()) {

            int number = heap.peek();

            int count = delayed.getOrDefault(number, 0);


            // The heap top is valid.
            if (count == 0) {
                break;
            }


            // Physically remove the stale element.
            heap.poll();


            // Update the delayed deletion count.
            if (count == 1) {

                delayed.remove(number);

            } else {

                delayed.put(number, count - 1);
            }
        }
    }


    /*
     * ---------------------------------------------------------
     * REBALANCE MEDIAN HEAPS
     * ---------------------------------------------------------
     *
     * Invariants:
     *
     * 1. lowerSize <= upperSize
     *
     * 2. upperSize <= lowerSize + 1
     *
     * 3. Every valid element in LOWER <= every valid element
     *    in UPPER.
     *
     * After balancing:
     *
     * upper.peek() = required median.
     *
     * Time Complexity: O(log n)
     */
    private void rebalance() {

        // Remove stale elements before accessing heap tops.
        prune(lower);
        prune(upper);


        /*
         * Case 1:
         *
         * LOWER has more elements than UPPER.
         *
         * Move the maximum element from LOWER to UPPER.
         */
        if (lowerSize > upperSize) {

            upper.offer(lower.poll());

            lowerSize--;
            upperSize++;
        }


        /*
         * Case 2:
         *
         * UPPER has more than one extra element.
         *
         * Move the minimum element from UPPER to LOWER.
         */
        else if (upperSize > lowerSize + 1) {

            lower.offer(upper.poll());

            upperSize--;
            lowerSize++;
        }


        // Clean any stale elements exposed after moving.
        prune(lower);
        prune(upper);
    }


    /*
     * ---------------------------------------------------------
     * GET MEAN
     * ---------------------------------------------------------
     *
     * Time Complexity: O(1)
     *
     * We maintain the sum incrementally.
     * No iteration is necessary.
     */
    public int getMean() {

        return (int) (sum / queue.size());
    }


    /*
     * ---------------------------------------------------------
     * GET MEDIAN
     * ---------------------------------------------------------
     *
     * Time Complexity: O(log n) worst case for pruning,
     * amortized O(1) over the sequence of operations.
     *
     * The median is the minimum element of UPPER.
     *
     * When the collection has an even number of elements,
     * UPPER contains the larger middle element at its root.
     */
    public int getMedian() {

        // Ensure the root is not a stale element.
        prune(lower);
        prune(upper);

        return upper.peek();
    }


    /*
     * ---------------------------------------------------------
     * GET MODE
     * ---------------------------------------------------------
     *
     * Time Complexity: O(1)
     *
     * TreeSet.first() directly provides:
     *
     * 1. Maximum frequency.
     * 2. Smallest number when frequencies tie.
     */
    public int getMode() {

        return modeSet.first().number();
    }
}

