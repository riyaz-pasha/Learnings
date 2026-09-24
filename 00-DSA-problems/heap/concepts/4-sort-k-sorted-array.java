import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * SORT A K-SORTED (NEARLY SORTED) ARRAY - COMPREHENSIVE GUIDE
 * 
 * Problem: Given an array where each element is at most k positions away from its
 * target position in the sorted array, sort the entire array efficiently.
 * 
 * CONSTRAINT: Each element is at most k distance from its correct position.
 * - If completely sorted position is j, element can be at indices [j-k, j+k]
 * 
 * EXAMPLES:
 * Input: arr[] = {6, 5, 3, 2, 8, 10, 9}, k = 3
 * Output: {2, 3, 5, 6, 8, 9, 10}
 * 
 * Input: arr[] = {10, 9, 8, 7, 4, 70, 60, 50}, k = 4
 * Output: {4, 7, 8, 9, 10, 50, 60, 70}
 * 
 * KEY INSIGHT: For position i in sorted array, we only need to look at
 * elements in window [i, i+k] of unsorted array.
 * 
 * DIFFICULTY: Medium
 * OPTIMAL TIME: O(n log k)
 * OPTIMAL SPACE: O(k)
 * 
 * KEY CONCEPTS:
 * 1. Min-Heap (Priority Queue) of size k+1
 * 2. Sliding Window pattern
 * 3. Insertion Sort optimization
 * 4. Comparison with standard sorting
 * 
 * COMPANIES: Google, Amazon, Microsoft, Facebook, Adobe, Flipkart
 * 
 * RELATED PROBLEMS:
 * - Merge K Sorted Lists
 * - Find K Closest Elements
 * - Kth Largest Element
 * 
 * EDGE CASES:
 * 1. k = 0 (already sorted)
 * 2. k >= n-1 (standard sorting needed)
 * 3. k = 1 (adjacent swaps only)
 * 4. Single element array
 * 5. All elements same
 */
class SortKSortedArray {

    // ========================================================================
    // APPROACH 1: MIN-HEAP (OPTIMAL - MOST IMPORTANT)
    // ========================================================================
    /**
     * Use a min-heap of size k+1 to efficiently sort the k-sorted array.
     * 
     * ALGORITHM:
     * 1. Create min-heap and add first k+1 elements
     * 2. For each remaining element:
     *    a. Extract min from heap (this goes to current sorted position)
     *    b. Add next element to heap
     * 3. Extract remaining elements from heap
     * 
     * WHY K+1 HEAP SIZE?
     * - Element at position i can be at most k positions away
     * - So element for sorted position 0 must be in first k+1 elements
     * - Similarly, element for position i is in elements [i, i+k]
     * 
     * KEY INSIGHT:
     * At any point, the minimum element in heap is the next element
     * in sorted order because:
     * - We've processed all elements that could be smaller (they're already placed)
     * - We have all elements that could be the next minimum (within k distance)
     * 
     * TIME: O(n log k)
     * - n elements to process
     * - Each heap operation takes O(log k)
     * - Heap size is k+1, not n
     * 
     * SPACE: O(k) for the heap
     * 
     * INTERVIEW FAVORITE:
     * This is THE expected solution. Shows understanding of:
     * - Min-heap applications
     * - Space-time optimization
     * - Problem constraints exploitation
     */
    public void sortKSorted(int[] arr, int k) {
        int n = arr.length;
        
        // Edge case: empty array or k is 0 (already sorted)
        if (n == 0 || k == 0) {
            return;
        }
        
        // Min-heap of size k+1
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        
        int index = 0; // Index to place next sorted element
        
        // Step 1: Add first k+1 elements to heap
        // (or all elements if array is smaller)
        for (int i = 0; i <= k && i < n; i++) {
            minHeap.offer(arr[i]);
        }
        
        // Step 2: Process remaining elements
        // For each element after k+1:
        // - Extract min (goes to sorted position)
        // - Add next element to heap
        for (int i = k + 1; i < n; i++) {
            arr[index++] = minHeap.poll();
            minHeap.offer(arr[i]);
        }
        
        // Step 3: Extract remaining elements from heap
        while (!minHeap.isEmpty()) {
            arr[index++] = minHeap.poll();
        }
    }

