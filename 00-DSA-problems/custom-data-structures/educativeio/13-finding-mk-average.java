/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * You are given two integers, m and k, and a stream of integers.
 * Design a data structure that efficiently calculates the MK Average:
 * 1. If stream has < m elements, return -1.
 * 2. Otherwise, look at the last m elements. Remove the smallest k and largest k.
 * 3. Calculate the average of the remaining (m - 2k) elements, rounded down.
 * 
 * Constraints: 3 <= m <= 10^5, 1 <= 2k < m, 1 <= num <= 10^5.
 * At most 10^5 calls to addElement and calculateMKAverage.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * 1. Is the window size `m` and removal count `k` fixed for the lifetime of the object? 
 *    (If they can change dynamically, caching elements in rigid structures breaks, forcing us to use flexible Fenwick/Segment trees).
 * 2. Can the stream contain duplicate numbers? 
 *    (Knowing this dictates whether we can use a standard `TreeSet` or if we must use a `TreeMap` to track value frequencies).
 * 3. Is `calculateMKAverage` called frequently compared to `addElement`? 
 *    (If averages are rarely requested, lazy evaluation or on-the-fly sorting might outperform strictly maintaining the boundaries on every insert).
 * 4. Will the sum of the middle elements exceed the 32-bit signed integer limit? 
 *    (With `m` up to 10^5 and numbers up to 10^5, the sum can reach 10 billion, which prevents overflow bugs by mandating a `long` accumulator).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement: Maintain a sliding window of data and instantly know the sum 
 *   of its "middle" elements, ignoring the sliding top and bottom extremes. 
 *   The binding constraint is maintaining sorted order dynamically as elements continuously enter and exit.
 * 
 * --- Approach 1: Queue + Sorting (The Brute Force) ---
 * 1. Intuition: Store the stream in a Queue. On query, convert to list, sort, drop ends, calculate.
 * 2. Try: Keep a `LinkedList` of size `m`. On `calculateMKAverage()`, dump to an `ArrayList`, 
 *    call `Collections.sort()`, sum from index `k` to `m - k - 1`, and divide.
 * 3. Why it works: Perfectly follows the literal instructions of the problem.
 * 4. Why it's too slow: Calculating the average requires a full sort. 10^5 elements 
 *    and 10^5 queries will trigger a Time Limit Exceeded error.
 * 5. Bottleneck: Re-sorting the entire window every time, even though 99% of elements haven't changed relative order.
 * 
 * Time Complexity: O(M log M) per calculation.
 * Space Complexity: O(M).
 * 
 * --- Approach 2: Two Heaps (Min/Max Priority Queues) ---
 * 1. Intuition: Heaps are great for tracking top and bottom elements.
 * 2. Try: Maintain three structures: a Max-Heap for the bottom `k`, a Min-Heap for the top `k`, and something for the middle.
 * 3. Why it works: Finding the extremes is fast.
 * 4. Why it's too costly: Removing an element that falls out of the sliding window takes O(M) time 
 *    if it is buried in the middle of a Java `PriorityQueue`.
 * 5. Bottleneck: Arbitrary deletions in standard Heaps take O(N).
 * 
 * Time Complexity: O(M) per addition (due to arbitrary removal).
 * Space Complexity: O(M).
 * 
 * --- Approach 3: Three TreeMaps (The Optimal Solution) ---
 * 1. Intuition: A Balanced Binary Search Tree (`TreeMap`) keeps elements sorted and allows 
 *    O(log M) insertions, deletions, and minimum/maximum queries.
 * 2. Try: Divide the window into three separate TreeMaps: `left` (smallest `k`), 
 *    `right` (largest `k`), and `mid` (the rest). Keep a running `midSum`.
 * 3. Why it works: When a new element enters, we add it to the correct map. When an old element 
 *    exits, we remove it. If the maps grow or shrink past their designated sizes, we pass the 
 *    minimum or maximum elements between the maps to rebalance them.
 * 4. Which to write in an interview?
 *    Approach 3. While Fenwick trees also work, Three TreeMaps directly model the domain of the problem 
 *    (Top K, Bottom K, Middle) and demonstrate strong software engineering organization and boundary management.
 * 
 * Time Complexity: O(log M) per insertion. O(1) per calculation.
 * Space Complexity: O(M) — exactly M elements distributed across the queue and maps.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Unfilled Window: The stream hasn't reached size `m`. Instead of heavily adapting our TreeMaps 
 *   to balance half-filled states, delay initialization until exactly `m` elements arrive.
 * - Duplicates: The stream is `[5, 5, 5, 5]`. A standard `TreeSet` collapses duplicates. 
 *   We must use a `TreeMap<Integer, Integer>` to map the *number* to its *frequency count*.
 * - Rebalancing Empty Maps: If we pull elements from `mid` when it is empty, we will hit a 
 *   `NoSuchElementException`. Orchestrate rebalancing to push excesses *into* `mid` before pulling deficits *out*.
 * 
 * ============================================================================
 * 4. KEY INSIGHTS & PITFALLS
 * ============================================================================
 * > Key Insight: "Delayed Initialization." Managing boundaries of 3 TreeMaps when the window 
 *   is partially full is a nightmare. Dump the first `m - 1` elements into a queue. 
 *   The exact moment the `m`-th element arrives, sort once and perfectly distribute.
 * 
 * ASCII DRY RUN (Steady-State Rebalancing):
 * m = 5, k = 1. 
 * State: left=[1], mid=[3, 4], right=[10]. Queue: [1, 3, 4, 10]. Add: 2.
 * 1. Evict oldest: Queue pops 1. Remove 1 from left. left is now empty (size 0).
 * 2. Insert new: 2 is smaller than right and mid bounds -> goes to mid. mid becomes [2, 3, 4].
 * 3. Push Excess: left/right not oversized. No action.
 * 4. Pull Deficit: left is size 0 (needs 1). Pull minimum from mid (2) and move to left.
 * Final State: left=[2], mid=[3, 4], right=[10]. Averages instantly to (3+4)/2 = 3.
 * 
 * Pitfalls: Integer overflow on `midSum`. Always use a `long` for the running accumulator.
 */

