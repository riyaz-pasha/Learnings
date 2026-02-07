import java.util.*;

/**
 * K CLOSEST POINTS TO ORIGIN - COMPREHENSIVE SOLUTION GUIDE
 * 
 * PROBLEM UNDERSTANDING:
 * =====================
 * Given points on X-Y plane and integer k, find k closest points to origin (0,0).
 * Distance formula: √(x² + y²) - Euclidean distance
 * 
 * Example: points = [[1,3],[-2,2]], k = 1
 * Point [1,3]: distance = √(1² + 3²) = √10 ≈ 3.16
 * Point [-2,2]: distance = √(4 + 4) = √8 ≈ 2.83
 * Answer: [[-2,2]] (closer to origin)
 * 
 * KEY INSIGHTS FOR INTERVIEWS:
 * ============================
 * 1. We don't need to compute √ - can compare x²+y² directly
 * 2. This is a "partial sorting" problem - don't need full sort
 * 3. Multiple approaches with different trade-offs
 * 4. For interviews, start with simplest, then optimize
 * 
 * APPROACH COMPARISON:
 * ===================
 * 1. Sort All Points: O(n log n) time, O(1) space - Simple but overkill
 * 2. Max Heap: O(n log k) time, O(k) space - Good for small k
 * 3. QuickSelect: O(n) average time, O(1) space - Optimal but complex
 * 4. Min Heap: O(n log n) time, O(n) space - Clean but not optimal
 * 
 * INTERVIEW STRATEGY:
 * ==================
 * 1. Start with approach #1 (sorting) - shows you understand problem
 * 2. Mention it's not optimal - "we only need k elements, not full sort"
 * 3. Suggest max heap approach - optimal for small k
 * 4. If time permits, discuss QuickSelect for theoretical best case
 */

class KClosestPoints {
    
    /**
     * APPROACH 1: SORTING (EASIEST TO CODE, GOOD STARTING POINT)
     * ===========================================================
     * Time Complexity: O(n log n) where n = number of points
     * Space Complexity: O(log n) for sorting (or O(n) depending on sort implementation)
     * 
     * INTUITION:
     * - Sort all points by distance
     * - Take first k points
     * - Simple and correct, but does more work than needed
     * 
     * WHEN TO USE:
     * - Interview time pressure - quickest to code correctly
     * - Small datasets where performance doesn't matter
     * - As starting point before optimizing
     */
    public int[][] kClosestSort(int[][] points, int k) {
        // Sort points by distance from origin
        // KEY INSIGHT: Don't need sqrt - x² + y² comparison is equivalent
        Arrays.sort(points, (a, b) -> {
            int distA = a[0] * a[0] + a[1] * a[1];
            int distB = b[0] * b[0] + b[1] * b[1];
            return distA - distB;
        });
        
        // Return first k points
        // Arrays.copyOfRange is O(k) which is acceptable
        return Arrays.copyOfRange(points, 0, k);
    }
    
    /**
     * APPROACH 2: MAX HEAP (OPTIMAL FOR SMALL K)
     * ===========================================
     * Time Complexity: O(n log k) where n = points, k = required closest
     * Space Complexity: O(k)
     * 
     * INTUITION:
     * - Maintain a max heap of size k
     * - For each point, if closer than farthest in heap, replace it
     * - Heap always contains k closest points seen so far
     * 
     * WHY MAX HEAP, NOT MIN HEAP?
     * - We want to quickly identify and remove the FARTHEST point
     * - Max heap puts farthest point at top for O(1) access
     * - When we see a closer point, we pop the max (farthest)
     * 
     * STEP-BY-STEP EXAMPLE:
     * points = [[3,3],[5,1],[1,2],[4,4]], k = 2
     * 
     * Initialize heap (max heap by distance):
     * Process [3,3]: dist=18, heap=[18], size=1 < k, add it
     * Process [5,1]: dist=26, heap=[26,18], size=2 = k, heap full
     * Process [1,2]: dist=5, 5 < 26, remove 26, add 5, heap=[18,5]
     * Process [4,4]: dist=32, 32 > 18, skip (farther than current max)
     * 
     * Final heap: [18,5] → [[3,3],[1,2]]
     * 
     * WHY THIS IS BETTER THAN SORTING:
     * - We only maintain k elements, not all n
     * - Each operation is log k, not log n
     * - When k << n (e.g., k=10, n=1000000), huge savings
     */
    public int[][] kClosestMaxHeap(int[][] points, int k) {
        // Max heap: largest distance at top
        // PriorityQueue is min heap by default, so reverse for max heap
        PriorityQueue<int[]> maxHeap = new PriorityQueue<>(
            (a, b) -> {
                int distA = a[0] * a[0] + a[1] * a[1];
                int distB = b[0] * b[0] + b[1] * b[1];
                return distB - distA; // Note: distB - distA for MAX heap
            }
        );
        
        for (int[] point : points) {
            maxHeap.offer(point);
            
            // If heap exceeds k elements, remove the farthest point
            if (maxHeap.size() > k) {
                maxHeap.poll(); // Removes point with largest distance
            }
        }
        
        // Convert heap to array
        int[][] result = new int[k][2];
        for (int i = 0; i < k; i++) {
            result[i] = maxHeap.poll();
        }
        
        return result;
    }
    
