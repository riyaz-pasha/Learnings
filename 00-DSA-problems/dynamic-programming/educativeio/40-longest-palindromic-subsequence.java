import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Longest Palindromic Subsequence
 * Given a string s, return the length of the longest palindromic subsequence in s.
 * A subsequence is a sequence that can be derived from another sequence by 
 * deleting some or no elements without changing the order of the remaining elements.
 * 
 * Constraints:
 * 1 <= s.length <= 1000
 * s consists only of lowercase English letters.
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, it is highly beneficial to point out the connection 
 * between this problem and the "Longest Common Subsequence" (LCS) problem.
 * 
 * Q: "Can I just reverse the string and find the Longest Common Subsequence 
 *     between the original string and the reversed string?"
 * A: YES! This is a classic 'Senior Flex'. Mathematically, the Longest 
 *    Palindromic Subsequence of a string `s` is exactly equal to 
 *    LCS(s, reverse(s)). While you can code it that way, solving it natively 
 *    using Interval DP is slightly more optimal in space and demonstrates 
 *    stronger state-transition skills.
 * 
 * CRITICAL SENIOR INSIGHT - INTERVAL DP:
 * "Just like Matrix Chain Multiplication or Burst Balloons, this is an Interval 
 * DP problem. We cannot simply evaluate left-to-right because a palindrome is 
 * defined by its boundaries (left and right). 
 * We must start with small intervals (length 1) and expand outward to larger 
 * intervals, OR start from the end of the string and work backwards to guarantee 
 * the subproblems inside the boundaries are already solved."
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "We place two pointers, `left` and `right`, at the ends of the string.
 * 
 * SCENARIO A: s.charAt(left) == s.charAt(right)
 * We have found two matching characters that can form the outer shell of a 
 * palindrome. We get a length of 2, PLUS the longest palindrome we can find 
 * strictly between them.
 * -> 2 + solve(left + 1, right - 1)
 * 
 * SCENARIO B: s.charAt(left) != s.charAt(right)
 * The characters don't match, so they cannot both be part of the outermost 
 * palindrome shell. We must try ignoring the left character, and then try 
 * ignoring the right character, taking the maximum of those two universes.
 * -> Math.max(solve(left + 1, right), solve(left, right - 1))
 * 
 * Because moving pointers inward results in evaluating the same inner substrings 
 * multiple times, we have overlapping subproblems and must use DP."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: s = "bbbab"
 * 
 * Trace (Top-Down):
 * solve(0, 4) -> 'b' == 'b'. Result is 2 + solve(1, 3).
 *   -> solve(1, 3) -> "bba". 'b' != 'a'. Max of:
 *      -> solve(2, 3) -> "ba". 'b' != 'a'. Max(solve(3,3), solve(2,2)) -> Max(1, 1) = 1.
 *      -> solve(1, 2) -> "bb". 'b' == 'b'. Result is 2 + solve(2,1). (Base case left > right -> 0) = 2.
 *   -> Max of (1, 2) is 2.
 * -> Outer result: 2 + 2 = 4.
 * 
 * Longest palindromic subsequence is "bbbb" (length 4).
 */
public class LongestPalindromicSubsequence {

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Test the two pointers on the ends. If they match, step both inward. 
     * If they don't, branch into two paths (step left inward, step right inward).
     * 
     * Time Complexity: O(2^N) - Exponential branching when characters don't match.
     * Space Complexity: O(N) - Maximum depth of the recursion tree.
     */
    public int longestPalindromeSubseqRecursive(String s) {
        if (s == null || s.length() == 0) return 0;
        return solveRecursive(s, 0, s.length() - 1);
    }

