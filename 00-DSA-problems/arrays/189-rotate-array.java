/*
 * Given an integer array nums, rotate the array to the right by k steps, where
 * k is non-negative.
 * 
 * Example 1:
 * Input: nums = [1,2,3,4,5,6,7], k = 3
 * Output: [5,6,7,1,2,3,4]
 * Explanation:
 * rotate 1 steps to the right: [7,1,2,3,4,5,6]
 * rotate 2 steps to the right: [6,7,1,2,3,4,5]
 * rotate 3 steps to the right: [5,6,7,1,2,3,4]
 * 
 * Example 2:
 * Input: nums = [-1,-100,3,99], k = 2
 * Output: [3,99,-1,-100]
 * Explanation:
 * rotate 1 steps to the right: [99,-1,-100,3]
 * rotate 2 steps to the right: [3,99,-1,-100]
 */

class ArrayRotation {

    // Solution 1: Using Extra Array (Easiest to understand)
    // Time: O(n), Space: O(n)
    public void rotate1(int[] nums, int k) {
        int n = nums.length;
        k = k % n; // Handle cases where k > n

        int[] temp = new int[n];

        // Copy elements to their new positions
        for (int i = 0; i < n; i++) {
            temp[(i + k) % n] = nums[i];
        }

        // Copy back to original array
        for (int i = 0; i < n; i++) {
            nums[i] = temp[i];
        }
    }

    // Solution 2: Reverse Array Approach (Most elegant)
    // Time: O(n), Space: O(1)
    public void rotate2(int[] nums, int k) {
        int n = nums.length;
        k = k % n;

        // Reverse entire array
        reverse(nums, 0, n - 1);
        // Reverse first k elements
        reverse(nums, 0, k - 1);
        // Reverse remaining elements
        reverse(nums, k, n - 1);
    }

    private void reverse(int[] nums, int start, int end) {
        while (start < end) {
            int temp = nums[start];
            nums[start] = nums[end];
            nums[end] = temp;
            start++;
            end--;
        }
    }

    // Solution 3: Cyclic Replacements (Most space efficient)
    // Time: O(n), Space: O(1)
    public void rotate3(int[] nums, int k) {
        int n = nums.length;
        k = k % n;

        int count = 0;
        for (int start = 0; count < n; start++) {
            int current = start;
            int prev = nums[start];

            do {
                int next = (current + k) % n;
                int temp = nums[next];
                nums[next] = prev;
                prev = temp;
                current = next;
                count++;
            } while (start != current);
        }
    }

    // Solution 4: Brute Force (One by one rotation)
    // Time: O(n*k), Space: O(1) - Not recommended for large k
    public void rotate4(int[] nums, int k) {
        int n = nums.length;
        k = k % n;

        for (int i = 0; i < k; i++) {
            rotateByOne(nums);
        }
    }

    private void rotateByOne(int[] nums) {
        int temp = nums[nums.length - 1];
        for (int i = nums.length - 1; i > 0; i--) {
            nums[i] = nums[i - 1];
        }
        nums[0] = temp;
    }

    // Test method to demonstrate all solutions
    public static void main(String[] args) {
        ArrayRotation solution = new ArrayRotation();

        // Test case 1
        int[] nums1 = { 1, 2, 3, 4, 5, 6, 7 };
        int k1 = 3;
        System.out.println("Original: " + java.util.Arrays.toString(nums1));
        solution.rotate2(nums1, k1); // Using reverse method
        System.out.println("After rotating by " + k1 + ": " + java.util.Arrays.toString(nums1));

        // Test case 2
        int[] nums2 = { -1, -100, 3, 99 };
        int k2 = 2;
        System.out.println("\nOriginal: " + java.util.Arrays.toString(nums2));
        solution.rotate2(nums2, k2);
        System.out.println("After rotating by " + k2 + ": " + java.util.Arrays.toString(nums2));

        // Edge cases
        int[] nums3 = { 1 };
        solution.rotate2(nums3, 1);
        System.out.println("\nSingle element after rotation: " + java.util.Arrays.toString(nums3));

        int[] nums4 = { 1, 2 };
        solution.rotate2(nums4, 3); // k > length
        System.out.println("k > length case: " + java.util.Arrays.toString(nums4));
    }

}
