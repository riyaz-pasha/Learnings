import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * Sliding Window Median (LC 480)
 *
 * Approach:
 *   Use two heaps:
 *     - maxHeap (lower half)
 *     - minHeap (upper half)
 *
 * Also use lazy deletion with HashMap because removing arbitrary elements
 * from a heap is not efficient.
 *
 * Time Complexity:
 *   O(n log k)
 *   Each insert/remove operation costs log k.
 *
 * Space Complexity:
 *   O(k) for heaps + map
 */
class SlidingWindowMedian {

    // maxHeap stores the smaller half (largest element on top)
    private PriorityQueue<Integer> maxHeap =
            new PriorityQueue<>(Collections.reverseOrder());

    // minHeap stores the larger half (smallest element on top)
    private PriorityQueue<Integer> minHeap =
            new PriorityQueue<>();

    // delayed[num] = how many times num should be removed lazily
    private Map<Integer, Integer> delayed = new HashMap<>();

    // These track valid sizes (excluding delayed elements)
    private int maxSize = 0;
    private int minSize = 0;

    public double[] medianSlidingWindow(int[] nums, int k) {
        int n = nums.length;
        double[] result = new double[n - k + 1];

        // Build initial window
        for (int i = 0; i < k; i++) {
            add(nums[i]);
        }

        result[0] = getMedian(k);

        // Slide window
        for (int i = k; i < n; i++) {
            int addVal = nums[i];
            int removeVal = nums[i - k];

            add(addVal);
            remove(removeVal);

            result[i - k + 1] = getMedian(k);
        }

        return result;
    }

    /**
     * Add number into one of the heaps
     */
    private void add(int num) {
        if (maxHeap.isEmpty() || num <= maxHeap.peek()) {
            maxHeap.offer(num);
            maxSize++;
        } else {
            minHeap.offer(num);
            minSize++;
        }

        balanceHeaps();
    }

    /**
     * Mark number for lazy removal
     */
    private void remove(int num) {
        // Mark num as delayed
        delayed.put(num, delayed.getOrDefault(num, 0) + 1);

        // Adjust size counters depending on which heap num belongs to
        if (num <= maxHeap.peek()) {
            maxSize--;
            if (num == maxHeap.peek()) {
                prune(maxHeap);
            }
        } else {
            minSize--;
            if (!minHeap.isEmpty() && num == minHeap.peek()) {
                prune(minHeap);
            }
        }

        balanceHeaps();
    }

    /**
     * Keep heaps balanced:
     * maxHeap should have either equal size or 1 more element than minHeap
     */
    private void balanceHeaps() {

        // maxHeap too large -> move top to minHeap
        if (maxSize > minSize + 1) {
            minHeap.offer(maxHeap.poll());
            maxSize--;
            minSize++;
            prune(maxHeap);
        }

        // minHeap too large -> move top to maxHeap
        else if (maxSize < minSize) {
            maxHeap.offer(minHeap.poll());
            minSize--;
            maxSize++;
            prune(minHeap);
        }
    }

    /**
     * Remove all delayed elements from heap top
     */
    private void prune(PriorityQueue<Integer> heap) {
        while (!heap.isEmpty()) {
            int num = heap.peek();

            if (delayed.containsKey(num)) {
                int count = delayed.get(num);

                if (count == 1) delayed.remove(num);
                else delayed.put(num, count - 1);

                heap.poll(); // physically remove
            } else {
                break;
            }
        }
    }

    /**
     * Get median based on heap tops
     */
    private double getMedian(int k) {
        if (k % 2 == 1) {
            return (double) maxHeap.peek();
        }

        // Avoid overflow by casting to long first
        return ((long) maxHeap.peek() + (long) minHeap.peek()) / 2.0;
    }
}



/**
 * Sliding Window Median (TreeMap / Multiset approach)
 *
 * Maintain two multisets:
 *   left  -> lower half (max side)
 *   right -> upper half (min side)
 *
 * Invariants:
 *   1) left.size() == right.size()   OR   left.size() == right.size() + 1
 *   2) All elements in left <= all elements in right
 *
 * Median:
 *   - odd k  -> max(left)
 *   - even k -> (max(left) + min(right)) / 2
 *
 * Time Complexity: O(n log k)
 * Space Complexity: O(k)
 */
