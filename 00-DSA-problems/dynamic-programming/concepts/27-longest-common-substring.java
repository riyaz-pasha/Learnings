
import java.util.HashMap;
import java.util.Map;

/**
 * LONGEST COMMON SUBSTRING - COMPLETE ANALYSIS AND IMPLEMENTATION
 * 
 * ============================================================================
 * PROBLEM UNDERSTANDING
 * ============================================================================
 * 
 * Given: Two strings str1 and str2
 * Find: Length of the longest CONTIGUOUS sequence of characters that appears in both strings
 * 
 * KEY DISTINCTION (Critical for interviews):
 * - Substring: CONTIGUOUS characters (e.g., "abc" is a substring of "abcde")
 * - Subsequence: NOT necessarily contiguous (e.g., "ace" is a subsequence of "abcde")
 * 
 * Example:
 * str1 = "abcdxyz"
 * str2 = "xyzabcd"
 * Longest common substring = "abcd" (length 4)
 * Note: "xyz" is also common but shorter (length 3)
 * 
 * ============================================================================
 * INTERVIEW APPROACH - HOW TO THINK THROUGH THIS PROBLEM
 * ============================================================================
 * 
 * STEP 1: Start with Brute Force (Always good to mention)
 * - Generate all substrings of str1
 * - Check if each exists in str2
 * - Track the maximum length found
 * - Time: O(n^3), Space: O(1)
 * 
 * STEP 2: Recognize the Pattern (Key insight)
 * - If characters match at position (i, j), we might be building a substring
 * - If str1[i] == str2[j], then the length at this position is 
 *   1 + length of match at (i-1, j-1)
 * - This is a DEPENDENCY on previous states → Think DP!
 * 
 * STEP 3: Define DP State
 * - dp[i][j] = length of common substring ENDING at str1[i-1] and str2[j-1]
 * - Why "ending at"? Because we need contiguous characters
 * - The answer is the MAXIMUM value in the entire dp table
 * 
 * STEP 4: Formulate Recurrence
 * - If str1[i-1] == str2[j-1]: dp[i][j] = dp[i-1][j-1] + 1
 * - Else: dp[i][j] = 0 (substring breaks, reset to 0)
 * 
 * STEP 5: Optimize Space (Impress the interviewer)
 * - Notice we only need the previous row to compute current row
 * - Can reduce space from O(m*n) to O(min(m,n))
 * 
 * ============================================================================
 */

class LongestCommonSubstring {

