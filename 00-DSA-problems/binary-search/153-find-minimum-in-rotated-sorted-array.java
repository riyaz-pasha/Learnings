/*
 * Suppose an array of length n sorted in ascending order is rotated between 1
 * and n times. For example, the array nums = [0,1,2,4,5,6,7] might become:
 * 
 * [4,5,6,7,0,1,2] if it was rotated 4 times.
 * [0,1,2,4,5,6,7] if it was rotated 7 times.
 * Notice that rotating an array [a[0], a[1], a[2], ..., a[n-1]] 1 time results
 * in the array [a[n-1], a[0], a[1], a[2], ..., a[n-2]].
 * 
 * Given the sorted rotated array nums of unique elements, return the minimum
 * element of this array.
 * 
 * You must write an algorithm that runs in O(log n) time.
 * 
 * Example 1:
 * Input: nums = [3,4,5,1,2]
 * Output: 1
 * Explanation: The original array was [1,2,3,4,5] rotated 3 times.
 * 
 * Example 2:
 * Input: nums = [4,5,6,7,0,1,2]
 * Output: 0
 * Explanation: The original array was [0,1,2,4,5,6,7] and it was rotated 4
 * times.
 * 
 * Example 3:
 * Input: nums = [11,13,15,17]
 * Output: 11
 * Explanation: The original array was [11,13,15,17] and it was rotated 4 times.
 */

class FindMinimumRotatedArray {

    /**
     * Solution 1: Classic Binary Search Approach
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public int findMin(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            // If mid element is greater than right element,
            // minimum is in the right half
            if (nums[mid] > nums[right]) {
                left = mid + 1;
            } else {
                // If mid element is less than or equal to right element,
                // minimum is in the left half (including mid)
                right = mid;
            }
        }

        return nums[left];
    }

    /**
     * Solution 2: Compare with Left Element
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public int findMinWithLeft(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        // If array is not rotated
        if (nums[left] < nums[right]) {
            return nums[left];
        }

        while (left < right) {
            int mid = left + (right - left) / 2;

            // If mid > left, the minimum is in right half
            if (nums[mid] >= nums[left]) {
                left = mid + 1;
            } else {
                // If mid < left, the minimum is in left half (including mid)
                right = mid;
            }
        }

        return nums[left];
    }

    /**
     * Solution 3: Recursive Approach
     * Time Complexity: O(log n)
     * Space Complexity: O(log n) due to recursion
     */
    public int findMinRecursive(int[] nums) {
        return findMinHelper(nums, 0, nums.length - 1);
    }

    private int findMinHelper(int[] nums, int left, int right) {
        // Base case: only one element
        if (left == right) {
            return nums[left];
        }

        // If array is sorted (not rotated in this range)
        if (nums[left] < nums[right]) {
            return nums[left];
        }

        int mid = left + (right - left) / 2;

        // If mid element is greater than right element,
        // minimum is in the right half
        if (nums[mid] > nums[right]) {
            return findMinHelper(nums, mid + 1, right);
        } else {
            // Minimum is in the left half (including mid)
            return findMinHelper(nums, left, mid);
        }
    }