import java.util.*;

public class MKAverageStudy {

    static class MKAverage {
        private final int m;
        private final int k;
        
        // Tracks the raw sequence of the stream to know what to evict when the window slides
        // Example: Stream is [3, 1, 10, 5, 5, 5]. Queue ensures we drop '3' first.
        private final Queue<Integer> streamQueue;
        
        // Maps to track frequencies of elements in their respective buckets.
        // TreeMap keeps keys sorted automatically. Key = Number, Value = Frequency Count.
        private final TreeMap<Integer, Integer> left;  // Stores the smallest 'k' elements
        private final TreeMap<Integer, Integer> mid;   // Stores the middle 'm - 2k' elements
        private final TreeMap<Integer, Integer> right; // Stores the largest 'k' elements
        
        // Tracks the exact number of elements in each bucket to trigger rebalancing
        private int leftSize;
        private int midSize;
        private int rightSize;
        
        // Accumulator for the middle bucket to answer calculate queries in O(1) time
        private long midSum;

        public MKAverage(int m, int k) {
            this.m = m;
            this.k = k;
            this.streamQueue = new LinkedList<>();
            this.left = new TreeMap<>();
            this.mid = new TreeMap<>();
            this.right = new TreeMap<>();
            this.leftSize = 0;
            this.midSize = 0;
            this.rightSize = 0;
            this.midSum = 0;
        }

        /**
         * Helper: Safely increments the frequency of a number in a specific map.
         */
        private void add(TreeMap<Integer, Integer> map, int num) {
            map.put(num, map.getOrDefault(num, 0) + 1);
        }

        /**
         * Helper: Safely decrements the frequency of a number, removing the key entirely if it hits 0.
         */
        private void remove(TreeMap<Integer, Integer> map, int num) {
            int count = map.get(num);
            if (count == 1) {
                map.remove(num);
            } else {
                map.put(num, count - 1);
            }
        }

