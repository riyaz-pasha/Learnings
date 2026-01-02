import java.util.PriorityQueue;

/**
 * PROBLEM ANALYSIS & INTERVIEW APPROACH
 * =====================================
 * 
 * This is a STREAMING problem testing:
 * 1. Understanding of heap data structures
 * 2. Ability to maintain a rolling "top k" elements
 * 3. Trade-offs between different approaches
 * 4. Knowledge of when to use min-heap vs max-heap
 * 
 * KEY INSIGHTS:
 * -------------
 * 1. We need the Kth LARGEST element, not the K largest elements
 *    - Once we have K elements, we only care about keeping the top K
 *    - Any element smaller than the Kth largest is irrelevant
 * 
 * 2. MIN HEAP is the perfect data structure (counter-intuitive!)
 *    - Keep exactly K elements in the heap
 *    - The ROOT (minimum) of this heap IS the Kth largest element
 *    - Why? Because we're keeping only the K largest elements
 *    - The smallest among these K largest is the Kth largest overall
 * 
 * 3. When adding a new element:
 *    - If heap has < K elements: just add it
 *    - If heap has K elements and new element > root: remove root, add new
 *    - If heap has K elements and new element ≤ root: ignore it
 * 
 * EXAMPLE WALKTHROUGH:
 * --------------------
 * k = 3, nums = [4, 5, 8, 2]
 * 
 * Build heap: [4, 5, 8, 2]
 * After sorting: [2, 4, 5, 8]
 * We want 3rd largest = 5
 * 
 * Min heap with k=3 elements: [4, 5, 8]
 * Root = 4? No, after heapify: [4, 8, 5] -> min heap property: [4, 5, 8]
 * Wait, in min heap the smallest is at root: root = 4
 * But we want 3rd largest = 5
 * 
 * Let's reconsider: sorted [2, 4, 5, 8]
 * 3rd largest from right = 5
 * Top 3 elements = [5, 8, 4] (in any order)
 * In a min heap of these: root = 4 is smallest
 * Wait, that's 2nd largest, not 3rd...
 * 
 * Actually: sorted [2, 4, 5, 8]
 * 1st largest = 8
 * 2nd largest = 5  
 * 3rd largest = 4  <- This is what we want!
 * 
 * So keep top k elements [4, 5, 8]
 * Min heap root = 4 ✓ Correct!
 * 
 * INTERVIEW STRATEGY:
 * -------------------
 * 1. Clarify the problem
 *    - "Kth largest" means Kth from the top when sorted descending
 *    - NOT "Kth smallest" or "Kth from the bottom"
 *    - Can there be duplicates? (Yes)
 *    - What if fewer than k elements? (Problem guarantees at least k)
 * 
 * 2. Discuss naive approaches first
 *    - Sort array every time: O(N log N) per add
 *    - Keep sorted list: O(N) per add for insertion
 *    - These are too slow for streaming
 * 
 * 3. Introduce heap solution
 *    - Explain why min heap (not max heap)
 *    - Draw a diagram showing how heap maintains invariant
 *    - Walk through time complexity
 * 
 * 4. Optimize initialization
 *    - Could add elements one by one: O(N log K)
 *    - Better: heapify in O(N), then trim to K
 * 
 * COMMON MISTAKES:
 * ----------------
 * 1. Using max heap instead of min heap
 *    - Max heap would give us the largest element, not Kth largest
 * 
 * 2. Keeping all elements in heap
 *    - Wastes space and time
 *    - Heap size should be exactly K
 * 
 * 3. Off-by-one errors with K
 *    - Remember: Kth largest means K elements are ≥ it
 *    - Sorted descending: arr[k-1] (0-indexed)
 */

class KthLargest {
    
