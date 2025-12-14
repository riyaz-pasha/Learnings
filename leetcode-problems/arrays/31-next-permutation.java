/*
 * A permutation of an array of integers is an arrangement of its members into a
 * sequence or linear order.
 * 
 * For example, for arr = [1,2,3], the following are all the permutations of
 * arr: [1,2,3], [1,3,2], [2, 1, 3], [2, 3, 1], [3,1,2], [3,2,1].
 * The next permutation of an array of integers is the next lexicographically
 * greater permutation of its integer. More formally, if all the permutations of
 * the array are sorted in one container according to their lexicographical
 * order, then the next permutation of that array is the permutation that
 * follows it in the sorted container. If such arrangement is not possible, the
 * array must be rearranged as the lowest possible order (i.e., sorted in
 * ascending order).
 * 
 * For example, the next permutation of arr = [1,2,3] is [1,3,2].
 * Similarly, the next permutation of arr = [2,3,1] is [3,1,2].
 * While the next permutation of arr = [3,2,1] is [1,2,3] because [3,2,1] does
 * not have a lexicographical larger rearrangement.
 * Given an array of integers nums, find the next permutation of nums.
 * 
 * The replacement must be in place and use only constant extra memory.
 * 
 * 
 * 
 * Example 1:
 * 
 * Input: nums = [1,2,3]
 * Output: [1,3,2]
 * Example 2:
 * 
 * Input: nums = [3,2,1]
 * Output: [1,2,3]
 * Example 3:
 * 
 * Input: nums = [1,1,5]
 * Output: [1,5,1]
 */

class NextPermutationSolution {

    /*
     * 1. Traverse from the end and find the first index where nums[i] < nums[i+1]
     * (pivot)
     * 2. Traverse again from the end and find the first element greater than pivot
     * and swap them
     * 3. Reverse the array to the right of the pivot
     * 4. If no pivot exists, reverse the whole array
     */

    public void nextPermutation(int[] nums) {
        int n = nums.length;
        int pivotIndex = -1;

        // Step 1: find pivot
        for (int i = n - 2; i >= 0; i--) {
            if (nums[i] < nums[i + 1]) {
                pivotIndex = i;
                break;
            }
        }

        // Step 4: if no pivot, reverse whole array
        if (pivotIndex == -1) {
            reverse(nums, 0, n - 1);
            return;
        }

        // Step 2: find rightmost element greater than pivot
        for (int i = n - 1; i > pivotIndex; i--) {
            if (nums[i] > nums[pivotIndex]) {
                swap(nums, pivotIndex, i);
                break;
            }
        }

        // Step 3: reverse suffix
        reverse(nums, pivotIndex + 1, n - 1);
    }

    private void reverse(int[] nums, int start, int end) {
        while (start < end) {
            swap(nums, start, end);
            start++;
            end--;
        }
    }

    private void swap(int[] nums, int i, int j) {
        int temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }
}

class NextPermutation {

    // Solution 1: Standard Algorithm (Optimal)
    // Time: O(n), Space: O(1)
    public void nextPermutation(int[] nums) {
        int n = nums.length;

        // Step 1: Find the first decreasing element from right to left
        int i = n - 2;
        while (i >= 0 && nums[i] >= nums[i + 1]) {
            i--;
        }

        // Step 2: If found, find the element just larger than nums[i]
        if (i >= 0) {
            int j = n - 1;
            while (j >= 0 && nums[j] <= nums[i]) {
                j--;
            }
            // Step 3: Swap them
            swap(nums, i, j);
        }

        // Step 4: Reverse the suffix (from i+1 to end)
        reverse(nums, i + 1, n - 1);
    }

    // Solution 2: Detailed Step-by-Step with Comments
    // Time: O(n), Space: O(1)
    public void nextPermutation2(int[] nums) {
        /*
         * Algorithm:
         * 1. Find rightmost ascending pair (i, i+1) where nums[i] < nums[i+1]
         * 2. Find smallest element to right of i that's larger than nums[i]
         * 3. Swap these two elements
         * 4. Reverse everything after position i
         */

        int n = nums.length;
        if (n <= 1)
            return;

        // Step 1: Find pivot (rightmost element where nums[i] < nums[i+1])
        int pivot = -1;
        for (int i = n - 2; i >= 0; i--) {
            if (nums[i] < nums[i + 1]) {
                pivot = i;
                break;
            }
        }

        // If no pivot found, array is in descending order
        // Reverse entire array to get smallest permutation
        if (pivot == -1) {
            reverse(nums, 0, n - 1);
            return;
        }

        // Step 2: Find the smallest element greater than nums[pivot]
        // from the right side
        int successor = -1;
        for (int i = n - 1; i > pivot; i--) {
            if (nums[i] > nums[pivot]) {
                successor = i;
                break;
            }
        }

        // Step 3: Swap pivot with successor
        swap(nums, pivot, successor);

        // Step 4: Reverse the suffix after pivot
        reverse(nums, pivot + 1, n - 1);
    }

