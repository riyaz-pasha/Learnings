/**
 * 0. ## Problem Statement
 * Design an Array Statistics Tracker.
 * 
 * Implement a `StatisticsTracker` class that tracks values in an array and supports:
 * - `StatisticsTracker()`: Initializes an empty tracker.
 * - `void addNumber(int number)`: Adds `number` to the tracker.
 * - `void removeFirstAddedNumber()`: Removes the earliest added number (FIFO order).
 * - `int getMean()`: Returns the floored mean of the numbers.
 * - `int getMedian()`: Returns the median. If the count is even, take the LARGER of the two middle values.
 * - `int getMode()`: Returns the mode (most frequent element). If tied, return the SMALLEST one.
 * 
 * Constraints:
 * - 1 <= number <= 10^9
 * - At most 10^5 total calls.
 * - Statistics queries and remove calls are only made when the array is non-empty.
 * 
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "How do we handle ties for the median when the array size is even?"
 *    -> Crucial: The problem explicitly states to take the larger of the two choices. 
 *       This tells us exactly how to balance our two median heaps (Upper half must take the extra element).
 * 2. "How do we handle ties for the mode?"
 *    -> We must return the lexically smallest value, meaning our Mode tracking PriorityQueue needs a secondary sort condition.
 * 3. "Can the sum exceed a 32-bit signed integer?"
 *    -> Yes. 10^5 elements of up to 10^9 can sum to 10^14. We must use a `long` for the running sum.
 * 4. "Is Java's PriorityQueue.remove(Object) fast enough for this?"
 *    -> No. It's O(N). We MUST use lazy deletion (Map-backed frequency counters of stale elements).
 * 
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: Up to 10^5 queries of additions, removals, and stats queries. 
 * If any update or query takes O(N), we hit 10^10 operations, causing a Time Limit Exceeded (TLE). 
 * We must keep operations to O(1) or O(log N).
 * 
 * --- Approach 1: The Brute Force ---
 * Idea: Use a `Queue` for maintaining FIFO order. When queried, sum all elements for mean, copy and sort 
 * for median, and build a HashMap to find the mode.
 * - Why it works: Simulates the problem exactly.
 * - Why it's too slow: Recalculating the median requires sorting, taking O(N log N) per query.
 * Time Complexity: add/remove: O(1). mean: O(N). median: O(N log N). mode: O(N).
 * Space Complexity: O(N)
 * 
 * --- Approach 2: Balanced Binary Search Trees ---
 * Idea: If we were in Python, we'd use `SortedList`. In C++, we'd use `std::multiset`.
 * - Why it works: Self-balancing BSTs support O(log N) insertion, deletion, and index-based lookups for the median.
 * - Why it's a trap in Java: Java lacks an indexed `TreeSet`. You'd have to write a custom Segment Tree, 
 *   Fenwick Tree (with coordinate compression since values are 10^9), or your own Red-Black Tree.
 * 
 * --- Approach 3: Multiple Heaps with Lazy Deletion (Optimal for Java) ---
 * Idea: Break the statistics down into specialized structures:
 * - Mean: A simple `long sum` and the queue's size.
 * - Mode: A Max-Heap storing `[frequency, number]`. Instead of finding and updating old entries when 
 *   a frequency changes, we just blindly add the new `[new_frequency, number]`. When querying the mode, 
 *   we check if the top of the heap matches the *actual* frequency in a HashMap. If it's stale, we pop it.
 * - Median: Two heaps (`Lower` Max-Heap, `Upper` Min-Heap). To remove elements in O(1), we keep two HashMaps 
 *   (`lazyLower`, `lazyUpper`) that track elements waiting to be deleted. We balance the heaps using 
 *   "logical" size counters rather than their physical sizes.
 * 
 * Time Complexity: 
 *   - add/remove: O(log N) for heap pushes.
 *   - getMean: O(1).
 *   - getMedian / getMode: Amortized O(1) (Worst-case O(log N) if it needs to clean up stale entries).
 * Space Complexity: O(N) — Storing elements and lazy-deletion metadata.
 * 
 * Verdict: Approach 3 is the standard advanced competitive programming pattern for Java. I would code this 
 * immediately in an interview, explaining the Lazy Deletion concept before touching the whiteboard.
 * 
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - Size is exactly 1: Both Median and Mode must gracefully handle and return that single element.
 * - Ties in Mode: Stream = `[4, 2]`. Freqs are identical. Max-heap must correctly prioritize `2` over `4`.
 * - The "Ghost Boundary" tie: The removed element exists physically in `upperHeap` but we conceptually delete it 
 *   from `lowerHeap` because `val == lowerHeap.peek()`. This is completely safe because identical values 
 *   are interchangeable across the boundary.
 * 
 * =========================================================================================
 * 4. ## Also include
 * 
 * -- Key insight --
 * **Lazy Deletion**: Never call `.remove(Object)` on a Java PriorityQueue. It takes O(N) time. 
 * Instead, use a Map to tally what should be removed, and only `poll()` the heap when a deleted element 
 * naturally surfaces to the `.peek()` position.
 * 
 * -- Dry run (Median Logic on Removal) --
 * Queue = [1, 2, 3], Lower = [1] (max-heap), Upper = [2, 3] (min-heap). Total Valid = 3. targetLower = 3/2 = 1.
 * Action: removeFirstAddedNumber() -> removes 1.
 * 1. Is 1 <= lower.peek() (1)? Yes.
 * 2. Logically remove 1 from Lower: `lazyLower.put(1, count: 1)`. `validLowerSize` becomes 0.
 * 3. Total Valid = 2. targetLower = 2/2 = 1.
 * 4. Balance: `validLowerSize (0) < targetLower (1)`. 
 *    - Must move 1 item from Upper to Lower.
 *    - Pop Upper -> gets 2. Push 2 to Lower. `validLowerSize` = 1, `validUpperSize` = 1.
 * 5. getMedian(): Upper peek is 3. Return 3. (Correct! Median of [2, 3] is 3).
 * 
 * -- Pitfalls --
 * 1. Forgetting to clean `lowerHeap` BEFORE checking `number <= lowerHeap.peek()`. If the top of the heap 
 *    is actually a "ghost" element that should have been deleted, you might route a new deletion to the 
 *    wrong half of the data structure, permanently corrupting your logical size balances.
 * 2. Integer division trap. Using `Math.floor(sum / size)` where `sum` is an int. Since all numbers are positive, 
 *    Java's native `long / int` integer division safely truncates towards zero (which is exactly flooring).
 * 
 * -- Pattern recognition --
 * When you see "Sliding Window / Data Stream" + "Median", think **Two-Heaps**.
 * When you see "O(1) requirement" + "Removing arbitrary items from a heap", think **Lazy Deletion**.
 * 
 * -- Interview script --
 * "To do this efficiently, I'll combine a Queue for the sliding window, a running sum for the mean, 
 * a Max-Heap for the mode, and Two-Heaps for the median. Because Java's PriorityQueue takes O(N) to remove 
 * arbitrary elements, I'll implement Lazy Deletion using HashMaps to track pending deletions. Whenever we 
 * need to peek or balance the heaps, we first discard any stale elements sitting at the top. This guarantees 
 * O(log N) updates and O(1) amortized reads."
 * 
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q: What if the Median rules required taking the average of the two middle elements when size is even?
 * A: We would adjust `getMedian()`: if `totalValid % 2 == 0`, we `cleanHeap` on both, then return 
 *    `(lowerHeap.peek() + upperHeap.peek()) / 2.0`.
 * 
 * Q: What if the range of incoming numbers was very small (e.g., 1 to 100)?
 * A: We could ditch the heaps entirely. A frequency array of size 101 could answer `getMedian()` and `getMode()` 
 *    in O(100) -> O(1) time by iterating through it. It would use virtually no memory.
 * =========================================================================================
 */

