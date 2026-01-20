/**
 * WILDCARD PATTERN MATCHING - Complete Analysis and Implementation
 * 
 * PROBLEM UNDERSTANDING:
 * =====================
 * Given a string s and a pattern p, implement wildcard pattern matching with:
 * - '?' matches any single character
 * - '*' matches any sequence of characters (including empty sequence)
 * - Must match the ENTIRE input string (not partial)
 * 
 * This is a CLASSIC Dynamic Programming problem, similar to Regular Expression Matching
 * but with slightly different semantics. Often asked at Google, Amazon, Microsoft, Facebook.
 * 
 * ============================================================================
 * HOW TO APPROACH THIS IN AN INTERVIEW:
 * ============================================================================
 * 
 * Step 1: CLARIFY THE PROBLEM (2-3 minutes)
 * -----------------------------------------
 * Ask questions:
 * - Can the string or pattern be empty? (Yes, handle edge cases)
 * - Does '*' match zero or more characters? (Yes, including empty)
 * - Does '?' match exactly one character? (Yes, any single character)
 * - Are there any other special characters? (No, just '?', '*', and literals)
 * - Case sensitive? (Usually yes)
 * - What's the expected length? (Helps decide optimization)
 * 
 * Step 2: WORK THROUGH EXAMPLES (3-5 minutes)
 * -------------------------------------------
 * Key insights from examples:
 * 
 * s = "aa", p = "a"     → false (pattern too short)
 * s = "aa", p = "*"     → true  (star matches everything)
 * s = "cb", p = "?a"    → false (? matches 'c', but 'a' doesn't match 'b')
 * s = "adceb", p = "*a*b" → true (star can match multiple chars)
 * s = "acdcb", p = "a*c?b" → false
 * 
 * Step 3: IDENTIFY THE PATTERN (5 minutes)
 * ----------------------------------------
 * This is a MATCHING problem with choices at each position:
 * 
 * At each position (i, j):
 * 1. If p[j] is a literal: must match s[i]
 * 2. If p[j] is '?': matches any s[i]
 * 3. If p[j] is '*': can match 0 or more characters
 *    - Match 0 chars: move pattern forward
 *    - Match 1+ chars: consume from string
 * 
 * Key insight: Overlapping subproblems + Optimal substructure = DP!
 * 
 * Step 4: SOLUTION PROGRESSION
 * ---------------------------
 * 1. Start with recursion (show understanding)
 * 2. Add memoization (optimize)
 * 3. Convert to bottom-up DP (expected solution)
 * 4. Optimize space if time permits
 * 5. Consider greedy approach (bonus)
 * 
 * ============================================================================
 * KEY DIFFERENCES FROM REGULAR EXPRESSION MATCHING:
 * ============================================================================
 * - '*' in wildcard = any sequence (greedy)
 * - '*' in regex = zero or more of PREVIOUS character
 * - Wildcard matching is generally simpler
 * 
 * ============================================================================
 */

class WildcardMatching {
    
    /**
     * ========================================================================
     * APPROACH 1: PURE RECURSION (Brute Force)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(2^(m+n)) - Exponential, each '*' creates branching
     * SPACE COMPLEXITY: O(m+n) - Recursion stack depth
     * 
     * WHY THIS WORKS:
     * At each position, we try all possible matches:
     * - For literals and '?': straightforward matching
     * - For '*': try matching 0, 1, 2, ... characters
     * 
     * INTERVIEW TIP: Start here to show understanding, then immediately
     * mention "this has exponential complexity due to '*' creating multiple
     * branches, so we need DP."
     */
    public boolean isMatchRecursive(String s, String p) {
        return recursiveHelper(s, p, 0, 0);
    }
    
