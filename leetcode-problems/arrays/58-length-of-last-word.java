/*
 * Given a string s consisting of words and spaces, return the length of the
 * last word in the string.
 * 
 * A word is a maximal substring consisting of non-space characters only.
 * 
 * Example 1:
 * Input: s = "Hello World"
 * Output: 5
 * Explanation: The last word is "World" with length 5.
 * 
 * Example 2:
 * Input: s = "   fly me   to   the moon  "
 * Output: 4
 * Explanation: The last word is "moon" with length 4.
 * 
 * Example 3:
 * Input: s = "luffy is still joyboy"
 * Output: 6
 * Explanation: The last word is "joyboy" with length 6.l
 */

class LengthOfLastWord {

    /**
     * Solution 1: Built-in String Methods (Simple)
     * Time Complexity: O(n)
     * Space Complexity: O(n) - due to split() creating array
     * 
     * Uses trim() and split() to handle spaces and get words.
     */
    public int lengthOfLastWordV1(String s) {
        // Remove leading and trailing spaces, then split by spaces
        String[] words = s.trim().split("\\s+");

        // Return length of last word
        return words[words.length - 1].length();
    }

    /**
     * Solution 2: Built-in with LastIndexOf (Efficient)
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Uses trim() to remove trailing spaces, then finds last space.
     */
    public int lengthOfLastWordV2(String s) {
        // Remove trailing spaces
        s = s.trim();

        // Find the last space
        int lastSpaceIndex = s.lastIndexOf(' ');

        // Length of last word is total length minus position after last space
        return s.length() - lastSpaceIndex - 1;
    }

    /**
     * Solution 3: Reverse Iteration (Optimal)
     * Time Complexity: O(n) worst case, O(k) average where k is length of last word
     * Space Complexity: O(1)
     * 
     * Iterates from the end, skips trailing spaces, then counts word characters.
     * Most efficient as it stops as soon as the last word is processed.
     */
    public int lengthOfLastWordV3(String s) {
        int length = 0;
        int i = s.length() - 1;

        // Skip trailing spaces
        while (i >= 0 && s.charAt(i) == ' ') {
            i--;
        }

        // Count characters of the last word
        while (i >= 0 && s.charAt(i) != ' ') {
            length++;
            i--;
        }

        return length;
    }