    private int solveRecursive(String s, int left, int right) {
        // BASE CASE REASONING:
        // If pointers cross, the interval is empty. Length is 0.
        if (left > right) {
            return 0;
        }
        
        // If pointers are on the exact same character, an odd-length palindrome 
        // has exactly 1 character in the direct center.
        if (left == right) {
            return 1;
        }

        // Universe 1: Match! Both characters contribute to the palindrome.
        if (s.charAt(left) == s.charAt(right)) {
            return 2 + solveRecursive(s, left + 1, right - 1);
        } 
        
        // Universe 2: Mismatch! Try dropping left, then try dropping right.
        int dropLeft = solveRecursive(s, left + 1, right);
        int dropRight = solveRecursive(s, left, right - 1);
        
        return Math.max(dropLeft, dropRight);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the results for `(left, right)` intervals so we evaluate 
     * each inner substring exactly once.
     * 
     * Time Complexity: O(N^2) - State space is N x N.
     * Space Complexity: O(N^2) - For the memo array + recursion stack.
     */
    public int longestPalindromeSubseqMemo(String s) {
        if (s == null || s.length() == 0) return 0;
        
        int n = s.length();
        int[][] memo = new int[n][n];
        for (int[] row : memo) Arrays.fill(row, -1);
        
        return solveMemo(s, 0, n - 1, memo);
    }

    private int solveMemo(String s, int left, int right, int[][] memo) {
        if (left > right) return 0;
        if (left == right) return 1;

        if (memo[left][right] != -1) {
            return memo[left][right];
        }

        if (s.charAt(left) == s.charAt(right)) {
            memo[left][right] = 2 + solveMemo(s, left + 1, right - 1, memo);
        } else {
            memo[left][right] = Math.max(
                solveMemo(s, left + 1, right, memo),
                solveMemo(s, left, right - 1, memo)
            );
        }

        return memo[left][right];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Build an N x N matrix. `dp[i][j]` signifies the length of the longest 
     * palindromic subsequence strictly within the substring from index `i` to `j`.
     * To ensure smaller intervals are processed before larger ones, we iterate 
     * `i` backwards from `N-1` to 0, and `j` forwards from `i+1` to `N-1`.
     * 
     * Time Complexity: O(N^2)
     * Space Complexity: O(N^2)
     */
    public int longestPalindromeSubseqTabulation(String s) {
        if (s == null || s.length() == 0) return 0;
        
        int n = s.length();
        int[][] dp = new int[n][n];

        // Outer loop iterates BACKWARDS through the start index (left pointer)
        for (int i = n - 1; i >= 0; i--) {
            
            // BASE CASE REASONING:
            // A single character (where left == right) is a palindrome of length 1.
            dp[i][i] = 1;
            
            // Inner loop iterates FORWARDS through the end index (right pointer)
            for (int j = i + 1; j < n; j++) {
                
                // --- DETAILED TABULATION EXPLANATION ---
                if (s.charAt(i) == s.charAt(j)) {
                    // Match! We take the characters at i and j (length 2), 
                    // and add the optimal palindrome from the strictly inner substring.
                    // The inner substring is represented by dp[i + 1][j - 1].
                    dp[i][j] = 2 + dp[i + 1][j - 1];
                } else {
                    // Mismatch! We take the best result of either:
                    // 1. Ignoring the left character (look at dp[i + 1][j])
                    // 2. Ignoring the right character (look at dp[i][j - 1])
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j - 1]);
                }
            }
        }

        // The answer for the entire string sits covering the interval 0 to N-1.
        return dp[0][n - 1];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: Look closely at the Tabulation loop above. To calculate row `i`, 
     * we ONLY need data from row `i+1` (the row immediately below it) and the 
     * values we are currently writing in row `i`.
     * 
     * We can collapse the N x N grid into a single 1D array representing the 
     * right pointer `j`. Because `dp[i+1][j-1]` is overwritten by the time 
     * we need it, we must store it temporarily in a `prev` variable.
     * 
     * Time Complexity: O(N^2)
     * Space Complexity: O(N) - Massively reduced memory footprint!
     */
    public int longestPalindromeSubseqSpaceOptimized(String s) {
        if (s == null || s.length() == 0) return 0;
        
        int n = s.length();
        int[] dp = new int[n];

        for (int i = n - 1; i >= 0; i--) {
            
            // For the start of each row, dp[i][i] is always 1.
            dp[i] = 1;
            
            // `prev` holds the value of dp[i+1][j-1].
            // Before the inner loop starts (where j = i+1), j-1 is exactly i.
            // And dp[i+1][i] is mathematically an empty string, so it's 0.
            int prev = 0; 
            
            for (int j = i + 1; j < n; j++) {
                
                // Before we potentially overwrite dp[j], we MUST save it.
                // Right now, dp[j] physically represents dp[i+1][j].
                // In the next loop iteration (j+1), this exact value will act 
                // as dp[i+1][(j+1)-1] ... which is the new `prev`!
                int temp = dp[j];
                
                if (s.charAt(i) == s.charAt(j)) {
                    
                    // MAGIC OF THE 1D ARRAY + PREV VARIABLE:
                    dp[j] = 2 + prev;
                    
                } else {
                    
                    // dp[j]   -> Ignored left character (from row i+1)
                    // dp[j-1] -> Ignored right character (calculated a microsecond ago)
                    dp[j] = Math.max(dp[j], dp[j - 1]);
                    
                }
                
                // Advance the diagonal anchor forward for the next column
                prev = temp;
            }
        }

        return dp[n - 1];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new LongestPalindromicSubsequence();
        
        record TestCase(String s, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase("bbbab", 4),     // "bbbb"
            new TestCase("cbbd", 2),      // "bb"
            new TestCase("a", 1),         // Single character
            new TestCase("abcde", 1),     // No duplicates, any 1 char is a palindrome
            new TestCase("racecar", 7)    // Entire string is already a palindrome
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("String  : \"" + tc.s + "\"");
            System.out.println("Expected: " + tc.expected);
            
            // Prevent recursive explosion on long strings during testing
            if (tc.s.length() <= 20) {
                System.out.println("Recursive (Brute) : " + solver.longestPalindromeSubseqRecursive(tc.s));
            } else {
                System.out.println("Recursive (Brute) : Skipped (Too slow for O(2^N))");
            }
            
            System.out.println("Memoization       : " + solver.longestPalindromeSubseqMemo(tc.s));
            System.out.println("Tabulation 2D     : " + solver.longestPalindromeSubseqTabulation(tc.s));
            System.out.println("Space Optimized   : " + solver.longestPalindromeSubseqSpaceOptimized(tc.s));
            System.out.println();
        }
    }
}


public class Solution {

