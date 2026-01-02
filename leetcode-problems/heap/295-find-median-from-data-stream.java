import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * PROBLEM ANALYSIS & INTERVIEW APPROACH
 * =====================================
 * 
 * This is a STREAMING MEDIAN problem testing:
 * 1. Understanding of heap data structures
 * 2. Ability to maintain balanced data structures
 * 3. Creative use of TWO heaps to find median efficiently
 * 4. Trade-offs between time and space complexity
 * 
 * KEY INSIGHTS:
 * -------------
 * 1. Median is the "middle" element(s) in sorted order
 *    - For odd length: the exact middle element
 *    - For even length: average of two middle elements
 * 
 * 2. We need FAST access to middle element(s)
 *    - Sorting every time: O(N log N) - too slow
 *    - Binary search insertion: O(N) - still slow for insertions
 *    - Need O(log N) add and O(1) findMedian
 * 
 * 3. THE BRILLIANT INSIGHT: Use TWO HEAPS
 *    - Max heap for SMALLER half of numbers (left side)
 *    - Min heap for LARGER half of numbers (right side)
 *    - Median is either: max of left heap, min of right heap, or their average
 * 
 * VISUALIZATION:
 * --------------
 * 
 * Numbers: [1, 2, 3, 4, 5, 6, 7]
 * Median: 4
 * 
 * Split into two halves:
 * Left half (≤ median): [1, 2, 3]     <- Store in MAX heap
 * Right half (≥ median): [5, 6, 7]    <- Store in MIN heap
 * Median: 4 (either in left or right, or between them)
 * 
 * MAX HEAP (left):          MIN HEAP (right):
 *       3                         5
 *      / \                       / \
 *     1   2                     6   7
 * 
 * Root of max heap = 3 (largest of smaller half)
 * Root of min heap = 5 (smallest of larger half)
 * 
 * If sizes equal: median = (3 + 5) / 2 = 4
 * If left has one more: median = 3
 * If right has one more: median = 5
 * 
 * INVARIANTS TO MAINTAIN:
 * -----------------------
 * 1. All elements in maxHeap ≤ all elements in minHeap
 * 2. Size difference ≤ 1: |maxHeap.size() - minHeap.size()| ≤ 1
 * 3. If sizes equal: median = (max of left + min of right) / 2
 * 4. If unequal: median = root of larger heap
 * 
 * INTERVIEW STRATEGY:
 * -------------------
 * 1. Start with naive approach
 *    "I could sort the array every time, but that's O(N log N) per findMedian"
 * 
 * 2. Discuss better approaches
 *    "Maintaining a sorted list would be O(N) per insertion due to shifting"
 * 
 * 3. Introduce two-heap solution
 *    "Here's a clever insight: I can use two heaps to partition the data"
 *    [Draw diagram on whiteboard]
 * 
 * 4. Walk through the algorithm
 *    - Show how to add elements
 *    - Show how to rebalance
 *    - Show how to find median
 * 
 * 5. Analyze complexity
 *    - addNum: O(log N)
 *    - findMedian: O(1)
 *    - Space: O(N)
 * 
 * 6. Discuss follow-up optimizations
 *    - Counting array for limited range
 *    - Hybrid approach for 99% case
 */

class MedianFinder {
    
    /**
     * TWO HEAP APPROACH - THE ELEGANT SOLUTION
     * =========================================
     * 
     * maxHeap (left side): Stores smaller half of numbers
     * - Java PriorityQueue with reverseOrder comparator
     * - Root = largest element in smaller half
     * 
     * minHeap (right side): Stores larger half of numbers  
     * - Java PriorityQueue with natural ordering
     * - Root = smallest element in larger half
     * 
     * Why this works:
     * - maxHeap.peek() = largest of smaller numbers (just below median)
     * - minHeap.peek() = smallest of larger numbers (just above median)
     * - Median is at the "boundary" between these two heaps
     * 
     * Balancing strategy:
     * - Keep heaps equal size OR maxHeap has 1 extra element
     * - This makes findMedian O(1) - just look at heap roots
     */
    