import java.util.*;

public class ArrayStatisticsTrackerStudyNote {

    static class StatisticsTracker {
        
        // Base structure to handle FIFO order
        private final Queue<Integer> queue;
        
        // -- MEAN STATE --
        private long sum;
        
        // -- MODE STATE --
        // Tracks actual, current frequencies
        private final Map<Integer, Integer> freqMap;
        
        // Max-heap storing int[]{frequency, value}
        // Ordered by frequency DESC, then value ASC (for ties)
        private final PriorityQueue<int[]> modeHeap; 
        
        // -- MEDIAN STATE --
        // Lower half of the sorted elements (Max-Heap)
        private final PriorityQueue<Integer> lowerHeap;
        // Upper half of the sorted elements (Min-Heap)
        private final PriorityQueue<Integer> upperHeap;
        
        // Lazy deletion trackers to avoid O(N) heap removals
        private final Map<Integer, Integer> lazyLower;
        private final Map<Integer, Integer> lazyUpper;
        
        // True number of valid elements logically stored in each heap
        private int validLowerSize;
        private int validUpperSize;

        public StatisticsTracker() {
            queue = new LinkedList<>();
            sum = 0;
            
            freqMap = new HashMap<>();
            modeHeap = new PriorityQueue<>((a, b) -> {
                if (a[0] != b[0]) return Integer.compare(b[0], a[0]); // Frequency DESC
                return Integer.compare(a[1], b[1]);                   // Number ASC
            });
            
            lowerHeap = new PriorityQueue<>(Collections.reverseOrder()); // Max-Heap
            upperHeap = new PriorityQueue<>();                           // Min-Heap
            
            lazyLower = new HashMap<>();
            lazyUpper = new HashMap<>();
            
            validLowerSize = 0;
            validUpperSize = 0;
        }
        