    /*
     * ============================================================
     * PROBLEM
     * ============================================================
     *
     * Given a string s, find the length of the longest subsequence
     * of s which is a palindrome.
     *
     * A subsequence:
     *   - keeps the relative order of characters
     *   - does NOT need to be contiguous
     *
     * Example:
     *
     * s = "bbbab"
     *
     * Longest palindromic subsequence = "bbbb"
     * Answer = 4
     *
     *
     * ============================================================
     * APPROACHES IN THIS FILE
     * ============================================================
     *
     * 1. LPS using LCS + Memoization
     *      Time  : O(n^2)
     *      Space : O(n^2)
     *
     * 2. LPS using LCS + Bottom-Up DP
     *      Time  : O(n^2)
     *      Space : O(n^2)
     *
     * 3. Direct LPS Bottom-Up DP
     *      Time  : O(n^2)
     *      Space : O(n^2)
     *
     * 4. Direct LPS Space-Optimized DP
     *      Time  : O(n^2)
     *      Space : O(n)
     *
     *
     * ============================================================
     */


    // ============================================================
    // 1. TOP-DOWN / MEMOIZATION
    // ============================================================
    //
    // Idea:
    //
    // A palindrome reads the same from both directions.
    //
    // Another way of looking at the problem:
    //
    //     LPS(s) = LCS(s, reverse(s))
    //
    // Example:
    //
    //     s       = "bbbab"
    //     reverse = "babbb"
    //
    // LCS("bbbab", "babbb") = 4
    //
    // Therefore:
    //
    //     LPS("bbbab") = 4
    //
    // ------------------------------------------------------------
    //
    // State:
    //
    // solve(p1, p2)
    //
    // means:
    //
    //     What is the LCS length between
    //
    //         s1[0 ... p1]
    //         s2[0 ... p2]
    //
    // ------------------------------------------------------------
    //
    // If characters match:
    //
    //     s1[p1] == s2[p2]
    //
    // then we can include this character:
    //
    //     1 + solve(p1 - 1, p2 - 1)
    //
    // If they don't match:
    //
    //     s1[p1] != s2[p2]
    //
    // we have two choices:
    //
    //     1. Ignore s1[p1]
    //     2. Ignore s2[p2]
    //
    // Therefore:
    //
    //     max(
    //         solve(p1 - 1, p2),
    //         solve(p1, p2 - 1)
    //     )
    //
    // ------------------------------------------------------------
    //
    // Complexity:
    //
    // There are n * n possible states.
    //
    // Each state does O(1) work.
    //
    // Time  = O(n^2)
    // Space = O(n^2) memo + O(n) recursion stack
    //
    // ============================================================

