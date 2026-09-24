import java.util.Arrays;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * KTH LARGEST ELEMENT IN AN ARRAY (LeetCode 215) - COMPREHENSIVE GUIDE
 * 
 * Problem: Find the kth largest element in an unsorted array.
 * Note: kth largest in sorted order, NOT kth distinct element.
 * 
 * DIFFICULTY: Medium
 * OPTIMAL TIME: O(n) average, O(n²) worst case with QuickSelect
 * OPTIMAL SPACE: O(1) with in-place QuickSelect
 * 
 * KEY INSIGHT: We DON'T need to sort the entire array!
 * 
 * CRITICAL CONCEPTS:
 * 1. QuickSelect Algorithm (derived from QuickSort)
 * 2. Min-Heap / Max-Heap techniques
 * 3. Partition schemes (Lomuto vs Hoare)
 * 4. Randomization to avoid worst case
 * 5. Selection algorithm optimization
 * 
 * COMPANIES: Amazon, Facebook, Google, Microsoft, Apple, Bloomberg, Adobe
 * 
 * IMPORTANT CLARIFICATIONS:
 * - k = 1 means LARGEST element (not smallest)
 * - Duplicates are counted separately: [3,2,3,1,2,4,5,5,6], k=4 → 4 (not 5)
 * - 1 <= k <= nums.length (always valid)
 * 
 * EDGE CASES:
 * 1. k = 1 (largest element)
 * 2. k = n (smallest element)
 * 3. All elements are the same
 * 4. Array with duplicates
 * 5. Single element array
 * 6. Already sorted arrays (ascending/descending)
 */
class KthLargestElement {

    // ========================================================================
    // APPROACH 1: QUICKSELECT (OPTIMAL - MOST IMPORTANT FOR INTERVIEWS)
    // ========================================================================
    /**
     * QuickSelect is a selection algorithm to find the kth element.
     * Similar to QuickSort but only recurses on one side.
     * 
     * ALGORITHM:
     * 1. Choose a pivot element
     * 2. Partition array so elements > pivot are on left, < pivot on right
     * 3. If pivot is at position k-1, we found it!
     * 4. If pivot is left of k-1, search right partition
     * 5. If pivot is right of k-1, search left partition
     * 
     * KEY INSIGHT:
     * - For kth LARGEST, we partition with larger elements on LEFT
     * - After partition, if pivot at index k-1, it's our answer
     * - We only need to recurse on ONE side (unlike QuickSort)
     * 
     * TIME: O(n) average, O(n²) worst case
     * - Average: Each partition reduces size by ~half: n + n/2 + n/4 + ... = 2n = O(n)
     * - Worst: Already sorted, bad pivot each time: n + (n-1) + (n-2) + ... = O(n²)
     * 
     * SPACE: O(1) - in-place partitioning
     * 
     * INTERVIEW FAVORITE:
     * This is THE expected solution for experienced candidates.
     * Shows deep understanding of algorithms and optimization.
     */
    public int findKthLargest(int[] nums, int k) {
        // Note: We're finding kth LARGEST, which is at index k-1 after sorting DESC
        return quickSelect(nums, 0, nums.length - 1, k - 1);
    }
    
    private int quickSelect(int[] nums, int left, int right, int k) {
        // Base case: only one element
        if (left == right) {
            return nums[left];
        }
        
        // Partition the array and get pivot's final position
        int pivotIndex = partition(nums, left, right);
        
        // Check if pivot is at the kth position
        if (pivotIndex == k) {
            return nums[pivotIndex]; // Found the kth largest!
        } else if (pivotIndex < k) {
            // kth largest is in the right partition
            return quickSelect(nums, pivotIndex + 1, right, k);
        } else {
            // kth largest is in the left partition
            return quickSelect(nums, left, pivotIndex - 1, k);
        }
    }
    
