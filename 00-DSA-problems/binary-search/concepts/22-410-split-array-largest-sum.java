import java.util.Arrays;

/*
 * Problem Statement: Given an integer array ‘A’ of size ‘N’ and an integer ‘K'.
 * Split the array ‘A’ into ‘K’ non-empty subarrays such that the largest sum of
 * any subarray is minimized. Your task is to return the minimized largest sum
 * of the split.
 * A subarray is a contiguous part of the array.
 * 
 * Examples
 * Example 1:
 * Input Format: N = 5, a[] = {1,2,3,4,5}, k = 3
 * Result: 6
 * Explanation: There are many ways to split the array a[] into k consecutive
 * subarrays. The best way to do this is to split the array a[] into [1, 2, 3],
 * [4], and [5], where the largest sum among the three subarrays is only 6.
 * 
 * Example 2:
 * Input Format: N = 3, a[] = {3,5,1}, k = 3
 * Result: 5
 * Explanation: There is only one way to split the array a[] into 3 subarrays,
 * i.e., [3], [5], and [1]. The largest sum among these subarrays is 5.
 */

class SplitArrayLargestSum {

    /**
     * Problem (LC 410):
     * Split the array into exactly k non-empty contiguous subarrays
     * such that the largest subarray sum is minimized.
     *
     * Return that minimized largest subarray sum.
     *
     * Example:
     * nums = [7,2,5,10,8], k = 2
     *
     * Possible split:
     *   [7,2,5] and [10,8]
     *   sums = 14 and 18
     *   largest = 18
     *
     * Answer = 18
     *
     * ---------------------------------------------------------
     * Why Binary Search on Answer?
     *
     * We are minimizing the "largest subarray sum".
     *
     * If we choose a value X as the maximum allowed subarray sum,
     * we can check:
     *
     *   "Can we split the array into <= k subarrays such that each
     *    subarray sum is <= X?"
     *
     * Monotonic property:
     *   - If X works, then any larger X will also work.
     *   - If X does not work, any smaller X will also fail.
     *
     * So feasibility is monotonic:
     *   false false false true true true ...
     *
     * Hence binary search on X.
     *
     * ---------------------------------------------------------
     * Search Space:
     * low  = max(nums)   (because no subarray can have sum < max element)
     * high = sum(nums)   (one subarray containing everything)
     *
     * ---------------------------------------------------------
     * Time Complexity:
     *   O(n log(sum(nums)))
     *
     * Space Complexity:
     *   O(1)
     */
    public int largestSubarraySumMinimized(int[] nums, int k) {

        // If k > n, impossible because each subarray must be non-empty
        if (k > nums.length) {
            return -1;
        }

        int low = Arrays.stream(nums).max().getAsInt();  // minimum possible answer
        int high = Arrays.stream(nums).sum();            // maximum possible answer

        // Binary search for the FIRST feasible maximum subarray sum
        while (low <= high) {

            int mid = low + (high - low) / 2;

            // If max allowed sum is mid, how many subarrays do we need?
            int subarraysNeeded = getSubArraysNeededCount(nums, mid);

            // If we need MORE than k subarrays, it means mid is too small.
            // We must increase max allowed sum.
            if (subarraysNeeded > k) {
                low = mid + 1;
            }
            // If we can split into <= k subarrays, mid is feasible.
            // Try smaller value to minimize answer.
            else {
                high = mid - 1;
            }
        }

        // low will be the minimum feasible maximum subarray sum
        return low;
    }

    /**
     * Given a limit maxSum, compute how many subarrays are required
     * if we split greedily.
     *
     * Greedy logic:
     * - Keep adding elements to current subarray until adding next element
     *   would exceed maxSum.
     * - Then start a new subarray.
     *
     * This greedy approach creates the MINIMUM number of subarrays
     * possible for that maxSum.
     *
     * Example:
     * nums = [7,2,5,10,8], maxSum = 18
     *
     * subarray1: 7+2+5 = 14
     * next add 10 -> would become 24 (exceeds 18) => new subarray
     * subarray2: 10+8 = 18
     *
     * subarraysNeeded = 2
     *
     * Time Complexity: O(n)
     */
    private int getSubArraysNeededCount(int[] nums, int maxSum) {

        int count = 1;        // start with first subarray
        long currentSum = 0;  // sum of current subarray

        for (int num : nums) {

            // if we can include num in current subarray
            if (currentSum + num <= maxSum) {
                currentSum += num;
            }
            // else we need a new subarray starting at num
            else {
                count++;
                currentSum = num;
            }
        }

        return count;
    }
}
