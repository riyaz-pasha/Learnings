/*
 * Given an integer array nums sorted in non-decreasing order, remove some
 * duplicates in-place such that each unique element appears at most twice. The
 * relative order of the elements should be kept the same.
 * 
 * Since it is impossible to change the length of the array in some languages,
 * you must instead have the result be placed in the first part of the array
 * nums. More formally, if there are k elements after removing the duplicates,
 * then the first k elements of nums should hold the final result. It does not
 * matter what you leave beyond the first k elements.
 * 
 * Return k after placing the final result in the first k slots of nums.
 * 
 * Do not allocate extra space for another array. You must do this by modifying
 * the input array in-place with O(1) extra memory.
 */

class RemoveDuplicatesAtMostTwo {

    /**
     * Solution 1: Two Pointers with Counter - Most Intuitive
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Keep track of current element and its count
     */
    public int removeDuplicates(int[] nums) {
        if (nums.length <= 2)
            return nums.length;

        int writePos = 1; // Position to write next valid element
        int count = 1; // Count of current element

        for (int i = 1; i < nums.length; i++) {
            if (nums[i] == nums[i - 1]) {
                count++;
            } else {
                count = 1; // Reset count for new element
            }

            // Only keep element if count <= 2
            if (count <= 2) {
                nums[writePos] = nums[i];
                writePos++;
            }
        }

        return writePos;
    }

    /**
     * Solution 2: Two Pointers (Elegant Pattern) - Most Optimal
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Key insight: For at most 2 duplicates, compare with element at (i-2)
     * If nums[i] != nums[i-2], then nums[i] is valid to keep
     */
    public int removeDuplicatesOptimal(int[] nums) {
        if (nums.length <= 2)
            return nums.length;

        int writePos = 2; // Start from index 2 (first two elements are always valid)

        for (int i = 2; i < nums.length; i++) {
            // If current element is different from element 2 positions back,
            // it means we can safely include it (at most 2 duplicates)
            if (nums[i] != nums[writePos - 2]) {
                nums[writePos] = nums[i];
                writePos++;
            }
        }

        return writePos;
    }

    /**
     * Solution 3: Generic Template for K Duplicates
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * This template works for "at most K duplicates" problems
     */
    public int removeDuplicatesGeneric(int[] nums, int k) {
        if (nums.length <= k)
            return nums.length;

        int writePos = k; // First k elements are always valid

        for (int i = k; i < nums.length; i++) {
            // If current element is different from element k positions back,
            // it's safe to include (ensures at most k duplicates)
            if (nums[i] != nums[writePos - k]) {
                nums[writePos] = nums[i];
                writePos++;
            }
        }

        return writePos;
    }

    // For this problem, k = 2
    public int removeDuplicatesUsingGeneric(int[] nums) {
        return removeDuplicatesGeneric(nums, 2);
    }

    /**
     * Solution 4: State Machine Approach
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Track state: how many times have we seen current element
     */
    public int removeDuplicatesStateMachine(int[] nums) {
        if (nums.length <= 2)
            return nums.length;

        int writePos = 0;
        int currentElement = nums[0];
        int currentCount = 0;

        for (int i = 0; i < nums.length; i++) {
            if (nums[i] == currentElement) {
                currentCount++;
                if (currentCount <= 2) {
                    nums[writePos] = nums[i];
                    writePos++;
                }
            } else {
                // New element found
                currentElement = nums[i];
                currentCount = 1;
                nums[writePos] = nums[i];
                writePos++;
            }
        }

        return writePos;
    }

    /**
     * Solution 5: Sliding Window Approach
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Use a sliding window to check if we can include current element
     */
    public int removeDuplicatesSlidingWindow(int[] nums) {
        if (nums.length <= 2)
            return nums.length;

        int writePos = 2;

        for (int i = 2; i < nums.length; i++) {
            // Check if including nums[i] would create more than 2 consecutive duplicates
            // By comparing with the element that would be 2 positions before in result
            // array
            if (!(nums[i] == nums[writePos - 1] && nums[i] == nums[writePos - 2])) {
                nums[writePos] = nums[i];
                writePos++;
            }
        }

        return writePos;
    }

    /**
     * Solution 6: Three Pointers Approach
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Use three pointers for more explicit control
     */
    public int removeDuplicatesThreePointers(int[] nums) {
        if (nums.length <= 2)
            return nums.length;

        int slow = 1; // Next position to write
        int fast = 1; // Current reading position
        int prev = 0; // Previous element position

        while (fast < nums.length) {
            // If current element is different from previous, or
            // if it's same but we haven't seen it twice yet
            if (nums[fast] != nums[prev] ||
                    (nums[fast] == nums[prev] && slow - prev == 1)) {
                nums[slow] = nums[fast];
                if (nums[fast] != nums[prev]) {
                    prev = slow; // Update prev only when element changes
                }
                slow++;
            }
            fast++;
        }

        return slow;
    }

