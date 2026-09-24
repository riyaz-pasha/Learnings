/*
 * A phrase is a palindrome if, after converting all uppercase letters into
 * lowercase letters and removing all non-alphanumeric characters, it reads the
 * same forward and backward. Alphanumeric characters include letters and
 * numbers.
 * 
 * Given a string s, return true if it is a palindrome, or false otherwise.
 * 
 * Example 1:
 * Input: s = "A man, a plan, a canal: Panama"
 * Output: true
 * Explanation: "amanaplanacanalpanama" is a palindrome.
 * 
 * Example 2:
 * Input: s = "race a car"
 * Output: false
 * Explanation: "raceacar" is not a palindrome.
 * 
 * Example 3:
 * Input: s = " "
 * Output: true
 * Explanation: s is an empty string "" after removing non-alphanumeric
 * characters.
 * Since an empty string reads the same forward and backward, it is a
 * palindrome.
 */

class PalindromeSolutions {

    // Solution 1: Clean string first, then check
    // Time: O(n), Space: O(n)
    public boolean isPalindrome1(String s) {
        // Clean the string: lowercase + remove non-alphanumeric
        StringBuilder cleaned = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                cleaned.append(Character.toLowerCase(c));
            }
        }

        // Check if cleaned string is palindrome
        String str = cleaned.toString();
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

    // Solution 2: Two pointers without extra space
    // Time: O(n), Space: O(1)
    public boolean isPalindrome2(String s) {
        int left = 0, right = s.length() - 1;

        while (left < right) {
            // Skip non-alphanumeric characters from left
            while (left < right && !Character.isLetterOrDigit(s.charAt(left))) {
                left++;
            }

            // Skip non-alphanumeric characters from right
            while (left < right && !Character.isLetterOrDigit(s.charAt(right))) {
                right--;
            }

            // Compare characters (case-insensitive)
            if (Character.toLowerCase(s.charAt(left)) != Character.toLowerCase(s.charAt(right))) {
                return false;
            }

            left++;
            right--;
        }
        return true;
    }

    // Solution 3: Using regex and StringBuilder reverse
    // Time: O(n), Space: O(n)
    public boolean isPalindrome3(String s) {
        // Remove non-alphanumeric and convert to lowercase
        String cleaned = s.replaceAll("[^A-Za-z0-9]", "").toLowerCase();

        // Compare with reversed string
        return cleaned.equals(new StringBuilder(cleaned).reverse().toString());
    }

    // Solution 4: Recursive approach
    // Time: O(n), Space: O(n) due to recursion stack
    public boolean isPalindrome4(String s) {
        // Clean the string first
        StringBuilder cleaned = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                cleaned.append(Character.toLowerCase(c));
            }
        }

        return isPalindromeRecursive(cleaned.toString(), 0, cleaned.length() - 1);
    }

    private boolean isPalindromeRecursive(String s, int left, int right) {
        if (left >= right)
            return true;

        if (s.charAt(left) != s.charAt(right)) {
            return false;
        }

        return isPalindromeRecursive(s, left + 1, right - 1);
    }

    // Test method
    public static void main(String[] args) {
        PalindromeSolutions solution = new PalindromeSolutions();

        // Test cases
        String[] testCases = {
                "A man, a plan, a canal: Panama",
                "race a car",
                " ",
                "Madam",
                "No 'x' in Nixon",
                "Mr. Owl ate my metal worm",
                ""
        };

        System.out.println("Testing all solutions:\n");

        for (String test : testCases) {
            System.out.println("Input: \"" + test + "\"");
            System.out.println("Solution 1: " + solution.isPalindrome1(test));
            System.out.println("Solution 2: " + solution.isPalindrome2(test));
            System.out.println("Solution 3: " + solution.isPalindrome3(test));
            System.out.println("Solution 4: " + solution.isPalindrome4(test));
            System.out.println();
        }
    }

}

/*
 * ANALYSIS:
 * 
 * Solution 1 - Clean First Approach:
 * - Pros: Easy to understand, clean separation of concerns
 * - Cons: Uses extra space for cleaned string
 * - Best for: When readability is prioritized
 * 
 * Solution 2 - Two Pointers (RECOMMENDED):
 * - Pros: Optimal space complexity O(1), efficient
 * - Cons: Slightly more complex logic
 * - Best for: Production code, technical interviews
 * 
 * Solution 3 - Regex Approach:
 * - Pros: Very concise, leverages built-in methods
 * - Cons: Regex can be slower, uses extra space
 * - Best for: Quick prototyping
 * 
 * Solution 4 - Recursive:
 * - Pros: Elegant, demonstrates recursion
 * - Cons: Uses O(n) stack space, potential stack overflow
 * - Best for: Academic purposes, demonstrating recursion
 * 
 * Time Complexity: All solutions are O(n)
 * Space Complexity:
 * - Solutions 1, 3, 4: O(n)
 * - Solution 2: O(1)
 * 
 * For most practical purposes, Solution 2 is recommended due to its
 * optimal space complexity and good performance.
 */