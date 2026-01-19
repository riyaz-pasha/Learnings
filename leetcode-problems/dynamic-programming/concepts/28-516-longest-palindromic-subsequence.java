/**
 * LONGEST PALINDROMIC SUBSEQUENCE - COMPLETE ANALYSIS AND IMPLEMENTATION
 * 
 * ============================================================================
 * PROBLEM UNDERSTANDING
 * ============================================================================
 * 
 * Given: A string s
 * Find: Length of the longest palindromic SUBSEQUENCE (not substring!)
 * 
 * KEY DEFINITIONS:
 * - Palindrome: Reads the same forwards and backwards (e.g., "aba", "racecar")
 * - Subsequence: Characters in original order but NOT necessarily contiguous
 *   Example: "ace" is a subsequence of "abcde"
 * - Substring: MUST be contiguous characters
 * 
 * CRITICAL DISTINCTION (Interview Red Flag):
 * - Palindromic SUBSEQUENCE: Can skip characters
 *   Example: s = "bbbab" → longest palindromic subsequence = "bbbb" (length 4)
 *            We skip 'a' to form the palindrome
 * 
 * - Palindromic SUBSTRING: Must be contiguous
 *   Example: s = "bbbab" → longest palindromic substring = "bbb" (length 3)
 * 
 * ============================================================================
 * INTERVIEW APPROACH - HOW TO ARRIVE AT THE SOLUTION
 * ============================================================================
 * 
 * STEP 1: Understand with Examples
 * Example 1: s = "bbbab"
 * - Possible palindromic subsequences: "b", "bb", "bbb", "bbbb", "bab", "aba"
 * - Longest: "bbbb" (length 4)
 * 
 * Example 2: s = "cbbd"
 * - Possible palindromic subsequences: "c", "b", "bb", "d", "cbc", "cbd" (not palindrome)
 * - Longest: "bb" (length 2)
 * 
 * STEP 2: Recognize the Key Insight (THE AHA MOMENT!)
 * If you REVERSE the string and find the Longest Common Subsequence (LCS)
 * between original and reversed, that gives you the longest palindromic subsequence!
 * 
 * Why? A palindrome reads the same forwards and backwards.
 * So characters that match between s and reverse(s) form a palindrome!
 * 
 * Example: s = "bbbab"
 *          reverse = "babbb"
 *          LCS(s, reverse) = "bbbb" → This is our answer!
 * 
 * STEP 3: Alternative Direct DP Approach
 * We can also solve it directly using interval DP:
 * - dp[i][j] = longest palindromic subsequence in s[i...j]
 * - If s[i] == s[j]: dp[i][j] = 2 + dp[i+1][j-1]
 * - Else: dp[i][j] = max(dp[i+1][j], dp[i][j-1])
 * 
 * STEP 4: Choose the Right Approach
 * - LCS approach: Easier to understand, reuses known algorithm
 * - Direct DP: More efficient, shows deeper understanding
 * - In interview: Mention both, implement the one you're most comfortable with
 * 
 * ============================================================================
 * DETAILED REASONING FOR DIRECT DP APPROACH
 * ============================================================================
 * 
 * State Definition:
 * dp[i][j] = length of longest palindromic subsequence in substring s[i...j]
 * 
 * Base Cases:
 * - Single character: dp[i][i] = 1 (every character is a palindrome)
 * - Two characters: dp[i][i+1] = 2 if s[i] == s[i+1], else 1
 * 
 * Recurrence Relation (The Heart of the Solution):
 * 
 * Case 1: If s[i] == s[j]
 *   → These characters can be part of the palindrome at both ends
 *   → dp[i][j] = 2 + dp[i+1][j-1]
 *   → Example: "bab" → 'b' matches 'b', so 2 + palindrome in "a" = 2 + 1 = 3
 * 
 * Case 2: If s[i] != s[j]
 *   → We can't use both characters in the same palindrome
 *   → Try excluding either left or right character
 *   → dp[i][j] = max(dp[i+1][j], dp[i][j-1])
 *   → Example: "cbbd" at "cb" → max(palindrome in "b", palindrome in "c") = 1
 * 
 * Fill Order (CRITICAL - Common Interview Mistake):
 * - Must fill by increasing LENGTH of substring
 * - Or fill diagonally (bottom-up, left-to-right)
 * - Why? dp[i][j] depends on dp[i+1][j-1], dp[i+1][j], dp[i][j-1]
 * 
 * ============================================================================
 */

class LongestPalindromicSubsequence {

