/*
 * Given an integer array nums, find the subarray with the largest sum, and
 * return its sum.
 * 
 * Example 1:
 * Input: nums = [-2,1,-3,4,-1,2,1,-5,4]
 * Output: 6
 * Explanation: The subarray [4,-1,2,1] has the largest sum 6.
 * 
 * Example 2:
 * Input: nums = [1]
 * Output: 1
 * Explanation: The subarray [1] has the largest sum 1.
 * 
 * Example 3:
 * Input: nums = [5,4,-1,7,8]
 * Output: 23
 * Explanation: The subarray [5,4,-1,7,8] has the largest sum 23.
 */

class Solution {

    // APPROACH 1: Kadane's Algorithm (Optimal) ⭐
    // Time: O(n), Space: O(1)
    public int maxSubArray(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;

        int maxSum = nums[0];
        int currentSum = nums[0];

        for (int i = 1; i < nums.length; i++) {
            // Either extend the existing subarray or start a new one
            currentSum = Math.max(nums[i], currentSum + nums[i]);
            maxSum = Math.max(maxSum, currentSum);
        }

        return maxSum;
    }

    public int maxSubArray2(int[] nums) {
        int max = Integer.MIN_VALUE;
        int sum = 0;
        for (int num : nums) {
            sum += num;
            max = Math.max(sum, max);
            sum = Math.max(0, sum);
        }
        return max;
    }

}

// APPROACH 2: Kadane's Algorithm with tracking indices
// Time: O(n), Space: O(1)
class SolutionWithIndices {

    public int maxSubArray(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;

        int maxSum = nums[0];
        int currentSum = nums[0];
        int start = 0, end = 0, tempStart = 0;

        for (int i = 1; i < nums.length; i++) {
            if (currentSum < 0) {
                currentSum = nums[i];
                tempStart = i; // Potential new start
            } else {
                currentSum += nums[i];
            }

            if (currentSum > maxSum) {
                maxSum = currentSum;
                start = tempStart;
                end = i;
            }
        }

        System.out.println("Max subarray is from index " + start + " to " + end);
        return maxSum;
    }

}

// APPROACH 3: Dynamic Programming (Clearer DP formulation)
// Time: O(n), Space: O(1)
class SolutionDP {

    public int maxSubArray(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;

        int n = nums.length;
        int dp = nums[0]; // dp represents max sum ending at current position
        int maxSum = nums[0];

        for (int i = 1; i < n; i++) {
            // DP recurrence: dp[i] = max(nums[i], dp[i-1] + nums[i])
            dp = Math.max(nums[i], dp + nums[i]);
            maxSum = Math.max(maxSum, dp);
        }

        return maxSum;
    }

}

// APPROACH 4: Divide and Conquer
// Time: O(n log n), Space: O(log n)
class SolutionDivideConquer {

    public int maxSubArray(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;
        return maxSubArrayHelper(nums, 0, nums.length - 1);
    }

    private int maxSubArrayHelper(int[] nums, int left, int right) {
        if (left == right)
            return nums[left];

        int mid = left + (right - left) / 2;

        // Max subarray is either in left half, right half, or crosses the middle
        int leftMax = maxSubArrayHelper(nums, left, mid);
        int rightMax = maxSubArrayHelper(nums, mid + 1, right);
        int crossMax = maxCrossingSum(nums, left, mid, right);

        return Math.max(Math.max(leftMax, rightMax), crossMax);
    }

    private int maxCrossingSum(int[] nums, int left, int mid, int right) {
        // Find max sum for left side ending at mid
        int leftSum = Integer.MIN_VALUE;
        int sum = 0;
        for (int i = mid; i >= left; i--) {
            sum += nums[i];
            leftSum = Math.max(leftSum, sum);
        }

        // Find max sum for right side starting at mid+1
        int rightSum = Integer.MIN_VALUE;
        sum = 0;
        for (int i = mid + 1; i <= right; i++) {
            sum += nums[i];
            rightSum = Math.max(rightSum, sum);
        }

        return leftSum + rightSum;
    }

}

// APPROACH 5: Brute Force (for understanding)
// Time: O(n²), Space: O(1)
class SolutionBruteForce {

    public int maxSubArray(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;

        int maxSum = Integer.MIN_VALUE;

        for (int i = 0; i < nums.length; i++) {
            int currentSum = 0;
            for (int j = i; j < nums.length; j++) {
                currentSum += nums[j];
                maxSum = Math.max(maxSum, currentSum);
            }
        }

        return maxSum;
    }

}

// APPROACH 6: Prefix Sum approach
// Time: O(n), Space: O(1)
class SolutionPrefixSum {

    public int maxSubArray(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;

        int maxSum = nums[0];
        int minPrefixSum = 0;
        int prefixSum = 0;

        for (int num : nums) {
            prefixSum += num;
            maxSum = Math.max(maxSum, prefixSum - minPrefixSum);
            minPrefixSum = Math.min(minPrefixSum, prefixSum);
        }

        return maxSum;
    }

}

// Test class to demonstrate all approaches
class TestMaxSubarray {

    public static void main(String[] args) {
        Solution solution = new Solution();
        SolutionWithIndices solutionIndices = new SolutionWithIndices();

        // Test cases
        int[][] testCases = {
                { -2, 1, -3, 4, -1, 2, 1, -5, 4 }, // Expected: 6
                { 1 }, // Expected: 1
                { 5, 4, -1, 7, 8 }, // Expected: 23
                { -2, -3, -1, -5 }, // Expected: -1 (all negative)
                { -1 } // Expected: -1
        };

        for (int i = 0; i < testCases.length; i++) {
            int[] nums = testCases[i];
            System.out.println("\nTest Case " + (i + 1) + ": " + java.util.Arrays.toString(nums));

            int result = solution.maxSubArray(nums);
            System.out.println("Kadane's Algorithm Result: " + result);

            int resultWithIndices = solutionIndices.maxSubArray(nums.clone());
            System.out.println("Result with indices: " + resultWithIndices);
        }

        // Performance comparison example
        performanceTest();
    }

    private static void performanceTest() {
        System.out.println("\n--- Performance Test ---");
        int[] largeArray = new int[100000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = (int) (Math.random() * 200) - 100; // Random numbers -100 to 99
        }

        Solution kadane = new Solution();
        SolutionBruteForce bruteForce = new SolutionBruteForce();

        // Test Kadane's Algorithm
        long start = System.nanoTime();
        int kadaneResult = kadane.maxSubArray(largeArray);
        long kadaneTime = System.nanoTime() - start;

        System.out.println("Kadane's Algorithm: " + kadaneResult + " (Time: " + kadaneTime / 1000000 + " ms)");

        // Note: Brute force would be too slow for large arrays
        // System.out.println("Brute Force: " + bruteForce.maxSubArray(largeArray));
    }

}

// Utility class for array operations
class ArrayUtils {

    public static void printSubarray(int[] nums, int start, int end) {
        System.out.print("Subarray [" + start + ", " + end + "]: [");
        for (int i = start; i <= end; i++) {
            System.out.print(nums[i]);
            if (i < end)
                System.out.print(", ");
        }
        System.out.println("]");
    }

    public static int sumSubarray(int[] nums, int start, int end) {
        int sum = 0;
        for (int i = start; i <= end; i++) {
            sum += nums[i];
        }
        return sum;
    }

}
