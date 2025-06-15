/*
 * Given an integer array nums sorted in non-decreasing order, remove the
 * duplicates in-place such that each unique element appears only once. The
 * relative order of the elements should be kept the same. Then return the
 * number of unique elements in nums.
 * 
 * Consider the number of unique elements of nums to be k, to get accepted, you
 * need to do the following things:
 * 
 * Change the array nums such that the first k elements of nums contain the
 * unique elements in the order they were present in nums initially. The
 * remaining elements of nums are not important as well as the size of nums.
 * Return k.
 */

class RemoveDuplicates {

    /**
     * Solution 1: Two Pointers (Slow and Fast) - Most Optimal
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Since array is sorted, duplicates are adjacent.
     * Use slow pointer for unique elements, fast pointer to scan array.
     */
    public int removeDuplicates(int[] nums) {
        if (nums.length == 0)
            return 0;

        int slow = 1; // Position for next unique element (first element is always unique)

        for (int fast = 1; fast < nums.length; fast++) {
            // If current element is different from previous, it's unique
            if (nums[fast] != nums[fast - 1]) {
                nums[slow] = nums[fast];
                slow++;
            }
        }

        return slow; // slow is the count of unique elements
    }

    /**
     * Solution 2: Two Pointers (Alternative Implementation)
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Compare current element with the last unique element found
     */
    public int removeDuplicatesAlt(int[] nums) {
        if (nums.length == 0)
            return 0;

        int uniqueIndex = 0; // Index of last unique element

        for (int i = 1; i < nums.length; i++) {
            if (nums[i] != nums[uniqueIndex]) {
                uniqueIndex++;
                nums[uniqueIndex] = nums[i];
            }
        }

        return uniqueIndex + 1; // +1 because index is 0-based
    }

    /**
     * Solution 3: Single Pointer with Previous Element Tracking
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Track the previous unique element and current write position
     */
    public int removeDuplicatesSinglePointer(int[] nums) {
        if (nums.length == 0)
            return 0;

        int writePos = 1;
        int prevUnique = nums[0];

        for (int i = 1; i < nums.length; i++) {
            if (nums[i] != prevUnique) {
                nums[writePos] = nums[i];
                prevUnique = nums[i];
                writePos++;
            }
        }

        return writePos;
    }

    /**
     * Solution 4: Iterator-style Approach
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * More explicit about the iteration process
     */
    public int removeDuplicatesIterator(int[] nums) {
        if (nums.length <= 1)
            return nums.length;

        int insertPos = 1;
        int currentVal = nums[0];

        for (int i = 1; i < nums.length; i++) {
            if (nums[i] > currentVal) { // Since array is sorted, > means different
                currentVal = nums[i];
                nums[insertPos] = currentVal;
                insertPos++;
            }
        }

        return insertPos;
    }

    /**
     * Solution 5: Compact Two-Pointer (Most Concise)
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Very compact implementation of the two-pointer approach
     */
    public int removeDuplicatesCompact(int[] nums) {
        int i = 0;
        for (int num : nums) {
            if (i == 0 || num > nums[i - 1]) {
                nums[i++] = num;
            }
        }
        return i;
    }

    /**
     * Solution 6: Generic Template for K Duplicates
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * This can be adapted for "at most K duplicates" problems
     */
    public int removeDuplicatesTemplate(int[] nums, int k) {
        int i = 0;
        for (int num : nums) {
            if (i < k || num > nums[i - k]) {
                nums[i++] = num;
            }
        }
        return i;
    }

    // For the original problem, k = 1 (at most 1 occurrence of each element)
    public int removeDuplicatesUsingTemplate(int[] nums) {
        return removeDuplicatesTemplate(nums, 1);
    }

    // Helper method to visualize array state
    private void printArrayState(int[] nums, int k, String description) {
        System.out.print(description + ": [");
        for (int i = 0; i < nums.length; i++) {
            if (i == k && k < nums.length) {
                System.out.print("| "); // Show boundary between valid and invalid elements
            }
            System.out.print(nums[i]);
            if (i < nums.length - 1)
                System.out.print(", ");
        }
        System.out.println("] k = " + k);
    }

    // Test method to demonstrate usage
    public static void main(String[] args) {
        RemoveDuplicates solution = new RemoveDuplicates();

        // Test case 1: [1,1,2]
        System.out.println("=== Test Case 1 ===");
        int[] nums1 = { 1, 1, 2 };
        System.out.println("Original: " + java.util.Arrays.toString(nums1));
        int k1 = solution.removeDuplicates(nums1.clone());
        solution.printArrayState(nums1, k1, "Result");

        // Test case 2: [0,0,1,1,1,2,2,3,3,4]
        System.out.println("\n=== Test Case 2 ===");
        int[] nums2 = { 0, 0, 1, 1, 1, 2, 2, 3, 3, 4 };
        System.out.println("Original: " + java.util.Arrays.toString(nums2));
        int k2 = solution.removeDuplicates(nums2.clone());
        solution.printArrayState(nums2, k2, "Result");

        // Test case 3: Single element
        System.out.println("\n=== Test Case 3 (Single Element) ===");
        int[] nums3 = { 1 };
        System.out.println("Original: " + java.util.Arrays.toString(nums3));
        int k3 = solution.removeDuplicates(nums3.clone());
        solution.printArrayState(nums3, k3, "Result");

        // Test case 4: All same elements
        System.out.println("\n=== Test Case 4 (All Same) ===");
        int[] nums4 = { 1, 1, 1, 1, 1 };
        System.out.println("Original: " + java.util.Arrays.toString(nums4));
        int k4 = solution.removeDuplicates(nums4.clone());
        solution.printArrayState(nums4, k4, "Result");

        // Test case 5: No duplicates
        System.out.println("\n=== Test Case 5 (No Duplicates) ===");
        int[] nums5 = { 1, 2, 3, 4, 5 };
        System.out.println("Original: " + java.util.Arrays.toString(nums5));
        int k5 = solution.removeDuplicates(nums5.clone());
        solution.printArrayState(nums5, k5, "Result");

        // Test case 6: Empty array
        System.out.println("\n=== Test Case 6 (Empty Array) ===");
        int[] nums6 = {};
        int k6 = solution.removeDuplicates(nums6);
        System.out.println("Empty array result: k = " + k6);

        // Demonstrate different approaches
        System.out.println("\n=== Comparing Different Approaches ===");
        int[] testArray = { 1, 1, 1, 2, 2, 3, 4, 4, 4, 5 };
        System.out.println("Test array: " + java.util.Arrays.toString(testArray));

        int[] arr1 = testArray.clone();
        int k_method1 = solution.removeDuplicates(arr1);
        solution.printArrayState(arr1, k_method1, "Method 1 (Standard)");

        int[] arr2 = testArray.clone();
        int k_method2 = solution.removeDuplicatesCompact(arr2);
        solution.printArrayState(arr2, k_method2, "Method 2 (Compact)");

        int[] arr3 = testArray.clone();
        int k_method3 = solution.removeDuplicatesUsingTemplate(arr3);
        solution.printArrayState(arr3, k_method3, "Method 3 (Template)");

        // Show first k elements clearly
        System.out.println("\nFirst " + k_method1 + " unique elements: " +
                java.util.Arrays.toString(java.util.Arrays.copyOf(arr1, k_method1)));
    }

}