    /**
     * APPROACH 1: DIRECT DYNAMIC PROGRAMMING (Interval DP)
     * 
     * Time Complexity: O(n²) where n = length of string
     * Space Complexity: O(n²) for the DP table
     * 
     * This is the OPTIMAL approach and shows deep DP understanding.
     * 
     * REASONING:
     * - We build solutions for small substrings and combine them
     * - If endpoints match, they contribute 2 to the length
     * - If they don't match, we try excluding each endpoint
     * 
     * VISUALIZATION for "bbbab":
     *     b  b  b  a  b
     *   +---------------
     * b | 1  2  3  3  4
     * b |    1  2  2  3
     * b |       1  1  3
     * a |          1  1
     * b |             1
     * 
     * Answer is dp[0][4] = 4
     */
    public static int longestPalindromeSubseq(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        
        int n = s.length();
        
        // dp[i][j] = length of longest palindromic subsequence in s[i..j]
        int[][] dp = new int[n][n];
        
        // Base case: every single character is a palindrome of length 1
        for (int i = 0; i < n; i++) {
            dp[i][i] = 1;
        }
        
        // Fill the table by increasing length of substring
        // len = 2 means we're looking at substrings of length 2, etc.
        for (int len = 2; len <= n; len++) {
            for (int i = 0; i <= n - len; i++) {
                int j = i + len - 1; // ending index of current substring
                
                // Case 1: Characters at both ends match
                if (s.charAt(i) == s.charAt(j)) {
                    // These characters can be part of palindrome
                    // Add 2 (for these two characters) + palindrome in middle
                    dp[i][j] = 2 + (len == 2 ? 0 : dp[i + 1][j - 1]);
                    
                    // Alternative way to write (handles len==2 automatically):
                    // dp[i][j] = 2 + dp[i + 1][j - 1];
                    // Works because dp[i+1][j-1] = 0 when j-1 < i+1
                } 
                // Case 2: Characters don't match
                else {
                    // Try excluding either the left or right character
                    // Take the maximum of both options
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j - 1]);
                }
            }
        }
        
        // Answer is the longest palindromic subsequence in entire string s[0..n-1]
        return dp[0][n - 1];
    }
    
    /**
     * APPROACH 2: USING LONGEST COMMON SUBSEQUENCE (LCS)
     * 
     * Time Complexity: O(n²)
     * Space Complexity: O(n²)
     * 
     * BRILLIANT INSIGHT:
     * - A palindrome reads the same forwards and backwards
     * - So, LCS(s, reverse(s)) gives us the longest palindromic subsequence!
     * 
     * REASONING:
     * - Characters that appear in both s and reverse(s) at matching positions
     *   form a subsequence that reads the same in both directions
     * - This is exactly what a palindrome is!
     * 
     * Example: s = "bbbab"
     *          reverse = "babbb"
     *          LCS will match 'b', 'b', 'b', 'b' → "bbbb"
     * 
     * INTERVIEW TIP:
     * - This is an excellent insight to mention
     * - Shows you can reduce problems to known solutions
     * - Easier to implement if you already know LCS
     */
    public static int longestPalindromeSubseqLCS(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        
        // Create reversed string
        String reversed = new StringBuilder(s).reverse().toString();
        
        // Find LCS between original and reversed
        return longestCommonSubsequence(s, reversed);
    }
    
    /**
     * Helper: Longest Common Subsequence (Standard DP)
     * 
     * dp[i][j] = LCS length of s1[0..i-1] and s2[0..j-1]
     */
    private static int longestCommonSubsequence(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = 1 + dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        
        return dp[m][n];
    }
    
    /**
     * APPROACH 3: SPACE-OPTIMIZED DP (1D Array)
     * 
     * Time Complexity: O(n²)
     * Space Complexity: O(n) - significant improvement!
     * 
     * OPTIMIZATION REASONING:
     * - Notice dp[i][j] only depends on:
     *   1. dp[i+1][j-1] (diagonal)
     *   2. dp[i+1][j] (below)
     *   3. dp[i][j-1] (left)
     * - We can use a single array if we're careful about update order
     * 
     * CAVEAT:
     * - More complex to implement correctly
     * - Need to save diagonal value before overwriting
     * 
     * INTERVIEW TIP:
     * - Mention this optimization after solving with 2D array
     * - Shows you understand space-time tradeoffs
     */
    public static int longestPalindromeSubseqOptimized(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        
        int n = s.length();
        int[] dp = new int[n];
        
        // Process by increasing substring length
        for (int len = 1; len <= n; len++) {
            int prev = 0; // Stores dp[i+1][j-1] value
            
            for (int i = n - len; i >= 0; i--) {
                int j = i + len - 1;
                int temp = dp[j]; // Save current value before overwriting
                
                if (len == 1) {
                    dp[j] = 1;
                } else if (s.charAt(i) == s.charAt(j)) {
                    dp[j] = 2 + prev;
                } else {
                    dp[j] = Math.max(dp[j], dp[j - 1]);
                }
                
                prev = temp; // For next iteration, this becomes dp[i+1][j-1]
            }
        }
        
        return dp[n - 1];
    }
    
    /**
     * APPROACH 4: RECURSIVE WITH MEMOIZATION (Top-Down DP)
     * 
     * Time Complexity: O(n²)
     * Space Complexity: O(n²) for memo + O(n) recursion stack
     * 
     * REASONING:
     * - Sometimes easier to think recursively first
     * - Add memoization to avoid recomputation
     * - Same logic as bottom-up but different implementation style
     * 
     * INTERVIEW TIP:
     * - If you think of recursive solution first, that's fine!
     * - Add memoization immediately to optimize
     * - Mention you can convert to bottom-up if needed
     */
    public static int longestPalindromeSubseqRecursive(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        
        int n = s.length();
        Integer[][] memo = new Integer[n][n];
        return lpHelper(s, 0, n - 1, memo);
    }
    
    private static int lpHelper(String s, int i, int j, Integer[][] memo) {
        // Base case: single character
        if (i == j) {
            return 1;
        }
        
        // Base case: empty string (when i > j)
        if (i > j) {
            return 0;
        }
        
        // Check memo
        if (memo[i][j] != null) {
            return memo[i][j];
        }
        
        int result;
        
        // If characters match, include both in palindrome
        if (s.charAt(i) == s.charAt(j)) {
            result = 2 + lpHelper(s, i + 1, j - 1, memo);
        } 
        // If they don't match, try excluding either end
        else {
            result = Math.max(
                lpHelper(s, i + 1, j, memo),
                lpHelper(s, i, j - 1, memo)
            );
        }
        
        memo[i][j] = result;
        return result;
    }
    
    /**
     * BONUS: GET THE ACTUAL PALINDROMIC SUBSEQUENCE (Not just length)
     * 
     * REASONING:
     * - Build the DP table as usual
     * - Backtrack through the table to construct the actual palindrome
     * - Follow the same logic we used to fill the table
     * 
     * INTERVIEW TIP:
     * - Ask if they want just length or actual subsequence
     * - Shows you're thinking about follow-up questions
     */
    public static String getLongestPalindromeSubseq(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        
        int n = s.length();
        int[][] dp = new int[n][n];
        
        // Fill DP table (same as before)
        for (int i = 0; i < n; i++) {
            dp[i][i] = 1;
        }
        
        for (int len = 2; len <= n; len++) {
            for (int i = 0; i <= n - len; i++) {
                int j = i + len - 1;
                if (s.charAt(i) == s.charAt(j)) {
                    dp[i][j] = 2 + (len == 2 ? 0 : dp[i + 1][j - 1]);
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j - 1]);
                }
            }
        }
        
        // Backtrack to build the palindrome
        return backtrack(s, dp, 0, n - 1);
    }
    
    private static String backtrack(String s, int[][] dp, int i, int j) {
        if (i > j) {
            return "";
        }
        if (i == j) {
            return String.valueOf(s.charAt(i));
        }
        
        // If characters match, they're part of the palindrome
        if (s.charAt(i) == s.charAt(j)) {
            return s.charAt(i) + backtrack(s, dp, i + 1, j - 1) + s.charAt(j);
        }
        
        // Move in direction of larger value
        if (dp[i + 1][j] > dp[i][j - 1]) {
            return backtrack(s, dp, i + 1, j);
        } else {
            return backtrack(s, dp, i, j - 1);
        }
    }
    
    /**
     * ========================================================================
     * TEST CASES - COMPREHENSIVE COVERAGE
     * ========================================================================
     */
    public static void main(String[] args) {
        System.out.println("=".repeat(70));
        System.out.println("LONGEST PALINDROMIC SUBSEQUENCE - TEST CASES");
        System.out.println("=".repeat(70));
        
        // Test Case 1: Multiple b's with one a
        System.out.println("\nTest 1: Multiple same characters");
        String s1 = "bbbab";
        System.out.println("Input: " + s1);
        System.out.println("Expected: 4 (bbbb)");
        System.out.println("Direct DP: " + longestPalindromeSubseq(s1));
        System.out.println("LCS Approach: " + longestPalindromeSubseqLCS(s1));
        System.out.println("Recursive: " + longestPalindromeSubseqRecursive(s1));
        System.out.println("Optimized: " + longestPalindromeSubseqOptimized(s1));
        System.out.println("Actual palindrome: " + getLongestPalindromeSubseq(s1));
        
        // Test Case 2: No repeating characters except one pair
        System.out.println("\nTest 2: Minimal repetition");
        String s2 = "cbbd";
        System.out.println("Input: " + s2);
        System.out.println("Expected: 2 (bb)");
        System.out.println("Direct DP: " + longestPalindromeSubseq(s2));
        System.out.println("Actual palindrome: " + getLongestPalindromeSubseq(s2));
        
        // Test Case 3: Already a palindrome
        System.out.println("\nTest 3: Already a palindrome");
        String s3 = "racecar";
        System.out.println("Input: " + s3);
        System.out.println("Expected: 7 (entire string)");
        System.out.println("Direct DP: " + longestPalindromeSubseq(s3));
        System.out.println("Actual palindrome: " + getLongestPalindromeSubseq(s3));
        
        // Test Case 4: Single character
        System.out.println("\nTest 4: Single character");
        String s4 = "a";
        System.out.println("Input: " + s4);
        System.out.println("Expected: 1");
        System.out.println("Direct DP: " + longestPalindromeSubseq(s4));
        
        // Test Case 5: Two identical characters
        System.out.println("\nTest 5: Two identical characters");
        String s5 = "aa";
        System.out.println("Input: " + s5);
        System.out.println("Expected: 2");
        System.out.println("Direct DP: " + longestPalindromeSubseq(s5));
        
        // Test Case 6: Two different characters
        System.out.println("\nTest 6: Two different characters");
        String s6 = "ab";
        System.out.println("Input: " + s6);
        System.out.println("Expected: 1 (either 'a' or 'b')");
        System.out.println("Direct DP: " + longestPalindromeSubseq(s6));
        
        // Test Case 7: Complex example
        System.out.println("\nTest 7: Complex example");
        String s7 = "agbdba";
        System.out.println("Input: " + s7);
        System.out.println("Expected: 5 (abdba or adbda)");
        System.out.println("Direct DP: " + longestPalindromeSubseq(s7));
        System.out.println("Actual palindrome: " + getLongestPalindromeSubseq(s7));
        
        // Test Case 8: All different characters
        System.out.println("\nTest 8: All different characters");
        String s8 = "abcdef";
        System.out.println("Input: " + s8);
        System.out.println("Expected: 1 (any single character)");
        System.out.println("Direct DP: " + longestPalindromeSubseq(s8));
        
        // Test Case 9: Alternating pattern
        System.out.println("\nTest 9: Alternating pattern");
        String s9 = "ababab";
        System.out.println("Input: " + s9);
        System.out.println("Expected: 5 (ababa or babab)");
        System.out.println("Direct DP: " + longestPalindromeSubseq(s9));
        System.out.println("Actual palindrome: " + getLongestPalindromeSubseq(s9));
        
        // Test Case 10: Long palindrome in middle
        System.out.println("\nTest 10: Palindrome in middle");
        String s10 = "xyzabcdcbastu";
        System.out.println("Input: " + s10);
        System.out.println("Expected: 7 (abcdcba)");
        System.out.println("Direct DP: " + longestPalindromeSubseq(s10));
        System.out.println("Actual palindrome: " + getLongestPalindromeSubseq(s10));
        
        System.out.println("\n" + "=".repeat(70));
    }
}

