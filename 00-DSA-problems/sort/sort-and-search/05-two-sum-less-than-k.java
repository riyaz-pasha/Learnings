/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an array of integers and a target integer `k`. 
 * We need to find two distinct elements in the array such that their sum is 
 * strictly less than `k`. Among all such valid pairs, we must return the 
 * maximum possible sum. If no two elements can form a sum less than `k`, 
 * we must return -1.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can the input array have fewer than 2 elements? 
 *    (The constraints say length >= 1, so if length is 1, we must return -1).
 * 2. Can the elements or `k` be negative? 
 *    (Constraints say nums[i] >= 1 and k >= 1, so all are positive).
 * 3. Does "distinct elements" mean distinct values or distinct indices?
 *    (Usually it means distinct indices. e.g., using nums[0] and nums[1] even if they have the same value).
 * 4. Is the array sorted? 
 *    (No, the input is unsorted).
 * 5. Are we allowed to modify the input array? 
 *    (Sorting it in place might be the most efficient approach. If not, we can clone it).
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Brute Force (The baseline):
 *    - Use two nested loops to check every possible pair of elements (i, j) where i < j.
 *    - Calculate their sum. If sum < k, update our tracked maximum sum.
 *    - Time: O(N^2), Space: O(1). Given N <= 100, N^2 = 10,000 operations, which 
 *      is practically instantaneous. This is perfectly viable for the constraints.
 * 
 * 2. Two Pointers (The optimal):
 *    - Sort the array first.
 *    - Place one pointer at the beginning (`left = 0`) and one at the end (`right = N - 1`).
 *    - If `nums[left] + nums[right] >= k`, the sum is too large. Since the array is 
 *      sorted, moving `left` rightward will only increase the sum further. 
 *      So, we must move `right` leftward (`right--`) to decrease the sum.
 *    - If `nums[left] + nums[right] < k`, we found a valid sum! We update our 
 *      max sum. Since we want an even larger valid sum, we move `left` rightward (`left++`).
 *    - Time: O(N log N) for sorting, O(N) for traversal -> Total O(N log N).
 *    - Space: O(1) auxiliary space (excluding the sort implementation).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Two Pointers Approach)
 * ============================================================================
 * Example: nums = [34, 23, 1, 24, 75, 33, 54, 8], k = 60
 * 
 * 1. Sort the array:
 *    [1, 8, 23, 24, 33, 34, 54, 75]
 * 
 * 2. Initialize left = 0 (val 1), right = 7 (val 75), maxSum = -1.
 * 
 * 3. Loop:
 *    - nums[0] + nums[7] = 1 + 75 = 76. 
 *      76 >= 60. Too big. right-- (right becomes 6).
 *    
 *    - nums[0] + nums[6] = 1 + 54 = 55. 
 *      55 < 60. Valid! maxSum = 55. We want bigger, so left++ (left becomes 1).
 * 
 *    - nums[1] + nums[6] = 8 + 54 = 62.
 *      62 >= 60. Too big. right-- (right becomes 5).
 * 
 *    - nums[1] + nums[5] = 8 + 34 = 42.
 *      42 < 60, but 42 < 55, so maxSum remains 55. left++ (left becomes 2).
 * 
 *    - nums[2] + nums[5] = 23 + 34 = 57.
 *      57 < 60. Valid! 57 > 55, so maxSum = 57. left++ (left becomes 3).
 * 
 *    - nums[3] + nums[5] = 24 + 34 = 58.
 *      58 < 60. Valid! 58 > 57, so maxSum = 58. left++ (left becomes 4).
 * 
 *    - nums[4] + nums[5] = 33 + 34 = 67.
 *      67 >= 60. Too big. right-- (right becomes 4).
 * 
 * 4. left (4) is no longer < right (4). Loop ends.
 * 5. Return maxSum = 58.
 */

import java.util.Arrays;

public class MaxSumLessThanK {

    public static void main(String[] args) {
        int[] nums1 = {34, 23, 1, 24, 75, 33, 54, 8};
        int k1 = 60;

        int[] nums2 = {10, 20, 30};
        int k2 = 15;

        System.out.println("Input: " + Arrays.toString(nums1) + ", k = " + k1);
        System.out.println("Solution 1 (Brute Force): " + maxSumBruteForce(nums1, k1));
        System.out.println("Solution 2 (Two Pointers): " + maxSumTwoPointers(nums1.clone(), k1));
        System.out.println("--------------------------------------------------");

        System.out.println("Input: " + Arrays.toString(nums2) + ", k = " + k2);
        System.out.println("Solution 1 (Brute Force): " + maxSumBruteForce(nums2, k2));
        System.out.println("Solution 2 (Two Pointers): " + maxSumTwoPointers(nums2.clone(), k2));
    }

    /**
     * SOLUTION 1: Brute Force
     * 
     * Idea: Test every possible unique pair of indices (i, j) where i < j. 
     * Keep track of the maximum sum that is strictly less than k.
     * 
     * Time Complexity: O(N^2) - Two nested loops over the array.
     * Space Complexity: O(1) - Constant extra space used.
     */
    public static int maxSumBruteForce(int[] nums, int k) {
        if (nums == null || nums.length < 2) {
            return -1;
        }

        int maxSum = -1;

        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                int sum = nums[i] + nums[j];
                if (sum < k && sum > maxSum) {
                    maxSum = sum;
                }
            }
        }

        return maxSum;
    }

    /**
     * SOLUTION 2: Two Pointers (Optimal)
     * 
     * Idea: Sort the array first. Place one pointer at the start and one at the end.
     * Adjust the pointers inward based on whether the current sum is too large or 
     * small relative to `k`.
     * 
     * Time Complexity: O(N log N) - Dominated by the sorting step. The two-pointer 
     *                  traversal takes O(N) time.
     * Space Complexity: O(1) or O(N) depending on the sorting algorithm used internally 
     *                   by Arrays.sort().
     */
    public static int maxSumTwoPointers(int[] nums, int k) {
        if (nums == null || nums.length < 2) {
            return -1;
        }

        Arrays.sort(nums);

        int left = 0;
        int right = nums.length - 1;
        int maxSum = -1;

        while (left < right) {
            int sum = nums[left] + nums[right];
            
            if (sum < k) {
                // Since the sum is strictly less than k, it's a valid candidate.
                // Update maxSum if this sum is larger than our previous best.
                maxSum = Math.max(maxSum, sum);
                
                // To find a potentially larger valid sum, we must increase the 
                // smaller component of the sum by moving `left` to the right.
                left++;
            } else {
                // The sum is >= k, which is invalid. Since the array is sorted, 
                // the only way to decrease the sum is to move `right` to the left.
                right--;
            }
        }

        return maxSum;
    }
}
