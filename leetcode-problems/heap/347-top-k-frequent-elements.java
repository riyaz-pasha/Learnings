import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * PROBLEM ANALYSIS & INTERVIEW APPROACH
 * =====================================
 * 
 * This is a TOP-K problem testing:
 * 1. Understanding of frequency counting
 * 2. Knowledge of heap data structures
 * 3. Understanding of bucket sort optimization
 * 4. Trade-offs between different approaches
 * 5. Ability to achieve optimal O(N) solution
 * 
 * KEY INSIGHTS:
 * -------------
 * 1. We need FREQUENCY information first
 *    - Use HashMap to count occurrences: O(N)
 * 
 * 2. We need TOP K by frequency
 *    - Multiple ways to solve this
 *    - Trade-offs between simplicity and optimality
 * 
 * 3. THE OPTIMAL INSIGHT: BUCKET SORT
 *    - Frequency range is limited: [1, N]
 *    - Create N buckets where bucket[i] = elements with frequency i
 *    - Iterate buckets from high to low frequency
 *    - This achieves O(N) time!
 * 
 * PROBLEM VARIATIONS:
 * -------------------
 * - "K most frequent" → This problem (frequency-based)
 * - "K largest" → Different problem (value-based)
 * - "K closest to target" → Yet another variant
 * 
 * Each needs different approach but similar techniques!
 * 
 * INTERVIEW STRATEGY:
 * -------------------
 * 1. Start with brute force
 *    "I could count frequencies, sort by frequency, take top K"
 *    Time: O(N log N) - acceptable but not optimal
 * 
 * 2. Optimize with heap
 *    "I can use a min heap of size K to track top K"
 *    Time: O(N log K) - better when K is small
 * 
 * 3. Achieve optimal with bucket sort
 *    "Since frequencies are bounded by N, I can use bucket sort"
 *    Time: O(N) - optimal!
 * 
 * 4. Discuss trade-offs
 *    - Heap: simpler code, good for small K
 *    - Bucket: optimal time, slightly more complex
 */

class Solution {
    
    /**
     * APPROACH 1: BUCKET SORT - OPTIMAL O(N) SOLUTION
     * ================================================
     * 
     * ALGORITHM:
     * ----------
     * 1. Count frequencies using HashMap: O(N)
     * 2. Create buckets where bucket[freq] = list of elements with that frequency
     * 3. Iterate from highest frequency to lowest
     * 4. Collect K elements
     * 
     * WHY BUCKET SORT WORKS:
     * ----------------------
     * - Maximum possible frequency = N (all elements same)
     * - Minimum possible frequency = 1
     * - So frequency range is [1, N] - limited and small!
     * - We can use array indexing instead of sorting
     * 
     * VISUALIZATION:
     * --------------
     * nums = [1,1,1,2,2,3], k = 2
     * 
     * Step 1: Count frequencies
     * {1: 3, 2: 2, 3: 1}
     * 
     * Step 2: Create buckets (index = frequency)
     * bucket[0] = []
     * bucket[1] = [3]       <- elements with frequency 1
     * bucket[2] = [2]       <- elements with frequency 2
     * bucket[3] = [1]       <- elements with frequency 3
     * 
     * Step 3: Iterate from high to low
     * Start at bucket[3]: add 1 → result = [1]
     * Move to bucket[2]: add 2 → result = [1, 2]
     * We have k=2 elements, done!
     * 
     * TIME COMPLEXITY: O(N)
     * - Counting: O(N)
     * - Building buckets: O(N) - each element added once
     * - Collecting results: O(N) worst case, O(K) average
     * Total: O(N)
     * 
     * SPACE COMPLEXITY: O(N)
     * - HashMap: O(N) for unique elements
     * - Buckets: O(N) total across all buckets
     * 
     * This is OPTIMAL - can't do better than O(N) since we must
     * read all elements at least once!
     */
    public int[] topKFrequentBucketSort(int[] nums, int k) {
        // STEP 1: Count frequency of each element
        // HashMap: element -> frequency
        Map<Integer, Integer> freqMap = new HashMap<>();
        for (int num : nums) {
            freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
        }
        
        // STEP 2: Create buckets
        // bucket[i] = list of elements with frequency i
        // We need nums.length + 1 buckets because frequency can be 1 to N
        List<Integer>[] bucket = new ArrayList[nums.length + 1];
        
        // Initialize buckets (can't do this inline in Java)
        for (int i = 0; i < bucket.length; i++) {
            bucket[i] = new ArrayList<>();
        }
        
        // Place each element in its frequency bucket
        for (Map.Entry<Integer, Integer> entry : freqMap.entrySet()) {
            int element = entry.getKey();
            int frequency = entry.getValue();
            bucket[frequency].add(element);
        }
        
        // STEP 3: Collect top K frequent elements
        // Iterate from highest frequency to lowest
        int[] result = new int[k];
        int index = 0;
        
        // Start from the highest possible frequency (nums.length)
        // and work backwards
        for (int freq = bucket.length - 1; freq >= 0 && index < k; freq--) {
            // Add all elements with this frequency
            for (int element : bucket[freq]) {
                result[index++] = element;
                if (index == k) {
                    return result; // Early exit when we have K elements
                }
            }
        }
        
        return result;
    }
    