        /**
         * Adds the integer num to the stream and maintains the 3-bucket invariant.
         * Time: O(log M) steady state. O(M log M) exactly once at initialization.
         */
        public void addElement(int num) {
            streamQueue.offer(num);

            // KEY INSIGHT: Delayed Initialization
            // Bypass complex rebalancing logic until we have exactly 'm' elements
            if (streamQueue.size() < m) {
                return;
            } else if (streamQueue.size() == m) {
                // The exact moment the window is full, initialize the maps cleanly
                List<Integer> initialList = new ArrayList<>(streamQueue);
                Collections.sort(initialList);
                
                for (int i = 0; i < k; i++) {
                    add(left, initialList.get(i));
                    leftSize++;
                }
                for (int i = k; i < m - k; i++) {
                    add(mid, initialList.get(i));
                    midSize++;
                    midSum += initialList.get(i);
                }
                for (int i = m - k; i < m; i++) {
                    add(right, initialList.get(i));
                    rightSize++;
                }
                return;
            }

            // ==========================================
            // STEADY STATE (Window size > m)
            // ==========================================

            // 1. Evict the oldest element that fell out of the sliding window
            int oldest = streamQueue.poll();
            if (left.containsKey(oldest)) {
                remove(left, oldest);
                leftSize--;
            } else if (right.containsKey(oldest)) {
                remove(right, oldest);
                rightSize--;
            } else {
                // If it wasn't in the extremes, it must be in the middle bucket
                remove(mid, oldest);
                midSize--;
                midSum -= oldest;
            }

            // 2. Insert the new element into the logically correct bucket based on boundaries
            if (leftSize > 0 && num <= left.lastKey()) {
                add(left, num);
                leftSize++;
            } else if (rightSize > 0 && num >= right.firstKey()) {
                add(right, num);
                rightSize++;
            } else {
                add(mid, num);
                midSize++;
                midSum += num;
            }

            // 3. Rebalance the buckets
            // Rule A: Push excesses OUT of the side buckets INTO mid first.
            // This guarantees 'mid' will not be empty if we need to pull from it later.
            while (leftSize > k) {
                int maxLeft = left.lastKey();
                remove(left, maxLeft);
                leftSize--;
                
                add(mid, maxLeft);
                midSize++;
                midSum += maxLeft;
            }
            
            while (rightSize > k) {
                int minRight = right.firstKey();
                remove(right, minRight);
                rightSize--;
                
                add(mid, minRight);
                midSize++;
                midSum += minRight;
            }

            // Rule B: Pull deficits INTO the side buckets FROM mid.
            while (leftSize < k) {
                int minMid = mid.firstKey();
                remove(mid, minMid);
                midSize--;
                midSum -= minMid;
                
                add(left, minMid);
                leftSize++;
            }
            
            while (rightSize < k) {
                int maxMid = mid.lastKey();
                remove(mid, maxMid);
                midSize--;
                midSum -= maxMid;
                
                add(right, maxMid);
                rightSize++;
            }
        }

        /**
         * Returns the current MK Average for the stream.
         * Time: O(1)
         */
        public int calculateMKAverage() {
            if (streamQueue.size() < m) {
                return -1;
            }
            // Average = Sum / Count. Integer division naturally rounds down.
            return (int) (midSum / (m - 2 * k));
        }
    }

    public static void main(String[] args) {
        MKAverage obj = new MKAverage(3, 1);
        obj.addElement(3);
        obj.addElement(1);
        System.out.println(obj.calculateMKAverage()); // Expected: -1 (only 2 elements)
        
        obj.addElement(10);
        System.out.println(obj.calculateMKAverage()); // Expected: 3 (sorted: 1, 3, 10 -> mid is 3)
        
        obj.addElement(5);
        obj.addElement(5);
        obj.addElement(5);
        System.out.println(obj.calculateMKAverage()); // Expected: 5 (sorted: 5, 5, 5 -> mid is 5)
    }
}

