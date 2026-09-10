/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an array of numbers `nums` and an array of target values `queries`.
 * For each query, we want to change every element in `nums` so that it equals 
 * the query value. 
 * We can only increment or decrement an element by 1 at a time (cost = 1).
 * We need to return an array containing the total minimum cost (number of 
 * operations) required for each query.
 * The `nums` array effectively resets to its original state after each query.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can the elements in `nums` or `queries` be negative? 
 *    (Constraints say positive integers >= 1).
 * 2. Can the total number of operations exceed the limit of a 32-bit integer?
 *    (Max N = 1000, Max diff = 10^5. Total max sum = 10^8, which fits in an `int`. 
 *    However, if constraints were standard competitive programming sizes like 10^5, 
 *    it would exceed 2 * 10^9. It is always safer to use `long` for sum of differences).
 * 3. Does the order of elements in `nums` matter?
 *    (No, the total number of operations is independent of the element order. 
 *    This hints that sorting the array might be a valid strategy).
 * 4. Are we modifying the array in-place for real?
 *    (No, the problem states the array is "reset", meaning we only need to 
 *    calculate the mathematical cost, not actually simulate the changes).
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Brute Force (The intuitive start):
 *    - For every query `q`, loop through all elements `x` in `nums`.
 *    - The cost to change `x` to `q` is simply the absolute difference: `|x - q|`.
 *    - Add up these differences for all elements.
 *    - Time Complexity: O(M * N). With constraints N, M <= 1000, 10^6 operations 
 *      is extremely fast and will easily pass.
 * 
 * 2. Sorting + Prefix Sum + Binary Search (The Optimal Approach for large constraints):
 *    - What if N and M were 10^5? O(M * N) would be 10^10 and time out!
 *    - Sort `nums`. 
 *    - For a query `q`, some elements are smaller than `q`, and some are larger.
 *      - Elements smaller than `q` need to be INCREASED. 
 *        Cost = (count_smaller * q) - (sum_of_smaller_elements)
 *      - Elements larger than `q` need to be DECREASED.
 *        Cost = (sum_of_larger_elements) - (count_larger * q)
 *    - We can use Binary Search to find the split point (how many are smaller).
 *    - We can use a Prefix Sum array to get the sum of any half in O(1) time.
 *    - Time Complexity: O(N log N + M log N). Space: O(N).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Optimal Approach)
 * ============================================================================
 * Example: nums = [3, 1, 6, 8], queries = [5]
 * 
 * 1. Sort nums -> [1, 3, 6, 8]
 * 2. Build Prefix Sum (1-indexed for easier math)
 *    prefix = [0, 1, 4, 10, 18]
 *    (prefix[i] is the sum of the first 'i' elements)
 * 
 * 3. Process Query: q = 5
 *    - Binary search for 5 in [1, 3, 6, 8].
 *    - The insertion point is index 2 (meaning 2 elements are strictly less than 5: [1, 3]).
 *    
 *    - Left Side (Smaller elements: [1, 3]):
 *      Count = 2
 *      Sum of these = prefix[2] = 4
 *      Cost to increase to 5 = (2 * 5) - 4 = 10 - 4 = 6.
 * 
 *    - Right Side (Larger/Equal elements: [6, 8]):
 *      Count = 4 - 2 = 2
 *      Sum of these = prefix[4] - prefix[2] = 18 - 4 = 14
 *      Cost to decrease to 5 = 14 - (2 * 5) = 14 - 10 = 4.
 * 
 *    - Total Cost = 6 + 4 = 10.
 */

import java.util.Arrays;

public class MinimumOperationsToMakeEqual {

    public static void main(String[] args) {
        int[] nums = {3, 1, 6, 8};
        int[] queries = {1, 5, 10};

        System.out.println("Nums: " + Arrays.toString(nums));
        System.out.println("Queries: " + Arrays.toString(queries));

        System.out.println("Solution 1 (Brute Force): " 
            + Arrays.toString(minOperationsBruteForce(nums.clone(), queries)));
            
        System.out.println("Solution 2 (Prefix + Binary Search): " 
            + Arrays.toString(minOperationsOptimal(nums.clone(), queries)));
    }

    /**
     * SOLUTION 1: Brute Force
     * 
     * Idea: Directly calculate the sum of absolute differences for each query.
     * Since N, M <= 1000, N*M <= 10^6, which easily executes within time limits.
     * 
     * Time Complexity: O(M * N)
     * Space Complexity: O(1) (excluding the output array)
     */
    public static long[] minOperationsBruteForce(int[] nums, int[] queries) {
        int m = queries.length;
        long[] answer = new long[m];
        
        for (int i = 0; i < m; i++) {
            long totalOperations = 0;
            long target = queries[i];
            
            for (int num : nums) {
                totalOperations += Math.abs(num - target);
            }
            
            answer[i] = totalOperations;
        }
        
        return answer;
    }

    /**
     * SOLUTION 2: Sorting + Prefix Sum + Binary Search (Optimal)
     * 
     * Idea: Sort the array. For each query, binary search to find how many 
     * elements are smaller than the query. Use a prefix sum array to calculate 
     * the total difference in O(1) time. 
     * This is the expected approach if constraints for N and M were up to 10^5.
     * 
     * Time Complexity: O(N log N + M log N)
     * Space Complexity: O(N) for the prefix sum array.
     */
    public static long[] minOperationsOptimal(int[] nums, int[] queries) {
        int n = nums.length;
        int m = queries.length;
        
        // 1. Sort the array
        Arrays.sort(nums);
        
        // 2. Build 1-indexed Prefix Sum Array
        // Using `long` to prevent overflow on sums.
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) {
            prefix[i + 1] = prefix[i] + nums[i];
        }
        
        long[] answer = new long[m];
        
        // 3. Process each query
        for (int i = 0; i < m; i++) {
            long target = queries[i];
            
            // Find the index of the first element >= target
            int idx = binarySearch(nums, target);
            
            // Elements to the left are strictly smaller than target.
            // Cost = (Count * target) - (Sum of elements)
            long leftCount = idx;
            long leftSum = prefix[idx];
            long leftCost = (leftCount * target) - leftSum;
            
            // Elements to the right (and at idx) are >= target.
            // Cost = (Sum of elements) - (Count * target)
            long rightCount = n - idx;
            long rightSum = prefix[n] - prefix[idx];
            long rightCost = rightSum - (rightCount * target);
            
            answer[i] = leftCost + rightCost;
        }
        
        return answer;
    }

    /**
     * Helper method to find the number of elements strictly less than the target.
     * This is equivalent to finding the insertion point of the target.
     * 
     * @param nums Sorted array of integers
     * @param target The query value
     * @return The count of elements strictly less than target
     */
    private static int binarySearch(int[] nums, long target) {
        int left = 0;
        int right = nums.length - 1;
        int result = nums.length; // Default to length if all elements are smaller
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            
            if (nums[mid] >= target) {
                result = mid;       // Potential first element >= target
                right = mid - 1;    // Keep searching left
            } else {
                left = mid + 1;     // Too small, search right
            }
        }
        
        return result;
    }
}
