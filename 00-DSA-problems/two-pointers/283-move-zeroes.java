class MoveZeros {

    // Solution 1: Two-Pointer Approach (Optimal)
    // Time: O(n), Space: O(1)
    public void moveZeroes1(int[] nums) {
        int nonZeroPos = 0; // Position to place next non-zero element

        // Move all non-zero elements to the front
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] != 0) {
                nums[nonZeroPos] = nums[i];
                nonZeroPos++;
            }
        }

        // Fill remaining positions with zeros
        for (int i = nonZeroPos; i < nums.length; i++) {
            nums[i] = 0;
        }
    }

    // Solution 2: Swap Approach (Single Pass)
    // Time: O(n), Space: O(1)
    public void moveZeroes2(int[] nums) {
        int nonZeroPos = 0;

        // Swap non-zero elements with the position at nonZeroPos
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] != 0) {
                // Swap only if positions are different
                if (i != nonZeroPos) {
                    int temp = nums[nonZeroPos];
                    nums[nonZeroPos] = nums[i];
                    nums[i] = temp;
                }
                nonZeroPos++;
            }
        }
    }

    // Solution 3: Snowball Approach
    // Time: O(n), Space: O(1)
    public void moveZeroes3(int[] nums) {
        int snowballSize = 0; // Size of the zero "snowball"

        for (int i = 0; i < nums.length; i++) {
            if (nums[i] == 0) {
                snowballSize++;
            } else if (snowballSize > 0) {
                // Swap current element with the first zero in the snowball
                int temp = nums[i];
                nums[i] = 0;
                nums[i - snowballSize] = temp;
            }
        }
    }

    // Test cases
    public static void main(String[] args) {
        MoveZeros solution = new MoveZeros();

        // Test case 1
        int[] nums1 = { 0, 1, 0, 3, 12 };
        solution.moveZeroes1(nums1);
        System.out.print("Example 1: ");
        printArray(nums1); // [1, 3, 12, 0, 0]

        // Test case 2
        int[] nums2 = { 0 };
        solution.moveZeroes1(nums2);
        System.out.print("Example 2: ");
        printArray(nums2); // [0]

        // Test case 3
        int[] nums3 = { 1, 2, 3, 4, 5 };
        solution.moveZeroes2(nums3);
        System.out.print("No zeros: ");
        printArray(nums3); // [1, 2, 3, 4, 5]

        // Test case 4
        int[] nums4 = { 0, 0, 0, 1 };
        solution.moveZeroes3(nums4);
        System.out.print("Multiple zeros: ");
        printArray(nums4); // [1, 0, 0, 0]
    }

    private static void printArray(int[] nums) {
        System.out.print("[");
        for (int i = 0; i < nums.length; i++) {
            System.out.print(nums[i]);
            if (i < nums.length - 1)
                System.out.print(", ");
        }
        System.out.println("]");
    }

}