    /**
     * APPROACH 1: DYNAMIC PROGRAMMING (2D Array)
     * 
     * Time Complexity: O(m * n) where m = str1.length, n = str2.length
     * Space Complexity: O(m * n) for the DP table
     * 
     * This is the STANDARD approach you should present first in an interview.
     * It's clear, easy to understand, and demonstrates DP thinking.
     * 
     * REASONING:
     * - We build a 2D table where dp[i][j] represents the length of the
     *   common substring that ENDS at str1[i-1] and str2[j-1]
     * - Key insight: We only increment the count when characters match AND
     *   they extend a previous match (dp[i-1][j-1])
     * - When characters don't match, we reset to 0 (substring must be contiguous)
     * 
     * INTERVIEW TIP:
     * - Draw a small example on the whiteboard/screen
     * - Show how the table fills up step by step
     * - Emphasize the "ending at" concept
     */
    public static int longestCommonSubstring2D(String str1, String str2) {
        // Edge cases: empty strings have no common substring
        if (str1 == null || str2 == null || str1.isEmpty() || str2.isEmpty()) {
            return 0;
        }
        
        int m = str1.length();
        int n = str2.length();
        
        // dp[i][j] = length of common substring ending at str1[i-1] and str2[j-1]
        // We use (m+1) x (n+1) to handle base case easily (empty string)
        int[][] dp = new int[m + 1][n + 1];
        
        int maxLength = 0; // Track the maximum length found
        
        // Fill the DP table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                // If characters match, extend the substring from previous diagonal
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    maxLength = Math.max(maxLength, dp[i][j]);
                }
                // If characters don't match, substring breaks (dp[i][j] remains 0)
                // This is implicitly handled by initialization
            }
        }
        
        return maxLength;
    }
    
    /**
     * APPROACH 2: SPACE-OPTIMIZED DP (1D Array)
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(min(m, n)) - significant improvement!
     * 
     * OPTIMIZATION REASONING:
     * - Notice that dp[i][j] only depends on dp[i-1][j-1]
     * - We only need the PREVIOUS ROW to compute the current row
     * - We can use a single array and update it in-place
     * 
     * CAVEAT:
     * - Must process from RIGHT to LEFT to avoid overwriting values we need
     * - Or use two arrays and alternate between them
     * 
     * INTERVIEW TIP:
     * - Mention this optimization AFTER explaining the 2D approach
     * - Shows you can think about space complexity trade-offs
     * - In real interviews, start with 2D, then ask "Should I optimize space?"
     */
    public static int longestCommonSubstringOptimized(String str1, String str2) {
        if (str1 == null || str2 == null || str1.isEmpty() || str2.isEmpty()) {
            return 0;
        }
        
        // Ensure str2 is the shorter string to minimize space
        if (str1.length() < str2.length()) {
            String temp = str1;
            str1 = str2;
            str2 = temp;
        }
        
        int m = str1.length();
        int n = str2.length();
        
        // Only need space for the shorter string
        int[] dp = new int[n + 1];
        int maxLength = 0;
        
        for (int i = 1; i <= m; i++) {
            // Process from right to left to avoid overwriting needed values
            // When we update dp[j], we haven't touched dp[j-1] yet (still has old value)
            for (int j = n; j >= 1; j--) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    // dp[j-1] still contains the value from previous iteration (i-1)
                    dp[j] = dp[j - 1] + 1;
                    maxLength = Math.max(maxLength, dp[j]);
                } else {
                    // Reset to 0 when characters don't match
                    dp[j] = 0;
                }
            }
        }
        
        return maxLength;
    }
    
    /**
     * APPROACH 3: BRUTE FORCE (For Completeness)
     * 
     * Time Complexity: O(m * n * min(m, n)) - checking each substring
     * Space Complexity: O(1)
     * 
     * REASONING:
     * - Generate all substrings of str1
     * - For each substring, check if it exists in str2
     * - Track the maximum length found
     * 
     * INTERVIEW TIP:
     * - Mention this briefly to show you understand the problem
     * - Quickly move to the DP solution
     * - Useful for small inputs or as a verification method
     */
    public static int longestCommonSubstringBruteForce(String str1, String str2) {
        if (str1 == null || str2 == null || str1.isEmpty() || str2.isEmpty()) {
            return 0;
        }
        
        int maxLength = 0;
        int m = str1.length();
        
        // Try all starting positions in str1
        for (int i = 0; i < m; i++) {
            // Try all ending positions from current start
            for (int j = i + 1; j <= m; j++) {
                String substring = str1.substring(i, j);
                // Check if this substring exists in str2
                if (str2.contains(substring)) {
                    maxLength = Math.max(maxLength, substring.length());
                }
            }
        }
        
        return maxLength;
    }
    
    /**
     * APPROACH 4: TWO-ARRAY SPACE OPTIMIZATION (Alternative to Approach 2)
     * 
     * Time Complexity: O(m * n)
     * Space Complexity: O(n) - using two arrays
     * 
     * REASONING:
     * - Use two arrays: previous row and current row
     * - Easier to understand than single-array approach
     * - No need to process right-to-left
     * 
     * INTERVIEW TIP:
     * - If the interviewer questions the right-to-left processing,
     *   you can mention this cleaner alternative
     */
    public static int longestCommonSubstringTwoArrays(String str1, String str2) {
        if (str1 == null || str2 == null || str1.isEmpty() || str2.isEmpty()) {
            return 0;
        }
        
        int m = str1.length();
        int n = str2.length();
        
        int[] prev = new int[n + 1]; // Previous row
        int[] curr = new int[n + 1]; // Current row
        int maxLength = 0;
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    curr[j] = prev[j - 1] + 1;
                    maxLength = Math.max(maxLength, curr[j]);
                } else {
                    curr[j] = 0;
                }
            }
            // Swap arrays for next iteration
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        
        return maxLength;
    }
    
    /**
     * BONUS: Get the actual substring, not just the length
     * 
     * REASONING:
     * - Track the ending position when we find a new maximum
     * - Use the ending position and length to extract the substring
     * 
     * INTERVIEW TIP:
     * - Ask if they want just the length or the actual substring
     * - This shows you're thinking beyond the basic requirements
     */
    public static String getLongestCommonSubstring(String str1, String str2) {
        if (str1 == null || str2 == null || str1.isEmpty() || str2.isEmpty()) {
            return "";
        }
        
        int m = str1.length();
        int n = str2.length();
        int[][] dp = new int[m + 1][n + 1];
        
        int maxLength = 0;
        int endIndex = 0; // Ending position in str1
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    if (dp[i][j] > maxLength) {
                        maxLength = dp[i][j];
                        endIndex = i; // Mark where the substring ends in str1
                    }
                }
            }
        }
        
        // Extract the substring using endIndex and maxLength
        return str1.substring(endIndex - maxLength, endIndex);
    }
    
    /**
     * ========================================================================
     * TEST CASES - COMPREHENSIVE COVERAGE
     * ========================================================================
     */
    public static void main(String[] args) {
        // Test Case 1: Basic example
        System.out.println("Test 1: Basic Example");
        String s1 = "abcdxyz";
        String s2 = "xyzabcd";
        System.out.println("str1: " + s1 + ", str2: " + s2);
        System.out.println("Expected: 4 (substring 'abcd')");
        System.out.println("2D DP: " + longestCommonSubstring2D(s1, s2));
        System.out.println("Optimized: " + longestCommonSubstringOptimized(s1, s2));
        System.out.println("Brute Force: " + longestCommonSubstringBruteForce(s1, s2));
        System.out.println("Actual substring: " + getLongestCommonSubstring(s1, s2));
        System.out.println();
        
        // Test Case 2: No common substring
        System.out.println("Test 2: No Common Substring");
        s1 = "abc";
        s2 = "def";
        System.out.println("str1: " + s1 + ", str2: " + s2);
        System.out.println("Expected: 0");
        System.out.println("Result: " + longestCommonSubstring2D(s1, s2));
        System.out.println();
        
        // Test Case 3: One string is substring of another
        System.out.println("Test 3: One is Substring of Other");
        s1 = "abcdefgh";
        s2 = "cdef";
        System.out.println("str1: " + s1 + ", str2: " + s2);
        System.out.println("Expected: 4 (substring 'cdef')");
        System.out.println("Result: " + longestCommonSubstring2D(s1, s2));
        System.out.println("Actual substring: " + getLongestCommonSubstring(s1, s2));
        System.out.println();
        
        // Test Case 4: Identical strings
        System.out.println("Test 4: Identical Strings");
        s1 = "hello";
        s2 = "hello";
        System.out.println("str1: " + s1 + ", str2: " + s2);
        System.out.println("Expected: 5 (entire string)");
        System.out.println("Result: " + longestCommonSubstring2D(s1, s2));
        System.out.println();
        
        // Test Case 5: Multiple common substrings (should find longest)
        System.out.println("Test 5: Multiple Common Substrings");
        s1 = "OldSite:GeeksforGeeks.org";
        s2 = "NewSite:GeeksQuiz.com";
        System.out.println("str1: " + s1);
        System.out.println("str2: " + s2);
        System.out.println("Expected: 10 (substring 'Site:Geeks')");
        System.out.println("Result: " + longestCommonSubstring2D(s1, s2));
        System.out.println("Actual substring: " + getLongestCommonSubstring(s1, s2));
        System.out.println();
        
        // Test Case 6: Empty strings
        System.out.println("Test 6: Edge Cases");
        System.out.println("Empty string: " + longestCommonSubstring2D("", "abc"));
        System.out.println("Both empty: " + longestCommonSubstring2D("", ""));
        System.out.println("Single char match: " + longestCommonSubstring2D("a", "a"));
        System.out.println("Single char no match: " + longestCommonSubstring2D("a", "b"));
        System.out.println();
        
        // Test Case 7: Common substring at different positions
        System.out.println("Test 7: Common Substring at Different Positions");
        s1 = "abcdefghijk";
        s2 = "xyzdefghabc";
        System.out.println("str1: " + s1);
        System.out.println("str2: " + s2);
        System.out.println("Expected: 5 (substring 'defgh')");
        System.out.println("Result: " + longestCommonSubstring2D(s1, s2));
        System.out.println("Actual substring: " + getLongestCommonSubstring(s1, s2));
    }
}