class SlidingWindowMedian2 {

    static class MultiSet {
        private final TreeMap<Integer, Integer> map = new TreeMap<>();
        private int size = 0;

        void add(int x) {
            map.put(x, map.getOrDefault(x, 0) + 1);
            size++;
        }

        void remove(int x) {
            int count = map.get(x);
            if (count == 1) map.remove(x);
            else map.put(x, count - 1);
            size--;
        }

        boolean contains(int x) {
            return map.containsKey(x);
        }

        int min() {
            return map.firstKey();
        }

        int max() {
            return map.lastKey();
        }

        int size() {
            return size;
        }

        boolean isEmpty() {
            return size == 0;
        }
    }

    public double[] medianSlidingWindow(int[] nums, int k) {
        int n = nums.length;
        double[] ans = new double[n - k + 1];

        MultiSet left = new MultiSet();   // max side
        MultiSet right = new MultiSet();  // min side

        // build first window
        for (int i = 0; i < k; i++) {
            add(nums[i], left, right);
        }
        ans[0] = median(left, right, k);

        // slide window
        for (int i = k; i < n; i++) {
            remove(nums[i - k], left, right);
            add(nums[i], left, right);

            ans[i - k + 1] = median(left, right, k);
        }

        return ans;
    }

    private void add(int num, MultiSet left, MultiSet right) {
        if (left.isEmpty() || num <= left.max()) {
            left.add(num);
        } else {
            right.add(num);
        }
        rebalance(left, right);
    }

    private void remove(int num, MultiSet left, MultiSet right) {
        // Remove from correct multiset
        if (left.contains(num)) {
            left.remove(num);
        } else {
            right.remove(num);
        }
        rebalance(left, right);
    }

    /**
     * Ensure:
     * left.size() == right.size() OR left.size() == right.size() + 1
     */
    private void rebalance(MultiSet left, MultiSet right) {

        // left too large -> move max(left) to right
        while (left.size() > right.size() + 1) {
            int val = left.max();
            left.remove(val);
            right.add(val);
        }

        // right too large -> move min(right) to left
        while (left.size() < right.size()) {
            int val = right.min();
            right.remove(val);
            left.add(val);
        }
    }

    private double median(MultiSet left, MultiSet right, int k) {
        if (k % 2 == 1) {
            return left.max();
        }
        return ((long) left.max() + (long) right.min()) / 2.0;
    }
}



/**
 * PROBLEM ANALYSIS: SLIDING WINDOW MEDIAN
 * ========================================
 * 
 * PROBLEM UNDERSTANDING:
 * - Given array of numbers and window size k
 * - Slide window from left to right, one position at a time
 * - Calculate median for each window position
 * - Return array of all medians
 * 
 * KEY CHALLENGES:
 * 1. Efficiently find median in a window
 * 2. Efficiently add/remove elements as window slides
 * 3. Handle both odd and even k values
 * 4. Deal with duplicates
 * 5. Large input sizes require optimal solution
 * 
 * INTERVIEW APPROACH - HOW TO THINK THROUGH THIS:
 * ================================================
 * 
 * Step 1: Understand median
 * - Odd window size k: middle element when sorted
 * - Even window size k: average of two middle elements
 * - Need to maintain sorted order or quick access to middle element(s)
 * 
 * Step 2: Naive approach (for understanding)
 * - For each window: sort k elements, find median
 * - Time: O(n * k log k) - too slow for large inputs
 * - This is good to mention but not optimal
 * 
 * Step 3: Think about data structures
 * - Need to maintain sorted order: BST, Heap, TreeMap
 * - Need to add/remove efficiently: O(log k)
 * - Need to access middle element(s): O(1) or O(log k)
 * 
 * Step 4: Key insight - Two Heaps!
 * - Similar to "Find Median from Data Stream"
 * - Max heap (left half) + Min heap (right half)
 * - Keep heaps balanced
 * - Median is at top of heap(s)
 * 
 * BUT: Heaps don't support efficient removal of arbitrary elements!
 * 
 * Step 5: Better approach - TreeMap or Multiset
 * - TreeMap maintains sorted order
 * - Can add/remove in O(log k)
 * - Can access middle element(s) efficiently
 * - Java's TreeMap with counts works well
 * 
 * Step 6: Two TreeSets approach (BEST for interviews)
 * - Split elements into two halves like two heaps
 * - Use TreeSet for each half (allows removal)
 * - Rebalance as we slide
 * 
 * APPROACHES TO DISCUSS IN INTERVIEW:
 * ===================================
 * 1. Naive: Sort each window - O(n * k log k)
 * 2. Two Heaps with lazy deletion - O(n log k)
 * 3. TreeMap with counts - O(n log k)
 * 4. Two Multisets - O(n log k) - RECOMMENDED
 */