     PriorityQueue<Integer> maxHeap; // Stores smaller half (max heap)
     PriorityQueue<Integer> minHeap; // Stores larger half (min heap)
    
    /**
     * Constructor: Initialize both heaps
     * 
     * TIME: O(1)
     * SPACE: O(1) - just initialization
     */
    public MedianFinder() {
        // Max heap - stores smaller half of numbers
        // Need to reverse natural ordering to get max heap behavior
        maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        
        // Min heap - stores larger half of numbers
        // Default PriorityQueue is min heap
        minHeap = new PriorityQueue<>();
    }
    
    /**
     * Add a number to the data structure
     * 
     * ALGORITHM:
     * ----------
     * 1. Decide which heap to add to
     *    - If maxHeap empty OR num ≤ maxHeap.peek(): add to maxHeap
     *    - Otherwise: add to minHeap
     * 
     * 2. Rebalance if needed
     *    - If maxHeap has 2+ more elements than minHeap: move one to minHeap
     *    - If minHeap has more elements than maxHeap: move one to maxHeap
     * 
     * WHY THIS WORKS:
     * ---------------
     * Step 1 maintains: maxHeap elements ≤ minHeap elements
     * Step 2 maintains: size difference ≤ 1
     * 
     * Example walkthrough:
     * Add 1: maxHeap=[1], minHeap=[]
     * Add 2: Compare with 1, goes to minHeap. maxHeap=[1], minHeap=[2]
     * Add 3: Compare with 1, goes to minHeap. maxHeap=[1], minHeap=[2,3]
     *        Rebalance! Move 2 to maxHeap. maxHeap=[1,2], minHeap=[3]
     * 
     * TIME COMPLEXITY: O(log N)
     * - offer(): O(log N) for heap insertion
     * - poll(): O(log N) for heap removal
     * - At most 3 heap operations per add
     * 
     * SPACE COMPLEXITY: O(1)
     * - No additional space beyond the heaps
     * 
     * @param num The number to add
     */
    public void addNum(int num) {
        // STEP 1: Add to appropriate heap
        // Decision: does this number belong in smaller half or larger half?
        
        if (maxHeap.isEmpty() || num <= maxHeap.peek()) {
            // Number is small enough to go in left heap (smaller half)
            maxHeap.offer(num);
        } else {
            // Number is large enough to go in right heap (larger half)
            minHeap.offer(num);
        }
        
        // STEP 2: Rebalance heaps
        // Maintain invariant: |maxHeap.size() - minHeap.size()| ≤ 1
        // And prefer maxHeap to be larger (or equal)
        
        // Case 1: maxHeap has too many elements (2+ more than minHeap)
        if (maxHeap.size() > minHeap.size() + 1) {
            minHeap.offer(maxHeap.poll());
        }
        // Case 2: minHeap has more elements than maxHeap
        // We want maxHeap to be larger or equal
        else if (minHeap.size() > maxHeap.size()) {
            maxHeap.offer(minHeap.poll());
        }
        
        // After rebalancing:
        // - If odd total: maxHeap has 1 more element
        // - If even total: heaps have equal size
    }
    
    /**
     * Find the median of all elements added so far
     * 
     * ALGORITHM:
     * ----------
     * 1. If heaps equal size: median = (maxHeap.peek() + minHeap.peek()) / 2.0
     * 2. If maxHeap larger: median = maxHeap.peek()
     * 
     * WHY THIS WORKS:
     * ---------------
     * The roots of the heaps are the elements closest to the median:
     * - maxHeap.peek() = largest element in smaller half
     * - minHeap.peek() = smallest element in larger half
     * 
     * Example 1: [1, 2, 3, 4, 5]
     * maxHeap=[3, 2, 1], minHeap=[4, 5]
     * maxHeap.size() > minHeap.size()
     * Median = maxHeap.peek() = 3 ✓
     * 
     * Example 2: [1, 2, 3, 4]
     * maxHeap=[2, 1], minHeap=[3, 4]
     * Equal sizes
     * Median = (2 + 3) / 2 = 2.5 ✓
     * 
     * TIME COMPLEXITY: O(1)
     * - peek() is O(1)
     * - Simple arithmetic
     * 
     * SPACE COMPLEXITY: O(1)
     * - No additional space
     * 
     * @return The median value
     */
    public double findMedian() {
        // Case 1: Odd number of elements
        // maxHeap has one extra element, its root is the median
        if (maxHeap.size() > minHeap.size()) {
            return maxHeap.peek();
        }
        
        // Case 2: Even number of elements
        // Median is average of the two middle elements
        // These are the roots of both heaps
        return (maxHeap.peek() + minHeap.peek()) / 2.0;
    }
    
