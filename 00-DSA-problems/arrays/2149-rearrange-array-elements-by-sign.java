class RearrangeArrayBySign {

    // Solution 1: Two-Pointer with Result Array (Optimal)
    // Time: O(n), Space: O(n)
    public int[] rearrangeArray1(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        int posIndex = 0; // Index for positive numbers (0, 2, 4, ...)
        int negIndex = 1; // Index for negative numbers (1, 3, 5, ...)

        for (int num : nums) {
            if (num > 0) {
                result[posIndex] = num;
                posIndex += 2;
            } else {
                result[negIndex] = num;
                negIndex += 2;
            }
        }

        return result;
    }

    // Solution 2: Separate Lists then Merge
    // Time: O(n), Space: O(n)
    public int[] rearrangeArray2(int[] nums) {
        java.util.List<Integer> positive = new java.util.ArrayList<>();
        java.util.List<Integer> negative = new java.util.ArrayList<>();

        // Separate positive and negative numbers
        for (int num : nums) {
            if (num > 0) {
                positive.add(num);
            } else {
                negative.add(num);
            }
        }

        // Merge alternately
        int[] result = new int[nums.length];
        for (int i = 0; i < positive.size(); i++) {
            result[2 * i] = positive.get(i);
            result[2 * i + 1] = negative.get(i);
        }

        return result;
    }

    // Solution 3: Single Pass with Queue-like Approach
    // Time: O(n), Space: O(n)
    public int[] rearrangeArray3(int[] nums) {
        int n = nums.length;
        int[] positives = new int[n / 2];
        int[] negatives = new int[n / 2];
        int posCount = 0, negCount = 0;

        // Collect positives and negatives separately
        for (int num : nums) {
            if (num > 0) {
                positives[posCount++] = num;
            } else {
                negatives[negCount++] = num;
            }
        }

        // Merge alternately
        int[] result = new int[n];
        for (int i = 0; i < n / 2; i++) {
            result[2 * i] = positives[i];
            result[2 * i + 1] = negatives[i];
        }

        return result;
    }

    // Solution 4: In-place with Auxiliary Space (Detailed)
    // Time: O(n), Space: O(n)
    public int[] rearrangeArray4(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];

        // Two pointers: one for placing positives, one for negatives
        int evenPos = 0; // Even indices (0, 2, 4...) for positive
        int oddPos = 1; // Odd indices (1, 3, 5...) for negative

        for (int i = 0; i < n; i++) {
            if (nums[i] > 0) {
                result[evenPos] = nums[i];
                evenPos += 2;
            } else {
                result[oddPos] = nums[i];
                oddPos += 2;
            }
        }

        return result;
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

    // Helper method to verify result
    private static boolean verify(int[] nums) {
        // Check if alternating signs
        for (int i = 0; i < nums.length - 1; i++) {
            if ((nums[i] > 0 && nums[i + 1] > 0) ||
                    (nums[i] < 0 && nums[i + 1] < 0)) {
                return false;
            }
        }
        // Check if starts with positive
        return nums[0] > 0;
    }

    // Test cases
    public static void main(String[] args) {
        RearrangeArrayBySign solution = new RearrangeArrayBySign();

        // Test case 1
        int[] nums1 = { 3, 1, -2, -5, 2, -4 };
        System.out.print("Example 1 - Input: ");
        printArray(nums1);
        int[] result1 = solution.rearrangeArray1(nums1);
        System.out.print("Output: ");
        printArray(result1);
        System.out.println("Valid: " + verify(result1));
        // Expected: [3, -2, 1, -5, 2, -4]

        // Test case 2
        int[] nums2 = { -1, 1 };
        System.out.print("\nExample 2 - Input: ");
        printArray(nums2);
        int[] result2 = solution.rearrangeArray1(nums2);
        System.out.print("Output: ");
        printArray(result2);
        System.out.println("Valid: " + verify(result2));
        // Expected: [1, -1]

        // Test case 3: Multiple same values
        int[] nums3 = { 1, 2, 3, -1, -2, -3 };
        System.out.print("\nSequential - Input: ");
        printArray(nums3);
        int[] result3 = solution.rearrangeArray2(nums3);
        System.out.print("Output: ");
        printArray(result3);
        System.out.println("Valid: " + verify(result3));
        // Expected: [1, -1, 2, -2, 3, -3]

        // Test case 4: Mixed order
        int[] nums4 = { -5, 10, -3, 8, -2, 6 };
        System.out.print("\nMixed order - Input: ");
        printArray(nums4);
        int[] result4 = solution.rearrangeArray3(nums4);
        System.out.print("Output: ");
        printArray(result4);
        System.out.println("Valid: " + verify(result4));
        // Expected: [10, -5, 8, -3, 6, -2]

        // Test case 5: Large values
        int[] nums5 = { 100, -100, 50, -50, 25, -25 };
        System.out.print("\nLarge values - Input: ");
        printArray(nums5);
        int[] result5 = solution.rearrangeArray4(nums5);
        System.out.print("Output: ");
        printArray(result5);
        System.out.println("Valid: " + verify(result5));
        // Expected: [100, -100, 50, -50, 25, -25]

        // Compare all solutions
        System.out.println("\n--- Comparing all solutions ---");
        int[] test = { 5, -3, 2, -1, 7, -4 };
        System.out.print("Input: ");
        printArray(test);
        System.out.print("Solution 1: ");
        printArray(solution.rearrangeArray1(test.clone()));
        System.out.print("Solution 2: ");
        printArray(solution.rearrangeArray2(test.clone()));
        System.out.print("Solution 3: ");
        printArray(solution.rearrangeArray3(test.clone()));
        System.out.print("Solution 4: ");
        printArray(solution.rearrangeArray4(test.clone()));
    }
}

/*
 * You are given a 0-indexed integer array nums of even length consisting of an
 * equal number of positive and negative integers.
 * 
 * You should return the array of nums such that the array follows the given
 * conditions:
 * 
 * Every consecutive pair of integers have opposite signs.
 * For all integers with the same sign, the order in which they were present in
 * nums is preserved.
 * The rearranged array begins with a positive integer.
 * Return the modified array after rearranging the elements to satisfy the
 * aforementioned conditions.
 * 
 * 
 * 
 * Example 1:
 * 
 * Input: nums = [3,1,-2,-5,2,-4]
 * Output: [3,-2,1,-5,2,-4]
 * Explanation:
 * The positive integers in nums are [3,1,2]. The negative integers are
 * [-2,-5,-4].
 * The only possible way to rearrange them such that they satisfy all conditions
 * is [3,-2,1,-5,2,-4].
 * Other ways such as [1,-2,2,-5,3,-4], [3,1,2,-2,-5,-4], [-2,3,-5,1,-4,2] are
 * incorrect because they do not satisfy one or more conditions.
 * Example 2:
 * 
 * Input: nums = [-1,1]
 * Output: [1,-1]
 * Explanation:
 * 1 is the only positive integer and -1 the only negative integer in nums.
 * So nums is rearranged to [1,-1].
 */