    // ========================================================================
    // APPROACH 2: MIN-HEAP (CLEANER IMPLEMENTATION)
    // ========================================================================
    /**
     * Alternative implementation with clearer structure.
     * Same logic but more readable for interviews.
     * 
     * TIME: O(n log k)
     * SPACE: O(k)
     */
    public void sortKSortedCleaner(int[] arr, int k) {
        int n = arr.length;
        if (n == 0) return;
        
        // Adjust k if it's larger than array size
        k = Math.min(k, n - 1);
        
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        
        // Build initial heap with k+1 elements
        int heapSize = Math.min(k + 1, n);
        for (int i = 0; i < heapSize; i++) {
            minHeap.offer(arr[i]);
        }
        
        // Place elements in sorted order
        int targetIndex = 0;
        for (int i = heapSize; i < n; i++) {
            // Place smallest element from heap
            arr[targetIndex++] = minHeap.poll();
            // Add new element to heap
            minHeap.offer(arr[i]);
        }
        
        // Place remaining elements
        while (!minHeap.isEmpty()) {
            arr[targetIndex++] = minHeap.poll();
        }
    }

    // ========================================================================
    // APPROACH 3: MIN-HEAP WITH CUSTOM OBJECT (FOR MAINTAINING INDICES)
    // ========================================================================
    /**
     * If we need to track original indices (stable sort variant).
     * 
     * TIME: O(n log k)
     * SPACE: O(k)
     * 
     * USE WHEN:
     * - Need stable sort
     * - Need to track original positions
     * - Working with objects, not just integers
     */
    static class Element implements Comparable<Element> {
        int value;
        int originalIndex;
        
        Element(int value, int originalIndex) {
            this.value = value;
            this.originalIndex = originalIndex;
        }
        
        @Override
        public int compareTo(Element other) {
            // Primary: compare by value
            int cmp = Integer.compare(this.value, other.value);
            if (cmp != 0) return cmp;
            // Secondary: compare by original index (for stability)
            return Integer.compare(this.originalIndex, other.originalIndex);
        }
    }
    
    public void sortKSortedStable(int[] arr, int k) {
        int n = arr.length;
        if (n == 0) return;
        
        PriorityQueue<Element> minHeap = new PriorityQueue<>();
        
        // Add first k+1 elements
        for (int i = 0; i <= k && i < n; i++) {
            minHeap.offer(new Element(arr[i], i));
        }
        
        int index = 0;
        
        // Process remaining elements
        for (int i = k + 1; i < n; i++) {
            arr[index++] = minHeap.poll().value;
            minHeap.offer(new Element(arr[i], i));
        }
        
        // Extract remaining
        while (!minHeap.isEmpty()) {
            arr[index++] = minHeap.poll().value;
        }
    }

    // ========================================================================
    // APPROACH 4: INSERTION SORT (FOR SMALL K)
    // ========================================================================
    /**
     * Optimized insertion sort that only looks back k positions.
     * 
     * ALGORITHM:
     * For each element, compare with at most k previous elements
     * and insert at correct position.
     * 
     * TIME: O(n * k)
     * - For each of n elements
     * - Compare with at most k elements
     * 
     * SPACE: O(1) - in-place sorting
     * 
     * WHEN TO USE:
     * - k is very small (k < 10)
     * - Memory is constrained (O(1) space)
     * - Simple implementation needed
     * 
     * PROS:
     * + O(1) space
     * + Simple to implement
     * + Good cache locality
     * + Stable sort
     * 
     * CONS:
     * - Slower than heap for large k
     * - O(n*k) can be worse than O(n log n) if k is large
     */
    public void sortKSortedInsertion(int[] arr, int k) {
        int n = arr.length;
        
        for (int i = 1; i < n; i++) {
            int key = arr[i];
            int j = i - 1;
            
            // Only look back at most k positions
            // (element can't be more than k positions away)
            int limit = Math.max(0, i - k - 1);
            
            // Shift elements greater than key
            while (j >= limit && arr[j] > key) {
                arr[j + 1] = arr[j];
                j--;
            }
            
            arr[j + 1] = key;
        }
    }