/**
 * APPROACH 1: TWO MULTISETS (BEST FOR INTERVIEWS)
 * ================================================
 * 
 * INTUITION:
 * - Maintain two balanced halves of the window
 * - Small half: contains smaller half of elements (max at top)
 * - Large half: contains larger half of elements (min at top)
 * - Median is at the boundary between these halves
 * 
 * KEY OPERATIONS:
 * 1. Add element: place in appropriate half, rebalance
 * 2. Remove element: remove from appropriate half, rebalance
 * 3. Get median: peek at tops of halves
 * 
 * BALANCING INVARIANT:
 * - small.size() == large.size() OR small.size() == large.size() + 1
 * - All elements in small <= all elements in large
 */
class Solution {
    
    /**
     * Main solution using TreeMap-based multisets
     * 
     * TreeMap allows:
     * - Sorted order
     * - O(log k) add/remove
     * - firstKey()/lastKey() for accessing boundaries
     * - Handling duplicates with count
     */
    public double[] medianSlidingWindow(int[] nums, int k) {
        int n = nums.length;
        double[] result = new double[n - k + 1];
        
        // Two TreeMaps to act as multisets
        // small: max heap behavior (reversed order)
        // large: min heap behavior (natural order)
        TreeMap<Integer, Integer> small = new TreeMap<>(Collections.reverseOrder());
        TreeMap<Integer, Integer> large = new TreeMap<>();
        
        // Process first window
        for (int i = 0; i < k; i++) {
            addNum(small, large, nums[i]);
        }
        
        result[0] = getMedian(small, large, k);
        
        // Slide the window
        for (int i = k; i < n; i++) {
            // Remove leftmost element of previous window
            int toRemove = nums[i - k];
            removeNum(small, large, toRemove);
            
            // Add new element
            int toAdd = nums[i];
            addNum(small, large, toAdd);
            
            // Get median for current window
            result[i - k + 1] = getMedian(small, large, k);
        }
        
        return result;
    }
    
    /**
     * Add number to the two-multiset structure
     * Always maintain: small.size() == large.size() or small.size() == large.size() + 1
     */
    private void addNum(TreeMap<Integer, Integer> small, TreeMap<Integer, Integer> large, int num) {
        // Add to small first
        if (small.isEmpty() || num <= small.firstKey()) {
            small.put(num, small.getOrDefault(num, 0) + 1);
        } else {
            large.put(num, large.getOrDefault(num, 0) + 1);
        }
        
        // Rebalance
        rebalance(small, large);
    }
    
    /**
     * Remove number from the two-multiset structure
     */
    private void removeNum(TreeMap<Integer, Integer> small, TreeMap<Integer, Integer> large, int num) {
        // Find which multiset contains the number
        if (small.containsKey(num)) {
            small.put(num, small.get(num) - 1);
            if (small.get(num) == 0) {
                small.remove(num);
            }
        } else {
            large.put(num, large.get(num) - 1);
            if (large.get(num) == 0) {
                large.remove(num);
            }
        }
        
        // Rebalance
        rebalance(small, large);
    }
    
    /**
     * Rebalance the two multisets
     * Maintain invariant: small.size() == large.size() or small.size() == large.size() + 1
     */
    private void rebalance(TreeMap<Integer, Integer> small, TreeMap<Integer, Integer> large) {
        // Get total counts
        int smallSize = getSize(small);
        int largeSize = getSize(large);
        
        // small has too many elements
        if (smallSize > largeSize + 1) {
            int key = small.firstKey();
            small.put(key, small.get(key) - 1);
            if (small.get(key) == 0) {
                small.remove(key);
            }
            large.put(key, large.getOrDefault(key, 0) + 1);
        }
        // large has too many elements
        else if (largeSize > smallSize) {
            int key = large.firstKey();
            large.put(key, large.get(key) - 1);
            if (large.get(key) == 0) {
                large.remove(key);
            }
            small.put(key, small.getOrDefault(key, 0) + 1);
        }
    }
    