    /**
     * COMPLEXITY SUMMARY
     * ==================
     * addNum():     O(log N) time, O(1) space
     * findMedian(): O(1) time, O(1) space
     * Total space:  O(N) for storing all elements
     * 
     * This is optimal for streaming median finding!
     */
}

/**
 * ALTERNATIVE APPROACHES
 * ======================
 * 
 * 1. SIMPLE SORTED LIST
 *    -------------------
 *    class MedianFinder {
 *        List<Integer> list = new ArrayList<>();
 *        
 *        public void addNum(int num) {
 *            list.add(num);
 *            Collections.sort(list);
 *        }
 *        
 *        public double findMedian() {
 *            int n = list.size();
 *            if (n % 2 == 1) return list.get(n/2);
 *            return (list.get(n/2-1) + list.get(n/2)) / 2.0;
 *        }
 *    }
 *    
 *    Time: O(N log N) per addNum - TOO SLOW
 *    Space: O(N)
 * 
 * 2. BINARY SEARCH INSERTION
 *    ------------------------
 *    - Maintain sorted list
 *    - Use binary search to find insertion point: O(log N)
 *    - Insert element: O(N) due to shifting
 *    
 *    Time: O(N) per addNum, O(1) findMedian
 *    Space: O(N)
 *    Better but still not optimal
 * 
 * 3. BALANCED BST (TreeMap)
 *    -----------------------
 *    - Use TreeMap with counts
 *    - Track median position
 *    
 *    Time: O(log N) per addNum, O(1) findMedian
 *    Space: O(N)
 *    Comparable to two-heap but more complex
 * 
 * 4. SINGLE HEAP (WRONG)
 *    --------------------
 *    - Can't efficiently get median from one heap
 *    - Would need to poll/re-insert: O(N log N)
 * 
 * WINNER: Two Heaps
 * - O(log N) addNum, O(1) findMedian
 * - Elegant and efficient
 * - Easy to implement and explain
 */

/**
 * FOLLOW-UP OPTIMIZATIONS
 * ========================
 * 
 * FOLLOW-UP 1: All numbers in range [0, 100]
 * -------------------------------------------
 * Use COUNTING SORT / BUCKET approach
 * 
 * class MedianFinder {
 *     int[] count = new int[101];  // count[i] = frequency of number i
 *     int total = 0;                // total count of numbers
 *     
 *     public void addNum(int num) {
 *         count[num]++;
 *         total++;
 *     }
 *     
 *     public double findMedian() {
 *         int mid1 = (total + 1) / 2;     // Position of first median element
 *         int mid2 = (total + 2) / 2;     // Position of second median element
 *         
 *         int cumulative = 0;
 *         int val1 = -1, val2 = -1;
 *         
 *         for (int i = 0; i <= 100; i++) {
 *             cumulative += count[i];
 *             if (val1 == -1 && cumulative >= mid1) val1 = i;
 *             if (val2 == -1 && cumulative >= mid2) val2 = i;
 *             if (val1 != -1 && val2 != -1) break;
 *         }
 *         
 *         return (val1 + val2) / 2.0;
 *     }
 * }
 * 
 * Time: addNum O(1), findMedian O(100) = O(1) constant time
 * Space: O(101) = O(1) constant space
 * 
 * This is BETTER than two heaps when range is limited!
 * 
 * 
 * FOLLOW-UP 2: 99% of numbers in range [0, 100]
 * ----------------------------------------------
 * Use HYBRID approach: counting array + two heaps
 * 
 * class MedianFinder {
 *     int[] count = new int[101];
 *     PriorityQueue<Integer> lowOutliers = new PriorityQueue<>(Collections.reverseOrder());
 *     PriorityQueue<Integer> highOutliers = new PriorityQueue<>();
 *     int inRangeTotal = 0;
 *     
 *     public void addNum(int num) {
 *         if (num >= 0 && num <= 100) {
 *             count[num]++;
 *             inRangeTotal++;
 *         } else if (num < 0) {
 *             lowOutliers.offer(num);
 *         } else {
 *             highOutliers.offer(num);
 *         }
 *     }
 *     
 *     public double findMedian() {
 *         // Build sorted view: lowOutliers + [0..100] + highOutliers
 *         // Find median position in this combined sorted array
 *         // Implementation details omitted for brevity
 *     }
 * }
 * 
 * Time: addNum O(log N) for 1% outliers, O(1) for 99%
 *       findMedian O(1) amortized
 * Space: O(N) but most in O(1) array
 * 
 * This optimizes for the common case while handling outliers!
 */