    // ========================================================================
    // APPROACH 5: STANDARD SORTING (BASELINE)
    // ========================================================================
    /**
     * Use built-in sort - ignores the k constraint.
     * 
     * TIME: O(n log n)
     * SPACE: O(1) or O(log n) depending on sort
     * 
     * WHEN TO MENTION:
     * - As baseline comparison
     * - When k is very large (k >= n/log n)
     * - To show understanding of when optimization matters
     * 
     * INTERVIEW NOTE:
     * "If k is large, standard sorting might be better.
     * Heap approach is O(n log k), standard is O(n log n).
     * When k ≈ n, heap approach degrades to O(n log n) too."
     */
    public void sortKSortedStandard(int[] arr, int k) {
        Arrays.sort(arr);
    }

    // ========================================================================
    // APPROACH 6: BUCKET SORT VARIANT (FOR LIMITED RANGE)
    // ========================================================================
    /**
     * When values have limited range, use bucket/counting sort approach.
     * 
     * TIME: O(n + range)
     * SPACE: O(range)
     * 
     * WHEN TO USE:
     * - Range of values is small
     * - Values are integers
     * - k is large but range is small
     */
    public void sortKSortedCounting(int[] arr, int k) {
        if (arr.length == 0) return;
        
        // Find range
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int num : arr) {
            min = Math.min(min, num);
            max = Math.max(max, num);
        }
        
        int range = max - min + 1;
        
        // Only use counting sort if range is reasonable
        if (range > arr.length * 10) {
            // Fall back to heap approach
            sortKSorted(arr, k);
            return;
        }
        
        // Count occurrences
        int[] count = new int[range];
        for (int num : arr) {
            count[num - min]++;
        }
        
