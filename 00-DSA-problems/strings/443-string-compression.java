/*
 * Given an array of characters chars, compress it using the following
 * algorithm:
 * 
 * Begin with an empty string s. For each group of consecutive repeating
 * characters in chars:
 * 
 * If the group's length is 1, append the character to s.
 * Otherwise, append the character followed by the group's length.
 * The compressed string s should not be returned separately, but instead, be
 * stored in the input character array chars. Note that group lengths that are
 * 10 or longer will be split into multiple characters in chars.
 * 
 * After you are done modifying the input array, return the new length of the
 * array.
 * 
 * You must write an algorithm that uses only constant extra space.
 * 
 * Example 1:
 * Input: chars = ["a","a","b","b","c","c","c"]
 * Output: Return 6, and the first 6 characters of the input array should be:
 * ["a","2","b","2","c","3"]
 * Explanation: The groups are "aa", "bb", and "ccc". This compresses to
 * "a2b2c3".
 * 
 * Example 2:
 * Input: chars = ["a"]
 * Output: Return 1, and the first character of the input array should be: ["a"]
 * Explanation: The only group is "a", which remains uncompressed since it's a
 * single character.
 * 
 * Example 3:
 * Input: chars = ["a","b","b","b","b","b","b","b","b","b","b","b","b"]
 * Output: Return 4, and the first 4 characters of the input array should be:
 * ["a","b","1","2"].
 * Explanation: The groups are "a" and "bbbbbbbbbbbb". This compresses to
 * "ab12".
 */

class StringCompression {

    /**
     * Solution 1: Two-pointer approach (OPTIMAL)
     * Time: O(n), Space: O(1)
     * In-place compression using read and write pointers
     */
    public int compress1(char[] chars) {
        int write = 0; // Write pointer for compressed result
        int read = 0; // Read pointer to traverse the array

        while (read < chars.length) {
            char currentChar = chars[read];
            int count = 0;

            // Count consecutive characters
            while (read < chars.length && chars[read] == currentChar) {
                read++;
                count++;
            }

            // Write the character
            chars[write++] = currentChar;

            // Write the count if greater than 1
            if (count > 1) {
                // Convert count to string and write each digit
                String countStr = String.valueOf(count);
                for (char c : countStr.toCharArray()) {
                    chars[write++] = c;
                }
            }
        }

        return write;
    }

    /**
     * Solution 2: Optimized without String conversion
     * Time: O(n), Space: O(1)
     * Manually handles number conversion to avoid String overhead
     */
    public int compress2(char[] chars) {
        int write = 0;
        int i = 0;

        while (i < chars.length) {
            char currentChar = chars[i];
            int count = 0;

            // Count consecutive characters
            while (i < chars.length && chars[i] == currentChar) {
                i++;
                count++;
            }

            // Write the character
            chars[write++] = currentChar;

            // Write count if > 1
            if (count > 1) {
                write = writeCount(chars, write, count);
            }
        }

        return write;
    }

    /**
     * Helper method to write count digits manually
     */
    private int writeCount(char[] chars, int writePos, int count) {
        int start = writePos;

        // Write digits in reverse order first
        while (count > 0) {
            chars[writePos++] = (char) ('0' + count % 10);
            count /= 10;
        }

        // Reverse the digits to get correct order
        int end = writePos - 1;
        while (start < end) {
            char temp = chars[start];
            chars[start] = chars[end];
            chars[end] = temp;
            start++;
            end--;
        }

        return writePos;
    }

    /**
     * Solution 3: Single pass with clean logic
     * Time: O(n), Space: O(1)
     * Most readable version of the optimal approach
     */
    public int compress3(char[] chars) {
        if (chars.length == 0)
            return 0;

        int writeIndex = 0;
        int readIndex = 0;

        while (readIndex < chars.length) {
            char currentChar = chars[readIndex];
            int count = 1;

            // Count consecutive occurrences
            while (readIndex + 1 < chars.length &&
                    chars[readIndex + 1] == currentChar) {
                readIndex++;
                count++;
            }

            // Write character
            chars[writeIndex++] = currentChar;

            // Write count if more than 1
            if (count > 1) {
                for (char c : Integer.toString(count).toCharArray()) {
                    chars[writeIndex++] = c;
                }
            }

            readIndex++;
        }

        return writeIndex;
    }

    /**
     * Solution 4: Iterative counting approach
     * Time: O(n), Space: O(1)
     * Alternative implementation with different loop structure
     */
    public int compress4(char[] chars) {
        int n = chars.length;
        if (n <= 1)
            return n;

        int writePos = 0;

        for (int i = 0; i < n;) {
            char currentChar = chars[i];
            int j = i;

            // Find the end of current character group
            while (j < n && chars[j] == currentChar) {
                j++;
            }

            int count = j - i;

            // Write character
            chars[writePos++] = currentChar;

            // Write count if > 1
            if (count > 1) {
                String countStr = String.valueOf(count);
                for (int k = 0; k < countStr.length(); k++) {
                    chars[writePos++] = countStr.charAt(k);
                }
            }

            i = j; // Move to next group
        }

        return writePos;
    }