/**
 * EDGE CASES & TESTING
 * =====================
 * 
 * Test Cases:
 * 
 * 1. Single element
 *    addNum(5) -> findMedian() = 5.0
 * 
 * 2. Two elements (even count)
 *    addNum(1), addNum(2) -> findMedian() = 1.5
 * 
 * 3. Three elements (odd count)
 *    addNum(1), addNum(2), addNum(3) -> findMedian() = 2.0
 * 
 * 4. Ascending order
 *    addNum(1), addNum(2), addNum(3), addNum(4), addNum(5)
 *    Should maintain heaps correctly
 * 
 * 5. Descending order
 *    addNum(5), addNum(4), addNum(3), addNum(2), addNum(1)
 *    Should maintain heaps correctly
 * 
 * 6. Random order
 *    addNum(3), addNum(1), addNum(5), addNum(2), addNum(4)
 * 
 * 7. Duplicates
 *    addNum(5), addNum(5), addNum(5) -> findMedian() = 5.0
 * 
 * 8. Negative numbers
 *    addNum(-1), addNum(-2), addNum(-3)
 * 
 * 9. Large numbers
 *    addNum(100000), addNum(99999)
 * 
 * 10. Alternating small and large
 *     addNum(1), addNum(100), addNum(2), addNum(99)
 */

/**
 * VISUALIZATION HELPER
 * ====================
 * 
 * Here's a helper method to visualize heap states during debugging:
 */
class MedianFinderDebug extends MedianFinder {
    public void printState() {
        System.out.println("MaxHeap (smaller half): " + maxHeap);
        System.out.println("MinHeap (larger half): " + minHeap);
        System.out.println("Current median: " + findMedian());
        System.out.println();
    }
}