    /**
     * WHY MIN HEAP?
     * =============
     * 
     * Intuition: We maintain exactly K largest elements seen so far.
     * The smallest among these K largest elements IS the Kth largest overall.
     * 
     * Min heap property: root = minimum element in the heap
     * If we keep only the K largest elements, root = Kth largest element
     * 
     * Visual Example (k=3):
     * All elements: [2, 4, 5, 8, 10]
     * Top 3: [10, 8, 5]
     * Min heap of top 3:
     *       5
     *      / \
     *     8   10
     * 
     * Root = 5 = 3rd largest ✓
     * 
     * When new element comes:
     * - If < 5: ignore (not in top 3)
     * - If ≥ 5: remove 5, add new, maintain top 3
     */
    
    private PriorityQueue<Integer> minHeap;  // Min heap to store k largest elements
    private int k;                            // The k value (kth largest)
    
    /**
     * Constructor: Initialize with k and initial stream of scores
     * 
     * TIME COMPLEXITY:
     * ---------------
     * Approach 1 (Used here): O(N log K)
     * - Add each element: O(log K) per element
     * - N elements: O(N log K)
     * 
     * Approach 2 (Heapify optimization): O(N + K log N)
     * - Build heap from all N elements: O(N)
     * - Remove smallest N-K elements: O((N-K) log N)
     * - For large N and small K: O(N)
     * - For small N or large K: first approach is better
     * 
     * SPACE: O(K) - heap stores at most K elements
     * 
     * @param k The position of the largest element to track
     * @param nums Initial stream of test scores
     */
    public KthLargest(int k, int[] nums) {
        this.k = k;
        // Min heap in Java - PriorityQueue is min heap by default
        this.minHeap = new PriorityQueue<>();
        
        // Add all initial elements
        // This maintains the invariant: heap size ≤ k, contains k largest elements
        for (int num : nums) {
            add(num);
        }
    }
    
    /**
     * ALTERNATIVE INITIALIZATION (More optimal for large N, small K)
     * 
     * This would be better when N >> K:
     * 
     * public KthLargest(int k, int[] nums) {
     *     this.k = k;
     *     this.minHeap = new PriorityQueue<>();
     *     
     *     // Build heap with all elements - O(N)
     *     for (int num : nums) {
     *         minHeap.offer(num);
     *     }
     *     
     *     // Keep only top k elements - O((N-K) log N)
     *     while (minHeap.size() > k) {
     *         minHeap.poll();
     *     }
     * }
     * 
     * Time: O(N + (N-K) log N) ≈ O(N) when K is small
     * But for typical cases, the current approach is simpler and sufficient.
     */
    
    /**
     * Add a new score and return the kth largest score
     * 
     * ALGORITHM:
     * ----------
     * 1. If heap size < k: add the element (we need k elements)
     * 2. If new element > heap root: remove root, add new element
     * 3. Otherwise: ignore (new element is not in top k)
     * 4. Return heap root (the kth largest element)
     * 
     * TIME COMPLEXITY: O(log K)
     * - offer(): O(log K) to maintain heap property
     * - poll(): O(log K) to maintain heap property
     * - peek(): O(1) to get root
     * 
     * SPACE COMPLEXITY: O(1)
     * - No additional space beyond the existing heap
     * 
     * WHY THIS WORKS:
     * ---------------
     * Invariant maintained: heap always contains exactly k largest elements
     * 
     * Case 1: heap.size() < k
     *   - We don't have k elements yet, so add this one
     * 
     * Case 2: heap.size() == k and val > heap.peek()
     *   - Current root is smallest of top k elements
     *   - New val is larger, so it's in top k
     *   - Remove current root (no longer in top k)
     *   - Add new val (now in top k)
     * 
     * Case 3: heap.size() == k and val ≤ heap.peek()
     *   - New val is not larger than kth largest
     *   - So it's not in top k
     *   - Don't add it (would violate invariant)
     * 
     * @param val New test score to add
     * @return The kth largest score after adding val
     */
    public int add(int val) {
        // CASE 1: Heap has fewer than k elements
        // Add the element because we need to fill up to k elements
        if (minHeap.size() < k) {
            minHeap.offer(val);
        }
        // CASE 2: Heap has k elements, and new value is larger than root
        // Root is the smallest of our k largest elements
        // New value deserves to be in top k, so replace root
        else if (val > minHeap.peek()) {
            minHeap.poll();      // Remove smallest of top k
            minHeap.offer(val);  // Add new element to top k
        }
        // CASE 3: val ≤ minHeap.peek()
        // New value is not in top k, so ignore it
        // No action needed
        
        // Root of min heap = smallest of k largest = kth largest
        return minHeap.peek();
    }
    