        // Place elements back
        int index = 0;
        for (int i = 0; i < range; i++) {
            while (count[i] > 0) {
                arr[index++] = i + min;
                count[i]--;
            }
        }
    }

    // ========================================================================
    // APPROACH 7: BINARY INSERTION SORT
    // ========================================================================
    /**
     * Use binary search to find insertion position.
     * Reduces comparisons but not shifts.
     * 
     * TIME: O(n * k) - still need to shift elements
     * SPACE: O(1)
     * 
     * WHEN TO USE:
     * - Comparisons are expensive
     * - k is small
     */
    public void sortKSortedBinaryInsertion(int[] arr, int k) {
        int n = arr.length;
        
        for (int i = 1; i < n; i++) {
            int key = arr[i];
            
            // Binary search in range [max(0, i-k-1), i-1]
            int left = Math.max(0, i - k - 1);
            int right = i - 1;
            
            // Find position to insert
            int pos = binarySearch(arr, left, right, key);
            
            // Shift elements
            for (int j = i - 1; j >= pos; j--) {
                arr[j + 1] = arr[j];
            }
            
            arr[pos] = key;
        }
    }
    
    private int binarySearch(int[] arr, int left, int right, int key) {
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (arr[mid] > key) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }
        return left;
    }

    // ========================================================================
    // COMMON MISTAKES & DEBUGGING TIPS
    // ========================================================================
    /**
     * CRITICAL MISTAKES:
     * 
     * 1. WRONG: Using heap of size k instead of k+1
     *    RIGHT: Heap must be size k+1
     *    Reason: Element for position 0 could be at index k
     *    Example: [1,2,3] with k=2 means 1 could be at index 2
     * 
     * 2. WRONG: Not handling case when k >= n
     *    RIGHT: Use k = min(k, n-1) to avoid array bounds issues
     * 
     * 3. WRONG: Forgetting to extract remaining elements from heap
     *    RIGHT: Always drain the heap after main loop
     * 
     * 4. WRONG: Using max-heap instead of min-heap
     *    RIGHT: Need min-heap to get smallest elements first
     * 
     * 5. WRONG: Modifying array while building heap
     *    RIGHT: Build heap first, then place elements
     * 
     * 6. WRONG: Off-by-one in insertion sort limit
     *    RIGHT: limit = max(0, i - k - 1) not i - k
     * 
     * 7. WRONG: Not considering k=0 edge case
     *    RIGHT: Return early if k=0 (already sorted)
     * 
     * 8. WRONG: Assuming elements are distinct
     *    RIGHT: Handle duplicates properly in stable sort
     * 
     * DEBUGGING TIPS:
     * - Print heap contents at each step
     * - Verify k+1 heap size is maintained
     * - Check that index never exceeds array bounds
     * - Test with k=0, k=1, k=n-1
     * - Verify sorted output with Arrays.equals()
     */

    // ========================================================================
    // INTERVIEW COMMUNICATION STRATEGY
    // ========================================================================
    /**
     * OPTIMAL INTERVIEW FLOW (25-30 minutes):
     * 
     * PHASE 1: CLARIFICATION (2-3 min)
     * Questions to ask:
     * - "Can elements be at exactly k positions away or at most k?" (AT MOST)
     * - "Can k be larger than array length?" (Yes, handle this case)
     * - "Should I sort in-place or can I use extra space?" (Usually in-place)
     * - "Are there duplicates?" (Usually YES)
     * - "What is the range of k? Small or large?" (Affects approach choice)
     * 
     * PHASE 2: EXAMPLES (2-3 min)
     * Work through example:
     * arr = [6, 5, 3, 2, 8, 10, 9], k = 3
     * 
     * Explain why heap works:
     * "The smallest element must be in first k+1 elements (indices 0-3).
     * So if I keep a min-heap of size k+1, I can always extract the next
     * sorted element."
     * 
     * Show heap state:
     * Initial heap: [6,5,3,2] → min is 2
     * Place 2, add 8: [6,5,3,8] → min is 3
     * Place 3, add 10: [6,5,8,10] → min is 5
     * ...and so on
     * 
     * PHASE 3: APPROACH DISCUSSION (3-5 min)
     * 
     * Start with intuition:
     * "Since each element is at most k away from its sorted position,
     * the minimum element is guaranteed to be in the first k+1 elements."
     * 
     * Brute force:
     * "Could use standard sort O(n log n), but we can do better by
     * exploiting the k constraint."
     * 
     * Optimizations:
     * - Insertion sort: O(n*k) - good for small k
     * - Min-heap: O(n log k) - optimal for most cases
     * 
     * Choose approach:
     * "I'll use min-heap of size k+1. It's O(n log k) which beats
     * standard sorting when k << n."
     * 
     * PHASE 4: COMPLEXITY ANALYSIS (1-2 min)
     * - Time: O(n log k)
     *   - n elements to process
     *   - Each heap op is O(log k)
     * - Space: O(k) for the heap
     * - When k is small, much better than O(n log n)
     * 
     * PHASE 5: CODING (15-18 min)
     * - Write clean code with comments
     * - Handle edge cases
     * - Explain each step
     * 
     * PHASE 6: TESTING (3-5 min)
     * Test cases:
     * 1. Given example
     * 2. k = 0: [1,2,3,4], k=0 → [1,2,3,4]
     * 3. k = n-1: [4,3,2,1], k=3 → [1,2,3,4]
     * 4. Single element: [5], k=0 → [5]
     * 5. Duplicates: [3,3,2,2,1,1], k=2 → [1,1,2,2,3,3]
     * 
     * PHASE 7: OPTIMIZATION DISCUSSION (if time)
     * - Insertion sort for small k (< 10)
     * - When k ≈ n, use standard sort
     * - Space optimization techniques
     */

    // ========================================================================
    // KEY INSIGHTS & PATTERNS
    // ========================================================================
    /**
     * PATTERN RECOGNITION:
     * 
     * 1. SLIDING WINDOW WITH HEAP:
     *    - Window size k+1
     *    - Maintain min-heap for window
     *    - Extract min, add new element
     *    - Classic pattern for k-constrained problems
     * 
     * 2. HEAP SIZE CALCULATION:
     *    - "At most k away" means k+1 elements to consider
     *    - Element at sorted position i can be at indices [i, i+k]
     *    - So check k+1 elements starting from i
     * 
     * 3. SPACE-TIME TRADEOFF:
     *    - O(k) space vs O(1) space
     *    - O(n log k) time vs O(n*k) time
     *    - Choose based on k value
     * 
     * 4. WHEN TO USE WHICH APPROACH:
     *    - k < 10: Insertion sort (O(1) space, simple)
     *    - 10 ≤ k < n/2: Min-heap (O(k) space, optimal)
     *    - k ≥ n/2: Standard sort (heap doesn't help much)
     * 
     * 5. WHY NOT OTHER DATA STRUCTURES:
     *    - BST: O(k) space but O(log k) not guaranteed
     *    - Deque: Can't efficiently find minimum
     *    - Array: Need O(k) search for minimum
     *    - Heap: Perfect - O(1) min access, O(log k) insert/delete
     */

    // ========================================================================
    // RELATED PROBLEMS & VARIATIONS
    // ========================================================================
    /**
     * SIMILAR LEETCODE PROBLEMS:
     * 
     * 1. LeetCode 23 - Merge K Sorted Lists
     *    - Same heap technique
     *    - Merge sorted sequences
     * 
     * 2. LeetCode 347 - Top K Frequent Elements
     *    - Use heap of size k
     *    - Similar constraint exploitation
     * 
     * 3. LeetCode 658 - Find K Closest Elements
     *    - Sliding window + heap
     *    - K-constrained selection
     * 
     * 4. LeetCode 703 - Kth Largest Element in Stream
     *    - Maintain heap of size k
     *    - Dynamic k-constrained problem
     * 
     * 5. LeetCode 373 - Find K Pairs with Smallest Sums
     *    - Multi-way merge with heap
     * 
     * VARIATIONS:
     * 
     * 1. SORT IN DESCENDING ORDER:
     *    - Use max-heap instead of min-heap
     * 
     * 2. FIND KTH ELEMENT ONLY:
     *    - Don't need to sort entire array
     *    - Build heap, extract k times
     * 
     * 3. K-SORTED LINKED LIST:
     *    - Same approach but with linked list
     *    - More complex pointer management
     * 
     * 4. 2D K-SORTED MATRIX:
     *    - Each row and column k-sorted
     *    - Use heap with 2D coordinates
     * 
     * 5. SORT WITH DUPLICATES REMOVAL:
     *    - Use TreeSet instead of heap
     *    - Maintain unique elements only
     */

    // ========================================================================
    // COMPLEXITY COMPARISON TABLE
    // ========================================================================
    /**
     * ┌─────────────────────┬──────────────┬───────────┬────────────────┐
     * │ Approach            │ Time         │ Space     │ Best For       │
     * ├─────────────────────┼──────────────┼───────────┼────────────────┤
     * │ Min-Heap            │ O(n log k)   │ O(k)      │ k << n         │
     * │ Insertion Sort      │ O(n * k)     │ O(1)      │ k < 10         │
     * │ Binary Insertion    │ O(n * k)     │ O(1)      │ k small        │
     * │ Standard Sort       │ O(n log n)   │ O(1)      │ k ≈ n          │
     * │ Counting Sort       │ O(n + range) │ O(range)  │ Small range    │
     * └─────────────────────┴──────────────┴───────────┴────────────────┘
     * 
     * DECISION TREE:
     * 
     * Is k < 10?
     * └─ YES → Insertion Sort (simple, O(1) space)
     * └─ NO  → Is k < n/log(n)?
     *          └─ YES → Min-Heap (optimal O(n log k))
     *          └─ NO  → Standard Sort (heap doesn't help)
     * 
     * CROSSOVER POINTS:
     * - Heap vs Insertion: k ≈ 10
     * - Heap vs Standard: k ≈ n/log(n)
     * - For k = n/2: both O(n log n)
     */

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================
    
    // Verify if array is k-sorted
    public boolean isKSorted(int[] arr, int k) {
        int n = arr.length;
        int[] sorted = arr.clone();
        Arrays.sort(sorted);
        
        for (int i = 0; i < n; i++) {
            // Find arr[i] in sorted array
            int sortedPos = -1;
            for (int j = 0; j < n; j++) {
                if (sorted[j] == arr[i]) {
                    sortedPos = j;
                    break;
                }
            }
            
            // Check if distance is at most k
            if (Math.abs(sortedPos - i) > k) {
                return false;
            }
        }
        return true;
    }
    
    // Print array helper
    private void printArray(int[] arr, String label) {
        System.out.print(label + ": ");
        System.out.println(Arrays.toString(arr));
    }

    // ========================================================================
    // TEST CASES & VALIDATION
    // ========================================================================
    public static void main(String[] args) {
        SortKSortedArray solution = new SortKSortedArray();
        
        System.out.println("=".repeat(70));
        System.out.println("SORT K-SORTED ARRAY - COMPREHENSIVE TEST SUITE");
        System.out.println("=".repeat(70));
        
        // Test Case 1: Standard example
        int[] arr1 = {6, 5, 3, 2, 8, 10, 9};
        int k1 = 3;
        System.out.println("\nTest 1: Standard Example");
        System.out.println("Input:  " + Arrays.toString(arr1) + ", k = " + k1);
        solution.sortKSorted(arr1.clone(), k1);
        System.out.println("Output: " + Arrays.toString(arr1));
        System.out.println("Expected: [2, 3, 5, 6, 8, 9, 10]");
        
        // Test Case 2: k = 0 (already sorted)
        int[] arr2 = {1, 2, 3, 4, 5};
        int k2 = 0;
        System.out.println("\nTest 2: k = 0 (Already Sorted)");
        System.out.println("Input:  " + Arrays.toString(arr2) + ", k = " + k2);
        solution.sortKSorted(arr2, k2);
        System.out.println("Output: " + Arrays.toString(arr2));
        System.out.println("Expected: [1, 2, 3, 4, 5]");
        
        // Test Case 3: k = n-1 (completely unsorted)
        int[] arr3 = {5, 4, 3, 2, 1};
        int k3 = 4;
        System.out.println("\nTest 3: k = n-1 (Reverse Sorted)");
        System.out.println("Input:  " + Arrays.toString(arr3) + ", k = " + k3);
        solution.sortKSorted(arr3, k3);
        System.out.println("Output: " + Arrays.toString(arr3));
        System.out.println("Expected: [1, 2, 3, 4, 5]");
        
        // Test Case 4: With duplicates
        int[] arr4 = {3, 3, 2, 2, 1, 1};
        int k4 = 2;
        System.out.println("\nTest 4: With Duplicates");
        System.out.println("Input:  " + Arrays.toString(arr4) + ", k = " + k4);
        solution.sortKSorted(arr4, k4);
        System.out.println("Output: " + Arrays.toString(arr4));
        System.out.println("Expected: [1, 1, 2, 2, 3, 3]");
        
        // Test Case 5: Single element
        int[] arr5 = {42};
        int k5 = 0;
        System.out.println("\nTest 5: Single Element");
        System.out.println("Input:  " + Arrays.toString(arr5) + ", k = " + k5);
        solution.sortKSorted(arr5, k5);
        System.out.println("Output: " + Arrays.toString(arr5));
        System.out.println("Expected: [42]");
        
        // Test Case 6: k = 1 (only adjacent swaps)
        int[] arr6 = {2, 1, 4, 3, 6, 5};
        int k6 = 1;
        System.out.println("\nTest 6: k = 1 (Adjacent Swaps)");
        System.out.println("Input:  " + Arrays.toString(arr6) + ", k = " + k6);
        solution.sortKSorted(arr6, k6);
        System.out.println("Output: " + Arrays.toString(arr6));
        System.out.println("Expected: [1, 2, 3, 4, 5, 6]");
        
        // Test Case 7: Large k
        int[] arr7 = {10, 9, 8, 7, 4, 70, 60, 50};
        int k7 = 4;
        System.out.println("\nTest 7: Larger Array");
        System.out.println("Input:  " + Arrays.toString(arr7) + ", k = " + k7);
        solution.sortKSorted(arr7, k7);
        System.out.println("Output: " + Arrays.toString(arr7));
        System.out.println("Expected: [4, 7, 8, 9, 10, 50, 60, 70]");
        
        // Test Case 8: All same elements
        int[] arr8 = {5, 5, 5, 5, 5};
        int k8 = 2;
        System.out.println("\nTest 8: All Same Elements");
        System.out.println("Input:  " + Arrays.toString(arr8) + ", k = " + k8);
        solution.sortKSorted(arr8, k8);
        System.out.println("Output: " + Arrays.toString(arr8));
        System.out.println("Expected: [5, 5, 5, 5, 5]");
        
        // Compare approaches
        System.out.println("\n" + "=".repeat(70));
        System.out.println("COMPARING ALL APPROACHES");
        System.out.println("=".repeat(70));
        
        int[] testArr = {6, 5, 3, 2, 8, 10, 9};
        int testK = 3;
        
        int[] arr_heap = testArr.clone();
        int[] arr_insertion = testArr.clone();
        int[] arr_standard = testArr.clone();
        
        solution.sortKSorted(arr_heap, testK);
        solution.sortKSortedInsertion(arr_insertion, testK);
        solution.sortKSortedStandard(arr_standard, testK);
        
        System.out.println("Original:    " + Arrays.toString(testArr));
        System.out.println("Min-Heap:    " + Arrays.toString(arr_heap));
        System.out.println("Insertion:   " + Arrays.toString(arr_insertion));
        System.out.println("Standard:    " + Arrays.toString(arr_standard));
        System.out.println("All Match:   " + Arrays.equals(arr_heap, arr_insertion) && 
                          Arrays.equals(arr_insertion, arr_standard));
        
        // Performance comparison
        System.out.println("\n" + "=".repeat(70));
        System.out.println("PERFORMANCE COMPARISON (10000 elements, k=100)");
        System.out.println("=".repeat(70));
        
        int size = 10000;
        int k_perf = 100;
        Random rand = new Random(42);
        
        // Generate k-sorted array
        int[] perfArr = new int[size];
        for (int i = 0; i < size; i++) {
            perfArr[i] = i;
        }
        // Shuffle with k constraint
        for (int i = 0; i < size; i++) {
            int swapWith = i + rand.nextInt(Math.min(k_perf + 1, size - i));
            int temp = perfArr[i];
            perfArr[i] = perfArr[swapWith];
            perfArr[swapWith] = temp;
        }
        
        long start, end;
        
        int[] heapArr = perfArr.clone();
        start = System.nanoTime();
        solution.sortKSorted(heapArr, k_perf);
        end = System.nanoTime();
        System.out.println("Min-Heap:      " + (end - start) / 1_000_000 + " ms");
        
        int[] insertArr = perfArr.clone();
        start = System.nanoTime();
        solution.sortKSortedInsertion(insertArr, k_perf);
        end = System.nanoTime();
        System.out.println("Insertion:     " + (end - start) / 1_000_000 + " ms");
        
        int[] stdArr = perfArr.clone();
        start = System.nanoTime();
        solution.sortKSortedStandard(stdArr, k_perf);
        end = System.nanoTime();
        System.out.println("Standard Sort: " + (end - start) / 1_000_000 + " ms");
        
        System.out.println("\nAll produce same result: " + 
                          Arrays.equals(heapArr, insertArr) && 
                          Arrays.equals(insertArr, stdArr));
        
        System.out.println("\n" + "=".repeat(70));
    }
}