    /**
     * APPROACH 2: MIN HEAP - O(N log K) SOLUTION
     * ===========================================
     * 
     * ALGORITHM:
     * ----------
     * 1. Count frequencies: O(N)
     * 2. Use min heap of size K
     * 3. For each unique element:
     *    - If heap size < K: add element
     *    - If element frequency > min heap root: remove root, add element
     * 4. Heap contains top K frequent elements
     * 
     * WHY MIN HEAP (not max heap)?
     * ----------------------------
     * Same reasoning as "Kth Largest Element" problem:
     * - Keep K most frequent elements
     * - Root = least frequent among these K
     * - When new element comes:
     *   - If more frequent than root: it deserves to be in top K
     *   - Replace root with new element
     * 
     * WHEN TO USE THIS:
     * -----------------
     * - K is much smaller than N (K << N)
     * - Want simpler code than bucket sort
     * - Okay with O(N log K) instead of O(N)
     * 
     * TIME COMPLEXITY: O(N log K)
     * - Counting: O(N)
     * - Heap operations: O(log K) each, done for each unique element
     * - Total: O(N + M log K) where M = unique elements ≤ N
     * - Simplifies to: O(N log K)
     * 
     * SPACE COMPLEXITY: O(N)
     * - HashMap: O(N)
     * - Heap: O(K)
     */
    public int[] topKFrequentHeap(int[] nums, int k) {
        // STEP 1: Count frequencies
        Map<Integer, Integer> freqMap = new HashMap<>();
        for (int num : nums) {
            freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
        }
        
        // STEP 2: Use min heap to keep top K frequent elements
        // Heap stores [element, frequency] pairs
        // Ordered by frequency (min heap on frequency)
        PriorityQueue<Map.Entry<Integer, Integer>> minHeap = 
            new PriorityQueue<>((a, b) -> a.getValue() - b.getValue());
        
        // Process each unique element
        for (Map.Entry<Integer, Integer> entry : freqMap.entrySet()) {
            minHeap.offer(entry);
            
            // Keep heap size at K
            if (minHeap.size() > k) {
                minHeap.poll(); // Remove least frequent
            }
        }
        
        // STEP 3: Extract elements from heap
        int[] result = new int[k];
        int index = 0;
        while (!minHeap.isEmpty()) {
            result[index++] = minHeap.poll().getKey();
        }
        
        return result;
    }
    
    /**
     * APPROACH 3: SORTING - O(N log N) SOLUTION
     * ==========================================
     * 
     * ALGORITHM:
     * ----------
     * 1. Count frequencies
     * 2. Sort elements by frequency (descending)
     * 3. Take first K elements
     * 
     * WHEN TO USE THIS:
     * -----------------
     * - Simplest code
     * - K is close to N (need most elements anyway)
     * - One-time computation (not repeated queries)
     * 
     * TIME COMPLEXITY: O(N log N)
     * - Counting: O(N)
     * - Sorting: O(M log M) where M = unique elements ≤ N
     * - In worst case (all unique): O(N log N)
     * 
     * SPACE COMPLEXITY: O(N)
     */
    public int[] topKFrequentSort(int[] nums, int k) {
        // STEP 1: Count frequencies
        Map<Integer, Integer> freqMap = new HashMap<>();
        for (int num : nums) {
            freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
        }
        
        // STEP 2: Create list of entries and sort by frequency
        List<Map.Entry<Integer, Integer>> entries = 
            new ArrayList<>(freqMap.entrySet());
        
        // Sort in descending order of frequency
        entries.sort((a, b) -> b.getValue() - a.getValue());
        
        // STEP 3: Take top K elements
        int[] result = new int[k];
        for (int i = 0; i < k; i++) {
            result[i] = entries.get(i).getKey();
        }
        
        return result;
    }
    
