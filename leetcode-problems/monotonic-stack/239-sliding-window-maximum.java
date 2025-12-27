/**
 * Sliding Window Maximum - Complete Solutions
 * 
 * Problem: Given an array and a sliding window of size k, return the maximum
 * value in each window as it slides from left to right.
 * 
 * Key Insights:
 * 1. Need to efficiently find maximum in each window of size k
 * 2. Use monotonic decreasing deque to maintain potential maximums
 * 3. Deque stores indices, front always has index of current maximum
 * 4. Remove elements outside current window and smaller than current element
 * 
 * Core Strategy:
 * - Maintain deque in decreasing order of values
 * - Front of deque is always the maximum for current window
 * - Remove indices outside window from front
 * - Remove smaller values from back (they can never be maximum)
 */

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.PriorityQueue;

class SlidingWindowMaximum {
    
    // ========================================================================
    // SOLUTION 1: MONOTONIC DEQUE (OPTIMAL)
    // Time Complexity: O(n)
    // Space Complexity: O(k)
    // ========================================================================
    
    /**
     * Monotonic Decreasing Deque Approach
     * 
     * Strategy:
     * 1. Use deque to store indices of elements in decreasing order of values
     * 2. For each element:
     *    a. Remove indices outside current window from front
     *    b. Remove indices with smaller values from back
     *    c. Add current index to back
     *    d. Front of deque is maximum for current window
     * 
     * Why Deque:
     * - Need to remove from both ends (front: old elements, back: smaller elements)
     * - Front always contains index of maximum element in current window
     * 
     * Monotonic Property:
     * - Values at indices in deque are in DECREASING order
     * - If nums[i] >= nums[j] and i > j, we can remove j (it will never be max)
     * 
     * Example: nums = [1,3,-1,-3,5,3,6,7], k = 3
     * 
     * Window [1,3,-1]:
     *   - Process 1: deque=[0]
     *   - Process 3: 3>1, remove 0, deque=[1]
     *   - Process -1: -1<3, deque=[1,2]
     *   - Max = nums[1] = 3
     * 
     * Window [3,-1,-3]:
     *   - Process -3: deque=[1,2,3]
     *   - Remove index 0 (out of window): deque=[1,2,3]
     *   - Max = nums[1] = 3
     * 
     * Window [-1,-3,5]:
     *   - Process 5: 5>all, remove all, deque=[4]
     *   - Remove index 1 (out of window)
     *   - Max = nums[4] = 5
     */
    public static int[] maxSlidingWindow(int[] nums, int k) {
        if (nums == null || nums.length == 0 || k <= 0) {
            return new int[0];
        }
        
        int n = nums.length;
        int[] result = new int[n - k + 1];
        Deque<Integer> deque = new LinkedList<>(); // stores indices
        
        for (int i = 0; i < n; i++) {
            // Remove indices outside current window from front
            while (!deque.isEmpty() && deque.peekFirst() < i - k + 1) {
                deque.pollFirst();
            }
            
            // Remove indices with smaller values from back
            // These can never be maximum in any future window
            while (!deque.isEmpty() && nums[deque.peekLast()] < nums[i]) {
                deque.pollLast();
            }
            
            // Add current index
            deque.offerLast(i);
            
            // Record maximum for current window (if window is complete)
            if (i >= k - 1) {
                result[i - k + 1] = nums[deque.peekFirst()];
            }
        }
        
        return result;
    }
    
    // ========================================================================
    // SOLUTION 2: PRIORITY QUEUE (HEAP)
    // Time Complexity: O(n log n)
    // Space Complexity: O(n)
    // ========================================================================
    
    /**
     * Max Heap Approach
     * 
     * Strategy:
     * - Use max heap to track elements in current window
     * - Store (value, index) pairs
     * - Remove outdated elements from heap
     * 
     * Note: This is less efficient than deque but demonstrates heap usage
     */
    public static int[] maxSlidingWindowHeap(int[] nums, int k) {
        if (nums == null || nums.length == 0 || k <= 0) {
            return new int[0];
        }
        
        int n = nums.length;
        int[] result = new int[n - k + 1];
        
        // Max heap: (value, index)
        PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> b[0] - a[0]);
        