    /**
     * APPROACH 3: QUICKSELECT (OPTIMAL AVERAGE CASE)
     * ===============================================
     * Time Complexity: O(n) average, O(n²) worst case
     * Space Complexity: O(1) excluding recursion stack
     * 
     * INTUITION:
     * - Similar to QuickSort, but only recurse on one side
     * - Partition array so k smallest elements are on left
     * - Don't need them sorted, just partitioned
     * 
     * WHY THIS IS OPTIMAL:
     * - Average case O(n) beats O(n log k) and O(n log n)
     * - Based on selection algorithm (finding kth smallest)
     * 
     * HOW IT WORKS:
     * 1. Choose a pivot
     * 2. Partition: smaller distances left, larger right
     * 3. If pivot index == k-1, done!
     * 4. If pivot index > k-1, recurse left
     * 5. If pivot index < k-1, recurse right
     * 
     * EXAMPLE:
     * points = [[3,3],[5,1],[1,2],[4,4],[2,2]], k = 3
     * distances = [18, 26, 5, 32, 8]
     * 
     * After partitioning around pivot (say 18):
     * [5, 8, 18, 32, 26]
     *  0  1   2   3   4
     * 
     * Pivot at index 2, k-1 = 2, perfect!
     * First 3 elements are k closest (not sorted, but that's OK)
     */
    public int[][] kClosestQuickSelect(int[][] points, int k) {
        quickSelect(points, 0, points.length - 1, k);
        return Arrays.copyOfRange(points, 0, k);
    }
    
    private void quickSelect(int[][] points, int left, int right, int k) {
        if (left >= right) return;
        
        // Partition and get pivot index
        int pivotIndex = partition(points, left, right);
        
        // Check if we've found the kth smallest
        if (pivotIndex == k - 1) {
            return; // Perfect! k smallest are in positions 0..k-1
        } else if (pivotIndex < k - 1) {
            // Need more elements, search right
            quickSelect(points, pivotIndex + 1, right, k);
        } else {
            // Have too many elements, search left
            quickSelect(points, left, pivotIndex - 1, k);
        }
    }
    
    /**
     * Partition using Lomuto scheme
     * Returns index where pivot ends up
     */
    private int partition(int[][] points, int left, int right) {
        // Choose rightmost element as pivot (could randomize for better average case)
        int[] pivot = points[right];
        int pivotDist = getDistance(pivot);
        
        // i tracks the boundary between smaller and larger elements
        int i = left;
        
        // Move all elements smaller than pivot to the left
        for (int j = left; j < right; j++) {
            if (getDistance(points[j]) <= pivotDist) {
                swap(points, i, j);
                i++;
            }
        }
        
        // Place pivot in its final position
        swap(points, i, right);
        return i;
    }
    
    private int getDistance(int[] point) {
        return point[0] * point[0] + point[1] * point[1];
    }
    
    private void swap(int[][] points, int i, int j) {
        int[] temp = points[i];
        points[i] = points[j];
        points[j] = temp;
    }
    