    private boolean recursiveHelper(String s, String p, int i, int j) {
        // BASE CASES:
        
        // Both exhausted - perfect match
        if (i == s.length() && j == p.length()) {
            return true;
        }
        
        // Pattern exhausted but string remains - no match
        if (j == p.length()) {
            return false;
        }
        
        // String exhausted but pattern remains
        // Only valid if remaining pattern is all '*'
        if (i == s.length()) {
            // Check if all remaining pattern chars are '*'
            for (int k = j; k < p.length(); k++) {
                if (p.charAt(k) != '*') {
                    return false;
                }
            }
            return true;
        }
        
        // RECURSIVE CASES:
        
        char pChar = p.charAt(j);
        char sChar = s.charAt(i);
        
        // Case 1: Pattern has '*'
        if (pChar == '*') {
            // '*' can match:
            // 1. Empty sequence: skip '*', keep string position
            // 2. One or more chars: keep '*', advance string position
            return recursiveHelper(s, p, i, j + 1) ||  // Match 0 chars
                   recursiveHelper(s, p, i + 1, j);     // Match 1+ chars
        }
        
        // Case 2: Pattern has '?' or literal character
        // '?' matches any single char, literal must match exactly
        if (pChar == '?' || pChar == sChar) {
            return recursiveHelper(s, p, i + 1, j + 1);
        }
        
        // No match possible
        return false;
    }
    
    /**
     * ========================================================================
     * APPROACH 2: RECURSION WITH MEMOIZATION (Top-Down DP)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(m * n) - Each subproblem solved once
     * SPACE COMPLEXITY: O(m * n) - Memoization table + O(m+n) recursion stack
     * 
     * WHY MEMOIZATION WORKS:
     * The recursive solution has overlapping subproblems.
     * For example, matching "aaa" with "a*a*":
     * - helper(1, 1) might be reached through multiple paths
     * 
     * We cache results: memo[i][j] = result for s[i...] matching p[j...]
     * 
     * INTERVIEW TIP: Good intermediate solution showing optimization skills.
     */
    public boolean isMatchMemoization(String s, String p) {
        Boolean[][] memo = new Boolean[s.length() + 1][p.length() + 1];
        return memoHelper(s, p, 0, 0, memo);
    }
    
    private boolean memoHelper(String s, String p, int i, int j, Boolean[][] memo) {
        // Check memo first
        if (memo[i][j] != null) {
            return memo[i][j];
        }
        
        boolean result;
        
        // BASE CASES (same as recursive)
        if (i == s.length() && j == p.length()) {
            result = true;
        } else if (j == p.length()) {
            result = false;
        } else if (i == s.length()) {
            result = true;
            for (int k = j; k < p.length(); k++) {
                if (p.charAt(k) != '*') {
                    result = false;
                    break;
                }
            }
        } else {
            // RECURSIVE CASES
            char pChar = p.charAt(j);
            char sChar = s.charAt(i);
            
            if (pChar == '*') {
                result = memoHelper(s, p, i, j + 1, memo) ||
                         memoHelper(s, p, i + 1, j, memo);
            } else if (pChar == '?' || pChar == sChar) {
                result = memoHelper(s, p, i + 1, j + 1, memo);
            } else {
                result = false;
            }
        }
        
        // Cache and return
        memo[i][j] = result;
        return result;
    }
    