// Comprehensive test with visualization
class Main {
    public static void main(String[] args) {
        System.out.println("=== Test Case 1: Basic Example ===");
        MedianFinder mf1 = new MedianFinder();
        
        System.out.println("addNum(1)");
        mf1.addNum(1);
        System.out.println("Median: " + mf1.findMedian()); // 1.0
        
        System.out.println("\naddNum(2)");
        mf1.addNum(2);
        System.out.println("Median: " + mf1.findMedian()); // 1.5
        
        System.out.println("\naddNum(3)");
        mf1.addNum(3);
        System.out.println("Median: " + mf1.findMedian()); // 2.0
        
        System.out.println("\n=== Test Case 2: Descending Order ===");
        MedianFinder mf2 = new MedianFinder();
        mf2.addNum(5);
        mf2.addNum(4);
        mf2.addNum(3);
        mf2.addNum(2);
        mf2.addNum(1);
        System.out.println("After adding [5,4,3,2,1]");
        System.out.println("Median: " + mf2.findMedian()); // 3.0
        
        System.out.println("\n=== Test Case 3: Duplicates ===");
        MedianFinder mf3 = new MedianFinder();
        mf3.addNum(5);
        mf3.addNum(5);
        mf3.addNum(5);
        System.out.println("After adding [5,5,5]");
        System.out.println("Median: " + mf3.findMedian()); // 5.0
        
        System.out.println("\n=== Test Case 4: Negative Numbers ===");
        MedianFinder mf4 = new MedianFinder();
        mf4.addNum(-1);
        mf4.addNum(-2);
        mf4.addNum(-3);
        System.out.println("After adding [-1,-2,-3]");
        System.out.println("Median: " + mf4.findMedian()); // -2.0
        
        System.out.println("\n=== Test Case 5: Mixed Order ===");
        MedianFinder mf5 = new MedianFinder();
        int[] nums = {6, 10, 2, 6, 5, 0, 6, 3, 1, 0, 0};
        System.out.println("Adding: " + Arrays.toString(nums));
        for (int num : nums) {
            mf5.addNum(num);
        }
        System.out.println("Median: " + mf5.findMedian()); // Should be 3.0
        
        System.out.println("\n=== Test Case 6: Large Numbers ===");
        MedianFinder mf6 = new MedianFinder();
        mf6.addNum(100000);
        mf6.addNum(99999);
        mf6.addNum(100001);
        System.out.println("After adding [100000, 99999, 100001]");
        System.out.println("Median: " + mf6.findMedian()); // 100000.0
    }
}

/**
 * INTERVIEW TALKING POINTS
 * =========================
 * 
 * When presenting this solution in an interview:
 * 
 * 1. "The key insight is to use two heaps to partition the data around
 *    the median. A max heap stores the smaller half, and a min heap
 *    stores the larger half."
 * 
 * 2. "I'll maintain the invariant that all elements in the max heap
 *    are less than or equal to all elements in the min heap, and
 *    their sizes differ by at most 1."
 * 
 * 3. "This gives us O(log N) insertion and O(1) median finding,
 *    which is optimal for streaming data."
 * 
 * 4. [Draw diagram on whiteboard showing the two heaps]
 * 
 * 5. "For the follow-up about limited range, I'd use a counting
 *    array which gives O(1) for both operations since we can
 *    just count up to the median position."
 * 
 * 6. "The hybrid approach for 99% in range combines both techniques:
 *    counting array for common cases, heaps for outliers."
 * 
 * 7. "Edge cases include: single element, duplicates, negative
 *    numbers, and ensuring integer division doesn't cause issues
 *    with the average calculation."
 */


/**
 * ============================================================
 * 1) STANDARD SOLUTION — TWO HEAPS (GENERAL CASE)
 * ============================================================
 */
class MedianFinderTwoHeaps {

    private PriorityQueue<Integer> left;   // max-heap
    private PriorityQueue<Integer> right;  // min-heap

    public MedianFinderTwoHeaps() {
        left = new PriorityQueue<>((a, b) -> b - a);
        right = new PriorityQueue<>();
    }

    public void addNum(int num) {
        left.offer(num);
        right.offer(left.poll());

        if (right.size() > left.size()) {
            left.offer(right.poll());
        }
    }

    public double findMedian() {
        if (left.size() > right.size()) {
            return left.peek();
        }
        return (left.peek() + right.peek()) / 2.0;
    }
}

/**
 * ============================================================
 * 2) OPTIMIZED SOLUTION — COUNTING ARRAY ([0,100] ONLY)
 * ============================================================
 */
class MedianFinderCounting {

    private int[] freq = new int[101];
    private int count = 0;

    public void addNum(int num) {
        freq[num]++;
        count++;
    }

    public double findMedian() {
        int mid1 = (count + 1) / 2;
        int mid2 = (count + 2) / 2;

        int current = 0;
        int a = -1, b = -1;

        for (int i = 0; i <= 100; i++) {
            current += freq[i];
            if (a == -1 && current >= mid1) a = i;
            if (current >= mid2) {
                b = i;
                break;
            }
        }
        return (a + b) / 2.0;
    }
}