/**
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q: What if `k` wasn't fixed, but passed as an argument to `calculateMKAverage(int dynamicK)`?
 * A: Our 3-TreeMap solution rigidly enforces sizes based on a fixed `k` and would completely break. 
 *    We would need to pivot to a Binary Indexed Tree (Fenwick Tree) or Segment Tree over the domain 
 *    of possible values (1 to 10^5). We would track frequencies and running sums in the tree, 
 *    allowing us to dynamically query the sum between the K-th and (M-K)-th elements in O(log(MAX_VAL)) time.
 * 
 * ============================================================================
 * 7. SHORT SUMMARY
 * ============================================================================
 * Core Pattern: Three TreeMaps + Delayed Initialization.
 * Key Observation: A sliding window with dynamic sorted extremes requires balancing data across 
 * designated segments. By tracking frequencies in a Balanced BST, insertions/deletions/extremes 
 * cost a stable O(log M). 
 * What to Memorize: Push excesses OUT of sides into middle BEFORE pulling deficits INTO sides 
 * from middle. This prevents NullPointer/NoSuchElement exceptions from an empty middle bucket.
 * Most Common Trap: Integer Overflow. The `midSum` accumulator can easily exceed 2.14 billion. 
 * Always use `long`.
 * Mental Trigger: "Sliding window middle average" -> "3 TreeMaps: Left K, Mid, Right K".
 * ============================================================================
 */














import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.NavigableMap;
import java.util.TreeMap;

class MKAverage {

    private final int m;
    private final int k;
    private final int midSize;

    /*
     * WHY DO WE NEED A QUEUE?
     *
     * We must always consider the most recent m numbers.
     * The oldest number expires whenever a new number arrives
     * after the window is already full.
     *
     * ArrayDeque keeps insertion order and removes the oldest
     * element in O(1) time.
     */
    private final Deque<Integer> window = new ArrayDeque<>();

    /*
     * WHY THREE TREEMAPS?
     *
     * low  = k smallest occurrences.
     * mid  = m - 2*k middle occurrences.
     * high = k largest occurrences.
     *
     * Each TreeMap stores:
     *     number -> frequency
     *
     * Example:
     *     {2 -> 3, 5 -> 1}
     *
     * represents the multiset [2, 2, 2, 5].
     *
     * TreeMap keeps DISTINCT keys sorted, but its size()
     * does not count duplicate occurrences.
     */
    private final NavigableMap<Integer, Integer> low =
            new TreeMap<>();

    private final NavigableMap<Integer, Integer> mid =
            new TreeMap<>();

    private final NavigableMap<Integer, Integer> high =
            new TreeMap<>();

    /*
     * IMPORTANT:
     *
     * These counters count TOTAL OCCURRENCES, not distinct keys.
     *
     * For example:
     *     low = {2 -> 3}
     *
     * Then:
     *     low.size() = 1       (one distinct key)
     *     lowCount   = 3       (three occurrences)
     *
     * We need occurrence counts because k refers to the number
     * of elements that must be discarded.
     */
    private int lowCount = 0;
    private int midCount = 0;
    private int highCount = 0;

    /*
     * The sum of all values in mid.
     *
     * Use long because as many as 100,000 values of up to
     * 100,000 can contribute to the sum: up to 10^10.
     */
    private long midSum = 0;

    public MKAverage(int m, int k) {
        this.m = m;
        this.k = k;
        this.midSize = m - 2 * k;
    }

    // ---------------------------------------------------------
    // 1. BASIC MULTISET OPERATIONS
    // ---------------------------------------------------------

    /*
     * Add exactly one occurrence of value to the given multiset.
     *
     * merge(value, 1, Integer::sum) means:
     * - If value is absent, store frequency 1.
     * - If value exists, increment its frequency.
     *
     * Time: O(log m).
     */
    private void addToSet(
            NavigableMap<Integer, Integer> set,
            int value) {

        set.merge(value, 1, Integer::sum);
    }

    /*
     * Remove exactly one occurrence.
     *
     * Example:
     *     {2 -> 3}
     *
     * Removing one 2 produces:
     *     {2 -> 2}
     *
     * If its frequency was already 1, remove the key entirely.
     *
     * Time: O(log m).
     */
    private void removeFromSet(
            NavigableMap<Integer, Integer> set,
            int value) {

        int frequency = set.get(value);

        if (frequency == 1) {
            set.remove(value);
        } else {
            set.put(value, frequency - 1);
        }
    }

