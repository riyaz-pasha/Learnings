import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * PROBLEM STATEMENT:
 * You are given an array, nums, consisting of non-negative integers, and an integer k 
 * representing the maximum number of allowed operations.
 * 
 * In each operation, you may select any element in nums and increment it by 1. 
 * You can perform, at most, k such operations.
 * 
 * Your task is to maximize the product of all elements in the array after performing up to k operations. 
 * As the resulting product can be very large, return the product modulo 10^9 + 7.
 * 
 * Note: Ensure that the product is maximized before applying the modulo operation.
 *
 * CONSTRAINTS:
 * 1 <= nums.length, k <= 10^3
 * 0 <= nums[i] <= 10^3
 * 
 * ==========================================================================================
 * CRITICAL INSIGHT - GREEDY MATH PROPERTY:
 * To maximize a product of numbers, you should always increment the SMALLEST number.
 * Why? The proportional increase is highest for the smallest number.
 * For example:
 * We have 2 and 5. k = 1.
 * Option A: Increment 5 to 6. Product = 2 * 6 = 12.
 * Option B: Increment 2 to 3. Product = 3 * 5 = 15.
 * Option B wins because 3/2 (50% increase) > 6/5 (20% increase).
 * 
 * Therefore, the optimal strategy is a Greedy Approach: continually find the smallest element, 
 * increment it by 1, and repeat k times.
 * ==========================================================================================
 * LATEST JAVA FEATURES USED:
 * - Records (`record TestCase(...)`): For defining clean immutable test scenarios.
 * - Local Variable Type Inference (`var`): To reduce boilerplate while keeping type safety.
 * ==========================================================================================
 */
class MaximizeArrayProduct {

    private static final int MOD = 1_000_000_007;

    // ==========================================================================================
    // SOLUTION 1: Brute Force Simulation
    // ==========================================================================================
    /*
     * EXPLANATION:
     * In this approach, for each of the 'k' operations, we linearly scan the array to find
     * the minimum element and increment it by 1. After completing all 'k' operations, we compute 
     * the product of the modified array elements.
     *
     * VISUAL:
     * nums = [1, 2], k = 3
     * 
     * Op 1: Min is 1 (index 0). Array becomes [2, 2]
     * Op 2: Min is 2 (index 0). Array becomes [3, 2]
     * Op 3: Min is 2 (index 1). Array becomes [3, 3]
     * 
     * Final Product = 3 * 3 = 9.
     *
     * COMPLEXITY:
     * - Time: O(k * N) - We do a full scan of length N, k times. With N, k <= 10^3, this is max 1,000,000 operations (very fast).
     * - Space: O(N) - To clone the array so we don't mutate the original input.
     */
    public static int maxProduct_BruteForce(int[] nums, int k) {
        var copy = nums.clone(); // Prevent side-effects on original array

        for (int step = 0; step < k; step++) {
            // Find min element index
            int minIdx = 0;
            for (int i = 1; i < copy.length; i++) {
                if (copy[i] < copy[minIdx]) {
                    minIdx = i;
                }
            }
            // Increment the minimum element
            copy[minIdx]++;
        }

        // Calculate product modulo 10^9 + 7
        long product = 1;
        for (int val : copy) {
            product = (product * val) % MOD;
        }

        return (int) product;
    }

    // ==========================================================================================
    // SOLUTION 2: Min-Heap / PriorityQueue (Optimal Approach)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * Scanning the array every time takes O(N) time. Instead, we can use a Min-Heap (PriorityQueue).
     * A Min-Heap naturally keeps the smallest element at the root. 
     * We can extract the minimum in O(log N) time, increment it by 1, and push it back into the heap 
     * in O(log N) time. This is much more efficient, especially if k was much larger.
     *
     * VISUAL:
     * nums = [0, 4], k = 5
     * 
     * Init Heap: [0, 4]
     * 
     * Op 1: Poll 0. Push 1. Heap: [1, 4]
     * Op 2: Poll 1. Push 2. Heap: [2, 4]
     * Op 3: Poll 2. Push 3. Heap: [3, 4]
     * Op 4: Poll 3. Push 4. Heap: [4, 4]
     * Op 5: Poll 4. Push 5. Heap: [4, 5]
     * 
     * Final Product = 4 * 5 = 20.
     *
     * COMPLEXITY:
     * - Time: O(N + k log N) -> Building heap takes O(N). Each of the k increments takes O(log N).
     * - Space: O(N) -> To store the elements in the PriorityQueue.
     */
    public static int maxProduct_MinHeap(int[] nums, int k) {
        PriorityQueue<Integer> minHeap = new PriorityQueue<>(nums.length);

        // Add all elements to the Min-Heap
        for (int num : nums) {
            minHeap.offer(num);
        }

        // Perform k operations
        for (int i = 0; i < k; i++) {
            // Get the smallest number
            int currentMin = minHeap.poll();
            
            // Increment it and put it back
            minHeap.offer(currentMin + 1);
        }

        // Calculate product modulo 10^9 + 7
        long product = 1;
        while (!minHeap.isEmpty()) {
            product = (product * minHeap.poll()) % MOD;
        }

        return (int) product;
    }

    // ==========================================================================================
    // TESTING SUITE
    // ==========================================================================================
    
    // Java 14+ Record for grouping test case parameters cleanly
    public record TestCase(int[] nums, int k, int expected) {}

    public static void main(String[] args) {
        var testCases = new TestCase[] {
            new TestCase(new int[]{1, 2}, 3, 9), // Explored in visual: [1,2] -> [2,2] -> [3,2] -> [3,3] => 9
            new TestCase(new int[]{0, 4}, 5, 20), // Explored in visual: [0,4] -> ... -> [4,5] => 20
            new TestCase(new int[]{6, 3, 3, 2}, 2, 216), // [6,3,3,2] -> [6,3,3,3] -> [6,4,3,3] => 216
            new TestCase(new int[]{1000, 1000}, 1000, 225000000), // Large values, testing modulus logic
            new TestCase(new int[]{0, 0, 0}, 3, 1), // [0,0,0] -> [1,0,0] -> [1,1,0] -> [1,1,1] => 1
            new TestCase(new int[]{10}, 5, 15) // Single element edge case: 10 + 5 = 15 => 15
        };

        System.out.println("Running Maximize Array Product Tests...\n");

        for (int i = 0; i < testCases.length; i++) {
            var tc = testCases[i];
            System.out.printf("Test Case %d:\n", i + 1);
            System.out.printf("Array: %s | k: %d | Expected: %d\n", Arrays.toString(tc.nums()), tc.k(), tc.expected());

            // Run Brute Force Solution
            int res1 = maxProduct_BruteForce(tc.nums(), tc.k());
            boolean pass1 = (res1 == tc.expected());
            System.out.printf("  [1. Brute Force] Result: %d -> %s\n", res1, pass1 ? "PASS" : "FAIL");

            // Run Min-Heap Solution
            int res2 = maxProduct_MinHeap(tc.nums(), tc.k());
            boolean pass2 = (res2 == tc.expected());
            System.out.printf("  [2. Min-Heap  ] Result: %d -> %s\n", res2, pass2 ? "PASS" : "FAIL");

            System.out.println("-".repeat(60));
        }
    }
}
