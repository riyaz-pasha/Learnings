/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an array of positive integers, `nums`.
 * We need to find the sum of EVERY possible continuous subarray.
 * Then, we collect all these sums, sort them in non-decreasing (ascending) order.
 * Finally, we return the sum of the elements in this sorted array from index 
 * `left` to `right` (1-based indexing).
 * Because the final sum can be very large, we must return it modulo 10^9 + 7.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can the elements in `nums` be zero or negative? 
 *    (Constraints say positive integers 1 <= nums[i] <= 100. This means subarray 
 *    sums are strictly increasing as we expand the subarray).
 * 2. What is the maximum possible size of `nums`?
 *    (N <= 1000. Total subarrays = 1000 * 1001 / 2 = 500,500. This easily fits 
 *    in memory).
 * 3. Should I worry about integer overflow for the subarray sums themselves?
 *    (Max subarray sum = 1000 * 100 = 100,000. This easily fits in a standard 
 *    32-bit integer. However, the final accumulated sum can overflow, so we 
 *    MUST apply the modulo 10^9 + 7 at each addition).
 * 4. Can `left` and `right` cover the entire range?
 *    (Yes, 1 <= left <= right <= n*(n+1)/2).
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Brute Force (Sorting):
 *    - Generate all possible subarray sums using two nested loops.
 *    - Store them in a dynamically sized list or an array of size N*(N+1)/2.
 *    - Sort the array.
 *    - Iterate from `left - 1` to `right - 1` and accumulate the sum, applying 
 *      modulo 10^9 + 7 at each step.
 *    - Time Complexity: O(N^2 log(N^2)) -> O(N^2 log N). 
 *    - Space Complexity: O(N^2).
 *    - Is this acceptable? Yes. For N = 1000, N^2 / 2 = 500,500. Sorting 
 *      500,500 elements takes milliseconds in Java. This is perfectly valid.
 * 
 * 2. Priority Queue / Min-Heap (Optimal Memory):
 *    - Instead of generating and storing ALL 500,500 sums, what if we only 
 *      generate them as needed? 
 *    - We can use a Min-Heap (PriorityQueue).
 *    - Initially, push all single-element subarrays into the heap. (We store 
 *      the `sum` and the `ending index` of that subarray).
 *    - When we pop the smallest sum, we can form the *next* slightly larger 
 *      subarray by extending it one element to the right (if not at the end).
 *    - We do this exactly `right` times. We only add to our total when our 
 *      extraction count is between `left` and `right`.
 *    - Time Complexity: O(R log N) where R is the `right` index. Max R = N^2.
 *    - Space Complexity: O(N) because the heap never holds more than N items!
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Priority Queue Approach)
 * ============================================================================
 * Example: nums = [1, 2, 3, 4], left = 1, right = 5
 * 
 * Initial state (Heap contains all 1-length subarrays):
 * Heap: [(1, idx:0), (2, idx:1), (3, idx:2), (4, idx:3)]
 * 
 * Extraction 1:
 * Pop (1, idx:0). This is the smallest sum. 
 * Can we extend? Yes, next element is nums[1]=2.
 * Push (1+2=3, idx:1) into Heap.
 * Heap: [(2, idx:1), (3, idx:1), (3, idx:2), (4, idx:3)]
 * (Since we popped the 1st item, and 1 >= left, total_sum = 1)
 * 
 * Extraction 2:
 * Pop (2, idx:1). 
 * Extend with nums[2]=3. Push (2+3=5, idx:2).
 * Heap: [(3, idx:1), (3, idx:2), (4, idx:3), (5, idx:2)]
 * (total_sum = 1 + 2 = 3)
 * 
 * Extraction 3:
 * Pop (3, idx:1). 
 * Extend with nums[2]=3. Push (3+3=6, idx:2).
 * Heap: [(3, idx:2), (4, idx:3), (5, idx:2), (6, idx:2)]
 * (total_sum = 3 + 3 = 6)
 * 
 * Continue this until we've extracted `right` times. 
 * The heap guarantees we ALWAYS extract the next smallest subarray sum!
 */