    // ---------------------------------------------------------
    // 2. PARTITION-AWARE OPERATIONS
    // ---------------------------------------------------------

    /*
     * Every insertion/removal goes through these helpers.
     *
     * WHY?
     *
     * We want the TreeMap contents, occurrence counts, and midSum
     * to stay synchronized.
     *
     * This prevents subtle bugs where a value moves between
     * partitions but midSum is not updated.
     */

    private void addLow(int value) {
        addToSet(low, value);
        lowCount++;
    }

    private void removeLow(int value) {
        removeFromSet(low, value);
        lowCount--;
    }

    private void addMid(int value) {
        addToSet(mid, value);
        midCount++;
        midSum += value;
    }

    private void removeMid(int value) {
        removeFromSet(mid, value);
        midCount--;
        midSum -= value;
    }

    private void addHigh(int value) {
        addToSet(high, value);
        highCount++;
    }

    private void removeHigh(int value) {
        removeFromSet(high, value);
        highCount--;
    }

    // ---------------------------------------------------------
    // 3. INITIALIZE THE FIRST COMPLETE WINDOW
    // ---------------------------------------------------------

    /*
     * Before m elements arrive, calculateMKAverage() returns -1.
     *
     * Once the first m elements arrive, we must establish
     * the three partitions for the first time.
     *
     * We sort the window ONCE:
     *
     *     [smallest k]
     *     [middle m - 2*k]
     *     [largest k]
     *
     * Subsequent updates do not require sorting the whole window.
     *
     * Time: O(m log m).
     */
    private void initializePartitions() {

        int[] sorted = window.stream()
                .mapToInt(Integer::intValue)
                .toArray();

        Arrays.sort(sorted);

        for (int i = 0; i < m; i++) {

            if (i < k) {
                addLow(sorted[i]);

            } else if (i < m - k) {
                addMid(sorted[i]);

            } else {
                addHigh(sorted[i]);
            }
        }
    }

    // ---------------------------------------------------------
    // 4. INSERT A NEW VALUE INTO AN APPROPRIATE PARTITION
    // ---------------------------------------------------------

    /*
     * We use the boundary values to decide where a number belongs.
     *
     * If value <= the largest value in low:
     *     It belongs in low.
     *
     * Otherwise, if value >= the smallest value in high:
     *     It belongs in high.
     *
     * Otherwise:
     *     It belongs in mid.
     *
     * WHY DO WE COMPARE BOUNDARIES?
     *
     * We maintain the ordering invariant:
     *
     *     every low value <= every mid value
     *     every mid value <= every high value
     *
     * Therefore, the boundary comparisons tell us the appropriate
     * partition without sorting the entire window.
     *
     * The insertion may temporarily change partition sizes.
     * rebalance() will restore the required counts afterward.
     */
    private void insertValue(int value) {

        if (lowCount == 0 || value <= low.lastKey()) {

            addLow(value);

        } else if (highCount == 0 || value >= high.firstKey()) {

            addHigh(value);

        } else {

            addMid(value);
        }
    }

    // ---------------------------------------------------------
    // 5. REMOVE THE EXPIRED VALUE
    // ---------------------------------------------------------

    /*
     * The queue tells us WHICH VALUE expires.
     *
     * The boundary comparisons tell us which partition contains
     * an occurrence of that value.
     *
     * Because equal values are interchangeable, we may remove
     * any matching occurrence, even if that particular numeric
     * occurrence originally entered a different partition.
     *
     * Example:
     *     low = {5 -> 1}
     *     mid = {5 -> 2}
     *
     * Removing one occurrence of 5 from low instead of mid is
     * still valid if the expired value is 5. The resulting
     * multiset of values is identical.
     *
     * This works because the partition ordering is maintained.
     */
    private void removeValue(int value) {

        if (lowCount > 0 && value <= low.lastKey()) {

            removeLow(value);

        } else if (highCount > 0 && value >= high.firstKey()) {

            removeHigh(value);

        } else {

            removeMid(value);
        }
    }