    // Solution 3: With Visual Example in Comments
    // Time: O(n), Space: O(1)
    public void nextPermutation3(int[] nums) {
        /*
         * Example: [1, 3, 5, 4, 2]
         * 
         * Step 1: Find pivot (first decrease from right)
         * [1, 3, 5, 4, 2]
         * ^ pivot (3 < 5)
         * 
         * Step 2: Find successor (smallest > pivot from right)
         * [1, 3, 5, 4, 2]
         * ^ successor (4 > 3)
         * 
         * Step 3: Swap pivot and successor
         * [1, 4, 5, 3, 2]
         * 
         * Step 4: Reverse suffix after pivot position
         * [1, 4, 2, 3, 5]
         * --------
         * reversed
         */

        int n = nums.length;
        int i = n - 2;

        // Find pivot
        while (i >= 0 && nums[i] >= nums[i + 1]) {
            i--;
        }

        if (i >= 0) {
            // Find successor
            int j = n - 1;
            while (nums[j] <= nums[i]) {
                j--;
            }
            // Swap
            swap(nums, i, j);
        }

        // Reverse suffix
        reverse(nums, i + 1, n - 1);
    }

    // Helper: Swap two elements
    private void swap(int[] nums, int i, int j) {
        int temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }

    // Helper: Reverse array from start to end (inclusive)
    private void reverse(int[] nums, int start, int end) {
        while (start < end) {
            swap(nums, start, end);
            start++;
            end--;
        }
    }

    // Helper: Print array
    private static void printArray(int[] nums) {
        System.out.print("[");
        for (int i = 0; i < nums.length; i++) {
            System.out.print(nums[i]);
            if (i < nums.length - 1)
                System.out.print(", ");
        }
        System.out.println("]");
    }

    // Test cases
    public static void main(String[] args) {
        NextPermutation solution = new NextPermutation();

        // Test case 1
        int[] nums1 = { 1, 2, 3 };
        System.out.print("Example 1 - Input: ");
        printArray(nums1);
        solution.nextPermutation(nums1);
        System.out.print("Output: ");
        printArray(nums1); // [1, 3, 2]

        // Test case 2
        int[] nums2 = { 3, 2, 1 };
        System.out.print("\nExample 2 - Input: ");
        printArray(nums2);
        solution.nextPermutation(nums2);
        System.out.print("Output: ");
        printArray(nums2); // [1, 2, 3]

        // Test case 3
        int[] nums3 = { 1, 1, 5 };
        System.out.print("\nExample 3 - Input: ");
        printArray(nums3);
        solution.nextPermutation(nums3);
        System.out.print("Output: ");
        printArray(nums3); // [1, 5, 1]

        // Test case 4: Single element
        int[] nums4 = { 1 };
        System.out.print("\nSingle element - Input: ");
        printArray(nums4);
        solution.nextPermutation(nums4);
        System.out.print("Output: ");
        printArray(nums4); // [1]

        // Test case 5: Two elements
        int[] nums5 = { 1, 2 };
        System.out.print("\nTwo elements - Input: ");
        printArray(nums5);
        solution.nextPermutation(nums5);
        System.out.print("Output: ");
        printArray(nums5); // [2, 1]

        // Test case 6: Complex example
        int[] nums6 = { 1, 3, 5, 4, 2 };
        System.out.print("\nComplex - Input: ");
        printArray(nums6);
        solution.nextPermutation2(nums6);
        System.out.print("Output: ");
        printArray(nums6); // [1, 4, 2, 3, 5]

        // Test case 7: All same elements
        int[] nums7 = { 2, 2, 2 };
        System.out.print("\nAll same - Input: ");
        printArray(nums7);
        solution.nextPermutation(nums7);
        System.out.print("Output: ");
        printArray(nums7); // [2, 2, 2]

        // Test case 8: Show sequence
        System.out.println("\n--- Permutation Sequence ---");
        int[] seq = { 1, 2, 3 };
        System.out.print("Start: ");
        printArray(seq);
        for (int i = 1; i <= 6; i++) {
            solution.nextPermutation3(seq);
            System.out.print("Step " + i + ": ");
            printArray(seq);
        }
    }
}