import java.util.Arrays;
import java.util.Collections;
import java.util.PriorityQueue;

/**
 * PROBLEM STATEMENT:
 * You are given a 0-indexed array of integer nums and an integer k. 
 * Your task is to maximize a score through a series of operations. Initially, score = 0.
 * 
 * In each operation:
 * 1. Select an index i.
 * 2. Add nums[i] to your score.
 * 3. Replace nums[i] with ceil(nums[i] / 3).
 * 
 * Repeat exactly k times and return the highest score achievable.
 *
 * CONSTRAINTS:
 * 1 <= nums.length, k <= 10^3
 * 1 <= nums[i] <= 10^5
 * 
 * ==========================================================================================
 * CRITICAL INSIGHT:
 * To maximize the score, we must ALWAYS pick the largest available number at any given step.
 * A Greedy approach works perfectly here. After picking the largest number, it shrinks 
 * (becomes roughly 1/3 of its size) and is placed back into the pool. We then need to find 
 * the NEW largest number for the next step.
 * 
 * Mathematically, ceil(x / 3) can be computed using integer arithmetic as: (x + 2) / 3.
 * ==========================================================================================
 * LATEST JAVA FEATURES USED:
 * - Records (`record TestCase(...)`): For clean, immutable test data structures.
 * - Local Variable Type Inference (`var`): Cleaner syntax.
 * ==========================================================================================
 */
public class MaximizeScoreAfterKOperations {

    // ==========================================================================================
    // SOLUTION 1: Brute Force / Linear Scan (Simple but slower)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * In this approach, for each of the 'k' operations, we linearly scan the array to find
     * the maximum element. We add it to our score, update that element in the array to its
     * ceiling divided by 3, and repeat.
     *
     * VISUAL:
     * nums = [10, 20, 7], k = 3
     * 
     * Op 1: Max is 20 at index 1.
     *       Score = 20. Array becomes [10, 7, 7]  (since ceil(20/3) = 7)
     * Op 2: Max is 10 at index 0.
     *       Score = 20 + 10 = 30. Array becomes [4, 7, 7]  (since ceil(10/3) = 4)
     * Op 3: Max is 7 at index 1 (or 2).
     *       Score = 30 + 7 = 37. Array becomes [4, 3, 7]
     * 
     * Final Score = 37.
     *
     * COMPLEXITY:
     * - Time: O(k * N) - We do a full scan of length N, k times.
     * - Space: O(N) to clone the array so we don't mutate the original input.
     */
    public static long maxScore_BruteForce(int[] nums, int k) {
        var copy = nums.clone(); // Prevent side-effects on original array
        long score = 0;

        for (int step = 0; step < k; step++) {
            // Find max element index
            int maxIdx = 0;
            for (int i = 1; i < copy.length; i++) {
                if (copy[i] > copy[maxIdx]) {
                    maxIdx = i;
                }
            }

            // Add max to score
            score += copy[maxIdx];

            // Replace with ceil(val / 3)
            // (val + 2) / 3 is a common trick for ceil(val / 3) with integers
            copy[maxIdx] = (copy[maxIdx] + 2) / 3; 
        }

        return score;
    }

    // ==========================================================================================
    // SOLUTION 2: Max-Heap / PriorityQueue (Optimal Approach)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * Scanning the array every time is inefficient. A PriorityQueue (Max-Heap) keeps the 
     * largest element at the root. We can extract the maximum in O(log N) time, add it 
     * to our score, compute the new value, and insert it back in O(log N) time.
     * 
     * By default, Java's PriorityQueue is a Min-Heap. We use Collections.reverseOrder() 
     * to turn it into a Max-Heap.
     *
     * VISUAL:
     * nums = [10, 20, 7], k = 3
     * 
     * Init Heap: [20, 10, 7]
     * 
     * Op 1: Poll 20. Score = 20. 
     *       Push ceil(20/3) = 7. 
     *       Heap: [10, 7, 7]
     * 
     * Op 2: Poll 10. Score = 20 + 10 = 30.
     *       Push ceil(10/3) = 4.
     *       Heap: [7, 7, 4]
     * 
     * Op 3: Poll 7. Score = 30 + 7 = 37.
     *       Push ceil(7/3) = 3.
     *       Heap: [7, 4, 3]
     *
     * COMPLEXITY:
     * - Time: O(N log N + k log N). Building the heap takes O(N log N) (or O(N) if bulk-loaded), 
     *         and performing k operations takes O(k log N).
     * - Space: O(N) to store the elements in the PriorityQueue.
     */
    public static long maxScore_MaxHeap(int[] nums, int k) {
        long score = 0;
        // Create a Max-Heap
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());

        // Add all elements to the heap
        for (var num : nums) {
            maxHeap.offer(num);
        }

        // Perform k operations
        for (int i = 0; i < k; i++) {
            // Get the largest available number
            int currentMax = maxHeap.poll();
            
            // Add to score
            score += currentMax;
            
            // Calculate ceil(currentMax / 3) and put it back
            int nextVal = (currentMax + 2) / 3;
            maxHeap.offer(nextVal);
        }

        return score;
    }

    // ==========================================================================================
    // TESTING SUITE
    // ==========================================================================================
    
    // Java 14+ Record for grouping test case parameters cleanly
    public record TestCase(int[] nums, int k, long expected) {}

    public static void main(String[] args) {
        var testCases = new TestCase[] {
            new TestCase(new int[]{10, 20, 7}, 3, 37),
            new TestCase(new int[]{1, 10, 3, 3, 3}, 3, 17),
            new TestCase(new int[]{100}, 2, 134), // Op1: +100 -> 34, Op2: +34 -> 12. Score: 134
            new TestCase(new int[]{1, 1, 1, 1}, 5, 5), // 1/3 ceil is 1. Score adds 1 each time.
            new TestCase(new int[]{100000, 100000}, 10, 349997) // High values test
        };

        System.out.println("Running Maximize Score Tests...\n");

        for (int i = 0; i < testCases.length; i++) {
            var tc = testCases[i];
            System.out.printf("Test Case %d:\n", i + 1);
            System.out.printf("Array: %s | k: %d | Expected: %d\n", Arrays.toString(tc.nums()), tc.k(), tc.expected());

            // Run Brute Force Solution
            long res1 = maxScore_BruteForce(tc.nums(), tc.k());
            boolean pass1 = (res1 == tc.expected());
            System.out.printf("  [1. Brute Force] Result: %d -> %s\n", res1, pass1 ? "PASS" : "FAIL");

            // Run Max-Heap Solution
            long res2 = maxScore_MaxHeap(tc.nums(), tc.k());
            boolean pass2 = (res2 == tc.expected());
            System.out.printf("  [2. Max-Heap  ] Result: %d -> %s\n", res2, pass2 ? "PASS" : "FAIL");

            System.out.println("-".repeat(60));
        }
    }
}