    /**
     * APPROACH 4: MIN HEAP - ALL ELEMENTS (EDUCATIONAL)
     * ==================================================
     * Time Complexity: O(n log n)
     * Space Complexity: O(n)
     * 
     * This is less efficient but shows understanding of heaps.
     * Good to mention as alternative but not recommended.
     */
    public int[][] kClosestMinHeap(int[][] points, int k) {
        // Min heap: smallest distance at top
        PriorityQueue<int[]> minHeap = new PriorityQueue<>(
            (a, b) -> {
                int distA = a[0] * a[0] + a[1] * a[1];
                int distB = b[0] * b[0] + b[1] * b[1];
                return distA - distB;
            }
        );
        
        // Add all points
        for (int[] point : points) {
            minHeap.offer(point);
        }
        
        // Extract k smallest
        int[][] result = new int[k][2];
        for (int i = 0; i < k; i++) {
            result[i] = minHeap.poll();
        }
        
        return result;
    }
    
    /**
     * APPROACH 5: OPTIMIZED QUICKSELECT WITH RANDOM PIVOT
     * ====================================================
     * Time Complexity: O(n) average case with high probability
     * Space Complexity: O(1)
     * 
     * Improvement over basic QuickSelect: randomized pivot prevents worst case
     */
    public int[][] kClosestQuickSelectOptimized(int[][] points, int k) {
        quickSelectRandom(points, 0, points.length - 1, k);
        return Arrays.copyOfRange(points, 0, k);
    }
    
    private void quickSelectRandom(int[][] points, int left, int right, int k) {
        if (left >= right) return;
        
        // Randomize pivot to avoid worst case
        int randomIndex = left + (int)(Math.random() * (right - left + 1));
        swap(points, randomIndex, right);
        
        int pivotIndex = partition(points, left, right);
        
        if (pivotIndex == k - 1) {
            return;
        } else if (pivotIndex < k - 1) {
            quickSelectRandom(points, pivotIndex + 1, right, k);
        } else {
            quickSelectRandom(points, left, pivotIndex - 1, k);
        }
    }
    
    /**
     * HELPER METHOD: Print points nicely
     */
    private static void printPoints(String label, int[][] points) {
        System.out.print(label + ": [");
        for (int i = 0; i < points.length; i++) {
            System.out.print(Arrays.toString(points[i]));
            if (i < points.length - 1) System.out.print(", ");
        }
        System.out.println("]");
    }
    
    /**
     * HELPER METHOD: Calculate actual distance (for verification)
     */
    private static double actualDistance(int[] point) {
        return Math.sqrt(point[0] * point[0] + point[1] * point[1]);
    }
    
