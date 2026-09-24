import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Count Palindromic Subsequences
 * Given a string 's' consisting only of digits, count how many subsequences 
 * of length 5 form a palindrome.
 * 
 * Note: Although not explicitly stated in the prompt, combinatorics for 
 * subsequences of length 5 on a string of size 10^4 will easily cause integer 
 * overflow. Standard platforms (like LeetCode 2484) require returning the 
 * answer modulo 10^9 + 7. We will enforce this safeguard.
 * 
 * Constraints:
 * 1 <= s.length <= 10^4
 * s consists of digits (0-9).
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, this problem is designed to test your ability to 
 * avoid "over-engineering". Many candidates will attempt to build massive 3D 
 * prefix and suffix array matrices.
 * 
 * Q: "Are the characters restricted to digits 0-9?"
 * A: Yes. This is the ultimate skeleton key to the problem!
 * 
 * CRITICAL SENIOR INSIGHT - PATTERN MATCHING TRANSFORMATION:
 * "Because a length 5 palindrome requires the 1st/5th characters and 2nd/4th 
 * characters to match, and the 3rd character can be anything, it forms a strict 
 * pattern: `a, b, ANY, b, a`.
 * 
 * Since there are only 10 possible digits (0-9), there are EXACTLY 
 * 10 * 10 = 100 possible patterns (e.g., '00.00', '01.10', ..., '99.99').
 * 
 * Instead of writing a complex 3D DP state, we can simply iterate through all 
 * 100 possible fixed patterns, and for each one, use the classic 'Distinct 
 * Subsequences' DP algorithm to count how many times that specific pattern 
 * appears in the string. We literally just run a simple sequence-matching DP 
 * 100 times!"
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "For a fixed target pattern like `1, 0, ., 0, 1` (where '.' is a wildcard):
 * We want to find how many times this pattern exists in our string.
 * 
 * At any character in the string, we have two choices:
 * 1. SKIP IT: We ignore the character and carry over the matches we found before.
 * 2. USE IT: If the character matches the current element of our pattern (or if 
 *    the pattern expects the wildcard '.'), we take it! The new combinations 
 *    created are equal to the number of times we matched the PREVIOUS elements 
 *    of the pattern.
 * 
 * We sum this across all 100 patterns."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: s = "103301", testing the specific pattern "1, 0, ., 0, 1"
 * 
 * We track a 1D DP array of size 6 representing matches for lengths 0 through 5.
 * 
 * Initial state (Length 0 is always 1): [1, 0, 0, 0, 0, 0]
 * 
 * Read '1' (Matches index 1):
 * dp[1] += dp[0]  ->  [1, 1, 0, 0, 0, 0]
 * 
 * Read '0' (Matches index 2):
 * dp[2] += dp[1]  ->  [1, 1, 1, 0, 0, 0]
 * 
 * Read '3' (Matches wildcard at index 3):
 * dp[3] += dp[2]  ->  [1, 1, 1, 1, 0, 0]
 * 
 * Read '3' (Matches wildcard at index 3 AGAIN):
 * dp[3] += dp[2]  ->  [1, 1, 1, 2, 0, 0]  (Two ways to pick the middle char!)
 * 
 * Read '0' (Matches index 4):
 * dp[4] += dp[3]  ->  [1, 1, 1, 2, 2, 0]
 * 
 * Read '1' (Matches index 5):
 * dp[5] += dp[4]  ->  [1, 1, 1, 2, 2, 2]
 * 
 * For this specific pattern, we found 2 valid palindromes ("10301" formed twice).
 */
public class CountPalindromicSubsequences {

    private static final int MOD = 1_000_000_007;

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Loop through all 100 possible patterns. For each one, recursively 
     * explore including or excluding characters from the string to match it.
     * 
     * Time Complexity: O(100 * 2^N) - Exponential branching for string matching.
     * Space Complexity: O(N) - Maximum recursion depth.
     */
    public int countPalindromesRecursive(String s) {
        if (s == null || s.length() < 5) return 0;
        long totalPalindromes = 0;

        for (char a = '0'; a <= '9'; a++) {
            for (char b = '0'; b <= '9'; b++) {
                char[] pattern = {a, b, '.', b, a};
                totalPalindromes = (totalPalindromes + solveRecursive(s, pattern, 0, 0)) % MOD;
            }
        }
        return (int) totalPalindromes;
    }

