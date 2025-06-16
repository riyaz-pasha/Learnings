import java.util.*;
/*
 * Given an integer array nums, return all the triplets [nums[i], nums[j],
 * nums[k]] such that i != j, i != k, and j != k, and nums[i] + nums[j] +
 * nums[k] == 0.
 * 
 * Notice that the solution set must not contain duplicate triplets.
 * 
 * Example 1:
 * Input: nums = [-1,0,1,2,-1,-4]
 * Output: [[-1,-1,2],[-1,0,1]]
 * Explanation:
 * nums[0] + nums[1] + nums[2] = (-1) + 0 + 1 = 0.
 * nums[1] + nums[2] + nums[4] = 0 + 1 + (-1) = 0.
 * nums[0] + nums[3] + nums[4] = (-1) + 2 + (-1) = 0.
 * The distinct triplets are [-1,0,1] and [-1,-1,2].
 * Notice that the order of the output and the order of the triplets does not
 * matter.
 * 
 * Example 2:
 * Input: nums = [0,1,1]
 * Output: []
 * Explanation: The only possible triplet does not sum up to 0.
 * 
 * Example 3:
 * Input: nums = [0,0,0]
 * Output: [[0,0,0]]
 * Explanation: The only possible triplet sums up to 0.
 */

class ThreeSum {

    // Solution 1: Brute Force Approach
    // Time Complexity: O(n³), Space Complexity: O(n) for result storage
    public List<List<Integer>> threeSumBruteForce(int[] nums) {
        Set<List<Integer>> resultSet = new HashSet<>();
        int n = nums.length;

        // Check all possible triplets
        for (int i = 0; i < n - 2; i++) {
            for (int j = i + 1; j < n - 1; j++) {
                for (int k = j + 1; k < n; k++) {
                    if (nums[i] + nums[j] + nums[k] == 0) {
                        List<Integer> triplet = Arrays.asList(nums[i], nums[j], nums[k]);
                        Collections.sort(triplet);
                        resultSet.add(triplet);
                    }
                }
            }
        }

        return new ArrayList<>(resultSet);
    }

    // Solution 2: HashSet Approach
    // Time Complexity: O(n²), Space Complexity: O(n)
    public List<List<Integer>> threeSumHashSet(int[] nums) {
        Set<List<Integer>> resultSet = new HashSet<>();
        int n = nums.length;

        for (int i = 0; i < n - 2; i++) {
            Set<Integer> seen = new HashSet<>();

            for (int j = i + 1; j < n; j++) {
                int target = -(nums[i] + nums[j]);

                if (seen.contains(target)) {
                    List<Integer> triplet = Arrays.asList(nums[i], nums[j], target);
                    Collections.sort(triplet);
                    resultSet.add(triplet);
                }
                seen.add(nums[j]);
            }
        }

        return new ArrayList<>(resultSet);
    }

    // Solution 3: Two Pointer Approach (Optimal)
    // Time Complexity: O(n²), Space Complexity: O(1) excluding result storage
    public List<List<Integer>> threeSum(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();

        // Sort the array first
        Arrays.sort(nums);
        int n = nums.length;

        for (int i = 0; i < n - 2; i++) {
            // Skip duplicate values for the first element
            if (i > 0 && nums[i] == nums[i - 1]) {
                continue;
            }

            int left = i + 1;
            int right = n - 1;

            while (left < right) {
                int sum = nums[i] + nums[left] + nums[right];

                if (sum == 0) {
                    result.add(Arrays.asList(nums[i], nums[left], nums[right]));

                    // Skip duplicates for left pointer
                    while (left < right && nums[left] == nums[left + 1]) {
                        left++;
                    }
                    // Skip duplicates for right pointer
                    while (left < right && nums[right] == nums[right - 1]) {
                        right--;
                    }

                    left++;
                    right--;
                } else if (sum < 0) {
                    left++; // Need a larger sum
                } else {
                    right--; // Need a smaller sum
                }
            }
        }

        return result;
    }

