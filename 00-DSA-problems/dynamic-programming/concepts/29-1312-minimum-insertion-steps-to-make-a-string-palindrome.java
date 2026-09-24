/**
 * MINIMUM INSERTION STEPS TO MAKE STRING PALINDROME - COMPLETE ANALYSIS
 * 
 * ============================================================================
 * PROBLEM UNDERSTANDING
 * ============================================================================
 * 
 * Given: A string s
 * Allowed Operation: Insert any character at any position
 * Find: Minimum number of insertions to make s a palindrome
 * 
 * KEY EXAMPLES:
 * 
 * Example 1: s = "zzazz"
 * - Already a palindrome → 0 insertions needed
 * 
 * Example 2: s = "mbadm"
 * - Insert 'a' and 'd': "mbaddam" or "mbdadbm" → 2 insertions
 * 
 * Example 3: s = "leetcode"
 * - One possible result: "leetcodocteel" → 5 insertions
 * 
 * Example 4: s = "abc"
 * - Options: "abcba" (insert 'b','a'), "cbabc" (insert 'b','c') → 2 insertions
 * 
 * ============================================================================
 * THE KEY INSIGHT (THIS IS THE AHA MOMENT!)
 * ============================================================================
 * 
 * BRILLIANT OBSERVATION:
 * Minimum insertions = n - (Length of Longest Palindromic Subsequence)
 * 
 * WHY THIS WORKS:
 * 1. The Longest Palindromic Subsequence (LPS) represents characters that
 *    are ALREADY in palindromic order - they don't need any insertions
 * 
 * 2. All OTHER characters (n - LPS) need to be "mirrored" by inserting
 *    their counterparts on the opposite side
 * 
 * 3. Example: s = "abc", LPS = 1 (any single character)
 *    - We have 3 characters, 1 is already "palindromic"
 *    - So we need 3 - 1 = 2 insertions
 *    - Result: "abcba" ('b' and 'a' inserted)
 * 
 * DEEPER UNDERSTANDING:
 * - If a character is part of LPS, it's already positioned correctly
 * - If not, we need to insert its mirror to make it palindromic
 * - LPS tells us the "backbone" of our final palindrome
 * - Everything else needs to be duplicated
 * 
 * ============================================================================
 * INTERVIEW APPROACH - STEP BY STEP THINKING
 * ============================================================================
 * 
 * STEP 1: Start with Simple Examples (Build Intuition)
 * 
 * s = "a" → Already palindrome → 0 insertions
 * s = "ab" → Need "aba" or "bab" → 1 insertion
 * s = "abc" → Need "abcba" or "cbabc" → 2 insertions
 * 
 * Pattern: For completely mismatched strings, we need (n-1) insertions
 * 
 * STEP 2: Think About What Stays
 * "What if some characters are already in good positions?"
 * 
 * s = "aba" → Already palindrome, LPS = 3 → 0 insertions needed
 * s = "abac" → "aba" is palindromic, just 'c' is out → 1 insertion
 * 
 * Insight: Characters forming LPS don't need mirroring!
 * 
 * STEP 3: Connect to Previous Problem
 * "Wait, I just solved Longest Palindromic Subsequence!"
 * "Can I reuse that solution?"
 * 
 * YES! min_insertions = n - LPS(s)
 * 
 * STEP 4: Alternative Direct DP Approach
 * We can also solve directly using interval DP:
 * - dp[i][j] = min insertions to make s[i...j] palindrome
 * - If s[i] == s[j]: dp[i][j] = dp[i+1][j-1]
 * - Else: dp[i][j] = 1 + min(dp[i+1][j], dp[i][j-1])
 * 
 * Both approaches work, but LPS approach shows problem-solving skills!
 * 
 * ============================================================================
 * DETAILED REASONING FOR LPS APPROACH
 * ============================================================================
 * 
 * Why does "n - LPS" give us minimum insertions?
 * 
 * PROOF BY CONSTRUCTION:
 * 
 * 1. Let LPS have length L
 * 2. These L characters are already in palindromic arrangement
 * 3. Remaining (n - L) characters are NOT part of this palindrome
 * 4. For each of these (n - L) characters, we need to insert a mirror
 * 5. Total insertions = n - L
 * 
 * EXAMPLE WALKTHROUGH: s = "mbadm"
 * 
 * Step 1: Find LPS
 * - Palindromic subsequences: "m", "b", "a", "d", "mam", "mdm", "bab", "dad"
 * - Longest: "mam" or "mdm" (length 3)
 * - LPS = 3
 * 
 * Step 2: Calculate insertions
 * - Total chars: 5
 * - Already palindromic: 3
 * - Need to mirror: 5 - 3 = 2
 * 
 * Step 3: Construct result
 * - Using "mam" as backbone: m_ba_d_m
 * - Insert 'd' before first 'm': d m_ba_d_m (wait, this doesn't work)
 * - Actually: m b_a_d m → Insert 'd' and 'b': m b d a d b m
 * - Or simpler: m b a d d a m → "mbaddam" (2 insertions)
 * 
 * ============================================================================
 */