        /**
         * Core utility: Aggressively cleans out any logically deleted ("ghost") elements 
         * sitting at the very top of the provided heap. 
         */
        private void cleanHeap(PriorityQueue<Integer> heap, Map<Integer, Integer> lazyTracker) {
            while (!heap.isEmpty() && lazyTracker.getOrDefault(heap.peek(), 0) > 0) {
                int staleValue = heap.poll();
                lazyTracker.put(staleValue, lazyTracker.get(staleValue) - 1);
            }
        }
        
        public void addNumber(int number) {
            // 1. Queue & Mean
            queue.offer(number);
            sum += number;
            
            // 2. Mode (Eager push, lazy cleanup)
            int newFreq = freqMap.getOrDefault(number, 0) + 1;
            freqMap.put(number, newFreq);
            modeHeap.offer(new int[]{newFreq, number});
            
            // 3. Median
            // Always clean before peeking to ensure we compare against a valid threshold.
            cleanHeap(lowerHeap, lazyLower);
            
            if (lowerHeap.isEmpty() || number <= lowerHeap.peek()) {
                lowerHeap.offer(number);
                validLowerSize++;
            } else {
                upperHeap.offer(number);
                validUpperSize++;
            }
            
            // Re-balance the median heaps
            balance();
        }
        
        public void removeFirstAddedNumber() {
            int number = queue.poll();
            sum -= number;
            
            // 1. Mode update
            int newFreq = freqMap.get(number) - 1;
            freqMap.put(number, newFreq);
            // Even if the frequency drops, we push the new state. 
            // The PQ will eventually discard the old, higher-frequency entry when it surfaces.
            if (newFreq > 0) {
                modeHeap.offer(new int[]{newFreq, number});
            }
            
            // 2. Median update
            cleanHeap(lowerHeap, lazyLower);
            
            // Determine which conceptual heap holds this number and mark it for lazy deletion.
            if (!lowerHeap.isEmpty() && number <= lowerHeap.peek()) {
                lazyLower.put(number, lazyLower.getOrDefault(number, 0) + 1);
                validLowerSize--;
            } else {
                lazyUpper.put(number, lazyUpper.getOrDefault(number, 0) + 1);
                validUpperSize--;
            }
            
            // Re-balance the median heaps to adjust for the logical size drop
            balance();
        }
        
        /**
         * Ensures that the total elements are properly distributed.
         * The problem states: "If size is even, the median is the LARGER of the two middle values."
         * This implies the upper half should hold the median.
         * e.g., N=4 -> Target Lower Size = 2, Target Upper Size = 2. Median is upperHeap.peek().
         * e.g., N=3 -> Target Lower Size = 1, Target Upper Size = 2. Median is upperHeap.peek().
         */
        private void balance() {
            int totalValid = validLowerSize + validUpperSize;
            int targetLower = totalValid / 2;
            
            while (validLowerSize > targetLower) {
                // Must clean before pulling a value to shift, to ensure it's a REAL value.
                cleanHeap(lowerHeap, lazyLower);
                int val = lowerHeap.poll();
                validLowerSize--;
                upperHeap.offer(val);
                validUpperSize++;
            }
            
            while (validLowerSize < targetLower) {
                cleanHeap(upperHeap, lazyUpper);
                int val = upperHeap.poll();
                validUpperSize--;
                lowerHeap.offer(val);
                validLowerSize++;
            }
        }
        
        public int getMean() {
            // Integer division naturally floors for positive numbers
            return (int) (sum / queue.size());
        }
        
        public int getMedian() {
            // The median is always strictly sitting at the top of the upper heap.
            // Just clean the top of any ghosts before observing it.
            cleanHeap(upperHeap, lazyUpper);
            return upperHeap.peek();
        }
        