import java.util.Arrays;
import java.util.PriorityQueue;

public class RangeSumSortedSubarraySums {

    private static final int MOD = 1_000_000_007;

    public static void main(String[] args) {
        int[] nums = {1, 2, 3, 4};
        int left = 1;
        int right = 5;

        System.out.println("Nums: " + Arrays.toString(nums) + ", left: " + left + ", right: " + right);
        System.out.println("Brute Force Solution: " + rangeSumBruteForce(nums.clone(), nums.length, left, right));
        System.out.println("Priority Queue Solution: " + rangeSumPQ(nums.clone(), nums.length, left, right));
        
        System.out.println("--------------------------------------------------");
        
        int[] nums2 = {1, 2, 3, 4};
        int left2 = 3;
        int right2 = 4;
        
        System.out.println("Nums: " + Arrays.toString(nums2) + ", left: " + left2 + ", right: " + right2);
        System.out.println("Brute Force Solution: " + rangeSumBruteForce(nums2.clone(), nums2.length, left2, right2));
        System.out.println("Priority Queue Solution: " + rangeSumPQ(nums2.clone(), nums2.length, left2, right2));
    }

    /**
     * SOLUTION 1: Brute Force (Generate All + Sort)
     * 
     * Idea: Generate every subarray sum, put them in a large array, sort it, 
     * and sum the requested range.
     * 
     * Time Complexity: O(N^2 log N)
     * Space Complexity: O(N^2)
     */
    public static int rangeSumBruteForce(int[] nums, int n, int left, int right) {
        // Total number of subarrays
        int totalSubarrays = n * (n + 1) / 2;
        int[] subarraySums = new int[totalSubarrays];
        int index = 0;
        
        // Generate all continuous subarray sums
        for (int i = 0; i < n; i++) {
            int currentSum = 0;
            for (int j = i; j < n; j++) {
                currentSum += nums[j];
                subarraySums[index++] = currentSum;
            }
        }
        
        // Sort the sums in non-decreasing order
        Arrays.sort(subarraySums);
        
        // Accumulate the sum from 'left' to 'right' (1-based indexing)
        long totalResult = 0;
        for (int i = left - 1; i < right; i++) {
            totalResult = (totalResult + subarraySums[i]) % MOD;
        }
        
        return (int) totalResult;
    }

    /**
     * SOLUTION 2: Priority Queue (Optimal Space & Modern Java)
     * 
     * Idea: Use a Min-Heap to dynamically generate the smallest subarray sums.
     * We utilize Java 16+ `record` to keep the code clean and readable.
     * 
     * Time Complexity: O(R log N) where R is the `right` bound.
     * Space Complexity: O(N) - Heap size never exceeds the length of the array.
     */
    public static int rangeSumPQ(int[] nums, int n, int left, int right) {
        // Record to hold the current sum and the index where this subarray ends.
        // Implementing Comparable to allow PriorityQueue to sort them correctly.
        record Subarray(int sum, int endIndex) implements Comparable<Subarray> {
            @Override
            public int compareTo(Subarray other) {
                return Integer.compare(this.sum, other.sum);
            }
        }

        PriorityQueue<Subarray> minHeap = new PriorityQueue<>();

        // Initialize heap with all single-element subarrays
        for (int i = 0; i < n; i++) {
            minHeap.offer(new Subarray(nums[i], i));
        }

        long totalResult = 0;

        // Extract exactly 'right' times
        for (int count = 1; count <= right; count++) {
            Subarray current = minHeap.poll();

            // Only add to total if we are within the [left, right] window
            if (count >= left) {
                totalResult = (totalResult + current.sum()) % MOD;
            }

            // If we can extend the subarray to the right, do so and add back to heap
            if (current.endIndex() + 1 < n) {
                int nextIndex = current.endIndex() + 1;
                int nextSum = current.sum() + nums[nextIndex];
                minHeap.offer(new Subarray(nextSum, nextIndex));
            }
        }

        return (int) totalResult;
    }
}