    /**
     * APPROACH 4: QUICKSELECT - O(N) AVERAGE CASE
     * ============================================
     * 
     * ALGORITHM:
     * ----------
     * Use quickselect to find the Kth most frequent element
     * Then return all elements with frequency ≥ that threshold
     * 
     * TIME COMPLEXITY:
     * - Average: O(N) - quickselect average case
     * - Worst: O(N²) - quickselect worst case
     * 
     * In practice, bucket sort is simpler and guaranteed O(N)
     * 
     * This approach is mentioned for completeness but not implemented
     * as bucket sort is superior for this specific problem.
     */
}

/**
 * COMPLEXITY COMPARISON
 * =====================
 * 
 * Approach          | Time       | Space | When to Use
 * ------------------|------------|-------|---------------------------
 * Bucket Sort       | O(N)       | O(N)  | Best overall, optimal time
 * Min Heap          | O(N log K) | O(N)  | When K << N, simpler code
 * Sorting           | O(N log N) | O(N)  | Simplest code, K ≈ N
 * QuickSelect       | O(N) avg   | O(N)  | Average case optimal
 * 
 * RECOMMENDATION:
 * - Interview: Start with heap, optimize to bucket sort if asked
 * - Production: Bucket sort for guaranteed O(N)
 * - Quick solve: Sorting for simplicity
 */

/**
 * EDGE CASES & TESTING
 * =====================
 * 
 * Test Cases:
 * 
 * 1. Single element
 *    nums = [1], k = 1
 *    Output: [1]
 * 
 * 2. All same element
 *    nums = [1,1,1,1,1], k = 1
 *    Output: [1]
 * 
 * 3. All different elements (k = nums.length)
 *    nums = [1,2,3,4,5], k = 5
 *    Output: [1,2,3,4,5] (any order)
 * 
 * 4. k = 1 (most frequent only)
 *    nums = [1,1,1,2,2,3], k = 1
 *    Output: [1]
 * 
 * 5. Multiple elements same frequency
 *    nums = [1,2], k = 2
 *    Output: [1,2] or [2,1]
 * 
 * 6. Negative numbers
 *    nums = [-1,-1,2,2,3], k = 2
 *    Output: [-1,2] or [2,-1]
 * 
 * 7. Large numbers
 *    nums = [100000, 100000, 99999], k = 1
 *    Output: [100000]
 */

/**
 * RELATED PROBLEMS & PATTERNS
 * ============================
 * 
 * This problem is part of the "TOP K" pattern family:
 * 
 * 1. Kth Largest Element
 *    - Use min heap of size K
 *    - Similar to approach 2 here
 * 
 * 2. Top K Frequent Words
 *    - Same as this problem but with lexicographic ordering
 *    - Need custom comparator
 * 
 * 3. K Closest Points to Origin
 *    - Calculate distances, find top K smallest
 *    - Max heap or quickselect
 * 
 * 4. Kth Largest Element in Stream
 *    - Maintain min heap dynamically
 *    - Related data structure problem
 * 
 * 5. Find K Pairs with Smallest Sums
 *    - Use min heap with clever merging
 *    - More complex variant
 * 
 * GENERAL TOP-K STRATEGY:
 * -----------------------
 * 1. If range is bounded and small: Use bucket/counting sort
 * 2. If K << N: Use heap
 * 3. If need guaranteed O(N): Use quickselect or bucket sort
 * 4. If simplicity matters: Sort and take top K
 */