        public int getMode() {
            // Check the top of the mode heap. If the frequency recorded in the heap 
            // doesn't match the true current frequency, it's a stale record -> Pop it.
            while (!modeHeap.isEmpty()) {
                int[] top = modeHeap.peek();
                int recordedFreq = top[0];
                int num = top[1];
                
                if (freqMap.getOrDefault(num, 0) == recordedFreq) {
                    return num;
                }
                modeHeap.poll(); // Stale entry, discard
            }
            return -1; // Should never be reached if called on a non-empty tracker
        }
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS
    // ----------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("Initializing Tracker...");
        StatisticsTracker tracker = new StatisticsTracker();
        
        tracker.addNumber(4); // [4]
        tracker.addNumber(2); // [4, 2]
        tracker.addNumber(1); // [4, 2, 1]
        
        System.out.println("After adding 4, 2, 1:");
        System.out.println("Mean: " + tracker.getMean() + " (Expected: 2 -> floor(7/3))");
        // Median of [1, 2, 4] is 2
        System.out.println("Median: " + tracker.getMedian() + " (Expected: 2)"); 
        // Smallest mode amongst all tied at frequency 1 is 1
        System.out.println("Mode: " + tracker.getMode() + " (Expected: 1)");
        
        System.out.println("\nRemoving first element (4)...");
        tracker.removeFirstAddedNumber(); // State: [2, 1]
        
        System.out.println("Mean: " + tracker.getMean() + " (Expected: 1 -> floor(3/2))");
        // Median of [1, 2] is 2 (Problem rule: take the larger middle value for even sizes)
        System.out.println("Median: " + tracker.getMedian() + " (Expected: 2)");
        // Mode tie between 1 and 2. Smallest is 1.
        System.out.println("Mode: " + tracker.getMode() + " (Expected: 1)");
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 * 
 * - Core pattern: Multi-Heaps with Lazy Deletion.
 * - Key observation: In Java, modifying elements strictly in the middle of a PriorityQueue is O(N). 
 *   You bypass this entirely by keeping a separate truth-table (HashMaps) of what's been deleted, 
 *   and only removing elements when they naturally surface to the top of the heap (`.peek()`).
 * - Worth memorizing: The exact balancing math: `targetLowerSize = total / 2`. This guarantees the 
 *   Upper heap always holds the true median element at its `.peek()` for BOTH even and odd array sizes 
 *   when the rule says "take the larger of the two middle elements".
 * - Most common trap: Calling `remove(obj)` natively, causing the solution to easily TLE on LeetCode.
 * - One-line mental trigger: "O(1) deletion from middle of a Heap? -> Use a HashMap of ghosts and delay the pop."
 * =========================================================================================
 */




































import java.util.*;

class StatisticsTracker {

    /*
     * ============================================================
     * 1. FIFO QUEUE
     * ============================================================
     *
     * Stores elements in insertion order.
     *
     * addNumber()              -> offerLast()
     * removeFirstAddedNumber() -> pollFirst()
     *
     * Both operations: O(1).
     */
    private final Deque<Integer> queue = new ArrayDeque<>();


    /*
     * ============================================================
     * 2. RUNNING SUM
     * ============================================================
     *
     * Maintaining the sum avoids traversing all elements
     * whenever getMean() is called.
     *
     * long is essential:
     *
     * Maximum possible value = 10^9
     * Maximum elements       = 10^5
     *
     * Maximum sum            = 10^14
     *
     * This exceeds Integer.MAX_VALUE.
     */
    private long sum = 0;
    private int count = 0;


    /*
     * ============================================================
     * 3. MEDIAN: TWO HEAPS
     * ============================================================
     *
     * lower = Max heap containing the smaller half.
     * upper = Min heap containing the larger half.
     *
     * We intentionally keep the extra element in upper.
     *
     * Invariants:
     *
     * 1. Every valid lower element <= every valid upper element.
     *
     * 2. upperSize == lowerSize
     *    OR
     *    upperSize == lowerSize + 1.
     *
     * Therefore:
     *
     * getMedian() = upper.peek()
     *
     * This also handles even-sized collections correctly:
     * we return the larger middle element.
     */

    private final PriorityQueue<Integer> lower =
        new PriorityQueue<>(Comparator.reverseOrder());

    private final PriorityQueue<Integer> upper =
        new PriorityQueue<>();