    /**
     * Lomuto Partition Scheme (easier to understand)
     * Partitions so that elements GREATER than pivot are on the LEFT.
     * This is crucial for finding kth LARGEST.
     * 
     * PROCESS:
     * - Choose rightmost element as pivot
     * - Maintain boundary: elements[left...i] > pivot, elements[i+1...j-1] <= pivot
     * - Place pivot at final position
     * 
     * Returns: Final position of pivot
     */
    private int partition(int[] nums, int left, int right) {
        int pivot = nums[right];
        int i = left; // Boundary index for elements > pivot
        
        // Move elements greater than pivot to the left
        for (int j = left; j < right; j++) {
            if (nums[j] > pivot) { // Note: > not < (for kth LARGEST)
                swap(nums, i, j);
                i++;
            }
        }
        
        // Place pivot at its final position
        swap(nums, i, right);
        return i;
    }
    
    private void swap(int[] nums, int i, int j) {
        int temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }

    // ========================================================================
    // APPROACH 2: RANDOMIZED QUICKSELECT (BETTER AVERAGE CASE)
    // ========================================================================
    /**
     * Randomized version to avoid O(n²) worst case on sorted arrays.
     * 
     * KEY IMPROVEMENT:
     * - Randomly choose pivot instead of always picking rightmost
     * - Makes worst case O(n²) extremely unlikely
     * - Expected time complexity O(n) with high probability
     * 
     * TIME: O(n) expected, O(n²) worst case (very rare)
     * SPACE: O(1)
     * 
     * WHEN TO MENTION:
     * - When interviewer asks about worst case handling
     * - When discussing optimization techniques
     * - For real-world production code
     */
    public int findKthLargestRandomized(int[] nums, int k) {
        return quickSelectRandom(nums, 0, nums.length - 1, k - 1);
    }
    
    private int quickSelectRandom(int[] nums, int left, int right, int k) {
        if (left == right) {
            return nums[left];
        }
        
        // Randomly select pivot index and swap with rightmost
        Random rand = new Random();
        int pivotIndex = left + rand.nextInt(right - left + 1);
        swap(nums, pivotIndex, right);
        
        // Now proceed with regular partition
        pivotIndex = partition(nums, left, right);
        
        if (pivotIndex == k) {
            return nums[pivotIndex];
        } else if (pivotIndex < k) {
            return quickSelectRandom(nums, pivotIndex + 1, right, k);
        } else {
            return quickSelectRandom(nums, left, pivotIndex - 1, k);
        }
    }

    // ========================================================================
    // APPROACH 3: MIN-HEAP (K ELEMENTS)
    // ========================================================================
    /**
     * Maintain a min-heap of size k containing the k largest elements.
     * The root of this heap is the kth largest element.
     * 
     * ALGORITHM:
     * 1. Build a min-heap of first k elements
     * 2. For remaining elements:
     *    - If element > heap root (min of k largest), replace root
     *    - Heapify to maintain heap property
     * 3. Root is kth largest element
     * 
     * WHY MIN-HEAP for LARGEST elements?
     * - We want to quickly remove the SMALLEST of the k largest
     * - Min-heap gives us O(log k) access to smallest element
     * 
     * TIME: O(n log k)
     * - Add/remove from heap of size k: O(log k)
     * - Do this for all n elements: O(n log k)
     * 
     * SPACE: O(k) for the heap
     * 
     * PROS:
     * + Better when k << n (e.g., finding top 10 in 1 million elements)
     * + Guaranteed O(n log k) time (no worst case like QuickSelect)
     * + Easy to implement with Java PriorityQueue
     * + Works well for streaming data
     * 
     * CONS:
     * - Slower than QuickSelect when k is large
     * - Uses O(k) extra space
     */
    public int findKthLargestHeap(int[] nums, int k) {
        // Min-heap of size k (smallest element at top)
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        
        // Process each element
        for (int num : nums) {
            minHeap.offer(num);
            
            // Keep heap size at k
            if (minHeap.size() > k) {
                minHeap.poll(); // Remove smallest element
            }
        }
        
        // Root of min-heap is kth largest
        return minHeap.peek();
    }