/**
 * ============================================================================
 * INTERVIEW TIPS AND TALKING POINTS
 * ============================================================================
 * 
 * 1. CLARIFY THE PROBLEM:
 *    - "Just to confirm, we're looking for contiguous characters, not subsequence?"
 *    - "Should I return the length or the actual substring?"
 *    - "Can the strings be empty or null?"
 * 
 * 2. DISCUSS APPROACHES IN ORDER:
 *    - Brute Force: "We could try all substrings... O(n³)"
 *    - Optimized: "But there's a DP solution in O(m*n)..."
 *    - Space Optimization: "We can reduce space to O(n)..."
 * 
 * 3. KEY INSIGHTS TO MENTION:
 *    - "The key insight is that if characters match, we extend the previous diagonal"
 *    - "We reset to 0 when characters don't match because substrings must be contiguous"
 *    - "We track the maximum across all positions, not just the final cell"
 * 
 * 4. COMPARISON WITH LONGEST COMMON SUBSEQUENCE (LCS):
 *    - LCS: Can skip characters, uses max(dp[i-1][j], dp[i][j-1])
 *    - LCSubstring: Must be contiguous, resets to 0 on mismatch
 * 
 * 5. TIME TO WRITE CODE:
 *    - Start with 2D DP (5-10 minutes)
 *    - Test with 2-3 examples
 *    - If time permits, optimize space
 * 
 * 6. COMPLEXITY ANALYSIS:
 *    - Always state time and space complexity
 *    - Explain why it's O(m*n) and not O(n³)
 * 
 * 7. EDGE CASES TO MENTION:
 *    - Empty strings
 *    - Single character strings
 *    - No common substring
 *    - Entire string is common
 * 
 * ============================================================================
 * COMMON MISTAKES TO AVOID
 * ============================================================================
 * 
 * 1. Confusing with Longest Common Subsequence (very common mistake!)
 * 2. Forgetting to track the maximum value (returning dp[m][n] instead)
 * 3. Not resetting to 0 when characters don't match
 * 4. Off-by-one errors with string indices
 * 5. In space-optimized version, processing left-to-right instead of right-to-left
 * 
 * ============================================================================
 */