    /**
     * Get total number of elements in a multiset
     */
    private int getSize(TreeMap<Integer, Integer> map) {
        int size = 0;
        for (int count : map.values()) {
            size += count;
        }
        return size;
    }
    
    /**
     * Get median from the two multisets
     */
    private double getMedian(TreeMap<Integer, Integer> small, TreeMap<Integer, Integer> large, int k) {
        if (k % 2 == 1) {
            // Odd window size: median is max of small
            return (double) small.firstKey();
        } else {
            // Even window size: median is average of max of small and min of large
            return ((double) small.firstKey() + (double) large.firstKey()) / 2.0;
        }
    }
}

/**
 * APPROACH 2: OPTIMIZED WITH SIZE TRACKING
 * =========================================
 * 
 * Same algorithm but tracks sizes separately for efficiency
 * Avoids recalculating size every time
 */
class SolutionOptimized {
    
    // Track sizes separately
    private int smallSize = 0;
    private int largeSize = 0;
    
    public double[] medianSlidingWindow(int[] nums, int k) {
        int n = nums.length;
        double[] result = new double[n - k + 1];
        
        TreeMap<Integer, Integer> small = new TreeMap<>(Collections.reverseOrder());
        TreeMap<Integer, Integer> large = new TreeMap<>();
        
        // Build first window
        for (int i = 0; i < k; i++) {
            addNum(small, large, nums[i]);
        }
        
        result[0] = getMedian(small, large, k);
        
        // Slide window
        for (int i = k; i < n; i++) {
            removeNum(small, large, nums[i - k]);
            addNum(small, large, nums[i]);
            result[i - k + 1] = getMedian(small, large, k);
        }
        
        return result;
    }
    
    private void addNum(TreeMap<Integer, Integer> small, TreeMap<Integer, Integer> large, int num) {
        if (small.isEmpty() || num <= small.firstKey()) {
            small.put(num, small.getOrDefault(num, 0) + 1);
            smallSize++;
        } else {
            large.put(num, large.getOrDefault(num, 0) + 1);
            largeSize++;
        }
        rebalance(small, large);
    }
    
    private void removeNum(TreeMap<Integer, Integer> small, TreeMap<Integer, Integer> large, int num) {
        if (small.containsKey(num)) {
            small.put(num, small.get(num) - 1);
            if (small.get(num) == 0) {
                small.remove(num);
            }
            smallSize--;
        } else {
            large.put(num, large.get(num) - 1);
            if (large.get(num) == 0) {
                large.remove(num);
            }
            largeSize--;
        }
        rebalance(small, large);
    }
    
    private void rebalance(TreeMap<Integer, Integer> small, TreeMap<Integer, Integer> large) {
        if (smallSize > largeSize + 1) {
            int key = small.firstKey();
            small.put(key, small.get(key) - 1);
            if (small.get(key) == 0) {
                small.remove(key);
            }
            smallSize--;
            
            large.put(key, large.getOrDefault(key, 0) + 1);
            largeSize++;
        } else if (largeSize > smallSize) {
            int key = large.firstKey();
            large.put(key, large.get(key) - 1);
            if (large.get(key) == 0) {
                large.remove(key);
            }
            largeSize--;
            
            small.put(key, small.getOrDefault(key, 0) + 1);
            smallSize++;
        }
    }
    
    private double getMedian(TreeMap<Integer, Integer> small, TreeMap<Integer, Integer> large, int k) {
        if (k % 2 == 1) {
            return (double) small.firstKey();
        } else {
            // Use long to avoid overflow for large integers
            return ((double) small.firstKey() + (double) large.firstKey()) / 2.0;
        }
    }
}

/**
 * APPROACH 3: SIMPLE TREEMAP (ALTERNATIVE)
 * =========================================
 * 
 * Use single TreeMap and track middle position
 * Simpler conceptually but needs careful index tracking
 */
class SolutionSimpleTreeMap {
    