    // TEST CASES
    public static void main(String[] args) {
        KClosestPoints solution = new KClosestPoints();
        
        System.out.println("=== K CLOSEST POINTS TO ORIGIN - TEST CASES ===\n");
        
        // Test Case 1: Standard example
        System.out.println("Test 1: Standard example");
        int[][] points1 = {{1,3},{-2,2}};
        int k1 = 1;
        System.out.println("Input: points = [[1,3],[-2,2]], k = 1");
        System.out.println("Distances:");
        System.out.println("  [1,3]: " + actualDistance(new int[]{1,3}));
        System.out.println("  [-2,2]: " + actualDistance(new int[]{-2,2}));
        printPoints("Output (Sort)", solution.kClosestSort(points1.clone(), k1));
        printPoints("Output (MaxHeap)", solution.kClosestMaxHeap(points1.clone(), k1));
        printPoints("Output (QuickSelect)", solution.kClosestQuickSelect(points1.clone(), k1));
        System.out.println("Expected: [[-2,2]]");
        System.out.println();
        
        // Test Case 2: Multiple points
        System.out.println("Test 2: Multiple close points");
        int[][] points2 = {{3,3},{5,-1},{-2,4}};
        int k2 = 2;
        System.out.println("Input: points = [[3,3],[5,-1],[-2,4]], k = 2");
        System.out.println("Distances:");
        System.out.println("  [3,3]: " + actualDistance(new int[]{3,3}));
        System.out.println("  [5,-1]: " + actualDistance(new int[]{5,-1}));
        System.out.println("  [-2,4]: " + actualDistance(new int[]{-2,4}));
        printPoints("Output (Sort)", solution.kClosestSort(points2.clone(), k2));
        printPoints("Output (MaxHeap)", solution.kClosestMaxHeap(points2.clone(), k2));
        printPoints("Output (QuickSelect)", solution.kClosestQuickSelect(points2.clone(), k2));
        System.out.println("Expected: [[3,3],[5,-1]] or [[3,3],[-2,4]] (order doesn't matter)");
        System.out.println();
        
        // Test Case 3: All points requested
        System.out.println("Test 3: k equals number of points");
        int[][] points3 = {{1,1},{2,2},{3,3}};
        int k3 = 3;
        System.out.println("Input: points = [[1,1],[2,2],[3,3]], k = 3");
        printPoints("Output (Sort)", solution.kClosestSort(points3.clone(), k3));
        System.out.println("Expected: All points (any order)");
        System.out.println();
        
        // Test Case 4: Points on axes
        System.out.println("Test 4: Points on axes and origin");
        int[][] points4 = {{0,1},{1,0},{-1,0},{0,-1}};
        int k4 = 2;
        System.out.println("Input: points = [[0,1],[1,0],[-1,0],[0,-1]], k = 2");
        System.out.println("All have distance = 1 from origin");
        printPoints("Output (Sort)", solution.kClosestSort(points4.clone(), k4));
        System.out.println("Expected: Any 2 points (all equidistant)");
        System.out.println();
        
        // Test Case 5: Larger dataset
        System.out.println("Test 5: Larger dataset");
        int[][] points5 = {{1,3},{3,1},{2,2},{4,4},{-1,-1},{0,2},{-2,0}};
        int k5 = 3;
        System.out.println("Input: 7 points, k = 3");
        for (int[] p : points5) {
            System.out.println("  " + Arrays.toString(p) + ": distance = " + actualDistance(p));
        }
        printPoints("Output (Sort)", solution.kClosestSort(points5.clone(), k5));
        printPoints("Output (MaxHeap)", solution.kClosestMaxHeap(points5.clone(), k5));
        printPoints("Output (QuickSelect)", solution.kClosestQuickSelect(points5.clone(), k5));
        System.out.println();
        
        // Performance comparison
        System.out.println("=== COMPLEXITY ANALYSIS ===");
        System.out.println("Approach          | Time Complexity | Space Complexity | Best For");
        System.out.println("------------------|-----------------|------------------|------------------");
        System.out.println("1. Sort           | O(n log n)      | O(log n)         | Simple, small n");
        System.out.println("2. Max Heap       | O(n log k)      | O(k)             | Small k << n");
        System.out.println("3. QuickSelect    | O(n) avg        | O(1)             | Large n, theory");
        System.out.println("4. Min Heap       | O(n log n)      | O(n)             | Not recommended");
        System.out.println("5. Random QS      | O(n) avg        | O(1)             | Production code");
        System.out.println();
        
        System.out.println("=== WHEN TO USE EACH APPROACH ===");
        System.out.println("In Interview:");
        System.out.println("  - Start with Sort (easiest to code, shows understanding)");
        System.out.println("  - Optimize to Max Heap (best practical solution for small k)");
        System.out.println("  - Mention QuickSelect (shows advanced knowledge)");
        System.out.println();
        System.out.println("In Production:");
        System.out.println("  - k < log(n): Max Heap");
        System.out.println("  - k ≈ n: Sort");
        System.out.println("  - k ≈ n/2: QuickSelect with random pivot");
    }
}