// Comprehensive test suite
class Main {
    public static void main(String[] args) {
        Solution sol = new Solution();
        
        System.out.println("=== Test Case 1: Basic Example ===");
        int[] nums1 = {1, 1, 1, 2, 2, 3};
        int k1 = 2;
        System.out.println("Input: " + Arrays.toString(nums1) + ", k = " + k1);
        System.out.println("Bucket Sort: " + Arrays.toString(sol.topKFrequentBucketSort(nums1, k1)));
        System.out.println("Heap: " + Arrays.toString(sol.topKFrequentHeap(nums1, k1)));
        System.out.println("Sort: " + Arrays.toString(sol.topKFrequentSort(nums1, k1)));
        
        System.out.println("\n=== Test Case 2: Single Element ===");
        int[] nums2 = {1};
        int k2 = 1;
        System.out.println("Input: " + Arrays.toString(nums2) + ", k = " + k2);
        System.out.println("Bucket Sort: " + Arrays.toString(sol.topKFrequentBucketSort(nums2, k2)));
        
        System.out.println("\n=== Test Case 3: Multiple Same Frequency ===");
        int[] nums3 = {1, 2, 1, 2, 1, 2, 3, 1, 3, 2};
        int k3 = 2;
        System.out.println("Input: " + Arrays.toString(nums3) + ", k = " + k3);
        System.out.println("Bucket Sort: " + Arrays.toString(sol.topKFrequentBucketSort(nums3, k3)));
        System.out.println("Expected: [1, 2] (both have frequency 4 and 4)");
        
        System.out.println("\n=== Test Case 4: All Same Element ===");
        int[] nums4 = {5, 5, 5, 5, 5};
        int k4 = 1;
        System.out.println("Input: " + Arrays.toString(nums4) + ", k = " + k4);
        System.out.println("Bucket Sort: " + Arrays.toString(sol.topKFrequentBucketSort(nums4, k4)));
        
        System.out.println("\n=== Test Case 5: Negative Numbers ===");
        int[] nums5 = {-1, -1, -1, 2, 2, 3};
        int k5 = 2;
        System.out.println("Input: " + Arrays.toString(nums5) + ", k = " + k5);
        System.out.println("Bucket Sort: " + Arrays.toString(sol.topKFrequentBucketSort(nums5, k5)));
        
        System.out.println("\n=== Test Case 6: All Different Elements ===");
        int[] nums6 = {1, 2, 3, 4, 5};
        int k6 = 3;
        System.out.println("Input: " + Arrays.toString(nums6) + ", k = " + k6);
        System.out.println("Bucket Sort: " + Arrays.toString(sol.topKFrequentBucketSort(nums6, k6)));
        System.out.println("Note: All have frequency 1, any 3 elements are valid");
        
        // Performance comparison
        System.out.println("\n=== Performance Test ===");
        int[] largeNums = new int[10000];
        for (int i = 0; i < largeNums.length; i++) {
            largeNums[i] = i % 100; // Create frequency pattern
        }
        
        long start = System.nanoTime();
        sol.topKFrequentBucketSort(largeNums, 10);
        long bucketTime = System.nanoTime() - start;
        
        start = System.nanoTime();
        sol.topKFrequentHeap(largeNums, 10);
        long heapTime = System.nanoTime() - start;
        
        start = System.nanoTime();
        sol.topKFrequentSort(largeNums, 10);
        long sortTime = System.nanoTime() - start;
        
        System.out.println("Array size: 10000, k = 10");
        System.out.println("Bucket Sort: " + bucketTime / 1000 + " μs");
        System.out.println("Heap: " + heapTime / 1000 + " μs");
        System.out.println("Sort: " + sortTime / 1000 + " μs");
    }
}

/**
 * INTERVIEW TALKING POINTS
 * =========================
 * 
 * When presenting this solution in an interview:
 * 
 * 1. "First, I need to count the frequency of each element using a HashMap.
 *    This takes O(N) time."
 * 
 * 2. "Now I need to find the top K by frequency. I have several approaches:
 *    - Sort by frequency: O(N log N)
 *    - Use a heap: O(N log K)
 *    - Use bucket sort: O(N)"
 * 
 * 3. "Let me explain the bucket sort approach since it's optimal.
 *    The key insight is that frequency is bounded by N..."
 *    [Draw diagram showing buckets]
 * 
 * 4. "I'll create buckets indexed by frequency. Then iterate from highest
 *    to lowest frequency, collecting K elements."
 * 
 * 5. "The time complexity is O(N) which is optimal - we can't do better
 *    since we must at least read all elements once."
 * 
 * 6. "If you prefer simpler code or K is very small, the heap approach
 *    with O(N log K) is also excellent and easier to implement."
 * 
 * 7. "Edge cases to consider: single element, all same frequency,
 *    negative numbers, and ensuring we handle the order correctly
 *    even though any order is valid."
 * 
 * 8. "For follow-up: If we need to handle streaming data with updates,
 *    I'd maintain the frequency map and rebuild the structure on query.
 *    For frequent queries, I'd consider caching with invalidation."
 */