    /**
     * COMPLEXITY SUMMARY
     * ==================
     * Constructor: O(N log K) time, O(K) space
     * add():       O(log K) time, O(1) space
     * 
     * Where:
     * - N = number of initial elements
     * - K = the k value (position of largest to track)
     * 
     * Why this is efficient:
     * - Heap size is bounded by k (not by total number of elements)
     * - Each operation is logarithmic in k, not in total elements
     * - For streaming data with millions of elements but small k, this is optimal
     */
}

/**
 * ALTERNATIVE APPROACHES & COMPARISONS
 * =====================================
 * 
 * 1. SORTING APPROACH
 *    -----------------
 *    class KthLargest {
 *        List<Integer> scores;
 *        int k;
 *        
 *        public KthLargest(int k, int[] nums) {
 *            this.k = k;
 *            scores = new ArrayList<>();
 *            for (int n : nums) scores.add(n);
 *        }
 *        
 *        public int add(int val) {
 *            scores.add(val);
 *            Collections.sort(scores, Collections.reverseOrder());
 *            return scores.get(k - 1);
 *        }
 *    }
 *    
 *    Time: O(N log N) per add - TOO SLOW for streaming
 *    Space: O(N) - stores all elements
 * 
 * 2. BINARY SEARCH WITH SORTED LIST
 *    -------------------------------
 *    - Maintain sorted list
 *    - Use binary search to find insertion position
 *    - Insert element at correct position
 *    
 *    Time: O(N) per add (shifting elements after insertion)
 *    Space: O(N)
 *    Better than full sort but still not optimal
 * 
 * 3. MAX HEAP (WRONG APPROACH)
 *    --------------------------
 *    Why this doesn't work:
 *    - Max heap gives us the largest element (1st largest)
 *    - To get kth largest, we'd need to poll k-1 times
 *    - This modifies the heap! Not reversible efficiently
 *    - Would need to rebuild heap each time: O(K log N)
 * 
 * 4. BALANCED BST (TreeMap)
 *    -----------------------
 *    - Use TreeMap to maintain sorted order with counts
 *    - Track which element is kth largest
 *    
 *    Time: O(log N) per add
 *    Space: O(N)
 *    Good solution but more complex and uses more space than heap
 * 
 * 5. QUICKSELECT (Not suitable)
 *    ---------------------------
 *    - O(N) average case to find kth largest in array
 *    - But needs to run on entire array each time
 *    - Not efficient for streaming
 * 
 * WINNER: Min Heap of size K
 * - O(log K) per operation (best for streaming)
 * - O(K) space (minimal)
 * - Simple to implement and understand
 */

/**
 * EDGE CASES & TESTING
 * =====================
 * 
 * Test Cases to Consider:
 * 
 * 1. Minimum case: k = 1 (tracking maximum)
 *    - Heap should always have 1 element (the max seen so far)
 * 
 * 2. All elements initially < first add
 *    - KthLargest(3, [1, 2, 3])
 *    - add(10) -> should return 3
 * 
 * 3. All elements initially > first add
 *    - KthLargest(3, [10, 9, 8])
 *    - add(1) -> should return 8 (ignore the 1)
 * 
 * 4. Duplicate values
 *    - KthLargest(2, [5, 5, 5])
 *    - add(5) -> should return 5
 * 
 * 5. Negative numbers
 *    - KthLargest(2, [-1, -2, -3])
 *    - add(0) -> should return -1
 * 
 * 6. k equals initial array size
 *    - KthLargest(3, [4, 5, 8])
 *    - add(2) -> should return 4
 * 
 * 7. Many additions
 *    - Verify heap maintains correct size
 *    - Verify heap property is maintained
 */