    private int solveRecursive(String s, char[] pattern, int sIndex, int pIndex) {
        // BASE CASE REASONING:
        // We matched all 5 characters of our pattern! This is 1 valid path.
        if (pIndex == 5) return 1;
        
        // We ran out of string characters before finishing the pattern. Dead end.
        if (sIndex == s.length()) return 0;

        // Universe 1: Always branch out to see what happens if we skip this character
        long ways = solveRecursive(s, pattern, sIndex + 1, pIndex);

        // Universe 2: If the character is a match (or a wildcard), try consuming it
        if (pattern[pIndex] == '.' || pattern[pIndex] == s.charAt(sIndex)) {
            ways = (ways + solveRecursive(s, pattern, sIndex + 1, pIndex + 1)) % MOD;
        }

        return (int) ways;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Recursively matching 'a b . b a' repeatedly re-evaluates the same 
     * suffixes. We cache the number of ways to match the remaining pattern 
     * from a specific string index.
     * 
     * Time Complexity: O(100 * N * 5) -> O(N)
     * Space Complexity: O(N * 5) per pattern -> O(N) memory overhead.
     */
    public int countPalindromesMemo(String s) {
        if (s == null || s.length() < 5) return 0;
        long totalPalindromes = 0;

        for (char a = '0'; a <= '9'; a++) {
            for (char b = '0'; b <= '9'; b++) {
                char[] pattern = {a, b, '.', b, a};
                
                // memo[stringIndex][patternIndex]
                int[][] memo = new int[s.length()][5];
                for (int[] row : memo) Arrays.fill(row, -1);
                
                totalPalindromes = (totalPalindromes + solveMemo(s, pattern, 0, 0, memo)) % MOD;
            }
        }
        return (int) totalPalindromes;
    }

    private int solveMemo(String s, char[] pattern, int sIndex, int pIndex, int[][] memo) {
        if (pIndex == 5) return 1;
        if (sIndex == s.length()) return 0;

        if (memo[sIndex][pIndex] != -1) {
            return memo[sIndex][pIndex];
        }

        long ways = solveMemo(s, pattern, sIndex + 1, pIndex, memo);

        if (pattern[pIndex] == '.' || pattern[pIndex] == s.charAt(sIndex)) {
            ways = (ways + solveMemo(s, pattern, sIndex + 1, pIndex + 1, memo)) % MOD;
        }

        memo[sIndex][pIndex] = (int) ways;
        return (int) ways;
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: This perfectly maps to the classic "Distinct Subsequences" algorithm. 
     * We build a 2D matrix where dp[i][j] signifies the number of times the 
     * prefix of the pattern (length 'j') appears in the prefix of the string (length 'i').
     * 
     * Time Complexity: O(100 * N * 5) -> O(N)
     * Space Complexity: O(N * 5) -> O(N)
     */
    public int countPalindromesTabulation(String s) {
        if (s == null || s.length() < 5) return 0;
        long totalPalindromes = 0;
        int n = s.length();

        for (char a = '0'; a <= '9'; a++) {
            for (char b = '0'; b <= '9'; b++) {
                char[] pattern = {a, b, '.', b, a};
                
                int[][] dp = new int[n + 1][6];
                
                // BASE CASE REASONING:
                // An empty pattern (length 0) can be "found" exactly 1 time inside 
                // any string prefix (by selecting nothing).
                for (int i = 0; i <= n; i++) {
                    dp[i][0] = 1;
                }

                for (int i = 1; i <= n; i++) {
                    for (int j = 1; j <= 5; j++) {
                        
                        // 1. We implicitly carry over matches from NOT using the current character
                        int ways = dp[i - 1][j];
                        
                        // 2. If the current character is a valid piece of the puzzle, 
                        // we add all historical matches where we had successfully built 
                        // the pattern exactly one step prior to this.
                        if (pattern[j - 1] == '.' || pattern[j - 1] == s.charAt(i - 1)) {
                            ways = (ways + dp[i - 1][j - 1]) % MOD;
                        }
                        
                        dp[i][j] = ways;
                    }
                }
                // Accumulate the fully completed patterns (length 5)
                totalPalindromes = (totalPalindromes + dp[n][5]) % MOD;
            }
        }
        return (int) totalPalindromes;
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In the 2D Tabulation, calculating row `i` ONLY relies on data from 
     * row `i-1`. We can collapse this into a single 1D array of size 6! 
     * 
     * To prevent a match at index `j` from falsely boosting a match at `j+1` 
     * inside the same step, we MUST iterate the inner loop BACKWARDS.
     * 
     * Time Complexity: O(100 * N * 5) -> O(N)
     * Space Complexity: O(6) -> O(1) strictly constant space. Extremely lightweight!
     */
    public int countPalindromesSpaceOptimized(String s) {
        if (s == null || s.length() < 5) return 0;
        long totalPalindromes = 0;
        int n = s.length();
        
        for (char a = '0'; a <= '9'; a++) {
            for (char b = '0'; b <= '9'; b++) {
                
                char[] pattern = {a, b, '.', b, a};
                
                // This single array represents our pattern progression (lengths 0 through 5)
                int[] dp = new int[6];
                dp[0] = 1; // Empty string base case
                
                for (int i = 0; i < n; i++) {
                    char c = s.charAt(i);
                    
                    // MAGIC OF THE 1D ARRAY:
                    // By iterating backwards, dp[j-1] holds the clean data from the 
                    // PREVIOUS character iteration, acting exactly like dp[i-1][j-1].
                    for (int j = 5; j >= 1; j--) {
                        if (pattern[j - 1] == '.' || pattern[j - 1] == c) {
                            dp[j] = (dp[j] + dp[j - 1]) % MOD;
                        }
                    }
                }
                
                totalPalindromes = (totalPalindromes + dp[5]) % MOD;
            }
        }
        
        return (int) totalPalindromes;
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new CountPalindromicSubsequences();
        
        record TestCase(String s, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase("103301", 2),         // "10301" formed using two different 3s
            new TestCase("0000000", 21),       // 7 Choose 5 = 21 combinations
            new TestCase("9999900000", 2),     // "99999" and "00000"
            new TestCase("123", 0)             // Too short
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("String  : \"" + tc.s + "\"");
            System.out.println("Expected: " + tc.expected);
            
            // Limit brute force test execution on larger combinations
            if (tc.s.length() <= 10) {
                System.out.println("Recursive (Brute) : " + solver.countPalindromesRecursive(tc.s));
            } else {
                System.out.println("Recursive (Brute) : Skipped");
            }
            
            System.out.println("Memoization       : " + solver.countPalindromesMemo(tc.s));
            System.out.println("Tabulation 2D     : " + solver.countPalindromesTabulation(tc.s));
            System.out.println("Space Optimized   : " + solver.countPalindromesSpaceOptimized(tc.s));
            System.out.println();
        }
    }
}


public class Solution {

    /*
     * memo[left][right][length] =
     *
     * Number of palindromic subsequences of length `length`
     * that can be formed using characters from s[left ... right].
     *
     * length only needs to be 0..5 for this problem.
     */
    private Long[][][] memo;

    public long countPalindromes(String s) {
        int n = s.length();

        memo = new Long[n][n][6];

        // Count palindromic subsequences of length 5
        // using the entire string.
        return solve(s, 0, n - 1, 5);
    }

    private long solve(String s, int left, int right, int length) {

        /*
         * Not enough characters are available to build
         * a subsequence of the required length.
         *
         * Example:
         *   range has 3 characters
         *   but we need a palindrome of length 5
         *
         * Therefore, impossible.
         */
        if (right - left + 1 < length) {
            return 0;
        }

        /*
         * A palindrome of length 1 is simply any single character.
         *
         * For example:
         *
         *   s[left ... right] = "abcde"
         *
         * Possible length-1 palindromes:
         *
         *   a, b, c, d, e
         *
         * So there are (right - left + 1) choices.
         */
        if (length == 1) {
            return right - left + 1;
        }

        /*
         * Empty palindrome.
         *
         * We use this as the base case when we have already
         * selected all required characters.
         *
         * There is exactly one way to select nothing:
         *
         *   {}
         */
        if (length == 0) {
            return 1;
        }

        /*
         * No characters left to choose from.
         */
        if (left > right) {
            return 0;
        }

        /*
         * Return previously computed result.
         *
         * The same (left, right, length) state can be reached
         * through many different recursive paths.
         */
        if (memo[left][right][length] != null) {
            return memo[left][right][length];
        }

        /*
         * First count all palindromic subsequences of the
         * required length that can be formed without
         * necessarily using BOTH endpoints.
         *
         * We use inclusion-exclusion:
         *
         *   1. Skip left
         *   2. Skip right
         *   3. Subtract cases where BOTH were skipped
         *
         * Why subtract?
         *
         * A subsequence that uses neither `left` nor `right`
         * is counted in BOTH:
         *
         *   solve(left + 1, right, length)
         *
         * and
         *
         *   solve(left, right - 1, length)
         *
         * So it was counted twice.
         */
        long result =
                solve(s, left + 1, right, length)
                + solve(s, left, right - 1, length)
                - solve(s, left + 1, right - 1, length);

        /*
         * Now consider subsequences that USE BOTH endpoints.
         *
         * For a palindrome, the first and last characters
         * must be equal.
         *
         * Example:
         *
         *       a b c b a
         *       ^       ^
         *       left   right
         *
         * If s[left] == s[right], we can use both.
         *
         * After selecting these two characters:
         *
         *       a [ b c b ] a
         *         <------>
         *
         * We need a palindrome of length `length - 2`
         * inside the remaining range.
         */
        if (s.charAt(left) == s.charAt(right)) {

            result += solve(
                    s,
                    left + 1,
                    right - 1,
                    length - 2
            );
        }

        /*
         * Save the result so that if we encounter the same
         * state again, we don't recompute it.
         */
        memo[left][right][length] = result;

        return result;
    }
}
