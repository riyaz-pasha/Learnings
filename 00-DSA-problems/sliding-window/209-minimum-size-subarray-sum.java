import java.util.*;
/*
 * Given an array of positive integers nums and a positive integer target,
 * return the minimal length of a subarray whose sum is greater than or equal to
 * target. If there is no such subarray, return 0 instead.
 * 
 * Example 1:
 * Input: target = 7, nums = [2,3,1,2,4,3]
 * Output: 2
 * Explanation: The subarray [4,3] has the minimal length under the problem
 * constraint.
 * 
 * Example 2:
 * Input: target = 4, nums = [1,4,4]
 * Output: 1
 * 
 * Example 3:
 * Input: target = 11, nums = [1,1,1,1,1,1,1,1]
 * Output: 0
 */

class MinimumSizeSubarraySum {

    // Solution 1: Brute Force Approach
    // Time Complexity: O(n³), Space Complexity: O(1)
    public int minSubArrayLenBruteForce(int target, int[] nums) {
        int n = nums.length;
        int minLen = Integer.MAX_VALUE;

        // Check all possible subarrays
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                int sum = 0;
                // Calculate sum of subarray from i to j
                for (int k = i; k <= j; k++) {
                    sum += nums[k];
                }

                // Check if sum meets the target
                if (sum >= target) {
                    minLen = Math.min(minLen, j - i + 1);
                    break; // No need to extend this subarray further
                }
            }
        }

        return minLen == Integer.MAX_VALUE ? 0 : minLen;
    }

    // Solution 2: Optimized Brute Force
    // Time Complexity: O(n²), Space Complexity: O(1)
    public int minSubArrayLenOptimizedBruteForce(int target, int[] nums) {
        int n = nums.length;
        int minLen = Integer.MAX_VALUE;

        // Check all possible subarrays
        for (int i = 0; i < n; i++) {
            int sum = 0;
            for (int j = i; j < n; j++) {
                sum += nums[j]; // Add current element to running sum

                if (sum >= target) {
                    minLen = Math.min(minLen, j - i + 1);
                    break; // No need to extend this subarray further
                }
            }
        }

        return minLen == Integer.MAX_VALUE ? 0 : minLen;
    }

    // Solution 3: Sliding Window Approach (Optimal)
    // Time Complexity: O(n), Space Complexity: O(1)
    public int minSubArrayLen(int target, int[] nums) {
        int n = nums.length;
        int left = 0, right = 0;
        int sum = 0;
        int minLen = Integer.MAX_VALUE;

        while (right < n) {
            // Expand window by adding right element
            sum += nums[right];

            // Contract window from left while sum >= target
            while (sum >= target) {
                minLen = Math.min(minLen, right - left + 1);
                sum -= nums[left];
                left++;
            }

            right++;
        }

        return minLen == Integer.MAX_VALUE ? 0 : minLen;
    }

    // Solution 4: Sliding Window with Early Termination
    // Time Complexity: O(n), Space Complexity: O(1)
    public int minSubArrayLenOptimized(int target, int[] nums) {
        int n = nums.length;
        int left = 0;
        int sum = 0;
        int minLen = Integer.MAX_VALUE;

        for (int right = 0; right < n; right++) {
            sum += nums[right];

            // Contract window while sum >= target
            while (sum >= target) {
                minLen = Math.min(minLen, right - left + 1);

                // Early termination: if we found length 1, it's optimal
                if (minLen == 1) {
                    return 1;
                }

                sum -= nums[left];
                left++;
            }
        }

        return minLen == Integer.MAX_VALUE ? 0 : minLen;
    }

    // Solution 5: Binary Search + Prefix Sum Approach
    // Time Complexity: O(n log n), Space Complexity: O(n)
    public int minSubArrayLenBinarySearch(int target, int[] nums) {
        int n = nums.length;

        // Create prefix sum array
        int[] prefixSum = new int[n + 1];
        for (int i = 0; i < n; i++) {
            prefixSum[i + 1] = prefixSum[i] + nums[i];
        }

        int minLen = Integer.MAX_VALUE;

        // For each starting position, use binary search to find the minimum ending
        // position
        for (int i = 0; i < n; i++) {
            int targetSum = target + prefixSum[i];
            int bound = Arrays.binarySearch(prefixSum, targetSum);

            if (bound < 0) {
                bound = -bound - 1; // Convert to insertion point
            }

            if (bound <= n) {
                minLen = Math.min(minLen, bound - i);
            }
        }

        return minLen == Integer.MAX_VALUE ? 0 : minLen;
    }

    // Helper method to visualize the sliding window process
    public int minSubArrayLenWithVisualization(int target, int[] nums) {
        int n = nums.length;
        int left = 0;
        int sum = 0;
        int minLen = Integer.MAX_VALUE;

        System.out.println("Sliding Window Visualization:");
        System.out.println("Array: " + Arrays.toString(nums));
        System.out.println("Target: " + target);
        System.out.println();

        for (int right = 0; right < n; right++) {
            sum += nums[right];
            System.out.printf("Expand: right=%d, sum=%d, window=[%d,%d]\n",
                    right, sum, left, right);

            while (sum >= target) {
                int currentLen = right - left + 1;
                minLen = Math.min(minLen, currentLen);
                System.out.printf("  Valid window found! Length=%d, sum=%d\n",
                        currentLen, sum);

                sum -= nums[left];
                left++;
                if (left <= right) {
                    System.out.printf("  Contract: left=%d, sum=%d, window=[%d,%d]\n",
                            left, sum, left, right);
                }
            }
            System.out.println();
        }

        return minLen == Integer.MAX_VALUE ? 0 : minLen;
    }

    // Test method
    public static void main(String[] args) {
        MinimumSizeSubarraySum solution = new MinimumSizeSubarraySum();

        // Test case 1
        int target1 = 7;
        int[] nums1 = { 2, 3, 1, 2, 4, 3 };
        System.out.println("=== Test Case 1 ===");
        System.out.println("Input: target = " + target1 + ", nums = " + Arrays.toString(nums1));
        System.out.println("Brute Force: " + solution.minSubArrayLenBruteForce(target1, nums1));
        System.out.println("Optimized Brute Force: " + solution.minSubArrayLenOptimizedBruteForce(target1, nums1));
        System.out.println("Sliding Window: " + solution.minSubArrayLen(target1, nums1));
        System.out.println("Binary Search: " + solution.minSubArrayLenBinarySearch(target1, nums1));
        System.out.println();

        // Test case 2
        int target2 = 4;
        int[] nums2 = { 1, 4, 4 };
        System.out.println("=== Test Case 2 ===");
        System.out.println("Input: target = " + target2 + ", nums = " + Arrays.toString(nums2));
        System.out.println("Sliding Window: " + solution.minSubArrayLen(target2, nums2));
        System.out.println();

        // Test case 3
        int target3 = 11;
        int[] nums3 = { 1, 1, 1, 1, 1, 1, 1, 1 };
        System.out.println("=== Test Case 3 ===");
        System.out.println("Input: target = " + target3 + ", nums = " + Arrays.toString(nums3));
        System.out.println("Sliding Window: " + solution.minSubArrayLen(target3, nums3));
        System.out.println();

        // Visualization for test case 1
        System.out.println("=== Visualization ===");
        solution.minSubArrayLenWithVisualization(target1, nums1);
    }

}