    // Helper method to visualize array state
    private void printArrayState(int[] nums, int k, String description) {
        System.out.print(description + ": [");
        for (int i = 0; i < nums.length; i++) {
            if (i == k && k < nums.length) {
                System.out.print("| "); // Show boundary
            }
            System.out.print(nums[i]);
            if (i < nums.length - 1)
                System.out.print(", ");
        }
        System.out.println("] k = " + k);
    }

    // Helper method to count element frequencies in first k positions
    private void showFrequencies(int[] nums, int k) {
        java.util.Map<Integer, Integer> freq = new java.util.HashMap<>();
        for (int i = 0; i < k; i++) {
            freq.put(nums[i], freq.getOrDefault(nums[i], 0) + 1);
        }
        System.out.println("Frequencies in first " + k + " elements: " + freq);
    }

    // Test method to demonstrate usage
    public static void main(String[] args) {
        RemoveDuplicatesAtMostTwo solution = new RemoveDuplicatesAtMostTwo();

        // Test case 1: [1,1,1,2,2,3]
        System.out.println("=== Test Case 1 ===");
        int[] nums1 = { 1, 1, 1, 2, 2, 3 };
        System.out.println("Original: " + java.util.Arrays.toString(nums1));
        int k1 = solution.removeDuplicates(nums1.clone());
        solution.printArrayState(nums1, k1, "Result");
        solution.showFrequencies(nums1, k1);

        // Test case 2: [0,0,1,1,1,1,2,3,3]
        System.out.println("\n=== Test Case 2 ===");
        int[] nums2 = { 0, 0, 1, 1, 1, 1, 2, 3, 3 };
        System.out.println("Original: " + java.util.Arrays.toString(nums2));
        int k2 = solution.removeDuplicates(nums2.clone());
        solution.printArrayState(nums2, k2, "Result");
        solution.showFrequencies(nums2, k2);

        // Test case 3: [1,1,1,1,1,1]
        System.out.println("\n=== Test Case 3 (All Same) ===");
        int[] nums3 = { 1, 1, 1, 1, 1, 1 };
        System.out.println("Original: " + java.util.Arrays.toString(nums3));
        int k3 = solution.removeDuplicates(nums3.clone());
        solution.printArrayState(nums3, k3, "Result");
        solution.showFrequencies(nums3, k3);

        // Test case 4: [1,2,3] (No duplicates)
        System.out.println("\n=== Test Case 4 (No Duplicates) ===");
        int[] nums4 = { 1, 2, 3 };
        System.out.println("Original: " + java.util.Arrays.toString(nums4));
        int k4 = solution.removeDuplicates(nums4.clone());
        solution.printArrayState(nums4, k4, "Result");

        // Test case 5: [1,1] (Exactly two duplicates)
        System.out.println("\n=== Test Case 5 (Exactly Two) ===");
        int[] nums5 = { 1, 1 };
        System.out.println("Original: " + java.util.Arrays.toString(nums5));
        int k5 = solution.removeDuplicates(nums5.clone());
        solution.printArrayState(nums5, k5, "Result");

        // Test case 6: Single element
        System.out.println("\n=== Test Case 6 (Single Element) ===");
        int[] nums6 = { 1 };
        System.out.println("Original: " + java.util.Arrays.toString(nums6));
        int k6 = solution.removeDuplicates(nums6.clone());
        solution.printArrayState(nums6, k6, "Result");

        // Demonstrate different approaches
        System.out.println("\n=== Comparing Different Approaches ===");
        int[] testArray = { 1, 1, 1, 2, 2, 2, 2, 3, 3, 4, 4, 4, 4, 4 };
        System.out.println("Test array: " + java.util.Arrays.toString(testArray));

        int[] arr1 = testArray.clone();
        int k_method1 = solution.removeDuplicates(arr1);
        solution.printArrayState(arr1, k_method1, "Method 1 (Counter)");

        int[] arr2 = testArray.clone();
        int k_method2 = solution.removeDuplicatesOptimal(arr2);
        solution.printArrayState(arr2, k_method2, "Method 2 (Optimal)");

        int[] arr3 = testArray.clone();
        int k_method3 = solution.removeDuplicatesUsingGeneric(arr3);
        solution.printArrayState(arr3, k_method3, "Method 3 (Generic)");

        // Show final result clearly
        System.out.println("\nFinal result: " +
                java.util.Arrays.toString(java.util.Arrays.copyOf(arr1, k_method1)));

        // Test generic template with different k values
        System.out.println("\n=== Testing Generic Template ===");
        int[] testK = { 1, 1, 1, 1, 2, 2, 2, 3, 3, 3, 3 };
        System.out.println("Original: " + java.util.Arrays.toString(testK));

        int[] testK1 = testK.clone();
        int kResult1 = solution.removeDuplicatesGeneric(testK1, 1);
        solution.printArrayState(testK1, kResult1, "At most 1 duplicate");

        int[] testK3 = testK.clone();
        int kResult3 = solution.removeDuplicatesGeneric(testK3, 3);
        solution.printArrayState(testK3, kResult3, "At most 3 duplicates");
    }

}
