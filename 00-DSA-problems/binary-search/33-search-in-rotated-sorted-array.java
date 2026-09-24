/*
 * There is an integer array nums sorted in ascending order (with distinct
 * values).
 * 
 * Prior to being passed to your function, nums is possibly rotated at an
 * unknown pivot index k (1 <= k < nums.length) such that the resulting array is
 * [nums[k], nums[k+1], ..., nums[n-1], nums[0], nums[1], ..., nums[k-1]]
 * (0-indexed). For example, [0,1,2,4,5,6,7] might be rotated at pivot index 3
 * and become [4,5,6,7,0,1,2].
 * 
 * Given the array nums after the possible rotation and an integer target,
 * return the index of target if it is in nums, or -1 if it is not in nums.
 * 
 * You must write an algorithm with O(log n) runtime complexity.
 * 
 * Example 1:
 * Input: nums = [4,5,6,7,0,1,2], target = 0
 * Output: 4
 * 
 * Example 2:
 * Input: nums = [4,5,6,7,0,1,2], target = 3
 * Output: -1
 * 
 * Example 3:
 * Input: nums = [1], target = 0
 * Output: -1
 */

class SearchRotatedArray {

    /**
     * Solution 1: Single Pass Binary Search
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public int search(int[] nums, int target) {
        int left = 0;
        int right = nums.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] == target) {
                return mid;
            }

            // Check which half is sorted
            if (nums[left] <= nums[mid]) {
                // Left half is sorted
                if (target >= nums[left] && target < nums[mid]) {
                    // Target is in the sorted left half
                    right = mid - 1;
                } else {
                    // Target is in the right half
                    left = mid + 1;
                }
            } else {
                // Right half is sorted
                if (target > nums[mid] && target <= nums[right]) {
                    // Target is in the sorted right half
                    left = mid + 1;
                } else {
                    // Target is in the left half
                    right = mid - 1;
                }
            }
        }

        return -1;
    }

    /**
     * Solution 2: Find Pivot First, Then Binary Search
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public int searchWithPivot(int[] nums, int target) {
        int n = nums.length;
        if (n == 0)
            return -1;
        if (n == 1)
            return nums[0] == target ? 0 : -1;

        // Find the pivot (minimum element index)
        int pivot = findPivot(nums);

        // If no rotation, do normal binary search
        if (pivot == 0) {
            return binarySearch(nums, 0, n - 1, target);
        }

        // If target is the pivot element
        if (nums[pivot] == target) {
            return pivot;
        }

        // Decide which part to search
        if (target >= nums[0]) {
            // Search in the left part
            return binarySearch(nums, 0, pivot - 1, target);
        } else {
            // Search in the right part
            return binarySearch(nums, pivot, n - 1, target);
        }
    }

    private int findPivot(int[] nums) {
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

    private int binarySearch(int[] nums, int left, int right, int target) {
        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] == target) {
                return mid;
            } else if (nums[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return -1;
    }

    /**
     * Solution 3: Recursive Approach
     * Time Complexity: O(log n)
     * Space Complexity: O(log n) due to recursion
     */
    public int searchRecursive(int[] nums, int target) {
        return searchHelper(nums, target, 0, nums.length - 1);
    }

    private int searchHelper(int[] nums, int target, int left, int right) {
        if (left > right) {
            return -1;
        }

        int mid = left + (right - left) / 2;

        if (nums[mid] == target) {
            return mid;
        }

        // Check which half is sorted
        if (nums[left] <= nums[mid]) {
            // Left half is sorted
            if (target >= nums[left] && target < nums[mid]) {
                return searchHelper(nums, target, left, mid - 1);
            } else {
                return searchHelper(nums, target, mid + 1, right);
            }
        } else {
            // Right half is sorted
            if (target > nums[mid] && target <= nums[right]) {
                return searchHelper(nums, target, mid + 1, right);
            } else {
                return searchHelper(nums, target, left, mid - 1);
            }
        }
    }

    /**
     * Solution 4: Handle Edge Cases Explicitly
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public int searchWithEdgeCases(int[] nums, int target) {
        if (nums == null || nums.length == 0) {
            return -1;
        }

        int left = 0;
        int right = nums.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] == target) {
                return mid;
            }

            // Handle duplicates at boundaries
            if (nums[left] == nums[mid] && nums[mid] == nums[right]) {
                left++;
                right--;
                continue;
            }

            if (nums[left] <= nums[mid]) {
                // Left half is sorted
                if (nums[left] <= target && target < nums[mid]) {
                    right = mid - 1;
                } else {
                    left = mid + 1;
                }
            } else {
                // Right half is sorted
                if (nums[mid] < target && target <= nums[right]) {
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
        }

        return -1;
    }

    /**
     * Test method to demonstrate all solutions
     */
    public static void main(String[] args) {
        SearchRotatedArray solution = new SearchRotatedArray();

        // Test Case 1: [4,5,6,7,0,1,2], target = 0
        int[] nums1 = { 4, 5, 6, 7, 0, 1, 2 };
        int target1 = 0;
        System.out.println("Array: [4,5,6,7,0,1,2], Target: 0");
        System.out.println("Single Pass: " + solution.search(nums1, target1));
        System.out.println("With Pivot: " + solution.searchWithPivot(nums1, target1));
        System.out.println("Recursive: " + solution.searchRecursive(nums1, target1));
        System.out.println();

        // Test Case 2: [4,5,6,7,0,1,2], target = 3
        int[] nums2 = { 4, 5, 6, 7, 0, 1, 2 };
        int target2 = 3;
        System.out.println("Array: [4,5,6,7,0,1,2], Target: 3");
        System.out.println("Single Pass: " + solution.search(nums2, target2));
        System.out.println("With Pivot: " + solution.searchWithPivot(nums2, target2));
        System.out.println("Recursive: " + solution.searchRecursive(nums2, target2));
        System.out.println();

        // Test Case 3: [1], target = 0
        int[] nums3 = { 1 };
        int target3 = 0;
        System.out.println("Array: [1], Target: 0");
        System.out.println("Single Pass: " + solution.search(nums3, target3));
        System.out.println("With Pivot: " + solution.searchWithPivot(nums3, target3));
        System.out.println("Recursive: " + solution.searchRecursive(nums3, target3));
        System.out.println();

        // Test Case 4: No rotation
        int[] nums4 = { 1, 2, 3, 4, 5 };
        int target4 = 3;
        System.out.println("Array: [1,2,3,4,5], Target: 3");
        System.out.println("Single Pass: " + solution.search(nums4, target4));
        System.out.println("With Pivot: " + solution.searchWithPivot(nums4, target4));
        System.out.println("Recursive: " + solution.searchRecursive(nums4, target4));
    }

}