/*
 * ALGORITHM EXPLANATION:
 * 
 * The optimal solution uses the Sliding Window (Two Pointer) technique:
 * 
 * SLIDING WINDOW APPROACH:
 * 1. Use two pointers: left and right to maintain a window
 * 2. Expand the window by moving right pointer and adding elements to sum
 * 3. When sum >= target, try to contract the window from left to find minimum
 * length
 * 4. Continue until right pointer reaches the end
 * 
 * WHY SLIDING WINDOW WORKS:
 * - All elements are positive, so adding elements increases sum
 * - Removing elements decreases sum
 * - Once we find a valid window, we try to make it smaller by removing from
 * left
 * - Each element is visited at most twice (once by right, once by left pointer)
 * 
 * DETAILED STEPS FOR [2,3,1,2,4,3], target=7:
 * 1. right=0: sum=2, not enough
 * 2. right=1: sum=5, not enough
 * 3. right=2: sum=6, not enough
 * 4. right=3: sum=8 >= 7, found valid window [2,3,1,2] length=4
 * - Contract: remove nums[0]=2, sum=6, left=1
 * 5. right=4: sum=10 >= 7, found valid window [3,1,2,4] length=4
 * - Contract: remove nums[1]=3, sum=7 >= 7, found [1,2,4] length=3
 * - Contract: remove nums[2]=1, sum=6, left=3
 * 6. right=5: sum=9 >= 7, found valid window [2,4,3] length=3
 * - Contract: remove nums[3]=2, sum=7 >= 7, found [4,3] length=2 ← MINIMUM
 * - Contract: remove nums[4]=4, sum=3, left=5
 * 
 * TIME COMPLEXITY COMPARISON:
 * - Brute Force: O(n³) - three nested loops
 * - Optimized Brute Force: O(n²) - two nested loops with running sum
 * - Sliding Window: O(n) - each element visited at most twice
 * - Binary Search: O(n log n) - binary search for each starting position
 * 
 * SPACE COMPLEXITY:
 * - Sliding Window: O(1) - only using pointers and variables
 * - Binary Search: O(n) - for prefix sum array
 * 
 * BEST SOLUTION: Sliding Window approach is optimal with O(n) time and O(1)
 * space.
 */
