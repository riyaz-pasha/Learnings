import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Regular Expression Matching
 * Given an input string `s` and a pattern `p`, implement regular expression 
 * matching with support for '.' and '*'.
 * 
 * '.' Matches any single character.
 * '*' Matches zero or more of the PRECEDING element.
 * 
 * The matching should cover the entire input string (not partial).
 * 
 * Constraints:
 * 1 <= s.length <= 20
 * 1 <= p.length <= 20
 * s contains only lowercase English letters.
 * p contains only lowercase English letters, '.', and '*'.
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, it is crucial to explicitly distinguish this from 
 * standard Wildcard Matching (LeetCode 44).
 * 
 * Q: "Does '*' match any sequence of characters like it does in a terminal shell?"
 * A: No! This is the most common trap. In Regex, '*' is NOT an independent 
 *    wildcard. It is a modifier attached exclusively to the character strictly 
 *    before it. 'a*' means 0 or more 'a's. '.*' means 0 or more of ANY character.
 * 
 * Q: "Can '*' appear at the very beginning of the pattern?"
 * A: The constraints guarantee that '*' will always have a valid preceding 
 *    character to modify. We don't need to handle malformed patterns like "*a".
 * 
 * CRITICAL SENIOR INSIGHT - THE ZERO OCCURRENCE RULE:
 * "The hardest part of this problem is that a character followed by a '*' can 
 * completely vanish. For example, pattern 'c*a' perfectly matches string 'a'. 
 * This means our pattern evaluation can jump forward by 2 indices while our 
 * string pointer stays completely still. Because we have multiple valid branches 
 * (consume the character OR ignore the pattern entirely), we have overlapping 
 * subproblems that require DP."
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "We place two pointers: `i` on string `s` and `j` on pattern `p`.
 * 
 * SCENARIO A: The next character in the pattern is NOT a '*'
 * We simply check if `s[i] == p[j]` or `p[j] == '.'`. 
 * If yes, we advance both pointers. If no, the path dies.
 * 
 * SCENARIO B: The next character in the pattern IS a '*' (at p[j+1])
 * We are at a crossroad with two parallel universes:
 * 1. ZERO OCCURRENCES: We decide to completely ignore the current pattern 
 *    character and its attached '*'. We advance the pattern pointer by 2, 
 *    leaving the string pointer exactly where it is.
 * 2. ONE OR MORE OCCURRENCES: IF the current string character actually matches 
 *    the pattern character, we consume ONE character from the string (`i+1`), 
 *    but we KEEP the pattern pointer exactly where it is so the '*' can 
 *    potentially match even more characters!
 * 
 * Since multiple evaluations will overlap on the same `(i, j)` state, we use 
 * Dynamic Programming."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: s = "aab", p = "c*a*b"
 * 
 * Top-Down Evaluation:
 * solve(0, 0) -> s="aab", p="c*a*b"
 * Next char in pattern is '*'. 
 *   Universe 1 (Zero 'c's): skip "c*". Call solve(0, 2) -> s="aab", p="a*b"
 *   Universe 2 (One+ 'c's): 'a' != 'c', so this universe instantly dies.
 * 
 * solve(0, 2) -> s="aab", p="a*b"
 * Next char is '*'.
 *   Universe 1 (Zero 'a's): skip "a*". Call solve(0, 4) -> s="aab", p="b" (Dies, 'a'!='b')
 *   Universe 2 (One+ 'a's): 'a' == 'a'. Advance string. Call solve(1, 2) -> s="ab", p="a*b"
 * 
 * solve(1, 2) -> s="ab", p="a*b"
 * Next char is '*'.
 *   Universe 1: solve(1, 4) -> s="ab", p="b" (Dies)
 *   Universe 2: 'a' == 'a'. Advance string. Call solve(2, 2) -> s="b", p="a*b"
 * 
 * solve(2, 2) -> s="b", p="a*b"
 * Next char is '*'.
 *   Universe 1: skip "a*". Call solve(2, 4) -> s="b", p="b".
 *   Universe 2: 'b' != 'a'. (Dies).
 * 
 * solve(2, 4) -> s="b", p="b".
 * Exact match! Advance both.
 * solve(3, 5) -> Both empty. Return TRUE!
 */