class MinimumInsertionStepsPalindrome {

    /**
     * APPROACH 1: USING LONGEST PALINDROMIC SUBSEQUENCE (Most Elegant!)
     * 
     * Time Complexity: O(n²) for computing LPS
     * Space Complexity: O(n²) for DP table
     * 
     * This is the BEST approach to present in an interview because:
     * 1. It reuses a known algorithm (LPS)
     * 2. Shows you can connect different problems
     * 3. The insight is elegant and impressive
     * 
     * FORMULA: min_insertions = n - LPS(s)
     * 
     * INTERVIEW TIP:
     * - Start by explaining the insight
     * - Show you solved LPS in the previous problem
     * - Implement LPS if interviewer asks, or use it as a helper
     */
    public static int minInsertions(String s) {
        if (s == null || s.length() <= 1) {
            return 0; // Empty or single char is already palindrome
        }
        
        int n = s.length();
        
        // Find length of Longest Palindromic Subsequence
        int lpsLength = longestPalindromeSubseq(s);
        
        // Characters not in LPS need to be mirrored
        return n - lpsLength;
    }
    
    /**
     * Helper: Longest Palindromic Subsequence
     * (Copied from previous problem - shows code reuse!)
     */
    private static int longestPalindromeSubseq(String s) {
        int n = s.length();
        int[][] dp = new int[n][n];
        
        // Base case: single character
        for (int i = 0; i < n; i++) {
            dp[i][i] = 1;
        }
        
        // Fill by increasing substring length
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
        
        return dp[0][n - 1];
    }
    
    /**
     * APPROACH 2: DIRECT DYNAMIC PROGRAMMING (Interval DP)
     * 
     * Time Complexity: O(n²)
     * Space Complexity: O(n²)
     * 
     * This solves the problem directly without using LPS.
     * 
     * STATE DEFINITION:
     * dp[i][j] = minimum insertions to make substring s[i...j] a palindrome
     * 
     * BASE CASES:
     * - dp[i][i] = 0 (single character is already palindrome)
     * - dp[i][i+1] = 0 if s[i]==s[i+1], else 1
     * 
     * RECURRENCE RELATION:
     * 
     * Case 1: If s[i] == s[j]
     *   → These characters can be endpoints of palindrome
     *   → No insertion needed for them
     *   → dp[i][j] = dp[i+1][j-1]
     * 
     * Case 2: If s[i] != s[j]
     *   → Need to insert one character to match
     *   → Option A: Insert s[j] at beginning → 1 + dp[i][j-1]
     *   → Option B: Insert s[i] at end → 1 + dp[i+1][j]
     *   → Take minimum: dp[i][j] = 1 + min(dp[i+1][j], dp[i][j-1])
     * 
     * REASONING:
     * - When endpoints match, they form palindrome boundaries naturally
     * - When they don't, we need to insert the missing character
     * - We choose the option that requires fewer total insertions
     */
    public static int minInsertionsDirect(String s) {
        if (s == null || s.length() <= 1) {
            return 0;
        }
        
        int n = s.length();
        int[][] dp = new int[n][n];
        
        // Base case: single character needs 0 insertions
        // (already initialized to 0)
        
        // Fill by increasing substring length
        for (int len = 2; len <= n; len++) {
            for (int i = 0; i <= n - len; i++) {
                int j = i + len - 1;
                
                if (s.charAt(i) == s.charAt(j)) {
                    // Characters match - no insertion needed for them
                    dp[i][j] = (len == 2) ? 0 : dp[i + 1][j - 1];
                } else {
                    // Characters don't match - need 1 insertion
                    // Choose option with fewer total insertions
                    dp[i][j] = 1 + Math.min(dp[i + 1][j], dp[i][j - 1]);
                }
            }
        }
        
        return dp[0][n - 1];
    }
    