    // ---------------------------------------------------------
    // 6. REBALANCE THE THREE PARTITIONS
    // ---------------------------------------------------------

    /*
     * THIS IS THE CORE OF THE SOLUTION.
     *
     * The required occurrence counts are:
     *
     *     lowCount  = k
     *     midCount  = m - 2*k
     *     highCount = k
     *
     * We fix low first, then high.
     *
     * Notice that we use lowCount, midCount, and highCount.
     * We NEVER use TreeMap.size() to check the number of elements.
     *
     * Moving boundary elements preserves sorted ordering.
     *
     * Each operation changes a partition count and, when relevant,
     * adjusts midSum through addMid/removeMid.
     */
    private void rebalance() {

        // STEP A: Make low contain exactly k occurrences.

        while (lowCount > k) {

            /*
             * low has too many elements.
             *
             * Move its LARGEST element into mid.
             * It is the closest boundary element, so the ordering
             * between low and mid remains valid.
             */
            int value = low.lastKey();

            removeLow(value);
            addMid(value);
        }

        while (lowCount < k) {

            /*
             * low has too few elements.
             *
             * Move the SMALLEST element in mid into low.
             *
             * That value is the next-smallest candidate for low.
             */
            int value = mid.firstKey();

            removeMid(value);
            addLow(value);
        }

        // STEP B: Make high contain exactly k occurrences.

        while (highCount > k) {

            /*
             * high has too many elements.
             *
             * Move its SMALLEST element into mid.
             */
            int value = high.firstKey();

            removeHigh(value);
            addMid(value);
        }

        while (highCount < k) {

            /*
             * high has too few elements.
             *
             * Move the LARGEST element in mid into high.
             */
            int value = mid.lastKey();

            removeMid(value);
            addHigh(value);
        }

        /*
         * WHY IS MID'S SIZE NOW CORRECT?
         *
         * The total number of occurrences across all three
         * partitions is exactly m.
         *
         * We have made:
         *     lowCount  = k
         *     highCount = k
         *
         * Therefore:
         *
         *     midCount = m - k - k
         *              = m - 2*k
         *
         * We don't need a separate loop to rebalance mid.
         *
         * The partition boundaries are preserved because we only
         * move the smallest or largest boundary elements.
         */
    }

    // ---------------------------------------------------------
    // 7. ADD A NUMBER TO THE STREAM
    // ---------------------------------------------------------

    public void addElement(int num) {

        // Preserve arrival order for FIFO expiration.
        window.addLast(num);

        if (window.size() < m) {

            /*
             * The window is not full yet.
             *
             * We don't need to build partitions until the first
             * valid MK Average can be calculated.
             */
            return;
        }

        if (window.size() == m && lowCount == 0) {

            /*
             * This is the first time the window has reached m.
             *
             * Initialize the partitions from the complete window.
             */
            initializePartitions();
            return;
        }

        /*
         * The window is now m+1 elements long.
         *
         * Remove the oldest element from BOTH:
         * 1. The FIFO window.
         * 2. Its corresponding partition.
         */
        if (window.size() > m) {

            int expired = window.removeFirst();

            removeValue(expired);
        }

        /*
         * Insert the new value into the appropriate partition.
         */
        insertValue(num);

        /*
         * Removing an old value and inserting a new one may change
         * the partition counts.
         *
         * Restore the exact counts before the next query.
         */
        rebalance();
    }

    // ---------------------------------------------------------
    // 8. CALCULATE THE MK AVERAGE
    // ---------------------------------------------------------

    public int calculateMKAverage() {

        if (window.size() < m) {
            return -1;
        }

        /*
         * The middle partition contains exactly m - 2*k elements.
         *
         * Its sum is already maintained incrementally in midSum.
         *
         * No sorting or iteration is required.
         *
         * Integer division rounds down because the inputs and sum
         * are nonnegative.
         *
         * Time: O(1).
         */
        return (int) (midSum / midSize);
    }
}