    /**
     * Solution 4: Single Pass Forward (Alternative)
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Single forward pass, keeps track of current word length.
     * Updates last word length whenever a new word starts.
     */
    public int lengthOfLastWordV4(String s) {
        int lastWordLength = 0;
        int currentWordLength = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c != ' ') {
                // We're in a word
                currentWordLength++;
            } else {
                // We hit a space
                if (currentWordLength > 0) {
                    // End of a word, update last word length
                    lastWordLength = currentWordLength;
                    currentWordLength = 0;
                }
            }
        }

        // If string doesn't end with space, current word is the last word
        return currentWordLength > 0 ? currentWordLength : lastWordLength;
    }

    /**
     * Solution 5: Regular Expression (Functional Style)
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     * 
     * Uses regex to find all words, then gets the last one.
     */
    public int lengthOfLastWordV5(String s) {
        // Find all sequences of non-space characters
        String[] words = s.split("\\s+");

        // Filter out empty strings and get the last non-empty word
        for (int i = words.length - 1; i >= 0; i--) {
            if (!words[i].isEmpty()) {
                return words[i].length();
            }
        }

        return 0; // No words found
    }

    /**
     * Solution 6: Two Pointers (Educational)
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Uses two pointers to find the boundaries of the last word.
     */
    public int lengthOfLastWordV6(String s) {
        int right = s.length() - 1;
        int left = s.length() - 1;

        // Move right pointer to skip trailing spaces
        while (right >= 0 && s.charAt(right) == ' ') {
            right--;
        }

        if (right < 0)
            return 0; // All spaces

        // Move left pointer to find start of last word
        left = right;
        while (left >= 0 && s.charAt(left) != ' ') {
            left--;
        }

        // Length is right - left
        return right - left;
    }

    /**
     * Helper method to validate solutions with custom test cases
     */
    public void testWithString(String s, int expected) {
        System.out.println("\nTesting: \"" + s + "\" (Expected: " + expected + ")");

        int result1 = lengthOfLastWordV1(s);
        int result2 = lengthOfLastWordV2(s);
        int result3 = lengthOfLastWordV3(s);
        int result4 = lengthOfLastWordV4(s);
        int result5 = lengthOfLastWordV5(s);
        int result6 = lengthOfLastWordV6(s);

        System.out.println("V1 (Split): " + result1 + (result1 == expected ? " ✓" : " ✗"));
        System.out.println("V2 (LastIndexOf): " + result2 + (result2 == expected ? " ✓" : " ✗"));
        System.out.println("V3 (Reverse Iter): " + result3 + (result3 == expected ? " ✓" : " ✗"));
        System.out.println("V4 (Forward Pass): " + result4 + (result4 == expected ? " ✓" : " ✗"));
        System.out.println("V5 (Regex): " + result5 + (result5 == expected ? " ✓" : " ✗"));
        System.out.println("V6 (Two Pointers): " + result6 + (result6 == expected ? " ✓" : " ✗"));
    }

    /**
     * Performance testing method
     */
    public void performanceTest() {
        // Create test strings of different sizes
        String shortString = "Hello World";
        String mediumString = "The quick brown fox jumps over the lazy dog";
        String longString = "This is a very long string with many words to test the performance of different algorithms for finding the length of the last word";
        String trailingSpaces = "Hello World    ";

        int iterations = 1000000;

        System.out.println("\n=== Performance Test (" + iterations + " iterations) ===");

        // Test V2 (LastIndexOf)
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            lengthOfLastWordV2(longString);
        }
        long v2Time = System.nanoTime() - startTime;

        // Test V3 (Reverse Iteration)
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            lengthOfLastWordV3(longString);
        }
        long v3Time = System.nanoTime() - startTime;

        // Test V4 (Forward Pass)
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            lengthOfLastWordV4(longString);
        }
        long v4Time = System.nanoTime() - startTime;

        System.out.println("V2 (LastIndexOf): " + v2Time / 1000000.0 + " ms");
        System.out.println("V3 (Reverse Iter): " + v3Time / 1000000.0 + " ms");
        System.out.println("V4 (Forward Pass): " + v4Time / 1000000.0 + " ms");

        System.out.println("\nRelative Performance:");
        System.out.println("V3 vs V2: " + String.format("%.2fx", (double) v2Time / v3Time));
        System.out.println("V3 vs V4: " + String.format("%.2fx", (double) v4Time / v3Time));
    }

    // Test all solutions
    public static void main(String[] args) {
        LengthOfLastWord solution = new LengthOfLastWord();

        System.out.println("=== Testing All Solutions ===");

        // Test case 1: Normal case
        solution.testWithString("Hello World", 5);

        // Test case 2: Trailing spaces
        solution.testWithString("   fly me   to   the moon  ", 4);

        // Test case 3: Single word
        solution.testWithString("luffy", 5);

        // Test case 4: Single word with spaces
        solution.testWithString("  hello  ", 5);

        // Test case 5: Multiple spaces between words
        solution.testWithString("a", 1);

        // Test case 6: Empty-like string
        solution.testWithString("   ", 0);

        // Test case 7: Two words
        solution.testWithString("ab cd", 2);

        // Test case 8: Long sentence
        solution.testWithString("The quick brown fox jumps over the lazy dog", 3);

        // Edge cases
        System.out.println("\n=== Edge Cases ===");

        // Single character
        System.out.println("Single char 'a': " + solution.lengthOfLastWordV3("a"));

        // Only spaces
        System.out.println("Only spaces '   ': " + solution.lengthOfLastWordV3("   "));

        // Leading spaces only
        System.out.println("Leading spaces '   word': " + solution.lengthOfLastWordV3("   word"));

        // Multiple consecutive spaces
        System.out.println("Multiple spaces 'word1    word2': " +
                solution.lengthOfLastWordV3("word1    word2"));

        // Performance comparison
        solution.performanceTest();

        // Demonstrate efficiency of reverse iteration
        System.out.println("\n=== Efficiency Demonstration ===");
        String longStringWithShortLastWord = "This is a very long string with many words but the last word is short end";
        String shortStringWithLongLastWord = "Short supercalifragilisticexpialidocious";

        System.out.println("Long string, short last word: " +
                solution.lengthOfLastWordV3(longStringWithShortLastWord));
        System.out.println("Short string, long last word: " +
                solution.lengthOfLastWordV3(shortStringWithLongLastWord));

        System.out.println("\nReverse iteration is most efficient because:");
        System.out.println("- It starts from the end where the last word is");
        System.out.println("- It stops as soon as it processes the last word");
        System.out.println("- It doesn't need to process the entire string");
        System.out.println("- It uses O(1) space");
    }

}
