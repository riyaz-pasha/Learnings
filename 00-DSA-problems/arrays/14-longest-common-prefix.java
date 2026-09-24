/*
 * Write a function to find the longest common prefix string amongst an array of
 * strings.
 * 
 * If there is no common prefix, return an empty string "".
 * 
 * Example 1:
 * Input: strs = ["flower","flow","flight"]
 * Output: "fl"
 * 
 * Example 2:
 * Input: strs = ["dog","racecar","car"]
 * Output: ""
 * Explanation: There is no common prefix among the input strings.
 */

class LongestCommonPrefix {

    /**
     * Solution 1: Vertical Scanning (Character by Character)
     * Time Complexity: O(S) where S is the sum of all characters in all strings
     * Space Complexity: O(1)
     * 
     * This approach compares characters at the same position across all strings.
     */
    public static String longestCommonPrefix1(String[] strs) {
        if (strs == null || strs.length == 0) {
            return "";
        }

        // Use the first string as reference
        for (int i = 0; i < strs[0].length(); i++) {
            char c = strs[0].charAt(i);

            // Compare this character with the same position in all other strings
            for (int j = 1; j < strs.length; j++) {
                // If we've reached the end of any string or characters don't match
                if (i >= strs[j].length() || strs[j].charAt(i) != c) {
                    return strs[0].substring(0, i);
                }
            }
        }

        // If we've gone through all characters of the first string, it's the common
        // prefix
        return strs[0];
    }

    /**
     * Solution 2: Horizontal Scanning (Pairwise Comparison)
     * Time Complexity: O(S) where S is the sum of all characters in all strings
     * Space Complexity: O(1)
     * 
     * This approach finds the common prefix between first two strings,
     * then between the result and the third string, and so on.
     */
    public static String longestCommonPrefix2(String[] strs) {
        if (strs == null || strs.length == 0) {
            return "";
        }

        String prefix = strs[0];

        for (int i = 1; i < strs.length; i++) {
            // Find common prefix between current prefix and next string
            while (strs[i].indexOf(prefix) != 0) {
                // Reduce prefix by one character from the end
                prefix = prefix.substring(0, prefix.length() - 1);
                if (prefix.isEmpty()) {
                    return "";
                }
            }
        }

        return prefix;
    }

    /**
     * Solution 3: Divide and Conquer
     * Time Complexity: O(S) where S is the sum of all characters in all strings
     * Space Complexity: O(m * log n) where m is the length of the longest string
     * and n is the number of strings (due to recursion stack)
     * 
     * This approach divides the array into two halves and recursively
     * finds the common prefix of each half.
     */
    public static String longestCommonPrefix3(String[] strs) {
        if (strs == null || strs.length == 0) {
            return "";
        }
        return longestCommonPrefixHelper(strs, 0, strs.length - 1);
    }

    private static String longestCommonPrefixHelper(String[] strs, int left, int right) {
        if (left == right) {
            return strs[left];
        }

        int mid = (left + right) / 2;
        String leftPrefix = longestCommonPrefixHelper(strs, left, mid);
        String rightPrefix = longestCommonPrefixHelper(strs, mid + 1, right);

        return commonPrefix(leftPrefix, rightPrefix);
    }

    private static String commonPrefix(String left, String right) {
        int minLen = Math.min(left.length(), right.length());
        for (int i = 0; i < minLen; i++) {
            if (left.charAt(i) != right.charAt(i)) {
                return left.substring(0, i);
            }
        }
        return left.substring(0, minLen);
    }