    /**
     * ========================================================================
     * APPROACH 3: BOTTOM-UP DYNAMIC PROGRAMMING (Expected Interview Solution)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(m * n) - Fill entire DP table
     * SPACE COMPLEXITY: O(m * n) - DP table
     * 
     * THE CORE DP INSIGHT:
     * --------------------
     * Define: dp[i][j] = true if s[0...i-1] matches p[0...j-1]
     * 
     * BASE CASES:
     * - dp[0][0] = true (empty string matches empty pattern)
     * - dp[i][0] = false for i > 0 (non-empty string doesn't match empty pattern)
     * - dp[0][j] = dp[0][j-1] if p[j-1] == '*' (pattern of only stars matches empty)
     * 
     * RECURRENCE RELATION:
     * 
     * If p[j-1] == '*':
     *     dp[i][j] = dp[i][j-1]     // '*' matches empty sequence
     *             || dp[i-1][j]     // '*' matches one or more chars
     * 
     * If p[j-1] == '?' or p[j-1] == s[i-1]:
     *     dp[i][j] = dp[i-1][j-1]   // Characters match
     * 
     * Otherwise:
     *     dp[i][j] = false          // No match
     * 
     * 
     * VISUALIZATION for s = "aa", p = "*":
     * 
     *        ""  *
     *    ""   T   T
     *    a    F   T
     *    a    F   T
     * 
     * dp[2][1] = true (answer)
     * 
     * 
     * VISUALIZATION for s = "adceb", p = "*a*b":
     * 
     *        ""  *   a   *   b
     *    ""   T   T   F   F   F
     *    a    F   T   T   T   F
     *    d    F   T   F   T   F
     *    c    F   T   F   T   F
     *    e    F   T   F   T   F
     *    b    F   T   F   T   T
     * 
     * dp[5][4] = true (answer)
     * 
     * 
     * INTERVIEW TIP: This is the EXPECTED solution. Practice:
     * 1. Drawing the DP table
     * 2. Explaining each cell's computation
     * 3. Handling the '*' logic carefully
     */
    public boolean isMatchDP(String s, String p) {
        int m = s.length();
        int n = p.length();
        
        // dp[i][j] = true if s[0...i-1] matches p[0...j-1]
        boolean[][] dp = new boolean[m + 1][n + 1];
        
        // BASE CASE 1: Empty string matches empty pattern
        dp[0][0] = true;
        
        // BASE CASE 2: Empty string matches pattern of only stars
        // For pattern like "***", empty string should match
        for (int j = 1; j <= n; j++) {
            if (p.charAt(j - 1) == '*') {
                dp[0][j] = dp[0][j - 1];
            }
            // If pattern[j-1] is not '*', dp[0][j] remains false
        }
        
        // FILL THE DP TABLE
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                char sChar = s.charAt(i - 1);
                char pChar = p.charAt(j - 1);
                
                if (pChar == '*') {
                    // '*' can match:
                    // 1. Empty sequence: dp[i][j-1] (skip the '*')
                    // 2. One or more characters: dp[i-1][j] (use '*' again)
                    dp[i][j] = dp[i][j - 1] || dp[i - 1][j];
                } else if (pChar == '?' || pChar == sChar) {
                    // '?' matches any char, or literal matches
                    // Take result from diagonal (both advance)
                    dp[i][j] = dp[i - 1][j - 1];
                }
                // else: dp[i][j] remains false (no match)
            }
        }
        
        return dp[m][n];
    }
    
    /**
     * ========================================================================
     * APPROACH 4: SPACE-OPTIMIZED DP
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(m * n)
     * SPACE COMPLEXITY: O(n) - Only two rows needed
     * 
     * KEY OBSERVATION:
     * To compute dp[i][j], we only need:
     * - dp[i-1][j-1] (diagonal)
     * - dp[i-1][j] (above)
     * - dp[i][j-1] (left)
     * 
     * We can use rolling arrays to reduce space from O(mn) to O(n).
     * 
     * INTERVIEW TIP: Implement if specifically asked about space optimization.
     */
    public boolean isMatchSpaceOptimized(String s, String p) {
        int m = s.length();
        int n = p.length();
        
        boolean[] prev = new boolean[n + 1];
        boolean[] curr = new boolean[n + 1];
        
        // BASE CASE: empty string matches empty pattern
        prev[0] = true;
        
        // BASE CASE: empty string matches pattern of stars
        for (int j = 1; j <= n; j++) {
            if (p.charAt(j - 1) == '*') {
                prev[j] = prev[j - 1];
            }
        }
        
        // FILL ROW BY ROW
        for (int i = 1; i <= m; i++) {
            curr[0] = false; // Non-empty string doesn't match empty pattern
            
            for (int j = 1; j <= n; j++) {
                char sChar = s.charAt(i - 1);
                char pChar = p.charAt(j - 1);
                
                if (pChar == '*') {
                    curr[j] = curr[j - 1] || prev[j];
                } else if (pChar == '?' || pChar == sChar) {
                    curr[j] = prev[j - 1];
                } else {
                    curr[j] = false;
                }
            }
            
            // Swap arrays
            boolean[] temp = prev;
            prev = curr;
            curr = temp;
        }
        
        return prev[n];
    }
    
    /**
     * ========================================================================
     * APPROACH 5: GREEDY WITH BACKTRACKING (Advanced, Optimal for some cases)
     * ========================================================================
     * 
     * TIME COMPLEXITY: O(m * n) worst case, but often much faster in practice
     * SPACE COMPLEXITY: O(1) - Only a few pointers
     * 
     * INTUITION:
     * Instead of trying all possibilities with '*', use a greedy approach:
     * - Match characters one by one
     * - When we hit '*', remember this position as a "fallback point"
     * - If matching fails later, backtrack to the last '*' and try matching more chars
     * 
     * This is harder to come up with but very elegant!
     * 
     * INTERVIEW TIP: Only mention if you have extra time or interviewer asks
     * for O(1) space solution. Most impressive if you can code this correctly.
     */
    public boolean isMatchGreedy(String s, String p) {
        int sIdx = 0, pIdx = 0;
        int starIdx = -1;  // Position of last '*' in pattern
        int matchIdx = 0;   // Position in string where we matched the last '*'
        
        while (sIdx < s.length()) {
            // Case 1: Characters match or pattern has '?'
            if (pIdx < p.length() && 
                (p.charAt(pIdx) == '?' || p.charAt(pIdx) == s.charAt(sIdx))) {
                sIdx++;
                pIdx++;
            }
            // Case 2: Pattern has '*'
            else if (pIdx < p.length() && p.charAt(pIdx) == '*') {
                starIdx = pIdx;      // Remember star position
                matchIdx = sIdx;     // Remember current string position
                pIdx++;              // Move pattern forward (try matching 0 chars)
            }
            // Case 3: Mismatch - backtrack to last '*' if exists
            else if (starIdx != -1) {
                pIdx = starIdx + 1;  // Go back to position after last '*'
                matchIdx++;          // Try matching one more char with '*'
                sIdx = matchIdx;     // Resume from here
            }
            // Case 4: No match and no '*' to fall back to
            else {
                return false;
            }
        }
        
        // Check remaining pattern - should be all stars
        while (pIdx < p.length() && p.charAt(pIdx) == '*') {
            pIdx++;
        }
        
        return pIdx == p.length();
    }
    
    /**
     * ========================================================================
     * APPROACH 6: PATTERN PREPROCESSING (Optimization)
     * ========================================================================
     * 
     * OPTIMIZATION INSIGHT:
     * Multiple consecutive stars are equivalent to a single star.
     * "a***b" is same as "a*b"
     * 
     * Preprocessing the pattern can speed up all approaches.
     */
    private String preprocessPattern(String p) {
        if (p.isEmpty()) return p;
        
        StringBuilder sb = new StringBuilder();
        sb.append(p.charAt(0));
        
        for (int i = 1; i < p.length(); i++) {
            // Skip consecutive stars
            if (p.charAt(i) == '*' && p.charAt(i - 1) == '*') {
                continue;
            }
            sb.append(p.charAt(i));
        }
        
        return sb.toString();
    }
    
    public boolean isMatchOptimized(String s, String p) {
        // Preprocess pattern to remove consecutive stars
        p = preprocessPattern(p);
        return isMatchGreedy(s, p);
    }
    
    /**
     * ========================================================================
     * HELPER: Debug method to visualize DP table
     * ========================================================================
     */
    public void printDPTable(String s, String p) {
        int m = s.length();
        int n = p.length();
        boolean[][] dp = new boolean[m + 1][n + 1];
        
        dp[0][0] = true;
        for (int j = 1; j <= n; j++) {
            if (p.charAt(j - 1) == '*') {
                dp[0][j] = dp[0][j - 1];
            }
        }
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                char sChar = s.charAt(i - 1);
                char pChar = p.charAt(j - 1);
                
                if (pChar == '*') {
                    dp[i][j] = dp[i][j - 1] || dp[i - 1][j];
                } else if (pChar == '?' || pChar == sChar) {
                    dp[i][j] = dp[i - 1][j - 1];
                }
            }
        }
        
        // Print table
        System.out.print("      \"\"");
        for (int j = 0; j < n; j++) {
            System.out.printf("  %c ", p.charAt(j));
        }
        System.out.println();
        
        System.out.print("\"\"    ");
        for (int j = 0; j <= n; j++) {
            System.out.printf(" %s ", dp[0][j] ? "T" : "F");
        }
        System.out.println();
        
        for (int i = 1; i <= m; i++) {
            System.out.printf("%c     ", s.charAt(i - 1));
            for (int j = 0; j <= n; j++) {
                System.out.printf(" %s ", dp[i][j] ? "T" : "F");
            }
            System.out.println();
        }
    }
    
    /**
     * ========================================================================
     * TESTING AND VALIDATION
     * ========================================================================
     */
    public static void main(String[] args) {
        WildcardMatching solution = new WildcardMatching();
        
        System.out.println("=== WILDCARD PATTERN MATCHING TESTS ===\n");
        
        // Test Case 1: Basic mismatch
        System.out.println("Test 1: s='aa', p='a'");
        String s1 = "aa", p1 = "a";
        System.out.println("Expected: false");
        System.out.println("Recursive: " + solution.isMatchRecursive(s1, p1));
        System.out.println("Memoization: " + solution.isMatchMemoization(s1, p1));
        System.out.println("DP: " + solution.isMatchDP(s1, p1));
        System.out.println("Space Optimized: " + solution.isMatchSpaceOptimized(s1, p1));
        System.out.println("Greedy: " + solution.isMatchGreedy(s1, p1));
        System.out.println();
        
        // Test Case 2: Star matches all
        System.out.println("Test 2: s='aa', p='*'");
        String s2 = "aa", p2 = "*";
        System.out.println("Expected: true");
        System.out.println("DP: " + solution.isMatchDP(s2, p2));
        System.out.println("Greedy: " + solution.isMatchGreedy(s2, p2));
        System.out.println();
        
        // Test Case 3: Question mark usage
        System.out.println("Test 3: s='cb', p='?a'");
        String s3 = "cb", p3 = "?a";
        System.out.println("Expected: false");
        System.out.println("DP: " + solution.isMatchDP(s3, p3));
        System.out.println("Greedy: " + solution.isMatchGreedy(s3, p3));
        System.out.println();
        
        // Test Case 4: Complex pattern
        System.out.println("Test 4: s='adceb', p='*a*b'");
        String s4 = "adceb", p4 = "*a*b";
        System.out.println("Expected: true");
        System.out.println("DP: " + solution.isMatchDP(s4, p4));
        System.out.println("Greedy: " + solution.isMatchGreedy(s4, p4));
        System.out.println("\nDP Table:");
        solution.printDPTable(s4, p4);
        System.out.println();
        
        // Test Case 5: Complex pattern 2
        System.out.println("Test 5: s='acdcb', p='a*c?b'");
        String s5 = "acdcb", p5 = "a*c?b";
        System.out.println("Expected: false");
        System.out.println("DP: " + solution.isMatchDP(s5, p5));
        System.out.println("Greedy: " + solution.isMatchGreedy(s5, p5));
        System.out.println();
        
        // Test Case 6: Empty string
        System.out.println("Test 6: s='', p='*'");
        System.out.println("Expected: true");
        System.out.println("DP: " + solution.isMatchDP("", "*"));
        System.out.println("Greedy: " + solution.isMatchGreedy("", "*"));
        System.out.println();
        
        // Test Case 7: Empty pattern
        System.out.println("Test 7: s='abc', p=''");
        System.out.println("Expected: false");
        System.out.println("DP: " + solution.isMatchDP("abc", ""));
        System.out.println("Greedy: " + solution.isMatchGreedy("abc", ""));
        System.out.println();
        
        // Test Case 8: Question marks
        System.out.println("Test 8: s='abc', p='???'");
        System.out.println("Expected: true");
        System.out.println("DP: " + solution.isMatchDP("abc", "???"));
        System.out.println("Greedy: " + solution.isMatchGreedy("abc", "???"));
        System.out.println();
        
        // Test Case 9: Multiple stars
        System.out.println("Test 9: s='abcd', p='*a***b*c*d*'");
        System.out.println("Expected: true");
        System.out.println("DP: " + solution.isMatchDP("abcd", "*a***b*c*d*"));
        System.out.println("Greedy: " + solution.isMatchGreedy("abcd", "*a***b*c*d*"));
        System.out.println("Optimized: " + solution.isMatchOptimized("abcd", "*a***b*c*d*"));
        System.out.println();
        
        // Test Case 10: Tricky case
        System.out.println("Test 10: s='aaaa', p='***a'");
        System.out.println("Expected: true");
        System.out.println("DP: " + solution.isMatchDP("aaaa", "***a"));
        System.out.println("Greedy: " + solution.isMatchGreedy("aaaa", "***a"));
        System.out.println();
    }
}