    public int longestPalindromeSubseqMemo(String s) {

        int n = s.length();

        // Empty string -> answer is 0.
        if (n == 0) {
            return 0;
        }

        // Reverse the string.
        String reversed =
                new StringBuilder(s).reverse().toString();

        /*
         * memo[p1][p2] stores:
         *
         * LCS length between
         *
         *     s[0 ... p1]
         *     reversed[0 ... p2]
         *
         * null means:
         *
         *     "this state has not been calculated yet"
         */
        Integer[][] memo = new Integer[n][n];

        return solveMemo(
                memo,
                s,
                reversed,
                n - 1,
                n - 1
        );
    }


    private int solveMemo(
            Integer[][] memo,
            String s1,
            String s2,
            int p1,
            int p2
    ) {

        /*
         * BASE CASE
         *
         * If either string has been completely consumed,
         * there cannot be any common subsequence.
         *
         * Example:
         *
         * s1 = "abc"
         * p1 = -1
         *
         * There are no characters left.
         *
         * Answer = 0
         */
        if (p1 < 0 || p2 < 0) {
            return 0;
        }


        /*
         * MEMOIZATION CHECK
         *
         * If we already solved this state,
         * don't calculate it again.
         *
         * This is what changes the naive exponential
         * recursion into O(n^2).
         */
        if (memo[p1][p2] != null) {
            return memo[p1][p2];
        }


        int result;


        /*
         * CASE 1: CHARACTERS MATCH
         *
         * Example:
         *
         * s1[p1] = 'b'
         * s2[p2] = 'b'
         *
         * We can include this character in our LCS.
         *
         * Therefore:
         *
         *     result =
         *         1 + solve(p1 - 1, p2 - 1)
         */
        if (s1.charAt(p1) == s2.charAt(p2)) {

            result = 1 + solveMemo(
                    memo,
                    s1,
                    s2,
                    p1 - 1,
                    p2 - 1
            );

        }

        /*
         * CASE 2: CHARACTERS DON'T MATCH
         *
         * Example:
         *
         * s1[p1] = 'a'
         * s2[p2] = 'b'
         *
         * We cannot take both characters together.
         *
         * We try:
         *
         *     A) Remove s1[p1]
         *
         *        solve(p1 - 1, p2)
         *
         *     B) Remove s2[p2]
         *
         *        solve(p1, p2 - 1)
         *
         * Take the better answer.
         */
        else {

            int skip1 = solveMemo(
                    memo,
                    s1,
                    s2,
                    p1 - 1,
                    p2
            );

            int skip2 = solveMemo(
                    memo,
                    s1,
                    s2,
                    p1,
                    p2 - 1
            );

            result = Math.max(skip1, skip2);
        }


        /*
         * Store the answer before returning it.
         *
         * This prevents us from solving the same state again.
         */
        memo[p1][p2] = result;

        return result;
    }