    // ========================================================================
    // APPROACH 4: MAX-HEAP (ALL ELEMENTS)
    // ========================================================================
    /**
     * Build max-heap with all elements, then extract k times.
     * 
     * ALGORITHM:
     * 1. Add all elements to max-heap
     * 2. Extract maximum k times
     * 3. kth extracted element is the answer
     * 
     * TIME: O(n + k log n)
     * - Build heap: O(n) using heapify
     * - Extract k times: O(k log n)
     * 
     * SPACE: O(n) for the heap
     * 
     * PROS:
     * + Simple and intuitive
     * + Better when k is small (k << n)
     * 
     * CONS:
     * - Uses O(n) space
     * - Slower than min-heap approach when k > n/2
     * - Not optimal for interviews
     */
    public int findKthLargestMaxHeap(int[] nums, int k) {
        // Max-heap (reverse natural ordering)
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        
        // Add all elements
        for (int num : nums) {
            maxHeap.offer(num);
        }
        
        // Extract k times
        int result = 0;
        for (int i = 0; i < k; i++) {
            result = maxHeap.poll();
        }
        
        return result;
    }

    // ========================================================================
    // APPROACH 5: SORTING (NAIVE BUT VALID)
    // ========================================================================
    /**
     * Sort the array and return kth element from end.
     * 
     * TIME: O(n log n) for sorting
     * SPACE: O(1) or O(n) depending on sorting algorithm
     * 
     * WHEN TO USE:
     * - Quick prototype or testing
     * - When explicitly allowed by interviewer
     * - As a baseline to compare other solutions
     * 
     * INTERVIEW APPROACH:
     * "I could solve this with sorting in O(n log n), but there's a better
     * approach using QuickSelect that's O(n) on average..."
     * 
     * DON'T start with this unless interviewer explicitly asks for it.
     */
    public int findKthLargestSort(int[] nums, int k) {
        Arrays.sort(nums);
        // kth largest is at index (n - k) in sorted array
        return nums[nums.length - k];
    }

    // ========================================================================
    // APPROACH 6: COUNTING SORT (FOR LIMITED RANGE)
    // ========================================================================
    /**
     * When the range of values is limited, use counting sort.
     * 
     * TIME: O(n + range) where range = max - min
     * SPACE: O(range)
     * 
     * WHEN TO USE:
     * - Range is small (e.g., 0 to 1000)
     * - Values are integers
     * - Interviewer mentions constraints on values
     * 
     * EXAMPLE CONSTRAINT:
     * -10^4 <= nums[i] <= 10^4 (range = 20,000)
     */
    public int findKthLargestCounting(int[] nums, int k) {
        // Find range
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int num : nums) {
            min = Math.min(min, num);
            max = Math.max(max, num);
        }
        
        // Count occurrences
        int range = max - min + 1;
        int[] count = new int[range];
        for (int num : nums) {
            count[num - min]++;
        }
        
        // Find kth largest by counting from right
        int remaining = k;
        for (int i = range - 1; i >= 0; i--) {
            remaining -= count[i];
            if (remaining <= 0) {
                return i + min;
            }
        }
        