    // Solution 4: Optimized Two Pointer with Early Termination
    // Time Complexity: O(n²), Space Complexity: O(1) excluding result storage
    public List<List<Integer>> threeSumOptimized(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();

        if (nums == null || nums.length < 3) {
            return result;
        }

        Arrays.sort(nums);
        int n = nums.length;

        for (int i = 0; i < n - 2; i++) {
            // Early termination: if smallest element is positive, no solution exists
            if (nums[i] > 0)
                break;

            // Skip duplicate values for the first element
            if (i > 0 && nums[i] == nums[i - 1]) {
                continue;
            }

            // Early termination: if minimum possible sum is too large
            if (nums[i] + nums[i + 1] + nums[i + 2] > 0)
                break;

            // Early termination: if maximum possible sum is too small
            if (nums[i] + nums[n - 2] + nums[n - 1] < 0)
                continue;

            int left = i + 1;
            int right = n - 1;

            while (left < right) {
                int sum = nums[i] + nums[left] + nums[right];

                if (sum == 0) {
                    result.add(Arrays.asList(nums[i], nums[left], nums[right]));

                    // Skip duplicates
                    while (left < right && nums[left] == nums[left + 1]) {
                        left++;
                    }
                    while (left < right && nums[right] == nums[right - 1]) {
                        right--;
                    }

                    left++;
                    right--;
                } else if (sum < 0) {
                    left++;
                } else {
                    right--;
                }
            }
        }

        return result;
    }

    // Helper method to print results
    private void printResult(String method, List<List<Integer>> result) {
        System.out.println(method + ": " + result);
    }

    // Test method
    public static void main(String[] args) {
        ThreeSum solution = new ThreeSum();

        // Test case 1
        int[] nums1 = { -1, 0, 1, 2, -1, -4 };
        System.out.println("Test case 1: " + Arrays.toString(nums1));
        solution.printResult("Brute Force", solution.threeSumBruteForce(nums1));
        solution.printResult("HashSet", solution.threeSumHashSet(nums1));
        solution.printResult("Two Pointer", solution.threeSum(nums1));
        solution.printResult("Optimized", solution.threeSumOptimized(nums1));
        System.out.println();

        // Test case 2
        int[] nums2 = { 0, 1, 1 };
        System.out.println("Test case 2: " + Arrays.toString(nums2));
        solution.printResult("Two Pointer", solution.threeSum(nums2));
        System.out.println();

        // Test case 3
        int[] nums3 = { 0, 0, 0 };
        System.out.println("Test case 3: " + Arrays.toString(nums3));
        solution.printResult("Two Pointer", solution.threeSum(nums3));
        System.out.println();

        // Test case 4: Edge case
        int[] nums4 = { -2, 0, 1, 1, 2 };
        System.out.println("Test case 4: " + Arrays.toString(nums4));
        solution.printResult("Two Pointer", solution.threeSum(nums4));
    }

}

/*
 * ALGORITHM EXPLANATION:
 * 
 * The optimal solution uses a two-pointer approach:
 * 
 * 1. Sort the array first
 * 2. For each element nums[i], use two pointers to find pairs that sum to
 * -nums[i]
 * 3. Use left and right pointers to avoid O(n³) complexity
 * 4. Skip duplicates to avoid duplicate triplets
 * 
 * DETAILED STEPS:
 * 1. Sort the array: [-4, -1, -1, 0, 1, 2]
 * 2. For i=0 (nums[i]=-4): find two numbers that sum to 4
 * - Use left=1, right=5 pointers
 * - Adjust pointers based on sum comparison
 * 3. Continue for each valid i, skipping duplicates
 * 
 * WHY SORTING HELPS:
 * - Enables two-pointer technique
 * - Makes duplicate skipping easier
 * - Allows early termination optimizations
 * 
 * DUPLICATE HANDLING:
 * - Skip duplicate values for the first element (i)
 * - Skip duplicates for left and right pointers after finding a valid triplet
 * - This ensures unique triplets in the result
 * 
 * TIME COMPLEXITY COMPARISON:
 * - Brute Force: O(n³) - check all combinations
 * - HashSet: O(n²) - for each pair, check if complement exists
 * - Two Pointer: O(n²) - sort O(n log n) + nested loop O(n²)
 * - Optimized: O(n²) - same as two pointer but with early terminations
 * 
 * SPACE COMPLEXITY:
 * - All solutions: O(1) extra space (excluding result storage and sorting
 * space)
 * 
 * BEST SOLUTION: The two-pointer approach (threeSum method) is optimal and most
 * commonly expected in interviews.
 */