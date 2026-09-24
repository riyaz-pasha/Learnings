/*
 * Given an integer x, return true if x is a palindrome, and false otherwise.
 * 
 * Example 1:
 * Input: x = 121
 * Output: true
 * Explanation: 121 reads as 121 from left to right and from right to left.
 * 
 * Example 2:
 * Input: x = -121
 * Output: false
 * Explanation: From left to right, it reads -121. From right to left, it
 * becomes 121-. Therefore it is not a palindrome.
 * 
 * Example 3:
 * Input: x = 10
 * Output: false
 * Explanation: Reads 01 from right to left. Therefore it is not a palindrome.
 */

class PalindromeInteger {

    // Solution 1: String Conversion (Simple but uses extra space)
    public boolean isPalindrome1(int x) {
        // Negative numbers are not palindromes
        if (x < 0)
            return false;

        String str = String.valueOf(x);
        int left = 0, right = str.length() - 1;

        while (left < right) {
            if (str.charAt(left) != str.charAt(right)) {
                return false;
            }
            left++;
            right--;
        }
        return true;
    }

    // Solution 2: Full Number Reversal (Mathematical approach)
    public boolean isPalindrome2(int x) {
        // Negative numbers are not palindromes
        if (x < 0)
            return false;

        int original = x;
        int reversed = 0;

        while (x > 0) {
            reversed = reversed * 10 + x % 10;
            x /= 10;
        }

        return original == reversed;
    }

    // Solution 3: Half Number Reversal (Optimized - Most Efficient)
    public boolean isPalindrome3(int x) {
        // Negative numbers and numbers ending with 0 (except 0 itself) are not
        // palindromes
        if (x < 0 || (x % 10 == 0 && x != 0)) {
            return false;
        }

        int reversed = 0;
        while (x > reversed) {
            reversed = reversed * 10 + x % 10;
            x /= 10;
        }

        // For even length numbers: x == reversed
        // For odd length numbers: x == reversed / 10 (middle digit doesn't matter)
        return x == reversed || x == reversed / 10;
    }

    // Solution 4: Recursive Approach
    public boolean isPalindrome4(int x) {
        if (x < 0)
            return false;
        return isPalindromeHelper(x, x);
    }

    private boolean isPalindromeHelper(int x, int original) {
        if (x == 0)
            return true;
        if (!isPalindromeHelper(x / 10, original))
            return false;

        // Get the number of digits processed so far
        int digits = String.valueOf(original).length() - String.valueOf(x).length();
        int leftDigit = original / (int) Math.pow(10, String.valueOf(original).length() - 1 - digits);
        int rightDigit = x % 10;

        return leftDigit == rightDigit;
    }

    // Test method to demonstrate all solutions
    public static void main(String[] args) {
        PalindromeInteger solution = new PalindromeInteger();

        int[] testCases = { 121, -121, 10, 0, 1, 12321, 1221 };

        System.out.println("Testing all solutions:");
        System.out.println("Input\tSol1\tSol2\tSol3\tSol4");
        System.out.println("----\t----\t----\t----\t----");

        for (int x : testCases) {
            boolean result1 = solution.isPalindrome1(x);
            boolean result2 = solution.isPalindrome2(x);
            boolean result3 = solution.isPalindrome3(x);
            boolean result4 = solution.isPalindrome4(x);

            System.out.printf("%d\t%b\t%b\t%b\t%b\n", x, result1, result2, result3, result4);
        }
    }

}

/*
 * Time and Space Complexity Analysis:
 * 
 * Solution 1 (String Conversion):
 * - Time: O(log n) where n is the input number (converting to string +
 * comparison)
 * - Space: O(log n) for storing the string
 * 
 * Solution 2 (Full Reversal):
 * - Time: O(log n) for reversing all digits
 * - Space: O(1) constant space
 * 
 * Solution 3 (Half Reversal) - RECOMMENDED:
 * - Time: O(log n) but processes only half the digits
 * - Space: O(1) constant space
 * - Most efficient as it stops at the middle
 * 
 * Solution 4 (Recursive):
 * - Time: O(log n)
 * - Space: O(log n) due to recursion stack
 * 
 * Key Insights:
 * 1. Negative numbers are never palindromes
 * 2. Numbers ending with 0 (except 0 itself) are never palindromes
 * 3. Half reversal is most efficient as we only need to check half the digits
 * 4. For odd-length numbers, the middle digit doesn't affect palindrome
 * property
 */
