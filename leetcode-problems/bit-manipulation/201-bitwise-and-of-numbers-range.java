/*
 * Given two integers left and right that represent the range [left, right],
 * return the bitwise AND of all numbers in this range, inclusive.
 * 
 * Example 1:
 * Input: left = 5, right = 7
 * Output: 4
 * 
 * Example 2:
 * Input: left = 0, right = 0
 * Output: 0
 * 
 * Example 3:
 * Input: left = 1, right = 2147483647
 * Output: 0
 */

class RangeBitwiseAND {

    // Solution 1: Bit Shift Approach (Most Efficient)
    // Time: O(log n), Space: O(1)
    public int rangeBitwiseAnd1(int left, int right) {
        int shift = 0;
        // Find the common prefix by right shifting until left == right
        while (left != right) {
            left >>= 1;
            right >>= 1;
            shift++;
        }
        // Left shift back to get the result
        return left << shift;
    }

    // Solution 2: Clear Rightmost Set Bit
    // Time: O(log n), Space: O(1)
    public int rangeBitwiseAnd2(int left, int right) {
        // Keep clearing the rightmost set bit of right until right <= left
        while (left < right) {
            right = right & (right - 1);
        }
        return right;
    }

    // Solution 3: Mask Approach
    // Time: O(log n), Space: O(1)
    public int rangeBitwiseAnd3(int left, int right) {
        int mask = Integer.MAX_VALUE;
        // Find differing bits and create mask
        while ((left & mask) != (right & mask)) {
            mask <<= 1;
        }
        return left & mask;
    }

    // Solution 4: Recursive Approach
    // Time: O(log n), Space: O(log n) due to recursion stack
    public int rangeBitwiseAnd4(int left, int right) {
        if (left == 0 || left < right) {
            if (left == right)
                return left;
            return rangeBitwiseAnd4(left >> 1, right >> 1) << 1;
        }
        return left;
    }

    // Helper method to demonstrate the logic with binary representation
    public void explainSolution(int left, int right) {
        System.out.println("Range: [" + left + ", " + right + "]");
        System.out.println("Left binary:  " + Integer.toBinaryString(left));
        System.out.println("Right binary: " + Integer.toBinaryString(right));

        int result = rangeBitwiseAnd1(left, right);
        System.out.println("Result:       " + Integer.toBinaryString(result));
        System.out.println("Result decimal: " + result);
        System.out.println();
    }

    public static void main(String[] args) {
        RangeBitwiseAND solution = new RangeBitwiseAND();

        // Test cases
        System.out.println("=== Test Cases ===");

        // Example 1: [5, 7]
        solution.explainSolution(5, 7);

        // Example 2: [0, 0]
        solution.explainSolution(0, 0);

        // Example 3: [1, 2147483647]
        solution.explainSolution(1, 2147483647);

        // Additional test cases
        solution.explainSolution(26, 30);
        solution.explainSolution(1, 1);

        // Verify all solutions produce same results
        System.out.println("=== Verification ===");
        int[][] testCases = { { 5, 7 }, { 0, 0 }, { 1, 2147483647 }, { 26, 30 }, { 1, 1 } };

        for (int[] test : testCases) {
            int left = test[0], right = test[1];
            int result1 = solution.rangeBitwiseAnd1(left, right);
            int result2 = solution.rangeBitwiseAnd2(left, right);
            int result3 = solution.rangeBitwiseAnd3(left, right);
            int result4 = solution.rangeBitwiseAnd4(left, right);

            System.out.printf("Range [%d, %d]: %d %d %d %d - %s%n",
                    left, right, result1, result2, result3, result4,
                    (result1 == result2 && result2 == result3 && result3 == result4) ? "✓" : "✗");
        }
    }
}