/**
 * ============================================================================
 * FINAL INTERVIEW CHECKLIST
 * ============================================================================
 * 
 * BEFORE CODING:
 * □ Clarified what "k positions away" means
 * □ Asked about k >= n case
 * □ Discussed space constraints
 * □ Confirmed handling of duplicates
 * □ Explained why heap size is k+1 not k
 * □ Stated time and space complexity
 * 
 * WHILE CODING:
 * □ Used min-heap (PriorityQueue)
 * □ Created heap of size k+1
 * □ Added first k+1 elements
 * □ Properly extracted and added elements
 * □ Drained remaining elements from heap
 * □ Handled edge cases (k=0, empty array, k>=n)
 * □ Used meaningful variable names
 * 
 * AFTER CODING:
 * □ Traced through example by hand
 * □ Tested k=0, k=1, k=n-1
 * □ Verified with duplicates
 * □ Confirmed time/space complexity
 * □ Discussed when to use insertion sort
 * □ Mentioned crossover points
 * 
 * KEY TALKING POINTS:
 * ✓ "Heap size is k+1 because element for position 0 could be at index k"
 * ✓ "This is O(n log k) which beats O(n log n) when k << n"
 * ✓ "At any point, min in heap is next element in sorted order"
 * ✓ "For small k, insertion sort might be simpler with O(1) space"
 * ✓ "When k ≈ n, might as well use standard sort"
 * 
 * COMMON PITFALLS AVOIDED:
 * ✗ Using heap size k instead of k+1
 * ✗ Not draining remaining heap elements
 * ✗ Using max-heap instead of min-heap
 * ✗ Not handling k >= n case
 * ✗ Off-by-one in array indices
 * 
 * ============================================================================
 */