    /**
     * Solution 4: Find Pivot Point (Rotation Index)
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public int findMinWithPivot(int[] nums) {
        int pivotIndex = findPivotIndex(nums);
        return nums[pivotIndex];
    }

    private int findPivotIndex(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] > nums[right]) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return left;
    }

    /**
     * Solution 5: Handle Edge Cases Explicitly
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public int findMinWithEdgeCases(int[] nums) {
        if (nums == null || nums.length == 0) {
            throw new IllegalArgumentException("Array cannot be null or empty");
        }

        if (nums.length == 1) {
            return nums[0];
        }

        if (nums.length == 2) {
            return Math.min(nums[0], nums[1]);
        }

        int left = 0;
        int right = nums.length - 1;

        // Check if array is not rotated
        if (nums[left] < nums[right]) {
            return nums[left];
        }

        while (left < right) {
            // If we have only two elements left
            if (right - left == 1) {
                return Math.min(nums[left], nums[right]);
            }

            int mid = left + (right - left) / 2;

            // Check if mid is the minimum
            if (nums[mid] < nums[mid - 1] && nums[mid] < nums[mid + 1]) {
                return nums[mid];
            }

            if (nums[mid] > nums[right]) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return nums[left];
    }

    /**
     * Solution 6: Optimized with Early Termination
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public int findMinOptimized(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        while (left < right) {
            // If current subarray is sorted, return the leftmost element
            if (nums[left] < nums[right]) {
                return nums[left];
            }

            int mid = left + (right - left) / 2;

            if (nums[mid] > nums[right]) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return nums[left];
    }

    /**
     * Solution 7: Using Three-way Comparison
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public int findMinThreeWay(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] > nums[right]) {
                // Minimum is in right half
                left = mid + 1;
            } else if (nums[mid] < nums[right]) {
                // Minimum is in left half (including mid)
                right = mid;
            } else {
                // nums[mid] == nums[right] (shouldn't happen with unique elements)
                // But handle it just in case
                right--;
            }
        }

        return nums[left];
    }

    /**
     * Utility method to find the number of rotations
     */
    public int findRotationCount(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] > nums[right]) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return left; // Number of rotations
    }

    /**
     * Test method to demonstrate all solutions
     */
    public static void main(String[] args) {
        FindMinimumRotatedArray solution = new FindMinimumRotatedArray();

        // Test Case 1: [3,4,5,1,2]
        int[] nums1 = { 3, 4, 5, 1, 2 };
        System.out.println("Array: [3,4,5,1,2]");
        System.out.println("Classic Binary Search: " + solution.findMin(nums1));
        System.out.println("With Left Comparison: " + solution.findMinWithLeft(nums1));
        System.out.println("Recursive: " + solution.findMinRecursive(nums1));
        System.out.println("With Pivot: " + solution.findMinWithPivot(nums1));
        System.out.println("Optimized: " + solution.findMinOptimized(nums1));
        System.out.println("Rotation Count: " + solution.findRotationCount(nums1));
        System.out.println();

        // Test Case 2: [4,5,6,7,0,1,2]
        int[] nums2 = { 4, 5, 6, 7, 0, 1, 2 };
        System.out.println("Array: [4,5,6,7,0,1,2]");
        System.out.println("Classic Binary Search: " + solution.findMin(nums2));
        System.out.println("With Left Comparison: " + solution.findMinWithLeft(nums2));
        System.out.println("Recursive: " + solution.findMinRecursive(nums2));
        System.out.println("Rotation Count: " + solution.findRotationCount(nums2));
        System.out.println();

        // Test Case 3: [11,13,15,17] (no rotation)
        int[] nums3 = { 11, 13, 15, 17 };
        System.out.println("Array: [11,13,15,17]");
        System.out.println("Classic Binary Search: " + solution.findMin(nums3));
        System.out.println("With Left Comparison: " + solution.findMinWithLeft(nums3));
        System.out.println("Optimized: " + solution.findMinOptimized(nums3));
        System.out.println("Rotation Count: " + solution.findRotationCount(nums3));
        System.out.println();

        // Test Case 4: Single element
        int[] nums4 = { 1 };
        System.out.println("Array: [1]");
        System.out.println("Classic Binary Search: " + solution.findMin(nums4));
        System.out.println();

        // Test Case 5: Two elements
        int[] nums5 = { 2, 1 };
        System.out.println("Array: [2,1]");
        System.out.println("Classic Binary Search: " + solution.findMin(nums5));
        System.out.println("With Edge Cases: " + solution.findMinWithEdgeCases(nums5));
        System.out.println();

        // Test Case 6: Maximum rotation
        int[] nums6 = { 1, 2, 3, 4, 5 };
        System.out.println("Array: [1,2,3,4,5] (fully rotated)");
        System.out.println("Classic Binary Search: " + solution.findMin(nums6));
        System.out.println("Rotation Count: " + solution.findRotationCount(nums6));
    }

}