    /*
     * Logical sizes exclude elements marked for deletion.
     *
     * PriorityQueue.size() is not reliable for balancing
     * because stale elements can remain inside the heaps.
     */
    private int lowerSize = 0;
    private int upperSize = 0;


    /*
     * Separate lazy-deletion maps are necessary.
     *
     * A value can exist in BOTH heaps.
     *
     * Therefore, a deletion marker for one heap must never
     * accidentally remove a valid occurrence from the other.
     */
    private final Map<Integer, Integer> delayedLower =
        new HashMap<>();

    private final Map<Integer, Integer> delayedUpper =
        new HashMap<>();


    /*
     * ============================================================
     * 4. MODE: FREQUENCY MAP + ORDERED SET
     * ============================================================
     *
     * frequency:
     * number -> occurrence count
     *
     * modeOrder:
     * Entries ordered by:
     *
     * 1. Frequency descending.
     * 2. Number ascending.
     *
     * Consequently, modeOrder.first() is always the mode.
     */

    private final Map<Integer, Integer> frequency =
        new HashMap<>();


    /*
     * Java 24 supports records.
     *
     * A record gives us an immutable value object with
     * automatically generated equals() and hashCode().
     */
    private record ModeEntry(int number, int frequency) {}


    private final TreeSet<ModeEntry> modeOrder =
        new TreeSet<>(
            (a, b) -> {
                // Higher frequency must appear first.
                int byFrequency =
                    Integer.compare(b.frequency(), a.frequency());

                if (byFrequency != 0) {
                    return byFrequency;
                }

                // For equal frequencies, smaller number first.
                return Integer.compare(a.number(), b.number());
            }
        );


    /*
     * ============================================================
     * CONSTRUCTOR
     * ============================================================
     *
     * All structures are initially empty.
     *
     * Time: O(1).
     */
    public StatisticsTracker() {
    }


    /*
     * ============================================================
     * ADD NUMBER
     * ============================================================
     *
     * Responsibilities:
     *
     * 1. Add to FIFO queue.
     * 2. Update running sum and count.
     * 3. Insert into median heaps.
     * 4. Rebalance median heaps.
     * 5. Update frequency and mode ordering.
     *
     * Time: O(log n).
     */
    public void addNumber(int number) {

        // 1. Maintain insertion order.
        queue.offerLast(number);


        // 2. Maintain mean.
        sum += number;
        count++;


        // 3. Remove stale heap elements before inspecting tops.
        pruneLower();
        pruneUpper();


        /*
         * 4. Insert into the correct heap.
         *
         * If upper is empty, or number >= upper's minimum,
         * the number belongs in the upper half.
         *
         * Otherwise, it belongs in the lower half.
         */
        if (upper.isEmpty() || number >= upper.peek()) {

            upper.offer(number);
            upperSize++;

        } else {

            lower.offer(number);
            lowerSize++;
        }


        // 5. Restore heap-size invariants.
        rebalance();


        // 6. Update mode information.
        updateFrequency(number, 1);
    }


    /*
     * ============================================================
     * REMOVE FIRST ADDED NUMBER
     * ============================================================
     *
     * FIFO removal is guaranteed by the problem.
     *
     * We use lazy deletion for the median heaps.
     *
     * Time: O(log n) amortized.
     */
    public void removeFirstAddedNumber() {

        // 1. Get the oldest inserted number.
        int number = queue.pollFirst();


        // 2. Update mean.
        sum -= number;
        count--;


        // 3. Remove the number from the frequency structures.
        updateFrequency(number, -1);


        /*
         * 4. Clean stale elements before inspecting heap tops.
         */
        pruneLower();
        pruneUpper();


        /*
         * 5. Determine which logical heap contains the number.
         *
         * Every lower element <= every upper element.
         *
         * If number >= upper.peek(), we logically remove
         * one occurrence from upper.
         *
         * Otherwise, it belongs to lower.
         *
         * When equal values exist across both heaps, treating
         * the occurrence as belonging to upper is valid:
         * identical values are interchangeable in a multiset,
         * and upper contains a matching occurrence at its top.
         */

        if (number >= upper.peek()) {

            // Mark one occurrence in upper for lazy deletion.
            delayedUpper.merge(number, 1, Integer::sum);

            upperSize--;

        } else {

            // Mark one occurrence in lower for lazy deletion.
            delayedLower.merge(number, 1, Integer::sum);

            lowerSize--;
        }


        // 6. Physically remove stale heap tops if necessary.
        pruneLower();
        pruneUpper();


        // 7. Restore the median heap invariants.
        rebalance();
    }