    public double[] medianSlidingWindow(int[] nums, int k) {
        int n = nums.length;
        double[] result = new double[n - k + 1];
        
        // TreeMap maintains sorted order with counts
        TreeMap<Integer, Integer> window = new TreeMap<>();
        
        // Build first window
        for (int i = 0; i < k; i++) {
            window.put(nums[i], window.getOrDefault(nums[i], 0) + 1);
        }
        
        result[0] = getMedian(window, k);
        
        // Slide window
        for (int i = k; i < n; i++) {
            // Remove leftmost
            int toRemove = nums[i - k];
            window.put(toRemove, window.get(toRemove) - 1);
            if (window.get(toRemove) == 0) {
                window.remove(toRemove);
            }
            
            // Add rightmost
            int toAdd = nums[i];
            window.put(toAdd, window.getOrDefault(toAdd, 0) + 1);
            
            result[i - k + 1] = getMedian(window, k);
        }
        
        return result;
    }
    
    private double getMedian(TreeMap<Integer, Integer> window, int k) {
        int mid = k / 2;
        int count = 0;
        Integer first = null, second = null;
        
        // Find the middle element(s)
        for (Map.Entry<Integer, Integer> entry : window.entrySet()) {
            int key = entry.getKey();
            int freq = entry.getValue();
            
            if (count <= mid && count + freq > mid) {
                first = key;
            }
            if (k % 2 == 0 && count < mid && count + freq >= mid) {
                second = key;
            }
            
            count += freq;
            if (first != null && (k % 2 == 1 || second != null)) {
                break;
            }
        }
        
        if (k % 2 == 1) {
            return first;
        } else {
            return ((double) first + (double) second) / 2.0;
        }
    }
}

/**
 * TEST CASES
 * ==========
 */
class TestSlidingWindowMedian {
    
    public static void main(String[] args) {
        Solution solution = new Solution();
        
        System.out.println("=== SLIDING WINDOW MEDIAN TESTS ===\n");
        
        // Test Case 1: Basic example with odd window
        int[] nums1 = {1, 3, -1, -3, 5, 3, 6, 7};
        int k1 = 3;
        System.out.println("Test 1: nums = " + Arrays.toString(nums1) + ", k = " + k1);
        double[] result1 = solution.medianSlidingWindow(nums1, k1);
        System.out.println("Result: " + Arrays.toString(result1));
        System.out.println("Expected: [1.0, -1.0, -1.0, 3.0, 5.0, 6.0]");
        System.out.println();
        
        // Test Case 2: Even window size
        int[] nums2 = {1, 2, 3, 4, 5, 6};
        int k2 = 4;
        System.out.println("Test 2: nums = " + Arrays.toString(nums2) + ", k = " + k2);
        double[] result2 = solution.medianSlidingWindow(nums2, k2);
        System.out.println("Result: " + Arrays.toString(result2));
        System.out.println("Expected: [2.5, 3.5, 4.5]");
        System.out.println();
        
        // Test Case 3: Window size = 1
        int[] nums3 = {1, 4, 2, 3};
        int k3 = 1;
        System.out.println("Test 3: nums = " + Arrays.toString(nums3) + ", k = " + k3);
        double[] result3 = solution.medianSlidingWindow(nums3, k3);
        System.out.println("Result: " + Arrays.toString(result3));
        System.out.println("Expected: [1.0, 4.0, 2.0, 3.0]");
        System.out.println();
        
        // Test Case 4: Duplicates
        int[] nums4 = {1, 2, 2, 2, 3, 4};
        int k4 = 3;
        System.out.println("Test 4: nums = " + Arrays.toString(nums4) + ", k = " + k4);
        double[] result4 = solution.medianSlidingWindow(nums4, k4);
        System.out.println("Result: " + Arrays.toString(result4));
        System.out.println("Expected: [2.0, 2.0, 2.0, 3.0]");
        System.out.println();
        
        // Test Case 5: Large numbers (overflow test)
        int[] nums5 = {2147483647, 2147483647};
        int k5 = 2;
        System.out.println("Test 5: nums = " + Arrays.toString(nums5) + ", k = " + k5);
        double[] result5 = solution.medianSlidingWindow(nums5, k5);
        System.out.println("Result: " + Arrays.toString(result5));
        System.out.println("Expected: [2147483647.0]");
        System.out.println();
        
        // Test Case 6: Negative numbers
        int[] nums6 = {-1, -2, -3, -4, -5};
        int k6 = 3;
        System.out.println("Test 6: nums = " + Arrays.toString(nums6) + ", k = " + k6);
        double[] result6 = solution.medianSlidingWindow(nums6, k6);
        System.out.println("Result: " + Arrays.toString(result6));
        System.out.println("Expected: [-2.0, -3.0, -4.0]");
        System.out.println();
        
        // Test Case 7: All same values
        int[] nums7 = {5, 5, 5, 5, 5};
        int k7 = 3;
        System.out.println("Test 7: nums = " + Arrays.toString(nums7) + ", k = " + k7);
        double[] result7 = solution.medianSlidingWindow(nums7, k7);
        System.out.println("Result: " + Arrays.toString(result7));
        System.out.println("Expected: [5.0, 5.0, 5.0]");
    }
}