    // ============================================================
    // 2. BOTTOM-UP LCS DP
    // ============================================================
    //
    // Same mathematical idea as the memoization solution:
    //
    //     LPS(s) = LCS(s, reverse(s))
    //
    // But instead of recursively calculating states,
    // we calculate them iteratively.
    //
    // ------------------------------------------------------------
    //
    // dp[i][j] means:
    //
    //     LCS length of
    //
    //     s[0 ... i-1]
    //     reversed[0 ... j-1]
    //
    // Notice the "-1".
    //
    // dp has dimensions:
    //
    //     (n + 1) x (n + 1)
    //
    // The extra row and column represent empty strings.
    //
    // ------------------------------------------------------------
    //
    // Transition:
    //
    // If characters match:
    //
    //     dp[i][j] = 1 + dp[i-1][j-1]
    //
    // Otherwise:
    //
    //     dp[i][j] = max(
    //         dp[i-1][j],
    //         dp[i][j-1]
    //     )
    //
    // ------------------------------------------------------------
    //
    // Time  : O(n^2)
    // Space : O(n^2)
    //
    // ============================================================

    public int longestPalindromeSubseqLCSBottomUp(String s) {

        int n = s.length();

        if (n == 0) {
            return 0;
        }

        String reversed =
                new StringBuilder(s).reverse().toString();

        /*
         * dp[i][j] =
         *
         * LCS length between:
         *
         *     first i characters of s
         *     first j characters of reversed
         *
         * Therefore we need n + 1.
         */
        int[][] dp = new int[n + 1][n + 1];


        /*
         * We start from 1 because:
         *
         * dp[0][j] = 0
         * dp[i][0] = 0
         *
         * These represent LCS with an empty string.
         */
        for (int i = 1; i <= n; i++) {

            for (int j = 1; j <= n; j++) {

                /*
                 * Convert DP indices to string indices:
                 *
                 * DP index i corresponds to s[i - 1].
                 * DP index j corresponds to reversed[j - 1].
                 */
                if (s.charAt(i - 1) ==
                        reversed.charAt(j - 1)) {

                    /*
                     * Matching characters can be included.
                     */
                    dp[i][j] =
                            1 + dp[i - 1][j - 1];

                } else {

                    /*
                     * Characters don't match.
                     *
                     * Try skipping either one.
                     */
                    dp[i][j] =
                            Math.max(
                                    dp[i - 1][j],
                                    dp[i][j - 1]
                            );
                }
            }
        }


        /*
         * dp[n][n] contains the LCS of the entire strings.
         *
         * Since:
         *
         *     LPS(s) = LCS(s, reverse(s))
         *
         * this is our answer.
         */
        return dp[n][n];
    }


    // ============================================================
    // 3. DIRECT LPS BOTTOM-UP DP
    // ============================================================
    //
    // This is the version I recommend that you learn for interviews.
    //
    // It doesn't require the LCS trick.
    //
    // We directly define:
    //
    //     dp[i][j]
    //
    // as:
    //
    //     length of the longest palindromic subsequence
    //     inside s[i ... j]
    //
    // ------------------------------------------------------------
    //
    // Example:
    //
    // s = "bbbab"
    //
    // dp[0][4] represents:
    //
    //     LPS("bbbab")
    //
    // ------------------------------------------------------------
    //
    // The key observation:
    //
    // If:
    //
    //     s[i] == s[j]
    //
    // then these two characters can be the two
    // ends of a palindrome.
    //
    // Therefore:
    //
    //     dp[i][j] =
    //         2 + dp[i+1][j-1]
    //
    //
    // If:
    //
    //     s[i] != s[j]
    //
    // then both cannot simultaneously be the two
    // ends of the palindrome.
    //
    // So we try:
    //
    //     skip left:
    //         dp[i+1][j]
    //
    //     skip right:
    //         dp[i][j-1]
    //
    // Therefore:
    //
    //     dp[i][j] =
    //         max(
    //             dp[i+1][j],
    //             dp[i][j-1]
    //         )
    //
    // ------------------------------------------------------------
    //
    // Base case:
    //
    // A single character is always a palindrome.
    //
    //     dp[i][i] = 1
    //
    // ------------------------------------------------------------
    //
    // Time  : O(n^2)
    // Space : O(n^2)
    //
    // ============================================================