    /*
     * ============================================================
     * MEAN
     * ============================================================
     *
     * Mean = sum / count.
     *
     * All numbers are positive, so Java integer division
     * produces the required floored mean.
     *
     * Time: O(1).
     * Extra space: O(1).
     */
    public int getMean() {
        return (int) (sum / count);
    }


    /*
     * ============================================================
     * MEDIAN
     * ============================================================
     *
     * upper contains the larger half.
     *
     * Its minimum is exactly the element at sorted index n / 2.
     *
     * Pruning is performed defensively before reading the top.
     *
     * Time: O(1) amortized.
     *
     * Note:
     * Each stale element is physically removed at most once.
     * Thus, the total cleanup work across operations is linear
     * in the number of heap insertions.
     */
    public int getMedian() {

        pruneUpper();

        return upper.peek();
    }


    /*
     * ============================================================
     * MODE
     * ============================================================
     *
     * modeOrder is sorted by:
     *
     * frequency descending,
     * number ascending.
     *
     * The first entry is therefore the correct mode.
     *
     * Time: O(1).
     */
    public int getMode() {
        return modeOrder.first().number();
    }


    /*
     * ============================================================
     * UPDATE FREQUENCY
     * ============================================================
     *
     * delta = +1 for insertion.
     * delta = -1 for removal.
     *
     * Important:
     * Remove the old TreeSet entry BEFORE modifying its frequency.
     *
     * TreeSet ordering depends on frequency.
     * Modifying a value without removing its old entry
     * would corrupt the ordering.
     *
     * Time: O(log d).
     */
    private void updateFrequency(int number, int delta) {

        int oldFrequency = frequency.getOrDefault(number, 0);


        // Remove the old frequency-number pair if it exists.
        if (oldFrequency > 0) {
            modeOrder.remove(
                new ModeEntry(number, oldFrequency)
            );
        }


        // Calculate updated frequency.
        int newFrequency = oldFrequency + delta;


        if (newFrequency == 0) {

            // Number no longer exists in the collection.
            frequency.remove(number);

        } else {

            // Save the updated frequency.
            frequency.put(number, newFrequency);

            // Insert its updated ordering information.
            modeOrder.add(
                new ModeEntry(number, newFrequency)
            );
        }
    }


    /*
     * ============================================================
     * PRUNE LOWER HEAP
     * ============================================================
     *
     * Remove elements that were logically deleted earlier.
     *
     * We only need to inspect the top.
     * Elements deeper inside the heap can remain until they
     * eventually reach the top.
     *
     * Each stale element is physically removed exactly once.
     */
    private void pruneLower() {

        while (!lower.isEmpty()) {

            int number = lower.peek();

            int pending = delayedLower.getOrDefault(number, 0);

            if (pending == 0) {
                break;
            }

            // Physically remove stale element.
            lower.poll();


            if (pending == 1) {
                delayedLower.remove(number);
            } else {
                delayedLower.put(number, pending - 1);
            }
        }
    }


    /*
     * ============================================================
     * PRUNE UPPER HEAP
     * ============================================================
     *
     * Same lazy-deletion process as lower.
     * Uses a separate deletion map.
     */
    private void pruneUpper() {

        while (!upper.isEmpty()) {

            int number = upper.peek();

            int pending = delayedUpper.getOrDefault(number, 0);

            if (pending == 0) {
                break;
            }

            upper.poll();


            if (pending == 1) {
                delayedUpper.remove(number);
            } else {
                delayedUpper.put(number, pending - 1);
            }
        }
    }


    /*
     * ============================================================
     * REBALANCE HEAPS
     * ============================================================
     *
     * Required invariant:
     *
     * upperSize == lowerSize
     * OR
     * upperSize == lowerSize + 1.
     *
     * We always maintain the extra element in upper.
     *
     * Each move takes O(log n).
     */
    private void rebalance() {

        pruneLower();
        pruneUpper();


        // Case 1: upper has too many elements.
        while (upperSize > lowerSize + 1) {

            int number = upper.poll();

            upperSize--;

            lower.offer(number);

            lowerSize++;


            pruneUpper();
        }


        // Case 2: lower has more elements.
        while (upperSize < lowerSize) {

            int number = lower.poll();

            lowerSize--;

            upper.offer(number);

            upperSize++;


            pruneLower();
        }


        // Remove any newly exposed stale elements.
        pruneLower();
        pruneUpper();
    }
}