        return -1; // Should never reach here
    }

    // ========================================================================
    // APPROACH 7: QUICKSELECT WITH MEDIAN OF MEDIANS (THEORETICAL)
    // ========================================================================
    /**
     * Guarantees O(n) worst-case time using median-of-medians pivot selection.
     * 
     * ALGORITHM:
     * 1. Divide array into groups of 5
     * 2. Find median of each group
     * 3. Recursively find median of medians
     * 4. Use this as pivot
     * 
     * TIME: O(n) worst case guaranteed
     * SPACE: O(log n) for recursion
     * 
     * INTERVIEW NOTE:
     * This is mostly theoretical. Mention only if interviewer asks about
     * guaranteed O(n) time complexity. Randomized QuickSelect is preferred
     * in practice due to better constants and simpler implementation.
     * 
     * DON'T implement this in interviews unless specifically asked!
     */
    public int findKthLargestMedianOfMedians(int[] nums, int k) {
        // Implementation omitted - complex and rarely needed
        // Mention as follow-up: "For guaranteed O(n), we could use median-of-medians..."
        return -1;
    }

    // ========================================================================
    // COMMON MISTAKES & DEBUGGING TIPS
    // ========================================================================
    /**
     * CRITICAL MISTAKES:
     * 
     * 1. WRONG: Partitioning for kth SMALLEST instead of LARGEST
     *    RIGHT: For kth LARGEST, put LARGER elements on LEFT
     *    Example: partition with (nums[j] > pivot) not (nums[j] < pivot)
     * 
     * 2. WRONG: Using k directly instead of k-1 for array indexing
     *    RIGHT: kth largest is at index k-1 (0-indexed)
     * 
     * 3. WRONG: Using MAX-heap for maintaining k largest elements
     *    RIGHT: Use MIN-heap so we can efficiently remove smallest of k largest
     * 
     * 4. WRONG: Not handling duplicates correctly
     *    RIGHT: Duplicates count separately [3,2,3], k=2 → 3 not 2
     * 
     * 5. WRONG: Continuing to recurse on both sides (like QuickSort)
     *    RIGHT: Only recurse on one side based on pivot position
     * 
     * 6. WRONG: Infinite recursion due to wrong partition logic
     *    RIGHT: Ensure partition always makes progress (pivot not included in recursive call)
     * 
     * 7. WRONG: Off-by-one in final result index after sorting
     *    RIGHT: kth largest at nums[n-k] not nums[n-k-1]
     * 
     * 8. WRONG: Not considering edge cases (k=1, k=n, single element)
     *    RIGHT: Test these explicitly
     * 
     * DEBUGGING TIPS:
     * - Print partition results to verify correctness
     * - Test with small sorted arrays [1,2,3,4,5]
     * - Test with all same elements [5,5,5,5,5]
     * - Verify partition maintains elements > pivot on left
     */

    // ========================================================================
    // INTERVIEW COMMUNICATION STRATEGY
    // ========================================================================
    /**
     * OPTIMAL INTERVIEW FLOW (25-30 minutes):
     * 
     * PHASE 1: CLARIFICATION (2-3 min)
     * Questions to ask:
     * - "Just to confirm, kth largest means the kth element when sorted in 
     *    descending order, correct?" (YES)
     * - "Should I handle duplicates as separate elements?" (YES)
     * - "Can I assume k is always valid (1 <= k <= n)?" (YES)
     * - "Are all values integers?" (Usually YES)
     * - "Is there a constraint on the value range?" (Affects approach choice)
     * 
     * PHASE 2: APPROACH DISCUSSION (3-5 min)
     * 
     * Start with simpler ideas:
     * "The straightforward approach is to sort the array in O(n log n) time
     * and return the kth element from the end."
     * 
     * Then optimize:
     * "But we can do better. We don't need a fully sorted array - just need
     * to find one element. QuickSelect can do this in O(n) average time."
     * 
     * Alternative if asked:
     * "Another approach is using a min-heap of size k, which gives us
     * O(n log k) time. This is better when k is much smaller than n."
     * 
     * PHASE 3: CHOOSE APPROACH (1 min)
     * "I'll implement QuickSelect as it's O(n) average time and O(1) space,
     * which is optimal. I can add randomization if we want to avoid worst case."
     * 
     * PHASE 4: COMPLEXITY ANALYSIS (1 min)
     * - Time: O(n) average, O(n²) worst (explain why)
     * - Space: O(1) for iterative, O(log n) for recursive stack
     * - Mention heap alternative: O(n log k) time, O(k) space
     * 
     * PHASE 5: CODING (15-18 min)
     * - Implement cleanly with comments
     * - Explain partition logic carefully
     * - Handle base cases
     * 
     * PHASE 6: TESTING (3-5 min)
     * Test cases:
     * 1. Given examples
     * 2. k = 1 (largest): [3,2,1,5,6,4], k=1 → 6
     * 3. k = n (smallest): [3,2,1,5,6,4], k=6 → 1
     * 4. Duplicates: [3,2,3,1,2,4,5,5,6], k=4 → 4
     * 5. All same: [5,5,5,5], k=2 → 5
     * 6. Single element: [1], k=1 → 1
     * 
     * PHASE 7: FOLLOW-UP (if time)
     * - Randomization for worst case
     * - Heap approach for small k
     * - Median of medians for guaranteed O(n)
     * - How to find kth smallest (same algorithm, change partition)
     */

    // ========================================================================
    // KEY INSIGHTS & PATTERNS
    // ========================================================================
    /**
     * PATTERN RECOGNITION:
     * 
     * 1. SELECTION vs SORTING:
     *    - Selection: Find kth element in O(n)
     *    - Sorting: Find all positions in O(n log n)
     *    - Key: We only need ONE element, not all sorted
     * 
     * 2. QUICKSELECT INTUITION:
     *    - Like binary search on QuickSort
     *    - Partition tells us exactly where pivot belongs
     *    - We only search one side (halving search space)
     * 
     * 3. HEAP SELECTION:
     *    - Min-heap of k elements = k largest seen so far
     *    - Root = smallest of k largest = kth largest
     *    - Works great for streaming data
     * 
     * 4. SPACE-TIME TRADEOFF:
     *    - QuickSelect: O(n) time, O(1) space
     *    - Min-heap: O(n log k) time, O(k) space
     *    - Trade space for guaranteed time complexity
     * 
     * 5. PARTITION DIRECTION:
     *    - kth LARGEST: larger elements on LEFT
     *    - kth SMALLEST: smaller elements on LEFT
     *    - Just flip comparison in partition!
     */

    // ========================================================================
    // RELATED PROBLEMS & VARIATIONS
    // ========================================================================
    /**
     * RELATED LEETCODE PROBLEMS:
     * 
     * 1. LeetCode 973 - K Closest Points to Origin
     *    - Same pattern: QuickSelect or heap
     *    - Sort by distance instead of value
     * 
     * 2. LeetCode 347 - Top K Frequent Elements
     *    - Count frequencies, then find kth largest frequency
     *    - Use heap or bucket sort
     * 
     * 3. LeetCode 703 - Kth Largest Element in a Stream
     *    - Maintain min-heap of size k
     *    - Add new elements dynamically
     * 
     * 4. LeetCode 414 - Third Maximum Number
     *    - Special case: k=3, find distinct values
     * 
     * 5. LeetCode 1985 - Find the Kth Largest Integer in Array
     *    - Same problem but with very large numbers (strings)
     * 
     * VARIATIONS:
     * - Find kth SMALLEST (flip partition comparison)
     * - Find kth DISTINCT largest (use Set, then select)
     * - Find top k elements (not just kth) - return list
     * - Find median (k = n/2)
     * - Find range [kth smallest, kth largest]
     */

    // ========================================================================
    // COMPLEXITY COMPARISON TABLE
    // ========================================================================
    /**
     * ┌─────────────────────┬──────────────┬───────────────┬──────────────┐
     * │ Approach            │ Time Avg     │ Time Worst    │ Space        │
     * ├─────────────────────┼──────────────┼───────────────┼──────────────┤
     * │ QuickSelect         │ O(n)         │ O(n²)         │ O(1)         │
     * │ Randomized QS       │ O(n)         │ O(n²) rare    │ O(1)         │
     * │ Min-Heap (k)        │ O(n log k)   │ O(n log k)    │ O(k)         │
     * │ Max-Heap (all)      │ O(n+k log n) │ O(n+k log n)  │ O(n)         │
     * │ Sorting             │ O(n log n)   │ O(n log n)    │ O(1) to O(n) │
     * │ Counting Sort       │ O(n+range)   │ O(n+range)    │ O(range)     │
     * │ Median-of-Medians   │ O(n)         │ O(n)          │ O(log n)     │
     * └─────────────────────┴──────────────┴───────────────┴──────────────┘
     * 
     * WHEN TO USE EACH:
     * 
     * QuickSelect:
     * ✓ Default choice for interviews
     * ✓ Best average case
     * ✓ No extra space
     * 
     * Min-Heap (size k):
     * ✓ When k << n (e.g., top 10 in 1M elements)
     * ✓ Guaranteed time complexity
     * ✓ Streaming data (can't fit all in memory)
     * 
     * Sorting:
     * ✓ Need multiple order statistics
     * ✓ Data already partially sorted
     * ✓ Quick prototype
     * 
     * Counting Sort:
     * ✓ Small value range (< 10^4)
     * ✓ Integer values only
     * ✓ Need guaranteed linear time
     */

    // ========================================================================
    // TEST CASES & VALIDATION
    // ========================================================================
    public static void main(String[] args) {
        KthLargestElement solution = new KthLargestElement();
        
        System.out.println("=".repeat(70));
        System.out.println("KTH LARGEST ELEMENT - COMPREHENSIVE TEST SUITE");
        System.out.println("=".repeat(70));
        
        // Test Case 1: Example 1
        int[] nums1 = {3, 2, 1, 5, 6, 4};
        int k1 = 2;
        System.out.println("\nTest 1: Standard Example");
        System.out.println("Input: nums = " + Arrays.toString(nums1) + ", k = " + k1);
        System.out.println("Expected: 5");
        System.out.println("QuickSelect:     " + solution.findKthLargest(nums1.clone(), k1));
        System.out.println("Randomized QS:   " + solution.findKthLargestRandomized(nums1.clone(), k1));
        System.out.println("Min-Heap:        " + solution.findKthLargestHeap(nums1.clone(), k1));
        System.out.println("Max-Heap:        " + solution.findKthLargestMaxHeap(nums1.clone(), k1));
        System.out.println("Sorting:         " + solution.findKthLargestSort(nums1.clone(), k1));
        
        // Test Case 2: Example 2 (with duplicates)
        int[] nums2 = {3, 2, 3, 1, 2, 4, 5, 5, 6};
        int k2 = 4;
        System.out.println("\nTest 2: Array with Duplicates");
        System.out.println("Input: nums = " + Arrays.toString(nums2) + ", k = " + k2);
        System.out.println("Expected: 4");
        System.out.println("Result:   " + solution.findKthLargest(nums2.clone(), k2));
        
        // Test Case 3: k = 1 (largest element)
        int[] nums3 = {3, 2, 1, 5, 6, 4};
        int k3 = 1;
        System.out.println("\nTest 3: k = 1 (Largest Element)");
        System.out.println("Input: nums = " + Arrays.toString(nums3) + ", k = " + k3);
        System.out.println("Expected: 6");
        System.out.println("Result:   " + solution.findKthLargest(nums3.clone(), k3));
        
        // Test Case 4: k = n (smallest element)
        int[] nums4 = {3, 2, 1, 5, 6, 4};
        int k4 = 6;
        System.out.println("\nTest 4: k = n (Smallest Element)");
        System.out.println("Input: nums = " + Arrays.toString(nums4) + ", k = " + k4);
        System.out.println("Expected: 1");
        System.out.println("Result:   " + solution.findKthLargest(nums4.clone(), k4));
        
        // Test Case 5: All same elements
        int[] nums5 = {5, 5, 5, 5, 5};
        int k5 = 3;
        System.out.println("\nTest 5: All Same Elements");
        System.out.println("Input: nums = " + Arrays.toString(nums5) + ", k = " + k5);
        System.out.println("Expected: 5");
        System.out.println("Result:   " + solution.findKthLargest(nums5.clone(), k5));
        
        // Test Case 6: Single element
        int[] nums6 = {1};
        int k6 = 1;
        System.out.println("\nTest 6: Single Element");
        System.out.println("Input: nums = " + Arrays.toString(nums6) + ", k = " + k6);
        System.out.println("Expected: 1");
        System.out.println("Result:   " + solution.findKthLargest(nums6.clone(), k6));
        
        // Test Case 7: Already sorted (worst case for basic QuickSelect)
        int[] nums7 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int k7 = 3;
        System.out.println("\nTest 7: Sorted Array (Worst Case)");
        System.out.println("Input: nums = " + Arrays.toString(nums7) + ", k = " + k7);
        System.out.println("Expected: 8");
        System.out.println("Basic QS:      " + solution.findKthLargest(nums7.clone(), k7));
        System.out.println("Randomized QS: " + solution.findKthLargestRandomized(nums7.clone(), k7));
        
        // Test Case 8: Negative numbers
        int[] nums8 = {-1, -5, -3, -2, -4};
        int k8 = 2;
        System.out.println("\nTest 8: Negative Numbers");
        System.out.println("Input: nums = " + Arrays.toString(nums8) + ", k = " + k8);
        System.out.println("Expected: -2");
        System.out.println("Result:   " + solution.findKthLargest(nums8.clone(), k8));
        
        // Performance Comparison
        System.out.println("\n" + "=".repeat(70));
        System.out.println("PERFORMANCE COMPARISON (10000 elements, k=5000)");
        System.out.println("=".repeat(70));
        
        int[] largeArray = new int[10000];
        Random rand = new Random(42); // Fixed seed for consistency
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = rand.nextInt(10000);
        }
        int kLarge = 5000;
        
        long start, end;
        
        start = System.nanoTime();
        int result1 = solution.findKthLargest(largeArray.clone(), kLarge);
        end = System.nanoTime();
        System.out.println("QuickSelect:     " + (end - start) / 1000 + " μs, result: " + result1);
        
        start = System.nanoTime();
        int result2 = solution.findKthLargestRandomized(largeArray.clone(), kLarge);
        end = System.nanoTime();
        System.out.println("Randomized QS:   " + (end - start) / 1000 + " μs, result: " + result2);
        
        start = System.nanoTime();
        int result3 = solution.findKthLargestHeap(largeArray.clone(), kLarge);
        end = System.nanoTime();
        System.out.println("Min-Heap:        " + (end - start) / 1000 + " μs, result: " + result3);
        
        start = System.nanoTime();
        int result4 = solution.findKthLargestSort(largeArray.clone(), kLarge);
        end = System.nanoTime();
        System.out.println("Sorting:         " + (end - start) / 1000 + " μs, result: " + result4);
        
        System.out.println("\n" + "=".repeat(70));
    }
}