/**
 * Longest Common Substring (CONTIGUOUS) - Recursive Variants
 *
 * This file contains:
 * 1) Pure brute-force recursion
 * 2) Memoized recursion (Top-Down DP)
 * 3) Clean wrapper + test cases
 *
 * Key Idea:
 * - If characters match, extend current substring (count + 1)
 * - If they don't, reset count to 0 (contiguity breaks)
 * - Always explore all start positions using branching
 */
class LongestCommonSubstringRecursive {

    // ==========================================================
    // 1️⃣ PURE RECURSIVE SOLUTION (BRUTE FORCE)
    // ==========================================================
    // Time: Exponential O(3^(m+n))
    // Space: O(m+n) recursion stack
    //
    // Parameters:
    // i, j     -> current indices in s1 and s2
    // count    -> current length of contiguous match
    //
    // Reasoning:
    // - count1: extend match if chars equal
    // - count2: skip char from s1 (reset count)
    // - count3: skip char from s2 (reset count)
    // - result = max of all three
    public static int lcsBruteForce(String s1, String s2) {
        return lcsBruteForceHelper(s1, s2, s1.length(), s2.length(), 0);
    }

    private static int lcsBruteForceHelper(String s1, String s2, int i, int j, int count) {

        // Base case:
        // If either string is fully consumed, no more matching possible.
        // Return the length of the current contiguous substring.
        if (i == 0 || j == 0) {
            return count;
        }

        int count1 = count;

        // Case 1: Characters match → extend current contiguous substring
        if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
            count1 = lcsBruteForceHelper(s1, s2, i - 1, j - 1, count + 1);
        }