/**
 * INTERVIEW STRATEGY GUIDE
 * ========================
 * 
 * 1. CLARIFY THE PROBLEM (2 minutes)
 *    Q: "Can points be negative?" A: Yes
 *    Q: "Can k be larger than number of points?" A: No, guaranteed k ≤ n
 *    Q: "Does order matter?" A: No
 *    Q: "Can we have duplicate points?" A: Yes (though rare)
 *    Q: "Do we need exact distances or can we compare squared distances?" 
 *       A: We only need relative ordering, so squared distances work!
 * 
 * 2. START WITH SIMPLE SOLUTION (5 minutes)
 *    "I'll start with sorting all points by distance..."
 *    [Code approach #1]
 *    "This works but does more work than needed since we sort all n elements"
 * 
 * 3. OPTIMIZE (10 minutes)
 *    "We only need k elements, not a full sort. Can we use a heap?"
 *    "If we use a max heap of size k..."
 *    [Code approach #2]
 *    "This improves from O(n log n) to O(n log k)"
 * 
 * 4. DISCUSS FURTHER OPTIMIZATION (3 minutes)
 *    "There's a QuickSelect approach that averages O(n)..."
 *    "But it's more complex and the improvement might not be worth it unless n is huge"
 *    "Max heap is probably the best practical solution"
 * 
 * 5. TEST (5 minutes)
 *    - Test with given examples
 *    - Edge case: k = 1
 *    - Edge case: k = n
 *    - Edge case: points on a circle (all equidistant)
 * 
 * 
 * COMMON MISTAKES TO AVOID
 * ========================
 * 
 * 1. COMPUTING SQUARE ROOT UNNECESSARILY
 *    ❌ double dist = Math.sqrt(x*x + y*y);
 *    ✓  int dist = x*x + y*y;
 *    
 *    Why? We only need relative ordering, sqrt is monotonic
 *    Savings: sqrt is expensive, and we avoid floating point issues
 * 
 * 2. USING MIN HEAP WHEN MAX HEAP IS BETTER
 *    ❌ PriorityQueue<int[]> heap = new PriorityQueue<>((a,b) -> distA - distB);
 *       // Then add all n points and poll k times
 *    ✓  PriorityQueue<int[]> maxHeap = new PriorityQueue<>((a,b) -> distB - distA);
 *       // Add points one by one, keep size ≤ k
 *    
 *    Why? Max heap approach is O(n log k), min heap is O(n log n)
 * 
 * 3. FORGETTING TO CLONE INPUT ARRAY IN QUICKSELECT
 *    ❌ quickSelect(points, 0, points.length - 1, k);
 *       return points; // Returns ALL points, modified!
 *    ✓  quickSelect(points, 0, points.length - 1, k);
 *       return Arrays.copyOfRange(points, 0, k);
 * 
 * 4. OVERFLOW WITH LARGE COORDINATES
 *    ❌ int dist = x*x + y*y; // Can overflow if x,y are large
 *    ✓  long dist = (long)x*x + (long)y*y;
 *    
 *    Ask interviewer about constraints! For typical LeetCode: int is fine
 * 
 * 5. WRONG HEAP COMPARATOR LOGIC
 *    ❌ return distB - distA; // Assumes no overflow
 *    ✓  return Integer.compare(distB, distA); // Safe comparison
 * 
 * 
 * FOLLOW-UP QUESTIONS TO PREPARE FOR
 * ==================================
 * 
 * Q: "What if k is very small (like k=1) or very large (like k=n-1)?"
 * A: For k=1, all approaches degenerate to O(n) linear scan
 *    For k=n-1, sorting might actually be simpler/faster
 *    Max heap is still good middle ground
 * 
 * Q: "What if points come in a stream and k can change?"
 * A: Maintain two heaps (like median finder problem)
 *    Or use a balanced BST to support dynamic k
 * 
 * Q: "How would you handle 3D points?"
 * A: Same algorithm, just distance = x²+y²+z²
 * 
 * Q: "What if we need k farthest points instead?"
 * A: Use min heap instead of max heap (or negate distances)
 * 
 * Q: "Can you do better than O(n) average?"
 * A: No, we must look at all n points at least once
 *    O(n) is optimal for this problem
 * 
 * Q: "What if distances must be exact (not squared)?"
 * A: Still don't compute sqrt until final result
 *    Compare squared distances throughout algorithm
 *    Only compute sqrt when returning to user (if needed)
 * 
 * 
 * KEY TAKEAWAYS
 * =============
 * 
 * 1. Don't over-optimize initially - start simple!
 * 2. Max heap is the sweet spot: not too simple, not too complex
 * 3. Avoid computing sqrt - use squared distances
 * 4. QuickSelect is theoretically optimal but practically overkill
 * 5. This problem tests: heaps, sorting, selection algorithms
 * 6. Always ask about constraints (k size, coordinate range)
 * 
 * Good luck! 🎯
 */
