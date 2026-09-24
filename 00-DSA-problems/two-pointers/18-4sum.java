/*
 * Given an array nums of n integers, return an array of all the unique
 * quadruplets [nums[a], nums[b], nums[c], nums[d]] such that:
 * 
 * 0 <= a, b, c, d < n
 * a, b, c, and d are distinct.
 * nums[a] + nums[b] + nums[c] + nums[d] == target
 * You may return the answer in any order.
 * 
 * 
 * 
 * Example 1:
 * 
 * Input: nums = [1,0,-1,0,-2,2], target = 0
 * Output: [[-2,-1,1,2],[-2,0,0,2],[-1,0,0,1]]
 * Example 2:
 * 
 * Input: nums = [2,2,2,2,2], target = 8
 * Output: [[2,2,2,2]]
 * 
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class FourSum {

    // Solution 1: Two Pointers (Optimal)
    // Time: O(n³), Space: O(1) excluding output
    public List<List<Integer>> fourSum(int[] nums, int target) {
        List<List<Integer>> result = new ArrayList<>();
        if (nums == null || nums.length < 4) {
            return result;
        }

        Arrays.sort(nums);
        int n = nums.length;

        for (int i = 0; i < n - 3; i++) {
            // Skip duplicates for first number
            if (i > 0 && nums[i] == nums[i - 1])
                continue;

            for (int j = i + 1; j < n - 2; j++) {
                // Skip duplicates for second number
                if (j > i + 1 && nums[j] == nums[j - 1])
                    continue;

                // Two pointers for remaining two numbers
                int left = j + 1;
                int right = n - 1;

                while (left < right) {
                    long sum = (long) nums[i] + nums[j] + nums[left] + nums[right];

                    if (sum == target) {
                        result.add(Arrays.asList(nums[i], nums[j], nums[left], nums[right]));

                        // Skip duplicates for third number
                        while (left < right && nums[left] == nums[left + 1])
                            left++;
                        // Skip duplicates for fourth number
                        while (left < right && nums[right] == nums[right - 1])
                            right--;

                        left++;
                        right--;
                    } else if (sum < target) {
                        left++;
                    } else {
                        right--;
                    }
                }
            }
        }

        return result;
    }

    // Solution 2: Two Pointers with Early Termination
    // Time: O(n³), Space: O(1) excluding output
    public List<List<Integer>> fourSum2(int[] nums, int target) {
        List<List<Integer>> result = new ArrayList<>();
        if (nums == null || nums.length < 4) {
            return result;
        }

        Arrays.sort(nums);
        int n = nums.length;

        for (int i = 0; i < n - 3; i++) {
            if (i > 0 && nums[i] == nums[i - 1])
                continue;

            // Early termination: smallest possible sum > target
            if ((long) nums[i] + nums[i + 1] + nums[i + 2] + nums[i + 3] > target)
                break;
            // Early termination: largest possible sum < target
            if ((long) nums[i] + nums[n - 3] + nums[n - 2] + nums[n - 1] < target)
                continue;

            for (int j = i + 1; j < n - 2; j++) {
                if (j > i + 1 && nums[j] == nums[j - 1])
                    continue;

                // Early termination for inner loop
                if ((long) nums[i] + nums[j] + nums[j + 1] + nums[j + 2] > target)
                    break;
                if ((long) nums[i] + nums[j] + nums[n - 2] + nums[n - 1] < target)
                    continue;

                int left = j + 1;
                int right = n - 1;

                while (left < right) {
                    long sum = (long) nums[i] + nums[j] + nums[left] + nums[right];

                    if (sum == target) {
                        result.add(Arrays.asList(nums[i], nums[j], nums[left], nums[right]));
                        while (left < right && nums[left] == nums[left + 1])
                            left++;
                        while (left < right && nums[right] == nums[right - 1])
                            right--;
                        left++;
                        right--;
                    } else if (sum < target) {
                        left++;
                    } else {
                        right--;
                    }
                }
            }
        }

        return result;
    }

    // Solution 3: HashSet for Deduplication (Alternative Approach)
    // Time: O(n³), Space: O(n)
    public List<List<Integer>> fourSum3(int[] nums, int target) {
        Set<List<Integer>> resultSet = new HashSet<>();
        if (nums == null || nums.length < 4) {
            return new ArrayList<>(resultSet);
        }

        Arrays.sort(nums);
        int n = nums.length;

        for (int i = 0; i < n - 3; i++) {
            for (int j = i + 1; j < n - 2; j++) {
                int left = j + 1;
                int right = n - 1;

                while (left < right) {
                    long sum = (long) nums[i] + nums[j] + nums[left] + nums[right];

                    if (sum == target) {
                        resultSet.add(Arrays.asList(nums[i], nums[j], nums[left], nums[right]));
                        left++;
                        right--;
                    } else if (sum < target) {
                        left++;
                    } else {
                        right--;
                    }
                }
            }
        }

        return new ArrayList<>(resultSet);
    }

    // Solution 4: Generic K-Sum (Can solve 2-sum, 3-sum, 4-sum, etc.)
    // Time: O(n^(k-1)), Space: O(k)
    public List<List<Integer>> fourSum4(int[] nums, int target) {
        Arrays.sort(nums);
        return kSum(nums, target, 0, 4);
    }

    private List<List<Integer>> kSum(int[] nums, long target, int start, int k) {
        List<List<Integer>> result = new ArrayList<>();

        if (start == nums.length)
            return result;

        // Base case: 2-sum
        if (k == 2) {
            int left = start, right = nums.length - 1;
            while (left < right) {
                long sum = (long) nums[left] + nums[right];
                if (sum == target) {
                    result.add(Arrays.asList(nums[left], nums[right]));
                    while (left < right && nums[left] == nums[left + 1])
                        left++;
                    while (left < right && nums[right] == nums[right - 1])
                        right--;
                    left++;
                    right--;
                } else if (sum < target) {
                    left++;
                } else {
                    right--;
                }
            }
        } else {
            // Recursive case: k-sum
            for (int i = start; i < nums.length - k + 1; i++) {
                if (i > start && nums[i] == nums[i - 1])
                    continue;

                List<List<Integer>> subResult = kSum(nums, target - nums[i], i + 1, k - 1);
                for (List<Integer> list : subResult) {
                    List<Integer> temp = new ArrayList<>();
                    temp.add(nums[i]);
                    temp.addAll(list);
                    result.add(temp);
                }
            }
        }

        return result;
    }

    // Helper: Print result
    private static void printResult(List<List<Integer>> result) {
        System.out.print("[");
        for (int i = 0; i < result.size(); i++) {
            System.out.print(result.get(i));
            if (i < result.size() - 1)
                System.out.print(", ");
        }
        System.out.println("]");
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
        FourSum solution = new FourSum();

        // Test case 1
        int[] nums1 = { 1, 0, -1, 0, -2, 2 };
        int target1 = 0;
        System.out.print("Example 1 - Input: nums = ");
        printArray(nums1);
        System.out.println(", target = " + target1);
        System.out.print("Output: ");
        printResult(solution.fourSum(nums1, target1));
        // Expected: [[-2,-1,1,2],[-2,0,0,2],[-1,0,0,1]]

        // Test case 2
        int[] nums2 = { 2, 2, 2, 2, 2 };
        int target2 = 8;
        System.out.print("\nExample 2 - Input: nums = ");
        printArray(nums2);
        System.out.println(", target = " + target2);
        System.out.print("Output: ");
        printResult(solution.fourSum(nums2, target2));
        // Expected: [[2,2,2,2]]

        // Test case 3: Empty result
        int[] nums3 = { 1, 2, 3, 4 };
        int target3 = 100;
        System.out.print("\nNo solution - Input: nums = ");
        printArray(nums3);
        System.out.println(", target = " + target3);
        System.out.print("Output: ");
        printResult(solution.fourSum2(nums3, target3));

        // Test case 4: Negative target
        int[] nums4 = { -3, -2, -1, 0, 1, 2, 3 };
        int target4 = -2;
        System.out.print("\nNegative target - Input: nums = ");
        printArray(nums4);
        System.out.println(", target = " + target4);
        System.out.print("Output: ");
        printResult(solution.fourSum2(nums4, target4));

        // Test case 5: Large numbers (overflow test)
        int[] nums5 = { 1000000000, 1000000000, 1000000000, 1000000000 };
        int target5 = -294967296;
        System.out.print("\nLarge numbers - Input: nums = ");
        printArray(nums5);
        System.out.println(", target = " + target5);
        System.out.print("Output: ");
        printResult(solution.fourSum(nums5, target5));

        // Test case 6: Minimum size
        int[] nums6 = { 1, 2, 3, 4 };
        int target6 = 10;
        System.out.print("\nExact four elements - Input: nums = ");
        printArray(nums6);
        System.out.println(", target = " + target6);
        System.out.print("Output: ");
        printResult(solution.fourSum3(nums6, target6));
        // Expected: [[1,2,3,4]]

        // Test case 7: Using generic k-sum
        int[] nums7 = { 1, 0, -1, 0, -2, 2 };
        int target7 = 0;
        System.out.print("\nUsing K-Sum approach - Input: nums = ");
        printArray(nums7);
        System.out.println(", target = " + target7);
        System.out.print("Output: ");
        printResult(solution.fourSum4(nums7, target7));

        // Performance comparison
        System.out.println("\n=== Solution Comparison ===");
        int[] test = { -1, 0, 1, 2, -1, -4 };
        int testTarget = -1;
        System.out.print("Test array: ");
        printArray(test);
        System.out.println(", target = " + testTarget);

        long start = System.nanoTime();
        List<List<Integer>> result1 = solution.fourSum(test, testTarget);
        long time1 = System.nanoTime() - start;
        System.out.println("Solution 1 (Standard): " + result1.size() + " results in " + time1 + " ns");

        start = System.nanoTime();
        List<List<Integer>> result2 = solution.fourSum2(test, testTarget);
        long time2 = System.nanoTime() - start;
        System.out.println("Solution 2 (Optimized): " + result2.size() + " results in " + time2 + " ns");

        start = System.nanoTime();
        List<List<Integer>> result3 = solution.fourSum3(test, testTarget);
        long time3 = System.nanoTime() - start;
        System.out.println("Solution 3 (HashSet): " + result3.size() + " results in " + time3 + " ns");

        start = System.nanoTime();
        List<List<Integer>> result4 = solution.fourSum4(test, testTarget);
        long time4 = System.nanoTime() - start;
        System.out.println("Solution 4 (K-Sum): " + result4.size() + " results in " + time4 + " ns");
    }
}
