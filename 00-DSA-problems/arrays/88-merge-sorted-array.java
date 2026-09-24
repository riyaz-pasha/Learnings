/*
 * You are given two integer arrays nums1 and nums2, sorted in non-decreasing
 * order, and two integers m and n, representing the number of elements in nums1
 * and nums2 respectively.
 * 
 * Merge nums1 and nums2 into a single array sorted in non-decreasing order.
 * 
 * The final sorted array should not be returned by the function, but instead be
 * stored inside the array nums1. To accommodate this, nums1 has a length of m +
 * n, where the first m elements denote the elements that should be merged, and
 * the last n elements are set to 0 and should be ignored. nums2 has a length of
 * n.
 * 
 * Example 1:
 * Input: nums1 = [1,2,3,0,0,0], m = 3, nums2 = [2,5,6], n = 3
 * Output: [1,2,2,3,5,6]
 * Explanation: The arrays we are merging are [1,2,3] and [2,5,6].
 * The result of the merge is [1,2,2,3,5,6] with the underlined elements coming
 * from nums1.
 * 
 * Example 2:
 * Input: nums1 = [1], m = 1, nums2 = [], n = 0
 * Output: [1]
 * Explanation: The arrays we are merging are [1] and [].
 * The result of the merge is [1].
 * 
 * Example 3:
 * Input: nums1 = [0], m = 0, nums2 = [1], n = 1
 * Output: [1]
 * Explanation: The arrays we are merging are [] and [1].
 * The result of the merge is [1].
 * Note that because m = 0, there are no elements in nums1. The 0 is only there
 * to ensure the merge result can fit in nums1.
 */

class MergeSortedArrays {

    /**
     * Solution 1: Two Pointers (Backward) - Most Optimal
     * Time Complexity: O(m + n)
     * Space Complexity: O(1)
     * 
     * Key insight: Start from the end to avoid overwriting elements
     */
    public void merge(int[] nums1, int m, int[] nums2, int n) {
        int i = m - 1; // Last element in nums1's valid part
        int j = n - 1; // Last element in nums2
        int k = m + n - 1; // Last position in nums1

        // Merge from the end
        while (i >= 0 && j >= 0) {
            if (nums1[i] > nums2[j]) {
                nums1[k] = nums1[i];
                i--;
            } else {
                nums1[k] = nums2[j];
                j--;
            }
            k--;
        }

        // Copy remaining elements from nums2 (if any)
        while (j >= 0) {
            nums1[k] = nums2[j];
            j--;
            k--;
        }

        // No need to copy remaining from nums1 as they're already in place
    }

    /**
     * Solution 2: Two Pointers (Forward with Extra Space)
     * Time Complexity: O(m + n)
     * Space Complexity: O(m)
     * 
     * Create a copy of nums1's valid elements to avoid overwriting
     */
    public void mergeWithExtraSpace(int[] nums1, int m, int[] nums2, int n) {
        // Create a copy of the first m elements of nums1
        int[] nums1Copy = new int[m];
        System.arraycopy(nums1, 0, nums1Copy, 0, m);

        int i = 0, j = 0, k = 0;

        // Merge elements from nums1Copy and nums2
        while (i < m && j < n) {
            if (nums1Copy[i] <= nums2[j]) {
                nums1[k] = nums1Copy[i];
                i++;
            } else {
                nums1[k] = nums2[j];
                j++;
            }
            k++;
        }

        // Copy remaining elements
        while (i < m) {
            nums1[k] = nums1Copy[i];
            i++;
            k++;
        }

        while (j < n) {
            nums1[k] = nums2[j];
            j++;
            k++;
        }
    }

    /**
     * Solution 3: Using Built-in Sort
     * Time Complexity: O((m + n) log(m + n))
     * Space Complexity: O(1) or O(log(m + n)) depending on sort implementation
     * 
     * Simple but less efficient approach
     */
    public void mergeWithSort(int[] nums1, int m, int[] nums2, int n) {
        // Copy nums2 elements to the end of nums1
        System.arraycopy(nums2, 0, nums1, m, n);

        // Sort the entire array
        java.util.Arrays.sort(nums1);
    }

    /**
     * Solution 4: Insertion Sort Style
     * Time Complexity: O(m * n) in worst case
     * Space Complexity: O(1)
     * 
     * Insert each element from nums2 into its correct position in nums1
     */
    public void mergeInsertionStyle(int[] nums1, int m, int[] nums2, int n) {
        for (int i = 0; i < n; i++) {
            int elementToInsert = nums2[i];
            int insertPos = m + i;

            // Find the correct position to insert
            while (insertPos > 0 && nums1[insertPos - 1] > elementToInsert) {
                nums1[insertPos] = nums1[insertPos - 1];
                insertPos--;
            }
            nums1[insertPos] = elementToInsert;
        }
    }

    // Test method to demonstrate usage
    public static void main(String[] args) {
        MergeSortedArrays solution = new MergeSortedArrays();

        // Test case 1
        int[] nums1 = { 1, 2, 3, 0, 0, 0 };
        int[] nums2 = { 2, 5, 6 };
        int m = 3, n = 3;

        System.out.println("Before merge: " + java.util.Arrays.toString(nums1));
        solution.merge(nums1, m, nums2, n);
        System.out.println("After merge: " + java.util.Arrays.toString(nums1));

        // Test case 2
        int[] nums1_2 = { 1 };
        int[] nums2_2 = {};
        solution.merge(nums1_2, 1, nums2_2, 0);
        System.out.println("Test case 2: " + java.util.Arrays.toString(nums1_2));

        // Test case 3
        int[] nums1_3 = { 0 };
        int[] nums2_3 = { 1 };
        solution.merge(nums1_3, 0, nums2_3, 1);
        System.out.println("Test case 3: " + java.util.Arrays.toString(nums1_3));
    }

}
