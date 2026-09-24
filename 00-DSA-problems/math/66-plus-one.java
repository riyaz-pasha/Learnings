import java.util.Arrays;

/*
 * You are given a large integer represented as an integer array digits, where
 * each digits[i] is the ith digit of the integer. The digits are ordered from
 * most significant to least significant in left-to-right order. The large
 * integer does not contain any leading 0's.
 * 
 * Increment the large integer by one and return the resulting array of digits.
 * 
 * Example 1:
 * Input: digits = [1,2,3]
 * Output: [1,2,4]
 * Explanation: The array represents the integer 123.
 * Incrementing by one gives 123 + 1 = 124.
 * Thus, the result should be [1,2,4].
 * 
 * Example 2:
 * Input: digits = [4,3,2,1]
 * Output: [4,3,2,2]
 * Explanation: The array represents the integer 4321.
 * Incrementing by one gives 4321 + 1 = 4322.
 * Thus, the result should be [4,3,2,2].
 * 
 * Example 3:
 * Input: digits = [9]
 * Output: [1,0]
 * Explanation: The array represents the integer 9.
 * Incrementing by one gives 9 + 1 = 10.
 * Thus, the result should be [1,0].
 */

class PlusOne {

    // Solution 1: Standard Approach with Carry Logic
    // Have O(n) time complexity in worst case, O(1) in best case
    // Use O(1) space in best case, O(n) only when overflow occurs
    public int[] plusOne1(int[] digits) {
        int n = digits.length;

        // Start from the least significant digit (rightmost)
        for (int i = n - 1; i >= 0; i--) {
            // If current digit is less than 9, just increment and return
            if (digits[i] < 9) {
                digits[i]++;
                return digits;
            }
            // If current digit is 9, it becomes 0 and we carry over
            digits[i] = 0;
        }

        // If we reach here, all digits were 9 (e.g., [9,9,9] -> [1,0,0,0])
        int[] result = new int[n + 1];
        result[0] = 1;
        // Rest of the array is already initialized to 0
        return result;
    }

    // Solution 2: Optimized Single Pass with Early Return
    public int[] plusOne2(int[] digits) {
        for (int i = digits.length - 1; i >= 0; i--) {
            if (digits[i] != 9) {
                digits[i]++;
                return digits;
            }
            digits[i] = 0;
        }

        // All digits were 9
        int[] result = new int[digits.length + 1];
        result[0] = 1;
        return result;
    }

    // Solution 3: Recursive Approach
    public int[] plusOne3(int[] digits) {
        if (addOne(digits, digits.length - 1)) {
            // Overflow occurred, need to create new array
            int[] result = new int[digits.length + 1];
            result[0] = 1;
            System.arraycopy(digits, 0, result, 1, digits.length);
            return result;
        }
        return digits;
    }

    private boolean addOne(int[] digits, int index) {
        if (index < 0)
            return true; // Overflow

        if (digits[index] < 9) {
            digits[index]++;
            return false; // No overflow
        }

        digits[index] = 0;
        return addOne(digits, index - 1);
    }

    // Solution 4: Using ArrayList (Dynamic approach)
    public int[] plusOne4(int[] digits) {
        java.util.List<Integer> result = new java.util.ArrayList<>();

        // Copy digits to list
        for (int digit : digits) {
            result.add(digit);
        }

        int carry = 1;
        for (int i = result.size() - 1; i >= 0 && carry > 0; i--) {
            int sum = result.get(i) + carry;
            result.set(i, sum % 10);
            carry = sum / 10;
        }

        if (carry > 0) {
            result.add(0, carry);
        }

        // Convert back to array
        return result.stream().mapToInt(i -> i).toArray();
    }

