class SortColors {

    // Solution 1: Dutch National Flag Algorithm (Optimal - One Pass)
    // Time: O(n), Space: O(1)
    public void sortColors1(int[] nums) {
        int low = 0; // Boundary for 0's
        int mid = 0; // Current element
        int high = nums.length - 1; // Boundary for 2's

        while (mid <= high) {
            if (nums[mid] == 0) {
                // Swap with low boundary and move both pointers
                swap(nums, low, mid);
                low++;
                mid++;
            } else if (nums[mid] == 1) {
                // Already in correct position, just move mid
                mid++;
            } else { // nums[mid] == 2
                // Swap with high boundary, don't move mid yet
                // (need to check the swapped element)
                swap(nums, mid, high);
                high--;
            }
        }
    }

    // Solution 2: Two-Pass Counting Sort
    // Time: O(n), Space: O(1)
    public void sortColors2(int[] nums) {
        int count0 = 0, count1 = 0, count2 = 0;

        // First pass: count occurrences
        for (int num : nums) {
            if (num == 0)
                count0++;
            else if (num == 1)
                count1++;
            else
                count2++;
        }

        // Second pass: fill the array
        int i = 0;
        while (count0-- > 0)
            nums[i++] = 0;
        while (count1-- > 0)
            nums[i++] = 1;
        while (count2-- > 0)
            nums[i++] = 2;
    }

    // Solution 3: Three-Way Partitioning (Similar to Dutch Flag)
    // Time: O(n), Space: O(1)
    public void sortColors3(int[] nums) {
        int n = nums.length;
        int left = 0, right = n - 1, i = 0;

        // First, move all 0's to the left
        while (i <= right) {
            if (nums[i] == 0) {
                swap(nums, left++, i++);
            } else if (nums[i] == 2) {
                swap(nums, i, right--);
            } else {
                i++;
            }
        }
    }

    // Solution 4: Detailed Dutch Flag with Comments
    // Time: O(n), Space: O(1)
    public void sortColors4(int[] nums) {
        /*
         * Partitioning scheme:
         * [0...low-1] -> all 0's
         * [low...mid-1] -> all 1's
         * [mid...high] -> unknown (to be processed)
         * [high+1...n-1] -> all 2's
         */
        int low = 0, mid = 0, high = nums.length - 1;

        while (mid <= high) {
            switch (nums[mid]) {
                case 0:
                    swap(nums, low++, mid++);
                    break;
                case 1:
                    mid++;
                    break;
                case 2:
                    swap(nums, mid, high--);
                    break;
            }
        }
    }

    // Helper method to swap elements
    private void swap(int[] nums, int i, int j) {
        int temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }

    // Helper method to print array
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
        SortColors solution = new SortColors();

        // Test case 1
        int[] nums1 = { 2, 0, 2, 1, 1, 0 };
        System.out.print("Example 1 - Input: ");
        printArray(nums1);
        solution.sortColors1(nums1);
        System.out.print("Output: ");
        printArray(nums1); // [0, 0, 1, 1, 2, 2]

        // Test case 2
        int[] nums2 = { 2, 0, 1 };
        System.out.print("\nExample 2 - Input: ");
        printArray(nums2);
        solution.sortColors1(nums2);
        System.out.print("Output: ");
        printArray(nums2); // [0, 1, 2]

        // Test case 3: Already sorted
        int[] nums3 = { 0, 1, 2 };
        System.out.print("\nAlready sorted - Input: ");
        printArray(nums3);
        solution.sortColors2(nums3);
        System.out.print("Output: ");
        printArray(nums3); // [0, 1, 2]

        // Test case 4: All same color
        int[] nums4 = { 1, 1, 1, 1 };
        System.out.print("\nAll same - Input: ");
        printArray(nums4);
        solution.sortColors3(nums4);
        System.out.print("Output: ");
        printArray(nums4); // [1, 1, 1, 1]

        // Test case 5: Reverse sorted
        int[] nums5 = { 2, 2, 1, 1, 0, 0 };
        System.out.print("\nReverse sorted - Input: ");
        printArray(nums5);
        solution.sortColors4(nums5);
        System.out.print("Output: ");
        printArray(nums5); // [0, 0, 1, 1, 2, 2]

        // Test case 6: Single element
        int[] nums6 = { 0 };
        System.out.print("\nSingle element - Input: ");
        printArray(nums6);
        solution.sortColors1(nums6);
        System.out.print("Output: ");
        printArray(nums6); // [0]
    }

}
