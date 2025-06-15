import java.util.*;
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

class ReverseWords {

    /**
     * Solution 1: Using Built-in Split and Collections.reverse()
     * Time Complexity: O(n) where n is the length of the string
     * Space Complexity: O(n) for storing the words
     * 
     * This is the most straightforward approach using Java's built-in methods.
     */
    public static String reverseWords1(String s) {
        // Split by one or more spaces and filter out empty strings
        String[] words = s.trim().split("\\s+");

        // Reverse the array
        Collections.reverse(Arrays.asList(words));

        // Join with single space
        return String.join(" ", words);
    }

    /**
     * Solution 2: Manual Split and Reverse (Two Pointers)
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     * 
     * This approach manually splits and reverses without using
     * Collections.reverse()
     */
    public static String reverseWords2(String s) {
        String[] words = s.trim().split("\\s+");

        // Reverse the array using two pointers
        int left = 0, right = words.length - 1;
        while (left < right) {
            String temp = words[left];
            words[left] = words[right];
            words[right] = temp;
            left++;
            right--;
        }

        return String.join(" ", words);
    }

    /**
     * Solution 3: Using StringBuilder and Manual Parsing
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     * 
     * This approach manually parses the string without using split()
     */
    public static String reverseWords3(String s) {
        // Remove leading and trailing spaces
        s = s.trim();

        // Use a list to store words
        List<String> words = new ArrayList<>();
        StringBuilder word = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ') {
                word.append(c);
            } else if (word.length() > 0) {
                // Found a complete word
                words.add(word.toString());
                word.setLength(0); // Clear the StringBuilder
            }
        }

        // Add the last word if any
        if (word.length() > 0) {
            words.add(word.toString());
        }

        // Build result in reverse order
        StringBuilder result = new StringBuilder();
        for (int i = words.size() - 1; i >= 0; i--) {
            result.append(words.get(i));
            if (i > 0) {
                result.append(" ");
            }
        }

        return result.toString();
    }

    /**
     * Solution 4: Using Stack (LIFO nature for reversal)
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     * 
     * This approach uses a stack to naturally reverse the order of words
     */
    public static String reverseWords4(String s) {
        Stack<String> stack = new Stack<>();
        StringBuilder word = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ') {
                word.append(c);
            } else if (word.length() > 0) {
                stack.push(word.toString());
                word.setLength(0);
            }
        }

        // Push the last word if any
        if (word.length() > 0) {
            stack.push(word.toString());
        }

        // Pop from stack to get reversed order
        StringBuilder result = new StringBuilder();
        while (!stack.isEmpty()) {
            result.append(stack.pop());
            if (!stack.isEmpty()) {
                result.append(" ");
            }
        }

        return result.toString();
    }

    /**
     * Solution 5: In-place Character Reversal (Most Space Efficient)
     * Time Complexity: O(n)
     * Space Complexity: O(1) if we ignore the output string
     * 
     * This approach reverses the entire string first, then reverses each word back
     */
    public static String reverseWords5(String s) {
        // Convert to char array for in-place operations
        char[] chars = s.toCharArray();
        int n = chars.length;

        // Step 1: Reverse the entire string
        reverse(chars, 0, n - 1);

        // Step 2: Reverse each word and remove extra spaces
        int writeIndex = 0;
        int i = 0;

        while (i < n) {
            // Skip spaces
            while (i < n && chars[i] == ' ') {
                i++;
            }

            // If we found a word
            if (i < n) {
                // Add space before word (except for first word)
                if (writeIndex > 0) {
                    chars[writeIndex++] = ' ';
                }

                // Mark the start of current word
                int wordStart = writeIndex;

                // Copy the word
                while (i < n && chars[i] != ' ') {
                    chars[writeIndex++] = chars[i++];
                }

                // Reverse the current word
                reverse(chars, wordStart, writeIndex - 1);
            }
        }

        return new String(chars, 0, writeIndex);
    }

    // Helper method for Solution 5
    private static void reverse(char[] chars, int start, int end) {
        while (start < end) {
            char temp = chars[start];
            chars[start] = chars[end];
            chars[end] = temp;
            start++;
            end--;
        }
    }

    /**
     * Solution 6: One-liner using Streams (Java 8+)
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     * 
     * This is a concise functional programming approach
     */
    public static String reverseWords6(String s) {
        return Arrays.stream(s.trim().split("\\s+"))
                .<List<String>>collect(
                        ArrayList::new,
                        (list, item) -> list.add(0, item),
                        (list1, list2) -> list2.addAll(list1))
                .stream()
                .reduce((a, b) -> a + " " + b)
                .orElse("");
        // return Arrays.stream(s.trim().split("\\s+"))
        // .collect(
        // Collector.<String, List<String>>of(
        // ArrayList::new,
        // (list, item) -> list.add(0, item),
        // (list1, list2) -> {
        // list2.addAll(list1);
        // return list2;
        // }))
        // .stream()
        // .collect(Collectors.joining(" "));

    }

    // Test method
    public static void main(String[] args) {
        // Test cases
        String[] testCases = {
                "the sky is blue",
                "  hello world  ",
                "a good   example",
                "  ",
                "word",
                "   multiple   spaces   between   words   "
        };

        String[] expected = {
                "blue is sky the",
                "world hello",
                "example good a",
                "",
                "word",
                "words between spaces multiple"
        };

        for (int i = 0; i < testCases.length; i++) {
            System.out.println("Input: \"" + testCases[i] + "\"");
            System.out.println("Expected: \"" + expected[i] + "\"");

            String result1 = reverseWords1(testCases[i]);
            String result2 = reverseWords2(testCases[i]);
            String result3 = reverseWords3(testCases[i]);
            String result4 = reverseWords4(testCases[i]);
            String result5 = reverseWords5(testCases[i]);
            String result6 = reverseWords6(testCases[i]);

            System.out.println("Solution 1 (Split + Collections): \"" + result1 + "\"");
            System.out.println("Solution 2 (Split + Two Pointers): \"" + result2 + "\"");
            System.out.println("Solution 3 (Manual Parsing): \"" + result3 + "\"");
            System.out.println("Solution 4 (Stack): \"" + result4 + "\"");
            System.out.println("Solution 5 (In-place): \"" + result5 + "\"");
            System.out.println("Solution 6 (Streams): \"" + result6 + "\"");

            // Verify all solutions match expected result
            boolean allCorrect = result1.equals(expected[i]) &&
                    result2.equals(expected[i]) &&
                    result3.equals(expected[i]) &&
                    result4.equals(expected[i]) &&
                    result5.equals(expected[i]) &&
                    result6.equals(expected[i]);

            System.out.println("All solutions correct: " + allCorrect);
            System.out.println("---");
        }
    }

}