/**
 * ============================================================================
 * INTERVIEW STRATEGY AND TALKING POINTS
 * ============================================================================
 * 
 * 1. CLARIFY THE PROBLEM (CRITICAL - 30 seconds):
 *    "Just to confirm - we're looking for a SUBSEQUENCE, not a SUBSTRING, right?"
 *    "So we can skip characters as long as we maintain order?"
 *    "Should I return the length or the actual palindrome?"
 *    
 *    WHY THIS MATTERS: Many candidates confuse subsequence with substring
 *    and waste 20 minutes solving the wrong problem!
 * 
 * 2. PROVIDE EXAMPLES (1-2 minutes):
 *    "Let me work through an example to make sure I understand..."
 *    s = "bbbab"
 *    - We can form "bbbb" by skipping 'a' → length 4
 *    - We can form "bab" → length 3
 *    - We can form "aba" → length 3
 *    "So the answer is 4."
 * 
 * 3. DISCUSS APPROACHES (2-3 minutes):
 *    
 *    Approach A - LCS Method:
 *    "One interesting insight: if we reverse the string and find the LCS
 *     between original and reversed, that gives us the longest palindromic
 *     subsequence! Because a palindrome reads the same both ways."
 *    
 *    Pros: Reuses known algorithm, easy to understand
 *    Cons: Requires implementing LCS first
 *    
 *    Approach B - Direct DP:
 *    "We can also solve it directly with interval DP:
 *     - If endpoints match: include both, solve for middle
 *     - If they don't match: try excluding each endpoint"
 *    
 *    Pros: More efficient, shows deeper understanding
 *    Cons: Slightly more complex logic
 *    
 *    "I'll go with the direct DP approach as it's more efficient."
 * 
 * 4. DESIGN THE SOLUTION (2-3 minutes):
 *    "Let me define my DP state:
 *     dp[i][j] = length of longest palindromic subsequence in s[i...j]
 *     
 *     Base case: dp[i][i] = 1 (single character)
 *     
 *     Recurrence:
 *     - If s[i] == s[j]: dp[i][j] = 2 + dp[i+1][j-1]
 *     - Else: dp[i][j] = max(dp[i+1][j], dp[i][j-1])
 *     
 *     Fill order: By increasing substring length"
 *    
 *    [Draw a small DP table on whiteboard to visualize]
 * 
 * 5. WRITE THE CODE (5-7 minutes):
 *    - Write clean, commented code
 *    - Explain as you write
 *    - Handle edge cases (empty string, single char)
 * 
 * 6. TEST YOUR CODE (2-3 minutes):
 *    "Let me trace through with 'bbbab':
 *     - dp[0][0] through dp[4][4] = 1
 *     - dp[0][1]: 'b','b' match → 2 + 0 = 2
 *     - ... [trace key values]
 *     - dp[0][4] = 4 ✓"
 * 
 * 7. ANALYZE COMPLEXITY (30 seconds):
 *    "Time: O(n²) - we fill n² cells, each in O(1)
 *     Space: O(n²) for the DP table
 *     Could optimize space to O(n) but adds complexity"
 * 
 * 8. DISCUSS FOLLOW-UPS (If time permits):
 *    - "We could return the actual palindrome with backtracking"
 *    - "Space optimization is possible using rolling array"
 *    - "Related: Longest Palindromic Substring uses different approach"
 * 
 * ============================================================================
 * COMMON MISTAKES TO AVOID
 * ============================================================================
 * 
 * 1. **Confusing Subsequence with Substring** (MOST COMMON!)
 *    - Subsequence: Can skip characters (this problem)
 *    - Substring: Must be contiguous (different problem)
 * 
 * 2. **Wrong Fill Order**
 *    - Must fill by increasing length or diagonally
 *    - Filling row-by-row left-to-right WON'T WORK
 *    - dp[i][j] depends on dp[i+1][...] which must be computed first
 * 
 * 3. **Off-by-One Errors**
 *    - When len=2, dp[i+1][j-1] might be invalid
 *    - Handle this case separately or ensure it's 0
 * 
 * 4. **Forgetting Base Cases**
 *    - Single character must return 1
 *    - Empty range (i > j) must return 0
 * 
 * 5. **Not Checking for NULL/Empty Input**
 *    - Always handle edge cases first
 * 
 * 6. **Confusing with Longest Palindromic Substring**
 *    - That problem requires contiguous characters
 *    - Uses completely different approach (expand around center or Manacher's)
 * 
 * ============================================================================
 * KEY INSIGHTS FOR MASTERY
 * ============================================================================
 * 
 * 1. **Pattern Recognition**:
 *    - "Longest" + "Subsequence" → Think DP
 *    - Palindrome → Consider reversing or endpoints
 * 
 * 2. **State Design**:
 *    - Interval DP works well for palindromes
 *    - dp[i][j] represents a RANGE, not just a single position
 * 
 * 3. **Two Approaches Connection**:
 *    - LCS(s, reverse(s)) = Longest Palindromic Subsequence
 *    - This is a beautiful insight worth remembering!
 * 
 * 4. **Comparison with Related Problems**:
 *    - LCS: Compare two strings, can skip in both
 *    - LPS: One string, but palindrome constraint
 *    - Longest Palindromic Substring: Must be contiguous
 * 
 * 5. **Optimization Opportunities**:
 *    - Space: Can reduce to O(n) with careful tracking
 *    - Time: O(n²) is optimal, can't do better for this approach
 * 
 * ============================================================================
 * TIME ALLOCATION (Total: ~20-25 minutes)
 * ============================================================================
 * 
 * - Understanding/Clarification: 1-2 min
 * - Examples: 1-2 min
 * - Approach Discussion: 2-3 min
 * - Solution Design: 2-3 min
 * - Coding: 7-10 min
 * - Testing: 2-3 min
 * - Complexity Analysis: 1 min
 * - Follow-ups: 2-3 min (if time)
 * 
 * ============================================================================
 */