        // Case 2: Skip one character from s1
        // Reset count to 0 because contiguity is broken
        int count2 = lcsBruteForceHelper(s1, s2, i - 1, j, 0);

        // Case 3: Skip one character from s2
        // Reset count to 0 because contiguity is broken
        int count3 = lcsBruteForceHelper(s1, s2, i, j - 1, 0);

        // The longest substring could be:
        // - the one we are currently building (count1)
        // - one starting earlier in s1 (count2)
        // - one starting earlier in s2 (count3)
        return Math.max(count1, Math.max(count2, count3));
    }

    // ==========================================================
    // 2️⃣ MEMOIZED RECURSION (TOP-DOWN DP)
    // ==========================================================
    // Time: O(m * n * min(m,n))
    // Space: O(m * n * min(m,n))
    //
    // Why memoization?
    // - Brute force recomputes the same (i, j, count) states many times.
    // - We cache results to avoid repeated work.
    private static Map<String, Integer> memo = new HashMap<>();

    public static int lcsMemoized(String s1, String s2) {
        memo.clear(); // important for multiple runs
        return lcsMemoizedHelper(s1, s2, s1.length(), s2.length(), 0);
    }

    private static int lcsMemoizedHelper(String s1, String s2, int i, int j, int count) {

        // Base case
        if (i == 0 || j == 0) {
            return count;
        }

        // Create a unique key for memoization
        // We must include 'count' because different ongoing substrings
        // at the same (i, j) can yield different future results.
        String key = i + "|" + j + "|" + count;

        if (memo.containsKey(key)) {
            return memo.get(key);
        }

        int count1 = count;

        // Case 1: Characters match → extend substring
        if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
            count1 = lcsMemoizedHelper(s1, s2, i - 1, j - 1, count + 1);
        }

        // Case 2: Skip character from s1
        int count2 = lcsMemoizedHelper(s1, s2, i - 1, j, 0);

        // Case 3: Skip character from s2
        int count3 = lcsMemoizedHelper(s1, s2, i, j - 1, 0);

        int result = Math.max(count1, Math.max(count2, count3));
        memo.put(key, result);

        return result;
    }

    // ==========================================================
    // 3️⃣ CLEAN DRIVER METHOD + TEST CASES
    // ==========================================================
    public static void main(String[] args) {

        String s1 = "abcdef";
        String s2 = "zcdemf";

        System.out.println("Input:");
        System.out.println("s1 = " + s1);
        System.out.println("s2 = " + s2);
        System.out.println();

        System.out.println("Brute Force Recursive Result: " + lcsBruteForce(s1, s2));
        System.out.println("Memoized Recursive Result   : " + lcsMemoized(s1, s2));

        // Additional test cases
        System.out.println("\nMore Tests:");

        test("abcdxyz", "xyzabcd");  // Expected: 4 ("abcd")
        test("zxabcdezy", "yzabcdezx"); // Expected: 6 ("abcdez")
        test("abc", "def"); // Expected: 0
        test("aaaa", "aa"); // Expected: 2
    }

    private static void test(String s1, String s2) {
        int result = lcsMemoized(s1, s2);
        System.out.println("s1 = " + s1 + ", s2 = " + s2 + " → " + result);
    }
}
