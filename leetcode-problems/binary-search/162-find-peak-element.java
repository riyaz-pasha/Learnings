/*
 * Topics
 * premium lock icon
 * Companies
 * A peak element is an element that is strictly greater than its neighbors.
 * 
 * Given a 0-indexed integer array nums, find a peak element, and return its
 * index. If the array contains multiple peaks, return the index to any of the
 * peaks.
 * 
 * You may imagine that nums[-1] = nums[n] = -∞. In other words, an element is
 * always considered to be strictly greater than a neighbor that is outside the
 * array.
 * 
 * You must write an algorithm that runs in O(log n) time.
 * 
 * Example 1:
 * Input: nums = [1,2,3,1]
 * Output: 2
 * Explanation: 3 is a peak element and your function should return the index
 * number 2.
 * 
 * Example 2:
 * Input: nums = [1,2,1,3,5,6,4]
 * Output: 5
 * Explanation: Your function can return either index number 1 where the peak
 * element is 2, or index number 5 where the peak element is 6.
 */

class PeakElement {

    /**
     * Solution 1: Binary Search - Iterative Approach
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public int findPeakElement(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            // If mid element is greater than next element,
            // peak must be on the left side (including mid)
            if (nums[mid] > nums[mid + 1]) {
                right = mid;
            } else {
                // If mid element is smaller than next element,
                // peak must be on the right side
                left = mid + 1;
            }
        }

        return left;
    }

    /**
     * Solution 2: Binary Search - Recursive Approach
     * Time Complexity: O(log n)
     * Space Complexity: O(log n) due to recursion stack
     */
    public int findPeakElementRecursive(int[] nums) {
        return findPeakHelper(nums, 0, nums.length - 1);
    }

    private int findPeakHelper(int[] nums, int left, int right) {
        if (left == right) {
            return left;
        }

        int mid = left + (right - left) / 2;

        if (nums[mid] > nums[mid + 1]) {
            return findPeakHelper(nums, left, mid);
        } else {
            return findPeakHelper(nums, mid + 1, right);
        }
    }

    /**
     * Solution 3: More Explicit Peak Checking
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public int findPeakElementExplicit(int[] nums) {
        int n = nums.length;

        // Handle edge cases
        if (n == 1)
            return 0;
        if (nums[0] > nums[1])
            return 0;
        if (nums[n - 1] > nums[n - 2])
            return n - 1;

        int left = 1;
        int right = n - 2;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            // Check if mid is a peak
            if (nums[mid] > nums[mid - 1] && nums[mid] > nums[mid + 1]) {
                return mid;
            }

            // If left neighbor is greater, search left half
            if (nums[mid] < nums[mid - 1]) {
                right = mid - 1;
            } else {
                // If right neighbor is greater, search right half
                left = mid + 1;
            }
        }

        return -1; // Should never reach here
    }

    /**
     * Test method to demonstrate the solutions
     */
    public static void main(String[] args) {
        PeakElement solution = new PeakElement();

        // Test Case 1: [1,2,3,1]
        int[] nums1 = { 1, 2, 3, 1 };
        System.out.println("Array: [1,2,3,1]");
        System.out.println("Peak index (iterative): " + solution.findPeakElement(nums1));
        System.out.println("Peak index (recursive): " + solution.findPeakElementRecursive(nums1));
        System.out.println("Peak index (explicit): " + solution.findPeakElementExplicit(nums1));
        System.out.println();

        // Test Case 2: [1,2,1,3,5,6,4]
        int[] nums2 = { 1, 2, 1, 3, 5, 6, 4 };
        System.out.println("Array: [1,2,1,3,5,6,4]");
        System.out.println("Peak index (iterative): " + solution.findPeakElement(nums2));
        System.out.println("Peak index (recursive): " + solution.findPeakElementRecursive(nums2));
        System.out.println("Peak index (explicit): " + solution.findPeakElementExplicit(nums2));
        System.out.println();

        // Test Case 3: Single element
        int[] nums3 = { 5 };
        System.out.println("Array: [5]");
        System.out.println("Peak index: " + solution.findPeakElement(nums3));
        System.out.println();

        // Test Case 4: Two elements
        int[] nums4 = { 1, 2 };
        System.out.println("Array: [1,2]");
        System.out.println("Peak index: " + solution.findPeakElement(nums4));
    }

}