    /**
     * Solution 4: Binary Search
     * Time Complexity: O(S * log m) where S is the sum of all characters
     * and m is the length of the shortest string
     * Space Complexity: O(1)
     * 
     * This approach uses binary search on the length of the common prefix.
     */
    public static String longestCommonPrefix4(String[] strs) {
        if (strs == null || strs.length == 0) {
            return "";
        }

        // Find the minimum length among all strings
        int minLen = Integer.MAX_VALUE;
        for (String str : strs) {
            minLen = Math.min(minLen, str.length());
        }

        int low = 1, high = minLen;
        while (low <= high) {
            int mid = (low + high) / 2;
            if (isCommonPrefix(strs, mid)) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return strs[0].substring(0, (low + high) / 2);
    }

    private static boolean isCommonPrefix(String[] strs, int len) {
        String prefix = strs[0].substring(0, len);
        for (int i = 1; i < strs.length; i++) {
            if (!strs[i].startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    // Test method
    public static void main(String[] args) {
        // Test cases
        String[] test1 = { "flower", "flow", "flight" };
        String[] test2 = { "dog", "racecar", "car" };
        String[] test3 = { "interspecies", "interstellar", "interstate" };
        String[] test4 = { "prefix", "prefixes", "preform" };
        String[] test5 = { "a" };
        String[] test6 = {};

        System.out.println("Test 1 - ['flower', 'flow', 'flight']:");
        System.out.println("Solution 1: '" + longestCommonPrefix1(test1) + "'");
        System.out.println("Solution 2: '" + longestCommonPrefix2(test1) + "'");
        System.out.println("Solution 3: '" + longestCommonPrefix3(test1) + "'");
        System.out.println("Solution 4: '" + longestCommonPrefix4(test1) + "'");

        System.out.println("\nTest 2 - ['dog', 'racecar', 'car']:");
        System.out.println("Solution 1: '" + longestCommonPrefix1(test2) + "'");
        System.out.println("Solution 2: '" + longestCommonPrefix2(test2) + "'");
        System.out.println("Solution 3: '" + longestCommonPrefix3(test2) + "'");
        System.out.println("Solution 4: '" + longestCommonPrefix4(test2) + "'");

        System.out.println("\nTest 3 - ['interspecies', 'interstellar', 'interstate']:");
        System.out.println("Solution 1: '" + longestCommonPrefix1(test3) + "'");
        System.out.println("Solution 2: '" + longestCommonPrefix2(test3) + "'");
        System.out.println("Solution 3: '" + longestCommonPrefix3(test3) + "'");
        System.out.println("Solution 4: '" + longestCommonPrefix4(test3) + "'");
    }

}

class Solution {
    /**
     * Your Original Solution - Horizontal Scanning with Character Comparison
     * Time Complexity: O(S) where S is the sum of all characters in all strings
     * Space Complexity: O(1) - only using a few variables
     * 
     * Strengths:
     * - Clean and readable code
     * - Efficient character-by-character comparison
     * - Good early termination when no common prefix exists
     * - Handles edge cases properly
     */
    public String longestCommonPrefix(String[] strs) {
        if (strs == null || strs.length == 0)
            return "";

        String longestPrefix = strs[0];

        for (int i = 1; i < strs.length; i++) {
            int j = 0;
            // Find the common prefix length between the current prefix and the current word
            while (j < longestPrefix.length() && j < strs[i].length() &&
                    longestPrefix.charAt(j) == strs[i].charAt(j)) {
                j++;
            }

            // Update the longest prefix
            longestPrefix = longestPrefix.substring(0, j);

            // If no common prefix remains, return immediately
            if (longestPrefix.isEmpty())
                return "";
        }

        return longestPrefix;
    }

    /**
     * Slightly Optimized Version - Minor improvements for readability
     * Same time and space complexity, but with cleaner variable names
     */
    public String longestCommonPrefixOptimized(String[] strs) {
        if (strs == null || strs.length == 0)
            return "";

        String prefix = strs[0];

        for (int i = 1; i < strs.length; i++) {
            // Find how many characters match from the beginning
            int matchCount = 0;
            int minLength = Math.min(prefix.length(), strs[i].length());

            while (matchCount < minLength && prefix.charAt(matchCount) == strs[i].charAt(matchCount)) {
                matchCount++;
            }

            // Update prefix to the common part
            prefix = prefix.substring(0, matchCount);

            // Early termination if no common prefix
            if (prefix.isEmpty())
                return "";
        }

        return prefix;
    }

    /**
     * Alternative: Using String's built-in methods (similar to your approach)
     * This version is more concise but potentially less efficient due to indexOf
     */
    public String longestCommonPrefixBuiltIn(String[] strs) {
        if (strs == null || strs.length == 0)
            return "";

        String prefix = strs[0];

        for (int i = 1; i < strs.length; i++) {
            while (strs[i].indexOf(prefix) != 0) {
                prefix = prefix.substring(0, prefix.length() - 1);
                if (prefix.isEmpty())
                    return "";
            }
        }

        return prefix;
    }

    // Test method to compare all approaches
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test cases
        String[][] testCases = {
                { "flower", "flow", "flight" }, // Expected: "fl"
                { "dog", "racecar", "car" }, // Expected: ""
                { "interspecies", "interstellar", "interstate" }, // Expected: "inter"
                { "prefix", "prefixes", "preform" }, // Expected: "pre"
                { "a" }, // Expected: "a"
                { "", "b" }, // Expected: ""
                { "abc", "abc", "abc" } // Expected: "abc"
        };

        for (int i = 0; i < testCases.length; i++) {
            String result1 = solution.longestCommonPrefix(testCases[i]);
            String result2 = solution.longestCommonPrefixOptimized(testCases[i]);
            String result3 = solution.longestCommonPrefixBuiltIn(testCases[i]);

            System.out.println("Test " + (i + 1) + ": " + java.util.Arrays.toString(testCases[i]));
            System.out.println("Original: '" + result1 + "'");
            System.out.println("Optimized: '" + result2 + "'");
            System.out.println("Built-in: '" + result3 + "'");
            System.out.println();
        }
    }

}