/**
 * ============================================================================
 * FINAL INTERVIEW CHECKLIST
 * ============================================================================
 * 
 * BEFORE CODING:
 * □ Clarified kth largest vs kth smallest
 * □ Confirmed handling of duplicates
 * □ Asked about constraints (array size, value range)
 * □ Discussed multiple approaches
 * □ Stated time and space complexity
 * □ Got interviewer approval on approach
 * 
 * WHILE CODING:
 * □ Used clear variable names (left, right, pivot, k)
 * □ Wrote helper functions (partition, swap)
 * □ Added comments for partition logic
 * □ Handled base cases (left == right)
 * □ Correct partition comparison (> for largest)
 * □ Correct index conversion (k-1 for 0-indexed)
 * 
 * AFTER CODING:
 * □ Traced through example by hand
 * □ Tested edge cases (k=1, k=n, duplicates)
 * □ Verified time/space complexity
 * □ Discussed randomization for worst case
 * □ Mentioned heap alternative
 * 
 * KEY TALKING POINTS:
 * ✓ "QuickSelect is like QuickSort but only recurses on one side"
 * ✓ "Average O(n): n + n/2 + n/4 + ... = 2n"
 * ✓ "For kth LARGEST, we partition with larger elements on left"
 * ✓ "Heap is better when k << n, QuickSelect better overall"
 * ✓ "Randomization makes worst case O(n²) extremely unlikely"
 * 
 * COMMON PITFALLS AVOIDED:
 * ✗ Wrong partition direction (< instead of >)
 * ✗ Using k instead of k-1 for indexing
 * ✗ Max-heap instead of min-heap for k elements
 * ✗ Including pivot in recursive calls (infinite loop)
 * ✗ Not handling already sorted arrays
 * 
 * ============================================================================
 */