/**
 * ============================================================
 * 3) HYBRID SOLUTION — 99% IN [0,100]
 * ============================================================
 */
class MedianFinderHybrid {

    private int[] freq = new int[101];
    private int inRangeCount = 0;

    private TreeMap<Integer, Integer> low = new TreeMap<>();
    private TreeMap<Integer, Integer> high = new TreeMap<>();

    private int totalCount = 0;

    public void addNum(int num) {
        if (num >= 0 && num <= 100) {
            freq[num]++;
            inRangeCount++;
        } else if (num < 0) {
            low.put(num, low.getOrDefault(num, 0) + 1);
        } else {
            high.put(num, high.getOrDefault(num, 0) + 1);
        }
        totalCount++;
    }

    public double findMedian() {
        int mid1 = (totalCount + 1) / 2;
        int mid2 = (totalCount + 2) / 2;

        int a = findKth(mid1);
        int b = findKth(mid2);

        return (a + b) / 2.0;
    }

    private int findKth(int k) {
        int count = 0;

        // Check low outliers
        for (Map.Entry<Integer, Integer> e : low.entrySet()) {
            count += e.getValue();
            if (count >= k) return e.getKey();
        }

        // Check [0,100] range
        for (int i = 0; i <= 100; i++) {
            count += freq[i];
            if (count >= k) return i;
        }

        // Check high outliers
        for (Map.Entry<Integer, Integer> e : high.entrySet()) {
            count += e.getValue();
            if (count >= k) return e.getKey();
        }

        throw new IllegalStateException("Should not reach here");
    }
}

/**
 * ============================================================
 * TEST DRIVER
 * ============================================================
 */
class MedianFromDataStreamAll {

    public static void main(String[] args) {

        // -------- Two Heaps --------
        MedianFinderTwoHeaps mf1 = new MedianFinderTwoHeaps();
        mf1.addNum(1);
        mf1.addNum(2);
        System.out.println("Two Heaps Median: " + mf1.findMedian()); // 1.5
        mf1.addNum(3);
        System.out.println("Two Heaps Median: " + mf1.findMedian()); // 2.0

        // -------- Counting --------
        MedianFinderCounting mf2 = new MedianFinderCounting();
        mf2.addNum(10);
        mf2.addNum(20);
        mf2.addNum(30);
        System.out.println("Counting Median: " + mf2.findMedian()); // 20

        // -------- Hybrid --------
        MedianFinderHybrid mf3 = new MedianFinderHybrid();
        mf3.addNum(1);
        mf3.addNum(2);
        mf3.addNum(3);
        mf3.addNum(1000);
        mf3.addNum(-10);
        System.out.println("Hybrid Median: " + mf3.findMedian()); // 2
    }
}


class ThreadSafeMedianFinder {

    // Max heap for lower half
    private final PriorityQueue<Integer> left;
    // Min heap for upper half
    private final PriorityQueue<Integer> right;

    // Read-write lock
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ThreadSafeMedianFinder() {
        left = new PriorityQueue<>((a, b) -> b - a); // max-heap
        right = new PriorityQueue<>();              // min-heap
    }

    // WRITE operation
    public void addNum(int num) {
        lock.writeLock().lock();
        try {
            left.offer(num);
            right.offer(left.poll());

            if (right.size() > left.size()) {
                left.offer(right.poll());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // READ operation
    public double findMedian() {
        lock.readLock().lock();
        try {
            if (left.size() > right.size()) {
                return left.peek();
            }
            return (left.peek() + right.peek()) / 2.0;
        } finally {
            lock.readLock().unlock();
        }
    }

    // Simple test
    public static void main(String[] args) {
        ThreadSafeMedianFinder mf = new ThreadSafeMedianFinder();
        mf.addNum(1);
        mf.addNum(2);
        System.out.println(mf.findMedian()); // 1.5
        mf.addNum(3);
        System.out.println(mf.findMedian()); // 2.0
    }
}