    /**
     * APPROACH 3: RECURSIVE WITH MEMOIZATION (Top-Down)
     * 
     * Time Complexity: O(n²)
     * Space Complexity: O(n²) + O(n) recursion stack
     * 
     * Sometimes it's easier to think recursively first.
     * This approach uses the same logic as direct DP but top-down.
     * 
     * INTERVIEW TIP:
     * - If recursive solution comes to mind first, go with it!
     * - Add memoization immediately
     * - Can convert to bottom-up if interviewer prefers
     */
    public static int minInsertionsRecursive(String s) {
        if (s == null || s.length() <= 1) {
            return 0;
        }
        
        int n = s.length();
        Integer[][] memo = new Integer[n][n];
        return minInsertHelper(s, 0, n - 1, memo);
    }
    
    private static int minInsertHelper(String s, int i, int j, Integer[][] memo) {
        // Base case: empty or single character
        if (i >= j) {
            return 0;
        }
        
        // Check memo
        if (memo[i][j] != null) {
            return memo[i][j];
        }
        
        int result;
        
        if (s.charAt(i) == s.charAt(j)) {
            // Characters match - solve for middle
            result = minInsertHelper(s, i + 1, j - 1, memo);
        } else {
            // Characters don't match - try both options
            int insertAtEnd = 1 + minInsertHelper(s, i + 1, j, memo);
            int insertAtBegin = 1 + minInsertHelper(s, i, j - 1, memo);
            result = Math.min(insertAtEnd, insertAtBegin);
        }
        
        memo[i][j] = result;
        return result;
    }
    
    /**
     * APPROACH 4: USING LCS (Another Elegant Insight!)
     * 
     * Time Complexity: O(n²)
     * Space Complexity: O(n²)
     * 
     * ANOTHER BRILLIANT CONNECTION:
     * 
     * min_insertions = n - LCS(s, reverse(s))
     * 
     * WHY?
     * - LCS(s, reverse(s)) gives us the longest palindromic subsequence!
     * - This is because palindromes read the same forwards and backwards
     * - So this reduces to the same formula: n - LPS
     * 
     * INTERVIEW TIP:
     * - Mention this as an alternative insight
     * - Shows you understand the connection between LCS and LPS
     * - Demonstrates deep problem-solving skills
     */
    public static int minInsertionsLCS(String s) {
        if (s == null || s.length() <= 1) {
            return 0;
        }
        
        String reversed = new StringBuilder(s).reverse().toString();
        int lcsLength = longestCommonSubsequence(s, reversed);
        
        return s.length() - lcsLength;
    }
    
