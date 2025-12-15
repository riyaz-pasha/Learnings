/*
 * Given an array of integers nums and an integer k, return the total number of
 * subarrays whose sum equals to k.
 * 
 * A subarray is a contiguous non-empty sequence of elements within an array.
 * 
 * 
 * 
 * Example 1:
 * 
 * Input: nums = [1,1,1], k = 2
 * Output: 2
 * Example 2:
 * 
 * Input: nums = [1,2,3], k = 3
 * Output: 2
 */

import java.util.HashMap;
import java.util.Map;

class SubarraySumEqualsK {

    // Solution 1: HashMap with Prefix Sum (Optimal)
    // Time: O(n), Space: O(n)
    public int subarraySum1(int[] nums, int k) {
        Map<Integer, Integer> prefixSumCount = new HashMap<>();
        prefixSumCount.put(0, 1); // Base case: empty prefix

        int count = 0;
        int prefixSum = 0;

        for (int num : nums) {
            prefixSum += num;

            // Check if (prefixSum - k) exists in map
            // If exists, means there's a subarray ending at current index with sum k
            if (prefixSumCount.containsKey(prefixSum - k)) {
                count += prefixSumCount.get(prefixSum - k);
            }

            // Add current prefix sum to map
            prefixSumCount.put(prefixSum, prefixSumCount.getOrDefault(prefixSum, 0) + 1);
        }

        return count;
    }

    // Solution 2: Detailed HashMap with Comments
    // Time: O(n), Space: O(n)
    public int subarraySum2(int[] nums, int k) {
        /*
         * Key Insight:
         * If prefixSum[j] - prefixSum[i] = k
         * Then sum of subarray from (i+1) to j equals k
         * 
         * Rearranging: prefixSum[i] = prefixSum[j] - k
         * So we look for (currentSum - k) in our map
         */

        Map<Integer, Integer> map = new HashMap<>();
        map.put(0, 1); // Empty subarray has sum 0

        int count = 0;
        int currentSum = 0;

        for (int i = 0; i < nums.length; i++) {
            currentSum += nums[i];

            // If (currentSum - k) exists, we found subarray(s) with sum k
            int target = currentSum - k;
            if (map.containsKey(target)) {
                count += map.get(target);
            }

            // Store current prefix sum
            map.put(currentSum, map.getOrDefault(currentSum, 0) + 1);
        }

        return count;
    }

    // Solution 3: Brute Force (For Understanding)
    // Time: O(n²), Space: O(1)
    public int subarraySum3(int[] nums, int k) {
        int count = 0;

        // Try all possible subarrays
        for (int start = 0; start < nums.length; start++) {
            int sum = 0;
            for (int end = start; end < nums.length; end++) {
                sum += nums[end];
                if (sum == k) {
                    count++;
                }
            }
        }

        return count;
    }

    // Solution 4: HashMap with Visualization Helper
    // Time: O(n), Space: O(n)
    public int subarraySum4(int[] nums, int k) {
        Map<Integer, Integer> prefixMap = new HashMap<>();
        prefixMap.put(0, 1);

        int result = 0;
        int sum = 0;

        for (int i = 0; i < nums.length; i++) {
            sum += nums[i];

            // Look for complement
            int complement = sum - k;
            result += prefixMap.getOrDefault(complement, 0);

            // Update map
            prefixMap.put(sum, prefixMap.getOrDefault(sum, 0) + 1);
        }

        return result;
    }

    // Helper: Print detailed explanation for an example
    private static void explainExample(int[] nums, int k) {
        System.out.println("\n=== Detailed Walkthrough ===");
        System.out.print("Array: [");
        for (int i = 0; i < nums.length; i++) {
            System.out.print(nums[i]);
            if (i < nums.length - 1)
                System.out.print(", ");
        }
        System.out.println("], k = " + k);

        Map<Integer, Integer> map = new HashMap<>();
        map.put(0, 1);
        int count = 0;
        int sum = 0;

        System.out.println("\nStep-by-step:");
        for (int i = 0; i < nums.length; i++) {
            sum += nums[i];
            int target = sum - k;

            System.out.println("Index " + i + ": num=" + nums[i] +
                    ", prefixSum=" + sum +
                    ", looking for " + target);

            if (map.containsKey(target)) {
                int occurrences = map.get(target);
                count += occurrences;
                System.out.println("  Found " + occurrences + " subarray(s)! Total count: " + count);
            }

            map.put(sum, map.getOrDefault(sum, 0) + 1);
            System.out.println("  Map: " + map);
        }

        System.out.println("\nFinal count: " + count);
    }