    public int longestPalindromeSubseqDirectDP(String s) {

        int n = s.length();

        if (n == 0) {
            return 0;
        }

        /*
         * dp[i][j] =
         *
         * LPS length inside s[i ... j]
         */
        int[][] dp = new int[n][n];


        /*
         * BASE CASE
         *
         * Every individual character is a palindrome
         * of length 1.
         *
         * Example:
         *
         * "abc"
         *
         * dp[0][0] = 1   -> "a"
         * dp[1][1] = 1   -> "b"
         * dp[2][2] = 1   -> "c"
         */
        for (int i = 0; i < n; i++) {
            dp[i][i] = 1;
        }


        /*
         * We need to solve smaller ranges before larger ranges.
         *
         * For:
         *
         *     dp[i][j]
         *
         * we depend on:
         *
         *     dp[i+1][j-1]
         *     dp[i+1][j]
         *     dp[i][j-1]
         *
         * Therefore we iterate:
         *
         *     i from right -> left
         *     j from left -> right
         *
         * Example:
         *
         * i = n-1
         * i = n-2
         * ...
         * i = 0
         */
        for (int i = n - 2; i >= 0; i--) {

            /*
             * j must be greater than or equal to i.
             *
             * We already initialized dp[i][i] = 1,
             * so we start from i + 1.
             */
            for (int j = i + 1; j < n; j++) {

                /*
                 * CASE 1:
                 *
                 * s[i] and s[j] are equal.
                 *
                 * They can form the two ends of a palindrome.
                 *
                 * Example:
                 *
                 *     b ... b
                 *
                 * So:
                 *
                 *     2 + best palindrome inside
                 *
                 *                     i+1 ... j-1
                 */
                if (s.charAt(i) == s.charAt(j)) {

                    /*
                     * If i + 1 > j - 1, the inside is empty.
                     *
                     * For adjacent characters such as:
                     *
                     *     "bb"
                     *
                     * the answer should simply be 2.
                     *
                     * dp[i + 1][j - 1] would be an invalid
                     * index in that case.
                     */
                    if (i + 1 > j - 1) {

                        dp[i][j] = 2;

                    } else {

                        dp[i][j] =
                                2 + dp[i + 1][j - 1];
                    }

                }

                /*
                 * CASE 2:
                 *
                 * s[i] != s[j]
                 *
                 * We cannot use both as matching endpoints.
                 *
                 * So we try:
                 *
                 *     1. Ignore s[i]
                 *     2. Ignore s[j]
                 */
                else {

                    dp[i][j] =
                            Math.max(
                                    dp[i + 1][j],
                                    dp[i][j - 1]
                            );
                }
            }
        }


        /*
         * The whole string is:
         *
         *     s[0 ... n-1]
         *
         * Therefore:
         *
         *     dp[0][n-1]
         */
        return dp[0][n - 1];
    }


    // ============================================================
    // 4. DIRECT LPS SPACE-OPTIMIZED DP
    // ============================================================
    //
    // We can reduce the O(n^2) space to O(n).
    //
    // The original recurrence is:
    //
    //     dp[i][j]
    //
    // depends on:
    //
    //     dp[i+1][j-1]
    //     dp[i+1][j]
    //     dp[i][j-1]
    //
    // We don't need the entire 2D table at the same time.
    //
    // We can reuse a single array.
    //
    // ------------------------------------------------------------
    //
    // Time  : O(n^2)
    // Space : O(n)
    //
    // ------------------------------------------------------------
    //
    // This version is more difficult to understand initially.
    // I recommend learning the 2D version first.
    //
    // ============================================================