    /**
     * Helper: Longest Common Subsequence
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
     * APPROACH 5: SPACE-OPTIMIZED VERSION
     * 
     * Time Complexity: O(n²)
     * Space Complexity: O(n)
     * 
     * We can optimize space by only keeping two rows of the DP table.
     * 
     * INTERVIEW TIP:
     * - Mention this after solving with 2D array
     * - Shows awareness of space-time tradeoffs
     */
    public static int minInsertionsOptimized(String s) {
        if (s == null || s.length() <= 1) {
            return 0;
        }
        
        int n = s.length();
        int[] prev = new int[n];
        int[] curr = new int[n];
        
        for (int len = 2; len <= n; len++) {
            for (int i = 0; i <= n - len; i++) {
                int j = i + len - 1;
                
                if (s.charAt(i) == s.charAt(j)) {
                    curr[j] = (len == 2) ? 0 : prev[j - 1];
                } else {
                    curr[j] = 1 + Math.min(curr[j - 1], prev[j]);
                }
            }
            
            // Swap arrays
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        
        return prev[n - 1];
    }
    
    /**
     * BONUS: CONSTRUCT THE ACTUAL PALINDROME (Not just count insertions)
     * 
     * This shows HOW to insert characters, not just how many.
     * 
     * APPROACH:
     * - Build DP table as usual
     * - Backtrack to determine where insertions were made
     * - Construct the resulting palindrome string
     * 
     * INTERVIEW TIP:
     * - Ask if they want the actual palindrome or just the count
     * - Shows you're thinking about practical applications
     */
    public static String constructPalindrome(String s) {
        if (s == null || s.length() <= 1) {
            return s;
        }
        
        int n = s.length();
        int[][] dp = new int[n][n];
        
        // Fill DP table
        for (int len = 2; len <= n; len++) {
            for (int i = 0; i <= n - len; i++) {
                int j = i + len - 1;
                
                if (s.charAt(i) == s.charAt(j)) {
                    dp[i][j] = (len == 2) ? 0 : dp[i + 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i + 1][j], dp[i][j - 1]);
                }
            }
        }
        
        // Backtrack to build palindrome
        return buildPalindrome(s, dp, 0, n - 1);
    }
    
    private static String buildPalindrome(String s, int[][] dp, int i, int j) {
        // Base cases
        if (i > j) {
            return "";
        }
        if (i == j) {
            return String.valueOf(s.charAt(i));
        }
        
        // If characters match, they're both in final palindrome
        if (s.charAt(i) == s.charAt(j)) {
            return s.charAt(i) + buildPalindrome(s, dp, i + 1, j - 1) + s.charAt(j);
        }
        
        // If they don't match, we inserted one
        // Follow the path that gave us minimum insertions
        if (dp[i][j] == 1 + dp[i + 1][j]) {
            // We inserted s[i] at the end
            return s.charAt(i) + buildPalindrome(s, dp, i + 1, j) + s.charAt(i);
        } else {
            // We inserted s[j] at the beginning
            return s.charAt(j) + buildPalindrome(s, dp, i, j - 1) + s.charAt(j);
        }
    }
    
