/*
 * PROBLEM: Shortest Common Supersequence (SCS)
 * 
 * INTERVIEW APPROACH - How to Think Through This Problem:
 * ========================================================
 * 
 * 1. UNDERSTAND THE PROBLEM:
 *    - Need a string that contains BOTH str1 and str2 as subsequences
 *    - Subsequence: can skip characters but maintain order
 *    - Goal: Find the SHORTEST such string
 * 
 * 2. KEY INSIGHT (Critical Connection):
 *    This problem is the INVERSE of the previous problem!
 *    
 *    Previous: Minimum deletions to make strings equal (find LCS)
 *    This one: Minimum string containing both (build from LCS)
 *    
 *    THE "AHA!" MOMENT:
 *    - If we merge two strings optimally, we want to OVERLAP common parts
 *    - The common parts = Longest Common Subsequence (LCS)
 *    - SCS length = len(str1) + len(str2) - len(LCS)
 *    - But we need to BUILD the actual string, not just find the length!
 * 
 * 3. VISUALIZATION (str1 = "abac", str2 = "cab"):
 *    
 *    str1: a b a c
 *    str2: c a b
 *    LCS:  a b (length 2)
 *    
 *    How to build SCS:
 *    - Start with empty result
 *    - For each position in LCS, we include characters from BOTH strings
 *    - Characters not in LCS come from their respective strings
 *    
 *    Building "cabac":
 *    c (from str2, before LCS)
 *    a (common, part of LCS)
 *    b (common, part of LCS)
 *    a (from str1, after LCS match)
 *    c (from str1, remaining)
 * 
 * 4. TWO-PHASE APPROACH:
 *    Phase 1: Find LCS using DP (similar to previous problem)
 *    Phase 2: Reconstruct SCS by backtracking through DP table
 * 
 * 5. WHY THIS WORKS:
 *    - We include every character from both strings
 *    - Characters in LCS are included ONCE (overlap)
 *    - Characters not in LCS are included from their respective strings
 *    - This gives us the shortest possible supersequence
 */

class ShortestCommonSupersequence {
    
    /*
     * APPROACH 1: DP with Backtracking (Most Intuitive)
     * ==================================================
     * 
     * ALGORITHM:
     * Step 1: Build LCS DP table
     * Step 2: Backtrack through table to construct SCS
     * 
     * DP TABLE MEANING:
     * dp[i][j] = length of LCS of str1[0...i-1] and str2[0...j-1]
     * 
     * BACKTRACKING LOGIC:
     * Start from dp[m][n] and move towards dp[0][0]
     * 
     * At each position (i, j):
     * - If str1[i-1] == str2[j-1]: This char is in LCS
     *   → Add it ONCE to result
     *   → Move diagonally: (i-1, j-1)
     * 
     * - If str1[i-1] != str2[j-1]: This char is NOT in LCS
     *   → Check which direction we came from in DP
     *   → If dp[i-1][j] > dp[i][j-1]: came from top
     *     → Add str1[i-1] to result, move to (i-1, j)
     *   → Else: came from left
     *     → Add str2[j-1] to result, move to (i, j-1)
     * 
     * When i == 0: Add all remaining chars from str2
     * When j == 0: Add all remaining chars from str1
     * 
     * TIME COMPLEXITY: O(m * n) for DP + O(m + n) for backtracking
     * SPACE COMPLEXITY: O(m * n) for DP table
     */
    public String shortestCommonSupersequence(String str1, String str2) {
        int m = str1.length();
        int n = str2.length();
        
        // Step 1: Build LCS DP table
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        
        // Step 2: Backtrack to build SCS
        StringBuilder result = new StringBuilder();
        int i = m, j = n;
        
        while (i > 0 && j > 0) {
            if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                // Character is in LCS - include it once
                result.append(str1.charAt(i - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] > dp[i][j - 1]) {
                // Came from top - include char from str1
                result.append(str1.charAt(i - 1));
                i--;
            } else {
                // Came from left - include char from str2
                result.append(str2.charAt(j - 1));
                j--;
            }
        }
        
        // Add remaining characters from str1 (if any)
        while (i > 0) {
            result.append(str1.charAt(i - 1));
            i--;
        }
        
        // Add remaining characters from str2 (if any)
        while (j > 0) {
            result.append(str2.charAt(j - 1));
            j--;
        }
        
        // We built the string backwards, so reverse it
        return result.reverse().toString();
    }
    
