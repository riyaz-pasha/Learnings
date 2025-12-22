
/*
 * Given an input string s, reverse the order of the words.
 * 
 * A word is defined as a sequence of non-space characters. The words in s will
 * be separated by at least one space.
 * 
 * Return a string of the words in reverse order concatenated by a single space.
 * 
 * Note that s may contain leading or trailing spaces or multiple spaces between
 * two words. The returned string should only have a single space separating the
 * words. Do not include any extra spaces.
 * 
 * Example 1:
 * Input: s = "the sky is blue"
 * Output: "blue is sky the"
 * 
 * Example 2:
 * Input: s = "  hello world  "
 * Output: "world hello"
 * Explanation: Your reversed string should not contain leading or trailing
 * spaces.
 * 
 * Example 3:
 * Input: s = "a good   example"
 * Output: "example good a"
 * Explanation: You need to reduce multiple spaces between two words to a single
 * space in the reversed string.
 */

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Stack;

class ReverseWords {

    /**
     * Solution 1: Using built-in split() and StringBuilder
     * Time: O(n), Space: O(n)
     * Most readable and concise approach
     */
    public String reverseWords1(String s) {
        // Split by one or more spaces and filter out empty strings
        String[] words = s.trim().split("\\s+");
        StringBuilder result = new StringBuilder();

        // Build result from right to left
        for (int i = words.length - 1; i >= 0; i--) {
            result.append(words[i]);
            if (i > 0) {
                result.append(" ");
            }
        }

        return result.toString();
    }

    /**
     * Solution 2: Two-pointer approach without split()
     * Time: O(n), Space: O(n) for result string
     * More control over the process, good for understanding
     */
    public String reverseWords2(String s) {
        StringBuilder result = new StringBuilder();
        int n = s.length();
        int i = n - 1;

        while (i >= 0) {
            // Skip trailing spaces
            while (i >= 0 && s.charAt(i) == ' ') {
                i--;
            }

            if (i < 0)
                break; // No more words

            // Find the start of current word
            int j = i;
            while (i >= 0 && s.charAt(i) != ' ') {
                i--;
            }

            // Add space if not the first word
            if (result.length() > 0) {
                result.append(' ');
            }

            // Add the word (from i+1 to j inclusive)
            result.append(s.substring(i + 1, j + 1));
        }

        return result.toString();
    }

    public String reverseWords22(String s) {
        StringBuilder result = new StringBuilder();
        int i = s.length() - 1;

        while (i >= 0) {
            // Skip spaces
            while (i >= 0 && s.charAt(i) == ' ') {
                i--;
            }

            if (i < 0)
                break;

            // End of word
            int end = i;

            // Move to start of word
            while (i >= 0 && s.charAt(i) != ' ') {
                i--;
            }

            // Add space between words
            if (result.length() > 0) {
                result.append(' ');
            }

            // Append characters directly
            for (int k = i + 1; k <= end; k++) {
                result.append(s.charAt(k));
            }
        }

        return result.toString();
    }

    /**
     * Solution 3: Using Collections.reverse()
     * Time: O(n), Space: O(n)
     * Clean approach using Java collections
     */
    public String reverseWords3(String s) {
        List<String> words = Arrays.asList(s.trim().split("\\s+"));
        Collections.reverse(words);
        return String.join(" ", words);
    }

    /**
     * Solution 4: Deque-based approach
     * Time: O(n), Space: O(n)
     * Using deque to naturally reverse the order
     */
    public String reverseWords4(String s) {
        Deque<String> deque = new ArrayDeque<>();
        StringBuilder word = new StringBuilder();

        for (char c : s.toCharArray()) {
            if (c != ' ') {
                word.append(c);
            } else if (word.length() > 0) {
                deque.addFirst(word.toString());
                word.setLength(0); // Reset StringBuilder
            }
        }

        // Add the last word if exists
        if (word.length() > 0) {
            deque.addFirst(word.toString());
        }

        return String.join(" ", deque);
    }

    /**
     * Solution 5: Stack-based approach
     * Time: O(n), Space: O(n)
     * Using stack's LIFO property
     */
    public String reverseWords5(String s) {
        Stack<String> stack = new Stack<>();
        StringBuilder word = new StringBuilder();

        for (char c : s.toCharArray()) {
            if (c != ' ') {
                word.append(c);
            } else if (word.length() > 0) {
                stack.push(word.toString());
                word.setLength(0);
            }
        }

        // Push the last word
        if (word.length() > 0) {
            stack.push(word.toString());
        }

        StringBuilder result = new StringBuilder();
        while (!stack.isEmpty()) {
            result.append(stack.pop());
            if (!stack.isEmpty()) {
                result.append(" ");
            }
        }

        return result.toString();
    }

    // Test method to verify all solutions
    public static void main(String[] args) {
        ReverseWords solution = new ReverseWords();

        // Test cases
        String[] testCases = {
                "the sky is blue",
                "  hello world  ",
                "a good   example",
                "  Bob    Loves  Alice   ",
                "Alice",
                " "
        };

        String[] expected = {
                "blue is sky the",
                "world hello",
                "example good a",
                "Alice Loves Bob",
                "Alice",
                ""
        };

        for (int i = 0; i < testCases.length; i++) {
            String input = testCases[i];
            String exp = expected[i];

            System.out.println("Input: \"" + input + "\"");
            System.out.println("Expected: \"" + exp + "\"");

            // Test all solutions
            String result1 = solution.reverseWords1(input);
            String result2 = solution.reverseWords2(input);
            String result3 = solution.reverseWords3(input);
            String result4 = solution.reverseWords4(input);
            String result5 = solution.reverseWords5(input);

            System.out.println("Solution 1: \"" + result1 + "\" " + (result1.equals(exp) ? "✓" : "✗"));
            System.out.println("Solution 2: \"" + result2 + "\" " + (result2.equals(exp) ? "✓" : "✗"));
            System.out.println("Solution 3: \"" + result3 + "\" " + (result3.equals(exp) ? "✓" : "✗"));
            System.out.println("Solution 4: \"" + result4 + "\" " + (result4.equals(exp) ? "✓" : "✗"));
            System.out.println("Solution 5: \"" + result5 + "\" " + (result5.equals(exp) ? "✓" : "✗"));
            System.out.println("---");
        }
    }

}
