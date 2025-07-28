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

    public int largestSubarraySumMinimized(int[] nums, int subarraysCount) {
        if (subarraysCount > nums.length) {
            return -1;
        }

        int low = Arrays.stream(nums).max().getAsInt();
        int high = Arrays.stream(nums).sum();

        while (low <= high) {
            int mid = low + (high - low) / 2;
            int subarrayNeeded = this.getSubArraysNeededCount(nums, mid);
            if (subarrayNeeded > subarraysCount) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return low;
    }

    private int getSubArraysNeededCount(int[] nums, int maxSum) {
        int subarraysCount = 1;
        long subarraySum = 0;
        for (int num : nums) {
            if (subarraySum + num <= maxSum) {
                subarraySum += num;
            } else {
                subarraysCount++;
                subarraySum = num;
            }
        }

        return subarraysCount;
    }

}