    /**
     * ========================================================================
     * COMPREHENSIVE TEST CASES
     * ========================================================================
     */
    public static void main(String[] args) {
        System.out.println("=".repeat(75));
        System.out.println("MINIMUM INSERTION STEPS TO MAKE STRING PALINDROME");
        System.out.println("=".repeat(75));
        
        // Test Case 1: Already a palindrome
        System.out.println("\nTest 1: Already a palindrome");
        String s1 = "zzazz";
        System.out.println("Input: \"" + s1 + "\"");
        System.out.println("Expected: 0");
        System.out.println("LPS Approach: " + minInsertions(s1));
        System.out.println("Direct DP: " + minInsertionsDirect(s1));
        System.out.println("Recursive: " + minInsertionsRecursive(s1));
        System.out.println("LCS Approach: " + minInsertionsLCS(s1));
        System.out.println("Result: \"" + constructPalindrome(s1) + "\"");
        
        // Test Case 2: Need some insertions
        System.out.println("\nTest 2: Need insertions");
        String s2 = "mbadm";
        System.out.println("Input: \"" + s2 + "\"");
        System.out.println("Expected: 2");
        System.out.println("LPS Approach: " + minInsertions(s2));
        System.out.println("Direct DP: " + minInsertionsDirect(s2));
        System.out.println("Result: \"" + constructPalindrome(s2) + "\"");
        
        // Test Case 3: Longer string
        System.out.println("\nTest 3: Longer string");
        String s3 = "leetcode";
        System.out.println("Input: \"" + s3 + "\"");
        System.out.println("Expected: 5");
        System.out.println("LPS Approach: " + minInsertions(s3));
        System.out.println("Direct DP: " + minInsertionsDirect(s3));
        System.out.println("Result: \"" + constructPalindrome(s3) + "\"");
        
        // Test Case 4: Simple case
        System.out.println("\nTest 4: Three different characters");
        String s4 = "abc";
        System.out.println("Input: \"" + s4 + "\"");
        System.out.println("Expected: 2");
        System.out.println("LPS Approach: " + minInsertions(s4));
        System.out.println("Result: \"" + constructPalindrome(s4) + "\"");
        
        // Test Case 5: Two characters - match
        System.out.println("\nTest 5: Two matching characters");
        String s5 = "aa";
        System.out.println("Input: \"" + s5 + "\"");
        System.out.println("Expected: 0");
        System.out.println("LPS Approach: " + minInsertions(s5));
        
        // Test Case 6: Two characters - no match
        System.out.println("\nTest 6: Two different characters");
        String s6 = "ab";
        System.out.println("Input: \"" + s6 + "\"");
        System.out.println("Expected: 1");
        System.out.println("LPS Approach: " + minInsertions(s6));
        System.out.println("Result: \"" + constructPalindrome(s6) + "\"");
        
        // Test Case 7: Single character
        System.out.println("\nTest 7: Single character");
        String s7 = "a";
        System.out.println("Input: \"" + s7 + "\"");
        System.out.println("Expected: 0");
        System.out.println("LPS Approach: " + minInsertions(s7));
        
        // Test Case 8: All different characters
        System.out.println("\nTest 8: All different characters");
        String s8 = "abcde";
        System.out.println("Input: \"" + s8 + "\"");
        System.out.println("Expected: 4 (n-1 for all different)");
        System.out.println("LPS Approach: " + minInsertions(s8));
        System.out.println("Result: \"" + constructPalindrome(s8) + "\"");
        
        // Test Case 9: Alternating pattern
        System.out.println("\nTest 9: Alternating pattern");
        String s9 = "abab";
        System.out.println("Input: \"" + s9 + "\"");
        System.out.println("Expected: 1 (ababa or babab)");
        System.out.println("LPS Approach: " + minInsertions(s9));
        System.out.println("Result: \"" + constructPalindrome(s9) + "\"");
        
        // Test Case 10: Complex example
        System.out.println("\nTest 10: Complex example");
        String s10 = "race";
        System.out.println("Input: \"" + s10 + "\"");
        System.out.println("Expected: 3");
        System.out.println("LPS Approach: " + minInsertions(s10));
        System.out.println("Result: \"" + constructPalindrome(s10) + "\"");
        
        // Test Case 11: Performance comparison
        System.out.println("\nTest 11: All approaches verification");
        String s11 = "programming";
        System.out.println("Input: \"" + s11 + "\"");
        System.out.println("LPS Approach: " + minInsertions(s11));
        System.out.println("Direct DP: " + minInsertionsDirect(s11));
        System.out.println("Recursive: " + minInsertionsRecursive(s11));
        System.out.println("LCS Approach: " + minInsertionsLCS(s11));
        System.out.println("Optimized: " + minInsertionsOptimized(s11));
        System.out.println("Result: \"" + constructPalindrome(s11) + "\"");
        
        System.out.println("\n" + "=".repeat(75));
    }
}