/**
 * ============================================================================
 * INTERVIEW TIPS SUMMARY:
 * ============================================================================
 * 
 * 1. PROBLEM RECOGNITION:
 *    - Pattern matching with wildcards → DP problem
 *    - Similar to "Regular Expression Matching" but simpler
 *    - Look for: choices at each position, optimal substructure
 * 
 * 2. SOLUTION PROGRESSION:
 *    - Start: "Let me work through an example first"
 *    - Identify: "This looks like a DP problem because..."
 *    - Explain: "I'll start with recursion to show the logic"
 *    - Optimize: "Then we can optimize with DP"
 * 
 * 3. KEY INSIGHTS TO MENTION:
 *    - '*' is the tricky part - it can match 0 or more chars
 *    - Need to try both options: skip star or use it
 *    - Overlapping subproblems make this DP
 * 
 * 4. COMMON MISTAKES TO AVOID:
 *    - Off-by-one errors in indexing
 *    - Forgetting to handle empty string/pattern
 *    - Not initializing dp[0][j] correctly for pattern of stars
 *    - Confusing when to use dp[i-1][j] vs dp[i][j-1] for '*'
 * 
 * 5. COMPLEXITY DISCUSSION:
 *    - Recursive: O(2^(m+n)) - mention this is too slow
 *    - DP: O(m*n) time, O(m*n) space
 *    - Space optimized: O(m*n) time, O(n) space
 *    - Greedy: O(m*n) worst case, O(1) space, often faster in practice
 * 
 * 6. TIME MANAGEMENT (45 min interview):
 *    - Problem understanding: 3-5 min
 *    - Discuss approach: 5-7 min
 *    - Code DP solution: 15-20 min
 *    - Testing: 5-7 min
 *    - Optimization discussion: 5-8 min
 * 
 * 7. FOLLOW-UP QUESTIONS YOU MIGHT GET:
 *    Q: Can you optimize space?
 *    A: Yes, use rolling arrays - O(n) space
 *    
 *    Q: What if we have more wildcards like '+' for one or more?
 *    A: Similar DP logic, adjust recurrence relation
 *    
 *    Q: How would you handle case-insensitive matching?
 *    A: Convert both to lowercase first
 *    
 *    Q: Can you do it in O(1) space?
 *    A: Yes, with greedy backtracking approach
 * 
 * 8. VARIATIONS YOU MIGHT SEE:
 *    - Regular Expression Matching (with '.' and '*')
 *    - File path matching (glob patterns)
 *    - String matching with multiple patterns
 *    - Fuzzy string matching
 * 
 * 9. TESTING STRATEGY:
 *    - Test empty cases first
 *    - Test with only '*'
 *    - Test with only '?'
 *    - Test combinations
 *    - Test edge cases (all stars in pattern)
 * 
 * 10. WHAT MAKES A STRONG INTERVIEW PERFORMANCE:
 *     ✓ Clear communication of thought process
 *     ✓ Start with simple solution, then optimize
 *     ✓ Handle edge cases
 *     ✓ Clean, bug-free code
 *     ✓ Good variable names
 *     ✓ Test your code
 *     ✓ Discuss tradeoffs
 * 
 * ============================================================================
 * ADDITIONAL INSIGHTS:
 * ============================================================================
 * 
 * 1. REAL-WORLD APPLICATIONS:
 *    - File systems (ls *.txt, find *.java)
 *    - Database queries (LIKE operator in SQL)
 *    - Text editors (search with wildcards)
 *    - Shell scripting (glob patterns)
 *    - Network routing tables
 *    - Access control lists
 * 
 * 2. RELATED PROBLEMS:
 *    - Regular Expression Matching (LeetCode 10)
 *    - Edit Distance (similarity: both are matching problems)
 *    - Longest Common Subsequence
 *    - String to String transformation
 * 
 * 3. PERFORMANCE OPTIMIZATIONS:
 *    - Preprocess pattern (remove consecutive stars)
 *    - Early termination (if pattern becomes impossible)
 *    - KMP for literal parts between wildcards
 * 
 * 4. COMPLEXITY LOWER BOUNDS:
 *    - Cannot do better than O(m*n) in general case
 *    - Some special cases can be optimized
 *    - O(1) space is possible with greedy approach
 * 
 * 5. DEBUGGING TIPS:
 *    - Print DP table to visualize
 *    - Trace through small examples by hand
 *    - Check base cases first
 *    - Verify recurrence relation matches intuition
 * 
 * ============================================================================
 */

