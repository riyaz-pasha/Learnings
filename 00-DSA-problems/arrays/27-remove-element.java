/*
 * Given an integer array nums and an integer val, remove all occurrences of val
 * in nums in-place. The order of the elements may be changed. Then return the
 * number of elements in nums which are not equal to val.
 * 
 * Consider the number of elements in nums which are not equal to val be k, to
 * get accepted, you need to do the following things:
 * 
 * Change the array nums such that the first k elements of nums contain the
 * elements which are not equal to val. The remaining elements of nums are not
 * important as well as the size of nums.
 * Return k.
 * 
 * Example 1:
 * Input: nums = [3,2,2,3], val = 3
 * Output: 2, nums = [2,2,_,_]
 * Explanation: Your function should return k = 2, with the first two elements
 * of nums being 2.
 * It does not matter what you leave beyond the returned k (hence they are
 * underscores).
 * 
 * Example 2:
 * Input: nums = [0,1,2,2,3,0,4,2], val = 2
 * Output: 5, nums = [0,1,4,0,3,_,_,_]
 * Explanation: Your function should return k = 5, with the first five elements
 * of nums containing 0, 0, 1, 3, and 4.
 * Note that the five elements can be returned in any order.
 */

class RemoveElement {

    /**
     * Solution 1: Two Pointers (Fast and Slow) - Most Common
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Keep all non-val elements at the beginning of the array
     */
    public int removeElement(int[] nums, int val) {
        int k = 0; // Slow pointer - position for next non-val element

        for (int i = 0; i < nums.length; i++) { // Fast pointer
            if (nums[i] != val) {
                nums[k] = nums[i];
                k++;
            }
        }

        return k;
    }

    /**
     * Solution 2: Two Pointers (Optimized for Few Elements to Remove)
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * When elements to remove are rare, avoid unnecessary copies
     */
    public int removeElementOptimized(int[] nums, int val) {
        int i = 0;
        int n = nums.length;

        while (i < n) {
            if (nums[i] == val) {
                // Replace current element with the last element
                nums[i] = nums[n - 1];
                n--; // Reduce array size
                // Don't increment i, check the new element at position i
            } else {
                i++; // Move to next element
            }
        }

        return n;
    }

    /**
     * Solution 3: Two Pointers (Left and Right)
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Use left and right pointers moving towards each other
     */
    public int removeElementTwoPointers(int[] nums, int val) {
        int left = 0;
        int right = nums.length - 1;

        while (left <= right) {
            if (nums[left] == val) {
                // Swap with element from right
                nums[left] = nums[right];
                right--;
            } else {
                left++;
            }
        }

        return left;
    }

    /**
     * Solution 4: Single Pass with Counter
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Simple approach counting valid elements while moving them forward
     */
    public int removeElementCounter(int[] nums, int val) {
        int writeIndex = 0;

        for (int readIndex = 0; readIndex < nums.length; readIndex++) {
            if (nums[readIndex] != val) {
                if (writeIndex != readIndex) {
                    nums[writeIndex] = nums[readIndex];
                }
                writeIndex++;
            }
        }

        return writeIndex;
    }

    /**
     * Solution 5: Partition Style (Similar to QuickSort Partition)
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Partition array into two parts: non-val elements and val elements
     */
    public int removeElementPartition(int[] nums, int val) {
        int partitionIndex = 0;

        for (int i = 0; i < nums.length; i++) {
            if (nums[i] != val) {
                // Swap elements
                int temp = nums[partitionIndex];
                nums[partitionIndex] = nums[i];
                nums[i] = temp;
                partitionIndex++;
            }
        }

        return partitionIndex;
    }

    // Helper method to print array state
    private void printArray(int[] nums, int k, String description) {
        System.out.print(description + ": [");
        for (int i = 0; i < nums.length; i++) {
            if (i < k) {
                System.out.print(nums[i]);
            } else {
                System.out.print("_"); // Show that these elements don't matter
            }
            if (i < nums.length - 1)
                System.out.print(", ");
        }
        System.out.println("] k = " + k);
    }

    // Test method to demonstrate usage
    public static void main(String[] args) {
        RemoveElement solution = new RemoveElement();

        // Test case 1: [3,2,2,3], val = 3
        System.out.println("=== Test Case 1 ===");
        int[] nums1 = { 3, 2, 2, 3 };
        int val1 = 3;
        System.out.println("Original: " + java.util.Arrays.toString(nums1) + ", val = " + val1);
        int k1 = solution.removeElement(nums1.clone(), val1);
        solution.printArray(nums1, k1, "Result");

        // Test case 2: [0,1,2,2,3,0,4,2], val = 2
        System.out.println("\n=== Test Case 2 ===");
        int[] nums2 = { 0, 1, 2, 2, 3, 0, 4, 2 };
        int val2 = 2;
        System.out.println("Original: " + java.util.Arrays.toString(nums2) + ", val = " + val2);
        int k2 = solution.removeElement(nums2.clone(), val2);
        solution.printArray(nums2, k2, "Result");

        // Test case 3: Edge case - empty array
        System.out.println("\n=== Test Case 3 (Empty Array) ===");
        int[] nums3 = {};
        int val3 = 1;
        int k3 = solution.removeElement(nums3, val3);
        System.out.println("Empty array result: k = " + k3);

        // Test case 4: All elements are val
        System.out.println("\n=== Test Case 4 (All Elements Same) ===");
        int[] nums4 = { 2, 2, 2, 2 };
        int val4 = 2;
        System.out.println("Original: " + java.util.Arrays.toString(nums4) + ", val = " + val4);
        int k4 = solution.removeElement(nums4.clone(), val4);
        solution.printArray(nums4, k4, "Result");

        // Test case 5: No elements equal to val
        System.out.println("\n=== Test Case 5 (No Elements to Remove) ===");
        int[] nums5 = { 1, 2, 3, 4 };
        int val5 = 5;
        System.out.println("Original: " + java.util.Arrays.toString(nums5) + ", val = " + val5);
        int k5 = solution.removeElement(nums5.clone(), val5);
        solution.printArray(nums5, k5, "Result");

        // Compare different approaches
        System.out.println("\n=== Comparing Different Approaches ===");
        int[] testArray = { 0, 1, 2, 2, 3, 0, 4, 2 };
        int testVal = 2;

        int[] arr1 = testArray.clone();
        int k_method1 = solution.removeElement(arr1, testVal);
        solution.printArray(arr1, k_method1, "Method 1 (Fast/Slow)");

        int[] arr2 = testArray.clone();
        int k_method2 = solution.removeElementOptimized(arr2, testVal);
        solution.printArray(arr2, k_method2, "Method 2 (Optimized)");

        int[] arr3 = testArray.clone();
        int k_method3 = solution.removeElementTwoPointers(arr3, testVal);
        solution.printArray(arr3, k_method3, "Method 3 (Left/Right)");
    }

}
