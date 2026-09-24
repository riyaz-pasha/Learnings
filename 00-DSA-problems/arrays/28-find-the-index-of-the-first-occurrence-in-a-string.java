/*
 * Given two strings needle and haystack, return the index of the first
 * occurrence of needle in haystack, or -1 if needle is not part of haystack.
 * 
 * Example 1:
 * Input: haystack = "sadbutsad", needle = "sad"
 * Output: 0
 * Explanation: "sad" occurs at index 0 and 6.
 * The first occurrence is at index 0, so we return 0.
 *
 * Example 2:
 * Input: haystack = "leetcode", needle = "leeto"
 * Output: -1
 * Explanation: "leeto" did not occur in "leetcode", so we return -1.
 */

class FindFirstOccurrence {

    /**
     * Solution 1: Built-in indexOf() method
     * Time Complexity: O(n*m) in worst case, but optimized internally
     * Space Complexity: O(1)
     * 
     * This is the simplest approach using Java's built-in method.
     */
    public static int strStr1(String haystack, String needle) {
        return haystack.indexOf(needle);
    }

    /**
     * Solution 2: Brute Force (Two Pointers)
     * Time Complexity: O(n*m) where n = haystack length, m = needle length
     * Space Complexity: O(1)
     * 
     * This is the most straightforward manual implementation.
     */
    public static int strStr2(String haystack, String needle) {
        if (needle.isEmpty())
            return 0;
        if (haystack.length() < needle.length())
            return -1;

        int n = haystack.length();
        int m = needle.length();

        // Try each possible starting position
        for (int i = 0; i <= n - m; i++) {
            int j = 0;

            // Check if needle matches starting at position i
            while (j < m && haystack.charAt(i + j) == needle.charAt(j)) {
                j++;
            }

            // If we matched the entire needle
            if (j == m) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Solution 3: Optimized Brute Force with early termination
     * Time Complexity: O(n*m) worst case, but better average case
     * Space Complexity: O(1)
     * 
     * This version has some optimizations for better average performance.
     */
    public static int strStr3(String haystack, String needle) {
        if (needle.isEmpty())
            return 0;
        if (haystack.length() < needle.length())
            return -1;

        int n = haystack.length();
        int m = needle.length();

        for (int i = 0; i <= n - m; i++) {
            // Quick check: if first character doesn't match, skip
            if (haystack.charAt(i) != needle.charAt(0)) {
                continue;
            }

            // Check the rest of the pattern
            boolean found = true;
            for (int j = 1; j < m; j++) {
                if (haystack.charAt(i + j) != needle.charAt(j)) {
                    found = false;
                    break;
                }
            }

            if (found) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Solution 4: KMP (Knuth-Morris-Pratt) Algorithm
     * Time Complexity: O(n + m)
     * Space Complexity: O(m) for the LPS array
     * 
     * This is the optimal solution for string matching with linear time complexity.
     */
    public static int strStr4(String haystack, String needle) {
        if (needle.isEmpty())
            return 0;
        if (haystack.length() < needle.length())
            return -1;

        // Build the LPS (Longest Proper Prefix which is also Suffix) array
        int[] lps = buildLPS(needle);

        int i = 0; // index for haystack
        int j = 0; // index for needle

        while (i < haystack.length()) {
            if (haystack.charAt(i) == needle.charAt(j)) {
                i++;
                j++;
            }

            if (j == needle.length()) {
                return i - j; // Found match
            } else if (i < haystack.length() && haystack.charAt(i) != needle.charAt(j)) {
                if (j != 0) {
                    j = lps[j - 1]; // Use LPS to avoid redundant comparisons
                } else {
                    i++;
                }
            }
        }

        return -1;
    }

    // Helper method to build LPS array for KMP
    private static int[] buildLPS(String pattern) {
        int m = pattern.length();
        int[] lps = new int[m];
        int len = 0; // length of previous longest prefix suffix
        int i = 1;

        while (i < m) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }

        return lps;
    }

    /**
     * Solution 5: Rolling Hash (Rabin-Karp Algorithm)
     * Time Complexity: O(n + m) average case, O(n*m) worst case
     * Space Complexity: O(1)
     * 
     * This approach uses hashing to quickly compare substrings.
     */
    public static int strStr5(String haystack, String needle) {
        if (needle.isEmpty())
            return 0;
        if (haystack.length() < needle.length())
            return -1;

        int n = haystack.length();
        int m = needle.length();
        int base = 256;
        int mod = 101; // A prime number

        // Calculate hash of needle and first window of haystack
        long needleHash = 0;
        long windowHash = 0;
        long h = 1;

        // Calculate h = base^(m-1) % mod
        for (int i = 0; i < m - 1; i++) {
            h = (h * base) % mod;
        }

        // Calculate hash of needle and first window
        for (int i = 0; i < m; i++) {
            needleHash = (base * needleHash + needle.charAt(i)) % mod;
            windowHash = (base * windowHash + haystack.charAt(i)) % mod;
        }

        // Slide the pattern over haystack
        for (int i = 0; i <= n - m; i++) {
            // Check if hash values match
            if (needleHash == windowHash) {
                // Verify character by character (to handle hash collisions)
                boolean match = true;
                for (int j = 0; j < m; j++) {
                    if (haystack.charAt(i + j) != needle.charAt(j)) {
                        match = false;
                        break;
                    }
                }
                if (match)
                    return i;
            }

            // Calculate hash for next window
            if (i < n - m) {
                windowHash = (base * (windowHash - haystack.charAt(i) * h) +
                        haystack.charAt(i + m)) % mod;

                // Handle negative hash
                if (windowHash < 0) {
                    windowHash += mod;
                }
            }
        }

        return -1;
    }

    /**
     * Solution 6: Using substring() method
     * Time Complexity: O(n*m) due to substring creation
     * Space Complexity: O(m) for substring creation
     * 
     * This approach uses Java's substring method for comparison.
     */
    public static int strStr6(String haystack, String needle) {
        if (needle.isEmpty())
            return 0;
        if (haystack.length() < needle.length())
            return -1;

        int n = haystack.length();
        int m = needle.length();

        for (int i = 0; i <= n - m; i++) {
            if (haystack.substring(i, i + m).equals(needle)) {
                return i;
            }
        }

        return -1;
    }

    // Test method with comprehensive test cases
    public static void main(String[] args) {
        // Test cases
        String[][] testCases = {
                { "sadbutsad", "sad" }, // Expected: 0
                { "leetcode", "leeto" }, // Expected: -1
                { "hello", "ll" }, // Expected: 2
                { "aaaaa", "bba" }, // Expected: -1
                { "", "" }, // Expected: 0
                { "a", "a" }, // Expected: 0
                { "mississippi", "issip" }, // Expected: 4
                { "aabaaabaaac", "aabaaac" }, // Expected: 5 (good for KMP)
                { "abcabcabcabc", "abcabc" }, // Expected: 0
                { "abababab", "abab" } // Expected: 0
        };

        int[] expected = { 0, -1, 2, -1, 0, 0, 4, 5, 0, 0 };
        String[] solutionNames = {
                "Built-in indexOf",
                "Brute Force",
                "Optimized Brute Force",
                "KMP Algorithm",
                "Rolling Hash",
                "Substring Method"
        };

        for (int i = 0; i < testCases.length; i++) {
            String haystack = testCases[i][0];
            String needle = testCases[i][1];
            int exp = expected[i];

            System.out.println("Test " + (i + 1) +
                    ": haystack=\"" + haystack +
                    "\", needle=\"" + needle + "\"");
            System.out.println("Expected: " + exp);

            // Test all solutions
            int[] results = {
                    strStr1(haystack, needle),
                    strStr2(haystack, needle),
                    strStr3(haystack, needle),
                    strStr4(haystack, needle),
                    strStr5(haystack, needle),
                    strStr6(haystack, needle)
            };

            for (int j = 0; j < results.length; j++) {
                System.out.println(solutionNames[j] + ": " + results[j] +
                        (results[j] == exp ? " ✓" : " ✗"));
            }
            System.out.println("---\n");
        }

        // Performance comparison
        System.out.println("Performance Test (Large Input):");
        StringBuilder largeHaystack = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeHaystack.append("ab");
        }
        largeHaystack.append("abc"); // Pattern at the end

        String haystack = largeHaystack.toString();
        String needle = "abc";

        long start, end;

        // Test KMP vs Brute Force on large input
        start = System.nanoTime();
        int result2 = strStr2(haystack, needle);
        end = System.nanoTime();
        System.out.println("Brute Force: " + (end - start) / 1000000.0 + " ms, result: " + result2);

        start = System.nanoTime();
        int result4 = strStr4(haystack, needle);
        end = System.nanoTime();
        System.out.println("KMP Algorithm: " + (end - start) / 1000000.0 + " ms, result: " + result4);

        start = System.nanoTime();
        int result5 = strStr5(haystack, needle);
        end = System.nanoTime();
        System.out.println("Rolling Hash: " + (end - start) / 1000000.0 + " ms, result: " + result5);
    }

    /**
     * Helper method to visualize KMP LPS array construction
     */
    public static void demonstrateKMP(String pattern) {
        System.out.println("KMP LPS Array for pattern: \"" + pattern + "\"");
        int[] lps = buildLPS(pattern);

        System.out.print("Index:   ");
        for (int i = 0; i < pattern.length(); i++) {
            System.out.printf("%2d ", i);
        }
        System.out.println();

        System.out.print("Char:    ");
        for (int i = 0; i < pattern.length(); i++) {
            System.out.printf("%2c ", pattern.charAt(i));
        }
        System.out.println();

        System.out.print("LPS:     ");
        for (int i = 0; i < lps.length; i++) {
            System.out.printf("%2d ", lps[i]);
        }
        System.out.println("\n");
    }

}
