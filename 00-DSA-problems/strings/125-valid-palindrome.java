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

class ValidPalindrome {

    /**
     * Solution 1: Two-pointer approach (Most Optimal)
     * Time: O(n), Space: O(1)
     * Best solution - processes characters in place without extra space
     */
    public boolean isPalindrome1(String s) {
        int left = 0;
        int right = s.length() - 1;

        while (left < right) {
            // Skip non-alphanumeric characters from left
            while (left < right && !Character.isLetterOrDigit(s.charAt(left))) {
                left++;
            }

            // Skip non-alphanumeric characters from right
            while (left < right && !Character.isLetterOrDigit(s.charAt(right))) {
                right--;
            }

            // Compare characters (convert to lowercase)
            if (Character.toLowerCase(s.charAt(left)) != Character.toLowerCase(s.charAt(right))) {
                return false;
            }

            left++;
            right--;
        }

        return true;
    }

    /**
     * Solution 2: Clean string first, then check
     * Time: O(n), Space: O(n)
     * More readable but uses extra space
     */
    public boolean isPalindrome2(String s) {
        // Clean the string: keep only alphanumeric and convert to lowercase
        StringBuilder cleaned = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                cleaned.append(Character.toLowerCase(c));
            }
        }

        String cleanStr = cleaned.toString();
        int left = 0;
        int right = cleanStr.length() - 1;

        while (left < right) {
            if (cleanStr.charAt(left) != cleanStr.charAt(right)) {
                return false;
            }
            left++;
            right--;
        }

        return true;
    }

    /**
     * Solution 3: Using StringBuilder reverse
     * Time: O(n), Space: O(n)
     * Simple approach using built-in reverse
     */
    public boolean isPalindrome3(String s) {
        StringBuilder cleaned = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                cleaned.append(Character.toLowerCase(c));
            }
        }

        String original = cleaned.toString();
        String reversed = cleaned.reverse().toString();

        return original.equals(reversed);
    }

    /**
     * Solution 4: Using regex and StringBuilder
     * Time: O(n), Space: O(n)
     * Concise but potentially slower due to regex
     */
    public boolean isPalindrome4(String s) {
        // Remove non-alphanumeric and convert to lowercase
        String cleaned = s.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        int left = 0;
        int right = cleaned.length() - 1;

        while (left < right) {
            if (cleaned.charAt(left) != cleaned.charAt(right)) {
                return false;
            }
            left++;
            right--;
        }

        return true;
    }

    /**
     * Solution 5: Recursive approach
     * Time: O(n), Space: O(n) due to recursion stack
     * Academic interest - not recommended for production
     */
    public boolean isPalindrome5(String s) {
        return isPalindromeRecursive(s, 0, s.length() - 1);
    }

    private boolean isPalindromeRecursive(String s, int left, int right) {
        // Base case
        if (left >= right) {
            return true;
        }

        // Skip non-alphanumeric from left
        if (!Character.isLetterOrDigit(s.charAt(left))) {
            return isPalindromeRecursive(s, left + 1, right);
        }

        // Skip non-alphanumeric from right
        if (!Character.isLetterOrDigit(s.charAt(right))) {
            return isPalindromeRecursive(s, left, right - 1);
        }

        // Compare current characters
        if (Character.toLowerCase(s.charAt(left)) != Character.toLowerCase(s.charAt(right))) {
            return false;
        }

        // Recurse with inner characters
        return isPalindromeRecursive(s, left + 1, right - 1);
    }

    /**
     * Solution 6: Stream API approach (Java 8+)
     * Time: O(n), Space: O(n)
     * Modern functional programming style
     */
    public boolean isPalindrome6(String s) {
        String cleaned = s.chars()
                .filter(Character::isLetterOrDigit)
                .mapToObj(c -> Character.toLowerCase((char) c))
                .map(String::valueOf)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();

        int left = 0;
        int right = cleaned.length() - 1;

        while (left < right) {
            if (cleaned.charAt(left) != cleaned.charAt(right)) {
                return false;
            }
            left++;
            right--;
        }

        return true;
    }

    /**
     * Helper method for manual character validation (alternative to
     * Character.isLetterOrDigit)
     */
    private boolean isAlphaNumeric(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9');
    }

    /**
     * Helper method for manual lowercase conversion (alternative to
     * Character.toLowerCase)
     */
    private char toLowerCase(char c) {
        if (c >= 'A' && c <= 'Z') {
            return (char) (c + 32);
        }
        return c;
    }

    // Test method to verify all solutions
    public static void main(String[] args) {
        ValidPalindrome solution = new ValidPalindrome();

        // Test cases
        String[] testCases = {
                "A man, a plan, a canal: Panama",
                "race a car",
                " ",
                "Madam",
                "No 'x' in Nixon",
                "Mr. Owl ate my metal worm",
                "Was it a car or a cat I saw?",
                "abc",
                "",
                "a"
        };

        boolean[] expected = {
                true, // "amanaplanacanalpanama"
                false, // "raceacar"
                true, // "" (empty after cleaning)
                true, // "madam"
                true, // "noxinnixon"
                true, // "mrowlatemymetalworm"
                true, // "wasitacaroracatisaw"
                false, // "abc"
                true, // ""
                true // "a"
        };

        for (int i = 0; i < testCases.length; i++) {
            String input = testCases[i];
            boolean exp = expected[i];

            System.out.println("Input: \"" + input + "\"");
            System.out.println("Expected: " + exp);

            // Test all solutions
            boolean result1 = solution.isPalindrome1(input);
            boolean result2 = solution.isPalindrome2(input);
            boolean result3 = solution.isPalindrome3(input);
            boolean result4 = solution.isPalindrome4(input);
            boolean result5 = solution.isPalindrome5(input);
            boolean result6 = solution.isPalindrome6(input);

            System.out.println("Solution 1 (Two-pointer): " + result1 + " " + (result1 == exp ? "✓" : "✗"));
            System.out.println("Solution 2 (Clean first): " + result2 + " " + (result2 == exp ? "✓" : "✗"));
            System.out.println("Solution 3 (StringBuilder): " + result3 + " " + (result3 == exp ? "✓" : "✗"));
            System.out.println("Solution 4 (Regex): " + result4 + " " + (result4 == exp ? "✓" : "✗"));
            System.out.println("Solution 5 (Recursive): " + result5 + " " + (result5 == exp ? "✓" : "✗"));
            System.out.println("Solution 6 (Stream API): " + result6 + " " + (result6 == exp ? "✓" : "✗"));
            System.out.println("---");
        }

        // Performance comparison for large input
        String largeInput = "A man, a plan, a canal: Panama".repeat(1000);

        long start = System.nanoTime();
        solution.isPalindrome1(largeInput);
        long time1 = System.nanoTime() - start;

        start = System.nanoTime();
        solution.isPalindrome2(largeInput);
        long time2 = System.nanoTime() - start;

        System.out.println("Performance comparison (large input):");
        System.out.println("Two-pointer: " + time1 + " ns");
        System.out.println("Clean first: " + time2 + " ns");
        System.out.println("Two-pointer is " + String.format("%.2f", (double) time2 / time1) + "x faster");
    }

}