    /**
     * Solution 5: Recursive approach (Not optimal due to space)
     * Time: O(n), Space: O(n) due to recursion
     * Academic interest - demonstrates recursive thinking
     */
    public int compress5(char[] chars) {
        return compressRecursive(chars, 0, 0);
    }

    private int compressRecursive(char[] chars, int readPos, int writePos) {
        if (readPos >= chars.length) {
            return writePos;
        }

        char currentChar = chars[readPos];
        int count = 1;
        int nextPos = readPos + 1;

        // Count consecutive characters
        while (nextPos < chars.length && chars[nextPos] == currentChar) {
            count++;
            nextPos++;
        }

        // Write character
        chars[writePos++] = currentChar;

        // Write count if > 1
        if (count > 1) {
            String countStr = String.valueOf(count);
            for (char c : countStr.toCharArray()) {
                chars[writePos++] = c;
            }
        }

        return compressRecursive(chars, nextPos, writePos);
    }

    /**
     * Utility method to print array for debugging
     */
    private void printArray(char[] arr, int length) {
        System.out.print("[");
        for (int i = 0; i < length; i++) {
            System.out.print("\"" + arr[i] + "\"");
            if (i < length - 1)
                System.out.print(",");
        }
        System.out.println("]");
    }

    /**
     * Utility method to create a copy of array for testing
     */
    private char[] copyArray(char[] original) {
        return java.util.Arrays.copyOf(original, original.length);
    }

    // Test method to verify all solutions
    public static void main(String[] args) {
        StringCompression solution = new StringCompression();

        // Test cases
        char[][] testCases = {
                { 'a', 'a', 'b', 'b', 'c', 'c', 'c' },
                { 'a' },
                { 'a', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b' },
                { 'a', 'a', 'a', 'b', 'b', 'a', 'a' },
                { 'a', 'b', 'c' },
                { 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a' },
                {}
        };

        int[] expectedLengths = { 6, 1, 4, 6, 3, 4, 0 };

        String[][] expectedResults = {
                { "a", "2", "b", "2", "c", "3" },
                { "a" },
                { "a", "b", "1", "2" },
                { "a", "3", "b", "2", "a", "2" },
                { "a", "b", "c" },
                { "a", "1", "2" },
                {}
        };

        for (int i = 0; i < testCases.length; i++) {
            char[] original = testCases[i];
            int expectedLen = expectedLengths[i];
            String[] expectedResult = expectedResults[i];

            System.out.println("Test Case " + (i + 1) + ":");
            System.out.print("Input: ");
            solution.printArray(original, original.length);
            System.out.println("Expected length: " + expectedLen);
            System.out.print("Expected result: ");
            if (expectedResult.length > 0) {
                System.out.print("[");
                for (int j = 0; j < expectedResult.length; j++) {
                    System.out.print("\"" + expectedResult[j] + "\"");
                    if (j < expectedResult.length - 1)
                        System.out.print(",");
                }
                System.out.println("]");
            } else {
                System.out.println("[]");
            }

            // Test all solutions
            char[] test1 = solution.copyArray(original);
            char[] test2 = solution.copyArray(original);
            char[] test3 = solution.copyArray(original);
            char[] test4 = solution.copyArray(original);
            char[] test5 = solution.copyArray(original);

            int result1 = solution.compress1(test1);
            int result2 = solution.compress2(test2);
            int result3 = solution.compress3(test3);
            int result4 = solution.compress4(test4);
            int result5 = solution.compress5(test5);

            System.out.println("Solution 1 - Two-pointer: Length = " + result1 +
                    (result1 == expectedLen ? " ✓" : " ✗"));
            System.out.print("Result: ");
            solution.printArray(test1, result1);

            System.out.println("Solution 2 - No String conversion: Length = " + result2 +
                    (result2 == expectedLen ? " ✓" : " ✗"));
            System.out.print("Result: ");
            solution.printArray(test2, result2);

            System.out.println("Solution 3 - Clean logic: Length = " + result3 +
                    (result3 == expectedLen ? " ✓" : " ✗"));
            System.out.print("Result: ");
            solution.printArray(test3, result3);

            System.out.println("Solution 4 - Iterative: Length = " + result4 +
                    (result4 == expectedLen ? " ✓" : " ✗"));
            System.out.print("Result: ");
            solution.printArray(test4, result4);

            System.out.println("Solution 5 - Recursive: Length = " + result5 +
                    (result5 == expectedLen ? " ✓" : " ✗"));
            System.out.print("Result: ");
            solution.printArray(test5, result5);

            System.out.println("---");
        }

        // Performance test
        System.out.println("Performance Test:");
        char[] largeArray = new char[10000];
        java.util.Arrays.fill(largeArray, 'a');

        char[] test = solution.copyArray(largeArray);
        long start = System.nanoTime();
        int result = solution.compress1(test);
        long time1 = System.nanoTime() - start;

        test = solution.copyArray(largeArray);
        start = System.nanoTime();
        result = solution.compress2(test);
        long time2 = System.nanoTime() - start;

        System.out.println("Large array (10000 'a's):");
        System.out.println("Solution 1: " + time1 + " ns");
        System.out.println("Solution 2: " + time2 + " ns");
        System.out.println("Compressed to length: " + result);
    }

}