        for (int i = 0; i < n; i++) {
            // Add current element
            heap.offer(new int[]{nums[i], i});
            
            // Remove elements outside current window
            while (!heap.isEmpty() && heap.peek()[1] < i - k + 1) {
                heap.poll();
            }
            
            // Record maximum for current window
            if (i >= k - 1) {
                result[i - k + 1] = heap.peek()[0];
            }
        }
        
        return result;
    }
    
    // ========================================================================
    // SOLUTION 3: BRUTE FORCE
    // Time Complexity: O(n * k)
    // Space Complexity: O(1)
    // ========================================================================
    
    /**
     * Brute Force Approach
     * 
     * For each window, scan all k elements to find maximum.
     * Simple but inefficient.
     */
    public static int[] maxSlidingWindowBruteForce(int[] nums, int k) {
        if (nums == null || nums.length == 0 || k <= 0) {
            return new int[0];
        }
        
        int n = nums.length;
        int[] result = new int[n - k + 1];
        
        for (int i = 0; i <= n - k; i++) {
            int max = nums[i];
            for (int j = i; j < i + k; j++) {
                max = Math.max(max, nums[j]);
            }
            result[i] = max;
        }
        
        return result;
    }
    
    // ========================================================================
    // SOLUTION 4: DYNAMIC PROGRAMMING (BLOCK-BASED)
    // Time Complexity: O(n)
    // Space Complexity: O(n)
    // ========================================================================
    
    /**
     * Block-Based DP Approach
     * 
     * Strategy:
     * - Divide array into blocks of size k
     * - For each block, precompute:
     *   • left[i]: max from block start to i
     *   • right[i]: max from i to block end
     * - Maximum in window [i, i+k-1] = max(right[i], left[i+k-1])
     * 
     * This is clever but deque is simpler and equally efficient.
     */
    public static int[] maxSlidingWindowDP(int[] nums, int k) {
        if (nums == null || nums.length == 0 || k <= 0) {
            return new int[0];
        }
        
        int n = nums.length;
        int[] left = new int[n];
        int[] right = new int[n];
        
        // Build left array (max from block start to i)
        for (int i = 0; i < n; i++) {
            if (i % k == 0) {
                left[i] = nums[i];
            } else {
                left[i] = Math.max(left[i - 1], nums[i]);
            }
        }
        
        // Build right array (max from i to block end)
        for (int i = n - 1; i >= 0; i--) {
            if (i == n - 1 || (i + 1) % k == 0) {
                right[i] = nums[i];
            } else {
                right[i] = Math.max(right[i + 1], nums[i]);
            }
        }
        
        // Calculate result
        int[] result = new int[n - k + 1];
        for (int i = 0; i <= n - k; i++) {
            result[i] = Math.max(right[i], left[i + k - 1]);
        }
        
        return result;
    }
    
    // ========================================================================
    // SOLUTION 5: VERBOSE VERSION WITH DETAILED TRACKING
    // Time Complexity: O(n)
    // Space Complexity: O(k)
    // ========================================================================
    
    /**
     * Verbose Deque Solution for Understanding
     * 
     * Shows step-by-step deque operations.
     */
    public static int[] maxSlidingWindowVerbose(int[] nums, int k, boolean debug) {
        if (nums == null || nums.length == 0 || k <= 0) {
            return new int[0];
        }
        
        if (debug) {
            System.out.println("\n=== Processing: nums = " + Arrays.toString(nums) + 
                             ", k = " + k + " ===");
        }
        
        int n = nums.length;
        int[] result = new int[n - k + 1];
        Deque<Integer> deque = new LinkedList<>();
        
        for (int i = 0; i < n; i++) {
            if (debug) {
                System.out.println("\n--- Processing index " + i + ", value = " + nums[i] + " ---");
                System.out.println("Deque before: " + dequeToString(deque, nums));
            }
            
            // Remove outdated indices
            while (!deque.isEmpty() && deque.peekFirst() < i - k + 1) {
                int removed = deque.pollFirst();
                if (debug) {
                    System.out.println("  Removed outdated index " + removed + 
                                     " (outside window)");
                }
            }
            
            // Remove smaller elements
            while (!deque.isEmpty() && nums[deque.peekLast()] < nums[i]) {
                int removed = deque.pollLast();
                if (debug) {
                    System.out.println("  Removed index " + removed + 
                                     " (value " + nums[removed] + " < " + nums[i] + ")");
                }
            }
            
            deque.offerLast(i);
            if (debug) {
                System.out.println("  Added index " + i);
                System.out.println("Deque after: " + dequeToString(deque, nums));
            }
            
            // Record result
            if (i >= k - 1) {
                result[i - k + 1] = nums[deque.peekFirst()];
                if (debug) {
                    System.out.println("Window [" + (i - k + 1) + "," + i + "]: max = " + 
                                     result[i - k + 1] + 
                                     " (from index " + deque.peekFirst() + ")");
                }
            }
        }
        
        return result;
    }
    
    private static String dequeToString(Deque<Integer> deque, int[] nums) {
        if (deque.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int idx : deque) {
            sb.append(idx).append("(").append(nums[idx]).append("), ");
        }
        sb.setLength(sb.length() - 2);
        sb.append("]");
        return sb.toString();
    }
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    /**
     * Visualize the sliding window process
     */
    public static void visualizeSlidingWindow(int[] nums, int k) {
        System.out.println("\nSliding Window Visualization:");
        System.out.println("Array: " + Arrays.toString(nums));
        System.out.println("Window size k = " + k);
        System.out.println("\nWindow positions:");
        
        for (int i = 0; i <= nums.length - k; i++) {
            // Print array with window highlighted
            System.out.print("  ");
            for (int j = 0; j < nums.length; j++) {
                if (j == i) System.out.print("[");
                System.out.printf("%2d", nums[j]);
                if (j == i + k - 1) System.out.print("]");
                System.out.print(" ");
            }
            
            // Find and print max
            int max = nums[i];
            for (int j = i; j < i + k; j++) {
                max = Math.max(max, nums[j]);
            }
            System.out.println("  → max = " + max);
        }
    }
    
    /**
     * Explain why deque approach is optimal
     */
    public static void explainDequeApproach() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("WHY MONOTONIC DEQUE WORKS");
        System.out.println("=".repeat(70));
        
        System.out.println("\nKey Insights:");
        System.out.println("1. We only care about potential maximums in current/future windows");
        System.out.println("2. If nums[i] >= nums[j] and i > j:");
        System.out.println("   → nums[j] can NEVER be maximum (nums[i] is better)");
        System.out.println("   → We can safely discard j");
        
        System.out.println("\nDeque Properties:");
        System.out.println("• Stores indices in order of their insertion");
        System.out.println("• Values at these indices are in DECREASING order");
        System.out.println("• Front always has index of maximum in current window");
        System.out.println("• Remove from front: outdated (outside window)");
        System.out.println("• Remove from back: smaller (will never be max)");
        
        System.out.println("\nWhy O(n)?");
        System.out.println("• Each element added to deque once → n operations");
        System.out.println("• Each element removed from deque at most once → n operations");
        System.out.println("• Total: 2n operations = O(n)");
    }
    
    /**
     * Test a single case with all solutions
     */
    public static void testCase(String name, int[] nums, int k) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST CASE: " + name);
        System.out.println("=".repeat(70));
        
        visualizeSlidingWindow(nums, k);
        
        int[] result1 = maxSlidingWindow(nums, k);
        int[] result2 = maxSlidingWindowHeap(nums, k);
        int[] result3 = maxSlidingWindowBruteForce(nums, k);
        int[] result4 = maxSlidingWindowDP(nums, k);
        
        System.out.println("\nResults:");
        System.out.println("Monotonic Deque:      " + Arrays.toString(result1));
        System.out.println("Max Heap:             " + Arrays.toString(result2));
        System.out.println("Brute Force:          " + Arrays.toString(result3));
        System.out.println("DP (Block-based):     " + Arrays.toString(result4));
        
        boolean match = Arrays.equals(result1, result2) && 
                       Arrays.equals(result2, result3) && 
                       Arrays.equals(result3, result4);
        System.out.println("All solutions match:  " + match);
        
        System.out.println("\n--- Detailed Deque Trace ---");
        maxSlidingWindowVerbose(nums, k, true);
    }
    
    // ========================================================================
    // MAIN METHOD WITH TEST CASES
    // ========================================================================
    
    public static void main(String[] args) {
        System.out.println("╔" + "═".repeat(68) + "╗");
        System.out.println("║" + " ".repeat(20) + "SLIDING WINDOW MAXIMUM" + " ".repeat(26) + "║");
        System.out.println("╚" + "═".repeat(68) + "╝");
        
        // Example 1
        testCase("Example 1", new int[]{1, 3, -1, -3, 5, 3, 6, 7}, 3);
        
        // Example 2
        testCase("Example 2", new int[]{1}, 1);
        
        // Additional test cases
        testCase("Increasing sequence", new int[]{1, 2, 3, 4, 5}, 3);
        testCase("Decreasing sequence", new int[]{5, 4, 3, 2, 1}, 3);
        testCase("All same values", new int[]{3, 3, 3, 3}, 2);
        testCase("Window size = array length", new int[]{1, 3, 2, 5, 4}, 5);
        testCase("Alternating values", new int[]{1, 5, 1, 5, 1, 5}, 2);
        testCase("Large window", new int[]{9, 10, 9, -7, -4, -8, 2, -6}, 5);
        testCase("Negative numbers", new int[]{-7, -8, 7, 5, 7, 1, 6, 0}, 4);
        testCase("Peak in middle", new int[]{1, 2, 3, 2, 1}, 3);
        
        explainDequeApproach();
        
        // Performance test
        System.out.println("\n" + "=".repeat(70));
        System.out.println("PERFORMANCE COMPARISON");
        System.out.println("=".repeat(70));
        
        int[] largeArray = new int[100000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = (int)(Math.random() * 10000);
        }
        int k = 1000;
        
        // Test deque approach
        long start = System.nanoTime();
        int[] result = maxSlidingWindow(largeArray, k);
        long end = System.nanoTime();
        double dequeTime = (end - start) / 1_000_000.0;
        
        // Test heap approach
        start = System.nanoTime();
        maxSlidingWindowHeap(largeArray, k);
        end = System.nanoTime();
        double heapTime = (end - start) / 1_000_000.0;
        
        System.out.println("\nArray size: 100,000");
        System.out.println("Window size k: 1,000");
        System.out.println("Number of windows: " + result.length);
        System.out.println("\nMonotonic Deque time: " + dequeTime + " ms");
        System.out.println("Max Heap time:        " + heapTime + " ms");
        System.out.println("Speedup:              " + (heapTime / dequeTime) + "x");
        
        // Complexity analysis
        System.out.println("\n" + "=".repeat(70));
        System.out.println("COMPLEXITY ANALYSIS");
        System.out.println("=".repeat(70));
        
        System.out.println("\n1. Monotonic Deque (RECOMMENDED):");
        System.out.println("   Time:  O(n) - each element added/removed once");
        System.out.println("   Space: O(k) - deque size at most k");
        System.out.println("   Pros:  Optimal time and space, elegant solution");
        System.out.println("   Best for: Production code, interviews");
        
        System.out.println("\n2. Max Heap:");
        System.out.println("   Time:  O(n log n) - n insertions, each O(log n)");
        System.out.println("   Space: O(n) - heap can grow to size n");
        System.out.println("   Pros:  Intuitive, easier to understand");
        System.out.println("   Best for: When deque is unfamiliar");
        
        System.out.println("\n3. Brute Force:");
        System.out.println("   Time:  O(n * k) - k operations per window");
        System.out.println("   Space: O(1) - no extra space");
        System.out.println("   Pros:  Simple to implement");
        System.out.println("   Best for: Small k or learning");
        
        System.out.println("\n4. DP (Block-based):");
        System.out.println("   Time:  O(n) - linear passes");
        System.out.println("   Space: O(n) - two arrays");
        System.out.println("   Pros:  Clever, no data structures needed");
        System.out.println("   Best for: Academic interest");
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("KEY INSIGHTS");
        System.out.println("=".repeat(70));
        System.out.println("• Deque maintains potential maximums in decreasing order");
        System.out.println("• Front of deque is always current window's maximum");
        System.out.println("• Remove from front: indices outside window");
        System.out.println("• Remove from back: values smaller than current");
        System.out.println("• Similar to: stock span, histogram, next greater element");
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("COMMON PATTERNS");
        System.out.println("=".repeat(70));
        System.out.println("\n1. When nums[i] >= nums[j] and i > j:");
        System.out.println("   → Remove j (will never be maximum)");
        System.out.println("\n2. When index < i - k + 1:");
        System.out.println("   → Remove from front (outside window)");
        System.out.println("\n3. Deque stores indices, not values:");
        System.out.println("   → Need index to check window boundaries");
        System.out.println("   → Access value via nums[index]");
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("INTERVIEW TIPS");
        System.out.println("=".repeat(70));
        System.out.println("✓ Start with brute force to show understanding");
        System.out.println("✓ Explain why we need O(n) solution for large arrays");
        System.out.println("✓ Discuss why smaller elements can be discarded");
        System.out.println("✓ Walk through deque operations with example");
        System.out.println("✓ Mention this is a classic monotonic deque problem");
        System.out.println("✓ Explain amortized O(n) time (each element touched twice)");
    }
}