public class RegularExpressionMatching {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Translate the logical rules directly into recursive calls.
     * 
     * Time Complexity: O(2^(T+P)) - Exponential branching due to '*' wildcard.
     * Space Complexity: O(T^2 + P^2) - Substring creation overhead + recursion stack.
     */
    public boolean isMatchRecursive(String s, String p) {
        if (p.isEmpty()) {
            return s.isEmpty();
        }
        
        // Check if the current first characters match
        boolean firstMatch = (!s.isEmpty() && 
                             (p.charAt(0) == s.charAt(0) || p.charAt(0) == '.'));

        // If the pattern has a '*' immediately after the current character
        if (p.length() >= 2 && p.charAt(1) == '*') {
            // Universe 1: Zero occurrences. Skip the character and the '*'.
            boolean zeroOccurrences = isMatchRecursive(s, p.substring(2));
            
            // Universe 2: One or more occurrences. If the first char matches, 
            // consume 1 string char and KEEP the '*' pattern to match more.
            boolean oneOrMore = (firstMatch && isMatchRecursive(s.substring(1), p));
            
            return zeroOccurrences || oneOrMore;
        } else {
            // Standard 1-to-1 character match
            return firstMatch && isMatchRecursive(s.substring(1), p.substring(1));
        }
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Avoid expensive substring creation by passing indices `i` and `j`. 
     * Cache the evaluated boolean results for state `(i, j)`.
     * 
     * Time Complexity: O(S * P) - We evaluate each index pair at most once.
     * Space Complexity: O(S * P) - For the 2D memo array + recursion stack.
     */
    public boolean isMatchMemo(String s, String p) {
        Boolean[][] memo = new Boolean[s.length() + 1][p.length() + 1];
        return solveMemo(s, p, 0, 0, memo);
    }

    private boolean solveMemo(String s, String p, int i, int j, Boolean[][] memo) {
        if (memo[i][j] != null) {
            return memo[i][j];
        }

        // BASE CASE: If pattern is exhausted, string must also be exhausted.
        if (j == p.length()) {
            return i == s.length();
        }

        boolean firstMatch = (i < s.length() && 
                             (p.charAt(j) == s.charAt(i) || p.charAt(j) == '.'));

        boolean result;
        if (j + 1 < p.length() && p.charAt(j + 1) == '*') {
            result = solveMemo(s, p, i, j + 2, memo) || 
                     (firstMatch && solveMemo(s, p, i + 1, j, memo));
        } else {
            result = firstMatch && solveMemo(s, p, i + 1, j + 1, memo);
        }

        memo[i][j] = result;
        return result;
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Build an (S+1) x (P+1) grid. dp[i][j] answers: "Does the prefix of 
     * `s` of length `i` match the prefix of `p` of length `j`?"
     * 
     * Time Complexity: O(S * P)
     * Space Complexity: O(S * P)
     */
    public boolean isMatchTabulation(String s, String p) {
        int m = s.length();
        int n = p.length();
        boolean[][] dp = new boolean[m + 1][n + 1];

        // BASE CASE: Two empty strings perfectly match each other.
        dp[0][0] = true;

        // BASE CASE: Empty string 's' against a pattern 'p'.
        // Patterns like "a*b*c*" can evaluate to an empty string!
        // We look back 2 spaces in the pattern to skip the char and the '*'.
        for (int j = 1; j <= n; j++) {
            if (p.charAt(j - 1) == '*') {
                dp[0][j] = dp[0][j - 2];
            }
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                
                // --- DETAILED TABULATION EXPLANATION ---
                // Case 1: The current characters match exactly, or pattern has a '.'.
                // We simply inherit the matching status from the previous characters.
                if (p.charAt(j - 1) == s.charAt(i - 1) || p.charAt(j - 1) == '.') {
                    dp[i][j] = dp[i - 1][j - 1];
                } 
                // Case 2: The pattern character is a '*'. (The complex case)
                else if (p.charAt(j - 1) == '*') {
                    
                    // Universe 1: ZERO occurrences of the preceding character.
                    // We look 2 columns left to totally ignore the 'X*' combo.
                    dp[i][j] = dp[i][j - 2];
                    
                    // Universe 2: ONE OR MORE occurrences.
                    // If the character directly preceding the '*' actually matches 
                    // our current string character (or is a '.')...
                    if (p.charAt(j - 2) == s.charAt(i - 1) || p.charAt(j - 2) == '.') {
                        // We check if the string ONE character ago was valid 
                        // against this exact same pattern `dp[i - 1][j]`.
                        dp[i][j] = dp[i][j] || dp[i - 1][j];
                    }
                }
            }
        }

        return dp[m][n];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In the 2D Tabulation, to calculate row `i`, we ONLY rely on the 
     * previous row `i-1` and the current row `i`. 
     * 
     * We can collapse the O(S * P) matrix into two 1D arrays of size (P+1): 
     * `prevRow` and `currRow`. We swap them at the end of each string character.
     * 
     * Time Complexity: O(S * P)
     * Space Complexity: O(P) - Massively reduced memory footprint.
     */
    public boolean isMatchSpaceOptimized(String s, String p) {
        int m = s.length();
        int n = p.length();
        
        boolean[] prevRow = new boolean[n + 1];
        boolean[] currRow = new boolean[n + 1];

        // Seed the initial state (matching against an empty string)
        prevRow[0] = true;
        for (int j = 1; j <= n; j++) {
            if (p.charAt(j - 1) == '*') {
                prevRow[j] = prevRow[j - 2];
            }
        }

        for (int i = 1; i <= m; i++) {
            
            // currRow[0] is matching a non-empty string with an empty pattern -> always false
            currRow[0] = false;
            
            for (int j = 1; j <= n; j++) {
                
                if (p.charAt(j - 1) == s.charAt(i - 1) || p.charAt(j - 1) == '.') {
                    currRow[j] = prevRow[j - 1];
                } 
                else if (p.charAt(j - 1) == '*') {
                    
                    currRow[j] = currRow[j - 2]; // Zero occurrences
                    
                    if (p.charAt(j - 2) == s.charAt(i - 1) || p.charAt(j - 2) == '.') {
                        currRow[j] = currRow[j] || prevRow[j]; // One+ occurrences
                    }
                    
                } else {
                    currRow[j] = false; // Mismatch
                }
            }
            
            // Swap the arrays for the next iteration. 
            // We reuse prevRow's memory to avoid allocating a new array.
            boolean[] temp = prevRow;
            prevRow = currRow;
            currRow = temp;
        }

        // Because we swap at the end of the outer loop, the final answer rests in prevRow
        return prevRow[n];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new RegularExpressionMatching();
        
        record TestCase(String s, String p, boolean expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase("aa", "a", false),
            new TestCase("aa", "a*", true),
            new TestCase("ab", ".*", true),
            new TestCase("aab", "c*a*b", true), // Traced in comments
            new TestCase("mississippi", "mis*is*p*.", false),
            new TestCase("a", "ab*", true) // Zero 'b's
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("String  : \"" + tc.s + "\"");
            System.out.println("Pattern : \"" + tc.p + "\"");
            System.out.println("Expected: " + tc.expected);
            
            System.out.println("Recursive (Brute) : " + solver.isMatchRecursive(tc.s, tc.p));
            System.out.println("Memoization       : " + solver.isMatchMemo(tc.s, tc.p));
            System.out.println("Tabulation 2D     : " + solver.isMatchTabulation(tc.s, tc.p));
            System.out.println("Space Optimized   : " + solver.isMatchSpaceOptimized(tc.s, tc.p));
            System.out.println();
        }
    }
}

import java.util.*;

public class Solution {

    public boolean isMatch(String text, String pattern) {
        return matchHelper(text, pattern, 0, 0);
    }

    /**
     * ============================================================
     * 🧠 CORE IDEA:
     * ------------------------------------------------------------
     * matchHelper(i, j) → does text[i...] match pattern[j...] ?
     *
     * We try to match BOTH strings from current indices.
     *
     * At each step:
     *   1. Check if current characters match
     *   2. Handle '*' (most important case)
     *   3. Move forward accordingly
     *
     * ============================================================
     */
    private boolean matchHelper(String text, String pattern,
                                int textIndex, int patternIndex) {

        int textLength = text.length();
        int patternLength = pattern.length();

        // ============================================================
        // ✅ BASE CASE:
        // If pattern is exhausted → text must also be exhausted
        // ============================================================
        if (patternIndex == patternLength) {
            return textIndex == textLength;
        }

        // ============================================================
        // ✅ CHECK CURRENT MATCH:
        //
        // We can match current characters if:
        //   1. text still has characters
        //   2. characters are equal OR pattern has '.'
        // ============================================================
        boolean isCurrentMatch =
                (textIndex < textLength) &&
                (text.charAt(textIndex) == pattern.charAt(patternIndex)
                        || pattern.charAt(patternIndex) == '.');

        // ============================================================
        // ⭐ MOST IMPORTANT BLOCK → HANDLE '*'
        //
        // Pattern like:  a*  OR  .*
        //
        // NOTE:
        // '*' ALWAYS belongs to previous character
        // So we check patternIndex + 1
        // ============================================================
        if (patternIndex + 1 < patternLength &&
                pattern.charAt(patternIndex + 1) == '*') {

            // --------------------------------------------------------
            // 🔹 OPTION 1: SKIP '*' (0 occurrences)
            //
            // Example:
            //   text = "b"
            //   pattern = "a*b"
            //
            // Skip "a*" → move pattern by 2
            // --------------------------------------------------------
            boolean skipStar =
                    matchHelper(text, pattern, textIndex, patternIndex + 2);

            // --------------------------------------------------------
            // 🔹 OPTION 2: USE '*' (1 or more occurrences)
            //
            // Only possible if current chars match
            //
            // Important:
            //   - We move text forward (consume char)
            //   - BUT pattern stays at same position
            //     (because '*' can repeat multiple times)
            //
            // Example:
            //   text = "aaa"
            //   pattern = "a*"
            // --------------------------------------------------------
            boolean useStar =
                    isCurrentMatch &&
                    matchHelper(text, pattern, textIndex + 1, patternIndex);

            // If ANY option works → match is successful
            return skipStar || useStar;
        }

        // ============================================================
        // ⭐ NORMAL CASE (NO '*')
        //
        // If current matches → move both pointers forward
        // Else → mismatch
        // ============================================================
        return isCurrentMatch &&
                matchHelper(text, pattern, textIndex + 1, patternIndex + 1);
    }
}