    /*
     * APPROACH 2: Direct DP with String Construction
     * ===============================================
     * 
     * OPTIMIZATION:
     * Instead of storing just lengths, store the actual SCS strings
     * 
     * DP STATE:
     * dp[i][j] = shortest common supersequence of str1[0...i-1] and str2[0...j-1]
     * 
     * BASE CASES:
     * dp[0][j] = str2[0...j-1] (just str2, since str1 is empty)
     * dp[i][0] = str1[0...i-1] (just str1, since str2 is empty)
     * 
     * RECURRENCE:
     * if str1[i-1] == str2[j-1]:
     *     dp[i][j] = dp[i-1][j-1] + str1[i-1]
     *     // Character is common, add it once
     * else:
     *     // Try both options, take shorter one
     *     option1 = dp[i-1][j] + str1[i-1]  // Include from str1
     *     option2 = dp[i][j-1] + str2[j-1]  // Include from str2
     *     dp[i][j] = shorter of option1 and option2
     * 
     * PROS: More intuitive, builds string directly
     * CONS: Higher space complexity due to storing strings
     * 
     * TIME COMPLEXITY: O(m * n * L) where L is average string length
     * SPACE COMPLEXITY: O(m * n * L) - stores strings, not just lengths
     */
    public String shortestCommonSupersequence_DirectDP(String str1, String str2) {
        int m = str1.length();
        int n = str2.length();
        
        // dp[i][j] = actual SCS string
        String[][] dp = new String[m + 1][n + 1];
        
        // Base cases
        dp[0][0] = "";
        
        // When str1 is empty, SCS is just str2
        for (int j = 1; j <= n; j++) {
            dp[0][j] = str2.substring(0, j);
        }
        
        // When str2 is empty, SCS is just str1
        for (int i = 1; i <= m; i++) {
            dp[i][0] = str1.substring(0, i);
        }
        
        // Fill the DP table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    // Characters match - include once
                    dp[i][j] = dp[i - 1][j - 1] + str1.charAt(i - 1);
                } else {
                    // Characters don't match - try both, pick shorter
                    String option1 = dp[i - 1][j] + str1.charAt(i - 1);
                    String option2 = dp[i][j - 1] + str2.charAt(j - 1);
                    
                    dp[i][j] = option1.length() < option2.length() ? option1 : option2;
                }
            }
        }
        
        return dp[m][n];
    }
    
    /*
     * APPROACH 3: Two-Pointer Merge with LCS
     * =======================================
     * 
     * CLEANER APPROACH:
     * 1. Find the actual LCS string (not just length)
     * 2. Use two pointers to merge str1 and str2 guided by LCS
     * 
     * ALGORITHM:
     * - Use pointer k for LCS
     * - Use pointer i for str1, j for str2
     * - If current chars match LCS[k], add it once, advance all pointers
     * - Otherwise, add non-matching chars from both strings
     * 
     * This is more intuitive once you understand the LCS connection
     * 
     * TIME COMPLEXITY: O(m * n)
     * SPACE COMPLEXITY: O(m * n) for DP table + O(LCS length)
     */
    public String shortestCommonSupersequence_TwoPointer(String str1, String str2) {
        // First, find the LCS string
        String lcs = findLCS(str1, str2);
        
        // Merge str1 and str2 using LCS as guide
        StringBuilder result = new StringBuilder();
        int i = 0, j = 0, k = 0;
        
        while (k < lcs.length()) {
            // Add characters from str1 until we match LCS[k]
            while (i < str1.length() && str1.charAt(i) != lcs.charAt(k)) {
                result.append(str1.charAt(i));
                i++;
            }
            
            // Add characters from str2 until we match LCS[k]
            while (j < str2.length() && str2.charAt(j) != lcs.charAt(k)) {
                result.append(str2.charAt(j));
                j++;
            }
            
            // Add the common character (from LCS)
            result.append(lcs.charAt(k));
            i++;
            j++;
            k++;
        }
        
        // Add any remaining characters from str1
        result.append(str1.substring(i));
        
        // Add any remaining characters from str2
        result.append(str2.substring(j));
        
        return result.toString();
    }
    
    /*
     * HELPER: Find actual LCS string (not just length)
     * 
     * This is similar to the previous problem but we reconstruct
     * the actual LCS string by backtracking
     */
    private String findLCS(String str1, String str2) {
        int m = str1.length();
        int n = str2.length();
        int[][] dp = new int[m + 1][n + 1];
        
        // Build LCS length table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        
        // Backtrack to build LCS string
        StringBuilder lcs = new StringBuilder();
        int i = m, j = n;
        
        while (i > 0 && j > 0) {
            if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                lcs.append(str1.charAt(i - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] > dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        
        return lcs.reverse().toString();
    }
    
    /*
     * DETAILED WALKTHROUGH: str1 = "abac", str2 = "cab"
     * ==================================================
     * 
     * STEP 1: Build LCS DP Table
     * 
     *       ""  c  a  b
     *   ""   0  0  0  0
     *   a    0  0  1  1
     *   b    0  0  1  2
     *   a    0  0  1  2
     *   c    0  1  1  2
     * 
     * LCS length = 2, LCS string = "ab"
     * 
     * STEP 2: Backtrack to Build SCS
     * 
     * Starting at dp[4][3] = 2
     * i=4, j=3: str1[3]='c', str2[2]='b', not equal
     *           dp[3][3]=2 > dp[4][2]=1, came from top
     *           Add 'c' from str1, move to (3,3)
     * 
     * i=3, j=3: str1[2]='a', str2[2]='b', not equal
     *           dp[2][3]=2 > dp[3][2]=1, came from top
     *           Add 'a' from str1, move to (2,3)
     * 
     * i=2, j=3: str1[1]='b', str2[2]='b', EQUAL (in LCS)
     *           Add 'b' once, move to (1,2)
     * 
     * i=1, j=2: str1[0]='a', str2[1]='a', EQUAL (in LCS)
     *           Add 'a' once, move to (0,1)
     * 
     * i=0, j=1: Add remaining from str2
     *           Add 'c'
     * 
     * Built (backwards): c, a, b, a, c
     * Reversed: "cabac"
     * 
     * VERIFICATION:
     * str1 = "abac" in "cabac"? YES: c[a][b][ac]
     * str2 = "cab" in "cabac"?  YES: [cab]ac
     */
    
    /*
     * INTERVIEW STRATEGY:
     * ===================
     * 
     * 1. RECOGNIZE THE PATTERN (1-2 minutes):
     *    "This is related to LCS - we want to merge two strings optimally"
     *    "The overlap will be the LCS"
     *    "Length formula: len(str1) + len(str2) - len(LCS)"
     * 
     * 2. CLARIFY REQUIREMENTS (30 seconds):
     *    "We need the actual string, not just the length, right?"
     *    "Any valid answer is fine if there are multiple?"
     * 
     * 3. EXPLAIN APPROACH (2 minutes):
     *    "I'll use DP to find LCS, then backtrack to build SCS"
     *    Draw a small example on whiteboard
     *    Show how backtracking works
     * 
     * 4. CODE THE SOLUTION (5-7 minutes):
     *    Start with Approach 1 (most efficient)
     *    Add clear comments
     * 
     * 5. WALK THROUGH EXAMPLE (2 minutes):
     *    Use given example or a simple one
     *    Show DP table and backtracking path
     * 
     * 6. DISCUSS ALTERNATIVES (if time):
     *    "I could also build strings directly in DP"
     *    "Or use two-pointer merge after finding LCS"
     *    Discuss trade-offs
     * 
     * 7. COMPLEXITY ANALYSIS:
     *    Time: O(m*n) for DP, O(m+n) for backtracking
     *    Space: O(m*n) for table
     * 
     * COMMON PITFALLS:
     * - Forgetting to reverse the result (built backwards)
     * - Not handling remaining characters after main loop
     * - Confusing when to add characters once vs twice
     * - Off-by-one errors in indexing
     */
    
    /*
     * RELATIONSHIP TO PREVIOUS PROBLEM:
     * =================================
     * 
     * Previous Problem: Minimum Delete Distance
     * - Delete chars to make strings equal
     * - Answer: m + n - 2*LCS
     * 
     * This Problem: Shortest Common Supersequence
     * - Merge strings optimally
     * - Answer length: m + n - LCS
     * - But we need to BUILD it, not just calculate length
     * 
     * MATHEMATICAL RELATIONSHIP:
     * If we have two strings A and B:
     * - LCS(A,B) = longest sequence in both
     * - SCS(A,B) = shortest sequence containing both
     * - |SCS(A,B)| = |A| + |B| - |LCS(A,B)|
     * 
     * This makes intuitive sense:
     * - We need all of A: |A| characters
     * - We need all of B: |B| characters
     * - We can overlap the common part: -|LCS(A,B)|
     */
    
    // Test cases with detailed output
    public static void main(String[] args) {
        ShortestCommonSupersequence solution = new ShortestCommonSupersequence();
        
        // Test Case 1
        System.out.println("=".repeat(60));
        String str1 = "abac";
        String str2 = "cab";
        System.out.println("Test Case 1:");
        System.out.println("str1 = \"" + str1 + "\", str2 = \"" + str2 + "\"");
        
        String result1 = solution.shortestCommonSupersequence(str1, str2);
        System.out.println("\nBacktracking Approach: " + result1);
        System.out.println("Length: " + result1.length());
        System.out.println("Expected: \"cabac\" (length 5)");
        verifySubsequence(str1, result1, "str1");
        verifySubsequence(str2, result1, "str2");
        
        String result2 = solution.shortestCommonSupersequence_DirectDP(str1, str2);
        System.out.println("\nDirect DP Approach: " + result2);
        
        String result3 = solution.shortestCommonSupersequence_TwoPointer(str1, str2);
        System.out.println("Two Pointer Approach: " + result3);
        
        // Test Case 2
        System.out.println("\n" + "=".repeat(60));
        str1 = "aaaaaaaa";
        str2 = "aaaaaaaa";
        System.out.println("Test Case 2:");
        System.out.println("str1 = \"" + str1 + "\", str2 = \"" + str2 + "\"");
        
        result1 = solution.shortestCommonSupersequence(str1, str2);
        System.out.println("\nBacktracking Approach: " + result1);
        System.out.println("Length: " + result1.length());
        System.out.println("Expected: \"aaaaaaaa\" (length 8)");
        verifySubsequence(str1, result1, "str1");
        verifySubsequence(str2, result1, "str2");
        
        // Test Case 3: No common characters
        System.out.println("\n" + "=".repeat(60));
        str1 = "abc";
        str2 = "def";
        System.out.println("Test Case 3 (No common chars):");
        System.out.println("str1 = \"" + str1 + "\", str2 = \"" + str2 + "\"");
        
        result1 = solution.shortestCommonSupersequence(str1, str2);
        System.out.println("\nBacktracking Approach: " + result1);
        System.out.println("Length: " + result1.length());
        System.out.println("Expected length: 6 (all characters needed)");
        verifySubsequence(str1, result1, "str1");
        verifySubsequence(str2, result1, "str2");
        
        // Test Case 4: One string is subsequence of other
        System.out.println("\n" + "=".repeat(60));
        str1 = "abc";
        str2 = "ac";
        System.out.println("Test Case 4 (One is subsequence):");
        System.out.println("str1 = \"" + str1 + "\", str2 = \"" + str2 + "\"");
        
        result1 = solution.shortestCommonSupersequence(str1, str2);
        System.out.println("\nBacktracking Approach: " + result1);
        System.out.println("Length: " + result1.length());
        System.out.println("Expected: \"abc\" (length 3, since str2 is subsequence of str1)");
        verifySubsequence(str1, result1, "str1");
        verifySubsequence(str2, result1, "str2");
        
        System.out.println("\n" + "=".repeat(60));
    }
    
    // Helper method to verify if str1 is a subsequence of str2
    private static void verifySubsequence(String str1, String str2, String name) {
        int i = 0, j = 0;
        while (i < str1.length() && j < str2.length()) {
            if (str1.charAt(i) == str2.charAt(j)) {
                i++;
            }
            j++;
        }
        boolean isSubsequence = (i == str1.length());
        System.out.println("✓ " + name + " is " + (isSubsequence ? "" : "NOT ") + 
                         "a subsequence of result: " + isSubsequence);
    }
}