/**
 * REAL-WORLD APPLICATIONS
 * ========================
 * 
 * 1. University Admissions (as in problem)
 *    - Track cutoff scores dynamically
 *    - Determine interview thresholds
 * 
 * 2. Stock Trading
 *    - Track kth highest bid price
 *    - Determine market depth
 * 
 * 3. Gaming Leaderboards
 *    - Track position thresholds (e.g., top 100)
 *    - Real-time ranking updates
 * 
 * 4. Service Level Monitoring
 *    - Track kth percentile latency
 *    - Alert on performance degradation
 * 
 * 5. Resource Allocation
 *    - Track kth highest resource usage
 *    - Trigger scaling decisions
 */

// Example usage and comprehensive test
class Main {
    public static void main(String[] args) {
        System.out.println("=== Test Case 1: Basic Example ===");
        KthLargest kthLargest1 = new KthLargest(3, new int[]{4, 5, 8, 2});
        System.out.println("Initial state - 3rd largest should be: 4");
        System.out.println("add(3) -> " + kthLargest1.add(3));  // returns 4
        System.out.println("add(5) -> " + kthLargest1.add(5));  // returns 5
        System.out.println("add(10) -> " + kthLargest1.add(10)); // returns 5
        System.out.println("add(9) -> " + kthLargest1.add(9));  // returns 8
        System.out.println("add(4) -> " + kthLargest1.add(4));  // returns 8
        
        System.out.println("\n=== Test Case 2: k=1 (Track Maximum) ===");
        KthLargest kthLargest2 = new KthLargest(1, new int[]{1, 2, 3});
        System.out.println("add(4) -> " + kthLargest2.add(4));  // returns 4
        System.out.println("add(5) -> " + kthLargest2.add(5));  // returns 5
        System.out.println("add(3) -> " + kthLargest2.add(3));  // returns 5
        
        System.out.println("\n=== Test Case 3: Duplicates ===");
        KthLargest kthLargest3 = new KthLargest(2, new int[]{5, 5, 5, 5});
        System.out.println("add(5) -> " + kthLargest3.add(5));  // returns 5
        System.out.println("add(6) -> " + kthLargest3.add(6));  // returns 5
        System.out.println("add(7) -> " + kthLargest3.add(7));  // returns 6
        
        System.out.println("\n=== Test Case 4: Negative Numbers ===");
        KthLargest kthLargest4 = new KthLargest(2, new int[]{-1, -2, -3});
        System.out.println("add(0) -> " + kthLargest4.add(0));   // returns -1
        System.out.println("add(-4) -> " + kthLargest4.add(-4)); // returns -1
        System.out.println("add(1) -> " + kthLargest4.add(1));   // returns 0
    }
}

/**
 * INTERVIEW TALKING POINTS
 * =========================
 * 
 * When presenting this solution in an interview:
 * 
 * 1. "I'll use a min heap of size k to track the k largest elements.
 *    The key insight is that the root of this min heap will always
 *    be the kth largest element overall."
 * 
 * 2. "Let me walk through why this works with an example..."
 *    [Draw diagram on whiteboard]
 * 
 * 3. "The time complexity is O(log k) per add operation, which is
 *    optimal for streaming data. Space is O(k), not O(n)."
 * 
 * 4. "I considered using a max heap, but that would give us the
 *    largest element, not the kth largest. We'd need to poll k-1
 *    times each time, which isn't efficient."
 * 
 * 5. "For initialization, I'm adding elements one at a time which
 *    is O(n log k). For very large initial arrays, we could optimize
 *    to O(n) by heapifying all elements then removing the smallest."
 * 
 * 6. "Edge cases to consider: k=1, duplicates, negative numbers,
 *    and ensuring we handle the case where we have fewer than k
 *    elements initially."
 */