class WildcardMatchingSolutions {

    /**
     * APPROACH 1: BRUTE FORCE RECURSIVE
     * ---------------------------------------------------------
     * Interview Reasoning:
     * Demonstrate understanding of the base logic.
     * * Time Complexity: Exponential in worst case (e.g., s="aaaa", p="*a*b")
     * Space Complexity: O(N) recursion stack
     */
    public boolean isMatchRecursive(String s, String p) {
        return solveRecursive(s, p, s.length() - 1, p.length() - 1);
    }

    private boolean solveRecursive(String s, String p, int i, int j) {
        // Base Case 1: Both strings exhausted -> Match found
        if (i < 0 && j < 0) return true;

        // Base Case 2: Pattern exhausted but string remains -> False
        if (j < 0 && i >= 0) return false;

        // Base Case 3: String exhausted but pattern remains
        // Only true if remaining pattern chars are all '*'
        if (i < 0 && j >= 0) {
            for (int k = 0; k <= j; k++) {
                if (p.charAt(k) != '*') return false;
            }
            return true;
        }

        // Logic Step 1: Exact match or '?'
        if (s.charAt(i) == p.charAt(j) || p.charAt(j) == '?') {
            return solveRecursive(s, p, i - 1, j - 1);
        }

        // Logic Step 2: '*' handling
        if (p.charAt(j) == '*') {
            // Option A: '*' matches nothing (skip '*') -> solve(i, j-1)
            // Option B: '*' matches current char s[i] (consume s[i]) -> solve(i-1, j)
            return solveRecursive(s, p, i, j - 1) || solveRecursive(s, p, i - 1, j);
        }

        // Logic Step 3: Mismatch
        return false;
    }