/**
 * ============================================================================
 * INTERVIEW STRATEGY - COMPLETE WALKTHROUGH
 * ============================================================================
 * 
 * PHASE 1: PROBLEM CLARIFICATION (1-2 minutes)
 * ============================================
 * 
 * YOU: "Let me make sure I understand the problem correctly:"
 *      "1. We can insert any character at any position?"
 *      "2. We want the MINIMUM number of insertions?"
 *      "3. The final string must be a palindrome?"
 *      "4. Should I return just the count or the actual palindrome?"
 * 
 * INTERVIEWER: "Yes to all. Just return the count."
 * 
 * PHASE 2: EXAMPLES AND PATTERN RECOGNITION (2-3 minutes)
 * =======================================================
 * 
 * YOU: "Let me work through some examples to understand the pattern..."
 * 
 * Example 1: s = "a"
 * - Already palindrome → 0 insertions
 * 
 * Example 2: s = "ab"
 * - Need "aba" or "bab" → 1 insertion
 * - We insert the missing character
 * 
 * Example 3: s = "abc"
 * - Need "abcba" or "cbabc" → 2 insertions
 * - For n characters all different, need n-1 insertions
 * 
 * Example 4: s = "aba"
 * - Already palindrome → 0 insertions
 * 
 * YOU: "I notice that if some characters are already forming a palindromic
 *       pattern, they don't need insertions. Only the 'extra' characters do."
 * 
 * PHASE 3: KEY INSIGHT (THE AHA MOMENT!) (2-3 minutes)
 * ====================================================
 * 
 * YOU: "Here's a key insight: This problem is deeply connected to the
 *       Longest Palindromic Subsequence (LPS) problem!"
 * 
 *      "If I find the LPS, those characters are already in good positions.
 *       They form the 'backbone' of our final palindrome."
 * 
 *      "All OTHER characters (n - LPS) need to be mirrored by insertions."
 * 
 *      "So: min_insertions = n - LPS(s)"
 * 
 * [Draw on whiteboard]:
 * 
 * s = "mbadm" (n = 5)
 * LPS = "mdm" or "mam" (length = 3)
 * Insertions needed = 5 - 3 = 2
 * 
 * Result: "mbdadbm" or "mbaddam"
 * 
 * INTERVIEWER: "Interesting! Can you prove why this works?"
 * 
 * YOU: "Sure! The LPS represents characters that are ALREADY in palindromic
 *       order. The remaining (n - LPS) characters aren't part of this
 *       palindrome, so we need to insert their mirrors to make the entire
 *       string a palindrome."
 * 
 * PHASE 4: APPROACH DISCUSSION (2-3 minutes)
 * ==========================================
 * 
 * YOU: "I see two main approaches:"
 * 
 * Approach 1 - Use LPS:
 * - Compute LPS using the algorithm from previous problem
 * - Return n - LPS
 * - Time: O(n²), Space: O(n²)
 * - Pros: Elegant, reuses known solution
 * 
 * Approach 2 - Direct DP:
 * - dp[i][j] = min insertions for s[i...j]
 * - If s[i] == s[j]: dp[i][j] = dp[i+1][j-1]
 * - Else: dp[i][j] = 1 + min(dp[i+1][j], dp[i][j-1])
 * - Time: O(n²), Space: O(n²)
 * - Pros: Direct solution, same complexity
 * 
 * YOU: "I'll use the LPS approach since it's more elegant and shows
 *       problem-solving skills. Would you like me to implement LPS from
 *       scratch or can I use it as a helper function?"
 * 
 * INTERVIEWER: "Implement it as a helper."
 * 
 * PHASE 5: IMPLEMENTATION (7-10 minutes)
 * ======================================
 * 
 * [Write clean code with comments]
 * [Explain key parts as you write]
 * 
 * public int minInsertions(String s) {
 *     if (s == null || s.length() <= 1) return 0;
 *     return s.length() - longestPalindromeSubseq(s);
 * }
 * 
 * private int longestPalindromeSubseq(String s) {
 *     // [Implement LPS using DP]
 * }
 * 
 * PHASE 6: TESTING (2-3 minutes)
 * ==============================
 * 
 * YOU: "Let me trace through with 'mbadm':"
 * 
 * LPS computation:
 * - dp[0][0] through dp[4][4] = 1
 * - dp[0][4] for "mbadm": 
 *   - 'm' == 'm' → 2 + dp[1][3] for "bad"
 *   - "bad": 'b' != 'd' → max(dp[2][3], dp[1][2])
 *   - Eventually get LPS = 3
 * 
 * Result: 5 - 3 = 2 ✓
 * 
 * Edge cases:
 * - Empty string: 0
 * - Single char: 0
 * - Already palindrome: 0
 * - All different: n-1
 * 
 * PHASE 7: COMPLEXITY ANALYSIS (1 minute)
 * =======================================
 * 
 * YOU: "Time Complexity: O(n²) for computing LPS
 */
