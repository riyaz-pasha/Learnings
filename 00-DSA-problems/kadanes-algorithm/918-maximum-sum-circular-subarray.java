/*
 * Given a circular integer array nums of length n, return the maximum possible
 * sum of a non-empty subarray of nums.
 * 
 * A circular array means the end of the array connects to the beginning of the
 * array. Formally, the next element of nums[i] is nums[(i + 1) % n] and the
 * previous element of nums[i] is nums[(i - 1 + n) % n].
 * 
 * A subarray may only include each element of the fixed buffer nums at most
 * once. Formally, for a subarray nums[i], nums[i + 1], ..., nums[j], there does
 * not exist i <= k1, k2 <= j with k1 % n == k2 % n.
 * 
 * Example 1:
 * Input: nums = [1,-2,3,-2]
 * Output: 3
 * Explanation: Subarray [3] has maximum sum 3.
 * 
 * Example 2:
 * Input: nums = [5,-3,5]
 * Output: 10
 * Explanation: Subarray [5,5] has maximum sum 5 + 5 = 10.
 * 
 * Example 3:
 * Input: nums = [-3,-2,-3]
 * Output: -2
 * Explanation: Subarray [-2] has maximum sum -2.
 */

class Solution {

    // APPROACH 1: Two-Pass Kadane's Algorithm (Optimal) ⭐
    // Time: O(n), Space: O(1)
    public int maxSubarraySumCircular(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;

        // Case 1: Maximum subarray is non-circular (normal Kadane's)
        int maxKadane = kadaneMax(nums);

        // Case 2: Maximum subarray is circular
        // This equals: total_sum - minimum_subarray_sum
        int totalSum = 0;
        for (int num : nums) {
            totalSum += num;
        }

        int minKadane = kadaneMin(nums);
        int maxCircular = totalSum - minKadane;

        // Edge case: if all numbers are negative, maxCircular would be 0
        // but we need at least one element, so return maxKadane
        if (maxCircular == 0) {
            return maxKadane;
        }

        return Math.max(maxKadane, maxCircular);
    }

    // Standard Kadane's algorithm for maximum subarray
    private int kadaneMax(int[] nums) {
        int maxSum = nums[0];
        int currentSum = nums[0];

        for (int i = 1; i < nums.length; i++) {
            currentSum = Math.max(nums[i], currentSum + nums[i]);
            maxSum = Math.max(maxSum, currentSum);
        }

        return maxSum;
    }

    // Kadane's algorithm for minimum subarray
    private int kadaneMin(int[] nums) {
        int minSum = nums[0];
        int currentSum = nums[0];

        for (int i = 1; i < nums.length; i++) {
            currentSum = Math.min(nums[i], currentSum + nums[i]);
            minSum = Math.min(minSum, currentSum);
        }

        return minSum;
    }

}

// APPROACH 2: Single Pass with Two Kadane's simultaneously
// Time: O(n), Space: O(1)
class SolutionSinglePass {

    public int maxSubarraySumCircular(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;

        int totalSum = 0;
        int maxKadane = Integer.MIN_VALUE;
        int minKadane = Integer.MAX_VALUE;
        int currentMax = 0;
        int currentMin = 0;

        for (int num : nums) {
            totalSum += num;

            // Maximum subarray (standard Kadane's)
            currentMax = Math.max(currentMax + num, num);
            maxKadane = Math.max(maxKadane, currentMax);

            // Minimum subarray (Kadane's for minimum)
            currentMin = Math.min(currentMin + num, num);
            minKadane = Math.min(minKadane, currentMin);
        }

        // If all elements are negative, maxKadane is the answer
        if (maxKadane < 0) {
            return maxKadane;
        }

        // Return maximum of non-circular and circular cases
        return Math.max(maxKadane, totalSum - minKadane);
    }

}

// APPROACH 3: Brute Force with Circular Logic (for understanding)
// Time: O(n²), Space: O(1)
class SolutionBruteForce {

    public int maxSubarraySumCircular(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;

        int n = nums.length;
        int maxSum = Integer.MIN_VALUE;

        // Try all possible subarrays
        for (int i = 0; i < n; i++) {
            int currentSum = 0;
            // Try subarrays starting at i with length 1 to n
            for (int len = 1; len <= n; len++) {
                int idx = (i + len - 1) % n;
                currentSum += nums[idx];
                maxSum = Math.max(maxSum, currentSum);
            }
        }

        return maxSum;
    }

}