    /**
     * APPROACH 2: BOTTOM-UP DYNAMIC PROGRAMMING (TABULATION)
     * ---------------------------------------------------------
     * Interview Reasoning:
     * Eliminate redundant calculations. This is a very standard DP grid problem.
     * * dp[i][j] = does s[0...i-1] match p[0...j-1]?
     * * Time Complexity: O(S * P) where S and P are string lengths.
     * Space Complexity: O(S * P) for the table.
     */
    public boolean isMatchTabulation(String s, String p) {
        int m = s.length();
        int n = p.length();
        boolean[][] dp = new boolean[m + 1][n + 1];

        // 1. Base Case: Empty string matches empty pattern
        dp[0][0] = true;

        // 2. Base Case: Pattern with '*' can match empty string
        for (int j = 1; j <= n; j++) {
            if (p.charAt(j - 1) == '*') {
                dp[0][j] = dp[0][j - 1]; // Propagate true if previous was true
            }
        }

        // 3. Fill the grid
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                char sChar = s.charAt(i - 1);
                char pChar = p.charAt(j - 1);

                if (pChar == sChar || pChar == '?') {
                    // Current chars match, inherit from diagonal
                    dp[i][j] = dp[i - 1][j - 1];
                } else if (pChar == '*') {
                    // '*' matches empty sequence OR '*' matches current char
                    // dp[i][j-1] -> Treat '*' as empty (ignore it)
                    // dp[i-1][j] -> Treat '*' as matching s[i] (move string index)
                    dp[i][j] = dp[i][j - 1] || dp[i - 1][j];
                } else {
                    dp[i][j] = false;
                }
            }
        }

        return dp[m][n];
    }

    /**
     * APPROACH 3: GREEDY TWO POINTERS (OPTIMAL)
     * ---------------------------------------------------------
     * Interview Reasoning:
     * If we make a "wrong" decision at a '*', we can just come back and try
     * to match more characters with that same '*'.
     * We don't need to backtrack recursively; we just need to remember the 
     * position of the last '*' and the position in 's' where we tried to match it.
     * * Time Complexity: O(S * P) in worst case (e.g. lots of stars), 
     * but often O(S) on average.
     * Space Complexity: O(1) - Constant space!
     */
    public boolean isMatchOptimal(String s, String p) {
        int sIdx = 0, pIdx = 0;
        int lastStarIdx = -1; // Index of the last '*' found in p
        int sTmpIdx = -1;     // Index in s when we encountered the last '*'

        while (sIdx < s.length()) {
            // Case 1: Match or '?'
            if (pIdx < p.length() && (p.charAt(pIdx) == '?' || p.charAt(pIdx) == s.charAt(sIdx))) {
                sIdx++;
                pIdx++;
            }
            // Case 2: Found '*'
            else if (pIdx < p.length() && p.charAt(pIdx) == '*') {
                lastStarIdx = pIdx; // Record star position
                sTmpIdx = sIdx;     // Record current string position
                pIdx++;             // Advance pattern (try matching 0 chars first)
            }
            // Case 3: Mismatch, but we have a previous '*' to fall back on
            else if (lastStarIdx != -1) {
                pIdx = lastStarIdx + 1; // Reset pattern to just after the '*'
                sTmpIdx++;              // Consume one more char from s for this '*'
                sIdx = sTmpIdx;         // Reset string pointer to the new consume point
            }
            // Case 4: Mismatch and no '*' to save us
            else {
                return false;
            }
        }

        // Check if remaining characters in pattern are all '*'
        while (pIdx < p.length() && p.charAt(pIdx) == '*') {
            pIdx++;
        }

        return pIdx == p.length();
    }

    // Main for testing
    public static void main(String[] args) {
        WildcardMatchingSolutions solver = new WildcardMatchingSolutions();
        
        String s = "adceb";
        String p = "*a*b";

        System.out.println("Matching s='" + s + "', p='" + p + "'");
        System.out.println("Tabulation: " + solver.isMatchTabulation(s, p));
        System.out.println("Optimal: " + solver.isMatchOptimal(s, p));
    }
}