    // Helper: Print array
    private static void printArray(int[] nums) {
        System.out.print("[");
        for (int i = 0; i < nums.length; i++) {
            System.out.print(nums[i]);
            if (i < nums.length - 1)
                System.out.print(", ");
        }
        System.out.print("]");
    }

    // Test cases
    public static void main(String[] args) {
        SubarraySumEqualsK solution = new SubarraySumEqualsK();

        // Test case 1
        int[] nums1 = { 1, 1, 1 };
        int k1 = 2;
        System.out.print("Example 1 - Input: nums = ");
        printArray(nums1);
        System.out.println(", k = " + k1);
        System.out.println("Output: " + solution.subarraySum1(nums1, k1));
        System.out.println("Subarrays: [1,1] at indices (0,1) and [1,1] at indices (1,2)");

        // Test case 2
        int[] nums2 = { 1, 2, 3 };
        int k2 = 3;
        System.out.print("\nExample 2 - Input: nums = ");
        printArray(nums2);
        System.out.println(", k = " + k2);
        System.out.println("Output: " + solution.subarraySum1(nums2, k2));
        System.out.println("Subarrays: [3] at index (2) and [1,2] at indices (0,1)");

        // Test case 3: With negatives
        int[] nums3 = { 1, -1, 1, 1, 1 };
        int k3 = 2;
        System.out.print("\nWith negatives - Input: nums = ");
        printArray(nums3);
        System.out.println(", k = " + k3);
        System.out.println("Output: " + solution.subarraySum2(nums3, k3));

        // Test case 4: No subarrays
        int[] nums4 = { 1, 2, 3 };
        int k4 = 7;
        System.out.print("\nNo match - Input: nums = ");
        printArray(nums4);
        System.out.println(", k = " + k4);
        System.out.println("Output: " + solution.subarraySum1(nums4, k4));

        // Test case 5: All elements sum to k
        int[] nums5 = { 1, 2, 3 };
        int k5 = 6;
        System.out.print("\nFull array - Input: nums = ");
        printArray(nums5);
        System.out.println(", k = " + k5);
        System.out.println("Output: " + solution.subarraySum1(nums5, k5));

        // Test case 6: Single element
        int[] nums6 = { 5 };
        int k6 = 5;
        System.out.print("\nSingle element - Input: nums = ");
        printArray(nums6);
        System.out.println(", k = " + k6);
        System.out.println("Output: " + solution.subarraySum1(nums6, k6));

        // Detailed walkthrough
        explainExample(new int[] { 1, 1, 1 }, 2);

        // Compare solutions
        System.out.println("\n=== Performance Comparison ===");
        int[] testArray = { 1, 2, 3, 4, 5 };
        int testK = 5;
        System.out.print("Test array: ");
        printArray(testArray);
        System.out.println(", k = " + testK);
        System.out.println("Solution 1 (HashMap): " + solution.subarraySum1(testArray, testK));
        System.out.println("Solution 2 (Detailed): " + solution.subarraySum2(testArray, testK));
        System.out.println("Solution 3 (Brute Force): " + solution.subarraySum3(testArray, testK));
        System.out.println("Solution 4 (Visual): " + solution.subarraySum4(testArray, testK));
    }
}
/*
 * Key Insight - Prefix Sum:
 * The core idea is:
 * 
 * If prefixSum[j] - prefixSum[i] = k, then the subarray from (i+1) to j has sum
 * k
 * Rearranging: prefixSum[i] = prefixSum[j] - k
 * So we look for (currentSum - k) in our HashMap
 * 
 * Visual Example:
 * Array: [1, 1, 1], k = 2
 * 
 * Index 0: num=1, prefixSum=1, looking for -1
 * Map: {0=1, 1=1}
 * 
 * Index 1: num=1, prefixSum=2, looking for 0
 * Found 1 subarray! [1,1] from index 0-1
 * Map: {0=1, 1=1, 2=1}
 * 
 * Index 2: num=1, prefixSum=3, looking for 1
 * Found 1 subarray! [1,1] from index 1-2
 * Map: {0=1, 1=1, 2=1, 3=1}
 * 
 * Result: 2
 */