// APPROACH 4: Prefix and Suffix Arrays
// Time: O(n), Space: O(n)
class SolutionPrefixSuffix {

    public int maxSubarraySumCircular(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;

        int n = nums.length;

        // Case 1: Non-circular maximum subarray
        int maxKadane = kadaneMax(nums);

        // Case 2: Circular maximum subarray
        // Calculate prefix sums
        int[] prefixSum = new int[n];
        prefixSum[0] = nums[0];
        for (int i = 1; i < n; i++) {
            prefixSum[i] = prefixSum[i - 1] + nums[i];
        }

        // Calculate maximum prefix sum ending at each position
        int[] maxPrefix = new int[n];
        maxPrefix[0] = prefixSum[0];
        for (int i = 1; i < n; i++) {
            maxPrefix[i] = Math.max(maxPrefix[i - 1], prefixSum[i]);
        }

        // Calculate suffix sums and find maximum circular sum
        int maxCircular = Integer.MIN_VALUE;
        int suffixSum = 0;

        for (int i = n - 1; i > 0; i--) {
            suffixSum += nums[i];
            maxCircular = Math.max(maxCircular, suffixSum + maxPrefix[i - 1]);
        }

        return Math.max(maxKadane, maxCircular);
    }

    private int kadaneMax(int[] nums) {
        int maxSum = nums[0];
        int currentSum = nums[0];

        for (int i = 1; i < nums.length; i++) {
            currentSum = Math.max(nums[i], currentSum + nums[i]);
            maxSum = Math.max(maxSum, currentSum);
        }

        return maxSum;
    }

}

// APPROACH 5: Deque-based solution (Advanced)
// Time: O(n), Space: O(n)
class SolutionDeque {

    public int maxSubarraySumCircular(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;

        int n = nums.length;

        // Case 1: Non-circular
        int maxNonCircular = kadaneMax(nums);

        // Case 2: Circular - use prefix sums with deque
        int[] prefixSum = new int[2 * n + 1];
        for (int i = 0; i < 2 * n; i++) {
            prefixSum[i + 1] = prefixSum[i] + nums[i % n];
        }

        java.util.Deque<Integer> deque = new java.util.ArrayDeque<>();
        int maxCircular = Integer.MIN_VALUE;

        for (int i = 0; i < 2 * n; i++) {
            // Remove elements outside the window of size n
            while (!deque.isEmpty() && deque.peekFirst() < i - n) {
                deque.pollFirst();
            }

            // Calculate maximum sum ending at current position
            if (!deque.isEmpty()) {
                maxCircular = Math.max(maxCircular,
                        prefixSum[i + 1] - prefixSum[deque.peekFirst()]);
            }

            // Maintain deque in ascending order of prefix sums
            while (!deque.isEmpty() &&
                    prefixSum[deque.peekLast()] >= prefixSum[i]) {
                deque.pollLast();
            }
            deque.offerLast(i);
        }

        return Math.max(maxNonCircular, maxCircular);
    }

    private int kadaneMax(int[] nums) {
        int maxSum = nums[0];
        int currentSum = nums[0];

        for (int i = 1; i < nums.length; i++) {
            currentSum = Math.max(nums[i], currentSum + nums[i]);
            maxSum = Math.max(maxSum, currentSum);
        }

        return maxSum;
    }

}

// Test class to demonstrate all approaches
class TestMaxCircularSubarray {

    public static void main(String[] args) {
        Solution solution = new Solution();
        SolutionSinglePass singlePass = new SolutionSinglePass();

        // Test cases
        int[][] testCases = {
                { 1, -2, 3, -2 }, // Expected: 3 (subarray [3])
                { 5, -3, 5 }, // Expected: 10 (circular [5,5])
                { -3, -2, -3 }, // Expected: -2 (subarray [-2])
                { 3, -1, 2, -1 }, // Expected: 4 (circular [2,-1,3])
                { 3, -2, 2, -3 }, // Expected: 3 (subarray [3] or [2])
                { -2, -3, -1 }, // Expected: -1 (all negative)
                { 1, -2, 3, -2, 5 } // Expected: 6 (subarray [3,-2,5])
        };

        for (int i = 0; i < testCases.length; i++) {
            int[] nums = testCases[i];
            System.out.println("\nTest Case " + (i + 1) + ": " +
                    java.util.Arrays.toString(nums));

            int result1 = solution.maxSubarraySumCircular(nums);
            int result2 = singlePass.maxSubarraySumCircular(nums);

            System.out.println("Two-pass result: " + result1);
            System.out.println("Single-pass result: " + result2);

            // Verify both approaches give same result
            if (result1 != result2) {
                System.out.println("⚠️  Results don't match!");
            }
        }

        // Demonstrate the key insight
        demonstrateKeyInsight();
    }