/**
 * COMPLEXITY ANALYSIS
 * ===================
 * 
 * Time Complexity: O(n log k)
 * - n windows to process
 * - Each add/remove operation: O(log k) for TreeMap
 * - Rebalance: O(log k)
 * - Total: O(n log k)
 * 
 * Space Complexity: O(k)
 * - TreeMaps store at most k elements
 * - Result array: O(n - k + 1) = O(n)
 * - Overall: O(k) auxiliary space
 * 
 * 
 * INTERVIEW STRATEGY
 * ==================
 * 
 * 1. CLARIFY REQUIREMENTS:
 *    - "Can k be 1?" (Yes)
 *    - "Can k equal n?" (Yes, single window)
 *    - "Can there be duplicates?" (Yes)
 *    - "Can numbers be negative?" (Yes)
 *    - "What about integer overflow?" (Use double/long)
 * 
 * 2. START WITH SIMPLE APPROACH:
 *    "The naive approach would be to sort each window and find the median.
 *     That's O(n * k log k). For large inputs, we need something better."
 * 
 * 3. EXPLAIN THE INSIGHT:
 *    "The key insight is to maintain the window in sorted order as we slide.
 *     Instead of resorting, we can use a balanced data structure.
 *     
 *     I'll use the two-multiset approach:
 *     - Split window into smaller and larger halves
 *     - Median is at the boundary
 *     - As we slide, we add/remove elements and rebalance
 *     - This gives us O(log k) operations per window"
 * 
 * 4. DRAW A DIAGRAM:
 *    Window: [1, 3, -1, -3, 5]  k=3
 *    
 *    First window [1, 3, -1]:
 *    small: [-1, 1]  large: [3]
 *    median = 1
 *    
 *    Slide right, remove 1, add -3:
 *    small: [-3, -1]  large: [3]
 *    median = -1
 * 
 * 5. CODE INCREMENTALLY:
 *    - First: implement add/remove for TreeMap
 *    - Second: implement rebalancing
 *    - Third: implement getMedian
 *    - Finally: put it all together with sliding
 * 
 * 6. DISCUSS EDGE CASES:
 *    - k = 1 (every element is median)
 *    - k = n (single window)
 *    - All duplicates
 *    - Large numbers (overflow in average)
 *    - Negative numbers
 * 
 * 7. OPTIMIZE IF TIME:
 *    - Track sizes separately (SolutionOptimized)
 *    - Discuss alternative: insertion/deletion in sorted array
 *    - Discuss alternative: order statistic tree
 * 
 * 8. ALTERNATIVE APPROACHES TO MENTION:
 *    a) Naive sorting: O(n * k log k)
 *    b) Two heaps with lazy deletion: O(n log n)
 *    c) Order statistic tree: O(n log k)
 *    d) Bucket sort (if values bounded): O(n * k)
 * 
 * 9. COMMON MISTAKES TO AVOID:
 *    - Integer overflow when computing average
 *    - Not handling duplicates correctly
 *    - Off-by-one errors in median calculation
 *    - Not rebalancing after removal
 *    - Incorrect TreeMap comparator
 * 
 * 10. FOLLOW-UP QUESTIONS:
 *     - "What if k is very large?" (might need different approach)
 *     - "What if we need multiple medians (25th, 50th, 75th percentile)?"
 *     - "What if data is streaming?" (similar to data stream median)
 *     - "How would you parallelize this?" (independent windows)
 */