    public int longestPalindromeSubseqSpaceOptimized(String s) {

        int n = s.length();

        if (n == 0) {
            return 0;
        }

        /*
         * dp[j] represents the answer for the current i
         * and range [i ... j].
         */
        int[] dp = new int[n];

        /*
         * Initially i = n - 1.
         *
         * Only one character exists:
         *
         *     dp[n - 1] = 1
         */
        Arrays.fill(dp, 1);


        /*
         * Move i from right to left.
         */
        for (int i = n - 2; i >= 0; i--) {

            /*
             * dp[i] corresponds to the single character s[i].
             *
             * Therefore:
             *
             *     dp[i] = 1
             */
            dp[i] = 1;

            /*
             * This variable stores the OLD value of:
             *
             *     dp[i + 1][j - 1]
             *
             * because when we overwrite dp[j],
             * we would otherwise lose that value.
             */
            int diagonal = 1;

            /*
             * Expand j from left to right.
             */
            for (int j = i + 1; j < n; j++) {

                /*
                 * Save current dp[j].
                 *
                 * Before updating dp[j], it represents:
                 *
                 *     dp[i + 1][j]
                 *
                 * which we may need for the next iteration.
                 */
                int oldDpJ = dp[j];


                if (s.charAt(i) == s.charAt(j)) {

                    /*
                     * Current characters match.
                     *
                     * diagonal represents:
                     *
                     *     dp[i + 1][j - 1]
                     *
                     * Therefore:
                     *
                     *     dp[i][j] =
                     *         diagonal + 2
                     */
                    dp[j] = diagonal + 2;

                } else {

                    /*
                     * Characters don't match.
                     *
                     * dp[j] currently contains:
                     *
                     *     dp[i + 1][j]
                     *
                     * dp[j - 1] contains:
                     *
                     *     dp[i][j - 1]
                     *
                     * Therefore:
                     *
                     *     dp[i][j] =
                     *         max(
                     *             dp[i + 1][j],
                     *             dp[i][j - 1]
                     *         )
                     */
                    dp[j] =
                            Math.max(
                                    dp[j],
                                    dp[j - 1]
                            );
                }


                /*
                 * Move the diagonal value forward.
                 *
                 * oldDpJ was:
                 *
                 *     dp[i + 1][j]
                 *
                 * On the next iteration this becomes the
                 * required:
                 *
                 *     dp[i + 1][(j + 1) - 1]
                 *
                 * i.e.
                 *
                 *     dp[i + 1][j]
                 */
                diagonal = oldDpJ;
            }
        }


        /*
         * dp[n - 1] eventually contains:
         *
         *     dp[0][n - 1]
         *
         * which is the answer for the entire string.
         */
        return dp[n - 1];
    }


    // ============================================================
    // MAIN METHOD
    // ============================================================
    //
    // Used only for testing.
    //
    // On LeetCode you normally don't need this.
    //
    // ============================================================

    public static void main(String[] args) {

        Solution solution = new Solution();

        String[] testCases = {
                "bbbab",
                "cbbd",
                "a",
                "aa",
                "abc",
                "racecar",
                "character",
                ""
        };


        for (String s : testCases) {

            int memo =
                    solution.longestPalindromeSubseqMemo(s);

            int lcs =
                    solution.longestPalindromeSubseqLCSBottomUp(s);

            int direct =
                    solution.longestPalindromeSubseqDirectDP(s);

            int optimized =
                    solution.longestPalindromeSubseqSpaceOptimized(s);


            System.out.println(
                    "s = \"" + s + "\""
            );

            System.out.println(
                    "  Memoization       : " + memo
            );

            System.out.println(
                    "  LCS Bottom-Up     : " + lcs
            );

            System.out.println(
                    "  Direct LPS DP     : " + direct
            );

            System.out.println(
                    "  Space Optimized   : " + optimized
            );

            System.out.println();
        }
    }
}


