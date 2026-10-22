/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * The median is the middle value in an ordered integer list. If the size of 
 * the list is even, there is no middle value, and the median is the mean of 
 * the two middle values.
 * 
 * Implement the MedianFinder class:
 * - MedianFinder() initializes the object.
 * - void addNum(int num) adds the integer num from the data stream.
 * - double findMedian() returns the median of all elements so far.
 * 
 * Constraints:
 * - -10^5 <= num <= 10^5
 * - At most 5 * 10^4 calls will be made to addNum and findMedian.
 * 
 * Follow up constraints to address:
 * - Numbers in range [0, 100].
 * - 99% of numbers in range [0, 100].
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Are numbers ever removed from the stream? 
 *    (If yes, we'd need to support lazy deletion in our heaps or use a Balanced BST).
 * 2. What is the expected ratio of `addNum` to `findMedian` calls? 
 *    (If extremely read-heavy, maintaining strict sorted order on insert is better. If write-heavy, lazy sorting is better. The prompt implies a balanced mix).
 * 3. Can we get arithmetic overflow when calculating the mean of two integers?
 *    (Yes, adding two large positive or negative integers can overflow a 32-bit `int`. We must cast to `double` before adding).
 * 4. How should we handle the first `findMedian` call if no numbers exist?
 *    (The constraints guarantee at least one element before `findMedian` is called, saving us from checking empty states).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: We need to maintain a continuous data stream and efficiently 
 *   retrieve the middle element(s) at any given time. The binding constraint is that 
 *   sorting the entire stream on every query is computationally prohibitive.
 * 
 * --- Approach 1: Append and Sort (Brute Force) ---
 * 1. Intuition: Just keep all numbers in a list and sort them when asked.
 * 2. Try: Use an `ArrayList`. `addNum` appends to the list. `findMedian` sorts the list and finds the middle.
 * 3. Why it works: It perfectly satisfies the definition of a median.
 * 4. Why it's too costly: `Collections.sort()` takes O(N log N). Calling this repeatedly across 50,000 queries leads to O(N^2 log N) total time, which will Time Limit Exceed (TLE).
 * 5. Bottleneck: Re-sorting already sorted data.
 * 
 * Time Complexity: addNum: O(1). findMedian: O(N log N) — sorting the whole list.
 * Space Complexity: O(N) — storing N elements.
 * 
 * --- Approach 2: Insertion Sort / Binary Search ---
 * 1. Intuition: Keep the list strictly sorted at all times.
 * 2. Try: On `addNum`, use Binary Search to find the correct insertion index, then insert it. `findMedian` instantly accesses the middle index.
 * 3. Why it works: Binary search finds the spot in O(log N). Lookups are O(1).
 * 4. Why it's suboptimal: Inserting an element into the middle of an `ArrayList` requires shifting all subsequent elements to the right, which takes O(N) time.
 * 5. Bottleneck: Array shifting overhead. We don't actually care about the exact sorted order of the ends, ONLY the middle.
 * 
 * Time Complexity: addNum: O(N) — array shifting. findMedian: O(1).
 * Space Complexity: O(N).
 * 
 * --- Approach 3: Two Heaps (Optimal) ---
 * 1. Intuition: The median divides a dataset into a "smaller half" and a "larger half". If we only track the largest of the small half and the smallest of the large half, we have our median!
 * 2. Try: Use a Max-Heap for the smaller half, and a Min-Heap for the larger half.
 * 3. Why it works: Heaps efficiently surface extreme values. We can keep them balanced such that their sizes differ by at most 1.
 * 4. Which to write in an interview? Approach 3. It is the gold standard for this problem, demonstrating mastery of priority queues and dynamic balancing.
 * 
 * Time Complexity: addNum: O(log N) — inserting and balancing heaps. findMedian: O(1) — peeking at the top of the heaps.
 * Space Complexity: O(N) — storing N elements distributed across two heaps.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Out of order insertions: Inserting 10, then 1, then 5. The routing logic must ensure 10 goes to the large half and 1 to the small half, regardless of insertion order.
 * - Negative numbers: Java's PriorityQueue handles negative numbers flawlessly.
 * - Overflow on Median Calculation: `(Integer.MAX_VALUE + Integer.MAX_VALUE - 2) / 2`. To prevent integer overflow, we must cast to double: `(low.peek() + high.peek()) / 2.0`.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "Data Routing." To ensure the Max-Heap strictly contains smaller elements than the Min-Heap, you cannot just compare sizes. You must push a new number into the Max-Heap, let the Max-Heap surface its largest element, and push THAT element into the Min-Heap.
 * 
 * ASCII DRY RUN (Two Heaps)
 * Adding: [4, 1, 5, 2]
 * 
 * 1. add(4)
 *    -> Push to Low (Max-Heap): Low = [4]
 *    -> Pop Low, Push to High (Min-Heap): High = [4]
 *    -> Balance (High > Low): Pop High, Push to Low: Low = [4], High = []
 *    Median: 4.0
 * 
 * 2. add(1)
 *    -> Push to Low: Low = [4, 1]
 *    -> Pop Low (4), Push to High: Low = [1], High = [4]
 *    -> Balance: Low and High sizes equal.
 *    Median: (1 + 4) / 2.0 = 2.5
 * 
 * 3. add(5)
 *    -> Push to Low: Low = [5, 1]
 *    -> Pop Low (5), Push to High: Low = [1], High = [4, 5]
 *    -> Balance (High > Low): Pop High (4), Push to Low: Low = [4, 1], High = [5]
 *    Median: 4.0
 * 
 * Pitfalls:
 * - Manual comparison routing: `if (num < low.peek()) low.add(num) else high.add(num)`. This requires tedious edge-case handling for empty heaps. The unconditional "push to A, pop A to B, balance" trick is foolproof.
 * 
 * Pattern Recognition: "Dynamic Median / Percentile" -> Two Heaps (Min & Max).
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: If all integer numbers from the stream are in the range [0, 100], how would you optimize?
 * A1: We abandon Heaps and use Counting Sort. Maintain an integer array `int[] counts = new int[101]` and a `total_count` variable. `addNum` increments `counts[num]` and `total_count` in O(1) time. `findMedian` iterates through the array accumulating the count until we hit `total_count / 2`. Time complexity becomes O(1) for both operations (since max iterations = 101). Space is O(1).
 * 
 * Q2: If 99% of all integer numbers from the stream are in the range [0, 100], how would you optimize?
 * A2: Keep the 101-bucket array for the 99%. Create an integer counter `lessThanZero` for the small outliers, and a standard list/heap for the `greaterThan100` outliers. When finding the median, if the middle index falls inside the `[0, 100]` buckets, we just do the same O(1) scan. We rarely have to access the outlier lists, vastly reducing memory overhead compared to full heaps.
 */

import java.util.Collections;
import java.util.PriorityQueue;

public class MedianFinderStudy {

    static class MedianFinder {
        // Max-Heap: Stores the smaller half of the numbers.
        // We use Collections.reverseOrder() to make it surface the LARGEST number.
        private PriorityQueue<Integer> low;
        
        // Min-Heap: Stores the larger half of the numbers.
        // Defaults to surfacing the SMALLEST number.
        private PriorityQueue<Integer> high;

        /** 
         * Initializes the MedianFinder object.
         */
        public MedianFinder() {
            low = new PriorityQueue<>(Collections.reverseOrder());
            high = new PriorityQueue<>();
        }

        /**
         * Adds the integer num from the data stream to the data structure.
         * Time: O(log N)
         */
        public void addNum(int num) {
            // STEP 1: Add to the lower half.
            low.offer(num);
            
            // STEP 2: The lower half might now contain an element that strictly 
            // belongs in the upper half. We fix this by popping the maximum 
            // element from the lower half and pushing it to the upper half.
            high.offer(low.poll());
            
            // STEP 3: Balancing sizes. 
            // We maintain the invariant that 'low' is either equal in size to 'high',
            // or it contains exactly one more element than 'high'.
            if (low.size() < high.size()) {
                low.offer(high.poll());
            }
        }

        /**
         * Returns the median of all elements so far.
         * Time: O(1)
         */
        public double findMedian() {
            // If total number of elements is odd, the lower half has the extra element.
            if (low.size() > high.size()) {
                return low.peek();
            }
            
            // If total is even, median is the average of the two middle elements.
            // Note: Division by 2.0 ensures we return a double and prevents integer division truncation.
            return (low.peek() + high.peek()) / 2.0;
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        MedianFinder medianFinder = new MedianFinder();

        System.out.println("Adding 1...");
        medianFinder.addNum(1);
        System.out.println("Median: " + medianFinder.findMedian()); // Expected: 1.0

        System.out.println("Adding 2...");
        medianFinder.addNum(2);
        System.out.println("Median: " + medianFinder.findMedian()); // Expected: 1.5

        System.out.println("Adding 3...");
        medianFinder.addNum(3);
        System.out.println("Median: " + medianFinder.findMedian()); // Expected: 2.0
        
        System.out.println("Adding -5 (Negative Edge Case)...");
        medianFinder.addNum(-5);
        // Array logically: [-5, 1, 2, 3]. Median is (1+2)/2 = 1.5
        System.out.println("Median: " + medianFinder.findMedian()); // Expected: 1.5
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: Dual Priority Queues (Max-Heap for smalls, Min-Heap for larges).
 * Key Observation: The median only relies on the middle elements. By splitting the 
 * data into two heaps, we get instant O(1) access to the middle without the O(N) 
 * insertion cost of a sorted array.
 * What to Memorize: The 3-step insert: `low.add(n); high.add(low.poll()); if(low.size < high.size) low.add(high.poll());`
 * Most Common Trap: Manual conditional routing (`if num < low.peek()`). It requires messy 
 * edge-case handling for empty heaps. The blind "pass-through" routing is elegant and safe.
 * Mental Trigger: "Streaming median" -> "Two Heaps".
 * ============================================================================
 */
















































import java.util.Comparator;
import java.util.PriorityQueue;

class MedianFinder {

    /*
     * Mental model:
     *
     * lowerHalf = the smaller half of the complete dataset.
     *             Its root is the largest value in this half.
     *
     * upperHalf = the larger half of the complete dataset.
     *             Its root is the smallest value in this half.
     *
     * We do NOT need the values inside either half to be fully sorted.
     * We only need their boundary values and balanced sizes.
     */

    private final PriorityQueue<Integer> lowerHalf =
            new PriorityQueue<>(Comparator.reverseOrder());

    private final PriorityQueue<Integer> upperHalf =
            new PriorityQueue<>();

    public void addNum(int num) {

        // INVARIANT 1 — partition order:
        // Every value in lowerHalf must be <= every value in upperHalf.
        //
        // Since lowerHalf.peek() is its maximum, comparing num with that
        // boundary tells us whether num can be added to the lower half.
        //
        // If lowerHalf is empty, put the first value there.
        if (lowerHalf.isEmpty() || num <= lowerHalf.peek()) {
            lowerHalf.offer(num);
        } else {
            upperHalf.offer(num);
        }

        // INVARIANT 2 — size balance:
        // lowerHalf.size() must equal upperHalf.size(), OR exceed it by 1.
        //
        // If lowerHalf has more than one extra value, move its maximum
        // to upperHalf. This restores balance without breaking partition
        // order: the moved value was the largest value in lowerHalf.
        if (lowerHalf.size() > upperHalf.size() + 1) {
            upperHalf.offer(lowerHalf.poll());
        }

        // If upperHalf has more values than lowerHalf, move its minimum
        // to lowerHalf. This preserves partition order because the moved
        // value is the smallest value in the upper half.
        if (upperHalf.size() > lowerHalf.size()) {
            lowerHalf.offer(upperHalf.poll());
        }

        // After rebalancing:
        // lowerHalf has either the same number of values as upperHalf,
        // or exactly one more.
        //
        // Both invariants hold, so the roots identify the middle values.
    }

    public double findMedian() {

        // The problem guarantees that at least one number exists.
        //
        // Case 1: Odd total count.
        // lowerHalf contains one extra value. Its maximum is the middle
        // element of the complete dataset.
        if (lowerHalf.size() > upperHalf.size()) {
            return lowerHalf.peek();
        }

        // Case 2: Even total count.
        // Both halves have the same size. The median is the average of
        // the largest value in the lower half and the smallest in upper.
        //
        // Convert to long before addition so the sum cannot overflow int.
        return ((long) lowerHalf.peek() + upperHalf.peek()) / 2.0;
    }

    /*
     * COMPLEXITY ANALYSIS
     *
     * Let n be the number of inserted values.
     *
     * addNum(num):
     *   - One heap insertion costs O(log n).
     *   - At most one rebalance move is needed for the chosen insertion
     *     strategy; a move consists of a heap removal and insertion,
     *     each O(log n).
     *   - Total: O(log n) worst-case time per insertion.
     *
     * findMedian():
     *   - Reads one or two heap roots.
     *   - peek() is O(1).
     *   - Total: O(1) time.
     *
     * Space:
     *   - Every inserted number is stored in exactly one heap.
     *   - Total: O(n) space.
     *
     * Overall: O(log n) per insertion, O(1) per median query, O(n) space.
     * This is the standard optimal solution for the general constraints.
     */
}