    private static void demonstrateKeyInsight() {
        System.out.println("\n--- Key Insight Demonstration ---");
        int[] nums = { 5, -3, 5 };
        System.out.println("Array: " + java.util.Arrays.toString(nums));

        // Calculate components
        int totalSum = 0;
        for (int num : nums)
            totalSum += num;

        Solution sol = new Solution();
        int maxNormal = sol.kadaneMax(nums);
        int minSubarray = sol.kadaneMin(nums);
        int maxCircular = totalSum - minSubarray;

        System.out.println("Total sum: " + totalSum);
        System.out.println("Max normal subarray: " + maxNormal);
        System.out.println("Min subarray: " + minSubarray);
        System.out.println("Max circular (total - min): " + maxCircular);
        System.out.println("Final answer: " + Math.max(maxNormal, maxCircular));
    }

}

// Utility class for visualization
class CircularArrayVisualizer {

    public static void visualizeCircular(int[] nums, int start, int length) {
        System.out.print("Circular subarray: [");
        for (int i = 0; i < length; i++) {
            int idx = (start + i) % nums.length;
            System.out.print(nums[idx]);
            if (i < length - 1)
                System.out.print(", ");
        }
        System.out.println("]");
    }

    public static void printArrayAsCircular(int[] nums) {
        System.out.print("Circular view: ");
        for (int i = 0; i < nums.length; i++) {
            System.out.print(nums[i]);
            if (i < nums.length - 1)
                System.out.print(" -> ");
        }
        System.out.println(" -> " + nums[0] + " (wraps around)");
    }

}

/**
 * 📌 Problem: Maximum Sum Circular Subarray
 * 
 * Given a circular array, find the maximum possible sum of a non-empty
 * subarray.
 * The subarray can wrap around the end to the beginning, but each element can
 * only be included once.
 * 
 * 🧠 Key Idea:
 * There are two possible scenarios to get the maximum sum:
 * 
 * ✅ Case 1: Normal subarray (no wrap-around)
 * - Use Kadane’s Algorithm to find the maximum sum subarray as usual.
 * 
 * 🔁 Case 2: Circular subarray (wrap-around)
 * - Think of wrapping around as removing a contiguous "bad" (minimum sum)
 * subarray.
 * - Formula: circularMax = totalSum - minSubarraySum
 * - This effectively gives the sum of the rest of the array — the circular
 * part.
 * 
 * ⚠️ Edge Case:
 * - If all numbers in the array are negative, then totalSum == minSubarraySum.
 * - In that case, circularMax would be 0, which is invalid (you must choose at
 * least one element).
 * - So, if all numbers are negative, return the result from normal Kadane’s
 * Algorithm.
 * 
 * 🪜 Step-by-Step Approach:
 * 1. Run Kadane’s Algorithm to get maxSubarraySum.
 * 2. Run modified Kadane’s to get minSubarraySum.
 * 3. Calculate totalSum of the array.
 * 4. If maxSubarraySum < 0 → return it (all elements are negative).
 * 5. Else → return max(maxSubarraySum, totalSum - minSubarraySum).
 * 
 * 💡 Example 1: [1, -2, 3, -2]
 * - Normal max subarray: [3] → sum = 3
 * - Total sum: 0
 * - Min subarray: [-2, 3, -2] → sum = -1
 * - Circular max: 0 - (-1) = 1
 * - Final result: max(3, 1) = 3
 * 
 * 💡 Example 2: [5, -3, 5]
 * - Normal max: [5, -3, 5] = 7
 * - Total = 7, Min subarray = -3
 * - Circular max = 10
 * - Final result = max(7, 10) = 10
 * 
 * 💡 Example 3: [-3, -2, -1]
 * - All negative → return max of them = -1
 */
