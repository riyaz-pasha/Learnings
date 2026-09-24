import java.util.*;
/*
 * Given an array of integers nums and an integer target, return indices of the
 * two numbers such that they add up to target.
 * 
 * You may assume that each input would have exactly one solution, and you may
 * not use the same element twice.
 * 
 * You can return the answer in any order.
 * 
 * Example 1:
 * Input: nums = [2,7,11,15], target = 9
 * Output: [0,1]
 * Explanation: Because nums[0] + nums[1] == 9, we return [0, 1].
 * 
 * Example 2:
 * Input: nums = [3,2,4], target = 6
 * Output: [1,2]
 * 
 * Example 3:
 * Input: nums = [3,3], target = 6
 * Output: [0,1]
 */

class TwoSum {

    // Solution 1: Brute Force Approach
    // Time Complexity: O(n²), Space Complexity: O(1)
    public int[] twoSumBruteForce(int[] nums, int target) {
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[i] + nums[j] == target) {
                    return new int[] { i, j };
                }
            }
        }
        return new int[] {}; // Should never reach here given problem constraints
    }

    // Solution 2: Two-Pass Hash Map
    // Time Complexity: O(n), Space Complexity: O(n)
    public int[] twoSumTwoPass(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();

        // First pass: store all numbers and their indices
        for (int i = 0; i < nums.length; i++) {
            map.put(nums[i], i);
        }

        // Second pass: find complement
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (map.containsKey(complement) && map.get(complement) != i) {
                return new int[] { i, map.get(complement) };
            }
        }

        return new int[] {}; // Should never reach here
    }

    // Solution 3: One-Pass Hash Map (Most Optimal)
    // Time Complexity: O(n), Space Complexity: O(n)
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();

        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];

            // Check if complement exists in map
            if (map.containsKey(complement)) {
                return new int[] { map.get(complement), i };
            }

            // Store current number and its index
            map.put(nums[i], i);
        }

        return new int[] {}; // Should never reach here
    }

    // Test method to verify solutions
    public static void main(String[] args) {
        TwoSum solution = new TwoSum();

        // Test Case 1
        int[] nums1 = { 2, 7, 11, 15 };
        int target1 = 9;
        System.out.println("Test 1: " + Arrays.toString(solution.twoSum(nums1, target1)));
        // Expected: [0, 1]

        // Test Case 2
        int[] nums2 = { 3, 2, 4 };
        int target2 = 6;
        System.out.println("Test 2: " + Arrays.toString(solution.twoSum(nums2, target2)));
        // Expected: [1, 2]

        // Test Case 3
        int[] nums3 = { 3, 3 };
        int target3 = 6;
        System.out.println("Test 3: " + Arrays.toString(solution.twoSum(nums3, target3)));
        // Expected: [0, 1]

        // Additional test cases
        int[] nums4 = { -1, -2, -3, -4, -5 };
        int target4 = -8;
        System.out.println("Test 4: " + Arrays.toString(solution.twoSum(nums4, target4)));
        // Expected: [2, 4] (nums[2] + nums[4] = -3 + (-5) = -8)

        int[] nums5 = { 0, 4, 3, 0 };
        int target5 = 0;
        System.out.println("Test 5: " + Arrays.toString(solution.twoSum(nums5, target5)));
        // Expected: [0, 3] (nums[0] + nums[3] = 0 + 0 = 0)
    }

}