    // Solution 5: Bit Manipulation Approach (Creative but not practical for this
    // problem)
    public int[] plusOne5(int[] digits) {
        // Convert to number, add 1, convert back
        // Note: This approach has limitations with very large numbers
        // and is mainly for educational purposes

        long num = 0;
        for (int digit : digits) {
            num = num * 10 + digit;
        }
        num++;

        String numStr = String.valueOf(num);
        int[] result = new int[numStr.length()];
        for (int i = 0; i < numStr.length(); i++) {
            result[i] = numStr.charAt(i) - '0';
        }

        return result;
    }

    // Helper method to print arrays nicely
    private static void printArray(int[] arr) {
        System.out.println(Arrays.toString(arr));
    }

    // Test method with comprehensive test cases
    public static void main(String[] args) {
        PlusOne solution = new PlusOne();

        // Test cases
        int[][] testCases = {
                { 1, 2, 3 }, // Normal case
                { 4, 3, 2, 1 }, // Normal case
                { 9 }, // Single digit 9
                { 9, 9, 9 }, // All 9s
                { 1, 9, 9 }, // Partial 9s
                { 0 }, // Single digit 0
                { 1, 0, 0, 0 }, // Number with trailing zeros
                { 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 } // Large number
        };

        System.out.println("Testing Plus One Solutions:");
        System.out.println("===========================");

        for (int i = 0; i < testCases.length; i++) {
            int[] original = testCases[i].clone();
            System.out.println("\nTest Case " + (i + 1) + ": " + Arrays.toString(original));

            // Test Solution 1
            int[] result1 = solution.plusOne1(testCases[i].clone());
            System.out.println("Solution 1: " + Arrays.toString(result1));

            // Test Solution 2
            int[] result2 = solution.plusOne2(testCases[i].clone());
            System.out.println("Solution 2: " + Arrays.toString(result2));

            // Test Solution 3
            int[] result3 = solution.plusOne3(testCases[i].clone());
            System.out.println("Solution 3: " + Arrays.toString(result3));

            // Test Solution 4
            int[] result4 = solution.plusOne4(testCases[i].clone());
            System.out.println("Solution 4: " + Arrays.toString(result4));

            // Verify all solutions give same result
            boolean allSame = Arrays.equals(result1, result2) &&
                    Arrays.equals(result2, result3) &&
                    Arrays.equals(result3, result4);
            System.out.println("All solutions match: " + allSame);
        }

        // Performance demonstration
        System.out.println("\n" + "=".repeat(50));
        System.out.println("Performance Characteristics:");
        System.out.println("Solution 1 & 2: O(n) time, O(1) space (best case), O(n) space (worst case)");
        System.out.println("Solution 3: O(n) time, O(n) space (recursion stack)");
        System.out.println("Solution 4: O(n) time, O(n) space (ArrayList)");
        System.out.println("Solution 5: Limited by integer overflow for large inputs");
    }

}

/*
 * Detailed Analysis:
 * 
 * Key Insights:
 * 1. We only need to handle carry propagation from right to left
 * 2. If any digit < 9, we can increment and return immediately
 * 3. Only when all remaining digits are 9 do we need a new array
 * 4. The worst case (all 9s) requires creating a new array with size n+1
 * 
 * Algorithm Steps:
 * 1. Start from the rightmost digit
 * 2. If digit < 9: increment and return
 * 3. If digit = 9: set to 0 and continue left (carry over)
 * 4. If all digits were 9: create new array [1,0,0,...,0]
 * 
 * Edge Cases Handled:
 * - Single digit 9 -> [1,0]
 * - All digits are 9 -> [1,0,0,...,0]
 * - Mixed digits with trailing 9s
 * - Numbers with leading/trailing zeros
 * 
 * Time Complexity: O(n) where n is the number of digits
 * Space Complexity:
 * - Best case: O(1) when no overflow
 * - Worst case: O(n) when creating new array
 * 
 * Recommended Solution: Solution 1 or 2 (both are optimal)
 * - Clean, readable code
 * - Optimal time and space complexity
 * - Handles all edge cases correctly
 * - Most commonly expected in interviews
 */