/**
 * ============================================================================
 * DETAILED ALGORITHM WALKTHROUGH - MONOTONIC DEQUE
 * ============================================================================
 * 
 * Example: nums = [1,3,-1,-3,5,3,6,7], k = 3
 * 
 * Goal: Find maximum in each sliding window of size 3
 * 
 * Deque Operations (stores indices, shown as index(value)):
 * 
 * i=0, nums[0]=1:
 *   Deque: []
 *   Not outside window: no removals
 *   Not smaller: no removals
 *   Add 0
 *   Deque: [0(1)]
 *   Window not complete yet (i < k-1)
 * 
 * i=1, nums[1]=3:
 *   Deque: [0(1)]
 *   Not outside window
 *   3 > 1: remove 0
 *   Add 1
 *   Deque: [1(3)]
 *   Window not complete yet
 * 
 * i=2, nums[2]=-1:
 *   Deque: [1(3)]
 *   Not outside window
 *   -1 < 3: no removal
 *   Add 2
 *   Deque: [1(3), 2(-1)]
 *   Window complete! result[0] = nums[1] = 3
 * 
 * i=3, nums[3]=-3:
 *   Deque: [1(3), 2(-1)]
 *   Index 0 < 3-3+1=1: not outside (already removed)
 *   -3 < -1: no removal
 *   Add 3
 *   Deque: [1(3), 2(-1), 3(-3)]
 *   result[1] = nums[1] = 3
 * 
 * i=4, nums[4]=5:
 *   Deque: [1(3), 2(-1), 3(-3)]
 *   Index 1 < 4-3+1=2: remove 1
 *   Deque: [2(-1), 3(-3)]
 *   5 > -3: remove 3
 *   5 > -1: remove 2
 *   Deque: []
 *   Add 4
 *   Deque: [4(5)]
 *   result[2] = nums[4] = 5
 * 
 * i=5, nums[5]=3:
 *   Deque: [4(5)]
 *   Not outside window
 *   3 < 5: no removal
 *   Add 5
 *   Deque: [4(5), 5(3)]
 *   result[3] = nums[4] = 5
 * 
 * i=6, nums[6]=6:
 *   Deque: [4(5), 5(3)]
 *   Index 4 < 6-3+1=4: not outside
 *   6 > 3: remove 5
 *   6 > 5: remove 4
 *   Deque: []
 *   Add 6
 *   Deque: [6(6)]
 *   result[4] = nums[6] = 6
 * 
 * i=7, nums[7]=7:
 *   Deque: [6(6)]
 *   Not outside window
 *   7 > 6: remove 6
 *   Deque: []
 *   Add 7
 *   Deque: [7(7)]
 *   result[5] = nums[7] = 7
 * 
 * Final result: [3, 3, 5, 5, 6, 7] ✓
 * 
 * ============================================================================
 * WHY THIS IS O(n)
 * ============================================================================
 * 
 * Each element is:
 * 1. Added to deque exactly once → n operations
 * 2. Removed from deque at most once → n operations
 * 
 * Total operations: 2n = O(n)
 * 
 * This is called "amortized analysis":
 * - While some iterations may do multiple removals
 * - Over all n iterations, each element is touched at most twice
 * - Therefore, average time per iteration is O(1)
 * - Total time is O(n)
 * 
 * ============================================================================
 */
